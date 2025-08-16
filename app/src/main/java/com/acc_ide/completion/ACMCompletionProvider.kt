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
 * Provides keyword completion and local symbol table management for competitive programming
 */
class ACMCompletionProvider {
    
    companion object {
        // Priority levels for sorting
        const val PRIORITY_KEYWORD = 100
        const val PRIORITY_STL_FUNCTION = 90
        const val PRIORITY_COMMON_FUNCTION = 80
        const val PRIORITY_LOCAL_VARIABLE = 70
        const val PRIORITY_LOCAL_FUNCTION = 60
        const val PRIORITY_TYPE = 50
        
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
        
        // STL algorithms commonly used in competitive programming
        private val STL_ALGORITHMS = listOf(
            "sort", "reverse", "find", "binary_search", "lower_bound", "upper_bound",
            "max", "min", "max_element", "min_element", "accumulate", "count",
            "unique", "next_permutation", "prev_permutation", "fill", "copy",
            "swap", "make_pair", "push_back", "pop_back", "insert", "erase",
            "clear", "size", "empty", "begin", "end", "front", "back"
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
    
    private val localSymbols = mutableMapOf<String, SymbolInfo>()
    
    data class SymbolInfo(
        val name: String,
        val type: SymbolType,
        val line: Int,
        val priority: Int,
        val description: String = ""
    )
    
    enum class SymbolType {
        VARIABLE, FUNCTION, CLASS, STRUCT, ENUM
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
        if (prefix.isEmpty()) return
        
        val items = mutableListOf<CompletionItem>()
        
        // Add language-specific completions
        when (language.lowercase()) {
            "cpp", "c++" -> {
                items.addAll(getCppCompletions(prefix))
            }
            "java" -> {
                items.addAll(getJavaCompletions(prefix))
            }
            "python", "py" -> {
                items.addAll(getPythonCompletions(prefix))
            }
        }
        
        // Add local symbols
        items.addAll(getLocalSymbolCompletions(prefix))
        
        // Update local symbol table
        updateLocalSymbols(contentRef)
        
        // Sort by priority and relevance
        val sortedItems = items.sortedWith(compareBy<CompletionItem> { 
            -((it as? PriorityCompletionItem)?.priority ?: 0)
        }.thenBy { 
            calculateRelevanceScore(it.label.toString(), prefix) 
        })
        
        publisher.addItems(sortedItems)
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
                PRIORITY_STL_FUNCTION, CompletionItemKind.Class
            ))
        }
        
        // STL algorithms
        STL_ALGORITHMS.filter { it.startsWith(lowerPrefix) }.forEach { algorithm ->
            items.add(PriorityCompletionItem(
                algorithm, "STL Algorithm", prefix.length, "$algorithm()",
                PRIORITY_STL_FUNCTION, CompletionItemKind.Function
            ))
        }
        
        // ACM templates
        ACM_TEMPLATES.entries.filter { it.key.startsWith(lowerPrefix) }.forEach { (name, template) ->
            items.add(PriorityCompletionItem(
                name, "ACM Template", prefix.length, template,
                PRIORITY_COMMON_FUNCTION, CompletionItemKind.Snippet
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
                PRIORITY_STL_FUNCTION, CompletionItemKind.Class
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
                PRIORITY_STL_FUNCTION, CompletionItemKind.Function
            ))
        }
        
        return items
    }
    
    private fun getLocalSymbolCompletions(prefix: String): List<CompletionItem> {
        return localSymbols.values.filter { 
            it.name.startsWith(prefix, ignoreCase = true) 
        }.map { symbol ->
            val kind = when (symbol.type) {
                SymbolType.VARIABLE -> CompletionItemKind.Variable
                SymbolType.FUNCTION -> CompletionItemKind.Function
                SymbolType.CLASS -> CompletionItemKind.Class
                SymbolType.STRUCT -> CompletionItemKind.Struct
                SymbolType.ENUM -> CompletionItemKind.Enum
            }
            
            PriorityCompletionItem(
                symbol.name, symbol.description.ifEmpty { symbol.type.name.lowercase() },
                prefix.length, symbol.name, symbol.priority, kind
            )
        }
    }
    
    private fun updateLocalSymbols(contentRef: ContentReference) {
        localSymbols.clear()
        
        try {
            val content = contentRef.reference
            val lines = content.toString().lines()
            
            lines.forEachIndexed { lineIndex, line ->
                extractSymbolsFromLine(line, lineIndex)
            }
        } catch (e: Exception) {
            // Handle content access safely
        }
    }
    
    private fun extractSymbolsFromLine(line: String, lineIndex: Int) {
        // Variable declarations (int x, string name, etc.)
        val variablePattern = Pattern.compile("""(?:int|long|double|float|char|string|bool)\s+(\w+)""")
        val variableMatcher = variablePattern.matcher(line)
        while (variableMatcher.find()) {
            val varName = variableMatcher.group(1) ?: continue
            localSymbols[varName] = SymbolInfo(
                varName, SymbolType.VARIABLE, lineIndex, 
                PRIORITY_LOCAL_VARIABLE, "Local variable"
            )
        }
        
        // Function definitions
        val functionPattern = Pattern.compile("""(?:void|int|long|double|float|char|string|bool)\s+(\w+)\s*\(""")
        val functionMatcher = functionPattern.matcher(line)
        while (functionMatcher.find()) {
            val funcName = functionMatcher.group(1) ?: continue
            localSymbols[funcName] = SymbolInfo(
                funcName, SymbolType.FUNCTION, lineIndex,
                PRIORITY_LOCAL_FUNCTION, "Local function"
            )
        }
        
        // Class/struct definitions
        val classPattern = Pattern.compile("""(?:class|struct)\s+(\w+)""")
        val classMatcher = classPattern.matcher(line)
        while (classMatcher.find()) {
            val className = classMatcher.group(1) ?: continue
            localSymbols[className] = SymbolInfo(
                className, SymbolType.CLASS, lineIndex,
                PRIORITY_TYPE, "User-defined type"
            )
        }
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