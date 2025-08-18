package com.acc_ide.completion.services

import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentReference
import android.util.Log
import com.acc_ide.completion.core.*

/**
 * 原生TreeSitter服务
 * 直接使用构建的.so库进行语法分析和代码补全
 * 
 * 支持的语言：C++, Java, Python
 * 提供语法树解析、符号提取、作用域分析等功能
 */
class NativeTreeSitterService : TreeSitterInterface {
    
    companion object {
        private const val TAG = "NativeTreeSitterService"
        private var isJniLoaded = false
        
        // 加载原生库
        init {
            try {
                System.loadLibrary("treesitter-jni")
                isJniLoaded = true
                Log.d(TAG, "TreeSitter JNI library loaded successfully")
            } catch (e: UnsatisfiedLinkError) {
                isJniLoaded = false
                Log.e(TAG, "Failed to load TreeSitter JNI library", e)
            }
        }
        
        fun isJniAvailable(): Boolean = isJniLoaded
    }
    
    // 缓存解析结果
    private val parseCache = mutableMapOf<String, ParseResult>()
    
    /**
     * 解析代码并返回语法分析结果
     */
    override fun parseCode(contentRef: ContentReference, language: String): ParseResult? {
        val content = contentRef.reference
        val text = content.toString()
        val cacheKey = "${language}:${text.hashCode()}"
        
        Log.d(TAG, "parseCode called for language: $language, text length: ${text.length}")
        Log.d(TAG, "JNI loaded: $isJniLoaded")
        
        // 检查缓存
        parseCache[cacheKey]?.let { 
            Log.d(TAG, "Returning cached result for $language")
            return it 
        }
        
        return try {
            val result = when (language.lowercase()) {
                "cpp", "c++" -> {
                    if (isJniLoaded) {
                        try {
                            Log.d(TAG, "Attempting TreeSitter parsing for C++")
                            val nativeResult = parseCppCode(text)
                            Log.d(TAG, "TreeSitter native result: $nativeResult")
                            if (nativeResult != null) {
                                Log.d(TAG, "Native TreeSitter found ${nativeResult.symbols.size} symbols")
                                nativeResult.symbols.forEach { symbol ->
                                    Log.d(TAG, "  Native symbol: ${symbol.name} (${symbol.type}) at ${symbol.line}:${symbol.column}")
                                }
                                nativeResult
                            } else {
                                Log.d(TAG, "Native result is null, using mock parser")
                                createMockParseResult(text, "cpp")
                            }
                        } catch (e: UnsatisfiedLinkError) {
                            Log.w(TAG, "Native TreeSitter not available, using fallback", e)
                            createMockParseResult(text, "cpp")
                        } catch (e: Exception) {
                            Log.e(TAG, "TreeSitter parsing failed, using fallback", e)
                            createMockParseResult(text, "cpp")
                        }
                    } else {
                        Log.d(TAG, "JNI not loaded, using mock parser")
                        createMockParseResult(text, "cpp")
                    }
                }
                "java" -> {
                    if (isJniLoaded) {
                        try {
                            Log.d(TAG, "Attempting TreeSitter parsing for Java")
                            val nativeResult = parseJavaCode(text)
                            Log.d(TAG, "TreeSitter parsing successful: ${nativeResult != null}")
                            nativeResult ?: createMockParseResult(text, "java")
                        } catch (e: Exception) {
                            Log.e(TAG, "TreeSitter parsing failed, using fallback", e)
                            createMockParseResult(text, "java")
                        }
                    } else {
                        createMockParseResult(text, "java")
                    }
                }
                "python", "py" -> {
                    if (isJniLoaded) {
                        try {
                            Log.d(TAG, "Attempting TreeSitter parsing for Python")
                            val nativeResult = parsePythonCode(text)
                            Log.d(TAG, "TreeSitter parsing successful: ${nativeResult != null}")
                            nativeResult ?: createMockParseResult(text, "python")
                        } catch (e: Exception) {
                            Log.e(TAG, "TreeSitter parsing failed, using fallback", e)
                            createMockParseResult(text, "python")
                        }
                    } else {
                        createMockParseResult(text, "python")
                    }
                }
                else -> {
                    Log.w(TAG, "Unsupported language: $language")
                    null
                }
            }
            
            if (result != null) {
                parseCache[cacheKey] = result
                Log.d(TAG, "Found ${result.symbols.size} symbols and ${result.scopes.size} scopes")
            }
            
            result
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse $language code", e)
            null
        }
    }
    
    /**
     * 获取指定位置的符号信息
     */
    override fun getSymbolsAtPosition(contentRef: ContentReference, language: String, line: Int, column: Int): List<SymbolInfo> {
        val result = parseCode(contentRef, language) ?: return emptyList()
        
        // 简化版本：返回当前位置之前声明的所有符号
        val visibleSymbols = result.symbols.filter { symbol ->
            // 符号必须在当前位置之前声明
            symbol.line < line || (symbol.line == line && symbol.column < column)
        }
        
        Log.d(TAG, "getSymbolsAtPosition($line:$column) found ${visibleSymbols.size} visible symbols")
        visibleSymbols.forEach { symbol ->
            Log.d(TAG, "  Symbol: ${symbol.name} (${symbol.type}) at ${symbol.line}:${symbol.column}")
        }
        
        return visibleSymbols
    }
    
    /**
     * 获取局部变量
     */
    override fun getLocalVariables(contentRef: ContentReference, language: String, line: Int, column: Int): List<SymbolInfo> {
        return getSymbolsAtPosition(contentRef, language, line, column).filter {
            it.type == SymbolType.VARIABLE || it.type == SymbolType.PARAMETER
        }
    }
    
    /**
     * 获取函数定义
     */
    override fun getFunctionDefinitions(contentRef: ContentReference, language: String): List<SymbolInfo> {
        val result = parseCode(contentRef, language) ?: return emptyList()
        return result.symbols.filter { it.type == SymbolType.FUNCTION }
    }
    
    /**
     * 获取类定义
     */
    override fun getClassDefinitions(contentRef: ContentReference, language: String): List<SymbolInfo> {
        val result = parseCode(contentRef, language) ?: return emptyList()
        return result.symbols.filter { it.type == SymbolType.CLASS || it.type == SymbolType.STRUCT }
    }
    
    // JNI原生方法声明
    
    /**
     * 解析C++代码
     */
    private external fun parseCppCode(code: String): ParseResult?
    
    /**
     * 解析Java代码
     */
    private external fun parseJavaCode(code: String): ParseResult?
    
    /**
     * 解析Python代码
     */
    private external fun parsePythonCode(code: String): ParseResult?
    
    /**
     * 创建模拟解析结果 (改进的实现)
     */
    private fun createMockParseResult(code: String, language: String): ParseResult {
        val symbols = mutableListOf<SymbolInfo>()
        val scopes = mutableListOf<ScopeInfo>()
        
        Log.d(TAG, "createMockParseResult for $language, code length: ${code.length}")
        
        val lines = code.split('\n')
        var currentScopeLevel = 0
        
        lines.forEachIndexed { lineIndex, line ->
            val trimmedLine = line.trim()
            
            when (language) {
                "cpp" -> {
                    // 匹配函数定义: return_type function_name(...) {
                    Regex("""(\w+(?:\s*\*)?)\s+(\w+)\s*\([^)]*\)\s*\{""").find(trimmedLine)?.let { match ->
                        symbols.add(SymbolInfo(
                            name = match.groupValues[2],
                            type = SymbolType.FUNCTION,
                            dataType = match.groupValues[1].trim(),
                            line = lineIndex,
                            column = match.range.first,
                            scopeLevel = currentScopeLevel,
                            description = "Function"
                        ))
                        Log.d(TAG, "Found function: ${match.groupValues[2]}")
                    }
                    
                    
                    // 匹配for循环变量: for(int i = 0; ...)
                    Regex("""for\s*\(\s*(\w+)\s+(\w+)\s*=""").find(trimmedLine)?.let { match ->
                        symbols.add(SymbolInfo(
                            name = match.groupValues[2],
                            type = SymbolType.VARIABLE,
                            dataType = match.groupValues[1],
                            line = lineIndex,
                            column = match.range.first,
                            scopeLevel = currentScopeLevel + 1,
                            description = "Loop variable"
                        ))
                        Log.d(TAG, "Found loop variable: ${match.groupValues[2]}")
                    }
                    
                    // 匹配struct/class定义
                    Regex("""(struct|class)\s+(\w+)""").find(trimmedLine)?.let { match ->
                        symbols.add(SymbolInfo(
                            name = match.groupValues[2],
                            type = if (match.groupValues[1] == "struct") SymbolType.STRUCT else SymbolType.CLASS,
                            dataType = match.groupValues[1],
                            line = lineIndex,
                            column = match.range.first,
                            scopeLevel = currentScopeLevel,
                            description = "${match.groupValues[1]} definition"
                        ))
                        Log.d(TAG, "Found ${match.groupValues[1]}: ${match.groupValues[2]}")
                    }
                }
                
                "java" -> {
                    // 匹配类定义
                    Regex("""class\s+(\w+)""").find(trimmedLine)?.let { match ->
                        symbols.add(SymbolInfo(
                            name = match.groupValues[1],
                            type = SymbolType.CLASS,
                            dataType = "class",
                            line = lineIndex,
                            column = match.range.first,
                            scopeLevel = currentScopeLevel,
                            description = "Class"
                        ))
                        Log.d(TAG, "Found class: ${match.groupValues[1]}")
                    }
                    
                    // 匹配方法定义
                    Regex("""(public|private|protected)?\s*(\w+)\s+(\w+)\s*\([^)]*\)\s*\{""").find(trimmedLine)?.let { match ->
                        symbols.add(SymbolInfo(
                            name = match.groupValues[3],
                            type = SymbolType.FUNCTION,
                            dataType = match.groupValues[2],
                            line = lineIndex,
                            column = match.range.first,
                            scopeLevel = currentScopeLevel,
                            description = "Method"
                        ))
                        Log.d(TAG, "Found method: ${match.groupValues[3]}")
                    }
                    
                    // 匹配变量声明
                    Regex("""(int|String|boolean|double|float)\s+(\w+)""").find(trimmedLine)?.let { match ->
                        symbols.add(SymbolInfo(
                            name = match.groupValues[2],
                            type = SymbolType.VARIABLE,
                            dataType = match.groupValues[1],
                            line = lineIndex,
                            column = match.range.first,
                            scopeLevel = currentScopeLevel,
                            description = "Variable"
                        ))
                        Log.d(TAG, "Found variable: ${match.groupValues[2]}")
                    }
                }
            }
            
            // 跟踪作用域级别
            if (trimmedLine.contains('{')) {
                currentScopeLevel++
            }
            if (trimmedLine.contains('}')) {
                currentScopeLevel = maxOf(0, currentScopeLevel - 1)
            }
        }
        
        Log.d(TAG, "Mock parser found ${symbols.size} symbols")
        
        return ParseResult(symbols, scopes)
    }
    
    /**
     * 检查符号在指定位置是否可见
     */
    private fun isSymbolVisibleAtPosition(
        symbol: SymbolInfo, 
        scopes: List<ScopeInfo>, 
        line: Int, 
        column: Int
    ): Boolean {
        // 查找当前位置所在的作用域
        val currentScope = scopes.firstOrNull { scope ->
            line >= scope.startLine && line <= scope.endLine
        }
        
        return when {
            // 全局符号总是可见
            symbol.scopeLevel == 0 -> true
            
            // 如果没有作用域信息，默认可见
            currentScope == null -> true
            
            // 符号必须在当前作用域或父作用域中
            symbol.scopeLevel <= currentScope.level -> {
                // 进一步检查符号是否在作用域范围内
                val symbolScope = scopes.find { scope ->
                    symbol.line >= scope.startLine && symbol.line <= scope.endLine &&
                    scope.level == symbol.scopeLevel
                }
                
                symbolScope?.let { scope ->
                    line >= scope.startLine && line <= scope.endLine
                } ?: true
            }
            
            else -> false
        }
    }
    
    /**
     * 清空缓存
     */
    fun clearCache() {
        parseCache.clear()
    }
    
    // TreeSitterInterface接口实现
    
    override fun isAvailable(): Boolean {
        // TODO: 实际检查JNI库是否加载成功
        return true // 目前总是返回true，因为有fallback实现
    }
    
    override fun getSupportedLanguages(): List<String> {
        return listOf("cpp", "java", "python")
    }
}

