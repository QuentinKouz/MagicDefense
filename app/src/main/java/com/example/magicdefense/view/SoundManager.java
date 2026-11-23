package com.example.magicdefense.view;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

import com.example.magicdefense.R;

import java.util.Random;

// Effets sonores du jeu.
// Le volume est INTERNE au jeu (curseur « Bruitages » des options) : on ne touche jamais au volume
// du téléphone, et on ne demande pas l'audio focus, pour que le joueur puisse écouter sa propre
// musique (Spotify...) en même temps.
public class SoundManager {
    private final SoundPool pool;
    private final Random random = new Random();
    private final int recruit, merge, kill, castleHit, ultimate, victory, defeat, click;
    private float volume = 1f; // volume interne des bruitages, de 0 à 1

    public SoundManager(Context context) {
        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)  // son de jeu : se mélange aux autres applis
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        pool = new SoundPool.Builder()
                .setMaxStreams(8)                      // jusqu'à 8 sons en même temps
                .setAudioAttributes(attributes)
                .build();

        recruit = pool.load(context, R.raw.recruit, 1);
        merge = pool.load(context, R.raw.merge, 1);
        kill = pool.load(context, R.raw.kill, 1);
        castleHit = pool.load(context, R.raw.castle_hit, 1);
        ultimate = pool.load(context, R.raw.ultimate, 1);
        victory = pool.load(context, R.raw.victory, 1);
        defeat = pool.load(context, R.raw.defeat, 1);
        click = pool.load(context, R.raw.click, 1);
    }

    // Volume interne des bruitages (curseur des options), entre 0 et 1
    public void setVolume(float v) {
        volume = Math.max(0f, Math.min(1f, v));
    }

    // relativeVolume : pour équilibrer les sons entre eux ; rate : hauteur (1 = normale)
    private void play(int soundId, float relativeVolume, float rate) {
        float v = relativeVolume * volume;
        if (v <= 0) {
            return; // bruitages coupés
        }
        pool.play(soundId, v, v, 1, 0, rate);
    }

    public void recruit()   { play(recruit, 0.8f, 1f); }
    public void merge()     { play(merge, 1f, 1f); }
    public void castleHit() { play(castleHit, 1f, 1f); }
    public void ultimate()  { play(ultimate, 1f, 1f); }
    public void victory()   { play(victory, 0.9f, 1f); }
    public void defeat()    { play(defeat, 0.9f, 1f); }
    public void click()     { play(click, 0.6f, 1f); }

    // Joué très souvent : plus discret, et hauteur légèrement variée pour ne pas lasser
    public void kill() {
        play(kill, 0.5f, 0.9f + random.nextFloat() * 0.2f);
    }

    // Libère la mémoire des sons quand le jeu est fermé
    public void release() {
        pool.release();
    }
}
