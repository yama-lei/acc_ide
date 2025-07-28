package com.acc_ide.util

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.preference.PreferenceManager
import com.acc_ide.R
import com.acc_ide.ui.main.MainActivity

/**
 * Theme manager for handling app theme and language settings
 * 主题管理器 - 负责处理应用主题、语言设置等
 */
class ThemeManager(private val activity: AppCompatActivity) {
    
    companion object {
        private const val TAG = "ThemeManager"
    }
    
    /**
     * Apply code editor theme - ensure TextMate theme is set correctly when Activity is created
     * 应用代码编辑器主题 - 确保在Activity创建时正确设置TextMate主题
     */
    fun applyCodeEditorTheme() {
        try {
            // Set TextMate theme based on system theme
            val isDarkMode =
                (activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                        Configuration.UI_MODE_NIGHT_YES
            val textMateTheme = if (isDarkMode) "dark.json" else "light.json"

            // Apply TextMate theme
            TextMateManager.setTheme(textMateTheme)
            Log.d(TAG, "Applied theme: $textMateTheme")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply theme: ${e.message}")
        }
    }
    
    /**
     * Apply language settings from preferences
     * 应用语言设置
     */
    fun applyLanguage() {
        val savedLanguage = LocaleHelper.getLanguage(activity)
        // Use saved language, if none use system default language
        if (savedLanguage.isEmpty()) {
            LocaleHelper.setLocale(activity, java.util.Locale.getDefault().language)
        } else {
            LocaleHelper.setLocale(activity, savedLanguage)
        }
    }
    
    /**
     * Apply saved theme settings from preferences
     * 应用保存的主题设置
     */
    fun applyThemeSettings() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(activity)
        val nightMode = prefs.getInt("app_night_mode", AppCompatDelegate.MODE_NIGHT_YES)
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
    
    /**
     * Change language for other fragments to call
     * 用于其他Fragment调用来切换语言
     */
    fun changeLanguage(languageCode: String) {
        LocaleHelper.setLocale(activity, languageCode)
        recreateActivityForLanguageChange()
    }
    
    /**
     * Recreate activity to apply language change
     * 重建Activity以应用语言变化
     */
    private fun recreateActivityForLanguageChange() {
        // Save current state
        val bundle = Bundle()

        // Save current fragment type
        val currentFragment = activity.supportFragmentManager.findFragmentById(R.id.content_frame)
        when (currentFragment) {
            is com.acc_ide.ui.settings.SettingsFragment -> bundle.putString("currentFragment", "settings")
            is com.acc_ide.ui.editor.EditorFragment -> bundle.putString("currentFragment", "editor")
            is com.acc_ide.ui.welcome.WelcomeFragment -> bundle.putString("currentFragment", "welcome")
        }

        // Save other needed state
        if (activity is MainActivity) {
            bundle.putString("currentFileName", (activity as MainActivity).files.keys.firstOrNull() ?: "")
        }
        
        bundle.putBoolean(
            "needsBackButton",
            currentFragment is com.acc_ide.ui.settings.SettingsFragment ||
                    activity.supportActionBar?.displayOptions?.and(androidx.appcompat.app.ActionBar.DISPLAY_HOME_AS_UP) != 0
        )

        // Restore these states in next activity
        val intent = Intent(activity, MainActivity::class.java)
        intent.putExtra("savedState", bundle)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        // Start new activity immediately
        activity.startActivity(intent)
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        activity.finish()
    }
    
    /**
     * Handle configuration changes such as night mode switching
     * 处理配置变化（如夜间模式切换）
     */
    fun handleConfigurationChanged(newConfig: Configuration) {
        // Check if it's night mode change
        val nightModeFlags = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        // Get current fragment
        val currentFragment = activity.supportFragmentManager.findFragmentById(R.id.content_frame)

        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            // Handle dark mode specific logic
            Log.d(TAG, "App switched to dark mode")
            if (currentFragment is com.acc_ide.ui.editor.EditorFragment) {
                currentFragment.refreshEditorTheme()
            }
        } else {
            // Handle light mode specific logic
            Log.d(TAG, "App switched to light mode")
            if (currentFragment is com.acc_ide.ui.editor.EditorFragment) {
                currentFragment.refreshEditorTheme()
            }
        }
    }
    
    /**
     * Update UI resources and text after language change
     * 更新界面资源和文本
     */
    fun updateResourcesAfterLanguageChange() {
        // Update title bar and other UI elements that might be affected by language
        if (activity is MainActivity) {
            val mainActivity = activity as MainActivity
            val currentFileName = mainActivity.files.keys.firstOrNull()
            
            activity.supportActionBar?.title = if (!currentFileName.isNullOrEmpty()) {
                currentFileName
            } else {
                activity.getString(R.string.app_name)
            }
        }

        // Refresh current fragment UI
        val currentFragment = activity.supportFragmentManager.findFragmentById(R.id.content_frame)
        if (currentFragment is com.acc_ide.ui.settings.SettingsFragment) {
            activity.supportActionBar?.title = activity.getString(R.string.settings)
        }

        // Refresh navigation drawer title
        val filesTextView = activity.findViewById<android.widget.TextView>(R.id.files_text)
        filesTextView?.text = activity.getString(R.string.files)

        val settingsTextView = activity.findViewById<android.widget.TextView>(R.id.settings_text)
        settingsTextView?.text = activity.getString(R.string.settings)

        val newFileButton = activity.findViewById<android.widget.ImageButton>(R.id.new_file_button)
        newFileButton?.contentDescription = activity.getString(R.string.new_file)
    }
    
    /**
     * Refresh editor syntax highlighting and theme
     * 刷新编辑器的语法高亮
     */
    fun refreshEditorSyntaxHighlighting() {
        // Get current EditorFragment
        val editorFragment = activity.supportFragmentManager.fragments
            .firstOrNull { it is com.acc_ide.ui.editor.EditorFragment } as? com.acc_ide.ui.editor.EditorFragment
        
        editorFragment?.let { fragment ->
            // Call method to refresh theme and syntax highlighting
            fragment.refreshEditorTheme()

            // Log info
            Log.d(TAG, "Refreshed editor syntax highlighting and theme")
        }
    }
    
    /**
     * Refresh theme when app resumes
     * 在应用恢复时刷新主题
     */
    fun onResume() {
        try {
            // Ensure correct editor theme is applied every time on resume
            applyCodeEditorTheme()

            // If current has editor fragment, refresh its theme
            val currentFragment = activity.supportFragmentManager.findFragmentById(R.id.content_frame)
            if (currentFragment is com.acc_ide.ui.editor.EditorFragment) {
                // Delay a bit to ensure activity is fully resumed
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    refreshEditorSyntaxHighlighting()
                    Log.d(TAG, "onResume: Refreshed editor theme")
                }, 100)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh theme on app resume", e)
        }
    }
} 