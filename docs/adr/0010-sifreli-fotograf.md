# ADR-0010: Cilt ve saç ilerleme fotoğrafları: şifreli cihaz içi depo, Keystore sarmalı dosya anahtarı, Room dışı dizin

- Tarih: 2026-10-12 · Durum: Kabul edildi · İş: #25 (US-093)
- İlgili: ADR-0002 (veri katmanı, anahtar kaybı politikası), ADR-0004 (WorkManager kullanılmaz),
  `docs/gizlilik-politikasi.md` §3.7, §4, §5 (bu ADR ile güncellenmesi gereken bölümler)

## Bağlam
Cilt ve saç bakımı (MVP 3) kullanıcının akne, leke/koyulaşma, tüylenme ve saç dökülmesindeki değişimi zaman içinde
görmesini ister; bunun en dürüst aracı fotoğraftır. Fotoğraf, PCOS bağlamında sağlık verisidir (KVKK özel nitelikli)
ve çoğu zaman yüzü içerir: galeriye düşerse bulut yedeğine, "Anılar" bildirimlerine ve başka uygulamalara sızar.
"Cihazda kalır, şifreli kalır, kullanıcı siler" ilkesi burada da geçerlidir. Teknik kısıtlar: Room şeması bu işte
değişmez (migration yok), `androidx.security:security-crypto` eklenmez (eskimiştir), Robolectric'te
Android Keystore yoktur, Android 17 Keystore anahtar sayısını sınırlar.

## Karar
1. **Depo:** `data/photos/PhotoStore` (Hilt tekil). Her fotoğraf `filesDir/photos/<uuid>.enc`, meta veriler
   `filesDir/photos/index.json` (o da şifreli). Galeriye, MediaStore'a ya da dış depolamaya hiçbir şey yazılmaz.
2. **Anahtar hiyerarşisi** (`PhotoKeyProvider` arayüzü, üretimde `KeystorePhotoKeyProvider`):
   - 32 baytlık rastgele dosya anahtarı (DEK), Android Keystore'daki **amaç başına tek** AES-GCM anahtarıyla
     (`ayla.photo.wrap.v1`) sarılıp ayrı tercih dosyasında (`ayla_photo_keys`) saklanır. Uygulamanın toplam Keystore
     anahtarı ikidir (veritabanı + fotoğraf); fotoğraf başına anahtar üretilmez.
   - DEK düz metin olarak diske yazılmaz, günlüğe düşmez, bellekte alan olarak tutulmaz: her işlemde sarmaldan
     açılır, işlem bitince düşer. Veritabanı anahtarına (`DatabaseKeyProvider`, `ayla_keys`) dokunulmaz.
   - DEK'in Keystore dışı (yazılımsal) olmasının nedeni akış şifrelemedir: dosya başına açık rastgele IV ve AAD
     gerekir; Keystore anahtarları açık IV'ye izin vermez. Keystore yalnızca sarmaldır.
   - Veritabanı parolasının aksine, sarmal `commit` ile eşzamanlı yazılır: şifreli dosya diske inmeden sarmal
     diskte olur (süreç ölürse dosyalar anahtarsız kalmasın).
3. **Dosya biçimi** (`PhotoCipher`, dizin ve fotoğraflar için aynı): `[12 bayt rastgele IV][AES/GCM/NoPadding
   içerik][128 bit etiket]`; dosyanın mantıksal adı (`<id>.enc` ya da `index.json`) AAD olarak bağlıdır, böylece bir
   dosyanın içeriği başka bir adın altına taşınamaz. Yazma `CipherOutputStream` ile akış hâlindedir
   (`Bitmap.compress` doğrudan şifreli akışa yazar); okuma `CipherInputStream` ile. GCM çözmede sağlayıcı etiketi
   doğrulamak için şifreli içeriği tamponlar; dosyalar 1600 px JPEG ile sınırlı olduğundan (~200–500 KB) bu kabul
   edilir. Dosyalar önce `.tmp` adıyla yazılıp yeniden adlandırılır; dizin ek olarak `fsync` edilir.
4. **Dizin, Room'da değil.** `index.json` şeması sürümlüdür (`schemaVersion: 1`; `id, kind, area, date, createdAt,
   byteSize, width, height`), kotlinx.serialization ile yazılır, `ignoreUnknownKeys` ile okunur; alan eklenirken
   varsayılan verilir. Tüm değişiklikler tek `Mutex` altındadır. Yazma sırası "önce dosya, sonra dizin": yarım kalan
   kayıt dizine girmez; dizinde olmayan `.enc`/`.tmp` dosyaları bir sonraki açılışta silinir (erişilemezler),
   dosyası kaybolmuş kayıt gösterilmez. Boş depo ilk açılışta boş bir dizin yazar; böylece "dizin yok ama şifreli
   dosya var" durumu yalnızca kayıp/müdahale anlamına gelir.
5. **Küçültme ve meta veri temizliği.** Kaynak `BitmapFactory` sınır okuması + `inSampleSize` ile bellek sınırlı
   çözülür, en uzun kenar 1600 px'e ölçeklenir ve JPEG kalite 85 ile **yeniden kodlanır**. Yeniden kodlama kaynaktaki
   EXIF'i (GPS, cihaz, zaman, küçük resim) tamamen atar; yalnızca yön etiketi okunup piksellere uygulanır
   (`androidx.exifinterface` 1.4.2, tek yeni bağımlılık; platform `android.media.ExifInterface` lint tarafından önerilmez ve Robolectric'te güvenilir değil). Kaynak dosya hiçbir zaman olduğu gibi saklanmaz.
6. **Alma yolları ve izinler.** Kamera: `ACTION_IMAGE_CAPTURE` + `EXTRA_OUTPUT` olarak uygulamanın kendi
   `FileProvider`'ından (`<applicationId>.photos`, `exported=false`, `grantUriPermissions=true`, yalnızca
   `cacheDir/camera/`) alınan geçici adres; sistem yazma iznini niyetle birlikte verir. Çekim bitince geçici düz dosya
   **her durumda** silinir (`saveCapture`/`discardCapture`); açılışta bir saatten eski artıklar temizlenir
   (`cleanupTemp`; yeni dosyalar korunur çünkü süreç kamera öndeyken ölmüş ve çekim henüz alınmamış olabilir).
   Galeri: sistem fotoğraf seçici (`PickVisualMedia`), tek fotoğraf. **`CAMERA`, `READ_MEDIA_IMAGES` ya da başka bir
   izin istenmez**; manifest izin listesi değişmez. Kaynak kopyası da `cacheDir/camera/` altında geçicidir.
7. **Yedek dışı.** `allowBackup=false` ve `data_extraction_rules.xml` `file` alanını (dolayısıyla `filesDir/photos`)
   hem bulut yedeğinden hem cihaz aktarımından dışlar; `cacheDir` zaten hiç yedeklenmez. Doğrulandı, değişiklik
   gerekmedi. Fotoğraflar JSON yedeğe (`DataExporter`, `schemaVersion` 2) **girmez**; yedekten geri yükleme
   fotoğrafları kapsamaz (MVP sonrası, ayrı karar).
8. **Anahtar kaybı politikası** (ADR-0002 ile aynı ilke): sarmal açılamıyorsa ya da şifreli dosya varken sarmal/dizin
   yoksa **sessizce yeni anahtar üretilmez**; işlemler `PhotoStoreUnavailableException` fırlatır,
   `PhotoStore.availability()` bunu `Unavailable(cause)` olarak döndürür. Karar kullanıcının: "Fotoğrafların
   açılamıyor" diyaloğu → `deleteAll()` ile temiz başlangıç (dosyalar, dizin, geçiciler ve anahtar silinir, sarmal
   sıfırlanır). Şifreli dosya yokken açılamayan sarmal/dizin kayıp sayılmaz, temiz dizinle devam edilir.
9. **Silme ve dışa aktarma (KVKK hakları).** Tek fotoğraf `delete(id)` (önce dizinden, sonra diskten). Tam silme
   `deleteAll()`: `DataExporter.wipeAll` ile birlikte çağrılır, hiçbir iz kalmaz. Dışa aktarma kullanıcının seçtiği
   SAF adresine **düz JPEG** yazar: `exportTo(uri, id)` (`CreateDocument("image/jpeg")`) ve `exportAllTo(treeUri)`
   (`OpenDocumentTree` + `DocumentsContract.createDocument`, DocumentFile kütüphanesi yok). Dosya adı
   `ayla-cilt-2026-10-12-1a2b3c4d.jpg`; dışa aktarılan dosya şifresizdir, politika bunu söyler.
10. **Arayüz kuralları (bu ADR'nin UI işi, #25).** Görüntüleyici ve karşılaştırma ekranı `FLAG_SECURE` ayarına uyar
    (`MainActivity` pencere bayrağını tüm ekranlar için yönetir; görüntüleyici ayrıca kapatmaz). Bildirimlerde, ana
    ekran widget'ında, son uygulamalar önizlemesinde ve doktor notu metninde fotoğraf **yoktur**. Önizlemeler
    `thumbnail(id, maxPx)` ile dizindeki boyuttan örneklenir; tam çözme yalnızca görüntüleyicide. Kamera akışında
    `PendingCapture.id` `SavedStateHandle`'da tutulur ve dönüşte `capture(id)` ile yeniden kurulur. Fotoğraf
    özelliği ayrı rıza olmadan açılmaz (madde 11).
11. **KVKK: ayrı, geri alınabilir rıza.** Fotoğraflar mevcut "sağlık verisi" rızasının metninde sayılmıyor ve
    nitelik olarak farklı (görsel kimlik, yüz). Açık rıza "belirli bir konuya ilişkin" olmalı; bu yüzden ilk fotoğraf
    çekiminde/seçiminde **ayrı** onay istenir (`UserPreferences.photoConsentAt: Instant?`, varsayılan null) ve
    Ben > Ayarlar > İzinlerin'den geri alınabilir; geri alma = `deleteAll()` + rıza sıfırlama. Yüz tanıma, analiz ya da
    otomatik değerlendirme yapılmadığı için biyometrik veri işlenmez; fotoğraflar bağlam gereği sağlık verisi olarak
    işlenir. Google Play Veri güvenliği formuna "Fotoğraflar: cihazda işlenir, paylaşılmaz, şifrelenir, kullanıcı
    silebilir" satırı eklenir. Politikaya eklenecek taslak aşağıdadır. **Güncelleme (12 Ekim 2026, uyum
    incelemesi):** taslak, politika sürümü 1.1'e işlendi (§3.10 fotoğraflar, §3.11 düzeltilen "fotoğraf toplanmaz"
    cümlesi, §4 ayrı onay, §5, §7 JPEG dışa aktarma, §9 izin notu; `res/raw` kopyası eş). Play Veri güvenliği satırı
    `docs/play/health-connect-beyani.md` §4'te. `UserPreferences.photoConsentAt` tercihlerde tanımlı (varsayılan
    null); onay diyaloğu, İzinlerin'den geri alma (= `deleteAll()` + alanın sıfırlanması) UI işiyle (#25) gelir;
    bunlar olmadan fotoğraf özelliği açılmaz.
12. **Test.** `PhotoKeyProvider` arayüzü sayesinde Robolectric testleri yazılımsal sabit anahtar kullanır
    (`veri/FotografTesti`, `@GraphicsMode(NATIVE)` ile gerçek JPEG kodlama). Keystore sarmalı ancak cihazda
    doğrulanır (`docs/ilk-cihaz-testi.md`'ye madde).

### Gizlilik politikasına eklenecek paragraf taslağı
**3.8 Cilt ve saç fotoğrafları (isteğe bağlı)**
- **Ne:** cildinin ya da saçının ilerlemesini görmek için çektiğin ya da galerinden seçtiğin fotoğraflar; her fotoğraf
  için tür (cilt ya da saç), isteğe bağlı bölge ve tarih.
- **Neden:** akne, leke ya da koyulaşma, tüylenme ve saç dökülmesindeki değişimi zaman içinde yan yana görebilmen ve
  istersen doktoruna gösterebilmen. Ayla fotoğraflarını incelemez, yüz tanıma yapmaz, onlardan tanı ya da puan
  çıkarmaz.
- **Nerede:** yalnızca bu cihazda, Ayla'nın kendi alanında, her biri ayrı ayrı şifrelenmiş dosyalar olarak.
  Fotoğraflar galerine ya da başka uygulamaların görebileceği bir yere yazılmaz; kameradan gelen geçici kopya şifreli
  kayıttan hemen sonra silinir. Fotoğrafın içindeki konum, cihaz ve çekim zamanı bilgileri (EXIF) saklanmaz; görüntü
  en uzun kenarı 1600 piksel olacak biçimde küçültülür. Şifreleme anahtarı Android Keystore ile korunur; fotoğraflar
  sistem yedeğine ve cihaz aktarımına girmez, dışa aktardığın JSON dosyasında da yer almaz.
- **Nasıl silinir:** fotoğrafı açıp Sil ile tek tek; hepsi için Ben > Ayarlar > Verilerin > Tüm verilerimi sil ile ya
  da fotoğraf onayını İzinlerin bölümünden geri alarak. Fotoğraflarını düz JPEG olarak seçtiğin bir yere dışa
  aktarabilirsin; dışa aktarılan dosyalar şifrelenmez, onları güvendiğin bir yerde sakla.

Ek düzeltmeler: §3.7'deki "fotoğraflarını, kamerayı ... toplamaz" cümlesi "Fotoğraflarına yalnızca sen bir cilt ya da
saç fotoğrafı çekmek ya da seçmek istediğinde, yalnızca o fotoğraf için erişir; kamera ya da galeri izni istemez"
biçiminde güncellenir. §4'teki sağlık verisi listesine "cilt ve saç fotoğrafları" eklenir ve "Fotoğraf özelliğini ilk
kez kullandığında bu fotoğraflar için ayrı bir onay istenir; onayı geri aldığında fotoğrafların silinir" cümlesi
girer. §5'e "Fotoğraflar: her dosya 256 bit AES-GCM ile ayrı ayrı şifrelenir; anahtar veritabanı anahtarı gibi
Android Keystore ile sarılır" maddesi, §9'a "Fotoğraf çekme ve seçme için yeni bir Android izni istenmez; kamera ve
fotoğraf seçici sistem ekranlarıyla açılır" cümlesi eklenir.

## Sonuçlar
+ Fotoğraf cihazdan çıkmaz, galeriye düşmez, yedeğe girmez; çalınan dosya anahtarsız okunamaz; EXIF konumu hiç
  saklanmaz. Manifest izin listesi ve Room şeması değişmedi; yeni bağımlılık yok.
+ Tüm depo davranışı Robolectric'te testli; UI yalnızca `availability()`, `records`, `thumbnail`, `load` ve kamera
  yardımcılarını bağlar.
− Keystore anahtarı kaybolursa fotoğraflar geri gelmez (veritabanıyla aynı sınır); politika ara ara JPEG dışa
  aktarmayı önerir. JSON yedek fotoğrafları içermediği için cihaz değişiminde fotoğraflar elle taşınır.
− Kaynak kopyası ve kamera çıktısı şifreli kayda kadar `cacheDir/camera/` altında düzdür; süreç kayıt ortasında
  ölürse artık en fazla bir saat durur (açılış temizliği). Uygulama alanı cihaz şifrelemesiyle korunur; kabul edildi.
− Dizin tek dosyadır ve her değişiklikte yeniden yazılır; binlerce fotoğrafta onlarca KB — yeterli. Depo boyutu
  sınırlanmadı; ayarlarda "n fotoğraf, x MB" göstermek UI işi (`records` toplamından).
− GCM çözme dosyayı belleğe alır; 1600 px sınırı bunu ~1 MB ile tutar. Daha büyük kenar istenirse biçim (parçalı
  GCM) yeniden ele alınır.

## Alternatifler
- **`androidx.security:security-crypto` `EncryptedFile`:** eskimiş (deprecated), Tink'i ve kendi Keystore anahtarını
  getirir, akış ve AAD denetimi yok. Reddedildi.
- **Room/SQLCipher'da BLOB:** şema değişikliği + migration gerektirir, WAL ve bellek maliyeti büyük, tek tek dışa
  aktarma zorlaşır. Reddedildi; dizin bile Room dışında tutuldu ki bu iş şema değiştirmesin.
- **MediaStore / uygulamaya özel dış depolama:** galeride ve diğer uygulamalarda görünür, bulut yedeklerine düşer.
  Reddedildi.
- **Veritabanı parolasıyla şifreleme (tek DEK):** anahtar sınırı açısından cazip ama veritabanı anahtarının bellek
  ömrü SQLCipher'a bağlıdır ve iki depo birbirine kilitlenir (biri açılamazsa diğeri de). Amaç başına ayrı sarmal
  seçildi.
- **Keystore anahtarıyla doğrudan dosya şifreleme:** IV'yi Keystore seçer, AAD ve akış şifreleme kısıtlı, her dosya
  için Keystore çağrısı yavaş; fotoğraf başına anahtar ise Android 17 sınırına takılır. Reddedildi.
- **Fotoğrafı olduğu gibi (EXIF ile) saklamak:** kolay ama konum/cihaz izi taşır; yeniden kodlama seçildi.
