package com.acc_ide.completion.services

import android.util.Log
import io.github.rosemoe.sora.text.ContentReference
import com.acc_ide.completion.core.*

/**
 * Tree-sitter Service - 官方设计理念实现
 * 
 * 核心原则：
 * 1. TSParser解析源码为AST
 * 2. 手动遍历TSNode提取符号
 * 3. 应用层处理语义分析
 */
class TreeSitterService {
    
    companion object {
        private const val TAG = "TreeSitterService"
        private var isLoaded = false
        
        init {
            try {
                System.loadLibrary("treesitter-jni")
                isLoaded = true
                Log.d(TAG, "Tree-sitter loaded")
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Tree-sitter load failed", e)
            }
        }
    }
    
    fun parseCode(contentRef: ContentReference, language: String): ParseResult? {
        if (!isLoaded) return null
        
        return try {
            val code = contentRef.reference.toString()
            when (language.lowercase()) {
                "cpp", "c++" -> parseCppCode(code)
                "java" -> parseJavaCode(code)
                "python", "py" -> parsePythonCode(code)
                else -> null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Parse failed", e)
            null
        }
    }
    
    fun getSymbolsAtPosition(contentRef: ContentReference, language: String, line: Int, column: Int): List<SymbolInfo> {
        val result = parseCode(contentRef, language) ?: return emptyList()
        
        Log.d(TAG, "getSymbolsAtPosition at $line:$column")
        
        // 确定当前位置的作用域上下文
        val currentScopes = result.scopes.filter { scope ->
            line >= scope.startLine && line <= scope.endLine
        }.sortedByDescending { it.level }
        
        Log.d(TAG, "Current position is in ${currentScopes.size} scopes: ${currentScopes.map { "${it.type}-${it.level}" }}")
        
        // 找到当前所在的类作用域（如果有）
        val currentClassScope = currentScopes.find { it.type == ScopeType.SCOPE_CLASS }
        val currentFunctionScope = currentScopes.find { it.type == ScopeType.SCOPE_FUNCTION }
        val currentBlockScope = currentScopes.find { it.type == ScopeType.SCOPE_BLOCK }
        
        return result.symbols.filter { symbol ->
            // 1. 基本时间可见性：符号必须在当前位置之前声明
            val isTemporallyVisible = symbol.line < line || (symbol.line == line && symbol.column < column)
            if (!isTemporallyVisible) {
                Log.d(TAG, "Symbol ${symbol.name} not temporally visible (declared at ${symbol.line}:${symbol.column})")
                return@filter false
            }
            
            // 2. 作用域可见性检查
            when (symbol.type) {
                SymbolType.STRUCT_MEMBER -> {
                    // struct/class成员：检查是否在同一类的方法内部
                    val currentFunctionScope = currentScopes.find { it.type == ScopeType.SCOPE_FUNCTION }
                    val currentClassScope = currentScopes.find { it.type == ScopeType.SCOPE_CLASS }
                    
                    val isAccessible = if (currentFunctionScope != null && currentClassScope != null) {
                        // 在类方法内部：检查成员是否属于当前类
                        val memberBelongsToCurrentClass = symbol.parentStruct.isNotEmpty() && 
                            result.symbols.any { classSymbol ->
                                classSymbol.type == SymbolType.CLASS &&
                                classSymbol.name == symbol.parentStruct &&
                                classSymbol.line >= currentClassScope.startLine &&
                                classSymbol.line <= currentClassScope.endLine
                            }
                        
                        if (memberBelongsToCurrentClass) {
                            Log.d(TAG, "Class member ${symbol.name} is accessible within class method")
                            true
                        } else {
                            Log.d(TAG, "Filtering out struct member ${symbol.name} - not in current class")
                            false
                        }
                    } else {
                        // 不在类方法内部：struct/class成员不可直接访问
                        Log.d(TAG, "Filtering out struct member ${symbol.name} from regular completion")
                        false
                    }
                    isAccessible
                }
                
                SymbolType.VARIABLE, SymbolType.PARAMETER -> {
                    // 变量和参数：检查作用域嵌套
                    val isInScope = when {
                        // 全局变量（作用域级别0）总是可见
                        symbol.scopeLevel == 0 -> {
                            Log.d(TAG, "Global variable ${symbol.name} is visible")
                            true
                        }
                        
                        // 局部变量：检查是否在当前位置可访问的作用域中
                        symbol.scopeLevel > 0 -> {
                            // 获取当前位置的函数作用域和类作用域
                            val currentFunctionScope = currentScopes.find { it.type == ScopeType.SCOPE_FUNCTION }
                            val currentClassScope = currentScopes.find { it.type == ScopeType.SCOPE_CLASS }
                            
                            val isVisible = if (currentFunctionScope != null) {
                                // 在函数内：检查符号是否在同一函数内
                                val inSameFunction = symbol.line >= currentFunctionScope.startLine && symbol.line <= currentFunctionScope.endLine
                                
                                if (inSameFunction) {
                                    // 在同一函数内：函数内声明的变量在声明后都可见
                                    true
                                } else {
                                    // 不在同一函数内：不可见
                                    false
                                }
                            } else if (currentClassScope != null) {
                                // 在类内但不在函数内（一般不会发生，但保险起见）
                                false
                            } else {
                                // 既不在函数内也不在类内：只能访问全局变量
                                symbol.scopeLevel == 0
                            }
                            
                            Log.d(TAG, "Local symbol ${symbol.name} (scope ${symbol.scopeLevel}, line ${symbol.line}) in function scope ${currentFunctionScope?.startLine}-${currentFunctionScope?.endLine}: $isVisible")
                            isVisible
                        }
                        
                        else -> false
                    }
                    isInScope
                }
                
                SymbolType.FUNCTION, SymbolType.CLASS, SymbolType.STRUCT, SymbolType.ENUM -> {
                    // 类型和函数：通常在全局作用域或当前类作用域可见
                    val isVisible = symbol.scopeLevel == 0 || 
                                  (currentClassScope != null && symbol.scopeLevel <= (currentClassScope.level + 1))
                    Log.d(TAG, "Type/Function ${symbol.name} (scope ${symbol.scopeLevel}) visible: $isVisible")
                    isVisible
                }
            }
        }
    }
    
    
    
    
    fun isAvailable(): Boolean = isLoaded
    
    // 原生方法 - 直接使用Tree-sitter核心API
    private external fun parseCppCode(code: String): ParseResult?
    private external fun parseJavaCode(code: String): ParseResult?
    private external fun parsePythonCode(code: String): ParseResult?
}