package com.example.magicdefense;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.magicdefense.model.Achievement;
import com.example.magicdefense.model.Board;
import com.example.magicdefense.model.GameModel;
import com.example.magicdefense.model.LevelSpec;
import com.example.magicdefense.model.Mage;
import com.example.magicdefense.model.Progression;
import com.example.magicdefense.model.Rarity;
import com.example.magicdefense.model.Roman;

import org.junit.Test;

// Retours joueurs : codes cadeaux, trésor de départ, succès à paliers sans fin, chiffres romains,
// prix des mages selon la rareté, titres des biomes par cycle
public class MetaFeaturesTest {

    private static GameModel newModel() {
        Board board = new Board(1f);
        board.resize(400, 800);
        return new GameModel(board);
    }

    // ---------- Codes cadeaux ----------
    @Test
    public void giftCodes_eachOncePerSave() {
        Progression p = new Progression();
        assertEquals(Progression.CODE_OK, p.redeemCode("thomasleboss2"));
        assertEquals(99999, p.getGems());
        assertEquals(Progression.CODE_ALREADY_USED, p.redeemCode("thomasleboss2"));
        assertEquals(Progression.CODE_OK, p.redeemCode("thomasleboss"));
        assertEquals(99999 + 9999, p.getGems());
        assertEquals(Progression.CODE_OK, p.redeemCode("  JeVeuxLesGrossesBoulesDeSagesse "));
        assertEquals(4, p.getWisdomOrbs());
        assertEquals(Progression.CODE_ALREADY_USED, p.redeemCode("jeveuxlesgrossesboulesdesagesse"));
        assertEquals(4, p.getWisdomOrbs());
        assertEquals(Progression.CODE_WRONG, p.redeemCode("thomasleboss3"));
    }

    @Test
    public void giftCodes_oldSaveKeepsFirstCodeUsed() {
        Progression p = new Progression();
        p.loadChests(0, 0, 0, -1, true); // ancienne sauvegarde : « thomasleboss » déjà utilisé
        assertTrue(p.isCodeUsed());
        assertEquals(Progression.CODE_ALREADY_USED, p.redeemCode("thomasleboss"));
        assertEquals(Progression.CODE_OK, p.redeemCode("thomasleboss2"));
        p.loadCodesUsed(new boolean[]{false, true, true});
        assertTrue(p.isCodeUsed(1));
        assertEquals(Progression.CODE_ALREADY_USED, p.redeemCode("jeveuxlesgrossesboulesdesagesse"));
    }

    @Test
    public void giftCodes_messageNamesTheReward() {
        assertTrue(Progression.giftCodeReward("thomasleboss2").contains("99999 gemmes"));
        assertTrue(Progression.giftCodeReward("thomasleboss").contains("9999 gemmes"));
        assertTrue(Progression.giftCodeReward("jeveuxlesgrossesboulesdesagesse").contains("4 orbes de sagesse"));
        assertEquals("", Progression.giftCodeReward("rien"));
        Progression p = new Progression();
        p.addWisdomOrbs(2);
        p.addWisdomOrbs(-5);
        assertEquals(2, p.getWisdomOrbs());
    }

    // ---------- Château : trésor de départ ----------
    @Test
    public void castleGold_costsAndLimit() {
        Progression p = new Progression();
        p.addGems(100000);
        int spent = 0;
        for (int lv = 0; lv < Progression.CASTLE_GOLD_MAX_LEVEL; lv++) {
            assertEquals(35 * (lv + 1), p.castleGoldCost());
            spent += p.castleGoldCost();
            assertTrue(p.upgradeCastleGold());
        }
        assertEquals(350, 35 * Progression.CASTLE_GOLD_MAX_LEVEL); // dernier prix
        assertEquals(100000 - spent, p.getGems());
        assertFalse(p.upgradeCastleGold()); // niveau 10 : maximum
        assertEquals(50, p.startGoldBonus());
    }

    @Test
    public void castleGold_addsGoldInCampaignAndEndless() {
        GameModel m = newModel();
        m.getProgression().addGems(1000);
        assertTrue(m.upgradeCastleGold());
        assertTrue(m.upgradeCastleGold());
        m.startLevel(1);
        assertEquals(GameModel.START_GOLD + 10, m.getGold());
        m.quitToMenu();
        m.startEndless();
        assertEquals(GameModel.START_GOLD + 10, m.getGold());
        Progression p = new Progression();
        p.loadCastleGold(99);
        assertEquals(Progression.CASTLE_GOLD_MAX_LEVEL, p.getCastleGoldLevel());
    }

    // ---------- Succès à paliers ----------
    @Test
    public void achievements_tiersAreEndless() {
        long[] kills = {500, 1000, 2500, 5000, 10000, 25000, 50000, 100000, 250000, 500000, 1000000};
        for (int t = 0; t < kills.length; t++) {
            assertEquals(kills[t], Achievement.target(Achievement.KILLS, t));
        }
        for (int f = 0; f < Achievement.COUNT; f++) {
            if (!Achievement.isTiered(f)) {
                continue;
            }
            for (int t = 1; t < 30 && t < Achievement.maxTiers(f); t++) {
                assertTrue(Achievement.target(f, t) > Achievement.target(f, t - 1));
                assertTrue(Achievement.reward(f, t) >= Achievement.reward(f, t - 1));
            }
            assertTrue(Achievement.reward(f, 20) > Achievement.reward(f, 2));
        }
        assertEquals("Chasseur de gobelins I", Achievement.name(Achievement.KILLS, 0));
        assertEquals("Chasseur de gobelins IV", Achievement.name(Achievement.KILLS, 3));
        assertEquals("Grand mage", Achievement.name(Achievement.MAX_LEVEL_MAGE, 0)); // unique : pas de chiffre
        assertFalse(Achievement.isTiered(Achievement.OWNED));
        assertEquals("Tuer 25 000 gobelins", Achievement.description(Achievement.KILLS, 5));
    }

    @Test
    public void achievements_severalTiersAtOnce_thenNextOneAppears() {
        GameModel m = newModel();
        Progression p = m.getProgression();
        p.loadStats(30000, 0, 0, 0, 0, 0, false, 0, new boolean[0]);
        int gems = p.getGems();
        m.checkAchievements();
        assertEquals(6, p.getAchievementTier(Achievement.KILLS)); // 500 ... 25 000
        int expected = 0;
        for (int t = 0; t < 6; t++) {
            expected += Achievement.reward(Achievement.KILLS, t);
        }
        assertEquals(gems + expected, p.getGems());
        m.checkAchievements();
        assertEquals(gems + expected, p.getGems()); // rien de plus avant 50 000
        assertFalse(p.isAchievementMaxed(Achievement.KILLS));
    }

    @Test
    public void achievements_uniqueOnlyOnce() {
        Progression p = new Progression();
        assertTrue(p.achieve(Achievement.MAX_LEVEL_MAGE));
        assertTrue(p.isAchievementMaxed(Achievement.MAX_LEVEL_MAGE));
        int gems = p.getGems();
        assertFalse(p.achieve(Achievement.MAX_LEVEL_MAGE));
        assertEquals(gems, p.getGems());
    }

    @Test
    public void achievements_oldSaveMigratesWithoutDoubleReward() {
        // Ancienne sauvegarde : niveaux 1 et 10, 500 et 5000 gobelins, mage niveau 5, posséder tous les mages
        boolean[] done = new boolean[Achievement.LEGACY_COUNT];
        done[0] = true;
        done[1] = true;
        done[4] = true;
        done[5] = true;
        done[10] = true;
        done[15] = true;
        GameModel m = newModel();
        Progression p = m.getProgression();
        p.loadStats(5200, 0, 0, 0, 0, 0, true, 0, done);
        assertEquals(2, p.getAchievementTier(Achievement.LEVELS));
        assertEquals(4, p.getAchievementTier(Achievement.KILLS)); // 500, 1000, 2500, 5000
        assertEquals(1, p.getAchievementTier(Achievement.MAX_LEVEL_MAGE));
        assertEquals(1, p.getAchievementTier(Achievement.OWNED));
        int gems = p.getGems();
        m.checkAchievements();
        assertEquals(gems, p.getGems()); // tout ce qui est atteint était déjà payé
        // Les paliers sauvegardés remplacent la conversion ; -1 la garde
        p.loadAchievementTiers(new int[]{-1, 7});
        assertEquals(2, p.getAchievementTier(Achievement.LEVELS));
        assertEquals(7, p.getAchievementTier(Achievement.KILLS));
        assertEquals(2 + 7 + 1 + 1, p.achievedCount());
    }

    // ---------- Chiffres romains ----------
    @Test
    public void roman_numerals() {
        assertEquals("I", Roman.of(1));
        assertEquals("II", Roman.of(2));
        assertEquals("IV", Roman.of(4));
        assertEquals("IX", Roman.of(9));
        assertEquals("XIV", Roman.of(14));
        assertEquals("XL", Roman.of(40));
        assertEquals("XC", Roman.of(90));
        assertEquals("CD", Roman.of(400));
        assertEquals("MCMXCIV", Roman.of(1994));
        assertEquals("MMMCMXCIX", Roman.of(3999));
        assertEquals("0", Roman.of(0));
        assertEquals("4000", Roman.of(4000));
    }

    // ---------- Prix des mages selon la rareté ----------
    @Test
    public void magePrice_dependsOnRarity() {
        assertEquals(10, Rarity.magePrice(Rarity.COMMON, 0));
        assertEquals(14, Rarity.magePrice(Rarity.RARE, 0));
        assertEquals(18, Rarity.magePrice(Rarity.EPIC, 0));
        assertEquals(25, Rarity.magePrice(Rarity.LEGENDARY, 0));
        assertEquals(10 + 2 * 3, Rarity.magePrice(Rarity.COMMON, 3));
        assertEquals(14 + 3 * 3, Rarity.magePrice(Rarity.RARE, 3));
        assertEquals(18 + 4 * 3, Rarity.magePrice(Rarity.EPIC, 3));
        assertEquals(25 + 6 * 3, Rarity.magePrice(Rarity.LEGENDARY, 3));
    }

    @Test
    public void magePrice_globalCounter_inGame() {
        GameModel m = newModel();
        m.startLevel(1);
        int[] run = m.exportRun();
        run[5] = 500;
        m.importRun(run);
        m.resume();
        assertEquals(25, m.getMageCost(Mage.LIGHT));
        assertEquals(14, m.getMageCost(Mage.WATER));
        m.recruitMage(Mage.FIRE);                    // 10 PO
        assertEquals(490, m.getGold());
        assertEquals(31, m.getMageCost(Mage.LIGHT));  // 25 + 6
        assertEquals(17, m.getMageCost(Mage.WATER));  // 14 + 3
        assertEquals(22, m.getMageCost(Mage.LIGHTNING)); // 18 + 4
        m.recruitMage(Mage.LIGHT);                   // 31 PO
        assertEquals(459, m.getGold());
        assertEquals(14, m.getMageCost(Mage.FIRE));   // 10 + 2 × 2
        int gold = m.getGold();
        assertTrue(m.recruitRandom());               // le dé : toujours 7 PO, non compté
        assertEquals(gold - GameModel.RANDOM_MAGE_COST, m.getGold());
        assertEquals(14, m.getMageCost(Mage.FIRE));
        // La partie reprise garde le compteur d'achats
        GameModel other = newModel();
        assertTrue(other.importRun(m.exportRun()));
        assertEquals(m.getMageCost(Mage.LIGHT), other.getMageCost(Mage.LIGHT));
    }

    // ---------- Titres des biomes ----------
    @Test
    public void biomeTitles_countTheCycles() {
        assertEquals("Prairie I", LevelSpec.biomeTitleForLevel(1));
        assertEquals("Volcan I", LevelSpec.biomeTitleForLevel(25));
        assertEquals("Crépuscule I", LevelSpec.biomeTitleForLevel(36));
        assertEquals("Prairie II", LevelSpec.biomeTitleForLevel(37));
        assertEquals("Volcan II", LevelSpec.biomeTitleForLevel(61));
        assertEquals("Volcan III", LevelSpec.biomeTitleForLevel(97));
        assertEquals("Prairie I", LevelSpec.biomeTitleForEndlessWave(1));
        assertEquals("Crépuscule I", LevelSpec.biomeTitleForEndlessWave(60));
        assertEquals("Prairie II", LevelSpec.biomeTitleForEndlessWave(61));
        assertEquals("Désert II", LevelSpec.biomeTitleForEndlessWave(71));
    }
}
