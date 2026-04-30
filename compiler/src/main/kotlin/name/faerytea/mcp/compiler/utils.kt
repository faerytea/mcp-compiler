package name.faerytea.mcp.compiler

import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.FunctionKind
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.squareup.kotlinpoet.MemberName

fun KSFunctionDeclaration.isStatic(): Boolean {
    if (functionKind == FunctionKind.TOP_LEVEL || functionKind == FunctionKind.STATIC) return true
    val parent = parentDeclaration as? KSClassDeclaration ?: return true
    return parent.classKind == ClassKind.OBJECT
}

fun KSFunctionDeclaration.toMemberName(): MemberName =
    MemberName(packageName.asString(), simpleName.asString(), extensionReceiver != null)