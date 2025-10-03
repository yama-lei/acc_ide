# TreeSitter Core Library

[![](https://img.shields.io/badge/Android-3DDC84?style=flat&logo=android&logoColor=white)]()
[![](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white)]()
[![](https://img.shields.io/badge/C++-00599C?style=flat&logo=cplusplus&logoColor=white)]()

Pure Tree-sitter CST (Concrete Syntax Tree) parsing library for Android.

## 🎯 Features

- ✅ **Pure CST Parsing**: No external dependencies
- ✅ **Multi-Language Support**: C++, Java, Python
- ✅ **Symbol Extraction**: Variables, functions, classes, structs, enums
- ✅ **Scope Analysis**: Function, class, block, namespace scopes
- ✅ **Native Performance**: C++ implementation with JNI bridge
- ✅ **Framework Agnostic**: Works with any completion engine

## 📦 Installation

### Local Module

```gradle
// settings.gradle.kts
include(":treesitter-core")

// app/build.gradle
dependencies {
    implementation project(':treesitter-core')
}
```

### JitPack (Coming Soon)

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    implementation 'com.github.acc_ide:treesitter-core:1.0.0'
}
```

## 🚀 Quick Start

```kotlin
val service = TreeSitterService()

// Parse source code
val result = service.parseCode("""
    int main() {
        int x = 10;
        string text = "hello";
        return 0;
    }
""".trimIndent(), "cpp")

// Get symbols at specific position
val symbols = service.getSymbolsAtPosition(code, "cpp", line = 2, column = 5)

symbols.forEach { symbol ->
    println("${symbol.name}: ${symbol.dataType} (${symbol.type})")
}
// Output:
// x: int (VARIABLE)
// text: string (VARIABLE)
```

## 📚 API Reference

### TreeSitterService

Main service class for parsing source code.

```kotlin
class TreeSitterService {
    /**
     * Parse source code and extract symbols
     * @param code Source code as string
     * @param language Language identifier ("cpp", "java", "python")
     * @return ParseResult with symbols and scopes, or null if failed
     */
    fun parseCode(code: String, language: String): ParseResult?
    
    /**
     * Get symbols visible at specific position
     * @param code Source code as string
     * @param language Language identifier
     * @param line Line number (0-based)
     * @param column Column number (0-based)
     * @return List of visible symbols at the given position
     */
    fun getSymbolsAtPosition(
        code: String, 
        language: String, 
        line: Int, 
        column: Int
    ): List<SymbolInfo>
    
    /**
     * Check if Tree-sitter service is available
     */
    fun isAvailable(): Boolean
}
```

### Data Models

```kotlin
/**
 * Symbol extracted from Tree-sitter AST
 */
data class SymbolInfo(
    val name: String,
    val type: SymbolType,
    val dataType: String,
    val line: Int,
    val column: Int,
    val scopeLevel: Int,
    val description: String = "",
    val parentStruct: String = ""
)

enum class SymbolType {
    VARIABLE, FUNCTION, CLASS, STRUCT, ENUM, PARAMETER, STRUCT_MEMBER
}

/**
 * Scope information for visibility analysis
 */
data class ScopeInfo(
    val level: Int,
    val startLine: Int,
    val endLine: Int,
    val type: ScopeType
)

enum class ScopeType {
    SCOPE_FUNCTION, SCOPE_CLASS, SCOPE_BLOCK, SCOPE_NAMESPACE, SCOPE_GLOBAL
}

/**
 * Parse result
 */
data class ParseResult(
    val symbols: List<SymbolInfo>,
    val scopes: List<ScopeInfo>
)
```

## 🔧 Supported Languages

| Language | Symbol Types | Scope Analysis |
|----------|-------------|----------------|
| **C++** | Variables, Functions, Classes, Structs, Enums, Parameters | ✅ Full support |
| **Java** | Variables, Functions, Classes, Parameters | ✅ Full support |
| **Python** | Variables, Functions, Classes, Parameters | ✅ Full support |

## 🏗️ Architecture

```
treesitter-core/
├── src/main/
│   ├── cpp/                          # Native C++ implementation
│   │   ├── core/                     # Core abstractions
│   │   │   ├── AbstractTreeSitterProcessor.h/.cpp
│   │   │   ├── ILanguageProcessor.h
│   │   │   └── TreeSitterRegistry.h/.cpp
│   │   ├── languages/                # Language-specific processors
│   │   │   ├── CppLanguageProcessor.h/.cpp
│   │   │   ├── JavaLanguageProcessor.h/.cpp
│   │   │   └── PythonLanguageProcessor.h/.cpp
│   │   └── TreeSitterJNI.cpp         # JNI bridge
│   ├── java/com/acc_ide/treesitter/
│   │   ├── core/                     # Data models
│   │   │   └── TreeSitterModels.kt
│   │   └── TreeSitterService.kt      # Main service
│   └── jniLibs/                      # Compiled native libraries
└── build.gradle
```

## 🎓 Design Philosophy

This library follows Tree-sitter's official design principles:

1. **CST-First**: Uses Concrete Syntax Tree, not Abstract Syntax Tree
2. **Incremental Parsing**: Efficient re-parsing of modified code
3. **Error Recovery**: Continues parsing even with syntax errors
4. **Manual Traversal**: Direct TSNode traversal for symbol extraction
5. **No LSP**: Lightweight alternative to Language Server Protocol

## 🔬 Use Cases

- **Code Completion**: Build intelligent completion engines
- **Code Analysis**: Extract symbols for linting/analysis
- **Syntax Highlighting**: Scope-aware highlighting
- **Code Navigation**: Jump to definition, find references
- **Refactoring Tools**: Symbol renaming, code transformation

## ⚙️ Requirements

- **Android API 24+**
- **NDK r27+**
- **CMake 3.22+**
- **Kotlin 2.1+**

## 📝 ProGuard Rules

```proguard
# Keep all public API
-keep public class com.acc_ide.treesitter.** { public *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}
```

## 🤝 Related Libraries

- **completion-library**: High-level completion framework based on treesitter-core
- **executor-library**: Code execution engine

## 📄 License

Part of the ACC IDE project.

## 🔗 Links

- [Main Repository](https://github.com/yourusername/acc_ide)
- [Tree-sitter Official Site](https://tree-sitter.github.io/tree-sitter/)

