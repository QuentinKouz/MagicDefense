package com.example.magicdefense;

import android.media.AudioManager;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.example.magicdefense.view.GameView;

public class MainActivity extends AppCompatActivity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC); // les touches de volume règlent le jeu
        EdgeToEdge.enable(this); // le jeu occupe tout l'écran, y compris sous les barres
        gameView = new GameView(this);
        setContentView(gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemBars();
        gameView.startGame();
    }

    @Override
    protected void onPause() {
        gameView.stopGame();
        super.onPause();
    }

    // Les barres peuvent réapparaître (notification, retour dans l'appli) : on les recache
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemBars();
        }
    }

    // Mode immersif : barre d'état et barre de navigation cachées
    private void hideSystemBars() {
        WindowInsetsControllerCompat controller =
                WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        // Un glissement depuis le bord les affiche temporairement, puis elles se recachent
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }
}