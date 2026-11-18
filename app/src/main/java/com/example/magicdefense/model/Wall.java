package com.example.magicdefense.model;

// Mur de pierre construit par un mage Ouvrier, devant sa colonne (entre le terrain et le damier).
// Les gobelins qui le touchent s'arrêtent et le frappent jusqu'à le détruire ; l'ouvrier le
// reconstruit ensuite après un délai. Les murs du « Grand rempart » et de la « Barricade »
// n'ont pas de propriétaire et disparaissent au bout d'un moment.
public class Wall {
    public float x;              // centre
    public final float y;        // centre
    public final float halfWidth;
    public final float halfHeight;
    public float hp;
    public final float maxHp;
    public final Mage owner;     // null : mur temporaire (compétence, réaction)
    public int life;             // images restantes (-1 : jusqu'à sa destruction)
    public int age = 0;          // images depuis la construction (animation)
    public int hitTimer = 0;     // > 0 : vient d'être frappé
    public boolean fighting = false;

    // Un gobelin au contact inflige chaque seconde cette part de SES PV max au mur
    public static final float GOBLIN_DPS_RATIO = 0.15f;
    public static final float BOSS_DPS_RATIO = 0.05f;

    public Wall(float x, float y, float halfWidth, float halfHeight, float maxHp, Mage owner, int life) {
        this.x = x;
        this.y = y;
        this.halfWidth = halfWidth;
        this.halfHeight = halfHeight;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.owner = owner;
        this.life = life;
    }

    public boolean isDead() {
        return hp <= 0 || life == 0;
    }

    public void repair(float amount) {
        hp = Math.min(maxHp, hp + amount);
    }

    // Le gobelin touche-t-il le mur ? (le mur est un rectangle, le gobelin un cercle)
    public boolean touches(Enemy e) {
        float cx = Math.max(x - halfWidth, Math.min(e.x, x + halfWidth));
        float cy = Math.max(y - halfHeight, Math.min(e.y, y + halfHeight));
        float dx = e.x - cx;
        float dy = e.y - cy;
        return dx * dx + dy * dy <= e.radius * e.radius;
    }
}
