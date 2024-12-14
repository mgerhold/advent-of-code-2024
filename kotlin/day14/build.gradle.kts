plugins {
    kotlin("jvm") version "2.0.21"
    application
    id("org.openjfx.javafxplugin") version "0.1.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.openjfx:javafx-base:20")
    implementation("org.openjfx:javafx-controls:20")
    implementation("org.openjfx:javafx-graphics:20")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(19)
}
javafx {
    version = "23.0.1"
    modules = listOf("javafx.controls", "javafx.fxml")
}
application {
    mainClass.set("org.example.MainKt") // Replace with your main class
    applicationDefaultJvmArgs = listOf(
        "--module-path",
        System.getProperty("javafx.module.path"), // Set this to your JavaFX library path
        "--add-modules",
        "javafx.controls,javafx.fxml" // Add other modules as needed
    )
}
