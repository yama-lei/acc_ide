# ACM Completion Provider Tests

This directory contains comprehensive tests for the ACMCompletionProvider to verify that the enhanced autocomplete functionality correctly identifies variables from complex C++ declarations.

## Test Coverage

The tests verify that the ACMCompletionProvider correctly identifies variables from:

1. **Multiple variable declarations**: `int x, *p, xx;`
2. **Template types with initialization**: `vector<int> arr(n);`
3. **Array declarations**: `int matrix[10][10];`
4. **Pointer declarations**: `int *ptr, **pptr;`
5. **Complex mixed declarations**: `vector<vector<int>> matrix(5, vector<int>(5));`

## Running the Tests

### Unit Tests (Kotlin/JUnit)

To run the Kotlin unit tests with Gradle:

```bash
./gradlew test
```

Or specifically for the app module:

```bash
./gradlew app:test
```

### Python Test Script

For a quick verification without setting up the full Android test environment, you can run the Python test script:

```bash
python run_completion_tests.py
```

This script provides a quick way to verify the parsing logic without needing the full Android testing framework.

## Test Structure

- `ACMCompletionProviderTest.kt` - Main JUnit test class with comprehensive test cases
- `run_completion_tests.py` - Python script for quick verification of parsing logic

## Dependencies

The tests use:
- JUnit 4 for test annotations and running
- Robolectric for Android framework mocking
- Google Truth for fluent assertions