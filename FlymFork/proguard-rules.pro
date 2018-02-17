-renamesourcefileattribute SourceFile    
-keepattributes SourceFile,LineNumberTable,MethodParameters,InnerClasses
-keepnames class *

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# for the search
-keep class android.support.v7.widget.SearchView { *; }

# for OkHttp
#-dontwarn okio.**
#-dontwarn com.squareup.okhttp3.**

-keepattributes Signature
-keepattributes Annotation
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**


# for Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}