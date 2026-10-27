package com.example.magicdefense.controller;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;

// Capteur de luminosité → mode nuit.
// Seuils RELATIFS à la lumière de la pièce : la nuit tombe quand la lumière chute fortement
// par rapport à d'habitude (capteur caché par le doigt, lumière éteinte), quelle que soit la pièce.
// Avec des seuils fixes (ex. 30 / 80 lux), une pièce un peu sombre restait bloquée en nuit.
public class LightSensor implements SensorEventListener {
    private static final String TAG = "LightSensor";

    private static final float NIGHT_RATIO = 0.25f;   // nuit : moins de 25 % de la lumière habituelle
    private static final float DAY_RATIO = 0.5f;      // jour : retour au-dessus de 50 % (hystérésis)
    private static final float DARK_LUX = 3f;         // noir complet : toujours la nuit
    private static final float MIN_REFERENCE_LUX = 10f;
    private static final long CONFIRM_DELAY_MS = 600; // le changement doit durer 0,6 s

    private final SensorManager sensorManager;
    private final Sensor lightSensor;

    private float lux = -1;          // dernière mesure (-1 : pas encore de mesure)
    private float reference = -1;    // lumière habituelle de la pièce
    private boolean night = false;
    private long candidateSince = 0; // début du changement en attente de confirmation (0 : aucun)
    private long lastLog = 0;

    public LightSensor(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        lightSensor = sensorManager != null ? sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) : null;
    }

    public boolean isAvailable() {
        return lightSensor != null;
    }

    public float getLux() {
        return lux;
    }

    // À appeler quand le jeu est visible
    public void start() {
        if (lightSensor != null) {
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    // À appeler quand le jeu passe en arrière-plan
    public void stop() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        candidateSince = 0;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        lux = event.values[0];

        // La référence suit la lumière de la pièce, mais pas les fortes baisses
        // (sinon cacher le capteur deviendrait "la normale" au bout de quelques secondes)
        if (!night) {
            if (reference < 0) {
                reference = Math.max(lux, MIN_REFERENCE_LUX);
            } else if (lux > reference) {
                reference += (lux - reference) * 0.3f;   // plus de lumière : on suit vite
            } else if (lux > reference * DAY_RATIO) {
                reference += (lux - reference) * 0.05f;  // légère baisse : on suit doucement
            }
            reference = Math.max(reference, MIN_REFERENCE_LUX);
        }

        long now = SystemClock.elapsedRealtime();
        if (now - lastLog > 1000) { // 1 ligne par seconde dans Logcat (filtre : LightSensor)
            Log.d(TAG, "lux=" + lux + " reference=" + reference + " night=" + night);
            lastLog = now;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // Appelée à chaque image : le capteur n'envoie une mesure que quand la lumière change,
    // donc la confirmation (0,6 s) est vérifiée ici avec l'horloge
    public boolean isNight() {
        if (lux < 0) {
            return false; // pas de capteur, ou pas encore de mesure : jour
        }
        boolean wantNight;
        if (night) {
            wantNight = lux < Math.max(DARK_LUX * 2, reference * DAY_RATIO);   // reste la nuit tant que c'est sombre
        } else {
            wantNight = lux < Math.max(DARK_LUX, reference * NIGHT_RATIO);     // devient la nuit si forte baisse
        }

        long now = SystemClock.elapsedRealtime();
        if (wantNight == night) {
            candidateSince = 0;              // pas de changement en cours
        } else if (candidateSince == 0) {
            candidateSince = now;            // un changement commence
        } else if (now - candidateSince >= CONFIRM_DELAY_MS) {
            night = wantNight;               // il a duré assez longtemps : confirmé
            candidateSince = 0;
            Log.d(TAG, (night ? "NUIT" : "JOUR") + " (lux=" + lux + ", reference=" + reference + ")");
        }
        return night;
    }
}