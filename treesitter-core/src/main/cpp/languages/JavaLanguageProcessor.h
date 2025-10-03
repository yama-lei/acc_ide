#ifndef JAVA_LANGUAGE_PROCESSOR_H
#define JAVA_LANGUAGE_PROCESSOR_H

#include "../core/AbstractTreeSitterProcessor.h"

extern "C" {
    const TSLanguage *tree_sitter_java(void);
}

/**
 * Java语言处理器 / Java Language Processor
 * 实现Java特定的语法树遍历和符号提取逻辑
 */
class JavaLanguageProcessor : public AbstractTreeSitterProcessor {
public:
    std::string getLanguageId() const override;
    std::string getLanguageName() const override;
    std::vector<std::string> getSupportedExtensions() const override;
    const TSLanguage* getTreeSitterLanguage() const override;

protected:
    void extractLanguageSpecificSymbols(
        TSNode rootNode, 
        const std::string &source, 
        std::vector<SymbolInfo> &symbols, 
        int scopeLevel = 0
    ) const override;

private:
    void extractJavaSymbols(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const;
    void extractJavaClasses(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const;
    void extractJavaMethods(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const;
    void extractJavaFields(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const;
    void extractJavaLocalVariables(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const;
};

#endif // JAVA_LANGUAGE_PROCESSOR_H