package com.example.magicdefense.view;

import android.content.Context;
import android.os.Build;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;

// Retours haptiques (vibrations) du jeu
public class Haptics {
    private final Vibrator vibrator;

    public Haptics(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 et plus
            VibratorManager manager =
                    (VibratorManager) context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            vibrator = (manager != null) ? manager.getDefaultVibrator() : null;
        } else {
            vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        }
    }

    private boolean canVibrate() {
        return vibrator != null && vibrator.hasVibrator();
    }

    // Joue une vibration en précisant à Android que c'est une vibration de jeu.
    // Sans ça, Android classe les vibrations courtes comme "retour tactile" (TOUCH)
    // et les ignore si la vibration au toucher est désactivée sur le téléphone.
    private void play(VibrationEffect effect) {
        if (!canVibrate()) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 et plus
            vibrator.vibrate(effect,
                    VibrationAttributes.createForUsage(VibrationAttributes.USAGE_MEDIA));
        } else {
            vibrator.vibrate(effect);
        }
    }

    // Impulsion pendant la secousse : de plus en plus forte avec la progression (0 → 1)
    public void shakePulse(float progress) {
        int amplitude = (int) (100 + 155 * progress); // de 100 à 255
        play(VibrationEffect.createOneShot(40, amplitude));
    }

    // Fusion : double tic, le second plus fort
    public void merge() {
        long[] timings = {0, 20, 60, 30};     // attente, vibre, attente, vibre (ms)
        int[] amplitudes = {0, 150, 0, 255};  // force de 0 à 255
        play(VibrationEffect.createWaveform(timings, amplitudes, -1));
    }

    // Château touché : un coup sourd et long
    public void castleHit() {
        play(VibrationEffect.createOneShot(200, 255));
    }

    // Ultime : trois impulsions, la dernière longue
    public void ultimate() {
        long[] timings = {0, 80, 60, 80, 60, 250};
        int[] amplitudes = {0, 255, 0, 255, 0, 255};
        play(VibrationEffect.createWaveform(timings, amplitudes, -1));
    }
}