package com.example.magicdefense.model;

// Un ennemi : uniquement des données et son déplacement (aucun dessin)
public class Enemy {
    public float x;
    public float y;
    public float radius;
    public float speed;
    public float hp;
    public float maxHp;

    public Enemy(float x, float radius, float speed, float hp) {
        this.x = x;
        this.y = -radius; // apparaît juste au-dessus de l'écran
        this.radius = radius;
        this.speed = speed;
        this.hp = hp;
        this.maxHp = hp;
    }

    // L'ennemi descend vers le château (speedMult : plus rapide la nuit)
    public void update(float speedMult) {
        y += speed * speedMult;
    }

    public boolean isDead() {
        return hp <= 0;
    }
}
