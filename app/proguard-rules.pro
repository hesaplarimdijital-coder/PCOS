# Ayla — R8 kuralları

# SQLCipher yerel kütüphanesi JNI ile yüklenir
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }

# kotlinx.serialization ile dışa aktarılan modeller
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.ayla.pcos.** {
    *** Companion;
}
-keepclasseswithmembers class com.ayla.pcos.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Navigation 3 geri yığını anahtarları süreç ölümünde yansımayla (Class.forName + serializer()) yazılır/okunur
-keepnames class com.ayla.pcos.ui.navigation.** implements androidx.navigation3.runtime.NavKey
