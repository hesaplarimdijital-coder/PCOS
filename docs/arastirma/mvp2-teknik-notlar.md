# MVP 2 teknik notları: Glance widget, Health Connect, Navigation 3 sürümleri

- Tarih: 2026-10-12 · İlgili işler: #14 (widget), #11 (Health Connect), #16 (Nav3; karar için
  `docs/adr/0003-navigasyon.md` → "Güncelleme (Ekim 2026)")
- Yöntem: Bulut oturumunda Maven'a erişim yok (`dl.google.com` 403, POM/AAR okunamadı). Sürümler
  developer.android.com sürüm sayfalarından ve kılavuzlarından alındı. Bağımlılık ve oturum ayrıntıları AndroidX
  GitHub aynasındaki `androidx-main` dosyalarından okundu; bunlar yayımlanmış POM'larla birebir aynı olmayabilir.
  **(doğrulanamadı)** işaretli maddeler ilk CI çalıştırmasında ya da cihazda teyit edilir.

## 1. Sürümler

| Yapıt | Son kararlı | Daha yeni ön sürüm | Gereksinimler (minSdk · compileSdk/AGP · Kotlin) | Kaynak |
|---|---|---|---|---|
| `androidx.glance:glance-appwidget` | **1.2.0** (26 Ağu 2026) | 1.3.0-alpha02 (1 Tem 2026; tarihçe olarak 1.2.0'dan eski) | minSdk 23 (1.2.0-beta01'den beri varsayılan) · 1.2.0-alpha01'de compileSdk 35; 1.3.0-alpha01'de Compose compileSdk 37 → AGP ≥ 9.2 · Kotlin belirtilmemiş | [glance-sn] |
| `androidx.glance:glance-material3` | **1.2.0** (aynı `glance-*` yayını) | 1.3.0-alpha02 | glance-appwidget ile aynı | [glance-sn] |
| `androidx.glance:glance-testing`, `glance-appwidget-testing` | 1.2.0 olduğu varsayılır; sürüm notu yalnızca `glance-*` diyor, test kılavuzu örneği 1.1.1 **(doğrulanamadı)** | — | — | [glance-test] |
| `androidx.health.connect:connect-client` | **1.1.0** (8 Eki 2025); katalogdaki sürüm hâlâ son kararlı | 1.2.0-alpha06 (26 Ağu 2026); 1.2.x'in beta/rc'si yok | Kılavuz: SDK Android 8 (API 26)+, Health Connect uygulaması Android 9 (API 28)+; 1.2.0-alpha05'te minSdk 24 · compileSdk ve Kotlin belirtilmemiş | [hc-sn], [hc-start] |
| `androidx.navigation3:navigation3-runtime`, `navigation3-ui` | **1.2.0** (23 Eyl 2026); 1.1.x yama hattı 1.1.7 (26 Ağu 2026) | 1.3.0-alpha02 (7 Eki 2026) | Göç kılavuzu: minSdk 23, compileSdk ≥ 36; 1.2.0-alpha03'te compileSdk 37 · Kotlin belirtilmemiş; `rememberNavBackStack` için kotlinx-serialization eklentisi (uygulamada var) | [nav3-sn], [nav3-goc] |
| `androidx.lifecycle:lifecycle-viewmodel-navigation3` | **2.11.0** (17 Haz 2026) = katalogdaki `lifecycle` | 2.12.0-alpha04 (23 Eyl 2026) | minSdk 23 (2.10.0) · Compose compileSdk 37 → AGP ≥ 9.2; Compose UI ≥ 1.7.0 · Kotlin belirtilmemiş | [lc-sn] |
| (isteğe bağlı) `androidx.compose.material3.adaptive:adaptive-navigation3` | **1.3.0** (12 Ağu 2026) | 1.4.0-alpha03 (7 Eki 2026) | Belirtilmemiş | [adaptive-sn] |

Ayla ile uyum: hepsi minSdk 28, compileSdk/targetSdk 37, AGP 9.4.1 ve Kotlin 2.4.21 ile uyumlu görünür; Kotlin
gereksinimini hiçbir sürüm notu belirtmiyor (CI doğrular). Navigation 3 satırları ADR-0003 güncellemesi gereği
**şimdi eklenmez**; burada geçiş PR'ı için kayıt altındadır.

### 1.1 BOM ve geçişli bağımlılıklar
- Compose BOM 2026.09.00, Compose 1.12.1'e (animation, foundation) eşleniyor. BOM eşleme sayfasının okunabilen
  kısmında Glance, Navigation 3, Lifecycle ve Material 3 Adaptive yok; hepsi ayrı `version.ref` ister. [bom]
- `androidx-main` derleme dosyalarına göre (yayımlanmış 1.2.0 POM'ları **doğrulanamadı**):
  - `glance-appwidget`: `compose.runtime 1.7.8`, `ui-graphics`/`ui-unit 1.6.0`, `datastore*` 1.0.0,
    `core-ktx 1.7.0`, **`work-runtime-ktx 2.11.2`**. WorkManager dışındakiler BOM/katalog sürümlerinin altında;
    Gradle yüksek olanı seçer ve BOM aşılmaz. **Yeni geçişli bağımlılık WorkManager'dır**: Glance oturumları
    WorkManager ile yürütür.
    ADR-0004'teki "WorkManager kullanılmaz" kuralı hatırlatmalar içindir; widget ADR'sinde (önerilen ADR-0007)
    bu istisna açıkça yazılmalıdır.
  - `glance-material3`: `compose.material3 1.0.0`, `runtime 1.1.1`, yani BOM'un altında.
  - `navigation3-ui` (1.3.0-alpha hattı): `animation`/`runtime` **`1.13.0-alpha03`**. Alpha hattı BOM'u alpha
    Compose'a yükseltir, kullanılmaz.
  - `lifecycle-viewmodel-navigation3`: `navigation3-runtime 1.0.1`'e sabitli; açıkça eklenen 1.2.0 seçilir.
- CI önerisi: bağımlılık ekleyen PR'da
  `./gradlew :app:dependencyInsight --configuration debugRuntimeClasspath --dependency androidx.compose.runtime:runtime`
  (ve `androidx.work`) özetini `::notice::` açıklaması olarak yayımla; ajan oturumu günlük indiremez.

### 1.2 `gradle/libs.versions.toml` önerisi
```toml
[versions]
glance = "1.2.0"
# healthConnect = "1.1.0"   # mevcut satır değişmez; 1.2.0 hâlâ alpha
# navigation3 = "1.2.0"     # ADR-0003: yalnızca geçiş PR'ında
# materialAdaptive = "1.3.0" # yalnızca liste-ayrıntı kararı verilirse

[libraries]
androidx-glance-appwidget = { group = "androidx.glance", name = "glance-appwidget", version.ref = "glance" }
androidx-glance-material3 = { group = "androidx.glance", name = "glance-material3", version.ref = "glance" }
androidx-glance-testing = { group = "androidx.glance", name = "glance-testing", version.ref = "glance" }
androidx-glance-appwidget-testing = { group = "androidx.glance", name = "glance-appwidget-testing", version.ref = "glance" }
# mevcut: androidx-health-connect = { group = "androidx.health.connect", name = "connect-client", version.ref = "healthConnect" }

# Geçiş PR'ında:
# androidx-navigation3-runtime = { group = "androidx.navigation3", name = "navigation3-runtime", version.ref = "navigation3" }
# androidx-navigation3-ui = { group = "androidx.navigation3", name = "navigation3-ui", version.ref = "navigation3" }
# androidx-lifecycle-viewmodel-navigation3 = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-navigation3", version.ref = "lifecycle" }
# androidx-compose-material3-adaptive-navigation3 = { group = "androidx.compose.material3.adaptive", name = "adaptive-navigation3", version.ref = "materialAdaptive" }
```
`app/build.gradle.kts`: `implementation(libs.androidx.glance.appwidget)`, `implementation(libs.androidx.glance.material3)`,
`testImplementation(libs.androidx.glance.testing)`, `testImplementation(libs.androidx.glance.appwidget.testing)`,
`implementation(libs.androidx.health.connect)`. `glance-material3` yalnızca `ColorProviders(light, dark)` ile
`AylaLightColorScheme`/`AylaDarkColorScheme` tokenlarını tek kaynaktan kullanmak için eklenir (bkz. 2.7).

## 2. Glance widget (#14)

### 2.1 İskelet
- `widget/AylaWidget.kt` (`GlanceAppWidget`), `widget/AylaWidgetReceiver.kt`
  (`GlanceAppWidgetReceiver`; `override val glanceAppWidget = AylaWidget()`), `widget/WidgetState.kt`
  (veri modeli), `widget/WidgetIcerigi.kt` (durumsuz Glance composable'ları; `XxxContent` kuralının widget
  karşılığı, testler bunu çizer), `widget/WidgetUpdater.kt` (`updateAll` çağrılarını toplar).
- Alıcıda `goAsync` çağrılmaz; `onReceive`/`onUpdate` geçersiz kılınırsa `super` çağrılır (Glance kendi
  asenkron yolunu kullanır). Alıcıya `@AndroidEntryPoint` gerekmez (bkz. 2.3). [glance-create]
- Manifest: `<receiver android:name=".widget.AylaWidgetReceiver" android:exported="false">` +
  `APPWIDGET_UPDATE` intent-filter'ı + `android.appwidget.provider` meta-data'sı. Kılavuz iki örnekte `true` ve
  `false` gösteriyor; sistem (AppWidgetService) yayınları dışa kapalı alıcıya da ulaştığı için `false`
  önerilir **(cihazda doğrulanmalı)**.
- Sağlayıcı bilgisi (`res/xml/ayla_widget_info.xml`, API 28–35):
  ```xml
  <appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
      android:initialLayout="@layout/glance_default_loading_layout"
      android:description="@string/widget_description"
      android:previewLayout="@layout/widget_preview"
      android:previewImage="@drawable/widget_preview"
      android:targetCellWidth="2" android:targetCellHeight="2"
      android:minWidth="120dp" android:minHeight="110dp"
      android:minResizeWidth="120dp" android:minResizeHeight="110dp"
      android:resizeMode="horizontal|vertical"
      android:updatePeriodMillis="0"
      android:widgetCategory="home_screen" />
  ```
  `res/xml-v36/ayla_widget_info.xml` aynıdır, yalnızca `android:widgetCategory="home_screen|not_keyguard"`
  (gerekçe 2.6). `updatePeriodMillis="0"` periyodik güncellemeyi kapatır; tetikler 2.4'tedir (kılavuz: en sık
  30 dk, önerilen saatte birden seyrek). Önizleme düzeni ve görseli **örnek veri** içerir.

### 2.2 `SizeMode.Responsive`: 2×2 ve 4×2
- Resmî tahmin (Pixel 4, 5×4 ızgara): dikey `(73n − 16) × (118m − 16)` dp, yatay `(142n − 15) × (66m − 15)` dp.
  Buna göre 2×2 ≈ 130×220 (dikey) / 269×117 (yatay), 4×2 ≈ 276×220 / 553×117. Genişlik için dikey, yükseklik
  için yatay hücre değerleri kullanılır. [layouts]
- Responsive kipte sistem, verilen boyutlardan en az bozulmayla sığan en büyüğünü seçer; hiçbiri sığmazsa en
  küçüğünü kullanır. Android 11 ve altında her boyut için ayrı çizim yapılır. [glance-ui]
- Öneri: `KUCUK = DpSize(120.dp, 110.dp)` (2×2'ye iki yönelimde de sığar), `GENIS = DpSize(270.dp, 110.dp)`
  (4×2 dikeyde 276 ≥ 270; 2×2 yatayda 269 < 270, küçük düzende kalır).
  `override val sizeMode = SizeMode.Responsive(setOf(KUCUK, GENIS))`, `previewSizeMode` da aynı küme.
  Değerler Pixel 4 formülünden türetildi; cihazda/tablette doğrulanmalı.
- İçerik önerisi: KUCUK'ta döngü günü (hap kullanıcısında paket günü) + sıradaki doz saati + "Aldım"; GENIS'te
  ek olarak ilaç adı ve su (bardak/hedef). Metinler sp ile ölçeklenir; %200 yazıda taşmayı önlemek için
  KUCUK'ta en fazla iki satır ve `maxLines` kullanılır.

### 2.3 Hilt bağımlılıklarına erişim (EntryPoint)
`GlanceAppWidget` bir Android bileşeni değildir ve `updateAll` her çağrıda yeni bir örnek oluşturabilir. Bu
yüzden bağımlılıklar alıcıya enjekte edilmez, EntryPoint ile alınır. [hilt-ep]
```kotlin
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun stateSource(): WidgetStateSource // @Inject constructor, kapsamsız (durum tutmaz → @Singleton değil)
}

override suspend fun provideGlance(context: Context, id: GlanceId) {
    // provideGlance ana iş parçacığında çalışır. Bağımlılığın ilk çözümü AppModule.provideDatabase'i tetikler
    // (SQLCipher loadLibrary + Keystore'dan parola çözme), bu yüzden IO'da yapılır.
    val source = withContext(Dispatchers.IO) {
        EntryPointAccessors.fromApplication(context, WidgetEntryPoint::class.java).stateSource()
    }
    val initial = withContext(Dispatchers.IO) { source.current() }
    provideContent {
        val state by source.observe().collectAsState(initial)
        WidgetIcerigi(state)
    }
}
```
`WidgetStateSource` mevcut depoları (`CycleRepository`, `MedicationRepository`, `HydrationRepository`,
`UserPreferencesRepository`, `DateTicker`) birleştirir; Context gerekiyorsa `@ApplicationContext` kullanır.

### 2.4 Veri akışı ve güncelleme tetikleri
- **Kaynak:** şifreli Room ve DataStore. Widget kendi durumunu tutmaz. `stateDefinition` varsayılanı
  `PreferencesGlanceStateDefinition`; bu, widget durumunu **şifresiz** bir DataStore dosyasında tutar. Ayla
  durum yazmayacağı için `override val stateDefinition: GlanceStateDefinition<*>? = null` ile bu yol kapatılır
  (imza nullable; `currentState`/`updateIf` kullanılmaz). `null` ile çalışma **cihazda doğrulanmalı**.
- **Oturum süresi:** `provideContent` içindeki `collectAsState` yalnızca oturum açıkken yeniden çizer. Oturum
  ilk kompozisyondan sonra yaklaşık 45 sn yaşar (`initialTimeout = 45 s`, `additionalTime = 5 s`,
  `idleTimeout = 5 s`; `androidx-main` `SessionWorker`, 1.2.0'da aynı olduğu varsayıldı). Sonrasındaki veri
  değişiklikleri ancak `update(context, id)` ya da `updateAll(context)` ile yansır. İkisi de askıya alan
  fonksiyondur ve her yerden çağrılabilir. [glance-update]
- **Tetikler:**

| Olay | Nerede | Not |
|---|---|---|
| Doz Aldım/Atla/Ertele (bildirim ya da widget) | `ReminderActionReceiver` | `goAsync` penceresinde (alıcı ≤ 10 sn), `markTaken`'dan sonra |
| Alarm çaldı (doz, su, günlük kontrol, akşam) | `ReminderReceiver` | Sıradaki doz değişir; zaten uyanık olunan ana denk gelir |
| Saat/dilim değişimi, açılış, uygulama güncellemesi | `SystemEventReceiver` | `TIME_SET`, `TIMEZONE_CHANGED`, `BOOT_COMPLETED`, `MY_PACKAGE_REPLACED` |
| Gün dönümü | Yeni: uyandırmayan `AlarmManager.set(RTC, gece yarısı)` zinciri (`ReminderScheduler`) | Döngü günü ve günün dozları değişir. Tam alarm izni gerekmez, WorkManager kullanılmaz |
| Uygulama içi yazmalar (UI) | Uygulama kapsamlı toplayıcı: widget durum akışı `drop(1)` + kısa birleştirme + `distinctUntilChanged()` → `updateAll` (`AylaApp` başlatır) | Her depoya dokunmadan tüm UI yazmalarını yakalar |
| Tam silme, yedekten geri yükleme, anahtar kaybı sıfırlaması, gizlilik ayarı değişimi | İlgili akışın sonunda açık çağrı | KVKK: silinen veri widget'ta görünmeye devam etmemeli |

- Toplayıcı tek başına yetmez: alıcı `finish()` ettikten sonra süreç önbelleğe alınıp askıya alınabilir ve Room
  geçersizleştirmesi asenkrondur. Bu yüzden alıcı yollarında açık çağrı yapılır. `@Singleton` yalnızca
  toplayıcı/birleştirici gerçekten tek örnek isterse kullanılır.
- `AylaApp.onCreate` her süreç başlangıcında `rescheduleAll()` çalıştırır. Widget kaynaklı soğuk başlatmalar da
  bunu tetikler; ucuzdur ama bilinmelidir.

### 2.5 Widget'tan eylem gönderme
- **Uygulamayı aç:** `actionStartActivity<MainActivity>(actionParametersOf(ActionParameters.Key<String>(MainActivity.EXTRA_OPEN) to "today"))`.
  Parametreler intent extra'sı olarak gelir, mevcut `EXTRA_OPEN` akışı değişmez. [glance-action]
- **"Aldım" → mevcut `ReminderActionReceiver`:**
  ```kotlin
  val intent = Intent(context, ReminderActionReceiver::class.java)
      .setAction(ReminderActionReceiver.ACTION_TAKEN)
      .putExtra(ReminderScheduler.EXTRA_MEDICATION_ID, dose.medicationId)
      .putExtra(ReminderScheduler.EXTRA_DATE, dose.date.toString())
      .putExtra(ReminderScheduler.EXTRA_TIME, dose.time.toString())
      .putExtra(ReminderActionReceiver.EXTRA_NOTIFICATION_ID, NotificationIds.dose(dose.medicationId, dose.time.toString()))
  Button(text = …, onClick = actionSendBroadcast(intent))
  ```
  - Glance yayın `PendingIntent`'ini istek kodu 0, `FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT` ile kurar.
    Benzersiz kimliği **yalnızca `intent.data == null` ise** ekler (API 29+ `Intent.setIdentifier`, altında
    `intent.data`). Bu yüzden `data` boş bırakılır; dolu verilirse extra'ları farklı iki "Aldım" aynı
    `PendingIntent`'e çökebilir. [glance-applyaction]
  - Alıcı `exported="false"` kalabilir (`PendingIntent` uygulamanın kimliğiyle gönderilir). `markTaken`
    idempotent olduğundan çift dokunuş stoku iki kez düşürmez. `EXTRA_NOTIFICATION_ID` ile o dozun bildirimi
    de kapanır.
  - Sonraki PR'da gereken küçük değişiklik: `ReminderActionReceiver` eylemden sonra
    `AylaWidget().updateAll(context)` çağırmalı.
- Lambda eylemleri (`onClick = { … }`) bir WorkManager worker'ında çalışır ve etkinlik başlatamaz;
  `actionRunCallback` ise alıcı mantığını çoğaltır. İkisi de kullanılmaz.
- Dikkat: `MainActivity` dışa açık. Bugün herhangi bir uygulama `EXTRA_OPEN="restore"` göndererek (kilit
  açıkken) içe aktarma seçicisini otomatik açtırabilir. Widget/bildirim hedefleri beyaz listede kalmalı,
  `"restore"` yalnızca içerideki `restoreRequested` bayrağından gelmeli.

### 2.6 Android 17, kilit ekranı ve gizlilik önerisi
- **Android 17 (hedef 37):** RemoteViews içindeki Bitmap ve Icon'ların toplam belleği
  `1,5 × ekran genişliği × ekran yüksekliği × 4` bayt ile sınırlı; aşılırsa `IllegalArgumentException` ile
  **süreç çöker**. Görseller `ImageProvider(R.drawable.…)` (vektör kaynak) ile verilir; bitmap gerekiyorsa
  küçük tutulur. [a17-target] Android 17 özellik sayfasında widget maddesi yok (1 Ekim 2026 güncellemesi).
  [a17-features] Remote Compose tabanlı "snap scrolling" Glance 1.3.0-alpha02 ve compileSdk 37 ister; alpha
  olduğu için kullanılmaz. [glance-ui]
- **Kilit ekranı widget'ları telefonlarda var.** Pixel telefonlara Android 16 QPR2 ile geldi (Aralık 2025;
  Settings > Display & touch > Lock screen > "Widgets on lock screen"). Google, AOSP'de tablet ve telefon
  desteğini "Android 16'dan sonraki sürüm (QPR1)" için duyurmuştu (Mart 2025). [aa-qpr2], [ls-faq] Google'a
  göre **tüm widget'lar varsayılan olarak** kilit ekranıyla uyumludur. Çıkış yolu `xml-v36` kaynak
  klasöründe `widgetCategory` değerine `not_keyguard` eklemektir
  (`AppWidgetProviderInfo.WIDGET_CATEGORY_NOT_KEYGUARD`, API 36'da eklendi). Kilit ekranından etkinlik açmak
  kimlik doğrulama ya da `showWhenLocked` ister. [ls-faq], [api36-diff]
- **Öneri (Ayla, KVKK'da özel nitelikli veri):**
  1. `res/xml-v36/` sürümünde `home_screen|not_keyguard` kullanılır; widget kilit ekranına eklenemez. #14'teki
     "kilit ekranında yalnızca 'Ayla' etiketi" seçeneği için barındırıcının `OPTION_APPWIDGET_HOST_CATEGORY`
     ile KEYGUARD bildirip bildirmediği **doğrulanamadı**. Cihaz testi yapılmadan çalışma zamanı algılamasına
     güvenilmez; OEM kilit ekranlarının `not_keyguard`'a uyumu da bilinmiyor.
  2. Widget uygulama kilidinin ve `FLAG_SECURE`'un dışındadır (başlatıcı sürecinde çizilir). `appLockEnabled`
     ya da yeni bir "widget'ta ayrıntıyı gizle" ayarı açıkken nötr içerik gösterilir: yalnızca "Ayla" ve
     "Uygulamayı aç"; ilaç adı, döngü günü ve tahmin görünmez. Varsayılanın `hideNotificationContent` ile
     hizalanması önerilir (ürün kararı; yeni DataStore anahtarı Room göçü gerektirmez).
  3. `hidePrediction` açıksa tahmin gösterilmez. Hap kullanıcısında döngü tahmini değil paket günü gösterilir.
     Kilo/bel bilgisi ve 60/90. gün uyarıları widget'ta hiç gösterilmez.
  4. Oluşturulan önizlemeler (API 35+, `providePreview` + `GlanceAppWidgetManager.setWidgetPreviews`, yaklaşık
     saatte 2 çağrı sınırı) sistemin widget seçicisinde saklanır ve çizilir. Kılavuz gerçek veri önerse de
     Ayla'da **yalnızca sabit örnek veri** kullanılır. `widgetCategories` varsayılanı HOME_SCREEN | KEYGUARD |
     SEARCHBOX olduğundan yalnızca `WIDGET_CATEGORY_HOME_SCREEN` verilir. API < 35 için `previewLayout` +
     `previewImage` kullanılır. [glance-preview], [glance-mgr]
  5. `stateDefinition = null` (bkz. 2.4).

### 2.7 Tasarım kısıtları (ADR-0006 ile ilişkisi)
- Yalnızca Glance composable'ları kullanılabilir (RemoteViews'a çevrilir). Canvas olmadığı için `HaleHalkasi`
  çizilemez; Glance `CircularProgressIndicator` ilerleme değeri almaz. Halka yerine metin ve belirli
  `LinearProgressIndicator(progress)` ya da düzey seviyeli bir vektör kaynak kullanılır. [glance-cpi]
- Uygulamanın kendi fontları desteklenmez (yalnızca sistem fontları), yani Bricolage Grotesque/Figtree
  kullanılamaz. `GlanceModifier.cornerRadius` yalnızca Android 12+ (S) sürümünde çalışır; API 28–30'da köşe için
  arka plan çizilebiliri gerekir. Polar `GenericShape` biçimleri kullanılamaz. [glance-ui], [glance-corner]
- Renk: dinamik renk kapalı (ADR-0006). `GlanceTheme(colors = ColorProviders(light = AylaLightColorScheme,
  dark = AylaDarkColorScheme))` (glance-material3) ile tokenlar tek kaynaktan gelir.
- Metinler `strings.xml`'den (`LocalContext.current.getString`) gelir. Türkçe büyük harf için `toTurkishUpper()`,
  tarih için `Locale.forLanguageTag("tr")` kullanılır. Dokunma hedefi ≥ 48 dp; her `Image` ve tıklanabilir öğe
  `contentDescription` taşır.
- Uygulama sırasında widget kararları için **ADR-0007** yazılmalı: WorkManager istisnası, gizlilik
  varsayılanları (`not_keyguard`, nötr içerik, örnek veriyle önizleme), güncelleme tetikleri,
  `stateDefinition = null`.

### 2.8 Test seçenekleri
- **Birim (JVM):** `glance-appwidget-testing` → `runGlanceAppWidgetUnitTest { … }`. Görüntü çizmez ve
  tıklama yapmaz; düğüm ve eylem eşleyicileri sunar (`onNode(hasTestTag(…)).assertHasText(…)`,
  `hasSendBroadcastAction<ReminderActionReceiver>()`, `hasStartActivityClickAction<MainActivity>()`,
  `assertHas…ClickAction`). Arayüz: `setAppWidgetSize(DpSize)`, `setContext(Context)` (composable
  `LocalContext` okuyorsa gerekir; `strings.xml` için Robolectric bağlamı), `provideComposable`, `setState`,
  `awaitIdle`. Varsayılan boyut 349×455 dp; Responsive kipte `KUCUK`/`GENIS` ayrı ayrı test edilir.
  Varsayılan zaman aşımı kılavuzda 1 sn, `androidx-main` KDoc'unda 10 sn yazıyor; değer açıkça verilir.
  [glance-test], [glance-unit-src]
  ```kotlin
  @Test fun `kucuk duzende siradaki doz saati gorunur`() = runGlanceAppWidgetUnitTest {
      setAppWidgetSize(AylaWidget.KUCUK)
      setContext(RuntimeEnvironment.getApplication()) // VeriTestOrtami ile aynı yol
      provideComposable { WidgetIcerigi(OrnekWidget.normal) }
      onNode(hasTestTag("siradaki_doz")).assertHasText("21:00")
  }
  ```
- **Ekran görüntüsü:** resmî bir Glance ekran görüntüsü aracı yok. `google/glance-experimental-tools`
  (appwidget-testing, `GlanceScreenshotTestActivity`) **18 Mayıs 2026'da arşivlendi**, bu yüzden kullanılmaz.
  [glance-exp] Olası yol: Robolectric'te `GlanceAppWidget.runComposition` (1.1.0+, RemoteViews akışı) ya da
  `compose` (1.0.0-beta01+) ile RemoteViews üretmek, `RemoteViews.apply(context, parent)` ile görünüme çevirmek
  ve Roborazzi `captureRoboImage()` ile kaydetmek. İmzalar ve Robolectric'in RemoteViews çizim doğruluğu
  (yuvarlak köşeler donanım hızlandırması ister) **doğrulanamadı**; küçük bir spike issue açılmalı. CLAUDE.md
  "ekranı görmeden arayüz değiştirme" kuralı gereği spike başarısız olursa ilk sürüm emülatör/cihaz görüntüsüyle
  incelenir. `glance-preview`/`glance-appwidget-preview` (1.0.0-alpha06, Şubat 2024) yalnızca Studio önizlemesi
  içindir; CI'da kullanılmaz.

## 3. Health Connect okuma (#11)
- **Sürüm:** `connect-client` 1.1.0 kalır. 1.2.0-alpha'daki `getChanges(token, pageSize)` ve eşleştirme
  (matchmaking) API'leri kullanılmaz. [hc-sn]
- **SDK durumu:** `HealthConnectClient.getSdkStatus(context, providerPackageName = varsayılan)`:
  `SDK_UNAVAILABLE` (1) → entegrasyon noktaları gizlenir; `SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED` (2) →
  sağlayıcı yok ya da eski (Android 13 ve altında Play'den Health Connect uygulaması gerekir) → yükleme
  yönlendirmesi; `SDK_AVAILABLE` (3) → `HealthConnectClient.getOrCreate(context)`. Android 14+'ta Health
  Connect çerçeve modülüdür; uygulama Android 9+'ı destekler, bu da minSdk 28 ile örtüşür. Play yönlendirme
  URI'si bu oturumda **doğrulanmadı**. [hc-start], [hc-client-src]
- **Manifest** (ilk commit'teki taslakla aynı): `android.permission.health.READ_SLEEP`;
  `<queries><package android:name="com.google.android.apps.healthdata" /></queries>`; dışa açık gerekçe
  Activity'si (`androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE`); Android 14+ için `activity-alias`
  (`android.intent.action.VIEW_PERMISSION_USAGE` + `android.intent.category.HEALTH_PERMISSIONS`,
  `android:permission="android.permission.START_VIEW_PERMISSION_USAGE"`). Gerekçe ekranı Play Console'daki
  gizlilik politikasının aynısını gösterir. Arka plan ve geçmiş izinleri beyan edilmez.
- **İzin sözleşmesi:** `rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract())`
  + `setOf(HealthPermission.getReadPermission(SleepSessionRecord::class))`. Kullanıcı izni her an geri
  alabildiği için her okumadan önce `permissionController.getGrantedPermissions()` kontrol edilir. KVKK: rıza
  ayrı ve geri alınabilir; geri alınınca içe aktarılan kayıtları silme seçeneği sunulur.
- **Okuma kısıtları:** okuma yalnızca ön planda yapılır (arka plan için `READ_HEALTH_DATA_IN_BACKGROUND`,
  `FEATURE_READ_HEALTH_DATA_IN_BACKGROUND` kontrolü ve WorkManager gerekir; MVP 2'de yok). Başka uygulamaların
  verisi ilk izinden önceki 30 güne kadar okunabilir (Android 14+'ta uygulamanın kendi verisine sınır yok); daha
  eskisi `READ_HEALTH_DATA_HISTORY` ister ve istenmez. [hc-read]
- **Sayfalama:** `ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(bas, son), pageToken = token)`.
  `pageSize` varsayılanı 1000 (`> 0` zorunlu, belgelenmiş üst sınır yok), `ascendingOrder = true`. Döngü
  `response.pageToken.isNullOrEmpty()` olana dek sürer (Android 12/13 APK'sı bitişte `""` dönebilir). Kota
  hatası `IllegalStateException` olarak gelir ve geri çekilme uygulanır. [hc-read], [hc-req-src]
- Aralık kayıtlarında `TimeRangeFilter`'ın örtüşme kuralı okunan sayfalarda belgelenmemiş. Pencere 1 gün geniş
  tutulur ve `metadata.id` ile tekilleştirilir.
- **Eşleme:** `SleepEntity(date = endTime'ın yerel günü (varsa endZoneOffset ile), startAt, endAt,
  source = HEALTH_CONNECT, externalId = metadata.id)`. Evreler (`stages`) MVP 2'de saklanmaz. [hc-sleep]
- **Değişiklik belirteçleri:** `getChangesToken(ChangesTokenRequest(setOf(SleepSessionRecord::class)))` ile
  veri türü başına ayrı belirteç alınır; belirteç **30 gün** geçerlidir. `getChanges(token)` döngüsü
  `hasMore`/`nextChangesToken` ile ilerler. `UpsertionChange` geldiğinde `metadata.id` + `lastModifiedTime` ile
  upsert yapılır. `DeletionChange` yalnızca kimlik taşır; `externalId` saklandığı için kayıt silinebilir.
  Belirtecin süresi dolarsa son eşitleme zamanından (en fazla 30 gün) yeniden okunur ve `externalId` ile
  tekilleştirilir. Belirteç ve son eşitleme zamanı DataStore'da tutulur (opak değerlerdir, sağlık verisi
  değildir). Eşitleme Planım'daki düğmeyle ve uygulama ön plana gelince yapılır. [hc-sync], [hc-client-src]
- **Koddaki açık:** `SleepRepository.importExternal` yalnızca yeni `externalId` ekliyor; güncelleme ve silme
  yok. `sleep_sessions.external_id` üzerinde benzersiz indeks de yok. Upsert ve silme için indeks eklenirse
  Room sürümü 1'den 2'ye çıkar, Migration yazılır ve `schemas/2.json` commit edilir (CLAUDE.md).
- **Play:** Health Connect beyan formu ve gizlilik politikası URL'si gerekir.

## 4. Doğrulanamayanlar
- Yayımlanmış POM'lar (glance 1.2.0, navigation3 1.2.0, lifecycle-viewmodel-navigation3 2.11.0) okunamadı;
  bağımlılık listeleri `androidx-main`'den alındı. Nav3 1.2.0'ın Compose sabitlemesi bilinmiyor.
- `glance-testing`/`glance-appwidget-testing` için 1.2.0 sürümünün varlığı.
- Glance oturum süreleri ve `runGlanceAppWidgetUnitTest` varsayılan zaman aşımı (`androidx-main` ile kılavuz
  çelişiyor); `stateDefinition = null` davranışı; `compose`/`runComposition` imzaları.
- Kilit ekranı barındırıcısının `OPTION_APPWIDGET_HOST_CATEGORY` değeri; `not_keyguard`'ın OEM kilit
  ekranlarındaki davranışı; dışa kapalı widget alıcısının tüm başlatıcılarda çalışması.
- Robolectric + Roborazzi ile RemoteViews görüntü doğruluğu.
- Health Connect 1.1.0'ın minSdk/compileSdk değeri (sürüm notunda yok; kılavuz SDK için API 26 diyor); Play
  yükleme yönlendirme URI'si; aralık kayıtlarında `TimeRangeFilter` örtüşme kuralı.
- Widget boyutları Pixel 4 tahmin formülünden türetildi.
- Hiçbir sürüm notu Kotlin gereksinimi belirtmiyor; Kotlin 2.4.21 ile tüketim CI'da doğrulanır.

## Kaynaklar (12 Ekim 2026'da okundu)
[glance-sn]: https://developer.android.com/jetpack/androidx/releases/glance
[glance-create]: https://developer.android.com/develop/ui/compose/glance/create-app-widget
[glance-ui]: https://developer.android.com/develop/ui/compose/glance/build-ui
[glance-update]: https://developer.android.com/develop/ui/compose/glance/glance-app-widget
[glance-action]: https://developer.android.com/develop/ui/compose/glance/user-interaction
[glance-test]: https://developer.android.com/develop/ui/compose/glance/testing
[glance-preview]: https://developer.android.com/develop/ui/compose/glance/generated-previews
[glance-mgr]: https://github.com/androidx/androidx/blob/androidx-main/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/GlanceAppWidgetManager.kt
[glance-applyaction]: https://github.com/androidx/androidx/blob/androidx-main/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/action/ApplyAction.kt
[glance-cpi]: https://github.com/androidx/androidx/blob/androidx-main/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/CircularProgressIndicator.kt
[glance-corner]: https://github.com/androidx/androidx/blob/androidx-main/glance/glance-appwidget/src/main/java/androidx/glance/appwidget/CornerRadius.kt
[glance-unit-src]: https://github.com/androidx/androidx/blob/androidx-main/glance/glance-appwidget-testing/src/main/java/androidx/glance/appwidget/testing/unit/GlanceAppWidgetUnitTest.kt
[glance-exp]: https://github.com/google/glance-experimental-tools
[layouts]: https://developer.android.com/develop/ui/views/appwidgets/layouts
[hilt-ep]: https://dagger.dev/hilt/entry-points.html
[a17-target]: https://developer.android.com/about/versions/17/behavior-changes-17
[a17-features]: https://developer.android.com/about/versions/17/features
[ls-faq]: https://android-developers.googleblog.com/2025/03/widgets-on-lock-screen-faq.html
[aa-qpr2]: https://www.androidauthority.com/android-16-qpr2-lock-screen-widgets-pixel-phones-how-use-3621781/
[api36-diff]: https://developer.android.com/sdk/api_diff/36/changes/android.appwidget.AppWidgetProviderInfo
[hc-sn]: https://developer.android.com/jetpack/androidx/releases/health-connect
[hc-start]: https://developer.android.com/health-and-fitness/guides/health-connect/develop/get-started
[hc-read]: https://developer.android.com/health-and-fitness/guides/health-connect/develop/read-data
[hc-sync]: https://developer.android.com/health-and-fitness/guides/health-connect/develop/sync-data
[hc-sleep]: https://developer.android.com/health-and-fitness/health-connect/features/sleep-sessions
[hc-client-src]: https://github.com/androidx/androidx/blob/androidx-main/health/connect/connect-client/src/main/java/androidx/health/connect/client/HealthConnectClient.kt
[hc-req-src]: https://github.com/androidx/androidx/blob/androidx-main/health/connect/connect-client/src/main/java/androidx/health/connect/client/request/ReadRecordsRequest.kt
[nav3-sn]: https://developer.android.com/jetpack/androidx/releases/navigation3
[nav3-goc]: https://developer.android.com/guide/navigation/navigation-3/migration-guide
[lc-sn]: https://developer.android.com/jetpack/androidx/releases/lifecycle
[adaptive-sn]: https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive
[bom]: https://developer.android.com/develop/ui/compose/bom/bom-mapping

- Glance sürüm notları: [glance-sn] · widget oluşturma: [glance-create] · arayüz/SizeMode: [glance-ui] ·
  güncelleme: [glance-update] · eylemler: [glance-action] · test: [glance-test] · önizlemeler: [glance-preview]
- Glance kaynakları (androidx-main): [glance-mgr], [glance-applyaction], [glance-cpi], [glance-corner],
  [glance-unit-src]; arşivlenmiş deneysel araçlar: [glance-exp]
- Widget boyutları: [layouts] · Hilt EntryPoint: [hilt-ep]
- Android 17: [a17-target], [a17-features] · kilit ekranı: [ls-faq], [aa-qpr2], [api36-diff]
- Health Connect: [hc-sn], [hc-start], [hc-read], [hc-sync], [hc-sleep], [hc-client-src], [hc-req-src]
- Navigation 3 ve ilgili: [nav3-sn], [nav3-goc], [lc-sn], [adaptive-sn] · Compose BOM: [bom]
