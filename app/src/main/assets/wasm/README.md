# WASM C++ 编译器文件说明

## 📁 当前目录内容

- ✅ `cpp_compiler.html` - WebView 加载的主页面（已存在）

## ⚠️ 缺少的文件

为了让 C++ 编译器工作，你需要下载以下文件到这个目录：

| 文件 | 大小 | 状态 | 说明 |
|------|------|------|------|
| `clang` | ~25MB | ❌ 需要下载 | Clang C++ 编译器 |
| `lld` | ~15MB | ❌ 需要下载 | LLVM 链接器 |
| `memfs` | ~1MB | ❌ 需要下载 | 内存文件系统 |
| `sysroot.tar` | ~10MB | ❌ 需要下载 | C++ 标准库 |
| `shared.js` | ~50KB | ❌ 需要下载 | 共享工具 |
| `worker.js` | ~20KB | ❌ 需要下载 | Web Worker |

**总计**: 约 50MB

## 🚀 如何下载

### 方法 1: 自动脚本（推荐）

在项目根目录运行：

**Windows:**
```powershell
cd treesitter-build
.\Download-WasmClang.ps1
```

**Linux/Mac:**
```bash
cd treesitter-build
chmod +x Download-WasmClang.sh
./Download-WasmClang.sh
```

### 方法 2: 手动下载

访问 https://github.com/binji/wasm-clang 并下载以下文件到当前目录：

1. https://raw.githubusercontent.com/binji/wasm-clang/master/clang
2. https://raw.githubusercontent.com/binji/wasm-clang/master/lld
3. https://raw.githubusercontent.com/binji/wasm-clang/master/memfs
4. https://raw.githubusercontent.com/binji/wasm-clang/master/sysroot.tar
5. https://raw.githubusercontent.com/binji/wasm-clang/master/shared.js
6. https://raw.githubusercontent.com/binji/wasm-clang/master/worker.js

### 方法 3: 使用在线 CDN（不推荐）

如果不下载文件，程序会尝试从 GitHub CDN 下载，但：
- ⚠️ 需要网络连接
- ⚠️ 首次运行会很慢（需要下载 50MB）
- ⚠️ 可能被防火墙拦截

## ✅ 验证安装

下载完成后，此目录应包含：

```
app/src/main/assets/wasm/
├── cpp_compiler.html  ✅
├── clang              ⬇️ 需要下载
├── lld                ⬇️ 需要下载
├── memfs              ⬇️ 需要下载
├── sysroot.tar        ⬇️ 需要下载
├── shared.js          ⬇️ 需要下载
└── worker.js          ⬇️ 需要下载
```

## 🔧 故障排除

### 问题: "Fetch API cannot load file://..."

**原因**: 这是正常的，已经修复。程序会使用 XMLHttpRequest 替代 fetch。

### 问题: "Failed to fetch"

**原因**: 没有下载文件到 assets，且无法从 GitHub 下载。

**解决**:
1. 运行下载脚本下载文件
2. 或手动下载文件到此目录
3. 重新构建 APK

### 问题: 文件太大

**原因**: wasm-clang 是完整的 C++ 编译器，需要 50MB。

**解决方案**:
- 如果不需要 STL，可以考虑其他轻量方案
- 如果需要完整 C++ 支持，这是最小的离线方案

## 📝 重要说明

1. **不要提交到 Git**: 这些文件很大，建议添加到 `.gitignore`
2. **每个开发者需要下载**: 运行下载脚本获取文件
3. **打包到 APK**: 文件会自动打包到 APK 中，用户无需额外下载

## 🎯 下一步

1. 运行下载脚本获取文件
2. 构建 APK: `./gradlew assembleDebug`
3. 测试 C++ 编译功能

查看详细文档:
- `../../../../../../CPP_WASM_IMPLEMENTATION.md` - 完整技术文档
- `../../../../../../WASM_CLANG_SETUP.md` - 快速开始指南
- `../../../../../../treesitter-build/README_WASM_CLANG.md` - 下载指南

