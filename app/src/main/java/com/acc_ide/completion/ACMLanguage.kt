package com.acc_ide.completion

import android.os.Bundle
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.completion.CompletionHelper
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.SymbolPairMatch

/**
 * Enhanced language implementation for ACM competitive programming
 * Extends EmptyLanguage with intelligent completion support
 */
class ACMLanguage(private val languageType: String) : EmptyLanguage() {
    
    private val completionProvider = ACMCompletionProvider()
    
    companion object {
        // Symbol pairs for different languages
        private val CPP_SYMBOL_PAIRS = SymbolPairMatch().apply {
            putPair('{', SymbolPairMatch.SymbolPair("{", "}"))
            putPair('(', SymbolPairMatch.SymbolPair("(", ")"))
            putPair('[', SymbolPairMatch.SymbolPair("[", "]"))
            putPair('"', SymbolPairMatch.SymbolPair("\"", "\""))
            putPair('\'', SymbolPairMatch.SymbolPair("'", "'"))
            putPair('<', SymbolPairMatch.SymbolPair("<", ">")) // For templates
        }
        
        private val JAVA_SYMBOL_PAIRS = SymbolPairMatch().apply {
            putPair('{', SymbolPairMatch.SymbolPair("{", "}"))
            putPair('(', SymbolPairMatch.SymbolPair("(", ")"))
            putPair('[', SymbolPairMatch.SymbolPair("[", "]"))
            putPair('"', SymbolPairMatch.SymbolPair("\"", "\""))
            putPair('\'', SymbolPairMatch.SymbolPair("'", "'"))
        }
        
        private val PYTHON_SYMBOL_PAIRS = SymbolPairMatch().apply {
            putPair('{', SymbolPairMatch.SymbolPair("{", "}"))
            putPair('(', SymbolPairMatch.SymbolPair("(", ")"))
            putPair('[', SymbolPairMatch.SymbolPair("[", "]"))
            putPair('"', SymbolPairMatch.SymbolPair("\"", "\""))
            putPair('\'', SymbolPairMatch.SymbolPair("'", "'"))
        }
    }
    
    private val symbolPairMatch: SymbolPairMatch by lazy {
        val pairs = when (languageType.lowercase()) {
            "cpp", "c++" -> CPP_SYMBOL_PAIRS
            "java" -> JAVA_SYMBOL_PAIRS
            "python", "py" -> PYTHON_SYMBOL_PAIRS
            else -> JAVA_SYMBOL_PAIRS // Default
        }
        SymbolPairMatch(pairs)
    }
    
    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        val prefix = extractPrefix(content, position)
        
        if (prefix.isNotEmpty()) {
            completionProvider.provideCompletions(
                content, position, prefix, languageType, publisher
            )
        }
    }
    
    override fun getSymbolPairs(): SymbolPairMatch {
        return symbolPairMatch
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
    
    override fun getInterruptionLevel(): Int {
        return INTERRUPTION_LEVEL_SLIGHT
    }
}