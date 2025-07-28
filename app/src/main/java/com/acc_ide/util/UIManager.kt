package com.acc_ide.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.acc_ide.R
import com.acc_ide.adapter.FileListAdapter
import com.acc_ide.data.repository.FileRepository
import com.acc_ide.dialog.DeleteFileConfirmDialog
import com.acc_ide.dialog.NewFileDialogFragment
import com.acc_ide.dialog.RenameFileDialog
import android.util.Log

/**
 * UI manager for handling UI operations including file list display and dialog management
 * UI管理器 - 负责处理UI相关的操作，包括文件列表显示、对话框管理等
 */
class UIManager(
    private val activity: AppCompatActivity,
    private val fileRepository: FileRepository
) {
    
    private lateinit var fileListAdapter: FileListAdapter
    private lateinit var fileListRecyclerView: RecyclerView
    private var emptyFileListMessage: View? = null
    
    // File operation callbacks
    var onFileOpened: ((String) -> Unit)? = null
    var onFileCreated: ((String, String) -> Unit)? = null
    var onFileImported: ((Uri) -> Unit)? = null
    var onFileSaved: ((String) -> Unit)? = null
    var onFileSaveAs: ((String) -> Unit)? = null
    
    // Open file ActivityResult handler
    private val openFileLauncher = activity.registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            onFileImported?.invoke(uri)
        }
    }
    
    /**
     * Initialize UI components and setup file list
     * 初始化UI组件
     */
    fun setupUI() {
        try {
            // Initialize empty file list message view
            emptyFileListMessage = activity.findViewById(R.id.empty_files_view)

            // Setup file list RecyclerView
            fileListRecyclerView = activity.findViewById(R.id.file_list_recyclerview)
            fileListRecyclerView.layoutManager = LinearLayoutManager(activity)

            // Initialize adapter
            fileListAdapter = FileListAdapter(
                onFileClickListener = { fileName ->
                    onFileOpened?.invoke(fileName)
                },
                onRenameClickListener = { fileName ->
                    showRenameFileDialog(fileName)
                },
                onDeleteClickListener = { fileName ->
                    showDeleteFileDialog(fileName)
                },
                onCloseClickListener = { fileName ->
                    fileRepository.closeFile(fileName)
                    updateFileList()
                    
                    // If closed file is current file, need to switch to other file or welcome page
                    if (fileRepository.currentFileName == fileName) {
                        val openedFiles = fileRepository.getOpenedFiles()
                        if (openedFiles.isEmpty()) {
                            // Logic to show welcome page needs to be handled in MainActivity
                            fileRepository.currentFileName = ""
                        } else {
                            // Switch to last file
                            val lastFile = openedFiles.last()
                            fileRepository.currentFileName = lastFile
                            onFileOpened?.invoke(lastFile)
                        }
                    }
                },
                onSaveClickListener = { fileName ->
                    onFileSaved?.invoke(fileName)
                },
                onSaveAsClickListener = { fileName ->
                    onFileSaveAs?.invoke(fileName)
                }
            )
            fileListRecyclerView.adapter = fileListAdapter

            // Setup open file button
            val openFileButton = activity.findViewById<ImageButton>(R.id.open_file_button)
            openFileButton.setOnClickListener {
                openFileSelector()
            }

            // Setup new file button
            val newFileButton = activity.findViewById<ImageButton>(R.id.new_file_button)
            newFileButton.setOnClickListener {
                showNewFileDialog()
            }

            Log.d("UIManager", "UI components initialized successfully")
        } catch (e: Exception) {
            Log.e("UIManager", "Error setting up UI components", e)
        }
    }
    
    /**
     * Update file list display with current opened files
     * 更新文件列表显示
     */
    fun updateFileList() {
        try {
            // Get file external save status
            val externalSaveStatus = fileRepository.getExternalSaveStatus()

            // Only show opened files
            val openedFiles = fileRepository.getOpenedFiles().toList()
            fileListAdapter.updateFileList(
                openedFiles,
                fileRepository.currentFileName,
                externalSaveStatus
            )

            // Update empty file list message visibility
            if (openedFiles.isEmpty()) {
                emptyFileListMessage?.visibility = View.VISIBLE
            } else {
                emptyFileListMessage?.visibility = View.GONE
            }

            Log.d("UIManager", "File list updated, total ${openedFiles.size} files")
        } catch (e: Exception) {
            Log.e("UIManager", "Error updating file list", e)
        }
    }
    
    /**
     * Set currently selected file in adapter
     * 设置当前选中的文件
     */
    fun setSelectedFile(fileName: String) {
        if (::fileListAdapter.isInitialized) {
            fileListAdapter.setSelectedFile(fileName)
        }
    }
    
    /**
     * Show new file dialog
     * 显示新建文件对话框
     */
    private fun showNewFileDialog() {
        NewFileDialogFragment().show(activity.supportFragmentManager, "new_file_dialog")
    }
    
    /**
     * Show rename file dialog
     * 显示重命名文件对话框
     */
    private fun showRenameFileDialog(fileName: String) {
        RenameFileDialog().setUp(fileName) { oldName, newName ->
            // Check if new file name already exists
            if (fileRepository.files.containsKey(newName)) {
                Toast.makeText(
                    activity, 
                    activity.getString(R.string.file_exists, newName), 
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                // Rename file
                val success = fileRepository.renameFile(oldName, newName)
                if (success) {
                    // Update file list
                    updateFileList()
                    
                    // If renamed file is current file, need to notify callback to update editor
                    if (fileRepository.currentFileName == newName) {
                        onFileOpened?.invoke(newName)
                    }
                    
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.file_renamed, oldName, newName),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        activity,
                        "Rename failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }.show(activity.supportFragmentManager, "RenameFileDialog")
    }
    
    /**
     * Show delete file confirmation dialog
     * 显示删除文件确认对话框
     */
    private fun showDeleteFileDialog(fileName: String) {
        DeleteFileConfirmDialog().setUp(fileName) { fileToDelete ->
            fileRepository.deleteFile(fileToDelete) { success ->
                if (success) {
                    // Update file list
                    updateFileList()
                    
                    // If deleted file is current file, need to switch to other file or welcome page
                    if (fileRepository.currentFileName == fileToDelete) {
                        val openedFiles = fileRepository.getOpenedFiles()
                        if (openedFiles.isEmpty()) {
                            // Logic to show welcome page needs to be handled in MainActivity
                            fileRepository.currentFileName = ""
                        } else {
                            // Switch to last file
                            val lastFile = openedFiles.last()
                            fileRepository.currentFileName = lastFile
                            onFileOpened?.invoke(lastFile)
                        }
                    }
                    
                    Toast.makeText(
                        activity,
                        activity.getString(R.string.file_deleted, fileToDelete),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(activity, "Delete file failed", Toast.LENGTH_SHORT).show()
                }
            }
        }.show(activity.supportFragmentManager, "DeleteFileConfirmDialog")
    }
    
    /**
     * Open file selector for importing files
     * 打开文件选择器
     */
    private fun openFileSelector() {
        try {
            // Open file selector, specify supported file types
            openFileLauncher.launch(
                arrayOf(
                    "text/plain",
                    "text/x-c++src", // C++
                    "text/x-java", // Java
                    "text/x-python", // Python
                    "application/octet-stream", // Generic binary file
                    "*/*" // All file types as fallback
                )
            )
        } catch (e: Exception) {
            Log.e("UIManager", "Failed to open file selector: ${e.message}")
            Toast.makeText(activity, "Failed to open file selector: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Handle imported file from file selector
     * 处理从文件选择器导入的文件
     */
    fun handleImportedFile(uri: Uri) {
        try {
            Log.d("UIManager", "User selected file: $uri")

            var fileName = getFileName(uri)

            // If cannot get file name, use default name
            if (fileName.isBlank()) {
                fileName = "Imported_${System.currentTimeMillis()}.txt"
            }

            // Read file content
            val inputStream = activity.contentResolver.openInputStream(uri)
            val content = inputStream?.use { stream ->
                stream.bufferedReader().use { it.readText() }
            } ?: ""

            // Try to get persistent permission
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                activity.contentResolver.takePersistableUriPermission(uri, takeFlags)
                Log.d("UIManager", "Obtained persistent permission for file: $uri")
            } catch (e: Exception) {
                Log.w("UIManager", "Cannot get persistent permission for file: ${e.message}")
            }

            // Import file
            val finalFileName = fileRepository.importFileFromUri(uri, fileName, content)
            
            if (finalFileName != null) {
                // Update file list
                updateFileList()

                // Open editor to display file
                onFileOpened?.invoke(finalFileName)

                // Show import success message
                Toast.makeText(activity, "Imported file: $finalFileName", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(activity, "Failed to import file", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("UIManager", "Failed to handle selected file: ${e.message}")
            Toast.makeText(activity, "Cannot open selected file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Get file name from URI
     * 获取URI对应的文件名
     */
    private fun getFileName(uri: Uri): String {
        // Try to get file name from content provider
        val cursor = activity.contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex =
                    it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    return it.getString(displayNameIndex)
                }
            }
        }

        // If cannot get from content provider, try to get from URI path
        val path = uri.path
        if (path != null) {
            val cut = path.lastIndexOf('/')
            if (cut != -1) {
                return path.substring(cut + 1)
            }
        }

        // If all methods fail, return empty string
        return ""
    }
    
    /**
     * Show toast message
     * 显示Toast消息
     */
    fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(activity, message, duration).show()
    }
    
    /**
     * Show toast message using string resource
     * 显示Toast消息（使用字符串资源）
     */
    fun showToast(messageResId: Int, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(activity, messageResId, duration).show()
    }
} 