# 🚀 C++ WASM 编译器快速开始

## ❌ 当前问题

你看到的错误是因为 **wasm-clang 文件还没有下载**。

```
[ERROR] Fetch API cannot load file:///android_asset/wasm/clang
[ERROR] Failed to fetch
[ERROR] WASM C++ initialization failed
```

## ✅ 解决方案：下载文件

### 第一步：运行下载脚本

**Windows PowerShell:**
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

这会下载约 50MB 的文件到 `app/src/main/assets/wasm/` 目录。

### 第二步：验证文件

检查文件是否下载成功：

```bash
# 应该看到以下文件：
ls app/src/main/assets/wasm/
# clang (~25MB)
# lld (~15MB)
# memfs (~1MB)
# sysroot.tar (~10MB)
# shared.js
# worker.js
# cpp_compiler.html
```

### 第三步：重新构建

```bash
./gradlew clean
./gradlew assembleDebug
```

### 第四步：测试

运行 APP，尝试编译这段代码：

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

## 📊 文件说明

| 文件 | 大小 | 用途 |
|------|------|------|
| `clang` | 25MB | C++ 编译器 |
| `lld` | 15MB | 链接器 |
| `memfs` | 1MB | 文件系统 |
| `sysroot.tar` | 10MB | C++ 标准库（STL） |
| `shared.js` | 50KB | 工具库 |
| `worker.js` | 20KB | Worker |

**总计**: 约 50MB

## 🔧 为什么需要这些文件？

1. **JSCPP 不支持 STL** ❌
   - 你之前遇到的 "cannot find library: vector" 错误

2. **wasm-clang 是完整编译器** ✅
   - 支持所有 STL 容器和算法
   - 真正的 Clang/LLVM 编译器
   - 完美支持 ACM 竞赛代码

3. **需要本地文件** 📦
   - 虽然可以从 CDN 下载，但会很慢
   - 打包到 APK 后立即可用
   - 完全离线运行

## ⚠️ 注意事项

### APK 体积
- 添加后 APK 会增加约 20-30MB（压缩后）
- 这是支持完整 C++ STL 的代价

### 首次加载
- 初始化需要几秒钟（加载 50MB 文件）
- 之后编译速度正常

### 不提交到 Git
- 这些文件已添加到 `.gitignore`
- 每个开发者需要运行下载脚本

## 🆘 故障排除

### 问题 1: 下载脚本失败

**原因**: 网络问题或 GitHub 访问受限

**解决**:
```bash
# 方法 1: 使用镜像
# 编辑下载脚本，将 raw.githubusercontent.com 替换为镜像地址

# 方法 2: 手动下载
# 访问 https://github.com/binji/wasm-clang
# 下载文件到 app/src/main/assets/wasm/
```

### 问题 2: 文件下载了但还是报错

**原因**: 需要重新构建 APK

**解决**:
```bash
./gradlew clean
./gradlew assembleDebug
```

### 问题 3: 不想打包这么大的文件

**选项 A**: 使用云端编译
- 不打包文件
- 调用在线 C++ 编译 API
- 需要网络连接

**选项 B**: 使用简化版本
- 但不支持 STL（会有 "cannot find library: vector" 错误）

**推荐**: 打包 wasm-clang 获得最佳体验

## 📚 更多文档

- `CPP_WASM_IMPLEMENTATION.md` - 技术实现细节
- `WASM_CLANG_SETUP.md` - 详细设置指南
- `treesitter-build/README_WASM_CLANG.md` - 下载说明
- `app/src/main/assets/wasm/README.md` - 文件清单

## 🎯 总结

**现在要做的**:

1. ✅ 运行下载脚本: `.\Download-WasmClang.ps1` 或 `./Download-WasmClang.sh`
2. ✅ 验证文件: 检查 `app/src/main/assets/wasm/` 目录
3. ✅ 重新构建: `./gradlew assembleDebug`
4. ✅ 测试: 编译带 STL 的 C++ 代码

**为什么这样做**:

- 获得完整的 C++ STL 支持
- 支持 vector, map, set, algorithm 等
- ACM 竞赛代码完美运行
- 完全离线，无需网络

**一次性设置，永久使用！** 🎉

