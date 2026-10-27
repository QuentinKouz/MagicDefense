package com.example.magicdefense.model;

// Géométrie de la zone de jeu : taille de l'écran, colonne de jeu, damier, château.
// Tout ce qui dépend de la taille de l'appareil est calculé ici, une seule fois.
public class Board {
    public static final int COLS = 4;
    public static final int ROWS = 3;

    // Largeur maximale de la colonne de jeu : sur tablette, le jeu reste centré
    // et à taille confortable, le décor remplit les côtés
    private static final float MAX_PLAY_WIDTH_DP = 480;
    // Hauteur de la zone traversée par les gobelins sur le Pixel 7a (écran de référence)
    private static final float REFERENCE_FIELD_DP = 578;

    // Sprite du mage : taille en pixels de sprite, et position de l'embout du bâton dans la grille
    public static final int MAGE_SPRITE_W = 19;
    public static final int MAGE_SPRITE_H = 18;
    private static final float STAFF_TIP_X = 16.5f;
    private static final float STAFF_TIP_Y = 2.5f;

    public final float density;
    public int width;              // taille de tout l'écran
    public int height;
    public float playLeft;         // colonne de jeu (= tout l'écran sur un téléphone)
    public float playWidth;
    public float topShift;         // décalage du HUD vers le bas pour éviter la caméra / l'encoche
    public float cellWidth;
    public float cellHeight;
    public float gridTop;          // haut du damier = position du château
    public float bottomBarHeight;  // place réservée aux boutons de recrutement
    private float safeInsetTop = 0; // hauteur de la caméra / de l'encoche, en pixels

    public Board(float density) {
        this.density = density;
    }

    // Zone cachée par la caméra ou l'encoche en haut de l'écran (fournie par GameView)
    public void setSafeInsetTop(float inset) {
        safeInsetTop = inset;
    }

    // Appelée quand la taille de la vue est connue (ou change : rotation, pliable...)
    public void resize(int w, int h) {
        width = w;
        height = h;
        playWidth = Math.min(w, MAX_PLAY_WIDTH_DP * density);
        playLeft = (w - playWidth) / 2;
        // Le HUD commence à 40 dp du haut, sauf si la caméra descend plus bas
        topShift = Math.max(0, safeInsetTop + 12 * density - 40 * density);
        bottomBarHeight = 96 * density;
        cellWidth = playWidth / COLS;
        cellHeight = Math.min(cellWidth, 80 * density);
        gridTop = h - bottomBarHeight - ROWS * cellHeight;
    }

    // ---------- Colonne de jeu ----------
    public float playRight() {
        return playLeft + playWidth;
    }

    public float centerX() {
        return playLeft + playWidth / 2;
    }

    // Vitesse relative des gobelins : ils mettent le même temps à atteindre le château
    // quelle que soit la hauteur de l'écran (1 sur le Pixel 7a)
    public float fieldScale() {
        return gridTop / (REFERENCE_FIELD_DP * density);
    }

    // ---------- Damier ----------
    public float cellLeft(int col) {
        return playLeft + col * cellWidth;
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
        if (y < gridTop || y >= gridTop + ROWS * cellHeight || x < playLeft || x >= playRight()) {
            return null;
        }
        int col = Math.min(COLS - 1, (int) ((x - playLeft) / cellWidth));
        int row = Math.min(ROWS - 1, (int) ((y - gridTop) / cellHeight));
        return new int[]{row, col};
    }

    // Agrandissement entier du sprite de mage pour qu'il tienne dans une case
    public int mageScale() {
        return Math.max(1, (int) Math.min(cellWidth * 0.9f / MAGE_SPRITE_W,
                cellHeight * 0.9f / MAGE_SPRITE_H));
    }

    // Position à l'écran de l'embout du bâton du mage d'une case
    public float staffTipX(int col) {
        return cellCenterX(col) + (STAFF_TIP_X - MAGE_SPRITE_W / 2f) * mageScale();
    }

    public float staffTipY(int row) {
        return cellCenterY(row) + (STAFF_TIP_Y - MAGE_SPRITE_H / 2f) * mageScale();
    }
}
