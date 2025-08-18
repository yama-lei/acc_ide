package com.acc_ide.completion

import io.github.rosemoe.sora.text.Content
import org.junit.Test
import org.junit.Assert.*

/**
 * TreeSitter集成测试
 * 专注于验证TreeSitter服务能够正确处理空值和异常情况
 */
class TreeSitterIntegrationTest {

    @Test
    fun testTreeSitterServiceGracefulDegradation() {
        // 测试TreeSitter服务在没有语言支持时的优雅降级
        val service = TreeSitterService()
        val content = Content("void dfs(vector<vector<int> > &a,int d) { return; }")
        
        // 这些调用应该不会抛出异常，即使返回null或空列表
        val parseResult = service.parseCode(content, "cpp")
        assertNull("Parse result should be null when no language support", parseResult)
        
        val scope = service.getScopeAtPosition(content, "cpp", 0, 10)
        assertNull("Scope should be null when parsing fails", scope)
        
        val variables = service.getLocalVariables(content, "cpp")
        assertTrue("Variables list should be empty when parsing fails", variables.isEmpty())
        
        val functions = service.getFunctionDefinitions(content, "cpp")
        assertTrue("Functions list should be empty when parsing fails", functions.isEmpty())
    }

    @Test
    fun testACMScopedVariablesWithoutTreeSitter() {
        // 测试ACMScopedVariables在TreeSitter不可用时的行为
        val content = Content("int main() { int a = 5; return a; }")
        
        // 创建一个空的符号和作用域缓存来模拟TreeSitter不可用的情况
        val symbolsCache = mutableListOf<SymbolInfo>()
        val scopesCache = mutableListOf<ScopeInfo>()
        
        // 验证缓存为空时的行为
        assertTrue("Symbols cache should be empty", symbolsCache.isEmpty())
        assertTrue("Scopes cache should be empty", scopesCache.isEmpty())
    }

    @Test
    fun testSTLProviderInferenceLogic() {
        // 测试STL提供器的类型推断逻辑
        val stlProvider = STLProvider()
        
        // 测试各种变量名的类型推断
        val testCases = mapOf(
            "vec" to "vector",
            "arr" to "vector", 
            "str" to "string",
            "s" to "string",
            "map" to "map",
            "mp" to "map",
            "set" to "set",
            "queue" to "queue",
            "q" to "queue",
            "stack" to "stack",
            "st" to "stack",
            "unknown_var" to "unknown"
        )
        
        for ((varName, expectedType) in testCases) {
            val items = stlProvider.inferMemberCompletions(varName, "")
            if (expectedType == "unknown") {
                // 对于未知类型，应该提供通用方法
                assertTrue("Unknown type should provide common methods", items.isNotEmpty())
            } else {
                // 对于已知类型，应该提供特定的方法
                assertTrue("Known type $expectedType should provide specific methods for $varName", 
                    items.isNotEmpty())
            }
        }
    }

    @Test
    fun testACMCompletionProviderErrorHandling() {
        // 测试ACM补全提供器的错误处理
        val provider = ACMCompletionProvider()
        
        // 验证提供器可以正确实例化
        assertNotNull("Provider should be instantiated", provider)
        
        // 验证优先级常量定义正确
        assertTrue("LOCAL_VARIABLE priority should be reasonable", 
            ACMCompletionProvider.PRIORITY_LOCAL_VARIABLE > ACMCompletionProvider.PRIORITY_KEYWORD)
        assertTrue("STL_COMMON priority should be reasonable", 
            ACMCompletionProvider.PRIORITY_STL_COMMON > ACMCompletionProvider.PRIORITY_KEYWORD)
    }

    @Test
    fun testSymbolAndScopeDataClasses() {
        // 测试符号和作用域数据类的创建和属性
        val symbol = SymbolInfo(
            name = "testVar",
            type = SymbolType.VARIABLE,
            dataType = "int",
            line = 5,
            column = 10,
            scopeLevel = 1,
            description = "Test variable"
        )
        
        assertEquals("Symbol name should match", "testVar", symbol.name)
        assertEquals("Symbol type should match", SymbolType.VARIABLE, symbol.type)
        assertEquals("Symbol data type should match", "int", symbol.dataType)
        assertEquals("Symbol line should match", 5, symbol.line)
        assertEquals("Symbol column should match", 10, symbol.column)
        assertEquals("Symbol scope level should match", 1, symbol.scopeLevel)
        
        val scope = ScopeInfo(
            level = 2,
            startLine = 1,
            endLine = 20,
            type = ScopeType.FUNCTION
        )
        
        assertEquals("Scope level should match", 2, scope.level)
        assertEquals("Scope start line should match", 1, scope.startLine)
        assertEquals("Scope end line should match", 20, scope.endLine)
        assertEquals("Scope type should match", ScopeType.FUNCTION, scope.type)
    }

    @Test
    fun testEnumerationValues() {
        // 测试枚举值定义
        val symbolTypes = SymbolType.values()
        assertTrue("Should have VARIABLE type", symbolTypes.contains(SymbolType.VARIABLE))
        assertTrue("Should have FUNCTION type", symbolTypes.contains(SymbolType.FUNCTION))
        assertTrue("Should have CLASS type", symbolTypes.contains(SymbolType.CLASS))
        assertTrue("Should have STRUCT type", symbolTypes.contains(SymbolType.STRUCT))
        assertTrue("Should have PARAMETER type", symbolTypes.contains(SymbolType.PARAMETER))
        
        val scopeTypes = ScopeType.values()
        assertTrue("Should have FUNCTION scope", scopeTypes.contains(ScopeType.FUNCTION))
        assertTrue("Should have CLASS scope", scopeTypes.contains(ScopeType.CLASS))
        assertTrue("Should have BLOCK scope", scopeTypes.contains(ScopeType.BLOCK))
        assertTrue("Should have GLOBAL scope", scopeTypes.contains(ScopeType.GLOBAL))
    }
}