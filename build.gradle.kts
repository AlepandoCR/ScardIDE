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



java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}


application {
    mainClass.set("dev.alepando.MainKt")
    mainModule.set("ScardIDE.main")
}


jlink {
    mergedModule {
        enabled = true
    }

    forceMerge("javafx", "org.fxmisc")

    launcher {
        name = "ScardIDE"
    }

    jpackage {
        imageName = "ScardIDE"
        installerType = "exe"
        appVersion = "1.0.0"
        vendor = "Alepando"
        installerName = "ScardIDE Installer"
        installerOptions.addAll(listOf("--win-menu", "--win-shortcut"))
    }

}



tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "dev.alepando.MainKt"
        )
    }
}
