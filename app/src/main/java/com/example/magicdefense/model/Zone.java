package com.example.magicdefense.model;

// Zone au sol laissée par une réaction : elle agit sur les ennemis qui s'y trouvent
public class Zone {
    public static final int LAVA = 0;     // brûle (dégâts par seconde)
    public static final int STEAM = 1;    // ralentit de moitié
    public static final int MUD = 2;      // ralentit fortement
    public static final int BOG = 3;      // empoisonne
    public static final int FIRE = 4;     // anneau de flammes : brûle

    public final float x;
    public final float y;
    public final float radius;    // en pixels
    public final int type;
    public final float dps;       // dégâts par seconde aux ennemis dans la zone (Lave)
    public final int duration;
    private int timer;

    public Zone(float x, float y, float radius, int type, float dps, int duration) {
        this.x = x;
        this.y = y;
        this.radius = radius;
        this.type = type;
        this.dps = dps;
        this.duration = duration;
        this.timer = duration;
    }

    public boolean contains(Enemy e) {
        float dx = e.x - x;
        float dy = e.y - y;
        return dx * dx + dy * dy <= radius * radius;
    }

    // 0 au début, 1 à la fin
    public float getProgress() {
        return 1 - timer / (float) duration;
    }

    public int getAge() {
        return duration - timer;
    }

    public boolean update() {
        timer--;
        return timer <= 0;
    }
}