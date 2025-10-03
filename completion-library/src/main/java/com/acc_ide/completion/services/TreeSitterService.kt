package com.acc_ide.completion.services

import android.util.Log
import io.github.rosemoe.sora.text.ContentReference
import com.acc_ide.completion.core.*
import com.acc_ide.completion.bridge.TreeSitterBridge

/**
 * Tree-sitter Service Adapter for Completion Library
 * 
 * This service adapts the core TreeSitterService (String-based)
 * to work with Sora Editor's ContentReference API.
 * 
 * All actual parsing is delegated to treesitter-core library.
 */
class TreeSitterService {
    
    private val TAG = "CompletionTreeSitterService"
    private val bridge = TreeSitterBridge()
    
    fun parseCode(contentRef: ContentReference, language: String): ParseResult? {
        return bridge.parseCode(contentRef, language)
    }
    
    fun getSymbolsAtPosition(contentRef: ContentReference, language: String, line: Int, column: Int): List<SymbolInfo> {
        return try {
            bridge.getSymbolsAtPosition(contentRef, language, line, column)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get symbols at position", e)
            emptyList()
        }
    }
    
    fun isAvailable(): Boolean = bridge.isAvailable()
}

