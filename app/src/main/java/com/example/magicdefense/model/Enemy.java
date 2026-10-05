package com.example.magicdefense.model;

// Un ennemi : uniquement des données et son déplacement (aucun dessin)
public class Enemy {
    public float x;
    public float y;
    public float radius;
    public float speed;
    public int hp;
    public int maxHp;

    public Enemy(float x, float radius, float speed, int hp) {
        this.x = x;
        this.y = -radius; // apparaît juste au-dessus de l'écran
        this.radius = radius;
        this.speed = speed;
        this.hp = hp;
        this.maxHp = hp;
    }

    // L'ennemi descend vers le château
    public void update() {
        y += speed;
    }

    public boolean isDead() {
        return hp <= 0;
    }
}
