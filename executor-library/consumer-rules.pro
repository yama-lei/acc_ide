# Consumer ProGuard rules for executor-library
# These rules will be applied to apps that use this library

# Keep public API classes
-keep public class com.acc_ide.executor.ICodeExecutor { *; }
-keep public class com.acc_ide.executor.ExecutorFactory { *; }
-keep public class com.acc_ide.executor.ExecutionResult { *; }
-keep public class com.acc_ide.executor.ExecutionStatus { *; }
-keep public class com.acc_ide.executor.LocalExecutor { *; }
-keep public class com.acc_ide.executor.GitHubExecutor { *; }

