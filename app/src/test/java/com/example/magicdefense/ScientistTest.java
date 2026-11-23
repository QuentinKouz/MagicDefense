package com.example.magicdefense;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.example.magicdefense.model.Artifact;
import com.example.magicdefense.model.Board;
import com.example.magicdefense.model.Chest;
import com.example.magicdefense.model.Enemy;
import com.example.magicdefense.model.GameModel;
import com.example.magicdefense.model.Mage;
import com.example.magicdefense.model.Nuke;
import com.example.magicdefense.model.Progression;
import com.example.magicdefense.model.Rarity;
import com.example.magicdefense.model.Reaction;

import org.junit.Test;

import java.util.Random;

// Mage légendaire « Scientifique » : fioles, bombe nucléaire automatique, Réaction en chaîne
public class ScientistTest {

    private static GameModel newModel() {
        Board board = new Board(1f);
        board.resize(400, 800);
        GameModel m = new GameModel(board);
        boolean[] owned = new boolean[Mage.TYPE_COUNT];
        java.util.Arrays.fill(owned, true);
        m.getProgression().load(0, 0, new int[Mage.TYPE_COUNT], new int[Mage.TYPE_COUNT], owned,
                new int[]{Mage.SCIENTIST, Mage.FIRE, Mage.WATER, Mage.EARTH}, 0);
        return m;
    }

    // Partie en cours, vague lancée, avec beaucoup de PO
    private static GameModel playing() {
        GameModel m = newModel();
        m.startLevel(1);
        int[] run = m.exportRun();
        run[5] = 1000;
        m.importRun(run);
        m.resume();
        m.launchWave();
        return m;
    }

    private static int[] findMage(GameModel m, int type) {
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                Mage mage = m.getMage(row, col);
                if (mage != null && mage.type == type) {
                    return new int[]{row, col};
                }
            }
        }
        return null;
    }

    // Gobelin immobile et très résistant, visible à l'écran
    private static Enemy dummy(float x, float y, int kind) {
        Enemy e = new Enemy(x, 20, 0f, 1_000_000f, kind);
        e.y = y;
        return e;
    }

    @Test
    public void scientist_isLegendaryAndInChests() {
        assertEquals(10, Mage.SCIENTIST);
        assertEquals(11, Mage.TYPE_COUNT);
        assertEquals(Rarity.LEGENDARY, Rarity.of(Mage.SCIENTIST));
        assertTrue(Mage.isExclusive(Mage.SCIENTIST));
        GameModel m = newModel();
        assertEquals(Rarity.magePrice(Rarity.LEGENDARY, 0), m.getMageCost(Mage.SCIENTIST));
        assertEquals(25, m.getMageCost(Mage.SCIENTIST));
        // Débloqué comme la Lumière et les Ténèbres : il sort des coffres (rareté légendaire)
        Random random = new Random(3);
        boolean seen = false;
        for (int i = 0; i < 3000 && !seen; i++) {
            for (int t : Chest.roll(Chest.MYSTERY, random).types) {
                seen |= t == Mage.SCIENTIST;
            }
        }
        assertTrue("le Scientifique peut sortir d'un coffre", seen);
        assertEquals(Mage.TYPE_COUNT, Mage.SKILL_NAMES.length);
        assertEquals(Mage.TYPE_COUNT, Mage.SKILL_DESCRIPTIONS.length);
        assertEquals("Réaction en chaîne", Mage.SKILL_NAMES[Mage.SCIENTIST]);
    }

    @Test
    public void nuke_hasLongCooldown_andScalesWithLevelAndRank() {
        Mage s = new Mage(Mage.SCIENTIST);
        assertEquals(Nuke.COOLDOWN, s.getNukeCooldown());
        assertTrue("≈ 25 s : bien plus long qu'une compétence", s.getNukeCooldown() > Mage.SKILL_COOLDOWN);
        assertEquals("la première bombe attend une recharge complète", s.getNukeCooldown(), s.nukeTimer);
        assertEquals(0f, s.getNukeProgress(), 1e-6);
        assertEquals(0, new Mage(Mage.FIRE).getNukeCooldown());
        Mage high = new Mage(Mage.SCIENTIST);
        high.level = 5;
        assertTrue(high.getNukeDamage() > 5 * s.getNukeDamage());
        assertTrue(new Mage(Mage.SCIENTIST, 5).getNukeDamage() > s.getNukeDamage());
        assertTrue("bombe bien plus forte qu'une fiole", s.getNukeDamage() >= 8 * s.getDamage());
        high.level = 7;
        assertTrue("sage : recharge plus courte", high.getNukeCooldown() < Nuke.COOLDOWN);
    }

    @Test
    public void nuke_hitsAllVisibleEnemies_bossReduced() {
        GameModel m = playing();
        m.recruitMage(Mage.SCIENTIST);
        int[] pos = findMage(m, Mage.SCIENTIST);
        assertNotNull(pos);
        Mage s = m.getMage(pos[0], pos[1]);
        s.cooldown = 1_000_000; // pas de fiole : seule la bombe blesse
        Board b = new Board(1f);
        b.resize(400, 800);
        Enemy left = dummy(30, 120, Enemy.NORMAL);
        Enemy right = dummy(370, b.gridTop - 60, Enemy.NORMAL);
        Enemy middle = dummy(200, 300, Enemy.TANK);
        Enemy boss = dummy(200, 200, Enemy.BOSS);
        Enemy hidden = dummy(200, -100, Enemy.NORMAL); // pas encore sorti de la forêt
        m.getEnemies().clear();
        m.getEnemies().add(left);
        m.getEnemies().add(right);
        m.getEnemies().add(middle);
        m.getEnemies().add(boss);
        m.getEnemies().add(hidden);
        float expected = s.getNukeDamage();
        s.nukeTimer = 1;
        m.update();
        assertEquals("la bombe est larguée", 1, m.getNukes().size());
        assertEquals(s.getNukeCooldown(), s.nukeTimer);
        for (int i = 0; i < Nuke.FALL_FRAMES + 2; i++) {
            m.update();
        }
        for (Enemy e : new Enemy[]{left, right, middle}) {
            assertEquals(expected, e.maxHp - e.hp, expected * 0.01f);
        }
        assertEquals(expected * Nuke.BOSS_DAMAGE, boss.maxHp - boss.hp, expected * 0.01f);
        assertEquals("hors de l'écran : pas touché", hidden.maxHp, hidden.hp, 1e-3);
        assertTrue(m.getShake() > 0);
        assertEquals("une seule bombe par recharge", 1, m.getNukes().size());
    }

    @Test
    public void chainReaction_halvesNukeCooldownDuringItsDuration() {
        GameModel m = playing();
        m.recruitMage(Mage.SCIENTIST);
        int[] pos = findMage(m, Mage.SCIENTIST);
        Mage s = m.getMage(pos[0], pos[1]);
        s.level = 5;
        s.skillTimer = 0;
        assertTrue(m.castSkill(pos[0], pos[1]));
        assertEquals(Nuke.CHAIN_DURATION, s.chainTimer);
        assertEquals(2, s.getNukeSpeed());
        s.nukeTimer = 1000;
        for (int i = 0; i < 100; i++) {
            m.update();
        }
        assertEquals("la jauge se remplit 2× plus vite", 800, s.nukeTimer);
        for (int i = 0; i < Nuke.CHAIN_DURATION; i++) {
            m.update();
        }
        assertEquals(0, s.chainTimer);
        assertEquals("retour à la normale", 1, s.getNukeSpeed());
        s.nukeTimer = 1000;
        m.update();
        assertEquals(999, s.nukeTimer);
        // Sage : version BOOST, 3× plus vite
        Mage sage = new Mage(Mage.SCIENTIST);
        sage.level = 7;
        sage.chainTimer = 1;
        assertEquals(3, sage.getNukeSpeed());
        assertEquals("Apocalypse nucléaire", sage.getSkillName());
    }

    @Test
    public void reactions_scientistHasTenNewOnes() {
        assertEquals(54, Reaction.COUNT);
        assertEquals(Reaction.COUNT, Reaction.NAMES.length);
        boolean[] used = new boolean[Reaction.COUNT];
        int count = 0;
        for (int t = 0; t < Mage.TYPE_COUNT; t++) {
            if (t == Mage.SCIENTIST) {
                continue;
            }
            int r = Reaction.between(Mage.SCIENTIST, t);
            assertNotEquals(Reaction.NONE, r);
            assertEquals(r, Reaction.between(t, Mage.SCIENTIST));
            assertTrue(r >= Reaction.SALINE && r <= Reaction.ANTIMATTER);
            assertFalse(used[r]);
            used[r] = true;
            count++;
        }
        assertEquals(10, count);
        assertEquals(Reaction.SALINE, Reaction.between(Mage.WATER, Mage.SCIENTIST));
        assertEquals(Reaction.COMBUSTION, Reaction.between(Mage.FIRE, Mage.SCIENTIST));
        assertEquals(Reaction.ELECTROLYSIS, Reaction.between(Mage.LIGHTNING, Mage.SCIENTIST));
        assertEquals(Reaction.MUTANT_TOXIN, Reaction.between(Mage.POISON, Mage.SCIENTIST));
        assertEquals(Reaction.FISSION, Reaction.between(Mage.LIGHT, Mage.SCIENTIST));
        assertEquals(Reaction.ANTIMATTER, Reaction.between(Mage.DARK, Mage.SCIENTIST));
        assertEquals("SOLUTION SALINE", Reaction.NAMES[Reaction.SALINE]);
        assertTrue(Reaction.leavesMark(Mage.SCIENTIST));
    }

    @Test
    public void vial_onWetGoblin_triggersSaline() {
        GameModel m = playing();
        m.recruitMage(Mage.SCIENTIST);
        int[] pos = findMage(m, Mage.SCIENTIST);
        m.getMage(pos[0], pos[1]).cooldown = 0;
        Enemy wet = dummy(200, 250, Enemy.NORMAL);
        wet.mark = Mage.WATER;
        wet.markTimer = 10_000;
        m.getEnemies().clear();
        m.getEnemies().add(wet);
        for (int i = 0; i < 300 && !wet.isVulnerable(); i++) {
            m.update();
        }
        assertTrue("solution saline : armure rongée", wet.isVulnerable());
        assertTrue("et ralenti", wet.slowTimer > 0);
        assertTrue(wet.hp < wet.maxHp);
    }

    @Test
    public void artifacts_threeForTheScientist() {
        assertEquals(33, Artifact.COUNT);
        assertEquals(Artifact.COUNT, Artifact.NAMES.length);
        assertEquals(Artifact.COUNT, Artifact.DESCRIPTIONS.length);
        for (int i = 0; i < Artifact.PER_TYPE; i++) {
            assertEquals(Mage.SCIENTIST, Artifact.typeOf(30 + i));
        }
        Mage base = new Mage(Mage.SCIENTIST);
        Mage uranium = new Mage(Mage.SCIENTIST, 1, Artifact.id(Mage.SCIENTIST, 0));
        assertTrue("bombe plus fréquente", uranium.getNukeCooldown() < base.getNukeCooldown());
        assertTrue(uranium.getDamage() < base.getDamage());
        Mage vial = new Mage(Mage.SCIENTIST, 1, Artifact.id(Mage.SCIENTIST, 1));
        assertTrue(vial.getAoeRadius() > base.getAoeRadius());
        assertTrue(vial.getFireDelay() < base.getFireDelay());
        assertTrue(vial.getNukeDamage() < base.getNukeDamage());
        Mage warhead = new Mage(Mage.SCIENTIST, 1, Artifact.id(Mage.SCIENTIST, 2));
        assertEquals(base.getNukeDamage() * 1.6f, warhead.getNukeDamage(), 1e-3);
        assertTrue(warhead.getNukeCooldown() > base.getNukeCooldown());
        // Les autres artefacts ne touchent pas à la bombe
        assertEquals(1f, Artifact.nukeCooldown(Artifact.id(Mage.FIRE, 0)), 1e-6);
    }

    @Test
    public void oldSaveWithTenTypes_loadsWithoutTheScientist() {
        Progression p = new Progression();
        int[] fragments = new int[10];
        int[] ranks = new int[10];
        boolean[] owned = new boolean[10];
        java.util.Arrays.fill(ranks, 3);
        java.util.Arrays.fill(owned, true);
        p.load(0, 50, fragments, ranks, owned, new int[]{Mage.DARK, Mage.LIGHT, 10, Mage.FIRE}, 0);
        assertFalse("le Scientifique n'est pas possédé", p.isOwned(Mage.SCIENTIST));
        assertEquals(1, p.getRank(Mage.SCIENTIST));
        assertEquals(3, p.getRank(Mage.DARK));
        assertFalse(p.isInTeam(Mage.SCIENTIST));
        boolean[] arts = new boolean[30];
        arts[29] = true;
        int[] equipped = new int[10];
        java.util.Arrays.fill(equipped, Artifact.NONE);
        equipped[Mage.DARK] = 29;
        p.loadArtifacts(arts, equipped);
        assertTrue(p.ownsArtifact(29));
        assertFalse(p.ownsArtifact(30));
        assertEquals(29, p.getEquippedArtifact(Mage.DARK));
        assertEquals(Artifact.NONE, p.getEquippedArtifact(Mage.SCIENTIST));
        assertEquals(0, p.artifactCount(Mage.SCIENTIST));
    }
}
