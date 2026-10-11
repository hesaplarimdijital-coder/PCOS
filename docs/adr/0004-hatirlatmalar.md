# ADR-0004: Hatırlatmalar — AlarmManager tam alarm + setWindow yedeği, doz kayıt defteri

- Tarih: 2026-10-11 · Durum: Kabul edildi

## Bağlam
Inofolic Combi günde iki doz (09:00/21:00) ve doğum kontrol hapı (22:00) için dakikası dakikasına hatırlatma
ürünün çekirdeğidir. Android 14+'ta `SCHEDULE_EXACT_ALARM` varsayılan kapalı; Android 17 arka plan
kısıtlarını sıkılaştırdı. WorkManager en az 15 dk gecikebilir; kalıcı ön plan servisi pil ve mağaza politikası
açısından kabul edilemez.

## Karar
- `ReminderScheduler`: izin varsa `setExactAndAllowWhileIdle`, yoksa 10 dk pencereli `setWindow`; kullanıcıya
  bilgi bandı ve "Alarm iznini aç" eylemi (Bugün, Ayarlar, Karşılama).
- Her doz için benzersiz data URI `ayla://reminder/dose/<ilaçId>/<HH:mm>` → ayrı PendingIntent; su, günlük
  kontrol (09:00) ve akşam rutini için ayrı tür URI'leri.
- Kurulu doz anahtarları SharedPreferences kayıt defterinde tutulur; `rescheduleAll()` farkı alıp eskileri iptal eder.
- Yeniden kurulum: uygulama açılışı (`AylaApp`), `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED`, `TIME_SET`,
  `TIMEZONE_CHANGED`, `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED` (`SystemEventReceiver`).
- `ReminderReceiver` çaldığında bir sonraki günü planlar (zincir), PENDING doz kaydı açar, bildirimi gösterir;
  "Aldım/Ertele" eylemleri `ReminderActionReceiver`'da; receiver'lar `@AndroidEntryPoint` + `goAsync()`.
- Kanallar: ilaç (yüksek), su (düşük), döngü, ilerleme, uyku. Sessiz saatlerde yalnızca ilaç kanalı.
- Günlük kontrol alarmı: eski PENDING dozları MISSED yapar, 60/90. gün uyarısını tek kez gönderir, stok uyarısı.

## Sonuçlar
+ İzin varsa dakika hassasiyeti; yoksa en fazla 10 dk gecikme ve şeffaf bilgi. Pil dostu, servis yok.
− Üretici pil optimizasyonları (bazı OEM'ler) alarmı geciktirebilir; ayarlara yönlendirme MVP 2'de.
− Zincirleme planlama tek bir kaçırılmış alarmda bir günlük boşluk bırakabilir; açılış ve günlük kontrol telafi eder.

## Alternatifler
WorkManager (gecikme kabul edilemez), Foreground service (politika/pil), FCM (bulut yok).
