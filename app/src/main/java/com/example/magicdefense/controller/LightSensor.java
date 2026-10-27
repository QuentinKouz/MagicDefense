package com.example.magicdefense.controller;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;

// Lit la luminosité ambiante (en lux)
public class LightSensor implements SensorEventListener {

    private final SensorManager sensorManager;
    private final Sensor lightSensor;
    private float lux = -1; // -1 = pas encore de mesure
    // Réglages anti-bruit
    private static final float NIGHT_LUX = 30;          // en dessous : il fait sombre
    private static final float DAY_LUX = 80;            // au-dessus : il fait jour
    private static final long CONFIRM_DELAY_MS = 600;  // le changement doit durer 0,6 s

    private boolean dark = false;   // lecture instantanée (avec hystérésis)
    private boolean night = false;  // mode nuit validé (après le délai)
    private long darkChangeTime = 0;

    public LightSensor(Context context) {
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        lightSensor = (sensorManager != null)
                ? sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
                : null; // pas de capteur : on ne plante pas
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
            sensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    // À appeler quand le jeu passe en arrière-plan
    public void stop() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        lux = event.values[0];

        // Hystérésis : le seuil dépend de l'état actuel
        boolean nowDark = dark ? lux < DAY_LUX : lux < NIGHT_LUX;

        if (nowDark != dark) {
            dark = nowDark;
            darkChangeTime = SystemClock.elapsedRealtime(); // on note quand ça a changé
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // inutile ici
    }

    // Mode nuit validé : la nouvelle luminosité doit durer CONFIRM_DELAY_MS
    public boolean isNight() {
        if (dark != night
                && SystemClock.elapsedRealtime() - darkChangeTime >= CONFIRM_DELAY_MS) {
            night = dark;
        }
        return night;
    }
}