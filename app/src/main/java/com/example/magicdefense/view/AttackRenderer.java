package com.example.magicdefense.view;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;

import com.example.magicdefense.model.Bolt;
import com.example.magicdefense.model.HitEffect;
import com.example.magicdefense.model.Mage;
import com.example.magicdefense.model.Projectile;
import com.example.magicdefense.model.Splash;

import java.util.Random;

// Dessin des attaques des mages, de leurs impacts et des éclaboussures.
// Chaque attaque évolue nettement avec le niveau du mage (taille, couleur et forme).
public class AttackRenderer {

    // ---------- Réglages par niveau (indice = niveau - 1) ----------
    private static final float[] ATTACK_SIZE = {0.8f, 1.0f, 1.3f, 1.6f, 2.0f};

    // Feu : braise → orange → rouge → feu bleu → soleil doré
    private static final int[] FIRE_TAIL = {Color.rgb(255, 170, 40), Color.rgb(255, 140, 40),
            Color.rgb(240, 70, 30), Color.rgb(60, 150, 255), Color.rgb(255, 200, 60)};
    private static final int[] FIRE_TAIL_LENGTH = {1, 3, 5, 6, 7};
    private static final int[] FIRE_SPARKS = {0, 2, 4, 5, 7};

    // Terre : caillou → ambre → émeraude → turquoise → or
    private static final int[] CRYSTAL_LIGHT = {Color.rgb(150, 140, 130), Color.rgb(214, 170, 95),
            Color.rgb(120, 200, 110), Color.rgb(110, 220, 200), Color.rgb(255, 215, 120)};
    private static final int[] CRYSTAL_DARK = {Color.rgb(95, 90, 85), Color.rgb(140, 95, 50),
            Color.rgb(60, 130, 60), Color.rgb(40, 140, 130), Color.rgb(200, 140, 40)};

    // Foudre : blanc → violet → cyan → bleu → or (0 = pas de halo)
    private static final int[] BOLT_CORE = {Color.WHITE, Color.rgb(255, 250, 220), Color.rgb(230, 250, 255),
            Color.rgb(220, 235, 255), Color.rgb(255, 250, 210)};
    private static final int[] BOLT_GLOW = {0, Color.rgb(170, 90, 255), Color.rgb(90, 200, 255),
            Color.rgb(60, 110, 255), Color.rgb(255, 200, 60)};
    private static final float[] BOLT_CORE_WIDTH = {1.5f, 2.2f, 3.0f, 3.8f, 4.6f}; // en dp
    private static final float[] BOLT_GLOW_WIDTH = {0f, 7f, 9f, 12f, 15f};
    private static final int[] BOLT_BRANCHES = {0, 0, 1, 2, 3};

    // Eau (niveau 3) : position des gouttes de l'averse, en unités
    private static final float[][] DROP_POS = {{0, 0}, {-1.5f, -1.3f}, {1.3f, -2.1f}, {-0.5f, -3.0f},
            {1.9f, -3.6f}};

    // ---------- Outils de dessin ----------
    private final float density;
    private final Paint paint = new Paint();
    private final Path path = new Path();
    private final RectF tmpRect = new RectF();
    private final Matrix shaderMatrix = new Matrix();
    private final Random boltRandom = new Random();
    private final float[] boltPoints = new float[20];
    private int frame = 0;

    // Dégradés créés une seule fois, puis déplacés et agrandis à chaque dessin
    private final RadialGradient[] fireGradients = {
            gradient(new int[]{Color.rgb(255, 250, 200), Color.rgb(255, 190, 60), Color.argb(0, 255, 120, 20)},
                    new float[]{0f, 0.5f, 1f}),
            gradient(new int[]{Color.rgb(255, 250, 215), Color.rgb(255, 205, 70), Color.rgb(255, 105, 20), Color.argb(0, 200, 40, 10)},
                    new float[]{0f, 0.3f, 0.65f, 1f}),
            gradient(new int[]{Color.rgb(255, 240, 200), Color.rgb(255, 150, 40), Color.rgb(220, 40, 20), Color.argb(0, 150, 20, 10)},
                    new float[]{0f, 0.3f, 0.65f, 1f}),
            gradient(new int[]{Color.WHITE, Color.rgb(150, 230, 255), Color.rgb(40, 120, 255), Color.argb(0, 20, 40, 200)},
                    new float[]{0f, 0.3f, 0.65f, 1f}),
            gradient(new int[]{Color.WHITE, Color.rgb(255, 240, 160), Color.rgb(255, 190, 40), Color.argb(0, 255, 120, 0)},
                    new float[]{0f, 0.3f, 0.65f, 1f}),
    };
    private final RadialGradient bubbleGradient = gradient(
            new int[]{Color.argb(230, 220, 245, 255), Color.argb(220, 90, 170, 245), Color.argb(240, 40, 110, 210)},
            new float[]{0f, 0.6f, 1f});

    public AttackRenderer(float density) {
        this.density = density;
        paint.setAntiAlias(true);
    }

    // À appeler à chaque image (sert aux animations)
    public void setFrame(int frame) {
        this.frame = frame;
    }

    private static RadialGradient gradient(int[] colors, float[] stops) {
        return new RadialGradient(0, 0, 100, colors, stops, Shader.TileMode.CLAMP);
    }

    // Peint un cercle avec un dégradé (créé autour de (0,0), rayon 100) placé et agrandi au bon endroit
    private void drawGradientCircle(Canvas canvas, RadialGradient g, float x, float y, float r) {
        shaderMatrix.setScale(r / 100f, r / 100f);
        shaderMatrix.postTranslate(x, y);
        g.setLocalMatrix(shaderMatrix);
        paint.setColor(Color.WHITE);
        paint.setShader(g);
        canvas.drawCircle(x, y, r, paint);
        paint.setShader(null); // très important : sinon tout le reste serait peint avec le dégradé
    }

    private static int withAlpha(int color, int alpha) {
        return Color.argb(Math.max(0, Math.min(255, alpha)), Color.red(color), Color.green(color), Color.blue(color));
    }

    // =====================================================================
    //  Projectiles
    // =====================================================================

    public void drawProjectile(Canvas canvas, Projectile p) {
        float size = p.radius * 1.3f * ATTACK_SIZE[p.level - 1];
        switch (p.type) {
            case Mage.FIRE:
                drawFireball(canvas, p, size);
                break;
            case Mage.EARTH:
                drawCrystal(canvas, p, size);
                break;
            case Mage.WATER:
                drawWater(canvas, p);
                break;
        }
    }

    // ---------- Feu ----------

    // Boule de feu : couleur, traînée et étincelles selon le niveau ; niveau 5 : aura et flammes en orbite
    private void drawFireball(Canvas canvas, Projectile p, float size) {
        int lv = p.level - 1;
        float perpX = -p.dirY; // perpendiculaire au vol (pour l'ondulation)
        float perpY = p.dirX;

        if (p.level >= 5) {
            paint.setColor(Color.argb(45, 255, 170, 40));
            canvas.drawCircle(p.x, p.y, size * 2.1f, paint);
            paint.setColor(Color.argb(230, 255, 220, 100));
            for (int j = 0; j < 4; j++) {
                double a = p.age * 0.2 + j * Math.PI / 2;
                canvas.drawCircle(p.x + (float) Math.cos(a) * size * 1.9f,
                        p.y + (float) Math.sin(a) * size * 1.9f, size * 0.35f, paint);
            }
        }

        // Traînée de flammes qui ondulent
        int tail = FIRE_TAIL_LENGTH[lv];
        for (int k = tail; k >= 1; k--) {
            float t = k / (float) (tail + 1);   // 0 près de la boule → 1 au bout
            float dist = k * size * 0.8f;
            float wobble = (float) Math.sin(p.age * 0.6 + k) * size * 0.25f;
            paint.setColor(withAlpha(FIRE_TAIL[lv], (int) (220 * (1 - t))));
            canvas.drawCircle(p.x - p.dirX * dist + perpX * wobble, p.y - p.dirY * dist + perpY * wobble,
                    size * (1.05f - 0.8f * t), paint);
        }

        // Étincelles qui s'échappent et s'éteignent
        int sparks = FIRE_SPARKS[lv];
        for (int j = 0; j < sparks; j++) {
            float t = ((p.age + j * 7) % 20) / 20f;
            float side = (j - (sparks - 1) / 2f) * size * 0.45f;
            paint.setColor(Color.argb((int) (255 * (1 - t)), 255, 230, 150));
            canvas.drawCircle(p.x - p.dirX * size * (1.5f + 3 * t) + perpX * side,
                    p.y - p.dirY * size * (1.5f + 3 * t) + perpY * side, size * 0.18f, paint);
        }

        drawGradientCircle(canvas, fireGradients[lv], p.x, p.y, size * 1.4f);
    }

    // ---------- Terre ----------

    // Caillou (1), cristal (2), grappe (3+), cristal rayonnant et éclats en orbite (4-5)
    private void drawCrystal(Canvas canvas, Projectile p, float size) {
        int light = CRYSTAL_LIGHT[p.level - 1];
        int dark = CRYSTAL_DARK[p.level - 1];

        // Poussière derrière
        for (int k = 1; k <= 3; k++) {
            float dist = k * size * 0.9f;
            paint.setColor(withAlpha(dark, 110 - k * 30));
            canvas.drawCircle(p.x - p.dirX * dist, p.y - p.dirY * dist, size * (0.45f - 0.1f * k), paint);
        }

        // Niveaux 4 et 5 : le cristal rayonne
        if (p.level >= 4) {
            paint.setColor(withAlpha(light, p.level == 5 ? 55 : 40));
            canvas.drawCircle(p.x, p.y, size * (p.level == 5 ? 2.0f : 1.7f), paint);
        }

        // Niveau 1 : simple caillou
        if (p.level == 1) {
            paint.setColor(Color.rgb(60, 50, 45));
            canvas.drawCircle(p.x, p.y, size * 0.8f, paint);
            paint.setColor(light);
            canvas.drawCircle(p.x, p.y, size * 0.7f, paint);
            paint.setColor(dark);
            canvas.drawCircle(p.x + size * 0.2f, p.y + size * 0.2f, size * 0.4f, paint);
            return;
        }

        // Cristal principal qui tourne (niveau 3+ : deux petits cristaux accrochés)
        canvas.save();
        canvas.rotate(p.age * 14f, p.x, p.y);
        drawCrystalShape(canvas, p.x, p.y, size * 0.75f, size * 1.5f, light, dark, size, true);
        if (p.level >= 3) {
            for (int side = -1; side <= 1; side += 2) {
                float cx = p.x + side * size * 0.8f;
                float cy = p.y + size * 0.5f;
                canvas.save();
                canvas.rotate(side * 34f, cx, cy);
                drawCrystalShape(canvas, cx, cy, size * 0.4f, size * 0.9f, light, dark, size * 0.5f, true);
                canvas.restore();
            }
        }
        canvas.restore();

        // Éclats en orbite : 2 au niveau 4, 3 au niveau 5
        int shards = p.level >= 5 ? 3 : (p.level == 4 ? 2 : 0);
        for (int i = 0; i < shards; i++) {
            double a = p.age * 0.25 + i * 2 * Math.PI / shards;
            drawCrystalShape(canvas, p.x + (float) Math.cos(a) * size * 2.2f,
                    p.y + (float) Math.sin(a) * size * 2.2f,
                    size * 0.3f, size * 0.6f, light, dark, size * 0.4f, false);
        }
    }

    // Losange à facettes : face claire, moitié gauche sombre, reflet, et contour si demandé
    private void drawCrystalShape(Canvas canvas, float x, float y, float w, float h,
                                  int light, int dark, float glintSize, boolean outline) {
        diamondPath(x, y, w, h);
        paint.setColor(light);
        canvas.drawPath(path, paint);

        path.reset();
        path.moveTo(x, y - h);
        path.lineTo(x - w, y);
        path.lineTo(x, y + h);
        path.close();
        paint.setColor(dark);
        canvas.drawPath(path, paint);

        paint.setColor(Color.argb(200, 255, 245, 215));
        paint.setStrokeWidth(glintSize * 0.18f);
        canvas.drawLine(x + w * 0.2f, y - h * 0.6f, x + w * 0.5f, y - h * 0.1f, paint);

        if (outline) {
            diamondPath(x, y, w, h);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.2f * density);
            paint.setColor(Color.rgb(60, 40, 25));
            canvas.drawPath(path, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    private void diamondPath(float x, float y, float w, float h) {
        path.reset();
        path.moveTo(x, y - h);
        path.lineTo(x + w, y);
        path.lineTo(x, y + h);
        path.lineTo(x - w, y);
        path.close();
    }

    // ---------- Eau ----------

    // Goutte (1), bulle (2), averse (3) tombent du ciel ; cyclone (4) et tsunami (5) partent du bâton
    private void drawWater(Canvas canvas, Projectile p) {
        float base = p.radius * 1.3f;
        switch (p.level) {
            case 1:
                drawDrop(canvas, p.x, p.y, base * 0.6f, 255);
                break;
            case 2:
                drawBubble(canvas, p, base * 1.3f);
                break;
            case 3:
                drawShower(canvas, p, base);
                break;
            case 4:
                drawCyclone(canvas, p, base * 1.1f);
                break;
            default:
                drawTsunami(canvas, p, base * 1.2f);
                break;
        }
    }

    // Goutte : un rond + une pointe vers le haut, un reflet et une traînée de chute
    private void drawDrop(Canvas canvas, float x, float y, float s, int alpha) {
        paint.setColor(Color.argb(alpha / 3, 170, 215, 255));
        paint.setStrokeWidth(s * 0.35f);
        canvas.drawLine(x, y - s * 1.8f, x, y - s * 5f, paint);

        paint.setColor(Color.argb(alpha, 80, 160, 245));
        canvas.drawCircle(x, y, s, paint);
        path.reset();
        path.moveTo(x, y - s * 2f);               // pointe
        path.lineTo(x + s * 0.87f, y - s * 0.5f);  // raccord tangent au rond
        path.lineTo(x - s * 0.87f, y - s * 0.5f);
        path.close();
        canvas.drawPath(path, paint);

        paint.setColor(Color.argb(alpha, 230, 248, 255));
        canvas.drawCircle(x - s * 0.35f, y - s * 0.2f, s * 0.28f, paint);
    }

    // Niveau 2 : bulle d'eau suivie de gouttelettes
    private void drawBubble(Canvas canvas, Projectile p, float r) {
        for (int k = 1; k <= 3; k++) {
            drawDrop(canvas, p.x + (k - 2) * r * 0.9f, p.y - r * (1.6f + k * 0.9f), r * 0.25f, 220 - k * 40);
        }
        drawGradientCircle(canvas, bubbleGradient, p.x, p.y, r);
        paint.setColor(Color.argb(220, 255, 255, 255)); // reflet
        canvas.drawCircle(p.x - r * 0.35f, p.y - r * 0.35f, r * 0.25f, paint);
    }

    // Niveau 3 : averse de 5 gouttes sous un nuage clair
    private void drawShower(Canvas canvas, Projectile p, float base) {
        float unit = base * 1.35f;
        for (int i = 0; i < DROP_POS.length; i++) {
            drawDrop(canvas, p.x + DROP_POS[i][0] * unit, p.y + DROP_POS[i][1] * unit,
                    base * 0.73f * (1 - i * 0.04f), 255 - i * 12);
        }
        float cx = p.x;
        float cy = p.y + DROP_POS[DROP_POS.length - 1][1] * unit - base * 1.6f;
        paint.setColor(Color.argb(230, 205, 215, 235));
        canvas.drawCircle(cx - base * 1.1f, cy, base * 0.9f, paint);
        canvas.drawCircle(cx + base * 1.1f, cy, base * 0.9f, paint);
        canvas.drawCircle(cx, cy - base * 0.5f, base * 1.2f, paint);
        canvas.drawRect(cx - base * 2f, cy, cx + base * 2f, cy + base * 0.9f, paint);
        paint.setColor(Color.argb(230, 154, 161, 176)); // dessous plus sombre
        canvas.drawRect(cx - base * 2f, cy + base * 0.5f, cx + base * 2f, cy + base * 0.9f, paint);
    }

    // Niveau 4 : trombe d'eau partie du bâton, qui file vers l'ennemi ; sa pointe touche le sol
    private void drawCyclone(Canvas canvas, Projectile p, float s) {
        float h = s * 5.5f;
        int n = 14;

        // Entonnoir : bord gauche de haut en bas, puis bord droit de bas en haut
        path.reset();
        for (int i = 0; i <= n; i++) {
            float f = i / (float) n;
            float px = funnelX(p, s, f) - funnelW(s, f);
            float py = p.y - h + f * h;
            if (i == 0) {
                path.moveTo(px, py);
            } else {
                path.lineTo(px, py);
            }
        }
        for (int i = n; i >= 0; i--) {
            float f = i / (float) n;
            path.lineTo(funnelX(p, s, f) + funnelW(s, f), p.y - h + f * h);
        }
        path.close();
        paint.setColor(Color.argb(170, 60, 130, 215));
        canvas.drawPath(path, paint);

        // Ouverture du haut
        float topX = funnelX(p, s, 0);
        float topW = funnelW(s, 0);
        tmpRect.set(topX - topW, p.y - h - topW * 0.3f, topX + topW, p.y - h + topW * 0.3f);
        paint.setColor(Color.argb(200, 110, 180, 240));
        canvas.drawOval(tmpRect, paint);

        // Stries en spirale qui descendent le long de l'entonnoir
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.6f * density);
        paint.setColor(Color.argb(210, 225, 245, 255));
        for (int k = 0; k < 6; k++) {
            float f0 = ((k / 6f) + p.age * 0.03f) % 1f;
            path.reset();
            for (int j = 0; j <= 8; j++) {
                float f = Math.min(1f, f0 + j * 0.02f);
                float t = j / 8f;
                float cx = funnelX(p, s, f);
                float w = funnelW(s, f);
                float px = cx - w + 2 * w * t;
                float py = p.y - h + f * h + w * 0.25f * (float) Math.sin(t * Math.PI);
                if (j == 0) {
                    path.moveTo(px, py);
                } else {
                    path.lineTo(px, py);
                }
            }
            canvas.drawPath(path, paint);
        }
        paint.setStyle(Paint.Style.FILL);

        // Gouttes aspirées qui tournent autour
        paint.setColor(Color.argb(230, 190, 230, 255));
        for (int j = 0; j < 6; j++) {
            double a = p.age * 0.35 + j * 2 * Math.PI / 6;
            float f = 0.15f + 0.6f * ((j * 0.37f) % 1f);
            float cx = funnelX(p, s, f);
            float w = funnelW(s, f);
            canvas.drawCircle(cx + (float) Math.cos(a) * (w + s * 0.6f),
                    p.y - h + f * h + (float) Math.sin(a) * w * 0.3f, s * 0.17f, paint);
        }
    }

    // Largeur de l'entonnoir à la hauteur f (0 = haut, large ; 1 = pointe)
    private float funnelW(float s, float f) {
        return s * (2.6f - 2.3f * f);
    }

    // Centre de l'entonnoir : le haut ondule, la pointe reste sur la cible
    private float funnelX(Projectile p, float s, float f) {
        return p.x + (float) Math.sin(p.age * 0.3 + f * 3) * s * 0.4f * (1 - f);
    }

    // Niveau 5 : raz-de-marée vu de dessus, qui part du bâton et fonce vers l'ennemi :
    // un mur d'eau en croissant, une crête d'écume à l'avant, la masse d'eau qui suit derrière
    private void drawTsunami(Canvas canvas, Projectile p, float s) {
        // La vague est dessinée "vers le haut", puis tournée dans la direction du vol
        float angle = (float) Math.toDegrees(Math.atan2(p.dirY, p.dirX)) + 90f;
        canvas.save();
        canvas.rotate(angle, p.x, p.y);

        float x = p.x;
        float y = p.y;
        float w = s * 4.5f;      // demi-largeur de la vague
        float bulge = s * 1.6f;  // avancée du centre de la crête
        float trail = s * 6f;    // longueur de la masse d'eau derrière
        float edge = y + s * 0.6f;
        float[] front = {x - w, edge, x - w * 0.6f, y - bulge, x + w * 0.6f, y - bulge, x + w, edge};

        // Masse d'eau sombre : la crête à l'avant, puis une traîne arrondie derrière
        path.reset();
        path.moveTo(x - w, edge);
        cubic(front);
        path.cubicTo(x + w * 0.7f, y + trail * 0.6f, x + w * 0.2f, y + trail, x, y + trail);
        path.cubicTo(x - w * 0.2f, y + trail, x - w * 0.7f, y + trail * 0.6f, x - w, edge);
        path.close();
        paint.setColor(Color.argb(225, 25, 85, 170));
        canvas.drawPath(path, paint);

        // Face claire de la vague, juste derrière l'écume
        path.reset();
        path.moveTo(x - w * 0.9f, edge);
        path.cubicTo(x - w * 0.55f, y - bulge * 0.9f, x + w * 0.55f, y - bulge * 0.9f, x + w * 0.9f, edge);
        path.cubicTo(x + w * 0.5f, y + s * 1.6f, x - w * 0.5f, y + s * 1.6f, x - w * 0.9f, edge);
        path.close();
        paint.setColor(Color.argb(230, 80, 160, 235));
        canvas.drawPath(path, paint);

        // Courants qui défilent vers l'arrière dans la masse d'eau
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f * density);
        paint.setColor(Color.argb(150, 150, 205, 255));
        for (int k = 0; k < 4; k++) {
            float off = ((k / 4f + p.age * 0.04f) % 1f) * trail * 0.75f + s * 1.8f;
            float ww = w * 0.55f * (1 - off / (trail * 1.1f));
            path.reset();
            path.moveTo(x - ww, y + off);
            path.cubicTo(x - ww * 0.5f, y + off - s * 0.5f, x + ww * 0.5f, y + off - s * 0.5f, x + ww, y + off);
            canvas.drawPath(path, paint);
        }
        paint.setStyle(Paint.Style.FILL);

        // Écume qui bouillonne le long de la crête
        paint.setColor(Color.argb(240, 240, 250, 255));
        for (int i = 0; i < 18; i++) {
            float t = i / 17f;
            canvas.drawCircle(bez(front, t, 0), bez(front, t, 1),
                    s * (0.4f + 0.15f * (float) Math.sin(p.age * 0.5 + i)), paint);
        }

        // Embruns projetés devant la vague
        for (int j = 0; j < 9; j++) {
            float t = ((p.age + j * 5) % 16) / 16f;
            paint.setColor(Color.argb((int) (230 * (1 - t)), 230, 245, 255));
            canvas.drawCircle(x + (j - 4) * w * 0.22f, y - bulge - s * (0.5f + 2.5f * t),
                    s * 0.18f * (1 - t * 0.6f), paint);
        }

        canvas.restore();
    }

    // Ajoute au path une courbe de Bézier (c = départ, contrôle 1, contrôle 2, arrivée)
    private void cubic(float[] c) {
        path.cubicTo(c[2], c[3], c[4], c[5], c[6], c[7]);
    }

    // Point d'une courbe de Bézier à l'instant t (axis : 0 = x, 1 = y)
    private static float bez(float[] c, float t, int axis) {
        float u = 1 - t;
        return u * u * u * c[axis] + 3 * u * u * t * c[2 + axis]
                + 3 * u * t * t * c[4 + axis] + t * t * t * c[6 + axis];
    }

    // =====================================================================
    //  Foudre
    // =====================================================================

    // Éclair entre le bâton et la cible : couleur, épaisseur et ramifications selon le niveau
    public void drawBolt(Canvas canvas, Bolt b) {
        int alpha = (int) (255 * (1 - b.getProgress()));
        float dx = b.toX - b.fromX;
        float dy = b.toY - b.fromY;
        if (dx * dx + dy * dy < 1) {
            return;
        }
        // Le zigzag change toutes les 2 images : l'éclair crépite
        boltRandom.setSeed((frame / 2) * 7919L + (long) (b.toX * 31 + b.fromY));

        // Niveau 4+ : éclat lumineux à l'embout du bâton
        if (b.level >= 4) {
            paint.setColor(withAlpha(BOLT_GLOW[b.level - 1], alpha * 90 / 255));
            canvas.drawCircle(b.fromX, b.fromY, (2 + 1.5f * b.level) * density, paint);
        }

        drawZigzag(canvas, b.fromX, b.fromY, b.toX, b.toY, 18 * density, b.level, alpha, true);
        if (b.level >= 5) {
            // Niveau 5 : un second éclair, plus fin et plus agité
            drawZigzag(canvas, b.fromX, b.fromY, b.toX, b.toY, 28 * density, b.level, alpha * 160 / 255, false);
        }
    }

    // Zigzag de (x1, y1) à (x2, y2), avec des ramifications à partir du niveau 3
    private void drawZigzag(Canvas canvas, float x1, float y1, float x2, float y2,
                            float amplitude, int level, int alpha, boolean branches) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float perpX = -dy / len;
        float perpY = dx / len;
        int segments = 9;

        path.reset();
        path.moveTo(x1, y1);
        for (int i = 1; i < segments; i++) {
            float f = i / (float) segments;
            float jitter = (boltRandom.nextFloat() - 0.5f) * amplitude;
            float px = x1 + dx * f + perpX * jitter;
            float py = y1 + dy * f + perpY * jitter;
            boltPoints[2 * i] = px;     // on garde les points pour y accrocher les ramifications
            boltPoints[2 * i + 1] = py;
            path.lineTo(px, py);
        }
        path.lineTo(x2, y2);
        strokeBolt(canvas, level, alpha, branches ? 1f : 0.6f);

        if (!branches) {
            return;
        }
        double mainAngle = Math.atan2(dy, dx);
        for (int j = 0; j < BOLT_BRANCHES[level - 1]; j++) {
            int idx = 2 + j * 2;                                // départ : points 2, 4 et 6
            float bx = boltPoints[2 * idx];
            float by = boltPoints[2 * idx + 1];
            double a = mainAngle + (j % 2 == 0 ? 0.6 : -0.6);   // alternativement à gauche et à droite
            float branchLen = len * 0.25f;
            path.reset();
            path.moveTo(bx, by);
            for (int s = 1; s <= 3; s++) {
                float f = s / 3f;
                float jitter = (boltRandom.nextFloat() - 0.5f) * amplitude * 0.6f;
                path.lineTo(bx + (float) Math.cos(a) * branchLen * f - (float) Math.sin(a) * jitter,
                        by + (float) Math.sin(a) * branchLen * f + (float) Math.cos(a) * jitter);
            }
            strokeBolt(canvas, level, alpha, 0.5f); // ramifications plus fines
        }
    }

    // Halo (à partir du niveau 2) et cœur lumineux, aux couleurs et épaisseurs du niveau
    private void strokeBolt(Canvas canvas, int level, int alpha, float widthFactor) {
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
        int glow = BOLT_GLOW[level - 1];
        if (glow != 0) {
            paint.setColor(withAlpha(glow, alpha / 2));
            paint.setStrokeWidth(BOLT_GLOW_WIDTH[level - 1] * density * widthFactor);
            canvas.drawPath(path, paint);
        }
        paint.setColor(withAlpha(BOLT_CORE[level - 1], alpha));
        paint.setStrokeWidth(BOLT_CORE_WIDTH[level - 1] * density * widthFactor);
        canvas.drawPath(path, paint);
        paint.setStrokeJoin(Paint.Join.MITER);
        paint.setStyle(Paint.Style.FILL);
    }

    // =====================================================================
    //  Impacts
    // =====================================================================

    // Explosion (Feu), éclats de cristal (Terre), étincelles (Foudre), aux couleurs du niveau
    public void drawHit(Canvas canvas, HitEffect hit) {
        float t = hit.getProgress();
        int alpha = (int) (255 * (1 - t));
        float u = (3 + 0.6f * hit.level) * density;
        int count = 3 + hit.level; // braises / éclats / étincelles

        switch (hit.type) {
            case Mage.FIRE:
                paint.setColor(withAlpha(FIRE_TAIL[hit.level - 1], alpha / 2));
                canvas.drawCircle(hit.x, hit.y, u * (2 + 4 * t), paint);
                paint.setColor(Color.argb(alpha, 255, 220, 120));
                for (int j = 0; j < count; j++) {
                    double a = j * 2 * Math.PI / count + hit.x;
                    float r = u * (1 + 6 * t);
                    canvas.drawCircle(hit.x + (float) Math.cos(a) * r, hit.y + (float) Math.sin(a) * r,
                            u * 0.4f * (1 - t) + 1, paint);
                }
                break;

            case Mage.EARTH:
                paint.setColor(withAlpha(CRYSTAL_LIGHT[hit.level - 1], alpha));
                for (int j = 0; j < count; j++) {
                    double a = j * 2 * Math.PI / count + hit.y;
                    float r = u * (1 + 5 * t);
                    float cx = hit.x + (float) Math.cos(a) * r;
                    float cy = hit.y + (float) Math.sin(a) * r;
                    float s = u * 0.8f;
                    path.reset();
                    path.moveTo(cx, cy - s);
                    path.lineTo(cx + s * 0.6f, cy + s * 0.6f);
                    path.lineTo(cx - s * 0.6f, cy + s * 0.6f);
                    path.close();
                    canvas.drawPath(path, paint);
                }
                break;

            case Mage.LIGHTNING:
                int glow = BOLT_GLOW[hit.level - 1] != 0 ? BOLT_GLOW[hit.level - 1] : BOLT_CORE[hit.level - 1];
                paint.setColor(withAlpha(glow, alpha / 2));
                canvas.drawCircle(hit.x, hit.y, u * (3 - t), paint);
                paint.setColor(Color.argb(alpha, 255, 250, 200));
                paint.setStrokeWidth(2 * density);
                for (int j = 0; j < count; j++) {
                    double a = j * 2 * Math.PI / count + frame * 0.5;
                    float r1 = u * 1.2f;
                    float r2 = u * (2.5f + 2 * t);
                    canvas.drawLine(hit.x + (float) Math.cos(a) * r1, hit.y + (float) Math.sin(a) * r1,
                            hit.x + (float) Math.cos(a) * r2, hit.y + (float) Math.sin(a) * r2, paint);
                }
                break;
        }
    }

    // Éclaboussure de l'Eau à la taille de la zone ; tourbillon (4) et déferlante (5)
    public void drawSplash(Canvas canvas, Splash s) {
        float t = s.getProgress();                 // 0 → 1
        float r = s.radius * (0.3f + 0.7f * t);    // de 30 % à 100 % du rayon
        int alpha = (int) (255 * (1 - t));         // de opaque à invisible

        // Intérieur de la zone, très transparent, puis contour
        paint.setColor(Color.argb(alpha / 4, 70, 150, 255));
        canvas.drawCircle(s.x, s.y, r, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3 * density);
        paint.setColor(Color.argb(alpha, 70, 150, 255));
        canvas.drawCircle(s.x, s.y, r, paint);
        paint.setStyle(Paint.Style.FILL);

        // Niveau 4 : tourbillon au sol (ellipses aplaties, comme vu en perspective)
        if (s.level >= 4) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(2.5f * density);
            paint.setColor(Color.argb(alpha, 200, 235, 255));
            for (int k = 0; k < 3; k++) {
                float rr = r * (0.35f + 0.25f * k);
                tmpRect.set(s.x - rr, s.y - rr * 0.6f, s.x + rr, s.y + rr * 0.6f);
                canvas.drawArc(tmpRect, frame * 8f + k * 120, 200, false, paint);
            }
            paint.setStyle(Paint.Style.FILL);
        }

        // Niveau 5 : déferlante, anneau d'écume et embruns projetés
        if (s.level >= 5) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(5 * density);
            paint.setColor(Color.argb(alpha, 240, 250, 255));
            canvas.drawCircle(s.x, s.y, r * 0.9f, paint);
            paint.setStyle(Paint.Style.FILL);
            for (int j = 0; j < 10; j++) {
                double a = j * Math.PI / 5;
                float dist = s.radius * (0.6f + 0.8f * t);
                canvas.drawCircle(s.x + (float) Math.cos(a) * dist, s.y + (float) Math.sin(a) * dist,
                        3 * density * (1 - t) + 1, paint);
            }
        }
    }
}
