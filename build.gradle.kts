buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // We explicitly tell Gradle where to download the binary files
        classpath("com.android.tools.build:gradle:8.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.0")
    }
}

// We apply the plugins, but we remove the 'version' here because
// we defined it above in 'dependencies'
plugins {
    id("com.android.application") apply false
    id("org.jetbrains.kotlin.android") apply false
}
