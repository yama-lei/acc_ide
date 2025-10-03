package com.acc_ide.treesitter

import android.util.Log
import com.acc_ide.treesitter.core.*

/**
 * Tree-sitter Service - Pure CST Parsing
 * 
 * Core principles:
 * 1. TSParser parses source code to CST
 * 2. Manual traversal of TSNode to extract symbols
 * 3. No external dependencies - accepts String input
 * 
 * This service is framework-agnostic and can be used by any completion engine.
 */
class TreeSitterService {
    
    private val TAG = "TreeSitterService"
    private var isLoaded = false
    
    init {
        try {
            System.loadLibrary("treesitter-jni")
            isLoaded = true
            Log.d(TAG, "Tree-sitter library loaded successfully")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "Failed to load Tree-sitter library", e)
        }
    }
    
    /**
     * Parse source code and extract symbols
     * 
     * @param code Source code as string
     * @param language Language identifier ("cpp", "java", "python")
     * @return ParseResult with symbols and scopes, or null if parsing failed
     */
    fun parseCode(code: String, language: String): ParseResult? {
        if (!isLoaded) {
            Log.w(TAG, "Tree-sitter library not loaded")
            return null
        }
        
        return try {
            when (language.lowercase()) {
                "cpp", "c++" -> parseCppCode(code)
                "java" -> parseJavaCode(code)
                "python", "py" -> parsePythonCode(code)
                else -> {
                    Log.w(TAG, "Unsupported language: $language")
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse $language code", e)
            null
        }
    }
    
    /**
     * Get symbols visible at a specific position
     * 
     * @param code Source code as string
     * @param language Language identifier
     * @param line Line number (0-based)
     * @param column Column number (0-based)
     * @return List of visible symbols at the given position
     */
    fun getSymbolsAtPosition(code: String, language: String, line: Int, column: Int): List<SymbolInfo> {
        val result = parseCode(code, language) ?: return emptyList()
        
        Log.d(TAG, "Getting symbols at position $line:$column")
        
        // Find current scopes at the given position
        val currentScopes = result.scopes.filter { scope ->
            line >= scope.startLine && line <= scope.endLine
        }.sortedByDescending { it.level }
        
        Log.d(TAG, "Position is in ${currentScopes.size} scopes: ${currentScopes.map { "${it.type}-${it.level}" }}")
        
        // Find current class and function scopes
        val currentClassScope = currentScopes.find { it.type == ScopeType.SCOPE_CLASS }
        val currentFunctionScope = currentScopes.find { it.type == ScopeType.SCOPE_FUNCTION }
        
        // Filter symbols based on visibility rules
        return result.symbols.filter { symbol ->
            // 1. Temporal visibility: symbol must be declared before current position
            val isTemporallyVisible = symbol.line < line || (symbol.line == line && symbol.column < column)
            if (!isTemporallyVisible) {
                Log.d(TAG, "Symbol ${symbol.name} not temporally visible (declared at ${symbol.line}:${symbol.column})")
                return@filter false
            }
            
            // 2. Scope visibility check
            when (symbol.type) {
                SymbolType.STRUCT_MEMBER -> {
                    // Struct/class members: check if in same class method
                    val isAccessible = if (currentFunctionScope != null && currentClassScope != null) {
                        // Inside class method: check if member belongs to current class
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
                        // Not in class method: struct/class members not directly accessible
                        Log.d(TAG, "Filtering out struct member ${symbol.name} from regular completion")
                        false
                    }
                    isAccessible
                }
                
                SymbolType.VARIABLE, SymbolType.PARAMETER -> {
                    // Variables and parameters: check scope nesting
                    val isInScope = when {
                        // Global variables (scope level 0) always visible
                        symbol.scopeLevel == 0 -> {
                            Log.d(TAG, "Global variable ${symbol.name} is visible")
                            true
                        }
                        
                        // Local variables: check if in accessible scope
                        symbol.scopeLevel > 0 -> {
                            val isVisible = if (currentFunctionScope != null) {
                                // Inside function: check if symbol is in same function
                                val inSameFunction = symbol.line >= currentFunctionScope.startLine && 
                                                   symbol.line <= currentFunctionScope.endLine
                                
                                if (inSameFunction) {
                                    // In same function: variables declared in function are visible after declaration
                                    true
                                } else {
                                    // Not in same function: not visible
                                    false
                                }
                            } else if (currentClassScope != null) {
                                // In class but not in function (rare case)
                                false
                            } else {
                                // Neither in function nor class: only global variables accessible
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
                    // Types and functions: usually visible in global or current class scope
                    val isVisible = symbol.scopeLevel == 0 || 
                                  (currentClassScope != null && symbol.scopeLevel <= (currentClassScope.level + 1))
                    Log.d(TAG, "Type/Function ${symbol.name} (scope ${symbol.scopeLevel}) visible: $isVisible")
                    isVisible
                }
            }
        }
    }
    
    /**
     * Check if Tree-sitter service is available
     */
    fun isAvailable(): Boolean = isLoaded
    
    // Native methods - direct Tree-sitter core API
    private external fun parseCppCode(code: String): ParseResult?
    private external fun parseJavaCode(code: String): ParseResult?
    private external fun parsePythonCode(code: String): ParseResult?
}

