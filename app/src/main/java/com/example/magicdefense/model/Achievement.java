package com.example.magicdefense.model;

// Succès : chacun suit une statistique et se débloque quand elle atteint un seuil.
// Les gemmes sont données tout de suite (un bandeau l'annonce).
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

    public static final String[] NAMES = {
            "Premier pas", "Défenseur", "Gardien du royaume", "Légende",
            "Chasseur de gobelins", "Fléau des gobelins", "Exterminateur",
            "Alchimiste", "Maître des éléments",
            "Régicide", "Grand mage",
            "Endurant", "Inarrêtable", "Éternel", "Enfer glacé",
            "Collectionneur", "Pilleur de coffres", "Perfectionniste",
            "Archéologue", "Vétéran", "Virtuose", "Seigneur des chevaliers",
    };
    private static final int[] STATS = {
            STAT_LEVEL, STAT_LEVEL, STAT_LEVEL, STAT_LEVEL,
            STAT_KILLS, STAT_KILLS, STAT_KILLS,
            STAT_MERGES, STAT_REACTIONS,
            STAT_BOSSES, STAT_MAX_LEVEL_MAGE,
            STAT_ENDLESS, STAT_ENDLESS, STAT_ENDLESS, STAT_INFERNAL,
            STAT_OWNED, STAT_CHESTS, STAT_PERFECT,
            STAT_ARTIFACTS, STAT_RANK, STAT_SKILLS, STAT_KNIGHTS,
    };
    private static final int[] TARGETS = {
            1, 10, 25, 50,
            500, 5000, 25000,
            100, 500,
            1, 1,
            10, 25, 50, 10,
            Mage.TYPE_COUNT, 50, 10,
            5, 10, 50, 200,
    };
    private static final int[] REWARDS = {
            2, 5, 10, 20,
            3, 8, 15,
            5, 5,
            5, 3,
            5, 10, 20, 15,
            10, 8, 8,
            10, 5, 5, 8,
    };
    public static final int COUNT = NAMES.length;

    private Achievement() {
    }

    public static int stat(int id) { return STATS[id]; }
    public static int target(int id) { return TARGETS[id]; }
    public static int reward(int id) { return REWARDS[id]; }

    // Description lisible du succès
    public static String description(int id) {
        int t = TARGETS[id];
        switch (STATS[id]) {
            case STAT_LEVEL: return t == 1 ? "Terminer le niveau 1" : "Terminer le niveau " + t;
            case STAT_KILLS: return "Tuer " + t + " gobelins";
            case STAT_MERGES: return "Fusionner " + t + " fois";
            case STAT_REACTIONS: return "Déclencher " + t + " réactions";
            case STAT_BOSSES: return "Vaincre un boss";
            case STAT_MAX_LEVEL_MAGE: return "Obtenir un mage de niveau 5";
            case STAT_ENDLESS: return "Atteindre la vague " + t + " en mode infini";
            case STAT_INFERNAL: return "Atteindre la vague " + t + " en Infernal";
            case STAT_OWNED: return "Posséder les " + t + " mages";
            case STAT_CHESTS: return "Ouvrir " + t + " coffres";
            case STAT_PERFECT: return "3 étoiles sur " + t + " niveaux";
            case STAT_ARTIFACTS: return "Obtenir " + t + " artefacts";
            case STAT_RANK: return "Monter un mage au rang " + t;
            case STAT_SKILLS: return "Lancer " + t + " compétences";
            case STAT_KNIGHTS: return "Invoquer " + t + " chevaliers";
            default: return "";
        }
    }
}
