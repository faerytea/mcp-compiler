package name.faerytea.mcp.compiler

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import name.faerytea.mcp.annotations.Description
import name.faerytea.mcp.annotations.Icon
import name.faerytea.mcp.annotations.PromptTemplate
import name.faerytea.mcp.annotations.ResourceTemplate
import name.faerytea.mcp.annotations.SafeTool
import name.faerytea.mcp.annotations.Tool
import name.faerytea.mcp.annotations.ToolAnnotation
import java.util.*
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
    private val jsonObject = ClassName(KTX_JSON, "JsonObject")
    private val jsonObjectNullable = jsonObject.copy(nullable = true)
    private var _kotlinTypes: KotlinTypes? = null
    private val kotlinTypes: KotlinTypes
        get() = _kotlinTypes ?: throw AssertionError("KotlinTypes not initialized!")
    private var _commonDeclarations: CommonDeclarations? = null
    private val commonDeclarations: CommonDeclarations
        get() = _commonDeclarations ?: throw AssertionError("commonDeclarations not initialized!")

    @OptIn(KspExperimental::class)
    private fun init(resolver: Resolver): Boolean {
        if (_kotlinTypes == null) {
            val builtIns = resolver.builtIns
            _kotlinTypes = KotlinTypes(
                builtIns = resolver.builtIns,
                primitiveArrayTypes = mapOf(
                    "kotlin.ByteArray" to builtIns.byteType,
                    "kotlin.ShortArray" to builtIns.shortType,
                    "kotlin.IntArray" to builtIns.intType,
                    "kotlin.LongArray" to builtIns.longType,
                    "kotlin.FloatArray" to builtIns.floatType,
                    "kotlin.DoubleArray" to builtIns.doubleType,
                    "kotlin.BooleanArray" to builtIns.booleanType,
                    "kotlin.CharArray" to builtIns.charType,
                ),
                charArrayType = resolver.getClassDeclarationByName<CharArray>()!!.asType(emptyList()),
                charSequenceType = resolver.getClassDeclarationByName<CharSequence>()!!.asType(emptyList()),
                collectionType = resolver.getClassDeclarationByName<Collection<*>>()!!.asStarProjectedType(),
                collectionMutableType = resolver.getClassDeclarationByName<MutableCollection<*>>()!!.asStarProjectedType(),
                listType = resolver.getClassDeclarationByName<List<*>>()!!.asStarProjectedType(),
                setType = resolver.getClassDeclarationByName<Set<*>>()!!.asStarProjectedType(),
            )
        }
        if (_commonDeclarations == null) {
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
            val resourceTemplateCD = resolver.getClassDeclarationByName("io.modelcontextprotocol.kotlin.sdk.types.ResourceTemplate")
            val readResourceResultCD = resolver.getClassDeclarationByName("io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult")
            val resourceContentsCD = resolver.getClassDeclarationByName("io.modelcontextprotocol.kotlin.sdk.types.ResourceContents")
            val textResourceContentsCD = resolver.getClassDeclarationByName("io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents")
            val genericAnnotationsCD = resolver.getClassDeclarationByName("io.modelcontextprotocol.kotlin.sdk.types.Annotations")
            val roleCD = resolver.getClassDeclarationByName("io.modelcontextprotocol.kotlin.sdk.types.Role")
            val promptCD = resolver.getClassDeclarationByName("io.modelcontextprotocol.kotlin.sdk.types.Prompt")
            val getPromptResultCD = resolver.getClassDeclarationByName("io.modelcontextprotocol.kotlin.sdk.types.GetPromptResult")
            val promptMessageCD = resolver.getClassDeclarationByName("io.modelcontextprotocol.kotlin.sdk.types.PromptMessage")
            val promptArgumentCD = resolver.getClassDeclarationByName("io.modelcontextprotocol.kotlin.sdk.types.PromptArgument")
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
                || resourceTemplateCD == null
                || readResourceResultCD == null
                || resourceContentsCD == null
                || textResourceContentsCD == null
                || genericAnnotationsCD == null
                || roleCD == null
                || promptCD == null
                || getPromptResultCD == null
                || promptMessageCD == null
                || promptArgumentCD == null
            ) {
                logger.error("Cannot find required MCP declarations")
                return false
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
                return false
            }
            val errorMN = errorFD.toMemberName()
            _commonDeclarations = CommonDeclarations(
                server = serverCD.toClassName(),
                toolSchema = toolSchemaCD.toClassName(),
                toolAnnotations = toolAnnotationsCD.toClassName(),
                contentBlock = contentBlockCD.asType(emptyList()),
                mediaContent = mediaContentCD,
                mediaContentText = mediaContentTextCD.toClassName(),
                callToolResult = callToolResultCD.toClassName(),
                requestMetaNullable = requestMetaCD.asType(emptyList()).makeNullable(),
                taskSupport = taskSupportCD.toClassName(),
                toolExecution = toolExecutionCD.toClassName(),
                error = errorMN,
                resourceTemplate = resourceTemplateCD.toClassName(),
                readResourceResult = readResourceResultCD.toClassName(),
                resourceContents = resourceContentsCD.asType(emptyList()),
                textResourceContents = textResourceContentsCD.toClassName(),
                genericAnnotations = genericAnnotationsCD.toClassName(),
                role = roleCD.toClassName(),
                prompt = promptCD.toClassName(),
                getPromptResult = getPromptResultCD.toClassName(),
                promptMessage = promptMessageCD.toClassName(),
                promptArgument = promptArgumentCD.toClassName(),
            )
        }
        return true
    }

    private data class FunctionsBundle(
        val tools: List<Pair<KSFunctionDeclaration, Tool>>,
        val resourceTemplates: List<Pair<KSFunctionDeclaration, ResourceTemplate>>,
        val promptTemplates: List<Pair<KSFunctionDeclaration, PromptTemplate>>,
    ) {
        fun isEmpty() = tools.isEmpty()
                && resourceTemplates.isEmpty()
                && promptTemplates.isEmpty()
    }

    @OptIn(KspExperimental::class)
    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (!init(resolver)) return emptyList()
        val declaredTools = (
                resolver.getSymbolsWithAnnotation(TOOL)
              + resolver.getSymbolsWithAnnotation(SAFE_TOOL)
                )
            .filterIsInstance<KSFunctionDeclaration>()
            .mapNotNull { f ->
                extractToolAnnotation(f)?.let { f to it }
            }
            .groupBy { it.first.containingFile }
        logger.info("Found ${declaredTools.size} tool files")
        val declaredResTemplates = (
                resolver.getSymbolsWithAnnotation(RES_TEMPLATE)
                )
            .filterIsInstance<KSFunctionDeclaration>()
            .map { it to it.getAnnotationsByType(ResourceTemplate::class).first() }
            .groupBy { it.first.containingFile }
        logger.info("Found ${declaredResTemplates.size} resource template files")
        val declaredPromptTemplates = (
                resolver.getSymbolsWithAnnotation(PROMPT_TEMPLATE)
                )
            .filterIsInstance<KSFunctionDeclaration>()
            .map { it to it.getAnnotationsByType(PromptTemplate::class).first() }
            .groupBy { it.first.containingFile }
        logger.info("Found ${declaredPromptTemplates.size} prompt template files")
        val pile = buildMap {
            for (f in declaredTools.keys + declaredResTemplates.keys + declaredPromptTemplates.keys) {
                if (f == null) continue
                put(
                    f,
                    FunctionsBundle(
                        declaredTools[f] ?: emptyList(),
                        declaredResTemplates[f] ?: emptyList(),
                        declaredPromptTemplates[f] ?: emptyList(),
                    )
                )
            }
        }
        logger.info("Processing ${pile.size} files")
        val notProcessed = ArrayList<KSAnnotated>()
        for ((file, functions) in pile) {
            logger.info(
                "Processing ${functions.tools.size} tools " +
                    "& ${functions.resourceTemplates.size} resource templates " +
                    "& ${functions.promptTemplates.size} prompt templates",
                file,
            )
            val originalFN = file.fileName.substringBefore('.')
            val fs = generateFile(
                file.packageName.asString(),
                "${originalFN}Things",
                functions,
            )
            if (fs == null) {
                notProcessed.addAll(functions.tools.map { it.first })
                notProcessed.addAll(functions.resourceTemplates.map { it.first })
                notProcessed.addAll(functions.promptTemplates.map { it.first })
                continue
            }
            fs.writeTo(codeGenerator, Dependencies(false, file))
        }
        return notProcessed
    }

    private fun generateFile(
        packageName: String,
        fileName: String,
        functions: FunctionsBundle,
    ): FileSpec? {
        if (functions.isEmpty()) return null
        val fs = FileSpec.builder(packageName, fileName)
            .addImport(
                KTX_JSON,
                "jsonPrimitive", "jsonArray", "JsonNull", "boolean", "int", "long", "float", "double")
        functions.tools.forEach { (f, t) ->
            generateToolSpec(f, t)?.let(fs::addFunction) ?: return null
        }
        functions.resourceTemplates.forEach { (f, rt) ->
            generateResourceTemplateSpec(f, rt)?.let(fs::addFunction) ?: return null
        }
        functions.promptTemplates.forEach { (f, pt) ->
            generatePromptTemplateSpec(f, pt)?.let(fs::addFunction) ?: return null
        }
        logger.info("Generated file ${fs.name} with package ${fs.packageName}")
        return fs.build()
    }

    @OptIn(KspExperimental::class)
    fun extractToolAnnotation(function: KSFunctionDeclaration): Tool? {
        val toolAnnotationNormal = function.getAnnotationsByType(Tool::class).firstOrNull()
        val toolAnnotationSafe = function.getAnnotationsByType(SafeTool::class).firstOrNull()
        if (toolAnnotationNormal != null && toolAnnotationSafe != null) {
            logger.error("Pick one of @Tool and @SafeTool", function)
            return null
        }
        check(toolAnnotationNormal != null || toolAnnotationSafe != null)
        return toolAnnotationNormal
            ?: Tool(
                name = toolAnnotationSafe!!.name,
                description = toolAnnotationSafe.description,
                title = toolAnnotationSafe.title,
                annotation = ToolAnnotation(readOnlyHint = true, openWorldHint = false),
                execution = Tool.Execution.OMIT,
            )
    }

    @OptIn(KspExperimental::class)
    fun generatePromptTemplateSpec(function: KSFunctionDeclaration, promptAnnotation: PromptTemplate): FunSpec? {
        val ptName = promptAnnotation.name.takeUnless { it.isBlank() } ?: function.simpleName.asString()
        val ptDesc = promptAnnotation.description.takeUnless { it.isBlank() } ?: function.docString
        val ptInputs = function.parameters.filter { it.name?.asString() != "_meta" }
        val metaInput = function.parameters.find { it.name?.asString() == "_meta" }
        if (metaInput != null) {
            if (metaInput.type.resolve() != commonDeclarations.requestMetaNullable) {
                logger.error("Request \$meta field must have type 'RequestMeta?'", metaInput)
                return null
            }
        }
        val params = buildMap(ptInputs.size) {
            for (p in ptInputs) {
                val conversion = when (val r = p.type.resolve()) {
                    kotlinTypes.builtIns.byteType -> ".toByte()"
                    kotlinTypes.builtIns.shortType -> ".toShort()"
                    kotlinTypes.builtIns.intType -> ".toInt()"
                    kotlinTypes.builtIns.longType -> ".toLong()"
                    kotlinTypes.builtIns.floatType -> ".toFloat()"
                    kotlinTypes.builtIns.doubleType -> ".toDouble()"
                    kotlinTypes.builtIns.booleanType -> ".equals(\"true\", true)".also {
                        logger.warn("${p.name?.asString()} declared as Boolean, only 'true' value will be considered as 'true'", p)
                    }
                    else -> if (r.isAssignableFrom(kotlinTypes.builtIns.stringType)) {
                        ""
                    } else {
                        logger.error("Unsupported type $r", p)
                        return null
                    }
                }
                put(p.name!!.asString(), conversion to p.hasDefault)
            }
        }
        val retTp = function.returnType?.resolve() ?: run {
            logger.warn("Cannot find return type (not generated yet?)", function)
            return null
        }
        val retTpCN = retTp.toClassName()
        val retCode = when {
            kotlinTypes.builtIns.stringType == retTp ->
                CodeBlock.of(
                    "%T(listOf(%T(role = %T.User, content = %T(result))))\n",
                    commonDeclarations.getPromptResult,
                    commonDeclarations.promptMessage,
                    commonDeclarations.role,
                    commonDeclarations.mediaContentText,
                )
            commonDeclarations.getPromptResult == retTpCN -> CodeBlock.of("result\n")
            commonDeclarations.promptMessage == retTpCN ->
                CodeBlock.of("%T(listOf(result))\n", commonDeclarations.getPromptResult)
            commonDeclarations.contentBlock.isAssignableFrom(retTp) ->
                CodeBlock.of(
                    "%T(listOf(%T(role = %T.User, content = (result))))\n",
                    commonDeclarations.getPromptResult,
                    commonDeclarations.promptMessage,
                    commonDeclarations.role,
                )
            else -> {
                logger.error("Incompatible return type (not String nor ContentBlock nor CallToolResult)", function)
                return null
            }
        }

        val optionalArguments = ptInputs.filter { it.hasDefault }.mapTo(HashSet()) { it.name!!.asString() }
        val optionalArgumentCount = optionalArguments.size

        val body = CodeBlock.builder()
            .add("server.addPrompt(\n")
            .withIndent {
                add("prompt = %T(\n", commonDeclarations.prompt)
                withIndent {
                    addStatement("name = %S,", ptName)
                    if (ptDesc != null) {
                        addStatement("description = %S,", ptDesc)
                    }
                    if (promptAnnotation.title.isNotBlank()) {
                        addStatement("title = %S,", promptAnnotation.title)
                    }
                    if (ptInputs.isNotEmpty()) {
                        add("arguments = listOf(\n")
                        withIndent {
                            for (input in ptInputs) {
                                add("%T(\n", commonDeclarations.promptArgument)
                                withIndent {
                                    val name = input.name!!.asString()
                                    addStatement("name = %S,", name)
                                    val description = input.getAnnotationsByType(Description::class).firstOrNull()?.value ?: ""
                                    if (description.isEmpty()) {
                                        logger.warn("No description provided for '$name' (see @Description)", input)
                                    } else {
                                        addStatement("description = %S,", description)
                                    }
                                    addStatement("required = %L,", !input.hasDefault)
                                    // TODO: No title for now
                                }
                                add("),\n")
                            }
                        }
                        add("),\n")
                    }
                    addIcons(function, promptAnnotation.icons)
                    addStatement("meta = meta,")
                }
                add(")\n")
            }
            .beginControlFlow(") { req ->")
            .apply {
                if (optionalArgumentCount == ptInputs.size) {
                    // everything is optional
                    addStatement("val arguments = req.arguments ?: emptyMap()")
                } else {
                    addStatement("val arguments = req.arguments ?: throw IllegalArgumentException(%S)", "No arguments provided!")
                }
            }
            .apply {
                if (optionalArgumentCount > 30) {
                    logger.error("Too many optional arguments ($optionalArgumentCount > 30)", function)
                    return null
                }
                val callMatrix = ArrayList<Set<String>>(1 shl optionalArgumentCount)
                callMatrix.add(emptySet())
                for ((name, value) in params) {
                    val (conversion, isOptional) = value
                    if (isOptional) {
                        addStatement("val _%L_is_present = arguments.containsKey(%S)", name, name)
                        beginControlFlow("val %L by lazy(LazyThreadSafetyMode.NONE)", name)
                        addStatement("arguments[%S]!!%L", name, conversion)
                        endControlFlow()
                        val curSize = callMatrix.size
                        for (i in 0 until curSize) {
                            callMatrix.add(callMatrix[i] + name)
                        }
                    } else {
                        addStatement("val %L = (arguments[%S] ?: throw IllegalArgumentException(%S))%L", name, name, "Parameter '$name' is required", conversion)
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
            .endControlFlow()
            .build()

        return FunSpec.builder("add${ptName.myCapitalize()}PromptTemplateTo")
            .addParameter("server", commonDeclarations.server)
            .addParameter(
                ParameterSpec.builder("meta", jsonObjectNullable)
                    .defaultValue("null")
                    .build()
            )
            .addKdoc("@see %M\n", function.toMemberName())
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

    @OptIn(KspExperimental::class)
    fun generateToolSpec(function: KSFunctionDeclaration, toolAnnotation: Tool): FunSpec? {
        val toolName = toolAnnotation.name.takeUnless { it.isBlank() } ?: function.simpleName.asString()
        val toolDescription = toolAnnotation.description.takeIf { it.isNotBlank() } ?: function.docString ?: ""
        val toolInputs = function.parameters.filter { it.name?.asString() != "_meta" }
        val metaInput = function.parameters.find { it.name?.asString() == "_meta" }
        if (metaInput != null) {
            if (metaInput.type.resolve() != commonDeclarations.requestMetaNullable) {
                logger.error("Request \$meta field must have type 'RequestMeta?'", metaInput)
                return null
            }
        }
        val params = LinkedHashMap<String, ParamDescription>(toolInputs.size)
        val retTp = function.returnType?.resolve() ?: run {
            logger.warn("Cannot find return type (not generated yet?)", function)
            return null
        }
        val retCode = when {
            kotlinTypes.builtIns.stringType == retTp ->
                CodeBlock.of(
                    "%T(listOf(%T(result)))\n",
                    commonDeclarations.callToolResult,
                    commonDeclarations.mediaContentText,
                )
            commonDeclarations.callToolResult == retTp.toClassName() -> CodeBlock.of("result\n")
            commonDeclarations.contentBlock.isAssignableFrom(retTp) ->
                CodeBlock.of("%T(listOf(result))\n", commonDeclarations.callToolResult)
            else -> {
                logger.error("Incompatible return type (not String nor ContentBlock nor CallToolResult)", function)
                return null
            }
        }
        val functionParentDeclarationCN = function.parentDeclaration.let {
            if (it is KSClassDeclaration && it.classKind != ClassKind.ANNOTATION_CLASS) {
                it.toClassName()
            } else {
                null
            }
        }
        val optionalArguments = toolInputs.filter { it.hasDefault }.mapTo(HashSet()) { it.name!!.asString() }
        val optionalArgumentCount = optionalArguments.size
        val body = CodeBlock.builder()
            .add("server.addTool(\n")
            .withIndent {
                if (functionParentDeclarationCN != null) {
                    add("name = nameOverride(%S),\n", toolName)
                } else {
                    add("name = %S,\n", toolName)
                }
                add("description = %S,\n", toolDescription)
                if (toolAnnotation.title.isNotBlank())
                    add("title = %S,\n", toolAnnotation.title)
                val requiredParams = mutableListOf<String>()
                if (toolInputs.isNotEmpty()) {
                    add("inputSchema = %T(\n", commonDeclarations.toolSchema)
                    withIndent {
                        beginControlFlow("properties = %M", bjo)
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
                        add(requiredParams.joinToString(", ", "required = listOf(", "),\n") { s -> "\"$s\"" })
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
                    add("toolAnnotations = %T(\n", commonDeclarations.toolAnnotations)
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
            .apply {
                if (optionalArgumentCount == toolInputs.size) {
                    // everything is optional
                    addStatement("val arguments = req.arguments ?: %T(emptyMap())", jsonObject)
                } else {
                    addStatement("val arguments = req.arguments ?: return@addTool %T.%M(%S)", commonDeclarations.callToolResult, commonDeclarations.error, "No arguments provided!")
                }
            }
            .beginControlFlow("return@addTool try")
            .apply {
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
                        beginControlFlow("val %L = (arguments[%S] ?: return@addTool %T.%M(%S)).let { e ->", name, name, commonDeclarations.callToolResult, commonDeclarations.error, "Parameter '$name' is required")
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
            .addStatement("%T.%M(err::class.simpleName + \": \" + err.message)", commonDeclarations.callToolResult, commonDeclarations.error)
            .endControlFlow()
            .endControlFlow()
            .build()

        return FunSpec.builder("add${toolName.myCapitalize()}ToolTo")
            .addParameter("server", commonDeclarations.server)
            .addParameter(
                ParameterSpec.builder("meta", jsonObjectNullable)
                    .defaultValue("null")
                    .build()
            )
            .apply {
                if (functionParentDeclarationCN != null) {
                    addParameter(
                        ParameterSpec.builder(
                            "nameOverride",
                            LambdaTypeName.get(
                                parameters = arrayOf(kotlinTypes.builtIns.stringType.toClassName()),
                                returnType = kotlinTypes.builtIns.stringType.toClassName(),
                            ),
                        )
                            .addKdoc("Mapping for tool name. Useful when same tool is added multiple times on different receivers.")
                            .defaultValue("{ it }")
                            .build()
                    )
                    receiver(functionParentDeclarationCN)
                }
            }
            .addKdoc("@see %M\n", function.toMemberName())
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
            kotlinTypes.builtIns.byteType -> "integer" to "jsonPrimitive.int.apply { if (it !in Byte.MIN_VALUE..Byte..MAX_VALUE) throw NumberFormatException(\"\$this is not Byte\") }.toByte()"
            kotlinTypes.builtIns.shortType -> "integer" to "jsonPrimitive.int.apply { if (it !in Short.MIN_VALUE..Short..MAX_VALUE) throw NumberFormatException(\"\$this is not Short\") }.toShort()"
            kotlinTypes.builtIns.intType -> "integer" to "jsonPrimitive.int"
            kotlinTypes.builtIns.longType -> "integer" to "jsonPrimitive.long"
            kotlinTypes.builtIns.floatType -> "number" to "jsonPrimitive.float"
            kotlinTypes.builtIns.doubleType -> "number" to "jsonPrimitive.double"
            kotlinTypes.charArrayType -> "string" to "jsonPrimitive.content.toCharArray()"
            kotlinTypes.builtIns.stringType -> "string" to "jsonPrimitive.content"
            kotlinTypes.builtIns.booleanType -> "boolean" to "jsonPrimitive.boolean"
            else -> if (nnTp.isAssignableFrom(kotlinTypes.builtIns.stringType)) "string" to "jsonPrimitive.string" else null to ""
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
            collectionContentType = kotlinTypes.primitiveArrayTypes[fqn]
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
                collectionContentType = resolved.arguments.firstOrNull()?.type?.resolve() ?: kotlinTypes.builtIns.anyType.makeNullable()
                collectionConstructor
                    .beginControlFlow("%T<%T>(%L.size) { %L ->", nnTp.toTypeName(), collectionContentType.toTypeName(), arrayInVar, indexInVar)
                    .addStatement("val %L = %L[%L]", elementInVar, arrayInVar, indexInVar)
            }
            // collection?
            if (collectionContentType == null && kotlinTypes.collectionType.isAssignableFrom(nnTp)) {
                collectionContentType = resolved.arguments.firstOrNull()?.type?.resolve() ?: kotlinTypes.builtIns.anyType.makeNullable()
                // check mutability
                if (kotlinTypes.collectionMutableType.isAssignableFrom(nnTp)) {
                    // create instance, then addAll
                    collectionConstructor
                        .beginControlFlow("%L.mapTo(%T()) { %L ->", arrayInVar, nnTp.toTypeName(), elementInVar)
                } else if (kotlinTypes.listType == nnTp.starProjection()) {
                    collectionConstructor
                        .beginControlFlow("%L.map { %L ->", arrayInVar, elementInVar)
                } else if (kotlinTypes.setType == nnTp.starProjection()) {
                    collectionConstructor
                        .beginControlFlow("%L.mapTo(HashSet<%T>(%L.size)) { %L ->", arrayInVar, collectionContentType.toTypeName(), inVar, elementInVar)
                } else {
                    throw IllegalStateException("Cannot create instance of $fqn")
                }
            }

            if (collectionContentType != null) {
                val outVarInner = outVar + "s"
                val (_, innerSchema, innerConstruction) = processType(
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
                        if (kotlinTypes.setType.isAssignableFrom(nnTp)) {
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

    private val uriTemplateVariable = Regex("\\{[^{}]+}")

    fun generateResourceTemplateSpec(function: KSFunctionDeclaration, resAnnotation: ResourceTemplate): FunSpec? {
        // no real parser since L1 is too simple
        val foundParams = uriTemplateVariable.findAll(resAnnotation.value)
            .map { resAnnotation.value.substring(it.range.first + 1, it.range.last) }
            .toSet()
        logger.info("Found ${foundParams.size} params: $foundParams", function)
        var meta: KSValueParameter? = null
        val conversions = LinkedHashMap<String, String>()
        for (arg in function.parameters) {
            val argName = arg.name!!.asString() // nameless arguments in functions aren't allowed
            // filter out _meta
            if (argName == "_meta") {
                if (arg.type.resolve() != commonDeclarations.requestMetaNullable) {
                    logger.error("Request \$meta field must have type 'RequestMeta?'", arg)
                    return null
                }
                meta = arg
                continue
            }
            // constant arguments
            if (argName !in foundParams) {
                if (arg.hasDefault) {
                    logger.warn("Parameter $argName will always have a default value", arg)
                    continue
                } else {
                    logger.error("Parameter $argName is not defined in template", arg)
                    return null
                }
            }
            // simple scalar conversions
            // TODO: complex types (at least list and map) for L4 templates
            conversions[argName] = when (arg.type.resolve()) {
                kotlinTypes.charSequenceType,
                kotlinTypes.builtIns.stringType -> ""
                kotlinTypes.builtIns.byteType -> ".toByte()"
                kotlinTypes.builtIns.shortType -> ".toShort()"
                kotlinTypes.builtIns.intType -> ".toInt()"
                kotlinTypes.builtIns.longType -> ".toLong()"
                kotlinTypes.builtIns.floatType -> ".toFloat()"
                kotlinTypes.builtIns.doubleType -> ".toDouble()"
                else -> {
                    logger.error("Unsupported type", arg)
                    return null
                }
            }
        }
        val diff = foundParams - conversions.keys
        if (diff.isNotEmpty()) {
            logger.error("Parameters from template aren't present in arguments: $diff", function)
            return null
        }
        val retTp = function.returnType?.resolve() ?: run {
            logger.warn("Cannot find return type (not generated yet?)", function)
            return null
        }
        val retCode = when {
            kotlinTypes.builtIns.stringType == retTp ->
                CodeBlock.of(
                    "%T(listOf(%T(text = result, uri = req.uri)))\n",
                    commonDeclarations.readResourceResult,
                    commonDeclarations.textResourceContents,
                )
            commonDeclarations.readResourceResult == retTp.toClassName() -> CodeBlock.of("result\n")
            commonDeclarations.resourceContents.isAssignableFrom(retTp) ->
                CodeBlock.of("%T(listOf(result))\n", commonDeclarations.readResourceResult)
            else -> {
                logger.error("Incompatible return type (not String nor ResourceContents nor ReadResourceResult)", function)
                return null
            }
        }
        val functionName = function.simpleName.asString()
        return FunSpec.builder("add${functionName.myCapitalize()}ResTemplateTo")
            .addParameter("server", commonDeclarations.server)
            .addParameter(
                ParameterSpec.builder("meta", jsonObjectNullable)
                    .defaultValue("null")
                    .build()
            )
            .apply {
                (function.parentDeclaration as? KSClassDeclaration)?.let {
                    receiver(it.toClassName())
                }
            }
            .addKdoc("@see %M\n", function.toMemberName())
            .returns(Unit::class)
            .addAnnotation(
                AnnotationSpec.builder(Generated::class)
                    .addMember("%S", "name.faerytea.mcp.compiler.Processor")
                    .build()
            )
            .addModifiers(KModifier.PUBLIC)
            .addCode(
                CodeBlock.builder()
                    .add("server.addResourceTemplate(\n")
                    .withIndent {
                        add("template = %T(\n", commonDeclarations.resourceTemplate)
                        withIndent {
                            addStatement("uriTemplate = %S,", resAnnotation.value)
                            val name = resAnnotation.name.takeUnless { it.isBlank() } ?: functionName
                            addStatement("name = %S,", name)
                            function.docString.takeUnless { it.isNullOrBlank() }?.let {
                                addStatement("description = %S,", it)
                            }
                            resAnnotation.mimeType.takeUnless { it.isBlank() }?.let {
                                addStatement("mimeType = %S,", it)
                            }
                            resAnnotation.title.takeUnless { it.isBlank() }?.let {
                                addStatement("title = %S,", it)
                            }
                            val priority = Double.NaN //resAnnotation.annotations.priority TODO: Unexpected KSP failure
                            val audience = resAnnotation.annotations.audience
                            if (!priority.isNaN() || audience.isNotEmpty()) {
                                add("annotations = %T(\n", commonDeclarations.genericAnnotations)
                                withIndent {
                                    if (!priority.isNaN()) {
                                        if (priority !in 0.0..1.0) {
                                            logger.error("Invalid priority ($priority)", function)
                                        }
                                        addStatement("priority = %L,", priority)
                                    }
                                    if (audience.isNotEmpty()) {
                                        add("audience = listOf(\n")
                                        withIndent {
                                            for (r in audience) {
                                                add("%T.%L,\n", commonDeclarations.role, r.lowercase().myCapitalize())
                                            }
                                        }
                                        add("),\n")
                                    }
                                }
                                add("),\n")
                            }
                            addIcons(function, resAnnotation.icons)
                            addStatement("meta = meta,")
                        }
                        add(")\n")
                    }
                    .beginControlFlow(") { req, parts ->")
                    .add("val result = %M(\n", function.toMemberName())
                    .withIndent {
                        for ((p, c) in conversions) {
                            addStatement("%L = parts.getValue(%S)%L,", p, p, c)
                        }
                        if (meta != null) {
                            addStatement("_meta = req.meta,")
                        }
                    }
                    .add(")\n")
                    .add("return@addResourceTemplate ")
                    .add(retCode)
                    .endControlFlow()
                    .build()
            )
            .build()
    }

    private fun CodeBlock.Builder.addIcons(
        function: KSFunctionDeclaration,
        icons: Array<Icon>
    ) {
        if (icons.isNotEmpty()) {
            add("icons = listOf(\n")
            withIndent {
                for (icon in icons) {
                    add("Icon(\n")
                    withIndent {
                        addStatement("src = %S,", icon.value)
                        if (icon.mimeType.isNotBlank()) {
                            addStatement("mimeType = %S,", icon.mimeType)
                        }
                        if (icon.size.isNotEmpty()) {
                            addStatement(icon.size.joinToString(prefix = "listOf(", postfix = "),") { "\"$it\"" })
                        }
                        val filteredThemes = ArrayList<String>(icon.theme.size)
                        for (t in icon.theme) {
                            when (val lct = t.lowercase()) {
                                "light" -> filteredThemes.add(lct)
                                "dark" -> filteredThemes.add(lct)
                                else -> logger.warn("Unsupported theme `$t'", function)
                            }
                        }
                        if (filteredThemes.isNotEmpty()) {
                            addStatement(
                                icon.size.joinToString(
                                    prefix = "listOf(",
                                    postfix = "),"
                                ) { "Icon.Theme.${it.myCapitalize()}" })
                        }
                    }
                    add(")\n")
                }
            }
            add("),\n")
        }
    }

    private fun String.myCapitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

    companion object {
        private const val PACKAGE = "name.faerytea.mcp.annotations"
        private const val TOOL = "${PACKAGE}.Tool"
        private const val SAFE_TOOL = "${PACKAGE}.SafeTool"
        private const val RES_TEMPLATE = "${PACKAGE}.ResourceTemplate"
        private const val PROMPT_TEMPLATE = "${PACKAGE}.PromptTemplate"

        private const val KTX_JSON = "kotlinx.serialization.json"
    }
}