#include "JavaLanguageProcessor.h"
#include <android/log.h>

#define LOG_TAG "JavaLanguageProcessor"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

std::string JavaLanguageProcessor::getLanguageId() const {
    return "java";
}

std::string JavaLanguageProcessor::getLanguageName() const {
    return "Java";
}

std::vector<std::string> JavaLanguageProcessor::getSupportedExtensions() const {
    return {"java"};
}

const TSLanguage* JavaLanguageProcessor::getTreeSitterLanguage() const {
    return tree_sitter_java();
}

/**
 * Java语言特定符号提取 / Java Language Specific Symbol Extraction
 * 
 * @param rootNode TreeSitter root node of parsed AST
 * @param source Original source code string  
 * @param symbols Output vector to store extracted symbols
 * @param scopeLevel Current scope depth level
 */
void JavaLanguageProcessor::extractLanguageSpecificSymbols(
    TSNode rootNode, 
    const std::string &source, 
    std::vector<SymbolInfo> &symbols, 
    int scopeLevel
) const {
    extractJavaSymbols(rootNode, source, symbols, scopeLevel);
}

void JavaLanguageProcessor::extractJavaSymbols(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const {
    if (ts_node_is_null(node)) {
        return;
    }
    
    const char *nodeType = ts_node_type(node);
    
    // Extract class declarations
    if (strcmp(nodeType, "class_declaration") == 0) {
        extractJavaClasses(node, source, symbols, scopeLevel);
        return; // Already handled recursion
    }
    // Extract method declarations
    else if (strcmp(nodeType, "method_declaration") == 0) {
        extractJavaMethods(node, source, symbols, scopeLevel);
        return; // Already handled recursion
    }
    // Extract field declarations
    else if (strcmp(nodeType, "field_declaration") == 0) {
        extractJavaFields(node, source, symbols, scopeLevel);
    }
    // Extract local variable declarations
    else if (strcmp(nodeType, "local_variable_declaration") == 0) {
        extractJavaLocalVariables(node, source, symbols, scopeLevel);
    }
    
    // Recurse through child nodes
    int nextScopeLevel = scopeLevel;
    if (strcmp(nodeType, "block") == 0) {
        nextScopeLevel++; // Increase scope level for blocks
    }
    
    uint32_t childCount = ts_node_child_count(node);
    for (uint32_t i = 0; i < childCount; i++) {
        TSNode child = ts_node_child(node, i);
        extractJavaSymbols(child, source, symbols, nextScopeLevel);
    }
}

void JavaLanguageProcessor::extractJavaClasses(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const {
    TSPoint startPoint = ts_node_start_point(node);
    
    // Find class name
    uint32_t childCount = ts_node_child_count(node);
    for (uint32_t i = 0; i < childCount; i++) {
        TSNode child = ts_node_child(node, i);
        if (strcmp(ts_node_type(child), "identifier") == 0) {
            SymbolInfo symbol;
            symbol.name = getNodeText(child, source);
            symbol.type = CLASS;
            symbol.dataType = "class";
            symbol.line = startPoint.row;
            symbol.column = startPoint.column;
            symbol.scopeLevel = scopeLevel;
            symbol.description = "Class";
            symbols.push_back(symbol);
            LOGD("Found Java class: %s", symbol.name.c_str());
            break;
        }
    }
    
    // Process class body with increased scope level
    for (uint32_t i = 0; i < childCount; i++) {
        TSNode child = ts_node_child(node, i);
        if (strcmp(ts_node_type(child), "class_body") == 0) {
            extractJavaSymbols(child, source, symbols, scopeLevel + 1);
            break;
        }
    }
}

void JavaLanguageProcessor::extractJavaMethods(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const {
    TSPoint startPoint = ts_node_start_point(node);
    
    SymbolInfo symbol;
    symbol.type = FUNCTION;
    symbol.dataType = "method";
    symbol.line = startPoint.row;
    symbol.column = startPoint.column;
    symbol.scopeLevel = scopeLevel;
    symbol.description = "Java method";
    
    // Extract method name
    TSNode nameNode = ts_node_child_by_field_name(node, "name", 4);
    if (!ts_node_is_null(nameNode)) {
        symbol.name = getNodeText(nameNode, source);
        symbols.push_back(symbol);
        LOGD("Found Java method: %s", symbol.name.c_str());
    }
    
    // Extract method parameters
    TSNode parameters = ts_node_child_by_field_name(node, "parameters", 10);
    if (!ts_node_is_null(parameters)) {
        uint32_t paramCount = ts_node_child_count(parameters);
        for (uint32_t i = 0; i < paramCount; i++) {
            TSNode param = ts_node_child(parameters, i);
            if (strcmp(ts_node_type(param), "formal_parameter") == 0) {
                TSNode paramNameNode = ts_node_child_by_field_name(param, "name", 4);
                if (!ts_node_is_null(paramNameNode)) {
                    SymbolInfo paramSymbol;
                    paramSymbol.name = getNodeText(paramNameNode, source);
                    paramSymbol.type = PARAMETER;
                    paramSymbol.dataType = "parameter";
                    paramSymbol.line = ts_node_start_point(param).row;
                    paramSymbol.column = ts_node_start_point(param).column;
                    paramSymbol.scopeLevel = scopeLevel + 1;
                    paramSymbol.description = "Method parameter";
                    symbols.push_back(paramSymbol);
                }
            }
        }
    }
    
    // Process method body with increased scope level
    TSNode body = ts_node_child_by_field_name(node, "body", 4);
    if (!ts_node_is_null(body)) {
        extractJavaSymbols(body, source, symbols, scopeLevel + 1);
    }
}

void JavaLanguageProcessor::extractJavaFields(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const {
    // Extract field type
    TSNode typeNode = ts_node_child_by_field_name(node, "type", 4);
    std::string javaType = "unknown";
    if (!ts_node_is_null(typeNode)) {
        javaType = getNodeText(typeNode, source);
    } else {
        // Fallback: look for type identifiers
        uint32_t childCount = ts_node_child_count(node);
        for (uint32_t i = 0; i < childCount; i++) {
            TSNode child = ts_node_child(node, i);
            const char *childType = ts_node_type(child);
            if (strcmp(childType, "type_identifier") == 0 || 
                strcmp(childType, "integral_type") == 0 || 
                strcmp(childType, "floating_point_type") == 0) {
                javaType = getNodeText(child, source);
                break;
            }
        }
    }
    
    // Extract all variable declarators
    uint32_t childCount = ts_node_child_count(node);
    for (uint32_t i = 0; i < childCount; i++) {
        TSNode child = ts_node_child(node, i);
        if (strcmp(ts_node_type(child), "variable_declarator") == 0) {
            TSNode nameNode = ts_node_child_by_field_name(child, "name", 4);
            if (!ts_node_is_null(nameNode)) {
                SymbolInfo symbol;
                symbol.name = getNodeText(nameNode, source);
                symbol.type = STRUCT_MEMBER; // Java fields as struct members
                symbol.dataType = javaType;
                symbol.line = ts_node_start_point(child).row;
                symbol.column = ts_node_start_point(child).column;
                symbol.scopeLevel = scopeLevel;
                symbol.description = "Field";
                symbols.push_back(symbol);
                LOGD("Found Java field: %s (%s)", symbol.name.c_str(), symbol.dataType.c_str());
            }
        }
    }
}

void JavaLanguageProcessor::extractJavaLocalVariables(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const {
    // Extract variable type
    TSNode typeNode = ts_node_child_by_field_name(node, "type", 4);
    std::string javaType = "unknown";
    if (!ts_node_is_null(typeNode)) {
        javaType = getNodeText(typeNode, source);
    }
    
    // Extract all variable declarators
    uint32_t childCount = ts_node_child_count(node);
    for (uint32_t i = 0; i < childCount; i++) {
        TSNode child = ts_node_child(node, i);
        if (strcmp(ts_node_type(child), "variable_declarator") == 0) {
            TSNode nameNode = ts_node_child_by_field_name(child, "name", 4);
            if (!ts_node_is_null(nameNode)) {
                SymbolInfo symbol;
                symbol.name = getNodeText(nameNode, source);
                symbol.type = VARIABLE;
                symbol.dataType = javaType;
                symbol.line = ts_node_start_point(child).row;
                symbol.column = ts_node_start_point(child).column;
                symbol.scopeLevel = scopeLevel;
                symbol.description = "Local variable";
                symbols.push_back(symbol);
                LOGD("Found Java local variable: %s (%s)", symbol.name.c_str(), symbol.dataType.c_str());
            }
        }
    }
}