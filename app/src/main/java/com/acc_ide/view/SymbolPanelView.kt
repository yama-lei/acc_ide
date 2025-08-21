package com.acc_ide.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.content.res.AppCompatResources
import androidx.viewpager2.widget.ViewPager2
import com.acc_ide.R
import com.google.android.material.button.MaterialButton
import io.github.rosemoe.sora.widget.CodeEditor
import android.widget.TextView
import android.view.Gravity
import android.util.TypedValue
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Build
import android.view.inputmethod.EditorInfo

/**
 * Symbol panel custom view for providing programming symbol input functionality
 * 符号面板自定义视图 - 提供编程符号输入功能，方便用户在移动设备上输入特殊字符
 */
class SymbolPanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val viewPager: ViewPager2
    private val pageIndicator: LinearLayout
    private var editor: CodeEditor? = null
    private var currentPage = 0
    private val pageCount = 2

    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_symbol_panel, this, true)
        
        // Initialize views
        viewPager = findViewById(R.id.symbol_view_pager)
        pageIndicator = findViewById(R.id.page_indicator)
        
        // Setup ViewPager adapter
        setupViewPager()
    }
    
    /**
     * Set editor instance for symbol insertion
     * 设置编辑器实例
     */
    fun setEditor(editor: CodeEditor) {
        this.editor = editor
    }
    
    /**
     * Setup ViewPager with symbol pages
     * 设置ViewPager
     */
    private fun setupViewPager() {
        viewPager.adapter = SymbolPagerAdapter()
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPage = position
                updatePageIndicator()
            }
        })
        
        // Initialize page indicator
        setupPageIndicator()
    }
    
    /**
     * Setup page indicator dots
     * 设置页面指示器
     */
    private fun setupPageIndicator() {
        pageIndicator.removeAllViews()
        
        for (i in 0 until pageCount) {
            val indicator = View(context).apply {
                layoutParams = LayoutParams(12, 12).apply {
                    setMargins(6, 0, 6, 0)
                }
                background = AppCompatResources.getDrawable(context, R.drawable.page_indicator)
                isSelected = i == currentPage
            }
            pageIndicator.addView(indicator)
        }
    }
    
    /**
     * Update page indicator selection state
     * 更新页面指示器
     */
    private fun updatePageIndicator() {
        for (i in 0 until pageIndicator.childCount) {
            val indicator = pageIndicator.getChildAt(i)
            indicator.isSelected = i == currentPage
        }
    }
    
    /**
     * Symbol pager adapter for managing symbol pages
     * 符号页面适配器
     */
    private inner class SymbolPagerAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<SymbolPagerAdapter.SymbolPageViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SymbolPageViewHolder {
            val pageView = LayoutInflater.from(parent.context)
                .inflate(R.layout.view_symbol_page, parent, false)
                
            // Ensure page view fills the ViewPager2
            pageView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
                
            return SymbolPageViewHolder(pageView)
        }
        
        override fun onBindViewHolder(holder: SymbolPageViewHolder, position: Int) {
            when (position) {
                0 -> holder.setupFirstPage()
                1 -> holder.setupSecondPage()
            }
        }
        
        override fun getItemCount(): Int = pageCount
        
        /**
         * Symbol page ViewHolder for displaying symbol buttons
         * 符号页面ViewHolder
         */
        inner class SymbolPageViewHolder(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {
            private val row1: LinearLayout = itemView.findViewById(R.id.symbol_buttons_row1)
            private val row2: LinearLayout = itemView.findViewById(R.id.symbol_buttons_row2)
            
            /**
             * Setup first page symbols including brackets and navigation
             * 设置第一页符号
             */
            fun setupFirstPage() {
                // Clear existing buttons
                row1.removeAllViews()
                row2.removeAllViews()
                
                // First row symbols
                val row1Symbols = listOf(
                    "TAB" to "\t",
                    "(" to "(",
                    ")" to ")",
                    "[" to "[",
                    "]" to "]",
                    "{" to "{",
                    "}" to "}",
                    "↑" to "UP",
                    "=" to "="
                )
                
                // Second row symbols
                val row2Symbols = listOf(
                    ";" to ";",
                    "<" to "<",
                    ">" to ">",
                    "-" to "-",
                    "+" to "+",
                    "," to ",",
                    "←" to "LEFT",
                    "↓" to "DOWN",
                    "→" to "RIGHT"
                )
                
                // Add first row symbol buttons
                for ((label, value) in row1Symbols) {
                    addSymbolButton(row1, label, value)
                }
                
                // Add second row symbol buttons
                for ((label, value) in row2Symbols) {
                    addSymbolButton(row2, label, value)
                }
            }
            
            /**
             * Setup second page symbols including operators and special characters
             * 设置第二页符号
             */
            fun setupSecondPage() {
                // Clear existing buttons
                row1.removeAllViews()
                row2.removeAllViews()
                
                // First row symbols
                val row1Symbols = listOf(
                    "*" to "*",
                    "/" to "/",
                    "\\" to "\\",
                    "?" to "?",
                    "|" to "|",
                    "," to ",",
                    "." to ".",
                    "^" to "^",
                    "&" to "&"
                )
                
                // Second row symbols
                val row2Symbols = listOf(
                    "@" to "@",
                    "!" to "!",
                    "'" to "'",
                    "\"" to "\"",
                    "_" to "_",
                    "#" to "#",
                    "$" to "$",
                    "%" to "%",
                    ":" to ":"
                )
                
                // Add first row symbol buttons
                for ((label, value) in row1Symbols) {
                    addSymbolButton(row1, label, value)
                }
                
                // Add second row symbol buttons
                for ((label, value) in row2Symbols) {
                    addSymbolButton(row2, label, value)
                }
            }
        }
    }
    
    /**
     * Provide haptic feedback when button is pressed
     * 按钮按下时提供触觉反馈
     */
    private fun performHapticFeedback() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            
            vibrator?.let { vib ->
                if (vib.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vib.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vib.vibrate(50)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SymbolPanelView", "Failed to provide haptic feedback: ${e.message}")
        }
    }
    
    /**
     * Add symbol button to parent layout with click handling
     * 添加符号按钮
     */
    private fun addSymbolButton(parent: LinearLayout, label: String, value: String) {
        val textView = TextView(context).apply {
            text = label
            setTextAppearance(R.style.SymbolTextStyle)
            gravity = Gravity.CENTER
            minHeight = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 34f, resources.displayMetrics
            ).toInt()
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(2, 2, 2, 2)
            }
            // Add ripple click feedback
            val typedArray = context.obtainStyledAttributes(intArrayOf(android.R.attr.selectableItemBackground))
            foreground = typedArray.getDrawable(0)
            typedArray.recycle()
            setOnClickListener {
                performHapticFeedback()
                when (value) {
                    "LEFT" -> moveCursor(-1, 0)
                    "RIGHT" -> moveCursor(1, 0)
                    "UP" -> moveCursor(0, -1)
                    "DOWN" -> moveCursor(0, 1)
                    else -> insertText(value)
                }
            }
        }
        parent.addView(textView)
    }
    
    
    /**
     * Insert text at current cursor position using keyboard simulation
     * 通过模拟键盘输入插入文本，以触发自动补全
     */
    private fun insertText(text: String) {
        try {
            editor?.let { editor ->
                // Use keyboard simulation to trigger auto-completion
                simulateKeyboardInput(text)
            }
        } catch (e: Exception) {
            android.util.Log.e("SymbolPanelView", "Failed to insert text: ${e.message}")
        }
    }
    
    /**
     * Simulate keyboard input to trigger auto-completion
     * 模拟键盘输入以触发自动补全
     */
    private fun simulateKeyboardInput(text: String) {
        try {
            editor?.let { editor ->
                // Method 1: Use input connection to simulate typing
                try {
                    val editorInfo = EditorInfo()
                    val inputConnection = editor.onCreateInputConnection(editorInfo)
                    inputConnection?.let { ic ->
                        // Simulate typing the character
                        ic.commitText(text, 1)
                        android.util.Log.d("SymbolPanelView", "Successfully inserted '$text' via input connection")
                        return
                    }
                } catch (e: Exception) {
                    android.util.Log.d("SymbolPanelView", "Input connection method failed: ${e.message}")
                }
                
                // Method 2: Check if bracket characters need auto-completion handling
                if (isBracketCharacter(text)) {
                    insertBracketWithAutoCompletion(text)
                    return
                }
                
                // Method 3: Fallback to direct insertion for other characters
                fallbackInsertText(text)
            }
        } catch (e: Exception) {
            android.util.Log.e("SymbolPanelView", "Failed to simulate keyboard input: ${e.message}")
            // Fallback to direct insertion if simulation fails
            fallbackInsertText(text)
        }
    }
    
    /**
     * Check if character needs auto-completion
     * 检查字符是否需要自动补全
     */
    private fun isBracketCharacter(text: String): Boolean {
        return text.length == 1 && text[0] in setOf('(', '[', '{', ')', ']', '}', '"', '\'')
    }
    
    /**
     * Insert bracket/quote with auto-completion handling
     * 插入括号/引号并处理自动补全
     */
    private fun insertBracketWithAutoCompletion(bracket: String) {
        try {
            editor?.let { editor ->
                val cursor = editor.cursor
                val line = cursor.leftLine
                val column = cursor.leftColumn
                
                // Determine closing character
                val closingChar = when (bracket) {
                    "(" -> ")"
                    "[" -> "]"
                    "{" -> "}"
                    "\"" -> "\""
                    "'" -> "'"
                    else -> null
                }
                
                if (closingChar != null && !isClosingCharacter(bracket)) {
                    // Insert opening and closing characters
                    editor.text.insert(line, column, bracket + closingChar)
                    // Move cursor between the pair
                    editor.setSelection(line, column + 1)
                    android.util.Log.d("SymbolPanelView", "Auto-completed pair: $bracket$closingChar")
                } else {
                    // For closing characters or when no auto-completion needed, just insert them
                    editor.text.insert(line, column, bracket)
                    android.util.Log.d("SymbolPanelView", "Inserted character: $bracket")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SymbolPanelView", "Failed to insert bracket with auto-completion: ${e.message}")
            fallbackInsertText(bracket)
        }
    }
    
    /**
     * Check if character is a closing character
     * 检查字符是否为闭合字符
     */
    private fun isClosingCharacter(char: String): Boolean {
        return char.length == 1 && char[0] in setOf(')', ']', '}')
    }
    

    
    /**
     * Fallback method for direct text insertion
     * 直接插入文本的后备方法
     */
    private fun fallbackInsertText(text: String) {
        try {
            editor?.let { editor ->
                // Get current cursor position
                val cursor = editor.cursor
                
                // Insert text directly
                val line = cursor.leftLine
                val column = cursor.leftColumn
                editor.text.insert(line, column, text)
            }
        } catch (e: Exception) {
            android.util.Log.e("SymbolPanelView", "Failed to insert text directly: ${e.message}")
        }
    }
    
    /**
     * Move cursor by specified delta values
     * 移动光标
     */
    private fun moveCursor(deltaX: Int, deltaY: Int) {
        try {
            editor?.let { editor ->
                val cursor = editor.cursor
                val text = editor.text
                
                if (deltaY != 0) {
                    val currentLine = cursor.leftLine
                    val currentColumn = cursor.leftColumn
                    val newLine = currentLine + deltaY
                    
                    if (newLine >= 0 && newLine < text.lineCount) {
                        val targetLineLength = text.getColumnCount(newLine)
                        val newColumn = minOf(currentColumn, targetLineLength)
                        editor.setSelection(newLine, newColumn)
                    }
                } else if (deltaX != 0) {
                    val currentLine = cursor.leftLine
                    val currentColumn = cursor.leftColumn
                    val newColumn = currentColumn + deltaX
                    val lineLength = text.getColumnCount(currentLine)
                    
                    when {
                        newColumn < 0 -> {
                            if (currentLine > 0) {
                                val prevLineLength = text.getColumnCount(currentLine - 1)
                                editor.setSelection(currentLine - 1, prevLineLength)
                            }
                        }
                        newColumn > lineLength -> {
                            if (currentLine < text.lineCount - 1) {
                                editor.setSelection(currentLine + 1, 0)
                            }
                        }
                        else -> {
                            editor.setSelection(currentLine, newColumn)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("SymbolPanelView", "Failed to move cursor: ${e.message}")
        }
    }
} 