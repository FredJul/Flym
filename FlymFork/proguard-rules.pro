-renamesourcefileattribute SourceFile    
-keepattributes SourceFile,LineNumberTable,MethodParameters,InnerClasses
-keepnames class *

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# for the search
-keep class android.support.v7.widget.SearchView { *; }

# for OkHttp
-dontwarn okio.**
-dontwarn com.squareup.okhttp.**

# for Glide
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}