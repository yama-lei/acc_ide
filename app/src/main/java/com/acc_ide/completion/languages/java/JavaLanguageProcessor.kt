package com.acc_ide.completion.languages.java

import android.util.Log
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import com.acc_ide.completion.core.*
import com.acc_ide.completion.framework.AbstractTreeSitterProcessor
import com.acc_ide.completion.framework.UniversalCompletionEngine.PriorityCompletionItem
import com.acc_ide.completion.providers.java.JavaStaticLibrary

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
    
    private val javaStaticLibrary = JavaStaticLibrary()
    
    
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
        
        try {
            Log.d(TAG, "Providing Java regular completions for prefix: '$prefix'")
            
            // 1. TreeSitter符号补全 - 使用抽象类方法
            val symbolsAtPosition = getLanguageSpecificSymbols(contentRef, position, prefix)
            
            if (symbolsAtPosition.isNotEmpty()) {
                Log.d(TAG, "Found ${symbolsAtPosition.size} symbols from TreeSitter")
                
                // 转换为补全项
                val symbolItems = convertSymbolsToCompletionItems(symbolsAtPosition, prefix)
                items.addAll(symbolItems)
                Log.d(TAG, "Converted ${symbolItems.size} symbol items")
            }
        } catch (e: Exception) {
            Log.e(TAG, "TreeSitter symbol extraction failed", e)
        }
        
        // 2. 总是提供Java静态库补全
        items.addAll(javaStaticLibrary.getAllCompletions(prefix))
        
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
            Log.d(TAG, "Providing Java member completions for contextVar: '$contextVar', prefix: '$prefix'")
            
            // 使用抽象类方法查找上下文变量
            val allSymbols = getScopeSymbols(contentRef, position)
            val localVariables = allSymbols.filter {
                it.type == SymbolType.VARIABLE || it.type == SymbolType.PARAMETER
            }
            
            // 查找上下文变量的类型信息
            val contextSymbol = localVariables.find { it.name == contextVar }
            if (contextSymbol != null) {
                Log.d(TAG, "Found context variable: ${contextSymbol.name} of type ${contextSymbol.dataType}")
                
                // 1. 优先检查是否为Java集合类型
                if (javaStaticLibrary.isJavaCollection(contextSymbol.dataType)) {
                    Log.d(TAG, "Type ${contextSymbol.dataType} is Java collection")
                    items.addAll(javaStaticLibrary.getCollectionMemberCompletions(contextSymbol.dataType, prefix))
                    return items
                }
                
                // 2. 检查是否为Java工具类
                if (javaStaticLibrary.isJavaUtilityClass(contextSymbol.dataType)) {
                    Log.d(TAG, "Type ${contextSymbol.dataType} is Java utility class")
                    items.addAll(javaStaticLibrary.getUtilityMemberCompletions(contextSymbol.dataType, prefix))
                    return items
                }
                
                // 3. 检查是否为基本类型（基本类型没有成员）
                if (isPrimitiveType(contextSymbol.dataType)) {
                    Log.d(TAG, "Type ${contextSymbol.dataType} is primitive type - no members")
                    return items // 返回空列表
                }
                
                // 4. 尝试获取用户定义的类成员
                val cleanTypeName = cleanTypeName(contextSymbol.dataType)
                Log.d(TAG, "Clean type name: '$cleanTypeName' (from '${contextSymbol.dataType}')") 
                
                val classMembers = getClassMembers(contentRef, cleanTypeName, prefix)
                if (classMembers.isNotEmpty()) {
                    Log.d(TAG, "Added ${classMembers.size} class members for type $cleanTypeName")
                    items.addAll(classMembers)
                    return items
                }
                
                // 5. 如果没有找到类成员，可能是未识别的类型，提供通用Java方法
                Log.d(TAG, "Type ${contextSymbol.dataType} not recognized, trying Java fallback")
                items.addAll(javaStaticLibrary.getCollectionMemberCompletions(contextSymbol.dataType, prefix))
                
            } else {
                Log.d(TAG, "Context variable '$contextVar' not found in TreeSitter results")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "TreeSitter member completion failed", e)
        }
        
        return items
    }
    
    override fun getKeywords(): Set<String> {
        // 从Java静态库获取关键字
        val keywords = mutableSetOf<String>()
        val keywordCompletions = javaStaticLibrary.getKeywordCompletions("")
        keywordCompletions.forEach { item ->
            keywords.add(item.label.toString())
        }
        return keywords
    }
    
    override fun getStandardLibraryItems(): Map<String, List<String>> {
        // 从Java静态库生成标准库映射
        val javaItems = mutableMapOf<String, List<String>>()
        
        // 获取所有集合类型的成员函数
        listOf(
            "String", "ArrayList", "LinkedList", "Vector", "Stack",
            "HashSet", "LinkedHashSet", "TreeSet",
            "HashMap", "LinkedHashMap", "TreeMap", "Hashtable",
            "PriorityQueue", "ArrayDeque", "StringBuilder", "StringBuffer"
        ).forEach { collection ->
            val members = javaStaticLibrary.getCollectionMemberCompletions(collection, "")
            if (members.isNotEmpty()) {
                javaItems[collection] = members.map { it.label.toString() }
            }
        }
        
        return javaItems
    }
    
    override fun isPrimitiveType(dataType: String): Boolean {
        val primitiveTypes = setOf(
            "boolean", "byte", "char", "short", "int", "long", "float", "double", "void"
        )
        return primitiveTypes.contains(dataType.lowercase())
    }
    
    override fun isStandardLibraryType(dataType: String): Boolean {
        return javaStaticLibrary.isJavaCollection(dataType) || javaStaticLibrary.isJavaUtilityClass(dataType)
    }
    
    // isAvailable() 现在由 AbstractTreeSitterProcessor 提供
    
    /**
     * 清理类型名：移除泛型参数、修饰符等
     */
    private fun cleanTypeName(dataType: String): String {
        return dataType
            .replace(Regex("<.*>"), "") // 移除泛型参数
            .replace("final", "")        // 移除final修饰符
            .replace("static", "")       // 移除static修饰符
            .replace("public", "")       // 移除访问修饰符
            .replace("private", "")
            .replace("protected", "")
            .trim()
    }
    
    /**
     * 获取类成员补全
     */
    private fun getClassMembers(
        contentRef: ContentReference,
        classTypeName: String,
        prefix: String
    ): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        
        try {
            // 获取所有symbols
            val result = treeSitterService.parseCode(contentRef, "java")
            val allSymbols = result?.symbols ?: emptyList()
            
            // 查找指定class的成员
            val classMembers = allSymbols.filter { symbol ->
                symbol.type == SymbolType.STRUCT_MEMBER && 
                symbol.parentStruct == classTypeName &&
                symbol.name.startsWith(prefix, ignoreCase = true)
            }
            
            Log.d(TAG, "Found ${classMembers.size} members for class '$classTypeName'")
            
            // 转换为补全项
            classMembers.forEach { member ->
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
            Log.e(TAG, "Failed to get class members", e)
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
                    SymbolType.STRUCT -> CompletionItemKind.Class  // Java中struct相当于class
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