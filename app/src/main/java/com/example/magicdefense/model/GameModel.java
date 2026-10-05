package com.example.magicdefense.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Tout l'état de la partie et ses règles. Aucun dessin, aucun toucher ici.
public class GameModel {

    // ---------- Réglages (équilibrage) ----------
    public static final int START_GOLD = 20;
    public static final int MAGE_COST = 10;
    public static final int GOLD_PER_KILL = 5;
    public static final int MAX_LEVEL = 5;
    public static final boolean SAME_TYPE_REQUIRED = true; // fusion du même élément uniquement
    private static final int SPAWN_DELAY = 60;             // ≈ 1 s entre deux ennemis

    // ---------- État ----------
    private final Board board;
    private final Random random = new Random();
    private GameState state = GameState.START;

    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Projectile> projectiles = new ArrayList<>();
    private Mage[][] grid = new Mage[Board.ROWS][Board.COLS]; // null = case vide

    private int gold = 0;
    private int wave = 0;
    private int enemiesLeftToSpawn = 0;
    private int spawnTimer = 0;

    public GameModel(Board board) {
        this.board = board;
    }

    // ---------- Accès en lecture (pour la vue) ----------
    public GameState getState() { return state; }
    public List<Enemy> getEnemies() { return enemies; }
    public List<Projectile> getProjectiles() { return projectiles; }
    public Mage getMage(int row, int col) { return grid[row][col]; }
    public int getGold() { return gold; }
    public int getWave() { return wave; }
    public boolean canAffordMage() { return gold >= MAGE_COST; }

    // ---------- Transitions d'état ----------
    public void startNewGame() {
        enemies.clear();
        projectiles.clear();
        grid = new Mage[Board.ROWS][Board.COLS];
        gold = START_GOLD;
        wave = 0;
        startNextWave();
        state = GameState.PLAYING;
    }

    public void pause() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        }
    }

    public void resume() {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
        }
    }

    // ---------- Une étape de jeu (appelée ≈ 60 fois/s) ----------
    public void update() {
        if (state != GameState.PLAYING) {
            return;
        }

        // 1. Apparition des ennemis de la vague
        if (enemiesLeftToSpawn > 0) {
            spawnTimer--;
            if (spawnTimer <= 0) {
                spawnEnemy();
                enemiesLeftToSpawn--;
                spawnTimer = SPAWN_DELAY;
            }
        }

        // 2. Déplacement des ennemis et défaite
        for (Enemy enemy : enemies) {
            enemy.update();
            if (enemy.y + enemy.radius >= board.gridTop) {
                state = GameState.GAME_OVER;
                return;
            }
        }

        // 3. Les mages tirent
        Enemy target = findTarget();
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                Mage mage = grid[row][col];
                if (mage == null) {
                    continue;
                }
                if (mage.cooldown > 0) {
                    mage.cooldown--;
                } else if (target != null) {
                    projectiles.add(new Projectile(board.cellCenterX(col), board.cellCenterY(row),
                            target, mage.getDamage(), mage.type, board.density));
                    mage.cooldown = mage.fireDelay;
                }
            }
        }

        // 4. Déplacement des projectiles et impacts
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);
            if (p.target.isDead()) {
                projectiles.remove(i);   // cible déjà tuée par un autre tir
            } else if (p.update()) {
                p.target.hp -= p.damage; // impact
                projectiles.remove(i);
            }
        }

        // 5. Retrait des ennemis morts et gain de PO
        for (int i = enemies.size() - 1; i >= 0; i--) {
            if (enemies.get(i).isDead()) {
                enemies.remove(i);
                gold += GOLD_PER_KILL;
            }
        }

        // 6. Vague terminée
        if (enemiesLeftToSpawn == 0 && enemies.isEmpty()) {
            startNextWave();
        }
    }

    // L'ennemi le plus bas (le plus proche du château), visible à l'écran
    private Enemy findTarget() {
        Enemy best = null;
        for (Enemy enemy : enemies) {
            if (enemy.y > 0 && (best == null || enemy.y > best.y)) {
                best = enemy;
            }
        }
        return best;
    }

    private void startNextWave() {
        wave++;
        enemiesLeftToSpawn = 3 + wave * 2; // 5, 7, 9... ennemis
        spawnTimer = 0;
    }

    private void spawnEnemy() {
        float radius = 20 * board.density;
        float x = radius + random.nextFloat() * (board.width - 2 * radius);
        float speed = (1f + wave * 0.2f) * board.density;
        int hp = 2 + wave; // 3 PV en vague 1, 4 en vague 2...
        enemies.add(new Enemy(x, radius, speed, hp));
    }

    // ---------- Actions du joueur ----------

    // Place un mage du type donné sur une case libre au hasard
    public void recruitMage(int type) {
        if (!canAffordMage()) {
            return;
        }

        ArrayList<int[]> freeCells = new ArrayList<>();
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                if (grid[row][col] == null) {
                    freeCells.add(new int[]{row, col});
                }
            }
        }
        if (freeCells.isEmpty()) {
            return; // damier plein
        }

        int[] cell = freeCells.get(random.nextInt(freeCells.size()));
        grid[cell[0]][cell[1]] = new Mage(type);
        gold -= MAGE_COST;
    }

    // Deux mages peuvent-ils fusionner ?
    public boolean canMerge(Mage a, Mage b) {
        if (a == null || b == null || a == b) {
            return false;
        }
        if (SAME_TYPE_REQUIRED && a.type != b.type) {
            return false;
        }
        return a.level == b.level && a.level < MAX_LEVEL;
    }

    // Lâcher le mage de (fromRow, fromCol) sur (toRow, toCol) : déplacement ou fusion
    public void dropMage(int fromRow, int fromCol, int toRow, int toCol) {
        Mage moved = grid[fromRow][fromCol];
        Mage target = grid[toRow][toCol];
        if (moved == null || moved == target) {
            return;
        }

        if (target == null) {
            // Case vide : on déplace
            grid[toRow][toCol] = moved;
            grid[fromRow][fromCol] = null;
        } else if (canMerge(moved, target)) {
            // Fusion : la cible gagne un niveau, l'autre disparaît
            target.level++;
            grid[fromRow][fromCol] = null;
        }
    }
}
