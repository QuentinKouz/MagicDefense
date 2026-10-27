package com.example.magicdefense.model;

// Un mage du damier. Sa position est donnée par sa case dans la grille.
// Spécificités des éléments :
//  - Eau    : zone d'effet autour de l'impact, dégâts plus faibles
//  - Feu    : dégâts qui grimpent fort à chaque fusion
//  - Terre  : autant de projectiles que son niveau
//  - Foudre : tire très souvent, projectile très rapide
public class Mage {
    public static final int WATER = 0;
    public static final int FIRE = 1;
    public static final int EARTH = 2;
    public static final int LIGHTNING = 3;
    public static final int TYPE_COUNT = 4;

    // ---------- Équilibrage (indice = niveau - 1) ----------
    private static final float[][] DAMAGE = {
            {1f, 2f, 3f, 4f, 5f},           // Eau (sur chaque ennemi de la zone)
            {1f, 2.5f, 5f, 8.5f, 13f},      // Feu
            {1.2f, 1.4f, 1.6f, 1.8f, 2f},   // Terre (par projectile)
            {0.8f, 1.6f, 2.4f, 3.2f, 4f}    // Foudre
    };
    private static final int[] FIRE_DELAY = {32, 50, 55, 26};          // images entre deux tirs
    private static final float[] PROJECTILE_SPEED = {9, 8, 7, 15};     // dp par image
    private static final float[] WATER_AOE = {100, 120, 140, 160, 180}; // rayon de la zone, en dp

    public int type;
    public int level = 1;
    public int cooldown = 0; // compte à rebours avant le prochain tir

    public Mage(int type) {
        this.type = type;
    }

    public float getDamage() {
        return DAMAGE[type][level - 1];
    }

    public int getFireDelay() {
        return FIRE_DELAY[type];
    }

    public float getProjectileSpeed() {
        return PROJECTILE_SPEED[type];
    }

    // Rayon de la zone d'effet en dp (0 = pas de zone)
    public float getAoeRadius() {
        return type == WATER ? WATER_AOE[level - 1] : 0;
    }

    // Nombre de projectiles par tir
    public int getShots() {
        return type == EARTH ? level : 1;
    }
}