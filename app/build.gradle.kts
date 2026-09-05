import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.navigation.safeargs)
}

// Local, git-ignored file holding release signing + AdMob secrets for local builds.
// See secrets.properties.example for the required keys. CI supplies the same
// values via environment variables / GitHub Secrets instead of this file.
val secretsPropertiesFile = rootProject.file("secrets.properties")
val secretsProperties = Properties().apply {
    if (secretsPropertiesFile.exists()) {
        FileInputStream(secretsPropertiesFile).use { load(it) }
    }
}

fun secret(key: String, envKey: String = key): String {
    return secretsProperties.getProperty(key)
        ?: System.getenv(envKey)
        ?: ""
}

android {
    namespace = "com.phonedoctor.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.phonedoctor.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true

        // Default AdMob placeholder used for non-release builds.
        manifestPlaceholders["admobAppId"] = secret(
            "ADMOB_APP_ID",
            "ADMOB_APP_ID"
        ).ifBlank { "ca-app-pub-3940256099942544~3347511713" } // Google's public AdMob test app ID
    }

    signingConfigs {
        val storeFilePath = secret("KEYSTORE_FILE", "KEYSTORE_FILE")
        val storePasswordValue = secret("KEYSTORE_PASSWORD", "KEYSTORE_PASSWORD")
        val keyAliasValue = secret("KEY_ALIAS", "KEY_ALIAS")
        val keyPasswordValue = secret("KEY_PASSWORD", "KEY_PASSWORD")

        if (storeFilePath.isNotBlank() && file(storeFilePath).exists()) {
            create("release") {
                storeFile = file(storeFilePath)
                storePassword = storePasswordValue
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isDebuggable = true
            buildConfigField("boolean", "SHOW_ADS", "false")
            buildConfigField(
                "String",
                "BANNER_AD_UNIT_ID",
                "\"ca-app-pub-3940256099942544/9214589741\""
            )
            buildConfigField(
                "String",
                "INTERSTITIAL_AD_UNIT_ID",
                "\"ca-app-pub-3940256099942544/1033173712\""
            )
            buildConfigField(
                "String",
                "REWARDED_AD_UNIT_ID",
                "\"ca-app-pub-3940256099942544/5224354917\""
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["admobAppId"] = "ca-app-pub-5961173995415325~9886076332"
            buildConfigField("boolean", "SHOW_ADS", "true")
            buildConfigField(
                "String",
                "BANNER_AD_UNIT_ID",
                "\"ca-app-pub-5961173995415325/4633749652\""
            )
            buildConfigField(
                "String",
                "INTERSTITIAL_AD_UNIT_ID",
                "\"ca-app-pub-5961173995415325/4176630251\""
            )
            buildConfigField(
                "String",
                "REWARDED_AD_UNIT_ID",
                "\"${secret("REWARDED_AD_UNIT_ID").ifBlank { "ca-app-pub-3940256099942544/5224354917" }}\""
            )

            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = false
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.viewpager2)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.guava)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.play.services.ads)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.arch.core.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
