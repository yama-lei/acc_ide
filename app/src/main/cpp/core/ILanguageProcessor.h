#ifndef ILANGUAGE_PROCESSOR_H
#define ILANGUAGE_PROCESSOR_H

#include <string>
#include <vector>
#include <memory>
#include "../TreeSitterJNI.h"

/**
 * 语言处理器接口
 * 定义了所有语言处理器必须实现的基本功能
 */
class ILanguageProcessor {
public:
    virtual ~ILanguageProcessor() = default;
    
    /**
     * 获取语言标识符
     */
    virtual std::string getLanguageId() const = 0;
    
    /**
     * 获取语言显示名称
     */
    virtual std::string getLanguageName() const = 0;
    
    /**
     * 获取支持的文件扩展名
     */
    virtual std::vector<std::string> getSupportedExtensions() const = 0;
    
    /**
     * 解析代码并返回符号和作用域信息
     */
    virtual ParseResult parseCode(const std::string &code) = 0;
    
    /**
     * 检查处理器是否可用
     */
    virtual bool isAvailable() const = 0;
    
    /**
     * 获取TreeSitter语言实例
     */
    virtual const TSLanguage* getTreeSitterLanguage() const = 0;
};

#endif // ILANGUAGE_PROCESSOR_H