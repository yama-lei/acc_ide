# ACM 竞赛编程智能补全系统

基于 Tree-sitter CST 的 C++ 智能代码补全系统，专为 ACM 竞赛编程设计。

## 系统架构

```
源码 → Tree-sitter解析器 → CST → 符号提取 → 作用域分析 → 智能补全
```

### 核心设计理念

- **Tree-sitter 提供 CST**：保留完整语法信息（括号、分号、注释等）
- **自定义符号提取**：从 CST 中提取符号信息用于补全
- **严格遵循官方设计**：使用 `TSParser` → `TSTree` → 手动遍历 `TSNode`

### 为什么使用 CST 而非 AST？

- **CST**：保留所有语法细节，支持增量解析，适合编辑器
- **AST**：抽象语法结构，丢弃格式信息，适合编译器

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
├── services/TreeSitterService.kt        # Tree-sitter Kotlin封装
├── core/ACMCompletionProvider.kt         # 主补全提供器
├── core/CompletionModels.kt              # 数据模型定义
├── providers/KeywordProvider.kt          # C++关键字补全
├── providers/STLProvider.kt              # STL补全
└── language/ACMLanguage.kt               # 语言集成
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

### 短期目标 (3-6个月)
- [ ] **Java 语法支持**：泛型、注解、Lambda 表达式
- [ ] **Python 语法支持**：动态类型推断、装饰器、生成器
- [ ] **高级 C++ 特性**：Lambda 表达式、auto 类型推导、模板特化
- [ ] **模糊匹配**：支持 "vect" 匹配 "vector"
- [ ] **智能参数提示**：函数调用时显示参数信息

### 中期目标 (6-12个月)
- [ ] **错误容忍解析**：语法错误时仍能提供补全
- [ ] **代码片段补全**：`for` 循环、`class` 定义等模板
- [ ] **继承关系分析**：支持多态和继承的成员访问
- [ ] **跨文件符号索引**：项目范围的符号查找
- [ ] **增量更新优化**：更精确的增量解析

### 长期目标 (1-2年)
- [ ] **AI 增强补全**：基于上下文的智能建议
- [ ] **代码生成**：自然语言到代码转换
- [ ] **全局符号索引**：跨项目符号查找
- [ ] **实时代码质量分析**：重构建议、代码异味检测

## 当前限制

- ❌ **只支持 C++**：Java 和 Python 解析器已集成但未实现补全逻辑
- ❌ **基础 C++ 语法**：不支持 C++20 新特性（概念、协程等）
- ❌ **简单类型推导**：不支持复杂的模板类型推导
- ❌ **单文件分析**：暂不支持跨文件的符号引用

---

**注意**：当前系统专注于 C++ ACM 竞赛编程场景，提供高精度的局部文件智能补全。Java 和 Python 的完整支持在开发计划中。