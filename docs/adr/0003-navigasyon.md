# ADR-0003: Gezinme — yerel sekme+yığın gezgini; Navigation 3 ertelendi

- Tarih: 2026-10-11 · Durum: Kabul edildi (Nav3 geçişi MVP 2'de yeniden değerlendirilecek)

## Bağlam
Uygulamanın gezinme grafı küçük ve sabit: 5 sekme, 3 ayrıntı ekranı (günlük kayıt, ilaç ekle, ayarlar),
bildirimden hedef sekmeye açılma. Navigation 3 (`androidx.navigation3`) 2026'da kararlı sürüme ulaştı ancak
bulut oturumunda API imzaları doğrulanamıyor ve adaptif düzen (liste-ayrıntı) MVP 1'de yok.

## Karar
`ui/navigation/Navigator.kt`: `Screen` sealed arayüzü (`Tab` alt türleri + `DailyLog(date)`, `AddMedication`,
`Settings`), `AylaNavigator` (sekme, yığın, `selectTab/push/pop`, `canGoBack`) ve `rememberAylaNavigator()`
(`listSaver` ile süreç ölümüne dayanıklı). Geçişler `AnimatedContent` + `AylaMotion`. Geri tuşu `BackHandler`.

## Sonuçlar
+ Sıfır bağımlılık, tamamen öngörülebilir, test edilebilir; bildirim derin bağlantıları `EXTRA_OPEN` ile basit.
− Tablet/katlanabilir liste-ayrıntı düzeni, öngörücü geri animasyonu ve ortak öğe geçişleri elle yazılmalı.
− Grafik büyüdüğünde (MVP 2 planlar, MVP 3 forum) Nav3'e geçiş maliyeti artar; bu nedenle sınır: 12 ekran.

## Alternatifler
Navigation Compose 2.x (tek backstack, sekme durumu koruması zor); Navigation 3 (ertelendi, yukarıdaki nedenle);
Voyager/Decompose (üçüncü taraf, bakım riski).
