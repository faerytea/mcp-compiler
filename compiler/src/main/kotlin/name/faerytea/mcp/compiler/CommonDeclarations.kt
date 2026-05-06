package name.faerytea.mcp.compiler

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName

data class CommonDeclarations(
    val server: ClassName,
    val toolSchema: ClassName,
    val toolAnnotations: ClassName,
    val contentBlock: KSType,
    val mediaContent: KSClassDeclaration,
    val mediaContentText: ClassName,
    val callToolResult: ClassName,
    val requestMetaNullable: KSType,
    val taskSupport: ClassName,
    val toolExecution: ClassName,
    val error: MemberName,
    val resourceTemplate: ClassName,
    val readResourceResult: ClassName,
    val resourceContents: KSType,
    val textResourceContents: ClassName,
    val genericAnnotations: ClassName,
    val role: ClassName,
)
