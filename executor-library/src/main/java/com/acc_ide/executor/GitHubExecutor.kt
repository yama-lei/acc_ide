package com.acc_ide.executor

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.preference.PreferenceManager
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

/**
 * GitHub Actions based code executor
 * GitHub Actions云端代码执行器
 */
class GitHubExecutor(private val context: Context) : ICodeExecutor {
    
    companion object {
        private const val TAG = "GitHubExecutor"
        private const val GITHUB_API_BASE = "https://api.github.com"
        private const val PREF_GITHUB_REPO_URL = "github_repo_url"
        private const val PREF_GITHUB_PAT = "github_pat"
        
        private val LANGUAGE_MAP = mapOf(
            "cpp" to "cpp",
            "python" to "py",
            "java" to "java"
        )
    }
    
    private var isRunning = false
    private var pollingJob: Job? = null
    
    override fun executeCode(
        code: String,
        language: String,
        input: String,
        expectedOutput: String,
        onProgress: ((String) -> Unit)?,
        onComplete: (ExecutionResult) -> Unit,
        onError: (String) -> Unit
    ) {
        if (isRunning) {
            onError("Already running")
            return
        }
        
        // Get GitHub settings
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val repoUrl = prefs.getString(PREF_GITHUB_REPO_URL, "") ?: ""
        val pat = prefs.getString(PREF_GITHUB_PAT, "") ?: ""
        
        if (repoUrl.isEmpty() || pat.isEmpty()) {
            onError("GitHub settings not configured")
            return
        }
        
        // Parse repository owner and name
        val regex = "https://github.com/([^/]+)/([^/]+)".toRegex()
        val match = regex.find(repoUrl)
        
        if (match == null || match.groupValues.size < 3) {
            onError("Invalid repository URL")
            return
        }
        
        val owner = match.groupValues[1]
        val repo = match.groupValues[2].removeSuffix(".git")
        val mappedLanguage = LANGUAGE_MAP[language] ?: "cpp"
        
        isRunning = true
        onProgress?.invoke("Triggering GitHub workflow...")
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val runId = triggerGitHubWorkflow(owner, repo, pat, code, input, expectedOutput, mappedLanguage)
                
                if (runId > 0) {
                    pollWorkflowStatus(owner, repo, pat, runId, onProgress, onComplete, onError)
                } else {
                    withContext(Dispatchers.Main) {
                        onError("Failed to trigger workflow")
                        isRunning = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Workflow error: ${e.message}")
                    isRunning = false
                }
            }
        }
    }
    
    override fun cancelExecution() {
        pollingJob?.cancel()
        isRunning = false
    }
    
    override fun isExecuting(): Boolean = isRunning
    
    override fun getExecutorName(): String = "GitHub Actions (Cloud)"
    
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
        
        val jsonBody = JSONObject().apply {
            put("ref", "main")
            val inputs = JSONObject().apply {
                val encodedCode = Base64.encodeToString(code.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                put("source_code", encodedCode)
                put("std_input", input)
                put("expected_output", expected)
                put("language", language)
            }
            put("inputs", inputs)
        }
        
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toString().toRequestBody(mediaType)
        val workflowFileName = "code-execution.yml"
        
        val request = Request.Builder()
            .url("$GITHUB_API_BASE/repos/$owner/$repo/actions/workflows/$workflowFileName/dispatches")
            .header("Authorization", "token $pat")
            .header("Accept", "application/vnd.github.v3+json")
            .post(requestBody)
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("GitHub API error: ${response.body?.string()}")
            }
            delay(2000)
            getLatestWorkflowRunId(owner, repo, pat)
        }
    }
    
    private suspend fun getLatestWorkflowRunId(owner: String, repo: String, pat: String): Long = 
        withContext(Dispatchers.IO) {
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
                    throw IOException("Failed to get workflow run ID")
                }
                
                val responseBody = response.body?.string() ?: "{}"
                val jsonResponse = JSONObject(responseBody)
                val workflowRuns = jsonResponse.getJSONArray("workflow_runs")
                
                if (workflowRuns.length() > 0) {
                    workflowRuns.getJSONObject(0).getLong("id")
                } else {
                    -1L
                }
            }
        }
    
    private fun pollWorkflowStatus(
        owner: String,
        repo: String,
        pat: String,
        runId: Long,
        onProgress: ((String) -> Unit)?,
        onComplete: (ExecutionResult) -> Unit,
        onError: (String) -> Unit
    ) {
        pollingJob?.cancel()
        
        pollingJob = CoroutineScope(Dispatchers.IO).launch {
            var isCompleted = false
            var attempts = 0
            
            while (!isCompleted && attempts < 60) {
                delay(2000)
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
                                onProgress?.invoke("Running... ${attempts * 2}s")
                            }
                            
                            if (status == "completed") {
                                isCompleted = true
                                downloadAndProcessArtifacts(owner, repo, pat, runId, onComplete, onError)
                            }
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        onError("Polling error: ${e.message}")
                    }
                    break
                }
            }
            
            if (!isCompleted) {
                withContext(Dispatchers.Main) {
                    onError("Workflow timeout")
                    isRunning = false
                }
            }
        }
    }
    
    private suspend fun downloadAndProcessArtifacts(
        owner: String,
        repo: String,
        pat: String,
        runId: Long,
        onComplete: (ExecutionResult) -> Unit,
        onError: (String) -> Unit
    ) = withContext(Dispatchers.IO) {
        try {
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
                    throw IOException("Failed to get artifacts")
                }
                
                val responseBody = response.body?.string() ?: "{}"
                val jsonResponse = JSONObject(responseBody)
                val artifacts = jsonResponse.getJSONArray("artifacts")
                
                var artifactId = -1L
                for (i in 0 until artifacts.length()) {
                    val artifact = artifacts.getJSONObject(i)
                    if (artifact.getString("name") == "judge-result") {
                        artifactId = artifact.getLong("id")
                        break
                    }
                }
                
                if (artifactId == -1L) {
                    throw IOException("Artifact not found")
                }
                
                val downloadRequest = Request.Builder()
                    .url("$GITHUB_API_BASE/repos/$owner/$repo/actions/artifacts/$artifactId/zip")
                    .header("Authorization", "token $pat")
                    .header("Accept", "application/vnd.github.v3+json")
                    .build()
                
                client.newCall(downloadRequest).execute().use { downloadResponse ->
                    if (!downloadResponse.isSuccessful) {
                        throw IOException("Download failed")
                    }
                    
                    val zipFile = File(context.cacheDir, "judge-result.zip")
                    downloadResponse.body?.byteStream()?.use { input ->
                        FileOutputStream(zipFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    
                    val resultsDir = File(context.cacheDir, "judge-results")
                    resultsDir.mkdirs()
                    resultsDir.listFiles()?.forEach { it.delete() }
                    
                    unzipFile(zipFile, resultsDir)
                    
                    val resultFile = File(resultsDir, "result.json")
                    val actualOutputFile = File(resultsDir, "actual.txt")
                    
                    if (resultFile.exists()) {
                        val resultJson = JSONObject(resultFile.readText())
                        val status = resultJson.getString("status")
                        val executionTime = resultJson.optInt("execution_time", 0)
                        
                        var actualOutput = "No output"
                        if (actualOutputFile.exists()) {
                            actualOutput = actualOutputFile.readText()
                        } else if (status == "CE") {
                            val compileErrorFile = File(resultsDir, "compile.log")
                            if (compileErrorFile.exists()) {
                                actualOutput = compileErrorFile.readText()
                            }
                        }
                        
                        withContext(Dispatchers.Main) {
                            onComplete(ExecutionResult(status, actualOutput, executionTime))
                            isRunning = false
                        }
                    } else {
                        throw IOException("result.json not found")
                    }
                    
                    zipFile.delete()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                onError("Result processing error: ${e.message}")
                isRunning = false
            }
        }
    }
    
    private fun unzipFile(zipFile: File, destinationDir: File) {
        ZipInputStream(BufferedInputStream(zipFile.inputStream())).use { zipInputStream ->
            var entry = zipInputStream.nextEntry
            val buffer = ByteArray(1024)
            
            while (entry != null) {
                val file = File(destinationDir, entry.name)
                file.parentFile?.mkdirs()
                
                if (!entry.isDirectory) {
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
}

