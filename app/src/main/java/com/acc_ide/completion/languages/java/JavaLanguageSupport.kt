package com.acc_ide.completion.languages.java

import android.os.Bundle
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.SymbolPairMatch
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import com.acc_ide.completion.core.ModernACMCompletionProvider

/**
 * Java 语言支持实现
 * 
 * 提供完整的Java语言支持，包括：
 * 1. 基于TextMate的语法高亮
 * 2. 基于TreeSitter的智能补全（变量、方法、类）
 * 3. Java集合框架和标准库补全
 * 4. 竞赛编程常用类和方法补全
 */
class JavaLanguageSupport(private val scopeName: String) : Language {
    
    private val TAG = "JavaLanguageSupport"
    private val acmCompletionProvider = ModernACMCompletionProvider()
    private val textMateLanguage: TextMateLanguage
    
    init {
        // 创建TextMate语言实例用于语法高亮
        textMateLanguage = TextMateLanguage.create(scopeName, true)
        android.util.Log.d(TAG, "JavaLanguageSupport initialized with scope: $scopeName")
    }
    
    override fun getAnalyzeManager(): AnalyzeManager {
        return textMateLanguage.analyzeManager
    }
    
    override fun getInterruptionLevel(): Int {
        return textMateLanguage.interruptionLevel
    }
    
    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        android.util.Log.d(TAG, "requireAutoComplete called at ${position.line}:${position.column}")
        
        try {
            // 根据scopeName检测语言类型
            val language = detectLanguageFromScope(scopeName)
            android.util.Log.d(TAG, "Detected language: $language for scope: $scopeName")
            
            // 使用现代化补全提供器进行智能补全
            // 包含：TreeSitter符号提取、Java集合框架、工具类、关键字
            android.util.Log.d(TAG, "Calling ModernACMCompletionProvider.requireAutoComplete for Java")
            acmCompletionProvider.requireAutoComplete(content, position, publisher, language)
            android.util.Log.d(TAG, "Java completion provider completed successfully")
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Java completion failed", e)
            
            // 降级处理：提供基础关键字补全
            try {
                android.util.Log.d(TAG, "Attempting fallback Java completion")
                acmCompletionProvider.requireAutoComplete(content, position, publisher, "java")
            } catch (fallbackError: Exception) {
                android.util.Log.e(TAG, "Java fallback completion also failed", fallbackError)
            }
        }
    }
    
    /**
     * 根据TextMate scope名称检测语言类型
     */
    private fun detectLanguageFromScope(scopeName: String): String {
        return when {
            scopeName.contains("java") -> {
                android.util.Log.d(TAG, "Detected Java language from scope")
                "java"
            }
            scopeName.contains("cpp") || scopeName.contains("c++") -> {
                android.util.Log.d(TAG, "Detected C++ language from scope")
                "cpp"
            }
            scopeName.contains("python") -> {
                android.util.Log.d(TAG, "Detected Python language from scope")
                "python"
            }
            else -> {
                android.util.Log.d(TAG, "Unknown scope, defaulting to Java: $scopeName")
                "java" // 默认使用Java
            }
        }
    }
    
    override fun getFormatter(): Formatter {
        return textMateLanguage.formatter
    }
    
    override fun getSymbolPairs(): SymbolPairMatch {
        // Java特定的符号配对
        val pairs = textMateLanguage.symbolPairs
        android.util.Log.d(TAG, "Retrieved Java symbol pairs")
        return pairs
    }
    
    override fun getNewlineHandlers(): Array<NewlineHandler> {
        val handlers = textMateLanguage.newlineHandlers ?: emptyArray()
        android.util.Log.d(TAG, "Retrieved ${handlers.size} Java newline handlers")
        return handlers
    }
    
    override fun useTab(): Boolean {
        // Java通常使用空格缩进，但保持TextMate的设置
        return textMateLanguage.useTab()
    }
    
    override fun getIndentAdvance(content: ContentReference, line: Int, column: Int): Int {
        return textMateLanguage.getIndentAdvance(content, line, column)
    }
    
    override fun destroy() {
        android.util.Log.d(TAG, "Destroying JavaLanguageSupport")
        textMateLanguage.destroy()
    }
    
}