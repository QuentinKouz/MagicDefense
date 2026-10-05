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
}
