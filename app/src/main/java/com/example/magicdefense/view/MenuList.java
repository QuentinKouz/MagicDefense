package com.example.magicdefense.view;

import android.graphics.RectF;

import com.example.magicdefense.model.Achievement;
import com.example.magicdefense.model.GameModel;
import com.example.magicdefense.model.Mage;
import com.example.magicdefense.model.Ultimate;

// Contenu des listes qui défilent verticalement (pages Marché, Mages et Château) :
// une suite de lignes de hauteurs différentes, chacune avec ses boutons.
// Le dessin (GameRenderer) et les touchers (TouchController) utilisent la même description,
// donc un bouton est toujours touché là où il est dessiné.
public class MenuList {
    // Types de ligne
    public static final int HEADER = 0;        // titre de section
    public static final int MAGE = 1;          // page Mages : un élément possédé
    public static final int SHOP_CHEST = 2;    // Marché : un coffre
    public static final int SHOP_UNIT = 3;     // Marché : unité exclusive
    public static final int SHOP_ULTIMATE = 4; // Marché : ultime
    public static final int SHOP_SPEED = 5;    // Marché : vitesse ×3
    public static final int CASTLE_HP = 6;     // Château : PV max
    public static final int CASTLE_RES = 7;    // Château : résistance
    public static final int ULTIMATE = 8;      // Château : ultime à équiper
    public static final int ACHIEVEMENT = 9;   // Château : succès
    public static final int INFO = 10;         // ligne de texte (consigne)

    // Boutons d'une ligne
    public static final int BTN_MAIN = 0;      // bouton de droite (acheter, +1 rang, équiper...)
    public static final int BTN_PORTRAIT = 1;  // portrait du mage (équipe)
    public static final int BTN_ARTIFACT = 2;  // bandeau de l'artefact (changer)

    private static final int MAX_ROWS = 64;
    public int count = 0;
    public final int[] kind = new int[MAX_ROWS];
    public final int[] arg = new int[MAX_ROWS];      // élément, ultime, succès...
    public final String[] text = new String[MAX_ROWS];
    private final float[] top = new float[MAX_ROWS];   // en dp depuis le haut du contenu
    private final float[] height = new float[MAX_ROWS];
    private float contentHeight = 0;
    private final float density;

    public MenuList(float density) {
        this.density = density;
    }

    private void add(int k, int a, String t, float hDp) {
        if (count >= MAX_ROWS) {
            return;
        }
        kind[count] = k;
        arg[count] = a;
        text[count] = t;
        top[count] = contentHeight;
        height[count] = hDp;
        contentHeight += hDp + 8;
        count++;
    }

    // Construit les lignes de la page (à refaire à chaque image : le contenu change avec les achats)
    public void build(int page, GameModel model) {
        count = 0;
        contentHeight = 0;
        switch (page) {
            case GameModel.PAGE_MARKET:
                add(HEADER, 0, "Coffres", 34);
                add(SHOP_CHEST, 0, null, 88);
                add(HEADER, 0, "Unités exclusives", 34);
                for (int t = Mage.BASE_TYPES; t < Mage.TYPE_COUNT; t++) {
                    add(SHOP_UNIT, t, null, 88);
                }
                add(HEADER, 0, "Ultimes", 34);
                for (int u = 1; u < Ultimate.COUNT; u++) {
                    add(SHOP_ULTIMATE, u, null, 88);
                }
                add(HEADER, 0, "Bonus", 34);
                add(SHOP_SPEED, 2, null, 88);   // vitesse ×2
                add(SHOP_SPEED, 3, null, 88);   // vitesse ×3 (après ×2)
                break;
            case GameModel.PAGE_MAGES:
                add(INFO, 0, "Portrait : équipe · bandeau : artefact", 22);
                for (int row = 0; ; row++) {
                    int type = model.ownedTypeAt(row);
                    if (type < 0) {
                        break;
                    }
                    add(MAGE, type, null, 124);
                }
                add(INFO, 0, "Artefacts : 1er succès sur un niveau de boss", 22);
                break;
            case GameModel.PAGE_CASTLE:
                add(HEADER, 0, "Défenses", 34);
                add(CASTLE_HP, 0, null, 84);
                add(CASTLE_RES, 0, null, 84);
                add(HEADER, 0, "Ultime équipé (secousse)", 34);
                for (int u = 0; u < Ultimate.COUNT; u++) {
                    add(ULTIMATE, u, null, 76);
                }
                add(HEADER, 0, "Succès " + model.getProgression().achievedCount() + " / " + Achievement.COUNT, 34);
                for (int a = 0; a < Achievement.COUNT; a++) {
                    add(ACHIEVEMENT, a, null, 56);
                }
                break;
            default:
                break;
        }
    }

    public float getContentHeight() {
        return contentHeight * density;
    }

    // Défilement maximal pour une zone visible de hauteur viewportH
    public float maxScroll(float viewportH) {
        return Math.max(0, getContentHeight() - viewportH + 16 * density);
    }

    // Rectangle de la ligne i à l'écran
    public RectF rowRect(int i, RectF viewport, float scroll, RectF out) {
        float d = density;
        out.set(viewport.left, viewport.top + top[i] * d - scroll,
                viewport.right, viewport.top + (top[i] + height[i]) * d - scroll);
        return out;
    }

    // Rectangle d'un bouton de la ligne (r = rectangle de la ligne). Retourne false si ce bouton n'existe pas.
    public boolean buttonRect(int i, int button, RectF r, RectF out) {
        float d = density;
        int k = kind[i];
        if (button == BTN_MAIN) {
            if (k == HEADER || k == ACHIEVEMENT || k == INFO) {
                return false;
            }
            float w = 88 * d;
            float cy = k == MAGE ? r.top + 36 * d : r.centerY();
            out.set(r.right - w - 8 * d, cy - 24 * d, r.right - 8 * d, cy + 24 * d);
            return true;
        }
        if (k != MAGE) {
            return false;
        }
        if (button == BTN_PORTRAIT) {
            out.set(r.left + 4 * d, r.top + 4 * d, r.left + 68 * d, r.top + 72 * d);
            return true;
        }
        if (button == BTN_ARTIFACT) {
            out.set(r.left + 6 * d, r.bottom - 44 * d, r.right - 6 * d, r.bottom - 6 * d);
            return true;
        }
        return false;
    }

    // Ligne et bouton sous le doigt : ligne × 4 + bouton, ou -1
    public int hit(float x, float y, RectF viewport, float scroll, RectF tmpRow, RectF tmpBtn) {
        if (!viewport.contains(x, y)) {
            return -1;
        }
        for (int i = 0; i < count; i++) {
            rowRect(i, viewport, scroll, tmpRow);
            if (y < tmpRow.top || y > tmpRow.bottom) {
                continue;
            }
            for (int b = BTN_MAIN; b <= BTN_ARTIFACT; b++) {
                if (buttonRect(i, b, tmpRow, tmpBtn) && tmpBtn.contains(x, y)) {
                    return i * 4 + b;
                }
            }
            return -1;
        }
        return -1;
    }
}
