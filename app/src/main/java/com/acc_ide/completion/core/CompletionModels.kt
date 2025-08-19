package com.acc_ide.completion.core

/**
 * Tree-sitter completion models - following official minimalist design
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
    VARIABLE, FUNCTION, CLASS, STRUCT, ENUM, PARAMETER, STRUCT_MEMBER
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
    SCOPE_FUNCTION, SCOPE_CLASS, SCOPE_BLOCK, SCOPE_NAMESPACE, SCOPE_GLOBAL
}

/**
 * Simple parse result - Tree-sitter provides AST, we extract symbols
 */
data class ParseResult(
    val symbols: List<SymbolInfo>,
    val scopes: List<ScopeInfo>
)