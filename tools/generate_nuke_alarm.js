// Génération par code des sons du Scientifique (mage légendaire) de Magic Defense :
//   - nuke_alarm.ogg : compétence « Réaction en chaîne » : sirène d'alerte nucléaire (deux montées-descentes)
//                      puis compte à rebours robotique « trois… deux… un… » (≈ 6 s)
//   - nuke_boom.ogg  : explosion de la bombe nucléaire : craquement, grondement grave et souffle (≈ 3 s)
//
// La voix robotique est une petite synthèse par formants : une source « bourdonnante » (dent de scie à
// hauteur fixe, comme un robot) passe dans trois filtres résonants réglés sur les formants de chaque son
// (voyelles, consonnes), plus un bruit filtré pour les consonnes soufflées. Encodage OGG Vorbis avec la
// bibliothèque WebAssembly « wasm-media-encoders » (comme generate_music.js, pas besoin de ffmpeg).
//
// Utilisation (depuis ce dossier tools/) :
//   npm install                        (installe wasm-media-encoders, une seule fois)
//   node generate_nuke_alarm.js        → écrit les .ogg dans app/src/main/res/raw/
//   node generate_nuke_alarm.js --wav  → écrit aussi des .wav à côté de ce script pour écouter
'use strict';

const fs = require('fs');
const path = require('path');

const RATE = 22050; // effets courts : 22 kHz suffisent et gardent des fichiers légers
const RAW_DIR = path.join(__dirname, '..', 'app', 'src', 'main', 'res', 'raw');
const TWO_PI = 2 * Math.PI;

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

// Filtre passe-bande résonant (biquad), dont la fréquence peut changer à chaque échantillon
function makeBandPass() {
    let x1 = 0, x2 = 0, y1 = 0, y2 = 0;
    return (x, freq, bandwidth) => {
        const w = TWO_PI * Math.min(freq, RATE * 0.45) / RATE;
        const q = Math.max(0.5, freq / bandwidth);
        const alpha = Math.sin(w) / (2 * q);
        const a0 = 1 + alpha;
        const b0 = alpha / a0, b2 = -alpha / a0;
        const a1 = -2 * Math.cos(w) / a0, a2 = (1 - alpha) / a0;
        const y = b0 * x + b2 * x2 - a1 * y1 - a2 * y2;
        x2 = x1; x1 = x; y2 = y1; y1 = y;
        return y;
    };
}

// Passe-bas à un pôle
function makeLowPass() {
    let y = 0;
    return (x, freq) => {
        const k = 1 - Math.exp(-TWO_PI * freq / RATE);
        y += k * (x - y);
        return y;
    };
}

// ---------- Sirène d'alerte ----------
// Deux « whoops » : la hauteur monte de 420 à 1000 Hz puis redescend ; timbre carré adouci
function siren(buf, start, duration) {
    const n = Math.floor(duration * RATE);
    const i0 = Math.floor(start * RATE);
    const cycle = duration / 2;
    let phase = 0;
    const lp = makeLowPass();
    for (let i = 0; i < n; i++) {
        const t = i / RATE;
        const c = (t % cycle) / cycle;                  // 0 → 1 dans chaque cycle
        const sweep = c < 0.6 ? c / 0.6 : 1 - (c - 0.6) / 0.4;
        const f = 420 + 580 * Math.sin(sweep * Math.PI / 2);
        phase += TWO_PI * f / RATE;
        // Onde carrée adoucie (somme des 4 premières harmoniques impaires)
        let v = 0;
        for (let k = 1; k <= 7; k += 2) {
            v += Math.sin(phase * k) / k;
        }
        const env = Math.min(1, t / 0.05) * Math.min(1, (duration - t) / 0.15);
        buf[i0 + i] += 0.32 * env * lp(v, 3500);
    }
}

// ---------- Voix robotique (synthèse par formants) ----------
// Chaque son : durée (s), formants F1-F3 (Hz), voisement (0-1), bruit (0-1) et sa fréquence centrale
const PHONEMES = {
    t: {d: 0.05, f: [400, 1700, 2600], voice: 0, noise: 1, nf: 4200, gap: 0.03},
    d: {d: 0.04, f: [300, 1700, 2600], voice: 0.6, noise: 0.5, nf: 3000, gap: 0.015},
    R: {d: 0.07, f: [500, 1250, 2300], voice: 0.5, noise: 0.45, nf: 1400},   // r français (uvulaire)
    w: {d: 0.07, f: [300, 750, 2200], voice: 1, noise: 0},
    a: {d: 0.26, f: [760, 1300, 2500], voice: 1, noise: 0},
    eu: {d: 0.3, f: [380, 1550, 2300], voice: 1, noise: 0},                   // ø de « deux »
    un: {d: 0.32, f: [560, 1450, 2400], voice: 1, noise: 0, nasal: 1},       // œ̃ de « un »
};

// Prononce une suite de sons à partir de `start` (en secondes) ; retourne l'instant de fin
function say(buf, start, word, rnd) {
    const f0 = 98; // hauteur fixe : voix de robot
    // Découpage en segments, avec transitions douces des formants
    const segs = [];
    let t = start;
    for (const p of word) {
        const ph = PHONEMES[p];
        if (ph.gap) {
            segs.push({t0: t, t1: t + ph.gap, ph: null}); // occlusion (silence avant l'explosion)
            t += ph.gap;
        }
        segs.push({t0: t, t1: t + ph.d, ph});
        t += ph.d;
    }
    const end = t;
    const filters = [makeBandPass(), makeBandPass(), makeBandPass()];
    const noiseFilter = makeBandPass();
    const nasalFilter = makeBandPass();
    let phase = 0;
    let voice = 0;
    const i0 = Math.floor(start * RATE);
    const n = Math.floor((end - start + 0.08) * RATE);
    let cur = segs[0].ph || PHONEMES.a;
    let formants = cur.f.slice();
    for (let i = 0; i < n; i++) {
        const time = start + i / RATE;
        let seg = segs[segs.length - 1];
        for (const s of segs) {
            if (time < s.t1) {
                seg = s;
                break;
            }
        }
        const ph = seg.ph;
        // Formants qui glissent vers ceux du son en cours (≈ 15 ms)
        const target = ph ? ph.f : formants;
        for (let k = 0; k < 3; k++) {
            formants[k] += (target[k] - formants[k]) * 0.004;
        }
        const local = time - seg.t0;
        const segLen = seg.t1 - seg.t0;
        let targetVoice = 0, noise = 0, nf = 3000, nasal = 0;
        if (ph && time < end) {
            targetVoice = ph.voice;
            noise = ph.noise * Math.min(1, local / 0.004) * Math.exp(-local * (ph.voice > 0.5 ? 10 : 35));
            nf = ph.nf || 3000;
            nasal = ph.nasal || 0;
        }
        // Voisement lissé (≈ 10 ms) : pas de clic entre deux sons
        voice += (targetVoice - voice) * 0.005;
        // Fin du mot : la voix s'éteint en 60 ms
        const tail = time > end ? Math.max(0, 1 - (time - end) / 0.06) : 1;
        // Source : dent de scie (voix) + bruit (consonnes)
        phase += f0 / RATE;
        if (phase >= 1) phase -= 1;
        const buzz = (2 * phase - 1) * voice * tail;
        const hiss = (rnd() * 2 - 1) * noise;
        let v = filters[0](buzz, formants[0], 90) * 1.0
            + filters[1](buzz, formants[1], 110) * 0.7
            + filters[2](buzz, formants[2], 160) * 0.35;
        v += noiseFilter(hiss, nf, 1800) * 1.6;
        if (nasal > 0) {
            v += nasalFilter(buzz, 250, 60) * 0.9 * nasal; // résonance nasale grave
        }
        // Effet robot : modulation en anneau légère et quantification (son « numérique »)
        v *= 0.75 + 0.25 * Math.sin(TWO_PI * 55 * time);
        v = Math.round(v * 24) / 24;
        buf[i0 + i] += v * 0.55;
    }
    return end;
}

// Petit bip électronique avant chaque chiffre
function beep(buf, start, f, dur, amp) {
    const i0 = Math.floor(start * RATE);
    const n = Math.floor(dur * RATE);
    for (let i = 0; i < n; i++) {
        const t = i / RATE;
        const env = Math.min(1, t / 0.005) * Math.min(1, (dur - t) / 0.02);
        buf[i0 + i] += amp * env * (Math.sin(TWO_PI * f * t) > 0 ? 1 : -1) * 0.5;
    }
}

function alarmTrack() {
    const total = 6.2;
    const buf = new Float32Array(Math.ceil(total * RATE));
    const rnd = makeRandom(7);
    siren(buf, 0, 2.6);
    // Compte à rebours : « trois… deux… un… »
    const words = [['t', 'R', 'w', 'a'], ['d', 'eu'], ['un']];
    let t = 2.85;
    for (const w of words) {
        beep(buf, t, 1320, 0.07, 0.18);
        say(buf, t + 0.12, w, rnd);
        t += 1.05;
    }
    beep(buf, t, 1760, 0.25, 0.22); // bip final
    return normalize(buf, 0.9);
}

// ---------- Explosion nucléaire ----------
function boomTrack() {
    const total = 3.2;
    const buf = new Float32Array(Math.ceil(total * RATE));
    const rnd = makeRandom(42);
    const lp1 = makeLowPass();
    const lp2 = makeLowPass();
    const lp3 = makeLowPass();
    let phase = 0;
    for (let i = 0; i < buf.length; i++) {
        const t = i / RATE;
        const white = rnd() * 2 - 1;
        // Craquement initial (bruit clair, très court)
        const crack = white * Math.exp(-t * 30) * 0.9;
        // Grondement : bruit filtré de plus en plus grave, qui roule longtemps
        const cutoff = 900 * Math.exp(-t * 2.2) + 70;
        const rumble = lp2(lp1(white, cutoff), cutoff) * 3.2 * Math.min(1, t / 0.02) * Math.exp(-t * 1.1);
        // Infrabasse qui descend de 70 à 28 Hz
        const f = 28 + 42 * Math.exp(-t * 2.5);
        phase += TWO_PI * f / RATE;
        const sub = Math.sin(phase) * 0.85 * Math.min(1, t / 0.01) * Math.exp(-t * 1.3);
        // Souffle qui revient (onde de choc), autour de 0,6 s
        const blast = lp3(white, 400) * 2.2 * Math.exp(-Math.pow((t - 0.6) / 0.35, 2));
        buf[i] = crack + rumble + sub + blast;
    }
    // Saturation douce : son plus massif
    for (let i = 0; i < buf.length; i++) {
        buf[i] = Math.tanh(buf[i] * 1.6);
    }
    const fade = Math.floor(0.3 * RATE);
    for (let i = 0; i < fade; i++) {
        buf[buf.length - 1 - i] *= i / fade;
    }
    return normalize(buf, 0.95);
}

function normalize(buf, peak) {
    let max = 0;
    for (let i = 0; i < buf.length; i++) {
        max = Math.max(max, Math.abs(buf[i]));
    }
    if (max > 0) {
        for (let i = 0; i < buf.length; i++) {
            buf[i] *= peak / max;
        }
    }
    return buf;
}

// ---------- Écriture des fichiers ----------
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
    encoder.configure({channels: 1, sampleRate: RATE, vbrQuality: 3});
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
    const sounds = {nuke_alarm: alarmTrack, nuke_boom: boomTrack};
    for (const [name, build] of Object.entries(sounds)) {
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
