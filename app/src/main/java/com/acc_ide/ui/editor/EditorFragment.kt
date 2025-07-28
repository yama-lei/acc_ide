package com.acc_ide.ui.editor

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.core.view.MenuProvider
import com.google.android.material.button.MaterialButton
import com.acc_ide.R
import com.acc_ide.ui.main.MainActivity
import com.acc_ide.ui.iopanel.IOPanelFragment
import com.acc_ide.ui.settings.SettingsFragment
import com.acc_ide.util.TextMateManager
import com.acc_ide.view.SymbolPanelView
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import io.github.rosemoe.sora.widget.schemes.SchemeDarcula
import io.github.rosemoe.sora.widget.schemes.SchemeVS2019
import io.github.rosemoe.sora.event.ContentChangeEvent
import android.util.TypedValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay

class EditorFragment : Fragment() {
    lateinit var editor: CodeEditor
    private lateinit var language: String
    lateinit var fileName: String  // 修改为public属性

    // 添加一个变量来跟踪当前设置的语言作用域
    private var currentLanguageScopeName: String? = null
    private var isIOPanelOpen = false
    private var runMenuItem: MenuItem? = null
    private var undoMenuItem: MenuItem? = null
    private var redoMenuItem: MenuItem? = null
    private var hasUnsavedChanges = false
    private var isUpdatingFontSize = false // 防止缩放监听器和设置更新相互触发

    // Symbol panel components
    private lateinit var symbolPanel: SymbolPanelView
    private var isSymbolPanelVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // We'll set up the menu in onViewCreated instead of here
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_editor, container, false)
        editor = view.findViewById(R.id.editor_view)
        symbolPanel = view.findViewById(R.id.symbol_panel)

        // 获取参数
        arguments?.let {
            language = it.getString(ARG_LANGUAGE, "text")
            fileName = it.getString(ARG_FILENAME, "")
        }

        // 配置编辑器基本设置
        configureEditor()

        // 使用协程异步设置内容和语言支持
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 获取文件内容
                val mainActivity = activity as MainActivity
                val fileContent = mainActivity.files[fileName] ?: ""

                // 预加载语言支持 - 在后台线程进行
                withContext(Dispatchers.IO) {
                    val fileExtension = when (language) {
                        "java" -> "java"
                        "cpp" -> "cpp"
                        "python" -> "py"
                        else -> "txt"
                    }

                    val languageMapping = com.acc_ide.util.TextMateManager.getLanguageMapping()
                    val scopeName = languageMapping[fileExtension] ?: "text.plain"

                    // 预先创建语言实例，放入缓存
                    EditorFragment.getOrCreateLanguage(scopeName)
                    android.util.Log.d("EditorFragment", "预加载语言: $scopeName")
                }

                // 在主线程设置文本内容
                withContext(Dispatchers.Main) {
                    // 设置内容前先应用语言支持（预设语言，不处理内容）
                    setupLanguageSupport()

                    // 设置内容
                    editor.setText(fileContent)

                    // 内容设置后再次触发语法高亮刷新
                    editor.post {
                        // 强制编辑器重新计算布局和渲染
                        editor.invalidate()

                        // 尝试触发语法高亮的应用
                        val tempText = editor.text.toString()
                        if (tempText.isNotEmpty()) {
                            try {
                                // 在文本开头插入和删除一个空格，而不是末尾
                                editor.text.insert(0, 0, " ")
                                editor.text.delete(0, 1)
                            } catch (e: Exception) {
                                android.util.Log.e(
                                    "EditorFragment",
                                    "触发语法高亮渲染失败: ${e.message}"
                                )
                            }
                        }
                    }

                    // 设置文本变化监听器
                    setupContentChangeListener()

                    // 初始化符号面板
                    initSymbolPanel()
                }
            } catch (e: Exception) {
                android.util.Log.e("EditorFragment", "加载内容时出错: ${e.message}")
            }
        }

        // 使用TextMate主题提供的默认样式

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 设置菜单提供者 - 在视图创建后设置，确保可以安全访问 viewLifecycleOwner
        activity?.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear() // 清除旧菜单
                menuInflater.inflate(R.menu.editor_menu, menu)
                runMenuItem = menu.findItem(R.id.action_run)
                undoMenuItem = menu.findItem(R.id.action_undo)
                redoMenuItem = menu.findItem(R.id.action_redo)
                updateUndoRedoMenuState()
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.action_run -> {
                        if (isIOPanelOpen) {
                            closeIOPanel()
                        } else {
                            // 运行代码前先保存
                            saveContent()
                            openIOPanel()
                        }
                        true
                    }

                    R.id.action_undo -> {
                        performUndo()
                        true
                    }

                    R.id.action_redo -> {
                        performRedo()
                        true
                    }

                    else -> false
                }
            }
        }, viewLifecycleOwner)

        // 初始化界面视图
        initViews()

        // 配置语言支持
        configureLanguage()

        // 应用编辑器设置
        applySettingsFromPreferences()

        // 注册主题变更监听器
        registerThemeChangeListener()

        // 配置自动补全组件的颜色
        configureAutoCompletionColors()

        // 设置自动补全组件的启用状态
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val autoCompletionEnabled =
            prefs.getBoolean(SettingsFragment.PREF_ENABLE_AUTO_COMPLETION, true)
        setAutoCompletionEnabled(autoCompletionEnabled)

        // 确保设置正确应用
        applySettingsFromPreferences()

        // 延迟设置光标宽度，确保在编辑器完全初始化后应用
        editor.post {
            val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val cursorWidth = prefs.getFloat(
                SettingsFragment.PREF_CURSOR_WIDTH,
                SettingsFragment.DEFAULT_CURSOR_WIDTH
            )
            setCursorWidth(cursorWidth)
        }
    }

    override fun onResume() {
        super.onResume()

        // 确保标题栏显示当前文件名
        (activity as? MainActivity)?.let { mainActivity ->
            mainActivity.supportActionBar?.title = fileName
        }

        // 应用设置
        applySettingsFromPreferences()

        // 强制刷新主题和语法高亮
        refreshEditorTheme()

        // 延迟一段时间后再次刷新，确保主题和语法高亮正确应用
        Handler(Looper.getMainLooper()).postDelayed({
            // 重新应用语言支持
            setupLanguageSupport()

            // 刷新编辑器主题
            refreshEditorTheme()

            android.util.Log.d("EditorFragment", "onResume中延迟刷新语法高亮完成")
        }, 200)
    }

    /**
     * 从SharedPreferences读取设置并应用到编辑器和符号面板
     */
    private fun applySettingsFromPreferences() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val symbolPanelEnabled = prefs.getBoolean(SettingsFragment.PREF_ENABLE_SYMBOL_PANEL, true)

        // 应用到符号面板
        setSymbolPanelVisibility(symbolPanelEnabled)
        isSymbolPanelVisible = symbolPanelEnabled

        // 应用光标宽度设置
        val cursorWidth = prefs.getFloat(
            SettingsFragment.PREF_CURSOR_WIDTH,
            SettingsFragment.DEFAULT_CURSOR_WIDTH
        )
        updateCursorWidth(cursorWidth)
    }

    /**
     * 初始化符号面板
     */
    private fun initSymbolPanel() {
        try {
            // 设置编辑器实例给符号面板
            symbolPanel.setEditor(editor)

            // 设置初始可见性
            setSymbolPanelVisibility(isSymbolPanelVisible)

        } catch (e: Exception) {
            android.util.Log.e("EditorFragment", "初始化符号面板失败: ${e.message}")
        }
    }

    /**
     * 设置符号面板的可见性
     */
    private fun setSymbolPanelVisibility(visible: Boolean) {
        symbolPanel.visibility = if (visible) View.VISIBLE else View.GONE
    }

    /**
     * 初始化界面视图
     * 此方法在Fragment视图创建后被调用，用于设置视图相关组件
     */
    private fun initViews() {
        try {
            // 初始化编辑器视图的其他UI元素（如工具栏、按钮等）
            // 在此处添加编辑器相关的视图初始化代码

            editor.setOnLongClickListener {
                // 显示上下文菜单或者执行其他操作
                true
            }

            android.util.Log.d("EditorFragment", "视图初始化完成")
        } catch (e: Exception) {
            android.util.Log.e("EditorFragment", "初始化视图时出错: ${e.message}")
        }
    }

    /**
     * 配置语言支持
     * 此方法用于根据文件类型配置编辑器的语言支持
     */
    private fun configureLanguage() {
        try {
            // 确保editor已经初始化
            if (::editor.isInitialized) {
                // 如果文件类型没有变化，无需重新配置
                val fileExtension = when (language) {
                    "java" -> "java"
                    "cpp" -> "cpp"
                    "python" -> "py"
                    else -> "txt"
                }

                val languageMapping = com.acc_ide.util.TextMateManager.getLanguageMapping()
                val scopeName = languageMapping[fileExtension] ?: "text.plain"

                // 如果语言没有变化，则跳过重复的语言设置
                if (currentLanguageScopeName == scopeName) {
                    android.util.Log.d("EditorFragment", "语言未变化，跳过重新配置：$scopeName")
                    return
                }

                // 设置语言
                setupLanguageSupport()

                // 设置语言特定的编辑器选项
                when (language) {
                    "java", "kotlin" -> {
                        // Java/Kotlin特定设置
                        try {
                            // 使用反射设置自动缩进，因为不同版本的编辑器可能有不同的API
                            val method = CodeEditor::class.java.getDeclaredMethod(
                                "setAutoIndentEnabled",
                                Boolean::class.java
                            )
                            method.invoke(editor, true)
                        } catch (e: Exception) {
                            android.util.Log.w("EditorFragment", "无法设置自动缩进: ${e.message}")
                        }
                    }

                    "python" -> {
                        // Python特定设置
                        try {
                            val method = CodeEditor::class.java.getDeclaredMethod(
                                "setAutoIndentEnabled",
                                Boolean::class.java
                            )
                            method.invoke(editor, true)
                        } catch (e: Exception) {
                            android.util.Log.w("EditorFragment", "无法设置自动缩进: ${e.message}")
                        }
                    }

                    else -> {
                        // 默认设置
                        try {
                            val method = CodeEditor::class.java.getDeclaredMethod(
                                "setAutoIndentEnabled",
                                Boolean::class.java
                            )
                            method.invoke(editor, false)
                        } catch (e: Exception) {
                            android.util.Log.w("EditorFragment", "无法设置自动缩进: ${e.message}")
                        }
                    }
                }

                android.util.Log.d("EditorFragment", "已为${language}语言配置编辑器")
            }
        } catch (e: Exception) {
            android.util.Log.e("EditorFragment", "配置语言支持时出错: ${e.message}")
        }
    }

    private fun setupContentChangeListener() {
        // 监听文本变化以标记未保存状态
        try {
            editor.subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
                hasUnsavedChanges = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveContent() {
        val content = editor.text.toString()
        val mainActivity = activity as? MainActivity
        mainActivity?.let {
            // 更新内存中的文件内容
            it.files[fileName] = content

            // 同时更新存储
            it.updateFileContent(fileName, content)

            // 重置未保存状态
            hasUnsavedChanges = false
        }
    }

    private fun openIOPanel() {
        // 检查是否已经有IO面板打开
        val existingPanel = parentFragmentManager.findFragmentByTag("io_panel")
        if (existingPanel != null) {
            // 如果已经有面板打开，先移除它
            parentFragmentManager.beginTransaction()
                .remove(existingPanel)
                .commit()
        }

        // 创建并显示IO面板
        val ioPanel = IOPanelFragment.newInstance(fileName, language)

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(
                R.anim.slide_in_right,
                R.anim.slide_out_left,
                R.anim.slide_in_left,
                R.anim.slide_out_right
            )
            .add(R.id.content_frame, ioPanel, "io_panel")
            .addToBackStack("io_panel")
            .commit()

        // 更新状态和图标
        isIOPanelOpen = true
        updateRunMenuIcon()
    }

    private fun closeIOPanel() {
        // 查找IO面板
        val ioPanel = parentFragmentManager.findFragmentByTag("io_panel")
        if (ioPanel != null) {
            // 移除IO面板
            parentFragmentManager.beginTransaction()
                .setCustomAnimations(
                    R.anim.slide_in_left,
                    R.anim.slide_out_right
                )
                .remove(ioPanel)
                .commit()

            // 弹出回退栈
            parentFragmentManager.popBackStack()

            // 确保Activity恢复正确的导航栏状态
            (activity as? MainActivity)?.let { mainActivity ->
                // 延迟执行，确保Fragment转换完成
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    // 直接调用MainActivity的导航栏更新方法
                    mainActivity.updateNavigationIcon()
                }, 100)
            }
        }

        // 更新状态和图标
        isIOPanelOpen = false
        updateRunMenuIcon()
    }

    private fun updateRunMenuIcon() {
        runMenuItem?.let {
            if (isIOPanelOpen) {
                it.setIcon(android.R.drawable.ic_menu_close_clear_cancel)
                it.setTitle(R.string.close)
            } else {
                it.setIcon(android.R.drawable.ic_media_play)
                it.setTitle(R.string.run)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // 当Fragment暂停时，只有在有未保存更改时才保存内容
        if (hasUnsavedChanges) {
            saveContent()
        }
    }

    /**
     * 配置编辑器属性
     * 设置编辑器的外观、行为和功能
     */
    private fun configureEditor() {
        // 设置编辑器属性
        editor.apply {
            // 基本设置
            isWordwrap = true  // 启用自动换行

            // 缩进设置
            tabWidth = 4  // 设置制表符宽度为4个空格

            // UI设置
            isLineNumberEnabled = true  // 显示行号

            // 设置自定义字体
            try {
                val fontPath = "fonts/AgaveNerdFontMono-Regular.ttf"
                val typeface = android.graphics.Typeface.createFromAsset(context.assets, fontPath)
                setTypefaceText(typeface)
                android.util.Log.d("EditorFragment", "已应用自定义字体: $fontPath")
            } catch (e: Exception) {
                android.util.Log.e("EditorFragment", "设置自定义字体失败: ${e.message}")
                e.printStackTrace()
            }

            // 撤销/重做
            isUndoEnabled = true  // 启用撤销/重做功能

            // 设置内容变化监听器
            subscribeEvent(ContentChangeEvent::class.java) { _, _ ->
                // 标记有未保存的更改
                hasUnsavedChanges = true

                // 更新撤销/重做按钮状态
                updateUndoRedoMenuState()
            }

            // 启用语法高亮
            setupLanguageSupport()

            // 从应用偏好设置获取保存的字体大小
            val sharedPrefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
            val fontSize = sharedPrefs.getFloat(
                SettingsFragment.PREF_FONT_SIZE,
                SettingsFragment.DEFAULT_FONT_SIZE
            )

            // 设置编辑器字体大小
            setTextSize(fontSize)

            // 设置光标宽度 - 从设置中读取
            val cursorWidth = sharedPrefs.getFloat(
                SettingsFragment.PREF_CURSOR_WIDTH,
                SettingsFragment.DEFAULT_CURSOR_WIDTH
            )
            setCursorWidth(cursorWidth)

            // 启用缩放功能，允许用户通过手势调整字体大小
            isScalable = true

            // 设置美化选项：显示空白字符
            nonPrintablePaintingFlags = CodeEditor.FLAG_DRAW_WHITESPACE_LEADING or
                    CodeEditor.FLAG_DRAW_WHITESPACE_IN_SELECTION

            // 启用代码块指示线 - 视觉上区分代码块
            isBlockLineEnabled = true

            // 自动完成面板样式由TextMateManager处理
        }
    }

    /**
     * 注册主题变更监听器，在主题变更时重新应用自动完成面板样式
     */
    private fun registerThemeChangeListener() {
        try {
            // 获取当前Activity的Configuration对象
            val activity = requireActivity()

            // 创建Configuration监听器
            val callback = object : android.content.ComponentCallbacks {
                override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
                    try {
                        // 检查是否是UI模式变化（深色/浅色模式切换）
                        if (newConfig.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK !=
                            resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                        ) {

                            // 应用TextMate配色方案
                            editor.colorScheme =
                                io.github.rosemoe.sora.langs.textmate.TextMateColorScheme.create(
                                    io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry.getInstance()
                                )

                            // 配置自动补全组件的颜色
                            configureAutoCompletionColors()

                            // 强制刷新编辑器
                            editor.invalidate()
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("EditorFragment", "处理主题变更失败: ${e.message}")
                    }
                }

                override fun onLowMemory() {
                    // 不需要处理
                }
            }

            // 注册监听器
            activity.registerComponentCallbacks(callback)

            // 在Fragment销毁时取消注册
            view?.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(v: View) {
                    // 不需要处理
                }

                override fun onViewDetachedFromWindow(v: View) {
                    activity.unregisterComponentCallbacks(callback)
                }
            })
        } catch (e: Exception) {
            android.util.Log.e("EditorFragment", "注册主题变更监听器失败: ${e.message}")
        }
    }

    /**
     * 设置语言支持
     * 此方法用于配置TextMate语言支持和语法高亮
     */
    fun setupLanguageSupport(specifiedLanguage: String? = null) {
        try {
            // 使用TextMate语法高亮
            val activity = requireActivity()
            val fileExtension = when (specifiedLanguage ?: language) {
                "java" -> "java"
                "cpp" -> "cpp"
                "python" -> "py"
                else -> "txt"
            }

            // 获取文件扩展名对应的语言作用域
            val languageMapping = com.acc_ide.util.TextMateManager.getLanguageMapping()
            val scopeName = languageMapping[fileExtension] ?: "text.plain"

            // 如果语言没有变化且未指定特定语言，跳过配置
            if (currentLanguageScopeName == scopeName && specifiedLanguage == null) {
                android.util.Log.d("EditorFragment", "跳过重复的语言设置: $scopeName")
                return
            }

            // 更新当前语言作用域
            currentLanguageScopeName = scopeName

            // 使用缓存获取TextMateLanguage实例
            val language = EditorFragment.getOrCreateLanguage(scopeName)

            // 应用TextMate配色方案
            editor.colorScheme = io.github.rosemoe.sora.langs.textmate.TextMateColorScheme.create(
                io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry.getInstance()
            )

            // 设置语言
            editor.setEditorLanguage(language)

            // 配置自动补全组件的颜色
            configureAutoCompletionColors()

            // 强制刷新编辑器以确保语法高亮立即应用
            editor.postInvalidate()

            // 通过微小改变和撤销来触发语法高亮渲染
            editor.post {
                try {
                    // 获取当前文本
                    val text = editor.text.toString()

                    // 如果文本不为空，尝试通过小的编辑操作触发重新渲染
                    if (text.isNotEmpty()) {
                        // 记住当前光标位置
                        val cursorPos = editor.cursor.left

                        // 安全地在文本开头插入和删除一个空格
                        editor.text.insert(0, 0, " ")
                        editor.text.delete(0, 1)

                        // 尝试恢复原来的光标位置
                        try {
                            if (cursorPos >= 0 && cursorPos < editor.text.length) {
                                editor.setSelection(cursorPos, cursorPos)
                            }
                        } catch (e: Exception) {
                            android.util.Log.w("EditorFragment", "恢复光标位置失败: ${e.message}")
                        }

                        // 强制重绘
                        editor.invalidate()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("EditorFragment", "触发语法高亮重新渲染失败: ${e.message}")
                }
            }

            android.util.Log.d(
                "EditorFragment",
                "已设置${specifiedLanguage ?: this.language}语言支持(TextMate: $scopeName)"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // 发生错误时退回到空语言解析器，确保编辑器仍可用
            editor.setEditorLanguage(io.github.rosemoe.sora.lang.EmptyLanguage())
            android.util.Log.e(
                "EditorFragment",
                "设置语言支持时出错: ${e.message}，已退回到空语言解析器"
            )
        }
    }

    /**
     * 更新编辑器字体大小
     *
     * @param fontSize 要设置的字体大小
     */
    fun updateFontSize(fontSize: Float) {
        if (::editor.isInitialized && !isUpdatingFontSize) {
            isUpdatingFontSize = true

            try {
                // 设置编辑器字体大小
                editor.setTextSize(fontSize)

                // 通知MainActivity保存设置
                (activity as? MainActivity)?.let { mainActivity ->
                    val prefs = PreferenceManager.getDefaultSharedPreferences(mainActivity)
                    prefs.edit().putFloat(SettingsFragment.PREF_FONT_SIZE, fontSize).apply()
                }
            } catch (e: Exception) {
                android.util.Log.e("EditorFragment", "更新字体大小失败: ${e.message}")
            }

            // 防止无限循环
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                isUpdatingFontSize = false
            }, 100)
        }
    }

    /**
     * 设置光标宽度
     * 使用有效方法设置光标宽度
     * @param widthInDp 光标宽度（单位：dp）
     */
    private fun setCursorWidth(widthInDp: Float) {
        try {
            android.util.Log.d("EditorFragment", "尝试设置光标宽度为 $widthInDp dp")

            // 尝试使用setCursorWidth方法
            try {
                val setCursorWidthMethod =
                    CodeEditor::class.java.getDeclaredMethod("setCursorWidth", Float::class.java)
                setCursorWidthMethod.isAccessible = true
                setCursorWidthMethod.invoke(editor, widthInDp)
                android.util.Log.d("EditorFragment", "方法2：通过方法设置光标宽度成功")
            } catch (e: Exception) {
                // 如果方法2失败，尝试方法6
                try {
                    val methods = CodeEditor::class.java.declaredMethods
                    for (method in methods) {
                        if (method.name.contains("cursor", ignoreCase = true) &&
                            method.name.contains("width", ignoreCase = true) &&
                            method.parameterTypes.size == 1 &&
                            (method.parameterTypes[0] == Float::class.java ||
                                    method.parameterTypes[0] == Int::class.java)
                        ) {

                            method.isAccessible = true
                            val widthParam = if (method.parameterTypes[0] == Float::class.java) {
                                widthInDp
                            } else {
                                widthInDp.toInt()
                            }
                            method.invoke(editor, widthParam)
                            android.util.Log.d(
                                "EditorFragment",
                                "方法6：通过方法 ${method.name} 设置光标宽度成功"
                            )
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.w("EditorFragment", "设置光标宽度失败: ${e.message}")
                }
            }

            // 强制刷新编辑器，确保更改生效
            editor.postInvalidate()

        } catch (e: Exception) {
            android.util.Log.e("EditorFragment", "设置光标宽度失败: ${e.message}")
        }
    }

    /**
     * 更新编辑器光标宽度
     *
     * @param widthInDp 要设置的光标宽度（单位：dp）
     */
    fun updateCursorWidth(widthInDp: Float) {
        if (::editor.isInitialized) {
            try {
                // 设置光标宽度
                setCursorWidth(widthInDp)

                // 保存设置到SharedPreferences
                (activity as? MainActivity)?.let { mainActivity ->
                    val prefs = PreferenceManager.getDefaultSharedPreferences(mainActivity)
                    prefs.edit().putFloat(SettingsFragment.PREF_CURSOR_WIDTH, widthInDp).apply()
                }

                android.util.Log.d("EditorFragment", "光标宽度已更新为 $widthInDp dp")
            } catch (e: Exception) {
                android.util.Log.e("EditorFragment", "更新光标宽度失败: ${e.message}")
            }
        }
    }

    /**
     * 设置自动补全组件的启用状态
     * @param enabled 是否启用自动补全组件
     */
    fun setAutoCompletionEnabled(enabled: Boolean) {
        if (::editor.isInitialized) {
            try {
                // 获取自动补全组件
                val autoCompletion =
                    editor.getComponent(io.github.rosemoe.sora.widget.component.EditorAutoCompletion::class.java)

                // 设置组件是否启用
                autoCompletion.isEnabled = enabled

                android.util.Log.d("EditorFragment", "自动补全组件状态已设置: $enabled")
            } catch (e: Exception) {
                android.util.Log.e("EditorFragment", "设置自动补全组件状态失败: ${e.message}")
            }
        }
    }

    /**
     * 执行撤销操作
     */
    private fun performUndo() {
        try {
            if (::editor.isInitialized && editor.canUndo()) {
                editor.undo()
                updateUndoRedoMenuState()
            } else {
                Toast.makeText(context, "无法撤销", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("EditorFragment", "撤销操作失败: ${e.message}")
            Toast.makeText(context, "撤销操作失败", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 执行重做操作
     */
    private fun performRedo() {
        try {
            if (::editor.isInitialized && editor.canRedo()) {
                editor.redo()
                updateUndoRedoMenuState()
            } else {
                Toast.makeText(context, "无法重做", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.util.Log.e("EditorFragment", "重做操作失败: ${e.message}")
            Toast.makeText(context, "重做操作失败", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 更新撤销/重做菜单项状态
     */
    private fun updateUndoRedoMenuState() {
        undoMenuItem?.isEnabled = editor.canUndo()
        redoMenuItem?.isEnabled = editor.canRedo()

        // 可选：根据启用状态调整图标的透明度
        undoMenuItem?.icon?.alpha = if (editor.canUndo()) 255 else 128
        redoMenuItem?.icon?.alpha = if (editor.canRedo()) 255 else 128
    }

    /**
     * 根据文件名获取适合的语言类型
     * @param fileName 文件名
     * @return 对应的语言类型（java、cpp、python或text）
     */
    fun getLanguageForFile(fileName: String): String {
        return when {
            fileName.endsWith(".java") -> "java"
            fileName.endsWith(".cpp") || fileName.endsWith(".c") || fileName.endsWith(".h") || fileName.endsWith(
                ".hpp"
            ) -> "cpp"

            fileName.endsWith(".py") -> "python"
            else -> "text"
        }
    }

    /**
     * 确保语言配置被正确应用
     * 当从设置页面返回或语言配置可能丢失时使用
     */
    fun ensureLanguageApplied() {
        if (::editor.isInitialized) {
            try {
                // 检查当前是否有应用语言
                val hasLanguageSet = editor.editorLanguage !is io.github.rosemoe.sora.lang.EmptyLanguage

                // 如果未设置语言或使用的是EmptyLanguage，重新应用语言支持
                if (!hasLanguageSet) {
                    android.util.Log.d("EditorFragment", "检测到编辑器未应用语言支持，正在重新应用")
                    setupLanguageSupport()
                } else {
                    android.util.Log.d("EditorFragment", "编辑器已应用语言支持，无需重新应用")
                }
            } catch (e: Exception) {
                android.util.Log.e("EditorFragment", "确保语言应用时出错: ${e.message}")
                // 发生错误时尝试重新应用语言
                setupLanguageSupport()
            }
        }
    }

    /**
     * 强制刷新编辑器主题和语法高亮
     * 当主题发生变化时调用此方法以确保编辑器立即更新
     */
    fun refreshEditorTheme() {
        if (::editor.isInitialized) {
            try {
                // 确保语言配置被正确应用
                ensureLanguageApplied()

                // 更新颜色方案
                editor.colorScheme =
                    io.github.rosemoe.sora.langs.textmate.TextMateColorScheme.create(
                        io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry.getInstance()
                    )

                // 配置自动补全组件的颜色
                configureAutoCompletionColors()

                // 强制重绘编辑器
                editor.invalidate()
                editor.postInvalidate()

                // 尝试通过微小编辑操作触发完全刷新
                val text = editor.text.toString()
                if (text.isNotEmpty()) {
                    val cursorPos = editor.cursor.left

                    // 在文本开头插入和删除一个空格以触发重新渲染
                    editor.text.insert(0, 0, " ")
                    editor.text.delete(0, 1)

                    // 恢复光标位置
                    if (cursorPos >= 0 && cursorPos < editor.text.length) {
                        editor.setSelection(cursorPos, cursorPos)
                    }

                    // 再次强制重绘
                    editor.invalidate()
                }

                android.util.Log.d("EditorFragment", "编辑器主题刷新完成")
            } catch (e: Exception) {
                android.util.Log.e("EditorFragment", "刷新编辑器主题失败: ${e.message}")
            }
        }
    }

    /**
     * 配置自动补全组件的颜色
     * 根据当前主题模式（深色/浅色）设置不同的颜色
     */
    private fun configureAutoCompletionColors() {
        try {
            // 检测当前主题模式
            val isDarkMode = resources.configuration.uiMode and
                    android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
                    android.content.res.Configuration.UI_MODE_NIGHT_YES

            // 获取编辑器的颜色方案
            val scheme = editor.colorScheme

            if (isDarkMode) {
                // 深色模式颜色
                scheme.setColor(
                    io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMPLETION_WND_BACKGROUND,
                    0xFF2C2C2C.toInt()
                )
                scheme.setColor(
                    io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMPLETION_WND_CORNER,
                    0xFF3A3D41.toInt()
                )
                scheme.setColor(
                    io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY,
                    0xFFEEEEEE.toInt()
                )
                scheme.setColor(
                    io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY,
                    0xFFAAAAAA.toInt()
                )
                scheme.setColor(
                    io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMPLETION_WND_ITEM_CURRENT,
                    0xFF3A3D41.toInt()
                )
            } else {
                // 浅色模式颜色
                scheme.setColor(
                    io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMPLETION_WND_BACKGROUND,
                    0xFFFEF7FF.toInt()
                )
                scheme.setColor(
                    io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMPLETION_WND_CORNER,
                    0xFFE6D7F2.toInt()
                )
                scheme.setColor(
                    io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY,
                    0xFF000000.toInt()
                )
                scheme.setColor(
                    io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY,
                    0xFF666666.toInt()
                )
                scheme.setColor(
                    io.github.rosemoe.sora.widget.schemes.EditorColorScheme.COMPLETION_WND_ITEM_CURRENT,
                    0xFFE6D7F2.toInt()
                )
            }

            android.util.Log.d("EditorFragment", "已配置自动补全组件颜色，深色模式: $isDarkMode")
        } catch (e: Exception) {
            android.util.Log.e("EditorFragment", "配置自动补全组件颜色失败: ${e.message}")
            e.printStackTrace()
        }
    }

    companion object {
        private const val ARG_LANGUAGE = "language"
        private const val ARG_FILENAME = "filename"

        // 缓存各语言的TextMateLanguage实例
        private val languageCache =
            mutableMapOf<String, io.github.rosemoe.sora.langs.textmate.TextMateLanguage>()

        // 获取或创建语言实例
        fun getOrCreateLanguage(scopeName: String): io.github.rosemoe.sora.langs.textmate.TextMateLanguage {
            return languageCache.getOrPut(scopeName) {
                android.util.Log.d("EditorFragment", "创建新TextMateLanguage实例: $scopeName")
                io.github.rosemoe.sora.langs.textmate.TextMateLanguage.create(scopeName, true)
            }
        }

        @JvmStatic
        fun newInstance(language: String, fileName: String) =
            EditorFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_LANGUAGE, language)
                    putString(ARG_FILENAME, fileName)
                }
            }
    }
} 