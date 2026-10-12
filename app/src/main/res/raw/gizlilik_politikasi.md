# Ayla Gizlilik Politikası ve KVKK Aydınlatma Metni

- **Yürürlük tarihi:** 12 Ekim 2026
- **Sürüm:** 1.1 (değişiklikler 14. bölümde)
- **Uygulama:** Ayla, PCOS (yeni adıyla PMOS) öz-yönetim uygulaması, Android (paket adı com.ayla.pcos)
- **Bu metnin güncel sürümü:** [POLİTİKA URL'Sİ]

Bu metin hem Ayla'nın gizlilik politikası hem de 6698 sayılı Kişisel Verilerin Korunması Kanunu ("KVKK") madde 10 uyarınca hazırlanan aydınlatma metnidir. Aynı metin Google Play'deki mağaza sayfasında, uygulamanın içinde ve Health Connect izin ekranındaki "Gizlilik politikası" bağlantısında gösterilir.

## Kısaca

- Ayla'yı kullanmak için hesap açman gerekmez. Ayla'nın sunucusu yoktur ve uygulamanın internet erişim izni yoktur; kayıtların bize ya da başka birine gönderilmez.
- Reklam, analitik, çökme raporu ya da izleme amaçlı hiçbir üçüncü taraf yazılım kiti (SDK) kullanmayız.
- Sağlık kayıtların yalnızca bu cihazda, şifreli bir veritabanında saklanır; şifrenin anahtarı Android Keystore ile korunur. Ayarların ve ilaç adı içermeyen hatırlatma günlüğü uygulamanın kendi alanındaki ayrı bir dosyadadır.
- Cilt ya da saç fotoğrafı eklersen fotoğrafların da yalnızca bu cihazda, ayrı ayrı şifrelenmiş dosyalar olarak durur; galerine yazılmaz, içindeki konum ve cihaz bilgileri saklanmaz ve bu özellik için ayrıca onayın istenir.
- Health Connect'ten yalnızca uyku oturumlarını, yalnızca sen bağlantıyı kurduktan sonra ve uygulama açıkken okuruz. Health Connect'e hiçbir şey yazmayız.
- Verilerini istediğin an bir dosyaya aktarabilir, doktoruna götürmek için PDF rapor hazırlayabilir, düzeltebilir ya da tamamen silebilirsin. Dosyayı ve raporu yalnızca sen paylaşırsın; Ayla kendisi hiçbir yere göndermez.
- Ayla tıbbi cihaz değildir; tanı koymaz, tedavi önermez, doz hesaplamaz, doğum kontrolü için kullanılmaz.

## 1. Veri sorumlusu ve iletişim

Ayla, [GELİŞTİRİCİ ADI] tarafından geliştirilir ve Google Play'de bu adla yayımlanır.

- **Veri sorumlusu:** [GELİŞTİRİCİ ADI]
- **Adres:** [POSTA ADRESİ]
- **E-posta:** [İLETİŞİM E-POSTASI]

Ayla, kayıtların sen açıkça dışa aktarmadıkça ya da paylaşmadıkça cihazından çıkmayacak biçimde tasarlandı. Bu yüzden kayıtlarını göremeyiz, kopyalayamayız; kaybolurlarsa da geri getiremeyiz.

## 2. Bu metin neyi kapsar

Bu metin, Ayla Android uygulamasının işlediği verileri anlatır. Health Connect, Android, Google Play ve verilerini paylaşmayı seçtiğin uygulamalar (örneğin e-posta, mesajlaşma ya da bulut depolama uygulamaları) kendi gizlilik koşullarına tabidir.

Google Play, telefonundaki "kullanım ve teşhis verileri" ayarına bağlı olarak uygulama çökmesi gibi teknik bilgileri kendi politikasına göre toplayabilir ve geliştiricilere toplu istatistik olarak gösterebilir. Bu bilgiler Ayla'daki kayıtlarını içermez.

## 3. Hangi verileri işliyoruz

Aşağıdaki kayıtların hepsi yalnızca bu cihazda tutulur ve sen silene ya da Ayla'yı kaldırana kadar saklanır. Ayla'yı kaldırdığında Android, uygulamanın verilerini ve şifreleme anahtarını da siler. Dışa aktardığın dosyalar ise kaydettiğin yerde kalır.

### 3.1 Adet kayıtları

- **Ne:** adet başlangıç ve bitiş tarihleri; bir döngüyü hesap dışı bırakma tercihin.
- **Neden:** döngü takibi, aralıklı tahmin, 60. ve 90. gün uyarıları ve son 12 ayda 4'ten az adet işareti.
- **Nerede:** şifreli veritabanında.
- **Nasıl silinir:** Takvim'de ilgili günü seçip "Adet kaydını kaldır" ile ya da Ben > Ayarlar > Verilerin > Tüm verilerimi sil ile.

### 3.2 Günlük kayıt

- **Ne:** ruh hâli, enerji, kanama yoğunluğu, semptomlar ve serbest not. İsteğe bağlı olarak kilo ve bel çevresi; bu alanlar varsayılan olarak gizlidir, yalnızca Ayarlar'dan açarsan görünür ve kaydedilir.
- **Neden:** kendi örüntülerini görmen, Ben ekranındaki özetler ve paylaşmayı seçebileceğin doktor notu ya da doktor raporu.
- **Nerede:** şifreli veritabanında.
- **Nasıl silinir:** günlük kayıt ekranındaki "Bu günün kaydını sil" ile tek tek; hepsi için Tüm verilerimi sil ile.

### 3.3 İlaçlar, takviyeler ve dozlar

- **Ne:** girdiğin ilaç ya da takviyenin adı, doz metni, türü ve biçimi, saatleri, yemekle ya da aç karna tercihi, stok sayısı ve uyarı eşiği; doğum kontrol hapı için paket düzeni ve paket başlangıç tarihi. Doz kayıtları: planlanan gün ve saat ile "alındı", "atlandı" ya da "kaçtı" durumu.
- **Neden:** hatırlatma göndermek, hap paketini takip etmek, stok azaldığında haber vermek ve uyumunu göstermek. Ayla doz hesaplamaz, ilaç önermez ve etkileşim uyarısı vermez; ilaç adlarını sen girersin.
- **Nerede:** şifreli veritabanında. Hatırlatma alarmları Android'in alarm hizmetine yalnızca kayıt numarası, gün ve saatle kurulur; ilacın adı bu alarmlara yazılmaz.
- **Nasıl silinir:** bir ilacı listeden kaldırdığında hatırlatmaları durur, ancak geçmiş doz kayıtları uyum geçmişin için saklanmaya devam eder. Bunları da silmek için Tüm verilerimi sil'i kullan.
- **Hatırlatma günlüğü:** hatırlatmaların zamanında çalıp çalmadığını görebilmen için son 14 gündeki en fazla 200 hatırlatmanın türü (doz, su, günlük kontrol, akşam rutini ya da haftalık özet), planlandığı ve gerçekten çaldığı an ve alarmın tam zamanlı olup olmadığı kaydedilir. İlacın adı ya da kayıt numarası yazılmaz. Günlük, 3.6. bölümdeki ayar dosyası gibi Ayla'nın uygulama alanında tutulur; Ben > Ayarlar > Bildirimler > Hatırlatma günlüğü > Temizle ile ya da Tüm verilerimi sil ile silinir.

### 3.4 Su

- **Ne:** günlük bardak sayısı.
- **Neden:** su takibi ve su hatırlatmaları.
- **Nerede:** şifreli veritabanında.
- **Nasıl silinir:** o günün bardak sayısını azaltarak ya da Tüm verilerimi sil ile.

### 3.5 Uyku

- **Ne:** elle girdiğin yatış ve kalkış saatleri ile isteğe bağlı kalite puanı; Health Connect'ten aldığın uyku oturumları (ayrıntılar 6. bölümde).
- **Neden:** uyku düzenini görmen (dün gece ve son 7 gece).
- **Nerede:** şifreli veritabanında.
- **Nasıl silinir:** Planım > Su ve uyku bölümündeki uyku kartından (dün gece ve son 7 gün) tek tek; Health Connect'ten alınanların hepsini Ben > Ayarlar > Health Connect > Health Connect'ten alınan geceleri sil ile; tüm uyku kayıtlarını Tüm verilerimi sil ile.

### 3.6 Profil ve ayarlar

- **Ne:** isteğe bağlı olarak adın; karşılamada seçtiğin öncelikler (döngü düzeni, semptomlar, gebelik planı, doğum kontrol hapı kullanımı); takibe başladığın tarih; hatırlatma, sessiz saat, görünüm ve koruma ayarların; uygulama içindeki rıza tercihlerin.
- **Neden:** uygulamayı sana göre ayarlamak ve seyrek adet işaretini doğru hesaplamak.
- **Nerede:** cihazında, yalnızca Ayla'nın erişebildiği uygulama alanındaki bir ayar dosyasında. Bu dosya Android'in uygulama yalıtımı ve cihaz şifrelemesiyle korunur; veritabanındaki ek şifreleme katmanı bu dosyada yoktur.
- **Nasıl silinir:** Ayarlar'dan değiştirerek; Tüm verilerimi sil ayarları da ilk hâline döndürür.

### 3.7 Doğurganlık kayıtları: bazal vücut ısısı, LH testi, servikal mukus (isteğe bağlı)

- **Ne:** gün başına, sen girersen: sabah bazal vücut ısın (°C) ve ölçüm saatin, ölçümün bozuk olabileceğine dair işaretin, LH (yumurtlama) testi sonucun (negatif, pozitif ya da geçersiz) ve servikal mukus gözlemin.
- **Neden:** döngündeki ısı değişimini grafikte görmen, yumurtlamanın ısı yükselmesiyle geriye dönük doğrulanıp doğrulanmadığını kaydetmen ve bunları doktorunla paylaşabilmen. Bu bölüm doğum kontrolü için kullanılmaz; tanı ya da tedavi değildir. Doğum kontrol hapı kullanırken bu bölüm hesap yapmaz.
- **Nerede:** şifreli veritabanında. Health Connect'ten okunmaz, oraya yazılmaz.
- **Ne kadar:** sen silene ya da Ayla'yı kaldırana kadar.
- **Nasıl silinir:** günlük kayıt ekranındaki Doğurganlık bölümünden o günün kaydını silerek; hepsi için Ben > Ayarlar > Verilerin > Tüm verilerimi sil ile.

### 3.8 Cilt ve saç profili ile bakım rutini (isteğe bağlı)

- **Ne:** kendi gözlemine dayanan öz-beyanların: cilt tipin, aknenin sende nasıl göründüğü (yok, hafif, orta, yoğun), leke ya da koyulaşma, boyun/koltuk altı/kasıkta kadifemsi koyu alanlar, yüz ya da vücut tüylenmesinin seni rahatsız ettiği, saç dökülmesi ya da incelme, gebe ya da emziriyor olduğun; sabah ve akşam rutini hatırlatma saatlerin; hangi gün hangi rutin adımlarını işaretlediğin.
- **Neden:** rutini sana göre seçmek (örneğin gebelik planında ya da gebelikte kaçınılan içerikleri rutinden çıkarmak, hassas ciltte bazı asitleri göstermemek), rutin hatırlatması göndermek ve istersen öz-beyanlarını doktor raporuna koymak. Ayla bunlardan tanı çıkarmaz, puan hesaplamaz, ürün ya da ilaç önermez.
- **Nerede:** şifreli veritabanında.
- **Ne kadar:** sen silene ya da Ayla'yı kaldırana kadar.
- **Nasıl silinir:** Cilt ve saç bölümünde profilini değiştirerek ya da temizleyerek ve rutin işaretlerini kaldırarak; hepsi için Tüm verilerimi sil ile.

### 3.9 Doktora soracakların (isteğe bağlı)

- **Ne:** randevuda sormak için yazdığın sorular, yazıldıkları an ve "sordum" işaretin.
- **Neden:** soruları unutmaman ve istersen doktor raporunun sonuna eklenmesi.
- **Nerede:** şifreli veritabanında.
- **Nasıl silinir:** soruyu listeden silerek; hepsi için Tüm verilerimi sil ile.

### 3.10 Cilt ve saç fotoğrafları (isteğe bağlı)

- **Ne:** cildinin ya da saçının ilerlemesini görmek için çektiğin ya da galerinden seçtiğin fotoğraflar; her fotoğraf için tür (cilt ya da saç), isteğe bağlı bölge notu ve tarih.
- **Neden:** akne, leke ya da koyulaşma, tüylenme ve saç dökülmesindeki değişimi zaman içinde yan yana görebilmen ve istersen doktoruna gösterebilmen. Ayla fotoğraflarını incelemez, yüz tanıma yapmaz, onlardan tanı ya da puan çıkarmaz; fotoğraflar bildirimlerde, ana ekran widget'ında, doktor notunda ve doktor raporunda yer almaz.
- **Nerede:** yalnızca bu cihazda, Ayla'nın kendi alanında, her biri ayrı ayrı şifrelenmiş dosyalar olarak. Fotoğraflar galerine ya da başka uygulamaların görebileceği bir yere yazılmaz; kameradan gelen geçici kopya şifreli kayıttan hemen sonra silinir. Fotoğrafın içindeki konum, cihaz ve çekim zamanı bilgileri (EXIF) saklanmaz; görüntü en uzun kenarı 1600 piksel olacak biçimde küçültülüp yeniden kodlanır. Şifreleme anahtarı Android Keystore ile korunur; fotoğraflar sistem yedeğine ve cihaz aktarımına girmez, dışa aktardığın JSON dosyasında da yer almaz.
- **Ne kadar:** sen silene, fotoğraf onayını geri alana ya da Ayla'yı kaldırana kadar.
- **Nasıl silinir:** fotoğrafı açıp Sil ile tek tek; hepsi için Ben > Ayarlar > Verilerin > Tüm verilerimi sil ile ya da fotoğraf onayını İzinlerin bölümünden geri alarak. Fotoğraflarını düz JPEG olarak seçtiğin bir yere dışa aktarabilirsin; dışa aktarılan dosyalar şifrelenmez, onları güvendiğin bir yerde sakla.

### 3.11 İşlemediğimiz veriler

Ayla konumunu, rehberini, mikrofonu, reklam kimliğini, cihaz tanımlayıcılarını, IP adresini, uygulama kullanım istatistiklerini veya çökme raporlarını toplamaz. Fotoğraflarına yalnızca sen bir cilt ya da saç fotoğrafı çekmek ya da seçmek istediğinde, yalnızca o fotoğraf için erişir; galerini taramaz, kamera ya da galeri izni istemez. Health Connect'ten uyku dışında hiçbir veri türünü okumaz.

## 4. Hukuki sebepler, açık rıza ve toplama yöntemi

- **Sağlık verileri:** adet, günlük kayıt, semptomlar, kilo ve bel çevresi, ilaç ve dozlar (hatırlatma günlüğündeki doz saatleri dahil), uyku kayıtları, doğurganlık kayıtları (bazal vücut ısısı, LH testi, servikal mukus), cilt ve saç profilin ile bakım rutini işaretlerin, doktora soracakların, cilt ve saç fotoğrafların ve önceliklerin arasındaki gebelik planı ile hap kullanımı, KVKK'ya göre özel nitelikli kişisel veridir. Bu veriler KVKK madde 6/3-(a) uyarınca açık rızana dayanarak işlenir. Bu rızayı karşılama ekranında, bu metne ulaşabileceğin bağlantıyla birlikte, ayrı bir onay vererek verirsin. Doğurganlık, cilt ve saç ile doktora sorular bölümleri bu metnin 1.1 sürümüyle eklendi; rızanı daha önceki bir sürümde verdiysen bu bölümleri ilk açtığında onayın bu kayıtlar için ayrıca istenir. Health Connect bağlantısı için, bağlantıyı kurarken ayrıca onay istenir.
- **Fotoğraflar için ayrı onay:** cilt ve saç fotoğrafları yüzünü içerebildiği için diğer kayıtlardan ayrı tutulur. Fotoğraf özelliğini ilk kez kullandığında bu fotoğraflar için ayrı bir onay istenir; onay zamanı ayarlarında saklanır. Onayı Ben > Ayarlar > İzinlerin bölümünden istediğin an geri alabilirsin; geri aldığında fotoğrafların silinir. Fotoğraf eklemezsen bu onay hiç istenmez.
- **Kişiselleştirme (isteğe bağlı, varsayılan kapalı):** açarsan Hareket sekmesindeki öneri, kaydettiğin adet günlerine göre seçilir. Bu da yalnızca cihazında yapılır; Ben > Ayarlar > İzinlerin bölümünden istediğin an kapatabilirsin. Kapalıyken herkese aynı genel içerik gösterilir.
- **Adın ve uygulama ayarların:** uygulamayı sana sunabilmemiz için gereklidir (KVKK madde 5/2-(c)).
- **Toplama yöntemi:** veriler uygulama üzerinden, elektronik ortamda toplanır: senin girdiğin kayıtlar, sen istediğinde Health Connect'ten okunan uyku oturumları, çektiğin ya da galerinden seçtiğin fotoğraflar ve seçtiğin yedek dosyasından içe aktarılan kayıtlar.
- **Rızanı geri alma:** Health Connect iznini (6. bölüm) ve fotoğraf onayını istediğin an kapatabilirsin. Sağlık verilerinin işlenmesine verdiğin rızayı geri almak istersen Ben > Ayarlar > Verilerin > Tüm verilerimi sil ile kayıtlarını silip Ayla'yı kaldırabilirsin; Ayla'nın temel işlevi bu verilere dayandığı için bu rıza olmadan kullanılamaz. Rızanı geri alman, ondan önce yapılan işlemlerin hukuka uygunluğunu etkilemez.
- **Başka amaç yok:** Ayla'da bunların dışında rızaya bağlı başka bir işleme (analitik, araştırma, reklam ya da verilerin cihaz dışına gönderilmesi) yoktur. İleride isteğe bağlı yeni bir özellik eklenirse varsayılan olarak kapalı gelir ve bu metin önceden güncellenir.

## 5. Verilerini nasıl koruyoruz

- **Şifreli veritabanı:** kayıtların SQLCipher ile 256 bit AES kullanılarak şifrelenen bir veritabanında tutulur. Veritabanı parolası 32 baytlık rastgele bir değerdir ve düz metin olarak diske yazılmaz; Android Keystore'da duran ve cihazdan dışarı çıkarılamayan bir AES-GCM anahtarıyla sarılarak saklanır.
- **Fotoğraflar:** her fotoğraf dosyası 256 bit AES-GCM ile ayrı ayrı şifrelenir; fotoğraf anahtarı da veritabanı anahtarı gibi Android Keystore'daki bir anahtarla sarılarak saklanır ve düz metin olarak diske yazılmaz. Fotoğraflar galeriye ya da ortak depolama alanına hiç yazılmaz.
- **Doktor raporu dosyası:** paylaşım menüsünün açabilmesi için PDF rapor uygulamanın önbelleğine şifrelenmeden yazılır ve kısa süre tutulur (7. bölüm).
- **Yedekleme kapalı:** Ayla'nın verileri (fotoğraflar dahil) Google'ın bulut yedeğine ya da cihazdan cihaza aktarıma dahil edilmez. Telefon değiştirirken verilerini dışa aktarıp yeni telefonda Ben > Ayarlar > Verilerin > Yedekten geri yükle ile alabilirsin; fotoğraflar JSON yedeğe girmediği için onları ayrıca JPEG olarak dışa aktarıp taşıman gerekir.
- **Uygulama kilidi (isteğe bağlı):** açarsan Ayla biyometrik doğrulama (örneğin parmak izi) ya da cihaz PIN'inle açılır. Uygulamadan bir dakikadan uzun ayrıldığında yeniden kilitlenir.
- **Ekran görüntüsü engeli (varsayılan olarak açık):** Ayla ekranlarının görüntüsü alınamaz ve son uygulamalar listesinde içerik gizlenir.
- **Gizli bildirimler (isteğe bağlı):** 8. bölüme bak.
- **Bilmen gereken sınırlar:** hatırlatmaların ekran kilitliyken de çalışabilmesi için veritabanı anahtarı ayrıca kimlik doğrulaması istemez; uygulama kilidi ayrı bir katmandır. Bu yüzden telefonunda ekran kilidi kullanmanı öneririz. Root'lanmış ya da kötü amaçlı yazılım bulaşmış cihazlarda hiçbir koruma tam güvence veremez.
- **Anahtar kaybı:** Android Keystore anahtarı geçersiz hâle gelirse (örneğin bazı sistem güncellemeleri ya da güvenlik sıfırlamalarından sonra) önceki kayıtlar açılamaz. Ayla bu durumda kayıtlarının üzerine sessizce yazmaz; açılamayan dosyayı kenara ayırır, yeni ve boş bir başlangıç yapar ve sana haber verir. Açılamayan dosya, anahtarı olmadığı için okunamaz hâlde cihazda kalabilir; Tüm verilerimi sil bu dosyaları da siler, Ayla'yı kaldırdığında da silinir. Aynı sınır fotoğraflar için de geçerlidir: fotoğraf anahtarı açılamazsa fotoğraflar geri getirilemez; Ayla sana haber verir ve temiz bir başlangıç yapmayı sana bırakır. Bu yüzden ara ara dışa aktarma yapmanı öneririz.

## 6. Health Connect

Health Connect, Android'de sağlık ve fitness verilerini cihazda bir arada tutan bir Google hizmetidir. Android 14 ve üstünde sistemin parçasıdır; Android 13 ve altında Google Play'den yüklenen bir uygulamadır. Ayla'yı Health Connect'e bağlamak isteğe bağlıdır; bağlamasan da uykunu elle girebilirsin.

- **Okunan veri:** yalnızca uyku oturumları (Health Connect'teki "Uyku" veri türü, READ_SLEEP izni). Ayla başka hiçbir veri türüne erişim istemez; adet, bazal vücut ısısı, yumurtlama testi gibi kayıtları Health Connect'ten okumaz ve oraya yazmaz, bunları yalnızca sen Ayla'ya girersin.
- **Ne zaman:** yalnızca Ayla açıkken. Sen "Health Connect'ten al" dediğinde okur; bağlantı açık kaldıkça Ayla'yı açtığında yeni geceleri eklemek için de okuyabilir. Ayla Health Connect'i arka planda okumaz.
- **Ne kadar geriye:** Ayla yalnızca son 7 günün uyku oturumlarını okur; uyku kartı da son 7 geceyi gösterir. Daha eski kayıtlar için izin istemez.
- **Saklanan alanlar:** her oturumun başlangıç ve bitiş zamanı ile aynı geceyi iki kez eklememek için Health Connect kayıt kimliği. Kaydı oluşturan uygulamanın adı, uyku evreleri, oturum başlığı ve notları saklanmaz. Ayla'dan sildiğin bir Health Connect gecesi bir sonraki almada geri gelmesin diye yalnızca o kaydın kimliği (saat ya da süre olmadan), kayıt 7 günlük pencereden çıkana kadar 3.6. bölümdeki ayar dosyasında tutulur.
- **Kullanım amacı:** uyku kaydını elle saat girmeden tamamlamak. Alınan geceler, Planım'daki uyku kartında elle girdiğin gecelerle birlikte gösterilir. Başka hiçbir amaçla kullanılmaz.
- **Saklama yeri:** yalnızca cihazında, diğer kayıtlarınla aynı şifreli veritabanında.
- **Paylaşım:** Health Connect'ten alınan veriler cihazından çıkmaz; kimseye aktarılmaz veya satılmaz; reklam, profil çıkarma, kredi ya da sigorta değerlendirmesi gibi amaçlarla kullanılmaz; biz dahil hiç kimse tarafından okunmaz. Yalnızca sen dışa aktarırsan oluşturduğun dosyada yer alır.
- **Yazma yok:** Ayla Health Connect'e veri yazmaz, oradaki kayıtları değiştirmez veya silmez. Ayla'daki geceler Health Connect'teki kayıtların kopyasıdır: Ayla'da sildiğin geceler Health Connect'teki asıl kayıtları etkilemez. Health Connect'te sildiğin bir kaydın Ayla'daki kopyasını da kaldırmak için onu Ayla'dan ayrıca silmen gerekebilir.
- **İzni geri alma:** Health Connect ayarlarında (Android 14 ve üstünde sistem Ayarları içinde, Android 13 ve altında Health Connect uygulamasında) "Uygulama izinleri" bölümünden Ayla'yı seçerek ya da Ayla'nın Ayarlar ekranındaki Health Connect bölümünden. İzin kapandıktan sonra Ayla yeni veri okuyamaz. Daha önce alınan geceler sen silene kadar Ayla'da kalır; onları 3.5. bölümde anlatıldığı gibi silebilirsin. Ayla'yı kaldırdığında Health Connect izinleri de kendiliğinden geri alınır.

## 7. Paylaşım ve aktarım

Ayla kişisel verilerini yurt içinde ya da yurt dışında hiçbir üçüncü kişiye aktarmaz. Verilerini bizim adımıza işleyen bir hizmet sağlayıcımız (bulut, barındırma, analitik) yoktur. Kayıtların bizde bulunmadığı için, bir kişi ya da makam talep etse bile tarafımızdan verilemez.

Verilerin yalnızca senin açıkça başlattığın şu işlemlerle cihazından çıkabilir:

- **Doktor notu:** Ben ekranındaki doktor notu, son 28 güne ait döngü, ilaç ve semptom özetini düz metin olarak hazırlar ve Android'in paylaşım menüsünü açar. Metni hangi uygulamayla kime göndereceğini sen seçersin; Ayla metni kendisi hiçbir yere göndermez. Gönderdiğin uygulamanın koşulları geçerli olur.
- **Doktor raporu (PDF):** Ben ekranındaki doktor raporu, seçtiğin tarih aralığı (28, 90 ya da 180 gün ya da özel aralık) ve seçtiğin bölümler için kayıtlarının özetini Türkçe bir PDF olarak hazırlar. Rapora girebilenler: döngü (son adet, son 12 aydaki adet sayısı, döngü uzunlukları, "60 gündür adet görülmedi" ya da "4'ten az adet" gibi kayıt cümleleri, hap kullanımı), günlük kayıt (kayıt tutulan gün sayısı, kanama günleri, ruh hâli ve enerji ortalaması, en sık işaretlediğin semptomlar; kilo ve bel çevresi yalnızca Ayarlar'da açtıysan), ilaç ve takviyeler (girdiğin ad ve doz metni, saatler, alınan/planlanan doz sayısı), uyku ve su ortalamaları, doğurganlık (ısı ölçümleri ve grafiği, LH pozitif günler, ısı yükselmesiyle doğrulanan yumurtlama tahmini; hap kullanırken bu bölüm yoktur ve bölümün sonunda "doğum kontrolü için kullanılmaz" ibaresi bulunur), cilt ve saç öz-beyanların ile rutin işaret sayıları ve doktora soracakların. Rapor yorum, tanı, tedavi önerisi ya da doz hesabı içermez ve ilk satırında bunu söyler; fotoğrafların ve adın rapora girmez. Paylaşım menüsünün açabilmesi için dosya uygulamanın önbelleğine şifrelenmeden yazılır; en fazla son 3 rapor tutulur ve raporlar en geç 24 saat sonra ya da uygulamayı bir sonraki açışında silinir. Dosya adı kişisel veri içermez (ayla-doktor-raporu-tarih.pdf). Raporu hangi uygulamayla kime göndereceğini sen seçersin; Ayla raporu kendisi hiçbir yere göndermez ve gönderdiğin uygulamanın koşulları geçerli olur. Aynı özeti düz metin olarak da paylaşabilirsin.
- **Hatırlatma günlüğü:** Ayarlar'daki hatırlatma günlüğünde Paylaş'a dokunursan günlük; telefonunun markası ve modeli, Android sürümü, tam zamanlı alarm izni ve pil ayarı durumuyla birlikte düz metin olarak hazırlanır ve paylaşım menüsü açılır. Kime göndereceğini sen seçersin; Ayla metni kendisi hiçbir yere göndermez.
- **Dışa aktarma:** Ben > Ayarlar > Verilerin > Verilerimi dışa aktar, tüm kayıtlarını (adet, günlük kayıt, ilaç ve dozlar, su, uyku, doğurganlık kayıtları, cilt ve saç profilin ile rutin işaretlerin, doktora soracakların) ve ayarlarını (rıza ve koruma ayarları hariç) bir JSON dosyasına yazar ve dosyayı senin seçtiğin yere kaydeder. Fotoğraflar bu dosyaya girmez. Bu dosya şifrelenmez; içindeki sağlık verilerini dosyaya erişen herkes okuyabilir. Dosyayı güvendiğin bir yerde sakla; bir bulut hizmetine kaydedersen o hizmetin koşulları geçerli olur.
- **Fotoğrafları dışa aktarma:** Cilt ve saç bölümünden tek bir fotoğrafı ya da hepsini, seçtiğin bir klasöre düz JPEG olarak kaydedebilirsin. Dosya adı fotoğrafın türünü ve tarihini içerir (örneğin ayla-cilt-2026-10-12-1a2b3c4d.jpg). Dışa aktarılan fotoğraflar şifrelenmez ve içlerine konum ya da cihaz bilgisi yazılmaz.
- **Yedekten geri yükleme:** seçtiğin dosya yalnızca cihazında okunur; kayıtlarına eklenir ya da sen seçersen onların yerine yazılır. Fotoğraflar yedekten geri yüklenmez.

## 8. Bildirimler

Hatırlatma bildirimlerinde ilacın adı ve dozu, su, döngü ve cilt bakım rutiniyle ilgili kısa metinler ve haftalık özet görünebilir; fotoğraflar ve doğurganlık kayıtların bildirimlerde yer almaz. Ben > Ayarlar > Koruma > Gizli bildirimler açıkken kilit ekranında yalnızca "Bir hatırlatman var" gibi genel bir metin görünür. Bildirimleri Android gösterir; bildirim erişimi verdiğin başka uygulamalar (örneğin akıllı saat uygulamaları) bildirim içeriğini görebilir. Bildirimleri Android ayarlarından tamamen kapatabilirsin.

Ana ekran widget'ı isteğe bağlıdır; eklersen ana ekranında bugünün döngü ya da hap günü, sıradaki dozun saati ve adı ve su sayın görünür. Telefonunun kilidini açabilen herkes bunları görebilir. Ekran görüntüsü engeli yalnızca Ayla'nın kendi ekranlarını kapsar; ana ekranın görüntüsünü aldığında ya da ekranını paylaştığında widget'taki bilgiler de görünür. Uygulama kilidi ya da Gizli bildirimler açıkken widget yalnızca "Ayla" yazar. Widget kilit ekranına eklenemez; durumunu ayrı bir yerde saklamaz, her seferinde şifreli veritabanından okur.

## 9. Android izinleri

- **Bildirim gösterme (POST_NOTIFICATIONS):** hatırlatmalar için. İlk kez gerektiğinde istenir; vermezsen Ayla bildirimsiz çalışır.
- **Tam zamanlı alarm (SCHEDULE_EXACT_ALARM):** hatırlatmaların tam saatinde çalması için. Vermezsen hatırlatmalar gecikebilir; telefon uzun süre kullanılmadığında gecikme 10 dakikayı aşabilir.
- **Açılışta başlatma (RECEIVE_BOOT_COMPLETED):** telefon yeniden başladığında hatırlatmaları yeniden kurmak için.
- **Titreşim (VIBRATE):** bildirimlerde titreşim için.
- **Biyometrik doğrulama (USE_BIOMETRIC; eski Android sürümlerinde USE_FINGERPRINT):** isteğe bağlı uygulama kilidi için.
- **Health Connect uyku okuma (READ_SLEEP):** yalnızca bağlantıyı kurarsan, 6. bölümde anlatılan amaçla.

Fotoğraf çekme ve seçme için yeni bir Android izni istenmez: kamera ve fotoğraf seçici, Android'in kendi ekranlarıyla açılır ve Ayla yalnızca o anda çektiğin ya da seçtiğin tek fotoğrafı alır. Ayla kamera (CAMERA) ya da galeri okuma (READ_MEDIA_IMAGES) izni istemez.

Ayla internet erişim izni (INTERNET) istemez; bu yüzden verilerini ağ üzerinden gönderemez.

## 10. Otomatik değerlendirmeler

Ayla, kayıtlarından döngü tahmin aralığını, 60. ve 90. gün uyarılarını ve son 12 ayda 4'ten az adet işaretini cihazında otomatik olarak hesaplar. Bunlar yalnızca bilgilendirme amaçlıdır; tanı değildir ve senin hakkında hukuki ya da benzeri bir sonuç doğurmaz. Tahmini Ben > Ayarlar > Görünüm > Tahmini gizle ile gizleyebilir, Takvim'de bir döngüyü "Bu döngüyü hesaba katma" ile hesap dışı bırakabilir ve döngü bildirimlerini kapatabilirsin. Doğum kontrol hapı kullanımında döngü tahmini yapılmaz.

Doğurganlık bölümünü kullanırsan Ayla, ısı ölçümlerine sabit bir kural (önceki altı geçerli ölçümün üstünde art arda üç ölçüm) uygulayarak yumurtlamanın geriye dönük doğrulanıp doğrulanmadığını, bozuk işaretli ölçüm sayısını ve yumurtlama sonrası gün sayısını hesaplar; bunlara göre "henüz yeterli ölçüm yok", "LH pozitif ama ısı yükselmedi" ya da "yumurtlama sonrası 18 günü geçti" gibi notlar gösterir. Notlar kayıtlarının özetidir; tanı değildir, gebe olduğunu ya da olmadığını söylemez ve doğum kontrolü için kullanılmaz. Hap kullanırken hiçbir hesap yapılmaz. Bölümü "Hamile kalmayı planlıyorum" önceliğini kapatarak gizleyebilir, kayıtlarını silebilirsin.

Cilt ve saç bölümünde rutin adımları, profiline girdiğin öz-beyanlara göre sabit kurallarla seçilir (örneğin gebelik planında ya da gebelikte kaçınılan içerikler çıkarılır, yoğun akne işaretlediysen aktif içerik gösterilmez ve dermatoloğa görünme kartı öne çıkar). Bu bir tanı ya da tedavi kararı değildir; profilini boşaltırsan yalnızca temel ritim (temizleyici, nemlendirici, güneş koruyucu) gösterilir. Fotoğraflar üzerinde hiçbir otomatik değerlendirme yapılmaz.

## 11. KVKK kapsamındaki hakların

KVKK madde 11 uyarınca veri sorumlusuna başvurarak:

- a) kişisel verilerinin işlenip işlenmediğini öğrenme,
- b) kişisel verilerin işlenmişse buna ilişkin bilgi talep etme,
- c) kişisel verilerinin işlenme amacını ve bunların amacına uygun kullanılıp kullanılmadığını öğrenme,
- ç) yurt içinde veya yurt dışında kişisel verilerinin aktarıldığı üçüncü kişileri bilme,
- d) kişisel verilerin eksik veya yanlış işlenmişse bunların düzeltilmesini isteme,
- e) KVKK madde 7'de öngörülen şartlar çerçevesinde kişisel verilerinin silinmesini veya yok edilmesini isteme,
- f) (d) ve (e) bentleri uyarınca yapılan işlemlerin, kişisel verilerinin aktarıldığı üçüncü kişilere bildirilmesini isteme,
- g) işlenen verilerin münhasıran otomatik sistemler vasıtasıyla analiz edilmesi suretiyle aleyhine bir sonucun ortaya çıkmasına itiraz etme,
- ğ) kişisel verilerinin kanuna aykırı olarak işlenmesi sebebiyle zarara uğraman hâlinde zararın giderilmesini talep etme

haklarına sahipsin.

**Ayla'da bu haklar nasıl karşılanır:**

- Kayıtların bizde değil cihazında olduğu için haklarının çoğunu doğrudan uygulamadan kullanabilirsin: kayıtlarını görebilir ve düzeltebilir, dışa aktarabilir ve silebilirsin.
- Hiçbir üçüncü kişiye aktarım yapılmadığından, (ç) ve (f) bentlerinde bildirilecek bir alıcı yoktur.
- Otomatik değerlendirmeler ve bunları nasıl kapatabileceğin için 10. bölüme bak.

**Başvuru:** taleplerini [İLETİŞİM E-POSTASI] adresine e-postayla ya da [POSTA ADRESİ] adresine yazılı olarak iletebilirsin. Başvurunu en geç 30 gün içinde ve kural olarak ücretsiz sonuçlandırırız (KVKK madde 13). Kayıtların bizde bulunmadığı için başvurunda sağlık bilgisi paylaşmana gerek yoktur; lütfen paylaşma. Başvurun reddedilirse, yanıtı yetersiz bulursan ya da süresinde yanıt verilmezse, yanıtı öğrendiğin tarihten itibaren 30 gün ve her hâlde başvuru tarihinden itibaren 60 gün içinde Kişisel Verileri Koruma Kurulu'na şikâyette bulunabilirsin (KVKK madde 14).

## 12. Çocuklar

Ayla yetişkinler için tasarlanmıştır; 18 yaşından küçüklere yönelik değildir.

## 13. Tıbbi uyarı

Ayla tıbbi bir cihaz değildir; herhangi bir hastalığı teşhis etmez, tedavi etmez, iyileştirmez veya önlemez. Uygulamadaki bilgiler tıbbi tavsiye yerine geçmez. Tanı ve tedavi için doktoruna ya da bir sağlık profesyoneline danış.

## 14. Değişiklikler

Bu metni değiştirdiğimizde yürürlük tarihini ve sürüm numarasını güncelleriz. Verilerinin işlenme biçimini etkileyen önemli değişiklikleri, yürürlüğe girmeden önce uygulama içinde de duyururuz; yeni bir rıza gerekiyorsa ayrıca isteriz. Önceki sürümlere [POLİTİKA URL'Sİ] adresinden ulaşabilirsin.

**Sürüm geçmişi:**

- **1.1 (12 Ekim 2026):** Doğurganlık kayıtları (3.7), cilt ve saç profili ile bakım rutini (3.8), doktora sorular (3.9) ve ayrı onayla saklanan şifreli cilt ve saç fotoğrafları (3.10, 4, 5, 9) eklendi; "fotoğraf toplanmaz" cümlesi buna göre düzeltildi (3.11). Doktor raporu PDF'sinin içeriği, önbellekte kısa süreli tutulması ve yalnızca senin paylaşman (7) ile doğurganlık ve cilt bölümlerindeki otomatik hesaplar (10) anlatıldı. Health Connect kapsamı değişmedi: yalnızca uyku okunur.
- **1.0 (12 Ekim 2026):** İlk sürüm.

## 15. İletişim

Bu metinle ya da verilerinle ilgili sorularını [İLETİŞİM E-POSTASI] adresine yazabilirsin.
