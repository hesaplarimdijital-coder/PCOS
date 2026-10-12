# Değişiklik günlüğü

Biçim [Keep a Changelog](https://keepachangelog.com/tr-TR/1.1.0/) ilkelerine, sürümleme
[Semantik Sürümleme](https://semver.org/lang/tr/)'ye uyar.

## [0.2.0] — yayımlanmadı (MVP 2)

### Eklendi
- Yedekten geri yükleme: dosyanın tarihi ve içeriğiyle önizleme, "Birleştir" (cihazdaki kayıt kazanır) ya da
  onaylı "Üzerine yaz"; anahtar kaybı diyaloğundan doğrudan geri yükleme.
- Planım > Beslenme ve Hareket: 2023 uluslararası kılavuzuna dayalı ilkeler, tabak modeli, haftalık hareket ritmi
  ve dört kısa rutin; her kartta kanıt düzeyi ve kaynak. Kalori, tartı ya da "yasak" dili yok.
- Bitkisel destekler: yeşil nane çayı, tarçın; her biri "ne denendi", alerji, dikkat ve "doktoruna söyle"
  notuyla. Listeye alınmayanlar uyarısı (sarı kantaron × hormonal doğum kontrol, meyan kökü, berberin, hayıt,
  zerdeçal takviyeleri).
- Health Connect'ten uyku (yalnızca okuma, yalnızca uyku): açık rıza ekranı, izin akışı (güncelleme gerekli,
  iki kez iptal), yalnızca son 7 gün, çakışan ve yinelenen gecelerin elenmesi, silinen gecenin geri gelmemesi,
  bağlıyken ön planda saatlik eşitleme;
  Ayarlar > Health Connect (bağlan, şimdi al, ayarlar, bağlantıyı kes, alınanları sil).
- Ana ekran widget'ı (2×2 ve 4×2): döngü ya da hap günü, sıradaki doz ve "Aldım", su; gizlilik ayarı açıkken
  nötr içerik, kilit ekranına eklenemez.
- Pil tasarrufu yönlendirmesi (üreticiye özel notlarla) ve hatırlatma günlüğü (son 48 saatin gecikmeleri,
  paylaş, temizle).
- KVKK: ayrı açık rıza (önceden işaretli değil), uygulama içi gizlilik politikası, kişiselleştirme rızası
  (varsayılan kapalı), günlük kaydı ve uyku gecesi için tek tek silme.

### Değişti
- "Dün gece" uyanılan günün en uzun oturumudur (öğle uykusu geceyi geçmez).
- Navigation 3'e geçiş eşiklere bağlandı (ADR-0003 güncellemesi); widget kararları ADR-0007.

### Erişilebilirlik
- %200 yazıda kanıt ve süre çipindeki simgeler, adım numaraları ve tabak açıklama noktaları yazıyla büyür;
  uyarı bandı düğmeleri sığmazsa alt satıra geçer; Health Connect gerekçe ekranında rıza ve düğmeler
  kaydırılan alana iner.

### Güvenlik ve gizlilik
- CI izin denetimi: `INTERNET` ya da `READ_SLEEP` dışındaki bir Health Connect izni eklenirse derleme kırmızıya
  döner.
- Dışa açık etkinliğe gelen "geri yükle" hedefi kaldırıldı; geri yükleme yalnızca uygulama içinden açılır.

## [0.1.0] — yayımlanmadı (MVP 1)

### Eklendi
- Döngü motoru (`core:cycle`): düzensiz döngüler için aralıklı tahmin ve güven düzeyi, 60/90. gün
  uyarıları, yılda 4'ten az adet işareti, doğum kontrol hapı paket hesabı, BBT yumurtlama tespiti.
- Bugün: hale halkası, hap modu, doz listesi (Aldım / geri al), su, uzun döngü bantları, ilk adet kartı.
- Takvim: kayıtlı adet, tahmini aralık, kayıt izi; adet başlangıcı/bitişi işaretleme; döngüyü
  ortalamaya katmama; döngü geçmişi ve konum çizgisi.
- İlaçlar: şablonlar, tam düzenleme formu, stok ve uyarı eşiği, haftalık uyum, kanıt notları.
- Günlük kayıt: ruh hâli, enerji, kanama, semptomlar, not; isteğe bağlı kilo/bel.
- Planım: su (hedef, 7 gün), uyku (elle), akşam rutini.
- Ben: döngü düzeni grafiği, uyum ve alışkanlık kartları, doktor notu paylaşımı.
- Hatırlatmalar: tam zamanlı alarmlar ve pencereli yedek, yeniden başlatma/saat dilimi dayanıklılığı,
  sessiz saatler, haftalık özet (Pazar 19:00).
- Ayarlar: profil ve öncelikler, su ve hatırlatma saatleri, koruma (kilit, ekran görüntüsü engeli,
  gizli bildirim), KVKK rızaları, dışa aktarma ve tam silme.
- Güvenlik: SQLCipher + Keystore; anahtar kaybında karantina ve dürüst bildirim.
- Simge, temalı simge, açılış ve bildirim simgesi.
- CI: birim testleri, debug APK, R8 sürüm derlemesi, lint özeti, ekran görüntüleri dalı.

### Erişilebilirlik
- Sistem yazı boyutu %200'de kesilme yok (alt gezinme, takvim başlıkları, sekmeler, ayar satırları);
  büyük yazıda satırlar dikey düzene geçer.
- "Animasyonları kaldır" açıkken dolum, çubuk, ölçüt ve dalga animasyonları durur.
- İki temada metin/zemin kontrastı WCAG AA (otomatik test).

### Düzeltildi
- Su bardağına ya da "Aldım"a hızlı art arda dokunuşta kayıt kaybı ve stokun iki kez düşmesi
  (yazmalar sıraya alındı).
