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
 * Java 语言处理器
 * 
 * TODO: 实现Java特定的补全逻辑
 * - 包导入补全
 * - 类和接口补全
 * - 泛型支持
 * - 注解补全
 * - Lambda表达式补全
 */
class JavaLanguageProcessor : AbstractTreeSitterProcessor() {
    
    private val TAG = "JavaLanguageProcessor"
    
    
    override fun getLanguageId(): String = "java"
    
    override fun getLanguageName(): String = "Java"
    
    override fun getSupportedExtensions(): Set<String> = setOf("java")
    
    // parseCode() 现在由 AbstractTreeSitterProcessor 提供
    
    // getSymbolsAtPosition() 现在由 AbstractTreeSitterProcessor 提供
    
    override fun provideRegularCompletions(
        contentRef: ContentReference,
        position: CharPosition,
        prefix: String
    ): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        
        // TODO: 实现Java特定的常规补全
        Log.d(TAG, "Java regular completions not implemented yet")
        
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
        // TODO: 实现Java特定的成员补全
        Log.d(TAG, "Java member completions not implemented yet")
        return emptyList()
    }
    
    override fun getKeywords(): Set<String> {
        return setOf(
            // Java关键字
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "const", "continue", "default", "do", "double", "else", "enum",
            "extends", "final", "finally", "float", "for", "goto", "if", "implements",
            "import", "instanceof", "int", "interface", "long", "native", "new", "package",
            "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient",
            "try", "void", "volatile", "while",
            // Java特有
            "true", "false", "null"
        )
    }
    
    override fun getStandardLibraryItems(): Map<String, List<String>> {
        return mapOf(
            "String" to listOf("length", "charAt", "substring", "indexOf", "replace", "trim", "toLowerCase", "toUpperCase"),
            "List" to listOf("add", "remove", "get", "set", "size", "isEmpty", "clear", "contains", "indexOf"),
            "ArrayList" to listOf("add", "remove", "get", "set", "size", "isEmpty", "clear", "contains", "indexOf"),
            "HashMap" to listOf("put", "get", "remove", "containsKey", "containsValue", "size", "isEmpty", "clear"),
            "Set" to listOf("add", "remove", "contains", "size", "isEmpty", "clear"),
            "HashSet" to listOf("add", "remove", "contains", "size", "isEmpty", "clear")
        )
    }
    
    override fun isPrimitiveType(dataType: String): Boolean {
        val primitiveTypes = setOf(
            "boolean", "byte", "char", "short", "int", "long", "float", "double", "void"
        )
        return primitiveTypes.contains(dataType.lowercase())
    }
    
    override fun isStandardLibraryType(dataType: String): Boolean {
        val standardTypes = setOf(
            "string", "list", "arraylist", "hashmap", "map", "set", "hashset",
            "collection", "iterator", "object"
        )
        return standardTypes.contains(dataType.lowercase())
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
                    "Java keyword",
                    prefix.length,
                    keyword,
                    CompletionConstants.PRIORITY_KEYWORD,
                    CompletionItemKind.Keyword
                )
            }
    }
}