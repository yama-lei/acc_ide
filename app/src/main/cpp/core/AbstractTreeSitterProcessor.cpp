#include "AbstractTreeSitterProcessor.h"
#include <android/log.h>

#define LOG_TAG "AbstractTreeSitterProcessor"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// 获取节点文本内容
std::string AbstractTreeSitterProcessor::getNodeText(TSNode node, const std::string &source) const {
    uint32_t start = ts_node_start_byte(node);
    uint32_t end = ts_node_end_byte(node);
    if (start < source.length() && end <= source.length() && start < end) {
        return source.substr(start, end - start);
    }
    return "";
}

// 查找函数名节点
TSNode AbstractTreeSitterProcessor::findFunctionName(TSNode declarator) const {
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

// 查找参数名节点
TSNode AbstractTreeSitterProcessor::findParameterName(TSNode declarator) const {
    if (ts_node_is_null(declarator)) {
        return (TSNode){{0, 0, 0, 0}, NULL, NULL};
    }
    
    const char *type = ts_node_type(declarator);
    
    // 直接是identifier
    if (strcmp(type, "identifier") == 0) {
        return declarator;
    }
    
    // 指针声明
    if (strcmp(type, "pointer_declarator") == 0) {
        TSNode inner = ts_node_child_by_field_name(declarator, "declarator", 10);
        if (!ts_node_is_null(inner)) {
            return findParameterName(inner);
        }
    }
    
    // 引用声明
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
    
    // 数组声明
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
    
    // 括号声明
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

// 提取变量的具体数据类型
std::string AbstractTreeSitterProcessor::extractDataType(TSNode declarationNode, const std::string &source) const {
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
        // 模板类型
        else if (strcmp(childType, "template_type") == 0) {
            return getNodeText(child, source);
        }
        // 类型标识符
        else if (strcmp(childType, "type_identifier") == 0) {
            return getNodeText(child, source);
        }
        // 限定标识符
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
    
    return "auto";
}

// 分析声明器获取完整类型信息
std::string AbstractTreeSitterProcessor::analyzeDeclaratorType(TSNode declarator, const std::string &baseType) const {
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
    
    return baseType;
}

// 遍历节点提取作用域信息
void AbstractTreeSitterProcessor::traverseNodeForScopes(TSNode node, std::vector<ScopeInfo> &scopes, int currentLevel) const {
    if (ts_node_is_null(node)) {
        return;
    }
    
    const char *nodeType = ts_node_type(node);
    TSPoint startPoint = ts_node_start_point(node);
    TSPoint endPoint = ts_node_end_point(node);
    
    // 识别函数作用域
    if (strcmp(nodeType, "function_definition") == 0 || strcmp(nodeType, "method_declaration") == 0) {
        ScopeInfo scope;
        scope.level = currentLevel;
        scope.startLine = startPoint.row;
        scope.endLine = endPoint.row;
        scope.type = SCOPE_FUNCTION;
        scopes.push_back(scope);
        currentLevel++;
    }
    // 识别块作用域
    else if (strcmp(nodeType, "compound_statement") == 0 || strcmp(nodeType, "block") == 0) {
        ScopeInfo scope;
        scope.level = currentLevel;
        scope.startLine = startPoint.row;
        scope.endLine = endPoint.row;
        scope.type = SCOPE_BLOCK;
        scopes.push_back(scope);
        currentLevel++;
    }
    // 识别类作用域
    else if (strcmp(nodeType, "class_specifier") == 0 || strcmp(nodeType, "class_declaration") == 0) {
        ScopeInfo scope;
        scope.level = currentLevel;
        scope.startLine = startPoint.row;
        scope.endLine = endPoint.row;
        scope.type = SCOPE_CLASS;
        scopes.push_back(scope);
        currentLevel++;
    }
    
    // 递归处理子节点
    uint32_t childCount = ts_node_child_count(node);
    for (uint32_t i = 0; i < childCount; i++) {
        TSNode child = ts_node_child(node, i);
        traverseNodeForScopes(child, scopes, currentLevel);
    }
}

// 执行通用的TreeSitter解析流程
ParseResult AbstractTreeSitterProcessor::executeTreeSitterParsing(const std::string &code, const TSLanguage* language) const {
    ParseResult result;
    
    TSParser *parser = ts_parser_new();
    if (!parser) {
        LOGE("Failed to create parser");
        return result;
    }
    
    if (!language || !ts_parser_set_language(parser, language)) {
        LOGE("Failed to set language");
        ts_parser_delete(parser);
        return result;
    }
    
    TSTree *tree = ts_parser_parse_string(parser, nullptr, code.c_str(), code.length());
    if (!tree) {
        LOGE("Failed to parse code");
        ts_parser_delete(parser);
        return result;
    }
    
    TSNode rootNode = ts_tree_root_node(tree);
    
    // 提取符号和作用域信息
    extractLanguageSpecificSymbols(rootNode, code, result.symbols, 0);
    traverseNodeForScopes(rootNode, result.scopes);
    
    LOGD("Parsed %zu symbols and %zu scopes", result.symbols.size(), result.scopes.size());
    
    ts_tree_delete(tree);
    ts_parser_delete(parser);
    
    return result;
}

// 检查是否可用
bool AbstractTreeSitterProcessor::isAvailable() const {
    const TSLanguage* lang = getTreeSitterLanguage();
    return lang != nullptr;
}

// 实现解析代码的主方法
ParseResult AbstractTreeSitterProcessor::parseCode(const std::string &code) {
    const TSLanguage* language = getTreeSitterLanguage();
    if (!language) {
        LOGE("Language not available for %s", getLanguageId().c_str());
        return ParseResult();
    }
    
    return executeTreeSitterParsing(code, language);
}