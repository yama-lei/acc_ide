#include "TreeSitterJNI.h"
#include "core/TreeSitterRegistry.h"
#include <android/log.h>

#define LOG_TAG "TreeSitterJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// JNI工具函数实现
jobject createSymbolInfo(JNIEnv *env, const SymbolInfo &symbol) {
    jclass symbolClass = env->FindClass("com/acc_ide/treesitter/core/SymbolInfo");
    jmethodID constructor = env->GetMethodID(symbolClass, "<init>", 
        "(Ljava/lang/String;Lcom/acc_ide/treesitter/core/SymbolType;Ljava/lang/String;IIILjava/lang/String;Ljava/lang/String;)V");
    
    jstring name = env->NewStringUTF(symbol.name.c_str());
    jstring dataType = env->NewStringUTF(symbol.dataType.c_str());
    jstring description = env->NewStringUTF(symbol.description.c_str());
    jstring parentStruct = env->NewStringUTF(symbol.parentStruct.c_str());
    
    // 获取SymbolType枚举
    jclass symbolTypeClass = env->FindClass("com/acc_ide/treesitter/core/SymbolType");
    jfieldID typeField;
    
    switch (symbol.type) {
        case VARIABLE:
            typeField = env->GetStaticFieldID(symbolTypeClass, "VARIABLE", "Lcom/acc_ide/treesitter/core/SymbolType;");
            break;
        case FUNCTION:
            typeField = env->GetStaticFieldID(symbolTypeClass, "FUNCTION", "Lcom/acc_ide/treesitter/core/SymbolType;");
            break;
        case CLASS:
            typeField = env->GetStaticFieldID(symbolTypeClass, "CLASS", "Lcom/acc_ide/treesitter/core/SymbolType;");
            break;
        case STRUCT:
            typeField = env->GetStaticFieldID(symbolTypeClass, "STRUCT", "Lcom/acc_ide/treesitter/core/SymbolType;");
            break;
        case ENUM:
            typeField = env->GetStaticFieldID(symbolTypeClass, "ENUM", "Lcom/acc_ide/treesitter/core/SymbolType;");
            break;
        case PARAMETER:
            typeField = env->GetStaticFieldID(symbolTypeClass, "PARAMETER", "Lcom/acc_ide/treesitter/core/SymbolType;");
            break;
        case STRUCT_MEMBER:
            typeField = env->GetStaticFieldID(symbolTypeClass, "STRUCT_MEMBER", "Lcom/acc_ide/treesitter/core/SymbolType;");
            break;
        default:
            typeField = env->GetStaticFieldID(symbolTypeClass, "VARIABLE", "Lcom/acc_ide/treesitter/core/SymbolType;");
    }
    
    jobject typeEnum = env->GetStaticObjectField(symbolTypeClass, typeField);
    
    return env->NewObject(symbolClass, constructor, name, typeEnum, dataType, 
                         symbol.line, symbol.column, symbol.scopeLevel, description, parentStruct);
}

jobject createScopeInfo(JNIEnv *env, const ScopeInfo &scope) {
    jclass scopeClass = env->FindClass("com/acc_ide/treesitter/core/ScopeInfo");
    jmethodID constructor = env->GetMethodID(scopeClass, "<init>", "(IIILcom/acc_ide/treesitter/core/ScopeType;)V");
    
    // 获取ScopeType枚举
    jclass scopeTypeClass = env->FindClass("com/acc_ide/treesitter/core/ScopeType");
    jfieldID typeField;
    
    switch (scope.type) {
        case SCOPE_FUNCTION:
            typeField = env->GetStaticFieldID(scopeTypeClass, "SCOPE_FUNCTION", "Lcom/acc_ide/treesitter/core/ScopeType;");
            break;
        case SCOPE_CLASS:
            typeField = env->GetStaticFieldID(scopeTypeClass, "SCOPE_CLASS", "Lcom/acc_ide/treesitter/core/ScopeType;");
            break;
        case SCOPE_BLOCK:
            typeField = env->GetStaticFieldID(scopeTypeClass, "SCOPE_BLOCK", "Lcom/acc_ide/treesitter/core/ScopeType;");
            break;
        case SCOPE_NAMESPACE:
            typeField = env->GetStaticFieldID(scopeTypeClass, "SCOPE_NAMESPACE", "Lcom/acc_ide/treesitter/core/ScopeType;");
            break;
        case SCOPE_GLOBAL:
            typeField = env->GetStaticFieldID(scopeTypeClass, "SCOPE_GLOBAL", "Lcom/acc_ide/treesitter/core/ScopeType;");
            break;
        default:
            typeField = env->GetStaticFieldID(scopeTypeClass, "SCOPE_GLOBAL", "Lcom/acc_ide/treesitter/core/ScopeType;");
    }
    
    jobject typeEnum = env->GetStaticObjectField(scopeTypeClass, typeField);
    
    return env->NewObject(scopeClass, constructor, scope.level, scope.startLine, scope.endLine, typeEnum);
}

jobject createParseResult(JNIEnv *env, const ParseResult &result) {
    jclass parseResultClass = env->FindClass("com/acc_ide/treesitter/core/ParseResult");
    jmethodID constructor = env->GetMethodID(parseResultClass, "<init>", "(Ljava/util/List;Ljava/util/List;)V");
    
    // 创建符号列表
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "()V");
    jmethodID arrayListAdd = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    
    jobject symbolList = env->NewObject(arrayListClass, arrayListConstructor);
    for (const auto &symbol : result.symbols) {
        jobject symbolInfo = createSymbolInfo(env, symbol);
        env->CallBooleanMethod(symbolList, arrayListAdd, symbolInfo);
        env->DeleteLocalRef(symbolInfo);
    }
    
    // 创建作用域列表
    jobject scopeList = env->NewObject(arrayListClass, arrayListConstructor);
    for (const auto &scope : result.scopes) {
        jobject scopeInfo = createScopeInfo(env, scope);
        env->CallBooleanMethod(scopeList, arrayListAdd, scopeInfo);
        env->DeleteLocalRef(scopeInfo);
    }
    
    return env->NewObject(parseResultClass, constructor, symbolList, scopeList);
}

// JNI接口函数 - 使用新的模块化架构
extern "C" {

JNIEXPORT jobject JNICALL
Java_com_acc_1ide_treesitter_TreeSitterService_parseCppCode(JNIEnv *env, jobject /* thiz */, jstring code) {
    const char *codeStr = env->GetStringUTFChars(code, nullptr);
    if (!codeStr) {
        LOGE("Failed to get code string");
        return nullptr;
    }
    
    LOGD("Parsing C++ code using modular architecture");
    
    try {
        // 使用注册管理器获取C++处理器
        auto& registry = TreeSitterRegistry::getInstance();
        auto processor = registry.getProcessor("cpp");
        
        if (!processor) {
            LOGE("Failed to get C++ processor");
            env->ReleaseStringUTFChars(code, codeStr);
            return nullptr;
        }
        
        // 使用处理器解析代码
        ParseResult result = processor->parseCode(std::string(codeStr));
        
        env->ReleaseStringUTFChars(code, codeStr);
        return createParseResult(env, result);
        
    } catch (const std::exception& e) {
        LOGE("Error parsing C++ code: %s", e.what());
        env->ReleaseStringUTFChars(code, codeStr);
        return nullptr;
    }
}

JNIEXPORT jobject JNICALL
Java_com_acc_1ide_treesitter_TreeSitterService_parseJavaCode(JNIEnv *env, jobject /* thiz */, jstring code) {
    const char *codeStr = env->GetStringUTFChars(code, nullptr);
    if (!codeStr) {
        LOGE("Failed to get code string");
        return nullptr;
    }
    
    LOGD("Parsing Java code using modular architecture");
    
    try {
        // 使用注册管理器获取Java处理器
        auto& registry = TreeSitterRegistry::getInstance();
        auto processor = registry.getProcessor("java");
        
        if (!processor) {
            LOGE("Failed to get Java processor");
            env->ReleaseStringUTFChars(code, codeStr);
            return nullptr;
        }
        
        // 使用处理器解析代码
        ParseResult result = processor->parseCode(std::string(codeStr));
        
        env->ReleaseStringUTFChars(code, codeStr);
        return createParseResult(env, result);
        
    } catch (const std::exception& e) {
        LOGE("Error parsing Java code: %s", e.what());
        env->ReleaseStringUTFChars(code, codeStr);
        return nullptr;
    }
}

JNIEXPORT jobject JNICALL
Java_com_acc_1ide_treesitter_TreeSitterService_parsePythonCode(JNIEnv *env, jobject /* thiz */, jstring code) {
    const char *codeStr = env->GetStringUTFChars(code, nullptr);
    if (!codeStr) {
        LOGE("Failed to get code string");
        return nullptr;
    }
    
    LOGD("Parsing Python code using modular architecture");
    
    try {
        // 使用注册管理器获取Python处理器
        auto& registry = TreeSitterRegistry::getInstance();
        auto processor = registry.getProcessor("python");
        
        if (!processor) {
            LOGE("Failed to get Python processor");
            env->ReleaseStringUTFChars(code, codeStr);
            return nullptr;
        }
        
        // 使用处理器解析代码
        ParseResult result = processor->parseCode(std::string(codeStr));
        
        env->ReleaseStringUTFChars(code, codeStr);
        return createParseResult(env, result);
        
    } catch (const std::exception& e) {
        LOGE("Error parsing Python code: %s", e.what());
        env->ReleaseStringUTFChars(code, codeStr);
        return nullptr;
    }
}

// 通用语言解析接口 - 新增功能
JNIEXPORT jobject JNICALL
Java_com_acc_1ide_treesitter_TreeSitterService_parseCode(JNIEnv *env, jobject /* thiz */, jstring code, jstring language) {
    const char *codeStr = env->GetStringUTFChars(code, nullptr);
    const char *langStr = env->GetStringUTFChars(language, nullptr);
    
    if (!codeStr || !langStr) {
        LOGE("Failed to get code or language string");
        if (codeStr) env->ReleaseStringUTFChars(code, codeStr);
        if (langStr) env->ReleaseStringUTFChars(language, langStr);
        return nullptr;
    }
    
    LOGD("Parsing code for language: %s", langStr);
    
    try {
        // 使用注册管理器获取指定语言的处理器
        auto& registry = TreeSitterRegistry::getInstance();
        auto processor = registry.getProcessor(std::string(langStr));
        
        if (!processor) {
            LOGE("Failed to get processor for language: %s", langStr);
            env->ReleaseStringUTFChars(code, codeStr);
            env->ReleaseStringUTFChars(language, langStr);
            return nullptr;
        }
        
        // 使用处理器解析代码
        ParseResult result = processor->parseCode(std::string(codeStr));
        
        env->ReleaseStringUTFChars(code, codeStr);
        env->ReleaseStringUTFChars(language, langStr);
        return createParseResult(env, result);
        
    } catch (const std::exception& e) {
        LOGE("Error parsing code for language %s: %s", langStr, e.what());
        env->ReleaseStringUTFChars(code, codeStr);
        env->ReleaseStringUTFChars(language, langStr);
        return nullptr;
    }
}

// 获取支持的语言列表 - 新增功能
JNIEXPORT jobject JNICALL
Java_com_acc_1ide_treesitter_TreeSitterService_getSupportedLanguages(JNIEnv *env, jobject /* thiz */) {
    try {
        auto& registry = TreeSitterRegistry::getInstance();
        auto languages = registry.getSupportedLanguages();
        
        // 创建字符串列表
        jclass arrayListClass = env->FindClass("java/util/ArrayList");
        jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "()V");
        jmethodID arrayListAdd = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
        
        jobject languageList = env->NewObject(arrayListClass, arrayListConstructor);
        
        for (const auto& lang : languages) {
            jstring langStr = env->NewStringUTF(lang.c_str());
            env->CallBooleanMethod(languageList, arrayListAdd, langStr);
            env->DeleteLocalRef(langStr);
        }
        
        return languageList;
        
    } catch (const std::exception& e) {
        LOGE("Error getting supported languages: %s", e.what());
        return nullptr;
    }
}

} // extern "C"