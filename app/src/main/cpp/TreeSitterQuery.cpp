#include "TreeSitterJNI.h"
#include <android/log.h>

#define LOG_TAG "TreeSitterQuery"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// 执行TreeSitter查询
QueryResult executeQuery(const std::string &code, const std::string &language, const std::string &query) {
    QueryResult result;
    result.success = false;
    
    LOGD("Executing query for language: %s", language.c_str());
    LOGD("Query: %s", query.c_str());
    
    // 获取语言实例
    TSLanguage *tsLanguage = getLanguageByName(language);
    if (!tsLanguage) {
        result.errorMessage = "Unsupported language: " + language;
        LOGE("%s", result.errorMessage.c_str());
        return result;
    }
    
    // 创建解析器
    TSParser *parser = ts_parser_new();
    if (!parser) {
        result.errorMessage = "Failed to create parser";
        LOGE("%s", result.errorMessage.c_str());
        return result;
    }
    
    if (!ts_parser_set_language(parser, tsLanguage)) {
        result.errorMessage = "Failed to set language";
        LOGE("%s", result.errorMessage.c_str());
        ts_parser_delete(parser);
        return result;
    }
    
    // 解析代码
    TSTree *tree = ts_parser_parse_string(parser, nullptr, code.c_str(), code.length());
    if (!tree) {
        result.errorMessage = "Failed to parse code";
        LOGE("%s", result.errorMessage.c_str());
        ts_parser_delete(parser);
        return result;
    }
    
    // 创建查询
    uint32_t errorOffset;
    TSQueryError errorType;
    TSQuery *tsQuery = ts_query_new(tsLanguage, query.c_str(), query.length(), &errorOffset, &errorType);
    
    if (!tsQuery) {
        result.errorMessage = "Failed to create query. Error type: " + std::to_string(errorType) + 
                             ", Error offset: " + std::to_string(errorOffset);
        LOGE("%s", result.errorMessage.c_str());
        ts_tree_delete(tree);
        ts_parser_delete(parser);
        return result;
    }
    
    // 创建查询游标
    TSQueryCursor *cursor = ts_query_cursor_new();
    if (!cursor) {
        result.errorMessage = "Failed to create query cursor";
        LOGE("%s", result.errorMessage.c_str());
        ts_query_delete(tsQuery);
        ts_tree_delete(tree);
        ts_parser_delete(parser);
        return result;
    }
    
    // 执行查询
    TSNode rootNode = ts_tree_root_node(tree);
    ts_query_cursor_exec(cursor, tsQuery, rootNode);
    
    // 收集匹配结果
    TSQueryMatch match;
    while (ts_query_cursor_next_match(cursor, &match)) {
        for (uint16_t i = 0; i < match.capture_count; i++) {
            TSQueryCapture capture = match.captures[i];
            TSNode node = capture.node;
            
            // 获取捕获名称
            uint32_t nameLength;
            const char *captureName = ts_query_capture_name_for_id(tsQuery, capture.index, &nameLength);
            
            // 获取节点信息
            TSPoint startPoint = ts_node_start_point(node);
            TSPoint endPoint = ts_node_end_point(node);
            std::string nodeText = getNodeText(node, code);
            const char *nodeType = ts_node_type(node);
            
            // 创建匹配结果
            QueryMatch queryMatch;
            queryMatch.captureName = std::string(captureName, nameLength);
            queryMatch.nodeText = nodeText;
            queryMatch.nodeType = nodeType;
            queryMatch.startLine = startPoint.row;
            queryMatch.startColumn = startPoint.column;
            queryMatch.endLine = endPoint.row;
            queryMatch.endColumn = endPoint.column;
            
            result.matches.push_back(queryMatch);
            
            LOGD("Match found: capture=%s, text=%s, type=%s, line=%d, col=%d", 
                 queryMatch.captureName.c_str(), queryMatch.nodeText.c_str(), 
                 queryMatch.nodeType.c_str(), queryMatch.startLine, queryMatch.startColumn);
        }
    }
    
    result.success = true;
    LOGD("Query execution completed. Found %zu matches", result.matches.size());
    
    // 清理资源
    ts_query_cursor_delete(cursor);
    ts_query_delete(tsQuery);
    ts_tree_delete(tree);
    ts_parser_delete(parser);
    
    return result;
}