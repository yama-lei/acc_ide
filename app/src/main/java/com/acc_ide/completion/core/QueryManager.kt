package com.acc_ide.completion.core

import io.github.rosemoe.sora.text.ContentReference
import com.acc_ide.completion.services.TreeSitterInterface

/**
 * TreeSitter 查询管理器
 * 提供预定义的查询模式用于代码分析和自动补全
 */
class QueryManager(private val treeSitterService: TreeSitterInterface) {
    
    companion object {
        /**
         * C++ 查询模式
         */
        object CppQueries {
            
            // 基于官方 tags.scm - 查找函数定义
            val FUNCTION_DEFINITIONS = """
                (function_declarator declarator: (identifier) @name) @definition.function
            """.trimIndent()
            
            // 查找方法定义（成员函数）
            val METHOD_DEFINITIONS = """
                (function_declarator declarator: (field_identifier) @name) @definition.function
            """.trimIndent()
            
            // 查找有命名空间的方法定义
            val NAMESPACED_METHOD_DEFINITIONS = """
                (function_declarator 
                  declarator: (qualified_identifier 
                    scope: (namespace_identifier) @local.scope 
                    name: (identifier) @name)) @definition.method
            """.trimIndent()
            
            // 基于官方 tags.scm - 查找结构体定义
            val STRUCT_DEFINITIONS = """
                (struct_specifier name: (type_identifier) @name body:(_)) @definition.class
            """.trimIndent()
            
            // 查找类定义
            val CLASS_DEFINITIONS = """
                (class_specifier name: (type_identifier) @name) @definition.class
            """.trimIndent()
            
            // 查找联合体定义
            val UNION_DEFINITIONS = """
                (declaration type: (union_specifier name: (type_identifier) @name)) @definition.class
            """.trimIndent()
            
            // 查找枚举定义
            val ENUM_DEFINITIONS = """
                (enum_specifier name: (type_identifier) @name) @definition.type
            """.trimIndent()
            
            // 查找类型定义
            val TYPE_DEFINITIONS = """
                (type_definition declarator: (type_identifier) @name) @definition.type
            """.trimIndent()
            
            // 基于官方 highlights.scm - 查找函数调用
            val FUNCTION_CALLS = """
                (call_expression
                  function: (qualified_identifier
                    name: (identifier) @function))
            """.trimIndent()
            
            // 查找模板函数
            val TEMPLATE_FUNCTIONS = """
                (template_function
                  name: (identifier) @function)
            """.trimIndent()
            
            // 查找模板方法
            val TEMPLATE_METHODS = """
                (template_method
                  name: (field_identifier) @function)
            """.trimIndent()
            
            // 查找变量声明（更精确的模式）
            val VARIABLE_DECLARATIONS = """
                (declaration
                  declarator: (identifier) @variable.name) @variable.declaration
            """.trimIndent()
            
            // 查找初始化声明器
            val INIT_DECLARATORS = """
                (declaration
                  declarator: (init_declarator
                    declarator: (identifier) @variable.name)) @variable.declaration
            """.trimIndent()
            
            // 查找参数声明
            val PARAMETER_DECLARATIONS = """
                (parameter_declaration
                  declarator: (identifier) @parameter.name) @parameter.declaration
            """.trimIndent()
            
            // 查找成员访问表达式（. 操作符）
            val MEMBER_ACCESS = """
                (field_expression
                  argument: (identifier) @object.name
                  field: (field_identifier) @field.name) @member.access
            """.trimIndent()
            
            // 查找指针成员访问表达式（-> 操作符）
            val POINTER_MEMBER_ACCESS = """
                (field_expression
                  argument: (identifier) @pointer.name
                  field: (field_identifier) @field.name) @pointer.access
            """.trimIndent()
            
            // 查找所有标识符（用于自动补全）
            val ALL_IDENTIFIERS = """
                (identifier) @identifier
            """.trimIndent()
            
            // 查找所有字段标识符
            val ALL_FIELD_IDENTIFIERS = """
                (field_identifier) @field.identifier
            """.trimIndent()
            
            // 查找所有类型标识符
            val ALL_TYPE_IDENTIFIERS = """
                (type_identifier) @type.identifier
            """.trimIndent()
            
            // 查找 this 引用
            val THIS_REFERENCES = """
                (this) @variable.builtin
            """.trimIndent()
            
            // 查找 nullptr 常量
            val NULLPTR_CONSTANTS = """
                (null "nullptr" @constant)
            """.trimIndent()
        }
    }
    
    /**
     * 查找函数定义
     */
    fun findFunctionDefinitions(contentRef: ContentReference, language: String): List<QueryMatch> {
        val results = mutableListOf<QueryMatch>()
        
        // 查找普通函数定义
        val functions = treeSitterService.executeQuery(contentRef, language, CppQueries.FUNCTION_DEFINITIONS)
        results.addAll(functions?.matches ?: emptyList())
        
        // 查找方法定义
        val methods = treeSitterService.executeQuery(contentRef, language, CppQueries.METHOD_DEFINITIONS)
        results.addAll(methods?.matches ?: emptyList())
        
        // 查找命名空间方法定义
        val namespacedMethods = treeSitterService.executeQuery(contentRef, language, CppQueries.NAMESPACED_METHOD_DEFINITIONS)
        results.addAll(namespacedMethods?.matches ?: emptyList())
        
        // 查找模板函数
        val templateFunctions = treeSitterService.executeQuery(contentRef, language, CppQueries.TEMPLATE_FUNCTIONS)
        results.addAll(templateFunctions?.matches ?: emptyList())
        
        return results
    }
    
    /**
     * 查找变量声明
     */
    fun findVariableDeclarations(contentRef: ContentReference, language: String): List<QueryMatch> {
        val results = mutableListOf<QueryMatch>()
        
        // 查找普通变量声明
        val variables = treeSitterService.executeQuery(contentRef, language, CppQueries.VARIABLE_DECLARATIONS)
        results.addAll(variables?.matches ?: emptyList())
        
        // 查找初始化声明器
        val initDeclarators = treeSitterService.executeQuery(contentRef, language, CppQueries.INIT_DECLARATORS)
        results.addAll(initDeclarators?.matches ?: emptyList())
        
        // 查找参数声明
        val parameters = treeSitterService.executeQuery(contentRef, language, CppQueries.PARAMETER_DECLARATIONS)
        results.addAll(parameters?.matches ?: emptyList())
        
        return results
    }
    
    /**
     * 查找结构体定义
     */
    fun findStructDefinitions(contentRef: ContentReference, language: String): List<QueryMatch> {
        val result = treeSitterService.executeQuery(contentRef, language, CppQueries.STRUCT_DEFINITIONS)
        return result?.matches ?: emptyList()
    }
    
    /**
     * 查找类定义
     */
    fun findClassDefinitions(contentRef: ContentReference, language: String): List<QueryMatch> {
        val result = treeSitterService.executeQuery(contentRef, language, CppQueries.CLASS_DEFINITIONS)
        return result?.matches ?: emptyList()
    }
    
    /**
     * 查找所有类型定义（结构体、类、联合体、枚举、typedef）
     */
    fun findAllTypeDefinitions(contentRef: ContentReference, language: String): List<QueryMatch> {
        val results = mutableListOf<QueryMatch>()
        
        results.addAll(findStructDefinitions(contentRef, language))
        results.addAll(findClassDefinitions(contentRef, language))
        
        val unions = treeSitterService.executeQuery(contentRef, language, CppQueries.UNION_DEFINITIONS)
        results.addAll(unions?.matches ?: emptyList())
        
        val enums = treeSitterService.executeQuery(contentRef, language, CppQueries.ENUM_DEFINITIONS)
        results.addAll(enums?.matches ?: emptyList())
        
        val typedefs = treeSitterService.executeQuery(contentRef, language, CppQueries.TYPE_DEFINITIONS)
        results.addAll(typedefs?.matches ?: emptyList())
        
        return results
    }
    
    /**
     * 查找所有标识符（用于自动补全）
     */
    fun findAllIdentifiers(contentRef: ContentReference, language: String): List<QueryMatch> {
        val result = treeSitterService.executeQuery(contentRef, language, CppQueries.ALL_IDENTIFIERS)
        return result?.matches ?: emptyList()
    }
    
    /**
     * 查找成员访问表达式
     */
    fun findMemberAccess(contentRef: ContentReference, language: String): List<QueryMatch> {
        val result = treeSitterService.executeQuery(contentRef, language, CppQueries.MEMBER_ACCESS)
        return result?.matches ?: emptyList()
    }
    
    /**
     * 查找指针成员访问表达式
     */
    fun findPointerMemberAccess(contentRef: ContentReference, language: String): List<QueryMatch> {
        val result = treeSitterService.executeQuery(contentRef, language, CppQueries.POINTER_MEMBER_ACCESS)
        return result?.matches ?: emptyList()
    }
    
    /**
     * 执行自定义查询
     */
    fun executeCustomQuery(contentRef: ContentReference, language: String, query: String): QueryResult? {
        return treeSitterService.executeQuery(contentRef, language, query)
    }
    
    /**
     * 获取用于自动补全的符号列表
     */
    fun getCompletionSymbols(contentRef: ContentReference, language: String): List<CompletionSymbol> {
        val symbols = mutableListOf<CompletionSymbol>()
        
        // 获取函数定义
        val functions = findFunctionDefinitions(contentRef, language)
        functions.filter { it.captureName == "name" }.forEach { match ->
            symbols.add(CompletionSymbol(
                name = match.nodeText,
                type = "function",
                detail = "Function definition",
                line = match.startLine,
                column = match.startColumn
            ))
        }
        
        // 获取变量声明
        val variables = findVariableDeclarations(contentRef, language)
        variables.filter { it.captureName == "variable.name" || it.captureName == "parameter.name" }.forEach { match ->
            val symbolType = if (match.captureName == "parameter.name") "parameter" else "variable"
            symbols.add(CompletionSymbol(
                name = match.nodeText,
                type = symbolType,
                detail = "${symbolType.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} declaration",
                line = match.startLine,
                column = match.startColumn
            ))
        }
        
        // 获取所有类型定义
        val types = findAllTypeDefinitions(contentRef, language)
        types.filter { it.captureName == "name" }.forEach { match ->
            val symbolType = when {
                match.nodeType.contains("struct") -> "struct"
                match.nodeType.contains("class") -> "class"
                match.nodeType.contains("enum") -> "enum"
                match.nodeType.contains("union") -> "union"
                else -> "type"
            }
            symbols.add(CompletionSymbol(
                name = match.nodeText,
                type = symbolType,
                detail = "${symbolType.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }} definition",
                line = match.startLine,
                column = match.startColumn
            ))
        }
        
        // 获取函数调用（用于识别常用函数）
        val functionCalls = treeSitterService.executeQuery(contentRef, language, CppQueries.FUNCTION_CALLS)
        functionCalls?.matches?.filter { it.captureName == "function" }?.forEach { match ->
            // 只添加不重复的函数调用
            if (symbols.none { it.name == match.nodeText && it.type == "function" }) {
                symbols.add(CompletionSymbol(
                    name = match.nodeText,
                    type = "function",
                    detail = "Function call",
                    line = match.startLine,
                    column = match.startColumn
                ))
            }
        }
        
        return symbols.distinctBy { "${it.name}_${it.type}" } // 去重
    }
    
    /**
     * 获取当前位置的上下文信息（用于智能补全）
     */
    fun getContextAtPosition(contentRef: ContentReference, language: String, line: Int, column: Int): CompletionContext {
        // 查找成员访问表达式
        val memberAccess = findMemberAccess(contentRef, language)
        val pointerAccess = findPointerMemberAccess(contentRef, language)
        
        // 查找最接近当前位置的访问表达式
        val nearestAccess = (memberAccess + pointerAccess)
            .filter { it.startLine <= line && (it.startLine < line || it.startColumn <= column) }
            .maxByOrNull { it.startLine * 1000 + it.startColumn }
        
        return CompletionContext(
            isInMemberAccess = nearestAccess != null,
            memberAccessObject = nearestAccess?.let { match ->
                when (match.captureName) {
                    "object.name", "pointer.name" -> match.nodeText
                    else -> null
                }
            },
            accessType = when (nearestAccess?.captureName) {
                "object.name" -> AccessType.DOT
                "pointer.name" -> AccessType.ARROW
                else -> AccessType.NONE
            }
        )
    }
}

/**
 * 补全符号数据类
 */
data class CompletionSymbol(
    val name: String,
    val type: String,
    val detail: String,
    val line: Int,
    val column: Int
)

/**
 * 成员访问类型枚举
 */
enum class AccessType {
    NONE,    // 无成员访问
    DOT,     // . 操作符
    ARROW    // -> 操作符
}

/**
 * 补全上下文数据类
 */
data class CompletionContext(
    val isInMemberAccess: Boolean,
    val memberAccessObject: String?,
    val accessType: AccessType
)