package com.acc_ide.completion.test

import com.acc_ide.completion.languages.java.JavaLanguageSupport
import com.acc_ide.completion.languages.python.PythonLanguageSupport

/**
 * 简化的测试类
 * 不使用复杂的CompletionPublisher实现，只测试基本功能
 */
class LanguageSupportSimpleTest {
    
    companion object {
        private const val TAG = "LanguageSupportSimpleTest"
        
        /**
         * 测试Java语言支持基本功能
         */
        fun testJavaLanguageBasics() {
            android.util.Log.d(TAG, "=== Testing Java Language Basics ===")
            
            try {
                // 创建Java语言支持实例
                val javaSupport = JavaLanguageSupport("source.java")
                
                android.util.Log.d(TAG, "Java Language Support initialized successfully")
                
                // 测试基本属性
                val analyzeManager = javaSupport.getAnalyzeManager()
                val useTab = javaSupport.useTab()
                val symbolPairs = javaSupport.getSymbolPairs()
                
                android.util.Log.d(TAG, "Java AnalyzeManager: ${analyzeManager != null}")
                android.util.Log.d(TAG, "Java uses tab: $useTab")
                android.util.Log.d(TAG, "Java symbol pairs: ${symbolPairs != null}")
                
                android.util.Log.d(TAG, "Java language support basic test completed successfully")
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Java language support basic test failed", e)
            }
        }
        
        /**
         * 测试Python语言支持基本功能
         */
        fun testPythonLanguageBasics() {
            android.util.Log.d(TAG, "=== Testing Python Language Basics ===")
            
            try {
                // 创建Python语言支持实例
                val pythonSupport = PythonLanguageSupport("source.python")
                
                android.util.Log.d(TAG, "Python Language Support initialized successfully")
                
                // 测试基本属性
                val analyzeManager = pythonSupport.getAnalyzeManager()
                val useTab = pythonSupport.useTab()
                val symbolPairs = pythonSupport.getSymbolPairs()
                
                android.util.Log.d(TAG, "Python AnalyzeManager: ${analyzeManager != null}")
                android.util.Log.d(TAG, "Python uses tab: $useTab")
                android.util.Log.d(TAG, "Python symbol pairs: ${symbolPairs != null}")
                
                android.util.Log.d(TAG, "Python language support basic test completed successfully")
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Python language support basic test failed", e)
            }
        }
        
        /**
         * 运行所有基本测试
         */
        fun runAllBasicTests() {
            android.util.Log.d(TAG, "Starting Language Support Basic Tests...")
            
            testJavaLanguageBasics()
            testPythonLanguageBasics()
            
            android.util.Log.d(TAG, "All Language Support Basic Tests completed")
        }
    }
}