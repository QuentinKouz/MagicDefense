package com.example.magicdefense.view;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.view.DisplayCutout;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;

import com.example.magicdefense.controller.TouchController;
import com.example.magicdefense.model.Board;
import com.example.magicdefense.model.GameModel;
import com.example.magicdefense.model.GameState;
import com.example.magicdefense.controller.LightSensor;
import com.example.magicdefense.data.SaveManager;

// La vue Android : fait tourner la boucle et relie modèle, dessin et touchers
public class GameView extends View {
    private final Board board;
    private final GameModel model;
    private final Hud hud;
    private final GameRenderer renderer;
    private final TouchController touchController;
    private final LightSensor lightSensor;
    private final Haptics haptics;
    private final SaveManager saveManager;
    private final SoundManager sounds;
    private final MusicPlayer music;
    private float volumeBeforeMute = 0.5f; // volume à retrouver en réactivant le son

    public GameView(Context context) {
        super(context);
        float density = getResources().getDisplayMetrics().density;
        board = new Board(density);
        model = new GameModel(board);
        hud = new Hud(board);
        renderer = new GameRenderer(board, hud, density);
        saveManager = new SaveManager(context);
        saveManager.load(model); // reprend la progression sauvegardée
        touchController = new TouchController(model, board, hud, density);
        lightSensor = new LightSensor(context);
        haptics = new Haptics(context);
        sounds = new SoundManager(context);
        music = new MusicPlayer(context);
        touchController.setOnButtonClick(() -> {
            sounds.setSystemVolume(model.getSoundVolume()); // applique le volume avant l'aperçu
            sounds.click();
        });
        // Bouton CODE des options : petite fenêtre Android avec un champ de texte
        touchController.setOnEnterCode(() -> showCodeDialog(context));
        // Bouton son de l'accueil : coupe, ou remet le volume d'avant
        touchController.setOnToggleMute(() -> {
            float current = sounds.getSystemVolume();
            if (current > 0) {
                volumeBeforeMute = current;
                sounds.setSystemVolume(0);
            } else {
                sounds.setSystemVolume(volumeBeforeMute);
                sounds.click(); // on entend tout de suite que le son est revenu
            }
            model.setSoundVolume(sounds.getSystemVolume());
        });
        model.setListener(new GameModel.GameListener() {
            @Override
            public void onCastleHit() {
                haptics.castleHit();
                sounds.castleHit();
            }

            @Override
            public void onUltimateTriggered() {
                haptics.ultimate();
                sounds.ultimate();
            }

            @Override
            public void onMerge() {
                haptics.merge();
                sounds.merge();
            }

            @Override
            public void onLevelCompleted() {
                saveManager.save(model); // sauvegarde dès la victoire
                sounds.victory();
            }

            @Override
            public void onGameOver() {
                sounds.defeat();
            }

            @Override
            public void onEnemyKilled() {
                sounds.kill();
            }

            @Override
            public void onRecruit() {
                sounds.recruit();
            }

            @Override
            public void onChestOpened() {
                haptics.merge();
                sounds.victory();
            }

            @Override
            public void onChestCardRevealed() {
                haptics.merge();
                sounds.merge();
            }

            @Override
            public void onMageUpgraded() {
                haptics.merge();
                sounds.merge();
            }

            @Override
            public void onUnitBought() {
                haptics.merge();
                sounds.victory();
            }

            @Override
            public void onReaction() {
                // Pas de son : les réactions peuvent s'enchaîner plusieurs fois par seconde
            }

            @Override
            public void onBossArrived() {
                haptics.castleHit();
                sounds.ultimate();
            }

            @Override
            public void onSkill() {
                haptics.merge();
                sounds.ultimate();
            }

            @Override
            public void onAchievement() {
                sounds.victory();
            }

            @Override
            public void onProgressChanged() {
                saveManager.save(model); // coffres, fragments, rangs : sauvegardés tout de suite
            }
        });
        model.setSoundVolume(sounds.getSystemVolume()); // le curseur part du volume actuel du téléphone
    }

    // Saisie d'un code (options de l'accueil) : le modèle vérifie, un message donne le résultat
    private void showCodeDialog(Context context) {
        android.widget.EditText input = new android.widget.EditText(context);
        input.setSingleLine(true);
        input.setHint("Code");
        int pad = Math.round(20 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout box = new android.widget.FrameLayout(context);
        box.setPadding(pad, pad / 2, pad, 0);
        box.addView(input);
        new android.app.AlertDialog.Builder(context)
                .setTitle("Entrer un code")
                .setView(box)
                .setPositiveButton("Valider", (dialog, which) -> {
                    String code = input.getText().toString();
                    int result = model.redeemCode(code);
                    String message = result == com.example.magicdefense.model.Progression.CODE_OK
                            ? "Code accepté : " + com.example.magicdefense.model.Progression.giftCodeReward(code) + " !"
                            : result == com.example.magicdefense.model.Progression.CODE_ALREADY_USED
                            ? "Ce code a déjà été utilisé" : "Code invalide";
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    // Appelée dès que la taille de la vue est connue (et à chaque changement : rotation, pliable...)
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        relayout(w, h);
    }

    // Recalcule toute la mise en page pour la taille actuelle de l'écran
    private void relayout(int w, int h) {
        if (w == 0 || h == 0) {
            return;
        }
        board.resize(w, h);
        hud.setLeftHanded(model.isLeftHanded());
        hud.resize(w, h);
        renderer.onResize(); // décor regénéré à la bonne taille
    }

    // Android indique ici la place prise par la caméra / l'encoche en haut de l'écran
    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        int top = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            DisplayCutout cutout = insets.getDisplayCutout();
            if (cutout != null) {
                top = cutout.getSafeInsetTop();
            }
        }
        board.setSafeInsetTop(top);
        relayout(getWidth(), getHeight());
        return super.onApplyWindowInsets(insets);
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
            // Les touches du téléphone font bouger le curseur et l'icône (sauf pendant un glissement)
            if (!touchController.isDraggingVolume()) {
                model.setSoundVolume(sounds.getSystemVolume());
            }
            music.setEnabled(model.isMusicEnabled()); // option "Musique" des réglages
            haptics.setEnabled(model.isVibrationEnabled()); // option "Vibrations"
            if (hud.isLeftHanded() != model.isLeftHanded()) {
                relayout(getWidth(), getHeight()); // option "Mode gaucher" : interface en miroir
            }
            model.setNightMode(lightSensor.isNight());
            model.updateUi();       // animations des menus, bandeaux de succès (une fois par image)
            touchController.tick(); // glissement entre les pages, inertie des listes
            // Vitesse du jeu : ×2 ou ×3 = 2 ou 3 étapes de jeu par image
            int steps = model.getState() == GameState.PLAYING ? model.getGameSpeed() : 1;
            for (int i = 0; i < steps; i++) {
                model.update();
            }
            invalidate();          // demande un nouvel appel à onDraw()
            postDelayed(this, 16); // ≈ 60 images/s
        }
    };

    public void startGame() {
        removeCallbacks(gameLoop); // évite deux boucles en parallèle
        post(gameLoop);
        lightSensor.start();
        music.resume();
    }

    public void stopGame() {
        model.pause();
        touchController.cancel();
        lightSensor.stop();
        music.pause(); // pas de musique quand l'appli est en arrière-plan
        saveManager.save(model); // sauvegarde quand l'appli passe en arrière-plan (options comprises)
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

    // Appelée quand la vue est retirée de l'écran (fermeture de l'activité)
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        sounds.release();
        music.release();
    }


}
