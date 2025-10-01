package com.acc_ide.execution.wasm

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * C++ WASM Executor using wasm-clang
 * 使用wasm-clang的C++ WASM执行器
 */
class WasmCppExecutor(private val context: Context) : WasmExecutorInterface {
    
    private val TAG = "WasmCppExecutor"
    private var webView: WebView? = null
    private var isInitialized = false
    
    private var onOutputCallback: ((String) -> Unit)? = null
    private var onErrorCallback: ((String) -> Unit)? = null
    private var onCompleteCallback: ((Int) -> Unit)? = null
    
    override fun initialize(onReady: () -> Unit, onError: (String) -> Unit) {
        Log.d(TAG, "Initializing WASM C++ executor...")
        
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
                        // 页面加载完成，初始化WASM
                        initializeWasm(onReady, onError)
                    }
                }
                
                // 加载WASM编译器HTML
                loadUrl("file:///android_asset/wasm/cpp_compiler.html")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize WebView", e)
            onError("Failed to initialize WASM: ${e.message}")
        }
    }
    
    private fun initializeWasm(onReady: () -> Unit, onError: (String) -> Unit) {
        webView?.evaluateJavascript("""
            (function() {
                if (typeof initializeClang === 'function') {
                    initializeClang()
                        .then(() => {
                            AndroidBridge.onWasmReady();
                            return 'WASM initialized';
                        })
                        .catch(err => {
                            AndroidBridge.onWasmError(err.toString());
                            return 'WASM init failed: ' + err;
                        });
                } else {
                    return 'initializeClang not found';
                }
            })()
        """) { result ->
            Log.d(TAG, "WASM initialization result: $result")
            if (result.contains("initialized")) {
                isInitialized = true
                onReady()
            } else {
                onError("WASM initialization failed: $result")
            }
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
    
    override fun getExecutorName(): String = "WASM C++ Compiler (Clang)"
    
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
            Log.d(TAG, "WASM runtime is ready")
            isInitialized = true
        }
        
        @JavascriptInterface
        fun onWasmError(error: String) {
            Log.e(TAG, "WASM error: $error")
            mainHandler.post {
                onErrorCallback?.invoke(error)
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

