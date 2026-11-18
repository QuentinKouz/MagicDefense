package com.example.magicdefense.data;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import androidx.core.content.pm.PackageInfoCompat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

// Mise à jour automatique : au lancement, on regarde sur GitHub (branche v2, dossier apk/)
// si une version plus récente existe. Si oui, on propose de la télécharger puis on ouvre
// l'installateur d'Android (l'utilisateur confirme ; la sauvegarde est gardée car la clé de
// signature est la même). Sans réseau ou en cas d'erreur : rien, le jeu démarre normalement.
// Tout le réseau se fait sur un fil à part : la boucle du jeu n'est jamais bloquée.
public class UpdateChecker {
    private static final String BASE_URL = "https://raw.githubusercontent.com/QuentinKouz/MagicDefense/v2/apk/";
    private static final String VERSION_URL = BASE_URL + "version.json";
    private static final String APK_URL = BASE_URL + "MagicDefense-v2.apk";
    private static final int TIMEOUT_MS = 8000;
    private static final String APK_DIR = "updates";            // sous-dossier du cache (voir file_paths.xml)
    private static final String APK_NAME = "MagicDefense-update.apk";

    private final Activity activity;
    private boolean checked = false;          // une seule vérification par lancement
    private boolean waitingPermission = false; // parti dans les réglages « sources inconnues »
    private VersionInfo available;

    public UpdateChecker(Activity activity) {
        this.activity = activity;
    }

    // ---------- 1. Vérification (au lancement) ----------
    public void check() {
        if (checked) {
            return;
        }
        checked = true;
        new Thread(() -> {
            try {
                // Paramètre unique : évite une ancienne version gardée en cache par le CDN
                String json = readText(VERSION_URL + "?t=" + System.currentTimeMillis());
                VersionInfo info = VersionInfo.parse(json);
                if (info != null && info.isNewerThan(installedVersionCode())) {
                    activity.runOnUiThread(() -> askToUpdate(info));
                }
            } catch (Exception ignored) {
                // pas de réseau, GitHub indisponible... : on joue normalement
            }
        }, "update-check").start();
    }

    private long installedVersionCode() throws PackageManager.NameNotFoundException {
        PackageInfo info = activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0);
        return PackageInfoCompat.getLongVersionCode(info);
    }

    private void askToUpdate(VersionInfo info) {
        if (activity.isFinishing()) {
            return;
        }
        available = info;
        new AlertDialog.Builder(activity)
                .setTitle("Mise à jour")
                .setMessage("Nouvelle version " + info.versionName + " disponible.\n"
                        + "Votre progression sera conservée.")
                .setPositiveButton("Mettre à jour", (d, w) -> startUpdate())
                .setNegativeButton("Plus tard", null)
                .show();
    }

    // ---------- 2. Autorisation d'installer (Android 8 et plus) ----------
    private void startUpdate() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && !activity.getPackageManager().canRequestPackageInstalls()) {
            // Il faut autoriser le jeu à installer des applis : on ouvre le bon réglage,
            // et on reprend dans onResume() quand le joueur revient
            waitingPermission = true;
            Toast.makeText(activity, "Autorisez l'installation, puis revenez dans le jeu",
                    Toast.LENGTH_LONG).show();
            try {
                activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + activity.getPackageName())));
            } catch (Exception e) {
                waitingPermission = false;
            }
            return;
        }
        download();
    }

    // À appeler depuis onResume() de l'activité
    public void onResume() {
        if (!waitingPermission) {
            return;
        }
        waitingPermission = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && activity.getPackageManager().canRequestPackageInstalls()) {
            download();
        } else {
            Toast.makeText(activity, "Mise à jour annulée", Toast.LENGTH_SHORT).show();
        }
    }

    // ---------- 3. Téléchargement, avec une barre de progression ----------
    private void download() {
        if (available == null || activity.isFinishing()) {
            return;
        }
        float d = activity.getResources().getDisplayMetrics().density;
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * d);
        box.setPadding(pad, pad, pad, pad);
        TextView label = new TextView(activity);
        label.setText("Téléchargement de la version " + available.versionName + "…");
        ProgressBar bar = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        box.addView(label);
        box.addView(bar);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Mise à jour")
                .setView(box)
                .setCancelable(false)
                .show();

        new Thread(() -> {
            File apk = new File(new File(activity.getCacheDir(), APK_DIR), APK_NAME);
            boolean ok = false;
            try {
                downloadTo(APK_URL + "?t=" + System.currentTimeMillis(), apk,
                        percent -> activity.runOnUiThread(() -> bar.setProgress(percent)));
                ok = true;
            } catch (Exception ignored) {
                // échec : on prévient, le jeu continue
            }
            boolean success = ok;
            activity.runOnUiThread(() -> {
                if (!activity.isFinishing()) {
                    dialog.dismiss();
                }
                if (success) {
                    install(apk);
                } else {
                    Toast.makeText(activity, "Échec du téléchargement de la mise à jour",
                            Toast.LENGTH_LONG).show();
                }
            });
        }, "update-download").start();
    }

    // ---------- 4. Installateur d'Android ----------
    private void install(File apk) {
        try {
            Uri uri = FileProvider.getUriForFile(activity, activity.getPackageName() + ".fileprovider", apk);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(activity, "Impossible d'ouvrir l'installateur", Toast.LENGTH_LONG).show();
        }
    }

    // ---------- Réseau ----------
    private interface Progress {
        void onPercent(int percent);
    }

    private static HttpURLConnection open(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(TIMEOUT_MS);
        c.setReadTimeout(TIMEOUT_MS);
        c.setUseCaches(false);
        c.setInstanceFollowRedirects(true);
        if (c.getResponseCode() != HttpURLConnection.HTTP_OK) {
            c.disconnect();
            throw new IOException("HTTP " + c.getResponseCode());
        }
        return c;
    }

    private static String readText(String url) throws IOException {
        HttpURLConnection c = open(url);
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0 && out.size() < 64 * 1024) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            c.disconnect();
        }
    }

    private static void downloadTo(String url, File dest, Progress progress) throws IOException {
        File dir = dest.getParentFile();
        if (dir != null && !dir.exists() && !dir.mkdirs()) {
            throw new IOException("dossier du cache");
        }
        HttpURLConnection c = open(url);
        long total = c.getContentLengthLong();
        File part = new File(dest.getPath() + ".part");
        try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(part)) {
            byte[] buf = new byte[16 * 1024];
            long done = 0;
            int last = -1;
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                done += n;
                if (total > 0) {
                    int percent = (int) (done * 100 / total);
                    if (percent != last) {
                        last = percent;
                        progress.onPercent(percent);
                    }
                }
            }
        } finally {
            c.disconnect();
        }
        if (dest.exists() && !dest.delete()) {
            throw new IOException("ancien fichier");
        }
        if (!part.renameTo(dest)) {
            throw new IOException("renommage");
        }
    }
}
