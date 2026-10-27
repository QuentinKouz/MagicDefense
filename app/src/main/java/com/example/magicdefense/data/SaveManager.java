package com.example.magicdefense.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.magicdefense.model.GameModel;

// Sauvegarde de la progression et des options (persiste après fermeture de l'appli)
public class SaveManager {
    private static final String PREFS_NAME = "magic_defense_save";
    private static final String KEY_UNLOCKED = "unlocked_level";
    private static final String KEY_STARS = "stars_";             // stars_1, stars_2...
    private static final String KEY_NIGHT_DISABLED = "night_mode_disabled";
    private static final String KEY_MUSIC = "music_enabled";   // ← à ajouter

    private final SharedPreferences prefs;

    public SaveManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void load(GameModel model) {
        int unlocked = prefs.getInt(KEY_UNLOCKED, 1); // 1 si jamais sauvegardé
        int[] stars = new int[GameModel.MAX_LEVELS + 1];
        for (int lv = 1; lv <= GameModel.MAX_LEVELS; lv++) {
            stars[lv] = prefs.getInt(KEY_STARS + lv, 0);
        }
        boolean nightDisabled = prefs.getBoolean(KEY_NIGHT_DISABLED, false);
        model.loadProgress(unlocked, stars, nightDisabled);
        model.loadProgress(unlocked, stars, nightDisabled);
        model.setMusicEnabled(prefs.getBoolean(KEY_MUSIC, true)); // ← à ajouter (true : musique activée la 1re fois)
    }

    public void save(GameModel model) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_UNLOCKED, model.getUnlockedLevel());
        for (int lv = 1; lv <= GameModel.MAX_LEVELS; lv++) {
            editor.putInt(KEY_STARS + lv, model.getBestStars(lv));
        }
        editor.putBoolean(KEY_NIGHT_DISABLED, model.isNightModeDisabled());
        editor.putBoolean(KEY_MUSIC, model.isMusicEnabled());
        editor.apply(); // écrit en arrière-plan, sans bloquer le jeu
    }
}