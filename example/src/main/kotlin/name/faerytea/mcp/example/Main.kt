package name.faerytea.mcp.example

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.routing.*
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcpStatelessStreamableHttp
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import org.slf4j.event.Level
import java.nio.file.Files

fun main() {
    val server = Server(
        Implementation(
            name = "Test server",
            version = "0.1.0",
        ),
        ServerOptions(
            capabilities = ServerCapabilities(
                ServerCapabilities.Tools()
            )
        )
    )

    addReadFileToolTo(server)
    addReadFile2ToolTo(server)
    addAggregateToolTo(server)
    addFetchToolTo(server)
    Mem.apply {
        addRememberToolTo(server)
        addRecallToolTo(server)
        addRecallKeysToolTo(server)
    }
    FileMem(Files.createTempFile("test-mem", ".txt")).apply {
        addRememberToolTo(server) { "$it-file" }
        addRecallToolTo(server) { "$it-file" }
        addRecallKeysToolTo(server) { "$it-file" }
    }
//    server.addReso

    embeddedServer(CIO, 8088) {
        install(CallLogging) {
            level = Level.INFO
        }
        install(CORS) {
            anyHost()
            anyMethod()
            allowOrigins { true }
            allowMethod(HttpMethod.Options)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Get)
            allowHeader(HttpHeaders.ContentType)
            allowHeader("X-MCP-Protocol-Version")
            exposeHeader(HttpHeaders.Location)
            allowNonSimpleContentTypes = true
        }
        mcpStatelessStreamableHttp {
            server
        }
        routingRoot.getAllRoutes().forEach {
            println(it)
        }
    }.start(wait = true)
}