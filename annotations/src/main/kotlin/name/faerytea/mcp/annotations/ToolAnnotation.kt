package name.faerytea.mcp.annotations

/**
 * Additional properties describing a Tool to clients.
 *
 * **IMPORTANT:** All properties in ToolAnnotations are **hints**. They are NOT guaranteed to provide
 * a faithful description of tool behavior (including descriptive properties like [title]).
 *
 * **Security warning:** Clients should NEVER make tool use decisions based on ToolAnnotations
 * received from untrusted servers.
 */
@Target()
@Retention(AnnotationRetention.SOURCE)
public annotation class ToolAnnotation(
    /**
     * Human-readable title (overrides [Tool.title] and [Tool.name] for UI)
     */
    val title: String = "",
    /**
     * If tool cannot modify environment, set to `true`.
     */
    val readOnlyHint: Boolean = false,
    /**
     * If tool cannot perform destructive modifications, set to `false`.
     * Ignored when [readOnlyHint] set to `true`.
     */
    val destructiveHint: Boolean = true,
    /**
     * If tool can perform only idempotent modifications, set to `true`.
     * Ignored when [readOnlyHint] set to `true`.
     *
     * Idempotent means that calling this tool with same args repeatedly
     * won't have more effect than first call.
     */
    val idempotentHint: Boolean = false,
    /**
     * If true, this tool may interact with an "open world" of external entities.
     * If false, the tool's domain of interaction is closed.
     * For example, the world of a web search tool is open, whereas that of a
     * memory tool is not.
     */
    val openWorldHint: Boolean = true,
)
