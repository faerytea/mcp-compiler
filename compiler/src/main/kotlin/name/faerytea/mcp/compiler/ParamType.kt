package name.faerytea.mcp.compiler

import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.CodeBlock

data class ParamType(
    val constructedType: KSType,
    val schema: CodeBlock,
//    val optional: Boolean,
    val construction: CodeBlock,
)