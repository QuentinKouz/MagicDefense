package com.example.magicdefense.model;

// Éclair de la Foudre : relie l'embout du bâton à la cible pendant un court instant
public class Bolt {
    public static final int DURATION = 9; // ≈ 0,15 s

    public final float fromX;
    public final float fromY;
    public final float toX;
    public final float toY;
    public final int level;               // niveau du mage : l'éclair évolue avec lui
    private int timer = DURATION;

    public Bolt(float fromX, float fromY, float toX, float toY, int level) {
        this.fromX = fromX;
        this.fromY = fromY;
        this.toX = toX;
        this.toY = toY;
        this.level = level;
    }

    // 0 au début, 1 à la fin
    public float getProgress() {
        return 1 - timer / (float) DURATION;
    }

    // Retourne true quand l'effet est terminé
    public boolean update() {
        timer--;
        return timer <= 0;
    }
}
