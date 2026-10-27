package com.example.magicdefense.model;

// Éclaboussure de l'Eau : montre la zone d'impact pendant un court instant
public class Splash {
    public static final int DURATION = 18; // ≈ 0,3 s

    public final float x;
    public final float y;
    public final float radius;             // rayon réel de la zone de dégâts
    public final int level;                // niveau du mage : tourbillon (4), déferlante (5)
    private int timer = DURATION;

    public Splash(float x, float y, float radius, int level) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.level = level;
    }

    // Avancement de l'effet : 0 au début, 1 à la fin
    public float getProgress() {
        return 1 - timer / (float) DURATION;
    }

    // Retourne true quand l'effet est terminé
    public boolean update() {
        timer--;
        return timer <= 0;
    }
}
