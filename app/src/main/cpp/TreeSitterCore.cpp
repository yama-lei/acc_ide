#include "TreeSitterJNI.h"
#include <android/log.h>

#define LOG_TAG "TreeSitterCore"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// 获取语言实例
TSLanguage* getLanguageByName(const std::string &language) {
    if (language == "cpp" || language == "c++") {
        return tree_sitter_cpp();
    } else if (language == "java") {
        return tree_sitter_java();
    } else if (language == "python") {
        return tree_sitter_python();
    }
    return nullptr;
}

// 获取节点文本内容
std::string getNodeText(TSNode node, const std::string &source) {
    uint32_t start = ts_node_start_byte(node);
    uint32_t end = ts_node_end_byte(node);
    if (start < source.length() && end <= source.length() && start < end) {
        return source.substr(start, end - start);
    }
    return "";
}

// 提取变量的具体数据类型
std::string extractDataType(TSNode declarationNode, const std::string &source) {
    if (ts_node_is_null(declarationNode)) {
        return "unknown";
    }
    
    // 查找类型说明符
    uint32_t childCount = ts_node_child_count(declarationNode);
    for (uint32_t i = 0; i < childCount; i++) {
        TSNode child = ts_node_child(declarationNode, i);
        const char *childType = ts_node_type(child);
        
        // 基本类型
        if (strcmp(childType, "primitive_type") == 0) {
            return getNodeText(child, source);
        }
        // 模板类型 (如 vector<int>)
        else if (strcmp(childType, "template_type") == 0) {
            return getNodeText(child, source);
        }
        // 类型标识符 (如 string, 自定义类型)
        else if (strcmp(childType, "type_identifier") == 0) {
            return getNodeText(child, source);
        }
        // 限定标识符 (如 std::string)
        else if (strcmp(childType, "qualified_identifier") == 0) {
            return getNodeText(child, source);
        }
        // 结构体说明符
        else if (strcmp(childType, "struct_specifier") == 0) {
            TSNode nameNode = ts_node_child_by_field_name(child, "name", 4);
            if (!ts_node_is_null(nameNode)) {
                return "struct " + getNodeText(nameNode, source);
            }
            return "struct";
        }
        // 类说明符
        else if (strcmp(childType, "class_specifier") == 0) {
            TSNode nameNode = ts_node_child_by_field_name(child, "name", 4);
            if (!ts_node_is_null(nameNode)) {
                return "class " + getNodeText(nameNode, source);
            }
            return "class";
        }
        // 枚举说明符
        else if (strcmp(childType, "enum_specifier") == 0) {
            TSNode nameNode = ts_node_child_by_field_name(child, "name", 4);
            if (!ts_node_is_null(nameNode)) {
                return "enum " + getNodeText(nameNode, source);
            }
            return "enum";
        }
    }
    
    return "auto"; // 默认返回auto类型
}

// 分析声明器获取完整类型信息（包括指针、引用等修饰符）
std::string analyzeDeclaratorType(TSNode declarator, const std::string &baseType) {
    if (ts_node_is_null(declarator)) {
        return baseType;
    }
    
    const char *declaratorType = ts_node_type(declarator);
    
    // 指针声明
    if (strcmp(declaratorType, "pointer_declarator") == 0) {
        TSNode inner = ts_node_child_by_field_name(declarator, "declarator", 10);
        if (!ts_node_is_null(inner)) {
            return analyzeDeclaratorType(inner, baseType) + "*";
        }
        return baseType + "*";
    }
    
    // 引用声明
    if (strcmp(declaratorType, "reference_declarator") == 0) {
        // 检查是否是右值引用 (&&) 或左值引用 (&)
        uint32_t childCount = ts_node_child_count(declarator);
        bool isRValueRef = false;
        for (uint32_t i = 0; i < childCount; i++) {
            TSNode child = ts_node_child(declarator, i);
            const char *nodeType = ts_node_type(child);
            if (strcmp(nodeType, "&&") == 0) {
                isRValueRef = true;
                break;
            }
        }
        
        std::string suffix = isRValueRef ? "&&" : "&";
        
        // 查找内层声明器
        for (uint32_t i = 0; i < childCount; i++) {
            TSNode child = ts_node_child(declarator, i);
            if (strcmp(ts_node_type(child), "identifier") != 0 && 
                strcmp(ts_node_type(child), "&") != 0 && 
                strcmp(ts_node_type(child), "&&") != 0) {
                return analyzeDeclaratorType(child, baseType) + suffix;
            }
        }
        return baseType + suffix;
    }
    
    // 数组声明
    if (strcmp(declaratorType, "array_declarator") == 0) {
        TSNode inner = ts_node_child_by_field_name(declarator, "declarator", 10);
        TSNode size = ts_node_child_by_field_name(declarator, "size", 4);
        
        std::string arraySize = "[]";
        if (!ts_node_is_null(size)) {
            // 这里可以进一步解析数组大小
            arraySize = "[" + std::to_string(ts_node_child_count(size)) + "]";
        }
        
        if (!ts_node_is_null(inner)) {
            return analyzeDeclaratorType(inner, baseType) + arraySize;
        }
        return baseType + arraySize;
    }
    
    // 函数声明器（函数指针）
    if (strcmp(declaratorType, "function_declarator") == 0) {
        TSNode inner = ts_node_child_by_field_name(declarator, "declarator", 10);
        if (!ts_node_is_null(inner)) {
            return analyzeDeclaratorType(inner, baseType) + "(*)";
        }
        return baseType + "(*)";
    }
    
    // 标识符或其他情况，直接返回基础类型
    return baseType;
}

// 查找函数名节点
TSNode findFunctionName(TSNode declarator) {
    const char *type = ts_node_type(declarator);
    
    // 直接是identifier
    if (strcmp(type, "identifier") == 0) {
        return declarator;
    }
    
    // 如果是function_declarator，查找其declarator字段
    if (strcmp(type, "function_declarator") == 0) {
        TSNode inner = ts_node_child_by_field_name(declarator, "declarator", 10);
        if (!ts_node_is_null(inner)) {
            return findFunctionName(inner); // 递归查找
        }
    }
    
    // 其他情况，遍历子节点查找identifier
    uint32_t childCount = ts_node_child_count(declarator);
    for (uint32_t i = 0; i < childCount; i++) {
        TSNode child = ts_node_child(declarator, i);
        if (strcmp(ts_node_type(child), "identifier") == 0) {
            return child;
        }
        // 递归查找
        TSNode result = findFunctionName(child);
        if (!ts_node_is_null(result)) {
            return result;
        }
    }
    
    return (TSNode){{0, 0, 0, 0}, NULL, NULL}; // 返回null节点
}

// 查找参数名节点 - 处理各种复杂的参数声明
TSNode findParameterName(TSNode declarator) {
    if (ts_node_is_null(declarator)) {
        return (TSNode){{0, 0, 0, 0}, NULL, NULL};
    }
    
    const char *type = ts_node_type(declarator);
    
    // 直接是identifier
    if (strcmp(type, "identifier") == 0) {
        return declarator;
    }
    
    // 指针声明 (int *x, int* x)
    if (strcmp(type, "pointer_declarator") == 0) {
        TSNode inner = ts_node_child_by_field_name(declarator, "declarator", 10);
        if (!ts_node_is_null(inner)) {
            return findParameterName(inner);
        }
    }
    
    // 引用声明 (int &x, int& x)
    if (strcmp(type, "reference_declarator") == 0) {
        uint32_t childCount = ts_node_child_count(declarator);
        for (uint32_t i = 0; i < childCount; i++) {
            TSNode child = ts_node_child(declarator, i);
            if (strcmp(ts_node_type(child), "identifier") == 0) {
                return child;
            }
            TSNode result = findParameterName(child);
            if (!ts_node_is_null(result)) {
                return result;
            }
        }
    }
    
    // 数组声明 (int x[], int x[10])
    if (strcmp(type, "array_declarator") == 0) {
        TSNode inner = ts_node_child_by_field_name(declarator, "declarator", 10);
        if (!ts_node_is_null(inner)) {
            return findParameterName(inner);
        }
    }
    
    // 函数指针声明
    if (strcmp(type, "function_declarator") == 0) {
        TSNode inner = ts_node_child_by_field_name(declarator, "declarator", 10);
        if (!ts_node_is_null(inner)) {
            return findParameterName(inner);
        }
    }
    
    // 括号声明 ((int x))
    if (strcmp(type, "parenthesized_declarator") == 0) {
        uint32_t childCount = ts_node_child_count(declarator);
        for (uint32_t i = 0; i < childCount; i++) {
            TSNode child = ts_node_child(declarator, i);
            TSNode result = findParameterName(child);
            if (!ts_node_is_null(result)) {
                return result;
            }
        }
    }
    
    // 递归查找所有子节点
    uint32_t childCount = ts_node_child_count(declarator);
    for (uint32_t i = 0; i < childCount; i++) {
        TSNode child = ts_node_child(declarator, i);
        if (strcmp(ts_node_type(child), "identifier") == 0) {
            return child;
        }
        TSNode result = findParameterName(child);
        if (!ts_node_is_null(result)) {
            return result;
        }
    }
    
    return (TSNode){{0, 0, 0, 0}, NULL, NULL}; // 返回null节点
}

// 遍历节点提取符号信息 - 修复作用域分析
void traverseNodeForSymbols(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) {
    if (ts_node_is_null(node)) {
        return;
    }
    
    const char *nodeType = ts_node_type(node);
    TSPoint startPoint = ts_node_start_point(node);
    
    LOGD("Traversing node: %s at %d:%d, scope level: %d", nodeType, startPoint.row, startPoint.column, scopeLevel);
    
    // 识别函数定义
    if (strcmp(nodeType, "function_definition") == 0) {
        TSNode declarator = ts_node_child_by_field_name(node, "declarator", 10);
        if (!ts_node_is_null(declarator)) {
            // 查找函数名
            TSNode functionName = findFunctionName(declarator);
            if (!ts_node_is_null(functionName)) {
                SymbolInfo symbol;
                symbol.name = getNodeText(functionName, source);
                symbol.type = FUNCTION;
                symbol.dataType = "function";
                symbol.line = startPoint.row;
                symbol.column = startPoint.column;
                symbol.scopeLevel = scopeLevel; // 函数定义在当前作用域
                symbol.description = "Function definition";
                symbols.push_back(symbol);
                LOGD("Found function: %s at scope level %d", symbol.name.c_str(), scopeLevel);
            }
            
            // 提取函数参数
            if (strcmp(ts_node_type(declarator), "function_declarator") == 0) {
                TSNode parameters = ts_node_child_by_field_name(declarator, "parameters", 10);
                if (!ts_node_is_null(parameters) && strcmp(ts_node_type(parameters), "parameter_list") == 0) {
                    uint32_t paramCount = ts_node_child_count(parameters);
                    for (uint32_t i = 0; i < paramCount; i++) {
                        TSNode param = ts_node_child(parameters, i);
                        const char *paramType = ts_node_type(param);
                        
                        if (strcmp(paramType, "parameter_declaration") == 0) {
                            TSNode paramDeclarator = ts_node_child_by_field_name(param, "declarator", 10);
                            if (!ts_node_is_null(paramDeclarator)) {
                                // 使用专门的函数查找参数名
                                TSNode paramName = findParameterName(paramDeclarator);
                                if (!ts_node_is_null(paramName)) {
                                    TSPoint paramPoint = ts_node_start_point(param);
                                    SymbolInfo paramSymbol;
                                    paramSymbol.name = getNodeText(paramName, source);
                                    paramSymbol.type = PARAMETER;
                                    
                                    // 提取具体的参数类型
                                    std::string baseType = extractDataType(param, source);
                                    paramSymbol.dataType = analyzeDeclaratorType(paramDeclarator, baseType);
                                    
                                    paramSymbol.line = paramPoint.row;
                                    paramSymbol.column = paramPoint.column;
                                    paramSymbol.scopeLevel = scopeLevel + 1; // 参数在函数作用域内
                                    paramSymbol.description = "Function parameter";
                                    symbols.push_back(paramSymbol);
                                    LOGD("Found parameter: %s (%s) at scope level %d", paramSymbol.name.c_str(), paramSymbol.dataType.c_str(), scopeLevel + 1);
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // 进入函数体，作用域级别+1
        TSNode body = ts_node_child_by_field_name(node, "body", 4);
        if (!ts_node_is_null(body)) {
            traverseNodeForSymbols(body, source, symbols, scopeLevel + 1);
        }
        return; // 不继续遍历子节点，已经处理了函数体
    }
    
    // 识别变量声明
    else if (strcmp(nodeType, "declaration") == 0) {
        // 处理多个声明器 (如 int a, b, c;)
        uint32_t childCount = ts_node_child_count(node);
        for (uint32_t i = 0; i < childCount; i++) {
            TSNode child = ts_node_child(node, i);
            const char *childType = ts_node_type(child);
            
            if (strcmp(childType, "identifier") == 0) {
                SymbolInfo symbol;
                symbol.name = getNodeText(child, source);
                symbol.type = VARIABLE;
                
                // 提取具体的变量类型
                std::string baseType = extractDataType(node, source);
                symbol.dataType = baseType; // 简单标识符不需要分析声明器
                
                symbol.line = ts_node_start_point(child).row;
                symbol.column = ts_node_start_point(child).column;
                symbol.scopeLevel = scopeLevel;
                symbol.description = "Variable declaration";
                symbols.push_back(symbol);
                LOGD("Found variable: %s (%s) at scope level %d", symbol.name.c_str(), symbol.dataType.c_str(), scopeLevel);
            } else if (strcmp(childType, "init_declarator") == 0) {
                TSNode declarator = ts_node_child_by_field_name(child, "declarator", 10);
                if (!ts_node_is_null(declarator)) {
                    TSNode identifier = findParameterName(declarator); // 复用函数来查找变量名
                    if (!ts_node_is_null(identifier)) {
                        SymbolInfo symbol;
                        symbol.name = getNodeText(identifier, source);
                        symbol.type = VARIABLE;
                        
                        // 提取具体的变量类型
                        std::string baseType = extractDataType(node, source);
                        symbol.dataType = analyzeDeclaratorType(declarator, baseType);
                        
                        symbol.line = ts_node_start_point(identifier).row;
                        symbol.column = ts_node_start_point(identifier).column;
                        symbol.scopeLevel = scopeLevel;
                        symbol.description = "Variable declaration";
                        symbols.push_back(symbol);
                        LOGD("Found init variable: %s (%s) at scope level %d", symbol.name.c_str(), symbol.dataType.c_str(), scopeLevel);
                    }
                }
            }
        }
    }
    
    // 识别结构体定义
    else if (strcmp(nodeType, "struct_specifier") == 0) {
        TSNode nameNode = ts_node_child_by_field_name(node, "name", 4);
        if (!ts_node_is_null(nameNode)) {
            SymbolInfo symbol;
            symbol.name = getNodeText(nameNode, source);
            symbol.type = STRUCT;
            symbol.dataType = "struct";
            symbol.line = startPoint.row;
            symbol.column = startPoint.column;
            symbol.scopeLevel = scopeLevel;
            symbol.description = "Struct definition";
            symbols.push_back(symbol);
            
            // 解析结构体成员
            TSNode body = ts_node_child_by_field_name(node, "body", 4);
            if (!ts_node_is_null(body)) {
                uint32_t memberCount = ts_node_child_count(body);
                for (uint32_t i = 0; i < memberCount; i++) {
                    TSNode member = ts_node_child(body, i);
                    if (strcmp(ts_node_type(member), "field_declaration") == 0) {
                        TSNode memberDeclarator = ts_node_child_by_field_name(member, "declarator", 10);
                        if (!ts_node_is_null(memberDeclarator)) {
                            TSPoint memberPoint = ts_node_start_point(member);
                            SymbolInfo memberSymbol;
                            memberSymbol.name = getNodeText(memberDeclarator, source);
                            memberSymbol.type = STRUCT_MEMBER;
                            memberSymbol.dataType = "member";
                            memberSymbol.line = memberPoint.row;
                            memberSymbol.column = memberPoint.column;
                            memberSymbol.scopeLevel = scopeLevel + 1;
                            memberSymbol.description = "Struct member";
                            memberSymbol.parentStruct = symbol.name;
                            symbols.push_back(memberSymbol);
                        }
                    }
                }
            }
        }
    }
    
    // 识别类定义
    else if (strcmp(nodeType, "class_specifier") == 0) {
        TSNode nameNode = ts_node_child_by_field_name(node, "name", 4);
        if (!ts_node_is_null(nameNode)) {
            SymbolInfo symbol;
            symbol.name = getNodeText(nameNode, source);
            symbol.type = CLASS;
            symbol.dataType = "class";
            symbol.line = startPoint.row;
            symbol.column = startPoint.column;
            symbol.scopeLevel = scopeLevel;
            symbol.description = "Class definition";
            symbols.push_back(symbol);
            
            // 解析类成员
            TSNode body = ts_node_child_by_field_name(node, "body", 4);
            if (!ts_node_is_null(body)) {
                uint32_t memberCount = ts_node_child_count(body);
                for (uint32_t i = 0; i < memberCount; i++) {
                    TSNode member = ts_node_child(body, i);
                    const char *memberType = ts_node_type(member);
                    
                    // 处理字段声明（成员变量）
                    if (strcmp(memberType, "field_declaration") == 0) {
                        uint32_t fieldChildCount = ts_node_child_count(member);
                        for (uint32_t j = 0; j < fieldChildCount; j++) {
                            TSNode fieldChild = ts_node_child(member, j);
                            const char *fieldChildType = ts_node_type(fieldChild);
                            
                            // 处理field_identifier (简单字段名)
                            if (strcmp(fieldChildType, "field_identifier") == 0) {
                                TSPoint memberPoint = ts_node_start_point(member);
                                SymbolInfo memberSymbol;
                                memberSymbol.name = getNodeText(fieldChild, source);
                                memberSymbol.type = STRUCT_MEMBER;
                                
                                // 提取具体的成员类型
                                std::string baseType = extractDataType(member, source);
                                memberSymbol.dataType = baseType;
                                
                                memberSymbol.line = memberPoint.row;
                                memberSymbol.column = memberPoint.column;
                                memberSymbol.scopeLevel = scopeLevel + 1;
                                memberSymbol.description = "Class member";
                                memberSymbol.parentStruct = symbol.name;
                                symbols.push_back(memberSymbol);
                                LOGD("Found class member: %s (%s) in class %s", memberSymbol.name.c_str(), memberSymbol.dataType.c_str(), symbol.name.c_str());
                            }
                            // 处理pointer_declarator (指针字段)
                            else if (strcmp(fieldChildType, "pointer_declarator") == 0) {
                                TSNode innerDeclarator = ts_node_child_by_field_name(fieldChild, "declarator", 10);
                                if (!ts_node_is_null(innerDeclarator) && strcmp(ts_node_type(innerDeclarator), "field_identifier") == 0) {
                                    TSPoint memberPoint = ts_node_start_point(member);
                                    SymbolInfo memberSymbol;
                                    memberSymbol.name = getNodeText(innerDeclarator, source);
                                    memberSymbol.type = STRUCT_MEMBER;
                                    
                                    // 提取具体的成员类型（包括指针修饰符）
                                    std::string baseType = extractDataType(member, source);
                                    memberSymbol.dataType = analyzeDeclaratorType(fieldChild, baseType);
                                    
                                    memberSymbol.line = memberPoint.row;
                                    memberSymbol.column = memberPoint.column;
                                    memberSymbol.scopeLevel = scopeLevel + 1;
                                    memberSymbol.description = "Class member";
                                    memberSymbol.parentStruct = symbol.name;
                                    symbols.push_back(memberSymbol);
                                    LOGD("Found class pointer member: %s (%s) in class %s", memberSymbol.name.c_str(), memberSymbol.dataType.c_str(), symbol.name.c_str());
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    // 识别枚举定义
    else if (strcmp(nodeType, "enum_specifier") == 0) {
        TSNode nameNode = ts_node_child_by_field_name(node, "name", 4);
        if (!ts_node_is_null(nameNode)) {
            SymbolInfo symbol;
            symbol.name = getNodeText(nameNode, source);
            symbol.type = ENUM;
            symbol.dataType = "enum";
            symbol.line = startPoint.row;
            symbol.column = startPoint.column;
            symbol.scopeLevel = scopeLevel;
            symbol.description = "Enum definition";
            symbols.push_back(symbol);
        }
    }
    
    // 识别typedef定义
    else if (strcmp(nodeType, "type_definition") == 0) {
        // typedef定义通常有declarator字段
        uint32_t childCount = ts_node_child_count(node);
        for (uint32_t i = 0; i < childCount; i++) {
            TSNode child = ts_node_child(node, i);
            const char *childType = ts_node_type(child);
            
            if (strcmp(childType, "type_identifier") == 0) {
                SymbolInfo symbol;
                symbol.name = getNodeText(child, source);
                symbol.type = CLASS; // 使用CLASS类型表示typedef
                symbol.dataType = "typedef";
                symbol.line = ts_node_start_point(child).row;
                symbol.column = ts_node_start_point(child).column;
                symbol.scopeLevel = scopeLevel;
                symbol.description = "Type definition";
                symbols.push_back(symbol);
                LOGD("Found typedef: %s at scope level %d", symbol.name.c_str(), scopeLevel);
            }
        }
    }
    
    // 识别for循环变量
    else if (strcmp(nodeType, "for_statement") == 0) {
        TSNode initializer = ts_node_child_by_field_name(node, "initializer", 11);
        if (!ts_node_is_null(initializer)) {
            // 递归处理初始化部分的变量声明
            traverseNodeForSymbols(initializer, source, symbols, scopeLevel + 1);
        }
        
        // 处理for语句体
        TSNode body = ts_node_child_by_field_name(node, "body", 4);
        if (!ts_node_is_null(body)) {
            traverseNodeForSymbols(body, source, symbols, scopeLevel + 1);
        }
        return; // 避免重复处理子节点
    }
    
    // 识别range-based for循环变量
    else if (strcmp(nodeType, "for_range_loop") == 0) {
        TSNode declarator = ts_node_child_by_field_name(node, "declarator", 10);
        if (!ts_node_is_null(declarator)) {
            TSNode varName = findParameterName(declarator);
            if (!ts_node_is_null(varName)) {
                SymbolInfo symbol;
                symbol.name = getNodeText(varName, source);
                symbol.type = VARIABLE;
                symbol.dataType = "range_variable";
                symbol.line = ts_node_start_point(declarator).row;
                symbol.column = ts_node_start_point(declarator).column;
                symbol.scopeLevel = scopeLevel + 1;
                symbol.description = "Range-based for loop variable";
                symbols.push_back(symbol);
                LOGD("Found range-for variable: %s at scope level %d", symbol.name.c_str(), scopeLevel + 1);
            }
        }
        
        // 处理for循环体
        TSNode body = ts_node_child_by_field_name(node, "body", 4);
        if (!ts_node_is_null(body)) {
            traverseNodeForSymbols(body, source, symbols, scopeLevel + 1);
        }
        return; // 避免重复处理子节点
    }
    
    // 识别catch语句参数
    else if (strcmp(nodeType, "catch_clause") == 0) {
        TSNode parameters = ts_node_child_by_field_name(node, "parameters", 10);
        if (!ts_node_is_null(parameters) && strcmp(ts_node_type(parameters), "parameter_list") == 0) {
            uint32_t paramCount = ts_node_child_count(parameters);
            for (uint32_t i = 0; i < paramCount; i++) {
                TSNode param = ts_node_child(parameters, i);
                if (strcmp(ts_node_type(param), "parameter_declaration") == 0) {
                    TSNode paramDeclarator = ts_node_child_by_field_name(param, "declarator", 10);
                    if (!ts_node_is_null(paramDeclarator)) {
                        TSNode paramName = findParameterName(paramDeclarator);
                        if (!ts_node_is_null(paramName)) {
                            SymbolInfo symbol;
                            symbol.name = getNodeText(paramName, source);
                            symbol.type = PARAMETER;
                            symbol.dataType = "catch_parameter";
                            symbol.line = ts_node_start_point(param).row;
                            symbol.column = ts_node_start_point(param).column;
                            symbol.scopeLevel = scopeLevel + 1;
                            symbol.description = "Catch clause parameter";
                            symbols.push_back(symbol);
                            LOGD("Found catch parameter: %s at scope level %d", symbol.name.c_str(), scopeLevel + 1);
                        }
                    }
                }
            }
        }
        
        // 处理catch体
        TSNode body = ts_node_child_by_field_name(node, "body", 4);
        if (!ts_node_is_null(body)) {
            traverseNodeForSymbols(body, source, symbols, scopeLevel + 1);
        }
        return; // 避免重复处理子节点
    }
    
    // 递归处理子节点，但跳过已经特殊处理的节点类型
    if (strcmp(nodeType, "function_definition") != 0 && 
        strcmp(nodeType, "for_statement") != 0 && 
        strcmp(nodeType, "for_range_loop") != 0 && 
        strcmp(nodeType, "catch_clause") != 0) {
        uint32_t childCount = ts_node_child_count(node);
        int nextScopeLevel = scopeLevel;
        
        // 检查是否需要增加作用域级别
        if (strcmp(nodeType, "compound_statement") == 0) {
            nextScopeLevel++;
            LOGD("Entering compound_statement, scope level: %d -> %d", scopeLevel, nextScopeLevel);
        }
        
        for (uint32_t i = 0; i < childCount; i++) {
            TSNode child = ts_node_child(node, i);
            traverseNodeForSymbols(child, source, symbols, nextScopeLevel);
        }
    }
}

// 遍历节点提取作用域信息 - 修复版本
void traverseNodeForScopes(TSNode node, std::vector<ScopeInfo> &scopes, int currentLevel) {
    if (ts_node_is_null(node)) {
        return;
    }
    
    const char *nodeType = ts_node_type(node);
    TSPoint startPoint = ts_node_start_point(node);
    TSPoint endPoint = ts_node_end_point(node);
    
    LOGD("Scope analysis - node: %s at %d:%d-%d:%d, level: %d", 
         nodeType, startPoint.row, startPoint.column, endPoint.row, endPoint.column, currentLevel);
    
    bool createdScope = false;
    
    // 识别函数作用域
    if (strcmp(nodeType, "function_definition") == 0) {
        ScopeInfo scope;
        scope.level = currentLevel;
        scope.startLine = startPoint.row;
        scope.endLine = endPoint.row;
        scope.type = FUNCTION_SCOPE;
        scopes.push_back(scope);
        LOGD("Created FUNCTION_SCOPE level %d: lines %d-%d", currentLevel, startPoint.row, endPoint.row);
        currentLevel++;
        createdScope = true;
    }
    // 识别块作用域（函数体、if/for/while语句块等）
    else if (strcmp(nodeType, "compound_statement") == 0) {
        ScopeInfo scope;
        scope.level = currentLevel;
        scope.startLine = startPoint.row;
        scope.endLine = endPoint.row;
        scope.type = BLOCK_SCOPE;
        scopes.push_back(scope);
        LOGD("Created BLOCK_SCOPE level %d: lines %d-%d", currentLevel, startPoint.row, endPoint.row);
        currentLevel++;
        createdScope = true;
    }
    // 识别类作用域
    else if (strcmp(nodeType, "class_specifier") == 0) {
        ScopeInfo scope;
        scope.level = currentLevel;
        scope.startLine = startPoint.row;
        scope.endLine = endPoint.row;
        scope.type = CLASS_SCOPE;
        scopes.push_back(scope);
        LOGD("Created CLASS_SCOPE level %d: lines %d-%d", currentLevel, startPoint.row, endPoint.row);
        currentLevel++;
        createdScope = true;
    }
    
    // 递归处理子节点
    uint32_t childCount = ts_node_child_count(node);
    for (uint32_t i = 0; i < childCount; i++) {
        TSNode child = ts_node_child(node, i);
        traverseNodeForScopes(child, scopes, currentLevel);
    }
}

// C++代码解析核心功能
ParseResult parseCppCodeCore(const std::string &code) {
    ParseResult result;
    
    TSParser *parser = ts_parser_new();
    if (!parser) {
        LOGE("Failed to create parser");
        return result;
    }
    
    TSLanguage *language = tree_sitter_cpp();
    if (!language || !ts_parser_set_language(parser, language)) {
        LOGE("Failed to set C++ language");
        ts_parser_delete(parser);
        return result;
    }
    
    TSTree *tree = ts_parser_parse_string(parser, nullptr, code.c_str(), code.length());
    if (!tree) {
        LOGE("Failed to parse C++ code");
        ts_parser_delete(parser);
        return result;
    }
    
    TSNode rootNode = ts_tree_root_node(tree);
    
    // 提取符号和作用域信息
    traverseNodeForSymbols(rootNode, code, result.symbols);
    traverseNodeForScopes(rootNode, result.scopes);
    
    LOGD("Parsed %zu symbols and %zu scopes", result.symbols.size(), result.scopes.size());
    
    ts_tree_delete(tree);
    ts_parser_delete(parser);
    
    return result;
}

// Java代码解析核心功能
ParseResult parseJavaCodeCore(const std::string &code) {
    ParseResult result;
    
    TSParser *parser = ts_parser_new();
    if (!parser) {
        LOGE("Failed to create parser");
        return result;
    }
    
    TSLanguage *language = tree_sitter_java();
    if (!language || !ts_parser_set_language(parser, language)) {
        LOGE("Failed to set Java language");
        ts_parser_delete(parser);
        return result;
    }
    
    TSTree *tree = ts_parser_parse_string(parser, nullptr, code.c_str(), code.length());
    if (!tree) {
        LOGE("Failed to parse Java code");
        ts_parser_delete(parser);
        return result;
    }
    
    TSNode rootNode = ts_tree_root_node(tree);
    
    // 提取符号和作用域信息
    traverseNodeForSymbols(rootNode, code, result.symbols);
    traverseNodeForScopes(rootNode, result.scopes);
    
    LOGD("Parsed %zu symbols and %zu scopes", result.symbols.size(), result.scopes.size());
    
    ts_tree_delete(tree);
    ts_parser_delete(parser);
    
    return result;
}

// Python代码解析核心功能
ParseResult parsePythonCodeCore(const std::string &code) {
    ParseResult result;
    
    TSParser *parser = ts_parser_new();
    if (!parser) {
        LOGE("Failed to create parser");
        return result;
    }
    
    TSLanguage *language = tree_sitter_python();
    if (!language || !ts_parser_set_language(parser, language)) {
        LOGE("Failed to set Python language");
        ts_parser_delete(parser);
        return result;
    }
    
    TSTree *tree = ts_parser_parse_string(parser, nullptr, code.c_str(), code.length());
    if (!tree) {
        LOGE("Failed to parse Python code");
        ts_parser_delete(parser);
        return result;
    }
    
    TSNode rootNode = ts_tree_root_node(tree);
    
    // 提取符号和作用域信息
    traverseNodeForSymbols(rootNode, code, result.symbols);
    traverseNodeForScopes(rootNode, result.scopes);
    
    LOGD("Parsed %zu symbols and %zu scopes", result.symbols.size(), result.scopes.size());
    
    ts_tree_delete(tree);
    ts_parser_delete(parser);
    
    return result;
}