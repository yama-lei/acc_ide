/*******************************************************************************
 *    acc-ide - ACM Contest IDE for Android
 *    Copyright (C) 2024  Contributors
 *
 *     This program is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU General Public License
 *     as published by the Free Software Foundation; either
 *     version 2 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 ******************************************************************************/
package com.acc_ide.completion

import com.google.common.truth.Truth.assertThat
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentReference
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Comprehensive test suite for ACMCompletionProvider to verify complex variable declaration parsing.
 * 
 * This test verifies that the enhanced autocomplete functionality correctly identifies variables from:
 * 1. Multiple variable declarations: "int x, *p, xx;"
 * 2. Template types with initialization: "vector<int> arr(n);"
 * 3. Array declarations: "int matrix[10][10];"
 * 4. Pointer declarations: "int *ptr, **pptr;"
 * 5. Complex mixed declarations: "vector<vector<int>> matrix(5, vector<int>(5));"
 */
@RunWith(RobolectricTestRunner::class)
class ACMCompletionProviderTest {

    private val provider = ACMCompletionProvider()

    @Test
    fun `test multiple variable declarations`() {
        val code = """
            int x, *p, xx;
            void main() {
                long long a, b, c;
                unsigned int u, v;
                const int c1, c2;
            }
        """.trimIndent()

        val symbols = extractSymbolsFromCode(code)
        
        // Check that all variables are correctly identified
        assertThat(symbols).containsKey("x")
        assertThat(symbols["x"]?.dataType).isEqualTo("int")
        
        assertThat(symbols).containsKey("p")
        assertThat(symbols["p"]?.dataType).isEqualTo("int*")
        
        assertThat(symbols).containsKey("xx")
        assertThat(symbols["xx"]?.dataType).isEqualTo("int")
        
        assertThat(symbols).containsKey("a")
        assertThat(symbols["a"]?.dataType).isEqualTo("long long")
        
        assertThat(symbols).containsKey("b")
        assertThat(symbols["b"]?.dataType).isEqualTo("long long")
        
        assertThat(symbols).containsKey("c")
        assertThat(symbols["c"]?.dataType).isEqualTo("long long")
        
        assertThat(symbols).containsKey("u")
        assertThat(symbols["u"]?.dataType).isEqualTo("unsigned int")
        
        assertThat(symbols).containsKey("v")
        assertThat(symbols["v"]?.dataType).isEqualTo("unsigned int")
        
        assertThat(symbols).containsKey("c1")
        assertThat(symbols["c1"]?.dataType).isEqualTo("const int")
        
        assertThat(symbols).containsKey("c2")
        assertThat(symbols["c2"]?.dataType).isEqualTo("const int")
    }

    @Test
    fun `test template types with initialization`() {
        val code = """
            #include <vector>
            #include <map>
            using namespace std;
            
            vector<int> arr(n);
            map<string, int> m(10);
            vector<vector<int>> matrix(5, vector<int>(5));
            vector<int> arr2{1, 2, 3};
        """.trimIndent()

        val symbols = extractSymbolsFromCode(code)
        
        // Check that all template variables are correctly identified
        assertThat(symbols).containsKey("arr")
        assertThat(symbols["arr"]?.dataType).isEqualTo("vector")
        
        assertThat(symbols).containsKey("m")
        assertThat(symbols["m"]?.dataType).isEqualTo("map")
        
        assertThat(symbols).containsKey("matrix")
        assertThat(symbols["matrix"]?.dataType).isEqualTo("vector")
        
        assertThat(symbols).containsKey("arr2")
        assertThat(symbols["arr2"]?.dataType).isEqualTo("vector")
    }

    @Test
    fun `test array declarations`() {
        val code = """
            int arr[10];
            char str[256];
            double matrix[10][10];
            float cube[5][5][5];
        """.trimIndent()

        val symbols = extractSymbolsFromCode(code)
        
        // Check that all array variables are correctly identified
        assertThat(symbols).containsKey("arr")
        assertThat(symbols["arr"]?.dataType).isEqualTo("int[]")
        
        assertThat(symbols).containsKey("str")
        assertThat(symbols["str"]?.dataType).isEqualTo("char[]")
        
        assertThat(symbols).containsKey("matrix")
        assertThat(symbols["matrix"]?.dataType).isEqualTo("double[]")
        
        assertThat(symbols).containsKey("cube")
        assertThat(symbols["cube"]?.dataType).isEqualTo("float[]")
    }

    @Test
    fun `test pointer declarations`() {
        val code = """
            int *ptr;
            int **pptr;
            char *str_ptr, **str_pptr;
            void* void_ptr;
        """.trimIndent()

        val symbols = extractSymbolsFromCode(code)
        
        // Check that all pointer variables are correctly identified
        assertThat(symbols).containsKey("ptr")
        assertThat(symbols["ptr"]?.dataType).isEqualTo("int*")
        
        assertThat(symbols).containsKey("pptr")
        assertThat(symbols["pptr"]?.dataType).isEqualTo("int**")
        
        assertThat(symbols).containsKey("str_ptr")
        assertThat(symbols["str_ptr"]?.dataType).isEqualTo("char*")
        
        assertThat(symbols).containsKey("str_pptr")
        assertThat(symbols["str_pptr"]?.dataType).isEqualTo("char**")
        
        assertThat(symbols).containsKey("void_ptr")
        assertThat(symbols["void_ptr"]?.dataType).isEqualTo("void*")
    }

    @Test
    fun `test complex mixed declarations`() {
        val code = """
            #include <vector>
            using namespace std;
            
            vector<vector<int>> matrix(5, vector<int>(5));
            int *ptr_arr[10];
            char *(*(*foo[5])())[10];
        """.trimIndent()

        val symbols = extractSymbolsFromCode(code)
        
        // Check that all complex variables are correctly identified
        assertThat(symbols).containsKey("matrix")
        assertThat(symbols["matrix"]?.dataType).isEqualTo("vector")
        
        assertThat(symbols).containsKey("ptr_arr")
        assertThat(symbols["ptr_arr"]?.dataType).isEqualTo("int*[]")
        
        // Note: Complex function pointer declarations might be simplified in parsing
        assertThat(symbols).containsKey("foo")
        // The exact type might vary based on parsing implementation
    }

    @Test
    fun `test provideCompletions with complex declarations`() {
        val code = """
            #include <vector>
            using namespace std;
            
            int x, *p, xx;
            vector<int> arr(n);
            int matrix[10][10];
            int *ptr, **pptr;
            vector<vector<int>> complex_matrix(5, vector<int>(5));
            
            void main() {
                // Test completions here
            }
        """.trimIndent()

        val content = Content(code)
        val contentRef = ContentReference(content)
        val position = CharPosition().also { 
            it.line = 9  // Position inside main function
            it.column = 4
        }
        val publisher = TestCompletionPublisher()

        // Test providing completions
        provider.provideCompletions(contentRef, position, "", "cpp", publisher)
        
        val items = publisher.items
        // Verify that our variables are in the completion items
        val labels = items.map { it.label.toString() }
        
        assertThat(labels).contains("x")
        assertThat(labels).contains("p")
        assertThat(labels).contains("xx")
        assertThat(labels).contains("arr")
        assertThat(labels).contains("matrix")
        assertThat(labels).contains("ptr")
        assertThat(labels).contains("pptr")
        assertThat(labels).contains("complex_matrix")
    }
    
    @Test
    fun `test sorting priority - local symbols should come before keywords`() {
        val code = """
            int localVar;
            void localFunction() {}
            
            void main() {
                int localInMain;
                // Test completions here
            }
        """.trimIndent()

        val content = Content(code)
        val contentRef = ContentReference(content)
        val position = CharPosition().also { 
            it.line = 6  // Position inside main function
            it.column = 4
        }
        val publisher = TestCompletionPublisher()

        // Test providing completions with empty prefix to get all items
        provider.provideCompletions(contentRef, position, "", "cpp", publisher)
        
        val items = publisher.items
        val labels = items.map { it.label.toString() }
        
        // Verify that local variables and functions are present
        assertThat(labels).contains("localVar")
        assertThat(labels).contains("localFunction")
        assertThat(labels).contains("localInMain")
        assertThat(labels).contains("int")
        assertThat(labels).contains("void")
        
        // Check that local variables come before keywords in the list
        // Find indices of local variables and keywords
        val localVarIndex = labels.indexOf("localVar")
        val localFunctionIndex = labels.indexOf("localFunction")
        val localInMainIndex = labels.indexOf("localInMain")
        val intIndex = labels.indexOf("int")
        val voidIndex = labels.indexOf("void")
        
        // Local variables should come before keywords
        assertThat(localVarIndex).isLessThan(intIndex)
        assertThat(localFunctionIndex).isLessThan(voidIndex)
    }

    /**
     * Helper method to extract symbols from code for testing
     */
    private fun extractSymbolsFromCode(code: String): Map<String, ACMCompletionProvider.SymbolInfo> {
        val content = Content(code)
        val contentRef = ContentReference(content)
        
        // Update the local symbols using the internal method
        provider.updateLocalSymbols(contentRef)
        
        // Return the internal symbols map
        return provider.localSymbols
    }
}

/**
 * Test completion publisher to capture completion items
 */
class TestCompletionPublisher : CompletionPublisher() {
    val items = mutableListOf<io.github.rosemoe.sora.lang.completion.CompletionItem>()
    
    override fun addItem(item: io.github.rosemoe.sora.lang.completion.CompletionItem) {
        items.add(item)
    }
    
    override fun addItems(items: MutableIterable<io.github.rosemoe.sora.lang.completion.CompletionItem>) {
        this.items.addAll(items)
    }
}

