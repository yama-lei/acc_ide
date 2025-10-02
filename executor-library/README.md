# ACC Code Executor Library

[![](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white)]()
[![](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)]()
[![](https://img.shields.io/badge/MinSDK-24-green)]()

一个强大的Android代码执行器库，支持本地WASM执行和云端GitHub Actions执行。

## 📋 功能特性

- ✅ **多语言支持**: C++, Python, Java
- ✅ **本地WASM执行**: 完全离线的本地代码编译和执行
- ✅ **云端执行**: 基于GitHub Actions的云端代码执行
- ✅ **统一接口**: 简洁的API设计，易于集成
- ✅ **ANSI颜色支持**: 终端输出的颜色渲染

## 🚀 快速开始

### 集成到项目

**步骤1: 添加模块**

```kotlin
// settings.gradle.kts
include(":app")
include(":executor-library")
```

**步骤2: 添加依赖**

```gradle
// app/build.gradle
dependencies {
    implementation project(':executor-library')
}
```

**步骤3: 使用**

```kotlin
import com.acc_ide.executor.ExecutorFactory

val executor = ExecutorFactory.createExecutor(context)

executor.executeCode(
    code = """
        #include <iostream>
        int main() {
            std::cout << "Hello World!" << std::endl;
            return 0;
        }
    """.trimIndent(),
    language = "cpp",
    input = "",
    expectedOutput = "Hello World!",
    onComplete = { result ->
        println("Output: ${result.actualOutput}")
        println("Status: ${result.status}")
    },
    onError = { error ->
        println("Error: $error")
    }
)
```

## 📚 核心API

### ExecutorFactory

```kotlin
// 创建执行器（自动选择本地/云端）
val executor = ExecutorFactory.createExecutor(context)

// 设置执行模式
ExecutorFactory.setExecutionMode(context, ExecutorFactory.MODE_LOCAL)  // 本地
ExecutorFactory.setExecutionMode(context, ExecutorFactory.MODE_CLOUD)  // 云端

// 获取当前模式
val mode = ExecutorFactory.getExecutionMode(context)
```

### ICodeExecutor

```kotlin
interface ICodeExecutor {
    fun executeCode(
        code: String,
        language: String,        // "cpp", "python", "java"
        input: String,
        expectedOutput: String,
        onProgress: ((String) -> Unit)? = null,
        onComplete: (ExecutionResult) -> Unit,
        onError: (String) -> Unit
    )
    fun cancelExecution()
    fun isExecuting(): Boolean
    fun getExecutorName(): String
}
```

### ExecutionResult

```kotlin
data class ExecutionResult(
    val status: String,         // AC, WA, CE, RE, TLE, MLE
    val actualOutput: String,   // 实际输出
    val executionTime: Int,     // 执行时间（毫秒）
    val errorMessage: String    // 错误信息
)
```

### 状态码

| 状态码 | 含义 |
|-------|------|
| AC | Accepted - 正确答案 |
| WA | Wrong Answer - 答案错误 |
| CE | Compile Error - 编译错误 |
| RE | Runtime Error - 运行时错误 |
| TLE | Time Limit Exceeded - 超时 |
| MLE | Memory Limit Exceeded - 内存超限 |

## 💡 使用示例

### 执行C++代码

```kotlin
executor.executeCode(
    code = """
        #include <iostream>
        int main() {
            int a, b;
            std::cin >> a >> b;
            std::cout << a + b << std::endl;
            return 0;
        }
    """.trimIndent(),
    language = "cpp",
    input = "5 10",
    expectedOutput = "15",
    onComplete = { result ->
        when (result.status) {
            "AC" -> println("✅ Success!")
            "CE" -> println("⚠️ Compile Error: ${result.errorMessage}")
            "WA" -> println("❌ Wrong Answer")
        }
    },
    onError = { println("Error: $it") }
)
```

### 执行Python代码

```kotlin
executor.executeCode(
    code = """
        name = input()
        print(f"Hello, {name}!")
    """.trimIndent(),
    language = "python",
    input = "World",
    expectedOutput = "Hello, World!",
    onComplete = { result -> println(result.actualOutput) },
    onError = { println(it) }
)
```

### 执行Java代码

```kotlin
executor.executeCode(
    code = """
        import java.util.Scanner;
        public class Main {
            public static void main(String[] args) {
                Scanner sc = new Scanner(System.in);
                System.out.println("Hello, " + sc.nextLine());
            }
        }
    """.trimIndent(),
    language = "java",
    input = "Java",
    expectedOutput = "Hello, Java",
    onComplete = { result -> println(result.actualOutput) },
    onError = { println(it) }
)
```

### 在Fragment中使用

```kotlin
class MyFragment : Fragment() {
    private lateinit var executor: ICodeExecutor
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        executor = ExecutorFactory.createExecutor(requireContext())
        
        runButton.setOnClickListener {
            executor.executeCode(
                code = codeEditor.text.toString(),
                language = "cpp",
                input = inputField.text.toString(),
                expectedOutput = "",
                onProgress = { msg -> statusText.text = msg },
                onComplete = { result -> 
                    outputText.text = result.actualOutput
                    statusText.text = result.status
                },
                onError = { error -> outputText.text = error }
            )
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        if (executor is LocalExecutor) {
            (executor as LocalExecutor).cleanup()
        }
    }
}
```

## 🔧 工具类

### AnsiTextParser

解析ANSI颜色代码为Android样式文本：

```kotlin
val coloredText = AnsiTextParser.parseAnsiText(
    "\u001B[31mError\u001B[0m: undefined reference"
)
textView.text = coloredText
```

### IOCacheManager

管理输入输出缓存：

```kotlin
// 保存缓存
IOCacheManager.save(fileName, IOInstance(
    input = "1 2 3",
    actualOutput = "6",
    expectedOutput = "6",
    status = "AC",
    executionTime = 50
))

// 获取缓存
val cached = IOCacheManager.get(fileName)

// 清除缓存
IOCacheManager.clear(fileName)
```

## 📦 支持的语言

### C++
- **编译器**: wasm-clang (完整的Clang/LLVM工具链)
- **标准**: C++17
- **特性**: 完整的STL支持
- **编译时间**: 1-3秒

### Python
- **解释器**: Pyodide 0.24+
- **版本**: Python 3.11
- **特性**: NumPy, Pandas等科学计算库
- **首次加载**: 10-30秒（后续<1秒）

### Java
- **运行时**: 简化的Java转译器
- **版本**: Java 8基本语法
- **特性**: 常用API支持
- **编译时间**: <1秒

## 🏗️ 架构设计

```
executor-library/
├── src/main/
│   ├── java/com/acc_ide/executor/
│   │   ├── ICodeExecutor.kt           # 执行器接口
│   │   ├── ExecutorFactory.kt         # 工厂类
│   │   ├── LocalExecutor.kt           # 本地执行器
│   │   ├── GitHubExecutor.kt          # 云端执行器
│   │   ├── ExecutionResult.kt         # 结果数据类
│   │   ├── AnsiTextParser.kt          # ANSI解析器
│   │   ├── IOCacheManager.kt          # IO缓存
│   │   └── wasm/                      # WASM执行器
│   │       ├── WasmExecutorInterface.kt
│   │       ├── WasmCppExecutor.kt
│   │       ├── WasmPythonExecutor.kt
│   │       └── WasmJavaExecutor.kt
│   └── assets/wasm/                   # WASM资源（按语言分类）
│       ├── cpp/                       # C++资源（~60MB）
│       ├── python/                    # Python资源
│       ├── java/                      # Java资源
│       └── common/                    # 共享JS资源
└── build.gradle
```

详细的目录结构说明请查看 [`src/main/assets/wasm/README.md`](src/main/assets/wasm/README.md)

## ⚙️ 配置要求

### 最低要求
- Android API 24+ (Android 7.0)
- Kotlin 1.9+
- 至少 200MB 存储空间（用于WASM资源）

### 推荐配置
- Android API 28+ (Android 9.0)
- 2GB+ RAM
- 网络连接（首次加载Pyodide时需要）

## 🔐 权限

Library会自动在AndroidManifest中声明以下权限：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 🎯 常见问题

**Q: 首次运行Python很慢？**

A: Pyodide需要从CDN下载资源（10-20MB），首次加载需要10-30秒。后续运行会使用缓存。

**Q: APK体积增大了很多？**

A: WASM资源约60MB。建议使用Android App Bundle (AAB)格式，或实现按需下载。

**Q: 如何只使用本地执行？**

A: 直接创建LocalExecutor实例：
```kotlin
val executor = LocalExecutor(context)
```

**Q: 如何配置GitHub Actions执行器？**

A: 在SharedPreferences中设置：
```kotlin
val prefs = PreferenceManager.getDefaultSharedPreferences(context)
prefs.edit().apply {
    putString("github_repo_url", "https://github.com/username/repo")
    putString("github_pat", "ghp_your_token")
    apply()
}
ExecutorFactory.setExecutionMode(context, ExecutorFactory.MODE_CLOUD)
```

## 📝 ProGuard配置

如果使用代码混淆，添加以下规则：

```proguard
-keep public class com.acc_ide.executor.** { public *; }
-keep interface com.acc_ide.executor.ICodeExecutor { *; }
-keepclassmembers class com.acc_ide.executor.ExecutionResult { *; }
```

## 📄 License

本library作为ACC IDE项目的一部分，遵循项目的开源协议。

## 🤝 贡献

欢迎提交Issue和Pull Request！

## 📚 更多文档

- [`CHANGELOG.md`](CHANGELOG.md) - 版本变更记录
- [`src/main/assets/wasm/README.md`](src/main/assets/wasm/README.md) - WASM资源详细说明

