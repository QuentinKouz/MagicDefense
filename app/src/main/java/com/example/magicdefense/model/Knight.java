package com.example.magicdefense.model;

// Chevalier invoqué par un mage Invocateur : grand guerrier (environ la largeur d'une case)
// qui part aussitôt à l'assaut des gobelins sur le terrain. Les gobelins de sa colonne qu'il
// touche s'arrêtent pour se battre au lieu d'avancer, sauf les petits rapides (rapides,
// diablotins) qui se faufilent. Sans cible, il revient monter la garde devant le château,
// dans sa colonne.
public class Knight {
    public float x;
    public float y;
    public final float radius;
    public float guardX;       // position de garde : centre de sa colonne, devant le rempart
    public float hp;
    public final float maxHp;
    public final float damage; // dégâts d'un coup d'épée
    public final Mage owner;   // mage qui l'a invoqué (limite de chevaliers par mage)
    public Enemy target = null; // gobelin qu'il poursuit
    public int attackTimer = 0;
    public int age = 0;        // images depuis l'apparition (animation)
    public int hitTimer = 0;   // > 0 : vient d'être frappé (clignote)
    public boolean fighting = false;
    public boolean marching = false; // en mouvement (animation de marche)

    public static final int ATTACK_DELAY = 30;      // un coup d'épée toutes les 0,5 s
    public static final float MOVE_SPEED = 2.4f;     // dp par image : il fonce sur les gobelins
    public static final float RADIUS_PER_CELL = 0.4f; // rayon = 0,4 × largeur d'une case
    public static final float HP_PER_DAMAGE = 4f;    // PV = 4 × dégâts du mage (il s'expose davantage)
    public static final float HIT_PER_DAMAGE = 0.6f; // coup d'épée = 0,6 × dégâts du mage
    // Un gobelin au contact inflige chaque seconde cette part de SES PV max au chevalier
    public static final float GOBLIN_DPS_RATIO = 0.25f;
    public static final float BOSS_DPS_RATIO = 0.08f;

    public Knight(float x, float y, float radius, float maxHp, float damage, Mage owner) {
        this.x = x;
        this.y = y;
        this.guardX = x;
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

    // Le chevalier arrête-t-il ce gobelin ? (au contact, et pas un petit rapide)
    public boolean blocks(Enemy e) {
        return !isDead() && !e.slipsPastKnights() && touches(e);
    }
}
