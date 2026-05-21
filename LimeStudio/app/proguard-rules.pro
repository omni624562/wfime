# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Art Hung\AppData\Local\Android\sdk/tools/proguard/proguard-android.txt
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
-keepattributes InnerClasses

# Keep data model classes accessed via reflection (LimeDB, Word, Im, Related)
-keep class net.toload.main.hd.data.** { *; }
-keep class net.toload.main.hd.limedb.** { *; }

# Keep keyboard view classes loaded via XML inflation
-keep class net.toload.main.hd.keyboard.** { *; }
-keepclassmembers class * extends android.view.View {
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep AIDL-generated and IME service classes
-keep class net.toload.main.hd.LIMEService { *; }

# Remove all debug logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}
