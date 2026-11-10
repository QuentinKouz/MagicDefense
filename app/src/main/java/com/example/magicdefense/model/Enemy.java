package com.example.magicdefense.model;

// Un ennemi : uniquement des données, son déplacement et ses états (aucun dessin)
public class Enemy {
    // Types de gobelins
    public static final int NORMAL = 0;
    public static final int FAST = 1;    // rapide : petit, fragile
    public static final int TANK = 2;    // costaud : gros, lent, très résistant, frappe plus fort
    public static final int BOSS = 3;    // chef gobelin : énorme, invoque des diablotins
    public static final int SHAMAN = 4;  // chaman (invocateur ennemi) : invoque des diablotins
    public static final int IMP = 5;     // diablotin invoqué : minuscule, rapide, très fragile
    public static final int KIND_COUNT = 6;

    // Caractéristiques relatives à un gobelin normal (indice = kind)
    public static final float[] SPEED_MULT = {1f, 1.5f, 0.7f, 0.45f, 0.8f, 1.6f};
    public static final float[] HP_MULT = {1f, 0.6f, 2.5f, 12f, 1.6f, 0.35f};
    public static final float[] RADIUS_MULT = {1f, 0.75f, 1.35f, 2.0f, 1.05f, 0.6f};
    public static final float[] CASTLE_DAMAGE_MULT = {1f, 1f, 1.5f, 3f, 1f, 0.4f};
    public static final float[] GOLD_MULT = {1f, 1f, 1.6f, 10f, 1.6f, 0.2f};
    // Invocation : délai entre deux invocations (0 = n'invoque pas) et nombre de diablotins
    public static final int[] SUMMON_DELAY = {0, 0, 0, 240, 200, 0};
    public static final int[] SUMMON_COUNT = {0, 0, 0, 3, 2, 0};

    public static final int MARK_DURATION = 180; // ≈ 3 s pour déclencher une réaction
    public static final int NO_MARK = -1;

    public float x;
    public float y;
    public float radius;
    public float speed;
    public float hp;
    public float maxHp;
    public int kind = NORMAL;

    // États
    public float poisonDps = 0;   // dégâts par seconde du poison
    public int poisonTimer = 0;
    public int slowTimer = 0;     // > 0 : ralenti de moitié (Vapeur)
    public int stunTimer = 0;     // > 0 : étourdi (esprit, Électrocution, Tempête de sable)
    public int mark = NO_MARK;    // dernier élément reçu (pour les réactions)
    public int markTimer = 0;
    public int summonTimer;       // compte à rebours avant la prochaine invocation

    public Enemy(float x, float radius, float speed, float hp) {
        this(x, radius, speed, hp, NORMAL);
    }

    public Enemy(float x, float radius, float speed, float hp, int kind) {
        this.kind = kind;
        this.x = x;
        this.y = -radius; // apparaît juste au-dessus de l'écran
        this.radius = radius;
        this.speed = speed;
        this.hp = hp;
        this.maxHp = hp;
        this.summonTimer = SUMMON_DELAY[kind] / 2; // première invocation assez vite
    }

    public boolean isBoss() {
        return kind == BOSS;
    }

    // L'ennemi descend vers le château (speedMult : plus rapide la nuit)
    public void update(float speedMult) {
        if (stunTimer > 0) {
            stunTimer--;
            return;
        }
        float mult = slowTimer > 0 ? 0.5f : 1f;
        if (slowTimer > 0) {
            slowTimer--;
        }
        y += speed * speedMult * mult;
    }

    // États qui s'écoulent à chaque image (même pendant l'ultime) : poison et marque
    public void tickStatus() {
        if (poisonTimer > 0) {
            hp -= poisonDps / 60f;
            poisonTimer--;
            if (poisonTimer == 0) {
                poisonDps = 0;
            }
        }
        if (markTimer > 0) {
            markTimer--;
            if (markTimer == 0) {
                mark = NO_MARK;
            }
        }
    }

    public void poison(float dps, int duration) {
        poisonDps = Math.max(poisonDps, dps); // le poison le plus fort l'emporte
        poisonTimer = Math.max(poisonTimer, duration);
    }

    // Étourdissement : le boss n'y est qu'à moitié sensible
    public void stun(int frames) {
        int f = isBoss() ? frames / 2 : frames;
        stunTimer = Math.max(stunTimer, f);
    }

    // Recul vers la forêt (le boss résiste)
    public void knockBack(float distance) {
        float d = isBoss() ? distance * 0.3f : distance;
        y = Math.max(-radius, y - d);
    }

    public boolean isPoisoned() {
        return poisonTimer > 0;
    }

    public boolean isDead() {
        return hp <= 0;
    }
}