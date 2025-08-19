package com.acc_ide.completion.framework

import android.util.Log
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.text.CharPosition
import com.acc_ide.completion.core.*
import com.acc_ide.completion.services.TreeSitterService

/**
 * 抽象TreeSitter处理器
 * 
 * 为所有使用TreeSitter的语言处理器提供基础实现，
 * 正确处理Content和ContentReference的创建和管理。
 */
abstract class AbstractTreeSitterProcessor : LanguageProcessor() {
    
    private val TAG = "AbstractTreeSitterProcessor"
    
    protected val treeSitterService = TreeSitterService()
    
    /**
     * 从字符串代码创建ContentReference
     * 使用sora-editor的实际API
     */
    protected fun createContentReference(code: String): ContentReference {
        return try {
            // 使用sora-editor的Content类创建内容
            val content = Content(code)
            ContentReference(content)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create ContentReference", e)
            // 返回空内容的ContentReference作为降级
            ContentReference(Content(""))
        }
    }
    
    /**
     * 从字符串代码解析并返回解析结果
     * 
     * 这个方法解决了语言处理器parseCode()方法的问题：
     * 将字符串转换为ContentReference，然后使用TreeSitter解析
     */
    override fun parseCode(code: String): ParseResult? {
        return try {
            Log.d(TAG, "Parsing ${getLanguageId()} code with TreeSitter")
            
            // 创建ContentReference
            val contentRef = createContentReference(code)
            
            // 使用TreeSitter解析
            val symbols = treeSitterService.getSymbolsAtPosition(
                contentRef, 
                getLanguageId(), 
                0, 
                0
            )
            
            // 创建ParseResult
            if (symbols.isNotEmpty()) {
                ParseResult(symbols, emptyList())
            } else {
                Log.d(TAG, "No symbols found in ${getLanguageId()} code")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse ${getLanguageId()} code with TreeSitter", e)
            null
        }
    }
    
    /**
     * 使用TreeSitter获取指定位置的符号信息
     */
    override fun getSymbolsAtPosition(
        contentRef: ContentReference,
        line: Int,
        column: Int
    ): List<SymbolInfo> {
        return try {
            treeSitterService.getSymbolsAtPosition(contentRef, getLanguageId(), line, column)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get symbols at position for ${getLanguageId()}", e)
            emptyList()
        }
    }
    
    /**
     * 检查TreeSitter处理器是否可用
     */
    override fun isAvailable(): Boolean {
        return treeSitterService.isAvailable()
    }
    
    /**
     * 获取TreeSitter支持的语言特定符号
     * 
     * 子类可以重写此方法来提供语言特定的符号获取逻辑
     */
    protected open fun getLanguageSpecificSymbols(
        contentRef: ContentReference,
        position: CharPosition,
        prefix: String
    ): List<SymbolInfo> {
        return try {
            treeSitterService.getSymbolsAtPosition(
                contentRef, 
                getLanguageId(), 
                position.line, 
                position.column
            ).filter { symbol ->
                symbol.name.startsWith(prefix, ignoreCase = true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get language-specific symbols for ${getLanguageId()}", e)
            emptyList()
        }
    }
    
    
    /**
     * 获取作用域内的符号
     * 
     * 使用TreeSitter的作用域分析来获取当前作用域内可见的符号
     */
    protected open fun getScopeSymbols(
        contentRef: ContentReference,
        position: CharPosition
    ): List<SymbolInfo> {
        return try {
            val symbols = treeSitterService.getSymbolsAtPosition(
                contentRef, 
                getLanguageId(), 
                position.line, 
                position.column
            )
            
            // TreeSitter服务已经处理了作用域过滤
            symbols
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get scope symbols for ${getLanguageId()}", e)
            emptyList()
        }
    }
    
    
}