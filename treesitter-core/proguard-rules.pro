# Add project specific ProGuard rules here.
# Keep all public API classes and methods
-keep public class com.acc_ide.treesitter.** { public *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep data classes
-keep class com.acc_ide.treesitter.core.** { *; }

