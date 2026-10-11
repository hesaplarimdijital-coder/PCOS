# ADR-0001: Araç zinciri — AGP 9.4 yerleşik Kotlin, tek sürüm kataloğu

- Tarih: 2026-10-11 · Durum: Kabul edildi

## Bağlam
Hedef Android 17 (API 37). Google Play, 31 Ağustos 2026'dan beri yeni uygulamalarda API ≥ 36 istiyor. AGP 9,
Kotlin'i yerleşik getiriyor ve `org.jetbrains.kotlin.android` eklentisiyle birlikte kullanılamıyor. Bulut ajan
oturumlarında SDK/Maven erişimi yok; derleme CI'da.

## Karar
- AGP **9.4.0**, Gradle **9.6** wrapper, Kotlin **2.4.10** (yerleşik), KSP **2.3.11**, JDK 17 hedef.
- Kotlin ayarları `kotlin { compilerOptions { } }` üst düzey bloğunda; `kotlinOptions` yok.
- Hilt 2.60 (KSP), Room 2.8 (`androidx.room` eklentisi, `room-ktx` artık runtime içinde), Compose BOM 2026.09.
- Tüm sürümler `gradle/libs.versions.toml`'da; modül dosyalarında sabit sürüm yazılmaz.
- `repositoriesMode = FAIL_ON_PROJECT_REPOS`; depolar yalnızca settings'te.

## Sonuçlar
+ Tek kaynaklı sürümler, Kotlin/AGP uyumsuzluğu riski düşük, Play gereksinimi karşılanır.
− AGP 9'a özel kurallar (eklenti yok, `kotlin {}` bloğu) öğrenme maliyeti; eski örneklerin çoğu uymaz.
− Yerelde derleme yok → ilk CI çalıştırmaları API uyumsuzluklarını yakalayacak; iteratif düzeltme beklenir.

## Alternatifler
AGP 8.x + kotlin-android eklentisi (reddedildi: 2027'de zorunlu geçiş olacak, Android 17 özellikleri için 9 gerekli).
