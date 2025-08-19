package com.acc_ide.completion.languages

import android.util.Log
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import com.acc_ide.completion.core.*
import com.acc_ide.completion.framework.AbstractTreeSitterProcessor
import com.acc_ide.completion.framework.UniversalCompletionEngine.PriorityCompletionItem

/**
 * Python 语言处理器
 * 
 * TODO: 实现Python特定的补全逻辑
 * - 动态类型推断
 * - 装饰器补全
 * - 内置函数补全
 * - 模块导入补全
 * - 列表推导式补全
 */
class PythonLanguageProcessor : AbstractTreeSitterProcessor() {
    
    private val TAG = "PythonLanguageProcessor"
    
    
    override fun getLanguageId(): String = "python"
    
    override fun getLanguageName(): String = "Python"
    
    override fun getSupportedExtensions(): Set<String> = setOf("py", "pyw", "pyi")
    
    // parseCode() 现在由 AbstractTreeSitterProcessor 提供
    
    // getSymbolsAtPosition() 现在由 AbstractTreeSitterProcessor 提供
    
    override fun provideRegularCompletions(
        contentRef: ContentReference,
        position: CharPosition,
        prefix: String
    ): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        
        // TODO: 实现Python特定的常规补全
        Log.d(TAG, "Python regular completions not implemented yet")
        
        // 提供基础关键字补全
        items.addAll(getKeywordCompletions(prefix))
        
        return items
    }
    
    override fun provideMemberCompletions(
        contentRef: ContentReference,
        position: CharPosition,
        contextVar: String,
        prefix: String
    ): List<CompletionItem> {
        // TODO: 实现Python特定的成员补全
        Log.d(TAG, "Python member completions not implemented yet")
        return emptyList()
    }
    
    override fun getKeywords(): Set<String> {
        return setOf(
            // Python关键字
            "False", "None", "True", "and", "as", "assert", "async", "await", "break",
            "class", "continue", "def", "del", "elif", "else", "except", "finally",
            "for", "from", "global", "if", "import", "in", "is", "lambda", "nonlocal",
            "not", "or", "pass", "raise", "return", "try", "while", "with", "yield"
        )
    }
    
    override fun getStandardLibraryItems(): Map<String, List<String>> {
        return mapOf(
            "str" to listOf("lower", "upper", "strip", "split", "join", "replace", "find", "startswith", "endswith"),
            "list" to listOf("append", "extend", "insert", "remove", "pop", "clear", "index", "count", "sort", "reverse"),
            "dict" to listOf("get", "keys", "values", "items", "pop", "popitem", "clear", "update", "setdefault"),
            "set" to listOf("add", "remove", "discard", "pop", "clear", "update", "intersection", "union", "difference"),
            "tuple" to listOf("count", "index"),
            "file" to listOf("read", "write", "readline", "readlines", "close", "seek", "tell")
        )
    }
    
    override fun isPrimitiveType(dataType: String): Boolean {
        val primitiveTypes = setOf(
            "int", "float", "str", "bool", "bytes", "complex", "none"
        )
        return primitiveTypes.contains(dataType.lowercase())
    }
    
    override fun isStandardLibraryType(dataType: String): Boolean {
        val standardTypes = setOf(
            "list", "dict", "tuple", "set", "frozenset", "range", "enumerate",
            "zip", "map", "filter", "str", "bytes", "bytearray"
        )
        return standardTypes.contains(dataType.lowercase())
    }
    
    override fun extractMemberAccessContext(
        contentRef: ContentReference,
        position: CharPosition,
        prefix: String
    ): Pair<String, String?> {
        return try {
            val line = contentRef.getLine(position.line)
            val beforeCursor = line.substring(0, position.column)
            
            // Python只有点号操作符
            val lastDotIndex = beforeCursor.lastIndexOf('.')
            
            if (lastDotIndex > 0) {
                val beforeOperator = beforeCursor.substring(0, lastDotIndex).trim()
                val afterOperator = beforeCursor.substring(lastDotIndex + 1)
                
                // 提取变量名（Python支持更复杂的表达式）
                val words = beforeOperator.split(Regex("\\W+"))
                val varName = words.lastOrNull { it.isNotEmpty() && (it[0].isLetter() || it[0] == '_') }
                
                if (varName != null) {
                    return Pair(afterOperator, varName)
                }
            }
            
            Pair(prefix, null)
        } catch (e: Exception) {
            Pair(prefix, null)
        }
    }
    
    override fun extractPrefix(contentRef: ContentReference, position: CharPosition): String {
        return try {
            val line = contentRef.getLine(position.line)
            val beforeCursor = line.substring(0, minOf(position.column, line.length))
            
            // Python标识符可以包含下划线开头
            val match = Regex("""[a-zA-Z_][a-zA-Z0-9_]*$""").find(beforeCursor)
            match?.value ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    // isAvailable() 现在由 AbstractTreeSitterProcessor 提供
    
    /**
     * 获取关键字补全项
     */
    private fun getKeywordCompletions(prefix: String): List<CompletionItem> {
        return getKeywords()
            .filter { it.startsWith(prefix, ignoreCase = true) }
            .map { keyword ->
                PriorityCompletionItem(
                    keyword,
                    "Python keyword",
                    prefix.length,
                    keyword,
                    CompletionConstants.PRIORITY_KEYWORD,
                    CompletionItemKind.Keyword
                )
            }
    }
}