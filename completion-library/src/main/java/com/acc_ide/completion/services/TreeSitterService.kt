package com.acc_ide.completion.services

import android.util.Log
import io.github.rosemoe.sora.text.ContentReference
import com.acc_ide.completion.core.*
import com.acc_ide.completion.bridge.TreeSitterBridge

/**
 * Adapter that wraps treesitter-core for ContentReference-based API
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

