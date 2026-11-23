package com.example.magicdefense.model;

// Chiffres romains (paliers des succès, cycles des biomes : « Volcan II », « Chasseur de gobelins III »...)
public final class Roman {
    private static final int[] VALUES = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
    private static final String[] SYMBOLS = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};

    private Roman() {
    }

    // 1 → « I », 4 → « IV », 2024 → « MMXXIV ». Hors de 1 à 3999 : le nombre en chiffres arabes.
    public static String of(int n) {
        if (n < 1 || n > 3999) {
            return String.valueOf(n);
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < VALUES.length; i++) {
            while (n >= VALUES[i]) {
                sb.append(SYMBOLS[i]);
                n -= VALUES[i];
            }
        }
        return sb.toString();
    }
}
