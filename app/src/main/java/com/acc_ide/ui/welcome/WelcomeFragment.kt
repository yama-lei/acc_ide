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

class WelcomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_welcome, container, false)

        // 获取新建文件按钮
        val newFileButton = view.findViewById<MaterialButton>(R.id.start_coding_button)

        // 设置按钮点击事件
        newFileButton.setOnClickListener {
            showNewFileDialog()
        }

        return view
    }

    private fun showNewFileDialog() {
        NewFileDialogFragment().show(parentFragmentManager, "new_file_dialog")
    }
} 