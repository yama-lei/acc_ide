![Display1](img/Display1.png)

# ACC IDE

- [Version list](RELEASE.md)
- [English](README_en.md)
- [简体中文](README.md)

If you're tired of OJ platforms with their mobile-unfriendly IDEs, or if you've ever wanted to jot down a brilliant algorithm idea on your phone, then ACC IDE is just what you need 🤗.

ACC IDE is a native Android integrated development environment designed specifically for algorithm competitions. It aims to enhance the competitive programming experience on mobile devices by providing a feature-rich environment for writing, testing, and submitting algorithmic solutions 😋.

## Overview

ACC IDE aims to be a comprehensive mobile solution for competitive programmers who need to code and test algorithms on the go. The application provides syntax highlighting, code completion, file management, and other essential features tailored for competitive programming.

## Project Structure

The project is built with native Android and includes the following main components:

### Core Structure
```
acc_ide/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/acc_ide/
│   │   │   │   ├── adapter/                      # RecyclerView adapters
│   │   │   │   ├── dialog/                       # Dialog components
│   │   │   │   ├── model/                        # Data models
│   │   │   │   ├── util/                         # Utility classes
│   │   │   │   ├── view/                         # Custom views
│   │   │   │   ├── MainActivity.kt               # Main application entry point
│   │   │   │   ├── EditorFragment.kt             # Code editor implementation
│   │   │   │   ├── IOPanelFragment.kt            # Input/output panel
│   │   │   │   ├── SettingsFragment.kt           # Application settings
│   │   │   │   ├── SplashActivity.kt             # Splash screen
│   │   │   │   ├── WelcomeFragment.kt            # Welcome screen
│   │   │   │   └── NewFileDialogFragment.kt      # New file creation dialog
│   │   │   ├── res/                              # Android resources
│   │   │   │   ├── drawable/                     # Image resources
│   │   │   │   ├── layout/                       # Layout files
│   │   │   │   ├── menu/                         # Menu resources
│   │   │   │   ├── values/                       # Strings, colors, etc.
│   │   │   │   └── values-zh/                    # Chinese localization
│   │   │   ├── assets/                           # Application assets
│   │   │   │   ├── fonts/                        # Font files
│   │   │   │   └── textmate/                     # TextMate syntax configuration
│   │   │   └── AndroidManifest.xml
│   ├── build.gradle                              # Module build config
├── gradle/                                       # Gradle wrapper files
└── build.gradle.kts                              # Project build config
```

### Interaction Flow

```mermaid
flowchart TD
    U["User"]
    APP["ACC IDE (Android App)"]
    GHA["GitHub Action<br/>code-execution.yml"]
    JUDGE["Compilation/Execution<br/>Environment"]
    RESULT["Evaluation Results"]

    U -- "Write/Submit code,<br/>input, expected output" --> APP
    APP -- "Trigger GitHub Action<br/>via API/Network" --> GHA
    GHA -- "Setup evaluation environment<br/>Write code/input/output files" --> JUDGE
    JUDGE -- "Compile/Run/Compare outputs" --> GHA
    GHA -- "Generate results<br/>(AC/WA/CE/RE/TLE/RS)" --> RESULT
    RESULT -- "Return evaluation<br/>results to APP" --> APP
    APP -- "Display results<br/>to user" --> U

    style U fill:#f9f,stroke:#333,stroke-width:2
    style APP fill:#bbf,stroke:#333,stroke-width:2
    style GHA fill:#ffd,stroke:#333,stroke-width:2
    style JUDGE fill:#bfb,stroke:#333,stroke-width:2
    style RESULT fill:#fc9,stroke:#333,stroke-width:2
```

## Implemented Features

### Editor Capabilities
- **Syntax highlighting**: Use textmate for syntax highlighting
- **Code Completion**: Simple code completion for common keywords and functions
- **Theme Support**: Dark and light modes with appropriate syntax coloring
- **Gesture Controls**: Adjust font size through zoom gestures
- **Line Numbers and Block Indentation**: Visual aids for code structure
- **Symbol Panel**: Minimalist, mobile-friendly panel for easy input of common programming symbols
- **Undo and Redo**: Support for code editing undo and redo operations

### File Management
- **Create, Open, Save Files**: Basic file operations through an intuitive interface
- **File Browser**: Side drawer with a list of available files
- **Rename and Delete**: File management tools with confirmation dialogs
- **Automatic Saving**: Changes are automatically saved to prevent data loss, with temporary files stored at `/storage/emulated/0/Android/data/com.acc_ide/files` and templates at `/template`

### Customization
- **Language Selection**: Interface language can be changed in settings
- **Theme Selection**: Toggle between dark and light themes
- **Font Size Control**: Adjust editor font size from settings or with gestures
- **Editor Preferences**: Customize editor behavior through settings, such as cursor width and symbol panel display

### Input/Output Panel
- **Input/Output Panel**: For manual input and output viewing
- **GitHub Action Backend**: Free runtime environment through GitHub Actions [repository link](https://github.com/META-Xiao/accide-code-execution), supporting compilation and execution of C/C++, Java, and Python (currently only C/C++ has been tested successfully 😾)
- **Compilation Progress Indicator**: Shows compilation progress and results upon completion
- **Memory and Time Limits**: Restricts code execution time (2s) and memory (512MB) through the GitHub Action backend
- **Execution Status Display**: Shows code execution status and runtime, including AC, WA, TLE, MLE, RE, CE, RS (Run Successful, indicated when user hasn't input an expected output)

## Planned Features

### Improvements to Existing Features
- **Enhanced GitHub Action**: Better support for Java and Python compilation and execution
- **Code Completion**: More comprehensive code completion functionality
- **Android Error Lens**: Highlight compilation errors directly in the editor

### competitive-companion Integration
- Android version of competitive-companion
- Import test cases directly from problem statements
- Support for major competitive programming platforms:
  - Codeforces
  - AtCoder
  - Luogu
  - Niuke

### Local Compiler Integration
- Integration with C/C++, Java, and Python compilers
- Local compilation and execution
- Support for different compiler versions
- Compilation progress indicators
- Compilation error highlighting in the editor

## Installation

- Download the latest version from [releases](https://github.com/META-Xiao/acc_ide/releases/latest)
- Or `clone` the repository locally, open it with Android Studio, and run the project

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## Acknowledgements

- [Sora Editor](https://github.com/Rosemoe/sora-editor) for the code editing capabilities
- [VSCode TextMate](https://github.com/microsoft/vscode-textmate) for syntax highlighting support

---

ACC IDE - Enhancing your OJ experience on Android. 