package com.acc_ide.completion.framework

import android.util.Log
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import com.acc_ide.completion.languages.cpp.CppLanguageProcessor
import com.acc_ide.completion.languages.java.JavaLanguageProcessor
import com.acc_ide.completion.languages.python.PythonLanguageProcessor

/**
 * Manages multi-language completion system
 */
class CompletionManager private constructor() {
    
    companion object {
        private const val TAG = "CompletionManager"
        
        @Volatile
        private var INSTANCE: CompletionManager? = null
        
        fun getInstance(): CompletionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CompletionManager().also { INSTANCE = it }
            }
        }
    }
    
    private val engine = UniversalCompletionEngine()
    
    init {
        initializeLanguageProcessors()
    }
    
    private fun initializeLanguageProcessors() {
        try {
            engine.registerProcessor(CppLanguageProcessor())
            engine.registerProcessor(JavaLanguageProcessor())
            engine.registerProcessor(PythonLanguageProcessor())
            
            Log.i(TAG, "Initialized completion manager with ${engine.getSupportedLanguages().size} language processors")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize language processors", e)
        }
    }
    
    fun getSupportedLanguages(): Set<String> = engine.getSupportedLanguages()
    
    fun isLanguageSupported(language: String): Boolean = engine.isLanguageSupported(language)
    

    fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        language: String = "cpp"
    ) {
        try {
            Log.d(TAG, "requireAutoComplete called for language: $language")
            
            if (!isLanguageSupported(language)) {
                Log.w(TAG, "Language '$language' is not supported")
                return
            }
            
            // Extract prefix
            val prefix = extractPrefix(content, position)
            Log.d(TAG, "Extracted prefix: '$prefix'")
            
            // Get completion suggestions
            val completions = engine.provideCompletions(content, position, prefix, language)
            
            // Publish results
            publisher.addItems(completions)
            Log.d(TAG, "Published ${completions.size} completions for language: $language")
            
        } catch (e: Exception) {
            Log.e(TAG, "Auto completion failed for language: $language", e)
            
            // Fallback to basic keyword completion
            try {
                val prefix = extractPrefix(content, position)
                val fallbackCompletions = engine.getKeywordCompletions(prefix, language)
                publisher.addItems(fallbackCompletions)
                Log.d(TAG, "Published ${fallbackCompletions.size} fallback completions")
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Fallback completion also failed", fallbackError)
            }
        }
    }
    
    /**
     * Get completion suggestions directly (without publisher)
     */
    fun getCompletions(
        content: ContentReference,
        position: CharPosition,
        language: String = "cpp"
    ): List<CompletionItem> {
        return try {
            if (!isLanguageSupported(language)) {
                Log.w(TAG, "Language '$language' is not supported")
                return emptyList()
            }
            
            val prefix = extractPrefix(content, position)
            engine.provideCompletions(content, position, prefix, language)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get completions for language: $language", e)
            emptyList()
        }
    }
    
    /**
     * Get member completion suggestions
     */
    fun getMemberCompletions(
        content: ContentReference,
        position: CharPosition,
        contextVar: String,
        prefix: String,
        language: String = "cpp"
    ): List<CompletionItem> {
        return try {
            if (!isLanguageSupported(language)) {
                return emptyList()
            }
            
            engine.provideMemberCompletions(content, position, language, contextVar, prefix)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get member completions", e)
            emptyList()
        }
    }
    
    /**
     * Detect language from file extension
     */
    fun detectLanguageFromExtension(filename: String): String {
        val extension = filename.substringAfterLast('.', "").lowercase()
        
        return when (extension) {
            "cpp", "cxx", "cc", "c", "h", "hpp", "hxx" -> "cpp"
            "java" -> "java"
            "py", "pyw", "pyi" -> "python"
            else -> "cpp" // Default to C++
        }
    }
    
    /**
     * Get language-specific status information
     */
    fun getLanguageStatus(language: String): Map<String, Any> {
        return mapOf(
            "supported" to isLanguageSupported(language),
            "available" to engine.isAvailable(),
            "implementation" to when (language.lowercase()) {
                "cpp" -> "Full implementation with TreeSitter"
                "java" -> "Basic implementation - keywords only"
                "python" -> "Basic implementation - keywords only"
                else -> "Not supported"
            }
        )
    }
    
    /**
     * Extract prefix
     */
    private fun extractPrefix(content: ContentReference, position: CharPosition): String {
        return try {
            val line = content.getLine(position.line)
            val beforeCursor = line.substring(0, minOf(position.column, line.length))
            
            // Extract current word as prefix
            val match = Regex("""[a-zA-Z_][a-zA-Z0-9_]*$""").find(beforeCursor)
            match?.value ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Reinitialize (for restart or config changes)
     */
    fun reinitialize() {
        Log.i(TAG, "Reinitializing completion manager")
        initializeLanguageProcessors()
    }
    
    /**
     * Get engine availability status
     */
    fun isAvailable(): Boolean {
        return engine.isAvailable()
    }
}

