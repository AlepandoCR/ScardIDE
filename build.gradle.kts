plugins {
    kotlin("jvm") version "2.0.21"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "dev.alepando"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.openjfx:javafx-controls:23")
    implementation("org.fxmisc.richtext:richtextfx:0.10.9")
}


javafx {
    version = "24.0.1"
    modules = listOf("javafx.controls")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

application {
    mainClass.set("dev.alepando.MainKt")
}