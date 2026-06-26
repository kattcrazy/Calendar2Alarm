plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
}

import com.android.build.api.dsl.ApplicationExtension
import java.util.Properties

subprojects {
    plugins.withId("com.android.application") {
        extensions.configure<ApplicationExtension> {
            lint {
                disable += "NullSafeMutableLiveData"
            }
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (!keystorePropertiesFile.exists()) return@configure
            val keystoreProperties = Properties().apply {
                keystorePropertiesFile.inputStream().use { load(it) }
            }
            val storeFilePath = keystoreProperties.getProperty("storeFile")
                ?: error("keystore.properties is missing storeFile")
            val keystoreFile = rootProject.file(storeFilePath)
            if (!keystoreFile.exists()) {
                error("Release keystore not found at ${keystoreFile.absolutePath}")
            }
            signingConfigs {
                create("release") {
                    keyAlias = keystoreProperties.getProperty("keyAlias")
                        ?: error("keystore.properties is missing keyAlias")
                    keyPassword = keystoreProperties.getProperty("keyPassword")
                        ?: error("keystore.properties is missing keyPassword")
                    storeFile = keystoreFile
                    storePassword = keystoreProperties.getProperty("storePassword")
                        ?: error("keystore.properties is missing storePassword")
                }
            }
            buildTypes {
                getByName("release") {
                    signingConfig = signingConfigs.getByName("release")
                }
            }
        }
    }
}

gradle.taskGraph.whenReady {
    val needsSigning = hasTask(":wear:assembleRelease") || hasTask(":wear:bundleRelease")
    if (!needsSigning) return@whenReady

    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreFile = rootProject.file("upload-keystore.jks")
    if (!keystorePropertiesFile.exists() || !keystoreFile.exists()) {
        error(
            "Release builds must be signed with upload-keystore.jks " +
                "(Play Console registered key). Copy keystore.properties.example and keep your local keystore.",
        )
    }
}
