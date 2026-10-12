# ADR-0009: Doktor raporu PDF (platform PdfDocument, saf yerleşim motoru) ve MVP 3 veri modeli (Room v2)

- Tarih: 2026-10-12 · Durum: Kabul edildi · İşler: #26 (rapor), #24 (LH/BBT), #25 (cilt/saç)
- İlgili: ADR-0002 (veri katmanı), ADR-0005 (döngü/doğurganlık motoru), ADR-0010 (fotoğraf), US-101/US-103

## Bağlam
MVP 1'den beri "doktor notu" düz metindir (`report/DoctorNote.kt`: son 28 gün, paylaşım menüsü). US-101 doktora
götürülecek **Türkçe PDF** ister: tarih aralığı seçimi, döngüler, semptomlar, ilaç uyumu, doğurganlık, cilt
notları ve kullanıcının biriktirdiği sorular (US-103). Kısıtlar: INTERNET izni ve üçüncü taraf PDF kitaplığı yok;
yerelde derleme yok (CI tek derleyici); Türkçe karakterler (ş, ğ, ı, İ) gömülü fontla doğru çıkmalı; rapor sağlık
verisidir (KVKK özel nitelikli) — uygulama hiçbir yere iletmez, kullanıcı paylaşır.

MVP 3'ün üç işi de yeni kalıcı veri ister: doğurganlık kayıtları, cilt/saç profili ve rutin işaretleri, doktora
sorular. Room şeması bugün sürüm 1. Fotoğraflar Room dışıdır (ADR-0010).

## Karar 1 — PDF: platform `android.graphics.pdf.PdfDocument`, çizim komutlarını üreten saf yerleşim motoru
1. **Üç katman, bağımlılıksız:**
   - `report/ReportModel.kt` (saf Kotlin): `DoctorReport(range, generatedOn, sections)`; `ReportSection(title,
     blocks)`; `ReportBlock` = `Paragraph(text, style)`, `Bullets(items)`, `Table(header, rows)`, `KeyValues`,
     `TemperatureChart(points, coverline, lhPositiveDays, ovulationDate)`, `Note(text)` (uyarı/ibare). Metinler
     ekran dilinde (`strings.xml`) üretilir; model yalnızca hazır dizeleri taşır.
   - `report/DoctorReportBuilder.kt`: depolardan okuyup modeli kurar (aralık, bölüm seçimi, kilo/bel gizliliği,
     hap modunda doğurganlık bölümü yok). Robolectric + bellek içi Room ile test edilir.
   - `report/ReportLayout.kt` (saf Kotlin): modeli **sayfalara ve çizim komutlarına** (`DrawOp.Text/Line/Rect/
     Circle/Polyline`) dönüştürür; A4 (595 × 842 pt), 40 pt kenar, üst bilgi/alt bilgi, sayfa numarası, satır
     kaydırma (kelime bazlı), tablo sütunları, bölüm başlığının sayfa sonunda yalnız kalmaması (en az 2 satır
     taşıma). Metin ölçümü `TextMeasurer` arayüzüyle dışarıdan gelir: üretimde `Paint.measureText`
     (`PaintTextMeasurer`), testte sabit genişlikli sahte ölçer. Böylece sayfalama JVM'de testlenir.
   - `report/PdfReportWriter.kt` (Android): `PdfDocument` ile her sayfayı açar, komutları `Canvas`'a çizer
     (`Typeface.SANS_SERIF`; Skia fontu gömer, Türkçe glifler sorunsuz), `cacheDir/raporlar/<ad>.pdf`'e yazar.
2. **Paylaşım:** `FileProvider` (mevcut yetki `<applicationId>.photos`; `photo_paths.xml`'e `cache-path raporlar`
   eklenir) + `ACTION_SEND` (`application/pdf`, `FLAG_GRANT_READ_URI_PERMISSION`); kullanıcı seçiciden kendisi
   gönderir. Rapor dosyaları uygulama açılışında ve yeni rapor üretiminde temizlenir (en fazla son 3 dosya, 24 saat).
   Dosya adı `ayla-doktor-raporu-<tarih>.pdf`; ad kişisel veri içermez.
3. **İçerik kuralları:** ilk satır "Bu belge kullanıcının kendi kayıtlarının özetidir; tanı, tedavi ya da tavsiye
   değildir." Yorum yok, eşik etiketleri (60./90. gün, < 4 adet) **kayıt** olarak yazılır ("12 ayda 3 adet").
   Kilo/bel yalnızca `showBodyMeasurements` açıkken. Fotoğraf hiç girmez. Cilt bölümü öz-beyanları ve
   "doktoruna söyle" işaretlerini listeler; ürün/ilaç önerisi yok. Doğurganlık bölümü hap modunda yoktur;
   "doğum kontrolü için kullanılmaz" ibaresi bölüm sonundadır.
4. **Düz metin not** (`DoctorNoteRepository`, 28 gün, kendi biçimi) şimdilik kalır; `DoctorReportBuilder.plainText()`
   (`DoctorReport.toPlainText()`) aynı modelden metin üretir ve rapor ekranı geldiğinde hızlı paylaşım da ona
   geçer (tek kaynak; `note_*` dizeleri o zaman kaldırılır).
5. **Rapor ekranı** (`ui/report/ReportScreen.kt`, `Screen.DoctorReport` anahtarı): aralık (28/90/180 gün, özel),
   bölüm anahtarları, sayfa sayısı önizlemesi (yerleşim motoru ucuz), "PDF oluştur ve paylaş". `FLAG_SECURE`
   ayarına uyar.

### Alternatifler
- Üçüncü taraf PDF kitaplığı (iText/OpenPDF/PdfBox-Android): lisans/boyut; font gömme ayrıca iş; reddedildi.
- `WebView.createPrintDocumentAdapter` + `PrintManager`: kullanıcı akışı sistem yazdırma diyaloğuna bağlı,
  dosya üretimi dolaylı; reddedildi.
- Yalnızca düz metin: hekimler tablo ve grafik ister (kullanıcı araştırması); reddedildi.

## Karar 2 — MVP 3 veri modeli: Room şema sürümü 2 (tek migration)
Üç iş tek sürüm artışında birleşir; `MIGRATION_1_2` yalnızca tablo ekler (yıkıcı değil). `schemas/2.json` CI'da
üretilip depoya yazılır. Dışa aktarma biçimi `schemaVersion` 3 (yeni listeler varsayılan boş; eski dosyalar
aynen okunur); içe aktarma "birleştir"de aynı anahtar varsa atlar, "üzerine yaz"da tabloyu boşaltır; tam silme
dört tabloyu da boşaltır.

| Tablo | Alanlar | Not |
|---|---|---|
| `fertility_entries` | `date` PK (TEXT ISO), `bbt_celsius` REAL?, `bbt_time` TEXT? (LocalTime), `bbt_disturbed` INTEGER NOT NULL 0, `lh_result` TEXT? (`LhResult` adı), `mucus` TEXT? (`CervicalMucus` adı), `updated_at` INTEGER NOT NULL | Motor girdisi `core:cycle FertilityEntry`'ye birebir eşlenir. Sıcaklık 2 ondalık. |
| `skin_profile` | `id` INTEGER PK = 1, `skin_type` TEXT?, `acne` TEXT?, `dark_spots` INTEGER NOT NULL 0, `acanthosis` INTEGER NOT NULL 0, `unwanted_hair` INTEGER NOT NULL 0, `hair_thinning` INTEGER NOT NULL 0, `pregnant_or_breastfeeding` INTEGER NOT NULL 0, `morning_reminder` TEXT? (LocalTime), `evening_reminder` TEXT?, `updated_at` INTEGER NOT NULL | Tek satır. Öz-beyanlar sağlık verisidir; DataStore'a değil şifreli veritabanına yazılır. Gebelik planı `UserPreferences.goals` (TTC) ile birleşir. |
| `skin_routine_checks` | `date` TEXT, `slot` TEXT (`MORNING`/`EVENING`), `done_steps` TEXT (`List<String>` dönüştürücü), `updated_at` INTEGER NOT NULL; PK (`date`, `slot`) | Adım anahtarları içerik tablosundan (`skin_step_*`). |
| `doctor_questions` | `id` INTEGER PK otomatik, `text` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `asked_at` INTEGER? | "Sordum" işaretlenen soru raporun "sorulanlar" kısmına düşer; silinebilir. |

Depolar: `FertilityRepository` (gün bazında upsert/sil; `observeRange`; `FertilityEngine.analyze` çağrısı için
adet başlangıçlarını `CycleRepository`'den alır; hap modunda `Disabled`), `SkinRepository` (profil, rutin
işaretleri), `DoctorQuestionRepository`. `DataExporter`/`DataImporter`: `ExportFertility`, `ExportSkinProfile`,
`ExportSkinRoutine`, `ExportDoctorQuestion`. Widget bu verilere dokunmaz.

### Sonuçlar
+ Tek migration, tek dışa aktarma sürüm artışı; üç özellik aynı veri katmanını paylaşır.
+ PDF sayfalama JVM'de test edilir; cihaz testi yalnızca font/çizim doğrulaması ister.
− `PdfDocument` Robolectric'te çalışmaz (doğrulandı, 12 Ekim 2026: yerel `nativeCreateDocument` yok, "document is
  closed!"); `PdfReportWriterTest`'in üç PdfDocument testi `@Ignore`, yazıcı cihazda doğrulanır
  (`docs/ilk-cihaz-testi.md`). Yerleşim (`ReportLayoutTest`) ve Canvas çizimi (`ReportCanvasTest`) CI'da çalışır.
− Rapor dosyası önbellekte düzdür (PDF şifrelenmez); kısa ömürlü tutulur ve paylaşımdan sonra temizlenir.
