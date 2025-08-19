package com.acc_ide.completion.providers

import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import com.acc_ide.completion.core.CompletionConstants
import com.acc_ide.completion.framework.UniversalCompletionEngine.PriorityCompletionItem

/**
 * 关键字补全提供器
 * 提供各种编程语言的关键字补全
 */
class KeywordProvider {
    
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
    
    // ACM 模板
    private val ACM_TEMPLATES = mapOf(
        "gcd" to "int gcd(int a, int b) { return b ? gcd(b, a % b) : a; }",
        "lcm" to "int lcm(int a, int b) { return a / gcd(a, b) * b; }",
        "pow_mod" to "long long pow_mod(long long base, long long exp, long long mod) {\n    long long result = 1;\n    while (exp > 0) {\n        if (exp % 2 == 1) result = (result * base) % mod;\n        base = (base * base) % mod;\n        exp /= 2;\n    }\n    return result;\n}",
        "fast_io" to "ios_base::sync_with_stdio(false);\ncin.tie(NULL);",
        "debug" to "#ifdef LOCAL\n#define debug(x) cerr << #x << \" = \" << x << endl\n#else\n#define debug(x)\n#endif"
    )
    
    /**
     * 获取关键字补全
     */
    fun getKeywordCompletions(prefix: String, language: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        when (language.lowercase()) {
            "cpp", "c++" -> {
                // C++ 关键字补全
                items.addAll(CPP_KEYWORDS
                    .filter { it.startsWith(lowerPrefix) }
                    .map { keyword ->
                        PriorityCompletionItem(
                            keyword,
                            "Keyword",
                            prefix.length,
                            keyword,
                            CompletionConstants.PRIORITY_KEYWORD,
                            CompletionItemKind.Keyword
                        )
                    })
                
                // ACM 模板补全
                items.addAll(ACM_TEMPLATES.entries
                    .filter { it.key.startsWith(lowerPrefix) }
                    .map { (name, template) ->
                        PriorityCompletionItem(
                            name,
                            "ACM Template",
                            prefix.length,
                            template,
                            CompletionConstants.PRIORITY_STL_FUNCTION,
                            CompletionItemKind.Snippet
                        )
                    })
            }
        }
        
        return items
    }
    
    
}