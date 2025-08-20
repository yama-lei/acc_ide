package com.acc_ide.completion

import org.junit.Test
import org.junit.Assert.*

/**
 * 测试TreeSitter集成的ACM补全提供器
 */
class ACMCompletionProviderTest {
    
    private val provider = ACMCompletionProvider()
    
    @Test
    fun testKeywordProviderCreation() {
        val keywordProvider = KeywordProvider()
        assertNotNull("KeywordProvider should be created successfully", keywordProvider)
    }
    
    @Test
    fun testSTLProviderCreation() {
        val stlProvider = STLProvider()
        assertNotNull("STLProvider should be created successfully", stlProvider)
    }
    
    @Test
    fun testTreeSitterServiceCreation() {
        val service = TreeSitterService()
        assertNotNull("TreeSitterService should be created successfully", service)
    }
    
    @Test
    fun testBasicFunctionality() {
        // 测试基本模块创建和初始化
        val keywordProvider = KeywordProvider()
        val stlProvider = STLProvider()
        val treeSitterService = TreeSitterService()
        
        assertNotNull("KeywordProvider should not be null", keywordProvider)
        assertNotNull("STLProvider should not be null", stlProvider)
        assertNotNull("TreeSitterService should not be null", treeSitterService)
        
        // 测试优先级常量
        assertEquals("PRIORITY_LOCAL_VARIABLE should be 200", 200, ACMCompletionProvider.PRIORITY_LOCAL_VARIABLE)
        assertEquals("PRIORITY_KEYWORD should be 100", 100, ACMCompletionProvider.PRIORITY_KEYWORD)
    }
    
    @Test
    fun testSymbolInfoCreation() {
        val symbolInfo = SymbolInfo(
            name = "testVar",
            type = SymbolType.VARIABLE,
            dataType = "int",
            line = 10,
            column = 5,
            scopeLevel = 1,
            description = "Test variable"
        )
        
        assertEquals("Name should match", "testVar", symbolInfo.name)
        assertEquals("Type should match", SymbolType.VARIABLE, symbolInfo.type)
        assertEquals("Data type should match", "int", symbolInfo.dataType)
        assertEquals("Line should match", 10, symbolInfo.line)
        assertEquals("Scope level should match", 1, symbolInfo.scopeLevel)
    }
    
    @Test
    fun testScopeInfoCreation() {
        val scopeInfo = ScopeInfo(
            level = 2,
            startLine = 5,
            endLine = 15,
            type = ScopeType.FUNCTION
        )
        
        assertEquals("Level should match", 2, scopeInfo.level)
        assertEquals("Start line should match", 5, scopeInfo.startLine)
        assertEquals("End line should match", 15, scopeInfo.endLine)
        assertEquals("Type should match", ScopeType.FUNCTION, scopeInfo.type)
    }
    
    @Test
    fun testSTLProviderMemberCompletions() {
        val stlProvider = STLProvider()
        
        // Test vector member completions
        val vectorItems = stlProvider.getMemberCompletions("vector", "pu")
        assertTrue("Should have push_back completion", 
            vectorItems.any { it.label.toString() == "push_back" })
        
        // Test string member completions
        val stringItems = stlProvider.getMemberCompletions("string", "si")
        assertTrue("Should have size completion", 
            stringItems.any { it.label.toString() == "size" })
    }
    
    @Test
    fun testSTLProviderInferMemberCompletions() {
        val stlProvider = STLProvider()
        
        // Test inference based on variable name
        val vecItems = stlProvider.inferMemberCompletions("vec", "pu")
        assertTrue("Should infer vector type and provide push_back", 
            vecItems.any { it.label.toString() == "push_back" })
        
        val strItems = stlProvider.inferMemberCompletions("str", "si")
        assertTrue("Should infer string type and provide size", 
            strItems.any { it.label.toString() == "size" })
        
        val mapItems = stlProvider.inferMemberCompletions("mp", "fi")
        assertTrue("Should infer map type and provide find", 
            mapItems.any { it.label.toString() == "find" })
    }
    
    @Test
    fun testKeywordProviderCppKeywords() {
        val keywordProvider = KeywordProvider()
        val cppKeywords = keywordProvider.getKeywordCompletions("in", "cpp")
        
        assertTrue("Should have int keyword", 
            cppKeywords.any { it.label.toString() == "int" })
        assertTrue("Should have include keyword", 
            cppKeywords.any { it.label.toString() == "#include" })
    }
}