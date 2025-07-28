package com.acc_ide.dialog

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.acc_ide.R

/**
 * Delete file confirmation dialog
 * 删除文件确认对话框
 */
class DeleteFileConfirmDialog : DialogFragment() {
    
    private lateinit var fileName: String
    private var onDeleteConfirmed: ((String) -> Unit)? = null
    
    /**
     * Set up file name and delete callback
     * 设置文件名和删除回调
     */
    fun setUp(fileName: String, callback: (String) -> Unit): DeleteFileConfirmDialog {
        this.fileName = fileName
        this.onDeleteConfirmed = callback
        return this
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        
        builder.setTitle(R.string.delete_file)
            .setMessage(getString(R.string.delete_file_confirm, fileName))
            .setPositiveButton(R.string.delete) { _, _ ->
                onDeleteConfirmed?.invoke(fileName)
            }
            .setNegativeButton(android.R.string.cancel) { dialog, _ ->
                dialog.cancel()
            }
            
        return builder.create()
    }
} 