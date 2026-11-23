package com.example.magicdefense.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;

import com.example.magicdefense.model.Board;
import com.example.magicdefense.model.Mage;
import com.example.magicdefense.model.Nuke;

// Dessin propre au Scientifique : jauge de la bombe sur sa case, chute de la bombe, flash blanc,
// champignon atomique en pixel art et onde de choc. Avec l'option « Effets des attaques » sur Non,
// une version minimale : petit rond qui tombe, anneau qui s'élargit et léger éclair.
public class NukeRenderer {
    private final float density;
    private final Paint paint = new Paint();
    private final RectF oval = new RectF();
    private int frame = 0;
    private boolean lowFx = false;

    public NukeRenderer(float density) {
        this.density = density;
        paint.setAntiAlias(true);
    }

    public void setFrame(int frame, boolean lowFx) {
        this.frame = frame;
        this.lowFx = lowFx;
    }

    // Décalage de l'écran pendant le tremblement (rien en effets réduits)
    public float shakeX(float shake) {
        return lowFx || shake <= 0 ? 0 : (float) Math.sin(frame * 1.9) * 5 * density * shake;
    }

    public float shakeY(float shake) {
        return lowFx || shake <= 0 ? 0 : (float) Math.cos(frame * 2.7) * 4 * density * shake;
    }

    // ---------- Jauge de la bombe (coin haut gauche de la case) ----------
    public void drawNukeGauge(Canvas canvas, Mage mage, float left, float top) {
        float cx = left + 14 * density;
        float cy = top + 14 * density;
        float r = 10 * density;
        float progress = mage.getNukeProgress();
        boolean chain = mage.chainTimer > 0;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(210, 26, 22, 38));
        canvas.drawCircle(cx, cy, r + 2 * density, paint);
        if (progress >= 1f) {
            // Prête : halo jaune qui clignote (elle partira dès qu'un gobelin sera à l'écran)
            float pulse = 0.5f + 0.5f * (float) Math.sin(frame * 0.3);
            paint.setColor(Color.argb((int) (80 + 100 * pulse), 255, 220, 40));
            canvas.drawCircle(cx, cy, r + (2 + 3 * pulse) * density, paint);
        }
        oval.set(cx - r, cy - r, cx + r, cy + r);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3 * density);
        paint.setColor(Palette.GAUGE_EMPTY);
        canvas.drawArc(oval, 0, 360, false, paint);
        // Réaction en chaîne : la jauge devient rouge et clignote
        int color = chain ? ((frame / 6) % 2 == 0 ? Color.rgb(255, 70, 50) : Color.rgb(255, 170, 60))
                : Color.rgb(255, 220, 40);
        paint.setColor(color);
        canvas.drawArc(oval, -90, 360 * progress, false, paint);
        paint.setStyle(Paint.Style.FILL);
        drawTrefoil(canvas, cx, cy, r * 0.62f, progress >= 1f ? Color.rgb(255, 220, 40) : Color.rgb(230, 230, 220));
    }

    // Symbole de radioactivité : trois pales et un point central
    private void drawTrefoil(Canvas canvas, float cx, float cy, float r, int color) {
        paint.setColor(color);
        oval.set(cx - r, cy - r, cx + r, cy + r);
        for (int k = 0; k < 3; k++) {
            canvas.drawArc(oval, -120 + k * 120 - 30, 60, true, paint);
        }
        paint.setColor(Color.rgb(26, 22, 38));
        canvas.drawCircle(cx, cy, r * 0.32f, paint);
        paint.setColor(color);
        canvas.drawCircle(cx, cy, r * 0.18f, paint);
    }

    // ---------- Bombe : chute puis explosion ----------
    public void drawNuke(Canvas canvas, Nuke n, Board board) {
        if (n.isFalling()) {
            drawFalling(canvas, n);
        } else {
            drawBlast(canvas, n, board);
        }
    }

    // Flash blanc plein écran juste après l'impact (à dessiner par-dessus tout le terrain)
    public void drawFlash(Canvas canvas, Nuke n, Board board) {
        if (n.isFalling()) {
            return;
        }
        float b = n.getBlast();
        float limit = 0.12f;
        if (b < limit) {
            int a = (int) ((lowFx ? 90 : 245) * (1 - b / limit));
            paint.setColor(Color.argb(a, 255, 255, 245));
            canvas.drawRect(0, 0, board.width, board.height, paint);
        }
    }

    private void drawFalling(Canvas canvas, Nuke n) {
        float t = n.getFall();
        float startY = -40 * density;
        float y = startY + (n.y - startY) * t * t; // accélère en tombant
        float x = n.x;
        float s = 12 * density;
        // Ombre au sol qui grandit
        paint.setColor(Color.argb((int) (40 + 100 * t), 0, 0, 0));
        canvas.drawOval(x - s * 1.5f * t, n.y - s * 0.4f * t, x + s * 1.5f * t, n.y + s * 0.4f * t, paint);
        if (lowFx) {
            paint.setColor(Color.rgb(60, 70, 50));
            canvas.drawCircle(x, y, s * 0.7f, paint);
            return;
        }
        // Ailerons
        paint.setColor(Color.rgb(26, 22, 38));
        canvas.drawRect(x - s * 0.8f, y - s * 1.9f, x + s * 0.8f, y - s * 1.4f, paint);
        paint.setColor(Color.rgb(90, 100, 80));
        canvas.drawRect(x - s * 0.7f, y - s * 1.85f, x + s * 0.7f, y - s * 1.5f, paint);
        // Corps arrondi vert militaire
        paint.setColor(Color.rgb(26, 22, 38));
        oval.set(x - s * 0.75f, y - s * 1.6f, x + s * 0.75f, y + s * 1.05f);
        canvas.drawOval(oval, paint);
        paint.setColor(Color.rgb(95, 110, 75));
        oval.inset(density * 1.5f, density * 1.5f);
        canvas.drawOval(oval, paint);
        // Bande jaune et symbole
        paint.setColor(Color.rgb(250, 210, 40));
        canvas.drawRect(x - s * 0.66f, y - s * 0.55f, x + s * 0.66f, y + s * 0.15f, paint);
        drawTrefoil(canvas, x, y - s * 0.2f, s * 0.3f, Color.rgb(26, 22, 38));
        // Reflet
        paint.setColor(Color.argb(120, 255, 255, 255));
        canvas.drawRect(x - s * 0.45f, y - s * 1.3f, x - s * 0.3f, y - s * 0.7f, paint);
        // Lumière qui clignote au bout
        if ((frame / 4) % 2 == 0) {
            paint.setColor(Color.rgb(255, 60, 40));
            canvas.drawCircle(x, y + s * 0.9f, s * 0.18f, paint);
        }
    }

    private void drawBlast(Canvas canvas, Nuke n, Board board) {
        float b = n.getBlast();
        float w = board.playWidth;
        float fade = b > 0.7f ? (1 - b) / 0.3f : 1f;

        // Onde de choc : anneau aplati qui balaie le terrain
        if (b < 0.6f) {
            float rr = w * (0.08f + 1.1f * b / 0.6f);
            int a = (int) (220 * (1 - b / 0.6f));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth((lowFx ? 3 : 7) * density * (1 - b / 0.6f) + density);
            paint.setColor(Color.argb(a, 255, 245, 210));
            canvas.drawOval(n.x - rr, n.y - rr * 0.45f, n.x + rr, n.y + rr * 0.45f, paint);
            paint.setStyle(Paint.Style.FILL);
        }
        if (lowFx) {
            return; // effets réduits : pas de champignon
        }

        float rise = Math.min(1f, b / 0.4f);
        rise = 1 - (1 - rise) * (1 - rise);                  // montée rapide puis ralentie
        float scale = 0.85f + 0.15f * Math.min(1, n.level / 5f) + (n.level >= 6 ? 0.15f : 0);
        float px = Math.max(3, Math.round(4 * density));     // taille d'un « pixel » du pixel art
        float capRx = w * (0.10f + 0.16f * rise) * scale;
        float capRy = capRx * 0.55f;
        float capCy = n.y - w * 0.36f * rise * scale;
        float stemW = capRx * 0.32f;
        float baseRx = w * (0.10f + 0.18f * rise) * scale;
        float baseRy = baseRx * 0.32f;
        float heat = Math.max(0f, 1f - b * 1.4f);            // le feu refroidit en fumée grise

        // Nuage au sol (poussière et feu), tige, puis chapeau du champignon
        drawPixelEllipse(canvas, n.x, n.y, baseRx, baseRy, px, heat * 0.8f, fade, 11);
        float stemTop = capCy + capRy * 0.4f;
        for (float y = n.y - baseRy * 0.5f; y > stemTop; y -= px) {
            float k = (n.y - y) / Math.max(1f, n.y - stemTop);
            float half = stemW * (0.75f + 0.25f * (float) Math.sin(k * 3 + frame * 0.05));
            for (float x = n.x - half; x < n.x + half; x += px) {
                float d = Math.abs(x - n.x) / half;
                paint.setColor(fireColor(d * 0.8f + 0.1f, heat, fade, hash(x, y)));
                canvas.drawRect(snap(x, px), snap(y, px), snap(x, px) + px, snap(y, px) + px, paint);
            }
        }
        drawPixelEllipse(canvas, n.x, capCy, capRx, capRy, px, heat, fade, 7);
        // Anneau de condensation autour de la tige
        float ringY = capCy + capRy * 1.4f;
        drawPixelEllipse(canvas, n.x, ringY, capRx * 0.75f, capRy * 0.25f, px, heat * 0.4f, fade * 0.7f, 3);
    }

    // Ellipse remplie de « pixels » dont la couleur dépend de la distance au centre (cœur blanc → bord rouge)
    private void drawPixelEllipse(Canvas canvas, float cx, float cy, float rx, float ry, float px, float heat,
                                  float fade, int seed) {
        if (rx < px || ry < px) {
            return;
        }
        for (float y = cy - ry; y <= cy + ry; y += px) {
            for (float x = cx - rx; x <= cx + rx; x += px) {
                float dx = (x - cx) / rx;
                float dy = (y - cy) / ry;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                // Bord irrégulier (bouillonnement)
                float edge = 1f - 0.12f * (float) Math.sin(Math.atan2(dy, dx) * 7 + frame * 0.08 + seed);
                if (d > edge) {
                    continue;
                }
                // Plus clair vers le haut du nuage
                float shade = d / edge + dy * 0.15f;
                paint.setColor(fireColor(shade, heat, fade, hash(x, y + seed)));
                float sx = snap(x, px);
                float sy = snap(y, px);
                canvas.drawRect(sx, sy, sx + px, sy + px, paint);
            }
        }
    }

    // Couleur du feu : d = 0 au cœur, 1 au bord ; heat = 1 brûlant, 0 fumée froide
    private static int fireColor(float d, float heat, float fade, float noise) {
        d = Math.max(0f, Math.min(1f, d + (noise - 0.5f) * 0.15f));
        int r, g, b;
        if (d < 0.3f) {        // cœur blanc-jaune
            r = 255; g = 250; b = 200;
        } else if (d < 0.55f) { // jaune orangé
            r = 255; g = 200; b = 70;
        } else if (d < 0.8f) {  // orange
            r = 245; g = 120; b = 40;
        } else {               // bord rouge sombre
            r = 170; g = 50; b = 35;
        }
        // Refroidissement : vers le gris de la fumée
        int gray = (int) (95 + 60 * (1 - d));
        r = (int) (gray + (r - gray) * heat);
        g = (int) (gray + (g - gray) * heat);
        b = (int) (gray + (b - gray) * heat);
        return Color.argb((int) (235 * fade), r, g, b);
    }

    private static float snap(float v, float px) {
        return (float) Math.floor(v / px) * px;
    }

    // Bruit fixe par pixel (le pixel art ne scintille pas)
    private static float hash(float x, float y) {
        int h = (int) (x * 0.37f) * 73856093 ^ (int) (y * 0.41f) * 19349663;
        h ^= h >>> 13;
        return ((h * 1103515245 + 12345) >>> 8 & 0xFFFF) / 65535f;
    }
}
