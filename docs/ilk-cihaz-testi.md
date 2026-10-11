# İlk cihaz testi (MVP 1 debug APK)

CI her `main` push'unda debug APK üretir. Ajan oturumları uygulamayı çalıştıramaz; bu liste gerçek cihazda
ilk kez açan kişi içindir. Bulguları GitHub Issues'a (etiket `mvp-1`) yaz.

## APK'yı alma
1. GitHub → **Actions** → en üstteki yeşil "Android CI" çalıştırması → sayfanın altında **Artifacts** → `ayla-debug-apk` (zip).
2. Zip içindeki `app-debug.apk` dosyasını telefona aktar, "bilinmeyen kaynaklardan yükleme" izniyle kur.
   Paket adı `com.ayla.pcos.debug`; mağaza sürümüyle yan yana durabilir.
3. Android 12+ cihazda ilk açılışta bildirim izni sorulur; "Alarmlar ve hatırlatıcılar" izni ayarlardan açılır.

## 25 dakikalık kontrol listesi
| # | Adım | Beklenen |
|---|---|---|
| 1 | Karşılama: ad gir, "Döngümü düzene sokmak" + "Doğum kontrol hapı" seç, son adet için 12 gün önceyi seç, rızaları olduğu gibi bırak, bildirim izni ver | Bugün ekranı açılır; hap modu kartı (24/4, 22:00) görünür, halka yerine paket ızgarası |
| 2 | İlaçlar → Hızlı ekle → Inofolic Combi | 09:00 ve 21:00 iki doz; Bugün'de "Bugünkü planın" altında iki satır |
| 3 | Bir doza "Aldım" | Buton yeşil onay rozetine dönüşür; tekrar dokununca geri alınır |
| 4 | Telefon saatini bir sonraki doz saatinin 1 dk öncesine al, bekle | Tam alarm izni açıksa tam saatinde bildirim; "Aldım" ve "15 dk ertele" eylemleri çalışır |
| 5 | Tam alarm iznini kapat, uygulamaya dön | Bugün'de "Tam zamanlı hatırlatma" bilgi bandı çıkar; izni açınca bant kaybolur |
| 6 | Takvim: geçmiş bir güne dokun → "Adet başlangıcı olarak işaretle"; 4 gün sonrasına → "Adet bitişi" | Günler dolu nar daire olur; Döngü geçmişi satırı ve istatistik metni belirir |
| 7 | Takvim: bir döngüyü "hesaba katma" | Satırda "Hesap dışı" rozeti; Ben ekranındaki çubuklardan düşer |
| 8 | Günlük kayıt: ruh hâli, enerji, 2 semptom, not → Kaydet | Takvimde o günün altında lavanta nokta; Ben'de kayıt ızgarası dolar |
| 9 | Planım: + ile 3 bardak su, − ile 1 çıkar; sonra + düğmesine hızlıca 10 kez dokun | Bardak dolumu yayla hareket eder, yüzeyde hafif dalga; 7 gün çubuğunda bugün 2, hızlı dokunuşlardan sonra 12 (hiçbiri kaybolmaz) |
| 10 | Planım: Uyku ekle (23:30 → 07:15, kalite İyi) | "7 sa 45 dk · Dün gece 23:30–07:15" |
| 11 | Ayarlar: Uygulama kilidi aç, uygulamayı arka plana al, 1 dk sonra dön | Biyometri/PIN istemi; iptal edilirse kilit ekranında kalır |
| 12 | Ayarlar: Ekran görüntüsünü engelle açıkken ekran görüntüsü al | Sistem engeller; kapatınca alınır |
| 13 | Ayarlar: Verilerimi dışa aktar → Dosyalar'a kaydet | JSON dosyası; içinde periods/dailyLogs/medications/doses/hydration/sleep |
| 14 | Koyu tema | Tüm metinler okunur, kart zeminleri ayrışır, nar vurgusu açık pembe |
| 15 | Erişilebilirlik → Yazı boyutu en büyük (%200) | Alt gezinme etiketleri tam; takvim başlıkları tek harf; Planım sekmeleri kaydırılır; ayar ve döngü satırları dikey; hiçbir metin kesilmez |
| 16 | Erişilebilirlik → Animasyonları kaldır | Halka nefes almaz, takvim ay geçişi ve seçili gün kartı anında, bardak dalgası durur |
| 17 | TalkBack ile Bugün | Halka: "Döngünün N. günü. Tahmini adet … güven …" okunur |
| 18 | Telefonu yeniden başlat | Bir sonraki doz alarmı yine çalar (SystemEventReceiver) |
| 19 | Sessiz saatlerde (22:30–08:00) su hatırlatması | Gelmez; akşam rutini açıksa 22:30'da gelir |
| 20 | Ben → Doktor notu | Paylaşım menüsü açılır; metnin en üstünde "tanı ya da tıbbi tavsiye içermez" ibaresi, altında döngü, ilaç uyumu ve son 4 hafta semptomları |
| 21 | Ayarlar: Tüm verilerimi sil → onayla | Karşılama ekranına dönülür, veriler ve ilaçlar silinmiştir |

## Bilinen eksikler
Health Connect uyku içe aktarma (#11), beslenme/hareket içeriği (#12) ve widget (#14) MVP 2'dedir.
Gerçek cihazda alarm güvenilirliği (üretici pil optimizasyonları) için #17.
