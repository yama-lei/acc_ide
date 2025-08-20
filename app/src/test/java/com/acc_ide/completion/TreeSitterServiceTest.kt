package com.acc_ide.completion

import io.github.rosemoe.sora.text.Content
import org.junit.Test
import org.junit.Assert.*

class TreeSitterServiceTest {

    @Test
    fun testTreeSitterServiceInitialization() {
        // Test that TreeSitterService can be instantiated without crashing
        val service = TreeSitterService()
        assertNotNull(service)
    }

    @Test
    fun testLanguageSpecCreation() {
        // Test that language spec creation works (even if it returns null due to missing .so files)
        val service = TreeSitterService()
        
        // This should not throw an exception, even if it returns null
        val cppSpec = service.createLanguageSpec("cpp")
        // Currently expected to be null since .so files are not loaded
        assertNull(cppSpec)
        
        val javaSpec = service.createLanguageSpec("java")
        assertNull(javaSpec)
        
        val pythonSpec = service.createLanguageSpec("python")
        assertNull(pythonSpec)
    }

    @Test
    fun testParseCodeWithNullLanguageSpec() {
        // Test that parseCode handles null language spec gracefully
        val service = TreeSitterService()
        val content = Content("int main() { return 0; }")
        
        // This should not throw an exception
        val result = service.parseCode(content, "cpp")
        assertNull(result) // Expected to be null since language spec is null
    }

    @Test
    fun testGetScopeAtPositionWithNullResult() {
        // Test that getScopeAtPosition handles null result gracefully
        val service = TreeSitterService()
        val content = Content("int main() { return 0; }")
        
        // This should not throw an exception
        val scope = service.getScopeAtPosition(content, "cpp", 0, 0)
        assertNull(scope) // Expected to be null since parseCode returns null
    }

    @Test
    fun testGetLocalVariablesWithNullResult() {
        // Test that getLocalVariables handles null result gracefully
        val service = TreeSitterService()
        val content = Content("int main() { return 0; }")
        
        // This should not throw an exception
        val variables = service.getLocalVariables(content, "cpp")
        assertTrue(variables.isEmpty()) // Expected to be empty since parseCode returns null
    }

    @Test
    fun testGetFunctionDefinitionsWithNullResult() {
        // Test that getFunctionDefinitions handles null result gracefully
        val service = TreeSitterService()
        val content = Content("int main() { return 0; }")
        
        // This should not throw an exception
        val functions = service.getFunctionDefinitions(content, "cpp")
        assertTrue(functions.isEmpty()) // Expected to be empty since parseCode returns null
    }
}