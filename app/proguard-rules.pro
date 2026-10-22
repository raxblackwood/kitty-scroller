# AntiScroller Kitty Proguard & R8 Optimization Rules
-keep class com.antiscroller.kitty.service.KittyAccessibilityService { *; }
-keep class com.antiscroller.kitty.admin.KittyAdminReceiver { *; }
-keep class com.antiscroller.kitty.receiver.BootReceiver { *; }
-keep class com.antiscroller.kitty.ui.KittyCurtainView { *; }
-keep class com.antiscroller.kitty.ui.** { *; }
-keep class com.antiscroller.kitty.worker.** { *; }
-keep class com.antiscroller.kitty.billing.** { *; }
-keep class com.antiscroller.kitty.guardian.** { *; }
-keep class com.antiscroller.kitty.data.** { *; }

# WorkManager Workers
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Google Play Billing Client
-keep class com.android.billingclient.api.** { *; }
-dontwarn com.android.billingclient.api.**
