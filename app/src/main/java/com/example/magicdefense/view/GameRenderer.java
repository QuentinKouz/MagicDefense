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
import com.example.magicdefense.model.LevelSpec;
import com.example.magicdefense.model.Progression;
import com.example.magicdefense.model.Mage;
import com.example.magicdefense.model.Projectile;
import com.example.magicdefense.model.Reaction;
import com.example.magicdefense.model.Splash;
import com.example.magicdefense.model.Zone;

import java.util.Random;

// Tout le dessin : lit le modèle et l'affiche, sans jamais le modifier
public class GameRenderer {
    private final Paint paint = new Paint();
    private final Board board;
    private final Hud hud;
    private final float density;
    private final RectF gaugeOval = new RectF();
    private final Path path = new Path(); // pour dessiner les icônes (triangle, flèche)
    private final RectF tmpRect = new RectF(); // rectangle réutilisable pour le dessin
    private final RectF spriteRect = new RectF(); // sprite de l'écran MAGES
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
    private final Paint fastPaint = new Paint();                       // gobelin rapide : teinte orangée
    private final Paint tankPaint = new Paint();                       // gobelin costaud : rouge sombre
    private final Paint shamanPaint = new Paint();                     // chaman : violet
    private final Paint bossPaint = new Paint();                       // chef gobelin : vert sombre
    private final Paint impPaint = new Paint();                        // diablotin : rouge vif
    private final Paint lockedPaint = new Paint();                     // unité pas encore achetée : assombrie
    private final Random decorRandom = new Random();                   // décor de l'accueil (graine fixe par biome)

    private final Bitmap[] chestSprites = Sprites.buildChest();        // { couvercle, caisse }

    private final AttackRenderer attacks;
    private final UiKit ui;                                            // boutons, fenêtres, rubans...
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
        fastPaint.setFilterBitmap(false);
        fastPaint.setColorFilter(new LightingColorFilter(0xFFDD99, 0x302000));
        tankPaint.setFilterBitmap(false);
        tankPaint.setColorFilter(new LightingColorFilter(0xDD7777, 0x200000));
        shamanPaint.setFilterBitmap(false);
        shamanPaint.setColorFilter(new LightingColorFilter(0xC090FF, 0x200030));
        bossPaint.setFilterBitmap(false);
        bossPaint.setColorFilter(new LightingColorFilter(0x90B070, 0x101000));
        impPaint.setFilterBitmap(false);
        impPaint.setColorFilter(new LightingColorFilter(0xFF6060, 0x400000));
        lockedPaint.setFilterBitmap(false);
        lockedPaint.setColorFilter(new LightingColorFilter(0x505060, 0x000000));
        attacks = new AttackRenderer(density);
        ui = new UiKit(density);
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
            case UPGRADES:
                drawUpgradesScreen(canvas, model, touch.getPressedMenuButton());
                break;
            case CHEST_OPENING:
                drawChestOpening(canvas, model, touch.getPressedMenuButton());
                break;
            case MARKET:
                drawMarketScreen(canvas, model, touch.getPressedMenuButton());
                break;
        }
    }

    // ---------- Écrans ----------
    // Accueil : carte des niveaux reliés par un chemin, bulle, JOUER, options et son
    private void drawStartScreen(Canvas canvas, GameModel model, int pressed) {
        int biome = model.getMapBiome();
        drawBiomeBackground(canvas, biome); // le décor change avec la page de la carte

        // Titre en lettres dorées à contour (un peu plus petit sur un écran étroit, pour laisser
        // la place aux boutons du coin)
        float titleSize = Math.min(28, board.playWidth / density / 15);
        ui.text(canvas, "MAGIC DEFENSE", board.centerX(), 70 * density + board.topShift, titleSize,
                Palette.GOLD, Paint.Align.CENTER);

        // Biome et page de la carte (les niveaux sont sans fin, 6 par page)
        int first = model.levelAtSlot(1);
        ui.text(canvas, LevelSpec.BIOME_NAMES[biome] + " · Niveaux " + first + " – "
                        + (first + GameModel.LEVELS_PER_PAGE - 1), board.centerX(), 96 * density + board.topShift,
                13, Palette.BIOME_COLORS[biome][5], Paint.Align.CENTER);

        // Chemin entre les niveaux (couleur du biome)
        paint.setColor(Palette.BIOME_COLORS[biome][3]);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(6 * density);
        for (int slot = 1; slot < GameModel.LEVELS_PER_PAGE; slot++) {
            RectF a = hud.levelNodes[slot];
            RectF b = hud.levelNodes[slot + 1];
            canvas.drawLine(a.centerX(), a.centerY(), b.centerX(), b.centerY(), paint);
        }
        paint.setStyle(Paint.Style.FILL);

        for (int slot = 1; slot <= GameModel.LEVELS_PER_PAGE; slot++) {
            drawLevelNode(canvas, model, slot, pressed == slot);
        }

        // Flèches de page (cachées s'il n'y a pas de page de ce côté)
        if (model.canChangeMapPage(-1)) {
            drawPageArrow(canvas, hud.prevPageButton, false, pressed == Hud.HOME_PREV_PAGE);
        }
        if (model.canChangeMapPage(1)) {
            drawPageArrow(canvas, hud.nextPageButton, true, pressed == Hud.HOME_NEXT_PAGE);
        }
        // Écran assez haut : bulle au-dessus du niveau choisi (maquette).
        // Écran court (petit téléphone, tablette en paysage) : la bulle cacherait le niveau
        // du dessus, alors le niveau choisi est écrit sur le bouton JOUER.
        boolean roomForBubble = hud.levelNodes[1].centerY() - hud.levelNodes[2].centerY() >= 90 * density;
        String playText = "JOUER";
        int selectedSlot = model.getSelectedLevel() - first + 1;
        boolean selectedOnPage = selectedSlot >= 1 && selectedSlot <= GameModel.LEVELS_PER_PAGE;
        if (roomForBubble && selectedOnPage) {
            drawLevelBubble(canvas, model);
        } else {
            playText = "JOUER · NIVEAU " + model.getSelectedLevel();
        }

        drawPillButton(canvas, hud.playButton, playText, Palette.RESUME, pressed == Hud.HOME_PLAY);

        // Mode INFINI (avec le record) et écran MAGES (avec les coffres à ouvrir)
        int best = model.getProgression().getBestEndlessWave();
        drawTwoLineButton(canvas, hud.endlessButton, "INFINI",
                best > 0 ? "Record : vague " + best : "Vagues sans fin",
                Palette.ENDLESS, pressed == Hud.HOME_ENDLESS);
        drawTwoLineButton(canvas, hud.upgradesButton, "MAGES", "Coffres, équipe",
                Palette.MENU_BUTTON, pressed == Hud.HOME_UPGRADES);
        drawTwoLineButton(canvas, hud.marketButton, "MARCHÉ",
                model.getProgression().getGems() + " gemmes", Palette.MARKET, pressed == Hud.HOME_MARKET);
        int chests = model.getProgression().getChests();
        if (chests > 0) {
            // Pastille dorée : des coffres attendent d'être ouverts
            RectF u = hud.upgradesButton;
            float br = 12 * density;
            paint.setColor(Palette.GOLD);
            canvas.drawCircle(u.right - 4 * density, u.top + 2 * density, br, paint);
            paint.setColor(Palette.POPUP);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize((chests > 99 ? 9 : 12) * density);
            canvas.drawText(chests > 999 ? "999+" : String.valueOf(chests),
                    u.right - 4 * density, u.top + 6 * density, paint);
        }

        // Options
        RectF o = hud.homeOptionsButton;
        boolean optPressed = pressed == Hud.HOME_OPTIONS;
        drawRoundIconButton(canvas, o, optPressed);
        paint.setColor(Color.WHITE);
        drawGearIcon(canvas, o.centerX(), o.centerY() + sink(optPressed), o.width() * 0.5f, Palette.MENU_BUTTON);

        drawSoundButton(canvas, model, pressed == Hud.HOME_SOUND, false);
    }

    // Bouton rond : médaillon à anneau doré (s'enfonce quand on appuie)
    private void drawRoundIconButton(Canvas canvas, RectF b, boolean pressed) {
        ui.medallion(canvas, b.centerX(), b.centerY(), b.width() / 2, Palette.MENU_BUTTON, pressed, 255);
    }

    // Décalage vertical d'un médaillon appuyé (pour que l'icône s'enfonce avec lui)
    private float sink(boolean pressed) {
        return pressed ? 2 * density : 0;
    }

    // Flèche de page : bouton rond avec un triangle vers la gauche ou la droite
    private void drawPageArrow(Canvas canvas, RectF b, boolean right, boolean pressed) {
        drawRoundIconButton(canvas, b, pressed);
        float cx = b.centerX();
        float cy = b.centerY() + sink(pressed);
        float s = b.width() * 0.2f;
        float dir = right ? 1 : -1;
        paint.setColor(Color.WHITE);
        path.reset();
        path.moveTo(cx + dir * s, cy);
        path.lineTo(cx - dir * s * 0.6f, cy - s);
        path.lineTo(cx - dir * s * 0.6f, cy + s);
        path.close();
        canvas.drawPath(path, paint);
    }

    // Un niveau : cercle numéroté (ou cadenas), meilleures étoiles en dessous
    // slot = emplacement dans la page (1 à 6)
    private void drawLevelNode(Canvas canvas, GameModel model, int slot, boolean pressed) {
        RectF n = hud.levelNodes[slot];
        int lv = model.levelAtSlot(slot);
        float cx = n.centerX();
        float cy = n.centerY();
        float r = 28 * density;
        boolean unlocked = lv <= model.getUnlockedLevel();

        // Halo doré qui pulse autour du niveau sélectionné
        if (lv == model.getSelectedLevel()) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(frame * 0.1);
            paint.setColor(Color.argb((int) (70 + 70 * pulse), 255, 215, 90));
            canvas.drawCircle(cx, cy, r + (6 + 3 * pulse) * density, paint);
        }

        // Médaillon (s'enfonce quand on appuie)
        boolean down = pressed && unlocked;
        ui.medallion(canvas, cx, cy, r, unlocked ? Palette.MENU_BUTTON : Palette.DISABLED, down, 255);
        cy += sink(down);

        if (unlocked) {
            float size = lv >= 1000 ? 13 : lv >= 100 ? 17 : 21; // les grands numéros tiennent
            ui.text(canvas, String.valueOf(lv), cx, cy + size * 0.36f * density, size, Color.WHITE, Paint.Align.CENTER);
        } else {
            drawLock(canvas, cx, cy, r * 0.45f);
        }

        // Couronne sur le dernier niveau de chaque page
        if (slot == GameModel.LEVELS_PER_PAGE) {
            drawCrown(canvas, cx, cy - r - 14 * density, 14 * density);
        }

        // Niveau à boss (10, 20, 30...) : étiquette rouge à droite du cercle
        if (lv % LevelSpec.BOSS_EVERY == 0) {
            paint.setTextSize(10 * density);
            float tw = paint.measureText("BOSS") + 10 * density;
            tmpRect.set(cx + r * 0.55f, cy - r - 4 * density, cx + r * 0.55f + tw, cy - r + 12 * density);
            paint.setColor(Palette.BOSS_BAR);
            canvas.drawRoundRect(tmpRect, 6 * density, 6 * density, paint);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("BOSS", tmpRect.centerX(), tmpRect.bottom - 4 * density, paint);
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
        int slot = lv - model.levelAtSlot(1) + 1;
        if (slot < 1 || slot > GameModel.LEVELS_PER_PAGE) {
            return; // le niveau choisi est sur une autre page
        }
        RectF n = hud.levelNodes[slot];
        LevelSpec spec = LevelSpec.forLevel(lv);
        float bw = (spec.isGenerated() ? 150 : 130) * density;
        float bh = 46 * density;
        float crownSpace = (slot == GameModel.LEVELS_PER_PAGE) ? 30 * density : 0;
        float bottom = n.centerY() - 40 * density - crownSpace;
        // Bulle centrée sur le niveau, sans sortir de l'écran
        float left = Math.max(board.playLeft + 8 * density,
                Math.min(n.centerX() - bw / 2, board.playRight() - bw - 8 * density));
        tmpRect.set(left, bottom - bh, left + bw, bottom);

        // Parchemin : contour sombre, papier, pointe vers le niveau
        path.reset();
        path.moveTo(n.centerX() - 9 * density, bottom - 2 * density);
        path.lineTo(n.centerX() + 9 * density, bottom - 2 * density);
        path.lineTo(n.centerX(), bottom + 10 * density);
        path.close();
        paint.setColor(UiKit.OUTLINE);
        canvas.drawPath(path, paint);
        canvas.drawRoundRect(tmpRect, 10 * density, 10 * density, paint);
        tmpRect.inset(2 * density, 2 * density);
        paint.setColor(Palette.PARCHMENT);
        canvas.drawRoundRect(tmpRect, 8 * density, 8 * density, paint);
        path.reset();
        path.moveTo(n.centerX() - 6.5f * density, bottom - 3 * density);
        path.lineTo(n.centerX() + 6.5f * density, bottom - 3 * density);
        path.lineTo(n.centerX(), bottom + 6.5f * density);
        path.close();
        canvas.drawPath(path, paint);
        paint.setColor(Palette.PARCHMENT_DARK); // bord bas plus foncé
        canvas.drawRect(tmpRect.left + 6 * density, tmpRect.bottom - 3 * density,
                tmpRect.right - 6 * density, tmpRect.bottom - 1.5f * density, paint);
        tmpRect.inset(-2 * density, -2 * density);

        paint.setColor(Palette.POPUP);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(16 * density);
        canvas.drawText("Niveau " + lv, tmpRect.centerX(), tmpRect.centerY() - 2 * density, paint);
        paint.setTextSize(12 * density);
        String detail = spec.waves + " vagues";
        if (spec.hasBossLevel()) {
            detail += " · BOSS";
        } else if (spec.isGenerated()) {
            detail += " · " + spec.getProfileName(); // Horde, Siège ou Mêlée
        }
        canvas.drawText(detail, tmpRect.centerX(), tmpRect.centerY() + 14 * density, paint);
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
        ui.panel(canvas, p, "GAME OVER", Palette.QUIT);

        if (model.isEndless()) {
            // Mode infini : vague atteinte, record et coffres gagnés
            paint.setColor(Color.WHITE);
            drawCenteredText(canvas, "Vague " + model.getWave() + " atteinte", p.top + 88 * density, 20);
            if (model.isLastEndlessRecord()) {
                paint.setColor(Palette.GOLD);
                drawCenteredText(canvas, "Nouveau record !", p.top + 116 * density, 15);
            } else {
                paint.setColor(Palette.TEXT_INACTIVE);
                drawCenteredText(canvas, "Record : vague " + model.getProgression().getBestEndlessWave(),
                        p.top + 116 * density, 15);
            }
            drawChestsEarned(canvas, model.getLastChestsEarned(), p.top + 168 * density);
        } else {
            paint.setColor(Color.WHITE);
            drawCenteredText(canvas, "Niveau " + model.getLevel(), p.top + 86 * density, 18);
            drawCenteredText(canvas, "Vague " + model.getWave() + "/" + model.getWavesInLevel(),
                    p.top + 112 * density, 18);
            paint.setColor(Palette.TEXT_INACTIVE);
            drawCenteredText(canvas, "Le château est tombé...", p.top + 146 * density, 16);
        }

        int pressed = touch.getPressedMenuButton();
        drawPillButton(canvas, hud.victoryButtons[Hud.GAMEOVER_RETRY], "REJOUER",
                Palette.RESUME, pressed == Hud.GAMEOVER_RETRY);
        drawPillButton(canvas, hud.victoryButtons[Hud.GAMEOVER_MENU], "MENU",
                Palette.MENU_BUTTON, pressed == Hud.GAMEOVER_MENU);
    }

    private void drawGame(Canvas canvas, GameModel model, TouchController touch, float shakeProgress) {
        Mage dragged = touch.getDraggedMage();

        terrain.draw(canvas, night, model.getBiome()); // forêt, prairie, rempart, cour... teintés selon le biome

        // Rempart qui rougit quand un gobelin l'atteint
        float wallFlash = model.getCastleHitFlash();
        if (wallFlash > 0) {
            paint.setColor(Color.argb((int) (150 * wallFlash), 230, 50, 70));
            canvas.drawRect(0, terrain.getWallTop(), board.width, board.gridTop, paint);
        }

        drawGrid(canvas, model, dragged);

        // Zones au sol (lave, vapeur) sous les ennemis
        for (Zone z : model.getZones()) {
            attacks.drawZone(canvas, z);
        }

        for (Enemy enemy : model.getEnemies()) {
            drawEnemy(canvas, enemy, model.isStunned() || enemy.stunTimer > 0);
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
        // Réactions élémentaires : anneau et nom de la réaction
        for (Reaction r : model.getReactions()) {
            attacks.drawReaction(canvas, r);
        }

        drawRecruitButtons(canvas, model, touch.getPressedButton());
        drawTopInfo(canvas, model);
        drawBossBar(canvas, model);
        drawAnnounce(canvas, model);
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
        // Chaman : halo violet qui pulse (il invoque des diablotins)
        if (enemy.kind == Enemy.SHAMAN || enemy.isBoss()) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(frame * 0.12 + enemy.x);
            paint.setColor(enemy.isBoss() ? Color.argb((int) (40 + 40 * pulse), 255, 60, 60) : Palette.SHAMAN_GLOW);
            canvas.drawCircle(enemy.x, enemy.y, enemy.radius * (1.1f + 0.15f * pulse), paint);
        }

        Paint p = spritePaint;
        if (stunned) {
            p = stunnedPaint;
        } else {
            switch (enemy.kind) {
                case Enemy.FAST:   p = fastPaint; break;   // rapide : plus petit et orangé
                case Enemy.TANK:   p = tankPaint; break;   // costaud : plus gros et rouge sombre
                case Enemy.SHAMAN: p = shamanPaint; break; // chaman : violet
                case Enemy.BOSS:   p = bossPaint; break;   // chef : vert sombre, énorme
                case Enemy.IMP:    p = impPaint; break;    // diablotin : minuscule et rouge
            }
        }
        canvas.drawBitmap(sprite, null, tmpRect, p);

        if (night) {
            drawTorchFlicker(canvas, left, top, scale, enemy); // flamme qui vacille
        }

        // Barre de vie au-dessus de la tête
        float barWidth = enemy.radius * 2;
        float barHeight = enemy.radius * 0.25f;
        float barLeft = enemy.x - enemy.radius;
        float barTop = top - barHeight * 1.5f;
        // Vulnérable (Acide, Malédiction) : liseré orange autour de la barre
        if (enemy.isVulnerable()) {
            float pad = 1.5f * density;
            paint.setColor(Palette.VULNERABLE);
            canvas.drawRect(barLeft - pad, barTop - pad, barLeft + barWidth + pad, barTop + barHeight + pad, paint);
        }
        paint.setColor(Color.DKGRAY);
        canvas.drawRect(barLeft, barTop, barLeft + barWidth, barTop + barHeight, paint);
        // Vert acide quand il est empoisonné
        paint.setColor(enemy.isPoisoned() ? Palette.POISONED_BAR : Color.GREEN);
        float ratio = Math.max(0f, enemy.hp / enemy.maxHp);
        canvas.drawRect(barLeft, barTop, barLeft + barWidth * ratio, barTop + barHeight, paint);

        // Marque d'élément (une réaction est possible) : point de la couleur de l'élément
        float dotR = Math.max(3 * density, barHeight * 0.9f);
        float dotX = barLeft - dotR * 1.6f;
        float dotY = barTop + barHeight / 2;
        if (enemy.mark != Enemy.NO_MARK) {
            paint.setColor(Color.argb(200, 20, 15, 30));
            canvas.drawCircle(dotX, dotY, dotR + density, paint);
            paint.setColor(Palette.ELEMENT_COLORS[enemy.mark]);
            canvas.drawCircle(dotX, dotY, dotR, paint);
        }
        // Empoisonné : petite bulle verte qui monte
        if (enemy.isPoisoned()) {
            float t = ((frame + (int) enemy.x) % 30) / 30f;
            paint.setColor(Color.argb((int) (200 * (1 - t)), 150, 230, 70));
            canvas.drawCircle(barLeft + barWidth + dotR * 1.4f, dotY - t * 12 * density, dotR * 0.8f, paint);
        }

        // Chef gobelin : couronne au-dessus de la barre
        if (enemy.isBoss()) {
            drawCrown(canvas, enemy.x, barTop - 12 * density, 12 * density);
        }
    }

    // Grande barre de vie du boss, sous les infos du haut
    private void drawBossBar(Canvas canvas, GameModel model) {
        Enemy boss = model.getBoss();
        if (boss == null) {
            return;
        }
        float cx = board.centerX();
        float w = board.playWidth * 0.7f;
        float top = 164 * density + board.topShift;
        float h = 10 * density;
        paint.setColor(Palette.BOSS_BAR);
        drawCenteredText(canvas, "CHEF GOBELIN", top - 5 * density, 12);
        tmpRect.set(cx - w / 2, top, cx + w / 2, top + h);
        paint.setColor(Palette.HP_BAR_BG);
        canvas.drawRoundRect(tmpRect, h / 2, h / 2, paint);
        tmpRect.right = tmpRect.left + w * Math.max(0f, boss.hp / boss.maxHp);
        paint.setColor(Palette.BOSS_BAR);
        canvas.drawRoundRect(tmpRect, h / 2, h / 2, paint);
    }

    // Bandeau d'annonce : arrivée du boss (rouge) ou nouveau biome du mode infini
    private void drawAnnounce(Canvas canvas, GameModel model) {
        float t = model.getAnnounce(); // 1 → 0
        if (t <= 0) {
            return;
        }
        int alpha = (int) (255 * Math.min(1f, t * 3));
        float cy = 250 * density + board.topShift;
        float grow = t > 0.9f ? (1 - t) * 10 : 1; // s'ouvre en grandissant
        tmpRect.set(board.playLeft + 16 * density, cy - 32 * density * grow,
                board.playRight() - 16 * density, cy + 26 * density * grow);
        paint.setColor(Color.argb(alpha * 210 / 255, 0, 0, 0));
        canvas.drawRoundRect(tmpRect, 14 * density, 14 * density, paint);
        if (model.isAnnounceBoss()) {
            paint.setColor(Color.argb(alpha, 255, 80, 80));
        } else {
            paint.setColor(Color.argb(alpha, 255, 215, 120));
        }
        drawCenteredText(canvas, model.getAnnounceTitle(), cy - 4 * density, 22);
        paint.setColor(Color.argb(alpha, 255, 255, 255));
        drawCenteredText(canvas, model.getAnnounceSubtitle(), cy + 16 * density, 13);
    }

    private void drawRecruitButtons(Canvas canvas, GameModel model, int pressedButton) {
        boolean canAfford = model.canAffordMage();

        for (int i = 0; i < hud.recruitButtons.length; i++) {
            RectF b = hud.recruitButtons[i];
            int type = model.teamTypeAt(i); // mage de cet emplacement d'équipe
            if (type == Progression.NO_UNIT) {
                // Emplacement vide (équipe de moins de 4 mages) : alcôve creusée
                ui.inset(canvas, b);
                ui.text(canvas, "Vide", b.centerX(), b.centerY() + 5 * density, 13, Palette.TEXT_INACTIVE,
                        Paint.Align.CENTER);
                continue;
            }

            // Tuile de la couleur de l'élément : grisée si pas assez d'or, enfoncée si appuyée
            RectF f = ui.button(canvas, b, Palette.ELEMENT_COLORS[type], canAfford && i == pressedButton, canAfford);
            int textColor = canAfford ? Color.WHITE : Color.rgb(170, 168, 182);
            ui.textPx(canvas, Palette.ELEMENT_NAMES[type], f.centerX(), f.centerY() - 1 * density, 14 * density,
                    textColor, Paint.Align.CENTER, f.width() - 8 * density); // « Invocateur » tient dans la tuile

            // Prix : pièce d'or + 10
            String cost = String.valueOf(GameModel.MAGE_COST);
            float coinR = 6 * density;
            float priceY = f.centerY() + 15 * density;
            ui.coin(canvas, f.centerX() - 8 * density, priceY - 4.5f * density, coinR);
            ui.text(canvas, cost, f.centerX() + 1 * density, priceY, 13, canAfford ? Palette.GOLD : textColor,
                    Paint.Align.LEFT);
        }
    }

    private void drawTopInfo(Canvas canvas, GameModel model) {
        float shift = board.topShift; // évite la caméra / l'encoche
        float cx = board.centerX();

        boolean endless = model.isEndless();
        paint.setColor(endless ? Palette.ENDLESS_TEXT : Color.WHITE);
        drawCenteredText(canvas, endless ? "MODE INFINI" : "NIVEAU " + model.getLevel(), 40 * density + shift, 14);
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, endless ? "VAGUE " + model.getWave()
                        : "VAGUE " + model.getWave() + "/" + model.getWavesInLevel(),
                64 * density + shift, 24);

        // PO sous la vague (le coin haut-gauche est pris par la jauge)
        paint.setColor(Palette.GOLD);
        drawCenteredText(canvas, model.getGold() + " PO", 92 * density + shift, 20);

        if (endless) {
            // Pas d'étoiles en mode infini : on rappelle le record à battre
            paint.setColor(Palette.TEXT_INACTIVE);
            drawCenteredText(canvas, "Record : vague " + model.getProgression().getBestEndlessWave(),
                    118 * density + shift, 13);
        } else {
            // Étoiles encore possibles
            int stars = model.getStars();
            float spacing = 28 * density;
            for (int i = 0; i < 3; i++) {
                drawStar(canvas, cx + (i - 1) * spacing, 114 * density + shift,
                        11 * density, i < stars);
            }
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

        if (!endless) {
            paint.setColor(Color.WHITE);
            for (float threshold : new float[]{GameModel.STARS_3_MIN, GameModel.STARS_2_MIN}) {
                float x = left + barW * threshold;
                canvas.drawRect(x - density, top - 3 * density, x + density, top + barH + 3 * density, paint);
            }
        }
    }

    // Texte centré horizontalement, taille en dp
    // (texte gras à contour sombre, dans la couleur actuelle de paint ; réduit s'il dépasse la colonne)
    private void drawCenteredText(Canvas canvas, String text, float y, float sizeDp) {
        ui.textPx(canvas, text, board.centerX(), y, sizeDp * density, paint.getColor(), Paint.Align.CENTER,
                board.playWidth - 24 * density); // centre de la colonne de jeu
    }

    // Jauge circulaire d'ultime, en haut à gauche
    private void drawUltimateGauge(Canvas canvas, GameModel model) {
        float radius = hud.gaugeRadius;
        float cx = hud.gaugeCenterX; // à gauche, ou à droite en mode gaucher
        float cy = hud.gaugeCenterY;
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

        // Médaillon un peu transparent : on voit le terrain derrière
        ui.medallion(canvas, b.centerX(), b.centerY(), b.width() / 2, Palette.MENU_BUTTON, pressed, 215);

        // Les deux barres
        paint.setColor(Color.WHITE);
        float barWidth = b.width() / 9;
        float top = b.top + b.height() * 0.33f + sink(pressed);
        float bottom = b.bottom - b.height() * 0.33f + sink(pressed);
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
        ui.panel(canvas, p, "PAUSE", UiKit.RIBBON);

        int pressed = touch.getPressedMenuButton();

        // QUITTER
        drawPillButton(canvas, hud.pauseMenuButtons[Hud.PAUSE_QUIT], "QUITTER", Palette.QUIT,
                pressed == Hud.PAUSE_QUIT);

        // Trois médaillons : recommencer, reprendre (mis en avant), options
        for (int i = Hud.PAUSE_RESTART; i <= Hud.PAUSE_OPTIONS; i++) {
            RectF b = hud.pauseMenuButtons[i];
            int bg = i == Hud.PAUSE_RESUME ? Palette.RESUME : Palette.MENU_BUTTON;
            boolean isPressed = i == pressed;
            ui.medallion(canvas, b.centerX(), b.centerY(), b.width() / 2, bg, isPressed, 255);

            // Icône (s'enfonce avec le médaillon)
            float s = b.width() * 0.42f;
            float cy = b.centerY() + sink(isPressed);
            paint.setColor(Color.WHITE);
            if (i == Hud.PAUSE_RESTART) {
                drawRestartIcon(canvas, b.centerX(), cy, s);
            } else if (i == Hud.PAUSE_RESUME) {
                drawPlayIcon(canvas, b.centerX() + 2 * density, cy, s);
            } else {
                drawGearIcon(canvas, b.centerX(), cy, s, bg);
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
        ui.panel(canvas, p, "OPTIONS", UiKit.RIBBON);
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, "Désactiver le mode nuit", p.top + Hud.OPT_NIGHT_Y * density, 18);

        int pressed = touch.getPressedMenuButton();

        // Interrupteur Non / Oui
        RectF row = hud.optionsButtons[Hud.OPTIONS_NIGHT_TOGGLE];
        if (pressed == Hud.OPTIONS_NIGHT_TOGGLE) {
            drawPressedOverlay(canvas, row, 12 * density);
        }
        drawToggle(canvas, row, model.isNightModeDisabled());

        // Musique de fond Non / Oui
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, "Musique", p.top + Hud.OPT_MUSIC_Y * density, 18);
        RectF musicRow = hud.optionsButtons[Hud.OPTIONS_MUSIC_TOGGLE];
        if (pressed == Hud.OPTIONS_MUSIC_TOGGLE) {
            drawPressedOverlay(canvas, musicRow, 12 * density);
        }
        drawToggle(canvas, musicRow, model.isMusicEnabled());

        // Mode gaucher : interface en miroir
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, "Mode gaucher", p.top + Hud.OPT_LEFT_Y * density, 18);
        RectF leftRow = hud.optionsButtons[Hud.OPTIONS_LEFT_HANDED];
        if (pressed == Hud.OPTIONS_LEFT_HANDED) {
            drawPressedOverlay(canvas, leftRow, 12 * density);
        }
        drawToggle(canvas, leftRow, model.isLeftHanded());

        // Volume
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, "Volume", p.top + Hud.OPT_VOLUME_Y * density, 18);
        // Pendant le glissement, on affiche la position du doigt ; sinon, le volume réel
        float shownVolume = touch.isDraggingVolume() ? touch.getDragVolume() : model.getSoundVolume();
        drawVolumeSlider(canvas, shownVolume, touch.isDraggingVolume());

        // RETOUR
        drawPillButton(canvas, hud.optionsButtons[Hud.OPTIONS_BACK], "RETOUR", Palette.MENU_BUTTON,
                pressed == Hud.OPTIONS_BACK);
    }

    // Interrupteur centré dans la ligne, avec "Non" à gauche et "Oui" à droite
    private void drawToggle(Canvas canvas, RectF row, boolean on) {
        float trackW = 64 * density;
        float trackH = 32 * density;
        float cx = row.centerX();
        float cy = row.centerY();
        tmpRect.set(cx - trackW / 2, cy - trackH / 2, cx + trackW / 2, cy + trackH / 2);
        float left = tmpRect.left;
        float right = tmpRect.right;

        // Rail creusé : vert si activé, gris sinon
        ui.bar(canvas, tmpRect, on ? 1f : 0f, Palette.TOGGLE_ON);

        // Bouton : pièce d'or, à droite si activé, à gauche sinon
        float knobR = trackH / 2 - 1 * density;
        float knobX = on ? right - trackH / 2 : left + trackH / 2;
        ui.coin(canvas, knobX, cy, knobR);

        // Libellés : celui qui est actif est en blanc
        ui.text(canvas, "Non", left - 12 * density, cy + 6 * density, 17,
                on ? Palette.TEXT_INACTIVE : Color.WHITE, Paint.Align.RIGHT);
        ui.text(canvas, "Oui", right + 12 * density, cy + 6 * density, 17,
                on ? Color.WHITE : Palette.TEXT_INACTIVE, Paint.Align.LEFT);
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
        ui.panel(canvas, p, "VICTOIRE", Palette.RESUME);
        ui.text(canvas, "Niveau " + model.getLevel() + " terminé", board.centerX(), p.top + 70 * density, 16,
                Palette.GOLD, Paint.Align.CENTER);
        // Coffres gagnés, sous les étoiles (juste au-dessus des boutons)
        if (stateFrames >= 60) { // après l'apparition des étoiles
            drawChestsEarned(canvas, model.getLastChestsEarned(), p.top + 186 * density);
        }

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

        // Les niveaux sont infinis : il y a toujours un niveau suivant
        drawPillButton(canvas, hud.victoryButtons[Hud.VICTORY_NEXT], "NIVEAU SUIVANT",
                Palette.RESUME, pressed == Hud.VICTORY_NEXT);
        drawPillButton(canvas, hud.victoryButtons[Hud.VICTORY_MENU], "MENU",
                Palette.MENU_BUTTON, pressed == Hud.VICTORY_MENU);
    }

    // Grand bouton : tuile en relief avec un texte centré
    private void drawPillButton(Canvas canvas, RectF b, String text, int color, boolean pressed) {
        boolean enabled = color != Palette.DISABLED;
        ui.labelButton(canvas, b, text, color, pressed && enabled, enabled, 19);
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
        ui.coin(canvas, knobX, cy, (dragging ? 15 : 12) * density); // bouton : pièce d'or
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
        // En jeu : médaillon un peu transparent, comme le bouton pause
        ui.medallion(canvas, s.centerX(), s.centerY(), s.width() / 2, Palette.MENU_BUTTON, pressed,
                overGame ? 215 : 255);
        float dy = sink(pressed);

        boolean muted = model.getSoundVolume() <= 0;
        drawSpeaker(canvas, s.centerX() - 3 * density, s.centerY() + dy, 10 * density, muted ? 0 : 2);
        if (muted) {
            paint.setColor(Palette.QUIT);
            paint.setStrokeWidth(4 * density);
            paint.setStrokeCap(Paint.Cap.ROUND);
            canvas.drawLine(s.left + s.width() * 0.3f, s.top + s.height() * 0.3f + dy,
                    s.right - s.width() * 0.3f, s.bottom - s.height() * 0.3f + dy, paint);
            paint.setStrokeCap(Paint.Cap.BUTT);
        }
    }

    // ---------- V2 : coffres, mode infini, écran MAGES ----------

    // Réduit la taille du texte (déjà réglée) pour qu'il tienne dans maxWidth
    private void fitText(String text, float maxWidth) {
        float w = paint.measureText(text);
        if (w > maxWidth && w > 0) {
            paint.setTextSize(paint.getTextSize() * maxWidth / w);
        }
    }

    // Petit bouton arrondi, texte centré sur le bouton lui-même
    private void drawSmallButton(Canvas canvas, RectF b, String text, int color, boolean pressed, float sizeDp) {
        boolean enabled = color != Palette.DISABLED;
        ui.labelButton(canvas, b, text, color, pressed && enabled, enabled, sizeDp);
    }

    // Tuile avec un titre et un sous-titre (INFINI, MAGES, MARCHÉ)
    private void drawTwoLineButton(Canvas canvas, RectF b, String title, String subtitle, int color, boolean pressed) {
        RectF f = ui.button(canvas, b, color, pressed, true);
        ui.textPx(canvas, title, f.centerX(), f.centerY() + 1 * density, 15 * density, Color.WHITE,
                Paint.Align.CENTER, f.width() - 10 * density);
        ui.textPx(canvas, subtitle, f.centerX(), f.centerY() + 15 * density, 10 * density,
                Color.argb(230, 255, 240, 200), Paint.Align.CENTER, f.width() - 12 * density);
    }

    // "+N coffres" avec un petit coffre devant, centré dans la colonne
    private void drawChestsEarned(Canvas canvas, int chests, float y) {
        if (chests <= 0) {
            paint.setColor(Palette.TEXT_INACTIVE);
            drawCenteredText(canvas, "Aucun coffre (1 toutes les "
                    + Progression.WAVES_PER_ENDLESS_CHEST + " vagues)", y, 13);
            return;
        }
        String text = "+" + chests + (chests > 1 ? " coffres" : " coffre");
        paint.setTextSize(17 * density);
        float textW = paint.measureText(text);
        float icon = 10 * density;
        float total = icon * 2 + 8 * density + textW;
        float x = board.centerX() - total / 2;
        drawChest(canvas, x + icon, y - 6 * density, icon, 0);
        paint.setColor(Palette.GOLD);
        paint.setTextSize(17 * density);
        paint.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(text, x + icon * 2 + 8 * density, y, paint);
    }

    // Coffre : caisse en bois, couvercle (qui se soulève de `open` entre 0 et 1), bande et serrure dorées
    // (cx, cy) : centre du haut de la caisse ; s : demi-largeur ; open : 0 fermé → 1 ouvert
    private void drawChest(Canvas canvas, float cx, float cy, float s, float open) {
        float px = s * 2 / Sprites.CHEST_WIDTH;          // taille d'un pixel du sprite
        float bodyTop = cy - s * 0.2f;
        float bodyH = Sprites.CHEST_BODY_HEIGHT * px;
        float lidH = Sprites.CHEST_LID_HEIGHT * px;

        // Lumière qui sort de la caisse ouverte
        if (open > 0) {
            paint.setColor(Color.argb((int) (150 * open), 255, 225, 110));
            canvas.drawCircle(cx, bodyTop, s * 1.1f * open, paint);
            paint.setColor(Color.argb((int) (230 * open), 255, 245, 190));
            tmpRect.set(cx - s + 2 * px, bodyTop - px, cx + s - 2 * px, bodyTop + px);
            canvas.drawRect(tmpRect, paint);
        }
        // Caisse
        tmpRect.set(cx - s, bodyTop, cx + s, bodyTop + bodyH);
        canvas.drawBitmap(chestSprites[1], null, tmpRect, spritePaint);
        // Couvercle : se soulève et bascule vers l'arrière (charnière à gauche)
        canvas.save();
        if (open > 0) {
            canvas.translate(0, -open * s * 0.5f);
            canvas.rotate(-28 * open, cx - s, bodyTop);
        }
        tmpRect.set(cx - s, bodyTop - lidH + px, cx + s, bodyTop + px);
        canvas.drawBitmap(chestSprites[0], null, tmpRect, spritePaint);
        canvas.restore();
    }

    // Écran MAGES : ouvrir les coffres et améliorer chaque élément de façon permanente
    private void drawUpgradesScreen(Canvas canvas, GameModel model, int pressed) {
        Progression prog = model.getProgression();

        drawStartScreen(canvas, model, -1); // la carte reste visible derrière
        paint.setColor(Color.argb(160, 0, 0, 0));
        canvas.drawRect(0, 0, board.width, board.height, paint);

        RectF p = hud.upgradesPopup;
        ui.panel(canvas, p, "MAGES", UiKit.RIBBON);

        // Pages (4 mages par page) : flèches de part et d'autre de RETOUR
        int pages = model.getUpgradePageCount();
        if (pages > 1) {
            RectF back = hud.upgradeButtons[Hud.UPGRADE_BACK];
            ui.text(canvas, "Page " + (model.getUpgradePage() + 1) + " / " + pages, board.centerX(),
                    back.top - 6 * density, 11, Palette.TEXT_INACTIVE, Paint.Align.CENTER);
            if (model.canChangeUpgradePage(-1)) {
                drawPageArrow(canvas, hud.upgradeButtons[Hud.UPGRADE_PREV], false, pressed == Hud.UPGRADE_PREV);
            }
            if (model.canChangeUpgradePage(1)) {
                drawPageArrow(canvas, hud.upgradeButtons[Hud.UPGRADE_NEXT], true, pressed == Hud.UPGRADE_NEXT);
            }
        }

        // --- Coffres et gemmes ---
        RectF c = hud.chestRow;
        ui.inset(canvas, c);

        int chests = prog.getChests();
        // Des coffres à ouvrir : le coffre se dandine pour attirer l'œil
        float wiggle = chests > 0 && (frame % 120) < 24 ? (float) Math.sin(frame * 0.8) * 6 : 0;
        canvas.save();
        canvas.rotate(wiggle, c.left + 36 * density, c.centerY() + 10 * density);
        drawChest(canvas, c.left + 36 * density, c.centerY() + 4 * density, 20 * density, 0);
        canvas.restore();

        float tx = c.left + 70 * density;
        float textRight = hud.upgradeButtons[Hud.UPGRADE_OPEN_CHEST].left - 8 * density;
        ui.textPx(canvas, "Coffres : " + chests, tx, c.centerY() - 2 * density, 17 * density, Color.WHITE,
                Paint.Align.LEFT, textRight - tx);
        // Gemmes (pour le Marché)
        float gy = c.centerY() + 16 * density;
        drawGem(canvas, tx + 6 * density, gy - 4 * density, 6 * density);
        ui.textPx(canvas, prog.getGems() + " gemmes", tx + 16 * density, gy, 13 * density, Palette.GEM,
                Paint.Align.LEFT, textRight - tx - 16 * density);

        boolean canOpen = chests > 0;
        drawSmallButton(canvas, hud.upgradeButtons[Hud.UPGRADE_OPEN_CHEST], "OUVRIR",
                canOpen ? Palette.RESUME : Palette.DISABLED, canOpen && pressed == Hud.UPGRADE_OPEN_CHEST, 15);

        // --- Consigne ---
        paint.setColor(Palette.TEXT_INACTIVE);
        drawCenteredText(canvas, "Touchez un portrait pour composer l'équipe ("
                + prog.teamCount() + "/" + Progression.TEAM_SIZE + ")", c.bottom + 24 * density, 12);
        if (prog.ownedCount() < Mage.TYPE_COUNT) {
            drawCenteredText(canvas, "Coffres : fin de niveau et mode infini", c.bottom + 40 * density, 11);
        }

        // --- Une ligne par mage possédé (4 par page) ---
        for (int row = 0; row < Hud.UPGRADE_ROWS; row++) {
            int type = model.upgradeTypeAtRow(row);
            if (type != -1) {
                drawUpgradeRow(canvas, prog, row, type, pressed == row, pressed == Hud.UPGRADE_TEAM_0 + row);
            }
        }

        drawPillButton(canvas, hud.upgradeButtons[Hud.UPGRADE_BACK], "RETOUR",
                Palette.MENU_BUTTON, pressed == Hud.UPGRADE_BACK);
    }

    // Une ligne de l'écran MAGES : portrait (équipe), rang, bonus, barre de fragments, bouton +1 RANG
    private void drawUpgradeRow(Canvas canvas, Progression prog, int row, int type,
                                boolean pressed, boolean portraitPressed) {
        RectF r = hud.upgradeRows[row];
        RectF btn = hud.upgradeButtons[row];
        int rank = prog.getRank(type);

        ui.inset(canvas, r); // alcôve creusée

        // Portrait : cadre vert si le mage fait partie de l'équipe
        RectF portrait = hud.upgradeButtons[Hud.UPGRADE_TEAM_0 + row];
        boolean inTeam = prog.isInTeam(type);
        // Portrait : petit vitrail de la couleur de l'élément, cadre vert si dans l'équipe
        tmpRect.set(portrait);
        tmpRect.inset(4 * density, 4 * density);
        paint.setColor(inTeam ? Palette.TEAM : UiKit.GOLD_DARK);
        canvas.drawRoundRect(tmpRect, 9 * density, 9 * density, paint);
        tmpRect.inset(2.5f * density, 2.5f * density);
        int ec = Palette.ELEMENT_COLORS[type];
        paint.setColor(UiKit.shade(ec, 0.35f));
        canvas.drawRoundRect(tmpRect, 7 * density, 7 * density, paint);
        paint.setColor(Color.argb(70, Color.red(ec), Color.green(ec), Color.blue(ec)));
        canvas.drawCircle(tmpRect.centerX(), tmpRect.centerY() - 2 * density, tmpRect.width() * 0.42f, paint);
        if (portraitPressed) {
            drawPressedOverlay(canvas, portrait, 10 * density);
        }

        // Sprite : son apparence évolue tous les 10 rangs (comme les fusions)
        int tier = Math.min(GameModel.MAX_LEVEL, 1 + (rank - 1) / 10);
        float box = r.height() - 12 * density;
        float sw = box;
        float sh = box * Sprites.MAGE_HEIGHT / Sprites.MAGE_WIDTH;
        float sx = r.left + 6 * density;
        float sy = r.centerY() - sh / 2;
        spriteRect.set(sx, sy, sx + sw, sy + sh);
        canvas.drawBitmap(mageSprites[type][tier - 1], null, spriteRect, spritePaint);
        if (inTeam) {
            paint.setTextSize(9 * density);
            float tw = paint.measureText("ÉQUIPE") + 8 * density;
            tmpRect.set(portrait.centerX() - tw / 2, portrait.bottom - 14 * density,
                    portrait.centerX() + tw / 2, portrait.bottom - 2 * density);
            paint.setColor(Palette.TEAM);
            canvas.drawRoundRect(tmpRect, 5 * density, 5 * density, paint);
            paint.setColor(Color.WHITE);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("ÉQUIPE", tmpRect.centerX(), tmpRect.bottom - 3 * density, paint);
        }

        float tx = sx + sw + 10 * density;
        float maxW = btn.left - 10 * density - tx;

        // Ligne 1 : élément (dans sa couleur) et rang
        String name = Palette.ELEMENT_NAMES[type];
        ui.text(canvas, name, tx, r.top + 22 * density, 15, Palette.ELEMENT_COLORS[type], Paint.Align.LEFT);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(15 * density);
        float nameW = paint.measureText(name + " ");
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        ui.text(canvas, "Rang " + rank, tx + nameW, r.top + 22 * density, 15, Palette.GOLD, Paint.Align.LEFT);

        // Ligne 2 : bonus permanents
        String bonus = String.format(java.util.Locale.FRANCE, "Dégâts ×%.1f · Cadence +%d %%",
                Progression.damageMultiplier(rank), Math.round(Progression.speedBonus(rank) * 100));
        paint.setColor(Palette.TEXT_INACTIVE);
        paint.setTextSize(11 * density);
        fitText(bonus, maxW);
        canvas.drawText(bonus, tx, r.top + 38 * density, paint);

        // Ligne 3 : barre de fragments vers le rang suivant
        boolean maxed = rank >= Progression.MAX_RANK;
        int frags = prog.getFragments(type);
        int cost = prog.upgradeCost(type);
        float barTop = r.top + 45 * density;
        float barH = 14 * density;
        tmpRect.set(tx, barTop, tx + maxW, barTop + barH);
        float ratio = maxed ? 1f : Math.min(1f, frags / (float) cost);
        ui.bar(canvas, tmpRect, ratio, Palette.ELEMENT_COLORS[type]);
        ui.text(canvas, maxed ? "MAX" : frags + " / " + cost, tx + maxW / 2, barTop + barH - 3.5f * density, 10,
                Color.WHITE, Paint.Align.CENTER);

        // Bouton
        boolean can = prog.canUpgrade(type);
        drawSmallButton(canvas, btn, maxed ? "MAX" : "+1 RANG",
                can ? Palette.RESUME : Palette.DISABLED, can && pressed, 14);
    }

    // =====================================================================
    //  Animation d'ouverture de coffre
    // =====================================================================
    //  Façon jeu de cartes : le coffre tombe en bas de l'écran puis attend. Chaque toucher fait sortir
    //  UNE carte : elle jaillit du coffre (dos visible), se retourne, s'affiche en grand avec des rayons
    //  de sa couleur, et son nombre monte jusqu'au total. Après la dernière : récapitulatif du butin.
    private void drawChestOpening(Canvas canvas, GameModel model, int pressed) {
        int f = model.getChestAnimFrame();
        int cf = model.getChestCardFrame();
        int revealed = model.getChestRevealed();
        int n = model.getChestCardCount();
        int intro = GameModel.CHEST_INTRO_FRAMES;

        drawUpgradesScreen(canvas, model, -1); // l'écran MAGES reste derrière
        paint.setColor(Color.argb(228, 8, 6, 22));
        canvas.drawRect(0, 0, board.width, board.height, paint);

        float cx = board.centerX();
        float h = board.height;

        if (model.isChestRevealComplete()) {
            drawChestSummary(canvas, model, pressed, cf);
            return;
        }

        float s = 44 * density;          // demi-largeur du coffre
        float chestY = h * 0.70f;        // le coffre reste en bas
        float cardCy = h * 0.38f;        // la carte s'affiche en grand au-dessus

        // Rayons de la couleur de la carte, derrière elle
        if (revealed > 0) {
            int color = cardColor(model.getChestCardType(revealed - 1));
            float appear = Math.min(1f, cf / 12f);
            drawLightRays(canvas, cx, cardCy, board.playWidth * 0.75f * appear, frame, color);
        }

        // Coffre : chute, écrasement, attente (se dandine), petit rebond à chaque carte
        float y = chestY;
        float scaleX = 1;
        float scaleY = 1;
        float rot = 0;
        if (f < 28) {
            float t = f / 28f;
            y = chestY - (1 - t) * (1 - t) * (chestY + 2 * s);
        } else if (f < intro) {
            float k = (float) Math.sin((f - 28) / (float) (intro - 28) * Math.PI);
            scaleX = 1 + 0.2f * k;
            scaleY = 1 - 0.2f * k;
        } else if (revealed == 0) {
            int idle = (f - intro) % 70;
            if (idle < 22) {
                rot = (float) Math.sin(idle * 0.9) * 7;
            }
        } else if (cf < 10) {
            float k = (float) Math.sin(cf / 10f * Math.PI);
            scaleX = 1 - 0.12f * k;
            scaleY = 1 + 0.14f * k;
        }
        float open = revealed > 0 ? Math.min(1f, (revealed == 1 ? cf : 6) / 6f) : 0;
        // Lueur dorée autour du coffre une fois posé
        if (f >= intro) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(frame * 0.1);
            paint.setColor(Color.argb((int) (35 + 35 * pulse + 60 * open), 255, 210, 90));
            canvas.drawCircle(cx, y + s * 0.2f, s * (1.5f + 0.15f * pulse), paint);
        }
        canvas.save();
        canvas.translate(cx, y);
        canvas.rotate(rot, 0, s * 0.8f);
        canvas.scale(scaleX, scaleY, 0, s * 0.8f); // se déforme sur sa base
        drawChest(canvas, 0, 0, s, open);
        canvas.restore();

        // Pastille : cartes encore dans le coffre
        int left = n - revealed;
        if (model.isChestIntroDone() && left > 0) {
            float bx = cx + s * 0.95f;
            float by = y - s * 0.85f;
            paint.setColor(UiKit.OUTLINE);
            canvas.drawCircle(bx, by, 15 * density, paint);
            paint.setColor(Palette.QUIT);
            canvas.drawCircle(bx, by, 13 * density, paint);
            ui.text(canvas, String.valueOf(left), bx, by + 6 * density, 16, Color.WHITE, Paint.Align.CENTER);
        }

        // Carte en cours
        if (revealed > 0) {
            drawBigCard(canvas, model, revealed - 1, cf, cx, cardCy, cx, y - s * 0.3f);
        }

        // Flash quand le couvercle saute (première carte)
        if (revealed == 1 && cf < 10) {
            paint.setColor(Color.argb((int) (190 * (1 - cf / 10f)), 255, 250, 230));
            canvas.drawRect(0, 0, board.width, board.height, paint);
        }

        // Titre et consigne
        ui.ribbon(canvas, cx, board.topShift + 58 * density, Math.min(board.playWidth * 0.7f, 240 * density),
                "COFFRE MAGIQUE", UiKit.RIBBON);
        if (revealed > 0) {
            ui.text(canvas, "Carte " + revealed + " / " + n, cx, board.topShift + 96 * density, 13,
                    Palette.TEXT_INACTIVE, Paint.Align.CENTER);
        }
        if (model.isChestIntroDone()) {
            String hint = revealed == 0 ? "Touchez le coffre !"
                    : left > 0 ? "Touchez pour la carte suivante" : "Touchez pour voir le butin";
            float pulse = 0.5f + 0.5f * (float) Math.sin(frame * 0.12);
            ui.text(canvas, hint, cx, h * 0.87f, 16, Color.argb((int) (160 + 95 * pulse), 255, 240, 200),
                    Paint.Align.CENTER);
        }
    }

    // Carte i en grand : jaillit du coffre en montrant son dos, se retourne, puis le nombre monte
    private void drawBigCard(Canvas canvas, GameModel model, int i, int cf, float cx, float cy,
                             float fromX, float fromY) {
        int type = model.getChestCardType(i);
        int amount = model.getChestCardAmount(i);
        float w = Math.min(170 * density, board.playWidth * 0.5f);
        float h = w * 1.32f;

        float t = Math.min(1f, cf / 12f);
        float rise = 1 - (1 - t) * (1 - t);                       // ralentit en arrivant
        float x = fromX + (cx - fromX) * rise;
        float y = fromY + (cy - fromY) * rise;
        float scale = 0.25f + 0.75f * rise;
        if (cf >= 12) {
            y += (float) Math.sin(frame * 0.06) * 3 * density;      // flotte doucement
        }
        // Retournement : de dos (angle π) à face (angle 0)
        double angle = (1 - Math.min(1f, Math.max(0f, (cf - 4) / 10f))) * Math.PI;
        float flip = (float) Math.abs(Math.cos(angle));
        boolean back = angle > Math.PI / 2;

        // Nombre qui monte jusqu'au total, avec un rebond à la fin
        int shown = cf < 14 ? 0 : Math.min(amount, Math.round(amount * (cf - 14) / 14f));
        float pop = cf >= 28 && cf < 36 ? 1 + 0.15f * (float) Math.sin((cf - 28) / 8f * Math.PI) : 1;

        // Étincelles quand la carte se retourne
        if (cf >= 10 && cf < 34) {
            float p = (cf - 10) / 24f;
            int color = cardColor(type);
            for (int j = 0; j < 14; j++) {
                double a = j * 2 * Math.PI / 14 + 0.2;
                float dist = w * (0.4f + 0.9f * p);
                paint.setColor(Color.argb((int) (255 * (1 - p)), Color.red(color), Color.green(color), Color.blue(color)));
                canvas.drawCircle(cx + (float) Math.cos(a) * dist, cy + (float) Math.sin(a) * dist,
                        4 * density * (1 - p) + density, paint);
            }
        }

        canvas.save();
        canvas.translate(x, y);
        canvas.scale(scale * Math.max(0.02f, flip), scale);
        if (back) {
            drawCardBack(canvas, 0, 0, w, h);
        } else {
            drawCardFace(canvas, 0, 0, w, h, type, "x" + shown, pop);
        }
        canvas.restore();
    }

    // Récapitulatif : toutes les cartes du coffre, qui apparaissent l'une après l'autre
    private void drawChestSummary(Canvas canvas, GameModel model, int pressed, int cf) {
        float cx = board.centerX();
        int n = model.getChestCardCount();
        ui.ribbon(canvas, cx, board.topShift + 58 * density, Math.min(board.playWidth * 0.6f, 200 * density),
                "BUTIN", Palette.RESUME);

        float gap = 8 * density;
        float w = n > 0 ? Math.min(80 * density, (board.playWidth - 32 * density - (n - 1) * gap) / n) : 0;
        float h = w * 1.32f;
        float cy = board.height * 0.4f;
        float rowLeft = cx - (n * w + (n - 1) * gap) / 2;
        for (int i = 0; i < n; i++) {
            int age = cf - i * 5;
            if (age < 0) {
                continue;
            }
            float t = Math.min(1f, age / 8f);
            float scale = t < 1 ? 0.6f + 0.5f * t : 1 + 0.08f * (float) Math.max(0, Math.sin((age - 8) / 6f * Math.PI)) * (age < 14 ? 1 : 0);
            canvas.save();
            canvas.translate(rowLeft + i * (w + gap) + w / 2, cy);
            canvas.scale(scale, scale);
            drawCardFace(canvas, 0, 0, w, h, model.getChestCardType(i), "x" + model.getChestCardAmount(i), 1);
            canvas.restore();
        }

        float pulse = 0.5f + 0.5f * (float) Math.sin(frame * 0.12);
        ui.text(canvas, "Touchez pour continuer", cx, cy + h / 2 + 40 * density, 15,
                Color.argb((int) (160 + 95 * pulse), 255, 240, 200), Paint.Align.CENTER);
        int chests = model.getProgression().getChests();
        if (chests > 0) {
            drawPillButton(canvas, hud.chestAgainButton, "OUVRIR ENCORE (" + chests + ")",
                    Palette.RESUME, pressed == Hud.CHEST_AGAIN);
        }
    }

    private static int cardColor(int type) {
        return type == GameModel.CARD_GEMS ? Palette.GEM : Palette.ELEMENT_COLORS[type];
    }

    // Face d'une carte centrée en (cx, cy) : cadre doré, fond de la couleur de l'élément,
    // fenêtre avec le portrait (ou la gemme), quantité, bandeau avec le nom
    private void drawCardFace(Canvas canvas, float cx, float cy, float w, float h, int type, String amount, float pop) {
        int color = cardColor(type);
        float l = cx - w / 2;
        float t = cy - h / 2;
        float rad = w * 0.09f;
        float e = Math.max(1.5f * density, w * 0.025f);
        // Cadre : contour, or sombre, or
        tmpRect.set(l, t, l + w, t + h);
        paint.setColor(UiKit.OUTLINE);
        canvas.drawRoundRect(tmpRect, rad, rad, paint);
        tmpRect.inset(e, e);
        paint.setColor(UiKit.GOLD_DARK);
        canvas.drawRoundRect(tmpRect, rad, rad, paint);
        tmpRect.inset(e * 0.6f, e * 0.6f);
        paint.setColor(UiKit.GOLD);
        canvas.drawRoundRect(tmpRect, rad, rad, paint);
        // Fond de la couleur de l'élément (plus clair en haut)
        tmpRect.inset(e, e);
        paint.setColor(UiKit.shade(color, 0.32f));
        canvas.drawRoundRect(tmpRect, rad * 0.7f, rad * 0.7f, paint);
        float bottom = tmpRect.bottom;
        tmpRect.bottom = tmpRect.top + tmpRect.height() * 0.5f;
        paint.setColor(UiKit.shade(color, 0.5f));
        canvas.drawRoundRect(tmpRect, rad * 0.7f, rad * 0.7f, paint);
        tmpRect.bottom = bottom;

        // Fenêtre du portrait
        RectF win = spriteRect;
        win.set(l + w * 0.14f, t + h * 0.1f, l + w * 0.86f, t + h * 0.58f);
        paint.setColor(UiKit.OUTLINE);
        canvas.drawRoundRect(win, rad * 0.6f, rad * 0.6f, paint);
        win.inset(e, e);
        paint.setColor(UiKit.shade(color, 0.22f));
        canvas.drawRoundRect(win, rad * 0.5f, rad * 0.5f, paint);
        float wcx = win.centerX();
        float wcy = win.centerY();
        float ww = win.width();
        paint.setColor(Color.argb(90, Color.red(color), Color.green(color), Color.blue(color)));
        canvas.drawCircle(wcx, wcy, ww * 0.42f, paint);
        if (type == GameModel.CARD_GEMS) {
            drawGem(canvas, wcx, wcy, ww * 0.3f);
        } else {
            float sw = ww * 0.8f;
            float sh = sw * Sprites.MAGE_HEIGHT / Sprites.MAGE_WIDTH;
            win.set(wcx - sw / 2, wcy - sh / 2, wcx + sw / 2, wcy + sh / 2);
            canvas.drawBitmap(mageSprites[type][2], null, win, spritePaint);
        }

        // Quantité
        ui.textPx(canvas, amount, cx, t + h * 0.76f, h * 0.15f * pop, Color.WHITE, Paint.Align.CENTER, w * 0.85f);

        // Bandeau du nom
        String name = type == GameModel.CARD_GEMS ? "Gemmes" : Palette.ELEMENT_NAMES[type];
        tmpRect.set(l + w * 0.06f, t + h * 0.81f, l + w * 0.94f, t + h * 0.95f);
        paint.setColor(UiKit.OUTLINE);
        canvas.drawRoundRect(tmpRect, rad * 0.4f, rad * 0.4f, paint);
        tmpRect.inset(e * 0.8f, e * 0.8f);
        paint.setColor(UiKit.shade(color, 0.75f));
        canvas.drawRoundRect(tmpRect, rad * 0.3f, rad * 0.3f, paint);
        ui.textPx(canvas, name, cx, tmpRect.centerY() + h * 0.028f, h * 0.075f, Color.WHITE,
                Paint.Align.CENTER, tmpRect.width() - 4 * density);
    }

    // Dos d'une carte : bois, liseré doré, étoile au centre
    private void drawCardBack(Canvas canvas, float cx, float cy, float w, float h) {
        float rad = w * 0.09f;
        float e = Math.max(1.5f * density, w * 0.025f);
        tmpRect.set(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);
        paint.setColor(UiKit.OUTLINE);
        canvas.drawRoundRect(tmpRect, rad, rad, paint);
        tmpRect.inset(e, e);
        paint.setColor(UiKit.WOOD);
        canvas.drawRoundRect(tmpRect, rad, rad, paint);
        paint.setColor(UiKit.WOOD_DARK);
        paint.setStrokeWidth(e * 0.8f);
        for (float y = tmpRect.top + h * 0.12f; y < tmpRect.bottom; y += h * 0.12f) {
            canvas.drawLine(tmpRect.left + e, y, tmpRect.right - e, y, paint);
        }
        tmpRect.inset(w * 0.08f, w * 0.08f);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(e * 1.2f);
        paint.setColor(UiKit.GOLD);
        canvas.drawRoundRect(tmpRect, rad * 0.6f, rad * 0.6f, paint);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(UiKit.OUTLINE);
        canvas.drawCircle(cx, cy, w * 0.24f, paint);
        drawStar(canvas, cx, cy, w * 0.2f, true);
    }

    // Rayons alternés qui tournent lentement (couleur de la récompense)
    private void drawLightRays(Canvas canvas, float x, float y, float len, int f, int color) {
        int rays = 12;
        for (int j = 0; j < rays; j++) {
            double a = j * 2 * Math.PI / rays + f * 0.01;
            double half = Math.PI / rays * 0.45;
            path.reset();
            path.moveTo(x, y);
            path.lineTo(x + (float) Math.cos(a - half) * len, y + (float) Math.sin(a - half) * len);
            path.lineTo(x + (float) Math.cos(a + half) * len, y + (float) Math.sin(a + half) * len);
            path.close();
            paint.setColor(Color.argb(j % 2 == 0 ? 75 : 40, Color.red(color), Color.green(color), Color.blue(color)));
            canvas.drawPath(path, paint);
        }
    }

    // Gemme : diamant taillé cyan, facette sombre et reflet
    private void drawGem(Canvas canvas, float cx, float cy, float s) {
        path.reset();
        path.moveTo(cx - s, cy - s * 0.3f);
        path.lineTo(cx - s * 0.5f, cy - s * 0.9f);
        path.lineTo(cx + s * 0.5f, cy - s * 0.9f);
        path.lineTo(cx + s, cy - s * 0.3f);
        path.lineTo(cx, cy + s);
        path.close();
        paint.setColor(Palette.GEM);
        canvas.drawPath(path, paint);
        path.reset();
        path.moveTo(cx - s, cy - s * 0.3f);
        path.lineTo(cx, cy + s);
        path.lineTo(cx - s * 0.2f, cy - s * 0.3f);
        path.close();
        paint.setColor(Palette.GEM_DARK);
        canvas.drawPath(path, paint);
        paint.setColor(Color.argb(220, 255, 255, 255));
        canvas.drawCircle(cx + s * 0.2f, cy - s * 0.55f, s * 0.15f, paint);
    }

    // =====================================================================
    //  Marché
    // =====================================================================
    private void drawMarketScreen(Canvas canvas, GameModel model, int pressed) {
        Progression prog = model.getProgression();
        drawStartScreen(canvas, model, -1);
        paint.setColor(Color.argb(160, 0, 0, 0));
        canvas.drawRect(0, 0, board.width, board.height, paint);

        RectF p = hud.marketPopup;
        ui.panel(canvas, p, "MARCHÉ", Palette.MARKET);

        // Gemmes du joueur
        String gems = prog.getGems() + " gemmes";
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        paint.setTextSize(16 * density);
        float gw = paint.measureText(gems) + 20 * density;
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        float gx = board.centerX() - gw / 2;
        drawGem(canvas, gx + 7 * density, p.top + 74 * density, 7 * density);
        ui.text(canvas, gems, gx + 20 * density, p.top + 80 * density, 16, Palette.GEM, Paint.Align.LEFT);

        for (int i = 0; i < Hud.MARKET_ROWS; i++) {
            int type = Mage.BASE_TYPES + i;
            RectF r = hud.marketRows[i];
            RectF btn = hud.marketButtons[i];
            boolean owned = prog.isOwned(type);
            ui.inset(canvas, r);

            // Portrait (apparence de niveau 3 : on voit l'emblème de l'élément)
            float box = r.height() - 16 * density;
            float sh = box * Sprites.MAGE_HEIGHT / Sprites.MAGE_WIDTH;
            spriteRect.set(r.left + 8 * density, r.centerY() - sh / 2, r.left + 8 * density + box, r.centerY() + sh / 2);
            canvas.drawBitmap(mageSprites[type][2], null, spriteRect, owned ? spritePaint : lockedPaint);

            float tx = spriteRect.right + 10 * density;
            float maxW = btn.left - 8 * density - tx;
            ui.textPx(canvas, Palette.ELEMENT_NAMES[type], tx, r.top + 28 * density, 17 * density,
                    Palette.ELEMENT_COLORS[type], Paint.Align.LEFT, maxW);
            paint.setTextAlign(Paint.Align.LEFT);
            paint.setColor(Palette.TEXT_INACTIVE);
            paint.setTextSize(12 * density);
            String desc = Palette.ELEMENT_DESCRIPTIONS[type];
            fitText(desc, maxW);
            canvas.drawText(desc, tx, r.top + 48 * density, paint);
            paint.setTextSize(11 * density);
            String status;
            if (owned) {
                paint.setColor(Palette.TEAM);
                status = prog.isInTeam(type) ? "Dans l'équipe" : "À ajouter à l'équipe (MAGES)";
            } else if (prog.canBuy(type)) {
                paint.setColor(Palette.GOLD);
                status = "Unité exclusive";
            } else {
                paint.setColor(Palette.TEXT_INACTIVE);
                status = "Il manque " + (Progression.price(type) - prog.getGems()) + " gemmes";
            }
            fitText(status, maxW);
            canvas.drawText(status, tx, r.top + 68 * density, paint);

            if (owned) {
                drawSmallButton(canvas, btn, "ACQUIS", Palette.TEAM, false, 13);
            } else {
                drawPriceButton(canvas, btn, Progression.price(type), prog.canBuy(type), pressed == i);
            }
        }

        drawPillButton(canvas, hud.marketButtons[Hud.MARKET_BACK], "RETOUR",
                Palette.MENU_BUTTON, pressed == Hud.MARKET_BACK);
    }

    // Bouton de prix : gemme + nombre
    private void drawPriceButton(Canvas canvas, RectF b, int price, boolean enabled, boolean pressed) {
        RectF f = ui.button(canvas, b, Palette.MARKET, enabled && pressed, enabled);
        String text = String.valueOf(price);
        paint.setTextSize(17 * density);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        float tw = paint.measureText(text);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        float total = 14 * density + 6 * density + tw;
        float x = f.centerX() - total / 2;
        float cy = f.centerY();
        drawGem(canvas, x + 7 * density, cy - 1 * density, 7 * density);
        ui.text(canvas, text, x + 20 * density, cy + 6 * density, 17,
                enabled ? Color.WHITE : Color.rgb(170, 168, 182), Paint.Align.LEFT);
    }

    // =====================================================================
    //  Décor de l'accueil : un biome par page de la carte
    // =====================================================================
    private void drawBiomeBackground(Canvas canvas, int biome) {
        int[] c = Palette.BIOME_COLORS[biome];
        float w = board.width;
        float h = board.height;
        float horizon = h * 0.2f + board.topShift;
        int bands = 16;

        // Ciel en dégradé
        for (int i = 0; i < bands; i++) {
            paint.setColor(lerpColor(c[0], c[1], i / (float) (bands - 1)));
            canvas.drawRect(0, horizon * i / bands, w, horizon * (i + 1) / bands + 1, paint);
        }
        // Étoiles qui scintillent
        decorRandom.setSeed(biome * 977L + 3);
        for (int i = 0; i < 18; i++) {
            float x = decorRandom.nextFloat() * w;
            float y = decorRandom.nextFloat() * horizon * 0.9f;
            float r = (1 + decorRandom.nextFloat()) * density;
            float tw = 0.5f + 0.5f * (float) Math.sin(frame * 0.05 + i * 1.7);
            paint.setColor(Color.argb((int) (50 + 130 * tw), 255, 255, 255));
            canvas.drawCircle(x, y, r, paint);
        }
        // Collines (ou dunes) à l'horizon
        paint.setColor(lerpColor(c[2], Color.BLACK, 0.3f));
        for (int i = 0; i < 6; i++) {
            float r = w * (0.18f + 0.08f * decorRandom.nextFloat());
            canvas.drawCircle(w * i / 5f, horizon + r * 0.75f, r, paint);
        }
        // Sol, de plus en plus sombre vers le bas
        for (int i = 0; i < bands; i++) {
            float y0 = horizon + (h - horizon) * i / bands;
            paint.setColor(lerpColor(c[2], lerpColor(c[2], Color.BLACK, 0.45f), i / (float) (bands - 1)));
            canvas.drawRect(0, y0, w, y0 + (h - horizon) / bands + 1, paint);
        }
        // Éléments de décor (plus grands au premier plan)
        float span = Math.max(1, h - horizon - 160 * density);
        for (int i = 0; i < 16; i++) {
            float x = decorRandom.nextFloat() * w;
            float depth = decorRandom.nextFloat();
            float y = horizon + 24 * density + depth * span;
            float s = 14 * density * (0.6f + 0.7f * depth) * (0.8f + 0.4f * decorRandom.nextFloat());
            drawDecor(canvas, biome, c, x, y, s, i);
        }
        drawBiomeParticles(canvas, biome, c, w, h, horizon);
    }

    // Un élément de décor du biome, posé au point (x, y)
    private void drawDecor(Canvas canvas, int biome, int[] c, float x, float y, float s, int i) {
        switch (biome) {
            case 0: // Prairie : arbres et fleurs
                if (i % 3 == 0) {
                    paint.setColor(c[5]);
                    canvas.drawCircle(x, y, s * 0.18f, paint);
                    canvas.drawCircle(x + s * 0.4f, y - s * 0.1f, s * 0.15f, paint);
                    paint.setColor(Color.rgb(230, 90, 110));
                    canvas.drawCircle(x - s * 0.35f, y + s * 0.05f, s * 0.15f, paint);
                } else {
                    paint.setColor(Color.rgb(80, 52, 30));
                    canvas.drawRect(x - s * 0.15f, y - s * 0.6f, x + s * 0.15f, y, paint);
                    paint.setColor(c[4]);
                    canvas.drawCircle(x, y - s * 1.1f, s * 0.75f, paint);
                    canvas.drawCircle(x - s * 0.45f, y - s * 0.75f, s * 0.5f, paint);
                    canvas.drawCircle(x + s * 0.45f, y - s * 0.75f, s * 0.5f, paint);
                    paint.setColor(lerpColor(c[4], Color.WHITE, 0.25f));
                    canvas.drawCircle(x - s * 0.2f, y - s * 1.35f, s * 0.25f, paint);
                }
                break;
            case 1: // Désert : cactus et cailloux
                if (i % 2 == 0) {
                    paint.setColor(c[4]);
                    tmpRect.set(x - s * 0.18f, y - s * 1.6f, x + s * 0.18f, y);
                    canvas.drawRoundRect(tmpRect, s * 0.18f, s * 0.18f, paint);
                    tmpRect.set(x - s * 0.6f, y - s * 1.0f, x - s * 0.1f, y - s * 0.78f);
                    canvas.drawRoundRect(tmpRect, s * 0.1f, s * 0.1f, paint);
                    tmpRect.set(x - s * 0.6f, y - s * 1.35f, x - s * 0.4f, y - s * 0.8f);
                    canvas.drawRoundRect(tmpRect, s * 0.1f, s * 0.1f, paint);
                    tmpRect.set(x + s * 0.1f, y - s * 0.75f, x + s * 0.55f, y - s * 0.55f);
                    canvas.drawRoundRect(tmpRect, s * 0.1f, s * 0.1f, paint);
                    tmpRect.set(x + s * 0.35f, y - s * 1.1f, x + s * 0.55f, y - s * 0.6f);
                    canvas.drawRoundRect(tmpRect, s * 0.1f, s * 0.1f, paint);
                } else {
                    paint.setColor(lerpColor(c[2], Color.BLACK, 0.25f));
                    tmpRect.set(x - s * 0.5f, y - s * 0.3f, x + s * 0.5f, y + s * 0.05f);
                    canvas.drawOval(tmpRect, paint);
                }
                break;
            case 2: // Toundra : sapins enneigés et congères
                if (i % 3 != 0) {
                    for (int k = 0; k < 3; k++) {
                        float ty = y - s * (0.4f + 0.55f * k);
                        float tw = s * (0.8f - 0.2f * k);
                        path.reset();
                        path.moveTo(x, ty - s * 0.75f);
                        path.lineTo(x + tw, ty);
                        path.lineTo(x - tw, ty);
                        path.close();
                        paint.setColor(c[4]);
                        canvas.drawPath(path, paint);
                        path.reset();
                        path.moveTo(x, ty - s * 0.75f);
                        path.lineTo(x + tw * 0.35f, ty - s * 0.48f);
                        path.lineTo(x - tw * 0.35f, ty - s * 0.48f);
                        path.close();
                        paint.setColor(c[5]);
                        canvas.drawPath(path, paint);
                    }
                } else {
                    paint.setColor(c[5]);
                    tmpRect.set(x - s * 0.7f, y - s * 0.35f, x + s * 0.7f, y + s * 0.1f);
                    canvas.drawOval(tmpRect, paint);
                }
                break;
            case 3: // Marais : mares et roseaux
                if (i % 2 == 0) {
                    paint.setColor(Color.argb(170, Color.red(c[5]), Color.green(c[5]), Color.blue(c[5])));
                    tmpRect.set(x - s, y - s * 0.35f, x + s, y + s * 0.35f);
                    canvas.drawOval(tmpRect, paint);
                } else {
                    paint.setStrokeWidth(Math.max(1, s * 0.1f));
                    for (int k = -1; k <= 1; k++) {
                        float sway = (float) Math.sin(frame * 0.03 + i + k) * s * 0.1f;
                        paint.setColor(c[4]);
                        canvas.drawLine(x + k * s * 0.25f, y, x + k * s * 0.35f + sway, y - s * 1.3f, paint);
                        paint.setColor(Color.rgb(110, 70, 40));
                        canvas.drawCircle(x + k * s * 0.35f + sway, y - s * 1.3f, s * 0.12f, paint);
                    }
                }
                break;
            case 4: // Volcan : rochers et fissures de lave
                paint.setColor(c[4]);
                path.reset();
                path.moveTo(x - s * 0.8f, y);
                path.lineTo(x - s * 0.5f, y - s * 0.7f);
                path.lineTo(x + s * 0.1f, y - s * 0.9f);
                path.lineTo(x + s * 0.7f, y - s * 0.4f);
                path.lineTo(x + s * 0.8f, y);
                path.close();
                canvas.drawPath(path, paint);
                if (i % 2 == 0) {
                    float glow = 0.6f + 0.4f * (float) Math.sin(frame * 0.08 + i);
                    paint.setColor(Color.argb((int) (255 * glow), Color.red(c[5]), Color.green(c[5]), Color.blue(c[5])));
                    paint.setStrokeWidth(Math.max(1, s * 0.12f));
                    canvas.drawLine(x - s * 0.9f, y + s * 0.2f, x + s * 0.1f, y + s * 0.35f, paint);
                    canvas.drawLine(x + s * 0.1f, y + s * 0.35f, x + s * 0.9f, y + s * 0.15f, paint);
                }
                break;
            default: // Crépuscule : cristaux lumineux
                float glow = 0.5f + 0.5f * (float) Math.sin(frame * 0.06 + i);
                paint.setColor(Color.argb((int) (40 + 40 * glow), Color.red(c[4]), Color.green(c[4]), Color.blue(c[4])));
                canvas.drawCircle(x, y - s * 0.7f, s * 1.1f, paint);
                path.reset();
                path.moveTo(x, y - s * 1.6f);
                path.lineTo(x + s * 0.4f, y - s * 0.5f);
                path.lineTo(x, y);
                path.lineTo(x - s * 0.4f, y - s * 0.5f);
                path.close();
                paint.setColor(c[4]);
                canvas.drawPath(path, paint);
                path.reset();
                path.moveTo(x, y - s * 1.6f);
                path.lineTo(x + s * 0.4f, y - s * 0.5f);
                path.lineTo(x, y - s * 0.6f);
                path.close();
                paint.setColor(lerpColor(c[4], Color.WHITE, 0.4f));
                canvas.drawPath(path, paint);
                break;
        }
    }

    // Particules animées : neige (toundra), braises (volcan), lucioles (prairie, marais), poussière (désert)
    private void drawBiomeParticles(Canvas canvas, int biome, int[] c, float w, float h, float horizon) {
        decorRandom.setSeed(biome * 131L + 17);
        int count = biome == 2 ? 40 : biome == 4 ? 22 : biome == 1 ? 14 : (biome == 5 ? 0 : 12);
        for (int i = 0; i < count; i++) {
            float bx = decorRandom.nextFloat() * w;
            float by = decorRandom.nextFloat() * h;
            float speed = 0.5f + decorRandom.nextFloat();
            float x;
            float y;
            switch (biome) {
                case 2: // neige qui tombe
                    x = (bx + (float) Math.sin(frame * 0.02 + i) * 12 * density) % w;
                    y = (by + frame * speed * density) % h;
                    paint.setColor(Color.argb(200, 255, 255, 255));
                    canvas.drawCircle(x, y, (1 + speed) * density, paint);
                    break;
                case 4: // braises qui montent
                    x = bx + (float) Math.sin(frame * 0.03 + i) * 8 * density;
                    y = h - ((h - by) + frame * speed * 0.8f * density) % h;
                    paint.setColor(Color.argb(200, 255, 140 + i * 4 % 80, 40));
                    canvas.drawCircle(x, y, 1.6f * density, paint);
                    break;
                case 1: // poussière soufflée par le vent
                    x = (bx + frame * speed * 1.2f * density) % w;
                    y = horizon + (by % Math.max(1, h - horizon));
                    paint.setColor(Color.argb(90, 250, 225, 170));
                    canvas.drawCircle(x, y, 1.4f * density, paint);
                    break;
                default: // lucioles qui clignotent
                    x = bx + (float) Math.sin(frame * 0.01 * speed + i) * 20 * density;
                    y = horizon + (by % Math.max(1, h - horizon)) + (float) Math.cos(frame * 0.013 + i) * 14 * density;
                    float blink = 0.5f + 0.5f * (float) Math.sin(frame * 0.07 * speed + i * 2);
                    paint.setColor(Color.argb((int) (220 * blink), 230, 255, 140));
                    canvas.drawCircle(x, y, 1.8f * density, paint);
                    break;
            }
        }
    }

    // Mélange de deux couleurs (t = 0 : a, t = 1 : b)
    private static int lerpColor(int a, int b, float t) {
        return Color.rgb((int) (Color.red(a) + (Color.red(b) - Color.red(a)) * t),
                (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * t),
                (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t));
    }
}
