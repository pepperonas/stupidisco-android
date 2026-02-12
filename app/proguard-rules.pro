# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Keep Compose
-keep class androidx.compose.** { *; }

# Google Tink / EncryptedSharedPreferences
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn com.google.crypto.tink.**
-keep class com.google.crypto.tink.** { *; }
