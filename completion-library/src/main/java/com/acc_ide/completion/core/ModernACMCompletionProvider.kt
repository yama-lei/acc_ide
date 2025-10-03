package com.acc_ide.completion.core

import android.os.Bundle
import android.util.Log
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import com.acc_ide.completion.framework.CompletionManager

/**
 * Modern ACM code completion provider
 */
class ModernACMCompletionProvider {
    
    private val TAG = "ModernACMCompletionProvider"
    private val completionManager = CompletionManager.getInstance()
    
    fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        val language = extraArguments.getString("language") 
            ?: detectLanguageFromContent(content) 
            ?: "cpp"
        
        try {
            Log.d(TAG, "requireAutoComplete called for language: $language")
            completionManager.requireAutoComplete(content, position, publisher, language)
            
        } catch (e: Exception) {
            Log.e(TAG, "Modern completion failed for language: $language", e)
            
            // Fallback with original detected language
            try {
                Log.d(TAG, "Attempting fallback completion with language: $language")
                completionManager.requireAutoComplete(content, position, publisher, language)
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Fallback completion also failed for language: $language", fallbackError)
            }
        }
    }
    
    fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        language: String
    ) {
        try {
            Log.d(TAG, "requireAutoComplete called with explicit language: $language")
            completionManager.requireAutoComplete(content, position, publisher, language)
        } catch (e: Exception) {
            Log.e(TAG, "Completion failed for language: $language", e)
        }
    }
    
    fun getSupportedLanguages(): Set<String> = completionManager.getSupportedLanguages()
    
    fun isLanguageSupported(language: String): Boolean = completionManager.isLanguageSupported(language)
    
    fun getLanguageStatus(language: String): Map<String, Any> = completionManager.getLanguageStatus(language)
    
    fun getAllLanguageStatus(): Map<String, Map<String, Any>> {
        return completionManager.getSupportedLanguages().associateWith { language ->
            completionManager.getLanguageStatus(language)
        }
    }
    
    private fun detectLanguageFromContent(content: ContentReference): String? {
        return try {
            val firstLine = content.getLine(0).trim()
            
            when {
                firstLine.contains("#include") || firstLine.contains("using namespace") -> "cpp"
                firstLine.contains("package ") || firstLine.contains("public class") || firstLine.contains("import java.") -> "java"
                firstLine.startsWith("#!") && firstLine.contains("python") || 
                    firstLine.contains("import ") && !firstLine.contains("java.") || 
                    firstLine.contains("def ") -> "python"
                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to detect language from content", e)
            null
        }
    }
    
    fun isAvailable(): Boolean = completionManager.isAvailable()
    
    fun reinitialize() = completionManager.reinitialize()
    
    fun getSystemInfo(): Map<String, Any> {
        return mapOf(
            "available" to isAvailable(),
            "supportedLanguages" to getSupportedLanguages(),
            "languageStatus" to getAllLanguageStatus(),
            "version" to "2.0-framework"
        )
    }
}
