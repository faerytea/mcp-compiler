package name.faerytea.mcp.example

import name.faerytea.mcp.annotations.Description
import name.faerytea.mcp.annotations.Tool
import name.faerytea.mcp.example.Memory.Companion.NO_SUCH_MEMORY
import name.faerytea.mcp.example.Memory.Companion.PREV_MEM
import java.io.BufferedWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.stream.Collectors
import java.util.stream.Stream
import kotlin.jvm.optionals.getOrDefault

interface Memory {
    /**
     * Remember text in memory. Shared across all connections.
     */
    @Tool
    fun remember(
        @Description("Key for retrieval")
        key: String,
        @Description("Actual memory. Put 'null' to erase memory.")
        value: String?,
    ): String

    /**
     * Recall text from memory. Shared across all connections.
     */
    @Tool
    fun recall(
        @Description("Key for retrieval")
        key: String,
    ): String

    /**
     * Recall memory keys. Shared across all connections.
     */
    @Tool
    fun recallKeys(): String = keys().sorted().toString()

    fun keys(): Set<String>

    companion object {
        const val PREV_MEM = "Previous memory: "
        const val NO_SUCH_MEMORY = "<no such memory>"
    }
}

object Mem : Memory {
    private val memory = mutableMapOf<String, String>()

    override fun remember(
        key: String,
        value: String?,
    ) = PREV_MEM + if (value == null) memory.remove(key) else (memory.put(key, value) ?: NO_SUCH_MEMORY)

    override fun recall(key: String): String = memory[key] ?: NO_SUCH_MEMORY

    override fun keys(): Set<String> = memory.keys
}

class FileMem(
    private val file: Path
) : Memory {
    init {
        Files.createDirectories(file.parent)
        if (!Files.exists(file)) {
            Files.createFile(file)
        }
    }

    override fun remember(key: String, value: String?): String {
        val tempFile = Files.createTempFile("mem", ".txt")
        var result = NO_SUCH_MEMORY
        Files.newBufferedWriter(tempFile).use { tmp ->
            Files.lines(file).forEach { line ->
                val readKey = line.substringBefore(0.toChar())
                if (readKey == key) {
                    result = line.substringAfter(0.toChar())
                    if (value != null) {
                        tmp.saveKV(key, value)
                    }
                } else {
                    tmp.appendLine(line)
                }
            }
            if (result == NO_SUCH_MEMORY) {
                tmp.saveKV(key, value)
            }
        }
        Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING)
        return PREV_MEM + result
    }

    private fun BufferedWriter.saveKV(key: String, value: String?) {
        append(key)
        append(0.toChar())
        append(value)
        newLine()
    }

    override fun recall(key: String): String = Files.lines(file).flatMap { line ->
        val memKey = line.substringBefore(0.toChar())
        if (memKey == key) Stream.of(line.substringAfter(0.toChar())) else Stream.empty()
    }.findFirst().getOrDefault(NO_SUCH_MEMORY)

    override fun keys(): Set<String> = Files.lines(file).map { line ->
        line.substringBefore(0.toChar())
    }.collect(Collectors.toSet())
}