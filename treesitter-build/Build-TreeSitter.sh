#!/bin/bash
#
# Tree-sitter Library Builder for Android
# 
# DESCRIPTION:
#     Builds Tree-sitter libraries with proper 16KB alignment for Android devices.
#     Supports C++, Java, and Python parsers with optimized configuration.
#     
# USAGE:
#     ./Build-TreeSitter.sh [options]
#     
# OPTIONS:
#     -n, --ndk-path PATH         Path to Android NDK (e.g., /opt/android-ndk-r26d)
#     -a, --architectures ARCHS   Comma-separated architectures (arm64-v8a,armeabi-v7a,x86_64,x86)
#     -c, --cmake-path PATH       Custom CMake executable path (optional)
#     --non-interactive           Run in non-interactive mode (requires all parameters)
#     -h, --help                  Show this help message
#     
# EXAMPLES:
#     ./Build-TreeSitter.sh
#     ./Build-TreeSitter.sh --ndk-path "/opt/android-ndk-r26d" --architectures "arm64-v8a" --non-interactive
#     
# NOTES:
#     Version: 0.7
#     Author: META XIAO
#     Date: 2025-08-18
#     
#     Requirements:
#     - Android NDK 27.0+
#     - Bash 4.0+
#     - curl or wget for downloads
#     - Internet connection for source downloads
#     
#     Output:
#     - Libraries: treesitter_build/libs/{arch}/
#     - App Copy: app/src/main/jniLibs/{arch}/

set -e  # Exit on any error

# Script metadata
SCRIPT_VERSION="0.7"
BUILD_DATE=$(date +"%Y-%m-%d")

# Initialize variables
ANDROID_NDK_PATH=""
ARCHITECTURES=()
CUSTOM_CMAKE_PATH=""
NON_INTERACTIVE=false

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIBS_OUTPUT_DIR="$SCRIPT_DIR/libs"
BUILD_DIR="$SCRIPT_DIR/build_temp"

# Color definitions
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
WHITE='\033[1;37m'
GRAY='\033[0;37m'
NC='\033[0m' # No Color

#region Helper Functions

# Display colored output
write_step_header() {
    local title="$1"
    local step="$2"
    echo ""
    echo -e "${CYAN}=== $step : $title ===${NC}"
}

write_success() {
    echo -e "${GREEN}SUCCESS: $1${NC}"
}

write_warning() {
    echo -e "${YELLOW}WARNING: $1${NC}"
}

write_error() {
    echo -e "${RED}ERROR: $1${NC}"
}

write_info() {
    echo -e "${CYAN}INFO: $1${NC}"
}

# Display header
display_header() {
    echo -e "${CYAN}=================================================================${NC}"
    echo -e "${GREEN}   Tree-sitter Library Builder v$SCRIPT_VERSION${NC}"
    echo -e "${GREEN}   Optimized for Android with 16KB Page Alignment${NC}"
    echo -e "${GRAY}   Build Date: $BUILD_DATE${NC}"
    echo -e "${CYAN}=================================================================${NC}"
    echo ""
}

# Show help
show_help() {
    cat << EOF
Tree-sitter Library Builder for Android v$SCRIPT_VERSION

USAGE:
    $0 [options]

OPTIONS:
    -n, --ndk-path PATH         Path to Android NDK (e.g., /opt/android-ndk-r26d)
    -a, --architectures ARCHS   Comma-separated architectures (arm64-v8a,armeabi-v7a,x86_64,x86)
    -c, --cmake-path PATH       Custom CMake executable path (optional)
    --non-interactive           Run in non-interactive mode (requires all parameters)
    -h, --help                  Show this help message

EXAMPLES:
    $0
    $0 --ndk-path "/opt/android-ndk-r26d" --architectures "arm64-v8a" --non-interactive

ARCHITECTURES:
    arm64-v8a     - 64-bit ARM (Modern phones)
    armeabi-v7a   - 32-bit ARM (Older devices)
    x86_64        - 64-bit x86 (Emulators)
    x86           - 32-bit x86 (Old emulators)

EOF
}

# Parse command line arguments
parse_arguments() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -n|--ndk-path)
                ANDROID_NDK_PATH="$2"
                shift 2
                ;;
            -a|--architectures)
                IFS=',' read -ra ARCHITECTURES <<< "$2"
                shift 2
                ;;
            -c|--cmake-path)
                CUSTOM_CMAKE_PATH="$2"
                shift 2
                ;;
            --non-interactive)
                NON_INTERACTIVE=true
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                echo "Unknown option: $1"
                show_help
                exit 1
                ;;
        esac
    done
}

# Clean build files
clear_build_files() {
    write_info "Cleaning previous build files..."
    
    local cleanup_items=(
        "$BUILD_DIR:Build directory"
        "$LIBS_OUTPUT_DIR:Output directory"
        "$SCRIPT_DIR/../app/src/main/jniLibs:App jniLibs directory"
    )
    
    for item in "${cleanup_items[@]}"; do
        local path="${item%%:*}"
        local name="${item##*:}"
        
        if [[ -d "$path" ]]; then
            if rm -rf "$path" 2>/dev/null; then
                write_success "Removed $name"
            else
                write_warning "Failed to remove $name"
            fi
        fi
    done
    
    write_success "Cleanup completed"
}

# Test Android NDK
test_android_ndk() {
    local ndk_path="$1"
    
    if [[ -z "$ndk_path" || ! -d "$ndk_path" ]]; then
        return 1
    fi
    
    local ndk_build="$ndk_path/ndk-build"
    [[ -f "$ndk_build" && -x "$ndk_build" ]]
}

# Get user architecture choice
get_user_architecture_choice() {
    local -A arch_choices=(
        ["1"]="arm64-v8a:arm64-v8a (64-bit ARM - Modern phones)"
        ["2"]="armeabi-v7a:armeabi-v7a (32-bit ARM - Older devices)"
        ["3"]="x86_64:x86_64 (64-bit x86 - Emulators)"
        ["4"]="x86:x86 (32-bit x86 - Old emulators)"
        ["5"]="arm64-v8a,armeabi-v7a,x86_64,x86:All architectures"
    )
    
    echo -e "${GREEN}Available architectures:${NC}"
    for key in $(printf '%s\n' "${!arch_choices[@]}" | sort); do
        local desc="${arch_choices[$key]##*:}"
        echo -e "${YELLOW}  $key. $desc${NC}"
    done
    echo ""
    
    while true; do
        read -p "Select architectures to build (1-5): " choice
        if [[ -n "${arch_choices[$choice]}" ]]; then
            local archs="${arch_choices[$choice]%%:*}"
            local desc="${arch_choices[$choice]##*:}"
            write_success "Selected: $desc"
            IFS=',' read -ra ARCHITECTURES <<< "$archs"
            break
        else
            write_error "Invalid choice, please enter 1-5"
        fi
    done
}

# Download Tree-sitter sources
download_tree_sitter_sources() {
    local build_dir="$1"
    
    # Define sources
    declare -A sources=(
        ["tree-sitter"]="https://github.com/tree-sitter/tree-sitter/archive/refs/tags/v0.22.6.zip:tree-sitter-0.22.6:tree-sitter"
        ["tree-sitter-java"]="https://github.com/tree-sitter/tree-sitter-java/archive/refs/heads/master.zip:tree-sitter-java-master:tree-sitter-java"
        ["tree-sitter-cpp"]="https://github.com/tree-sitter/tree-sitter-cpp/archive/refs/heads/master.zip:tree-sitter-cpp-master:tree-sitter-cpp"
        ["tree-sitter-python"]="https://github.com/tree-sitter/tree-sitter-python/archive/refs/heads/master.zip:tree-sitter-python-master:tree-sitter-python"
    )
    
    cd "$build_dir"
    
    # Detect download tool
    local download_cmd=""
    if command -v curl >/dev/null 2>&1; then
        download_cmd="curl -L -o"
    elif command -v wget >/dev/null 2>&1; then
        download_cmd="wget -O"
    else
        echo "Error: Neither curl nor wget found. Please install one of them."
        exit 1
    fi
    
    for source in "${!sources[@]}"; do
        write_info "Downloading $source..."
        local source_info="${sources[$source]}"
        local url="${source_info%%:*}"
        local extract_name="${source_info#*:}"
        extract_name="${extract_name%%:*}"
        local final_name="${source_info##*:}"
        
        local zip_file="$source.zip"
        
        if $download_cmd "$zip_file" "$url"; then
            if command -v unzip >/dev/null 2>&1; then
                unzip -q "$zip_file"
            else
                write_error "unzip command not found. Please install unzip."
                exit 1
            fi
            rm -f "$zip_file"
            
            if [[ -d "$extract_name" ]]; then
                mv "$extract_name" "$final_name"
            fi
            
            write_success "Downloaded and extracted $source"
        else
            write_error "Failed to download $source"
            exit 1
        fi
    done
    
    # Verify required files
    local required_files=(
        "tree-sitter/lib/src/lib.c"
        "tree-sitter-java/src/parser.c"
        "tree-sitter-cpp/src/parser.c"
        "tree-sitter-python/src/parser.c"
    )
    
    for file in "${required_files[@]}"; do
        if [[ ! -f "$file" ]]; then
            write_error "Required source file not found: $file"
            exit 1
        fi
    done
    
    write_success "All source files verified"
}

# Create build configuration
create_build_configuration() {
    local architectures=("$@")
    
    # Detect scanner files
    local cpp_scanner_file=""
    local cpp_scanner_candidates=("tree-sitter-cpp/src/scanner.cc" "tree-sitter-cpp/src/scanner.c")
    for candidate in "${cpp_scanner_candidates[@]}"; do
        if [[ -f "$candidate" ]]; then
            cpp_scanner_file="$candidate"
            break
        fi
    done
    
    local python_scanner_file=""
    if [[ -f "tree-sitter-python/src/scanner.c" ]]; then
        python_scanner_file="tree-sitter-python/src/scanner.c"
    fi
    
    # Build source lists
    local cpp_sources="tree-sitter-cpp/src/parser.c"
    if [[ -n "$cpp_scanner_file" ]]; then
        cpp_sources="$cpp_sources $cpp_scanner_file"
    fi
    
    local python_sources="tree-sitter-python/src/parser.c"
    if [[ -n "$python_scanner_file" ]]; then
        python_sources="$python_sources $python_scanner_file"
    fi
    
    # Create Android.mk
    cat > Android.mk << EOF
LOCAL_PATH := \$(call my-dir)

# Global 16KB alignment flags for Android 15+ compatibility
ALIGNMENT_FLAGS := -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384 -Wl,-z,relro -Wl,-z,now

# Tree-sitter core library
include \$(CLEAR_VARS)
LOCAL_MODULE := tree-sitter
LOCAL_SRC_FILES := tree-sitter/lib/src/lib.c
LOCAL_C_INCLUDES := tree-sitter/lib/include tree-sitter/lib/src
LOCAL_EXPORT_C_INCLUDES := tree-sitter/lib/include
LOCAL_CFLAGS := -std=c11 -O2 -DTREE_SITTER_HIDE_SYMBOLS -fPIC -D_GNU_SOURCE
LOCAL_LDFLAGS := \$(ALIGNMENT_FLAGS) -s
LOCAL_LDLIBS := \$(ALIGNMENT_FLAGS)
include \$(BUILD_SHARED_LIBRARY)

# Java grammar parser
include \$(CLEAR_VARS)
LOCAL_MODULE := tree-sitter-java
LOCAL_SRC_FILES := tree-sitter-java/src/parser.c
LOCAL_C_INCLUDES := tree-sitter/lib/include
LOCAL_SHARED_LIBRARIES := tree-sitter
LOCAL_CFLAGS := -std=c11 -O2 -fPIC -D_GNU_SOURCE
LOCAL_LDFLAGS := \$(ALIGNMENT_FLAGS) -s
LOCAL_LDLIBS := \$(ALIGNMENT_FLAGS)
include \$(BUILD_SHARED_LIBRARY)

# C++ grammar parser
include \$(CLEAR_VARS)
LOCAL_MODULE := tree-sitter-cpp
LOCAL_SRC_FILES := $cpp_sources
LOCAL_C_INCLUDES := tree-sitter/lib/include
LOCAL_SHARED_LIBRARIES := tree-sitter
LOCAL_CFLAGS := -std=c11 -O2 -fPIC -D_GNU_SOURCE
LOCAL_CPPFLAGS := -std=c++11 -O2 -fPIC -D_GNU_SOURCE
LOCAL_LDFLAGS := \$(ALIGNMENT_FLAGS) -s
LOCAL_LDLIBS := \$(ALIGNMENT_FLAGS)
include \$(BUILD_SHARED_LIBRARY)

# Python grammar parser
include \$(CLEAR_VARS)
LOCAL_MODULE := tree-sitter-python
LOCAL_SRC_FILES := $python_sources
LOCAL_C_INCLUDES := tree-sitter/lib/include
LOCAL_SHARED_LIBRARIES := tree-sitter
LOCAL_CFLAGS := -std=c11 -O2 -fPIC -D_GNU_SOURCE
LOCAL_LDFLAGS := \$(ALIGNMENT_FLAGS) -s
LOCAL_LDLIBS := \$(ALIGNMENT_FLAGS)
include \$(BUILD_SHARED_LIBRARY)
EOF
    
    # Create Application.mk
    local arch_string=$(IFS=' '; echo "${architectures[*]}")
    cat > Application.mk << EOF
# Tree-sitter Builder v$SCRIPT_VERSION - Android Configuration
# Enhanced NDK configuration for 16KB alignment

NDK_TOOLCHAIN_VERSION := clang

APP_PLATFORM := android-21
APP_ABI := $arch_string
APP_STL := c++_shared
APP_CPPFLAGS := -frtti -fexceptions -std=c++11

# Enhanced 16KB alignment with proper linker settings
APP_LDFLAGS := -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384 -Wl,-z,relro -Wl,-z,now -Wl,--hash-style=gnu

# Force use of lld linker for better 16KB support  
APP_LD := lld

# Optimization settings
APP_OPTIM := release
APP_CFLAGS := -O2 -DNDEBUG -fPIC -D_GNU_SOURCE
APP_CPPFLAGS += -fPIC -D_GNU_SOURCE
EOF
    
    write_success "Build configuration created for: ${architectures[*]}"
    
    if [[ -n "$cpp_scanner_file" ]]; then
        write_info "C++ scanner: $cpp_scanner_file"
    fi
    if [[ -n "$python_scanner_file" ]]; then
        write_info "Python scanner: $python_scanner_file"
    fi
}

# Invoke NDK build
invoke_ndk_build() {
    local ndk_build="$1"
    
    write_info "Starting NDK compilation..."
    write_info "This may take several minutes..."
    
    if "$ndk_build" NDK_PROJECT_PATH=. APP_BUILD_SCRIPT=Android.mk; then
        write_success "NDK compilation completed!"
        return 0
    else
        write_error "NDK build failed with exit code $?"
        return 1
    fi
}

# Copy libraries
copy_libraries() {
    local architectures=("$@")
    local output_dir="$LIBS_OUTPUT_DIR"
    local ndk_path="$ANDROID_NDK_PATH"
    
    local obj_dir="obj/local"
    local all_libs=(
        "libtree-sitter.so"
        "libtree-sitter-java.so"
        "libtree-sitter-cpp.so"
        "libtree-sitter-python.so"
    )
    
    local copied_count=0
    local total_size=0
    
    # Check for readelf
    local readelf_paths=(
        "$ndk_path/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-readelf"
        "$ndk_path/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-readelf"
        "$(which readelf 2>/dev/null)"
    )
    
    local readelf=""
    for path in "${readelf_paths[@]}"; do
        if [[ -n "$path" && -x "$path" ]]; then
            readelf="$path"
            break
        fi
    done
    
    if [[ -z "$readelf" ]]; then
        write_warning "readelf not found, skipping alignment validation"
    fi
    
    for arch in "${architectures[@]}"; do
        local source_dir="$obj_dir/$arch"
        local target_dir="$output_dir/$arch"
        
        if [[ ! -d "$source_dir" ]]; then
            write_error "No build output for $arch"
            continue
        fi
        
        mkdir -p "$target_dir"
        write_info "Processing $arch..."
        
        for lib in "${all_libs[@]}"; do
            local source_file="$source_dir/$lib"
            local target_file="$target_dir/$lib"
            
            if [[ -f "$source_file" ]]; then
                cp "$source_file" "$target_file"
                local size=$(stat -f%z "$source_file" 2>/dev/null || stat -c%s "$source_file" 2>/dev/null || echo "0")
                ((total_size += size))
                
                # Check alignment
                local alignment_status="Unknown"
                if [[ -n "$readelf" ]]; then
                    if "$readelf" -l "$source_file" 2>/dev/null | grep -q "align.*0x4000"; then
                        alignment_status="16KB aligned"
                    else
                        alignment_status="Standard alignment"
                    fi
                fi
                
                local size_kb=$((size / 1024))
                write_success "$lib (${size_kb} KB) - $alignment_status"
                ((copied_count++))
            else
                write_warning "$lib not built"
            fi
        done
    done
    
    echo "$copied_count:$total_size"
}

# Copy to app
copy_to_app() {
    local architectures=("$@")
    local output_dir="$LIBS_OUTPUT_DIR"
    
    local app_jni_libs="$SCRIPT_DIR/../app/src/main/jniLibs"
    write_info "Copying libraries to app jniLibs..."
    
    for arch in "${architectures[@]}"; do
        local source_dir="$output_dir/$arch"
        local target_dir="$app_jni_libs/$arch"
        
        if [[ -d "$source_dir" ]]; then
            mkdir -p "$target_dir"
            cp "$source_dir"/* "$target_dir"/
            write_success "Copied $arch libraries to app"
        fi
    done
}

#endregion

#region Main Script

main() {
    display_header
    
    write_info "Output Directory: $LIBS_OUTPUT_DIR"
    write_info "Build Directory: $BUILD_DIR"
    echo ""
    
    # Parse arguments
    parse_arguments "$@"
    
    # Interactive mode
    if [[ "$NON_INTERACTIVE" != true ]]; then
        # Step 1: NDK Path
        write_step_header "Android NDK Configuration" "Step 1"
        echo -e "${YELLOW}Download from: https://developer.android.com/ndk/downloads${NC}"
        echo ""
        
        while ! test_android_ndk "$ANDROID_NDK_PATH"; do
            read -p "Enter Android NDK path (e.g., /opt/android-ndk-r26d): " ANDROID_NDK_PATH
            if ! test_android_ndk "$ANDROID_NDK_PATH"; then
                write_error "Invalid NDK path or ndk-build not found"
            fi
        done
        write_success "Valid NDK found at: $ANDROID_NDK_PATH"
        
        # Step 2: Architecture Selection
        write_step_header "Architecture Selection" "Step 2"
        if [[ ${#ARCHITECTURES[@]} -eq 0 ]]; then
            get_user_architecture_choice
        fi
        
        # Step 3: Confirmation
        write_step_header "Build Configuration Summary" "Step 3"
        echo -e "${WHITE}NDK Path: $ANDROID_NDK_PATH${NC}"
        echo -e "${WHITE}Architectures: ${ARCHITECTURES[*]}${NC}"
        echo -e "${WHITE}Output Path: $LIBS_OUTPUT_DIR${NC}"
        echo ""
        
        read -p "Proceed with build? [Y/n]: " confirm
        if [[ "$confirm" == "n" || "$confirm" == "N" ]]; then
            write_warning "Build cancelled by user"
            exit 0
        fi
    fi
    
    # Validate parameters
    if ! test_android_ndk "$ANDROID_NDK_PATH"; then
        write_error "Invalid Android NDK path!"
        if [[ "$NON_INTERACTIVE" == true ]]; then
            echo -e "${YELLOW}Usage: $0 --ndk-path '/opt/android-ndk-r26d' --architectures 'arm64-v8a' --non-interactive${NC}"
        fi
        exit 1
    fi
    
    if [[ ${#ARCHITECTURES[@]} -eq 0 ]]; then
        ARCHITECTURES=("arm64-v8a")
        write_warning "No architectures specified, defaulting to arm64-v8a"
    fi
    
    local ndk_build="$ANDROID_NDK_PATH/ndk-build"
    
    # Step 4: Cleanup
    write_step_header "Cleanup Previous Builds" "Step 4"
    clear_build_files
    
    # Create directories
    mkdir -p "$BUILD_DIR" "$LIBS_OUTPUT_DIR"
    
    # Step 5: Download Sources
    write_step_header "Download Tree-sitter Sources" "Step 5"
    download_tree_sitter_sources "$BUILD_DIR"
    
    # Step 6: Build Configuration
    write_step_header "Create Build Configuration" "Step 6"
    create_build_configuration "${ARCHITECTURES[@]}"
    
    # Step 7: Compilation
    write_step_header "NDK Compilation" "Step 7"
    if ! invoke_ndk_build "$ndk_build"; then
        write_error "NDK build failed"
        exit 1
    fi
    
    # Step 8: Copy Libraries
    write_step_header "Copy Libraries" "Step 8"
    local copy_result
    copy_result=$(copy_libraries "${ARCHITECTURES[@]}")
    local copied_count="${copy_result%%:*}"
    local total_size="${copy_result##*:}"
    
    # Step 9: Copy to App
    write_step_header "Copy to Application" "Step 9"
    copy_to_app "${ARCHITECTURES[@]}"
    
    # Success Summary
    echo ""
    echo -e "${GREEN}=================================================================${NC}"
    echo -e "${GREEN}   BUILD COMPLETED SUCCESSFULLY!${NC}"
    echo -e "${GREEN}=================================================================${NC}"
    echo ""
    echo -e "${CYAN}Build Statistics:${NC}"
    echo -e "${WHITE}   Libraries built: $copied_count${NC}"
    echo -e "${WHITE}   Total size: $((total_size / 1024 / 1024)) MB${NC}"
    echo -e "${WHITE}   Architectures: ${ARCHITECTURES[*]}${NC}"
    echo -e "${WHITE}   Version: v$SCRIPT_VERSION${NC}"
    echo ""
    echo -e "${CYAN}Output Locations:${NC}"
    echo -e "${WHITE}   Script libs: $LIBS_OUTPUT_DIR${NC}"
    echo -e "${WHITE}   App jniLibs: ../app/src/main/jniLibs${NC}"
    echo ""
    echo -e "${YELLOW}Next Steps:${NC}"
    echo -e "${WHITE}   1. cd ..${NC}"
    echo -e "${WHITE}   2. ./gradlew assembleDebug${NC}"
    echo -e "${WHITE}   3. Test TreeSitter-powered auto-completion!${NC}"
    echo ""
    
    # Return to script root
    cd "$SCRIPT_DIR"
    
    # Cleanup option
    echo ""
    read -p "Delete temporary build files to save space? [Y/n]: " cleanup
    if [[ "$cleanup" != "n" && "$cleanup" != "N" ]]; then
        if [[ -d "$BUILD_DIR" ]]; then
            if rm -rf "$BUILD_DIR" 2>/dev/null; then
                write_success "Temporary build files cleaned up"
            else
                write_warning "Failed to clean up build files"
                echo -e "${YELLOW}You can manually delete: $BUILD_DIR${NC}"
            fi
        fi
    else
        write_info "Temporary build files preserved at: $BUILD_DIR"
    fi
    
    echo ""
    echo -e "${CYAN}=================================================================${NC}"
    echo -e "${GREEN}   Tree-sitter Builder v$SCRIPT_VERSION - Process Complete${NC}"
    echo -e "${CYAN}=================================================================${NC}"
}

# Handle errors
trap 'echo ""; echo -e "${RED}=================================================================${NC}"; echo -e "${RED}   BUILD FAILED!${NC}"; echo -e "${RED}=================================================================${NC}"; write_error "Error occurred at line $LINENO"; cd "$SCRIPT_DIR"; exit 1' ERR

# Run main function with all arguments
main "$@"

#endregion 