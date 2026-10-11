# Değişiklik günlüğü

Biçim [Keep a Changelog](https://keepachangelog.com/tr-TR/1.1.0/) ilkelerine, sürümleme
[Semantik Sürümleme](https://semver.org/lang/tr/)'ye uyar.

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
