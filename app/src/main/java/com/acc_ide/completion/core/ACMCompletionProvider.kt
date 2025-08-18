package com.acc_ide.completion.core

import android.os.Bundle
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.text.Content
import com.acc_ide.completion.providers.KeywordProvider
import com.acc_ide.completion.providers.STLProvider
import com.acc_ide.completion.services.NativeTreeSitterService

/**
 * ACM竞赛编程智能补全提供器
 * 集成混合语言系统，支持TreeSitter增强的作用域感知补全
 * 
 * 工作原理:
 * 1. 优先使用TreeSitter分析结果进行智能补全
 * 2. 当TreeSitter不可用时，降级到基于正则表达式的补全
 * 3. 与ACMHybridLanguage协作提供更精确的补全建议
 */
class ACMCompletionProvider {
    
    private val keywordProvider = KeywordProvider()
    private val stlProvider = STLProvider()
    private val usageFrequency = mutableMapOf<String, Int>()
    
    // 原生TreeSitter服务（用于语义分析）
    private val treeSitterService = NativeTreeSitterService()
    
    // 当前关联的混合语言实例，用于获取TreeSitter分析结果 (临时禁用)
    // private var hybridLanguage: ACMHybridLanguage? = null
    
    // 移除重复常量定义，使用CompletionConstants
    
    /**
     * 设置关联的混合语言实例 (临时禁用)
     */
    // fun setHybridLanguage(language: ACMHybridLanguage?) {
    //     this.hybridLanguage = language
    // }
    
    /**
     * 主要的自动补全入口点
     * 与sora-editor的Language接口兼容
     */
    fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        try {
            // 获取前缀
            val prefix = extractPrefix(content, position)
            
            // 推断语言类型 (临时固定为cpp)
            val language = "cpp"
            
            android.util.Log.d("ACMCompletionProvider", "requireAutoComplete called: prefix='$prefix', language='$language'")
            
            // 调用补全逻辑
            provideCompletions(content, position, prefix, language, publisher)
        } catch (e: Exception) {
            android.util.Log.e("ACMCompletionProvider", "Completion failed, falling back to keywords", e)
            // 降级到关键字补全
            val prefix = extractPrefix(content, position)
            val items = keywordProvider.getKeywordCompletions(prefix, "cpp")
            publisher.addItems(items)
            android.util.Log.d("ACMCompletionProvider", "Published ${items.size} fallback keywords")
        }
    }
    
    /**
     * 提取前缀
     */
    private fun extractPrefix(content: ContentReference, position: CharPosition): String {
        try {
            val line = content.getLine(position.line)
            val beforeCursor = line.substring(0, minOf(position.column, line.length))
            
            // 提取当前单词作为前缀
            val match = Regex("""[a-zA-Z_][a-zA-Z0-9_]*$""").find(beforeCursor)
            return match?.value ?: ""
        } catch (e: Exception) {
            return ""
        }
    }
    
    /**
     * 提供代码补全建议
     */
    fun provideCompletions(
        contentRef: ContentReference,
        position: CharPosition,
        prefix: String,
        language: String,
        publisher: CompletionPublisher
    ) {
        val items = mutableListOf<CompletionItem>()
        
        try {
            android.util.Log.d("ACMCompletionProvider", "provideCompletions: prefix='$prefix'")
            
            // 检查是否为成员访问（例如：obj.）
            val (actualPrefix, contextVar) = extractMemberAccessContext(contentRef, position, prefix)
            
            android.util.Log.d("ACMCompletionProvider", "Extracted: actualPrefix='$actualPrefix', contextVar='$contextVar'")
            
            if (contextVar != null) {
                // 处理成员访问补全
                items.addAll(handleMemberCompletion(contentRef, language, contextVar, actualPrefix))
                android.util.Log.d("ACMCompletionProvider", "Added ${items.size} member completions")
            } else {
                // 常规补全
                items.addAll(handleRegularCompletion(contentRef, language, position, actualPrefix))
                android.util.Log.d("ACMCompletionProvider", "Added ${items.size} regular completions")
            }
            
            // 更新使用频率
            updateUsageFrequency(prefix)
            
            // 排序并发布结果
            publishSortedItems(items, actualPrefix, publisher)
            android.util.Log.d("ACMCompletionProvider", "Published ${items.size} total completions")
            
        } catch (e: Exception) {
            android.util.Log.e("ACMCompletionProvider", "provideCompletions failed", e)
            // 降级到关键字补全
            items.addAll(keywordProvider.getKeywordCompletions(prefix, language))
            publisher.addItems(items)
            android.util.Log.d("ACMCompletionProvider", "Published ${items.size} fallback completions")
        }
    }
    
    /**
     * 处理成员访问补全
     */
    private fun handleMemberCompletion(
        contentRef: ContentReference,
        language: String,
        contextVar: String,
        prefix: String
    ): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        
        try {
            android.util.Log.d("ACMCompletionProvider", "handleMemberCompletion: contextVar='$contextVar', prefix='$prefix'")
            
            // 使用原生TreeSitter服务查找上下文变量（获取所有变量）
            val parseResult = treeSitterService.parseCode(contentRef, language)
            val localVariables = parseResult?.symbols?.filter {
                it.type == SymbolType.VARIABLE || it.type == SymbolType.PARAMETER
            } ?: emptyList()
            
            // 查找上下文变量的类型信息
            val contextSymbol = localVariables.find { it.name == contextVar }
            if (contextSymbol != null) {
                android.util.Log.d("ACMCompletionProvider", "Found context variable: ${contextSymbol.name} of type ${contextSymbol.dataType}")
                
                // 首先打印所有可用的符号用于调试
                android.util.Log.d("ACMCompletionProvider", "All available symbols:")
                parseResult?.symbols?.forEach { symbol ->
                    android.util.Log.d("ACMCompletionProvider", "  Symbol: ${symbol.name}, type: ${symbol.type}, dataType: ${symbol.dataType}, parentStruct: ${symbol.parentStruct}")
                }
                
                // 1. 优先检查是否为STL容器类型
                if (isSTLType(contextSymbol.dataType)) {
                    android.util.Log.d("ACMCompletionProvider", "Type ${contextSymbol.dataType} is STL container")
                    items.addAll(stlProvider.getMemberCompletions(contextSymbol.dataType, prefix))
                    return items
                }
                
                // 2. 检查是否为基本类型（基本类型没有成员）
                if (isPrimitiveType(contextSymbol.dataType)) {
                    android.util.Log.d("ACMCompletionProvider", "Type ${contextSymbol.dataType} is primitive type - no members")
                    return items // 返回空列表
                }
                
                // 3. 尝试获取用户定义的struct成员
                val structMembers = getStructMembers(contentRef, language, contextSymbol.dataType, prefix)
                if (structMembers.isNotEmpty()) {
                    android.util.Log.d("ACMCompletionProvider", "Added ${structMembers.size} struct members for type ${contextSymbol.dataType}")
                    items.addAll(structMembers)
                    return items
                }
                
                // 4. 如果没有找到struct成员，可能是未识别的类型，提供通用STL方法
                android.util.Log.d("ACMCompletionProvider", "Type ${contextSymbol.dataType} not recognized, trying STL fallback")
                items.addAll(stlProvider.getMemberCompletions(contextSymbol.dataType, prefix))
                
            } else {
                android.util.Log.d("ACMCompletionProvider", "Context variable '$contextVar' not found in TreeSitter results")
                // 临时禁用STL推断，优先让struct成员补全工作
                // items.addAll(stlProvider.inferMemberCompletions(contextVar, prefix))
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ACMCompletionProvider", "TreeSitter member completion failed", e)
            // 临时禁用STL推断，优先让struct成员补全工作
            // items.addAll(stlProvider.inferMemberCompletions(contextVar, prefix))
        }
        
        return items
    }
    
    /**
     * 判断是否为STL容器类型
     */
    private fun isSTLType(dataType: String): Boolean {
        val stlTypes = setOf(
            "vector", "string", "map", "set", "unordered_map", "unordered_set",
            "queue", "priority_queue", "stack", "deque", "list", "array",
            "multiset", "multimap", "pair"
        )
        return stlTypes.contains(dataType.lowercase())
    }
    
    /**
     * 判断是否为基本类型
     */
    private fun isPrimitiveType(dataType: String): Boolean {
        val primitiveTypes = setOf(
            "int", "long", "short", "char", "byte",
            "float", "double", "bool", "boolean",
            "void", "auto", "size_t", "uint32_t", "int64_t"
        )
        return primitiveTypes.contains(dataType.lowercase())
    }
    
    /**
     * 获取struct成员补全
     */
    private fun getStructMembers(
        contentRef: ContentReference,
        language: String,
        structTypeName: String,
        prefix: String
    ): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        
        try {
            // 获取所有symbols
            val parseResult = treeSitterService.parseCode(contentRef, language)
            val allSymbols = parseResult?.symbols ?: emptyList()
            
            // 查找指定struct的成员
            val structMembers = allSymbols.filter { symbol ->
                symbol.type == SymbolType.STRUCT_MEMBER && 
                symbol.parentStruct == structTypeName &&
                symbol.name.startsWith(prefix, ignoreCase = true)
            }
            
            android.util.Log.d("ACMCompletionProvider", "Found ${structMembers.size} members for struct '$structTypeName'")
            
            // 转换为补全项
            structMembers.forEach { member ->
                android.util.Log.d("ACMCompletionProvider", "  Member: ${member.name} (${member.dataType})")
                
                val item = PriorityCompletionItem(
                    member.name,
                    "${member.dataType} - ${member.description}",
                    prefix.length,
                    member.name,
                    CompletionConstants.PRIORITY_STRUCT_MEMBER,
                    CompletionItemKind.Field
                )
                items.add(item)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("ACMCompletionProvider", "Failed to get struct members", e)
        }
        
        return items
    }
    
    /**
     * 处理常规补全
     */
    private fun handleRegularCompletion(
        contentRef: ContentReference,
        language: String,
        position: CharPosition,
        prefix: String
    ): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        
        try {
            // 使用原生TreeSitter服务进行语义分析
            android.util.Log.d("ACMCompletionProvider", "Getting symbols at position ${position.line}:${position.column}")
            val symbolsAtPosition = treeSitterService.getSymbolsAtPosition(contentRef, language, position.line, position.column)
            
            android.util.Log.d("ACMCompletionProvider", "Found ${symbolsAtPosition.size} symbols at position")
            
            if (symbolsAtPosition.isNotEmpty()) {
                // 获取所有可见符号
                val localVariables = treeSitterService.getLocalVariables(contentRef, language, position.line, position.column)
                val functions = treeSitterService.getFunctionDefinitions(contentRef, language)
                val classes = treeSitterService.getClassDefinitions(contentRef, language)
                
                android.util.Log.d("ACMCompletionProvider", "Found: ${localVariables.size} vars, ${functions.size} functions, ${classes.size} classes")
                
                // 合并所有符号
                val allSymbols = (localVariables + functions + classes).distinctBy { "${it.name}_${it.type}" }
                
                // 转换为补全项
                val symbolItems = convertSymbolsToCompletionItems(allSymbols, prefix, 0)
                items.addAll(symbolItems)
                android.util.Log.d("ACMCompletionProvider", "Converted ${symbolItems.size} symbol items")
            }
        } catch (e: Exception) {
            android.util.Log.e("ACMCompletionProvider", "TreeSitter symbol extraction failed", e)
            // TreeSitter解析失败时，继续提供基础补全
        }
        
        // 总是提供语言关键字和STL补全（即使TreeSitter不可用）
        items.addAll(keywordProvider.getKeywordCompletions(prefix, language))
        items.addAll(stlProvider.getSTLCompletions(prefix, language))
        
        return items
    }
    
    
    /**
     * 提取成员访问上下文
     */
    private fun extractMemberAccessContext(
        contentRef: ContentReference,
        position: CharPosition,
        prefix: String
    ): Pair<String, String?> {
        try {
            val line = contentRef.getLine(position.line)
            val beforeCursor = line.substring(0, position.column)
            
            // 检查点号操作符 (.)
            val lastDotIndex = beforeCursor.lastIndexOf('.')
            // 检查箭头操作符 (->)
            val lastArrowIndex = beforeCursor.lastIndexOf("->")
            
            // 选择最靠近光标的操作符
            val (operatorIndex, operatorLength) = when {
                lastDotIndex > lastArrowIndex -> Pair(lastDotIndex, 1)
                lastArrowIndex >= 0 -> Pair(lastArrowIndex, 2)
                else -> Pair(-1, 0)
            }
            
            if (operatorIndex > 0) {
                val beforeOperator = beforeCursor.substring(0, operatorIndex).trim()
                val afterOperator = beforeCursor.substring(operatorIndex + operatorLength)
                
                // 提取变量名（简化实现）
                val words = beforeOperator.split(Regex("\\W+"))
                val varName = words.lastOrNull { it.isNotEmpty() && it[0].isLetter() }
                
                if (varName != null) {
                    return Pair(afterOperator, varName)
                }
            }
            
            return Pair(prefix, null)
        } catch (e: Exception) {
            return Pair(prefix, null)
        }
    }
    
    /**
     * 将符号转换为补全项
     */
    private fun convertSymbolsToCompletionItems(
        symbols: List<SymbolInfo>,
        prefix: String,
        currentScopeLevel: Int
    ): List<CompletionItem> {
        return symbols
            .filter { it.name.startsWith(prefix, ignoreCase = true) }
            .map { symbol ->
                val priority = calculateSymbolPriority(symbol, currentScopeLevel)
                val kind = when (symbol.type) {
                    SymbolType.VARIABLE -> CompletionItemKind.Variable
                    SymbolType.FUNCTION -> CompletionItemKind.Function
                    SymbolType.CLASS -> CompletionItemKind.Class
                    SymbolType.STRUCT -> CompletionItemKind.Struct
                    SymbolType.ENUM -> CompletionItemKind.Enum
                    SymbolType.PARAMETER -> CompletionItemKind.Variable
                    SymbolType.STRUCT_MEMBER -> CompletionItemKind.Field
                }
                
                PriorityCompletionItem(
                    symbol.name,
                    "${symbol.dataType} ${symbol.description}",
                    prefix.length,
                    symbol.name,
                    priority,
                    kind
                )
            }
    }
    
    /**
     * 将结构体转换为补全项
     */
    private fun convertStructsToCompletionItems(
        structs: List<StructInfo>,
        prefix: String
    ): List<CompletionItem> {
        return structs
            .filter { it.name.startsWith(prefix, ignoreCase = true) }
            .map { struct ->
                PriorityCompletionItem(
                    struct.name,
                    "User-defined struct",
                    prefix.length,
                    struct.name,
                    CompletionConstants.PRIORITY_TYPE,
                    CompletionItemKind.Struct
                )
            }
    }
    
    /**
     * 计算符号优先级
     */
    private fun calculateSymbolPriority(symbol: SymbolInfo, currentScopeLevel: Int): Int {
        val basePriority = when {
            symbol.scopeLevel == currentScopeLevel -> CompletionConstants.PRIORITY_RECENT_LOCAL
            symbol.scopeLevel == 0 -> CompletionConstants.PRIORITY_GLOBAL_VARIABLE
            symbol.type == SymbolType.FUNCTION -> CompletionConstants.PRIORITY_LOCAL_FUNCTION
            else -> CompletionConstants.PRIORITY_LOCAL_VARIABLE
        }
        
        // 根据使用频率调整优先级
        val usageBoost = if ((usageFrequency[symbol.name] ?: 0) > 2) 10 else 0
        return basePriority + usageBoost
    }
    
    /**
     * 更新使用频率
     */
    private fun updateUsageFrequency(prefix: String) {
        if (prefix.isNotEmpty()) {
            usageFrequency[prefix] = (usageFrequency[prefix] ?: 0) + 1
        }
    }
    
    /**
     * 发布排序后的补全项
     */
    private fun publishSortedItems(
        items: List<CompletionItem>,
        prefix: String,
        publisher: CompletionPublisher
    ) {
        val sortedItems = items.sortedWith(
            compareByDescending<CompletionItem> { 
                (it as? PriorityCompletionItem)?.priority ?: 0
            }.thenBy { 
                calculateRelevanceScore(it.label.toString(), prefix) 
            }.thenByDescending {
                usageFrequency[it.label.toString()] ?: 0
            }.thenBy { 
                it.label.toString() 
            }
        )
        
        publisher.addItems(sortedItems)
    }
    
    /**
     * 计算相关性分数
     */
    private fun calculateRelevanceScore(suggestion: String, prefix: String): Int {
        return when {
            suggestion.equals(prefix, ignoreCase = true) -> 0
            suggestion.startsWith(prefix, ignoreCase = true) -> 1
            suggestion.contains(prefix, ignoreCase = true) -> 2
            else -> 3
        }
    }
    
    /**
     * 带优先级的补全项
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