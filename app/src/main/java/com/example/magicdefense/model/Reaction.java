package com.example.magicdefense.model;

// Réactions élémentaires : un ennemi marqué par un élément, touché par un second élément
// différent, déclenche une réaction. Chaque paire des 11 éléments a sa réaction (54 au total),
// sauf Lumière + Ténèbres qui s'annulent (l'Invocateur laisse une marque « arcanique »,
// l'Ouvrier une marque « chantier », le Scientifique une marque « laboratoire »). Un objet Reaction est aussi l'effet visuel affiché.
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
    // Ouvrier (marque « chantier »)
    public static final int CONCRETE = 21;      // Ouvrier + Eau : béton frais qui englue presque sur place
    public static final int FORGE = 22;         // Ouvrier + Feu : coup de forge, cible vulnérable
    public static final int FOUNDATIONS = 23;   // Ouvrier + Terre : onde sismique qui étourdit, murs réparés
    public static final int PILEDRIVER = 24;    // Ouvrier + Foudre : 3 frappes sur la cible, étourdissement
    public static final int TAR = 25;           // Ouvrier + Poison : goudron qui ralentit et empoisonne
    public static final int CATAPULT = 26;      // Ouvrier + Air : la cible est catapultée et retombe en zone
    public static final int BARRICADE = 27;     // Ouvrier + Invocateur : barricade temporaire devant elle
    public static final int SANCTUARY = 28;     // Ouvrier + Lumière : murs et chevaliers soignés
    public static final int DEMOLITION = 29;    // Ouvrier + Ténèbres : grosse explosion qui repousse
    // Lumière
    public static final int RAINBOW = 30;       // Lumière + Eau : ralentit et rend vulnérable
    public static final int SUPERNOVA = 31;     // Lumière + Feu : explosion de lumière brûlante
    public static final int PRISM = 32;         // Lumière + Terre : rayons réfractés vers 4 ennemis
    public static final int JUDGMENT = 33;      // Lumière + Foudre : colonne de lumière, étourdissement
    public static final int PURIFICATION = 34;  // Lumière + Poison : le poison restant explose d'un coup
    public static final int AURORA = 35;        // Lumière + Air : aveugle (étourdit) et repousse
    public static final int CRUSADE = 36;       // Lumière + Invocateur : un paladin rejoint la garde
    // Ténèbres (pas de réaction Lumière + Ténèbres)
    public static final int ABYSS = 37;         // Ténèbres + Eau : abysse qui ralentit et ronge
    public static final int BLACK_FLAMES = 38;  // Ténèbres + Feu : flammes noires (gros dégâts sur la durée)
    public static final int TOMB = 39;          // Ténèbres + Terre : tombeau, long étourdissement, vulnérable
    public static final int DARK_BOLT = 40;     // Ténèbres + Foudre : chaîne qui exécute les blessés
    public static final int NECROSIS = 41;      // Ténèbres + Poison : poison violent et vulnérabilité de zone
    public static final int BLACK_HOLE = 42;    // Ténèbres + Air : aspire fort et broie
    public static final int REAPING = 43;       // Ténèbres + Invocateur : moisson des âmes (exécution de zone)
    // Scientifique (marque « laboratoire »)
    public static final int SALINE = 44;        // Scientifique + Eau : solution saline, ralentit et ronge l'armure
    public static final int COMBUSTION = 45;    // Scientifique + Feu : explosion chimique et flammes au sol
    public static final int CALCINATION = 46;   // Scientifique + Terre : la roche cuit, dégâts et étourdissement
    public static final int ELECTROLYSIS = 47;  // Scientifique + Foudre : arcs vers 4 ennemis, cibles vulnérables
    public static final int MUTANT_TOXIN = 48;  // Scientifique + Poison : poison très violent qui se propage
    public static final int AEROSOL = 49;       // Scientifique + Air : grand nuage qui ralentit et empoisonne
    public static final int CLONING = 50;       // Scientifique + Invocateur : un clone du chevalier rejoint la garde
    public static final int DYNAMITE = 51;      // Scientifique + Ouvrier : grosse explosion qui repousse
    public static final int FISSION = 52;       // Scientifique + Lumière : l'atome se scinde, rebonds sur 2 ennemis
    public static final int ANTIMATTER = 53;    // Scientifique + Ténèbres : annihilation, exécute les blessés autour
    public static final int COUNT = 54;

    public static final String[] NAMES = {"ÉLECTROCUTION", "LAVE", "VAPEUR",
            "EXPLOSION TOXIQUE", "NUAGE TOXIQUE", "TEMPÊTE DE SABLE",
            "BOUE", "MARÉCAGE", "GEL", "GEYSER", "SURCHARGE", "EMBRASEMENT", "FEUX FOLLETS",
            "CRISTALLISATION", "ACIDE", "ÉBOULEMENT", "PARALYSIE", "ORAGE", "CHAÎNE SPECTRALE",
            "MALÉDICTION", "VORTEX",
            "BÉTON", "FORGE", "FONDATIONS", "MARTEAU-PILON", "GOUDRON", "CATAPULTE", "BARRICADE",
            "SANCTUAIRE", "DÉMOLITION",
            "ARC-EN-CIEL", "SUPERNOVA", "PRISME", "JUGEMENT", "PURIFICATION", "AURORE", "CROISADE",
            "ABYSSES", "FLAMMES NOIRES", "TOMBEAU", "ÉCLAIR NOIR", "NÉCROSE", "TROU NOIR", "MOISSON",
            "SOLUTION SALINE", "COMBUSTION", "CALCINATION", "ÉLECTROLYSE", "TOXINE MUTANTE", "AÉROSOL",
            "CLONAGE", "DYNAMITE", "FISSION", "ANTIMATIÈRE"};

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
            {Mage.WORKER, Mage.WATER},
            {Mage.WORKER, Mage.FIRE},
            {Mage.WORKER, Mage.EARTH},
            {Mage.WORKER, Mage.LIGHTNING},
            {Mage.WORKER, Mage.POISON},
            {Mage.WORKER, Mage.AIR},
            {Mage.WORKER, Mage.SUMMONER},
            {Mage.WORKER, Mage.LIGHT},
            {Mage.WORKER, Mage.DARK},
            {Mage.LIGHT, Mage.WATER},
            {Mage.LIGHT, Mage.FIRE},
            {Mage.LIGHT, Mage.EARTH},
            {Mage.LIGHT, Mage.LIGHTNING},
            {Mage.LIGHT, Mage.POISON},
            {Mage.LIGHT, Mage.AIR},
            {Mage.LIGHT, Mage.SUMMONER},
            {Mage.DARK, Mage.WATER},
            {Mage.DARK, Mage.FIRE},
            {Mage.DARK, Mage.EARTH},
            {Mage.DARK, Mage.LIGHTNING},
            {Mage.DARK, Mage.POISON},
            {Mage.DARK, Mage.AIR},
            {Mage.DARK, Mage.SUMMONER},
            {Mage.SCIENTIST, Mage.WATER},
            {Mage.SCIENTIST, Mage.FIRE},
            {Mage.SCIENTIST, Mage.EARTH},
            {Mage.SCIENTIST, Mage.LIGHTNING},
            {Mage.SCIENTIST, Mage.POISON},
            {Mage.SCIENTIST, Mage.AIR},
            {Mage.SCIENTIST, Mage.SUMMONER},
            {Mage.SCIENTIST, Mage.WORKER},
            {Mage.SCIENTIST, Mage.LIGHT},
            {Mage.SCIENTIST, Mage.DARK},
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

    // Ouvrier
    public static final float CONCRETE_RADIUS = 80;
    public static final int CONCRETE_DURATION = 240;
    public static final float CONCRETE_SLOW = 0.15f;     // vitesse × 0,15 dans le béton
    public static final float FORGE_DAMAGE = 2.5f;
    public static final float FOUNDATIONS_RADIUS = 100;
    public static final int FOUNDATIONS_STUN = 50;
    public static final float FOUNDATIONS_REPAIR = 0.3f;  // murs réparés de 30 % de leurs PV max
    public static final int PILEDRIVER_HITS = 3;
    public static final float PILEDRIVER_DAMAGE = 1.0f;  // par frappe
    public static final int PILEDRIVER_STUN = 40;
    public static final float TAR_RADIUS = 80;
    public static final int TAR_DURATION = 270;
    public static final float TAR_SLOW = 0.4f;
    public static final float TAR_DPS_FACTOR = 0.8f;     // poison appliqué dans le goudron
    public static final float CATAPULT_KNOCKBACK = 120;
    public static final float CATAPULT_RADIUS = 70;
    public static final float CATAPULT_DAMAGE = 1.5f;
    public static final float BARRICADE_HP_FACTOR = 15f;  // PV = 15 × le coup
    public static final int BARRICADE_LIFE = 480;        // ≈ 8 s
    public static final float SANCTUARY_HEAL = 0.5f;     // +50 % des PV max des murs et chevaliers
    public static final float DEMOLITION_RADIUS = 110;
    public static final float DEMOLITION_DAMAGE = 2.5f;
    public static final float DEMOLITION_KNOCKBACK = 30;
    // Lumière
    public static final float RAINBOW_RADIUS = 110;
    public static final float RAINBOW_SLOW = 0.5f;
    public static final int RAINBOW_DURATION = 150;
    public static final float SUPERNOVA_RADIUS = 120;
    public static final float SUPERNOVA_DAMAGE = 2.2f;
    public static final int PRISM_BEAMS = 4;
    public static final float PRISM_RANGE = 220;
    public static final float PRISM_DAMAGE = 1.3f;
    public static final float JUDGMENT_DAMAGE = 3f;
    public static final int JUDGMENT_STUN = 50;
    public static final float PURIFICATION_RADIUS = 100;
    public static final float PURIFICATION_FACTOR = 1.5f; // poison restant × 1,5, tout de suite
    public static final float AURORA_RADIUS = 120;
    public static final int AURORA_STUN = 40;
    public static final float AURORA_KNOCKBACK = 20;
    public static final float CRUSADE_HP_FACTOR = 6f;    // PV du paladin = 6 × le coup
    public static final float CRUSADE_HIT_FACTOR = 1f;
    // Ténèbres
    public static final float ABYSS_RADIUS = 85;
    public static final int ABYSS_DURATION = 240;
    public static final float ABYSS_SLOW = 0.35f;
    public static final float ABYSS_DPS_FACTOR = 0.8f;
    public static final float BLACK_FLAMES_RADIUS = 85;
    public static final int BLACK_FLAMES_DURATION = 180;
    public static final float BLACK_FLAMES_DPS_FACTOR = 2.2f;
    public static final int TOMB_STUN = 100;
    public static final int CHAIN_EXECUTE_JUMPS = 4;
    public static final float CHAIN_EXECUTE_RATIO = 0.15f; // l'éclair noir exécute sous 15 % de PV
    public static final float NECROSIS_RADIUS = 100;
    public static final float NECROSIS_DPS_FACTOR = 1.5f;
    public static final float BLACK_HOLE_RADIUS = 150;
    public static final float BLACK_HOLE_PULL = 0.8f;
    public static final float BLACK_HOLE_DAMAGE = 1.5f;
    public static final float REAPING_RADIUS = 120;
    public static final float REAPING_RATIO = 0.20f;     // exécute sous 20 % de PV (jamais le boss)
    // Scientifique
    public static final float SALINE_RADIUS = 100;
    public static final float SALINE_SLOW = 0.5f;
    public static final int SALINE_DURATION = 180;       // ralenti et vulnérable ≈ 3 s
    public static final float COMBUSTION_RADIUS = 110;
    public static final float COMBUSTION_DAMAGE = 2.5f;
    public static final float COMBUSTION_DPS_FACTOR = 1.0f; // flammes au sol
    public static final int COMBUSTION_DURATION = 120;
    public static final float CALCINATION_RADIUS = 80;
    public static final float CALCINATION_DAMAGE = 1.5f;
    public static final int CALCINATION_STUN = 60;
    public static final int ELECTROLYSIS_TARGETS = 4;
    public static final float ELECTROLYSIS_RANGE = 150;
    public static final float ELECTROLYSIS_DAMAGE = 1.2f;
    public static final int ELECTROLYSIS_VULNERABLE = 120; // ≈ 2 s
    public static final float MUTANT_TOXIN_RADIUS = 110;
    public static final float MUTANT_TOXIN_DPS_FACTOR = 2.0f;
    public static final int MUTANT_TOXIN_DURATION = 240;
    public static final float AEROSOL_RADIUS = 140;
    public static final float AEROSOL_SLOW = 0.6f;
    public static final int AEROSOL_DURATION = 150;
    public static final float AEROSOL_DPS_FACTOR = 0.8f;
    public static final float CLONING_HP_FACTOR = 5f;    // PV du clone = 5 × le coup
    public static final float CLONING_HIT_FACTOR = 1f;
    public static final float DYNAMITE_RADIUS = 110;
    public static final float DYNAMITE_DAMAGE = 3f;
    public static final float DYNAMITE_KNOCKBACK = 35;
    public static final float FISSION_DAMAGE = 3f;       // sur la cible
    public static final int FISSION_SPLITS = 2;
    public static final float FISSION_RANGE = 160;
    public static final float FISSION_SPLIT_DAMAGE = 1.5f; // sur chaque fragment
    public static final float ANTIMATTER_DAMAGE = 3f;
    public static final float ANTIMATTER_RADIUS = 90;
    public static final float ANTIMATTER_RATIO = 0.15f;  // annihile sous 15 % de PV (jamais le boss)

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
