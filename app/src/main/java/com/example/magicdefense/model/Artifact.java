package com.example.magicdefense.model;

// Artefacts : 3 par élément (33 au total ; les ids 21 à 29 sont ceux de l'Ouvrier, de la Lumière et
// des Ténèbres, 30 à 32 ceux du Scientifique). On en gagne un en battant un boss de la campagne
// pour la première fois (niveaux 10, 20, 30...). Chaque mage peut en équiper un : il le
// spécialise, avec un avantage et un inconvénient.
public final class Artifact {
    public static final int PER_TYPE = 3;
    public static final int COUNT = Mage.TYPE_COUNT * PER_TYPE;
    public static final int NONE = -1;

    // Identifiant d'un artefact : type × 3 + numéro (0 à 2)
    public static int id(int type, int index) {
        return type * PER_TYPE + index;
    }

    public static int typeOf(int id) {
        return id / PER_TYPE;
    }

    public static final String[] NAMES = {
            "Baguette de corail", "Conque des marées", "Perle abyssale",          // Eau
            "Baguette de braise", "Cœur de volcan", "Grimoire incandescent",      // Feu
            "Fronde de granit", "Marteau du titan", "Racine ancienne",           // Terre
            "Baguette d'orage", "Prisme d'éclair", "Cristal de tempête",         // Foudre
            "Fiole corrosive", "Baguette venimeuse", "Crâne pestilent",          // Poison
            "Éventail des cieux", "Plume de griffon", "Corne des vents",         // Air
            "Bannière royale", "Bouclier d'argent", "Épée bénie",                // Invocateur
            "Masse du forgeron", "Truelle bénie", "Plan d'architecte",          // Ouvrier
            "Lanterne sacrée", "Prisme solaire", "Auréole",                     // Lumière
            "Faux du néant", "Grimoire interdit", "Œil du vide",                // Ténèbres
            "Uranium enrichi", "Fiole instable", "Ogive thermonucléaire",       // Scientifique
    };

    // Effet concret de chaque artefact, avec les vrais chiffres des tableaux ci-dessous.
    // Cadence : un délai ×0,6 entre deux tirs = tire 67 % plus vite ; un délai ×1,3 = tire 23 % moins vite.
    public static final String[] DESCRIPTIONS = {
            // Eau
            "Tire 67 % plus vite, mais −30 % de dégâts",
            "Zone d'éclaboussure +40 %, mais −15 % de dégâts",
            "+50 % de dégâts, mais tire 23 % moins vite",
            // Feu
            "Tire 82 % plus vite, mais −35 % de dégâts",
            "+70 % de dégâts, mais tire 29 % moins vite",
            "Pluie de feu 2× plus souvent, mais −10 % de dégâts",
            // Terre
            "1 projectile de plus par salve, mais −25 % de dégâts",
            "+60 % de dégâts, mais tire 26 % moins vite",
            "Séisme 2× plus souvent, mais tire 9 % moins vite",
            // Foudre
            "Tire 2× plus vite, mais −40 % de dégâts",
            "+50 % de dégâts, mais tire 20 % moins vite",
            "Tempête 2× plus souvent, mais −10 % de dégâts",
            // Poison
            "Poison +80 % de dégâts, mais coup direct −50 %",
            "Tire 67 % plus vite, mais poison −25 %",
            "Peste 2× plus souvent, mais poison −10 %",
            // Air
            "Recul +80 %, mais −30 % de dégâts",
            "Tire 82 % plus vite, mais recul −30 %",
            "+60 % de dégâts, mais recul −40 %",
            // Invocateur (les PV des chevaliers suivent aussi les dégâts du mage)
            "1 chevalier de plus, mais chevaliers −25 % de PV",
            "Chevaliers +80 % de PV, mais invoque 23 % moins vite",
            "Chevaliers +60 % de dégâts, mais PV seulement +28 %",
            // Ouvrier (les PV du mur suivent aussi les dégâts des marteaux)
            "Marteaux et mur +50 %, mais lance 23 % moins vite",
            "Mur +44 % de PV, mais marteaux −20 % de dégâts",
            "Grand rempart 2× plus souvent, mais lance 13 % moins vite",
            // Lumière
            "Aura 50 % plus forte, mais rayons −30 % de dégâts",
            "Rayons +60 % de dégâts, mais tire 23 % moins vite",
            "Aube sacrée 2× plus souvent, mais rayons −10 %",
            // Ténèbres
            "Exécute sous 15 % de PV (au lieu de 10 %), mais −20 % de dégâts",
            "Zone d'ombre +40 %, mais −15 % de dégâts",
            "Éclipse 2× plus souvent, mais −10 % de dégâts",
            // Scientifique (la bombe suit aussi les dégâts des fioles)
            "Bombe 30 % plus fréquente, mais fioles et bombe −20 % de dégâts",
            "Fioles 33 % plus rapides, éclaboussure +50 %, mais bombe −30 %",
            "Bombe +60 % de dégâts, mais 20 % moins fréquente",
    };

    // Multiplicateurs (indice = id) : dégâts, délai entre deux tirs (< 1 = plus rapide), zone de l'Eau,
    // effet propre (poison, recul, PV des chevaliers), temps de recharge de la compétence ;
    // EXTRA : projectile (Terre) ou chevalier (Invocateur) en plus
    private static final float[] DAMAGE = {
            0.7f, 0.85f, 1.5f,
            0.65f, 1.7f, 0.9f,
            0.75f, 1.6f, 1f,
            0.6f, 1.5f, 0.9f,
            0.5f, 1f, 1f,
            0.7f, 1f, 1.6f,
            1f, 1f, 1.6f,
            1.5f, 0.8f, 1f,
            0.7f, 1.6f, 0.9f,
            0.8f, 0.85f, 0.9f,
            0.8f, 1f, 1f,
    };
    private static final float[] DELAY = {
            0.6f, 1f, 1.3f,
            0.55f, 1.4f, 1f,
            1f, 1.35f, 1.1f,
            0.5f, 1.25f, 1f,
            1f, 0.6f, 1f,
            1f, 0.55f, 1f,
            1f, 1.3f, 1f,
            1.3f, 1f, 1.15f,
            1f, 1.3f, 1f,
            1f, 1f, 1f,
            1f, 0.75f, 1f,       // Scientifique : Fiole instable
    };
    private static final float[] AOE = {
            1f, 1.4f, 1f,
            1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f,
            1f, 1f, 1f, 1f, 1f, 1f,
            1f, 1.4f, 1f,          // Ténèbres : Grimoire interdit
            1f, 1.5f, 1f,          // Scientifique : Fiole instable
    };
    private static final float[] EFFECT = {
            1f, 1f, 1f,
            1f, 1f, 1f,
            1f, 1f, 1f,
            1f, 1f, 1f,
            1.8f, 0.75f, 0.9f,   // Poison : dégâts du poison
            1.8f, 0.7f, 0.6f,    // Air : recul
            0.75f, 1.8f, 0.8f,   // Invocateur : PV des chevaliers
            1f, 1.8f, 1f,        // Ouvrier : PV du mur
            1.5f, 1f, 1f,        // Lumière : force de l'aura
            1.5f, 1f, 1f,        // Ténèbres : seuil d'exécution
            1f, 0.7f, 1.6f,      // Scientifique : dégâts de la bombe
    };
    private static final float[] SKILL_COOLDOWN = {
            1f, 1f, 1f,
            1f, 1f, 0.5f,
            1f, 1f, 0.5f,
            1f, 1f, 0.5f,
            1f, 1f, 0.5f,
            1f, 1f, 1f,
            1f, 1f, 1f,
            1f, 1f, 0.5f,
            1f, 1f, 0.5f,
            1f, 1f, 0.5f,
            1f, 1f, 1f,
    };
    private static final int[] EXTRA = {
            0, 0, 0, 0, 0, 0,
            1, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0,
            1, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0,
            0, 0, 0,
    };
    // Scientifique (ids 30 à 32) : temps de recharge de la bombe nucléaire
    // (×0,77 = 30 % plus fréquente ; ×1,25 = 20 % moins fréquente)
    private static final float[] NUKE_COOLDOWN = {0.77f, 1f, 1.25f};

    private Artifact() {
    }

    public static float damage(int id) { return id == NONE ? 1f : DAMAGE[id]; }
    public static float delay(int id) { return id == NONE ? 1f : DELAY[id]; }
    public static float aoe(int id) { return id == NONE ? 1f : AOE[id]; }
    public static float effect(int id) { return id == NONE ? 1f : EFFECT[id]; }
    public static float skillCooldown(int id) { return id == NONE ? 1f : SKILL_COOLDOWN[id]; }
    public static int extra(int id) { return id == NONE ? 0 : EXTRA[id]; }

    public static float nukeCooldown(int id) {
        return id == NONE || typeOf(id) != Mage.SCIENTIST ? 1f : NUKE_COOLDOWN[id - id(Mage.SCIENTIST, 0)];
    }
}
