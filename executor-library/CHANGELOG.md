# Executor Library - 变更日志

## [1.0.2] - 2025-10-02

### ✨ 新增功能

**添加RS (Running Success) 状态码**
- 当未提供期望输出时，程序运行成功返回RS状态
- 与云端执行器保持一致的状态判断逻辑

### 🔧 改进

**优化状态判断逻辑**
- 区分"运行成功"(RS)和"答案正确"(AC)
- 改进输出对比逻辑：
  - `exitCode != 0` → RE（运行时错误）
  - `expectedOutput.isEmpty()` → RS（运行成功）
  - `actualOutput == expectedOutput` → AC（答案正确）
  - 其他 → WA（答案错误）

### 🐛 修复

- 清理app模块中残留的execution目录
- 修复编译错误（Unresolved reference 'WasmExecutorInterface'）
- 更新IOPanelFragment中的状态码引用

### 📝 文档更新

- 更新README中的状态码说明
- 添加判题模式和运行模式的使用示例
- 简化文档结构，减少md文件数量（6个→3个）

## [1.0.1] - 2025-10-02

### 🔧 目录结构优化

**重大改进：重组WASM资源目录结构**

#### 变更内容
- ✅ 按语言分类组织WASM资源
- ✅ 统一文件命名格式（所有执行器都使用 `xxx_executor.html`）
- ✅ 创建独立的语言子目录 (cpp/, python/, java/)
- ✅ 创建common目录存放共享JS资源
- ✅ 添加详细的目录结构文档 (STRUCTURE.md)

#### 新的目录结构
```
wasm/
├── cpp/          # C++ 执行器资源
├── python/       # Python 执行器资源
├── java/         # Java 执行器资源
└── common/       # 共享JS资源
```

#### 文件重命名
- `cpp_compiler_v2.html` → `cpp/cpp_executor.html`
- `python_executor.html` → `python/python_executor.html`
- `java_executor.html` → `java/java_executor.html`

#### 代码更新
- 更新所有Kotlin执行器中的资源路径
- 更新HTML文件中的JS资源引用路径
- 添加 `STRUCTURE.md` 详细文档

#### 优势
- 📁 更清晰的文件组织
- 🎯 易于定位和维护
- 📝 统一的命名规范
- 🔍 更好的可扩展性

## [1.0.0] - 2025-10-02

### 🎉 初始版本

首次从ACC IDE主项目中提取为独立library模块。

### ✨ 功能特性

#### 核心功能
- ✅ 统一的代码执行器接口 `ICodeExecutor`
- ✅ 执行器工厂模式 `ExecutorFactory`
- ✅ 本地WASM执行支持
- ✅ 云端GitHub Actions执行支持
- ✅ 执行结果标准化 `ExecutionResult`

#### 支持的语言
- ✅ C++ (通过wasm-clang)
- ✅ Python (通过Pyodide)
- ✅ Java (通过CheerpJ)

#### 实用工具
- ✅ ANSI颜色代码解析器 `AnsiTextParser`
- ✅ IO缓存管理器 `IOCacheManager`
- ✅ 完整的错误处理机制

### 📦 模块结构

```
executor-library/
├── src/main/
│   ├── java/com/acc_ide/executor/    # Kotlin源代码
│   ├── assets/wasm/                   # WASM资源 (~60MB)
│   └── AndroidManifest.xml
├── build.gradle                       # Library配置
├── README.md                          # 完整文档
├── INTEGRATION_GUIDE.md              # 集成指南
├── QUICK_START.md                    # 快速开始
└── CHANGELOG.md                      # 本文件
```

### 🔄 从主项目迁移的文件

#### Kotlin源文件
- `ICodeExecutor.kt` - 执行器接口
- `ExecutorFactory.kt` - 工厂类
- `LocalExecutor.kt` - 本地执行器
- `GitHubExecutor.kt` - 云端执行器
- `ExecutionResult.kt` - 结果数据类
- `AnsiTextParser.kt` - ANSI解析器
- `IOCacheManager.kt` - IO缓存管理
- `wasm/WasmExecutorInterface.kt` - WASM接口
- `wasm/WasmCppExecutor.kt` - C++执行器
- `wasm/WasmPythonExecutor.kt` - Python执行器
- `wasm/WasmJavaExecutor.kt` - Java执行器

#### 资源文件
- `cpp_compiler_v2.html` - C++编译器页面
- `python_executor.html` - Python执行器页面
- `java_executor.html` - Java执行器页面
- `clang` - Clang编译器 (30MB)
- `lld` - LLVM链接器 (19MB)
- `sysroot.tar` - 系统根目录 (9MB)
- `memfs` - 内存文件系统
- `shared.js`, `shared_web.js`, `web.js`, `worker.js` - WASM工具脚本

### 📝 包名变更

为避免与主项目冲突，使用新的包名结构：

- 旧包名: `com.acc_ide.execution`
- 新包名: `com.acc_ide.executor`

### 🔧 依赖项

```gradle
// AndroidX
implementation 'androidx.core:core-ktx:1.10.1'
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'androidx.preference:preference-ktx:1.2.1'

// Kotlin Coroutines
implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'

// Network
implementation 'com.squareup.okhttp3:okhttp:4.12.0'
```

### 📱 系统要求

- Android API 24+ (Android 7.0)
- Kotlin 1.9+
- 至少 200MB 存储空间

### 📄 文档

- ✅ README.md - 完整的API文档和使用说明
- ✅ INTEGRATION_GUIDE.md - 详细的集成指南
- ✅ QUICK_START.md - 5分钟快速上手
- ✅ CHANGELOG.md - 版本变更记录

### 🚀 使用示例

最简单的使用方式：

```kotlin
val executor = ExecutorFactory.createExecutor(context)

executor.executeCode(
    code = cppCode,
    language = "cpp",
    input = "5 10",
    expectedOutput = "15",
    onComplete = { result -> 
        println("Output: ${result.actualOutput}")
    },
    onError = { error ->
        println("Error: $error")
    }
)
```

### 🔐 权限

Library自动声明以下权限：
- `INTERNET` - 用于云端执行和Pyodide加载
- `ACCESS_NETWORK_STATE` - 检查网络状态

### ⚡ 性能

- C++ 编译时间: ~1-3秒
- Python 首次加载: ~10-30秒 (后续<1秒)
- Java 编译时间: ~2-5秒
- 运行时开销: 接近原生性能

### 🎯 已测试场景

- ✅ 算法竞赛代码执行
- ✅ 在线编程教学
- ✅ 代码性能测试
- ✅ 多语言混合项目

### 🐛 已知问题

1. **Pyodide首次加载慢**
   - 需要从CDN下载10-20MB资源
   - 建议提供预加载选项

2. **APK体积增大**
   - WASM资源约60MB
   - 建议使用AAB格式或按需下载

3. **WebView兼容性**
   - 部分旧设备可能不支持最新的WASM特性
   - 建议minSdkVersion 24+

### 🔮 未来计划

#### v1.1.0 (计划中)
- [ ] 支持自定义编译参数
- [ ] 添加代码执行队列
- [ ] 支持断点调试
- [ ] 添加内存和时间限制配置

#### v1.2.0 (计划中)
- [ ] 支持更多语言 (Rust, Go)
- [ ] 性能分析工具
- [ ] 代码覆盖率统计
- [ ] 离线Pyodide资源包

#### v2.0.0 (未来)
- [ ] 完全重写WASM执行引擎
- [ ] 支持图形化输出
- [ ] 实时协作编程
- [ ] 插件系统

### 🤝 贡献

欢迎贡献代码！提交Pull Request前请：

1. 确保代码通过所有测试
2. 更新相关文档
3. 遵循Kotlin代码规范
4. 添加必要的注释

### 📞 支持

遇到问题？

- 📖 查看 [完整文档](README.md)
- 🔧 查看 [集成指南](INTEGRATION_GUIDE.md)
- 💬 提交 [Issue](https://github.com/your-repo/acc_ide/issues)

### 📜 许可

本library作为ACC IDE项目的一部分，遵循项目的开源协议。

### 👏 致谢

感谢以下开源项目：

- [wasm-clang](https://github.com/binji/wasm-clang) - C++编译器
- [Pyodide](https://pyodide.org/) - Python解释器
- [CheerpJ](https://leaningtech.com/cheerpj/) - Java运行时
- [OkHttp](https://square.github.io/okhttp/) - HTTP客户端

---

**发布日期:** 2025-10-02  
**维护者:** ACC IDE Team  
**版本:** 1.0.0

