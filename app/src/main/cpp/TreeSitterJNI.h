#ifndef TREESITTER_JNI_H
#define TREESITTER_JNI_H

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

// 查询匹配结构
struct QueryMatch {
    std::string captureName;
    std::string nodeText;
    std::string nodeType;
    int startLine;
    int startColumn;
    int endLine;
    int endColumn;
};

// 查询结果结构
struct QueryResult {
    bool success;
    std::vector<QueryMatch> matches;
    std::string errorMessage;
};

// JNI工具函数声明
jobject createSymbolInfo(JNIEnv *env, const SymbolInfo &symbol);
jobject createScopeInfo(JNIEnv *env, const ScopeInfo &scope);
jobject createParseResult(JNIEnv *env, const ParseResult &result);
jobject createQueryMatch(JNIEnv *env, const QueryMatch &match);
jobject createQueryResult(JNIEnv *env, const QueryResult &result);

// 核心解析函数声明
ParseResult parseCppCodeCore(const std::string &code);
ParseResult parseJavaCodeCore(const std::string &code);
ParseResult parsePythonCodeCore(const std::string &code);

// 查询系统函数声明
QueryResult executeQuery(const std::string &code, const std::string &language, const std::string &query);

// 辅助函数声明
TSLanguage* getLanguageByName(const std::string &language);
std::string getNodeText(TSNode node, const std::string &source);
TSNode findFunctionName(TSNode declarator);
TSNode findParameterName(TSNode declarator);
void traverseNodeForSymbols(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel = 0);
void traverseNodeForScopes(TSNode node, std::vector<ScopeInfo> &scopes, int currentLevel = 0);

#endif // TREESITTER_JNI_H