package com.example.magicdefense.model;

// Chevalier invoqué par un mage Invocateur : il monte la garde devant le château,
// se décale pour intercepter le gobelin le plus proche et le combat au corps à corps.
// Les gobelins qui le touchent s'arrêtent pour se battre au lieu d'avancer.
public class Knight {
    public float x;
    public float y;            // position de garde (ligne devant le rempart)
    public final float radius;
    public float hp;
    public final float maxHp;
    public final float damage; // dégâts d'un coup d'épée
    public final Mage owner;   // mage qui l'a invoqué (limite de chevaliers par mage)
    public int attackTimer = 0;
    public int age = 0;        // images depuis l'apparition (animation)
    public int hitTimer = 0;   // > 0 : vient d'être frappé (clignote)
    public boolean fighting = false;

    public static final int ATTACK_DELAY = 30;      // un coup d'épée toutes les 0,5 s
    public static final float MOVE_SPEED = 1.6f;     // dp par image, pour se placer
    public static final float HP_PER_DAMAGE = 3f;    // PV = 3 × dégâts du mage
    public static final float HIT_PER_DAMAGE = 0.6f; // coup d'épée = 0,6 × dégâts du mage
    // Un gobelin au contact inflige chaque seconde cette part de SES PV max au chevalier
    public static final float GOBLIN_DPS_RATIO = 0.25f;
    public static final float BOSS_DPS_RATIO = 0.08f;

    public Knight(float x, float y, float radius, float maxHp, float damage, Mage owner) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.maxHp = maxHp;
        this.hp = maxHp;
        this.damage = damage;
        this.owner = owner;
    }

    public boolean isDead() {
        return hp <= 0;
    }

    // Le gobelin est-il au contact ?
    public boolean touches(Enemy e) {
        float dx = e.x - x;
        float dy = e.y - y;
        float r = e.radius + radius;
        return dx * dx + dy * dy <= r * r;
    }
}
