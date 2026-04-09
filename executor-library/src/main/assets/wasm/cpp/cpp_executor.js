/**
 * C++ WASM Executor - wasm-clang
 */

let api = null;
let isReady = false;
let allRuntimeOutput = '';

/**
 * Load asset file using XMLHttpRequest
 */
async function loadAssetFile(filename) {
    return new Promise((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        xhr.open('GET', filename, true);
        xhr.responseType = 'arraybuffer';
        
        xhr.onload = () => {
            if (xhr.status === 200) {
                resolve(xhr.response);
            } else {
                reject(new Error(`Failed to load ${filename}: ${xhr.status}`));
            }
        };
        
        xhr.onerror = () => reject(new Error(`Network error loading ${filename}`));
        xhr.send();
    });
}

/**
 * Initialize C++ compiler
 * 
 * Performance Notes:
 * - Initial load: ~100ms (sysroot untar)
 * - First compilation: ~2.8s (load clang ~440ms + compile ~2s + load lld ~240ms + link ~100ms)
 * - Subsequent compilations: ~2.1s (clang/lld are cached, only compile+link time)
 * - The module cache in shared.js significantly improves repeated executions
 */
async function initializeCompiler() {
    try {
        console.log('Initializing wasm-clang with shared.js API...');
        document.getElementById('status').textContent = 'Loading compiler...';
        
        api = new API({
            readBuffer: async (filename) => {
                console.log(`Loading ${filename}...`);
                return await loadAssetFile(filename);
            },
            compileStreaming: async (filename) => {
                console.log(`Compiling ${filename}...`);
                const bytes = await loadAssetFile(filename);
                // WebAssembly.compile is called once per module (clang, lld, memfs)
                // Results are cached in API.moduleCache for reuse
                return await WebAssembly.compile(bytes);
            },
            hostWrite: (text) => {
                console.log('[Compiler Output]', text);
                allRuntimeOutput += text;
                if (typeof AndroidBridge !== 'undefined') {
                    AndroidBridge.onOutput(text);
                }
            },
            clang: 'clang',
            lld: 'lld',
            memfs: 'memfs',
            sysroot: 'sysroot.tar',
            showTiming: true
        });
        
        console.log('Waiting for API initialization...');
        document.getElementById('status').textContent = 'Initializing compiler (loading memfs and sysroot)...';
        
        await api.ready;
        
        console.log('C++ compiler ready!');
        document.getElementById('status').textContent = 'C++ compiler ready with full STL support!';
        
        isReady = true;
        
        if (typeof AndroidBridge !== 'undefined') {
            AndroidBridge.onWasmReady();
        }
        
    } catch (error) {
        console.error('Failed to initialize compiler:', error);
        document.getElementById('status').textContent = 'Error: ' + error.message;
        
        if (typeof AndroidBridge !== 'undefined') {
            AndroidBridge.onWasmError(error.toString());
        }
    }
}

/**
 * Compile and run C++ code
 */
async function compileAndRun(sourceCode, input) {
    if (!isReady || !api) {
        const error = 'Compiler not ready. Please wait for initialization.';
        console.error(error);
        if (typeof AndroidBridge !== 'undefined') {
            AndroidBridge.onExecutionError(error);
        }
        return;
    }

    let programOutput = '';
    let allCompilerOutput = '';
    let executionTimeMs = 0;
    let isExecuting = false;
    let compilationSucceeded = false;
    let originalHostWrite = null;
    const needsCanvasLibrary = /#\s*include\s*[<\"]canvas\.h[>\"]/.test(sourceCode);

    allRuntimeOutput = '';
    
    try {
        console.log('Starting compilation...');
        document.getElementById('status').textContent = 'Compiling...';
        
        if (input && input.trim()) {
            console.log('Setting stdin input:', input);
            api.memfs.setStdinStr(input);
        }
        
        originalHostWrite = api.hostWrite;
        api.hostWrite = (text) => {
            console.log('[Compiler]', text);
            
            const cleanText = text.replace(/\x1b\[[0-9;]*m/g, '');
            
            // 检测编译成功标记
            if (cleanText.includes('> test.wasm')) {
                compilationSucceeded = true;
                isExecuting = true;
                return;
            }
            
            if (!isExecuting) {
                allCompilerOutput += text;
            }
            
            // (instantiate_time/execution_time)
            if (isExecuting && cleanText.match(/\(\d+\.\d+s\/\d+\.\d+s\)/)) {
                const timeMatch = cleanText.match(/\((\d+\.\d+)s\/(\d+\.\d+)s\)/);
                if (timeMatch) {
                    executionTimeMs = Math.round(parseFloat(timeMatch[2]) * 1000);
                    console.log('Extracted execution time:', executionTimeMs, 'ms');
                }
                isExecuting = false;
                return;
            }
            
            if (isExecuting) {
                programOutput += text;
            }
        };
        
        console.log('Calling api.compileLinkRun()...');
        document.getElementById('status').textContent = 'Compiling and linking...';
        
        await api.compileLinkRun(sourceCode, {
            extraLibraries: needsCanvasLibrary ? ['-lcanvas'] : []
        });
        
        if (originalHostWrite) {
            api.hostWrite = originalHostWrite;
        }
        
        console.log('Compilation and execution completed successfully!');
        document.getElementById('status').textContent = 'Done!';
        
        const cleanOutput = programOutput.replace(/\x1b\[[0-9;]*m/g, '').trim();
        
        if (typeof AndroidBridge !== 'undefined') {
            AndroidBridge.onOutput(`[EXEC_TIME_MS:${executionTimeMs}]`);
            
            if (cleanOutput) {
                AndroidBridge.onOutput(cleanOutput + '\n');
            }
            
            AndroidBridge.onExecutionComplete(0);
        }
        
    } catch (error) {
        console.error('Compilation/execution error:', error);
        console.log('Compilation succeeded:', compilationSucceeded);
        
        if (api && originalHostWrite) {
            api.hostWrite = originalHostWrite;
        }
        
        let errorMessage = error.message || error.toString();
        
        if (!compilationSucceeded) {
            console.log('Compilation failed, treating as CE');
            const cleanCompilerOutput = allCompilerOutput.replace(/\x1b\[[0-9;]*m/g, '');
            errorMessage = 'Compilation Error:\n' + cleanCompilerOutput;
        } else {
            console.log('Compilation succeeded but execution failed, treating as RE');
            
            const match = error.message.match(/code (\d+)/);
            const exitCode = match ? parseInt(match[1]) : 1;
            
            const cleanOutput = allRuntimeOutput.replace(/\x1b\[[0-9;]*m/g, '').trim();
            const hasErrorInOutput = cleanOutput.length > 0 && 
                                    (cleanOutput.toLowerCase().includes('error') || 
                                     cleanOutput.toLowerCase().includes('runtimeerror'));
            
            console.log('allRuntimeOutput length:', allRuntimeOutput.length);
            console.log('cleanOutput length:', cleanOutput.length);
            console.log('hasErrorInOutput:', hasErrorInOutput);
            
            if (hasErrorInOutput) {
                errorMessage = `Runtime Error (Exit Code: ${exitCode})`;
            } else {
                let detailedError = `Runtime Error: ${error.message || error.toString()}`;
                
                if (error.stack) {
                    detailedError += '\n' + error.stack;
                }
                
                if (cleanOutput) {
                    detailedError += '\n\nProgram Output:\n' + cleanOutput;
                }
                
                errorMessage = `Runtime Error (Exit Code: ${exitCode})\n\n${detailedError}`;
            }
        }
        
        document.getElementById('status').textContent = 'Error';
        
        if (typeof AndroidBridge !== 'undefined') {
            AndroidBridge.onExecutionError(errorMessage);
        }
    }
}

window.addEventListener('load', function() {
    console.log('Page loaded, initializing C++ compiler with shared.js API...');
    initializeCompiler();
});
