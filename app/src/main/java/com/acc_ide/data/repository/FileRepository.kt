package com.acc_ide.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import android.widget.Toast
import com.acc_ide.R
import com.acc_ide.data.model.CodeFile
import com.acc_ide.util.FileStorageManager
import com.acc_ide.util.TemplateManager

/**
 * 文件仓库类 - 负责所有文件操作，提供干净的API给ViewModel
 * 遵循仓库模式，集中处理所有文件相关业务逻辑
 * File warehouse class - responsible for all file operations, providing a clean API to ViewModel
 * Follow the warehouse model and centrally handle all file-related business logic
 */
class FileRepository(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("acc_ide_prefs", Context.MODE_PRIVATE)
    private lateinit var fileStorageManager: FileStorageManager
    private lateinit var templateManager: TemplateManager
    private val openedFiles = mutableSetOf<String>()
    
    //Map used to store code files in memory
    val files = mutableMapOf<String, String>()
    var currentFileName: String = ""
    
    init {
        loadOpenedFilesList()
    }
 
    /**
     * 初始化管理器
     * Initialize manager
     */
    fun initialize() {
        fileStorageManager = FileStorageManager(context)
        templateManager = TemplateManager(context)
    }
    
    /**
     * 加载已打开的文件列表
     * Load the list of opened files
     */
    private fun loadOpenedFilesList() {
        val openedFileSet = prefs.getStringSet("opened_files", null)
        
        // Clear the current list and load the saved list
        openedFiles.clear()
        if (openedFileSet != null) {
            openedFiles.addAll(openedFileSet)
            Log.d("FileRepository", "The last opened file list is loaded: ${openedFiles.joinToString(", ")}")
        }
    }
    
    /**
     * 保存已打开的文件列表
     * Save the list of opened files
     */
    fun saveOpenedFilesList() {
        prefs.edit()
            .putStringSet("opened_files", HashSet(openedFiles))
            .apply()
        Log.d("FileRepository", "The list of currently opened files has been saved: ${openedFiles.joinToString(", ")}")
    }
    
    /**
     * 获取已打开的文件列表
     * Get the list of opened files
     */
    fun getOpenedFiles(): Set<String> = openedFiles.toSet()
    
    /**
     * 加载文件列表，从存储中读取所有可用的代码文件
     * Load the file list, read all available code files from storage
     */
    fun loadFileList() {
        try {
            files.clear()
            val codeFiles = fileStorageManager.getAllCodeFiles()

            // Add the file to memory
            for (codeFile in codeFiles) {
                files[codeFile.name] = codeFile.content
            }

            Log.d("FileRepository", "${files.size} files loaded")
        } catch (e: Exception) {
            Log.e("FileRepository", "Failed to load file list: ${e.message}")
        }
    }
    
    /**
     * 创建新文件
     * Create a new file
     */
    fun createNewFile(language: String): String? {
        try {
            val fileExtension = when (language) {
                "cpp" -> ".cpp"
                "python" -> ".py"
                "java" -> ".java"
                "kotlin" -> ".kt"
                "javascript" -> ".js"
                "html" -> ".html"
                "css" -> ".css"
                "xml" -> ".xml"
                "json" -> ".json"
                "markdown" -> ".md"
                else -> ".txt"
            }
            val initialContent = templateManager.getTemplateContent(language)

            // Force refresh the file system to ensure the latest status
            fileStorageManager.refreshFileSystem()

            // Synchronize the file list in memory with the file system
            val filesDir = fileStorageManager.getCodeFilesDir()
            val actualFiles = filesDir.listFiles()?.filter {
                !it.name.endsWith(".meta") && it.name.endsWith(fileExtension)
            }?.map { it.name }?.toSet() ?: emptySet()

            // Remove files from memory that no longer exist in the file system
            val filesToRemove = files.keys.filter {
                it.endsWith(fileExtension) && !actualFiles.contains(it)
            }

            if (filesToRemove.isNotEmpty()) {
                Log.d("FileRepository", "Remove files from memory that do not exist in the file system: ${filesToRemove.joinToString()}")
                filesToRemove.forEach { files.remove(it) }
            }

            val existingIndexes = mutableListOf<Int>()

            files.keys.forEach { fileName ->
                if (fileName.endsWith(fileExtension)) {
                    val numberPattern = "^(\\d+)\\..+$".toRegex()
                    val matchResult = numberPattern.find(fileName)
                    if (matchResult != null) {
                        val indexStr = matchResult.groupValues[1]
                        indexStr.toIntOrNull()?.let {
                            existingIndexes.add(it)
                        }
                    }
                }
            }

            // Check the file system for files that may exist but not loaded into memory
            val filesList = filesDir.listFiles()
            filesList?.forEach { file ->
                val fileName = file.name
                if (fileName.endsWith(fileExtension) && !fileName.endsWith(".meta")) {
                    val numberPattern = "^(\\d+)\\..+$".toRegex()
                    val matchResult = numberPattern.find(fileName)
                    if (matchResult != null) {
                        val indexStr = matchResult.groupValues[1]
                        indexStr.toIntOrNull()?.let {
                            if (!existingIndexes.contains(it)) {
                                existingIndexes.add(it)
                            }
                        }
                    }
                }
            }

            // Find the smallest available sequence number
            val sortedIndexes = existingIndexes.sorted()
            var i = 1
            while (sortedIndexes.contains(i)) {
                i++
            }
            val newFileIndex = i

            val newFileName = "$newFileIndex$fileExtension"
            Log.d("FileRepository", "Create a new file: $newFileName")

            // Create a code file object - the new file is a temporary file by default (not saved externally)
            val codeFile = CodeFile(
                name = newFileName,
                content = initialContent,
                language = language,
                isExternallySaved = false
            )

            fileStorageManager.saveFile(codeFile)
            files[newFileName] = initialContent
            currentFileName = newFileName
            openedFiles.add(newFileName)
            saveOpenedFilesList()

            return newFileName
        } catch (e: Exception) {
            Log.e("FileRepository", "Failed to create file: ${e.message}", e)
            return null
        }
    }
    
    /**
     * 打开文件
     * Opne a file and add it to the openedFiles list.
     * @param fileName 文件名
     * @return 是否成功打开文件
     */
    fun openFile(fileName: String): Boolean {
        return try {
            // make sure the file is in the openedFiles list
            if (!openedFiles.contains(fileName)) {
                openedFiles.add(fileName)
                saveOpenedFilesList()
            }
            
            currentFileName = fileName
            true
        } catch (e: Exception) {
            Log.e("FileRepository", "Error opening file: $fileName", e)
            false
        }
    }
    
    /**
     * 删除文件
     * Delete a file from the file system and memory
     * @param fileName 文件名
     * @param onComplete 回调函数，表示删除操作是否完成
     * @return 是否成功删除文件
     */
    fun deleteFile(fileName: String, onComplete: ((Boolean) -> Unit)? = null) {
        try {
            Log.d("FileRepository", "Start deleting file: $fileName")
            files.remove(fileName)
            openedFiles.remove(fileName)
            saveOpenedFilesList()

            // Asynchronously delete a file from the file storage
            fileStorageManager.deleteFileAsync(fileName) { success ->
                onComplete?.invoke(success)
            }
        } catch (e: Exception) {
            Log.e("FileRepository", "An exception occurred during file deletion", e)
            onComplete?.invoke(false)
        }
    }
    
    /**
     * 关闭文件
     * Close a file and remove it from the openedFiles list
     * @param fileName 文件名
     * @return 是否成功关闭文件
     */
    fun closeFile(fileName: String): Boolean {
        return try {
            Log.d("FileRepository", "Closing file: $fileName")
            if (!files.containsKey(fileName)) {
                Log.e("FileRepository", "Trying to close a nonexistent file: $fileName")
                return false
            }

            // Get the file save status
            val codeFile = fileStorageManager.readFile(fileName)
            val isExternallySaved = codeFile?.isExternallySaved ?: false
            val externalUri = codeFile?.externalUri ?: ""

            // If the file has been saved externally, save the latest content to the external location first
            if (isExternallySaved && externalUri.isNotEmpty()) {
                val content = files[fileName] ?: ""
                val language = getFileLanguage(fileName)

                val updatedCodeFile = CodeFile(
                    name = fileName,
                    content = content,
                    language = language,
                    isExternallySaved = true,
                    externalUri = externalUri
                )

                try {
                    fileStorageManager.updateExternalFile(updatedCodeFile)
                } catch (e: Exception) {
                    Log.e("FileRepository", "Failed to update external file: ${e.message}", e)
                }
            }
            openedFiles.remove(fileName)
            saveOpenedFilesList()
            files.remove(fileName)

            // Delete the temporary copy
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                fileStorageManager.deleteFileAsync(fileName) { success ->
                    if (success) {
                        Log.d("FileRepository", "Temporary file asynchronously deleted successfully: $fileName")
                    } else {
                        Log.e("FileRepository", "Failed to asynchronously delete temporary file: $fileName")
                    }
                }
            }, 200)

            true
        } catch (e: Exception) {
            Log.e("FileRepository", "Error closing file: $fileName", e)
            false
        }
    }
    
    /**
     * 更新文件内容
     * Update the content of a file and save it to the file system
     * @param fileName 文件名
     * @param content 新内容
     */
    fun updateFileContent(fileName: String, content: String): Boolean {
        return try {
            if (files.containsKey(fileName)) {
                files[fileName] = content
                val existingFile = fileStorageManager.readFile(fileName)
                val isExternallySaved = existingFile?.isExternallySaved ?: false
                val externalUri = existingFile?.externalUri ?: ""

                // Create a code file object, retaining the external save state and URI
                val codeFile = CodeFile(
                    name = fileName,
                    content = content,
                    language = getFileLanguage(fileName),
                    isExternallySaved = isExternallySaved,
                    externalUri = externalUri
                )

                // Save to internal storage
                val success = fileStorageManager.saveFile(codeFile)

                // If the file has been saved externally, update the external file at the same time
                if (isExternallySaved && externalUri.isNotEmpty()) {
                    try {
                        fileStorageManager.updateExternalFile(codeFile)
                    } catch (e: Exception) {
                        Log.e("FileRepository", "Failed to update external file: ${e.message}", e)
                    }
                }

                success
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("FileRepository", "Failed to update file content: ${e.message}", e)
            false
        }
    }
    
    /**
     * 重命名文件
     * Rename a file and update the file system accordingly
     * @param oldFileName 原文件名
     * @param newFileName 新文件名
     */
    fun renameFile(oldFileName: String, newFileName: String): Boolean {
        return try {
            if (files.containsKey(newFileName)) {
                return false
            }

            val content = files[oldFileName] ?: return false
            val success = fileStorageManager.renameFile(oldFileName, newFileName)
            
            if (success) {
                files.remove(oldFileName)
                files[newFileName] = content

                // Update the list of opened files
                if (openedFiles.contains(oldFileName)) {
                    openedFiles.remove(oldFileName)
                    openedFiles.add(newFileName)
                    saveOpenedFilesList()
                }

                // If the file currently being edited is renamed, update the current file name
                if (currentFileName == oldFileName) {
                    currentFileName = newFileName
                }
                
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("FileRepository", "Failed to rename file: ${e.message}", e)
            false
        }
    }
    
    /**
     * 保存文件到外部存储
     * Save a file to external storage
     * @param fileName 文件名
     * @param uri 外部存储URI
     */
    fun saveFileToUri(fileName: String, uri: Uri): Boolean {
        return try {
            val content = files[fileName] ?: return false
            val language = getFileLanguage(fileName)

            val codeFile = CodeFile(
                name = fileName,
                content = content,
                language = language,
                isExternallySaved = true,
                externalUri = uri.toString()
            )

            fileStorageManager.saveFileToUri(codeFile, uri)
        } catch (e: Exception) {
            Log.e("FileRepository", "Failed to save the file externally: ${e.message}", e)
            false
        }
    }
    
    /**
     * 处理文件保存
     * Handle file save
     * @param fileName 文件名
     * @return 是否成功
     */
    fun handleFileSave(fileName: String): Boolean {
        return try {
            if (!files.containsKey(fileName)) return false

            val codeFile = fileStorageManager.readFile(fileName) ?: return false

            if (codeFile.isExternallySaved && codeFile.externalUri.isNotEmpty()) {
                // Already saved externally, update the external file directly
                val content = files[fileName] ?: ""
                val updatedCodeFile = codeFile.copy(
                    content = content,
                    language = getFileLanguage(fileName),
                    lastModified = System.currentTimeMillis()
                )

                fileStorageManager.updateExternalFile(updatedCodeFile)
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("FileRepository", "Failed to process file save: ${e.message}", e)
            false
        }
    }
    
    /**
     * 从URI导入文件
     * Import a file from a URI
     * @param uri 文件URI
     * @param fileName 文件名
     * @param content 文件内容
     */
    fun importFileFromUri(uri: Uri, fileName: String, content: String): String? {
        return try {
            var finalFileName = fileName
            var counter = 1
            while (files.containsKey(finalFileName)) {
                val baseName = fileName.substringBeforeLast(".")
                val extension = if (fileName.contains(".")) ".${fileName.substringAfterLast(".")}" else ""
                finalFileName = "${baseName}_${counter}${extension}"
                counter++
            }

            val codeFile = CodeFile(
                name = finalFileName,
                content = content,
                language = getFileLanguage(finalFileName),
                isExternallySaved = true,
                externalUri = uri.toString()
            )

            fileStorageManager.saveFile(codeFile)
            files[finalFileName] = content
            openedFiles.add(finalFileName)
            saveOpenedFilesList()

            finalFileName
        } catch (e: Exception) {
            Log.e("FileRepository", "Failed to import file: ${e.message}", e)
            null
        }
    }
    
    /**
     * 根据文件名确定编程语言
     * Determine the programming language based on the file name
     * @param fileName 文件名
     * @return 编程语言
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
    
    /**
     * 获取文件的外部保存状态
     * Get the external save status of a file
     * @return 外部保存状态
     *
     */
    fun getExternalSaveStatus(): Map<String, Boolean> {
        val externalSaveStatus = mutableMapOf<String, Boolean>()
        val codeFiles = fileStorageManager.getAllCodeFiles()
        for (file in codeFiles) {
            if (openedFiles.contains(file.name)) {
                externalSaveStatus[file.name] = file.isExternallySaved
            }
        }
        return externalSaveStatus
    }
} 