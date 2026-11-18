package com.example.magicdefense;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.example.magicdefense.model.Board;
import com.example.magicdefense.model.Chest;
import com.example.magicdefense.model.GameModel;
import com.example.magicdefense.model.GameState;
import com.example.magicdefense.model.LevelSpec;
import com.example.magicdefense.model.Mage;
import com.example.magicdefense.model.Progression;
import com.example.magicdefense.model.Rarity;

import org.junit.Test;

import java.util.Random;

// Système de cartes : raretés, 3 coffres, déblocage, amélioration (cartes + gemmes), équipe,
// achat seulement quand la réserve est vide, coffres gratuits, code, récompenses du mode infini
public class CardSystemTest {

    private static GameModel newModel() {
        Board board = new Board(1f);
        board.resize(400, 800);
        return new GameModel(board);
    }

    @Test
    public void rarities_matchTheDesign() {
        assertEquals(Rarity.COMMON, Rarity.of(Mage.FIRE));
        assertEquals(Rarity.COMMON, Rarity.of(Mage.EARTH));
        assertEquals(Rarity.RARE, Rarity.of(Mage.POISON));
        assertEquals(Rarity.RARE, Rarity.of(Mage.WATER));
        assertEquals(Rarity.EPIC, Rarity.of(Mage.AIR));
        assertEquals(Rarity.EPIC, Rarity.of(Mage.SUMMONER));
        assertEquals(Rarity.EPIC, Rarity.of(Mage.WORKER));
        assertEquals(Rarity.EPIC, Rarity.of(Mage.LIGHTNING));
        assertEquals(Rarity.LEGENDARY, Rarity.of(Mage.DARK));
        assertEquals(Rarity.LEGENDARY, Rarity.of(Mage.LIGHT));
    }

    @Test
    public void chestOdds_sumToAtMost100Percent() {
        for (int c = 0; c < Chest.COUNT; c++) {
            for (int s = 0; s < Chest.slots(c); s++) {
                int sum = 0;
                for (int r = 0; r < Rarity.COUNT; r++) {
                    sum += Chest.odds(c, s, r);
                }
                assertTrue(sum <= 1000);
            }
        }
        assertEquals(2, Chest.slots(Chest.SMALL));
        assertEquals(3, Chest.slots(Chest.BIG));
        assertEquals(4, Chest.slots(Chest.MYSTERY));
        assertEquals("50 % C · 25 % R · 25 % rien", Chest.oddsText(Chest.SMALL, 1));
        assertEquals("30 % C · 60 % R · 9,5 % É · 0,5 % L", Chest.oddsText(Chest.BIG, 1));
    }

    @Test
    public void chestLoot_respectsSizesAndDistinctTypes() {
        Random r = new Random(11);
        for (int c = 0; c < Chest.COUNT; c++) {
            for (int k = 0; k < 300; k++) {
                Chest.Loot loot = Chest.roll(c, r);
                int total = 0;
                boolean[] seen = new boolean[Mage.TYPE_COUNT];
                for (int s = 0; s < loot.types.length; s++) {
                    if (loot.types[s] < 0) {
                        continue;
                    }
                    assertFalse("types différents", seen[loot.types[s]]);
                    seen[loot.types[s]] = true;
                    assertTrue(loot.amounts[s] >= 1);
                    total += loot.amounts[s];
                }
                assertTrue(total >= Chest.cards(c) - Chest.spread(c) && total <= Chest.cards(c) + Chest.spread(c));
                assertTrue(loot.gems >= Chest.gemsMin(c) && loot.gems <= Chest.gemsMax(c));
                if (c != Chest.MYSTERY) {
                    assertEquals(Rarity.COMMON, Rarity.of(loot.types[0])); // 1re carte : commune garantie
                }
            }
        }
    }

    @Test
    public void smallChest_sometimesHasOnlyOneType() {
        Random r = new Random(5);
        int single = 0;
        for (int k = 0; k < 400; k++) {
            if (Chest.roll(Chest.SMALL, r).types[1] < 0) {
                single++;
            }
        }
        assertTrue(single > 60 && single < 140); // ≈ 25 %
    }

    @Test
    public void buyChests_onlyWhenNoneLeft() {
        Progression p = new Progression();
        p.addGems(1000);
        assertTrue(p.buyChests(Chest.SMALL, 5));
        assertEquals(1000 - 5 * 35, p.getGems());
        assertEquals(5, p.getChests(Chest.SMALL));
        assertFalse(p.buyChests(Chest.SMALL, 1)); // il en reste : pas d'achat
        assertTrue(p.buyChests(Chest.BIG, 1));    // une autre sorte : possible
        assertEquals(5, p.openChests(Chest.SMALL, Integer.MAX_VALUE, new Random(1))); // tout ouvrir
        assertEquals(0, p.getChests(Chest.SMALL));
        assertTrue(p.buyChests(Chest.SMALL, 10));
    }

    @Test
    public void openingChest_unlocksNewMages_andJoinsFreeTeamSlot() {
        Progression p = new Progression();
        p.load(0, 0, new int[Mage.TYPE_COUNT], new int[0], null, new int[]{Mage.WATER, -1, -1, -1}, 0);
        p.addChests(Chest.MYSTERY, 30);
        p.openChests(Chest.MYSTERY, 30, new Random(2));
        int newOnes = 0;
        for (int t = Mage.BASE_TYPES; t < Mage.TYPE_COUNT; t++) {
            if (p.isOwned(t)) {
                newOnes++;
                assertTrue(p.getFragments(t) > 0);
            }
        }
        assertTrue(newOnes > 0);
        assertTrue(p.teamCount() > 1); // les nouveaux mages ont rempli les places libres
    }

    @Test
    public void chestCards_sortedGemsFirstThenRarity() {
        GameModel m = newModel();
        m.getProgression().addChests(Chest.MYSTERY, 1);
        assertTrue(m.openChests(Chest.MYSTERY, 1));
        assertEquals(GameState.CHEST_OPENING, m.getState());
        int n = m.getChestCardCount();
        assertEquals(GameModel.CARD_GEMS, m.getChestCardType(0));
        for (int i = 2; i < n; i++) {
            assertTrue(Rarity.of(m.getChestCardType(i - 1)) <= Rarity.of(m.getChestCardType(i)));
        }
    }

    @Test
    public void upgrade_costsCardsAndGems_lessCardsForRarerMages() {
        Progression p = new Progression();
        assertTrue(p.upgradeCost(Mage.FIRE) > p.upgradeCost(Mage.WATER));
        int[] frags = new int[Mage.TYPE_COUNT];
        frags[Mage.FIRE] = 100;
        p.load(0, 0, frags, new int[0], null, null, 0);
        assertTrue(p.hasCardsToUpgrade(Mage.FIRE));
        assertFalse(p.canUpgrade(Mage.FIRE)); // pas de gemmes
        p.addGems(p.upgradeGemCost(Mage.FIRE));
        assertTrue(p.upgrade(Mage.FIRE));
        assertEquals(2, p.getRank(Mage.FIRE));
        assertEquals(0, p.getGems());
        assertEquals(100 - 10, p.getFragments(Mage.FIRE));
    }

    @Test
    public void team_useMageSwapsWhenFull() {
        GameModel m = newModel();
        Progression p = m.getProgression();
        boolean[] owned = new boolean[Mage.TYPE_COUNT];
        owned[Mage.POISON] = true;
        p.load(0, 0, new int[Mage.TYPE_COUNT], new int[0], owned, null, 0);
        assertTrue(p.isTeamFull());
        m.setHomePage(GameModel.PAGE_MAGES);
        m.selectMage(Mage.POISON);
        assertEquals(Mage.POISON, m.getSelectedMage());
        assertTrue(m.useMage(Mage.POISON));
        assertEquals(Mage.POISON, m.getSwappingMage()); // l'équipe est pleine : on choisit qui remplacer
        assertTrue(m.chooseTeamSlot(1));
        assertEquals(Mage.POISON, p.getTeamSlot(1));
        assertFalse(p.isInTeam(Mage.FIRE));
        assertArrayEquals(new int[]{Mage.FIRE}, new int[]{m.collectionTypes()[0]});
        assertTrue(p.setTeamSlot(0, Mage.POISON)); // déjà dans l'équipe : les deux places s'échangent
        assertEquals(Mage.WATER, p.getTeamSlot(1));
    }

    @Test
    public void dailyChests_onceADay() {
        Progression p = new Progression();
        assertTrue(p.claimDaily(100));
        assertEquals(2, p.getChests(Chest.SMALL));
        assertFalse(p.claimDaily(100));
        assertTrue(p.claimDaily(101));
        assertEquals(4, p.getChests(Chest.SMALL));
    }

    @Test
    public void giftCode_onlyOnce() {
        Progression p = new Progression();
        assertEquals(Progression.CODE_WRONG, p.redeemCode("bonjour"));
        assertEquals(Progression.CODE_OK, p.redeemCode("thomasleboss"));
        assertEquals(9999, p.getGems());
        assertEquals(Progression.CODE_ALREADY_USED, p.redeemCode("thomasleboss"));
        assertEquals(9999, p.getGems());
    }

    @Test
    public void endlessRewards_followTheCycles() {
        // Facile : petit aux vagues 5, 15 ; grand aux vagues 10, 20
        assertEquals(Chest.SMALL, Progression.chestForEndlessWave(5, LevelSpec.EASY));
        assertEquals(Chest.BIG, Progression.chestForEndlessWave(10, LevelSpec.EASY));
        assertEquals(-1, Progression.chestForEndlessWave(7, LevelSpec.EASY));
        // Normal : toutes les 3 vagues ; 4e, 8e, 12e grands, 15e mystérieux, puis nouveau cycle
        assertEquals(Chest.SMALL, Progression.chestForEndlessWave(3, LevelSpec.NORMAL));
        assertEquals(Chest.BIG, Progression.chestForEndlessWave(12, LevelSpec.NORMAL));
        assertEquals(Chest.BIG, Progression.chestForEndlessWave(36, LevelSpec.NORMAL));
        assertEquals(Chest.MYSTERY, Progression.chestForEndlessWave(45, LevelSpec.NORMAL));
        assertEquals(Chest.SMALL, Progression.chestForEndlessWave(48, LevelSpec.NORMAL));
        assertEquals(Chest.BIG, Progression.chestForEndlessWave(57, LevelSpec.NORMAL)); // 4e du 2e cycle
        // Difficile : toutes les 2 vagues ; 4e grand, 8e mystérieux
        assertEquals(Chest.BIG, Progression.chestForEndlessWave(8, LevelSpec.HARD));
        assertEquals(Chest.MYSTERY, Progression.chestForEndlessWave(16, LevelSpec.HARD));
        assertEquals(Chest.SMALL, Progression.chestForEndlessWave(18, LevelSpec.HARD));
        // Infernal : un grand toutes les 3 vagues, le 6e mystérieux
        assertEquals(Chest.BIG, Progression.chestForEndlessWave(3, LevelSpec.INFERNAL));
        assertEquals(Chest.MYSTERY, Progression.chestForEndlessWave(18, LevelSpec.INFERNAL));
        assertEquals(Chest.BIG, Progression.chestForEndlessWave(21, LevelSpec.INFERNAL));
        int[] normal45 = Progression.chestsForEndless(45, LevelSpec.NORMAL);
        assertArrayEquals(new int[]{11, 3, 1}, normal45);
        assertNotEquals(0, Progression.chestsForEndless(20, LevelSpec.HARD)[Chest.SMALL]);
    }
}
