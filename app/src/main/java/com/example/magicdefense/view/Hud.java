package com.example.magicdefense.view;

import android.graphics.RectF;

import com.example.magicdefense.model.Board;
import com.example.magicdefense.model.GameModel;
import com.example.magicdefense.model.Progression;

// Position des boutons et fenêtres de l'interface.
// Mode gaucher : l'interface est en miroir (pause / options / son à gauche, jauge à droite,
// ordre des boutons de recrutement inversé).
public class Hud {
    private final Board board;
    private boolean leftHanded = false;

    // Barre de recrutement : un bouton par emplacement d'équipe (indice = emplacement)
    public final RectF[] recruitButtons = new RectF[Progression.TEAM_SIZE];
    public final RectF pauseButton = new RectF();
    public float gaugeCenterX, gaugeCenterY; // jauge d'ultime
    public float gaugeRadius;

    // Boutons de la fenêtre de pause
    public static final int PAUSE_QUIT = 0;
    public static final int PAUSE_RESTART = 1;
    public static final int PAUSE_RESUME = 2;
    public static final int PAUSE_OPTIONS = 3;

    public final RectF pausePopup = new RectF();
    public final RectF[] pauseMenuButtons = new RectF[4];
    // Boutons de la fenêtre d'options
    public static final int OPTIONS_NIGHT_TOGGLE = 0;
    public static final int OPTIONS_BACK = 1;
    public static final int OPTIONS_VOLUME = 2;
    public static final int OPTIONS_MUSIC_TOGGLE = 3;
    public static final int OPTIONS_LEFT_HANDED = 4;

    public final RectF optionsPopup = new RectF();
    public final RectF[] optionsButtons = new RectF[5];
    public final RectF volumeTrack = new RectF(); // rail du curseur (seuls left, right et centerY servent)
    // Hauteur (depuis le haut de la fenêtre) du titre de chaque réglage, en dp
    public static final float OPT_NIGHT_Y = 95, OPT_MUSIC_Y = 185, OPT_LEFT_Y = 275, OPT_VOLUME_Y = 365;

    // Boutons de l'écran Victoire
    public static final int VICTORY_NEXT = 0;
    public static final int VICTORY_MENU = 1;
    // Écran Game Over : même fenêtre et mêmes boutons que la victoire
    public static final int GAMEOVER_RETRY = VICTORY_NEXT;  // bouton du haut
    public static final int GAMEOVER_MENU = VICTORY_MENU;   // bouton du bas

    public final RectF victoryPopup = new RectF();
    public final RectF[] victoryButtons = new RectF[2];

    // Carte des niveaux (écran d'accueil)
    public static final int HOME_PLAY = 0;  // bouton JOUER ; les niveaux utilisent leur emplacement (1 à 6)
    public final RectF playButton = new RectF();
    public final RectF[] levelNodes = new RectF[GameModel.LEVELS_PER_PAGE + 1]; // index = emplacement dans la page

    public static final int HOME_OPTIONS = 100; // valeurs hors des emplacements de niveau (1 à 6)
    public static final int HOME_SOUND = 101;
    public static final int HOME_PREV_PAGE = 102;
    public static final int HOME_NEXT_PAGE = 103;
    public static final int HOME_ENDLESS = 104;
    public static final int HOME_UPGRADES = 105;
    public static final int HOME_MARKET = 106;
    public final RectF homeOptionsButton = new RectF();
    public final RectF homeSoundButton = new RectF();
    public final RectF prevPageButton = new RectF();
    public final RectF nextPageButton = new RectF();
    public final RectF endlessButton = new RectF();
    public final RectF upgradesButton = new RectF();
    public final RectF marketButton = new RectF();

    // Écran MAGES : 4 lignes par page. Indices des boutons :
    //   0 à 3 : +1 RANG de la ligne ; 4 : OUVRIR (coffre) ; 5 : RETOUR ; 6-7 : pages ; 8 à 11 : portrait (équipe)
    public static final int UPGRADE_ROWS = GameModel.UPGRADE_ROWS_PER_PAGE;
    public static final int UPGRADE_OPEN_CHEST = 4;
    public static final int UPGRADE_BACK = 5;
    public static final int UPGRADE_PREV = 6;
    public static final int UPGRADE_NEXT = 7;
    public static final int UPGRADE_TEAM_0 = 8;
    public final RectF upgradesPopup = new RectF();
    public final RectF[] upgradeButtons = new RectF[UPGRADE_TEAM_0 + UPGRADE_ROWS];
    public final RectF[] upgradeRows = new RectF[UPGRADE_ROWS]; // ligne de chaque élément (dessin)
    public final RectF chestRow = new RectF();

    // Animation d'ouverture de coffre : OUVRIR ENCORE (le reste de l'écran = continuer)
    public static final int CHEST_AGAIN = 0;
    public static final int CHEST_CONTINUE = 1;
    public final RectF chestAgainButton = new RectF();

    // Marché : une ligne par unité exclusive, puis RETOUR
    public static final int MARKET_ROWS = 3;
    public static final int MARKET_BACK = MARKET_ROWS;
    public final RectF marketPopup = new RectF();
    public final RectF[] marketRows = new RectF[MARKET_ROWS];
    public final RectF[] marketButtons = new RectF[MARKET_ROWS + 1];

    // Position de chaque niveau : x en fraction de la colonne de jeu, y en fraction de l'écran
    // (resize() étire ensuite ces hauteurs entre le titre et le bouton JOUER) ; zigzag du bas vers le haut
    private static final float[][] NODE_POS = {
            {0.30f, 0.76f},  // niveau 1
            {0.65f, 0.66f},
            {0.35f, 0.56f},
            {0.68f, 0.46f},
            {0.35f, 0.36f},
            {0.55f, 0.24f}   // niveau 6 (couronne)
    };

    public Hud(Board board) {
        this.board = board;
        init(recruitButtons);
        init(pauseMenuButtons);
        init(optionsButtons);
        init(victoryButtons);
        init(levelNodes);
        init(upgradeButtons);
        init(upgradeRows);
        init(marketRows);
        init(marketButtons);
    }

    private static void init(RectF[] rects) {
        for (int i = 0; i < rects.length; i++) {
            rects[i] = new RectF();
        }
    }

    public boolean isLeftHanded() {
        return leftHanded;
    }

    // Change de main ; il faut ensuite rappeler resize()
    public void setLeftHanded(boolean on) {
        leftHanded = on;
    }

    // À appeler après board.resize().
    // Tout est placé dans la colonne de jeu (board.playLeft → board.playRight()),
    // et le haut de l'écran est décalé de board.topShift pour éviter la caméra.
    public void resize(int w, int h) {
        float d = board.density;
        float left = board.playLeft;
        float right = board.playRight();
        float cw = board.playWidth;
        float cx = board.centerX();
        float shift = board.topShift;

        // Boutons de recrutement : 4 côte à côte en bas de la colonne (ordre inversé pour les gauchers)
        float margin = 6 * d;
        int n = recruitButtons.length;
        float buttonWidth = cw / n;
        float buttonTop = h - board.bottomBarHeight + margin;
        float buttonBottom = buttonTop + 60 * d; // grande cible pour le doigt
        for (int i = 0; i < n; i++) {
            int pos = leftHanded ? n - 1 - i : i;
            recruitButtons[i].set(left + pos * buttonWidth + margin, buttonTop,
                    left + (pos + 1) * buttonWidth - margin, buttonBottom);
        }

        // Bouton pause rond en haut à droite (à gauche pour les gauchers), aligné avec la jauge d'ultime
        float size = 56 * d;   // grande cible pour le doigt
        float side = 16 * d;
        float top = 40 * d + shift;
        float bx = leftHanded ? left + side : right - side - size;
        pauseButton.set(bx, top, bx + size, top + size);

        // Jauge d'ultime : du côté opposé
        gaugeRadius = 28 * d;
        gaugeCenterX = leftHanded ? right - 16 * d - gaugeRadius : left + 16 * d + gaugeRadius;
        gaugeCenterY = 40 * d + shift + gaugeRadius;

        // Accueil : options et son l'un sous l'autre, du même côté que la pause
        homeOptionsButton.set(pauseButton);
        homeSoundButton.set(bx, top + size + 12 * d, bx + size, top + 2 * size + 12 * d);

        // Fenêtres : même largeur pour toutes, centrées dans la colonne
        float pw = cw * 0.78f;
        float pLeft = cx - pw / 2;

        // Fenêtre de pause : PAUSE, QUITTER, puis 3 boutons ronds
        float ph = 240 * d;
        float pTop = popupTop(h, ph);
        pausePopup.set(pLeft, pTop, pLeft + pw, pTop + ph);

        // QUITTER : bouton large, séparé des autres
        float quitW = pw * 0.7f;
        float quitTop = pTop + 72 * d;
        pauseMenuButtons[PAUSE_QUIT].set(cx - quitW / 2, quitTop, cx + quitW / 2, quitTop + 52 * d);

        // Recommencer, Reprendre, Options : ronds, côte à côte et bien espacés
        float diameter = 64 * d;
        float rowY = pTop + 180 * d;
        float spacing = pw * 0.28f;
        for (int i = 0; i < 3; i++) {
            float px = cx + (i - 1) * spacing;
            pauseMenuButtons[PAUSE_RESTART + i].set(px - diameter / 2, rowY - diameter / 2,
                    px + diameter / 2, rowY + diameter / 2);
        }

        // Fenêtre d'options : mode nuit, musique, mode gaucher, volume, retour
        float oH = 525 * d;
        float oTop = popupTop(h, oH);
        optionsPopup.set(pLeft, oTop, pLeft + pw, oTop + oH);

        // Interrupteurs : toute la ligne "Non [===] Oui" est touchable (grande cible)
        toggleRow(optionsButtons[OPTIONS_NIGHT_TOGGLE], pLeft, pw, oTop, OPT_NIGHT_Y);
        toggleRow(optionsButtons[OPTIONS_MUSIC_TOGGLE], pLeft, pw, oTop, OPT_MUSIC_Y);
        toggleRow(optionsButtons[OPTIONS_LEFT_HANDED], pLeft, pw, oTop, OPT_LEFT_Y);

        // Volume : toute la ligne est touchable ; le rail laisse la place aux haut-parleurs
        optionsButtons[OPTIONS_VOLUME].set(pLeft + 16 * d, oTop + (OPT_VOLUME_Y + 5) * d,
                pLeft + pw - 16 * d, oTop + (OPT_VOLUME_Y + 75) * d);
        float railY = oTop + (OPT_VOLUME_Y + 40) * d;
        volumeTrack.set(pLeft + 60 * d, railY, pLeft + pw - 60 * d, railY);

        // RETOUR
        float backW = pw * 0.6f;
        optionsButtons[OPTIONS_BACK].set(cx - backW / 2, oTop + oH - 70 * d, cx + backW / 2, oTop + oH - 18 * d);

        // Fenêtre de victoire (et de game over) : titre, 3 étoiles, coffres, puis 2 boutons
        float vH = 340 * d;
        float vTop = popupTop(h, vH);
        victoryPopup.set(pLeft, vTop, pLeft + pw, vTop + vH);

        float vBtnW = pw * 0.75f;
        victoryButtons[VICTORY_NEXT].set(cx - vBtnW / 2, vTop + 206 * d, cx + vBtnW / 2, vTop + 258 * d);
        victoryButtons[VICTORY_MENU].set(cx - vBtnW / 2, vTop + 272 * d, cx + vBtnW / 2, vTop + 324 * d);

        // JOUER : large, en bas (zone facile au pouce)
        float playW = cw * 0.6f;
        playButton.set(cx - playW / 2, h - 150 * d, cx + playW / 2, h - 86 * d);

        // Sous JOUER : INFINI, MAGES, MARCHÉ côte à côte
        float rowW = cw * 0.92f;
        float gap = 8 * d;
        float smallW = (rowW - 2 * gap) / 3;
        float rowLeft = cx - rowW / 2;
        endlessButton.set(rowLeft, h - 76 * d, rowLeft + smallW, h - 24 * d);
        upgradesButton.set(rowLeft + smallW + gap, h - 76 * d, rowLeft + 2 * smallW + gap, h - 24 * d);
        marketButton.set(rowLeft + 2 * (smallW + gap), h - 76 * d, rowLeft + rowW, h - 24 * d);

        // Carte : les niveaux occupent l'espace entre le titre et le bouton JOUER,
        // quelle que soit la hauteur de l'écran (petit téléphone, tablette en paysage...)
        int last = GameModel.LEVELS_PER_PAGE;
        float mapTop = Math.max(h * NODE_POS[last - 1][1], 205 * d + shift);
        float mapBottom = Math.max(mapTop + 1, playButton.top - 66 * d);
        float fyTop = NODE_POS[last - 1][1];
        float fyBottom = NODE_POS[0][1];
        float nodeSize = 72 * d; // zone touchable de 72 dp (le cercle dessiné en fait 56)
        for (int slot = 1; slot <= last; slot++) {
            float nx = left + cw * NODE_POS[slot - 1][0];
            float t = (NODE_POS[slot - 1][1] - fyTop) / (fyBottom - fyTop); // 0 en haut, 1 en bas
            float ny = mapTop + t * (mapBottom - mapTop);
            levelNodes[slot].set(nx - nodeSize / 2, ny - nodeSize / 2, nx + nodeSize / 2, ny + nodeSize / 2);
        }

        // Flèches de page : sur les bords, à mi-hauteur de la carte (les niveaux sont au centre)
        float arrow = 48 * d;
        float arrowY = (mapTop + mapBottom) / 2;
        prevPageButton.set(left + 8 * d, arrowY - arrow / 2, left + 8 * d + arrow, arrowY + arrow / 2);
        nextPageButton.set(right - 8 * d - arrow, arrowY - arrow / 2, right - 8 * d, arrowY + arrow / 2);

        // Écran MAGES : titre (avec les flèches de page), coffres, 4 lignes, RETOUR
        float uw = cw * 0.92f;
        float uLeft = cx - uw / 2;
        float uH = 580 * d;
        float uTop = popupTop(h, uH);
        upgradesPopup.set(uLeft, uTop, uLeft + uw, uTop + uH);
        float btnW = 84 * d;
        float pad = 12 * d;


        chestRow.set(uLeft + pad, uTop + 64 * d, uLeft + uw - pad, uTop + 136 * d);
        upgradeButtons[UPGRADE_OPEN_CHEST].set(chestRow.right - btnW, chestRow.centerY() - 26 * d,
                chestRow.right, chestRow.centerY() + 26 * d);

        float rowsTop = uTop + 184 * d; // sous la consigne
        float rowH = 76 * d;
        for (int i = 0; i < UPGRADE_ROWS; i++) {
            float rTop = rowsTop + i * rowH;
            upgradeRows[i].set(uLeft + pad, rTop, uLeft + uw - pad, rTop + rowH - 8 * d);
            RectF r = upgradeRows[i];
            upgradeButtons[i].set(r.right - btnW, r.centerY() - 26 * d, r.right, r.centerY() + 26 * d);
            // Portrait : toucher le mage l'ajoute à l'équipe ou l'en retire
            upgradeButtons[UPGRADE_TEAM_0 + i].set(r.left, r.top, r.left + r.height(), r.bottom);
        }

        float uBackW = uw * 0.5f;
        upgradeButtons[UPGRADE_BACK].set(cx - uBackW / 2, uTop + uH - 70 * d,
                cx + uBackW / 2, uTop + uH - 18 * d);
        // Pages : flèches de part et d'autre de RETOUR (en bas, zone du pouce)
        float ay = upgradeButtons[UPGRADE_BACK].centerY() - 2 * d;
        upgradeButtons[UPGRADE_PREV].set(uLeft + 16 * d, ay - arrow / 2, uLeft + 16 * d + arrow, ay + arrow / 2);
        upgradeButtons[UPGRADE_NEXT].set(uLeft + uw - 16 * d - arrow, ay - arrow / 2, uLeft + uw - 16 * d, ay + arrow / 2);

        // Animation de coffre : OUVRIR ENCORE en bas, zone du pouce
        float againW = cw * 0.7f;
        chestAgainButton.set(cx - againW / 2, h - 170 * d, cx + againW / 2, h - 118 * d);

        // Marché : titre, gemmes, 3 unités exclusives, RETOUR
        float mH = 470 * d;
        float mTop = popupTop(h, mH);
        marketPopup.set(uLeft, mTop, uLeft + uw, mTop + mH);
        float mRowH = 96 * d;
        for (int i = 0; i < MARKET_ROWS; i++) {
            float rTop = mTop + 100 * d + i * mRowH;
            marketRows[i].set(uLeft + pad, rTop, uLeft + uw - pad, rTop + mRowH - 10 * d);
            RectF r = marketRows[i];
            marketButtons[i].set(r.right - btnW - 6 * d, r.centerY() - 25 * d, r.right - 6 * d, r.centerY() + 25 * d);
        }
        marketButtons[MARKET_BACK].set(cx - uBackW / 2, mTop + mH - 70 * d, cx + uBackW / 2, mTop + mH - 18 * d);
    }

    // Ligne d'interrupteur, sous le titre du réglage (titleY en dp depuis le haut de la fenêtre)
    private void toggleRow(RectF r, float pLeft, float pw, float oTop, float titleY) {
        float d = board.density;
        r.set(pLeft + 16 * d, oTop + (titleY + 20) * d, pLeft + pw - 16 * d, oTop + (titleY + 70) * d);
    }

    // Haut d'une fenêtre centrée vers 40 % de l'écran, sans dépasser en haut ni en bas
    private float popupTop(int h, float popupHeight) {
        float top = h * 0.4f - popupHeight / 2;
        float minTop = board.topShift + 16 * board.density;
        float maxTop = h - popupHeight - 16 * board.density;
        return Math.max(minTop, Math.min(top, maxTop));
    }

    private static int indexAt(RectF[] rects, float x, float y) {
        for (int i = 0; i < rects.length; i++) {
            if (rects[i].contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    // Emplacement de recrutement sous le point (x, y), ou -1
    public int buttonAt(float x, float y) {
        return indexAt(recruitButtons, x, y);
    }

    public boolean isOnPauseButton(float x, float y) {
        return pauseButton.contains(x, y);
    }

    public int pauseMenuButtonAt(float x, float y) {
        return indexAt(pauseMenuButtons, x, y);
    }

    public int optionsButtonAt(float x, float y) {
        return indexAt(optionsButtons, x, y);
    }

    public int victoryButtonAt(float x, float y) {
        return indexAt(victoryButtons, x, y);
    }

    public int upgradeButtonAt(float x, float y) {
        return indexAt(upgradeButtons, x, y);
    }

    public int marketButtonAt(float x, float y) {
        return indexAt(marketButtons, x, y);
    }

    // Animation de coffre : OUVRIR ENCORE, ou n'importe où ailleurs pour continuer
    public int chestButtonAt(float x, float y) {
        return chestAgainButton.contains(x, y) ? CHEST_AGAIN : CHEST_CONTINUE;
    }

    // HOME_xxx, emplacement du niveau touché (1 à 6), ou -1
    public int homeButtonAt(float x, float y) {
        if (homeOptionsButton.contains(x, y)) {
            return HOME_OPTIONS;
        }
        if (homeSoundButton.contains(x, y)) {
            return HOME_SOUND;
        }
        if (playButton.contains(x, y)) {
            return HOME_PLAY;
        }
        if (endlessButton.contains(x, y)) {
            return HOME_ENDLESS;
        }
        if (upgradesButton.contains(x, y)) {
            return HOME_UPGRADES;
        }
        if (marketButton.contains(x, y)) {
            return HOME_MARKET;
        }
        if (prevPageButton.contains(x, y)) {
            return HOME_PREV_PAGE;
        }
        if (nextPageButton.contains(x, y)) {
            return HOME_NEXT_PAGE;
        }
        for (int slot = 1; slot < levelNodes.length; slot++) {
            if (levelNodes[slot].contains(x, y)) {
                return slot;
            }
        }
        return -1;
    }

    // Volume (de 0 à 1) correspondant à une position horizontale sur le rail
    public float volumeAt(float x) {
        float v = (x - volumeTrack.left) / (volumeTrack.right - volumeTrack.left);
        return Math.max(0f, Math.min(1f, v));
    }
}