package com.acc_ide.model

import android.net.Uri

/**
 * 表示一个代码文件的数据类
 *
 * @property name 文件名
 * @property content 文件内容
 * @property language 编程语言类型
 * @property lastModified 最后修改时间
 * @property isExternallySaved 是否已保存到非默认位置（用户选择的位置）
 * @property externalUri 外部保存的文件URI，用于直接更新外部文件
 */
data class CodeFile(
    val name: String,
    val content: String,
    val language: String,
    val lastModified: Long = System.currentTimeMillis(),
    val isExternallySaved: Boolean = false,
    val externalUri: String = "" // 存储为字符串以便于保存到元数据文件
) 