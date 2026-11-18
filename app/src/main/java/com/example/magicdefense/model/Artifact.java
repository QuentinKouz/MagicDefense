package com.example.magicdefense.model;

// Artefacts : 3 par élément (30 au total ; les ids 21 à 29 sont ceux des 3 derniers mages). On en gagne un en battant un boss de la campagne
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
    };

    public static final String[] DESCRIPTIONS = {
            "Cadence ++ · dégâts −30 %", "Zone +40 % · dégâts −15 %", "Dégâts +50 % · cadence −",
            "Cadence +++ · dégâts −35 %", "Dégâts +70 % · cadence −−", "Compétence 2× plus souvent · dégâts −10 %",
            "+1 projectile · dégâts −25 %", "Dégâts +60 % · cadence −−", "Compétence 2× plus souvent · cadence −",
            "Cadence +++ · dégâts −40 %", "Dégâts +50 % · cadence −", "Compétence 2× plus souvent · dégâts −10 %",
            "Poison +80 % · coup direct −50 %", "Cadence ++ · poison −25 %", "Compétence 2× plus souvent · poison −10 %",
            "Recul +80 % · dégâts −30 %", "Cadence +++ · recul −30 %", "Dégâts +60 % · recul −40 %",
            "+1 chevalier · chevaliers −25 % PV", "Chevaliers +80 % PV · invoque moins vite", "Chevaliers +60 % dégâts · −20 % PV",
            "Marteaux +50 % · cadence −", "Mur +80 % PV · marteaux −20 %", "Compétence 2× plus souvent · cadence −",
            "Aura +50 % · rayons −30 %", "Rayons +60 % · cadence −", "Compétence 2× plus souvent · rayons −10 %",
            "Exécute sous 15 % · dégâts −20 %", "Zone +40 % · dégâts −15 %", "Compétence 2× plus souvent · dégâts −10 %",
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
    };
    private static final float[] AOE = {
            1f, 1.4f, 1f,
            1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f,
            1f, 1f, 1f, 1f, 1f, 1f,
            1f, 1.4f, 1f,          // Ténèbres : Grimoire interdit
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
    };
    private static final int[] EXTRA = {
            0, 0, 0, 0, 0, 0,
            1, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0,
            1, 0, 0,
            0, 0, 0, 0, 0, 0, 0, 0, 0,
    };

    private Artifact() {
    }

    public static float damage(int id) { return id == NONE ? 1f : DAMAGE[id]; }
    public static float delay(int id) { return id == NONE ? 1f : DELAY[id]; }
    public static float aoe(int id) { return id == NONE ? 1f : AOE[id]; }
    public static float effect(int id) { return id == NONE ? 1f : EFFECT[id]; }
    public static float skillCooldown(int id) { return id == NONE ? 1f : SKILL_COOLDOWN[id]; }
    public static int extra(int id) { return id == NONE ? 0 : EXTRA[id]; }
}
