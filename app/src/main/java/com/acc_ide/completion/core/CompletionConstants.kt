package com.acc_ide.completion.core

/**
 * 补全优先级常量
 * 避免循环依赖，统一管理所有补全相关的常量
 */
object CompletionConstants {
    // 补全优先级常量
    const val PRIORITY_RECENT_LOCAL = 250
    const val PRIORITY_LOCAL_VARIABLE = 200
    const val PRIORITY_LOCAL_FUNCTION = 190
    const val PRIORITY_GLOBAL_VARIABLE = 180
    const val PRIORITY_STRUCT_MEMBER = 170
    const val PRIORITY_STL_COMMON = 150
    const val PRIORITY_STL_FUNCTION = 140
    const val PRIORITY_KEYWORD = 100
}