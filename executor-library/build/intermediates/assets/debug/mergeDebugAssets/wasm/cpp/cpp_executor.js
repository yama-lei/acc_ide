/**
 * C++ WASM Executor - wasm-clang
 */

let api = null;
let isReady = false;

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
                return await WebAssembly.compile(bytes);
            },
            hostWrite: (text) => {
                console.log('[Compiler Output]', text);
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
    let executionStartTime = 0;
    let executionEndTime = 0;
    let isExecuting = false;
    let compilationSucceeded = false;
    let originalHostWrite = null;
    
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
            
            if (cleanText.includes('> test.wasm')) {
                compilationSucceeded = true;
                isExecuting = true;
                executionStartTime = performance.now();
                return;
            }
            
            if (!isExecuting) {
                allCompilerOutput += text;
            }
            
            if (isExecuting && cleanText.match(/\(\d+\.\d+s\/\d+\.\d+s\)/)) {
                executionEndTime = performance.now();
                isExecuting = false;
                return;
            }
            
            if (isExecuting) {
                programOutput += text;
            }
        };
        
        console.log('Calling api.compileLinkRun()...');
        document.getElementById('status').textContent = 'Compiling and linking...';
        
        await api.compileLinkRun(sourceCode);
        
        if (originalHostWrite) {
            api.hostWrite = originalHostWrite;
        }
        
        console.log('Compilation and execution completed successfully!');
        document.getElementById('status').textContent = 'Done!';
        
        const cleanOutput = programOutput.replace(/\x1b\[[0-9;]*m/g, '').trim();
        const executionTimeMs = executionEndTime > 0 
            ? Math.round(executionEndTime - executionStartTime)
            : 0;
        
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
            errorMessage = errorMessage.replace(/\x1b\[[0-9;]*m/g, '');
            
            const match = error.message.match(/code (\d+)/);
            const exitCode = match ? parseInt(match[1]) : 1;
            
            errorMessage = `Runtime Error (Exit Code: ${exitCode})`;
            
            if (programOutput.trim()) {
                const cleanOutput = programOutput.replace(/\x1b\[[0-9;]*m/g, '').trim();
                errorMessage += '\n\nProgram Output:\n' + cleanOutput;
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
