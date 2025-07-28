package com.acc_ide.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

/**
 * 文件仓库类 - 负责所有文件操作，提供干净的API给ViewModel
 * 遵循仓库模式，集中处理所有文件相关业务逻辑
 */
class FileRepository(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("acc_ide_prefs", Context.MODE_PRIVATE)
    
    // 当前打开的文件列表
    private val openedFiles = mutableSetOf<String>()
    
    init {
        // 初始化时加载已打开的文件列表
        loadOpenedFilesList()
    }
 

    
    /**
     * 加载已打开的文件列表
     */
    private fun loadOpenedFilesList() {
        val openedFileSet = prefs.getStringSet("opened_files", null)
        
        // 清空当前列表并加载保存的列表
        openedFiles.clear()
        if (openedFileSet != null) {
            openedFiles.addAll(openedFileSet)
            Log.d("FileRepository", "已加载上次打开的文件列表: ${openedFiles.joinToString(", ")}")
        }
    }
    

    

} 