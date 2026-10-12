# Play Console: Health Connect beyanı ve Veri güvenliği yanıtları

- Tarih: 12 Ekim 2026 · İlgili iş: #11 (Health Connect'ten uyku içe aktarma)
- Kapsam: "Health apps" beyan formu (Health Connect veri türleri dahil), Veri güvenliği (Data safety) formu,
  gizlilik politikası alanı, mağaza açıklaması denetimi.
- Hukuki görüş değildir; uygulanabilir kontrol listesidir. Politika metinleri her gönderimden önce yeniden
  okunmalıdır (aşağıdaki kaynaklar 12 Ekim 2026'da kontrol edildi).

## 0. Karar özeti

| Konu | Öneri |
|---|---|
| Health Connect izni | Yalnızca `android.permission.health.READ_SLEEP` (okuma). Yazma, geçmiş (30 günden eski) ve arka plan okuma izni yok. |
| Onaylı kullanım durumu | "Fitness, wellness and coaching" altında uyku düzenini izleme ("analyzing sleep health and patterns"). |
| Health apps formu, ekran 1 | Period tracking, Sleep management, Medication management, Diseases and conditions management (son ikisi aşağıda gerekçeli). Medical device apps **seçilmez**. |
| Health apps formu, ekran 2 | Yalnızca Sleep management > Sleep, okuma; gerekçe metni §3.2'de (İngilizce, yapıştırmaya hazır). |
| Veri güvenliği formu | "Kullanıcı verisi topluyor veya paylaşıyor mu?" → **Hayır**. Mağazada "veri toplanmıyor / paylaşılmıyor" görünür. |
| Gizlilik politikası | `docs/gizlilik-politikasi.md`; herkese açık HTML sayfa olarak [POLİTİKA URL'Sİ]'nde, aynı metin uygulama içinde ve Health Connect gerekçe etkinliğinde. |
| Gönderimden önce düzeltilecekler | §2'deki engelleyici maddeler (rıza anahtarları, karşılamada açık rıza ve politika bağlantısı, gerekçe etkinliği). |

## 1. Doğrulanan politika şartları

Health Connect izinleri (Play "Permissions and APIs that Access Sensitive Information" > Health Connect by
Android Permissions; "Android Health Permissions: Guidance and FAQs"):

- **Onaylı kullanım durumu:** erişim yalnızca onaylı kullanım durumlarında verilir: fitness, wellness ve koçluk;
  ödüller; kurumsal wellness; tıbbi bakım; insan katılımlı araştırma; sağlıkla bütünleşik oyunlar. Wellness
  kapsamında "analyzing sleep health and patterns" açıkça sayılır. Yalnızca çocuklara yönelik uygulamalar,
  yaşamsal kritik sistemler ve arayüzsüz (headless) uygulamalar kapsam dışıdır.
- **En az kapsam:** "You must only request access to the permissions that are necessary to implementing your
  product's features or services." Her veri türü, kullanıcıya görünen bir özellikle gerekçelendirilir;
  "uygulama işlevi için gerekli" gibi genel gerekçeler eksik sayılır.
- **Sınırlı kullanım:** veriler yalnızca kullanıcıya görünen özelliği sağlamak ya da iyileştirmek için kullanılır;
  reklam platformlarına ve veri simsarlarına aktarma ya da satış, reklam gösterimi, kredi değerlendirmesi
  yasaktır; insan erişimi ancak açık rıza, güvenlik ya da yasal zorunlulukla mümkündür.
- **Şeffaflık:** kapsamlı gizlilik politikası; erişen uygulamanın kimliğinin doğru gösterilmesi; veri türleri,
  kullanım ve paylaşımın açıklanması; kullanıcının verisini uygulamada nasıl yöneteceğini ve sileceğini anlatan
  yardım metni; verinin güvenli işlenmesi.
- **Yüksek hassasiyetli türler:** üreme sağlığı (ör. `READ_MENSTRUAL_CYCLE_PHASE`), semptomlar, klinik
  ölçümler ek incelemeye tabidir. Ayla bunları istemez (MVP 3 için bkz. §8).

Beyan süreci (Health Connect "Publish" ve "Declare access" rehberleri):

- Üç adım: politikaları gözden geçir, Veri güvenliği formunu doldur, App content sayfasında **Health apps**
  formunu doldur. Ekran 1: sağlık özellikleri; ekran 2: her Health Connect veri türü için kullanım açıklaması.
- Gizlilik politikası Play mağaza sayfasında yayımlanır ve "Health Connect'teki gizlilik politikası bağlantısına
  tıklandığında gösterilenle aynı politika" olmalıdır.
- Erişim, sürümden bağımsız olarak paket adı için izin listesine alınır. Veri türü eklenir ya da çıkarılırsa
  form yeniden doldurulur (mevcut türler dahil edilir, artık kullanılmayanlar çıkarılır).
- Herkese açık yayımlanmış ama veri türü erişimi istenmemiş bir uygulamada kullanıcılar "Health Connect'e
  erişilemiyor" iletişim kutusu görür.

Sağlık uygulamaları ve kullanıcı verisi (Health Content and Services; User Data):

- **Herkes** Health apps beyanını doldurur (sağlık özelliği olmayanlar da).
- Gizlilik politikası Play Console'daki alana ve uygulama içine konur; etkin, herkese açık, coğrafi kısıtsız bir
  URL'de olur (PDF olmaz), düzenlenebilir olmaz, açıkça "gizlilik politikası" olarak adlandırılır, Play
  sayfasındaki geliştirici adını ya da uygulama adını içerir, iletişim noktası ve saklama/silme politikasını verir.
- Tıbbi cihaz olmayan sağlık uygulamaları açıklamada "not a medical device and does not diagnose, treat, cure,
  or prevent any medical condition" anlamında açık bir uyarı taşır ve kullanıcıya sağlık profesyoneline
  danışmasını hatırlatır.
- Health Connect ile erişilen veri "kişisel ve hassas kullanıcı verisi"dir (User Data politikasının tamamı
  geçerli).

## 2. Gönderim öncesi kontrol listesi

Engelleyici (beyandan önce kapanmalı):

- [ ] **Rıza anahtarları:** Ayarlar ve karşılamadaki "Anonim kullanım istatistikleri" ve "Araştırmaya katkı"
      anahtarları kaldırılır. Uygulamada analitik ya da araştırma işlemesi yok; bu anahtarlar Veri güvenliği
      formundaki "veri toplanmıyor" yanıtıyla ve politikayla çelişir, araştırma ise Health Connect'te etik kurul
      onayı ve ayrı onam gerektirir. "Kişiselleştirme" kalacaksa gerçek bir işleve bağlanır ve **varsayılan
      kapalı** olur (`UserPreferences.consentPersonalization` bugün `true`).
- [ ] **Karşılamada açık rıza:** sağlık verilerinin cihazda işlenmesi için ayrı, önceden işaretlenmemiş bir onay
      ve politika bağlantısı (metinler: `docs/arastirma/health-connect-teknik.md` > Arayüz metinleri).
- [ ] **Uygulama içi politika:** Ayarlar'da "Gizlilik ve KVKK aydınlatma metni" satırı; Health Connect gerekçe
      etkinliği (Android 13 ve altı intent-filter, Android 14 ve üstü activity-alias) aynı metni tam olarak gösterir.
- [ ] **Yer tutucular dolduruldu:** [GELİŞTİRİCİ ADI] (Play'deki geliştirici adıyla aynı), [POSTA ADRESİ],
      [İLETİŞİM E-POSTASI], [POLİTİKA URL'Sİ].
- [ ] **Politika yayında:** [POLİTİKA URL'Sİ] herkese açık HTML sayfa; giriş gerektirmez, PDF değil,
      yalnızca sahibince düzenlenebilir.
- [ ] **Manifest:** sağlık izinlerinden yalnızca `READ_SLEEP`; birleştirilmiş manifestte `INTERNET` yok
      (`apkanalyzer manifest permissions` ile doğrula).
- [ ] **Politika ile uygulama aynı şeyi söylüyor:** Ayarlar'daki Health Connect bölümü ("bağlantıyı kes",
      Health Connect ayarlarına bağlantı) ve kaynak uygulama adının gösterimi uygulandı; uygulanmadıysa
      politikanın 6. bölümündeki ilgili ibareler çıkarıldı.

Gönderimle birlikte:

- [ ] Health apps formu (§3), Veri güvenliği formu (§4), gizlilik politikası URL'si.
- [ ] Mağaza açıklamasında tıbbi cihaz uyarısı (§6).
- [ ] Hedef kitle 18+ (karar sahibinde; politika 12. bölüm buna göre yazıldı).
- [ ] Reklam beyanı: "Uygulamamda reklam yok".
- [ ] `READ_SLEEP` içeren sürüm, beyan onaylanmadan herkese açık yayına çıkmaz (kullanıcılar "erişilemiyor"
      iletişim kutusu görür); önce kapalı test kanalında doğrula.

## 3. Health apps beyan formu: önerilen yanıtlar

Form, App content sayfasındadır (Policy > App content > Health apps). Play Console Türkçe arayüzünde adlar
çevrilmiş olabilir; aşağıda İngilizce adlar kullanıldı.

### 3.1 Ekran 1: sağlık özellikleri

Health apps beyanı uygulamanın **tüm** sağlık özelliklerini yansıtır; Health Connect gerekçesi ekran 2'de
yalnızca istenen veri türleri için yazılır.

| Özellik | Öneri | Gerekçe |
|---|---|---|
| Period tracking | Seç | Adet kaydı, aralıklı döngü tahmini, 60/90. gün uyarıları. |
| Sleep management | Seç | Elle uyku kaydı ve Health Connect'ten uyku oturumu içe aktarma. |
| Medication and treatment management (rehberde "Medication and pain management") | Seç | İlaç ve takviye hatırlatmaları, doğum kontrol hapı paket takibi, doz uyumu, stok uyarısı. Doz hesabı ya da ilaç önerisi yok. |
| Diseases and conditions management | Seç (karar sahibinde) | Uygulama PCOS ile yaşayanlara yönelik öz-takip aracı olarak konumlanıyor; seçmemek beyanı eksik gösterebilir. Tanı ya da tedavi işlevi yok. |
| Medical device apps | **Seçme** | TİTCK / MDCG 2019-11 wellness sınırı: tanı, tedavi önerisi, doz hesabı, etkileşim uyarısı yok. |
| Nutrition and weight management | Seçme | MVP 1'de beslenme yok; kilo/bel varsayılan gizli, hedef ve kalori yok. MVP 2 beslenme planıyla yeniden değerlendir. |
| Reproductive and sexual health | Seçme | Doğurganlık tedavisi ya da cinsel sağlık işlevi yok; "Hamile kalmayı planlıyorum" yalnızca öncelik etiketi. LH/BBT (MVP 3) ile yeniden değerlendir. |
| Stress management, relaxation, mental acuity; Mental and behavioral health | Seçme | Akşam rutini yalnızca hatırlatma; ruh hâli yalnızca günlük kayıt alanı. |
| Clinical decision support, research ve diğerleri | Seçme | Kapsam dışı. |

### 3.2 Ekran 2: Health Connect veri türleri

Yalnızca **Sleep management > Sleep (read)** seçilir ve gerekçelendirilir. Başka veri türü seçilmez.

Yapıştırılacak gerekçe (İngilizce, uzun sürüm):

```text
Ayla is a self-tracking app for people living with PCOS (cycle tracking, medication and supplement
reminders, daily log, water and sleep). It requests read-only access to Sleep (READ_SLEEP) and no other
Health Connect data type.

Where: Plan tab > "Water & sleep" > Sleep card > "Import from Health Connect" (Turkish UI: Planım >
Su ve uyku > "Health Connect'ten al").

Why: so users do not have to type their bedtime and wake-up time by hand. Imported nights appear next to
manually entered nights in the sleep card (last night and the last 7 nights with durations).

How: data is read only while the app is in the foreground and only when the user taps the button. We read
sleep sessions from the last 30 days and store only each session's start time, end time, Health Connect
record ID (to avoid duplicates) and the source app name (to show where the data came from). Sleep stages,
titles and notes are not stored.

Storage and sharing: data stays on the device in an encrypted SQLCipher database whose key is protected by
Android Keystore. The app has no INTERNET permission, no account, no server and no analytics or ads SDKs.
Health Connect data is never transferred, sold or used for advertising, profiling, credit or insurance
decisions, and no human can access it. Users can delete imported nights in the app and revoke access in
Health Connect settings or in Ayla's settings. Ayla does not write to Health Connect and does not request
background or historical (older than 30 days) read access.
```

Ayla ön plana her gelişte de eşitleme yapacaksa (bkz. `docs/arastirma/mvp2-teknik-notlar.md` §3), "How:"
paragrafının ilk cümlesi şöyle değişir:

```text
How: data is read only while the app is in the foreground: when the user taps the button and, after the
user has connected Health Connect, when the user opens the app (to add new nights).
```

Kısa sürüm (alan karakter sınırı gösterirse):

```text
Read-only Sleep access. In Plan > Water & sleep, the user taps "Import from Health Connect" to fill the
sleep log (last night and last 7 nights) without typing times. Read only in the foreground on user action;
only start/end time, record ID and source app name are stored, on-device, in an encrypted database. No
internet permission, no server, no SDKs; data is never shared, sold or used for ads. No write, background
or history access.
```

Türkçe karşılığı (kayıt için): Ayla yalnızca uyku oturumlarını okur. Kullanıcı Planım > Su ve uyku'da
"Health Connect'ten al"a dokunduğunda, yatış/kalkış saatini elle girmeden son 30 günün uyku oturumları içe
aktarılır; dün gece ve son 7 gece uyku kartında gösterilir. Okuma yalnızca ön planda ve kullanıcı isteğiyle
yapılır; yalnızca başlangıç/bitiş zamanı, kayıt kimliği ve kaynak uygulama adı cihazda şifreli veritabanında
saklanır. İnternet izni, sunucu, SDK yok; veri paylaşılmaz, satılmaz, reklamda kullanılmaz. Yazma, arka plan
ve geçmiş veri izni istenmez.

### 3.3 Bilerek istenmeyen izinler

| İzin | Neden istenmiyor |
|---|---|
| `WRITE_SLEEP` | Ayla Health Connect'e veri yazmaz; elle girilen uyku yalnızca Ayla'da kalır. |
| `READ_HEALTH_DATA_HISTORY` | Uyku kartı son 7 geceyi gösterir; varsayılan 30 günlük pencere yeterli. Her ek izin ayrı gerekçe ister. |
| `READ_HEALTH_DATA_IN_BACKGROUND` | Okuma yalnızca kullanıcı dokunduğunda, ön planda; Ayla WorkManager kullanmaz. |
| Adet, ovülasyon testi, bazal ısı, semptom vb. | MVP 1–2 kapsamı dışında; üreme sağlığı türleri yüksek hassasiyetli incelemeye tabi. |

## 4. Veri güvenliği (Data safety) formu: önerilen yanıtlar

| Soru (Play yardım sayfasındaki ad) | Yanıt | Gerekçe |
|---|---|---|
| Does your app collect or share any of the required user data types? | **No** | "Collect" = verinin uygulamadan cihaz dışına iletilmesi. "User data accessed by your app that is only processed locally on the user's device and not sent off device does not need to be disclosed." Ayla'da internet izni, sunucu ve SDK yok; Health Connect verisi de cihazda kalır. |
| Kullanıcının başlattığı dışa aktarma ve doktor notu paylaşımı | Beyan gerekmez | "Sharing" istisnası: "Transferring user data to a third party based on a specific user-initiated action, where the user reasonably expects the data to be shared". Dosyanın yerini ya da alıcıyı kullanıcı seçer; Ayla veriyi kendisi iletmez. |
| Encrypted in transit / deletion request soruları | Gösterilmez | Yardım sayfasına göre bu sorular yalnızca "Yes" dalında sorulur. Konsol yine de silme sorusunu gösterirse: kullanıcı verisini uygulama içinden silebilir (Tüm verilerimi sil; adet ve uyku kayıtları tek tek de). |
| Hesap oluşturma (gösterilirse) | Uygulama hesap oluşturmaya izin vermiyor | Hesap yok; hesap silme URL'si gerekmez. |
| Bağımsız güvenlik incelemesi (isteğe bağlı rozet) | Boş bırak | Böyle bir doğrulama yapılmadı. |
| Privacy policy | [POLİTİKA URL'Sİ] | Veri toplamayan uygulamalar için de zorunlu ("Even developers with apps that do not collect any user data must complete this form and provide a link to their privacy policy"). |

Bu yanıtı **geçersiz kılan** değişiklikler (olursa form ve politika birlikte güncellenir): `INTERNET` izni
getiren herhangi bir bağımlılık; çökme raporu, analitik, uzaktan yapılandırma ya da reklam SDK'sı; bulut
yedekleme/eşitleme; sunucuya bağlanan forum (MVP 3); uygulama içi satın alma kayıtlarının sunucuda tutulması.

## 5. Diğer App content öğeleri (kısa)

- **Privacy policy:** [POLİTİKA URL'Sİ].
- **Ads:** No.
- **App access:** tüm işlevler kısıtlamasız (hesap yok). Health Connect özelliğinin yeri gerekçe metninde yazılı.
- **Target audience:** 18 ve üzeri (karar sahibinde).
- **Health apps:** §3. **Data safety:** §4.

## 6. Mağaza metni denetimi

Uzun açıklamanın ilk paragrafına konacak uyarı:

```text
Ayla tıbbi bir cihaz değildir; herhangi bir hastalığı teşhis etmez, tedavi etmez, iyileştirmez veya önlemez.
Tıbbi tavsiye, tanı ya da tedavi için doktoruna veya bir sağlık profesyoneline danış.
```

Kısa açıklama önerisi (73/80 karakter): `PCOS (PMOS) için döngü, ilaç, su ve uyku takibi; verin cihazında şifreli.`

Health Connect cümlesi önerisi: "İsteğe bağlı: Health Connect'ten uyku oturumlarını içe aktar (yalnızca okuma;
veriler cihazında kalır)."

| Kullanılabilir | Kullanılmaz (TİTCK / MDCG 2019-11 sınırı ve Play sağlık politikası) |
|---|---|
| takip et, kaydet, hatırlat, not tut, doktorunla paylaş | teşhis, tedavi, iyileştirir, düzeltir, dengeler (ör. "hormonlarını dengele") |
| aralıklı tahmin ("tıbbi karar aracı değildir" ile) | doğurganlık / gebelik önleme amaçlı tahmin, "güvenli günler" |
| "kanıt düzeyi: deneysel" (inositol) | doz önerisi, ilaç etkileşimi uyarısı, "ilacını değiştir" |
| "verin cihazında şifreli kalır" | "%100 güvenli", "asla ele geçirilemez" gibi mutlak güvenlik iddiaları |
| ritim, alışkanlık, düzen | kilo verme, kalori, "ideal kilo" gibi kilo odaklı dil |

Mağaza görselleri ve ekran görüntüleri de aynı sınıra uyar (ör. "PCOS'u yen" yazan görsel kullanılmaz).

## 7. Ret risklerini azaltma

| Risk | Önlem |
|---|---|
| Genel ya da belirsiz gerekçe | §3.2 metni: özellik, ekran yolu, saklanan alanlar, paylaşım yok. |
| Gerekmeyen izin | Manifest ve birleştirilmiş manifestte yalnızca `READ_SLEEP`; §3.3. |
| Politika uyuşmazlığı | Gerekçe etkinliği, Play'deki URL ile **aynı** metni gösterir; politika 6. bölümü Health Connect'i ayrıntılı anlatır. |
| Politika erişilemiyor | HTML, giriş gerektirmeyen, coğrafi kısıtsız sayfa; PDF değil. |
| İnceleyici özelliği bulamıyor | Ekran yolu gerekçede; düğme Health Connect yüklü değilse de görünür ve kurulum/güncelleme yolunu gösterir. |
| Veri güvenliği ile politika çelişkisi | İkisi de "toplama ve paylaşma yok" der; analitik/araştırma anahtarları kaldırılır (§2). |
| Araştırma çağrışımı | "Araştırmaya katkı" anahtarı Health Connect'in araştırma şartlarını (etik kurul, onam) tetikleyebilir; kaldırılır. |

## 8. Beyanı yeniden gönderme tetikleyicileri

- Yeni Health Connect veri türü (ör. MVP 3: ovülasyon testi, bazal vücut ısısı, adet akışı → üreme sağlığı,
  yüksek hassasiyetli inceleme), yazma izni, arka plan ya da geçmiş okuma: form yeniden doldurulur; mevcut
  türler dahil edilir, kullanılmayanlar çıkarılır.
- `READ_SLEEP`'in kaldırılması: form, tür çıkarılarak yeniden gönderilir.
- Ağ erişimi, SDK ya da yeni bir veri akışı: Veri güvenliği formu ve politika birlikte güncellenir.
- Uygulamanın AB/AEA'da da yayımlanması: politika GDPR açısından ayrıca gözden geçirilir (bu metin KVKK'ya göre yazıldı).

## 9. Kaynaklar

- Play, Health Connect by Android Permissions: https://support.google.com/googleplay/android-developer/answer/9888170
- Play, Android Health Permissions: Guidance and FAQs: https://support.google.com/googleplay/android-developer/answer/12991134
- Play, Health Content and Services: https://support.google.com/googleplay/android-developer/answer/13996823
- Play, Health apps declaration form: https://support.google.com/googleplay/android-developer/answer/14738291
- Play, User Data (gizlilik politikası şartları): https://support.google.com/googleplay/android-developer/answer/10144311
- Play, Data safety section: https://support.google.com/googleplay/android-developer/answer/10787469
- Android, Publish your health app on Google Play (son güncelleme 2026-03-10): https://developer.android.com/health-and-fitness/health-connect/publish
- Android, Declare access to Health Connect data types (son güncelleme 2026-03-10): https://developer.android.com/health-and-fitness/guides/health-connect/publish/declare-access
- Android, Get started with Health Connect (gizlilik politikası etkinliği; son güncelleme 2026-09-08): https://developer.android.com/health-and-fitness/health-connect/get-started
- Teknik ayrıntılar: `docs/arastirma/health-connect-teknik.md`; politika metni: `docs/gizlilik-politikasi.md`.
