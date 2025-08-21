# ACM 竞赛编程智能补全系统

基于 Tree-sitter CST 的本地代码补全系统，专为 ACM 竞赛编程设计。
值得注意的是treesitter原本作为语法高亮的引擎，并不适合用于语法补全，因为其CST树的缘故，不能把变量结合语义抽象起来。

这里使用的方案也仅仅把CST树上的变量拿出来，以及一些函数表作为补全内容，所以极有可能出现如下不能补全情况：

```cpp
struct node{int l,r;};
int main() {
    int n;
    vector<vector<node> > a(n, vector<node>(n, {0, 0} ));
    a[0][0].
}
```

此时`.`后的`l,r`是不能被识别补全的。

此方案日后一定会被`lsp`给取代。

## 系统架构

### 三层架构设计

```
┌─────────────────────────────────────────────────────────────────┐
│                        应用层 (Kotlin)                          │
├─────────────────────────────────────────────────────────────────┤
│  CompletionManager → TreeSitterService → UniversalCompletionEngine │
│           ↓                    ↓                       ↓        │
│  语言处理器框架        JNI服务层            补全引擎           │
├─────────────────────────────────────────────────────────────────┤
│                        JNI桥接层 (C++)                          │
├─────────────────────────────────────────────────────────────────┤
│  TreeSitterJNI.cpp → TreeSitterRegistry → AbstractTreeSitterProcessor │
│           ↓                    ↓                       ↓        │
│      JNI接口            处理器注册管理            抽象处理器基类     │
├─────────────────────────────────────────────────────────────────┤
│                      原生处理层 (C++)                            │
├─────────────────────────────────────────────────────────────────┤
│  CppLanguageProcessor → JavaLanguageProcessor → PythonLanguageProcessor │
│           ↓                    ↓                       ↓        │
│      具体语言处理器实现 (继承AbstractTreeSitterProcessor)        │
│           ↓                    ↓                       ↓        │
│  tree_sitter_cpp()     tree_sitter_java()      tree_sitter_python() │
└─────────────────────────────────────────────────────────────────┘
```

### 数据流程

```
用户输入 → TreeSitterService → JNI → TreeSitterRegistry → 
具体语言处理器 → Tree-sitter解析 → 符号提取 → 作用域过滤 → 补全建议
```

### 核心设计理念

- **Tree-sitter 提供 CST**：本来用于语法高亮，这里复用做符号提取
- **模块化注册机制**：使用Registry模式管理多语言处理器
- **严格遵循官方设计**：使用 `TSParser` → `TSTree` → 手动遍历 `TSNode`
- **分层抽象设计**：AbstractTreeSitterProcessor提供通用功能，子类实现语言特定逻辑
- **本地化方案**：无需网络，适合竞赛环境
- **非 LSP 架构**：不依赖 Language Server Protocol，减少复杂性

## 文件结构

### C++原生层
```
app/src/main/cpp/
├── core/                                   # 核心抽象层
│   ├── AbstractTreeSitterProcessor.h/.cpp  # 抽象处理器基类
│   ├── ILanguageProcessor.h                # 语言处理器接口
│   ├── TreeSitterRegistry.h/.cpp          # 注册管理器
├── languages/                              # 语言特定实现
│   ├─�� CppLanguageProcessor.h/.cpp         # C++处理器
│   ├── JavaLanguageProcessor.h/.cpp        # Java处理器
│   └── PythonLanguageProcessor.h/.cpp      # Python处理器
├── TreeSitterJNI.h/.cpp                   # JNI桥接层
└── CMakeLists.txt                          # 构建配置
```

### Kotlin应用层
```
app/src/main/java/com/acc_ide/completion/
├── core/                                   # 核心数据模型
│   ├── CompletionModels.kt                 # 符号、作用域等数据类
│   └── CompletionConstants.kt              # 常量定义
├── framework/                              # 抽象框架层
│   ├── AbstractTreeSitterProcessor.kt      # Kotlin抽象处理器
│   ├── CompletionManager.kt                # 补全管理器
│   └── UniversalCompletionEngine.kt        # 通用补全引擎
├── languages/                              # 语言特定实现
│   ├── cpp/, java/, python/                # 各语言支持
├── providers/                              # 静态提示库
│   └── cpp/CppStaticLibrary.kt            # C++标准库提示
└── services/                               # 核心服务
    └── TreeSitterService.kt                # Tree-sitter JNI服务
```

## 实现原理

### 1. Registry注册模式
```cpp
// TreeSitterRegistry单例管理所有语言处理器
class TreeSitterRegistry {
    std::unordered_map<std::string, std::unique_ptr<ILanguageProcessor>> processors;
    
    void initializeBuiltinProcessors() {
        registerProcessor(std::make_unique<CppLanguageProcessor>());
        registerProcessor(std::make_unique<JavaLanguageProcessor>());  
        registerProcessor(std::make_unique<PythonLanguageProcessor>());
    }
};
```

### 2. Tree-sitter解析流程
```cpp
ParseResult executeTreeSitterParsing(const std::string &code, const TSLanguage* language) {
    // 1. 创建parser和tree
    TSParser *parser = ts_parser_new();
    ts_parser_set_language(parser, language);
    TSTree *tree = ts_parser_parse_string(parser, nullptr, code.c_str(), code.length());
    
    // 2. 遍历AST提取符号
    TSNode rootNode = ts_tree_root_node(tree);
    extractLanguageSpecificSymbols(rootNode, code, symbols, 0);
    
    // 3. 清理资源并返回
    ts_tree_delete(tree);
    ts_parser_delete(parser);
    return ParseResult{symbols, scopes};
}
```

### 3. 作用域过滤
```kotlin
// 根据当前位置过滤可见符号
fun getSymbolsAtPosition(line: Int, column: Int): List<SymbolInfo> {
    return symbols.filter { symbol ->
        // 时间可见性：符号必须在当前位置之前声明
        val isTemporallyVisible = symbol.line < line || (symbol.line == line && symbol.column < column)
        
        // 作用域可见性：根据符号类型和作用域层级判断
        isTemporallyVisible && when (symbol.type) {
            SymbolType.VARIABLE -> symbol.scopeLevel == 0 || isInCurrentScope(symbol)
            SymbolType.STRUCT_MEMBER -> isInClassMethod(symbol)
            else -> true
        }
    }
}
```

## 已实现功能 ✅

### C++/Java/Python 语法支持

- ✅ **基本变量声明**：`int x, y = 5;`
- ✅ **数组变量**：`bool vis[MAXN];`
- ✅ **指针和引用**：`int *ptr, &ref;`
- ✅ **结构体成员**：`struct node { int l, r; };`
- ✅ **类成员和方法**：`class MyClass { int data; };`
- ✅ **枚举值**：`enum Color { RED, GREEN, BLUE };`
- ✅ **函数参数和局部变量**
- ✅ **命名空间**：`namespace myspace { };`

### 智能补全特性

- ✅ **作用域感知**：根据当前作用域显示可用符号
- ✅ **成员访问补全**：`obj.` / `obj->` 智能成员提示
- ✅ **类型识别**：精确识别变量类型，包括数组、指针、引用
- ✅ **STL 容器支持**：`vector`, `string`, `map` 等成员方法补全
- ✅ **优先级排序**：局部变量 > 参数 > 全局变量
- ✅ **使用频率统计**：常用符号优先显示
- ✅ **ACM 模板补全**：`gcd`, `lcm`, `pow_mod` 等常用模板

## 性能特性

- ✅ **原生性能**：C++ 实现，JNI 集成，响应迅速
- ✅ **增量解析**：Tree-sitter 支持增量更新，只重新解析修改部分
- ✅ **符号缓存**：解析结果缓存，避免重复计算
- ✅ **异步处理**：后台线程解析，不阻塞 UI

## 测试验证

### 基本变量补全
```cpp
int main() {
    int x = 5;
    vector<int> nums;
    string text = "hello";
    
    // 输入 'x' → 显示: x (int)
    // 输入 'n' → 显示: nums (vector<int>)
    // 输入 't' → 显示: text (string)
}
```

### 结构体成员补全
```cpp
struct node {
    int l, r;  // 多成员声明
};

int main() {
    node nd;
    nd.  // 显示: l (int), r (int)
}
```

## TODO

### 近期计划
- [ ] **完善 Java 补全**：实现完整的 Java 语言补全逻辑
- [ ] **完善 Python 补全**：实现完整的 Python 语言补全逻辑
- [ ] **高级 C++ 特性**：Lambda 表达式、auto 类型推导、模板特化
- [ ] **模糊匹配**：支持 "vect" 匹配 "vector"
- [ ] **智能参数提示**：函数调用时显示参数信息

### 中期计划
- [ ] **错误容忍解析**：语法错误时仍能提供补全
- [ ] **代码片段补全**：`for` 循环、`class` 定义等模板
- [ ] **继承关系分析**：支持多态和继承的成员访问
- [ ] **跨文件符号索引**：项目范围的符号查找
- [ ] **增量更新优化**：更精确的增量解析

### 长期规划
- [ ] **迁移到 LSP 架构**：实现标准 Language Server Protocol 支持
- [ ] **语法高亮职责分离**：Tree-sitter 专注语法高亮，LSP 负责补全和诊断
- [ ] **跨文件符号索引**：通过 LSP 实现项目级别的符号管理
- [ ] **现代化 IDE 特性**：错误诊断、代码格式化、重构建议（通过 LSP）

## 当前限制

- ❌ **基础 C++ 语法**：不支持 C++20 新特性（概念、协程等）
- ❌ **简单类型推导**：不支持复杂的模板类型推导
- ❌ **单文件分析**：暂不支持跨文件的符号引用