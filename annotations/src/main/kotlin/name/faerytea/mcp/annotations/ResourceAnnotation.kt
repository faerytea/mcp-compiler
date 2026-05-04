package name.faerytea.mcp.annotations

@Target()
@Retention(AnnotationRetention.SOURCE)
annotation class ResourceAnnotation(
    /**
     * Target audience of resource.
     * Supported values are `user` and `assistant`.
     */
    val audience: Array<String> = [],
    /**
     * Priority in range `[0..1]`. 0.0 means entirely optional
     * and 1.0 means effectively required.
     */
    val priority: Double = Double.NaN,
)
