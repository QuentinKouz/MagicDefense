package com.example.magicdefense.model;

import java.util.Random;

// Progression permanente (gardée d'une partie à l'autre) :
// coffres, fragments et rang de chaque élément, gemmes, unités possédées, équipe, record du mode infini.
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
    public static final int WAVES_PER_ENDLESS_CHEST = 3;  // mode infini : 1 coffre toutes les 3 vagues
    public static final int LEVELS_PER_BONUS_CHEST = 4;   // un coffre de plus tous les 4 niveaux
    public static final int TEAM_SIZE = 4;                // mages emmenés en partie
    public static final int NO_UNIT = -1;

    // Prix au Marché, en gemmes (indice = type ; 0 = possédé dès le départ)
    private static final int[] PRICES = {0, 0, 0, 0, 15, 25, 40};

    // ---------- État ----------
    private int chests = 0;
    private int gems = 0;
    private final int[] fragments = new int[Mage.TYPE_COUNT];
    private final int[] ranks = new int[Mage.TYPE_COUNT];
    private final boolean[] owned = new boolean[Mage.TYPE_COUNT];
    private final int[] team = new int[TEAM_SIZE];
    private int bestEndlessWave = 0;
    private final int[] lastChestReward = new int[Mage.TYPE_COUNT]; // contenu du dernier coffre ouvert
    private int lastChestGems = 0;
    private int chestsOpened = 0; // nombre de coffres ouverts (sert à animer l'ouverture)

    public Progression() {
        for (int t = 0; t < Mage.TYPE_COUNT; t++) {
            ranks[t] = 1;
            owned[t] = !Mage.isExclusive(t);
        }
        for (int i = 0; i < TEAM_SIZE; i++) {
            team[i] = i; // Eau, Feu, Terre, Foudre
        }
    }

    // ---------- Lecture ----------
    public int getChests() { return chests; }
    public int getGems() { return gems; }
    public int getFragments(int type) { return fragments[type]; }
    public int getRank(int type) { return ranks[type]; }
    public boolean isOwned(int type) { return owned[type]; }
    public int getBestEndlessWave() { return bestEndlessWave; }
    public int getLastChestReward(int type) { return lastChestReward[type]; }
    public int getLastChestGems() { return lastChestGems; }
    public int getChestsOpened() { return chestsOpened; }
    public static int price(int type) { return PRICES[type]; }

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

    // Coffres gagnés en mode infini selon les vagues entièrement repoussées
    public static int chestsForEndless(int wavesCleared) {
        return Math.max(0, wavesCleared) / WAVES_PER_ENDLESS_CHEST;
    }

    // ---------- Actions ----------
    public void addChests(int count) {
        chests += Math.max(0, count);
    }

    // Ouvre un coffre : des fragments d'éléments possédés tirés au hasard, et des gemmes.
    // Retourne false s'il n'y a pas de coffre.
    public boolean openChest(Random random) {
        if (chests <= 0) {
            return false;
        }
        chests--;
        chestsOpened++;
        for (int t = 0; t < Mage.TYPE_COUNT; t++) {
            lastChestReward[t] = 0;
        }
        int[] pool = new int[Mage.TYPE_COUNT];
        int n = 0;
        for (int t = 0; t < Mage.TYPE_COUNT; t++) {
            if (owned[t]) {
                pool[n++] = t;
            }
        }
        for (int i = 0; i < LOTS_PER_CHEST; i++) {
            int type = pool[random.nextInt(n)];
            int amount = LOT_MIN + random.nextInt(LOT_MAX - LOT_MIN + 1);
            fragments[type] += amount;
            lastChestReward[type] += amount;
        }
        lastChestGems = GEMS_MIN + random.nextInt(GEMS_MAX - GEMS_MIN + 1);
        gems += lastChestGems;
        return true;
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

    // Nouveau record du mode infini ? (retourne true si battu)
    public boolean recordEndlessWave(int wave) {
        if (wave > bestEndlessWave) {
            bestEndlessWave = wave;
            return true;
        }
        return false;
    }

    // Chargement de la sauvegarde (anciennes sauvegardes : seulement les 4 premiers éléments)
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
        this.bestEndlessWave = Math.max(0, bestEndlessWave);
    }
}