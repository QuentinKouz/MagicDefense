package com.example.magicdefense.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.magicdefense.model.GameModel;
import com.example.magicdefense.model.Mage;
import com.example.magicdefense.model.Progression;

// Sauvegarde de la progression et des options (persiste après fermeture de l'appli)
public class SaveManager {
    private static final String PREFS_NAME = "magic_defense_save";
    private static final String KEY_UNLOCKED = "unlocked_level";
    private static final String KEY_STARS = "stars_";             // stars_1, stars_2...
    private static final String KEY_NIGHT_DISABLED = "night_mode_disabled";
    private static final String KEY_MUSIC = "music_enabled";



    // Progression permanente
    private static final String KEY_CHESTS = "chests";
    private static final String KEY_FRAGMENTS = "fragments_";     // fragments_0 (Eau)...
    private static final String KEY_RANK = "rank_";               // rank_0 (Eau)...
    private static final String KEY_BEST_ENDLESS = "best_endless_wave";
    private static final String KEY_GEMS = "gems";
    private static final String KEY_OWNED = "owned_";             // owned_4 (Poison)...
    private static final String KEY_TEAM = "team_";               // team_0 à team_3 (-1 = vide)
    private static final String KEY_LEFT_HANDED = "left_handed";

    private final SharedPreferences prefs;

    public SaveManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void load(GameModel model) {
        int unlocked = Math.max(1, prefs.getInt(KEY_UNLOCKED, 1)); // 1 si jamais sauvegardé
        int[] stars = new int[unlocked + 1];
        for (int lv = 1; lv <= unlocked; lv++) {
            stars[lv] = prefs.getInt(KEY_STARS + lv, 0);
        }
        boolean nightDisabled = prefs.getBoolean(KEY_NIGHT_DISABLED, false);
        model.loadProgress(unlocked, stars, nightDisabled);
        model.setMusicEnabled(prefs.getBoolean(KEY_MUSIC, true)); // musique activée la 1re fois

        model.setLeftHanded(prefs.getBoolean(KEY_LEFT_HANDED, false));

        int[] fragments = new int[Mage.TYPE_COUNT];
        int[] ranks = new int[Mage.TYPE_COUNT];
        boolean[] owned = new boolean[Mage.TYPE_COUNT];
        for (int t = 0; t < Mage.TYPE_COUNT; t++) {
            fragments[t] = prefs.getInt(KEY_FRAGMENTS + t, 0);
            ranks[t] = prefs.getInt(KEY_RANK + t, 1);
            owned[t] = prefs.getBoolean(KEY_OWNED + t, !Mage.isExclusive(t));
        }
        int[] team = new int[Progression.TEAM_SIZE];
        for (int i = 0; i < team.length; i++) {
            team[i] = prefs.getInt(KEY_TEAM + i, i); // par défaut : Eau, Feu, Terre, Foudre
        }
        model.getProgression().load(prefs.getInt(KEY_CHESTS, 0), prefs.getInt(KEY_GEMS, 0),
                fragments, ranks, owned, team, prefs.getInt(KEY_BEST_ENDLESS, 0));
    }

    public void save(GameModel model) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_UNLOCKED, model.getUnlockedLevel());
        for (int lv = 1; lv <= model.getUnlockedLevel(); lv++) {
            editor.putInt(KEY_STARS + lv, model.getBestStars(lv));
        }
        editor.putBoolean(KEY_NIGHT_DISABLED, model.isNightModeDisabled());
        editor.putBoolean(KEY_MUSIC, model.isMusicEnabled());

        Progression p = model.getProgression();
        editor.putInt(KEY_CHESTS, p.getChests());
        for (int t = 0; t < Mage.TYPE_COUNT; t++) {
            editor.putInt(KEY_FRAGMENTS + t, p.getFragments(t));
            editor.putInt(KEY_RANK + t, p.getRank(t));
            editor.putBoolean(KEY_OWNED + t, p.isOwned(t));
        }
        for (int i = 0; i < Progression.TEAM_SIZE; i++) {
            editor.putInt(KEY_TEAM + i, p.getTeamSlot(i));
        }
        editor.putInt(KEY_GEMS, p.getGems());
        editor.putInt(KEY_BEST_ENDLESS, p.getBestEndlessWave());
        editor.putBoolean(KEY_LEFT_HANDED, model.isLeftHanded());
        editor.apply(); // écrit en arrière-plan, sans bloquer le jeu
    }
}