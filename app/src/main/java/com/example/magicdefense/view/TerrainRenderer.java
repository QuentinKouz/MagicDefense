package com.example.magicdefense.view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.magicdefense.model.Board;

import java.util.Random;

// Décor fantasy médiéval en pixel art : forêt, prairie, rempart du château, cour pavée, estrade.
// Il est généré une seule fois (version jour + version nuit) quand la taille de l'écran est connue,
// puis simplement affiché à chaque image.
public class TerrainRenderer {

    // ---------- Couleurs ----------
    private static final int[] GRASS = {rgb(66, 122, 52), rgb(78, 140, 60), rgb(92, 158, 68)};
    private static final int[] DIRT = {rgb(120, 92, 58), rgb(140, 110, 70)};
    private static final int[] FLOWERS = {rgb(250, 250, 240), rgb(250, 220, 70), rgb(230, 80, 90), rgb(180, 140, 240)};
    private static final int FOREST_BACK = rgb(20, 48, 28);
    private static final int[] LEAVES = {rgb(30, 72, 38), rgb(44, 100, 48), rgb(72, 140, 60)}; // ombre, milieu, lumière
    private static final int TRUNK = rgb(78, 52, 30);
    private static final int[] STONE = {rgb(128, 126, 134), rgb(118, 116, 124), rgb(140, 137, 142)};
    private static final int STONE_LIGHT = rgb(165, 162, 168);
    private static final int MORTAR = rgb(78, 76, 86);
    private static final int OUTLINE = rgb(40, 36, 46);
    private static final int[] DOOR = {rgb(96, 60, 30), rgb(78, 48, 24)};
    private static final int[] PAVING = {rgb(152, 146, 134), rgb(138, 132, 122)};
    private static final int PAVING_LIGHT = rgb(176, 170, 158);
    private static final int PAVING_DARK = rgb(108, 102, 94);
    private static final int CELL_JOINT = rgb(70, 66, 62);
    private static final int[] PLANKS = {rgb(112, 74, 42), rgb(98, 64, 36)};
    private static final int PLANK_DARK = rgb(62, 40, 22);
    private static final int NAIL = rgb(170, 170, 175);

    // ---------- État ----------
    private Bitmap dayBitmap;
    private Bitmap nightBitmap;
    private final Paint pixelPaint = new Paint();
    private final RectF dst = new RectF();
    private float wallTop; // haut du rempart, en pixels de l'écran

    public TerrainRenderer() {
        pixelPaint.setFilterBitmap(false); // pixels nets, comme les sprites
    }

    // À appeler quand la taille de l'écran est connue (après board.resize)
    public void build(Board board) {
        int px = Math.max(1, board.mageScale() / 2);                  // taille d'un pixel du décor
        int w = (int) Math.ceil(board.width / (float) px);
        int h = (int) Math.ceil(board.height / (float) px);
        float gridLeft = board.playLeft / px;
        float gridTop = board.gridTop / px;
        float cellW = board.cellWidth / px;
        float cellH = board.cellHeight / px;
        int wallH = Math.round(30 * board.density / px);               // hauteur du rempart
        // Forêt sous le HUD, mais jamais plus de 40 % du terrain (petits écrans, tablette en paysage)
        float forestDp = Math.min(150 * board.density + board.topShift, board.gridTop * 0.4f);
        int forestH = Math.round(forestDp / px);

        int[] pixels = generate(w, h, gridLeft, gridTop, cellW, cellH, Board.ROWS, Board.COLS, wallH, forestH);
        dayBitmap = Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888);
        nightBitmap = Bitmap.createBitmap(toNight(pixels), w, h, Bitmap.Config.ARGB_8888);
        dst.set(0, 0, w * px, h * px);
        wallTop = (Math.round(gridTop) - wallH) * px;
    }

    public void draw(Canvas canvas, boolean night) {
        Bitmap bmp = night ? nightBitmap : dayBitmap;
        if (bmp != null) {
            canvas.drawBitmap(bmp, null, dst, pixelPaint);
        }
    }

    // Haut du rempart en pixels de l'écran (pour le flash rouge quand il est touché)
    public float getWallTop() {
        return wallTop;
    }

    // =====================================================================
    //  Génération (en pixels du décor)
    // =====================================================================

    static int[] generate(int w, int h, float gridLeft, float gridTop, float cellW, float cellH,
                          int rows, int cols, int wallH, int forestH) {
        int[] p = new int[w * h];
        Random rnd = new Random(7); // graine fixe : le décor est toujours le même
        int gTop = Math.round(gridTop);
        int gBottom = Math.round(gridTop + rows * cellH);
        int wallTop = gTop - wallH;

        drawMeadow(p, w, wallTop, rnd);
        drawForest(p, w, forestH, rnd);
        drawWall(p, w, wallTop, gTop, rnd);
        drawCourtyard(p, w, gridLeft, gTop, gBottom, cellW, cellH, rows, cols, rnd);
        drawPlanks(p, w, h, gBottom, rnd);
        return p;
    }

    // Prairie : herbe en plaques de trois verts, terre battue, touffes, fleurs et cailloux
    private static void drawMeadow(int[] p, int w, int bottom, Random rnd) {
        Noise grassNoise = new Noise(w, bottom, 9, rnd);
        Noise dirtNoise = new Noise(w, bottom, 16, rnd);
        for (int y = 0; y < bottom; y++) {
            for (int x = 0; x < w; x++) {
                float d = dirtNoise.at(x, y);
                int c;
                if (d > 0.78f) {
                    c = DIRT[rnd.nextInt(10) < 7 ? 0 : 1];
                } else {
                    float g = grassNoise.at(x, y) * 3 + (rnd.nextFloat() - 0.5f) * 0.6f;
                    c = GRASS[clamp((int) g, 0, 2)];
                }
                p[y * w + x] = c;
            }
        }
        // Touffes d'herbe : un pixel sombre surmonté d'un pixel clair
        for (int i = 0; i < w * bottom / 70; i++) {
            int x = rnd.nextInt(w);
            int y = 1 + rnd.nextInt(Math.max(1, bottom - 1));
            set(p, w, x, y, GRASS[0]);
            set(p, w, x, y - 1, GRASS[2]);
        }
        // Fleurs
        for (int i = 0; i < w * bottom / 220; i++) {
            set(p, w, rnd.nextInt(w), rnd.nextInt(Math.max(1, bottom)), FLOWERS[rnd.nextInt(FLOWERS.length)]);
        }
        // Cailloux : 3 x 2 pixels avec un reflet
        for (int i = 0; i < w * bottom / 900; i++) {
            int x = rnd.nextInt(Math.max(1, w - 3));
            int y = rnd.nextInt(Math.max(1, bottom - 2));
            for (int dx = 0; dx < 3; dx++) {
                set(p, w, x + dx, y, STONE[1]);
                set(p, w, x + dx, y + 1, MORTAR);
            }
            set(p, w, x, y, STONE_LIGHT);
        }
    }

    // Forêt : un fond sombre et deux rangées de feuillages arrondis, avec leurs troncs
    private static void drawForest(int[] p, int w, int forestH, Random rnd) {
        for (int y = 0; y < forestH * 0.6f; y++) {
            for (int x = 0; x < w; x++) {
                set(p, w, x, y, FOREST_BACK);
            }
        }
        // Ombre de la forêt sur la prairie
        for (int y = forestH; y < forestH + 3; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                if (i >= 0 && i < p.length) {
                    p[i] = darken(p[i], 0.75f);
                }
            }
        }
        for (int layer = 0; layer < 3; layer++) {           // du fond (sombre) vers le devant (éclairé)
            float shade = 0.55f + 0.225f * layer;
            int baseY = forestH - (2 - layer) * 9 - 2;
            for (int cx = -4 + layer * 4; cx < w + 8; cx += 8 + rnd.nextInt(5)) {
                int r = 6 + rnd.nextInt(5);
                int cy = baseY - r + rnd.nextInt(3);
                for (int ty = cy + r - 2; ty < cy + r + 3; ty++) {   // tronc
                    set(p, w, cx, ty, darken(TRUNK, shade));
                    set(p, w, cx + 1, ty, darken(TRUNK, shade * 0.8f));
                }
                for (int dy = -r; dy <= r; dy++) {                    // feuillage
                    for (int dx = -r; dx <= r; dx++) {
                        if (dx * dx + dy * dy > r * r) {
                            continue;
                        }
                        int c;
                        if (dx * dx + dy * dy > (r - 1) * (r - 1)) {
                            c = LEAVES[0];                             // bord sombre
                        } else if (dx + dy < -r * 0.5f) {
                            c = LEAVES[2];                             // lumière en haut à gauche
                        } else if (dx + dy > r * 0.4f) {
                            c = LEAVES[0];                             // ombre en bas à droite
                        } else {
                            c = LEAVES[1];
                        }
                        if (rnd.nextInt(9) == 0) {
                            c = LEAVES[rnd.nextInt(3)];                // feuillage irrégulier
                        }
                        set(p, w, cx + dx, cy + dy, darken(c, shade));
                    }
                }
            }
        }
    }

    // Rempart : briques de pierre, créneaux, deux tours aux extrémités et une porte au centre
    private static void drawWall(int[] p, int w, int wallTop, int gTop, Random rnd) {
        int towerW = 16;
        int towerExtra = 12;
        // Murs : partie basse sur toute la largeur, plus les tours qui montent plus haut
        for (int y = wallTop - towerExtra; y < gTop; y++) {
            for (int x = 0; x < w; x++) {
                boolean tower = x < towerW || x >= w - towerW;
                if (y < wallTop && !tower) {
                    continue;
                }
                set(p, w, x, y, brick(x, y - (wallTop - towerExtra), rnd));
            }
        }
        // Créneaux du rempart et des tours
        drawMerlons(p, w, towerW, w - towerW, wallTop, rnd);
        drawMerlons(p, w, 0, towerW, wallTop - towerExtra, rnd);
        drawMerlons(p, w, w - towerW, w, wallTop - towerExtra, rnd);
        // Contour vertical des tours
        for (int y = wallTop - towerExtra - 6; y < gTop; y++) {
            set(p, w, towerW, y, OUTLINE);
            set(p, w, w - towerW - 1, y, OUTLINE);
        }
        // Meurtrières dans les tours
        for (int t = 0; t < 2; t++) {
            int x = t == 0 ? towerW / 2 : w - towerW / 2 - 1;
            for (int y = wallTop - 6; y < wallTop + 2; y++) {
                set(p, w, x, y, OUTLINE);
            }
        }
        // Porte en bois arrondie au centre
        int doorW = 20;
        int doorH = gTop - wallTop - 1;
        int dx0 = w / 2 - doorW / 2;
        int archY = wallTop + doorW / 2;
        for (int y = wallTop + 1; y < gTop; y++) {
            for (int x = dx0 - 1; x <= dx0 + doorW; x++) {
                float fx = (x - (dx0 + doorW / 2f - 0.5f)) / (doorW / 2f + 0.5f);
                boolean inArch = y >= archY || fx * fx + Math.pow((y - archY) / (doorW / 2f), 2) <= 1;
                if (!inArch) {
                    continue;
                }
                boolean border = x == dx0 - 1 || x == dx0 + doorW
                        || (y < archY && fx * fx + Math.pow((y - archY) / (doorW / 2f), 2) > 0.75);
                if (border) {
                    set(p, w, x, y, OUTLINE);
                } else {
                    set(p, w, x, y, ((x - dx0) % 3 == 0) ? DOOR[1] : DOOR[0]); // planches verticales
                }
            }
        }
        for (int x = dx0; x < dx0 + doorW; x++) {        // ferrures
            set(p, w, x, archY + 1, OUTLINE);
            set(p, w, x, archY + doorH / 2, OUTLINE);
        }
        // Ombre du rempart sur la cour
        for (int x = 0; x < w; x++) {
            set(p, w, x, gTop - 1, OUTLINE);
        }
    }

    private static void drawMerlons(int[] p, int w, int from, int to, int top, Random rnd) {
        for (int x = from; x < to; x++) {
            if ((x - from) % 12 >= 7) {
                continue; // espace entre deux créneaux
            }
            for (int y = top - 6; y < top; y++) {
                boolean edge = y == top - 6 || (x - from) % 12 == 0 || (x - from) % 12 == 6;
                set(p, w, x, y, edge ? OUTLINE : (y == top - 5 ? STONE_LIGHT : STONE[rnd.nextInt(3)]));
            }
        }
    }

    // Pierre d'un mur de briques décalées (4 pixels de haut, 8 de large)
    private static int brick(int x, int y, Random rnd) {
        int row = y / 4;
        int yIn = y % 4;
        int xIn = (x + (row % 2) * 4) % 8;
        if (yIn == 3 || xIn == 7) {
            return MORTAR;
        }
        int id = ((x + (row % 2) * 4) / 8) * 31 + row * 17;
        int c = STONE[Math.abs(id) % 3];
        if (yIn == 0) {
            c = lighten(c, 1.12f);                     // arête éclairée
        }
        return rnd.nextInt(14) == 0 ? darken(c, 0.88f) : c;
    }

    // Cour pavée : chaque case est une dalle de 2 x 2 pavés, joints plus marqués entre les cases.
    // Sur un écran plus large que la colonne de jeu (tablette), les côtés sont pavés aussi,
    // un peu plus sombres et sans joints de case, pour que le damier ressorte.
    private static void drawCourtyard(int[] p, int w, float gridLeft, int gTop, int gBottom,
                                      float cellW, float cellH, int rows, int cols, Random rnd) {
        float gridRight = gridLeft + cols * cellW;
        for (int y = gTop; y < gBottom; y++) {
            for (int x = 0; x < w; x++) {
                boolean inGrid = x >= gridLeft - 0.5f && x < gridRight + 0.5f;
                float gx = x - gridLeft;
                int col = (int) Math.floor(gx / cellW);
                int row = clamp((int) ((y - gTop) / cellH), 0, rows - 1);
                float lx = gx - col * cellW;
                float ly = y - gTop - row * cellH;
                int c;
                if (inGrid && (lx < 1.5f || ly < 1.5f)) {
                    c = CELL_JOINT;                                       // joint entre deux cases
                } else {
                    float sx = lx % (cellW / 2);                          // position dans le pavé
                    float sy = ly % (cellH / 2);
                    boolean joint = (sx < 1 && (lx > 2 || !inGrid)) || (sy < 1 && (ly > 2 || !inGrid));
                    int base = PAVING[Math.abs(row + col) % 2];
                    if (joint) {
                        c = PAVING_DARK;
                    } else if (sx < 2 || sy < 2) {
                        c = PAVING_LIGHT;                                 // arête éclairée
                    } else if (sx > cellW / 2 - 2 || sy > cellH / 2 - 2) {
                        c = PAVING_DARK;                                  // arête dans l'ombre
                    } else {
                        c = rnd.nextInt(16) == 0 ? darken(base, 0.9f) : base;
                    }
                    if (!inGrid) {
                        c = darken(c, 0.82f);                             // hors du damier
                    }
                }
                set(p, w, x, y, c);
            }
        }
        // Bords du damier bien marqués quand il ne prend pas toute la largeur
        if (gridLeft >= 1) {
            for (int y = gTop; y < gBottom; y++) {
                set(p, w, Math.round(gridLeft) - 1, y, CELL_JOINT);
                set(p, w, Math.round(gridRight), y, CELL_JOINT);
            }
        }
    }

    // Estrade en planches de bois clouées sous les boutons de recrutement
    private static void drawPlanks(int[] p, int w, int h, int top, Random rnd) {
        for (int y = top; y < h; y++) {
            int row = (y - top) / 6;
            int yIn = (y - top) % 6;
            for (int x = 0; x < w; x++) {
                int c = PLANKS[row % 2];
                if (yIn == 0) {
                    c = PLANK_DARK;                                   // jointure entre deux planches
                } else if (rnd.nextInt(18) == 0) {
                    c = darken(c, 0.85f);                             // veinage du bois
                }
                set(p, w, x, y, c);
            }
            // Raccords de planches et clous, décalés d'une rangée à l'autre
            if (yIn == 0) {
                for (int x = (row * 23) % 40; x < w; x += 40) {
                    for (int dy = 1; dy < 6; dy++) {
                        set(p, w, x, y + dy, PLANK_DARK);
                    }
                    set(p, w, x - 2, y + 2, NAIL);
                    set(p, w, x + 2, y + 2, NAIL);
                }
            }
        }
    }

    // Version de nuit : couleurs assombries et bleutées
    static int[] toNight(int[] day) {
        int[] n = new int[day.length];
        for (int i = 0; i < day.length; i++) {
            int c = day[i];
            int r = (c >> 16) & 255;
            int g = (c >> 8) & 255;
            int b = c & 255;
            n[i] = rgb((int) (r * 0.40f), (int) (g * 0.46f), Math.min(255, (int) (b * 0.70f) + 8));
        }
        return n;
    }

    // ---------- Outils ----------

    // Bruit doux : valeurs au hasard sur une grille grossière, interpolées entre elles
    private static class Noise {
        private final int cell;
        private final int gw;
        private final float[] values;

        Noise(int w, int h, int cell, Random rnd) {
            this.cell = cell;
            this.gw = w / cell + 2;
            int gh = h / cell + 2;
            values = new float[gw * gh];
            for (int i = 0; i < values.length; i++) {
                values[i] = rnd.nextFloat();
            }
        }

        float at(int x, int y) {
            int cx = x / cell;
            int cy = y / cell;
            float fx = (x % cell) / (float) cell;
            float fy = (y % cell) / (float) cell;
            float a = values[cy * gw + cx];
            float b = values[cy * gw + cx + 1];
            float c = values[(cy + 1) * gw + cx];
            float d = values[(cy + 1) * gw + cx + 1];
            float top = a + (b - a) * fx;
            float bottom = c + (d - c) * fx;
            return top + (bottom - top) * fy;
        }
    }

    private static void set(int[] p, int w, int x, int y, int color) {
        if (x >= 0 && x < w && y >= 0 && y * w + x < p.length) {
            p[y * w + x] = color;
        }
    }

    private static int rgb(int r, int g, int b) {
        return 0xff000000 | (clamp(r, 0, 255) << 16) | (clamp(g, 0, 255) << 8) | clamp(b, 0, 255);
    }

    private static int darken(int c, float f) {
        return rgb((int) (((c >> 16) & 255) * f), (int) (((c >> 8) & 255) * f), (int) ((c & 255) * f));
    }

    private static int lighten(int c, float f) {
        return darken(c, f);
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
