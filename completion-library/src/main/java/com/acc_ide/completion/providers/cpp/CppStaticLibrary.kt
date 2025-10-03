package com.acc_ide.completion.providers.cpp

import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import com.acc_ide.completion.core.CompletionConstants
import com.acc_ide.completion.framework.UniversalCompletionEngine.PriorityCompletionItem

/**
 * C++ 静态提示库
 * 
 * 包含：
 * 1. C++核心关键字
 * 2. STL容器和算法
 * 3. 竞赛常用函数
 * 4. 数学/字符串/IO相关函数
 */
class CppStaticLibrary {

    /**
     * C++ 核心关键字
     */
    private val CPP_KEYWORDS = mapOf(
        // 基本类型关键字
        "int" to "Integer type",
        "long" to "Long integer type", 
        "long long" to "64-bit integer type",
        "short" to "Short integer type",
        "char" to "Character type",
        "bool" to "Boolean type",
        "float" to "Floating point type",
        "double" to "Double precision floating point",
        "void" to "Void type",
        "auto" to "Automatic type deduction",
        "signed" to "Signed type modifier",
        "unsigned" to "Unsigned type modifier",
        
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
        "goto" to "Goto statement",
        
        // 函数和类关键字
        "struct" to "Structure definition",
        "class" to "Class definition", 
        "enum" to "Enumeration",
        "union" to "Union type",
        "typedef" to "Type definition",
        "using" to "Using declaration",
        "namespace" to "Namespace declaration",
        "template" to "Template declaration",
        "typename" to "Type name",
        "public" to "Public access specifier",
        "private" to "Private access specifier", 
        "protected" to "Protected access specifier",
        "virtual" to "Virtual function",
        "override" to "Override specifier",
        "static" to "Static storage class",
        "extern" to "External linkage",
        "inline" to "Inline function",
        "friend" to "Friend declaration",
        "operator" to "Operator overloading",
        
        // 内存管理
        "new" to "Dynamic allocation",
        "delete" to "Dynamic deallocation", 
        "malloc" to "Memory allocation",
        "free" to "Memory deallocation",
        "sizeof" to "Size operator",
        
        // 异常处理
        "try" to "Try block",
        "catch" to "Exception handler",
        "throw" to "Throw exception",
        "noexcept" to "No exception specifier",
        
        // 常量和修饰符
        "const" to "Constant modifier",
        "volatile" to "Volatile modifier", 
        "mutable" to "Mutable modifier",
        "constexpr" to "Constant expression",
        "register" to "Register storage class",
        
        // 竞赛常用宏
        "define" to "Preprocessor definition",
        "include" to "Include header",
        "pragma" to "Compiler directive",
        "ifdef" to "Conditional compilation",
        "ifndef" to "Conditional compilation",
        "endif" to "End conditional",
        
        // 逻辑运算符作为关键字
        "and" to "Logical AND operator",
        "or" to "Logical OR operator", 
        "not" to "Logical NOT operator",
        "true" to "Boolean true value",
        "false" to "Boolean false value",
        "nullptr" to "Null pointer literal"
    )

    /**
     * STL 容器定义
     */
    private val STL_CONTAINERS = mapOf(
        // 序列容器
        "vector" to ContainerInfo("Dynamic array", "vector<T>"),
        "string" to ContainerInfo("String class", "string"),
        "deque" to ContainerInfo("Double-ended queue", "deque<T>"),
        "list" to ContainerInfo("Doubly linked list", "list<T>"),
        "array" to ContainerInfo("Fixed-size array", "array<T,N>"),
        
        // 关联容器
        "set" to ContainerInfo("Ordered set", "set<T>"),
        "multiset" to ContainerInfo("Ordered multiset", "multiset<T>"),
        "map" to ContainerInfo("Ordered map", "map<K,V>"),
        "multimap" to ContainerInfo("Ordered multimap", "multimap<K,V>"),
        
        // 无序关联容器
        "unordered_set" to ContainerInfo("Hash set", "unordered_set<T>"),
        "unordered_multiset" to ContainerInfo("Hash multiset", "unordered_multiset<T>"),
        "unordered_map" to ContainerInfo("Hash map", "unordered_map<K,V>"),
        "unordered_multimap" to ContainerInfo("Hash multimap", "unordered_multimap<K,V>"),
        
        // 容器适配器  
        "stack" to ContainerInfo("LIFO stack", "stack<T,Ctr>"),
        "queue" to ContainerInfo("FIFO queue", "queue<T,Ctr>"),
        "priority_queue" to ContainerInfo("Priority queue/heap", "priority_queue<T,Ctr,Cmp>"),
        
        // 其他
        "pair" to ContainerInfo("Pair of values", "pair<T1,T2>"),
        "tuple" to ContainerInfo("Tuple of values", "tuple<T...>")
    )

    /**
     * STL 容器成员函数
     */
    private val STL_MEMBER_FUNCTIONS = mapOf(
        "vector" to listOf(
            MemberFunction("push_back", "void push_back(const T& value)", "Add element to end"),
            MemberFunction("pop_back", "void pop_back()", "Remove last element"),
            MemberFunction("size", "size_t size() const", "Return size"),
            MemberFunction("empty", "bool empty() const", "Test if empty"),
            MemberFunction("clear", "void clear()", "Clear all elements"),
            MemberFunction("begin", "iterator begin()", "Return iterator to beginning"),
            MemberFunction("end", "iterator end()", "Return iterator to end"),
            MemberFunction("front", "T& front()", "Access first element"),
            MemberFunction("back", "T& back()", "Access last element"),
            MemberFunction("at", "T& at(size_t pos)", "Access element at position"),
            MemberFunction("resize", "void resize(size_t n)", "Change size"),
            MemberFunction("reserve", "void reserve(size_t n)", "Reserve capacity"),
            MemberFunction("capacity", "size_t capacity() const", "Return capacity"),
            MemberFunction("insert", "iterator insert(iterator pos, const T& val)", "Insert element"),
            MemberFunction("erase", "iterator erase(iterator pos)", "Erase element")
        ),
        
        "string" to listOf(
            MemberFunction("length", "size_t length() const", "Return length"),
            MemberFunction("size", "size_t size() const", "Return size"),
            MemberFunction("empty", "bool empty() const", "Test if empty"),
            MemberFunction("clear", "void clear()", "Clear string"),
            MemberFunction("substr", "string substr(size_t pos, size_t len) const", "Generate substring"),
            MemberFunction("find", "size_t find(const string& str) const", "Find substring"),
            MemberFunction("replace", "string& replace(size_t pos, size_t len, const string& str)", "Replace portion"),
            MemberFunction("append", "string& append(const string& str)", "Append to string"),
            MemberFunction("push_back", "void push_back(char c)", "Append character"),
            MemberFunction("pop_back", "void pop_back()", "Delete last character"),
            MemberFunction("c_str", "const char* c_str() const", "Get C string"),
            MemberFunction("begin", "iterator begin()", "Return iterator to beginning"),
            MemberFunction("end", "iterator end()", "Return iterator to end"),
            MemberFunction("front", "char& front()", "Access first character"),
            MemberFunction("back", "char& back()", "Access last character")
        ),
        
        "set" to listOf(
            MemberFunction("insert", "pair<iterator,bool> insert(const T& val)", "Insert element"),
            MemberFunction("erase", "size_t erase(const T& val)", "Erase element"),
            MemberFunction("find", "iterator find(const T& val)", "Find element"),
            MemberFunction("count", "size_t count(const T& val) const", "Count elements"),
            MemberFunction("size", "size_t size() const", "Return size"),
            MemberFunction("empty", "bool empty() const", "Test if empty"),
            MemberFunction("clear", "void clear()", "Clear all elements"),
            MemberFunction("begin", "iterator begin()", "Return iterator to beginning"),
            MemberFunction("end", "iterator end()", "Return iterator to end"),
            MemberFunction("lower_bound", "iterator lower_bound(const T& val)", "Lower bound"),
            MemberFunction("upper_bound", "iterator upper_bound(const T& val)", "Upper bound"),
            MemberFunction("equal_range", "pair<iterator,iterator> equal_range(const T& val)", "Equal range")
        ),
        
        "multiset" to listOf(
            MemberFunction("insert", "iterator insert(const T& val)", "Insert element"),
            MemberFunction("erase", "size_t erase(const T& val)", "Erase all matching elements"),
            MemberFunction("find", "iterator find(const T& val)", "Find element"),
            MemberFunction("count", "size_t count(const T& val) const", "Count elements"),
            MemberFunction("size", "size_t size() const", "Return size"),
            MemberFunction("empty", "bool empty() const", "Test if empty"),
            MemberFunction("clear", "void clear()", "Clear all elements"),
            MemberFunction("begin", "iterator begin()", "Return iterator to beginning"),
            MemberFunction("end", "iterator end()", "Return iterator to end"),
            MemberFunction("lower_bound", "iterator lower_bound(const T& val)", "Lower bound"),
            MemberFunction("upper_bound", "iterator upper_bound(const T& val)", "Upper bound"),
            MemberFunction("equal_range", "pair<iterator,iterator> equal_range(const T& val)", "Equal range")
        ),
        
        "map" to listOf(
            MemberFunction("insert", "pair<iterator,bool> insert(const pair<K,V>& val)", "Insert element"),
            MemberFunction("erase", "size_t erase(const K& key)", "Erase element"),
            MemberFunction("find", "iterator find(const K& key)", "Find element"),
            MemberFunction("count", "size_t count(const K& key) const", "Count elements"),
            MemberFunction("size", "size_t size() const", "Return size"),
            MemberFunction("empty", "bool empty() const", "Test if empty"),
            MemberFunction("clear", "void clear()", "Clear all elements"),
            MemberFunction("begin", "iterator begin()", "Return iterator to beginning"),
            MemberFunction("end", "iterator end()", "Return iterator to end"),
            MemberFunction("lower_bound", "iterator lower_bound(const K& key)", "Lower bound"),
            MemberFunction("upper_bound", "iterator upper_bound(const K& key)", "Upper bound"),
            MemberFunction("at", "V& at(const K& key)", "Access element")
        ),
        
        "multimap" to listOf(
            MemberFunction("insert", "iterator insert(const pair<K,V>& val)", "Insert element"),
            MemberFunction("erase", "size_t erase(const K& key)", "Erase all matching elements"),
            MemberFunction("find", "iterator find(const K& key)", "Find element"),
            MemberFunction("count", "size_t count(const K& key) const", "Count elements"),
            MemberFunction("size", "size_t size() const", "Return size"),
            MemberFunction("empty", "bool empty() const", "Test if empty"),
            MemberFunction("clear", "void clear()", "Clear all elements"),
            MemberFunction("begin", "iterator begin()", "Return iterator to beginning"),
            MemberFunction("end", "iterator end()", "Return iterator to end"),
            MemberFunction("lower_bound", "iterator lower_bound(const K& key)", "Lower bound"),
            MemberFunction("upper_bound", "iterator upper_bound(const K& key)", "Upper bound"),
            MemberFunction("equal_range", "pair<iterator,iterator> equal_range(const K& key)", "Equal range")
        ),
        
        "unordered_set" to listOf(
            MemberFunction("insert", "pair<iterator,bool> insert(const T& val)", "Insert element"),
            MemberFunction("erase", "size_t erase(const T& val)", "Erase element"),
            MemberFunction("find", "iterator find(const T& val)", "Find element"),
            MemberFunction("count", "size_t count(const T& val) const", "Count elements"),
            MemberFunction("size", "size_t size() const", "Return size"),
            MemberFunction("empty", "bool empty() const", "Test if empty"),
            MemberFunction("clear", "void clear()", "Clear all elements"),
            MemberFunction("begin", "iterator begin()", "Return iterator to beginning"),
            MemberFunction("end", "iterator end()", "Return iterator to end"),
            MemberFunction("bucket_count", "size_t bucket_count() const", "Number of buckets"),
            MemberFunction("load_factor", "float load_factor() const", "Load factor"),
            MemberFunction("rehash", "void rehash(size_t n)", "Rehash container")
        ),
        
        "unordered_map" to listOf(
            MemberFunction("insert", "pair<iterator,bool> insert(const pair<K,V>& val)", "Insert element"),
            MemberFunction("erase", "size_t erase(const K& key)", "Erase element"),
            MemberFunction("find", "iterator find(const K& key)", "Find element"),
            MemberFunction("count", "size_t count(const K& key) const", "Count elements"),
            MemberFunction("size", "size_t size() const", "Return size"),
            MemberFunction("empty", "bool empty() const", "Test if empty"),
            MemberFunction("clear", "void clear()", "Clear all elements"),
            MemberFunction("begin", "iterator begin()", "Return iterator to beginning"),
            MemberFunction("end", "iterator end()", "Return iterator to end"),
            MemberFunction("at", "V& at(const K& key)", "Access element"),
            MemberFunction("bucket_count", "size_t bucket_count() const", "Number of buckets"),
            MemberFunction("load_factor", "float load_factor() const", "Load factor"),
            MemberFunction("rehash", "void rehash(size_t n)", "Rehash container")
        ),
        
        "deque" to listOf(
            MemberFunction("push_back", "void push_back(const T& val)", "Add element to end"),
            MemberFunction("pop_back", "void pop_back()", "Remove last element"),
            MemberFunction("push_front", "void push_front(const T& val)", "Add element to beginning"),
            MemberFunction("pop_front", "void pop_front()", "Remove first element"),
            MemberFunction("size", "size_t size() const", "Return size"),
            MemberFunction("empty", "bool empty() const", "Test if empty"),
            MemberFunction("clear", "void clear()", "Clear all elements"),
            MemberFunction("begin", "iterator begin()", "Return iterator to beginning"),
            MemberFunction("end", "iterator end()", "Return iterator to end"),
            MemberFunction("front", "T& front()", "Access first element"),
            MemberFunction("back", "T& back()", "Access last element"),
            MemberFunction("at", "T& at(size_t pos)", "Access element at position")
        ),
        
        "list" to listOf(
            MemberFunction("push_back", "void push_back(const T& val)", "Add element to end"),
            MemberFunction("pop_back", "void pop_back()", "Remove last element"),
            MemberFunction("push_front", "void push_front(const T& val)", "Add element to beginning"),
            MemberFunction("pop_front", "void pop_front()", "Remove first element"),
            MemberFunction("size", "size_t size() const", "Return size"),
            MemberFunction("empty", "bool empty() const", "Test if empty"),
            MemberFunction("clear", "void clear()", "Clear all elements"),
            MemberFunction("begin", "iterator begin()", "Return iterator to beginning"),
            MemberFunction("end", "iterator end()", "Return iterator to end"),
            MemberFunction("front", "T& front()", "Access first element"),
            MemberFunction("back", "T& back()", "Access last element"),
            MemberFunction("sort", "void sort()", "Sort elements"),
            MemberFunction("reverse", "void reverse()", "Reverse order"),
            MemberFunction("unique", "void unique()", "Remove consecutive duplicates")
        ),
        
        "stack" to listOf(
            MemberFunction("push", "void push(const T& val)", "Insert element"),
            MemberFunction("pop", "void pop()", "Remove top element"),
            MemberFunction("top", "T& top()", "Access top element"),
            MemberFunction("size", "size_t size() const", "Return size"),
            MemberFunction("empty", "bool empty() const", "Test if empty")
        ),
        
        "queue" to listOf(
            MemberFunction("push", "void push(const T& val)", "Insert element"),
            MemberFunction("pop", "void pop()", "Remove next element"),
            MemberFunction("front", "T& front()", "Access next element"),
            MemberFunction("back", "T& back()", "Access last element"),
            MemberFunction("size", "size_t size() const", "Return size"),
            MemberFunction("empty", "bool empty() const", "Test if empty")
        ),
        
        "priority_queue" to listOf(
            MemberFunction("push", "void push(const T& val)", "Insert element"),
            MemberFunction("pop", "void pop()", "Remove top element"),
            MemberFunction("top", "const T& top() const", "Access top element"),
            MemberFunction("size", "size_t size() const", "Return size"),
            MemberFunction("empty", "bool empty() const", "Test if empty")
        ),
        
        "pair" to listOf(
            MemberFunction("first", "T1 first", "First element"),
            MemberFunction("second", "T2 second", "Second element"),
            MemberFunction("make_pair", "pair<T1,T2> make_pair(T1 x, T2 y)", "Construct pair")
        )
    )

    /**
     * STL 算法函数
     */
    private val STL_ALGORITHMS = mapOf(
        // 排序算法
        "sort" to AlgorithmFunction("void sort(RandomIt first, RandomIt last)", "Sort elements in range"),
        "stable_sort" to AlgorithmFunction("void stable_sort(RandomIt first, RandomIt last)", "Sort maintaining relative order"),
        "partial_sort" to AlgorithmFunction("void partial_sort(RandomIt first, RandomIt middle, RandomIt last)", "Partially sort range"),
        "nth_element" to AlgorithmFunction("void nth_element(RandomIt first, RandomIt nth, RandomIt last)", "Sort nth element"),
        
        // 搜索算法
        "find" to AlgorithmFunction("InputIt find(InputIt first, InputIt last, const T& val)", "Find value in range"),
        "find_if" to AlgorithmFunction("InputIt find_if(InputIt first, InputIt last, UnaryPredicate p)", "Find element satisfying condition"),
        "binary_search" to AlgorithmFunction("bool binary_search(ForwardIt first, ForwardIt last, const T& val)", "Test if value exists in sorted range"),
        "lower_bound" to AlgorithmFunction("ForwardIt lower_bound(ForwardIt first, ForwardIt last, const T& val)", "Return iterator to lower bound"),
        "upper_bound" to AlgorithmFunction("ForwardIt upper_bound(ForwardIt first, ForwardIt last, const T& val)", "Return iterator to upper bound"),
        "equal_range" to AlgorithmFunction("pair<ForwardIt,ForwardIt> equal_range(ForwardIt first, ForwardIt last, const T& val)", "Return range of equal elements"),
        
        // 修改算法
        "reverse" to AlgorithmFunction("void reverse(BidirIt first, BidirIt last)", "Reverse range"),
        "rotate" to AlgorithmFunction("ForwardIt rotate(ForwardIt first, ForwardIt middle, ForwardIt last)", "Rotate range"),
        "random_shuffle" to AlgorithmFunction("void random_shuffle(RandomIt first, RandomIt last)", "Randomly shuffle range"),
        "unique" to AlgorithmFunction("ForwardIt unique(ForwardIt first, ForwardIt last)", "Remove consecutive duplicates"),
        "remove" to AlgorithmFunction("ForwardIt remove(ForwardIt first, ForwardIt last, const T& val)", "Remove value from range"),
        "remove_if" to AlgorithmFunction("ForwardIt remove_if(ForwardIt first, ForwardIt last, UnaryPredicate p)", "Remove elements satisfying condition"),
        
        // 数值算法
        "accumulate" to AlgorithmFunction("T accumulate(InputIt first, InputIt last, T init)", "Accumulate values in range"),
        "inner_product" to AlgorithmFunction("T inner_product(InputIt1 first1, InputIt1 last1, InputIt2 first2, T init)", "Compute inner product"),
        "partial_sum" to AlgorithmFunction("OutputIt partial_sum(InputIt first, InputIt last, OutputIt d_first)", "Compute partial sums"),
        
        // 集合算法
        "set_union" to AlgorithmFunction("OutputIt set_union(InputIt1 first1, InputIt1 last1, InputIt2 first2, InputIt2 last2, OutputIt d_first)", "Compute set union"),
        "set_intersection" to AlgorithmFunction("OutputIt set_intersection(InputIt1 first1, InputIt1 last1, InputIt2 first2, InputIt2 last2, OutputIt d_first)", "Compute set intersection"),
        "set_difference" to AlgorithmFunction("OutputIt set_difference(InputIt1 first1, InputIt1 last1, InputIt2 first2, InputIt2 last2, OutputIt d_first)", "Compute set difference"),
        
        // 堆算法
        "make_heap" to AlgorithmFunction("void make_heap(RandomIt first, RandomIt last)", "Make heap from range"),
        "push_heap" to AlgorithmFunction("void push_heap(RandomIt first, RandomIt last)", "Push element into heap"),
        "pop_heap" to AlgorithmFunction("void pop_heap(RandomIt first, RandomIt last)", "Pop element from heap"),
        "sort_heap" to AlgorithmFunction("void sort_heap(RandomIt first, RandomIt last)", "Sort heap"),
        
        // 最值算法
        "min" to AlgorithmFunction("const T& min(const T& a, const T& b)", "Return smaller value"),
        "max" to AlgorithmFunction("const T& max(const T& a, const T& b)", "Return larger value"),
        "min_element" to AlgorithmFunction("ForwardIt min_element(ForwardIt first, ForwardIt last)", "Return iterator to minimum element"),
        "max_element" to AlgorithmFunction("ForwardIt max_element(ForwardIt first, ForwardIt last)", "Return iterator to maximum element"),
        "minmax_element" to AlgorithmFunction("pair<ForwardIt,ForwardIt> minmax_element(ForwardIt first, ForwardIt last)", "Return iterators to min and max elements"),
        
        // 排列算法
        "next_permutation" to AlgorithmFunction("bool next_permutation(BidirIt first, BidirIt last)", "Generate next permutation"),
        "prev_permutation" to AlgorithmFunction("bool prev_permutation(BidirIt first, BidirIt last)", "Generate previous permutation")
    )

    /**
     * 竞赛常用数学函数
     */
    private val MATH_FUNCTIONS = mapOf(
        // 基本数学函数
        "abs" to MathFunction("T abs(T n)", "Absolute value"),
        "fabs" to MathFunction("double fabs(double x)", "Floating-point absolute value"),
        "sqrt" to MathFunction("double sqrt(double x)", "Square root"),
        "pow" to MathFunction("double pow(double base, double exp)", "Power function"),
        "exp" to MathFunction("double exp(double x)", "Exponential function"),
        "log" to MathFunction("double log(double x)", "Natural logarithm"),
        "log10" to MathFunction("double log10(double x)", "Base-10 logarithm"),
        "log2" to MathFunction("double log2(double x)", "Base-2 logarithm"),
        
        // 三角函数
        "sin" to MathFunction("double sin(double x)", "Sine"),
        "cos" to MathFunction("double cos(double x)", "Cosine"), 
        "tan" to MathFunction("double tan(double x)", "Tangent"),
        "asin" to MathFunction("double asin(double x)", "Arc sine"),
        "acos" to MathFunction("double acos(double x)", "Arc cosine"),
        "atan" to MathFunction("double atan(double x)", "Arc tangent"),
        "atan2" to MathFunction("double atan2(double y, double x)", "Arc tangent of y/x"),
        
        // 舍入函数
        "ceil" to MathFunction("double ceil(double x)", "Round up"),
        "floor" to MathFunction("double floor(double x)", "Round down"),
        "round" to MathFunction("double round(double x)", "Round to nearest"),
        "trunc" to MathFunction("double trunc(double x)", "Truncate"),
        
        // 其他数学函数
        "fmod" to MathFunction("double fmod(double x, double y)", "Floating-point remainder"),
        "gcd" to MathFunction("T gcd(T a, T b)", "Greatest common divisor"),
        "lcm" to MathFunction("T lcm(T a, T b)", "Least common multiple"),
        "hypot" to MathFunction("double hypot(double x, double y)", "Hypotenuse"),
        
        // 随机数
        "rand" to MathFunction("int rand()", "Generate random number"),
        "srand" to MathFunction("void srand(unsigned seed)", "Seed random generator"),
        "random_shuffle" to MathFunction("void random_shuffle(RandomIt first, RandomIt last)", "Randomly shuffle range")
    )

    /**
     * IO 函数
     */
    private val IO_FUNCTIONS = mapOf(
        // C-style IO
        "printf" to IOFunction("int printf(const char* format, ...)", "Formatted output"),
        "scanf" to IOFunction("int scanf(const char* format, ...)", "Formatted input"),
        "puts" to IOFunction("int puts(const char* str)", "Write string to stdout"),
        // "gets" - REMOVED: Deprecated in C11, unsafe (buffer overflow risk). Use fgets() instead.
        "fgets" to IOFunction("char* fgets(char* str, int n, FILE* stream)", "Read string safely from stream"),
        "getchar" to IOFunction("int getchar()", "Read character"),
        "putchar" to IOFunction("int putchar(int ch)", "Write character"),
        
        // C++ style IO
        "cin" to IOFunction("istream cin", "Standard input stream"),
        "cout" to IOFunction("ostream cout", "Standard output stream"),
        "cerr" to IOFunction("ostream cerr", "Standard error stream"),
        "endl" to IOFunction("ostream& endl(ostream& os)", "End line and flush"),
        
        // File IO
        "fopen" to IOFunction("FILE* fopen(const char* filename, const char* mode)", "Open file"),
        "fclose" to IOFunction("int fclose(FILE* stream)", "Close file"),
        "fread" to IOFunction("size_t fread(void* ptr, size_t size, size_t count, FILE* stream)", "Read from file"),
        "fwrite" to IOFunction("size_t fwrite(const void* ptr, size_t size, size_t count, FILE* stream)", "Write to file"),
        "fprintf" to IOFunction("int fprintf(FILE* stream, const char* format, ...)", "Formatted file output"),
        "fscanf" to IOFunction("int fscanf(FILE* stream, const char* format, ...)", "Formatted file input")
    )

    /**
     * 竞赛常用宏和常量
     */
    private val COMPETITIVE_MACROS = mapOf(
        "INT_MAX" to "Maximum int value (2147483647)",
        "INT_MIN" to "Minimum int value (-2147483648)",
        "LLONG_MAX" to "Maximum long long value",
        "LLONG_MIN" to "Minimum long long value",
        "DBL_MAX" to "Maximum double value",
        "DBL_MIN" to "Minimum positive double value",
        "PI" to "Pi constant (3.14159265358979323846)",
        "EPS" to "Small epsilon value for floating comparison",
        "MOD" to "Common modulo value (1e9+7)",
        "INF" to "Infinity representation",
        "MAXN" to "Maximum array size",
        
        // 常用宏定义
        "ll" to "typedef long long ll",
        "ull" to "typedef unsigned long long ull",
        "pii" to "typedef pair<int,int> pii",
        "pll" to "typedef pair<long long, long long> pll",
        "vi" to "typedef vector<int> vi",
        "vll" to "typedef vector<long long> vll",
        "vs" to "typedef vector<string> vs",
        "vvi" to "typedef vector<vector<int>> vvi",
        "pb" to "#define pb push_back",
        "mp" to "#define mp make_pair",
        "fi" to "#define fi first",
        "se" to "#define se second",
        "sz" to "#define sz(x) ((int)(x).size())",
        "all" to "#define all(x) (x).begin(), (x).end()",
        "rall" to "#define rall(x) (x).rbegin(), (x).rend()"
    )

    // 数据类定义
    data class ContainerInfo(val description: String, val signature: String)
    data class MemberFunction(val name: String, val signature: String, val description: String)
    data class AlgorithmFunction(val signature: String, val description: String)
    data class MathFunction(val signature: String, val description: String)
    data class IOFunction(val signature: String, val description: String)

    /**
     * 获取关键字补全
     */
    fun getKeywordCompletions(prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        // C++ 关键字
        CPP_KEYWORDS.forEach { (keyword, desc) ->
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
     * 获取容器补全
     */
    fun getContainerCompletions(prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        STL_CONTAINERS.forEach { (container, info) ->
            if (container.startsWith(lowerPrefix)) {
                items.add(PriorityCompletionItem(
                    container,
                    "${info.signature} - ${info.description}",
                    prefix.length,
                    container,
                    CompletionConstants.PRIORITY_STL_CONTAINER,
                    CompletionItemKind.Class
                ))
            }
        }
        
        return items
    }

    /**
     * 获取算法函数补全
     */
    fun getAlgorithmCompletions(prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        STL_ALGORITHMS.forEach { (algorithm, func) ->
            if (algorithm.startsWith(lowerPrefix)) {
                items.add(PriorityCompletionItem(
                    algorithm,
                    "${func.signature} - ${func.description}",
                    prefix.length,
                    algorithm,
                    CompletionConstants.PRIORITY_STL_ALGORITHM,
                    CompletionItemKind.Function
                ))
            }
        }
        
        return items
    }

    /**
     * 获取数学函数补全
     */
    fun getMathCompletions(prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        MATH_FUNCTIONS.forEach { (function, func) ->
            if (function.startsWith(lowerPrefix)) {
                items.add(PriorityCompletionItem(
                    function,
                    "${func.signature} - ${func.description}",
                    prefix.length,
                    function,
                    CompletionConstants.PRIORITY_FUNCTION,
                    CompletionItemKind.Function
                ))
            }
        }
        
        return items
    }

    /**
     * 获取IO函数补全
     */
    fun getIOCompletions(prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        IO_FUNCTIONS.forEach { (function, func) ->
            if (function.startsWith(lowerPrefix)) {
                items.add(PriorityCompletionItem(
                    function,
                    "${func.signature} - ${func.description}",
                    prefix.length,
                    function,
                    CompletionConstants.PRIORITY_FUNCTION,
                    CompletionItemKind.Function
                ))
            }
        }
        
        return items
    }

    /**
     * 获取宏和常量补全
     */
    fun getMacroCompletions(prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        COMPETITIVE_MACROS.forEach { (macro, desc) ->
            if (macro.lowercase().startsWith(lowerPrefix)) {
                items.add(PriorityCompletionItem(
                    macro,
                    desc,
                    prefix.length,
                    macro,
                    CompletionConstants.PRIORITY_CONSTANT,
                    CompletionItemKind.Constant
                ))
            }
        }
        
        return items
    }

    /**
     * 获取容器成员函数补全
     */
    fun getContainerMemberCompletions(containerType: String, prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        // 清理容器类型名（移除模板参数）
        val cleanType = cleanContainerType(containerType)
        val members = STL_MEMBER_FUNCTIONS[cleanType] ?: return items
        
        members.forEach { member ->
            if (member.name.startsWith(lowerPrefix)) {
                items.add(PriorityCompletionItem(
                    member.name,
                    "${member.signature} - ${member.description}",
                    prefix.length,
                    member.name,
                    CompletionConstants.PRIORITY_STRUCT_MEMBER,
                    if (member.signature.contains("(")) CompletionItemKind.Method else CompletionItemKind.Field
                ))
            }
        }
        
        return items
    }

    /**
     * 清理容器类型名，移除模板参数
     */
    private fun cleanContainerType(containerType: String): String {
        return containerType
            .replace(Regex("<.*>"), "") // 移除模板参数
            .replace("*", "")            // 移除指针标记
            .replace("&", "")            // 移除引用标记
            .replace("const", "")        // 移除const修饰符
            .trim()
    }

    /**
     * 检查是否为STL容器类型
     */
    fun isSTLContainer(dataType: String): Boolean {
        val cleanType = cleanContainerType(dataType)
        return STL_CONTAINERS.containsKey(cleanType)
    }

    /**
     * 获取所有补全（用于常规补全）
     */
    fun getAllCompletions(prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        
        items.addAll(getKeywordCompletions(prefix))
        items.addAll(getContainerCompletions(prefix))
        items.addAll(getAlgorithmCompletions(prefix))
        items.addAll(getMathCompletions(prefix))
        items.addAll(getIOCompletions(prefix))
        items.addAll(getMacroCompletions(prefix))
        
        return items
    }
}