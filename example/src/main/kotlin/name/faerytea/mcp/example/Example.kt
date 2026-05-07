package name.faerytea.mcp.example

import name.faerytea.mcp.annotations.Description
import name.faerytea.mcp.annotations.PromptTemplate
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

/**
 * Convert code between programming languages
 */
@PromptTemplate
fun translateCode(
    originalLanguage: String,
    targetLanguage: String,
    content: String,
    @Description("1 = keep structure, lower = more code shrinking, higher = simpler code")
    verbosity: Int = 1,
) = buildString {
    append("Here's the piece of code in ")
    append(originalLanguage)
    appendLine(":")
    append("```")
    appendLine(originalLanguage.lowercase().trim())
    appendLine(content)
    appendLine("```")
    appendLine("I want you to rewrite this code to $targetLanguage.")
    if (verbosity > 1) appendLine("Write simple & verbose code, do not use complex shortcuts.")
    if (verbosity > 2) appendLine("I have zero experience with $originalLanguage, so, please, annotate each line.")
    if (verbosity == 1) appendLine("Keep code structure as close to original as possible.")
    if (verbosity < 1) appendLine("And please, try to reduce code size.")
    if (verbosity < 0) appendLine("Feel free to use dirty hacks.")
}
