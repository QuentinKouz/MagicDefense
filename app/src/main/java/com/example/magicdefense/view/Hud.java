package com.example.magicdefense.view;

import android.graphics.RectF;

import com.example.magicdefense.model.Board;
import com.example.magicdefense.model.GameModel;
import com.example.magicdefense.model.LevelSpec;
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
    // En jeu : vitesse et mode automatique (sous le son), soin et mage au hasard (sous la jauge)
    public final RectF speedButton = new RectF();
    public final RectF autoButton = new RectF();
    public final RectF healButton = new RectF();
    public final RectF randomButton = new RectF();
    public static final int GAME_PAUSE = 0;
    public static final int GAME_SOUND = 1;
    public static final int GAME_SPEED = 2;
    public static final int GAME_AUTO = 3;
    public static final int GAME_HEAL = 4;
    public static final int GAME_RANDOM = 5;
    public static final int GAME_ULTIMATE = 6;  // toucher la jauge lance l'ultime
    public static final int GAME_LAUNCH = 7;    // « Lancer la vague »
    public final RectF launchButton = new RectF();

    // Boutons de la fenêtre de pause
    public static final int PAUSE_QUIT = 0;
    public static final int PAUSE_RESTART = 1;
    public static final int PAUSE_RESUME = 2;
    public static final int PAUSE_OPTIONS = 3;
    public static final int PAUSE_VIBRATION = 4;

    public final RectF pausePopup = new RectF();
    public final RectF[] pauseMenuButtons = new RectF[5];
    public static final float PAUSE_VIBRATION_Y = 168; // titre du réglage, en dp depuis le haut de la fenêtre
    // Boutons de la fenêtre d'options
    public static final int OPTIONS_VIBRATION = 0;
    public static final int OPTIONS_BACK = 1;
    public static final int OPTIONS_VOLUME = 2;
    public static final int OPTIONS_MUSIC_TOGGLE = 3;
    public static final int OPTIONS_LEFT_HANDED = 4;

    public final RectF optionsPopup = new RectF();
    public final RectF[] optionsButtons = new RectF[5];
    public final RectF volumeTrack = new RectF(); // rail du curseur (seuls left, right et centerY servent)
    // Hauteur (depuis le haut de la fenêtre) du titre de chaque réglage, en dp
    public static final float OPT_VIBRATION_Y = 95, OPT_MUSIC_Y = 185, OPT_LEFT_Y = 275, OPT_VOLUME_Y = 365;

    // Boutons de l'écran Victoire
    public static final int VICTORY_NEXT = 0;
    public static final int VICTORY_MENU = 1;
    public static final int VICTORY_RESTART = 2;
    // Écran Game Over : même fenêtre et mêmes boutons que la victoire
    public static final int GAMEOVER_RETRY = VICTORY_NEXT;  // bouton du haut
    public static final int GAMEOVER_MENU = 3;             // bouton du bas (pleine largeur)

    public final RectF victoryPopup = new RectF();
    public final RectF[] victoryButtons = new RectF[4];

    // ---------- Accueil : pages parcourues en glissant, barre d'onglets en bas ----------
    public final RectF tabBar = new RectF();
    public final RectF[] tabButtons = new RectF[GameModel.PAGE_COUNT];

    // Page Campagne : carte des niveaux
    public static final int HOME_PLAY = 0;  // bouton JOUER ; les niveaux utilisent leur emplacement (1 à 6)
    public final RectF playButton = new RectF();
    public final RectF[] levelNodes = new RectF[GameModel.LEVELS_PER_PAGE + 1]; // index = emplacement dans la page

    public static final int HOME_OPTIONS = 100; // valeurs hors des emplacements de niveau (1 à 6)
    public static final int HOME_SOUND = 101;
    public static final int HOME_PREV_PAGE = 102;
    public static final int HOME_NEXT_PAGE = 103;
    public static final int HOME_TAB_0 = 110;    // onglets : 110 à 114
    public static final int HOME_SHEET_UPGRADE = 150; // fiche d'un mage (page Mages)
    public static final int HOME_SHEET_USE = 151;
    public static final int HOME_SHEET_ARTIFACT = 152;
    public static final int HOME_SHEET_CLOSE = 153;
    public static final int HOME_DIFF_0 = 130;   // page Infini : difficultés 130 à 133
    public static final int HOME_ENDLESS_PLAY = 140;
    public static final int HOME_LIST_0 = 1000;  // listes : 1000 + ligne × 4 + bouton
    public final RectF homeOptionsButton = new RectF();
    public final RectF homeSoundButton = new RectF();
    public final RectF prevPageButton = new RectF();
    public final RectF nextPageButton = new RectF();

    // Pages à liste (Marché, Mages, Château) : zone qui défile
    public final RectF listViewport = new RectF();
    public final RectF magesViewport = new RectF();
    // Fiche d'un mage : carte, statistiques, artefact, AMÉLIORER, UTILISER, FERMER
    public final RectF mageSheet = new RectF();
    public final RectF sheetCard = new RectF();
    public final RectF sheetArtifact = new RectF();
    public final RectF sheetUpgrade = new RectF();
    public final RectF sheetUse = new RectF();
    public final RectF sheetClose = new RectF();

    // Page Infini : panneau, difficultés, JOUER
    public final RectF endlessPanel = new RectF();
    public final RectF[] difficultyButtons = new RectF[LevelSpec.DIFFICULTY_COUNT];
    public final RectF endlessPlayButton = new RectF();

    // Animation d'ouverture de coffre : OUVRIR ENCORE (le reste de l'écran = continuer)
    public static final int CHEST_AGAIN = 0;
    public static final int CHEST_CONTINUE = 1;
    public final RectF chestAgainButton = new RectF();

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
        init(tabButtons);
        init(difficultyButtons);
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

        // Barre de recrutement : les 4 mages de l'équipe, puis le mage au hasard (ordre inversé pour les gauchers)
        float margin = 6 * d;
        int n = recruitButtons.length + 1;
        float buttonWidth = cw / n;
        float buttonTop = h - board.bottomBarHeight + margin;
        float buttonBottom = buttonTop + 60 * d; // grande cible pour le doigt
        for (int i = 0; i < n; i++) {
            RectF slotRect = i < recruitButtons.length ? recruitButtons[i] : randomButton;
            int pos = leftHanded ? n - 1 - i : i;
            slotRect.set(left + pos * buttonWidth + margin, buttonTop,
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

        // Son sous la pause ; puis vitesse et mode automatique (plus petits)
        homeOptionsButton.set(pauseButton);
        homeSoundButton.set(bx, top + size + 12 * d, bx + size, top + 2 * size + 12 * d);
        float small = 48 * d;
        float sx = bx + (size - small) / 2;
        float sy = homeSoundButton.bottom + 12 * d;
        speedButton.set(sx, sy, sx + small, sy + small);
        autoButton.set(sx, sy + small + 10 * d, sx + small, sy + 2 * small + 10 * d);
        // Sous la jauge (et la consigne « Touchez ! ») : soin
        float gx = gaugeCenterX - small / 2;
        float gy = gaugeCenterY + gaugeRadius + 34 * d;
        healButton.set(gx, gy, gx + small, gy + small);
        // « Lancer la vague » : au milieu du terrain
        float lw2 = cw * 0.62f;
        float ly = board.gridTop * 0.55f;
        launchButton.set(cx - lw2 / 2, ly - 30 * d, cx + lw2 / 2, ly + 30 * d);

        // Fenêtres : même largeur pour toutes, centrées dans la colonne
        float pw = cw * 0.78f;
        float pLeft = cx - pw / 2;

        // Fenêtre de pause : PAUSE, QUITTER, vibrations, puis 3 boutons ronds
        float ph = 350 * d;
        float pTop = popupTop(h, ph);
        pausePopup.set(pLeft, pTop, pLeft + pw, pTop + ph);

        // QUITTER : bouton large, séparé des autres
        float quitW = pw * 0.7f;
        float quitTop = pTop + 72 * d;
        pauseMenuButtons[PAUSE_QUIT].set(cx - quitW / 2, quitTop, cx + quitW / 2, quitTop + 52 * d);

        // Recommencer, Reprendre, Options : ronds, côte à côte et bien espacés
        float diameter = 64 * d;
        toggleRow(pauseMenuButtons[PAUSE_VIBRATION], pLeft, pw, pTop, PAUSE_VIBRATION_Y);
        float rowY = pTop + 292 * d;
        float spacing = pw * 0.28f;
        for (int i = 0; i < 3; i++) {
            float px = cx + (i - 1) * spacing;
            pauseMenuButtons[PAUSE_RESTART + i].set(px - diameter / 2, rowY - diameter / 2,
                    px + diameter / 2, rowY + diameter / 2);
        }

        // Fenêtre d'options : vibrations, musique, mode gaucher, volume, retour
        float oH = 525 * d;
        float oTop = popupTop(h, oH);
        optionsPopup.set(pLeft, oTop, pLeft + pw, oTop + oH);

        // Interrupteurs : toute la ligne "Non [===] Oui" est touchable (grande cible)
        toggleRow(optionsButtons[OPTIONS_VIBRATION], pLeft, pw, oTop, OPT_VIBRATION_Y);
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

        // Fenêtre de victoire (et de game over) : titre, 3 étoiles, coffres, artefact, puis 2 boutons
        float vH = 380 * d;
        float vTop = popupTop(h, vH);
        victoryPopup.set(pLeft, vTop, pLeft + pw, vTop + vH);

        float vBtnW = pw * 0.75f;
        victoryButtons[VICTORY_NEXT].set(cx - vBtnW / 2, vTop + 246 * d, cx + vBtnW / 2, vTop + 298 * d);
        // Victoire : REJOUER et MENU côte à côte ; game over : MENU sur toute la largeur
        float gap = 6 * d;
        victoryButtons[VICTORY_RESTART].set(cx - vBtnW / 2, vTop + 312 * d, cx - gap, vTop + 364 * d);
        victoryButtons[VICTORY_MENU].set(cx + gap, vTop + 312 * d, cx + vBtnW / 2, vTop + 364 * d);
        victoryButtons[GAMEOVER_MENU].set(cx - vBtnW / 2, vTop + 312 * d, cx + vBtnW / 2, vTop + 364 * d);

        // ---------- Accueil ----------
        // Barre d'onglets en bas (zone du pouce)
        float tabH = 62 * d;
        tabBar.set(left, h - tabH - 6 * d, right, h - 6 * d);
        float tabW = cw / GameModel.PAGE_COUNT;
        for (int i = 0; i < GameModel.PAGE_COUNT; i++) {
            tabButtons[i].set(left + i * tabW + 2 * d, tabBar.top, left + (i + 1) * tabW - 2 * d, tabBar.bottom);
        }

        // Page Campagne : JOUER, juste au-dessus des onglets
        float playW = cw * 0.6f;
        playButton.set(cx - playW / 2, tabBar.top - 74 * d, cx + playW / 2, tabBar.top - 12 * d);

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

        // Pages à liste : sous le titre et les gemmes, au-dessus des onglets
        float lw = cw * 0.94f;
        float lLeft = cx - lw / 2;
        float listTop = shift + 112 * d;
        listViewport.set(lLeft, listTop, lLeft + lw, tabBar.top - 8 * d);
        magesViewport.set(listViewport);

        // Fiche d'un mage : grande carte à gauche, infos à droite, artefact, puis les boutons
        float sw = cw * 0.92f;
        float sH = 470 * d;
        float sTop = popupTop(h, sH);
        float sLeft = cx - sw / 2;
        mageSheet.set(sLeft, sTop, sLeft + sw, sTop + sH);
        float cardW = Math.min(124 * d, sw * 0.38f);
        sheetCard.set(sLeft + 16 * d, sTop + 62 * d, sLeft + 16 * d + cardW, sTop + 62 * d + cardW * 1.32f);
        float aTop = sheetCard.bottom + 14 * d;
        sheetArtifact.set(sLeft + 16 * d, aTop, sLeft + sw - 16 * d, aTop + 46 * d);
        float bTop = sTop + sH - 136 * d;
        float bw = (sw - 16 * d * 2 - 10 * d) / 2;
        sheetUpgrade.set(sLeft + 16 * d, bTop, sLeft + 16 * d + bw, bTop + 58 * d);
        sheetUse.set(sheetUpgrade.right + 10 * d, bTop, sLeft + sw - 16 * d, bTop + 58 * d);
        float closeW = sw * 0.5f;
        sheetClose.set(cx - closeW / 2, sTop + sH - 64 * d, cx + closeW / 2, sTop + sH - 16 * d);

        // Page Infini : panneau des records, difficultés en 2 × 2, JOUER
        endlessPlayButton.set(cx - playW / 2, tabBar.top - 74 * d, cx + playW / 2, tabBar.top - 12 * d);
        float epTop = shift + 112 * d;
        endlessPanel.set(lLeft, epTop, lLeft + lw, endlessPlayButton.top - 12 * d);
        float dW = (lw - 3 * 10 * d) / 2;
        float dH = 64 * d;
        float dTop = epTop + 70 * d;
        for (int i = 0; i < difficultyButtons.length; i++) {
            int c = i % 2;
            int r = i / 2;
            float x0 = lLeft + 10 * d + c * (dW + 10 * d);
            float y0 = dTop + r * (dH + 10 * d);
            difficultyButtons[i].set(x0, y0, x0 + dW, y0 + dH);
        }

        // Animation de coffre : OUVRIR ENCORE en bas, zone du pouce
        float againW = cw * 0.7f;
        chestAgainButton.set(cx - againW / 2, h - 170 * d, cx + againW / 2, h - 118 * d);
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

    // Boutons ronds de la partie (pause, son, vitesse, auto, soin, mage au hasard), ou -1
    public int gameButtonAt(float x, float y) {
        if (pauseButton.contains(x, y)) {
            return GAME_PAUSE;
        }
        if (homeSoundButton.contains(x, y)) {
            return GAME_SOUND;
        }
        if (speedButton.contains(x, y)) {
            return GAME_SPEED;
        }
        if (autoButton.contains(x, y)) {
            return GAME_AUTO;
        }
        if (healButton.contains(x, y)) {
            return GAME_HEAL;
        }
        if (randomButton.contains(x, y)) {
            return GAME_RANDOM;
        }
        float dx = x - gaugeCenterX;
        float dy = y - gaugeCenterY;
        float r = gaugeRadius + 12 * board.density; // un peu plus grand que l'anneau
        if (dx * dx + dy * dy <= r * r) {
            return GAME_ULTIMATE;
        }
        return -1;
    }

    public int pauseMenuButtonAt(float x, float y) {
        return indexAt(pauseMenuButtons, x, y);
    }

    public int optionsButtonAt(float x, float y) {
        return indexAt(optionsButtons, x, y);
    }

    public int victoryButtonAt(float x, float y) {
        for (int b : new int[]{VICTORY_NEXT, VICTORY_RESTART, VICTORY_MENU}) {
            if (victoryButtons[b].contains(x, y)) {
                return b;
            }
        }
        return -1;
    }

    public int gameOverButtonAt(float x, float y) {
        for (int b : new int[]{GAMEOVER_RETRY, GAMEOVER_MENU}) {
            if (victoryButtons[b].contains(x, y)) {
                return b;
            }
        }
        return -1;
    }

    // Animation de coffre : OUVRIR ENCORE, ou n'importe où ailleurs pour continuer
    public int chestButtonAt(float x, float y) {
        return chestAgainButton.contains(x, y) ? CHEST_AGAIN : CHEST_CONTINUE;
    }

    // Onglet sous le point, ou -1
    public int tabAt(float x, float y) {
        int i = indexAt(tabButtons, x, y);
        return i >= 0 ? HOME_TAB_0 + i : -1;
    }

    // Boutons fixes de la page (hors listes) : HOME_xxx, emplacement du niveau (1 à 6), ou -1
    public int homeButtonAt(int page, float x, float y) {
        int tab = tabAt(x, y);
        if (tab >= 0) {
            return tab;
        }
        switch (page) {
            case GameModel.PAGE_CAMPAIGN:
                if (homeOptionsButton.contains(x, y)) {
                    return HOME_OPTIONS;
                }
                if (homeSoundButton.contains(x, y)) {
                    return HOME_SOUND;
                }
                if (playButton.contains(x, y)) {
                    return HOME_PLAY;
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
            case GameModel.PAGE_ENDLESS: {
                if (endlessPlayButton.contains(x, y)) {
                    return HOME_ENDLESS_PLAY;
                }
                int c = indexAt(difficultyButtons, x, y);
                return c >= 0 ? HOME_DIFF_0 + c : -1;
            }
            default:
                return -1;
        }
    }

    // Fiche d'un mage ouverte : ses boutons, sinon FERMER (toucher à côté ferme aussi)
    public int sheetButtonAt(float x, float y) {
        if (sheetUpgrade.contains(x, y)) {
            return HOME_SHEET_UPGRADE;
        }
        if (sheetUse.contains(x, y)) {
            return HOME_SHEET_USE;
        }
        if (sheetArtifact.contains(x, y)) {
            return HOME_SHEET_ARTIFACT;
        }
        return HOME_SHEET_CLOSE;
    }

    // Zone de liste qui défile sur cette page (null : pas de liste)
    public RectF viewportFor(int page) {
        switch (page) {
            case GameModel.PAGE_MARKET:
            case GameModel.PAGE_CASTLE:
                return listViewport;
            case GameModel.PAGE_MAGES:
                return magesViewport;
            default:
                return null;
        }
    }

    // Volume (de 0 à 1) correspondant à une position horizontale sur le rail
    public float volumeAt(float x) {
        float v = (x - volumeTrack.left) / (volumeTrack.right - volumeTrack.left);
        return Math.max(0f, Math.min(1f, v));
    }
}
