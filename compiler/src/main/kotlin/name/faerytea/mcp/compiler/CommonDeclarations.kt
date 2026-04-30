package name.faerytea.mcp.compiler

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.MemberName
import name.faerytea.mcp.annotations.Tool

data class CommonDeclarations(
    val server: KSClassDeclaration,
    val toolSchema: KSClassDeclaration,
    val toolAnnotations: KSClassDeclaration,
    val contentBlock: KSClassDeclaration,
    val mediaContent: KSClassDeclaration,
    val mediaContentText: KSClassDeclaration,
    val callToolResult: KSClassDeclaration,
    val requestMeta: KSClassDeclaration,
    val taskSupport: ClassName,
    val toolExecution: ClassName,
    val error: MemberName,
)
