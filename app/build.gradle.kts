import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.chaquo)
}

android {
    val appNamespace = "${project.group}.android"

    namespace = appNamespace
    compileSdk = 37

    val abis = listOf("arm64-v8a", "x86_64")
    val cmakeVersion = "4.1.2"
    ndkVersion = "29.0.14206865"

    defaultConfig {
        applicationId = "app.ravyn.music"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        minSdk = 24
        targetSdk = 37

        versionCode = System.getenv("ANDROID_VERSION_CODE")?.toIntOrNull() ?: 25
        versionName = project.version.toString()

        multiDexEnabled = true

        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += abis
        }

        @Suppress("UnstableApiUsage")
        externalNativeBuild {
            cmake {
                arguments += listOf("-DANDROID_STL=c++_static")
            }
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            isUniversalApk = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = System.getenv("ANDROID_RELEASE_KEYSTORE")?.let { file(it) }
            storePassword = System.getenv("ANDROID_RELEASE_STORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_RELEASE_KEY_ALIAS")
            keyPassword = System.getenv("ANDROID_RELEASE_KEY_PASSWORD")
        }
        create("ci") {
            storeFile = System.getenv("ANDROID_NIGHTLY_KEYSTORE")?.let { file(it) }
            storePassword = System.getenv("ANDROID_NIGHTLY_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("ANDROID_NIGHTLY_KEYSTORE_ALIAS")
            keyPassword = System.getenv("ANDROID_NIGHTLY_KEYSTORE_PASSWORD")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            manifestPlaceholders["appName"] = "Music Player Debug"
        }

        release {
            if (System.getenv("ANDROID_RELEASE_KEYSTORE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            versionNameSuffix = "-RELEASE"
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders["appName"] = "@string/product_name"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        create("nightly") {
            initWith(getByName("release"))
            matchingFallbacks += "release"

            applicationIdSuffix = ".nightly"
            versionNameSuffix = "-NIGHTLY"
            manifestPlaceholders["appName"] = "Music Player Nightly"
            signingConfig = signingConfigs.findByName("ci")
        }
    }

    buildFeatures {
        buildConfig = true
        resValues = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    packaging {
        resources.excludes.add("META-INF/**/*")
        jniLibs.useLegacyPackaging = true
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }

    externalNativeBuild {
        cmake {
            version = cmakeVersion
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }
}

// QuickJS is an executable disguised as a native library so Android installs
// it in nativeLibraryDir with executable permissions. It must be staged BEFORE
// mergeJniLibFolders/package, rather than in assemble.doFirst (which runs last).
android.buildTypes.configureEach {
    val variantName = name
    val capitalized = variantName.replaceFirstChar { it.uppercase() }
    val stagedDirectory = layout.buildDirectory.dir("generated/quickjs/$variantName")
    android.sourceSets.getByName(variantName).jniLibs.srcDir(stagedDirectory.get().asFile)
    val stageQuickJs = tasks.register<Sync>("stage${capitalized}QuickJs") {
        dependsOn("externalNativeBuild$capitalized")
        from(layout.projectDirectory.dir(".cxx")) {
            include("${if (variantName == "debug") "Debug" else "RelWithDebInfo"}/**/bin/*/qjs")
            eachFile { relativePath = RelativePath(true, file.parentFile.name, "libqjs.so") }
            includeEmptyDirs = false
        }
        into(stagedDirectory)
        doLast {
            listOf("arm64-v8a", "x86_64").forEach { abi ->
                check(stagedDirectory.get().file("$abi/libqjs.so").asFile.isFile) {
                    "Missing QuickJS executable for $abi"
                }
            }
        }
    }
    tasks.configureEach {
        if (name == "merge${capitalized}JniLibFolders") dependsOn(stageQuickJs)
    }
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())

    compilerOptions {
        languageVersion.set(KotlinVersion.KOTLIN_2_5)

        freeCompilerArgs.addAll(
            "-Xconsistent-data-class-copy-visibility"
        )
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

composeCompiler {
    if (project.findProperty("enableComposeCompilerReports") == "true") {
        val dest = layout.buildDirectory.dir("compose_metrics")
        metricsDestination = dest
        reportsDestination = dest
    }
}

chaquopy {
    defaultConfig {
        version = "3.14"
        pip {
            install("yt-dlp>=2026.08.19")
            install("yt-dlp-ejs>=0.8.0")
            install("pip")
        }
    }
}

dependencies {
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    coreLibraryDesugaring(libs.desugaring)

    implementation(projects.compose.persist)
    implementation(projects.compose.preferences)
    implementation(projects.compose.routing)
    implementation(projects.compose.reordering)

    implementation(fileTree(projectDir.resolve("vendor")))

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.activity)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.util)
    implementation(libs.compose.shimmer)
    implementation(libs.compose.lottie)
    implementation(libs.compose.material3)

    implementation(libs.coil.compose)
    implementation(libs.coil.ktor)

    implementation(libs.palette)
    implementation(libs.monet)
    runtimeOnly(projects.core.materialCompat)

    implementation(libs.exoplayer)
    implementation(libs.exoplayer.workmanager)
    implementation(libs.media3.session)
    implementation(libs.media)

    implementation(libs.workmanager)
    implementation(libs.workmanager.ktx)

    implementation(libs.credentials)
    implementation(libs.credentials.play)

    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.immutable)
    implementation(libs.kotlin.datetime)

    implementation(libs.room)
    ksp(libs.room.compiler)

    implementation(libs.log4j)
    implementation(libs.slf4j)
    implementation(libs.logback)

    implementation(projects.providers.github)
    implementation(projects.providers.innertube)
    implementation(projects.providers.kugou)
    implementation(projects.providers.lrclib)
    implementation(projects.providers.piped)
    implementation(projects.providers.sponsorblock)
    implementation(projects.providers.translate)
    implementation(projects.core.data)
    implementation(projects.core.ui)
}
