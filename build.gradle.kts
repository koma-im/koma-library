import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinTest

version = "0.9.5"

plugins {
    id("java")
    id("maven")
    id("org.jetbrains.kotlin.jvm") version "1.3.50"
    id("org.jetbrains.kotlin.kapt") version "1.3.50"
}

repositories {
    mavenLocal()
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
    implementation(kotlin("stdlib-jdk8"))
    compileOnly(kotlin("reflect"))
    compile("com.squareup.moshi:moshi:1.8.0")
    compile("com.squareup.retrofit2:retrofit:2.6.1")
    implementation("com.squareup.retrofit2:converter-moshi:2.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.1")
    implementation("io.github.microutils:kotlin-logging:1.6.22")
    implementation("org.slf4j:slf4j-api:1.8.0-beta2")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.8.0")

    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.2")
    testImplementation("com.squareup.okhttp3", "mockwebserver", "4.2.0")
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
    )
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}
