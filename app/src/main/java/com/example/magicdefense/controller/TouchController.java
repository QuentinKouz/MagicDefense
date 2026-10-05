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

    private int pressedButton = -1; // bouton sous le doigt (-1 = aucun)
    private Mage draggedMage = null; // mage en cours de déplacement (null = aucun)
    private int dragRow, dragCol;    // case d'origine
    private float dragX, dragY;      // position du doigt

    public TouchController(GameModel model, Board board, Hud hud, float density) {
        this.model = model;
        this.board = board;
        this.hud = hud;
        this.dragOffset = 48 * density;
    }

    // ---------- Accès en lecture (pour le feedback visuel) ----------
    public int getPressedButton() { return pressedButton; }
    public Mage getDraggedMage() { return draggedMage; }
    public float getDragX() { return dragX; }
    public float getDragY() { return dragY - dragOffset; } // position affichée, au-dessus du doigt

    // Retourne true quand le geste se termine (pour appeler performClick)
    public boolean onTouchEvent(MotionEvent event) {
        float tx = event.getX();
        float ty = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (model.getState() == GameState.PLAYING) {
                    pressedButton = hud.buttonAt(tx, ty);
                    if (pressedButton == -1) {
                        startDrag(tx, ty); // pas un bouton : peut-être un mage
                    }
                }
                return false;

            case MotionEvent.ACTION_MOVE:
                if (draggedMage != null) {
                    dragX = tx;            // le mage suit le doigt
                    dragY = ty;
                } else if (pressedButton != -1 && hud.buttonAt(tx, ty) != pressedButton) {
                    pressedButton = -1;    // le doigt sort du bouton : annulé
                }
                return false;

            case MotionEvent.ACTION_UP:
                if (draggedMage != null) {
                    endDrag();
                } else {
                    handleRelease(tx, ty);
                }
                pressedButton = -1;
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
    }

    // Toutes les actions se déclenchent au relâchement (choix du dossier de design)
    private void handleRelease(float tx, float ty) {
        switch (model.getState()) {
            case START:
            case GAME_OVER:
                model.startNewGame();
                break;

            case PLAYING:
                // Bouton de recrutement : seulement si appuyé ET relâché dessus
                if (pressedButton != -1 && hud.buttonAt(tx, ty) == pressedButton) {
                    model.recruitMage(pressedButton);
                }
                break;

            case PAUSED:
                model.resume();
                break;
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
