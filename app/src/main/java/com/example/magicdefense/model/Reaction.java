package com.example.magicdefense.model;

// Réactions élémentaires : un ennemi marqué par un élément, touché par un second élément
// différent, déclenche une réaction. Chacune des 21 paires des 7 éléments a sa réaction
// (l'Invocateur laisse une marque « arcanique »). Un objet Reaction est aussi l'effet visuel affiché.
public class Reaction {
    public static final int NONE = -1;
    // Réactions d'origine
    public static final int ELECTROCUTION = 0;  // Eau + Foudre : arcs électriques et étourdissement
    public static final int LAVA = 1;           // Terre + Feu : flaque de lave brûlante
    public static final int STEAM = 2;          // Eau + Feu : nuage de vapeur qui ralentit
    public static final int TOXIC_BLAST = 3;    // Poison + Feu : explosion de zone
    public static final int TOXIC_CLOUD = 4;    // Poison + Air : le poison se propage
    public static final int SANDSTORM = 5;      // Terre + Air : recul et étourdissement de zone
    // Toutes les autres paires
    public static final int MUD = 6;            // Eau + Terre : boue qui ralentit fortement
    public static final int BOG = 7;            // Eau + Poison : marécage qui empoisonne
    public static final int FREEZE = 8;         // Eau + Air : gel de zone
    public static final int GEYSER = 9;         // Eau + Invocateur : la cible est projetée, éclaboussures
    public static final int OVERLOAD = 10;      // Feu + Foudre : explosion qui repousse
    public static final int WILDFIRE = 11;      // Feu + Air : anneau de flammes
    public static final int WISPS = 12;         // Feu + Invocateur : feux follets qui chassent
    public static final int CRYSTALLIZE = 13;   // Terre + Foudre : grosse frappe, long étourdissement
    public static final int ACID = 14;          // Terre + Poison : dégâts doublés, cible vulnérable
    public static final int ROCKFALL = 15;      // Terre + Invocateur : éboulement de zone
    public static final int PARALYSIS = 16;     // Foudre + Poison : étourdissement, poison doublé
    public static final int THUNDERSTORM = 17;  // Foudre + Air : la foudre frappe 5 ennemis au hasard
    public static final int SPECTRAL_CHAIN = 18; // Foudre + Invocateur : arc qui rebondit 5 fois
    public static final int CURSE = 19;         // Poison + Invocateur : ennemis proches vulnérables
    public static final int VORTEX = 20;        // Air + Invocateur : aspire les ennemis, étourdit
    public static final int COUNT = 21;

    public static final String[] NAMES = {"ÉLECTROCUTION", "LAVE", "VAPEUR",
            "EXPLOSION TOXIQUE", "NUAGE TOXIQUE", "TEMPÊTE DE SABLE",
            "BOUE", "MARÉCAGE", "GEL", "GEYSER", "SURCHARGE", "EMBRASEMENT", "FEUX FOLLETS",
            "CRISTALLISATION", "ACIDE", "ÉBOULEMENT", "PARALYSIE", "ORAGE", "CHAÎNE SPECTRALE",
            "MALÉDICTION", "VORTEX"};

    // Les deux éléments de chaque réaction (l'ordre n'a pas d'importance)
    private static final int[][] PAIRS = {
            {Mage.WATER, Mage.LIGHTNING},
            {Mage.EARTH, Mage.FIRE},
            {Mage.WATER, Mage.FIRE},
            {Mage.POISON, Mage.FIRE},
            {Mage.POISON, Mage.AIR},
            {Mage.EARTH, Mage.AIR},
            {Mage.WATER, Mage.EARTH},
            {Mage.WATER, Mage.POISON},
            {Mage.WATER, Mage.AIR},
            {Mage.WATER, Mage.SUMMONER},
            {Mage.FIRE, Mage.LIGHTNING},
            {Mage.FIRE, Mage.AIR},
            {Mage.FIRE, Mage.SUMMONER},
            {Mage.EARTH, Mage.LIGHTNING},
            {Mage.EARTH, Mage.POISON},
            {Mage.EARTH, Mage.SUMMONER},
            {Mage.LIGHTNING, Mage.POISON},
            {Mage.LIGHTNING, Mage.AIR},
            {Mage.LIGHTNING, Mage.SUMMONER},
            {Mage.POISON, Mage.SUMMONER},
            {Mage.AIR, Mage.SUMMONER},
    };

    // ---------- Réglages (rayons en dp, durées en images) ----------
    public static final float ELECTRO_BONUS = 1.0f;      // dégâts en plus sur la cible (× le coup)
    public static final float ELECTRO_RANGE = 130;       // portée des arcs
    public static final int ELECTRO_TARGETS = 3;
    public static final int ELECTRO_STUN = 40;
    public static final float LAVA_RADIUS = 55;
    public static final int LAVA_DURATION = 240;         // ≈ 4 s
    public static final float LAVA_DPS_FACTOR = 1.2f;    // dégâts/s = 1,2 × le coup
    public static final float STEAM_RADIUS = 90;
    public static final int STEAM_DURATION = 150;
    public static final float STEAM_DAMAGE = 0.5f;
    public static final float STEAM_SLOW = 0.5f;         // vitesse × 0,5 dans la vapeur
    public static final float TOXIC_BLAST_RADIUS = 100;
    public static final float TOXIC_BLAST_DAMAGE = 2f;
    public static final float TOXIC_CLOUD_RADIUS = 110;
    public static final float SANDSTORM_RADIUS = 90;
    public static final float SANDSTORM_KNOCKBACK = 40;
    public static final int SANDSTORM_STUN = 20;

    public static final float MUD_RADIUS = 80;
    public static final int MUD_DURATION = 270;
    public static final float MUD_SLOW = 0.3f;           // vitesse × 0,3 dans la boue
    public static final float MUD_DAMAGE = 0.5f;
    public static final float BOG_RADIUS = 70;
    public static final int BOG_DURATION = 240;
    public static final float BOG_DPS_FACTOR = 1.0f;     // poison appliqué dans le marécage
    public static final float FREEZE_RADIUS = 85;
    public static final int FREEZE_STUN = 70;            // ≈ 1,2 s
    public static final float GEYSER_RADIUS = 70;
    public static final float GEYSER_KNOCKBACK = 70;
    public static final float GEYSER_DAMAGE = 1.5f;
    public static final float OVERLOAD_RADIUS = 90;
    public static final float OVERLOAD_DAMAGE = 1.5f;
    public static final float OVERLOAD_KNOCKBACK = 25;
    public static final float WILDFIRE_RADIUS = 95;
    public static final int WILDFIRE_DURATION = 150;
    public static final float WILDFIRE_DPS_FACTOR = 1.5f;
    public static final int WISPS_COUNT = 3;
    public static final float WISPS_RANGE = 220;
    public static final float WISPS_DAMAGE = 1.2f;
    public static final float CRYSTALLIZE_DAMAGE = 1.5f;
    public static final int CRYSTALLIZE_STUN = 80;
    public static final float ACID_DAMAGE = 1.0f;        // dégâts en plus (× le coup)
    public static final int VULNERABLE_DURATION = 240;   // ≈ 4 s
    public static final float VULNERABLE_MULT = 1.5f;    // dégâts subis × 1,5
    public static final float ROCKFALL_RADIUS = 85;
    public static final float ROCKFALL_DAMAGE = 2f;
    public static final int ROCKFALL_STUN = 15;
    public static final int PARALYSIS_STUN = 45;
    public static final int THUNDERSTORM_TARGETS = 5;
    public static final float THUNDERSTORM_DAMAGE = 1.2f;
    public static final int CHAIN_JUMPS = 5;
    public static final float CHAIN_RANGE = 140;
    public static final float CHAIN_DAMAGE = 1.0f;
    public static final float CURSE_RADIUS = 110;
    public static final int CURSE_DURATION = 180;
    public static final float VORTEX_RADIUS = 130;
    public static final float VORTEX_PULL = 0.6f;        // rapproche de 60 % vers le centre
    public static final int VORTEX_STUN = 25;

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

    // Tous les éléments laissent une marque (l'Invocateur : marque arcanique)
    public static boolean leavesMark(int element) {
        return element >= 0 && element < Mage.TYPE_COUNT;
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
