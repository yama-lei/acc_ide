#include <jni.h>
#include <string>
#include <vector>
#include <memory>
#include "tree_sitter/api.h"

// TreeSitter语言声明
extern "C" {
    TSLanguage *tree_sitter_cpp();
    TSLanguage *tree_sitter_java();
    TSLanguage *tree_sitter_python();
}

// 符号类型枚举
enum SymbolType {
    VARIABLE = 0,
    FUNCTION = 1,
    CLASS = 2,
    STRUCT = 3,
    ENUM = 4,
    PARAMETER = 5
};

// 作用域类型枚举
enum ScopeType {
    FUNCTION_SCOPE = 0,
    CLASS_SCOPE = 1,
    BLOCK_SCOPE = 2,
    NAMESPACE_SCOPE = 3,
    GLOBAL_SCOPE = 4
};

// 符号信息结构
struct SymbolInfo {
    std::string name;
    SymbolType type;
    std::string dataType;
    int line;
    int column;
    int scopeLevel;
    std::string description;
};

// 作用域信息结构
struct ScopeInfo {
    int level;
    int startLine;
    int endLine;
    ScopeType type;
};

// 解析结果结构
struct ParseResult {
    std::vector<SymbolInfo> symbols;
    std::vector<ScopeInfo> scopes;
};

// 工具函数：创建Java SymbolInfo对象
jobject createSymbolInfo(JNIEnv *env, const SymbolInfo &symbol) {
    jclass symbolClass = env->FindClass("com/acc_ide/completion/core/SymbolInfo");
    jmethodID constructor = env->GetMethodID(symbolClass, "<init>", 
        "(Ljava/lang/String;Lcom/acc_ide/completion/core/SymbolType;Ljava/lang/String;IIILjava/lang/String;)V");
    
    jstring name = env->NewStringUTF(symbol.name.c_str());
    jstring dataType = env->NewStringUTF(symbol.dataType.c_str());
    jstring description = env->NewStringUTF(symbol.description.c_str());
    
    // 获取SymbolType枚举
    jclass symbolTypeClass = env->FindClass("com/acc_ide/completion/core/SymbolType");
    jfieldID typeField;
    
    switch (symbol.type) {
        case VARIABLE:
            typeField = env->GetStaticFieldID(symbolTypeClass, "VARIABLE", "Lcom/acc_ide/completion/core/SymbolType;");
            break;
        case FUNCTION:
            typeField = env->GetStaticFieldID(symbolTypeClass, "FUNCTION", "Lcom/acc_ide/completion/core/SymbolType;");
            break;
        case CLASS:
            typeField = env->GetStaticFieldID(symbolTypeClass, "CLASS", "Lcom/acc_ide/completion/core/SymbolType;");
            break;
        case STRUCT:
            typeField = env->GetStaticFieldID(symbolTypeClass, "STRUCT", "Lcom/acc_ide/completion/core/SymbolType;");
            break;
        case ENUM:
            typeField = env->GetStaticFieldID(symbolTypeClass, "ENUM", "Lcom/acc_ide/completion/core/SymbolType;");
            break;
        case PARAMETER:
            typeField = env->GetStaticFieldID(symbolTypeClass, "PARAMETER", "Lcom/acc_ide/completion/core/SymbolType;");
            break;
        default:
            typeField = env->GetStaticFieldID(symbolTypeClass, "VARIABLE", "Lcom/acc_ide/completion/core/SymbolType;");
    }
    
    jobject symbolTypeObj = env->GetStaticObjectField(symbolTypeClass, typeField);
    
    return env->NewObject(symbolClass, constructor, name, symbolTypeObj, dataType, 
                         symbol.line, symbol.column, symbol.scopeLevel, description);
}

// 工具函数：创建Java ScopeInfo对象
jobject createScopeInfo(JNIEnv *env, const ScopeInfo &scope) {
    jclass scopeClass = env->FindClass("com/acc_ide/completion/core/ScopeInfo");
    jmethodID constructor = env->GetMethodID(scopeClass, "<init>", 
        "(IIILcom/acc_ide/completion/core/ScopeType;)V");
    
    // 获取ScopeType枚举
    jclass scopeTypeClass = env->FindClass("com/acc_ide/completion/core/ScopeType");
    jfieldID typeField;
    
    switch (scope.type) {
        case FUNCTION_SCOPE:
            typeField = env->GetStaticFieldID(scopeTypeClass, "FUNCTION", "Lcom/acc_ide/completion/core/ScopeType;");
            break;
        case CLASS_SCOPE:
            typeField = env->GetStaticFieldID(scopeTypeClass, "CLASS", "Lcom/acc_ide/completion/core/ScopeType;");
            break;
        case BLOCK_SCOPE:
            typeField = env->GetStaticFieldID(scopeTypeClass, "BLOCK", "Lcom/acc_ide/completion/core/ScopeType;");
            break;
        case NAMESPACE_SCOPE:
            typeField = env->GetStaticFieldID(scopeTypeClass, "NAMESPACE", "Lcom/acc_ide/completion/core/ScopeType;");
            break;
        case GLOBAL_SCOPE:
            typeField = env->GetStaticFieldID(scopeTypeClass, "GLOBAL", "Lcom/acc_ide/completion/core/ScopeType;");
            break;
        default:
            typeField = env->GetStaticFieldID(scopeTypeClass, "BLOCK", "Lcom/acc_ide/completion/core/ScopeType;");
    }
    
    jobject scopeTypeObj = env->GetStaticObjectField(scopeTypeClass, typeField);
    
    return env->NewObject(scopeClass, constructor, scope.level, scope.startLine, scope.endLine, scopeTypeObj);
}

// 工具函数：创建Java ParseResult对象
jobject createParseResult(JNIEnv *env, const ParseResult &result) {
    jclass parseResultClass = env->FindClass("com/acc_ide/completion/core/ParseResult");
    if (!parseResultClass) return nullptr;
    
    jmethodID constructor = env->GetMethodID(parseResultClass, "<init>", 
        "(Ljava/util/List;Ljava/util/List;)V");
    if (!constructor) return nullptr;
    
    // 创建symbols列表
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    if (!arrayListClass) return nullptr;
    
    jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "()V");
    jmethodID addMethod = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    if (!arrayListConstructor || !addMethod) return nullptr;
    
    jobject symbolsList = env->NewObject(arrayListClass, arrayListConstructor);
    for (const auto &symbol : result.symbols) {
        jobject symbolObj = createSymbolInfo(env, symbol);
        if (symbolObj) {
            env->CallBooleanMethod(symbolsList, addMethod, symbolObj);
        }
    }
    
    jobject scopesList = env->NewObject(arrayListClass, arrayListConstructor);
    for (const auto &scope : result.scopes) {
        jobject scopeObj = createScopeInfo(env, scope);
        if (scopeObj) {
            env->CallBooleanMethod(scopesList, addMethod, scopeObj);
        }
    }
    
    return env->NewObject(parseResultClass, constructor, symbolsList, scopesList);
}

// 递归遍历AST节点
void traverseNode(TSNode node, const std::string &source, ParseResult &result, int scopeLevel = 0) {
    const char *nodeType = ts_node_type(node);
    std::string type(nodeType);
    
    TSPoint startPoint = ts_node_start_point(node);
    TSPoint endPoint = ts_node_end_point(node);
    
    // 根据节点类型提取符号
    if (type == "function_definition" || type == "method_declaration") {
        // 查找函数名
        for (uint32_t i = 0; i < ts_node_child_count(node); i++) {
            TSNode child = ts_node_child(node, i);
            const char *childType = ts_node_type(child);
            
            if (strcmp(childType, "identifier") == 0) {
                uint32_t start = ts_node_start_byte(child);
                uint32_t end = ts_node_end_byte(child);
                std::string name = source.substr(start, end - start);
                
                SymbolInfo symbol;
                symbol.name = name;
                symbol.type = FUNCTION;
                symbol.dataType = "function";
                symbol.line = startPoint.row;
                symbol.column = startPoint.column;
                symbol.scopeLevel = scopeLevel;
                symbol.description = "Function definition";
                
                result.symbols.push_back(symbol);
                break;
            }
        }
        
        // 添加函数作用域
        ScopeInfo scope;
        scope.level = scopeLevel + 1;
        scope.startLine = startPoint.row;
        scope.endLine = endPoint.row;
        scope.type = FUNCTION_SCOPE;
        result.scopes.push_back(scope);
        
        scopeLevel++;
    }
    else if (type == "class_definition" || type == "class_declaration" || type == "struct_specifier") {
        // 查找类名
        for (uint32_t i = 0; i < ts_node_child_count(node); i++) {
            TSNode child = ts_node_child(node, i);
            const char *childType = ts_node_type(child);
            
            if (strcmp(childType, "identifier") == 0 || strcmp(childType, "type_identifier") == 0) {
                uint32_t start = ts_node_start_byte(child);
                uint32_t end = ts_node_end_byte(child);
                std::string name = source.substr(start, end - start);
                
                SymbolInfo symbol;
                symbol.name = name;
                symbol.type = (type == "struct_specifier") ? STRUCT : CLASS;
                symbol.dataType = (type == "struct_specifier") ? "struct" : "class";
                symbol.line = startPoint.row;
                symbol.column = startPoint.column;
                symbol.scopeLevel = scopeLevel;
                symbol.description = symbol.dataType + " definition";
                
                result.symbols.push_back(symbol);
                break;
            }
        }
        
        // 添加类作用域
        ScopeInfo scope;
        scope.level = scopeLevel + 1;
        scope.startLine = startPoint.row;
        scope.endLine = endPoint.row;
        scope.type = CLASS_SCOPE;
        result.scopes.push_back(scope);
        
        scopeLevel++;
    }
    else if (type == "declaration" || type == "variable_declaration") {
        // 查找变量声明
        for (uint32_t i = 0; i < ts_node_child_count(node); i++) {
            TSNode child = ts_node_child(node, i);
            const char *childType = ts_node_type(child);
            
            if (strcmp(childType, "identifier") == 0) {
                uint32_t start = ts_node_start_byte(child);
                uint32_t end = ts_node_end_byte(child);
                std::string name = source.substr(start, end - start);
                
                SymbolInfo symbol;
                symbol.name = name;
                symbol.type = VARIABLE;
                symbol.dataType = "auto"; // 简化处理
                symbol.line = startPoint.row;
                symbol.column = startPoint.column;
                symbol.scopeLevel = scopeLevel;
                symbol.description = "Variable declaration";
                
                result.symbols.push_back(symbol);
            }
        }
    }
    else if (type == "compound_statement" || type == "block") {
        // 添加块作用域
        ScopeInfo scope;
        scope.level = scopeLevel + 1;
        scope.startLine = startPoint.row;
        scope.endLine = endPoint.row;
        scope.type = BLOCK_SCOPE;
        result.scopes.push_back(scope);
        
        scopeLevel++;
    }
    
    // 递归处理子节点
    uint32_t childCount = ts_node_child_count(node);
    for (uint32_t i = 0; i < childCount; i++) {
        TSNode child = ts_node_child(node, i);
        traverseNode(child, source, result, scopeLevel);
    }
}

// 通用解析函数
ParseResult parseWithLanguage(const std::string &code, TSLanguage *language) {
    ParseResult result;
    
    TSParser *parser = ts_parser_new();
    ts_parser_set_language(parser, language);
    
    TSTree *tree = ts_parser_parse_string(parser, nullptr, code.c_str(), code.length());
    
    if (tree) {
        TSNode rootNode = ts_tree_root_node(tree);
        traverseNode(rootNode, code, result);
        ts_tree_delete(tree);
    }
    
    ts_parser_delete(parser);
    return result;
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_acc_1ide_completion_services_NativeTreeSitterService_parseCppCode(JNIEnv *env, jobject thiz, jstring code) {
    const char *codeStr = env->GetStringUTFChars(code, nullptr);
    std::string codeString(codeStr);
    env->ReleaseStringUTFChars(code, codeStr);
    
    try {
        ParseResult result = parseWithLanguage(codeString, tree_sitter_cpp());
        return createParseResult(env, result);
    } catch (...) {
        return nullptr;
    }
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_acc_1ide_completion_services_NativeTreeSitterService_parseJavaCode(JNIEnv *env, jobject thiz, jstring code) {
    const char *codeStr = env->GetStringUTFChars(code, nullptr);
    std::string codeString(codeStr);
    env->ReleaseStringUTFChars(code, codeStr);
    
    try {
        ParseResult result = parseWithLanguage(codeString, tree_sitter_java());
        return createParseResult(env, result);
    } catch (...) {
        return nullptr;
    }
}

extern "C" JNIEXPORT jobject JNICALL
Java_com_acc_1ide_completion_services_NativeTreeSitterService_parsePythonCode(JNIEnv *env, jobject thiz, jstring code) {
    const char *codeStr = env->GetStringUTFChars(code, nullptr);
    std::string codeString(codeStr);
    env->ReleaseStringUTFChars(code, codeStr);
    
    try {
        ParseResult result = parseWithLanguage(codeString, tree_sitter_python());
        return createParseResult(env, result);
    } catch (...) {
        return nullptr;
    }
}