# 多语言补全框架

这是一个为ACC IDE设计的可扩展多语言补全框架，基于Tree-sitter做本地符号解析，专为竞赛编程场景优化。

## 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    CompletionManager                        │
│                     (单例管理器)                              │
└─────────────────────┬───────────────────────────────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────┐
│                UniversalCompletionEngine                    │
│                  (通用补全引擎)                               │
└─────────────────────┬───────────────────────────────────────┘
                      │
         ┌────────────┼────────────┐
         ▼            ▼            ▼
┌───────────────┐ ┌──────────────┐ ┌─────────────────┐
│CppLanguage    │ │JavaLanguage  │ │PythonLanguage   │
│Processor      │ │Processor     │ │Processor        │
│(完全实现)      │ │(基础实现)     │ │(基础实现)        │
└───────────────┘ └──────────────┘ └─────────────────┘
         │            │            │
         ▼            ▼            ▼
┌───────────────┐ ┌──────────────┐ ┌─────────────────┐
│TreeSitter     │ │TreeSitter    │ │TreeSitter       │
│C++ Service    │ │Java Service  │ │Python Service   │
└───────────────┘ └──────────────┘ └─────────────────┘
```

## 核心组件

### 1. 抽象层 (Abstract Classes)

#### AbstractTreeSitterProcessor
```kotlin
abstract class AbstractTreeSitterProcessor : LanguageProcessor {
    abstract fun getLanguageId(): String
    abstract fun parseCode(content: ContentReference, language: String): ParseResult?
    abstract fun provideRegularCompletions(...): List<CompletionItem>
    abstract fun provideMemberCompletions(...): List<CompletionItem>
}
```

### 2. 实现层 (Implementations)

#### UniversalCompletionEngine
- 通用补全引擎具体实现
- 管理多个语言处理器的注册与调用
- 提供统一的多语言补全API
- 支持语言检测和处理器路由
- 处理错误降级和性能优化

#### CompletionManager  
- 单例模式的全局管理器
- 自动注册所有语言处理器
- 提供简单易用的API
- 支持语言自动检测

### 3. 语言层 (Language Processors)

#### CppLanguageProcessor (完全实现)
- ✅ 基于TreeSitter的C++解析
- ✅ 作用域感知补全
- ✅ 类成员访问控制
- ✅ STL容器补全
- ✅ 函数参数和局部变量
- ✅ 结构体/类成员补全

#### JavaLanguageProcessor (基础实现)
- ✅ Java关键字补全
- ✅ 基础TreeSitter集成
- 🚧 类和接口补全 (TODO)
- 🚧 包导入补全 (TODO)
- 🚧 泛型支持 (TODO)
- 🚧 注解补全 (TODO)

#### PythonLanguageProcessor (基础实现)
- ✅ Python关键字补全
- ✅ 基础TreeSitter集成
- 🚧 动态类型推断 (TODO)
- 🚧 装饰器补全 (TODO)
- 🚧 内置函数补全 (TODO)
- 🚧 模块导入补全 (TODO)

## 使用方法

### 1. 基本使用

```kotlin
// 获取补全管理器实例
val completionManager = CompletionManager.getInstance()

// 检查语言支持
if (completionManager.isLanguageSupported("cpp")) {
    // 执行补全
    completionManager.requireAutoComplete(content, position, publisher, "cpp")
}
```

### 2. 现代化补全提供器

```kotlin
// 使用新的补全提供器
val provider = ModernACMCompletionProvider()

// 自动检测语言并补全
provider.requireAutoComplete(content, position, publisher, extraArguments)

// 或指定语言
provider.requireAutoComplete(content, position, publisher, "java")
```

### 3. 直接获取补全项

```kotlin
val completions = completionManager.getCompletions(content, position, "python")
completions.forEach { item ->
    println("${item.label}: ${item.desc}")
}
```

## 扩展新语言

### 1. 创建语言处理器

```kotlin
class NewLanguageProcessor : AbstractTreeSitterProcessor() {
    override fun getLanguageId(): String = "newlang"
    override fun getLanguageName(): String = "New Language"
    override fun getSupportedExtensions(): Set<String> = setOf("nl", "newlang")
    
    override fun provideRegularCompletions(...): List<CompletionItem> {
        // 实现常规补全逻辑
    }
    
    override fun provideMemberCompletions(...): List<CompletionItem> {
        // 实现成员补全逻辑
    }
    
    override fun getKeywords(): Set<String> {
        return setOf("keyword1", "keyword2", "keyword3")
    }
    
    // 实现其他抽象方法...
}
```

### 2. 注册新处理器

```kotlin
// 在CompletionManager.initializeLanguageProcessors()中添加
val newLangProcessor = NewLanguageProcessor()
engine.registerProcessor(newLangProcessor)
```

### 3. 添加TreeSitter支持

需要在C++层添加对应的TreeSitter语言支持：

```cpp
// 在TreeSitterCore.cpp中添加
TSLanguage* getLanguageByName(const std::string &language) {
    if (language == "newlang") {
        return tree_sitter_newlang();
    }
    // ... 其他语言
}
```

## 配置和优化

### 1. 性能配置

```kotlin
// 在CompletionManager中
class CompletionManager {
    companion object {
        private const val MAX_COMPLETIONS = 100  // 最大补全项数量
        private const val TIMEOUT_MS = 2000      // 补全超时时间
        private const val CACHE_SIZE = 50        // 缓存大小
    }
}
```

### 2. 调试配置

```kotlin
// 获取系统状态
val systemInfo = ModernACMCompletionProvider().getSystemInfo()
Log.d("Completion", "System info: $systemInfo")

// 获取特定语言状态
val cppStatus = completionManager.getLanguageStatus("cpp")
Log.d("Completion", "C++ status: $cppStatus")
```

## 架构说明

### 本地化设计
- **Tree-sitter复用**：将语法高亮工具复用做符号提取
- **无网络依赖**：适合竞赛环境的离线使用
- **简化逻辑**：专注基础补全功能，不追求IDE级别的复杂特性

### 非LSP架构
- **直接集成**：避免LSP协议的复杂性
- **性能优化**：减少进程间通信开销
- **竞赛优化**：针对ACM场景的特定需求

## 测试和验证

### 1. 单元测试

```kotlin
@Test
fun testCppCompletions() {
    val manager = CompletionManager.getInstance()
    val content = createTestContent("int x = 5; x.")
    val completions = manager.getCompletions(content, position, "cpp")
    assertTrue(completions.isNotEmpty())
}
```

### 2. 集成测试

```kotlin
@Test
fun testMultiLanguageSupport() {
    val manager = CompletionManager.getInstance()
    val supportedLanguages = manager.getSupportedLanguages()
    assertTrue(supportedLanguages.contains("cpp"))
    assertTrue(supportedLanguages.contains("java"))
    assertTrue(supportedLanguages.contains("python"))
}
```
