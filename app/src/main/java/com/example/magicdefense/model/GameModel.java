package com.example.magicdefense.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

// Tout l'état de la partie et ses règles. Aucun dessin, aucun toucher ici.
public class GameModel {

    // Événements du jeu, pour les retours (vibrations, sons, sauvegarde)
    public interface GameListener {
        void onCastleHit();
        void onUltimateTriggered();
        void onMerge();
        void onLevelCompleted();
        void onGameOver();
        void onEnemyKilled();
        void onRecruit();
        void onChestOpened();     // le couvercle saute (première carte du coffre)
        void onChestCardRevealed(); // une carte suivante sort du coffre
        void onMageUpgraded();
        void onUnitBought();
        void onReaction();
        void onBossArrived();
        void onProgressChanged(); // progression permanente modifiée : à sauvegarder
    }

    private GameListener listener;

    public void setListener(GameListener listener) {
        this.listener = listener;
    }

    // ---------- Réglages (équilibrage) ----------
    public static final int START_GOLD = 20;
    public static final int MAGE_COST = 10;
    public static final int GOLD_PER_KILL = 5;
    public static final int MAX_LEVEL = 5;                 // niveau de fusion maximal d'un mage
    public static final boolean SAME_TYPE_REQUIRED = true; // fusion du même élément uniquement
    public static final int ULTIMATE_KILLS_REQUIRED = 20; // ennemis à tuer pour remplir la jauge
    public static final int STUN_DURATION = 240; // ≈ 4 s d'étourdissement
    // Château
    public static final int CASTLE_MAX_HP = 10000;
    public static final float STARS_3_MIN = 0.70f; // à partir de 70 % de PV : 3 étoiles
    public static final float STARS_2_MIN = 0.40f; // à partir de 40 % de PV : 2 étoiles
    private static final int HIT_FLASH_DURATION = 20; // ≈ 0,3 s de flash rouge
    // Plafonds : au-delà du niveau 6, seuls les PV des gobelins continuent de grimper
    private static final float MAX_ENEMY_SPEED = 4.2f;   // vitesse max (≈ celle du niveau 6)
    private static final int MAX_ENEMY_DAMAGE = 5000;    // dégâts max d'un gobelin normal
    private static final float MAX_HIT_RATIO = 0.6f;     // aucun coup (même du boss) ne dépasse 60 % des PV

    // Nuit : plus dur, mais un peu plus rentable
    public static final float NIGHT_SPEED_MULT = 1.3f; // ennemis 30 % plus rapides
    public static final int GOLD_PER_KILL_NIGHT = 6;   // 20 % d'or en plus
    private static final int MODE_BANNER_DURATION = 120; // bandeau affiché ≈ 2 s
    private static final int ANNOUNCE_DURATION = 150;    // bandeau boss / biome ≈ 2,5 s

    // Carte : les niveaux (sans fin) sont affichés par pages de 6
    public static final int LEVELS_PER_PAGE = 6;
    // Écran MAGES : 4 éléments par page
    public static final int UPGRADE_ROWS_PER_PAGE = 4;

    // Ouverture de coffre, carte par carte (façon jeu de cartes) :
    // le coffre tombe (CHEST_INTRO_FRAMES), puis chaque toucher fait sortir une carte, puis récapitulatif
    public static final int CHEST_INTRO_FRAMES = 45;
    public static final int CARD_GEMS = -2;              // « type » de la carte des gemmes

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
    private boolean bossPending = false; // le boss arrive en dernier dans cette vague
    // Ultime
    private int ultimateCharge = 0; // ennemis tués depuis le dernier ultime
    private int stunTimer = 0; // > 0 : les ennemis sont étourdis
    private boolean nightMode = false;
    private boolean nightModeDisabled = false; // option du joueur
    private boolean musicEnabled = true;       // option du joueur : musique de fond
    private boolean leftHanded = false;        // option du joueur : interface en miroir
    private int castleHp = CASTLE_MAX_HP;
    private int castleHitTimer = 0; // > 0 : le château vient d'être touché
    private int level = 1;
    private LevelSpec spec = LevelSpec.forLevel(1); // description du niveau en cours
    private final ArrayList<Splash> splashes = new ArrayList<>();
    private final ArrayList<Zone> zones = new ArrayList<>();          // lave, vapeur
    private final ArrayList<Reaction> reactions = new ArrayList<>();  // effets visuels des réactions

    private int unlockedLevel = 1;          // plus haut niveau jouable (sans limite)
    private int[] bestStars = new int[LEVELS_PER_PAGE + 1]; // index = numéro du niveau, agrandi au besoin
    private int selectedLevel = 1;          // niveau choisi sur la carte
    private int mapPage = 0;                // page de la carte affichée (0 = niveaux 1 à 6)
    private int upgradePage = 0;            // page de l'écran MAGES

    // Progression permanente (coffres, fragments, rangs, gemmes, équipe) et fin de partie
    private final Progression progression = new Progression();
    private int lastChestsEarned = 0;       // coffres gagnés à la dernière fin de partie
    private boolean endlessRewarded = false; // la partie infinie en cours a déjà été récompensée
    private boolean lastEndlessRecord = false; // la dernière partie infinie a battu le record
    private int chestAnimFrame = 0;          // images depuis l'ouverture du coffre
    private int chestCardFrame = 0;          // images depuis la dernière carte sortie
    private int chestRevealed = 0;           // cartes déjà sorties du coffre
    private boolean chestSummary = false;    // récapitulatif affiché
    private int[] chestCardTypes = new int[0];   // contenu du coffre, dans l'ordre de sortie
    private int[] chestCardAmounts = new int[0];

    // Bandeau d'annonce (boss, nouveau biome)
    private int announceTimer = 0;
    private String announceTitle = "";
    private String announceSubtitle = "";
    private boolean announceBoss = false;

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
    public Progression getProgression() { return progression; }
    public boolean isEndless() { return spec.endless; }
    public LevelSpec getSpec() { return spec; }
    public int getLastChestsEarned() { return lastChestsEarned; }
    public boolean isLastEndlessRecord() { return lastEndlessRecord; }
    public List<Zone> getZones() { return zones; }
    public List<Reaction> getReactions() { return reactions; }

    // Biome du décor en cours (change toutes les 10 vagues en mode infini)
    public int getBiome() {
        return spec.biome(Math.max(1, wave));
    }

    // Boss vivant sur le terrain (pour sa grande barre de vie), ou null
    public Enemy getBoss() {
        for (Enemy e : enemies) {
            if (e.isBoss() && !e.isDead()) {
                return e;
            }
        }
        return null;
    }

    // Bandeau d'annonce : 1 quand il apparaît, 0 quand il a disparu
    public float getAnnounce() { return announceTimer / (float) ANNOUNCE_DURATION; }
    public String getAnnounceTitle() { return announceTitle; }
    public String getAnnounceSubtitle() { return announceSubtitle; }
    public boolean isAnnounceBoss() { return announceBoss; }

    private void announce(String title, String subtitle, boolean boss) {
        announceTitle = title;
        announceSubtitle = subtitle;
        announceBoss = boss;
        announceTimer = ANNOUNCE_DURATION;
    }

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

    public boolean isLeftHanded() {
        return leftHanded;
    }

    public void setLeftHanded(boolean on) {
        leftHanded = on;
    }

    public void toggleLeftHanded() {
        leftHanded = !leftHanded;
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

    // Dégâts d'un ennemi qui atteint le château : plus forts à chaque vague (plafonnés)
    private int enemyDamage(Enemy enemy) {
        float base = Math.min(MAX_ENEMY_DAMAGE, 1000 + (difficulty() - 1) * 100);
        float dmg = base * Enemy.CASTLE_DAMAGE_MULT[enemy.kind];
        return (int) Math.min(dmg, CASTLE_MAX_HP * MAX_HIT_RATIO);
    }

    public int getLevel() {
        return level;
    }

    // Difficulté de la vague en cours (voir LevelSpec)
    private float difficulty() {
        return spec.difficulty(wave);
    }

    // Nombre de vagues du niveau en cours (0 en mode infini)
    public int getWavesInLevel() {
        return spec.waves;
    }


    public List<Splash> getSplashes() {
        return splashes;
    }
    // Nombre de vagues d'un niveau : 1 → 3 vagues, 2 → 4... puis généré à partir du niveau 7
    public static int wavesForLevel(int lv) {
        return LevelSpec.forLevel(lv).waves;
    }

    public int getUnlockedLevel() { return unlockedLevel; }
    public int getSelectedLevel() { return selectedLevel; }

    public int getBestStars(int lv) {
        return lv >= 1 && lv < bestStars.length ? bestStars[lv] : 0;
    }

    // Les niveaux sont infinis : il y a toujours un niveau suivant (sauf en mode infini)
    public boolean hasNextLevel() {
        return !spec.endless;
    }

    // ---------- Carte des niveaux (pages de 6) ----------
    public int getMapPage() { return mapPage; }

    // Dernière page consultable : celle qui contient le plus haut niveau débloqué
    public int getMaxMapPage() {
        return (unlockedLevel - 1) / LEVELS_PER_PAGE;
    }

    // Biome de la page de carte affichée
    public int getMapBiome() {
        return LevelSpec.biomeForLevel(levelAtSlot(1));
    }

    // Numéro du niveau affiché dans la case `slot` (1 à 6) de la page en cours
    public int levelAtSlot(int slot) {
        return mapPage * LEVELS_PER_PAGE + slot;
    }

    public boolean canChangeMapPage(int delta) {
        int target = mapPage + delta;
        return target >= 0 && target <= getMaxMapPage();
    }

    public void changeMapPage(int delta) {
        if (canChangeMapPage(delta)) {
            mapPage += delta;
        }
    }

    private void showPageOf(int lv) {
        mapPage = (lv - 1) / LEVELS_PER_PAGE;
    }

    // Toucher un niveau sur la carte : seulement s'il est débloqué
    public void selectLevel(int lv) {
        if (lv >= 1 && lv <= unlockedLevel) {
            selectedLevel = lv;
        }
    }

    // Chargement de la sauvegarde
    public void loadProgress(int unlocked, int[] stars, boolean nightDisabled) {
        unlockedLevel = Math.max(1, unlocked);
        bestStars = new int[Math.max(unlockedLevel, LEVELS_PER_PAGE) + 1];
        for (int lv = 1; lv < stars.length && lv < bestStars.length; lv++) {
            bestStars[lv] = stars[lv];
        }
        nightModeDisabled = nightDisabled;
        selectedLevel = unlockedLevel;
        showPageOf(selectedLevel);
    }

    // Fin de niveau réussie : on garde le meilleur score, on débloque la suite, on donne les coffres
    private void recordVictory() {
        int stars = getStars();
        if (level >= bestStars.length) {
            bestStars = Arrays.copyOf(bestStars, level * 2 + 1); // tableau agrandi
        }
        if (stars > bestStars[level]) {
            bestStars[level] = stars;
        }
        if (level == unlockedLevel) {
            unlockedLevel = level + 1;
        }
        selectedLevel = level + 1;
        showPageOf(selectedLevel);

        lastChestsEarned = Progression.chestsForLevel(level);
        progression.addChests(lastChestsEarned);
        if (listener != null) {
            listener.onLevelCompleted();
        }
    }

    // Fin d'une partie infinie (game over, abandon) : coffres et record, une seule fois par partie
    private void finishEndlessRun() {
        if (!spec.endless || endlessRewarded) {
            return;
        }
        endlessRewarded = true;
        int wavesCleared = wave - 1; // la vague en cours n'a pas été repoussée
        lastChestsEarned = Progression.chestsForEndless(wavesCleared);
        progression.addChests(lastChestsEarned);
        lastEndlessRecord = progression.recordEndlessWave(wave);
        if (listener != null) {
            listener.onProgressChanged();
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
        level = Math.max(1, newLevel);
        startRun(LevelSpec.forLevel(level));
    }

    // Mode vague infinie : les vagues s'enchaînent jusqu'à la chute du château
    public void startEndless() {
        level = 1;
        startRun(LevelSpec.endless());
    }

    private void startRun(LevelSpec newSpec) {
        spec = newSpec;
        enemies.clear();
        projectiles.clear();
        splashes.clear();
        bolts.clear();
        hits.clear();
        zones.clear();
        reactions.clear();
        grid = new Mage[Board.ROWS][Board.COLS];
        gold = START_GOLD;
        wave = 0;
        ultimateCharge = 0;
        stunTimer = 0;
        castleHp = CASTLE_MAX_HP;
        castleHitTimer = 0;
        modeBannerTimer = 0;
        announceTimer = 0;
        lastChestsEarned = 0;
        endlessRewarded = false;
        lastEndlessRecord = false;
        startNextWave();
        state = GameState.PLAYING;
    }

    // Depuis l'accueil : on commence au niveau 1
    public void startNewGame() {
        startLevel(1);
    }

    // Recommencer la partie en cours (pause, game over)
    public void restartLevel() {
        if (spec.endless) {
            finishEndlessRun(); // l'abandon garde les coffres déjà mérités
            startEndless();
        } else {
            startLevel(level);
        }
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
        if (spec.endless) {
            finishEndlessRun();
        } else if (state != GameState.VICTORY) {
            selectedLevel = level;
            showPageOf(level);
        }
        state = GameState.START;
    }

    // ---------- Écran MAGES : coffres, rangs, équipe ----------
    public void openUpgrades() {
        if (state == GameState.START) {
            upgradePage = 0;
            state = GameState.UPGRADES;
        }
    }

    public void closeUpgrades() {
        if (state == GameState.UPGRADES) {
            state = GameState.START;
        }
    }

    public int getUpgradePage() { return upgradePage; }

    public int getUpgradePageCount() {
        int owned = progression.ownedCount();
        return Math.max(1, (owned + UPGRADE_ROWS_PER_PAGE - 1) / UPGRADE_ROWS_PER_PAGE);
    }

    public boolean canChangeUpgradePage(int delta) {
        int target = upgradePage + delta;
        return target >= 0 && target < getUpgradePageCount();
    }

    public void changeUpgradePage(int delta) {
        if (canChangeUpgradePage(delta)) {
            upgradePage += delta;
        }
    }

    // Élément affiché sur la ligne `row` de la page en cours (seulement les mages possédés), ou -1
    public int upgradeTypeAtRow(int row) {
        int index = upgradePage * UPGRADE_ROWS_PER_PAGE + row;
        int n = 0;
        for (int t = 0; t < Mage.TYPE_COUNT; t++) {
            if (progression.isOwned(t)) {
                if (n == index) {
                    return t;
                }
                n++;
            }
        }
        return -1;
    }

    // Ouvre un coffre et lance l'animation (depuis l'écran MAGES, ou « encore » à la fin de l'animation)
    public boolean openChest() {
        boolean fromAnim = state == GameState.CHEST_OPENING && isChestRevealComplete();
        if ((state != GameState.UPGRADES && !fromAnim) || !progression.openChest(random)) {
            return false;
        }
        state = GameState.CHEST_OPENING;
        chestAnimFrame = 0;
        chestCardFrame = 0;
        chestRevealed = 0;
        chestSummary = false;
        buildChestCards();
        if (listener != null) {
            listener.onProgressChanged(); // sauvegardé tout de suite (même si l'appli est fermée pendant l'animation)
        }
        return true;
    }

    // Cartes du coffre : une par élément obtenu (de la plus petite à la plus grosse), les gemmes en dernier
    private void buildChestCards() {
        int n = getChestCardCount();
        chestCardTypes = new int[n];
        chestCardAmounts = new int[n];
        int k = 0;
        for (int t = 0; t < Mage.TYPE_COUNT; t++) {
            if (progression.getLastChestReward(t) > 0) {
                chestCardTypes[k] = t;
                chestCardAmounts[k] = progression.getLastChestReward(t);
                k++;
            }
        }
        // Tri par quantité croissante : le suspense monte jusqu'à la dernière carte
        for (int i = 1; i < k; i++) {
            for (int j = i; j > 0 && chestCardAmounts[j] < chestCardAmounts[j - 1]; j--) {
                int ta = chestCardTypes[j];
                chestCardTypes[j] = chestCardTypes[j - 1];
                chestCardTypes[j - 1] = ta;
                int aa = chestCardAmounts[j];
                chestCardAmounts[j] = chestCardAmounts[j - 1];
                chestCardAmounts[j - 1] = aa;
            }
        }
        if (progression.getLastChestGems() > 0) {
            chestCardTypes[k] = CARD_GEMS;
            chestCardAmounts[k] = progression.getLastChestGems();
        }
    }

    public int getChestAnimFrame() { return chestAnimFrame; }
    public int getChestCardFrame() { return chestCardFrame; }
    public int getChestRevealed() { return chestRevealed; }
    public boolean isChestIntroDone() { return chestAnimFrame >= CHEST_INTRO_FRAMES; }

    // Carte i (dans l'ordre de sortie) : élément (ou CARD_GEMS) et quantité
    public int getChestCardType(int i) { return chestCardTypes[i]; }
    public int getChestCardAmount(int i) { return chestCardAmounts[i]; }

    // Nombre de cartes de récompense : une par élément obtenu, plus les gemmes
    public int getChestCardCount() {
        int n = progression.getLastChestGems() > 0 ? 1 : 0;
        for (int t = 0; t < Mage.TYPE_COUNT; t++) {
            if (progression.getLastChestReward(t) > 0) {
                n++;
            }
        }
        return n;
    }

    // Toutes les cartes sont sorties et le récapitulatif est affiché
    public boolean isChestRevealComplete() {
        return chestSummary;
    }

    // Toucher pendant l'ouverture : arrivée du coffre → carte suivante → récapitulatif → fermer
    public void tapChestOpening() {
        if (state != GameState.CHEST_OPENING) {
            return;
        }
        if (!isChestIntroDone()) {
            chestAnimFrame = CHEST_INTRO_FRAMES; // le coffre se pose tout de suite
        } else if (chestRevealed < chestCardTypes.length) {
            if (listener != null) {
                if (chestRevealed == 0) {
                    listener.onChestOpened();       // le couvercle saute
                } else {
                    listener.onChestCardRevealed(); // carte suivante
                }
            }
            chestRevealed++;
            chestCardFrame = 0;
        } else if (!chestSummary) {
            chestSummary = true;
            chestCardFrame = 0;
        } else {
            state = GameState.UPGRADES;
        }
    }

    private void updateChestAnimation() {
        chestAnimFrame++;
        chestCardFrame++;
    }

    public boolean upgradeMage(int type) {
        if (state != GameState.UPGRADES || !progression.upgrade(type)) {
            return false;
        }
        if (listener != null) {
            listener.onMageUpgraded();
            listener.onProgressChanged();
        }
        return true;
    }

    // Toucher le portrait d'un mage : il rejoint ou quitte l'équipe
    public boolean toggleTeam(int type) {
        if (state != GameState.UPGRADES || !progression.toggleTeam(type)) {
            return false;
        }
        if (listener != null) {
            listener.onProgressChanged();
        }
        return true;
    }

    // ---------- Marché ----------
    public void openMarket() {
        if (state == GameState.START) {
            state = GameState.MARKET;
        }
    }

    public void closeMarket() {
        if (state == GameState.MARKET) {
            state = GameState.START;
        }
    }

    public boolean buyUnit(int type) {
        if (state != GameState.MARKET || !progression.buy(type)) {
            return false;
        }
        if (listener != null) {
            listener.onUnitBought();
            listener.onProgressChanged();
        }
        return true;
    }

    // ---------- Une étape de jeu (appelée ≈ 60 fois/s) ----------
    public void update() {
        if (state == GameState.CHEST_OPENING) {
            updateChestAnimation();
            return;
        }
        if (state != GameState.PLAYING) {
            return;
        }

        if (castleHitTimer > 0) {
            castleHitTimer--;
        }
        if (modeBannerTimer > 0) {
            modeBannerTimer--;
        }
        if (announceTimer > 0) {
            announceTimer--;
        }

        // Les effets s'estompent puis disparaissent
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
        for (int i = reactions.size() - 1; i >= 0; i--) {
            if (reactions.get(i).update()) {
                reactions.remove(i);
            }
        }

        // Zones au sol : la lave et les flammes brûlent, la vapeur et la boue ralentissent,
        // le marécage empoisonne
        for (int i = zones.size() - 1; i >= 0; i--) {
            Zone z = zones.get(i);
            for (Enemy e : enemies) {
                if (z.contains(e)) {
                    switch (z.type) {
                        case Zone.LAVA:
                        case Zone.FIRE:
                            hurt(e, z.dps / 60f);
                            break;
                        case Zone.STEAM:
                            e.slow(Reaction.STEAM_SLOW, 5);
                            break;
                        case Zone.MUD:
                            e.slow(Reaction.MUD_SLOW, 5);
                            break;
                        case Zone.BOG:
                            e.poison(z.dps, 60);
                            break;
                    }
                }
            }
            if (z.update()) {
                zones.remove(i);
            }
        }

        // Poison et marques élémentaires (s'écoulent même pendant l'ultime)
        for (Enemy e : enemies) {
            e.tickStatus();
        }

        // 1. Apparition des ennemis de la vague (le boss en dernier)
        if (enemiesLeftToSpawn > 0) {
            spawnTimer--;
            if (spawnTimer <= 0) {
                if (bossPending && enemiesLeftToSpawn == 1) {
                    spawnBoss();
                } else {
                    spawnEnemy(spec.rollKind(wave, random));
                }
                enemiesLeftToSpawn--;
                spawnTimer = spec.spawnDelay(wave);
            }
        }

        // 2. Déplacement des ennemis (sauf pendant l'ultime) et invocations
        if (stunTimer > 0) {
            stunTimer--;
        } else {
            ArrayList<Enemy> summoned = null;
            for (int i = enemies.size() - 1; i >= 0; i--) {
                Enemy enemy = enemies.get(i);
                enemy.update(nightMode ? NIGHT_SPEED_MULT : 1f);

                // Chamans et boss : invoquent des diablotins à intervalle régulier
                if (Enemy.SUMMON_DELAY[enemy.kind] > 0 && enemy.stunTimer == 0 && enemy.y > 0) {
                    enemy.summonTimer--;
                    if (enemy.summonTimer <= 0) {
                        enemy.summonTimer = Enemy.SUMMON_DELAY[enemy.kind];
                        if (summoned == null) {
                            summoned = new ArrayList<>();
                        }
                        summonImps(enemy, summoned);
                    }
                }

                if (enemy.y + enemy.radius >= board.gridTop) {
                    enemies.remove(i);              // l'ennemi s'écrase sur le château
                    castleHp -= enemyDamage(enemy);
                    castleHitTimer = HIT_FLASH_DURATION;

                    if (listener != null) {
                        listener.onCastleHit();     // vibration et son
                    }

                    if (castleHp <= 0) {
                        castleHp = 0;
                        state = GameState.GAME_OVER;
                        finishEndlessRun(); // mode infini : coffres selon la vague atteinte
                        if (listener != null) {
                            listener.onGameOver();
                        }
                        return;
                    }
                }
            }
            if (summoned != null) {
                enemies.addAll(summoned);
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
                        applyHit(target, mage.getDamage(), Mage.LIGHTNING, 0, 0, 0);
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
                        // Feu, Terre, Poison, Air, Invocateur : tirés depuis l'embout du bâton
                        // (Terre : salve en éventail)
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
                    float cx = p.target.x;
                    float cy = p.target.y;
                    for (Enemy e : enemies) {
                        float dx = e.x - cx;
                        float dy = e.y - cy;
                        if (dx * dx + dy * dy <= p.aoeRadius * p.aoeRadius) {
                            applyHit(e, p.damage, p.type, 0, 0, 0);
                        }
                    }
                } else if (p.fromReaction) {
                    hurt(p.target, p.damage); // feu follet : pas de marque, pas de nouvelle réaction
                } else {
                    applyHit(p.target, p.damage, p.type, p.poisonDps, p.knockback, p.stunFrames);
                    hits.add(new HitEffect(p.target.x, p.target.y, p.type, p.level)); // explosion, éclats...
                }
                projectiles.remove(i);
            }
        }

        // 5. Retrait des ennemis morts et gain de PO
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy dead = enemies.get(i);
            if (dead.isDead()) {
                enemies.remove(i);
                int base = nightMode ? GOLD_PER_KILL_NIGHT : GOLD_PER_KILL;
                gold += Math.max(1, Math.round(base * Enemy.GOLD_MULT[dead.kind]));
                if (listener != null) {
                    listener.onEnemyKilled();
                }
                if (ultimateCharge < ULTIMATE_KILLS_REQUIRED) {
                    ultimateCharge++; // chaque ennemi tué remplit la jauge
                }
            }
        }

        // 6. Vague terminée : vague suivante, ou victoire si c'était la dernière (jamais en mode infini)
        if (enemiesLeftToSpawn == 0 && enemies.isEmpty()) {
            if (!spec.endless && wave >= spec.waves) {
                projectiles.clear();
                zones.clear();
                state = GameState.VICTORY;
                recordVictory();
            } else {
                startNextWave();
            }
        }
    }

    // ---------- Coups et réactions élémentaires ----------

    // Un coup d'élément `element` touche l'ennemi : dégâts, effets, puis marque ou réaction
    private void applyHit(Enemy e, float damage, int element, float poisonDps, float knockback, int stun) {
        hurt(e, damage);
        if (poisonDps > 0) {
            e.poison(poisonDps, Mage.POISON_DURATION);
        }
        if (knockback > 0) {
            e.knockBack(knockback);
        }
        if (stun > 0) {
            e.stun(stun);
        }
        if (!Reaction.leavesMark(element)) {
            return;
        }
        int r = Reaction.between(e.mark, element);
        if (r != Reaction.NONE) {
            e.mark = Enemy.NO_MARK;
            e.markTimer = 0;
            triggerReaction(r, e, damage);
        } else {
            e.mark = element;
            e.markTimer = Enemy.MARK_DURATION;
        }
    }

    // Effet de la réaction autour de l'ennemi e (dmg = dégâts du coup qui l'a déclenchée)
    private void triggerReaction(int r, Enemy e, float dmg) {
        float d = board.density;
        float ringRadius = 60 * d;
        switch (r) {
            case Reaction.ELECTROCUTION: {
                hurt(e, dmg * Reaction.ELECTRO_BONUS);
                e.stun(Reaction.ELECTRO_STUN);
                // Arcs électriques vers les ennemis les plus proches
                float range = Reaction.ELECTRO_RANGE * d;
                ArrayList<Enemy> near = new ArrayList<>();
                for (Enemy o : enemies) {
                    if (o != e && !o.isDead() && dist2(o, e) <= range * range) {
                        near.add(o);
                    }
                }
                near.sort((a, b) -> Float.compare(dist2(a, e), dist2(b, e)));
                for (int k = 0; k < Math.min(Reaction.ELECTRO_TARGETS, near.size()); k++) {
                    Enemy o = near.get(k);
                    hurt(o, dmg);
                    o.stun(Reaction.ELECTRO_STUN / 2);
                    bolts.add(new Bolt(e.x, e.y, o.x, o.y, 3));
                }
                ringRadius = range * 0.6f;
                break;
            }
            case Reaction.LAVA:
                zones.add(new Zone(e.x, e.y, Reaction.LAVA_RADIUS * d, Zone.LAVA,
                        dmg * Reaction.LAVA_DPS_FACTOR, Reaction.LAVA_DURATION));
                ringRadius = Reaction.LAVA_RADIUS * d;
                break;
            case Reaction.STEAM: {
                float rad = Reaction.STEAM_RADIUS * d;
                zones.add(new Zone(e.x, e.y, rad, Zone.STEAM, 0, Reaction.STEAM_DURATION));
                damageAround(e.x, e.y, rad, dmg * Reaction.STEAM_DAMAGE);
                ringRadius = rad;
                break;
            }
            case Reaction.TOXIC_BLAST: {
                float rad = Reaction.TOXIC_BLAST_RADIUS * d;
                damageAround(e.x, e.y, rad, dmg * Reaction.TOXIC_BLAST_DAMAGE);
                ringRadius = rad;
                break;
            }
            case Reaction.TOXIC_CLOUD: {
                float rad = Reaction.TOXIC_CLOUD_RADIUS * d;
                float dps = Math.max(e.poisonDps, dmg);
                for (Enemy o : enemies) {
                    if (dist2(o, e) <= rad * rad) {
                        o.poison(dps, Mage.POISON_DURATION);
                    }
                }
                ringRadius = rad;
                break;
            }
            case Reaction.SANDSTORM: {
                float rad = Reaction.SANDSTORM_RADIUS * d;
                float ex = e.x;
                float ey = e.y;
                for (Enemy o : enemies) {
                    float dx = o.x - ex;
                    float dy = o.y - ey;
                    if (dx * dx + dy * dy <= rad * rad) {
                        o.knockBack(Reaction.SANDSTORM_KNOCKBACK * d);
                        o.stun(Reaction.SANDSTORM_STUN);
                    }
                }
                ringRadius = rad;
                break;
            }
            case Reaction.MUD: {          // Eau + Terre : boue qui englue
                float rad = Reaction.MUD_RADIUS * d;
                zones.add(new Zone(e.x, e.y, rad, Zone.MUD, 0, Reaction.MUD_DURATION));
                damageAround(e.x, e.y, rad, dmg * Reaction.MUD_DAMAGE);
                ringRadius = rad;
                break;
            }
            case Reaction.BOG: {          // Eau + Poison : marécage empoisonné
                float rad = Reaction.BOG_RADIUS * d;
                zones.add(new Zone(e.x, e.y, rad, Zone.BOG, Math.max(1f, dmg * Reaction.BOG_DPS_FACTOR),
                        Reaction.BOG_DURATION));
                ringRadius = rad;
                break;
            }
            case Reaction.FREEZE: {       // Eau + Air : gel de zone
                float rad = Reaction.FREEZE_RADIUS * d;
                for (Enemy o : enemiesNear(e.x, e.y, rad, null)) {
                    o.stun(Reaction.FREEZE_STUN);
                }
                ringRadius = rad;
                break;
            }
            case Reaction.GEYSER: {       // Eau + Invocateur : la cible est projetée, éclaboussures
                float rad = Reaction.GEYSER_RADIUS * d;
                damageAround(e.x, e.y, rad, dmg * Reaction.GEYSER_DAMAGE);
                splashes.add(new Splash(e.x, e.y, rad, 4));
                e.knockBack(Reaction.GEYSER_KNOCKBACK * d);
                ringRadius = rad;
                break;
            }
            case Reaction.OVERLOAD: {     // Feu + Foudre : explosion qui repousse
                float rad = Reaction.OVERLOAD_RADIUS * d;
                for (Enemy o : enemiesNear(e.x, e.y, rad, null)) {
                    hurt(o, dmg * Reaction.OVERLOAD_DAMAGE);
                    o.knockBack(Reaction.OVERLOAD_KNOCKBACK * d);
                }
                hits.add(new HitEffect(e.x, e.y, Mage.FIRE, 5));
                ringRadius = rad;
                break;
            }
            case Reaction.WILDFIRE: {     // Feu + Air : anneau de flammes
                float rad = Reaction.WILDFIRE_RADIUS * d;
                zones.add(new Zone(e.x, e.y, rad, Zone.FIRE, dmg * Reaction.WILDFIRE_DPS_FACTOR,
                        Reaction.WILDFIRE_DURATION));
                ringRadius = rad;
                break;
            }
            case Reaction.WISPS: {        // Feu + Invocateur : feux follets lancés vers les ennemis proches
                ArrayList<Enemy> near = enemiesNear(e.x, e.y, Reaction.WISPS_RANGE * d, e);
                for (int k = 0; k < Reaction.WISPS_COUNT; k++) {
                    Enemy target = near.isEmpty() ? e : near.get(k % near.size());
                    projectiles.add(new Projectile(e.x, e.y - 10 * d, target, Mage.FIRE, 2,
                            dmg * Reaction.WISPS_DAMAGE, 6, d));
                }
                break;
            }
            case Reaction.CRYSTALLIZE:    // Terre + Foudre : la cible est figée dans le cristal
                hurt(e, dmg * Reaction.CRYSTALLIZE_DAMAGE);
                e.stun(Reaction.CRYSTALLIZE_STUN);
                hits.add(new HitEffect(e.x, e.y, Mage.EARTH, 5));
                break;
            case Reaction.ACID:           // Terre + Poison : ronge l'armure
                hurt(e, dmg * Reaction.ACID_DAMAGE);
                e.makeVulnerable(Reaction.VULNERABLE_DURATION);
                break;
            case Reaction.ROCKFALL: {     // Terre + Invocateur : éboulement
                float rad = Reaction.ROCKFALL_RADIUS * d;
                for (Enemy o : enemiesNear(e.x, e.y, rad, null)) {
                    hurt(o, dmg * Reaction.ROCKFALL_DAMAGE);
                    o.stun(Reaction.ROCKFALL_STUN);
                    hits.add(new HitEffect(o.x, o.y, Mage.EARTH, 3));
                }
                ringRadius = rad;
                break;
            }
            case Reaction.PARALYSIS:      // Foudre + Poison : paralysie, le poison redouble
                e.stun(Reaction.PARALYSIS_STUN);
                if (e.isPoisoned()) {
                    e.poisonDps *= 2;
                } else {
                    e.poison(dmg, Mage.POISON_DURATION);
                }
                break;
            case Reaction.THUNDERSTORM: { // Foudre + Air : la foudre tombe sur 5 ennemis du terrain
                ArrayList<Enemy> pool = new ArrayList<>();
                for (Enemy o : enemies) {
                    if (!o.isDead() && o.y > 0) {
                        pool.add(o);
                    }
                }
                for (int k = 0; k < Reaction.THUNDERSTORM_TARGETS && !pool.isEmpty(); k++) {
                    Enemy o = pool.remove(random.nextInt(pool.size()));
                    hurt(o, dmg * Reaction.THUNDERSTORM_DAMAGE);
                    bolts.add(new Bolt(o.x + (random.nextFloat() - 0.5f) * 40 * d, 0, o.x, o.y, 4));
                    hits.add(new HitEffect(o.x, o.y, Mage.LIGHTNING, 4));
                }
                break;
            }
            case Reaction.SPECTRAL_CHAIN: { // Foudre + Invocateur : arc qui rebondit d'ennemi en ennemi
                Enemy from = e;
                ArrayList<Enemy> done = new ArrayList<>();
                done.add(e);
                for (int k = 0; k < Reaction.CHAIN_JUMPS; k++) {
                    Enemy next = null;
                    for (Enemy o : enemiesNear(from.x, from.y, Reaction.CHAIN_RANGE * d, from)) {
                        if (!done.contains(o)) {
                            next = o;
                            break;
                        }
                    }
                    if (next == null) {
                        break;
                    }
                    hurt(next, dmg * Reaction.CHAIN_DAMAGE);
                    bolts.add(new Bolt(from.x, from.y, next.x, next.y, 2));
                    done.add(next);
                    from = next;
                }
                break;
            }
            case Reaction.CURSE: {        // Poison + Invocateur : les ennemis proches deviennent vulnérables
                float rad = Reaction.CURSE_RADIUS * d;
                for (Enemy o : enemiesNear(e.x, e.y, rad, null)) {
                    o.makeVulnerable(Reaction.CURSE_DURATION);
                }
                ringRadius = rad;
                break;
            }
            case Reaction.VORTEX: {       // Air + Invocateur : aspire les ennemis vers la cible
                float rad = Reaction.VORTEX_RADIUS * d;
                for (Enemy o : enemiesNear(e.x, e.y, rad, e)) {
                    o.pullTowards(e.x, e.y, Reaction.VORTEX_PULL);
                    o.stun(Reaction.VORTEX_STUN);
                }
                e.stun(Reaction.VORTEX_STUN);
                ringRadius = rad;
                break;
            }
        }
        reactions.add(new Reaction(e.x, e.y, r, ringRadius));
        if (listener != null) {
            listener.onReaction();
        }
    }

    private void damageAround(float x, float y, float radius, float damage) {
        for (Enemy o : enemies) {
            float dx = o.x - x;
            float dy = o.y - y;
            if (dx * dx + dy * dy <= radius * radius) {
                hurt(o, damage);
            }
        }
    }

    // Dégâts subis par un ennemi : +50 % s'il est vulnérable (Acide, Malédiction)
    private void hurt(Enemy e, float damage) {
        e.hp -= e.isVulnerable() ? damage * Reaction.VULNERABLE_MULT : damage;
    }

    // Ennemis vivants à portée d'un point (hors `except`), du plus proche au plus loin
    private ArrayList<Enemy> enemiesNear(float x, float y, float radius, Enemy except) {
        ArrayList<Enemy> near = new ArrayList<>();
        for (Enemy o : enemies) {
            float dx = o.x - x;
            float dy = o.y - y;
            if (o != except && !o.isDead() && dx * dx + dy * dy <= radius * radius) {
                near.add(o);
            }
        }
        near.sort((a, b) -> Float.compare((a.x - x) * (a.x - x) + (a.y - y) * (a.y - y),
                (b.x - x) * (b.x - x) + (b.y - y) * (b.y - y)));
        return near;
    }

    private static float dist2(Enemy a, Enemy b) {
        float dx = a.x - b.x;
        float dy = a.y - b.y;
        return dx * dx + dy * dy;
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
        int previousBiome = spec.biome(Math.max(1, wave));
        wave++;
        bossPending = spec.hasBoss(wave);
        enemiesLeftToSpawn = spec.enemiesInWave(wave) + (bossPending ? 1 : 0); // 5, 7, 9... (+ le boss)
        spawnTimer = 0;
        // Mode infini : nouveau décor toutes les 10 vagues
        if (spec.endless && wave > 1 && spec.biome(wave) != previousBiome) {
            announce(LevelSpec.BIOME_NAMES[spec.biome(wave)].toUpperCase(), "Nouveau territoire", false);
        }
    }

    // Crée un ennemi du type donné en haut de l'écran (selon la difficulté de la vague)
    private Enemy createEnemy(int kind, float x) {
        float radius = 20 * board.density * Enemy.RADIUS_MULT[kind];
        float d = difficulty();
        // Vitesse ajustée à la hauteur de l'écran : même temps de traversée sur tous les appareils
        float baseSpeed = Math.min(1f + d * 0.08f, MAX_ENEMY_SPEED);
        float speed = baseSpeed * Enemy.SPEED_MULT[kind] * board.density * board.fieldScale();
        float hp = (2 + d) * Enemy.HP_MULT[kind];
        return new Enemy(x, radius, speed, hp, kind);
    }

    private void spawnEnemy(int kind) {
        float radius = 20 * board.density * Enemy.RADIUS_MULT[kind];
        // Apparition au hasard dans la largeur de la colonne de jeu
        float x = board.playLeft + radius + random.nextFloat() * (board.playWidth - 2 * radius);
        enemies.add(createEnemy(kind, x));
    }

    private void spawnBoss() {
        bossPending = false;
        enemies.add(createEnemy(Enemy.BOSS, board.centerX())); // le boss arrive au centre
        announce("UN BOSS APPROCHE !", "Le chef gobelin et ses diablotins", true);
        if (listener != null) {
            listener.onBossArrived();
        }
    }

    // Diablotins invoqués autour d'un chaman ou du boss
    private void summonImps(Enemy summoner, List<Enemy> out) {
        int count = Enemy.SUMMON_COUNT[summoner.kind];
        for (int k = 0; k < count; k++) {
            float offset = (k - (count - 1) / 2f) * summoner.radius * 1.1f;
            float minX = board.playLeft + 10 * board.density;
            float maxX = board.playRight() - 10 * board.density;
            float x = Math.max(minX, Math.min(maxX, summoner.x + offset));
            Enemy imp = createEnemy(Enemy.IMP, x);
            imp.y = summoner.y + summoner.radius * 0.5f; // juste devant l'invocateur
            out.add(imp);
        }
    }

    // ---------- Actions du joueur ----------

    // Le bouton `slot` (0 à 3) de la barre de recrutement : le mage de cet emplacement d'équipe
    public int teamTypeAt(int slot) {
        return progression.getTeamSlot(slot);
    }

    public boolean canRecruitSlot(int slot) {
        return canAffordMage() && teamTypeAt(slot) != Progression.NO_UNIT;
    }

    // Recrute le mage de l'emplacement d'équipe `slot`
    public void recruitSlot(int slot) {
        int type = teamTypeAt(slot);
        if (type != Progression.NO_UNIT) {
            recruitMage(type);
        }
    }

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
        grid[cell[0]][cell[1]] = new Mage(type, progression.getRank(type)); // rang permanent de l'élément
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