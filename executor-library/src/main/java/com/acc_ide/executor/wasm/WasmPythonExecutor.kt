package com.acc_ide.executor.wasm

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Python WASM Executor using Pyodide
 * 使用Pyodide的Python WASM执行器
 */
class WasmPythonExecutor(private val context: Context) : WasmExecutorInterface {
    
    private val TAG = "WasmPythonExecutor"
    private var webView: WebView? = null
    private var isInitialized = false
    
    private var onOutputCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private var onCompleteCallback: ((Int) -> Unit)? = null
    
    // 保存初始化回调
    private var initReadyCallback: (() -> Unit)? = null
    private var initErrorCallback: ((String) -> Unit)? = null
    
    override fun initialize(onReady: () -> Unit, onError: (String) -> Unit) {
        Log.d(TAG, "Initializing WASM Python executor...")
        
        // 保存回调
        this.initReadyCallback = onReady
        this.initErrorCallback = onError
        
        try {
            webView = WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    databaseEnabled = true
                    // Pyodide需要更多内存
                    javaScriptCanOpenWindowsAutomatically = true
                }
                
                addJavascriptInterface(JsInterface(), "AndroidBridge")
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        Log.d(TAG, "WebView page loaded: $url")
                        // Pyodide会在HTML中自动初始化，等待ready消息
                    }
                    
                    override fun onReceivedError(
                        view: WebView?,
                        errorCode: Int,
                        description: String?,
                        failingUrl: String?
                    ) {
                        Log.e(TAG, "WebView error: $description")
                        initErrorCallback?.invoke("WebView error: $description")
                    }
                }
                
                // 加载Pyodide执行器
                loadUrl("file:///android_asset/wasm/python/python_executor.html")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize WebView", e)
            onError("Failed to initialize Python WASM: ${e.message}")
        }
    }
    
    override fun execute(
        code: String,
        input: String,
        onOutput: (String) -> Unit,
        onError: (String) -> Unit,
        onComplete: (Int) -> Unit
    ) {
        if (!isInitialized) {
            onError("Python WASM not initialized")
            return
        }
        
        this.onOutputCallback = onOutput
        this.onErrorCallback = onError
        this.onCompleteCallback = onComplete
        
        val escapedCode = escapeForJs(code)
        val escapedInput = escapeForJs(input)
        
        Log.d(TAG, "Executing Python code via WASM...")
        
        webView?.evaluateJavascript("""
            runPythonCode(`$escapedCode`, `$escapedInput`)
        """) { result ->
            Log.d(TAG, "Execution started: $result")
        }
    }
    
    override fun isReady(): Boolean = isInitialized
    
    override fun getExecutorName(): String = "WASM Python (Pyodide)"
    
    override fun cleanup() {
        webView?.destroy()
        webView = null
        isInitialized = false
    }
    
    private fun escapeForJs(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
    }
    
    inner class JsInterface {
        
        private val mainHandler = Handler(Looper.getMainLooper())
        
        @JavascriptInterface
        fun onPyodideReady() {
            Log.d(TAG, "Pyodide is ready, calling onReady callback on main thread")
            isInitialized = true
            // 在主线程调用回调
            mainHandler.post {
                initReadyCallback?.invoke()
                // 清理回调引用
                initReadyCallback = null
                initErrorCallback = null
            }
        }
        
        @JavascriptInterface
        fun onPyodideError(error: String) {
            Log.e(TAG, "Pyodide initialization error: $error")
            mainHandler.post {
                initErrorCallback?.invoke(error)
            }
        }
        
        @JavascriptInterface
        fun onOutput(output: String) {
            Log.d(TAG, "Python output: $output")
            mainHandler.post {
                onOutputCallback?.invoke(output)
            }
        }
        
        @JavascriptInterface
        fun onError(error: String) {
            Log.e(TAG, "Python error: $error")
            mainHandler.post {
                onErrorCallback?.invoke(error)
            }
        }
        
        @JavascriptInterface
        fun onComplete(exitCode: Int) {
            Log.d(TAG, "Python execution completed: $exitCode")
            mainHandler.post {
                onCompleteCallback?.invoke(exitCode)
            }
        }
    }
}

