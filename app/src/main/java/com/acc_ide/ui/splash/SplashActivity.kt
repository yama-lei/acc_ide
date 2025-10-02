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
import com.acc_ide.executor.wasm.WasmPrewarmManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Splash activity for app initialization and system setup
 * 启动页活动 - 用于应用初始化和系统设置
 */
class SplashActivity : AppCompatActivity() {
    
    private lateinit var loadingText: TextView
    private lateinit var logInfoText: TextView
    private lateinit var fileStorageManager: FileStorageManager
    private lateinit var templateManager: TemplateManager
    private val logBuffer = StringBuilder()
    
    override fun attachBaseContext(newBase: Context) {
        val savedLanguage = LocaleHelper.getLanguage(newBase)
        if (savedLanguage.isEmpty()) {
            // If no saved language, use system default language
            super.attachBaseContext(newBase)
        } else {
            // Otherwise use saved language
            super.attachBaseContext(LocaleHelper.setLocale(newBase, savedLanguage))
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Apply saved theme settings
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val nightMode = prefs.getInt("app_night_mode", AppCompatDelegate.MODE_NIGHT_YES)
        AppCompatDelegate.setDefaultNightMode(nightMode)
        
        // Apply language settings
        applyLanguageSetting()
        
        setContentView(R.layout.activity_splash)
        
        loadingText = findViewById(R.id.loading_text)
        logInfoText = findViewById(R.id.log_info_text)
        
        // Start TextMate initialization immediately
        CoroutineScope(Dispatchers.IO).launch {
            TextMateManager.initialize(this@SplashActivity)
            logInfo(getString(R.string.log_textmate_init_started))
        }
        
        // Execute initialization operations in background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Update UI to show current operation
                updateLoadingText(getString(R.string.initializing_file_system))
                logInfo(getString(R.string.log_start_init_fs))
                
                // Initialize file storage manager
                fileStorageManager = FileStorageManager(this@SplashActivity)
                logInfo(getString(R.string.log_fs_manager_init_complete))
                
                // Clean expired deletion markers
                updateLoadingText(getString(R.string.cleaning_temp_files))
                logInfo(getString(R.string.log_start_cleaning_temp))
                cleanupExpiredDeletionMarkers()
                
                // Verify file system integrity
                updateLoadingText(getString(R.string.verifying_file_system))
                logInfo(getString(R.string.log_start_verifying_fs))
                verifyFileSystemIntegrity()
                
                // Initialize template system
                updateLoadingText(getString(R.string.initializing_templates))
                logInfo(getString(R.string.log_start_init_templates))
                templateManager = TemplateManager(this@SplashActivity)
                templateManager.initializeTemplates()
                logInfo(getString(R.string.log_templates_init_complete))
                
                // Set TextMate status
                updateLoadingText(getString(R.string.initializing_syntax_highlighting))
                logInfo(getString(R.string.log_start_init_textmate))
                logInfo(getString(R.string.log_textmate_init_complete))
                updateLoadingText(getString(R.string.initializing_cpp_compiler))
                logInfo(getString(R.string.log_start_cpp_prewarm))
                WasmPrewarmManager.prewarmCppExecutor(
                    context = this@SplashActivity,
                    onComplete = { success ->
                        CoroutineScope(Dispatchers.Main).launch {
                            if (success) {
                                logInfo(getString(R.string.log_cpp_compiler_ready))
                            } else {
                                logInfo(getString(R.string.log_cpp_prewarm_skipped))
                            }
                        }
                    }
                )
                
                // After all initialization work is complete, start MainActivity
                updateLoadingText(getString(R.string.loading_complete))
                logInfo(getString(R.string.log_all_tasks_complete))
                
                // Delay a bit to let user see "loading complete" message
                withContext(Dispatchers.Main) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        startMainActivity()
                    }, 800) // Delay 800ms
                }
            } catch (e: Exception) {
                Log.e("SplashActivity", "Error during initialization process", e)
                logInfo(getString(R.string.log_error, e.message))
                // Try to start MainActivity even if error occurs
                withContext(Dispatchers.Main) {
                    startMainActivity()
                }
            }
        }
    }
    
    /**
     * Apply language settings - use user-set language if available, otherwise use system language
     * 应用语言设置 - 如果用户已经设置了语言，则使用该语言，否则使用系统语言
     */
    private fun applyLanguageSetting() {
        val savedLanguage = LocaleHelper.getLanguage(this)
        
        if (savedLanguage.isNotEmpty()) {
            Log.d("SplashActivity", "Applying user-set language: $savedLanguage")
            // Ensure Locale is set correctly
            val locale = when (savedLanguage) {
                "en" -> Locale.ENGLISH
                "zh" -> Locale.CHINESE
                "zh-CN" -> Locale.SIMPLIFIED_CHINESE
                "zh-TW" -> Locale.TRADITIONAL_CHINESE
                else -> Locale(savedLanguage)
            }
            
            // Set default Locale
            Locale.setDefault(locale)
            Log.d("SplashActivity", "Set default Locale: $locale")
            
            // Update configuration
            val resources = resources
            val configuration = resources.configuration
            configuration.setLocale(locale)
            Log.d("SplashActivity", "Updated resource configuration")
            
            // Check if current language setting is effective
            val currentLocale = configuration.locales.get(0)
            Log.d("SplashActivity", "Current Locale: $currentLocale, language: ${currentLocale.language}")
            
            // Apply language setting
            val updatedContext = LocaleHelper.setLocale(this, savedLanguage)
            
            // Verify updated context language setting
            val updatedLocale = updatedContext.resources.configuration.locales.get(0)
            Log.d("SplashActivity", "Updated context Locale: $updatedLocale, language: ${updatedLocale.language}")
            
            // Ensure string resources use correct language
            try {
                val testString = getString(R.string.app_name)
                Log.d("SplashActivity", "Test string 'app_name': $testString")
            } catch (e: Exception) {
                Log.e("SplashActivity", "Error getting string resource", e)
            }
        } else {
            val systemLanguage = Locale.getDefault().language
            Log.d("SplashActivity", "Applying system language: $systemLanguage")
            LocaleHelper.setLocale(this, systemLanguage)
        }
    }
    
    /**
     * Update loading text on UI thread
     * 在UI线程更新加载文本
     */
    private suspend fun updateLoadingText(text: String) {
        withContext(Dispatchers.Main) {
            loadingText.text = text
        }
    }
    
    /**
     * Log info message and update UI
     * 记录信息消息并更新UI
     */
    private suspend fun logInfo(message: String) {
        Log.d("SplashActivity", message)
        logBuffer.append("• ").append(message).append("\n")
        withContext(Dispatchers.Main) {
            logInfoText.text = logBuffer.toString()
        }
    }
    
    /**
     * Start MainActivity and finish splash
     * 启动MainActivity并结束启动页
     */
    private fun startMainActivity() {
        val intent = Intent(this@SplashActivity, MainActivity::class.java)
        startActivity(intent)
        finish() // Close splash activity
    }
    
    /**
     * Clean up expired deletion markers - runs on app startup to clean deletion markers for non-existent files
     * 清理过期的删除标记 - 在应用启动时运行，清理那些已不存在文件的删除标记
     */
    private suspend fun cleanupExpiredDeletionMarkers() {
        try {
            // Get actual file list
            val filesDir = fileStorageManager.getCodeFilesDir()
            val actualFiles = filesDir.listFiles() ?: return
            
            // Check if there are meta files without corresponding main files
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
            Log.e("SplashActivity", "Error cleaning expired files", e)
            logInfo(getString(R.string.log_cleanup_error, e.message))
        }
    }
    
    /**
     * Verify file system integrity - ensure all files have corresponding metadata files
     * 验证文件系统的完整性 - 确保所有文件都有对应的元数据文件
     */
    private suspend fun verifyFileSystemIntegrity() {
        try {
            // Get actual file list
            val filesDir = fileStorageManager.getCodeFilesDir()
            val actualFiles = filesDir.listFiles() ?: return
            
            // Get main files and meta files
            val mainFiles = actualFiles.filter { !it.name.endsWith(".meta") }
            
            logInfo(getString(R.string.log_found_code_files, mainFiles.size))
            
            var fixedCount = 0
            
            // Check if each main file has corresponding meta file
            for (file in mainFiles) {
                val metaFile = java.io.File(filesDir, "${file.name}.meta")
                
                // If meta file doesn't exist, create a default one
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
            Log.e("SplashActivity", "Error verifying file system integrity", e)
            logInfo(getString(R.string.log_verify_error, e.message))
        }
    }
} 