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
import com.example.magicdefense.controller.ShakeDetector;
import com.example.magicdefense.controller.LightSensor;
import com.example.magicdefense.data.SaveManager;

// La vue Android : fait tourner la boucle et relie modèle, dessin et touchers
public class GameView extends View {
    private final Board board;
    private final GameModel model;
    private final Hud hud;
    private final GameRenderer renderer;
    private final TouchController touchController;
    private final ShakeDetector shakeDetector;
    private final LightSensor lightSensor;
    private final Haptics haptics;
    private int shakePulseTimer = 0; // images avant la prochaine impulsion
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
        // Secousse détectée → on tente de lancer l'ultime (ignoré si la jauge n'est pas pleine)
        shakeDetector = new ShakeDetector(context, () -> model.triggerUltimate());
        lightSensor = new LightSensor(context);
        haptics = new Haptics(context);
        sounds = new SoundManager(context);
        music = new MusicPlayer(context);
        touchController.setOnButtonClick(() -> {
            sounds.setSystemVolume(model.getSoundVolume()); // applique le volume avant l'aperçu
            sounds.click();
        });
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
        });
        model.setSoundVolume(sounds.getSystemVolume()); // le curseur part du volume actuel du téléphone
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
        renderer.draw(canvas, model, touchController, shakeDetector.getProgress());
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
            model.setNightMode(lightSensor.isNight());
            model.update();
            // Pendant une secousse (jauge pleine) : une impulsion toutes les 8 images (≈ 7 par seconde)
            float shake = model.isUltimateReady() ? shakeDetector.getProgress() : 0;
            if (shake > 0) {
                shakePulseTimer--;
                if (shakePulseTimer <= 0) {
                    haptics.shakePulse(shake);
                    shakePulseTimer = 8;
                }
            } else {
                shakePulseTimer = 0; // la prochaine secousse vibre tout de suite
            }
            invalidate();          // demande un nouvel appel à onDraw()
            postDelayed(this, 16); // ≈ 60 images/s
        }
    };

    public void startGame() {
        removeCallbacks(gameLoop); // évite deux boucles en parallèle
        post(gameLoop);
        shakeDetector.start();
        lightSensor.start();
        music.resume();
    }

    public void stopGame() {
        model.pause();
        touchController.cancel();
        shakeDetector.stop(); // capteur coupé en arrière-plan
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
