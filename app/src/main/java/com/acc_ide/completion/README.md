# ACM 竞赛编程智能补全系统

基于 Tree-sitter CST 的本地代码补全系统，专为 ACM 竞赛编程设计。

## 系统架构

```
源码 → Tree-sitter解析器 → CST → 符号提取 → 作用域分析 → 智能补全
```

### 核心设计理念

- **Tree-sitter 提供 CST**：本来用于语法高亮，这里复用做符号提取
- **自定义符号提取**：从 CST 中提取符号信息用于补全
- **严格遵循官方设计**：使用 `TSParser` → `TSTree` → 手动遍历 `TSNode`
- **模块化设计**：按语言分层组织，便于扩展
- **本地化方案**：无需网络，适合竞赛环境

### 架构说明

- **Tree-sitter**：主要用于语法高亮，这里复用做基础语法解析
- **本地补全**：由CST简化的补全逻辑，专注竞赛编程场景
- **非 LSP 架构**：不依赖 Language Server Protocol，减少复杂性

## 已实现功能 ✅

### C++ 语法支持
- ✅ **基本变量声明**：`int x, y = 5;`
- ✅ **数组变量**：`bool vis[MAXN];`
- ✅ **指针和引用**：`int *ptr, &ref;`
- ✅ **结构体成员**：`struct node { int l, r; };`
- ✅ **类成员和方法**：`class MyClass { int data; };`
- ✅ **枚举值**：`enum Color { RED, GREEN, BLUE };`
- ✅ **联合体成员**：`union Data { int i; float f; };`
- ✅ **函数参数和局部变量**
- ✅ **命名空间**：`namespace myspace { };`
- ✅ **类型别名**：`using MyInt = int;`

### 智能补全特性
- ✅ **作用域感知**：根据当前作用域显示可用符号
- ✅ **成员访问补全**：`obj.` / `obj->` 智能成员提示
- ✅ **类型识别**：精确识别变量类型，包括数组、指针、引用
- ✅ **STL 容器支持**：`vector`, `string`, `map` 等成员方法补全
- ✅ **优先级排序**：局部变量 > 参数 > 全局变量
- ✅ **使用频率统计**：常用符号优先显示
- ✅ **ACM 模板补全**：`gcd`, `lcm`, `pow_mod` 等常用模板

### Java 和 Python 支持
- ✅ **基础语法解析**：变量、函数、类声明
- ⏳ **补全逻辑开发中**：完整智能补全功能待实现

## 系统组件

### 原生层 (C++)
```
app/src/main/cpp/
├── TreeSitterCore.cpp      # 核心符号提取逻辑
├── TreeSitterJNI.cpp       # JNI桥接层
└── TreeSitterJNI.h         # 头文件定义
```

### Kotlin 服务层
```
app/src/main/java/com/acc_ide/completion/
├── core/                    # 核心组件
│   ├── CompletionConstants.kt
│   ├── CompletionModels.kt
│   └── ModernACMCompletionProvider.kt
├── framework/               # 通用补全框架
│   ├── AbstractTreeSitterProcessor.kt
│   ├── CompletionManager.kt
│   ├── LanguageProcessor.kt
│   └── UniversalCompletionEngine.kt
├── languages/               # 语言处理器
│   ├── LanguageManager.kt
│   ├── cpp/
│   │   ├── CppLanguageProcessor.kt
│   │   └── CppLanguageSupport.kt
│   ├── java/
│   │   └── JavaLanguageProcessor.kt
│   └── python/
│       └── PythonLanguageProcessor.kt
├── providers/               # 静态提示库
│   ├── cpp/
│   │   └── CppStaticLibrary.kt
│   ├── java/
│   └── python/
└── services/                # Tree-sitter服务
    └── TreeSitterService.kt
```

## 实现原理

### 1. 符号提取流程
```cpp
// 1. 解析源码为CST
TSParser *parser = ts_parser_new();
ts_parser_set_language(parser, tree_sitter_cpp());
TSTree *tree = ts_parser_parse_string(parser, nullptr, code.c_str(), code.length());

// 2. 遍历CST节点
TSNode rootNode = ts_tree_root_node(tree);
traverseNodeForSymbols(rootNode, code, symbols);

// 3. 提取符号信息
if (strcmp(nodeType, "declaration") == 0) {
    // 处理变量声明
} else if (strcmp(nodeType, "function_definition") == 0) {
    // 处理函数定义
}
```

### 2. 作用域分析
- **函数作用域**：函数参数在函数体内可见
- **块作用域**：`{}` 内的变量只在块内可见
- **类作用域**：类成员在类内可见
- **全局作用域**：全局符号在任何地方可见

### 3. 补全触发
```kotlin
// 成员访问: obj.
val (actualPrefix, contextVar) = extractMemberAccessContext(content, position, prefix)
if (contextVar != null) {
    // 查找 obj 的类型，提供成员补全
    handleMemberCompletion(contentRef, language, contextVar, actualPrefix)
} else {
    // 常规补全：变量、函数、关键字
    handleRegularCompletion(contentRef, language, position, actualPrefix)
}
```

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

### 数组变量补全 ✅
```cpp
const int MAXN = 1e5+5;
bool vis[MAXN];

int main() {
    // 输入 'v' → 显示: vis (bool[])
}
```

### 结构体成员补全 ✅
```cpp
struct node {
    int l, r;  // 多成员声明
};

int main() {
    node nd;
    nd.  // 显示: l (int), r (int)
}
```

## 未来 TODO

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

- ⏳ **多语言支持开发中**：Java 和 Python 的完整补全逻辑待实现
- ❌ **基础 C++ 语法**：不支持 C++20 新特性（概念、协程等）
- ❌ **简单类型推导**：不支持复杂的模板类型推导
- ❌ **单文件分析**：暂不支持跨文件的符号引用

---