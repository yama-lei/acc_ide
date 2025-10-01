package com.acc_ide.ui.iopanel

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.acc_ide.R
import com.acc_ide.execution.AnsiTextParser
import com.acc_ide.execution.ExecutionResult
import com.acc_ide.execution.ExecutionStatus
import com.acc_ide.execution.ExecutorFactory
import com.acc_ide.execution.ICodeExecutor
import com.acc_ide.execution.IOCacheManager
import com.acc_ide.ui.main.MainActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

/**
 * IO panel fragment for code execution and testing
 * IO面板Fragment - 用于代码执行和测试
 * 
 * Refactored version with clean architecture:
 * - Supports both cloud (GitHub) and local execution
 * - Modular design with clear separation of concerns
 * - Easy to extend with new execution engines
 */
class IOPanelFragment : Fragment() {
    
    // UI Components
    private lateinit var runCodeButton: MaterialButton
    private lateinit var runStatus: TextView
    private lateinit var btnCloudMode: MaterialButton
    private lateinit var btnLocalMode: MaterialButton
    private lateinit var inputText: TextInputEditText
    private lateinit var actualOutputText: TextInputEditText
    private lateinit var expectedOutputText: TextInputEditText
    
    // Fragment arguments
    private var fileName: String = ""
    private var language: String = ""
    
    // Code executor
    private var executor: ICodeExecutor? = null

    companion object {
        private const val ARG_FILENAME = "filename"
        private const val ARG_LANGUAGE = "language"

        @JvmStatic
        fun newInstance(fileName: String, language: String) =
            IOPanelFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FILENAME, fileName)
                    putString(ARG_LANGUAGE, language)
                }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_io_panel, container, false)
        
        // Get arguments
        arguments?.let {
            fileName = it.getString(ARG_FILENAME, "")
            language = it.getString(ARG_LANGUAGE, "")
        }
        
        // Initialize views
        initializeViews(view)
        
        // Load cached data
        loadFromCache()
        
        // Set up event listeners
        setupListeners()
        
        return view
    }
    
    /**
     * Initialize UI components
     * 初始化UI组件
     */
    private fun initializeViews(view: View) {
        runCodeButton = view.findViewById(R.id.run_code_button)
        runStatus = view.findViewById(R.id.run_status)
        btnCloudMode = view.findViewById(R.id.btn_cloud_mode)
        btnLocalMode = view.findViewById(R.id.btn_local_mode)
        inputText = view.findViewById(R.id.input_text)
        actualOutputText = view.findViewById(R.id.actual_output_text)
        expectedOutputText = view.findViewById(R.id.expected_output_text)
        
        runStatus.text = ""
        
        // Set initial execution mode button style
        updateExecutionModeButtons(ExecutorFactory.getExecutionMode(requireContext()))
    }
    
    /**
     * Set up event listeners
     * 设置事件监听器
     */
    private fun setupListeners() {
        runCodeButton.setOnClickListener {
            executeCode()
        }
        
        // Cloud mode button listener
        btnCloudMode.setOnClickListener {
            switchExecutionMode(ExecutorFactory.MODE_CLOUD)
        }
        
        // Local mode button listener
        btnLocalMode.setOnClickListener {
            switchExecutionMode(ExecutorFactory.MODE_LOCAL)
        }
    }
    
    /**
     * Switch execution mode
     * 切换执行模式
     */
    private fun switchExecutionMode(mode: String) {
        val currentMode = ExecutorFactory.getExecutionMode(requireContext())
        
        // Only switch if different from current mode
        if (mode != currentMode) {
            ExecutorFactory.setExecutionMode(requireContext(), mode)
            
            // Reset executor to use new mode
            executor = null
            
            // Update button styles
            updateExecutionModeButtons(mode)
            
            // Show toast
            val message = when (mode) {
                ExecutorFactory.MODE_LOCAL -> getString(R.string.local_compiler_enabled)
                else -> getString(R.string.cloud_compiler_enabled)
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Update execution mode button styles
     * 更新执行模式按钮样式
     */
    private fun updateExecutionModeButtons(mode: String) {
        // Get theme colors
        val typedValue = android.util.TypedValue()
        val theme = requireContext().theme
        
        // Get blue color for buttons (same as run button)
        val blueColor = ContextCompat.getColorStateList(requireContext(), R.color.blue_500)
        val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)
        
        // Get surface color for inactive button background
        theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true)
        val surfaceColor = ContextCompat.getColorStateList(requireContext(), typedValue.resourceId)
        
        when (mode) {
            ExecutorFactory.MODE_LOCAL -> {
                // Local mode active (filled blue style)
                btnLocalMode.backgroundTintList = blueColor
                btnLocalMode.setTextColor(whiteColor)
                btnLocalMode.iconTint = ContextCompat.getColorStateList(requireContext(), android.R.color.white)
                btnLocalMode.strokeWidth = 0
                
                // Cloud mode inactive (outlined style with surface background)
                btnCloudMode.backgroundTintList = surfaceColor
                btnCloudMode.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_500))
                btnCloudMode.iconTint = blueColor
                btnCloudMode.strokeWidth = 2
                btnCloudMode.strokeColor = blueColor
            }
            else -> {
                // Cloud mode active (filled blue style)
                btnCloudMode.backgroundTintList = blueColor
                btnCloudMode.setTextColor(whiteColor)
                btnCloudMode.iconTint = ContextCompat.getColorStateList(requireContext(), android.R.color.white)
                btnCloudMode.strokeWidth = 0
                
                // Local mode inactive (outlined style with surface background)
                btnLocalMode.backgroundTintList = surfaceColor
                btnLocalMode.setTextColor(ContextCompat.getColor(requireContext(), R.color.blue_500))
                btnLocalMode.iconTint = blueColor
                btnLocalMode.strokeWidth = 2
                btnLocalMode.strokeColor = blueColor
            }
        }
    }
    
    /**
     * Load cached IO data
     * 加载缓存的IO数据
     */
    private fun loadFromCache() {
        val cachedInstance = IOCacheManager.get(fileName) ?: return
        
            inputText.setText(cachedInstance.input)
        
        // Parse ANSI codes in output
        val styledOutput = AnsiTextParser.parseAnsiText(cachedInstance.actualOutput)
            actualOutputText.setText(styledOutput)
        
            expectedOutputText.setText(cachedInstance.expectedOutput)
        
        // Display status if available
            if (cachedInstance.status.isNotEmpty()) {
            displayStatus(cachedInstance.status, cachedInstance.executionTime)
        }
    }
    
    /**
     * Save current data to cache
     * 保存当前数据到缓存
     */
    private fun saveToCache(executionTime: Int = 0) {
        IOCacheManager.update(
            fileName = fileName,
            input = inputText.text.toString(),
            actualOutput = actualOutputText.text.toString(),
            expectedOutput = expectedOutputText.text.toString(),
            status = extractStatusCode(runStatus.text.toString()),
            executionTime = executionTime
        )
    }
    
    /**
     * Extract status code from status text
     * 从状态文本中提取状态码
     */
    private fun extractStatusCode(statusText: String): String {
        return when {
            statusText.startsWith("AC") -> ExecutionStatus.ACCEPTED
            statusText.startsWith("WA") -> ExecutionStatus.WRONG_ANSWER
            statusText.startsWith("CE") -> ExecutionStatus.COMPILE_ERROR
            statusText.startsWith("TLE") -> ExecutionStatus.TIME_LIMIT_EXCEEDED
            statusText.startsWith("MLE") -> ExecutionStatus.MEMORY_LIMIT_EXCEEDED
            statusText.startsWith("RE") -> ExecutionStatus.RUNTIME_ERROR
            statusText.startsWith("RS") -> ExecutionStatus.RUNNING
            else -> statusText
        }
    }
    
    /**
     * Execute code using selected executor
     * 使用选定的执行器执行代码
     */
    private fun executeCode() {
        // Create executor if needed
        if (executor == null) {
            executor = ExecutorFactory.createExecutor(requireContext())
        }
        
        // Check if already running
        if (executor?.isExecuting() == true) {
            Toast.makeText(context, getString(R.string.already_running), Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get code from main activity
        val mainActivity = activity as? MainActivity
        val code = mainActivity?.files?.get(fileName) ?: ""
        
        if (code.isBlank()) {
            Toast.makeText(context, getString(R.string.code_empty), Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get input and expected output
        val input = inputText.text.toString()
        val expected = expectedOutputText.text.toString()
        
        // Update UI to show running state
        runCodeButton.isEnabled = false
        runStatus.text = getString(R.string.code_running)
        runStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        actualOutputText.setText(getString(R.string.code_running_on_github))
        
        // Execute code
        executor?.executeCode(
            code = code,
            language = language,
            input = input,
            expectedOutput = expected,
            onProgress = { progress ->
                actualOutputText.setText(progress)
            },
            onComplete = { result ->
                handleExecutionComplete(result)
            },
            onError = { error ->
                handleExecutionError(error)
            }
        )
    }
    
    /**
     * Handle execution completion
     * 处理执行完成
     */
    private fun handleExecutionComplete(result: ExecutionResult) {
        // Parse ANSI codes in output
        val styledOutput = AnsiTextParser.parseAnsiText(result.actualOutput)
        actualOutputText.setText(styledOutput)
        
        // Display status
        displayStatus(result.status, result.executionTime)
        
        // Save to cache
        saveToCache(result.executionTime)
        
        // Reset button state
        runCodeButton.isEnabled = true
    }
    
    /**
     * Handle execution error
     * 处理执行错误
     */
    private fun handleExecutionError(error: String) {
        Toast.makeText(context, error, Toast.LENGTH_LONG).show()
        runStatus.text = "Error"
        runStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
        actualOutputText.setText(error)
        runCodeButton.isEnabled = true
    }
    
    /**
     * Display execution status
     * 显示执行状态
     */
    private fun displayStatus(status: String, executionTime: Int) {
        if (executionTime > 0 && status != ExecutionStatus.COMPILE_ERROR) {
            // Show status with execution time
                                runStatus.text = getString(R.string.execution_status_with_time, status, executionTime)
                                setStatusColor(status)
            
            // Make execution time gray
                                val spannable = SpannableString(runStatus.text)
                                spannable.setSpan(
                                    ForegroundColorSpan(ContextCompat.getColor(requireContext(), android.R.color.darker_gray)),
                status.length,
                runStatus.text.length,
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                                runStatus.text = spannable
                            } else {
            // Show status only
                                runStatus.text = status
                                setStatusColor(status)
        }
    }
    
    /**
     * Set status text color based on status type
     * 根据状态类型设置状态文本颜色
     */
    private fun setStatusColor(status: String) {
        val color = when (status) {
            ExecutionStatus.COMPILE_ERROR -> android.R.color.holo_red_dark
            ExecutionStatus.ACCEPTED -> android.R.color.holo_green_dark
            ExecutionStatus.WRONG_ANSWER -> android.R.color.holo_red_light
            ExecutionStatus.TIME_LIMIT_EXCEEDED -> android.R.color.holo_orange_dark
            ExecutionStatus.MEMORY_LIMIT_EXCEEDED -> android.R.color.holo_orange_light
            ExecutionStatus.RUNTIME_ERROR -> android.R.color.holo_purple
            ExecutionStatus.RUNNING -> android.R.color.holo_blue_light
            else -> android.R.color.darker_gray
        }
        runStatus.setTextColor(ContextCompat.getColor(requireContext(), color))
    }
    
    override fun onPause() {
        super.onPause()
        // Save to cache when pausing
        saveToCache()
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel any running execution
        executor?.cancelExecution()
    }
}
