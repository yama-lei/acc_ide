    /**
     * 使用标准Tree-sitter locals查询解析代码
     * 完全基于官方Tree-sitter设计理念，不再重复造轮子
     */
    fun parseCodeWithQueries(contentRef: ContentReference, language: String): ParseResult? {
        val content = contentRef.reference
        val text = content.toString()
        
        Log.d(TAG, "parseCodeWithQueries called for language: $language, text length: ${text.length}")
        Log.d(TAG, "Using ONLY standard Tree-sitter locals query - no fallback to old systems")
        
        return try {
            // 直接使用标准Tree-sitter locals查询 - 唯一的解析方法
            val result = parseCode(contentRef, language)
            
            if (result != null) {
                Log.d(TAG, "Standard Tree-sitter locals query result: ${result.symbols.size} symbols, ${result.scopes.size} scopes")
                result
            } else {
                Log.w(TAG, "Standard Tree-sitter query returned null")
                createEmptyParseResult()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse $language code with standard Tree-sitter", e)
            createEmptyParseResult()
        }
    }