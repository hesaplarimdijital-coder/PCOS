# İlk cihaz testi (debug APK)

CI her `main` push'unda debug APK üretir. Ajan oturumları uygulamayı çalıştıramaz; bu liste gerçek cihazda
ilk kez açan kişi içindir. Bulguları GitHub Issues'a (etiket `mvp-1` ya da `mvp-2`) yaz.

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
| 21 | Ayarlar: Yedekten geri yükle → 13. adımdaki dosya → Birleştir | Önizlemede tarih ve kayıt sayıları; sonuç "N kayıt zaten vardı" der, hiçbir kayıt ikilenmez |
| 22 | Planım → Beslenme: bir kanıt çipine dokun; "Yürüyüş rutinini aç" | Çip düzeyin ne anlama geldiğini açıklar; Hareket sekmesine geçilir, yürüyüş rutininin adımları açık |
| 23 | İlaçlar (doz saati olan bir ilaç varken), pil ayarı "Optimize edilmiş" | "Hatırlatmaların gecikmesin" bandı; Xiaomi/Samsung vb. cihazda üreticiye özel ek not. "Pil ayarlarını aç" uygulama bilgilerini açar; "Kısıtlanmamış" seçip dönünce bant kaybolur, Ayarlar > Bildirimler'de "Pil tasarrufu kısıtlamıyor" yazar |
| 24 | Uygulama bilgileri > Pil > "Kısıtlanmış" | İlaçlar'da kapatılamayan "Arka plan kullanımı kısıtlı" bandı |
| 25 | Planım > Uyku > "Health Connect'ten al" (Android 14+ ya da Health Connect uygulaması kurulu) | Önce Ayla'nın rıza ekranı, sonra Health Connect izin penceresi; izin verilince son 7 günün geceleri "Health Connect'ten" etiketiyle eklenir, ikinci denemede "Eklenecek yeni gece yok"; kartta silinen bir gece "Şimdi al" ile geri gelmez |
| 26 | Health Connect izin ekranındaki "Gizlilik politikası" bağlantısı | Ayla'nın politika ekranı açılır (kilit ekranı ya da Bugün sekmesi değil); kapatınca Health Connect'e dönülür |
| 27 | İzin penceresini iki kez "İptal" ile kapat, sonra yeniden dene | "İzin artık Health Connect ayarlarında" diyaloğu ve ayarlara giden düğme |
| 28 | Ayarlar > Health Connect > Bağlantıyı kes | Durum "Bağlı değil"; alınan geceler kalır; "Health Connect'ten alınan geceleri sil" yalnızca onları siler |
| 29 | Ana ekrana Ayla widget'ını ekle (2×2 ve 4×2) | Seçicide örnek veriyle önizleme; eklenince bugünün günü ve sıradaki doz; "Aldım" dozu işaretler, widget sıradaki doza geçer, bildirim kapanır |
| 30 | Uygulama kilidini ya da Gizli bildirimleri aç | Widget yalnızca "AYLA · Bugün · Görmek için dokun" gösterir |
| 31 | Android 16+ kilit ekranı widget'ları | Ayla widget'ı kilit ekranı seçicisinde görünmez |
| 32 | Gece yarısını geçir (telefon uyurken) | Telefon uyanınca widget yeni günün dozlarını gösterir |
| 33 | Ayarlar: Tüm verilerimi sil → onayla | Karşılama ekranına dönülür, veriler, ilaçlar ve hatırlatma günlüğü silinmiştir; widget nötr içeriğe döner, Health Connect izni bırakılır |

## 48 saatlik alarm güvenilirliği (#17)
Üreticilerin pil tasarrufu kuralları emülatörde görülmez; bu test her yeni cihaz modelinde bir kez yapılır.

1. Inofolic Combi'yi (09:00 ve 21:00) ekle, su hatırlatmasını ve akşam rutinini açık bırak. Pil ayarına
   dokunma (varsayılan "Optimize edilmiş"); telefonu normal kullan, geceleri ekran kapalı beklesin.
2. 48 saat sonra Ayarlar > Bildirimler > Hatırlatma günlüğü. Beklenen: tam zamanlı alarm izni açıkken doz
   gecikmeleri 1 dakikanın altında; izin kapalıyken en fazla 10 dakika. Beklenen saatte kaydı olmayan bir doz,
   alarmın hiç çalmadığı anlamına gelir.
3. "10 dakikadan fazla gecikti" satırı çıkıyorsa ya da kayıt eksikse: İlaçlar'daki banttan pil ayarını
   "Kısıtlanmamış" yap, üreticiye özel notu uygula, günlüğü temizleyip 48 saati tekrarla.
4. Günlükte Paylaş → metni cihaz testi issue'suna (#5, #7) yapıştır. Metin cihaz modelini, Android sürümünü,
   tam zamanlı alarm iznini ve pil durumunu kendisi ekler; ilaç adı içermez.

## Bilinen eksikler
Erişilebilirlik ve boş/hata durumlarının cihazda denetimi (#5) ile bildirim izni reddi akışının son hâli (#7)
gerçek cihaz gerektirir. Widget'ın `exported="false"` sağlayıcıyla güncellendiği ve kilit ekranı dışı
bırakmanın OEM başlatıcılarında çalıştığı da cihazda doğrulanmalıdır (ADR-0007).
