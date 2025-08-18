#include <jni.h>
#include <string>
#include <vector>
#include <memory>
#include <android/log.h>
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
    PARAMETER = 5,
    STRUCT_MEMBER = 6
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
    std::string parentStruct; // 用于struct成员，记录所属的struct名称
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
        "(Ljava/lang/String;Lcom/acc_ide/completion/core/SymbolType;Ljava/lang/String;IIILjava/lang/String;Ljava/lang/String;)V");
    
    jstring name = env->NewStringUTF(symbol.name.c_str());
    jstring dataType = env->NewStringUTF(symbol.dataType.c_str());
    jstring description = env->NewStringUTF(symbol.description.c_str());
    jstring parentStruct = env->NewStringUTF(symbol.parentStruct.c_str());
    
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
        case STRUCT_MEMBER:
            typeField = env->GetStaticFieldID(symbolTypeClass, "STRUCT_MEMBER", "Lcom/acc_ide/completion/core/SymbolType;");
            break;
        default:
            typeField = env->GetStaticFieldID(symbolTypeClass, "VARIABLE", "Lcom/acc_ide/completion/core/SymbolType;");
    }
    
    jobject symbolTypeObj = env->GetStaticObjectField(symbolTypeClass, typeField);
    
    return env->NewObject(symbolClass, constructor, name, symbolTypeObj, dataType, 
                         symbol.line, symbol.column, symbol.scopeLevel, description, parentStruct);
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
        "(Ljava/util/List;Ljava/util/List;Ljava/lang/String;)V");
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
    
    // 创建parseTree参数（设为null）
    jstring parseTree = nullptr;
    
    return env->NewObject(parseResultClass, constructor, symbolsList, scopesList, parseTree);
}

// 函数声明
void extractVariableNamesFromDeclarator(TSNode node, const std::string &source, std::vector<std::string> &varNames);
void extractStructMembers(TSNode node, const std::string &source, ParseResult &result, const std::string &structName, int scopeLevel);

// 提取变量声明信息
void extractVariableInfo(TSNode node, const std::string &source, std::string &varType, std::vector<std::string> &varNames) {
    __android_log_print(ANDROID_LOG_DEBUG, "TreeSitterJNI", 
        "extractVariableInfo: node has %d children", ts_node_child_count(node));
        
    // 查找类型节点
    for (uint32_t i = 0; i < ts_node_child_count(node); i++) {
        TSNode child = ts_node_child(node, i);
        const char *childType = ts_node_type(child);
        
        __android_log_print(ANDROID_LOG_DEBUG, "TreeSitterJNI", 
            "  Child %d: %s", i, childType);
        
        // 查找基本类型 (primitive_type, type_identifier)
        if (strcmp(childType, "primitive_type") == 0 || strcmp(childType, "type_identifier") == 0) {
            uint32_t start = ts_node_start_byte(child);
            uint32_t end = ts_node_end_byte(child);
            varType = source.substr(start, end - start);
            __android_log_print(ANDROID_LOG_DEBUG, "TreeSitterJNI", 
                "  Found type: %s", varType.c_str());
        }
        // 查找初始化声明器列表
        else if (strcmp(childType, "init_declarator") == 0 || strcmp(childType, "declarator") == 0) {
            __android_log_print(ANDROID_LOG_DEBUG, "TreeSitterJNI", 
                "  Extracting from declarator");
            extractVariableNamesFromDeclarator(child, source, varNames);
        }
        // 直接的标识符（包括field_identifier用于struct成员）
        else if (strcmp(childType, "identifier") == 0 || strcmp(childType, "field_identifier") == 0) {
            uint32_t start = ts_node_start_byte(child);
            uint32_t end = ts_node_end_byte(child);
            std::string name = source.substr(start, end - start);
            varNames.push_back(name);
            __android_log_print(ANDROID_LOG_DEBUG, "TreeSitterJNI", 
                "  Found %s: %s", childType, name.c_str());
        }
        // 递归查找
        else {
            extractVariableInfo(child, source, varType, varNames);
        }
    }
}

// 从声明器中提取变量名
void extractVariableNamesFromDeclarator(TSNode node, const std::string &source, std::vector<std::string> &varNames) {
    const char *nodeType = ts_node_type(node);
    
    // 如果是标识符（包括field_identifier），直接添加
    if (strcmp(nodeType, "identifier") == 0 || strcmp(nodeType, "field_identifier") == 0) {
        uint32_t start = ts_node_start_byte(node);
        uint32_t end = ts_node_end_byte(node);
        std::string name = source.substr(start, end - start);
        varNames.push_back(name);
        return;
    }
    
    // 递归查找子节点中的标识符
    for (uint32_t i = 0; i < ts_node_child_count(node); i++) {
        TSNode child = ts_node_child(node, i);
        const char *childType = ts_node_type(child);
        
        if (strcmp(childType, "identifier") == 0 || strcmp(childType, "field_identifier") == 0) {
            uint32_t start = ts_node_start_byte(child);
            uint32_t end = ts_node_end_byte(child);
            std::string name = source.substr(start, end - start);
            varNames.push_back(name);
        } else {
            // 递归查找，处理复杂的声明器结构
            extractVariableNamesFromDeclarator(child, source, varNames);
        }
    }
}

// 提取struct成员
void extractStructMembers(TSNode node, const std::string &source, ParseResult &result, const std::string &structName, int scopeLevel) {
    __android_log_print(ANDROID_LOG_DEBUG, "TreeSitterJNI", 
        "extractStructMembers: processing %d children for struct %s", ts_node_child_count(node), structName.c_str());
    
    for (uint32_t i = 0; i < ts_node_child_count(node); i++) {
        TSNode child = ts_node_child(node, i);
        const char *childType = ts_node_type(child);
        
        __android_log_print(ANDROID_LOG_DEBUG, "TreeSitterJNI", 
            "  Struct member child %d: %s", i, childType);
        
        // 查找field_declaration（成员声明）
        if (strcmp(childType, "field_declaration") == 0) {
            std::string memberType = "auto";
            std::vector<std::string> memberNames;
            
            // 提取成员类型和名称
            extractVariableInfo(child, source, memberType, memberNames);
            
            // 为每个成员创建符号
            for (const std::string &memberName : memberNames) {
                __android_log_print(ANDROID_LOG_DEBUG, "TreeSitterJNI", 
                    "Creating struct member: %s.%s of type %s", structName.c_str(), memberName.c_str(), memberType.c_str());
                
                TSPoint memberPoint = ts_node_start_point(child);
                
                SymbolInfo symbol;
                symbol.name = memberName;
                symbol.type = STRUCT_MEMBER;
                symbol.dataType = memberType;
                symbol.line = memberPoint.row;
                symbol.column = memberPoint.column;
                symbol.scopeLevel = scopeLevel;
                symbol.description = "Struct member of " + structName;
                symbol.parentStruct = structName;
                
                result.symbols.push_back(symbol);
            }
        }
        // 递归处理子节点
        else {
            extractStructMembers(child, source, result, structName, scopeLevel);
        }
    }
}

// 递归遍历AST节点
void traverseNode(TSNode node, const std::string &source, ParseResult &result, int scopeLevel = 0) {
    const char *nodeType = ts_node_type(node);
    std::string type(nodeType);
    
    TSPoint startPoint = ts_node_start_point(node);
    TSPoint endPoint = ts_node_end_point(node);
    
    // 添加详细的调试日志
    __android_log_print(ANDROID_LOG_DEBUG, "TreeSitterJNI", 
        "Traversing node: %s at %d:%d", type.c_str(), startPoint.row, startPoint.column);
    
    // 根据节点类型提取符号
    if (type == "function_definition" || type == "method_declaration") {
        __android_log_print(ANDROID_LOG_DEBUG, "TreeSitterJNI", 
            "Found function definition node at %d:%d", startPoint.row, startPoint.column);
            
        // 查找函数名（通常在function_declarator中）
        for (uint32_t i = 0; i < ts_node_child_count(node); i++) {
            TSNode child = ts_node_child(node, i);
            const char *childType = ts_node_type(child);
            
            __android_log_print(ANDROID_LOG_DEBUG, "TreeSitterJNI", 
                "  Function child %d: %s", i, childType);
            
            // 查找function_declarator
            if (strcmp(childType, "function_declarator") == 0) {
                // 在function_declarator中查找identifier
                for (uint32_t j = 0; j < ts_node_child_count(child); j++) {
                    TSNode grandchild = ts_node_child(child, j);
                    const char *grandchildType = ts_node_type(grandchild);
                    
                    if (strcmp(grandchildType, "identifier") == 0) {
                        uint32_t start = ts_node_start_byte(grandchild);
                        uint32_t end = ts_node_end_byte(grandchild);
                        std::string name = source.substr(start, end - start);
                        
                        __android_log_print(ANDROID_LOG_DEBUG, "TreeSitterJNI", 
                            "Creating function symbol: %s", name.c_str());
                        
                        SymbolInfo symbol;
                        symbol.name = name;
                        symbol.type = FUNCTION;
                        symbol.dataType = "function";
                        symbol.line = startPoint.row;
                        symbol.column = startPoint.column;
                        symbol.scopeLevel = scopeLevel;
                        symbol.description = "Function definition";
                        symbol.parentStruct = "";
                        
                        result.symbols.push_back(symbol);
                        break;
                    }
                }
                break; // 找到function_declarator后就退出
            }
            // 如果直接找到identifier（某些简单情况）
            else if (strcmp(childType, "identifier") == 0) {
                uint32_t start = ts_node_start_byte(child);
                uint32_t end = ts_node_end_byte(child);
                std::string name = source.substr(start, end - start);
                
                __android_log_print(ANDROID_LOG_DEBUG, "TreeSitterJNI", 
                    "Creating function symbol (direct): %s", name.c_str());
                
                SymbolInfo symbol;
                symbol.name = name;
                symbol.type = FUNCTION;
                symbol.dataType = "function";
                symbol.line = startPoint.row;
                symbol.column = startPoint.column;
                symbol.scopeLevel = scopeLevel;
                symbol.description = "Function definition";
                symbol.parentStruct = "";
                
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
        std::string structName = "";
        
        // 查找类名/结构体名
        for (uint32_t i = 0; i < ts_node_child_count(node); i++) {
            TSNode child = ts_node_child(node, i);
            const char *childType = ts_node_type(child);
            
            if (strcmp(childType, "identifier") == 0 || strcmp(childType, "type_identifier") == 0) {
                uint32_t start = ts_node_start_byte(child);
                uint32_t end = ts_node_end_byte(child);
                structName = source.substr(start, end - start);
                
                SymbolInfo symbol;
                symbol.name = structName;
                symbol.type = (type == "struct_specifier") ? STRUCT : CLASS;
                symbol.dataType = (type == "struct_specifier") ? "struct" : "class";
                symbol.line = startPoint.row;
                symbol.column = startPoint.column;
                symbol.scopeLevel = scopeLevel;
                symbol.description = symbol.dataType + " definition";
                symbol.parentStruct = "";
                
                result.symbols.push_back(symbol);
                break;
            }
        }
        
        // 如果是struct，需要查找成员变量
        if (type == "struct_specifier" && !structName.empty()) {
            __android_log_print(ANDROID_LOG_DEBUG, "TreeSitterJNI", 
                "Processing struct %s members", structName.c_str());
            
            // 查找field_declaration_list（struct体）
            for (uint32_t i = 0; i < ts_node_child_count(node); i++) {
                TSNode child = ts_node_child(node, i);
                const char *childType = ts_node_type(child);
                
                if (strcmp(childType, "field_declaration_list") == 0) {
                    // 递归查找struct成员
                    extractStructMembers(child, source, result, structName, scopeLevel + 1);
                    break;
                }
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
        __android_log_print(ANDROID_LOG_DEBUG, "TreeSitterJNI", 
            "Found variable declaration node at %d:%d", startPoint.row, startPoint.column);
            
        // 提取变量类型和名称
        std::string varType = "auto";
        std::vector<std::string> varNames;
        
        // 递归查找类型信息和变量名
        extractVariableInfo(node, source, varType, varNames);
        
        __android_log_print(ANDROID_LOG_DEBUG, "TreeSitterJNI", 
            "Extracted type: %s, found %d variables", varType.c_str(), (int)varNames.size());
        
        // 为每个变量创建符号
        for (const std::string &name : varNames) {
            __android_log_print(ANDROID_LOG_DEBUG, "TreeSitterJNI", 
                "Creating symbol: %s of type %s", name.c_str(), varType.c_str());
                
            SymbolInfo symbol;
            symbol.name = name;
            symbol.type = VARIABLE;
            symbol.dataType = varType;
            symbol.line = startPoint.row;
            symbol.column = startPoint.column;
            symbol.scopeLevel = scopeLevel;
            symbol.description = "Variable declaration";
            symbol.parentStruct = "";
            
            result.symbols.push_back(symbol);
        }
    }
    else if (type == "preproc_def") {
        __android_log_print(ANDROID_LOG_DEBUG, "TreeSitterJNI", 
            "Found macro definition node at %d:%d", startPoint.row, startPoint.column);
            
        // 查找宏名称
        for (uint32_t i = 0; i < ts_node_child_count(node); i++) {
            TSNode child = ts_node_child(node, i);
            const char *childType = ts_node_type(child);
            
            if (strcmp(childType, "identifier") == 0) {
                uint32_t start = ts_node_start_byte(child);
                uint32_t end = ts_node_end_byte(child);
                std::string name = source.substr(start, end - start);
                
                __android_log_print(ANDROID_LOG_DEBUG, "TreeSitterJNI", 
                    "Creating macro symbol: %s", name.c_str());
                
                SymbolInfo symbol;
                symbol.name = name;
                symbol.type = VARIABLE; // 暂时使用VARIABLE类型，后续可以考虑添加MACRO类型
                symbol.dataType = "macro";
                symbol.line = startPoint.row;
                symbol.column = startPoint.column;
                symbol.scopeLevel = scopeLevel;
                symbol.description = "Macro definition";
                symbol.parentStruct = "";
                
                result.symbols.push_back(symbol);
                break; // 只需要第一个identifier（宏名称）
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