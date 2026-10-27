package com.example.magicdefense.view;

import android.graphics.Bitmap;
import android.graphics.Color;

// Sprites pixel art : chaque personnage est une grille de caractères,
// chaque caractère correspond à une couleur de la palette ('.' = transparent).
// Les grilles sont converties une seule fois en petites images (Bitmap).
public final class Sprites {

    private Sprites() {
    }

    // ---------- Mages : 19 x 18 pixels ----------

    // Niveau 1 : apprenti, sans barbe, robe simple
    private static final String[] BODY_APPRENTICE = {
            "......KK",
            ".....KRRK",
            "....KRRRK",
            "....KRRRRK",
            "...KRRRRRK",
            "..KRRRRRRRK",
            ".KKKKKKKKKKK.....Tt",
            "...KSSSSSSK......Tt",
            "...KSESSESK......Tt",
            "...KSSSSSSK......Tt",
            "....KsSSsK.......Tt",
            "..KKRRRRRRKK...KSSK",
            ".KRRRRRRRRRRKKKSSSK",
            "KRRRRRRRRRRRRRKKKK",
            "KrRRRRRRRRRRRrK..Tt",
            "KrrRRRRRRRRRrrK..Tt",
            "KrrrrrrrrrrrrrK..Tt",
            ".KKKKKKKKKKKKKK..Tt",
    };

    // Niveaux 2 à 5 : barbe et ceinture dorée
    private static final String[] BODY_MAGE = {
            "......KK",
            ".....KRRK",
            "....KRRRK",
            "....KRRRRK",
            "...KRRRRRK",
            "..KRRRRRRRK",
            ".KKKKKKKKKKK.....Tt",
            "...KSSSSSSK......Tt",
            "...KSESSESK......Tt",
            "...KsSSSSsK......Tt",
            "..KRWWWWWWRK.....Tt",
            ".KRRwWWWWwRRK..KSSK",
            ".KRRRwWWwRRRRKKSSSK",
            "KRRRRRwwRRRRRRKKKK",
            "KrRRRRYYRRRRRrK..Tt",
            "KrrRRRYYRRRRrrK..Tt",
            "KrrrrrYYrrrrrrK..Tt",
            ".KKKKKKKKKKKKKK..Tt",
    };

    // Embout du bâton (6 lignes, posé en haut à droite, colonnes 15 à 18), par élément
    private static final String[][] TIPS = {
            {"..g.", ".gGg", "gGGg", "gGGg", ".gg.", "..T."},   // Eau : goutte
            {".G..", ".GgG", "gGgg", "gGGg", ".gg.", "..T."},   // Feu : flamme
            {"..g.", ".gGg", "gGGg", ".gGg", ".gg.", "..T."},   // Terre : feuille
            {"..gG", ".gG.", "gGGg", "..Gg", ".Gg.", "..T."},   // Foudre : éclair
    };
    private static final String[] SMALL_TIP = {"....", "....", "..g.", ".gGg", "..g.", "..T."};

    // Ourlet de la robe (ligne 16, colonnes 1 à 13), à partir du niveau 3
    private static final String[] HEMS = {
            "rGrrGYYrGrrGr",   // Eau : vaguelettes
            "rgrgrYYrgrgrr",   // Feu : braises
            "rrGrrYYrrGrrr",   // Terre : feuilles
            "rGrrrYYrrrGrr",   // Foudre : étincelles
    };

    // Couleurs propres à chaque élément : robe, robe ombrée, embout clair, embout foncé
    private static final int[][] ELEMENT_COLORS = {
            {Color.rgb(70, 140, 235), Color.rgb(40, 90, 175), Color.rgb(150, 220, 255), Color.rgb(60, 150, 230)},
            {Color.rgb(225, 80, 45), Color.rgb(160, 45, 30), Color.rgb(255, 220, 90), Color.rgb(255, 120, 30)},
            {Color.rgb(140, 105, 60), Color.rgb(95, 70, 40), Color.rgb(120, 190, 80), Color.rgb(70, 130, 50)},
            {Color.rgb(150, 80, 230), Color.rgb(95, 45, 165), Color.rgb(255, 245, 140), Color.rgb(250, 200, 40)},
    };

    public static final int MAGE_WIDTH = 19;
    public static final int MAGE_HEIGHT = 18;

    // Grille du mage d'un élément et d'un niveau : le design évolue à chaque fusion
    static String[] mageRows(int type, int level) {
        char[][] g = toGrid(level == 1 ? BODY_APPRENTICE : BODY_MAGE, MAGE_WIDTH);

        String[] tip = (level == 1) ? SMALL_TIP : TIPS[type];
        for (int i = 0; i < tip.length; i++) {
            for (int j = 0; j < tip[i].length(); j++) {
                if (tip[i].charAt(j) != '.') {
                    g[i][15 + j] = tip[i].charAt(j);
                }
            }
        }
        if (level >= 3) {                       // emblème sur le chapeau + ourlet décoré
            g[4][6] = 'G';
            for (int j = 0; j < HEMS[type].length(); j++) {
                g[16][1 + j] = HEMS[type].charAt(j);
            }
        }
        if (level >= 4) {                       // bord du chapeau doré, longue barbe, étincelles
            for (int x = 2; x <= 10; x += 2) {
                g[6][x] = 'Y';
            }
            g[13] = pad("KRRRRwWWwRRRRRKKKK", MAGE_WIDTH);
            g[14] = pad("KrRRRRwwRRRRRrK..Tt", MAGE_WIDTH);
            g[0][15] = 'Y';
            g[0][18] = 'Y';
            g[2][14] = 'Y';
        }
        if (level >= 5) {                       // grand mage : pointe et bande du chapeau dorées
            g[0][6] = 'Y';
            g[0][7] = 'Y';
            g[1][6] = 'Y';
            g[1][7] = 'Y';
            for (int x = 4; x <= 8; x++) {
                g[5][x] = 'Y';
            }
            g[5][6] = 'G';
        }
        return fromGrid(g);
    }

    // ---------- Gobelins : 19 x 16 pixels, 2 images de marche ----------
    // De jour : yeux jaunes. De nuit : torche dans la main, peau plus sombre, yeux rouges.

    private static final String[] GOBLIN_WALK_1 = {
            "...K..............K",
            "...KLK..KKKKKK..KLK",
            "....KLKKLLLLLLKKLK.",
            "....KLLLLLLLLLLLLK.",
            ".....KLLQELLQELLK..",
            ".....KLLQQLLQQLLK..",
            ".....KlLLLLLLLLlK..",
            "......KLFKFFKFLK...",
            ".......KKLLLLKK....",
            "......KLlLLLLlLK...",
            ".....KLKLLLLLLKLK..",
            ".....KLKPPPPPPKLK..",
            ".....KKKPPppPPKKK..",
            ".......KLK..KLK....",
            ".......KlK..KlK....",
            "......KKKK..KKKK...",
    };
    private static final String[] GOBLIN_WALK_2 = {
            "...K..............K",
            "...KLK..KKKKKK..KLK",
            "....KLKKLLLLLLKKLK.",
            "....KLLLLLLLLLLLLK.",
            ".....KLLQELLQELLK..",
            ".....KLLQQLLQQLLK..",
            ".....KlLLLLLLLLlK..",
            "......KLFKFFKFLK...",
            ".......KKLLLLKK....",
            "......KLlLLLLlLK...",
            ".....KLKLLLLLLKLK..",
            ".....KLKPPPPPPKLK..",
            ".....KKKPPppPPKKK..",
            "........KLKKLK.....",
            ".......KlK..KlK....",
            "......KKK....KKK...",
    };
    private static final String[] GOBLIN_TORCH_1 = {
            "...K.............oK",
            "...KLK..KKKKKK..OoO",
            "....KLKKLLLLLLKKOTO",
            "....KLLLLLLLLLLLLTt",
            ".....KLLQELLQELLKTt",
            ".....KLLQQLLQQLLKTt",
            ".....KlLLLLLLLLKLT.",
            "......KLFKFFKFLKLK.",
            ".......KKLLLLKKLKK.",
            "......KLlLLLLlLLK..",
            ".....KLKLLLLLLK....",
            ".....KLKPPPPPPK....",
            ".....KKKPPppPPK....",
            ".......KLK..KLK....",
            ".......KlK..KlK....",
            "......KKKK..KKKK...",
    };
    private static final String[] GOBLIN_TORCH_2 = {
            "...K.............oK",
            "...KLK..KKKKKK..OoO",
            "....KLKKLLLLLLKKOTO",
            "....KLLLLLLLLLLLLTt",
            ".....KLLQELLQELLKTt",
            ".....KLLQQLLQQLLKTt",
            ".....KlLLLLLLLLKLT.",
            "......KLFKFFKFLKLK.",
            ".......KKLLLLKKLKK.",
            "......KLlLLLLlLLK..",
            ".....KLKLLLLLLK....",
            ".....KLKPPPPPPK....",
            ".....KKKPPppPPK....",
            "........KLKKLK.....",
            ".......KlK..KlK....",
            "......KKK....KKK...",
    };

    public static final int GOBLIN_WIDTH = 19;
    public static final int GOBLIN_HEIGHT = 16;
    public static final float GOBLIN_CENTER_X = 11f;  // centre du corps (colonne), la torche dépasse à droite
    public static final float TORCH_FLAME_X = 17.5f;  // position de la flamme dans la grille
    public static final float TORCH_FLAME_Y = 1.3f;

    // ---------- Palettes ----------

    private static int[] commonPalette() {
        int[] p = new int[128];                 // case vide = 0 = transparent
        p['K'] = Color.rgb(26, 22, 38);         // contour
        p['E'] = Color.rgb(26, 22, 38);         // pupilles
        p['S'] = Color.rgb(242, 199, 155);      // peau
        p['s'] = Color.rgb(205, 150, 110);      // peau ombrée
        p['W'] = Color.rgb(236, 236, 240);      // barbe
        p['w'] = Color.rgb(176, 176, 192);      // barbe ombrée
        p['T'] = Color.rgb(139, 90, 43);        // bois
        p['t'] = Color.rgb(96, 60, 28);         // bois ombré
        p['Y'] = Color.rgb(240, 192, 64);       // or
        return p;
    }

    private static int[] goblinPalette(boolean night) {
        int[] p = commonPalette();
        p['L'] = night ? Color.rgb(70, 110, 50) : Color.rgb(120, 190, 70);   // peau verte
        p['l'] = night ? Color.rgb(45, 75, 32) : Color.rgb(75, 135, 45);     // peau ombrée
        p['Q'] = night ? Color.rgb(255, 50, 40) : Color.rgb(255, 230, 60);   // yeux : rouges la nuit
        p['P'] = night ? Color.rgb(95, 60, 35) : Color.rgb(140, 85, 45);     // pagne
        p['p'] = night ? Color.rgb(65, 40, 25) : Color.rgb(95, 55, 30);
        p['F'] = Color.rgb(245, 245, 230);                                   // crocs
        p['O'] = Color.rgb(255, 150, 40);                                    // flamme
        p['o'] = Color.rgb(255, 230, 120);                                   // cœur de la flamme
        return p;
    }

    // ---------- Construction des images ----------

    // mages[type][niveau - 1]
    public static Bitmap[][] buildMages() {
        Bitmap[][] mages = new Bitmap[ELEMENT_COLORS.length][5];
        for (int type = 0; type < ELEMENT_COLORS.length; type++) {
            int[] p = commonPalette();
            p['R'] = ELEMENT_COLORS[type][0];
            p['r'] = ELEMENT_COLORS[type][1];
            p['G'] = ELEMENT_COLORS[type][2];
            p['g'] = ELEMENT_COLORS[type][3];
            for (int level = 1; level <= 5; level++) {
                mages[type][level - 1] = toBitmap(mageRows(type, level), p);
            }
        }
        return mages;
    }

    // goblins[0 ou 1 : image de marche] ; de nuit, la variante avec la torche
    public static Bitmap[] buildGoblins(boolean night) {
        int[] p = goblinPalette(night);
        if (night) {
            return new Bitmap[]{toBitmap(GOBLIN_TORCH_1, p), toBitmap(GOBLIN_TORCH_2, p)};
        }
        return new Bitmap[]{toBitmap(GOBLIN_WALK_1, p), toBitmap(GOBLIN_WALK_2, p)};
    }

    private static Bitmap toBitmap(String[] rows, int[] palette) {
        int h = rows.length;
        int w = 0;
        for (String row : rows) {
            w = Math.max(w, row.length());
        }
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                char c = x < rows[y].length() ? rows[y].charAt(x) : '.';
                bmp.setPixel(x, y, c < 128 ? palette[c] : 0);
            }
        }
        return bmp;
    }

    // ---------- Outils de grille ----------

    private static char[] pad(String row, int width) {
        char[] r = new char[width];
        for (int x = 0; x < width; x++) {
            r[x] = x < row.length() ? row.charAt(x) : '.';
        }
        return r;
    }

    private static char[][] toGrid(String[] rows, int width) {
        char[][] g = new char[rows.length][];
        for (int y = 0; y < rows.length; y++) {
            g[y] = pad(rows[y], width);
        }
        return g;
    }

    private static String[] fromGrid(char[][] g) {
        String[] rows = new String[g.length];
        for (int y = 0; y < g.length; y++) {
            rows[y] = new String(g[y]);
        }
        return rows;
    }
}