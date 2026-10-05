package com.example.magicdefense.model;

// Un tir de mage qui suit sa cible
public class Projectile {
    public float x;
    public float y;
    public float speed;
    public float radius;
    public int damage;
    public int type;      // élément du mage qui a tiré (pour la couleur)
    public Enemy target;

    public Projectile(float x, float y, Enemy target, int damage, int type, float density) {
        this.x = x;
        this.y = y;
        this.target = target;
        this.damage = damage;
        this.type = type;
        this.speed = 8 * density;
        this.radius = 6 * density;
    }

    // Avance vers la cible. Retourne true si elle est atteinte.
    public boolean update() {
        float dx = target.x - x;
        float dy = target.y - y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance <= speed + target.radius) {
            return true; // impact
        }
        x += dx / distance * speed;
        y += dy / distance * speed;
        return false;
    }
}
