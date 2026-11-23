package com.example.magicdefense.model;

// Succès : chacun suit une statistique et se débloque quand elle atteint un seuil.
// Les gemmes sont données tout de suite (un bandeau l'annonce).
//
// Succès « infinis » : chaque famille (gobelins tués, fusions...) a des paliers sans fin.
// Quand un palier est atteint, le suivant apparaît : « Chasseur de gobelins I », « II », « III »...
// avec un objectif plus haut et une récompense en gemmes plus grosse.
// Les succès uniques (posséder tous les mages, mage de niveau 5...) n'ont qu'un palier.
// La progression du joueur est un simple compteur de paliers obtenus par famille (Progression).
public final class Achievement {
    // Statistiques suivies (valeurs calculées par GameModel.statValue)
    public static final int STAT_LEVEL = 0;          // niveaux de campagne terminés
    public static final int STAT_KILLS = 1;          // gobelins tués (au total)
    public static final int STAT_MERGES = 2;         // fusions
    public static final int STAT_REACTIONS = 3;      // réactions élémentaires déclenchées
    public static final int STAT_BOSSES = 4;         // boss vaincus
    public static final int STAT_MAX_LEVEL_MAGE = 5; // un mage de niveau 5 obtenu (0 ou 1)
    public static final int STAT_ENDLESS = 6;        // meilleure vague du mode infini (toutes difficultés)
    public static final int STAT_INFERNAL = 7;       // meilleure vague en Infernal
    public static final int STAT_OWNED = 8;          // mages possédés
    public static final int STAT_CHESTS = 9;         // coffres ouverts
    public static final int STAT_PERFECT = 10;       // niveaux à 3 étoiles
    public static final int STAT_ARTIFACTS = 11;     // artefacts obtenus
    public static final int STAT_RANK = 12;          // meilleur rang d'un élément
    public static final int STAT_SKILLS = 13;        // compétences de mage lancées
    public static final int STAT_KNIGHTS = 14;       // chevaliers invoqués

    // ---------- Familles (indice = identifiant du succès) ----------
    public static final int LEVELS = 0;
    public static final int KILLS = 1;
    public static final int MERGES = 2;
    public static final int REACTIONS = 3;
    public static final int BOSSES = 4;
    public static final int ENDLESS = 5;
    public static final int INFERNAL = 6;
    public static final int CHESTS = 7;
    public static final int PERFECT = 8;
    public static final int RANK = 9;
    public static final int SKILLS = 10;
    public static final int KNIGHTS = 11;
    public static final int MAX_LEVEL_MAGE = 12; // uniques à partir d'ici
    public static final int OWNED = 13;
    public static final int ARTIFACTS = 14;

    public static final String[] NAMES = {
            "Défenseur du royaume", "Chasseur de gobelins", "Alchimiste", "Maître des éléments",
            "Régicide", "Endurant", "Enfer glacé", "Pilleur de coffres", "Perfectionniste",
            "Vétéran", "Virtuose", "Seigneur des chevaliers",
            "Grand mage", "Collectionneur", "Archéologue",
    };
    private static final int[] STATS = {
            STAT_LEVEL, STAT_KILLS, STAT_MERGES, STAT_REACTIONS,
            STAT_BOSSES, STAT_ENDLESS, STAT_INFERNAL, STAT_CHESTS, STAT_PERFECT,
            STAT_RANK, STAT_SKILLS, STAT_KNIGHTS,
            STAT_MAX_LEVEL_MAGE, STAT_OWNED, STAT_ARTIFACTS,
    };
    // Premiers paliers de chaque famille (les anciens succès en font partie)
    private static final long[][] TARGETS = {
            {1, 10, 25, 50},                                          // niveaux (+25 ensuite)
            {500, 1000, 2500, 5000, 10000, 25000, 50000, 100000},     // gobelins (×2,5 / ×2 ensuite)
            {100, 250, 500, 1000},                                    // fusions
            {500, 1000, 2500, 5000},                                  // réactions
            {1, 5, 10, 25, 50},                                       // boss
            {10, 25, 50},                                             // vague du mode infini (+25)
            {10, 20, 30},                                             // vague en Infernal (+10)
            {50, 100, 250, 500},                                      // coffres
            {10, 25, 50},                                             // niveaux à 3 étoiles (+25)
            {10, 20, 30},                                             // rang (+10, jusqu'au rang max)
            {50, 100, 250, 500},                                      // compétences
            {200, 500, 1000, 2500},                                   // chevaliers
            {1},                                                      // mage de niveau 5
            {Mage.TYPE_COUNT},                                        // tous les mages
            {5},                                                      // artefacts
    };
    // Après les premiers paliers : + pas fixe (> 0) ou suite « ronde » 1 - 2,5 - 5 - 10... (0)
    private static final long[] STEP = {25, 0, 0, 0, 0, 25, 10, 0, 25, 10, 0, 0, 0, 0, 0};
    // Gemmes des premiers paliers, puis +REWARD_STEP à chaque palier suivant
    private static final int[][] REWARDS = {
            {2, 5, 10, 20},
            {3, 5, 6, 8, 10, 15, 20, 25},
            {5, 8, 10, 12},
            {5, 8, 10, 12},
            {5, 8, 10, 15, 20},
            {5, 10, 20},
            {15, 20, 25},
            {8, 10, 12, 15},
            {8, 12, 16},
            {5, 8, 10},
            {5, 8, 10, 12},
            {8, 10, 12, 15},
            {3},
            {10},
            {10},
    };
    public static final int REWARD_STEP = 5;
    // Nombre de paliers (0 = sans fin) : les succès uniques n'en ont qu'un ; le rang s'arrête au rang max
    private static final int[] MAX_TIERS = {0, 0, 0, 0, 0, 0, 0, 0, 0, Progression.MAX_RANK / 10, 0, 0, 1, 1, 1};

    public static final int COUNT = NAMES.length;

    // ---------- Anciennes sauvegardes : 22 succès uniques (booléens), convertis en paliers ----------
    private static final int[] LEGACY_FAMILY = {
            LEVELS, LEVELS, LEVELS, LEVELS,
            KILLS, KILLS, KILLS,
            MERGES, REACTIONS,
            BOSSES, MAX_LEVEL_MAGE,
            ENDLESS, ENDLESS, ENDLESS, INFERNAL,
            OWNED, CHESTS, PERFECT,
            ARTIFACTS, RANK, SKILLS, KNIGHTS,
    };
    private static final long[] LEGACY_TARGET = {
            1, 10, 25, 50,
            500, 5000, 25000,
            100, 500,
            1, 1,
            10, 25, 50, 10,
            10, 50, 10,
            5, 10, 50, 200,
    };
    public static final int LEGACY_COUNT = LEGACY_FAMILY.length;

    private Achievement() {
    }

    public static int stat(int family) { return STATS[family]; }

    // Succès à paliers sans fin (sinon : unique)
    public static boolean isTiered(int family) { return MAX_TIERS[family] != 1; }

    // Nombre maximal de paliers (Integer.MAX_VALUE = sans fin)
    public static int maxTiers(int family) {
        return MAX_TIERS[family] == 0 ? Integer.MAX_VALUE : MAX_TIERS[family];
    }

    // Objectif du palier `tier` (0 = premier palier)
    public static long target(int family, int tier) {
        long[] first = TARGETS[family];
        if (tier < first.length) {
            return first[Math.max(0, tier)];
        }
        long v = first[first.length - 1];
        for (int k = first.length; k <= tier; k++) {
            v = STEP[family] > 0 ? v + STEP[family] : nextRound(v);
        }
        return v;
    }

    // Gemmes du palier `tier`
    public static int reward(int family, int tier) {
        int[] first = REWARDS[family];
        if (tier < first.length) {
            return first[Math.max(0, tier)];
        }
        return first[first.length - 1] + REWARD_STEP * (tier - first.length + 1);
    }

    // Nombre « rond » suivant : 1, 2, 5, 10, 25, 50, 100, 250, 500, 1000, 2500...
    static long nextRound(long n) {
        for (long p = 1; p <= Long.MAX_VALUE / 100; p *= 10) {
            long[] candidates = {p, p * 5 / 2, p * 5};
            for (long c : candidates) {
                if (c > n) {
                    return c;
                }
            }
        }
        return Long.MAX_VALUE;
    }

    // Paliers dont l'objectif est atteint avec cette valeur
    public static int tierFor(int family, long value) {
        int tier = 0;
        int max = maxTiers(family);
        while (tier < max && tier < 10000 && value >= target(family, tier)) {
            tier++;
        }
        return tier;
    }

    // Nom du palier : « Chasseur de gobelins III » (les uniques gardent leur nom)
    public static String name(int family, int tier) {
        return isTiered(family) ? NAMES[family] + " " + Roman.of(tier + 1) : NAMES[family];
    }

    // Description lisible du palier
    public static String description(int family, int tier) {
        long t = target(family, tier);
        String n = format(t);
        switch (STATS[family]) {
            case STAT_LEVEL: return "Terminer le niveau " + n;
            case STAT_KILLS: return "Tuer " + n + " gobelins";
            case STAT_MERGES: return "Fusionner " + n + " fois";
            case STAT_REACTIONS: return "Déclencher " + n + " réactions";
            case STAT_BOSSES: return t == 1 ? "Vaincre un boss" : "Vaincre " + n + " boss";
            case STAT_MAX_LEVEL_MAGE: return "Obtenir un mage de niveau 5";
            case STAT_ENDLESS: return "Atteindre la vague " + n + " en mode infini";
            case STAT_INFERNAL: return "Atteindre la vague " + n + " en Infernal";
            case STAT_OWNED: return "Posséder les " + n + " mages";
            case STAT_CHESTS: return "Ouvrir " + n + " coffres";
            case STAT_PERFECT: return "3 étoiles sur " + n + " niveaux";
            case STAT_ARTIFACTS: return "Obtenir " + n + " artefacts";
            case STAT_RANK: return "Monter un mage au rang " + n;
            case STAT_SKILLS: return "Lancer " + n + " compétences";
            case STAT_KNIGHTS: return "Invoquer " + n + " chevaliers";
            default: return "";
        }
    }

    // 25000 → « 25 000 » (espace insécable entre les milliers)
    public static String format(long n) {
        String s = String.valueOf(Math.abs(n));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) {
                sb.append(' ');
            }
            sb.append(s.charAt(i));
        }
        return (n < 0 ? "-" : "") + sb;
    }

    // Conversion d'une ancienne sauvegarde (succès uniques obtenus) en paliers par famille :
    // chaque famille repart du plus haut palier déjà mérité, sans redonner les gemmes déjà reçues.
    public static int[] tiersFromLegacy(boolean[] done) {
        int[] tiers = new int[COUNT];
        for (int i = 0; i < LEGACY_COUNT && done != null && i < done.length; i++) {
            if (!done[i]) {
                continue;
            }
            int f = LEGACY_FAMILY[i];
            int tier = isTiered(f) ? tierFor(f, LEGACY_TARGET[i]) : 1;
            tiers[f] = Math.max(tiers[f], tier);
        }
        return tiers;
    }
}
