![展示1](img/Display_cn.png)

# ACC IDE

- [Version list](RELEASE.md)
- [English](README_en.md)
- [简体中文](README.md)

如果你也为OJ平台自带的手机不友好型IDE感到厌烦，如果你也想在手机上把灵光一现的算法写出来，那么你应该试试ACC IDE🤗。

ACC IDE 是一个专为算法竞赛设计的，基于 Android 的原生集成开发环境。它旨在增强移动设备上的竞赛编程体验，为编写、测试和提交算法解决方案提供功能丰富的环境😋。

## 概述

ACC IDE 致力于为需要随时随地编码和测试算法的竞赛程序员提供全面的移动解决方案。该应用程序提供语法高亮、代码补全、文件管理等基本功能，专为算法竞赛量身定制。

## 项目结构

该项目由安卓原生构建，包含以下主要部分：

### 核心结构

```
acc_ide/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/acc_ide/
│   │   │   │   ├── adapter/                          # RecyclerView 适配器
│   │   │   │   │   └── FileListAdapter.kt            # 文件列表适配器
│   │   │   │   ├── completion/                       # 代码补全系统
│   │   │   │   │   ├── core/                         # 补全核心组件
│   │   │   │   │   ├── framework/                    # 补全框架
│   │   │   │   │   ├── languages/                    # 语言特定补全支持
│   │   │   │   │   ├── providers/                    # 补全提供器
│   │   │   │   │   ├── services/                     # 补全服务
│   │   │   │   ├── data/                             
│   │   │   │   │   ├── model/                        # 数据模型
│   │   │   │   │   └── repository/                   # 数据仓库
│   │   │   │   ├── dialog/                           # 对话框组件
│   │   │   │   ├── ui/                               # UI 组件              
│   │   │   │   ├── util/                             # 工具类
│   │   │   │   └── view/                             # 自定义视图
│   │   │   ├── cpp/                                  
│   │   │   │   ├── core/                             # Tree-sitter 核心
│   │   │   │   ├── languages/                        # 语言处理器
│   │   │   │   └── TreeSitterJNI.cpp                 # JNI 接口
│   │   │   ├── res/                                  
│   │   │   ├── assets/                               
│   │   │   ├── jniLibs/                              
│   │   │   └── AndroidManifest.xml
│   ├── build.gradle                                  
├── gradle/                                           
├── build.gradle.kts                                  
└── settings.gradle.kts                               
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
- **编译进度指示器**：显示编译进度，并在编译完成后显示结果
- **限制运行内存和时间**： 通过Github Action的运行后端限制代码运行时间（2s）和内存（512MB）
- **运行状态显示**： 显示代码运行状态和运行时间，AC、WA、TLE、MLE、RE、CE、RS（Run successful，当用户未输入`答案输出`时运行成功的标志）

## 计划实现功能

### 完善部分功能
- **完善Github Action**： 完善对 Java 和 Python 的编译运行支持
- **安卓版本的Error Lens**： 在编辑器中高亮显示编译错误
- **LSP**: 计划采用`tree-sitter+LSP`的方案进行精确语法高亮和语义级代码补全  

### competitive-companion 集成
- Android 版本的 competitive-companion
- 直接从问题陈述导入测试用例
- 支持主要竞赛编程平台：
  - Codeforces
  - AtCoder
  - 洛谷
  - 牛客

### 编译器本地集成
- 本地编译和执行
- 支持不同编译器版本
- 在编辑器中高亮显示编译错误


## 安装

- 点击[releases](https://github.com/META-Xiao/acc_ide/releases/latest)安装最新版本
- 或者 `clone`项目到本地，使用 Android Studio 打开项目并运行

## 贡献

如果您在使用的过程中发现任何问题或功能需求，欢迎提交 `issue` 或 `pull request`。


## 致谢

- [Sora Editor](https://github.com/Rosemoe/sora-editor) 提供代码编辑功能
- [VSCode TextMate](https://github.com/microsoft/vscode-textmate) 提供语法高亮支持
- [Tree-sitter](https://github.com/tree-sitter/tree-sitter) 提供`CST`的构建支持  