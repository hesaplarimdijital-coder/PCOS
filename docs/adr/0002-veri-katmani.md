# ADR-0002: Veri katmanı — Room + SQLCipher, Keystore sarmalı parola, DataStore

- Tarih: 2026-10-11 · Durum: Kabul edildi

## Bağlam
Adet, semptom, ilaç ve uyku verisi KVKK'da özel nitelikli kişisel veridir. Kullanıcı araştırmasının en güçlü
bulgusu "verim nerede?" kaygısı. Hesap, bulut ve analitik SDK istenmiyor.

## Karar
- Tek Room veritabanı `ayla.db` (sürüm 1, `exportSchema = true`, `app/schemas/`).
- SQLCipher (`net.zetetic:sqlcipher-android 4.18`) ile şifreli; 32 baytlık rastgele parola Android Keystore'da
  AES-GCM anahtarıyla sarılıp SharedPreferences'ta saklanır (`DatabaseKeyProvider`, alias `ayla.db.wrap.v1`).
- Tercihler Preferences DataStore (`ayla_prefs`); tek `UserPreferences` veri sınıfı.
- `allowBackup=false`, `dataExtractionRules` tüm alanları dışlar; yedek yerine kullanıcı kontrolünde JSON dışa
  aktarma (`DataExporter`, sürümlü şema) ve tam silme.
- Tarih/saat Room'da ISO metin, Instant epoch-millis (`Converters`).
- **Anahtar kaybı politikası:** sarmalı parola açılamazsa (Keystore anahtarı sistem güncellemesi/kilit
  sıfırlamasıyla geçersizleşmişse) asla sessizce yeni parola üretilmez — bu, dosyayı kalıcı olarak
  okunamaz kılıp uygulamayı çökme döngüsüne sokar. Bunun yerine `ayla.db*` dosyaları
  `ayla.db.kilitli-<zaman>` olarak karantinaya alınır, yeni anahtarla temiz başlanır ve kullanıcıya
  tek seferlik "önceki kayıtların açılamadı" diyaloğu gösterilir (`DatabaseKeyProvider.rotateAfterLoss`,
  `RootUiState.dataResetNotice`). Veritabanı dosyası varken sarmalı parola yoksa aynı yol izlenir.

## Sonuçlar
+ Cihaz kilidi + dosya şifresi iki katman; veri çalınsa da okunamaz. Kullanıcı taahhüdü "cihazda, şifreli".
− Yedekleme kapalı: cihaz değişiminde kullanıcı dışa/içe aktarma yapmalı (içe aktarma MVP 2).
− SQLCipher native kütüphanesi APK'yı ~7 MB büyütür; 16 KB sayfa uyumu yayın öncesi doğrulanacak.

## Alternatifler
EncryptedFile + Room şifresiz (reddedildi: sorgu başına şifre çözme yok, pratik değil); Jetpack Security
(durduruldu); bulut senkron (kapsam dışı, KVKK yurt dışı aktarım riski).
