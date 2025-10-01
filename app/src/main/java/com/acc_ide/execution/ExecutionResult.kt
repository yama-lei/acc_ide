package com.acc_ide.execution

/**
 * Code execution result data class
 * 代码执行结果数据类
 */
data class ExecutionResult(
    val status: String,              // AC, WA, CE, TLE, MLE, RE, RS
    val actualOutput: String,
    val executionTime: Int = 0,      // Execution time in milliseconds
    val errorMessage: String = ""
)

/**
 * Execution status constants
 * 执行状态常量
 */
object ExecutionStatus {
    const val ACCEPTED = "AC"              // Accepted
    const val WRONG_ANSWER = "WA"          // Wrong Answer
    const val COMPILE_ERROR = "CE"         // Compile Error
    const val TIME_LIMIT_EXCEEDED = "TLE"  // Time Limit Exceeded
    const val MEMORY_LIMIT_EXCEEDED = "MLE"// Memory Limit Exceeded
    const val RUNTIME_ERROR = "RE"         // Runtime Error
    const val RUNNING = "RS"               // Running/In Progress
}

