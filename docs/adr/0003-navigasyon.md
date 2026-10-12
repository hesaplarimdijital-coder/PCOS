# ADR-0003: Gezinme — yerel sekme+yığın gezgini; Navigation 3 ertelendi

- Tarih: 2026-10-11 · Durum: Kabul edildi · Güncelleme 2026-10-12 (#16): yeniden değerlendirildi; geçiş,
  aşağıdaki eşiklerden biri gerçekleşene dek ertelendi (bkz. "Güncelleme (Ekim 2026)")

## Bağlam
Uygulamanın gezinme grafı küçük ve sabit: 5 sekme, 3 ayrıntı ekranı (günlük kayıt, ilaç ekle, ayarlar),
bildirimden hedef sekmeye açılma. Navigation 3 (`androidx.navigation3`) 2026'da kararlı sürüme ulaştı ancak
bulut oturumunda API imzaları doğrulanamıyor ve adaptif düzen (liste-ayrıntı) MVP 1'de yok.

## Karar
`ui/navigation/Navigator.kt`: `Screen` sealed arayüzü (`Tab` alt türleri + `DailyLog(date)`, `AddMedication`,
`Settings`), `AylaNavigator` (sekme, yığın, `selectTab/push/pop`, `canGoBack`) ve `rememberAylaNavigator()`
(`listSaver` ile süreç ölümüne dayanıklı). Geçişler `AnimatedContent` + `AylaMotion`. Geri tuşu `BackHandler`.

## Sonuçlar
+ Sıfır bağımlılık, tamamen öngörülebilir, test edilebilir; bildirim derin bağlantıları `EXTRA_OPEN` ile basit.
− Tablet/katlanabilir liste-ayrıntı düzeni, öngörücü geri animasyonu ve ortak öğe geçişleri elle yazılmalı.
− Grafik büyüdüğünde (MVP 2 planlar, MVP 3 forum) Nav3'e geçiş maliyeti artar; bu nedenle sınır: 12 ekran.

## Alternatifler
Navigation Compose 2.x (tek backstack, sekme durumu koruması zor); Navigation 3 (ertelendi, yukarıdaki nedenle);
Voyager/Decompose (üçüncü taraf, bakım riski).

## Güncelleme (Ekim 2026) — #16: Navigation 3 yeniden değerlendirmesi

- Tarih: 2026-10-12 · Durum: Kabul edildi · Özet: yerel gezgin MVP 2 boyunca kalır; Nav3'e geçiş aşağıdaki
  eşiklerden ilki gerçekleşince, kararlı sürümle ve tek konu odaklı bir PR olarak yapılır.

### Bağlam
- **Hedef envanteri (MVP 2 sonu).** Gezgindeki hedefler (`Screen`): 5 sekme (Bugün, Takvim, İlaçlar, Planım,
  Ben) + Günlük kayıt, İlaç ekle, İlaç düzenle, Ayarlar, Yedekten geri yükle (Ayarlar'ın bir kipi) = **10**.
  Gezgin dışında kalanlar: kök durumlar Karşılama ve Uygulama kilidi (`AylaRoot` içindeki `when`; geri
  yığınına girmez), Health Connect izin gerekçesi (Health Connect izin ekranındaki gizlilik bağlantısının açtığı
  ayrı, dışa açık `Activity`), ana ekran widget'ı (başlatıcıda çizilir; uygulamayı bildirimlerle aynı
  `EXTRA_OPEN` hedefleriyle açar). Kök durumlar sayılsa bile 12 → **ölçüt 1 (12'yi aşma) gerçekleşmedi.**
- **Liste-ayrıntı.** Android 17'yi hedefleyen uygulamalar sw ≥ 600 dp'de yönlendirme/boyut kısıtlarından artık
  çıkamıyor; kabuk içeriği 720 dp'ye ortalıyor. MVP 2'de iki bölmeli düzen için issue ya da ürün kararı yok
  (`docs/arastirma/android-17-uyum.md`'deki "tablet liste-ayrıntı MVP 2" notu bir işe bağlanmadı) →
  **ölçüt 2 gerçekleşmedi.**
- **Nav3'ün durumu (12 Ekim 2026).** `navigation3-runtime`/`-ui`: 1.0.0 kararlı 19 Kasım 2025, 1.1.0 8 Nisan
  2026, **1.2.0 23 Eylül 2026** (son kararlı: derin bağlantı eşleyicileri, sonuç API'si,
  `NavigationBackHandler`); 1.1.x yama hattı 1.1.7; ön sürüm 1.3.0-alpha02 (7 Ekim).
  `lifecycle-viewmodel-navigation3` 2.11.0 kararlı (katalogdaki `lifecycle` ile aynı sürüm); M3
  `adaptive-navigation3` 1.3.0 kararlı (12 Ağustos 2026). İlk ertelemenin "API imzaları doğrulanamıyor"
  gerekçesi düştü: resmî göç kılavuzu katalog satırlarını ve sekme başına yığın kalıbını veriyor.
- **API özeti.** Geri yığını uygulamaya aittir: `SnapshotStateList` ya da `rememberNavBackStack` (süreç ölümüne
  dayanıklı; anahtarlar `NavKey` + `@Serializable`). `NavDisplay(backStack, onBack, entryDecorators,
  sceneStrategies, sceneDecoratorStrategies, sharedTransitionScope, transitionSpec, popTransitionSpec,
  predictivePopTransitionSpec, entryProvider)`; içerik `entryProvider { entry<K> { key -> … } }` DSL'iyle.
  `entryDecorators` varsayılanı yalnızca `rememberSaveableStateHolderNavEntryDecorator()`; entry başına
  ViewModel için `rememberViewModelStoreNavEntryDecorator()` elle eklenir (VM entry yığına girince oluşur,
  çıkınca temizlenir; lifecycle 2.11'den beri üst `ViewModelStoreOwner`'ın fabrikası ve `CreationExtras`'ı
  aktarılır). Çoklu yığın (sekme) yerleşik değil: kılavuz `NavigationState` + `Navigator` + sekme başına
  `NavBackStack` önerir; gösterilmeyen yığınların durumunu ve VM'lerini yaşatmak `rememberDecoratedNavEntries`
  ister. Uyarlanabilir düzen `SceneStrategy`/`Scene` ile (`ListDetailSceneStrategy`, metadata
  `listPane()`/`detailPane()`). Derin bağlantı (1.2.0+): `DeepLinkRequest(intent)` +
  `UriDeepLinkMatcher`/`StaticKeyDeepLinkMatcher`/`withBackStack`, `onCreate`/`onNewIntent` içinde eşlenir;
  manifest intent-filter'ı yine gerekir.
- **Bugünkü gezginin koddan görülen eksikleri.** (1) `AnimatedContent` bir `ViewModelStoreOwner` sağlamadığı
  için tüm `hiltViewModel()` çağrıları Activity kapsamında: `DailyLogViewModel`/`EditMedicationViewModel` farklı
  gün ve kimlikler için yeniden kullanılıyor (`load(key)` + `state.date != date` koruması bu yüzden var) ve
  Activity bitene dek temizlenmiyor; olumlu yan etkisi, sekme VM'lerinin sekme değişiminde sıcak kalması.
  (2) `SaveableStateHolder` yok: `rememberSaveable` ile tutulan ekran içi durum (ör. kaydırma konumu) itilen
  ekrandan dönünce sıfırlanıyor. (3) `BackHandler` öngörücü geri önizlemesi vermiyor; liste-ayrıntı yok.

### Seçenekler
- **A. Şimdi geçiş (MVP 2'de):** `navigation3-runtime`/`-ui` 1.2.0 + `lifecycle-viewmodel-navigation3` 2.11.0
  (+ liste-ayrıntı için `adaptive-navigation3` 1.3.0).
  - (+) Entry kapsamlı VM ve ekran durumu hazır gelir; öngörücü geri geçişleri; `Scene` ile liste-ayrıntı;
    grafik en küçükken geçiş; Google'ın yatırım yaptığı yol.
  - (−) Yeni bağımlılıklar: iki Nav3 yapıtı + geçişli `navigationevent-compose`, `savedstate-compose`;
    liste-ayrıntıda ayrıca adaptive yapıtları.
  - (−) Kararlı hatta da API hareketli: 1.1.0'da tek `SceneStrategy` (`then` zinciri) yerini
    `List<SceneStrategy>`'e bıraktı; eski aşırı yüklemeler önce deprecated, `androidx-main`'de artık `HIDDEN`
    (kaynak uyumsuz). Lifecycle 2.11'de VM dekoratörü `ViewModelStore` yerine `ViewModelStoreOwner` almaya
    başladı, `removeViewModelStoreOnPop` deprecated oldu.
  - (−) BOM: `androidx-main`'deki `navigation3-ui` derleme dosyası Compose `1.13.0-alpha03`'e sabitli; yani
    1.3.0-alpha hattı BOM 2026.09.00'ın Compose 1.12.1'ini alpha'ya çeker. 1.2.0'ın POM'u Maven erişimi
    olmadığından okunamadı.
  - (−) Davranış farkı: VM dekoratörlü tek yığında sekme değişimi sekme kök VM'lerini temizler; bugünkü
    davranışı korumak sekme başına yığın + `rememberDecoratedNavEntries` ister. Ayrıca geçiş animasyonları
    (`AylaMotion`, `LocalReducedMotion`) `transitionSpec`/`popTransitionSpec`/`predictivePopTransitionSpec`'e,
    720 dp ortalama ve alt çubuk görünürlüğü bir dekoratöre taşınır; `LocalDate` için `KSerializer` gerekir;
    `NavigatorTest` yeniden yazılır.
  - (−) Yerelde derleme yok: 2–4 CI turu ve ekran görüntüsü incelemesi, MVP 2'nin Health Connect, widget ve
    plan işleriyle aynı döneme düşer.
- **B. Yerel gezgini koru; eşikleri keskinleştir; geçişi ucuz tutan küçük hazırlıklar yap.**
  - (+) Sıfır bağımlılık; MVP 2 riskini artırmaz; Nav3 API'si bir minör daha olgunlaşır. Yeni hedefin
    maliyeti `ScreenHost`'ta tek satır; geçişin asıl maliyeti parametreli VM'ler (bugün iki tane), bu yüzden
    eşiğe kadar beklemek maliyeti anlamlı büyütmez.
  - (−) Entry kapsamlı VM, öngörücü geri önizlemesi ve liste-ayrıntı yok.
- **C. Yerel gezgin + material3-adaptive `ListDetailPaneScaffold` ya da elle iki bölme:** iki gezinme durumu
  (AylaNavigator ve bölme gezgini) çakışır, Nav3 geçişinde atılacak iş üretir; reddedildi.
- **D. Navigation Compose 2.x, Voyager/Decompose:** ilk karardaki gerekçelerle reddedildi.

### Karar
**B.** MVP 2 boyunca `ui/navigation/Navigator.kt` kalır; `androidx.navigation3` ve `androidx.navigation`
eklenmez.
- Gezgin dışı yüzeyler Nav3 geçişinde de dışarıda kalır: Karşılama/kilit kök durumdur (geri yığına girmez),
  Health Connect gerekçesi ayrı `Activity`'dir, widget `MainActivity`'yi `EXTRA_OPEN` ile açar. Bu yüzden
  widget (#14) ve Health Connect (#11) işleri bu karardan bağımsızdır.
- Hazırlık (ayrı, küçük PR'lar; bu güncelleme kod içermez):
  1. `MainShell`'de `rememberSaveableStateHolder()` + `SaveableStateProvider(screen.route)`; yığından çıkan
     rotanın durumu `removeState` ile silinir (Nav3 varsayılan dekoratörünün yerel karşılığı; eksik 2'yi giderir).
  2. Yeni `Screen` alt türleri yalnızca serileştirilebilir değerler taşır (ilkel tür, `String`, ISO metne
     çevrilen `LocalDate`); lambda ya da varlık nesnesi taşımaz; `route`/`fromRoute` simetrisi `NavigatorTest`'te
     kalır.
  3. Parametreli yeni ekranlar `load(key)` + anahtar koruması kalıbını izler (geçişte assisted injection'a
     mekanik dönüşür).
  4. `NavigatorTest`'teki davranışlar (geri sırası, `replaceTop`, aynı ekranın üst üste itilmemesi) geçiş
     PR'ının kabul testleridir.
- Geçiş tetiklendiğinde: yalnızca kararlı Nav3 (o tarihteki son 1.x) kullanılır; `lifecycle-viewmodel-navigation3`
  mevcut `lifecycle` sürüm ref'ini paylaşır; CI'da `dependencyInsight` ile `androidx.compose.runtime`'ın BOM
  sürümünde kaldığı doğrulanır; liste-ayrıntı önce özel bir `Scene` ile denenir (ADR-0006 hareket/biçim
  kuralları; Material bölme hareketleri bunlara aykırı olabilir), `adaptive-navigation3` ayrıca değerlendirilir;
  geçiş PR'ı işlev değişikliği içermez.

### Sonuçlar
+ MVP 2'de yeni gezinme bağımlılığı yok; widget ve Health Connect gezinmeden bağımsız ilerler.
+ Hazırlık 1 ile ekran içi durum kaybı giderilir; geçiş maliyeti eşikte düşük kalır.
− VM'ler Activity kapsamında kalır, `load(key)` kalıbı sürer.
− Öngörücü geri önizlemesi ve tablet iki bölmeli düzen yok; büyük ekranlarda 720 dp'ye ortalanmış tek sütun.
− `CLAUDE.md` yol haritasındaki "MVP 2: … Nav3" ile `android-17-uyum.md`'deki "tablet liste-ayrıntı MVP 2"
  bu kararla çelişir; insan onayıyla güncellenmelidir (bu güncelleme onlara dokunmaz).

### Yeniden değerlendirme ölçütleri (ilk gerçekleşen geçişi başlatır)
1. **13. hedef:** `Screen` alt türü sayısı 12'yi aşacaksa geçiş, 13. hedefi ekleyen PR'dan önce ayrı bir PR
   olarak yapılır (MVP 3'teki forum, cilt bakımı ve LH/BBT ekranlarıyla kesin gerçekleşir).
2. **Büyük ekran:** sw ≥ 600 dp iki bölmeli düzen (İlaçlar → İlaç düzenle, Takvim → Günlük kayıt) bir issue
   olarak kabul edilirse geçiş o işin ilk adımıdır; elle liste-ayrıntı yazılmaz.
3. **Gezinme gereksinimi:** sekme başına korunan yığın, ekranlar arası sonuç döndürme (en az iki yerde) ya da
   öngörücü geri önizlemesi tasarımca istenirse.
4. **Harici derin bağlantı:** URI ile açılan bir hedef gerekirse (ör. doktor raporu paylaşımı).
5. **Takvim:** en geç MVP 3 planlamasında yeniden bakılır.

Engelleyici koşul: o tarihteki kararlı Nav3 sürümü Compose'u BOM'un üstüne çekiyorsa geçiş BOM yükseltmesine
kadar bekler.

### Kaynaklar (12 Ekim 2026'da okundu)
- Navigation 3 sürüm notları: https://developer.android.com/jetpack/androidx/releases/navigation3
- Göç kılavuzu (katalog satırları, sekme başına yığın, derin bağlantı):
  https://developer.android.com/guide/navigation/navigation-3/migration-guide
- Durum kaydetme ve VM kapsamı: https://developer.android.com/guide/navigation/navigation-3/save-state
- Scene ve liste-ayrıntı: https://developer.android.com/guide/navigation/navigation-3/custom-layouts
- Derin bağlantılar: https://developer.android.com/guide/navigation/navigation-3/deep-links
- Lifecycle sürüm notları (`lifecycle-viewmodel-navigation3`):
  https://developer.android.com/jetpack/androidx/releases/lifecycle
- Material 3 Adaptive sürüm notları: https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive
- `NavDisplay` kaynağı ve `navigation3-ui` derleme dosyası (androidx-main):
  https://github.com/androidx/androidx/tree/androidx-main/navigation3/navigation3-ui
- Android 17 davranış değişiklikleri: https://developer.android.com/about/versions/17/behavior-changes-17
- Ayrıntılı sürüm tablosu ve katalog önerileri: `docs/arastirma/mvp2-teknik-notlar.md`

## Uygulama (Ekim 2026) — #22: Navigation 3'e geçildi

- Tarih: 2026-10-12 · Durum: Uygulandı · Tetik: ölçüt 1 (MVP 3'ün cilt bakımı, LH/BBT ve doktor raporu
  ekranlarıyla 12 hedef aşılacak) ve ölçüt 5 (MVP 3 planlaması). Geçiş, MVP 3 ekranlarından önce ayrı ve
  işlev değiştirmeyen bir değişiklik olarak yapıldı.

### Ne yapıldı
- `navigation3-runtime` / `navigation3-ui` **1.2.0** (kararlı, 23 Eylül 2026). `lifecycle-viewmodel-navigation3`
  katalogda hazır (`lifecycle` sürüm ref'i), henüz bağımlılık değil (aşağıya bakın).
- `Screen` sealed arayüzü `NavKey` oldu; her alt tür `@Serializable` ve yalnızca ilkel değer taşıyor
  (`LocalDate` → ISO metin serileştiricisi `data/IsoSerializers.kt`). `route`/`fromRoute` simetrisi kaldı
  (sekme kaydı ve testler için).
- Geri yığını `rememberNavBackStack(TabsRoot)`: kök giriş `TabsRoot` hiç çıkmaz, üstüne itilen `Screen`'ler
  gelir. Beş sekme kök girişin **içinde** değişir (`SekmeKoku`: `AnimatedContent` + sekme başına
  `SaveableStateProvider`); seçili sekme ayrı `rememberSaveable` ile tutulur. Böylece sekme ViewModel'leri
  sıcak kalır, sekme değişimi geri yığınına girmez ve "sekme değişimi yığını boşaltır / geri önce yığın sonra
  Bugün" davranışı birebir korunur. `AylaNavigator` bu kuralları `MutableList<NavKey>` üzerinde uygular ve
  Compose'suz test edilir (`NavigatorTest`: geçişin kabul testleri, 7 test).
- `NavDisplay(backStack, onBack, transitionSpec, popTransitionSpec, predictivePopTransitionSpec, entryProvider)`:
  itme/çıkarma animasyonları eskisiyle aynı (`AylaMotion.enter()`, kayma + solma); **öngörücü geri** artık
  NavDisplay'den gelir; "Animasyonları kaldır" açıkken geçişler anında biter. Yığın boşken Bugün dışındaki
  sekmeden geri tuşu `BackHandler` ile Bugün'e döner (NavDisplay'in işleyicisi yalnızca önceki giriş varken
  etkindir; ikisi çakışmaz). 720 dp ortalama `NavDisplay` çevresinde.
- `rememberSaveableStateHolderNavEntryDecorator` (varsayılan) ile itilen ekranların `rememberSaveable` durumu
  korunur; sekme içi durum da `SekmeKoku`'daki tutucu ile (ilk güncellemedeki hazırlık 1 böylece kapandı).
- CI (`android.yml`, "Compose BOM uyumu"): `dependencyInsight` ile `androidx.compose.runtime:runtime`,
  `ui:ui` ve `animation:animation`'ın BOM kısıtının üstüne çekilmediği doğrulanır (engelleyici koşul).
- R8: NavKey sınıf adları korunur (`-keepnames`), çünkü `NavKeySerializer` `Class.forName` + `serializer()`
  yansımasıyla yazar/okur; `serializer()` üyeleri zaten korunuyordu.

### Bilinçli olarak yapılmayan
- **Entry kapsamlı ViewModel yok** (`rememberViewModelStoreNavEntryDecorator` eklenmedi, #27). Entry kapsamı,
  ekran yığından çıkar çıkmaz `viewModelScope`'u iptal eder; bugün birkaç yazma "ateşle ve unut" biçiminde
  (`AddMedicationScreen` → `addTemplate()` + hemen `onDone()`, `SettingsViewModel.update/exportTo/…`). Önce
  bu yazmalar uygulama kapsamına taşınır, sonra dekoratör eklenir. `DailyLogViewModel`/`EditMedicationViewModel`
  `load(key)` kalıbı o zamana dek kalır.
- Sekme başına korunan yığın yok (ürün davranışı "sekme değişimi yığını boşaltır").
- Liste-ayrıntı (`SceneStrategy`) yok; ölçüt 2 gerçekleşince özel `Scene` ile denenir.
- Derin bağlantı eşleyicileri kullanılmadı; `EXTRA_OPEN` hedefleri (bildirim, widget) `selectTab` ile sürüyor.

### Sonuçlar
+ MVP 3 ekranları `entryProvider`'a birer `entry<K>` satırı olarak eklenir; 12 sınırı kalktı.
+ Öngörücü geri önizlemesi itilen ekranlarda hazır; süreç ölümünde yığın serileştirilerek döner.
− Yeni geçişli bağımlılıklar: `navigationevent-compose`, `savedstate-compose` (ikisi de AndroidX; izin yok).
− Ekran görüntüsü testleri kabuğu (`MainShell`) çizmez; gezinme davranışı yalnızca birim testleriyle ve
  cihazda doğrulanır (`docs/ilk-cihaz-testi.md`).
