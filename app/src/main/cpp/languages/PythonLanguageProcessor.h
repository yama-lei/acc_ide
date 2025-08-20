#ifndef PYTHON_LANGUAGE_PROCESSOR_H
#define PYTHON_LANGUAGE_PROCESSOR_H

#include "../core/AbstractTreeSitterProcessor.h"

extern "C" {
    const TSLanguage *tree_sitter_python(void);
}

/**
 * Python语言处理器 / Python Language Processor
 * 实现Python特定的语法树遍历和符号提取逻辑
 */
class PythonLanguageProcessor : public AbstractTreeSitterProcessor {
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
    void extractPythonSymbols(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const;
    void extractPythonFunctions(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const;
    void extractPythonClasses(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const;
    void extractPythonAssignments(TSNode node, const std::string &source, std::vector<SymbolInfo> &symbols, int scopeLevel) const;
};

#endif // PYTHON_LANGUAGE_PROCESSOR_H