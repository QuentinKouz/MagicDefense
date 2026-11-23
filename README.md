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

**Version 2 (branche `v2`)** : l'APK est directement dans le dépôt, [`apk/MagicDefense-v2.apk`](https://github.com/QuentinKouz/MagicDefense/raw/v2/apk/MagicDefense-v2.apk) (version 2.3).  
**Mise à jour sans perte** : installez le nouvel APK par-dessus l'ancien, sans désinstaller. Tous les APK de la branche `v2` sont signés avec la même clé (`app/signing/magicdefense.keystore`), donc Android accepte la mise à jour et garde la sauvegarde (progression, coffres, gemmes, partie en cours).  
Seule exception : passer de la V1 (Releases GitHub) à la V2 demande encore de désinstaller la V1, signée avec une autre clé.

**Mise à jour automatique** (à partir de la 2.2) : au lancement, le jeu lit `apk/version.json` sur la branche `v2` du dépôt GitHub. Si son `versionCode` est plus grand que celui installé, il propose de télécharger `apk/MagicDefense-v2.apk` puis ouvre l'installateur d'Android (il faut confirmer ; Android peut demander d'autoriser l'installation depuis l'application). La progression est conservée, car l'APK est toujours signé avec la même clé. Sans réseau, le jeu démarre normalement.

1. Ouvrez le lien ci-dessus **depuis votre téléphone Android** (ou transférez-y le fichier).
2. Ouvrez `MagicDefense.apk` une fois téléchargé.
3. Android demande d'autoriser l'installation depuis cette source (navigateur, gestionnaire de fichiers…) : acceptez, puis **Installer**.
4. Lancez **Magic Defense** depuis l'écran d'accueil.

> Android uniquement (pas d'iPhone).  
> L'application n'étant pas publiée sur le Play Store, Play Protect peut afficher un avertissement : choisissez « Installer quand même ».

---

## Le jeu

Les gobelins sortent de la forêt et marchent vers le rempart. Vos mages, placés sur un damier de 4 × 3 cases devant le château, les attaquent automatiquement.

- **Se préparer** : au début du niveau, rien ne bouge tant que vous n'avez pas touché **LANCER LA VAGUE** : placez et fusionnez vos mages tranquillement (une partie reprise après la fermeture de l'appli attend aussi votre signal).
- **Recruter** : la barre du bas contient les 4 mages de l'équipe, puis le **dé**. Le prix d'un mage dépend de sa **rareté**, et augmente à chaque mage acheté dans la partie (compteur commun à tous les mages) : Commune 10 PO (+2), Rare 14 PO (+3), Épique 18 PO (+4), Légendaire 25 PO (+6). Le dé place un mage **au hasard** de l'équipe pour 7 PO, toujours au même prix (il ne fait pas monter le prix des autres).
- **Cases bloquées** : la rangée du fond du damier (4 cases) est enchaînée au début de chaque partie. Touchez une case pour la débloquer avec des PO (30, puis 50, 80, 120).
- **Soigner** : le bouton **+** (sous la jauge) rend 25 % des PV max du château, sans dépasser le maximum. 3 soins par partie (50, 75, puis 100 PO). **Une étoile perdue ne revient pas** : se soigner ne rend pas les étoiles.
- **Vitesse ×1 / ×2 / ×3** : bouton sous le son. ×2 (250 gemmes) puis ×3 (600 gemmes) s'achètent une fois pour toutes au Marché. **Chaque combat commence en ×1** ; le bouton passe ensuite à ×2 puis ×3.
- **AUTO** : le mode automatique (400 gemmes au Marché) lance seul l'ultime et les compétences dès qu'ils sont prêts. Grisé tant qu'il n'est pas acheté.
- **Fusionner** : glissez un mage sur un autre **du même élément et du même niveau** → il passe au niveau supérieur (**niveau 7 maximum** ; le niveau 7, le **sage**, demande une orbe de sagesse posée sur cet élément). Glisser sur une case vide déplace le mage.
- **Monter de niveau avec des PO** : **appui long** (≈ 0,4 s sans bouger) sur un mage → une bulle « ↑ NIV. n+1 · prix » s'ouvre au-dessus de sa case ; la toucher achète le niveau (elle reste ouverte pour enchaîner). Prix : 100 PO (1→2), 200, 400, 800, 1600, puis 3200 PO (6→7, orbe requise). Toucher ailleurs ferme la bulle.
- **Gagner de l'or** : chaque gobelin tué rapporte des pièces d'or (une pièce s'envole vers le compteur).
- **Protéger le château** : il a 10 000 PV (plus avec les améliorations de la page Château). Les étoiles dépendent des PV : 3 étoiles à partir de 70 %, 2 à partir de 40 %.
- **Victoire** : NIVEAU SUIVANT, **REJOUER** (le même niveau) ou MENU.

### Les mages et leurs raretés

Les mages sont des **cartes** à collectionner : on les débloque et on les améliore avec les cartes trouvées dans les coffres (plus d'achat de mage avec des gemmes).

| Mage | Rareté | Spécificité |
|---|---|---|
| **Feu** | Commune | Dégâts qui augmentent fortement à chaque fusion |
| **Terre** | Commune | Une salve de projectiles par tir (autant que son niveau) |
| **Eau** | Rare | Dégâts de zone, rayon qui grandit avec le niveau |
| **Poison** | Rare | Faibles dégâts directs, empoisonne 3 s |
| **Foudre** | Épique | Tir instantané et cadence rapide |
| **Air** | Épique | Rafale qui repousse l'ennemi vers la forêt |
| **Invocateur** | Épique | Invoque des **chevaliers** (1 à 5), grands comme une case, qui **foncent sur les gobelins** et arrêtent ceux de leur colonne (les rapides et les diablotins se faufilent) ; sans cible, ils reviennent en garde devant le château |
| **Ouvrier** | Épique | Construit un **mur** devant sa colonne (il bloque les gobelins et se reconstruit 5 s après sa destruction) et lance lentement de gros **marteaux** |
| **Lumière** | Légendaire | **Aura sur tous les mages** : +10 à +38 % de dégâts et tirs/compétences plus rapides (2 auras cumulables, +60 % max) ; ses **rayons** touchent tous les ennemis à la fois |
| **Ténèbres** | Légendaire | **Tue net** les gobelins sous 10 % de PV (pas le boss), dégâts de zone plus grands que l'Eau, **ralentit** les ennemis touchés |
| **Scientifique** | Légendaire | Lance des **fioles** de potion (éclaboussure de zone) et, toutes les ≈ 25 s (jauge sur sa case), largue une **bombe nucléaire** au milieu de la carte qui frappe **tous** les gobelins visibles (énormes dégâts, ×0,35 sur le boss, court étourdissement) |

Le Feu, la Terre, l'Eau et la Foudre sont possédés dès le départ ; les autres se débloquent dans les coffres.

### Archimage (niveau 6) et Sage (niveau 7)

- Les niveaux 6 et 7 ont leur **propre apparence**, dans le thème de l'élément : Poséidon (couronne, trident) pour l'Eau, couronne de flammes et phénix pour le Feu, golem à runes pour la Terre, casque ailé et marteau de Thor pour la Foudre, médecin de peste pour le Poison, moine du vent pour l'Air, roi nécromancien pour l'Invocateur, forgeron nain pour l'Ouvrier, séraphin pour la Lumière, faucheuse pour les Ténèbres, casque anti-radiations et fiole géante pour le Scientifique.
- Niveau 6 ≈ ×1,5 les dégâts du niveau 5 (tir 8 % plus rapide) ; niveau 7 ≈ ×2,4 (tir 20 % plus rapide), zones, poison, recul et aura plus forts.
- Le **sage** (niveau 7) a une longue barbe, des ornements dorés, une aura, et **le contour de sa case devient doré**. Sa compétence passe en version **BOOST** (voir ci-dessous).

### Orbe de sagesse

- Objet **commun à tous les mages** (comme une méga-gemme) : il faut en poser une sur un élément pour que ses mages puissent devenir des sages (niveau 7).
- **Obtention** : 1 orbe en repoussant la **vague 1000 du mode infini**, une fois par difficulté (Facile, Normal, Difficile, Infernal → 4 orbes). La page Infini montre une mini-orbe par difficulté (brillante si obtenue).
- **Pose** : bouton **ORBE** dans la fiche du mage (page Mages) ; l'orbe quitte la réserve (compteur en haut de la page Mages). On peut la retirer pour la remettre en réserve.

### Compétences (niveau 5)

Un mage de niveau 5 a une **compétence active** : touchez-le (sans le glisser) pour la lancer. Un anneau autour de lui montre la recharge (≈ 20 s), une étoile dorée qu'elle est prête.

| Mage | Compétence (niveau 5) | Version BOOST du sage (niveau 7) |
|---|---|---|
| Eau | **Raz-de-marée** : repousse, ralentit et blesse tous les gobelins | **Déluge** |
| Feu | **Pluie de feu** : 8 boules de feu tombent du ciel | **Envol du phénix** (16 boules) |
| Terre | **Séisme** : étourdit et blesse tous les gobelins | **Cataclysme** |
| Foudre | **Tempête** : 10 éclairs sur le terrain | **Marteau du tonnerre** (20 éclairs) |
| Poison | **Peste** : empoisonne tous les gobelins | **Pandémie** |
| Air | **Tornade** : renvoie tous les gobelins vers la forêt | **Ouragan** (étourdit en plus) |
| Invocateur | **Appel aux armes** : 3 chevaliers d'élite (PV doublés) | **Légion royale** (5 chevaliers, PV triplés) |
| Ouvrier | **Grand rempart** : un mur renforcé devant chaque colonne pendant 15 s | **Citadelle** |
| Lumière | **Aube sacrée** : brûle et étourdit tous les gobelins, recharge à moitié les compétences des autres mages | **Jugement céleste** (recharge les autres à 75 %) |
| Ténèbres | **Éclipse** : exécute les gobelins sous 25 % de PV, blesse et ralentit fortement les autres | **Nuit éternelle** (exécute sous 40 %) |
| Scientifique | **Réaction en chaîne** : la bombe se recharge 2× plus vite pendant 10 s (sirène d'alerte et compte à rebours) | **Apocalypse nucléaire** (3× plus vite pendant 15 s) |

**BOOST** : au niveau 7, la compétence devient sa forme renforcée (effets ≈ ×2, recharge −40 %), avec flash doré et anneau de recharge doré.

### Ultimes

La jauge se remplit en tuant des gobelins ; **toucher la jauge** (en haut à gauche) lance l'ultime **équipé** sur la page Château (ou le mode AUTO). Après chaque ultime, **60 s de recharge** s'ajoutent à la jauge (voile et secondes restantes affichés) : impossible de les enchaîner.

| Ultime | Effet | Jauge | Prix |
|---|---|---|---|
| Onde de choc | tous les gobelins étourdis 4 s | 20 | offert |
| Rage arcanique | dégâts des mages ×2 pendant 8 s | 25 | 250 gemmes |
| Météorite géante | 70 % des PV de chaque gobelin (20 % pour le boss) | 30 | 450 gemmes |
| Bouclier divin | château invincible 10 s | 25 | 350 gemmes |

### Artefacts

33 artefacts, 3 par mage. Chaque victoire **en 3 étoiles sur un niveau de boss** de la campagne (10, 20, 30…) donne **10 % de chances** d'en obtenir un, à chaque fois (pas seulement la première). S'ils sont tous obtenus : 20 gemmes. Chaque mage en équipe un (fiche du mage, page Mages) : il le spécialise avec un avantage et un inconvénient. Chaque fiche donne l'effet chiffré, par exemple *Baguette de corail* (Eau) : « Tire 67 % plus vite, mais −30 % de dégâts » ; *Cœur de volcan* (Feu) : « +70 % de dégâts, mais tire 29 % moins vite » ; *Fronde de granit* (Terre) : « 1 projectile de plus par salve, mais −25 % de dégâts »…

### Page Mages (façon jeu de cartes)

- **Équipe** : les 4 mages emmenés en partie, en grille **2 × 2**.
- **Collection** : les autres mages débloqués, par rangées de 3, triés par rareté. Chaque carte montre son **rang** et une barre de cartes vers le rang suivant (verte avec ↑ quand l'amélioration est possible).
- **À débloquer** : les mages pas encore trouvés, assombris avec un cadenas (« À débloquer »), impossibles à mettre dans l'équipe.
- **Toucher une carte** ouvre sa **fiche** : rareté, bonus du rang, cartes, compétence, artefact (toucher pour changer), et deux boutons :
  - **AMÉLIORER** : passer du rang *n* au rang *n + 1* coûte des **cartes** (10 × n pour une commune, 6 × n rare, 3 × n épique, 1 × n légendaire) **et des gemmes** (2, 3, 5 ou 8 × n). Chaque rang donne +10 % de dégâts et +1 % de cadence (plafonnée à +30 %) à tous les mages de cet élément ;
  - **UTILISER** : le mage rejoint l'équipe. Si elle est pleine, les cartes de l'équipe se mettent à bouger : touchez celle à **remplacer**.

### Coffres

Les coffres se trouvent au **Marché**, avec leurs **probabilités affichées**. Chaque coffre contient plusieurs **types** de cartes (des mages différents) ; les gemmes sont en plus.

| Coffre | Prix | Cartes | Types | Gemmes | Rareté de chaque type |
|---|---|---|---|---|---|
| **Petit coffre** | 35 | 22 (± 2) | 2 | 1 à 3 | 1re : commune · 2e : 50 % commune, 25 % rare, 25 % aucune |
| **Grand coffre** | 75 | 42 (± 4) | 3 | 3 à 5 | 1re : commune · 2e : 30 % C, 60 % R, 9,5 % É, 0,5 % L · 3e : 80 % R, 19 % É, 1 % L |
| **Coffre mystérieux** | 160 | 78 (± 6) | 4 | 7 à 10 | 1re : 80 % C, 20 % R · 2e : 80 % R, 20 % É · 3e : 65 % R, 30 % É, 5 % L · 4e : 75 % É, 25 % L |

- Pour la 3e carte du coffre mystérieux, les probabilités demandées (55 % + 30 % + 5 %) ne faisaient que 90 % : les 10 % restants vont à la rare.
- Les cartes sont réparties selon la rareté (beaucoup de communes, peu de légendaires). Une carte d'un mage pas encore possédé le **débloque** (« NOUVEAU MAGE ! ») ; il rejoint l'équipe s'il reste une place.
- **Achat** ×1, ×5 ou ×10, **seulement quand on n'a plus aucun coffre de cette sorte** ; sinon la ligne propose **OUVRIR** ou **TOUT OUVRIR**.
- **2 petits coffres gratuits par jour** (bouton PRENDRE en haut du Marché ; pastille « ! » sur l'onglet).
- **Ouverture carte par carte** : le coffre tombe puis attend ; chaque toucher fait sortir une carte, de dos, qui se retourne avec des rayons et sa **rareté**. Les gemmes sortent d'abord, puis les cartes **de la rareté la plus basse à la plus haute** : la plus rare arrive en dernier. Un récapitulatif « BUTIN » clôt l'ouverture (« OUVRIR ENCORE » s'il reste des coffres de cette sorte).
- **Gagner des coffres** : en campagne, 1 petit coffre par niveau terminé (+1 tous les 4 niveaux) et un **grand coffre** en plus sur les niveaux de boss ; en mode infini, voir ci-dessous.

### Navigation : 5 pages en glissant

L'accueil se parcourt en **glissant le doigt** vers la gauche ou la droite (ou avec la barre d'onglets en bas) :

**Marché · Mages · Campagne · Infini · Château** (le jeu s'ouvre sur la Campagne, au centre).

- **Marché** : coffres gratuits, les 3 coffres, ultimes, vitesses ×2 et ×3, mode automatique.
- **Mages** : équipe, collection, cartes à débloquer, fiches.
- **Campagne** : la carte des niveaux et JOUER.
- **Infini** : choix de la difficulté, records et récompenses.
- **Château** : améliorations permanentes avec des gemmes (**PV max** +10 % par niveau, jusqu'à 20, 25 × niveau gemmes ; **résistance** −3 % de dégâts subis par niveau, jusqu'à 15, 30 × niveau gemmes ; **Trésor de départ** +5 PO au début de chaque partie par niveau, jusqu'à 10, 35 × niveau gemmes, soit 35 à 350), ultime équipé, **succès**.

### Succès

Des **succès à paliers sans fin** : quand un palier est atteint, le suivant apparaît, numéroté en chiffres romains (*Chasseur de gobelins I*, *II*, *III*…). Familles : niveaux terminés, gobelins tués (500, 1000, 2500, 5000, 10 000, 25 000…), fusions, réactions, boss, vagues du mode infini, vagues en Infernal, coffres ouverts, niveaux à 3 étoiles, rang, compétences, chevaliers. Trois succès restent uniques : *Grand mage* (mage de niveau 5), *Collectionneur* (tous les mages), *Archéologue* (5 artefacts). Chaque palier donne des **gemmes** (de plus en plus), avec un bandeau en haut de l'écran. La page Château montre le palier en cours de chaque famille avec une barre de progression. Les anciennes sauvegardes sont converties sans perte ni double récompense.

### Réactions élémentaires

Chaque coup laisse une **marque** de son élément sur l'ennemi pendant 3 s (point de couleur à côté de sa barre de vie). **N'importe quel autre élément** déclenche alors une réaction : les 55 paires des 11 mages ont chacune la leur, **sauf Lumière + Ténèbres** (aucune réaction), soit 54 réactions.

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
| **Béton** | Ouvrier + Eau | béton frais qui englue presque sur place |
| **Forge** | Ouvrier + Feu | coup de forge, cible vulnérable |
| **Fondations** | Ouvrier + Terre | onde qui étourdit, murs réparés |
| **Marteau-pilon** | Ouvrier + Foudre | 3 frappes sur la cible, étourdissement |
| **Goudron** | Ouvrier + Poison | goudron qui ralentit et empoisonne |
| **Catapulte** | Ouvrier + Air | la cible est catapultée et retombe en zone |
| **Barricade** | Ouvrier + Invocateur | barricade temporaire devant la cible |
| **Sanctuaire** | Ouvrier + Lumière | murs et chevaliers soignés |
| **Démolition** | Ouvrier + Ténèbres | grosse explosion qui repousse |
| **Arc-en-ciel** | Lumière + Eau | ralentit et rend vulnérable |
| **Supernova** | Lumière + Feu | explosion de lumière brûlante |
| **Prisme** | Lumière + Terre | rayons réfractés vers 4 ennemis |
| **Jugement** | Lumière + Foudre | colonne de lumière, étourdissement |
| **Purification** | Lumière + Poison | le poison restant explose d'un coup |
| **Aurore** | Lumière + Air | aveugle (étourdit) et repousse |
| **Croisade** | Lumière + Invocateur | un paladin rejoint la garde |
| **Abysses** | Ténèbres + Eau | abysse qui ralentit et ronge |
| **Flammes noires** | Ténèbres + Feu | gros dégâts sur la durée |
| **Tombeau** | Ténèbres + Terre | long étourdissement, cible vulnérable |
| **Éclair noir** | Ténèbres + Foudre | chaîne qui exécute les blessés |
| **Nécrose** | Ténèbres + Poison | poison violent et vulnérabilité de zone |
| **Trou noir** | Ténèbres + Air | aspire fort et broie |
| **Moisson** | Ténèbres + Invocateur | moisson des âmes : exécution de zone des blessés |
| **Solution saline** | Scientifique + Eau | ralentit fortement et rend vulnérable |
| **Combustion** | Scientifique + Feu | explosion de zone et flammes au sol |
| **Calcination** | Scientifique + Terre | frappe de zone, étourdissement |
| **Électrolyse** | Scientifique + Foudre | arcs vers 4 ennemis, tous vulnérables |
| **Toxine mutante** | Scientifique + Poison | poison violent de zone |
| **Aérosol** | Scientifique + Air | nuage qui ralentit et empoisonne |
| **Clonage** | Scientifique + Invocateur | un chevalier clone rejoint la garde |
| **Dynamite** | Scientifique + Ouvrier | grosse explosion qui repousse |
| **Fission** | Scientifique + Lumière | gros coup sur la cible, puis 2 fragments |
| **Antimatière** | Scientifique + Ténèbres | gros coup, exécute les blessés autour (sous 15 %, pas le boss) |

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
| Chaman (invocateur) | ×0,8 | ×1,6 | ×1 | capuche bordeaux, plumes, bâton à crâne, cercle d'invocation vert ; invoque 2 diablotins toutes les ≈ 3 s |
| Diablotin | ×1,6 | ×0,35 | ×0,4 | minuscule, rouge, invoqué |
| **Gobelin mage** | ×0,9 | ×1,3 | tir ×0,25 toutes les 2,5 s | chapeau pointu, orbe ; **s'arrête à la sortie de la forêt** et lance des projectiles magiques sur le rempart (ou sur le mur de l'Ouvrier de sa colonne) ; revient à sa place s'il est repoussé |
| **Chef gobelin (boss)** | ×0,45 | ×12 | ×3 (max 60 % des PV) | couronne, grande barre de vie, invoque 3 diablotins, résiste au recul et à l'étourdissement |

**Boss** : à la dernière vague des niveaux 10, 20, 30… et toutes les 10 vagues en mode infini (bandeau « UN BOSS APPROCHE ! »). Les chamans apparaissent dans les niveaux générés et à partir de la vague 7 du mode infini ; les gobelins mages à partir du niveau 12 et de la vague 12 du mode infini (jusqu'à 10 % des gobelins).

### Biomes

L'accueil change de décor à chaque page de la carte : **Prairie** (1-6), **Désert** (7-12), **Toundra** (13-18), **Marais** (19-24), **Volcan** (25-30), **Crépuscule** (31-36), puis le cycle recommence, numéroté en chiffres romains : *Prairie I* au premier passage, *Prairie II* au deuxième (niveaux 37-42), *Volcan III*… (en mode infini, le numéro augmente toutes les 60 vagues). Chaque biome a son ciel, ses collines, son décor et ses particules animées. Le terrain de jeu prend la teinte du biome ; en mode infini, le biome change toutes les 10 vagues.

### Nuit dans la campagne et cycle jour / nuit

- Certains niveaux générés de la campagne se jouent **de nuit** (7, 11, 15, 19… : lune sur la carte).
- En mode infini, **une vague sur trois** tombe de nuit (3, 6, 9…).
- La nuit ne vient **que** de ces niveaux et de ces vagues (le capteur de luminosité a été retiré) : gobelins plus rapides, torches, yeux rouges, mais plus d'or.
- Musique dédiée la nuit (voir « Retours »).

### Mode infini

Page **Infini** : les vagues s'enchaînent, de plus en plus fortes et variées, jusqu'à la chute du château. Quatre difficultés, chacune avec son record. Les coffres sont donnés selon les vagues repoussées, même en abandonnant la partie :

| Difficulté | Gobelins | Coffres |
|---|---|---|
| Facile | 60 % | 1 petit coffre aux vagues 5, 15, 25… et 1 grand coffre aux vagues 10, 20, 30… |
| Normal | 100 % | 1 coffre toutes les 3 vagues, par cycles de 15 : petits, sauf les 4e, 8e et 12e (grands) et le 15e (mystérieux) ; le 16e recommence un cycle |
| Difficile | 150 %, plus rapides (+1 gemme par coffre) | 1 coffre toutes les 2 vagues, par cycles de 8 : petits, sauf le 4e (grand) et le 8e (mystérieux) |
| Infernal | 220 %, encore plus rapides (+2 gemmes par coffre) | 1 grand coffre toutes les 3 vagues, par cycles de 6 : le 6e est mystérieux |

Repousser la **vague 1000** d'une difficulté donne une **orbe de sagesse** (une seule fois par difficulté).

---

## Interactions

### Tactile
- **Validation au relâchement** : un bouton n'agit que si le doigt est relâché dessus ; glisser hors du bouton annule l'action.
- **Glisser-déposer** pour fusionner ou déplacer les mages. Le mage déplacé est dessiné au-dessus du doigt pour rester visible, et les cases sont surlignées pendant le geste (vert : fusion possible, blanc : case libre).
- **Toucher la jauge** pour lancer l'ultime, **toucher un mage** de niveau 5 ou plus pour sa compétence, **appui long** sur un mage pour la bulle de montée de niveau (PO).
- **Grandes cibles** (≥ 48 dp) et éléments espacés pour éviter les erreurs de doigt ; boutons principaux en bas de l'écran, accessibles au pouce, pour jouer à une main.
- Une seule saisie de texte : le **code** des options.

### Capteurs
Aucun capteur n'est utilisé : le capteur de luminosité (qui faisait tomber la nuit) a été retiré à la demande des joueurs ; la nuit vient uniquement des niveaux et des vagues de nuit. L'accéléromètre n'est plus utilisé non plus : l'ultime se lance en touchant la jauge.

### Sauvegarde

- Tout est sauvegardé dès que ça change (achats, coffres, rangs, succès) et quand l'application passe en arrière-plan.
- **Partie en cours** : si l'application est quittée (ou fermée par le téléphone) en plein niveau, la partie est sauvegardée. Au prochain lancement, elle reprend **en pause, au début de la vague en cours**, avec les mages, les PO, les PV, les étoiles perdues, le prix des mages et les cases débloquées (un sage dont l'orbe a été retirée revient au niveau 6).
- Orbes de sagesse (réserve, posées, obtenues par difficulté), paliers des succès, codes utilisés et Trésor de départ sont sauvegardés.
- **Mises à jour** : installer un nouvel APK par-dessus l'ancien garde la sauvegarde (même clé de signature, voir « Télécharger et jouer »). Les données ajoutées par une mise à jour ont toutes une valeur par défaut : une ancienne sauvegarde se charge sans problème (les anciens fragments deviennent des cartes, les anciens coffres des petits coffres, les mages déjà achetés restent débloqués).

### Style de l'interface
Toute l'interface reprend la direction artistique pixel art médiéval-fantasy du décor (classe `UiKit`) : boutons en **tuiles en relief** qui s'enfoncent à l'appui, boutons ronds en **médaillons dorés**, fenêtres en **planches de bois cloutées** avec un **ruban** de titre, listes en **alcôves creusées**, cartes à liseré de la couleur de leur rareté, texte gras à contour sombre.

### Retours
- **Visuels** : feedback d'appui sur tous les boutons, flash rouge du rempart, bandeau jour/nuit, étoiles animées à la victoire, recharge de l'ultime.
- **Visuels (or)** : les PO sont affichées avec une **pièce d'or** en pixel art ; à chaque gobelin tué, une pièce s'envole vers le compteur, qui rebondit.
- **Sonores** : bruitages (recrutement, fusion, élimination, château touché, ultime, victoire, défaite, clic) et **trois musiques** en boucle, désactivables : médiévale (normale), **nuit** (calme et inquiétante) et **boss** (épique), avec un fondu de 0,75 s entre elles. Le son du jeu a ses **propres volumes** : il ne touche jamais au volume du téléphone et ne coupe pas la musique d'une autre application (on peut écouter sa musique à côté).
- **Haptiques** : vibrations à la fusion, quand le château est touché et au déclenchement de l'ultime ; **désactivables** dans les options et dans le menu pause.

### Options
- **Vibrations** (Non / Oui) — aussi dans le menu pause
- Musique (Non / Oui)
- **Mode gaucher** (Non / Oui) : interface en miroir (pause, options et son à gauche, jauge d'ultime à droite, ordre des boutons de recrutement inversé)
- **Volume de la musique** et **Volume des bruitages** : volumes internes au jeu (indépendants du volume du téléphone ; les boutons du téléphone règlent le volume multimédia général)
- **Effets des attaques** (Non / Oui) : sur Non, les attaques des mages sont dessinées de façon minimale (sans particules ni effets lourds) pour les téléphones peu puissants ; le jeu ne change pas
- **CODE** (seulement dans les options de l'accueil, pas dans la pause) : chaque code cadeau se saisit une fois par sauvegarde (`thomasleboss` +9999 gemmes, `thomasleboss2` +99999 gemmes, `jeveuxlesgrossesboulesdesagesse` +4 orbes de sagesse)
- Bouton de coupure du son à l'accueil et en jeu (coupe les deux volumes du jeu)

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
│   ├── Progression        Progression permanente : coffres par sorte, cartes, rangs, gemmes, équipe, records, codes, orbes de sagesse
│   ├── Roman              Chiffres romains (paliers de succès, biomes)
│   ├── MusicMood          Choix de la musique (normale, nuit, boss)
│   ├── Rarity             Rareté de chaque mage et coûts d'amélioration
│   ├── Chest              Les 3 coffres : prix, probabilités, tirage des cartes
│   ├── Ultimate           Les 4 ultimes (prix, jauge, effets)
│   ├── Artifact           Les 33 artefacts (3 par mage) et leurs modificateurs
│   ├── Achievement        Les succès à paliers sans fin (familles, objectifs, gemmes) et uniques
│   ├── Knight, Wall       Chevaliers de l'Invocateur, murs de l'Ouvrier
│   ├── EnemyBolt          Projectiles des gobelins mages
│   ├── Reaction, Zone     Les 54 réactions élémentaires et les zones au sol (lave, vapeur, béton, abysse…)
│   ├── Nuke               Bombe nucléaire du Scientifique (recharge, chute, impact)
│   ├── Mage, Enemy        Unités (caractéristiques par élément, niveau, rang ; types de gobelins)
│   └── Projectile, Bolt, Splash, HitEffect   Attaques et effets
├── view/                  Affichage et retours
│   ├── GameView           Vue Android : boucle de jeu (~60 images/s), relie tout
│   ├── GameRenderer       Dessin des écrans, du HUD et des unités
│   ├── AttackRenderer     Dessin des attaques selon l'élément et le niveau
│   ├── NukeRenderer       Bombe, flash, champignon atomique, onde de choc, tremblement
│   ├── TerrainRenderer    Décor pixel art généré (forêt, prairie, rempart, cour)
│   ├── Sprites            Mages et gobelins en pixel art
│   ├── Hud                Position de tous les boutons et fenêtres (miroir en mode gaucher)
│   ├── MenuList           Contenu des listes qui défilent (Marché, grille de cartes des Mages, Château), partagé dessin / touchers
│   ├── UiKit              Style de l'interface : tuiles, médaillons, fenêtres en bois, rubans
│   ├── Palette            Couleurs
│   ├── SoundManager       Bruitages (SoundPool)
│   ├── MusicPlayer        Musiques (normale, nuit, boss) avec fondu, volume interne (MediaPlayer)
│   └── Haptics            Vibrations
├── controller/            Entrées du joueur
│   └── TouchController    Touchers, glisser-déposer, glissement entre pages, défilement, cartes, boutons
└── data/
    ├── SaveManager        Sauvegarde (SharedPreferences) : progression, options, partie en cours à reprendre
    ├── UpdateChecker      Mise à jour automatique : lit apk/version.json (branche v2), télécharge l'APK, ouvre l'installateur
    └── VersionInfo        Lecture et comparaison de version.json (Java pur, testé)
```

Le modèle prévient la vue de ce qui se passe (château touché, fusion, victoire…) par l'interface `GameModel.GameListener`, ce qui déclenche sons, vibrations et sauvegarde.

---

## Installation

1. Cloner le dépôt et l'ouvrir avec **Android Studio**.
2. Laisser Gradle synchroniser le projet.
3. Brancher un téléphone Android (débogage USB activé) ou lancer un émulateur.
4. Cliquer sur **Run ▶**.


Permissions utilisées : `VIBRATE`, `INTERNET` (vérification des mises à jour) et `REQUEST_INSTALL_PACKAGES` (installer la mise à jour téléchargée).

**Publier une nouvelle version** : 1) augmenter `versionCode` (et `versionName`) dans `app/build.gradle.kts` ; 2) reconstruire l'APK dans `apk/MagicDefense-v2.apk` ; 3) mettre les mêmes valeurs dans `apk/version.json` ; 4) commiter et pousser les deux fichiers **ensemble** sur `v2` (sinon les joueurs retéléchargeraient l'ancien APK).

---

## Ressources

Graphismes (pixel art), bruitages et musiques créés pour le projet, générés par code : aucune ressource externe. La sirène et l'explosion du Scientifique sont générées par `tools/generate_nuke_alarm.js`, les musiques de nuit et de boss par `tools/generate_music.js` (Node.js, encodeur OGG `wasm-media-encoders` : `cd tools && npm install && node generate_music.js`). L'icône de l'application (chapeau de mage sur ciel de nuit) est une icône adaptative vectorielle.
