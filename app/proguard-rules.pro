# ProGuard rules for Lee Vaakki
# Firebase rules are automatically included via the library dependencies.

# Add your custom rules here if needed.
-keep class com.leevaakki.cafe.models.** { *; }
-keep class com.leevaakki.cafe.viewmodel.** { *; }

# Razorpay & Google Pay
-keep class com.razorpay.** {*;}
-dontwarn com.razorpay.**
-keep class com.google.android.apps.nbu.paisa.** {*;}
-dontwarn com.google.android.apps.nbu.paisa.**
-keep class proguard.annotation.** { *; }
-keepnames class com.razorpay.**

# Standard Android/GMS rules that might be missing
-dontwarn com.google.android.gms.**
-keep class com.google.android.gms.** { *; }
