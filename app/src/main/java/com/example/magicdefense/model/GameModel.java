package com.example.magicdefense.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Tout l'état de la partie et ses règles. Aucun dessin, aucun toucher ici.
public class GameModel {

    // Événements du jeu, pour les retours (vibrations, sauvegarde, et plus tard les sons)
    public interface GameListener {
        void onCastleHit();
        void onUltimateTriggered();
        void onMerge();
        void onLevelCompleted();
        void onGameOver();
        void onEnemyKilled();
        void onRecruit();
    }

    private GameListener listener;

    public void setListener(GameListener listener) {
        this.listener = listener;
    }

    // ---------- Réglages (équilibrage) ----------
    public static final int START_GOLD = 20;
    public static final int MAGE_COST = 10;
    public static final int GOLD_PER_KILL = 5;
    public static final int MAX_LEVEL = 5;
    public static final boolean SAME_TYPE_REQUIRED = true; // fusion du même élément uniquement
    private static final int SPAWN_DELAY = 60;             // ≈ 1 s entre deux ennemis
    public static final int ULTIMATE_KILLS_REQUIRED = 20; // ennemis à tuer pour remplir la jauge
    public static final int STUN_DURATION = 240; // ≈ 4 s d'étourdissement
    // Château
    public static final int CASTLE_MAX_HP = 10000;
    public static final float STARS_3_MIN = 0.70f; // à partir de 70 % de PV : 3 étoiles
    public static final float STARS_2_MIN = 0.40f; // à partir de 40 % de PV : 2 étoiles
    private static final int HIT_FLASH_DURATION = 20; // ≈ 0,3 s de flash rouge

    // Nuit : plus dur, mais un peu plus rentable
    public static final float NIGHT_SPEED_MULT = 1.3f; // ennemis 30 % plus rapides
    public static final int GOLD_PER_KILL_NIGHT = 6;   // 20 % d'or en plus
    private static final int MODE_BANNER_DURATION = 120; // bandeau affiché ≈ 2 s
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
    // Ultime
    private int ultimateCharge = 0; // ennemis tués depuis le dernier ultime
    private int stunTimer = 0; // > 0 : les ennemis sont étourdis
    private boolean nightMode = false;
    private boolean nightModeDisabled = false; // option du joueur
    private boolean musicEnabled = true;       // option du joueur : musique de fond
    private int castleHp = CASTLE_MAX_HP;
    private int castleHitTimer = 0; // > 0 : le château vient d'être touché
    private int level = 1;
    private final ArrayList<Splash> splashes = new ArrayList<>();
    public static final int MAX_LEVELS = 6;

    private int unlockedLevel = 1;                           // plus haut niveau jouable
    private final int[] bestStars = new int[MAX_LEVELS + 1]; // index = numéro du niveau
    private int selectedLevel = 1;                           // niveau choisi sur la carte

    private float soundVolume = 1f; // position du curseur, de 0 à 1

    private GameState optionsReturnState = GameState.PAUSED; // écran à retrouver en quittant les options
    private int modeBannerTimer = 0; // > 0 : le bandeau jour/nuit est affiché

    private final ArrayList<Bolt> bolts = new ArrayList<>();
    private final ArrayList<HitEffect> hits = new ArrayList<>();



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

    // Remplissage de la jauge, entre 0 et 1 (pour l'affichage)
    public float getUltimateCharge() {
        return ultimateCharge / (float) ULTIMATE_KILLS_REQUIRED;
    }

    public int getUltimateKills() {
        return ultimateCharge;
    }

    public boolean isUltimateReady() {
        return state == GameState.PLAYING
                && ultimateCharge >= ULTIMATE_KILLS_REQUIRED
                && stunTimer == 0;
    }

    public boolean isStunned() {
        return stunTimer > 0;
    }

    public boolean isNightMode() {
        return nightMode;
    }

    // Le capteur propose, l'option du joueur a le dernier mot
    public void setNightMode(boolean night) {
        boolean newMode = night && !nightModeDisabled;
        if (newMode != nightMode && state == GameState.PLAYING) {
            modeBannerTimer = MODE_BANNER_DURATION; // le mode change pendant la partie : on prévient
        }
        nightMode = newMode;
    }
    public boolean isNightModeDisabled() {
        return nightModeDisabled;
    }

    public void toggleNightModeDisabled() {
        nightModeDisabled = !nightModeDisabled;
    }

    public boolean isMusicEnabled() {
        return musicEnabled;
    }

    public void setMusicEnabled(boolean on) {
        musicEnabled = on;
    }

    public void toggleMusic() {
        musicEnabled = !musicEnabled;
    }

    // Pause ou accueil → Options (on retient d'où l'on vient)
    public void openOptions() {
        if (state == GameState.PAUSED || state == GameState.START) {
            optionsReturnState = state;
            state = GameState.OPTIONS;
        }
    }

    // Options → écran d'origine
    public void closeOptions() {
        if (state == GameState.OPTIONS) {
            state = optionsReturnState;
        }
    }

    // PV restants du château, entre 0 et 1
    public float getCastleHpRatio() {
        return castleHp / (float) CASTLE_MAX_HP;
    }

    // Étoiles encore possibles selon les PV restants
    public int getStars() {
        float ratio = getCastleHpRatio();
        if (ratio >= STARS_3_MIN) {
            return 3;
        }
        if (ratio >= STARS_2_MIN) {
            return 2;
        }
        return 1;
    }

    // Intensité du flash de dégât, de 1 (vient d'être touché) à 0
    public float getCastleHitFlash() {
        return castleHitTimer / (float) HIT_FLASH_DURATION;
    }

    // Dégâts d'un ennemi qui atteint le château : plus forts à chaque vague
    private int enemyDamage() {
        return (int) (1000 + (difficulty() - 1) * 100);
    }

    public int getLevel() {
        return level;
    }

    // Difficulté : la vague 1 reste facile, mais chaque niveau accélère la progression
    private float difficulty() {
        return wave * (1 + 0.75f * (level - 1));
    }

    // Niveau 1 = 3 vagues, niveau 2 = 4, niveau 3 = 5...
    public int getWavesInLevel() {
        return wavesForLevel(level);
    }


    public List<Splash> getSplashes() {
        return splashes;
    }
    // Nombre de vagues d'un niveau : 1 → 3 vagues, 2 → 4...
    public static int wavesForLevel(int lv) {
        return lv + 2;
    }

    public int getUnlockedLevel() { return unlockedLevel; }
    public int getBestStars(int lv) { return bestStars[lv]; }
    public int getSelectedLevel() { return selectedLevel; }

    public boolean hasNextLevel() {
        return level < MAX_LEVELS;
    }

    // Toucher un niveau sur la carte : seulement s'il est débloqué
    public void selectLevel(int lv) {
        if (lv >= 1 && lv <= unlockedLevel) {
            selectedLevel = lv;
        }
    }

    // Chargement de la sauvegarde
    public void loadProgress(int unlocked, int[] stars, boolean nightDisabled) {
        unlockedLevel = Math.max(1, Math.min(unlocked, MAX_LEVELS));
        for (int lv = 1; lv <= MAX_LEVELS; lv++) {
            bestStars[lv] = stars[lv];
        }
        nightModeDisabled = nightDisabled;
        selectedLevel = unlockedLevel;
    }

    // Fin de niveau réussie : on garde le meilleur score et on débloque la suite
    private void recordVictory() {
        int stars = getStars();
        if (stars > bestStars[level]) {
            bestStars[level] = stars;
        }
        if (level == unlockedLevel && level < MAX_LEVELS) {
            unlockedLevel = level + 1;
        }
        selectedLevel = hasNextLevel() ? level + 1 : level;
        if (listener != null) {
            listener.onLevelCompleted();
        }
    }

    public float getSoundVolume() {
        return soundVolume;
    }

    public void setSoundVolume(float volume) {
        soundVolume = Math.max(0f, Math.min(1f, volume)); // toujours entre 0 et 1
    }

    public GameState getOptionsReturnState() {
        return optionsReturnState;
    }

    // Bandeau jour/nuit : 1 quand il apparaît, 0 quand il a disparu
    public float getModeBanner() {
        return modeBannerTimer / (float) MODE_BANNER_DURATION;
    }


    public List<Bolt> getBolts() { return bolts; }
    public List<HitEffect> getHits() { return hits; }

    // ---------- Transitions d'état ----------
    public void startLevel(int newLevel) {
        level = newLevel;
        enemies.clear();
        projectiles.clear();
        splashes.clear();
        bolts.clear();
        hits.clear();
        grid = new Mage[Board.ROWS][Board.COLS];
        gold = START_GOLD;
        wave = 0;
        ultimateCharge = 0;
        stunTimer = 0;
        castleHp = CASTLE_MAX_HP;
        castleHitTimer = 0;
        modeBannerTimer = 0;
        startNextWave();
        state = GameState.PLAYING;

    }

    // Depuis l'accueil : on commence au niveau 1
    public void startNewGame() {
        startLevel(1);
    }

    // Recommencer le niveau en cours (pause, game over)
    public void restartLevel() {
        startLevel(level);
    }

    public void nextLevel() {
        if (hasNextLevel()) {
            startLevel(level + 1);
        }
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

    // Retour à la carte, avec le niveau en cours sélectionné
    public void quitToMenu() {
        if (state != GameState.VICTORY) {
            selectedLevel = level;
        }
        state = GameState.START;
    }

    // ---------- Une étape de jeu (appelée ≈ 60 fois/s) ----------
    public void update() {
        if (state != GameState.PLAYING) {
            return;
        }

        if (castleHitTimer > 0) {
            castleHitTimer--;
        }


        if (modeBannerTimer > 0) {
            modeBannerTimer--;
        }

        // Les éclaboussures s'estompent puis disparaissent
        for (int i = splashes.size() - 1; i >= 0; i--) {
            if (splashes.get(i).update()) {
                splashes.remove(i);
            }
        }

        for (int i = bolts.size() - 1; i >= 0; i--) {
            if (bolts.get(i).update()) {
                bolts.remove(i);
            }
        }
        for (int i = hits.size() - 1; i >= 0; i--) {
            if (hits.get(i).update()) {
                hits.remove(i);
            }
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

        // 2. Déplacement des ennemis (sauf s'ils sont étourdis)
        if (stunTimer > 0) {
            stunTimer--;
        } else {
            for (int i = enemies.size() - 1; i >= 0; i--) {
                Enemy enemy = enemies.get(i);
                enemy.update(nightMode ? NIGHT_SPEED_MULT : 1f);

                if (enemy.y + enemy.radius >= board.gridTop) {
                    enemies.remove(i);              // l'ennemi s'écrase sur le château
                    castleHp -= enemyDamage();
                    castleHitTimer = HIT_FLASH_DURATION;

                    if (listener != null) {
                        listener.onCastleHit();     // vibration (et plus tard un son)
                    }

                    if (castleHp <= 0) {
                        castleHp = 0;
                        state = GameState.GAME_OVER;
                        if (listener != null) {
                            listener.onGameOver();
                        }
                        return;
                    }
                }
            }
        }

        // 3. Les mages attaquent, chacun à sa façon
        List<Enemy> targets = findTargets();
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                Mage mage = grid[row][col];
                if (mage == null) {
                    continue;
                }
                if (mage.cooldown > 0) {
                    mage.cooldown--;
                    continue;
                }
                Enemy target = firstAlive(targets);
                if (target == null) {
                    continue;
                }
                float tipX = board.staffTipX(col);
                float tipY = board.staffTipY(row);

                switch (mage.type) {
                    case Mage.LIGHTNING:
                        // Foudre : l'éclair relie instantanément le bâton à la cible
                        target.hp -= mage.getDamage();
                        bolts.add(new Bolt(tipX, tipY, target.x, target.y, mage.level));
                        hits.add(new HitEffect(target.x, target.y, Mage.LIGHTNING, mage.level));
                        break;

                    case Mage.WATER:
                        if (mage.level >= 4) {
                            // Cyclone et tsunami : partent du bâton et foncent vers l'ennemi
                            projectiles.add(new Projectile(tipX, tipY, target, mage, board.density));
                        } else {
                            // Goutte, bulle et averse : tombent du ciel sur l'ennemi
                            projectiles.add(new Projectile(target.x, target.y - 160 * board.density,
                                    target, mage, board.density));
                        }
                        break;

                    default:
                        // Feu et Terre : tirés depuis l'embout du bâton (Terre : salve en éventail)
                        int shots = mage.getShots();
                        for (int k = 0; k < shots; k++) {
                            float offset = (k - (shots - 1) / 2f) * 12 * board.density;
                            projectiles.add(new Projectile(tipX + offset, tipY, target, mage, board.density));
                        }
                        break;
                }
                mage.cooldown = mage.getFireDelay();
            }
        }

        // 4. Déplacement des projectiles et impacts
        for (int i = projectiles.size() - 1; i >= 0; i--) {
            Projectile p = projectiles.get(i);

            // Cible déjà tuée (ou écrasée sur le château) : le projectile se redirige
            if (p.target.isDead() || !enemies.contains(p.target)) {
                p.target = findClosestEnemy(p.x, p.y);
                if (p.target == null) {
                    projectiles.remove(i); // plus aucun ennemi : le projectile disparaît
                    continue;
                }
            }

            if (p.update()) {
                if (p.aoeRadius > 0) {
                    splashes.add(new Splash(p.target.x, p.target.y, p.aoeRadius, p.level)); // effet visuel
                    // Eau : touche tous les ennemis dans la zone autour de l'impact
                    for (Enemy e : enemies) {
                        float dx = e.x - p.target.x;
                        float dy = e.y - p.target.y;
                        if (dx * dx + dy * dy <= p.aoeRadius * p.aoeRadius) {
                            e.hp -= p.damage;
                        }
                    }

                } else {
                    p.target.hp -= p.damage;
                    hits.add(new HitEffect(p.target.x, p.target.y, p.type, p.level)); // explosion, éclats...
                }
                projectiles.remove(i);
            }
        }

        // 5. Retrait des ennemis morts et gain de PO
        for (int i = enemies.size() - 1; i >= 0; i--) {
            if (enemies.get(i).isDead()) {
                enemies.remove(i);
                gold += nightMode ? GOLD_PER_KILL_NIGHT : GOLD_PER_KILL;
                if (listener != null) {
                    listener.onEnemyKilled();
                }
                if (ultimateCharge < ULTIMATE_KILLS_REQUIRED) {
                    ultimateCharge++; // chaque ennemi tué remplit la jauge
                }
            }
        }

        // 6. Vague terminée : vague suivante, ou victoire si c'était la dernière
        if (enemiesLeftToSpawn == 0 && enemies.isEmpty()) {
            if (wave >= getWavesInLevel()) {
                projectiles.clear();
                recordVictory();
                state = GameState.VICTORY;
            } else {
                startNextWave();
            }
        }
    }

    // Ennemis visibles, du plus proche du château au plus éloigné
    private List<Enemy> findTargets() {
        ArrayList<Enemy> targets = new ArrayList<>();
        for (Enemy enemy : enemies) {
            if (enemy.y > 0) {
                targets.add(enemy);
            }
        }
        targets.sort((a, b) -> Float.compare(b.y, a.y)); // le plus bas en premier
        return targets;
    }

    // Premier ennemi de la liste encore en vie (évite de viser un ennemi déjà condamné)
    private Enemy firstAlive(List<Enemy> targets) {
        for (Enemy e : targets) {
            if (!e.isDead()) {
                return e;
            }
        }
        return null;
    }

    // Ennemi vivant et visible le plus proche d'un point (pour rediriger un projectile)
    private Enemy findClosestEnemy(float x, float y) {
        Enemy best = null;
        float bestDist = Float.MAX_VALUE;
        for (Enemy e : enemies) {
            if (e.isDead() || e.y <= 0) {
                continue;
            }
            float dx = e.x - x;
            float dy = e.y - y;
            float dist = dx * dx + dy * dy;
            if (dist < bestDist) {
                bestDist = dist;
                best = e;
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
        // Apparition au hasard dans la largeur de la colonne de jeu
        float x = board.playLeft + radius + random.nextFloat() * (board.playWidth - 2 * radius);
        float d = difficulty();
        // Vitesse ajustée à la hauteur de l'écran : même temps de traversée sur tous les appareils
        float speed = (1f + d * 0.08f) * board.density * board.fieldScale();
        float hp = 2 + d;
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
        if (listener != null) {
            listener.onRecruit();
        }
    }


    // Lance l'ultime si la jauge est pleine. Retourne true s'il a été lancé.
    public boolean triggerUltimate() {
        if (!isUltimateReady()) {
            return false; // secousse ignorée : jauge pas pleine, pause, accueil...
        }
        stunTimer = STUN_DURATION;
        ultimateCharge = 0;
        if (listener != null) {
            listener.onUltimateTriggered();
        }
        return true;
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
            if (listener != null) {
                listener.onMerge();
            }
        }
    }
}
