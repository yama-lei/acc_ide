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
        
        // STL 算法
        private val STL_ALGORITHMS = listOf(
            "sort", "reverse", "find", "binary_search", "lower_bound", "upper_bound",
            "max", "min", "max_element", "min_element", "accumulate", "count",
            "unique", "next_permutation", "prev_permutation", "fill", "copy",
            "swap", "make_pair"
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
            "erase" to CompletionConstants.PRIORITY_STL_FUNCTION,
            "resize" to CompletionConstants.PRIORITY_STL_UNCOMMON,
            "reserve" to CompletionConstants.PRIORITY_STL_UNCOMMON,
            "capacity" to CompletionConstants.PRIORITY_STL_UNCOMMON,
            "shrink_to_fit" to CompletionConstants.PRIORITY_STL_UNCOMMON
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
            "c_str" to CompletionConstants.PRIORITY_STL_FUNCTION,
            "compare" to CompletionConstants.PRIORITY_STL_UNCOMMON
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
        
        // 通用容器方法
        private val COMMON_CONTAINER_METHODS = listOf(
            "size", "empty", "clear", "begin", "end", "push_back", "pop_back", "insert", "erase"
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
                items.addAll(getAlgorithmCompletions(lowerPrefix, prefix.length))
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
                // 如果类型未知，提供通用方法
                items.addAll(getCommonMethodCompletions(prefix))
            }
        }
        
        return items
    }
    
    /**
     * 基于变量名推断类型并提供成员补全
     * 当TreeSitter不可用时的降级方案
     */
    fun inferMemberCompletions(variableName: String, prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerVarName = variableName.lowercase()
        
        // 基于变量名推断类型
        val inferredType = when {
            lowerVarName.contains("vec") || lowerVarName.contains("arr") -> "vector"
            lowerVarName.contains("str") || lowerVarName.contains("s") -> "string"
            lowerVarName.contains("map") || lowerVarName.contains("mp") -> "map"
            lowerVarName.contains("set") -> "set"
            lowerVarName.contains("queue") || lowerVarName.contains("q") -> "queue"
            lowerVarName.contains("stack") || lowerVarName.contains("st") -> "stack"
            else -> "unknown"
        }
        
        items.addAll(getMemberCompletions(inferredType, prefix))
        
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
     * 获取算法补全
     */
    private fun getAlgorithmCompletions(prefix: String, prefixLength: Int): List<CompletionItem> {
        return STL_ALGORITHMS
            .filter { it.startsWith(prefix) }
            .map { algorithm ->
                PriorityCompletionItem(
                    algorithm,
                    "STL Algorithm",
                    prefixLength,
                    "$algorithm()",
                    CompletionConstants.PRIORITY_STL_COMMON,
                    CompletionItemKind.Function
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
    
    /**
     * 获取通用方法补全
     */
    private fun getCommonMethodCompletions(prefix: String): List<CompletionItem> {
        return COMMON_CONTAINER_METHODS
            .filter { it.startsWith(prefix, ignoreCase = true) }
            .map { method ->
                PriorityCompletionItem(
                    method,
                    "Common STL method",
                    prefix.length,
                    "$method()",
                    CompletionConstants.PRIORITY_STL_FUNCTION,
                    CompletionItemKind.Method
                )
            }
    }
}