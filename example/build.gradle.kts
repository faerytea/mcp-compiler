plugins {
    kotlin("jvm")
    id("application")
    alias(libs.plugins.ksp)
}

group = "name.faerytea.mcp"
version = "unspecified"

dependencies {
    compileOnly(project(":annotations"))
    ksp(project(":compiler"))
    val ktorVersion = "3.2.3"
    implementation("io.modelcontextprotocol:kotlin-sdk:0.12.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:${ktorVersion}")
    implementation("io.ktor:ktor-server-cio-jvm:${ktorVersion}")
    implementation("io.ktor:ktor-server-content-negotiation:${ktorVersion}")
    implementation("io.ktor:ktor-server-call-logging:${ktorVersion}")
    implementation("io.ktor:ktor-server-cors:${ktorVersion}")
    implementation("org.slf4j:slf4j-simple:2.0.17")
}

application {
    mainClass.set("name.faerytea.mcp.example.MainKt")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}