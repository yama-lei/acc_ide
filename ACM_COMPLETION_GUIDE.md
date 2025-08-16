# ACM智能补全系统说明 / ACM Intelligent Completion System Guide

## 概述 / Overview

本系统为ACM竞赛训练编辑器添加了智能的代码补全功能，包含关键字补全、本地符号表管理以及常用函数模板的建议。

This system adds intelligent code completion for ACM competition training, including keyword completion, local symbol table management, and common function template suggestions.

## 系统架构 / System Architecture

### 核心组件 / Core Components

1. **ACMCompletionProvider** - 核心补全提供器
   - 管理C++、Java、Python的关键字
   - 提供STL容器和算法建议
   - 包含ACM常用模板
   - 维护本地符号表

2. **ACMLanguage** - ACM专用语言实现
   - 扩展sora-editor的Language接口
   - 集成智能补全功能
   - 支持符号匹配

3. **HybridACMLanguage** - 混合语言实现
   - 结合TextMate语法高亮
   - 提供ACM智能补全
   - 最佳的用户体验

4. **LanguageManager** - 语言管理器
   - 管理不同补全模式
   - 根据用户偏好选择语言实现
   - 处理模式切换

## 补全模式 / Completion Modes

### 1. 混合模式 (Hybrid)
- **特点**: TextMate语法高亮 + ACM智能补全
- **适用**: 大多数用户推荐
- **优势**: 完整的语法高亮和智能补全

### 2. ACM专注模式 (ACM Focused)
- **特点**: 纯ACM补全，无TextMate依赖
- **适用**: 轻量级或TextMate有问题时
- **优势**: 快速启动，专注竞赛编程

### 3. TextMate模式 (TextMate Only)
- **特点**: 仅使用原始TextMate补全
- **适用**: 保持原有行为
- **优势**: 稳定的语法高亮

## 智能补全特性 / Intelligent Completion Features

### 关键字补全 / Keyword Completion
- **C++**: auto, vector, string, map, set, algorithm等
- **Java**: ArrayList, HashMap, Scanner, Collections等
- **Python**: list, dict, input, print, range等

### STL算法补全 / STL Algorithm Completion
```cpp
sort()          // 排序
binary_search() // 二分查找
lower_bound()   // 下界
upper_bound()   // 上界
max_element()   // 最大元素
min_element()   // 最小元素
```

### ACM模板补全 / ACM Template Completion
```cpp
gcd             // 最大公约数模板
lcm             // 最小公倍数模板
pow_mod         // 快速幂模板
fast_io         // 快速IO设置
debug           // 调试宏定义
```

### 本地符号表 / Local Symbol Table
- 自动提取变量声明
- 函数定义识别
- 类/结构体检测
- 智能优先级排序

## 优先级系统 / Priority System

1. **关键字** (Priority: 100) - 语言关键字优先
2. **STL函数** (Priority: 90) - 标准库函数
3. **常用函数** (Priority: 80) - ACM模板函数
4. **本地变量** (Priority: 70) - 用户定义变量
5. **本地函数** (Priority: 60) - 用户定义函数
6. **类型** (Priority: 50) - 用户定义类型

## 使用方法 / Usage

### 设置补全模式 / Setting Completion Mode
1. 打开设置页面
2. 找到"Completion Mode"选项
3. 选择合适的模式：
   - Hybrid (推荐)
   - ACM Focused  
   - TextMate Only

### 触发补全 / Triggering Completion
- 输入字符时自动触发
- 支持前缀匹配和模糊匹配
- 按优先级和相关性排序

### 选择建议 / Selecting Suggestions
- 方向键上下选择
- Tab或Enter确认选择
- Esc取消补全

## 文件结构 / File Structure

```
app/src/main/java/com/acc_ide/completion/
├── ACMCompletionProvider.kt     # 核心补全提供器
├── ACMLanguage.kt              # ACM语言实现
├── HybridACMLanguage.kt        # 混合语言实现
└── LanguageManager.kt          # 语言管理器
```

## 扩展指南 / Extension Guide

### 添加新关键字 / Adding New Keywords
在`ACMCompletionProvider.kt`中修改相应的关键字列表：
```kotlin
private val CPP_KEYWORDS = listOf(
    // 添加新的C++关键字
    "your_keyword"
)
```

### 添加新模板 / Adding New Templates
在`ACM_TEMPLATES`映射中添加：
```kotlin
"template_name" to "template_code"
```

### 自定义优先级 / Custom Priorities
修改`PRIORITY_*`常量来调整补全项的排序。

## 注意事项 / Notes

1. **性能**: 本地符号表会在每次补全时更新，大文件可能有轻微延迟
2. **兼容性**: 与sora-editor完全兼容，不影响现有功能
3. **内存**: 符号表使用WeakReference避免内存泄漏
4. **线程安全**: 补全操作在后台线程执行，不阻塞UI

## 故障排除 / Troubleshooting

### 补全不工作
1. 检查自动补全是否启用
2. 验证补全模式设置
3. 查看日志输出

### 语法高亮问题
1. 切换到混合模式
2. 检查TextMate主题设置
3. 重启编辑器

### 性能问题
1. 考虑使用ACM专注模式
2. 检查文件大小
3. 清理符号表

---

此系统专为ACM竞赛编程优化，提供快速、准确的代码补全体验。
This system is optimized for ACM competitive programming, providing fast and accurate code completion.