package com.example.magicdefense.model;

// Bombe nucléaire du Scientifique (mage légendaire). Elle se recharge toute seule (≈ 25 s, jauge sur
// la case du mage), tombe au MILIEU du terrain puis explose : elle touche TOUS les ennemis visibles,
// avec de très gros dégâts (le boss n'en subit que 35 %).
// L'objet sert aussi à l'effet visuel : chute, flash blanc, champignon atomique, onde de choc.
public class Nuke {
    // ---------- Réglages ----------
    public static final int COOLDOWN = 1500;            // ≈ 25 s entre deux bombes (×0,92 niv. 6, ×0,8 niv. 7)
    // Dégâts de la bombe = dégâts de la fiole × ce multiplicateur (indice = niveau - 1, niveaux 1 à 7)
    static final float[] DAMAGE_MULT = {8f, 9f, 10f, 11f, 12f, 13f, 15f};
    public static final float BOSS_DAMAGE = 0.35f;      // le boss ne subit que 35 % des dégâts
    public static final int STUN = 30;                  // le souffle étourdit ≈ 0,5 s
    // Compétence « Réaction en chaîne » (niveau 5) et sa version BOOST « Apocalypse nucléaire » (sage)
    public static final int CHAIN_DURATION = 600;       // ≈ 10 s
    public static final int BOOST_DURATION = 900;       // ≈ 15 s
    public static final int CHAIN_SPEED = 2;            // la jauge se remplit 2× plus vite
    public static final int BOOST_SPEED = 3;            // 3× plus vite
    // Effet visuel
    public static final int FALL_FRAMES = 50;           // chute de la bombe ≈ 0,8 s
    public static final int BLAST_FRAMES = 120;         // flash, champignon et onde de choc ≈ 2 s
    public static final int SHAKE_FRAMES = 40;          // tremblement de l'écran

    public final float x;       // point d'impact (pixels)
    public final float y;
    public final float damage;  // dégâts sur chaque ennemi (avant le malus du boss)
    public final int level;     // niveau du Scientifique (taille du champignon)
    private int age = 0;

    public Nuke(float x, float y, float damage, int level) {
        this.x = x;
        this.y = y;
        this.damage = damage;
        this.level = level;
    }

    // Dégâts réellement infligés à cet ennemi (boss réduit)
    public static float damageOn(Enemy e, float damage) {
        return e.isBoss() ? damage * BOSS_DAMAGE : damage;
    }

    public boolean isFalling() {
        return age < FALL_FRAMES;
    }

    // Chute : 0 au largage, 1 à l'impact
    public float getFall() {
        return Math.min(1f, age / (float) FALL_FRAMES);
    }

    // Explosion : 0 à l'impact, 1 à la fin de l'effet
    public float getBlast() {
        return Math.max(0f, Math.min(1f, (age - FALL_FRAMES) / (float) BLAST_FRAMES));
    }

    public int getAge() {
        return age;
    }

    // Avance d'une image. Retourne true à l'image exacte de l'impact.
    public boolean update() {
        age++;
        return age == FALL_FRAMES;
    }

    public boolean isOver() {
        return age >= FALL_FRAMES + BLAST_FRAMES;
    }
}
