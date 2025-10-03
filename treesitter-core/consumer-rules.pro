# Consumer ProGuard rules for treesitter-core

# Keep all public API
-keep public class com.acc_ide.treesitter.** { public *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

