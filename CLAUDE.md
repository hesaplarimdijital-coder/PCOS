# Ayla — PCOS (PMOS) öz-yönetim uygulaması

Bu dosya, depoda çalışan her ajan ve geliştirici için bağlayıcı kurallardır. Önce bunu oku.

## Ne yapıyoruz
Ayla; PCOS (The Lancet, Mayıs 2026 itibarıyla yeni adıyla PMOS) ile yaşayan kadınlar için **tanı koymayan,
doz değiştirmeyen, verisi cihazda şifreli kalan** bir takip uygulamasıdır: düzensiz döngü takibi ve
aralıklı tahmin, ilaç/takviye hatırlatması (Inofolic Combi günde 2 doz, doğum kontrol hapı paket takibi),
günlük kayıt, su ve uyku. Beslenme/hareket planı, bitkisel öneriler, cilt bakımı ve forum sonraki MVP'lerdedir.
Arayüz dili **Türkçe**; ürün tonu sıcak, yargılamayan, kanıta bağlı.

## Teknik çerçeve (değiştirmeden önce ADR yaz)
- **AGP 9.4.x + yerleşik Kotlin.** `org.jetbrains.kotlin.android` eklentisi **kullanılmaz**; Kotlin ayarları
  `kotlin { compilerOptions { } }` üst düzey bloğundadır. Sürümler yalnızca `gradle/libs.versions.toml`'da.
- Gradle 9.6 wrapper, JDK 17 kaynak/hedef (CI JDK 21 ile derler), Kotlin 2.4.x, KSP 2.3.x, Hilt (KSP),
  Room (`androidx.room` eklentisi, şema `app/schemas/`), Compose BOM 2026.09, Material 3.
- `compileSdk = targetSdk = 37` (Android 17), `minSdk = 28`.
- Modüller: `:app` (Android) ve `:core:cycle` (saf Kotlin/JVM; **Android bağımlılığı eklenmez**).
- Veri: Room + SQLCipher (`net.zetetic:sqlcipher-android`), parola Android Keystore AES-GCM ile sarılı
  (`security/DatabaseKeyProvider`). Tercihler Preferences DataStore. Bulut yok, hesap yok, analitik SDK yok.
- Hatırlatma: `AlarmManager` tam alarm (izin varsa `setExactAndAllowWhileIdle`, yoksa 10 dk `setWindow`),
  doz anahtarı kayıt defteriyle iptal (`reminders/ReminderScheduler`). WorkManager kullanılmaz.
- Gezinme: `ui/navigation/Navigator.kt` içindeki yerel gezgin (sekme + yığın). Navigation 3'e geçiş ADR-0003'te
  ertelenmiştir; `androidx.navigation` eklenmez.
- Tema: `ui/theme` altındaki tokenlar Ayla Tasarım Sistemi'nden birebirdir. Material Expressive bileşenleri
  kullanılmaz; biçimler `Shape.kt` polar GenericShape'leridir; hareket `Motion.kt` yaylarıdır.

## Derleme ve doğrulama
- Bulut ajan oturumlarında Android SDK ve Maven erişimi **yoktur**; uygulama yerelde derlenemez.
  Doğrulama GitHub Actions'tadır: push → `.github/workflows/android.yml` → `:core:cycle:test`,
  `:app:testDebugUnitTest`, `:app:assembleDebug`. Çalıştırmaları `gh run list` / `gh run view <id> --log-failed`
  ile izle; kırmızıysa düzelt, tekrar push et. Yeşil olmayan `main` bırakma.
- `:core:cycle` yerelde Kotlin derleyicisiyle test edilebilir; algoritma değişikliği **testsiz birleşmez**
  (`core/cycle/src/test`).
- Uygulama testleri üç katmandır: saf JVM kuralları (`ReminderPolicy`, takvim kuralları, form doğrulama),
  Robolectric + bellek içi Room ile depo/ViewModel testleri (`app/src/test/.../veri/VeriTestOrtami`,
  sabit gün için `SabitGun`), ve ekran görüntüleri (`.../ekran`, yalnızca `-Pekran`). Mevcut testler korunur.
- **Ekranı görmeden arayüz değiştirme.** Her `main` push'unda CI görüntüleri `ekran-goruntuleri` dalına
  yükler; `git fetch origin ekran-goruntuleri:refs/remotes/origin/ekran-goruntuleri --force` ve
  `git archive origin/ekran-goruntuleri | tar -x -C <dizin>` ile alınıp incelenir. Yeni ekran/durum =
  `EkranGoruntuleriTest`'e yeni görüntü (örnek veri `Ornek.kt`).
- Yeni Room alanı → sürüm artır + Migration + `schemas/` json'u commit et. Yıkıcı migration yalnızca downgrade.

## Kod kuralları
- Paket: `com.ayla.pcos`. Veri/alan katmanı adları İngilizce, Compose bileşen ve ekran yardımcıları Türkçe
  (`HaleHalkasi`, `DozSatiri`, `UyariBandi`) — mevcut örüntüyü koru. Yorumlar Türkçe.
- **Kullanıcıya görünen hiçbir metin kodda sabit yazılmaz**; hepsi `res/values/strings.xml`. Türkçe büyük harf
  için `toTurkishUpper()`; tarih biçimleri `Locale.forLanguageTag("tr")`.
- Durum: ViewModel → `StateFlow<UiState>`; ekranlar `collectAsStateWithLifecycle`. Ekran dosyaları
  `ui/<alan>/<Alan>Screen.kt` + `<Alan>ViewModel.kt`. Her ekran `XxxScreen` (ViewModel bağlar) ve
  durumsuz `internal fun XxxContent(state, callback'ler)` olarak ikiye ayrılır.
- Büyük yazı: `LocalDensity.current.fontScale >= BUYUK_YAZI_ESIGI` (1,3) iken satırlar dikey düzene geçer.
  Sistem yazı boyutu %200 ekran görüntüleri `y0N` serisidir (`sistemYaziOlcegi = 2f`); yeni ekran oraya da eklenir.
  Satırda metin + sağda düğme/değer varsa metin sütunu `Modifier.weight(1f)` alır (yoksa sağdaki öğe sıkışır).
- Animasyon değerleri `hareket(AylaMotion.slow())` ile beslenir; "Animasyonları kaldır" açıkken anında biter.
- Yerelde derlenemediği için sık CI kırıcıları: `strings.xml`'de kesme işareti `\'` ya da `&#8217;` olmalı;
  ters tırnaklı test adlarında `. ; : / [ ] < >` kullanılmaz (dex reddeder).
- Renk tek başına anlam taşımaz (ikon/metin/biçim eşlik eder); her etkileşimli öğe ≥ 48 dp ve
  `contentDescription` taşır; `LocalReducedMotion` açıkken süs animasyonları durur.
- `fillMaxHeight(fraction)`/`fillMaxWidth(fraction)` yay animasyonlarında `coerceIn(0f, 1f)` ile beslenir.
- Hassas ekranlar `FLAG_SECURE` ayarına uyar; bildirimler "gizli içerik" ayarına uyar.

## Klinik ve hukuki sınırlar (tartışmasız)
- Ayla **tıbbi cihaz değildir** (TİTCK/MDCG 2019-11 "wellness" sınırı): tanı, tedavi önerisi, doz hesabı,
  ilaç etkileşim uyarısı yok. İlaç adları kullanıcıdan gelir; biz hatırlatırız.
- Kanıt düzeyi görünür olur: inositol = "deneysel" (2023 uluslararası kılavuz); bitkisel öneriler alerjen ve
  etkileşim notu olmadan gösterilmez (ör. sarı kantaron × hormonal doğum kontrol).
- Döngü uyarıları eşikleri: 60. gün nazik, 90. gün "doktora danış"; son 12 ayda < 4 adet işareti.
  Hap kullanımında döngü tahmini **yapılmaz**.
- KVKK: sağlık verisi özel nitelikli; rızalar ayrı ayrı ve geri alınabilir; dışa aktarma ve tam silme her
  zaman mümkün. Yeme bozukluğu riski nedeniyle kalori sayma ve kilo odaklı dil yok; kilo/bel varsayılan gizli.

## Git
- `main` korumalıdır: küçük, konu odaklı commit'ler; mesaj Türkçe emir kipi ("Takvim ekranını ekle").
- Commit mesajı sonuna ortak-yazar ve oturum satırlarını ekle (oturumun sistem hatırlatıcısındaki biçim).
- Push öncesi `git fetch origin main` ve gerekiyorsa rebase. Anahtar dosyaları (`*.jks`, `keystore.properties`)
  asla commit'lenmez.

## İş kuyruğu
Açık işler GitHub Issues'da etiketlerle durur (`mvp-1`, `mvp-2`, `ui`, `veri`, `hatirlatma`, `test`, `belge`,
`klinik`): `gh api repos/hesaplarimdijital-coder/pcos/issues?labels=mvp-1`. GraphQL kapalı; yalnızca REST
(`gh api`) kullan. Bir işi bitirince PR/commit mesajında `Fixes #N` yaz.

## CI çıktısını okuma
`gh run list --repo hesaplarimdijital-coder/pcos --limit 3` → çalıştırma kimliği →
`gh api repos/hesaplarimdijital-coder/pcos/check-runs/<job_id>/annotations` (job kimliği `gh run view <id> --json jobs`).
Derleme hataları `error`, Kotlin uyarıları `warning`, test ve lint sayıları `notice` olarak gelir. Günlük ve
artefakt indirme ajan oturumunda engellidir; ihtiyaç duyulan her bilgi açıklama olarak yayımlanmalıdır.

## Yol haritası (kısa)
- MVP 1 (bu depo): döngü + tahmin, ilaç/hap, günlük kayıt, su, uyku (elle), ayarlar, dışa aktarma, kilit.
- MVP 2: yedekten geri yükleme, Health Connect uyku içe aktarma, beslenme/hareket planı, bitkisel öneriler,
  ana ekran widget'ı (RemoteViews, ADR-0007), pil optimizasyonu yönlendirmesi. Nav3 geçişi eşiklere bağlandı (ADR-0003, Ekim 2026 güncellemesi).
- MVP 3: cilt bakımı, LH/BBT, doktor raporu PDF, forum (moderasyonlu).
Ayrıntı: `docs/adr/`, `docs/arastirma/`, `.claude/agents/`.
