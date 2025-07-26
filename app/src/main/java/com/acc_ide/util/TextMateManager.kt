package com.acc_ide.util

import android.content.Context
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.langs.textmate.registry.model.ThemeModel
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import org.eclipse.tm4e.core.registry.IThemeSource

/**
 * Utility class for managing TextMate integration in the editor
 */
object TextMateManager {
    private var isInitialized = false
    private const val THEME_PATH = "textmate/themes/"
    private const val DEFAULT_THEME = "light_plus"
    
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
        
        // Set default theme
        ThemeRegistry.getInstance().setTheme(DEFAULT_THEME)
        
        loadLanguages()
        
        isInitialized = true
    }
    
    /**
     * Load all available TextMate themes from assets
     */
    private fun loadThemes() {
        val themeRegistry = ThemeRegistry.getInstance()
        
        // List of themes to load
        val themes = listOf(
            "darcula" to true,
            "dark_plus" to true, 
            "dimmed-monokai-color-theme" to true,
            "light" to false,
            "light_plus" to false,
            "monokai-color-theme" to true,
            "solarized-dark-color-theme" to true,
            "solarized-light-color-theme" to false
        )
        
        // Load each theme
        themes.forEach { (themeName, isDark) ->
            val themeFile = "$THEME_PATH$themeName.json"
            try {
                // 获取输入流
                val inputStream = FileProviderRegistry.getInstance().tryGetInputStream(themeFile)
                if (inputStream != null) {
                    val source = IThemeSource.fromInputStream(inputStream, themeFile, null)
                    val model = ThemeModel(source, themeName)
                    model.isDark = isDark
                    themeRegistry.loadTheme(model)
                }
            } catch (e: Exception) {
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
     * Apply TextMate support to a code editor
     * @param editor The editor to apply TextMate to
     * @param languageScopeName The scope name for the language (e.g. "source.java")
     * @param enableCompletion Whether to enable auto-completion
     */
    fun applyTextMate(editor: CodeEditor, languageScopeName: String, enableCompletion: Boolean = true) {
        // Ensure TextMate is initialized
        if (!isInitialized) {
            throw IllegalStateException("TextMateManager must be initialized before applying to an editor")
        }
        
        // Set color scheme
        editor.colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
        
        // Set language
        val language = TextMateLanguage.create(languageScopeName, enableCompletion)
        editor.setEditorLanguage(language)
    }
    
    /**
     * Set the active TextMate theme
     * @param themeName The name of the theme to use
     */
    fun setTheme(themeName: String) {
        ThemeRegistry.getInstance().setTheme(themeName)
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