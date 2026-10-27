package com.example.magicdefense.view;

import android.graphics.RectF;

import com.example.magicdefense.model.Board;
import com.example.magicdefense.model.Mage;
import com.example.magicdefense.model.GameModel;

// Position des boutons de l'interface (barre de recrutement en bas)
public class Hud {
    private final Board board;
    public final RectF[] recruitButtons = new RectF[Mage.TYPE_COUNT];
    public final RectF pauseButton = new RectF();
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

    public final RectF optionsPopup = new RectF();
    public final RectF[] optionsButtons = new RectF[4];
    // Boutons de l'écran Victoire
    public static final int VICTORY_NEXT = 0;
    public static final int VICTORY_MENU = 1;

    // Écran Game Over : même fenêtre et mêmes boutons que la victoire
    public static final int GAMEOVER_RETRY = VICTORY_NEXT;  // bouton du haut
    public static final int GAMEOVER_MENU = VICTORY_MENU;   // bouton du bas

    public static final int OPTIONS_VOLUME = 2;
    public static final int OPTIONS_MUSIC_TOGGLE = 3;
    public final RectF volumeTrack = new RectF(); // rail du curseur (seuls left, right et centerY servent)

    public final RectF victoryPopup = new RectF();
    public final RectF[] victoryButtons = new RectF[2];

    // Carte des niveaux (écran d'accueil)
    public static final int HOME_PLAY = 0;  // bouton JOUER ; les niveaux utilisent leur numéro (1 à 6)
    public final RectF playButton = new RectF();
    public final RectF[] levelNodes = new RectF[GameModel.MAX_LEVELS + 1]; // index = niveau

    public static final int HOME_OPTIONS = 100; // valeurs hors des numéros de niveau (1 à 6)
    public static final int HOME_SOUND = 101;
    public final RectF homeOptionsButton = new RectF();
    public final RectF homeSoundButton = new RectF();

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
        for (int i = 0; i < recruitButtons.length; i++) {
            recruitButtons[i] = new RectF();
        }
        for (int i = 0; i < pauseMenuButtons.length; i++) {
            pauseMenuButtons[i] = new RectF();
        }
        for (int i = 0; i < optionsButtons.length; i++) {
            optionsButtons[i] = new RectF();
        }
        for (int i = 0; i < victoryButtons.length; i++) {
            victoryButtons[i] = new RectF();
        }
        for (int i = 0; i < levelNodes.length; i++) {
            levelNodes[i] = new RectF();
        }
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

        // Boutons de recrutement : 4 côte à côte en bas de la colonne
        float margin = 6 * d;
        float buttonWidth = cw / recruitButtons.length;
        float buttonTop = h - board.bottomBarHeight + margin;
        float buttonBottom = buttonTop + 60 * d; // grande cible pour le doigt
        for (int i = 0; i < recruitButtons.length; i++) {
            recruitButtons[i].set(left + i * buttonWidth + margin, buttonTop,
                    left + (i + 1) * buttonWidth - margin, buttonBottom);
        }

        // Bouton pause rond en haut à droite, aligné avec la jauge d'ultime
        float size = 56 * d;   // grande cible pour le doigt
        float side = 16 * d;
        float top = 40 * d + shift;
        pauseButton.set(right - side - size, top, right - side, top + size);

        // Accueil : options et son en haut à droite, l'un sous l'autre (comme sur la maquette)
        homeOptionsButton.set(pauseButton); // même place que le bouton pause en jeu
        homeSoundButton.set(right - side - size, top + size + 12 * d, right - side, top + 2 * size + 12 * d);

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
            float bx = cx + (i - 1) * spacing;
            pauseMenuButtons[PAUSE_RESTART + i].set(bx - diameter / 2, rowY - diameter / 2,
                    bx + diameter / 2, rowY + diameter / 2);
        }

        // Fenêtre d'options : mode nuit, musique, volume, retour
        float oH = 435 * d;
        float oTop = popupTop(h, oH);
        optionsPopup.set(pLeft, oTop, pLeft + pw, oTop + oH);

        // Interrupteur : toute la ligne "Non [===] Oui" est touchable (grande cible)
        optionsButtons[OPTIONS_NIGHT_TOGGLE].set(pLeft + 16 * d, oTop + 115 * d,
                pLeft + pw - 16 * d, oTop + 165 * d);

        // Musique : même interrupteur, sous le mode nuit
        optionsButtons[OPTIONS_MUSIC_TOGGLE].set(pLeft + 16 * d, oTop + 205 * d,
                pLeft + pw - 16 * d, oTop + 255 * d);

        // Volume : toute la ligne est touchable ; le rail laisse la place aux haut-parleurs
        optionsButtons[OPTIONS_VOLUME].set(pLeft + 16 * d, oTop + 285 * d,
                pLeft + pw - 16 * d, oTop + 355 * d);
        float railY = oTop + 320 * d;
        volumeTrack.set(pLeft + 60 * d, railY, pLeft + pw - 60 * d, railY);

        // RETOUR
        float backW = pw * 0.6f;
        optionsButtons[OPTIONS_BACK].set(cx - backW / 2, oTop + 365 * d, cx + backW / 2, oTop + 417 * d);

        // Fenêtre de victoire (et de game over) : titre, 3 étoiles, puis 2 boutons l'un sous l'autre
        float vH = 310 * d;
        float vTop = popupTop(h, vH);
        victoryPopup.set(pLeft, vTop, pLeft + pw, vTop + vH);

        float vBtnW = pw * 0.75f;
        victoryButtons[VICTORY_NEXT].set(cx - vBtnW / 2, vTop + 176 * d, cx + vBtnW / 2, vTop + 228 * d);
        victoryButtons[VICTORY_MENU].set(cx - vBtnW / 2, vTop + 242 * d, cx + vBtnW / 2, vTop + 294 * d);

        // JOUER : large, en bas (zone facile au pouce)
        float playW = cw * 0.6f;
        playButton.set(cx - playW / 2, h - 150 * d, cx + playW / 2, h - 86 * d);

        // Carte : les niveaux occupent l'espace entre le titre et le bouton JOUER,
        // quelle que soit la hauteur de l'écran (petit téléphone, tablette en paysage...)
        float mapTop = Math.max(h * NODE_POS[GameModel.MAX_LEVELS - 1][1], 205 * d + shift);
        float mapBottom = Math.max(mapTop + 1, playButton.top - 66 * d);
        float fyTop = NODE_POS[GameModel.MAX_LEVELS - 1][1];
        float fyBottom = NODE_POS[0][1];
        float nodeSize = 72 * d; // zone touchable de 72 dp (le cercle dessiné en fait 56)
        for (int lv = 1; lv <= GameModel.MAX_LEVELS; lv++) {
            float nx = left + cw * NODE_POS[lv - 1][0];
            float t = (NODE_POS[lv - 1][1] - fyTop) / (fyBottom - fyTop); // 0 en haut, 1 en bas
            float ny = mapTop + t * (mapBottom - mapTop);
            levelNodes[lv].set(nx - nodeSize / 2, ny - nodeSize / 2, nx + nodeSize / 2, ny + nodeSize / 2);
        }
    }

    // Haut d'une fenêtre centrée vers 40 % de l'écran, sans dépasser en haut ni en bas
    private float popupTop(int h, float popupHeight) {
        float top = h * 0.4f - popupHeight / 2;
        float minTop = board.topShift + 16 * board.density;
        float maxTop = h - popupHeight - 16 * board.density;
        return Math.max(minTop, Math.min(top, maxTop));
    }

    // Index du bouton sous le point (x, y), ou -1
    public int buttonAt(float x, float y) {
        for (int i = 0; i < recruitButtons.length; i++) {
            if (recruitButtons[i].contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    public boolean isOnPauseButton(float x, float y) {
        return pauseButton.contains(x, y);
    }

    // Index du bouton de pause sous le point (x, y), ou -1
    public int pauseMenuButtonAt(float x, float y) {
        for (int i = 0; i < pauseMenuButtons.length; i++) {
            if (pauseMenuButtons[i].contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    // Index du bouton d'options sous le point (x, y), ou -1
    public int optionsButtonAt(float x, float y) {
        for (int i = 0; i < optionsButtons.length; i++) {
            if (optionsButtons[i].contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    // Index du bouton de victoire sous le point (x, y), ou -1
    public int victoryButtonAt(float x, float y) {
        for (int i = 0; i < victoryButtons.length; i++) {
            if (victoryButtons[i].contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    // HOME_PLAY, numéro du niveau touché, ou -1
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
        for (int lv = 1; lv < levelNodes.length; lv++) {
            if (levelNodes[lv].contains(x, y)) {
                return lv;
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
