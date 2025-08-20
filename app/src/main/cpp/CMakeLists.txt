cmake_minimum_required(VERSION 3.22.1)

project("treesitter-jni")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Add include directories
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)
include_directories(${CMAKE_CURRENT_SOURCE_DIR})

# Set library paths
set(TREESITTER_LIB_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

# Library files
set(TREE_SITTER_LIB ${TREESITTER_LIB_DIR}/libtree-sitter.so)
set(TREE_SITTER_CPP_LIB ${TREESITTER_LIB_DIR}/libtree-sitter-cpp.so)
set(TREE_SITTER_JAVA_LIB ${TREESITTER_LIB_DIR}/libtree-sitter-java.so)
set(TREE_SITTER_PYTHON_LIB ${TREESITTER_LIB_DIR}/libtree-sitter-python.so)

# Verify library files exist
if(NOT EXISTS ${TREE_SITTER_LIB})
    message(FATAL_ERROR "TreeSitter library not found at ${TREE_SITTER_LIB}")
endif()
if(NOT EXISTS ${TREE_SITTER_CPP_LIB})
    message(FATAL_ERROR "TreeSitter C++ library not found at ${TREE_SITTER_CPP_LIB}")
endif()
if(NOT EXISTS ${TREE_SITTER_JAVA_LIB})
    message(FATAL_ERROR "TreeSitter Java library not found at ${TREE_SITTER_JAVA_LIB}")
endif()
if(NOT EXISTS ${TREE_SITTER_PYTHON_LIB})
    message(FATAL_ERROR "TreeSitter Python library not found at ${TREE_SITTER_PYTHON_LIB}")
endif()

# New modular TreeSitter JNI library with layered architecture
add_library(treesitter-jni SHARED 
    # Core abstraction layer
    core/AbstractTreeSitterProcessor.cpp
    core/TreeSitterRegistry.cpp
    
    # Language-specific implementations
    languages/CppLanguageProcessor.cpp
    languages/JavaLanguageProcessor.cpp
    languages/PythonLanguageProcessor.cpp
    
    # JNI interface layer
    TreeSitterJNI_New.cpp
)

# Link libraries
target_link_libraries(treesitter-jni
    ${TREE_SITTER_LIB}
    ${TREE_SITTER_CPP_LIB}
    ${TREE_SITTER_JAVA_LIB}
    ${TREE_SITTER_PYTHON_LIB}
    log
)

# Compile options
target_compile_options(treesitter-jni PRIVATE
    -Wall
    -Wextra
    -O2
    -fPIC
    -DANDROID
)

# Link options
target_link_options(treesitter-jni PRIVATE
    -Wl,-z,max-page-size=16384
    -Wl,-z,common-page-size=16384
    -Wl,-z,relro
    -Wl,-z,now
)

# Optional: Build legacy version for compatibility testing
option(BUILD_LEGACY "Build legacy TreeSitterCore.cpp for comparison" OFF)
if(BUILD_LEGACY)
    add_library(treesitter-jni-legacy SHARED 
        TreeSitterCore.cpp
        TreeSitterJNI.cpp
    )
    
    target_link_libraries(treesitter-jni-legacy
        ${TREE_SITTER_LIB}
        ${TREE_SITTER_CPP_LIB}
        ${TREE_SITTER_JAVA_LIB}
        ${TREE_SITTER_PYTHON_LIB}
        log
    )
    
    target_compile_options(treesitter-jni-legacy PRIVATE
        -Wall
        -Wextra
        -O2
        -fPIC
    )
endif()