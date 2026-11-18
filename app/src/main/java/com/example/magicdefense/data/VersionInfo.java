package com.example.magicdefense.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Contenu du fichier apk/version.json publié sur GitHub, par exemple :
// {"versionCode": 4, "versionName": "2.2"}
// (Java pur, sans Android : testé par UpdateCheckerTest)
public final class VersionInfo {
    private static final Pattern CODE = Pattern.compile("\"versionCode\"\\s*:\\s*(\\d+)");
    private static final Pattern NAME = Pattern.compile("\"versionName\"\\s*:\\s*\"([^\"]*)\"");

    public final int versionCode;
    public final String versionName;

    public VersionInfo(int versionCode, String versionName) {
        this.versionCode = versionCode;
        this.versionName = versionName;
    }

    // Lit le JSON ; retourne null s'il est vide ou abîmé (pas de versionCode)
    public static VersionInfo parse(String json) {
        if (json == null) {
            return null;
        }
        Matcher code = CODE.matcher(json);
        if (!code.find()) {
            return null;
        }
        int versionCode;
        try {
            versionCode = Integer.parseInt(code.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
        Matcher name = NAME.matcher(json);
        String versionName = name.find() ? name.group(1) : String.valueOf(versionCode);
        return new VersionInfo(versionCode, versionName);
    }

    // La version publiée est-elle plus récente que celle installée ?
    public boolean isNewerThan(long installedVersionCode) {
        return versionCode > installedVersionCode;
    }
}
