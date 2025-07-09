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
 * 文件列表适配器
 */
class FileListAdapter(
    private var fileList: List<String> = emptyList(),
    private val onFileClickListener: (String) -> Unit,
    private val onRenameClickListener: (String) -> Unit,
    private val onDeleteClickListener: (String) -> Unit,
    private val onCloseClickListener: (String) -> Unit,
    private val onSaveClickListener: (String) -> Unit,
    private val onSaveAsClickListener: (String) -> Unit, // 新增另存为回调
    private var externalSavedFilesMap: Map<String, Boolean> = emptyMap() // 文件名到是否外部保存的映射
) : RecyclerView.Adapter<FileListAdapter.FileViewHolder>() {

    // 当前选中的文件位置
    private var selectedPosition = RecyclerView.NO_POSITION

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.file_list_item, parent, false)
        return FileViewHolder(view)
    }

    override fun onBindViewHolder(holder: FileViewHolder, position: Int) {
        val fileName = fileList[position]
        
        // 检查文件是否已保存到外部
        val isExternallySaved = externalSavedFilesMap[fileName] ?: false
        
        // 设置文件名，添加星号标记仅保存在临时目录的文件
        if (isExternallySaved) {
            holder.fileName.text = fileName // 已保存到外部，不显示星号
        } else {
            holder.fileName.text = "$fileName*" // 添加星号表示仅保存在临时目录
        }
        
        // 设置文件图标（根据文件类型可以设置不同图标）
        when {
            fileName.endsWith(".cpp") -> holder.fileIcon.setImageResource(R.drawable.file_cpp_icon)
            fileName.endsWith(".py") -> holder.fileIcon.setImageResource(R.drawable.file_python_icon)
            fileName.endsWith(".java") -> holder.fileIcon.setImageResource(R.drawable.file_java_icon)
            else -> holder.fileIcon.setImageResource(R.drawable.file_text_icon)
        }
        
        // 设置选中状态
        val isSelected = position == selectedPosition
        holder.fileItemContainer.isChecked = isSelected
        
        // 设置点击事件
        holder.fileItemContainer.setOnClickListener {
            val previousSelected = selectedPosition
            selectedPosition = holder.adapterPosition
            
            // 刷新先前选中和新选中的项
            if (previousSelected != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousSelected)
            }
            notifyItemChanged(selectedPosition)
            
            onFileClickListener(fileName)
        }
        
        // 设置长按事件
        holder.fileItemContainer.setOnLongClickListener {
            showFileOptionsMenu(it, fileName, isExternallySaved)
            true
        }
        
        // 设置关闭按钮点击事件
        holder.closeButton.setOnClickListener {
            onCloseClickListener(fileName)
        }
    }

    override fun getItemCount(): Int = fileList.size
    
    /**
     * 显示文件操作菜单
     */
    private fun showFileOptionsMenu(view: View, fileName: String, isExternallySaved: Boolean) {
        val popupMenu = PopupMenu(view.context, view, Gravity.END)
        popupMenu.inflate(R.menu.file_options_menu)
        
        // 根据文件保存状态设置菜单项文本和可见性
        val saveMenuItem = popupMenu.menu.findItem(R.id.action_save_file)
        val saveAsMenuItem = popupMenu.menu.findItem(R.id.action_save_as_file)
        
        if (isExternallySaved) {
            // 已保存到外部，显示"保存"选项（覆盖保存）
            saveMenuItem.isVisible = true
            saveMenuItem.setTitle(R.string.save)
            
            // 显示"另存为"选项
            saveAsMenuItem.isVisible = true
        } else {
            // 仅保存在临时目录，只显示"保存"选项
            saveMenuItem.isVisible = true
            saveMenuItem.setTitle(R.string.save)
            
            // 隐藏"另存为"选项
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
     * 更新文件列表数据
     */
    fun updateFileList(
        newFileList: List<String>,
        currentFileName: String? = null,
        newExternalSavedMap: Map<String, Boolean> = emptyMap()
    ) {
        fileList = newFileList
        
        // 更新外部保存状态映射
        val updatedMap = mutableMapOf<String, Boolean>()
        updatedMap.putAll(newExternalSavedMap)
        
        // 保存外部保存状态映射
        externalSavedFilesMap = updatedMap
        
        // 如果有当前文件名，更新选中位置
        if (currentFileName != null) {
            selectedPosition = fileList.indexOf(currentFileName)
        }
        
        notifyDataSetChanged()
    }
    
    /**
     * 设置当前选中的文件
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
     * 文件视图持有者
     */
    class FileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileItemContainer: MaterialCardView = itemView.findViewById(R.id.file_item_container)
        val fileIcon: ImageView = itemView.findViewById(R.id.file_icon)
        val fileName: TextView = itemView.findViewById(R.id.file_name)
        val closeButton: MaterialButton = itemView.findViewById(R.id.file_close_button)
    }
} 