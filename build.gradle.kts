import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

version = "0.9.25"

plugins {
    id("java")
    id("maven")
    id("org.jetbrains.kotlin.jvm") version "1.4-M2"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.4-M2"
    id("org.jetbrains.kotlin.kapt") version "1.4-M2"
}

repositories {
    mavenLocal()
    jcenter()
    maven ("https://dl.bintray.com/jerady/maven")
    maven("https://dl.bintray.com/kittinunf/maven")
    maven("https://jitpack.io")
    maven("https://repo.maven.apache.org/maven2")
    maven ("https://dl.bintray.com/kotlin/kotlin-eap")
    maven ("https://kotlin.bintray.com/kotlinx")
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
    val ktorVersion = "1.3.2"
    val serialVer = "0.20.0-1.4-M2"
    val coroutineVer = "1.3.7"
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx", "kotlinx-coroutines-core", coroutineVer)
    implementation("org.jetbrains.kotlinx", "kotlinx-serialization-runtime", serialVer)
    implementation("io.ktor", "ktor-client-okhttp", ktorVersion)
    implementation("io.ktor", "ktor-client-core", ktorVersion)
    implementation("io.ktor", "ktor-client-serialization-jvm", ktorVersion)
    implementation("io.github.microutils", "kotlin-logging", "1.7.8")
    implementation("org.slf4j", "slf4j-api", "1.8.0-beta2")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.2")
    testImplementation("com.squareup.okhttp3", "mockwebserver", "4.2.0")
    testImplementation("org.jetbrains.kotlinx", "kotlinx-coroutines-test", coroutineVer)
    testImplementation("org.jetbrains.kotlinx", "kotlinx-serialization-properties", serialVer)
    testRuntimeOnly("org.slf4j:slf4j-simple:1.8.0-beta2")
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
