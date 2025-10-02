/**
 * Python WASM Executor - Pyodide
 */

let pyodide = null;

/**
 * Add ANSI color codes to Python traceback
 */
function colorizeTraceback(errorStr) {
    const ANSI = {
        RED: '\x1b[0;1;31m',
        YELLOW: '\x1b[0;1;33m',
        CYAN: '\x1b[0;1;36m',
        GREEN: '\x1b[0;1;32m',
        RESET: '\x1b[0m'
    };
    
    return errorStr
        .replace(/^(\w+Error):/gm, ANSI.RED + '$1:' + ANSI.RESET)
        .replace(/File "(.*?)", line (\d+)/g, 
            'File ' + ANSI.YELLOW + '"$1"' + ANSI.RESET + ', line ' + ANSI.CYAN + '$2' + ANSI.RESET)
        .replace(/^(Traceback \(most recent call last\):)/gm, ANSI.CYAN + '$1' + ANSI.RESET)
        .replace(/^(\s*\^+\s*)$/gm, ANSI.GREEN + '$1' + ANSI.RESET);
}

/**
 * Initialize Pyodide
 */
async function loadPyodideAndPackages() {
    try {
        console.log('Starting Pyodide initialization...');
        document.getElementById('status').textContent = 'Loading Pyodide...';
        
        pyodide = await loadPyodide({
            indexURL: "https://cdn.jsdelivr.net/pyodide/v0.24.1/full/"
        });
        
        console.log('Pyodide loaded, setting up output capture...');
        
        await pyodide.runPythonAsync(`
            import sys
            import io
            
            class OutputCapture:
                def __init__(self):
                    self.output = []
                
                def write(self, text):
                    self.output.append(text)
                
                def flush(self):
                    pass
                
                def get_output(self):
                    return ''.join(self.output)
            
            _output_capture = OutputCapture()
            sys.stdout = _output_capture
            sys.stderr = _output_capture
        `);
        
        console.log('Pyodide ready, notifying Android...');
        document.getElementById('status').textContent = 'Python ready!';
        
        if (typeof AndroidBridge !== 'undefined') {
            AndroidBridge.onPyodideReady();
        } else {
            console.error('AndroidBridge not found!');
        }
        
    } catch (error) {
        console.error('Failed to load Pyodide:', error);
        document.getElementById('status').textContent = 'Error: ' + error.message;
        
        if (typeof AndroidBridge !== 'undefined') {
            AndroidBridge.onPyodideError(error.toString());
        } else {
            console.error('AndroidBridge not found for error reporting!');
        }
    }
}

/**
 * Execute Python code
 */
async function runPythonCode(code, input) {
    if (!pyodide) {
        AndroidBridge.onError('Pyodide not loaded');
        return;
    }
    
    try {
        document.getElementById('status').textContent = 'Running Python...';
        
        if (input) {
            await pyodide.runPythonAsync(`
                import sys
                import io
                sys.stdin = io.StringIO('''${input}''')
            `);
        }
        
        await pyodide.runPythonAsync(`_output_capture.output = []`);
        
        try {
            await pyodide.runPythonAsync(code);
            
            const output = await pyodide.runPythonAsync(`_output_capture.get_output()`);
            
            document.getElementById('status').textContent = 'Done!';
            AndroidBridge.onOutput(output || '');
            AndroidBridge.onComplete(0);
            
        } catch (pythonError) {
            const errorOutput = await pyodide.runPythonAsync(`_output_capture.get_output()`);
            const errorStr = pythonError.toString();
            const isSyntaxError = errorStr.includes('SyntaxError') || 
                                 errorStr.includes('IndentationError') || 
                                 errorStr.includes('TabError');
            
            let errorMessage = errorOutput ? errorOutput + '\n' : '';
            const errorType = isSyntaxError ? 'Compilation Error' : 'Runtime Error';
            errorMessage = `\x1b[0;1;31m${errorType}:\x1b[0m\n${errorMessage}${colorizeTraceback(errorStr)}`;
            
            document.getElementById('status').textContent = 'Python Error';
            AndroidBridge.onError(errorMessage);
        }
        
    } catch (error) {
        console.error('Execution error:', error);
        document.getElementById('status').textContent = 'Error: ' + error.message;
        AndroidBridge.onError(error.toString());
    }
}

window.addEventListener('load', loadPyodideAndPackages);
