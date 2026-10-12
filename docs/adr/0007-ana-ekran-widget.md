# ADR-0007: Ana ekran widget'ı: Glance yerine RemoteViews, olaylarla tazeleme, gizlilik varsayılanları

- Tarih: 2026-10-12 · Durum: Kabul edildi · İş: #14
- İlgili: ADR-0004 (hatırlatmalar, "WorkManager kullanılmaz"), ADR-0006 (tasarım dili),
  `docs/arastirma/mvp2-teknik-notlar.md` §2 (Glance araştırması)

## Bağlam
Widget bugünün gününü (döngü ya da hap günü), sıradaki dozu, tek dokunuşla "Aldım"ı ve geniş boyutta suyu
gösterecek. Araştırma notu Glance 1.2.0'ı önerdi ve iki açık konu bıraktı: WorkManager istisnası ve ekran
görüntüsü. Uygulamaya geçerken şunlar netleşti:

- **WorkManager ve izinler.** `glance-appwidget` oturumlarını WorkManager ile çalıştırır. WorkManager birleştirilmiş
  manifeste `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE` izinlerini ve kendi şifresiz SQLite
  veritabanını ekler. İçinde sağlık verisi olmasa da gizlilik politikası, Veri güvenliği formu ve Play'deki izin
  listesi değişir; ADR-0004'ün "WorkManager kullanılmaz" ilkesine de istisna gerekir.
- **Ekran görüntüsü.** CLAUDE.md "ekranı görmeden arayüz değiştirme" der. Glance için resmî bir ekran görüntüsü
  aracı yok (`glance-experimental-tools` Mayıs 2026'da arşivlendi). RemoteViews ise Robolectric'te
  `RemoteViews.apply()` ile gerçek görünüme açılır ve Roborazzi ile çizilir.
- **Güncelleme modeli.** Glance oturumu yaklaşık 45 sn yaşar; sonrası `update`/`updateAll` ister ve her çağrı
  yeni bir kompozisyon başlatır. RemoteViews'ta `AppWidgetManager.updateAppWidget` eşzamanlı ve ucuzdur.
- İki yolda da uygulamanın fontları ve polar biçimleri kullanılamaz; fark burada değil.

## Karar
1. **RemoteViews + `AppWidgetProvider`.** Yeni bağımlılık yok (Glance eklenmez, WorkManager gelmez). İki düzen:
   2×2 (`widget_ayla`) ve 4×2 (`widget_ayla_genis`). Android 12+ `RemoteViews(Map<SizeF, RemoteViews>)` ile sistem
   boyuta göre seçer; daha eskide `OPTION_APPWIDGET_MIN_WIDTH ≥ 250 dp` geniş düzeni seçer.
2. **Veri.** `WidgetStateSource` mevcut depolardan (şifreli Room ve DataStore) okur; widget kendi durumunu hiçbir
   yere yazmaz. Kurallar saf `buildWidgetState` fonksiyonundadır (testli).
3. **Gizlilik varsayılanları.** Widget uygulama kilidinin ve ekran görüntüsü engelinin dışında, başlatıcıda çizilir.
   - Uygulama kilidi ya da "Gizli bildirimler" açıksa, karşılama bitmemişse ya da açık rıza yoksa yalnızca nötr
     içerik gösterilir: "AYLA · Bugün · Görmek için dokun". Bu durumda veritabanı hiç okunmaz.
   - Tahmin, 60/90. gün uyarıları, kilo ve bel çevresi widget'ta hiç gösterilmez. Hap kullanıcısında döngü günü
     yerine paket günü gösterilir.
   - Android 16+ (`res/xml-v36`): `widgetCategory="home_screen|not_keyguard"`; widget kilit ekranına eklenemez.
   - Seçicideki önizleme (`previewLayout`, Android 12+) yalnızca sabit örnek veri içerir.
4. **Tazeleme olaylarla yapılır; periyodik güncelleme yok** (`updatePeriodMillis = 0`). Tetikler, sağlayıcıya açık
   `ACTION_REFRESH` yayını gönderen `WidgetUpdater.requestUpdate` ile tek yoldan gider. Widget yoksa bu çağrı
   hiçbir şey yapmaz.
   - Doz eylemi (bildirim ya da widget): `ReminderActionReceiver`.
   - Alarm çaldı: `ReminderReceiver`.
   - Saat/dilim değişimi, yeniden başlatma, güncelleme: `SystemEventReceiver`.
   - Uygulama içi yazmalar: `WidgetSync` (uygulama kapsamı, durum akışında `distinctUntilChanged` + 500 ms
     birleştirme). Yalnızca widget varken başlar. Tam silme ve yedekten geri yükleme de buradan yansır.
   - Gün dönümü: uyandırmayan `AlarmManager.set(RTC, gece yarısı + 1 dk)`. Tam zamanlı alarm izni gerekmez.
5. **"Aldım"** bildirimdeki eylemle aynı alıcıya gider (`ReminderActionReceiver.ACTION_TAKEN`). Her doz ayrı veri
   URI'si taşır ki iki dozun PendingIntent'i birbirine çökmesin. İşaretleme idempotenttir. Uygulamayı açan
   niyetler yalnızca beyaz listedeki hedefleri (`today`, `meds`) taşır.
6. Sağlayıcı `exported="false"`. Hilt bileşeni değildir; bağımlılıklar `WidgetEntryPoint` ile, IO'da alınır.

## Sonuçlar
+ Manifest izinleri ve politika değişmedi; WorkManager ve ek veritabanı yok.
+ Widget ekran görüntüleri CI'da (`38-widget`, `39-widget-koyu`, `y16-yazi200-widget`) ve düzen testleri
  Robolectric'te çalışır.
− Duyarlı düzen ve gece rengi XML'de elle tutulur (`values-night/colors.xml`, `widget_*` tokenları).
  ADR-0006 tokenlarıyla aynı değerlerdir; token değişirse ikisi birlikte güncellenir.
− Kilit ekranı dışı bırakma (`not_keyguard`) ve `exported="false"` sağlayıcı gerçek cihazda doğrulanmalı
  (`docs/ilk-cihaz-testi.md`).
− İlk sürümde tek widget türü var; büyük ekran ya da etkileşimli liste gerekirse Glance yeniden değerlendirilir.

## Alternatifler
- **Glance 1.2.0:** Compose benzeri API ve test kitaplığı, ama WorkManager, ek izinler ve ekran görüntüsü yokluğu
  yukarıdaki nedenlerle ertelendi.
- **`updatePeriodMillis` ile periyodik güncelleme:** cihazı uyandırır ve doz eylemlerini geç yansıtır.
