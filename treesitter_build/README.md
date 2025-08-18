# Tree-sitter Library Builder

**Version:** 0.7  
**Build Date:** 2025-08-18  
**Compatibility:** Android NDK 27.0+, Android 15+ (16KB alignment)

## 🎯 Overview

This script builds Tree-sitter libraries with proper 16KB page alignment for Android devices, ensuring compatibility with Android 15+ and Google Play validation requirements.

### 🔧 Supported Languages
- **C++** - Complete parser with scanner support
- **Java** - Full language parsing
- **Python** - Parser with scanner support  
- **Core** - Tree-sitter runtime library

## 🚀 Quick Start

### Interactive Mode (Recommended)
```powershell
.\Build-TreeSitter.ps1
```

### Non-Interactive Mode
```powershell
# Single architecture
.\Build-TreeSitter.ps1 -AndroidNdkPath "C:\android-ndk-r26d" -Architectures @("arm64-v8a") -NonInteractive

# Multiple architectures  
.\Build-TreeSitter.ps1 -AndroidNdkPath "C:\android-ndk-r26d" -Architectures @("arm64-v8a", "armeabi-v7a") -NonInteractive
```

## 📋 Requirements

- **Android NDK 27.0+** (Download: https://developer.android.com/ndk/downloads)
- **PowerShell 5.0+**
- **Internet connection** (for source downloads)
- **Disk space:** ~500MB during build

## 🏗️ Build Process

The script performs the following steps:

1. **🧹 Cleanup** - Removes previous build artifacts
2. **📥 Download** - Fetches Tree-sitter sources from GitHub
3. **⚙️ Configure** - Creates Android.mk and Application.mk with 16KB alignment
4. **🔨 Compile** - Builds libraries using Android NDK
5. **📦 Package** - Copies libraries to output directories
6. **✅ Validate** - Checks library alignment (if llvm-readelf available)

## 📁 Output Structure

```
treesitter_build/
├── Build-TreeSitter.ps1    # Main build script
├── README.md                     # This file
├── libs/                         # Output libraries
│   └── {architecture}/
│       ├── libtree-sitter.so
│       ├── libtree-sitter-cpp.so
│       ├── libtree-sitter-java.so
│       └── libtree-sitter-python.so
└── build_temp/                   # Temporary files (optional cleanup)
```

Libraries are also automatically copied to: `../app/src/main/jniLibs/{architecture}/`

## 🎮 Architecture Options

| Option | Architecture | Description |
|--------|-------------|-------------|
| 1 | arm64-v8a | 64-bit ARM - Modern phones (recommended) |
| 2 | armeabi-v7a | 32-bit ARM - Older devices |
| 3 | x86_64 | 64-bit x86 - Emulators |
| 4 | x86 | 32-bit x86 - Old emulators |
| 5 | All | All architectures |

## 🔧 Advanced Features

### 16KB Page Alignment
- Automatic 16KB alignment for Android 15+ compatibility
- Google Play validation compliance (November 2025)
- Enhanced performance on modern devices

### Build Optimization
- Optimized compiler flags (`-O2`, `-fPIC`)
- Security hardening (`-z relro`, `-z now`)
- Symbol stripping for smaller binaries

### Error Handling
- Comprehensive error checking
- Detailed progress reporting
- Automatic cleanup on failure

## 🎯 Integration with ACC IDE

After successful build:

1. **Compile app:**
   ```bash
   cd ..
   ./gradlew assembleDebug
   ```

2. **Test features:**
   - Open C++/Java/Python files in editor
   - Verify TreeSitter-powered auto-completion
   - Check variable/function scope detection

## 🐛 Troubleshooting

### Common Issues

**NDK not found:**
- Ensure Android NDK 27.0+ is installed
- Check ndk-build.cmd exists in NDK directory

**Download failures:**
- Check internet connection
- Verify GitHub is accessible
- Try running script again (auto-retry on some failures)

**Build failures:**
- Ensure sufficient disk space (>500MB)
- Check antivirus isn't blocking build process
- Try building single architecture first

**Alignment warnings:**
- Libraries are functional even without perfect 16KB alignment
- Warnings mainly affect Android 15+ optimization
- Most users won't experience issues

### Debug Information

The script provides detailed logging:
- ✅ SUCCESS: Operations completed successfully (green)
- ⚠️ WARNING: Issues that don't prevent completion (yellow)  
- ❌ ERROR: Critical failures (red)
- ℹ️ INFO: General information (cyan)
