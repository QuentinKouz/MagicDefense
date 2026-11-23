package com.example.magicdefense.view;

import android.graphics.Bitmap;
import android.graphics.Color;

// Sprites pixel art : chaque personnage est une grille de caractères,
// chaque caractère correspond à une couleur de la palette ('.' = transparent).
// Les grilles sont converties une seule fois en petites images (Bitmap).
public final class Sprites {

    private Sprites() {
    }

    // ---------- Mages : 19 x 18 pixels ----------

    // Niveau 1 : apprenti, sans barbe, robe simple
    private static final String[] BODY_APPRENTICE = {
            "......KK",
            ".....KRRK",
            "....KRRRK",
            "....KRRRRK",
            "...KRRRRRK",
            "..KRRRRRRRK",
            ".KKKKKKKKKKK.....Tt",
            "...KSSSSSSK......Tt",
            "...KSESSESK......Tt",
            "...KSSSSSSK......Tt",
            "....KsSSsK.......Tt",
            "..KKRRRRRRKK...KSSK",
            ".KRRRRRRRRRRKKKSSSK",
            "KRRRRRRRRRRRRRKKKK",
            "KrRRRRRRRRRRRrK..Tt",
            "KrrRRRRRRRRRrrK..Tt",
            "KrrrrrrrrrrrrrK..Tt",
            ".KKKKKKKKKKKKKK..Tt",
    };

    // Niveaux 2 à 5 : barbe et ceinture dorée
    private static final String[] BODY_MAGE = {
            "......KK",
            ".....KRRK",
            "....KRRRK",
            "....KRRRRK",
            "...KRRRRRK",
            "..KRRRRRRRK",
            ".KKKKKKKKKKK.....Tt",
            "...KSSSSSSK......Tt",
            "...KSESSESK......Tt",
            "...KsSSSSsK......Tt",
            "..KRWWWWWWRK.....Tt",
            ".KRRwWWWWwRRK..KSSK",
            ".KRRRwWWwRRRRKKSSSK",
            "KRRRRRwwRRRRRRKKKK",
            "KrRRRRYYRRRRRrK..Tt",
            "KrrRRRYYRRRRrrK..Tt",
            "KrrrrrYYrrrrrrK..Tt",
            ".KKKKKKKKKKKKKK..Tt",
    };

    // Embout du bâton (6 lignes, posé en haut à droite, colonnes 15 à 18), par élément
    private static final String[][] TIPS = {
            {"..g.", ".gGg", "gGGg", "gGGg", ".gg.", "..T."},   // Eau : goutte
            {".G..", ".GgG", "gGgg", "gGGg", ".gg.", "..T."},   // Feu : flamme
            {"..g.", ".gGg", "gGGg", ".gGg", ".gg.", "..T."},   // Terre : feuille
            {"..gG", ".gG.", "gGGg", "..Gg", ".Gg.", "..T."},   // Foudre : éclair
            {".g.g", "g.G.", ".gGg", "gGGg", ".gg.", "..T."},   // Poison : bulles toxiques
            {"gGg.", "...g", ".gG.", "g...", ".gGg", "..T."},   // Air : spirale de vent
            {".gg.", "gGGg", "gKGg", "gGGg", ".gg.", "..T."},   // Invocateur : orbe avec un œil
            {"gggg", "gGGg", "gggg", "..T.", "..T.", "..T."},   // Ouvrier : tête de marteau
            {"g.g.", ".GG.", "gGGg", ".GG.", "g.g.", "..T."},   // Lumière : soleil rayonnant
            {".gg.", "gKKg", "gKGg", "gKKg", ".gg.", "..T."},   // Ténèbres : orbe d'ombre
            {".KK.", ".WW.", "KWGK", "KGgK", ".KK.", "..T."},   // Scientifique : fiole de potion
    };
    private static final String[] SMALL_TIP = {"....", "....", "..g.", ".gGg", "..g.", "..T."};

    // Ourlet de la robe (ligne 16, colonnes 1 à 13), à partir du niveau 3
    private static final String[] HEMS = {
            "rGrrGYYrGrrGr",   // Eau : vaguelettes
            "rgrgrYYrgrgrr",   // Feu : braises
            "rrGrrYYrrGrrr",   // Terre : feuilles
            "rGrrrYYrrrGrr",   // Foudre : étincelles
            "rGrrgYYrgrrGr",   // Poison : gouttes
            "rGrGrYYrGrGrr",   // Air : rafales
            "rgrrGYYGrrgrr",   // Invocateur : runes
            "rGGrrYYrrGGrr",   // Ouvrier : briques
            "GrGrGYYGrGrGr",   // Lumière : rayons
            "rKrrKYYKrrKrr",   // Ténèbres : ombres
            "rrgGrYYrrGgrr",   // Scientifique : poches et stylos
    };

    // Couleurs propres à chaque élément : robe, robe ombrée, embout clair, embout foncé
    private static final int[][] ELEMENT_COLORS = {
            {Color.rgb(70, 140, 235), Color.rgb(40, 90, 175), Color.rgb(150, 220, 255), Color.rgb(60, 150, 230)},
            {Color.rgb(225, 80, 45), Color.rgb(160, 45, 30), Color.rgb(255, 220, 90), Color.rgb(255, 120, 30)},
            {Color.rgb(140, 105, 60), Color.rgb(95, 70, 40), Color.rgb(120, 190, 80), Color.rgb(70, 130, 50)},
            {Color.rgb(150, 80, 230), Color.rgb(95, 45, 165), Color.rgb(255, 245, 140), Color.rgb(250, 200, 40)},
            {Color.rgb(90, 170, 60), Color.rgb(55, 115, 35), Color.rgb(200, 255, 120), Color.rgb(110, 190, 40)},
            {Color.rgb(185, 215, 232), Color.rgb(120, 160, 185), Color.rgb(255, 255, 255), Color.rgb(160, 210, 240)},
            {Color.rgb(200, 80, 160), Color.rgb(130, 45, 105), Color.rgb(255, 190, 240), Color.rgb(230, 110, 200)},
            {Color.rgb(215, 140, 40), Color.rgb(150, 90, 25), Color.rgb(200, 200, 210), Color.rgb(120, 120, 135)},
            {Color.rgb(245, 240, 225), Color.rgb(205, 190, 150), Color.rgb(255, 250, 200), Color.rgb(255, 210, 70)},
            {Color.rgb(75, 45, 110), Color.rgb(45, 25, 70), Color.rgb(200, 140, 255), Color.rgb(130, 70, 200)},
            // Scientifique : blouse blanche, potion vert fluo et violette
            {Color.rgb(238, 242, 240), Color.rgb(180, 195, 195), Color.rgb(150, 255, 80), Color.rgb(165, 80, 230)},
    };

    private static final int WORKER_TYPE = 7; // Mage.WORKER
    private static final String[] WORKER_HELMET = {
            "",
            "",
            "....KKKK",
            "...KYYYYK",
            "..KYYYYYYK",
            "..KYYYYYYYK",
    };

    public static final int MAGE_WIDTH = 19;
    public static final int MAGE_HEIGHT = 18;
    public static final int MAGE_LEVELS = 7; // 1 à 5, 6 = archimage, 7 = sage

    // ---------- Niveau 6 (archimage) : coiffe et arme propres à chaque élément ----------
    // Coiffe : lignes 0 à 6 (colonnes 0 à 13, remplacent le chapeau) ; lignes suivantes : seuls les
    // pixels dessinés recouvrent le corps (cheveux, masque...). Couleurs en plus : A/a argent,
    // y or ombré, Z/z os, H ombre, P rouge, O orange, B cuir, V runes, C cyan (voir buildMages).
    private static final String[][] HEADS_6 = {
            {   // Eau : couronne à trois pointes de Poséidon, cheveux ondulés
                    ".............",
                    "...Y..YY..Y..",
                    "...YY.YY.YY..",
                    "...YYYYYYYY..",
                    "...YCYYYYCY..",
                    "..KgGgGgGgGK.",
                    ".KgKKKKKKKKgK",
                    "..g........g.",
                    "..g........g.",
                    "..G........G.",
            },
            {   // Feu : couronne de flammes, cheveux de phénix
                    "......GG.....",
                    "...G..gg..G..",
                    "...gG.OO.Gg..",
                    "...gOGOOGOg..",
                    "...YYYYYYYY..",
                    "..KPPPPPPPPK.",
                    ".KPKKKKKKKKPK",
                    "..P........P.",
                    "..O........O.",
            },
            {   // Terre : heaume de golem en pierre, runes qui luisent
                    ".............",
                    "...KKKKKKKK..",
                    "..KaAAAAAAaK.",
                    "..KAVAAAAVAK.",
                    "..KaAAVVAAaK.",
                    "..KAAAAAAAAK.",
                    ".KaKKKKKKKKaK",
                    "..a........a.",
                    "..a........a.",
            },
            {   // Foudre : casque ailé façon Thor, cheveux roux
                    ".............",
                    ".W.........W.",
                    ".WW.KKKKKK.WW",
                    "..WKAAAAAAKW.",
                    "..KAaAAAAaAK.",
                    "..KAAAAAAAAK.",
                    ".KYKKKKKKKKYK",
                    "..O........O.",
                    "..O........O.",
            },
            {   // Poison : chapeau noir et masque à bec de médecin de peste
                    ".............",
                    "....KKKKKK...",
                    "....KHHHHK...",
                    "....KHHHHK...",
                    "...KggggggK..",
                    ".KKHHHHHHHHKK",
                    "KHHHHHHHHHHHK",
                    "...KZCZZCZK..",
                    "...KZZZZZZZZZz",
                    "...KzZZZZZzzz.",
            },
            {   // Air : moine du vent, crâne rasé et flèche tatouée
                    ".............",
                    "......G......",
                    "....KKKKKK...",
                    "...KSSGGSSK..",
                    "..KSSSGGSSSK.",
                    "..KSSGGGGSSK.",
                    "...KSSSSSSK..",
            },
            {   // Invocateur : roi nécromancien, couronne sombre, capuche, yeux qui brillent
                    ".............",
                    "...a..aa..a..",
                    "...aa.aa.aa..",
                    "...aaaaaaaa..",
                    "...aGaaaaGa..",
                    "..KRRRRRRRRK.",
                    ".KRKKKKKKKKRK",
                    "..R........R.",
                    "..R..G..G..R.",
            },
            {   // Ouvrier : maître bâtisseur nain, casque à cornes et bandeau doré
                    ".............",
                    ".Z..KKKKKK..Z",
                    ".Zz.KAAAAK.zZ",
                    "..zKAAAAAAKz.",
                    "...KAaAAaAK..",
                    "..KYYYYYYYYK.",
                    ".KAKKKKKKKKAK",
                    "..O........O.",
            },
            {   // Lumière : séraphin, auréole dorée, cheveux d'or, ailes
                    "....YYYYYY...",
                    "...Y......Y..",
                    "....YYYYYY...",
                    ".............",
                    "....KKKKKK...",
                    "...KGGGGGGK..",
                    "..KGgGGGGgGK.",
                    "WW.........WW",
                    "WW.........WW",
                    "Ww.........wW",
                    "w...........w",
            },
            {   // Ténèbres : faucheuse, grande capuche et crâne aux yeux brillants
                    "......K......",
                    ".....KRK.....",
                    "....KRRRK....",
                    "...KRRRRRK...",
                    "..KRRRRRRRK..",
                    "..KRrHHHHrRK.",
                    ".KRrHHHHHHrRK",
                    "..RKZZZZZZKR.",
                    "..RKZGZZGZKR.",
                    "..RKzZZZZzKR.",
                    "...RRRRRRRR..",     // (archimage seulement) robe à la place de la barbe
                    "....RRZZRR...",
                    ".....RZZR....",
                    ".....RRRR....",
                    "......RR.....",
            },
            {   // Scientifique : casque anti-radiations jaune, visière et masque à gaz
                    ".............",
                    "....KKKKKK...",
                    "...KYYYYYYK..",
                    "..KYYYYYYYYK.",
                    "..KYYyYYyYYK.",
                    "..KYYYYYYYYK.",
                    ".KYYYYYYYYYYK",
                    "..YKCCCCCCKY.",
                    "..YKCWCCCCKY.",
                    "..YKKaAAaKKY.",
            },
    };
    // Arme (lignes 0 à 5, colonnes 14 à 18) : le bâton continue colonne 17
    private static final String[][] TIPS_6 = {
            {"Y.Y.Y", "Y.Y.Y", "YYYYY", "..Yt.", "...T.", "...Tt"},   // Eau : trident
            {"G...G", "gG.Gg", "OgGgO", ".OPO.", "..PT.", "...Tt"},   // Feu : phénix
            {".aAa.", "aAVAa", "aVAVa", "aAVAa", ".aaT.", "...Tt"},   // Terre : pierre runique
            {".G..G", "aAAAa", "AAGAA", "aAAAa", "...B.", "...Tt"},   // Foudre : marteau de Thor
            {"..g..", ".KAK.", ".KAK.", "KGGGK", "KgGgK", ".KKTt"},   // Poison : fiole d'alchimiste
            {"..G..", "W.G.W", "WWGWW", ".WgW.", "...T.", "...Tt"},   // Air : bâton ailé
            {"...Y.", "RRRT.", "RGRT.", "RRRT.", "R.RT.", "...Tt"},   // Invocateur : bannière
            {"AAAAA", "AaaaA", "AAAAA", "...B.", "...B.", "...Tt"},   // Ouvrier : marteau de forge
            {"Y.G.Y", ".YGY.", "GGWGG", ".YGY.", "...Y.", "...Tt"},   // Lumière : soleil
            {"AAAT.", "A..T.", "a..T.", "...T.", "...T.", "...Tt"},   // Ténèbres : faux
            {".KWK.", ".KWK.", "KGGGK", "KGWGK", "KgGgK", "..KTt"},   // Scientifique : fiole géante
    };
    // Élément sans dessin dédié (nouveau mage) : couronne d'archimage et orbe
    private static final String[] DEFAULT_HEAD_6 = {
            ".............",
            "...Y..YY..Y..",
            "...YYYYYYYY..",
            "...YGYYYYGY..",
            "...KRRRRRRK..",
            "..KRRRRRRRRK.",
            ".KKKKKKKKKKKK",
    };
    private static final String[] DEFAULT_TIP_6 = {".gGg.", "gGWGg", "gGGGg", ".gGg.", "..YT.", "...Tt"};

    // ---------- Niveau 7 (sage) : longue barbe jusqu'au sol, col et ourlet dorés ----------
    // Lignes 7 à 17 (sourcils blancs, barbe, robe brodée d'or)
    private static final String[] BODY_SAGE = {
            "...KWWSSWWK......Tt",
            "...KSESSESK......Tt",
            "...KWSSSSWK......Tt",
            "..KYWWWWWWYK.....Tt",
            ".KRYwWWWWwYRK..KSSK",
            ".KRRYWWWWWWYRKKSSSK",
            "KRRRYwWWWWwYRRKKKK",
            "KrRRYwWWWWwYRrK..Tt",
            "KrrRYRwWWwRYRrK..Tt",
            "KYYYYYwWWwYYYYK..Tt",
            ".KKKKKKKKKKKKKK..Tt",
    };

    // Archimage (6) et sage (7) : partent du grand mage (5), puis coiffe et arme de l'élément ;
    // le sage a en plus le corps de sage, l'argent devenu or et des étincelles
    private static String[] highMageRows(int type, int level) {
        boolean sage = level >= 7;
        char[][] g = toGrid(mageRows(type, 5), MAGE_WIDTH);
        if (sage) {
            for (int i = 0; i < BODY_SAGE.length; i++) {
                g[7 + i] = pad(BODY_SAGE[i], MAGE_WIDTH);
            }
        } else {
            g[10][3] = 'Y';  // fermoirs dorés de l'archimage
            g[10][10] = 'Y';
        }
        String[] head = type < HEADS_6.length ? HEADS_6[type] : DEFAULT_HEAD_6;
        for (int y = 0; y < head.length && y < MAGE_HEIGHT; y++) {
            if (sage && y >= 10) {
                break; // le sage garde sa longue barbe
            }
            for (int x = 0; x < 14; x++) {
                char c = x < head[y].length() ? head[y].charAt(x) : '.';
                if (y < 7 || c != '.') {
                    g[y][x] = c;
                }
            }
        }
        String[] tip = type < TIPS_6.length ? TIPS_6[type] : DEFAULT_TIP_6;
        for (int y = 0; y < 6; y++) {
            for (int x = 0; x < 5; x++) {
                g[y][14 + x] = y < tip.length && x < tip[y].length() ? tip[y].charAt(x) : '.';
            }
        }
        if (sage) {
            // Ornements dorés : l'argent de la coiffe et de l'arme devient or
            for (int y = 0; y < 10; y++) {
                for (int x = 0; x < MAGE_WIDTH; x++) {
                    if (g[y][x] == 'A') {
                        g[y][x] = 'Y';
                    } else if (g[y][x] == 'a') {
                        g[y][x] = 'y';
                    }
                }
            }
            // Étincelles de sagesse autour de la tête
            int[][] sparks = {{0, 1}, {0, 12}, {3, 0}, {2, 13}};
            for (int[] s : sparks) {
                if (g[s[0]][s[1]] == '.') {
                    g[s[0]][s[1]] = 'W';
                }
            }
        }
        return fromGrid(g);
    }

    // Grille du mage d'un élément et d'un niveau : le design évolue à chaque fusion
    static String[] mageRows(int type, int level) {
        if (level >= 6) {
            return highMageRows(type, level);
        }
        char[][] g = toGrid(level == 1 ? BODY_APPRENTICE : BODY_MAGE, MAGE_WIDTH);
        if (type == WORKER_TYPE) {
            // Ouvrier : casque de chantier jaune à la place du chapeau pointu
            for (int i = 0; i < WORKER_HELMET.length; i++) {
                g[i] = pad(WORKER_HELMET[i], MAGE_WIDTH);
            }
        }

        String[] tip = (level == 1) ? SMALL_TIP : TIPS[type];
        for (int i = 0; i < tip.length; i++) {
            for (int j = 0; j < tip[i].length(); j++) {
                if (tip[i].charAt(j) != '.') {
                    g[i][15 + j] = tip[i].charAt(j);
                }
            }
        }
        if (level >= 3) {                       // emblème sur le chapeau + ourlet décoré
            g[4][6] = 'G';
            for (int j = 0; j < HEMS[type].length(); j++) {
                g[16][1 + j] = HEMS[type].charAt(j);
            }
        }
        if (level >= 4) {                       // bord du chapeau doré, longue barbe, étincelles
            for (int x = 2; x <= 10; x += 2) {
                g[6][x] = 'Y';
            }
            g[13] = pad("KRRRRwWWwRRRRRKKKK", MAGE_WIDTH);
            g[14] = pad("KrRRRRwwRRRRRrK..Tt", MAGE_WIDTH);
            g[0][15] = 'Y';
            g[0][18] = 'Y';
            g[2][14] = 'Y';
        }
        if (level >= 5 && type == WORKER_TYPE) { // chef de chantier : lampe frontale sur le casque
            g[3][5] = 'W';
            g[3][6] = 'W';
        } else if (level >= 5) {                 // grand mage : pointe et bande du chapeau dorées
            g[0][6] = 'Y';
            g[0][7] = 'Y';
            g[1][6] = 'Y';
            g[1][7] = 'Y';
            for (int x = 4; x <= 8; x++) {
                g[5][x] = 'Y';
            }
            g[5][6] = 'G';
        }
        if (type == SCIENTIST_TYPE) {
            scientistLook(g, level); // cheveux fous et lunettes à la place du chapeau
        }
        return fromGrid(g);
    }

    // ---------- Scientifique : blouse blanche, cheveux en bataille, lunettes de protection ----------
    private static final int SCIENTIST_TYPE = 10; // Mage.SCIENTIST
    // Lignes 0 à 8 (colonnes 0 à 13) : lignes 0 à 6 remplacées (plus de chapeau), puis seuls les pixels
    // dessinés recouvrent le visage (favoris, lunettes)
    private static final String[] SCIENTIST_HAIR = {
            "..............",
            "....A..a..A...",
            "..A.AaAAaA.A..",
            "...aAAAAAAAa..",
            "..AAaAAAAaAAA.",
            ".aAAAAAAAAAAa.",
            "..aKKKKKKKKa..",
            "..A........A..",
            "...KCCAACCK...",
    };

    private static void scientistLook(char[][] g, int level) {
        for (int y = 0; y < SCIENTIST_HAIR.length; y++) {
            for (int x = 0; x < 14; x++) {
                char c = SCIENTIST_HAIR[y].charAt(x);
                if (y < 7 || c != '.') {
                    g[y][x] = c;
                }
            }
        }
        // Barbe grise en bataille (la blouse est blanche)
        for (int y = 9; y < MAGE_HEIGHT; y++) {
            for (int x = 0; x < 14; x++) {
                if (g[y][x] == 'W') {
                    g[y][x] = 'A';
                } else if (g[y][x] == 'w') {
                    g[y][x] = 'a';
                }
            }
        }
        if (level >= 4) {                       // lunettes cerclées d'or
            g[8][3] = 'Y';
            g[8][6] = 'Y';
            g[8][7] = 'Y';
            g[8][10] = 'Y';
        }
        if (level >= 5) {                       // mèches irradiées vert fluo
            g[1][4] = 'G';
            g[2][8] = 'G';
            g[3][5] = 'G';
            g[4][10] = 'G';
            g[5][3] = 'G';
        }
    }

    // ---------- Gobelins : 19 x 16 pixels, 2 images de marche ----------
    // De jour : yeux jaunes. De nuit : torche dans la main, peau plus sombre, yeux rouges.

    private static final String[] GOBLIN_WALK_1 = {
            "...K..............K",
            "...KLK..KKKKKK..KLK",
            "....KLKKLLLLLLKKLK.",
            "....KLLLLLLLLLLLLK.",
            ".....KLLQELLQELLK..",
            ".....KLLQQLLQQLLK..",
            ".....KlLLLLLLLLlK..",
            "......KLFKFFKFLK...",
            ".......KKLLLLKK....",
            "......KLlLLLLlLK...",
            ".....KLKLLLLLLKLK..",
            ".....KLKPPPPPPKLK..",
            ".....KKKPPppPPKKK..",
            ".......KLK..KLK....",
            ".......KlK..KlK....",
            "......KKKK..KKKK...",
    };
    private static final String[] GOBLIN_WALK_2 = {
            "...K..............K",
            "...KLK..KKKKKK..KLK",
            "....KLKKLLLLLLKKLK.",
            "....KLLLLLLLLLLLLK.",
            ".....KLLQELLQELLK..",
            ".....KLLQQLLQQLLK..",
            ".....KlLLLLLLLLlK..",
            "......KLFKFFKFLK...",
            ".......KKLLLLKK....",
            "......KLlLLLLlLK...",
            ".....KLKLLLLLLKLK..",
            ".....KLKPPPPPPKLK..",
            ".....KKKPPppPPKKK..",
            "........KLKKLK.....",
            ".......KlK..KlK....",
            "......KKK....KKK...",
    };
    private static final String[] GOBLIN_TORCH_1 = {
            "...K.............oK",
            "...KLK..KKKKKK..OoO",
            "....KLKKLLLLLLKKOTO",
            "....KLLLLLLLLLLLLTt",
            ".....KLLQELLQELLKTt",
            ".....KLLQQLLQQLLKTt",
            ".....KlLLLLLLLLKLT.",
            "......KLFKFFKFLKLK.",
            ".......KKLLLLKKLKK.",
            "......KLlLLLLlLLK..",
            ".....KLKLLLLLLK....",
            ".....KLKPPPPPPK....",
            ".....KKKPPppPPK....",
            ".......KLK..KLK....",
            ".......KlK..KlK....",
            "......KKKK..KKKK...",
    };
    private static final String[] GOBLIN_TORCH_2 = {
            "...K.............oK",
            "...KLK..KKKKKK..OoO",
            "....KLKKLLLLLLKKOTO",
            "....KLLLLLLLLLLLLTt",
            ".....KLLQELLQELLKTt",
            ".....KLLQQLLQQLLKTt",
            ".....KlLLLLLLLLKLT.",
            "......KLFKFFKFLKLK.",
            ".......KKLLLLKKLKK.",
            "......KLlLLLLlLLK..",
            ".....KLKLLLLLLK....",
            ".....KLKPPPPPPK....",
            ".....KKKPPppPPK....",
            "........KLKKLK.....",
            ".......KlK..KlK....",
            "......KKK....KKK...",
    };

    // ---------- Chaman gobelin : 19 x 18 pixels ----------
    // Capuche et robe bordeaux à motifs tribaux, plumes sur la capuche, collier d'os,
    // bâton surmonté d'un crâne (à gauche) : on le distingue d'un coup d'œil.
    private static final String[] SHAMAN_1 = {
            ".KKK.....R.Y.C.....",
            "KBBBK....RKYKC.....",
            "KGBGK...KHHHHHK....",
            ".KbK...KHHhhhHHK...",
            "..T..KlHhLLLLLhHlK.",
            "..T...KHLQELQELHK..",
            "..T...KHLLLLLLLHK..",
            "..T...KHKLFKFLKHK..",
            "..T..KHHHKKKKKHHHK.",
            ".KLKKHHhHHYHHhHHK..",
            "..TKHHHhHBbBHhHHK..",
            "..T.KHHhHHHHHhHHK..",
            "..T.KHHhHHCHHhHKLK.",
            "..T.KHhHHCCCHHhHK..",
            "..T.KHhHHHCHHHhHK..",
            "..TKHhHHHHHHHHHhHK.",
            "..TKhhhhhhhhhhhhhK.",
            "..t.KKK.KKK.KKK....",
    };
    private static final String[] SHAMAN_2 = {
            ".KKK.....R.Y.C.....",
            "KBBBK....RKYKC.....",
            "KGBGK...KHHHHHK....",
            ".KbK...KHHhhhHHK...",
            "..T..KlHhLLLLLhHlK.",
            "..T...KHLQELQELHK..",
            "..T...KHLLLLLLLHK..",
            "..T...KHKLFKFLKHK..",
            "..T..KHHHKKKKKHHHK.",
            ".KLKKHHhHHYHHhHHK..",
            "..TKHHHhHBbBHhHHK..",
            "..T.KHHhHHHHHhHHK..",
            "..T.KHHhHHCHHhHKLK.",
            "..T.KHhHHCCCHHhHK..",
            "..T.KHhHHHCHHHhHK..",
            "..TKHhHHHHHHHHHhHK.",
            "..TKhhhhhhhhhhhhhK.",
            "..t...KKK...KKK....",
    };
    public static final int SHAMAN_WIDTH = 19;
    public static final int SHAMAN_HEIGHT = 18;
    public static final float SHAMAN_CENTER_X = 11.5f; // centre du corps (le bâton dépasse à gauche)
    public static final float SHAMAN_SKULL_X = 2.5f;   // yeux du crâne (lueur d'invocation)
    public static final float SHAMAN_SKULL_Y = 2.5f;

    // ---------- Gobelin mage : 19 x 20 pixels ----------
    // Grand chapeau pointu étoilé, robe bleu nuit, orbe magique tenu à bout de bras (à droite).
    private static final String[] GOBLIN_MAGE_1 = {
            "..........KK.......",
            ".........KNNK......",
            "........KNNK.......",
            ".......KNNnK.......",
            ".......KNYNK.......",
            "......KNNNnNK......",
            ".....KNnNNNYNK.....",
            "....KNNNnNNNNnNK...",
            "..KKKKKKKKKKKKKKK..",
            "..KlKLLLLLLLLLKlK..",
            "....KLLQELQELLK.KK.",
            "....KlLLLLLLLlKKoOK",
            ".....KLFKKFLLK.KOOK",
            "....KNNKKKKKNNK.KK.",
            "...KNNNnNYNnNNKLK..",
            "...KNNnNNYNNnNNK...",
            "..KNNnNNNYNNNnNNK..",
            "..KNnNNNNNNNNNnNK..",
            "..KnnnnnnnnnnnnnK..",
            "....KKK.....KKK....",
    };
    private static final String[] GOBLIN_MAGE_2 = {
            "..........KK.......",
            ".........KNNK......",
            "........KNNK.......",
            ".......KNNnK.......",
            ".......KNYNK.......",
            "......KNNNnNK......",
            ".....KNnNNNYNK.....",
            "....KNNNnNNNNnNK...",
            "..KKKKKKKKKKKKKKK..",
            "..KlKLLLLLLLLLKlK..",
            "....KLLQELQELLK.KK.",
            "....KlLLLLLLLlKKoOK",
            ".....KLFKKFLLK.KOOK",
            "....KNNKKKKKNNK.KK.",
            "...KNNNnNYNnNNKLK..",
            "...KNNnNNYNNnNNK...",
            "..KNNnNNNYNNNnNNK..",
            "..KNnNNNNNNNNNnNK..",
            "..KnnnnnnnnnnnnnK..",
            "...KKK.......KKK...",
    };
    public static final int GOBLIN_MAGE_WIDTH = 19;
    public static final int GOBLIN_MAGE_HEIGHT = 20;
    public static final float GOBLIN_MAGE_CENTER_X = 9.5f; // centre du corps (l'orbe dépasse à droite)
    public static final float GOBLIN_MAGE_ORB_X = 17f;     // centre de l'orbe dans la grille
    public static final float GOBLIN_MAGE_ORB_Y = 11.5f;

    // ---------- Coffre : 16 pixels de large, couvercle (6 lignes) et caisse (8 lignes) séparés ----------
    // Le couvercle est une image à part pour pouvoir se soulever et basculer à l'ouverture.
    private static final String[] CHEST_LID = {
            "..KKKKKKKKKKKK..",
            ".KLLLLLYYLLLLLK.",
            "KLLLLLLYYLLLLLLK",
            "KlLLLLLYYLLLLLlK",
            "KllllllYYllllllK",
            "KKKKKKKYYKKKKKKK",
    };
    private static final String[] CHEST_BODY = {
            "KWWWWWYYYYWWWWWK",
            "KYWWWWYyyYWWWWYK",
            "KWwWWWYKKYWWWwWK",
            "KWWWWWYyyYWWWWWK",
            "KWWwWWWYYWWWwWWK",
            "KwwwwwwYYwwwwwwK",
            "KYwwwwwYYwwwwwYK",
            "KKKKKKKKKKKKKKKK",
    };
    public static final int CHEST_WIDTH = 16;
    public static final int CHEST_LID_HEIGHT = 6;
    public static final int CHEST_BODY_HEIGHT = 8;

    private static int[] chestPalette() {
        int[] p = commonPalette();
        p['L'] = Color.rgb(176, 110, 52);   // couvercle
        p['l'] = Color.rgb(132, 80, 38);    // couvercle ombré
        p['W'] = Color.rgb(146, 88, 42);    // caisse
        p['w'] = Color.rgb(106, 62, 30);    // caisse ombrée
        p['y'] = Color.rgb(176, 128, 36);   // or ombré
        return p;
    }

    // { couvercle, caisse }
    public static Bitmap[] buildChest() {
        int[] p = chestPalette();
        return new Bitmap[]{toBitmap(CHEST_LID, p), toBitmap(CHEST_BODY, p)};
    }

    public static final int GOBLIN_WIDTH = 19;
    public static final int GOBLIN_HEIGHT = 16;
    public static final float GOBLIN_CENTER_X = 11f;  // centre du corps (colonne), la torche dépasse à droite
    public static final float TORCH_FLAME_X = 17.5f;  // position de la flamme dans la grille
    public static final float TORCH_FLAME_Y = 1.3f;

    // ---------- Palettes ----------

    private static int[] commonPalette() {
        int[] p = new int[128];                 // case vide = 0 = transparent
        p['K'] = Color.rgb(26, 22, 38);         // contour
        p['E'] = Color.rgb(26, 22, 38);         // pupilles
        p['S'] = Color.rgb(242, 199, 155);      // peau
        p['s'] = Color.rgb(205, 150, 110);      // peau ombrée
        p['W'] = Color.rgb(236, 236, 240);      // barbe
        p['w'] = Color.rgb(176, 176, 192);      // barbe ombrée
        p['T'] = Color.rgb(139, 90, 43);        // bois
        p['t'] = Color.rgb(96, 60, 28);         // bois ombré
        p['Y'] = Color.rgb(240, 192, 64);       // or
        return p;
    }

    private static int[] goblinPalette(boolean night) {
        int[] p = commonPalette();
        p['L'] = night ? Color.rgb(70, 110, 50) : Color.rgb(120, 190, 70);   // peau verte
        p['l'] = night ? Color.rgb(45, 75, 32) : Color.rgb(75, 135, 45);     // peau ombrée
        p['Q'] = night ? Color.rgb(255, 50, 40) : Color.rgb(255, 230, 60);   // yeux : rouges la nuit
        p['P'] = night ? Color.rgb(95, 60, 35) : Color.rgb(140, 85, 45);     // pagne
        p['p'] = night ? Color.rgb(65, 40, 25) : Color.rgb(95, 55, 30);
        p['F'] = Color.rgb(245, 245, 230);                                   // crocs
        p['O'] = Color.rgb(255, 150, 40);                                    // flamme
        p['o'] = Color.rgb(255, 230, 120);                                   // cœur de la flamme
        return p;
    }

    // ---------- Chevalier (invoqué par l'Invocateur) : 13 x 15 pixels ----------
    // Armure d'argent, plumet magenta (couleur de l'Invocateur), épée et bouclier
    private static final String[] KNIGHT_GUARD = {
            "....MM.......",
            "...MMm.......",
            "..KKKKK......",
            ".KAAAAAK...W.",
            ".KAKKKAK...W.",
            ".KAAAAAK...W.",
            "KKKAAAKKK..W.",
            "KMMKAAAAAKKYK",
            "KMYMKAaAAAKK.",
            "KMMKAAaAAK...",
            "KMMKAaAAAK...",
            ".KKKAAAAAK...",
            "...KaK.KaK...",
            "...KaK.KaK...",
            "...KKK.KKK...",
    };
    // Coup d'épée : la lame passe devant
    private static final String[] KNIGHT_STRIKE = {
            "....MM.......",
            "...MMm.......",
            "..KKKKK......",
            ".KAAAAAK.....",
            ".KAKKKAK.....",
            ".KAAAAAK.....",
            "KKKAAAKKK....",
            "KMMKAAAAAKKYK",
            "KMYMKAaAAAWWW",
            "KMMKAAaAAKWWW",
            "KMMKAaAAAK.WW",
            ".KKKAAAAAK...",
            "...KaK.KaK...",
            "...KaK.KaK...",
            "...KKK.KKK...",
    };
    public static final int KNIGHT_WIDTH = 13;
    public static final int KNIGHT_HEIGHT = 15;

    public static Bitmap[] buildKnight() {
        int[] p = commonPalette();
        p['A'] = Color.rgb(196, 204, 218);   // armure
        p['a'] = Color.rgb(130, 138, 160);   // armure ombrée
        p['M'] = Color.rgb(220, 90, 170);    // plumet et bouclier (Invocateur)
        p['m'] = Color.rgb(150, 50, 115);
        p['W'] = Color.rgb(240, 240, 250);   // lame
        return new Bitmap[]{toBitmap(KNIGHT_GUARD, p), toBitmap(KNIGHT_STRIKE, p)};
    }

    // ---------- Construction des images ----------

    // mages[type][niveau - 1]
    public static Bitmap[][] buildMages() {
        Bitmap[][] mages = new Bitmap[ELEMENT_COLORS.length][MAGE_LEVELS];
        for (int type = 0; type < ELEMENT_COLORS.length; type++) {
            int[] p = commonPalette();
            p['R'] = ELEMENT_COLORS[type][0];
            p['r'] = ELEMENT_COLORS[type][1];
            p['G'] = ELEMENT_COLORS[type][2];
            p['g'] = ELEMENT_COLORS[type][3];
            // Couleurs des niveaux 6 (archimage) et 7 (sage)
            p['A'] = Color.rgb(205, 210, 225);   // argent / pierre claire
            p['a'] = Color.rgb(125, 130, 150);   // argent ombré
            p['y'] = Color.rgb(176, 128, 36);    // or ombré
            p['Z'] = Color.rgb(236, 230, 210);   // os, masque
            p['z'] = Color.rgb(180, 170, 145);   // os ombré
            p['H'] = Color.rgb(30, 22, 42);      // ombre (capuche, chapeau noir)
            p['P'] = Color.rgb(230, 55, 60);     // rouge phénix
            p['O'] = Color.rgb(255, 160, 40);    // orange flamme
            p['B'] = Color.rgb(120, 75, 40);     // cuir
            p['V'] = Color.rgb(190, 110, 255);   // runes violettes
            p['C'] = Color.rgb(130, 235, 255);   // lueur cyan
            for (int level = 1; level <= MAGE_LEVELS; level++) {
                mages[type][level - 1] = toBitmap(mageRows(type, level), p);
            }
        }
        return mages;
    }

    // goblins[0 ou 1 : image de marche] ; de nuit, la variante avec la torche
    public static Bitmap[] buildGoblins(boolean night) {
        int[] p = goblinPalette(night);
        if (night) {
            return new Bitmap[]{toBitmap(GOBLIN_TORCH_1, p), toBitmap(GOBLIN_TORCH_2, p)};
        }
        return new Bitmap[]{toBitmap(GOBLIN_WALK_1, p), toBitmap(GOBLIN_WALK_2, p)};
    }

    // Chaman : 2 images de marche (de nuit, peau plus sombre et yeux rouges)
    public static Bitmap[] buildShaman(boolean night) {
        int[] p = goblinPalette(night);
        p['H'] = night ? Color.rgb(96, 26, 40) : Color.rgb(140, 36, 52);    // robe et capuche bordeaux
        p['h'] = night ? Color.rgb(62, 16, 28) : Color.rgb(92, 22, 36);
        p['B'] = Color.rgb(236, 228, 200);                                   // os, crâne
        p['b'] = Color.rgb(176, 166, 136);
        p['G'] = Color.rgb(90, 255, 140);                                    // orbites : lueur verte
        p['R'] = Color.rgb(230, 60, 50);                                     // plumes
        p['C'] = Color.rgb(60, 210, 190);                                    // plume et motifs turquoise
        return new Bitmap[]{toBitmap(SHAMAN_1, p), toBitmap(SHAMAN_2, p)};
    }

    // Gobelin mage : 2 images de marche
    public static Bitmap[] buildGoblinMage(boolean night) {
        int[] p = goblinPalette(night);
        p['N'] = night ? Color.rgb(36, 44, 110) : Color.rgb(52, 64, 160);   // robe et chapeau bleu nuit
        p['n'] = night ? Color.rgb(22, 26, 70) : Color.rgb(32, 38, 104);
        p['O'] = Color.rgb(110, 200, 255);                                   // orbe
        p['o'] = Color.rgb(235, 250, 255);                                   // reflet de l'orbe
        return new Bitmap[]{toBitmap(GOBLIN_MAGE_1, p), toBitmap(GOBLIN_MAGE_2, p)};
    }

    private static Bitmap toBitmap(String[] rows, int[] palette) {
        int h = rows.length;
        int w = 0;
        for (String row : rows) {
            w = Math.max(w, row.length());
        }
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                char c = x < rows[y].length() ? rows[y].charAt(x) : '.';
                bmp.setPixel(x, y, c < 128 ? palette[c] : 0);
            }
        }
        return bmp;
    }

    // ---------- Outils de grille ----------

    private static char[] pad(String row, int width) {
        char[] r = new char[width];
        for (int x = 0; x < width; x++) {
            r[x] = x < row.length() ? row.charAt(x) : '.';
        }
        return r;
    }

    private static char[][] toGrid(String[] rows, int width) {
        char[][] g = new char[rows.length][];
        for (int y = 0; y < rows.length; y++) {
            g[y] = pad(rows[y], width);
        }
        return g;
    }

    private static String[] fromGrid(char[][] g) {
        String[] rows = new String[g.length];
        for (int y = 0; y < g.length; y++) {
            rows[y] = new String(g[y]);
        }
        return rows;
    }
}