package com.acc_ide.completion.core

/**
 * Completion Models - Type Aliases and Extensions
 * 
 * This file re-exports types from treesitter-core and provides
 * backward compatibility for existing completion code.
 */

// Re-export core types from treesitter-core
typealias SymbolInfo = com.acc_ide.treesitter.core.SymbolInfo
typealias SymbolType = com.acc_ide.treesitter.core.SymbolType
typealias ScopeInfo = com.acc_ide.treesitter.core.ScopeInfo
typealias ScopeType = com.acc_ide.treesitter.core.ScopeType
typealias ParseResult = com.acc_ide.treesitter.core.ParseResult

/**
 * Completion priority constants
 * Unified management of all completion-related constants to avoid circular dependencies
 */
object CompletionConstants {
    // Completion priority constants (higher number = higher priority)
    const val PRIORITY_LOCAL_VARIABLE = 200
    const val PRIORITY_LOCAL_FUNCTION = 190
    const val PRIORITY_PARAMETER = 185
    const val PRIORITY_GLOBAL_VARIABLE = 180
    const val PRIORITY_STRUCT_MEMBER = 170
    
    // STL and competitive programming related
    const val PRIORITY_STL_CONTAINER = 160      // STL container types
    const val PRIORITY_STL_ALGORITHM = 150      // STL algorithm functions
    const val PRIORITY_STL_COMMON = 140         // Common STL functions
    const val PRIORITY_STL_FUNCTION = 130       // Other STL functions
    
    // General programming elements
    const val PRIORITY_FUNCTION = 120           // General functions
    const val PRIORITY_KEYWORD = 110            // C++ keywords
    const val PRIORITY_CONSTANT = 100           // Constants and macros
    const val PRIORITY_TYPE = 90                // Type names
}

