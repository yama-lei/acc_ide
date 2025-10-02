package com.acc_ide.executor.wasm

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Java WASM Executor using CheerpJ
 * 使用CheerpJ的Java WASM执行器
 * 基于 https://leaningtech.com/cheerpj/
 */
class WasmJavaExecutor(private val context: Context) : WasmExecutorInterface {
    
    private val TAG = "WasmJavaExecutor"
    private var webView: WebView? = null
    private var isInitialized = false
    
    private var onOutputCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private var onCompleteCallback: ((Int) -> Unit)? = null
    
    // 保存初始化回调
    private var initReadyCallback: (() -> Unit)? = null
    private var initErrorCallback: ((String) -> Unit)? = null
    
    override fun initialize(onReady: () -> Unit, onError: (String) -> Unit) {
        Log.d(TAG, "Initializing WASM Java executor...")
        
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
                    // 允许WASM和跨域访问（CheerpJ需要）
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = true
                    javaScriptCanOpenWindowsAutomatically = true
                }
                
                addJavascriptInterface(JsInterface(), "AndroidBridge")
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        Log.d(TAG, "WebView page loaded: $url")
                        // CheerpJ会在HTML中自动初始化，等待ready消息
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
                
                // 加载Java执行器HTML
                loadUrl("file:///android_asset/wasm/java/java_executor.html")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize WebView", e)
            onError("Failed to initialize Java WASM: ${e.message}")
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
            onError("Java WASM not initialized")
            return
        }
        
        this.onOutputCallback = onOutput
        this.onErrorCallback = onError
        this.onCompleteCallback = onComplete
        
        val escapedCode = escapeForJs(code)
        val escapedInput = escapeForJs(input)
        
        Log.d(TAG, "Executing Java code via WASM...")
        
        webView?.evaluateJavascript("""
            compileAndRunJava(`$escapedCode`, `$escapedInput`)
        """) { result ->
            Log.d(TAG, "Execution started: $result")
        }
    }
    
    override fun isReady(): Boolean = isInitialized
    
    override fun getExecutorName(): String = "WASM Java (CheerpJ)"
    
    override fun cleanup() {
        webView?.destroy()
        webView = null
        isInitialized = false
        initReadyCallback = null
        initErrorCallback = null
        onOutputCallback = null
        onErrorCallback = null
        onCompleteCallback = null
    }
    
    private fun escapeForJs(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("`", "\\`")
            .replace("$", "\\$")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\"", "\\\"")
    }
    
    /**
     * JavaScript Interface for WebView communication
     * WebView通信的JavaScript接口
     */
    inner class JsInterface {
        
        private val mainHandler = Handler(Looper.getMainLooper())
        
        @JavascriptInterface
        fun onJavaReady() {
            Log.d(TAG, "CheerpJ Java executor is ready")
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
        fun onJavaError(error: String) {
            Log.e(TAG, "CheerpJ initialization error: $error")
            mainHandler.post {
                initErrorCallback?.invoke(error)
            }
        }
        
        @JavascriptInterface
        fun onOutput(output: String) {
            Log.d(TAG, "Java output: $output")
            mainHandler.post {
                onOutputCallback?.invoke(output)
            }
        }
        
        @JavascriptInterface
        fun onError(error: String) {
            Log.e(TAG, "Java execution error: $error")
            mainHandler.post {
                onErrorCallback?.invoke(error)
            }
        }
        
        @JavascriptInterface
        fun onComplete(exitCode: Int) {
            Log.d(TAG, "Java execution completed: $exitCode")
            mainHandler.post {
                onCompleteCallback?.invoke(exitCode)
            }
        }
        
        @JavascriptInterface
        fun onCompileError(error: String) {
            Log.e(TAG, "Java compilation error: $error")
            mainHandler.post {
                onErrorCallback?.invoke("Compilation Error:\n$error")
                onCompleteCallback?.invoke(1)
            }
        }
    }
}

