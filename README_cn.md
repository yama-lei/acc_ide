![展示1](img/Display_cn.png)

# ACC IDE

- [Version list](RELEASE.md)
- [English](README.md)
- [简体中文](README_cn.md)

如果你也为OJ平台自带的手机不友好型IDE感到厌烦，如果你也想在手机上把灵光一现的算法写出来，那么你应该试试ACC IDE🤗。

ACC IDE 是一个专为算法竞赛设计的，基于 Android 的原生集成开发环境。它旨在增强移动设备上的竞赛编程体验，为编写、测试和提交算法解决方案提供功能丰富的环境😋。

## 项目结构

该项目由安卓原生构建，包含以下主要部分：

### 核心结构

```
acc_ide/
├── app/                          # 主应用模块
│   ├── src/main/
│   │   ├── java/com/acc_ide/    # Kotlin 源代码
│   │   │   ├── completion/      # 代码补全系统
│   │   │   ├── ui/              # UI 组件
│   │   │   ├── util/            # 工具类
│   │   │   └── view/            # 自定义视图
│   │   ├── cpp/                 # Tree-sitter JNI
│   │   ├── res/                 # 资源文件
│   │   └── assets/              # 静态资源
│   └── build.gradle
├── executor-library/             # 代码执行器库
│   ├── src/main/
│   │   ├── java/                # 执行器实现
│   │   └── assets/wasm/         # WASM 资源
│   └── build.gradle
├── treesitter-build/             # Tree-sitter 构建脚本
├── wasmClang-build/              # WASM Clang 构建脚本
└── gradle/                       # Gradle 配置
```


## 已实现功能

### 编辑器功能
- **语法高亮**：使用`textmate`进行语法高亮
- **代码补全**：基于`CST(tree-siiter)`的代码补全
- **主题支持**：深色和浅色模式，适当的语法着色
- **手势控制**：通过缩放手势调整字体大小
- **行号和代码块缩进**：提供代码结构视觉辅助

### 文件管理
- **文件浏览器**：带有可用文件列表的侧边抽屉
- **重命名和删除**：带有确认对话框的文件管理工具
- **自动保存**：自动保存更改，防止数据丢失，临时文件夹的路径为`/storage/emulated/0/Android/data/com.acc_ide/files`，其底下的`/tempalte`为模板文件

### 自定义功能
- **语言选择**：可以在设置中更改界面语言
- **主题选择**：在深色和浅色主题之间切换
- **字体大小控制**：通过设置或手势调整编辑器字体大小
- **编辑器偏好**：通过设置自定义编辑器行为，如光标粗细、符号面板显示等

### 输入/输出面板
- **输入/输出面板**：用于手动输入和查看输出
- **Github Action的运行后端**： 通过 Github Action 提供的免费运行后端[仓库地址](https://github.com/META-Xiao/accide-code-execution)，支持 C/C++、Java 和 Python 的在线编译和执行
- **基于wasm的本地运行环境**： 基于WebAssembly的编译环境，但由于cpp没有预编译好的wsm，所以运行cpp时可能会有些问题
- **限制运行内存和时间**： 运行后端限制代码运行时间（2s）和内存（512MB）
- **运行状态显示**： 显示代码运行状态和运行时间，AC、WA、TLE、MLE、RE、CE、RS（Run successful，当用户未输入`答案输出`时运行成功的标志），编译错误等信息也会高亮显示

## 计划实现功能

### 完善部分功能
- **安卓版本的Error Lens**： 在编辑器中高亮显示编译错误
- **LSP**: 计划采用`tree-sitter+LSP`的方案进行精确语法高亮和语义级代码补全  
- **维护/优化wasm-clang**

### 添加 competitive-companion 
- Android 版本的 competitive-companion
- 直接从问题陈述导入测试用例
- 支持主要竞赛编程平台：
  - Codeforces
  - AtCoder
  - 洛谷
  - 牛客

## 安装

- 点击[releases](https://github.com/META-Xiao/acc_ide/releases/latest)安装最新版本
- 或者 `clone`项目到本地，使用 Android Studio 打开项目并运行

## 贡献

如果您在使用的过程中发现任何问题或功能需求，欢迎提交 `issue` 或 `pull request`。


## 致谢

- [Sora Editor](https://github.com/Rosemoe/sora-editor) 提供代码编辑功能
- [VSCode TextMate](https://github.com/microsoft/vscode-textmate) 提供语法高亮支持
- [Tree-sitter](https://github.com/tree-sitter/tree-sitter) 提供`CST`的构建支持  
- [wasm-clang](https://github.com/binji/wasm-clang) 提供`wasm-clang`demo
- [pyodide](https://github.com/pyodide/pyodide) 提供开箱即用的`wasm-python`