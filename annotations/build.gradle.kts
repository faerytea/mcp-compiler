plugins {
    kotlin("jvm")
}

group = "name.faerytea.mcp.annotations"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain(8)
}

dependencies {
    testImplementation(kotlin("test"))
}