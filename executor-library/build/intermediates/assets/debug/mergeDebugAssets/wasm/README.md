# WASM Executor Resources

WebAssembly执行引擎的资源文件，按语言分类组织。

## 📁 目录结构

```
wasm/
├── cpp/              # C++ 执行器资源 (~60MB)
│   ├── cpp_executor.html
│   ├── clang         # Clang编译器 (30MB)
│   ├── lld           # LLVM链接器 (19MB)
│   ├── memfs         # 内存文件系统
│   └── sysroot.tar   # C++标准库 (9MB)
│
├── python/           # Python 执行器资源
│   └── python_executor.html
│
├── java/             # Java 执行器资源
│   └── java_executor.html
│
└── common/           # 共享JS资源
    ├── shared.js
    ├── shared_web.js
    ├── web.js
    └── worker.js
```

## 📊 资源说明

| 语言 | 文件 | 大小 | 说明 |
|------|------|------|------|
| **C++** | clang | 30MB | Clang/LLVM编译器WASM版 |
| | lld | 19MB | LLVM链接器 |
| | sysroot.tar | 9MB | C++标准库头文件 |
| | memfs | 337KB | 内存文件系统 |
| | cpp_executor.html | 10KB | 执行器页面 |
| **Python** | python_executor.html | 5KB | Pyodide执行器页面 |
| **Java** | java_executor.html | 10KB | Java执行器页面 |
| **共享** | common/*.js | 36KB | 共享工具库 |
| **总计** | | **~60MB** | |

## 🎯 使用方式

### 在Kotlin中引用

```kotlin
// C++执行器
webView.loadUrl("file:///android_asset/wasm/cpp/cpp_executor.html")

// Python执行器
webView.loadUrl("file:///android_asset/wasm/python/python_executor.html")

// Java执行器
webView.loadUrl("file:///android_asset/wasm/java/java_executor.html")
```

### 在HTML中引用共享资源

```html
<!-- cpp_executor.html -->
<script src="../common/shared.js"></script>

<!-- python_executor.html -->
<script src="../common/shared_web.js"></script>
```

## ⚡ 性能特性

### C++ (cpp/)
- ✅ 完整的C++17支持
- ✅ 编译时间: 1-3秒
- ✅ 运行性能: 接近原生
- ✅ 完整的STL支持

### Python (python/)
- ✅ Python 3.11
- ⚠️ 首次加载: 10-30秒（下载Pyodide）
- ✅ 后续运行: <1秒（使用缓存）
- ✅ 支持NumPy, Pandas等科学计算库

### Java (java/)
- ✅ Java基本语法
- ✅ 编译时间: <1秒
- ✅ 常用API支持
- ⚠️ 部分Java特性不支持

## 🔧 扩展指南

### 添加新语言

1. 在 `wasm/` 下创建新目录（如 `rust/`）
2. 添加 `rust_executor.html` 执行器页面
3. 添加必要的WASM资源
4. 创建对应的Kotlin执行器类

### 更新现有执行器

1. 修改对应目录下的HTML文件
2. 测试所有功能
3. 更新CHANGELOG

## 📚 相关链接

- [wasm-clang项目](https://github.com/binji/wasm-clang) - C++编译器
- [Pyodide文档](https://pyodide.org/) - Python解释器
- [WebAssembly标准](https://webassembly.org/)

---

**更新日期:** 2025-10-02  
**维护者:** ACC IDE Team
