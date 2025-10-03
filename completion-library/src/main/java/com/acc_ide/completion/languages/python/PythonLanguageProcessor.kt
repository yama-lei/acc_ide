package com.acc_ide.completion.languages.python

import android.util.Log
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import com.acc_ide.completion.core.*
import com.acc_ide.completion.framework.AbstractTreeSitterProcessor
import com.acc_ide.completion.framework.UniversalCompletionEngine.PriorityCompletionItem
import com.acc_ide.completion.providers.python.PythonStaticLibrary

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
    
    private val pythonStaticLibrary = PythonStaticLibrary()
    
    
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
        
        try {
            Log.d(TAG, "Providing Python regular completions for prefix: '$prefix'")
            
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
        
        // 2. 总是提供Python静态库补全
        items.addAll(pythonStaticLibrary.getAllCompletions(prefix))
        
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
            Log.d(TAG, "Providing Python member completions for contextVar: '$contextVar', prefix: '$prefix'")
            
            // 使用抽象类方法查找上下文变量
            val allSymbols = getScopeSymbols(contentRef, position)
            val localVariables = allSymbols.filter {
                it.type == SymbolType.VARIABLE || it.type == SymbolType.PARAMETER
            }
            
            // 查找上下文变量的类型信息
            val contextSymbol = localVariables.find { it.name == contextVar }
            if (contextSymbol != null) {
                Log.d(TAG, "Found context variable: ${contextSymbol.name} of type ${contextSymbol.dataType}")
                
                // 1. 优先检查是否为Python内置类型
                if (pythonStaticLibrary.isBuiltinType(contextSymbol.dataType)) {
                    Log.d(TAG, "Type ${contextSymbol.dataType} is Python builtin type")
                    items.addAll(pythonStaticLibrary.getBuiltinMemberCompletions(contextSymbol.dataType, prefix))
                    return items
                }
                
                // 2. 检查是否为Python模块
                if (pythonStaticLibrary.isModule(contextSymbol.dataType)) {
                    Log.d(TAG, "Type ${contextSymbol.dataType} is Python module")
                    items.addAll(pythonStaticLibrary.getModuleMemberCompletions(contextSymbol.dataType, prefix))
                    return items
                }
                
                // 3. 尝试获取用户定义的类成员
                val cleanTypeName = cleanTypeName(contextSymbol.dataType)
                Log.d(TAG, "Clean type name: '$cleanTypeName' (from '${contextSymbol.dataType}')") 
                
                val classMembers = getClassMembers(contentRef, cleanTypeName, prefix)
                if (classMembers.isNotEmpty()) {
                    Log.d(TAG, "Added ${classMembers.size} class members for type $cleanTypeName")
                    items.addAll(classMembers)
                    return items
                }
                
                // 4. 如果没有找到类成员，可能是未识别的类型，提供通用Python方法
                Log.d(TAG, "Type ${contextSymbol.dataType} not recognized, trying Python fallback")
                items.addAll(pythonStaticLibrary.getBuiltinMemberCompletions(contextSymbol.dataType, prefix))
                
            } else {
                Log.d(TAG, "Context variable '$contextVar' not found in TreeSitter results")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "TreeSitter member completion failed", e)
        }
        
        return items
    }
    
    override fun getKeywords(): Set<String> {
        // 从Python静态库获取关键字
        val keywords = mutableSetOf<String>()
        val keywordCompletions = pythonStaticLibrary.getKeywordCompletions("")
        keywordCompletions.forEach { item ->
            keywords.add(item.label.toString())
        }
        return keywords
    }
    
    override fun getStandardLibraryItems(): Map<String, List<String>> {
        // 从Python静态库生成标准库映射
        val pythonItems = mutableMapOf<String, List<String>>()
        
        // 获取所有内置类型的成员方法
        listOf("str", "list", "dict", "set", "tuple").forEach { builtinType ->
            val members = pythonStaticLibrary.getBuiltinMemberCompletions(builtinType, "")
            if (members.isNotEmpty()) {
                pythonItems[builtinType] = members.map { it.label.toString() }
            }
        }
        
        return pythonItems
    }
    
    override fun isPrimitiveType(dataType: String): Boolean {
        val primitiveTypes = setOf(
            "int", "float", "str", "bool", "bytes", "complex", "none"
        )
        return primitiveTypes.contains(dataType.lowercase())
    }
    
    override fun isStandardLibraryType(dataType: String): Boolean {
        return pythonStaticLibrary.isBuiltinType(dataType) || pythonStaticLibrary.isModule(dataType)
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
     * 清理类型名：移除装饰器等Python特定修饰符
     */
    private fun cleanTypeName(dataType: String): String {
        return dataType
            .replace("@", "")           // 移除装饰器标记
            // NOTE: DO NOT remove "__" - Python dunder methods (__init__, __str__, etc.) need them
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
            val result = treeSitterService.parseCode(contentRef, "python")
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
                    SymbolType.STRUCT -> CompletionItemKind.Class  // Python中class
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