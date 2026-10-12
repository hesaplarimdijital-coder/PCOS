# Planım içeriği: beslenme, hareket ve bitkisel destekler (MVP 2)

Durum: klinik içerik taslağı, uygulamaya hazır · 12 Ekim 2026 · İlgili işler: #12 (Beslenme ve Hareket
sekmeleri), #13 (bitkisel öneriler) · Kanıt satırları: [pcos-kanit-ozeti.md](pcos-kanit-ozeti.md)

Bu belge tıbbi tavsiye değildir. Ayla'nın wellness sınırı (TİTCK / MDCG 2019-11) içinde kalan, `strings.xml`'e
girecek metinleri ve dayanaklarını toplar.

## Kullanım kuralları

- **Anahtarlar.** Başlık ve metin sütunu olan tablolarda taban anahtara sonek eklenir: `<anahtar>_title`,
  `<anahtar>_text`, kullanıcıya görünen kısa künye `<anahtar>_source`. Kanıt etiketlerinde `_label` ve `_text`.
  Rutin adımları, tabak dilimleri ve bitkisel alanlar tabloda tam anahtarla verilir.
- **Sınırlar.** Başlık ≤ 40, metin ≤ 180, rutin adımı ≤ 80, tabak dilimi ≤ 18 karakter. Hepsi betikle sayıldı:
  en uzun başlık 32, en uzun metin 176, en uzun adım 71 karakter.
- **XML'e aktarım.** `’` → `&#8217;` (ya da `\'`). Metinlerde `%`, `&`, `<` ve düz tırnak yok; `8–12` gibi
  aralıklardaki uzun tire olduğu gibi kalır. Biçim dizgesi yalnızca "Ortak metinler" tablosunda var.
- **Kanıt etiketi** her kartta görünür (D bölümü); etiket metin ve simgeyle ayırt edilir, renk tek başına anlam
  taşımaz.
- **Kaynak satırı** (`_source`) tablodaki "Kaynak" sütunudur; `translatable="false"` önerilir. Tam künye ve
  doğrulama durumu E bölümünde.
- **Dil.** Şu sözcükler geçmez: kalori, kilo, yağ yakma, ideal, diyet (meslek adı "diyetisyen" hariç), hedef,
  kaçırdın, yasak, zayıflama, detoks. "Hedef" yerine "ritim", "kaçırdın" yerine "kaçtı / kaçarsa". Kişi önce
  gelir: "PCOS’u olan kadınlar".
- **Klinik sınır.** Tanı, tedavi önerisi, doz ya da kür, kişiye özel etkileşim denetimi yok. Bitkisel bölümde
  miktar ve süre verilmez; çalışma protokolleri yalnızca kanıt özetinde durur.
- **Adlandırma.** Her sekmenin ilk anışında "PCOS (yeni adıyla PMOS)", sonra "PCOS".

## Ortak metinler

| Anahtar | Metin (≤ 180) | Not |
|---|---|---|
| `plan_content_disclaimer` | Bu bilgiler geneldir; tanı ya da tedavi yerine geçmez. Sana uygun olanı doktorunla konuşabilirsin. | Beslenme, Hareket ve Bitkisel bölümlerinin sonunda |
| `plan_source_format` | Kaynak: %1$s | `%1$s` = `<anahtar>_source` |
| `movement_duration_short` | %1$d dk | Rutin süresi rozeti |
| `movement_duration_a11y` | %1$d dakika | Ekran okuyucu için; "dk" harf harf okunmasın |

## A) Beslenme sekmesi

### A0. Giriş

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `nutrition_intro` | Beslenmede ritim | PCOS (yeni adıyla PMOS) için hazırlanan bu ilkeler 2023 uluslararası kılavuzuna dayanır. Hepsini birden uygulaman gerekmez; sana uyanla başla. | güçlü | Teede ve ark. 2023, Fertil Steril / Hum Reprod |

### A1. İlke kartları (6)

Sıra bilinçli: önce esneklik, sonra pratik ilkeler. Hiçbir kart "yerine şunu ye" ya da "bırak" demez.

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `nutrition_flexible` | Sana uyan bir düzen | Kılavuza göre PCOS’ta tek bir doğru beslenme biçimi yok. Kısıtlayıcı listeler yerine sevdiğin yiyeceklerle sürdürebileceğin bir düzen öneriliyor. | güçlü | Teede ve ark. 2023, Fertil Steril / Hum Reprod |
| `nutrition_rhythm` | Düzenli öğün ritmi | Öğünlerini güne benzer aralıklarla yaymak, açlığın birden bastırmasını önlemeye yardımcı olabilir. Bir öğün kaçarsa sorun değil; bir sonrakiyle devam. | uzman görüşü | St-Onge ve ark. 2017, Circulation (AHA) |
| `nutrition_fiber_protein` | Lif ve protein bir arada | Sebze, baklagil ve tam tahıldaki lif ile yoğurt, yumurta, balık gibi proteinler öğünü daha doyurucu yapabilir ve kan şekerinin dengeli seyretmesine yardımcı olabilir. | orta | Reynolds ve ark. 2019, Lancet; Reynolds ve ark. 2020, PLoS Med; Dhillon ve ark. 2016, J Acad Nutr Diet |
| `nutrition_slow_carbs` | Kan şekerine nazik seçimler | Bulgur, mercimek, nohut ve kuru fasulye kan şekerini daha yavaş yükseltir; PCOS’ta yararı küçük çalışmalarda görüldü. Bir öğünde tek bir değişiklikle başlayabilirsin. | sınırlı | Kazemi ve ark. 2021, Adv Nutr; Atkinson ve ark. 2021, Am J Clin Nutr |
| `nutrition_water` | Susadığında önce su | Şekerli içecekler kan şekerini hızla yükseltebilir. Günlük içeceğin çoğunlukla su, şekersiz çay ya da maden suyu olabilir; ara sıra keyif içeceği de olur. | güçlü | DSÖ 2015, şeker kılavuzu; Imamura ve ark. 2015, BMJ |
| `nutrition_whole_foods` | Çoğunlukla az işlenmiş | Sofranın çoğunu ev yemekleri, sebze, meyve, baklagil ve yoğurt gibi az işlenmiş yiyecekler oluşturabilir. Paketli ürünler de hayatın parçası; her yiyeceğe yer var. | sınırlı | Lane ve ark. 2024, BMJ; TÜBER 2022, T.C. Sağlık Bakanlığı |

Editör notları:
- `nutrition_slow_carbs`: örnekler bilerek seçildi. Tam buğday ekmeği çoğu ölçümde beyaz ekmeğe yakın glisemik
  indekse sahip olduğu için bu kartta yer almaz (Atkinson 2021). "Daha yavaş yükseltir" fizyolojik bir olgu;
  "sınırlı" etiketi PCOS'taki yarar kanıtı içindir ve metin bunu açıkça söyler.
- `nutrition_flexible`: kılavuz önerisidir, bu yüzden "güçlü"; altta yatan çalışmaların kesinliği düşüktür (kanıt
  özetinde yazılı).
- `nutrition_rhythm`: yeme bozukluğu riskine karşı esnek kurgu; "atladıysan", "kaçırdıysan" yazılmaz.

### A2. Tabak modeli

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `nutrition_plate` | Tabak modeli | Tabağın yarısı sebze, çeyreği protein, çeyreği tam tahıl ya da baklagil olabilir; yanında su. Tartmak ya da saymak gerekmez, göz kararı yeter. | uzman görüşü | Harvard T.H. Chan, Healthy Eating Plate 2011; ADA, Diabetes Plate Method |

Çizim dilimleri:

| Anahtar | Etiket (≤ 18) | Çizimdeki yeri |
|---|---|---|
| `nutrition_plate_slice_vegetables` | Sebze | Tabağın yarısı |
| `nutrition_plate_slice_protein` | Protein | Çeyrek |
| `nutrition_plate_slice_grains` | Tam tahıl | Çeyrek |
| `nutrition_plate_slice_water` | Su | Tabağın yanında bardak |

Örnekler ve erişilebilirlik:

| Anahtar | Metin (≤ 180) |
|---|---|
| `nutrition_plate_vegetables_examples` | Salata, zeytinyağlı sebze yemekleri, ızgara sebze, çorba |
| `nutrition_plate_protein_examples` | Yumurta, yoğurt, peynir, balık, tavuk, köfte |
| `nutrition_plate_grains_examples` | Bulgur, tam buğday ekmeği, mercimek, nohut, kuru fasulye |
| `nutrition_plate_legume_note` | Baklagiller hem protein hem lif kaynağıdır; tabağın protein ya da tahıl çeyreğine koyabilirsin. |
| `nutrition_plate_a11y` | Tabak çizimi: yarısı sebze, çeyreği protein, çeyreği tam tahıl ya da baklagil. Yanında bir bardak su. |

Çizim notu: "Tam tahıl, baklagil" 19 karakter olduğu için dilim etiketi "Tam tahıl" kısaltıldı; baklagil bilgisi
not ve örneklerde. Canvas `contentDescription` = `nutrition_plate_a11y`; dilimler renk, etiket ve konumla ayrılır.
Büyük yazıda (≥ 1,3) etiketler dilimlerin içinden çizimin altındaki listeye taşınır. Porsiyon, gram, ölçü yok.

### A3. Yemek sonrası yürüyüş ipucu

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `nutrition_walk_tip` | Yemekten sonra kısa yürüyüş | Yemekten sonra 10–15 dakika yürümek, tokluk kan şekerinin daha yumuşak yükselmesine yardımcı olabilir. Hareket sekmesinde hazır bir rutin var. | orta | Buffey ve ark. 2022, Sports Med; Reynolds ve ark. 2016, Diabetologia |

İsteğe bağlı hatırlatma (eski ön izleme metni "yürüyüş hatırlatması" diyordu). Eklenirse "Gizli bildirimler"
ayarına uyar ve sessiz saatlerde gelmez:

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `notif_walk` | Kısa bir yürüyüş? | Yemekten sonra 10 dakika yürümek iyi gelebilir. Bugün istemezsen sorun değil. | orta | Buffey ve ark. 2022, Sports Med |

### A4. Yemekle ilişki zorlayıcıysa

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `nutrition_support` | Yemekle ilişkin zorlayıcıysa | Yemek düşünceleri gününü zorluyorsa, suçluluk ya da kontrol kaybı hissediyorsan yalnız değilsin; PCOS’ta bu daha sık görülür. Hekim, diyetisyen ya da psikologla konuşabilirsin. | güçlü | Teede ve ark. 2023, Fertil Steril / Hum Reprod; J Clin Endocrinol Metab 2024, meta-analiz |

Yerleşim: sekmenin sonunda, `plan_content_disclaimer`'dan önce; kişiselleştirme rızasından bağımsız, her zaman
görünür. Uyarı bandı değil, sakin bir bilgi kartı. Tanı dili yok ("yeme bozukluğun olabilir" yazılmaz), uygulama
içinde tarama testi yapılmaz.

## B) Hareket sekmesi

### B0. Giriş

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `movement_intro` | Kendi temponda | Burada amaç performans değil, sürdürülebilir bir ritim. 2023 uluslararası kılavuzu düzenli hareketi PCOS (yeni adıyla PMOS) yönetiminin temel parçası sayar. | güçlü | Teede ve ark. 2023, Fertil Steril / Hum Reprod |

### B1. Haftalık ritim

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `movement_weekly` | Haftalık hareket ritmi | Kılavuz haftada en az 150 dk orta ya da 75 dk yoğun tempolu hareket ve 2 gün kas güçlendirme öneriyor. Daha azı da değerli: her dakika sayılır. | güçlü | Teede ve ark. 2023, Hum Reprod; Bull ve ark. 2020, Br J Sports Med (DSÖ) |
| `movement_every_minute` | Her dakika sayılır | Merdiven çıkmak, ev işi, yürüyerek bir yere gitmek de harekettir. Kısa parçalar gün içinde birikir; biraz hareket hiç yoktan iyidir. | güçlü | Bull ve ark. 2020, Br J Sports Med (DSÖ); Teede ve ark. 2023, Hum Reprod |
| `movement_intensity` | Tempo nasıl anlaşılır? | Orta tempoda konuşabilirsin ama şarkı söyleyemezsin. Yoğun tempoda birkaç kelimeden fazlasını söylemek zorlaşır. | uzman görüşü | CDC, Measuring Physical Activity Intensity |

Not: Kartta ilerleme gösterilecekse "hedef", "başarı" ya da yüzde kullanılmaz; "Bu hafta 40 dk" gibi nötr bir sayı
yeterli. Kılavuzdaki 250 / 150 dk eşiği kilo yönetimi içindir, uygulamada gösterilmez.

### B2. Kısa rutinler (4)

Süreler kodda dakika olarak tutulur ve "Ortak metinler"deki biçim dizgeleriyle gösterilir: yürüyüş 10, kuvvet 15,
esneme 10, oturma molası 3 dakika. Adımlar numaralı liste olarak, her biri ayrı satırda çizilir.

#### Rutin 1: Yemek sonrası yürüyüş (10 dk)

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `movement_routine_walk` | Yemek sonrası yürüyüş | Yemekten sonra kısa bir yürüyüş, tokluk kan şekerinin daha yumuşak yükselmesine yardımcı olabilir. Hız değil süreklilik önemli. | orta | Buffey ve ark. 2022, Sports Med; Reynolds ve ark. 2016, Diabetologia; DiPietro ve ark. 2013, Diabetes Care |

| Anahtar | Adım (≤ 80) |
|---|---|
| `movement_routine_walk_step_1` | Yemeği bitirince çok beklemeden kalk; rahat bir ayakkabı yeter. |
| `movement_routine_walk_step_2` | İlk 2 dakika yavaş yürü, nefesin sakinleşsin. |
| `movement_routine_walk_step_3` | Rahatça konuşabileceğin bir tempoyla 6 dakika devam et. |
| `movement_routine_walk_step_4` | Dışarı çıkamıyorsan evde, koridorda ya da balkonda dolaş. |
| `movement_routine_walk_step_5` | Son 2 dakikada yavaşla, omuzlarını gevşet. |

#### Rutin 2: Ekipmansız kuvvet (15 dk)

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `movement_routine_strength` | Ekipmansız kuvvet | Kılavuz haftada 2 gün kas güçlendirmeyi öneriyor. Bu rutin evde, ekipmansız yapılır; zorlanırsan tekrarı azalt, ağrı olursa dur. | güçlü | Teede ve ark. 2023, Hum Reprod; Bull ve ark. 2020 (DSÖ); ESSA 2024, J Sci Med Sport |

| Anahtar | Adım (≤ 80) |
|---|---|
| `movement_routine_strength_step_1` | 2 dakika ısın: yerinde yürü, kollarını öne ve arkaya çevir. |
| `movement_routine_strength_step_2` | Sandalyeden otur-kalk: yavaşça otur, topuklarından güç alarak kalk. |
| `movement_routine_strength_step_3` | Duvar şınavı: ellerin duvarda, göğsünü duvara yaklaştırıp uzaklaştır. |
| `movement_routine_strength_step_4` | Köprü: sırtüstü, dizler bükülü; kalçanı kaldır, yavaşça indir. |
| `movement_routine_strength_step_5` | Her hareketi 8–12 kez yap, kısa mola ver; turu 2–3 kez tekrarla. |
| `movement_routine_strength_step_6` | Son 2 dakika bacaklarını ve göğsünü esneterek bitir. |

Not: "Güçlü" etiketi kılavuz önerisine (haftada 2 gün kas güçlendirme) aittir; hareketlerin seçimi uzman görüşüdür
ve kanıt özetinde ayrı yazılır. Tekrar sayısı egzersiz yönergesidir, doz değildir.

#### Rutin 3: Esneme ve nefes (10 dk)

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `movement_routine_stretch` | Esneme ve nefes | Yavaş nefes ve hafif esneme gevşemeye yardımcı olabilir. PCOS’ta yoga ve farkındalık çalışmalarında kaygı ve streste azalma görüldü; çalışmalar küçük. | sınırlı | Nidhi ve ark. 2012, Int J Yoga; Stefanaki ve ark. 2015, Stress |

| Anahtar | Adım (≤ 80) |
|---|---|
| `movement_routine_stretch_step_1` | Rahat otur; 4’e kadar sayarak burnundan al, 6’ya kadar sayarak ver. |
| `movement_routine_stretch_step_2` | Omuzlarını kulaklarına doğru kaldır, bırak; başını yavaşça iki yana eğ. |
| `movement_routine_stretch_step_3` | Dört ayak üstünde sırtını yavaşça yuvarla, sonra çukurlaştır. |
| `movement_routine_stretch_step_4` | Dizlerini aç, kalçanı topuklarına indir, kollarını öne uzat; nefes al. |
| `movement_routine_stretch_step_5` | Sırtüstü uzan, dizlerini göğsüne çek, iki yana hafifçe sallan. |
| `movement_routine_stretch_step_6` | Son 2 dakika gözlerin kapalı, yalnızca nefesini izle. |

Not: Akşam rutini (Su ve uyku sekmesi) bu rutine bağlanabilir; bildirim metni değişmez.

#### Rutin 4: Oturma molası (3 dk)

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `movement_routine_sit_break` | Oturma molası | Uzun oturmayı kısa yürüyüşlerle bölmek tokluk kan şekeri yükselişini azaltabilir. Kılavuz da oturarak geçen süreyi azaltmayı öneriyor. | orta | Buffey ve ark. 2022, Sports Med; Teede ve ark. 2023, Hum Reprod |

| Anahtar | Adım (≤ 80) |
|---|---|
| `movement_routine_sit_break_step_1` | Uzun süre oturuyorsan yarım saatte ya da saatte bir kalk. |
| `movement_routine_sit_break_step_2` | 2–3 dakika yürü: su doldur, merdiven in-çık ya da odanı dolaş. |
| `movement_routine_sit_break_step_3` | Ayakta omuzlarını çevir, parmak ucunda yükselip in. |
| `movement_routine_sit_break_step_4` | Telefonla konuşurken ayakta durmak ya da yürümek de sayılır. |

### B3. Güvenlik notu

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `movement_safety` | Güvenle hareket | Göğüs ağrısı, nefes darlığı, baş dönmesi ya da keskin bir ağrı olursa dur. Gebeysen ya da bir sağlık durumun varsa sana uygun hareketleri doktoruna sor. | uzman görüşü | Riebe ve ark. 2015, Med Sci Sports Exerc (ACSM); ACOG 2020, Komite Görüşü No. 804 |

Yerleşim: rutin listesinin hemen üstünde, her zaman görünür.

### B4. Döngüye göre öneri

**Değerlendirme (ekip için, kullanıcıya görünmez).** Evre bazlı (foliküler / luteal) hareket önerisi bilimsel
olarak savunulamaz:

1. Düzenli döngülü kadınlarda bile evrenin egzersiz performansına etkisi önemsiz düzeyde ve kanıt kalitesi düşük;
   yazarlar genel öneri yerine bireysel yaklaşımı öneriyor (McNulty 2020). Kuvvet performansında ve antrenman
   uyumunda evre etkisi gösterilmedi (Colenso-Semple 2023); evreye göre planlanan kuvvet antrenmanı çalışmaları az
   ve tutarsız (Kissow 2022).
2. PCOS’ta döngü düzensiz, yumurtlama öngörülemez; kılavuz düzenli döngüde bile ovulatuvar disfonksiyon
   olabileceğini belirtir (Teede 2023). Ayla hormonla doğrulama yapmaz; takvimden evre tahmini yanıltır. Hap
   kullanırken döngü tahmini zaten yapılmaz.
3. Savunulabilir tek ayrım kullanıcının **kaydettiği** kanama günleridir: düzenli egzersizin adet ağrısını
   azaltabileceğine dair düşük kaliteli kanıt var (Armour 2019, Cochrane). Bu yüzden yalnızca "adet günleri" ve
   "diğer günler" ayrımı yapılır, evre adı kullanılmaz, kanıt "sınırlı" etiketlenir.

**Görünürlük kuralı.**
- `movement_cycle_period` ya da `movement_cycle_other` (isteğe bağlı açıklama `movement_cycle_why` ile) yalnızca
  kişiselleştirme rızası açık **ve** hap modu kapalıyken görünür.
- "Adet günü": bugün kayıtlı bir adet aralığının içinde olmak ya da bugünkü kanama kaydının "Hafif" ve üstü olması.
  Tahmini aralık kullanılmaz; tahmin gizli ya da yokken de çalışır.
- Rıza kapalı ya da hap modu açık: yalnızca `movement_cycle_general` görünür ve kanama kaydı okunmaz. Hap modunda
  kanama çekilme kanamasıdır; genel metin bu yüzden "adet" demez.

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `movement_cycle_period` | Adet günlerinde | Bugün kanama kaydın var. İstersen yürüyüş ya da esneme gibi hafif hareket, istersen dinlenme; ikisi de olur. Düzenli hareket adet ağrısını hafifletebilir. | sınırlı | Armour ve ark. 2019, Cochrane |
| `movement_cycle_other` | Bugün için | Hangi rutin bugüne uyuyorsa onu seç. Döngü evresine göre öneri yapmıyoruz; PCOS’ta yumurtlama düzensiz olabildiği için evre tahmini güvenilir değil. | sınırlı | McNulty ve ark. 2020, Sports Med; Teede ve ark. 2023, Hum Reprod |
| `movement_cycle_why` | Neden evreye göre değil? | Döngü evresine göre antrenman popüler ama çalışmalar net bir fark göstermiyor. PCOS’ta yumurtlama öngörülemez olabildiği için takvimden evre tahmin etmiyoruz. | sınırlı | McNulty ve ark. 2020, Sports Med; Colenso-Semple ve ark. 2023, Front Sports Act Living |
| `movement_cycle_general` | Kendini dinle | Hareket için en iyi gün, kendini hazır hissettiğin gün. Kanama, kramp ya da yorgunluk olan günlerde hafif hareket de dinlenmek de olur. | sınırlı | Armour ve ark. 2019, Cochrane |

## C) Bitkisel destekler (#13)

### C0. Giriş ve ortak alanlar

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `herbal_intro` | Bitkisel destekler | Burada yalnızca PCOS’ta denenmiş ve kanıt düzeyi belli bitkiler var. Tedavi yerine geçmezler; miktar ya da süre önermiyoruz. | — | — |

| Anahtar | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|
| `herbal_tell_professional` | Kullanıyorsan doktoruna ya da eczacına söyle. | uzman görüşü | Teede ve ark. 2023, Hum Reprod (uygulama notu) |
| `herbal_field_tried` | Ne denendi? | — | — |
| `herbal_field_allergen` | Alerji | — | — |
| `herbal_field_caution` | Dikkat | — | — |
| `herbal_tea_iron_note` | Bitki çayları yemekle birlikte içildiğinde demir emilimini azaltabilir. Demir eksikliğin varsa çayı öğün aralarında içmek daha uygun olabilir. | orta | Hurrell ve ark. 1999, Br J Nutr |

Kart kuralı: her bitkisel kartta "Ne denendi?", kanıt etiketi, "Alerji", "Dikkat" ve `herbal_tell_professional`
birlikte görünür; biri eksikse kart gösterilmez (CLAUDE.md). "İlaçlarıma ekle" düğmesi olacaksa şablon miktar ve
saat önermemeli (F bölümü, madde 6).

### C1. Yeşil nane (spearmint) çayı

| Anahtar | Alan | Metin (≤ 180) |
|---|---|---|
| `herbal_spearmint_title` | Başlık | Yeşil nane (spearmint) çayı |
| `herbal_spearmint_tried` | Ne denendi? | Küçük bir çalışmada yeşil nane çayı içen PCOS’u olan kadınlarda testosteron düştü. Kadınlar tüylenmede azalma bildirdi; ölçümle bakılan tüylenme değişmedi. |
| `herbal_spearmint_note` | Not | Çalışmalarda yeşil nane (Mentha spicata) kullanıldı; tıbbi nane (peppermint) farklı bir bitkidir. |
| `herbal_spearmint_allergen` | Alerji | Nane ya da aynı aileden bitkilere (kekik, fesleğen, adaçayı gibi) alerjin varsa uygun olmayabilir. |
| `herbal_spearmint_caution` | Dikkat | Mide yanması ya da reflüyü artırabilir. Gebeysen, gebelik planlıyorsan ya da emziriyorsan düzenli içmeden önce doktoruna danış. |

- **Kanıt:** sınırlı
- **Kaynak (`herbal_spearmint_source`):** Grant 2010, Phytother Res; Akdoğan ve ark. 2007, Phytother Res
- **Not dayanakları (ekip için):** alerji notu uzman görüşü (aynı bitki ailesi); reflü notu NIDDK (nane reflüyü
  artırabilecek yiyecekler arasında); gebelik notu ihtiyat ilkesi (insan verisi yok, antiandrojen etki).

### C2. Tarçın

| Anahtar | Alan | Metin (≤ 180) |
|---|---|---|
| `herbal_cinnamon_title` | Başlık | Tarçın |
| `herbal_cinnamon_tried` | Ne denendi? | Birkaç küçük çalışmada PCOS’u olan kadınlara kapsülle tarçın verildi; bazılarında insülin direnci göstergeleri iyileşti, birinde daha sık adet görüldü. Sonuçlar kesin değil. |
| `herbal_cinnamon_allergen` | Alerji | Tarçına alerjin varsa uygun değil. Ağızda yanma, kaşıntı ya da ciltte döküntü olursa bırak ve doktoruna danış. |
| `herbal_cinnamon_caution` | Dikkat | Yaygın satılan kasya (Çin) tarçını kumarin içerir; uzun süre yüksek miktarda alınması karaciğeri zorlayabilir. |
| `herbal_cinnamon_caution_2` | Dikkat (devam) | Karaciğer hastalığın varsa, gebeysen ya da kan şekerini düşüren ilaç kullanıyorsan önce doktoruna danış. |

- **Kanıt:** sınırlı
- **Kaynak (`herbal_cinnamon_source`):** Heydarpour ve ark. 2020, J Ethnopharmacol; Kort ve Lobo 2014, Am J Obstet
  Gynecol; BfR 2012
- **Neden listede:** bitkisel adaylar arasında PCOS'ta en çok RCT'si olan (küçük, kısa, heterojen) ve meta-analizi
  bulunan; mutfakta yaygın. Kumarin riski doza bağlı ve öngörülebilir olduğu için uyarıyla gösterilir (kurkumindeki
  öngörülemeyen karaciğer sinyalinden farkı bu).

### C3. Listeye alınmayanlar

Biçim: uyarı bandı (`UyariBandi`), öneri kartı değil; "ekle" düğmesi yok. Kanıt etiketi gösterilir ama uyarı kanıt
düzeyinden bağımsız olarak her zaman görünür (`evidence_safety_note`).

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `herbal_excluded` | Listeye almadıklarımız | Bunlar PCOS için sıkça anılsa da etkileşim ya da güvenlik nedeniyle listemizde yok. Kullanıyorsan doktoruna ya da eczacına söyle. | — | — |
| `herbal_excluded_st_johns_wort` | Sarı kantaron | Ağızdan alınan sarı kantaron (çay, kapsül, damla) doğum kontrol hapı, bant, halka ve implantın etkisini azaltabilir; istenmeyen gebelik riski doğar. | güçlü | MHRA 2014, Drug Safety Update; FSRH 2022 |
| `herbal_excluded_licorice` | Meyan kökü | Meyan kökü (şerbet, çay, takviye) bol ve uzun süre alınınca tansiyonu yükseltip potasyumu düşürebilir. Tansiyon, kalp, böbrek sorununda ve gebelikte uygun değil. | güçlü | NCCIH, Licorice Root; EMA/HMPC, Liquiritiae radix monografı |
| `herbal_excluded_berberine` | Berberin | Berberin birçok ilacın vücuttaki düzeyini değiştirebilir, kan şekeri ilaçlarıyla birlikte şekeri fazla düşürebilir. Gebelikte ve emzirirken güvenli kabul edilmez. | orta | MedlinePlus (ABD Ulusal Tıp Kütüphanesi), Berberine |
| `herbal_excluded_vitex` | Hayıt (Vitex, keşiş biberi) | Hayıt hormonları ve dopamini etkileyebilir. Hormonal doğum kontrol ya da hormon tedavisi kullanıyorsan, gebeysen ya da emziriyorsan önce doktoruna danış. | sınırlı | NCCIH, Chasteberry |
| `herbal_excluded_turmeric` | Zerdeçal ve kurkumin takviyeleri | Zerdeçal ya da kurkumin içeren takviyelerle ilişkili karaciğer hasarı bildirimleri var. Karaciğer sorunun varsa ya da düzenli ilaç kullanıyorsan önce doktoruna danış. | sınırlı | TGA 2023, güvenlik uyarısı |

Gerekçeler (ekip için):
- **Sarı kantaron:** zorunlu uyarı (CLAUDE.md, MHRA). Türkiye'de yaygın olan sarı kantaron yağı cilde sürülür;
  uyarı ağızdan alınan ürünlerle sınırlı yazıldı, cilt kullanımı için güvenli ya da güvensiz iddiası yok.
- **Meyan kökü:** PCOS için "antiandrojen" diye anılır; meyan şerbeti Türkiye'de geleneksel içecek. Glisirizin
  kaynaklı tansiyon yükselmesi ve potasyum düşüklüğü iyi tanımlı.
- **Berberin:** "doğal metformin" diye pazarlanıyor; PCOS'ta küçük RCT'ler var ama ilaç etkileşimleri ve gebelikte
  güvenlik endişesi nedeniyle yalnızca uyarı.
- **Hayıt:** "hormon dengeleyici" diye anılır; PCOS kanıtı zayıf, hormonal ilaçlarla ve gebelikte dikkat.
- **Zerdeçal / kurkumin:** PCOS'ta küçük RCT'ler ve iki meta-analiz metabolik göstergelerde iyileşme bildiriyor;
  takviyelerle öngörülemeyen karaciğer hasarı bildirimleri nedeniyle öneri listesine alınmadı.

### C4. Değerlendirilip eklenmeyenler (ekip için, kullanıcıya görünmez)

- **Yeşil çay ve ekstresi:** sistematik derlemede sonuçlar tutarsız, sonlanımların çoğu kilo odaklı; uygulamanın
  dil ilkesiyle de uyumsuz (Maleki 2021).
- **Mercanköşk çayı:** tek küçük pilot çalışma (bitki çayları sistematik derlemesi, J Midwifery Reprod Health).
- **Öğün zamanlaması (kahvaltı ağırlıklı dağılım):** Jakubowicz 2013, küçük RCT; öneri kalori dağılımına dayandığı
  için kullanılmadı.

## D) Kanıt etiketleri

Kodda tek bir enum önerilir: `EvidenceLevel { STRONG, MODERATE, LIMITED, EXPERIMENTAL, EXPERT_OPINION }`. Çip
`_label`, çipe dokununca açılan açıklama `_text`. Mevcut `meds_evidence_limited` ("Sınırlı kanıt") ile
`evidence_limited_label` aynı metin; birleştirilmesi önerilir.

| Anahtar | Etiket (≤ 40) | Açıklama (≤ 180) |
|---|---|---|
| `evidence_strong` | Güçlü kanıt | Uluslararası kılavuzlar ya da çok sayıda çalışmayı birleştiren analizler aynı yönü gösteriyor. |
| `evidence_moderate` | Orta kanıt | İyi tasarlanmış çalışmalar var ama sayıları az ya da çoğu PCOS’a özgü değil; yeni çalışmalarla değişebilir. |
| `evidence_limited` | Sınırlı kanıt | Küçük ya da az sayıda çalışma var. Sonuç umut verici olabilir ama kesin değil. |
| `evidence_experimental` | Deneysel | Uluslararası kılavuz bu yöntemi henüz deneysel sayıyor; etkisi araştırılıyor. |
| `evidence_expert` | Uzman görüşü | Doğrudan çalışma yerine uzmanların ortak görüşüne ve sağlık kurumlarının önerilerine dayanır. |

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) |
|---|---|---|
| `evidence_info` | Kanıt düzeyi ne demek? | Kanıt düzeyi, bir bilginin ne kadar ve nasıl araştırıldığını gösterir; sana iyi gelip gelmeyeceğini değil. Her kartta kaynağı da görebilirsin. |

| Anahtar | Metin (≤ 180) |
|---|---|
| `evidence_safety_note` | Güvenlik uyarılarında ihtiyatlıyız: kanıt sınırlı olsa bile olası riski bilmeni isteriz. |

Etiketleme kuralı (kanıt özetiyle aynı): PCOS kılavuzunun benimsediği öneri "güçlü"; kılavuzun benimsemediği genel
nüfus meta-analizi PCOS'a uyarlanırken bir basamak düşer ("orta"); küçük RCT ve gözlemsel çalışmalar "sınırlı";
uzlaşı bildirgesi, kurum önerisi ve eğitim aracı "uzman görüşü".

## E) Kaynakça

Erişim notu: 12 Ekim 2026 oturumunda birincil kaynak sayfaları (monash.edu, ncbi.nlm.nih.gov, gov.uk,
ema.europa.eu, nccih.nih.gov) indirilemedi (kuruluş çıkış politikası ve WebFetch hız sınırı). Doğrulama, web
aramasındaki başlık, dergi ve URL (PMID/DOI) eşleşmesiyle yapıldı. Durum etiketleri:
**doğrulandı** (başlık + dergi/kurum + yıl), **kısmen** (varlık doğrulandı; yazar, cilt/sayfa ya da içerik
ayrıntısı bellekten), **doğrulanamadı**. Kılavuz önerilerinin Türkçe özetleri yayımlanmış metne dayanır; uygulamaya
almadan önce Monash özet PDF'iyle karşılaştırılmalıdır.

**Kılavuz ve genel öneriler**
1. Teede HJ, Tay CT, Laven JJE ve ark. Recommendations from the 2023 International Evidence-based Guideline for the
   Assessment and Management of Polycystic Ovary Syndrome. Fertil Steril. 2023;120(4):767–793 · Hum Reprod.
   2023;38(9):1655–1679 · J Clin Endocrinol Metab. 2023;108(10):2447–2469 (doi:10.1210/clinem/dgad463) · Eur J
   Endocrinol. 2023;189(2):G43–G64. — kısmen (başlık ve eşzamanlı yayın doğrulandı; cilt/sayfa ve öneri metinleri
   bellekten)
2. Bull FC, Al-Ansari SS, Biddle S ve ark. World Health Organization 2020 guidelines on physical activity and
   sedentary behaviour. Br J Sports Med. 2020;54(24):1451–1462. — doğrulandı ("every move counts" çerçevesi dâhil)
3. Exercise and Sports Science Australia. Exercise in the management of polycystic ovary syndrome: A position
   statement from Exercise and Sports Science Australia. J Sci Med Sport. 2024;27:668–677. — doğrulandı (yazarlar
   görülemedi)
4. World Health Organization. Guideline: Sugars intake for adults and children. Cenevre: WHO; 2015. — doğrulandı
5. T.C. Sağlık Bakanlığı. Türkiye Beslenme Rehberi (TÜBER) 2022. Ankara; 2022. — doğrulandı (içerik okunmadı;
   yalnızca genel nüfus rehberi olarak anılıyor)
6. Harvard T.H. Chan School of Public Health, The Nutrition Source. Healthy Eating Plate (2011). — doğrulandı
7. American Diabetes Association. Diabetes Plate Method. diabetes.org. — kısmen (üniversite yayım sayfalarıyla)
8. CDC. Measuring Physical Activity Intensity (konuşma testi). cdc.gov. — doğrulandı
9. Riebe D, Franklin BA, Thompson PD ve ark. Updating ACSM's Recommendations for Exercise Preparticipation Health
   Screening. Med Sci Sports Exerc. 2015;47(11):2473–2479. — kısmen (başlık doğrulandı)
10. ACOG Committee Opinion No. 804: Physical Activity and Exercise During Pregnancy and the Postpartum Period. Obstet
    Gynecol. 2020;135(4):e178–e188. — kısmen

**Beslenme**
11. St-Onge MP, Ard J, Baskin ML ve ark. Meal Timing and Frequency: Implications for Cardiovascular Disease
    Prevention: A Scientific Statement From the American Heart Association. Circulation. 2017;135(9):e96–e121.
    — kısmen
12. Reynolds A, Mann J, Cummings J, Winter N, Mete E, Te Morenga L. Carbohydrate quality and human health: a series
    of systematic reviews and meta-analyses. Lancet. 2019;393(10170):434–445. — doğrulandı
13. Reynolds AN, Akerman AP, Mann J. Dietary fibre and whole grains in diabetes management: Systematic review and
    meta-analyses. PLoS Med. 2020;17(3):e1003053. — doğrulandı
14. Dhillon J, Craig BA, Leidy HJ ve ark. The Effects of Increased Protein Intake on Fullness: A Meta-Analysis and
    Its Limitations. J Acad Nutr Diet. 2016;116(6):968–983. — kısmen (başlık ve dergi haber kaynaklarından)
15. Kazemi M, Hadi A, Pierson RA, Lujan ME, Zello GA, Chilibeck PD. Effects of Dietary Glycemic Index and Glycemic
    Load on Cardiometabolic and Reproductive Profiles in Women with Polycystic Ovary Syndrome: A Systematic Review
    and Meta-analysis of Randomized Controlled Trials. Adv Nutr. 2021;12(1):161–178. — doğrulandı (yazarlar
    bellekten)
16. Atkinson FS, Brand-Miller JC, Foster-Powell K, Buyken AE, Goletzke J. International tables of glycemic index
    and glycemic load values 2021: a systematic review. Am J Clin Nutr. 2021;114(5):1625–1632. — kısmen
17. Imamura F, O'Connor L, Ye Z ve ark. Consumption of sugar sweetened beverages, artificially sweetened beverages,
    and fruit juice and incidence of type 2 diabetes: systematic review, meta-analysis, and estimation of
    population attributable fraction. BMJ. 2015;351:h3576. — doğrulandı
18. Lane MM, Gamage E, Du S ve ark. Ultra-processed food exposure and adverse health outcomes: umbrella review of
    epidemiological meta-analyses. BMJ. 2024;384:e077310. — doğrulandı
19. Increased prevalence of binge eating disorder and bulimia nervosa … (sistematik derleme ve meta-analiz). J Clin
    Endocrinol Metab. 2024;109(12):3293 (PMID 39115340). — kısmen (dergi, cilt, ilk sayfa doğrulandı; ilk yazar
    görülemediği için yazarsız verildi)
20. Jakubowicz D, Barnea M, Wainstein J, Froy O. Effects of caloric intake timing on insulin resistance and
    hyperandrogenism in lean women with polycystic ovary syndrome. Clin Sci (Lond). 2013;125(9):423–432. —
    doğrulandı (yalnızca "eklenmeyen" gerekçesinde)

**Hareket**
21. Buffey AJ, Herring MP, Langley CK, Donnelly AE, Carson BP. The Acute Effects of Interrupting Prolonged Sitting
    Time in Adults with Standing and Light-Intensity Walking on Biomarkers of Cardiometabolic Health in Adults: A
    Systematic Review and Meta-analysis. Sports Med. 2022;52(8):1765–1787. — doğrulandı
22. Reynolds AN, Mann JI, Williams S, Venn BJ. Advice to walk after meals is more effective for lowering
    postprandial glycaemia in type 2 diabetes mellitus than advice that does not specify timing: a randomised
    crossover study. Diabetologia. 2016;59(12):2572–2578. — doğrulandı (yazarlar ve cilt bellekten)
23. DiPietro L, Gribok A, Stevens MS, Hamm LF, Rumpler W. Three 15-min bouts of moderate postmeal walking
    significantly improves 24-h glycemic control in older people at risk for impaired glucose tolerance. Diabetes
    Care. 2013;36(10):3262–3268. — doğrulandı
24. Nidhi R, Padmalatha V, Nagarathna R, Amritanshu R. Effect of holistic yoga program on anxiety symptoms in
    adolescent girls with polycystic ovarian syndrome: A randomized control trial. Int J Yoga. 2012;5(2):112–117.
    — kısmen (aramada kayıt göründü, başlık görülemedi)
25. Stefanaki C, Bacopoulou F, Livadas S ve ark. Impact of a mindfulness stress management program on stress,
    anxiety, depression and quality of life in women with polycystic ovary syndrome: a randomized controlled trial.
    Stress. 2015;18(1):57–66. — kısmen (PMID aramada göründü)
26. Armour M, Ee CC, Naidoo D ve ark. Exercise for dysmenorrhoea. Cochrane Database Syst Rev. 2019;9:CD004142.
    — doğrulandı
27. McNulty KL, Elliott-Sale KJ, Dolan E ve ark. The Effects of Menstrual Cycle Phase on Exercise Performance in
    Eumenorrheic Women: A Systematic Review and Meta-Analysis. Sports Med. 2020;50(10):1813–1827. — doğrulandı
28. Colenso-Semple LM, D'Souza AC, Elliott-Sale KJ, Phillips SM. Current evidence shows no influence of women's
    menstrual cycle phase on acute strength performance or adaptations to resistance exercise training. Front Sports
    Act Living. 2023;5:1054542. — doğrulandı
29. Kissow J ve ark. Effects of Follicular and Luteal Phase-Based Menstrual Cycle Resistance Training on Muscle
    Strength and Mass. Sports Med. 2022. — kısmen (cilt/sayfa verilmedi)

**Bitkisel destekler ve uyarılar**
30. Grant P. Spearmint herbal tea has significant anti-androgen effects in polycystic ovarian syndrome. A randomized
    controlled trial. Phytother Res. 2010;24(2):186–188. — doğrulandı
31. Akdoğan M, Tamer MN, Cüre E, Cüre MC, Köroğlu BK, Delibaş N. Effect of spearmint (Mentha spicata Labiatae) teas
    on androgen levels in women with hirsutism. Phytother Res. 2007;21(5):444–447. — doğrulandı
32. Heydarpour F, Hemati N, Hadi A, Moradi S, Mohammadi E, Farzaei MH. Effects of cinnamon on controlling metabolic
    parameters of polycystic ovary syndrome: A systematic review and meta-analysis. J Ethnopharmacol.
    2020;254:112741. — kısmen (PMID aramada göründü)
33. Kort DH, Lobo RA. Preliminary evidence that cinnamon improves menstrual cyclicity in women with polycystic ovary
    syndrome: a randomized controlled trial. Am J Obstet Gynecol. 2014;211(5):487.e1–6 (NCT01483118). — doğrulandı
34. The effect of cinnamon supplementation on glycemic control in patients with type 2 diabetes or with polycystic
    ovary syndrome: an umbrella meta-analysis on interventional meta-analyses. Diabetol Metab Syndr. 2023
    (doi:10.1186/s13098-023-01057-2). — kısmen (yazarlar görülemedi)
35. BfR (Bundesinstitut für Risikobewertung). New insights into coumarin contained in cinnamon (2012); Cassia
    cinnamon with high coumarin contents to be consumed in moderation; FAQ on coumarin in cinnamon and other foods.
    — kısmen (belgeler doğrulandı; görüş tarihi bellekten)
36. Hurrell RF, Reddy M, Cook JD. Inhibition of non-haem iron absorption in man by polyphenolic-containing beverages.
    Br J Nutr. 1999;81(4):289–295. — kısmen (başlık doğrulandı; nane çayının test edilen içecekler arasında olduğu
    bellekten)
37. NIDDK. Eating, Diet, & Nutrition for GER & GERD. niddk.nih.gov. — kısmen (sayfa doğrulandı; içerik bellekten)
38. MHRA. St John's wort: interaction with hormonal contraceptives, including implants. Drug Safety Update, Mart
    2014. gov.uk. — doğrulandı
39. FSRH Clinical Effectiveness Unit. Drug Interactions with Hormonal Contraception (Mayıs 2022 güncellemesi).
    — kısmen (yalnızca NHS kopyasının varlığı görüldü)
40. NCCIH. Licorice Root. nccih.nih.gov/health/licoriceroot. — kısmen (sayfa doğrulandı; içerik bellekten)
41. EMA/HMPC. European Union herbal monograph on Glycyrrhiza glabra L. and/or Glycyrrhiza inflata Bat. and/or
    Glycyrrhiza uralensis Fisch., radix (Revision 1). — kısmen (belge doğrulandı; yıl görülemedi)
42. MedlinePlus (U.S. National Library of Medicine). Berberine. medlineplus.gov/druginfo/natural/1126.html. —
    kısmen (sayfa doğrulandı; içerik bellekten)
43. NCCIH. Chasteberry. nccih.nih.gov/health/chasteberry. — kısmen (sayfa doğrulandı; içerik bellekten)
44. TGA (Therapeutic Goods Administration, Avustralya). Medicines containing turmeric or curcumin – risk of liver
    injury. Güvenlik uyarısı, 2023. — doğrulandı
45. Maleki V ve ark. A comprehensive insight into effects of green tea extract in polycystic ovary syndrome: a
    systematic review. Reprod Biol Endocrinol. 2021;19:147. — kısmen (yalnızca "eklenmeyen" gerekçesinde)
46. The Effect of Herbal Teas on Management of Polycystic Ovary Syndrome: A Systematic Review. J Midwifery Reprod
    Health (jmrh.mums.ac.ir, makale 9368). — kısmen (yıl ve yazarlar görülemedi; yalnızca "eklenmeyen" gerekçesinde)
47. Kurkumin meta-analizleri (yalnızca uyarı gerekçesinde): Effects of Curcumin on Glycemic Control and Lipid
    Profile in Polycystic Ovary Syndrome: Systematic Review with Meta-Analysis and Trial Sequential Analysis (2021);
    Therapeutic effect and safety of curcumin in women with PCOS: A systematic review and meta-analysis. Front
    Endocrinol. 2022 (doi:10.3389/fendo.2022.1051111). — kısmen (yazarlar görülemedi)

## F) Uygulama notları (ekip için, kullanıcıya görünmez)

1. **Görünürlük.** Döngüye göre kart kuralı B4'te. Kişiselleştirme rızası yalnızca bu kartı etkiler; beslenme
   ilkeleri, rutinler, güvenlik ve destek notları herkese aynıdır (kişiye özel öneri yok).
2. **Hap modu.** Döngüye göre kart gizlenir, `movement_cycle_general` görünür; hiçbir metin evre ya da yumurtlama
   tahmini içermez.
3. **Kaynak ve kanıt gösterimi.** Her kartın altında `plan_source_format` + `_source`, yanında kanıt çipi. Bitkisel
   ve uyarı kartlarında kaynak satırı zorunlu.
4. **Ekran ve test.** Yeni durumlar `EkranGoruntuleriTest`'e ve %200 yazı (`y0N`) serisine eklenir: Beslenme,
   Hareket (rıza açık + adet günü, rıza açık + diğer gün, rıza kapalı, hap modu), Bitkisel liste ve uyarılar.
   Satırlarda metin sütunu `Modifier.weight(1f)` alır; rutin adımları büyük yazıda dikey kalır.
5. **Eski metinler.** Sekmeler dolunca `plan_coming_soon`, `plan_nutrition_preview`, `plan_movement_preview`
   kaldırılır (lint UnusedResources). `plan_nutrition_preview` "kalori" sözcüğünü olumsuz cümlede de içeriyor;
   yeni metinlerde bu sözcük hiç yok.
6. **Nane çayı şablonu (#13).** `MedicationRepository.SPEARMINT_TEA` adı "Nane çayı", doz "1 fincan" ve 10:00 /
   16:00 saatleriyle ön dolu. Bu, Grant 2010 protokolünü (günde iki fincan) andırır ve wellness sınırında kür
   önerisi gibi okunabilir. Öneri: ad "Yeşil nane çayı", doz ve saatleri kullanıcı seçsin. Kod değişikliğidir, bu
   belgede yapılmadı.
7. **Dil denetimi testi.** `nutrition_`, `movement_`, `herbal_`, `evidence_` önekli metinlerde yasaklı sözcükleri
   (Kullanım kuralları) tarayan saf JVM testi önerilir; "diyetisyen" istisna.
8. **`settings_about_text`.** Kullanıcıyı "docs klasörüne" yönlendiriyor; kullanıcı bu klasöre erişemez. Uygulama
   içi bir "Kaynaklar" listesi MVP 2'de bu belgenin E bölümünden üretilebilir (ayrı iş).
