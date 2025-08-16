package com.acc_ide.completion

import android.os.Bundle
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.SymbolPairMatch

/**
 * Hybrid language implementation that combines TextMate syntax highlighting
 * with ACM-focused intelligent completion
 */
class HybridACMLanguage(
    private val textMateLanguage: Language,
    private val languageType: String
) : EmptyLanguage() {
    
    private val completionProvider = ACMCompletionProvider()
    private val acmLanguage = ACMLanguage(languageType)
    
    override fun getAnalyzeManager(): AnalyzeManager {
        // Use TextMate's analyze manager for syntax highlighting
        return textMateLanguage.analyzeManager
    }
    
    override fun getInterruptionLevel(): Int {
        return Language.INTERRUPTION_LEVEL_SLIGHT
    }
    
    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        // Use ACM completion provider for intelligent suggestions
        val prefix = extractPrefix(content, position)
        
        if (prefix.isNotEmpty()) {
            completionProvider.provideCompletions(
                content, position, prefix, languageType, publisher
            )
        }
        
        // Removed TextMate completion to disable identifier auto-completion
        // The ACM completion provider handles all the necessary completions
    }
    
    override fun getFormatter(): Formatter {
        // Prefer TextMate formatter if available
        return textMateLanguage.formatter ?: super.getFormatter()
    }
    
    override fun getSymbolPairs(): SymbolPairMatch {
        // Use ACM language's symbol pairs as they're more complete for competitive programming
        return acmLanguage.symbolPairs
    }
    
    override fun getNewlineHandlers(): Array<NewlineHandler>? {
        // Use TextMate's newline handlers for proper indentation
        return textMateLanguage.newlineHandlers
    }
    
    override fun getIndentAdvance(content: ContentReference, line: Int, column: Int): Int {
        // Delegate to TextMate language for indentation
        return textMateLanguage.getIndentAdvance(content, line, column)
    }
    
    /**
     * Extract the word prefix at the current cursor position for completion
     */
    private fun extractPrefix(content: ContentReference, position: CharPosition): String {
        return try {
            val text = content.reference
            val line = text.getLine(position.line)
            val lineText = line.toString()
            
            if (position.column == 0) return ""
            
            var start = position.column - 1
            while (start >= 0 && isIdentifierChar(lineText[start])) {
                start--
            }
            start++
            
            lineText.substring(start, position.column)
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Check if character is valid for identifiers
     */
    private fun isIdentifierChar(c: Char): Boolean {
        return c.isLetterOrDigit() || c == '_'
    }
}