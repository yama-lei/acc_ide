package com.acc_ide.completion.framework

import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import com.acc_ide.completion.core.SymbolInfo
import com.acc_ide.completion.core.ParseResult

/**
 * Language Processor Abstract Base Class
 * 
 * Each language inherits this class to implement specific parsing and completion logic
 */
abstract class LanguageProcessor {
    
    /**
     * Language identifier (e.g. "cpp", "java", "python")
     */
    abstract fun getLanguageId(): String
    
    /**
     * Language display name
     */
    abstract fun getLanguageName(): String
    
    /**
     * Supported file extensions
     */
    abstract fun getSupportedExtensions(): Set<String>
    
    /**
     * Parse code to get symbol information
     */
    abstract fun parseCode(code: String): ParseResult?
    
    /**
     * Get visible symbols at specific position
     */
    abstract fun getSymbolsAtPosition(
        contentRef: ContentReference,
        line: Int,
        column: Int
    ): List<SymbolInfo>
    
    /**
     * Provide regular completion suggestions
     */
    abstract fun provideRegularCompletions(
        contentRef: ContentReference,
        position: CharPosition,
        prefix: String
    ): List<CompletionItem>
    
    /**
     * Provide member access completions
     */
    abstract fun provideMemberCompletions(
        contentRef: ContentReference,
        position: CharPosition,
        contextVar: String,
        prefix: String
    ): List<CompletionItem>
    
    /**
     * Get language keywords
     */
    abstract fun getKeywords(): Set<String>
    
    /**
     * Get standard library types/functions
     */
    abstract fun getStandardLibraryItems(): Map<String, List<String>>
    
    /**
     * Check if a type is primitive
     */
    abstract fun isPrimitiveType(dataType: String): Boolean
    
    /**
     * Check if a type is from standard library
     */
    abstract fun isStandardLibraryType(dataType: String): Boolean
    
    /**
     * Extract member access context
     */
    open fun extractMemberAccessContext(
        contentRef: ContentReference,
        position: CharPosition,
        prefix: String
    ): Pair<String, String?> {
        return try {
            val line = contentRef.getLine(position.line)
            val beforeCursor = line.substring(0, position.column)
            
            // Check for dot operator (.)
            val lastDotIndex = beforeCursor.lastIndexOf('.')
            // Check for arrow operator (->)
            val lastArrowIndex = beforeCursor.lastIndexOf("->")
            
            // Select the operator closest to cursor
            val (operatorIndex, operatorLength) = when {
                lastDotIndex > lastArrowIndex -> Pair(lastDotIndex, 1)
                lastArrowIndex >= 0 -> Pair(lastArrowIndex, 2)
                else -> Pair(-1, 0)
            }
            
            if (operatorIndex > 0) {
                val beforeOperator = beforeCursor.substring(0, operatorIndex).trim()
                val afterOperator = beforeCursor.substring(operatorIndex + operatorLength)
                
                // Extract variable name (simplified)
                val words = beforeOperator.split(Regex("\\W+"))
                val varName = words.lastOrNull { it.isNotEmpty() && it[0].isLetter() }
                
                if (varName != null) {
                    return Pair(afterOperator, varName)
                }
            }
            
            Pair(prefix, null)
        } catch (e: Exception) {
            Pair(prefix, null)
        }
    }
    
    /**
     * Extract prefix
     */
    open fun extractPrefix(contentRef: ContentReference, position: CharPosition): String {
        return try {
            val line = contentRef.getLine(position.line)
            val beforeCursor = line.substring(0, minOf(position.column, line.length))
            
            // Extract current word as prefix
            val match = Regex("""[a-zA-Z_][a-zA-Z0-9_]*$""").find(beforeCursor)
            match?.value ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Check if processor is available
     */
    open fun isAvailable(): Boolean = true
}

