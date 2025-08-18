package com.acc_ide.completion.services

import android.util.Log
import io.github.rosemoe.sora.text.ContentReference
import com.acc_ide.completion.core.*

/**
 * TreeSitter服务管理器
 * 提供统一的TreeSitter接口，管理不同的实现
 */
class TreeSitterService : TreeSitterInterface {
    
    companion object {
        private const val TAG = "TreeSitterService"
        
        // 支持的语言配置
        private val LANGUAGE_SPECS = mapOf(
            "cpp" to TsLanguageSpec(
                languageName = "cpp",
                scopeName = "source.cpp",
                fileExtensions = listOf("cpp", "c++", "cc", "cxx", "h", "hpp"),
                treeSitterLibrary = "tree-sitter-cpp"
            ),
            "java" to TsLanguageSpec(
                languageName = "java", 
                scopeName = "source.java",
                fileExtensions = listOf("java"),
                treeSitterLibrary = "tree-sitter-java"
            ),
            "python" to TsLanguageSpec(
                languageName = "python",
                scopeName = "source.python", 
                fileExtensions = listOf("py", "python"),
                treeSitterLibrary = "tree-sitter-python"
            )
        )
    }
    
    private val nativeService: NativeTreeSitterService by lazy {
        NativeTreeSitterService()
    }
    
    override fun parseCode(contentRef: ContentReference, language: String): ParseResult? {
        return try {
            nativeService.parseCode(contentRef, language)
        } catch (e: Exception) {
            Log.w(TAG, "TreeSitter parsing failed, using fallback", e)
            null
        }
    }
    
    override fun getSymbolsAtPosition(contentRef: ContentReference, language: String, line: Int, column: Int): List<SymbolInfo> {
        return try {
            nativeService.getSymbolsAtPosition(contentRef, language, line, column)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get symbols at position", e)
            emptyList()
        }
    }
    
    override fun getLocalVariables(contentRef: ContentReference, language: String, line: Int, column: Int): List<SymbolInfo> {
        return try {
            nativeService.getLocalVariables(contentRef, language, line, column)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get local variables", e)
            emptyList()
        }
    }
    
    override fun getFunctionDefinitions(contentRef: ContentReference, language: String): List<SymbolInfo> {
        return try {
            nativeService.getFunctionDefinitions(contentRef, language)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get function definitions", e)
            emptyList()
        }
    }
    
    override fun getClassDefinitions(contentRef: ContentReference, language: String): List<SymbolInfo> {
        return try {
            nativeService.getClassDefinitions(contentRef, language)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get class definitions", e)
            emptyList()
        }
    }
    
    override fun isAvailable(): Boolean {
        return try {
            // 简单测试TreeSitter是否可用
            true // 目前总是返回true，因为有fallback实现
        } catch (e: Exception) {
            false
        }
    }
    
    override fun getSupportedLanguages(): List<String> {
        return LANGUAGE_SPECS.keys.toList()
    }
    
    /**
     * 创建语言规范
     */
    fun createLanguageSpec(language: String): TsLanguageSpec? {
        return LANGUAGE_SPECS[language.lowercase()]
    }
    
    /**
     * 根据文件扩展名获取语言规范
     */
    fun getLanguageSpecByExtension(extension: String): TsLanguageSpec? {
        return LANGUAGE_SPECS.values.find { spec ->
            spec.fileExtensions.any { it.equals(extension, ignoreCase = true) }
        }
    }
    
    /**
     * 根据作用域名获取语言规范
     */
    fun getLanguageSpecByScopeName(scopeName: String): TsLanguageSpec? {
        return LANGUAGE_SPECS.values.find { it.scopeName == scopeName }
    }
}