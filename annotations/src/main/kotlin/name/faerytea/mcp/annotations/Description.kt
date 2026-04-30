package name.faerytea.mcp.annotations

/**
 * Since we cannot copy KDoc from function parameters
 * ([proof](https://github.com/google/ksp/issues/475)),
 * you must annotate tool parameters with this description.
 */
@MustBeDocumented
@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Description(
    val value: String,
)
