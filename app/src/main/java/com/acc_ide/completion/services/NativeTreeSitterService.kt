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
        
        // 临时禁用缓存以测试新的解析逻辑
        // parseCache[cacheKey]?.let { 
        //     Log.d(TAG, "Returning cached result for $language")
        //     return it 
        // }
        
        Log.d(TAG, "Forcing fresh parse (cache disabled for testing)")
        
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
                                Log.d(TAG, "Native result is null, using empty fallback")
                                createEmptyParseResult()
                            }
                        } catch (e: UnsatisfiedLinkError) {
                            Log.w(TAG, "Native TreeSitter not available, using empty fallback", e)
                            createEmptyParseResult()
                        } catch (e: Exception) {
                            Log.e(TAG, "TreeSitter parsing failed, using empty fallback", e)
                            createEmptyParseResult()
                        }
                    } else {
                        Log.d(TAG, "JNI not loaded, using empty fallback")
                        createEmptyParseResult()
                    }
                }
                "java" -> {
                    if (isJniLoaded) {
                        try {
                            Log.d(TAG, "Attempting TreeSitter parsing for Java")
                            val nativeResult = parseJavaCode(text)
                            Log.d(TAG, "TreeSitter parsing successful: ${nativeResult != null}")
                            nativeResult ?: createEmptyParseResult()
                        } catch (e: Exception) {
                            Log.e(TAG, "TreeSitter parsing failed, using fallback", e)
                            createEmptyParseResult()
                        }
                    } else {
                        createEmptyParseResult()
                    }
                }
                "python", "py" -> {
                    if (isJniLoaded) {
                        try {
                            Log.d(TAG, "Attempting TreeSitter parsing for Python")
                            val nativeResult = parsePythonCode(text)
                            Log.d(TAG, "TreeSitter parsing successful: ${nativeResult != null}")
                            nativeResult ?: createEmptyParseResult()
                        } catch (e: Exception) {
                            Log.e(TAG, "TreeSitter parsing failed, using fallback", e)
                            createEmptyParseResult()
                        }
                    } else {
                        createEmptyParseResult()
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
        
        Log.d(TAG, "getSymbolsAtPosition($line:$column) - analyzing scope visibility")
        
        // 获取当前位置所在的作用域
        val currentScope = getCurrentScope(result.scopes, line, column)
        Log.d(TAG, "Current position is in scope level: ${currentScope?.level ?: "global"}")
        
        // 过滤符号：只包含在当前作用域可见的符号
        val visibleSymbols = result.symbols.filter { symbol ->
            isSymbolVisibleAtPosition(symbol, line, column, result.scopes)
        }
        
        Log.d(TAG, "getSymbolsAtPosition($line:$column) found ${visibleSymbols.size} visible symbols")
        visibleSymbols.forEach { symbol ->
            Log.d(TAG, "  Visible symbol: ${symbol.name} (${symbol.type}) at ${symbol.line}:${symbol.column}, scope: ${symbol.scopeLevel}")
        }
        
        return visibleSymbols
    }
    
    /**
     * 获取当前位置所在的作用域
     */
    private fun getCurrentScope(scopes: List<ScopeInfo>, line: Int, column: Int): ScopeInfo? {
        return scopes
            .filter { scope ->
                // 当前位置在作用域范围内
                line >= scope.startLine && line <= scope.endLine
            }
            .maxByOrNull { it.level } // 返回最深层的作用域
    }
    
    /**
     * 判断符号在指定位置是否可见
     */
    private fun isSymbolVisibleAtPosition(symbol: SymbolInfo, line: Int, column: Int, scopes: List<ScopeInfo>): Boolean {
        // 1. 符号必须在当前位置之前声明
        if (symbol.line > line || (symbol.line == line && symbol.column >= column)) {
            return false
        }
        
        // 2. 全局符号（函数、结构体、宏等）总是可见
        if (symbol.scopeLevel == 0) {
            return true
        }
        
        // 3. 局部变量需要检查作用域
        if (symbol.type == SymbolType.VARIABLE || symbol.type == SymbolType.PARAMETER) {
            return isInSameOrParentScope(symbol, line, column, scopes)
        }
        
        return true
    }
    
    /**
     * 判断变量是否在当前位置的作用域内或父作用域中
     */
    private fun isInSameOrParentScope(symbol: SymbolInfo, line: Int, column: Int, scopes: List<ScopeInfo>): Boolean {
        // 获取符号所在的作用域
        val symbolScope = scopes.find { scope ->
            symbol.scopeLevel == scope.level &&
            symbol.line >= scope.startLine && symbol.line <= scope.endLine
        }
        
        // 获取当前位置所在的作用域
        val currentScope = getCurrentScope(scopes, line, column)
        
        if (symbolScope == null) {
            Log.d(TAG, "Symbol ${symbol.name} scope not found, treating as global")
            return true
        }
        
        if (currentScope == null) {
            Log.d(TAG, "Current position scope not found, only global symbols visible")
            return symbol.scopeLevel == 0
        }
        
        // 检查当前位置是否在符号的作用域内或其子作用域内
        val isInSymbolScope = line >= symbolScope.startLine && line <= symbolScope.endLine
        
        Log.d(TAG, "Symbol ${symbol.name} scope: ${symbolScope.startLine}-${symbolScope.endLine}, current: $line, visible: $isInSymbolScope")
        
        return isInSymbolScope
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
     * 创建空的解析结果（当TreeSitter失败时的fallback）
     */
    private fun createEmptyParseResult(): ParseResult {
        Log.d(TAG, "Creating empty parse result as fallback")
        return ParseResult(emptyList(), emptyList())
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

