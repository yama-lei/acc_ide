# 🚧 wasm-clang 完整实现 TODO

参考项目：[binji/wasm-clang](https://github.com/binji/wasm-clang)

## 📊 当前状态

### ✅ 已完成
- [x] 文件下载 (57.58 MB)
- [x] 文件打包到 APK
- [x] XMLHttpRequest 加载 assets 文件
- [x] 文件成功加载到内存
  - Clang: 29.77 MB ✅
  - LLD: 18.59 MB ✅
  - Memfs: 0.33 MB ✅
  - Sysroot: 8.87 MB ✅

### ❌ 当前错误
```
TypeError: WebAssembly.instantiate(): Import #0 "wasi_unstable": module is not an object or function
```

**问题**: 我们的简化 WASI 实现不够，clang.wasm 需要完整的 WASI 运行时。

## 🎯 实现路线图

### 阶段 1: 复用原项目的 JavaScript 文件 【推荐优先】

#### TODO 1.1: 下载并集成原项目的 JS 文件
- [ ] 下载 `shared.js` - 已下载 ✅
- [ ] 下载 `worker.js` - 已下载 ✅  
- [ ] 下载 `shared_web.js`
- [ ] 下载 `web.js` (主要的编译逻辑)
- [ ] 研究这些文件的依赖关系

**参考**:
- https://github.com/binji/wasm-clang/blob/master/shared.js
- https://github.com/binji/wasm-clang/blob/master/shared_web.js
- https://github.com/binji/wasm-clang/blob/master/web.js
- https://github.com/binji/wasm-clang/blob/master/worker.js

#### TODO 1.2: 理解原项目架构
- [ ] 阅读 `index.html` 源码
- [ ] 理解 Service Worker 的作用
- [ ] 理解 Web Worker 的编译流程
- [ ] 画出数据流图

**关键文件**:
```
index.html          → 用户界面
web.js              → 主逻辑，与 worker 通信
worker.js           → 编译 worker，运行 clang/lld
shared.js           → WASI 实现和文件系统
shared_web.js       → Web 特定工具
service_worker.js   → 缓存和离线支持
```

#### TODO 1.3: 适配到 Android WebView
- [ ] 修改 HTML 使用原项目的 JS
- [ ] 处理 Service Worker（WebView 可能不支持）
- [ ] 处理 Web Worker（WebView 支持）
- [ ] 添加 Android Bridge 回调

---

### 阶段 2: 实现完整的 WASI 运行时

#### TODO 2.1: 使用原项目的 memfs.wasm
- [ ] 研究 memfs.wasm 的接口
- [ ] 理解如何初始化 memfs
- [ ] 理解如何挂载文件系统
- [ ] 测试 memfs 加载

**参考**: 原项目使用自己编译的 memfs，提供完整的文件系统抽象
- https://github.com/binji/llvm-project/tree/master/binji (memfs 源码)

#### TODO 2.2: 实现 WASI 系统调用
- [ ] 实现 `wasi_unstable` 接口（旧版本）
- [ ] 实现 `wasi_snapshot_preview1` 接口（新版本）
- [ ] 文件描述符管理
- [ ] stdin/stdout/stderr 重定向
- [ ] 环境变量和参数传递

**关键系统调用**:
```javascript
// 必须实现的 WASI 调用
- fd_write        // 输出
- fd_read         // 输入
- fd_close        // 关闭文件
- path_open       // 打开文件
- path_filestat_get  // 获取文件信息
- proc_exit       // 进程退出
- random_get      // 随机数
- clock_time_get  // 时间
```

#### TODO 2.3: 实现虚拟文件系统
- [ ] 解压 sysroot.tar
- [ ] 挂载 C++ 标准库
- [ ] 实现内存中的文件读写
- [ ] 支持目录操作

**需要的库**:
- js-untar: https://github.com/InvokIT/js-untar (tar 解压)
- 或使用原项目的 tar 解析代码

---

### 阶段 3: 编译流程实现

#### TODO 3.1: 初始化 Clang 编译器
- [ ] 加载 clang.wasm
- [ ] 初始化 WASI 环境
- [ ] 挂载 sysroot
- [ ] 测试简单的编译命令

**测试命令**:
```bash
clang -o hello.o -c hello.cpp --target=wasm32-wasi -std=c++17
```

#### TODO 3.2: 实现编译管道
- [ ] 写入源代码到虚拟文件系统
- [ ] 调用 clang 编译 .cpp → .o
- [ ] 捕获编译错误
- [ ] 读取目标文件

#### TODO 3.3: 初始化 LLD 链接器
- [ ] 加载 lld.wasm
- [ ] 初始化 WASI 环境
- [ ] 链接 C++ 标准库

**测试命令**:
```bash
lld -o program.wasm hello.o -lc -lc++
```

#### TODO 3.4: 链接和生成可执行文件
- [ ] 调用 lld 链接 .o → .wasm
- [ ] 链接 C++ 标准库
- [ ] 处理链接错误
- [ ] 生成最终的 wasm 程序

---

### 阶段 4: 运行编译后的程序

#### TODO 4.1: 加载用户程序
- [ ] 读取编译后的 .wasm
- [ ] 创建新的 WASI 环境
- [ ] 准备 stdin 输入

#### TODO 4.2: 执行并捕获输出
- [ ] 运行 wasm 程序
- [ ] 捕获 stdout
- [ ] 捕获 stderr
- [ ] 处理运行时错误

#### TODO 4.3: 返回结果到 Android
- [ ] 通过 JavaScript Bridge 返回输出
- [ ] 返回退出码
- [ ] 返回错误信息

---

### 阶段 5: 错误处理和优化

#### TODO 5.1: 友好的错误提示
- [ ] 解析编译错误
- [ ] 高亮错误行号
- [ ] 提供修复建议

#### TODO 5.2: 性能优化
- [ ] 缓存编译结果
- [ ] 增量编译
- [ ] 并行处理

#### TODO 5.3: 用户体验改进
- [ ] 显示编译进度
- [ ] 添加超时保护
- [ ] 内存限制

---

## 🔍 关键参考文件

### 必读文件（按优先级）

#### 1. shared.js - WASI 核心实现
```
https://github.com/binji/wasm-clang/blob/master/shared.js
```
**包含**:
- WASI 系统调用实现
- 文件系统抽象
- 进程管理
- 内存管理

#### 2. web.js - 主逻辑
```
https://github.com/binji/wasm-clang/blob/master/web.js
```
**包含**:
- 编译器初始化
- Worker 通信
- UI 更新

#### 3. worker.js - 编译 Worker
```
https://github.com/binji/wasm-clang/blob/master/worker.js
```
**包含**:
- Clang 调用
- LLD 调用
- 程序执行

#### 4. index.html - 用户界面
```
https://github.com/binji/wasm-clang/blob/master/index.html
```
**包含**:
- HTML 结构
- 编辑器集成
- 示例代码

### 辅助文件

#### 5. shared_web.js - Web 工具
```
https://github.com/binji/wasm-clang/blob/master/shared_web.js
```

#### 6. service_worker.js - 离线支持
```
https://github.com/binji/wasm-clang/blob/master/service_worker.js
```

---

## 📝 实现策略

### 方案 A: 完全复用原项目代码 ⭐ 推荐

**优点**:
- 已经过验证，稳定可靠
- 节省大量开发时间
- 完整的功能支持

**步骤**:
1. 下载所有 JS 文件
2. 创建新的 `cpp_compiler_full.html`
3. 引入原项目的 JS
4. 适配 Android WebView
5. 添加 Android Bridge

**预计时间**: 1-2 天

### 方案 B: 从零实现 WASI

**优点**:
- 学习深入理解
- 代码完全可控

**缺点**:
- 需要大量时间
- 容易出错
- 需要深入理解 WASI 规范

**预计时间**: 1-2 周

### 方案 C: 混合方案

**优点**:
- 复用核心 WASI 实现
- 自定义 UI 和流程

**步骤**:
1. 复用 `shared.js` 的 WASI 实现
2. 复用 `worker.js` 的编译逻辑
3. 自己实现 UI 和 Android 集成

**预计时间**: 3-5 天

---

## 🚀 快速开始（方案 A）

### 第一步：下载原项目的所有 JS 文件

```powershell
# 在 wasmClang-build 目录添加到下载脚本
$jsFiles = @(
    "shared.js",
    "shared_web.js", 
    "web.js",
    "worker.js"
)
```

### 第二步：创建新的 HTML

基于 `index.html` 创建 Android 版本：
- 移除 Service Worker（WebView 不支持）
- 保留 Web Worker
- 添加 Android Bridge
- 简化 UI

### 第三步：测试基础功能

测试代码：
```cpp
#include <iostream>
int main() {
    std::cout << "Hello from wasm-clang!" << std::endl;
    return 0;
}
```

---

## 📚 学习资源

### WASI 规范
- https://github.com/WebAssembly/WASI
- https://wasi.dev/

### 原作者的实现笔记
- https://gist.github.com/binji/b7541f9740c21d7c6dac95cbc9ea6fca

### CppCon 演讲
- 搜索 "CppCon wasm-clang binji"

### WebAssembly 文档
- https://webassembly.org/
- https://developer.mozilla.org/en-US/docs/WebAssembly

---

## 🎯 下一步行动

### 立即开始（今天）

#### 任务 1: 下载并研究原项目 JS 文件
```bash
cd wasmClang-build
# 修改下载脚本，添加 shared_web.js 和 web.js
```

#### 任务 2: 阅读 shared.js 源码
重点关注：
- WASI 接口实现
- 文件系统抽象
- 如何调用 clang/lld

#### 任务 3: 创建测试 HTML
- 复制 index.html 内容
- 移除不需要的部分
- 添加 Android Bridge

### 本周目标

- [ ] 理解原项目架构
- [ ] 成功加载并运行一个简单的编译示例
- [ ] 在 Android WebView 中显示结果

### 本月目标

- [ ] 完整的编译流程
- [ ] STL 支持
- [ ] 友好的错误提示
- [ ] 性能优化

---

## 🔄 迭代计划

### Sprint 1 (1-2 天): 原型验证
- 下载原项目 JS
- 创建基础 HTML
- 测试能否调用 clang

### Sprint 2 (2-3 天): 编译流程
- 实现完整编译
- 支持 STL
- 错误处理

### Sprint 3 (1-2 天): Android 集成
- WebView 适配
- JavaScript Bridge
- UI 集成

### Sprint 4 (1 天): 测试和优化
- 各种测试用例
- 性能优化
- Bug 修复

---

## ⚠️ 已知挑战

1. **Service Worker**: WebView 可能不支持，需要替代方案
2. **内存限制**: Android 设备内存有限，需要优化
3. **首次加载**: 57MB 文件需要时间，需要进度提示
4. **错误提示**: 需要将编译器错误转换为友好格式

---

## 💡 备选方案

如果 wasm-clang 太复杂，可以考虑：

### 备选 1: 云端编译 API
- Wandbox API
- Compiler Explorer API
- 自建编译服务器

**优点**: 简单、快速
**缺点**: 需要网络

### 备选 2: WebAssembly Studio
- https://webassembly.studio/
- 提供在线 C++ 编译

### 备选 3: Emscripten 简化版
- 只支持部分 C++ 功能
- 体积更小

---

## 📊 成功标准

- [x] 文件成功加载
- [ ] 能编译 Hello World
- [ ] 能编译使用 vector 的代码
- [ ] 能编译使用 map/set 的代码
- [ ] 能运行并输出结果
- [ ] 编译错误友好提示
- [ ] 性能可接受（编译 < 5秒）

---

**总结**: 推荐使用方案 A（完全复用原项目代码），这是最快最可靠的方式。下一步立即下载并研究 `shared.js` 和 `web.js`。

**预计总时间**: 3-5 天可以完成基础功能，1 周可以完成优化。

