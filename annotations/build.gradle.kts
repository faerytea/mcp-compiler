plugins {
    kotlin("jvm")
    alias(libs.plugins.maven.publish)
}

group = "name.faerytea.mcp.annotations"
version = "0.1.0"

kotlin {
    jvmToolchain(8)
}

dependencies {
    testImplementation(kotlin("test"))
}

mavenPublishing {
    publishToMavenCentral()

    signAllPublications()

    coordinates(group.toString(), name, version.toString())

    pom {
        name.set("MCP Compiler annotations")
        description.set("Annotations for MCP tools compiler.")
        inceptionYear.set("2026")
        url.set("https://github.com/faerytea/mcp-compiler/")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("faerytea")
                name.set("Valery Maevsky")
                url.set("https://faerytea.name")
            }
        }
        scm {
            url.set("https://github.com/faerytea/mcp-compiler/")
            connection.set("scm:git:git://github.com/faerytea/mcp-compiler.git")
            developerConnection.set("scm:git:ssh://git@github.com/faerytea/mcp-compiler.git")
        }
    }
}
