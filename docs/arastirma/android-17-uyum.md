# Android 17 (API 37) uyum notları

Android 17 kararlı sürümü 16 Haziran 2026'da yayımlandı. Ayla `compileSdk = targetSdk = 37`, `minSdk = 28`.

## Uygulamayı etkileyen değişiklikler
- **Büyük ekranlarda yönlendirme/boyut kısıtları yok sayılır** → tüm ekranlar yeniden boyutlanabilir; sabit
  genişlik varsayımı yok, `fillMaxWidth` + `weight` düzenleri; tablet liste-ayrıntı MVP 2.
- **Arka plan kısıtları** (ses, bellek) → kalıcı servis yok; AlarmManager + kısa `goAsync()` receiver'ları (ADR-0004).
- **İçerik yakalama / FLAG_SECURE** → "Ekran görüntüsünü engelle" ayarı `MainActivity`'de pencere bayrağını yönetir.
- **Keystore anahtar sayısı sınırları** → tek sarmal anahtar (`ayla.db.wrap.v1`), ilaç başına anahtar üretilmez.
- **Tam alarm izni** (14+'tan beri varsayılan kapalı) → `setWindow` yedeği + bilgi bandı; izin durumu değişimi
  `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` ile dinlenir.
- **Bildirim izni** (13+) → karşılamada kullanım anında istenir; reddedilirse uygulama bildirimsiz çalışır.
- **Öngörücü geri** → `enableOnBackInvokedCallback=true`; Compose `BackHandler`.
- **Health Connect** → MVP 2'de: `READ_SLEEP` izni, `<queries>` (com.google.android.apps.healthdata),
  `ACTION_SHOW_PERMISSIONS_RATIONALE` intent-filter'ı ve Android 14+ için `VIEW_PERMISSION_USAGE`
  activity-alias'ı manifeste eklenir; Play Health Connect beyan formu ve gizlilik politikası URL'si gerekir.
  MVP 1'de kullanılmayan izin beyan edilmez (Play incelemesini gereksiz tetiklememek için).

## Google Play
- 31 Ağustos 2026'dan beri yeni uygulamalar/güncellemeler API ≥ 36 hedeflemeli → 37 ile karşılanır.
- 16 KB sayfa boyutu: SQLCipher 4.18 native kütüphaneleri uyumlu bildiriliyor; yayın öncesi `check-elf-alignment` ile doğrula.
- Veri güvenliği formu: "veri cihazda işlenir, paylaşılmaz, şifrelenir, kullanıcı silebilir".
- Sağlık uygulaması politikası: tıbbi tavsiye iddiası yok, kaynak gösterimi var.

## Türkiye
- KVKK 2024 değişiklikleri: özel nitelikli veri için açık rıza ya da yasal dayanak; cihaz içi işleme aktarım doğurmaz.
- TİTCK/MDCG 2019-11: takip ve hatırlatma "wellness" kapsamında; tanı/tedavi iddiası eklenirse sınıflandırma değişir.
