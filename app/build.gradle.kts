plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.magicdefense"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.magicdefense"
        minSdk = 27
        targetSdk = 37
        versionCode = 5
        versionName = "2.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Clé de signature partagée, versionnée dans le dépôt : tous les APK (quel que soit le PC qui les
    // construit) ont la même signature, donc Android accepte d'installer une nouvelle version
    // PAR-DESSUS l'ancienne, sans la désinstaller : la sauvegarde est conservée.
    // C'est la clé qui a signé l'APK de la V2, pour que la mise à jour depuis la V2 fonctionne aussi.
    signingConfigs {
        create("shared") {
            storeFile = file("signing/magicdefense.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("shared")
        }
        release {
            signingConfig = signingConfigs.getByName("shared")
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}