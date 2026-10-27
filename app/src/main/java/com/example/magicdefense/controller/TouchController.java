package com.example.magicdefense.controller;

import android.view.MotionEvent;

import com.example.magicdefense.model.Board;
import com.example.magicdefense.model.GameModel;
import com.example.magicdefense.model.GameState;
import com.example.magicdefense.model.Mage;
import com.example.magicdefense.view.Hud;

// Traduit les touchers en actions sur le modèle (boutons, drag & drop, changements d'écran)
public class TouchController {
    private final GameModel model;
    private final Board board;
    private final Hud hud;
    private final float dragOffset; // décalage pour ne pas cacher le mage sous le doigt

    private int pressedButton = -1;       // bouton de recrutement sous le doigt (-1 = aucun)
    private boolean pausePressed = false; // doigt posé sur le bouton pause
    private int pressedMenuButton = -1;   // bouton de pause ou d'options sous le doigt (-1 = aucun)
    private Mage draggedMage = null;      // mage en cours de déplacement (null = aucun)
    private int dragRow, dragCol;         // case d'origine
    private float dragX, dragY;           // position du doigt
    private Runnable onButtonClick; // appelé quand un bouton est validé (pour le son)
    private float dragVolume = 0; // position du curseur pendant le glissement
    private Runnable onToggleMute; // couper / remettre le son (géré par GameView)
    private boolean mutePressed = false; // doigt posé sur le bouton son (en jeu)




    public TouchController(GameModel model, Board board, Hud hud, float density) {
        this.model = model;
        this.board = board;
        this.hud = hud;
        this.dragOffset = 48 * density;
    }

    // ---------- Accès en lecture (pour le feedback visuel) ----------
    public int getPressedButton() { return pressedButton; }
    public boolean isPausePressed() { return pausePressed; }
    public int getPressedMenuButton() { return pressedMenuButton; }
    public Mage getDraggedMage() { return draggedMage; }
    public float getDragX() { return dragX; }
    public float getDragY() { return dragY - dragOffset; } // position affichée, au-dessus du doigt
    private boolean draggingVolume = false; // doigt en train de régler le volume

    public boolean isDraggingVolume() { return draggingVolume; }

    public void setOnButtonClick(Runnable onButtonClick) {
        this.onButtonClick = onButtonClick;
    }

    public float getDragVolume() { return dragVolume; }

    public void setOnToggleMute(Runnable onToggleMute) {
        this.onToggleMute = onToggleMute;
    }

    public boolean isMutePressed() { return mutePressed; }

    // Retourne true quand le geste se termine (pour appeler performClick)
    public boolean onTouchEvent(MotionEvent event) {
        float tx = event.getX();
        float ty = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (model.getState() == GameState.PLAYING) {
                    if (hud.isOnPauseButton(tx, ty)) {
                        pausePressed = true;
                    } else if (hud.homeSoundButton.contains(tx, ty)) {
                        mutePressed = true;
                    } else {
                        pressedButton = hud.buttonAt(tx, ty);
                        if (pressedButton == -1) {
                            startDrag(tx, ty); // pas un bouton : peut-être un mage
                        }
                    }
                } else {
                    pressedMenuButton = menuButtonAt(tx, ty); // pause ou options
                    // Curseur de volume : on commence à le régler dès l'appui
                    if (model.getState() == GameState.OPTIONS && pressedMenuButton == Hud.OPTIONS_VOLUME) {
                        draggingVolume = true;
                        dragVolume = hud.volumeAt(tx);
                    }
                }
                return false;

            case MotionEvent.ACTION_MOVE:
                // Réglage du volume : le curseur suit le doigt, même s'il sort de la ligne
                if (draggingVolume) {
                    dragVolume = hud.volumeAt(tx);
                    return false;
                }
                // Le doigt sort du bouton : on annule l'appui
                if (pressedMenuButton != -1 && menuButtonAt(tx, ty) != pressedMenuButton) {
                    pressedMenuButton = -1;
                }
                if (pausePressed && !hud.isOnPauseButton(tx, ty)) {
                    pausePressed = false;
                }
                if (mutePressed && !hud.homeSoundButton.contains(tx, ty)) {
                    mutePressed = false;
                }
                if (draggedMage != null) {
                    dragX = tx; // le mage suit le doigt
                    dragY = ty;
                } else if (pressedButton != -1 && hud.buttonAt(tx, ty) != pressedButton) {
                    pressedButton = -1;
                }
                return false;

            case MotionEvent.ACTION_UP:
                // Fin du réglage du volume : on applique la valeur, puis clic d'aperçu
                if (draggingVolume) {
                    draggingVolume = false;
                    pressedMenuButton = -1;
                    model.setSoundVolume(dragVolume);
                    if (onButtonClick != null) {
                        onButtonClick.run(); // applique au téléphone et joue le clic
                    }
                    return true;
                }
                if (draggedMage != null) {
                    endDrag();
                } else {
                    handleRelease(tx, ty);
                }
                // Le geste est fini : on remet tout à zéro
                pressedButton = -1;
                pausePressed = false;
                mutePressed = false;
                pressedMenuButton = -1;
                return true;

            case MotionEvent.ACTION_CANCEL:
                cancel(); // geste interrompu par le système
                return false;
        }
        return false;
    }

    // Annule tout geste en cours (pause, interruption...)
    public void cancel() {
        draggedMage = null;
        pressedButton = -1;
        pausePressed = false;
        pressedMenuButton = -1;
        draggingVolume = false;
        mutePressed = false;
    }

    // Toutes les actions se déclenchent au relâchement (choix du dossier de design)
    private void handleRelease(float tx, float ty) {
        // Son de clic quand un bouton de menu ou le bouton pause est validé
        boolean menuClick = pressedMenuButton != -1 && menuButtonAt(tx, ty) == pressedMenuButton;
        boolean pauseClick = pausePressed && hud.isOnPauseButton(tx, ty);
        boolean muteClick = mutePressed && hud.homeSoundButton.contains(tx, ty);
        if ((menuClick || pauseClick || muteClick) && onButtonClick != null) {
            onButtonClick.run();
        }
        switch (model.getState()) {
            case START:
                if (pressedMenuButton != -1 && menuButtonAt(tx, ty) == pressedMenuButton) {
                    if (pressedMenuButton == Hud.HOME_PLAY) {
                        model.startLevel(model.getSelectedLevel());
                    } else if (pressedMenuButton == Hud.HOME_OPTIONS) {
                        model.openOptions();
                    } else if (pressedMenuButton == Hud.HOME_SOUND) {
                        if (onToggleMute != null) {
                            onToggleMute.run();
                        }
                    } else {
                        model.selectLevel(pressedMenuButton); // numéro du niveau touché
                    }
                }
                break;

            case GAME_OVER:
                if (pressedMenuButton != -1 && menuButtonAt(tx, ty) == pressedMenuButton) {
                    if (pressedMenuButton == Hud.GAMEOVER_RETRY) {
                        model.restartLevel();
                    } else if (pressedMenuButton == Hud.GAMEOVER_MENU) {
                        model.quitToMenu();
                    }
                }
                break;

            case PLAYING:
                if (pauseClick) {
                    model.pause();
                } else if (muteClick) {
                    if (onToggleMute != null) {
                        onToggleMute.run();
                    }
                } else if (pressedButton != -1 && hud.buttonAt(tx, ty) == pressedButton) {
                    model.recruitMage(pressedButton);
                }
                break;

            case PAUSED:
                // Seulement si appuyé ET relâché sur le même bouton
                if (pressedMenuButton != -1 && menuButtonAt(tx, ty) == pressedMenuButton) {
                    switch (pressedMenuButton) {
                        case Hud.PAUSE_QUIT:
                            model.quitToMenu();
                            break;
                        case Hud.PAUSE_RESTART:
                            model.restartLevel();
                            break;
                        case Hud.PAUSE_RESUME:
                            model.resume();
                            break;
                        case Hud.PAUSE_OPTIONS:
                            model.openOptions();
                            break;
                    }
                }
                break;

            case OPTIONS:
                if (pressedMenuButton != -1 && menuButtonAt(tx, ty) == pressedMenuButton) {
                    if (pressedMenuButton == Hud.OPTIONS_NIGHT_TOGGLE) {
                        model.toggleNightModeDisabled();
                    } else if (pressedMenuButton == Hud.OPTIONS_MUSIC_TOGGLE) {
                        model.toggleMusic();
                    } else if (pressedMenuButton == Hud.OPTIONS_BACK) {
                        model.closeOptions();
                    }
                }
                break;

            case VICTORY:
                if (pressedMenuButton != -1 && menuButtonAt(tx, ty) == pressedMenuButton) {
                    if (pressedMenuButton == Hud.VICTORY_NEXT) {
                        model.nextLevel();
                    } else if (pressedMenuButton == Hud.VICTORY_MENU) {
                        model.quitToMenu();
                    }
                }
                break;
        }
    }

    // Bouton de menu sous le doigt, selon la fenêtre affichée
    private int menuButtonAt(float x, float y) {
        switch (model.getState()) {
            case PAUSED:
                return hud.pauseMenuButtonAt(x, y);
            case OPTIONS:
                return hud.optionsButtonAt(x, y);
            case VICTORY:
                return hud.victoryButtonAt(x, y);
            case START:
                return hud.homeButtonAt(x, y);
            case GAME_OVER:
                return hud.victoryButtonAt(x, y); // même fenêtre que la victoire
            default:
                return -1;
        }
    }

    private void startDrag(float x, float y) {
        int[] cell = board.cellAt(x, y);
        if (cell != null && model.getMage(cell[0], cell[1]) != null) {
            draggedMage = model.getMage(cell[0], cell[1]);
            dragRow = cell[0];
            dragCol = cell[1];
            dragX = x;
            dragY = y;
        }
    }

    private void endDrag() {
        // On vise avec le mage affiché (au-dessus du doigt), pas avec le doigt
        int[] cell = board.cellAt(getDragX(), getDragY());
        if (cell != null) {
            model.dropMage(dragRow, dragCol, cell[0], cell[1]);
        }
        draggedMage = null;
    }
}