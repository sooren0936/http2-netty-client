plugins {
    id("java")
}

group = "com.dempsey"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.netty:netty-all:4.1.89.Final")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")

    implementation("ch.qos.logback:logback-classic:1.4.5")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}