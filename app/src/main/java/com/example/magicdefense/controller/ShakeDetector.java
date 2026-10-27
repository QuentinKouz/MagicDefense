package com.example.magicdefense.controller;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

// Détecte une secousse forte ET maintenue avec l'accéléromètre
public class ShakeDetector implements SensorEventListener {

    // Ce qu'on appelle quand une secousse valide est détectée
    public interface OnShakeListener {
        void onShake();
    }

    // Réglages anti-déclenchement involontaire
    private static final float SHAKE_THRESHOLD_G = 2.2f;     // au-delà de 2,2 g : vraie secousse
    private static final long MAX_GAP_MS = 400;              // pause tolérée entre deux à-coups
    private static final long REQUIRED_DURATION_MS = 1500;   // durée de secousse nécessaire

    private final SensorManager sensorManager;
    private final Sensor accelerometer;
    private final OnShakeListener listener;

    private long shakeStart = -1;   // début de la secousse en cours (-1 = aucune)
    private long lastShakeTime = 0; // dernier à-coup fort détecté
    private long lastEventTime = 0; // dernière mesure reçue

    public ShakeDetector(Context context, OnShakeListener listener) {
        this.listener = listener;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        accelerometer = (sensorManager != null)
                ? sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
                : null; // téléphone sans accéléromètre : on ne plante pas
    }

    // À appeler quand le jeu est visible
    public void start() {
        shakeStart = -1;
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        }
    }

    // À appeler quand le jeu passe en arrière-plan (économise la batterie)
    public void stop() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        shakeStart = -1;
    }

    // Progression de la secousse en cours, entre 0 et 1 (servira au feedback visuel)
    public float getProgress() {
        if (shakeStart < 0) {
            return 0;
        }
        return Math.min(1f, (lastEventTime - shakeStart) / (float) REQUIRED_DURATION_MS);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // Accélération totale en "g" (téléphone immobile = 1 g, à cause de la gravité)
        float gx = event.values[0] / SensorManager.GRAVITY_EARTH;
        float gy = event.values[1] / SensorManager.GRAVITY_EARTH;
        float gz = event.values[2] / SensorManager.GRAVITY_EARTH;
        float gForce = (float) Math.sqrt(gx * gx + gy * gy + gz * gz);

        long now = event.timestamp / 1_000_000L; // nanosecondes → millisecondes
        lastEventTime = now;

        if (gForce > SHAKE_THRESHOLD_G) {
            // Nouvel à-coup : on démarre une secousse si aucune n'est en cours
            // (ou si la précédente s'est arrêtée trop longtemps)
            if (shakeStart < 0 || now - lastShakeTime > MAX_GAP_MS) {
                shakeStart = now;
            }
            lastShakeTime = now;

            // Secousse maintenue assez longtemps : déclenchement
            if (now - shakeStart >= REQUIRED_DURATION_MS) {
                shakeStart = -1;
                listener.onShake();
            }
        } else if (shakeStart >= 0 && now - lastShakeTime > MAX_GAP_MS) {
            shakeStart = -1; // le joueur a arrêté de secouer : on repart de zéro
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // inutile ici
    }
}