package com.example.magicdefense;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.example.magicdefense.model.Artifact;
import com.example.magicdefense.model.Board;
import com.example.magicdefense.model.Enemy;
import com.example.magicdefense.model.GameModel;
import com.example.magicdefense.model.Mage;
import com.example.magicdefense.model.Reaction;
import com.example.magicdefense.model.Wall;

import org.junit.Test;

// Les 3 nouveaux mages : Ouvrier (mur), Lumière (aura), Ténèbres (exécution) et leurs réactions
public class NewMagesTest {

    private static GameModel newModel() {
        Board board = new Board(1f);
        board.resize(400, 800);
        GameModel m = new GameModel(board);
        boolean[] owned = new boolean[Mage.TYPE_COUNT];
        java.util.Arrays.fill(owned, true);
        m.getProgression().load(0, 0, new int[Mage.TYPE_COUNT], new int[Mage.TYPE_COUNT], owned,
                new int[]{Mage.WORKER, Mage.LIGHT, Mage.DARK, Mage.FIRE}, 0);
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

    @Test
    public void reactions_lightAndDarkCancel_otherPairsAreDistinct() {
        assertEquals(10, Mage.TYPE_COUNT);
        assertEquals(44, Reaction.COUNT);
        assertEquals(Reaction.COUNT, Reaction.NAMES.length);
        assertEquals(Reaction.NONE, Reaction.between(Mage.LIGHT, Mage.DARK));
        assertEquals(Reaction.NONE, Reaction.between(Mage.DARK, Mage.LIGHT));
        boolean[] used = new boolean[Reaction.COUNT];
        int pairs = 0;
        for (int a = 0; a < Mage.TYPE_COUNT; a++) {
            for (int b = a + 1; b < Mage.TYPE_COUNT; b++) {
                int r = Reaction.between(a, b);
                if (a == Mage.LIGHT && b == Mage.DARK) {
                    continue;
                }
                assertNotEquals(Reaction.NONE, r);
                assertFalse("réaction en double : " + Reaction.NAMES[r], used[r]);
                used[r] = true;
                pairs++;
            }
        }
        assertEquals(44, pairs);
        assertEquals(Reaction.CONCRETE, Reaction.between(Mage.WATER, Mage.WORKER));
        assertEquals(Reaction.SANCTUARY, Reaction.between(Mage.LIGHT, Mage.WORKER));
        assertEquals(Reaction.REAPING, Reaction.between(Mage.SUMMONER, Mage.DARK));
        assertTrue(Reaction.leavesMark(Mage.WORKER));
    }

    @Test
    public void artifacts_newMagesHaveThreeEach() {
        assertEquals(30, Artifact.COUNT);
        assertEquals(Artifact.COUNT, Artifact.NAMES.length);
        assertEquals(Artifact.COUNT, Artifact.DESCRIPTIONS.length);
        assertEquals(Mage.WORKER, Artifact.typeOf(21));
        assertEquals(Mage.DARK, Artifact.typeOf(29));
        // Faux du néant : exécute sous 15 %
        Mage scythe = new Mage(Mage.DARK, 1, Artifact.id(Mage.DARK, 0));
        assertEquals(0.15f, scythe.getExecuteRatio(), 1e-6);
        // Truelle bénie : mur plus solide
        Mage trowel = new Mage(Mage.WORKER, 1, Artifact.id(Mage.WORKER, 1));
        assertTrue(trowel.getWallHp() > new Mage(Mage.WORKER, 1).getWallHp());
        // Ténèbres : zone plus grande que l'Eau
        for (int lv = 1; lv <= 5; lv++) {
            Mage water = new Mage(Mage.WATER);
            Mage dark = new Mage(Mage.DARK);
            water.level = lv;
            dark.level = lv;
            assertTrue(dark.getAoeRadius() > water.getAoeRadius());
        }
    }

    @Test
    public void dark_executesWoundedGoblins_butNotTheBoss() {
        GameModel m = newModel();
        m.startLevel(1);
        m.recruitMage(Mage.DARK);
        Board b = new Board(1f);
        b.resize(400, 800);
        Enemy wounded = new Enemy(200, 20, 0, 1_000_000f);
        wounded.y = b.gridTop - 150;
        wounded.hp = wounded.maxHp * 0.05f;   // 5 % : sous le seuil de 10 %
        Enemy boss = new Enemy(200, 20, 0, 1_000_000f, Enemy.BOSS);
        boss.y = b.gridTop - 160;
        boss.hp = boss.maxHp * 0.05f;
        m.getEnemies().add(wounded);
        m.getEnemies().add(boss);
        for (int i = 0; i < 400 && m.getEnemies().contains(wounded); i++) {
            m.update();
        }
        assertFalse("le gobelin blessé est exécuté", m.getEnemies().contains(wounded));
        assertTrue("le boss n'est jamais exécuté", m.getEnemies().contains(boss));
        assertTrue(boss.hp > 0);
        assertTrue("la zone des Ténèbres ralentit", boss.slowTimer > 0);
    }

    @Test
    public void light_auraBoostsAllOtherMages() {
        GameModel m = newModel();
        m.startLevel(1);
        m.recruitMage(Mage.FIRE);
        int[] fire = findMage(m, Mage.FIRE);
        assertEquals(0f, m.getAuraBonus(fire[0], fire[1]), 1e-6);
        m.recruitMage(Mage.LIGHT); // 20 PO au départ : deux mages à 10 PO
        int[] light = findMage(m, Mage.LIGHT);
        assertTrue(light != null);
        assertEquals(0.10f, m.getAuraBonus(fire[0], fire[1]), 1e-6);   // +10 % au niveau 1
        assertEquals(0f, m.getAuraBonus(light[0], light[1]), 1e-6);    // sa propre aura ne compte pas
        assertTrue(new Mage(Mage.LIGHT).getAuraBonus() > 0);
        assertEquals(0f, new Mage(Mage.FIRE).getAuraBonus(), 1e-6);
    }

    @Test
    public void worker_buildsWallThatBlocksGoblins() {
        GameModel m = newModel();
        m.startLevel(1);
        m.recruitMage(Mage.WORKER);
        m.update();
        assertEquals(1, m.getWalls().size());
        Wall w = m.getWalls().get(0);
        float hpBefore = w.hp;
        Enemy goblin = new Enemy(w.x, 20, 3f, 20f); // le mur perd 15 % des PV max du gobelin par seconde
        goblin.y = w.y - w.halfHeight - 30;
        m.getEnemies().add(goblin);
        for (int i = 0; i < 120; i++) {
            m.update();
        }
        assertTrue("le gobelin est arrêté par le mur", goblin.y - goblin.radius < w.y + w.halfHeight);
        assertTrue(m.getEnemies().contains(goblin));
        assertTrue("le mur encaisse", m.getWalls().isEmpty() || m.getWalls().get(0).hp < hpBefore);
        assertEquals(1, m.getWalls().size()); // un seul mur par ouvrier
    }
}
