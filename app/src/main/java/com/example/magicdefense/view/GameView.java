package com.example.magicdefense.view;

import android.content.Context;
import android.graphics.Canvas;
import android.view.MotionEvent;
import android.view.View;

import com.example.magicdefense.controller.TouchController;
import com.example.magicdefense.model.Board;
import com.example.magicdefense.model.GameModel;

// La vue Android : fait tourner la boucle et relie modèle, dessin et touchers
public class GameView extends View {
    private final Board board;
    private final GameModel model;
    private final Hud hud;
    private final GameRenderer renderer;
    private final TouchController touchController;

    public GameView(Context context) {
        super(context);
        float density = getResources().getDisplayMetrics().density;
        board = new Board(density);
        model = new GameModel(board);
        hud = new Hud(board);
        renderer = new GameRenderer(board, hud, density);
        touchController = new TouchController(model, board, hud, density);
    }

    // Appelée dès que la taille de la vue est connue
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        board.resize(w, h);
        hud.resize(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        renderer.draw(canvas, model, touchController);
    }

    // ---------- Boucle ----------
    private final Runnable gameLoop = new Runnable() {
        @Override
        public void run() {
            model.update();
            invalidate();          // demande un nouvel appel à onDraw()
            postDelayed(this, 16); // ≈ 60 images/s
        }
    };

    public void startGame() {
        removeCallbacks(gameLoop); // évite deux boucles en parallèle
        post(gameLoop);
    }

    public void stopGame() {
        model.pause();
        touchController.cancel();
        removeCallbacks(gameLoop);
    }

    // ---------- Interaction ----------
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (touchController.onTouchEvent(event)) {
            performClick();
        }
        return true; // toujours true, sinon on ne reçoit pas la suite du geste
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
