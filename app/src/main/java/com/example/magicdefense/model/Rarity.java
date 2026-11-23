package com.example.magicdefense.model;

// Rareté des cartes de mage (système façon jeu de cartes à collectionner) :
//  - Commune    : Feu, Terre
//  - Rare       : Eau, Poison
//  - Épique     : Foudre, Air, Invocateur, Ouvrier
//  - Légendaire : Lumière, Ténèbres
// Plus une carte est rare, moins on en trouve dans les coffres, et moins il en faut pour l'améliorer.
public final class Rarity {
    public static final int COMMON = 0;
    public static final int RARE = 1;
    public static final int EPIC = 2;
    public static final int LEGENDARY = 3;
    public static final int COUNT = 4;

    public static final String[] NAMES = {"Commune", "Rare", "Épique", "Légendaire"};
    public static final String[] SHORT = {"C", "R", "É", "L"};

    // Rareté de chaque type de mage (indice = type : Eau, Feu, Terre, Foudre, Poison, Air, Invocateur,
    // Ouvrier, Lumière, Ténèbres)
    private static final int[] OF_TYPE = {RARE, COMMON, COMMON, EPIC, RARE, EPIC, EPIC, EPIC, LEGENDARY, LEGENDARY};

    // Part des cartes d'un coffre selon la rareté (les communes sont les plus nombreuses)
    private static final float[] CARD_WEIGHT = {1f, 0.45f, 0.15f, 0.05f};
    // Amélioration du rang n au rang n+1 : CARDS[r] × n cartes et GEMS[r] × n gemmes
    private static final int[] UPGRADE_CARDS = {10, 6, 3, 1};
    private static final int[] UPGRADE_GEMS = {2, 3, 5, 8};
    // Prix d'un mage choisi en partie (PO) : prix de base + hausse × mages déjà choisis dans la partie
    private static final int[] MAGE_PRICE = {10, 14, 18, 25};
    private static final int[] MAGE_PRICE_STEP = {2, 3, 4, 6};

    private Rarity() {
    }

    public static int of(int type) {
        return type >= 0 && type < OF_TYPE.length ? OF_TYPE[type] : COMMON;
    }

    public static float cardWeight(int rarity) {
        return CARD_WEIGHT[rarity];
    }

    public static int upgradeCards(int rarity) {
        return UPGRADE_CARDS[rarity];
    }

    public static int upgradeGems(int rarity) {
        return UPGRADE_GEMS[rarity];
    }

    // Prix d'un mage de cette rareté quand `bought` mages ont déjà été choisis dans la partie
    // (Commune 10 +2, Rare 14 +3, Épique 18 +4, Légendaire 25 +6)
    public static int magePrice(int rarity, int bought) {
        return MAGE_PRICE[rarity] + MAGE_PRICE_STEP[rarity] * Math.max(0, bought);
    }
}
