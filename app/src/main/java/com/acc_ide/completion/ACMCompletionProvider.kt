package com.acc_ide.completion

import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import java.util.regex.Pattern

/**
 * ACM-focused intelligent completion provider
 * Enhanced with struct member completion, scope management, and smart sorting
 */
class ACMCompletionProvider {
    
    companion object {
        // Priority levels for sorting - reordered to put local symbols first
        const val PRIORITY_EXACT_MATCH = 300
        const val PRIORITY_RECENT_LOCAL = 250
        const val PRIORITY_LOCAL_VARIABLE = 200
        const val PRIORITY_LOCAL_FUNCTION = 190
        const val PRIORITY_GLOBAL_VARIABLE = 180
        const val PRIORITY_STRUCT_MEMBER = 170
        const val PRIORITY_TYPE = 160
        const val PRIORITY_STL_COMMON = 150
        const val PRIORITY_STL_FUNCTION = 140
        const val PRIORITY_STL_UNCOMMON = 130
        const val PRIORITY_KEYWORD = 100
        
        // C++ Keywords for ACM
        private val CPP_KEYWORDS = listOf(
            "auto", "break", "case", "char", "const", "continue", "default", "do",
            "double", "else", "enum", "extern", "float", "for", "goto", "if",
            "inline", "int", "long", "register", "return", "short", "signed",
            "sizeof", "static", "struct", "switch", "typedef", "union", "unsigned",
            "void", "volatile", "while", "class", "namespace", "template", "typename",
            "public", "private", "protected", "virtual", "bool", "true", "false",
            "new", "delete", "this", "try", "catch", "throw", "using", "std"
        )
        
        // Common STL containers and algorithms frequently used in ACM
        private val STL_CONTAINERS = listOf(
            "vector", "string", "map", "set", "unordered_map", "unordered_set",
            "queue", "priority_queue", "stack", "deque", "pair", "array",
            "list", "multiset", "multimap"
        )
        
        // STL container methods with priority (common first)
        private val STL_VECTOR_METHODS = mapOf(
            "push_back" to PRIORITY_STL_COMMON,
            "size" to PRIORITY_STL_COMMON,
            "empty" to PRIORITY_STL_COMMON,
            "clear" to PRIORITY_STL_COMMON,
            "begin" to PRIORITY_STL_COMMON,
            "end" to PRIORITY_STL_COMMON,
            "front" to PRIORITY_STL_FUNCTION,
            "back" to PRIORITY_STL_FUNCTION,
            "pop_back" to PRIORITY_STL_FUNCTION,
            "insert" to PRIORITY_STL_FUNCTION,
            "erase" to PRIORITY_STL_FUNCTION,
            "resize" to PRIORITY_STL_UNCOMMON,
            "reserve" to PRIORITY_STL_UNCOMMON,
            "capacity" to PRIORITY_STL_UNCOMMON,
            "shrink_to_fit" to PRIORITY_STL_UNCOMMON
        )
        
        private val STL_STRING_METHODS = mapOf(
            "size" to PRIORITY_STL_COMMON,
            "length" to PRIORITY_STL_COMMON,
            "empty" to PRIORITY_STL_COMMON,
            "clear" to PRIORITY_STL_COMMON,
            "substr" to PRIORITY_STL_COMMON,
            "find" to PRIORITY_STL_COMMON,
            "append" to PRIORITY_STL_FUNCTION,
            "insert" to PRIORITY_STL_FUNCTION,
            "erase" to PRIORITY_STL_FUNCTION,
            "replace" to PRIORITY_STL_FUNCTION,
            "c_str" to PRIORITY_STL_FUNCTION,
            "compare" to PRIORITY_STL_UNCOMMON
        )
        
        private val STL_MAP_METHODS = mapOf(
            "find" to PRIORITY_STL_COMMON,
            "insert" to PRIORITY_STL_COMMON,
            "erase" to PRIORITY_STL_COMMON,
            "size" to PRIORITY_STL_COMMON,
            "empty" to PRIORITY_STL_COMMON,
            "clear" to PRIORITY_STL_COMMON,
            "begin" to PRIORITY_STL_FUNCTION,
            "end" to PRIORITY_STL_FUNCTION,
            "count" to PRIORITY_STL_FUNCTION,
            "lower_bound" to PRIORITY_STL_FUNCTION,
            "upper_bound" to PRIORITY_STL_FUNCTION
        )
        
        // STL algorithms commonly used in competitive programming
        private val STL_ALGORITHMS = listOf(
            "sort", "reverse", "find", "binary_search", "lower_bound", "upper_bound",
            "max", "min", "max_element", "min_element", "accumulate", "count",
            "unique", "next_permutation", "prev_permutation", "fill", "copy",
            "swap", "make_pair"
        )
        
        // Common competitive programming function templates
        private val ACM_TEMPLATES = mapOf(
            "gcd" to "int gcd(int a, int b) { return b ? gcd(b, a % b) : a; }",
            "lcm" to "int lcm(int a, int b) { return a / gcd(a, b) * b; }",
            "pow_mod" to "long long pow_mod(long long base, long long exp, long long mod) {\n    long long result = 1;\n    while (exp > 0) {\n        if (exp % 2 == 1) result = (result * base) % mod;\n        base = (base * base) % mod;\n        exp /= 2;\n    }\n    return result;\n}",
            "fast_io" to "ios_base::sync_with_stdio(false);\ncin.tie(NULL);",
            "debug" to "#ifdef LOCAL\n#define debug(x) cerr << #x << \" = \" << x << endl\n#else\n#define debug(x)\n#endif"
        )
        
        // Java keywords for ACM
        private val JAVA_KEYWORDS = listOf(
            "abstract", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
            "int", "interface", "long", "native", "new", "package", "private", "protected",
            "public", "return", "short", "static", "super", "switch", "synchronized",
            "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
            "true", "false", "null"
        )
        
        // Common Java collections and utilities for ACM
        private val JAVA_COLLECTIONS = listOf(
            "ArrayList", "LinkedList", "HashMap", "TreeMap", "HashSet", "TreeSet",
            "PriorityQueue", "Stack", "Queue", "Deque", "Collections", "Arrays",
            "Scanner", "StringBuilder", "StringBuffer"
        )
        
        // Python keywords for ACM
        private val PYTHON_KEYWORDS = listOf(
            "and", "as", "assert", "break", "class", "continue", "def", "del",
            "elif", "else", "except", "finally", "for", "from", "global", "if",
            "import", "in", "is", "lambda", "not", "or", "pass", "raise",
            "return", "try", "while", "with", "yield", "True", "False", "None"
        )
        
        // Python built-in functions commonly used in competitive programming
        private val PYTHON_BUILTINS = listOf(
            "len", "range", "enumerate", "zip", "map", "filter", "sorted", "reversed",
            "max", "min", "sum", "abs", "pow", "round", "int", "float", "str",
            "list", "tuple", "set", "dict", "input", "print", "open", "chr", "ord"
        )
    }
    
    internal val localSymbols = mutableMapOf<String, SymbolInfo>()
    internal val structDefinitions = mutableMapOf<String, StructInfo>()
    private val usageFrequency = mutableMapOf<String, Int>()
    private val scopeStack = mutableListOf<ScopeInfo>()
    
    data class SymbolInfo(
        val name: String,
        val type: SymbolType,
        val dataType: String,
        val line: Int,
        val priority: Int,
        val scopeLevel: Int,
        val description: String = ""
    )
    
    data class StructInfo(
        val name: String,
        val members: List<StructMember>,
        val line: Int
    )
    
    data class StructMember(
        val name: String,
        val type: String,
        val line: Int
    )
    
    data class ScopeInfo(
        val level: Int,
        val startLine: Int,
        val type: ScopeType
    )
    
    enum class SymbolType {
        VARIABLE, FUNCTION, CLASS, STRUCT, ENUM, PARAMETER
    }
    
    enum class ScopeType {
        FUNCTION, CLASS, BLOCK, NAMESPACE
    }
    
    /**
     * Provide completion suggestions based on prefix and language
     */
    fun provideCompletions(
        contentRef: ContentReference,
        position: CharPosition,
        prefix: String,
        language: String,
        publisher: CompletionPublisher
    ) {
        val items = mutableListOf<CompletionItem>()
        
        // Update local symbol table and struct definitions
        updateLocalSymbols(contentRef)
        
        // Check if this is a member access (dot operator)
        val (actualPrefix, contextVar) = extractMemberAccessContext(contentRef, position, prefix)
        
        if (contextVar != null) {
            // Handle struct/class member completion
            items.addAll(getMemberCompletions(contextVar, actualPrefix))
        } else {
            // Regular completion
            // Add language-specific completions
            when (language.lowercase()) {
                "cpp", "c++" -> {
                    items.addAll(getCppCompletions(actualPrefix))
                }
                "java" -> {
                    items.addAll(getJavaCompletions(actualPrefix))
                }
                "python", "py" -> {
                    items.addAll(getPythonCompletions(actualPrefix))
                }
            }
            
            // Add local symbols
            items.addAll(getLocalSymbolCompletions(actualPrefix))
        }
        
        // Update usage frequency for better future sorting
        if (prefix.isNotEmpty()) {
            usageFrequency[prefix] = (usageFrequency[prefix] ?: 0) + 1
        }
        
        // Sort by priority, relevance, and usage frequency
        val sortedItems = items.sortedWith(
            compareByDescending<CompletionItem> { 
                (it as? PriorityCompletionItem)?.priority ?: 0
            }.thenBy { 
                calculateRelevanceScore(it.label.toString(), actualPrefix) 
            }.thenByDescending {
                usageFrequency[it.label.toString()] ?: 0
            }.thenBy { 
                it.label.toString() 
            }
        )
        
        publisher.addItems(sortedItems)
    }
    
    /**
     * Extract member access context (e.g., "obj." -> return ("", "obj"))
     */
    private fun extractMemberAccessContext(
        contentRef: ContentReference, 
        position: CharPosition, 
        prefix: String
    ): Pair<String, String?> {
        try {
            val content = contentRef.reference
            val line = content.getLine(position.line).toString()
            val beforeCursor = line.substring(0, position.column)
            
            // Look for patterns like "varName." or "varName->"
            val dotPattern = Pattern.compile("""(\w+)\s*\.\s*(\w*)$""")
            val arrowPattern = Pattern.compile("""(\w+)\s*->\s*(\w*)$""")
            
            val dotMatcher = dotPattern.matcher(beforeCursor)
            val arrowMatcher = arrowPattern.matcher(beforeCursor)
            
            when {
                dotMatcher.find() -> {
                    val varName = dotMatcher.group(1) ?: return Pair(prefix, null)
                    val memberPrefix = dotMatcher.group(2) ?: ""
                    return Pair(memberPrefix, varName)
                }
                arrowMatcher.find() -> {
                    val varName = arrowMatcher.group(1) ?: return Pair(prefix, null)
                    val memberPrefix = arrowMatcher.group(2) ?: ""
                    return Pair(memberPrefix, varName)
                }
                else -> return Pair(prefix, null)
            }
        } catch (e: Exception) {
            return Pair(prefix, null)
        }
    }
    
    /**
     * Get member completions for struct/class/STL container
     */
    private fun getMemberCompletions(varName: String, prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        
        // Check if it's a known local variable
        val symbol = localSymbols[varName]
        if (symbol != null) {
            when (symbol.dataType.lowercase()) {
                "vector" -> {
                    items.addAll(getSTLMethodCompletions(prefix, STL_VECTOR_METHODS, "vector"))
                }
                "string" -> {
                    items.addAll(getSTLMethodCompletions(prefix, STL_STRING_METHODS, "string"))
                }
                "map", "unordered_map" -> {
                    items.addAll(getSTLMethodCompletions(prefix, STL_MAP_METHODS, "map"))
                }
                else -> {
                    // Check if it's a struct
                    val structInfo = structDefinitions[symbol.dataType]
                    if (structInfo != null) {
                        items.addAll(getStructMemberCompletions(structInfo, prefix))
                    }
                }
            }
        }
        
        // If no specific type found, try common STL container methods
        if (items.isEmpty()) {
            items.addAll(getCommonSTLMethods(prefix))
        }
        
        return items
    }
    
    /**
     * Get STL method completions
     */
    private fun getSTLMethodCompletions(
        prefix: String, 
        methods: Map<String, Int>, 
        containerType: String
    ): List<CompletionItem> {
        return methods.entries
            .filter { it.key.startsWith(prefix, ignoreCase = true) }
            .map { (method, priority) ->
                PriorityCompletionItem(
                    method, "$containerType method", prefix.length, 
                    if (method.endsWith("()")) method else "$method()",
                    priority, CompletionItemKind.Method
                )
            }
    }
    
    /**
     * Get struct member completions
     */
    private fun getStructMemberCompletions(structInfo: StructInfo, prefix: String): List<CompletionItem> {
        return structInfo.members
            .filter { it.name.startsWith(prefix, ignoreCase = true) }
            .map { member ->
                PriorityCompletionItem(
                    member.name, "${member.type} ${structInfo.name}.${member.name}",
                    prefix.length, member.name, PRIORITY_STRUCT_MEMBER, CompletionItemKind.Field
                )
            }
    }
    
    /**
     * Get common STL methods when type is unknown
     */
    private fun getCommonSTLMethods(prefix: String): List<CompletionItem> {
        val commonMethods = listOf(
            "size", "empty", "clear", "begin", "end", "push_back", "pop_back", "insert", "erase"
        )
        
        return commonMethods
            .filter { it.startsWith(prefix, ignoreCase = true) }
            .map { method ->
                PriorityCompletionItem(
                    method, "Common STL method", prefix.length, "$method()",
                    PRIORITY_STL_FUNCTION, CompletionItemKind.Method
                )
            }
    }
    
    private fun getCppCompletions(prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        // Keywords
        CPP_KEYWORDS.filter { it.startsWith(lowerPrefix) }.forEach { keyword ->
            items.add(PriorityCompletionItem(
                keyword, "C++ Keyword", prefix.length, keyword,
                PRIORITY_KEYWORD, CompletionItemKind.Keyword
            ))
        }
        
        // STL containers
        STL_CONTAINERS.filter { it.startsWith(lowerPrefix) }.forEach { container ->
            items.add(PriorityCompletionItem(
                container, "STL Container", prefix.length, container,
                PRIORITY_STL_COMMON, CompletionItemKind.Class
            ))
        }
        
        // STL algorithms
        STL_ALGORITHMS.filter { it.startsWith(lowerPrefix) }.forEach { algorithm ->
            items.add(PriorityCompletionItem(
                algorithm, "STL Algorithm", prefix.length, "$algorithm()",
                PRIORITY_STL_COMMON, CompletionItemKind.Function
            ))
        }
        
        // ACM templates
        ACM_TEMPLATES.entries.filter { it.key.startsWith(lowerPrefix) }.forEach { (name, template) ->
            items.add(PriorityCompletionItem(
                name, "ACM Template", prefix.length, template,
                PRIORITY_STL_FUNCTION, CompletionItemKind.Snippet
            ))
        }
        
        return items
    }
    
    private fun getJavaCompletions(prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        // Keywords
        JAVA_KEYWORDS.filter { it.startsWith(lowerPrefix) }.forEach { keyword ->
            items.add(PriorityCompletionItem(
                keyword, "Java Keyword", prefix.length, keyword,
                PRIORITY_KEYWORD, CompletionItemKind.Keyword
            ))
        }
        
        // Collections
        JAVA_COLLECTIONS.filter { it.lowercase().startsWith(lowerPrefix) }.forEach { collection ->
            items.add(PriorityCompletionItem(
                collection, "Java Collection", prefix.length, collection,
                PRIORITY_STL_COMMON, CompletionItemKind.Class
            ))
        }
        
        return items
    }
    
    private fun getPythonCompletions(prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        // Keywords
        PYTHON_KEYWORDS.filter { it.startsWith(lowerPrefix) }.forEach { keyword ->
            items.add(PriorityCompletionItem(
                keyword, "Python Keyword", prefix.length, keyword,
                PRIORITY_KEYWORD, CompletionItemKind.Keyword
            ))
        }
        
        // Built-in functions
        PYTHON_BUILTINS.filter { it.startsWith(lowerPrefix) }.forEach { builtin ->
            items.add(PriorityCompletionItem(
                builtin, "Python Built-in", prefix.length, "$builtin()",
                PRIORITY_STL_COMMON, CompletionItemKind.Function
            ))
        }
        
        return items
    }
    
    private fun getLocalSymbolCompletions(prefix: String): List<CompletionItem> {
        return localSymbols.values
            .filter { it.name.startsWith(prefix, ignoreCase = true) }
            .map { symbol ->
                // Calculate priority based on scope and usage
                val scopePriority = when {
                    symbol.scopeLevel == scopeStack.size -> PRIORITY_RECENT_LOCAL // Current scope
                    symbol.scopeLevel == 0 -> PRIORITY_GLOBAL_VARIABLE // Global scope
                    else -> symbol.priority // Original priority
                }
                
                val finalPriority = if (usageFrequency[symbol.name] ?: 0 > 2) {
                    scopePriority + 10 // Boost frequently used symbols
                } else {
                    scopePriority
                }
                
                val kind = when (symbol.type) {
                    SymbolType.VARIABLE -> CompletionItemKind.Variable
                    SymbolType.FUNCTION -> CompletionItemKind.Function
                    SymbolType.CLASS -> CompletionItemKind.Class
                    SymbolType.STRUCT -> CompletionItemKind.Struct
                    SymbolType.ENUM -> CompletionItemKind.Enum
                    SymbolType.PARAMETER -> CompletionItemKind.Variable
                }
                
                PriorityCompletionItem(
                    symbol.name, symbol.description, prefix.length, symbol.name,
                    finalPriority, kind
                )
            }
    }
    
    internal fun updateLocalSymbols(contentRef: ContentReference) {
        localSymbols.clear()
        structDefinitions.clear()
        scopeStack.clear()
        
        try {
            val content = contentRef.reference
            val lines = content.toString().lines()
            
            lines.forEachIndexed { lineIndex, line ->
                updateScopeStack(line, lineIndex)
                extractSymbolsFromLine(line, lineIndex)
                extractStructDefinitions(line, lineIndex, lines)
            }
        } catch (e: Exception) {
            // Handle content access safely
        }
    }
    
    private fun updateScopeStack(line: String, lineIndex: Int) {
        val trimmedLine = line.trim()
        
        // Handle opening braces (new scope)
        if (trimmedLine.endsWith("{") || trimmedLine.contains("{")) {
            val scopeType = when {
                trimmedLine.contains("struct") || trimmedLine.contains("class") -> ScopeType.CLASS
                trimmedLine.contains("namespace") -> ScopeType.NAMESPACE
                else -> ScopeType.BLOCK
            }
            scopeStack.add(ScopeInfo(scopeStack.size, lineIndex, scopeType))
        }
        
        // Handle closing braces (end scope)
        if (trimmedLine.contains("}")) {
            if (scopeStack.isNotEmpty()) {
                scopeStack.removeLastOrNull()
            }
        }
    }
    
    private fun extractSymbolsFromLine(line: String, lineIndex: Int) {
        val currentScope = scopeStack.size
        
        // Handle complex variable declarations with multiple variables
        extractMultipleVariableDeclarations(line, lineIndex, currentScope)
        
        // Handle template types with initialization
        extractTemplateDeclarations(line, lineIndex, currentScope)
        
        // Handle array declarations
        extractArrayDeclarations(line, lineIndex, currentScope)
        
        // Handle simple declarations (backward compatibility)
        val patterns = listOf(
            // Simple STL containers: vector v, string s
            Pattern.compile("""(vector|string|map|set|unordered_map|unordered_set|queue|stack|deque|pair|list)\s+(\w+)"""),
            // Auto keyword: auto x = 
            Pattern.compile("""auto\s+(\w+)\s*=""")
        )
        
        for (pattern in patterns) {
            val matcher = pattern.matcher(line)
            while (matcher.find()) {
                val type = if (pattern.pattern().contains("auto")) "auto" else matcher.group(1) ?: "unknown"
                val varName = if (pattern.pattern().contains("auto")) matcher.group(1) else matcher.group(2)
                
                if (varName != null && !isKeyword(varName)) {
                    val priority = if (currentScope == 0) PRIORITY_GLOBAL_VARIABLE else PRIORITY_LOCAL_VARIABLE
                    localSymbols[varName] = SymbolInfo(
                        varName, SymbolType.VARIABLE, type, lineIndex, 
                        priority, currentScope, "$type variable"
                    )
                }
            }
        }
        
        // Function definitions with return type
        val functionPattern = Pattern.compile("""(void|int|long|double|float|char|string|bool|auto|[A-Z]\w*)\s+(\w+)\s*\([^)]*\)\s*\{?""")
        val functionMatcher = functionPattern.matcher(line)
        while (functionMatcher.find()) {
            val returnType = functionMatcher.group(1) ?: "void"
            val funcName = functionMatcher.group(2) ?: continue
            
            if (!isKeyword(funcName)) {
                localSymbols[funcName] = SymbolInfo(
                    funcName, SymbolType.FUNCTION, returnType, lineIndex,
                    PRIORITY_LOCAL_FUNCTION, currentScope, "$returnType function"
                )
            }
        }
        
        // Class/struct declarations
        val classPattern = Pattern.compile("""(class|struct)\s+(\w+)""")
        val classMatcher = classPattern.matcher(line)
        while (classMatcher.find()) {
            val classType = classMatcher.group(1) ?: "class"
            val className = classMatcher.group(2) ?: continue
            
            localSymbols[className] = SymbolInfo(
                className, if (classType == "struct") SymbolType.STRUCT else SymbolType.CLASS, 
                classType, lineIndex, PRIORITY_TYPE, currentScope, "User-defined $classType"
            )
        }
    }
    
    /**
     * Extract variables from multiple variable declarations like "int x, *p, y;"
     */
    private fun extractMultipleVariableDeclarations(line: String, lineIndex: Int, currentScope: Int) {
        // Enhanced pattern to match type declarations with multiple variables
        // Handles: int x, *p, y; long long a, b, c; unsigned int u, v; const int c1, c2; etc.
        val multiVarPattern = Pattern.compile(
            """((?:const\s+)?(?:unsigned\s+)?(?:signed\s+)?(?:int|long\s+long|long|double|float|char|bool|string|short|[A-Z]\w*))\s+([^;{}]+);"""
        )
        
        val matcher = multiVarPattern.matcher(line)
        if (matcher.find()) {
            val baseType = matcher.group(1)?.trim() ?: return
            val variablesPart = matcher.group(2)?.trim() ?: return
            
            // Split by comma to get individual variable declarations, but be careful of nested commas in initialization
            val variables = smartSplitVariableDeclarations(variablesPart)
            
            for (varDecl in variables) {
                val trimmedDecl = varDecl.trim()
                
                // Extract variable name, handling pointers and arrays
                val varName = extractVariableNameFromDeclaration(trimmedDecl)
                
                if (varName != null && !isKeyword(varName)) {
                    val type = when {
                        trimmedDecl.contains("**") -> "$baseType**"
                        trimmedDecl.contains("*") -> "$baseType*"
                        trimmedDecl.contains("[") -> "$baseType[]"
                        else -> baseType
                    }
                    val priority = if (currentScope == 0) PRIORITY_GLOBAL_VARIABLE else PRIORITY_LOCAL_VARIABLE
                    
                    localSymbols[varName] = SymbolInfo(
                        varName, SymbolType.VARIABLE, type, lineIndex,
                        priority, currentScope, "$type variable"
                    )
                }
            }
        }
    }
    
    /**
     * Smart split for variable declarations, handling nested parentheses and brackets
     */
    private fun smartSplitVariableDeclarations(declarations: String): List<String> {
        val result = mutableListOf<String>()
        var current = StringBuilder()
        var parenDepth = 0
        var bracketDepth = 0
        
        for (char in declarations) {
            when (char) {
                '(' -> {
                    parenDepth++
                    current.append(char)
                }
                ')' -> {
                    parenDepth--
                    current.append(char)
                }
                '[' -> {
                    bracketDepth++
                    current.append(char)
                }
                ']' -> {
                    bracketDepth--
                    current.append(char)
                }
                ',' -> {
                    if (parenDepth == 0 && bracketDepth == 0) {
                        result.add(current.toString().trim())
                        current = StringBuilder()
                    } else {
                        current.append(char)
                    }
                }
                else -> current.append(char)
            }
        }
        
        if (current.isNotEmpty()) {
            result.add(current.toString().trim())
        }
        
        return result
    }
    
    /**
     * Extract variables from template declarations with initialization like "vector<int> arr(n);"
     */
    private fun extractTemplateDeclarations(line: String, lineIndex: Int, currentScope: Int) {
        // Enhanced pattern to match template types with initialization
        // Handles: vector<int> arr(n); map<string, int> m(10); vector<vector<int>> matrix(5, vector<int>(5));
        // Also handles initialization with braces: vector<int> arr{1, 2, 3};
        val templateWithInitPattern = Pattern.compile(
            """(vector|string|map|set|unordered_map|unordered_set|queue|stack|deque|pair|list|priority_queue|array|bitset)\s*<[^<>]*(?:<[^<>]*>[^<>]*)*>\s+(\w+)(?:\s*\([^)]*\)|\s*\{[^}]*\})?"""
        )
        
        val initMatcher = templateWithInitPattern.matcher(line)
        while (initMatcher.find()) {
            val containerType = initMatcher.group(1) ?: continue
            val varName = initMatcher.group(2) ?: continue
            
            if (!isKeyword(varName)) {
                val priority = if (currentScope == 0) PRIORITY_GLOBAL_VARIABLE else PRIORITY_LOCAL_VARIABLE
                
                localSymbols[varName] = SymbolInfo(
                    varName, SymbolType.VARIABLE, containerType, lineIndex,
                    priority, currentScope, "$containerType container"
                )
            }
        }
        
        // Also handle basic template declarations without initialization
        val basicTemplatePattern = Pattern.compile(
            """(vector|string|map|set|unordered_map|unordered_set|queue|stack|deque|pair|list|priority_queue|array|bitset)\s*<[^<>]*(?:<[^<>]*>[^<>]*)*>\s+(\w+)\s*;"""
        )
        
        val basicMatcher = basicTemplatePattern.matcher(line)
        while (basicMatcher.find()) {
            val containerType = basicMatcher.group(1) ?: continue
            val varName = basicMatcher.group(2) ?: continue
            
            if (!isKeyword(varName) && !localSymbols.containsKey(varName)) {
                val priority = if (currentScope == 0) PRIORITY_GLOBAL_VARIABLE else PRIORITY_LOCAL_VARIABLE
                
                localSymbols[varName] = SymbolInfo(
                    varName, SymbolType.VARIABLE, containerType, lineIndex,
                    priority, currentScope, "$containerType container"
                )
            }
        }
    }
    
    /**
     * Extract variable name from declaration, handling pointers and arrays
     * Examples: "x" -> "x", "*p" -> "p", "arr[10]" -> "arr", "*ptr[5]" -> "ptr"
     * Also handles: "x = 5" -> "x", "ptr = nullptr" -> "ptr", "arr(n)" -> "arr"
     */
    private fun extractVariableNameFromDeclaration(declaration: String): String? {
        val trimmed = declaration.trim()
        
        // Remove initialization part if present (e.g., "x = 5" -> "x", "arr(10)" -> "arr")
        val withoutInit = trimmed.split(Regex("[=()]"))[0].trim()
        
        // Handle array declarations (e.g., "arr[10]" -> "arr")
        val withoutArray = withoutInit.split("[")[0].trim()
        
        // Handle pointer declarations (e.g., "*p" -> "p", "**pp" -> "pp")
        val withoutPointers = withoutArray.replace("*", "").trim()
        
        // Handle reference declarations (e.g., "&ref" -> "ref")
        val withoutReference = withoutPointers.replace("&", "").trim()
        
        // Extract the identifier, handling whitespace
        val identifierPattern = Pattern.compile("""(\w+)""")
        val matcher = identifierPattern.matcher(withoutReference)
        
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            null
        }
    }
    
    /**
     * Extract variables from array declarations like "int arr[10];"
     */
    private fun extractArrayDeclarations(line: String, lineIndex: Int, currentScope: Int) {
        // Pattern to match array declarations including arrays of pointers
        // Handles: int arr[10]; char str[256]; double matrix[10][10]; int *ptr_arr[10]; etc.
        // First pattern: arrays of pointers like "int *ptr_arr[10];"
        val pointerArrayPattern = Pattern.compile(
            """((?:const\s+)?(?:unsigned\s+)?(?:int|long\s+long|long|double|float|char|bool|string|[A-Z]\w*))\s*(\*+)\s*(\w+)\s*\[[^\]]*\]"""
        )
        
        val pointerArrayMatcher = pointerArrayPattern.matcher(line)
        while (pointerArrayMatcher.find()) {
            val baseType = pointerArrayMatcher.group(1) ?: continue
            val pointerStars = pointerArrayMatcher.group(2) ?: continue
            val varName = pointerArrayMatcher.group(3) ?: continue
            
            if (!isKeyword(varName)) {
                val dataType = "$baseType$pointerStars[]"
                val priority = if (currentScope == 0) PRIORITY_GLOBAL_VARIABLE else PRIORITY_LOCAL_VARIABLE
                
                localSymbols[varName] = SymbolInfo(
                    varName, SymbolType.VARIABLE, dataType, lineIndex,
                    priority, currentScope, "$dataType array"
                )
            }
        }
        
        // Second pattern: regular arrays like "int arr[10];"
        val arrayPattern = Pattern.compile(
            """((?:const\s+)?(?:unsigned\s+)?(?:int|long\s+long|long|double|float|char|bool|string|[A-Z]\w*))\s+(\w+)\s*\[[^\]]*\]"""
        )
        
        val arrayMatcher = arrayPattern.matcher(line)
        while (arrayMatcher.find()) {
            val baseType = arrayMatcher.group(1) ?: continue
            val varName = arrayMatcher.group(2) ?: continue
            
            // Skip if this variable was already added as a pointer array
            if (localSymbols.containsKey(varName)) continue
            
            if (!isKeyword(varName)) {
                val dataType = "${baseType}[]"
                val priority = if (currentScope == 0) PRIORITY_GLOBAL_VARIABLE else PRIORITY_LOCAL_VARIABLE
                
                localSymbols[varName] = SymbolInfo(
                    varName, SymbolType.VARIABLE, dataType, lineIndex,
                    priority, currentScope, "$dataType array"
                )
            }
        }
    }
    
    private fun extractStructDefinitions(line: String, lineIndex: Int, allLines: List<String>) {
        val structPattern = Pattern.compile("""struct\s+(\w+)\s*\{""")
        val structMatcher = structPattern.matcher(line)
        
        if (structMatcher.find()) {
            val structName = structMatcher.group(1) ?: return
            val members = mutableListOf<StructMember>()
            
            // Parse struct members from following lines
            var currentLine = lineIndex + 1
            var braceCount = 1
            
            while (currentLine < allLines.size && braceCount > 0) {
                val memberLine = allLines[currentLine].trim()
                
                // Count braces to know when struct ends
                braceCount += memberLine.count { it == '{' }
                braceCount -= memberLine.count { it == '}' }
                
                if (braceCount > 0) {
                    // Parse member declarations
                    val memberPattern = Pattern.compile("""(int|long|double|float|char|string|bool|[A-Z]\w*)\s+(\w+)\s*;""")
                    val memberMatcher = memberPattern.matcher(memberLine)
                    
                    while (memberMatcher.find()) {
                        val memberType = memberMatcher.group(1) ?: continue
                        val memberName = memberMatcher.group(2) ?: continue
                        
                        members.add(StructMember(memberName, memberType, currentLine))
                    }
                }
                
                currentLine++
            }
            
            if (members.isNotEmpty()) {
                structDefinitions[structName] = StructInfo(structName, members, lineIndex)
            }
        }
    }
    
    private fun isKeyword(word: String): Boolean {
        return CPP_KEYWORDS.contains(word.lowercase()) || 
               JAVA_KEYWORDS.contains(word.lowercase()) ||
               PYTHON_KEYWORDS.contains(word.lowercase())
    }
    
    private fun calculateRelevanceScore(suggestion: String, prefix: String): Int {
        return when {
            suggestion.equals(prefix, ignoreCase = true) -> 0
            suggestion.startsWith(prefix, ignoreCase = true) -> 1
            suggestion.contains(prefix, ignoreCase = true) -> 2
            else -> 3
        }
    }
    
    /**
     * Extended completion item with priority for sorting
     */
    class PriorityCompletionItem(
        label: CharSequence,
        desc: CharSequence,
        prefixLength: Int,
        commitText: String,
        val priority: Int,
        kind: CompletionItemKind
    ) : SimpleCompletionItem(label, desc, prefixLength, commitText) {
        
        init {
            this.kind(kind)
        }
    }
}