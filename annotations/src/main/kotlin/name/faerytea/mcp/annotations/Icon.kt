package name.faerytea.mcp.annotations

@Target()
@Retention(AnnotationRetention.SOURCE)
annotation class Icon(
    /**
     * URI for icon.
     */
    val value: String,
    /**
     * Override for icon MIME type.
     */
    val mimeType: String = "",
    /**
     * List of sizes in `WxH` format or `any`.
     */
    val size: Array<String> = [],
    /**
     * List of supported themes.
     * Supported values are `dark` and `light`
     */
    val theme: Array<String> = [],
)
