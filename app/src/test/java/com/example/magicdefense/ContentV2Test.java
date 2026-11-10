package com.example.magicdefense;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.magicdefense.model.Enemy;
import com.example.magicdefense.model.LevelSpec;
import com.example.magicdefense.model.Mage;
import com.example.magicdefense.model.Progression;
import com.example.magicdefense.model.Reaction;

import org.junit.Test;

import java.util.Random;

// V2 (suite) : gemmes, Marché, équipe, réactions élémentaires, boss, biomes, états des ennemis
public class ContentV2Test {

    // ---------- Coffres et gemmes ----------

    @Test
    public void chest_givesFragmentsOnlyForOwnedUnits_andGems() {
        Progression p = new Progression();
        p.addChests(200);
        Random r = new Random(7);
        int gems = 0;
        for (int i = 0; i < 200; i++) {
            assertTrue(p.openChest(r));
            assertTrue(p.getLastChestGems() >= Progression.GEMS_MIN && p.getLastChestGems() <= Progression.GEMS_MAX);
            gems += p.getLastChestGems();
        }
        assertEquals(gems, p.getGems());
        for (int t = Mage.BASE_TYPES; t < Mage.TYPE_COUNT; t++) {
            assertEquals("pas de fragments pour une unité non possédée", 0, p.getFragments(t));
        }
    }

    @Test
    public void chestOpening_oneCardPerTap_thenSummary() {
        com.example.magicdefense.model.Board board = new com.example.magicdefense.model.Board(1f);
        board.resize(400, 800);
        com.example.magicdefense.model.GameModel model = new com.example.magicdefense.model.GameModel(board);
        model.getProgression().addChests(1);
        model.openUpgrades();
        assertTrue(model.openChest());
        assertEquals(com.example.magicdefense.model.GameState.CHEST_OPENING, model.getState());

        model.tapChestOpening();                 // le coffre se pose (arrivée passée)
        assertTrue(model.isChestIntroDone());
        int n = model.getChestCardCount();
        assertTrue(n >= 2);                      // au moins un élément + les gemmes
        assertEquals(com.example.magicdefense.model.GameModel.CARD_GEMS, model.getChestCardType(n - 1));
        for (int i = 1; i < n - 1; i++) {        // fragments par quantité croissante
            assertTrue(model.getChestCardAmount(i - 1) <= model.getChestCardAmount(i));
        }
        for (int i = 1; i <= n; i++) {
            model.tapChestOpening();             // une carte par toucher
            assertEquals(i, model.getChestRevealed());
            assertFalse(model.isChestRevealComplete());
        }
        model.tapChestOpening();                 // récapitulatif
        assertTrue(model.isChestRevealComplete());
        model.tapChestOpening();                 // fermer
        assertEquals(com.example.magicdefense.model.GameState.UPGRADES, model.getState());
    }

    // ---------- Marché ----------

    @Test
    public void buy_needsEnoughGems_andJoinsTeamIfRoom() {
        Progression p = new Progression();
        assertFalse(p.isOwned(Mage.POISON));
        assertFalse(p.buy(Mage.POISON)); // 0 gemme
        p.load(0, 100, new int[Mage.TYPE_COUNT], new int[]{1, 1, 1, 1, 1, 1, 1}, null,
                new int[]{Mage.WATER, Mage.FIRE, -1, -1}, 0);
        assertTrue(p.buy(Mage.POISON));
        assertTrue(p.isOwned(Mage.POISON));
        assertEquals(100 - Progression.price(Mage.POISON), p.getGems());
        assertTrue(p.isInTeam(Mage.POISON)); // une place était libre
        assertFalse(p.buy(Mage.POISON));    // déjà possédée
        assertFalse(p.buy(Mage.WATER));     // unité de base : rien à acheter
    }

    @Test
    public void buy_doesNotJoinFullTeam() {
        Progression p = new Progression();
        p.load(0, 100, new int[Mage.TYPE_COUNT], new int[]{1, 1, 1, 1, 1, 1, 1}, null, null, 0);
        assertTrue(p.isTeamFull());
        assertTrue(p.buy(Mage.AIR));
        assertFalse(p.isInTeam(Mage.AIR));
    }

    // ---------- Équipe ----------

    @Test
    public void team_toggle_limits() {
        Progression p = new Progression();
        assertEquals(4, p.teamCount());
        assertFalse(p.toggleTeam(Mage.POISON));       // pas possédé
        assertTrue(p.toggleTeam(Mage.FIRE));           // retire le Feu
        assertEquals(3, p.teamCount());
        assertEquals(Mage.WATER, p.getTeamSlot(0));    // l'équipe se resserre
        assertEquals(Mage.EARTH, p.getTeamSlot(1));
        assertEquals(Progression.NO_UNIT, p.getTeamSlot(3));
        assertTrue(p.toggleTeam(Mage.FIRE));           // le remet (en dernière place)
        assertEquals(Mage.FIRE, p.getTeamSlot(3));
        assertTrue(p.isTeamFull());
        // Jamais d'équipe vide
        Progression q = new Progression();
        q.toggleTeam(Mage.FIRE);
        q.toggleTeam(Mage.EARTH);
        q.toggleTeam(Mage.LIGHTNING);
        assertEquals(1, q.teamCount());
        assertFalse(q.toggleTeam(Mage.WATER));
        assertEquals(1, q.teamCount());
    }

    @Test
    public void team_load_isSanitized() {
        Progression p = new Progression();
        // Doublon, unité non possédée, valeur invalide : tout est nettoyé
        p.load(0, 0, new int[Mage.TYPE_COUNT], new int[]{1, 1, 1, 1, 1, 1, 1}, null,
                new int[]{Mage.FIRE, Mage.FIRE, Mage.AIR, 99}, 0);
        assertEquals(1, p.teamCount());
        assertEquals(Mage.FIRE, p.getTeamSlot(0));
        // Équipe entièrement invalide : on garde au moins l'Eau
        p.load(0, 0, new int[Mage.TYPE_COUNT], new int[]{1, 1, 1, 1, 1, 1, 1}, null,
                new int[]{-1, -1, -1, -1}, 0);
        assertEquals(Mage.WATER, p.getTeamSlot(0));
    }

    // ---------- Réactions élémentaires ----------

    @Test
    public void reactions_pairsInBothOrders() {
        assertEquals(Reaction.ELECTROCUTION, Reaction.between(Mage.WATER, Mage.LIGHTNING));
        assertEquals(Reaction.ELECTROCUTION, Reaction.between(Mage.LIGHTNING, Mage.WATER));
        assertEquals(Reaction.LAVA, Reaction.between(Mage.EARTH, Mage.FIRE));
        assertEquals(Reaction.LAVA, Reaction.between(Mage.FIRE, Mage.EARTH));
        assertEquals(Reaction.STEAM, Reaction.between(Mage.FIRE, Mage.WATER));
        assertEquals(Reaction.TOXIC_BLAST, Reaction.between(Mage.POISON, Mage.FIRE));
        assertEquals(Reaction.TOXIC_CLOUD, Reaction.between(Mage.AIR, Mage.POISON));
        assertEquals(Reaction.SANDSTORM, Reaction.between(Mage.AIR, Mage.EARTH));
    }

    @Test
    public void reactions_none() {
        assertEquals(Reaction.NONE, Reaction.between(Mage.FIRE, Mage.FIRE));
        assertEquals(Reaction.NONE, Reaction.between(Enemy.NO_MARK, Mage.FIRE));
        assertEquals(Reaction.NONE, Reaction.between(Mage.LIGHTNING, Mage.EARTH));
        assertEquals(Reaction.NONE, Reaction.between(Mage.WATER, Mage.SUMMONER));
        assertFalse(Reaction.leavesMark(Mage.SUMMONER));
        assertTrue(Reaction.leavesMark(Mage.POISON));
        assertEquals(Reaction.COUNT, Reaction.NAMES.length);
    }

    // ---------- Boss et biomes ----------

    @Test
    public void boss_everyTenLevels_lastWaveOnly() {
        for (int lv = 1; lv <= LevelSpec.HANDMADE_LEVELS; lv++) {
            LevelSpec s = LevelSpec.forLevel(lv);
            for (int w = 1; w <= s.waves; w++) {
                assertFalse(s.hasBoss(w));
            }
        }
        LevelSpec ten = LevelSpec.forLevel(10);
        assertTrue(ten.hasBossLevel());
        assertTrue(ten.hasBoss(ten.waves));
        assertFalse(ten.hasBoss(ten.waves - 1));
        assertTrue(LevelSpec.forLevel(20).hasBossLevel());
        assertFalse(LevelSpec.forLevel(11).hasBossLevel());
    }

    @Test
    public void boss_everyTenWavesInEndless() {
        LevelSpec e = LevelSpec.endless();
        assertFalse(e.hasBoss(9));
        assertTrue(e.hasBoss(10));
        assertFalse(e.hasBoss(11));
        assertTrue(e.hasBoss(20));
    }

    @Test
    public void biomes() {
        assertEquals(0, LevelSpec.biomeForLevel(1));
        assertEquals(0, LevelSpec.biomeForLevel(6));
        assertEquals(1, LevelSpec.biomeForLevel(7));
        assertEquals(0, LevelSpec.biomeForLevel(6 * LevelSpec.BIOME_COUNT + 1)); // on recommence le cycle
        assertEquals(0, LevelSpec.biomeForEndlessWave(10));
        assertEquals(1, LevelSpec.biomeForEndlessWave(11));
    }

    @Test
    public void shamans_onlyInGeneratedLevels() {
        Random r = new Random(3);
        LevelSpec handmade = LevelSpec.forLevel(5);
        for (int i = 0; i < 500; i++) {
            assertTrue(handmade.rollKind(7, r) != Enemy.SHAMAN);
        }
        assertTrue(LevelSpec.forLevel(30).shamanChance(5) > 0);
        assertEquals(0f, LevelSpec.endless().shamanChance(3), 1e-6);
        assertTrue(LevelSpec.endless().shamanChance(20) > 0);
    }

    // ---------- États des ennemis ----------

    @Test
    public void enemy_poison_stun_knockback() {
        Enemy e = new Enemy(0, 10, 2, 100);
        e.y = 200;
        e.poison(60, 60); // 60 dégâts/s pendant 1 s
        for (int i = 0; i < 60; i++) {
            e.tickStatus();
        }
        assertEquals(40, e.hp, 0.01);
        assertFalse(e.isPoisoned());

        e.stun(10);
        e.update(1f);
        assertEquals(200, e.y, 1e-6);  // étourdi : ne bouge pas
        e.knockBack(50);
        assertEquals(150, e.y, 1e-6);
    }

    @Test
    public void boss_resistsControl() {
        Enemy boss = new Enemy(0, 10, 2, 100, Enemy.BOSS);
        boss.y = 200;
        boss.knockBack(100);
        assertEquals(170, boss.y, 1e-4); // seulement 30 % du recul
        boss.stun(40);
        assertEquals(20, boss.stunTimer); // moitié de l'étourdissement
    }

    @Test
    public void exclusiveMages_stats() {
        assertTrue(new Mage(Mage.POISON).getPoisonDps() > 0);
        assertEquals(0f, new Mage(Mage.FIRE).getPoisonDps(), 0f);
        assertTrue(new Mage(Mage.AIR).getKnockback() > 0);
        assertTrue(new Mage(Mage.SUMMONER).getStunFrames() > 0);
        assertTrue(Mage.isExclusive(Mage.SUMMONER));
        assertFalse(Mage.isExclusive(Mage.LIGHTNING));
    }
}