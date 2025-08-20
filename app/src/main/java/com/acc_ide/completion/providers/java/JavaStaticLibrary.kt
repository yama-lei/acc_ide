package com.acc_ide.completion.providers.java

import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import com.acc_ide.completion.core.CompletionConstants
import com.acc_ide.completion.framework.UniversalCompletionEngine.PriorityCompletionItem

/**
 * Java 静态提示库
 * 
 * 包含：
 * 1. Java核心关键字
 * 2. 集合框架和API
 * 3. 竞赛常用类和方法
 * 4. IO/数学相关类
 */
class JavaStaticLibrary {

    /**
     * Java 核心关键字
     */
    private val JAVA_KEYWORDS = mapOf(
        // 基本类型关键字
        "int" to "Integer type",
        "long" to "Long integer type",
        "short" to "Short integer type",
        "byte" to "Byte type",
        "char" to "Character type",
        "boolean" to "Boolean type",
        "float" to "Floating point type",
        "double" to "Double precision floating point",
        "void" to "Void type",
        
        // 控制流关键字
        "if" to "Conditional statement",
        "else" to "Alternative branch",
        "for" to "Loop statement",
        "while" to "While loop",
        "do" to "Do-while loop",
        "switch" to "Switch statement",
        "case" to "Switch case",
        "default" to "Default case",
        "break" to "Break statement",
        "continue" to "Continue statement",
        "return" to "Return statement",
        
        // 面向对象关键字
        "class" to "Class definition",
        "interface" to "Interface definition",
        "enum" to "Enumeration",
        "extends" to "Class inheritance",
        "implements" to "Interface implementation",
        "abstract" to "Abstract modifier",
        "final" to "Final modifier",
        "static" to "Static modifier",
        "public" to "Public access modifier",
        "private" to "Private access modifier",
        "protected" to "Protected access modifier",
        "package" to "Package declaration",
        "import" to "Import statement",
        
        // 异常处理
        "try" to "Try block",
        "catch" to "Exception handler",
        "finally" to "Finally block",
        "throw" to "Throw exception",
        "throws" to "Method throws declaration",
        
        // 其他关键字
        "new" to "Object instantiation",
        "instanceof" to "Type checking",
        "super" to "Parent class reference",
        "this" to "Current object reference",
        "null" to "Null reference",
        "true" to "Boolean true value",
        "false" to "Boolean false value",
        "synchronized" to "Synchronization",
        "volatile" to "Volatile modifier",
        "transient" to "Transient modifier",
        "native" to "Native method",
        "strictfp" to "Strict floating point"
    )

    /**
     * Java 集合框架
     */
    private val JAVA_COLLECTIONS = mapOf(
        // List接口实现
        "ArrayList" to CollectionInfo("Dynamic array list", "ArrayList<T>"),
        "LinkedList" to CollectionInfo("Doubly linked list", "LinkedList<T>"),
        "Vector" to CollectionInfo("Synchronized dynamic array", "Vector<T>"),
        "Stack" to CollectionInfo("LIFO stack", "Stack<T>"),
        
        // Set接口实现
        "HashSet" to CollectionInfo("Hash set", "HashSet<T>"),
        "LinkedHashSet" to CollectionInfo("Ordered hash set", "LinkedHashSet<T>"),
        "TreeSet" to CollectionInfo("Sorted set", "TreeSet<T>"),
        
        // Map接口实现
        "HashMap" to CollectionInfo("Hash map", "HashMap<K,V>"),
        "LinkedHashMap" to CollectionInfo("Ordered hash map", "LinkedHashMap<K,V>"),
        "TreeMap" to CollectionInfo("Sorted map", "TreeMap<K,V>"),
        "Hashtable" to CollectionInfo("Synchronized hash table", "Hashtable<K,V>"),
        
        // Queue接口实现
        "PriorityQueue" to CollectionInfo("Priority queue/heap", "PriorityQueue<T>"),
        "ArrayDeque" to CollectionInfo("Array-based deque", "ArrayDeque<T>"),
        
        // 其他常用类
        "StringBuilder" to CollectionInfo("Mutable string builder", "StringBuilder"),
        "StringBuffer" to CollectionInfo("Synchronized string buffer", "StringBuffer"),
        "String" to CollectionInfo("Immutable string", "String")
    )

    /**
     * 集合成员方法
     */
    private val COLLECTION_METHODS = mapOf(
        "ArrayList" to listOf(
            MemberMethod("add", "boolean add(T e)", "Add element to end"),
            MemberMethod("get", "T get(int index)", "Get element at index"),
            MemberMethod("set", "T set(int index, T element)", "Replace element at index"),
            MemberMethod("remove", "T remove(int index)", "Remove element at index"),
            MemberMethod("size", "int size()", "Return size"),
            MemberMethod("isEmpty", "boolean isEmpty()", "Test if empty"),
            MemberMethod("clear", "void clear()", "Clear all elements"),
            MemberMethod("contains", "boolean contains(Object o)", "Test if contains element"),
            MemberMethod("indexOf", "int indexOf(Object o)", "Find first index"),
            MemberMethod("lastIndexOf", "int lastIndexOf(Object o)", "Find last index"),
            MemberMethod("toArray", "Object[] toArray()", "Convert to array")
        ),
        
        "LinkedList" to listOf(
            MemberMethod("add", "boolean add(T e)", "Add element to end"),
            MemberMethod("addFirst", "void addFirst(T e)", "Add element to beginning"),
            MemberMethod("addLast", "void addLast(T e)", "Add element to end"),
            MemberMethod("removeFirst", "T removeFirst()", "Remove first element"),
            MemberMethod("removeLast", "T removeLast()", "Remove last element"),
            MemberMethod("getFirst", "T getFirst()", "Get first element"),
            MemberMethod("getLast", "T getLast()", "Get last element"),
            MemberMethod("size", "int size()", "Return size"),
            MemberMethod("isEmpty", "boolean isEmpty()", "Test if empty"),
            MemberMethod("clear", "void clear()", "Clear all elements"),
            MemberMethod("contains", "boolean contains(Object o)", "Test if contains element")
        ),
        
        "HashSet" to listOf(
            MemberMethod("add", "boolean add(T e)", "Add element"),
            MemberMethod("remove", "boolean remove(Object o)", "Remove element"),
            MemberMethod("contains", "boolean contains(Object o)", "Test if contains element"),
            MemberMethod("size", "int size()", "Return size"),
            MemberMethod("isEmpty", "boolean isEmpty()", "Test if empty"),
            MemberMethod("clear", "void clear()", "Clear all elements"),
            MemberMethod("iterator", "Iterator<T> iterator()", "Get iterator")
        ),
        
        "HashMap" to listOf(
            MemberMethod("put", "V put(K key, V value)", "Put key-value pair"),
            MemberMethod("get", "V get(Object key)", "Get value by key"),
            MemberMethod("remove", "V remove(Object key)", "Remove by key"),
            MemberMethod("containsKey", "boolean containsKey(Object key)", "Test if contains key"),
            MemberMethod("containsValue", "boolean containsValue(Object value)", "Test if contains value"),
            MemberMethod("size", "int size()", "Return size"),
            MemberMethod("isEmpty", "boolean isEmpty()", "Test if empty"),
            MemberMethod("clear", "void clear()", "Clear all mappings"),
            MemberMethod("keySet", "Set<K> keySet()", "Get key set"),
            MemberMethod("values", "Collection<V> values()", "Get values collection"),
            MemberMethod("entrySet", "Set<Map.Entry<K,V>> entrySet()", "Get entry set")
        ),
        
        "PriorityQueue" to listOf(
            MemberMethod("add", "boolean add(T e)", "Insert element"),
            MemberMethod("offer", "boolean offer(T e)", "Insert element"),
            MemberMethod("poll", "T poll()", "Remove and return head"),
            MemberMethod("peek", "T peek()", "Return head without removing"),
            MemberMethod("remove", "boolean remove(Object o)", "Remove element"),
            MemberMethod("size", "int size()", "Return size"),
            MemberMethod("isEmpty", "boolean isEmpty()", "Test if empty"),
            MemberMethod("clear", "void clear()", "Clear all elements")
        ),
        
        "String" to listOf(
            MemberMethod("length", "int length()", "Return length"),
            MemberMethod("charAt", "char charAt(int index)", "Get character at index"),
            MemberMethod("substring", "String substring(int beginIndex)", "Get substring"),
            MemberMethod("indexOf", "int indexOf(String str)", "Find substring index"),
            MemberMethod("contains", "boolean contains(CharSequence s)", "Test if contains substring"),
            MemberMethod("startsWith", "boolean startsWith(String prefix)", "Test if starts with"),
            MemberMethod("endsWith", "boolean endsWith(String suffix)", "Test if ends with"),
            MemberMethod("replace", "String replace(char oldChar, char newChar)", "Replace characters"),
            MemberMethod("toLowerCase", "String toLowerCase()", "Convert to lowercase"),
            MemberMethod("toUpperCase", "String toUpperCase()", "Convert to uppercase"),
            MemberMethod("trim", "String trim()", "Remove leading/trailing whitespace"),
            MemberMethod("split", "String[] split(String regex)", "Split by regex"),
            MemberMethod("equals", "boolean equals(Object obj)", "Test equality"),
            MemberMethod("compareTo", "int compareTo(String str)", "Compare strings")
        ),
        
        "StringBuilder" to listOf(
            MemberMethod("append", "StringBuilder append(String str)", "Append string"),
            MemberMethod("insert", "StringBuilder insert(int offset, String str)", "Insert string"),
            MemberMethod("delete", "StringBuilder delete(int start, int end)", "Delete characters"),
            MemberMethod("deleteCharAt", "StringBuilder deleteCharAt(int index)", "Delete character"),
            MemberMethod("replace", "StringBuilder replace(int start, int end, String str)", "Replace range"),
            MemberMethod("reverse", "StringBuilder reverse()", "Reverse characters"),
            MemberMethod("toString", "String toString()", "Convert to string"),
            MemberMethod("length", "int length()", "Return length"),
            MemberMethod("setLength", "void setLength(int newLength)", "Set length"),
            MemberMethod("charAt", "char charAt(int index)", "Get character at index")
        )
    )

    /**
     * Java 常用工具类
     */
    private val UTILITY_CLASSES = mapOf(
        // 数学类
        "Math" to listOf(
            UtilityMethod("abs", "int abs(int a)", "Absolute value"),
            UtilityMethod("max", "int max(int a, int b)", "Maximum value"),
            UtilityMethod("min", "int min(int a, int b)", "Minimum value"),
            UtilityMethod("pow", "double pow(double a, double b)", "Power function"),
            UtilityMethod("sqrt", "double sqrt(double a)", "Square root"),
            UtilityMethod("ceil", "double ceil(double a)", "Round up"),
            UtilityMethod("floor", "double floor(double a)", "Round down"),
            UtilityMethod("round", "long round(double a)", "Round to nearest"),
            UtilityMethod("random", "double random()", "Random number [0,1)")
        ),
        
        // 数组工具类
        "Arrays" to listOf(
            UtilityMethod("sort", "void sort(int[] a)", "Sort array"),
            UtilityMethod("binarySearch", "int binarySearch(int[] a, int key)", "Binary search"),
            UtilityMethod("equals", "boolean equals(int[] a, int[] a2)", "Test equality"),
            UtilityMethod("fill", "void fill(int[] a, int val)", "Fill with value"),
            UtilityMethod("copyOf", "int[] copyOf(int[] original, int newLength)", "Copy array"),
            UtilityMethod("toString", "String toString(int[] a)", "Convert to string")
        ),
        
        // 集合工具类
        "Collections" to listOf(
            UtilityMethod("sort", "void sort(List<T> list)", "Sort list"),
            UtilityMethod("reverse", "void reverse(List<?> list)", "Reverse list"),
            UtilityMethod("shuffle", "void shuffle(List<?> list)", "Shuffle list"),
            UtilityMethod("max", "T max(Collection<T> coll)", "Find maximum"),
            UtilityMethod("min", "T min(Collection<T> coll)", "Find minimum"),
            UtilityMethod("binarySearch", "int binarySearch(List<T> list, T key)", "Binary search"),
            UtilityMethod("fill", "void fill(List<T> list, T obj)", "Fill with object")
        ),
        
        // IO类
        "Scanner" to listOf(
            UtilityMethod("nextInt", "int nextInt()", "Read next integer"),
            UtilityMethod("nextLong", "long nextLong()", "Read next long"),
            UtilityMethod("nextDouble", "double nextDouble()", "Read next double"),
            UtilityMethod("next", "String next()", "Read next token"),
            UtilityMethod("nextLine", "String nextLine()", "Read next line"),
            UtilityMethod("hasNext", "boolean hasNext()", "Test if has next token"),
            UtilityMethod("hasNextInt", "boolean hasNextInt()", "Test if has next int"),
            UtilityMethod("close", "void close()", "Close scanner")
        )
    )

    /**
     * 竞赛常用常量
     */
    private val COMPETITIVE_CONSTANTS = mapOf(
        "Integer.MAX_VALUE" to "Maximum int value (2147483647)",
        "Integer.MIN_VALUE" to "Minimum int value (-2147483648)",
        "Long.MAX_VALUE" to "Maximum long value",
        "Long.MIN_VALUE" to "Minimum long value",
        "Double.MAX_VALUE" to "Maximum double value",
        "Double.MIN_VALUE" to "Minimum positive double value",
        "Math.PI" to "Pi constant (3.141592653589793)",
        "Math.E" to "Euler's number (2.718281828459045)"
    )

    // 数据类定义
    data class CollectionInfo(val description: String, val signature: String)
    data class MemberMethod(val name: String, val signature: String, val description: String)
    data class UtilityMethod(val name: String, val signature: String, val description: String)

    /**
     * 获取关键字补全
     */
    fun getKeywordCompletions(prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        JAVA_KEYWORDS.forEach { (keyword, desc) ->
            if (keyword.startsWith(lowerPrefix)) {
                items.add(PriorityCompletionItem(
                    keyword,
                    desc,
                    prefix.length,
                    keyword,
                    CompletionConstants.PRIORITY_KEYWORD,
                    CompletionItemKind.Keyword
                ))
            }
        }
        
        return items
    }

    /**
     * 获取集合类补全
     */
    fun getCollectionCompletions(prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        JAVA_COLLECTIONS.forEach { (collection, info) ->
            if (collection.lowercase().startsWith(lowerPrefix)) {
                items.add(PriorityCompletionItem(
                    collection,
                    "${info.signature} - ${info.description}",
                    prefix.length,
                    collection,
                    CompletionConstants.PRIORITY_STL_CONTAINER,
                    CompletionItemKind.Class
                ))
            }
        }
        
        return items
    }

    /**
     * 获取工具类补全
     */
    fun getUtilityCompletions(prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        UTILITY_CLASSES.keys.forEach { className ->
            if (className.lowercase().startsWith(lowerPrefix)) {
                items.add(PriorityCompletionItem(
                    className,
                    "Utility class - $className",
                    prefix.length,
                    className,
                    CompletionConstants.PRIORITY_STL_ALGORITHM,
                    CompletionItemKind.Class
                ))
            }
        }
        
        return items
    }

    /**
     * 获取常量补全
     */
    fun getConstantCompletions(prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        COMPETITIVE_CONSTANTS.forEach { (constant, desc) ->
            if (constant.lowercase().startsWith(lowerPrefix)) {
                items.add(PriorityCompletionItem(
                    constant,
                    desc,
                    prefix.length,
                    constant,
                    CompletionConstants.PRIORITY_CONSTANT,
                    CompletionItemKind.Constant
                ))
            }
        }
        
        return items
    }

    /**
     * 获取集合成员方法补全
     */
    fun getCollectionMemberCompletions(collectionType: String, prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        // 清理类型名（移除泛型参数）
        val cleanType = cleanCollectionType(collectionType)
        val methods = COLLECTION_METHODS[cleanType] ?: return items
        
        methods.forEach { method ->
            if (method.name.startsWith(lowerPrefix)) {
                items.add(PriorityCompletionItem(
                    method.name,
                    "${method.signature} - ${method.description}",
                    prefix.length,
                    method.name,
                    CompletionConstants.PRIORITY_STRUCT_MEMBER,
                    CompletionItemKind.Method
                ))
            }
        }
        
        return items
    }

    /**
     * 获取工具类静态方法补全
     */
    fun getUtilityMemberCompletions(className: String, prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        val methods = UTILITY_CLASSES[className] ?: return items
        
        methods.forEach { method ->
            if (method.name.startsWith(lowerPrefix)) {
                items.add(PriorityCompletionItem(
                    method.name,
                    "${method.signature} - ${method.description}",
                    prefix.length,
                    method.name,
                    CompletionConstants.PRIORITY_STRUCT_MEMBER,
                    CompletionItemKind.Method
                ))
            }
        }
        
        return items
    }

    /**
     * 清理集合类型名，移除泛型参数
     */
    private fun cleanCollectionType(collectionType: String): String {
        return collectionType
            .replace(Regex("<.*>"), "") // 移除泛型参数
            .replace("*", "")            // 移除指针标记
            .replace("&", "")            // 移除引用标记
            .replace("final", "")        // 移除final修饰符
            .trim()
    }

    /**
     * 检查是否为Java集合类型
     */
    fun isJavaCollection(dataType: String): Boolean {
        val cleanType = cleanCollectionType(dataType)
        return JAVA_COLLECTIONS.containsKey(cleanType)
    }

    /**
     * 检查是否为Java工具类
     */
    fun isJavaUtilityClass(dataType: String): Boolean {
        return UTILITY_CLASSES.containsKey(dataType)
    }

    /**
     * 获取所有补全（用于常规补全）
     */
    fun getAllCompletions(prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        
        items.addAll(getKeywordCompletions(prefix))
        items.addAll(getCollectionCompletions(prefix))
        items.addAll(getUtilityCompletions(prefix))
        items.addAll(getConstantCompletions(prefix))
        
        return items
    }
}