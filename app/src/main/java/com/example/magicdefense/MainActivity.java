package com.example.magicdefense;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.magicdefense.view.GameView;

public class MainActivity extends AppCompatActivity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameView = new GameView(this);
        setContentView(gameView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.startGame();   // l'écran redevient visible
    }

    @Override
    protected void onPause() {
        gameView.stopGame();    // l'appli passe en arrière-plan
        super.onPause();
    }
}
