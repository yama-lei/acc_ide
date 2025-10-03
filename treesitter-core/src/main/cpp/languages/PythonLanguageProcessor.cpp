#include "PythonLanguageProcessor.h"
#include <android/log.h>

#define LOG_TAG "PythonLanguageProcessor"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

std::string PythonLanguageProcessor::getLanguageId() const {
    return "python";
}

std::string PythonLanguageProcessor::getLanguageName() const {
    return "Python";
}

std::vector<std::string> PythonLanguageProcessor::getSupportedExtensions() const {
    return {"py", "pyw", "pyi"};
}

const TSLanguage* PythonLanguageProcessor::getTreeSitterLanguage() const {
    return tree_sitter_python();
}

/**
 * Python语言特定符号提取 / Python Language Specific Symbol Extraction
 * 
 * @param rootNode TreeSitter root node of parsed AST
 * @param source Original source code string  
 * @param symbols Output vector to store extracted symbols
 * @param scopeLevel Current scope depth level
 */
void PythonLanguageProcessor::extractLanguageSpecificSymbols(
    TSNode rootNode, 
    const std::string &source, 
    std::vector<SymbolInfo> &symbols, 
    int scopeLevel
) const {
    extractPythonSymbols(rootNode, source, symbols, scopeLevel);
}

void PythonLanguageProcessor::extractPythonSymbols(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const {
    if (ts_node_is_null(node)) {
        return;
    }
    
    const char *nodeType = ts_node_type(node);
    
    // Extract function definitions
    if (strcmp(nodeType, "function_definition") == 0) {
        extractPythonFunctions(node, source, symbols, scopeLevel);
        return; // Already handled recursion
    }
    // Extract class definitions
    else if (strcmp(nodeType, "class_definition") == 0) {
        extractPythonClasses(node, source, symbols, scopeLevel);
        return; // Already handled recursion
    }
    // Extract assignment statements (variable definitions)
    else if (strcmp(nodeType, "assignment") == 0) {
        extractPythonAssignments(node, source, symbols, scopeLevel);
    }
    
    // Recurse through child nodes
    int nextScopeLevel = scopeLevel;
    if (strcmp(nodeType, "block") == 0) {
        nextScopeLevel++; // Increase scope level for blocks
    }
    
    uint32_t childCount = ts_node_child_count(node);
    for (uint32_t i = 0; i < childCount; i++) {
        TSNode child = ts_node_child(node, i);
        extractPythonSymbols(child, source, symbols, nextScopeLevel);
    }
}

void PythonLanguageProcessor::extractPythonFunctions(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const {
    TSPoint startPoint = ts_node_start_point(node);
    
    SymbolInfo symbol;
    symbol.type = FUNCTION;
    symbol.dataType = "function";
    symbol.line = startPoint.row;
    symbol.column = startPoint.column;
    symbol.scopeLevel = scopeLevel;
    symbol.description = "Function definition";
    
    // Extract function name
    TSNode nameNode = ts_node_child_by_field_name(node, "name", 4);
    if (!ts_node_is_null(nameNode)) {
        symbol.name = getNodeText(nameNode, source);
        symbols.push_back(symbol);
        LOGD("Found Python function: %s", symbol.name.c_str());
    }
    
    // Extract function parameters
    TSNode parameters = ts_node_child_by_field_name(node, "parameters", 10);
    if (!ts_node_is_null(parameters)) {
        uint32_t paramCount = ts_node_child_count(parameters);
        for (uint32_t i = 0; i < paramCount; i++) {
            TSNode param = ts_node_child(parameters, i);
            if (strcmp(ts_node_type(param), "identifier") == 0) {
                SymbolInfo paramSymbol;
                paramSymbol.name = getNodeText(param, source);
                paramSymbol.type = PARAMETER;
                paramSymbol.dataType = "parameter";
                paramSymbol.line = ts_node_start_point(param).row;
                paramSymbol.column = ts_node_start_point(param).column;
                paramSymbol.scopeLevel = scopeLevel + 1;
                paramSymbol.description = "Function parameter";
                symbols.push_back(paramSymbol);
                LOGD("Found Python parameter: %s", paramSymbol.name.c_str());
            }
        }
    }
    
    // Process function body with increased scope level
    TSNode body = ts_node_child_by_field_name(node, "body", 4);
    if (!ts_node_is_null(body)) {
        extractPythonSymbols(body, source, symbols, scopeLevel + 1);
    }
}

void PythonLanguageProcessor::extractPythonClasses(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const {
    TSPoint startPoint = ts_node_start_point(node);
    
    SymbolInfo symbol;
    symbol.type = CLASS;
    symbol.dataType = "class";
    symbol.line = startPoint.row;
    symbol.column = startPoint.column;
    symbol.scopeLevel = scopeLevel;
    symbol.description = "Class definition";
    
    // Extract class name
    TSNode nameNode = ts_node_child_by_field_name(node, "name", 4);
    if (!ts_node_is_null(nameNode)) {
        symbol.name = getNodeText(nameNode, source);
        symbols.push_back(symbol);
        LOGD("Found Python class: %s", symbol.name.c_str());
    }
    
    // Process class body with increased scope level
    TSNode body = ts_node_child_by_field_name(node, "body", 4);
    if (!ts_node_is_null(body)) {
        extractPythonSymbols(body, source, symbols, scopeLevel + 1);
    }
}

void PythonLanguageProcessor::extractPythonAssignments(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const {
    TSPoint startPoint = ts_node_start_point(node);
    
    // Extract left side of assignment (variable name)
    TSNode left = ts_node_child_by_field_name(node, "left", 4);
    if (!ts_node_is_null(left) && strcmp(ts_node_type(left), "identifier") == 0) {
        SymbolInfo symbol;
        symbol.name = getNodeText(left, source);
        symbol.type = VARIABLE;
        symbol.dataType = "variable";
        symbol.line = startPoint.row;
        symbol.column = startPoint.column;
        symbol.scopeLevel = scopeLevel;
        symbol.description = "Python variable assignment";
        symbols.push_back(symbol);
        LOGD("Found Python variable: %s", symbol.name.c_str());
    }
}