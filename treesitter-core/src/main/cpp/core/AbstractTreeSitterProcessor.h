#ifndef ABSTRACT_TREESITTER_PROCESSOR_H
#define ABSTRACT_TREESITTER_PROCESSOR_H

#include "ILanguageProcessor.h"
#include "../TreeSitterJNI.h"
#include <memory>

/**
 * 抽象TreeSitter处理器基类
 * 提供通用的TreeSitter解析功能和工具方法
 */
class AbstractTreeSitterProcessor : public ILanguageProcessor {
protected:
    // 通用工具方法
    
    /**
     * 获取节点文本内容
     */
    std::string getNodeText(TSNode node, const std::string &source) const;
    
    /**
     * 查找函数名节点
     */
    TSNode findFunctionName(TSNode declarator) const;
    
    /**
     * 查找参数名节点
     */
    TSNode findParameterName(TSNode declarator) const;
    
    /**
     * 提取数据类型
     */
    std::string extractDataType(TSNode declarationNode, const std::string &source) const;
    
    /**
     * 分析声明器类型（指针、引用等修饰符）
     */
    std::string analyzeDeclaratorType(TSNode declarator, const std::string &baseType) const;
    
    /**
     * 遍历节点提取作用域信息
     */
    void traverseNodeForScopes(TSNode node, std::vector<ScopeInfo> &scopes, int currentLevel = 0) const;
    
    /**
     * 执行通用的TreeSitter解析流程
     */
    ParseResult executeTreeSitterParsing(const std::string &code, const TSLanguage* language) const;

public:
    virtual ~AbstractTreeSitterProcessor() = default;
    
    // 实现ILanguageProcessor的基本方法
    bool isAvailable() const override;
    ParseResult parseCode(const std::string &code) override final;

protected:
    /**
     * 语言特定的符号提取方法，由子类实现
     */
    virtual void extractLanguageSpecificSymbols(
        TSNode rootNode, 
        const std::string &source, 
        std::vector<SymbolInfo> &symbols, 
        int scopeLevel = 0
    ) const = 0;
};

#endif // ABSTRACT_TREESITTER_PROCESSOR_H