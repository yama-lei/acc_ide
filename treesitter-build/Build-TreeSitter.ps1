#!/usr/bin/env powershell
<#
.SYNOPSIS
    Tree-sitter Library Builder for Android
    
.DESCRIPTION
    Builds Tree-sitter libraries with proper 16KB alignment for Android devices.
    Supports C++, Java, and Python parsers with optimized configuration.
    
.PARAMETER AndroidNdkPath
    Path to Android NDK (e.g., C:\android-ndk-r26d)
    
.PARAMETER Architectures
    Array of architectures to build (@("arm64-v8a", "armeabi-v7a", "x86_64", "x86"))
    
.PARAMETER CustomCMakePath
    Custom CMake executable path (optional)
    
.PARAMETER NonInteractive
    Run in non-interactive mode (requires all parameters)
    
.EXAMPLE
    .\Build-TreeSitter-v0.6.ps1
    
.EXAMPLE
    .\Build-TreeSitter-v0.6.ps1 -AndroidNdkPath "C:\android-ndk-r26d" -Architectures @("arm64-v8a") -NonInteractive
    
.NOTES
    Version: 0.7
    Author: META XIAO
    Date: 2025-08-18
    
    Requirements:
    - Android NDK 27.0+
    - PowerShell 5.0+
    - Internet connection for source downloads
    
    Output:
    - Libraries: treesitter_build/libs/{arch}/
    - App Copy: app/src/main/jniLibs/{arch}/
#>

param(
    [Parameter(HelpMessage="Path to Android NDK")]
    [string]$AndroidNdkPath = "",
    
    [Parameter(HelpMessage="Architectures to build")]
    [string[]]$Architectures = @(),
    
    [Parameter(HelpMessage="Custom CMake path")]
    [string]$CustomCMakePath = "",
    
    [Parameter(HelpMessage="Run without user interaction")]
    [switch]$NonInteractive = $false
)

# Script metadata
$Script:Version = "0.7"
$Script:BuildDate = Get-Date -Format "yyyy-MM-dd"

# Display header
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "   Tree-sitter Library Builder v$($Script:Version)" -ForegroundColor Green
Write-Host "   Optimized for Android with 16KB Page Alignment" -ForegroundColor Green
Write-Host "   Build Date: $($Script:BuildDate)" -ForegroundColor Gray
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host ""

# Get script root directory
$ScriptRoot = $PSScriptRoot
$LibsOutputDir = Join-Path $ScriptRoot "libs"
$BuildDir = Join-Path $ScriptRoot "build_temp"

Write-Host "INFO: Output Directory: $LibsOutputDir" -ForegroundColor Cyan
Write-Host "INFO: Build Directory: $BuildDir" -ForegroundColor Cyan
Write-Host ""

#region Helper Functions

function Write-StepHeader {
    param([string]$Title, [string]$Step)
    Write-Host ""
    Write-Host "=== $Step : $Title ===" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Message)
    Write-Host "SUCCESS: $Message" -ForegroundColor Green
}

function Write-Warning {
    param([string]$Message)
    Write-Host "WARNING: $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "ERROR: $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "INFO: $Message" -ForegroundColor Cyan
}

function Clear-BuildFiles {
    param([string]$BuildDir, [string]$OutputDir)
    
    Write-Info "Cleaning previous build files..."
    
    $CleanupItems = @(
        @{ Path = $BuildDir; Name = "Build directory" },
        @{ Path = $OutputDir; Name = "Output directory" },
        @{ Path = (Join-Path $ScriptRoot "..\app\src\main\jniLibs"); Name = "App jniLibs directory" }
    )
    
    foreach ($item in $CleanupItems) {
        if (Test-Path $item.Path) {
            try {
                Remove-Item $item.Path -Recurse -Force -ErrorAction Stop
                Write-Success "Removed $($item.Name)"
            } catch {
                Write-Warning "Failed to remove $($item.Name): $($_.Exception.Message)"
            }
        }
    }
    
    Write-Success "Cleanup completed"
}

function Test-AndroidNdk {
    param([string]$NdkPath)
    
    if ([string]::IsNullOrEmpty($NdkPath) -or !(Test-Path $NdkPath)) {
        return $false
    }
    
    $ndkBuild = Join-Path $NdkPath "ndk-build.cmd"
    return (Test-Path $ndkBuild)
}

function Get-UserArchitectureChoice {
    $ArchChoices = @{
        "1" = @{ Archs = @("arm64-v8a"); Desc = "arm64-v8a (64-bit ARM - Modern phones)" }
        "2" = @{ Archs = @("armeabi-v7a"); Desc = "armeabi-v7a (32-bit ARM - Older devices)" }
        "3" = @{ Archs = @("x86_64"); Desc = "x86_64 (64-bit x86 - Emulators)" }
        "4" = @{ Archs = @("x86"); Desc = "x86 (32-bit x86 - Old emulators)" }
        "5" = @{ Archs = @("arm64-v8a", "armeabi-v7a", "x86_64", "x86"); Desc = "All architectures" }
    }
    
    Write-Host "Available architectures:" -ForegroundColor Green
    foreach ($key in $ArchChoices.Keys | Sort-Object) {
        Write-Host "  $key. $($ArchChoices[$key].Desc)" -ForegroundColor Yellow
    }
    Write-Host ""
    
    do {
        $choice = Read-Host "Select architectures to build (1-5)"
        if ($ArchChoices.ContainsKey($choice)) {
            Write-Success "Selected: $($ArchChoices[$choice].Desc)"
            return $ArchChoices[$choice].Archs
        } else {
            Write-Error "Invalid choice, please enter 1-5"
        }
    } while ($true)
}

function Download-TreeSitterSources {
    param([string]$BuildDir)
    
    $Sources = @{
        "tree-sitter" = @{
            Url = "https://github.com/tree-sitter/tree-sitter/archive/refs/tags/v0.22.6.zip"
            ExtractName = "tree-sitter-0.22.6"
            FinalName = "tree-sitter"
        }
        "tree-sitter-java" = @{
            Url = "https://github.com/tree-sitter/tree-sitter-java/archive/refs/heads/master.zip"
            ExtractName = "tree-sitter-java-master"
            FinalName = "tree-sitter-java"
        }
        "tree-sitter-cpp" = @{
            Url = "https://github.com/tree-sitter/tree-sitter-cpp/archive/refs/heads/master.zip"
            ExtractName = "tree-sitter-cpp-master"
            FinalName = "tree-sitter-cpp"
        }
        "tree-sitter-python" = @{
            Url = "https://github.com/tree-sitter/tree-sitter-python/archive/refs/heads/master.zip"
            ExtractName = "tree-sitter-python-master"
            FinalName = "tree-sitter-python"
        }
    }
    
    Set-Location $BuildDir
    
    foreach ($source in $Sources.GetEnumerator()) {
        Write-Info "Downloading $($source.Key)..."
        $zipFile = "$($source.Key).zip"
        
        try {
            Invoke-WebRequest -Uri $source.Value.Url -OutFile $zipFile -UseBasicParsing
            Expand-Archive -Path $zipFile -DestinationPath "." -Force
            Remove-Item $zipFile
            
            if (Test-Path $source.Value.ExtractName) {
                Rename-Item $source.Value.ExtractName $source.Value.FinalName
            }
            
            Write-Success "Downloaded and extracted $($source.Key)"
        } catch {
            throw "Failed to download $($source.Key): $($_.Exception.Message)"
        }
    }
    
    # Verify required files
    $RequiredFiles = @(
        "tree-sitter/lib/src/lib.c",
        "tree-sitter-java/src/parser.c",
        "tree-sitter-cpp/src/parser.c",
        "tree-sitter-python/src/parser.c"
    )
    
    foreach ($file in $RequiredFiles) {
        if (!(Test-Path $file)) {
            throw "Required source file not found: $file"
        }
    }
    
    Write-Success "All source files verified"
}

function Create-BuildConfiguration {
    param([string[]]$Architectures)
    
    # Detect scanner files
    $CppScannerFile = ""
    $CppScannerCandidates = @("tree-sitter-cpp/src/scanner.cc", "tree-sitter-cpp/src/scanner.c")
    foreach ($candidate in $CppScannerCandidates) {
        if (Test-Path $candidate) {
            $CppScannerFile = $candidate
            break
        }
    }
    
    $PythonScannerFile = ""
    if (Test-Path "tree-sitter-python/src/scanner.c") {
        $PythonScannerFile = "tree-sitter-python/src/scanner.c"
    }
    
    # Build source lists
    $CppSources = "tree-sitter-cpp/src/parser.c"
    if ($CppScannerFile) { $CppSources += " $CppScannerFile" }
    
    $PythonSources = "tree-sitter-python/src/parser.c"
    if ($PythonScannerFile) { $PythonSources += " $PythonScannerFile" }
    
    # Create Android.mk
    $AndroidMk = @"
LOCAL_PATH := `$(call my-dir)

# Global 16KB alignment flags for Android 15+ compatibility
ALIGNMENT_FLAGS := -Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384 -Wl,-z,relro -Wl,-z,now

# Tree-sitter core library
include `$(CLEAR_VARS)
LOCAL_MODULE := tree-sitter
LOCAL_SRC_FILES := tree-sitter/lib/src/lib.c
LOCAL_C_INCLUDES := tree-sitter/lib/include tree-sitter/lib/src
LOCAL_EXPORT_C_INCLUDES := tree-sitter/lib/include
LOCAL_CFLAGS := -std=c11 -O2 -DTREE_SITTER_HIDE_SYMBOLS -fPIC -D_GNU_SOURCE
LOCAL_LDFLAGS := `$(ALIGNMENT_FLAGS) -s
LOCAL_LDLIBS := `$(ALIGNMENT_FLAGS)
include `$(BUILD_SHARED_LIBRARY)

# Java grammar parser
include `$(CLEAR_VARS)
LOCAL_MODULE := tree-sitter-java
LOCAL_SRC_FILES := tree-sitter-java/src/parser.c
LOCAL_C_INCLUDES := tree-sitter/lib/include
LOCAL_SHARED_LIBRARIES := tree-sitter
LOCAL_CFLAGS := -std=c11 -O2 -fPIC -D_GNU_SOURCE
LOCAL_LDFLAGS := `$(ALIGNMENT_FLAGS) -s
LOCAL_LDLIBS := `$(ALIGNMENT_FLAGS)
include `$(BUILD_SHARED_LIBRARY)

# C++ grammar parser
include `$(CLEAR_VARS)
LOCAL_MODULE := tree-sitter-cpp
LOCAL_SRC_FILES := $CppSources
LOCAL_C_INCLUDES := tree-sitter/lib/include
LOCAL_SHARED_LIBRARIES := tree-sitter
LOCAL_CFLAGS := -std=c11 -O2 -fPIC -D_GNU_SOURCE
LOCAL_CPPFLAGS := -std=c++11 -O2 -fPIC -D_GNU_SOURCE
LOCAL_LDFLAGS := `$(ALIGNMENT_FLAGS) -s
LOCAL_LDLIBS := `$(ALIGNMENT_FLAGS)
include `$(BUILD_SHARED_LIBRARY)

# Python grammar parser
include `$(CLEAR_VARS)
LOCAL_MODULE := tree-sitter-python
LOCAL_SRC_FILES := $PythonSources
LOCAL_C_INCLUDES := tree-sitter/lib/include
LOCAL_SHARED_LIBRARIES := tree-sitter
LOCAL_CFLAGS := -std=c11 -O2 -fPIC -D_GNU_SOURCE
LOCAL_LDFLAGS := `$(ALIGNMENT_FLAGS) -s
LOCAL_LDLIBS := `$(ALIGNMENT_FLAGS)
include `$(BUILD_SHARED_LIBRARY)
"@
    
    Set-Content -Path "Android.mk" -Value $AndroidMk -Encoding UTF8
    
    # Create Application.mk
    $ArchString = $Architectures -join " "
    $ApplicationMk = @"
# Tree-sitter Builder v$($Script:Version) - Android Configuration
# Enhanced NDK configuration for 16KB alignment

NDK_TOOLCHAIN_VERSION := clang

APP_PLATFORM := android-21
APP_ABI := $ArchString
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
"@
    
    Set-Content -Path "Application.mk" -Value $ApplicationMk -Encoding UTF8
    
    Write-Success "Build configuration created for: $($Architectures -join ', ')"
    
    if ($CppScannerFile) {
        Write-Info "C++ scanner: $CppScannerFile"
    }
    if ($PythonScannerFile) {
        Write-Info "Python scanner: $PythonScannerFile"
    }
}

function Invoke-NdkBuild {
    param([string]$NdkBuild)
    
    Write-Info "Starting NDK compilation..."
    Write-Info "This may take several minutes..."
    
    try {
        & $NdkBuild NDK_PROJECT_PATH=. APP_BUILD_SCRIPT=Android.mk
        if ($LASTEXITCODE -eq 0) {
            Write-Success "NDK compilation completed!"
            return $true
        } else {
            Write-Error "NDK build failed with exit code $LASTEXITCODE"
            return $false
        }
    } catch {
        Write-Error "NDK build exception: $($_.Exception.Message)"
        return $false
    }
}

function Copy-Libraries {
    param([string[]]$Architectures, [string]$OutputDir, [string]$NdkPath)
    
    $ObjDir = "obj\local"
    $AllLibs = @(
        "libtree-sitter.so",
        "libtree-sitter-java.so", 
        "libtree-sitter-cpp.so",
        "libtree-sitter-python.so"
    )
    
    $CopiedCount = 0
    $TotalSize = 0
    
    # Get readelf for alignment checking
    $ReadElf = Join-Path $NdkPath "toolchains\llvm\prebuilt\windows-x86_64\bin\llvm-readelf.exe"
    $HasReadElf = (Test-Path $ReadElf)
    
    if (-not $HasReadElf) {
        Write-Warning "llvm-readelf not found, skipping alignment validation"
    }
    
    foreach ($arch in $Architectures) {
        $SourceDir = Join-Path $ObjDir $arch
        $TargetDir = Join-Path $OutputDir $arch
        
        if (!(Test-Path $SourceDir)) {
            Write-Error "No build output for $arch"
            continue
        }
        
        if (!(Test-Path $TargetDir)) {
            New-Item -ItemType Directory -Path $TargetDir -Force | Out-Null
        }
        
        Write-Info "Processing $arch..."
        
        foreach ($lib in $AllLibs) {
            $sourceFile = Join-Path $SourceDir $lib
            $targetFile = Join-Path $TargetDir $lib
            
            if (Test-Path $sourceFile) {
                Copy-Item $sourceFile $targetFile -Force
                $size = (Get-Item $sourceFile).Length
                $TotalSize += $size
                
                # Check alignment
                $alignmentStatus = "Unknown"
                if ($HasReadElf) {
                    try {
                        $output = & $ReadElf -l $sourceFile 2>$null | Select-String "LOAD"
                        $alignmentFound = $false
                        
                        foreach ($line in $output) {
                            if ($line -match "align\s+0x4000") {
                                $alignmentFound = $true
                                break
                            }
                        }
                        
                        $alignmentStatus = if ($alignmentFound) { "16KB aligned" } else { "Standard alignment" }
                    } catch {
                        $alignmentStatus = "Check failed"
                    }
                }
                
                Write-Success "$lib ($([math]::Round($size/1KB, 1)) KB) - $alignmentStatus"
                $CopiedCount++
            } else {
                Write-Warning "$lib not built"
            }
        }
    }
    
    return @{ Count = $CopiedCount; Size = $TotalSize }
}

function Copy-ToApp {
    param([string[]]$Architectures, [string]$OutputDir)
    
    $AppJniLibs = Join-Path $ScriptRoot "..\app\src\main\jniLibs"
    Write-Info "Copying libraries to app jniLibs..."
    
    foreach ($arch in $Architectures) {
        $SourceDir = Join-Path $OutputDir $arch
        $TargetDir = Join-Path $AppJniLibs $arch
        
        if (Test-Path $SourceDir) {
            if (!(Test-Path $TargetDir)) {
                New-Item -ItemType Directory -Path $TargetDir -Force | Out-Null
            }
            
            Copy-Item "$SourceDir\*" $TargetDir -Force
            Write-Success "Copied $arch libraries to app"
        }
    }
}

#endregion

#region Main Script

try {
    # Interactive mode
    if (-not $NonInteractive) {
        # Step 1: NDK Path
        Write-StepHeader "Android NDK Configuration" "Step 1"
        Write-Host "Download from: https://developer.android.com/ndk/downloads" -ForegroundColor Yellow
        Write-Host ""
        
        while (-not (Test-AndroidNdk $AndroidNdkPath)) {
            $AndroidNdkPath = Read-Host "Enter Android NDK path (e.g., C:\android-ndk-r26d)"
            if (-not (Test-AndroidNdk $AndroidNdkPath)) {
                Write-Error "Invalid NDK path or ndk-build.cmd not found"
            }
        }
        Write-Success "Valid NDK found at: $AndroidNdkPath"
        
        # Step 2: Architecture Selection
        Write-StepHeader "Architecture Selection" "Step 2"
        if ($Architectures.Count -eq 0) {
            $Architectures = Get-UserArchitectureChoice
        }
        
        # Step 3: Confirmation
        Write-StepHeader "Build Configuration Summary" "Step 3"
        Write-Host "NDK Path: $AndroidNdkPath" -ForegroundColor White
        Write-Host "Architectures: $($Architectures -join ', ')" -ForegroundColor White
        Write-Host "Output Path: $LibsOutputDir" -ForegroundColor White
        Write-Host ""
        
        $confirm = Read-Host "Proceed with build? [Y/n]"
        if ($confirm -eq "n" -or $confirm -eq "N") {
            Write-Warning "Build cancelled by user"
            exit 0
        }
    }
    
    # Validate parameters
    if (-not (Test-AndroidNdk $AndroidNdkPath)) {
        Write-Error "Invalid Android NDK path!"
        if ($NonInteractive) {
            Write-Host "Usage: .\Build-TreeSitter-v0.6.ps1 -AndroidNdkPath 'C:\android-ndk-r26d' -Architectures @('arm64-v8a') -NonInteractive" -ForegroundColor Yellow
        }
        exit 1
    }
    
    if ($Architectures.Count -eq 0) {
        $Architectures = @("arm64-v8a")
        Write-Warning "No architectures specified, defaulting to arm64-v8a"
    }
    
    $NdkBuild = Join-Path $AndroidNdkPath "ndk-build.cmd"
    
    # Step 4: Cleanup
    Write-StepHeader "Cleanup Previous Builds" "Step 4"
    Clear-BuildFiles -BuildDir $BuildDir -OutputDir $LibsOutputDir
    
    # Create directories
    New-Item -ItemType Directory -Path $BuildDir -Force | Out-Null
    New-Item -ItemType Directory -Path $LibsOutputDir -Force | Out-Null
    
    # Step 5: Download Sources
    Write-StepHeader "Download Tree-sitter Sources" "Step 5"
    Download-TreeSitterSources -BuildDir $BuildDir
    
    # Step 6: Build Configuration
    Write-StepHeader "Create Build Configuration" "Step 6"
    Create-BuildConfiguration -Architectures $Architectures
    
    # Step 7: Compilation
    Write-StepHeader "NDK Compilation" "Step 7"
    $buildSuccess = Invoke-NdkBuild -NdkBuild $NdkBuild
    
    if (-not $buildSuccess) {
        throw "NDK build failed"
    }
    
    # Step 8: Copy Libraries
    Write-StepHeader "Copy Libraries" "Step 8"
    $copyResult = Copy-Libraries -Architectures $Architectures -OutputDir $LibsOutputDir -NdkPath $AndroidNdkPath
    
    # Step 9: Copy to App
    Write-StepHeader "Copy to Application" "Step 9"
    Copy-ToApp -Architectures $Architectures -OutputDir $LibsOutputDir
    
    # Success Summary
    Write-Host ""
    Write-Host "=================================================================" -ForegroundColor Green
    Write-Host "   BUILD COMPLETED SUCCESSFULLY!" -ForegroundColor Green
    Write-Host "=================================================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Build Statistics:" -ForegroundColor Cyan
    Write-Host "   Libraries built: $($copyResult.Count)" -ForegroundColor White
    Write-Host "   Total size: $([math]::Round($copyResult.Size/1MB, 2)) MB" -ForegroundColor White
    Write-Host "   Architectures: $($Architectures -join ', ')" -ForegroundColor White
    Write-Host "   Version: v$($Script:Version)" -ForegroundColor White
    Write-Host ""
    Write-Host "Output Locations:" -ForegroundColor Cyan
    Write-Host "   Script libs: $LibsOutputDir" -ForegroundColor White
    Write-Host "   App jniLibs: ..\app\src\main\jniLibs" -ForegroundColor White
    Write-Host ""
    Write-Host "Next Steps:" -ForegroundColor Yellow
    Write-Host "   1. cd .." -ForegroundColor White
    Write-Host "   2. ./gradlew assembleDebug" -ForegroundColor White
    Write-Host "   3. Test TreeSitter-powered auto-completion!" -ForegroundColor White
    Write-Host ""
    
} catch {
    Write-Host ""
    Write-Host "=================================================================" -ForegroundColor Red
    Write-Host "   BUILD FAILED!" -ForegroundColor Red
    Write-Host "=================================================================" -ForegroundColor Red
    Write-Error "Error: $($_.Exception.Message)"
    Set-Location $ScriptRoot
    exit 1
}

# Return to script root
Set-Location $ScriptRoot

# Cleanup option
Write-Host ""
$cleanup = Read-Host "Delete temporary build files to save space? [Y/n]"
if ($cleanup -ne "n" -and $cleanup -ne "N") {
    if (Test-Path $BuildDir) {
        try {
            Remove-Item $BuildDir -Recurse -Force -ErrorAction Stop
            Write-Success "Temporary build files cleaned up"
        } catch {
            Write-Warning "Failed to clean up build files: $($_.Exception.Message)"
            Write-Host "You can manually delete: $BuildDir" -ForegroundColor Yellow
        }
    }
} else {
    Write-Info "Temporary build files preserved at: $BuildDir"
}

Write-Host ""
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "   Tree-sitter Builder v$($Script:Version) - Process Complete" -ForegroundColor Green  
Write-Host "=================================================================" -ForegroundColor Cyan

#endregion