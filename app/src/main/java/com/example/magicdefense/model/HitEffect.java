package com.example.magicdefense.model;

// Effet d'impact d'une attaque (explosion, éclats, étincelles)
public class HitEffect {
    public static final int DURATION = 14; // ≈ 0,25 s

    public final float x;
    public final float y;
    public final int type;                 // élément
    public final int level;                // niveau du mage : impact plus impressionnant
    private int timer = DURATION;

    public HitEffect(float x, float y, int type, int level) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.level = level;
    }

    public float getProgress() {
        return 1 - timer / (float) DURATION;
    }

    public boolean update() {
        timer--;
        return timer <= 0;
    }
}
