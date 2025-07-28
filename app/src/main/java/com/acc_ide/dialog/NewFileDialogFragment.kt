package com.acc_ide.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.acc_ide.R
import com.acc_ide.ui.main.MainActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * New file creation dialog
 * 新文件创建对话框
 */
class NewFileDialogFragment : DialogFragment() {

    /**
     * Create dialog for selecting file type
     * 创建用于选择文件类型的对话框
     * @param savedInstanceState 保存的实例状态
     * @return Dialog 对话框
     */
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle(R.string.new_file)
            .setItems(arrayOf(getString(R.string.cpp), getString(R.string.python), getString(R.string.java))) { _, which ->
                val language = when(which) {
                    0 -> "cpp"
                    1 -> "python"
                    2 -> "java"
                    else -> "cpp"
                }
                
                // Create new file with selected language
                val activity = activity as MainActivity
                activity.createNewFile(language)
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                dismiss()
            }
        
        return builder.create()

    }
} 