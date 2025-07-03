plugins {
    kotlin("jvm") version "2.0.21"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.beryx.jlink") version "3.0.1"
}

group = "dev.alepando"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.openjfx:javafx-controls:24.0.1")
    implementation("org.fxmisc.richtext:richtextfx:0.10.9")
}

javafx {
    version = "24.0.1"
    modules = listOf("javafx.controls")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(22))
}

application {
    mainClass.set("dev.alepando.MainKt")
}

jlink {
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))
    extraModulePaths.add("javafx.controls")
    extraModulePaths.add("javafx.graphics")
    launcher {
        name = "ScardIDE"
    }
    jpackage {
        imageName = "MiApp"
        installerType = "exe"
    }
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "dev.alepando.MainKt"
        )
    }
}
