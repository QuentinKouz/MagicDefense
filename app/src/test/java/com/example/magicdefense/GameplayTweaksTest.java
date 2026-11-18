package com.example.magicdefense;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.magicdefense.model.Board;
import com.example.magicdefense.model.GameModel;
import com.example.magicdefense.model.GameState;
import com.example.magicdefense.model.Mage;
import com.example.magicdefense.model.Progression;

import org.junit.Test;

// Ajustements de jeu : prix croissant, 1re vague lancée par le joueur, recharge de l'ultime,
// étoiles perdues, mode automatique payant, vitesse ×1 au départ
public class GameplayTweaksTest {

    private static GameModel newModel() {
        Board board = new Board(1f);
        board.resize(400, 800);
        return new GameModel(board);
    }

    // Partie reprise avec des valeurs choisies (PO, PV, jauge, étoiles...)
    private static GameModel withRun(int gold, float hpRatio, int charge, int lowestStars) {
        GameModel m = newModel();
        m.startLevel(1);
        int[] data = m.exportRun();
        data[5] = gold;
        data[6] = Math.round(data[10] * hpRatio);
        data[7] = charge;
        data[13] = lowestStars;
        GameModel other = newModel();
        assertTrue(other.importRun(data));
        other.resume();
        return other;
    }

    @Test
    public void magePrice_risesByTwo_exceptRandom() {
        GameModel m = withRun(1000, 1f, 0, 3);
        assertEquals(GameModel.MAGE_COST, m.getMageCost());
        m.recruitMage(Mage.FIRE);
        assertEquals(GameModel.MAGE_COST + 2, m.getMageCost());
        m.recruitMage(Mage.WATER);
        assertEquals(GameModel.MAGE_COST + 4, m.getMageCost());
        int gold = m.getGold();
        assertTrue(m.recruitRandom());
        assertEquals(gold - GameModel.RANDOM_MAGE_COST, m.getGold()); // prix fixe
        assertEquals(GameModel.MAGE_COST + 4, m.getMageCost());       // et pas compté
        m.startLevel(2);
        assertEquals(GameModel.MAGE_COST, m.getMageCost());           // nouvelle partie : prix de départ
    }

    @Test
    public void firstWave_waitsForThePlayer() {
        GameModel m = newModel();
        m.startLevel(1);
        assertTrue(m.isWaitingForWave());
        for (int i = 0; i < 300; i++) {
            m.update();
        }
        assertTrue(m.getEnemies().isEmpty()); // personne n'arrive tant que la vague n'est pas lancée
        assertEquals(1, m.getWave());
        assertTrue(m.launchWave());
        assertFalse(m.launchWave());
        for (int i = 0; i < 5; i++) {
            m.update();
        }
        assertFalse(m.getEnemies().isEmpty());
    }

    @Test
    public void ultimate_hasOneMinuteCooldown() {
        GameModel m = withRun(0, 1f, 100, 3);
        m.launchWave();
        assertTrue(m.isUltimateReady());
        assertTrue(m.triggerUltimate());
        int[] data = m.exportRun();
        data[7] = 100; // jauge de nouveau pleine...
        GameModel other = newModel();
        assertTrue(other.importRun(data));
        other.resume();
        assertFalse(other.isUltimateReady()); // ...mais la minute de recharge n'est pas passée
        assertTrue(other.getUltimateCooldownSeconds() > 50);
        for (int i = 0; i < GameModel.ULTIMATE_COOLDOWN; i++) {
            other.update();
        }
        assertTrue(other.getState() != GameState.PLAYING || other.getUltimateCooldown() == 0);
    }

    @Test
    public void heal_doesNotGiveBackLostStars() {
        GameModel m = withRun(1000, 0.5f, 0, 2);
        assertEquals(2, m.getStars());
        assertTrue(m.buyHeal()); // 50 % → 75 %
        assertTrue(m.getCastleHpRatio() >= GameModel.STARS_3_MIN);
        assertEquals(2, m.getStars());
    }

    @Test
    public void autoMode_mustBeBought() {
        GameModel m = newModel();
        m.setAutoMode(true);
        assertFalse(m.isAutoMode());
        m.toggleAutoMode();
        assertFalse(m.isAutoMode());
        m.getProgression().addGems(Progression.AUTO_PRICE);
        assertTrue(m.buyAuto());
        assertTrue(m.isAutoMode());
        assertEquals(0, m.getProgression().getGems());
        assertFalse(m.buyAuto());
    }

    @Test
    public void everyFight_startsAtSpeedOne() {
        GameModel m = newModel();
        m.getProgression().loadCastle(0, 0, true, true);
        m.startLevel(1);
        m.setGameSpeed(3);
        assertEquals(3, m.getGameSpeed());
        m.startLevel(2);
        assertEquals(1, m.getGameSpeed());
    }
}
