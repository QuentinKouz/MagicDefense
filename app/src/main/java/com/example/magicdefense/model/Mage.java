package com.example.magicdefense.model;

// Un mage du damier. Sa position est donnée par sa case dans la grille.
public class Mage {
    // Les 4 éléments
    public static final int WATER = 0;
    public static final int FIRE = 1;
    public static final int EARTH = 2;
    public static final int LIGHTNING = 3;
    public static final int TYPE_COUNT = 4;

    public int type;
    public int level = 1;
    public int fireDelay = 45; // images entre deux tirs (≈ 0,75 s)
    public int cooldown = 0;   // compte à rebours avant le prochain tir

    public Mage(int type) {
        this.type = type;
    }

    public int getDamage() {
        return level; // un mage niveau 2 fait 2 dégâts, etc.
    }
}
