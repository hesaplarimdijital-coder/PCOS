# Ayla

PCOS (yeni adıyla PMOS) ile yaşayan kadınlar için **tanı koymayan, verisi cihazda şifreli kalan** Android
takip uygulaması. Düzensiz döngüler için aralıklı tahmin ve açık güven düzeyi, ilaç/takviye hatırlatması
(günde iki doz Inofolic, doğum kontrol hapı paket takibi), 30 saniyelik günlük kayıt, su ve uyku.

> Ayla tıbbi tavsiye vermez. Tanı ve tedavi için doktorunuza danışın.

## Durum
MVP 1 geliştirme aşamasında. Derleme ve testler GitHub Actions'ta (`.github/workflows/android.yml`);
her `main` push'unda debug APK artefakt olarak üretilir.

## Özellikler (MVP 1)
- **Bugün**: hale halkası (döngü günü, tahmin aralığı, güven rozeti), adet başladı/bitti, dozlar, su.
- **Takvim**: kayıtlı adet, tahmini aralık, kayıt izi; adet başlangıcı/bitişi işaretleme, döngüyü hesap dışı bırakma, döngü geçmişi.
- **İlaçlar**: hazır şablonlar (Inofolic Combi, hap 24/4 ve 21/7, metformin, D vitamini, nane çayı) ve özel ekleme; stok uyarısı; haftalık uyum.
- **Planım**: su (bardak hedefi, 7 gün), uyku (elle), akşam rutini. Beslenme ve hareket MVP 2.
- **Ben**: döngü düzeni eğilimi (21–35 gün bandı), takviye uyumu, su/kayıt haftası, 4 haftalık kayıt ızgarası.
- **Gizlilik**: SQLCipher + Android Keystore, uygulama kilidi (biyometri/PIN), ekran görüntüsü engeli, gizli bildirim, KVKK rızaları, JSON dışa aktarma, tam silme.
- Uzun döngü uyarıları (60/90. gün) ve yılda 4'ten az adet işareti; hap modunda tahmin kapalı.

## Teknik
AGP 9.4 (yerleşik Kotlin 2.4), Gradle 9.6, Compose BOM 2026.09 / Material 3, Hilt (KSP), Room 2.8 + SQLCipher,
DataStore, AlarmManager tam alarmlar. `compileSdk/targetSdk 37` (Android 17), `minSdk 28`.
Modüller: `:app` ve saf Kotlin `:core:cycle` (döngü motoru, 32 birim testi).

```bash
./gradlew :core:cycle:test            # döngü motoru testleri
./gradlew :app:assembleDebug          # debug APK (Android SDK gerekir)
```

Mimari kararlar `docs/adr/`, kanıt özeti ve platform notları `docs/arastirma/`, ajan rolleri `.claude/agents/`,
çalışma kuralları `CLAUDE.md`.

## Tasarım
Ayla Tasarım Sistemi: nar/adaçayı/kayısı/lavanta paleti, Bricolage Grotesque + Figtree (OFL, `docs/licenses/`),
yaylı hareket, polar biçimler, renk tek başına anlam taşımaz, animasyonları kaldır ayarına saygı.

## Yol haritası
MVP 2: Health Connect uyku, beslenme/hareket planı, bitkisel öneriler (alerjen/etkileşim notlu), Glance widget.
MVP 3: cilt bakımı, LH/BBT, doktor raporu PDF, moderasyonlu forum.

## Lisans
Henüz belirlenmedi. Fontlar SIL Open Font License 1.1.
