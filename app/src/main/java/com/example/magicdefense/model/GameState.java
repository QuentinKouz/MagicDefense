package com.example.magicdefense.model;

// Les différents écrans / états du jeu
public enum GameState {
    START,
    PLAYING,
    PAUSED,
    VICTORY,
    GAME_OVER,
    OPTIONS,
    UPGRADES,      // écran MAGES : coffres, rangs permanents, équipe
    CHEST_OPENING, // animation d'ouverture d'un coffre (par-dessus l'écran MAGES)
    MARKET,        // Marché : achat des unités exclusives avec les gemmes
}
