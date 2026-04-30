package name.faerytea.mcp.compiler

import com.google.devtools.ksp.processing.KSBuiltIns
import com.google.devtools.ksp.symbol.KSType

data class KotlinTypes(
    val builtIns: KSBuiltIns,
    val primitiveArrayTypes: Map<String, KSType>,
    val charArrayType: KSType,
    val charSequenceType: KSType,
    val collectionType: KSType,
    val collectionMutableType: KSType,
    val listType: KSType,
    val setType: KSType,
)
