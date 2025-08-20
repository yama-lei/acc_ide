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
 * 补全管理器
 * 
 * 统一管理多语言补全系统，提供简单的API接口
 * 自动注册所有可用的语言处理器
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
    
    /**
     * 初始化所有语言处理器
     */
    private fun initializeLanguageProcessors() {
        try {
            // 注册C++处理器（完全实现）
            val cppProcessor = CppLanguageProcessor()
            engine.registerProcessor(cppProcessor)
            Log.i(TAG, "Registered C++ language processor")
            
            // 注册Java处理器（基础实现）
            val javaProcessor = JavaLanguageProcessor()
            engine.registerProcessor(javaProcessor)
            Log.i(TAG, "Registered Java language processor (basic)")
            
            // 注册Python处理器（基础实现）
            val pythonProcessor = PythonLanguageProcessor()
            engine.registerProcessor(pythonProcessor)
            Log.i(TAG, "Registered Python language processor (basic)")
            
            Log.i(TAG, "Initialized completion manager with ${engine.getSupportedLanguages().size} language processors")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize language processors", e)
        }
    }
    
    /**
     * 获取支持的语言列表
     */
    fun getSupportedLanguages(): Set<String> {
        return engine.getSupportedLanguages()
    }
    
    /**
     * 检查是否支持指定语言
     */
    fun isLanguageSupported(language: String): Boolean {
        return engine.isLanguageSupported(language)
    }
    
    /**
     * 主要的自动补全入口点
     * 与sora-editor的Language接口兼容
     */
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
            
            // 提取前缀
            val prefix = extractPrefix(content, position)
            Log.d(TAG, "Extracted prefix: '$prefix'")
            
            // 获取补全建议
            val completions = engine.provideCompletions(content, position, prefix, language)
            
            // 发布结果
            publisher.addItems(completions)
            Log.d(TAG, "Published ${completions.size} completions for language: $language")
            
        } catch (e: Exception) {
            Log.e(TAG, "Auto completion failed for language: $language", e)
            
            // 降级到基础关键字补全
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
     * 直接获取补全建议（不通过publisher）
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
     * 获取成员补全建议
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
     * 根据文件扩展名推断语言类型
     */
    fun detectLanguageFromExtension(filename: String): String {
        val extension = filename.substringAfterLast('.', "").lowercase()
        
        return when (extension) {
            "cpp", "cxx", "cc", "c", "h", "hpp", "hxx" -> "cpp"
            "java" -> "java"
            "py", "pyw", "pyi" -> "python"
            else -> "cpp" // 默认使用C++
        }
    }
    
    /**
     * 获取语言特定的状态信息
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
     * 提取前缀
     */
    private fun extractPrefix(content: ContentReference, position: CharPosition): String {
        return try {
            val line = content.getLine(position.line)
            val beforeCursor = line.substring(0, minOf(position.column, line.length))
            
            // 提取当前单词作为前缀
            val match = Regex("""[a-zA-Z_][a-zA-Z0-9_]*$""").find(beforeCursor)
            match?.value ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 重新初始化（用于重启或配置更改）
     */
    fun reinitialize() {
        Log.i(TAG, "Reinitializing completion manager")
        initializeLanguageProcessors()
    }
    
    /**
     * 获取引擎可用状态
     */
    fun isAvailable(): Boolean {
        return engine.isAvailable()
    }
}