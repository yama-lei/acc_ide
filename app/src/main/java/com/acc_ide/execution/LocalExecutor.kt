package com.acc_ide.execution

import android.content.Context

/**
 * Local code executor - placeholder for future implementation
 * 本地代码执行器 - 未来实现的占位符
 * 
 * Possible implementations:
 * - TCC compiler for C/C++
 * - Chaquopy for Python
 * - ECJ for Java
 */
class LocalExecutor(private val context: Context) : ICodeExecutor {
    
    private var isRunning = false
    
    override fun executeCode(
        code: String,
        language: String,
        input: String,
        expectedOutput: String,
        onProgress: ((String) -> Unit)?,
        onComplete: (ExecutionResult) -> Unit,
        onError: (String) -> Unit
    ) {
        // TODO: Implement local execution
        onError("Local execution not yet implemented")
    }
    
    override fun cancelExecution() {
        isRunning = false
    }
    
    override fun isExecuting(): Boolean = isRunning
    
    override fun getExecutorName(): String = "Local Compiler (Not Implemented)"
}

