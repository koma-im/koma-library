import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

version = "0.9.23"

plugins {
    id("java")
    id("maven")
    id("org.jetbrains.kotlin.jvm") version "1.3.70"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.3.70"
    id("org.jetbrains.kotlin.kapt") version "1.3.70"
}

repositories {
    mavenLocal()
    jcenter()
    maven ("https://dl.bintray.com/jerady/maven")
    maven("https://dl.bintray.com/kittinunf/maven")
    maven("https://jitpack.io")
    maven("https://repo.maven.apache.org/maven2")
}

tasks {
    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }
    
    artifacts {
        archives(sourcesJar)
    }
}

dependencies {
    val ktorVersion = "1.3.1"
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.3")
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-runtime", "0.20.0")
    implementation("io.ktor", "ktor-client-okhttp", ktorVersion)
    implementation("io.ktor", "ktor-client-core", ktorVersion)
    implementation("io.ktor", "ktor-client-serialization-jvm", ktorVersion)
    implementation("io.github.microutils", "kotlin-logging", "1.7.8")
    implementation("org.slf4j", "slf4j-api", "1.8.0-beta2")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.2")
    testImplementation("com.squareup.okhttp3", "mockwebserver", "4.2.0")
    testImplementation("org.jetbrains.kotlinx", "kotlinx-coroutines-test", "1.3.3")
    testImplementation("org.jetbrains.kotlinx", "kotlinx-serialization-properties", "0.20.0")
    testRuntime("org.slf4j:slf4j-simple:1.8.0-beta2")
}

group = "io.github.koma-im"
description = "koma-library"
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    freeCompilerArgs = listOf(
            "-XXLanguage:+InlineClasses",
            "-Xuse-experimental=kotlin.contracts.ExperimentalContracts"
            , "-Xuse-experimental=kotlin.time.ExperimentalTime"
            , "-Xuse-experimental=kotlinx.coroutines.ExperimentalCoroutinesApi"
            , "-Xuse-experimental=kotlin.ExperimentalUnsignedTypes"
            , "-Xuse-experimental=io.ktor.util.KtorExperimentalAPI"
    )
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}
