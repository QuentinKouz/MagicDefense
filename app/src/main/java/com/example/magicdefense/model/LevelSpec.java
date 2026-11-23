package com.example.magicdefense.model;

import java.util.Random;

// Description d'un niveau : nombre de vagues, composition des vagues, rythme d'apparition.
//  - Niveaux 1 à 6 : faits à la main (comme avant), uniquement des gobelins normaux.
//  - Niveaux 7 et + : générés à l'infini. Le hasard dépend du numéro du niveau,
//    donc un même niveau est toujours identique (on peut le rejouer pour les étoiles).
//  - Mode infini : vagues sans fin, de plus en plus variées.
public class LevelSpec {
    public static final int HANDMADE_LEVELS = 6;  // niveaux d'origine
    public static final int MAX_ENEMIES_PER_WAVE = 25;
    private static final int BASE_SPAWN_DELAY = 60; // ≈ 1 s entre deux ennemis

    // Profils des niveaux générés
    public static final int PROFILE_CLASSIC = 0;
    public static final int PROFILE_HORDE = 1;  // beaucoup de rapides
    public static final int PROFILE_SIEGE = 2;  // beaucoup de costauds
    public static final int PROFILE_MIXED = 3;  // un peu de tout
    public static final String[] PROFILE_NAMES = {"Classique", "Horde", "Siège", "Mêlée"};

    // Boss : dernière vague des niveaux 10, 20, 30... et toutes les 10 vagues en mode infini
    public static final int BOSS_EVERY = 10;

    // Biomes : un par page de la carte (6 niveaux) ; en mode infini, un nouveau toutes les 10 vagues
    public static final String[] BIOME_NAMES = {"Prairie", "Désert", "Toundra", "Marais", "Volcan", "Crépuscule"};
    public static final int BIOME_COUNT = BIOME_NAMES.length;
    public static final int ENDLESS_WAVES_PER_BIOME = 10;

    // Nuit : certains niveaux générés de la campagne se jouent de nuit (7, 11, 15, 19...),
    // et en mode infini, une vague sur trois tombe la nuit (3, 6, 9...)
    public static final int NIGHT_LEVEL_EVERY = 4;
    public static final int NIGHT_LEVEL_OFFSET = 3;
    public static final int ENDLESS_NIGHT_EVERY = 3;

    // Difficultés du mode infini
    public static final int EASY = 0;
    public static final int NORMAL = 1;
    public static final int HARD = 2;
    public static final int INFERNAL = 3;
    public static final int DIFFICULTY_COUNT = 4;
    public static final String[] DIFFICULTY_NAMES = {"Facile", "Normal", "Difficile", "Infernal"};
    private static final float[] DIFFICULTY_MULT = {0.6f, 1f, 1.5f, 2.2f};       // PV, dégâts, vitesse
    private static final float[] DIFFICULTY_SPEED = {0.9f, 1f, 1.1f, 1.2f};      // vitesse en plus
    private static final int[] DIFFICULTY_WAVES_PER_CHEST = {5, 3, 3, 2};         // 1 coffre toutes les N vagues
    private static final int[] DIFFICULTY_BONUS_GEMS = {0, 0, 1, 2};             // gemmes par coffre en plus

    // PV, dégâts et vitesse des gobelins par rapport au mode Normal
    public static float difficultyMultiplier(int difficulty) {
        return DIFFICULTY_MULT[clampDifficulty(difficulty)];
    }

    public static int wavesPerChest(int difficulty) {
        return DIFFICULTY_WAVES_PER_CHEST[clampDifficulty(difficulty)];
    }

    public static int bonusGemsPerChest(int difficulty) {
        return DIFFICULTY_BONUS_GEMS[clampDifficulty(difficulty)];
    }

    public static int clampDifficulty(int d) {
        return Math.max(0, Math.min(DIFFICULTY_COUNT - 1, d));
    }

    public final boolean endless;
    public final int level;     // numéro du niveau (1 en mode infini)
    public final int waves;     // nombre de vagues (0 = infini)
    public final int profile;
    public final int endlessDifficulty; // difficulté du mode infini (NORMAL en campagne)
    public final boolean nightLevel;    // niveau de campagne joué de nuit
    private final float fastBase, tankBase; // proportions au début du niveau
    private final float fastGrowth, tankGrowth; // augmentation par vague
    private final float shamanBase, shamanGrowth;
    private final int spawnDelay;

    private LevelSpec(boolean endless, int level, int waves, int profile,
                      float fastBase, float tankBase, float fastGrowth, float tankGrowth,
                      float shamanBase, float shamanGrowth, int spawnDelay) {
        this(endless, level, waves, profile, fastBase, tankBase, fastGrowth, tankGrowth, shamanBase, shamanGrowth,
                spawnDelay, NORMAL);
    }

    private LevelSpec(boolean endless, int level, int waves, int profile,
                      float fastBase, float tankBase, float fastGrowth, float tankGrowth,
                      float shamanBase, float shamanGrowth, int spawnDelay, int endlessDifficulty) {
        this.endlessDifficulty = clampDifficulty(endlessDifficulty);
        this.nightLevel = !endless && isNightLevel(level);
        this.endless = endless;
        this.level = level;
        this.waves = waves;
        this.profile = profile;
        this.fastBase = fastBase;
        this.tankBase = tankBase;
        this.fastGrowth = fastGrowth;
        this.tankGrowth = tankGrowth;
        this.shamanBase = shamanBase;
        this.shamanGrowth = shamanGrowth;
        this.spawnDelay = spawnDelay;
    }

    // Biome d'un niveau (page de la carte)
    public static int biomeForLevel(int lv) {
        return ((Math.max(1, lv) - 1) / 6) % BIOME_COUNT;
    }

    // Biome du mode infini à une vague donnée
    public static int biomeForEndlessWave(int wave) {
        return ((Math.max(1, wave) - 1) / ENDLESS_WAVES_PER_BIOME) % BIOME_COUNT;
    }

    // Passage dans le cycle des biomes (1 = premier tour, 2 = deuxième...)
    public static int biomeCycleForLevel(int lv) {
        return (Math.max(1, lv) - 1) / (6 * BIOME_COUNT) + 1;
    }

    public static int biomeCycleForEndlessWave(int wave) {
        return (Math.max(1, wave) - 1) / (ENDLESS_WAVES_PER_BIOME * BIOME_COUNT) + 1;
    }

    // Nom du biome avec son cycle en chiffres romains : « Volcan I », puis « Volcan II » au tour suivant
    public static String biomeTitleForLevel(int lv) {
        return BIOME_NAMES[biomeForLevel(lv)] + " " + Roman.of(biomeCycleForLevel(lv));
    }

    public static String biomeTitleForEndlessWave(int wave) {
        return BIOME_NAMES[biomeForEndlessWave(wave)] + " " + Roman.of(biomeCycleForEndlessWave(wave));
    }

    public int biome(int wave) {
        return endless ? biomeForEndlessWave(wave) : biomeForLevel(level);
    }

    // Un boss arrive-t-il à la fin de cette vague ?
    public boolean hasBoss(int wave) {
        if (endless) {
            return wave > 0 && wave % BOSS_EVERY == 0;
        }
        return level % BOSS_EVERY == 0 && wave == waves;
    }

    // Le niveau a-t-il un boss ? (pour la carte)
    public boolean hasBossLevel() {
        return !endless && level % BOSS_EVERY == 0;
    }

    // Niveau numéro lv (1, 2, 3... sans limite)
    public static LevelSpec forLevel(int lv) {
        lv = Math.max(1, lv);
        if (lv <= HANDMADE_LEVELS) {
            return new LevelSpec(false, lv, lv + 2, PROFILE_CLASSIC, 0, 0, 0, 0, 0, 0, BASE_SPAWN_DELAY);
        }
        // Graine fixe par niveau : toujours le même niveau généré
        Random r = new Random(lv * 7919L + 104729L);
        int beyond = lv - HANDMADE_LEVELS; // 1 pour le niveau 7
        int waves = 6 + r.nextInt(5);      // 6 à 10 vagues
        int profile = PROFILE_HORDE + r.nextInt(3);
        float pressure = Math.min(1f, beyond / 30f); // monte doucement jusqu'au niveau 36
        float fast = 0.08f + 0.17f * pressure;
        float tank = 0.04f + 0.11f * pressure;
        if (profile == PROFILE_HORDE) {
            fast *= 1.8f;
            tank *= 0.4f;
        } else if (profile == PROFILE_SIEGE) {
            fast *= 0.4f;
            tank *= 1.8f;
        }
        float shaman = 0.03f + 0.07f * pressure; // chamans invocateurs, de plus en plus fréquents
        int delay = Math.max(30, BASE_SPAWN_DELAY - beyond * 2);
        return new LevelSpec(false, lv, waves, profile, fast, tank, 0.01f, 0.006f, shaman, 0.004f, delay);
    }

    // Mode vague infinie (chamans à partir de la vague 7)
    public static LevelSpec endless() {
        return endless(NORMAL);
    }

    public static LevelSpec endless(int difficulty) {
        return new LevelSpec(true, 1, 0, PROFILE_MIXED, -0.06f, -0.08f, 0.02f, 0.015f,
                -0.06f, 0.01f, BASE_SPAWN_DELAY, difficulty);
    }

    // Niveau de campagne joué de nuit (seulement parmi les niveaux générés)
    public static boolean isNightLevel(int lv) {
        return lv > HANDMADE_LEVELS && lv % NIGHT_LEVEL_EVERY == NIGHT_LEVEL_OFFSET;
    }

    // Cette vague se joue-t-elle de nuit ? (en plus du capteur de luminosité)
    public boolean isNight(int wave) {
        if (endless) {
            return wave > 0 && wave % ENDLESS_NIGHT_EVERY == 0;
        }
        return nightLevel;
    }

    // Vitesse des gobelins en plus, selon la difficulté du mode infini
    public float speedMultiplier() {
        return endless ? DIFFICULTY_SPEED[endlessDifficulty] : 1f;
    }

    public boolean isGenerated() {
        return !endless && level > HANDMADE_LEVELS;
    }

    public String getProfileName() {
        return PROFILE_NAMES[profile];
    }

    // Probabilité qu'un gobelin de la vague soit rapide / costaud
    public float fastChance(int wave) {
        return clamp(fastBase + fastGrowth * (wave - 1), 0f, 0.45f);
    }

    public float tankChance(int wave) {
        return clamp(tankBase + tankGrowth * (wave - 1), 0f, 0.35f);
    }

    public float shamanChance(int wave) {
        return clamp(shamanBase + shamanGrowth * (wave - 1), 0f, 0.12f);
    }

    // Nombre de gobelins d'une vague : 5, 7, 9... plafonné (les PV prennent le relais)
    public int enemiesInWave(int wave) {
        return Math.min(3 + wave * 2, MAX_ENEMIES_PER_WAVE);
    }

    // Délai entre deux apparitions (en images) : raccourci en mode infini
    public int spawnDelay(int wave) {
        if (endless) {
            return Math.max(24, BASE_SPAWN_DELAY - wave);
        }
        return spawnDelay;
    }

    // Difficulté d'une vague : pilote les PV, la vitesse et les dégâts des gobelins
    public float difficulty(int wave) {
        if (endless) {
            // accélère doucement : vague 20 → 30, vague 40 → 80 (en Normal)
            return (wave + wave * wave / 40f) * DIFFICULTY_MULT[endlessDifficulty];
        }
        return wave * (1 + 0.75f * (level - 1)); // formule d'origine
    }

    // Tire le type d'un gobelin
    public int rollKind(int wave, Random random) {
        float roll = random.nextFloat();
        float tank = tankChance(wave);
        if (roll < tank) {
            return Enemy.TANK;
        }
        if (roll < tank + fastChance(wave)) {
            return Enemy.FAST;
        }
        if (roll < tank + fastChance(wave) + shamanChance(wave)) {
            return Enemy.SHAMAN;
        }
        return Enemy.NORMAL;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}