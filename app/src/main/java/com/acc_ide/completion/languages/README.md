# 多语言支持实现

基于 TreeSitter 的本地变量识别和智能补全系统，现已完善支持 C++、Java 和 Python 三种语言。

## 实现概览

### 语言支持类 (LanguageSupport)

每种语言都有对应的 LanguageSupport 类，集成了 TextMate 语法高亮和 TreeSitter 符号提取：

- **CppLanguageSupport.kt** - C++ 语言完整支持
- **JavaLanguageSupport.kt** - Java 语言完整支持  
- **PythonLanguageSupport.kt** - Python 语言完整支持

### 语言处理器 (LanguageProcessor)

每种语言的核心补全逻辑由对应的处理器实现：

- **CppLanguageProcessor.kt** - C++ 补全逻辑（STL、算法、符号）
- **JavaLanguageProcessor.kt** - Java 补全逻辑（集合框架、工具类、符号）
- **PythonLanguageProcessor.kt** - Python 补全逻辑（内置类型、模块、符号）

### 静态补全库 (Static Libraries)

提供每种语言的关键字、标准库和常用函数补全：

- **CppStaticLibrary.kt** - C++ STL 容器、算法、关键字
- **JavaStaticLibrary.kt** - Java 集合框架、工具类、关键字
- **PythonStaticLibrary.kt** - Python 内置函数、模块、关键字

## 核心功能

### 1. TreeSitter 符号提取
- ✅ **本地变量识别** - 自动识别函数内的局部变量
- ✅ **函数参数识别** - 识别函数参数并提供补全
- ✅ **类成员识别** - 识别 struct/class 成员变量和方法
- ✅ **作用域感知** - 根据当前作用域过滤可见符号
- ✅ **类型推断** - 基础的变量类型推断

### 2. 成员访问补全
- ✅ **C++**: `obj.` 和 `obj->` 操作符支持
- ✅ **Java**: `obj.` 操作符支持
- ✅ **Python**: `obj.` 操作符支持
- ✅ **STL/集合/内置类型成员方法补全**
- ✅ **用户定义类型成员补全**

### 3. 语言特定补全

#### C++ 支持
- STL 容器：vector, string, map, set, queue 等
- 算法函数：sort, find, binary_search 等
- 数据结构：pair, tuple 等
- 竞赛编程常用模板

#### Java 支持  
- 集合框架：ArrayList, HashMap, HashSet 等
- 工具类：Math, Arrays, Collections, Scanner 等
- 基本类型包装类：Integer, String 等
- 竞赛编程常用常量

#### Python 支持
- 内置类型：list, dict, set, str 等
- 内置函数：len, max, min, sorted 等  
- 常用模块：math, collections, itertools 等
- 竞赛编程相关函数

## 使用方式

### 自动语言检测

LanguageManager 会根据文件扩展名自动选择对应的语言支持：

```kotlin
// 在 LanguageManager.kt 中
fun getLanguageForFile(fileName: String, fileExtension: String): Language {
    return when (fileExtension.lowercase()) {
        "cpp", "c++", "cc", "cxx", "h", "hpp" -> CppLanguageSupport("source.cpp")
        "java" -> JavaLanguageSupport("source.java") 
        "py", "python" -> PythonLanguageSupport("source.python")
        else -> CppLanguageSupport("text.plain") // 默认C++
    }
}
```

### 手动测试

可以使用 `LanguageSupportTest.kt` 进行测试：

```kotlin
// 测试Java补全
LanguageSupportTest.testJavaLanguageSupport()

// 测试Python补全  
LanguageSupportTest.testPythonLanguageSupport()

// 运行所有测试
LanguageSupportTest.runAllTests()
```

## 补全效果示例

### Java 示例
```java
import java.util.*;

public class Test {
    public static void main(String[] args) {
        ArrayList<Integer> list = new ArrayList<>();
        String text = "Hello";
        Scanner scanner = new Scanner(System.in);
        
        list.    // 自动显示：add, get, set, remove, size, clear 等
        text.    // 自动显示：length, charAt, substring, indexOf 等
        scanner. // 自动显示：nextInt, nextLine, hasNext 等
    }
}
```

### Python 示例
```python
import math
from collections import Counter

def main():
    numbers = [1, 2, 3, 4, 5]
    text = "hello world"
    counter = Counter()
    
    numbers.  # 自动显示：append, extend, remove, pop, sort 等
    text.     # 自动显示：split, join, replace, find, strip 等
    math.     # 自动显示：sqrt, pow, ceil, floor, sin, cos 等
```

### C++ 示例
```cpp
#include <vector>
#include <string>

int main() {
    std::vector<int> nums;
    std::string text = "hello";
    
    nums.  // 自动显示：push_back, size, empty, clear, at 等
    text.  // 自动显示：length, substr, find, replace, c_str 等
    
    return 0;
}
```

## 技术架构

### 处理流程
1. **语法高亮** - TextMate 提供语法高亮
2. **符号提取** - TreeSitter 解析代码获取符号信息
3. **作用域分析** - 确定当前位置可见的符号
4. **类型推断** - 推断变量类型以提供成员补全
5. **补全合并** - 合并 TreeSitter 符号和静态库补全
6. **优先级排序** - 按相关性和使用频率排序

### 错误处理
- TreeSitter 解析失败时降级到关键字补全
- 类型识别失败时提供通用补全
- 完整的异常处理和日志记录

## 性能特性
- ✅ **原生性能** - C++ TreeSitter 核心，JNI 桥接
- ✅ **增量解析** - TreeSitter 支持增量更新  
- ✅ **符号缓存** - 避免重复解析
- ✅ **异步处理** - 后台线程，不阻塞 UI

## 扩展新语言

要添加新语言支持，需要：

1. 在 `providers/` 下创建静态库类
2. 在 `languages/` 下创建语言处理器和支持类
3. 在 TreeSitter C++ 层添加对应语言解析器
4. 在 `LanguageManager.kt` 中注册新语言

这个实现为 ACC IDE 提供了完整的多语言智能补全支持，特别适合竞赛编程环境的需求。