package com.acc_ide.completion.framework

import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import com.acc_ide.completion.core.SymbolInfo
import com.acc_ide.completion.core.ParseResult

/**
 * 语言处理器抽象基类
 * 
 * 每种语言继承此类实现特定的解析和补全逻辑
 */
abstract class LanguageProcessor {
    
    /**
     * 语言标识符（如 "cpp", "java", "python"）
     */
    abstract fun getLanguageId(): String
    
    /**
     * 语言显示名称
     */
    abstract fun getLanguageName(): String
    
    /**
     * 支持的文件扩展名
     */
    abstract fun getSupportedExtensions(): Set<String>
    
    /**
     * 解析代码获取符号信息
     */
    abstract fun parseCode(code: String): ParseResult?
    
    /**
     * 获取指定位置的可见符号
     */
    abstract fun getSymbolsAtPosition(
        contentRef: ContentReference,
        line: Int,
        column: Int
    ): List<SymbolInfo>
    
    /**
     * 提供常规补全建议
     */
    abstract fun provideRegularCompletions(
        contentRef: ContentReference,
        position: CharPosition,
        prefix: String
    ): List<CompletionItem>
    
    /**
     * 提供成员访问补全
     */
    abstract fun provideMemberCompletions(
        contentRef: ContentReference,
        position: CharPosition,
        contextVar: String,
        prefix: String
    ): List<CompletionItem>
    
    /**
     * 获取语言关键字
     */
    abstract fun getKeywords(): Set<String>
    
    /**
     * 获取标准库类型/函数
     */
    abstract fun getStandardLibraryItems(): Map<String, List<String>>
    
    /**
     * 判断是否为基本类型
     */
    abstract fun isPrimitiveType(dataType: String): Boolean
    
    /**
     * 判断是否为标准库类型
     */
    abstract fun isStandardLibraryType(dataType: String): Boolean
    
    /**
     * 提取成员访问上下文
     */
    open fun extractMemberAccessContext(
        contentRef: ContentReference,
        position: CharPosition,
        prefix: String
    ): Pair<String, String?> {
        return try {
            val line = contentRef.getLine(position.line)
            val beforeCursor = line.substring(0, position.column)
            
            // 检查点号操作符 (.)
            val lastDotIndex = beforeCursor.lastIndexOf('.')
            // 检查箭头操作符 (->)
            val lastArrowIndex = beforeCursor.lastIndexOf("->")
            
            // 选择最靠近光标的操作符
            val (operatorIndex, operatorLength) = when {
                lastDotIndex > lastArrowIndex -> Pair(lastDotIndex, 1)
                lastArrowIndex >= 0 -> Pair(lastArrowIndex, 2)
                else -> Pair(-1, 0)
            }
            
            if (operatorIndex > 0) {
                val beforeOperator = beforeCursor.substring(0, operatorIndex).trim()
                val afterOperator = beforeCursor.substring(operatorIndex + operatorLength)
                
                // 提取变量名（简化实现）
                val words = beforeOperator.split(Regex("\\W+"))
                val varName = words.lastOrNull { it.isNotEmpty() && it[0].isLetter() }
                
                if (varName != null) {
                    return Pair(afterOperator, varName)
                }
            }
            
            Pair(prefix, null)
        } catch (e: Exception) {
            Pair(prefix, null)
        }
    }
    
    /**
     * 提取前缀
     */
    open fun extractPrefix(contentRef: ContentReference, position: CharPosition): String {
        return try {
            val line = contentRef.getLine(position.line)
            val beforeCursor = line.substring(0, minOf(position.column, line.length))
            
            // 提取当前单词作为前缀
            val match = Regex("""[a-zA-Z_][a-zA-Z0-9_]*$""").find(beforeCursor)
            match?.value ?: ""
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * 处理器是否可用
     */
    open fun isAvailable(): Boolean = true
}