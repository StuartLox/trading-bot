import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.2.4.RELEASE"
    id("io.spring.dependency-management") version "1.0.9.RELEASE"
    id("com.bmuschko.docker-spring-boot-application") version "6.1.4"
    kotlin("jvm") version "1.3.61"
    kotlin("plugin.spring") version "1.3.61"
}

group = "com.stuartloxton.bitcoin-price-events"
version = "0.0.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_1_8


repositories {
    gradlePluginPortal()
    jcenter()
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
    maven {
        name = "JCenter Gradle Plugins"
        url  = uri("https://dl.bintray.com/gradle/gradle-plugins")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.apache.kafka:kafka-streams:2.4.0")
    implementation("org.apache.kafka:kafka-clients:2.4.0")
    implementation("io.confluent:kafka-avro-serializer:5.4.0")
    implementation ("io.confluent:kafka-streams-avro-serde:5.3.1")
    implementation("org.apache.avro:avro:1.9.1")
    implementation("org.json:json:20190722")

    //Deeplearning4j Dependencies
    implementation("org.deeplearning4j:deeplearning4j-core:1.0.0-beta6")
    implementation("org.deeplearning4j:deeplearning4j-modelimport:1.0.0-beta6")
    implementation("org.nd4j:nd4j-native-platform:1.0.0-beta6")

    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.kafka:spring-kafka-test")
}

buildscript {

    repositories {
        jcenter()
    }
    dependencies {
        classpath("com.commercehub.gradle.plugin:gradle-avro-plugin:0.17.0")

    }
}
apply(plugin = "com.commercehub.gradle.plugin.avro")


tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "1.8"
    }
}
val fileTree = configurations.testRuntimeClasspath.get().filter {
    it.name.endsWith(".so") || it.name.endsWith(".dll") || it.name.endsWith(".dylib")
}.asFileTree

tasks.register<Copy>("copyNativeDeps") {

    from(fileTree.files)
    into("build/native-libs")

    doFirst {
        mkdir("build/native-libs")
    }
}

tasks.withType<KotlinCompile> {
    dependsOn("generateAvroJava")
}

tasks {
    bootJar {
        launchScript()
    }
}

docker {
    springBootApplication {
        baseImage.set("anapsix/alpine-java")
        ports.set(listOf(9090, 8080))
        images.set(setOf("stuartloxton/bitcoin-price-events:0.2.0", "stuartloxton/bitcoin-price-events:latest"))
        jvmArgs.set(listOf("-Dspring.profiles.active=production", "-Xmx2048m"))
    }
}

tasks.build { dependsOn(tasks.dockerBuildImage) }
