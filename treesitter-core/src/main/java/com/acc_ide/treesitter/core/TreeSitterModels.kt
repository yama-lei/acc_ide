package com.acc_ide.treesitter.core

/**
 * Tree-sitter Core Models
 * 
 * Pure data models for Tree-sitter parsing results.
 * No dependencies on external libraries.
 */

/**
 * Symbol extracted from Tree-sitter AST
 */
data class SymbolInfo(
    val name: String,
    val type: SymbolType,
    val dataType: String,
    val line: Int,
    val column: Int,
    val scopeLevel: Int,
    val description: String = "",
    val parentStruct: String = ""
)

/**
 * Symbol types
 */
enum class SymbolType {
    VARIABLE, 
    FUNCTION, 
    CLASS, 
    STRUCT, 
    ENUM, 
    PARAMETER, 
    STRUCT_MEMBER
}

/**
 * Scope information for visibility analysis
 */
data class ScopeInfo(
    val level: Int,
    val startLine: Int,
    val endLine: Int,
    val type: ScopeType
)

/**
 * Scope types
 */
enum class ScopeType {
    SCOPE_FUNCTION, 
    SCOPE_CLASS, 
    SCOPE_BLOCK, 
    SCOPE_NAMESPACE, 
    SCOPE_GLOBAL
}

/**
 * Parse result - Tree-sitter provides CST, we extract symbols
 */
data class ParseResult(
    val symbols: List<SymbolInfo>,
    val scopes: List<ScopeInfo>
)

