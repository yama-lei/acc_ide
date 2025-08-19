package com.acc_ide.completion.languages

import android.util.Log
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import com.acc_ide.completion.core.*
import com.acc_ide.completion.framework.AbstractTreeSitterProcessor
import com.acc_ide.completion.framework.UniversalCompletionEngine.PriorityCompletionItem
import com.acc_ide.completion.providers.KeywordProvider
import com.acc_ide.completion.providers.STLProvider

/**
 * C++ 语言处理器
 * 
 * 封装现有的C++补全逻辑，实现LanguageProcessor接口
 */
class CppLanguageProcessor : AbstractTreeSitterProcessor() {
    
    private val TAG = "CppLanguageProcessor"
    
    private val keywordProvider = KeywordProvider()
    private val stlProvider = STLProvider()
    
    override fun getLanguageId(): String = "cpp"
    
    override fun getLanguageName(): String = "C++"
    
    override fun getSupportedExtensions(): Set<String> = setOf("cpp", "cxx", "cc", "c", "h", "hpp", "hxx")
    
    // parseCode() 现在由 AbstractTreeSitterProcessor 提供
    
    // getSymbolsAtPosition() 现在由 AbstractTreeSitterProcessor 提供
    
    override fun provideRegularCompletions(
        contentRef: ContentReference,
        position: CharPosition,
        prefix: String
    ): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        
        try {
            Log.d(TAG, "Providing regular completions for prefix: '$prefix'")
            
            // 1. TreeSitter符号补全 - 使用抽象类方法
            val symbolsAtPosition = getLanguageSpecificSymbols(contentRef, position, prefix)
            
            if (symbolsAtPosition.isNotEmpty()) {
                Log.d(TAG, "Found ${symbolsAtPosition.size} symbols from TreeSitter")
                
                // 按类型分组记录
                val variables = symbolsAtPosition.filter { it.type == SymbolType.VARIABLE }
                val parameters = symbolsAtPosition.filter { it.type == SymbolType.PARAMETER }
                val structMembers = symbolsAtPosition.filter { it.type == SymbolType.STRUCT_MEMBER }
                val functions = symbolsAtPosition.filter { it.type == SymbolType.FUNCTION }
                val classes = symbolsAtPosition.filter { it.type == SymbolType.CLASS || it.type == SymbolType.STRUCT }
                
                Log.d(TAG, "Found: ${variables.size} vars, ${parameters.size} params, ${structMembers.size} members, ${functions.size} functions, ${classes.size} classes")
                
                // 转换为补全项
                val symbolItems = convertSymbolsToCompletionItems(symbolsAtPosition, prefix)
                items.addAll(symbolItems)
                Log.d(TAG, "Converted ${symbolItems.size} symbol items")
            }
        } catch (e: Exception) {
            Log.e(TAG, "TreeSitter symbol extraction failed", e)
        }
        
        // 2. 总是提供关键字和STL补全
        items.addAll(keywordProvider.getKeywordCompletions(prefix, "cpp"))
        items.addAll(stlProvider.getSTLCompletions(prefix, "cpp"))
        
        return items
    }
    
    override fun provideMemberCompletions(
        contentRef: ContentReference,
        position: CharPosition,
        contextVar: String,
        prefix: String
    ): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        
        try {
            Log.d(TAG, "Providing member completions for contextVar: '$contextVar', prefix: '$prefix'")
            
            // 使用抽象类方法查找上下文变量
            val allSymbols = getScopeSymbols(contentRef, position)
            val localVariables = allSymbols.filter {
                it.type == SymbolType.VARIABLE || it.type == SymbolType.PARAMETER
            }
            
            // 查找上下文变量的类型信息
            val contextSymbol = localVariables.find { it.name == contextVar }
            if (contextSymbol != null) {
                Log.d(TAG, "Found context variable: ${contextSymbol.name} of type ${contextSymbol.dataType}")
                
                // 1. 优先检查是否为STL容器类型
                if (isStandardLibraryType(contextSymbol.dataType)) {
                    Log.d(TAG, "Type ${contextSymbol.dataType} is STL container")
                    items.addAll(stlProvider.getMemberCompletions(contextSymbol.dataType, prefix))
                    return items
                }
                
                // 2. 检查是否为基本类型（基本类型没有成员）
                if (isPrimitiveType(contextSymbol.dataType)) {
                    Log.d(TAG, "Type ${contextSymbol.dataType} is primitive type - no members")
                    return items // 返回空列表
                }
                
                // 3. 尝试获取用户定义的struct/class成员
                val cleanTypeName = cleanTypeName(contextSymbol.dataType)
                Log.d(TAG, "Clean type name: '$cleanTypeName' (from '${contextSymbol.dataType}')")
                
                val structMembers = getStructMembers(contentRef, cleanTypeName, prefix)
                if (structMembers.isNotEmpty()) {
                    Log.d(TAG, "Added ${structMembers.size} struct/class members for type $cleanTypeName")
                    items.addAll(structMembers)
                    return items
                }
                
                // 4. 如果没有找到struct成员，可能是未识别的类型，提供通用STL方法
                Log.d(TAG, "Type ${contextSymbol.dataType} not recognized, trying STL fallback")
                items.addAll(stlProvider.getMemberCompletions(contextSymbol.dataType, prefix))
                
            } else {
                Log.d(TAG, "Context variable '$contextVar' not found in TreeSitter results")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "TreeSitter member completion failed", e)
        }
        
        return items
    }
    
    override fun getKeywords(): Set<String> {
        return setOf(
            // C++ 关键字
            "auto", "break", "case", "char", "const", "continue", "default", "do",
            "double", "else", "enum", "extern", "float", "for", "goto", "if",
            "int", "long", "register", "return", "short", "signed", "sizeof", "static",
            "struct", "switch", "typedef", "union", "unsigned", "void", "volatile", "while",
            // C++11/14/17/20 关键字
            "alignas", "alignof", "and", "and_eq", "asm", "bitand", "bitor", "bool",
            "catch", "class", "compl", "const_cast", "constexpr", "decltype", "delete",
            "dynamic_cast", "explicit", "export", "false", "friend", "inline", "mutable",
            "namespace", "new", "noexcept", "not", "not_eq", "nullptr", "operator", "or",
            "or_eq", "private", "protected", "public", "reinterpret_cast", "static_assert",
            "static_cast", "template", "this", "thread_local", "throw", "true", "try",
            "typeid", "typename", "using", "virtual", "wchar_t", "xor", "xor_eq",
            // ACM常用
            "main", "include", "define", "pragma", "once", "iostream", "algorithm",
            "vector", "string", "map", "set", "queue", "stack", "priority_queue"
        )
    }
    
    override fun getStandardLibraryItems(): Map<String, List<String>> {
        return mapOf(
            "vector" to listOf("push_back", "pop_back", "size", "empty", "clear", "begin", "end", "front", "back", "at", "resize", "reserve"),
            "string" to listOf("length", "size", "empty", "clear", "substr", "find", "replace", "append", "push_back", "pop_back", "c_str"),
            "map" to listOf("insert", "erase", "find", "count", "size", "empty", "clear", "begin", "end", "lower_bound", "upper_bound"),
            "set" to listOf("insert", "erase", "find", "count", "size", "empty", "clear", "begin", "end", "lower_bound", "upper_bound"),
            "queue" to listOf("push", "pop", "front", "back", "size", "empty"),
            "stack" to listOf("push", "pop", "top", "size", "empty"),
            "priority_queue" to listOf("push", "pop", "top", "size", "empty"),
            "deque" to listOf("push_back", "pop_back", "push_front", "pop_front", "size", "empty", "clear", "begin", "end", "front", "back"),
            "list" to listOf("push_back", "pop_back", "push_front", "pop_front", "size", "empty", "clear", "begin", "end", "front", "back"),
            "pair" to listOf("first", "second", "make_pair")
        )
    }
    
    override fun isPrimitiveType(dataType: String): Boolean {
        val primitiveTypes = setOf(
            "int", "long", "short", "char", "byte",
            "float", "double", "bool", "boolean",
            "void", "auto", "size_t", "uint32_t", "int64_t"
        )
        return primitiveTypes.contains(dataType.lowercase())
    }
    
    override fun isStandardLibraryType(dataType: String): Boolean {
        val stlTypes = setOf(
            "vector", "string", "map", "set", "unordered_map", "unordered_set",
            "queue", "priority_queue", "stack", "deque", "list", "array",
            "multiset", "multimap", "pair"
        )
        return stlTypes.contains(dataType.lowercase())
    }
    
    // isAvailable() 现在由 AbstractTreeSitterProcessor 提供
    
    /**
     * 清理类型名：移除指针标记、引用标记等修饰符
     */
    private fun cleanTypeName(dataType: String): String {
        return dataType
            .replace("*", "")
            .replace("&", "")
            .replace("const", "")
            .replace("class", "")
            .replace("struct", "")
            .trim()
    }
    
    /**
     * 获取struct/class成员补全
     */
    private fun getStructMembers(
        contentRef: ContentReference,
        structTypeName: String,
        prefix: String
    ): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        
        try {
            // 获取所有symbols
            val result = treeSitterService.parseCode(contentRef, "cpp")
            val allSymbols = result?.symbols ?: emptyList()
            
            // 查找指定struct的成员
            val structMembers = allSymbols.filter { symbol ->
                symbol.type == SymbolType.STRUCT_MEMBER && 
                symbol.parentStruct == structTypeName &&
                symbol.name.startsWith(prefix, ignoreCase = true)
            }
            
            Log.d(TAG, "Found ${structMembers.size} members for struct '$structTypeName'")
            
            // 转换为补全项
            structMembers.forEach { member ->
                Log.d(TAG, "  Member: ${member.name} (${member.dataType})")
                
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
            Log.e(TAG, "Failed to get struct members", e)
        }
        
        return items
    }
    
    /**
     * 将符号转换为补全项
     */
    private fun convertSymbolsToCompletionItems(
        symbols: List<SymbolInfo>,
        prefix: String
    ): List<CompletionItem> {
        return symbols
            .filter { it.name.startsWith(prefix, ignoreCase = true) }
            .map { symbol ->
                val priority = calculateSymbolPriority(symbol)
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
     * 计算符号优先级
     */
    private fun calculateSymbolPriority(symbol: SymbolInfo): Int {
        return when {
            symbol.scopeLevel == 0 -> CompletionConstants.PRIORITY_GLOBAL_VARIABLE
            symbol.type == SymbolType.FUNCTION -> CompletionConstants.PRIORITY_LOCAL_FUNCTION
            symbol.type == SymbolType.PARAMETER -> CompletionConstants.PRIORITY_PARAMETER
            else -> CompletionConstants.PRIORITY_LOCAL_VARIABLE
        }
    }
}