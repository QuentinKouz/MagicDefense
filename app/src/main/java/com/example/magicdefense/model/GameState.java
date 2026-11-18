package com.example.magicdefense.model;

// Les différents écrans / états du jeu
public enum GameState {
    START,         // accueil : pages Marché, Mages, Campagne, Infini, Château (navigation par glissement)
    PLAYING,
    PAUSED,
    VICTORY,
    GAME_OVER,
    OPTIONS,
    CHEST_OPENING, // animation d'ouverture d'un coffre (par-dessus la page Mages)
}
