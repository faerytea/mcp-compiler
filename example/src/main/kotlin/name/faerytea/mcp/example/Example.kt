package name.faerytea.mcp.example

import name.faerytea.mcp.annotations.Description
import name.faerytea.mcp.annotations.SafeTool
import name.faerytea.mcp.annotations.Tool
import name.faerytea.mcp.annotations.ToolAnnotation
import java.net.URL
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Paths
import java.util.stream.Collectors
import kotlin.io.path.useLines

/**
 * Read text file on disk.
 */
@Tool(
    annotation = ToolAnnotation(readOnlyHint = true, openWorldHint = true)
)
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

/**
 * Reads text file using [charset].
 */
@Tool(
    annotation = ToolAnnotation(readOnlyHint = true, openWorldHint = true)
)
fun readFile2(
    @Description("Full path to file")
    path: String,
    charset: String = "UTF-8", // will raise warning
    @Description("Maximum amount of lines")
    maxLines: Long = Long.MAX_VALUE,
): String = Files.lines(Paths.get(path), Charset.forName(charset))
    .limit(maxLines)
    .collect(Collectors.joining("\n"))



/**
 * Fetches text content from WWW.
 */
@Tool(
    annotation = ToolAnnotation(readOnlyHint = true, openWorldHint = true)
)
fun fetch(
    @Description("URL")
    url: String
) = URL(url).openStream().reader().use { it.readText() }
