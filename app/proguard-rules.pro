# Pertahankan kelas aplikasi dan JavascriptInterface untuk YouTube IFrame Bridge
-keep class com.jadwalshalat.gt5.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes *Annotation*
