package com.example.magicdefense.model;

// Un mage du damier. Sa position est donnée par sa case dans la grille.
// Spécificités des éléments :
//  - Eau        : zone d'effet autour de l'impact, dégâts plus faibles
//  - Feu        : dégâts qui grimpent fort à chaque fusion
//  - Terre      : autant de projectiles que son niveau
//  - Foudre     : tire très souvent, projectile très rapide
//  - Poison     : faibles dégâts directs, mais empoisonne (dégâts sur la durée)      [exclusif]
//  - Air        : rafale qui repousse l'ennemi vers la forêt                          [exclusif]
//  - Invocateur : invoque des chevaliers qui bloquent les gobelins devant le château [exclusif]
// Au niveau 5, chaque mage a une compétence active (toucher le mage), avec un temps de recharge.
// Un artefact équipé (gagné contre les boss) modifie ses caractéristiques.
public class Mage {
    public static final int WATER = 0;
    public static final int FIRE = 1;
    public static final int EARTH = 2;
    public static final int LIGHTNING = 3;
    public static final int POISON = 4;
    public static final int AIR = 5;
    public static final int SUMMONER = 6;
    public static final int TYPE_COUNT = 7;
    public static final int BASE_TYPES = 4; // les 4 premiers sont possédés dès le départ

    public static boolean isExclusive(int type) {
        return type >= BASE_TYPES;
    }

    // ---------- Équilibrage (indice = niveau - 1) ----------
    private static final float[][] DAMAGE = {
            {1f, 2f, 3f, 4f, 5f},           // Eau (sur chaque ennemi de la zone)
            {1f, 2.5f, 5f, 8.5f, 13f},      // Feu
            {1.2f, 1.4f, 1.6f, 1.8f, 2f},   // Terre (par projectile)
            {0.8f, 1.6f, 2.4f, 3.2f, 4f},   // Foudre
            {0.4f, 0.8f, 1.2f, 1.6f, 2f},   // Poison (coup direct, le poison fait le reste)
            {0.5f, 1f, 1.5f, 2f, 2.5f},     // Air
            {3f, 6f, 10f, 15f, 22f},        // Invocateur (base des PV et des coups des chevaliers)
    };
    private static final int[] FIRE_DELAY = {32, 50, 55, 26, 40, 45, 120};       // images entre deux tirs
    private static final float[] PROJECTILE_SPEED = {9, 8, 7, 15, 8, 11, 4};     // dp par image
    private static final float[] WATER_AOE = {100, 120, 140, 160, 180};          // rayon de la zone, en dp
    private static final float[] POISON_DPS = {1f, 2f, 3.2f, 4.6f, 6.2f};        // dégâts par seconde
    public static final int POISON_DURATION = 180;                               // ≈ 3 s
    private static final float[] AIR_KNOCKBACK = {20, 26, 32, 38, 45};           // recul en dp

    // Compétences actives (niveau 5) : nom, description, temps de recharge
    public static final String[] SKILL_NAMES = {"Raz-de-marée", "Pluie de feu", "Séisme", "Tempête",
            "Peste", "Tornade", "Appel aux armes"};
    public static final String[] SKILL_DESCRIPTIONS = {
            "Repousse et ralentit tous les gobelins", "Boules de feu sur 8 gobelins",
            "Étourdit et blesse tous les gobelins", "10 éclairs sur le terrain",
            "Empoisonne tous les gobelins", "Renvoie tous les gobelins vers la forêt",
            "Invoque 3 chevaliers d'élite"};
    public static final int SKILL_COOLDOWN = 1200;  // ≈ 20 s
    public static final int SKILL_LEVEL = 5;        // niveau de fusion qui débloque la compétence

    public int type;
    public int level = 1;      // niveau de fusion, pendant la partie (1 à 5)
    public final int rank;     // rang permanent de l'élément (amélioré avec les fragments)
    public final int artifact; // artefact équipé (Artifact.NONE si aucun)
    public int cooldown = 0;   // compte à rebours avant le prochain tir
    public int skillTimer = 0; // compte à rebours avant que la compétence soit prête (0 = prête)

    public Mage(int type) {
        this(type, 1);
    }

    public Mage(int type, int rank) {
        this(type, rank, Artifact.NONE);
    }

    public Mage(int type, int rank, int artifact) {
        this.type = type;
        this.rank = Math.max(1, rank);
        this.artifact = artifact >= 0 && artifact < Artifact.COUNT && Artifact.typeOf(artifact) == type
                ? artifact : Artifact.NONE;
    }

    private float rankMult() {
        return Progression.damageMultiplier(rank);
    }

    // Dégâts de base du niveau, augmentés par le rang permanent et modifiés par l'artefact
    public float getDamage() {
        return DAMAGE[type][level - 1] * rankMult() * Artifact.damage(artifact);
    }

    // Délai entre deux tirs, raccourci par le rang permanent (cadence plafonnée), modifié par l'artefact
    public int getFireDelay() {
        return Math.max(1, Math.round(FIRE_DELAY[type] * (1f - Progression.speedBonus(rank))
                * Artifact.delay(artifact)));
    }

    public float getProjectileSpeed() {
        return PROJECTILE_SPEED[type];
    }

    // Rayon de la zone d'effet en dp (0 = pas de zone)
    public float getAoeRadius() {
        return type == WATER ? WATER_AOE[level - 1] * Artifact.aoe(artifact) : 0;
    }

    // Nombre de projectiles par tir
    public int getShots() {
        return type == EARTH ? level + Artifact.extra(artifact) : 1;
    }

    // Poison : dégâts par seconde infligés pendant POISON_DURATION (0 pour les autres)
    public float getPoisonDps() {
        return type == POISON ? POISON_DPS[level - 1] * rankMult() * Artifact.effect(artifact) : 0;
    }

    // Air : recul en dp (0 pour les autres)
    public float getKnockback() {
        return type == AIR ? AIR_KNOCKBACK[level - 1] * Artifact.effect(artifact) : 0;
    }

    // ---------- Compétence active (niveau 5) ----------
    public boolean hasSkill() {
        return level >= SKILL_LEVEL;
    }

    public boolean isSkillReady() {
        return hasSkill() && skillTimer == 0;
    }

    public int getSkillCooldown() {
        return Math.max(60, Math.round(SKILL_COOLDOWN * Artifact.skillCooldown(artifact)));
    }

    // Avancement de la recharge : 0 juste après le lancement, 1 quand c'est prêt
    public float getSkillProgress() {
        return 1f - skillTimer / (float) getSkillCooldown();
    }

    // ---------- Chevaliers (Invocateur) ----------
    public int getMaxKnights() {
        return type == SUMMONER ? 1 + level / 2 + Artifact.extra(artifact) : 0;
    }

    public float getKnightHp() {
        return getDamage() * Knight.HP_PER_DAMAGE * Artifact.effect(artifact);
    }

    public float getKnightDamage() {
        return getDamage() * Knight.HIT_PER_DAMAGE;
    }
}
