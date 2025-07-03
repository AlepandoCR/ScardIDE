import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import java.io.File
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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

val javafxVersion = "24.0.1"

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.openjfx:javafx-controls:$javafxVersion")
    implementation("org.fxmisc.richtext:richtextfx:0.10.9")
}

javafx {
    version = javafxVersion
    modules = listOf("javafx.controls", "javafx.graphics")
}

sourceSets {
    main {
        java {

            srcDirs("src/main/java")
        }
        kotlin {

            srcDirs("src/main/kotlin")
        }

        resources {
            srcDirs("src/main/resources")
        }
    }
}

tasks.named<Copy>("processResources") {
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(tasks.named("compileKotlin"))


    val currentModuleName = "ScardIDE.main" // As defined in jlink
    val compileKotlinTask = tasks.named<KotlinCompile>("compileKotlin").get()


    destinationDirectory.set(compileKotlinTask.destinationDirectory)


    val dependencyModulePath = project.configurations.runtimeClasspath.get().asPath

    val javaSrcDir = sourceSets.main.get().java.srcDirs.first().absolutePath
    val kotlinOutputDir = compileKotlinTask.destinationDirectory.get().asFile

    options.compilerArgs = mutableListOf(
        "--module-path", dependencyModulePath,

        "--patch-module", "$currentModuleName=$javaSrcDir${File.pathSeparator}${kotlinOutputDir.absolutePath}"
    )

    classpath = project.configurations.compileClasspath.get()
}


application {
    mainClass.set("dev.alepando.MainKt")
    mainModule.set("ScardIDE.main")
}

tasks.named<JavaExec>("run") {
    val modulePathString = sourceSets.main.get().runtimeClasspath.files.joinToString(File.pathSeparator) { it.absolutePath }

    jvmArgs = listOf(
        "--module-path", modulePathString,
        "--add-modules", "ALL-MODULE-PATH",
        "--add-opens", "ScardIDE.main/dev.alepando=ALL-UNNAMED"
    )
}


jlink {
    moduleName.set("ScardIDE.main")



    forceMerge("javafx", "org.fxmisc")

    launcher {
        name = "ScardIDE"
    }

    jpackage {
        imageName = "ScardIDE"
        installerType = "msi"
        appVersion = "1.0.0"
        vendor = "Alepando"
        installerName = "ScardIDE Installer"
    }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(
            "Main-Class" to "dev.alepando.MainKt"
        )
    }
}



