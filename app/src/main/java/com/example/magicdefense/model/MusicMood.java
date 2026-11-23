package com.example.magicdefense.model;

// Choix de la musique de fond selon la situation (logique pure, testable sans Android).
// Priorité : boss > nuit > normale. Hors d'une partie (accueil, coffres, victoire, défaite) : normale.
public final class MusicMood {
    public static final int NORMAL = 0;
    public static final int NIGHT = 1;
    public static final int BOSS = 2;
    public static final int COUNT = 3;

    private MusicMood() {
    }

    // bossOnField : un boss vivant est sur le terrain ; bossWave : la vague en cours amène un boss
    public static int choose(GameState state, boolean bossOnField, boolean bossWave, boolean night) {
        boolean inGame = state == GameState.PLAYING || state == GameState.PAUSED
                || state == GameState.OPTIONS;
        if (!inGame) {
            return NORMAL;
        }
        if (bossOnField || bossWave) {
            return BOSS;
        }
        return night ? NIGHT : NORMAL;
    }

    // Même choix, lu directement dans le modèle
    public static int of(GameModel model) {
        GameState state = model.getState();
        // Options ouvertes depuis l'accueil : on n'est pas en partie
        if (state == GameState.OPTIONS && model.getOptionsReturnState() == GameState.START) {
            state = GameState.START;
        }
        boolean bossWave = model.getSpec() != null && model.getSpec().hasBoss(model.getWave());
        return choose(state, model.getBoss() != null, bossWave, model.isNightMode());
    }
}
