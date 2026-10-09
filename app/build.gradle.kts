plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "fr.jakez.todo"
    compileSdk = 34

    defaultConfig {
        applicationId = "fr.jakez.todo"
        minSdk = 26
        targetSdk = 34
        // Chaque compilation GitHub incrémente la version : la mise à jour s'installe par-dessus.
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1.0." + (System.getenv("GITHUB_RUN_NUMBER") ?: "0")
    }

    // Clé fixe, versionnée dans le dépôt : chaque nouvel APK remplace l'ancien sans perdre les tâches.
    signingConfigs {
        getByName("debug") {
            storeFile = file("todo.keystore")
            storePassword = "android"
            keyAlias = "todo"
            keyPassword = "android"
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}
