package com.example.magicdefense.model;

import java.util.Random;

// Les 3 coffres (achetés au Marché ou gagnés en jouant). Chaque coffre contient plusieurs « types »
// de cartes (des mages différents) ; la rareté de chaque type est tirée avec les probabilités
// ci-dessous, puis les cartes sont réparties (les communes sont les plus nombreuses).
// Les gemmes sont en plus et ne comptent pas comme un type de carte.
public final class Chest {
    public static final int SMALL = 0;    // Petit coffre
    public static final int BIG = 1;      // Grand coffre
    public static final int MYSTERY = 2;  // Coffre mystérieux
    public static final int COUNT = 3;

    public static final String[] NAMES = {"Petit coffre", "Grand coffre", "Coffre mystérieux"};

    private static final int[] PRICES = {35, 75, 160};     // en gemmes
    private static final int[] CARDS = {22, 42, 78};       // cartes en moyenne...
    private static final int[] SPREAD = {2, 4, 6};         // ...à ± tant près
    private static final int[] GEMS_MIN = {1, 3, 7};
    private static final int[] GEMS_MAX = {3, 5, 10};

    // Probabilités (en ‰) de chaque rareté pour chaque type de carte du coffre :
    // {commune, rare, épique, légendaire} ; ce qui manque pour arriver à 1000 = pas de carte de ce type
    private static final int[][][] ODDS = {
            {   // Petit coffre : 2 types
                    {1000, 0, 0, 0},          // 1re carte : commune garantie
                    {500, 250, 0, 0},         // 2e : 50 % commune, 25 % rare, 25 % rien
            },
            {   // Grand coffre : 3 types
                    {1000, 0, 0, 0},
                    {300, 600, 95, 5},        // 30 % C, 60 % R, 9,5 % É, 0,5 % L
                    {0, 800, 190, 10},        // 80 % R, 19 % É, 1 % L
            },
            {   // Coffre mystérieux : 4 types
                    {800, 200, 0, 0},
                    {0, 800, 200, 0},
                    {0, 650, 300, 50},        // 55 % R (+ les 10 % non attribués), 30 % É, 5 % L
                    {0, 0, 750, 250},
            },
    };

    private Chest() {
    }

    public static int price(int chest) { return PRICES[chest]; }
    public static int cards(int chest) { return CARDS[chest]; }
    public static int spread(int chest) { return SPREAD[chest]; }
    public static int gemsMin(int chest) { return GEMS_MIN[chest]; }
    public static int gemsMax(int chest) { return GEMS_MAX[chest]; }
    public static int slots(int chest) { return ODDS[chest].length; }

    // Probabilité (‰) d'une rareté pour le type de carte n° slot
    public static int odds(int chest, int slot, int rarity) {
        return ODDS[chest][slot][rarity];
    }

    // Texte des probabilités d'un type de carte, ex. « 50 % C · 25 % R · 25 % rien »
    public static String oddsText(int chest, int slot) {
        StringBuilder sb = new StringBuilder();
        int total = 0;
        for (int r = 0; r < Rarity.COUNT; r++) {
            int p = ODDS[chest][slot][r];
            total += p;
            if (p > 0) {
                if (sb.length() > 0) {
                    sb.append(" · ");
                }
                sb.append(permille(p)).append(' ').append(Rarity.SHORT[r]);
            }
        }
        if (total < 1000) {
            sb.append(" · ").append(permille(1000 - total)).append(" rien");
        }
        return sb.toString();
    }

    private static String permille(int p) {
        return (p % 10 == 0 ? String.valueOf(p / 10) : (p / 10) + "," + (p % 10)) + " %";
    }

    // ---------- Tirage ----------
    // Contenu d'un coffre : un type de mage par case (-1 = case vide), le nombre de cartes, les gemmes
    public static final class Loot {
        public final int[] types;
        public final int[] amounts;
        public final int gems;

        Loot(int[] types, int[] amounts, int gems) {
            this.types = types;
            this.amounts = amounts;
            this.gems = gems;
        }
    }

    public static Loot roll(int chest, Random random) {
        int slots = slots(chest);
        int[] types = new int[slots];
        int[] rarities = new int[slots];
        boolean[] taken = new boolean[Mage.TYPE_COUNT];
        int filled = 0;
        for (int s = 0; s < slots; s++) {
            types[s] = -1;
            int rarity = rollRarity(chest, s, random);
            if (rarity < 0) {
                continue; // pas de carte pour cette case (petit coffre)
            }
            int type = pickType(rarity, taken, random);
            if (type < 0) {
                continue; // plus aucun mage disponible
            }
            taken[type] = true;
            types[s] = type;
            rarities[s] = Rarity.of(type);
            filled++;
        }

        // Nombre total de cartes, réparti selon la rareté (au moins 1 carte par type)
        int total = CARDS[chest] - SPREAD[chest] + random.nextInt(2 * SPREAD[chest] + 1);
        int[] amounts = new int[slots];
        if (filled > 0) {
            float sum = 0;
            for (int s = 0; s < slots; s++) {
                if (types[s] >= 0) {
                    sum += Rarity.cardWeight(rarities[s]);
                }
            }
            int given = 0;
            int first = -1;
            for (int s = 0; s < slots; s++) {
                if (types[s] >= 0) {
                    amounts[s] = Math.max(1, Math.round(total * Rarity.cardWeight(rarities[s]) / sum));
                    given += amounts[s];
                    if (first < 0) {
                        first = s;
                    }
                }
            }
            amounts[first] = Math.max(1, amounts[first] + total - given); // l'arrondi va à la 1re carte
        }
        int gems = GEMS_MIN[chest] + random.nextInt(GEMS_MAX[chest] - GEMS_MIN[chest] + 1);
        return new Loot(types, amounts, gems);
    }

    private static int rollRarity(int chest, int slot, Random random) {
        int roll = random.nextInt(1000);
        int acc = 0;
        for (int r = 0; r < Rarity.COUNT; r++) {
            acc += ODDS[chest][slot][r];
            if (roll < acc) {
                return r;
            }
        }
        return -1;
    }

    // Un mage de cette rareté pas encore tiré dans ce coffre ; s'il n'y en a plus,
    // on cherche dans la rareté au-dessus, puis en dessous
    private static int pickType(int rarity, boolean[] taken, Random random) {
        for (int d = 0; d < Rarity.COUNT * 2; d++) {
            int r = d < Rarity.COUNT - rarity ? rarity + d : rarity - (d - (Rarity.COUNT - rarity) + 1);
            if (r < 0 || r >= Rarity.COUNT) {
                continue;
            }
            int[] pool = new int[Mage.TYPE_COUNT];
            int n = 0;
            for (int t = 0; t < Mage.TYPE_COUNT; t++) {
                if (!taken[t] && Rarity.of(t) == r) {
                    pool[n++] = t;
                }
            }
            if (n > 0) {
                return pool[random.nextInt(n)];
            }
        }
        return -1;
    }
}
