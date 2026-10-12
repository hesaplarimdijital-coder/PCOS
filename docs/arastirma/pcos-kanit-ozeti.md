# PCOS / PMOS kanıt özeti (Ekim 2026)

Uygulamadaki her sağlık cümlesinin dayanağı. Kanıt düzeyi: **güçlü** (kılavuz önerisi/meta-analiz), **orta** (RCT
ya da PCOS kılavuzunun benimsemediği genel nüfus meta-analizi), **sınırlı** (küçük RCT/gözlem), **deneysel**
(kılavuz böyle sınıflıyor), **uzman görüşü** (uzlaşı bildirgesi, kurum önerisi ya da eğitim aracı; doğrudan çalışma
yok). Kural: PCOS kılavuzunun benimsemediği genel nüfus kanıtı PCOS'a uyarlanırken bir basamak düşürülür. Güvenlik
uyarıları kanıt düzeyinden bağımsız gösterilir (ihtiyat ilkesi). MVP 2 metinleri ve anahtarları:
[plan-icerigi.md](plan-icerigi.md).

| Konu | Uygulamadaki kullanım | Dayanak | Düzey |
|---|---|---|---|
| Adlandırma | "PCOS (yeni adıyla PMOS)" | The Lancet, 12 Mayıs 2026 — uluslararası uzlaşı, polikistik over sendromu → polikistik metabolik over sendromu (PMOS); Monash/H. Teede öncülüğünde | güçlü |
| Yaşam tarzı ilk basamak | Planım sekmesi, hareket metinleri (`nutrition_intro`, `movement_intro`, `movement_weekly`) | 2023 Uluslararası PCOS Kılavuzu (Teede ve ark., Monash) — haftada ≥150 dk orta ya da 75 dk yoğun + 2 gün kuvvet; hareketsiz süreyi azaltma; kilo yönetimi için 250/150 dk (uygulamada gösterilmez). Genel nüfus dayanağı: DSÖ 2020 (Bull ve ark., Br J Sports Med) | güçlü |
| Yeme bozukluğu riski | Kalori sayma yok, kilo/bel varsayılan gizli, yargılamayan dil; Beslenme sekmesi destek notu (`nutrition_support`) | 2023 kılavuzu: PCOS'ta yeme bozukluğu ve beden algısı sorunları taranmalı. J Clin Endocrinol Metab 2024;109(12):3293 sistematik derleme ve meta-analiz: tıkınırcasına yeme bozukluğu ve bulimiya yaygınlığı PCOS'ta daha yüksek, beden ağırlığından bağımsız | güçlü |
| Inositol | "Deneysel" etiketi, doz değişikliği önerilmez | 2023 kılavuzu inositolü deneysel olarak sınıflar; metformine göre kanıt düşük | deneysel |
| Uzun döngü eşikleri | 60. gün nazik, 90. gün doktora danış | NHS: PCOS'ta düzensiz döngü; 12 haftadır kanama yoksa gebelik testi ve hekim; yılda ≥4 adet (ya da hormonal koruma) rahim iç tabakası için | güçlü (NHS rehberi) |
| Nane çayı | Bitkisel destekler (`herbal_spearmint_*`), "sınırlı kanıt" etiketi, alerjen ve reflü/gebelik notu; miktar ve süre kullanıcıya verilmez | Grant P. (2010) Phytother Res — RCT, n=42, 30 gün, günde iki fincan yeşil nane (Mentha spicata) çayı ile plasebo bitki çayı: serbest/total testosteron düştü, LH/FSH yükseldi; öz-değerlendirmede tüylenme azaldı, nesnel Ferriman-Gallwey puanı değişmedi; küçük örneklem. Akdoğan ve ark. (2007) Phytother Res — randomize olmayan, n=21, 5 gün: serbest testosteron düştü. Tıbbi nane (peppermint) çalışılmadı | sınırlı |
| Yemek sonrası yürüyüş | Beslenme ipucu (`nutrition_walk_tip`, `notif_walk`), rutinler (`movement_routine_walk*`, `movement_routine_sit_break*`) | Buffey ve ark. (2022) Sports Medicine meta-analiz — uzun oturmayı hafif yürüyüşle bölmek tokluk glukozunu ve insülinini düşürür (akut etki, genel yetişkin). Reynolds ve ark. (2016) Diabetologia — tip 2 diyabette her ana öğünden sonra 10 dk yürüyüş, zamanı belirtilmemiş aynı süreden etkili (çapraz RCT). DiPietro ve ark. (2013) Diabetes Care — öğün sonrası 3 × 15 dk. PCOS'a özgü veri yok; tek etiket gerektiği için Ekim 2026'da "orta–güçlü"den "orta"ya çekildi | orta |
| Sarı kantaron × hormonal doğum kontrol | Bitkisel bölümde "Listeye almadıklarımız" uyarısı (`herbal_excluded_st_johns_wort`), öneri listesine girmez | MHRA Drug Safety Update (Mart 2014): ağızdan alınan sarı kantaron hap, bant, halka ve implant dâhil hormonal kontraseptiflerin etkisini düşürür; FSRH ilaç etkileşimleri rehberi (2022) | güçlü |
| Hap kullanırken döngü | Tahmin kapalı; "çekilme kanaması adet değildir" notu; döngüye göre hareket kartı gösterilmez (`movement_cycle_general`) | Standart kontrasepsiyon bilgisi (NHS) | güçlü |
| BBT ile yumurtlama | MVP 3; 3-üstü-6 kuralı | Klasik semptotermal yöntem; PCOS'ta anovulasyon nedeniyle sınırlı | sınırlı |

## MVP 2: Beslenme (#12)

| Konu | Uygulamadaki kullanım | Dayanak | Düzey |
|---|---|---|---|
| Esnek, kişiye uygun düzen | `nutrition_flexible`, `nutrition_intro` | 2023 kılavuzu: hiçbir diyet bileşimi diğerinden üstün değil; genel nüfus kılavuzlarına uygun, sürdürülebilir ve kişinin tercihlerine göre beslenme; aşırı kısıtlayıcı ve dengesiz diyetlerden kaçınılır. Kılavuz önerisi; altta yatan çalışmaların kesinliği düşük | güçlü |
| Düzenli öğün ritmi | `nutrition_rhythm` | St-Onge ve ark. (2017) Circulation, AHA bilimsel bildirgesi: öğün zamanı ve sıklığı kardiyometabolik sağlıkla ilişkili; planlı, düzenli öğün yaklaşımı önerilir. PCOS'a özgü doğrudan kanıt yok; esnek kurgu ("bir öğün kaçarsa sorun değil") yeme bozukluğu riskine göre seçildi | uzman görüşü |
| Lif ve protein | `nutrition_fiber_protein` | Reynolds ve ark. (2019) Lancet — lif ve tam tahıl, kohort ve RCT meta-analizleri; Reynolds ve ark. (2020) PLoS Med — diyabette lif glisemik kontrolü iyileştirir; Dhillon ve ark. (2016) J Acad Nutr Diet — protein tokluk hissini artırır. Genel nüfus ve diyabet kanıtı, PCOS'a uyarlama | orta |
| Düşük glisemik seçimler | `nutrition_slow_carbs` | Kazemi ve ark. (2021) Adv Nutr — PCOS'ta düşük GI/GY RCT'lerinin meta-analizi: insülin direnci, lipid ve bazı androjen göstergelerinde iyileşme; çalışmalar küçük ve kısa. 2023 kılavuzu hiçbir diyeti üstün bulmadı. Örneklerin GI değerleri Atkinson ve ark. (2021) Am J Clin Nutr tablolarından; tam buğday ekmeği bilerek örneklenmedi | sınırlı |
| Şekerli içecek yerine su | `nutrition_water` | DSÖ (2015) şeker kılavuzu — serbest şekerin azaltılması (güçlü öneri); Imamura ve ark. (2015) BMJ — şekerli içecek tüketimi ile tip 2 diyabet ilişkisi (kohort meta-analizi). 2023 PCOS kılavuzu genel nüfus beslenme kılavuzlarına uyumu önerir | güçlü |
| Az işlenmiş yiyecekler | `nutrition_whole_foods` | Lane ve ark. (2024) BMJ şemsiye derleme — ultra işlenmiş gıda ile çok sayıda olumsuz sonuç arasında ilişki (gözlemsel); TÜBER 2022 (T.C. Sağlık Bakanlığı). Kısıtlayıcı dil yok ("her yiyeceğe yer var") | sınırlı |
| Tabak modeli | `nutrition_plate*` | Harvard Healthy Eating Plate (2011), ADA Diabetes Plate Method — eğitim araçları; PCOS'ta test edilmedi. Porsiyon, gram, ölçü yok | uzman görüşü |

## MVP 2: Hareket (#12)

| Konu | Uygulamadaki kullanım | Dayanak | Düzey |
|---|---|---|---|
| Her dakika sayılır | `movement_every_minute`, `movement_weekly` | DSÖ 2020 (Bull ve ark.): biraz hareket hiç yoktan iyidir, her hareket sayılır, süre alt sınırı yok. 2023 kılavuzu: ev işi, ulaşım ve iş dâhil her fiziksel aktivite sayılır | güçlü |
| Tempo tarifi | `movement_intensity` | CDC "Measuring Physical Activity Intensity" — konuşma testi | uzman görüşü |
| Ekipmansız kuvvet rutini | `movement_routine_strength*` | Öneri: 2023 kılavuzu ve DSÖ 2020 — haftada 2 gün kas güçlendirme. Exercise and Sports Science Australia (2024) pozisyon bildirgesi, J Sci Med Sport 27:668–677 — PCOS'ta direnç egzersizini destekler. Hareket seçimi (otur-kalk, duvar şınavı, köprü) ve tekrar aralığı uzman görüşü | güçlü (öneri); adımlar uzman görüşü |
| Esneme ve nefes rutini | `movement_routine_stretch*` | Nidhi ve ark. (2012) Int J Yoga — PCOS'u olan ergenlerde yoga, kaygıda azalma (küçük RCT); Stefanaki ve ark. (2015) Stress — farkındalık temelli stres programı, stres ve kaygıda azalma (küçük RCT) | sınırlı |
| Oturma molası | `movement_routine_sit_break*` | Buffey ve ark. (2022) (bkz. yemek sonrası yürüyüş); 2023 kılavuzu hareketsiz süreyi azaltmayı önerir | orta |
| Güvenlik notu | `movement_safety` | Riebe ve ark. (2015) Med Sci Sports Exerc — ACSM ön tarama (belirti varsa hekim değerlendirmesi); ACOG (2020) Komite Görüşü No. 804 — gebelikte egzersiz hekimle planlanır; DSÖ 2020 | uzman görüşü |
| Döngüye göre hareket | `movement_cycle_*`; yalnızca kişiselleştirme rızası açık ve hap modu kapalıyken; aksi hâlde `movement_cycle_general` | Evre bazlı öneri savunulamaz: McNulty ve ark. (2020) Sports Med — düzenli döngüde evrenin performansa etkisi önemsiz, kanıt kalitesi düşük, bireysel yaklaşım önerilir; Colenso-Semple ve ark. (2023) Front Sports Act Living — kuvvet performansı ve uyumunda evre etkisi yok; Kissow ve ark. (2022) Sports Med — evreye göre kuvvet antrenmanı çalışmaları az ve tutarsız. PCOS'ta düzenli döngüde bile ovulatuvar disfonksiyon olabilir (2023 kılavuzu), takvimden evre tahmini güvenilmez. Yalnızca kayıtlı kanama günü ayrımı: Armour ve ark. (2019) Cochrane — egzersiz adet ağrısını azaltabilir (düşük kalite) | sınırlı |

## MVP 2: Bitkisel destekler ve uyarılar (#13)

| Konu | Uygulamadaki kullanım | Dayanak | Düzey |
|---|---|---|---|
| Tarçın | `herbal_cinnamon_*` | Heydarpour ve ark. (2020) J Ethnopharmacol — PCOS'ta meta-analiz, açlık insülini/HOMA-IR ve lipidlerde iyileşme; Kort ve Lobo (2014) Am J Obstet Gynecol — RCT, 6 ay, adet sıklığı arttı; Diabetol Metab Syndr (2023) şemsiye meta-analizi. Çalışmalar küçük, kapsül formu, heterojen. Güvenlik: kasya tarçınında kumarin, yüksek ve uzun süreli alımda karaciğer (BfR 2012) | sınırlı |
| Bitki çayları ve demir | `herbal_tea_iron_note` | Hurrell ve ark. (1999) Br J Nutr — polifenollü içecekler (siyah çay ve bitki çayları dâhil) hem dışı demir emilimini azaltır | orta |
| Takviyeyi hekime söyleme | `herbal_tell_professional`, `herbal_excluded_text` | 2023 kılavuzu uygulama notu: inositol ve diğer tamamlayıcı tedavileri kullananlar sağlık profesyoneline bildirmeye teşvik edilir | uzman görüşü |
| Nane: ek notlar | `herbal_spearmint_allergen`, `herbal_spearmint_caution` | Nane reflüyü artırabilecek yiyecekler arasında (NIDDK); aynı bitki ailesine alerji ve gebelikte ihtiyat uzman görüşü. Ana dayanak üstteki "Nane çayı" satırı | uzman görüşü |
| Meyan kökü | `herbal_excluded_licorice` | NCCIH (Licorice Root); EMA/HMPC monografı: glisirizin bol ve uzun süre alınınca tansiyon yükselmesi ve potasyum düşüklüğü (psödoaldosteronizm); hipertansiyon, kalp ya da böbrek hastalığı ve gebelikte kaçınılır | güçlü |
| Berberin | `herbal_excluded_berberine` | MedlinePlus (ABD Ulusal Tıp Kütüphanesi) Berberine: karaciğer enzimleri üzerinden ilaç etkileşimleri, diyabet ilaçlarıyla kan şekeri düşmesi; gebelik ve emzirmede muhtemelen güvensiz (yenidoğanda kernikterus bildirimleri). PCOS'ta küçük RCT'ler var; 2023 kılavuzu önermiyor | orta |
| Hayıt (Vitex) | `herbal_excluded_vitex` | NCCIH (Chasteberry): hormon düzeylerini ve dopamin sistemini etkileyebilir; gebelik, emzirme, hormona duyarlı durumlar, dopaminle ilgili ve hormonal ilaçlarla dikkat | sınırlı |
| Zerdeçal / kurkumin takviyeleri | `herbal_excluded_turmeric` | TGA (2023) güvenlik uyarısı: zerdeçal/kurkumin içeren ürünlerle karaciğer hasarı bildirimleri. PCOS'ta küçük RCT'ler ve iki meta-analiz (2021, 2022) metabolik göstergelerde iyileşme bildiriyor; güvenlik sinyali nedeniyle yalnızca uyarı | sınırlı |

## Değerlendirilip eklenmeyenler

| Aday | Gerekçe | Karar |
|---|---|---|
| Yeşil çay ve ekstresi | Maleki ve ark. (2021) Reprod Biol Endocrinol sistematik derleme: sonuçlar tutarsız, sonlanımların çoğu kilo odaklı | eklenmedi |
| Mercanköşk çayı | Tek küçük pilot çalışma (bitki çayları sistematik derlemesi, J Midwifery Reprod Health) | eklenmedi |
| Kahvaltı ağırlıklı öğün zamanlaması | Jakubowicz ve ark. (2013) Clin Sci — küçük RCT; öneri kalori dağılımına dayanıyor | eklenmedi |
| Evreye göre antrenman | "Döngüye göre hareket" satırı | yalnızca kayıtlı kanama günü / diğer gün ayrımı |
| Kurkumin, berberin, meyan kökü, hayıt (öneri olarak) | Güvenlik ve etkileşim; yukarıdaki satırlar | yalnızca uyarı |

Kaynak bağlantıları: NHS PCOS sayfaları (nhs.uk/conditions/polycystic-ovary-syndrome-pcos), Monash 2023 kılavuzu
(monash.edu/medicine/mchri/pcos/guideline), The Lancet editöryal (Mayıs 2026), MHRA St John's wort uyarısı,
Buffey AJ et al. Sports Med 2022;52:1765–87, Grant P. Phytother Res 2010;24:186–8. MVP 2 kaynaklarının tam künyesi
ve doğrulama durumu: [plan-icerigi.md](plan-icerigi.md), E bölümü.

Doğrulama notu (12 Ekim 2026): MVP 2 satırlarının künyeleri web aramasındaki başlık, dergi ve PMID/DOI/URL
eşleşmesiyle doğrulandı. Birincil kaynak sayfaları (monash.edu, ncbi.nlm.nih.gov, gov.uk, nccih.nih.gov) oturumda
indirilemediği için kılavuz önerilerinin ifadesi ve bazı kurum sayfalarının içeriği (NCCIH, MedlinePlus, NIDDK, FSRH)
yeniden okunamadı; plan-icerigi.md E bölümünde "kısmen" olarak işaretli olanlar uygulamaya almadan önce
okunmalıdır.

Not: Bu belge tıbbi tavsiye değildir; klinik içerik ajanı (`.claude/agents/klinik-icerik.md`) her güncellemede
tabloyu yeniler.
