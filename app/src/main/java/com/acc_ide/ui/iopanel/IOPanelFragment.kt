package com.acc_ide.ui.iopanel

import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.acc_ide.R
import com.acc_ide.ui.main.MainActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.zip.ZipInputStream

class IOPanelFragment : Fragment() {
    private lateinit var runCodeButton: MaterialButton
    private lateinit var runStatus: TextView
    private lateinit var inputText: TextInputEditText
    private lateinit var actualOutputText: TextInputEditText
    private lateinit var expectedOutputText: TextInputEditText
    
    private var fileName: String = ""
    private var language: String = ""
    private var isGitHubRunning = false
    private var pollingJob: Job? = null

    // 用于存储IO实例的缓存
    companion object {
        private const val ARG_FILENAME = "filename"
        private const val ARG_LANGUAGE = "language"
        
        // 缓存每个文件的IO实例
        private val ioCache = mutableMapOf<String, IOInstance>()

        // GitHub相关常量
        private const val GITHUB_API_BASE = "https://api.github.com"
        private const val PREF_GITHUB_REPO_URL = "github_repo_url"
        private const val PREF_GITHUB_PAT = "github_pat"
        
        // 语言映射
        private val LANGUAGE_MAP = mapOf(
            "cpp" to "cpp",
            "python" to "py",
            "java" to "java"
        )

        @JvmStatic
        fun newInstance(fileName: String, language: String) =
            IOPanelFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FILENAME, fileName)
                    putString(ARG_LANGUAGE, language)
                }
            }
    }
    
    // IO实例数据类
    data class IOInstance(
        var input: String = "",
        var actualOutput: String = "",
        var expectedOutput: String = "",
        var status: String = "",
        var executionTime: Int = 0
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_io_panel, container, false)
        
        // 获取参数
        arguments?.let {
            fileName = it.getString(ARG_FILENAME, "")
            language = it.getString(ARG_LANGUAGE, "")
        }
        
        // 初始化视图
        runCodeButton = view.findViewById(R.id.run_code_button)
        runStatus = view.findViewById(R.id.run_status)
        inputText = view.findViewById(R.id.input_text)
        actualOutputText = view.findViewById(R.id.actual_output_text)
        expectedOutputText = view.findViewById(R.id.expected_output_text)
        
        // 设置初始状态
        runStatus.text = ""
        
        // 从缓存加载数据
        loadFromCache()
        
        // 设置事件监听
        setupListeners()
        
        return view
    }
    
    private fun setupListeners() {
        // 运行代码按钮
        runCodeButton.setOnClickListener {
            // 从GitHub运行代码
            runCodeOnGitHub()
        }
    }
    
    private fun loadFromCache() {
        // 从缓存加载数据
        val cachedInstance = ioCache[fileName]
        if (cachedInstance != null) {
            inputText.setText(cachedInstance.input)
            actualOutputText.setText(cachedInstance.actualOutput)
            expectedOutputText.setText(cachedInstance.expectedOutput)
            if (cachedInstance.status.isNotEmpty()) {
                // 显示状态和执行时间（如果有）
                if (cachedInstance.executionTime > 0) {
                    // 设置状态文本
                    runStatus.text = getString(R.string.execution_status_with_time, cachedInstance.status, cachedInstance.executionTime)
                    
                    // 为状态部分设置颜色
                    setStatusColor(cachedInstance.status)
                    
                    // 为执行时间部分设置灰色
                    val spannable = SpannableString(runStatus.text)
                    spannable.setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(requireContext(), android.R.color.darker_gray)),
                        cachedInstance.status.length, // 从状态后开始
                        runStatus.text.length, // 到文本结束
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                    runStatus.text = spannable
                } else {
                runStatus.text = cachedInstance.status
                setStatusColor(cachedInstance.status)
                }
            }
        }
    }
    
    private fun saveToCache(executionTime: Int = 0) {
        // 保存数据到缓存
        val statusText = runStatus.text.toString()
        // 提取状态码（前2-3个字符，如AC, WA, TLE等）
        val status = if (statusText.length >= 2) {
            when {
                statusText.startsWith("AC") -> "AC"
                statusText.startsWith("WA") -> "WA"
                statusText.startsWith("CE") -> "CE"
                statusText.startsWith("TLE") -> "TLE"
                statusText.startsWith("MLE") -> "MLE"
                statusText.startsWith("RE") -> "RE"
                statusText.startsWith("RS") -> "RS"
                else -> statusText
            }
        } else {
            statusText
        }
        
        ioCache[fileName] = IOInstance(
            input = inputText.text.toString(),
            actualOutput = actualOutputText.text.toString(),
            expectedOutput = expectedOutputText.text.toString(),
            status = status, // 只保存状态码，不保存执行时间
            executionTime = executionTime
        )
    }
    
    private fun runCodeOnGitHub() {
        // 检查是否已经有一个运行中的工作流
        if (isGitHubRunning) {
            Toast.makeText(context, getString(R.string.already_running), Toast.LENGTH_SHORT).show()
            return
        }
        
        // 获取GitHub设置
        val prefs = PreferenceManager.getDefaultSharedPreferences(requireContext())
        val repoUrl = prefs.getString(PREF_GITHUB_REPO_URL, "") ?: ""
        val pat = prefs.getString(PREF_GITHUB_PAT, "") ?: ""
        
        if (repoUrl.isEmpty() || pat.isEmpty()) {
            Toast.makeText(context, getString(R.string.no_github_settings), Toast.LENGTH_LONG).show()
            return
        }
        
        // 解析仓库所有者和名称
        val regex = "https://github.com/([^/]+)/([^/]+)".toRegex()
        val match = regex.find(repoUrl)
        
        if (match == null || match.groupValues.size < 3) {
            Toast.makeText(context, getString(R.string.invalid_repo_url), Toast.LENGTH_SHORT).show()
            return
        }
        
        val owner = match.groupValues[1]
        val repo = match.groupValues[2].removeSuffix(".git")
        
        // 获取输入、代码和预期输出
        val input = inputText.text.toString()
        val expected = expectedOutputText.text.toString()
        
        // 获取代码
        val mainActivity = activity as MainActivity
        val code = mainActivity.files[fileName] ?: ""
        
        if (code.isBlank()) {
            Toast.makeText(context, getString(R.string.code_empty), Toast.LENGTH_SHORT).show()
            return
        }
        
        // 映射语言格式
        val mappedLanguage = LANGUAGE_MAP[language] ?: "cpp"
        
        // 禁用运行按钮，显示进度
        runCodeButton.isEnabled = false
        runStatus.text = getString(R.string.code_running)
        runStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.darker_gray))
        actualOutputText.setText(getString(R.string.code_running_on_github))
        isGitHubRunning = true
        
        // 使用协程在后台执行GitHub API调用
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 调用GitHub API触发工作流
                val runId = triggerGitHubWorkflow(owner, repo, pat, code, input, expected, mappedLanguage)
                
                if (runId > 0) {
                    // 开始轮询工作流状态
                    pollWorkflowStatus(owner, repo, pat, runId)
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, getString(R.string.workflow_trigger_failed), Toast.LENGTH_SHORT).show()
                        resetRunState()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, getString(R.string.workflow_error, e.message), Toast.LENGTH_LONG).show()
                    resetRunState()
                }
            }
        }
    }
    
    private suspend fun triggerGitHubWorkflow(
        owner: String,
        repo: String,
        pat: String,
        code: String,
        input: String,
        expected: String,
        language: String
    ): Long = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        // 准备请求体
        val jsonBody = JSONObject().apply {
            put("ref", "main") // 假设工作流在main分支
            
            // 输入参数
            val inputs = JSONObject().apply {
                put("source_code", code)
                put("std_input", input)
                put("expected_output", expected)
                put("language", language)
            }
            
            put("inputs", inputs)
        }
        
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toString().toRequestBody(mediaType)
        
        // 构建API请求 - 修正工作流文件路径
        val workflowFileName = "code-execution.yml"
        val request = Request.Builder()
            .url("$GITHUB_API_BASE/repos/$owner/$repo/actions/workflows/$workflowFileName/dispatches")
            .header("Authorization", "token $pat")
            .header("Accept", "application/vnd.github.v3+json")
            .post(requestBody)
            .build()
        
        // 执行请求
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                throw IOException("GitHub API错误: $errorBody")
            }
            
            // 获取最新的工作流运行ID
            // 由于GitHub API在触发工作流时不直接返回run ID，
            // 我们需要额外查询最近的工作流运行
            delay(2000) // 给GitHub一点时间（2s）来创建工作流运行
            getLatestWorkflowRunId(owner, repo, pat)
        }
    }
    
    private suspend fun getLatestWorkflowRunId(
        owner: String,
        repo: String,
        pat: String
    ): Long = withContext(Dispatchers.IO) {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        
        val request = Request.Builder()
            .url("$GITHUB_API_BASE/repos/$owner/$repo/actions/runs?per_page=1")
            .header("Authorization", "token $pat")
            .header("Accept", "application/vnd.github.v3+json")
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Unknown error"
                throw IOException("获取工作流运行ID失败: $errorBody")
            }
            
            val responseBody = response.body?.string() ?: "{}"
            val jsonResponse = JSONObject(responseBody)
            val workflowRuns = jsonResponse.getJSONArray("workflow_runs")
            
            if (workflowRuns.length() > 0) {
                val latestRun = workflowRuns.getJSONObject(0)
                latestRun.getLong("id")
            } else {
                -1L
            }
        }
    }
    
    private fun pollWorkflowStatus(owner: String, repo: String, pat: String, runId: Long) {
        // 取消任何正在进行的轮询
        pollingJob?.cancel()
        
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            var isCompleted = false
            var attempts = 0
            
            while (!isCompleted && attempts < 60) { // 最多尝试60次
                delay(2000) // 每2秒钟检查一次
                attempts++
                
                try {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(10, TimeUnit.SECONDS)
                        .readTimeout(10, TimeUnit.SECONDS)
                        .build()
                    
                    val request = Request.Builder()
                        .url("$GITHUB_API_BASE/repos/$owner/$repo/actions/runs/$runId")
                        .header("Authorization", "token $pat")
                        .header("Accept", "application/vnd.github.v3+json")
                        .build()
                    
                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val responseBody = response.body?.string() ?: "{}"
                            val jsonResponse = JSONObject(responseBody)
                            val status = jsonResponse.getString("status")
                            
                            withContext(Dispatchers.Main) {
                                val progressText = getString(R.string.code_running_time, attempts * 2)
                                actualOutputText.setText(progressText)
                            }
                            
                            if (status == "completed") {
                                isCompleted = true
                                // 获取工作流结果
                                downloadAndProcessArtifacts(owner, repo, pat, runId)
                            }
                        } else {
                            val errorBody = response.body?.string() ?: "Unknown error"
                            throw IOException("检查工作流状态失败: $errorBody")
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, getString(R.string.workflow_polling_error, e.message), Toast.LENGTH_SHORT).show()
                    }
                    break
                }
            }
            
            if (!isCompleted) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, getString(R.string.workflow_timeout), Toast.LENGTH_SHORT).show()
                    resetRunState()
                }
            }
        }
    }
    
    private suspend fun downloadAndProcessArtifacts(
        owner: String,
        repo: String,
        pat: String,
        runId: Long
    ) = withContext(Dispatchers.IO) {
        try {
            // 1. 获取Artifacts列表
            val client = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            
            val listRequest = Request.Builder()
                .url("$GITHUB_API_BASE/repos/$owner/$repo/actions/runs/$runId/artifacts")
                .header("Authorization", "token $pat")
                .header("Accept", "application/vnd.github.v3+json")
                .build()
            
            client.newCall(listRequest).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Unknown error"
                    throw IOException(getString(R.string.result_processing_error, "获取Artifacts失败: $errorBody"))
                }
                
                val responseBody = response.body?.string() ?: "{}"
                val jsonResponse = JSONObject(responseBody)
                val artifacts = jsonResponse.getJSONArray("artifacts")
                
                if (artifacts.length() == 0) {
                    throw IOException(getString(R.string.artifact_not_found) + " - 工作流执行可能未生成结果")
                }
                
                // 找到judge-result artifact
                var artifactId = -1L
                var artifactName = ""
                
                for (i in 0 until artifacts.length()) {
                    val artifact = artifacts.getJSONObject(i)
                    val name = artifact.getString("name")
                    if (name == "judge-result") {
                        artifactId = artifact.getLong("id")
                        artifactName = name
                        break
                    }
                }
                
                if (artifactId == -1L) {
                    // 列出所有可用的artifacts名称，方便调试
                    val availableArtifacts = (0 until artifacts.length())
                        .map { artifacts.getJSONObject(it).getString("name") }
                        .joinToString(", ")
                    
                    throw IOException(getString(R.string.artifact_not_found) + 
                        " - 可用的artifacts: $availableArtifacts")
                }
                
                // 2. 下载Artifact
                val downloadRequest = Request.Builder()
                    .url("$GITHUB_API_BASE/repos/$owner/$repo/actions/artifacts/$artifactId/zip")
                    .header("Authorization", "token $pat")
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()
                
                client.newCall(downloadRequest).execute().use { downloadResponse ->
                    if (!downloadResponse.isSuccessful) {
                        val errorBody = downloadResponse.body?.string() ?: "Unknown error"
                        throw IOException(getString(R.string.result_processing_error, "下载失败 (${downloadResponse.code}): $errorBody"))
                    }
                    
                    // 将ZIP文件保存到临时目录
                    val zipFile = File(requireContext().cacheDir, "$artifactName.zip")
                    val inputStream = downloadResponse.body?.byteStream()
                        ?: throw IOException(getString(R.string.result_processing_error, "下载响应为空"))
                    
                    FileOutputStream(zipFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                    
                    // 3. 解压并处理结果
                    val resultsDir = File(requireContext().cacheDir, "judge-results")
                    if (!resultsDir.exists()) {
                        resultsDir.mkdirs()
                    } else {
                        resultsDir.listFiles()?.forEach { it.delete() }
                    }
                    
                    unzipFile(zipFile, resultsDir)
                    
                    // 列出解压后的所有文件，以便调试
                    val extractedFiles = resultsDir.listFiles()?.map { it.name } ?: listOf()
                    Log.d("IOPanelFragment", "解压的文件: ${extractedFiles.joinToString()}")
                    
                    // 4. 读取结果
                    val resultFile = File(resultsDir, "result.json")
                    val actualOutputFile = File(resultsDir, "actual.txt")
                    
                    if (resultFile.exists()) {
                        val resultJson = JSONObject(resultFile.readText())
                        val status = resultJson.getString("status")
                        val executionTime = resultJson.optInt("execution_time", 0)
                        
                        var actualOutput = getString(R.string.no_output)
                        if (actualOutputFile.exists()) {
                            actualOutput = actualOutputFile.readText()
                        } else {
                            // 如果actual.txt不存在，但有编译错误日志，则显示编译错误
                            val compileErrorFile = File(resultsDir, "compile.log")
                            if (compileErrorFile.exists() && status == "CE") {
                                actualOutput = compileErrorFile.readText()
                            }
                        }
                        
                        // 更新UI
                        withContext(Dispatchers.Main) {
                            // 显示状态和执行时间
                            if (executionTime > 0 && status != "CE") {
                                // 设置状态文本
                                runStatus.text = getString(R.string.execution_status_with_time, status, executionTime)
                                
                                // 为状态部分设置颜色
                                setStatusColor(status)
                                
                                // 为执行时间部分设置灰色
                                val spannable = SpannableString(runStatus.text)
                                spannable.setSpan(
                                    ForegroundColorSpan(ContextCompat.getColor(requireContext(), android.R.color.darker_gray)),
                                    status.length, // 从状态后开始
                                    runStatus.text.length, // 到文本结束
                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                )
                                runStatus.text = spannable
                            } else {
                                runStatus.text = status
                                setStatusColor(status)
                            }
                            actualOutputText.setText(actualOutput)
                            // 保存执行时间到缓存
                            saveToCache(executionTime)
                            resetRunState()
                        }
                    } else {
                        throw IOException(getString(R.string.artifact_not_found) + 
                            " - 未找到result.json文件。已提取的文件: ${extractedFiles.joinToString()}")
                    }
                    
                    // 清理临时文件
                    zipFile.delete()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, getString(R.string.result_processing_error, e.message), Toast.LENGTH_LONG).show()
                resetRunState()
            }
        }
    }
    
    private fun unzipFile(zipFile: File, destinationDir: File) {
        ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zipInputStream ->
            var entry = zipInputStream.nextEntry
            val buffer = ByteArray(1024)
            
            while (entry != null) {
                val file = File(destinationDir, entry.name)
                
                // 创建父目录
                if (!file.parentFile?.exists()!!) {
                    file.parentFile?.mkdirs()
                }
                
                // 如果是目录，创建目录
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    // 写入文件内容
                    FileOutputStream(file).use { fileOutputStream ->
                        var len: Int
                        while (zipInputStream.read(buffer).also { len = it } > 0) {
                            fileOutputStream.write(buffer, 0, len)
                        }
                    }
                }
                
                zipInputStream.closeEntry()
                entry = zipInputStream.nextEntry
            }
        }
    }
    
    private fun resetRunState() {
        runCodeButton.isEnabled = true
        isGitHubRunning = false
    }
    
    private fun setStatusColor(status: String) {
        when (status) {
            "CE" -> runStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            "AC" -> runStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
            "WA" -> runStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_light))
            "TLE" -> runStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark))
            "MLE" -> runStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_orange_light))
            "RE" -> runStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_purple))
            "RS" -> runStatus.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_blue_light))
        }
    }
    
    override fun onPause() {
        super.onPause()
        // 保存到缓存
        saveToCache()
        
        // 取消轮询任务
        pollingJob?.cancel()
    }
} 