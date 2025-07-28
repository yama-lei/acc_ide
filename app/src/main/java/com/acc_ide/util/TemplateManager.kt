package com.acc_ide.util

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import android.os.Environment

/**
 * 模板管理器，用于处理代码模板的加载与使用
 */
class TemplateManager(private val context: Context) {
    
    private val TAG = "TemplateManager"
    
    // 模板文件名
    private val CPP_TEMPLATE = "template.cpp"
    private val JAVA_TEMPLATE = "template.java"
    private val PYTHON_TEMPLATE = "template.py"
    
    // 默认C++模板
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
    
    // 默认Java模板
    private val DEFAULT_JAVA_TEMPLATE = """
        import java.util.*;
        import java.io.*;
        
        public class Main {
            static class FastReader {
                BufferedReader br;
                StringTokenizer st;
                
                public FastReader() {
                    br = new BufferedReader(new InputStreamReader(System.in));
                }
                
                String next() {
                    while (st == null || !st.hasMoreElements()) {
                        try {
                            st = new StringTokenizer(br.readLine());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    return st.nextToken();
                }
                
                int nextInt() { return Integer.parseInt(next()); }
                long nextLong() { return Long.parseLong(next()); }
                double nextDouble() { return Double.parseDouble(next()); }
                
                String nextLine() {
                    String str = "";
                    try {
                        str = br.readLine();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return str;
                }
            }
            
            public static void main(String[] args) {
                FastReader in = new FastReader();
                
                
                
            }
        }
    """.trimIndent()
    
    // 默认Python模板
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
            # 快读
            
            
            
            
        if __name__ == "__main__":
            main()
    """.trimIndent()

    // 文件存储管理器实例
    private val fileStorageManager = FileStorageManager(context)
    
    /**
     * 获取模板目录
     */
    private fun getTemplateDir(): File {
        try {
            // 获取包名，即com.acc_ide
            val packageName = context.packageName
            
            // 获取外部存储根目录
            val externalDir = Environment.getExternalStorageDirectory()
            
            // 创建template目录：/storage/emulated/0/com.acc_ide/template/
            val templateDir = File(externalDir, "$packageName/template")
            
            // 确保目录存在
            if (!templateDir.exists()) {
                // 确保父目录也存在
                val parentDir = templateDir.parentFile
                if (parentDir != null && !parentDir.exists()) {
                    val parentSuccess = parentDir.mkdirs()
                    Log.d(TAG, "创建父目录: ${parentDir.absolutePath}, 成功: $parentSuccess")
                }
                
                val success = templateDir.mkdirs()
                Log.d(TAG, "创建模板目录: ${templateDir.absolutePath}, 成功: $success")
                
                // 如果创建失败，尝试备用路径
                if (!success) {
                    throw IOException("无法创建模板目录: ${templateDir.absolutePath}")
                }
            }
            
            return templateDir
        } catch (e: Exception) {
            Log.e(TAG, "获取模板目录失败，使用备用路径", e)
            
            // 备用路径：直接在应用文件目录下创建template子目录
            val backupTemplateDir = File(fileStorageManager.getCodeFilesDir(), "template")
            if (!backupTemplateDir.exists()) {
                backupTemplateDir.mkdirs()
                Log.d(TAG, "创建备用模板目录: ${backupTemplateDir.absolutePath}")
            }
            
            return backupTemplateDir
        }
    }
    
    /**
     * 初始化模板文件
     * 如果模板文件不存在，创建默认模板
     */
    fun initializeTemplates() {
        try {
            val templateDir = getTemplateDir()
            
            // 确保目录存在且可写
            if (!templateDir.exists() && !templateDir.mkdirs()) {
                Log.e(TAG, "无法创建模板目录: ${templateDir.absolutePath}")
                // 如果无法创建目录，直接返回，避免后续操作
                return
            }
            
            if (!templateDir.canWrite()) {
                Log.e(TAG, "模板目录不可写: ${templateDir.absolutePath}")
                return
            }
            
            // 使用安全的文件写入方法
            safeWriteTemplateFile(templateDir, CPP_TEMPLATE, DEFAULT_CPP_TEMPLATE)
            safeWriteTemplateFile(templateDir, JAVA_TEMPLATE, DEFAULT_JAVA_TEMPLATE)
            safeWriteTemplateFile(templateDir, PYTHON_TEMPLATE, DEFAULT_PYTHON_TEMPLATE)
            
            Log.d(TAG, "模板初始化完成")
        } catch (e: Exception) {
            Log.e(TAG, "初始化模板失败", e)
        }
    }
    
    /**
     * 获取指定语言的模板内容
     * 
     * @param language 编程语言
     * @return 模板内容
     */
    fun getTemplateContent(language: String): String {
        try {
            val templateFile = when (language) {
                "cpp" -> File(getTemplateDir(), CPP_TEMPLATE)
                "java" -> File(getTemplateDir(), JAVA_TEMPLATE)
                "python" -> File(getTemplateDir(), PYTHON_TEMPLATE)
                else -> return ""
            }
            
            // 如果模板文件存在，读取其内容
            if (templateFile.exists()) {
                return templateFile.readText()
            }
            
            // 如果模板文件不存在，返回默认模板
            return when (language) {
                "cpp" -> DEFAULT_CPP_TEMPLATE
                "java" -> DEFAULT_JAVA_TEMPLATE
                "python" -> DEFAULT_PYTHON_TEMPLATE
                else -> ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取模板内容失败: $language", e)
            
            // 发生错误时返回默认模板
            return when (language) {
                "cpp" -> DEFAULT_CPP_TEMPLATE
                "java" -> DEFAULT_JAVA_TEMPLATE
                "python" -> DEFAULT_PYTHON_TEMPLATE
                else -> ""
            }
        }
    }

    /**
     * 安全地写入模板文件，包含错误处理
     */
    private fun safeWriteTemplateFile(dir: File, fileName: String, content: String) {
        try {
            val file = File(dir, fileName)
            if (!file.exists()) {
                // 使用临时文件写入，成功后再重命名，避免部分写入导致的文件损坏
                val tempFile = File(dir, "$fileName.tmp")
                tempFile.writeText(content)
                val success = tempFile.renameTo(file)
                
                if (success) {
                    Log.d(TAG, "创建默认${fileName}模板成功")
                } else {
                    // 如果重命名失败，直接写入原文件
                    file.writeText(content)
                    Log.d(TAG, "通过直接写入创建默认${fileName}模板")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "创建${fileName}模板失败", e)
            // 尝试使用备用方法
            try {
                val file = File(dir, fileName)
                file.outputStream().use { output ->
                    output.write(content.toByteArray())
                    output.flush()
                }
                Log.d(TAG, "使用备用方法创建${fileName}模板成功")
            } catch (e2: Exception) {
                Log.e(TAG, "使用备用方法创建${fileName}模板也失败", e2)
            }
        }
    }
} 