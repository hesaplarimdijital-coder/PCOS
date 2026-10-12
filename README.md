# Ayla

PCOS (yeni adıyla PMOS) ile yaşayan kadınlar için **tanı koymayan, verisi cihazda şifreli kalan** Android
takip uygulaması. Düzensiz döngüler için aralıklı tahmin ve açık güven düzeyi, ilaç/takviye hatırlatması
(günde iki doz Inofolic, doğum kontrol hapı paket takibi), 30 saniyelik günlük kayıt, su ve uyku.

> Ayla tıbbi tavsiye vermez. Tanı ve tedavi için doktorunuza danışın.

## Durum
MVP 1 kod olarak tamamlandı; ilk gerçek cihaz testi bekleniyor ([docs/ilk-cihaz-testi.md](docs/ilk-cihaz-testi.md)).
Derleme ve testler GitHub Actions'ta (`.github/workflows/android.yml`); her `main` push'unda debug APK
artefakt olarak üretilir.

## Ekranlar

Görüntüler her `main` derlemesinde CI tarafından gerçek Compose koduyla (Robolectric + Roborazzi) üretilir
ve [`ekran-goruntuleri`](https://github.com/hesaplarimdijital-coder/PCOS/tree/ekran-goruntuleri) dalında durur.

| Bugün | Takvim | İlaçlar | Planım | Ben |
|---|---|---|---|---|
| ![Bugün](https://raw.githubusercontent.com/hesaplarimdijital-coder/PCOS/ekran-goruntuleri/01-bugun.png) | ![Takvim](https://raw.githubusercontent.com/hesaplarimdijital-coder/PCOS/ekran-goruntuleri/02-takvim.png) | ![İlaçlar](https://raw.githubusercontent.com/hesaplarimdijital-coder/PCOS/ekran-goruntuleri/03-ilaclar.png) | ![Planım](https://raw.githubusercontent.com/hesaplarimdijital-coder/PCOS/ekran-goruntuleri/04-planim.png) | ![Ben](https://raw.githubusercontent.com/hesaplarimdijital-coder/PCOS/ekran-goruntuleri/05-ben.png) |

| Hap modu | Uzun döngü | Günlük kayıt | Koyu tema | Büyük yazı |
|---|---|---|---|---|
| ![Hap](https://raw.githubusercontent.com/hesaplarimdijital-coder/PCOS/ekran-goruntuleri/06-bugun-hap.png) | ![Uzun döngü](https://raw.githubusercontent.com/hesaplarimdijital-coder/PCOS/ekran-goruntuleri/07-bugun-uzun-dongu.png) | ![Günlük](https://raw.githubusercontent.com/hesaplarimdijital-coder/PCOS/ekran-goruntuleri/09-gunluk-kayit.png) | ![Koyu](https://raw.githubusercontent.com/hesaplarimdijital-coder/PCOS/ekran-goruntuleri/15-koyu-bugun.png) | ![Büyük yazı](https://raw.githubusercontent.com/hesaplarimdijital-coder/PCOS/ekran-goruntuleri/17-buyuk-yazi-bugun.png) |

## Özellikler (MVP 1)
- **Bugün**: hale halkası (döngü günü, tahmin aralığı, güven rozeti), adet başladı/bitti, dozlar, su.
- **Takvim**: kayıtlı adet, tahmini aralık, kayıt izi; adet başlangıcı/bitişi işaretleme, döngüyü hesap dışı bırakma, döngü geçmişi.
- **İlaçlar**: hazır şablonlar (Inofolic Combi, hap 24/4 ve 21/7, metformin, D vitamini, nane çayı) ve tam düzenleme formu (saatler, yemekle/aç karna, stok ve uyarı eşiği, hap paket düzeni ve başlangıcı); haftalık uyum; inositol için kanıt notu.
- **Planım**: su (bardak hedefi, 7 gün), uyku (elle), akşam rutini. Beslenme ve hareket MVP 2.
- **Ben**: döngü düzeni (21–35 gün tipik aralık), takviye uyumu, su/kayıt haftası, 4 haftalık kayıt ızgarası, **doktor notu** (döngü, ilaç ve semptom özeti; paylaşım menüsüyle).
- **Hatırlatmalar**: tam saatinde doz alarmı (Aldım / 15 dk ertele), su, akşam rutini, Pazar akşamı nazik haftalık özet; sessiz saatlere uyar.
- **Gizlilik**: SQLCipher + Android Keystore, uygulama kilidi (biyometri/PIN), ekran görüntüsü engeli, gizli bildirim, KVKK rızaları, JSON dışa aktarma, tam silme; anahtar kaybında veri ezilmez.
- **Erişilebilirlik**: sistem yazı boyutu %200'de kesilme yok (büyük yazıda dikey düzen; görüntüler `y01`–`y08`), iki temada WCAG AA kontrast (otomatik test), renk tek başına anlam taşımaz, animasyonları kaldır ayarına saygı, 48 dp dokunma alanları.
- Uzun döngü uyarıları (60/90. gün) ve yılda 4'ten az adet işareti; hap modunda tahmin kapalı.

## Teknik
AGP 9.4 (yerleşik Kotlin 2.4), Gradle 9.6, Compose BOM 2026.09 / Material 3, Hilt (KSP), Room 2.8 + SQLCipher,
DataStore, AlarmManager tam alarmlar. `compileSdk/targetSdk 37` (Android 17), `minSdk 28`.
Modüller: `:app` ve saf Kotlin `:core:cycle` (döngü motoru, 32 birim testi). Uygulamada 109 test: saf kurallar,
Robolectric + bellek içi Room ile depo/ViewModel testleri, tema kontrastı; ayrıca ekran görüntüleri.

```bash
./gradlew :core:cycle:test                         # döngü motoru testleri
./gradlew :app:testDebugUnitTest                   # uygulama birim testleri
./gradlew :app:testDebugUnitTest -Pekran           # + ekran görüntüleri (app/build/ekran)
./gradlew :app:assembleDebug                       # debug APK (Android SDK gerekir)
```

Katkı kuralları için [CONTRIBUTING.md](CONTRIBUTING.md), değişiklikler için [CHANGELOG.md](CHANGELOG.md).

Mimari kararlar `docs/adr/`, kanıt özeti ve platform notları `docs/arastirma/`, ajan rolleri `.claude/agents/`,
çalışma kuralları `CLAUDE.md`.

## Tasarım
Ayla Tasarım Sistemi: nar/adaçayı/kayısı/lavanta paleti, Bricolage Grotesque + Figtree (OFL, `docs/licenses/`),
yaylı hareket, polar biçimler, renk tek başına anlam taşımaz, animasyonları kaldır ayarına saygı.

## Yol haritası
MVP 2: Health Connect uyku, beslenme/hareket planı, bitkisel öneriler (alerjen/etkileşim notlu), ana ekran widget'ı.
MVP 3: cilt bakımı, LH/BBT, doktor raporu PDF, moderasyonlu forum.

## Lisans
Henüz belirlenmedi. Fontlar SIL Open Font License 1.1.
