# --- Apache POI / OOXML / XmlBeans ---
-keep class org.apache.poi.** { *; }
-keep class org.openxmlformats.** { *; }
-keep class org.apache.xmlbeans.** { *; }
-keep class schemaorg_apache_xmlbeans.** { *; }
-dontwarn org.apache.poi.**
-dontwarn org.apache.xmlbeans.**
-dontwarn javax.xml.stream.**
-dontwarn java.awt.**
-dontwarn org.w3c.dom.**

# --- Commons Compress / zip4j reflectively-loaded codecs ---
-dontwarn org.apache.commons.compress.**
-dontwarn org.tukaani.xz.**

# --- Hilt / Room generated code ---
-keep class * extends androidx.room.RoomDatabase
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
