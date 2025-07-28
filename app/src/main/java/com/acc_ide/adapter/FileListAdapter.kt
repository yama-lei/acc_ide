package com.acc_ide.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.acc_ide.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import androidx.appcompat.widget.PopupMenu
import android.view.Gravity

/**
 * File List Adapter
 */
class FileListAdapter(
    private var fileList: List<String> = emptyList(),
    private val onFileClickListener: (String) -> Unit,
    private val onRenameClickListener: (String) -> Unit,
    private val onDeleteClickListener: (String) -> Unit,
    private val onCloseClickListener: (String) -> Unit,
    private val onSaveClickListener: (String) -> Unit,
    private val onSaveAsClickListener: (String) -> Unit, 
    private var externalSavedFilesMap: Map<String, Boolean> = emptyMap() // check mapping of filenames to whether to save externally
) : RecyclerView.Adapter<FileListAdapter.FileViewHolder>() {

    private var selectedPosition = RecyclerView.NO_POSITION
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.file_list_item, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val fileName = fileList[position]
        
        // Check if the file has been saved externally
        val isExternallySaved = externalSavedFilesMap[fileName] ?: false
        
        // Set the file name, add an asterisk to mark the file saved only in the temporary directory
        if (isExternallySaved) {
            holder.fileName.text = fileName // Already saved externally, no asterisk is displayed
        } else {
            holder.fileName.text = holder.itemView.context.getString(R.string.file_name_unsaved, fileName) // 添加星号表示仅保存在临时目录
        }
        
        // Set the file icon (different icons can be set according to the file type)
        when {
            fileName.endsWith(".cpp") -> holder.fileIcon.setImageResource(R.drawable.file_cpp_icon)
            fileName.endsWith(".py") -> holder.fileIcon.setImageResource(R.drawable.file_python_icon)
            fileName.endsWith(".java") -> holder.fileIcon.setImageResource(R.drawable.file_java_icon)
            else -> holder.fileIcon.setImageResource(R.drawable.file_text_icon)
        }
        
        // Set the selected state
        val isSelected = position == selectedPosition
        holder.fileItemContainer.isChecked = isSelected
        
        // Set up click event
        holder.fileItemContainer.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = holder.adapterPosition
            if (previousSelected != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousSelected)
            }
            notifyItemChanged(selectedPosition)
            
            onFileClickListener(fileName)
        }
        
        // Set up long press event
        holder.fileItemContainer.setOnLongClickListener {
            showFileOptionsMenu(it, fileName, isExternallySaved)
            true
        }
        
        // Set the close button click event
        holder.closeButton.setOnClickListener {
            onCloseClickListener(fileName)
        }
    }

    override fun getItemCount(): Int = fileList.size
    
    /**
     * Display the file operations menu
     */
    private fun showFileOptionsMenu(view: View, fileName: String, isExternallySaved: Boolean) {
        val popupMenu = PopupMenu(view.context, view, Gravity.END)
        popupMenu.inflate(R.menu.file_options_menu)
        
        // Set the menu item text and visibility based on the file save status
        val saveMenuItem = popupMenu.menu.findItem(R.id.action_save_file)
        val saveAsMenuItem = popupMenu.menu.findItem(R.id.action_save_as_file)
        
        if (isExternallySaved) {
            // Already saved externally, display the "Save" option (overwrite save)
            saveMenuItem.isVisible = true
            saveMenuItem.setTitle(R.string.save)
            
            // Display the "Save As" option
            saveAsMenuItem.isVisible = true
        } else {
            // Only save in the temporary directory and only show the "Save" option
            saveMenuItem.isVisible = true
            saveMenuItem.setTitle(R.string.save)
            
            // Hide the "Save As" option
            saveAsMenuItem.isVisible = false
        }
        
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_rename_file -> {
                    onRenameClickListener(fileName)
                    true
                }
                R.id.action_save_file -> {
                    onSaveClickListener(fileName)
                    true
                }
                R.id.action_save_as_file -> {
                    onSaveAsClickListener(fileName)
                    true
                }
                R.id.action_delete_file -> {
                    onDeleteClickListener(fileName)
                    true
                }
                else -> false
            }
        }
        
        popupMenu.show()
    }
    
    /**
     * Update file list data
     */
    fun updateFileList(
        newFileList: List<String>,
        currentFileName: String? = null,
        newExternalSavedMap: Map<String, Boolean> = emptyMap()
    ) {
        fileList = newFileList
        
        // Update external save state map
        val updatedMap = mutableMapOf<String, Boolean>()
        updatedMap.putAll(newExternalSavedMap)
        
        externalSavedFilesMap = updatedMap
        
        // If there is a current file name, update the selected position
        if (currentFileName != null) {
            selectedPosition = fileList.indexOf(currentFileName)
        }
        
        notifyDataSetChanged()
    }
    
    /**
     * Set the currently selected file
     */
    fun setSelectedFile(fileName: String) {
        val newPosition = fileList.indexOf(fileName)
        if (newPosition >= 0) {
            val previousSelected = selectedPosition
            selectedPosition = newPosition
            
            if (previousSelected != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousSelected)
            }
            notifyItemChanged(selectedPosition)
        }
    }

    /**
     * File View Holder
     */
    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileItemContainer: MaterialCardView = itemView.findViewById(R.id.file_item_container)
        val fileIcon: ImageView = itemView.findViewById(R.id.file_icon)
        val fileName: TextView = itemView.findViewById(R.id.file_name)
        val closeButton: MaterialButton = itemView.findViewById(R.id.file_close_button)
    }
} 