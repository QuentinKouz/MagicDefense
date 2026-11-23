package com.example.magicdefense.view;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;

import com.example.magicdefense.R;
import com.example.magicdefense.model.MusicMood;

// Musique de fond : trois boucles (normale, nuit, boss) choisies selon la situation (MusicMood),
// avec un fondu enchaîné court quand on change de piste. Une piste déjà en cours n'est jamais
// redémarrée ; une piste mise de côté reprend là où elle s'était arrêtée.
//
// Le volume est INTERNE au jeu (curseur « Musique » des options, MediaPlayer.setVolume) : on ne
// modifie jamais le volume du téléphone. On ne demande pas l'audio focus (MediaPlayer ne le fait
// pas tout seul) : la musique d'une autre appli (Spotify...) continue de jouer à côté du jeu.
public class MusicPlayer {
    // Ressource et niveau de chaque piste (plus bas que les bruitages, qui doivent rester audibles)
    private static final int[] TRACKS = {R.raw.music, R.raw.music_night, R.raw.music_boss};
    private static final float[] TRACK_LEVEL = {0.45f, 0.5f, 0.42f};
    private static final float FADE_STEP = 1f / 45; // fondu de 0,75 s à 60 images/s

    private final Context context;
    private final AudioAttributes attributes;
    private final AudioManager audioManager;
    private final MediaPlayer[] players = new MediaPlayer[MusicMood.COUNT]; // créés au premier besoin
    private final float[] gain = new float[MusicMood.COUNT];          // fondu de chaque piste, de 0 à 1
    private final float[] appliedVolume = new float[MusicMood.COUNT]; // dernier volume envoyé au lecteur
    private boolean foreground = false; // l'appli est à l'écran
    private boolean released = false;

    public MusicPlayer(Context context) {
        this.context = context.getApplicationContext();
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build();
        for (int i = 0; i < appliedVolume.length; i++) {
            appliedVolume[i] = -1;
        }
    }

    // Lecteur de la piste i, créé au premier besoin.
    // Les attributs audio doivent être donnés AVANT la préparation : MediaPlayer.create(context, id)
    // prépare tout de suite, donc on passe par la version qui reçoit les attributs.
    private MediaPlayer player(int i) {
        if (players[i] == null && !released) {
            int session = audioManager != null ? audioManager.generateAudioSessionId() : AudioManager.ERROR;
            MediaPlayer p = session > 0
                    ? MediaPlayer.create(context, TRACKS[i], attributes, session)
                    : MediaPlayer.create(context, TRACKS[i]);
            if (p != null) {
                p.setLooping(true);
                p.setVolume(0f, 0f);
                appliedVolume[i] = 0f;
            }
            players[i] = p;
        }
        return players[i];
    }

    // Appelée à chaque image : option « Musique », piste voulue (MusicMood) et volume interne (0 à 1)
    public void update(boolean enabled, int mood, float volume) {
        if (!foreground || released) {
            return;
        }
        for (int i = 0; i < MusicMood.COUNT; i++) {
            float target = enabled && i == mood ? 1f : 0f;
            MediaPlayer p = target > 0 || gain[i] > 0 ? player(i) : players[i];
            if (p == null) {
                gain[i] = 0;
                continue;
            }
            if (target > 0 && !p.isPlaying()) {
                p.start(); // démarre (ou reprend là où elle s'était arrêtée), en partant du silence
            } else if (target == 0 && !p.isPlaying()) {
                gain[i] = 0; // déjà arrêtée : rien à fondre
                continue;
            }
            // Fondu vers la cible
            if (gain[i] < target) {
                gain[i] = Math.min(target, gain[i] + FADE_STEP);
            } else if (gain[i] > target) {
                gain[i] = Math.max(target, gain[i] - FADE_STEP);
            }
            if (gain[i] <= 0 && target == 0) {
                p.pause(); // fondu terminé : la piste est mise de côté
                continue;
            }
            float v = gain[i] * TRACK_LEVEL[i] * Math.max(0f, Math.min(1f, volume));
            if (Math.abs(v - appliedVolume[i]) > 0.001f) {
                p.setVolume(v, v);
                appliedVolume[i] = v;
            }
        }
    }

    // L'appli revient à l'écran (la musique repart à la prochaine image, via update)
    public void resume() {
        foreground = true;
    }

    // L'appli passe en arrière-plan : on ne joue pas de musique en dehors du jeu
    public void pause() {
        foreground = false;
        for (MediaPlayer p : players) {
            if (p != null && p.isPlaying()) {
                p.pause();
            }
        }
    }

    public void release() {
        released = true;
        for (int i = 0; i < players.length; i++) {
            if (players[i] != null) {
                players[i].release();
                players[i] = null;
            }
        }
    }
}
