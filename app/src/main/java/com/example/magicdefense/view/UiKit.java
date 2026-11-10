package com.example.magicdefense.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

// Éléments d'interface dans la direction artistique du jeu (pixel art médiéval-fantasy) :
// tuiles en relief, médaillons dorés, fenêtres en planches cloutées, rubans de titre,
// alcôves creusées, barres encadrées et texte à contour. Tout le style est ici.
public class UiKit {
    // Couleurs du style (reprises du décor : bois de l'estrade, or, contour des sprites)
    public static final int OUTLINE = Color.rgb(26, 22, 38);
    public static final int WOOD = Color.rgb(112, 74, 42);
    public static final int WOOD_DARK = Color.rgb(84, 54, 30);
    public static final int WOOD_LIGHT = Color.rgb(140, 96, 56);
    public static final int NAIL = Color.rgb(176, 176, 184);
    public static final int GOLD = Color.rgb(240, 192, 64);
    public static final int GOLD_DARK = Color.rgb(168, 120, 32);
    public static final int SLATE_TOP = Color.rgb(48, 44, 76);
    public static final int SLATE_BOTTOM = Color.rgb(30, 27, 50);
    public static final int INSET = Color.rgb(24, 22, 40);
    public static final int RIBBON = Color.rgb(150, 40, 60);   // ruban par défaut : pourpre

    private final Paint paint = new Paint();
    private final Path path = new Path();
    private final RectF r = new RectF();
    private final RectF face = new RectF();
    private final float d;

    public UiKit(float density) {
        this.d = density;
        paint.setAntiAlias(true);
    }

    // ---------- Couleurs ----------
    public static int shade(int c, float f) { // f < 1 : plus sombre
        return Color.argb(Color.alpha(c), clamp(Color.red(c) * f), clamp(Color.green(c) * f), clamp(Color.blue(c) * f));
    }

    public static int tint(int c, float t) { // vers le blanc
        return Color.argb(Color.alpha(c), clamp(Color.red(c) + (255 - Color.red(c)) * t),
                clamp(Color.green(c) + (255 - Color.green(c)) * t), clamp(Color.blue(c) + (255 - Color.blue(c)) * t));
    }

    private static int clamp(float v) {
        return Math.max(0, Math.min(255, Math.round(v)));
    }

    // ---------- Texte ----------

    // Texte en gras avec un contour sombre : lisible sur toutes les couleurs
    public void text(Canvas c, String s, float x, float y, float sizeDp, int color, Paint.Align align) {
        textPx(c, s, x, y, sizeDp * d, color, align, 0);
    }

    // Même chose, réduit si besoin pour tenir dans maxWidth (0 = pas de limite)
    public void textPx(Canvas c, String s, float x, float y, float sizePx, int color, Paint.Align align, float maxWidth) {
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextAlign(align);
        paint.setTextSize(sizePx);
        if (maxWidth > 0) {
            float w = paint.measureText(s);
            if (w > maxWidth) {
                paint.setTextSize(sizePx * maxWidth / w);
            }
        }
        float stroke = Math.max(2 * d, paint.getTextSize() * 0.16f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        paint.setStrokeWidth(stroke);
        paint.setColor(Color.argb(Color.alpha(color), 26, 22, 38));
        c.drawText(s, x, y + d, paint); // contour un peu décalé vers le bas : effet d'ombre
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        c.drawText(s, x, y, paint);
        paint.setTypeface(Typeface.DEFAULT);
        paint.setStrokeJoin(Paint.Join.MITER);
    }

    // ---------- Bouton tuile en relief ----------

    // Dessine le bouton et retourne la face (pour y centrer le texte). Appuyé : il s'enfonce.
    public RectF button(Canvas c, RectF b, int color, boolean pressed, boolean enabled) {
        int base = enabled ? color : Color.rgb(78, 76, 92);
        float depth = pressed ? 1.5f * d : 5 * d;   // hauteur de la tranche
        float sink = pressed ? 3.5f * d : 0;        // le dessus descend quand on appuie
        float rad = Math.min(10 * d, b.height() / 3);

        // Ombre au sol
        if (!pressed) {
            paint.setColor(Color.argb(90, 0, 0, 0));
            r.set(b.left + 2 * d, b.top + 4 * d, b.right + 2 * d, b.bottom + 3 * d);
            c.drawRoundRect(r, rad, rad, paint);
        }
        // Contour
        paint.setColor(OUTLINE);
        r.set(b.left, b.top + sink, b.right, b.bottom);
        c.drawRoundRect(r, rad, rad, paint);
        // Tranche (dessous, plus sombre)
        float px = 2 * d;
        paint.setColor(shade(base, 0.58f));
        r.set(b.left + px, b.top + sink + px, b.right - px, b.bottom - px);
        c.drawRoundRect(r, rad - px, rad - px, paint);
        // Dessus
        face.set(b.left + px, b.top + sink + px, b.right - px, b.bottom - px - depth);
        paint.setColor(base);
        c.drawRoundRect(face, rad - px, rad - px, paint);
        // Ombrage du bas du dessus, puis reflet en haut
        paint.setColor(shade(base, 0.85f));
        r.set(face.left, face.centerY(), face.right, face.bottom);
        c.drawRoundRect(r, rad - px, rad - px, paint);
        paint.setColor(base);
        r.set(face.left, face.top, face.right, face.bottom - 3 * d);
        c.drawRoundRect(r, rad - px, rad - px, paint);
        paint.setColor(tint(base, 0.35f));
        r.set(face.left + rad * 0.6f, face.top + 2 * d, face.right - rad * 0.6f, face.top + 4.5f * d);
        c.drawRoundRect(r, 2 * d, 2 * d, paint);
        if (!enabled) {
            paint.setColor(Color.argb(70, 0, 0, 0));
            c.drawRoundRect(face, rad - px, rad - px, paint);
        }
        return face;
    }

    // Bouton tuile avec un libellé centré
    public void labelButton(Canvas c, RectF b, String label, int color, boolean pressed, boolean enabled, float sizeDp) {
        RectF f = button(c, b, color, pressed, enabled);
        float size = sizeDp * d;
        textPx(c, label, f.centerX(), f.centerY() + size * 0.36f, size,
                enabled ? Color.WHITE : Color.rgb(170, 168, 182), Paint.Align.CENTER, f.width() - 14 * d);
    }

    // ---------- Médaillon (bouton rond) ----------

    // Bouton rond : anneau doré, disque coloré avec reflet. Retourne le rayon du disque intérieur.
    public float medallion(Canvas c, float cx, float cy, float radius, int color, boolean pressed, int alpha) {
        float sink = pressed ? 2 * d : 0;
        cy += sink;
        if (!pressed) {
            paint.setColor(Color.argb(alpha * 90 / 255, 0, 0, 0));
            c.drawCircle(cx + 1.5f * d, cy + 3 * d, radius, paint);
        }
        paint.setColor(withAlpha(OUTLINE, alpha));
        c.drawCircle(cx, cy, radius, paint);
        paint.setColor(withAlpha(GOLD_DARK, alpha));
        c.drawCircle(cx, cy, radius - 2 * d, paint);
        paint.setColor(withAlpha(GOLD, alpha));
        c.drawCircle(cx, cy - d, radius - 3 * d, paint);
        float inner = radius - 5.5f * d;
        paint.setColor(withAlpha(OUTLINE, alpha));
        c.drawCircle(cx, cy, inner + d, paint);
        int base = pressed ? shade(color, 0.8f) : color;
        paint.setColor(withAlpha(shade(base, 0.75f), alpha));
        c.drawCircle(cx, cy, inner, paint);
        paint.setColor(withAlpha(base, alpha));
        c.drawCircle(cx, cy - inner * 0.08f, inner * 0.92f, paint);
        // Reflet en haut à gauche
        paint.setColor(Color.argb(alpha * 70 / 255, 255, 255, 255));
        r.set(cx - inner * 0.7f, cy - inner * 0.8f, cx + inner * 0.2f, cy - inner * 0.25f);
        c.drawOval(r, paint);
        return inner;
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(alpha * Color.alpha(color) / 255, Color.red(color), Color.green(color), Color.blue(color));
    }

    // ---------- Fenêtre en planches cloutées ----------

    public void panel(Canvas c, RectF p, String title, int ribbonColor) {
        float rad = 14 * d;
        // Ombre portée
        paint.setColor(Color.argb(130, 0, 0, 0));
        r.set(p.left + 3 * d, p.top + 7 * d, p.right + 3 * d, p.bottom + 7 * d);
        c.drawRoundRect(r, rad, rad, paint);
        // Contour et cadre en bois
        paint.setColor(OUTLINE);
        c.drawRoundRect(p, rad, rad, paint);
        paint.setColor(WOOD);
        r.set(p.left + 2.5f * d, p.top + 2.5f * d, p.right - 2.5f * d, p.bottom - 2.5f * d);
        c.drawRoundRect(r, rad - 2 * d, rad - 2 * d, paint);
        // Veines du bois (planches horizontales)
        paint.setColor(WOOD_DARK);
        paint.setStrokeWidth(1.5f * d);
        for (float y = p.top + 9 * d; y < p.bottom - 4 * d; y += 11 * d) {
            c.drawLine(p.left + 4 * d, y, p.right - 4 * d, y, paint);
        }
        paint.setColor(WOOD_LIGHT);
        r.set(p.left + 6 * d, p.top + 3.5f * d, p.right - 6 * d, p.top + 5 * d);
        c.drawRect(r, paint);
        // Liseré doré
        float inset = 9 * d;
        paint.setColor(OUTLINE);
        r.set(p.left + inset - 2 * d, p.top + inset - 2 * d, p.right - inset + 2 * d, p.bottom - inset + 2 * d);
        c.drawRoundRect(r, 8 * d, 8 * d, paint);
        paint.setColor(GOLD_DARK);
        r.set(p.left + inset - d, p.top + inset - d, p.right - inset + d, p.bottom - inset + d);
        c.drawRoundRect(r, 7 * d, 7 * d, paint);
        // Fond d'ardoise, plus clair en haut
        r.set(p.left + inset, p.top + inset, p.right - inset, p.bottom - inset);
        paint.setColor(SLATE_BOTTOM);
        c.drawRoundRect(r, 6 * d, 6 * d, paint);
        r.bottom = r.top + (p.height() - 2 * inset) * 0.55f;
        paint.setColor(SLATE_TOP);
        c.drawRoundRect(r, 6 * d, 6 * d, paint);
        paint.setColor(blend(SLATE_TOP, SLATE_BOTTOM));
        r.set(p.left + inset, r.bottom - 8 * d, p.right - inset, r.bottom + 8 * d);
        c.drawRect(r, paint);
        // Ombre intérieure sous le cadre
        paint.setColor(Color.argb(80, 0, 0, 0));
        r.set(p.left + inset, p.top + inset, p.right - inset, p.top + inset + 4 * d);
        c.drawRect(r, paint);
        // Clous aux quatre coins
        float n = 5.5f * d;
        nail(c, p.left + n, p.top + n);
        nail(c, p.right - n, p.top + n);
        nail(c, p.left + n, p.bottom - n);
        nail(c, p.right - n, p.bottom - n);

        if (title != null) {
            ribbon(c, p.centerX(), p.top + 20 * d, Math.min(p.width() * 0.72f, 240 * d), title, ribbonColor);
        }
    }

    private static int blend(int a, int b) {
        return Color.rgb((Color.red(a) + Color.red(b)) / 2, (Color.green(a) + Color.green(b)) / 2,
                (Color.blue(a) + Color.blue(b)) / 2);
    }

    private void nail(Canvas c, float x, float y) {
        paint.setColor(OUTLINE);
        c.drawCircle(x, y, 2.6f * d, paint);
        paint.setColor(NAIL);
        c.drawCircle(x, y, 1.7f * d, paint);
        paint.setColor(Color.WHITE);
        c.drawCircle(x - 0.5f * d, y - 0.5f * d, 0.6f * d, paint);
    }

    // ---------- Ruban de titre ----------

    // Ruban de tissu centré en (cx, cy), bouts repliés derrière, bordures dorées, titre à contour
    public void ribbon(Canvas c, float cx, float cy, float w, String title, int color) {
        float h = 38 * d;
        float tail = 16 * d;
        float left = cx - w / 2;
        float right = cx + w / 2;
        int dark = shade(color, 0.6f);
        // Bouts repliés (derrière)
        for (int side = -1; side <= 1; side += 2) {
            float x0 = side < 0 ? left + 6 * d : right - 6 * d;
            float x1 = x0 + side * (tail + 6 * d);
            path.reset();
            path.moveTo(x0, cy - h * 0.3f);
            path.lineTo(x1, cy - h * 0.3f);
            path.lineTo(x1 - side * 7 * d, cy + h * 0.15f);
            path.lineTo(x1, cy + h * 0.6f);
            path.lineTo(x0, cy + h * 0.6f);
            path.close();
            paint.setColor(OUTLINE);
            c.drawPath(path, paint);
            path.reset();
            float k = 2 * d;
            path.moveTo(x0, cy - h * 0.3f + k);
            path.lineTo(x1 - side * k, cy - h * 0.3f + k);
            path.lineTo(x1 - side * (7 * d + k), cy + h * 0.15f);
            path.lineTo(x1 - side * k, cy + h * 0.6f - k);
            path.lineTo(x0, cy + h * 0.6f - k);
            path.close();
            paint.setColor(dark);
            c.drawPath(path, paint);
        }
        // Bande principale
        paint.setColor(OUTLINE);
        r.set(left, cy - h / 2, right, cy + h / 2);
        c.drawRoundRect(r, 4 * d, 4 * d, paint);
        paint.setColor(color);
        r.set(left + 2 * d, cy - h / 2 + 2 * d, right - 2 * d, cy + h / 2 - 2 * d);
        c.drawRoundRect(r, 3 * d, 3 * d, paint);
        paint.setColor(shade(color, 0.8f));
        r.set(left + 2 * d, cy + 2 * d, right - 2 * d, cy + h / 2 - 2 * d);
        c.drawRect(r, paint);
        // Bordures dorées
        paint.setColor(GOLD);
        c.drawRect(left + 2 * d, cy - h / 2 + 4 * d, right - 2 * d, cy - h / 2 + 5.5f * d, paint);
        c.drawRect(left + 2 * d, cy + h / 2 - 5.5f * d, right - 2 * d, cy + h / 2 - 4 * d, paint);
        textPx(c, title, cx, cy + 7.5f * d, 21 * d, Color.WHITE, Paint.Align.CENTER, w - 24 * d);
    }

    // ---------- Alcôve creusée (ligne de liste) ----------

    public void inset(Canvas c, RectF b) {
        float rad = 10 * d;
        paint.setColor(Color.argb(110, 255, 255, 255));        // arête claire en bas
        r.set(b.left, b.top + d, b.right, b.bottom + 1.5f * d);
        c.drawRoundRect(r, rad, rad, paint);
        paint.setColor(OUTLINE);
        c.drawRoundRect(b, rad, rad, paint);
        paint.setColor(INSET);
        r.set(b.left + 1.5f * d, b.top + 1.5f * d, b.right - 1.5f * d, b.bottom - 1.5f * d);
        c.drawRoundRect(r, rad - d, rad - d, paint);
        paint.setColor(Color.argb(90, 0, 0, 0));                 // ombre en haut (creux)
        r.set(b.left + 1.5f * d, b.top + 1.5f * d, b.right - 1.5f * d, b.top + 5 * d);
        c.drawRoundRect(r, rad - d, rad - d, paint);
    }

    // ---------- Barre encadrée ----------

    public void bar(Canvas c, RectF b, float ratio, int color) {
        float rad = b.height() / 2;
        paint.setColor(OUTLINE);
        c.drawRoundRect(b, rad, rad, paint);
        paint.setColor(Color.rgb(46, 42, 66));
        r.set(b.left + 1.5f * d, b.top + 1.5f * d, b.right - 1.5f * d, b.bottom - 1.5f * d);
        c.drawRoundRect(r, rad, rad, paint);
        ratio = Math.max(0f, Math.min(1f, ratio));
        if (ratio > 0) {
            float inner = r.width();
            r.right = r.left + Math.max(r.height(), inner * ratio);
            paint.setColor(shade(color, 0.8f));
            c.drawRoundRect(r, rad, rad, paint);
            paint.setColor(color);
            float bottom = r.bottom;
            r.bottom = r.top + r.height() * 0.6f;
            c.drawRoundRect(r, rad, rad, paint);
            r.bottom = bottom;
            paint.setColor(Color.argb(90, 255, 255, 255));
            c.drawRect(r.left + rad * 0.6f, r.top + d, r.right - rad * 0.6f, r.top + 2.2f * d, paint);
        }
    }

    // ---------- Pièce d'or ----------

    public void coin(Canvas c, float cx, float cy, float s) {
        paint.setColor(OUTLINE);
        c.drawCircle(cx, cy, s, paint);
        paint.setColor(GOLD_DARK);
        c.drawCircle(cx, cy, s - 1.2f * d, paint);
        paint.setColor(GOLD);
        c.drawCircle(cx - 0.4f * d, cy - 0.4f * d, s - 2.2f * d, paint);
        paint.setColor(Color.argb(200, 255, 245, 200));
        c.drawCircle(cx - s * 0.3f, cy - s * 0.3f, s * 0.22f, paint);
    }
}
