package com.example.magicdefense.view;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;

import com.example.magicdefense.R;

// Musique de fond : une boucle de 40 s jouée en continu, que le joueur peut couper dans les options.
// Le volume suit le volume multimédia du téléphone (comme les bruitages).
public class MusicPlayer {
    private static final float MUSIC_VOLUME = 0.45f; // plus bas que les bruitages, qui doivent rester audibles

    private MediaPlayer player;
    private boolean enabled = true;     // option du joueur
    private boolean foreground = false; // l'appli est à l'écran

    public MusicPlayer(Context context) {
        player = MediaPlayer.create(context, R.raw.music);
        if (player != null) {
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build());
            player.setLooping(true);
            player.setVolume(MUSIC_VOLUME, MUSIC_VOLUME);
        }
    }

    // Option "Musique" : appelée à chaque image, ne fait quelque chose que si l'état change
    public void setEnabled(boolean on) {
        if (on != enabled) {
            enabled = on;
            refresh();
        }
    }

    // L'appli revient à l'écran
    public void resume() {
        foreground = true;
        refresh();
    }

    // L'appli passe en arrière-plan : on ne joue pas de musique en dehors du jeu
    public void pause() {
        foreground = false;
        refresh();
    }

    public void release() {
        if (player != null) {
            player.release();
            player = null;
        }
    }

    private void refresh() {
        if (player == null) {
            return;
        }
        boolean shouldPlay = enabled && foreground;
        if (shouldPlay && !player.isPlaying()) {
            player.start();     // reprend là où elle s'était arrêtée
        } else if (!shouldPlay && player.isPlaying()) {
            player.pause();
        }
    }
}
