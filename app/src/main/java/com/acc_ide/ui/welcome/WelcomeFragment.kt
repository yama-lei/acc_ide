package com.acc_ide.ui.welcome

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import com.acc_ide.R
import com.acc_ide.dialog.NewFileDialogFragment
import com.acc_ide.ui.main.MainActivity
import com.google.android.material.button.MaterialButton

/**
 * Welcome fragment for app startup and new file creation
 * 欢迎Fragment - 用于应用启动和新文件创建
 */
class WelcomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate layout for welcome fragment
        val view = inflater.inflate(R.layout.fragment_welcome, container, false)

        // Get new file button
        val newFileButton = view.findViewById<MaterialButton>(R.id.start_coding_button)

        // Set button click event to show new file dialog
        newFileButton.setOnClickListener {
            showNewFileDialog()
        }

        return view
    }

    /**
     * Show new file dialog for file creation
     * 显示新文件对话框用于文件创建
     */
    private fun showNewFileDialog() {
        NewFileDialogFragment().show(parentFragmentManager, "new_file_dialog")
    }
} 