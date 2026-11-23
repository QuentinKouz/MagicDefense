package com.example.magicdefense;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.magicdefense.model.Board;
import com.example.magicdefense.model.GameModel;
import com.example.magicdefense.model.LevelSpec;
import com.example.magicdefense.model.Mage;
import com.example.magicdefense.model.Progression;

import org.junit.Test;

// Niveaux 6 (archimage) et 7 (sage), orbe de sagesse, montée de niveau avec des PO, compétences BOOST
public class SageLevelsTest {

    private static GameModel newModel() {
        Board board = new Board(1f);
        board.resize(400, 800);
        return new GameModel(board);
    }

    // Partie en cours avec des mages placés (type, niveau) sur la première rangée et `gold` PO
    private static GameModel withMages(GameModel m, int gold, int[][] mages) {
        m.startLevel(1);
        int[] run = m.exportRun();
        run[5] = gold;
        for (int i = 0; i < mages.length; i++) {
            int cell = 16 + i * 3; // rangée 0, colonne i
            run[cell] = mages[i][0];
            run[cell + 1] = mages[i][1];
            run[cell + 2] = 0;
        }
        assertTrue(m.importRun(run));
        m.resume();
        return m;
    }

    @Test
    public void merge_goesUpToLevel6_withoutOrb() {
        GameModel m = withMages(newModel(), 0, new int[][]{{Mage.FIRE, 5}, {Mage.FIRE, 5}});
        assertTrue(m.canMerge(m.getMage(0, 0), m.getMage(0, 1)));
        m.dropMage(0, 0, 0, 1);
        assertNull(m.getMage(0, 0));
        assertEquals(6, m.getMage(0, 1).level);
        assertTrue(m.getProgression().hasMaxLevelMage()); // le succès « mage de niveau 5 » reste valable
    }

    @Test
    public void merge_toLevel7_blockedWithoutOrb_allowedWithOrb() {
        GameModel m = withMages(newModel(), 0, new int[][]{{Mage.WATER, 6}, {Mage.WATER, 6}});
        assertFalse(m.canMerge(m.getMage(0, 0), m.getMage(0, 1)));
        m.dropMage(0, 0, 0, 1);
        assertEquals(6, m.getMage(0, 1).level);  // rien n'a changé
        assertEquals(6, m.getMage(0, 0).level);

        GameModel m2 = newModel();
        Progression p = m2.getProgression();
        p.addWisdomOrbs(1);
        assertTrue(p.placeWisdomOrb(Mage.WATER));
        withMages(m2, 0, new int[][]{{Mage.WATER, 6}, {Mage.WATER, 6}, {Mage.FIRE, 6}, {Mage.FIRE, 6}});
        assertTrue(m2.canMerge(m2.getMage(0, 0), m2.getMage(0, 1)));
        assertFalse("l'orbe ne vaut que pour son élément", m2.canMerge(m2.getMage(0, 2), m2.getMage(0, 3)));
        m2.dropMage(0, 0, 0, 1);
        Mage sage = m2.getMage(0, 1);
        assertEquals(Mage.SAGE_LEVEL, sage.level);
        assertTrue(sage.isSage());
        // Niveau 7 = maximum : plus de fusion
        assertFalse(m2.canReachLevel(sage, 8));
    }

    @Test
    public void levelUpCosts_doubleEachLevel() {
        int[] expected = {100, 200, 400, 800, 1600, 3200};
        for (int lv = 1; lv <= 6; lv++) {
            assertEquals(expected[lv - 1], GameModel.levelUpCost(lv));
        }
        assertEquals(-1, GameModel.levelUpCost(7));
    }

    @Test
    public void levelUpWithGold_paysAndNeedsOrbForLevel7() {
        GameModel m = withMages(newModel(), 100 + 200 + 50, new int[][]{{Mage.EARTH, 1}});
        assertEquals(GameModel.LEVEL_UP_OK, m.levelUpStatus(0, 0));
        assertTrue(m.levelUpMage(0, 0));
        assertEquals(2, m.getMage(0, 0).level);
        assertEquals(250, m.getGold());
        assertTrue(m.levelUpMage(0, 0));
        assertEquals(3, m.getMage(0, 0).level);
        assertEquals(50, m.getGold());
        assertEquals(GameModel.LEVEL_UP_NO_GOLD, m.levelUpStatus(0, 0));
        assertFalse(m.levelUpMage(0, 0));
        assertEquals(3, m.getMage(0, 0).level);

        // 6 → 7 : orbe obligatoire, même avec assez de PO
        GameModel m2 = withMages(newModel(), 10000, new int[][]{{Mage.LIGHTNING, 6}});
        assertEquals(GameModel.LEVEL_UP_NO_ORB, m2.levelUpStatus(0, 0));
        assertFalse(m2.levelUpMage(0, 0));
        assertEquals(10000, m2.getGold());

        GameModel m3 = newModel();
        m3.getProgression().addWisdomOrbs(1);
        assertTrue(m3.getProgression().placeWisdomOrb(Mage.LIGHTNING));
        withMages(m3, 10000, new int[][]{{Mage.LIGHTNING, 6}});
        assertTrue(m3.levelUpMage(0, 0));
        assertEquals(7, m3.getMage(0, 0).level);
        assertEquals(10000 - 3200, m3.getGold());
        assertEquals(GameModel.LEVEL_UP_MAX, m3.levelUpStatus(0, 0));
    }

    @Test
    public void levelUpBubble_opensOnMage_andCloses() {
        GameModel m = withMages(newModel(), 0, new int[][]{{Mage.FIRE, 2}});
        m.openLevelUp(0, 0);
        assertEquals(0, m.getLevelUpRow());
        assertEquals(0, m.getLevelUpCol());
        m.closeLevelUp();
        assertEquals(-1, m.getLevelUpRow());
        m.openLevelUp(0, 3); // case vide : pas de bulle
        assertEquals(-1, m.getLevelUpRow());
    }

    @Test
    public void wisdomOrb_onePerDifficultyAtWave1000_onlyOnce() {
        Progression p = new Progression();
        assertEquals(0, p.getWisdomOrbs());
        assertFalse(p.claimWisdomOrb(999, LevelSpec.EASY));
        assertTrue(p.claimWisdomOrb(Progression.WISDOM_ORB_WAVE, LevelSpec.EASY));
        assertTrue(p.isWisdomOrbEarned(LevelSpec.EASY));
        assertFalse("une seule fois par difficulté", p.claimWisdomOrb(1000, LevelSpec.EASY));
        assertFalse(p.claimWisdomOrb(1001, LevelSpec.EASY));
        assertEquals(1, p.getWisdomOrbs());
        for (int d = 0; d < LevelSpec.DIFFICULTY_COUNT; d++) {
            p.claimWisdomOrb(1000, d);
        }
        assertEquals(4, p.getWisdomOrbs());
        for (int d = 0; d < LevelSpec.DIFFICULTY_COUNT; d++) {
            assertTrue(p.isWisdomOrbEarned(d));
        }
    }

    @Test
    public void wisdomOrb_grantedByEndlessWave1000InGame() {
        GameModel m = newModel();
        m.setEndlessDifficulty(LevelSpec.HARD);
        m.startEndless();
        m.endlessWaveCleared(999);
        assertEquals(0, m.getProgression().getWisdomOrbs());
        m.endlessWaveCleared(1000);
        assertEquals(1, m.getProgression().getWisdomOrbs());
        assertTrue(m.getProgression().isWisdomOrbEarned(LevelSpec.HARD));
        assertFalse(m.getProgression().isWisdomOrbEarned(LevelSpec.NORMAL));
        assertTrue(m.getAnnounce() > 0);
        m.endlessWaveCleared(1000);
        assertEquals(1, m.getProgression().getWisdomOrbs());
    }

    @Test
    public void wisdomOrb_placeConsumesInventory_andCanBeRemoved() {
        GameModel m = newModel();
        Progression p = m.getProgression();
        assertFalse(m.toggleWisdomOrb(Mage.FIRE)); // aucune orbe en réserve
        p.addWisdomOrbs(2);
        assertTrue(m.toggleWisdomOrb(Mage.FIRE));
        assertTrue(p.hasWisdomOrb(Mage.FIRE));
        assertEquals(1, p.getWisdomOrbs());
        assertFalse("pas deux orbes sur le même mage", p.placeWisdomOrb(Mage.FIRE));
        assertFalse("mage pas encore possédé", p.placeWisdomOrb(Mage.DARK));
        assertTrue(m.toggleWisdomOrb(Mage.FIRE)); // retirée : retour en réserve
        assertFalse(p.hasWisdomOrb(Mage.FIRE));
        assertEquals(2, p.getWisdomOrbs());
    }

    @Test
    public void stats_growFrom5To6To7() {
        for (int t = 0; t < Mage.TYPE_COUNT; t++) {
            Mage m5 = new Mage(t);
            m5.level = 5;
            Mage m6 = new Mage(t);
            m6.level = 6;
            Mage m7 = new Mage(t);
            m7.level = 7;
            assertTrue(m6.getDamage() > m5.getDamage() * 1.3f);
            assertTrue("le sage est beaucoup plus fort", m7.getDamage() > m6.getDamage() * 1.4f);
            assertTrue(m7.getFireDelay() < m5.getFireDelay());
            assertTrue(m6.hasSkill() && m7.hasSkill());
        }
        Mage w5 = new Mage(Mage.WATER);
        w5.level = 5;
        Mage w7 = new Mage(Mage.WATER);
        w7.level = 7;
        assertTrue(w7.getAoeRadius() > w5.getAoeRadius());
        Mage s7 = new Mage(Mage.SUMMONER);
        s7.level = 7;
        assertEquals(5, s7.getMaxKnights());
        Mage l7 = new Mage(Mage.LIGHT);
        l7.level = 7;
        Mage l5 = new Mage(Mage.LIGHT);
        l5.level = 5;
        assertTrue(l7.getAuraBonus() > l5.getAuraBonus());
    }

    @Test
    public void sageSkill_isBoosted_withDistinctNameAndShorterCooldown() {
        Mage m5 = new Mage(Mage.WATER);
        m5.level = 5;
        Mage m6 = new Mage(Mage.WATER);
        m6.level = 6;
        Mage m7 = new Mage(Mage.WATER);
        m7.level = 7;
        assertFalse(m5.isSkillBoosted());
        assertFalse(m6.isSkillBoosted());
        assertTrue(m7.isSkillBoosted());
        assertEquals("Raz-de-marée", m6.getSkillName());
        assertEquals("Déluge", m7.getSkillName());
        assertTrue(m7.getSkillCooldown() < m5.getSkillCooldown());
        assertEquals(Mage.TYPE_COUNT, Mage.BOOST_SKILL_NAMES.length);
        assertEquals(Mage.TYPE_COUNT, Mage.BOOST_SKILL_DESCRIPTIONS.length);
        for (int t = 0; t < Mage.TYPE_COUNT; t++) {
            assertNotEquals(Mage.SKILL_NAMES[t], Mage.BOOST_SKILL_NAMES[t]);
        }
    }

    @Test
    public void savedRun_keepsLevel7_onlyWithOrb() {
        GameModel m = newModel();
        m.getProgression().addWisdomOrbs(1);
        m.getProgression().placeWisdomOrb(Mage.FIRE);
        withMages(m, 0, new int[][]{{Mage.FIRE, 7}, {Mage.EARTH, 7}, {Mage.WATER, 9}});
        assertEquals(7, m.getMage(0, 0).level);
        assertEquals("sans orbe : ramené au niveau 6", 6, m.getMage(0, 1).level);
        assertEquals(6, m.getMage(0, 2).level);
        int[] run = m.exportRun();
        GameModel copy = newModel();
        copy.getProgression().addWisdomOrbs(1);
        copy.getProgression().placeWisdomOrb(Mage.FIRE);
        assertTrue(copy.importRun(run));
        assertEquals(7, copy.getMage(0, 0).level);
    }
}
