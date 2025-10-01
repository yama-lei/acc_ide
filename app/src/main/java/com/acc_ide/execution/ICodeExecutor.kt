package com.acc_ide.execution

/**
 * Code executor interface
 * 代码执行器接口 - 定义云端和本地执行的统一接口
 */
interface ICodeExecutor {
    
    /**
     * Execute code with given input
     * 执行代码
     * 
     * @param code Source code to execute
     * @param language Programming language (cpp, java, py)
     * @param input Standard input
     * @param expectedOutput Expected output (for comparison)
     * @param onProgress Progress callback (optional)
     * @param onComplete Completion callback
     * @param onError Error callback
     */
    fun executeCode(
        code: String,
        language: String,
        input: String,
        expectedOutput: String,
        onProgress: ((String) -> Unit)? = null,
        onComplete: (ExecutionResult) -> Unit,
        onError: (String) -> Unit
    )
    
    /**
     * Cancel ongoing execution
     * 取消正在执行的任务
     */
    fun cancelExecution()
    
    /**
     * Check if executor is currently running
     * 检查是否正在执行
     */
    fun isExecuting(): Boolean
    
    /**
     * Get executor name/type
     * 获取执行器名称
     */
    fun getExecutorName(): String
}

