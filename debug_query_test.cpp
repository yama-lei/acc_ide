#include <iostream>
#include <string>

int main() {
    // 创建一个最简单的查询来测试
    std::string testQuery = R"(
; Test query - 最简单的函数匹配
(function_definition
  declarator: (function_declarator
    declarator: (identifier) @local.definition.function))
)";

    std::cout << "Test query:" << std::endl;
    std::cout << testQuery << std::endl;
    
    return 0;
}