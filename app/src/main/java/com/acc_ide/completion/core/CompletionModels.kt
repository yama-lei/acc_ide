package com.acc_ide.completion.core

/**
 * 补全相关的数据模型
 * 包含符号信息、作用域信息、解析结果等数据类
 */

/**
 * 符号信息数据类
 */
data class SymbolInfo(
    val name: String,
    val type: SymbolType,
    val dataType: String,
    val line: Int,
    val column: Int,
    val scopeLevel: Int,
    val description: String = ""
)

/**
 * 作用域信息数据类
 */
data class ScopeInfo(
    val level: Int,
    val startLine: Int,
    val endLine: Int,
    val type: ScopeType
)

/**
 * 符号类型枚举
 */
enum class SymbolType {
    VARIABLE, FUNCTION, CLASS, STRUCT, ENUM, PARAMETER
}

/**
 * 作用域类型枚举
 */
enum class ScopeType {
    FUNCTION, CLASS, BLOCK, NAMESPACE, GLOBAL
}

/**
 * 结构体信息数据类
 */
data class StructInfo(
    val name: String,
    val members: List<StructMember>,
    val line: Int
)

/**
 * 结构体成员数据类
 */
data class StructMember(
    val name: String,
    val type: String,
    val line: Int
)

/**
 * TreeSitter解析结果数据类
 */
data class TreeSitterResult(
    val rootNode: TreeSitterNode,
    val symbols: List<SymbolInfo>,
    val scopes: List<ScopeInfo>
)

/**
 * TreeSitter节点数据类
 */
data class TreeSitterNode(
    val type: String,
    val startPosition: Pair<Int, Int>,
    val endPosition: Pair<Int, Int>,
    val children: List<TreeSitterNode> = emptyList()
)

/**
 * 解析结果数据类（原生TreeSitter使用）
 */
data class ParseResult(
    val symbols: List<SymbolInfo>,
    val scopes: List<ScopeInfo>,
    val parseTree: String? = null // 可选的语法树文本表示
)

// 移除自定义ContentReference，统一使用sora-editor的类型
// import io.github.rosemoe.sora.text.ContentReference