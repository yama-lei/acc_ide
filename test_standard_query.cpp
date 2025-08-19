#include <iostream>
#include <string>
#include <vector>
#include "app/src/main/cpp/TreeSitterStandardQuery.h"

int main() {
    std::string code = R"(
#include <iostream>

const int MAXN = 100005;

bool vis[MAXN];  // Global array variable

int main() {
    int arr[10];    // Local array variable  
    char str[256];  // Another array variable
    
    for(int i = 0; i < 10; i++) {
        vis[i] = true;
        arr[i] = i;
    }
    
    return 0;
}
)";

    std::cout << "Testing standard Tree-sitter query for array recognition..." << std::endl;
    
    // Test standard query
    TreeSitterStandardQuery::StandardQueryResult result = 
        TreeSitterStandardQuery::parseWithStandardQuery(code, "cpp");
    
    std::cout << "Query success: " << (result.success ? "true" : "false") << std::endl;
    
    if (!result.success) {
        std::cout << "Error: " << result.errorMessage << std::endl;
        return 1;
    }
    
    std::cout << "Found " << result.symbols.size() << " symbols:" << std::endl;
    
    for (const auto& symbol : result.symbols) {
        std::cout << "- " << symbol.name << " (" << symbol.dataType << ") at line " << symbol.line << std::endl;
    }
    
    // Check specifically for array variables
    std::cout << "\nArray variables found:" << std::endl;
    bool found_vis = false, found_arr = false, found_str = false;
    
    for (const auto& symbol : result.symbols) {
        if (symbol.name == "vis") {
            found_vis = true;
            std::cout << "✓ Found global array: vis" << std::endl;
        } else if (symbol.name == "arr") {
            found_arr = true;
            std::cout << "✓ Found local array: arr" << std::endl;
        } else if (symbol.name == "str") {
            found_str = true;
            std::cout << "✓ Found local array: str" << std::endl;
        }
    }
    
    if (!found_vis) std::cout << "✗ Global array 'vis' not found" << std::endl;
    if (!found_arr) std::cout << "✗ Local array 'arr' not found" << std::endl;
    if (!found_str) std::cout << "✗ Local array 'str' not found" << std::endl;
    
    return 0;
}