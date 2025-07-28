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
 * File storage manager for handling persistent file storage and reading operations
 * 文件存储管理器 - 用于处理文件的持久化存储和读取
 */
class FileStorageManager(private val context: Context) {
    
    companion object {
        private const val TAG = "FileStorageManager"
        
        // File lock set to prevent concurrent deletion of same file
        private val fileLocks = ConcurrentHashMap<String, Boolean>()
        
        // Coroutine scope for async file operations
        private val fileOperationScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        
        // File deletion queue
        private val deletionQueue = ConcurrentLinkedQueue<Pair<String, (Boolean) -> Unit>>()
        
        // Whether currently processing deletion queue
        private val isProcessingQueue = AtomicBoolean(false)
    }
    
    /**
     * Get directory for storing code files
     * 获取存储代码文件的目录
     */
    fun getCodeFilesDir(): File {
        // Directly use external storage directory, path: /storage/emulated/0/Android/data/com.acc_ide/files
        val dir = context.getExternalFilesDir(null)
            ?: throw IOException("Cannot get external storage directory")
            
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * Refresh file system state from disk - mainly to ensure file system state is up-to-date, especially after file deletion
     * 从磁盘刷新文件状态 - 主要用于确保文件系统状态是最新的，特别是在文件删除后
     */
    fun refreshFileSystem() {
        try {
            Log.d(TAG, "Starting file system state refresh...")
            val filesDir = getCodeFilesDir()
            
            // Force refresh file system cache
            try {
                // Use MediaScannerConnection to refresh file directory
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(filesDir.absolutePath),
                    null,
                    null
                )
                
                // Wait a bit for system to complete scan
                Thread.sleep(100)
                
                // List all files to force cache refresh
                val files = filesDir.listFiles() ?: emptyArray()
                Log.d(TAG, "File system refresh completed, found ${files.size} files")
            
                // Output all file names for debugging
                files.forEach { file ->
                    Log.d(TAG, "File: ${file.name}, size: ${file.length()}, last modified: ${file.lastModified()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to refresh file system cache", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh file system state", e)
        }
    }
    
    /**
     * Save file to internal storage
     * 将文件存储到内部存储
     * @param file Code file to store
     * @return Whether storage was successful
     */
    fun saveFile(file: CodeFile): Boolean {
        try {
            val filesDir = getCodeFilesDir()
            val destFile = File(filesDir, sanitizeFileName(file.name))
            
            // Create directory if not exists
            if (!filesDir.exists()) {
                filesDir.mkdirs()
            }
            
            // Write file content
            destFile.writeText(file.content)
            
            // Write file metadata
            val metaFile = File(filesDir, "${sanitizeFileName(file.name)}.meta")
            metaFile.writeText("${file.language}\n${file.lastModified}\n${file.isExternallySaved}\n${file.externalUri}")
            
            Log.d(TAG, "File saved successfully: ${file.name}, external save status: ${file.isExternallySaved}, URI: ${file.externalUri}")
            return true
        } catch (e: IOException) {
            Log.e(TAG, "File save failed: ${file.name}", e)
            return false
        }
    }
    
    /**
     * Save file to user-selected location
     * 将文件保存到用户选择的位置
     * @param file Code file to save
     * @param uri Target URI
     * @return Whether save was successful
     */
    fun saveFileToUri(file: CodeFile, uri: Uri): Boolean {
        try {
            // Try to get persistent permission for future file updates
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
                Log.d(TAG, "Obtained persistent permission for file: $uri")
            } catch (e: Exception) {
                Log.w(TAG, "Cannot get persistent permission for file: ${e.message}")
                // Continue processing, even without persistent permission we can try to save file
            }
            
            // Try to open output stream with different modes
            var outputStream = try {
                // First try "wt" mode (overwrite text)
                context.contentResolver.openOutputStream(uri, "wt")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to open output stream with 'wt' mode, trying 'w' mode: ${e.message}")
                try {
                    // If failed, try "w" mode (overwrite)
                    context.contentResolver.openOutputStream(uri, "w")
                } catch (e2: Exception) {
                    Log.w(TAG, "Failed to open output stream with 'w' mode, trying default mode: ${e2.message}")
                    // If still failed, try default mode
                    context.contentResolver.openOutputStream(uri)
                }
            }
            
            if (outputStream == null) {
                Log.e(TAG, "Cannot open output stream, possibly permission issue: ${file.name}, URI: $uri")
                return false
            }
            
            outputStream.use { stream ->
                stream.write(file.content.toByteArray())
                stream.flush()
            }
            
            // Convert URI to string for saving
            val uriString = uri.toString()
            
            // Create updated CodeFile object with external URI
            val updatedCodeFile = file.copy(
                isExternallySaved = true,
                externalUri = uriString
            )
            
            saveFile(updatedCodeFile)
            
            Log.d(TAG, "File saved to external URI: ${file.name}, URI: $uriString")
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "Save file to external URI failed, permission denied: ${file.name}", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Save file to external URI failed: ${file.name}", e)
            return false
        }
    }
    
    /**
     * Update external file directly
     * 直接更新外部文件
     * @param file Code file with latest content
     * @return Whether update was successful
     */
    fun updateExternalFile(file: CodeFile): Boolean {
        if (!file.isExternallySaved || file.externalUri.isEmpty()) {
            Log.e(TAG, "Cannot update external file, file not saved externally or URI empty: ${file.name}")
            return false
        }
        
        try {
            val uri = Uri.parse(file.externalUri)
            
            // Try to open output stream with different modes
            var outputStream = try {
                // First try "wt" mode (overwrite text)
                context.contentResolver.openOutputStream(uri, "wt")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to open output stream with 'wt' mode, trying 'w' mode: ${e.message}")
                try {
                    context.contentResolver.openOutputStream(uri, "w")
                } catch (e2: Exception) {
                    Log.w(TAG, "Failed to open output stream with 'w' mode, trying default mode: ${e2.message}")
                    context.contentResolver.openOutputStream(uri)
                }
            }
            
            if (outputStream == null) {
                Log.e(TAG, "Cannot open output stream, possibly permission issue: ${file.name}, URI: ${file.externalUri}")
                return false
            }
            
            // Write file content
            outputStream.use { stream ->
                stream.write(file.content.toByteArray())
                stream.flush()
            }
            
            Log.d(TAG, "Updated external file: ${file.name}, URI: ${file.externalUri}")
            return true
        } catch (e: SecurityException) {
            Log.e(TAG, "Update external file failed, permission denied: ${file.name}", e)
            return false
        } catch (e: Exception) {
            Log.e(TAG, "Update external file failed: ${file.name}", e)
            return false
        }
    }
    
    /**
     * Delete file from storage
     * 删除文件
     * @param fileName Name of file to delete
     * @return Whether deletion was successful
     */
    fun deleteFile(fileName: String): Boolean {
        // If file is being deleted, return false directly
        if (fileLocks.putIfAbsent(fileName, true) != null) {
            Log.w(TAG, "File $fileName is being deleted by another operation, skipping this deletion")
            return false
        }
        
        try {
            Log.d(TAG, "Starting file deletion: $fileName")
            val filesDir = getCodeFilesDir()
            val sanitizedFileName = sanitizeFileName(fileName)
            val fileToDelete = File(filesDir, sanitizedFileName)
            val metaFileToDelete = File(filesDir, "${sanitizedFileName}.meta")
            
            // Force close any possibly open file streams
            try {
                System.gc()
                Thread.sleep(200) // Give GC more time
            } catch (e: Exception) {
                Log.e(TAG, "GC call failed", e)
            }
            
            var success = true
            
            // Force refresh file state
            if (fileToDelete.exists()) {
            fileToDelete.setLastModified(System.currentTimeMillis())
            }
            
            if (metaFileToDelete.exists()) {
            metaFileToDelete.setLastModified(System.currentTimeMillis())
            }
            
            // Ensure files are writable
            if (fileToDelete.exists()) {
                fileToDelete.setWritable(true)
            }
            
            if (metaFileToDelete.exists()) {
                metaFileToDelete.setWritable(true)
            }
            
            // Check if file exists
            if (!fileToDelete.exists()) {
                Log.d(TAG, "File to delete does not exist: $fileName (path: ${fileToDelete.absolutePath})")
                // If file doesn't exist, consider deletion successful
            } else {
                // Delete main file
                for (i in 1..3) {  // Try up to 3 times
                    if (fileToDelete.delete()) {
                        Log.d(TAG, "Successfully deleted main file: $fileName (attempt $i)")
                        break
                    } else {
                        Log.e(TAG, "File deletion failed: $fileName (attempt $i)")
                        if (i == 3) success = false
                        // Brief delay between attempts
                        Thread.sleep(200) // Increase delay time
                    }
                }
            }
            
            // Check if metadata file exists
            if (!metaFileToDelete.exists()) {
                Log.d(TAG, "Metadata file to delete does not exist: ${fileName}.meta (path: ${metaFileToDelete.absolutePath})")
            } else {
                // Delete metadata file
                for (i in 1..3) {  // Try up to 3 times
                    if (metaFileToDelete.delete()) {
                        Log.d(TAG, "Successfully deleted metadata file: ${fileName}.meta (attempt $i)")
                        break
                    } else {
                        Log.e(TAG, "Metadata file deletion failed: ${fileName}.meta (attempt $i)")
                        if (i == 3) success = false
                        // Brief delay between attempts
                        Thread.sleep(200) // Increase delay time
                    }
                }
            }
            
            // Additional file deletion method - force delete
            if (fileToDelete.exists()) {
                try {
                    Log.d(TAG, "Attempting forced file deletion: ${fileToDelete.absolutePath}")
                    // Force delete file
                    val runtime = Runtime.getRuntime()
                    runtime.exec("rm -f ${fileToDelete.absolutePath}")
                    
                    // Wait for file system to complete deletion operation
                    Thread.sleep(300) // Increase wait time
                    
                    if (fileToDelete.exists()) {
                        // If still exists, try using Java IO method for forced deletion
                        Log.d(TAG, "Attempting Java IO method file deletion")
                        if (fileToDelete.setWritable(true) && fileToDelete.delete()) {
                            Log.d(TAG, "Java IO method file deletion successful")
                        } else {
                            Log.e(TAG, "File still exists after forced deletion: ${fileToDelete.absolutePath}")
                            success = false
                        }
                    } else {
                        Log.d(TAG, "Forced file deletion successful: ${fileToDelete.absolutePath}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during forced file deletion: ${e.message}")
                }
            }
            
            // If metadata file still exists, also try forced deletion
            if (metaFileToDelete.exists()) {
                try {
                    Log.d(TAG, "Attempting forced metadata file deletion: ${metaFileToDelete.absolutePath}")
                    // Force delete file
                    val runtime = Runtime.getRuntime()
                    runtime.exec("rm -f ${metaFileToDelete.absolutePath}")
                    
                    // Wait for file system to complete deletion operation
                    Thread.sleep(300) // Increase wait time
                    
                    if (metaFileToDelete.exists()) {
                        // If still exists, try using Java IO method for forced deletion
                        Log.d(TAG, "Attempting Java IO method metadata file deletion")
                        if (metaFileToDelete.setWritable(true) && metaFileToDelete.delete()) {
                            Log.d(TAG, "Java IO method metadata file deletion successful")
                        } else {
                            Log.e(TAG, "Metadata file still exists after forced deletion: ${metaFileToDelete.absolutePath}")
                            success = false
                        }
                    } else {
                        Log.d(TAG, "Forced metadata file deletion successful: ${metaFileToDelete.absolutePath}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error during forced metadata file deletion: ${e.message}")
                }
            }
            
            // Force refresh media library
            try {
                // Notify media scanner to refresh file directory
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
                        Log.e(TAG, "Error updating media store", e)
                        // Continue execution, don't interrupt deletion process due to media scanner update failure
                    }
                } else {
                    // Older Android - use MediaScannerConnection
                    MediaScannerConnection.scanFile(
                        context,
                        arrayOf(filesDir.absolutePath),
                        null,
                        null
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing media library", e)
                // Continue execution, don't interrupt deletion process due to media scanner update failure
                }
                
            // Final verification
            val fileStillExists = fileToDelete.exists()
            val metaFileStillExists = metaFileToDelete.exists()
            
            if (fileStillExists || metaFileStillExists) {
                Log.e(TAG, "Files still exist after deletion: main file=$fileStillExists, metadata file=$metaFileStillExists")
                success = false
            } else {
                Log.d(TAG, "File deletion verification successful: $fileName")
                success = true
            }
            
            // List all files in directory after deletion
            val remainingFiles = filesDir.listFiles()
            Log.d(TAG, "Remaining ${remainingFiles?.size ?: 0} files in directory after deletion")
            remainingFiles?.forEach { file ->
                Log.d(TAG, "  - Remaining file: ${file.name}")
            }
            
            // Force refresh file system state
            refreshFileSystem()
            
            return success
        } catch (e: Exception) {
            Log.e(TAG, "Exception during file deletion process: $fileName", e)
            return false
        } finally {
            // Release file lock regardless of success or failure
            fileLocks.remove(fileName)
            
            // Process next file in queue
            processNextInQueue()
        }
    }
    
    /**
     * Process next file deletion request in queue
     * 处理队列中的下一个文件删除请求
     */
    private fun processNextInQueue() {
        if (isProcessingQueue.get()) {
            return  // Already have a thread processing queue
        }
        
        val nextItem = deletionQueue.poll() ?: return
        
        isProcessingQueue.set(true)
        
        fileOperationScope.launch {
            try {
                val (fileName, callback) = nextItem
                val result = deleteFile(fileName)
                
                // Call callback on main thread
                withContext(Dispatchers.Main) {
                    callback(result)
                }
                
                // Add delay to ensure file system has enough time to complete operation
                delay(300)
            } finally {
                isProcessingQueue.set(false)
                
                // If queue still has items, continue processing
                if (!deletionQueue.isEmpty()) {
                    processNextInQueue()
                }
            }
        }
    }
    
    /**
     * Delete file asynchronously
     * 异步删除文件
     * @param fileName Name of file to delete
     * @param onComplete Callback function after deletion complete, parameter is whether deletion was successful
     */
    fun deleteFileAsync(fileName: String, onComplete: ((Boolean) -> Unit)? = null) {
        // First release any previously existing file lock
        fileLocks.remove(fileName)
        
        // Add deletion request to queue
        deletionQueue.offer(fileName to { success ->
            // After deletion completes, refresh file system state regardless of success
            if (success) {
                Log.d(TAG, "File $fileName deletion successful, refreshing file system state")
                refreshFileSystem()
            } else {
                Log.d(TAG, "File $fileName deletion failed, attempting to refresh file system state")
                refreshFileSystem()
            }
            
            // Call original callback
            onComplete?.invoke(success)
        })
        
        // If queue was previously empty, start processing
        if (deletionQueue.size == 1 && !isProcessingQueue.get()) {
            processNextInQueue()
        } else {
            Log.d(TAG, "File $fileName added to deletion queue, current queue length: ${deletionQueue.size}")
        }
    }
    
    /**
     * Rename file
     * 重命名文件
     * @param oldFileName Old file name
     * @param newFileName New file name
     * @return Whether rename was successful
     */
    fun renameFile(oldFileName: String, newFileName: String): Boolean {
        try {
            val filesDir = getCodeFilesDir()
            val oldFile = File(filesDir, sanitizeFileName(oldFileName))
            val oldMetaFile = File(filesDir, "${sanitizeFileName(oldFileName)}.meta")
            
            if (!oldFile.exists() || !oldMetaFile.exists()) {
                Log.e(TAG, "File to rename does not exist: $oldFileName")
                return false
            }
            
            // Read old file content and metadata
            val content = oldFile.readText()
            val metaLines = oldMetaFile.readLines()
            
            if (metaLines.size < 2) {
                Log.e(TAG, "Metadata format error: $oldFileName")
                return false
            }
            
            // Create new file object
            val codeFile = CodeFile(
                name = newFileName,
                content = content,
                language = metaLines[0],
                lastModified = metaLines[1].toLongOrNull() ?: System.currentTimeMillis()
            )
            
            // Save new file
            if (!saveFile(codeFile)) {
                return false
            }
            
            // Delete old file
            return deleteFile(oldFileName)
        } catch (e: Exception) {
            Log.e(TAG, "File rename failed: $oldFileName -> $newFileName", e)
            return false
        }
    }
    
    /**
     * Read file content
     * 读取文件内容
     * @param fileName File name
     * @return Code file object, null if read failed
     */
    fun readFile(fileName: String): CodeFile? {
        try {
            val filesDir = getCodeFilesDir()
            val file = File(filesDir, sanitizeFileName(fileName))
            val metaFile = File(filesDir, "${sanitizeFileName(fileName)}.meta")
            
            if (!file.exists()) {
                Log.e(TAG, "File does not exist: $fileName")
                return null
            }
            
            if (!metaFile.exists()) {
                Log.e(TAG, "Metadata file does not exist: $fileName")
                // Create default metadata
                val defaultLanguage = when {
                    fileName.endsWith(".cpp") -> "cpp"
                    fileName.endsWith(".py") -> "python"
                    fileName.endsWith(".java") -> "java"
                    else -> "text"
                }
                metaFile.writeText("$defaultLanguage\n${System.currentTimeMillis()}\nfalse\n")
            }
            
            val content = file.readText()
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
            Log.e(TAG, "File read failed: $fileName", e)
            return null
        }
    }
    
    /**
     * Sanitize file name to ensure safety
     * 清理文件名，确保安全
     */
    fun sanitizeFileName(fileName: String): String {
        // Replace unsafe characters
        return fileName.replace("[\\\\/:*?\"<>|]".toRegex(), "_")
    }
    
    /**
     * Get all code files from storage
     * 获取所有代码文件
     * @return List of code files
     */
    fun getAllCodeFiles(): List<CodeFile> {
        val files = mutableListOf<CodeFile>()
        try {
            val filesDir = getCodeFilesDir()
            if (!filesDir.exists()) {
                filesDir.mkdirs()
                return files
            }
            
            val fileList = filesDir.listFiles() ?: return files
            
            // Only process actual code files (not metadata files)
            val codeFiles = fileList.filter { 
                !it.isDirectory && !it.name.endsWith(".meta") && !it.name.startsWith(".") 
            }
            
            // Convert each file to CodeFile object
            for (file in codeFiles) {
                try {
                    // Read metadata
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
                    Log.e(TAG, "Error reading file: ${file.name}", e)
                }
            }
            
            Log.d(TAG, "Loaded ${files.size} code files")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get code file list", e)
        }
        return files
    }
    
    /**
     * Determine programming language based on file name
     * 根据文件名确定编程语言
     * @param fileName File name
     * @return Corresponding programming language
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