package com.acc_ide.completion.framework

import android.util.Log
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import com.acc_ide.completion.core.*

/**
 * Universal Completion Engine Implementation
 * 
 * Manages multiple language processors and provides unified completion interface
 */
class UniversalCompletionEngine {
    
    private val TAG = "UniversalCompletionEngine"
    
    // Language processor registry
    private val processors = mutableMapOf<String, LanguageProcessor>()
    private val usageFrequency = mutableMapOf<String, Int>()
    
    /**
     * Register language processor
     */
    fun registerProcessor(processor: LanguageProcessor) {
        processors[processor.getLanguageId()] = processor
        Log.d(TAG, "Registered processor for language: ${processor.getLanguageId()}")
    }
    
    
    /**
     * Get language processor
     */
    private fun getProcessor(language: String): LanguageProcessor? {
        return processors[language.lowercase()]
    }
    
    fun getSupportedLanguages(): Set<String> {
        return processors.keys.toSet()
    }
    
    fun isLanguageSupported(language: String): Boolean {
        return processors.containsKey(language.lowercase())
    }
    
    
    
    fun provideCompletions(
        contentRef: ContentReference,
        position: CharPosition,
        prefix: String,
        language: String
    ): List<CompletionItem> {
        val processor = getProcessor(language) ?: return emptyList()
        
        val items = mutableListOf<CompletionItem>()
        
        try {
            Log.d(TAG, "Providing completions for language: $language, prefix: '$prefix'")
            
            // Check if this is member access
            val (actualPrefix, contextVar) = processor.extractMemberAccessContext(contentRef, position, prefix)
            
            if (contextVar != null) {
                // Member access completion
                items.addAll(processor.provideMemberCompletions(contentRef, position, contextVar, actualPrefix))
                Log.d(TAG, "Added ${items.size} member completions")
            } else {
                // Regular completion
                items.addAll(processor.provideRegularCompletions(contentRef, position, actualPrefix))
                Log.d(TAG, "Added ${items.size} regular completions")
            }
            
            // Update usage frequency
            updateUsageFrequency(prefix)
            
            // Sort and return
            return sortCompletionItems(items, actualPrefix)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to provide completions for language: $language", e)
            // Fallback to keyword completion
            return getKeywordCompletions(prefix, language)
        }
    }
    
    fun provideMemberCompletions(
        contentRef: ContentReference,
        position: CharPosition,
        language: String,
        contextVar: String,
        prefix: String
    ): List<CompletionItem> {
        val processor = getProcessor(language) ?: return emptyList()
        
        return try {
            processor.provideMemberCompletions(contentRef, position, contextVar, prefix)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to provide member completions for language: $language", e)
            emptyList()
        }
    }
    
    fun getKeywordCompletions(prefix: String, language: String): List<CompletionItem> {
        val processor = getProcessor(language) ?: return emptyList()
        
        return processor.getKeywords()
            .filter { it.startsWith(prefix, ignoreCase = true) }
            .map { keyword ->
                PriorityCompletionItem(
                    keyword,
                    "Keyword",
                    prefix.length,
                    keyword,
                    CompletionConstants.PRIORITY_KEYWORD,
                    CompletionItemKind.Keyword
                )
            }
    }
    
    
    fun isAvailable(): Boolean {
        return processors.values.any { it.isAvailable() }
    }
    
    /**
     * Update usage frequency
     */
    private fun updateUsageFrequency(prefix: String) {
        if (prefix.isNotEmpty()) {
            usageFrequency[prefix] = (usageFrequency[prefix] ?: 0) + 1
        }
    }
    
    /**
     * Sort completion items
     */
    private fun sortCompletionItems(items: List<CompletionItem>, prefix: String): List<CompletionItem> {
        return items.sortedWith(
            compareByDescending<CompletionItem> { 
                (it as? PriorityCompletionItem)?.priority ?: 0
            }.thenBy { 
                calculateRelevanceScore(it.label.toString(), prefix) 
            }.thenByDescending {
                // Use prefix for frequency lookup to match updateUsageFrequency()
                usageFrequency[prefix] ?: 0
            }.thenBy { 
                it.label.toString() 
            }
        )
    }
    
    /**
     * Calculate relevance score
     */
    private fun calculateRelevanceScore(suggestion: String, prefix: String): Int {
        return when {
            suggestion.equals(prefix, ignoreCase = true) -> 0
            suggestion.startsWith(prefix, ignoreCase = true) -> 1
            suggestion.contains(prefix, ignoreCase = true) -> 2
            else -> 3
        }
    }
    
    /**
     * Completion item with priority
     */
    class PriorityCompletionItem(
        label: CharSequence,
        desc: CharSequence,
        prefixLength: Int,
        commitText: String,
        val priority: Int,
        kind: CompletionItemKind
    ) : SimpleCompletionItem(label, desc, prefixLength, commitText) {
        
        init {
            this.kind(kind)
        }
    }
}

