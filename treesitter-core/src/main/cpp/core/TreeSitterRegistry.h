#ifndef TREESITTER_REGISTRY_H
#define TREESITTER_REGISTRY_H

#include "ILanguageProcessor.h"
#include <unordered_map>
#include <memory>
#include <vector>

/**
 * TreeSitter语言注册管理器
 * 负责管理所有语言处理器的注册、查找和生命周期
 */
class TreeSitterRegistry {
private:
    // 单例实例
    static std::unique_ptr<TreeSitterRegistry> instance;
    
    // 语言处理器映射表（按语言ID索引）
    std::unordered_map<std::string, std::unique_ptr<ILanguageProcessor>> processors;
    
    // 文件扩展名到语言ID的映射
    std::unordered_map<std::string, std::string> extensionToLanguage;
    
    // 私有构造函数
    TreeSitterRegistry() = default;

public:
    // 获取单例实例
    static TreeSitterRegistry& getInstance();
    
    // 禁用拷贝和移动
    TreeSitterRegistry(const TreeSitterRegistry&) = delete;
    TreeSitterRegistry& operator=(const TreeSitterRegistry&) = delete;
    TreeSitterRegistry(TreeSitterRegistry&&) = delete;
    TreeSitterRegistry& operator=(TreeSitterRegistry&&) = delete;
    
    /**
     * 注册语言处理器
     * @param processor 语言处理器实例
     * @return 注册是否成功
     */
    bool registerProcessor(std::unique_ptr<ILanguageProcessor> processor);
    
    /**
     * 根据语言ID获取处理器
     * @param languageId 语言标识符
     * @return 语言处理器指针，如果未找到返回nullptr
     */
    ILanguageProcessor* getProcessor(const std::string& languageId) const;
    
    /**
     * 根据文件扩展名获取处理器
     * @param extension 文件扩展名（如"cpp", "java"）
     * @return 语言处理器指针，如果未找到返回nullptr
     */
    ILanguageProcessor* getProcessorByExtension(const std::string& extension) const;
    
    /**
     * 获取所有已注册的语言ID
     */
    std::vector<std::string> getRegisteredLanguages() const;
    
    /**
     * 获取所有支持的语言ID（别名方法）
     */
    std::vector<std::string> getSupportedLanguages() const;
    
    /**
     * 检查指定语言是否已注册
     */
    bool isLanguageRegistered(const std::string& languageId) const;
    
    /**
     * 初始化所有内置语言处理器
     */
    void initializeBuiltinProcessors();
    
    /**
     * 清理所有注册的处理器
     */
    void clear();
};

#endif // TREESITTER_REGISTRY_H