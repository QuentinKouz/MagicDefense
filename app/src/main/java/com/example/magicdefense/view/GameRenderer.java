package com.example.magicdefense.view;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;

import com.example.magicdefense.controller.TouchController;
import com.example.magicdefense.model.Achievement;
import com.example.magicdefense.model.Artifact;
import com.example.magicdefense.model.Board;
import com.example.magicdefense.model.Chest;
import com.example.magicdefense.model.Rarity;
import com.example.magicdefense.model.Knight;
import com.example.magicdefense.model.Ultimate;
import com.example.magicdefense.model.Bolt;
import com.example.magicdefense.model.Enemy;
import com.example.magicdefense.model.EnemyBolt;
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
    private final RectF auraRect = new RectF(); // cercles magiques au sol (chaman, gobelin mage)
    private final RectF spriteRect = new RectF(); // sprite de l'écran MAGES
    private int frame = 0; // compteur d'images, pour l'animation de pulsation
    private boolean night = false; // mode nuit pour l'image en cours
    private GameState lastState = null;
    private int stateFrames = 0; // images depuis le dernier changement d'écran
    // Sprites pixel art (créés une seule fois)
    private final Bitmap[][] mageSprites = Sprites.buildMages();       // [élément][niveau - 1]
    private final Bitmap[] goblinDay = Sprites.buildGoblins(false);    // 2 images de marche
    private final Bitmap[] goblinNight = Sprites.buildGoblins(true);   // avec la torche
    private final Bitmap[] shamanDay = Sprites.buildShaman(false);     // chaman : capuche, crâne, plumes
    private final Bitmap[] shamanNight = Sprites.buildShaman(true);
    private final Bitmap[] goblinMageDay = Sprites.buildGoblinMage(false); // gobelin mage : chapeau, orbe
    private final Bitmap[] goblinMageNight = Sprites.buildGoblinMage(true);
    private final Paint spritePaint = new Paint();                     // pixels nets
    private final Paint stunnedPaint = new Paint();                    // teinte bleu glacé
    private final Paint fastPaint = new Paint();                       // gobelin rapide : teinte orangée
    private final Paint tankPaint = new Paint();                       // gobelin costaud : rouge sombre
    private final Paint bossPaint = new Paint();                       // chef gobelin : vert sombre
    private final Paint impPaint = new Paint();                        // diablotin : rouge vif
    private final Paint lockedPaint = new Paint();                     // unité pas encore achetée : assombrie
    private final Random decorRandom = new Random();                   // décor de l'accueil (graine fixe par biome)

    private final Bitmap[] chestSprites = Sprites.buildChest();        // { couvercle, caisse }
    private final Bitmap[] knightSprites = Sprites.buildKnight();      // { garde, coup d'épée }
    private final Paint hitPaint = new Paint();                        // chevalier frappé : rougit
    private final MenuList menu;                                       // lignes des listes (Marché, Mages, Château)
    private final RectF rowRect = new RectF();
    private final RectF btnRect = new RectF();
    private final RectF cellRect = new RectF();
    private final Paint[] chestPaints = new Paint[Chest.COUNT];

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
        bossPaint.setFilterBitmap(false);
        bossPaint.setColorFilter(new LightingColorFilter(0x90B070, 0x101000));
        impPaint.setFilterBitmap(false);
        impPaint.setColorFilter(new LightingColorFilter(0xFF6060, 0x400000));
        lockedPaint.setFilterBitmap(false);
        lockedPaint.setColorFilter(new LightingColorFilter(0x505060, 0x000000));
        // Coffres : bois naturel, doré, violet
        for (int c = 0; c < Chest.COUNT; c++) {
            chestPaints[c] = new Paint();
            chestPaints[c].setFilterBitmap(false);
        }
        chestPaints[Chest.BIG].setColorFilter(new LightingColorFilter(0xFFF0B0, 0x302000));
        chestPaints[Chest.MYSTERY].setColorFilter(new LightingColorFilter(0xC090FF, 0x200030));
        hitPaint.setFilterBitmap(false);
        hitPaint.setColorFilter(new LightingColorFilter(0xFF8080, 0x400000));
        attacks = new AttackRenderer(density);
        ui = new UiKit(density);
        menu = new MenuList(density);
    }

    // À appeler quand la taille de l'écran change (après board.resize et hud.resize)
    public void onResize() {
        terrain.build(board);
    }

    public void draw(Canvas canvas, GameModel model, TouchController touch) {
        frame++;
        attacks.setFrame(frame);
        attacks.setLowFx(!model.isAttackEffectsEnabled()); // option « Effets des attaques »
        if (model.getState() == GameState.START) {
            java.util.Arrays.fill(coinAge, -1); // de retour à l'accueil : plus de pièce en vol
        }
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
                drawHome(canvas, model, touch, touch.getPressedMenuButton());
                break;
            case PLAYING:
                drawGame(canvas, model, touch);
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
            case CHEST_OPENING:
                drawChestOpening(canvas, model, touch, touch.getPressedMenuButton());
                break;
        }
        drawToast(canvas, model); // succès débloqué : par-dessus tous les écrans
    }

    // ---------- Écrans ----------
    // Accueil : décor, page courante (et sa voisine pendant un glissement), barre d'onglets
    private void drawHome(Canvas canvas, GameModel model, TouchController touch, int pressed) {
        drawBiomeBackground(canvas, model.getMapBiome()); // le décor change avec la page de la carte
        int page = model.getHomePage();
        float offset = touch.getPageOffset();
        float w = board.width;
        drawHomePage(canvas, model, touch, page, offset, pressed);
        if (offset > 0 && page > 0) {
            drawHomePage(canvas, model, touch, page - 1, offset - w, -1);
        } else if (offset < 0 && page < GameModel.PAGE_COUNT - 1) {
            drawHomePage(canvas, model, touch, page + 1, offset + w, -1);
        }
        drawTabBar(canvas, model, pressed);
        // Fiche d'un mage par-dessus tout l'accueil
        if (page == GameModel.PAGE_MAGES && model.getSelectedMage() >= 0 && model.getState() == GameState.START) {
            drawMageSheet(canvas, model, model.getSelectedMage(), pressed);
        }
    }

    private void drawHomePage(Canvas canvas, GameModel model, TouchController touch, int page, float dx, int pressed) {
        canvas.save();
        canvas.translate(dx, 0);
        switch (page) {
            case GameModel.PAGE_CAMPAIGN:
                drawStartScreen(canvas, model, pressed);
                break;
            case GameModel.PAGE_MAGES:
                drawMagesPage(canvas, model, touch.getScroll(page), pressed);
                break;
            case GameModel.PAGE_MARKET:
                drawListPage(canvas, model, page, "MARCHÉ", Palette.MARKET, touch.getScroll(page), pressed);
                break;
            case GameModel.PAGE_ENDLESS:
                drawEndlessPage(canvas, model, pressed);
                break;
            case GameModel.PAGE_CASTLE:
                drawListPage(canvas, model, page, "CHÂTEAU", Palette.CASTLE_PAGE, touch.getScroll(page), pressed);
                break;
        }
        canvas.restore();
    }

    // Campagne : carte des niveaux reliés par un chemin, bulle, JOUER, options et son
    private void drawStartScreen(Canvas canvas, GameModel model, int pressed) {
        int biome = model.getMapBiome();

        // Titre en lettres dorées à contour (un peu plus petit sur un écran étroit, pour laisser
        // la place aux boutons du coin)
        float titleSize = Math.min(28, board.playWidth / density / 15);
        ui.text(canvas, "MAGIC DEFENSE", board.centerX(), 70 * density + board.topShift, titleSize,
                Palette.GOLD, Paint.Align.CENTER);

        // Biome et page de la carte (les niveaux sont sans fin, 6 par page)
        int first = model.levelAtSlot(1);
        ui.text(canvas, model.getMapBiomeTitle() + " · Niveaux " + first + " – "
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

        // Niveau de nuit : petite lune à gauche du cercle
        if (LevelSpec.isNightLevel(lv)) {
            drawMoon(canvas, cx - r * 0.95f, cy - r * 0.75f, 9 * density);
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
        float bw = (spec.isGenerated() ? (spec.nightLevel ? 184 : 150) : 130) * density;
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
        if (spec.nightLevel) {
            detail += " · NUIT";
        }
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
        drawGame(canvas, model, touch);

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
            drawChestsEarned(canvas, model, p.top + 168 * density);
            int diff = model.getSpec().endlessDifficulty;
            String line = "Difficulté : " + LevelSpec.DIFFICULTY_NAMES[diff];
            if (model.getLastBonusGems() > 0) {
                line += " · +" + model.getLastBonusGems() + " gemmes";
            }
            ui.text(canvas, line, board.centerX(), p.top + 200 * density, 13, Palette.DIFFICULTY_COLORS[diff],
                    Paint.Align.CENTER);
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

    private void drawGame(Canvas canvas, GameModel model, TouchController touch) {
        Mage dragged = touch.getDraggedMage();

        terrain.draw(canvas, night, model.getBiome()); // forêt, prairie, rempart, cour... teintés selon le biome

        // Rempart qui rougit quand un gobelin l'atteint
        float wallFlash = model.getCastleHitFlash();
        if (wallFlash > 0) {
            paint.setColor(Color.argb((int) (150 * wallFlash), 230, 50, 70));
            canvas.drawRect(0, terrain.getWallTop(), board.width, board.gridTop, paint);
        }

        drawGrid(canvas, model, dragged, touch);

        // Zones au sol (lave, vapeur) sous les ennemis
        for (Zone z : model.getZones()) {
            attacks.drawZone(canvas, z);
        }

        // Bouclier divin : dôme lumineux devant le rempart
        if (model.isShielded()) {
            drawShieldDome(canvas, model.getShield());
        }

        for (com.example.magicdefense.model.Wall w : model.getWalls()) {
            attacks.drawWall(canvas, w); // murs des Ouvriers, devant les chevaliers
        }
        for (Knight k : model.getKnights()) {
            drawKnight(canvas, k);
        }
        for (Enemy enemy : model.getEnemies()) {
            drawEnemy(canvas, enemy, model.isStunned() || enemy.stunTimer > 0);
        }
        for (EnemyBolt b : model.getEnemyBolts()) {
            drawEnemyBolt(canvas, b); // projectiles des gobelins mages
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

        // Météorite : chute puis explosion
        drawMeteor(canvas, model);

        drawRecruitButtons(canvas, model, touch.getPressedButton(),
                touch.getPressedGameButton() == Hud.GAME_RANDOM);
        drawTopInfo(canvas, model);
        drawFlyingCoins(canvas, model.getState() == GameState.PLAYING);
        drawBossBar(canvas, model);
        drawAnnounce(canvas, model);
        drawSkillText(canvas, model);
        drawUltimateGauge(canvas, model);
        drawGameButtons(canvas, model, touch.getPressedGameButton());
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
    private void drawGrid(Canvas canvas, GameModel model, Mage dragged, TouchController touch) {
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                float left = board.cellLeft(col);
                float top = board.cellTop(row);
                Mage mage = model.getMage(row, col);

                // Case bloquée : chaînes, cadenas et prix en PO
                if (model.isLocked(row, col)) {
                    drawLockedCell(canvas, model, left, top,
                            touch.getPressedLockedRow() == row && touch.getPressedLockedCol() == col);
                    continue;
                }

                // (les cases elles-mêmes font partie du décor : dalles de la cour)
                // Le mage en cours de déplacement n'est pas dessiné dans sa case
                if (mage != null && mage != dragged) {
                    if (model.isFury()) {
                        // Rage arcanique : aura rouge qui palpite
                        float pulse = 0.5f + 0.5f * (float) Math.sin(frame * 0.3 + col);
                        paint.setColor(Color.argb((int) (60 + 60 * pulse), 255, 70, 50));
                        canvas.drawCircle(left + board.cellWidth / 2, top + board.cellHeight / 2,
                                board.cellHeight * 0.45f, paint);
                    }
                    drawMage(canvas, mage, left, top);
                    if (mage.hasSkill()) {
                        drawSkillRing(canvas, mage, left, top);
                    }
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
        // Chaman et gobelin mage : sprites à part (robe, capuche ou chapeau), sans torche
        boolean shaman = enemy.kind == Enemy.SHAMAN;
        boolean caster = enemy.kind == Enemy.MAGE;
        float centerX = shaman ? Sprites.SHAMAN_CENTER_X : caster ? Sprites.GOBLIN_MAGE_CENTER_X : Sprites.GOBLIN_CENTER_X;
        int spriteW = shaman ? Sprites.SHAMAN_WIDTH : caster ? Sprites.GOBLIN_MAGE_WIDTH : Sprites.GOBLIN_WIDTH;
        int spriteH = shaman ? Sprites.SHAMAN_HEIGHT : caster ? Sprites.GOBLIN_MAGE_HEIGHT : Sprites.GOBLIN_HEIGHT;
        float left = enemy.x - centerX * scale;
        float top = enemy.y + (Sprites.GOBLIN_HEIGHT / 2f - spriteH) * scale; // pieds au même niveau
        float w = spriteW * scale;
        float h = spriteH * scale;

        // Marche : 2 images en alternance, décalées d'un gobelin à l'autre ; immobile s'il est étourdi
        int step = stunned ? 0 : ((frame / 10) + (int) enemy.x) % 2;
        if (caster && enemy.inPosition) {
            step = 0; // en position de tir : il ne marche plus
        }

        float flameX = left + Sprites.TORCH_FLAME_X * scale;
        float flameY = top + Sprites.TORCH_FLAME_Y * scale;
        if (night && !shaman && !caster) {
            drawTorchGlow(canvas, flameX, flameY, enemy); // halo derrière le gobelin
        }

        tmpRect.set(left, top, left + w, top + h);
        Bitmap sprite;
        if (shaman) {
            sprite = (night ? shamanNight : shamanDay)[step];
            drawShamanAura(canvas, enemy, top + h, stunned); // cercle d'invocation au sol
        } else if (caster) {
            sprite = (night ? goblinMageNight : goblinMageDay)[step];
            drawGoblinMageAura(canvas, enemy, top + h);
        } else {
            sprite = (night ? goblinNight : goblinDay)[step];
        }
        // Chef gobelin : halo rouge qui pulse (il invoque des diablotins)
        if (enemy.isBoss()) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(frame * 0.12 + enemy.x);
            paint.setColor(Color.argb((int) (40 + 40 * pulse), 255, 60, 60));
            canvas.drawCircle(enemy.x, enemy.y, enemy.radius * (1.1f + 0.15f * pulse), paint);
        }

        Paint p = spritePaint;
        if (stunned) {
            p = stunnedPaint;
        } else {
            switch (enemy.kind) {
                case Enemy.FAST:   p = fastPaint; break;   // rapide : plus petit et orangé
                case Enemy.TANK:   p = tankPaint; break;   // costaud : plus gros et rouge sombre
                case Enemy.BOSS:   p = bossPaint; break;   // chef : vert sombre, énorme
                case Enemy.IMP:    p = impPaint; break;    // diablotin : minuscule et rouge
            }
        }
        canvas.drawBitmap(sprite, null, tmpRect, p);

        if (shaman) {
            drawShamanSkullGlow(canvas, enemy, left, top, scale, stunned);
        } else if (caster) {
            drawGoblinMageOrb(canvas, enemy, left, top, scale, stunned);
        } else if (night) {
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

    // ---------- Chaman et gobelin mage ----------

    // Chaman : cercle d'invocation vert au sol, runes qui tournent et âmes qui montent,
    // de plus en plus vif à l'approche de l'invocation
    private void drawShamanAura(Canvas canvas, Enemy e, float feetY, boolean stunned) {
        float charge = 1f - Math.max(0, e.summonTimer) / (float) Math.max(1, Enemy.SUMMON_DELAY[e.kind]);
        charge = Math.max(0f, Math.min(1f, charge));
        float rx = e.radius * 1.35f;
        float ry = rx * 0.38f;
        float cy = feetY - ry * 0.4f;
        auraRect.set(e.x - rx, cy - ry, e.x + rx, cy + ry);
        paint.setColor(Color.argb((int) (35 + 75 * charge), 90, 255, 140));
        canvas.drawOval(auraRect, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f * density);
        paint.setColor(Color.argb((int) (120 + 120 * charge), 120, 255, 160));
        canvas.drawOval(auraRect, paint);
        paint.setStyle(Paint.Style.FILL);
        // Runes : 6 points qui tournent sur le cercle
        double turn = stunned ? 0 : frame * 0.05;
        for (int i = 0; i < 6; i++) {
            double a = turn + i * Math.PI / 3;
            canvas.drawCircle(e.x + (float) Math.cos(a) * rx * 0.8f, cy + (float) Math.sin(a) * ry * 0.8f,
                    1.8f * density, paint);
        }
        // Âmes vertes qui s'élèvent autour de lui
        for (int i = 0; i < 4; i++) {
            float t = ((frame + i * 13 + (int) e.x) % 50) / 50f;
            float px = e.x + (float) Math.sin(i * 2.1 + frame * 0.05) * rx * 0.75f;
            float py = cy - t * e.radius * 2.4f;
            paint.setColor(Color.argb((int) (220 * (1 - t) * (0.35f + 0.65f * charge)), 140, 255, 170));
            canvas.drawCircle(px, py, (1.5f + 1.5f * charge) * density, paint);
        }
    }

    // Lueur verte dans les orbites du crâne du bâton
    private void drawShamanSkullGlow(Canvas canvas, Enemy e, float left, float top, int scale, boolean stunned) {
        if (stunned) {
            return;
        }
        float charge = 1f - Math.max(0, e.summonTimer) / (float) Math.max(1, Enemy.SUMMON_DELAY[e.kind]);
        float pulse = 0.5f + 0.5f * (float) Math.sin(frame * 0.2 + e.x);
        float sx = left + Sprites.SHAMAN_SKULL_X * scale;
        float sy = top + Sprites.SHAMAN_SKULL_Y * scale;
        paint.setColor(Color.argb((int) (60 + 80 * pulse * Math.max(0.3f, charge)), 90, 255, 140));
        canvas.drawCircle(sx, sy, scale * (2.2f + 1.5f * charge), paint);
    }

    // Gobelin mage : cercle magique bleu sous ses pieds quand il est en position de tir
    private void drawGoblinMageAura(Canvas canvas, Enemy e, float feetY) {
        if (!e.inPosition) {
            return;
        }
        float rx = e.radius * 1.2f;
        float ry = rx * 0.36f;
        float cy = feetY - ry * 0.4f;
        auraRect.set(e.x - rx, cy - ry, e.x + rx, cy + ry);
        paint.setColor(Color.argb(60, 90, 150, 255));
        canvas.drawOval(auraRect, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.5f * density);
        paint.setColor(Color.argb(190, 130, 190, 255));
        canvas.drawOval(auraRect, paint);
        auraRect.inset(rx * 0.3f, ry * 0.3f);
        canvas.drawOval(auraRect, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    // Orbe du gobelin mage : il se charge avant chaque tir (halo qui grossit, étincelles)
    private void drawGoblinMageOrb(Canvas canvas, Enemy e, float left, float top, int scale, boolean stunned) {
        float ox = left + Sprites.GOBLIN_MAGE_ORB_X * scale;
        float oy = top + Sprites.GOBLIN_MAGE_ORB_Y * scale;
        float charge = e.inPosition && !stunned
                ? 1f - Math.max(0, e.castTimer) / (float) Enemy.MAGE_CAST_DELAY : 0f;
        float pulse = 0.5f + 0.5f * (float) Math.sin(frame * 0.18 + e.x);
        paint.setColor(Color.argb((int) (50 + 40 * pulse + 100 * charge), 110, 200, 255));
        canvas.drawCircle(ox, oy, scale * (2.2f + 3f * charge), paint);
        if (charge > 0.6f) {
            // Étincelles qui tournent autour de l'orbe juste avant le tir
            paint.setColor(Color.argb(230, 235, 250, 255));
            for (int i = 0; i < 3; i++) {
                double a = frame * 0.3 + i * 2 * Math.PI / 3;
                float r = scale * (3.5f - 1.5f * charge);
                canvas.drawCircle(ox + (float) Math.cos(a) * r, oy + (float) Math.sin(a) * r, 1.4f * density, paint);
            }
        }
    }

    // Projectile magique d'un gobelin mage : boule bleue avec une traînée et des étincelles
    private void drawEnemyBolt(Canvas canvas, EnemyBolt b) {
        float dx = b.dirX();
        float dy = b.dirY();
        // Traînée : des cercles de plus en plus petits et transparents derrière lui
        for (int i = 5; i >= 1; i--) {
            float back = i * 5 * density;
            paint.setColor(Color.argb(150 - i * 25, 90, 150, 255));
            canvas.drawCircle(b.x - dx * back, b.y - dy * back, (6 - i * 0.8f) * density, paint);
        }
        float pulse = 0.5f + 0.5f * (float) Math.sin(b.age * 0.5);
        paint.setColor(Color.argb(90, 110, 180, 255));
        canvas.drawCircle(b.x, b.y, (9 + 2 * pulse) * density, paint);
        paint.setColor(Color.argb(220, 140, 210, 255));
        canvas.drawCircle(b.x, b.y, 5.5f * density, paint);
        paint.setColor(Color.WHITE);
        canvas.drawCircle(b.x, b.y, 2.8f * density, paint);
        // Deux étincelles en orbite
        for (int i = 0; i < 2; i++) {
            double a = b.age * 0.35 + i * Math.PI;
            canvas.drawCircle(b.x + (float) Math.cos(a) * 8 * density, b.y + (float) Math.sin(a) * 8 * density,
                    1.3f * density, paint);
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

    private void drawRecruitButtons(Canvas canvas, GameModel model, int pressedButton, boolean pressedRandom) {
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
            boolean canAfford = model.canAffordMage(type); // le prix dépend de la rareté du mage

            // Tuile de la couleur de l'élément : grisée si pas assez d'or, enfoncée si appuyée
            RectF f = ui.button(canvas, b, Palette.ELEMENT_COLORS[type], canAfford && i == pressedButton, canAfford);
            int textColor = canAfford ? Color.WHITE : Color.rgb(170, 168, 182);
            ui.textPx(canvas, Palette.ELEMENT_NAMES[type], f.centerX(), f.centerY() - 1 * density, 14 * density,
                    textColor, Paint.Align.CENTER, f.width() - 8 * density); // « Invocateur » tient dans la tuile

            // Prix : pièce d'or + prix (selon la rareté ; il monte à chaque mage choisi)
            drawBarPrice(canvas, f, model.getMageCost(type), canAfford, textColor);
        }

        // Dernière case : mage au hasard parmi l'équipe, prix fixe
        RectF r = hud.randomButton;
        boolean canRandom = model.canRecruitRandom() && model.hasFreeCell();
        boolean pressed = canRandom && pressedRandom;
        RectF f = ui.button(canvas, r, Palette.ENDLESS, pressed, canRandom);
        drawDice(canvas, f.centerX(), f.centerY() - 4 * density, 9 * density, canRandom);
        drawBarPrice(canvas, f, GameModel.RANDOM_MAGE_COST, canRandom,
                canRandom ? Color.WHITE : Color.rgb(170, 168, 182));
    }

    private void drawBarPrice(Canvas canvas, RectF f, int price, boolean can, int textColor) {
        float priceY = f.centerY() + 15 * density;
        String cost = String.valueOf(price);
        float half = (12 * density + cost.length() * 7.5f * density) / 2; // pièce + chiffres, centrés
        ui.coin(canvas, f.centerX() - half + 6 * density, priceY - 4.5f * density, 6 * density);
        ui.text(canvas, cost, f.centerX() - half + 14 * density, priceY, 13, can ? Palette.GOLD : textColor,
                Paint.Align.LEFT);
    }

    // ---------- Pièces d'or ----------
    private static final int MAX_FLYING_COINS = 24;
    private static final int COIN_FLIGHT_FRAMES = 32; // ≈ 0,5 s
    private final float[] coinStartX = new float[MAX_FLYING_COINS];
    private final float[] coinStartY = new float[MAX_FLYING_COINS];
    private final int[] coinAge = new int[MAX_FLYING_COINS]; // -1 : emplacement libre
    private float goldCoinX, goldCoinY; // pièce du compteur de PO (cible des pièces qui s'envolent)
    private int goldPulse = 0;          // > 0 : le compteur grossit un instant (pièce arrivée)

    {
        java.util.Arrays.fill(coinAge, -1);
    }

    // Un gobelin meurt : une pièce s'envole de sa position vers le compteur de PO
    // (rien si l'option « Effets des attaques » est sur Non)
    public void spawnCoin(float x, float y) {
        if (attacks.isLowFx()) {
            return;
        }
        for (int i = 0; i < MAX_FLYING_COINS; i++) {
            if (coinAge[i] < 0) {
                coinStartX[i] = x;
                coinStartY[i] = y;
                coinAge[i] = 0;
                return;
            }
        }
        // Trop de pièces en vol : on n'en ajoute pas (le compteur, lui, est déjà à jour)
    }

    // Pièces en vol : elles montent un peu, puis filent vers le compteur en accélérant
    private void drawFlyingCoins(Canvas canvas, boolean advance) {
        for (int i = 0; i < MAX_FLYING_COINS; i++) {
            if (coinAge[i] < 0) {
                continue;
            }
            if (attacks.isLowFx()) {
                coinAge[i] = -1; // option désactivée pendant le vol
                continue;
            }
            float t = coinAge[i] / (float) COIN_FLIGHT_FRAMES;
            float e = t * t; // accélère vers la cible
            float x = coinStartX[i] + (goldCoinX - coinStartX[i]) * e;
            float y = coinStartY[i] + (goldCoinY - coinStartY[i]) * e - (float) Math.sin(t * Math.PI) * 30 * density;
            ui.coin(canvas, x, y, (7 - 2 * t) * density);
            if (advance && ++coinAge[i] > COIN_FLIGHT_FRAMES) {
                coinAge[i] = -1;
                goldPulse = 8;
            }
        }
    }

    // Compteur de PO : pièce d'or en pixel art suivie du nombre, l'ensemble centré en cx
    private void drawGoldCounter(Canvas canvas, int gold, float cx, float y) {
        String text = String.valueOf(gold);
        float scale = 1f + 0.03f * goldPulse; // petit rebond quand une pièce arrive
        if (goldPulse > 0) {
            goldPulse--;
        }
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        paint.setTextSize(20 * density * scale);
        float textW = paint.measureText(text);
        paint.setTypeface(Typeface.DEFAULT);
        float coinR = 10 * density * scale;
        float gap = 6 * density;
        float left = cx - (2 * coinR + gap + textW) / 2;
        goldCoinX = left + coinR;
        goldCoinY = y - 7 * density;
        ui.coin(canvas, goldCoinX, goldCoinY, coinR);
        ui.text(canvas, text, left + 2 * coinR + gap, y, 20 * scale, Palette.GOLD, Paint.Align.LEFT);
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

        // PO sous la vague (le coin haut-gauche est pris par la jauge) : pièce d'or + nombre
        drawGoldCounter(canvas, model.getGold(), cx, 92 * density + shift);

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
        // (pendant l'effet : la couleur de l'ultime, qui se vide)
        int ult = model.getUltimateId();
        int ultColor = Palette.ULTIMATE_COLORS[ult];
        float active = model.isStunned() ? model.getStun() : model.isFury() ? model.getFury()
                : model.isShielded() ? model.getShield() : model.isMeteorFalling() ? 1f : 0f;
        if (active > 0) {
            paint.setColor(ultColor);
            canvas.drawArc(gaugeOval, -90, 360 * active, false, paint);
        } else {
            paint.setColor(ready ? Palette.GOLD : UiKit.shade(ultColor, 0.8f));
            canvas.drawArc(gaugeOval, -90, 360 * model.getUltimateCharge(), false, paint);
        }

        paint.setStyle(Paint.Style.FILL); // on remet le mode normal

        // Texte au centre (et en dessous quand c'est prêt)
        String[] shortNames = {"STUN", "RAGE", "MÉTÉO", "ÉGIDE"};
        if (active > 0) {
            ui.text(canvas, shortNames[ult], cx, cy + 5 * density, 12, ultColor, Paint.Align.CENTER);
        } else if (ready) {
            ui.text(canvas, shortNames[ult], cx, cy + 5 * density, 12, Palette.GOLD, Paint.Align.CENTER);
            ui.text(canvas, model.isAutoMode() ? "Auto" : "Touchez !", cx, cy + radius + 22 * density, 13,
                    Palette.GOLD, Paint.Align.CENTER);
        } else if (model.getUltimateCooldown() > 0) {
            // Recharge d'une minute : voile qui se retire et secondes restantes
            paint.setColor(Color.argb(150, 10, 8, 20));
            canvas.drawArc(gaugeOval, -90, 360 * model.getUltimateCooldown(), true, paint);
            ui.text(canvas, model.getUltimateCooldownSeconds() + " s", cx, cy + 5 * density, 12,
                    Palette.TEXT_INACTIVE, Paint.Align.CENTER);
        } else {
            ui.text(canvas, model.getUltimateKills() + "/" + model.getUltimateKillsRequired(),
                    cx, cy + 5 * density, 12, Color.WHITE, Paint.Align.CENTER);
        }
    }

    // ---------- Mise à jour : éléments de la partie ----------

    // Case bloquée : dalle assombrie, chaînes en croix, cadenas et prix
    private void drawLockedCell(Canvas canvas, GameModel model, float left, float top, boolean pressed) {
        float m = 4 * density;
        tmpRect.set(left + m, top + m, left + board.cellWidth - m, top + board.cellHeight - m);
        paint.setColor(Color.argb(pressed ? 120 : 170, 10, 8, 20));
        canvas.drawRoundRect(tmpRect, 8 * density, 8 * density, paint);
        // Chaînes
        paint.setColor(Color.argb(200, 150, 150, 165));
        paint.setStrokeWidth(3 * density);
        canvas.drawLine(tmpRect.left + 6 * density, tmpRect.top + 6 * density,
                tmpRect.right - 6 * density, tmpRect.bottom - 6 * density, paint);
        canvas.drawLine(tmpRect.right - 6 * density, tmpRect.top + 6 * density,
                tmpRect.left + 6 * density, tmpRect.bottom - 6 * density, paint);
        float cx = tmpRect.centerX();
        float cy = tmpRect.top + tmpRect.height() * 0.36f;
        float py = tmpRect.bottom - 5 * density; // (drawLock réutilise tmpRect : on retient la position avant)
        paint.setColor(UiKit.OUTLINE);
        canvas.drawCircle(cx, cy, 13 * density, paint);
        drawLock(canvas, cx, cy, 7 * density);
        // Prix : pièce + nombre (doré si on peut payer)
        boolean can = model.canUnlockCell();
        ui.coin(canvas, cx - 12 * density, py - 4.5f * density, 6 * density);
        ui.text(canvas, String.valueOf(model.getUnlockCost()), cx - 3 * density, py, 13,
                can ? Palette.GOLD : Palette.TEXT_INACTIVE, Paint.Align.LEFT);
    }

    // Compétence d'un mage de niveau 5 : anneau de recharge, puis étoile qui pulse quand c'est prêt
    private void drawSkillRing(Canvas canvas, Mage mage, float left, float top) {
        float cx = left + board.cellWidth - 14 * density;
        float cy = top + 14 * density;
        float r = 10 * density;
        paint.setColor(Color.argb(210, 26, 22, 38));
        canvas.drawCircle(cx, cy, r + 2 * density, paint);
        if (mage.isSkillReady()) {
            float pulse = 0.5f + 0.5f * (float) Math.sin(frame * 0.2);
            paint.setColor(Color.argb((int) (90 + 90 * pulse), 255, 215, 90));
            canvas.drawCircle(cx, cy, r + (3 + 3 * pulse) * density, paint);
            drawStar(canvas, cx, cy, r * 0.85f, true);
        } else {
            gaugeOval.set(cx - r, cy - r, cx + r, cy + r);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3 * density);
            paint.setColor(Palette.GAUGE_EMPTY);
            canvas.drawArc(gaugeOval, 0, 360, false, paint);
            paint.setColor(Palette.ELEMENT_COLORS[mage.type]);
            canvas.drawArc(gaugeOval, -90, 360 * mage.getSkillProgress(), false, paint);
            paint.setStyle(Paint.Style.FILL);
        }
    }

    // Chevalier : sprite, coup d'épée en alternance quand il se bat, barre de vie
    private void drawKnight(Canvas canvas, Knight k) {
        // Grand chevalier : environ la largeur d'une case, agrandi d'un nombre entier (pixels nets)
        int scale = Math.max(1, (int) (k.radius * 2.4f / Sprites.KNIGHT_WIDTH));
        float w = Sprites.KNIGHT_WIDTH * scale;
        float h = Sprites.KNIGHT_HEIGHT * scale;
        // Apparition : il grandit en sortant d'un cercle magique
        float grow = Math.min(1f, k.age / 12f);
        if (k.age < 20) {
            paint.setColor(Color.argb((int) (160 * (1 - k.age / 20f)), 230, 110, 200));
            canvas.drawCircle(k.x, k.y + h * 0.4f, k.radius * 1.3f, paint);
        }
        // Ombre au sol
        paint.setColor(Color.argb(70, 0, 0, 0));
        auraRect.set(k.x - w * 0.4f * grow, k.y + h * 0.42f, k.x + w * 0.4f * grow, k.y + h * 0.52f);
        canvas.drawOval(auraRect, paint);
        // Marche : petits sauts à chaque pas ; en garde : respiration lente
        float bob;
        if (k.fighting) {
            bob = 0;
        } else if (k.marching) {
            bob = -Math.abs((float) Math.sin(frame * 0.3 + k.x)) * scale;
        } else {
            bob = (float) Math.sin(frame * 0.15 + k.x) * density;
        }
        tmpRect.set(k.x - w / 2 * grow, k.y - h / 2 * grow + bob, k.x + w / 2 * grow, k.y + h / 2 * grow + bob);
        boolean strike = k.fighting && k.attackTimer > Knight.ATTACK_DELAY - 8;
        canvas.drawBitmap(knightSprites[strike ? 1 : 0], null, tmpRect, k.hitTimer > 0 ? hitPaint : spritePaint);
        // Barre de vie
        float bw = k.radius * 1.6f;
        float bh = 4 * density;
        float bl = k.x - bw / 2;
        float bt = tmpRect.top - bh - 3 * density;
        paint.setColor(Color.DKGRAY);
        canvas.drawRect(bl, bt, bl + bw, bt + bh, paint);
        paint.setColor(Palette.KNIGHT_BAR);
        canvas.drawRect(bl, bt, bl + bw * Math.max(0f, k.hp / k.maxHp), bt + bh, paint);
    }

    // Bouclier divin : dôme bleu translucide qui ondule devant le rempart
    private void drawShieldDome(Canvas canvas, float t) {
        float alpha = Math.min(1f, t * 4);
        float top = board.gridTop - 70 * density;
        float pulse = (float) Math.sin(frame * 0.15) * 4 * density;
        tmpRect.set(board.playLeft - 30 * density, top - pulse, board.playRight() + 30 * density,
                board.gridTop + 140 * density);
        paint.setColor(Color.argb((int) (55 * alpha), 120, 200, 255));
        canvas.drawOval(tmpRect, paint);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4 * density);
        paint.setColor(Color.argb((int) (200 * alpha), 170, 225, 255));
        canvas.drawArc(tmpRect, 180, 180, false, paint);
        paint.setStyle(Paint.Style.FILL);
    }

    // Météorite : boule de feu qui tombe en diagonale, puis grande explosion
    private void drawMeteor(Canvas canvas, GameModel model) {
        float tx = model.getMeteorX();
        float ty = model.getMeteorY();
        if (model.isMeteorFalling()) {
            float t = model.getMeteorFall();
            float sx = tx + board.playWidth * 0.6f;
            float sy = -120 * density;
            float x = sx + (tx - sx) * t;
            float y = sy + (ty - sy) * t;
            float r = (26 + 30 * t) * density;
            // Ombre au sol qui grandit
            paint.setColor(Color.argb((int) (120 * t), 0, 0, 0));
            canvas.drawOval(tx - r * 1.4f, ty - r * 0.4f, tx + r * 1.4f, ty + r * 0.4f, paint);
            // Traînée
            for (int i = 1; i <= 6; i++) {
                float k = i * 0.06f;
                paint.setColor(Color.argb(150 - i * 20, 255, 160 - i * 15, 40));
                canvas.drawCircle(x + (sx - tx) * k, y + (sy - ty) * k, r * (1 - i * 0.12f), paint);
            }
            paint.setColor(Palette.METEOR);
            canvas.drawCircle(x, y, r, paint);
            paint.setColor(Color.rgb(255, 230, 140));
            canvas.drawCircle(x - r * 0.2f, y - r * 0.2f, r * 0.55f, paint);
            paint.setColor(Color.rgb(110, 60, 40));
            canvas.drawCircle(x + r * 0.3f, y + r * 0.25f, r * 0.22f, paint);
        }
        float b = model.getMeteorBlast();
        if (b > 0) {
            float p = 1 - b;
            paint.setColor(Color.argb((int) (200 * b), 255, 200, 90));
            canvas.drawCircle(tx, ty, board.playWidth * (0.2f + 0.6f * p), paint);
            paint.setColor(Color.argb((int) (140 * b), 255, 255, 230));
            canvas.drawRect(0, 0, board.width, board.height, paint);
        }
    }

    // Nom de la compétence lancée, au-dessus du terrain
    private void drawSkillText(Canvas canvas, GameModel model) {
        float t = model.getSkillText();
        if (t <= 0) {
            return;
        }
        int alpha = (int) (255 * Math.min(1f, t * 3));
        int c = Palette.ELEMENT_COLORS[model.getSkillTextType()];
        float grow = t > 0.85f ? 1 + (t - 0.85f) * 2 : 1;
        ui.text(canvas, model.getSkillTextString(), board.centerX(), 205 * density + board.topShift, 22 * grow,
                Color.argb(alpha, Color.red(c), Color.green(c), Color.blue(c)), Paint.Align.CENTER);
    }

    // Boutons ronds de la partie : vitesse, mode automatique, soin, mage au hasard
    private void drawGameButtons(Canvas canvas, GameModel model, int pressed) {
        // Vitesse ×1 / ×2 / ×3
        // (grisé tant qu'aucune vitesse n'est achetée au Marché)
        RectF s = hud.speedButton;
        int speed = model.getGameSpeed();
        boolean speedUnlocked = model.getProgression().maxSpeed() > 1;
        ui.medallion(canvas, s.centerX(), s.centerY(), s.width() / 2,
                !speedUnlocked ? Palette.DISABLED : speed > 1 ? Palette.MARKET : Palette.MENU_BUTTON,
                speedUnlocked && pressed == Hud.GAME_SPEED, 215);
        ui.text(canvas, "×" + speed, s.centerX(), s.centerY() + 6 * density + sink(pressed == Hud.GAME_SPEED), 16,
                Color.WHITE, Paint.Align.CENTER);

        // Mode automatique (ultime et compétences) : grisé tant qu'il n'est pas acheté au Marché
        RectF a = hud.autoButton;
        boolean auto = model.isAutoMode();
        boolean autoOwned = model.getProgression().hasAuto();
        ui.medallion(canvas, a.centerX(), a.centerY(), a.width() / 2,
                !autoOwned ? Palette.DISABLED : auto ? Palette.RESUME : Palette.MENU_BUTTON,
                autoOwned && pressed == Hud.GAME_AUTO, 215);
        ui.text(canvas, "AUTO", a.centerX(), a.centerY() + 4 * density + sink(pressed == Hud.GAME_AUTO), 11,
                auto ? Color.WHITE : Palette.TEXT_INACTIVE, Paint.Align.CENTER);

        // Soin du château (3 au maximum)
        RectF h = hud.healButton;
        boolean canHeal = model.canHeal();
        boolean healsLeft = model.getHealsLeft() > 0;
        ui.medallion(canvas, h.centerX(), h.centerY(), h.width() / 2, canHeal ? Palette.HEAL : Palette.DISABLED,
                canHeal && pressed == Hud.GAME_HEAL, 215);
        float hy = h.centerY() + sink(canHeal && pressed == Hud.GAME_HEAL);
        paint.setColor(canHeal ? Color.WHITE : Palette.ICON_DISABLED);
        float arm = 4 * density;
        float len = 12 * density;
        canvas.drawRect(h.centerX() - arm, hy - len, h.centerX() + arm, hy + len, paint);
        canvas.drawRect(h.centerX() - len, hy - arm, h.centerX() + len, hy + arm, paint);
        drawPriceTag(canvas, h, healsLeft ? String.valueOf(model.getHealCost()) : "—", canHeal,
                model.getHealsLeft() + "/" + GameModel.MAX_HEALS);

        // Le joueur se prépare : grand bouton « Lancer la vague »
        if (model.isWaitingForWave()) {
            RectF l = hud.launchButton;
            float pulse = 1 + 0.04f * (float) Math.sin(frame * 0.12);
            tmpRect.set(l.centerX() - l.width() / 2 * pulse, l.centerY() - l.height() / 2 * pulse,
                    l.centerX() + l.width() / 2 * pulse, l.centerY() + l.height() / 2 * pulse);
            drawPillButton(canvas, tmpRect, "LANCER LA VAGUE " + model.getWave(), Palette.RESUME,
                    pressed == Hud.GAME_LAUNCH);
            ui.text(canvas, "Placez vos mages, puis lancez", l.centerX(), l.bottom + 22 * density, 13,
                    Color.WHITE, Paint.Align.CENTER);
        }
    }

    // Prix en PO sous un bouton rond (et petite mention à droite, ex. soins restants)
    private void drawPriceTag(Canvas canvas, RectF b, String price, boolean can, String extra) {
        float y = b.bottom + 14 * density;
        ui.coin(canvas, b.centerX() - 9 * density, y - 4.5f * density, 5.5f * density);
        ui.text(canvas, price, b.centerX() - 1 * density, y, 12, can ? Palette.GOLD : Palette.TEXT_INACTIVE,
                Paint.Align.LEFT);
        if (extra != null) {
            ui.text(canvas, extra, b.right + 2 * density, b.top + 10 * density, 10, Color.WHITE, Paint.Align.LEFT);
        }
    }

    // Dé : carré arrondi blanc avec 5 points
    private void drawDice(Canvas canvas, float cx, float cy, float s, boolean enabled) {
        tmpRect.set(cx - s, cy - s, cx + s, cy + s);
        paint.setColor(enabled ? Color.WHITE : Palette.ICON_DISABLED);
        canvas.drawRoundRect(tmpRect, s * 0.3f, s * 0.3f, paint);
        paint.setColor(UiKit.OUTLINE);
        float o = s * 0.5f;
        float pr = s * 0.17f;
        canvas.drawCircle(cx, cy, pr, paint);
        canvas.drawCircle(cx - o, cy - o, pr, paint);
        canvas.drawCircle(cx + o, cy - o, pr, paint);
        canvas.drawCircle(cx - o, cy + o, pr, paint);
        canvas.drawCircle(cx + o, cy + o, pr, paint);
    }

    // Lune (niveau de nuit)
    private void drawMoon(Canvas canvas, float cx, float cy, float r) {
        paint.setColor(UiKit.OUTLINE);
        canvas.drawCircle(cx, cy, r + 1.5f * density, paint);
        paint.setColor(Palette.NIGHT_LEVEL);
        canvas.drawCircle(cx, cy, r, paint);
        paint.setColor(UiKit.OUTLINE);
        canvas.drawCircle(cx + r * 0.45f, cy - r * 0.3f, r * 0.8f, paint);
    }

    // Bandeau « succès débloqué » en haut de l'écran
    private void drawToast(Canvas canvas, GameModel model) {
        int id = model.getToastId();
        if (id < 0) {
            return;
        }
        float t = model.getToast(); // 1 → 0
        float slide = t > 0.9f ? (1 - t) * 10 : t < 0.1f ? t * 10 : 1; // descend, reste, remonte
        float h = 58 * density;
        float top = board.topShift + 8 * density - (1 - slide) * (h + 20 * density);
        tmpRect.set(board.playLeft + 16 * density, top, board.playRight() - 16 * density, top + h);
        paint.setColor(UiKit.OUTLINE);
        canvas.drawRoundRect(tmpRect, 12 * density, 12 * density, paint);
        tmpRect.inset(2 * density, 2 * density);
        paint.setColor(UiKit.GOLD_DARK);
        canvas.drawRoundRect(tmpRect, 10 * density, 10 * density, paint);
        tmpRect.inset(2 * density, 2 * density);
        paint.setColor(UiKit.SLATE_TOP);
        canvas.drawRoundRect(tmpRect, 9 * density, 9 * density, paint);
        drawStar(canvas, tmpRect.left + 22 * density, tmpRect.centerY(), 13 * density, true);
        float tx = tmpRect.left + 44 * density;
        float maxW = tmpRect.right - tx - 64 * density;
        int tier = model.getToastTier();
        ui.textPx(canvas, "Succès : " + Achievement.name(id, tier), tx, tmpRect.centerY() - 3 * density, 15 * density,
                Palette.ACHIEVEMENT, Paint.Align.LEFT, maxW);
        ui.textPx(canvas, Achievement.description(id, tier), tx, tmpRect.centerY() + 14 * density, 11 * density,
                Color.WHITE, Paint.Align.LEFT, maxW);
        drawGem(canvas, tmpRect.right - 46 * density, tmpRect.centerY() - 1 * density, 8 * density);
        ui.text(canvas, "+" + Achievement.reward(id, tier), tmpRect.right - 34 * density, tmpRect.centerY() + 6 * density,
                16, Palette.GEM, Paint.Align.LEFT);
    }

    // Coupe un texte en 2 lignes s'il dépasse maxW (au mot le plus proche du milieu, jamais juste avant
    // un « % »). Retourne 1 ligne s'il tient, sinon 2 (chacune est ensuite réduite si besoin par textPx).
    private String[] splitTwoLines(String s, float sizePx, float maxW) {
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD); // même police que UiKit.textPx
        paint.setTextSize(sizePx);
        float w = paint.measureText(s);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        if (w <= maxW) {
            return new String[]{s};
        }
        int mid = s.length() / 2;
        int best = -1;
        for (int i = 0; i < s.length(); i++) {
            boolean breakable = s.charAt(i) == ' ' && i + 1 < s.length() && s.charAt(i + 1) != '%';
            if (breakable && (best < 0 || Math.abs(i - mid) < Math.abs(best - mid))) {
                best = i;
            }
        }
        if (best < 0) {
            return new String[]{s};
        }
        return new String[]{s.substring(0, best), s.substring(best + 1)};
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
        drawGame(canvas, model, touch);

        paint.setColor(Color.argb(160, 0, 0, 0));
        canvas.drawRect(0, 0, board.width, board.height, paint);

        // Fenêtre
        RectF p = hud.pausePopup;
        ui.panel(canvas, p, "PAUSE", UiKit.RIBBON);
        // Partie reprise après la fermeture de l'appli : elle repart au début de la vague
        if (model.isResumed()) {
            ui.text(canvas, "Partie reprise · vague " + model.getWave(), board.centerX(), p.top + 60 * density, 13,
                    Palette.GOLD, Paint.Align.CENTER);
        }

        int pressed = touch.getPressedMenuButton();

        // QUITTER
        drawPillButton(canvas, hud.pauseMenuButtons[Hud.PAUSE_QUIT], "QUITTER", Palette.QUIT,
                pressed == Hud.PAUSE_QUIT);

        // Vibrations Non / Oui
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, "Vibrations", p.top + Hud.PAUSE_VIBRATION_Y * density, 17);
        RectF vib = hud.pauseMenuButtons[Hud.PAUSE_VIBRATION];
        if (pressed == Hud.PAUSE_VIBRATION) {
            drawPressedOverlay(canvas, vib, 12 * density);
        }
        drawToggle(canvas, vib, model.isVibrationEnabled());

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
            drawHome(canvas, model, touch, -1); // -1 : aucun bouton de l'accueil n'est appuyé
        } else {
            drawGame(canvas, model, touch);
        }

        paint.setColor(Color.argb(160, 0, 0, 0));
        canvas.drawRect(0, 0, board.width, board.height, paint);

        // Fenêtre
        RectF p = hud.optionsPopup;
        ui.panel(canvas, p, "OPTIONS", UiKit.RIBBON);
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, "Vibrations", p.top + Hud.OPT_VIBRATION_Y * density, 18);

        int pressed = touch.getPressedMenuButton();

        // Interrupteur Non / Oui
        RectF row = hud.optionsButtons[Hud.OPTIONS_VIBRATION];
        if (pressed == Hud.OPTIONS_VIBRATION) {
            drawPressedOverlay(canvas, row, 12 * density);
        }
        drawToggle(canvas, row, model.isVibrationEnabled());

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

        // Effets des attaques : Non = dessin minimal (téléphones modestes)
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, "Effets des attaques", p.top + Hud.OPT_EFFECTS_Y * density, 18);
        RectF fxRow = hud.optionsButtons[Hud.OPTIONS_EFFECTS];
        if (pressed == Hud.OPTIONS_EFFECTS) {
            drawPressedOverlay(canvas, fxRow, 12 * density);
        }
        drawToggle(canvas, fxRow, model.isAttackEffectsEnabled());

        // Volumes internes du jeu (indépendants du volume du téléphone)
        // Pendant le glissement, on affiche la position du doigt ; sinon, le volume réel
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, "Volume de la musique", p.top + Hud.OPT_MUSIC_VOLUME_Y * density, 18);
        boolean dragMusic = touch.isDraggingMusicVolume();
        drawVolumeSlider(canvas, hud.musicVolumeTrack, dragMusic ? touch.getDragVolume() : model.getMusicVolume(),
                dragMusic);
        paint.setColor(Color.WHITE);
        drawCenteredText(canvas, "Volume des bruitages", p.top + Hud.OPT_VOLUME_Y * density, 18);
        boolean dragSound = touch.isDraggingSoundVolume();
        drawVolumeSlider(canvas, hud.volumeTrack, dragSound ? touch.getDragVolume() : model.getSoundVolume(),
                dragSound);

        // CODE (seulement depuis l'accueil)
        if (model.getOptionsReturnState() == GameState.START) {
            drawPillButton(canvas, hud.optionsButtons[Hud.OPTIONS_CODE], "CODE", Palette.MARKET,
                    pressed == Hud.OPTIONS_CODE);
        }

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
        drawGame(canvas, model, touch);

        paint.setColor(Color.argb(160, 0, 0, 0));
        canvas.drawRect(0, 0, board.width, board.height, paint);

        RectF p = hud.victoryPopup;
        ui.panel(canvas, p, "VICTOIRE", Palette.RESUME);
        ui.text(canvas, "Niveau " + model.getLevel() + " terminé", board.centerX(), p.top + 70 * density, 16,
                Palette.GOLD, Paint.Align.CENTER);
        // Coffres gagnés, sous les étoiles (juste au-dessus des boutons)
        if (stateFrames >= 60) { // après l'apparition des étoiles
            drawChestsEarned(canvas, model, p.top + 186 * density);
            // Niveau de boss en 3 étoiles : 10 % de chances d'artefact
            int art = model.getLastArtifact();
            if (art != Artifact.NONE) {
                float pulse = 0.5f + 0.5f * (float) Math.sin(frame * 0.12);
                String effect = Palette.ELEMENT_NAMES[Artifact.typeOf(art)] + " · " + Artifact.DESCRIPTIONS[art];
                String[] lines = splitTwoLines(effect, 11 * density, p.width() - 24 * density);
                float nameY = lines.length > 1 ? 210 : 214; // remonte un peu si l'effet tient sur 2 lignes
                ui.textPx(canvas, "Artefact : " + Artifact.NAMES[art], board.centerX(), p.top + nameY * density,
                        15 * density, UiKit.tint(Palette.ARTIFACT, 0.3f * pulse), Paint.Align.CENTER,
                        p.width() - 24 * density);
                for (int li = 0; li < lines.length; li++) {
                    ui.textPx(canvas, lines[li], board.centerX(), p.top + (nameY + 16 + li * 13) * density,
                            (lines.length > 1 ? 10.5f : 11) * density, Color.WHITE, Paint.Align.CENTER,
                            p.width() - 24 * density);
                }
            } else if (model.getLastArtifactGems() > 0) {
                ui.text(canvas, "Tous les artefacts : +" + model.getLastArtifactGems() + " gemmes", board.centerX(),
                        p.top + 222 * density, 14, Palette.GEM, Paint.Align.CENTER);
            } else if (model.isLastArtifactRolled()) {
                ui.text(canvas, "Pas d'artefact cette fois (10 % de chances)", board.centerX(),
                        p.top + 222 * density, 12, Palette.TEXT_INACTIVE, Paint.Align.CENTER);
            } else if (model.getSpec().hasBossLevel()) {
                ui.text(canvas, "Artefact possible seulement en 3 étoiles", board.centerX(),
                        p.top + 222 * density, 12, Palette.TEXT_INACTIVE, Paint.Align.CENTER);
            }
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
        drawPillButton(canvas, hud.victoryButtons[Hud.VICTORY_RESTART], "REJOUER",
                Palette.MENU_BUTTON, pressed == Hud.VICTORY_RESTART);
        drawPillButton(canvas, hud.victoryButtons[Hud.VICTORY_MENU], "MENU",
                Palette.MENU_BUTTON, pressed == Hud.VICTORY_MENU);
    }

    // Grand bouton : tuile en relief avec un texte centré
    private void drawPillButton(Canvas canvas, RectF b, String text, int color, boolean pressed) {
        boolean enabled = color != Palette.DISABLED;
        ui.labelButton(canvas, b, text, color, pressed && enabled, enabled, 19);
    }

    // Curseur de volume : haut-parleurs aux deux bouts, rail rempli jusqu'au bouton
    private void drawVolumeSlider(Canvas canvas, RectF t, float volume, boolean dragging) {
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

        boolean muted = model.getSoundVolume() <= 0 && model.getMusicVolume() <= 0;
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

    // Coffres gagnés : « +2 petits · +1 grand » avec un coffre devant, centré dans la colonne
    private static final String[][] CHEST_WORDS = {{"petit", "petits"}, {"grand", "grands"},
            {"mystérieux", "mystérieux"}};

    private void drawChestsEarned(Canvas canvas, GameModel model, float y) {
        if (model.getLastChestsEarned() <= 0) {
            paint.setColor(Palette.TEXT_INACTIVE);
            drawCenteredText(canvas, "Aucun coffre cette fois", y, 13);
            return;
        }
        StringBuilder sb = new StringBuilder();
        int best = Chest.SMALL;
        for (int c = 0; c < Chest.COUNT; c++) {
            int k = model.getLastChestsEarned(c);
            if (k > 0) {
                if (sb.length() > 0) {
                    sb.append(" · ");
                }
                sb.append('+').append(k).append(' ').append(CHEST_WORDS[c][k > 1 ? 1 : 0]);
                best = c;
            }
        }
        String text = sb.toString();
        paint.setTextSize(16 * density);
        float textW = Math.min(paint.measureText(text), board.playWidth * 0.7f);
        float icon = 10 * density;
        float total = icon * 2 + 8 * density + textW;
        float x = board.centerX() - total / 2;
        drawChestOfType(canvas, x + icon, y - 6 * density, icon, 0, best, false);
        ui.textPx(canvas, text, x + icon * 2 + 8 * density, y, 16 * density, Palette.GOLD, Paint.Align.LEFT,
                board.playWidth * 0.7f);
    }

    // Coffre : caisse en bois, couvercle (qui se soulève de `open` entre 0 et 1), bande et serrure dorées
    // (cx, cy) : centre du haut de la caisse ; s : demi-largeur ; open : 0 fermé → 1 ouvert
    private void drawChest(Canvas canvas, float cx, float cy, float s, float open) {
        drawChest(canvas, cx, cy, s, open, spritePaint);
    }

    private void drawChest(Canvas canvas, float cx, float cy, float s, float open, Paint chestPaint) {
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
        canvas.drawBitmap(chestSprites[1], null, tmpRect, chestPaint);
        // Couvercle : se soulève et bascule vers l'arrière (charnière à gauche)
        canvas.save();
        if (open > 0) {
            canvas.translate(0, -open * s * 0.5f);
            canvas.rotate(-28 * open, cx - s, bodyTop);
        }
        tmpRect.set(cx - s, bodyTop - lidH + px, cx + s, bodyTop + px);
        canvas.drawBitmap(chestSprites[0], null, tmpRect, chestPaint);
        canvas.restore();
    }

    // =====================================================================
    //  Accueil : onglets et pages (Marché, Mages, Infini, Château)
    // =====================================================================
    private static final String[] TAB_NAMES = {"Marché", "Mages", "Campagne", "Infini", "Château"};

    // Barre d'onglets : l'onglet de la page affichée est en relief et doré
    private void drawTabBar(Canvas canvas, GameModel model, int pressed) {
        RectF bar = hud.tabBar;
        tmpRect.set(bar.left - 4 * density, bar.top - 4 * density, bar.right + 4 * density, bar.bottom + 4 * density);
        paint.setColor(UiKit.OUTLINE);
        canvas.drawRoundRect(tmpRect, 12 * density, 12 * density, paint);
        tmpRect.inset(2 * density, 2 * density);
        paint.setColor(UiKit.WOOD_DARK);
        canvas.drawRoundRect(tmpRect, 10 * density, 10 * density, paint);
        int page = model.getHomePage();
        for (int i = 0; i < GameModel.PAGE_COUNT; i++) {
            RectF b = hud.tabButtons[i];
            boolean sel = i == page;
            RectF f;
            if (sel) {
                f = ui.button(canvas, b, UiKit.GOLD_DARK, pressed == Hud.HOME_TAB_0 + i, true);
            } else {
                tmpRect.set(b);
                tmpRect.inset(2 * density, 4 * density);
                if (pressed == Hud.HOME_TAB_0 + i) {
                    paint.setColor(Color.argb(60, 255, 255, 255));
                    canvas.drawRoundRect(tmpRect, 8 * density, 8 * density, paint);
                }
                f = tmpRect;
            }
            float cx = f.centerX();
            float cy = f.centerY() - 7 * density;
            float labelY = f.bottom - 5 * density; // (l'icône réutilise tmpRect : on retient la position avant)
            drawTabIcon(canvas, i, cx, cy, (sel ? 13 : 11) * density, model);
            ui.textPx(canvas, TAB_NAMES[i], cx, labelY, (sel ? 11 : 10) * density,
                    sel ? Color.WHITE : Palette.TEXT_INACTIVE, Paint.Align.CENTER, b.width() - 4 * density);
        }
        // Pastille du Marché : coffres à ouvrir (ou « ! » : coffres gratuits du jour)
        int chests = model.getProgression().getChests();
        boolean daily = model.getProgression().canClaimDaily(todayLocal());
        if (chests > 0 || daily) {
            RectF u = hud.tabButtons[GameModel.PAGE_MARKET];
            drawBadge(canvas, u.right - 10 * density, u.top + 6 * density,
                    chests == 0 ? "!" : chests > 999 ? "999+" : String.valueOf(chests));
        }
    }

    private void drawBadge(Canvas canvas, float x, float y, String text) {
        paint.setColor(UiKit.OUTLINE);
        canvas.drawCircle(x, y, 12 * density, paint);
        paint.setColor(Palette.QUIT);
        canvas.drawCircle(x, y, 10.5f * density, paint);
        ui.textPx(canvas, text, x, y + 4 * density, (text.length() > 2 ? 8 : 11) * density, Color.WHITE,
                Paint.Align.CENTER, 20 * density);
    }

    private void drawTabIcon(Canvas canvas, int tab, float cx, float cy, float s, GameModel model) {
        switch (tab) {
            case GameModel.PAGE_MARKET:
                drawGem(canvas, cx, cy, s * 0.9f);
                break;
            case GameModel.PAGE_MAGES: {
                float sw = s * 2.2f;
                float sh = sw * Sprites.MAGE_HEIGHT / Sprites.MAGE_WIDTH;
                spriteRect.set(cx - sw / 2, cy - sh / 2, cx + sw / 2, cy + sh / 2);
                canvas.drawBitmap(mageSprites[Mage.FIRE][2], null, spriteRect, spritePaint);
                break;
            }
            case GameModel.PAGE_CAMPAIGN:
                drawCrown(canvas, cx, cy, s);
                break;
            case GameModel.PAGE_ENDLESS:
                ui.textPx(canvas, "∞", cx, cy + s * 0.7f, s * 2.2f, Palette.ENDLESS_TEXT, Paint.Align.CENTER, 0);
                break;
            case GameModel.PAGE_CASTLE:
                drawTower(canvas, cx, cy, s);
                break;
        }
    }

    // Tour de château : corps, créneaux et porte
    private void drawTower(Canvas canvas, float cx, float cy, float s) {
        paint.setColor(UiKit.OUTLINE);
        tmpRect.set(cx - s * 0.8f, cy - s * 0.6f, cx + s * 0.8f, cy + s);
        canvas.drawRect(tmpRect, paint);
        for (int i = 0; i < 3; i++) {
            float x = cx - s * 0.8f + i * s * 0.6f;
            canvas.drawRect(x, cy - s, x + s * 0.4f, cy - s * 0.5f, paint);
        }
        paint.setColor(Color.rgb(170, 170, 185));
        tmpRect.inset(s * 0.12f, s * 0.12f);
        canvas.drawRect(tmpRect, paint);
        for (int i = 0; i < 3; i++) {
            float x = cx - s * 0.8f + i * s * 0.6f + s * 0.1f;
            canvas.drawRect(x, cy - s * 0.9f, x + s * 0.2f, cy - s * 0.5f, paint);
        }
        paint.setColor(UiKit.WOOD);
        canvas.drawRect(cx - s * 0.25f, cy + s * 0.25f, cx + s * 0.25f, cy + s * 0.88f, paint);
    }

    // Titre de la page (ruban) et réserves : gemmes et coffres
    private void drawPageHeader(Canvas canvas, GameModel model, String title, int color) {
        float cx = board.centerX();
        ui.ribbon(canvas, cx, board.topShift + 52 * density, Math.min(board.playWidth * 0.62f, 230 * density),
                title, color);
        Progression prog = model.getProgression();
        String gems = String.valueOf(prog.getGems());
        String chests = String.valueOf(prog.getChests());
        float y = board.topShift + 96 * density;
        drawGem(canvas, cx - 70 * density, y - 5 * density, 8 * density);
        ui.text(canvas, gems, cx - 58 * density, y, 16, Palette.GEM, Paint.Align.LEFT);
        drawChest(canvas, cx + 24 * density, y - 9 * density, 10 * density, 0);
        ui.text(canvas, chests, cx + 40 * density, y, 16, Palette.GOLD, Paint.Align.LEFT);
    }

    // Page Mages : équipe en 2 × 2, collection, cartes à débloquer (la fiche s'ouvre au toucher)
    private void drawMagesPage(Canvas canvas, GameModel model, float scroll, int pressed) {
        drawPageHeader(canvas, model, "MAGES", UiKit.RIBBON);
        drawList(canvas, model, GameModel.PAGE_MAGES, hud.magesViewport, scroll, pressed);
    }

    // Pages Marché et Château : titre, réserves, liste qui défile
    private void drawListPage(Canvas canvas, GameModel model, int page, String title, int color, float scroll,
                              int pressed) {
        drawPageHeader(canvas, model, title, color);
        drawList(canvas, model, page, hud.listViewport, scroll, pressed);
    }

    // Liste qui défile : seules les lignes visibles sont dessinées, dans la zone de la liste
    private void drawList(Canvas canvas, GameModel model, int page, RectF vp, float scroll, int pressed) {
        menu.build(page, model);
        canvas.save();
        canvas.clipRect(vp.left - 4 * density, vp.top, vp.right + 4 * density, vp.bottom);
        for (int i = 0; i < menu.count; i++) {
            menu.rowRect(i, vp, scroll, rowRect);
            if (rowRect.bottom < vp.top || rowRect.top > vp.bottom) {
                continue;
            }
            int base = Hud.HOME_LIST_0 + i * 4;
            drawListRow(canvas, model, i, new RectF(rowRect), pressed - base);
        }
        canvas.restore();
        // Fondu en haut et en bas (le contenu glisse sous les bords) et barre de défilement
        float max = menu.maxScroll(vp.height());
        if (max > 0) {
            float trackH = vp.height();
            float thumbH = Math.max(30 * density, trackH * vp.height() / (vp.height() + max));
            float thumbTop = vp.top + (trackH - thumbH) * (scroll / max);
            paint.setColor(Color.argb(90, 255, 255, 255));
            tmpRect.set(vp.right + 1 * density, thumbTop, vp.right + 4 * density, thumbTop + thumbH);
            canvas.drawRoundRect(tmpRect, 2 * density, 2 * density, paint);
        }
    }

    // Une ligne de liste ; pressedButton = bouton appuyé dans cette ligne (BTN_xxx), sinon autre valeur
    private void drawListRow(Canvas canvas, GameModel model, int i, RectF r, int pressedButton) {
        Progression prog = model.getProgression();
        int a = menu.arg[i];
        boolean mainPressed = pressedButton == MenuList.BTN_MAIN;
        switch (menu.kind[i]) {
            case MenuList.HEADER:
                ui.text(canvas, menu.text[i], r.left + 6 * density, r.bottom - 10 * density, 15, Palette.GOLD,
                        Paint.Align.LEFT);
                paint.setColor(Color.argb(80, 240, 192, 64));
                canvas.drawRect(r.left + 6 * density, r.bottom - 4 * density, r.right - 6 * density,
                        r.bottom - 3 * density, paint);
                return;
            case MenuList.INFO:
                ui.textPx(canvas, menu.text[i], r.centerX(), r.centerY() + 4 * density, 11 * density,
                        Palette.TEXT_INACTIVE, Paint.Align.CENTER, r.width());
                return;
            case MenuList.TEAM_ROW:
            case MenuList.CARD_ROW:
            case MenuList.LOCKED_ROW:
                drawCardRow(canvas, model, i, r, pressedButton);
                return;
            case MenuList.SHOP_CHEST:
                drawChestShopRow(canvas, prog, i, a, r, pressedButton);
                return;
            case MenuList.ACHIEVEMENT:
                drawAchievementRow(canvas, model, a, r);
                return;
        }

        ui.inset(canvas, r);
        menu.buttonRect(i, MenuList.BTN_MAIN, r, btnRect);
        float iconCx = r.left + 36 * density;
        float iconCy = r.centerY();
        float tx = r.left + 74 * density;
        float maxW = btnRect.left - 8 * density - tx;
        String title = "";
        String desc = "";
        String status = "";
        int titleColor = Color.WHITE;
        int statusColor = Palette.TEXT_INACTIVE;

        switch (menu.kind[i]) {
            case MenuList.FREE_CHEST: {
                long day = todayLocal();
                boolean can = prog.canClaimDaily(day);
                drawChestOfType(canvas, iconCx, iconCy - 4 * density, 20 * density, 0, Chest.SMALL, can);
                title = "Coffres gratuits";
                titleColor = Palette.GOLD;
                desc = Progression.DAILY_FREE_CHESTS + " petits coffres chaque jour";
                status = can ? "Disponibles !" : "Revenez demain";
                statusColor = can ? Palette.TEAM : Palette.TEXT_INACTIVE;
                drawSmallButton(canvas, btnRect, can ? "PRENDRE" : "DEMAIN", can ? Palette.RESUME : Palette.DISABLED,
                        can && mainPressed, 13);
                break;
            }
            case MenuList.SHOP_ULTIMATE: {
                boolean owned = prog.ownsUltimate(a);
                drawUltimateIcon(canvas, a, iconCx, iconCy, 24 * density, owned);
                title = Ultimate.NAMES[a];
                titleColor = Palette.ULTIMATE_COLORS[a];
                desc = Ultimate.DESCRIPTIONS[a];
                if (owned) {
                    statusColor = Palette.TEAM;
                    status = prog.getEquippedUltimate() == a ? "Équipé" : "À équiper (page Château)";
                    drawSmallButton(canvas, btnRect, "ACQUIS", Palette.TEAM, false, 13);
                } else {
                    status = "Jauge : " + Ultimate.killsRequired(a) + " gobelins";
                    drawPriceButton(canvas, btnRect, Ultimate.price(a), prog.canBuyUltimate(a), mainPressed);
                }
                break;
            }
            case MenuList.SHOP_SPEED: {
                boolean owned = prog.hasSpeed(a);
                ui.medallion(canvas, iconCx, iconCy, 24 * density, owned ? Palette.MARKET : Palette.DISABLED, false, 255);
                ui.text(canvas, "×" + a, iconCx, iconCy + 7 * density, 19, Color.WHITE, Paint.Align.CENTER);
                title = "Vitesse ×" + a;
                titleColor = Palette.GOLD;
                desc = a == 2 ? "Le jeu va 2 fois plus vite" : "Le jeu va 3 fois plus vite";
                if (owned) {
                    statusColor = Palette.TEAM;
                    status = "Achat définitif · bouton en partie";
                    drawSmallButton(canvas, btnRect, "ACQUIS", Palette.TEAM, false, 13);
                } else {
                    boolean locked = a == 3 && !prog.hasSpeed2();
                    status = locked ? "Débloquez d'abord la vitesse ×2" : "Achat définitif";
                    drawPriceButton(canvas, btnRect, Progression.speedPrice(a), prog.canBuySpeed(a), mainPressed);
                }
                break;
            }
            case MenuList.SHOP_AUTO: {
                boolean owned = prog.hasAuto();
                ui.medallion(canvas, iconCx, iconCy, 24 * density, owned ? Palette.RESUME : Palette.DISABLED, false, 255);
                ui.text(canvas, "AUTO", iconCx, iconCy + 5 * density, 12, Color.WHITE, Paint.Align.CENTER);
                title = "Mode automatique";
                titleColor = Palette.GOLD;
                desc = "Ultime et compétences lancés tout seuls";
                if (owned) {
                    statusColor = Palette.TEAM;
                    status = "Achat définitif · bouton en partie";
                    drawSmallButton(canvas, btnRect, "ACQUIS", Palette.TEAM, false, 13);
                } else {
                    status = "Achat définitif";
                    drawPriceButton(canvas, btnRect, Progression.AUTO_PRICE, prog.canBuyAuto(), mainPressed);
                }
                break;
            }
            case MenuList.CASTLE_HP: {
                drawTower(canvas, iconCx, iconCy, 20 * density);
                int lv = prog.getCastleHpLevel();
                boolean max = lv >= Progression.CASTLE_HP_MAX_LEVEL;
                int now = Math.round(GameModel.CASTLE_MAX_HP * prog.castleHpMultiplier());
                int next = Math.round(GameModel.CASTLE_MAX_HP * (prog.castleHpMultiplier() + Progression.CASTLE_HP_PER_LEVEL));
                title = "Remparts renforcés";
                titleColor = Palette.CASTLE;
                desc = max ? "PV max : " + now : "PV max : " + now + " → " + next;
                status = "Niveau " + lv + " / " + Progression.CASTLE_HP_MAX_LEVEL;
                if (max) {
                    drawSmallButton(canvas, btnRect, "MAX", Palette.TEAM, false, 14);
                } else {
                    drawPriceButton(canvas, btnRect, prog.castleHpCost(), prog.canUpgradeCastleHp(), mainPressed);
                }
                break;
            }
            case MenuList.CASTLE_RES: {
                ui.medallion(canvas, iconCx, iconCy, 22 * density, Palette.SHIELD, false, 255);
                drawShieldGlyph(canvas, iconCx, iconCy, 12 * density);
                int lv = prog.getCastleResLevel();
                boolean max = lv >= Progression.CASTLE_RES_MAX_LEVEL;
                int pct = Math.round(Progression.CASTLE_RES_PER_LEVEL * lv * 100);
                title = "Pierres enchantées";
                titleColor = Palette.SHIELD;
                desc = max ? "Dégâts subis −" + pct + " %"
                        : "Dégâts subis −" + pct + " % → −" + (pct + Math.round(Progression.CASTLE_RES_PER_LEVEL * 100)) + " %";
                status = "Niveau " + lv + " / " + Progression.CASTLE_RES_MAX_LEVEL;
                if (max) {
                    drawSmallButton(canvas, btnRect, "MAX", Palette.TEAM, false, 14);
                } else {
                    drawPriceButton(canvas, btnRect, prog.castleResCost(), prog.canUpgradeCastleRes(), mainPressed);
                }
                break;
            }
            case MenuList.CASTLE_GOLD: {
                // Trésor de départ : pièces d'or en plus au début de chaque partie
                ui.medallion(canvas, iconCx, iconCy, 22 * density, UiKit.shade(Palette.GOLD, 0.55f), false, 255);
                ui.coin(canvas, iconCx, iconCy, 11 * density);
                int lv = prog.getCastleGoldLevel();
                boolean max = lv >= Progression.CASTLE_GOLD_MAX_LEVEL;
                int now = prog.startGoldBonus();
                title = "Trésor de départ";
                titleColor = Palette.GOLD;
                desc = max ? "+" + now + " PO en début de partie"
                        : "+" + now + " → +" + (now + Progression.CASTLE_GOLD_PER_LEVEL) + " PO en début de partie";
                status = "Niveau " + lv + " / " + Progression.CASTLE_GOLD_MAX_LEVEL + " · campagne et infini";
                if (max) {
                    drawSmallButton(canvas, btnRect, "MAX", Palette.TEAM, false, 14);
                } else {
                    drawPriceButton(canvas, btnRect, prog.castleGoldCost(), prog.canUpgradeCastleGold(), mainPressed);
                }
                break;
            }
            case MenuList.ULTIMATE: {
                boolean owned = prog.ownsUltimate(a);
                boolean equipped = prog.getEquippedUltimate() == a;
                drawUltimateIcon(canvas, a, iconCx, iconCy, 22 * density, owned);
                title = Ultimate.NAMES[a];
                titleColor = owned ? Palette.ULTIMATE_COLORS[a] : Palette.TEXT_INACTIVE;
                desc = Ultimate.DESCRIPTIONS[a];
                status = "Jauge : " + Ultimate.killsRequired(a) + " gobelins";
                if (equipped) {
                    paint.setColor(Palette.TEAM);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(2.5f * density);
                    canvas.drawRoundRect(r, 10 * density, 10 * density, paint);
                    paint.setStyle(Paint.Style.FILL);
                    drawSmallButton(canvas, btnRect, "ÉQUIPÉ", Palette.TEAM, false, 13);
                } else if (owned) {
                    drawSmallButton(canvas, btnRect, "ÉQUIPER", Palette.RESUME, mainPressed, 13);
                } else {
                    drawSmallButton(canvas, btnRect, "MARCHÉ", Palette.MARKET, mainPressed, 13);
                }
                break;
            }
        }
        boolean tall = r.height() > 80 * density;
        float y0 = r.top + (tall ? 28 : 24) * density;
        float step = (tall ? 20 : 18) * density;
        ui.textPx(canvas, title, tx, y0, 16 * density, titleColor, Paint.Align.LEFT, maxW);
        ui.textPx(canvas, desc, tx, y0 + step, 11.5f * density, Color.WHITE, Paint.Align.LEFT, maxW);
        ui.textPx(canvas, status, tx, y0 + 2 * step, 11 * density, statusColor, Paint.Align.LEFT, maxW);
    }

    // =====================================================================
    //  Cartes de mage, fiche d'un mage, coffres du Marché
    // =====================================================================

    private static long todayLocal() {
        long now = System.currentTimeMillis();
        return (now + java.util.TimeZone.getDefault().getOffset(now)) / 86_400_000L;
    }

    // Coffre teinté selon sa sorte (petit : bois, grand : doré, mystérieux : violet), avec une lueur
    private void drawChestOfType(Canvas canvas, float cx, float cy, float s, float open, int chest, boolean glow) {
        if (glow) {
            int c = Palette.CHEST_COLORS[chest];
            float pulse = 0.5f + 0.5f * (float) Math.sin(frame * 0.1 + chest);
            paint.setColor(Color.argb((int) (40 + 40 * pulse), Color.red(c), Color.green(c), Color.blue(c)));
            canvas.drawCircle(cx, cy + s * 0.2f, s * 1.35f, paint);
        }
        drawChest(canvas, cx, cy, s, open, chestPaints[chest]);
    }

    // Une rangée de cartes (équipe, collection, à débloquer)
    private void drawCardRow(Canvas canvas, GameModel model, int i, RectF r, int pressedButton) {
        Progression prog = model.getProgression();
        int k = menu.kind[i];
        int n = k == MenuList.TEAM_ROW ? 2 : MenuList.CARDS_PER_ROW;
        boolean swapping = model.getSwappingMage() >= 0;
        for (int b = 0; b < n; b++) {
            menu.buttonRect(i, b, r, cellRect);
            int type = menu.cardType(i, b, model);
            if (k == MenuList.TEAM_ROW) {
                ui.inset(canvas, cellRect);
                if (swapping) {
                    // Choix de l'emplacement : les cartes de l'équipe se dandinent, cadre doré
                    float pulse = 0.5f + 0.5f * (float) Math.sin(frame * 0.2);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(3 * density);
                    paint.setColor(Color.argb((int) (150 + 100 * pulse), 255, 215, 90));
                    canvas.drawRoundRect(cellRect, 10 * density, 10 * density, paint);
                    paint.setStyle(Paint.Style.FILL);
                }
            }
            if (type < 0) {
                if (k == MenuList.TEAM_ROW) {
                    ui.text(canvas, "Vide", cellRect.centerX(), cellRect.centerY(), 14, Palette.TEXT_INACTIVE,
                            Paint.Align.CENTER);
                }
                continue;
            }
            float wiggle = k == MenuList.TEAM_ROW && swapping ? (float) Math.sin(frame * 0.35 + b) * 2.5f : 0;
            canvas.save();
            canvas.rotate(wiggle, cellRect.centerX(), cellRect.centerY());
            drawMiniCard(canvas, prog, type, cellRect, k == MenuList.LOCKED_ROW, k == MenuList.TEAM_ROW);
            canvas.restore();
            if (pressedButton == b) {
                drawPressedOverlay(canvas, cellRect, 10 * density);
            }
        }
    }

    // Carte d'un mage dans une case : la carte (rang), puis la barre de cartes vers le rang suivant
    private void drawMiniCard(Canvas canvas, Progression prog, int type, RectF cell, boolean locked, boolean team) {
        float pad = team ? 10 * density : 2 * density;
        float barH = 16 * density;
        float maxH = cell.height() - barH - 14 * density - pad * 2;
        float w = Math.min(cell.width() - pad * 2, maxH / 1.32f);
        float h = w * 1.32f;
        float cx = cell.centerX();
        float cy = cell.top + pad + h / 2;
        int rarity = Rarity.of(type);
        // Liseré de la couleur de la rareté
        paint.setColor(Palette.RARITY_COLORS[rarity]);
        tmpRect.set(cx - w / 2 - 3 * density, cy - h / 2 - 3 * density, cx + w / 2 + 3 * density, cy + h / 2 + 3 * density);
        canvas.drawRoundRect(tmpRect, w * 0.11f, w * 0.11f, paint);
        drawCardFace(canvas, cx, cy, w, h, type, locked ? "?" : "Rang " + prog.getRank(type), 1);
        float barTop = cy + h / 2 + 8 * density;
        tmpRect.set(cx - w / 2, barTop, cx + w / 2, barTop + barH);
        if (locked) {
            // Pas encore débloqué : carte assombrie
            paint.setColor(Color.argb(150, 10, 8, 20));
            canvas.drawRoundRect(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2, w * 0.09f, w * 0.09f, paint);
            drawLock(canvas, cx, cy - h * 0.12f, w * 0.12f);
            ui.textPx(canvas, "À débloquer", cx, barTop + barH - 3 * density, 11 * density, Palette.TEXT_INACTIVE,
                    Paint.Align.CENTER, cell.width());
            return;
        }
        int cards = prog.getFragments(type);
        int need = prog.upgradeCost(type);
        boolean ready = prog.hasCardsToUpgrade(type);
        ui.bar(canvas, tmpRect, Math.min(1f, cards / (float) need), ready ? Palette.TEAM : Palette.RARITY_COLORS[rarity]);
        ui.textPx(canvas, ready ? "↑ " + cards + "/" + need : cards + "/" + need, cx, barTop + barH - 3.5f * density,
                10 * density, Color.WHITE, Paint.Align.CENTER, w);
    }

    // Fiche d'un mage : carte, rareté, bonus du rang, cartes, compétence, artefact, AMÉLIORER / UTILISER
    private void drawMageSheet(Canvas canvas, GameModel model, int type, int pressed) {
        Progression prog = model.getProgression();
        paint.setColor(Color.argb(170, 0, 0, 0));
        canvas.drawRect(0, 0, board.width, board.height, paint);
        RectF p = hud.mageSheet;
        int rarity = Rarity.of(type);
        boolean owned = prog.isOwned(type);
        int rank = prog.getRank(type);
        int ec = Palette.ELEMENT_COLORS[type];
        ui.panel(canvas, p, Palette.ELEMENT_NAMES[type].toUpperCase(), UiKit.shade(Palette.RARITY_COLORS[rarity], 0.7f));

        RectF c = hud.sheetCard;
        drawCardFace(canvas, c.centerX(), c.centerY(), c.width(), c.height(), type, owned ? "Rang " + rank : "?", 1);
        if (!owned) {
            paint.setColor(Color.argb(150, 10, 8, 20));
            canvas.drawRoundRect(c, c.width() * 0.09f, c.width() * 0.09f, paint);
            drawLock(canvas, c.centerX(), c.centerY(), c.width() * 0.14f);
        }

        float tx = c.right + 14 * density;
        float maxW = p.right - 16 * density - tx;
        float y = c.top + 14 * density;
        ui.textPx(canvas, Rarity.NAMES[rarity], tx, y, 15 * density, Palette.RARITY_COLORS[rarity], Paint.Align.LEFT, maxW);
        ui.textPx(canvas, Palette.ELEMENT_DESCRIPTIONS[type], tx, y + 20 * density, 11 * density, Color.WHITE,
                Paint.Align.LEFT, maxW);
        String bonus = String.format(java.util.Locale.FRANCE, "Dégâts ×%.1f · Cadence +%d %%",
                Progression.damageMultiplier(rank), Math.round(Progression.speedBonus(rank) * 100));
        ui.textPx(canvas, bonus, tx, y + 40 * density, 11 * density, Palette.TEXT_INACTIVE, Paint.Align.LEFT, maxW);
        // Cartes vers le rang suivant
        int cards = prog.getFragments(type);
        int need = prog.upgradeCost(type);
        tmpRect.set(tx, y + 52 * density, tx + maxW, y + 70 * density);
        ui.bar(canvas, tmpRect, owned ? Math.min(1f, cards / (float) need) : 0,
                prog.hasCardsToUpgrade(type) ? Palette.TEAM : Palette.RARITY_COLORS[rarity]);
        ui.textPx(canvas, owned ? "Cartes " + cards + " / " + need : "Aucune carte", tx + maxW / 2, y + 65 * density,
                10.5f * density, Color.WHITE, Paint.Align.CENTER, maxW);
        // Compétence de niveau 5
        ui.textPx(canvas, "Niv. 5 : " + Mage.SKILL_NAMES[type], tx, y + 92 * density, 12 * density,
                UiKit.tint(ec, 0.35f), Paint.Align.LEFT, maxW);
        ui.textPx(canvas, Mage.SKILL_DESCRIPTIONS[type], tx, y + 108 * density, 10 * density, Palette.TEXT_INACTIVE,
                Paint.Align.LEFT, maxW);
        ui.textPx(canvas, "(toucher le mage en partie)", tx, y + 122 * density, 10 * density, Palette.TEXT_INACTIVE,
                Paint.Align.LEFT, maxW);

        // Artefact : toucher pour changer
        RectF a = hud.sheetArtifact;
        int ownedArt = prog.artifactCount(type);
        int art = prog.getEquippedArtifact(type);
        if (owned && ownedArt > 0) {
            RectF f = ui.button(canvas, a, UiKit.shade(Palette.ARTIFACT, 0.6f), pressed == Hud.HOME_SHEET_ARTIFACT, true);
            String label = art == Artifact.NONE ? "Aucun artefact (" + ownedArt + "/3) · toucher pour changer"
                    : Artifact.NAMES[art] + " · " + Artifact.DESCRIPTIONS[art];
            // Nom + effet : sur 2 lignes si la phrase est trop longue pour une seule
            float size = 11.5f * density;
            String[] lines = splitTwoLines(label, size, f.width() - 10 * density);
            if (lines.length > 1) {
                size = 10.5f * density;
                lines = splitTwoLines(label, size, f.width() - 10 * density);
            }
            float lineH = size * 1.2f;
            float y0 = f.centerY() + 4 * density - (lines.length - 1) * lineH / 2;
            for (int li = 0; li < lines.length; li++) {
                ui.textPx(canvas, lines[li], f.centerX(), y0 + li * lineH, size,
                        art == Artifact.NONE ? Palette.TEXT_INACTIVE : Color.WHITE, Paint.Align.CENTER,
                        f.width() - 10 * density);
            }
        } else {
            paint.setColor(Color.argb(90, 0, 0, 0));
            canvas.drawRoundRect(a, 8 * density, 8 * density, paint);
            ui.textPx(canvas, "Artefact : 0/3 · niveaux de boss en 3 étoiles (10 %)", a.centerX(), a.centerY() + 4 * density,
                    11 * density, Palette.TEXT_INACTIVE, Paint.Align.CENTER, a.width() - 10 * density);
        }

        if (owned) {
            boolean maxed = rank >= Progression.MAX_RANK;
            boolean can = prog.canUpgrade(type);
            String cost = maxed ? "Rang maximal" : need + " cartes · " + prog.upgradeGemCost(type) + " gemmes";
            if (!maxed && prog.hasCardsToUpgrade(type) && !can) {
                cost = "Il manque " + (prog.upgradeGemCost(type) - prog.getGems()) + " gemmes";
            }
            drawTwoLineButton(canvas, hud.sheetUpgrade, "AMÉLIORER", cost, can ? Palette.RESUME : Palette.DISABLED,
                    can && pressed == Hud.HOME_SHEET_UPGRADE);
            boolean inTeam = prog.isInTeam(type);
            drawTwoLineButton(canvas, hud.sheetUse, inTeam ? "DANS L'ÉQUIPE" : "UTILISER",
                    inTeam ? "Déjà emmené en partie" : prog.isTeamFull() ? "Remplace un mage" : "Rejoint l'équipe",
                    inTeam ? Palette.DISABLED : Palette.TEAM, !inTeam && pressed == Hud.HOME_SHEET_USE);
        } else {
            RectF u = hud.sheetUpgrade;
            ui.textPx(canvas, "À débloquer : trouvez une carte dans un coffre", p.centerX(), u.centerY() - 4 * density,
                    13 * density, Palette.GOLD, Paint.Align.CENTER, p.width() - 24 * density);
            ui.textPx(canvas, rarity >= Rarity.EPIC ? "Grand coffre ou coffre mystérieux (Marché)"
                            : "Tous les coffres (Marché)", p.centerX(), u.centerY() + 16 * density,
                    11 * density, Palette.TEXT_INACTIVE, Paint.Align.CENTER, p.width() - 24 * density);
        }
        drawPillButton(canvas, hud.sheetClose, "FERMER", Palette.MENU_BUTTON, pressed == Hud.HOME_SHEET_CLOSE);
    }

    // Marché : une sorte de coffre (contenu, probabilités, réserve, achat ×1 / ×5 / ×10 ou ouverture)
    private void drawChestShopRow(Canvas canvas, Progression prog, int i, int chest, RectF r, int pressedButton) {
        ui.inset(canvas, r);
        int stock = prog.getChests(chest);
        float iconCx = r.left + 42 * density;
        float iconCy = r.top + 46 * density;
        float wiggle = stock > 0 && (frame % 120) < 24 ? (float) Math.sin(frame * 0.8) * 6 : 0;
        canvas.save();
        canvas.rotate(wiggle, iconCx, iconCy + 16 * density);
        drawChestOfType(canvas, iconCx, iconCy - 6 * density, (20 + 3 * chest) * density, 0, chest, true);
        canvas.restore();
        float tx = r.left + 84 * density;
        float maxW = r.right - 10 * density - tx;
        ui.textPx(canvas, Chest.NAMES[chest], tx, r.top + 24 * density, 16 * density, Palette.CHEST_COLORS[chest],
                Paint.Align.LEFT, maxW);
        ui.textPx(canvas, Chest.cards(chest) + " cartes (± " + Chest.spread(chest) + ") · " + Chest.slots(chest)
                        + " types · " + Chest.gemsMin(chest) + " à " + Chest.gemsMax(chest) + " gemmes",
                tx, r.top + 42 * density, 11 * density, Color.WHITE, Paint.Align.LEFT, maxW);
        String status = stock > 0 ? "En réserve : " + stock + " (achat possible une fois vide)"
                : "Prix : " + Chest.price(chest) + " gemmes l'unité";
        ui.textPx(canvas, status, tx, r.top + 60 * density, 11 * density, stock > 0 ? Palette.GOLD : Palette.TEXT_INACTIVE,
                Paint.Align.LEFT, maxW);
        // Probabilités de chaque type de carte
        for (int s = 0; s < Chest.slots(chest); s++) {
            ui.textPx(canvas, "Carte " + (s + 1) + " : " + Chest.oddsText(chest, s), r.left + 14 * density,
                    r.top + (84 + 17 * s) * density, 10.5f * density, UiKit.tint(Palette.CHEST_COLORS[chest], 0.5f),
                    Paint.Align.LEFT, r.width() - 28 * density);
        }
        // Boutons
        int n = menu.buttons[i];
        for (int b = 0; b < n; b++) {
            menu.buttonRect(i, b, r, btnRect);
            boolean pressed = pressedButton == b;
            if (stock > 0) {
                drawTwoLineButton(canvas, btnRect, b == 0 ? "OUVRIR" : "TOUT OUVRIR",
                        b == 0 ? "1 coffre" : stock + (stock > 1 ? " coffres" : " coffre"), Palette.RESUME, pressed);
            } else {
                int count = b == 0 ? 1 : b == 1 ? 5 : 10;
                boolean can = prog.canBuyChests(chest, count);
                drawTwoLineButton(canvas, btnRect, "×" + count, (Chest.price(chest) * count) + " gemmes",
                        can ? Palette.MARKET : Palette.DISABLED, can && pressed);
            }
        }
    }

    // Ligne d'un succès : étoile (pleine si au moins un palier obtenu), palier en cours, description,
    // barre de progression, récompense. Succès unique obtenu / dernier palier atteint : « Obtenu ».
    private void drawAchievementRow(Canvas canvas, GameModel model, int id, RectF r) {
        Progression prog = model.getProgression();
        boolean maxed = prog.isAchievementMaxed(id);
        int tier = prog.getAchievementTier(id);
        int shown = maxed ? Math.max(0, tier - 1) : tier; // palier affiché : le prochain à obtenir
        ui.inset(canvas, r);
        drawStar(canvas, r.left + 26 * density, r.centerY(), 14 * density, tier > 0);
        float tx = r.left + 50 * density;
        float maxW = r.width() - 50 * density - 80 * density;
        ui.textPx(canvas, Achievement.name(id, shown), tx, r.top + 20 * density, 14 * density,
                maxed ? Palette.ACHIEVEMENT : Color.WHITE, Paint.Align.LEFT, maxW);
        long target = Achievement.target(id, shown);
        long value = Math.min(model.statValue(Achievement.stat(id)), target);
        String progress = maxed ? "Obtenu"
                : Achievement.format(value) + " / " + Achievement.format(target);
        ui.textPx(canvas, Achievement.description(id, shown) + " · " + progress, tx, r.top + 36 * density,
                10.5f * density, maxed ? Palette.TEAM : Palette.TEXT_INACTIVE, Paint.Align.LEFT, maxW);
        if (!maxed) {
            tmpRect.set(tx, r.top + 42 * density, tx + maxW, r.top + 49 * density);
            ui.bar(canvas, tmpRect, target > 0 ? value / (float) target : 0, Palette.ACHIEVEMENT);
        }
        drawGem(canvas, r.right - 52 * density, r.centerY() - 1 * density, 7 * density);
        ui.text(canvas, "+" + Achievement.reward(id, shown), r.right - 40 * density, r.centerY() + 5 * density, 15,
                maxed ? Palette.TEXT_INACTIVE : Palette.GEM, Paint.Align.LEFT);
    }

    // Icône d'un ultime : médaillon de sa couleur et symbole
    private void drawUltimateIcon(Canvas canvas, int id, float cx, float cy, float r, boolean owned) {
        int color = owned ? Palette.ULTIMATE_COLORS[id] : Palette.DISABLED;
        ui.medallion(canvas, cx, cy, r, UiKit.shade(color, 0.7f), false, 255);
        paint.setColor(owned ? Color.WHITE : Palette.ICON_DISABLED);
        float s = r * 0.5f;
        switch (id) {
            case Ultimate.STUN:
                drawStar(canvas, cx, cy, s, true);
                break;
            case Ultimate.FURY:
                path.reset();
                path.moveTo(cx, cy - s * 1.2f);
                path.lineTo(cx + s * 0.8f, cy + s * 0.2f);
                path.lineTo(cx + s * 0.4f, cy + s);
                path.lineTo(cx - s * 0.4f, cy + s);
                path.lineTo(cx - s * 0.8f, cy + s * 0.2f);
                path.close();
                canvas.drawPath(path, paint);
                break;
            case Ultimate.METEOR:
                paint.setColor(owned ? Palette.METEOR : Palette.ICON_DISABLED);
                canvas.drawCircle(cx - s * 0.2f, cy + s * 0.2f, s * 0.7f, paint);
                paint.setStrokeWidth(s * 0.3f);
                canvas.drawLine(cx + s * 0.2f, cy - s * 0.2f, cx + s, cy - s, paint);
                break;
            default:
                drawShieldGlyph(canvas, cx, cy, s);
                break;
        }
    }

    // Écu : bouclier pointu
    private void drawShieldGlyph(Canvas canvas, float cx, float cy, float s) {
        paint.setColor(Color.WHITE);
        path.reset();
        path.moveTo(cx - s, cy - s);
        path.lineTo(cx + s, cy - s);
        path.lineTo(cx + s, cy);
        path.lineTo(cx, cy + s * 1.2f);
        path.lineTo(cx - s, cy);
        path.close();
        canvas.drawPath(path, paint);
    }

    // Récompenses du mode infini selon la difficulté (voir Progression.chestForEndlessWave)
    private static final String[] ENDLESS_REWARDS = {
            "Petit coffre aux vagues 5, 15, 25… · grand aux vagues 10, 20…",
            "1 coffre / 3 vagues · grands (4e, 8e, 12e) et mystérieux (15e)",
            "1 coffre / 2 vagues · grand (4e) et mystérieux (8e)",
            "1 grand coffre / 3 vagues · le 6e est mystérieux",
    };

    // Page Infini : difficultés (avec leur record), récompenses, JOUER
    private void drawEndlessPage(Canvas canvas, GameModel model, int pressed) {
        Progression prog = model.getProgression();
        drawPageHeader(canvas, model, "MODE INFINI", Palette.ENDLESS);
        RectF p = hud.endlessPanel;
        ui.inset(canvas, p);
        float cx = p.centerX();
        ui.text(canvas, "Choisissez la difficulté", cx, p.top + 28 * density, 15, Color.WHITE, Paint.Align.CENTER);
        ui.text(canvas, "Vagues sans fin · nuit une vague sur " + LevelSpec.ENDLESS_NIGHT_EVERY + " · boss toutes les "
                + LevelSpec.BOSS_EVERY, cx, p.top + 50 * density, 11, Palette.TEXT_INACTIVE, Paint.Align.CENTER);

        int sel = model.getEndlessDifficulty();
        for (int d = 0; d < LevelSpec.DIFFICULTY_COUNT; d++) {
            RectF b = hud.difficultyButtons[d];
            int color = Palette.DIFFICULTY_COLORS[d];
            if (d == sel) {
                tmpRect.set(b);
                tmpRect.inset(-3 * density, -3 * density);
                paint.setColor(UiKit.GOLD);
                canvas.drawRoundRect(tmpRect, 12 * density, 12 * density, paint);
            }
            RectF f = ui.button(canvas, b, d == sel ? color : UiKit.shade(color, 0.6f), pressed == Hud.HOME_DIFF_0 + d,
                    true);
            ui.textPx(canvas, LevelSpec.DIFFICULTY_NAMES[d], f.centerX(), f.centerY() - 1 * density, 16 * density,
                    Color.WHITE, Paint.Align.CENTER, f.width() - 8 * density);
            int best = prog.getBestEndlessWave(d);
            ui.textPx(canvas, best > 0 ? "Record : vague " + best : "Aucun record", f.centerX(),
                    f.centerY() + 16 * density, 10.5f * density, Color.argb(230, 255, 240, 200), Paint.Align.CENTER,
                    f.width() - 8 * density);
        }

        // Ce que change la difficulté choisie
        float y = hud.difficultyButtons[LevelSpec.DIFFICULTY_COUNT - 1].bottom + 30 * density;
        int pct = Math.round(LevelSpec.difficultyMultiplier(sel) * 100);
        ui.text(canvas, LevelSpec.DIFFICULTY_NAMES[sel] + " : gobelins à " + pct + " %", cx, y, 14,
                Palette.DIFFICULTY_COLORS[sel], Paint.Align.CENTER);
        String reward = ENDLESS_REWARDS[sel];
        if (LevelSpec.bonusGemsPerChest(sel) > 0) {
            reward += " · +" + LevelSpec.bonusGemsPerChest(sel) + " gemmes par coffre";
        }
        ui.textPx(canvas, reward, cx, y + 22 * density, 12 * density, Palette.GOLD, Paint.Align.CENTER,
                p.width() - 16 * density);
        ui.textPx(canvas, "Les coffres sont gagnés même en abandonnant", cx, y + 42 * density, 11 * density,
                Palette.TEXT_INACTIVE, Paint.Align.CENTER, p.width() - 16 * density);

        drawPillButton(canvas, hud.endlessPlayButton, "JOUER · " + LevelSpec.DIFFICULTY_NAMES[sel].toUpperCase(),
                Palette.ENDLESS, pressed == Hud.HOME_ENDLESS_PLAY);
    }

    // =====================================================================
    //  Animation d'ouverture de coffre
    // =====================================================================
    //  Façon jeu de cartes : le coffre tombe en bas de l'écran puis attend. Chaque toucher fait sortir
    //  UNE carte : elle jaillit du coffre (dos visible), se retourne, s'affiche en grand avec des rayons
    //  de sa couleur, et son nombre monte jusqu'au total. Après la dernière : récapitulatif du butin.
    private void drawChestOpening(Canvas canvas, GameModel model, TouchController touch, int pressed) {
        int f = model.getChestAnimFrame();
        int cf = model.getChestCardFrame();
        int revealed = model.getChestRevealed();
        int n = model.getChestCardCount();
        int intro = GameModel.CHEST_INTRO_FRAMES;

        drawHome(canvas, model, touch, -1); // la page Mages reste derrière
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
                model.getProgression().getLastChestCount() > 1
                        ? model.getProgression().getLastChestCount() + " COFFRES"
                        : Chest.NAMES[model.getProgression().getLastChestType()].toUpperCase(),
                UiKit.shade(Palette.CHEST_COLORS[model.getProgression().getLastChestType()], 0.7f));
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
        // Rareté sous la carte, et « NOUVEAU ! » quand la carte débloque un mage
        if (!back && type >= 0 && cf >= 14) {
            int rarity = Rarity.of(type);
            ui.text(canvas, Rarity.NAMES[rarity].toUpperCase(), cx, cy + h / 2 + 30 * density, 18,
                    Palette.RARITY_COLORS[rarity], Paint.Align.CENTER);
            if (model.isChestCardNew(i)) {
                float p = 1 + 0.08f * (float) Math.sin(frame * 0.2);
                ui.text(canvas, "NOUVEAU MAGE !", cx, cy - h / 2 - 16 * density, 20 * p, Palette.GOLD,
                        Paint.Align.CENTER);
            }
        }
    }

    // Récapitulatif : toutes les cartes du coffre (par rangées de 4), qui apparaissent l'une après l'autre
    private void drawChestSummary(Canvas canvas, GameModel model, int pressed, int cf) {
        float cx = board.centerX();
        int n = model.getChestCardCount();
        ui.ribbon(canvas, cx, board.topShift + 58 * density, Math.min(board.playWidth * 0.6f, 200 * density),
                "BUTIN", Palette.RESUME);

        int perRow = 4;
        int rows = (n + perRow - 1) / perRow;
        float gap = 10 * density;
        float w = Math.min(80 * density, (board.playWidth - 32 * density - (perRow - 1) * gap) / perRow);
        float h = w * 1.32f;
        float rowGap = 34 * density; // place pour « NOUVEAU » sous la carte
        float top = board.height * 0.42f - (rows * h + (rows - 1) * rowGap) / 2;
        for (int i = 0; i < n; i++) {
            int age = cf - i * 5;
            if (age < 0) {
                continue;
            }
            int row = i / perRow;
            int inRow = Math.min(perRow, n - row * perRow);
            float rowLeft = cx - (inRow * w + (inRow - 1) * gap) / 2;
            float x = rowLeft + (i % perRow) * (w + gap) + w / 2;
            float y = top + row * (h + rowGap) + h / 2;
            float t = Math.min(1f, age / 8f);
            float scale = t < 1 ? 0.6f + 0.5f * t : 1;
            int type = model.getChestCardType(i);
            if (type >= 0) {
                paint.setColor(Palette.RARITY_COLORS[Rarity.of(type)]);
                tmpRect.set(x - w / 2 * scale - 3 * density, y - h / 2 * scale - 3 * density,
                        x + w / 2 * scale + 3 * density, y + h / 2 * scale + 3 * density);
                canvas.drawRoundRect(tmpRect, w * 0.11f, w * 0.11f, paint);
            }
            canvas.save();
            canvas.translate(x, y);
            canvas.scale(scale, scale);
            drawCardFace(canvas, 0, 0, w, h, type, "x" + model.getChestCardAmount(i), 1);
            canvas.restore();
            if (model.isChestCardNew(i)) {
                ui.text(canvas, "NOUVEAU !", x, y + h / 2 + 18 * density, 12, Palette.GOLD, Paint.Align.CENTER);
            }
        }

        float bottom = top + rows * h + (rows - 1) * rowGap;
        float pulse = 0.5f + 0.5f * (float) Math.sin(frame * 0.12);
        ui.text(canvas, "Touchez pour continuer", cx, bottom + 44 * density, 15,
                Color.argb((int) (160 + 95 * pulse), 255, 240, 200), Paint.Align.CENTER);
        int chests = model.getProgression().getChests(model.getProgression().getLastChestType());
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
