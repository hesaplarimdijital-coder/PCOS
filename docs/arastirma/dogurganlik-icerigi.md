# Doğurganlık içeriği: LH, bazal vücut ısısı ve servikal mukus (MVP 3, #24)

Durum: klinik içerik taslağı, uzman onayı bekliyor · 12 Ekim 2026 · İlgili iş: #24 (US-016) · Motor:
`core/cycle/.../Fertility.kt`, `FertilityEngine.kt`, `OvulationDetector.kt` (ADR-0005, Ekim 2026 güncellemesi) ·
Kanıt etiketleme ve kaynak doğrulama kalıbı: [plan-icerigi.md](plan-icerigi.md) · Dize taslakları: scratchpad
`mvp3/dogurganlik_strings.xml` (depo dışı).

Bu belge tıbbi tavsiye değildir. Motor yalnızca enum ve tarih döndürür; kullanıcıya görünen her metin burada
tanımlanır, `strings.xml`'e `fertility_` önekiyle girer ve kanıt çipi + kaynak taşır.

## 1. Klinik sınır ve ton

- **Yumurtlama yalnızca ısı yükselmesiyle doğrulanır** (3-üstü-6 kuralı, Sensiplan 1. istisna). LH testi ve
  mukus kayıt amaçlıdır; karara girmez (ADR-0005). Doğrulama geriye dönüktür: "bugün yumurtluyorsun" denmez,
  "bu döngüde yumurtlama olmuş görünüyor" denir.
- **Doğum kontrolü için kullanılmaz.** Her ekranda `fertility_not_contraception` görünür. Doğal aile planlaması
  eğitimli danışmanla öğrenilen bir yöntemdir (NHS: doğru kullanımda yüzde 91–99, tipik kullanımda yüzde 76
  etkili; uzman eğitimi gerekir); Ayla bunun yerini tutmaz.
- **Tanı yok.** "Anovulasyon", "luteal faz yetmezliği", "gebe" gibi sonuçlar yazılmaz. Döngü düzensizse
  "yumurtlama düzensiz olabilir, doktorunla konuş"; 18 günü aşan luteal evrede "gebelik testi yapabilirsin,
  negatifse ve adet gelmezse doktorunla konuş".
- **Hap modunda bölüm kapalı** (`FertilityOverview.Disabled`); hiçbir hesap yapılmaz.
- **Ton.** Hamile kalmaya çalışan kullanıcıya dürüst ama umut kırmayan dil: "yumurtlama her ay olmayabilir; bu,
  gebeliğin olmayacağı anlamına gelmez; tedaviler var; bir yıl beklemeden doktora gidilebilir". "Başarısız döngü",
  "kaçırdın", "geç kaldın" yok; ölçüm atlanırsa "kaçarsa sorun değil".
- **Adlandırma.** İlk anışta "PCOS (yeni adıyla PMOS)", sonra "PCOS".

## 2. Ortak metinler

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `fertility_title` | Doğurganlık | — | — | — |
| `fertility_intro` | Yumurtlamayı birlikte okuyalım | LH testi, bazal vücut ısısı ve mukusunu kaydedebilirsin. PCOS (yeni adıyla PMOS) ile yumurtlama düzensiz olabildiği için Ayla onu yalnızca ısı yükselmesiyle doğrulanmış sayar. | güçlü | Teede ve ark. 2023, Hum Reprod; Sensiplan (Frank-Herrmann ve ark. 2007) |
| `fertility_not_contraception` | — | Bu bölüm doğum kontrolü için kullanılmaz. Doğal aile planlaması ancak eğitimli bir danışmanla öğrenilir; Ayla bunun yerini tutmaz. | — (güvenlik) | NHS, Natural family planning |
| `fertility_not_diagnosis` | — | Buradaki notlar tanı ya da tedavi değildir; gözlemlerini doktorunla paylaşmana yardım eder. | — | — |
| `fertility_disabled_pill` | Hap kullanırken kapalı | Doğum kontrol hapı yumurtlamayı baskıladığı için bu bölümde hesap yapılmaz. Hap modunu kapatınca kayıtların yeniden görünür. | güçlü | Standart kontrasepsiyon bilgisi (NHS) |
| `fertility_enable_hint` | — | Bu bölüm "Hamile kalmayı planlıyorum" önceliği açıkken görünür; ayarlardan açabilirsin. | — | — |
| `fertility_tell_doctor` | — | Grafiği ve notları doktor raporuna ekleyebilirsin; yorumunu birlikte yaparsınız. | — | — |

## 3. Bazal vücut ısısı (BBT)

### 3.1 Nasıl ölçülür?

Bölüm başlığı `fertility_bbt_title` "Bazal vücut ısısı (BBT)"; giriş alanları `fertility_bbt_value_label` "Isı (°C)",
`fertility_bbt_value_hint` "36,55", `fertility_bbt_time_label` "Ölçüm saati". Adımlar (`fertility_bbt_step_1..5`,
her biri ≤ 120 karakter), kart başlığı `fertility_bbt_how_title` "Nasıl ölçülür?":

| Anahtar | Metin | Dayanak |
|---|---|---|
| `fertility_bbt_step_1` | Uyanır uyanmaz, yataktan kalkmadan ve bir şey yiyip içmeden ölç. | DSÖ/JHU Aile Planlaması El Kitabı; Family Planning NSW 2013 |
| `fertility_bbt_step_2` | Her gün yaklaşık aynı saatte, yeterli bir uykudan sonra ölç. | DSÖ/JHU El Kitabı; Family Planning NSW 2013; Smoley ve Robinson 2012 |
| `fertility_bbt_step_3` | Ağızdan ölçebilirsin; döngü boyunca aynı yeri, aynı termometreyi ve aynı süreyi kullan. | Family Planning NSW 2013 (oral ya da vajinal, döngü içinde değişmez); mynfp/Sensiplan kılavuzu |
| `fertility_bbt_step_4` | İki ondalık gösteren (36,55 gibi) bir bazal termometre uygun; ölçüm bitene kadar bekle. | mynfp/Sensiplan kılavuzu (en az 0,05 °C çözünürlük, iki ondalık önerilir) |
| `fertility_bbt_step_5` | Değeri hemen kaydet; ateş, alkol ya da kötü uyku gibi bir etken varsa "ölçüm bozuk olabilir" işaretini aç. | Sensiplan bozucu etkenler; Family Planning NSW 2013 |

Kanıt: uzman görüşü (yöntem kuralı). Kaynak dizesi `fertility_bbt_how_source`: "DSÖ/JHU, Family Planning
Handbook; Family Planning NSW 2013; Sensiplan (Frank-Herrmann ve ark. 2007)". "En az 3 saat uyku" ölçütü ikincil
kaynakta görüldü, birincil kaynakta doğrulanmadığı için "yeterli bir uyku" yazıldı.

### 3.2 Bozuk ölçüm etkenleri

`FertilityEntry.bbtDisturbed` için kullanıcı bir ya da birkaç etken seçer (yalnızca açıklama; motor tek bayrak
tutar). Liste başlığı `fertility_bbt_reasons_title` "Ölçümü etkileyebilen etkenler"; kaynak dizesi
`fertility_bbt_reasons_source`. Liste Sensiplan "bozucu etkenler" (Störfaktoren) kavramına ve doğrulanan kurum
listelerine dayanır:

| Anahtar | Etiket (≤ 60) | Dayanak |
|---|---|---|
| `fertility_bbt_reason_fever` | Ateş ya da hastalık | DSÖ/JHU El Kitabı; Family Planning NSW 2013; Smoley ve Robinson 2012 |
| `fertility_bbt_reason_alcohol` | Önceki akşam alkol | Family Planning NSW 2013 |
| `fertility_bbt_reason_sleep` | Kısa, kesintili ya da alışılmıştan geç uyku | Family Planning NSW 2013 ("sleeping in later", "too little sleep"); Smoley ve Robinson 2012 |
| `fertility_bbt_reason_time` | Alışılmış saatten çok farklı bir saatte ölçüm | DSÖ/JHU ve FPNSW "aynı saat" kuralının karşılığı |
| `fertility_bbt_reason_travel` | Yolculuk, saat farkı ya da vardiya | Sensiplan (kısmen); vardiya: RRM Academy semptotermal özeti |
| `fertility_bbt_reason_medication` | Yeni başlanan ilaç | Smoley ve Robinson 2012 |
| `fertility_bbt_reason_blanket` | Elektrikli battaniye | Family Planning NSW 2013 |

| Anahtar | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|
| `fertility_bbt_disturbed_label` | Ölçüm bozuk olabilir | — | — |
| `fertility_bbt_disturbed_help` | Bozuk işaretli ölçüm grafikte içi boş görünür ve yükselme hesabına girmez; kaydın silinmez. | uzman görüşü | Sensiplan (bozuk ölçüm ayraç içine alınır) |
| `fertility_bbt_out_of_range` | 35,0–38,5 °C dışındaki değerler hesaba girmez; yazım hatası olabilir, kontrol et. | — | ADR-0005 (giriş hatası süzgeci) |

### 3.3 Isı neden yükselir, neyi söyler?

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `fertility_bbt_why` | Isı neden yükselir? | Yumurtlamadan sonra salınan progesteron vücut ısısını yaklaşık 0,2–0,5 °C yükseltir ve adete kadar yüksek tutar. Yükselme yumurtlamayı sonradan doğrular; önceden haber vermez. | orta | DSÖ/JHU, Family Planning Handbook; Family Planning NSW 2013; Ecochard ve ark. 2001, BJOG |
| `fertility_bbt_imprecise` | Tahmini gün yaklaşıktır | Isı yükselmesi çoğu zaman yumurtlamadan birkaç gün sonra görünür; bu yüzden "tahmini yumurtlama günü" yaklaşık bir tarihtir, kesin değildir. | orta | Ecochard ve ark. 2001, BJOG |
| `fertility_rule` | Doğrulama kuralı | Önceki altı geçerli ölçümün en yükseğinin (örtü çizgisi) üstünde art arda üç ölçüm; üçüncüsü çizginin en az 0,2 °C üstünde. Kural sağlanmadan yumurtlama "doğrulandı" denmez. | orta | Sensiplan (Frank-Herrmann ve ark. 2007, Hum Reprod); Family Planning NSW 2013; DSÖ/JHU El Kitabı |

Dayanak notu: Ecochard 2001 (107 kadın, 326 döngü, ultrason referanslı): ısı yükselmesi döngülerin yüzde 98'inde
görüldü ama yumurtlamanın ±1 günü içine yalnızca yüzde 10,8'inde düştü; LH zirvesi ±1 gün içinde yüzde 67,6.
Bu, "doğrular ama günü kesin söylemez" cümlesinin dayanağıdır. Çalışma PCOS'u dışladığı için PCOS'ta bu
kesinlik daha da düşük varsayılır; etiket "orta" (genel nüfus, kılavuz dışı; bir basamak düşürüldü).

## 4. LH (yumurtlama) testi

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `fertility_lh` | LH (yumurtlama) testi | — | — | — |
| `fertility_lh_how` | Nasıl okunur? | Test çizgisi kontrol çizgisi kadar ya da daha koyuysa pozitif, daha açıksa negatiftir; silik çizgi de negatif sayılır. Her gün aynı saatte test et, öncesinde çok sıvı içme. | uzman görüşü | Clearblue Digital Ovulation Test kullanım talimatı |
| `fertility_lh_how_note` | — | Kendi testinin talimatı önceliklidir; markalar arasında küçük farklar olabilir. | — | — |
| `fertility_lh_invalid_help` | — | Kontrol çizgisi çıkmadıysa test geçersizdir; yeni bir testle tekrarla. | uzman görüşü | Clearblue kullanım talimatı |
| `fertility_lh_positive_meaning` | Pozitif ne demek? | Pozitif LH, yumurtlamanın yaklaştığını düşündürür; çoğu zaman 1–2 gün içinde olur ama her LH yükselişi yumurtlamayla sonuçlanmaz. Kesin bilgi ısı yükselmesinden gelir. | orta | Su ve ark. 2017, Bioeng Transl Med; Ecochard ve ark. 2001, BJOG; Hoff ve ark. 1983 ve Testart ve ark. 1981 (Clearblue derlemesi) |
| `fertility_lh_pcos` | PCOS’ta LH testi yanıltabilir | PCOS’ta LH döngü boyunca yüksek seyredebilir; test, yumurtlama olmadan da pozitif görünebilir. Üreticiler de bu uyarıyı yapar. Bu yüzden LH yalnızca kayıttır. | sınırlı | Clearblue kullanım talimatı; McGovern ve ark. 2004, Fertil Steril; Taylor ve ark. 1997, J Clin Endocrinol Metab |

Etiketler: `fertility_lh_negative` Negatif · `fertility_lh_positive` Pozitif · `fertility_lh_invalid` Geçersiz.

Dayanak notları:
- Clearblue talimatı (505126_11): "If the Surge Line is a lighter shade than the Reference Line or there is no
  Surge Line present" → yükseliş yok; "have polycystic ovarian syndrome you may get a misleading result";
  "Not for contraceptive use"; aynı saatte test, en az 4 saat idrar yapmama ve aşırı sıvıdan kaçınma. "4 saat"
  markaya özgü olduğu için metne alınmadı; "kendi testinin talimatı öncelikli" yazıldı.
- McGovern 2004 (12 merkez, düzenli döngülü fertil ve infertil kadınlar): pozitif ev LH testinden 7–13 gün sonra
  alınan biyopsilerin yüzde 7'sinden fazlasında sekretuvar endometrium yoktu (yanlış pozitif). PCOS'a özgü
  değil; PCOS'ta yüksek bazal LH (Taylor 1997, kısmen) riski artırır varsayımı "sınırlı" etiketiyle.
- Su 2017: normal fertil kadınlarda döngülerin yüzde 10,7'sinde LH yükselişi olup yumurta atılmadı
  (luteinize rüptüre olmamış folikül) → "her LH yükselişi yumurtlamayla sonuçlanmaz".
- LH yükselişinden yumurtlamaya 34–38 saat (Hoff 1983; Testart 1981, Clearblue derlemesinde) → "1–2 gün".

## 5. Servikal mukus

| Anahtar | Etiket | Not |
|---|---|---|
| `fertility_mucus_dry` | Kuru | `CervicalMucus.DRY` |
| `fertility_mucus_sticky` | Yapışkan | `STICKY` |
| `fertility_mucus_creamy` | Kremsi | `CREAMY` |
| `fertility_mucus_watery` | Sulu | `WATERY` |
| `fertility_mucus_egg_white` | Yumurta akı | `EGG_WHITE`: şeffaf, uzayan, kaygan |

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `fertility_mucus` | Servikal mukus | Yalnızca kayıt için; doğrulamaya girmez. Yumurta akı kıvamındaki şeffaf, uzayan mukus en verimli günlere denk gelir; düzenli gözlem gebelik şansının yükselmesiyle ilişkili bulundu. | sınırlı | Evans-Hoeker ve ark. 2013, Fertil Steril; Bigelow ve ark. 2004, Hum Reprod |
| `fertility_mucus_pcos_note` | — | PCOS’ta yumurtlama gecikince verimli tip mukus günlerce sürebilir ya da yinelenebilir; mukus tek başına yumurtlama demek değildir. | uzman görüşü | Sensiplan çifte kontrol ilkesi (mukus tek başına yetmez; RRM Academy özeti) |

Dayanak notu: Evans-Hoeker 2013 (331 kadın): tip 4 mukus "şeffaf, uzayan, çiğ yumurta akını andıran"; tutarlı
mukus gözlemi yapılan döngülerde gebelik olasılığı daha yüksek (düzeltilmiş FR 2,32; yüzde 95 GA 1,23–4,40).
Bigelow 2004 (782 kadın, 7 288 döngü): mukus puanı yükseldikçe gebelik olasılığı artar; mukus kalitesi ilişki
zamanlamasından daha iyi öngörücü. İkisi de gözlemsel ve PCOS'a özgü değil → "sınırlı". Mukus tipleri Türkçe
etiketlere uygulamada yaygın beş kategoriyle çevrildi; Sensiplan ve FPNSW üç/dört basamaklı sınıflama kullanır,
kayıt amaçlı olduğu için fark önemsiz.

## 6. Grafik ve döngü özeti metinleri

| Anahtar | Metin | Not |
|---|---|---|
| `fertility_chart_title` | Isı grafiği | |
| `fertility_chart_coverline` | Örtü çizgisi | yalnızca doğrulama varken çizilir |
| `fertility_chart_excluded` | Hesaba girmedi | içi boş nokta açıklaması (bozuk ya da aralık dışı) |
| `fertility_chart_lh_marker` | LH pozitif | işaretçi etiketi |
| `fertility_chart_empty` | Isı ölçümlerini kaydettikçe grafik burada oluşur. İlk kural için dokuz ölçüm gerekir; birkaç gün kaçarsa sorun değil. | |
| `fertility_chart_a11y` | Bu döngünün ısı grafiği: %1$d ölçüm, %2$d tanesi hesaba girmedi. %3$s | `%3$s` = örtü çizgisi varsa "Örtü çizgisi 36,5 derece." yoksa boş |
| `fertility_chart_coverline_a11y` | Örtü çizgisi %1$s derece. | |
| `fertility_ovulation_estimate` | Tahmini yumurtlama: %1$s (yaklaşık) | `OvulationEstimate.ovulationDate` |
| `fertility_confirmed_on` | %1$s tarihinde doğrulandı | `confirmedOn` |
| `fertility_luteal_day` | Yumurtlama sonrası %1$d. gün | `lutealDay` |
| `fertility_cycle_day` | Döngünün %1$d. günü | |
| `fertility_bbt_value_label` | Isı (°C) | giriş alanı |
| `fertility_bbt_time_label` | Ölçüm saati | giriş alanı |
| `fertility_bbt_value_hint` | 36,55 | yer tutucu |

## 7. `FertilityNote` metinleri

Her nota başlık, metin, kanıt çipi ve kaynak. Sıra motorun bildirim sırasıdır.

| Enum | Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|---|
| `OVULATION_CONFIRMED` | `fertility_note_confirmed` | Yumurtlama doğrulandı | Isın üç gün üst üste örtü çizgisinin üstünde kaldı; bu döngüde yumurtlama olmuş görünüyor. Tahmini gün yaklaşıktır, yükselme yumurtlamadan birkaç gün sonra görünebilir. | orta | Sensiplan (Frank-Herrmann ve ark. 2007, Hum Reprod); Ecochard ve ark. 2001, BJOG |
| `TOO_FEW_READINGS` | `fertility_note_too_few` | Henüz yeterli ölçüm yok | Kural için en az dokuz geçerli ölçüm gerekir: altı önceki, üç yüksek. Birkaç gün kaçarsa sorun değil; ölçmeye devam et. | uzman görüşü | Sensiplan yöntem kuralı; Family Planning NSW 2013 |
| `MANY_DISTURBED` | `fertility_note_many_disturbed` | Bozuk ölçüm fazla | Bu döngüde ölçümlerin önemli bir kısmı bozuk işaretli; grafik güvenilir okunmayabilir. Mümkünse ölçüm saatini ve koşullarını sabitlemeyi dene. | uzman görüşü | Sensiplan bozucu etkenler; ADR-0005 (eşik ürün sezgisi) |
| `LH_POSITIVE_WITHOUT_RISE` | `fertility_note_lh_without_rise` | LH pozitif ama ısı yükselmedi | Pozitif testten sonra beş ölçümde yükselme yok. PCOS’ta LH testi yumurtlama olmadan da pozitif çıkabilir; yumurtlama olmamış ya da gecikmiş olabilir. Sık olursa doktoruna söyle. | sınırlı | Clearblue kullanım talimatı; McGovern ve ark. 2004, Fertil Steril; Su ve ark. 2017, Bioeng Transl Med |
| `RISE_WITHOUT_LH_POSITIVE` | `fertility_note_rise_without_lh` | Isı yükseldi, LH pozitif yakalanmadı | Yumurtlama ısıyla doğrulandı ama testlerin pozitif göstermedi. LH yükselişi kısa sürebilir; günde tek test onu kaçırabilir. Doğrulama geçerli. | uzman görüşü | Direito ve ark. 2013, Fertil Steril; Clearblue kullanım talimatı (günlük, aynı saatte test) |
| `LONG_LUTEAL` | `fertility_note_long_luteal` | Yumurtlama sonrası 18 günü geçti | Isın 18 günden uzun süredir yüksek ve adet gelmedi. Bu, gebelik olasılığının klasik işaretidir; bir gebelik testi yapabilirsin. Test negatifse ve adet gelmezse doktorunla konuş. | orta | Lenton ve ark. 1984, Br J Obstet Gynaecol; Crawford ve ark. 2017, Fertil Steril; NHS, Doing a pregnancy test; Sensiplan |

Dayanak notları:
- `OVULATION_CONFIRMED` "orta": semptotermal ısı kuralı DSÖ/JHU el kitabı ve büyük prospektif kohortla
  (Frank-Herrmann 2007: 900 kadın, 17 638 döngü) desteklenir ama PCOS kılavuzu bunu benimsemez ve PCOS'ta
  test edilmemiştir; kanıt özetindeki "BBT ile yumurtlama — sınırlı" satırının "orta"ya çekilmesi önerilir
  (çip açıklaması "iyi tasarlanmış çalışmalar var ama çoğu PCOS'a özgü değil" bu duruma tam oturur).
- `LONG_LUTEAL` 18 gün: normal luteal evre ortalama 14,1 gün (SS 1,4; Lenton 1984), Crawford 2017'de 16–20 gün
  "uzun" kategorisi; 18'i aşan yüksek ısı doğal aile planlamasında gebelik işaretidir (Sensiplan öğretisi,
  birincil kaynak okunamadı, kısmen). Kullanıcı metni gebelik testine ve NHS zamanlamasına yönlendirir, tanı
  koymaz.
- `RISE_WITHOUT_LH_POSITIVE`: LH yükselişinin süresi ve biçimi kadınlar arasında değişkendir (Direito 2013,
  künye doğrulandı, içerik kısmen); Clearblue günlük ve aynı saatte test ister. Metin "doğrulama geçerli" diyerek
  kullanıcıyı LH'ye değil ısıya güvenmeye çağırır.
- `MANY_DISTURBED` eşiği (≥ 3 ve ≥ yüzde 30) klinik değil, ürün sezgisidir; metin bunu iddia etmez.

## 8. Düzensiz döngü, umut ve "ne zaman doktora?"

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `fertility_irregular` | Döngün düzensizse | Döngüler 21 günden kısa, 35 günden uzun ya da yılda sekizden azsa yumurtlama düzensiz olabilir. Bu konuşulabilir bir durum; doktoruna götürecek bir not hazırlayabiliriz. | güçlü | Teede ve ark. 2023, Hum Reprod (1.1.1, 1.1.5) |
| `fertility_regular_anovulation` | Düzenli döngüde de olabilir | Düzenli döngüde de yumurtlama olmayabilir; grafikte yükselme görünmeyen döngüler bunun ipucu olabilir. Emin olmak için doktorun kan testi isteyebilir. | güçlü | Teede ve ark. 2023, Hum Reprod (1.1.5: serum progesteron) |
| `fertility_hope` | Dürüst bir not | PCOS’ta yumurtlama her ay olmayabilir; bu, gebeliğin olmayacağı anlamına gelmez. Yumurtlamayı destekleyen tedaviler var; bir yıl beklemeden doktora gidilebilir. | güçlü | NHS, Polycystic ovary syndrome (PMOS); NICE CG156; Teede ve ark. 2023 |
| `fertility_doctor_when` | Ne zaman doktora? | Bir yıl korunmasız ilişkiye rağmen gebelik olmadıysa değerlendirme önerilir; döngün düzensizse ya da 36 yaş ve üstüysen daha erken. PCOS bilinen bir nedendir; beklemen gerekmez. | güçlü | NICE CG156 (2013, güncelleme 2017); NHS, PMOS |
| `fertility_pregnancy_test` | Gebelik testi ne zaman? | Adetin geciktiği ilk günden test yapabilirsin; döngün düzensizse son korunmasız ilişkiden en az 21 gün sonra. Pozitif sonuç neredeyse kesindir; negatifse birkaç gün sonra tekrarla. | güçlü (NHS rehberi) | NHS, Doing a pregnancy test |

Dayanak notları:
- 2023 kılavuzu 1.1.1: menarştan 3 yıl sonrası–perimenopoz için düzensiz döngü "< 21 ya da > 35 gün ya da
  yılda < 8 döngü"; 1.1.5: "Ovulatory dysfunction can still occur with regular cycles", gerekirse serum
  progesteron. MJA özeti: anovulatuvar infertilitede letrozol ilk basamak (kanıta dayalı öneri) → "tedaviler
  var" cümlesinin dayanağı; ilaç adı kullanıcıya verilmez.
- NICE CG156: 1 yıl korunmasız ilişki sonrası değerlendirme; 36 yaş ve üstü ya da bilinen/şüphelenilen neden
  (örnekler arasında düzensiz ya da olmayan adet) varsa daha erken yönlendirme. Özet ikincil kaynaktan (kısmen).
- NHS PMOS (30 Haziran 2026): "PMOS can sometimes cause problems getting pregnant"; neden "ovaries do not release
  an egg each month"; klomifen "a medicine used to improve fertility". Önceki "most women are able to get
  pregnant" cümlesi sayfada artık yok; bu yüzden metin "olmayacağı anlamına gelmez" der, oran vermez.

## 9. Kanıt özeti satırları (pcos-kanit-ozeti.md'ye eklenecek: "MVP 3: Doğurganlık (#24)")

| Konu | Uygulamadaki kullanım | Dayanak | Düzey |
|---|---|---|---|
| Isı yükselmesiyle yumurtlama doğrulama (3-üstü-6, +0,2 °C) | `fertility_rule`, `fertility_note_confirmed` | Sensiplan kuralları; Frank-Herrmann ve ark. 2007 Hum Reprod (900 kadın, 17 638 döngü); DSÖ/JHU El Kitabı (3 gün); Ecochard ve ark. 2001 BJOG (yükselme ±1 gün içinde yalnızca yüzde 10,8) | orta (mevcut "sınırlı" satırı güncellenmeli) |
| BBT ölçüm kuralları ve bozucu etkenler | `fertility_bbt_step_*`, `fertility_bbt_reason_*` | DSÖ/JHU El Kitabı; Family Planning NSW 2013; Smoley ve Robinson 2012 Am Fam Physician; mynfp/Sensiplan kılavuzu (0,05 °C) | uzman görüşü |
| Progesteron ve 0,2–0,5 °C | `fertility_bbt_why` | DSÖ/JHU El Kitabı; Family Planning NSW 2013 | orta |
| LH testi okuma | `fertility_lh_how`, `fertility_lh_invalid_help` | Clearblue kullanım talimatı | uzman görüşü |
| PCOS'ta LH yanlış pozitif | `fertility_lh_pcos`, `fertility_note_lh_without_rise` | Clearblue uyarısı; McGovern ve ark. 2004 (> yüzde 7 yanlış pozitif); Taylor ve ark. 1997 (PCOS'ta LH yüksekliği, kısmen); Su ve ark. 2017 (LUF yüzde 10,7) | sınırlı |
| LH → yumurtlama zamanı | `fertility_lh_positive_meaning` | Hoff 1983, Testart 1981 (34–38 saat; Clearblue derlemesi, kısmen); Ecochard 2001 | orta |
| Servikal mukus | `fertility_mucus` | Evans-Hoeker ve ark. 2013; Bigelow ve ark. 2004 | sınırlı |
| 18 günü aşan luteal evre | `fertility_note_long_luteal` | Lenton 1984; Crawford 2017; Sensiplan (kısmen); NHS gebelik testi | orta |
| Düzensiz döngü ve anovulasyon | `fertility_irregular`, `fertility_regular_anovulation` | Teede 2023 (1.1.1, 1.1.5) | güçlü |
| Umut ve yönlendirme | `fertility_hope`, `fertility_doctor_when` | NHS PMOS; NICE CG156 (kısmen); MJA 2024 özeti (letrozol) | güçlü |
| Doğum kontrolü için kullanılmaz | `fertility_not_contraception` | NHS Natural family planning (uzman eğitimi; yüzde 91–99 / 76); Clearblue "not for contraceptive use" | güvenlik (kanıt düzeyinden bağımsız) |

## 10. Kaynakça ve doğrulama durumu

Sayım: 23 kaynak → 17 doğrulandı, 6 kısmen, 0 doğrulanamadı. Etiketler plan-icerigi.md ile aynı.

1. Frank-Herrmann P, Heil J, Gnoth C ve ark. The effectiveness of a fertility awareness based method to avoid
   pregnancy in relation to a couple's sexual behaviour during the fertile time: a prospective longitudinal
   study. Hum Reprod. 2007;22(5):1310–1319. — doğrulandı (künye ve kohort büyüklüğü ikincil sayfada; ADR-0005)
2. Arbeitsgruppe NFP. Natürlich und sicher: Das Praxisbuch (Sensiplan el kitabı), TRIAS. — kısmen (kitap;
   kurallar, bozucu etkenler ve 18 gün kuralı bellekten; mynfp kılavuzu bu kitaba yönlendiriyor)
3. mynfp. Gebrauchsanleitung (Sensiplan kuralları için uygulama kılavuzu). mynfp.de. — doğrulandı (termometre
   en az 0,05 °C çözünürlük, iki ondalık; döngü içinde ölçüm yeri/süresi değişmez; bozuk değerler ayraç içine)
4. Family Planning NSW. Fertility awareness based methods of contraception, lactational amenorrhoea and
   withdrawal (Fact Sheet, Mayıs 2013). — doğrulandı
5. WHO/Johns Hopkins CCP. Family Planning: A Global Handbook for Providers, Basal Body Temperature bölümü
   (fphandbook.org). — doğrulandı (sayfa; baskı yılı görülmedi)
6. Smoley BA, Robinson CM. Natural family planning. Am Fam Physician. 2012;86(10):924–928. — doğrulandı
7. RRM Academy. The Sympto-Thermal Method (özet sayfa). — doğrulandı (ikincil; yalnızca "vardiya" ve "3 saat"
   ayrıntıları buradan, 3 saat metne alınmadı)
8. Ecochard R, Boehringer H, Rabilloud M, Marret H. Chronological aspects of ultrasonic, hormonal, and other
   indirect indices of ovulation. BJOG. 2001;108(8):822–829. — doğrulandı
9. Direito A, Bailly S, Mariani A, Ecochard R. Relationships between the luteinizing hormone surge and other
   characteristics of the menstrual cycle in normally ovulating women. Fertil Steril. 2013;99(1):279–285. —
   kısmen (künye Clearblue derlemesinde doğrulandı; içerik bellekten; uygulamaya almadan önce özet okunmalı)
10. Hoff JD ve ark. 1983; Testart J ve ark. 1981 (LH yükselişinden yumurtlamaya 34–35 / 36–38 saat). —
    kısmen (yalnızca Clearblue HCP derlemesi PUB-0108'de görüldü)
11. Clearblue. Digital Ovulation Test kullanım talimatı (505126_11). clearblue.com. — doğrulandı
12. Godbert S, Shreeves S, Johnson S (SPD Development). Clearblue HCP yayını PUB-0108 (LH yükselişi tanımı
    derlemesi; üretici finansmanlı). — doğrulandı (yalnızca 9 ve 10 numaralı künyeler için)
13. McGovern PG, Myers ER, Silva S ve ark. Absence of secretory endometrium after false-positive home urine
    luteinizing hormone testing. Fertil Steril. 2004;82(5):1273–1277. — doğrulandı
14. Su HW, Yi YC, Wei TY, Chang TC, Cheng CM. Detection of ovulation, a review of currently available methods.
    Bioeng Transl Med. 2017;2(3):238–246. — doğrulandı
15. Taylor AE ve ark. Determinants of abnormal gonadotropin secretion in clinically defined women with polycystic
    ovary syndrome. J Clin Endocrinol Metab. 1997;82(7):2248–2256. — kısmen (başlık aramada görüldü; sayfa
    açılamadı)
16. Escobar-Morreale HF. Polycystic ovary syndrome: definition, aetiology, diagnosis and treatment. Nat Rev
    Endocrinol. 2018;14(5):270–284. — kısmen (başlık doğrulandı; yalnızca ekip notu için)
17. Lenton EA, Landgren BM, Sexton L. Normal variation in the length of the luteal phase of the menstrual
    cycle: identification of the short luteal phase. Br J Obstet Gynaecol. 1984;91(7):685–689. — doğrulandı
18. Crawford NM, Pritchard DA, Herring AH, Steiner AZ. Prospective evaluation of luteal phase length and natural
    fertility. Fertil Steril. 2017;107(3):749–755. — doğrulandı
19. Evans-Hoeker E, Pritchard DA, Long DL, Herring AH, Stanford JB, Steiner AZ. Cervical mucus monitoring
    prevalence and associated fecundability in women trying to conceive. Fertil Steril. 2013;100(4):1033–1038.
    — doğrulandı
20. Bigelow JL, Dunson DB, Stanford JB, Ecochard R, Gnoth C, Colombo B. Mucus observations in the fertile window:
    a better predictor of conception than timing of intercourse. Hum Reprod. 2004;19(4):889–892. — doğrulandı
21. NHS. Doing a pregnancy test. nhs.uk/pregnancy/trying-for-a-baby/doing-a-pregnancy-test. — doğrulandı
22. NHS. Natural family planning (fertility awareness). nhs.uk/contraception. — doğrulandı
23. NICE. Fertility problems: assessment and treatment (CG156), 2013, güncelleme 2017. — kısmen (öneriler
    ikincil özetten; birincil sayfa okunmadı)
24. Teede HJ ve ark. 2023 kılavuzu (Hum Reprod 2023;38(9):1655–1679) ve Monash tam kılavuz Bölüm 1 (1.1.1,
    1.1.5); Teede ve ark. Med J Aust 2024 özeti (letrozol ilk basamak). — doğrulandı (cilt-sac-icerigi.md E.1–3)
25. NHS. Polycystic ovary syndrome (PMOS), son inceleme 30 Haziran 2026. — doğrulandı

Not: 24 ve 25 cilt belgesindeki kaynak listesiyle ortaktır; sayıma ayrı girmez (toplam 23).

## 11. Uygulama notları (ekip için)

1. **Görünürlük.** Bölüm yalnızca `Goal.TTC` açıkken ve hap modu kapalıyken; hap modunda
   `fertility_disabled_pill` kartı (kayıtlar silinmez). Günlük kayıt ekranında "Doğurganlık" bölümü, grafik
   ayrı ekran.
2. **Her ekranda** `fertility_not_contraception` (bant) ve bölüm sonunda `fertility_not_diagnosis` +
   `plan_content_disclaimer`.
3. **Notlar** `FertilityNote` sırasıyla, her biri kanıt çipi ve `plan_source_format` ile; `LONG_LUTEAL` ve
   `LH_POSITIVE_WITHOUT_RISE` için "Doktor notu oluştur" düğmesi (`alert_action_doctor_note` yeniden kullanılır).
4. **Bozuk ölçüm nedenleri** yalnızca kullanıcı açıklamasıdır; motor `bbtDisturbed` bayrağını okur. Nedenler
   Room'da saklanacaksa yeni alan + Migration + şema json.
5. **Grafik erişilebilirliği.** `fertility_chart_a11y`; içi boş nokta "hesaba girmedi" ile metinle de ayrılır;
   büyük yazıda eksen etiketleri alt listeye taşınır; `LocalReducedMotion` açıkken çizgi animasyonu yok.
6. **Ekran görüntüleri.** `EkranGoruntuleriTest` + `y0N`: boş grafik, doğrulanmış döngü (örtü çizgisi + LH
   işareti), LH pozitif–yükselme yok notu, 18 gün notu, hap modu kapalı kartı.
7. **Dil denetimi.** `fertility_` metinlerinde "başarısız", "kaçırdın", "geç kaldın", "kısır" geçmez; test
   ekleyin.
8. **Kanıt özeti.** pcos-kanit-ozeti.md'deki "BBT ile yumurtlama — sınırlı" satırı 9. bölüme göre
   güncellenir; "deneysel" etiketi bu bölümde hiçbir yerde kullanılmaz.
