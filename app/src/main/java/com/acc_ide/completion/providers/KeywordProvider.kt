package com.acc_ide.completion.providers

import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import com.acc_ide.completion.core.CompletionConstants
import com.acc_ide.completion.core.ACMCompletionProvider.PriorityCompletionItem

/**
 * 关键字补全提供器
 * 提供各种编程语言的关键字补全
 */
class KeywordProvider {
    
    companion object {
        // C++ 关键字
        private val CPP_KEYWORDS = listOf(
            "auto", "break", "case", "char", "const", "continue", "default", "do",
            "double", "else", "enum", "extern", "float", "for", "goto", "if",
            "inline", "int", "long", "register", "return", "short", "signed",
            "sizeof", "static", "struct", "switch", "typedef", "union", "unsigned",
            "void", "volatile", "while", "class", "namespace", "template", "typename",
            "public", "private", "protected", "virtual", "bool", "true", "false",
            "new", "delete", "this", "try", "catch", "throw", "using", "std"
        )
        
        // Java 关键字
        private val JAVA_KEYWORDS = listOf(
            "abstract", "boolean", "break", "byte", "case", "catch", "char", "class",
            "const", "continue", "default", "do", "double", "else", "extends", "final",
            "finally", "float", "for", "goto", "if", "implements", "import", "instanceof",
            "int", "interface", "long", "native", "new", "package", "private", "protected",
            "public", "return", "short", "static", "super", "switch", "synchronized",
            "this", "throw", "throws", "transient", "try", "void", "volatile", "while",
            "true", "false", "null"
        )
        
        // Python 关键字
        private val PYTHON_KEYWORDS = listOf(
            "and", "as", "assert", "break", "class", "continue", "def", "del",
            "elif", "else", "except", "finally", "for", "from", "global", "if",
            "import", "in", "is", "lambda", "not", "or", "pass", "raise",
            "return", "try", "while", "with", "yield", "True", "False", "None"
        )
        
        // Python 内置函数
        private val PYTHON_BUILTINS = listOf(
            "len", "range", "enumerate", "zip", "map", "filter", "sorted", "reversed",
            "max", "min", "sum", "abs", "pow", "round", "int", "float", "str",
            "list", "tuple", "set", "dict", "input", "print", "open", "chr", "ord"
        )
        
        // Java 集合类
        private val JAVA_COLLECTIONS = listOf(
            "ArrayList", "LinkedList", "HashMap", "TreeMap", "HashSet", "TreeSet",
            "PriorityQueue", "Stack", "Queue", "Deque", "Collections", "Arrays",
            "Scanner", "StringBuilder", "StringBuffer"
        )
        
        // ACM 模板
        private val ACM_TEMPLATES = mapOf(
            "gcd" to "int gcd(int a, int b) { return b ? gcd(b, a % b) : a; }",
            "lcm" to "int lcm(int a, int b) { return a / gcd(a, b) * b; }",
            "pow_mod" to "long long pow_mod(long long base, long long exp, long long mod) {\n    long long result = 1;\n    while (exp > 0) {\n        if (exp % 2 == 1) result = (result * base) % mod;\n        base = (base * base) % mod;\n        exp /= 2;\n    }\n    return result;\n}",
            "fast_io" to "ios_base::sync_with_stdio(false);\ncin.tie(NULL);",
            "debug" to "#ifdef LOCAL\n#define debug(x) cerr << #x << \" = \" << x << endl\n#else\n#define debug(x)\n#endif"
        )
    }
    
    /**
     * 获取关键字补全
     */
    fun getKeywordCompletions(prefix: String, language: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        when (language.lowercase()) {
            "cpp", "c++" -> {
                items.addAll(getKeywordItems(CPP_KEYWORDS, lowerPrefix, prefix.length))
                items.addAll(getTemplateItems(ACM_TEMPLATES, lowerPrefix, prefix.length))
            }
            "java" -> {
                items.addAll(getKeywordItems(JAVA_KEYWORDS, lowerPrefix, prefix.length))
                items.addAll(getCollectionItems(JAVA_COLLECTIONS, lowerPrefix, prefix.length))
            }
            "python", "py" -> {
                items.addAll(getKeywordItems(PYTHON_KEYWORDS, lowerPrefix, prefix.length))
                items.addAll(getBuiltinItems(PYTHON_BUILTINS, lowerPrefix, prefix.length))
            }
        }
        
        return items
    }
    
    /**
     * 获取关键字补全项
     */
    private fun getKeywordItems(
        keywords: List<String>,
        prefix: String,
        prefixLength: Int
    ): List<CompletionItem> {
        return keywords
            .filter { it.startsWith(prefix) }
            .map { keyword ->
                PriorityCompletionItem(
                    keyword,
                    "Keyword",
                    prefixLength,
                    keyword,
                    CompletionConstants.PRIORITY_KEYWORD,
                    CompletionItemKind.Keyword
                )
            }
    }
    
    /**
     * 获取模板补全项
     */
    private fun getTemplateItems(
        templates: Map<String, String>,
        prefix: String,
        prefixLength: Int
    ): List<CompletionItem> {
        return templates.entries
            .filter { it.key.startsWith(prefix) }
            .map { (name, template) ->
                PriorityCompletionItem(
                    name,
                    "ACM Template",
                    prefixLength,
                    template,
                    CompletionConstants.PRIORITY_STL_FUNCTION,
                    CompletionItemKind.Snippet
                )
            }
    }
    
    /**
     * 获取集合类补全项
     */
    private fun getCollectionItems(
        collections: List<String>,
        prefix: String,
        prefixLength: Int
    ): List<CompletionItem> {
        return collections
            .filter { it.lowercase().startsWith(prefix) }
            .map { collection ->
                PriorityCompletionItem(
                    collection,
                    "Java Collection",
                    prefixLength,
                    collection,
                    CompletionConstants.PRIORITY_STL_COMMON,
                    CompletionItemKind.Class
                )
            }
    }
    
    /**
     * 获取内置函数补全项
     */
    private fun getBuiltinItems(
        builtins: List<String>,
        prefix: String,
        prefixLength: Int
    ): List<CompletionItem> {
        return builtins
            .filter { it.startsWith(prefix) }
            .map { builtin ->
                PriorityCompletionItem(
                    builtin,
                    "Python Built-in",
                    prefixLength,
                    "$builtin()",
                    CompletionConstants.PRIORITY_STL_COMMON,
                    CompletionItemKind.Function
                )
            }
    }
}