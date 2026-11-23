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
    public static final int DAILY_FREE_CHESTS = 2;        // petits coffres gratuits chaque jour
    public static final String GIFT_CODE = "thomasleboss"; // code des options : une seule fois par sauvegarde
    public static final int GIFT_CODE_GEMS = 9999;
    // Codes cadeaux des options : chacun une seule fois par sauvegarde (le 1er est l'ancien code)
    public static final String[] GIFT_CODES = {GIFT_CODE, "thomasleboss2", "jeveuxlesgrossesboulesdesagesse"};
    private static final int[] GIFT_CODE_GEM_REWARDS = {GIFT_CODE_GEMS, 99999, 0};
    private static final int[] GIFT_CODE_ORB_REWARDS = {0, 0, 4};
    public static final int CODE_OK = 1;
    public static final int CODE_WRONG = 0;
    public static final int CODE_ALREADY_USED = -1;
    public static final int LEVELS_PER_BONUS_CHEST = 4;   // un coffre de plus tous les 4 niveaux
    public static final int TEAM_SIZE = 4;                // mages emmenés en partie
    public static final int NO_UNIT = -1;

    // Marché (gemmes)
    public static final int SPEED2_PRICE = 250;           // vitesse ×2, achat définitif
    public static final int AUTO_PRICE = 400;             // mode automatique, achat définitif
    public static final int SPEED3_PRICE = 600;           // vitesse ×3, achat définitif (après ×2)

    // Château (gemmes) : PV max et résistance
    public static final int CASTLE_HP_MAX_LEVEL = 20;
    public static final float CASTLE_HP_PER_LEVEL = 0.10f;    // +10 % de PV max par niveau
    public static final int CASTLE_RES_MAX_LEVEL = 15;
    public static final float CASTLE_RES_PER_LEVEL = 0.03f;   // −3 % de dégâts subis par niveau
    // Trésor de départ : +5 PO au début de chaque partie (campagne et infini) par niveau
    public static final int CASTLE_GOLD_MAX_LEVEL = 10;
    public static final int CASTLE_GOLD_PER_LEVEL = 5;

    // Orbe de sagesse : une par difficulté du mode infini, en repoussant la vague 1000 (une seule fois)
    public static final int WISDOM_ORB_WAVE = 1000;

    // ---------- État ----------
    private final int[] chests = new int[Chest.COUNT]; // coffres en réserve, par sorte
    private int gems = 0;
    private final int[] fragments = new int[Mage.TYPE_COUNT];
    private final int[] ranks = new int[Mage.TYPE_COUNT];
    private final boolean[] owned = new boolean[Mage.TYPE_COUNT];
    private final int[] team = new int[TEAM_SIZE];
    private final int[] bestEndless = new int[LevelSpec.DIFFICULTY_COUNT]; // record par difficulté
    private final int[] lastChestReward = new int[Mage.TYPE_COUNT]; // contenu du dernier coffre ouvert
    private int lastChestGems = 0;
    private int lastChestCount = 0;  // coffres ouverts d'un coup (« tout ouvrir »)
    private int lastChestType = Chest.SMALL;
    private final boolean[] lastChestNew = new boolean[Mage.TYPE_COUNT]; // mage débloqué par ce coffre
    private long lastDailyDay = -1;  // jour (depuis 1970) des derniers coffres gratuits
    private final boolean[] codesUsed = new boolean[GIFT_CODES.length]; // [0] = ancien « codeUsed »
    private int wisdomOrbs = 0;      // orbes de sagesse

    private final boolean[] ultimates = new boolean[Ultimate.COUNT];
    private int equippedUltimate = Ultimate.STUN;
    private boolean speed2 = false;
    private boolean speed3 = false;
    private boolean auto = false;          // mode automatique acheté
    private int castleHpLevel = 0;
    private int castleResLevel = 0;
    private int castleGoldLevel = 0;  // trésor de départ
    private final boolean[] artifacts = new boolean[Artifact.COUNT];
    private final int[] equippedArtifact = new int[Mage.TYPE_COUNT];
    private final boolean[] wisdomOrbOn = new boolean[Mage.TYPE_COUNT]; // orbe posée sur ce type de mage
    private final boolean[] wisdomOrbEarned = new boolean[LevelSpec.DIFFICULTY_COUNT]; // vague 1000 réussie

    // Statistiques (succès)
    private long kills = 0;
    private int merges = 0;
    private int reactions = 0;
    private int bosses = 0;
    private int skills = 0;
    private int knights = 0;
    private boolean maxLevelMage = false;
    private int chestsOpened = 0;
    private final int[] achievementTiers = new int[Achievement.COUNT]; // paliers obtenus par famille

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
    public int getChests() { return chests[Chest.SMALL] + chests[Chest.BIG] + chests[Chest.MYSTERY]; }
    public int getChests(int chest) { return chests[chest]; }
    public int getLastChestType() { return lastChestType; }
    public boolean isLastChestNew(int type) { return lastChestNew[type]; }
    public boolean isCodeUsed() { return codesUsed[0]; }
    public boolean isCodeUsed(int index) { return codesUsed[index]; }
    public int getWisdomOrbs() { return wisdomOrbs; }

    public void addWisdomOrbs(int count) {
        wisdomOrbs += Math.max(0, count);
    }

    public void loadWisdomOrbs(int count) {
        wisdomOrbs = Math.max(0, count);
    }
    public long getLastDailyDay() { return lastDailyDay; }
    public int getGems() { return gems; }
    public int getFragments(int type) { return fragments[type]; }
    public int getRank(int type) { return ranks[type]; }
    public boolean isOwned(int type) { return owned[type]; }
    public int getLastChestReward(int type) { return lastChestReward[type]; }
    public int getLastChestGems() { return lastChestGems; }
    public int getLastChestCount() { return lastChestCount; }
    public int getChestsOpened() { return chestsOpened; }

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

    // Cartes nécessaires pour passer au rang suivant : proportionnel au rang, moins pour les cartes rares
    public int upgradeCost(int type) {
        return Rarity.upgradeCards(Rarity.of(type)) * ranks[type];
    }

    // Gemmes à payer en plus des cartes
    public int upgradeGemCost(int type) {
        return Rarity.upgradeGems(Rarity.of(type)) * ranks[type];
    }

    public boolean hasCardsToUpgrade(int type) {
        return owned[type] && ranks[type] < MAX_RANK && fragments[type] >= upgradeCost(type);
    }

    public boolean canUpgrade(int type) {
        return hasCardsToUpgrade(type) && gems >= upgradeGemCost(type);
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

    // Coffre gagné en repoussant la vague `wave` du mode infini (-1 : aucun)
    //  - Facile    : petit coffre aux vagues 5, 15, 25... ; grand coffre aux vagues 10, 20, 30...
    //  - Normal    : un coffre toutes les 3 vagues, par cycles de 15 coffres :
    //                petits, sauf les 4e, 8e et 12e (grands) et le 15e (mystérieux)
    //  - Difficile : un coffre toutes les 2 vagues, par cycles de 8 coffres :
    //                petits, sauf le 4e (grand) et le 8e (mystérieux)
    //  - Infernal  : un grand coffre toutes les 3 vagues, par cycles de 6 : le 6e est mystérieux
    public static int chestForEndlessWave(int wave, int difficulty) {
        if (wave <= 0) {
            return -1;
        }
        switch (LevelSpec.clampDifficulty(difficulty)) {
            case LevelSpec.EASY:
                return wave % 10 == 0 ? Chest.BIG : wave % 10 == 5 ? Chest.SMALL : -1;
            case LevelSpec.HARD: {
                if (wave % 2 != 0) {
                    return -1;
                }
                int pos = (wave / 2 - 1) % 8 + 1;
                return pos == 8 ? Chest.MYSTERY : pos == 4 ? Chest.BIG : Chest.SMALL;
            }
            case LevelSpec.INFERNAL: {
                if (wave % 3 != 0) {
                    return -1;
                }
                int pos = (wave / 3 - 1) % 6 + 1;
                return pos == 6 ? Chest.MYSTERY : Chest.BIG;
            }
            default: { // Normal
                if (wave % 3 != 0) {
                    return -1;
                }
                int pos = (wave / 3 - 1) % 15 + 1;
                return pos == 15 ? Chest.MYSTERY : pos % 4 == 0 ? Chest.BIG : Chest.SMALL;
            }
        }
    }

    // Coffres gagnés (par sorte) en repoussant les vagues 1 à wavesCleared
    public static int[] chestsForEndless(int wavesCleared, int difficulty) {
        int[] out = new int[Chest.COUNT];
        for (int w = 1; w <= wavesCleared; w++) {
            int c = chestForEndlessWave(w, difficulty);
            if (c >= 0) {
                out[c]++;
            }
        }
        return out;
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

    // ---------- Mode automatique (achat définitif) ----------
    public boolean hasAuto() { return auto; }

    public boolean canBuyAuto() {
        return !auto && gems >= AUTO_PRICE;
    }

    public boolean buyAuto() {
        if (!canBuyAuto()) {
            return false;
        }
        gems -= AUTO_PRICE;
        auto = true;
        return true;
    }

    // Coffres par sorte (absents des anciennes sauvegardes : on garde alors les anciens coffres en petits),
    // coffres gratuits du jour, code des options
    public void loadChests(int small, int big, int mystery, long dailyDay, boolean codeUsed) {
        if (small >= 0) {
            chests[Chest.SMALL] = small;
        }
        chests[Chest.BIG] = Math.max(0, big);
        chests[Chest.MYSTERY] = Math.max(0, mystery);
        lastDailyDay = dailyDay;
        this.codesUsed[0] = codeUsed;
    }

    // Codes cadeaux déjà utilisés (indice = GIFT_CODES ; l'indice 0 vient de loadChests)
    public void loadCodesUsed(boolean[] used) {
        for (int i = 1; i < GIFT_CODES.length && i < used.length; i++) {
            codesUsed[i] = used[i];
        }
    }

    public void loadAuto(boolean owned) {
        auto = owned;
    }

    // ---------- Coffres du Marché ----------
    // On ne peut acheter une sorte de coffre que si l'on n'en a plus aucun en réserve (×1, ×5 ou ×10)
    public boolean canBuyChests(int chest, int count) {
        return count > 0 && chests[chest] == 0 && gems >= Chest.price(chest) * count;
    }

    public boolean buyChests(int chest, int count) {
        if (!canBuyChests(chest, count)) {
            return false;
        }
        gems -= Chest.price(chest) * count;
        chests[chest] += count;
        return true;
    }

    public void addChests(int chest, int count) {
        chests[chest] += Math.max(0, count);
    }

    // ---------- Coffres gratuits du jour ----------
    public boolean canClaimDaily(long day) {
        return day != lastDailyDay;
    }

    public boolean claimDaily(long day) {
        if (!canClaimDaily(day)) {
            return false;
        }
        lastDailyDay = day;
        chests[Chest.SMALL] += DAILY_FREE_CHESTS;
        return true;
    }

    // ---------- Code des options ----------
    // Indice du code dans GIFT_CODES (sans tenir compte des majuscules ni des espaces autour), ou -1
    public static int giftCodeIndex(String code) {
        if (code == null) {
            return -1;
        }
        for (int i = 0; i < GIFT_CODES.length; i++) {
            if (code.trim().equalsIgnoreCase(GIFT_CODES[i])) {
                return i;
            }
        }
        return -1;
    }

    // Récompense d'un code, en toutes lettres (« +99999 gemmes », « +4 orbes de sagesse »)
    public static String giftCodeReward(String code) {
        int i = giftCodeIndex(code);
        if (i < 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (GIFT_CODE_GEM_REWARDS[i] > 0) {
            sb.append("+").append(GIFT_CODE_GEM_REWARDS[i]).append(" gemmes");
        }
        if (GIFT_CODE_ORB_REWARDS[i] > 0) {
            if (sb.length() > 0) {
                sb.append(" et ");
            }
            sb.append("+").append(GIFT_CODE_ORB_REWARDS[i])
                    .append(GIFT_CODE_ORB_REWARDS[i] > 1 ? " orbes de sagesse" : " orbe de sagesse");
        }
        return sb.toString();
    }

    public int redeemCode(String code) {
        int i = giftCodeIndex(code);
        if (i < 0) {
            return CODE_WRONG;
        }
        if (codesUsed[i]) {
            return CODE_ALREADY_USED;
        }
        codesUsed[i] = true;
        gems += GIFT_CODE_GEM_REWARDS[i];
        wisdomOrbs += GIFT_CODE_ORB_REWARDS[i];
        return CODE_OK;
    }

    // ---------- Château ----------
    public int getCastleHpLevel() { return castleHpLevel; }
    public int getCastleResLevel() { return castleResLevel; }
    public int getCastleGoldLevel() { return castleGoldLevel; }

    // Trésor de départ : PO en plus au début de chaque partie
    public int startGoldBonus() {
        return CASTLE_GOLD_PER_LEVEL * castleGoldLevel;
    }

    public int castleGoldCost() {
        return 35 * (castleGoldLevel + 1);
    }

    public boolean canUpgradeCastleGold() {
        return castleGoldLevel < CASTLE_GOLD_MAX_LEVEL && gems >= castleGoldCost();
    }

    public boolean upgradeCastleGold() {
        if (!canUpgradeCastleGold()) {
            return false;
        }
        gems -= castleGoldCost();
        castleGoldLevel++;
        return true;
    }

    public float castleHpMultiplier() {
        return 1f + CASTLE_HP_PER_LEVEL * castleHpLevel;
    }

    // Part des dégâts subis par le château (1 = tous)
    public float castleDamageFactor() {
        return 1f - CASTLE_RES_PER_LEVEL * castleResLevel;
    }

    public int castleHpCost() {
        return 25 * (castleHpLevel + 1);
    }

    public int castleResCost() {
        return 30 * (castleResLevel + 1);
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

    // ---------- Orbes de sagesse (niveau 7 : sage) ----------
    // (réserve : wisdomOrbs, getWisdomOrbs, addWisdomOrbs, loadWisdomOrbs plus haut)

    // Une orbe est-elle posée sur ce type de mage ? (ses mages peuvent alors atteindre le niveau 7)
    public boolean hasWisdomOrb(int type) {
        return type >= 0 && type < Mage.TYPE_COUNT && wisdomOrbOn[type];
    }

    // Nombre d'orbes posées sur des mages
    public int placedWisdomOrbs() {
        int n = 0;
        for (boolean on : wisdomOrbOn) {
            if (on) {
                n++;
            }
        }
        return n;
    }

    public boolean canPlaceWisdomOrb(int type) {
        return type >= 0 && type < Mage.TYPE_COUNT && owned[type] && !wisdomOrbOn[type] && wisdomOrbs > 0;
    }

    // Pose une orbe de la réserve sur ce type de mage (de façon permanente)
    public boolean placeWisdomOrb(int type) {
        if (!canPlaceWisdomOrb(type)) {
            return false;
        }
        wisdomOrbs--;
        wisdomOrbOn[type] = true;
        return true;
    }

    // Retire l'orbe de ce type de mage : elle revient dans la réserve
    public boolean removeWisdomOrb(int type) {
        if (!hasWisdomOrb(type)) {
            return false;
        }
        wisdomOrbOn[type] = false;
        wisdomOrbs++;
        return true;
    }

    public boolean isWisdomOrbEarned(int difficulty) {
        return wisdomOrbEarned[LevelSpec.clampDifficulty(difficulty)];
    }

    // Vague `wave` repoussée en mode infini : à la vague 1000, une orbe (une seule fois par difficulté).
    // Retourne true si une orbe vient d'être gagnée.
    public boolean claimWisdomOrb(int wave, int difficulty) {
        int d = LevelSpec.clampDifficulty(difficulty);
        if (wave < WISDOM_ORB_WAVE || wisdomOrbEarned[d]) {
            return false;
        }
        wisdomOrbEarned[d] = true;
        addWisdomOrbs(1);
        return true;
    }

    // Chargement : orbes posées et difficultés déjà récompensées (la réserve : loadWisdomOrbs(int))
    public void loadWisdomOrbs(boolean[] placed, boolean[] earned) {
        for (int t = 0; t < Mage.TYPE_COUNT && t < placed.length; t++) {
            wisdomOrbOn[t] = placed[t];
        }
        for (int d = 0; d < LevelSpec.DIFFICULTY_COUNT && d < earned.length; d++) {
            wisdomOrbEarned[d] = earned[d];
        }
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
    // Paliers obtenus dans cette famille (0 = aucun ; le palier en cours est donc le n° tier + 1)
    public int getAchievementTier(int family) { return achievementTiers[family]; }

    // Au moins un palier obtenu (succès unique : obtenu)
    public boolean isAchieved(int family) { return achievementTiers[family] > 0; }

    // Tous les paliers de la famille sont obtenus (succès unique déjà obtenu)
    public boolean isAchievementMaxed(int family) {
        return achievementTiers[family] >= Achievement.maxTiers(family);
    }

    // Total des paliers obtenus, toutes familles confondues
    public int achievedCount() {
        int n = 0;
        for (int t : achievementTiers) {
            n += t;
        }
        return n;
    }

    // Valide le palier en cours de la famille et donne ses gemmes. Retourne false si tout est déjà obtenu.
    public boolean achieve(int family) {
        if (isAchievementMaxed(family)) {
            return false;
        }
        gems += Achievement.reward(family, achievementTiers[family]);
        achievementTiers[family]++;
        return true;
    }

    // Paliers sauvegardés par famille (une valeur négative garde la valeur actuelle,
    // par exemple celle convertie depuis une ancienne sauvegarde par loadStats)
    public void loadAchievementTiers(int[] tiers) {
        for (int f = 0; f < Achievement.COUNT && f < tiers.length; f++) {
            if (tiers[f] >= 0) {
                achievementTiers[f] = Math.min(tiers[f], Achievement.maxTiers(f));
            }
        }
    }

    // ---------- Actions ----------
    public void addGems(int count) {
        gems += Math.max(0, count);
    }

    // Ouvre un coffre de cette sorte. Retourne false s'il n'y en a pas.
    public boolean openChest(int chest, Random random) {
        return openChests(chest, 1, random) > 0;
    }

    // Ouvre jusqu'à `count` coffres de cette sorte d'un coup (« tout ouvrir ») : le butin est additionné.
    // Une carte d'un mage pas encore possédé le débloque (il rejoint l'équipe s'il reste une place).
    // Retourne le nombre de coffres ouverts.
    public int openChests(int chest, int count, Random random) {
        int n = Math.min(count, chests[chest]);
        if (n <= 0) {
            return 0;
        }
        for (int t = 0; t < Mage.TYPE_COUNT; t++) {
            lastChestReward[t] = 0;
            lastChestNew[t] = false;
        }
        lastChestGems = 0;
        for (int c = 0; c < n; c++) {
            Chest.Loot loot = Chest.roll(chest, random);
            for (int s = 0; s < loot.types.length; s++) {
                int type = loot.types[s];
                if (type < 0) {
                    continue;
                }
                fragments[type] += loot.amounts[s];
                lastChestReward[type] += loot.amounts[s];
                if (!owned[type]) {
                    owned[type] = true;
                    lastChestNew[type] = true;
                    addToTeamIfRoom(type);
                }
            }
            lastChestGems += loot.gems;
        }
        gems += lastChestGems;
        chests[chest] -= n;
        chestsOpened += n;
        lastChestCount = n;
        lastChestType = chest;
        return n;
    }

    private void addToTeamIfRoom(int type) {
        for (int i = 0; i < TEAM_SIZE; i++) {
            if (team[i] == NO_UNIT) {
                team[i] = type;
                return;
            }
        }
    }

    // Dépense les cartes et les gemmes, et monte le rang. Retourne false si ce n'est pas possible.
    public boolean upgrade(int type) {
        if (!canUpgrade(type)) {
            return false;
        }
        fragments[type] -= upgradeCost(type);
        gems -= upgradeGemCost(type);
        ranks[type]++;
        return true;
    }

    // Place le mage dans l'emplacement `slot` de l'équipe. S'il y était déjà ailleurs,
    // les deux emplacements sont échangés. Retourne false si rien n'a changé.
    public boolean setTeamSlot(int slot, int type) {
        if (slot < 0 || slot >= TEAM_SIZE || type < 0 || type >= Mage.TYPE_COUNT || !owned[type]
                || team[slot] == type) {
            return false;
        }
        for (int i = 0; i < TEAM_SIZE; i++) {
            if (team[i] == type) {
                team[i] = team[slot];
                break;
            }
        }
        team[slot] = type;
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
        this.chests[Chest.SMALL] = Math.max(0, chests); // anciens coffres : des petits coffres
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

    public void loadCastleGold(int level) {
        castleGoldLevel = Math.max(0, Math.min(CASTLE_GOLD_MAX_LEVEL, level));
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
        // Anciennes sauvegardes : les 22 succès uniques sont convertis en paliers (sans redonner de gemmes)
        int[] tiers = Achievement.tiersFromLegacy(done);
        System.arraycopy(tiers, 0, achievementTiers, 0, Achievement.COUNT);
    }
}
