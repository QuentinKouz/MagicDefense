package com.example.magicdefense.view;

import android.graphics.Color;

// Couleurs et noms affichés pour chaque élément (indice = Mage.type)
public class Palette {
    public static final String[] ELEMENT_NAMES = {"Eau", "Feu", "Terre", "Foudre"};
    public static final int[] ELEMENT_COLORS = {
            Color.rgb(70, 150, 255),   // Eau
            Color.rgb(255, 110, 50),   // Feu
            Color.rgb(150, 110, 60),   // Terre
            Color.rgb(170, 80, 255)    // Foudre
    };

    public static final int BACKGROUND = Color.rgb(20, 20, 40);
    public static final int CELL_DARK = Color.rgb(45, 60, 45);
    public static final int CELL_LIGHT = Color.rgb(60, 80, 60);
    public static final int CASTLE = Color.rgb(80, 200, 255);
    public static final int ENEMY = Color.rgb(220, 60, 60);
    public static final int GOLD = Color.rgb(255, 215, 0);
    public static final int DISABLED = Color.rgb(70, 70, 80);
    public static final int GAME_OVER = Color.rgb(255, 90, 90);
    public static final int GAUGE_EMPTY = Color.rgb(60, 60, 80);
    public static final int GAUGE_CHARGING = Color.rgb(170, 170, 200);
    public static final int STUNNED = Color.rgb(120, 200, 255); // ennemi étourdi

    // Mode nuit
    public static final int BACKGROUND_NIGHT = Color.rgb(5, 5, 15);
    public static final int CELL_DARK_NIGHT = Color.rgb(20, 28, 40);
    public static final int CELL_LIGHT_NIGHT = Color.rgb(28, 38, 55);

    // Unités de nuit
    public static final int ENEMY_NIGHT = Color.rgb(45, 25, 30);
    public static final int ENEMY_EYES = Color.rgb(255, 40, 40);
    public static final int TORCH_STICK = Color.rgb(110, 70, 35);
    public static final int TORCH_FLAME = Color.rgb(255, 140, 30);
    public static final int TORCH_CORE = Color.rgb(255, 230, 120);
    public static final int TORCH_GLOW = Color.argb(45, 255, 150, 50); // halo transparent

    // Fenêtre de pause
    public static final int POPUP = Color.rgb(35, 35, 60);
    public static final int MENU_BUTTON = Color.rgb(80, 80, 130);
    public static final int RESUME = Color.rgb(70, 170, 90);
    public static final int QUIT = Color.rgb(190, 60, 60);
    public static final int ICON_DISABLED = Color.rgb(120, 120, 130);

    // Interrupteur
    public static final int TOGGLE_ON = Color.rgb(70, 170, 90);
    public static final int TOGGLE_OFF = Color.rgb(90, 90, 110);
    public static final int TEXT_INACTIVE = Color.rgb(130, 130, 150);

    // Château et étoiles
    public static final int CASTLE_HIT = Color.rgb(230, 50, 70);
    public static final int STAR_EMPTY = Color.rgb(70, 70, 90);
    public static final int HP_BAR_BG = Color.rgb(50, 50, 70);

    public static final int MAP_PATH = Color.rgb(70, 70, 110);

}
