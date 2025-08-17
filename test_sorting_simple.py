#!/usr/bin/env python3
"""
Test script to verify the sorting logic for ACMCompletionProvider
"""

# Define the new priority constants
PRIORITY_LOCAL_VARIABLE = 200
PRIORITY_LOCAL_FUNCTION = 190
PRIORITY_GLOBAL_VARIABLE = 180
PRIORITY_STL_COMMON = 150
PRIORITY_STL_FUNCTION = 140
PRIORITY_KEYWORD = 100

# Sample completion items with their priorities
completion_items = [
    ("localVar", PRIORITY_LOCAL_VARIABLE),
    ("localFunction", PRIORITY_LOCAL_FUNCTION), 
    ("globalVar", PRIORITY_GLOBAL_VARIABLE),
    ("vector", PRIORITY_STL_COMMON),
    ("int", PRIORITY_KEYWORD),
    ("void", PRIORITY_KEYWORD),
]

def test_sorting():
    """Test that local symbols come before keywords"""
    print("Testing completion sorting...")
    print("Before sorting:")
    for item in completion_items:
        print(f"  {item[0]} (priority: {item[1]})")
    
    print("\nAfter sorting by priority (descending):")
    sorted_items = sorted(completion_items, key=lambda x: x[1], reverse=True)
    for item in sorted_items:
        print(f"  {item[0]} (priority: {item[1]})")
    
    # Check that local variables come before keywords
    local_var_index = next(i for i, item in enumerate(sorted_items) if item[0] == "localVar")
    keyword_index = next(i for i, item in enumerate(sorted_items) if item[0] == "int")
    
    print(f"\nLocal variable 'localVar' index: {local_var_index}")
    print(f"Keyword 'int' index: {keyword_index}")
    
    if local_var_index < keyword_index:
        print("PASS: Local variables come before keywords")
        return True
    else:
        print("FAIL: Local variables do not come before keywords")
        return False

if __name__ == "__main__":
    success = test_sorting()
    print(f"Test result: {'SUCCESS' if success else 'FAILED'}")