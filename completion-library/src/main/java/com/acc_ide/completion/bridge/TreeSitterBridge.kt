package com.acc_ide.completion.bridge

import com.acc_ide.treesitter.TreeSitterService
import com.acc_ide.treesitter.core.*
import io.github.rosemoe.sora.text.ContentReference

/**
 * Bridge between treesitter-core and completion-library
 * 
 * Adapts String-based TreeSitterService to ContentReference-based Sora Editor API
 */
class TreeSitterBridge {
    
    private val treeSitterService = TreeSitterService()
    
    /**
     * Parse code from ContentReference
     */
    fun parseCode(contentRef: ContentReference, language: String): ParseResult? {
        val code = contentRef.reference.toString()
        return treeSitterService.parseCode(code, language)
    }
    
    /**
     * Get symbols at position from ContentReference
     */
    fun getSymbolsAtPosition(
        contentRef: ContentReference,
        language: String,
        line: Int,
        column: Int
    ): List<SymbolInfo> {
        val code = contentRef.reference.toString()
        return treeSitterService.getSymbolsAtPosition(code, language, line, column)
    }
    
    /**
     * Check if Tree-sitter service is available
     */
    fun isAvailable(): Boolean = treeSitterService.isAvailable()
}

