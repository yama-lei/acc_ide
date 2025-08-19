package com.acc_ide.completion.providers

import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import com.acc_ide.completion.core.CompletionConstants
import com.acc_ide.completion.core.ACMCompletionProvider.PriorityCompletionItem

/**
 * STL和标准库补全提供器
 * 提供STL容器、算法和方法的补全
 */
class STLProvider {
    
    companion object {
        // STL 容器
        private val STL_CONTAINERS = listOf(
            "vector", "string", "map", "set", "unordered_map", "unordered_set",
            "queue", "priority_queue", "stack", "deque", "pair", "array",
            "list", "multiset", "multimap"
        )
        
        
        // Vector 方法
        private val VECTOR_METHODS = mapOf(
            "push_back" to CompletionConstants.PRIORITY_STL_COMMON,
            "size" to CompletionConstants.PRIORITY_STL_COMMON,
            "empty" to CompletionConstants.PRIORITY_STL_COMMON,
            "clear" to CompletionConstants.PRIORITY_STL_COMMON,
            "begin" to CompletionConstants.PRIORITY_STL_COMMON,
            "end" to CompletionConstants.PRIORITY_STL_COMMON,
            "front" to CompletionConstants.PRIORITY_STL_FUNCTION,
            "back" to CompletionConstants.PRIORITY_STL_FUNCTION,
            "pop_back" to CompletionConstants.PRIORITY_STL_FUNCTION,
            "insert" to CompletionConstants.PRIORITY_STL_FUNCTION,
            "erase" to CompletionConstants.PRIORITY_STL_FUNCTION
        )
        
        // String 方法
        private val STRING_METHODS = mapOf(
            "size" to CompletionConstants.PRIORITY_STL_COMMON,
            "length" to CompletionConstants.PRIORITY_STL_COMMON,
            "empty" to CompletionConstants.PRIORITY_STL_COMMON,
            "clear" to CompletionConstants.PRIORITY_STL_COMMON,
            "substr" to CompletionConstants.PRIORITY_STL_COMMON,
            "find" to CompletionConstants.PRIORITY_STL_COMMON,
            "append" to CompletionConstants.PRIORITY_STL_FUNCTION,
            "insert" to CompletionConstants.PRIORITY_STL_FUNCTION,
            "erase" to CompletionConstants.PRIORITY_STL_FUNCTION,
            "replace" to CompletionConstants.PRIORITY_STL_FUNCTION,
            "c_str" to CompletionConstants.PRIORITY_STL_FUNCTION
        )
        
        // Map 方法
        private val MAP_METHODS = mapOf(
            "find" to CompletionConstants.PRIORITY_STL_COMMON,
            "insert" to CompletionConstants.PRIORITY_STL_COMMON,
            "erase" to CompletionConstants.PRIORITY_STL_COMMON,
            "size" to CompletionConstants.PRIORITY_STL_COMMON,
            "empty" to CompletionConstants.PRIORITY_STL_COMMON,
            "clear" to CompletionConstants.PRIORITY_STL_COMMON,
            "begin" to CompletionConstants.PRIORITY_STL_FUNCTION,
            "end" to CompletionConstants.PRIORITY_STL_FUNCTION,
            "count" to CompletionConstants.PRIORITY_STL_FUNCTION,
            "lower_bound" to CompletionConstants.PRIORITY_STL_FUNCTION,
            "upper_bound" to CompletionConstants.PRIORITY_STL_FUNCTION
        )
        
    }
    
    /**
     * 获取STL补全
     */
    fun getSTLCompletions(prefix: String, language: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        when (language.lowercase()) {
            "cpp", "c++" -> {
                items.addAll(getContainerCompletions(lowerPrefix, prefix.length))
            }
        }
        
        return items
    }
    
    /**
     * 获取成员方法补全
     */
    fun getMemberCompletions(dataType: String, prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        
        when (dataType.lowercase()) {
            "vector" -> {
                items.addAll(getMethodCompletions(prefix, VECTOR_METHODS, "vector"))
            }
            "string" -> {
                items.addAll(getMethodCompletions(prefix, STRING_METHODS, "string"))
            }
            "map", "unordered_map" -> {
                items.addAll(getMethodCompletions(prefix, MAP_METHODS, "map"))
            }
            else -> {
                // 如果类型未知，不提供补全
            }
        }
        
        return items
    }
    
    
    /**
     * 获取容器补全
     */
    private fun getContainerCompletions(prefix: String, prefixLength: Int): List<CompletionItem> {
        return STL_CONTAINERS
            .filter { it.startsWith(prefix) }
            .map { container ->
                PriorityCompletionItem(
                    container,
                    "STL Container",
                    prefixLength,
                    container,
                    CompletionConstants.PRIORITY_STL_COMMON,
                    CompletionItemKind.Class
                )
            }
    }
    
    
    /**
     * 获取方法补全
     */
    private fun getMethodCompletions(
        prefix: String,
        methods: Map<String, Int>,
        containerType: String
    ): List<CompletionItem> {
        return methods.entries
            .filter { it.key.startsWith(prefix, ignoreCase = true) }
            .map { (method, priority) ->
                PriorityCompletionItem(
                    method,
                    "$containerType method",
                    prefix.length,
                    if (method.endsWith("()")) method else "$method()",
                    priority,
                    CompletionItemKind.Method
                )
            }
    }
    
}