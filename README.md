# Magic Defense

Mini-jeu Android de type *tower defense* à fusion : recrutez des mages, fusionnez-les pour les rendre plus puissants et empêchez les gobelins d'atteindre le château.

Projet réalisé en binôme dans le cadre du TP d'IHM (M2) — application Android native en Java, développée avec Android Studio.

**Auteurs**
- Quentin KOUZMITCH — [@QuentinKouz](https://github.com/QuentinKouz)
- Valentin MIAS — [@MIASValentin](https://github.com/MIASValentin)

*Master 2 IHM — Université, année 2026-2027*

---

## 📥 Télécharger et jouer

**[⬇️ Télécharger MagicDefense.apk](https://github.com/QuentinKouz/MagicDefense/releases/latest/download/MagicDefense.apk)**  
(toutes les versions : [page des releases](https://github.com/QuentinKouz/MagicDefense/releases))

1. Ouvrez le lien ci-dessus **depuis votre téléphone Android** (ou transférez-y le fichier).
2. Ouvrez `MagicDefense.apk` une fois téléchargé.
3. Android demande d'autoriser l'installation depuis cette source (navigateur, gestionnaire de fichiers…) : acceptez, puis **Installer**.
4. Lancez **Magic Defense** depuis l'écran d'accueil.

> Android uniquement (pas d'iPhone). Le jeu utilise l'accéléromètre et le capteur de luminosité : jouez de préférence sur un vrai téléphone.  
> L'application n'étant pas publiée sur le Play Store, Play Protect peut afficher un avertissement : choisissez « Installer quand même ».

---

## Le jeu

Les gobelins sortent de la forêt et marchent vers le rempart. Vos mages, placés sur un damier de 4 × 3 cases devant le château, les attaquent automatiquement.

- **Recruter** : chaque bouton du bas place un mage de l'élément choisi sur une case libre (10 PO).
- **Fusionner** : glissez un mage sur un autre **du même élément et du même niveau** → il passe au niveau supérieur (niveau 5 maximum). Glisser sur une case vide déplace le mage.
- **Gagner de l'or** : chaque gobelin tué rapporte des pièces d'or.
- **Protéger le château** : il a 10 000 PV. Les étoiles obtenues dépendent des PV restants à la fin du niveau (3 étoiles à partir de 70 %, 2 à partir de 40 %).

### Les quatre éléments

| Élément | Spécificité | Évolution visuelle |
|---|---|---|
| **Eau** | Dégâts de zone, rayon qui grandit avec le niveau | goutte → bulle → averse → cyclone → tsunami |
| **Feu** | Dégâts qui augmentent fortement à chaque fusion | braise → boule de feu → flamme bleue → aura dorée |
| **Terre** | Une salve de projectiles par tir (autant que son niveau), concentrée sur une cible | caillou → ambre → émeraude → cristaux dorés |
| **Foudre** | Tir instantané et cadence rapide | éclair violet → ramifications → double éclair doré |

### Progression

6 niveaux débloqués l'un après l'autre sur une carte. Chaque niveau a plus de vagues (niveau 1 : 3 vagues, niveau 2 : 4 vagues…) et des gobelins plus résistants. Les meilleures étoiles de chaque niveau sont sauvegardées.

---

## Interactions

### Tactile
- **Validation au relâchement** : un bouton n'agit que si le doigt est relâché dessus ; glisser hors du bouton annule l'action.
- **Glisser-déposer** pour fusionner ou déplacer les mages. Le mage déplacé est dessiné au-dessus du doigt pour rester visible, et les cases sont surlignées pendant le geste (vert : fusion possible, blanc : case libre).
- **Grandes cibles** (≥ 48 dp) et éléments espacés pour éviter les erreurs de doigt ; boutons principaux en bas de l'écran, accessibles au pouce, pour jouer à une main.
- Aucune saisie de texte.

### Capteurs
| Capteur | Action | Robustesse |
|---|---|---|
| **Accéléromètre** | **Secouer le téléphone** lance l'ultime (tous les gobelins étourdis 4 s) quand la jauge est pleine (20 gobelins tués) | Secousse forte (> 2,2 g) et **maintenue 1,5 s** ; un anneau de progression et des vibrations guident le joueur |
| **Capteur de luminosité** | **Cacher le capteur** (ou jouer dans le noir) fait tomber la nuit : gobelins 30 % plus rapides, torches, yeux rouges, mais +20 % d'or | Seuils différents pour entrer et sortir de la nuit (30 / 80 lux) et confirmation de 600 ms pour éviter le clignotement ; désactivable dans les options |

Les capteurs sont coupés quand l'application passe en arrière-plan.

### Retours
- **Visuels** : feedback d'appui sur tous les boutons, flash rouge du rempart, bandeau jour/nuit, étoiles animées à la victoire.
- **Sonores** : bruitages (recrutement, fusion, élimination, château touché, ultime, victoire, défaite, clic) et musique de fond médiévale en boucle, désactivable.
- **Haptiques** : vibrations à la fusion, quand le château est touché, pendant la secousse et au déclenchement de l'ultime.

### Options
- Désactiver le mode nuit (Non / Oui)
- Musique (Non / Oui)
- Volume (curseur synchronisé avec les boutons de volume du téléphone)
- Bouton de coupure du son à l'accueil et en jeu

---

## Compatibilité des écrans

- Tailles exprimées en **dp** : même taille physique sur tous les appareils.
- **Colonne de jeu de 480 dp maximum**, centrée : sur tablette, le damier et les fenêtres restent à taille confortable et le décor remplit les côtés.
- **Même difficulté partout** : la vitesse des gobelins est ajustée à la hauteur de l'écran (temps de traversée identique sur petit téléphone, Pixel 7a ou tablette).
- **Caméra / encoche** prises en compte : le HUD se décale si nécessaire.
- Plein écran immersif (barres système masquées, réapparition par glissement).

Testé sur **Google Pixel 7a** (Android) et sur émulateurs (petit téléphone, tablette).

> Application Android uniquement : elle ne fonctionne pas sur iPhone (iOS).

---

## Architecture

Organisation **Modèle – Vue – Contrôleur** : le modèle ne dépend d'aucune classe Android.

```
com.example.magicdefense
├── MainActivity           Plein écran, cycle de vie (pause / reprise)
├── model/                 Règles du jeu, sans dessin ni toucher
│   ├── GameModel          État de la partie, vagues, tirs, fusion, victoire, événements
│   ├── GameState          START, PLAYING, PAUSED, OPTIONS, VICTORY, GAME_OVER
│   ├── Board              Géométrie : colonne de jeu, damier, adaptation à l'écran
│   ├── Mage, Enemy        Unités (caractéristiques par élément et par niveau)
│   └── Projectile, Bolt, Splash, HitEffect   Attaques et effets
├── view/                  Affichage et retours
│   ├── GameView           Vue Android : boucle de jeu (~60 images/s), relie tout
│   ├── GameRenderer       Dessin des écrans, du HUD et des unités
│   ├── AttackRenderer     Dessin des attaques selon l'élément et le niveau
│   ├── TerrainRenderer    Décor pixel art généré (forêt, prairie, rempart, cour)
│   ├── Sprites            Mages et gobelins en pixel art
│   ├── Hud                Position de tous les boutons et fenêtres
│   ├── Palette            Couleurs
│   ├── SoundManager       Bruitages (SoundPool)
│   ├── MusicPlayer        Musique de fond (MediaPlayer)
│   └── Haptics            Vibrations
├── controller/            Entrées du joueur
│   ├── TouchController    Touchers, glisser-déposer, boutons, curseur de volume
│   ├── ShakeDetector      Accéléromètre → ultime
│   └── LightSensor        Luminosité → mode nuit
└── data/
    └── SaveManager        Sauvegarde (SharedPreferences) : niveaux, étoiles, options
```

Le modèle prévient la vue de ce qui se passe (château touché, fusion, victoire…) par l'interface `GameModel.GameListener`, ce qui déclenche sons, vibrations et sauvegarde.

---

## Installation

1. Cloner le dépôt et l'ouvrir avec **Android Studio**.
2. Laisser Gradle synchroniser le projet.
3. Brancher un téléphone Android (débogage USB activé) ou lancer un émulateur.
4. Cliquer sur **Run ▶**.

Pour tester les capteurs sur émulateur : *Extended controls → Virtual sensors* (accélération et luminosité).

Permission utilisée : `VIBRATE`.

---

## Ressources

Graphismes (pixel art), bruitages et musique créés pour le projet, générés par code : aucune ressource externe.
