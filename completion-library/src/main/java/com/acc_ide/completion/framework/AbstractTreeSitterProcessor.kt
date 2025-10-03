package com.acc_ide.completion.framework

import android.util.Log
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.text.CharPosition
import com.acc_ide.completion.core.*
import com.acc_ide.completion.services.TreeSitterService

/**
 * Base implementation for TreeSitter-based language processors
 */
abstract class AbstractTreeSitterProcessor : LanguageProcessor() {
    
    private val TAG = "AbstractTreeSitterProcessor"
    
    protected val treeSitterService = TreeSitterService()
    
    protected fun createContentReference(code: String): ContentReference {
        return try {
            val content = Content(code)
            ContentReference(content)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create ContentReference", e)
            ContentReference(Content(""))
        }
    }
    
    override fun parseCode(code: String): ParseResult? {
        return try {
            Log.d(TAG, "Parsing ${getLanguageId()} code with TreeSitter")
            
            val contentRef = createContentReference(code)
            val result = treeSitterService.parseCode(contentRef, getLanguageId())
            
            if (result != null && result.symbols.isNotEmpty()) {
                result
            } else {
                Log.d(TAG, "No symbols found in ${getLanguageId()} code")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse ${getLanguageId()} code with TreeSitter", e)
            null
        }
    }
    
    override fun getSymbolsAtPosition(
        contentRef: ContentReference,
        line: Int,
        column: Int
    ): List<SymbolInfo> {
        return try {
            treeSitterService.getSymbolsAtPosition(contentRef, getLanguageId(), line, column)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get symbols at position for ${getLanguageId()}", e)
            emptyList()
        }
    }
    
    override fun isAvailable(): Boolean {
        return treeSitterService.isAvailable()
    }
    
    protected open fun getLanguageSpecificSymbols(
        contentRef: ContentReference,
        position: CharPosition,
        prefix: String
    ): List<SymbolInfo> {
        return try {
            treeSitterService.getSymbolsAtPosition(
                contentRef, 
                getLanguageId(), 
                position.line, 
                position.column
            ).filter { symbol ->
                symbol.name.startsWith(prefix, ignoreCase = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get language-specific symbols for ${getLanguageId()}", e)
            emptyList()
        }
    }
    
    /**
     * Get symbols in current scope
     * 
     * Uses TreeSitter's scope analysis to get visible symbols
     */
    protected open fun getScopeSymbols(
        contentRef: ContentReference,
        position: CharPosition
    ): List<SymbolInfo> {
        return try {
            val symbols = treeSitterService.getSymbolsAtPosition(
                contentRef, 
                getLanguageId(), 
                position.line, 
                position.column
            )
            
            // TreeSitter service already handles scope filtering
            symbols
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get scope symbols for ${getLanguageId()}", e)
            emptyList()
        }
    }
}

