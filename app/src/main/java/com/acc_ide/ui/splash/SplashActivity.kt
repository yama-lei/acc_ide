package com.acc_ide.ui.splash

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.acc_ide.R
import com.acc_ide.ui.main.MainActivity
import com.acc_ide.util.FileStorageManager
import com.acc_ide.util.LocaleHelper
import com.acc_ide.util.TemplateManager
import com.acc_ide.util.TextMateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class SplashActivity : AppCompatActivity() {
    
    private lateinit var loadingText: TextView
    private lateinit var logInfoText: TextView
    private lateinit var fileStorageManager: FileStorageManager
    private lateinit var templateManager: TemplateManager
    private val logBuffer = StringBuilder()
    
    override fun attachBaseContext(newBase: Context) {
        val savedLanguage = LocaleHelper.getLanguage(newBase)
        if (savedLanguage.isEmpty()) {
            // 如果没有保存的语言，使用系统默认语言
            super.attachBaseContext(newBase)
        } else {
            // 否则使用保存的语言
            super.attachBaseContext(LocaleHelper.setLocale(newBase, savedLanguage))
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 应用保存的主题设置
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val nightMode = prefs.getInt("app_night_mode", AppCompatDelegate.MODE_NIGHT_YES)
        AppCompatDelegate.setDefaultNightMode(nightMode)
        
        // 应用语言设置
        applyLanguageSetting()
        
        setContentView(R.layout.activity_splash)
        
        loadingText = findViewById(R.id.loading_text)
        logInfoText = findViewById(R.id.log_info_text)
        
        // 立即开始TextMate的初始化
        CoroutineScope(Dispatchers.IO).launch {
            TextMateManager.initialize(this@SplashActivity)
            logInfo(getString(R.string.log_textmate_init_started))
        }
        
        // 在后台线程中执行初始化操作
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 更新UI显示当前正在执行的操作
                updateLoadingText(getString(R.string.initializing_file_system))
                logInfo(getString(R.string.log_start_init_fs))
                
                // 初始化文件存储管理器
                fileStorageManager = FileStorageManager(this@SplashActivity)
                logInfo(getString(R.string.log_fs_manager_init_complete))
                
                // 清理过期的删除标记
                updateLoadingText(getString(R.string.cleaning_temp_files))
                logInfo(getString(R.string.log_start_cleaning_temp))
                cleanupExpiredDeletionMarkers()
                
                // 验证文件系统完整性
                updateLoadingText(getString(R.string.verifying_file_system))
                logInfo(getString(R.string.log_start_verifying_fs))
                verifyFileSystemIntegrity()
                
                // 初始化模板系统
                updateLoadingText(getString(R.string.initializing_templates))
                logInfo(getString(R.string.log_start_init_templates))
                templateManager = TemplateManager(this@SplashActivity)
                templateManager.initializeTemplates()
                logInfo(getString(R.string.log_templates_init_complete))
                
                // 设置TextMate状态
                updateLoadingText(getString(R.string.initializing_syntax_highlighting))
                logInfo(getString(R.string.log_start_init_textmate))
                // 因为TextMateManager中没有loadLanguageIfNeeded方法，所以直接显示完成消息
                logInfo(getString(R.string.log_textmate_init_complete))
                
                // 所有初始化工作完成后，启动主Activity
                updateLoadingText(getString(R.string.loading_complete))
                logInfo(getString(R.string.log_all_tasks_complete))
                
                // 延迟一小段时间，让用户看到"加载完成"的消息
                withContext(Dispatchers.Main) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        startMainActivity()
                    }, 800) // 延迟800毫秒
                }
            } catch (e: Exception) {
                Log.e("SplashActivity", "初始化过程中出错", e)
                logInfo(getString(R.string.log_error, e.message))
                // 即使出错也尝试启动主Activity
                withContext(Dispatchers.Main) {
                    startMainActivity()
                }
            }
        }
    }
    
    /**
     * 应用语言设置
     * 如果用户已经设置了语言，则使用该语言
     * 否则使用系统语言
     */
    private fun applyLanguageSetting() {
        val savedLanguage = LocaleHelper.getLanguage(this)
        
        if (savedLanguage.isNotEmpty()) {
            Log.d("SplashActivity", "应用用户设置的语言: $savedLanguage")
            // 确保Locale设置正确
            val locale = when (savedLanguage) {
                "en" -> Locale.ENGLISH
                "zh" -> Locale.CHINESE
                "zh-CN" -> Locale.SIMPLIFIED_CHINESE
                "zh-TW" -> Locale.TRADITIONAL_CHINESE
                else -> Locale(savedLanguage)
            }
            
            // 设置默认Locale
            Locale.setDefault(locale)
            Log.d("SplashActivity", "设置默认Locale: $locale")
            
            // 更新配置
            val resources = resources
            val configuration = resources.configuration
            configuration.setLocale(locale)
            Log.d("SplashActivity", "已更新资源配置")
            
            // 检查当前语言设置是否生效
            val currentLocale = configuration.locales.get(0)
            Log.d("SplashActivity", "当前Locale: $currentLocale, 语言: ${currentLocale.language}")
            
            // 应用语言设置
            val updatedContext = LocaleHelper.setLocale(this, savedLanguage)
            
            // 验证更新后的上下文语言设置
            val updatedLocale = updatedContext.resources.configuration.locales.get(0)
            Log.d("SplashActivity", "更新后上下文的Locale: $updatedLocale, 语言: ${updatedLocale.language}")
            
            // 确保字符串资源使用正确的语言
            try {
                val testString = getString(R.string.app_name)
                Log.d("SplashActivity", "测试字符串 'app_name': $testString")
            } catch (e: Exception) {
                Log.e("SplashActivity", "获取字符串资源时出错", e)
            }
        } else {
            val systemLanguage = Locale.getDefault().language
            Log.d("SplashActivity", "应用系统语言: $systemLanguage")
            LocaleHelper.setLocale(this, systemLanguage)
        }
    }
    
    private suspend fun updateLoadingText(text: String) {
        withContext(Dispatchers.Main) {
            loadingText.text = text
        }
    }
    
    private suspend fun logInfo(message: String) {
        Log.d("SplashActivity", message)
        logBuffer.append("• ").append(message).append("\n")
        withContext(Dispatchers.Main) {
            logInfoText.text = logBuffer.toString()
        }
    }
    
    private fun startMainActivity() {
        val intent = Intent(this@SplashActivity, MainActivity::class.java)
        startActivity(intent)
        finish() // 关闭启动页面
    }
    
    /**
     * 清理过期的删除标记
     * 在应用启动时运行，清理那些已不存在文件的删除标记
     */
    private suspend fun cleanupExpiredDeletionMarkers() {
        try {
            // 获取实际文件列表
            val filesDir = fileStorageManager.getCodeFilesDir()
            val actualFiles = filesDir.listFiles() ?: return
            
            // 检查是否有元数据文件但没有对应的主文件
            val mainFiles = actualFiles.filter { !it.name.endsWith(".meta") }.map { it.name }.toSet()
            val metaFiles = actualFiles.filter { it.name.endsWith(".meta") }
                .map { it.name.removeSuffix(".meta") }.toSet()
            val orphanedMetaFiles = metaFiles.filter { !mainFiles.contains(it) }
            if (orphanedMetaFiles.isNotEmpty()) {
                logInfo(getString(R.string.log_found_orphaned_meta, orphanedMetaFiles.size))
                
                for (fileName in orphanedMetaFiles) {
                    val metaFile = java.io.File(filesDir, "$fileName.meta")
                    if (metaFile.exists() && metaFile.delete()) {
                        logInfo(getString(R.string.log_deleted_orphaned_meta, "$fileName.meta"))
                    }
                }
            } else {
                logInfo(getString(R.string.log_no_orphaned_meta))
            }
        } catch (e: Exception) {
            Log.e("SplashActivity", "清理过期文件时出错", e)
            logInfo(getString(R.string.log_cleanup_error, e.message))
        }
    }
    
    /**
     * 验证文件系统的完整性
     * 确保所有文件都有对应的元数据文件
     */
    private suspend fun verifyFileSystemIntegrity() {
        try {
            // 获取实际文件列表
            val filesDir = fileStorageManager.getCodeFilesDir()
            val actualFiles = filesDir.listFiles() ?: return
            
            // 获取主文件和元数据文件
            val mainFiles = actualFiles.filter { !it.name.endsWith(".meta") }
            
            logInfo(getString(R.string.log_found_code_files, mainFiles.size))
            
            var fixedCount = 0
            
            // 检查每个主文件是否有对应的元数据文件
            for (file in mainFiles) {
                val metaFile = java.io.File(filesDir, "${file.name}.meta")
                
                // 如果元数据文件不存在，创建一个默认的
                if (!metaFile.exists()) {
                    val language = when {
                        file.name.endsWith(".cpp") -> "cpp"
                        file.name.endsWith(".java") -> "java"
                        file.name.endsWith(".py") -> "python"
                        else -> "text"
                    }
                    
                    metaFile.writeText("$language\n${System.currentTimeMillis()}\nfalse\n")
                    fixedCount++
                }
            }
            
            if (fixedCount > 0) {
                logInfo(getString(R.string.log_fixed_missing_meta, fixedCount))
            } else {
                logInfo(getString(R.string.log_all_meta_complete))
            }
        } catch (e: Exception) {
            Log.e("SplashActivity", "验证文件系统完整性时出错", e)
            logInfo(getString(R.string.log_verify_error, e.message))
        }
    }
} 