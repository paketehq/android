# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/rad182/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
-keep class io.realm.annotations.RealmModule
-keep @io.realm.annotations.RealmModule class *
-keep class io.realm.internal.Keep
-keep @io.realm.internal.Keep class * { *; }
-keep class com.crashlytics.** { *; }
-keep class retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes EnclosingMethod
-dontwarn retrofit2.**
-dontwarn retrofit.**
-dontwarn rx.internal.util.**
-dontwarn com.crashlytics.**
-dontwarn io.smooch.**
-dontwarn io.fabric.**
-dontwarn com.devmarvel.**
-dontwarn okio.**
-dontwarn com.squareup.**
-dontwarn javax.**
-dontwarn io.realm.**
-dontwarn java.lang.invoke.*
-dontwarn com.google.**
-dontwarn com.mixpanel.**

# rxjava
-keep class rx.schedulers.Schedulers {
    public static <methods>;
}
-keep class rx.schedulers.ImmediateScheduler {
    public <methods>;
}
-keep class rx.schedulers.TestScheduler {
    public <methods>;
}
-keep class rx.schedulers.Schedulers {
    public static ** test();
}
-keepclassmembers class rx.internal.util.unsafe.*ArrayQueue*Field* {
    long producerIndex;
    long consumerIndex;
}
-keepclassmembers class rx.internal.util.unsafe.BaseLinkedQueueProducerNodeRef {
    long producerNode;
    long consumerNode;
}
