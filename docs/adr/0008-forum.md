# ADR-0008: Moderasyonlu forum MVP 3 kapsamı dışında; ön koşullar ve yerine konan

- Tarih: 2026-10-12 · Durum: Kabul edildi · İş: #23 · İlgili: ADR-0002 (veri katmanı), ADR-0010 (fotoğraf),
  `docs/gizlilik-politikasi.md`, `docs/play/health-connect-beyani.md` §8, backlog US-140

## Bağlam
Yol haritası (CLAUDE.md, README) MVP 3'te "forum (moderasyonlu)" diyor; ürün backlog'unda ise aynı istek
(US-140 "PCOS'lu kadınlarla anonim konuşmak") **sürüm 2**'de ve "uzman moderasyonu ile anonim takma ad ön koşul"
notuyla duruyor. Kullanıcı araştırmasındaki gereksinim gerçek: yalnız olmama hissi, deneyim paylaşımı.

Uygulamanın bugünkü taahhütleri (gizlilik politikası "Kısaca" bölümü, CI izin denetimi, KVKK aydınlatma metni):
hesap yok, sunucu yok, **INTERNET izni yok**, üçüncü taraf SDK yok, sağlık verisi yalnızca cihazda. Uygulama içi
bir forum bunların hepsini bozar:

1. **Teknik:** sunucu, hesap/takma ad, oturum, INTERNET izni, ağ kitaplığı, bildirim altyapısı (yanıt bildirimi),
   çevrimdışı önbellek. CI'daki "INTERNET izni eklendi" denetimi bilinçli olarak kırmızıdır.
2. **Hukuki (KVKK):** gönderilerde sağlık bilgisi kaçınılmaz → özel nitelikli kişisel veri; ayrı, belirli ve geri
   alınabilir açık rıza; veri sorumlusu olarak sunucu tarafında saklama, erişim/silme talepleri (m. 11), veri
   ihlali bildirimi (72 saat), yurt dışı aktarım (sunucu konumu), VERBİS kaydı eşikleri, 18 yaş altı kontrolü.
   Takma ad kişisel veriyi anonim yapmaz (yeniden tanımlanabilirlik).
3. **Mağaza:** Google Play "Kullanıcı tarafından oluşturulan içerik" politikası: içerik bildirme, engelleme,
   moderasyon, kullanım koşulları; sağlık içeriğinde yanıltıcı tıbbi bilginin denetimi.
4. **Klinik güvenlik:** gönderilerde doz/ilaç tavsiyesi, yeme bozukluğu tetikleyicileri, kendine zarar ifadeleri →
   7/24 moderasyon, kriz yönlendirme protokolü, uzman (hemşire/diyetisyen) gözetimi. "Moderasyonlu" sıfatı bir
   özellik değil, sürekli bir insan kaynağı gereksinimidir.
5. **Wellness sınırı:** topluluk içeriği uygulamanın "kaynaklı, uzman onaylı" bilgi tonunu sulandırır;
   TİTCK/MDCG 2019-11 değerlendirmesi yeniden yapılır.

## Seçenekler
- **A. Uygulama içi forum, MVP 3'te.** Tüm maddeler bu sürümde çözülmeli; tek geliştiricili, bütçesiz ve
  sunucusuz projede gerçekçi değil. Reddedildi.
- **B. Dış topluluklara bağlantı** (tarayıcı intent'i; INTERNET izni gerekmez). Hangi topluluklar? Ad vermek
  onay anlamına gelir; moderasyon kalitesini denetleyemeyiz; dernek/platform adları doğrulanmadan metne giremez
  (klinik içerik kuralı). Bu sürümde reddedildi; sağlık kuruluşu ya da hasta derneği resmî bir iş birliği
  olursa yeniden bakılır.
- **C. Erteleme + ön koşul listesi; boşluğu cihaz içi özelliklerle kapatma.** Kabul edildi (aşağıda).
- **D. Anonim, sunucusuz "paylaşım kartları"** (deneyimini metin/görsel olarak dışa aktar, kendi seçtiğin
  kanalda paylaş). Sunucu gerektirmez; ilerleyen MVP'lerde "doktor raporu" paylaşım altyapısıyla birleşebilir.
  Bu sürümde kapsam dışı, backlog'a not.

## Karar
1. **MVP 3'te uygulama içi forum yoktur.** CLAUDE.md ve README yol haritasındaki "forum (moderasyonlu)" maddesi
   "topluluk (sürüm 2, ADR-0008 ön koşulları)" olarak güncellenir. Gizlilik politikası ve CI izin denetimi
   değişmez.
2. **Ön koşullar (sürüm 2'de forum ADR'si bunları karşılamadan açılmaz):**
   - Ayrı bir arka uç ve ayrı bir ADR (veri konumu, şifreleme, saklama süresi, silme garantisi, günlükleme).
   - Hesap/takma ad modeli ve 18+ doğrulaması; takma adın kişisel veri olduğu kabulüyle KVKK envanteri,
     ayrı aydınlatma metni ve ayrı açık rıza; politika sürümlemesi (#20) ile yeniden onay.
   - Moderasyon: yazılı topluluk kuralları, içerik bildirme/engelleme, ilaç-doz tavsiyesi ve yeme bozukluğu
     tetikleyicileri için filtre + insan inceleme, kriz yönlendirme (kendine zarar ifadelerinde yardım hattı),
     yanıt süresi hedefi ve bunu karşılayacak insan kaynağı; uzman gözetimi.
   - Play "kullanıcı içeriği" politikası kontrol listesi; kullanım koşulları.
   - Uygulama kilidi, FLAG_SECURE ve "gizli bildirim" ayarının forum bildirimlerine uygulanması.
   - Çevrimdışı çalışmanın korunması: forum olmadan uygulamanın tüm işlevleri çalışmaya devam eder; INTERNET izni
     yalnızca forum modülüne bağlı kalır (ayrı modül, isteğe bağlı).
3. **Boşluğu kapatan cihaz içi işler (MVP 3 ve sonrası):**
   - Bilgi kütüphanesi (US-130): kaynaklı, uzman onaylı kısa yazılar; "yalnız değilsin" tonunda deneyim
     anlatıları yerine kanıta bağlı "sık sorulanlar". MVP 3 sonrasına issue açılır.
   - Doktor raporu (#26) ve "doktora soracaklarım": randevuda sesini duyurma gereksinimi.
   - Motivasyon bildirimlerinin (MVP 1) "yalnız değilsin" dili korunur; kilo odaklı dil yok.

## Sonuçlar
+ Gizlilik taahhütleri, INTERNET izni denetimi ve wellness sınırı MVP 3'te bozulmaz; Play Veri güvenliği formu
  değişmez.
+ Forum kararı bütçe ve insan kaynağı kararıyla birlikte, ayrı bir ADR'de verilir; kapsam sürünmesi önlenir.
− Kullanıcı araştırmasındaki topluluk gereksinimi bu sürümde karşılanmaz; mağaza açıklaması "topluluk" vaat etmez.
− Dış topluluklara yönlendirme yapılmadığı için kullanıcı kendi başına arar; bilgi kütüphanesi bunu kısmen dengeler.
