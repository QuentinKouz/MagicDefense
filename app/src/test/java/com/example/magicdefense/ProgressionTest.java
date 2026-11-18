package com.example.magicdefense;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.magicdefense.model.Enemy;
import com.example.magicdefense.model.LevelSpec;
import com.example.magicdefense.model.Mage;
import com.example.magicdefense.model.Progression;

import org.junit.Test;

import java.util.Random;

// Logique de la V2 : progression permanente, coffres, génération des niveaux
public class ProgressionTest {

    @Test
    public void upgradeCost_isProportionalToRank() {
        Progression p = new Progression();
        assertEquals(10, p.upgradeCost(Mage.FIRE));                // rang 1 → 2
        p.load(0, new int[]{0, 1000, 0, 0}, new int[]{1, 5, 1, 1}, 0);
        assertEquals(50, p.upgradeCost(Mage.FIRE));                // rang 5 → 6
        p.addGems(p.upgradeGemCost(Mage.FIRE));                     // + des gemmes
        assertTrue(p.upgrade(Mage.FIRE));
        assertEquals(6, p.getRank(Mage.FIRE));
        assertEquals(950, p.getFragments(Mage.FIRE));
        assertEquals(60, p.upgradeCost(Mage.FIRE));
    }

    @Test
    public void upgrade_refusedWithoutEnoughFragments() {
        Progression p = new Progression();
        p.load(0, new int[]{9, 0, 0, 0}, new int[]{1, 1, 1, 1}, 0);
        assertFalse(p.canUpgrade(Mage.WATER));
        assertFalse(p.upgrade(Mage.WATER));
        assertEquals(1, p.getRank(Mage.WATER));
        assertEquals(9, p.getFragments(Mage.WATER));
    }

    @Test
    public void upgrade_stopsAtMaxRank() {
        Progression p = new Progression();
        p.load(0, new int[]{0, 0, 0, Integer.MAX_VALUE / 2}, new int[]{1, 1, 1, Progression.MAX_RANK}, 0);
        assertFalse(p.upgrade(Mage.LIGHTNING));
        assertEquals(Progression.MAX_RANK, p.getRank(Mage.LIGHTNING));
    }

    @Test
    public void rankBonuses() {
        assertEquals(1f, Progression.damageMultiplier(1), 1e-6);
        assertEquals(2f, Progression.damageMultiplier(11), 1e-6);
        assertEquals(0f, Progression.speedBonus(1), 1e-6);
        assertEquals(0.10f, Progression.speedBonus(11), 1e-6);
        assertEquals(Progression.MAX_SPEED_BONUS, Progression.speedBonus(500), 1e-6); // plafonné
    }

    @Test
    public void mageStats_useRank() {
        Mage base = new Mage(Mage.FIRE, 1);
        Mage ranked = new Mage(Mage.FIRE, 11);
        assertEquals(base.getDamage() * 2f, ranked.getDamage(), 1e-4);
        assertTrue(ranked.getFireDelay() < base.getFireDelay());
        assertTrue(new Mage(Mage.LIGHTNING, Progression.MAX_RANK).getFireDelay() >= 1);
    }

    @Test
    public void chestsEarned() {
        assertEquals(1, Progression.chestsForLevel(1));
        assertEquals(1, Progression.chestsForLevel(4));
        assertEquals(2, Progression.chestsForLevel(5));
    }

    @Test
    public void endlessRecord() {
        Progression p = new Progression();
        assertTrue(p.recordEndlessWave(8));
        assertFalse(p.recordEndlessWave(5));
        assertEquals(8, p.getBestEndlessWave());
    }

    @Test
    public void handmadeLevels_unchanged() {
        for (int lv = 1; lv <= LevelSpec.HANDMADE_LEVELS; lv++) {
            LevelSpec s = LevelSpec.forLevel(lv);
            assertEquals(lv + 2, s.waves);
            assertFalse(s.isGenerated());
            // Uniquement des gobelins normaux, comme dans la V1
            Random r = new Random(42);
            for (int i = 0; i < 200; i++) {
                assertEquals(Enemy.NORMAL, s.rollKind(s.waves, r));
            }
            assertEquals(lv * (1 + 0.75f * (lv - 1)), s.difficulty(lv), 1e-4); // formule d'origine
        }
    }

    @Test
    public void generatedLevels_areDeterministicAndBounded() {
        for (int lv = 7; lv < 300; lv++) {
            LevelSpec a = LevelSpec.forLevel(lv);
            LevelSpec b = LevelSpec.forLevel(lv);
            assertTrue(a.isGenerated());
            assertEquals(a.waves, b.waves);
            assertEquals(a.profile, b.profile);
            assertTrue(a.waves >= 6 && a.waves <= 10);
            assertTrue(a.fastChance(a.waves) + a.tankChance(a.waves) < 1f);
            assertTrue(a.spawnDelay(1) >= 30);
        }
    }

    @Test
    public void endless_isInfiniteAndCapped() {
        LevelSpec e = LevelSpec.endless();
        assertTrue(e.endless);
        assertEquals(0, e.waves);
        assertEquals(LevelSpec.MAX_ENEMIES_PER_WAVE, e.enemiesInWave(1000));
        assertTrue(e.spawnDelay(1000) >= 24);
        assertTrue(e.difficulty(40) > e.difficulty(20));
    }
}