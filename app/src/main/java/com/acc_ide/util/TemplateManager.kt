package com.acc_ide.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import android.os.Environment

/**
 * Template manager for handling code template loading and usage
 * 模板管理器 - 用于处理代码模板的加载与使用
 */
class TemplateManager(private val context: Context) {
    
    private val TAG = "TemplateManager"
    
    // Template file names
    private val CPP_TEMPLATE = "template.cpp"
    private val JAVA_TEMPLATE = "template.java"
    private val PYTHON_TEMPLATE = "template.py"
    
    // Default C++ template
    private val DEFAULT_CPP_TEMPLATE = """
        #include <iostream>
        #include <vector>
        #include <queue>
        #include <algorithm>
        #include <functional>
        #include <set>
        #include <map>
        #include <string>
        using namespace std;
        #define ll long long
        #define ull unsigned long long
        #define endl '\n'
        
        int main()
        {
        	ios::sync_with_stdio(false);
        	cin.tie(0);
        	cout.tie(0);
        

        

        

        

        
        	return 0;
        }
    """.trimIndent()
    
    // Default Java template
    private val DEFAULT_JAVA_TEMPLATE = """
        import java.io.*;
        import java.util.*;

        public class Main {
            static class FastScanner {
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                StringTokenizer st;
                String next() throws IOException {
                    while (st == null || !st.hasMoreTokens())
                        st = new StringTokenizer(br.readLine());
                    return st.nextToken();
                }
                int nextInt() throws IOException { return Integer.parseInt(next()); }
                long nextLong() throws IOException { return Long.parseLong(next()); }
                double nextDouble() throws IOException { return Double.parseDouble(next()); }
                String nextLine() throws IOException { return br.readLine(); }
            }

            public static void main(String[] args) throws Exception {
                FastScanner in = new FastScanner();
                PrintWriter out = new PrintWriter(System.out);

                int n = in.nextInt();
                long x = in.nextLong();
                out.println(n + " " + x);

                out.flush();
            }
        }
    """.trimIndent()
    
    // Default Python template
    private val DEFAULT_PYTHON_TEMPLATE = """
        import sys
        from collections import defaultdict, Counter, deque
        from heapq import heapify, heappush, heappop
        from bisect import bisect_left, bisect_right
        from math import gcd, sqrt, inf

        def input(): return sys.stdin.readline().strip()
        def list_input(): return list(map(int, input().split()))
        def int_input(): return int(input())
        
        def main():
            # Fast input
            
            
            
            
        if __name__ == "__main__":
            main()
    """.trimIndent()

    // File storage manager instance
    private val fileStorageManager = FileStorageManager(context)
    
    /**
     * Get template directory path
     * 获取模板目录
     */
    private fun getTemplateDir(): File {
        try {
            // Get package name, i.e. com.acc_ide
            val packageName = context.packageName
            
            // Get external storage root directory
            val externalDir = Environment.getExternalStorageDirectory()
            
            // Create template directory: /storage/emulated/0/com.acc_ide/template/
            val templateDir = File(externalDir, "$packageName/template")
            
            // Ensure directory exists
            if (!templateDir.exists()) {
                // Ensure parent directory also exists
                val parentDir = templateDir.parentFile
                if (parentDir != null && !parentDir.exists()) {
                    val parentSuccess = parentDir.mkdirs()
                    Log.d(TAG, "Create parent directory: ${parentDir.absolutePath}, success: $parentSuccess")
                }
                
                val success = templateDir.mkdirs()
                Log.d(TAG, "Create template directory: ${templateDir.absolutePath}, success: $success")
                
                // If creation fails, try backup path
                if (!success) {
                    throw IOException("Cannot create template directory: ${templateDir.absolutePath}")
                }
            }
            
            return templateDir
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get template directory, using backup path", e)
            
            // Backup path: create template subdirectory directly under app files directory
            val backupTemplateDir = File(fileStorageManager.getCodeFilesDir(), "template")
            if (!backupTemplateDir.exists()) {
                backupTemplateDir.mkdirs()
                Log.d(TAG, "Create backup template directory: ${backupTemplateDir.absolutePath}")
            }
            
            return backupTemplateDir
        }
    }
    
    /**
     * Initialize template files - create default templates if template files don't exist
     * 初始化模板文件 - 如果模板文件不存在，创建默认模板
     */
    fun initializeTemplates() {
        try {
            val templateDir = getTemplateDir()
            
            // Ensure directory exists and is writable
            if (!templateDir.exists() && !templateDir.mkdirs()) {
                Log.e(TAG, "Cannot create template directory: ${templateDir.absolutePath}")
                // If unable to create directory, return directly to avoid subsequent operations
                return
            }
            
            if (!templateDir.canWrite()) {
                Log.e(TAG, "Template directory is not writable: ${templateDir.absolutePath}")
                return
            }
            
            // Use safe file writing method
            safeWriteTemplateFile(templateDir, CPP_TEMPLATE, DEFAULT_CPP_TEMPLATE)
            safeWriteTemplateFile(templateDir, JAVA_TEMPLATE, DEFAULT_JAVA_TEMPLATE)
            safeWriteTemplateFile(templateDir, PYTHON_TEMPLATE, DEFAULT_PYTHON_TEMPLATE)
            
            Log.d(TAG, "Template initialization completed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize templates", e)
        }
    }
    
    /**
     * Get template content for specified language
     * 获取指定语言的模板内容
     * @param language Programming language
     * @return Template content
     */
    fun getTemplateContent(language: String): String {
        try {
            val templateFile = when (language) {
                "cpp" -> File(getTemplateDir(), CPP_TEMPLATE)
                "java" -> File(getTemplateDir(), JAVA_TEMPLATE)
                "python" -> File(getTemplateDir(), PYTHON_TEMPLATE)
                else -> return ""
            }
            
            // If template file exists, read its content
            if (templateFile.exists()) {
                return templateFile.readText()
            }
            
            // If template file doesn't exist, return default template
            return when (language) {
                "cpp" -> DEFAULT_CPP_TEMPLATE
                "java" -> DEFAULT_JAVA_TEMPLATE
                "python" -> DEFAULT_PYTHON_TEMPLATE
                else -> ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get template content: $language", e)
            
            // Return default template when error occurs
            return when (language) {
                "cpp" -> DEFAULT_CPP_TEMPLATE
                "java" -> DEFAULT_JAVA_TEMPLATE
                "python" -> DEFAULT_PYTHON_TEMPLATE
                else -> ""
            }
        }
    }

    /**
     * Safely write template file with error handling
     * 安全地写入模板文件，包含错误处理
     */
    private fun safeWriteTemplateFile(dir: File, fileName: String, content: String) {
        try {
            val file = File(dir, fileName)
            if (!file.exists()) {
                // Use temporary file for writing, then rename after success to avoid file corruption from partial writes
                val tempFile = File(dir, "$fileName.tmp")
                tempFile.writeText(content)
                val success = tempFile.renameTo(file)
                
                if (success) {
                    Log.d(TAG, "Successfully created default $fileName template")
                } else {
                    // If rename fails, write directly to original file
                    file.writeText(content)
                    Log.d(TAG, "Created default $fileName template by direct writing")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create $fileName template", e)
            // Try backup method
            try {
                val file = File(dir, fileName)
                file.outputStream().use { output ->
                    output.write(content.toByteArray())
                    output.flush()
                }
                Log.d(TAG, "Successfully created $fileName template using backup method")
            } catch (e2: Exception) {
                Log.e(TAG, "Backup method for creating $fileName template also failed", e2)
            }
        }
    }
} 