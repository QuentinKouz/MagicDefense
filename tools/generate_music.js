// Génération par code des musiques supplémentaires de Magic Defense :
//   - music_boss.ogg  : tendue et épique (boss sur le terrain), ré mineur, 140 BPM, 24 mesures ≈ 41 s
//   - music_night.ogg : calme et inquiétante (vagues de nuit), la mineur, 66 BPM, 12 mesures ≈ 44 s
//
// Synthèse additive simple (cuivres, cordes, basse, boîte à musique, timbales, caisse claire...),
// petite réverbération, puis encodage OGG Vorbis avec la bibliothèque WebAssembly « wasm-media-encoders »
// (pas besoin de ffmpeg). Chaque morceau boucle sans coupure : la queue (réverbération, notes qui
// résonnent) est recopiée au début du fichier.
//
// Utilisation (depuis ce dossier tools/) :
//   npm install            (installe wasm-media-encoders, une seule fois)
//   node generate_music.js            → écrit les .ogg dans app/src/main/res/raw/
//   node generate_music.js --wav      → écrit aussi des .wav à côté de ce script pour écouter
'use strict';

const fs = require('fs');
const path = require('path');

const RATE = 44100;
const RAW_DIR = path.join(__dirname, '..', 'app', 'src', 'main', 'res', 'raw');

// ---------- Outils ----------

// Générateur pseudo-aléatoire à graine : le résultat est identique à chaque génération
function makeRandom(seed) {
    let s = seed >>> 0;
    return () => {
        s = (s + 0x6D2B79F5) >>> 0;
        let t = s;
        t = Math.imul(t ^ (t >>> 15), t | 1);
        t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
        return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
    };
}

const mtof = (m) => 440 * Math.pow(2, (m - 69) / 12);
const TWO_PI = 2 * Math.PI;

// Enveloppe ADSR simple (temps en secondes depuis le début de la note)
function adsr(t, dur, a, d, s, r) {
    if (t < 0) return 0;
    let v;
    if (t < a) v = t / a;
    else if (t < a + d) v = 1 - (1 - s) * (t - a) / d;
    else v = s;
    if (t > dur) v *= Math.max(0, 1 - (t - dur) / r);
    return v;
}

// Ajoute une note à un tampon : voice(t, dur, f, rnd) renvoie un échantillon, ou null quand la note est finie
function addNote(buf, start, dur, freq, amp, voice, tail) {
    const i0 = Math.floor(start * RATE);
    const n = Math.floor((dur + tail) * RATE);
    for (let i = 0; i < n; i++) {
        const idx = i0 + i;
        if (idx >= buf.length) break;
        const t = i / RATE;
        buf[idx] += amp * voice(t, dur, freq);
    }
}

// Somme d'harmoniques (dent de scie adoucie) limitée sous la fréquence de Nyquist
function additive(f, t, count, slope, phase) {
    let v = 0;
    const max = Math.min(count, Math.floor(RATE / 2.2 / f));
    for (let k = 1; k <= max; k++) {
        v += Math.sin(TWO_PI * k * f * t + phase * k) / Math.pow(k, slope);
    }
    return v;
}

// ---------- Instruments ----------

// Cuivre : harmoniques qui s'éclaircissent à l'attaque, léger vibrato après 0,2 s
function brass(t, dur, f) {
    const env = adsr(t, dur, 0.04, 0.15, 0.75, 0.12);
    if (env <= 0 && t > dur) return 0;
    const vib = 1 + 0.004 * Math.sin(TWO_PI * 5.5 * t) * Math.min(1, Math.max(0, (t - 0.2) * 3));
    const bright = 1.2 + 0.6 * Math.exp(-t * 6); // plus brillant au début
    return env * 0.5 * additive(f * vib, t, 12, bright, 0);
}

// Nappe de cordes : trois voix légèrement désaccordées, attaque lente
function strings(t, dur, f) {
    const env = adsr(t, dur, 0.35, 0.3, 0.85, 0.5);
    let v = 0;
    const detune = [0.996, 1, 1.004];
    for (let i = 0; i < 3; i++) {
        v += additive(f * detune[i], t, 8, 1.6, i * 1.3);
    }
    return env * v * 0.18;
}

// Cordes staccato (ostinato épique)
function stringStab(t, dur, f) {
    const env = adsr(t, dur, 0.008, 0.08, 0.4, 0.06);
    return env * 0.4 * (additive(f, t, 10, 1.3, 0) + 0.6 * additive(f * 1.003, t, 10, 1.3, 2));
}

// Basse : dent de scie grave + sous-oscillateur sinusoïdal
function bass(t, dur, f) {
    const env = adsr(t, dur, 0.005, 0.12, 0.55, 0.05);
    return env * (0.45 * additive(f, t, 7, 1.4, 0) + 0.5 * Math.sin(TWO_PI * f * t));
}

// Basse douce de la nuit : sinus avec une pointe de 2e harmonique
function softBass(t, dur, f) {
    const env = adsr(t, dur, 0.05, 0.6, 0.5, 0.6);
    return env * (Math.sin(TWO_PI * f * t) + 0.2 * Math.sin(TWO_PI * 2 * f * t));
}

// Boîte à musique / célesta : partiels de cloche à décroissance rapide
function celesta(t, dur, f) {
    if (t > 3) return 0;
    const a = Math.min(1, t / 0.003);
    return a * (Math.sin(TWO_PI * f * t) * Math.exp(-t * 1.6)
        + 0.35 * Math.sin(TWO_PI * 2 * f * t) * Math.exp(-t * 3)
        + 0.12 * Math.sin(TWO_PI * 3.01 * f * t) * Math.exp(-t * 6)
        + 0.06 * Math.sin(TWO_PI * 4.2 * f * t) * Math.exp(-t * 9));
}

// Nappe sombre : harmoniques impaires (son de flûte/orgue doux), attaque très lente
function darkPad(t, dur, f) {
    const env = adsr(t, dur, 1.2, 0.5, 0.8, 1.5);
    const trem = 1 + 0.08 * Math.sin(TWO_PI * 0.4 * t);
    let v = 0;
    for (const det of [0.997, 1.003]) {
        for (let k = 1; k <= 7; k += 2) {
            v += Math.sin(TWO_PI * k * f * det * t + k) / (k * k);
        }
    }
    return env * trem * v * 0.5;
}

// ---------- Percussions (bruit pseudo-aléatoire à graine) ----------

function makeDrums(seed) {
    const rnd = makeRandom(seed);
    const noise = new Float32Array(RATE * 2);
    for (let i = 0; i < noise.length; i++) noise[i] = rnd() * 2 - 1;

    return {
        // Grosse caisse / tambour de guerre : sinus dont la hauteur chute
        kick(buf, start, amp, low) {
            const f0 = low ? 90 : 120, f1 = low ? 42 : 50;
            const n = Math.floor(0.6 * RATE);
            let ph = 0;
            const i0 = Math.floor(start * RATE);
            for (let i = 0; i < n && i0 + i < buf.length; i++) {
                const t = i / RATE;
                const f = f1 + (f0 - f1) * Math.exp(-t * 25);
                ph += TWO_PI * f / RATE;
                buf[i0 + i] += amp * Math.sin(ph) * Math.exp(-t * (low ? 5 : 7));
            }
        },
        // Timbale accordée : note grave qui résonne, petit bruit d'attaque
        timpani(buf, start, freq, amp) {
            const n = Math.floor(1.2 * RATE);
            const i0 = Math.floor(start * RATE);
            for (let i = 0; i < n && i0 + i < buf.length; i++) {
                const t = i / RATE;
                const v = Math.sin(TWO_PI * freq * t) * Math.exp(-t * 3)
                    + 0.4 * Math.sin(TWO_PI * freq * 1.5 * t) * Math.exp(-t * 5)
                    + 0.3 * noise[i] * Math.exp(-t * 40);
                buf[i0 + i] += amp * v;
            }
        },
        // Caisse claire : bruit + corps à 185 Hz
        snare(buf, start, amp) {
            const n = Math.floor(0.25 * RATE);
            const i0 = Math.floor(start * RATE);
            let lp = 0;
            for (let i = 0; i < n && i0 + i < buf.length; i++) {
                const t = i / RATE;
                lp += (noise[(i * 3) % noise.length] - lp) * 0.6;
                buf[i0 + i] += amp * (0.7 * lp * Math.exp(-t * 18) + 0.5 * Math.sin(TWO_PI * 185 * t) * Math.exp(-t * 30));
            }
        },
        // Charleston : bruit aigu (différence d'échantillons) très court
        hat(buf, start, amp, open) {
            const n = Math.floor((open ? 0.3 : 0.06) * RATE);
            const i0 = Math.floor(start * RATE);
            const off = Math.floor(start * 997) % RATE;
            for (let i = 1; i < n && i0 + i < buf.length; i++) {
                const t = i / RATE;
                const hp = noise[off + i] - noise[off + i - 1];
                buf[i0 + i] += amp * 0.5 * hp * Math.exp(-t * (open ? 12 : 70));
            }
        },
        // Cymbale : bruit aigu long
        crash(buf, start, amp) {
            const n = Math.floor(1.8 * RATE);
            const i0 = Math.floor(start * RATE);
            for (let i = 1; i < n && i0 + i < buf.length; i++) {
                const t = i / RATE;
                const j = (i * 7) % noise.length;
                const hp = noise[j] - noise[(j + 1) % noise.length];
                buf[i0 + i] += amp * 0.35 * hp * Math.exp(-t * 2.2);
            }
        },
        // Tom : sinus qui descend un peu
        tom(buf, start, freq, amp) {
            const n = Math.floor(0.4 * RATE);
            const i0 = Math.floor(start * RATE);
            let ph = 0;
            for (let i = 0; i < n && i0 + i < buf.length; i++) {
                const t = i / RATE;
                ph += TWO_PI * freq * (1 + 0.5 * Math.exp(-t * 20)) / RATE;
                buf[i0 + i] += amp * (Math.sin(ph) * Math.exp(-t * 9) + 0.2 * noise[i] * Math.exp(-t * 50));
            }
        },
        // Petit clic de bois (bloc chinois)
        wood(buf, start, amp) {
            const n = Math.floor(0.08 * RATE);
            const i0 = Math.floor(start * RATE);
            for (let i = 0; i < n && i0 + i < buf.length; i++) {
                const t = i / RATE;
                buf[i0 + i] += amp * (Math.sin(TWO_PI * 1250 * t) + 0.5 * Math.sin(TWO_PI * 2100 * t)) * Math.exp(-t * 60);
            }
        },
        // Vent : bruit filtré dont le volume ondule lentement
        wind(buf, amp, period) {
            let lp1 = 0, lp2 = 0;
            for (let i = 0; i < buf.length; i++) {
                const t = i / RATE;
                lp1 += (noise[i % noise.length] - lp1) * 0.02;
                lp2 += (lp1 - lp2) * 0.05;
                const swell = 0.5 - 0.5 * Math.cos(TWO_PI * t / period);
                buf[i] += amp * lp2 * 6 * swell;
            }
        },
    };
}

// ---------- Effets ----------

// Réverbération de Schroeder : 4 filtres en peigne en parallèle, puis 2 passe-tout
function reverb(input, decay, mix) {
    const combs = [1557, 1617, 1491, 1422];
    const out = new Float32Array(input.length);
    for (const d of combs) {
        const line = new Float32Array(d);
        let pos = 0, lp = 0;
        for (let i = 0; i < input.length; i++) {
            const y = line[pos];
            lp = y * 0.7 + lp * 0.3; // amortit les aigus
            line[pos] = input[i] + lp * decay;
            out[i] += y * 0.25;
            pos = (pos + 1) % d;
        }
    }
    for (const d of [225, 556]) {
        const line = new Float32Array(d);
        let pos = 0;
        for (let i = 0; i < out.length; i++) {
            const buffered = line[pos];
            const x = out[i];
            const y = -x * 0.5 + buffered;
            line[pos] = x + buffered * 0.5;
            out[i] = y;
            pos = (pos + 1) % d;
        }
    }
    const res = new Float32Array(input.length);
    for (let i = 0; i < input.length; i++) res[i] = input[i] + out[i] * mix;
    return res;
}

// Boucle sans coupure : la fin (après loopLen) est ajoutée au début, puis on coupe
function wrapLoop(buf, loopLen) {
    const n = Math.floor(loopLen * RATE);
    const out = new Float32Array(n);
    out.set(buf.subarray(0, n));
    for (let i = n; i < buf.length; i++) out[i - n] += buf[i];
    return out;
}

// Normalise puis adoucit les crêtes (tanh)
function master(buf, peak) {
    let max = 0;
    for (const v of buf) max = Math.max(max, Math.abs(v));
    const g = peak / Math.max(max, 1e-6) * 1.15;
    const out = new Float32Array(buf.length);
    for (let i = 0; i < buf.length; i++) out[i] = Math.tanh(buf[i] * g) * peak / Math.tanh(1.15 * peak);
    return out;
}

// ---------- Morceau du boss : ré mineur, 140 BPM ----------

function bossTrack() {
    const BPM = 140, beat = 60 / BPM, bar = 4 * beat;
    const BARS = 24;
    const loopLen = BARS * bar;
    const tail = 2.5;
    const len = Math.ceil((loopLen + tail) * RATE);
    const music = new Float32Array(len); // instruments mélodiques (passent dans la réverbération)
    const drums = new Float32Array(len);
    const dr = makeDrums(1234);

    // Accords (une mesure chacun) : Rém Rém Sib Do Rém Rém Solm La
    const chords = [
        {root: 38, notes: [62, 65, 69]}, {root: 38, notes: [62, 65, 69]},
        {root: 34, notes: [58, 62, 65]}, {root: 36, notes: [60, 64, 67]},
        {root: 38, notes: [62, 65, 69]}, {root: 38, notes: [62, 65, 69]},
        {root: 31, notes: [55, 58, 62]}, {root: 33, notes: [57, 61, 64]},
    ];
    // Thème des cuivres (8 mesures) : [note MIDI ou null, durée en temps]
    const theme = [
        [74, 1.5], [69, 0.5], [74, 1], [77, 1],
        [76, 1.5], [74, 0.5], [72, 1], [69, 1],
        [70, 2], [74, 1], [77, 1],
        [76, 2], [79, 1], [76, 1],
        [77, 1.5], [76, 0.5], [74, 1], [81, 1],
        [79, 1.5], [77, 0.5], [76, 1], [74, 1],
        [82, 1], [81, 1], [79, 1], [74, 1],
        [73, 2], [76, 1], [69, 1],
    ];

    for (let b = 0; b < BARS; b++) {
        const t0 = b * bar;
        const section = Math.floor(b / 8); // 0 : ostinato, 1 : thème, 2 : thème renforcé
        const c = chords[b % 8];
        const lastOfSection = b % 8 === 7;

        // Nappe de cordes (accord tenu)
        for (const n of c.notes) addNote(music, t0, bar * 0.95, mtof(n), 0.10, strings, 0.6);

        // Basse en croches : fondamentale, octave sur le « et » de 2 et de 4
        const pattern = [0, 0, 0, 12, 0, 0, 0, 12];
        for (let e = 0; e < 8; e++) {
            const accent = e % 4 === 0 ? 1 : 0.75;
            addNote(music, t0 + e * beat / 2, beat * 0.42, mtof(c.root + pattern[e]), 0.22 * accent, bass, 0.06);
        }

        // Ostinato de cordes staccato (arpège de l'accord)
        const arp = [0, 1, 2, 1, 0, 1, 2, 1];
        const ostAmp = section === 0 ? 0.12 : 0.07;
        for (let e = 0; e < 8; e++) {
            addNote(music, t0 + e * beat / 2, beat * 0.3, mtof(c.notes[arp[e]] + 12), ostAmp, stringStab, 0.08);
        }

        // Percussions
        dr.timpani(drums, t0, mtof(c.root + 12), 0.5);
        dr.kick(drums, t0, 0.55, false);
        dr.kick(drums, t0 + 2 * beat, 0.5, false);
        if (section === 0) {
            dr.timpani(drums, t0 + 2.5 * beat, mtof(c.root + 12), 0.25);
            dr.kick(drums, t0 + 3.5 * beat, 0.35, false);
        }
        if (section >= 1) {
            dr.snare(drums, t0 + beat, 0.4);
            dr.snare(drums, t0 + 3 * beat, 0.4);
            dr.kick(drums, t0 + 1.5 * beat, 0.35, false);
            dr.kick(drums, t0 + 2.5 * beat, 0.35, false);
        }
        const hatStep = section === 2 ? 0.25 : 0.5;
        if (section >= 1) {
            for (let h = 0; h < 4; h += hatStep) {
                dr.hat(drums, t0 + h * beat, (h % 1 === 0 ? 0.18 : 0.11), false);
            }
        } else {
            for (let h = 0.5; h < 4; h += 1) dr.hat(drums, t0 + h * beat, 0.1, false);
        }
        if (b % 8 === 0 && b > 0) dr.crash(drums, t0, 0.35);
        if (b === 0) dr.crash(drums, t0, 0.3); // aussi au début (enchaîne la boucle)
        if (lastOfSection) {
            // Roulement de toms sur les temps 3 et 4 (doubles croches)
            const toms = [196, 175, 147, 131, 110, 98, 87, 73];
            for (let k = 0; k < 8; k++) dr.tom(drums, t0 + 2 * beat + k * beat / 4, toms[k], 0.35);
        }
    }

    // Thème des cuivres sur les sections 2 et 3 (doublé à l'octave grave sur la 3e)
    for (let s = 1; s <= 2; s++) {
        let t = s * 8 * bar;
        for (const [n, d] of theme) {
            if (n !== null) {
                addNote(music, t, d * beat * 0.92, mtof(n), 0.16, brass, 0.15);
                if (s === 2) addNote(music, t, d * beat * 0.92, mtof(n - 12), 0.11, brass, 0.15);
            }
            t += d * beat;
        }
    }
    // Section 1 : appel de cuivres grave à chaque début de phrase de 2 mesures
    for (let b = 0; b < 8; b += 2) {
        const c = chords[b];
        addNote(music, b * bar, beat * 1.5, mtof(c.root + 24), 0.12, brass, 0.15);
        addNote(music, b * bar + 1.5 * beat, beat * 0.5, mtof(c.root + 24), 0.10, brass, 0.1);
        addNote(music, b * bar + 2 * beat, beat * 1.9, mtof(c.root + 31), 0.12, brass, 0.15);
    }

    const wet = reverb(music, 0.78, 0.35);
    const mix = new Float32Array(len);
    for (let i = 0; i < len; i++) mix[i] = wet[i] + drums[i] * 0.9;
    return master(wrapLoop(mix, loopLen), 0.9);
}

// ---------- Morceau de la nuit : la mineur, 66 BPM ----------

function nightTrack() {
    const BPM = 66, beat = 60 / BPM, bar = 4 * beat;
    const BARS = 12;
    const loopLen = BARS * bar;
    const tail = 4;
    const len = Math.ceil((loopLen + tail) * RATE);
    const music = new Float32Array(len);
    const perc = new Float32Array(len);
    const dr = makeDrums(777);

    // Accords : Lam Fa Rém Mi | Lam Fa Sol Mi | Lam Rém Fa Mi
    const chords = [
        {root: 45, notes: [57, 60, 64]}, {root: 41, notes: [57, 60, 65]},
        {root: 38, notes: [57, 62, 65]}, {root: 40, notes: [56, 59, 64]},
        {root: 45, notes: [57, 60, 64]}, {root: 41, notes: [57, 60, 65]},
        {root: 43, notes: [55, 59, 62]}, {root: 40, notes: [56, 59, 64]},
        {root: 45, notes: [57, 60, 64]}, {root: 38, notes: [57, 62, 65]},
        {root: 41, notes: [57, 60, 65]}, {root: 40, notes: [56, 59, 64]},
    ];
    // Mélodie de boîte à musique, clairsemée (null = silence)
    const melody = [
        [null, 1], [76, 0.5], [81, 0.5], [84, 1], [83, 1],
        [81, 2], [null, 0.5], [77, 0.5], [76, 1],
        [74, 1], [77, 1], [81, 1], [80, 1],
        [83, 2], [80, 1], [null, 1],
        [null, 1], [84, 0.5], [83, 0.5], [81, 1], [76, 1],
        [77, 1], [81, 1], [84, 2],
        [83, 1.5], [86, 0.5], [83, 1], [79, 1],
        [80, 2], [null, 2],
        [88, 1], [null, 0.5], [84, 0.5], [81, 2],
        [77, 1], [81, 1], [86, 1], [84, 1],
        [81, 1.5], [79, 0.5], [77, 1], [76, 1],
        [80, 1], [83, 1], [76, 2],
    ];

    // Bourdon grave (la + mi) sur tout le morceau : tension sous les accords
    const drone = (t) => 0.5 * Math.sin(TWO_PI * mtof(33) * t) + 0.25 * Math.sin(TWO_PI * mtof(40) * t + 1);
    for (let i = 0; i < len; i++) {
        const t = i / RATE;
        music[i] += 0.07 * drone(t) * (0.8 + 0.2 * Math.sin(TWO_PI * t / (loopLen / 3)));
    }

    for (let b = 0; b < BARS; b++) {
        const t0 = b * bar;
        const c = chords[b];
        for (const n of c.notes) addNote(music, t0, bar * 0.98, mtof(n - 12), 0.09, darkPad, 1.6);
        addNote(music, t0, beat * 1.8, mtof(c.root - 12 + 12), 0.22, softBass, 0.7);
        addNote(music, t0 + 2 * beat, beat * 1.5, mtof(c.root - 12 + 12), 0.15, softBass, 0.7);

        // Battement de cœur (boum-boum) et petits clics de bois
        dr.kick(perc, t0, 0.45, true);
        dr.kick(perc, t0 + 0.32, 0.3, true);
        dr.wood(perc, t0 + 1.5 * beat, 0.05);
        dr.wood(perc, t0 + 3.5 * beat, 0.035);
        for (let h = 0; h < 4; h++) dr.hat(perc, t0 + h * beat + beat / 2, 0.04, false);
    }

    let t = 0;
    for (const [n, d] of melody) {
        if (n !== null) addNote(music, t, d * beat, mtof(n), 0.2, celesta, 3);
        t += d * beat;
    }
    // Écho de la mélodie une octave plus bas, discret et en retard d'une croche
    t = beat / 2;
    for (const [n, d] of melody) {
        if (n !== null) addNote(music, t, d * beat, mtof(n - 12), 0.06, celesta, 3);
        t += d * beat;
    }

    dr.wind(perc, 0.06, loopLen / 2);

    const wet = reverb(music, 0.86, 0.6);
    const mix = new Float32Array(len);
    for (let i = 0; i < len; i++) mix[i] = wet[i] + perc[i];
    return master(wrapLoop(mix, loopLen), 0.8);
}

// ---------- Sortie ----------

function writeWav(file, samples) {
    const data = Buffer.alloc(samples.length * 2);
    for (let i = 0; i < samples.length; i++) {
        data.writeInt16LE(Math.max(-32768, Math.min(32767, Math.round(samples[i] * 32767))), i * 2);
    }
    const h = Buffer.alloc(44);
    h.write('RIFF', 0); h.writeUInt32LE(36 + data.length, 4); h.write('WAVE', 8);
    h.write('fmt ', 12); h.writeUInt32LE(16, 16); h.writeUInt16LE(1, 20); h.writeUInt16LE(1, 22);
    h.writeUInt32LE(RATE, 24); h.writeUInt32LE(RATE * 2, 28); h.writeUInt16LE(2, 32); h.writeUInt16LE(16, 34);
    h.write('data', 36); h.writeUInt32LE(data.length, 40);
    fs.writeFileSync(file, Buffer.concat([h, data]));
}

async function writeOgg(file, samples) {
    const {createEncoder} = require('wasm-media-encoders');
    const wasm = fs.readFileSync(require.resolve('wasm-media-encoders/wasm/ogg'));
    const encoder = await createEncoder('audio/ogg', wasm);
    encoder.configure({channels: 1, sampleRate: RATE, vbrQuality: 2});
    const parts = [];
    const chunk = 8192;
    for (let i = 0; i < samples.length; i += chunk) {
        parts.push(Buffer.from(encoder.encode([samples.subarray(i, Math.min(samples.length, i + chunk))])));
    }
    parts.push(Buffer.from(encoder.finalize()));
    const out = Buffer.concat(parts);
    fs.writeFileSync(file, out);
    return out.length;
}

async function main() {
    const wav = process.argv.includes('--wav');
    const tracks = {music_boss: bossTrack, music_night: nightTrack};
    for (const [name, build] of Object.entries(tracks)) {
        const samples = build();
        const size = await writeOgg(path.join(RAW_DIR, name + '.ogg'), samples);
        console.log(`${name}.ogg : ${(samples.length / RATE).toFixed(1)} s, ${(size / 1024).toFixed(0)} Ko`);
        if (wav) writeWav(path.join(__dirname, name + '.wav'), samples);
    }
}

main().catch((e) => {
    console.error(e);
    process.exit(1);
});
