import com.android.build.gradle.tasks.PackageAndroidArtifact
import com.android.build.gradle.tasks.SourceJarTask
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URL

val currentModuleName: String = "sdk"

plugins {
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlinKover)
    alias(libs.plugins.mavenPublish)
    alias(libs.plugins.ktlint)
    id("org.gradle.signing")
}

if (System.getenv().containsKey("OSSRH_GPG_SECRET_KEY")) {
    signing {
        useInMemoryPgpKeys(
            System.getenv("OSSRH_GPG_SECRET_KEY"),
            System.getenv("OSSRH_GPG_SECRET_KEY_PASSWORD")
        )
        sign(publishing.publications)
    }
}

kover {
    useJacoco("0.8.11")
    excludeJavaCode()
    excludeInstrumentation {
        packages("androidx.test.espresso", "androidx.test.ext")
    }
}

koverReport {
    filters {
        excludes {
            packages(
                "org.hyperledger.identus.protos",
                "org.hyperledger.identus.walletsdk.domain",
                "org.hyperledger.identus.walletsdk.pluto.data"
            )
        }
    }

    defaults {
        xml {
            setReportFile(layout.buildDirectory.file("reports/jvm/result.xml"))
        }
        html {
            title = "Wallet SDK - JVM"
            setReportDir(layout.buildDirectory.dir("reports/jvm/html"))
        }
    }

    androidReports("release") {
        xml {
            setReportFile(layout.buildDirectory.file("reports/android/result.xml"))
        }
        html {
            title = "SDK - Android"
            setReportDir(layout.buildDirectory.dir("reports/android/html"))
        }
    }
}

/**
 * The `javadocJar` variable is used to register a `Jar` task to generate a Javadoc JAR file.
 * The Javadoc JAR file is created with the classifier "javadoc" and it includes the HTML documentation generated
 * by the `dokkaHtml` task.
 */
val javadocJar by tasks.registering(Jar::class) {
    archiveClassifier.set("javadoc")
    from(tasks.dokkaHtml)
}

kotlin {
    androidTarget {
        publishAllLibraryVariants()
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
    }

    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "17"
            }
        }
        publishing {
            publications {
                withType<MavenPublication> {
                    artifact(javadocJar)
                }
            }
        }
    }
    applyDefaultHierarchyTemplate()

    sourceSets {
        val commonMain by getting {
            kotlin.srcDir("${project(":protosLib").layout.buildDirectory.asFile.get()}/generated/source/proto/main/kotlin")
            resources.srcDir("${project(":protosLib").projectDir}/src/main")
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.kotlinx.serialization.json)

                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.ktor.websockets)

                implementation(libs.dependencies.didpeer)
                implementation(libs.dependencies.apollo)

                implementation(libs.dependencies.kotlincrypto.hash.sha)

                implementation(libs.pbandk.runtime)

                implementation("${libs.dependencies.didcomm.get()}") {
                    exclude("com.google.protobuf")
                }

//                implementation(libs.dependencies)
                implementation("${libs.dependencies.protoc.get()}") {
                    exclude("com.google.protobuf")
                }

                implementation(libs.pbandk.runtime)
                implementation(libs.sqldelight.couroutines.extensions)

                api(libs.lighthouse.logging)

                implementation(libs.dependencies.anoncreds)
                implementation(libs.dependencies.ionspin.bignum)
                implementation(libs.dependencies.bouncycastle)
                implementation("${libs.dependencies.eudi.sdjwt.get()}") {
                    exclude(group = "com.nimbusds", module = "nimbus-jose-jwt")
                }
                implementation(kotlin("reflect"))

                implementation(libs.dependencies.json.ld)
                implementation(libs.dependencies.json)
                implementation(libs.dependencies.setl.rdf.urdna)
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(libs.kotlinx.coroutines.test)
                implementation(libs.ktor.client.mock)
                implementation(libs.mockito.core)
                implementation(libs.mockito.kotlin)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.ktor.client.okhttp)
                implementation(libs.ktor.client.java)
                implementation(libs.sqldelight.sqlite.driver)
            }
        }
        val jvmTest by getting
        val androidMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.android)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.ktor.client.android)
                implementation(libs.sqldelight.android.driver)
            }
        }
        val androidInstrumentedTest by getting {
            dependencies {
                dependsOn(commonTest)
                implementation(libs.androidx.test.espresso)
                implementation(libs.androidx.test.junit)
            }
        }
        /*
        Not going to support JS for the time being
        val jsMain by getting
        val jsTest by getting
         */

        all {
            languageSettings {
                optIn("kotlin.RequiresOptIn")
                optIn("kotlin.ExperimentalStdlibApi")
            }
        }
    }
}

android {
    compileSdk = 34
    namespace = "org.hyperledger.identus"
    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    /**
     * Because Software Components will not be created automatically for Maven publishing from
     * Android Gradle Plugin 8.0. To opt-in to the future behavior, set the Gradle property android.
     * disableAutomaticComponentCreation=true in the `gradle.properties` file or use the new
     * publishing DSL.
     */
    publishing {
        multipleVariants {
            withSourcesJar()
            withJavadocJar()
            allVariants()
        }
    }

    packaging {
        resources {
            merges += "**/**.proto"
        }
    }
}

sqldelight {
    databases {
        create("SdkPlutoDb") {
            packageName.set("org.hyperledger.identus.walletsdk")
            srcDirs.setFrom("src/commonMain/sqldelight")
        }
    }
}

// Dokka implementation
tasks.withType<DokkaTask>().configureEach {
    moduleName.set(currentModuleName)
    moduleVersion.set(rootProject.version.toString())
    description = "This is a Kotlin Multiplatform implementation of Identus SDK KMP"
    dokkaSourceSets {
        configureEach {
            jdkVersion.set(17)
            languageVersion.set("1.9.22")
            apiVersion.set("2.0")
            includes.from(
                "docs/EdgeAgentSDK.md",
                "docs/Apollo.md",
                "docs/Castor.md",
                "docs/Mercury.md",
                "docs/Pluto.md",
                "docs/Pollux.md",
                "docs/EdgeAgent.md",
                "docs/BackUp.md"
            )
            sourceLink {
                localDirectory.set(projectDir.resolve("src"))
                remoteUrl.set(URL("https://github.com/hyperledger-identus/sdk-kmp/tree/main/src"))
                remoteLineSuffix.set("#L")
            }
            externalDocumentationLink {
                url.set(URL("https://kotlinlang.org/api/latest/jvm/stdlib/"))
            }
            externalDocumentationLink {
                url.set(URL("https://kotlinlang.org/api/kotlinx.serialization/"))
            }
            externalDocumentationLink {
                url.set(URL("https://api.ktor.io/"))
            }
            externalDocumentationLink {
                url.set(URL("https://kotlinlang.org/api/kotlinx-datetime/"))
                packageListUrl.set(URL("https://kotlinlang.org/api/kotlinx-datetime/"))
            }
            externalDocumentationLink {
                url.set(URL("https://kotlinlang.org/api/kotlinx.coroutines/"))
            }
        }
    }
}

val buildProtoLibsGen: Task by tasks.creating {
    group = "build"
    this.dependsOn(":protosLib:generateProto")
}

afterEvaluate {
    tasks.withType<PublishToMavenRepository> {
        dependsOn(tasks.withType<Sign>())
    }
    tasks.withType<PublishToMavenLocal> {
        dependsOn(tasks.withType<Sign>())
    }
    tasks.getByName("runKtlintCheckOverCommonMainSourceSet") {
        dependsOn(buildProtoLibsGen)
    }
    tasks.getByName("build") {
        dependsOn(buildProtoLibsGen)
    }
    tasks.withType<KotlinCompile> {
        dependsOn(buildProtoLibsGen)
    }
    tasks.withType<ProcessResources> {
        dependsOn(buildProtoLibsGen)
    }
    tasks.withType<SourceJarTask> {
        dependsOn(buildProtoLibsGen)
    }
    tasks.withType<org.gradle.jvm.tasks.Jar> {
        dependsOn(buildProtoLibsGen)
    }
    tasks.withType<PackageAndroidArtifact> {
        dependsOn(buildProtoLibsGen)
    }
    tasks.named("packageDebugResources") {
        dependsOn(buildProtoLibsGen)
    }
    tasks.named("packageReleaseResources") {
        dependsOn(buildProtoLibsGen)
    }
    tasks.named("androidReleaseSourcesJar") {
        dependsOn(buildProtoLibsGen)
    }
    tasks.named("androidDebugSourcesJar") {
        dependsOn(buildProtoLibsGen)
    }
    tasks.named("jvmSourcesJar") {
        dependsOn(buildProtoLibsGen)
    }
    tasks.named("sourcesJar") {
        dependsOn(buildProtoLibsGen)
    }
}

mavenPublishing {
    val shouldAutoRelease = project.findProperty("autoRelease")?.toString()?.toBoolean() ?: false
    val artifactId = project.name
    val version = project.version.toString()
    publishToMavenCentral(automaticRelease = shouldAutoRelease)
    signAllPublications()
    coordinates(group.toString(), artifactId, version.toString())
    pom {
        name.set("SDK")
        description.set("Identus Kotlin Multiplatform (Android/JVM) SDK")
        url.set("https://hyperledger-identus.github.io/docs/")
        organization {
            name.set("Hyperledger")
            url.set("https://hyperledger.org/")
        }
        issueManagement {
            system.set("Github")
            url.set("https://github.com/hyperledger/identus-edge-agent-sdk-kmp")
        }
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("cristianIOHK")
                name.set("Cristian Gonzalez")
                email.set("cristian.castro@iohk.io")
                organization.set("IOG")
                roles.add("developer")
                url.set("https://github.com/cristianIOHK")
            }
            developer {
                id.set("hamada147")
                name.set("Ahmed Moussa")
                email.set("ahmed.moussa@iohk.io")
                organization.set("IOG")
                roles.add("developer")
                url.set("https://github.com/hamada147")
            }
            developer {
                id.set("elribonazo")
                name.set("Javier Ribó")
                email.set("javier.ribo@iohk.io")
                organization.set("IOG")
                roles.add("developer")
            }
            developer {
                id.set("amagyar-iohk")
                name.set("Allain Magyar")
                email.set("allain.magyar@iohk.io")
                organization.set("IOG")
                roles.add("qc")
            }
            developer {
                id.set("antonbaliasnikov")
                name.set("Anton Baliasnikov")
                email.set("anton.baliasnikov@iohk.io")
                organization.set("IOG")
                roles.add("qc")
            }
            developer {
                id.set("goncalo-frade-iohk")
                name.set("Gonçalo Frade")
                email.set("goncalo.frade@iohk.io")
                organization.set("IOG")
                roles.add("developer")
            }
        }
        scm {
            connection.set("scm:git:git://hyperledger-identus/sdk-kmp.git")
            developerConnection.set("scm:git:ssh://hyperledger-identus/sdk-kmp.git")
            url.set("https://github.com/hyperledger-identus/sdk-kmp")
        }
    }
}
