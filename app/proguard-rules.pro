# Proguard rules for ExpenseApp
# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\leohu\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any custom rules here

# Keep DTOs and Domain Models for Gson serialization
-keepclassmembers class com.leohu.expense.data.remote.dto.** { *; }
-keepclassmembers class com.leohu.expense.domain.model.** { *; }

# Prevent R8 from removing @SerializedName annotations
-keepattributes Signature, EnclosingMethod, InnerClasses, *Annotation*
-keep class com.google.gson.annotations.SerializedName { *; }
