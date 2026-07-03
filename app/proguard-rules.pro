# --- Apache POI / OOXML / XmlBeans ---
-keep class org.apache.poi.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn javax.xml.stream.**
-dontwarn javax.el.**
-dontwarn java.awt.**
-dontwarn org.w3c.dom.**
-dontwarn org.openxmlformats.schemas.**
-dontwarn aQute.bnd.annotation.**
-dontwarn edu.umd.cs.findbugs.annotations.**
-dontwarn org.osgi.framework.**

# --- Commons Compress / zip4j reflectively-loaded codecs ---
-dontwarn org.apache.commons.compress.**
-dontwarn org.tukaani.xz.**

# --- Hilt / Room generated code ---
-keep class * extends androidx.room.RoomDatabase
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# --- JNA / uniffi rustcore ---
-keep class com.sun.jna.** { *; }
-keepclassmembers class * extends com.sun.jna.** { *; }
-keep class uniffi.** { *; }

# --- SMBJ ---
-keep class com.hierynomus.** { *; }
-dontwarn com.hierynomus.**

# --- ACRA ---
-keep class org.acra.** { *; }
-keep class * extends org.acra.config.ConfigurationBuilder { *; }
