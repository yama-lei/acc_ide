cmake_minimum_required(VERSION 3.22.1)

project("treesitter-jni")

# 设置C++标准
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 添加include目录
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)

# 设置库文件路径 - 修正为正确的相对路径
set(TREESITTER_LIB_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI})

# 直接指定库文件路径
set(TREE_SITTER_LIB ${TREESITTER_LIB_DIR}/libtree-sitter.so)
set(TREE_SITTER_CPP_LIB ${TREESITTER_LIB_DIR}/libtree-sitter-cpp.so)
set(TREE_SITTER_JAVA_LIB ${TREESITTER_LIB_DIR}/libtree-sitter-java.so)
set(TREE_SITTER_PYTHON_LIB ${TREESITTER_LIB_DIR}/libtree-sitter-python.so)

# 验证库文件是否存在
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

# Tree-sitter JNI库 - 官方设计：TSParser → TSTree → 手动符号提取
add_library(treesitter-jni SHARED 
    TreeSitterCore.cpp
    TreeSitterJNI.cpp
)

# 链接库
target_link_libraries(treesitter-jni
    ${TREE_SITTER_LIB}
    ${TREE_SITTER_CPP_LIB}
    ${TREE_SITTER_JAVA_LIB}
    ${TREE_SITTER_PYTHON_LIB}
    log
)

# 设置编译选项
target_compile_options(treesitter-jni PRIVATE
    -Wall
    -Wextra
    -O2
    -fPIC
)

# 设置链接选项
target_link_options(treesitter-jni PRIVATE
    -Wl,-z,max-page-size=16384
    -Wl,-z,common-page-size=16384
    -Wl,-z,relro
    -Wl,-z,now
)