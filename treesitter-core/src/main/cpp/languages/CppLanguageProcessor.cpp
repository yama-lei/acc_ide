#include "CppLanguageProcessor.h"
#include <android/log.h>

#define LOG_TAG "CppLanguageProcessor"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

std::string CppLanguageProcessor::getLanguageId() const {
    return "cpp";
}

std::string CppLanguageProcessor::getLanguageName() const {
    return "C++";
}

std::vector<std::string> CppLanguageProcessor::getSupportedExtensions() const {
    return {"cpp", "cxx", "cc", "c++", "hpp", "hxx", "h"};
}

const TSLanguage* CppLanguageProcessor::getTreeSitterLanguage() const {
    return tree_sitter_cpp();
}

/**
 * C++语言特定符号提取 / C++ Language Specific Symbol Extraction
 * 
 * @param rootNode TreeSitter root node of parsed AST
 * @param source Original source code string  
 * @param symbols Output vector to store extracted symbols
 * @param scopeLevel Current scope depth level
 */
void CppLanguageProcessor::extractLanguageSpecificSymbols(
    TSNode rootNode, 
    const std::string &source, 
    std::vector<SymbolInfo> &symbols, 
    int scopeLevel
) const {
    extractCppSymbols(rootNode, source, symbols, scopeLevel);
}

void CppLanguageProcessor::extractCppSymbols(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const {
    if (ts_node_is_null(node)) {
        return;
    }
    
    const char *nodeType = ts_node_type(node);
    
    // Extract function definitions
    if (strcmp(nodeType, "function_definition") == 0) {
        extractCppFunctions(node, source, symbols, scopeLevel);
        return; // Already handled recursion in function
    }
    // Extract variable declarations
    else if (strcmp(nodeType, "declaration") == 0) {
        extractCppVariables(node, source, symbols, scopeLevel);
    }
    // Extract class definitions
    else if (strcmp(nodeType, "class_specifier") == 0) {
        extractCppClasses(node, source, symbols, scopeLevel);
        return; // Already handled recursion in class
    }
    // Extract struct definitions
    else if (strcmp(nodeType, "struct_specifier") == 0) {
        extractCppStructs(node, source, symbols, scopeLevel);
        return; // Already handled recursion in struct
    }
    // Extract enum definitions
    else if (strcmp(nodeType, "enum_specifier") == 0) {
        extractCppEnums(node, source, symbols, scopeLevel);
        return; // Already handled recursion in enum
    }
    // Extract namespace definitions
    else if (strcmp(nodeType, "namespace_definition") == 0) {
        extractCppNamespaces(node, source, symbols, scopeLevel);
        return; // Already handled recursion in namespace
    }
    
    // Recurse through child nodes
    int nextScopeLevel = scopeLevel;
    if (strcmp(nodeType, "compound_statement") == 0) {
        nextScopeLevel++; // Increase scope level for blocks
    }
    
    uint32_t childCount = ts_node_child_count(node);
    for (uint32_t i = 0; i < childCount; i++) {
        TSNode child = ts_node_child(node, i);
        extractCppSymbols(child, source, symbols, nextScopeLevel);
    }
}

void CppLanguageProcessor::extractCppFunctions(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const {
    TSPoint startPoint = ts_node_start_point(node);
    
    SymbolInfo symbol;
    symbol.type = FUNCTION;
    symbol.dataType = "function";
    symbol.line = startPoint.row;
    symbol.column = startPoint.column;
    symbol.scopeLevel = scopeLevel;
    symbol.description = "Function definition";
    
    TSNode declarator = ts_node_child_by_field_name(node, "declarator", 10);
    if (!ts_node_is_null(declarator)) {
        TSNode functionName = findFunctionName(declarator);
        if (!ts_node_is_null(functionName)) {
            symbol.name = getNodeText(functionName, source);
            symbols.push_back(symbol);
            LOGD("Found C++ function: %s", symbol.name.c_str());
        }
        
        // Extract function parameters
        if (strcmp(ts_node_type(declarator), "function_declarator") == 0) {
            TSNode parameters = ts_node_child_by_field_name(declarator, "parameters", 10);
            if (!ts_node_is_null(parameters) && strcmp(ts_node_type(parameters), "parameter_list") == 0) {
                uint32_t paramCount = ts_node_child_count(parameters);
                for (uint32_t i = 0; i < paramCount; i++) {
                    TSNode param = ts_node_child(parameters, i);
                    if (strcmp(ts_node_type(param), "parameter_declaration") == 0) {
                        TSNode paramDeclarator = ts_node_child_by_field_name(param, "declarator", 10);
                        if (!ts_node_is_null(paramDeclarator)) {
                            TSNode paramName = findParameterName(paramDeclarator);
                            if (!ts_node_is_null(paramName)) {
                                SymbolInfo paramSymbol;
                                paramSymbol.name = getNodeText(paramName, source);
                                paramSymbol.type = PARAMETER;
                                paramSymbol.dataType = analyzeDeclaratorType(paramDeclarator, extractDataType(param, source));
                                paramSymbol.line = ts_node_start_point(param).row;
                                paramSymbol.column = ts_node_start_point(param).column;
                                paramSymbol.scopeLevel = scopeLevel + 1;
                                paramSymbol.description = "Function parameter";
                                symbols.push_back(paramSymbol);
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Process function body with increased scope level
    TSNode body = ts_node_child_by_field_name(node, "body", 4);
    if (!ts_node_is_null(body)) {
        extractCppSymbols(body, source, symbols, scopeLevel + 1);
    }
}

void CppLanguageProcessor::extractCppVariables(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const {
    uint32_t childCount = ts_node_child_count(node);
    
    for (uint32_t i = 0; i < childCount; i++) {
        TSNode child = ts_node_child(node, i);
        const char *childType = ts_node_type(child);
        
        if (strcmp(childType, "identifier") == 0) {
            SymbolInfo symbol;
            symbol.name = getNodeText(child, source);
            symbol.type = VARIABLE;
            symbol.dataType = extractDataType(node, source);
            symbol.line = ts_node_start_point(child).row;
            symbol.column = ts_node_start_point(child).column;
            symbol.scopeLevel = scopeLevel;
            symbol.description = "Variable";
            symbols.push_back(symbol);
        } 
        else if (strcmp(childType, "init_declarator") == 0) {
            TSNode declarator = ts_node_child_by_field_name(child, "declarator", 10);
            if (!ts_node_is_null(declarator)) {
                TSNode identifier = findParameterName(declarator);
                if (!ts_node_is_null(identifier)) {
                    SymbolInfo symbol;
                    symbol.name = getNodeText(identifier, source);
                    symbol.type = VARIABLE;
                    symbol.dataType = analyzeDeclaratorType(declarator, extractDataType(node, source));
                    symbol.line = ts_node_start_point(identifier).row;
                    symbol.column = ts_node_start_point(identifier).column;
                    symbol.scopeLevel = scopeLevel;
                    symbol.description = "Variable";
                    symbols.push_back(symbol);
                }
            }
        }
        // Handle other declarator types (pointer, reference, array, etc.)
        else if (strstr(childType, "_declarator") != nullptr) {
            TSNode identifier = findParameterName(child);
            if (!ts_node_is_null(identifier)) {
                SymbolInfo symbol;
                symbol.name = getNodeText(identifier, source);
                symbol.type = VARIABLE;
                symbol.dataType = analyzeDeclaratorType(child, extractDataType(node, source));
                symbol.line = ts_node_start_point(identifier).row;
                symbol.column = ts_node_start_point(identifier).column;
                symbol.scopeLevel = scopeLevel;
                symbol.description = "Variable";
                symbols.push_back(symbol);
            }
        }
    }
}

void CppLanguageProcessor::extractCppClasses(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const {
    TSPoint startPoint = ts_node_start_point(node);
    
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
        
        // Process class body with increased scope level
        TSNode body = ts_node_child_by_field_name(node, "body", 4);
        if (!ts_node_is_null(body)) {
            extractCppSymbols(body, source, symbols, scopeLevel + 1);
        }
    }
}

void CppLanguageProcessor::extractCppStructs(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const {
    TSPoint startPoint = ts_node_start_point(node);
    
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
        
        // Extract struct members
        TSNode body = ts_node_child_by_field_name(node, "body", 4);
        if (!ts_node_is_null(body)) {
            uint32_t memberCount = ts_node_child_count(body);
            for (uint32_t i = 0; i < memberCount; i++) {
                TSNode member = ts_node_child(body, i);
                if (strcmp(ts_node_type(member), "field_declaration") == 0) {
                    std::string baseType = extractDataType(member, source);
                    
                    uint32_t fieldChildCount = ts_node_child_count(member);
                    for (uint32_t j = 0; j < fieldChildCount; j++) {
                        TSNode fieldChild = ts_node_child(member, j);
                        if (strcmp(ts_node_type(fieldChild), "field_identifier") == 0) {
                            SymbolInfo memberSymbol;
                            memberSymbol.name = getNodeText(fieldChild, source);
                            memberSymbol.type = STRUCT_MEMBER;
                            memberSymbol.dataType = baseType;
                            memberSymbol.line = ts_node_start_point(fieldChild).row;
                            memberSymbol.column = ts_node_start_point(fieldChild).column;
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
}

void CppLanguageProcessor::extractCppEnums(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const {
    TSPoint startPoint = ts_node_start_point(node);
    
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
        
        // Extract enum values
        TSNode body = ts_node_child_by_field_name(node, "body", 4);
        if (!ts_node_is_null(body)) {
            uint32_t enumMemberCount = ts_node_child_count(body);
            for (uint32_t i = 0; i < enumMemberCount; i++) {
                TSNode enumMember = ts_node_child(body, i);
                if (strcmp(ts_node_type(enumMember), "enumerator") == 0) {
                    TSNode enumName = ts_node_child_by_field_name(enumMember, "name", 4);
                    if (!ts_node_is_null(enumName)) {
                        SymbolInfo enumSymbol;
                        enumSymbol.name = getNodeText(enumName, source);
                        enumSymbol.type = VARIABLE;
                        enumSymbol.dataType = symbol.name;
                        enumSymbol.line = ts_node_start_point(enumMember).row;
                        enumSymbol.column = ts_node_start_point(enumMember).column;
                        enumSymbol.scopeLevel = scopeLevel + 1;
                        enumSymbol.description = "Enum value";
                        enumSymbol.parentStruct = symbol.name;
                        symbols.push_back(enumSymbol);
                    }
                }
            }
        }
    }
}

void CppLanguageProcessor::extractCppNamespaces(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const {
    TSPoint startPoint = ts_node_start_point(node);
    
    TSNode nameNode = ts_node_child_by_field_name(node, "name", 4);
    if (!ts_node_is_null(nameNode)) {
        SymbolInfo symbol;
        symbol.name = getNodeText(nameNode, source);
        symbol.type = CLASS; // Use CLASS type for namespace
        symbol.dataType = "namespace";
        symbol.line = startPoint.row;
        symbol.column = startPoint.column;
        symbol.scopeLevel = scopeLevel;
        symbol.description = "Namespace";
        symbols.push_back(symbol);
        
        // Process namespace body with increased scope level
        TSNode body = ts_node_child_by_field_name(node, "body", 4);
        if (!ts_node_is_null(body)) {
            extractCppSymbols(body, source, symbols, scopeLevel + 1);
        }
    }
}