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
//  - Ouvrier    : construit un mur devant sa colonne, lance lentement des marteaux   [exclusif]
//  - Lumière    : aura qui renforce TOUS les autres mages ; rayons sur tous les ennemis [exclusif]
//  - Ténèbres   : exécute les gobelins sous 10 % de PV, grande zone qui ralentit     [exclusif]
//  - Scientifique : fioles lancées en arc (petite zone) et bombe nucléaire automatique sur tout
//                 le terrain, toutes les ≈ 25 s (voir Nuke)                         [exclusif]
// Au niveau 5, chaque mage a une compétence active (toucher le mage), avec un temps de recharge.
// Niveau 6 : archimage, nettement plus fort. Niveau 7 : SAGE (seulement si son élément porte une orbe
// de sagesse), beaucoup plus puissant, et sa compétence devient sa version BOOST.
// Un artefact équipé (gagné contre les boss) modifie ses caractéristiques.
public class Mage {
    public static final int WATER = 0;
    public static final int FIRE = 1;
    public static final int EARTH = 2;
    public static final int LIGHTNING = 3;
    public static final int POISON = 4;
    public static final int AIR = 5;
    public static final int SUMMONER = 6;
    public static final int WORKER = 7;
    public static final int LIGHT = 8;
    public static final int DARK = 9;
    public static final int SCIENTIST = 10;
    public static final int TYPE_COUNT = 11;
    public static final int BASE_TYPES = 4; // les 4 premiers sont possédés dès le départ

    public static boolean isExclusive(int type) {
        return type >= BASE_TYPES;
    }

    // ---------- Niveaux ----------
    public static final int SKILL_LEVEL = 5;        // niveau de fusion qui débloque la compétence
    public static final int ARCHMAGE_LEVEL = 6;     // archimage : nettement plus fort que le niveau 5
    public static final int SAGE_LEVEL = 7;         // sage : demande une orbe de sagesse sur son élément
    public static final int MAX_LEVEL = SAGE_LEVEL; // niveau maximal d'un mage en partie

    // ---------- Équilibrage (indice = niveau - 1, niveaux 1 à 7) ----------
    // Niveau 6 ≈ ×1,5 du niveau 5 ; niveau 7 (sage) ≈ ×1,6 du niveau 6 et tirs 20 % plus rapides.
    // Un nouvel élément peut ne donner que ses 5 premiers niveaux : les niveaux 6 et 7 sont alors
    // extrapolés (voir levelValue).
    private static final float[][] DAMAGE = {
            {1f, 2f, 3f, 4f, 5f, 7.5f, 12f},               // Eau (sur chaque ennemi de la zone)
            {1f, 2.5f, 5f, 8.5f, 13f, 20f, 32f},           // Feu
            {1.2f, 1.4f, 1.6f, 1.8f, 2f, 2.7f, 3.8f},    // Terre (par projectile : 6 puis 7 projectiles)
            {0.8f, 1.6f, 2.4f, 3.2f, 4f, 6f, 9.6f},        // Foudre
            {0.4f, 0.8f, 1.2f, 1.6f, 2f, 3f, 4.8f},        // Poison (coup direct, le poison fait le reste)
            {0.5f, 1f, 1.5f, 2f, 2.5f, 3.7f, 6f},          // Air
            {3f, 6f, 10f, 15f, 22f, 33f, 52f},             // Invocateur (base des PV et des coups des chevaliers)
            {2f, 4f, 7f, 11f, 16f, 24f, 38f},              // Ouvrier (marteau ; base des PV du mur)
            {0.5f, 1f, 1.6f, 2.3f, 3.2f, 4.8f, 7.6f},      // Lumière (sur CHAQUE ennemi visible)
            {1f, 2f, 3.2f, 4.6f, 6.2f, 9.3f, 15f},         // Ténèbres (sur chaque ennemi de la zone)
            {1.2f, 2.4f, 3.8f, 5.4f, 7.2f},                // Scientifique (fiole, sur chaque ennemi de la zone ;
                                                           // niveaux 6 et 7 extrapolés : 10.8f, 17.3f)
    };
    // Extrapolation des niveaux absents d'un tableau (×1,5 au niveau 6, ×2,4 au niveau 7)
    private static final float[] EXTRA_LEVEL_MULT = {1.5f, 2.4f};
    // Cadence : délai entre deux tirs multiplié selon le niveau (archimage −8 %, sage −20 %)
    private static final float[] LEVEL_DELAY_MULT = {1f, 1f, 1f, 1f, 1f, 0.92f, 0.8f};
    private static final int[] FIRE_DELAY = {32, 50, 55, 26, 40, 45, 120, 95, 60, 55, 60}; // images entre deux tirs
    private static final float[] PROJECTILE_SPEED = {9, 8, 7, 15, 8, 11, 4, 6, 20, 8, 7}; // dp par image
    private static final float[] WATER_AOE = {100, 120, 140, 160, 180, 205, 240};     // rayon de la zone, en dp
    private static final float[] POISON_DPS = {1f, 2f, 3.2f, 4.6f, 6.2f, 9.3f, 15f};  // dégâts par seconde
    public static final int POISON_DURATION = 180;                                    // ≈ 3 s
    private static final float[] AIR_KNOCKBACK = {20, 26, 32, 38, 45, 54, 66};        // recul en dp

    // Ouvrier : un mur par ouvrier, reconstruit après WALL_REBUILD images s'il est détruit
    public static final float WALL_HP_PER_DAMAGE = 25f;   // PV du mur = 25 × dégâts du marteau
    public static final int WALL_REBUILD = 300;           // ≈ 5 s
    // Lumière : aura (toujours plus forte avec le niveau), au plus 2 auras cumulées
    private static final float[] LIGHT_AURA = {0.10f, 0.13f, 0.16f, 0.20f, 0.25f, 0.30f, 0.38f}; // +% dégâts, −% délais
    public static final int MAX_LIGHT_AURAS = 2;
    public static final float MAX_AURA_BONUS = 0.6f;      // plafond (artefact compris)
    // Ténèbres : zone plus grande que celle de l'Eau, ralentissement, exécution
    private static final float[] DARK_AOE = {130, 155, 180, 205, 230, 260, 300};                  // rayon, en dp
    private static final float[] DARK_SLOW = {0.75f, 0.7f, 0.65f, 0.6f, 0.55f, 0.5f, 0.42f}; // vitesse pendant le ralenti
    public static final int DARK_SLOW_FRAMES = 120;                             // ≈ 2 s
    public static final float DARK_EXECUTE = 0.10f; // tue net sous 10 % des PV (jamais le boss)
    // Scientifique : petite éclaboussure de fiole (les réglages de la bombe sont dans Nuke)
    private static final float[] SCIENTIST_AOE = {55, 62, 70, 78, 86, 96, 110}; // rayon de la fiole, en dp

    // Compétences actives (niveau 5) : nom, description, temps de recharge
    public static final String[] SKILL_NAMES = {"Raz-de-marée", "Pluie de feu", "Séisme", "Tempête",
            "Peste", "Tornade", "Appel aux armes", "Grand rempart", "Aube sacrée", "Éclipse",
            "Réaction en chaîne"};
    public static final String[] SKILL_DESCRIPTIONS = {
            "Repousse et ralentit tous les gobelins", "Boules de feu sur 8 gobelins",
            "Étourdit et blesse tous les gobelins", "10 éclairs sur le terrain",
            "Empoisonne tous les gobelins", "Renvoie tous les gobelins vers la forêt",
            "Invoque 3 chevaliers d'élite",
            "Un mur renforcé devant chaque colonne",
            "Brûle et aveugle tous les gobelins, recharge les compétences",
            "Exécute les gobelins sous 25 % de PV, ralentit tous les autres",
            "Bombe nucléaire 2× plus rapide pendant 10 s"};
    public static final int SKILL_COOLDOWN = 1200;  // ≈ 20 s

    // Compétences BOOST (sage, niveau 7) : effets environ doublés, recharge plus courte
    public static final String[] BOOST_SKILL_NAMES = {"Déluge", "Envol du phénix", "Cataclysme",
            "Marteau du tonnerre", "Pandémie", "Ouragan", "Légion royale", "Citadelle", "Jugement céleste",
            "Nuit éternelle", "Apocalypse nucléaire"};
    public static final String[] BOOST_SKILL_DESCRIPTIONS = {
            "Repousse 2× plus loin, ralentit et blesse 2× plus",
            "16 boules de feu du phénix, plus brûlantes",
            "Étourdit 2× plus longtemps, dégâts doublés",
            "20 éclairs plus puissants sur le terrain",
            "Poison doublé, deux fois plus long",
            "Renvoie très loin vers la forêt et étourdit",
            "Invoque 5 chevaliers royaux (PV triplés)",
            "Murs 2× plus solides, deux fois plus longtemps",
            "Brûle 2× plus, aveugle longtemps, recharge presque tout",
            "Exécute sous 40 % de PV, ralentit fortement les autres",
            "Bombe nucléaire 3× plus rapide pendant 15 s"};
    public static final float BOOST_POWER = 2f;           // multiplicateur des effets en version BOOST
    public static final float BOOST_COOLDOWN_MULT = 0.6f; // recharge 40 % plus courte

    public int type;
    public int level = 1;      // niveau du mage, pendant la partie (1 à 7)
    public final int rank;     // rang permanent de l'élément (amélioré avec les fragments)
    public final int artifact; // artefact équipé (Artifact.NONE si aucun)
    public int cooldown = 0;   // compte à rebours avant le prochain tir
    public int skillTimer = 0; // compte à rebours avant que la compétence soit prête (0 = prête)
    public int wallTimer = 0;  // Ouvrier : compte à rebours avant de (re)construire son mur
    public int nukeTimer = 0;  // Scientifique : compte à rebours avant la prochaine bombe (0 = prête)
    public int chainTimer = 0; // Scientifique : > 0 tant que « Réaction en chaîne » accélère la bombe

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
        if (type == SCIENTIST) {
            nukeTimer = getNukeCooldown(); // première bombe après une recharge complète
        }
    }

    private float rankMult() {
        return Progression.damageMultiplier(rank);
    }

    // Valeur d'un tableau par niveau ; un niveau absent du tableau (nouvel élément décrit sur 5 niveaux)
    // est extrapolé à partir du dernier niveau donné
    static float levelValue(float[] table, int level) {
        int lv = Math.max(1, Math.min(MAX_LEVEL, level));
        if (lv <= table.length) {
            return table[lv - 1];
        }
        int extra = Math.min(EXTRA_LEVEL_MULT.length, lv - table.length) - 1;
        return table[table.length - 1] * EXTRA_LEVEL_MULT[extra];
    }

    // Sage (niveau 7) : look de grand sage, case dorée, compétence BOOST
    public boolean isSage() {
        return level >= SAGE_LEVEL;
    }

    // Dégâts de base du niveau, augmentés par le rang permanent et modifiés par l'artefact
    public float getDamage() {
        return levelValue(DAMAGE[type], level) * rankMult() * Artifact.damage(artifact);
    }

    // Délai entre deux tirs, raccourci par le rang permanent (cadence plafonnée), le niveau 6-7
    // et modifié par l'artefact
    public int getFireDelay() {
        return Math.max(1, Math.round(FIRE_DELAY[type] * (1f - Progression.speedBonus(rank))
                * Artifact.delay(artifact) * levelValue(LEVEL_DELAY_MULT, level)));
    }

    public float getProjectileSpeed() {
        return PROJECTILE_SPEED[type];
    }

    // Rayon de la zone d'effet en dp (0 = pas de zone) : Eau et Ténèbres
    public float getAoeRadius() {
        if (type == WATER) {
            return levelValue(WATER_AOE, level) * Artifact.aoe(artifact);
        }
        if (type == DARK) {
            return levelValue(DARK_AOE, level) * Artifact.aoe(artifact);
        }
        if (type == SCIENTIST) {
            return levelValue(SCIENTIST_AOE, level) * Artifact.aoe(artifact);
        }
        return 0;
    }

    // Nombre de projectiles par tir
    public int getShots() {
        return type == EARTH ? level + Artifact.extra(artifact) : 1;
    }

    // Poison : dégâts par seconde infligés pendant POISON_DURATION (0 pour les autres)
    public float getPoisonDps() {
        return type == POISON ? levelValue(POISON_DPS, level) * rankMult() * Artifact.effect(artifact) : 0;
    }

    // Air : recul en dp (0 pour les autres)
    public float getKnockback() {
        return type == AIR ? levelValue(AIR_KNOCKBACK, level) * Artifact.effect(artifact) : 0;
    }

    // ---------- Ouvrier ----------
    public float getWallHp() {
        return type == WORKER ? getDamage() * WALL_HP_PER_DAMAGE * Artifact.effect(artifact) : 0;
    }

    // ---------- Lumière ----------
    // Bonus de l'aura : +x % de dégâts et −x % de délai pour les autres mages (0 si pas Lumière)
    public float getAuraBonus() {
        return type == LIGHT ? levelValue(LIGHT_AURA, level) * Artifact.effect(artifact) : 0;
    }

    // ---------- Ténèbres ----------
    // Vitesse des ennemis touchés pendant DARK_SLOW_FRAMES (1 = pas de ralenti)
    public float getSlow() {
        return type == DARK ? levelValue(DARK_SLOW, level) : 1f;
    }

    // Seuil d'exécution, en part des PV max (0 si pas Ténèbres)
    public float getExecuteRatio() {
        return type == DARK ? DARK_EXECUTE * Artifact.effect(artifact) : 0;
    }

    // ---------- Scientifique : bombe nucléaire ----------
    // Recharge de la bombe (images), raccourcie aux niveaux 6-7 et modifiée par l'artefact
    public int getNukeCooldown() {
        return type == SCIENTIST ? Math.max(60, Math.round(Nuke.COOLDOWN * Artifact.nukeCooldown(artifact)
                * levelValue(LEVEL_DELAY_MULT, level))) : 0;
    }

    // Dégâts de la bombe sur chaque ennemi (avant le malus du boss), selon le niveau, le rang et l'artefact
    public float getNukeDamage() {
        return type == SCIENTIST ? getDamage() * levelValue(Nuke.DAMAGE_MULT, level) * Artifact.effect(artifact) : 0;
    }

    // Avancement de la recharge de la bombe : 0 juste après le largage, 1 quand elle est prête
    public float getNukeProgress() {
        return type == SCIENTIST ? Math.max(0f, Math.min(1f, 1f - nukeTimer / (float) getNukeCooldown())) : 0;
    }

    // Vitesse de recharge de la bombe : ×2 pendant « Réaction en chaîne » (×3 en version BOOST)
    public int getNukeSpeed() {
        if (type != SCIENTIST || chainTimer <= 0) {
            return 1;
        }
        return isSkillBoosted() ? Nuke.BOOST_SPEED : Nuke.CHAIN_SPEED;
    }

    // ---------- Compétence active (niveau 5) ----------
    public boolean hasSkill() {
        return level >= SKILL_LEVEL;
    }

    public boolean isSkillReady() {
        return hasSkill() && skillTimer == 0;
    }

    // Sage : la compétence devient sa version BOOST
    public boolean isSkillBoosted() {
        return isSage();
    }

    public String getSkillName() {
        return isSkillBoosted() ? BOOST_SKILL_NAMES[type] : SKILL_NAMES[type];
    }

    public String getSkillDescription() {
        return isSkillBoosted() ? BOOST_SKILL_DESCRIPTIONS[type] : SKILL_DESCRIPTIONS[type];
    }

    public int getSkillCooldown() {
        float boost = isSkillBoosted() ? BOOST_COOLDOWN_MULT : 1f;
        return Math.max(60, Math.round(SKILL_COOLDOWN * Artifact.skillCooldown(artifact) * boost));
    }

    // Avancement de la recharge : 0 juste après le lancement, 1 quand c'est prêt
    public float getSkillProgress() {
        return Math.max(0f, 1f - skillTimer / (float) getSkillCooldown());
    }

    // ---------- Chevaliers (Invocateur) ----------
    public int getMaxKnights() {
        // 1, 2, 2, 3, 3, 4 puis 5 au niveau 7 (sage)
        return type == SUMMONER ? 1 + level / 2 + (isSage() ? 1 : 0) + Artifact.extra(artifact) : 0;
    }

    public float getKnightHp() {
        return getDamage() * Knight.HP_PER_DAMAGE * Artifact.effect(artifact);
    }

    public float getKnightDamage() {
        return getDamage() * Knight.HIT_PER_DAMAGE;
    }
}
