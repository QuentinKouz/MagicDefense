package com.example.magicdefense.model;

// Géométrie de la zone de jeu : taille de l'écran, damier, château
public class Board {
    public static final int COLS = 4;
    public static final int ROWS = 3;

    public final float density;
    public int width;
    public int height;
    public float cellWidth;
    public float cellHeight;
    public float gridTop;          // haut du damier = position du château
    public float bottomBarHeight;  // place réservée aux boutons de recrutement

    public Board(float density) {
        this.density = density;
    }

    // Appelée quand la taille de la vue est connue
    public void resize(int w, int h) {
        width = w;
        height = h;
        bottomBarHeight = 96 * density;
        cellWidth = w / (float) COLS;
        cellHeight = Math.min(cellWidth, 80 * density);
        gridTop = h - bottomBarHeight - ROWS * cellHeight;
    }

    public float cellLeft(int col) {
        return col * cellWidth;
    }

    public float cellTop(int row) {
        return gridTop + row * cellHeight;
    }

    public float cellCenterX(int col) {
        return cellLeft(col) + cellWidth / 2;
    }

    public float cellCenterY(int row) {
        return cellTop(row) + cellHeight / 2;
    }

    // Case {ligne, colonne} sous le point (x, y), ou null si hors du damier
    public int[] cellAt(float x, float y) {
        if (y < gridTop || y >= gridTop + ROWS * cellHeight || x < 0 || x >= width) {
            return null;
        }
        int col = (int) (x / cellWidth);
        int row = (int) ((y - gridTop) / cellHeight);
        return new int[]{row, col};
    }
}
