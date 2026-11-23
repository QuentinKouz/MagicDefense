package com.example.magicdefense.view;

import android.graphics.Color;

// Couleurs et noms affichés pour chaque élément (indice = Mage.type)
public class Palette {
    public static final String[] ELEMENT_NAMES = {"Eau", "Feu", "Terre", "Foudre", "Poison", "Air", "Invocateur",
            "Ouvrier", "Lumière", "Ténèbres", "Scientifique"};
    public static final int[] ELEMENT_COLORS = {
            Color.rgb(70, 150, 255),   // Eau
            Color.rgb(255, 110, 50),   // Feu
            Color.rgb(150, 110, 60),   // Terre
            Color.rgb(170, 80, 255),   // Foudre
            Color.rgb(120, 200, 60),   // Poison
            Color.rgb(185, 225, 240),  // Air
            Color.rgb(230, 100, 180),  // Invocateur
            Color.rgb(230, 160, 50),   // Ouvrier
            Color.rgb(255, 235, 140),  // Lumière
            Color.rgb(120, 70, 170),   // Ténèbres
            Color.rgb(130, 245, 80),   // Scientifique (vert fluo des potions)
    };
    // Description courte (Marché)
    public static final String[] ELEMENT_DESCRIPTIONS = {
            "Dégâts de zone", "Dégâts qui explosent", "Salve de cristaux", "Tir éclair",
            "Empoisonne : dégâts sur la durée", "Repousse les ennemis", "Invoque des chevaliers gardiens",
            "Construit un mur, lance des marteaux", "Renforce tous les mages, rayons sur tous",
            "Exécute les blessés, grande zone qui ralentit",
            "Fioles de zone, bombe nucléaire sur tout le terrain",
    };

    // Réactions élémentaires (indice = Reaction.xxx)
    public static final int[] REACTION_COLORS = {
            Color.rgb(120, 210, 255),  // Électrocution
            Color.rgb(255, 120, 30),   // Lave
            Color.rgb(225, 228, 238),  // Vapeur
            Color.rgb(180, 240, 60),   // Explosion toxique
            Color.rgb(130, 200, 80),   // Nuage toxique
            Color.rgb(225, 195, 125),  // Tempête de sable
            Color.rgb(150, 110, 70),   // Boue
            Color.rgb(100, 170, 90),   // Marécage
            Color.rgb(190, 235, 255),  // Gel
            Color.rgb(110, 190, 255),  // Geyser
            Color.rgb(255, 190, 60),   // Surcharge
            Color.rgb(255, 90, 40),    // Embrasement
            Color.rgb(255, 160, 220),  // Feux follets
            Color.rgb(150, 240, 220),  // Cristallisation
            Color.rgb(200, 230, 40),   // Acide
            Color.rgb(170, 140, 100),  // Éboulement
            Color.rgb(230, 255, 120),  // Paralysie
            Color.rgb(200, 180, 255),  // Orage
            Color.rgb(240, 130, 255),  // Chaîne spectrale
            Color.rgb(170, 80, 200),   // Malédiction
            Color.rgb(210, 220, 240),  // Vortex
            Color.rgb(175, 175, 180),  // Béton
            Color.rgb(255, 140, 50),   // Forge
            Color.rgb(200, 150, 90),   // Fondations
            Color.rgb(250, 210, 90),   // Marteau-pilon
            Color.rgb(70, 60, 50),     // Goudron
            Color.rgb(220, 190, 140),  // Catapulte
            Color.rgb(190, 140, 80),   // Barricade
            Color.rgb(255, 245, 190),  // Sanctuaire
            Color.rgb(255, 110, 70),   // Démolition
            Color.rgb(160, 220, 255),  // Arc-en-ciel
            Color.rgb(255, 230, 150),  // Supernova
            Color.rgb(200, 255, 240),  // Prisme
            Color.rgb(255, 250, 200),  // Jugement
            Color.rgb(220, 255, 180),  // Purification
            Color.rgb(170, 255, 210),  // Aurore
            Color.rgb(255, 225, 120),  // Croisade
            Color.rgb(60, 80, 170),    // Abysses
            Color.rgb(150, 80, 220),   // Flammes noires
            Color.rgb(140, 120, 150),  // Tombeau
            Color.rgb(180, 110, 255),  // Éclair noir
            Color.rgb(130, 170, 90),   // Nécrose
            Color.rgb(110, 60, 160),   // Trou noir
            Color.rgb(200, 160, 230),  // Moisson
            Color.rgb(170, 225, 235),  // Solution saline
            Color.rgb(255, 150, 60),   // Combustion
            Color.rgb(230, 150, 90),   // Calcination
            Color.rgb(150, 255, 200),  // Électrolyse
            Color.rgb(170, 255, 60),   // Toxine mutante
            Color.rgb(200, 240, 170),  // Aérosol
            Color.rgb(160, 255, 150),  // Clonage
            Color.rgb(255, 90, 60),    // Dynamite
            Color.rgb(240, 255, 120),  // Fission
            Color.rgb(210, 120, 255),  // Antimatière
    };
    // Murs de l'Ouvrier
    public static final int WALL_STONE = Color.rgb(150, 140, 130);
    public static final int WALL_STONE_DARK = Color.rgb(95, 88, 82);
    public static final int WALL_MORTAR = Color.rgb(70, 64, 60);
    public static final int WALL_BAR = Color.rgb(230, 160, 50);
    public static final int VULNERABLE = Color.rgb(255, 150, 40); // ennemi vulnérable (Acide, Malédiction)

    // Ennemis
    public static final int BOSS_BAR = Color.rgb(200, 40, 60);
    public static final int SHAMAN_GLOW = Color.argb(70, 200, 90, 255);
    public static final int POISONED_BAR = Color.rgb(140, 220, 60);

    // Biomes (indice = LevelSpec.BIOME_xxx) : ciel haut, ciel bas, sol, chemin, décor, accent
    public static final int[][] BIOME_COLORS = {
            {Color.rgb(25, 30, 60), Color.rgb(60, 95, 120), Color.rgb(50, 95, 55), Color.rgb(150, 125, 80),
                    Color.rgb(35, 110, 50), Color.rgb(230, 200, 90)},    // Prairie : arbres, fleurs
            {Color.rgb(60, 35, 40), Color.rgb(200, 120, 70), Color.rgb(200, 160, 95), Color.rgb(150, 105, 60),
                    Color.rgb(70, 140, 70), Color.rgb(250, 220, 150)},   // Désert : cactus, dunes
            {Color.rgb(25, 35, 70), Color.rgb(120, 150, 200), Color.rgb(215, 225, 240), Color.rgb(140, 160, 190),
                    Color.rgb(40, 90, 80), Color.rgb(255, 255, 255)},    // Toundra : sapins enneigés
            {Color.rgb(20, 30, 30), Color.rgb(60, 85, 70), Color.rgb(55, 75, 45), Color.rgb(95, 85, 55),
                    Color.rgb(85, 110, 50), Color.rgb(90, 140, 120)},    // Marais : roseaux, mares
            {Color.rgb(30, 10, 15), Color.rgb(130, 40, 25), Color.rgb(60, 40, 40), Color.rgb(120, 60, 40),
                    Color.rgb(45, 30, 30), Color.rgb(255, 120, 30)},     // Volcan : rochers, lave
            {Color.rgb(15, 10, 40), Color.rgb(90, 50, 120), Color.rgb(45, 35, 80), Color.rgb(120, 100, 170),
                    Color.rgb(140, 110, 220), Color.rgb(255, 240, 180)}, // Crépuscule : cristaux, étoiles
    };
    // Filtre du terrain en jeu : multiplicateur et ajout (0xRRGGBB), Prairie = pas de filtre
    public static final int[][] BIOME_TERRAIN_FILTER = {
            {0xFFFFFF, 0x000000},
            {0xFFE0A0, 0x302000},
            {0xC8D8FF, 0x505870},
            {0x90B080, 0x101808},
            {0xFF9070, 0x200000},
            {0xA090FF, 0x100820},
    };

    public static final int BACKGROUND = Color.rgb(20, 20, 40);
    public static final int CELL_DARK = Color.rgb(45, 60, 45);
    public static final int CELL_LIGHT = Color.rgb(60, 80, 60);
    public static final int CASTLE = Color.rgb(80, 200, 255);
    public static final int ENEMY = Color.rgb(220, 60, 60);
    public static final int GOLD = Color.rgb(255, 215, 0);
    public static final int DISABLED = Color.rgb(70, 70, 80);
    public static final int GAME_OVER = Color.rgb(255, 90, 90);
    public static final int GAUGE_EMPTY = Color.rgb(60, 60, 80);
    public static final int GAUGE_CHARGING = Color.rgb(170, 170, 200);
    public static final int STUNNED = Color.rgb(120, 200, 255); // ennemi étourdi

    // Mode nuit
    public static final int BACKGROUND_NIGHT = Color.rgb(5, 5, 15);
    public static final int CELL_DARK_NIGHT = Color.rgb(20, 28, 40);
    public static final int CELL_LIGHT_NIGHT = Color.rgb(28, 38, 55);

    // Unités de nuit
    public static final int ENEMY_NIGHT = Color.rgb(45, 25, 30);
    public static final int ENEMY_EYES = Color.rgb(255, 40, 40);
    public static final int TORCH_STICK = Color.rgb(110, 70, 35);
    public static final int TORCH_FLAME = Color.rgb(255, 140, 30);
    public static final int TORCH_CORE = Color.rgb(255, 230, 120);
    public static final int TORCH_GLOW = Color.argb(45, 255, 150, 50); // halo transparent

    // Fenêtre de pause
    public static final int POPUP = Color.rgb(35, 35, 60);
    public static final int MENU_BUTTON = Color.rgb(80, 80, 130);
    public static final int RESUME = Color.rgb(70, 170, 90);
    public static final int QUIT = Color.rgb(190, 60, 60);
    public static final int ICON_DISABLED = Color.rgb(120, 120, 130);

    // Interrupteur
    public static final int TOGGLE_ON = Color.rgb(70, 170, 90);
    public static final int TOGGLE_OFF = Color.rgb(90, 90, 110);
    public static final int TEXT_INACTIVE = Color.rgb(130, 130, 150);

    // Château et étoiles
    public static final int CASTLE_HIT = Color.rgb(230, 50, 70);
    public static final int STAR_EMPTY = Color.rgb(70, 70, 90);
    public static final int HP_BAR_BG = Color.rgb(50, 50, 70);

    public static final int MAP_PATH = Color.rgb(70, 70, 110);

    // V2 : mode infini, écran MAGES, coffres
    public static final int ENDLESS = Color.rgb(120, 60, 150);        // bouton INFINI
    public static final int ENDLESS_TEXT = Color.rgb(210, 160, 255);  // « MODE INFINI » en jeu
    public static final int UPGRADE_ROW = Color.rgb(50, 50, 82);      // fond d'une ligne de l'écran MAGES
    public static final int CHEST_WOOD = Color.rgb(140, 85, 40);
    public static final int CHEST_LID = Color.rgb(170, 105, 50);
    public static final int GEM = Color.rgb(80, 220, 230);            // gemmes
    public static final int GEM_DARK = Color.rgb(30, 140, 170);
    public static final int MARKET = Color.rgb(180, 120, 40);         // bouton MARCHÉ
    public static final int TEAM = Color.rgb(90, 200, 110);           // cadre « dans l'équipe »
    public static final int PARCHMENT = Color.rgb(240, 224, 184);     // bulle de la carte
    public static final int PARCHMENT_DARK = Color.rgb(200, 176, 128);

    // Mise à jour : ultimes, château, succès, artefacts, navigation
    public static final int FURY = Color.rgb(255, 80, 60);            // Rage arcanique
    public static final int SHIELD = Color.rgb(120, 200, 255);        // Bouclier divin
    public static final int METEOR = Color.rgb(255, 140, 40);         // Météorite
    public static final int ARTIFACT = Color.rgb(255, 170, 60);       // artefacts (orangé doré)
    public static final int ACHIEVEMENT = Color.rgb(250, 210, 80);    // succès
    public static final int CASTLE_PAGE = Color.rgb(90, 110, 150);    // page Château
    public static final int HEAL = Color.rgb(80, 190, 110);           // soin du château
    public static final int NIGHT_LEVEL = Color.rgb(150, 170, 255);   // niveau de nuit (lune)
    public static final int KNIGHT_BAR = Color.rgb(230, 120, 200);    // PV des chevaliers
    public static final int[] DIFFICULTY_COLORS = {
            Color.rgb(80, 170, 100), Color.rgb(80, 120, 200), Color.rgb(200, 120, 40), Color.rgb(190, 40, 50)};
    public static final int[] ULTIMATE_COLORS = {STUNNED, FURY, METEOR, SHIELD};

    // Cartes et coffres : couleur de chaque rareté (commune, rare, épique, légendaire) et de chaque coffre
    public static final int[] RARITY_COLORS = {
            Color.rgb(140, 170, 200), Color.rgb(255, 150, 50), Color.rgb(190, 90, 255), Color.rgb(90, 240, 210)};
    public static final int[] CHEST_COLORS = {
            Color.rgb(200, 140, 70), Color.rgb(240, 190, 60), Color.rgb(170, 90, 240)};

}
