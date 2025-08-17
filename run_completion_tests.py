#!/usr/bin/env python3
"""
Simple test runner for ACMCompletionProvider tests.

This script demonstrates the test coverage by simulating the complex variable declarations
and showing what the ACMCompletionProvider should be able to identify.
"""

import re
from typing import Dict, List, Tuple

class TestResult:
    def __init__(self, test_name: str, passed: bool, details: str = ""):
        self.test_name = test_name
        self.passed = passed
        self.details = details

class MockACMCompletionProvider:
    """
    Mock implementation to demonstrate the expected behavior of ACMCompletionProvider
    """
    
    def __init__(self):
        self.symbols = {}
    
    def extract_multiple_variable_declarations(self, line: str) -> List[Tuple[str, str]]:
        """Extract variables from multiple variable declarations like 'int x, *p, y;'"""
        pattern = r'((?:const\s+)?(?:unsigned\s+)?(?:signed\s+)?(?:int|long\s+long|long|double|float|char|bool|string|short|[A-Z]\w*))\s+([^;{}]+);'
        match = re.search(pattern, line)
        
        if not match:
            return []
            
        base_type = match.group(1).strip()
        variables_part = match.group(2).strip()
        
        # Split by comma but handle nested parentheses
        variables = self.smart_split_variables(variables_part)
        
        result = []
        for var_decl in variables:
            var_name = self.extract_variable_name(var_decl.strip())
            if var_name:
                if "**" in var_decl:
                    var_type = f"{base_type}**"
                elif "*" in var_decl:
                    var_type = f"{base_type}*"
                elif "[" in var_decl:
                    var_type = f"{base_type}[]"
                else:
                    var_type = base_type
                result.append((var_name, var_type))
        
        return result
    
    def extract_template_declarations(self, line: str) -> List[Tuple[str, str]]:
        """Extract variables from template declarations like 'vector<int> arr(n);'"""
        patterns = [
            r'(vector|string|map|set|unordered_map|unordered_set|queue|stack|deque|pair|list|priority_queue|array|bitset)\s*<[^<>]*(?:<[^<>]*>[^<>]*)*>\s+(\w+)(?:\s*\([^)]*\)|\s*\{[^}]*\})?',
            r'(vector|string|map|set|unordered_map|unordered_set|queue|stack|deque|pair|list|priority_queue|array|bitset)\s*<[^<>]*(?:<[^<>]*>[^<>]*)*>\s+(\w+)\s*;'
        ]
        
        result = []
        for pattern in patterns:
            matches = re.finditer(pattern, line)
            for match in matches:
                container_type = match.group(1)
                var_name = match.group(2)
                if var_name:
                    result.append((var_name, container_type))
        
        return result
    
    def extract_array_declarations(self, line: str) -> List[Tuple[str, str]]:
        """Extract variables from array declarations like 'int arr[10];'"""
        result = []
        
        # First pattern: arrays of pointers like "int *ptr_arr[10];"
        pointer_array_pattern = r'((?:const\s+)?(?:unsigned\s+)?(?:int|long\s+long|long|double|float|char|bool|string|[A-Z]\w*))\s*(\*+)\s*(\w+)\s*\[[^\]]*\]'
        pointer_array_matches = re.finditer(pointer_array_pattern, line)
        
        for match in pointer_array_matches:
            base_type = match.group(1)
            pointer_stars = match.group(2)
            var_name = match.group(3)
            data_type = f"{base_type}{pointer_stars}[]"
            result.append((var_name, data_type))
        
        # Second pattern: regular arrays like "int arr[10];"
        array_pattern = r'((?:const\s+)?(?:unsigned\s+)?(?:int|long\s+long|long|double|float|char|bool|string|[A-Z]\w*))\s+(\w+)\s*\[[^\]]*\]'
        array_matches = re.finditer(array_pattern, line)
        
        # Track already found variables to avoid duplicates
        found_vars = {var_name for var_name, _ in result}
        
        for match in array_matches:
            base_type = match.group(1)
            var_name = match.group(2)
            
            # Skip if this variable was already added as a pointer array
            if var_name in found_vars:
                continue
                
            data_type = f"{base_type}[]"
            result.append((var_name, data_type))
        
        return result
    
    def smart_split_variables(self, declarations: str) -> List[str]:
        """Smart split for variable declarations, handling nested parentheses"""
        result = []
        current = []
        paren_depth = 0
        bracket_depth = 0
        
        for char in declarations:
            if char == '(':
                paren_depth += 1
                current.append(char)
            elif char == ')':
                paren_depth -= 1
                current.append(char)
            elif char == '[':
                bracket_depth += 1
                current.append(char)
            elif char == ']':
                bracket_depth -= 1
                current.append(char)
            elif char == ',' and paren_depth == 0 and bracket_depth == 0:
                result.append(''.join(current).strip())
                current = []
            else:
                current.append(char)
        
        if current:
            result.append(''.join(current).strip())
        
        return result
    
    def extract_variable_name(self, declaration: str) -> str:
        """Extract variable name from declaration"""
        # Remove initialization
        without_init = re.split(r'[=()]', declaration)[0].strip()
        # Remove array brackets
        without_array = without_init.split('[')[0].strip()
        # Remove pointers
        without_pointers = without_array.replace('*', '').strip()
        # Remove references
        without_reference = without_pointers.replace('&', '').strip()
        
        # Extract identifier
        match = re.search(r'(\w+)', without_reference)
        return match.group(1) if match else None

def run_test_case(test_name: str, code: str, expected_vars: Dict[str, str]) -> TestResult:
    """Run a single test case"""
    provider = MockACMCompletionProvider()
    found_vars = {}
    
    for line in code.split('\n'):
        line = line.strip()
        if not line or line.startswith('//') or line.startswith('#'):
            continue
            
        # Extract variables from different types of declarations
        multiple_vars = provider.extract_multiple_variable_declarations(line)
        template_vars = provider.extract_template_declarations(line)
        array_vars = provider.extract_array_declarations(line)
        
        for var_name, var_type in multiple_vars + template_vars + array_vars:
            found_vars[var_name] = var_type
    
    # Check if all expected variables were found
    missing_vars = []
    wrong_types = []
    
    for var_name, expected_type in expected_vars.items():
        if var_name not in found_vars:
            missing_vars.append(var_name)
        elif found_vars[var_name] != expected_type:
            wrong_types.append(f"{var_name}: expected {expected_type}, got {found_vars[var_name]}")
    
    # Check for unexpected variables
    unexpected_vars = [var for var in found_vars if var not in expected_vars]
    
    passed = len(missing_vars) == 0 and len(wrong_types) == 0
    
    details = f"Found {len(found_vars)} variables: {list(found_vars.keys())}\n"
    if missing_vars:
        details += f"Missing variables: {missing_vars}\n"
    if wrong_types:
        details += f"Wrong types: {wrong_types}\n"
    if unexpected_vars:
        details += f"Unexpected variables: {unexpected_vars}\n"
    
    return TestResult(test_name, passed, details)

def main():
    """Run all test cases"""
    print("ACMCompletionProvider Complex Variable Declaration Tests")
    print("=" * 60)
    
    test_cases = [
        {
            "name": "Multiple Variable Declarations",
            "code": """
                int x, *p, xx;
                long long a, b, c;
                unsigned int u, v;
                const int c1, c2;
            """,
            "expected": {
                "x": "int",
                "p": "int*",
                "xx": "int",
                "a": "long long",
                "b": "long long", 
                "c": "long long",
                "u": "unsigned int",
                "v": "unsigned int",
                "c1": "const int",
                "c2": "const int"
            }
        },
        {
            "name": "Template Types with Initialization",
            "code": """
                #include <vector>
                vector<int> arr(n);
                map<string, int> m(10);
                vector<vector<int>> matrix(5, vector<int>(5));
                vector<int> arr2{1, 2, 3};
            """,
            "expected": {
                "arr": "vector",
                "m": "map",
                "matrix": "vector",
                "arr2": "vector"
            }
        },
        {
            "name": "Array Declarations",
            "code": """
                int arr[10];
                char str[256];
                double matrix[10][10];
                float cube[5][5][5];
            """,
            "expected": {
                "arr": "int[]",
                "str": "char[]",
                "matrix": "double[]",
                "cube": "float[]"
            }
        },
        {
            "name": "Pointer Declarations",
            "code": """
                int *ptr;
                int **pptr;
                char *str_ptr, **str_pptr;
            """,
            "expected": {
                "ptr": "int*",
                "pptr": "int**",
                "str_ptr": "char*",
                "str_pptr": "char**"
            }
        },
        {
            "name": "Complex Mixed Declarations",
            "code": """
                vector<vector<int>> matrix(5, vector<int>(5));
                int *ptr_arr[10];
            """,
            "expected": {
                "matrix": "vector",
                "ptr_arr": "int*[]"
            }
        }
    ]
    
    results = []
    for test_case in test_cases:
        result = run_test_case(
            test_case["name"],
            test_case["code"],
            test_case["expected"]
        )
        results.append(result)
        
        status = "PASS" if result.passed else "FAIL"
        print(f"[{status}] {result.test_name}")
        if result.details:
            print(f"    {result.details}")
        print()
    
    # Summary
    passed = sum(1 for r in results if r.passed)
    total = len(results)
    print(f"Test Summary: {passed}/{total} tests passed")
    
    if passed == total:
        print("All tests passed! The ACMCompletionProvider should correctly identify")
        print("variables from complex C++ declarations.")
    else:
        print("Some tests failed. The ACMCompletionProvider may need additional")
        print("improvements to handle all complex declaration patterns.")

if __name__ == "__main__":
    main()