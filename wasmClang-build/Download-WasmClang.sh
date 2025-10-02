#!/bin/bash
# Download wasm-clang files from GitHub
# 从 GitHub 下载 wasm-clang 文件

set -e

echo "Downloading wasm-clang files from GitHub..."
echo "This will download ~50MB of files"

BASE_URL="https://raw.githubusercontent.com/binji/wasm-clang/master/"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
TARGET_DIR="$SCRIPT_DIR/../app/src/main/assets/wasm"
mkdir -p "$TARGET_DIR"

FILES=(
    "clang"
    "lld"
    "memfs"
    "sysroot.tar"
    "shared.js"
    "worker.js"
)

for file in "${FILES[@]}"; do
    url="${BASE_URL}${file}"
    output="$TARGET_DIR/$file"
    
    echo "Downloading $file..."
    
    if curl -L -o "$output" "$url"; then
        size=$(du -h "$output" | cut -f1)
        echo "  Downloaded $file ($size)"
    else
        echo "  Failed to download $file"
    fi
done

echo ""
echo "Download complete!"
echo "Files saved to: $TARGET_DIR"
echo ""
echo "Note: These files are from https://github.com/binji/wasm-clang"
echo "License: Apache-2.0, LLVM, and vasm licenses apply"

