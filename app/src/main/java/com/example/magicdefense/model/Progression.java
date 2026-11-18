package com.example.magicdefense.model;

import java.util.Random;

// Progression permanente (gardée d'une partie à l'autre) :
// coffres, fragments et rang de chaque élément, gemmes, unités possédées, équipe, records du mode infini,
// ultimes, artefacts, améliorations du château, vitesse ×3, statistiques et succès.
// Le rang améliore les stats de base de TOUS les mages de cet élément, à chaque partie.
public class Progression {

    // ---------- Réglages (équilibrage) ----------
    public static final int MAX_RANK = 999;               // « presque infini »
    public static final int COST_PER_RANK = 10;           // rang n → n+1 : 10 × n fragments
    public static final float DAMAGE_BONUS_PER_RANK = 0.10f;  // +10 % de dégâts par rang
    public static final float SPEED_BONUS_PER_RANK = 0.01f;   // +1 % de cadence par rang...
    public static final float MAX_SPEED_BONUS = 0.30f;        // ...plafonné à +30 %
    public static final int LOTS_PER_CHEST = 3;           // un coffre = 3 lots de fragments
    public static final int LOT_MIN = 4;                  // chaque lot : 4 à 8 fragments
    public static final int LOT_MAX = 8;
    public static final int GEMS_MIN = 1;                 // et 1 à 3 gemmes
    public static final int GEMS_MAX = 3;
    public static final int WAVES_PER_ENDLESS_CHEST = 3;  // mode infini (Normal) : 1 coffre toutes les 3 vagues
    public static final int LEVELS_PER_BONUS_CHEST = 4;   // un coffre de plus tous les 4 niveaux
    public static final int TEAM_SIZE = 4;                // mages emmenés en partie
    public static final int NO_UNIT = -1;

    // Marché (gemmes)
    public static final int CHEST_PRICE = 10;             // un coffre
    public static final int SPEED2_PRICE = 120;           // vitesse ×2, achat définitif
    public static final int SPEED3_PRICE = 300;           // vitesse ×3, achat définitif (après ×2)
    // Prix des unités exclusives (indice = type ; 0 = possédé dès le départ)
    private static final int[] PRICES = {0, 0, 0, 0, 60, 100, 160, 0, 0, 0}; // 3 derniers : cartes (lot 3)

    // Château (gemmes) : PV max et résistance
    public static final int CASTLE_HP_MAX_LEVEL = 20;
    public static final float CASTLE_HP_PER_LEVEL = 0.10f;    // +10 % de PV max par niveau
    public static final int CASTLE_RES_MAX_LEVEL = 15;
    public static final float CASTLE_RES_PER_LEVEL = 0.03f;   // −3 % de dégâts subis par niveau

    // ---------- État ----------
    private int chests = 0;
    private int gems = 0;
    private final int[] fragments = new int[Mage.TYPE_COUNT];
    private final int[] ranks = new int[Mage.TYPE_COUNT];
    private final boolean[] owned = new boolean[Mage.TYPE_COUNT];
    private final int[] team = new int[TEAM_SIZE];
    private final int[] bestEndless = new int[LevelSpec.DIFFICULTY_COUNT]; // record par difficulté
    private final int[] lastChestReward = new int[Mage.TYPE_COUNT]; // contenu du dernier coffre ouvert
    private int lastChestGems = 0;
    private int lastChestCount = 0;  // coffres ouverts d'un coup (×1, ×5, ×10)

    private final boolean[] ultimates = new boolean[Ultimate.COUNT];
    private int equippedUltimate = Ultimate.STUN;
    private boolean speed2 = false;
    private boolean speed3 = false;
    private int castleHpLevel = 0;
    private int castleResLevel = 0;
    private final boolean[] artifacts = new boolean[Artifact.COUNT];
    private final int[] equippedArtifact = new int[Mage.TYPE_COUNT];

    // Statistiques (succès)
    private long kills = 0;
    private int merges = 0;
    private int reactions = 0;
    private int bosses = 0;
    private int skills = 0;
    private int knights = 0;
    private boolean maxLevelMage = false;
    private int chestsOpened = 0;
    private final boolean[] achievements = new boolean[Achievement.COUNT];

    public Progression() {
        for (int t = 0; t < Mage.TYPE_COUNT; t++) {
            ranks[t] = 1;
            owned[t] = !Mage.isExclusive(t);
            equippedArtifact[t] = Artifact.NONE;
        }
        for (int i = 0; i < TEAM_SIZE; i++) {
            team[i] = i; // Eau, Feu, Terre, Foudre
        }
        ultimates[Ultimate.STUN] = true;
    }

    // ---------- Lecture ----------
    public int getChests() { return chests; }
    public int getGems() { return gems; }
    public int getFragments(int type) { return fragments[type]; }
    public int getRank(int type) { return ranks[type]; }
    public boolean isOwned(int type) { return owned[type]; }
    public int getLastChestReward(int type) { return lastChestReward[type]; }
    public int getLastChestGems() { return lastChestGems; }
    public int getLastChestCount() { return lastChestCount; }
    public int getChestsOpened() { return chestsOpened; }
    public static int price(int type) { return PRICES[type]; }

    // Meilleure vague, toutes difficultés confondues
    public int getBestEndlessWave() {
        int best = 0;
        for (int b : bestEndless) {
            best = Math.max(best, b);
        }
        return best;
    }

    public int getBestEndlessWave(int difficulty) {
        return bestEndless[LevelSpec.clampDifficulty(difficulty)];
    }

    // Type de mage dans l'emplacement `slot` de l'équipe (NO_UNIT si vide)
    public int getTeamSlot(int slot) { return team[slot]; }

    public boolean isInTeam(int type) {
        for (int t : team) {
            if (t == type) {
                return true;
            }
        }
        return false;
    }

    public int teamCount() {
        int n = 0;
        for (int t : team) {
            if (t != NO_UNIT) {
                n++;
            }
        }
        return n;
    }

    public int ownedCount() {
        int n = 0;
        for (boolean o : owned) {
            if (o) {
                n++;
            }
        }
        return n;
    }

    // Fragments nécessaires pour passer au rang suivant : proportionnel au rang actuel
    public int upgradeCost(int type) {
        return COST_PER_RANK * ranks[type];
    }

    public boolean canUpgrade(int type) {
        return owned[type] && ranks[type] < MAX_RANK && fragments[type] >= upgradeCost(type);
    }

    public boolean canBuy(int type) {
        return !owned[type] && gems >= PRICES[type];
    }

    // Multiplicateur de dégâts d'un rang (rang 1 = ×1, rang 11 = ×2...)
    public static float damageMultiplier(int rank) {
        return 1f + DAMAGE_BONUS_PER_RANK * (rank - 1);
    }

    // Bonus de cadence d'un rang, entre 0 et MAX_SPEED_BONUS
    public static float speedBonus(int rank) {
        return Math.min(MAX_SPEED_BONUS, SPEED_BONUS_PER_RANK * (rank - 1));
    }

    // Coffres gagnés en terminant un niveau : 1, puis un de plus tous les 4 niveaux
    public static int chestsForLevel(int level) {
        return 1 + (level - 1) / LEVELS_PER_BONUS_CHEST;
    }

    // Coffres gagnés en mode infini (Normal) selon les vagues entièrement repoussées
    public static int chestsForEndless(int wavesCleared) {
        return chestsForEndless(wavesCleared, LevelSpec.NORMAL);
    }

    public static int chestsForEndless(int wavesCleared, int difficulty) {
        return Math.max(0, wavesCleared) / LevelSpec.wavesPerChest(difficulty);
    }

    // ---------- Ultimes ----------
    public boolean ownsUltimate(int id) { return ultimates[id]; }
    public int getEquippedUltimate() { return equippedUltimate; }

    public boolean canBuyUltimate(int id) {
        return !ultimates[id] && gems >= Ultimate.price(id);
    }

    public boolean buyUltimate(int id) {
        if (!canBuyUltimate(id)) {
            return false;
        }
        gems -= Ultimate.price(id);
        ultimates[id] = true;
        equippedUltimate = id; // le nouvel ultime est équipé tout de suite
        return true;
    }

    public boolean equipUltimate(int id) {
        if (id < 0 || id >= Ultimate.COUNT || !ultimates[id] || equippedUltimate == id) {
            return false;
        }
        equippedUltimate = id;
        return true;
    }

    // ---------- Vitesse ×2 et ×3 (achats définitifs ; ×3 demande d'avoir ×2) ----------
    public boolean hasSpeed2() { return speed2; }
    public boolean hasSpeed3() { return speed3; }

    public boolean hasSpeed(int speed) {
        return speed <= 1 || (speed == 2 && speed2) || (speed == 3 && speed3);
    }

    // Vitesse la plus rapide débloquée
    public int maxSpeed() {
        return speed3 ? 3 : speed2 ? 2 : 1;
    }

    public static int speedPrice(int speed) {
        return speed == 3 ? SPEED3_PRICE : SPEED2_PRICE;
    }

    public boolean canBuySpeed(int speed) {
        boolean prerequisite = speed == 2 || (speed == 3 && speed2);
        return prerequisite && !hasSpeed(speed) && gems >= speedPrice(speed);
    }

    public boolean buySpeed(int speed) {
        if (!canBuySpeed(speed)) {
            return false;
        }
        gems -= speedPrice(speed);
        if (speed == 2) {
            speed2 = true;
        } else {
            speed3 = true;
        }
        return true;
    }

    public boolean canBuySpeed3() {
        return canBuySpeed(3);
    }

    public boolean buySpeed3() {
        return buySpeed(3);
    }

    // ---------- Coffres achetés ----------
    public boolean canBuyChest() {
        return gems >= CHEST_PRICE;
    }

    public boolean buyChest() {
        if (!canBuyChest()) {
            return false;
        }
        gems -= CHEST_PRICE;
        chests++;
        return true;
    }

    // ---------- Château ----------
    public int getCastleHpLevel() { return castleHpLevel; }
    public int getCastleResLevel() { return castleResLevel; }

    public float castleHpMultiplier() {
        return 1f + CASTLE_HP_PER_LEVEL * castleHpLevel;
    }

    // Part des dégâts subis par le château (1 = tous)
    public float castleDamageFactor() {
        return 1f - CASTLE_RES_PER_LEVEL * castleResLevel;
    }

    public int castleHpCost() {
        return 15 * (castleHpLevel + 1);
    }

    public int castleResCost() {
        return 20 * (castleResLevel + 1);
    }

    public boolean canUpgradeCastleHp() {
        return castleHpLevel < CASTLE_HP_MAX_LEVEL && gems >= castleHpCost();
    }

    public boolean canUpgradeCastleRes() {
        return castleResLevel < CASTLE_RES_MAX_LEVEL && gems >= castleResCost();
    }

    public boolean upgradeCastleHp() {
        if (!canUpgradeCastleHp()) {
            return false;
        }
        gems -= castleHpCost();
        castleHpLevel++;
        return true;
    }

    public boolean upgradeCastleRes() {
        if (!canUpgradeCastleRes()) {
            return false;
        }
        gems -= castleResCost();
        castleResLevel++;
        return true;
    }

    // ---------- Artefacts ----------
    public boolean ownsArtifact(int id) { return artifacts[id]; }
    public int getEquippedArtifact(int type) { return equippedArtifact[type]; }

    public int artifactCount() {
        int n = 0;
        for (boolean a : artifacts) {
            if (a) {
                n++;
            }
        }
        return n;
    }

    public int artifactCount(int type) {
        int n = 0;
        for (int i = 0; i < Artifact.PER_TYPE; i++) {
            if (artifacts[Artifact.id(type, i)]) {
                n++;
            }
        }
        return n;
    }

    // Donne un artefact au hasard parmi ceux qu'on n'a pas encore (mages possédés en priorité).
    // Retourne son identifiant, ou Artifact.NONE s'ils sont tous obtenus.
    public int grantRandomArtifact(Random random) {
        int[] pool = new int[Artifact.COUNT];
        int n = 0;
        for (int id = 0; id < Artifact.COUNT; id++) {
            if (!artifacts[id] && owned[Artifact.typeOf(id)]) {
                pool[n++] = id;
            }
        }
        if (n == 0) {
            for (int id = 0; id < Artifact.COUNT; id++) {
                if (!artifacts[id]) {
                    pool[n++] = id;
                }
            }
        }
        if (n == 0) {
            return Artifact.NONE;
        }
        int id = pool[random.nextInt(n)];
        artifacts[id] = true;
        if (equippedArtifact[Artifact.typeOf(id)] == Artifact.NONE) {
            equippedArtifact[Artifact.typeOf(id)] = id; // premier artefact de ce mage : équipé tout de suite
        }
        return id;
    }

    // Change l'artefact équipé du mage : aucun → 1er possédé → 2e... → aucun
    public boolean cycleArtifact(int type) {
        if (artifactCount(type) == 0) {
            return false;
        }
        int current = equippedArtifact[type];
        int start = current == Artifact.NONE ? 0 : current - Artifact.id(type, 0) + 1;
        for (int i = start; i < Artifact.PER_TYPE; i++) {
            if (artifacts[Artifact.id(type, i)]) {
                equippedArtifact[type] = Artifact.id(type, i);
                return true;
            }
        }
        equippedArtifact[type] = Artifact.NONE;
        return true;
    }

    // ---------- Statistiques ----------
    public long getKills() { return kills; }
    public int getMerges() { return merges; }
    public int getReactions() { return reactions; }
    public int getBosses() { return bosses; }
    public int getSkills() { return skills; }
    public int getKnights() { return knights; }
    public boolean hasMaxLevelMage() { return maxLevelMage; }

    public void addKill() { kills++; }
    public void addMerge() { merges++; }
    public void addReaction() { reactions++; }
    public void addBoss() { bosses++; }
    public void addSkill() { skills++; }
    public void addKnight() { knights++; }
    public void setMaxLevelMage() { maxLevelMage = true; }

    public int maxRank() {
        int best = 1;
        for (int t = 0; t < Mage.TYPE_COUNT; t++) {
            if (owned[t]) {
                best = Math.max(best, ranks[t]);
            }
        }
        return best;
    }

    // ---------- Succès ----------
    public boolean isAchieved(int id) { return achievements[id]; }

    public int achievedCount() {
        int n = 0;
        for (boolean a : achievements) {
            if (a) {
                n++;
            }
        }
        return n;
    }

    // Marque le succès comme obtenu et donne les gemmes. Retourne false s'il l'était déjà.
    public boolean achieve(int id) {
        if (achievements[id]) {
            return false;
        }
        achievements[id] = true;
        gems += Achievement.reward(id);
        return true;
    }

    // ---------- Actions ----------
    public void addChests(int count) {
        chests += Math.max(0, count);
    }

    public void addGems(int count) {
        gems += Math.max(0, count);
    }

    // Ouvre un coffre : des fragments d'éléments possédés tirés au hasard, et des gemmes.
    // Retourne false s'il n'y a pas de coffre.
    public boolean openChest(Random random) {
        return openChests(1, random) > 0;
    }

    // Ouvre jusqu'à `count` coffres d'un coup : le butin est additionné (une carte par élément).
    // Retourne le nombre de coffres ouverts.
    public int openChests(int count, Random random) {
        int n = Math.min(count, chests);
        if (n <= 0) {
            return 0;
        }
        for (int t = 0; t < Mage.TYPE_COUNT; t++) {
            lastChestReward[t] = 0;
        }
        lastChestGems = 0;
        int[] pool = new int[Mage.TYPE_COUNT];
        int poolSize = 0;
        for (int t = 0; t < Mage.TYPE_COUNT; t++) {
            if (owned[t]) {
                pool[poolSize++] = t;
            }
        }
        for (int c = 0; c < n; c++) {
            for (int i = 0; i < LOTS_PER_CHEST; i++) {
                int type = pool[random.nextInt(poolSize)];
                int amount = LOT_MIN + random.nextInt(LOT_MAX - LOT_MIN + 1);
                fragments[type] += amount;
                lastChestReward[type] += amount;
            }
            lastChestGems += GEMS_MIN + random.nextInt(GEMS_MAX - GEMS_MIN + 1);
        }
        gems += lastChestGems;
        chests -= n;
        chestsOpened += n;
        lastChestCount = n;
        return n;
    }

    // Dépense les fragments et monte le rang. Retourne false si ce n'est pas possible.
    public boolean upgrade(int type) {
        if (!canUpgrade(type)) {
            return false;
        }
        fragments[type] -= upgradeCost(type);
        ranks[type]++;
        return true;
    }

    // Achète une unité exclusive au Marché ; elle rejoint l'équipe s'il reste une place
    public boolean buy(int type) {
        if (!canBuy(type)) {
            return false;
        }
        gems -= PRICES[type];
        owned[type] = true;
        for (int i = 0; i < TEAM_SIZE; i++) {
            if (team[i] == NO_UNIT) {
                team[i] = type;
                break;
            }
        }
        return true;
    }

    // Ajoute le mage à l'équipe (s'il reste une place) ou l'en retire (s'il n'est pas le dernier).
    // Retourne false si rien n'a changé.
    public boolean toggleTeam(int type) {
        if (!owned[type]) {
            return false;
        }
        if (isInTeam(type)) {
            if (teamCount() <= 1) {
                return false; // il faut au moins un mage
            }
            // On retire, puis on resserre l'équipe vers la gauche
            int[] packed = new int[TEAM_SIZE];
            int n = 0;
            for (int t : team) {
                if (t != NO_UNIT && t != type) {
                    packed[n++] = t;
                }
            }
            for (int i = 0; i < TEAM_SIZE; i++) {
                team[i] = i < n ? packed[i] : NO_UNIT;
            }
            return true;
        }
        for (int i = 0; i < TEAM_SIZE; i++) {
            if (team[i] == NO_UNIT) {
                team[i] = type;
                return true;
            }
        }
        return false; // équipe pleine
    }

    public boolean isTeamFull() {
        return teamCount() >= TEAM_SIZE;
    }

    // Nouveau record du mode infini (Normal) ? (retourne true si battu)
    public boolean recordEndlessWave(int wave) {
        return recordEndlessWave(wave, LevelSpec.NORMAL);
    }

    public boolean recordEndlessWave(int wave, int difficulty) {
        int d = LevelSpec.clampDifficulty(difficulty);
        if (wave > bestEndless[d]) {
            bestEndless[d] = wave;
            return true;
        }
        return false;
    }

    // ---------- Chargement de la sauvegarde ----------
    // (anciennes sauvegardes : seulement les 4 premiers éléments)
    public void load(int chests, int[] fragments, int[] ranks, int bestEndlessWave) {
        load(chests, 0, fragments, ranks, null, null, bestEndlessWave);
    }

    public void load(int chests, int gems, int[] fragments, int[] ranks, boolean[] owned, int[] team,
                     int bestEndlessWave) {
        this.chests = Math.max(0, chests);
        this.gems = Math.max(0, gems);
        for (int t = 0; t < Mage.TYPE_COUNT; t++) {
            if (t < fragments.length) {
                this.fragments[t] = Math.max(0, fragments[t]);
            }
            if (t < ranks.length) {
                this.ranks[t] = Math.max(1, Math.min(MAX_RANK, ranks[t]));
            }
            if (owned != null && t < owned.length) {
                this.owned[t] = owned[t] || !Mage.isExclusive(t);
            }
        }
        if (team != null) {
            // On ne garde que des mages possédés, sans doublon ; au moins un mage
            for (int i = 0; i < TEAM_SIZE; i++) {
                this.team[i] = NO_UNIT;
            }
            int n = 0;
            for (int i = 0; i < team.length && n < TEAM_SIZE; i++) {
                int t = team[i];
                if (t >= 0 && t < Mage.TYPE_COUNT && this.owned[t] && !isInTeam(t)) {
                    this.team[n++] = t;
                }
            }
            if (n == 0) {
                this.team[0] = Mage.WATER;
            }
        }
        this.bestEndless[LevelSpec.NORMAL] = Math.max(0, bestEndlessWave);
    }

    // Données ajoutées avec la mise à jour (les anciennes sauvegardes n'en ont pas : valeurs par défaut)
    public void loadEndlessRecords(int[] records) {
        for (int d = 0; d < LevelSpec.DIFFICULTY_COUNT && d < records.length; d++) {
            bestEndless[d] = Math.max(bestEndless[d], Math.max(0, records[d]));
        }
    }

    public void loadUltimates(boolean[] ownedUltimates, int equipped) {
        for (int i = 0; i < Ultimate.COUNT && i < ownedUltimates.length; i++) {
            ultimates[i] = ownedUltimates[i] || i == Ultimate.STUN;
        }
        equippedUltimate = equipped >= 0 && equipped < Ultimate.COUNT && ultimates[equipped] ? equipped : Ultimate.STUN;
    }

    public void loadCastle(int hpLevel, int resLevel, boolean speed2, boolean speed3) {
        castleHpLevel = Math.max(0, Math.min(CASTLE_HP_MAX_LEVEL, hpLevel));
        castleResLevel = Math.max(0, Math.min(CASTLE_RES_MAX_LEVEL, resLevel));
        this.speed2 = speed2 || speed3;
        this.speed3 = speed3;
    }

    public void loadArtifacts(boolean[] ownedArtifacts, int[] equipped) {
        for (int id = 0; id < Artifact.COUNT && id < ownedArtifacts.length; id++) {
            artifacts[id] = ownedArtifacts[id];
        }
        for (int t = 0; t < Mage.TYPE_COUNT && t < equipped.length; t++) {
            int id = equipped[t];
            equippedArtifact[t] = id >= 0 && id < Artifact.COUNT && artifacts[id] && Artifact.typeOf(id) == t
                    ? id : Artifact.NONE;
        }
    }

    public void loadStats(long kills, int merges, int reactions, int bosses, int skills, int knights,
                          boolean maxLevelMage, int chestsOpened, boolean[] done) {
        this.kills = Math.max(0, kills);
        this.merges = Math.max(0, merges);
        this.reactions = Math.max(0, reactions);
        this.bosses = Math.max(0, bosses);
        this.skills = Math.max(0, skills);
        this.knights = Math.max(0, knights);
        this.maxLevelMage = maxLevelMage;
        this.chestsOpened = Math.max(0, chestsOpened);
        for (int i = 0; i < Achievement.COUNT && i < done.length; i++) {
            achievements[i] = done[i];
        }
    }
}
