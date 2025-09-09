plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    // Gradle core plugin 'maven-publish' tidak bisa via alias versi; apply langsung di bawah:
    id("maven-publish")
}

android {
    namespace = "com.mascill.serialport"
    compileSdk = 36

//    ndkVersion = providers.gradleProperty("ANDROID_NDK_VERSION").get()

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
       compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    // Artefak untuk publish (JitPack)
    publishing {
        singleVariant("release") {
            withSourcesJar()
            // withJavadocJar() opsional untuk Android; kalau bermasalah, skip saja
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
}

version = (findProperty("VERSION_NAME") ?: "0.0.0").toString()

// ---- Publish untuk JitPack ----
// (JitPack akan override koordinat saat konsumsi via tag, tapi bagus untuk konsistensi & lokal)
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.chairoel"
                artifactId = "serial-port-lib"
                version = project.version.toString()
            }
        }
    }
}