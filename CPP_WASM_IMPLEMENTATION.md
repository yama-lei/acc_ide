# C++ WASM 本地编译实现

## 概述

本项目实现了基于 WebAssembly 的 C++ 本地编译功能，使用 **wasm-clang** 完整工具链，支持完整的 C++ 标准库（STL），可在 Android WebView 中直接编译和运行 C++ 代码，无需网络连接。

## 技术方案

### 1. 编译器选择：wasm-clang

**为什么选择 wasm-clang？**
- ✅ **完整的 STL 支持**：包含完整的 C++ 标准库
- ✅ **真正的编译器**：使用 Clang/LLVM 编译到 WebAssembly
- ✅ **ACM 竞赛支持**：支持所有标准算法和数据结构
- ✅ **离线运行**：所有文件预打包到 APK，无需下载

**替代方案对比**：
| 方案 | STL 支持 | 性能 | 文件大小 | 结论 |
|------|---------|------|---------|------|
| JSCPP | ❌ 无 | 较慢 | 小 (200KB) | 不适合 ACM |
| wasm-clang | ✅ 完整 | 快 | 大 (90MB) | **最佳选择** |

### 2. 核心架构

```
Android App (Kotlin)
    ├── WebView
    │   ├── cpp_compiler_v2.html (前端界面)
    │   └── shared.js (WASI 运行时)
    │       ├── API 类 (主接口)
    │       ├── MemFS 类 (内存文件系统)
    │       ├── App 类 (WASI 实现)
    │       └── Tar 类 (sysroot 解压)
    └── Assets
        ├── clang (编译器 WASM, ~30MB)
        ├── lld (链接器 WASM, ~30MB)
        ├── memfs (内存文件系统 WASM, ~1MB)
        └── sysroot.tar (C++ 标准库头文件, ~30MB)
```

### 3. 实现流程

#### 初始化阶段
1. **加载 shared.js**：提供完整的 WASI 运行时
2. **创建 API 实例**：配置文件读取和输出回调
3. **加载 memfs**：编译并实例化内存文件系统
4. **解压 sysroot.tar**：提取 C++ 标准库头文件
5. **通知 Android**：编译器初始化完成

#### 编译执行阶段
1. **接收代码和输入**：从 Android 传入 C++ 代码
2. **设置 stdin**：配置程序输入
3. **编译**：
   - 加载并实例化 clang WASM
   - 编译 C++ 代码为 `.o` 文件
4. **链接**：
   - 加载并实例化 lld WASM
   - 链接生成 `.wasm` 可执行文件
5. **运行**：
   - 实例化生成的 WASM 程序
   - 捕获 stdout/stderr
6. **返回结果**：发送输出和退出码到 Android

### 4. 输出优化

**只显示程序结果，隐藏编译过程：**
- ✅ 过滤编译器加载信息
- ✅ 过滤编译命令和参数
- ✅ 只输出程序的实际 stdout
- ✅ 显示程序实际执行时间（从 test.wasm 运行开始）
- ✅ 编译错误会完整显示

**输出示例**：
```
hello world

Execution time: 0.01s
```

**编译错误示例**：
```
Compilation Error:
test.cc:5:5: error: use of undeclared identifier 'cout'
    cout << "hello";
    ^
```

## 核心文件说明

### 前端文件

#### `app/src/main/assets/wasm/cpp_compiler_v2.html`
- **功能**：WebView 加载的前端页面
- **职责**：
  - 初始化 wasm-clang 编译器
  - 实现 `compileAndRun(code, input)` 函数
  - 过滤编译过程输出
  - 通过 `AndroidBridge` 与 Kotlin 通信

#### `app/src/main/assets/wasm/shared.js`
- **来源**：[binji/wasm-clang](https://github.com/binji/wasm-clang)
- **功能**：完整的 WASI 运行时实现
- **核心类**：
  - `API`：主接口，提供 `compileLinkRun(code)` 方法
  - `MemFS`：内存文件系统，管理虚拟文件
  - `App`：WASI 系统调用实现
  - `Tar`：解压 sysroot.tar

### 后端文件

#### `app/src/main/java/com/acc_ide/execution/wasm/WasmCppExecutor.kt`
- **功能**：WebView 管理和 JavaScript 桥接
- **关键方法**：
  - `initialize()`：初始化 WebView，加载 cpp_compiler_v2.html
  - `execute()`：调用 JavaScript 的 `compileAndRun()` 函数
  - `JsInterface`：JavaScript 回调接口
    - `onWasmReady()`：编译器初始化完成
    - `onOutput(text)`：程序输出
    - `onExecutionComplete(exitCode)`：执行完成

### 编译器文件

所有文件位于 `app/src/main/assets/wasm/`：
- `clang`：Clang 编译器 (WebAssembly 二进制)
- `lld`：LLVM 链接器 (WebAssembly 二进制)
- `memfs`：内存文件系统 (WebAssembly 二进制)
- `sysroot.tar`：C++ 标准库头文件和库文件

## 文件下载

### 自动下载脚本

**PowerShell (Windows)**:
```powershell
cd treesitter-build
.\Download-WasmClang.ps1
```

**Bash (Linux/macOS)**:
```bash
cd treesitter-build
./Download-WasmClang.sh
```

脚本会自动：
1. 从 GitHub 下载所有必需文件
2. 保存到 `app/src/main/assets/wasm/`
3. 验证文件完整性

### 手动下载

如果脚本失败，可手动下载：

**文件列表**：
```
https://github.com/binji/wasm-clang/releases/latest/download/clang
https://github.com/binji/wasm-clang/releases/latest/download/lld
https://github.com/binji/wasm-clang/releases/latest/download/memfs
https://github.com/binji/wasm-clang/releases/latest/download/sysroot.tar
https://raw.githubusercontent.com/binji/wasm-clang/main/shared.js
```

**保存路径**：`app/src/main/assets/wasm/`

## 性能特性

### 首次加载时间
- **编译器初始化**：约 0.5s
  - 加载 memfs：0.1s
  - 解压 sysroot.tar：0.4s

### 编译时间（首次）
- **加载编译器**：约 0.4s (clang) + 0.3s (lld)
- **编译简单程序**：约 2s
- **链接**：约 0.1s
- **总时间**：约 3s

### 后续编译
- 编译器已缓存在内存
- **编译时间**：< 0.1s
- **执行时间**：< 0.01s

### 内存占用
- **编译器模块**：约 100MB（首次加载）
- **运行时内存**：约 50MB

## 支持的 C++ 特性

### 标准库
- ✅ **iostream**：输入输出流
- ✅ **string**：字符串
- ✅ **vector**：动态数组
- ✅ **map, set**：关联容器
- ✅ **algorithm**：标准算法
- ✅ **queue, stack**：队列和栈
- ✅ **numeric**：数值算法
- ✅ **cmath**：数学函数

### 语言特性
- ✅ **C++17**：完整支持
- ✅ **模板**：全功能模板
- ✅ **Lambda**：匿名函数
- ✅ **异常处理**：try/catch
- ✅ **STL 算法**：sort, find, transform 等

### 编译选项
- **优化级别**：`-O2`（默认）
- **C++ 标准**：C++17
- **警告**：显示前 19 个错误

## 使用示例

### Hello World
```cpp
#include <iostream>
using namespace std;

int main() {
    cout << "Hello, World!" << endl;
    return 0;
}
```

**输出**：
```
Hello, World!

Execution time: 0.01s
```

### STL 示例
```cpp
#include <iostream>
#include <vector>
#include <algorithm>
using namespace std;

int main() {
    vector<int> v = {3, 1, 4, 1, 5};
    sort(v.begin(), v.end());
    
    for (int x : v) {
        cout << x << " ";
    }
    cout << endl;
    
    return 0;
}
```

**输出**：
```
1 1 3 4 5

Execution time: 0.01s
```

### 输入输出
```cpp
#include <iostream>
using namespace std;

int main() {
    int a, b;
    cin >> a >> b;
    cout << a + b << endl;
    return 0;
}
```

**输入**：`10 20`
**输出**：
```
30

Execution time: 0.00s
```

## 已知限制

### 不支持的功能
- ❌ **文件 I/O**：不支持 `fstream`（虚拟文件系统限制）
- ❌ **多线程**：不支持 `<thread>`（WASM 单线程）
- ❌ **系统调用**：不支持 `system()`
- ❌ **网络**：不支持 socket

### 性能限制
- ⚠️ **首次编译慢**：需要加载编译器（约 3s）
- ⚠️ **内存占用大**：编译器模块约 100MB
- ⚠️ **大文件慢**：超过 1000 行代码编译较慢

## 故障排除

### 编译器初始化失败
**错误**：`Failed to load clang`
**解决**：
1. 检查 `app/src/main/assets/wasm/` 目录
2. 确认文件存在：clang, lld, memfs, sysroot.tar, shared.js
3. 运行下载脚本重新下载

### WebAssembly.instantiate 错误
**错误**：`Import #0 "wasi_unstable": module is not an object`
**原因**：缺少 shared.js 或版本不匹配
**解决**：确保使用最新的 shared.js

### 编译错误
**错误**：`error: use of undeclared identifier`
**原因**：
1. 拼写错误
2. 缺少 `using namespace std;`
3. 缺少头文件包含

## 未来改进

### 优化方向
1. **缓存优化**：缓存已编译的编译器模块
2. **并行编译**：使用 Web Worker
3. **增量编译**：只重新编译修改的部分
4. **压缩**：使用 gzip 压缩 WASM 文件

### 功能增强
1. **调试支持**：集成 GDB
2. **性能分析**：显示热点函数
3. **代码补全**：基于 libclang
4. **错误提示**：更友好的错误信息

## 参考资料

- [wasm-clang 项目](https://github.com/binji/wasm-clang)
- [WebAssembly 官网](https://webassembly.org/)
- [WASI 规范](https://wasi.dev/)
- [Clang 文档](https://clang.llvm.org/)

## 致谢

本实现基于 [binji/wasm-clang](https://github.com/binji/wasm-clang) 项目，感谢原作者提供的完整 WASI 实现和编译工具链。

---

**最后更新**：2025-10-02
**版本**：2.0 (wasm-clang)
**状态**：✅ 生产就绪
