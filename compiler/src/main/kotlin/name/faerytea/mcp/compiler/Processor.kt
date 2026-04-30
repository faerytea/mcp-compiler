package name.faerytea.mcp.compiler

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.containingFile
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.KSBuiltIns
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.MemberName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import com.squareup.kotlinpoet.withIndent
import name.faerytea.mcp.annotations.Description
import name.faerytea.mcp.annotations.SafeTool
import name.faerytea.mcp.annotations.Tool
import name.faerytea.mcp.annotations.ToolAnnotation
import java.util.Locale
import javax.annotation.processing.Generated

class Processor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
): SymbolProcessor {
    private val bjo = MemberName(KTX_JSON, "buildJsonObject")
    private val pjo = MemberName(KTX_JSON, "putJsonObject")
    private val bja = MemberName(KTX_JSON, "buildJsonArray")
    private val pja = MemberName(KTX_JSON, "putJsonArray")
    private val put = MemberName(KTX_JSON, "put")
    private val add = MemberName(KTX_JSON, "add")
    private val jsonObjectNullable = ClassName(KTX_JSON, "JsonObject")
        .copy(nullable = true)
    private lateinit var builtIns: KSBuiltIns
    private lateinit var primitiveArrayTypes: Map<String, KSType>
    private lateinit var charArrayType: KSType
    private lateinit var charSequenceType: KSType
    private lateinit var collectionType: KSType
    private lateinit var collectionMutableType: KSType
    private lateinit var listType: KSType
    private lateinit var setType: KSType

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        val serverCD = resolver.getClassDeclarationByName("io.modelcontextprotocol.kotlin.sdk.server.Server")
        val toolSchemaCD = resolver.getClassDeclarationByName("io.modelcontextprotocol.kotlin.sdk.types.ToolSchema")
        val toolAnnotationsCD = resolver.getClassDeclarationByName("io.modelcontextprotocol.kotlin.sdk.types.ToolAnnotations")
        val contentBlockCD = resolver.getClassDeclarationByName("io.modelcontextprotocol.kotlin.sdk.types.ContentBlock")
        val mediaContentCD = resolver.getClassDeclarationByName("io.modelcontextprotocol.kotlin.sdk.types.MediaContent")
        val mediaContentTextCD = resolver.getClassDeclarationByName("io.modelcontextprotocol.kotlin.sdk.types.TextContent")
        val callToolResultCD = resolver.getClassDeclarationByName("io.modelcontextprotocol.kotlin.sdk.types.CallToolResult")
        val requestMetaCD = resolver.getClassDeclarationByName("io.modelcontextprotocol.kotlin.sdk.types.RequestMeta")
        val taskSupportCD = resolver.getClassDeclarationByName("io.modelcontextprotocol.kotlin.sdk.types.TaskSupport")
        val toolExecutionCD = resolver.getClassDeclarationByName("io.modelcontextprotocol.kotlin.sdk.types.ToolExecution")
        if (serverCD == null
            || toolSchemaCD == null
            || toolAnnotationsCD == null
            || contentBlockCD == null
            || mediaContentCD == null
            || mediaContentTextCD == null
            || callToolResultCD == null
            || requestMetaCD == null
            || taskSupportCD == null
            || toolExecutionCD == null
            ) {
            logger.error("Cannot find required MCP classes")
            return emptyList()
        }
        val errorFD = resolver.getDeclarationsFromPackage(callToolResultCD.packageName.asString())
            .filterIsInstance<KSFunctionDeclaration>()
            .filter {
                val receiver = it.extensionReceiver ?: return@filter false
                if (it.simpleName.asString() != "error") return@filter false
                val cDecl = receiver.resolve().declaration as? KSClassDeclaration ?: return@filter false
                cDecl.isCompanionObject && cDecl.parentDeclaration == callToolResultCD
            }
            .firstOrNull() ?: run {
                logger.error("Cannot find required MCP functions")
                return emptyList()
            }
        val errorMN = errorFD.toMemberName()
        builtIns = resolver.builtIns
        primitiveArrayTypes = mapOf(
            "kotlin.ByteArray" to builtIns.byteType,
            "kotlin.ShortArray" to builtIns.shortType,
            "kotlin.IntArray" to builtIns.intType,
            "kotlin.LongArray" to builtIns.longType,
            "kotlin.FloatArray" to builtIns.floatType,
            "kotlin.DoubleArray" to builtIns.doubleType,
            "kotlin.BooleanArray" to builtIns.booleanType,
            "kotlin.CharArray" to builtIns.charType,
        )
        charArrayType = resolver.getClassDeclarationByName<CharArray>()!!.asType(emptyList())
        charSequenceType = resolver.getClassDeclarationByName<CharSequence>()!!.asType(emptyList())
        collectionType = resolver.getClassDeclarationByName<Collection<*>>()!!.asStarProjectedType()
        collectionMutableType = resolver.getClassDeclarationByName<MutableCollection<*>>()!!.asStarProjectedType()
        listType = resolver.getClassDeclarationByName<List<*>>()!!.asStarProjectedType()
        setType = resolver.getClassDeclarationByName<Set<*>>()!!.asStarProjectedType()
        val commonDeclarations = CommonDeclarations(
            server = serverCD,
            toolSchema = toolSchemaCD,
            toolAnnotations = toolAnnotationsCD,
            contentBlock = contentBlockCD,
            mediaContent = mediaContentCD,
            mediaContentText = mediaContentTextCD,
            callToolResult = callToolResultCD,
            requestMeta = requestMetaCD,
            taskSupport = taskSupportCD.toClassName(),
            toolExecution = toolExecutionCD.toClassName(),
            error = errorMN,
        )
        val declaredTools = (
                resolver.getSymbolsWithAnnotation(TOOL)
              + resolver.getSymbolsWithAnnotation(SAFE_TOOL)
                )
            .map {
                it.also {
                    logger.info("Looking at ${it::class} in ${it.containingFile}", it)
                }
            }
            .filterIsInstance<KSFunctionDeclaration>()
            .filter { f ->
                f.isStatic().also {
                    if (!it) logger.warn("Only object methods and top level functions are supported!", f)
                }
            }
            .groupBy { it.containingFile }
        logger.info("Found ${declaredTools.size} tool files")
        for ((file, functions) in declaredTools) {
            logger.info("Processing ${functions.size} tools in $file")
            if (file == null) continue
            val originalFN = file.fileName.substringBefore('.')
            val fs = generateFile(
                file.packageName.asString(),
                "${originalFN}Tools",
                functions,
                commonDeclarations,
            )
            if (fs == null) continue
            fs.writeTo(codeGenerator, Dependencies(false, file))
        }
        return emptyList()
    }

    fun generateFile(
        packageName: String,
        fileName: String,
        functions: List<KSFunctionDeclaration>,
        commonDeclarations: CommonDeclarations,
    ): FileSpec? {
        if (functions.isEmpty()) return null
        val fs = FileSpec.builder(packageName, fileName)
            .addImport(
                KTX_JSON,
                "jsonPrimitive", "jsonArray", "JsonNull", "boolean", "int", "long", "float", "double")
        functions.forEach {
            generateToolSpec(commonDeclarations, it)?.let(fs::addFunction)
        }
        logger.info("Generated file ${fs.name} with package ${fs.packageName}")
        return fs.build()
    }

    @OptIn(KspExperimental::class)
    fun generateToolSpec(commonDeclarations: CommonDeclarations, function: KSFunctionDeclaration): FunSpec? {
        val toolAnnotationNormal = function.getAnnotationsByType(Tool::class).firstOrNull()
        val toolAnnotationSafe = function.getAnnotationsByType(SafeTool::class).firstOrNull()
        if (toolAnnotationNormal != null && toolAnnotationSafe != null) {
            logger.error("Pick one of @Tool and @SafeTool", function)
            return null
        }
        check(toolAnnotationNormal != null || toolAnnotationSafe != null)
        val toolAnnotation = toolAnnotationNormal
            ?: Tool(
                name = toolAnnotationSafe!!.name,
                description = toolAnnotationSafe.description,
                title = toolAnnotationSafe.title,
                annotation = ToolAnnotation(readOnlyHint = true, openWorldHint = false),
                execution = Tool.Execution.OMIT,
            )
        val toolName = toolAnnotation.name.takeUnless { it.isBlank() } ?: function.simpleName.asString()
        val toolDescription = toolAnnotation.description.takeIf { it.isNotBlank() } ?: function.docString ?: ""
        val toolInputs = function.parameters.filter { it.name?.asString() != "_meta" }
        val metaInput = function.parameters.find { it.name?.asString() == "_meta" }
        if (metaInput != null) {
            if (metaInput.type.resolve() != commonDeclarations.requestMeta.asType(emptyList()).makeNullable()) {
                logger.error("Request \$meta field must have type 'RequestMeta?'", metaInput)
                return null
            }
        }
        val params = LinkedHashMap<String, ParamDescription>(toolInputs.size)
        val retTp = function.returnType?.resolve() ?: run {
            logger.error("Cannot find return type", function)
            return null
        }
        val retCode = when {
            builtIns.stringType == retTp ->
                CodeBlock.of(
                    "%T(listOf(%T(result)))\n",
                    commonDeclarations.callToolResult.toClassName(),
                    commonDeclarations.mediaContentText.toClassName(),
                )
            commonDeclarations.callToolResult == retTp -> CodeBlock.of("result\n")
            commonDeclarations.contentBlock.asType(emptyList()).isAssignableFrom(retTp) ->
                CodeBlock.of("%T(listOf(result))\n", commonDeclarations.callToolResult.toClassName())
            else -> {
                logger.error("Incompatible return type (not String nor ContentBlock nor CallToolResult)", function)
                return null
            }
        }
        val body = CodeBlock.builder()
            .add("server.addTool(\n")
            .withIndent {
                add("name = %S,\n", toolName)
                add("description = %S,\n", toolDescription)
                if (toolAnnotation.title.isNotBlank())
                    add("title = %S,\n", toolAnnotation.title)
                val requiredParams = mutableListOf<String>()
                if (toolInputs.isNotEmpty()) {
                    add("inputSchema = %T(\n", commonDeclarations.toolSchema.toClassName())
                    withIndent {
                        beginControlFlow("%M", bjo)
                        for (param in toolInputs) {
                            val paramName = param.name!!.asString()
                            beginControlFlow("%M(%S)", pjo, paramName)
                            if (!param.hasDefault) requiredParams += paramName
                            val paramType = processType(param.type.resolve())
                            params[paramName] = ParamDescription(paramType, param.hasDefault)
                            add(paramType.schema)
                            val description = param.getAnnotationsByType(Description::class).firstOrNull()?.value ?: ""
                            if (description.isBlank()) {
                                logger.warn("No description provided for $paramName in $toolName (see @Description)", param)
                            }
                            addStatement("%M(%S, %S)", put, "description", description)
                            endControlFlow()
                        }
                        endControlFlow()
                        add(",\n")
                        add(requiredParams.joinToString(", ", "listOf(", "),\n") { s -> "\"$s\"" })
                    }
                    add("),\n")
                }
                // TODO Output schema
                if (
                    toolAnnotation.annotation.title.isNotBlank()
                    || toolAnnotation.annotation.readOnlyHint
                    || !toolAnnotation.annotation.destructiveHint
                    || toolAnnotation.annotation.idempotentHint
                    || !toolAnnotation.annotation.openWorldHint
                    ) {
                    add("toolAnnotations = %T(\n", commonDeclarations.toolAnnotations.toClassName())
                    withIndent {
                        if (toolAnnotation.annotation.title.isNotBlank()) add("title = %S,\n", toolAnnotation.annotation.title)
                        if (toolAnnotation.annotation.readOnlyHint) add("readOnlyHint = true,\n")
                        if (!toolAnnotation.annotation.destructiveHint) add("destructiveHint = false,\n")
                        if (toolAnnotation.annotation.idempotentHint) add("idempotentHint = true,\n")
                        if (!toolAnnotation.annotation.openWorldHint) add("openWorldHint = false,\n")
                    }
                    add("),\n")
                }
                if (toolAnnotation.execution != Tool.Execution.OMIT) {
                    add(
                        "execution = %T(%T.%L),\n",
                        commonDeclarations.toolExecution,
                        commonDeclarations.taskSupport,
                        toolAnnotation.execution.name.lowercase().myCapitalize(),
                    )
                }
                add("meta = meta,\n")
            }
            .beginControlFlow(") { req ->")
            .addStatement("val arguments = req.arguments ?: return@addTool %T.%M(%S)", commonDeclarations.callToolResult.toClassName(), commonDeclarations.error, "No arguments provided!")
            .beginControlFlow("return@addTool try")
            .apply {
                val optionalArguments = toolInputs.filter { it.hasDefault }.mapTo(HashSet()) { it.name!!.asString() }
                val optionalArgumentCount = optionalArguments.size
                if (optionalArgumentCount > 30) {
                    logger.error("Too many optional arguments ($optionalArgumentCount > 30)", function)
                    return null
                }
                val callMatrix = ArrayList<Set<String>>(1 shl optionalArgumentCount)
                callMatrix.add(emptySet())
                for ((name, pd) in params) {
                    if (pd.optional) {
                        addStatement("val _%L_is_present = arguments.containsKey(%S)", name, name)
                        beginControlFlow("val %L by lazy(LazyThreadSafetyMode.NONE)", name)
                        addStatement("val e = arguments[%S]!!", name)
                        add(pd.paramType.construction)
                        addStatement("\nres")
                        endControlFlow()
                        val curSize = callMatrix.size
                        for (i in 0 until curSize) {
                            callMatrix.add(callMatrix[i] + name)
                        }
                    } else {
                        beginControlFlow("val %L = (arguments[%S] ?: return@addTool %T.%M(%S)).let { e ->", name, name, commonDeclarations.callToolResult.toClassName(), commonDeclarations.error, "Parameter '$name' is required")
                        add(pd.paramType.construction)
                        add("\n")
                        addStatement("res")
                        endControlFlow()
                    }
                }
                if (callMatrix.size == 1) {
                    // no defaults
                    add("val result = %M(\n", function.toMemberName())
                    withIndent {
                        for (name in params.keys) {
                            add("%L = %L,\n", name, name)
                        }
                        if (metaInput != null) {
                            add("_meta = req.meta,\n")
                        }
                    }
                    add(")\n")
                } else {
                    // Oh shit, HERE WE GO
                    beginControlFlow("val result = when")
                    for (s in callMatrix) {
                        val condition = optionalArguments.joinToString(" && ", postfix = " -> ") {
                            if (it in s) "_${it}_is_present" else "!_${it}_is_present"
                        }
                        add(condition)
                        add("%M(\n", function.toMemberName())
                        withIndent {
                            for (name in params.keys)
                                if (name in s || name !in optionalArguments)
                                    add("%L = %L,\n", name, name)
                            if (metaInput != null)
                                add("_meta = req.meta,\n")
                        }
                        add(")\n")
                    }
                    addStatement("else -> throw AssertionError(\"unreachable\")\n")
                    endControlFlow()
                }
                add(retCode)
            }
            .nextControlFlow("catch(err: Throwable)")
            .addStatement("%T.%M(err::class.simpleName + \": \" + err.message)", commonDeclarations.callToolResult.toClassName(), commonDeclarations.error)
            .endControlFlow()
            .endControlFlow()
            .build()

        return FunSpec.builder("add${toolName.myCapitalize()}ToolTo")
            .addParameter("server", commonDeclarations.server.asType(emptyList()).toTypeName())
            .addParameter(
                ParameterSpec.builder("meta", jsonObjectNullable)
                    .defaultValue("null")
                    .build()
            )
            .apply {
                val parentDeclaration = function.parentDeclaration
                if (parentDeclaration is KSClassDeclaration && parentDeclaration.classKind == ClassKind.OBJECT) {
                    receiver(parentDeclaration.toClassName())
                }
            }
            .addKdoc("@see %M", function.toMemberName())
            .returns(Unit::class)
            .addAnnotation(
                AnnotationSpec.builder(Generated::class)
                    .addMember("%S", "name.faerytea.mcp.compiler.Processor")
                    .build()
            )
            .addModifiers(KModifier.PUBLIC)
            .addCode(body)
            .build()
    }

    private fun processType(
        resolved: KSType,
        inVar: String = "e",
        outVar: String = "res",
    ): ParamType {
        val nnTp = resolved.makeNotNullable()
        val (simpleType, fromJsonSimple) = when (nnTp) {
            builtIns.byteType -> "integer" to "jsonPrimitive.int.apply { if (it !in Byte.MIN_VALUE..Byte..MAX_VALUE) throw NumberFormatException(\"\$this is not Byte\") }.toByte()"
            builtIns.shortType -> "integer" to "jsonPrimitive.int.apply { if (it !in Short.MIN_VALUE..Short..MAX_VALUE) throw NumberFormatException(\"\$this is not Short\") }.toShort()"
            builtIns.intType -> "integer" to "jsonPrimitive.int"
            builtIns.longType -> "integer" to "jsonPrimitive.long"
            builtIns.floatType -> "number" to "jsonPrimitive.float"
            builtIns.doubleType -> "number" to "jsonPrimitive.double"
            charArrayType -> "string" to "jsonPrimitive.content.toCharArray()"
            builtIns.stringType -> "string" to "jsonPrimitive.content"
            builtIns.booleanType -> "boolean" to "jsonPrimitive.boolean"
            else -> if (nnTp.isAssignableFrom(builtIns.stringType)) "string" to "jsonPrimitive.string" else null to ""
        }
        if (simpleType != null) {
            val schema = CodeBlock.builder()
                .addResolvedJsonType(simpleType, resolved.isMarkedNullable)
                .build()
            val construction = CodeBlock.builder()
                .constructResult(resolved, outVar, inVar, CodeBlock.of("%L.%L", inVar, fromJsonSimple))
                .build()
            return ParamType(resolved, schema, construction)
        } else {
            // check array / collection
            var collectionContentType: KSType?
            val collectionConstructor: CodeBlock.Builder = CodeBlock.builder()
            val fqn = resolved.declaration.qualifiedName!!.asString()
            // primitive array?
            collectionContentType = primitiveArrayTypes[fqn]
            val indexInVar = inVar + "i"
            val elementInVar = inVar + "e"
            val arrayInVar = inVar + "a"
            if (collectionContentType != null) {
                collectionConstructor
                    .beginControlFlow("%T(%L.size) { %L ->", nnTp.toTypeName(), arrayInVar, indexInVar)
                    .addStatement("val %L = %L[%L]", elementInVar, arrayInVar, indexInVar)
            }
            // generic array?
            if (collectionContentType == null && fqn == "kotlin.Array") {
                collectionContentType = resolved.arguments.firstOrNull()?.type?.resolve() ?: builtIns.anyType.makeNullable()
                collectionConstructor
                    .beginControlFlow("%T<%T>(%L.size) { %L ->", nnTp.toTypeName(), collectionContentType.toTypeName(), arrayInVar, indexInVar)
                    .addStatement("val %L = %L[%L]", elementInVar, arrayInVar, indexInVar)
            }
            // collection?
            if (collectionContentType == null && collectionType.isAssignableFrom(nnTp)) {
                collectionContentType = resolved.arguments.firstOrNull()?.type?.resolve() ?: builtIns.anyType.makeNullable()
                // check mutability
                if (collectionMutableType.isAssignableFrom(nnTp)) {
                    // create instance, then addAll
                    collectionConstructor
                        .beginControlFlow("%L.mapTo(%T()) { %L ->", arrayInVar, nnTp.toTypeName(), elementInVar)
                } else if (listType == nnTp.starProjection()) {
                    collectionConstructor
                        .beginControlFlow("%L.map { %L ->", arrayInVar, elementInVar)
                } else if (setType == nnTp.starProjection()) {
                    collectionConstructor
                        .beginControlFlow("%L.mapTo(HashSet<%T>(%L.size)) { %L ->", arrayInVar, collectionContentType.toTypeName(), inVar, elementInVar)
                } else {
                    throw IllegalStateException("Cannot create instance of $fqn")
                }
            }

            if (collectionContentType != null) {
                val outVarInner = outVar + "s"
                val (resTp, innerSchema, innerConstruction) = processType(
                    collectionContentType,
                    elementInVar,
                    outVarInner
                )
                val schema = CodeBlock.builder()
                    .addResolvedJsonType("array", resolved.isMarkedNullable)
                    .beginControlFlow("%M(%S)", pjo, "items")
                    .add(innerSchema)
                    .endControlFlow()
                    .apply {
                        if (setType.isAssignableFrom(nnTp)) {
                            addStatement("%M(%S, true)", put, "uniqueItems")
                        }
                    }
                    .build()
                val construction = collectionConstructor
                    .add(innerConstruction)
                    .addStatement("%L", outVarInner)
                    .endControlFlow()
                    .build()

                return ParamType(
                    resolved,
                    schema,
                    CodeBlock.builder()
                        .addStatement("val %L = %L.jsonArray", arrayInVar, inVar)
                        .constructResult(resolved, outVar, inVar, construction)
                        .build()
                )
            } else {
                // check enum
                if ((nnTp.declaration as? KSClassDeclaration)?.classKind == ClassKind.ENUM_CLASS) {
                    val schema = CodeBlock.builder()
                        .addResolvedJsonType("string", resolved.isMarkedNullable)
                        .beginControlFlow("%M(%S)", pja, "enum")
                        .beginControlFlow("for (ene in %T.entries)", nnTp.toTypeName())
                        .addStatement("%M(ene.name)", add)
                        .endControlFlow()
                        .endControlFlow()
                        .build()
                    val construction = CodeBlock.of("%T.valueOf(%L.jsonPrimitive.content)", nnTp.toTypeName(), inVar)
                    return ParamType(
                        resolved,
                        schema,
                        CodeBlock.builder()
                            .constructResult(resolved, outVar, inVar, construction)
                            .build()
                    )
                } else {
                    // a complex type!
                    logger.error("Unsupported type ${nnTp.declaration.qualifiedName?.asString()}", resolved.declaration)
                    throw NotImplementedError("Unsupported type ${nnTp.declaration.qualifiedName?.asString()}")
                }
            }
        }
    }

    private fun CodeBlock.Builder.constructResult(
        resolved: KSType,
        outVar: String,
        inVar: String,
        constructNNRValue: CodeBlock,
    ): CodeBlock.Builder =
        if (resolved.isMarkedNullable) {
            beginControlFlow("val %L = if (%L.jsonPrimitive == JsonNull)", outVar, inVar)
            addStatement("null")
            nextControlFlow("else")
            add(constructNNRValue)
            add("\n")
            endControlFlow()
        } else {
            add("val %L = ", outVar)
            add(constructNNRValue)
        }.add("\n")

    private fun CodeBlock.Builder.addResolvedJsonType(
        simpleType: String,
        isNullable: Boolean,
    ): CodeBlock.Builder {
        if (isNullable) {
            beginControlFlow("%M(%S)", pja, "type")
            addStatement("%M(%S)", add, "null")
            addStatement("%M(%S)", add, simpleType)
            endControlFlow()
        } else {
            addStatement("%M(%S, %S)", put, "type", simpleType)
        }
        return this
    }

    private fun String.myCapitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    companion object {
        private const val PACKAGE = "name.faerytea.mcp.annotations"
        private const val TOOL = "${PACKAGE}.Tool"
        private const val SAFE_TOOL = "${PACKAGE}.SafeTool"

        private const val KTX_JSON = "kotlinx.serialization.json"
    }
}