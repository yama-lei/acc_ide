package com.acc_ide.executor.wasm

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * C++ WASM Executor using wasm-clang
 * 使用wasm-clang的C++ WASM执行器 - 完整的C++编译器支持
 * 基于 https://github.com/binji/wasm-clang
 */
class WasmCppExecutor(private val context: Context) : WasmExecutorInterface {
    
    private val TAG = "WasmCppExecutor"
    private var webView: WebView? = null
    private var isInitialized = false
    
    private var onOutputCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private var onCompleteCallback: ((Int) -> Unit)? = null
    
    private var readyCallback: (() -> Unit)? = null
    private var errorCallback: ((String) -> Unit)? = null
    
    override fun initialize(onReady: () -> Unit, onError: (String) -> Unit) {
        Log.d(TAG, "Initializing wasm-clang C++ executor...")
        
        // 保存回调，等待JavaScript通知
        this.readyCallback = onReady
        this.errorCallback = onError
        
        try {
            webView = WebView(context).apply {
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    allowFileAccess = true
                    allowContentAccess = true
                    databaseEnabled = true
                    // 允许WASM
                    @Suppress("DEPRECATION")
                    allowUniversalAccessFromFileURLs = true
                }
                
                // 添加JavaScript接口
                addJavascriptInterface(JsInterface(), "AndroidBridge")
                
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        Log.d(TAG, "WebView page loaded: $url")
                        // 页面加载完成后，JavaScript会自动初始化并调用onWasmReady
                        Log.d(TAG, "Waiting for wasm-clang initialization callback...")
                    }
                }
                
                // 加载WASM编译器HTML
                loadUrl("file:///android_asset/wasm/cpp/cpp_executor.html")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize WebView", e)
            onError("Failed to initialize WASM: ${e.message}")
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
            onError("WASM not initialized")
            return
        }
        
        this.onOutputCallback = onOutput
        this.onErrorCallback = onError
        this.onCompleteCallback = onComplete
        
        // 转义代码和输入
        val escapedCode = escapeForJs(code)
        val escapedInput = escapeForJs(input)
        
        Log.d(TAG, "Executing C++ code via WASM...")
        
        webView?.evaluateJavascript("""
            (function() {
                try {
                    compileAndRun(`$escapedCode`, `$escapedInput`);
                    return 'Execution started';
                } catch (err) {
                    AndroidBridge.onExecutionError(err.toString());
                    return 'Error: ' + err;
                }
            })()
        """) { result ->
            Log.d(TAG, "Execution trigger result: $result")
        }
    }
    
    override fun isReady(): Boolean = isInitialized
    
    override fun getExecutorName(): String = "C++ Compiler (wasm-clang)"
    
    override fun cleanup() {
        webView?.destroy()
        webView = null
        isInitialized = false
        readyCallback = null
        errorCallback = null
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
        fun onWasmReady() {
            Log.d(TAG, "wasm-clang C++ compiler is ready")
            isInitialized = true
            mainHandler.post {
                readyCallback?.invoke()
                readyCallback = null // 清空，只调用一次
            }
        }
        
        @JavascriptInterface
        fun onWasmError(error: String) {
            Log.e(TAG, "WASM error: $error")
            mainHandler.post {
                errorCallback?.invoke(error)
                errorCallback = null // 清空
            }
        }
        
        @JavascriptInterface
        fun onOutput(output: String) {
            Log.d(TAG, "Program output: $output")
            mainHandler.post {
                onOutputCallback?.invoke(output)
            }
        }
        
        @JavascriptInterface
        fun onExecutionError(error: String) {
            Log.e(TAG, "Execution error: $error")
            mainHandler.post {
                onErrorCallback?.invoke(error)
            }
        }
        
        @JavascriptInterface
        fun onExecutionComplete(exitCode: Int) {
            Log.d(TAG, "Execution completed with exit code: $exitCode")
            mainHandler.post {
                onCompleteCallback?.invoke(exitCode)
            }
        }
    }
}

