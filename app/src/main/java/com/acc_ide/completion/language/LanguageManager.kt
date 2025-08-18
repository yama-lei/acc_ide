package com.acc_ide.completion.language

import android.content.Context
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import com.acc_ide.util.TextMateManager
import com.acc_ide.completion.language.ACMLanguage

/**
 * 语言管理器，用于管理和切换不同编程语言
 * 现在集成了TreeSitter支持的ACM语言
 */
object LanguageManager {
    
    private var currentLanguage: String = "cpp"
    private var isInitialized = false
    private var isTextMateEnabledValue = true
    private var isTreeSitterPreferredValue = true
    
    /**
     * 初始化LanguageManager
     */
    fun initialize(context: Context) {
        if (!isInitialized) {
            // 初始化TextMate支持
            TextMateManager.initialize(context)
            isInitialized = true
        }
    }
    
    /**
     * 获取当前语言
     */
    fun getCurrentLanguage(): String {
        return currentLanguage
    }
    
    /**
     * 设置当前语言
     */
    fun setCurrentLanguage(language: String) {
        currentLanguage = language
    }
    
    /**
     * 根据文件名和扩展名获取Language实例
     * 这是EditorFragment调用的主要方法
     */
    fun getLanguageForFile(fileName: String, fileExtension: String): Language {
        return when (fileExtension.lowercase()) {
            "cpp", "c++", "cc", "cxx", "h", "hpp" -> {
                createTextMateLanguage("source.cpp")
            }
            "java" -> {
                createTextMateLanguage("source.java")
            }
            "py", "python" -> {
                createTextMateLanguage("source.python")
            }
            else -> {
                createTextMateLanguage("text.plain")
            }
        }
    }
    
    /**
     * 创建ACM语言实例（集成TextMate和ACM补全）
     */
    private fun createTextMateLanguage(scopeName: String): Language {
        return if (isTextMateEnabled()) {
            ACMLanguage(scopeName)
        } else {
            EmptyLanguage()
        }
    }
    
    /**
     * 根据语言名称获取Language实例
     */
    fun getLanguageInstance(language: String): Language {
        return when (language.lowercase()) {
            "cpp", "c++" -> createTextMateLanguage("source.cpp")
            "java" -> createTextMateLanguage("source.java") 
            "python", "py" -> createTextMateLanguage("source.python")
            else -> createTextMateLanguage("text.plain")
        }
    }
    
    /**
     * 获取支持的语言列表
     */
    fun getSupportedLanguages(): List<String> {
        return listOf("cpp", "java", "python")
    }
    
    /**
     * 检查语言是否支持
     */
    fun isLanguageSupported(language: String): Boolean {
        return getSupportedLanguages().contains(language.lowercase())
    }
    
    /**
     * 检查TextMate是否启用
     */
    fun isTextMateEnabled(): Boolean {
        return isTextMateEnabledValue
    }
    
    /**
     * 设置TextMate启用状态
     */
    fun setTextMateEnabled(enabled: Boolean) {
        isTextMateEnabledValue = enabled
    }
    
    /**
     * 检查是否优先使用TreeSitter
     */
    fun isTreeSitterPreferred(): Boolean {
        return isTreeSitterPreferredValue
    }
    
    /**
     * 设置是否优先使用TreeSitter
     */
    fun setTreeSitterPreferred(preferred: Boolean) {
        isTreeSitterPreferredValue = preferred
    }
}