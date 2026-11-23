package com.example.magicdefense.model;

// Projectile magique lancé par un gobelin mage depuis l'orée de la forêt :
// il file vers le rempart (ou vers un mur d'Ouvrier) et l'abîme à l'arrivée.
public class EnemyBolt {
    public float x;
    public float y;
    public final float startX;
    public final float startY;
    public final float targetX;
    public final float targetY;
    public final float speed;  // pixels par image
    public final int damage;   // dégâts au château
    public final float wallDamage; // dégâts au mur visé
    public final Wall wall;    // null : vise le château
    public int age = 0;        // images depuis le tir (animation)

    public EnemyBolt(float x, float y, float targetX, float targetY, float speed,
                     int damage, float wallDamage, Wall wall) {
        this.x = x;
        this.y = y;
        this.startX = x;
        this.startY = y;
        this.targetX = targetX;
        this.targetY = targetY;
        this.speed = speed;
        this.damage = damage;
        this.wallDamage = wallDamage;
        this.wall = wall;
    }

    // Avance d'une image ; renvoie true à l'arrivée sur la cible
    public boolean update() {
        age++;
        float dx = targetX - x;
        float dy = targetY - y;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist <= speed) {
            x = targetX;
            y = targetY;
            return true;
        }
        x += dx / dist * speed;
        y += dy / dist * speed;
        return false;
    }

    // Direction du vol (pour dessiner la traînée)
    public float dirX() {
        float dx = targetX - startX;
        float dy = targetY - startY;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        return d == 0 ? 0 : dx / d;
    }

    public float dirY() {
        float dx = targetX - startX;
        float dy = targetY - startY;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        return d == 0 ? 1 : dy / d;
    }
}
