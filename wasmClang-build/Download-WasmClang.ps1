# Download wasm-clang files from GitHub
# 从 GitHub 下载 wasm-clang 文件

$ErrorActionPreference = "Stop"

Write-Host "Downloading wasm-clang files from GitHub..." -ForegroundColor Green
Write-Host "This will download ~50MB of files" -ForegroundColor Yellow
$baseUrl = "https://raw.githubusercontent.com/binji/wasm-clang/master/"
$targetDir = Join-Path $PSScriptRoot "..\app\src\main\assets\wasm"

if (!(Test-Path $targetDir)) {
    New-Item -ItemType Directory -Path $targetDir -Force | Out-Null
}

$files = @(
    "clang",
    "lld",
    "memfs",
    "sysroot.tar",
    "shared.js",   
    "shared_web.js",
    "web.js", 
    "worker.js"
)

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

