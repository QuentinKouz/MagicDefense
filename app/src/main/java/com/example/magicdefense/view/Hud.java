package com.example.magicdefense.view;

import android.graphics.RectF;

import com.example.magicdefense.model.Board;
import com.example.magicdefense.model.Mage;

// Position des boutons de l'interface (barre de recrutement en bas)
public class Hud {
    private final Board board;
    public final RectF[] recruitButtons = new RectF[Mage.TYPE_COUNT];

    public Hud(Board board) {
        this.board = board;
        for (int i = 0; i < recruitButtons.length; i++) {
            recruitButtons[i] = new RectF();
        }
    }

    // À appeler après board.resize()
    public void resize(int w, int h) {
        float d = board.density;
        float margin = 6 * d;
        float buttonWidth = w / (float) recruitButtons.length;
        float buttonTop = h - board.bottomBarHeight + margin;
        float buttonBottom = buttonTop + 60 * d; // grande cible pour le doigt
        for (int i = 0; i < recruitButtons.length; i++) {
            recruitButtons[i].set(i * buttonWidth + margin, buttonTop,
                    (i + 1) * buttonWidth - margin, buttonBottom);
        }
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
}
