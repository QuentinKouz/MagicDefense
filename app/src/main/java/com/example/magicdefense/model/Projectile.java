package com.example.magicdefense.model;

// Un tir de mage qui suit sa cible (Feu, Terre, Eau ; la Foudre est instantanée)
public class Projectile {
    public float x;
    public float y;
    public float speed;
    public float radius;
    public float damage;
    public float aoeRadius;       // > 0 : dégâts de zone (Eau)
    public int type;              // élément du mage qui a tiré
    public int level;             // niveau du mage : l'attaque évolue avec lui
    public float dirX = 0;        // direction du dernier déplacement (pour les traînées)
    public float dirY = -1;       // par défaut : vers le haut (vers les ennemis)
    public int age = 0;           // images depuis le tir (animations)
    public Enemy target;

    // Le projectile reprend les caractéristiques du mage qui l'a tiré
    public Projectile(float x, float y, Enemy target, Mage mage, float density) {
        this.x = x;
        this.y = y;
        this.target = target;
        this.type = mage.type;
        this.level = mage.level;
        this.damage = mage.getDamage();
        this.speed = mage.getProjectileSpeed() * density;
        this.aoeRadius = mage.getAoeRadius() * density;
        this.radius = 6 * density;
    }

    // Avance vers la cible. Retourne true si elle est atteinte.
    public boolean update() {
        age++;
        float dx = target.x - x;
        float dy = target.y - y;
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance > 0) {
            dirX = dx / distance;
            dirY = dy / distance;
        }

        if (distance <= speed + target.radius) {
            return true; // impact
        }
        x += dirX * speed;
        y += dirY * speed;
        return false;
    }
}
