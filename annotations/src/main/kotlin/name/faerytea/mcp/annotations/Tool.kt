package name.faerytea.mcp.annotations

/**
 * Marks function as tool.
 *
 * `add<FunctionName>Tool(server: Server)` function
 * will be generated.
 *
 * @see SafeTool
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
public annotation class Tool(
    /**
     * Model-visible tool name. Defaults to function name.
     */
    val name: String = "",
    /**
     * Model-visible tool description. Defaults to KDoc of function.
     */
    val description: String = "",
    /**
     * Name for a human.
     */
    val title: String = "",
    /**
     * Additional tool info.
     */
    val annotation: ToolAnnotation = ToolAnnotation(),
    /**
     * Indicates whether a tool supports task-augmented execution.
     */
    val execution: Execution = Execution.OMIT,
) {
    public enum class Execution {
        /** Special value for explicitly omitting parameter. Same as [FORBIDDEN] by MCP spec. */
        OMIT,
        /** Tool does not support task-augmented execution. */
        FORBIDDEN,
        /** Tool may support task-augmented execution. */
        OPTIONAL,
        /** Tool requires task-augmented execution. */
        REQUIRED,
    }
}
