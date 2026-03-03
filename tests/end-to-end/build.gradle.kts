plugins {
    kotlin("jvm") version "1.9.21"
    idea
    id("com.github.ben-manes.versions") version "0.47.0"
    id("net.serenity-bdd.serenity-gradle-plugin") version "4.0.1"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
}

group = "org.hyperledger.identus"
version = "1.0-SNAPSHOT"

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/input-output-hk/atala-automation/")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
    maven {
        url = uri("https://maven.pkg.github.com/LF-Decentralized-Trust-labs/aries-uniffi-wrappers")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    testImplementation("org.hyperledger.identus:cloud-agent-client:2.1.0")
    testImplementation("org.hyperledger.identus:sdk:latest")
    testImplementation("io.iohk.atala:atala-automation:0.3.2")
    testImplementation("app.cash.sqldelight:sqlite-driver:2.0.2")
    testImplementation("io.ktor:ktor-client-core-jvm:2.3.12")
}

tasks.register<Delete>("cleanTarget") {
    delete("target")
}

tasks.test {
    dependsOn("cleanTarget")
    testLogging.showStandardStreams = true
    systemProperty("cucumber.filter.tags", System.getProperty("tags") ?: "")
    filter {
        includeTestsMatching("org.hyperledger.identus.walletsdk.TestSuite")
    }
}

kotlin {
    jvmToolchain(21)
}

ktlint {
    verbose.set(true)
    outputToConsole.set(true)
    filter {
        exclude(
            "build/generated-src/**",
            "**/generated/**",
            "**/generated-src/**",
            "build/**",
            "build/generated/**"
        )
        exclude {
            it.file.path.contains("generated-src") ||
                it.file.toString().contains("generated") ||
                it.file.path.contains("generated")
        }
    }
}
