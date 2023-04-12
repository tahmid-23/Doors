plugins {
    application
}

group = "com.github.tahmid_23"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository {
            maven("https://dl.cloudsmith.io/public/steanky/element/maven/")
        }
        filter {
            includeModule("com.github.steanky", "element-core")
        }
    }
    exclusiveContent {
        forRepository {
            maven("https://dl.cloudsmith.io/public/steanky/ethylene/maven/")
        }
        filter {
            includeModuleByRegex("com\\.github\\.steanky", "ethylene-.+")
        }
    }
    exclusiveContent {
        forRepository {
            maven("https://dl.cloudsmith.io/public/steanky/toolkit/maven/")
        }
        filter {
            includeModuleByRegex("com\\.github\\.steanky", "toolkit-.+")
        }
    }
    maven("https://jitpack.io/")
}

dependencies {
    implementation("com.github.Minestom.Minestom:Minestom:8ad2c7701f")
    implementation("com.github.steanky:element-core:0.14.1")
    implementation("com.github.steanky:ethylene-mapper:0.19.1")
    implementation("com.github.steanky:ethylene-toml:0.19.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
}

application {
    mainClass.set("com.github.tahmid_23.doors.Main")
}

(tasks.run) {
    setWorkingDir("run/")
}

tasks.test {
    useJUnitPlatform()
}