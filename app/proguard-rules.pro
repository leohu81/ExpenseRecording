# Proguard rules for ExpenseApp
# Add project specific ProGuard rules here.

# Add any custom rules here

# Keep DTOs and Domain Models for Gson serialization
-keepclassmembers class com.leohu.expense.data.remote.dto.** { *; }
-keepclassmembers class com.leohu.expense.domain.model.** { *; }
-keep class com.leohu.expense.data.remote.db.** { *; }
-keep class com.leohu.expense.domain.usecase.** { *; }

# Prevent R8 from removing @SerializedName annotations
-keepattributes Signature, EnclosingMethod, InnerClasses, *Annotation*
-keep class com.google.gson.annotations.SerializedName { *; }

# Keep Retrofit API interfaces
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Keep OkHttp
-dontwarn okio.**
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Keep Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# Keep Gson
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep database models
-keep class com.leohu.expense.data.remote.db.DbProfile { *; }
-keep class com.leohu.expense.data.remote.db.ExpenseItem { *; }
-keep class com.leohu.expense.data.remote.db.ExpenseDto { *; }
-keep interface com.leohu.expense.data.remote.db.N8nApi { *; }
