# TreeSitter Modular Architecture

## 架构概述 / Architecture Overview

本项目将TreeSitter的C++代码重构为模块化架构，分离抽象层和应用层，便于添加新语言支持。

### 目录结构 / Directory Structure

```
cpp/
├── core/                           # 抽象层 / Abstraction Layer
│   ├── ILanguageProcessor.h        # 语言处理器接口
│   ├── AbstractTreeSitterProcessor.h/.cpp  # 抽象基类
│   └── TreeSitterRegistry.h/.cpp   # 语言注册管理器
├── languages/                      # 应用层 / Application Layer  
│   ├── CppLanguageProcessor.h/.cpp  # C++语言处理器
│   ├── JavaLanguageProcessor.h/.cpp # Java语言处理器
│   └── PythonLanguageProcessor.h/.cpp # Python语言处理器
├── TreeSitterJNI_New.cpp          # 新JNI接口
├── CMakeLists_New.txt              # 新构建配置
└── TreeSitterJNI.h                 # 共享头文件
```

## 核心设计 / Core Design

### 抽象层 / Abstraction Layer

1. **ILanguageProcessor**: 定义所有语言处理器的通用接口
2. **AbstractTreeSitterProcessor**: 提供TreeSitter通用功能的基类实现
3. **TreeSitterRegistry**: 单例注册管理器，负责语言处理器的注册和查找

### 应用层 / Application Layer

每种语言都有独立的处理器类，继承抽象基类并实现语言特定的符号提取逻辑：

- **CppLanguageProcessor**: 处理C++语法结构
- **JavaLanguageProcessor**: 处理Java语法结构  
- **PythonLanguageProcessor**: 处理Python语法结构

## 添加新语言 / Adding New Languages

要添加新语言支持，只需：

1. 创建新的语言处理器类，继承`AbstractTreeSitterProcessor`
2. 实现`extractLanguageSpecificSymbols`方法
3. 在`TreeSitterRegistry::initializeBuiltinProcessors()`中注册新处理器

### 示例 / Example

```cpp
class GoLanguageProcessor : public AbstractTreeSitterProcessor {
public:
    std::string getLanguageId() const override { return "go"; }
    std::string getLanguageName() const override { return "Go"; }
    std::vector<std::string> getSupportedExtensions() const override { return {"go"}; }
    const TSLanguage* getTreeSitterLanguage() const override { return tree_sitter_go(); }

protected:
    void extractLanguageSpecificSymbols(
        TSNode rootNode, 
        const std::string &source, 
        std::vector<SymbolInfo> &symbols, 
        int scopeLevel = 0
    ) const override {
        // 实现Go语言特定的符号提取逻辑
    }
};
```

## 构建说明 / Build Instructions

1. 替换`CMakeLists.txt`为`CMakeLists_New.txt`
2. 替换`TreeSitterJNI.cpp`为`TreeSitterJNI_New.cpp`
3. 重新构建项目

## 优势 / Benefits

- **模块化**: 每种语言独立实现，易于维护
- **可扩展**: 添加新语言只需实现一个类
- **职责分离**: 抽象层处理通用逻辑，应用层处理语言特定逻辑
- **代码复用**: 通用TreeSitter功能在基类中实现
- **单一责任**: 每个类只负责一种语言的处理