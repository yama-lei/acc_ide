package com.acc_ide.completion.core

import android.os.Bundle
import android.util.Log
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import com.acc_ide.completion.framework.CompletionManager

/**
 * 现代化ACM竞赛编程智能补全提供器
 * 
 * 基于新的多语言补全框架重构
 * 
 * 特性：
 * 1. 支持多语言（C++, Java, Python）
 * 2. 统一的补全接口
 * 3. 可扩展的语言处理器架构
 * 4. 向后兼容原有API
 */
class ModernACMCompletionProvider {
    
    private val TAG = "ModernACMCompletionProvider"
    
    private val completionManager = CompletionManager.getInstance()
    
    /**
     * 主要的自动补全入口点
     * 与sora-editor的Language接口兼容
     */
    fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        // 从extraArguments中获取语言类型，或从文件名推断
        val language = extraArguments.getString("language") 
            ?: detectLanguageFromContent(content) 
            ?: "cpp" // 默认C++
        
        try {
            Log.d(TAG, "requireAutoComplete called for language: $language")
            
            // 使用统一的补全管理器
            completionManager.requireAutoComplete(content, position, publisher, language)
            
        } catch (e: Exception) {
            Log.e(TAG, "Modern completion failed for language: $language", e)
            
            // 降级到最基本的补全 - 使用原始检测到的语言，而不是硬编码
            try {
                Log.d(TAG, "Attempting fallback completion with language: $language")
                completionManager.requireAutoComplete(content, position, publisher, language)
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Fallback completion also failed for language: $language", fallbackError)
            }
        }
    }
    
    /**
     * 重载方法：支持指定语言类型
     */
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
    
    /**
     * 获取支持的语言列表
     */
    fun getSupportedLanguages(): Set<String> {
        return completionManager.getSupportedLanguages()
    }
    
    /**
     * 检查是否支持指定语言
     */
    fun isLanguageSupported(language: String): Boolean {
        return completionManager.isLanguageSupported(language)
    }
    
    /**
     * 获取语言状态信息
     */
    fun getLanguageStatus(language: String): Map<String, Any> {
        return completionManager.getLanguageStatus(language)
    }
    
    /**
     * 获取所有语言的状态
     */
    fun getAllLanguageStatus(): Map<String, Map<String, Any>> {
        return completionManager.getSupportedLanguages().associateWith { language ->
            completionManager.getLanguageStatus(language)
        }
    }
    
    /**
     * 从内容推断语言类型
     */
    private fun detectLanguageFromContent(content: ContentReference): String? {
        return try {
            // 简单的启发式检测
            val firstLine = content.getLine(0).trim()
            
            when {
                // C++ 特征
                firstLine.contains("#include") -> "cpp"
                firstLine.contains("using namespace") -> "cpp"
                
                // Java 特征
                firstLine.contains("package ") -> "java"
                firstLine.contains("public class") -> "java"
                firstLine.contains("import java.") -> "java"
                
                // Python 特征
                firstLine.startsWith("#!") && firstLine.contains("python") -> "python"
                firstLine.contains("import ") && !firstLine.contains("java.") -> "python"
                firstLine.contains("def ") -> "python"
                
                else -> null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to detect language from content", e)
            null
        }
    }
    
    /**
     * 检查补全系统是否可用
     */
    fun isAvailable(): Boolean {
        return completionManager.isAvailable()
    }
    
    /**
     * 重新初始化补全系统
     */
    fun reinitialize() {
        completionManager.reinitialize()
    }
    
    /**
     * 获取系统信息
     */
    fun getSystemInfo(): Map<String, Any> {
        return mapOf(
            "available" to isAvailable(),
            "supportedLanguages" to getSupportedLanguages(),
            "languageStatus" to getAllLanguageStatus(),
            "version" to "2.0-framework"
        )
    }
}