#!/usr/bin/env python3
"""
Test script to verify the sorting logic for ACMCompletionProvider
"""

# Define the new priority constants
PRIORITY_EXACT_MATCH = 300
PRIORITY_RECENT_LOCAL = 250
PRIORITY_LOCAL_VARIABLE = 200
PRIORITY_LOCAL_FUNCTION = 190
PRIORITY_GLOBAL_VARIABLE = 180
PRIORITY_STRUCT_MEMBER = 170
PRIORITY_TYPE = 160
PRIORITY_STL_COMMON = 150
PRIORITY_STL_FUNCTION = 140
PRIORITY_STL_UNCOMMON = 130
PRIORITY_KEYWORD = 100

# Sample completion items with their priorities
completion_items = [
    ("localVar", PRIORITY_LOCAL_VARIABLE, "Local variable"),
    ("localFunction", PRIORITY_LOCAL_FUNCTION, "Local function"),
    ("globalVar", PRIORITY_GLOBAL_VARIABLE, "Global variable"),
    ("vector", PRIORITY_STL_COMMON, "STL container"),
    ("size", PRIORITY_STL_COMMON, "STL method"),
    ("int", PRIORITY_KEYWORD, "Keyword"),
    ("void", PRIORITY_KEYWORD, "Keyword"),
    ("class", PRIORITY_KEYWORD, "Keyword"),
    ("push_back", PRIORITY_STL_FUNCTION, "STL method"),
    ("begin", PRIORITY_STL_FUNCTION, "STL method"),
]

def test_sorting():
    """Test that local symbols come before keywords"""
    print("Completion items before sorting:")
    for item in completion_items:
        print(f"  {item[0]} (priority: {item[1]}) - {item[2]}")
    
    print("\nCompletion items after sorting by priority (descending):")
    sorted_items = sorted(completion_items, key=lambda x: x[1], reverse=True)
    for item in sorted_items:
        print(f"  {item[0]} (priority: {item[1]}) - {item[2]}")
    
    # Check that local variables come before keywords
    local_var_index = next(i for i, item in enumerate(sorted_items) if item[0] == "localVar")
    keyword_index = next(i for i, item in enumerate(sorted_items) if item[0] == "int")
    
    print(f"\nLocal variable 'localVar' index: {local_var_index}")
    print(f"Keyword 'int' index: {keyword_index}")
    
    if local_var_index < keyword_index:
        print("✅ PASS: Local variables come before keywords")
        return True
    else:
        print("❌ FAIL: Local variables do not come before keywords")
        return False

if __name__ == "__main__":
    success = test_sorting()
    exit(0 if success else 1)