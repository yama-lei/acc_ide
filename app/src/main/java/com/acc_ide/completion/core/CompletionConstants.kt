package com.acc_ide.completion.core

/**
 * 补全优先级常量
 * 避免循环依赖，统一管理所有补全相关的常量
 */
object CompletionConstants {
    // 补全优先级常量（数字越大优先级越高）
    const val PRIORITY_LOCAL_VARIABLE = 200
    const val PRIORITY_LOCAL_FUNCTION = 190
    const val PRIORITY_PARAMETER = 185
    const val PRIORITY_GLOBAL_VARIABLE = 180
    const val PRIORITY_STRUCT_MEMBER = 170
    
    // STL和竞赛编程相关
    const val PRIORITY_STL_CONTAINER = 160      // STL容器类型
    const val PRIORITY_STL_ALGORITHM = 150      // STL算法函数
    const val PRIORITY_STL_COMMON = 140         // 常用STL函数
    const val PRIORITY_STL_FUNCTION = 130       // 其他STL函数
    
    // 一般编程元素
    const val PRIORITY_FUNCTION = 120           // 一般函数
    const val PRIORITY_KEYWORD = 110            // C++关键字
    const val PRIORITY_CONSTANT = 100           // 常量和宏
    const val PRIORITY_TYPE = 90                // 类型名
}