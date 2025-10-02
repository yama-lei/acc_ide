# WASM Executor Resources

WebAssembly 执行引擎资源文件，按语言分类组织。

## 📁 目录结构

```
wasm/
├── cpp/                    # C++ 执行器 (~60MB)
│   ├── lib/                # wasm-clang 库文件
│   │   ├── shared.js       # 核心 API
│   │   ├── shared_web.js
│   │   ├── web.js
│   │   └── worker.js
│   ├── cpp_executor.html   # 执行器页面
│   ├── cpp_executor.js     # 执行逻辑
│   ├── clang               # Clang 编译器 (30MB)
│   ├── lld                 # LLVM 链接器 (19MB)
│   ├── memfs               # 内存文件系统 (337KB)
│   └── sysroot.tar         # C++ 标准库 (9MB)
│
├── python/                 # Python 执行器
│   ├── python_executor.html
│   └── python_executor.js
│
└── java/                   # Java 执行器
    └── java_executor.html
```

## 📊 资源大小

| 类型 | 文件 | 大小 | 说明 |
|------|------|------|------|
| **C++** | clang | 30MB | Clang/LLVM 编译器 WASM 版 |
| | lld | 19MB | LLVM 链接器 |
| | sysroot.tar | 9MB | C++ 标准库头文件 |
| | memfs | 337KB | 内存文件系统 |
| | lib/*.js | 36KB | wasm-clang 库文件 |
| **Python** | *.html, *.js | <10KB | Pyodide 执行器（运行时从 CDN 加载） |
| **Java** | *.html | <10KB | Java 执行器 |
| **总计** | | **~60MB** | |

## 🎯 使用方式

### 在 Kotlin 中引用

```kotlin
webView.loadUrl("file:///android_asset/wasm/cpp/cpp_executor.html")
webView.loadUrl("file:///android_asset/wasm/python/python_executor.html")
webView.loadUrl("file:///android_asset/wasm/java/java_executor.html")
```

### HTML 结构

每个执行器包含：
- `*.html` - WebView 容器页面
- `*.js` - 执行器业务逻辑（如果需要）

C++ 执行器引用本地库：
```html
<script src="lib/shared.js"></script>
<script src="cpp_executor.js"></script>
```

Python 执行器引用 CDN：
```html
<script src="https://cdn.jsdelivr.net/pyodide/v0.24.1/full/pyodide.js"></script>
<script src="python_executor.js"></script>
```

## ⚡ 性能特性

### C++
- ✅ 完整 C++17 支持
- ✅ 编译时间: 1-3 秒
- ✅ 运行性能: 接近原生
- ✅ 完整 STL 支持

### Python
- ✅ Python 3.11
- ⚠️ 首次加载: 10-30 秒（下载 Pyodide）
- ✅ 后续运行: <1 秒（缓存）
- ✅ 支持 NumPy, Pandas 等库

### Java
- ✅ Java 基本语法
- ✅ 编译时间: <1 秒
- ⚠️ 部分特性不支持

## 🔧 扩展指南

### 添加新语言

1. 创建新目录（如 `rust/`）
2. 添加 `rust_executor.html` 和 `rust_executor.js`
3. 添加必要的 WASM 资源
4. 创建对应的 Kotlin 执行器类 `WasmRustExecutor.kt`

### 更新执行器

1. 修改对应目录下的 HTML/JS 文件
2. 测试所有功能
3. 更新文档

## 📚 相关链接

- [wasm-clang](https://github.com/binji/wasm-clang) - C++ 编译器
- [Pyodide](https://pyodide.org/) - Python 解释器
- [WebAssembly](https://webassembly.org/) - WASM 标准

---

**更新日期**: 2025-10-02
