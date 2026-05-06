# MCPCompiler

[KSP](https://github.com/google/ksp) processor for creating 
 [MCP](https://modelcontextprotocol.io/) tools.

Generates boilerplate for [MCP Kotlin SDK](https://github.com/modelcontextprotocol/kotlin-sdk) —
 just write actual tool code, annotate with `@Tool` (or `@SafeTool`)
 and let codegen care about tool schema, `null` safety and wrapping.

Compare:
```kotlin
server.addTool(
    name = "readFile",
    description = "Read text file on disk.",
    inputSchema = ToolSchema(
        properties = buildJsonObject {
            putJsonObject("file") {
                put("type", "string")
                put("description", "File on disk. Path can be relative or absolute.")
            }
            putJsonObject("chunk") {
                put("type", "number")
                put("description", "Read only 'chunk' lines.")
            }
            putJsonObject("start") {
                put("type", "number")
                put("description", "Skip first 'start' lines.")
            }
        },
        required = listOf("file"),
    ),
) { req ->
    val arguments = req.arguments ?: return@addTool CallToolResult.error("No arguments provided!")
    val path = arguments["file"]?.jsonPrimitive?.content
        ?: return@addTool CallToolResult.error("The 'file' parameter is required.")
    val chunk = arguments["chunk"]?.jsonPrimitive?.content?.toIntOrNull() ?: Int.MAX_VALUE
    val start = arguments["start"]?.jsonPrimitive?.content?.toIntOrNull() ?: 0

    val data = try {
        Paths.get(path).useLines { lines ->
            lines
                .drop(start)
                .take(chunk)
                .joinToString(separator = "\n")
        }
    } catch (e: Throwable) {
        return@addTool CallToolResult.error(err::class.simpleName + ": " + err.message)
    }

    return@addTool CallToolResult.success(data)
}
```
and
```kotlin
// when configuring server
addReadFileToolTo(server) // generated function
```
```kotlin
/**
 * Read text file on disk.
 */
@SafeTool
fun readFile(
    @Description("File on disk. Path can be relative or absolute.")
    path: String,
    @Description("Read only 'chunk' lines.")
    chunk: Int = Int.MAX_VALUE,
    @Description("Skip first 'start' lines.")
    start: Int = 0,
): String = Paths.get(path).useLines { lines ->
    lines
        .drop(start)
        .take(chunk)
        .joinToString(separator = "\n")
}
```

See example project.

[![Mvn](https://badges.mvnrepository.com/badge/name.faerytea.mcp.compiler/compiler/badge.svg?label=Mvn)](https://mvnrepository.com/artifact/name.faerytea.mcp.compiler/compiler)
