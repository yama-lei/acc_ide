package com.acc_ide.completion

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.widget.CodeEditor
import com.acc_ide.util.TextMateManager

/**
 * Manager for switching between different language implementations
 * Handles both TextMate-based syntax highlighting and ACM-enhanced completion
 */
object LanguageManager {
    
    private lateinit var context: Context
    
    fun initialize(context: Context) {
        this.context = context
    }
    
    /**
     * Get the appropriate language implementation for the given file extension
     * Always uses hybrid mode with TextMate syntax highlighting and ACM completion (no identifier completion)
     */
    fun getLanguageForFile(fileName: String, fileExtension: String): Language {
        // Always use modified hybrid mode (TextMate syntax + ACM completion, no identifier completion)
        return createHybridLanguage(fileExtension)
    }
    
    /**
     * Create ACM-enhanced language with intelligent completion
     */
    private fun createACMLanguage(fileExtension: String): Language {
        val languageType = mapExtensionToLanguageType(fileExtension)
        return ACMLanguage(languageType)
    }
    
    /**
     * Create standard TextMate language
     */
    private fun createTextMateLanguage(fileExtension: String): Language {
        val languageMapping = TextMateManager.getLanguageMapping()
        val scopeName = languageMapping[fileExtension] ?: "text.plain"
        
        return try {
            // Create TextMate language with proper scope
            val language = TextMateLanguage.create(scopeName, true)
            
            // Set up file provider if not already done
            try {
                FileProviderRegistry.getInstance().addFileProvider(
                    io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver(
                        context.assets
                    )
                )
            } catch (e: Exception) {
                // FileProvider already exists, ignore
            }
            
            language
        } catch (e: Exception) {
            // Fallback to ACM language if TextMate fails
            createACMLanguage(fileExtension)
        }
    }
    
    /**
     * Create hybrid language combining TextMate syntax highlighting with ACM completion
     */
    private fun createHybridLanguage(fileExtension: String): Language {
        return try {
            val textMateLanguage = createTextMateLanguage(fileExtension)
            val languageType = mapExtensionToLanguageType(fileExtension)
            
            // Create wrapper that combines both
            HybridACMLanguage(textMateLanguage, languageType)
        } catch (e: Exception) {
            // Fallback to pure ACM language
            createACMLanguage(fileExtension)
        }
    }
    
    /**
     * Map file extension to language type for ACM completion
     */
    private fun mapExtensionToLanguageType(extension: String): String {
        return when (extension.lowercase()) {
            "cpp", "cc", "cxx", "c++" -> "cpp"
            "c", "h" -> "c"
            "java" -> "java"
            "py", "pyw" -> "python"
            "js", "ts" -> "javascript"
            "kt", "kts" -> "kotlin"
            else -> "cpp" // Default to C++ for competitive programming
        }
    }
    
    /**
     * Check if ACM completion is enabled
     */
    fun isACMCompletionEnabled(): Boolean {
        // Since we are using a fixed hybrid mode, ACM completion is always enabled
        return true
    }
    
    /**
     * Check if TextMate highlighting is enabled  
     */
    fun isTextMateEnabled(): Boolean {
        // Since we are using a fixed hybrid mode, TextMate highlighting is always enabled
        return true
    }
}