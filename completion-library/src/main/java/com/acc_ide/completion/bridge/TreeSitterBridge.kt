package com.acc_ide.completion.bridge

import com.acc_ide.treesitter.TreeSitterService
import com.acc_ide.treesitter.core.*
import io.github.rosemoe.sora.text.ContentReference

/**
 * Adapts String-based TreeSitterService to ContentReference-based Sora Editor API
 */
class TreeSitterBridge {
    
    private val treeSitterService = TreeSitterService()
    
    fun parseCode(contentRef: ContentReference, language: String): ParseResult? {
        val code = contentRef.reference.toString()
        return treeSitterService.parseCode(code, language)
    }
    
    fun getSymbolsAtPosition(
        contentRef: ContentReference,
        language: String,
        line: Int,
        column: Int
    ): List<SymbolInfo> {
        val code = contentRef.reference.toString()
        return treeSitterService.getSymbolsAtPosition(code, language, line, column)
    }
    
    fun isAvailable(): Boolean = treeSitterService.isAvailable()
}

