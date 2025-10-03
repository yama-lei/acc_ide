#include "TreeSitterRegistry.h"
#include "../languages/CppLanguageProcessor.h"
#include "../languages/JavaLanguageProcessor.h"
#include "../languages/PythonLanguageProcessor.h"
#include <android/log.h>

#define LOG_TAG "TreeSitterRegistry"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

std::unique_ptr<TreeSitterRegistry> TreeSitterRegistry::instance = nullptr;

TreeSitterRegistry& TreeSitterRegistry::getInstance() {
    if (!instance) {
        instance = std::unique_ptr<TreeSitterRegistry>(new TreeSitterRegistry());
        instance->initializeBuiltinProcessors();
    }
    return *instance;
}

/**
 * 注册语言处理器 / Register Language Processor
 * 
 * @param processor Unique pointer to language processor
 * @return true if registration successful, false otherwise
 */
bool TreeSitterRegistry::registerProcessor(std::unique_ptr<ILanguageProcessor> processor) {
    if (!processor) {
        LOGE("Cannot register null processor");
        return false;
    }
    
    std::string languageId = processor->getLanguageId();
    
    // Check if language is already registered
    if (processors.find(languageId) != processors.end()) {
        LOGD("Language %s is already registered, replacing...", languageId.c_str());
    }
    
    // Register file extensions
    auto extensions = processor->getSupportedExtensions();
    for (const auto& ext : extensions) {
        extensionToLanguage[ext] = languageId;
        LOGD("Registered extension .%s -> %s", ext.c_str(), languageId.c_str());
    }
    
    // Store processor
    processors[languageId] = std::move(processor);
    LOGD("Registered language processor: %s", languageId.c_str());
    
    return true;
}

ILanguageProcessor* TreeSitterRegistry::getProcessor(const std::string& languageId) const {
    auto it = processors.find(languageId);
    if (it != processors.end()) {
        return it->second.get();
    }
    return nullptr;
}

ILanguageProcessor* TreeSitterRegistry::getProcessorByExtension(const std::string& extension) const {
    auto it = extensionToLanguage.find(extension);
    if (it != extensionToLanguage.end()) {
        return getProcessor(it->second);
    }
    return nullptr;
}

std::vector<std::string> TreeSitterRegistry::getRegisteredLanguages() const {
    std::vector<std::string> languages;
    languages.reserve(processors.size());
    
    for (const auto& pair : processors) {
        languages.push_back(pair.first);
    }
    
    return languages;
}

std::vector<std::string> TreeSitterRegistry::getSupportedLanguages() const {
    return getRegisteredLanguages(); // Alias method
}

bool TreeSitterRegistry::isLanguageRegistered(const std::string& languageId) const {
    return processors.find(languageId) != processors.end();
}

/**
 * 初始化内置语言处理器 / Initialize Builtin Language Processors
 * 注册C++、Java和Python语言处理器
 */
void TreeSitterRegistry::initializeBuiltinProcessors() {
    LOGD("Initializing builtin language processors...");
    
    // Register C++ processor
    auto cppProcessor = std::make_unique<CppLanguageProcessor>();
    if (cppProcessor->isAvailable()) {
        registerProcessor(std::move(cppProcessor));
        LOGD("C++ processor registered successfully");
    } else {
        LOGE("C++ processor not available");
    }
    
    // Register Java processor
    auto javaProcessor = std::make_unique<JavaLanguageProcessor>();
    if (javaProcessor->isAvailable()) {
        registerProcessor(std::move(javaProcessor));
        LOGD("Java processor registered successfully");
    } else {
        LOGE("Java processor not available");
    }
    
    // Register Python processor
    auto pythonProcessor = std::make_unique<PythonLanguageProcessor>();
    if (pythonProcessor->isAvailable()) {
        registerProcessor(std::move(pythonProcessor));
        LOGD("Python processor registered successfully");
    } else {
        LOGE("Python processor not available");
    }
    
    LOGD("Builtin language processors initialization completed");
}

void TreeSitterRegistry::clear() {
    processors.clear();
    extensionToLanguage.clear();
    LOGD("Registry cleared");
}