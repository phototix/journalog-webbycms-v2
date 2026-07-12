-keepattributes Signature
-keepattributes *Annotation*

# Retrofit
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Gson
-keep class com.journalog.app.data.remote.dto.** { *; }
-keepclassmembers class com.journalog.app.data.remote.dto.** { *; }
