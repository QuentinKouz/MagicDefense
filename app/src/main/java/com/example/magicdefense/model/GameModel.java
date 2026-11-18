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
        void onSkill();           // compétence de mage lancée
        void onAchievement();     // succès débloqué
        void onProgressChanged(); // progression permanente modifiée : à sauvegarder
    }

    private GameListener listener;

    public void setListener(GameListener listener) {
        this.listener = listener;
    }

    // ---------- Réglages (équilibrage) ----------
    public static final int START_GOLD = 20;
    public static final int MAGE_COST = 10;
    public static final int MAGE_COST_STEP = 2;            // chaque mage choisi coûte 2 PO de plus (dans la partie)
    public static final int RANDOM_MAGE_COST = 7;          // mage au hasard (parmi l'équipe) : prix fixe
    public static final int ULTIMATE_COOLDOWN = 3600;      // ≈ 60 s entre deux ultimes, en plus de la jauge
    public static final float ARTIFACT_DROP_CHANCE = 0.10f; // niveau de boss gagné en 3 étoiles : 10 %
    public static final int GOLD_PER_KILL = 5;
    public static final int MAX_LEVEL = 5;                 // niveau de fusion maximal d'un mage
    public static final boolean SAME_TYPE_REQUIRED = true; // fusion du même élément uniquement
    public static final int ULTIMATE_KILLS_REQUIRED = 20;  // jauge de l'ultime de départ (Onde de choc)
    public static final int STUN_DURATION = Ultimate.STUN_FRAMES;
    // Château
    public static final int CASTLE_MAX_HP = 10000;         // PV de base (améliorables avec des gemmes)
    public static final float STARS_3_MIN = 0.70f; // à partir de 70 % de PV : 3 étoiles
    public static final float STARS_2_MIN = 0.40f; // à partir de 40 % de PV : 2 étoiles
    private static final int HIT_FLASH_DURATION = 20; // ≈ 0,3 s de flash rouge
    // Plafonds : au-delà du niveau 6, seuls les PV des gobelins continuent de grimper
    private static final float MAX_ENEMY_SPEED = 4.2f;   // vitesse max (≈ celle du niveau 6)
    private static final int MAX_ENEMY_DAMAGE = 5000;    // dégâts max d'un gobelin normal
    private static final float MAX_HIT_RATIO = 0.6f;     // aucun coup (même du boss) ne dépasse 60 % des PV

    // Soins achetés en partie (avec les PO) : 3 au maximum, sans dépasser les PV max
    public static final int MAX_HEALS = 3;
    private static final int[] HEAL_COSTS = {50, 75, 100};
    public static final float HEAL_RATIO = 0.25f;        // +25 % des PV max

    // Cases bloquées : la rangée du fond du damier, à débloquer avec des PO (prix croissant)
    public static final int LOCKED_ROW = Board.ROWS - 1;
    private static final int[] UNLOCK_COSTS = {30, 50, 80, 120};

    // Vitesse du jeu (×3 : achat au Marché)
    public static final int MAX_SPEED = 3;

    // Nuit : plus dur, mais un peu plus rentable
    public static final float NIGHT_SPEED_MULT = 1.3f; // ennemis 30 % plus rapides
    public static final int GOLD_PER_KILL_NIGHT = 6;   // 20 % d'or en plus
    private static final int MODE_BANNER_DURATION = 120; // bandeau affiché ≈ 2 s
    private static final int ANNOUNCE_DURATION = 150;    // bandeau boss / biome ≈ 2,5 s
    private static final int SKILL_TEXT_DURATION = 70;   // nom de la compétence lancée
    private static final int TOAST_DURATION = 200;       // bandeau « succès débloqué »
    private static final int METEOR_BLAST_DURATION = 30;

    // Carte : les niveaux (sans fin) sont affichés par pages de 6
    public static final int LEVELS_PER_PAGE = 6;

    // Accueil : pages parcourues en glissant le doigt (la Campagne au centre)
    public static final int PAGE_MARKET = 0;
    public static final int PAGE_MAGES = 1;
    public static final int PAGE_CAMPAIGN = 2;
    public static final int PAGE_ENDLESS = 3;
    public static final int PAGE_CASTLE = 4;
    public static final int PAGE_COUNT = 5;

    // Ouverture de coffre, carte par carte (façon jeu de cartes) :
    // le coffre tombe (CHEST_INTRO_FRAMES), puis chaque toucher fait sortir une carte, puis récapitulatif
    public static final int CHEST_INTRO_FRAMES = 45;
    public static final int CARD_GEMS = -2;              // « type » de la carte des gemmes
    public static final int BOSS_ARTIFACT_FALLBACK_GEMS = 20; // tous les artefacts obtenus : des gemmes

    // ---------- État ----------
    private final Board board;
    private final Random random = new Random();
    private GameState state = GameState.START;
    private int homePage = PAGE_CAMPAIGN;

    private final ArrayList<Enemy> enemies = new ArrayList<>();
    private final ArrayList<Projectile> projectiles = new ArrayList<>();
    private final ArrayList<Knight> knights = new ArrayList<>();
    private final ArrayList<Wall> walls = new ArrayList<>();          // murs des Ouvriers
    private final float[][] auras = new float[Board.ROWS][Board.COLS]; // bonus de Lumière reçu par case
    private Mage[][] grid = new Mage[Board.ROWS][Board.COLS]; // null = case vide
    private boolean[][] locked = new boolean[Board.ROWS][Board.COLS];
    private int cellsUnlocked = 0;
    private int healsUsed = 0;
    private int magesBought = 0;          // mages choisis dans la partie (le prix monte)
    private int ultimateCooldown = 0;     // > 0 : l'ultime se recharge
    private boolean waitingForWave = false; // le joueur se prépare : la vague attend son signal
    private int lowestStars = 3;          // étoiles perdues : un soin ne les rend pas

    private int gold = 0;
    private int wave = 0;
    private int enemiesLeftToSpawn = 0;
    private int spawnTimer = 0;
    private boolean bossPending = false; // le boss arrive en dernier dans cette vague
    // Ultime et ses effets
    private int ultimateCharge = 0; // ennemis tués depuis le dernier ultime
    private int stunTimer = 0;      // > 0 : les ennemis sont étourdis (Onde de choc)
    private int furyTimer = 0;      // > 0 : Rage arcanique
    private int shieldTimer = 0;    // > 0 : Bouclier divin
    private int meteorTimer = 0;    // > 0 : la météorite tombe
    private int meteorBlast = 0;    // > 0 : explosion de la météorite
    private float meteorX, meteorY;
    private boolean sensorNight = false;       // le capteur de luminosité veut la nuit
    private boolean nightMode = false;         // nuit effective (capteur, niveau de nuit, cycle du mode infini)
    private boolean vibrationEnabled = true;   // option du joueur : vibrations
    private boolean musicEnabled = true;       // option du joueur : musique de fond
    private boolean leftHanded = false;        // option du joueur : interface en miroir
    private int gameSpeed = 1;                 // ×1, ×2, ×3
    private boolean autoMode = false;          // lance seul l'ultime et les compétences
    private int castleMaxHp = CASTLE_MAX_HP;
    private int castleHp = CASTLE_MAX_HP;
    private int castleHitTimer = 0; // > 0 : le château vient d'être touché
    private int level = 1;
    private LevelSpec spec = LevelSpec.forLevel(1); // description du niveau en cours
    private int endlessDifficulty = LevelSpec.NORMAL; // difficulté choisie pour le mode infini
    private final ArrayList<Splash> splashes = new ArrayList<>();
    private final ArrayList<Zone> zones = new ArrayList<>();          // lave, vapeur
    private final ArrayList<Reaction> reactions = new ArrayList<>();  // effets visuels des réactions
    private boolean resumed = false;        // partie reprise après fermeture de l'appli

    private int unlockedLevel = 1;          // plus haut niveau jouable (sans limite)
    private int[] bestStars = new int[LEVELS_PER_PAGE + 1]; // index = numéro du niveau, agrandi au besoin
    private int selectedLevel = 1;          // niveau choisi sur la carte
    private int mapPage = 0;                // page de la carte affichée (0 = niveaux 1 à 6)

    // Progression permanente et fin de partie
    private final Progression progression = new Progression();
    private int[] lastChests = new int[Chest.COUNT]; // coffres gagnés (par sorte) à la dernière fin de partie
    private int lastBonusGems = 0;          // gemmes en plus (difficulté du mode infini)
    private int lastArtifact = Artifact.NONE; // artefact gagné contre le boss
    private int lastArtifactGems = 0;
    private boolean lastArtifactRolled = false; // la victoire donnait une chance d'artefact
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
    // Nom de la compétence lancée
    private int skillTextTimer = 0;
    private String skillText = "";
    private int skillTextType = 0;
    // Succès débloqués : affichés l'un après l'autre
    private final ArrayList<Integer> toastQueue = new ArrayList<>();
    private int toastId = -1;
    private int toastTimer = 0;
    private int uiFrame = 0;

    private float soundVolume = 1f; // position du curseur, de 0 à 1

    private GameState optionsReturnState = GameState.PAUSED; // écran à retrouver en quittant les options
    private int modeBannerTimer = 0; // > 0 : le bandeau jour/nuit est affiché

    private final ArrayList<Bolt> bolts = new ArrayList<>();
    private final ArrayList<HitEffect> hits = new ArrayList<>();


    public GameModel(Board board) {
        this.board = board;
        lockBackRow();
    }

    // ---------- Accès en lecture (pour la vue) ----------
    public GameState getState() { return state; }
    public List<Enemy> getEnemies() { return enemies; }
    public List<Projectile> getProjectiles() { return projectiles; }
    public List<Knight> getKnights() { return knights; }
    public List<Wall> getWalls() { return walls; }
    public Mage getMage(int row, int col) { return grid[row][col]; }
    public int getGold() { return gold; }
    public int getWave() { return wave; }
    public boolean canAffordMage() { return gold >= getMageCost(); }
    public int getMageCost() { return MAGE_COST + MAGE_COST_STEP * magesBought; }
    public boolean isWaitingForWave() { return waitingForWave; }
    public Progression getProgression() { return progression; }
    public boolean isEndless() { return spec.endless; }
    public LevelSpec getSpec() { return spec; }
    public int getLastChestsEarned() { return lastChests[0] + lastChests[1] + lastChests[2]; }
    public int getLastChestsEarned(int chest) { return lastChests[chest]; }
    public int getLastBonusGems() { return lastBonusGems; }
    public int getLastArtifact() { return lastArtifact; }
    public int getLastArtifactGems() { return lastArtifactGems; }
    public boolean isLastArtifactRolled() { return lastArtifactRolled; }
    public boolean isLastEndlessRecord() { return lastEndlessRecord; }
    public List<Zone> getZones() { return zones; }
    public List<Reaction> getReactions() { return reactions; }
    public boolean isResumed() { return resumed; }

    // ---------- Pages de l'accueil ----------
    public int getHomePage() { return homePage; }

    public void setHomePage(int page) {
        int p = Math.max(0, Math.min(PAGE_COUNT - 1, page));
        if (p != homePage) {
            selectedMage = -1;   // on quitte la page : la fiche et le choix d'emplacement se ferment
            swappingMage = -1;
        }
        homePage = p;
    }

    public int getEndlessDifficulty() { return endlessDifficulty; }

    public void setEndlessDifficulty(int d) {
        endlessDifficulty = LevelSpec.clampDifficulty(d);
    }

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

    public float getSkillText() { return skillTextTimer / (float) SKILL_TEXT_DURATION; }
    public String getSkillTextString() { return skillText; }
    public int getSkillTextType() { return skillTextType; }

    // Succès affiché (−1 : aucun) et avancement du bandeau (1 → 0)
    public int getToastId() { return toastId; }
    public float getToast() { return toastTimer / (float) TOAST_DURATION; }

    // ---------- Ultime ----------
    public int getUltimateId() {
        return progression.getEquippedUltimate();
    }

    public int getUltimateKillsRequired() {
        return Ultimate.killsRequired(getUltimateId());
    }

    // Remplissage de la jauge, entre 0 et 1 (pour l'affichage)
    public float getUltimateCharge() {
        return Math.min(1f, ultimateCharge / (float) getUltimateKillsRequired());
    }

    public int getUltimateKills() {
        return ultimateCharge;
    }

    // Un effet d'ultime est-il en cours ?
    public boolean isUltimateActive() {
        return stunTimer > 0 || furyTimer > 0 || shieldTimer > 0 || meteorTimer > 0;
    }

    // Prêt : jauge pleine, aucun effet en cours, et la minute de recharge est passée
    public boolean isUltimateReady() {
        return state == GameState.PLAYING
                && ultimateCharge >= getUltimateKillsRequired()
                && ultimateCooldown == 0
                && !isUltimateActive();
    }

    // Recharge de l'ultime : 1 juste après le lancement, 0 quand elle est finie
    public float getUltimateCooldown() {
        return ultimateCooldown / (float) ULTIMATE_COOLDOWN;
    }

    public int getUltimateCooldownSeconds() {
        return (ultimateCooldown + 59) / 60;
    }

    public boolean isStunned() { return stunTimer > 0; }
    public boolean isFury() { return furyTimer > 0; }
    public boolean isShielded() { return shieldTimer > 0; }
    public float getFury() { return furyTimer / (float) Ultimate.FURY_FRAMES; }
    public float getShield() { return shieldTimer / (float) Ultimate.SHIELD_FRAMES; }
    public float getStun() { return stunTimer / (float) Ultimate.STUN_FRAMES; }
    // Météorite : avancement de la chute (0 → 1), puis de l'explosion
    public boolean isMeteorFalling() { return meteorTimer > 0; }
    public float getMeteorFall() { return 1f - meteorTimer / (float) Ultimate.METEOR_FALL_FRAMES; }
    public float getMeteorBlast() { return meteorBlast / (float) METEOR_BLAST_DURATION; }
    public float getMeteorX() { return meteorX; }
    public float getMeteorY() { return meteorY; }

    private float damageMult() {
        return furyTimer > 0 ? Ultimate.FURY_DAMAGE_MULT : 1f;
    }

    // ---------- Nuit ----------
    public boolean isNightMode() {
        return nightMode;
    }

    // Le capteur de luminosité (en plus des niveaux de nuit et du cycle du mode infini)
    public void setNightMode(boolean night) {
        sensorNight = night;
        refreshNight();
    }

    private void refreshNight() {
        boolean forced = (state == GameState.PLAYING || state == GameState.PAUSED || state == GameState.OPTIONS
                || state == GameState.VICTORY || state == GameState.GAME_OVER) && spec.isNight(wave);
        boolean newMode = sensorNight || forced;
        if (newMode != nightMode && state == GameState.PLAYING) {
            modeBannerTimer = MODE_BANNER_DURATION; // le mode change pendant la partie : on prévient
        }
        nightMode = newMode;
    }

    public boolean isVibrationEnabled() {
        return vibrationEnabled;
    }

    public void setVibrationEnabled(boolean on) {
        vibrationEnabled = on;
    }

    public void toggleVibration() {
        vibrationEnabled = !vibrationEnabled;
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

    // ---------- Vitesse et mode automatique ----------
    public int getGameSpeed() {
        return gameSpeed;
    }

    public void setGameSpeed(int speed) {
        int max = progression.maxSpeed();
        gameSpeed = Math.max(1, Math.min(max, speed));
    }

    // ×1 → ×2 → ×3 (si achetée) → ×1
    public void cycleGameSpeed() {
        int max = progression.maxSpeed();
        gameSpeed = gameSpeed >= max ? 1 : gameSpeed + 1;
    }

    // Mode automatique : seulement s'il est acheté au Marché
    public boolean isAutoMode() {
        return autoMode && progression.hasAuto();
    }

    public void setAutoMode(boolean on) {
        autoMode = on;
    }

    public void toggleAutoMode() {
        if (progression.hasAuto()) {
            autoMode = !autoMode;
        }
    }

    public boolean buyAuto() {
        boolean ok = afterPurchase(inMenu() && progression.buyAuto());
        if (ok) {
            autoMode = true;
        }
        return ok;
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

    // ---------- Château ----------
    public int getCastleHp() { return castleHp; }
    public int getCastleMaxHp() { return castleMaxHp; }

    // PV restants du château, entre 0 et 1
    public float getCastleHpRatio() {
        return castleHp / (float) castleMaxHp;
    }

    // Étoiles encore possibles selon les PV restants
    // (une étoile perdue l'est pour de bon : les soins ne la rendent pas)
    public int getStars() {
        return Math.min(lowestStars, starsForRatio(getCastleHpRatio()));
    }

    private static int starsForRatio(float ratio) {
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

    // Dégâts d'un ennemi qui atteint le château : plus forts à chaque vague (plafonnés),
    // réduits par la résistance du château
    private int enemyDamage(Enemy enemy) {
        float base = Math.min(MAX_ENEMY_DAMAGE, 1000 + (difficulty() - 1) * 100);
        float dmg = base * Enemy.CASTLE_DAMAGE_MULT[enemy.kind];
        dmg = Math.min(dmg, castleMaxHp * MAX_HIT_RATIO);
        return (int) (dmg * progression.castleDamageFactor());
    }

    // Soins achetés avec les PO
    public int getHealsLeft() { return MAX_HEALS - healsUsed; }

    public int getHealCost() {
        return HEAL_COSTS[Math.min(healsUsed, HEAL_COSTS.length - 1)];
    }

    public boolean canHeal() {
        return state == GameState.PLAYING && healsUsed < MAX_HEALS && gold >= getHealCost() && castleHp < castleMaxHp;
    }

    public boolean buyHeal() {
        if (!canHeal()) {
            return false;
        }
        gold -= getHealCost();
        healsUsed++;
        castleHp = Math.min(castleMaxHp, castleHp + Math.round(castleMaxHp * HEAL_RATIO)); // jamais au-delà du max
        if (listener != null) {
            listener.onRecruit();
        }
        return true;
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
    public void loadProgress(int unlocked, int[] stars) {
        unlockedLevel = Math.max(1, unlocked);
        bestStars = new int[Math.max(unlockedLevel, LEVELS_PER_PAGE) + 1];
        for (int lv = 1; lv < stars.length && lv < bestStars.length; lv++) {
            bestStars[lv] = stars[lv];
        }
        selectedLevel = unlockedLevel;
        showPageOf(selectedLevel);
    }

    // Fin de niveau réussie : on garde le meilleur score, on débloque la suite, on donne les coffres
    // (et un artefact la première fois qu'un boss est vaincu)
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

        // Petits coffres (1, puis un de plus tous les 4 niveaux) et un grand coffre sur les niveaux de boss
        lastChests = new int[Chest.COUNT];
        lastChests[Chest.SMALL] = Progression.chestsForLevel(level);
        if (spec.hasBossLevel()) {
            lastChests[Chest.BIG] = 1;
        }
        addLastChests();
        lastArtifact = Artifact.NONE;
        lastArtifactGems = 0;
        // Niveau de boss gagné en 3 étoiles : 10 % de chances d'obtenir un artefact (à chaque fois)
        lastArtifactRolled = spec.hasBossLevel() && stars >= 3;
        if (lastArtifactRolled && random.nextFloat() < ARTIFACT_DROP_CHANCE) {
            lastArtifact = progression.grantRandomArtifact(random);
            if (lastArtifact == Artifact.NONE) {
                lastArtifactGems = BOSS_ARTIFACT_FALLBACK_GEMS;
                progression.addGems(lastArtifactGems);
            }
        }
        checkAchievements();
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
        lastChests = Progression.chestsForEndless(wavesCleared, spec.endlessDifficulty);
        addLastChests();
        lastBonusGems = getLastChestsEarned() * LevelSpec.bonusGemsPerChest(spec.endlessDifficulty);
        progression.addGems(lastBonusGems);
        lastEndlessRecord = progression.recordEndlessWave(wave, spec.endlessDifficulty);
        checkAchievements();
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
        startRun(LevelSpec.endless(endlessDifficulty));
    }

    private void lockBackRow() {
        locked = new boolean[Board.ROWS][Board.COLS];
        for (int col = 0; col < Board.COLS; col++) {
            locked[LOCKED_ROW][col] = true;
        }
        cellsUnlocked = 0;
    }

    private void startRun(LevelSpec newSpec) {
        spec = newSpec;
        enemies.clear();
        projectiles.clear();
        knights.clear();
        walls.clear();
        splashes.clear();
        bolts.clear();
        hits.clear();
        zones.clear();
        reactions.clear();
        grid = new Mage[Board.ROWS][Board.COLS];
        lockBackRow();
        healsUsed = 0;
        magesBought = 0;
        lowestStars = 3;
        gold = START_GOLD;
        wave = 0;
        ultimateCharge = 0;
        ultimateCooldown = 0;
        stunTimer = 0;
        furyTimer = 0;
        shieldTimer = 0;
        meteorTimer = 0;
        meteorBlast = 0;
        castleMaxHp = Math.round(CASTLE_MAX_HP * progression.castleHpMultiplier());
        castleHp = castleMaxHp;
        castleHitTimer = 0;
        modeBannerTimer = 0;
        announceTimer = 0;
        skillTextTimer = 0;
        lastChests = new int[Chest.COUNT];
        lastBonusGems = 0;
        lastArtifact = Artifact.NONE;
        lastArtifactGems = 0;
        endlessRewarded = false;
        lastEndlessRecord = false;
        lastArtifactRolled = false;
        resumed = false;
        gameSpeed = 1;           // chaque combat commence en ×1
        startNextWave();
        waitingForWave = true;   // la 1re vague attend que le joueur la lance
        state = GameState.PLAYING;
        nightMode = sensorNight || spec.isNight(wave); // pas de bandeau au lancement
        if (spec.nightLevel) {
            announce("NIVEAU DE NUIT", "Gobelins plus rapides, +20 % d'or", false);
        }
    }

    // Depuis l'accueil : on commence au niveau 1
    public void startNewGame() {
        startLevel(1);
    }

    // Recommencer la partie en cours (pause, game over)
    public void restartLevel() {
        if (spec.endless) {
            finishEndlessRun(); // l'abandon garde les coffres déjà mérités
            endlessDifficulty = spec.endlessDifficulty;
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

    // Bouton « Lancer la vague » : le joueur a fini de se préparer
    public boolean launchWave() {
        if (state != GameState.PLAYING || !waitingForWave) {
            return false;
        }
        waitingForWave = false;
        return true;
    }

    public void pause() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED;
        }
    }

    public void resume() {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING;
            resumed = false;
        }
    }

    // Retour à l'accueil (page Campagne ou Infini), avec le niveau en cours sélectionné
    public void quitToMenu() {
        if (spec.endless) {
            finishEndlessRun();
            homePage = PAGE_ENDLESS;
        } else {
            homePage = PAGE_CAMPAIGN;
            if (state != GameState.VICTORY) {
                selectedLevel = level;
                showPageOf(level);
            }
        }
        state = GameState.START;
        enemies.clear();
        projectiles.clear();
        knights.clear();
        walls.clear();
        if (listener != null) {
            listener.onProgressChanged(); // la partie en cours n'est plus à reprendre
        }
    }

    // ---------- Page Mages : équipe (2 × 2), collection, cartes à débloquer ----------

    // Mages possédés qui ne sont pas dans l'équipe (la collection), puis mages à débloquer,
    // chacun trié par rareté puis par élément
    public int[] collectionTypes() {
        return typesWhere(true);
    }

    public int[] lockedTypes() {
        return typesWhere(false);
    }

    private int[] typesWhere(boolean ownedList) {
        int[] out = new int[Mage.TYPE_COUNT];
        int n = 0;
        for (int r = 0; r < Rarity.COUNT; r++) {
            for (int t = 0; t < Mage.TYPE_COUNT; t++) {
                if (Rarity.of(t) != r) {
                    continue;
                }
                boolean mine = progression.isOwned(t);
                if (ownedList ? mine && !progression.isInTeam(t) : !mine) {
                    out[n++] = t;
                }
            }
        }
        return Arrays.copyOf(out, n);
    }

    // Fiche d'un mage (toucher une carte) et choix de l'emplacement d'équipe (bouton UTILISER)
    private int selectedMage = -1;
    private int swappingMage = -1;

    public int getSelectedMage() { return selectedMage; }
    public int getSwappingMage() { return swappingMage; }

    public void selectMage(int type) {
        if (inMenu() && type >= 0 && type < Mage.TYPE_COUNT) {
            selectedMage = type;
            swappingMage = -1;
        }
    }

    public void closeMageSheet() {
        selectedMage = -1;
    }

    public void cancelSwap() {
        swappingMage = -1;
    }

    // UTILISER : le mage rejoint l'équipe (place libre), sinon on choisit le mage qu'il remplace
    public boolean useMage(int type) {
        if (!inMenu() || !progression.isOwned(type) || progression.isInTeam(type)) {
            return false;
        }
        selectedMage = -1;
        for (int slot = 0; slot < Progression.TEAM_SIZE; slot++) {
            if (progression.getTeamSlot(slot) == Progression.NO_UNIT) {
                return chooseTeamSlot(slot, type);
            }
        }
        swappingMage = type; // l'équipe est pleine : toucher le mage à remplacer
        return true;
    }

    // Toucher un emplacement d'équipe pendant le choix : le mage y prend place
    public boolean chooseTeamSlot(int slot) {
        return swappingMage >= 0 && chooseTeamSlot(slot, swappingMage);
    }

    private boolean chooseTeamSlot(int slot, int type) {
        swappingMage = -1;
        if (!progression.setTeamSlot(slot, type)) {
            return false;
        }
        if (listener != null) {
            listener.onProgressChanged();
        }
        return true;
    }

    private boolean inMenu() {
        return state == GameState.START;
    }

    private void addLastChests() {
        for (int c = 0; c < Chest.COUNT; c++) {
            progression.addChests(c, lastChests[c]);
        }
    }

    // ---------- Coffres (page Marché) ----------

    // Ouvre un coffre de cette sorte (ou tous : « tout ouvrir ») et lance l'animation
    public boolean openChests(int chest, int count) {
        boolean fromAnim = state == GameState.CHEST_OPENING && isChestRevealComplete();
        if (!inMenu() && !fromAnim) {
            return false;
        }
        if (progression.openChests(chest, count, random) == 0) {
            return false;
        }
        state = GameState.CHEST_OPENING;
        homePage = PAGE_MARKET;
        chestAnimFrame = 0;
        chestCardFrame = 0;
        chestRevealed = 0;
        chestSummary = false;
        buildChestCards();
        checkAchievements();
        if (listener != null) {
            listener.onProgressChanged(); // sauvegardé tout de suite (même si l'appli est fermée pendant l'animation)
        }
        return true;
    }

    // « Ouvrir encore » à la fin de l'animation : un coffre de la même sorte
    public boolean openChest() {
        return openChests(progression.getLastChestType(), 1);
    }

    // Achat au Marché (seulement s'il n'en reste aucun de cette sorte)
    public boolean buyChests(int chest, int count) {
        return afterPurchase(inMenu() && progression.buyChests(chest, count));
    }

    // Petits coffres gratuits, une fois par jour (day = jour depuis 1970, donné par la vue)
    public boolean claimDailyChests(long day) {
        return afterPurchase(inMenu() && progression.claimDaily(day));
    }

    // Code des options (CODE_OK, CODE_WRONG, CODE_ALREADY_USED)
    public int redeemCode(String code) {
        int result = progression.redeemCode(code);
        if (result == Progression.CODE_OK && listener != null) {
            listener.onUnitBought();
            listener.onProgressChanged();
        }
        return result;
    }

    // Cartes du coffre : les gemmes d'abord, puis les mages de la rareté la plus basse à la plus haute
    // (le suspense monte jusqu'à la dernière carte)
    private void buildChestCards() {
        int n = getChestCardCount();
        chestCardTypes = new int[n];
        chestCardAmounts = new int[n];
        int k = 0;
        if (progression.getLastChestGems() > 0) {
            chestCardTypes[k] = CARD_GEMS;
            chestCardAmounts[k] = progression.getLastChestGems();
            k++;
        }
        int first = k;
        for (int t = 0; t < Mage.TYPE_COUNT; t++) {
            if (progression.getLastChestReward(t) > 0) {
                chestCardTypes[k] = t;
                chestCardAmounts[k] = progression.getLastChestReward(t);
                k++;
            }
        }
        // Tri : rareté croissante, puis quantité décroissante
        for (int i = first + 1; i < k; i++) {
            for (int j = i; j > first && before(j, j - 1); j--) {
                int ta = chestCardTypes[j];
                chestCardTypes[j] = chestCardTypes[j - 1];
                chestCardTypes[j - 1] = ta;
                int aa = chestCardAmounts[j];
                chestCardAmounts[j] = chestCardAmounts[j - 1];
                chestCardAmounts[j - 1] = aa;
            }
        }
    }

    private boolean before(int a, int b) {
        int ra = Rarity.of(chestCardTypes[a]);
        int rb = Rarity.of(chestCardTypes[b]);
        return ra < rb || (ra == rb && chestCardAmounts[a] > chestCardAmounts[b]);
    }

    // La carte i débloque un nouveau mage
    public boolean isChestCardNew(int i) {
        int t = chestCardTypes[i];
        return t >= 0 && progression.isLastChestNew(t);
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
            state = GameState.START;
            homePage = PAGE_MARKET;
        }
    }

    public boolean upgradeMage(int type) {
        if (!inMenu() || type < 0 || !progression.upgrade(type)) {
            return false;
        }
        checkAchievements();
        if (listener != null) {
            listener.onMageUpgraded();
            listener.onProgressChanged();
        }
        return true;
    }

    // Toucher le portrait d'un mage : il rejoint ou quitte l'équipe
    public boolean toggleTeam(int type) {
        if (!inMenu() || type < 0 || !progression.toggleTeam(type)) {
            return false;
        }
        if (listener != null) {
            listener.onProgressChanged();
        }
        return true;
    }

    // Change l'artefact équipé par ce mage
    public boolean cycleArtifact(int type) {
        if (!inMenu() || type < 0 || !progression.cycleArtifact(type)) {
            return false;
        }
        if (listener != null) {
            listener.onProgressChanged();
        }
        return true;
    }

    // ---------- Marché ----------
    private boolean afterPurchase(boolean ok) {
        if (ok) {
            checkAchievements();
            if (listener != null) {
                listener.onUnitBought();
                listener.onProgressChanged();
            }
        }
        return ok;
    }

    public boolean buyUltimate(int id) {
        return afterPurchase(inMenu() && progression.buyUltimate(id));
    }

    public boolean buySpeed3() {
        return buySpeed(3);
    }

    // Vitesse ×2 ou ×3 : achat définitif au Marché
    public boolean buySpeed(int speed) {
        boolean ok = afterPurchase(inMenu() && progression.buySpeed(speed));
        if (ok) {
            gameSpeed = speed; // la nouvelle vitesse est choisie tout de suite
        }
        return ok;
    }

    // ---------- Page Château ----------
    public boolean upgradeCastleHp() {
        return afterPurchase(inMenu() && progression.upgradeCastleHp());
    }

    public boolean upgradeCastleRes() {
        return afterPurchase(inMenu() && progression.upgradeCastleRes());
    }

    public boolean equipUltimate(int id) {
        if (!inMenu() || !progression.equipUltimate(id)) {
            return false;
        }
        if (listener != null) {
            listener.onProgressChanged();
        }
        return true;
    }

    // ---------- Succès ----------
    public long statValue(int stat) {
        switch (stat) {
            case Achievement.STAT_LEVEL: return unlockedLevel - 1;
            case Achievement.STAT_KILLS: return progression.getKills();
            case Achievement.STAT_MERGES: return progression.getMerges();
            case Achievement.STAT_REACTIONS: return progression.getReactions();
            case Achievement.STAT_BOSSES: return progression.getBosses();
            case Achievement.STAT_MAX_LEVEL_MAGE: return progression.hasMaxLevelMage() ? 1 : 0;
            case Achievement.STAT_ENDLESS: {
                int current = spec.endless && state != GameState.START ? wave - 1 : 0;
                return Math.max(progression.getBestEndlessWave(), current);
            }
            case Achievement.STAT_INFERNAL: {
                int current = spec.endless && spec.endlessDifficulty == LevelSpec.INFERNAL
                        && state != GameState.START ? wave - 1 : 0;
                return Math.max(progression.getBestEndlessWave(LevelSpec.INFERNAL), current);
            }
            case Achievement.STAT_OWNED: return progression.ownedCount();
            case Achievement.STAT_CHESTS: return progression.getChestsOpened();
            case Achievement.STAT_PERFECT: {
                int n = 0;
                for (int s : bestStars) {
                    if (s >= 3) {
                        n++;
                    }
                }
                return n;
            }
            case Achievement.STAT_ARTIFACTS: return progression.artifactCount();
            case Achievement.STAT_RANK: return progression.maxRank();
            case Achievement.STAT_SKILLS: return progression.getSkills();
            case Achievement.STAT_KNIGHTS: return progression.getKnights();
            default: return 0;
        }
    }

    // Débloque les succès atteints : gemmes données tout de suite, bandeau affiché
    public void checkAchievements() {
        boolean changed = false;
        for (int id = 0; id < Achievement.COUNT; id++) {
            if (!progression.isAchieved(id) && statValue(Achievement.stat(id)) >= Achievement.target(id)) {
                progression.achieve(id);
                toastQueue.add(id);
                changed = true;
            }
        }
        if (changed && listener != null) {
            listener.onAchievement();
            listener.onProgressChanged();
        }
    }

    // ---------- Animations des menus (une fois par image, quelle que soit la vitesse du jeu) ----------
    public void updateUi() {
        uiFrame++;
        if (state == GameState.CHEST_OPENING) {
            chestAnimFrame++;
            chestCardFrame++;
        }
        if (toastTimer > 0) {
            toastTimer--;
            if (toastTimer == 0) {
                toastId = -1;
            }
        } else if (!toastQueue.isEmpty()) {
            toastId = toastQueue.remove(0);
            toastTimer = TOAST_DURATION;
        }
        if (state == GameState.PLAYING && uiFrame % 30 == 0) {
            checkAchievements(); // gobelins tués, vague atteinte...
        }
    }

    // ---------- Une étape de jeu (appelée ≈ 60 fois/s, × la vitesse du jeu) ----------
    public void update() {
        if (state != GameState.PLAYING) {
            return;
        }
        refreshNight();

        if (castleHitTimer > 0) {
            castleHitTimer--;
        }
        if (modeBannerTimer > 0) {
            modeBannerTimer--;
        }
        if (announceTimer > 0) {
            announceTimer--;
        }
        if (skillTextTimer > 0) {
            skillTextTimer--;
        }
        if (furyTimer > 0) {
            furyTimer--;
        }
        if (ultimateCooldown > 0) {
            ultimateCooldown--;
        }
        if (shieldTimer > 0) {
            shieldTimer--;
        }
        if (meteorBlast > 0) {
            meteorBlast--;
        }
        if (meteorTimer > 0) {
            meteorTimer--;
            if (meteorTimer == 0) {
                meteorImpact();
            }
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
                        case Zone.CONCRETE:
                            e.slow(Reaction.CONCRETE_SLOW, 5);
                            break;
                        case Zone.TAR:
                            e.slow(Reaction.TAR_SLOW, 5);
                            e.poison(z.dps, 60);
                            break;
                        case Zone.ABYSS:
                            e.slow(Reaction.ABYSS_SLOW, 5);
                            hurt(e, z.dps / 60f);
                            break;
                        case Zone.BLACK_FIRE:
                            hurt(e, z.dps / 60f);
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
        if (enemiesLeftToSpawn > 0 && !waitingForWave) {
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

        // 2. Déplacement des ennemis (sauf pendant l'Onde de choc), combats contre les chevaliers, invocations
        for (Knight k : knights) {
            k.fighting = false;
            if (k.hitTimer > 0) {
                k.hitTimer--;
            }
        }
        if (stunTimer > 0) {
            stunTimer--;
        } else {
            ArrayList<Enemy> summoned = null;
            float speedMult = (nightMode ? NIGHT_SPEED_MULT : 1f) * spec.speedMultiplier();
            for (int i = enemies.size() - 1; i >= 0; i--) {
                Enemy enemy = enemies.get(i);

                // Un mur d'Ouvrier ou un chevalier lui barre la route : il s'arrête et frappe
                Wall wall = blockingWall(enemy);
                Knight blocker = wall == null ? blockingKnight(enemy) : null;
                if (wall != null && !enemy.isDead()) {
                    enemy.hold();
                    wall.fighting = true;
                    if (enemy.stunTimer == 0) {
                        float ratio = enemy.isBoss() ? Wall.BOSS_DPS_RATIO : Wall.GOBLIN_DPS_RATIO;
                        wall.hp -= enemy.maxHp * ratio / 60f;
                        wall.hitTimer = 6;
                    }
                } else if (blocker != null && !enemy.isDead()) {
                    enemy.hold();
                    blocker.fighting = true;
                    if (enemy.stunTimer == 0) {
                        float ratio = enemy.isBoss() ? Knight.BOSS_DPS_RATIO : Knight.GOBLIN_DPS_RATIO;
                        blocker.hp -= enemy.maxHp * ratio / 60f;
                        blocker.hitTimer = 6;
                    }
                } else {
                    enemy.update(speedMult);
                }

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
                    if (shieldTimer > 0) {
                        hits.add(new HitEffect(enemy.x, board.gridTop, Mage.AIR, 4)); // le bouclier encaisse
                        continue;
                    }
                    castleHp -= enemyDamage(enemy);
                    castleHitTimer = HIT_FLASH_DURATION;
                    lowestStars = getStars(); // l'étoile perdue ne reviendra pas

                    if (listener != null) {
                        listener.onCastleHit();     // vibration et son
                    }

                    if (castleHp <= 0) {
                        castleHp = 0;
                        state = GameState.GAME_OVER;
                        finishEndlessRun(); // mode infini : coffres selon la vague atteinte
                        if (listener != null) {
                            listener.onGameOver();
                            listener.onProgressChanged();
                        }
                        return;
                    }
                }
            }
            if (summoned != null) {
                enemies.addAll(summoned);
            }
        }

        // 3. Les chevaliers se placent et frappent ; les Ouvriers (re)construisent leurs murs
        updateKnights();
        updateWalls();

        // 4. Les mages attaquent, chacun à sa façon (renforcés par l'aura des mages de Lumière)
        computeAuras();
        float baseMult = damageMult();
        List<Enemy> targets = findTargets();
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                Mage mage = grid[row][col];
                if (mage == null) {
                    continue;
                }
                float aura = auras[row][col];
                float mult = baseMult * (1f + aura);
                if (mage.skillTimer > 0) {
                    mage.skillTimer--;
                }
                if (mage.cooldown > 0) {
                    mage.cooldown--;
                    continue;
                }
                float tipX = board.staffTipX(col);
                float tipY = board.staffTipY(row);

                if (mage.type == Mage.SUMMONER) {
                    // Invocateur : un chevalier de plus devant le château (jusqu'à sa limite)
                    if (knightsOf(mage) < mage.getMaxKnights()) {
                        spawnKnight(mage, board.cellCenterX(col), 1f);
                        mage.cooldown = fireDelay(mage, aura);
                    }
                    continue;
                }

                Enemy target = firstAlive(targets);
                if (target == null) {
                    continue;
                }

                switch (mage.type) {
                    case Mage.LIGHT:
                        // Lumière : un rayon du ciel sur CHAQUE ennemi visible, au même instant
                        for (Enemy e : targets) {
                            if (e.isDead()) {
                                continue;
                            }
                            applyHit(e, mage.getDamage() * mult, Mage.LIGHT, 0, 0, 0);
                            bolts.add(new Bolt(e.x, 0, e.x, e.y, mage.level).as(Mage.LIGHT));
                            hits.add(new HitEffect(e.x, e.y, Mage.LIGHT, mage.level));
                        }
                        break;

                    case Mage.LIGHTNING:
                        // Foudre : l'éclair relie instantanément le bâton à la cible
                        applyHit(target, mage.getDamage() * mult, Mage.LIGHTNING, 0, 0, 0);
                        bolts.add(new Bolt(tipX, tipY, target.x, target.y, mage.level));
                        hits.add(new HitEffect(target.x, target.y, Mage.LIGHTNING, mage.level));
                        break;

                    case Mage.WATER:
                        if (mage.level >= 4) {
                            // Cyclone et tsunami : partent du bâton et foncent vers l'ennemi
                            projectiles.add(new Projectile(tipX, tipY, target, mage, board.density).boost(mult));
                        } else {
                            // Goutte, bulle et averse : tombent du ciel sur l'ennemi
                            projectiles.add(new Projectile(target.x, target.y - 160 * board.density,
                                    target, mage, board.density).boost(mult));
                        }
                        break;

                    default:
                        // Feu, Terre, Poison, Air : tirés depuis l'embout du bâton (Terre : salve en éventail)
                        int shots = mage.getShots();
                        for (int k = 0; k < shots; k++) {
                            float offset = (k - (shots - 1) / 2f) * 12 * board.density;
                            projectiles.add(new Projectile(tipX + offset, tipY, target, mage, board.density)
                                    .boost(mult));
                        }
                        break;
                }
                mage.cooldown = fireDelay(mage, aura);
            }
        }

        // 5. Déplacement des projectiles et impacts
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
                    Splash splash = new Splash(p.target.x, p.target.y, p.aoeRadius, p.level); // effet visuel
                    splash.type = p.type;
                    splashes.add(splash);
                    // Eau, Ténèbres : touche tous les ennemis dans la zone autour de l'impact
                    float cx = p.target.x;
                    float cy = p.target.y;
                    for (Enemy e : enemies) {
                        float dx = e.x - cx;
                        float dy = e.y - cy;
                        if (dx * dx + dy * dy <= p.aoeRadius * p.aoeRadius) {
                            applyHit(e, p.damage, p.type, 0, 0, 0);
                            if (p.slow < 1f) {
                                e.slow(p.slow, Mage.DARK_SLOW_FRAMES); // Ténèbres : ralenti
                            }
                            if (p.executeRatio > 0) {
                                tryExecute(e, p.executeRatio);   // Ténèbres : exécution des blessés
                            }
                        }
                    }
                } else if (p.fromReaction) {
                    hurt(p.target, p.damage); // feu follet : pas de marque, pas de nouvelle réaction
                    hits.add(new HitEffect(p.target.x, p.target.y, p.type, p.level));
                } else {
                    applyHit(p.target, p.damage, p.type, p.poisonDps, p.knockback, p.stunFrames);
                    hits.add(new HitEffect(p.target.x, p.target.y, p.type, p.level)); // explosion, éclats...
                }
                projectiles.remove(i);
            }
        }

        // 6. Retrait des ennemis morts et gain de PO
        for (int i = enemies.size() - 1; i >= 0; i--) {
            Enemy dead = enemies.get(i);
            if (dead.isDead()) {
                enemies.remove(i);
                int base = nightMode ? GOLD_PER_KILL_NIGHT : GOLD_PER_KILL;
                gold += Math.max(1, Math.round(base * Enemy.GOLD_MULT[dead.kind]));
                progression.addKill();
                if (dead.isBoss()) {
                    progression.addBoss();
                }
                if (listener != null) {
                    listener.onEnemyKilled();
                }
                if (ultimateCharge < getUltimateKillsRequired()) {
                    ultimateCharge++; // chaque ennemi tué remplit la jauge
                }
            }
        }

        // 7. Mode automatique : ultime et compétences lancés dès qu'ils sont prêts (s'il y a des gobelins)
        if (isAutoMode() && hasVisibleEnemy()) {
            triggerUltimate();
            for (int row = 0; row < Board.ROWS; row++) {
                for (int col = 0; col < Board.COLS; col++) {
                    Mage m = grid[row][col];
                    if (m != null && m.isSkillReady()) {
                        castSkill(row, col);
                    }
                }
            }
        }

        // 8. Vague terminée : vague suivante, ou victoire si c'était la dernière (jamais en mode infini)
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

    private boolean hasVisibleEnemy() {
        for (Enemy e : enemies) {
            if (e.y > 0 && !e.isDead()) {
                return true;
            }
        }
        return false;
    }

    // ---------- Chevaliers ----------
    private float knightGuardY() {
        return board.gridTop - 34 * board.density;
    }

    private int knightsOf(Mage mage) {
        int n = 0;
        for (Knight k : knights) {
            if (k.owner == mage) {
                n++;
            }
        }
        return n;
    }

    // hpMult : chevaliers d'élite de la compétence « Appel aux armes »
    private void spawnKnight(Mage mage, float x, float hpMult) {
        float d = board.density;
        float radius = 14 * d;
        float minX = board.playLeft + radius;
        float maxX = board.playRight() - radius;
        float kx = Math.max(minX, Math.min(maxX, x + (random.nextFloat() - 0.5f) * 30 * d));
        Knight k = new Knight(kx, knightGuardY(), radius, mage.getKnightHp() * hpMult, mage.getKnightDamage(),
                hpMult > 1f ? null : mage);
        knights.add(k);
        progression.addKnight();
        hits.add(new HitEffect(kx, k.y, Mage.SUMMONER, 2));
    }

    private Knight blockingKnight(Enemy e) {
        for (Knight k : knights) {
            if (!k.isDead() && k.touches(e)) {
                return k;
            }
        }
        return null;
    }

    private void updateKnights() {
        float d = board.density;
        float mult = damageMult();
        for (int i = knights.size() - 1; i >= 0; i--) {
            Knight k = knights.get(i);
            if (k.isDead()) {
                hits.add(new HitEffect(k.x, k.y, Mage.SUMMONER, 3));
                knights.remove(i);
                continue;
            }
            k.age++;
            if (k.attackTimer > 0) {
                k.attackTimer--;
            }
            // Au contact : coup d'épée
            Enemy foe = null;
            for (Enemy e : enemies) {
                if (!e.isDead() && k.touches(e)) {
                    foe = e;
                    break;
                }
            }
            if (foe != null) {
                k.fighting = true;
                if (k.attackTimer == 0) {
                    applyHit(foe, k.damage * mult, Mage.SUMMONER, 0, 0, 0);
                    hits.add(new HitEffect(foe.x, foe.y, Mage.SUMMONER, 1));
                    k.attackTimer = Knight.ATTACK_DELAY;
                }
                continue;
            }
            // Sinon : il se décale pour intercepter le gobelin le plus avancé de son côté
            Enemy target = null;
            float best = Float.MAX_VALUE;
            for (Enemy e : enemies) {
                if (e.isDead() || e.y <= 0) {
                    continue;
                }
                float dist = Math.abs(e.x - k.x) + (k.y - e.y) * 0.5f;
                if (dist < best) {
                    best = dist;
                    target = e;
                }
            }
            float step = Knight.MOVE_SPEED * d;
            if (target != null) {
                float dx = target.x - k.x;
                k.x += Math.max(-step, Math.min(step, dx));
            }
            float dy = knightGuardY() - k.y;
            k.y += Math.max(-step, Math.min(step, dy));
        }
    }

    // ---------- Murs (Ouvrier) ----------
    // Ligne des murs : devant la garde des chevaliers, juste avant le damier
    private float wallY() {
        return board.gridTop - 64 * board.density;
    }

    private float wallHalfWidth() {
        return board.cellWidth * 0.45f;
    }

    private float wallHalfHeight() {
        return 9 * board.density;
    }

    private Wall wallOf(Mage mage) {
        for (Wall w : walls) {
            if (w.owner == mage) {
                return w;
            }
        }
        return null;
    }

    private boolean isOnBoard(Mage mage) {
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                if (grid[row][col] == mage) {
                    return true;
                }
            }
        }
        return false;
    }

    // Murs : usure, destruction (l'ouvrier attend avant de reconstruire), puis construction
    // du mur de chaque Ouvrier devant SA colonne (le mur suit l'ouvrier s'il change de colonne)
    private void updateWalls() {
        for (int i = walls.size() - 1; i >= 0; i--) {
            Wall w = walls.get(i);
            w.age++;
            if (w.hitTimer > 0) {
                w.hitTimer--;
            }
            if (w.life > 0) {
                w.life--;
            }
            boolean ownerGone = w.owner != null && !isOnBoard(w.owner); // fusionné ou parti
            if (w.isDead() || ownerGone) {
                if (w.owner != null && !ownerGone) {
                    w.owner.wallTimer = Mage.WALL_REBUILD;
                }
                hits.add(new HitEffect(w.x, w.y, Mage.WORKER, 4));
                walls.remove(i);
            }
            w.fighting = false;
        }
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                Mage m = grid[row][col];
                if (m == null || m.type != Mage.WORKER) {
                    continue;
                }
                Wall w = wallOf(m);
                if (w != null) {
                    w.x = board.cellCenterX(col);
                } else if (m.wallTimer > 0) {
                    m.wallTimer--;
                } else {
                    walls.add(new Wall(board.cellCenterX(col), wallY(), wallHalfWidth(), wallHalfHeight(),
                            m.getWallHp(), m, -1));
                    hits.add(new HitEffect(board.cellCenterX(col), wallY(), Mage.WORKER, 3));
                }
            }
        }
    }

    private Wall blockingWall(Enemy e) {
        for (Wall w : walls) {
            if (!w.isDead() && w.touches(e)) {
                return w;
            }
        }
        return null;
    }

    // ---------- Aura de Lumière ----------
    // Chaque mage reçoit le bonus des (au plus 2) plus fortes auras de Lumière du damier, hors la sienne
    private void computeAuras() {
        int n = 0;
        float[] all = new float[Board.ROWS * Board.COLS];
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                Mage m = grid[row][col];
                if (m != null && m.getAuraBonus() > 0) {
                    all[n++] = m.getAuraBonus();
                }
            }
        }
        Arrays.sort(all, 0, n); // croissant
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                Mage m = grid[row][col];
                float own = m == null ? 0 : m.getAuraBonus();
                float sum = 0;
                int taken = 0;
                boolean skippedOwn = own <= 0;
                for (int k = n - 1; k >= 0 && taken < Mage.MAX_LIGHT_AURAS; k--) {
                    if (!skippedOwn && all[k] == own) {
                        skippedOwn = true; // sa propre aura ne compte pas
                        continue;
                    }
                    sum += all[k];
                    taken++;
                }
                auras[row][col] = Math.min(Mage.MAX_AURA_BONUS, sum);
            }
        }
    }

    // Bonus de Lumière reçu par le mage de cette case (+x % de dégâts, −x % de délais)
    public float getAuraBonus(int row, int col) {
        computeAuras();
        return auras[row][col];
    }

    private static int fireDelay(Mage mage, float aura) {
        return Math.max(1, Math.round(mage.getFireDelay() * (1f - aura)));
    }

    // ---------- Ténèbres ----------
    // Tue net un gobelin blessé sous `ratio` de ses PV max (le boss n'est jamais exécuté)
    private boolean tryExecute(Enemy e, float ratio) {
        if (e.isBoss() || e.isDead() || e.hp > e.maxHp * ratio) {
            return false;
        }
        e.hp = 0;
        hits.add(new HitEffect(e.x, e.y, Mage.DARK, 5));
        return true;
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
            default:
                ringRadius = triggerNewReaction(r, e, dmg, ringRadius);
                break;
        }
        reactions.add(new Reaction(e.x, e.y, r, ringRadius));
        progression.addReaction();
        if (listener != null) {
            listener.onReaction();
        }
    }

    // Réactions de l'Ouvrier, de la Lumière et des Ténèbres. Retourne le rayon de l'anneau affiché.
    private float triggerNewReaction(int r, Enemy e, float dmg, float ringRadius) {
        float d = board.density;
        switch (r) {
            // ---------- Ouvrier ----------
            case Reaction.CONCRETE: {     // Eau : béton frais, les gobelins s'y engluent
                float rad = Reaction.CONCRETE_RADIUS * d;
                zones.add(new Zone(e.x, e.y, rad, Zone.CONCRETE, 0, Reaction.CONCRETE_DURATION));
                return rad;
            }
            case Reaction.FORGE:          // Feu : coup de forge, l'armure est fendue
                hurt(e, dmg * Reaction.FORGE_DAMAGE);
                e.makeVulnerable(Reaction.VULNERABLE_DURATION);
                hits.add(new HitEffect(e.x, e.y, Mage.FIRE, 5));
                return ringRadius;
            case Reaction.FOUNDATIONS: {  // Terre : onde sismique, les murs sont consolidés
                float rad = Reaction.FOUNDATIONS_RADIUS * d;
                for (Enemy o : enemiesNear(e.x, e.y, rad, null)) {
                    hurt(o, dmg);
                    o.stun(Reaction.FOUNDATIONS_STUN);
                }
                for (Wall w : walls) {
                    w.repair(w.maxHp * Reaction.FOUNDATIONS_REPAIR);
                }
                return rad;
            }
            case Reaction.PILEDRIVER:     // Foudre : trois coups de marteau électrique
                for (int k = 0; k < Reaction.PILEDRIVER_HITS; k++) {
                    hurt(e, dmg * Reaction.PILEDRIVER_DAMAGE);
                    bolts.add(new Bolt(e.x + (k - 1) * 10 * d, 0, e.x, e.y, 3));
                }
                e.stun(Reaction.PILEDRIVER_STUN);
                hits.add(new HitEffect(e.x, e.y, Mage.WORKER, 5));
                return ringRadius;
            case Reaction.TAR: {          // Poison : goudron collant et toxique
                float rad = Reaction.TAR_RADIUS * d;
                zones.add(new Zone(e.x, e.y, rad, Zone.TAR, Math.max(1f, dmg * Reaction.TAR_DPS_FACTOR),
                        Reaction.TAR_DURATION));
                return rad;
            }
            case Reaction.CATAPULT: {     // Air : la cible est catapultée et retombe sur les autres
                e.knockBack(Reaction.CATAPULT_KNOCKBACK * d);
                float rad = Reaction.CATAPULT_RADIUS * d;
                damageAround(e.x, e.y, rad, dmg * Reaction.CATAPULT_DAMAGE);
                hits.add(new HitEffect(e.x, e.y, Mage.EARTH, 4));
                return rad;
            }
            case Reaction.BARRICADE: {    // Invocateur : barricade temporaire sur la ligne des murs
                float hw = wallHalfWidth();
                float x = Math.max(board.playLeft + hw, Math.min(board.playRight() - hw, e.x));
                walls.add(new Wall(x, wallY(), hw, wallHalfHeight(), Math.max(1f, dmg * Reaction.BARRICADE_HP_FACTOR),
                        null, Reaction.BARRICADE_LIFE));
                hits.add(new HitEffect(x, wallY(), Mage.WORKER, 3));
                return ringRadius;
            }
            case Reaction.SANCTUARY:      // Lumière : murs et chevaliers soignés
                for (Wall w : walls) {
                    w.repair(w.maxHp * Reaction.SANCTUARY_HEAL);
                }
                for (Knight k : knights) {
                    k.hp = Math.min(k.maxHp, k.hp + k.maxHp * Reaction.SANCTUARY_HEAL);
                    hits.add(new HitEffect(k.x, k.y, Mage.LIGHT, 2));
                }
                return ringRadius;
            case Reaction.DEMOLITION: {   // Ténèbres : charge de démolition
                float rad = Reaction.DEMOLITION_RADIUS * d;
                for (Enemy o : enemiesNear(e.x, e.y, rad, null)) {
                    hurt(o, dmg * Reaction.DEMOLITION_DAMAGE);
                    o.knockBack(Reaction.DEMOLITION_KNOCKBACK * d);
                }
                hits.add(new HitEffect(e.x, e.y, Mage.FIRE, 5));
                return rad;
            }

            // ---------- Lumière ----------
            case Reaction.RAINBOW: {      // Eau : arc-en-ciel, ralentit et rend vulnérable
                float rad = Reaction.RAINBOW_RADIUS * d;
                for (Enemy o : enemiesNear(e.x, e.y, rad, null)) {
                    o.slow(Reaction.RAINBOW_SLOW, Reaction.RAINBOW_DURATION);
                    o.makeVulnerable(Reaction.RAINBOW_DURATION);
                }
                return rad;
            }
            case Reaction.SUPERNOVA: {    // Feu : explosion de lumière
                float rad = Reaction.SUPERNOVA_RADIUS * d;
                damageAround(e.x, e.y, rad, dmg * Reaction.SUPERNOVA_DAMAGE);
                hits.add(new HitEffect(e.x, e.y, Mage.LIGHT, 5));
                return rad;
            }
            case Reaction.PRISM: {        // Terre : le cristal réfracte la lumière vers 4 ennemis
                ArrayList<Enemy> near = enemiesNear(e.x, e.y, Reaction.PRISM_RANGE * d, e);
                for (int k = 0; k < Math.min(Reaction.PRISM_BEAMS, near.size()); k++) {
                    Enemy o = near.get(k);
                    hurt(o, dmg * Reaction.PRISM_DAMAGE);
                    bolts.add(new Bolt(e.x, e.y, o.x, o.y, 3).as(Mage.LIGHT));
                }
                return ringRadius;
            }
            case Reaction.JUDGMENT:       // Foudre : colonne de lumière foudroyante
                hurt(e, dmg * Reaction.JUDGMENT_DAMAGE);
                e.stun(Reaction.JUDGMENT_STUN);
                bolts.add(new Bolt(e.x, 0, e.x, e.y, 5).as(Mage.LIGHT));
                return ringRadius;
            case Reaction.PURIFICATION: { // Poison : tout le poison restant éclate d'un coup
                float rad = Reaction.PURIFICATION_RADIUS * d;
                for (Enemy o : enemiesNear(e.x, e.y, rad, null)) {
                    float rest = o.poisonDps * o.poisonTimer / 60f;
                    hurt(o, dmg + rest * Reaction.PURIFICATION_FACTOR);
                    o.poisonDps = 0;
                    o.poisonTimer = 0;
                }
                return rad;
            }
            case Reaction.AURORA: {       // Air : lumière aveuglante, les gobelins reculent
                float rad = Reaction.AURORA_RADIUS * d;
                for (Enemy o : enemiesNear(e.x, e.y, rad, null)) {
                    o.stun(Reaction.AURORA_STUN);
                    o.knockBack(Reaction.AURORA_KNOCKBACK * d);
                }
                return rad;
            }
            case Reaction.CRUSADE: {      // Invocateur : un paladin rejoint la garde
                float radius = 14 * d;
                float x = Math.max(board.playLeft + radius, Math.min(board.playRight() - radius, e.x));
                knights.add(new Knight(x, knightGuardY(), radius, Math.max(1f, dmg * Reaction.CRUSADE_HP_FACTOR),
                        dmg * Reaction.CRUSADE_HIT_FACTOR, null));
                progression.addKnight();
                hits.add(new HitEffect(x, knightGuardY(), Mage.LIGHT, 3));
                return ringRadius;
            }

            // ---------- Ténèbres ----------
            case Reaction.ABYSS: {        // Eau : abysse qui ralentit et ronge
                float rad = Reaction.ABYSS_RADIUS * d;
                zones.add(new Zone(e.x, e.y, rad, Zone.ABYSS, dmg * Reaction.ABYSS_DPS_FACTOR,
                        Reaction.ABYSS_DURATION));
                return rad;
            }
            case Reaction.BLACK_FLAMES: { // Feu : flammes noires
                float rad = Reaction.BLACK_FLAMES_RADIUS * d;
                zones.add(new Zone(e.x, e.y, rad, Zone.BLACK_FIRE, dmg * Reaction.BLACK_FLAMES_DPS_FACTOR,
                        Reaction.BLACK_FLAMES_DURATION));
                return rad;
            }
            case Reaction.TOMB:           // Terre : la cible est enfermée dans un tombeau
                e.stun(Reaction.TOMB_STUN);
                e.makeVulnerable(Reaction.VULNERABLE_DURATION);
                hits.add(new HitEffect(e.x, e.y, Mage.EARTH, 5));
                return ringRadius;
            case Reaction.DARK_BOLT: {    // Foudre : éclair noir qui rebondit et achève les blessés
                Enemy from = e;
                ArrayList<Enemy> done = new ArrayList<>();
                done.add(e);
                tryExecute(e, Reaction.CHAIN_EXECUTE_RATIO);
                for (int k = 0; k < Reaction.CHAIN_EXECUTE_JUMPS; k++) {
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
                    hurt(next, dmg);
                    tryExecute(next, Reaction.CHAIN_EXECUTE_RATIO);
                    bolts.add(new Bolt(from.x, from.y, next.x, next.y, 3).as(Mage.DARK));
                    done.add(next);
                    from = next;
                }
                return ringRadius;
            }
            case Reaction.NECROSIS: {     // Poison : nécrose, poison violent et chairs vulnérables
                float rad = Reaction.NECROSIS_RADIUS * d;
                for (Enemy o : enemiesNear(e.x, e.y, rad, null)) {
                    o.poison(Math.max(1f, dmg * Reaction.NECROSIS_DPS_FACTOR), Mage.POISON_DURATION);
                    o.makeVulnerable(Reaction.CURSE_DURATION);
                }
                return rad;
            }
            case Reaction.BLACK_HOLE: {   // Air : trou noir qui aspire et broie
                float rad = Reaction.BLACK_HOLE_RADIUS * d;
                for (Enemy o : enemiesNear(e.x, e.y, rad, e)) {
                    o.pullTowards(e.x, e.y, Reaction.BLACK_HOLE_PULL);
                    hurt(o, dmg * Reaction.BLACK_HOLE_DAMAGE);
                }
                hurt(e, dmg * Reaction.BLACK_HOLE_DAMAGE);
                return rad;
            }
            case Reaction.REAPING: {      // Invocateur : moisson des âmes blessées
                float rad = Reaction.REAPING_RADIUS * d;
                for (Enemy o : enemiesNear(e.x, e.y, rad, null)) {
                    tryExecute(o, Reaction.REAPING_RATIO);
                }
                return rad;
            }
            default:
                return ringRadius;
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

    // Ennemis vivants et visibles (compétences, météorite)
    private ArrayList<Enemy> visibleEnemies() {
        ArrayList<Enemy> list = new ArrayList<>();
        for (Enemy e : enemies) {
            if (!e.isDead() && e.y > 0) {
                list.add(e);
            }
        }
        return list;
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

    public boolean isLocked(int row, int col) {
        return locked[row][col];
    }

    public boolean hasFreeCell() {
        return freeCells().size() > 0;
    }

    private ArrayList<int[]> freeCells() {
        ArrayList<int[]> free = new ArrayList<>();
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                if (grid[row][col] == null && !locked[row][col]) {
                    free.add(new int[]{row, col});
                }
            }
        }
        return free;
    }

    // Prix de la prochaine case débloquée (de plus en plus cher)
    public int getUnlockCost() {
        return UNLOCK_COSTS[Math.min(cellsUnlocked, UNLOCK_COSTS.length - 1)];
    }

    public boolean canUnlockCell() {
        return gold >= getUnlockCost();
    }

    // Débloque une case bloquée en payant des PO
    public boolean unlockCell(int row, int col) {
        if (state != GameState.PLAYING || !locked[row][col] || !canUnlockCell()) {
            return false;
        }
        gold -= getUnlockCost();
        locked[row][col] = false;
        cellsUnlocked++;
        if (listener != null) {
            listener.onRecruit();
        }
        return true;
    }

    // Place un mage du type donné sur une case libre au hasard
    // (chaque mage choisi fait monter le prix du suivant de 2 PO ; le mage au hasard n'en tient pas compte)
    public void recruitMage(int type) {
        if (placeMage(type, getMageCost())) {
            magesBought++;
        }
    }

    public boolean canRecruitRandom() {
        return gold >= RANDOM_MAGE_COST && progression.teamCount() > 0;
    }

    // Mage au hasard parmi l'équipe, moins cher
    public boolean recruitRandom() {
        if (!canRecruitRandom()) {
            return false;
        }
        int[] team = new int[Progression.TEAM_SIZE];
        int n = 0;
        for (int i = 0; i < Progression.TEAM_SIZE; i++) {
            if (teamTypeAt(i) != Progression.NO_UNIT) {
                team[n++] = teamTypeAt(i);
            }
        }
        return placeMage(team[random.nextInt(n)], RANDOM_MAGE_COST);
    }

    private boolean placeMage(int type, int cost) {
        if (gold < cost) {
            return false;
        }
        ArrayList<int[]> free = freeCells();
        if (free.isEmpty()) {
            return false; // damier plein
        }
        int[] cell = free.get(random.nextInt(free.size()));
        // rang permanent de l'élément et artefact équipé
        grid[cell[0]][cell[1]] = new Mage(type, progression.getRank(type), progression.getEquippedArtifact(type));
        gold -= cost;
        if (listener != null) {
            listener.onRecruit();
        }
        return true;
    }

    // Lance l'ultime équipé si la jauge est pleine. Retourne true s'il a été lancé.
    public boolean triggerUltimate() {
        if (!isUltimateReady()) {
            return false; // secousse ignorée : jauge pas pleine, pause, accueil...
        }
        switch (getUltimateId()) {
            case Ultimate.FURY:
                furyTimer = Ultimate.FURY_FRAMES;
                break;
            case Ultimate.METEOR:
                meteorTimer = Ultimate.METEOR_FALL_FRAMES;
                meteorX = board.centerX();
                meteorY = board.gridTop * 0.45f;
                break;
            case Ultimate.SHIELD:
                shieldTimer = Ultimate.SHIELD_FRAMES;
                break;
            default:
                stunTimer = Ultimate.STUN_FRAMES;
                break;
        }
        ultimateCharge = 0;
        ultimateCooldown = ULTIMATE_COOLDOWN; // une minute avant le prochain
        if (listener != null) {
            listener.onUltimateTriggered();
        }
        return true;
    }

    // La météorite s'écrase : énormes dégâts à tous les gobelins du terrain
    private void meteorImpact() {
        meteorBlast = METEOR_BLAST_DURATION;
        for (Enemy e : visibleEnemies()) {
            float ratio = e.isBoss() ? Ultimate.METEOR_BOSS_DAMAGE : Ultimate.METEOR_DAMAGE;
            hurt(e, e.maxHp * ratio);
            e.stun(30);
            hits.add(new HitEffect(e.x, e.y, Mage.FIRE, 5));
        }
        castleHitTimer = HIT_FLASH_DURATION / 2;
        if (listener != null) {
            listener.onCastleHit(); // grosse vibration
        }
    }

    // Toucher un mage de niveau 5 : il lance sa compétence (si elle est rechargée)
    public boolean castSkill(int row, int col) {
        Mage m = grid[row][col];
        if (state != GameState.PLAYING || m == null || !m.isSkillReady()) {
            return false;
        }
        float d = board.density;
        float dmg = m.getDamage() * damageMult();
        ArrayList<Enemy> field = visibleEnemies();
        if (field.isEmpty() && m.type != Mage.SUMMONER && m.type != Mage.WORKER) {
            return false; // pas de cible : on garde la compétence
        }
        switch (m.type) {
            case Mage.WATER:     // Raz-de-marée
                for (Enemy e : field) {
                    hurt(e, dmg * 3);
                    e.knockBack(90 * d);
                    e.slow(0.4f, 180);
                    splashes.add(new Splash(e.x, e.y, 60 * d, 5));
                }
                break;
            case Mage.FIRE:      // Pluie de feu
                for (int k = 0; k < 8; k++) {
                    Enemy t = field.get(k % field.size());
                    projectiles.add(new Projectile(t.x + (random.nextFloat() - 0.5f) * 60 * d, 0, t, Mage.FIRE, 5,
                            dmg * 4, 10, d));
                }
                break;
            case Mage.EARTH:     // Séisme
                for (Enemy e : field) {
                    hurt(e, dmg * 3);
                    e.stun(90);
                    hits.add(new HitEffect(e.x, e.y, Mage.EARTH, 5));
                }
                castleHitTimer = 6;
                break;
            case Mage.LIGHTNING: // Tempête
                for (int k = 0; k < 10; k++) {
                    Enemy t = field.get(random.nextInt(field.size()));
                    hurt(t, dmg * 3);
                    bolts.add(new Bolt(t.x + (random.nextFloat() - 0.5f) * 40 * d, 0, t.x, t.y, 5));
                    hits.add(new HitEffect(t.x, t.y, Mage.LIGHTNING, 5));
                }
                break;
            case Mage.POISON:    // Peste
                for (Enemy e : field) {
                    e.poison(Math.max(m.getPoisonDps(), dmg) * 3, 300);
                    hits.add(new HitEffect(e.x, e.y, Mage.POISON, 4));
                }
                break;
            case Mage.AIR:       // Tornade
                for (Enemy e : field) {
                    e.knockBack(160 * d);
                    hits.add(new HitEffect(e.x, e.y, Mage.AIR, 5));
                }
                break;
            case Mage.SUMMONER:  // Appel aux armes : 3 chevaliers d'élite (hors limite)
                for (int k = 0; k < 3; k++) {
                    spawnKnight(m, board.playLeft + board.playWidth * (k + 1) / 4f, 2f);
                }
                break;
            case Mage.WORKER:    // Grand rempart : un mur renforcé (temporaire) devant chaque colonne
                for (int c = 0; c < Board.COLS; c++) {
                    walls.add(new Wall(board.cellCenterX(c), wallY(), wallHalfWidth(), wallHalfHeight(),
                            m.getWallHp() * 2, null, 900));
                    hits.add(new HitEffect(board.cellCenterX(c), wallY(), Mage.WORKER, 5));
                }
                break;
            case Mage.LIGHT:     // Aube sacrée : brûle et aveugle tout le terrain, recharge les autres compétences
                for (Enemy e : field) {
                    hurt(e, dmg * 4);
                    e.stun(60);
                    bolts.add(new Bolt(e.x, 0, e.x, e.y, 5).as(Mage.LIGHT));
                }
                for (int r = 0; r < Board.ROWS; r++) {
                    for (int c = 0; c < Board.COLS; c++) {
                        Mage o = grid[r][c];
                        if (o != null && o != m) {
                            o.skillTimer /= 2;
                        }
                    }
                }
                break;
            case Mage.DARK:      // Éclipse : exécute les blessés (< 25 %), ralentit et blesse les autres
                for (Enemy e : field) {
                    if (!tryExecute(e, 0.25f)) {
                        hurt(e, dmg * 2);
                        e.slow(0.4f, 240);
                    }
                    hits.add(new HitEffect(e.x, e.y, Mage.DARK, 4));
                }
                break;
        }
        // Recharge raccourcie par l'aura des mages de Lumière
        m.skillTimer = Math.max(1, Math.round(m.getSkillCooldown() * (1f - getAuraBonus(row, col))));
        progression.addSkill();
        skillText = Mage.SKILL_NAMES[m.type].toUpperCase();
        skillTextType = m.type;
        skillTextTimer = SKILL_TEXT_DURATION;
        if (listener != null) {
            listener.onSkill();
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
        if (moved == null || moved == target || locked[toRow][toCol]) {
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
            progression.addMerge();
            if (target.level >= MAX_LEVEL) {
                progression.setMaxLevelMage();
            }
            if (listener != null) {
                listener.onMerge();
            }
        }
    }

    // ---------- Partie à reprendre (sauvegarde quand l'appli est fermée en pleine partie) ----------
    private static final int RUN_VERSION = 2;          // 1 : avant le prix croissant et les étoiles perdues
    private static final int RUN_HEADER = 16;
    private static final int RUN_HEADER_V1 = 12;

    // Une partie est en cours (en jeu, en pause, ou dans les options ouvertes depuis la pause)
    public boolean hasRunInProgress() {
        return state == GameState.PLAYING || state == GameState.PAUSED
                || (state == GameState.OPTIONS && optionsReturnState == GameState.PAUSED);
    }

    // La partie en cours, sous forme de nombres : elle reprendra au début de la vague en cours
    public int[] exportRun() {
        int[] data = new int[RUN_HEADER + Board.ROWS * Board.COLS * 3];
        data[0] = RUN_VERSION;
        data[1] = spec.endless ? 1 : 0;
        data[2] = level;
        data[3] = spec.endlessDifficulty;
        data[4] = wave;
        data[5] = gold;
        data[6] = castleHp;
        data[7] = ultimateCharge;
        data[8] = healsUsed;
        data[9] = cellsUnlocked;
        data[10] = castleMaxHp;
        data[11] = 0;
        data[12] = magesBought;
        data[13] = lowestStars;
        data[14] = ultimateCooldown;
        data[15] = 0;
        int i = RUN_HEADER;
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                Mage m = grid[row][col];
                data[i++] = m == null ? -1 : m.type;
                data[i++] = m == null ? 0 : m.level;
                data[i++] = locked[row][col] ? 1 : 0;
            }
        }
        return data;
    }

    // Reprend une partie sauvegardée : elle s'ouvre en pause, au début de la vague où l'on était
    public boolean importRun(int[] data) {
        if (data == null || data.length < 1 || (data[0] != RUN_VERSION && data[0] != 1)) {
            return false;
        }
        int header = data[0] == 1 ? RUN_HEADER_V1 : RUN_HEADER; // partie sauvegardée par l'ancienne version
        if (data.length < header + Board.ROWS * Board.COLS * 3) {
            return false;
        }
        boolean endless = data[1] == 1;
        if (endless) {
            endlessDifficulty = LevelSpec.clampDifficulty(data[3]);
            level = 1;
            startRun(LevelSpec.endless(endlessDifficulty));
        } else {
            level = Math.max(1, data[2]);
            startRun(LevelSpec.forLevel(level));
        }
        announceTimer = 0;
        int savedWave = Math.max(1, data[4]);
        if (!endless) {
            savedWave = Math.min(savedWave, spec.waves);
        }
        wave = savedWave - 1;
        startNextWave();
        announceTimer = 0;
        gold = Math.max(0, data[5]);
        castleMaxHp = Math.max(1, data[10]);
        castleHp = Math.max(1, Math.min(castleMaxHp, data[6]));
        ultimateCharge = Math.max(0, data[7]);
        healsUsed = Math.max(0, Math.min(MAX_HEALS, data[8]));
        cellsUnlocked = Math.max(0, data[9]);
        if (header == RUN_HEADER) {
            magesBought = Math.max(0, data[12]);
            lowestStars = Math.max(1, Math.min(3, data[13]));
            ultimateCooldown = Math.max(0, Math.min(ULTIMATE_COOLDOWN, data[14]));
        } else {
            lowestStars = starsForRatio(castleHp / (float) castleMaxHp);
        }
        int i = header;
        for (int row = 0; row < Board.ROWS; row++) {
            for (int col = 0; col < Board.COLS; col++) {
                int type = data[i++];
                int lv = data[i++];
                locked[row][col] = data[i++] == 1;
                if (type >= 0 && type < Mage.TYPE_COUNT) {
                    Mage m = new Mage(type, progression.getRank(type), progression.getEquippedArtifact(type));
                    m.level = Math.max(1, Math.min(MAX_LEVEL, lv));
                    grid[row][col] = m;
                    locked[row][col] = false;
                }
            }
        }
        nightMode = sensorNight || spec.isNight(wave);
        state = GameState.PAUSED;
        resumed = true;
        return true;
    }
}
