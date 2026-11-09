# ProGuard rules for Sauti Sovereign Android Offline Engine

# Keep JNI export methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep Compose models and State
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Keep Coroutines
-keepclassmembers class kotlinx.coroutines.** {
    *;
}

