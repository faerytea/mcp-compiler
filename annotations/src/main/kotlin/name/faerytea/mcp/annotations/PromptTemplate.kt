package name.faerytea.mcp.annotations

@Retention(AnnotationRetention.SOURCE)
@Target(AnnotationTarget.FUNCTION)
annotation class PromptTemplate(
    val name: String = "",
    val description: String = "",
    val title: String = "",
    val icons: Array<Icon> = [],
)
