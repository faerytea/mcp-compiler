package name.faerytea.mcp.example

import name.faerytea.mcp.annotations.Description
import name.faerytea.mcp.annotations.Tool

object Mem {
    private val memory = mutableMapOf<String, String>()

    /**
     * Remember text in memory. Shared across all connections.
     */
    @Tool
    fun remember(
        @Description("Key for retrieval")
        key: String,
        @Description("Actual memory. Put 'null' to erase memory.")
        value: String?,
    ) = "Previous memory: " + if (value == null) memory.remove(key) else memory.put(key, value)
}