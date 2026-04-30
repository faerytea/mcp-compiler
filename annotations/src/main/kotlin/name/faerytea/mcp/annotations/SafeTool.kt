package name.faerytea.mcp.annotations

/**
 * [Tool], but with 'safe' defaults.
 *
 * Use it on tools that won't modify anything
 * and have no access to wilderness.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
public annotation class SafeTool(
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
)
