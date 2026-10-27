package com.example.magicdefense.view;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;

import com.example.magicdefense.R;

import java.util.Random;

// Effets sonores du jeu, et accès au volume multimédia du téléphone
public class SoundManager {
    private final SoundPool pool;
    private final AudioManager audioManager;
    private final Random random = new Random();
    private final int recruit, merge, kill, castleHit, ultimate, victory, defeat, click;

    public SoundManager(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)  // son de jeu (volume multimédia)
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

    // relativeVolume : pour équilibrer les sons entre eux ; rate : hauteur (1 = normale)
    private void play(int soundId, float relativeVolume, float rate) {
        pool.play(soundId, relativeVolume, relativeVolume, 1, 0, rate);
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

    // Volume multimédia du téléphone, entre 0 et 1 (celui des touches de volume)
    public float getSystemVolume() {
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / (float) max;
    }

    // Règle le volume multimédia du téléphone depuis le curseur
    public void setSystemVolume(float volume) {
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int index = Math.round(volume * max); // le téléphone a des crans (25 sur un Pixel)
        if (index != audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0); // 0 : sans le panneau système
        }
    }

    // Libère la mémoire des sons quand le jeu est fermé
    public void release() {
        pool.release();
    }
}