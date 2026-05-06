package name.faerytea.mcp.annotations

/**
 * Resource template.
 *
 * `add<FunctionName>ResTemplateTo(server: Server)` function
 * will be generated.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.SOURCE)
public annotation class ResourceTemplate(
    /**
     * URI template (see [RFC 6570](https://datatracker.ietf.org/doc/html/rfc6570)).
     * MCP Kotlin SDK supports only Level 1 templates
     * (i.e. only `{var}` segments without modifiers).
     *
     * All template variables will be populated into
     * corresponding function arguments.
     */
    val value: String,
    /**
     * Logical name, cat be used as a display name
     * if [title] is not provided.
     * Defaults to function name.
     */
    val name: String = "",
    /**
     * LLM-visible description of a resource.
     * Defaults to function docstring.
     */
    val description: String = "",
    /**
     * Human-readable descriptive name of this resource.
     * Defaults to [name].
     */
    val title: String = "",
    /**
     * Optional mime type for all resources accessible
     * via this template. Must not be provided if resources
     * have different mime type (e.g. `file://{path}` template)
     */
    val mimeType: String = "",
    /**
     * Optional resource annotations.
     */
    val annotations: ResourceAnnotation = ResourceAnnotation(),
    /**
     * Resource icons.
     */
    val icons: Array<Icon> = [],
)
