# wasm-clang 下载和集成指南

## 📥 下载文件

### 快速开始

运行下载脚本自动下载所有需要的文件：

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

### 手动下载

如果自动脚本失败，可以手动下载：

1. 访问 https://github.com/binji/wasm-clang
2. 下载以下文件到 `app/src/main/assets/wasm/`:

| 文件 | 下载链接 | 大小 |
|------|---------|------|
| clang | [下载](https://raw.githubusercontent.com/binji/wasm-clang/master/clang) | ~25MB |
| lld | [下载](https://raw.githubusercontent.com/binji/wasm-clang/master/lld) | ~15MB |
| memfs | [下载](https://raw.githubusercontent.com/binji/wasm-clang/master/memfs) | ~1MB |
| sysroot.tar | [下载](https://raw.githubusercontent.com/binji/wasm-clang/master/sysroot.tar) | ~10MB |
| shared.js | [下载](https://raw.githubusercontent.com/binji/wasm-clang/master/shared.js) | ~50KB |
| worker.js | [下载](https://raw.githubusercontent.com/binji/wasm-clang/master/worker.js) | ~20KB |

## 📁 文件放置位置

下载后的文件应该放在：

```
app/src/main/assets/wasm/
├── cpp_compiler.html       # ✅ 已存在
├── clang                   # ⬇️ 需要下载
├── lld                     # ⬇️ 需要下载
├── memfs                   # ⬇️ 需要下载
├── sysroot.tar            # ⬇️ 需要下载
├── shared.js              # ⬇️ 需要下载
└── worker.js              # ⬇️ 需要下载
```

## ✅ 验证安装

下载完成后，检查文件：

**Windows:**
```powershell
dir app\src\main\assets\wasm\
```

**Linux/Mac:**
```bash
ls -lh app/src/main/assets/wasm/
```

应该看到：
- clang (~25MB)
- lld (~15MB)
- memfs (~1MB)
- sysroot.tar (~10MB)
- shared.js (~50KB)
- worker.js (~20KB)
- cpp_compiler.html

## 🔨 构建 APK

文件下载完成后，正常构建 APK：

```bash
./gradlew assembleDebug
```

或在 Android Studio 中：
- Build → Build Bundle(s) / APK(s) → Build APK(s)

## 📦 APK 大小影响

- **添加前**: 原始大小
- **添加后**: 增加约 50MB（压缩后约 20-30MB）

## ⚠️ 注意事项

### 文件大小
由于添加了 50MB 的编译器文件，APK 会变大。如果担心 APK 体积：

1. **方案 A**: 不打包文件，在运行时从 CDN 下载
   - 优点：APK 小
   - 缺点：用户首次使用需要下载 50MB

2. **方案 B**: 打包到 APK（推荐）
   - 优点：用户体验好，立即可用
   - 缺点：APK 变大

### Git
这些文件较大，建议添加到 `.gitignore`：

```gitignore
# wasm-clang files (download locally)
app/src/main/assets/wasm/clang
app/src/main/assets/wasm/lld
app/src/main/assets/wasm/memfs
app/src/main/assets/wasm/sysroot.tar
app/src/main/assets/wasm/shared.js
app/src/main/assets/wasm/worker.js
```

然后每个开发者本地运行下载脚本。

## 🚀 测试

下载并构建后，测试 C++ 编译功能：

### 测试代码 1: Hello World
```cpp
#include <iostream>
using namespace std;

int main() {
    cout << "Hello from wasm-clang!" << endl;
    return 0;
}
```

### 测试代码 2: STL Vector
```cpp
#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

int main() {
    vector<int> v = {5, 2, 8, 1, 9};
    sort(v.begin(), v.end());
    
    for(int x : v) {
        cout << x << " ";
    }
    cout << endl;
    
    return 0;
}
```

## 📝 许可证

wasm-clang 项目使用以下许可证：
- Apache-2.0 (wasm-clang)
- LLVM License (Clang/LLVM)
- vasm License

请确保遵守这些开源许可证的要求。

## 🔗 资源链接

- **项目**: https://github.com/binji/wasm-clang
- **Demo**: https://binji.github.io/wasm-clang
- **实现笔记**: https://gist.github.com/binji/b7541f9740c21d7c6dac95cbc9ea6fca

## ❓ 常见问题

### Q: 下载失败怎么办？
A: 可以使用 VPN 或从 CDN 下载：
```
https://cdn.jsdelivr.net/gh/binji/wasm-clang@master/[filename]
```

### Q: 可以压缩这些文件吗？
A: 这些已经是编译后的 wasm 文件，压缩空间有限。Android APK 会自动压缩资源。

### Q: 有更小的替代方案吗？
A: 可以考虑云端编译 API，但失去了离线能力。wasm-clang 是目前最好的离线方案。

### Q: 需要下载所有文件吗？
A: 是的，所有文件都是必需的：
- clang: 编译器
- lld: 链接器
- memfs: 文件系统
- sysroot.tar: C++ 标准库
- shared.js, worker.js: 运行时支持

## 📧 支持

如有问题，请查看：
- 项目文档: `CPP_WASM_IMPLEMENTATION.md`
- GitHub Issues: https://github.com/binji/wasm-clang/issues

