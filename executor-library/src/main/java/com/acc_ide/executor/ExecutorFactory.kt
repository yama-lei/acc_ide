package com.acc_ide.executor

import android.content.Context
import androidx.preference.PreferenceManager

/**
 * Factory for creating code executors
 * 代码执行器工厂
 */
object ExecutorFactory {
    
    const val PREF_EXECUTION_MODE = "execution_mode"
    const val MODE_CLOUD = "cloud"
    const val MODE_LOCAL = "local"
    
    /**
     * Create executor based on user preference
     * 根据用户偏好创建执行器
     */
    fun createExecutor(context: Context): ICodeExecutor {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val mode = prefs.getString(PREF_EXECUTION_MODE, MODE_CLOUD) ?: MODE_CLOUD
        
        return when (mode) {
            MODE_LOCAL -> LocalExecutor(context)
            else -> GitHubExecutor(context)
        }
    }
    
    /**
     * Get current execution mode
     * 获取当前执行模式
     */
    fun getExecutionMode(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(PREF_EXECUTION_MODE, MODE_CLOUD) ?: MODE_CLOUD
    }
    
    /**
     * Set execution mode
     * 设置执行模式
     */
    fun setExecutionMode(context: Context, mode: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit().putString(PREF_EXECUTION_MODE, mode).apply()
    }
}

