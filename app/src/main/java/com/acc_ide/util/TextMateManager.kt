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
 * TextMate syntax highlighting manager for code editor theme and language support
 * TextMate语法高亮管理器 - 用于代码编辑器主题和语言支持
 */
object TextMateManager {
    private var isInitialized = false
    private const val THEME_PATH = "textmate/themes/"
    private const val THEME_DARK = "dark.json" 
    private const val THEME_LIGHT = "light.json"  
    
    /**
     * Initialize TextMate support - should be called once when application starts
     * 初始化TextMate支持 - 应该在应用程序启动时调用一次
     */
    fun initialize(context: Context) {
        if (isInitialized) return
        
        // Setup file resolver for assets
        FileProviderRegistry.getInstance().addFileProvider(
            AssetsFileResolver(context.applicationContext.assets)
        )
        loadThemes()
        
        // Check current system theme, automatically select corresponding theme
        val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        val defaultThemeFile = if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            THEME_DARK 
        } else {
            THEME_LIGHT 
        }
        
        // Set default theme ID (without .json extension)
        val themeId = defaultThemeFile.substringBeforeLast(".json")
        ThemeRegistry.getInstance().setTheme(themeId)
        android.util.Log.d("TextMateManager", "Set default theme during initialization: $defaultThemeFile (ID: $themeId)")
        
        loadLanguages()
        
        isInitialized = true
    }
    
    /**
     * Load all available TextMate themes from assets
     * 从assets加载所有可用的TextMate主题
     */
    private fun loadThemes() {
        val themeRegistry = ThemeRegistry.getInstance()
        
        // List of themes to load with their filenames and dark mode flag
        val themes = listOf(
            THEME_DARK to true,  
            THEME_LIGHT to false 
        )
        
        // Load each theme
        themes.forEach { (themeFile, isDark) ->
            try {
                // Get input stream
                val inputStream = FileProviderRegistry.getInstance().tryGetInputStream(THEME_PATH + themeFile)
                if (inputStream != null) {
                    val themeSource = IThemeSource.fromInputStream(inputStream, themeFile, null)
                    // Load theme
                    themeRegistry.loadTheme(themeSource, isDark)
                    
                    // Log successfully loaded theme
                    android.util.Log.d("TextMateManager", "Theme loaded successfully: $themeFile")
                }
            } catch (e: Exception) {
                android.util.Log.e("TextMateManager", "Theme loading failed: $themeFile - ${e.message}")
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Load TextMate language definitions from language.json file in assets
     * 从assets中的language.json文件加载TextMate语言定义
     */
    private fun loadLanguages() {
        GrammarRegistry.getInstance().loadGrammars("textmate/languages/language.json")
    }
    

    /**
     * Set active TextMate theme for editor
     * 设置编辑器的活动TextMate主题
     * @param themeName The name of the theme to use 
     */
    fun setTheme(themeName: String) {
        val themeId = themeName.substringBeforeLast(".json")
        ThemeRegistry.getInstance().setTheme(themeId)
        
        android.util.Log.d("TextMateManager", "Applied theme: $themeName (ID: $themeId)")
    }
    
    /**
     * Get file extension to language scope name mapping for syntax highlighting
     * 获取文件扩展名到语言作用域名称的映射用于语法高亮
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