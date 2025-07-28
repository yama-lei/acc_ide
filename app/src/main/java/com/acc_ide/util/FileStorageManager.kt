package com.acc_ide.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.acc_ide.data.model.CodeFile
import java.io.File
import java.io.IOException
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import android.content.Intent
import android.media.MediaScannerConnection

/**
 * 文件存储管理器，用于处理文件的持久化存储和读取
 */
class FileStorageManager(private val context: Context) {
    
    companion object {
        private const val TAG = "FileStorageManager"
        
        // 文件锁集合，防止同一个文件被并发删除
        private val fileLocks = ConcurrentHashMap<String, Boolean>()
        
        // 协程作用域，用于异步执行文件操作
        private val fileOperationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        
        // 文件删除队列
        private val deletionQueue = ConcurrentLinkedQueue<Pair<String, (Boolean) -> Unit>>()
        
        // 是否正在处理删除队列
        private val isProcessingQueue = AtomicBoolean(false)
    }
    
    /**
     * 获取存储代码文件的目录
     */
    fun getCodeFilesDir(): File {
        // 直接使用外部存储目录，路径为: /storage/emulated/0/Android/data/com.acc_ide/files
        val dir = context.getExternalFilesDir(null)
            ?: throw IOException("无法获取外部存储目录")
            
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * 从磁盘刷新文件状态
     * 主要用于确保文件系统状态是最新的，特别是在文件删除后
     */
    fun refreshFileSystem() {
        try {
            Log.d(TAG, "开始刷新文件系统状态...")
            val filesDir = getCodeFilesDir()
            
            // 强制刷新文件系统缓存
            try {
                // 使用MediaScannerConnection刷新文件目录
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(filesDir.absolutePath),
                    null,
                    null
                )
                
                // 等待一小段时间，让系统完成扫描
                Thread.sleep(100)
                
                // 列出所有文件，强制刷新缓存
                val files = filesDir.listFiles() ?: emptyArray()
                Log.d(TAG, "文件系统刷新完成，发现 ${files.size} 个文件")
            
                // 输出所有文件名，用于调试
                files.forEach { file ->
                    Log.d(TAG, "文件: ${file.name}, 大小: ${file.length()}, 最后修改时间: ${file.lastModified()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "刷新文件系统缓存失败", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "刷新文件系统状态失败", e)
        }
    }
    
    /**
     * 将文件存储到内部存储
     * 
     * @param file 要存储的代码文件
     * @return 是否存储成功
     */
    fun saveFile(file: CodeFile): Boolean {
        try {
            val filesDir = getCodeFilesDir()
            val destFile = File(filesDir, sanitizeFileName(file.name))
            
            // 创建目录（如果不存在）
            if (!filesDir.exists()) {
                filesDir.mkdirs()
            }
            
            // 写入文件内容
            destFile.writeText(file.content)
            
            // 写入文件元数据
            val metaFile = File(filesDir, "${sanitizeFileName(file.name)}.meta")
            metaFile.writeText("${file.language}\n${file.lastModified}\n${file.isExternallySaved}\n${file.externalUri}")
            
            Log.d(TAG, "文件保存成功: ${file.name}, 外部保存状态: ${file.isExternallySaved}, URI: ${file.externalUri}")
            return true
        } catch (e: IOException) {
            Log.e(TAG, "文件保存失败: ${file.name}", e)
            return false
        }
    }
    
    /**
     * 将文件保存到用户选择的位置
     * 
     * @param file 要保存的代码文件
     * @param uri 目标URI
     * @return 是否保存成功
     */
    fun saveFileToUri(file: CodeFile, uri: Uri): Boolean {
        try {
            // 尝试获取持久性权限，以便后续更新文件
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                Log.d(TAG, "已获取文件的持久性权限: $uri")
            } catch (e: Exception) {
                Log.w(TAG, "无法获取文件的持久性权限: ${e.message}")
                // 继续处理，即使没有持久性权限也可以尝试保存文件
            }
            
            // 尝试使用不同的模式打开输出流
            var outputStream = try {
                // 首先尝试使用"wt"模式（覆盖写入文本）
                context.contentResolver.openOutputStream(uri, "wt")
            } catch (e: Exception) {
                Log.w(TAG, "使用'wt'模式打开输出流失败，尝试使用'w'模式: ${e.message}")
                try {
                    // 如果失败，尝试使用"w"模式（覆盖写入）
                    context.contentResolver.openOutputStream(uri, "w")
                } catch (e2: Exception) {
                    Log.w(TAG, "使用'w'模式打开输出流失败，尝试使用默认模式: ${e2.message}")
                    // 如果仍然失败，尝试使用默认模式
                    context.contentResolver.openOutputStream(uri)
                }
            }
            
            if (outputStream == null) {
                Log.e(TAG, "无法打开输出流，可能是权限问题: ${file.name}, URI: $uri")
                return false
            }
            
            // 写入文件内容
            outputStream.use { stream ->
                stream.write(file.content.toByteArray())
                stream.flush()
            }
            
            // 将URI转换为字符串保存
            val uriString = uri.toString()
            
            // 创建更新后的CodeFile对象，包含外部URI
            val updatedCodeFile = file.copy(
                isExternallySaved = true,
                externalUri = uriString
            )
            
            // 保存更新后的元数据
            saveFile(updatedCodeFile)
            
            Log.d(TAG, "文件已保存到外部URI: ${file.name}, URI: $uriString")
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "保存文件到外部URI失败，权限被拒绝: ${file.name}", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "保存文件到外部URI失败: ${file.name}", e)
            return false
        }
    }
    
    /**
     * 直接更新外部文件
     * 
     * @param file 包含最新内容的代码文件
     * @return 是否更新成功
     */
    fun updateExternalFile(file: CodeFile): Boolean {
        if (!file.isExternallySaved || file.externalUri.isEmpty()) {
            Log.e(TAG, "无法更新外部文件，文件未保存到外部或URI为空: ${file.name}")
            return false
        }
        
        try {
            val uri = Uri.parse(file.externalUri)
            
            // 尝试使用不同的模式打开输出流
            var outputStream = try {
                // 首先尝试使用"wt"模式（覆盖写入文本）
                context.contentResolver.openOutputStream(uri, "wt")
            } catch (e: Exception) {
                Log.w(TAG, "使用'wt'模式打开输出流失败，尝试使用'w'模式: ${e.message}")
                try {
                    // 如果失败，尝试使用"w"模式（覆盖写入）
                    context.contentResolver.openOutputStream(uri, "w")
                } catch (e2: Exception) {
                    Log.w(TAG, "使用'w'模式打开输出流失败，尝试使用默认模式: ${e2.message}")
                    // 如果仍然失败，尝试使用默认模式
                    context.contentResolver.openOutputStream(uri)
                }
            }
            
            if (outputStream == null) {
                Log.e(TAG, "无法打开输出流，可能是权限问题: ${file.name}, URI: ${file.externalUri}")
                return false
            }
            
            // 写入文件内容
            outputStream.use { stream ->
                stream.write(file.content.toByteArray())
                stream.flush()
            }
            
            Log.d(TAG, "已更新外部文件: ${file.name}, URI: ${file.externalUri}")
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "更新外部文件失败，权限被拒绝: ${file.name}", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "更新外部文件失败: ${file.name}", e)
            return false
        }
    }
    
    /**
     * 删除文件
     * 
     * @param fileName 要删除的文件名
     * @return 是否删除成功
     */
    fun deleteFile(fileName: String): Boolean {
        // 如果文件正在被删除，直接返回false
        if (fileLocks.putIfAbsent(fileName, true) != null) {
            Log.w(TAG, "文件 $fileName 正在被其他操作删除，跳过本次删除")
            return false
        }
        
        try {
            Log.d(TAG, "开始删除文件: $fileName")
            val filesDir = getCodeFilesDir()
            val sanitizedFileName = sanitizeFileName(fileName)
            val fileToDelete = File(filesDir, sanitizedFileName)
            val metaFileToDelete = File(filesDir, "${sanitizedFileName}.meta")
            
            // 强制关闭任何可能打开的文件流
            try {
                System.gc()
                Thread.sleep(200) // 给GC更多时间
            } catch (e: Exception) {
                Log.e(TAG, "GC调用失败", e)
            }
            
            var success = true
            
            // 强制刷新文件状态
            if (fileToDelete.exists()) {
            fileToDelete.setLastModified(System.currentTimeMillis())
            }
            
            if (metaFileToDelete.exists()) {
            metaFileToDelete.setLastModified(System.currentTimeMillis())
            }
            
            // 确保文件可写
            if (fileToDelete.exists()) {
                fileToDelete.setWritable(true)
            }
            
            if (metaFileToDelete.exists()) {
                metaFileToDelete.setWritable(true)
            }
            
            // 检查文件是否存在
            if (!fileToDelete.exists()) {
                Log.d(TAG, "要删除的文件不存在: $fileName (路径: ${fileToDelete.absolutePath})")
                // 如果文件不存在，也算删除成功
            } else {
                // 删除主文件
                for (i in 1..3) {  // 尝试最多3次
                    if (fileToDelete.delete()) {
                        Log.d(TAG, "成功删除主文件: $fileName (第" + i + "次尝试)")
                        break
                    } else {
                        Log.e(TAG, "删除文件失败: $fileName (第" + i + "次尝试)")
                        if (i == 3) success = false
                        // 在尝试之间短暂延迟
                        Thread.sleep(200) // 增加延迟时间
                    }
                }
            }
            
            // 检查元数据文件是否存在
            if (!metaFileToDelete.exists()) {
                Log.d(TAG, "要删除的元数据文件不存在: ${fileName}.meta (路径: ${metaFileToDelete.absolutePath})")
            } else {
                // 删除元数据文件
                for (i in 1..3) {  // 尝试最多3次
                    if (metaFileToDelete.delete()) {
                        Log.d(TAG, "成功删除元数据文件: ${fileName}.meta (第" + i + "次尝试)")
                        break
                    } else {
                        Log.e(TAG, "删除元数据文件失败: ${fileName}.meta (第" + i + "次尝试)")
                        if (i == 3) success = false
                        // 在尝试之间短暂延迟
                        Thread.sleep(200) // 增加延迟时间
                    }
                }
            }
            
            // 额外的文件删除方法 - 强制删除
            if (fileToDelete.exists()) {
                try {
                    Log.d(TAG, "尝试强制删除文件: ${fileToDelete.absolutePath}")
                    // 强制删除文件
                    val runtime = Runtime.getRuntime()
                    runtime.exec("rm -f ${fileToDelete.absolutePath}")
                    
                    // 等待文件系统完成删除操作
                    Thread.sleep(300) // 增加等待时间
                    
                    if (fileToDelete.exists()) {
                        // 如果仍然存在，尝试使用Java IO的方式强制删除
                        Log.d(TAG, "尝试使用Java IO方式删除文件")
                        if (fileToDelete.setWritable(true) && fileToDelete.delete()) {
                            Log.d(TAG, "Java IO方式删除文件成功")
                        } else {
                            Log.e(TAG, "强制删除文件后文件仍然存在: ${fileToDelete.absolutePath}")
                            success = false
                        }
                    } else {
                        Log.d(TAG, "强制删除文件成功: ${fileToDelete.absolutePath}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "强制删除文件出错: ${e.message}")
                }
            }
            
            // 如果元数据文件仍然存在，也尝试强制删除
            if (metaFileToDelete.exists()) {
                try {
                    Log.d(TAG, "尝试强制删除元数据文件: ${metaFileToDelete.absolutePath}")
                    // 强制删除文件
                    val runtime = Runtime.getRuntime()
                    runtime.exec("rm -f ${metaFileToDelete.absolutePath}")
                    
                    // 等待文件系统完成删除操作
                    Thread.sleep(300) // 增加等待时间
                    
                    if (metaFileToDelete.exists()) {
                        // 如果仍然存在，尝试使用Java IO的方式强制删除
                        Log.d(TAG, "尝试使用Java IO方式删除元数据文件")
                        if (metaFileToDelete.setWritable(true) && metaFileToDelete.delete()) {
                            Log.d(TAG, "Java IO方式删除元数据文件成功")
                        } else {
                            Log.e(TAG, "强制删除元数据文件后文件仍然存在: ${metaFileToDelete.absolutePath}")
                            success = false
                        }
                    } else {
                        Log.d(TAG, "强制删除元数据文件成功: ${metaFileToDelete.absolutePath}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "强制删除元数据文件出错: ${e.message}")
                }
            }
            
            // 强制刷新媒体库
            try {
                // 通知媒体扫描器刷新文件目录
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    // Android 10+
                    try {
                        if (fileToDelete.exists()) {
                            val contentValues = android.content.ContentValues().apply {
                                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                            }
                            context.contentResolver.update(android.net.Uri.fromFile(fileToDelete), contentValues, null, null)
                }
                
                if (metaFileToDelete.exists()) {
                            val contentValues = android.content.ContentValues().apply {
                                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                            }
                            context.contentResolver.update(android.net.Uri.fromFile(metaFileToDelete), contentValues, null, null)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "更新媒体存储时出错", e)
                        // 继续执行，不要因为媒体扫描器更新失败而中断删除过程
                    }
                } else {
                    // 旧版Android - 使用MediaScannerConnection
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(filesDir.absolutePath),
                        null,
                        null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "刷新媒体库时出错", e)
                // 继续执行，不要因为媒体扫描器更新失败而中断删除过程
                }
                
            // 最终验证
            val fileStillExists = fileToDelete.exists()
            val metaFileStillExists = metaFileToDelete.exists()
            
            if (fileStillExists || metaFileStillExists) {
                Log.e(TAG, "删除后文件仍然存在: 主文件=${fileStillExists}, 元数据文件=${metaFileStillExists}")
                success = false
            } else {
                Log.d(TAG, "文件删除验证成功: $fileName")
                success = true
            }
            
            // 列出删除后目录中的所有文件
            val remainingFiles = filesDir.listFiles()
            Log.d(TAG, "删除后目录中剩余 ${remainingFiles?.size ?: 0} 个文件")
            remainingFiles?.forEach { file ->
                Log.d(TAG, "  - 剩余文件: ${file.name}")
            }
            
            // 强制刷新文件系统状态
            refreshFileSystem()
            
            return success
        } catch (e: Exception) {
            Log.e(TAG, "删除文件过程中发生异常: $fileName", e)
            return false
        } finally {
            // 无论成功失败，都释放文件锁
            fileLocks.remove(fileName)
            
            // 处理队列中的下一个文件
            processNextInQueue()
        }
    }
    
    /**
     * 处理队列中的下一个文件删除请求
     */
    private fun processNextInQueue() {
        if (isProcessingQueue.get()) {
            return  // 已经有一个线程在处理队列
        }
        
        val nextItem = deletionQueue.poll() ?: return
        
        isProcessingQueue.set(true)
        
        fileOperationScope.launch {
            try {
                val (fileName, callback) = nextItem
                val result = deleteFile(fileName)
                
                // 在主线程中调用回调
                withContext(Dispatchers.Main) {
                    callback(result)
                }
                
                // 添加延迟，确保文件系统有足够时间完成操作
                delay(300)
            } finally {
                isProcessingQueue.set(false)
                
                // 如果队列中还有项目，继续处理
                if (!deletionQueue.isEmpty()) {
                    processNextInQueue()
                }
            }
        }
    }
    
    /**
     * 异步删除文件
     * 
     * @param fileName 要删除的文件名
     * @param onComplete 删除完成后的回调函数，参数为是否删除成功
     */
    fun deleteFileAsync(fileName: String, onComplete: ((Boolean) -> Unit)? = null) {
        // 先释放之前可能存在的文件锁
        fileLocks.remove(fileName)
        
        // 将删除请求添加到队列
        deletionQueue.offer(fileName to { success ->
            // 在删除完成后，无论成功与否，都刷新文件系统状态
            if (success) {
                Log.d(TAG, "文件 $fileName 删除成功，正在刷新文件系统状态")
                refreshFileSystem()
            } else {
                Log.d(TAG, "文件 $fileName 删除失败，尝试刷新文件系统状态")
                refreshFileSystem()
            }
            
            // 调用原始回调
            onComplete?.invoke(success)
        })
        
        // 如果队列之前是空的，开始处理
        if (deletionQueue.size == 1 && !isProcessingQueue.get()) {
            processNextInQueue()
        } else {
            Log.d(TAG, "文件 $fileName 已添加到删除队列，当前队列长度: ${deletionQueue.size}")
        }
    }
    
    /**
     * 重命名文件
     * 
     * @param oldFileName 旧文件名
     * @param newFileName 新文件名
     * @return 是否重命名成功
     */
    fun renameFile(oldFileName: String, newFileName: String): Boolean {
        try {
            val filesDir = getCodeFilesDir()
            val oldFile = File(filesDir, sanitizeFileName(oldFileName))
            val oldMetaFile = File(filesDir, "${sanitizeFileName(oldFileName)}.meta")
            
            if (!oldFile.exists() || !oldMetaFile.exists()) {
                Log.e(TAG, "要重命名的文件不存在: $oldFileName")
                return false
            }
            
            // 读取旧文件内容和元数据
            val content = oldFile.readText()
            val metaLines = oldMetaFile.readLines()
            
            if (metaLines.size < 2) {
                Log.e(TAG, "元数据格式错误: $oldFileName")
                return false
            }
            
            // 创建新文件对象
            val codeFile = CodeFile(
                name = newFileName,
                content = content,
                language = metaLines[0],
                lastModified = metaLines[1].toLongOrNull() ?: System.currentTimeMillis()
            )
            
            // 保存新文件
            if (!saveFile(codeFile)) {
                return false
            }
            
            // 删除旧文件
            return deleteFile(oldFileName)
        } catch (e: Exception) {
            Log.e(TAG, "文件重命名失败: $oldFileName -> $newFileName", e)
            return false
        }
    }
    
    /**
     * 读取文件内容
     * 
     * @param fileName 文件名
     * @return 代码文件对象，如果读取失败返回null
     */
    fun readFile(fileName: String): CodeFile? {
        try {
            val filesDir = getCodeFilesDir()
            val file = File(filesDir, sanitizeFileName(fileName))
            val metaFile = File(filesDir, "${sanitizeFileName(fileName)}.meta")
            
            if (!file.exists()) {
                Log.e(TAG, "文件不存在: $fileName")
                return null
            }
            
            if (!metaFile.exists()) {
                Log.e(TAG, "元数据文件不存在: $fileName")
                // 创建默认元数据
                val defaultLanguage = when {
                    fileName.endsWith(".cpp") -> "cpp"
                    fileName.endsWith(".py") -> "python"
                    fileName.endsWith(".java") -> "java"
                    else -> "text"
                }
                metaFile.writeText("$defaultLanguage\n${System.currentTimeMillis()}\nfalse\n")
            }
            
            // 读取文件内容
            val content = file.readText()
            
            // 读取元数据
            val metaLines = metaFile.readLines()
            val language = if (metaLines.isNotEmpty()) metaLines[0] else "text"
            val lastModified = if (metaLines.size > 1) metaLines[1].toLongOrNull() ?: System.currentTimeMillis() else System.currentTimeMillis()
            val isExternallySaved = if (metaLines.size > 2) metaLines[2].toBoolean() else false
            val externalUri = if (metaLines.size > 3) metaLines[3] else ""
            
            return CodeFile(
                name = fileName,
                content = content,
                language = language,
                lastModified = lastModified,
                isExternallySaved = isExternallySaved,
                externalUri = externalUri
            )
        } catch (e: Exception) {
            Log.e(TAG, "文件读取失败: $fileName", e)
            return null
        }
    }
    

    
    /**
     * 清理文件名，确保安全
     */
    fun sanitizeFileName(fileName: String): String {
        // 替换不安全的字符
        return fileName.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
    }
    
    /**
     * 获取所有代码文件
     * 
     * @return 代码文件列表
     */
    fun getAllCodeFiles(): List<CodeFile> {
        val files = mutableListOf<CodeFile>()
        try {
            val filesDir = getCodeFilesDir()
            if (!filesDir.exists()) {
                filesDir.mkdirs()
                return files
            }
            
            // 获取所有文件
            val fileList = filesDir.listFiles() ?: return files
            
            // 只处理实际代码文件（非元数据文件）
            val codeFiles = fileList.filter { 
                !it.isDirectory && !it.name.endsWith(".meta") && !it.name.startsWith(".") 
            }
            
            // 将每个文件转换为CodeFile对象
            for (file in codeFiles) {
                try {
                    // 读取元数据
                    val metaFile = File(filesDir, "${file.name}.meta")
                    var isExternallySaved = false
                    var externalUri = ""
                    
                    if (metaFile.exists()) {
                        val metaLines = metaFile.readLines()
                        isExternallySaved = if (metaLines.size > 2) metaLines[2].toBoolean() else false
                        externalUri = if (metaLines.size > 3) metaLines[3] else ""
                    }
                    
                    val content = file.readText()
                    val language = getFileLanguage(file.name)
                    val lastModified = file.lastModified()
                    
                    files.add(CodeFile(file.name, content, language, lastModified, isExternallySaved, externalUri))
                } catch (e: Exception) {
                    Log.e(TAG, "读取文件时出错: ${file.name}", e)
                }
            }
            
            Log.d(TAG, "已加载 ${files.size} 个代码文件")
        } catch (e: Exception) {
            Log.e(TAG, "获取代码文件列表失败", e)
        }
        return files
    }
    
    /**
     * 根据文件名确定编程语言
     * 
     * @param fileName 文件名
     * @return 对应的编程语言
     */
    private fun getFileLanguage(fileName: String): String {
        return when {
            fileName.endsWith(".java") -> "java"
            fileName.endsWith(".kt") -> "kotlin"
            fileName.endsWith(".py") -> "python"
            fileName.endsWith(".cpp") || fileName.endsWith(".c") -> "cpp"
            fileName.endsWith(".js") -> "javascript"
            fileName.endsWith(".html") -> "html"
            fileName.endsWith(".css") -> "css"
            fileName.endsWith(".xml") -> "xml"
            fileName.endsWith(".json") -> "json"
            fileName.endsWith(".md") -> "markdown"
            fileName.endsWith(".txt") -> "text"
            else -> "text"
        }
    }
} 