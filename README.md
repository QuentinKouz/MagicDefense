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

**Version 2 (branche `v2`)** : l'APK est directement dans le dépôt, [`apk/MagicDefense-v2.apk`](https://github.com/QuentinKouz/MagicDefense/raw/v2/apk/MagicDefense-v2.apk) (version 2.1).  
**Mise à jour sans perte** : installez le nouvel APK par-dessus l'ancien, sans désinstaller. Tous les APK de la branche `v2` sont signés avec la même clé (`app/signing/magicdefense.keystore`), donc Android accepte la mise à jour et garde la sauvegarde (progression, coffres, gemmes, partie en cours).  
Seule exception : passer de la V1 (Releases GitHub) à la V2 demande encore de désinstaller la V1, signée avec une autre clé.

1. Ouvrez le lien ci-dessus **depuis votre téléphone Android** (ou transférez-y le fichier).
2. Ouvrez `MagicDefense.apk` une fois téléchargé.
3. Android demande d'autoriser l'installation depuis cette source (navigateur, gestionnaire de fichiers…) : acceptez, puis **Installer**.
4. Lancez **Magic Defense** depuis l'écran d'accueil.

> Android uniquement (pas d'iPhone). Le jeu utilise l'accéléromètre et le capteur de luminosité : jouez de préférence sur un vrai téléphone.  
> L'application n'étant pas publiée sur le Play Store, Play Protect peut afficher un avertissement : choisissez « Installer quand même ».

---

## Le jeu

Les gobelins sortent de la forêt et marchent vers le rempart. Vos mages, placés sur un damier de 4 × 3 cases devant le château, les attaquent automatiquement.

- **Recruter** : chaque bouton du bas place un mage de l'élément choisi sur une case libre (10 PO). Le bouton **dé** (sous la jauge) place un mage **au hasard** de l'équipe pour 7 PO.
- **Cases bloquées** : la rangée du fond du damier (4 cases) est enchaînée au début de chaque partie. Touchez une case pour la débloquer avec des PO (30, puis 50, 80, 120).
- **Soigner** : le bouton **+** (sous la jauge) rend 25 % des PV max du château, sans jamais dépasser le maximum. 3 soins par partie (50, 75, puis 100 PO).
- **Vitesse ×1 / ×2 / ×3** : bouton sous le son. ×2 est gratuit, ×3 s'achète une fois pour toutes au Marché.
- **AUTO** : le mode automatique lance seul l'ultime et les compétences dès qu'ils sont prêts.
- **Fusionner** : glissez un mage sur un autre **du même élément et du même niveau** → il passe au niveau supérieur (niveau 5 maximum). Glisser sur une case vide déplace le mage.
- **Gagner de l'or** : chaque gobelin tué rapporte des pièces d'or.
- **Protéger le château** : il a 10 000 PV (plus avec les améliorations de la page Château). Les étoiles obtenues dépendent des PV restants à la fin du niveau (3 étoiles à partir de 70 %, 2 à partir de 40 %).

### Les éléments

| Élément | Spécificité | Évolution visuelle |
|---|---|---|
| **Eau** | Dégâts de zone, rayon qui grandit avec le niveau | goutte → bulle → averse → cyclone → tsunami |
| **Feu** | Dégâts qui augmentent fortement à chaque fusion | braise → boule de feu → flamme bleue → aura dorée |
| **Terre** | Une salve de projectiles par tir (autant que son niveau), concentrée sur une cible | caillou → ambre → émeraude → cristaux dorés |
| **Foudre** | Tir instantané et cadence rapide | éclair violet → ramifications → double éclair doré |
| **Poison** *(exclusif)* | Faibles dégâts directs, empoisonne 3 s (dégâts sur la durée) | boule toxique → épines en orbite |
| **Air** *(exclusif)* | Rafale qui repousse l'ennemi vers la forêt | croissants de vent qui tournent |
| **Invocateur** *(exclusif)* | Invoque des **chevaliers** devant le château (1 à 3 selon le niveau) : ils se placent face au gobelin le plus proche, le bloquent et le combattent à l'épée. Les gobelins doivent les abattre avant d'avancer | chevalier d'argent au plumet magenta |

Les unités exclusives s'achètent au **Marché** avec des gemmes (Poison 60, Air 100, Invocateur 160).

### Compétences (niveau 5)

Un mage de niveau 5 a une **compétence active** : touchez-le (sans le glisser) pour la lancer. Un anneau autour de lui montre la recharge (≈ 20 s), une étoile dorée qu'elle est prête.

| Élément | Compétence |
|---|---|
| Eau | **Raz-de-marée** : repousse, ralentit et blesse tous les gobelins |
| Feu | **Pluie de feu** : 8 boules de feu tombent du ciel |
| Terre | **Séisme** : étourdit et blesse tous les gobelins |
| Foudre | **Tempête** : 10 éclairs sur le terrain |
| Poison | **Peste** : empoisonne tous les gobelins |
| Air | **Tornade** : renvoie tous les gobelins vers la forêt |
| Invocateur | **Appel aux armes** : 3 chevaliers d'élite (PV doublés) |

### Ultimes

La jauge se remplit en tuant des gobelins ; **secouer le téléphone** (ou le mode AUTO) lance l'ultime **équipé** sur la page Château.

| Ultime | Effet | Jauge | Prix |
|---|---|---|---|
| Onde de choc | tous les gobelins étourdis 4 s | 20 | offert |
| Rage arcanique | dégâts des mages ×2 pendant 8 s | 25 | 120 gemmes |
| Météorite géante | une météorite s'écrase : 70 % des PV de chaque gobelin (20 % pour le boss) | 30 | 220 gemmes |
| Bouclier divin | château invincible 10 s | 25 | 160 gemmes |

### Artefacts

21 artefacts, 3 par mage. Battre un **boss de la campagne** (niveaux 10, 20, 30…) pour la première fois en donne un au hasard (pour un mage possédé en priorité ; 20 gemmes s'ils sont tous obtenus). Chaque mage en équipe un (bandeau de sa ligne, page Mages) : il le spécialise avec un avantage et un inconvénient, par exemple *Baguette de corail* (Eau, cadence bien plus rapide, −30 % de dégâts), *Cœur de volcan* (Feu, +70 % de dégâts, tire moins vite), *Fronde de granit* (Terre, un projectile de plus), *Grimoire incandescent* (compétence deux fois plus souvent), *Bannière royale* (Invocateur, un chevalier de plus)…

### Équipe

On emmène **4 mages** en partie : sur la page Mages, toucher le portrait d'un mage l'ajoute à l'équipe ou l'en retire (cadre vert « ÉQUIPE »). La barre de recrutement affiche les mages de l'équipe.

### Navigation : 5 pages en glissant

L'accueil se parcourt en **glissant le doigt** vers la gauche ou la droite (ou avec la barre d'onglets en bas) :

**Marché · Mages · Campagne · Infini · Château** (le jeu s'ouvre sur la Campagne, au centre).

- **Marché** (liste qui défile) : coffres (10 gemmes), unités exclusives, ultimes, vitesse ×3 (300 gemmes).
- **Mages** (liste qui défile) : ouvrir les coffres **×1, ×5 ou ×10**, monter les rangs, composer l'équipe, changer d'artefact.
- **Campagne** : la carte des niveaux et JOUER.
- **Infini** : choix de la difficulté et records.
- **Château** (liste qui défile) : améliorations permanentes du château avec des gemmes (**PV max** +10 % par niveau, jusqu'à 20 ; **résistance** −3 % de dégâts subis par niveau, jusqu'à 15), choix de l'ultime équipé, et **succès**.

### Succès

22 succès (niveaux, gobelins tués, fusions, réactions, boss, mode infini, coffres, étoiles, artefacts, rangs, compétences, chevaliers…). Chacun donne des **gemmes** dès qu'il est accompli, avec un bandeau en haut de l'écran. La liste et la progression sont sur la page Château.

### Réactions élémentaires

Chaque coup laisse une **marque** de son élément sur l'ennemi pendant 3 s (point de couleur à côté de sa barre de vie ; l'esprit de l'Invocateur laisse une marque *arcanique*). **N'importe quel autre élément** déclenche alors une réaction : les 21 paires des 7 éléments ont chacune la leur.

| Réaction | Éléments | Effet |
|---|---|---|
| **Vapeur** | Eau + Feu | nuage qui ralentit de moitié et blesse légèrement |
| **Boue** | Eau + Terre | grande zone qui ralentit fortement |
| **Électrocution** | Eau + Foudre | dégâts doublés, étourdissement, arcs vers 3 ennemis proches |
| **Marécage** | Eau + Poison | zone qui empoisonne ceux qui la traversent |
| **Gel** | Eau + Air | gel de zone (≈ 1,2 s) |
| **Geyser** | Eau + Invocateur | la cible est projetée vers la forêt, éclaboussures autour |
| **Lave** | Feu + Terre | flaque de lave qui brûle pendant 4 s |
| **Surcharge** | Feu + Foudre | explosion qui repousse |
| **Explosion toxique** | Feu + Poison | explosion de zone (2 × le coup) |
| **Embrasement** | Feu + Air | anneau de flammes qui brûle |
| **Feux follets** | Feu + Invocateur | 3 boules de feu chassent les ennemis proches |
| **Cristallisation** | Terre + Foudre | grosse frappe, long étourdissement |
| **Acide** | Terre + Poison | dégâts doublés, cible **vulnérable** 4 s |
| **Tempête de sable** | Terre + Air | recul et étourdissement de zone |
| **Éboulement** | Terre + Invocateur | rochers de zone, court étourdissement |
| **Paralysie** | Foudre + Poison | étourdissement, le poison redouble |
| **Orage** | Foudre + Air | la foudre frappe 5 ennemis du terrain |
| **Chaîne spectrale** | Foudre + Invocateur | arc qui rebondit sur 5 ennemis |
| **Nuage toxique** | Poison + Air | le poison se propage aux ennemis proches |
| **Malédiction** | Poison + Invocateur | ennemis proches **vulnérables** |
| **Vortex** | Air + Invocateur | aspire les ennemis vers la cible et les étourdit |

Un ennemi **vulnérable** (liseré orange autour de sa barre de vie) subit 50 % de dégâts en plus.

### Progression

Des niveaux **sans fin**, débloqués l'un après l'autre sur une carte affichée par pages de 6 (flèches ◀ ▶). Les meilleures étoiles de chaque niveau sont sauvegardées.

- **Niveaux 1 à 6** : faits à la main (niveau 1 : 3 vagues, niveau 2 : 4 vagues…).
- **Niveaux 7 et +** : générés automatiquement. Chaque niveau est toujours le même (il dépend de son numéro) et a un profil : **Horde** (beaucoup de gobelins rapides), **Siège** (beaucoup de costauds) ou **Mêlée**. La vitesse et les dégâts des gobelins sont plafonnés ; seuls leurs PV continuent de grimper : il faut améliorer ses mages pour avancer.

| Ennemi | Vitesse | PV | Dégâts au château | Particularité |
|---|---|---|---|---|
| Gobelin | ×1 | ×1 | ×1 | vert |
| Rapide | ×1,5 | ×0,6 | ×1 | petit, orangé |
| Costaud | ×0,7 | ×2,5 | ×1,5 | gros, rouge sombre |
| Chaman (invocateur) | ×0,8 | ×1,6 | ×1 | violet, invoque 2 diablotins toutes les ≈ 3 s |
| Diablotin | ×1,6 | ×0,35 | ×0,4 | minuscule, rouge, invoqué |
| **Chef gobelin (boss)** | ×0,45 | ×12 | ×3 (max 60 % des PV) | couronne, grande barre de vie, invoque 3 diablotins, résiste au recul et à l'étourdissement |

**Boss** : à la dernière vague des niveaux 10, 20, 30… et toutes les 10 vagues en mode infini (bandeau « UN BOSS APPROCHE ! »). Les chamans apparaissent dans les niveaux générés et à partir de la vague 7 du mode infini.

### Biomes

L'accueil change de décor à chaque page de la carte : **Prairie** (1-6), **Désert** (7-12), **Toundra** (13-18), **Marais** (19-24), **Volcan** (25-30), **Crépuscule** (31-36), puis le cycle recommence. Chaque biome a son ciel, ses collines, son décor (arbres, cactus, sapins enneigés, roseaux, rochers et lave, cristaux) et ses particules animées (lucioles, poussière, neige, braises). Le terrain de jeu prend la teinte du biome ; en mode infini, le biome change toutes les 10 vagues.

### Nuit dans la campagne et cycle jour / nuit

- Certains niveaux générés de la campagne se jouent **de nuit** (7, 11, 15, 19… : lune sur la carte).
- En mode infini, **une vague sur trois** tombe de nuit (3, 6, 9…).
- Le capteur de luminosité peut toujours faire tomber la nuit à tout moment.

### Mode infini

Page **Infini** : les vagues s'enchaînent, de plus en plus fortes et variées, jusqu'à la chute du château. Quatre difficultés, chacune avec son record :

| Difficulté | Gobelins | Coffres | Bonus |
|---|---|---|---|
| Facile | 60 % | 1 toutes les 5 vagues | — |
| Normal | 100 % | 1 toutes les 3 vagues | — |
| Difficile | 150 %, plus rapides | 1 toutes les 3 vagues | +1 gemme par coffre |
| Infernal | 220 %, encore plus rapides | 1 toutes les 2 vagues | +2 gemmes par coffre |

### Coffres et améliorations permanentes

- **Coffres** : 1 coffre par niveau terminé (+1 tous les 4 niveaux : 2 coffres à partir du niveau 5, 3 à partir du 9…). En mode infini, selon la difficulté (voir ci-dessus), même en abandonnant la partie. Ils s'achètent aussi au Marché (10 gemmes) et s'ouvrent par 1, 5 ou 10 (le butin est additionné).
- **Ouverture carte par carte** : le coffre (pixel art) tombe puis attend. Chaque toucher fait sortir **une** carte : elle jaillit du coffre de dos, se retourne, s'affiche en grand avec des rayons de sa couleur et son nombre qui monte. Une pastille indique les cartes restantes ; les gemmes sortent en dernier. Un récapitulatif « BUTIN » clôt l'ouverture, avec « OUVRIR ENCORE » s'il reste des coffres.
- **Contenu** : 3 lots de 4 à 8 fragments (chacun d'un élément possédé tiré au hasard) et 1 à 3 **gemmes**.
- **Rangs** (écran **MAGES**) : chaque élément a un rang permanent, de 1 à 999. Passer du rang *n* au rang *n + 1* coûte **10 × n fragments** de cet élément. Chaque rang donne **+10 % de dégâts** et **+1 % de cadence** (plafonnée à +30 %) à tous les mages de cet élément, dans toutes les parties. L'apparence du mage sur l'écran MAGES évolue tous les 10 rangs.

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
| **Capteur de luminosité** | **Cacher le capteur** (ou jouer dans une pièce sombre) fait tomber la nuit : gobelins 30 % plus rapides, torches, yeux rouges, mais +20 % d'or | Seuils relatifs à la lumière de la pièce, assouplis : la nuit tombe sous 50 % de la lumière habituelle (ou sous 10 lux) et le jour revient au-dessus de 70 % ; confirmation de 400 ms pour éviter le clignotement ; désactivable dans les options (les niveaux de nuit restent de nuit) |

Les capteurs sont coupés quand l'application passe en arrière-plan.

### Sauvegarde

- Tout est sauvegardé dès que ça change (achats, coffres, rangs, succès) et quand l'application passe en arrière-plan.
- **Partie en cours** : si l'application est quittée (ou fermée par le téléphone) en plein niveau, la partie est sauvegardée. Au prochain lancement, elle reprend **en pause, au début de la vague en cours**, avec les mages, les PO, les PV et les cases débloquées.
- **Mises à jour** : installer un nouvel APK par-dessus l'ancien garde la sauvegarde (même clé de signature, voir « Télécharger et jouer »). Les données ajoutées par une mise à jour ont toutes une valeur par défaut : une ancienne sauvegarde se charge sans problème.

### Style de l'interface
Toute l'interface reprend la direction artistique pixel art médiéval-fantasy du décor (classe `UiKit`) : boutons en **tuiles en relief** qui s'enfoncent à l'appui, boutons ronds en **médaillons dorés**, fenêtres en **planches de bois cloutées** avec un **ruban** de titre, listes en **alcôves creusées**, barres encadrées, texte gras à contour sombre, curseurs en pièces d'or, bulle de la carte en parchemin.

### Retours
- **Visuels** : feedback d'appui sur tous les boutons, flash rouge du rempart, bandeau jour/nuit, étoiles animées à la victoire.
- **Sonores** : bruitages (recrutement, fusion, élimination, château touché, ultime, victoire, défaite, clic) et musique de fond médiévale en boucle, désactivable.
- **Haptiques** : vibrations à la fusion, quand le château est touché, pendant la secousse et au déclenchement de l'ultime.

### Options
- Désactiver le mode nuit (Non / Oui)
- Musique (Non / Oui)
- **Mode gaucher** (Non / Oui) : interface en miroir pour jouer d'une main avec la main gauche (pause, options et son à gauche, jauge d'ultime à droite, ordre des boutons de recrutement inversé)
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
│   ├── GameState          START (5 pages), PLAYING, PAUSED, OPTIONS, VICTORY, GAME_OVER, CHEST_OPENING
│   ├── Board              Géométrie : colonne de jeu, damier, adaptation à l'écran
│   ├── LevelSpec          Description d'un niveau : faits main (1-6), générés (7+), mode infini, boss, biomes
│   ├── Progression        Progression permanente : coffres, fragments, rangs, gemmes, unités, équipe, record
│   ├── Ultimate           Les 4 ultimes (prix, jauge, effets)
│   ├── Artifact           Les 21 artefacts (3 par mage) et leurs modificateurs
│   ├── Achievement        Les succès (statistique suivie, seuil, gemmes)
│   ├── Knight             Chevaliers invoqués par l'Invocateur
│   ├── Reaction, Zone     Réactions élémentaires et zones au sol (lave, vapeur)
│   ├── Mage, Enemy        Unités (caractéristiques par élément, niveau, rang ; types de gobelins)
│   └── Projectile, Bolt, Splash, HitEffect   Attaques et effets
├── view/                  Affichage et retours
│   ├── GameView           Vue Android : boucle de jeu (~60 images/s), relie tout
│   ├── GameRenderer       Dessin des écrans, du HUD et des unités
│   ├── AttackRenderer     Dessin des attaques selon l'élément et le niveau
│   ├── TerrainRenderer    Décor pixel art généré (forêt, prairie, rempart, cour)
│   ├── Sprites            Mages et gobelins en pixel art
│   ├── Hud                Position de tous les boutons et fenêtres (miroir en mode gaucher)
│   ├── MenuList           Contenu des listes qui défilent (Marché, Mages, Château), partagé dessin / touchers
│   ├── UiKit              Style de l'interface : tuiles, médaillons, fenêtres en bois, rubans
│   ├── Palette            Couleurs
│   ├── SoundManager       Bruitages (SoundPool)
│   ├── MusicPlayer        Musique de fond (MediaPlayer)
│   └── Haptics            Vibrations
├── controller/            Entrées du joueur
│   ├── TouchController    Touchers, glisser-déposer, glissement entre pages, défilement, boutons
│   ├── ShakeDetector      Accéléromètre → ultime
│   └── LightSensor        Luminosité → mode nuit
└── data/
    └── SaveManager        Sauvegarde (SharedPreferences) : progression, options, partie en cours à reprendre
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

Graphismes (pixel art), bruitages et musique créés pour le projet, générés par code : aucune ressource externe. L'icône de l'application (chapeau de mage sur ciel de nuit) est une icône adaptative vectorielle.
