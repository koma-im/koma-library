import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("maven")
    id("org.jetbrains.kotlin.jvm").version("1.3.41")
    id("org.jetbrains.kotlin.kapt").version("1.3.41")
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.41")
    compileOnly("org.jetbrains.kotlin:kotlin-reflect:1.3.41")
    compile("com.squareup.moshi:moshi:1.8.0")
    compile("com.squareup.retrofit2:retrofit:2.6.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.0-M2")
    implementation("io.github.microutils:kotlin-logging:1.6.22")
    implementation("org.slf4j:slf4j-api:1.8.0-beta2")
    implementation("org.jetbrains.kotlin:kotlin-test:1.3.41")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.8.0")

    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.3.2")
    testRuntime("org.slf4j:slf4j-simple:1.8.0-beta2")
}

group = "io.github.koma-im"
version = "0.8.8"
description = "koma-library"
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    freeCompilerArgs = listOf("-XXLanguage:+InlineClasses")
}
