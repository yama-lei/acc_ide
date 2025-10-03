#ifndef CPP_LANGUAGE_PROCESSOR_H
#define CPP_LANGUAGE_PROCESSOR_H

#include "../core/AbstractTreeSitterProcessor.h"

// TreeSitter C++语言声明
extern "C" {
    const TSLanguage *tree_sitter_cpp(void);
}

/**
 * C++语言处理器
 * 实现C++特定的语法树遍历和符号提取逻辑
 */
class CppLanguageProcessor : public AbstractTreeSitterProcessor {
public:
    // 实现ILanguageProcessor接口
    std::string getLanguageId() const override;
    std::string getLanguageName() const override;
    std::vector<std::string> getSupportedExtensions() const override;
    const TSLanguage* getTreeSitterLanguage() const override;

protected:
    // 实现语言特定的符号提取
    void extractLanguageSpecificSymbols(
        TSNode rootNode, 
        const std::string &source, 
        std::vector<SymbolInfo> &symbols, 
        int scopeLevel = 0
    ) const override;

private:
    // C++特定的符号提取方法
    void extractCppSymbols(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const;
    void extractCppFunctions(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const;
    void extractCppVariables(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const;
    void extractCppClasses(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const;
    void extractCppStructs(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const;
    void extractCppEnums(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const;
    void extractCppNamespaces(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const;
};

#endif // CPP_LANGUAGE_PROCESSOR_H