# 代码执行引擎架构说明

## 📋 概述

这是ACC IDE的代码执行模块，采用模块化设计，支持多种执行方式（云端/本地）。

## 🏗️ 架构设计

### 核心组件

```
execution/
├── ICodeExecutor.kt           # 执行器接口
├── ExecutionResult.kt          # 执行结果数据类
├── ExecutorFactory.kt          # 执行器工厂
├── GitHubExecutor.kt           # GitHub Actions云端执行器
├── LocalExecutor.kt            # 本地执行器（占位）
├── AnsiTextParser.kt           # ANSI颜色代码解析器
└── IOCacheManager.kt           # IO缓存管理器
```

### 设计模式

1. **策略模式（Strategy Pattern）**
   - `ICodeExecutor` 定义执行接口
   - `GitHubExecutor` 和 `LocalExecutor` 实现不同策略
   - 运行时可切换执行方式

2. **工厂模式（Factory Pattern）**
   - `ExecutorFactory` 根据用户设置创建执行器
   - 封装创建逻辑，便于扩展

3. **单例模式（Singleton Pattern）**
   - `IOCacheManager` 管理全局IO缓存
   - `AnsiTextParser` 提供工具方法

## 🚀 使用方式

### 基本使用

```kotlin
// 在Fragment中使用
val executor = ExecutorFactory.createExecutor(requireContext())

executor.executeCode(
    code = sourceCode,
    language = "cpp",
    input = "1 2 3",
    expectedOutput = "6",
    onProgress = { message -> 
        // 更新进度
        outputText.setText(message)
    },
    onComplete = { result ->
        // 执行完成
        displayResult(result)
    },
    onError = { error ->
        // 处理错误
        showError(error)
    }
)
```

### 切换执行模式

用户可以在设置界面切换执行模式：

1. **云端模式（Cloud）**：使用GitHub Actions
2. **本地模式（Local）**：本地编译执行（待实现）

```kotlin
// 获取当前模式
val mode = ExecutorFactory.getExecutionMode(context)

// 设置模式
ExecutorFactory.setExecutionMode(context, ExecutorFactory.MODE_LOCAL)
```

## 📊 执行流程

### GitHub云端执行流程

```
用户点击运行
    ↓
GitHubExecutor.executeCode()
    ↓
触发GitHub Workflow
    ↓
轮询执行状态（2秒/次）
    ↓
下载Artifact结果
    ↓
解析result.json
    ↓
返回ExecutionResult
    ↓
更新UI显示结果
```

### 本地执行流程（规划中）

```
用户点击运行
    ↓
LocalExecutor.executeCode()
    ↓
编写临时源文件
    ↓
调用编译器（TCC/ECJ/Chaquopy）
    ↓
执行编译后的程序
    ↓
捕获输出和错误
    ↓
返回ExecutionResult
    ↓
更新UI显示结果
```

## 🔧 扩展指南

### 添加新的执行器

1. 实现 `ICodeExecutor` 接口：

```kotlin
class MyCustomExecutor(private val context: Context) : ICodeExecutor {
    override fun executeCode(
        code: String,
        language: String,
        input: String,
        expectedOutput: String,
        onProgress: ((String) -> Unit)?,
        onComplete: (ExecutionResult) -> Unit,
        onError: (String) -> Unit
    ) {
        // 实现执行逻辑
    }
    
    override fun cancelExecution() {
        // 实现取消逻辑
    }
    
    override fun isExecuting(): Boolean {
        // 返回执行状态
    }
    
    override fun getExecutorName(): String = "My Custom Executor"
}
```

2. 在 `ExecutorFactory` 中注册：

```kotlin
fun createExecutor(context: Context): ICodeExecutor {
    return when (mode) {
        MODE_LOCAL -> LocalExecutor(context)
        MODE_CUSTOM -> MyCustomExecutor(context)
        else -> GitHubExecutor(context)
    }
}
```

### 添加新的语言支持

在语言映射中添加：

```kotlin
// GitHubExecutor.kt
private val LANGUAGE_MAP = mapOf(
    "cpp" to "cpp",
    "python" to "py",
    "java" to "java",
    "rust" to "rs"  // 新增语言
)
```

## 📝 执行结果格式

### ExecutionResult 结构

```kotlin
data class ExecutionResult(
    val status: String,         // 状态码：AC, WA, CE, TLE, MLE, RE
    val actualOutput: String,   // 实际输出
    val executionTime: Int,     // 执行时间（毫秒）
    val errorMessage: String    // 错误信息
)
```

### 状态码说明

| 状态码 | 含义 | 说明 |
|-------|------|------|
| AC    | Accepted | 正确答案 |
| WA    | Wrong Answer | 答案错误 |
| CE    | Compile Error | 编译错误 |
| TLE   | Time Limit Exceeded | 超时 |
| MLE   | Memory Limit Exceeded | 内存超限 |
| RE    | Runtime Error | 运行时错误 |
| RS    | Running | 运行中 |

## 🎨 ANSI颜色支持

`AnsiTextParser` 可以将终端ANSI颜色代码转换为Android样式：

```kotlin
val coloredText = AnsiTextParser.parseAnsiText(
    "\u001B[31mError\u001B[0m: undefined reference"
)
textView.setText(coloredText)
```

支持的ANSI代码：
- 前景色：30-37（标准）、90-97（高亮）
- 粗体：1
- 重置：0

## 🔒 IO缓存机制

`IOCacheManager` 为每个文件缓存IO数据：

```kotlin
// 保存
IOCacheManager.save(fileName, IOInstance(
    input = "1 2 3",
    actualOutput = "6",
    expectedOutput = "6",
    status = "AC",
    executionTime = 50
))

// 读取
val cached = IOCacheManager.get(fileName)

// 更新
IOCacheManager.update(
    fileName = fileName,
    status = "AC",
    executionTime = 50
)
```

## 🚧 待实现功能

### LocalExecutor 本地执行器

**C++支持方案：**
1. ✅ 集成TCC编译器.so库（轻量级）
2. ⏳ 集成Clang工具链（完整支持）
3. ⏳ 使用Cling C++解释器

**Python支持方案：**
- ✅ 集成Chaquopy（官方推荐）

**Java支持方案：**
- ✅ 集成ECJ（Eclipse Compiler for Java）
- ✅ 使用DexClassLoader动态加载

### 其他改进
- [ ] 添加执行队列（支持多任务）
- [ ] 添加执行历史记录
- [ ] 支持自定义编译参数
- [ ] 支持调试功能
- [ ] 添加性能分析工具

## 📚 参考资料

- [GitHub Actions API](https://docs.github.com/en/rest/actions)
- [TCC - Tiny C Compiler](https://bellard.org/tcc/)
- [Chaquopy - Python for Android](https://chaquo.com/chaquopy/)
- [ECJ - Eclipse Compiler for Java](https://www.eclipse.org/jdt/core/)

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork本项目
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

---

**最后更新：** 2025-10-01  
**维护者：** ACC IDE Team

