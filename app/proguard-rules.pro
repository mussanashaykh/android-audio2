
 # Apache HttpClient
-dontwarn org.apache.http.**
-keepattributes InnerClasses
-keep public class android.net.http.SslError
-keep public class android.webkit.WebViewClient
-keep class com.nurulquran.audio.object.CategoryMusic {*;}
-keep class com.nurulquran.audio.object.Song.* {*;}
-keep class com.nurulquran.audio.object.Album.* {*;}
-keep class com.nurulquran.audio.object.Banner.* {*;}
-keep class com.nurulquran.audio.object.OfflineData.* {*;}
-keep class com.nurulquran.audio.object.Playlist.* {*;}
-keep class com.nurulquran.audio.object.Radio.* {*;}
-keepclassmembers class com.nurulquran.audio.object.* { public protected private*; }
-dontwarn android.webkit.WebView
-dontwarn android.net.http.SslError
-dontwarn android.webkit.WebViewClient
-keepattributes *Annotation*
-keepattributes EnclosingMethod