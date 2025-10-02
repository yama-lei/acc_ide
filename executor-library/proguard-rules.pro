# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep executor classes and their public APIs
-keep public class com.acc_ide.executor.** { public *; }

# Keep data classes for ExecutionResult
-keepclassmembers class com.acc_ide.executor.ExecutionResult {
    *;
}

# Keep interface methods
-keep interface com.acc_ide.executor.ICodeExecutor { *; }

# OkHttp rules
-dontwarn okhttp3.**
-dontwarn okio.**

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

