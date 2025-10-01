package com.acc_ide.execution

import android.content.Context
import android.util.Log
import com.acc_ide.execution.wasm.WasmCppExecutor
import com.acc_ide.execution.wasm.WasmPythonExecutor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Local code executor using WebAssembly
 * 本地代码执行器 - 使用WebAssembly
 * 
 * Implementations:
 * - WASM Clang for C/C++
 * - Pyodide for Python
 * - Java (future)
 */
class LocalExecutor(private val context: Context) : ICodeExecutor {
    
    private val TAG = "LocalExecutor"
    private var isRunning = false
    
    // WASM执行器（懒加载）
    private var wasmCpp: WasmCppExecutor? = null
    private var wasmPython: WasmPythonExecutor? = null
    
    override fun executeCode(
        code: String,
        language: String,
        input: String,
        expectedOutput: String,
        onProgress: ((String) -> Unit)?,
        onComplete: (ExecutionResult) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isRunning) {
            onError("Already executing code")
            return
        }
        
        isRunning = true
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                when (language.lowercase()) {
                    "cpp", "c" -> {
                        executeCppWithWasm(code, input, expectedOutput, onProgress, onComplete, onError)
                    }
                    "python", "py" -> {
                        executePythonWithWasm(code, input, expectedOutput, onProgress, onComplete, onError)
                    }
                    "java" -> {
                        onError("Java local execution not yet implemented. Please use cloud mode.")
                        isRunning = false
                    }
                    else -> {
                        onError("Unsupported language: $language")
                        isRunning = false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Execution error", e)
                onError(e.message ?: "Unknown error occurred")
                isRunning = false
            }
        }
    }
    
    private fun executeCppWithWasm(
        code: String,
        input: String,
        expectedOutput: String,
        onProgress: ((String) -> Unit)?,
        onComplete: (ExecutionResult) -> Unit,
        onError: (String) -> Unit
    ) {
        onProgress?.invoke("Initializing C++ compiler...")
        
        if (wasmCpp == null) {
            wasmCpp = WasmCppExecutor(context)
        }
        
        val executor = wasmCpp!!
        
        if (!executor.isReady()) {
            onProgress?.invoke("Loading WASM C++ compiler (first time may take a while)...")
            
            executor.initialize(
                onReady = {
                    Log.d(TAG, "WASM C++ ready, executing code")
                    onProgress?.invoke("Compiling and running...")
                    runCppCode(executor, code, input, expectedOutput, onComplete, onError)
                },
                onError = { error ->
                    Log.e(TAG, "WASM C++ initialization failed: $error")
                    onError("Failed to initialize C++ compiler: $error")
                    isRunning = false
                }
            )
        } else {
            onProgress?.invoke("Compiling and running...")
            runCppCode(executor, code, input, expectedOutput, onComplete, onError)
        }
    }
    
    private fun runCppCode(
        executor: WasmCppExecutor,
        code: String,
        input: String,
        expectedOutput: String,
        onComplete: (ExecutionResult) -> Unit,
        onError: (String) -> Unit
    ) {
        val startTime = System.currentTimeMillis()
        val outputBuilder = StringBuilder()
        var execTimeMs = 0 // C++ 程序实际执行时间（从 WASM 获取）
        
        executor.execute(
            code = code,
            input = input,
            onOutput = { output ->
                // 解析执行时间标记 [EXEC_TIME_MS:10]
                val timePattern = """\[EXEC_TIME_MS:(\d+)\]""".toRegex()
                val match = timePattern.find(output)
                if (match != null) {
                    execTimeMs = match.groupValues[1].toIntOrNull() ?: 0
                    // 不添加这个标记到输出中
                } else {
                    // 正常输出
                    outputBuilder.append(output)
                }
            },
            onError = { error ->
                val executionTime = (System.currentTimeMillis() - startTime).toInt()
                onComplete(ExecutionResult(
                    status = if (error.contains("compil", ignoreCase = true)) "CE" else "RE",
                    actualOutput = outputBuilder.toString(),
                    executionTime = executionTime,
                    errorMessage = error
                ))
                isRunning = false
            },
            onComplete = { exitCode ->
                // 使用程序实际执行时间（如果有），否则使用总时间
                val executionTime = if (execTimeMs > 0) execTimeMs else (System.currentTimeMillis() - startTime).toInt()
                val actualOutput = outputBuilder.toString()
                
                val status = if (exitCode != 0) {
                    "RE"
                } else if (expectedOutput.isNotEmpty() && actualOutput.trim() != expectedOutput.trim()) {
                    "WA"
                } else {
                    "AC"
                }
                
                onComplete(ExecutionResult(
                    status = status,
                    actualOutput = actualOutput,
                    executionTime = executionTime,
                    errorMessage = if (exitCode != 0) "Exit code: $exitCode" else ""
                ))
                isRunning = false
            }
        )
    }
    
    private fun executePythonWithWasm(
        code: String,
        input: String,
        expectedOutput: String,
        onProgress: ((String) -> Unit)?,
        onComplete: (ExecutionResult) -> Unit,
        onError: (String) -> Unit
    ) {
        onProgress?.invoke("Initializing Python interpreter...")
        
        if (wasmPython == null) {
            wasmPython = WasmPythonExecutor(context)
        }
        
        val executor = wasmPython!!
        
        if (!executor.isReady()) {
            onProgress?.invoke("Loading Pyodide (this may take 10-30 seconds on first run)...")
            
            executor.initialize(
                onReady = {
                    Log.d(TAG, "Pyodide ready, executing code")
                    onProgress?.invoke("Running Python code...")
                    runPythonCode(executor, code, input, expectedOutput, onComplete, onError)
                },
                onError = { error ->
                    Log.e(TAG, "Pyodide initialization failed: $error")
                    onError("Failed to initialize Python: $error\n\nNote: Pyodide requires internet connection on first load.")
                    isRunning = false
                }
            )
        } else {
            onProgress?.invoke("Running Python code...")
            runPythonCode(executor, code, input, expectedOutput, onComplete, onError)
        }
    }
    
    private fun runPythonCode(
        executor: WasmPythonExecutor,
        code: String,
        input: String,
        expectedOutput: String,
        onComplete: (ExecutionResult) -> Unit,
        onError: (String) -> Unit
    ) {
        val startTime = System.currentTimeMillis()
        val outputBuilder = StringBuilder()
        
        executor.execute(
            code = code,
            input = input,
            onOutput = { output ->
                outputBuilder.append(output)
            },
            onError = { error ->
                val executionTime = (System.currentTimeMillis() - startTime).toInt()
                onComplete(ExecutionResult(
                    status = "RE",
                    actualOutput = outputBuilder.toString(),
                    executionTime = executionTime,
                    errorMessage = error
                ))
                isRunning = false
            },
            onComplete = { exitCode ->
                val executionTime = (System.currentTimeMillis() - startTime).toInt()
                val actualOutput = outputBuilder.toString()
                
                val status = when {
                    exitCode != 0 -> "RE"
                    expectedOutput.isNotEmpty() && actualOutput.trim() != expectedOutput.trim() -> "WA"
                    else -> "AC"
                }
                
                onComplete(ExecutionResult(
                    status = status,
                    actualOutput = actualOutput,
                    executionTime = executionTime,
                    errorMessage = ""
                ))
                isRunning = false
            }
        )
    }
    
    override fun cancelExecution() {
        isRunning = false
        // WASM执行器目前不支持中断，但可以标记为取消
    }
    
    override fun isExecuting(): Boolean = isRunning
    
    override fun getExecutorName(): String = "Local WASM Compiler"
    
    /**
     * 清理资源
     */
    fun cleanup() {
        wasmCpp?.cleanup()
        wasmPython?.cleanup()
        wasmCpp = null
        wasmPython = null
    }
}

