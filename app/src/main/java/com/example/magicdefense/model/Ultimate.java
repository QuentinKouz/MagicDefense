package com.example.magicdefense.model;

// Les ultimes : un seul est équipé (page Château) ; la jauge se remplit en tuant des gobelins
// et la secousse du téléphone (ou le mode automatique) le déclenche.
public final class Ultimate {
    public static final int STUN = 0;     // Onde de choc : tous les gobelins étourdis (possédé dès le départ)
    public static final int FURY = 1;     // Rage arcanique : dégâts des mages ×2 pendant quelques secondes
    public static final int METEOR = 2;   // Météorite géante : énormes dégâts à tous les gobelins
    public static final int SHIELD = 3;   // Bouclier divin : château invincible pendant quelques secondes
    public static final int COUNT = 4;

    public static final String[] NAMES = {"Onde de choc", "Rage arcanique", "Météorite géante", "Bouclier divin"};
    public static final String[] DESCRIPTIONS = {
            "Tous les gobelins étourdis 4 s",
            "Dégâts des mages ×2 pendant 8 s",
            "Écrase tous les gobelins du terrain",
            "Château invincible pendant 10 s",
    };

    // Prix au Marché, en gemmes (0 = possédé dès le départ)
    private static final int[] PRICES = {0, 250, 450, 350};
    // Gobelins à tuer pour remplir la jauge
    private static final int[] KILLS_REQUIRED = {20, 25, 30, 25};

    // ---------- Réglages ----------
    public static final int STUN_FRAMES = 240;          // ≈ 4 s
    public static final int FURY_FRAMES = 480;          // ≈ 8 s
    public static final float FURY_DAMAGE_MULT = 2f;
    public static final int SHIELD_FRAMES = 600;        // ≈ 10 s
    public static final int METEOR_FALL_FRAMES = 45;    // chute de la météorite avant l'impact
    public static final float METEOR_DAMAGE = 0.7f;     // 70 % des PV max de chaque gobelin...
    public static final float METEOR_BOSS_DAMAGE = 0.2f; // ...mais seulement 20 % de ceux du boss

    private Ultimate() {
    }

    public static int price(int id) {
        return PRICES[id];
    }

    public static int killsRequired(int id) {
        return KILLS_REQUIRED[id];
    }
}
