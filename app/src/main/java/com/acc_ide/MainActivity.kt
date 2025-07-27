package com.acc_ide

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.acc_ide.adapter.FileListAdapter
import com.acc_ide.dialog.DeleteFileConfirmDialog
import com.acc_ide.dialog.RenameFileDialog
import com.acc_ide.model.CodeFile
import com.acc_ide.util.FileStorageManager
import com.acc_ide.util.LocaleHelper
import com.acc_ide.util.TemplateManager
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.Locale
import androidx.preference.PreferenceManager
import androidx.appcompat.app.AppCompatDelegate
import android.net.Uri
import android.app.Activity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import com.acc_ide.util.TextMateManager

/**
 * 主活动类
 * 负责管理应用程序的主界面、侧边栏、文件操作等核心功能
 */
class MainActivity : AppCompatActivity() {
    // 界面组件
    private lateinit var drawerLayout: DrawerLayout
    lateinit var actionBarDrawerToggle: ActionBarDrawerToggle
    private lateinit var fileListAdapter: FileListAdapter
    private lateinit var fileListRecyclerView: RecyclerView
    private var isDrawerToggleEnabled = true
    private var emptyFileListMessage: View? = null

    // 文件存储管理器
    private lateinit var fileStorageManager: FileStorageManager

    // Template manager
    private lateinit var templateManager: TemplateManager

    // 用于在内存中存储代码文件的Map
    val files = mutableMapOf<String, String>()
    var currentFileName: String = ""

    // 用于存储当前打开的文件列表 (只存文件名)
    private val openedFiles = mutableSetOf<String>()

    // 存储权限请求
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var allGranted = true
        permissions.entries.forEach {
            if (!it.value) {
                allGranted = false
                return@forEach
            }
        }

        if (allGranted) {
            // 权限已授予，初始化存储和加载文件
            initializeFileSystem()
        } else {
            // 权限被拒绝，显示提示
            showPermissionExplanationDialog()
        }
    }

    // 打开文件的ActivityResult处理
    private val openFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            handleOpenedFile(uri)
        }
    }

    // 注册文件保存结果处理
    private val saveFileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data
            if (uri != null && fileToSave.isNotEmpty()) {
                handleSaveFile(fileToSave, uri)
                fileToSave = "" // 重置
            }
        }
    }

    // 存储当前要保存的文件名
    private var fileToSave: String = ""

    // 权限请求常量
    private val REQUEST_STORAGE_PERMISSION = 100

    /**
     * Activity创建时的初始化
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            // 应用保存的主题设置
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val nightMode = prefs.getInt("app_night_mode", AppCompatDelegate.MODE_NIGHT_YES)
            AppCompatDelegate.setDefaultNightMode(nightMode)

            // 应用TextMate主题 - 确保在Activity创建时正确设置编辑器主题
            applyCodeEditorTheme()

            // 加载已保存的语言设置
            applyLanguage()

            // 设置主界面布局
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_main)

            // 设置工具栏
            val toolbar: Toolbar = findViewById(R.id.toolbar)
            setSupportActionBar(toolbar)

            // 初始化绘制布局
            drawerLayout = findViewById(R.id.drawer_layout)

            // 初始化UI组件
            setupUI()

            // 初始化文件存储管理器
            fileStorageManager = FileStorageManager(this)

            // 创建模板管理器
            templateManager = TemplateManager(this)

            // 打印外部存储目录路径，方便调试
            Log.i("MainActivity", "外部存储路径: ${getExternalFilesDir(null)?.absolutePath}")
            Log.i(
                "MainActivity",
                "代码文件目录路径: ${fileStorageManager.getCodeFilesDir().absolutePath}"
            )

            // 验证文件系统完整性，在应用启动时清理可能的问题
            // 注意：这些初始化操作已移至SplashActivity

            // 检查文件存储权限 - 权限通过后会初始化模板系统和文件系统
            requestStoragePermission()

            // 设置设置按钮
            val settingsItem = findViewById<LinearLayout>(R.id.settings_item)
            settingsItem.setOnClickListener {
                // 打开设置界面
                supportFragmentManager.beginTransaction()
                    .replace(R.id.content_frame, SettingsFragment())
                    .addToBackStack(null)
                    .commit()

                // 关闭侧边栏
                drawerLayout.closeDrawer(GravityCompat.START)

                // 锁定抽屉，防止在设置页面滑动打开
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            }

            // 设置ActionBarDrawerToggle
            setupNavigationDrawer(toolbar)

            // 监听Fragment变化，确保设置页面显示返回按钮
            supportFragmentManager.registerFragmentLifecycleCallbacks(object :
                FragmentManager.FragmentLifecycleCallbacks() {
                override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
                    // 当任何Fragment恢复时，更新导航图标
                    updateNavigationIcon()

                    // 如果是设置页面，设置标题
                    if (f is SettingsFragment) {
                        supportActionBar?.title = getString(R.string.settings)
                    }
                }
            }, true)

            // 检查是否从语言切换回来
            val savedState = intent.getBundleExtra("savedState")
            if (savedState != null) {
                // 恢复之前保存的状态
                val currentFragment = savedState.getString("currentFragment")
                val needsBackButton = savedState.getBoolean("needsBackButton", false)
                currentFileName = savedState.getString("currentFileName") ?: ""

                Log.d(
                    "MainActivity",
                    "恢复状态: currentFragment=$currentFragment, needsBackButton=$needsBackButton"
                )

                // 如果需要显示返回按钮，立即设置
                if (needsBackButton) {
                    // 锁定抽屉
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                    isDrawerToggleEnabled = false

                    // 强制显示返回按钮
                    forceShowBackButton()

                    // 设置标题
                    supportActionBar?.title = getString(R.string.settings)

                    // 打开设置页面
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.content_frame, SettingsFragment())
                        .commit()
                } else {
                    // 处理其他类型的Fragment恢复
                    if (!currentFileName.isEmpty()) {
                        // 如果有当前文件，打开它
                        loadFileList()
                        openFile(currentFileName)
                    } else {
                        // 否则加载欢迎页面
                        showWelcomeFragment()
                    }
                }
            } else {
                // 初始状态，加载文件列表
                loadFileList()

                // 检查是否有已打开的文件
                if (openedFiles.isNotEmpty()) {
                    // 如果有已打开的文件，打开最后一个文件
                    val lastOpenedFile = openedFiles.last()
                    openFile(lastOpenedFile)
                    Log.d("MainActivity", "启动时打开上次的文件: $lastOpenedFile")
                } else {
                    // 如果没有已打开的文件，显示欢迎页面
                    showWelcomeFragment()
                    Log.d("MainActivity", "没有已打开的文件，显示欢迎页面")
                }
            }
        } catch (e: Exception) {
            // 捕获任何未处理的异常，确保应用不会崩溃
            Log.e("MainActivity", "onCreate发生未处理的异常", e)

            // 显示一个错误对话框
            AlertDialog.Builder(this)
                .setTitle(R.string.error_title)
                .setMessage(getString(R.string.error_message, e.message))
                .setPositiveButton(R.string.ok_button, null)
                .show()
        }

        // 监听返回栈变化，确保从设置页面返回时标题和UI状态正确更新
        supportFragmentManager.addOnBackStackChangedListener {
            // 获取当前顶部Fragment
            val currentFragment = supportFragmentManager.findFragmentById(R.id.content_frame)
            Log.d(
                "MainActivity",
                "返回栈变化，当前Fragment: ${currentFragment?.javaClass?.simpleName}"
            )

            // 更新导航图标和标题
            updateNavigationIcon()

            // 如果返回到编辑器页面，确保刷新内容和语法高亮
            if (currentFragment is EditorFragment && currentFileName.isNotEmpty()) {
                // 立即尝试一次刷新
                currentFragment.refreshEditorTheme()

                // 确保从设置页返回时编辑器内容和语法高亮正确
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        // 确保编辑器已完全恢复并触发语法高亮更新
                        Log.d("MainActivity", "从设置页返回，刷新编辑器: $currentFileName")

                        // 重新应用TextMate语法高亮
                        val language = currentFragment.getLanguageForFile(currentFileName)
                        currentFragment.setupLanguageSupport(language)

                        // 刷新编辑器主题
                        currentFragment.refreshEditorTheme()

                        Log.d("MainActivity", "语法高亮刷新完成，使用语言: $language")
                    } catch (e: Exception) {
                        Log.e("MainActivity", "从设置页返回刷新编辑器失败: ${e.message}")
                    }
                }, 200) // 延长延迟确保Fragment完全恢复

                // 再增加一次延迟更长的刷新，以防前两次没有生效
                Handler(Looper.getMainLooper()).postDelayed({
                    currentFragment.refreshEditorTheme()
                    Log.d("MainActivity", "执行最终的语法高亮刷新尝试")
                }, 500)
            }
        }
    }

    /**
     * 应用代码编辑器主题
     * 确保在Activity创建时正确设置TextMate主题
     */
    private fun applyCodeEditorTheme() {
        // 尝试设置正确的主题
        try {
            // 根据系统主题设置TextMate主题
            val isDarkMode =
                (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                        Configuration.UI_MODE_NIGHT_YES
            val textMateTheme = if (isDarkMode) "dark.json" else "light.json"

            // 应用TextMate主题
            TextMateManager.setTheme(textMateTheme)
            Log.d("MainActivity", "onCreate: 应用主题: $textMateTheme")
        } catch (e: Exception) {
            Log.e("MainActivity", "onCreate: 应用主题失败: ${e.message}")
        }
    }

    /**
     * 清理过期的删除标记
     * 在应用启动时运行，清理那些已不存在文件的删除标记
     * 注意：我们现在使用数字前缀命名文件，不再需要删除标记
     */
    private fun cleanupExpiredDeletionMarkers() {
        try {
            Log.d("MainActivity", "开始清理过期的文件...")

            // 获取实际文件列表
            val filesDir = fileStorageManager.getCodeFilesDir()
            val actualFiles = filesDir.listFiles() ?: return

            // 检查是否有元数据文件但没有对应的主文件
            val mainFiles =
                actualFiles.filter { !it.name.endsWith(".meta") }.map { it.name }.toSet()
            val metaFiles = actualFiles.filter { it.name.endsWith(".meta") }
                .map { it.name.removeSuffix(".meta") }.toSet()

            // 找出孤立的元数据文件（没有对应主文件的元数据文件）
            val orphanedMetaFiles = metaFiles.filter { !mainFiles.contains(it) }

            // 删除孤立的元数据文件
            if (orphanedMetaFiles.isNotEmpty()) {
                Log.d(
                    "MainActivity",
                    "发现 ${orphanedMetaFiles.size} 个孤立的元数据文件，正在清理..."
                )

                for (fileName in orphanedMetaFiles) {
                    val metaFile = File(filesDir, "$fileName.meta")
                    if (metaFile.exists() && metaFile.delete()) {
                        Log.d("MainActivity", "已删除孤立的元数据文件: $fileName.meta")
                    } else {
                        Log.e("MainActivity", "无法删除孤立的元数据文件: $fileName.meta")
                    }
                }
            }

            // 清空旧的删除记录，因为我们不再使用它
            val prefs = getSharedPreferences("acc_ide_prefs", Context.MODE_PRIVATE)
            if (prefs.contains("deleted_files")) {
                prefs.edit().remove("deleted_files").apply()
                Log.d("MainActivity", "已清空旧的删除记录")
            }

        } catch (e: Exception) {
            Log.e("MainActivity", "清理过期文件时出错", e)
        }
    }

    /**
     * 验证文件系统完整性，清理可能存在的问题
     */
    private fun verifyFileSystemIntegrity() {
        try {
            Log.d("MainActivity", "正在验证文件系统完整性...")

            // 清空内存中的文件列表，确保从干净状态开始
            files.clear()

            // 加载文件前，先进行一次刷新以确保文件系统信息是最新的
            fileStorageManager.refreshFileSystem()

            // 检查被删除文件的记录
            val prefs = getSharedPreferences("acc_ide_prefs", Context.MODE_PRIVATE)
            val deletedFiles = prefs.getStringSet("deleted_files", emptySet()) ?: emptySet()

            // 清理可能已经不存在的文件的删除记录
            val filesDir = fileStorageManager.getCodeFilesDir()
            val actualFileNames = filesDir.listFiles()
                ?.filter { !it.name.endsWith(".meta") }
                ?.map { it.name }
                ?.toSet() ?: emptySet()

            // 找出需要从已删除列表中移除的文件（因为它们已经不存在了）
            val toRemoveFromDeleted = deletedFiles.filter { !actualFileNames.contains(it) }.toSet()

            // 如果有需要清理的记录，更新SharedPreferences
            if (toRemoveFromDeleted.isNotEmpty()) {
                val newDeletedFiles = HashSet(deletedFiles)
                newDeletedFiles.removeAll(toRemoveFromDeleted)

                prefs.edit()
                    .putStringSet("deleted_files", newDeletedFiles)
                    .apply()

                Log.d("MainActivity", "已清理 ${toRemoveFromDeleted.size} 个不存在文件的删除记录")
            }

            // 从存储加载所有文件，这个过程会自动清理孤立文件
            val storedFiles = fileStorageManager.getAllFiles()
            Log.d("MainActivity", "文件系统验证完成，找到 ${storedFiles.size} 个有效文件")

            // 记录未删除的文件总数，用于验证
            var validFileCount = 0

            // 将文件加载到内存 - 只加载未被删除的文件
            for (file in storedFiles) {
                // 只加载未被标记为删除的文件
                if (!deletedFiles.contains(file.name)) {
                    files[file.name] = file.content
                    validFileCount++
                } else {
                    Log.d("MainActivity", "跳过已删除的文件: ${file.name}")
                }
            }

            Log.d(
                "MainActivity",
                "加载了 $validFileCount 个有效文件，跳过 ${storedFiles.size - validFileCount} 个已删除文件"
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "文件系统验证失败", e)
        }
    }

    /**
     * 检查是否有存储权限
     *
     * @return 是否有必要的存储权限
     */
    private fun hasStoragePermissions(): Boolean {
        return when {
            // Android 13+ (API 33+) 使用新的权限模型
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // 检查图片、视频、音频媒体权限（替代存储权限）
                val imagePermission =
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                val videoPermission =
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                val audioPermission =
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)

                imagePermission == PackageManager.PERMISSION_GRANTED &&
                        videoPermission == PackageManager.PERMISSION_GRANTED &&
                        audioPermission == PackageManager.PERMISSION_GRANTED
            }

            // Android 10 (API 29) - 12 (API 32) 使用分区存储
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // 对应用专用目录的访问不需要特殊权限
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }

            // Android 9 (API 28) 及以下使用传统存储权限
            else -> {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
    }

    /**
     * 检查并请求存储权限
     */
    private fun checkStoragePermissions() {
        if (!hasStoragePermissions()) {
            requestStoragePermission()
        } else {
            // 已有权限，初始化文件系统
            initializeFileSystem()
        }
    }

    /**
     * 请求存储权限
     */
    private fun requestStoragePermission() {
        val permissionCheck = when {
            // Android 13+ (API 33+) 使用新的权限模型
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // 检查图片、视频、音频媒体权限（替代存储权限）
                val imagePermission =
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                val videoPermission =
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                val audioPermission =
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)

                imagePermission == PackageManager.PERMISSION_GRANTED &&
                        videoPermission == PackageManager.PERMISSION_GRANTED &&
                        audioPermission == PackageManager.PERMISSION_GRANTED
            }

            // Android 10 (API 29) - 12 (API 32) 使用分区存储
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // 对应用专用目录的访问不需要特殊权限
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }

            // Android 9 (API 28) 及以下使用传统存储权限
            else -> {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        // 根据权限检查结果决定请求或初始化
        if (!permissionCheck) {
            // 显示权限解释对话框
            val alertBuilder = AlertDialog.Builder(this)
            alertBuilder.setTitle(R.string.storage_permission_title)
            alertBuilder.setMessage(R.string.storage_permission_message)
            alertBuilder.setPositiveButton("OK") { _, _ ->
                // 请求相应权限
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Android 13+: 请求媒体权限
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            Manifest.permission.READ_MEDIA_IMAGES,
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.READ_MEDIA_AUDIO
                        ),
                        REQUEST_STORAGE_PERMISSION
                    )
                } else {
                    // Android 12-: 请求存储权限
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        ),
                        REQUEST_STORAGE_PERMISSION
                    )
                }
            }
            alertBuilder.show()
        } else {
            // 已有权限，初始化文件系统
            initializeFileSystem()

            // 初始化模板系统
            initializeTemplateSystem()
        }
    }

    /**
     * 显示权限解释对话框
     */
    private fun showPermissionExplanationDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.storage_permission_title)
            .setMessage(R.string.storage_permission_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                requestStoragePermission()
            }
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                Toast.makeText(this, R.string.storage_permission_denied, Toast.LENGTH_LONG).show()
            }
            .create()
            .show()
    }

    /**
     * 初始化文件系统
     * 当权限已获取时调用
     */
    private fun initializeFileSystem() {
        // 加载已打开的文件列表
        loadOpenedFilesList()

        // 加载所有文件
        loadFileList()

        Log.d("MainActivity", "文件系统初始化完成")
    }

    /**
     * 初始化模板系统
     * 当权限已获取时调用
     */
    private fun initializeTemplateSystem() {
        try {
            // 初始化模板管理器并创建默认模板
            templateManager = TemplateManager(this)
            templateManager.initializeTemplates()
            Log.d("MainActivity", "模板系统初始化完成")
        } catch (e: Exception) {
            Log.e("MainActivity", "模板系统初始化失败", e)
        }
    }

    /**
     * 从存储加载文件
     */
    private fun loadFilesFromStorage() {
        try {
            Log.d("MainActivity", "开始从存储加载文件...")

            // 清空内存中的文件列表
            files.clear()

            // 先强制刷新文件系统
            try {
                fileStorageManager.refreshFileSystem()
            } catch (e: Exception) {
                Log.e("MainActivity", "刷新文件系统时出错", e)
            }

            // 加载上次打开的文件列表
            loadOpenedFilesList()

            // 获取文件系统中实际存在的文件
            val filesDir = fileStorageManager.getCodeFilesDir()
            val actualFileNames = filesDir.listFiles()
                ?.filter { !it.name.endsWith(".meta") }
                ?.map { it.name }
                ?.toSet() ?: emptySet()

            Log.d("MainActivity", "文件系统中实际存在的文件: ${actualFileNames.joinToString()}")

            // 清理openedFiles中不存在的文件
            val notExistFiles = openedFiles.filter { !actualFileNames.contains(it) }.toSet()
            if (notExistFiles.isNotEmpty()) {
                openedFiles.removeAll(notExistFiles)
                saveOpenedFilesList()
                Log.d(
                    "MainActivity",
                    "从已打开文件列表中清理了 ${notExistFiles.size} 个不存在的文件: ${notExistFiles.joinToString()}"
                )
            }

            // 从存储加载当前打开的文件
            val storedFiles = fileStorageManager.getAllFiles()
            Log.d("MainActivity", "存储中找到 ${storedFiles.size} 个文件")

            // 如果没有已打开的文件，但有存储的文件，则将所有有效文件添加到已打开文件列表
            // 这样可以确保文件树中显示所有可用的文件
            if (openedFiles.isEmpty() && storedFiles.isNotEmpty()) {
                // 找出所有有效文件（存在于文件系统中的文件）
                val validFiles = storedFiles.filter {
                    actualFileNames.contains(it.name) && it.name.isNotBlank()
                }

                if (validFiles.isNotEmpty()) {
                    // 将所有有效文件添加到已打开文件列表
                    validFiles.forEach { file ->
                        openedFiles.add(file.name)
                    }
                    saveOpenedFilesList()
                    Log.d(
                        "MainActivity",
                        "没有已打开的文件，添加了 ${validFiles.size} 个文件到文件树: ${
                            validFiles.map { it.name }.joinToString()
                        }"
                    )
                }
            }

            // 计数器，用于记录有效文件数
            var validFileCount = 0

            // 将已打开的文件加载到内存
            for (fileName in openedFiles.toList()) {
                // 跳过不存在于文件系统中的文件
                if (!actualFileNames.contains(fileName)) {
                    openedFiles.remove(fileName)
                    Log.d("MainActivity", "跳过不存在的文件: $fileName")
                    continue
                }

                // 查找对应的文件内容
                val fileContent = storedFiles.find { it.name == fileName }?.content ?: ""

                // 确保文件内容已加载
                if (fileContent.isNotEmpty()) {
                    files[fileName] = fileContent
                    validFileCount++
                    Log.d("MainActivity", "已加载文件: $fileName, 内容长度: ${fileContent.length}")
                } else {
                    // 文件可能已被删除或损坏，从打开列表中移除
                    openedFiles.remove(fileName)
                    Log.w("MainActivity", "文件内容为空，从打开列表移除: $fileName")
                }
            }

            // 保存更新后的已打开文件列表
            saveOpenedFilesList()

            Log.d(
                "MainActivity",
                "从存储加载了 ${openedFiles.size} 个已打开的文件，有效文件: $validFileCount 个"
            )

            // 更新文件列表UI (只有在适配器已初始化的情况下)
            if (::fileListAdapter.isInitialized) {
                updateFileList()
            } else {
                Log.d("MainActivity", "fileListAdapter尚未初始化，跳过更新UI")
            }

            // 输出当前文件列表状态
            Log.d("MainActivity", "当前文件列表: ${files.keys.joinToString(", ")}")
        } catch (e: Exception) {
            Log.e("MainActivity", "加载文件时出错", e)
        }
    }

    /**
     * 加载已打开的文件列表
     */
    private fun loadOpenedFilesList() {
        val prefs = getSharedPreferences("acc_ide_prefs", Context.MODE_PRIVATE)
        val openedFileSet = prefs.getStringSet("opened_files", null)

        // 清空当前列表并加载保存的列表
        openedFiles.clear()
        if (openedFileSet != null) {
            openedFiles.addAll(openedFileSet)
            Log.d("MainActivity", "已加载上次打开的文件列表: ${openedFiles.joinToString(", ")}")
        } else {
            Log.d("MainActivity", "没有找到已保存的文件列表")
        }
    }

    /**
     * 保存已打开的文件列表
     */
    private fun saveOpenedFilesList() {
        val prefs = getSharedPreferences("acc_ide_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet("opened_files", HashSet(openedFiles))
            .apply()
        Log.d("MainActivity", "已保存当前打开的文件列表: ${openedFiles.joinToString(", ")}")
    }

    // 应用恢复时刷新文件
    override fun onResume() {
        super.onResume()
        try {
            // 确保每次恢复时应用正确的编辑器主题
            applyCodeEditorTheme()

            // 如果当前有编辑器片段，刷新其主题
            val currentFragment = supportFragmentManager.findFragmentById(R.id.content_frame)
            if (currentFragment is EditorFragment) {
                // 延迟一点执行，确保Activity完全恢复
                Handler(Looper.getMainLooper()).postDelayed({
                    refreshEditorSyntaxHighlighting()
                    Log.d("MainActivity", "onResume: 已刷新编辑器主题")
                }, 100)
            }

            // 如果fileStorageManager已初始化，则刷新文件列表
            if (::fileStorageManager.isInitialized) {
                // 只刷新文件信息，不完全重载
                fileStorageManager.refreshFileSystem()

                // 更新UI
                if (::fileListAdapter.isInitialized) {
                    updateFileList()
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "恢复应用时刷新文件失败", e)
        }
    }

    // 设置导航抽屉
    private fun setupNavigationDrawer(toolbar: androidx.appcompat.widget.Toolbar) {
        actionBarDrawerToggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(actionBarDrawerToggle)
        actionBarDrawerToggle.syncState()
        isDrawerToggleEnabled = true
    }

    // 禁用导航抽屉开关
    fun disableDrawerToggle() {
        // 禁用抽屉滑动
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        isDrawerToggleEnabled = false

        // 完全禁用ActionBarDrawerToggle
        actionBarDrawerToggle.isDrawerIndicatorEnabled = false
        actionBarDrawerToggle.setToolbarNavigationClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 直接设置返回箭头
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)

        Log.d("MainActivity", "已禁用导航抽屉并设置返回按钮")
    }

    // 重置导航抽屉
    fun resetNavigationDrawer() {
        updateNavigationIcon()
    }

    // 强制返回按钮显示（用于设置页面等）
    fun forceShowBackButton() {
        // 完全禁用ActionBarDrawerToggle
        actionBarDrawerToggle.isDrawerIndicatorEnabled = false
        actionBarDrawerToggle.setToolbarNavigationClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // 直接设置返回箭头
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_arrow_back)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        Log.d("MainActivity", "已强制显示返回按钮")
    }

    // 处理返回按钮点击
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // 先让ActionBarDrawerToggle处理点击事件
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true
        }

        // 处理其他菜单项
        when (item.itemId) {
            android.R.id.home -> {
                // 如果抽屉不可用（例如在设置页面），则按下返回键
                if (!isDrawerToggleEnabled) {
                    onBackPressedDispatcher.onBackPressed()
                    return true
                }
                return false
            }

            else -> return super.onOptionsItemSelected(item)
        }
    }

    // 处理系统返回按钮
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // 如果抽屉打开，先关闭抽屉
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
            return
        }

        // 获取当前Fragment
        val currentFragment = supportFragmentManager.findFragmentById(R.id.content_frame)

        // 如果当前是欢迎页面，并且有已打开的文件，则打开最后一个文件
        if (currentFragment is WelcomeFragment && openedFiles.isNotEmpty()) {
            val lastOpenedFile = openedFiles.last()
            openFile(lastOpenedFile)
            return
        }

        // 检查是否有IO面板打开，如果有，关闭它
        val ioPanel = supportFragmentManager.findFragmentByTag("io_panel")
        if (ioPanel != null) {
            supportFragmentManager.beginTransaction()
                .remove(ioPanel)
                .commit()
            supportFragmentManager.popBackStack()

            // 重置ActionBar和菜单状态
            Handler(Looper.getMainLooper()).postDelayed({
                updateNavigationIcon()
            }, 100)
            return
        }

        // 如果当前是设置页面，手动处理返回逻辑
        if (currentFragment is SettingsFragment) {
            // 恢复主页面
            if (files.isEmpty()) {
                // 如果没有文件，显示欢迎界面
                supportFragmentManager.beginTransaction()
                    .replace(R.id.content_frame, WelcomeFragment())
                    .commit()
                currentFileName = ""
                supportActionBar?.title = getString(R.string.welcome)
            } else {
                // 如果有文件，显示第一个文件或当前文件
                if (currentFileName.isEmpty() || !files.containsKey(currentFileName)) {
                    currentFileName = files.keys.first()
                }
                showEditorWithFile(currentFileName, getFileLanguage(currentFileName))
            }

            // 解锁抽屉
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)

            // 恢复菜单图标
            Handler(Looper.getMainLooper()).postDelayed({
                updateNavigationIcon()
            }, 100)
            return
        }

        // 如果有回退栈，弹出一个页面
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
            // 延迟更新导航图标，确保Fragment转换完成
            Handler(Looper.getMainLooper()).postDelayed({
                updateNavigationIcon()
            }, 100)
            return
        }

        // 默认行为
        @Suppress("DEPRECATION")
        super.onBackPressed()
    }

    // 应用语言设置
    private fun applyLanguage() {
        val savedLanguage = LocaleHelper.getLanguage(this)
        // 使用已保存的语言，如果没有则使用系统默认语言
        if (savedLanguage.isEmpty()) {
            // 如果没有保存的语言设置，使用系统默认语言
            LocaleHelper.setLocale(this, Locale.getDefault().language)
        } else {
            // 否则使用保存的语言设置
            LocaleHelper.setLocale(this, savedLanguage)
        }
    }

    // 用于应用自定义的语言设置
    override fun attachBaseContext(newBase: Context) {
        val savedLanguage = LocaleHelper.getLanguage(newBase)
        if (savedLanguage.isEmpty()) {
            // 如果没有保存的语言，使用系统默认语言
            super.attachBaseContext(newBase)
        } else {
            // 否则使用保存的语言
            super.attachBaseContext(LocaleHelper.setLocale(newBase, savedLanguage))
        }
    }

    /**
     * 更新文件列表显示
     */
    private fun updateFileList() {
        try {
            if (!::fileListAdapter.isInitialized) {
                Log.d("MainActivity", "初始化文件列表适配器")
                fileListAdapter = FileListAdapter(
                    onFileClickListener = { fileName ->
                        openFile(fileName)
                    },
                    onRenameClickListener = { fileName ->
                        showRenameFileDialog(fileName)
                    },
                    onDeleteClickListener = { fileName ->
                        showDeleteFileDialog(fileName)
                    },
                    onCloseClickListener = { fileName ->
                        closeFile(fileName)
                    },
                    onSaveClickListener = { fileName ->
                        handleFileSave(fileName)
                    },
                    onSaveAsClickListener = { fileName ->
                        saveFileToExternal(fileName)
                    }
                )
                fileListRecyclerView.adapter = fileListAdapter
            }

            // 获取文件外部保存状态
            val externalSaveStatus = mutableMapOf<String, Boolean>()
            val codeFiles = fileStorageManager.getAllCodeFiles()
            for (file in codeFiles) {
                if (openedFiles.contains(file.name)) {
                    externalSaveStatus[file.name] = file.isExternallySaved
                }
            }

            // 只显示已打开的文件
            fileListAdapter.updateFileList(
                openedFiles.toList(),
                currentFileName,
                externalSaveStatus
            )

            // 更新空文件列表提示的可见性
            if (openedFiles.isEmpty()) {
                emptyFileListMessage?.visibility = View.VISIBLE
            } else {
                emptyFileListMessage?.visibility = View.GONE
            }

            Log.d("MainActivity", "文件列表已更新，共 ${openedFiles.size} 个文件")
        } catch (e: Exception) {
            Log.e("MainActivity", "更新文件列表出错", e)
        }
    }

    /**
     * 确保内存中的文件与已打开文件列表同步
     */
    private fun synchronizeFilesWithOpenedList() {
        try {
            // 获取已打开文件列表和内存中的文件列表
            val currentOpenedFiles = HashSet(openedFiles)
            val currentLoadedFiles = HashSet(files.keys)

            // 检查文件系统中的文件
            val storedFiles = fileStorageManager.getAllFiles()
            val storedFileMap = storedFiles.associateBy { it.name }

            // 检查已删除文件的记录
            val prefs = getSharedPreferences("acc_ide_prefs", Context.MODE_PRIVATE)
            val deletedFiles = prefs.getStringSet("deleted_files", emptySet()) ?: emptySet()

            // 找出已打开但未加载到内存的文件
            for (fileName in currentOpenedFiles) {
                // 跳过已删除的文件
                if (deletedFiles.contains(fileName)) {
                    openedFiles.remove(fileName)
                    Log.d("MainActivity", "从已打开列表移除已删除的文件: $fileName")
                    continue
                }

                // 如果文件在已打开列表中但不在内存中，尝试加载
                if (!currentLoadedFiles.contains(fileName)) {
                    val fileContent = storedFileMap[fileName]?.content
                    if (fileContent != null) {
                        files[fileName] = fileContent
                        Log.d("MainActivity", "将缺失的已打开文件加载到内存: $fileName")
                    } else {
                        // 如果文件不存在，从已打开列表中移除
                        openedFiles.remove(fileName)
                        Log.d("MainActivity", "从已打开列表移除不存在的文件: $fileName")
                    }
                }
            }

            // 找出已加载但不在已打开列表中的文件
            val filesToRemove = currentLoadedFiles - currentOpenedFiles
            for (fileName in filesToRemove) {
                files.remove(fileName)
                Log.d("MainActivity", "从内存中移除不再打开的文件: $fileName")
            }

            // 如果当前文件名不在已打开列表中，需要重置当前文件名
            if (currentFileName.isNotEmpty() && !openedFiles.contains(currentFileName)) {
                if (openedFiles.isEmpty()) {
                    // 如果没有已打开的文件，显示欢迎页面
                    currentFileName = ""
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.content_frame, WelcomeFragment())
                        .commit()
                    Log.d("MainActivity", "没有已打开的文件，显示欢迎页面")
                    supportActionBar?.title = getString(R.string.welcome)
                } else {
                    // 否则打开第一个可用文件
                    currentFileName = openedFiles.first()
                    showEditorWithFile(currentFileName, getFileLanguage(currentFileName))
                    Log.d("MainActivity", "当前文件已关闭，切换到: $currentFileName")
                }
            }

            // 保存更新后的已打开文件列表
            saveOpenedFilesList()
        } catch (e: Exception) {
            Log.e("MainActivity", "同步文件列表时出错", e)
        }
    }

    /**
     * 显示新建文件对话框
     */
    private fun showNewFileDialog() {
        NewFileDialogFragment().show(supportFragmentManager, "new_file_dialog")
    }

    /**
     * 显示重命名文件对话框
     */
    private fun showRenameFileDialog(fileName: String) {
        RenameFileDialog().setUp(fileName) { oldName, newName ->
            // 检查新文件名是否已存在
            if (files.containsKey(newName)) {
                Toast.makeText(this, getString(R.string.file_exists, newName), Toast.LENGTH_SHORT)
                    .show()
            } else {
                // 获取原始内容
                val content = files[oldName] ?: ""

                // 重命名存储中的文件
                val language = getFileLanguage(oldName)
                fileStorageManager.renameFile(oldName, newName)

                // 更新内存中的文件
                files.remove(oldName)
                files[newName] = content

                // 如果当前正在编辑的是被重命名的文件，更新当前文件名
                if (currentFileName == oldName) {
                    currentFileName = newName
                    supportActionBar?.title = newName

                    // 重新加载编辑器
                    showEditorWithFile(newName, language)
                }

                // 更新文件列表
                updateFileList()
                Toast.makeText(
                    this,
                    getString(R.string.file_renamed, oldName, newName),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.show(supportFragmentManager, "RenameFileDialog")
    }

    /**
     * 显示删除文件确认对话框
     */
    private fun showDeleteFileDialog(fileName: String) {
        DeleteFileConfirmDialog().setUp(fileName) { fileToDelete ->
            deleteFileFromList(fileToDelete)
        }.show(supportFragmentManager, "DeleteFileConfirmDialog")
    }

    /**
     * 打开文件
     */
    private fun openFile(fileName: String) {
        try {
            // 确保文件在已打开列表中
            if (!openedFiles.contains(fileName)) {
                openedFiles.add(fileName)
                saveOpenedFilesList()
            }

            if (currentFileName != fileName) {
                currentFileName = fileName
                showEditorWithFile(fileName, getFileLanguage(fileName))

                // 更新RecyclerView中的选中状态 (只有在适配器已初始化的情况下)
                if (::fileListAdapter.isInitialized) {
                    fileListAdapter.setSelectedFile(fileName)
                }
            }

            // 关闭侧边栏
            if (::drawerLayout.isInitialized) {
                drawerLayout.closeDrawer(GravityCompat.START)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "打开文件时出错: $fileName", e)
            Toast.makeText(this, "打开文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to create a new file with the selected language
    fun createNewFile(language: String) {
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

            // 使用模板内容
            val initialContent = templateManager.getTemplateContent(language)

            // 清理状态：如果在welcome页面创建文件，确保文件列表是干净的
            if (currentFileName.isEmpty() && supportFragmentManager.findFragmentById(R.id.content_frame) is WelcomeFragment) {
                if (files.isNotEmpty()) {
                    Log.d("MainActivity", "检测到异常状态：在欢迎页面但文件列表不为空，正在清理...")
                    files.clear()
                }
            }

            // 强制刷新文件系统，确保获取最新状态
            fileStorageManager.refreshFileSystem()

            // 同步内存中的文件列表与文件系统
            // 获取文件系统中实际存在的文件
            val filesDir = fileStorageManager.getCodeFilesDir()
            val actualFiles = filesDir.listFiles()?.filter {
                !it.name.endsWith(".meta") && it.name.endsWith(fileExtension)
            }?.map { it.name }?.toSet() ?: emptySet()

            // 从内存中移除已不存在于文件系统的文件
            val filesToRemove = files.keys.filter {
                it.endsWith(fileExtension) && !actualFiles.contains(it)
            }

            if (filesToRemove.isNotEmpty()) {
                Log.d(
                    "MainActivity",
                    "从内存中移除不存在于文件系统的文件: ${filesToRemove.joinToString()}"
                )
                filesToRemove.forEach { files.remove(it) }
            }

            // 获取特定文件类型的所有已存在序号
            val existingIndexes = mutableListOf<Int>()

            // 检查当前内存中的文件
            Log.d("MainActivity", "检查内存中的文件，当前文件数量: ${files.size}")
            files.keys.forEach { fileName ->
                // 只查找与当前文件扩展名相同的文件
                if (fileName.endsWith(fileExtension)) {
                    // 尝试提取数字前缀
                    val numberPattern = "^(\\d+)\\..+$".toRegex()
                    val matchResult = numberPattern.find(fileName)
                    if (matchResult != null) {
                        val indexStr = matchResult.groupValues[1]
                        indexStr.toIntOrNull()?.let {
                            existingIndexes.add(it)
                            Log.d("MainActivity", "从内存中找到文件: $fileName, 序号: $it")
                        }
                    }
                }
            }

            // 检查文件系统中可能存在但未加载到内存的文件
            Log.d("MainActivity", "检查文件系统中的文件，目录: ${filesDir.absolutePath}")
            val filesList = filesDir.listFiles()
            Log.d("MainActivity", "文件系统中文件数量: ${filesList?.size ?: 0}")

            filesList?.forEach { file ->
                val fileName = file.name
                // 只查找与当前文件扩展名相同的文件，且不是元数据文件
                if (fileName.endsWith(fileExtension) && !fileName.endsWith(".meta")) {
                    // 尝试提取数字前缀
                    val numberPattern = "^(\\d+)\\..+$".toRegex()
                    val matchResult = numberPattern.find(fileName)
                    if (matchResult != null) {
                        val indexStr = matchResult.groupValues[1]
                        indexStr.toIntOrNull()?.let {
                            if (!existingIndexes.contains(it)) {
                                existingIndexes.add(it)
                                Log.d("MainActivity", "从文件系统中找到文件: $fileName, 序号: $it")
                            }
                        }
                    }
                }
            }

            // 输出当前存在的索引，用于调试
            Log.d("MainActivity", "当前存在的${fileExtension}文件索引: ${existingIndexes.sorted()}")

            // 找出最小的可用序号
            val sortedIndexes = existingIndexes.sorted()

            // 查找第一个缺失的序号
            var i = 1
            while (sortedIndexes.contains(i)) {
                i++
            }
            val newFileIndex = i

            Log.d("MainActivity", "找到的最小可用序号: $newFileIndex")

            val newFileName = "$newFileIndex$fileExtension"
            Log.d("MainActivity", "创建新文件，使用序号: $newFileIndex, 文件名: $newFileName")

            // 创建代码文件对象 - 新建文件默认为临时文件（未外部保存）
            val codeFile = CodeFile(
                name = newFileName,
                content = initialContent,
                language = language,
                isExternallySaved = false // 明确标记为临时文件
            )

            // 保存到存储
            fileStorageManager.saveFile(codeFile)

            // 添加文件到内存集合
            files[newFileName] = initialContent
            currentFileName = newFileName

            // 添加到已打开文件列表
            openedFiles.add(newFileName)
            saveOpenedFilesList()

            // 更新文件列表
            updateFileList()

            // 显示编辑器
            showEditorWithFile(newFileName, language)

            // 提示创建成功
            Toast.makeText(this, "文件 $newFileName 创建成功", Toast.LENGTH_SHORT).show()

            // 调试日志
            Log.d(
                "MainActivity",
                "创建新文件: $newFileName, 当前文件数: ${files.size}, 文件列表: ${files.keys}"
            )
        } catch (e: Exception) {
            // 处理异常
            Log.e("MainActivity", "创建文件失败: ${e.message}", e)
            Toast.makeText(this, "创建文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveCurrentFile() {
        if (currentFileName.isNotEmpty() && files.containsKey(currentFileName)) {
            val content = files[currentFileName] ?: ""
            val language = getFileLanguage(currentFileName)

            // 获取文件的当前保存状态
            val existingFile = fileStorageManager.readFile(currentFileName)
            val isExternallySaved = existingFile?.isExternallySaved ?: false

            // 创建代码文件对象，保留外部保存状态
            val codeFile = CodeFile(
                name = currentFileName,
                content = content,
                language = language,
                isExternallySaved = isExternallySaved // 保留原有的外部保存状态
            )

            // 保存到存储
            fileStorageManager.saveFile(codeFile)

            Log.d(
                "MainActivity",
                "已保存当前文件: $currentFileName, 外部保存状态: $isExternallySaved"
            )
        }
    }

    // 在Activity暂停时保存所有文件
    override fun onPause() {
        super.onPause()
        saveCurrentFile()
    }

    // 编辑器内容已更新时保存文件
    fun updateFileContent(fileName: String, content: String) {
        if (files.containsKey(fileName)) {
            // 更新内存中的内容
            files[fileName] = content

            // 获取文件的当前保存状态和URI
            val existingFile = fileStorageManager.readFile(fileName)
            val isExternallySaved = existingFile?.isExternallySaved ?: false
            val externalUri = existingFile?.externalUri ?: ""

            // 创建代码文件对象，保留外部保存状态和URI
            val codeFile = CodeFile(
                name = fileName,
                content = content,
                language = getFileLanguage(fileName),
                isExternallySaved = isExternallySaved,
                externalUri = externalUri
            )

            // 保存到内部存储
            val success = fileStorageManager.saveFile(codeFile)

            // 如果文件已保存到外部，同时更新外部文件
            if (isExternallySaved && externalUri.isNotEmpty()) {
                try {
                    fileStorageManager.updateExternalFile(codeFile)
                } catch (e: Exception) {
                    Log.e("MainActivity", "更新外部文件失败: ${e.message}", e)
                    // 更新外部文件失败不影响内部保存
                }
            }

            // 确保文件不在删除标记列表中
            try {
                val prefs = getSharedPreferences("acc_ide_prefs", Context.MODE_PRIVATE)
                val deletedFiles = prefs.getStringSet("deleted_files", emptySet()) ?: emptySet()

                // 如果文件在删除标记列表中，将其移除
                if (deletedFiles.contains(fileName)) {
                    val newDeletedFiles = HashSet(deletedFiles)
                    newDeletedFiles.remove(fileName)

                    prefs.edit()
                        .putStringSet("deleted_files", newDeletedFiles)
                        .apply()

                    Log.d("MainActivity", "已从删除标记中移除文件: $fileName")
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "清理删除标记时出错", e)
            }

            Log.d(
                "MainActivity",
                "已更新文件内容: $fileName, 保存状态: $success, 外部保存: $isExternallySaved"
            )
        }
    }

    private fun showEditorWithFile(fileName: String, language: String) {
        try {
            // Update the toolbar title
            supportActionBar?.title = fileName

            // Show the editor fragment
            val editorFragment = EditorFragment.newInstance(language, fileName)
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, editorFragment)
                .commit()

            // 确保导航抽屉图标在编辑器页面正确显示
            Handler(Looper.getMainLooper()).postDelayed({
                updateNavigationIcon()
            }, 100)

            Log.d("MainActivity", "显示编辑器: 文件=${fileName}, 语言=${language}")
        } catch (e: Exception) {
            // 处理异常
            Log.e("MainActivity", "显示编辑器失败: ${e.message}", e)
            Toast.makeText(this, "显示编辑器失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

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

    // 用于其他Fragment调用来切换语言
    fun changeLanguage(languageCode: String) {
        // 应用新语言设置
        val context = LocaleHelper.setLocale(this, languageCode)

        // 重新创建整个Activity以确保所有UI元素都使用新语言
        recreateActivityForLanguageChange()

        // 通知用户语言已更改
        Toast.makeText(this, getString(R.string.language_changed), Toast.LENGTH_SHORT).show()
    }

    // 重建Activity以应用语言变化
    private fun recreateActivityForLanguageChange() {
        // 保存当前状态
        val bundle = Bundle()

        // 保存当前Fragment类型
        val currentFragment = supportFragmentManager.findFragmentById(R.id.content_frame)
        when (currentFragment) {
            is SettingsFragment -> bundle.putString("currentFragment", "settings")
            is EditorFragment -> bundle.putString("currentFragment", "editor")
            is WelcomeFragment -> bundle.putString("currentFragment", "welcome")
        }

        // 保存其他需要的状态
        bundle.putString("currentFileName", currentFileName)
        bundle.putBoolean(
            "needsBackButton",
            currentFragment is SettingsFragment ||
                    supportActionBar?.displayOptions?.and(androidx.appcompat.app.ActionBar.DISPLAY_HOME_AS_UP) != 0
        )

        // 在下一个Activity中恢复这些状态
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("savedState", bundle)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        // 立即启动新的Activity
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    // 重写 onConfigurationChanged 以处理语言变化
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // 检查是否是夜间模式变化
        val nightModeFlags = newConfig.uiMode and Configuration.UI_MODE_NIGHT_MASK
        // 获取当前Fragment
        val currentFragment = supportFragmentManager.findFragmentById(R.id.content_frame)

        if (nightModeFlags == Configuration.UI_MODE_NIGHT_YES) {
            // 处理深色模式特定逻辑
            Log.d("MainActivity", "应用切换到深色模式")
            // 刷新当前Fragment
            if (currentFragment != null) {
                supportFragmentManager.beginTransaction()
                    .detach(currentFragment)
                    .attach(currentFragment)
                    .commit()
            }
        } else {
            // 处理浅色模式特定逻辑
            Log.d("MainActivity", "应用切换到浅色模式")
            // 刷新当前Fragment
            if (currentFragment != null) {
                supportFragmentManager.beginTransaction()
                    .detach(currentFragment)
                    .attach(currentFragment)
                    .commit()
            }
        }

        // 更新资源和界面元素
        updateResourcesAfterLanguageChange()
    }

    // 更新界面资源和文本
    private fun updateResourcesAfterLanguageChange() {
        // 更新标题栏和可能受语言影响的其他UI元素
        supportActionBar?.title = if (currentFileName.isNotEmpty()) {
            currentFileName
        } else {
            getString(R.string.app_name)
        }

        // 刷新文件列表
        updateFileList()

        // 刷新当前Fragment的UI
        val currentFragment = supportFragmentManager.findFragmentById(R.id.content_frame)
        if (currentFragment is SettingsFragment) {
            supportActionBar?.title = getString(R.string.settings)
        }

        // 刷新导航抽屉标题
        val filesTextView = findViewById<android.widget.TextView>(R.id.files_text)
        filesTextView?.text = getString(R.string.files)

        val settingsTextView = findViewById<android.widget.TextView>(R.id.settings_text)
        settingsTextView?.text = getString(R.string.settings)

        val newFileButton = findViewById<ImageButton>(R.id.new_file_button)
        newFileButton.contentDescription = getString(R.string.new_file)

        // 更新导航图标
        updateNavigationIcon()
    }

    // 专门用于IO面板关闭后重置导航栏
    private fun resetNavigationDrawerAfterIOPanel() {
        // 延迟执行，确保Fragment状态已更新
        Handler(Looper.getMainLooper()).postDelayed({
            updateNavigationIcon()
        }, 100) // 延迟100毫秒执行，确保Fragment转换完成
    }

    /**
     * 更新导航栏状态
     * 根据当前Fragment类型设置导航栏图标：
     * - 设置页面: 显示返回键
     * - 其他所有页面: 显示菜单图标
     */
    fun updateNavigationIcon() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.content_frame)

        if (currentFragment is SettingsFragment) {
            // 设置页面: 显示返回键并锁定抽屉
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            actionBarDrawerToggle.isDrawerIndicatorEnabled = false
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            actionBarDrawerToggle.setToolbarNavigationClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            // 设置标题为"设置"
            supportActionBar?.title = getString(R.string.settings)
            Log.d("MainActivity", "导航图标: 设置页面 - 返回键")
        } else if (currentFragment is EditorFragment) {
            // 编辑器页面: 显示菜单图标并确保标题是文件名
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            isDrawerToggleEnabled = true
            actionBarDrawerToggle.isDrawerIndicatorEnabled = true
            actionBarDrawerToggle.setToolbarNavigationClickListener(null)
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            actionBarDrawerToggle.syncState()

            // 确保标题是当前文件名
            if (currentFileName.isNotEmpty()) {
                supportActionBar?.title = currentFileName
            }
            Log.d("MainActivity", "导航图标: EditorFragment - 菜单图标")
        } else {
            // 其他所有页面: 显示菜单图标并解锁抽屉
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            isDrawerToggleEnabled = true
            actionBarDrawerToggle.isDrawerIndicatorEnabled = true
            actionBarDrawerToggle.setToolbarNavigationClickListener(null)
            supportActionBar?.setDisplayHomeAsUpEnabled(false)
            actionBarDrawerToggle.syncState()

            // 如果是欢迎页面，设置标题为"ACC IDE"
            if (currentFragment is WelcomeFragment) {
                supportActionBar?.title = getString(R.string.welcome)
            }

            Log.d(
                "MainActivity",
                "导航图标: ${currentFragment?.javaClass?.simpleName ?: "未知"} - 菜单图标"
            )
        }
    }

    /**
     * 从磁盘刷新文件列表
     * 用于手动刷新文件列表，特别是在文件删除后
     */
    private fun refreshFileListFromDisk() {
        try {
            Log.d("MainActivity", "开始从磁盘刷新文件列表...")

            // 强制刷新文件系统
            fileStorageManager.refreshFileSystem()

            // 重新加载所有文件
            loadFilesFromStorage()

            // 更新UI
            updateFileList()

            // 显示刷新结果
            Toast.makeText(this, "文件列表已刷新，发现 ${files.size} 个文件", Toast.LENGTH_SHORT)
                .show()
            Log.d(
                "MainActivity",
                "文件列表刷新完成，当前文件数: ${files.size}, 文件列表: ${files.keys}"
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "刷新文件列表失败", e)
            Toast.makeText(this, "刷新文件列表失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    /**
     * 显示清除所有文件的确认对话框
     */
    private fun showClearAllFilesDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.clear_all_files_title))
            .setMessage(getString(R.string.clear_all_files_message))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                // 删除所有文件
                val success = fileStorageManager.deleteAllFiles()
                if (success) {
                    // 清空内存中的文件列表
                    files.clear()

                    // 显示欢迎界面
                    currentFileName = ""
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.content_frame, WelcomeFragment())
                        .commit()

                    // 更新标题
                    supportActionBar?.title = getString(R.string.app_name)

                    // 更新文件列表
                    updateFileList()

                    Toast.makeText(
                        this,
                        getString(R.string.clear_all_files_success),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.clear_all_files_failure),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    /**
     * 显示存储路径信息
     */
    private fun showStoragePath() {
        val filesPath = fileStorageManager.getCodeFilesDir().absolutePath

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.storage_path_title))
            .setMessage(getString(R.string.storage_path_message, filesPath))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    /**
     * 从文件列表中删除文件
     */
    private fun deleteFileFromList(fileName: String) {
        try {
            Log.d("MainActivity", "开始删除文件: $fileName")

            // 从内存中删除文件
            files.remove(fileName)
            Log.d("MainActivity", "已从内存中移除文件: $fileName, 当前文件数: ${files.size}")

            // 更新RecyclerView
            updateFileList()

            // 从已打开文件列表中移除
            openedFiles.remove(fileName)
            saveOpenedFilesList()

            // 如果当前文件是被删除的文件，则显示其他文件或欢迎界面
            if (currentFileName == fileName) {
                if (openedFiles.isEmpty()) {
                    // 没有其他文件，显示欢迎界面
                    showWelcomeFragment()
                    currentFileName = ""
                    Log.d("MainActivity", "删除最后一个文件，显示欢迎页面")
                } else {
                    // 显示最后一个打开的文件（最新的）
                    currentFileName = openedFiles.last()
                    showEditorWithFile(currentFileName, getFileLanguage(currentFileName))
                    Log.d("MainActivity", "删除当前文件，切换到: $currentFileName")
                }
            }

            // 异步从文件存储中删除文件
            fileStorageManager.deleteFileAsync(fileName) { success ->
                if (success) {
                    Log.d("MainActivity", "文件异步删除成功: $fileName")

                    // 检查文件是否真的被删除了
                    val fileStillExists =
                        File(fileStorageManager.getCodeFilesDir(), fileName).exists()
                    if (fileStillExists) {
                        Log.w("MainActivity", "文件删除失败，但UI已更新: $fileName")
                    } else {
                        Log.d("MainActivity", "文件确认已被删除: $fileName")

                        // 再次确保从内存中移除
                        if (files.containsKey(fileName)) {
                            Log.w("MainActivity", "文件已从文件系统删除，但内存中仍存在，现在移除")
                            files.remove(fileName)
                            updateFileList()
                        }
                    }
                } else {
                    Log.e("MainActivity", "文件异步删除失败: $fileName")
                    Toast.makeText(this@MainActivity, "删除文件失败", Toast.LENGTH_SHORT).show()
                }
            }

            Toast.makeText(this, getString(R.string.file_deleted, fileName), Toast.LENGTH_SHORT)
                .show()

            // 日志输出当前状态
            Log.d(
                "MainActivity",
                "删除文件后状态: 文件数量=${files.size}, 当前文件名=$currentFileName"
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "删除文件过程中发生异常", e)
            Toast.makeText(this, "删除文件过程中发生错误: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 保存指定文件
     */
    private fun saveSpecificFile(fileName: String) {
        if (files.containsKey(fileName)) {
            val content = files[fileName] ?: ""
            val language = getFileLanguage(fileName)

            // 创建代码文件对象
            val codeFile = CodeFile(
                name = fileName,
                content = content,
                language = language
            )

            // 保存到存储
            fileStorageManager.saveFile(codeFile)

            Log.d("MainActivity", "已保存文件: $fileName")
        }
    }

    /**
     * 关闭文件（从显示列表中移除）并根据保存状态进行处理
     *
     * 如果文件只保存在临时目录中，则删除该文件
     * 如果文件已保存在外部，则保存文件内容到外部位置并关闭，同时删除临时副本
     */
    private fun closeFile(fileName: String) {
        try {
            Log.d("MainActivity", "正在关闭文件: $fileName")

            // 验证文件是否存在
            if (!files.containsKey(fileName)) {
                Log.e("MainActivity", "尝试关闭不存在的文件: $fileName")
                Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show()
                return
            }

            // 获取文件保存状态
            val codeFile = fileStorageManager.readFile(fileName)
            if (codeFile == null) {
                Log.e("MainActivity", "无法读取文件信息: $fileName")
                // 即使读取失败，也尝试关闭文件
            }

            val isExternallySaved = codeFile?.isExternallySaved ?: false
            val externalUri = codeFile?.externalUri ?: ""
            Log.d(
                "MainActivity",
                "文件 $fileName 外部保存状态: $isExternallySaved, URI: $externalUri"
            )

            // 如果文件已保存到外部，先保存最新内容到外部位置
            if (isExternallySaved && externalUri.isNotEmpty()) {
                Log.d("MainActivity", "文件 $fileName 已保存到外部，保存最新内容")
                val content = files[fileName] ?: ""
                val language = getFileLanguage(fileName)

                // 创建代码文件对象并保存
                val updatedCodeFile = CodeFile(
                    name = fileName,
                    content = content,
                    language = language,
                    isExternallySaved = true,
                    externalUri = externalUri
                )

                try {
                    // 更新外部文件
                    val saveResult = fileStorageManager.updateExternalFile(updatedCodeFile)
                    Log.d("MainActivity", "保存外部文件结果: $saveResult")
                } catch (e: Exception) {
                    Log.e("MainActivity", "更新外部文件失败: ${e.message}", e)
                    // 更新失败不影响关闭操作
                }
            }

            // 从已打开文件列表中移除
            openedFiles.remove(fileName)

            // 保存已打开文件列表
            saveOpenedFilesList()

            // 从内存中移除
            files.remove(fileName)
            Log.d("MainActivity", "已从内存中移除文件: $fileName, 当前文件数: ${files.size}")

            // 更新文件列表UI
            updateFileList()

            // 如果关闭的是当前打开的文件，则切换到其他文件或欢迎界面
            if (currentFileName == fileName) {
                if (openedFiles.isEmpty()) {
                    // 没有其他文件，显示欢迎界面
                    showWelcomeFragment()
                    currentFileName = ""
                    Log.d("MainActivity", "关闭最后一个文件，显示欢迎页面")
                } else {
                    // 显示最后一个打开的文件（最新的）
                    currentFileName = openedFiles.last()
                    showEditorWithFile(currentFileName, getFileLanguage(currentFileName))
                    Log.d("MainActivity", "关闭当前文件，切换到: $currentFileName")
                }
            }

            // 无论是否是外部文件，都需要删除临时副本
            // 使用主线程Handler延迟执行文件删除，避免连续快速关闭多个文件时的冲突
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                // 异步删除文件，在UI更新后进行
                fileStorageManager.deleteFileAsync(fileName) { success ->
                    // 在文件删除完成后处理
                    if (success) {
                        Log.d("MainActivity", "临时文件异步删除成功: $fileName")
                    } else {
                        Log.e("MainActivity", "临时文件异步删除失败: $fileName")
                    }
                }
            }, 200) // 添加200毫秒延迟，避免连续关闭文件时的冲突

            Toast.makeText(this, getString(R.string.file_closed, fileName), Toast.LENGTH_SHORT)
                .show()
        } catch (e: Exception) {
            Log.e("MainActivity", "关闭文件时出错: $fileName", e)
            Toast.makeText(this, "关闭文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 处理文件保存
     * 如果文件已保存到外部，则直接更新外部文件
     * 否则打开文件选择器让用户选择保存位置
     */
    private fun handleFileSave(fileName: String) {
        try {
            // 确保文件存在
            if (!files.containsKey(fileName)) {
                Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show()
                return
            }

            // 获取文件信息
            val codeFile = fileStorageManager.readFile(fileName)
            if (codeFile == null) {
                Toast.makeText(this, "无法读取文件信息", Toast.LENGTH_SHORT).show()
                return
            }

            // 检查文件是否已保存到外部
            if (codeFile.isExternallySaved && codeFile.externalUri.isNotEmpty()) {
                // 已保存到外部，直接更新外部文件
                val content = files[fileName] ?: ""
                val language = getFileLanguage(fileName)

                // 创建更新后的代码文件对象
                val updatedCodeFile = codeFile.copy(
                    content = content,
                    language = language,
                    lastModified = System.currentTimeMillis()
                )

                // 更新外部文件
                val success = fileStorageManager.updateExternalFile(updatedCodeFile)

                if (success) {
                    Toast.makeText(this, "文件已保存", Toast.LENGTH_SHORT).show()
                    Log.d(
                        "MainActivity",
                        "文件已直接更新到外部: $fileName, URI: ${codeFile.externalUri}"
                    )
                } else {
                    Toast.makeText(this, "保存文件失败，尝试另存为", Toast.LENGTH_SHORT).show()
                    // 如果直接更新失败，尝试另存为
                    saveFileToExternal(fileName)
                }
            } else {
                // 未保存到外部或URI为空，打开文件选择器
                saveFileToExternal(fileName)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "处理文件保存失败: ${e.message}", e)
            Toast.makeText(this, "保存文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 保存文件到用户选择的位置
     */
    private fun saveFileToExternal(fileName: String) {
        try {
            // 根据文件扩展名确定MIME类型
            val mimeType = when {
                fileName.endsWith(".cpp") -> "text/x-c++src"
                fileName.endsWith(".py") -> "text/x-python"
                fileName.endsWith(".java") -> "text/x-java"
                else -> "text/plain"
            }

            // 创建ACTION_CREATE_DOCUMENT意图以让用户选择保存位置
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = mimeType
                putExtra(Intent.EXTRA_TITLE, fileName)
            }

            // 保存当前要保存的文件名，以便在onActivityResult中使用
            fileToSave = fileName

            Log.d("MainActivity", "打开文件选择器保存文件: $fileName")

            // 启动文件选择器
            saveFileLauncher.launch(intent)
        } catch (e: Exception) {
            Log.e("MainActivity", "保存文件失败: ${e.message}", e)
            Toast.makeText(this, "保存文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 处理文件保存逻辑
     */
    private fun handleSaveFile(fileName: String, uri: Uri) {
        try {
            // 获取文件内容
            val content = files[fileName] ?: ""
            val language = getFileLanguage(fileName)

            // 创建代码文件对象，标记为已外部保存
            val updatedCodeFile = CodeFile(
                name = fileName,
                content = content,
                language = language,
                isExternallySaved = true,
                externalUri = uri.toString()
            )

            // 保存到用户选择的位置
            if (fileStorageManager.saveFileToUri(updatedCodeFile, uri)) {
                Log.d("MainActivity", "文件已保存到外部: ${uri.toString()}")

                // 更新内存中的文件状态为已外部保存
                files[fileName] = content

                // 更新文件列表显示（去掉星号）
                val externalSaveStatus = mutableMapOf<String, Boolean>()
                openedFiles.forEach { filename ->
                    externalSaveStatus[filename] = if (filename == fileName) true else
                        fileStorageManager.readFile(filename)?.isExternallySaved ?: false
                }

                // 更新文件列表UI，显示没有星号的文件名
                if (::fileListAdapter.isInitialized) {
                    fileListAdapter.updateFileList(
                        openedFiles.toList(),
                        currentFileName,
                        externalSaveStatus
                    )
                }

                Toast.makeText(this, "文件已保存到外部", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "保存文件失败", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "保存文件到URI失败: ${e.message}", e)
            Toast.makeText(this, "保存文件失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 更新编辑器的字体大小
     */
    fun updateEditorFontSize(size: Float) {
        // 获取当前的EditorFragment
        val editorFragment =
            supportFragmentManager.fragments.firstOrNull { it is EditorFragment } as? EditorFragment
        editorFragment?.updateFontSize(size)
    }

    /**
     * 更新编辑器的光标宽度
     */
    fun updateEditorCursorWidth(width: Float) {
        // 获取当前的EditorFragment
        val editorFragment =
            supportFragmentManager.fragments.firstOrNull { it is EditorFragment } as? EditorFragment
        editorFragment?.updateCursorWidth(width)
    }

    /**
     * 更新自动补全组件的状态
     * @param enabled 是否启用自动补全
     */
    fun updateAutoCompletionState(enabled: Boolean) {
        try {
            // 获取所有EditorFragment并更新自动补全状态
            supportFragmentManager.fragments.forEach { fragment ->
                if (fragment is EditorFragment) {
                    fragment.setAutoCompletionEnabled(enabled)
                }
            }
            Log.d("MainActivity", "自动补全状态已更新: $enabled")
        } catch (e: Exception) {
            Log.e("MainActivity", "更新自动补全状态失败: ${e.message}")
        }
    }

    /**
     * 刷新编辑器的语法高亮
     */
    fun refreshEditorSyntaxHighlighting() {
        // 获取当前的EditorFragment
        val editorFragment =
            supportFragmentManager.fragments.firstOrNull { it is EditorFragment } as? EditorFragment
        editorFragment?.let { fragment ->
            // 调用新添加的方法刷新主题和语法高亮
            fragment.refreshEditorTheme()

            // 记录日志
            Log.d("MainActivity", "已刷新编辑器的语法高亮和主题")
        }
    }

    /**
     * 加载文件列表，从存储中读取所有可用的代码文件
     */
    private fun loadFileList() {
        try {
            // 清空当前文件列表
            files.clear()

            // 获取所有代码文件
            val codeFiles = fileStorageManager.getAllCodeFiles()

            // 将文件添加到内存中
            for (codeFile in codeFiles) {
                files[codeFile.name] = codeFile.content
            }

            // 更新UI上的文件列表
            updateFileList()

            Log.d("MainActivity", "已加载${files.size}个文件")
        } catch (e: Exception) {
            Log.e("MainActivity", "加载文件列表失败: ${e.message}")
        }
    }

    /**
     * 显示欢迎页面
     */
    private fun showWelcomeFragment() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, WelcomeFragment())
            .commit()
        supportActionBar?.title = getString(R.string.welcome)
        Log.d("MainActivity", "显示欢迎页面")
    }

    /**
     * 打开文件选择器
     */
    private fun openFileSelector() {
        try {
            // 打开文件选择器，指定支持的文件类型
            openFileLauncher.launch(
                arrayOf(
                    "text/plain",
                    "text/x-c++src", // C++
                    "text/x-java", // Java
                    "text/x-python", // Python
                    "application/octet-stream", // 通用二进制文件，有些系统可能用这个类型表示代码文件
                    "*/*" // 所有文件类型作为备选
                )
            )
        } catch (e: Exception) {
            Log.e("MainActivity", "打开文件选择器失败: ${e.message}")
            Toast.makeText(this, "打开文件选择器失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 处理用户从文件选择器中选择的文件
     */
    private fun handleOpenedFile(uri: Uri) {
        try {
            Log.d("MainActivity", "用户选择了文件: $uri")

            // 获取文件名
            var fileName = getFileName(uri)

            // 如果获取不到文件名，使用默认名称
            if (fileName.isBlank()) {
                fileName = "Imported_${System.currentTimeMillis()}.txt"
            }

            // 读取文件内容
            val inputStream = contentResolver.openInputStream(uri)
            val content = inputStream?.use { stream ->
                stream.bufferedReader().use { it.readText() }
            } ?: ""

            // 确保文件名唯一
            var finalFileName = fileName
            var counter = 1
            while (files.containsKey(finalFileName)) {
                // 如果文件名已存在，添加数字后缀
                val baseName = fileName.substringBeforeLast(".")
                val extension =
                    if (fileName.contains(".")) ".${fileName.substringAfterLast(".")}" else ""
                finalFileName = "${baseName}_${counter}${extension}"
                counter++
            }

            // 尝试获取持久性权限，以便后续更新文件
            try {
                val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                contentResolver.takePersistableUriPermission(uri, takeFlags)
                Log.d("MainActivity", "已获取文件的持久性权限: $uri")
            } catch (e: Exception) {
                Log.w("MainActivity", "无法获取文件的持久性权限: ${e.message}")
                // 继续处理，即使没有持久性权限也可以打开文件
            }

            // 创建代码文件对象 - 标记为外部保存的文件
            val codeFile = CodeFile(
                name = finalFileName,
                content = content,
                language = getFileLanguage(finalFileName),
                isExternallySaved = true,
                externalUri = uri.toString()
            )

            // 保存到存储
            fileStorageManager.saveFile(codeFile)

            // 添加到内存中
            files[finalFileName] = content

            // 添加到已打开文件列表
            openedFiles.add(finalFileName)
            saveOpenedFilesList()

            // 更新文件列表
            updateFileList()

            // 打开编辑器显示文件
            currentFileName = finalFileName
            showEditorWithFile(finalFileName, getFileLanguage(finalFileName))

            // 提示导入成功
            Toast.makeText(this, "已导入文件: $finalFileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("MainActivity", "处理选择的文件失败: ${e.message}")
            Toast.makeText(this, "无法打开选择的文件: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 获取URI对应的文件名
     */
    private fun getFileName(uri: Uri): String {
        // 尝试从内容提供者获取文件名
        val cursor = contentResolver.query(uri, null, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex =
                    it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    return it.getString(displayNameIndex)
                }
            }
        }

        // 如果无法从内容提供者获取，尝试从URI路径中获取
        val path = uri.path
        if (path != null) {
            val cut = path.lastIndexOf('/')
            if (cut != -1) {
                return path.substring(cut + 1)
            }
        }

        // 如果都无法获取，返回空字符串
        return ""
    }

    /**
     * 设置UI组件
     */
    private fun setupUI() {
        try {
            // 初始化空文件列表提示视图
            emptyFileListMessage = findViewById(R.id.empty_files_view)

            // 设置文件列表RecyclerView
            fileListRecyclerView = findViewById(R.id.file_list_recyclerview)
            fileListRecyclerView.layoutManager = LinearLayoutManager(this)

            // 初始化适配器
            fileListAdapter = FileListAdapter(
                onFileClickListener = { fileName ->
                    openFile(fileName)
                },
                onRenameClickListener = { fileName ->
                    showRenameFileDialog(fileName)
                },
                onDeleteClickListener = { fileName ->
                    showDeleteFileDialog(fileName)
                },
                onCloseClickListener = { fileName ->
                    closeFile(fileName)
                },
                onSaveClickListener = { fileName ->
                    handleFileSave(fileName)
                },
                onSaveAsClickListener = { fileName ->
                    saveFileToExternal(fileName)
                }
            )
            fileListRecyclerView.adapter = fileListAdapter

            // 设置打开文件按钮
            val openFileButton = findViewById<ImageButton>(R.id.open_file_button)
            openFileButton.setOnClickListener {
                openFileSelector()
            }

            // 设置新建文件按钮
            val newFileButton = findViewById<ImageButton>(R.id.new_file_button)
            newFileButton.setOnClickListener {
                showNewFileDialog()
            }

            Log.d("MainActivity", "UI组件初始化成功")
        } catch (e: Exception) {
            Log.e("MainActivity", "设置UI组件时出错", e)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 权限被授予，初始化文件系统
                initializeFileSystem()

                // 权限获取后初始化模板系统
                initializeTemplateSystem()
            } else {
                // 权限被拒绝，提示用户文件将只保存在内存中
                Toast.makeText(this, R.string.storage_permission_denied, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * 处理从Fragment返回的请求
     * 为避免栈溢出错误，由Fragment调用
     */
    fun onFragmentBackPressed() {
        // 查找当前显示的Fragment
        val currentFragment = supportFragmentManager.findFragmentById(R.id.content_frame)

        // 如果当前显示的是设置Fragment，返回到前一个Fragment或欢迎页面
        if (currentFragment is SettingsFragment) {
            // 尝试显示编辑器页面（如果有）
            if (files.isNotEmpty()) {
                val firstFileName = files.keys.first()
                openFile(firstFileName)
            } else {
                // 如果没有文件，显示欢迎页面
                showWelcomeFragment()
            }

            // 更新UI
            invalidateOptionsMenu()
            resetNavigationDrawer()
        } else {
            // 其他类型的Fragment，调用系统的返回行为
            finishAfterTransition()
        }
    }
} 