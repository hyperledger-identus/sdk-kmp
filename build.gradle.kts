import org.gradle.internal.os.OperatingSystem

val groupId = "org.hyperledger.identus"
val os: OperatingSystem = OperatingSystem.current()

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.mavenPublish) apply false
    alias(libs.plugins.dokka) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.ktlint)
}

buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
        gradlePluginPortal()
    }
}

kotlin {
    jvmToolchain(21)
}

allprojects {
    this.group = groupId

    configurations.all {
        resolutionStrategy {
            eachDependency {
                if (requested.group == "org.bouncycastle") {
                    when (requested.name) {
                        "bcprov-jdk15on", "bcprov-jdk15to18" -> {
                            useTarget("org.bouncycastle:bcprov-jdk15on:1.68")
                        }
                    }
                } else if (requested.group == "com.nimbusds") {
                    // Making sure we are using the latest version of `nimbus-jose-jwt` instead if 9.25.6
                    useTarget("com.nimbusds:nimbus-jose-jwt:9.39")
                } else if (requested.group == "com.google.protobuf") {
                    // Because of Duplicate Classes issue happening on the sampleapp module
                    if (requested.name == "protobuf-javalite" || requested.name == "protobuf-java") {
                        useTarget("com.google.protobuf:protobuf-java:3.14.0")
                    }
                }
            }
        }
    }
}

tasks.register<Exec>("lintText") {
    group = "verification"
    description = "Lint text files (markdown, YAML, editorconfig)"
    commandLine(
        "bash",
        "-c",
        """
        EXIT_CODE=0
        echo "=== Markdown Lint ==="
        npx --yes markdownlint-cli2 '**/*.md' || EXIT_CODE=${'$'}?
        echo ""
        echo "=== YAML Lint ==="
        npx --yes yamllint-ts -c .yamllint.yml ${'$'}(find . \( -name '*.yml' -o -name '*.yaml' \) -not -path '*/node_modules/*' -not -path '*/.git/*' -not -path '*/build/*') || EXIT_CODE=${'$'}?
        echo ""
        echo "=== EditorConfig Check ==="
        npx --yes editorconfig-checker -exclude '\.git|node_modules|build|megalinter' || EXIT_CODE=${'$'}?
        exit ${'$'}EXIT_CODE
        """.trimIndent(),
    )
}

tasks.register<Exec>("lintTextFix") {
    group = "verification"
    description = "Auto-fix markdown formatting issues"
    commandLine(
        "npx",
        "--yes",
        "markdownlint-cli2",
        "**/*.md",
        "--fix",
    )
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

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
}
