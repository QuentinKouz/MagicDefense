package com.example.magicdefense;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.magicdefense.model.Achievement;
import com.example.magicdefense.model.Artifact;
import com.example.magicdefense.model.Board;
import com.example.magicdefense.model.GameModel;
import com.example.magicdefense.model.GameState;
import com.example.magicdefense.model.LevelSpec;
import com.example.magicdefense.model.Mage;
import com.example.magicdefense.model.Progression;
import com.example.magicdefense.model.Ultimate;

import org.junit.Test;

import java.util.Random;

// Mise à jour : ultimes, château, artefacts, succès, difficultés, cases bloquées, soins, chevaliers, reprise
public class UpdateFeaturesTest {

    private static GameModel newModel() {
        Board board = new Board(1f);
        board.resize(400, 800);
        return new GameModel(board);
    }

    @Test
    public void prices_areMuchHigher() {
        assertEquals(60, Progression.price(Mage.POISON));
        assertEquals(100, Progression.price(Mage.AIR));
        assertEquals(160, Progression.price(Mage.SUMMONER));
        assertEquals(0, Ultimate.price(Ultimate.STUN));
        assertTrue(Ultimate.price(Ultimate.METEOR) > Ultimate.price(Ultimate.FURY));
        assertEquals(10, Progression.CHEST_PRICE);
        assertEquals(300, Progression.SPEED3_PRICE);
    }

    @Test
    public void ultimates_buyAndEquip() {
        Progression p = new Progression();
        assertTrue(p.ownsUltimate(Ultimate.STUN));
        assertFalse(p.buyUltimate(Ultimate.METEOR)); // pas de gemmes
        p.addGems(500);
        assertTrue(p.buyUltimate(Ultimate.METEOR));
        assertEquals(Ultimate.METEOR, p.getEquippedUltimate()); // équipé tout de suite
        assertFalse(p.buyUltimate(Ultimate.METEOR));          // déjà acheté
        assertTrue(p.equipUltimate(Ultimate.STUN));
        assertFalse(p.equipUltimate(Ultimate.SHIELD));         // pas possédé
    }

    @Test
    public void speed3_needsPurchase() {
        GameModel m = newModel();
        m.setGameSpeed(3);
        assertEquals(2, m.getGameSpeed());
        m.cycleGameSpeed();
        assertEquals(1, m.getGameSpeed());
        m.getProgression().addGems(300);
        assertTrue(m.buySpeed3());
        m.setGameSpeed(3);
        assertEquals(3, m.getGameSpeed());
    }

    @Test
    public void chests_buyAndOpenSeveral() {
        Progression p = new Progression();
        p.addGems(25);
        assertTrue(p.buyChest());
        assertTrue(p.buyChest());
        assertFalse(p.buyChest());
        assertEquals(5, p.getGems());
        p.addChests(8);
        assertEquals(5, p.openChests(5, new Random(3)));
        assertEquals(5, p.getChests());
        assertEquals(5, p.getLastChestCount());
        assertEquals(5, p.openChests(10, new Random(3))); // ×10 : ouvre ce qu'il reste
        assertEquals(0, p.getChests());
        assertEquals(10, p.getChestsOpened());
    }

    @Test
    public void castle_upgrades() {
        Progression p = new Progression();
        p.addGems(1000);
        assertEquals(1f, p.castleHpMultiplier(), 1e-6);
        assertTrue(p.upgradeCastleHp());
        assertTrue(p.upgradeCastleRes());
        assertEquals(1.1f, p.castleHpMultiplier(), 1e-6);
        assertEquals(0.97f, p.castleDamageFactor(), 1e-6);
        GameModel m = newModel();
        m.getProgression().loadCastle(5, 0, false);
        m.startLevel(1);
        assertEquals(15000, m.getCastleMaxHp());
    }

    @Test
    public void artifacts_grantCycleAndModify() {
        Progression p = new Progression();
        Random r = new Random(1);
        int id = p.grantRandomArtifact(r);
        assertNotEquals(Artifact.NONE, id);
        assertTrue(p.isOwned(Artifact.typeOf(id)));            // un mage possédé en priorité
        int type = Artifact.typeOf(id);
        assertEquals(id, p.getEquippedArtifact(type));          // équipé automatiquement
        assertTrue(p.cycleArtifact(type));
        assertEquals(Artifact.NONE, p.getEquippedArtifact(type)); // un seul : retour à « aucun »
        for (int i = 0; i < Artifact.COUNT; i++) {
            p.grantRandomArtifact(r);
        }
        assertEquals(Artifact.COUNT, p.artifactCount());
        assertEquals(Artifact.NONE, p.grantRandomArtifact(r)); // tous obtenus

        // Baguette de corail : cadence plus rapide, dégâts réduits
        Mage plain = new Mage(Mage.WATER, 1);
        Mage wand = new Mage(Mage.WATER, 1, Artifact.id(Mage.WATER, 0));
        assertTrue(wand.getFireDelay() < plain.getFireDelay());
        assertTrue(wand.getDamage() < plain.getDamage());
        // Un artefact d'un autre élément est ignoré
        assertEquals(Artifact.NONE, new Mage(Mage.FIRE, 1, Artifact.id(Mage.WATER, 0)).artifact);
        // Fronde de granit : un projectile de plus
        assertEquals(2, new Mage(Mage.EARTH, 1, Artifact.id(Mage.EARTH, 0)).getShots());
    }

    @Test
    public void endless_difficulties() {
        LevelSpec easy = LevelSpec.endless(LevelSpec.EASY);
        LevelSpec infernal = LevelSpec.endless(LevelSpec.INFERNAL);
        assertTrue(infernal.difficulty(10) > LevelSpec.endless().difficulty(10));
        assertTrue(easy.difficulty(10) < LevelSpec.endless().difficulty(10));
        assertEquals(1, Progression.chestsForEndless(5, LevelSpec.EASY));
        assertEquals(3, Progression.chestsForEndless(6, LevelSpec.INFERNAL));
        Progression p = new Progression();
        assertTrue(p.recordEndlessWave(12, LevelSpec.HARD));
        assertEquals(12, p.getBestEndlessWave(LevelSpec.HARD));
        assertEquals(0, p.getBestEndlessWave(LevelSpec.NORMAL));
        assertEquals(12, p.getBestEndlessWave());
    }

    @Test
    public void night_levelsAndEndlessCycle() {
        assertFalse(LevelSpec.forLevel(3).nightLevel);  // niveaux faits main : de jour
        assertTrue(LevelSpec.forLevel(7).nightLevel);
        assertTrue(LevelSpec.forLevel(11).nightLevel);
        assertFalse(LevelSpec.forLevel(8).nightLevel);
        LevelSpec e = LevelSpec.endless();
        assertFalse(e.isNight(1));
        assertTrue(e.isNight(3));
        assertTrue(e.isNight(6));
        GameModel m = newModel();
        m.startLevel(7);
        assertTrue(m.isNightMode()); // nuit forcée même sans capteur
    }

    @Test
    public void lockedCells_heal_randomMage() {
        GameModel m = newModel();
        m.startLevel(1);
        for (int col = 0; col < Board.COLS; col++) {
            assertTrue(m.isLocked(GameModel.LOCKED_ROW, col));
        }
        assertFalse(m.unlockCell(GameModel.LOCKED_ROW, 0)); // 20 PO < 30
        // Le recrutement ne place jamais de mage sur une case bloquée
        m.recruitMage(Mage.FIRE);
        m.recruitMage(Mage.FIRE);
        for (int col = 0; col < Board.COLS; col++) {
            assertEquals(null, m.getMage(GameModel.LOCKED_ROW, col));
        }
        assertEquals(0, m.getGold());
        assertFalse(m.canHeal()); // PV au maximum : pas de soin
        assertFalse(m.recruitRandom());
    }

    @Test
    public void runSnapshot_roundTrip() {
        GameModel m = newModel();
        m.startLevel(2);
        m.recruitMage(Mage.WATER);
        int[] data = m.exportRun();
        GameModel other = newModel();
        assertTrue(other.importRun(data));
        assertEquals(GameState.PAUSED, other.getState());
        assertTrue(other.isResumed());
        assertEquals(2, other.getLevel());
        assertEquals(m.getGold(), other.getGold());
        int mages = 0;
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                if (other.getMage(row, col) != null) {
                    mages++;
                }
            }
        }
        assertEquals(1, mages);
        assertFalse(other.importRun(new int[]{99}));
    }

    @Test
    public void quitToMenu_endsRun() {
        GameModel m = newModel();
        m.startLevel(1);
        assertTrue(m.hasRunInProgress());
        m.quitToMenu();
        assertFalse(m.hasRunInProgress());
        assertEquals(GameModel.PAGE_CAMPAIGN, m.getHomePage());
    }

    @Test
    public void achievements_giveGemsOnce() {
        GameModel m = newModel();
        Progression p = m.getProgression();
        p.loadStats(600, 0, 0, 0, 0, 0, false, 0, new boolean[0]);
        int gems = p.getGems();
        m.checkAchievements();
        int id = 4; // Chasseur de gobelins : 500 gobelins
        assertEquals(Achievement.STAT_KILLS, Achievement.stat(id));
        assertTrue(p.isAchieved(id));
        assertEquals(gems + Achievement.reward(id), p.getGems());
        m.checkAchievements();
        assertEquals(gems + Achievement.reward(id), p.getGems()); // pas deux fois
    }

    @Test
    public void summoner_spawnsKnights_andSkillsHaveCooldown() {
        Mage s = new Mage(Mage.SUMMONER, 1);
        assertEquals(1, s.getMaxKnights());
        s.level = 5;
        assertEquals(3, s.getMaxKnights());
        assertTrue(s.getKnightHp() > s.getKnightDamage());
        assertTrue(s.isSkillReady());
        assertFalse(new Mage(Mage.FIRE, 1).hasSkill());
        Mage grimoire = new Mage(Mage.FIRE, 1, Artifact.id(Mage.FIRE, 2));
        assertTrue(grimoire.getSkillCooldown() < new Mage(Mage.FIRE, 1).getSkillCooldown());
    }

    @Test
    public void knights_blockGoblinsInGame() {
        GameModel m = newModel();
        m.getProgression().load(0, 0, new int[7], new int[]{1, 1, 1, 1, 1, 1, 1},
                new boolean[]{true, true, true, true, true, true, true}, new int[]{6, 0, 1, 2}, 0);
        m.startLevel(1);
        m.recruitMage(Mage.SUMMONER);
        for (int i = 0; i < 5; i++) {
            m.update();
        }
        assertFalse(m.getKnights().isEmpty());
        assertNotNull(m.getKnights().get(0));
    }
}
