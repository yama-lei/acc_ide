# 📋 wasm-clang 快速参考

## ✅ 当前状态（2025-10-02）

### 已完成
- ✅ 所有文件已下载（57.66 MB）
  - clang (29.77 MB)
  - lld (18.59 MB)
  - memfs (0.33 MB)
  - sysroot.tar (8.87 MB)
  - shared.js (0.02 MB) ⭐ 核心
  - shared_web.js (0.01 MB)
  - web.js (0 MB)
  - worker.js (0 MB)

### 当前问题
```
❌ TypeError: WebAssembly.instantiate(): 
   Import #0 "wasi_unstable": module is not an object or function
```

**原因**: 简化的 WASI 实现不够  
**解决**: 使用 `shared.js` 中的完整 WASI

## 📚 关键文档

| 文档 | 内容 | 优先级 |
|------|------|--------|
| `WASM_CLANG_TODO.md` | 完整的实现计划 | ⭐⭐⭐ |
| `NEXT_STEPS.md` | 本周具体行动 | ⭐⭐⭐ |
| `QUICK_REFERENCE.md` | 本文件 | ⭐⭐ |
| `CPP_WASM_IMPLEMENTATION.md` | 技术方案 | ⭐ |
| `TEST_CPP_COMPILER.md` | 测试用例 | ⭐ |

## 🎯 下一步行动（按顺序）

### 1. 阅读源码（2-3 小时）
```bash
# 最重要的文件
code app/src/main/assets/wasm/shared.js
```

**重点关注**:
- Line ~100: WASI 接口定义
- Line ~500: 文件系统实现
- Line ~1000: WebAssembly 加载

### 2. 创建测试 HTML（1 小时）
```bash
# 创建新文件
touch app/src/main/assets/wasm/cpp_compiler_v2.html
```

**模板**:
```html
<!DOCTYPE html>
<html>
<head>
    <script src="shared.js"></script>
</head>
<body>
    <script>
        // TODO: 初始化编译器
    </script>
</body>
</html>
```

### 3. 测试基础功能（1 小时）
```cpp
// Hello World
#include <iostream>
int main() {
    std::cout << "Hello!" << std::endl;
    return 0;
}
```

## 🔍 关键代码片段

### shared.js 关键函数

```javascript
// 1. WASI 初始化
class WASI {
    constructor(options) {
        this.wasiImport = {
            wasi_unstable: { /* 系统调用 */ },
            wasi_snapshot_preview1: { /* 系统调用 */ }
        };
    }
}

// 2. WebAssembly 加载
async function instantiateWasm(bytes, imports) {
    return await WebAssembly.instantiate(bytes, imports);
}

// 3. 文件系统
class MemFS {
    constructor() {
        this.files = new Map();
    }
    
    writeFile(path, data) { /* ... */ }
    readFile(path) { /* ... */ }
}

// 4. 编译命令
async function compileCode(code) {
    // 写入源文件
    memfs.writeFile('/tmp/main.cpp', code);
    
    // 调用 clang
    await runWasm(clang, ['clang', '-c', '/tmp/main.cpp', '-o', '/tmp/main.o']);
    
    // 调用 lld
    await runWasm(lld, ['lld', '/tmp/main.o', '-o', '/tmp/a.wasm']);
    
    // 读取结果
    return memfs.readFile('/tmp/a.wasm');
}
```

## 📊 架构图

```
Android App
    ↓
WebView
    ↓
cpp_compiler_v2.html
    ↓
shared.js (WASI 实现)
    ↓
┌─────────────┬─────────────┬─────────────┐
│  clang.wasm │  lld.wasm   │  memfs.wasm │
└─────────────┴─────────────┴─────────────┘
    ↓
sysroot.tar (C++ 标准库)
    ↓
编译后的 .wasm 程序
    ↓
运行并返回结果
```

## 🔧 调试命令

### 桌面浏览器测试
```bash
cd app/src/main/assets/wasm
python -m http.server 8000
# 访问 http://localhost:8000
```

### Android 调试
```bash
# 查看日志
adb logcat | grep -E "chromium|WasmCppExecutor"

# 启用 WebView 调试
# Chrome: chrome://inspect
```

## ⚡ 快速开始模板

### 最小可行实现

```javascript
// cpp_compiler_v2.html
let clangBytes, lldBytes, memfsBytes, sysrootBytes;

// 1. 加载文件
async function init() {
    clangBytes = await loadFile('clang');
    lldBytes = await loadFile('lld');
    memfsBytes = await loadFile('memfs');
    sysrootBytes = await loadFile('sysroot.tar');
}

// 2. 使用 shared.js 的 WASI
const wasi = new WASI({
    args: [],
    env: {},
    preopens: { '/': memfs }
});

// 3. 实例化 clang
const clang = await WebAssembly.instantiate(clangBytes, {
    ...wasi.getImports()
});

// 4. 编译代码
function compile(code) {
    // 使用 shared.js 提供的函数
    return compileAndRun(code);
}
```

## 🎯 本周目标

### 今天（Day 1）
- [ ] ✅ 下载所有 JS 文件
- [ ] 阅读 shared.js（2-3 小时）
- [ ] 理解 WASI 接口
- [ ] 画出架构图

### 明天（Day 2）
- [ ] 创建 cpp_compiler_v2.html
- [ ] 实现基础加载
- [ ] 测试 WebAssembly 实例化

### Day 3
- [ ] 实现编译流程
- [ ] 测试 Hello World

### Day 4-5
- [ ] 添加 Android Bridge
- [ ] 测试 STL 支持
- [ ] 错误处理

## 📞 救命锦囊

### 如果 shared.js 太复杂
1. 先用在线 Demo 测试理解流程
2. 只关注核心函数，忽略细节
3. 从简单示例开始

### 如果 WebAssembly 加载失败
1. 检查 imports 对象是否正确
2. 确认 WASI 接口完整
3. 查看 Console 错误详情

### 如果编译失败
1. 确认 sysroot.tar 已解压
2. 检查编译参数
3. 查看编译器输出

## 🔗 重要链接

| 资源 | 链接 |
|------|------|
| 原项目 | https://github.com/binji/wasm-clang |
| 在线 Demo | https://binji.github.io/wasm-clang |
| 实现笔记 | https://gist.github.com/binji/b7541f9740c21d7c6dac95cbc9ea6fca |
| WASI 规范 | https://github.com/WebAssembly/WASI |
| WebAssembly | https://webassembly.org/ |

## 💡 小贴士

1. **不要重新发明轮子** - 直接用 shared.js
2. **先桌面后移动** - 浏览器调试更方便
3. **增量开发** - 一次一个功能
4. **多看日志** - console.log 是好朋友
5. **参考 Demo** - 在线版本是最好的文档

## 📈 进度追踪

```
[██░░░░░░░░] 20% - 文件下载完成
[░░░░░░░░░░]  0% - 源码理解
[░░░░░░░░░░]  0% - 原型实现
[░░░░░░░░░░]  0% - Android 集成
[░░░░░░░░░░]  0% - 测试优化
```

**预计完成**: 5-7 天（基础功能）

---

**记住**: 
- 📚 先读 shared.js
- 🎯 一次一个功能
- 🔍 多看原项目代码
- 🐛 遇到问题先看 Console

**开始吧！** 🚀

