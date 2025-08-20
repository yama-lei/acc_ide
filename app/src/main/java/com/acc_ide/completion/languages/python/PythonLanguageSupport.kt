package com.acc_ide.completion.languages.python

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
 * Python 语言支持实现
 * 
 * 提供完整的Python语言支持，包括：
 * 1. 基于TextMate的语法高亮
 * 2. 基于TreeSitter的智能补全（变量、函数、类）
 * 3. Python内置类型和函数补全
 * 4. 竞赛编程常用模块补全（math、collections、itertools）
 * 5. 动态类型推断和成员访问补全
 */
class PythonLanguageSupport(private val scopeName: String) : Language {
    
    private val TAG = "PythonLanguageSupport"
    private val acmCompletionProvider = ModernACMCompletionProvider()
    private val textMateLanguage: TextMateLanguage
    
    init {
        // 创建TextMate语言实例用于语法高亮
        textMateLanguage = TextMateLanguage.create(scopeName, true)
        android.util.Log.d(TAG, "PythonLanguageSupport initialized with scope: $scopeName")
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
            // 包含：TreeSitter符号提取、Python内置类型、模块函数、关键字
            android.util.Log.d(TAG, "Calling ModernACMCompletionProvider.requireAutoComplete for Python")
            acmCompletionProvider.requireAutoComplete(content, position, publisher, language)
            android.util.Log.d(TAG, "Python completion provider completed successfully")
            
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Python completion failed", e)
            
            // 降级处理：提供基础关键字补全
            try {
                android.util.Log.d(TAG, "Attempting fallback Python completion")
                acmCompletionProvider.requireAutoComplete(content, position, publisher, "python")
            } catch (fallbackError: Exception) {
                android.util.Log.e(TAG, "Python fallback completion also failed", fallbackError)
            }
        }
    }
    
    /**
     * 根据TextMate scope名称检测语言类型
     */
    private fun detectLanguageFromScope(scopeName: String): String {
        return when {
            scopeName.contains("python") -> {
                android.util.Log.d(TAG, "Detected Python language from scope")
                "python"
            }
            scopeName.contains("java") -> {
                android.util.Log.d(TAG, "Detected Java language from scope")
                "java"
            }
            scopeName.contains("cpp") || scopeName.contains("c++") -> {
                android.util.Log.d(TAG, "Detected C++ language from scope")
                "cpp"
            }
            else -> {
                android.util.Log.d(TAG, "Unknown scope, defaulting to Python: $scopeName")
                "python" // 默认使用Python
            }
        }
    }
    
    override fun getFormatter(): Formatter {
        return textMateLanguage.formatter
    }
    
    override fun getSymbolPairs(): SymbolPairMatch {
        // Python特定的符号配对
        val pairs = textMateLanguage.symbolPairs
        android.util.Log.d(TAG, "Retrieved Python symbol pairs")
        return pairs
    }
    
    override fun getNewlineHandlers(): Array<NewlineHandler> {
        val handlers = textMateLanguage.newlineHandlers ?: emptyArray()
        android.util.Log.d(TAG, "Retrieved ${handlers.size} Python newline handlers")
        return handlers
    }
    
    override fun useTab(): Boolean {
        // Python通常使用空格缩进，但保持TextMate的设置
        return textMateLanguage.useTab()
    }
    
    override fun getIndentAdvance(content: ContentReference, line: Int, column: Int): Int {
        // Python对缩进非常敏感，使用TextMate的缩进处理
        val indentAdvance = textMateLanguage.getIndentAdvance(content, line, column)
        android.util.Log.d(TAG, "Python indent advance: $indentAdvance at ${line}:${column}")
        return indentAdvance
    }
    
    override fun destroy() {
        android.util.Log.d(TAG, "Destroying PythonLanguageSupport")
        textMateLanguage.destroy()
    }
    
    
    /**
     * 检查是否为Python特定的语法上下文
     */
    private fun isPythonSpecificContext(content: ContentReference, position: CharPosition): Boolean {
        return try {
            val line = content.getLine(position.line)
            val beforeCursor = line.substring(0, minOf(position.column, line.length))
            
            // 检查Python特定的语法特征
            when {
                // 装饰器语法 @decorator
                beforeCursor.contains("@") -> {
                    android.util.Log.d(TAG, "Detected Python decorator context")
                    true
                }
                // 导入语法 from ... import
                beforeCursor.contains("from ") && beforeCursor.contains("import") -> {
                    android.util.Log.d(TAG, "Detected Python import context")
                    true
                }
                // f-string语法 f"..."
                beforeCursor.contains("f\"") || beforeCursor.contains("f'") -> {
                    android.util.Log.d(TAG, "Detected Python f-string context")
                    true
                }
                // lambda表达式
                beforeCursor.contains("lambda") -> {
                    android.util.Log.d(TAG, "Detected Python lambda context")
                    true
                }
                else -> false
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to detect Python context", e)
            false
        }
    }
}