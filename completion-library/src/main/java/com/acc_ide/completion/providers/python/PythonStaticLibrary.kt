package com.acc_ide.completion.providers.python

import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import com.acc_ide.completion.core.CompletionConstants
import com.acc_ide.completion.framework.UniversalCompletionEngine.PriorityCompletionItem

/**
 * Python 静态提示库
 * 
 * 包含：
 * 1. Python核心关键字
 * 2. 内置类型和数据结构
 * 3. 竞赛常用模块和函数
 * 4. 数学/IO相关函数
 */
class PythonStaticLibrary {

    /**
     * Python 核心关键字
     */
    private val PYTHON_KEYWORDS = mapOf(
        // 控制流关键字
        "if" to "Conditional statement",
        "elif" to "Else if branch",
        "else" to "Else branch",
        "for" to "For loop",
        "while" to "While loop",
        "break" to "Break statement",
        "continue" to "Continue statement",
        "pass" to "Null statement",
        "return" to "Return statement",
        
        // 函数和类定义
        "def" to "Function definition",
        "class" to "Class definition",
        "lambda" to "Lambda function",
        "global" to "Global variable declaration",
        "nonlocal" to "Nonlocal variable declaration",
        
        // 异常处理
        "try" to "Try block",
        "except" to "Exception handler",
        "finally" to "Finally block",
        "raise" to "Raise exception",
        
        // 逻辑操作符
        "and" to "Logical AND",
        "or" to "Logical OR",
        "not" to "Logical NOT",
        "in" to "Membership test",
        "is" to "Identity test",
        
        // 其他关键字
        "import" to "Import statement",
        "from" to "From import",
        "as" to "Alias declaration",
        "with" to "Context manager",
        "assert" to "Assertion",
        "del" to "Delete statement",
        "yield" to "Yield statement",
        "async" to "Async function",
        "await" to "Await coroutine"
    )

    /**
     * Python 内置类型
     */
    private val BUILTIN_TYPES = mapOf(
        "int" to "Integer type",
        "float" to "Floating point type",
        "complex" to "Complex number type",
        "bool" to "Boolean type",
        "str" to "String type",
        "list" to "List type",
        "tuple" to "Tuple type",
        "dict" to "Dictionary type",
        "set" to "Set type",
        "frozenset" to "Immutable set type",
        "bytes" to "Bytes type",
        "bytearray" to "Mutable bytes type",
        "range" to "Range type",
        "None" to "Null value",
        "True" to "Boolean true",
        "False" to "Boolean false"
    )

    /**
     * 内置数据结构方法
     */
    private val BUILTIN_METHODS = mapOf(
        "list" to listOf(
            MemberMethod("append", "list.append(x)", "Add item to end"),
            MemberMethod("extend", "list.extend(iterable)", "Extend list with iterable"),
            MemberMethod("insert", "list.insert(i, x)", "Insert item at position"),
            MemberMethod("remove", "list.remove(x)", "Remove first occurrence of x"),
            MemberMethod("pop", "list.pop([i])", "Remove and return item at index"),
            MemberMethod("clear", "list.clear()", "Remove all items"),
            MemberMethod("index", "list.index(x[, start[, end]])", "Find index of item"),
            MemberMethod("count", "list.count(x)", "Count occurrences of x"),
            MemberMethod("sort", "list.sort(*, key=None, reverse=False)", "Sort list in place"),
            MemberMethod("reverse", "list.reverse()", "Reverse list in place"),
            MemberMethod("copy", "list.copy()", "Return shallow copy of list"),
            MemberMethod("__len__", "len(list)", "Return length of list")
        ),
        
        "str" to listOf(
            MemberMethod("capitalize", "str.capitalize()", "Return capitalized string"),
            MemberMethod("upper", "str.upper()", "Return uppercase string"),
            MemberMethod("lower", "str.lower()", "Return lowercase string"),
            MemberMethod("title", "str.title()", "Return titlecased string"),
            MemberMethod("strip", "str.strip([chars])", "Remove leading/trailing chars"),
            MemberMethod("lstrip", "str.lstrip([chars])", "Remove leading chars"),
            MemberMethod("rstrip", "str.rstrip([chars])", "Remove trailing chars"),
            MemberMethod("split", "str.split(sep=None, maxsplit=-1)", "Split string"),
            MemberMethod("join", "str.join(iterable)", "Join iterable with string"),
            MemberMethod("replace", "str.replace(old, new[, count])", "Replace substring"),
            MemberMethod("find", "str.find(sub[, start[, end]])", "Find substring"),
            MemberMethod("startswith", "str.startswith(prefix[, start[, end]])", "Test if starts with"),
            MemberMethod("endswith", "str.endswith(suffix[, start[, end]])", "Test if ends with"),
            MemberMethod("isdigit", "str.isdigit()", "Test if all digits"),
            MemberMethod("isalpha", "str.isalpha()", "Test if all alphabetic"),
            MemberMethod("isalnum", "str.isalnum()", "Test if all alphanumeric"),
            MemberMethod("format", "str.format(*args, **kwargs)", "Format string"),
            MemberMethod("__len__", "len(str)", "Return length of string")
        ),
        
        "dict" to listOf(
            MemberMethod("keys", "dict.keys()", "Return view of keys"),
            MemberMethod("values", "dict.values()", "Return view of values"),
            MemberMethod("items", "dict.items()", "Return view of (key, value) pairs"),
            MemberMethod("get", "dict.get(key[, default])", "Get value for key"),
            MemberMethod("pop", "dict.pop(key[, default])", "Remove and return value"),
            MemberMethod("popitem", "dict.popitem()", "Remove and return (key, value) pair"),
            MemberMethod("clear", "dict.clear()", "Remove all items"),
            MemberMethod("update", "dict.update([other])", "Update with key/value pairs"),
            MemberMethod("setdefault", "dict.setdefault(key[, default])", "Set default for key"),
            MemberMethod("__len__", "len(dict)", "Return number of items")
        ),
        
        "set" to listOf(
            MemberMethod("add", "set.add(elem)", "Add element to set"),
            MemberMethod("remove", "set.remove(elem)", "Remove element from set"),
            MemberMethod("discard", "set.discard(elem)", "Remove element if present"),
            MemberMethod("pop", "set.pop()", "Remove and return arbitrary element"),
            MemberMethod("clear", "set.clear()", "Remove all elements"),
            MemberMethod("union", "set.union(*others)", "Return union of sets"),
            MemberMethod("intersection", "set.intersection(*others)", "Return intersection of sets"),
            MemberMethod("difference", "set.difference(*others)", "Return difference of sets"),
            MemberMethod("symmetric_difference", "set.symmetric_difference(other)", "Return symmetric difference"),
            MemberMethod("issubset", "set.issubset(other)", "Test if subset"),
            MemberMethod("issuperset", "set.issuperset(other)", "Test if superset"),
            MemberMethod("__len__", "len(set)", "Return number of elements")
        )
    )

    /**
     * 常用内置函数
     */
    private val BUILTIN_FUNCTIONS = mapOf(
        // 类型转换
        "int" to BuiltinFunction("int([x[, base]])", "Convert to integer"),
        "float" to BuiltinFunction("float([x])", "Convert to float"),
        "str" to BuiltinFunction("str(object='')", "Convert to string"),
        "bool" to BuiltinFunction("bool([x])", "Convert to boolean"),
        "list" to BuiltinFunction("list([iterable])", "Create list"),
        "tuple" to BuiltinFunction("tuple([iterable])", "Create tuple"),
        "dict" to BuiltinFunction("dict(**kwargs)", "Create dictionary"),
        "set" to BuiltinFunction("set([iterable])", "Create set"),
        
        // 数学函数
        "abs" to BuiltinFunction("abs(x)", "Absolute value"),
        "max" to BuiltinFunction("max(iterable, *[, key, default])", "Maximum value"),
        "min" to BuiltinFunction("min(iterable, *[, key, default])", "Minimum value"),
        "sum" to BuiltinFunction("sum(iterable[, start])", "Sum of elements"),
        "pow" to BuiltinFunction("pow(base, exp[, mod])", "Power function"),
        "round" to BuiltinFunction("round(number[, ndigits])", "Round number"),
        "divmod" to BuiltinFunction("divmod(a, b)", "Quotient and remainder"),
        
        // 序列操作
        "len" to BuiltinFunction("len(s)", "Length of object"),
        "range" to BuiltinFunction("range(stop)", "Create range object"),
        "enumerate" to BuiltinFunction("enumerate(iterable, start=0)", "Enumerate iterable"),
        "zip" to BuiltinFunction("zip(*iterables)", "Zip iterables together"),
        "sorted" to BuiltinFunction("sorted(iterable, *, key=None, reverse=False)", "Return sorted list"),
        "reversed" to BuiltinFunction("reversed(seq)", "Return reverse iterator"),
        
        // 对象操作
        "type" to BuiltinFunction("type(object)", "Return type of object"),
        "isinstance" to BuiltinFunction("isinstance(object, classinfo)", "Test instance type"),
        "issubclass" to BuiltinFunction("issubclass(class, classinfo)", "Test subclass"),
        "hasattr" to BuiltinFunction("hasattr(object, name)", "Test if object has attribute"),
        "getattr" to BuiltinFunction("getattr(object, name[, default])", "Get attribute value"),
        "setattr" to BuiltinFunction("setattr(object, name, value)", "Set attribute value"),
        "delattr" to BuiltinFunction("delattr(object, name)", "Delete attribute"),
        
        // 输入输出
        "print" to BuiltinFunction("print(*objects, sep=' ', end='\\n', file=sys.stdout, flush=False)", "Print objects"),
        "input" to BuiltinFunction("input([prompt])", "Read input from stdin"),
        
        // 其他常用函数
        "map" to BuiltinFunction("map(function, iterable, ...)", "Apply function to items"),
        "filter" to BuiltinFunction("filter(function, iterable)", "Filter items with function"),
        "any" to BuiltinFunction("any(iterable)", "Test if any element is true"),
        "all" to BuiltinFunction("all(iterable)", "Test if all elements are true"),
        "chr" to BuiltinFunction("chr(i)", "Return character from Unicode code"),
        "ord" to BuiltinFunction("ord(c)", "Return Unicode code from character")
    )

    /**
     * 竞赛常用模块
     */
    private val COMPETITIVE_MODULES = mapOf(
        "math" to listOf(
            ModuleFunction("sqrt", "math.sqrt(x)", "Square root"),
            ModuleFunction("pow", "math.pow(x, y)", "Power function"),
            ModuleFunction("ceil", "math.ceil(x)", "Ceiling function"),
            ModuleFunction("floor", "math.floor(x)", "Floor function"),
            ModuleFunction("fabs", "math.fabs(x)", "Absolute value"),
            ModuleFunction("log", "math.log(x[, base])", "Natural logarithm"),
            ModuleFunction("log10", "math.log10(x)", "Base-10 logarithm"),
            ModuleFunction("sin", "math.sin(x)", "Sine function"),
            ModuleFunction("cos", "math.cos(x)", "Cosine function"),
            ModuleFunction("tan", "math.tan(x)", "Tangent function"),
            ModuleFunction("pi", "math.pi", "Pi constant"),
            ModuleFunction("e", "math.e", "Euler's number")
        ),
        
        "collections" to listOf(
            ModuleFunction("Counter", "collections.Counter([iterable-or-mapping])", "Count hashable objects"),
            ModuleFunction("deque", "collections.deque([iterable[, maxlen]])", "Double-ended queue"),
            ModuleFunction("defaultdict", "collections.defaultdict([default_factory])", "Dictionary with default factory"),
            ModuleFunction("OrderedDict", "collections.OrderedDict([items])", "Dictionary that remembers insertion order"),
            ModuleFunction("namedtuple", "collections.namedtuple(typename, field_names)", "Factory function for tuples")
        ),
        
        "itertools" to listOf(
            ModuleFunction("count", "itertools.count(start=0, step=1)", "Infinite iterator of evenly spaced values"),
            ModuleFunction("cycle", "itertools.cycle(iterable)", "Infinite iterator cycling through iterable"),
            ModuleFunction("repeat", "itertools.repeat(object[, times])", "Repeat object endlessly or n times"),
            ModuleFunction("chain", "itertools.chain(*iterables)", "Chain multiple iterables together"),
            ModuleFunction("combinations", "itertools.combinations(iterable, r)", "Combinations of iterable elements"),
            ModuleFunction("permutations", "itertools.permutations(iterable, r=None)", "Permutations of iterable elements")
        )
    )

    // 数据类定义
    data class MemberMethod(val name: String, val signature: String, val description: String)
    data class BuiltinFunction(val signature: String, val description: String)
    data class ModuleFunction(val name: String, val signature: String, val description: String)

    /**
     * 获取关键字补全
     */
    fun getKeywordCompletions(prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        PYTHON_KEYWORDS.forEach { (keyword, desc) ->
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
     * 获取内置类型补全
     */
    fun getBuiltinTypeCompletions(prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        BUILTIN_TYPES.forEach { (typeName, desc) ->
            if (typeName.lowercase().startsWith(lowerPrefix)) {
                items.add(PriorityCompletionItem(
                    typeName,
                    desc,
                    prefix.length,
                    typeName,
                    CompletionConstants.PRIORITY_TYPE,
                    CompletionItemKind.Class
                ))
            }
        }
        
        return items
    }

    /**
     * 获取内置函数补全
     */
    fun getBuiltinFunctionCompletions(prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        BUILTIN_FUNCTIONS.forEach { (functionName, func) ->
            if (functionName.lowercase().startsWith(lowerPrefix)) {
                items.add(PriorityCompletionItem(
                    functionName,
                    "${func.signature} - ${func.description}",
                    prefix.length,
                    functionName,
                    CompletionConstants.PRIORITY_FUNCTION,
                    CompletionItemKind.Function
                ))
            }
        }
        
        return items
    }

    /**
     * 获取模块补全
     */
    fun getModuleCompletions(prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        COMPETITIVE_MODULES.keys.forEach { moduleName ->
            if (moduleName.lowercase().startsWith(lowerPrefix)) {
                items.add(PriorityCompletionItem(
                    moduleName,
                    "Module - $moduleName",
                    prefix.length,
                    moduleName,
                    CompletionConstants.PRIORITY_STL_CONTAINER,
                    CompletionItemKind.Module
                ))
            }
        }
        
        return items
    }

    /**
     * 获取内置类型成员方法补全
     */
    fun getBuiltinMemberCompletions(typeName: String, prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        val methods = BUILTIN_METHODS[typeName] ?: return items
        
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
     * 获取模块函数补全
     */
    fun getModuleMemberCompletions(moduleName: String, prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        val lowerPrefix = prefix.lowercase()
        
        val functions = COMPETITIVE_MODULES[moduleName] ?: return items
        
        functions.forEach { function ->
            if (function.name.startsWith(lowerPrefix)) {
                items.add(PriorityCompletionItem(
                    function.name,
                    "${function.signature} - ${function.description}",
                    prefix.length,
                    function.name,
                    CompletionConstants.PRIORITY_STRUCT_MEMBER,
                    CompletionItemKind.Function
                ))
            }
        }
        
        return items
    }

    /**
     * 检查是否为Python内置类型
     */
    fun isBuiltinType(dataType: String): Boolean {
        return BUILTIN_TYPES.containsKey(dataType)
    }

    /**
     * 检查是否为Python模块
     */
    fun isModule(dataType: String): Boolean {
        return COMPETITIVE_MODULES.containsKey(dataType)
    }

    /**
     * 获取所有补全（用于常规补全）
     */
    fun getAllCompletions(prefix: String): List<CompletionItem> {
        val items = mutableListOf<CompletionItem>()
        
        items.addAll(getKeywordCompletions(prefix))
        items.addAll(getBuiltinTypeCompletions(prefix))
        items.addAll(getBuiltinFunctionCompletions(prefix))
        items.addAll(getModuleCompletions(prefix))
        
        return items
    }
}