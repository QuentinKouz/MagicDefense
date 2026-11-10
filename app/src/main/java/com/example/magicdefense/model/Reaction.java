package com.example.magicdefense.model;

// Réactions élémentaires : un ennemi marqué par un élément, touché par un second élément
// compatible, déclenche une réaction. Un objet Reaction est aussi l'effet visuel affiché.
public class Reaction {
    public static final int NONE = -1;
    public static final int ELECTROCUTION = 0;  // Eau + Foudre : arcs électriques et étourdissement
    public static final int LAVA = 1;           // Terre + Feu : flaque de lave brûlante
    public static final int STEAM = 2;          // Eau + Feu : nuage de vapeur qui ralentit
    public static final int TOXIC_BLAST = 3;    // Poison + Feu : explosion de zone
    public static final int TOXIC_CLOUD = 4;    // Poison + Air : le poison se propage
    public static final int SANDSTORM = 5;      // Terre + Air : recul et étourdissement de zone
    public static final int COUNT = 6;

    public static final String[] NAMES = {"ÉLECTROCUTION", "LAVE", "VAPEUR",
            "EXPLOSION TOXIQUE", "NUAGE TOXIQUE", "TEMPÊTE DE SABLE"};

    // Les deux éléments de chaque réaction (l'ordre n'a pas d'importance)
    private static final int[][] PAIRS = {
            {Mage.WATER, Mage.LIGHTNING},
            {Mage.EARTH, Mage.FIRE},
            {Mage.WATER, Mage.FIRE},
            {Mage.POISON, Mage.FIRE},
            {Mage.POISON, Mage.AIR},
            {Mage.EARTH, Mage.AIR},
    };

    // ---------- Réglages ----------
    public static final float ELECTRO_BONUS = 1.0f;      // dégâts en plus sur la cible (× le coup)
    public static final float ELECTRO_RANGE = 130;       // portée des arcs, en dp
    public static final int ELECTRO_TARGETS = 3;
    public static final int ELECTRO_STUN = 40;
    public static final float LAVA_RADIUS = 55;          // dp
    public static final int LAVA_DURATION = 240;         // ≈ 4 s
    public static final float LAVA_DPS_FACTOR = 1.2f;    // dégâts/s = 1,2 × le coup
    public static final float STEAM_RADIUS = 90;
    public static final int STEAM_DURATION = 150;
    public static final float STEAM_DAMAGE = 0.5f;
    public static final float TOXIC_BLAST_RADIUS = 100;
    public static final float TOXIC_BLAST_DAMAGE = 2f;
    public static final float TOXIC_CLOUD_RADIUS = 110;
    public static final float SANDSTORM_RADIUS = 90;
    public static final float SANDSTORM_KNOCKBACK = 40;
    public static final int SANDSTORM_STUN = 20;

    // Réaction déclenchée par la marque `mark` puis l'élément `element`, ou NONE
    public static int between(int mark, int element) {
        if (mark == Enemy.NO_MARK || mark == element) {
            return NONE;
        }
        for (int r = 0; r < PAIRS.length; r++) {
            if ((PAIRS[r][0] == mark && PAIRS[r][1] == element)
                    || (PAIRS[r][1] == mark && PAIRS[r][0] == element)) {
                return r;
            }
        }
        return NONE;
    }

    // L'élément laisse-t-il une marque ? (l'esprit de l'Invocateur n'a pas d'élément)
    public static boolean leavesMark(int element) {
        return element != Mage.SUMMONER;
    }

    public static int[] pairOf(int reaction) {
        return PAIRS[reaction];
    }

    // ---------- Effet visuel ----------
    public static final int DURATION = 45; // ≈ 0,75 s

    public final float x;
    public final float y;
    public final int type;
    public final float radius; // taille de l'anneau, en pixels
    private int timer = DURATION;

    public Reaction(float x, float y, int type, float radius) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.radius = radius;
    }

    public float getProgress() {
        return 1 - timer / (float) DURATION;
    }

    public boolean update() {
        timer--;
        return timer <= 0;
    }
}