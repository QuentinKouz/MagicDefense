package com.example.magicdefense.view;

import android.graphics.RectF;

import com.example.magicdefense.model.Achievement;
import com.example.magicdefense.model.Chest;
import com.example.magicdefense.model.GameModel;
import com.example.magicdefense.model.Ultimate;

// Contenu des listes qui défilent verticalement (pages Marché, Mages et Château) :
// une suite de lignes de hauteurs différentes, chacune avec ses boutons.
// Le dessin (GameRenderer) et les touchers (TouchController) utilisent la même description,
// donc un bouton est toujours touché là où il est dessiné.
public class MenuList {
    // Types de ligne
    public static final int HEADER = 0;        // titre de section
    public static final int MAGE = 1;          // (ancienne ligne de mage, plus utilisée)
    public static final int SHOP_CHEST = 2;    // Marché : une sorte de coffre (arg = Chest.xxx)
    public static final int FREE_CHEST = 3;    // Marché : petits coffres gratuits du jour
    public static final int SHOP_ULTIMATE = 4; // Marché : ultime
    public static final int SHOP_SPEED = 5;    // Marché : vitesse ×3
    public static final int CASTLE_HP = 6;     // Château : PV max
    public static final int CASTLE_RES = 7;    // Château : résistance
    public static final int ULTIMATE = 8;      // Château : ultime à équiper
    public static final int ACHIEVEMENT = 9;   // Château : succès
    public static final int INFO = 10;         // ligne de texte (consigne)
    public static final int SHOP_AUTO = 11;    // Marché : mode automatique
    public static final int TEAM_ROW = 12;     // Mages : 2 cartes de l'équipe (arg = 1er emplacement)
    public static final int CARD_ROW = 13;     // Mages : 3 cartes de la collection
    public static final int LOCKED_ROW = 14;   // Mages : 3 cartes à débloquer

    public static final int CARDS_PER_ROW = 3;
    public static final int EMPTY = 15;
    public static final float TEAM_ROW_H = 196;
    public static final float CARD_ROW_H = 158;

    // Boutons d'une ligne
    public static final int BTN_MAIN = 0;      // bouton de droite (acheter, +1 rang, équiper...)
    public static final int BTN_PORTRAIT = 1;  // (boutons des cartes et des coffres : 0 à 3)
    public static final int BTN_ARTIFACT = 2;  // bandeau de l'artefact (changer)

    private static final int MAX_ROWS = 64;
    public int count = 0;
    public final int[] kind = new int[MAX_ROWS];
    public final int[] arg = new int[MAX_ROWS];      // élément, ultime, succès...
    public final String[] text = new String[MAX_ROWS];
    public final int[] buttons = new int[MAX_ROWS];  // SHOP_CHEST : nombre de boutons (3 ou 2)
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
                add(FREE_CHEST, 0, null, 80);
                for (int c = 0; c < Chest.COUNT; c++) {
                    add(SHOP_CHEST, c, null, 142 + 17 * Chest.slots(c));
                    buttons[count - 1] = model.getProgression().getChests(c) > 0 ? 2 : 3;
                }
                add(INFO, 0, "Artefacts : 10 % de chances par niveau de boss gagné en 3 étoiles", 22);
                add(HEADER, 0, "Ultimes", 34);
                for (int u = 1; u < Ultimate.COUNT; u++) {
                    add(SHOP_ULTIMATE, u, null, 88);
                }
                add(HEADER, 0, "Bonus", 34);
                add(SHOP_SPEED, 2, null, 88);   // vitesse ×2
                add(SHOP_SPEED, 3, null, 88);   // vitesse ×3 (après ×2)
                add(SHOP_AUTO, 0, null, 88);    // mode automatique
                break;
            case GameModel.PAGE_MAGES: {
                boolean swapping = model.getSwappingMage() >= 0;
                add(INFO, 0, swapping ? "Touchez le mage de l'équipe à remplacer"
                        : "Touchez une carte pour l'améliorer ou l'utiliser", 22);
                add(HEADER, 0, "Équipe", 34);
                add(TEAM_ROW, 0, null, TEAM_ROW_H);   // emplacements 0 et 1
                add(TEAM_ROW, 2, null, TEAM_ROW_H);   // emplacements 2 et 3
                int[] coll = model.collectionTypes();
                add(HEADER, 0, "Collection (" + coll.length + ")", 34);
                addCardRows(CARD_ROW, coll);
                if (coll.length == 0) {
                    add(INFO, 0, "Tous vos mages sont dans l'équipe", 22);
                }
                int[] locked = model.lockedTypes();
                if (locked.length > 0) {
                    add(HEADER, 0, "À débloquer (" + locked.length + ")", 34);
                    addCardRows(LOCKED_ROW, locked);
                    add(INFO, 0, "Les nouveaux mages se trouvent dans les coffres (Marché)", 22);
                }
                break;
            }
            case GameModel.PAGE_CASTLE:
                add(HEADER, 0, "Défenses", 34);
                add(CASTLE_HP, 0, null, 84);
                add(CASTLE_RES, 0, null, 84);
                add(HEADER, 0, "Ultime équipé (toucher la jauge en partie)", 34);
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

    // Cartes par rangées de 3 ; arg = les 3 types (4 bits chacun, EMPTY = case vide)
    private void addCardRows(int kind, int[] types) {
        for (int i = 0; i < types.length; i += CARDS_PER_ROW) {
            int packed = 0;
            for (int k = 0; k < CARDS_PER_ROW; k++) {
                int t = i + k < types.length ? types[i + k] : EMPTY;
                packed |= t << (4 * k);
            }
            add(kind, packed, null, CARD_ROW_H);
        }
    }

    // Type de la carte n° b d'une rangée (TEAM_ROW : type de l'emplacement), ou -1
    public int cardType(int i, int b, GameModel model) {
        if (kind[i] == TEAM_ROW) {
            return b < 2 ? model.teamTypeAt(arg[i] + b) : -1;
        }
        if (kind[i] == CARD_ROW || kind[i] == LOCKED_ROW) {
            int t = b < CARDS_PER_ROW ? (arg[i] >> (4 * b)) & 15 : EMPTY;
            return t == EMPTY ? -1 : t;
        }
        return -1;
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
    //  - TEAM_ROW : 2 cartes (boutons 0 et 1) ; CARD_ROW / LOCKED_ROW : 3 cartes (0 à 2)
    //  - SHOP_CHEST : en bas, ×1 / ×5 / ×10 (aucun en réserve) ou OUVRIR / TOUT OUVRIR
    //  - les autres lignes : un bouton à droite (BTN_MAIN)
    public boolean buttonRect(int i, int button, RectF r, RectF out) {
        float d = density;
        int k = kind[i];
        if (k == TEAM_ROW || k == CARD_ROW || k == LOCKED_ROW) {
            int n = k == TEAM_ROW ? 2 : CARDS_PER_ROW;
            if (button >= n) {
                return false;
            }
            float gap = 8 * d;
            float w = (r.width() - (n - 1) * gap) / n;
            float left = r.left + button * (w + gap);
            out.set(left, r.top, left + w, r.bottom);
            return true;
        }
        if (k == SHOP_CHEST) {
            int n = buttons[i];
            if (button >= n) {
                return false;
            }
            float gap = 8 * d;
            float inner = r.width() - 20 * d;
            float w = (inner - (n - 1) * gap) / n;
            float left = r.left + 10 * d + button * (w + gap);
            out.set(left, r.bottom - 56 * d, left + w, r.bottom - 10 * d);
            return true;
        }
        if (button != BTN_MAIN || k == HEADER || k == ACHIEVEMENT || k == INFO) {
            return false;
        }
        float w = 88 * d;
        float cy = r.centerY();
        out.set(r.right - w - 8 * d, cy - 24 * d, r.right - 8 * d, cy + 24 * d);
        return true;
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
            for (int b = 0; b < 4; b++) {
                if (buttonRect(i, b, tmpRow, tmpBtn) && tmpBtn.contains(x, y)) {
                    return i * 4 + b;
                }
            }
            return -1;
        }
        return -1;
    }
}
