package com.acc_ide.completion.framework

import android.util.Log
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import com.acc_ide.completion.core.*

/**
 * 通用补全引擎实现
 * 
 * 管理多个语言处理器，提供统一的补全接口
 */
class UniversalCompletionEngine {
    
    private val TAG = "UniversalCompletionEngine"
    
    // 语言处理器注册表
    private val processors = mutableMapOf<String, LanguageProcessor>()
    private val usageFrequency = mutableMapOf<String, Int>()
    
    /**
     * 注册语言处理器
     */
    fun registerProcessor(processor: LanguageProcessor) {
        processors[processor.getLanguageId()] = processor
        Log.d(TAG, "Registered processor for language: ${processor.getLanguageId()}")
    }
    
    
    /**
     * 获取语言处理器
     */
    private fun getProcessor(language: String): LanguageProcessor? {
        return processors[language.lowercase()]
    }
    
    fun getSupportedLanguages(): Set<String> {
        return processors.keys.toSet()
    }
    
    fun isLanguageSupported(language: String): Boolean {
        return processors.containsKey(language.lowercase())
    }
    
    
    
    fun provideCompletions(
        contentRef: ContentReference,
        position: CharPosition,
        prefix: String,
        language: String
    ): List<CompletionItem> {
        val processor = getProcessor(language) ?: return emptyList()
        
        val items = mutableListOf<CompletionItem>()
        
        try {
            Log.d(TAG, "Providing completions for language: $language, prefix: '$prefix'")
            
            // 检查是否为成员访问
            val (actualPrefix, contextVar) = processor.extractMemberAccessContext(contentRef, position, prefix)
            
            if (contextVar != null) {
                // 成员访问补全
                items.addAll(processor.provideMemberCompletions(contentRef, position, contextVar, actualPrefix))
                Log.d(TAG, "Added ${items.size} member completions")
            } else {
                // 常规补全
                items.addAll(processor.provideRegularCompletions(contentRef, position, actualPrefix))
                Log.d(TAG, "Added ${items.size} regular completions")
            }
            
            // 更新使用频率
            updateUsageFrequency(prefix)
            
            // 排序并返回
            return sortCompletionItems(items, actualPrefix)
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to provide completions for language: $language", e)
            // 降级到关键字补全
            return getKeywordCompletions(prefix, language)
        }
    }
    
    fun provideMemberCompletions(
        contentRef: ContentReference,
        position: CharPosition,
        language: String,
        contextVar: String,
        prefix: String
    ): List<CompletionItem> {
        val processor = getProcessor(language) ?: return emptyList()
        
        return try {
            processor.provideMemberCompletions(contentRef, position, contextVar, prefix)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to provide member completions for language: $language", e)
            emptyList()
        }
    }
    
    fun getKeywordCompletions(prefix: String, language: String): List<CompletionItem> {
        val processor = getProcessor(language) ?: return emptyList()
        
        return processor.getKeywords()
            .filter { it.startsWith(prefix, ignoreCase = true) }
            .map { keyword ->
                PriorityCompletionItem(
                    keyword,
                    "Keyword",
                    prefix.length,
                    keyword,
                    CompletionConstants.PRIORITY_KEYWORD,
                    CompletionItemKind.Keyword
                )
            }
    }
    
    
    fun isAvailable(): Boolean {
        return processors.values.any { it.isAvailable() }
    }
    
    /**
     * 更新使用频率
     */
    private fun updateUsageFrequency(prefix: String) {
        if (prefix.isNotEmpty()) {
            usageFrequency[prefix] = (usageFrequency[prefix] ?: 0) + 1
        }
    }
    
    /**
     * 排序补全项
     */
    private fun sortCompletionItems(items: List<CompletionItem>, prefix: String): List<CompletionItem> {
        return items.sortedWith(
            compareByDescending<CompletionItem> { 
                (it as? PriorityCompletionItem)?.priority ?: 0
            }.thenBy { 
                calculateRelevanceScore(it.label.toString(), prefix) 
            }.thenByDescending {
                usageFrequency[it.label.toString()] ?: 0
            }.thenBy { 
                it.label.toString() 
            }
        )
    }
    
    /**
     * 计算相关性分数
     */
    private fun calculateRelevanceScore(suggestion: String, prefix: String): Int {
        return when {
            suggestion.equals(prefix, ignoreCase = true) -> 0
            suggestion.startsWith(prefix, ignoreCase = true) -> 1
            suggestion.contains(prefix, ignoreCase = true) -> 2
            else -> 3
        }
    }
    
    /**
     * 带优先级的补全项
     */
    class PriorityCompletionItem(
        label: CharSequence,
        desc: CharSequence,
        prefixLength: Int,
        commitText: String,
        val priority: Int,
        kind: CompletionItemKind
    ) : SimpleCompletionItem(label, desc, prefixLength, commitText) {
        
        init {
            this.kind(kind)
        }
    }
}