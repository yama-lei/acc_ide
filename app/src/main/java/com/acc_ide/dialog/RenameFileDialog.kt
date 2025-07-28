package com.acc_ide.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.acc_ide.R

/**
 * File rename dialog
 * 文件重命名对话框
 */
class RenameFileDialog : DialogFragment() {
    private lateinit var originalFileName: String
    private var onRenameConfirmed: ((String, String) -> Unit)? = null
    
    /**
     * Set up original file name and rename callback
     * 设置原始文件名和重命名回调
     * @param fileName Original file name 原始文件名
     * @param callback Rename callback 重命名回调
     */
    fun setUp(fileName: String, callback: (originalName: String, newName: String) -> Unit): RenameFileDialog {
        this.originalFileName = fileName
        this.onRenameConfirmed = callback
        return this
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        
        // Create dialog view
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_rename_file, null)
        
        // Get input field
        val inputField = view.findViewById<EditText>(R.id.rename_input)
        
        // Set default value to current file name (without extension)
        val extension = originalFileName.substringAfterLast(".", "")
        val nameWithoutExtension = if (extension.isNotEmpty()) {
            originalFileName.substringBeforeLast(".")
        } else {
            originalFileName
        }
        inputField.setText(nameWithoutExtension)
        inputField.setSelection(0, nameWithoutExtension.length)

        builder.setView(view)
            .setTitle(R.string.rename_file)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                val newName = inputField.text.toString().trim()
            
                if (newName.isEmpty()) {
                    Toast.makeText(requireContext(), R.string.file_name_empty, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                
                // Add original extension
                val newFileName = if (extension.isNotEmpty()) {
                    "$newName.$extension"
                } else {
                    newName
                }
                
                // Call callback
                onRenameConfirmed?.invoke(originalFileName, newFileName)
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
        
        return builder.create()
    }
} 