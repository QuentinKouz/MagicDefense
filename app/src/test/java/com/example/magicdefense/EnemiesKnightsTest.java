package com.example.magicdefense;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.example.magicdefense.model.Board;
import com.example.magicdefense.model.Enemy;
import com.example.magicdefense.model.GameModel;
import com.example.magicdefense.model.Knight;
import com.example.magicdefense.model.LevelSpec;
import com.example.magicdefense.model.Mage;

import org.junit.Test;

import java.util.Random;

// Chevaliers de l'Invocateur (ils foncent sur les gobelins et les arrêtent) et gobelin mage
// (il s'arrête à l'orée de la forêt et tire sur le rempart)
public class EnemiesKnightsTest {

    private static Board newBoard() {
        Board board = new Board(1f);
        board.resize(400, 800);
        return board;
    }

    // Partie lancée sans mage : la 1re vague attend le joueur, aucun gobelin n'apparaît tout seul
    private static GameModel newGame(Board board) {
        GameModel m = new GameModel(board);
        m.startLevel(1);
        return m;
    }

    // Chevalier très résistant et inoffensif, en garde devant la colonne 1
    private static Knight addKnight(GameModel m, Board b) {
        float radius = b.cellWidth * Knight.RADIUS_PER_CELL;
        Knight k = new Knight(b.cellCenterX(1), b.gridTop - radius - 4, radius, 1_000_000f, 0f, null);
        m.getKnights().add(k);
        return k;
    }

    @Test
    public void knight_isAboutOneCellWide() {
        Board b = newBoard();
        float radius = b.cellWidth * Knight.RADIUS_PER_CELL;
        assertTrue(radius * 2 > b.cellWidth * 0.7f);
        assertTrue(radius * 2 <= b.cellWidth);
    }

    @Test
    public void knight_blocksNormalGoblinOfItsColumn_butNotFastOne() {
        Board b = newBoard();
        GameModel m = newGame(b);
        Knight k = addKnight(m, b);
        Enemy goblin = new Enemy(k.x, 20, 3f, 20f);
        goblin.y = k.y - k.radius - 60;
        m.getEnemies().add(goblin);
        for (int i = 0; i < 180; i++) {
            m.update();
        }
        assertTrue("le gobelin normal est arrêté", m.getEnemies().contains(goblin));
        assertTrue("il se bat au contact", k.touches(goblin));
        assertTrue("il ne passe pas", goblin.y < k.y);
        assertEquals(m.getCastleMaxHp(), m.getCastleHp());

        // Un gobelin rapide de la même colonne se faufile et atteint le château
        GameModel m2 = newGame(b);
        Knight k2 = addKnight(m2, b);
        Enemy fast = new Enemy(k2.x, 15, 3f, 20f, Enemy.FAST);
        fast.y = k2.y - k2.radius - 60;
        m2.getEnemies().add(fast);
        boolean passed = false;
        for (int i = 0; i < 300 && !passed; i++) {
            m2.update();
            passed = !m2.getEnemies().contains(fast) || fast.y > k2.y + k2.radius;
        }
        assertTrue("le rapide passe", passed);
        assertFalse(k2.blocks(new Enemy(k2.x, 10, 0, 1, Enemy.IMP)));
    }

    @Test
    public void knight_marchesTowardGoblin_thenReturnsToGuard() {
        Board b = newBoard();
        GameModel m = newGame(b);
        Knight k = addKnight(m, b);
        float guardY = k.y;
        Enemy goblin = new Enemy(b.cellCenterX(2), 20, 0f, 1_000_000f); // immobile, plus haut, colonne voisine
        goblin.y = b.forestBottom() + 80;
        m.getEnemies().add(goblin);
        float startDist = Math.abs(goblin.y - k.y) + Math.abs(goblin.x - k.x);
        for (int i = 0; i < 30; i++) {
            m.update();
        }
        float dist = Math.abs(goblin.y - k.y) + Math.abs(goblin.x - k.x);
        assertTrue("il avance aussitôt vers le gobelin", dist < startDist - 30);
        for (int i = 0; i < 400 && !k.touches(goblin); i++) {
            m.update();
        }
        assertTrue("il finit par l'atteindre", k.touches(goblin));
        m.update();
        assertTrue("et le combat", k.fighting);

        // Plus de cible : retour en garde devant le château, dans sa colonne
        m.getEnemies().clear();
        for (int i = 0; i < 400; i++) {
            m.update();
        }
        assertEquals(guardY, k.y, 0.01f);
        assertEquals(b.cellCenterX(1), k.x, 0.01f);
    }

    @Test
    public void goblinMage_stopsAtForestEdge_andHurtsCastleFromAfar() {
        Board b = newBoard();
        GameModel m = newGame(b);
        float stopY = m.mageGoblinStopY();
        assertTrue("il s'arrête juste sous la forêt", stopY > b.forestBottom() && stopY < b.gridTop / 2);
        Enemy mage = new Enemy(b.centerX(), 20, 3f, 1_000_000f, Enemy.MAGE);
        m.getEnemies().add(mage);
        boolean sawBolt = false;
        for (int i = 0; i < 300; i++) {
            m.update();
            sawBolt |= !m.getEnemyBolts().isEmpty();
        }
        assertEquals("il reste à sa position de tir", stopY, mage.y, 0.01f);
        assertTrue(mage.inPosition);
        assertTrue("son projectile est visible", sawBolt);
        assertTrue("le château est touché à distance", m.getCastleHp() < m.getCastleMaxHp());
        assertTrue(m.getEnemies().contains(mage));

        // Repoussé, il revient à sa position de tir
        mage.knockBack(80);
        assertTrue(mage.y < stopY);
        for (int i = 0; i < 120; i++) {
            m.update();
        }
        assertEquals(stopY, mage.y, 0.01f);

        // Étourdi : il ne tire plus
        mage.stun(600);
        m.getEnemyBolts().clear();
        int hp = m.getCastleHp();
        for (int i = 0; i < 300; i++) {
            m.update();
        }
        assertEquals(hp, m.getCastleHp());
    }

    @Test
    public void goblinMage_targetsWorkerWallOfItsColumn() {
        Board b = newBoard();
        GameModel m = new GameModel(b);
        boolean[] owned = new boolean[Mage.TYPE_COUNT];
        java.util.Arrays.fill(owned, true);
        m.getProgression().load(0, 0, new int[Mage.TYPE_COUNT], new int[Mage.TYPE_COUNT], owned,
                new int[]{Mage.WORKER, Mage.LIGHT, Mage.DARK, Mage.FIRE}, 0);
        m.startLevel(1);
        m.recruitMage(Mage.WORKER);
        m.update();
        assertEquals(1, m.getWalls().size());
        float wallX = m.getWalls().get(0).x;
        float wallHp = m.getWalls().get(0).hp;
        Enemy mage = new Enemy(wallX, 20, 3f, 1_000_000f, Enemy.MAGE);
        m.getEnemies().add(mage);
        for (int i = 0; i < 240; i++) {
            m.update();
        }
        assertEquals("le château est épargné", m.getCastleMaxHp(), m.getCastleHp());
        assertTrue("le mur encaisse", m.getWalls().isEmpty() || m.getWalls().get(0).hp < wallHp);
    }

    @Test
    public void goblinMage_onlyFromPlannedLevelsAndWaves() {
        assertEquals(7, Enemy.KIND_COUNT);
        assertEquals(Enemy.KIND_COUNT, Enemy.SPEED_MULT.length);
        assertEquals(Enemy.KIND_COUNT, Enemy.HP_MULT.length);
        assertEquals(Enemy.KIND_COUNT, Enemy.RADIUS_MULT.length);
        assertEquals(Enemy.KIND_COUNT, Enemy.CASTLE_DAMAGE_MULT.length);
        assertEquals(Enemy.KIND_COUNT, Enemy.GOLD_MULT.length);
        assertEquals(Enemy.KIND_COUNT, Enemy.SUMMON_DELAY.length);
        assertEquals(Enemy.KIND_COUNT, Enemy.SUMMON_COUNT.length);

        Random r = new Random(42);
        for (int lv = 1; lv < LevelSpec.MAGE_FIRST_LEVEL; lv++) {
            LevelSpec s = LevelSpec.forLevel(lv);
            for (int w = 1; w <= s.waves; w++) {
                assertEquals(0f, s.mageChance(w), 0f);
                for (int i = 0; i < 200; i++) {
                    assertTrue(s.rollKind(w, r) != Enemy.MAGE);
                }
            }
        }
        assertTrue(LevelSpec.forLevel(LevelSpec.MAGE_FIRST_LEVEL).mageChance(1) > 0);
        LevelSpec late = LevelSpec.forLevel(30);
        boolean rolled = false;
        for (int i = 0; i < 2000 && !rolled; i++) {
            rolled = late.rollKind(5, r) == Enemy.MAGE;
        }
        assertTrue("des gobelins mages apparaissent dans les niveaux avancés", rolled);
        assertTrue(late.mageChance(10) <= 0.10f);

        LevelSpec endless = LevelSpec.endless();
        for (int w = 1; w < LevelSpec.MAGE_FIRST_ENDLESS_WAVE; w++) {
            assertEquals(0f, endless.mageChance(w), 0f);
            for (int i = 0; i < 200; i++) {
                assertTrue(endless.rollKind(w, r) != Enemy.MAGE);
            }
        }
        assertTrue(endless.mageChance(LevelSpec.MAGE_FIRST_ENDLESS_WAVE) > 0);
    }
}
