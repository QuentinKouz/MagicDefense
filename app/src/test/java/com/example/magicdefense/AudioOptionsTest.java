package com.example.magicdefense;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.magicdefense.model.Board;
import com.example.magicdefense.model.GameModel;
import com.example.magicdefense.model.GameState;
import com.example.magicdefense.model.LevelSpec;
import com.example.magicdefense.model.MusicMood;

import org.junit.Test;

// Choix de la musique (normale / nuit / boss), volumes internes, option des effets, nuit sans capteur
public class AudioOptionsTest {

    private static GameModel newModel() {
        Board board = new Board(1f);
        board.resize(400, 800);
        return new GameModel(board);
    }

    @Test
    public void mood_bossHasPriority_thenNight() {
        assertEquals(MusicMood.BOSS, MusicMood.choose(GameState.PLAYING, true, false, true));
        assertEquals(MusicMood.BOSS, MusicMood.choose(GameState.PLAYING, false, true, false));
        assertEquals(MusicMood.NIGHT, MusicMood.choose(GameState.PLAYING, false, false, true));
        assertEquals(MusicMood.NORMAL, MusicMood.choose(GameState.PLAYING, false, false, false));
        // Pause et options en jeu : on garde l'ambiance de la partie
        assertEquals(MusicMood.NIGHT, MusicMood.choose(GameState.PAUSED, false, false, true));
        assertEquals(MusicMood.BOSS, MusicMood.choose(GameState.OPTIONS, true, false, false));
    }

    @Test
    public void mood_outsideGame_isNormal() {
        assertEquals(MusicMood.NORMAL, MusicMood.choose(GameState.START, true, true, true));
        assertEquals(MusicMood.NORMAL, MusicMood.choose(GameState.VICTORY, false, false, true));
        assertEquals(MusicMood.NORMAL, MusicMood.choose(GameState.GAME_OVER, true, false, false));
    }

    @Test
    public void mood_fromModel() {
        GameModel m = newModel();
        assertEquals(MusicMood.NORMAL, MusicMood.of(m)); // accueil
        m.startLevel(1);
        assertEquals(MusicMood.NORMAL, MusicMood.of(m)); // niveau 1 : de jour, sans boss
        // Premier niveau de nuit de la campagne
        int night = 1;
        while (!LevelSpec.isNightLevel(night)) {
            night++;
        }
        m.quitToMenu();
        m.startLevel(night);
        assertTrue(m.isNightMode());
        assertEquals(LevelSpec.forLevel(night).hasBoss(m.getWave()) ? MusicMood.BOSS : MusicMood.NIGHT,
                MusicMood.of(m));
    }

    @Test
    public void night_onlyFromLevel() {
        GameModel m = newModel();
        m.startLevel(1);
        for (int i = 0; i < 120; i++) {
            m.update();
        }
        assertFalse(m.isNightMode()); // plus de capteur de luminosité : le niveau 1 reste de jour
    }

    @Test
    public void volumes_areInternalAndClamped() {
        GameModel m = newModel();
        m.setMusicVolume(1.5f);
        assertEquals(1f, m.getMusicVolume(), 1e-6);
        m.setSoundVolume(-1f);
        assertEquals(0f, m.getSoundVolume(), 1e-6);
        m.setMusicVolume(0.3f);
        assertEquals(0.3f, m.getMusicVolume(), 1e-6);
    }

    @Test
    public void attackEffects_toggle() {
        GameModel m = newModel();
        assertTrue(m.isAttackEffectsEnabled()); // activés par défaut
        m.toggleAttackEffects();
        assertFalse(m.isAttackEffectsEnabled());
        m.setAttackEffectsEnabled(true);
        assertTrue(m.isAttackEffectsEnabled());
    }
}
