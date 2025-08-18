package com.acc_ide.completion.services

import io.github.rosemoe.sora.text.ContentReference
import com.acc_ide.completion.core.*

/**
 * TreeSitter服务接口
 * 定义TreeSitter语法分析的统一接口
 */
interface TreeSitterInterface {
    
    /**
     * 解析代码并返回语法分析结果
     */
    fun parseCode(contentRef: ContentReference, language: String): ParseResult?
    
    /**
     * 获取指定位置的符号信息
     */
    fun getSymbolsAtPosition(contentRef: ContentReference, language: String, line: Int, column: Int): List<SymbolInfo>
    
    /**
     * 获取局部变量
     */
    fun getLocalVariables(contentRef: ContentReference, language: String, line: Int, column: Int): List<SymbolInfo>
    
    /**
     * 获取函数定义
     */
    fun getFunctionDefinitions(contentRef: ContentReference, language: String): List<SymbolInfo>
    
    /**
     * 获取类定义
     */
    fun getClassDefinitions(contentRef: ContentReference, language: String): List<SymbolInfo>
    
    /**
     * 检查TreeSitter是否可用
     */
    fun isAvailable(): Boolean
    
    /**
     * 获取支持的语言列表
     */
    fun getSupportedLanguages(): List<String>
    
    /**
     * 执行TreeSitter查询
     */
    fun executeQuery(contentRef: ContentReference, language: String, query: String): QueryResult?
}

/**
 * TreeSitter语言规范
 * 用于替代AndroidIDE的TsLanguageSpec
 */
data class TsLanguageSpec(
    val languageName: String,
    val scopeName: String,
    val fileExtensions: List<String>,
    val treeSitterLibrary: String? = null
)

/**
 * TreeSitter解析结果
 * 用于替代AndroidIDE的TreeSitterResult
 */
data class TreeSitterResult(
    val rootNode: TreeSitterNode,
    val symbols: List<SymbolInfo>,
    val scopes: List<ScopeInfo>,
    val parseTree: String? = null
)

/**
 * TreeSitter节点表示
 */
data class TreeSitterNode(
    val type: String,
    val text: String,
    val startPosition: Pair<Int, Int>,
    val endPosition: Pair<Int, Int>,
    val children: List<TreeSitterNode> = emptyList()
)