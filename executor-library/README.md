# ACC Code Executor Library

[![](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white)]()
[![](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)]()
[![](https://img.shields.io/badge/MinSDK-24-green)]()

Android 代码执行器库，支持本地 WASM 执行和云端 GitHub Actions 执行。

## 📋 功能特性

- ✅ **多语言支持**: C++, Python, Java
- ✅ **本地 WASM 执行**: 完全离线的本地编译和执行
- ✅ **云端执行**: 基于 GitHub Actions 的云端执行
- ✅ **性能优化**: 模块缓存 + 预热系统，首次执行快 25%
- ✅ **ANSI 颜色支持**: 终端输出的颜色渲染
- ✅ **统一接口**: 简洁的 API 设计

## 🚀 快速开始

### 集成

```gradle
// settings.gradle.kts
include(":app", ":executor-library")

// app/build.gradle
dependencies {
    implementation project(':executor-library')
}
```

### 基本使用

```kotlin
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
        println("Status: ${result.status}")
        println("Output: ${result.actualOutput}")
    },
    onError = { error -> println("Error: $error") }
)
```

## 📚 核心 API

### ExecutorFactory

```kotlin
// 创建执行器
val executor = ExecutorFactory.createExecutor(context)

// 设置执行模式
ExecutorFactory.setExecutionMode(context, ExecutorFactory.MODE_LOCAL)  // 本地
ExecutorFactory.setExecutionMode(context, ExecutorFactory.MODE_CLOUD)  // 云端
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
}
```

### ExecutionResult

```kotlin
data class ExecutionResult(
    val status: String,         // AC, WA, CE, RE, TLE, MLE, RS
    val actualOutput: String,
    val executionTime: Int,     // ms
    val errorMessage: String
)
```

**状态码**：`AC` (正确) | `WA` (答案错误) | `CE` (编译错误) | `RE` (运行时错误) | `TLE` (超时) | `MLE` (内存超限) | `RS` (运行成功)

## 💡 使用示例

### C++ 代码

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
            "AC" -> println("✅ Accepted")
            "CE" -> println("⚠️ Compile Error")
            "WA" -> println("❌ Wrong Answer")
        }
    }
)
```

### Python 代码

```kotlin
executor.executeCode(
    code = "print('Hello, World!')",
    language = "python",
    input = "",
    expectedOutput = "Hello, World!",
    onComplete = { println(it.actualOutput) }
)
```

### Java 代码

```kotlin
executor.executeCode(
    code = """
        public class Main {
            public static void main(String[] args) {
                System.out.println("Hello Java");
            }
        }
    """.trimIndent(),
    language = "java",
    input = "",
    expectedOutput = "Hello Java",
    onComplete = { println(it.actualOutput) }
)
```

## 🔧 工具类

### AnsiTextParser

解析 ANSI 颜色代码为 Android 样式文本：

```kotlin
val coloredText = AnsiTextParser.parseAnsiText("\u001B[31mError\u001B[0m")
textView.text = coloredText
```

### IOCacheManager

管理输入输出缓存：

```kotlin
IOCacheManager.save(fileName, IOInstance(...))
val cached = IOCacheManager.get(fileName)
IOCacheManager.clear(fileName)
```

## 📦 支持的语言

| 语言 | 引擎 | 版本 | 性能 |
|------|------|------|------|
| **C++** | wasm-clang | C++17 | 首次 ~2.1s，后续 ~2.1s (预热优化) |
| **Python** | Pyodide | 3.11 | ~600ms (CDN首次加载需10-30s) |
| **Java** | 简化解释器 | Java 8 | <1s |

**优化亮点**: C++ 预热系统在应用启动时预加载编译器，首次执行节省 ~700ms

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
│   │   ├── AnsiTextParser.kt          # ANSI 解析器
│   │   ├── IOCacheManager.kt          # IO 缓存
│   │   └── wasm/                      # WASM 执行器
│   │       ├── WasmExecutorInterface.kt
│   │       ├── WasmCppExecutor.kt
│   │       ├── WasmPythonExecutor.kt
│   │       └── WasmJavaExecutor.kt
│   └── assets/wasm/                   # WASM 资源
│       ├── cpp/                       # C++ 资源 (~60MB)
│       │   ├── lib/                   # wasm-clang 库文件
│       │   ├── cpp_executor.html
│       │   ├── cpp_executor.js
│       │   ├── clang, lld, memfs
│       │   └── sysroot.tar
│       ├── python/                    # Python 资源
│       │   ├── python_executor.html
│       │   └── python_executor.js
│       └── java/                      # Java 资源
│           └── java_executor.html
└── build.gradle
```

详见 [`src/main/assets/wasm/README.md`](src/main/assets/wasm/README.md)

## ⚙️ 配置要求

- **最低**: Android API 24+, Kotlin 1.9+, 200MB 存储
- **推荐**: Android API 28+, 2GB+ RAM
- **权限**: `INTERNET`, `ACCESS_NETWORK_STATE`

## 🎯 常见问题

**Q: 首次运行 Python 很慢？**  
A: Pyodide 首次需下载资源（10-20MB），约 10-30 秒，后续使用缓存。

**Q: C++ 编译时间如何优化？**  
A: 预热系统已自动启用，在 SplashActivity 中预加载编译器，首次执行节省 ~700ms。

**Q: APK 体积增大？**  
A: WASM 资源约 60MB，建议使用 Android App Bundle (AAB) 或按需下载。

**Q: 只使用本地执行？**  
A: 直接创建 `LocalExecutor(context)`

**Q: 配置 GitHub Actions 执行？**  
A: 在 SharedPreferences 中设置 `github_repo_url` 和 `github_pat`

## 📝 ProGuard 配置

```proguard
-keep public class com.acc_ide.executor.** { public *; }
-keep interface com.acc_ide.executor.ICodeExecutor { *; }
```

## 📄 License

本 library 作为 ACC IDE 项目的一部分开源。

## ⚡ 性能优化

### Bug 修复 (2025-10-02)
- ✅ 修复 `write64()` - 防止 64 位值截断导致内存损坏
- ✅ 修复 `check()` - 正确检测 WASM 内存增长
- ✅ Canvas API 枚举验证 - 防止越界访问

### 预热系统
**自动启用**: C++ 编译器在应用启动时预加载，首次执行快 25%

| 场景 | 优化前 | 优化后 | 提升 |
|------|--------|--------|------|
| 应用启动 | 1-2s | 1.5-2.5s | -500ms |
| 首次执行 | 2.8s | 2.1s | **+700ms** ✅ |

**工作原理**: 在 `SplashActivity` 中预加载 clang/lld，用户执行时直接编译代码。

**高级用法**:
```kotlin
// 检查状态
WasmPrewarmManager.isCppReady()

// 清理资源（低内存时）
WasmPrewarmManager.cleanup()
```

### 模块缓存
编译器模块自动缓存，后续执行无需重新加载，节省 ~680ms。

### 性能对比
| 语言 | 编译 | 执行 | 总耗时 | 特点 |
|------|------|------|--------|------|
| Python | 0ms | 50-100ms | ~600ms | 预编译 VM，无需编译 |
| C++ | 2110ms | 10ms | ~2210ms | 必须编译，但执行快 |

**推荐**: Python 适合快速迭代，C++ 适合算法竞赛和性能密集场景。

## 📚 更多文档

- [`src/main/assets/wasm/README.md`](src/main/assets/wasm/README.md) - WASM 资源详细说明

---

**更新日期**: 2025-10-02  
**版本**: 1.1 (优化版)
