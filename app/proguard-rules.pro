-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Log (Ref: https://stackoverflow.com/a/2466662/4729203)
-dontskipnonpubliclibraryclasses
-dontobfuscate
-forceprocessing
-optimizationpasses 5

-keep class * extends android.app.Activity
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

# Kakao
-keep class com.kakao.** { *; }
-keepattributes Signature
-keepclassmembers class * {
  public static <fields>;
  public *;
}
-dontwarn android.support.v4.**,org.slf4j.**,com.google.android.gms.**

# Naver (Ref: https://developers.naver.com/docs/login/android)
-keep public class com.nhn.android.naverlogin.** {
       public protected *;
}

# Firebase

# Add this global rule
-keepattributes Signature

# This rule will properly ProGuard all the model classes in
# the package com.yourcompany.models. Modify to fit the structure
# of your app.
-keepclassmembers class com.chaigene.petnolja.model.** {
  *;
}

# Basic ProGuard rules for Firebase Android SDK 2.0.0+
-keep class com.firebase.** { *; }
-keep class org.apache.** { *; }
-keepnames class com.fasterxml.jackson.** { *; }
-keepnames class javax.servlet.** { *; }
-keepnames class org.ietf.jgss.** { *; }
-dontwarn org.apache.**
-dontwarn org.w3c.dom.**

# Ref: https://github.com/firebase/FirebaseUI-Android/issues/397
-dontwarn retrofit2.**
-dontwarn okio.**

# Ref: https://stackoverflow.com/a/46596848/4729203
-dontwarn android.arch.**

# Glide (Ref: https://github.com/bumptech/glide#proguard)
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public class * extends com.bumptech.glide.module.AppGlideModule
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# for DexGuard only
-keepresourcexmlelements manifest/application/meta-data@value=GlideModule