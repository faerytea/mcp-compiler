plugins {
    kotlin("jvm")
}

group = "name.faerytea.mcp.compiler"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(project(":annotations"))
    implementation(libs.square.kotlin.poet)
    implementation(libs.square.kotlin.poet.ksp)
    implementation(libs.ksp.api)
    testImplementation(kotlin("test"))
}