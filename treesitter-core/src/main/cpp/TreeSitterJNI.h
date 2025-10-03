#ifndef TREESITTER_JNI_H
#define TREESITTER_JNI_H

#include <jni.h>
#include <string>
#include <vector>
#include <memory>
#include "tree_sitter/api.h"

// TreeSitter语言声明
extern "C" {
    const TSLanguage *tree_sitter_cpp(void);
    const TSLanguage *tree_sitter_java(void);
    const TSLanguage *tree_sitter_python(void);
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
    SCOPE_FUNCTION = 0,
    SCOPE_CLASS = 1,
    SCOPE_BLOCK = 2,
    SCOPE_NAMESPACE = 3,
    SCOPE_GLOBAL = 4
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
const TSLanguage* getLanguageByName(const std::string &language);
std::string getNodeText(TSNode node, const std::string &source);
TSNode findFunctionName(TSNode declarator);
TSNode findParameterName(TSNode declarator);
void traverseNodeForSymbols(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel = 0, const std::string &language = "cpp");
void traverseNodeForScopes(TSNode node, std::vector<ScopeInfo> &scopes, int currentLevel = 0);

#endif // TREESITTER_JNI_H