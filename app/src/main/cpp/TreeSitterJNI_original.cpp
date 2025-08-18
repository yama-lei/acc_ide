#include "TreeSitterJNI.h"
#include <android/log.h>

#define LOG_TAG "TreeSitterJNI"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// JNI工具函数实现
jobject createSymbolInfo(JNIEnv *env, const SymbolInfo &symbol) {
    jclass symbolClass = env->FindClass("com/acc_ide/completion/core/SymbolInfo");
    jmethodID constructor = env->GetMethodID(symbolClass, "<init>", 
        "(Ljava/lang/String;Lcom/acc_ide/completion/core/SymbolType;Ljava/lang/String;IIILjava/lang/String;Ljava/lang/String;)V");
    
    jstring name = env->NewStringUTF(symbol.name.c_str());
    jstring dataType = env->NewStringUTF(symbol.dataType.c_str());
    jstring description = env->NewStringUTF(symbol.description.c_str());
    jstring parentStruct = env->NewStringUTF(symbol.parentStruct.c_str());
    
    // 获取SymbolType枚举
    jclass symbolTypeClass = env->FindClass("com/acc_ide/completion/core/SymbolType");
    jfieldID typeField;
    
    switch (symbol.type) {
        case VARIABLE:
            typeField = env->GetStaticFieldID(symbolTypeClass, "VARIABLE", "Lcom/acc_ide/completion/core/SymbolType;");
            break;
        case FUNCTION:
            typeField = env->GetStaticFieldID(symbolTypeClass, "FUNCTION", "Lcom/acc_ide/completion/core/SymbolType;");
            break;
        case CLASS:
            typeField = env->GetStaticFieldID(symbolTypeClass, "CLASS", "Lcom/acc_ide/completion/core/SymbolType;");
            break;
        case STRUCT:
            typeField = env->GetStaticFieldID(symbolTypeClass, "STRUCT", "Lcom/acc_ide/completion/core/SymbolType;");
            break;
        case ENUM:
            typeField = env->GetStaticFieldID(symbolTypeClass, "ENUM", "Lcom/acc_ide/completion/core/SymbolType;");
            break;
        case PARAMETER:
            typeField = env->GetStaticFieldID(symbolTypeClass, "PARAMETER", "Lcom/acc_ide/completion/core/SymbolType;");
            break;
        case STRUCT_MEMBER:
            typeField = env->GetStaticFieldID(symbolTypeClass, "STRUCT_MEMBER", "Lcom/acc_ide/completion/core/SymbolType;");
            break;
        default:
            typeField = env->GetStaticFieldID(symbolTypeClass, "VARIABLE", "Lcom/acc_ide/completion/core/SymbolType;");
    }
    
    jobject typeEnum = env->GetStaticObjectField(symbolTypeClass, typeField);
    
    return env->NewObject(symbolClass, constructor, name, typeEnum, dataType, 
                         symbol.line, symbol.column, symbol.scopeLevel, description, parentStruct);
}

jobject createScopeInfo(JNIEnv *env, const ScopeInfo &scope) {
    jclass scopeClass = env->FindClass("com/acc_ide/completion/core/ScopeInfo");
    jmethodID constructor = env->GetMethodID(scopeClass, "<init>", "(IIILcom/acc_ide/completion/core/ScopeType;)V");
    
    // 获取ScopeType枚举
    jclass scopeTypeClass = env->FindClass("com/acc_ide/completion/core/ScopeType");
    jfieldID typeField;
    
    switch (scope.type) {
        case FUNCTION_SCOPE:
            typeField = env->GetStaticFieldID(scopeTypeClass, "FUNCTION", "Lcom/acc_ide/completion/core/ScopeType;");
            break;
        case CLASS_SCOPE:
            typeField = env->GetStaticFieldID(scopeTypeClass, "CLASS", "Lcom/acc_ide/completion/core/ScopeType;");
            break;
        case BLOCK_SCOPE:
            typeField = env->GetStaticFieldID(scopeTypeClass, "BLOCK", "Lcom/acc_ide/completion/core/ScopeType;");
            break;
        case NAMESPACE_SCOPE:
            typeField = env->GetStaticFieldID(scopeTypeClass, "NAMESPACE", "Lcom/acc_ide/completion/core/ScopeType;");
            break;
        case GLOBAL_SCOPE:
            typeField = env->GetStaticFieldID(scopeTypeClass, "GLOBAL", "Lcom/acc_ide/completion/core/ScopeType;");
            break;
        default:
            typeField = env->GetStaticFieldID(scopeTypeClass, "GLOBAL", "Lcom/acc_ide/completion/core/ScopeType;");
    }
    
    jobject typeEnum = env->GetStaticObjectField(scopeTypeClass, typeField);
    
    return env->NewObject(scopeClass, constructor, scope.level, scope.startLine, scope.endLine, typeEnum);
}

jobject createParseResult(JNIEnv *env, const ParseResult &result) {
    jclass parseResultClass = env->FindClass("com/acc_ide/completion/core/ParseResult");
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

jobject createQueryMatch(JNIEnv *env, const QueryMatch &match) {
    jclass queryMatchClass = env->FindClass("com/acc_ide/completion/core/QueryMatch");
    jmethodID constructor = env->GetMethodID(queryMatchClass, "<init>", 
        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;IIII)V");
    
    jstring captureName = env->NewStringUTF(match.captureName.c_str());
    jstring nodeText = env->NewStringUTF(match.nodeText.c_str());
    jstring nodeType = env->NewStringUTF(match.nodeType.c_str());
    
    return env->NewObject(queryMatchClass, constructor, captureName, nodeText, nodeType,
                         match.startLine, match.startColumn, match.endLine, match.endColumn);
}

jobject createQueryResult(JNIEnv *env, const QueryResult &result) {
    jclass queryResultClass = env->FindClass("com/acc_ide/completion/core/QueryResult");
    jmethodID constructor = env->GetMethodID(queryResultClass, "<init>", "(ZLjava/util/List;Ljava/lang/String;)V");
    
    // 创建匹配列表
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID arrayListConstructor = env->GetMethodID(arrayListClass, "<init>", "()V");
    jmethodID arrayListAdd = env->GetMethodID(arrayListClass, "add", "(Ljava/lang/Object;)Z");
    
    jobject matchList = env->NewObject(arrayListClass, arrayListConstructor);
    for (const auto &match : result.matches) {
        jobject queryMatch = createQueryMatch(env, match);
        env->CallBooleanMethod(matchList, arrayListAdd, queryMatch);
        env->DeleteLocalRef(queryMatch);
    }
    
    jstring errorMessage = env->NewStringUTF(result.errorMessage.c_str());
    
    return env->NewObject(queryResultClass, constructor, result.success, matchList, errorMessage);
}

// JNI接口函数
extern "C" {

JNIEXPORT jobject JNICALL
Java_com_acc_1ide_completion_services_NativeTreeSitterService_parseCppCode(JNIEnv *env, jobject thiz, jstring code) {
    const char *codeStr = env->GetStringUTFChars(code, nullptr);
    if (!codeStr) {
        LOGE("Failed to get code string");
        return nullptr;
    }
    
    LOGD("Parsing C++ code: %s", codeStr);
    
    ParseResult result = parseCppCodeCore(codeStr);
    
    env->ReleaseStringUTFChars(code, codeStr);
    
    return createParseResult(env, result);
}

JNIEXPORT jobject JNICALL
Java_com_acc_1ide_completion_services_NativeTreeSitterService_parseJavaCode(JNIEnv *env, jobject thiz, jstring code) {
    const char *codeStr = env->GetStringUTFChars(code, nullptr);
    if (!codeStr) {
        LOGE("Failed to get code string");
        return nullptr;
    }
    
    LOGD("Parsing Java code: %s", codeStr);
    
    ParseResult result = parseJavaCodeCore(codeStr);
    
    env->ReleaseStringUTFChars(code, codeStr);
    
    return createParseResult(env, result);
}

JNIEXPORT jobject JNICALL
Java_com_acc_1ide_completion_services_NativeTreeSitterService_parsePythonCode(JNIEnv *env, jobject thiz, jstring code) {
    const char *codeStr = env->GetStringUTFChars(code, nullptr);
    if (!codeStr) {
        LOGE("Failed to get code string");
        return nullptr;
    }
    
    LOGD("Parsing Python code: %s", codeStr);
    
    ParseResult result = parsePythonCodeCore(codeStr);
    
    env->ReleaseStringUTFChars(code, codeStr);
    
    return createParseResult(env, result);
}

JNIEXPORT jobject JNICALL
Java_com_acc_1ide_completion_services_NativeTreeSitterService_executeQuery(JNIEnv *env, jobject thiz, 
                                                                          jstring code, jstring language, jstring query) {
    const char *codeStr = env->GetStringUTFChars(code, nullptr);
    const char *languageStr = env->GetStringUTFChars(language, nullptr);
    const char *queryStr = env->GetStringUTFChars(query, nullptr);
    
    if (!codeStr || !languageStr || !queryStr) {
        LOGE("Failed to get string parameters");
        if (codeStr) env->ReleaseStringUTFChars(code, codeStr);
        if (languageStr) env->ReleaseStringUTFChars(language, languageStr);
        if (queryStr) env->ReleaseStringUTFChars(query, queryStr);
        return nullptr;
    }
    
    LOGD("Executing query for language: %s", languageStr);
    
    QueryResult result = executeQuery(codeStr, languageStr, queryStr);
    
    env->ReleaseStringUTFChars(code, codeStr);
    env->ReleaseStringUTFChars(language, languageStr);
    env->ReleaseStringUTFChars(query, queryStr);
    
    return createQueryResult(env, result);
}

} // extern "C"