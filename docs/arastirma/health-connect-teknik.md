# Health Connect (yalnızca uyku okuma): teknik kontrol listesi

- Tarih: 12 Ekim 2026 · İş: #11 · Politika: `docs/gizlilik-politikasi.md` · Play: `docs/play/health-connect-beyani.md`
- Bilgiler developer.android.com'dan bu tarihte doğrulandı; sayfa güncelleme tarihleri §10'da.
- Eski taslak: Health Connect manifesti ilk kod commit'inde (`6a1c78b`) vardı; depo kökündeki ilk commit
  (`8734084`) yalnızca README içerir. O taslaktan farkı: gerekçe hedefi `MainActivity` değil, ayrı bir etkinlik (§2).

## 1. Kararlar

| Konu | Karar | Neden |
|---|---|---|
| İzin | Yalnızca `android.permission.health.READ_SLEEP` | Play "en az kapsam"; uyku kartı başka veri istemez. |
| Yazma | Yok (`WRITE_SLEEP` yok) | Elle girilen uyku Ayla'da kalır. |
| Geçmiş izni | Yok (`READ_HEALTH_DATA_HISTORY` yok) | Varsayılan pencere ilk izinden 30 gün öncesine kadar; kart son 7 geceyi gösterir. |
| Arka plan | Yok (`READ_HEALTH_DATA_IN_BACKGROUND` yok) | Okuma yalnızca ön planda (düğme; isteğe bağlı olarak ön plana gelişte); WorkManager kullanılmaz (CLAUDE.md). |
| Kütüphane | `libs.androidx.health.connect` = `connect-client:1.1.0` (katalogda) | Son kararlı sürüm (8 Ekim 2025). 1.2.0 alfa (1.2.0-alpha06, 26 Ağustos 2026) kullanılmaz. |
| minSdk | 28 değişmez | SDK API 26+, Health Connect uygulaması API 28+ destekler. |
| Gerekçe etkinliği | Ayrı `PermissionsRationaleActivity` | Tam gizlilik politikasını göstermek zorunda; kilit, `singleTask` ve sekme gezintisi araya girmemeli. |

## 2. Manifest

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <!-- Health Connect: yalnızca uyku okuma (#11). Başka android.permission.health.* izni eklenmez. -->
    <uses-permission android:name="android.permission.health.READ_SLEEP" />

    <!-- Android 11–13 (API 30–33): Health Connect APK'sı getSdkStatus için görünür olsun -->
    <queries>
        <package android:name="com.google.android.apps.healthdata" />
    </queries>

    <application ...>
        <!-- MainActivity ve alıcılar değişmez -->

        <!-- Health Connect izin ekranındaki "Gizlilik politikası" bağlantısı: Android 13 ve altı -->
        <activity
            android:name=".PermissionsRationaleActivity"
            android:exported="true"
            android:theme="@style/Theme.Ayla">
            <intent-filter>
                <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" />
            </intent-filter>
        </activity>

        <!-- Aynı ekran, Android 14 ve üstü. Hedef etkinlikten SONRA tanımlanmalı. -->
        <activity-alias
            android:name="ViewPermissionUsageActivity"
            android:exported="true"
            android:permission="android.permission.START_VIEW_PERMISSION_USAGE"
            android:targetActivity=".PermissionsRationaleActivity">
            <intent-filter>
                <action android:name="android.intent.action.VIEW_PERMISSION_USAGE" />
                <category android:name="android.intent.category.HEALTH_PERMISSIONS" />
            </intent-filter>
        </activity-alias>
    </application>
</manifest>
```

- [ ] `<activity-alias>` manifestte hedef etkinlikten sonra yazılır (hedef "declared before the alias in the
      manifest" olmalı).
- [ ] `PermissionsRationaleActivity`: `ComponentActivity`; Hilt, veritabanı ve Keystore'a dokunmaz;
      `AylaTheme` içinde **tam** gizlilik politikasını gösterir ("must display the same privacy policy you
      provide for your app in the Google Play Console"); uygulama kilidi ve `FLAG_SECURE` uygulanmaz (herkese
      açık metin); kapatınca `finish()` ile Health Connect'e döner. Gelen intent'ten veri okumaz.
- [ ] `MainActivity` kullanılmaz: `singleTask` Ayla'nın görevini öne getirir, uygulama kilidi açıksa kilit
      ekranı çıkar, `onNewIntent` eylemi yok sayar; kullanıcı politika yerine Bugün sekmesini görür.
- [ ] Politika metni uygulamada `res/raw/gizlilik_politikasi.md` olarak durur (uygulamanın internet izni yok).
      Tek kaynak: tercihen derlemede `docs/gizlilik-politikasi.md`'den üretilir (AGP
      `variant.sources.res.addGeneratedSourceDirectory`); kopyalanırsa iki dosyanın aynılığını bir JVM testi
      denetler. Desteklenecek Markdown alt kümesi: `#`/`##`/`###`, `- ` madde, `**kalın**`, boş satırla
      paragraf. Politika bu alt kümeyle yazıldı (tablo, kod, bağlantı yok).
- [ ] Ayarlar'daki "Gizlilik ve KVKK aydınlatma metni" satırı ve karşılamadaki bağlantı aynı ekranı açar.
- [ ] Yayın APK'sında izin denetimi: `apkanalyzer manifest permissions app-release.apk` çıktısında
      `android.permission.INTERNET` ve `READ_SLEEP` dışındaki `android.permission.health.*` izinleri olmamalı.
      Politika "internet izni yok" der; bunu CI'da bir adım olarak denetlemek önerilir. Aynı çıktı politikanın
      9. bölümündeki izin listesiyle karşılaştırılır (ör. biyometri kütüphanesinin eklediği `USE_FINGERPRINT`).

## 3. Kullanılabilirlik (getSdkStatus)

| Cihaz | Durum | Ayla ne yapar |
|---|---|---|
| Android 14+ (API 34+) | Health Connect sistem modülü; kurulum gerekmez | `SDK_AVAILABLE` ise devam. |
| Android 9–13 (API 28–33) | Play'den yüklenen Health Connect uygulaması gerekir | Uygulama yoksa ya da eskiyse `SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED` → Play Store'a yönlendir (`hc_update_required`). |
| Desteklenmeyen cihaz | `SDK_UNAVAILABLE` | `sleep_hc_unavailable` göster ya da giriş noktasını gizle (ürün kararı); elle giriş her zaman açık. |

```kotlin
const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

enum class HealthConnectAvailability { AVAILABLE, UPDATE_REQUIRED, UNAVAILABLE }

fun healthConnectAvailability(context: Context): HealthConnectAvailability =
    when (HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PACKAGE)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UPDATE_REQUIRED
        else -> HealthConnectAvailability.UNAVAILABLE
    }

/** Rehberdeki örnek: Health Connect'i Play Store'da açar. İnternet izni gerektirmez. */
fun openHealthConnectInPlayStore(context: Context) {
    val uri = "market://details?id=$HEALTH_CONNECT_PACKAGE&url=healthconnect%3A%2F%2Fonboarding".toUri()
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, uri)
                .setPackage("com.android.vending")
                .putExtra("overlay", true)
                .putExtra("callerId", context.packageName),
        )
    }
}
```

- [ ] `HealthConnectClient.getOrCreate(context)` yalnızca `SDK_AVAILABLE` iken çağrılır.
- [ ] Durum her ön plana gelişte yeniden okunur (kullanıcı Play'den güncelleyip dönebilir).

## 4. İzin isteme

Akış: dokunuş → izin var mı? → yoksa **Ayla'nın gerekçe ekranı** (Arayüz metinleri) → "Onaylıyorum, devam et"
→ Health Connect izin ekranı → sonuç. Gerekçe ekranı KVKK açık rıza adımıdır; Health Connect'in kendi
"Gizlilik politikası" bağlantısı ise §2'deki etkinliği açar. İkisi ayrı şeylerdir.

```kotlin
val SLEEP_PERMISSIONS = setOf(HealthPermission.getReadPermission(SleepSessionRecord::class))

suspend fun HealthConnectClient.hasSleepPermission(): Boolean =
    permissionController.getGrantedPermissions().containsAll(SLEEP_PERMISSIONS)

// Compose (ekran tarafı)
val izinIste = rememberLauncherForActivityResult(
    PermissionController.createRequestPermissionResultContract(),
) { verilen -> viewModel.onPermissionResult(verilen.containsAll(SLEEP_PERMISSIONS)) }
// Gerekçe ekranında onaydan sonra:
izinIste.launch(SLEEP_PERMISSIONS)
```

- [ ] İzin yalnızca kullanım anında istenir (karşılamada değil).
- [ ] **İki kez iptal:** kullanıcı izin ekranını art arda iki kez iptal ederse Health Connect ekranı bir daha
      göstermez; izin yalnızca Health Connect ayarlarından verilebilir. Ardışık iptaller DataStore'da sayılır
      (sağlık verisi değil; izin verilince sıfırlanır); 2'ye ulaşınca `hc_cancelled_twice_*` ve
      "Health Connect ayarlarını aç" gösterilir.
- [ ] Ayarlar bağlantısı: `Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)`; değeri Android
      sürümüne göre kütüphanede seçilir. `runCatching` ile başlatılır.

## 5. Okuma ve eşleme

```kotlin
suspend fun HealthConnectClient.readRecentSleep(now: Instant, days: Long = 30): List<SleepSessionRecord> {
    val filter = TimeRangeFilter.between(now.minus(Duration.ofDays(days)), now)
    val out = mutableListOf<SleepSessionRecord>()
    var pageToken: String? = null
    do {
        val response = readRecords(ReadRecordsRequest(SleepSessionRecord::class, filter, pageToken = pageToken))
        out += response.records
        pageToken = response.pageToken
    } while (!pageToken.isNullOrEmpty()) // bazı sürümler null yerine "" döndürür
    return out
}

fun SleepSessionRecord.toSleepEntity(fallbackZone: ZoneId): SleepEntity = SleepEntity(
    date = endTime.atZone(endZoneOffset ?: fallbackZone).toLocalDate(), // uyanılan gün
    startAt = startTime,
    endAt = endTime,
    quality = null,
    source = SleepSource.HEALTH_CONNECT,
    externalId = metadata.id,
)
```

- [ ] Sorgu başlangıcı en fazla 30 gün geride. Varsayılan pencere "ilk izinden 30 gün öncesine kadar"dır;
      izin sonrası yeni kayıtlar okunmaya devam eder. Pencere dışı okuma için rehber hata verir derken 1.1.0-alpha11
      notu hatanın yalnızca tek kayıt okumasında olduğunu söyler; iki duruma da güvenme, başlangıcı sabitle.
- [ ] Yinelenen kayıt: `SleepRepository.importExternal` `externalId` ile eler (hazır).
- [ ] Saklanmayanlar: `stages`, `title`, `notes`. Kayıt içeriği log'a yazılmaz.
- [ ] Şekerleme de bir oturumdur; "dün gece" kartını bozmaması için ürün kuralı gerekir (ör. aynı uyanış
      gününde en uzun oturum gösterilir). **Karar: ürün sahibi.**
- [ ] Atıf (UI kılavuzu "should"): kaynak uygulamanın adı `metadata.dataOrigin.packageName`'den
      `PackageManager` ile alınır (rehber `QUERY_ALL_PACKAGES` gerekmediğini söyler; `NameNotFoundException`
      olursa yalnızca "Health Connect" yazılır). Saklanacaksa `sleep_sessions`'a `origin_package` sütunu →
      Room sürüm 2 + `Migration(1, 2)` + `app/schemas/.../2.json`; dışa aktarma şemasına varsayılanlı alan.
      **Karar: ürün sahibi**; uygulanmazsa politikadaki "kaydı oluşturan uygulamanın adı" ibaresi çıkarılır.
- [ ] Okuma ön planda yapılır; kullanıcı okuma sırasında uygulamadan çıkarsa işlem yarıda kalabilir:
      sonuç "sonra dene" olarak gösterilir, ön plan hizmeti kullanılmaz.
- [ ] Eşitleme tetikleyicisi: yalnızca düğme ya da ek olarak ön plana gelişte (değişiklik belirteci tasarımı
      için bkz. `docs/arastirma/mvp2-teknik-notlar.md` §3). Politika 6. bölüm ikisini de kapsar ("okuyabilir");
      Play gerekçesi seçilen davranışa göre yazılır (`docs/play/health-connect-beyani.md` §3.2).
- [ ] Health Connect'te silinen kaydın Ayla'ya yansıtılması (`DeletionChange` ile `externalId` silme) isteğe
      bağlıdır; politika yansıtma vaat etmez ("Ayla'dan ayrıca silmen gerekebilir"). Uygulanırsa metin
      sadeleştirilebilir.
- [ ] Hatalar: `SecurityException` → izin yok (§6); `IllegalStateException` → servis yok, geçersiz istek ya da
      kota (rehber örneği kotayı da bununla yakalar) → bekle ve `hc_import_failed`; `RemoteException`,
      `IOException` → `hc_import_failed`. Hiçbiri sessizce yutulup "yeni gece yok" diye gösterilmez.

## 6. İptal edilen izin ve bağlantıyı kesme

- [ ] Her okumadan önce `hasSleepPermission()`; izin okuma sırasında da kalkabilir (`SecurityException`).
- [ ] İzin kapalıysa: `hc_revoked_text` + "Health Connect ayarlarını aç"; önceden alınan geceler kalır
      (politika 6. bölüm). Öneri: "Health Connect'ten alınan geceleri sil" eylemi
      (`SleepDao`'ya `source = HEALTH_CONNECT` silme sorgusu; Health Connect'teki asıl kayıtlar değişmez).
- [ ] Ayarlar'da Health Connect bölümü: bağlantı durumu, "Health Connect ayarlarını aç" (izin yönetimi),
      isteğe bağlı "Bağlantıyı kes" = `permissionController.revokeAllPermissions()`. Rehber uyarısı: bu değişiklik
      uygulama yeniden başlatılana dek Health Connect'te görünmeyebilir; kullanıcıya söylenir (`hc_disconnect_text`)
      ve Health Connect ayarlarına gitme seçeneği verilir.
- [ ] Uygulama kaldırılınca tüm izinler (geçmiş izni dahil) geri alınır; yeniden kurulumda 30 günlük pencere
      yeni izin tarihinden hesaplanır.
- [ ] Ayla Health Connect'te hiçbir kaydı silmez; "Tüm verilerimi sil" yalnızca Ayla'daki kopyaları siler.

## 7. Veri alanı satırları (neden / nerede / ne kadar / nasıl silinir)

| Alan | Neden | Nerede | Ne kadar | Nasıl silinir |
|---|---|---|---|---|
| `sleep_sessions` (`source = HEALTH_CONNECT`): `start_at`, `end_at`, `date` | Elle saat girmeden uyku kaydı; Planım'da dün gece ve son 7 gece | SQLCipher veritabanı | Kullanıcı silene ya da uygulamayı kaldırana kadar | Planım "Son 7 gün" satırından; Tüm verilerimi sil; (öneri) "Health Connect'ten alınanları sil" |
| `external_id` (Health Connect `metadata.id`) | Aynı geceyi iki kez eklememek | Aynı | Aynı | Aynı |
| `origin_package` (öneri, Room v2) | Kaynak uygulama atfı | Aynı | Aynı | Aynı |
| Ardışık iptal sayacı (DataStore) | "İki kez iptal" ekranı | DataStore | İzin verilince sıfırlanır | Tüm verilerimi sil (tercihler sıfırlanır) |

Dışa aktarma bu geceleri `source` ve `externalId` ile içerir (kullanıcının başlattığı işlem; politika 7. bölüm).

## 8. Test ve doğrulama

- [ ] Saf JVM: `toSleepEntity` (gece yarısını aşan oturum, `endZoneOffset` null, saat dilimi değişimi),
      şekerleme kuralı, ardışık iptal sayacı.
- [ ] Health Connect'i bir arayüzün arkasına alıp ViewModel'i sahte kaynakla test et (izin yok, kullanılamıyor,
      güncelleme gerekli, `SecurityException`, boş sonuç, yinelenen kayıt). Resmî `connect-testing`
      (`FakeHealthConnectClient`) 1.0.0-alpha03 alfadır; eklenecekse katalog + ADR notu.
- [ ] Ekran görüntüleri (`EkranGoruntuleriTest`, örnek veri `Ornek.kt`): gerekçe ekranı, gizlilik politikası
      ekranı, güncelleme gerekli, kullanılamıyor, iki kez iptal, izin kapalı; her biri `y0N` serisinde
      (`sistemYaziOlcegi = 2f`) de.
- [ ] Cihaz: Android 13 (APK) ve 14+ (sistem) üzerinde Health Connect izin ekranındaki "Gizlilik politikası"
      bağlantısı Ayla'nın politika ekranını açar; iki kez iptal; Health Connect ayarlarından izni kaldırma;
      kaldır/yeniden kur; uygulama kilidi açıkken akış; R8 yayın derlemesi.

## 9. Açık kararlar (ürün sahibi)

1. Kaynak uygulama adını saklamak (Room v2) ya da politikadan çıkarmak.
2. Şekerleme / birden çok oturum kuralı.
3. "Health Connect'ten alınanları sil" ve Ayarlar'daki "Bağlantıyı kes" (politika 6. bölüm Ayarlar yolunu anar).
4. Politika metninin tek kaynaktan üretilmesi (derleme görevi) ya da kopya + eşitlik testi.
5. Eşitleme tetikleyicisi (yalnızca düğme / ön plana gelişte de) ve Health Connect'teki silmelerin yansıtılması.

## 10. Kaynaklar

- Get started with Health Connect (2026-09-08): https://developer.android.com/health-and-fitness/health-connect/get-started
- Aynı rehberin önceki sürümü, `SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED` ve Play Store yönlendirme örneği: https://developer.android.com/guide/health-and-fitness/health-connect/develop/get-started?hl=ar
- Read raw data: 30 gün kuralı, geçmiş ve arka plan okuma, istisna tablosu (2026-10-09): https://developer.android.com/health-and-fitness/health-connect/read-data
- Check for feature availability (2026-09-23): https://developer.android.com/health-and-fitness/health-connect/features/availability
- Track sleep sessions (2026-09-23): https://developer.android.com/health-and-fitness/health-connect/features/sleep-sessions
- Permissions and data access: iki kez iptal, `revokeAllPermissions` (2026-09-22): https://developer.android.com/health-and-fitness/health-connect/ui/permissions
- Data display and attribution (2026-09-22): https://developer.android.com/health-and-fitness/health-connect/ui/data
- Migrate from Android 13 (APK) to Android 14 (framework): `ACTION_HEALTH_CONNECT_SETTINGS` (2026-09-22): https://developer.android.com/health-and-fitness/health-connect/migration/android-13-to-14
- Health Connect Testing library (2026-05-11): https://developer.android.com/health-and-fitness/health-connect/test/unit-tests
- Jetpack Health Connect sürüm notları: https://developer.android.com/jetpack/androidx/releases/health-connect
- `<activity-alias>` öğesi: https://developer.android.com/guide/topics/manifest/activity-alias-element

## Arayüz metinleri

`strings.xml`'e girecek öneriler. Kesme işareti proje kuralına göre `&#8217;` yazıldı. Başlıklar ≤ 40,
paragraflar ≤ 220 karakter (`&#8217;` tek karakter sayıldı; en uzun paragraf 178). Var olan `sleep_import_hc`
("Health Connect'ten al") ve `sleep_hc_unavailable` aynen kullanılır.

Gerekçe ekranı (izin isteğinden önce; ne okunuyor, neden, nerede, paylaşılmıyor, nasıl geri alınır):

```text
hc_rationale_title: Uyku verini Health Connect&#8217;ten al
hc_rationale_what_title: Ne okunur?
hc_rationale_what: Ayla, Health Connect&#8217;ten yalnızca uyku oturumlarını okur ve her gecenin başlangıç ile bitiş saatini alır. Başka hiçbir veri türüne erişmez, Health Connect&#8217;e veri yazmaz.
hc_rationale_why_title: Neden?
hc_rationale_why: Yatış ve kalkış saatini elle girmen gerekmez. Alınan geceler Planım&#8217;daki uyku kartında elle girdiklerinle birlikte görünür. Ayla, Health Connect&#8217;i yalnızca uygulama açıkken okur.
hc_rationale_where_title: Nerede saklanır?
hc_rationale_where: Geceler yalnızca bu telefonda, diğer kayıtlarınla birlikte şifreli veritabanında saklanır. Ayla&#8217;nın sunucusu yoktur; uygulamanın internet izni de yoktur.
hc_rationale_sharing_title: Kimseyle paylaşılmaz
hc_rationale_sharing: Uyku verin kimseye aktarılmaz, satılmaz, reklam için kullanılmaz; biz de göremeyiz. Yalnızca sen dışa aktarırsan oluşturduğun dosyada yer alır.
hc_rationale_revoke_title: Nasıl geri alınır?
hc_rationale_revoke: İzni istediğin an Health Connect ayarlarından ya da Ayla&#8217;nın Ayarlar ekranından kapatabilirsin. Daha önce alınan geceler sen silene kadar Ayla&#8217;da kalır.
hc_rationale_consent: Uyku oturumlarımın Health Connect&#8217;ten okunup yalnızca bu cihazda saklanmasına açık rıza veriyorum.
hc_rationale_accept: Onaylıyorum, devam et
hc_rationale_decline: Şimdi değil
hc_rationale_policy: Gizlilik politikasının tamamı
```

Durumlar:

```text
hc_update_required: Health Connect&#8217;in yüklenmesi ya da güncellenmesi gerekiyor. Google Play&#8217;de bunu yaptıktan sonra yeniden dene.
hc_update_action: Google Play&#8217;i aç
hc_cancelled_twice_title: İzin artık Health Connect ayarlarında
hc_cancelled_twice_text: İzin penceresini iki kez kapattığın için Health Connect onu yeniden göstermiyor. İstersen Health Connect ayarlarından Ayla&#8217;ya uyku izni verebilirsin.
hc_open_settings: Health Connect ayarlarını aç
hc_revoked_text: Health Connect izni kapalı; yeni gece alınmıyor. Daha önce alınan geceler sen silene kadar Ayla&#8217;da kalır.
hc_import_done: %1$d gece eklendi
hc_import_nothing_new: Eklenecek yeni gece yok
hc_import_failed: Health Connect&#8217;e şu an ulaşılamadı. Biraz sonra yeniden dene.
hc_source: %1$s · Health Connect
hc_disconnect: Health Connect bağlantısını kes
hc_disconnect_text: Ayla artık yeni uyku verisi okumaz. Bu değişiklik Health Connect ayarlarında ancak Ayla yeniden açıldığında görünür.
hc_delete_imported: Health Connect&#8217;ten alınan geceleri sil
hc_delete_imported_confirm: Health Connect&#8217;ten alınan %1$d gece Ayla&#8217;dan silinir. Health Connect&#8217;teki asıl kayıtlar değişmez.
```

Gizlilik politikası bağlantıları ve karşılamadaki açık rıza (politika 4. bölüm bunlara dayanır):

```text
privacy_policy_title: Gizlilik politikası
settings_privacy_policy: Gizlilik ve KVKK aydınlatma metni
onboarding_policy_link: Gizlilik politikasını ve KVKK aydınlatma metnini oku
onboarding_health_consent: Sağlık verilerimin (adet, semptom, ilaç, uyku) yalnızca bu cihazda işlenmesine açık rıza veriyorum.
```

Rıza onayı önceden işaretli olmaz; kullanıcının kendisi işaretler ya da onay düğmesine basar. Aydınlatma
(politika) ile rıza cümlesi ayrı öğelerdir; rıza cümlesi politika metninin içine gömülmez.
