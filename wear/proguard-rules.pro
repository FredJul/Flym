-renamesourcefileattribute SourceFile    
-keepattributes SourceFile,LineNumberTable
-keepnames class *

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# for picasso
-dontwarn com.squareup.okhttp.**
