package com.example.magicdefense.controller;

import android.graphics.RectF;
import android.view.MotionEvent;

import com.example.magicdefense.model.Board;
import com.example.magicdefense.model.GameModel;
import com.example.magicdefense.model.GameState;
import com.example.magicdefense.model.Mage;
import com.example.magicdefense.view.Hud;
import com.example.magicdefense.view.MenuList;

// Traduit les touchers en actions sur le modèle (boutons, drag & drop, changements d'écran).
// Accueil : glisser horizontalement change de page (Marché, Mages, Campagne, Infini, Château),
// glisser verticalement fait défiler la liste de la page.
public class TouchController {
    private final GameModel model;
    private final Board board;
    private final Hud hud;
    private final float density;
    private final float dragOffset; // décalage pour ne pas cacher le mage sous le doigt
    private final float touchSlop;  // au-delà, le doigt « glisse » (ce n'est plus un simple toucher)

    private int pressedButton = -1;       // bouton de recrutement sous le doigt (-1 = aucun)
    private int pressedGameButton = -1;   // bouton rond de la partie (pause, son, vitesse...)
    private int pressedMenuButton = -1;   // bouton de menu sous le doigt (-1 = aucun)
    private Mage draggedMage = null;      // mage en cours de déplacement (null = aucun)
    private int dragRow, dragCol;         // case d'origine
    private float dragX, dragY;           // position du doigt
    private int lockedRow = -1, lockedCol = -1; // case bloquée sous le doigt
    private float downX, downY;           // point de départ du geste
    private boolean moved = false;        // le doigt a glissé au-delà du seuil
    private Runnable onButtonClick; // appelé quand un bouton est validé (pour le son)
    private float dragVolume = 0; // position du curseur pendant le glissement
    private Runnable onToggleMute; // couper / remettre le son (géré par GameView)
    private boolean draggingVolume = false; // doigt en train de régler le volume

    // Accueil : glissement entre les pages et défilement des listes
    private static final int GESTURE_NONE = 0;
    private static final int GESTURE_SWIPE = 1;
    private static final int GESTURE_SCROLL = 2;
    private int gesture = GESTURE_NONE;
    private float pageOffset = 0;         // décalage horizontal des pages (animation)
    private final float[] scroll = new float[GameModel.PAGE_COUNT];
    private float scrollStart = 0;
    private float scrollVelocity = 0;
    private float lastY = 0;
    private long lastMoveTime = 0;
    private final MenuList menu;
    private final RectF tmpRow = new RectF();
    private final RectF tmpBtn = new RectF();

    public TouchController(GameModel model, Board board, Hud hud, float density) {
        this.model = model;
        this.board = board;
        this.hud = hud;
        this.density = density;
        this.dragOffset = 48 * density;
        this.touchSlop = 10 * density;
        this.menu = new MenuList(density);
    }

    // ---------- Accès en lecture (pour le feedback visuel) ----------
    public int getPressedButton() { return pressedButton; }
    public boolean isPausePressed() { return pressedGameButton == Hud.GAME_PAUSE; }
    public int getPressedGameButton() { return pressedGameButton; }
    public int getPressedMenuButton() { return pressedMenuButton; }
    public Mage getDraggedMage() { return moved ? draggedMage : null; } // un simple toucher ne soulève pas le mage
    public float getDragX() { return dragX; }
    public float getDragY() { return dragY - dragOffset; } // position affichée, au-dessus du doigt
    public boolean isDraggingVolume() { return draggingVolume; }
    public float getDragVolume() { return dragVolume; }
    public boolean isMutePressed() { return pressedGameButton == Hud.GAME_SOUND; }
    public int getPressedLockedRow() { return lockedRow; }
    public int getPressedLockedCol() { return lockedCol; }
    public float getPageOffset() { return pageOffset; }
    public float getScroll(int page) { return scroll[page]; }

    public void setOnButtonClick(Runnable onButtonClick) {
        this.onButtonClick = onButtonClick;
    }

    public void setOnToggleMute(Runnable onToggleMute) {
        this.onToggleMute = onToggleMute;
    }

    private void click() {
        if (onButtonClick != null) {
            onButtonClick.run();
        }
    }

    // Une fois par image : animation du glissement entre pages et inertie du défilement
    public void tick() {
        if (gesture != GESTURE_SWIPE) {
            pageOffset *= 0.72f;
            if (Math.abs(pageOffset) < 1) {
                pageOffset = 0;
            }
        }
        int page = model.getHomePage();
        RectF vp = hud.viewportFor(page);
        if (vp != null) {
            if (gesture != GESTURE_SCROLL && Math.abs(scrollVelocity) > 0.5f) {
                scroll[page] += scrollVelocity;
                scrollVelocity *= 0.92f;
            } else if (gesture != GESTURE_SCROLL) {
                scrollVelocity = 0;
            }
            menu.build(page, model);
            scroll[page] = Math.max(0, Math.min(menu.maxScroll(vp.height()), scroll[page]));
        }
    }

    // Retourne true quand le geste se termine (pour appeler performClick)
    public boolean onTouchEvent(MotionEvent event) {
        float tx = event.getX();
        float ty = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = tx;
                downY = ty;
                lastY = ty;
                lastMoveTime = event.getEventTime();
                moved = false;
                gesture = GESTURE_NONE;
                scrollVelocity = 0;
                if (model.getState() == GameState.PLAYING) {
                    pressedGameButton = gameButtonAt(tx, ty);
                    if (pressedGameButton == -1) {
                        pressedButton = hud.buttonAt(tx, ty);
                        if (pressedButton != -1 && model.teamTypeAt(pressedButton) == -1) {
                            pressedButton = -2; // emplacement d'équipe vide : ni recrutement, ni glisser
                        }
                        if (pressedButton == -1) {
                            startDrag(tx, ty); // pas un bouton : peut-être un mage ou une case bloquée
                        }
                    }
                } else {
                    pressedMenuButton = menuButtonAt(tx, ty);
                    // Curseur de volume : on commence à le régler dès l'appui
                    if (model.getState() == GameState.OPTIONS && pressedMenuButton == Hud.OPTIONS_VOLUME) {
                        draggingVolume = true;
                        dragVolume = hud.volumeAt(tx);
                    }
                }
                return false;

            case MotionEvent.ACTION_MOVE: {
                // Réglage du volume : le curseur suit le doigt, même s'il sort de la ligne
                if (draggingVolume) {
                    dragVolume = hud.volumeAt(tx);
                    return false;
                }
                float dx = tx - downX;
                float dy = ty - downY;
                if (!moved && (Math.abs(dx) > touchSlop || Math.abs(dy) > touchSlop)) {
                    moved = true;
                }
                if (model.getState() == GameState.START) {
                    handleHomeMove(dx, dy, ty, event.getEventTime());
                }
                // Le doigt sort du bouton : on annule l'appui
                if (pressedMenuButton != -1 && menuButtonAt(tx, ty) != pressedMenuButton) {
                    pressedMenuButton = -1;
                }
                if (pressedGameButton != -1 && gameButtonAt(tx, ty) != pressedGameButton) {
                    pressedGameButton = -1;
                }
                if (lockedRow != -1) {
                    int[] cell = board.cellAt(tx, ty);
                    if (cell == null || cell[0] != lockedRow || cell[1] != lockedCol) {
                        lockedRow = -1;
                        lockedCol = -1;
                    }
                }
                if (draggedMage != null) {
                    dragX = tx; // le mage suit le doigt
                    dragY = ty;
                } else if (pressedButton != -1 && hud.buttonAt(tx, ty) != pressedButton) {
                    pressedButton = -1;
                }
                return false;
            }

            case MotionEvent.ACTION_UP:
                // Fin du réglage du volume : on applique la valeur, puis clic d'aperçu
                if (draggingVolume) {
                    draggingVolume = false;
                    pressedMenuButton = -1;
                    model.setSoundVolume(dragVolume);
                    click(); // applique au téléphone et joue le clic
                    return true;
                }
                if (gesture == GESTURE_SWIPE) {
                    endSwipe(tx - downX);
                } else if (gesture == GESTURE_SCROLL) {
                    // l'inertie continue dans tick()
                } else if (draggedMage != null) {
                    endDrag();
                } else {
                    handleRelease(tx, ty);
                }
                // Le geste est fini : on remet tout à zéro
                gesture = GESTURE_NONE;
                pressedButton = -1;
                pressedGameButton = -1;
                pressedMenuButton = -1;
                lockedRow = -1;
                lockedCol = -1;
                draggedMage = null;
                return true;

            case MotionEvent.ACTION_CANCEL:
                cancel(); // geste interrompu par le système
                return false;
        }
        return false;
    }

    // Accueil : le geste devient un glissement de page (horizontal) ou un défilement (vertical)
    private void handleHomeMove(float dx, float dy, float ty, long time) {
        int page = model.getHomePage();
        RectF vp = hud.viewportFor(page);
        if (gesture == GESTURE_NONE && moved) {
            if (Math.abs(dx) > Math.abs(dy)) {
                gesture = GESTURE_SWIPE;
            } else if (vp != null && vp.contains(downX, downY)) {
                gesture = GESTURE_SCROLL;
                scrollStart = scroll[page];
            }
            if (gesture != GESTURE_NONE) {
                pressedMenuButton = -1; // ce n'est plus un toucher de bouton
            }
        }
        if (gesture == GESTURE_SWIPE) {
            boolean edge = (page == 0 && dx > 0) || (page == GameModel.PAGE_COUNT - 1 && dx < 0);
            pageOffset = edge ? dx * 0.3f : dx; // résistance au bout
        } else if (gesture == GESTURE_SCROLL && vp != null) {
            menu.build(page, model);
            scroll[page] = Math.max(0, Math.min(menu.maxScroll(vp.height()), scrollStart - dy));
            long dt = Math.max(1, time - lastMoveTime);
            scrollVelocity = (lastY - ty) * 16f / dt; // en pixels par image (≈ 16 ms)
            lastY = ty;
            lastMoveTime = time;
        }
    }

    // Fin du glissement : page voisine si le doigt est allé assez loin, sinon retour
    private void endSwipe(float dx) {
        int page = model.getHomePage();
        float w = board.width;
        if (Math.abs(dx) > w * 0.18f) {
            int target = page + (dx < 0 ? 1 : -1);
            if (target >= 0 && target < GameModel.PAGE_COUNT) {
                model.setHomePage(target);
                pageOffset = dx < 0 ? dx + w : dx - w; // la nouvelle page continue son mouvement
                click();
                return;
            }
        }
        pageOffset = dx; // revient doucement (tick)
    }

    // Changement de page par un onglet : petite glissade dans le bon sens
    private void goToPage(int page) {
        int current = model.getHomePage();
        if (page == current) {
            return;
        }
        model.setHomePage(page);
        pageOffset = (page > current ? 1 : -1) * board.width * 0.35f;
    }

    // Annule tout geste en cours (pause, interruption...)
    public void cancel() {
        draggedMage = null;
        pressedButton = -1;
        pressedGameButton = -1;
        pressedMenuButton = -1;
        draggingVolume = false;
        lockedRow = -1;
        lockedCol = -1;
        gesture = GESTURE_NONE;
    }

    // Toutes les actions se déclenchent au relâchement (choix du dossier de design)
    private void handleRelease(float tx, float ty) {
        boolean menuClick = pressedMenuButton != -1 && menuButtonAt(tx, ty) == pressedMenuButton;
        boolean gameClick = pressedGameButton != -1 && gameButtonAt(tx, ty) == pressedGameButton;
        if (menuClick || gameClick) {
            click();
        }
        switch (model.getState()) {
            case START:
                if (menuClick) {
                    handleHomeButton(pressedMenuButton);
                }
                break;

            case CHEST_OPENING:
                if (menuClick) {
                    if (pressedMenuButton == Hud.CHEST_AGAIN) {
                        model.openChest(); // coffre suivant
                    } else {
                        model.tapChestOpening(); // passer l'animation, ou fermer
                    }
                }
                break;

            case GAME_OVER:
                if (menuClick) {
                    if (pressedMenuButton == Hud.GAMEOVER_RETRY) {
                        model.restartLevel();
                    } else if (pressedMenuButton == Hud.GAMEOVER_MENU) {
                        model.quitToMenu();
                    }
                }
                break;

            case PLAYING:
                if (gameClick) {
                    switch (pressedGameButton) {
                        case Hud.GAME_PAUSE:
                            model.pause();
                            break;
                        case Hud.GAME_SOUND:
                            if (onToggleMute != null) {
                                onToggleMute.run();
                            }
                            break;
                        case Hud.GAME_SPEED:
                            model.cycleGameSpeed();
                            break;
                        case Hud.GAME_AUTO:
                            model.toggleAutoMode();
                            break;
                        case Hud.GAME_HEAL:
                            model.buyHeal();
                            break;
                        case Hud.GAME_ULTIMATE:
                            model.triggerUltimate();
                            break;
                        case Hud.GAME_LAUNCH:
                            model.launchWave();
                            break;
                        case Hud.GAME_RANDOM:
                            model.recruitRandom();
                            break;
                    }
                } else if (pressedButton >= 0 && hud.buttonAt(tx, ty) == pressedButton) {
                    model.recruitSlot(pressedButton); // emplacement d'équipe → mage de cet élément
                } else if (lockedRow != -1) {
                    model.unlockCell(lockedRow, lockedCol); // case bloquée achetée avec des PO
                }
                break;

            case PAUSED:
                // Seulement si appuyé ET relâché sur le même bouton
                if (menuClick) {
                    switch (pressedMenuButton) {
                        case Hud.PAUSE_QUIT:
                            model.quitToMenu();
                            break;
                        case Hud.PAUSE_RESTART:
                            model.restartLevel();
                            break;
                        case Hud.PAUSE_RESUME:
                            model.resume();
                            break;
                        case Hud.PAUSE_VIBRATION:
                            model.toggleVibration();
                            break;
                        case Hud.PAUSE_OPTIONS:
                            model.openOptions();
                            break;
                    }
                }
                break;

            case OPTIONS:
                if (menuClick) {
                    if (pressedMenuButton == Hud.OPTIONS_VIBRATION) {
                        model.toggleVibration();
                    } else if (pressedMenuButton == Hud.OPTIONS_MUSIC_TOGGLE) {
                        model.toggleMusic();
                    } else if (pressedMenuButton == Hud.OPTIONS_LEFT_HANDED) {
                        model.toggleLeftHanded(); // l'interface passe en miroir (GameView refait la mise en page)
                    } else if (pressedMenuButton == Hud.OPTIONS_BACK) {
                        model.closeOptions();
                    }
                }
                break;

            case VICTORY:
                if (menuClick) {
                    if (pressedMenuButton == Hud.VICTORY_NEXT) {
                        model.nextLevel();
                    } else if (pressedMenuButton == Hud.VICTORY_RESTART) {
                        model.restartLevel();
                    } else if (pressedMenuButton == Hud.VICTORY_MENU) {
                        model.quitToMenu();
                    }
                }
                break;
        }
    }

    // Accueil : bouton validé
    private void handleHomeButton(int b) {
        if (b >= Hud.HOME_LIST_0) {
            handleListButton((b - Hud.HOME_LIST_0) / 4, (b - Hud.HOME_LIST_0) % 4);
        } else if (b >= Hud.HOME_ENDLESS_PLAY) {
            model.startEndless();
        } else if (b >= Hud.HOME_DIFF_0) {
            model.setEndlessDifficulty(b - Hud.HOME_DIFF_0);
        } else if (b >= Hud.HOME_CHEST_0) {
            model.openChests(Hud.CHEST_BATCHES[b - Hud.HOME_CHEST_0]); // ×1, ×5 ou ×10
        } else if (b >= Hud.HOME_TAB_0) {
            goToPage(b - Hud.HOME_TAB_0);
        } else if (b == Hud.HOME_PLAY) {
            model.startLevel(model.getSelectedLevel());
        } else if (b == Hud.HOME_OPTIONS) {
            model.openOptions();
        } else if (b == Hud.HOME_SOUND) {
            if (onToggleMute != null) {
                onToggleMute.run();
            }
        } else if (b == Hud.HOME_PREV_PAGE) {
            model.changeMapPage(-1);
        } else if (b == Hud.HOME_NEXT_PAGE) {
            model.changeMapPage(1);
        } else {
            // Emplacement touché (1 à 6) → numéro réel du niveau sur cette page
            model.selectLevel(model.levelAtSlot(b));
        }
    }

    // Bouton d'une ligne de liste (Marché, Mages, Château)
    private void handleListButton(int row, int button) {
        menu.build(model.getHomePage(), model);
        if (row >= menu.count) {
            return;
        }
        int a = menu.arg[row];
        switch (menu.kind[row]) {
            case MenuList.MAGE:
                if (button == MenuList.BTN_PORTRAIT) {
                    model.toggleTeam(a);
                } else if (button == MenuList.BTN_ARTIFACT) {
                    model.cycleArtifact(a);
                } else {
                    model.upgradeMage(a); // +1 RANG
                }
                break;
            case MenuList.SHOP_CHEST:
                model.buyChest();
                break;
            case MenuList.SHOP_UNIT:
                model.buyUnit(a);
                break;
            case MenuList.SHOP_ULTIMATE:
                model.buyUltimate(a);
                break;
            case MenuList.SHOP_AUTO:
                model.buyAuto();
                break;
            case MenuList.SHOP_SPEED:
                model.buySpeed(a);
                break;
            case MenuList.CASTLE_HP:
                model.upgradeCastleHp();
                break;
            case MenuList.CASTLE_RES:
                model.upgradeCastleRes();
                break;
            case MenuList.ULTIMATE:
                if (model.getProgression().ownsUltimate(a)) {
                    model.equipUltimate(a);
                } else {
                    goToPage(GameModel.PAGE_MARKET); // pas encore acheté : direction le Marché
                }
                break;
        }
    }

    // Bouton de menu sous le doigt, selon la fenêtre affichée
    private int menuButtonAt(float x, float y) {
        switch (model.getState()) {
            case PAUSED:
                return hud.pauseMenuButtonAt(x, y);
            case OPTIONS:
                return hud.optionsButtonAt(x, y);
            case VICTORY:
                return hud.victoryButtonAt(x, y);
            case START: {
                if (pageOffset != 0 && gesture == GESTURE_NONE && Math.abs(pageOffset) > 4 * density) {
                    return -1; // page encore en mouvement
                }
                int page = model.getHomePage();
                int b = hud.homeButtonAt(page, x, y);
                // Flèche de page sans page de ce côté : elle est cachée, donc inactive
                if ((b == Hud.HOME_PREV_PAGE && !model.canChangeMapPage(-1))
                        || (b == Hud.HOME_NEXT_PAGE && !model.canChangeMapPage(1))) {
                    return -1;
                }
                if (b == -1) {
                    RectF vp = hud.viewportFor(page);
                    if (vp != null) {
                        menu.build(page, model);
                        int hit = menu.hit(x, y, vp, scroll[page], tmpRow, tmpBtn);
                        if (hit >= 0) {
                            return Hud.HOME_LIST_0 + hit;
                        }
                    }
                }
                return b;
            }
            case GAME_OVER:
                return hud.gameOverButtonAt(x, y); // même fenêtre que la victoire
            case CHEST_OPENING: {
                int c = hud.chestButtonAt(x, y);
                // OUVRIR ENCORE n'existe qu'à la fin de l'animation, s'il reste des coffres
                if (c == Hud.CHEST_AGAIN && !(model.isChestRevealComplete()
                        && model.getProgression().getChests() > 0)) {
                    return Hud.CHEST_CONTINUE;
                }
                return c;
            }
            default:
                return -1;
        }
    }

    // Boutons de la partie ; « Lancer la vague » seulement tant que la vague attend
    private int gameButtonAt(float x, float y) {
        if (model.isWaitingForWave() && hud.launchButton.contains(x, y)) {
            return Hud.GAME_LAUNCH;
        }
        return hud.gameButtonAt(x, y);
    }

    private void startDrag(float x, float y) {
        int[] cell = board.cellAt(x, y);
        if (cell == null) {
            return;
        }
        if (model.isLocked(cell[0], cell[1])) {
            lockedRow = cell[0];
            lockedCol = cell[1];
            return;
        }
        if (model.getMage(cell[0], cell[1]) != null) {
            draggedMage = model.getMage(cell[0], cell[1]);
            dragRow = cell[0];
            dragCol = cell[1];
            dragX = x;
            dragY = y;
        }
    }

    private void endDrag() {
        if (!moved) {
            // Simple toucher sur un mage : il lance sa compétence (niveau 5, si elle est rechargée)
            model.castSkill(dragRow, dragCol);
            draggedMage = null;
            return;
        }
        // On vise avec le mage affiché (au-dessus du doigt), pas avec le doigt
        int[] cell = board.cellAt(getDragX(), getDragY());
        if (cell != null) {
            model.dropMage(dragRow, dragCol, cell[0], cell[1]);
        }
        draggedMage = null;
    }
}
