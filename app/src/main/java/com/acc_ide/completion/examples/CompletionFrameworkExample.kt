package com.acc_ide.completion.examples

import android.util.Log
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import com.acc_ide.completion.framework.CompletionManager
import com.acc_ide.completion.core.ModernACMCompletionProvider

/**
 * 多语言补全框架使用示例
 * 
 * 展示如何使用新的多语言补全系统
 */
class CompletionFrameworkExample {
    
    companion object {
        private const val TAG = "CompletionFrameworkExample"
    }
    
    private val completionManager = CompletionManager.getInstance()
    private val modernProvider = ModernACMCompletionProvider()
    
    /**
     * 示例1：检查系统状态
     */
    fun checkSystemStatus() {
        Log.d(TAG, "=== 系统状态检查 ===")
        
        // 检查补全系统是否可用
        val isAvailable = completionManager.isAvailable()
        Log.d(TAG, "补全系统可用: $isAvailable")
        
        // 获取支持的语言列表
        val supportedLanguages = completionManager.getSupportedLanguages()
        Log.d(TAG, "支持的语言: $supportedLanguages")
        
        // 检查各语言状态
        supportedLanguages.forEach { language ->
            val status = completionManager.getLanguageStatus(language)
            Log.d(TAG, "$language 状态: $status")
        }
        
        // 获取完整系统信息
        val systemInfo = modernProvider.getSystemInfo()
        Log.d(TAG, "系统信息: $systemInfo")
    }
    
    /**
     * 示例2：使用现代化补全提供器
     */
    fun useModernCompletionProvider(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher
    ) {
        Log.d(TAG, "=== 使用现代化补全提供器 ===")
        
        // 方式1：自动检测语言
        modernProvider.requireAutoComplete(content, position, publisher, android.os.Bundle())
        
        // 方式2：显式指定语言
        modernProvider.requireAutoComplete(content, position, publisher, "cpp")
        modernProvider.requireAutoComplete(content, position, publisher, "java")
        modernProvider.requireAutoComplete(content, position, publisher, "python")
    }
    
    /**
     * 示例3：直接使用补全管理器
     */
    fun useCompletionManagerDirectly(
        content: ContentReference,
        position: CharPosition
    ) {
        Log.d(TAG, "=== 直接使用补全管理器 ===")
        
        // 获取C++补全建议
        val cppCompletions = completionManager.getCompletions(content, position, "cpp")
        Log.d(TAG, "C++补全项数量: ${cppCompletions.size}")
        cppCompletions.take(5).forEach { item ->
            Log.d(TAG, "  - ${item.label}: ${item.desc}")
        }
        
        // 获取Java补全建议（基础实现）
        val javaCompletions = completionManager.getCompletions(content, position, "java")
        Log.d(TAG, "Java补全项数量: ${javaCompletions.size}")
        javaCompletions.take(5).forEach { item ->
            Log.d(TAG, "  - ${item.label}: ${item.desc}")
        }
        
        // 获取Python补全建议（基础实现）
        val pythonCompletions = completionManager.getCompletions(content, position, "python")
        Log.d(TAG, "Python补全项数量: ${pythonCompletions.size}")
        pythonCompletions.take(5).forEach { item ->
            Log.d(TAG, "  - ${item.label}: ${item.desc}")
        }
    }
    
    /**
     * 示例4：语言检测和切换
     */
    fun demonstrateLanguageDetection() {
        Log.d(TAG, "=== 语言检测示例 ===")
        
        val testFiles = mapOf(
            "main.cpp" to "cpp",
            "HelloWorld.java" to "java",
            "script.py" to "python",
            "unknown.txt" to "cpp" // 默认
        )
        
        testFiles.forEach { (filename, expectedLanguage) ->
            val detectedLanguage = completionManager.detectLanguageFromExtension(filename)
            val isCorrect = detectedLanguage == expectedLanguage
            Log.d(TAG, "$filename -> $detectedLanguage (${if (isCorrect) "✓" else "✗"})")
        }
    }
    
    /**
     * 示例5：成员补全
     */
    fun demonstrateMemberCompletion(content: ContentReference) {
        Log.d(TAG, "=== 成员补全示例 ===")
        
        // 模拟不同类型的成员补全
        val memberTests = listOf(
            Triple("cpp", "vector_obj", ""),  // vector成员
            Triple("cpp", "my_struct", ""),   // 用户定义struct成员
            Triple("java", "string_obj", ""), // String成员（基础实现）
            Triple("python", "list_obj", "")  // list成员（基础实现）
        )
        
        memberTests.forEach { (language, contextVar, prefix) ->
            val members = completionManager.getMemberCompletions(content, CharPosition(10, 5), contextVar, prefix, language)
            Log.d(TAG, "$language.$contextVar 成员数量: ${members.size}")
            members.take(3).forEach { item ->
                Log.d(TAG, "  - ${item.label}: ${item.desc}")
            }
        }
    }
    
    /**
     * 示例6：性能测试
     */
    fun performanceTest(content: ContentReference, position: CharPosition) {
        Log.d(TAG, "=== 性能测试 ===")
        
        val languages = listOf("cpp", "java", "python")
        val testCount = 10
        
        languages.forEach { language ->
            val startTime = System.currentTimeMillis()
            
            repeat(testCount) {
                completionManager.getCompletions(content, position, language)
            }
            
            val endTime = System.currentTimeMillis()
            val avgTime = (endTime - startTime) / testCount.toDouble()
            
            Log.d(TAG, "$language 平均补全时间: ${avgTime}ms")
        }
    }
    
    /**
     * 示例7：错误处理和降级
     */
    fun demonstrateErrorHandling(content: ContentReference, position: CharPosition) {
        Log.d(TAG, "=== 错误处理和降级 ===")
        
        // 测试不支持的语言
        try {
            val unsupportedCompletions = completionManager.getCompletions(content, position, "unsupported")
            Log.d(TAG, "不支持的语言返回了 ${unsupportedCompletions.size} 个补全项")
        } catch (e: Exception) {
            Log.w(TAG, "不支持的语言处理异常: ${e.message}")
        }
        
        // 测试系统重新初始化
        Log.d(TAG, "重新初始化补全系统...")
        completionManager.reinitialize()
        Log.d(TAG, "重新初始化完成")
    }
    
    /**
     * 完整的演示流程
     */
    fun runCompleteDemo(content: ContentReference, position: CharPosition, publisher: CompletionPublisher) {
        Log.d(TAG, "开始多语言补全框架完整演示")
        
        checkSystemStatus()
        demonstrateLanguageDetection()
        useModernCompletionProvider(content, position, publisher)
        useCompletionManagerDirectly(content, position)
        demonstrateMemberCompletion(content)
        performanceTest(content, position)
        demonstrateErrorHandling(content, position)
        
        Log.d(TAG, "多语言补全框架演示完成")
    }
}