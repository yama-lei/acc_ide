package com.acc_ide.execution.wasm

/**
 * WASM Executor Interface
 * WebAssembly执行器接口
 */
interface WasmExecutorInterface {
    
    /**
     * Initialize WASM runtime
     * 初始化WASM运行时
     */
    fun initialize(onReady: () -> Unit, onError: (String) -> Unit)
    
    /**
     * Execute code using WASM compiler/interpreter
     * 使用WASM编译器/解释器执行代码
     */
    fun execute(
        code: String,
        input: String,
        onOutput: (String) -> Unit,
        onError: (String) -> Unit,
        onComplete: (Int) -> Unit // Exit code
    )
    
    /**
     * Check if WASM runtime is ready
     * 检查WASM运行时是否就绪
     */
    fun isReady(): Boolean
    
    /**
     * Get executor name
     * 获取执行器名称
     */
    fun getExecutorName(): String
    
    /**
     * Clean up resources
     * 清理资源
     */
    fun cleanup()
}

