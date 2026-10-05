package com.example.magicdefense.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.magicdefense.controller.TouchController;
import com.example.magicdefense.model.Board;
import com.example.magicdefense.model.Enemy;
import com.example.magicdefense.model.GameModel;
import com.example.magicdefense.model.Mage;
import com.example.magicdefense.model.Projectile;

// Tout le dessin : lit le modèle et l'affiche, sans jamais le modifier
public class GameRenderer {
    private final Paint paint = new Paint();
    private final Board board;
    private final Hud hud;
    private final float density;

    public GameRenderer(Board board, Hud hud, float density) {
        this.board = board;
        this.hud = hud;
        this.density = density;
        paint.setAntiAlias(true);
    }

    public void draw(Canvas canvas, GameModel model, TouchController touch) {
        canvas.drawColor(Palette.BACKGROUND);

        switch (model.getState()) {
            case START:
                drawStartScreen(canvas);
                break;
            case PLAYING:
            case PAUSED: // provisoire : même affichage que le jeu
                drawGame(canvas, model, touch);
                break;
            case GAME_OVER:
                drawGameOverScreen(canvas, model);
                break;
        }
    }

    // ---------- Écrans ----------
    private void drawStartScreen(Canvas canvas) {
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, "MAGIC DEFENSE", board.height * 0.4f, 40);
        drawCenteredText(canvas, "Toucher pour jouer", board.height * 0.55f, 22);
    }

    private void drawGameOverScreen(Canvas canvas, GameModel model) {
        paint.setColor(Palette.GAME_OVER);
        drawCenteredText(canvas, "GAME OVER", board.height * 0.4f, 40);
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, "Vague atteinte : " + model.getWave(), board.height * 0.5f, 24);
        drawCenteredText(canvas, "Toucher pour rejouer", board.height * 0.6f, 22);
    }

    private void drawGame(Canvas canvas, GameModel model, TouchController touch) {
        Mage dragged = touch.getDraggedMage();

        drawGrid(canvas, model, dragged);

        // Château
        paint.setColor(Palette.CASTLE);
        canvas.drawRect(0, board.gridTop - 12 * density, board.width, board.gridTop, paint);

        for (Enemy enemy : model.getEnemies()) {
            drawEnemy(canvas, enemy);
        }
        for (Projectile p : model.getProjectiles()) {
            paint.setColor(Palette.ELEMENT_COLORS[p.type]);
            canvas.drawCircle(p.x, p.y, p.radius, paint);
        }

        drawRecruitButtons(canvas, model, touch.getPressedButton());
        drawTopInfo(canvas, model);

        // Mage déplacé : dessiné en dernier (par-dessus tout), au-dessus du doigt
        if (dragged != null) {
            drawMage(canvas, dragged,
                    touch.getDragX() - board.cellWidth / 2,
                    touch.getDragY() - board.cellHeight / 2);
        }
    }

    // ---------- Éléments ----------
    private void drawGrid(Canvas canvas, GameModel model, Mage dragged) {
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                float left = board.cellLeft(col);
                float top = board.cellTop(row);
                Mage mage = model.getMage(row, col);

                // Case du damier
                paint.setColor((row + col) % 2 == 0 ? Palette.CELL_DARK : Palette.CELL_LIGHT);
                canvas.drawRect(left, top, left + board.cellWidth, top + board.cellHeight, paint);

                // Le mage en cours de déplacement n'est pas dessiné dans sa case
                if (mage != null && mage != dragged) {
                    drawMage(canvas, mage, left, top);
                }

                // Pendant un glissement : indiquer où l'on peut lâcher
                if (dragged != null && mage != dragged) {
                    if (model.canMerge(dragged, mage)) {
                        drawCellOutline(canvas, left, top, Color.GREEN);                   // fusion possible
                    } else if (mage == null) {
                        drawCellOutline(canvas, left, top, Color.argb(120, 255, 255, 255)); // case libre
                    }
                }
            }
        }
    }

    private void drawCellOutline(Canvas canvas, float left, float top, int color) {
        paint.setColor(color);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4 * density);
        float m = 3 * density;
        canvas.drawRect(left + m, top + m, left + board.cellWidth - m, top + board.cellHeight - m, paint);
        paint.setStyle(Paint.Style.FILL); // très important : on remet le mode normal
    }

    // Dessine un mage dans une case dont le coin haut-gauche est (left, top)
    private void drawMage(Canvas canvas, Mage mage, float left, float top) {
        float cx = left + board.cellWidth / 2;
        float cy = top + board.cellHeight / 2;
        float r = Math.min(board.cellWidth, board.cellHeight) * 0.35f;

        paint.setColor(Palette.ELEMENT_COLORS[mage.type]);
        canvas.drawCircle(cx, cy, r, paint);

        // Niveau au centre
        paint.setColor(Color.BLACK);
        paint.setTextSize(18 * density);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.valueOf(mage.level), cx, cy + 6 * density, paint);
    }

    private void drawEnemy(Canvas canvas, Enemy enemy) {
        paint.setColor(Palette.ENEMY);
        canvas.drawCircle(enemy.x, enemy.y, enemy.radius, paint);

        // Barre de vie au-dessus
        float barWidth = enemy.radius * 2;
        float barHeight = enemy.radius * 0.25f;
        float left = enemy.x - enemy.radius;
        float top = enemy.y - enemy.radius - barHeight * 2;
        paint.setColor(Color.DKGRAY);
        canvas.drawRect(left, top, left + barWidth, top + barHeight, paint);
        paint.setColor(Color.GREEN);
        canvas.drawRect(left, top, left + barWidth * enemy.hp / enemy.maxHp, top + barHeight, paint);
    }

    private void drawRecruitButtons(Canvas canvas, GameModel model, int pressedButton) {
        boolean canAfford = model.canAffordMage();

        for (int i = 0; i < hud.recruitButtons.length; i++) {
            RectF b = hud.recruitButtons[i];

            // Feedback : grisé si pas assez de PO, plus clair si appuyé
            if (!canAfford) {
                paint.setColor(Palette.DISABLED);
            } else if (i == pressedButton) {
                paint.setColor(Color.WHITE);
            } else {
                paint.setColor(Palette.ELEMENT_COLORS[i]);
            }
            canvas.drawRoundRect(b, 10 * density, 10 * density, paint);

            // Texte : élément + coût
            paint.setColor(Color.BLACK);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(15 * density);
            canvas.drawText("+1 " + Palette.ELEMENT_NAMES[i], b.centerX(), b.centerY() - 2 * density, paint);
            paint.setTextSize(13 * density);
            canvas.drawText(GameModel.MAGE_COST + " PO", b.centerX(), b.centerY() + 16 * density, paint);
        }
    }

    private void drawTopInfo(Canvas canvas, GameModel model) {
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, "VAGUE " + model.getWave(), 64 * density, 24);

        paint.setColor(Palette.GOLD);
        paint.setTextSize(20 * density);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(model.getGold() + " PO", 16 * density, 64 * density, paint);
    }

    // Texte centré horizontalement, taille en dp
    private void drawCenteredText(Canvas canvas, String text, float y, float sizeDp) {
        paint.setTextSize(sizeDp * density);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, board.width / 2f, y, paint);
    }
}
