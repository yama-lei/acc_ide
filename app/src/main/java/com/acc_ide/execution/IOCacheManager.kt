package com.acc_ide.execution

/**
 * IO instance data class for caching input/output data
 * IO实例数据类 - 用于缓存输入/输出数据
 */
data class IOInstance(
    var input: String = "",
    var actualOutput: String = "",
    var expectedOutput: String = "",
    var status: String = "",
    var executionTime: Int = 0
)

/**
 * IO cache manager - Singleton
 * IO缓存管理器 - 管理每个文件的输入输出缓存
 */
object IOCacheManager {
    
    private val cache = mutableMapOf<String, IOInstance>()
    
    /**
     * Get cached IO instance for a file
     * 获取文件的缓存IO实例
     */
    fun get(fileName: String): IOInstance? {
        return cache[fileName]
    }
    
    /**
     * Save IO instance for a file
     * 保存文件的IO实例
     */
    fun save(fileName: String, instance: IOInstance) {
        cache[fileName] = instance
    }
    
    /**
     * Update IO instance for a file
     * 更新文件的IO实例
     */
    fun update(
        fileName: String,
        input: String? = null,
        actualOutput: String? = null,
        expectedOutput: String? = null,
        status: String? = null,
        executionTime: Int? = null
    ) {
        val existing = cache[fileName] ?: IOInstance()
        
        cache[fileName] = existing.copy(
            input = input ?: existing.input,
            actualOutput = actualOutput ?: existing.actualOutput,
            expectedOutput = expectedOutput ?: existing.expectedOutput,
            status = status ?: existing.status,
            executionTime = executionTime ?: existing.executionTime
        )
    }
    
    /**
     * Clear cache for a file
     * 清除文件的缓存
     */
    fun clear(fileName: String) {
        cache.remove(fileName)
    }
    
    /**
     * Clear all cache
     * 清除所有缓存
     */
    fun clearAll() {
        cache.clear()
    }
}

