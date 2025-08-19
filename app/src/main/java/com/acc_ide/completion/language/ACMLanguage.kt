package com.acc_ide.completion.language

import android.os.Bundle
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.lang.analysis.AnalyzeManager
import io.github.rosemoe.sora.lang.analysis.SimpleAnalyzeManager
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.SymbolPairMatch
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import com.acc_ide.completion.core.ModernACMCompletionProvider

/**
 * ACM编程语言实现
 * 提供基础的关键字、STL容器、算法补全
 * 结合TextMate语法高亮和ACM专用补全
 */
class ACMLanguage(private val scopeName: String) : Language {
    
    private val acmCompletionProvider = ModernACMCompletionProvider()
    private val textMateLanguage: TextMateLanguage
    
    init {
        // 创建TextMate语言实例用于语法高亮
        textMateLanguage = TextMateLanguage.create(scopeName, true)
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
        android.util.Log.d("ACMLanguage", "requireAutoComplete called at ${position.line}:${position.column}")
        
        try {
            // 根据scopeName检测语言类型
            val language = detectLanguageFromScope(scopeName)
            android.util.Log.d("ACMLanguage", "Detected language: $language for scope: $scopeName")
            
            // 使用现代化ACM补全提供器进行智能补全（包含关键字、STL、TreeSitter符号）
            android.util.Log.d("ACMLanguage", "Calling ModernACMCompletionProvider.requireAutoComplete")
            acmCompletionProvider.requireAutoComplete(content, position, publisher, language)
            android.util.Log.d("ACMLanguage", "ModernACMCompletionProvider completed successfully")
        } catch (e: Exception) {
            android.util.Log.e("ACMLanguage", "Modern ACM completion failed", e)
            // 发生异常时不提供补全，避免错误的fallback
        }
    }
    
    /**
     * 根据TextMate scope名称检测语言类型
     */
    private fun detectLanguageFromScope(scopeName: String): String {
        return when {
            scopeName.contains("cpp") || scopeName.contains("c++") -> "cpp"
            scopeName.contains("java") -> "java"
            scopeName.contains("python") -> "python"
            else -> "cpp" // 默认使用C++，保持与老系统一致
        }
    }
    
    override fun getFormatter(): Formatter {
        return textMateLanguage.formatter
    }
    
    override fun getSymbolPairs(): SymbolPairMatch {
        return textMateLanguage.symbolPairs
    }
    
    override fun getNewlineHandlers(): Array<NewlineHandler> {
        return textMateLanguage.newlineHandlers ?: emptyArray()
    }
    
    override fun useTab(): Boolean {
        return textMateLanguage.useTab()
    }
    
    override fun getIndentAdvance(content: ContentReference, line: Int, column: Int): Int {
        return textMateLanguage.getIndentAdvance(content, line, column)
    }
    
    override fun destroy() {
        textMateLanguage.destroy()
    }
}