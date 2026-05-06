package name.faerytea.mcp.example

import io.ktor.util.encodeBase64
import io.modelcontextprotocol.kotlin.sdk.types.BlobResourceContents
import io.modelcontextprotocol.kotlin.sdk.types.ResourceContents
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import name.faerytea.mcp.annotations.ResourceTemplate
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.readText

@ResourceTemplate("file://{path}")
fun file(path: String): ResourceContents {
    val p = Paths.get(path)
    val uri = "file://${p.pathString}"
    if (Files.exists(p)) {
        when {
            Files.isDirectory(p) -> {
                val res = buildJsonArray {
                    Files.list(p).forEach { f ->
                        add(JsonPrimitive(f.name))
                    }
                }
                return TextResourceContents(
                    Json.encodeToString(res),
                    uri,
                    "application/json",
                )
            }
            Files.isSymbolicLink(p) -> {
                return file(Files.readSymbolicLink(p).pathString)
            }
            Files.isRegularFile(p) -> {
                val mime = Files.probeContentType(p)
                return if (mime.startsWith("text/")) {
                    TextResourceContents(
                        p.readText(),
                        uri,
                        mime,
                    )
                } else {
                    BlobResourceContents(
                        Files.readAllBytes(p).encodeBase64(),
                        uri,
                        mime,
                    )
                }
            }
            else -> {
                throw UnsupportedOperationException()
            }
        }
    }
    throw NoSuchFileException(p.toFile())
}