package com.example.magicdefense.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.magicdefense.model.Achievement;
import com.example.magicdefense.model.Artifact;
import com.example.magicdefense.model.Chest;
import com.example.magicdefense.model.GameModel;
import com.example.magicdefense.model.LevelSpec;
import com.example.magicdefense.model.Mage;
import com.example.magicdefense.model.Progression;
import com.example.magicdefense.model.Ultimate;

// Sauvegarde de la progression et des options (persiste après fermeture de l'appli).
// Les SharedPreferences restent en place quand on installe une nouvelle version de l'APK par-dessus
// l'ancienne (même clé de signature) : rien n'est perdu à la mise à jour. Les clés ajoutées par une
// mise à jour ont toutes une valeur par défaut, donc une ancienne sauvegarde se charge sans problème.
public class SaveManager {
    private static final String PREFS_NAME = "magic_defense_save";
    private static final String KEY_SAVE_VERSION = "save_version";
    private static final int SAVE_VERSION = 3;
    private static final String KEY_UNLOCKED = "unlocked_level";
    private static final String KEY_STARS = "stars_";             // stars_1, stars_2...
    private static final String KEY_NIGHT_DISABLED = "night_mode_disabled";
    private static final String KEY_MUSIC = "music_enabled";

    // Progression permanente
    private static final String KEY_CHESTS = "chests";
    private static final String KEY_FRAGMENTS = "fragments_";     // fragments_0 (Eau)...
    private static final String KEY_RANK = "rank_";               // rank_0 (Eau)...
    private static final String KEY_BEST_ENDLESS = "best_endless_wave";       // record en Normal (V2)
    private static final String KEY_BEST_ENDLESS_DIFF = "best_endless_";      // best_endless_0 (Facile)...
    private static final String KEY_GEMS = "gems";
    private static final String KEY_OWNED = "owned_";             // owned_4 (Poison)...
    private static final String KEY_TEAM = "team_";               // team_0 à team_3 (-1 = vide)
    private static final String KEY_LEFT_HANDED = "left_handed";

    // Ajouts de la mise à jour
    private static final String KEY_ULTIMATE = "ultimate_";       // ultimate_1 (Rage)...
    private static final String KEY_ULTIMATE_EQUIPPED = "ultimate_equipped";
    private static final String KEY_SPEED2 = "speed2";
    private static final String KEY_SPEED3 = "speed3";
    private static final String KEY_CASTLE_HP = "castle_hp_level";
    private static final String KEY_CASTLE_RES = "castle_res_level";
    private static final String KEY_ARTIFACT = "artifact_";       // artifact_0 à artifact_20
    private static final String KEY_ARTIFACT_EQUIPPED = "artifact_equipped_"; // par élément
    private static final String KEY_ACHIEVEMENT = "achievement_";
    private static final String KEY_STAT_KILLS = "stat_kills";
    private static final String KEY_STAT_MERGES = "stat_merges";
    private static final String KEY_STAT_REACTIONS = "stat_reactions";
    private static final String KEY_STAT_BOSSES = "stat_bosses";
    private static final String KEY_STAT_SKILLS = "stat_skills";
    private static final String KEY_STAT_KNIGHTS = "stat_knights";
    private static final String KEY_STAT_MAX_MAGE = "stat_max_mage";
    private static final String KEY_STAT_CHESTS = "stat_chests_opened";
    private static final String KEY_SPEED = "game_speed";
    private static final String KEY_AUTO = "auto_mode";
    private static final String KEY_AUTO_OWNED = "auto_owned";
    private static final String KEY_VIBRATION = "vibration";
    private static final String KEY_ATTACK_FX = "attack_effects";   // effets des attaques (Non = minimal)
    private static final String KEY_SOUND_VOLUME = "sound_volume";  // volume interne des bruitages
    private static final String KEY_MUSIC_VOLUME = "music_volume";  // volume interne de la musique
    private static final String KEY_CHESTS_BIG = "chests_big";        // (KEY_CHESTS = petits coffres)
    private static final String KEY_CHESTS_MYSTERY = "chests_mystery";
    private static final String KEY_DAILY_DAY = "daily_day";
    private static final String KEY_CODE_USED = "code_used";          // 1er code (thomasleboss)
    private static final String KEY_CODE_USED_N = "code_used_";       // code_used_1, code_used_2... (autres codes)
    private static final String KEY_WISDOM_ORBS = "wisdomOrbs";
    private static final String KEY_CASTLE_GOLD = "castle_gold_level"; // trésor de départ
    private static final String KEY_ACHIEVEMENT_TIER = "achievement_tier_"; // paliers par famille de succès
    private static final String KEY_DIFFICULTY = "endless_difficulty";
    private static final String KEY_RUN = "run_in_progress";      // partie à reprendre (vide = aucune)
    private static final String KEY_WISDOM_ORB_ON = "wisdom_orb_on_";         // par type de mage
    private static final String KEY_WISDOM_ORB_EARNED = "wisdom_orb_earned_"; // par difficulté (vague 1000)

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
        model.setVibrationEnabled(prefs.getBoolean(KEY_VIBRATION, true));
        model.loadProgress(unlocked, stars);
        model.setMusicEnabled(prefs.getBoolean(KEY_MUSIC, true)); // musique activée la 1re fois
        model.setAttackEffectsEnabled(prefs.getBoolean(KEY_ATTACK_FX, true));
        model.setSoundVolume(prefs.getFloat(KEY_SOUND_VOLUME, 1f));
        model.setMusicVolume(prefs.getFloat(KEY_MUSIC_VOLUME, 1f));

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
        Progression p = model.getProgression();
        p.load(prefs.getInt(KEY_CHESTS, 0), prefs.getInt(KEY_GEMS, 0),
                fragments, ranks, owned, team, prefs.getInt(KEY_BEST_ENDLESS, 0));

        int[] records = new int[LevelSpec.DIFFICULTY_COUNT];
        for (int d = 0; d < records.length; d++) {
            records[d] = prefs.getInt(KEY_BEST_ENDLESS_DIFF + d, 0);
        }
        p.loadEndlessRecords(records);

        boolean[] ultimates = new boolean[Ultimate.COUNT];
        for (int i = 0; i < Ultimate.COUNT; i++) {
            ultimates[i] = prefs.getBoolean(KEY_ULTIMATE + i, i == Ultimate.STUN);
        }
        p.loadUltimates(ultimates, prefs.getInt(KEY_ULTIMATE_EQUIPPED, Ultimate.STUN));
        p.loadCastle(prefs.getInt(KEY_CASTLE_HP, 0), prefs.getInt(KEY_CASTLE_RES, 0),
                prefs.getBoolean(KEY_SPEED2, false), prefs.getBoolean(KEY_SPEED3, false));

        boolean[] artifacts = new boolean[Artifact.COUNT];
        for (int i = 0; i < Artifact.COUNT; i++) {
            artifacts[i] = prefs.getBoolean(KEY_ARTIFACT + i, false);
        }
        int[] equipped = new int[Mage.TYPE_COUNT];
        for (int t = 0; t < Mage.TYPE_COUNT; t++) {
            equipped[t] = prefs.getInt(KEY_ARTIFACT_EQUIPPED + t, Artifact.NONE);
        }
        p.loadArtifacts(artifacts, equipped);

        // Anciens succès (22 booléens) : convertis en paliers, puis remplacés par les paliers sauvegardés s'il y en a
        boolean[] done = new boolean[Achievement.LEGACY_COUNT];
        for (int i = 0; i < Achievement.LEGACY_COUNT; i++) {
            done[i] = prefs.getBoolean(KEY_ACHIEVEMENT + i, false);
        }
        p.loadStats(prefs.getLong(KEY_STAT_KILLS, 0), prefs.getInt(KEY_STAT_MERGES, 0),
                prefs.getInt(KEY_STAT_REACTIONS, 0), prefs.getInt(KEY_STAT_BOSSES, 0),
                prefs.getInt(KEY_STAT_SKILLS, 0), prefs.getInt(KEY_STAT_KNIGHTS, 0),
                prefs.getBoolean(KEY_STAT_MAX_MAGE, false), prefs.getInt(KEY_STAT_CHESTS, 0), done);
        int[] tiers = new int[Achievement.COUNT];
        for (int f = 0; f < Achievement.COUNT; f++) {
            tiers[f] = prefs.getInt(KEY_ACHIEVEMENT_TIER + f, -1); // -1 : pas encore sauvegardé en paliers
        }
        p.loadAchievementTiers(tiers);
        p.loadCastleGold(prefs.getInt(KEY_CASTLE_GOLD, 0));
        p.loadWisdomOrbs(prefs.getInt(KEY_WISDOM_ORBS, 0));

        model.setGameSpeed(prefs.getInt(KEY_SPEED, 1));
        p.loadAuto(prefs.getBoolean(KEY_AUTO_OWNED, false));
        p.loadChests(-1, prefs.getInt(KEY_CHESTS_BIG, 0), prefs.getInt(KEY_CHESTS_MYSTERY, 0),
                prefs.getLong(KEY_DAILY_DAY, -1), prefs.getBoolean(KEY_CODE_USED, false));
        boolean[] codes = new boolean[Progression.GIFT_CODES.length];
        for (int i = 1; i < codes.length; i++) {
            codes[i] = prefs.getBoolean(KEY_CODE_USED_N + i, false);
        }
        p.loadCodesUsed(codes);
        model.setAutoMode(prefs.getBoolean(KEY_AUTO, false));
        model.setEndlessDifficulty(prefs.getInt(KEY_DIFFICULTY, LevelSpec.NORMAL));

        // Orbes de sagesse posées sur les mages, difficultés déjà récompensées (réserve : plus haut)
        boolean[] orbOn = new boolean[Mage.TYPE_COUNT];
        for (int t = 0; t < orbOn.length; t++) {
            orbOn[t] = prefs.getBoolean(KEY_WISDOM_ORB_ON + t, false);
        }
        boolean[] orbEarned = new boolean[LevelSpec.DIFFICULTY_COUNT];
        for (int d = 0; d < orbEarned.length; d++) {
            orbEarned[d] = prefs.getBoolean(KEY_WISDOM_ORB_EARNED + d, false);
        }
        p.loadWisdomOrbs(orbOn, orbEarned);

        // Partie interrompue (appli fermée en plein niveau) : elle reprend en pause
        // (chargée après les orbes : un mage de niveau 7 demande une orbe sur son élément)
        model.importRun(parse(prefs.getString(KEY_RUN, "")));
    }

    public void save(GameModel model) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_SAVE_VERSION, SAVE_VERSION);
        editor.putInt(KEY_UNLOCKED, model.getUnlockedLevel());
        for (int lv = 1; lv <= model.getUnlockedLevel(); lv++) {
            editor.putInt(KEY_STARS + lv, model.getBestStars(lv));
        }
        editor.putBoolean(KEY_VIBRATION, model.isVibrationEnabled());
        editor.remove(KEY_NIGHT_DISABLED); // option retirée
        editor.putBoolean(KEY_MUSIC, model.isMusicEnabled());
        editor.putBoolean(KEY_ATTACK_FX, model.isAttackEffectsEnabled());
        editor.putFloat(KEY_SOUND_VOLUME, model.getSoundVolume());
        editor.putFloat(KEY_MUSIC_VOLUME, model.getMusicVolume());

        Progression p = model.getProgression();
        editor.putInt(KEY_CHESTS, p.getChests(Chest.SMALL));
        editor.putInt(KEY_CHESTS_BIG, p.getChests(Chest.BIG));
        editor.putInt(KEY_CHESTS_MYSTERY, p.getChests(Chest.MYSTERY));
        editor.putLong(KEY_DAILY_DAY, p.getLastDailyDay());
        editor.putBoolean(KEY_CODE_USED, p.isCodeUsed());
        for (int i = 1; i < Progression.GIFT_CODES.length; i++) {
            editor.putBoolean(KEY_CODE_USED_N + i, p.isCodeUsed(i));
        }
        editor.putInt(KEY_WISDOM_ORBS, p.getWisdomOrbs());
        editor.putInt(KEY_CASTLE_GOLD, p.getCastleGoldLevel());
        for (int t = 0; t < Mage.TYPE_COUNT; t++) {
            editor.putInt(KEY_FRAGMENTS + t, p.getFragments(t));
            editor.putInt(KEY_RANK + t, p.getRank(t));
            editor.putBoolean(KEY_OWNED + t, p.isOwned(t));
            editor.putInt(KEY_ARTIFACT_EQUIPPED + t, p.getEquippedArtifact(t));
        }
        for (int i = 0; i < Progression.TEAM_SIZE; i++) {
            editor.putInt(KEY_TEAM + i, p.getTeamSlot(i));
        }
        editor.putInt(KEY_GEMS, p.getGems());
        editor.putInt(KEY_BEST_ENDLESS, p.getBestEndlessWave(LevelSpec.NORMAL));
        for (int d = 0; d < LevelSpec.DIFFICULTY_COUNT; d++) {
            editor.putInt(KEY_BEST_ENDLESS_DIFF + d, p.getBestEndlessWave(d));
        }
        editor.putBoolean(KEY_LEFT_HANDED, model.isLeftHanded());

        for (int i = 0; i < Ultimate.COUNT; i++) {
            editor.putBoolean(KEY_ULTIMATE + i, p.ownsUltimate(i));
        }
        editor.putInt(KEY_ULTIMATE_EQUIPPED, p.getEquippedUltimate());
        editor.putBoolean(KEY_SPEED2, p.hasSpeed2());
        editor.putBoolean(KEY_SPEED3, p.hasSpeed3());
        editor.putInt(KEY_CASTLE_HP, p.getCastleHpLevel());
        editor.putInt(KEY_CASTLE_RES, p.getCastleResLevel());
        for (int i = 0; i < Artifact.COUNT; i++) {
            editor.putBoolean(KEY_ARTIFACT + i, p.ownsArtifact(i));
        }
        for (int f = 0; f < Achievement.COUNT; f++) {
            editor.putInt(KEY_ACHIEVEMENT_TIER + f, p.getAchievementTier(f));
        }
        // (les anciennes clés achievement_0 à 21 restent telles quelles : elles ne servent plus
        // qu'à convertir une sauvegarde qui n'a pas encore de paliers)
        editor.putLong(KEY_STAT_KILLS, p.getKills());
        editor.putInt(KEY_STAT_MERGES, p.getMerges());
        editor.putInt(KEY_STAT_REACTIONS, p.getReactions());
        editor.putInt(KEY_STAT_BOSSES, p.getBosses());
        editor.putInt(KEY_STAT_SKILLS, p.getSkills());
        editor.putInt(KEY_STAT_KNIGHTS, p.getKnights());
        editor.putBoolean(KEY_STAT_MAX_MAGE, p.hasMaxLevelMage());
        editor.putInt(KEY_STAT_CHESTS, p.getChestsOpened());
        editor.putInt(KEY_SPEED, model.getGameSpeed());
        editor.putBoolean(KEY_AUTO, model.isAutoMode());
        editor.putBoolean(KEY_AUTO_OWNED, p.hasAuto());
        editor.putInt(KEY_DIFFICULTY, model.getEndlessDifficulty());
        for (int t = 0; t < Mage.TYPE_COUNT; t++) {
            editor.putBoolean(KEY_WISDOM_ORB_ON + t, p.hasWisdomOrb(t));
        }
        for (int d = 0; d < LevelSpec.DIFFICULTY_COUNT; d++) {
            editor.putBoolean(KEY_WISDOM_ORB_EARNED + d, p.isWisdomOrbEarned(d));
        }

        if (model.hasRunInProgress()) {
            editor.putString(KEY_RUN, join(model.exportRun()));
        } else {
            editor.remove(KEY_RUN);
        }
        editor.apply(); // écrit en arrière-plan, sans bloquer le jeu
    }

    private static String join(int[] data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(data[i]);
        }
        return sb.toString();
    }

    private static int[] parse(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        String[] parts = s.split(",");
        int[] data = new int[parts.length];
        try {
            for (int i = 0; i < parts.length; i++) {
                data[i] = Integer.parseInt(parts[i].trim());
            }
        } catch (NumberFormatException e) {
            return null; // sauvegarde abîmée : on l'ignore
        }
        return data;
    }
}
