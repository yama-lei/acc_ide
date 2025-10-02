# Download wasm-clang files from GitHub
# 从 GitHub 下载 wasm-clang 文件

$ErrorActionPreference = "Stop"

Write-Host "Downloading wasm-clang files from GitHub..." -ForegroundColor Green
Write-Host "This will download ~50MB of files" -ForegroundColor Yellow

# 基础 URL
$baseUrl = "https://raw.githubusercontent.com/binji/wasm-clang/master/"

# 目标目录
$targetDir = Join-Path $PSScriptRoot "..\app\src\main\assets\wasm"

# 确保目录存在
if (!(Test-Path $targetDir)) {
    New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
}

# 需要下载的文件
$files = @(
    # 编译器和链接器 (50MB+)
    "clang",
    "lld",
    "memfs",
    "sysroot.tar",
    
    # 关键 JavaScript 文件
    "shared.js",        # WASI 核心实现、文件系统
    "shared_web.js",    # Web 特定工具
    "web.js",           # 主逻辑和 UI
    "worker.js"         # 编译 Worker
)

# 下载文件
foreach ($file in $files) {
    $url = $baseUrl + $file
    $output = Join-Path $targetDir $file
    
    Write-Host "Downloading $file..." -ForegroundColor Cyan
    
    try {
        Invoke-WebRequest -Uri $url -OutFile $output
        $size = (Get-Item $output).Length / 1MB
        Write-Host "  Downloaded $file ($([math]::Round($size, 2)) MB)" -ForegroundColor Green
    }
    catch {
        Write-Host "  Failed to download $file : $_" -ForegroundColor Red
    }
}

Write-Host "`nDownload complete!" -ForegroundColor Green
Write-Host "Files saved to: $targetDir" -ForegroundColor Cyan
Write-Host "`nNote: These files are from https://github.com/binji/wasm-clang" -ForegroundColor Yellow
Write-Host "License: Apache-2.0, LLVM, and vasm licenses apply" -ForegroundColor Yellow

