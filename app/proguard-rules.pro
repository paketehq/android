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
-keep class **.R$* { <fields>; }

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
