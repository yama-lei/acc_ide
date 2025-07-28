package com.acc_ide.util

import android.content.Context
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.eclipse.tm4e.core.registry.IThemeSource
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import android.content.res.Configuration

/**
 * TextMate语法高亮管理器
 */
object TextMateManager {
    private var isInitialized = false
    private const val THEME_PATH = "textmate/themes/"
    private const val THEME_DARK = "dark.json"  // 自定义暗色主题
    private const val THEME_LIGHT = "light.json"  // 自定义亮色主题
    
    /**
     * Initialize TextMate support
     * This should be called once when the application starts
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        // Setup file resolver for assets
        FileProviderRegistry.getInstance().addFileProvider(
            AssetsFileResolver(context.applicationContext.assets)
        )
        loadThemes()
        
        // 检查当前系统主题，自动选择对应的主题
        val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val defaultThemeFile = if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            THEME_DARK // 深色模式使用自定义的Material Dark主题
        } else {
            THEME_LIGHT // 浅色模式使用自定义的Material Light主题
        }
        
        // 设置默认主题ID (不带.json扩展名)
        val themeId = defaultThemeFile.substringBeforeLast(".json")
        ThemeRegistry.getInstance().setTheme(themeId)
        android.util.Log.d("TextMateManager", "初始化时设置默认主题: $defaultThemeFile (ID: $themeId)")
        
        loadLanguages()
        
        isInitialized = true
    }
    
    /**
     * Load all available TextMate themes from assets
     */
    private fun loadThemes() {
        val themeRegistry = ThemeRegistry.getInstance()
        
        // List of themes to load with their filenames and dark mode flag
        val themes = listOf(
            THEME_DARK to true,   // 自定义暗色主题
            THEME_LIGHT to false  // 自定义亮色主题
        )
        
        // Load each theme
        themes.forEach { (themeFile, isDark) ->
            try {
                // 获取输入流
                val inputStream = FileProviderRegistry.getInstance().tryGetInputStream(THEME_PATH + themeFile)
                if (inputStream != null) {
                    val themeSource = IThemeSource.fromInputStream(inputStream, themeFile, null)
                    // 加载主题
                    themeRegistry.loadTheme(themeSource, isDark)
                    
                    // 记录加载成功的主题
                    android.util.Log.d("TextMateManager", "主题加载成功: $themeFile")
                }
            } catch (e: Exception) {
                android.util.Log.e("TextMateManager", "主题加载失败: $themeFile - ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Load TextMate language definitions from language.json file in assets
     */
    private fun loadLanguages() {
        GrammarRegistry.getInstance().loadGrammars("textmate/languages/language.json")
    }
    

    /**
     * Set the active TextMate theme
     * @param themeName The name of the theme to use 
     */
    fun setTheme(themeName: String) {
        val themeId = themeName.substringBeforeLast(".json")
        ThemeRegistry.getInstance().setTheme(themeId)
        
        android.util.Log.d("TextMateManager", "已应用主题: $themeName (ID: $themeId)")
    }
    
    /**
     * Get the file extension to language scope name mapping
     * @return Map of file extensions to scope names
     */
    fun getLanguageMapping(): Map<String, String> {
        return mapOf(
            "java" to "source.java",
            "kt" to "source.kotlin",
            "py" to "source.python",
            "cpp" to "source.cpp",
            "c" to "source.c",
            "h" to "source.c",
            "hpp" to "source.cpp",
            "js" to "source.js",
            "html" to "text.html.basic",
            "css" to "source.css",
            "md" to "text.html.markdown",
            "xml" to "text.xml",
            "json" to "source.json",
            "txt" to "text.plain"
        )
    }


}