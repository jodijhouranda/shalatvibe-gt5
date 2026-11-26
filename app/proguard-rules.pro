# Pertahankan kelas aplikasi dan JavascriptInterface untuk YouTube IFrame Bridge
-keep class com.jadwalshalat.gt5.** { *; }
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keepattributes *Annotation*

# Pertahankan Credential Manager & Google Identity Services untuk Google Sign-In pada Release APK
-keep class androidx.credentials.** { *; }
-keep class com.google.android.libraries.identity.googleid.** { *; }

