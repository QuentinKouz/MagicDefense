package com.example.magicdefense.view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

import com.example.magicdefense.controller.TouchController;
import com.example.magicdefense.model.Board;
import com.example.magicdefense.model.Bolt;
import com.example.magicdefense.model.Enemy;
import com.example.magicdefense.model.GameModel;
import com.example.magicdefense.model.GameState;
import com.example.magicdefense.model.HitEffect;
import com.example.magicdefense.model.Mage;
import com.example.magicdefense.model.Projectile;
import com.example.magicdefense.model.Splash;

// Tout le dessin : lit le modèle et l'affiche, sans jamais le modifier
public class GameRenderer {
    private final Paint paint = new Paint();
    private final Board board;
    private final Hud hud;
    private final float density;
    private final RectF gaugeOval = new RectF();
    private final Path path = new Path(); // pour dessiner les icônes (triangle, flèche)
    private final RectF tmpRect = new RectF(); // rectangle réutilisable pour le dessin
    private int frame = 0; // compteur d'images, pour l'animation de pulsation
    private boolean night = false; // mode nuit pour l'image en cours
    private GameState lastState = null;
    private int stateFrames = 0; // images depuis le dernier changement d'écran
    // Sprites pixel art (créés une seule fois)
    private final Bitmap[][] mageSprites = Sprites.buildMages();       // [élément][niveau - 1]
    private final Bitmap[] goblinDay = Sprites.buildGoblins(false);    // 2 images de marche
    private final Bitmap[] goblinNight = Sprites.buildGoblins(true);   // avec la torche
    private final Paint spritePaint = new Paint();                     // pixels nets
    private final Paint stunnedPaint = new Paint();                    // teinte bleu glacé

    private final AttackRenderer attacks;
    private final TerrainRenderer terrain = new TerrainRenderer(); // décor pixel art

    public GameRenderer(Board board, Hud hud, float density) {
        this.board = board;
        this.hud = hud;
        this.density = density;
        paint.setAntiAlias(true);
        // Agrandissement sans lissage : chaque pixel du sprite reste un carré net (style rétro)
        spritePaint.setFilterBitmap(false);
        stunnedPaint.setFilterBitmap(false);
        // Gobelin étourdi : couleurs multipliées vers le bleu, puis éclaircies
        stunnedPaint.setColorFilter(new LightingColorFilter(0x6688FF, 0x203050));
        attacks = new AttackRenderer(density);
    }

    // À appeler quand la taille de l'écran change (après board.resize et hud.resize)
    public void onResize() {
        terrain.build(board);
    }

    public void draw(Canvas canvas, GameModel model, TouchController touch, float shakeProgress) {
        frame++;
        attacks.setFrame(frame);
        if (model.getState() != lastState) {
            lastState = model.getState();
            stateFrames = 0;  // on vient de changer d'écran
        } else {
            stateFrames++;
        }
        night = model.isNightMode();
        canvas.drawColor(model.isNightMode() ? Palette.BACKGROUND_NIGHT : Palette.BACKGROUND);

        switch (model.getState()) {
            case START:
                drawStartScreen(canvas, model, touch.getPressedMenuButton());
                break;
            case PLAYING:
                drawGame(canvas, model, touch, shakeProgress);
                break;
            case PAUSED:
                drawPauseScreen(canvas, model, touch);
                break;
            case GAME_OVER:
                drawGameOverScreen(canvas, model, touch);
                break;
            case OPTIONS:
                drawOptionsScreen(canvas, model, touch);
                break;
            case VICTORY:
                drawVictoryScreen(canvas, model, touch);
                break;
        }
    }

    // ---------- Écrans ----------
    // Accueil : carte des niveaux reliés par un chemin, bulle, JOUER, options et son
    private void drawStartScreen(Canvas canvas, GameModel model, int pressed) {
        paint.setColor(Color.WHITE);
        // Titre un peu plus petit sur un écran étroit, pour ne pas toucher les boutons
        float titleSize = Math.min(26, board.playWidth / density / 16);
        drawCenteredText(canvas, "MAGIC DEFENSE", 70 * density + board.topShift, titleSize); // plus petit : laisse la place aux boutons

        // Chemin entre les niveaux
        paint.setColor(Palette.MAP_PATH);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6 * density);
        for (int lv = 1; lv < GameModel.MAX_LEVELS; lv++) {
            RectF a = hud.levelNodes[lv];
            RectF b = hud.levelNodes[lv + 1];
            canvas.drawLine(a.centerX(), a.centerY(), b.centerX(), b.centerY(), paint);
        }
        paint.setStyle(Paint.Style.FILL);

        for (int lv = 1; lv <= GameModel.MAX_LEVELS; lv++) {
            drawLevelNode(canvas, model, lv, pressed == lv);
        }
        // Écran assez haut : bulle au-dessus du niveau choisi (maquette).
        // Écran court (petit téléphone, tablette en paysage) : la bulle cacherait le niveau
        // du dessus, alors le niveau choisi est écrit sur le bouton JOUER.
        boolean roomForBubble = hud.levelNodes[1].centerY() - hud.levelNodes[2].centerY() >= 90 * density;
        String playText = "JOUER";
        if (roomForBubble) {
            drawLevelBubble(canvas, model);
        } else {
            playText = "JOUER · NIVEAU " + model.getSelectedLevel();
        }

        drawPillButton(canvas, hud.playButton, playText, Palette.RESUME, pressed == Hud.HOME_PLAY);

        // Options
        RectF o = hud.homeOptionsButton;
        drawRoundIconButton(canvas, o, pressed == Hud.HOME_OPTIONS);
        paint.setColor(Color.WHITE);
        drawGearIcon(canvas, o.centerX(), o.centerY(), o.width() * 0.5f, Palette.MENU_BUTTON);

        drawSoundButton(canvas, model, pressed == Hud.HOME_SOUND, false);
    }

    // Bouton rond de l'accueil, avec le feedback d'appui
    private void drawRoundIconButton(Canvas canvas, RectF b, boolean pressed) {
        paint.setColor(Palette.MENU_BUTTON);
        canvas.drawCircle(b.centerX(), b.centerY(), b.width() / 2, paint);
        if (pressed) {
            paint.setColor(Color.argb(90, 255, 255, 255));
            canvas.drawCircle(b.centerX(), b.centerY(), b.width() / 2, paint);
        }
    }

    // Un niveau : cercle numéroté (ou cadenas), meilleures étoiles en dessous
    private void drawLevelNode(Canvas canvas, GameModel model, int lv, boolean pressed) {
        RectF n = hud.levelNodes[lv];
        float cx = n.centerX();
        float cy = n.centerY();
        float r = 28 * density;
        boolean unlocked = lv <= model.getUnlockedLevel();

        // Anneau doré autour du niveau sélectionné
        if (lv == model.getSelectedLevel()) {
            paint.setColor(Palette.GOLD);
            canvas.drawCircle(cx, cy, r + 5 * density, paint);
        }

        paint.setColor(unlocked ? Palette.MENU_BUTTON : Palette.DISABLED);
        canvas.drawCircle(cx, cy, r, paint);
        if (pressed && unlocked) {
            paint.setColor(Color.argb(90, 255, 255, 255)); // feedback d'appui
            canvas.drawCircle(cx, cy, r, paint);
        }

        if (unlocked) {
            paint.setColor(Color.WHITE);
            paint.setTextSize(22 * density);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(String.valueOf(lv), cx, cy + 8 * density, paint);
        } else {
            drawLock(canvas, cx, cy, r * 0.45f);
        }

        // Couronne sur le dernier niveau
        if (lv == GameModel.MAX_LEVELS) {
            drawCrown(canvas, cx, cy - r - 14 * density, 14 * density);
        }

        // Meilleures étoiles obtenues
        int stars = model.getBestStars(lv);
        if (stars > 0) {
            for (int i = 0; i < 3; i++) {
                drawStar(canvas, cx + (i - 1) * 16 * density, cy + r + 12 * density,
                        7 * density, i < stars);
            }
        }
    }

    // Bulle au-dessus du niveau sélectionné
    private void drawLevelBubble(Canvas canvas, GameModel model) {
        int lv = model.getSelectedLevel();
        RectF n = hud.levelNodes[lv];
        float bw = 130 * density;
        float bh = 46 * density;
        float crownSpace = (lv == GameModel.MAX_LEVELS) ? 30 * density : 0;
        float bottom = n.centerY() - 40 * density - crownSpace;
        // Bulle centrée sur le niveau, sans sortir de l'écran
        float left = Math.max(board.playLeft + 8 * density,
                Math.min(n.centerX() - bw / 2, board.playRight() - bw - 8 * density));
        tmpRect.set(left, bottom - bh, left + bw, bottom);

        paint.setColor(Color.WHITE);
        canvas.drawRoundRect(tmpRect, 12 * density, 12 * density, paint);
        // Pointe vers le niveau
        path.reset();
        path.moveTo(n.centerX() - 8 * density, bottom);
        path.lineTo(n.centerX() + 8 * density, bottom);
        path.lineTo(n.centerX(), bottom + 8 * density);
        path.close();
        canvas.drawPath(path, paint);

        paint.setColor(Palette.POPUP);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(16 * density);
        canvas.drawText("Niveau " + lv, tmpRect.centerX(), tmpRect.centerY() - 2 * density, paint);
        paint.setTextSize(12 * density);
        canvas.drawText(GameModel.wavesForLevel(lv) + " vagues",
                tmpRect.centerX(), tmpRect.centerY() + 14 * density, paint);
    }

    // Cadenas : un corps arrondi et un arceau
    private void drawLock(Canvas canvas, float cx, float cy, float s) {
        paint.setColor(Palette.ICON_DISABLED);
        tmpRect.set(cx - s, cy - s * 0.1f, cx + s, cy + s);
        canvas.drawRoundRect(tmpRect, 3 * density, 3 * density, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(s * 0.3f);
        tmpRect.set(cx - s * 0.6f, cy - s, cx + s * 0.6f, cy + s * 0.2f);
        canvas.drawArc(tmpRect, 180, 180, false, paint); // demi-cercle du haut
        paint.setStyle(Paint.Style.FILL);
    }

    // Couronne : une bande avec trois pointes
    private void drawCrown(Canvas canvas, float cx, float cy, float s) {
        paint.setColor(Palette.GOLD);
        path.reset();
        path.moveTo(cx - s, cy + s * 0.6f);
        path.lineTo(cx - s, cy - s * 0.4f);
        path.lineTo(cx - s * 0.5f, cy + s * 0.1f);
        path.lineTo(cx, cy - s * 0.7f);
        path.lineTo(cx + s * 0.5f, cy + s * 0.1f);
        path.lineTo(cx + s, cy - s * 0.4f);
        path.lineTo(cx + s, cy + s * 0.6f);
        path.close();
        canvas.drawPath(path, paint);
    }

    // Écran Game Over : même fenêtre que la victoire, partie figée derrière
    private void drawGameOverScreen(Canvas canvas, GameModel model, TouchController touch) {
        drawGame(canvas, model, touch, 0);

        paint.setColor(Color.argb(160, 0, 0, 0));
        canvas.drawRect(0, 0, board.width, board.height, paint);

        RectF p = hud.victoryPopup;
        paint.setColor(Palette.POPUP);
        canvas.drawRoundRect(p, 20 * density, 20 * density, paint);

        paint.setColor(Palette.GAME_OVER);
        drawCenteredText(canvas, "GAME OVER", p.top + 52 * density, 34);

        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, "Niveau " + model.getLevel(), p.top + 86 * density, 18);
        drawCenteredText(canvas, "Vague " + model.getWave() + "/" + model.getWavesInLevel(),
                p.top + 112 * density, 18);
        paint.setColor(Palette.TEXT_INACTIVE);
        drawCenteredText(canvas, "Le château est tombé...", p.top + 146 * density, 16);

        int pressed = touch.getPressedMenuButton();
        drawPillButton(canvas, hud.victoryButtons[Hud.GAMEOVER_RETRY], "REJOUER",
                Palette.RESUME, pressed == Hud.GAMEOVER_RETRY);
        drawPillButton(canvas, hud.victoryButtons[Hud.GAMEOVER_MENU], "MENU",
                Palette.MENU_BUTTON, pressed == Hud.GAMEOVER_MENU);
    }

    private void drawGame(Canvas canvas, GameModel model, TouchController touch, float shakeProgress) {
        Mage dragged = touch.getDraggedMage();

        terrain.draw(canvas, night); // forêt, prairie, rempart, cour pavée, estrade

        // Rempart qui rougit quand un gobelin l'atteint
        float wallFlash = model.getCastleHitFlash();
        if (wallFlash > 0) {
            paint.setColor(Color.argb((int) (150 * wallFlash), 230, 50, 70));
            canvas.drawRect(0, terrain.getWallTop(), board.width, board.gridTop, paint);
        }

        drawGrid(canvas, model, dragged);

        for (Enemy enemy : model.getEnemies()) {
            drawEnemy(canvas, enemy, model.isStunned());
        }
        // Attaques
        for (Projectile p : model.getProjectiles()) {
            attacks.drawProjectile(canvas, p);
        }
        for (Bolt b : model.getBolts()) {
            attacks.drawBolt(canvas, b);
        }

        // Éclaboussures de l'Eau et impacts
        for (Splash s : model.getSplashes()) {
            attacks.drawSplash(canvas, s);
        }
        for (HitEffect hit : model.getHits()) {
            attacks.drawHit(canvas, hit);
        }

        drawRecruitButtons(canvas, model, touch.getPressedButton());
        drawTopInfo(canvas, model);
        drawUltimateGauge(canvas, model);
        drawShakeFeedback(canvas, model, shakeProgress);
        // Flash rouge quand le château est touché (s'estompe tout seul)
        float flash = model.getCastleHitFlash();
        if (flash > 0) {
            paint.setColor(Color.argb((int) (90 * flash), 255, 0, 0));
            canvas.drawRect(0, 0, board.width, board.height, paint);
        }
        drawModeBanner(canvas, model);
        drawPauseButton(canvas, touch.isPausePressed());
        drawSoundButton(canvas, model, touch.isMutePressed(), true);

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

                // (les cases elles-mêmes font partie du décor : dalles de la cour)
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

    // Dessine un mage (sprite) dans une case dont le coin haut-gauche est (left, top)
    private void drawMage(Canvas canvas, Mage mage, float left, float top) {
        float cx = left + board.cellWidth / 2;
        float cy = top + board.cellHeight / 2;

        // Échelle entière : chaque pixel du sprite fait un nombre entier de pixels de l'écran
        int scale = board.mageScale();
        float w = Sprites.MAGE_WIDTH * scale;
        float h = Sprites.MAGE_HEIGHT * scale;
        int c = Palette.ELEMENT_COLORS[mage.type];

        if (mage.level >= 5) {
            // Grand mage : aura qui pulse, de jour comme de nuit
            float pulse = 0.5f + 0.5f * (float) Math.sin(frame * 0.1);
            paint.setColor(Color.argb((int) (50 + 50 * pulse), Color.red(c), Color.green(c), Color.blue(c)));
            canvas.drawCircle(cx, cy, Math.max(w, h) * (0.55f + 0.05f * pulse), paint);
        } else if (night) {
            // Nuit : halo de la couleur de l'élément
            paint.setColor(Color.argb(70, Color.red(c), Color.green(c), Color.blue(c)));
            canvas.drawCircle(cx, cy, Math.max(w, h) * 0.55f, paint);
        }

        tmpRect.set(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);
        canvas.drawBitmap(mageSprites[mage.type][mage.level - 1], null, tmpRect, spritePaint);

        // Badge du niveau en bas à gauche : on voit d'un coup d'œil quoi fusionner
        float bx = tmpRect.left + 9 * density;
        float by = tmpRect.bottom - 9 * density;
        paint.setColor(Color.argb(220, 26, 22, 38));
        canvas.drawCircle(bx, by, 9 * density, paint);
        paint.setColor(Color.WHITE);
        paint.setTextSize(12 * density);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(String.valueOf(mage.level), bx, by + 4 * density, paint);
    }

    // Gobelin : sprite animé ; la nuit, il tient une torche avec son halo de lumière
    private void drawEnemy(Canvas canvas, Enemy enemy, boolean stunned) {
        // Échelle entière : le gobelin est un peu plus grand que sa zone de collision
        int scale = Math.max(1, (int) (enemy.radius * 2.4f / Sprites.GOBLIN_HEIGHT));
        float left = enemy.x - Sprites.GOBLIN_CENTER_X * scale;
        float top = enemy.y - Sprites.GOBLIN_HEIGHT * scale / 2f;
        float w = Sprites.GOBLIN_WIDTH * scale;
        float h = Sprites.GOBLIN_HEIGHT * scale;

        // Marche : 2 images en alternance, décalées d'un gobelin à l'autre ; immobile s'il est étourdi
        int step = stunned ? 0 : ((frame / 10) + (int) enemy.x) % 2;

        float flameX = left + Sprites.TORCH_FLAME_X * scale;
        float flameY = top + Sprites.TORCH_FLAME_Y * scale;
        if (night) {
            drawTorchGlow(canvas, flameX, flameY, enemy); // halo derrière le gobelin
        }

        tmpRect.set(left, top, left + w, top + h);
        Bitmap sprite = (night ? goblinNight : goblinDay)[step];
        canvas.drawBitmap(sprite, null, tmpRect, stunned ? stunnedPaint : spritePaint);

        if (night) {
            drawTorchFlicker(canvas, left, top, scale, enemy); // flamme qui vacille
        }

        // Barre de vie au-dessus de la tête
        float barWidth = enemy.radius * 2;
        float barHeight = enemy.radius * 0.25f;
        float barLeft = enemy.x - enemy.radius;
        float barTop = top - barHeight * 1.5f;
        paint.setColor(Color.DKGRAY);
        canvas.drawRect(barLeft, barTop, barLeft + barWidth, barTop + barHeight, paint);
        paint.setColor(Color.GREEN);
        canvas.drawRect(barLeft, barTop, barLeft + barWidth * enemy.hp / enemy.maxHp,
                barTop + barHeight, paint);
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
        float shift = board.topShift; // évite la caméra / l'encoche
        float cx = board.centerX();

        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, "NIVEAU " + model.getLevel(), 40 * density + shift, 14);
        drawCenteredText(canvas, "VAGUE " + model.getWave() + "/" + model.getWavesInLevel(),
                64 * density + shift, 24);

        // PO sous la vague (le coin haut-gauche est pris par la jauge)
        paint.setColor(Palette.GOLD);
        drawCenteredText(canvas, model.getGold() + " PO", 92 * density + shift, 20);

        // Étoiles encore possibles
        int stars = model.getStars();
        float spacing = 28 * density;
        for (int i = 0; i < 3; i++) {
            drawStar(canvas, cx + (i - 1) * spacing, 114 * density + shift,
                    11 * density, i < stars);
        }

        // Barre de PV du château, avec un repère à chaque seuil d'étoile
        float barW = 140 * density;
        float barH = 8 * density;
        float left = cx - barW / 2;
        float top = 132 * density + shift;

        paint.setColor(Palette.HP_BAR_BG);
        canvas.drawRect(left, top, left + barW, top + barH, paint);
        paint.setColor(Palette.CASTLE);
        canvas.drawRect(left, top, left + barW * model.getCastleHpRatio(), top + barH, paint);

        paint.setColor(Color.WHITE);
        for (float threshold : new float[]{GameModel.STARS_3_MIN, GameModel.STARS_2_MIN}) {
            float x = left + barW * threshold;
            canvas.drawRect(x - density, top - 3 * density, x + density, top + barH + 3 * density, paint);
        }
    }

    // Texte centré horizontalement, taille en dp
    private void drawCenteredText(Canvas canvas, String text, float y, float sizeDp) {
        paint.setTextSize(sizeDp * density);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, board.centerX(), y, paint); // centre de la colonne de jeu
    }

    // Jauge circulaire d'ultime, en haut à gauche
    private void drawUltimateGauge(Canvas canvas, GameModel model) {
        float radius = 28 * density;
        float cx = board.playLeft + 16 * density + radius;
        float cy = 40 * density + board.topShift + radius;
        float stroke = 7 * density;
        boolean ready = model.isUltimateReady();

        // Pleine : la jauge pulse légèrement pour attirer l'attention
        float pulse = ready ? (float) Math.sin(frame * 0.15) * 2 * density : 0;
        gaugeOval.set(cx - radius - pulse, cy - radius - pulse,
                cx + radius + pulse, cy + radius + pulse);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);

        // Fond de l'anneau
        paint.setColor(Palette.GAUGE_EMPTY);
        canvas.drawArc(gaugeOval, 0, 360, false, paint);

        // Remplissage : part du haut (-90°) dans le sens des aiguilles d'une montre
        paint.setColor(ready ? Palette.GOLD : Palette.GAUGE_CHARGING);
        canvas.drawArc(gaugeOval, -90, 360 * model.getUltimateCharge(), false, paint);

        paint.setStyle(Paint.Style.FILL); // on remet le mode normal

        // Texte au centre (et en dessous quand c'est prêt)
        paint.setTextAlign(Paint.Align.CENTER);
        if (model.isStunned()) {
            paint.setColor(Palette.STUNNED);
            paint.setTextSize(14 * density);
            canvas.drawText("STUN", cx, cy + 5 * density, paint);
        } else if (ready) {
            paint.setColor(Palette.GOLD);
            paint.setTextSize(14 * density);
            canvas.drawText("ULTI", cx, cy + 5 * density, paint);
            paint.setTextSize(13 * density);
            canvas.drawText("Secouez !", cx, cy + radius + 22 * density, paint);
        } else {
            paint.setColor(Color.WHITE);
            paint.setTextSize(13 * density);
            canvas.drawText(model.getUltimateKills() + "/" + GameModel.ULTIMATE_KILLS_REQUIRED,
                    cx, cy + 5 * density, paint);
        }
    }

    // Feedback de secousse : grand anneau au centre de la zone de jeu
    private void drawShakeFeedback(Canvas canvas, GameModel model, float shakeProgress) {
        if (!model.isUltimateReady() || shakeProgress <= 0) {
            return;
        }

        // Voile sombre pour faire ressortir l'anneau
        paint.setColor(Color.argb(140, 0, 0, 0));
        canvas.drawRect(0, 0, board.width, board.height, paint);

        float cx = board.centerX();
        float cy = board.gridTop / 2f;          // milieu de la zone des ennemis
        float radius = board.playWidth * 0.25f; // grand : un quart de la colonne de jeu
        gaugeOval.set(cx - radius, cy - radius, cx + radius, cy + radius);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(16 * density);
        paint.setColor(Palette.GAUGE_EMPTY);
        canvas.drawArc(gaugeOval, 0, 360, false, paint);
        paint.setColor(Palette.GOLD);
        canvas.drawArc(gaugeOval, -90, 360 * shakeProgress, false, paint);
        paint.setStyle(Paint.Style.FILL);

        // Textes en gros
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, (int) (shakeProgress * 100) + "%", cy + 12 * density, 34);
        drawCenteredText(canvas, "Continuez !", cy + radius + 44 * density, 26);
    }

    // Halo de la torche : deux cercles transparents qui respirent (le jeu de lumière d'avant)
    private void drawTorchGlow(Canvas canvas, float fx, float fy, Enemy enemy) {
        float r = enemy.radius;
        float flicker = (float) Math.sin(frame * 0.4 + enemy.x) * 0.15f * r;
        paint.setColor(Palette.TORCH_GLOW);
        canvas.drawCircle(fx, fy, r * 2.2f + flicker, paint);
        canvas.drawCircle(fx, fy, r * 1.3f + flicker, paint);
    }

    // Flamme qui vacille : un pixel de flamme qui apparaît, change de place et disparaît
    private void drawTorchFlicker(Canvas canvas, float left, float top, int scale, Enemy enemy) {
        int phase = ((frame / 6) + (int) enemy.x) % 3;
        if (phase == 2) {
            return; // flamme basse
        }
        float px = left + (phase == 0 ? 17 : 16) * scale;
        paint.setColor(phase == 0 ? Palette.TORCH_CORE : Palette.TORCH_FLAME);
        canvas.drawRect(px, top - scale, px + scale, top, paint); // un pixel au-dessus de la torche
    }

    // Bouton pause rond avec deux barres verticales
    private void drawPauseButton(Canvas canvas, boolean pressed) {
        RectF b = hud.pauseButton;

        // Fond : plus clair quand le doigt est dessus (feedback d'appui)
        paint.setColor(pressed ? Color.argb(170, 255, 255, 255) : Color.argb(70, 255, 255, 255));
        canvas.drawCircle(b.centerX(), b.centerY(), b.width() / 2, paint);

        // Les deux barres
        paint.setColor(Color.WHITE);
        float barWidth = b.width() / 8;
        float top = b.top + b.height() * 0.3f;
        float bottom = b.bottom - b.height() * 0.3f;
        float cx = b.centerX();
        canvas.drawRect(cx - barWidth * 1.5f, top, cx - barWidth * 0.5f, bottom, paint);
        canvas.drawRect(cx + barWidth * 0.5f, top, cx + barWidth * 1.5f, bottom, paint);
    }

    // Écran pause : partie figée derrière un voile, fenêtre de la maquette au centre
    private void drawPauseScreen(Canvas canvas, GameModel model, TouchController touch) {
        drawGame(canvas, model, touch, 0);

        paint.setColor(Color.argb(160, 0, 0, 0));
        canvas.drawRect(0, 0, board.width, board.height, paint);

        // Fenêtre
        RectF p = hud.pausePopup;
        paint.setColor(Palette.POPUP);
        canvas.drawRoundRect(p, 20 * density, 20 * density, paint);
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, "PAUSE", p.top + 48 * density, 32);

        int pressed = touch.getPressedMenuButton();

        // QUITTER
        RectF q = hud.pauseMenuButtons[Hud.PAUSE_QUIT];
        paint.setColor(Palette.QUIT);
        canvas.drawRoundRect(q, q.height() / 2, q.height() / 2, paint);
        if (pressed == Hud.PAUSE_QUIT) {
            drawPressedOverlay(canvas, q, q.height() / 2);
        }
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, "QUITTER", q.centerY() + 7 * density, 20);

        // Trois boutons ronds
        for (int i = Hud.PAUSE_RESTART; i <= Hud.PAUSE_OPTIONS; i++) {
            RectF b = hud.pauseMenuButtons[i];
            boolean enabled = true;

            int bg;
            if (!enabled) {
                bg = Palette.DISABLED;
            } else if (i == Hud.PAUSE_RESUME) {
                bg = Palette.RESUME; // action principale mise en avant
            } else {
                bg = Palette.MENU_BUTTON;
            }
            paint.setColor(bg);
            canvas.drawCircle(b.centerX(), b.centerY(), b.width() / 2, paint);
            if (enabled && i == pressed) {
                drawPressedOverlay(canvas, b, b.width() / 2);
            }

            // Icône
            float s = b.width() * 0.5f;
            paint.setColor(enabled ? Color.WHITE : Palette.ICON_DISABLED);
            if (i == Hud.PAUSE_RESTART) {
                drawRestartIcon(canvas, b.centerX(), b.centerY(), s);
            } else if (i == Hud.PAUSE_RESUME) {
                drawPlayIcon(canvas, b.centerX(), b.centerY(), s);
            } else {
                drawGearIcon(canvas, b.centerX(), b.centerY(), s, bg);
            }
        }
    }

    // Feedback d'appui : voile blanc transparent par-dessus le bouton
    private void drawPressedOverlay(Canvas canvas, RectF b, float cornerRadius) {
        paint.setColor(Color.argb(90, 255, 255, 255));
        canvas.drawRoundRect(b, cornerRadius, cornerRadius, paint);
    }

    // triangle pointant vers la droite
    private void drawPlayIcon(Canvas canvas, float cx, float cy, float s) {
        path.reset();
        path.moveTo(cx - s * 0.3f, cy - s * 0.45f);
        path.lineTo(cx - s * 0.3f, cy + s * 0.45f);
        path.lineTo(cx + s * 0.5f, cy);
        path.close();
        canvas.drawPath(path, paint);
    }

    // arc de cercle presque complet avec une pointe de flèche
    private void drawRestartIcon(Canvas canvas, float cx, float cy, float s) {
        float r = s * 0.42f;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(s * 0.16f);
        gaugeOval.set(cx - r, cy - r, cx + r, cy + r);
        canvas.drawArc(gaugeOval, -60, 300, false, paint); // ouverture en haut
        paint.setStyle(Paint.Style.FILL);

        // Pointe de flèche au bout de l'arc (angle 240°)
        double a = Math.toRadians(240);
        float ex = cx + (float) Math.cos(a) * r;    // bout de l'arc
        float ey = cy + (float) Math.sin(a) * r;
        float tx = (float) -Math.sin(a);            // direction de rotation
        float ty = (float) Math.cos(a);
        float nx = (float) Math.cos(a);             // direction vers l'extérieur
        float ny = (float) Math.sin(a);

        path.reset();
        path.moveTo(ex + tx * s * 0.25f, ey + ty * s * 0.25f); // pointe
        path.lineTo(ex + nx * s * 0.2f, ey + ny * s * 0.2f);
        path.lineTo(ex - nx * s * 0.2f, ey - ny * s * 0.2f);
        path.close();
        canvas.drawPath(path, paint);
    }

    // disque entouré de 8 dents, avec un trou au centre
    private void drawGearIcon(Canvas canvas, float cx, float cy, float s, int holeColor) {
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            canvas.drawCircle(cx + (float) Math.cos(a) * s * 0.38f,
                    cy + (float) Math.sin(a) * s * 0.38f, s * 0.12f, paint);
        }
        canvas.drawCircle(cx, cy, s * 0.32f, paint);
        paint.setColor(holeColor);
        canvas.drawCircle(cx, cy, s * 0.13f, paint);
    }

    // Écran options : partie figée derrière, fenêtre de la maquette au centre
    private void drawOptionsScreen(Canvas canvas, GameModel model, TouchController touch) {
        // Derrière la fenêtre : l'écran d'où l'on vient (carte ou partie en pause)
        if (model.getOptionsReturnState() == GameState.START) {
            drawStartScreen(canvas, model, -1); // -1 : aucun bouton de la carte n'est appuyé
        } else {
            drawGame(canvas, model, touch, 0);
        }

        paint.setColor(Color.argb(160, 0, 0, 0));
        canvas.drawRect(0, 0, board.width, board.height, paint);

        // Fenêtre
        RectF p = hud.optionsPopup;
        paint.setColor(Palette.POPUP);
        canvas.drawRoundRect(p, 20 * density, 20 * density, paint);
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, "OPTIONS", p.top + 48 * density, 32);
        drawCenteredText(canvas, "Désactiver le mode nuit", p.top + 95 * density, 18);

        int pressed = touch.getPressedMenuButton();

        // Interrupteur Non / Oui
        RectF row = hud.optionsButtons[Hud.OPTIONS_NIGHT_TOGGLE];
        if (pressed == Hud.OPTIONS_NIGHT_TOGGLE) {
            drawPressedOverlay(canvas, row, 12 * density);
        }
        drawToggle(canvas, row, model.isNightModeDisabled());

        // Musique de fond Non / Oui
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, "Musique", p.top + 190 * density, 18);
        RectF musicRow = hud.optionsButtons[Hud.OPTIONS_MUSIC_TOGGLE];
        if (pressed == Hud.OPTIONS_MUSIC_TOGGLE) {
            drawPressedOverlay(canvas, musicRow, 12 * density);
        }
        drawToggle(canvas, musicRow, model.isMusicEnabled());

        // Volume
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, "Volume", p.top + 280 * density, 18);
        // Pendant le glissement, on affiche la position du doigt ; sinon, le volume réel
        float shownVolume = touch.isDraggingVolume() ? touch.getDragVolume() : model.getSoundVolume();
        drawVolumeSlider(canvas, shownVolume, touch.isDraggingVolume());

        // RETOUR
        RectF back = hud.optionsButtons[Hud.OPTIONS_BACK];
        paint.setColor(Palette.MENU_BUTTON);
        canvas.drawRoundRect(back, back.height() / 2, back.height() / 2, paint);
        if (pressed == Hud.OPTIONS_BACK) {
            drawPressedOverlay(canvas, back, back.height() / 2);
        }
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, "RETOUR", back.centerY() + 7 * density, 20);
    }

    // Interrupteur centré dans la ligne, avec "Non" à gauche et "Oui" à droite
    private void drawToggle(Canvas canvas, RectF row, boolean on) {
        float trackW = 64 * density;
        float trackH = 32 * density;
        float cx = row.centerX();
        float cy = row.centerY();
        RectF track = new RectF(cx - trackW / 2, cy - trackH / 2, cx + trackW / 2, cy + trackH / 2);

        // Rail : vert si activé, gris sinon
        paint.setColor(on ? Palette.TOGGLE_ON : Palette.TOGGLE_OFF);
        canvas.drawRoundRect(track, trackH / 2, trackH / 2, paint);

        // Bouton rond : à droite si activé, à gauche sinon
        float knobR = trackH / 2 - 3 * density;
        float knobX = on ? track.right - trackH / 2 : track.left + trackH / 2;
        paint.setColor(Color.WHITE);
        canvas.drawCircle(knobX, cy, knobR, paint);

        // Libellés : celui qui est actif est en blanc
        paint.setTextSize(18 * density);
        paint.setColor(on ? Palette.TEXT_INACTIVE : Color.WHITE);
        paint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("Non", track.left - 12 * density, cy + 6 * density, paint);
        paint.setColor(on ? Color.WHITE : Palette.TEXT_INACTIVE);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText("Oui", track.right + 12 * density, cy + 6 * density, paint);
    }

    // Étoile à 5 branches : on alterne 10 points, loin puis près du centre
    private void drawStar(Canvas canvas, float cx, float cy, float r, boolean full) {
        paint.setColor(full ? Palette.GOLD : Palette.STAR_EMPTY);
        path.reset();
        for (int i = 0; i < 10; i++) {
            float radius = (i % 2 == 0) ? r : r * 0.45f; // pointe ou creux
            double a = Math.toRadians(-90 + i * 36);    // -90° : première pointe en haut
            float x = cx + (float) Math.cos(a) * radius;
            float y = cy + (float) Math.sin(a) * radius;
            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }
        path.close();
        canvas.drawPath(path, paint);
    }

    // Écran victoire : partie figée derrière, fenêtre avec les étoiles gagnées
    private void drawVictoryScreen(Canvas canvas, GameModel model, TouchController touch) {
        drawGame(canvas, model, touch, 0);

        paint.setColor(Color.argb(160, 0, 0, 0));
        canvas.drawRect(0, 0, board.width, board.height, paint);

        RectF p = hud.victoryPopup;
        paint.setColor(Palette.POPUP);
        canvas.drawRoundRect(p, 20 * density, 20 * density, paint);

        paint.setColor(Palette.GOLD);
        drawCenteredText(canvas, "VICTOIRE", p.top + 52 * density, 34);
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, "Niveau " + model.getLevel() + " terminé", p.top + 80 * density, 16);

        // Étoiles : elles apparaissent une par une, avec un petit effet "pop"
        int stars = model.getStars();
        for (int i = 0; i < 3; i++) {
            boolean middle = (i == 1);
            float r = (middle ? 30 : 24) * density;           // celle du milieu, plus grande
            float sx = board.centerX() + (i - 1) * 72 * density;
            float sy = p.top + (middle ? 126 : 136) * density; // et un peu plus haute

            int appearFrame = 15 + i * 15;                     // ≈ 0,25 s entre deux étoiles
            boolean full = i < stars && stateFrames >= appearFrame;
            if (full) {
                int age = stateFrames - appearFrame;
                if (age < 8) {
                    r *= 1 + 0.3f * (8 - age) / 8f;            // grossit puis revient à sa taille
                }
            }
            drawStar(canvas, sx, sy, r, full);
        }

        int pressed = touch.getPressedMenuButton();

        // Après le dernier niveau, il n'y a plus de niveau suivant
        boolean hasNext = model.hasNextLevel();
        drawPillButton(canvas, hud.victoryButtons[Hud.VICTORY_NEXT],
                hasNext ? "NIVEAU SUIVANT" : "DERNIER NIVEAU !",
                hasNext ? Palette.RESUME : Palette.DISABLED,
                hasNext && pressed == Hud.VICTORY_NEXT);
        drawPillButton(canvas, hud.victoryButtons[Hud.VICTORY_MENU], "MENU",
                Palette.MENU_BUTTON, pressed == Hud.VICTORY_MENU);
    }

    // Bouton arrondi avec un texte centré
    private void drawPillButton(Canvas canvas, RectF b, String text, int color, boolean pressed) {
        paint.setColor(color);
        canvas.drawRoundRect(b, b.height() / 2, b.height() / 2, paint);
        if (pressed) {
            drawPressedOverlay(canvas, b, b.height() / 2);
        }
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, text, b.centerY() + 7 * density, 20);
    }

    // Curseur de volume : haut-parleurs aux deux bouts, rail rempli jusqu'au bouton
    private void drawVolumeSlider(Canvas canvas, float volume, boolean dragging) {
        RectF t = hud.volumeTrack;
        float cy = t.centerY();
        float knobX = t.left + (t.right - t.left) * volume;

        // Haut-parleurs : petit à gauche, avec des ondes à droite
        drawSpeaker(canvas, t.left - 26 * density, cy, 10 * density, 0);
        drawSpeaker(canvas, t.right + 22 * density, cy, 10 * density, 2);

        // Rail : vide en gris, rempli en vert jusqu'au bouton
        paint.setStrokeCap(Paint.Cap.ROUND);
        paint.setStrokeWidth(6 * density);
        paint.setColor(Palette.TOGGLE_OFF);
        canvas.drawLine(t.left, cy, t.right, cy, paint);
        paint.setColor(Palette.TOGGLE_ON);
        canvas.drawLine(t.left, cy, knobX, cy, paint);
        paint.setStrokeCap(Paint.Cap.BUTT); // on remet le réglage par défaut

        // Bouton : plus gros pendant le glissement, avec le pourcentage au-dessus du doigt
        paint.setColor(Color.WHITE);
        canvas.drawCircle(knobX, cy, (dragging ? 15 : 12) * density, paint);
        if (dragging) {
            paint.setTextSize(14 * density);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText((int) (volume * 100) + " %", knobX, cy - 24 * density, paint);
        }
    }

    // Haut-parleur : un cône, et des ondes en arc de cercle
    private void drawSpeaker(Canvas canvas, float cx, float cy, float s, int waves) {
        paint.setColor(Color.WHITE);
        path.reset();
        path.moveTo(cx - s, cy - s * 0.35f);
        path.lineTo(cx - s * 0.4f, cy - s * 0.35f);
        path.lineTo(cx + s * 0.2f, cy - s * 0.8f);
        path.lineTo(cx + s * 0.2f, cy + s * 0.8f);
        path.lineTo(cx - s * 0.4f, cy + s * 0.35f);
        path.lineTo(cx - s, cy + s * 0.35f);
        path.close();
        canvas.drawPath(path, paint);

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(s * 0.15f);
        for (int i = 1; i <= waves; i++) {
            float r = s * (0.3f + 0.35f * i);
            tmpRect.set(cx + s * 0.2f - r, cy - r, cx + s * 0.2f + r, cy + r);
            canvas.drawArc(tmpRect, -45, 90, false, paint); // arc tourné vers la droite
        }
        paint.setStyle(Paint.Style.FILL);
    }

    // Bandeau quand la nuit tombe ou que le jour se lève
    private void drawModeBanner(Canvas canvas, GameModel model) {
        float t = model.getModeBanner(); // 1 → 0
        if (t <= 0) {
            return;
        }
        int alpha = (int) (255 * Math.min(1f, t * 3)); // opaque, puis s'estompe sur le dernier tiers
        boolean isNight = model.isNightMode();

        float cy = 185 * density + board.topShift;
        tmpRect.set(board.playLeft + 24 * density, cy - 30 * density,
                board.playRight() - 24 * density, cy + 26 * density);
        paint.setColor(Color.argb(alpha * 200 / 255, 0, 0, 0));
        canvas.drawRoundRect(tmpRect, 14 * density, 14 * density, paint);

        paint.setColor(isNight ? Color.argb(alpha, 170, 190, 255) : Color.argb(alpha, 255, 215, 0));
        drawCenteredText(canvas, isNight ? "LA NUIT TOMBE" : "LE JOUR SE LÈVE", cy - 4 * density, 22);
        paint.setColor(Color.argb(alpha, 255, 255, 255));
        drawCenteredText(canvas, isNight ? "Ennemis plus rapides, +20 % d'or" : "Retour à la normale",
                cy + 16 * density, 14);
    }

    // Bouton son : haut-parleur avec des ondes, ou barré si coupé
    private void drawSoundButton(Canvas canvas, GameModel model, boolean pressed, boolean overGame) {
        RectF s = hud.homeSoundButton;
        if (overGame) {
            // En jeu : même style transparent que le bouton pause
            paint.setColor(pressed ? Color.argb(170, 255, 255, 255) : Color.argb(70, 255, 255, 255));
            canvas.drawCircle(s.centerX(), s.centerY(), s.width() / 2, paint);
        } else {
            drawRoundIconButton(canvas, s, pressed);
        }

        boolean muted = model.getSoundVolume() <= 0;
        drawSpeaker(canvas, s.centerX() - 3 * density, s.centerY(), 11 * density, muted ? 0 : 2);
        if (muted) {
            paint.setColor(Palette.QUIT);
            paint.setStrokeWidth(4 * density);
            paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawLine(s.left + s.width() * 0.25f, s.top + s.height() * 0.25f,
                    s.right - s.width() * 0.25f, s.bottom - s.height() * 0.25f, paint);
            paint.setStrokeCap(Paint.Cap.BUTT);
        }
    }
}
