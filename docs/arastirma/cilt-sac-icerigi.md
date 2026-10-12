# Cilt ve saç içeriği (MVP 3, #25)

Durum: klinik içerik taslağı, uzman onayı bekliyor · 12 Ekim 2026 · İlgili iş: #25 (US-090..094: cilt ve saç
profili, sabah/akşam rutini, adım işaretleme ve hatırlatma, şifreli fotoğraf, dermatoloğa gitme ölçütleri) ·
Kanıt etiketleme ve kaynak doğrulama kalıbı: [plan-icerigi.md](plan-icerigi.md) · Kanıt satırları: G bölümü
(pcos-kanit-ozeti.md'ye aktarılacak) · Dize taslakları: scratchpad `mvp3/cilt_strings.xml` (depo dışı).

Bu belge tıbbi tavsiye değildir. Ayla'nın wellness sınırı (TİTCK / MDCG 2019-11) içinde kalan, `strings.xml`'e
girecek metinleri ve dayanaklarını toplar. Belgedeki hiçbir cümle tanı, tedavi ya da ilaç önerisi değildir.

## 1. Kapsam ve klinik sınır

**Ne yapıyoruz.** Cilt ve saç bölümü, PCOS (yeni adıyla PMOS) ile birlikte sık görülen cilt bulgularına (akne,
koyulaşma, tüylenme, saç incelmesi) **kozmetik içerik maddesi** düzeyinde, kanıt etiketi ve kaynak taşıyan bir
bakım ritmi sunar; fotoğrafla değişimi izlemeye ve "ne zaman doktora?" sorusuna yardım eder.

**İçerik maddesi düzeyi (girer):** nazik temizleyici, nemlendirici (seramid, gliserin), geniş spektrumlu SPF 30+
güneş koruyucu (hassas ciltte mineral filtre), niasinamid, azelaik asit (kozmetik yüzdeler), salisilik asitli
temizleyici (durulanan, en çok yüzde 2), AHA (glikolik/laktik, kozmetik yüzde), gözenek tıkamayan (non-komedojenik)
ve kokusuz ürün seçimi.

**Girmez (yalnızca "doktoruna/dermatoloğuna danış" yönlendirmesi):** topikal retinoidler (tretinoin, adapalen,
tazaroten), izotretinoin, benzoil peroksit, topikal ve ağızdan antibiyotikler, spironolakton ve diğer
antiandrojenler, eflornitin, minoksidil, metformin, doğum kontrol hapının cilde etkisi, hidrokinon, klaskoteron,
kimyasal peeling ve lazerle leke/akne tedavisi. Bunlar 2024 AAD akne kılavuzunda ve 2023 PCOS kılavuzunda
tedavi seçenekleri olarak yer alır; Ayla tedavi önermediği için adlarını yalnızca "kaçınılacaklar" ve "doktora
sor" bağlamında kullanır (gebelikte izotretinoin uyarısı gibi güvenlik bilgisi hariç). Marka adı hiç geçmez.

**Sıklık ve miktar dili.** Doz yok. Yalnızca kozmetik güvenli genel ifadeler: "sabah güneş koruyucu, akşam
nemlendirici", "haftada birkaç geceyle başla", "günde en fazla iki kez yıka". Yüzde bilgisi yalnızca mevzuat
üst sınırı olarak ve "yüzde 2" biçiminde (XML'de `%` kullanılmaz).

**Tanı yok.**
- Akne şiddeti kullanıcının **öz-beyanıdır** ("senin gözlemin"); klinik derecelendirme (IGA vb.) yapılmaz.
- Hirsutizm için **Ferriman-Gallwey puanı hesaplanmaz**; yalnızca "yüz/vücut tüylenmesi beni rahatsız ediyor"
  öz-beyanı alınır.
- Akantozis nigrikans öz-beyandır; metin "insülin direnciyle ilişkili olabilir" der (kanıt etiketiyle), tanı
  koymaz, kiloya atıf yapmaz, "doktorunla paylaş" ve doktor notuna ekleme önerir.
- Fotoğraf tanı aracı değildir; yalnızca değişimi görmeye ve doktora göstermeye yarar.
- "Dermatolog onaylı" iddiası yapılmaz; kart üstünde `skin_review_pending` ("uzman onayı bekliyor") görünür,
  onay alınınca kaldırılır.

**Dil.** plan-icerigi.md kuralları aynen geçerli (kalori, kilo, diyet, hedef, kaçırdın, yasak, zayıflama, detoks
geçmez; "kaçtı / kaçarsa"). Ek olarak görünüm idealleştiren sözcükler yok: kusur, kusursuz, pürüzsüz,
lekesiz, mükemmel, "cilt sorunu" yerine "cilt bulgusu / cildin". Akne ve tüylenme için "utanç", "mücadele",
"savaş" kullanılmaz. Kişi önce gelir.

**Veri ve gizlilik.** Cilt profili ve fotoğraflar sağlık verisidir (KVKK özel nitelikli): ayrı rıza, şifreli
saklama (ADR-0010), galeriye düşmez, `FLAG_SECURE`, dışa aktarma ve tam silme. Fotoğraf hiçbir yere gönderilmez,
görüntü analizi yapılmaz.

## 1b. Giriş ve sınır ibareleri (kullanıcı metinleri)

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `skin_title` | Cilt ve saç | — | — | — |
| `skin_intro` | Cildine nazik bir ritim | PCOS (yeni adıyla PMOS) ciltte akne, koyulaşma ve tüylenmeyle kendini gösterebilir. Burada içerik maddesi düzeyinde, kanıtı belli bakım adımları var; tedavi değildir. | güçlü | NHS, Polycystic ovary syndrome (PMOS); Teede ve ark. 2023, Hum Reprod |
| `skin_not_treatment` | — | Bu rutin bir tedavi değildir; reçeteli ürün ya da ilaç önermiyoruz. Cildin için bir tedavi düşünüyorsan dermatoloğuna danış. | — | — |
| `skin_not_diagnosis` | — | Profil bilgileri senin gözlemindir; tanı yerine geçmez. Doktorunun değerlendirmesi farklı olabilir. | — | — |
| `skin_review_pending` | — | Bu içerik uzman onayı bekliyor; kaynakları her kartta görebilirsin. | — | — |
| `skin_doctor_note_add` | Doktor notuna ekle | — | — | — |

Yerleşim: `skin_review_pending` bölümün en üstünde tek bant; `skin_not_treatment` rutin listesinin altında;
`skin_not_diagnosis` profil ekranının altında; bölüm sonunda `plan_content_disclaimer`.

## 2. Profil alanları

Enum anahtarları İngilizce (veri katmanı), etiketler Türkçe (`skin_` dizeleri). Hepsi isteğe bağlı; boş profil
"temizleyici + nemlendirici + güneş koruyucu" temel ritmini verir.

| Alan | Tip / enum | Etiket anahtarı | Not |
|---|---|---|---|
| Cilt tipi | `SkinType { DRY, OILY, COMBINATION, NORMAL, SENSITIVE }` | `skin_type_*` | Hassasiyet aslında diğer tiplerle birlikte görülür; uygulama kolaylığı için ayrı `sensitive: Boolean` alanı tercih edilebilir (uygulama kararı; metinler iki kurguda da aynı). Yardım metni tip tanımı vermez ("emin değilsen Normal") |
| Akne | `AcneSelfReport { NONE, MILD, MODERATE, SEVERE }` | `skin_acne_*` | Öz-beyan; etiketler Yok / Hafif / Orta / Yoğun. SEVERE seçilince aktif önerilmez, dermatolog kartı öne çıkar |
| Leke / koyulaşma | `darkSpots: Boolean` | `skin_profile_dark_spots` | Akne sonrası koyulaşma ve güneş lekesi ayrımı yapılmaz |
| Akantozis nigrikans | `acanthosisSelfReport: Boolean` | `skin_profile_acanthosis` | Yardım metni `skin_acanthosis_help`; işaretlenince "doktor notuna ekle" önerilir |
| Tüylenme | `unwantedHairBothers: Boolean` | `skin_profile_hair_growth` | Puan yok; yalnızca rahatsızlık öz-beyanı. Tüy alma bilgisi ve "hızla arttıysa doktora" notu açılır |
| Saç dökülmesi / incelme | `hairThinning: Boolean` | `skin_profile_hair_thinning` | Saç bakımı kartları ve "doktoruna git" notu açılır |
| Gebelik planı | `UserPreferences.Goal.TTC` (ayarlardaki "Hamile kalmayı planlıyorum") | `skin_pregnancy_from_goals` | Profilde düzenlenmez, ayarlardan gelir; açıkken 3.5 listesi rutinden çıkar |
| Gebe ya da emziriyor | `pregnantOrBreastfeeding: Boolean` (isteğe bağlı, öneri) | `skin_profile_pregnant_now` | TTC ile aynı dışlama kuralını uygular; ek olarak her kartta "ebene/doktoruna sor" |

Öneri (Kotlin, `data/skin`):

```kotlin
enum class SkinType { DRY, OILY, COMBINATION, NORMAL, SENSITIVE }
enum class AcneSelfReport { NONE, MILD, MODERATE, SEVERE }

data class SkinProfile(
    val skinType: SkinType? = null,
    val acne: AcneSelfReport = AcneSelfReport.NONE,
    val darkSpots: Boolean = false,
    val acanthosisSelfReport: Boolean = false,
    val unwantedHairBothers: Boolean = false,
    val hairThinning: Boolean = false,
    val pregnantOrBreastfeeding: Boolean = false,
)
// pregnancyPlan = Goal.TTC in goals || profile.pregnantOrBreastfeeding
```

### 2.1 Profil ekranı metinleri

| Anahtar | Metin | Kanıt | Kaynak |
|---|---|---|---|
| `skin_profile_title` | Cilt ve saç profilin | — | — |
| `skin_profile_text` | Rutinini buna göre kuruyoruz. Hepsi isteğe bağlı; istediğin zaman değiştirebilirsin. | — | — |
| `skin_profile_skin_type` | Cilt tipin | — | — |
| `skin_type_dry` / `_oily` / `_combination` / `_normal` / `_sensitive` | Kuru / Yağlı / Karma / Normal / Hassas | — | — |
| `skin_type_help` | Emin değilsen Normal seçebilirsin; dermatoloğun söylediği tipi yazman en doğrusu. | — | — |
| `skin_profile_acne` | Akne | — | — |
| `skin_acne_none` / `_mild` / `_moderate` / `_severe` | Yok / Hafif / Orta / Yoğun | — | — |
| `skin_acne_help` | Bu senin gözlemin, tıbbi bir derecelendirme değil. Ağrılı, derin sivilceler ya da iz kalıyorsa dermatoloğa görünmek iyi olur. | güçlü (NHS rehberi) | NHS, Acne |
| `skin_profile_dark_spots` | Leke ya da koyulaşma | — | — |
| `skin_profile_acanthosis` | Boyun, koltuk altı ya da kasıkta kadifemsi koyu alanlar | — | — |
| `skin_acanthosis_help` | Buna akantozis nigrikans denir; insülin direnciyle ilişkili olabilir. Tanı değildir; bir sonraki muayenede doktorunla paylaşmanı öneririz. | güçlü (NHS/AAD rehberi) | AAD, Acanthosis nigricans: Overview; NHS, Acanthosis nigricans |
| `skin_profile_hair_growth` | Yüz ya da vücut tüylenmesi beni rahatsız ediyor | — | — |
| `skin_hair_growth_help` | Puanlama yapmıyoruz; önemli olan senin nasıl hissettiğin. Tüylenme hızla arttıysa doktoruna söyle. | güçlü (NHS rehberi) | NHS, Hirsutism |
| `skin_profile_hair_thinning` | Saç dökülmesi ya da incelme | — | — |
| `skin_profile_pregnancy` | Gebelik planı | — | — |
| `skin_pregnancy_from_goals` | Önceliklerinden alındı: hamile kalmayı planlıyorsun. Rutinden gebelikte kaçınılan içerikler çıkarıldı. | — | — |
| `skin_profile_pregnant_now` | Gebeyim ya da emziriyorum | — | — |
| `skin_pregnant_help` | Bu seçenek açıkken rutin yalnızca gebelikte uygun sayılan içerikleri gösterir. Her ürünü yine de doktoruna ya da ebene sorabilirsin. | — | — |

### 2.2 Rutin, fotoğraf ve hatırlatma arayüz metinleri

| Anahtar | Metin | Not |
|---|---|---|
| `skin_routine_morning` / `skin_routine_evening` | Sabah rutini / Akşam rutini | sekme ya da bölüm başlığı |
| `skin_routine_rules_title` | Başlarken | 3.1 kurallarının başlığı |
| `skin_step_done` / `skin_step_skip` | Yapıldı / Bugün geç | "kaçırdın" yok; "geç" nötr |
| `skin_step_done_a11y` | %1$s, yapıldı olarak işaretlendi | ekran okuyucu |
| `skin_routine_week_count` | Bu hafta %1$d sabah, %2$d akşam | nötr sayı; yüzde ya da "hedef" yok |
| `skin_routine_empty` | Profilini doldurunca rutin buna göre şekillenir; şimdilik temel ritim: temizleyici, nemlendirici, güneş koruyucu. | boş profil |
| `skin_hair_title` | Saç bakımı | 4.1 başlığı |
| `skin_doctor_urgency_now` / `_same_day` / `_soon` | Gecikmeden / Aynı gün / Yakın zamanda | 5. bölüm rozetleri; renk tek başına anlam taşımaz |
| `skin_photo_title` | Fotoğrafla takip | 6. bölüm başlığı |
| `skin_photo_add` / `skin_photo_compare` / `skin_photo_delete` | Fotoğraf ekle / Karşılaştır / Fotoğrafı sil | |
| `skin_photo_date_a11y` | %1$s tarihli cilt fotoğrafı | ekran okuyucu |
| `skin_photo_empty` | İlk fotoğrafını ekleyince burada tarihle görünür; ikinciyle karşılaştırabilirsin. | |
| `skin_notif_morning_title` / `_text` | Sabah rutini / Temizleyici, nemlendirici, güneş koruyucu. Bugün olmazsa sorun değil. | "Gizli bildirimler" ayarına uyar |
| `skin_notif_evening_title` / `_text` | Akşam rutini / Makyajı temizle, nemlendir. Birkaç dakika yeter. | sessiz saatlerde gelmez |
| `skin_reminder_morning` / `skin_reminder_evening` | Sabah hatırlatması / Akşam hatırlatması | ayar satırları |

## 3. Sabah ve akşam rutini

### 3.1 Ortak kurallar (her zaman görünür)

| Anahtar | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|
| `skin_routine_rule_one_product` | Aynı anda tek yeni ürün başlat; cildinin nasıl karşıladığını birkaç hafta izle. | uzman görüşü | AAD, Acne: Tips for managing |
| `skin_routine_rule_patch` | Yeni ürünü önce kol içine madenî para büyüklüğünde sür; 7–10 gün boyunca günde iki kez tekrarla. Kızarıklık, kaşıntı ya da şişlik olursa kullanma. | uzman görüşü | AAD, How to test skin care products before use |
| `skin_routine_rule_irritation` | Yanma, soyulma ya da kızarıklık olursa aktif ürüne ara ver; yalnızca temizleyici, nemlendirici ve güneş koruyucuyla devam et. | uzman görüşü | AAD, Acne: Tips for managing |
| `skin_routine_rule_one_active` | Bir gecede tek aktif içerik yeter; asitleri üst üste kullanmak tahrişi artırabilir. Aktif geceleri dönüşümlü planla. | uzman görüşü | AAD, Acne: Tips for managing (tahriş eden ürünlerden kaçınma) |

### 3.2 Sabah rutini: seçim mantığı (koşul → adım)

| Sıra | Koşul | Adım | Anahtar |
|---|---|---|---|
| 1 | Herkes | Nazik temizleyici; kuru ya da hassas ciltte sabah yalnızca su ya da köpürmeyen süt temizleyici | `skin_step_cleanser`, kuru/hassas: `skin_step_cleanser_dry` |
| 2 | (yağlı ∨ karma ∨ akne hafif/orta ∨ leke) ∧ akne ≠ yoğun | Niasinamid (isteğe bağlı); hassas ciltte önce yama testi | `skin_step_niacinamide` |
| 3 | Herkes | Nemlendirici; yağlı/akneli: hafif, yağsız, non-komedojenik; kuru: seramid/gliserinli yoğun | `skin_step_moisturizer` |
| 4 | Herkes | Geniş spektrumlu SPF 30+; akneli: non-komedojenik; hassas: mineral filtre notu | `skin_step_sunscreen`, hassas: `skin_step_sunscreen_sensitive` |

Gebelik planı açıkken sabah rutini değişmez (temizleyici, niasinamid, nemlendirici, güneş koruyucu kalır;
niasinamid kartına `skin_ttc_niacinamide_note` eklenir).

### 3.3 Akşam rutini: seçim mantığı (koşul → adım)

| Sıra | Koşul | Adım | Anahtar |
|---|---|---|---|
| 1 | Herkes | Temizleyici; makyaj ve güneş koruyucu çıkarılır | `skin_step_cleanser` |
| 2a | Akne hafif/orta ∧ gebelik planı kapalı | Salisilik asitli temizleyici (durulanan, en çok yüzde 2), haftada birkaç gece; ya da azelaik asit | `skin_step_salicylic`, `skin_step_azelaic` |
| 2b | Akne hafif/orta ∧ gebelik planı açık | Azelaik asit; salisilik asit yalnızca durulanan düşük yüzdeli temizleyicide ve kısa süreli (kart notu); leave-on asit ürünleri yok | `skin_step_azelaic`, `skin_avoid_salicylic` |
| 2c | Leke ∧ hassas değil | Azelaik asit ya da niasinamid; AHA haftada 1–2 gece (salisilik gecesiyle aynı gece değil) | `skin_step_azelaic`, `skin_step_niacinamide`, `skin_step_aha` |
| 2d | Leke ∧ hassas | Yalnızca niasinamid (yama testi sonrası); AHA ve salisilik yok | `skin_step_niacinamide` |
| 2e | Akne yoğun | Aktif önerilmez; dermatolog kartı rutinin üstünde | `skin_doctor_acne_severe` |
| 2f | Normal/kuru/hassas ∧ akne yok ∧ leke yok | Aktif yok | — |
| 3 | Herkes | Nemlendirici | `skin_step_moisturizer` |

Rutin dışı ipuçları (her profilde): `skin_tip_hands_off`, saçı yağlı olanlarda `skin_tip_hair_oily`.

### 3.4 Adım kartları (kullanıcı metinleri)

Başlık ≤ 40, metin ≤ 180 karakter. Her kartta kanıt çipi (`KanitCipi`) ve `plan_source_format` + `_source`.

| Anahtar | Başlık | Metin | Kanıt | Kaynak (`_source`) |
|---|---|---|---|---|
| `skin_step_cleanser` | Nazik temizleyici | Yüzünü günde en fazla iki kez ve terledikten sonra, parmak uçlarınla ve ovmadan yıka. Alkolsüz, kokusuz, aşındırmayan bir temizleyici yeter. | uzman görüşü | AAD, Acne: Tips for managing; Türk Dermatoloji Derneği, akne broşürü |
| `skin_step_cleanser_dry` | Kuru ve hassas ciltte temizlik | Sabah yalnızca su ya da köpürmeyen bir süt temizleyici yetebilir. Akşam makyajı ve güneş koruyucuyu mutlaka çıkar; ovmak ve lif kullanmak tahrişi artırır. | uzman görüşü | AAD, Acne: Tips for managing; Türk Dermatoloji Derneği, akne broşürü |
| `skin_step_moisturizer` | Nemlendirici | Her cilt tipi nemlendiriciden yararlanır. Yağlı ciltte hafif, yağsız ve gözenek tıkamayan (non-komedojenik); kuru ciltte seramid ya da gliserinli yoğun bir ürün uygun olabilir. | uzman görüşü | Türk Dermatoloji Derneği, akne broşürü; AAD; Araviiskaia ve Dréno 2016, JEADV |
| `skin_step_sunscreen` | Geniş spektrumlu güneş koruyucu | Sabahın son adımı: SPF 30 ve üzeri, geniş spektrumlu, akneli ciltte gözenek tıkamayan bir güneş koruyucu. Bulutlu günde de sürülür; dışarıdayken yaklaşık iki saatte bir yenilenir. | güçlü | AAD, Sunscreen FAQs; Green ve ark. 2011, J Clin Oncol; Türk Dermatoloji Derneği, akne broşürü |
| `skin_step_sunscreen_sensitive` | Hassas ciltte güneş koruyucu | Çinko oksit ya da titanyum dioksit içeren (mineral) güneş koruyucular hassas ciltte daha az tahriş eder. | uzman görüşü | AAD, Sunscreen FAQs |
| `skin_step_niacinamide` | Niasinamid | B3 vitamininin bir biçimi. Küçük çalışmalarda aknede ve koyu lekelerde yardımcı oldu; kanıt sınırlı. Sabah ya da akşam, nemlendiriciden önce sürülebilir. | sınırlı | Liu ve ark. 2020, Cochrane; Hakozaki ve ark. 2002, Br J Dermatol; Navarrete-Solís ve ark. 2011, Dermatol Res Pract |
| `skin_step_azelaic` | Azelaik asit | Aknede ve koyu lekelerde çalışılmış; kozmetik yüzdelerde kanıt sınırlı. Gebelik planında da kullanılabildiği düşünülen az sayıdaki aktiften biri. Akşam, haftada birkaç gece başla. | sınırlı | Liu ve ark. 2020, Cochrane; AAD, Is any acne treatment safe to use during pregnancy? |
| `skin_step_salicylic` | Salisilik asitli temizleyici | Gözenekleri temizlemeye yardımcı olan, yağda çözünen bir asit; kozmetik ürünlerde en çok yüzde 2 bulunur. Durulanan temizleyici biçimi daha nazik; haftada birkaç kezle başla. | sınırlı | Liu ve ark. 2020, Cochrane; SCCS 2023 ve AB Kozmetik Tüzüğü Ek III |
| `skin_step_aha` | AHA (glikolik ya da laktik asit) | Ölü deriyi nazikçe çözen asitler; koyu leke ya da pürüzlü görünen ciltte haftada 1–2 gece yeter. Güneşe duyarlılığı artırır, sabah güneş koruyucu şart. Hassas ciltte önerilmez. | sınırlı | Liu ve ark. 2020, Cochrane; SCCNFP 2004, AHA görüşü |
| `skin_tip_hands_off` | Ellerini yüzünden uzak tut | Sivilceleri sıkmak iyileşmeyi geciktirir, iz ve koyu leke riskini artırır. Gün içinde yüzüne dokunmak da alevlenmeyi tetikleyebilir. | uzman görüşü | AAD, Acne: Tips for managing; Türk Dermatoloji Derneği, akne broşürü |
| `skin_tip_hair_oily` | Saç ve yüz | Saçın yağlıysa daha sık yıkamak sorun değil; saçı ve saç ürünlerini yüzünden uzak tut, alın ve yanaklarda sivilceyi tetikleyebilir. | uzman görüşü | AAD, Acne: Tips for managing; NHS, Acne |
| `skin_ttc_niacinamide_note` | — | Niasinamid için gebelikte bilinen bir uyarı kaynaklarımızda yok; yine de doktoruna sorabilirsin. | — | — |

Editör notları:
- `skin_step_sunscreen` "güçlü": kurum önerisi (AAD) + randomize çalışma izlemi (Green 2011, melanom) + TDD broşürü
  (günlük SPF 30, non-komedojenik). Lekelerin güneşle koyulaşması iddiası kaynak doğrulanamadığı için metne
  alınmadı.
- `skin_step_niacinamide`: Cochrane'de nikotinamid-klindamisin karşılaştırmaları düşük kesinlikte ve küresel
  değerlendirme ölçülmemiş; pigmentasyon çalışması (Hakozaki 2002) üretici çalışanlarınca yapılmış. "Sınırlı"
  bu yüzden.
- `skin_step_azelaic`: Cochrane'deki orta kesinlikli karşılaştırmalar reçeteli yüzde 15–20 ürünlere aittir;
  kozmetik yüzdeler için kanıt yok denecek kadar az. Kart bu farkı söyler.
- `skin_step_salicylic`: AB Ek III üst sınırı (leave-on yüzde 2, durulanan saç ürünü yüzde 3, vücut losyonu
  yüzde 0,5; 3 yaş altı yok) ikincil düzenleyici özetlerden doğrulandı; resmi metin okunmadı (E.25).
- `skin_step_aha`: SCCNFP tüketici ürünlerinde glikolik asit en çok yüzde 4 (pH ≥ 3,8), laktik asit yüzde 2,5
  (pH ≥ 5) ve UV uyarısı önerir; bu sayılar kullanıcıya gösterilmez, yalnızca "güneşe duyarlılık" uyarısı
  metne girer.
- Gebelik planında AHA rutinden **çıkarılmaz**: MotherSafe (2021) glikolik asidi gebelikte güvenli sayar. Hassas
  ciltte zaten yok.

### 3.5 Gebelik planı açıkken kaçınılacaklar

Kural: `Goal.TTC` açık **ya da** `pregnantOrBreastfeeding` işaretliyken aşağıdakiler rutinden çıkarılır
(zaten rutinde olmayan reçeteli ürünler de dâhil açıkça listelenir) ve `skin_avoid_*` uyarı bandı
(`UyariBandi`) görünür. Güvenlik uyarıları kanıt düzeyinden bağımsız gösterilir (`evidence_safety_note`).
Ayla ilaç bıraktırmaz; "doktorunla konuş" der.

| Anahtar | Başlık | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `skin_avoid` | Gebelik planında çıkardıklarımız | Hamile kalmayı planladığın için aşağıdaki içerikler rutinde yok. Çoğunda gebelikte risk biliniyor ya da güvenlik verisi yetersiz; kullanıyorsan doktoruna söyle. | — | — |
| `skin_avoid_retinoids` | Retinol ve retinoidler | Retinol, retinal ve reçeteli retinoidler (tretinoin, adapalen, tazaroten, izotretinoin). Ağızdan izotretinoin bebeğe ciddi zarar verir; sürülenler de gebelikte bırakılır. | güçlü | AAD, Is any acne treatment safe to use during pregnancy?; MotherSafe 2021 |
| `skin_avoid_hydroquinone` | Hidrokinon | Leke açıcı bir ilaç etken maddesi; ciltten yüksek oranda emildiği için gebelikte kaçınılması öneriliyor. Kozmetikte bulunmaz; yalnızca hekim kontrolünde kullanılır. | uzman görüşü | MotherSafe 2021; AB Kozmetik Tüzüğü Ek II/1339 |
| `skin_avoid_salicylic` | Yüksek yüzdeli salisilik asit | Durulanan düşük yüzdeli temizleyiciler sınırlı süre kullanılabilir sayılıyor. Yüksek yüzdeli ürünler, kimyasal peeling ve ağızdan alınan salisilatlar için doktoruna danış. | uzman görüşü | AAD, Is any acne treatment safe to use during pregnancy?; MotherSafe 2021 |
| `skin_avoid_essential_oils` | Uçucu yağlar | Adaçayı (clary sage) gibi bazı uçucu yağlar rahmi uyarabilir; gebelikte güvenlikleri yeterince çalışılmamış. Seyreltilmeden sürülmez; kullanmadan önce ebene ya da doktoruna sor. | uzman görüşü | NHS Lanarkshire, Aromatherapy Guideline for Midwives 2022 |
| `skin_avoid_rx` | Reçeteli akne ve tüylenme ilaçları | Ağızdan antibiyotikler, hormon ilaçları ve antiandrojenler gebelikte özel değerlendirme gerektirir. Kullanıyorsan bırakmadan, düşünüyorsan başlamadan önce doktorunla konuş. | uzman görüşü | AAD, Is any acne treatment safe to use during pregnancy?; Türk Dermatoloji Derneği, hirsutizm broşürü |
| `skin_avoid_kept` | Gebelik planında kalanlar | Nazik temizleyici, nemlendirici, güneş koruyucu ve azelaik asit rutinde kalır; gebelikte kullanılabildikleri düşünülüyor. Her yeni ürünü yine de doktoruna ya da ebene sorabilirsin. | uzman görüşü | AAD, Is any acne treatment safe to use during pregnancy?; MotherSafe 2021 |

Dayanak notları (ekip için):
- **Retinol (kozmetik):** MotherSafe kozmetikteki retinol miktarını "önemsiz" sayar; AAD kozmetik retinolü ele
  almaz ama tüm reçeteli retinoidlerde bırakmayı önerir. Ayla ihtiyat ilkesiyle kozmetik retinolü de çıkarır;
  metin "öneriliyor" der, "zarar verir" demez. Çip "güçlü" izotretinoin/tazaroten uyarısına aittir.
- **Spironolakton, siproteron, finasterid:** TDD hirsutizm broşürü "bu ilaçları kullanan kadınlar hamilelikten
  kaçınmalıdır" der; AAD spironolaktonu gebelikte "asla" listesine koyar. Ayla ilaç adı vermeden `skin_avoid_rx`
  ile doktora yönlendirir; ilaç listesine kullanıcı kendi eklediyse hatırlatma motoru değişmez.
- **Oral tetrasiklinler:** AAD 15. gebelik haftasından önce bırakılmasını söyler; Ayla hafta vermez.
- **Lazer ve ışık:** AAD gebelikte lazeri "görece güvenli" sayar, MotherSafe lazer epilasyonu güvenli sayar;
  4.2'de bilgi olarak verilir, tedavi önerisi değildir.
- **Benzoil peroksit, topikal klindamisin:** gebelikte güvenli sayılır (AAD) ama eczane/reçete ürünü olduğu
  için Ayla'da zaten yoktur; listeye alınmaz.

### 3.6 Hassas cilt, yama testi ve alerjen notları

| Anahtar | Başlık | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `skin_allergen` | Alerji ve hassasiyet | Parfüm ve uçucu yağlar en sık alerjen gruplarından. Avrupa kurallarına göre belli kokulu maddeler etikette tek tek yazılır; kokusuz ürün seçmek hassas ciltte işi kolaylaştırır. | uzman görüşü | AB Tüzüğü 2023/1545 (koku alerjenleri etiketi); AAD, How to test skin care products before use |
| `skin_allergen_reaction` | Tepki olursa | Yanma, kaşıntı, şişlik ya da döküntü olursa ürünü nazikçe yıka ve bir daha kullanma; soğuk kompres ya da vazelin rahatlatabilir. Geçmiyorsa dermatoloğa görün. | uzman görüşü | AAD, How to test skin care products before use |

Yama testi metni `skin_routine_rule_patch` (3.1) her yeni kart açılışında bir kez gösterilir; hassas ciltte
"Başlarken" bölümünün ilk maddesidir. Not: AB 2023/1545 ile etikette ayrı yazılması zorunlu koku alerjeni sayısı
26'dan 80'in üzerine çıktı (31 Temmuz 2026'dan itibaren pazara arzda zorunlu); Türkiye Kozmetik Yönetmeliği AB
eklerini izler, bu kısım kullanıcıya sayı olmadan verilir (E.27, E.26).

## 4. Saç ve tüylenme

### 4.1 Saç bakımı ve incelme

| Anahtar | Başlık | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `skin_hair_gentle` | Saçına nazik | Sıkı at kuyruğu, topuz ve örgü saçı kökünden çeker; sık yapılırsa kalıcı incelmeye yol açabilir. Saçı gevşek topla, modelleri değiştir, ısı ve kimyasal işlemi azalt. | uzman görüşü | AAD, Hairstyles that pull can lead to hair loss |
| `skin_hair_wash` | Yıkama | Saç derin yağlanıyorsa daha sık yıkamak sorun değil; saç ve kafa derisi ürünlerini yüzden uzak tut. | uzman görüşü | AAD, Acne: Tips for managing |
| `skin_hair_thinning` | Saç incelmesi | PCOS’ta saç incelmesi görülebilir; reçeteli seçenekler var ama biz önermiyoruz. Dökülme seni endişelendiriyorsa doktoruna git; saç kliniğinden önce nedenini öğrenmek önemli. | güçlü (NHS rehberi) | NHS, Hair loss; NHS, Polycystic ovary syndrome (PMOS) |

Not: Minoksidil (NHS: kadın tipi saç dökülmesinde kullanılabilir; finasterid kadınlarda kullanılmaz) tedavi
olduğu için metne girmez. Kadın tipi saç dökülmesinde PCOS'a özgü kanıt 2023 kılavuzunda da sınırlıdır; bu bölüm
kasıtlı olarak kısa tutuldu.

### 4.2 Tüy alma yöntemleri (bilgi, tedavi önerisi değil)

| Anahtar | Başlık | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|---|
| `skin_hair_removal` | Tüy alma yöntemleri | Hangi yöntem sana uyar, sen seçersin. Burada tedavi değil bilgi var; her yöntemin cilde etkisi farklı. Tüylenmenin nedenini araştırmak doktorunun işi. | — | — |
| `skin_hair_removal_shave` | Tıraş | Hızlı ve ucuz. Tıraş tüyü kalınlaştırmaz, hızlandırmaz ya da büyüme süresini uzatmaz; bu yaygın bir yanılgı. | uzman görüşü | Martin ve ark. 2018, J Clin Endocrinol Metab (Endocrine Society) |
| `skin_hair_removal_wax` | Ağda, cımbız ve epilatör | Tüyü kökünden alır. Kıl kökü iltihabı (folikülit) ve özellikle koyu tenlerde koyulaşma yapabilir; tahriş olan bölgede ara ver. | uzman görüşü | Martin ve ark. 2018, J Clin Endocrinol Metab (Endocrine Society) |
| `skin_hair_removal_cream` | Tüy dökücü krem ve ağartma | Kremler tüyü yüzeyden çözer, tahriş yapabilir; önce küçük bir alanda dene. Ağartma tüyü almaz, görünürlüğünü azaltır. | uzman görüşü | Martin ve ark. 2018 (Endocrine Society); Türk Dermatoloji Derneği, hirsutizm broşürü |
| `skin_hair_removal_laser` | Lazer ve IPL | 2023 kılavuzu: tüylenmede lazer ve ışık tedavileri düşünülebilir, deneyimli uygulayıcıda. Tıbbi ya da kozmetik bir işlemdir; birden çok seans gerekir, sonuç kişiye göre değişir. | güçlü (öneri); çalışmalar küçük | Teede ve ark. 2023, Hum Reprod; Teede ve ark. 2024, Med J Aust; Tan ve ark. 2024, JAMA Dermatol; NHS, Hirsutism |
| `skin_hair_removal_laser_skin` | Ten ve tüy rengi | Koyu tüy ve açık tende en iyi yanıt alınır. Koyu tende uzun dalga boylu cihaz ve soğutma gerekir; beyaz ya da sarı tüyde ışık işe yaramaz, elektroliz düşünülür. | uzman görüşü | Martin ve ark. 2018, J Clin Endocrinol Metab (Endocrine Society) |
| `skin_hair_removal_paradox` | Nadir bir etki | Yüzde lazer sonrası nadiren tüylenmenin artması (paradoksal hipertrikoz) görülebilir; Akdeniz ve Orta Doğu kökenli kadınlarda daha sık bildirilmiş. Uygulayıcınla konuş. | uzman görüşü | Martin ve ark. 2018, J Clin Endocrinol Metab (Endocrine Society) |
| `skin_hair_removal_pregnancy` | Gebelikte | Tüy dökücü krem ve lazer epilasyonun bebeğe zarar vermediği düşünülüyor; yine de uygulayıcıya gebe olduğunu söyle. | uzman görüşü | MotherSafe 2021 |

Dayanak notları:
- 2023 kılavuzu (MJA özeti): "Mechanical laser and light therapies should be considered for reducing hirsutism"
  (kanıta dayalı öneri), "ideally delivered by experienced practitioners" (uzlaşı önerisi). Tan 2024 sistematik
  derlemesi: 6 çalışma, 423 kadın; aleksandrit lazer IPL'den etkili; koyu tenlerde veri az; kesinlik düşük.
  Bu yüzden "güçlü (öneri); çalışmalar küçük".
- Endocrine Society 2018: fotoepilasyon kumral/kahverengi/siyah tüyde, elektroliz beyaz/sarı tüyde; koyu tende
  Nd:YAG ya da diyot lazer + cilt soğutması; Akdeniz/Orta Doğu kökenli kadınlarda yüz lazerinde paradoksal
  hipertrikoz uyarısı. Bunlar "suggest" düzeyinde (zayıf öneri) olduğu için "uzman görüşü".
- Seans sayısı (TDD: 4–6 haftada bir 6–8 seans) kullanıcıya verilmez; "birden çok seans" yeter.
- Eflornitin kremi ve hormonal tedaviler (COCP, antiandrojen) reçeteli olduğu için yalnızca "doktoruna danış".
- Lazer/IPL'nin Türkiye'de kim tarafından uygulanabileceğine dair mevzuat bu oturumda doğrulanmadı; metin bu
  yüzden "tıbbi ya da kozmetik işlem, deneyimli uygulayıcı" der, yer ya da unvan saymaz.

## 5. Ne zaman dermatoloğa ya da doktora?

Ton: "geç kalmayayım", yargısız. Her madde "doktor notuna ekle" düğmesiyle doktor raporuna gider. Aciliyet sözcükleri
yalnızca kaynağın söylediği yerde ("gecikmeden", "aynı gün").

| Anahtar | Başlık (≤ 40) | Metin (≤ 180) | Aciliyet | Kanıt | Kaynak |
|---|---|---|---|---|---|
| `skin_doctor` | Ne zaman doktora? | Erken görünmek iz kalmasını ve geç kalmayı önler. Aşağıdakilerden biri varsa randevu almak iyi bir fikir; seçtiklerini doktor notuna ekleyebilirsin. | — | — | — |
| `skin_doctor_virilization` | Hızla artan tüylenme, kalınlaşan ses | Yüz ve vücutta birden çıkan kalın koyu tüyler, sesin kalınlaşması ya da kasların belirgin büyümesi hızlı bir hormon değişimine işaret edebilir; gecikmeden doktora başvur. | gecikmeden | güçlü (NHS rehberi; kılavuz) | NHS, Hirsutism; Martin ve ark. 2018 (Endocrine Society); Türk Dermatoloji Derneği, hirsutizm broşürü |
| `skin_doctor_acne_severe` | Derin, ağrılı sivilceler ya da iz | Deri altında ağrılı şişlikler, iz bırakan sivilceler ya da kendi bakımına rağmen düzelmeyen akne için dermatoloğa görün; erken tedavi izi önler. | yakın zamanda | güçlü (NHS rehberi) | NHS, Acne; Türk Dermatoloji Derneği, akne broşürü |
| `skin_doctor_acne_mood` | Akne ruh hâlini etkiliyorsa | Akne kaygı, keyifsizlik ya da insanlardan uzaklaşmaya yol açıyorsa bu da doktora gitmek için yeterli bir neden. | yakın zamanda | güçlü (NHS rehberi) | NHS, Acne |
| `skin_doctor_infection` | Sıcak, şiş, kızarık cilt | Sivilce bölgesi sıcak, şiş ve yaygın kızarıksa enfeksiyon olabilir; aynı gün doktora ya da eczacıya danış. | aynı gün | güçlü (NHS rehberi) | NHS, Acne |
| `skin_doctor_acanthosis` | Boyun, koltuk altı, kasıkta koyu alanlar | Yeni çıkan ya da yayılan kadifemsi koyu, kalın alanlar insülin direnci ya da prediyabetin işareti olabilir; bazen başka nedenler de olur. Bir sonraki muayenede doktoruna göster. | yakın zamanda | güçlü (NHS/AAD rehberi) | AAD, Acanthosis nigricans: Overview; NHS, Acanthosis nigricans |
| `skin_doctor_hair_loss` | Saç dökülmesi | Dökülme seni endişelendiriyorsa ya da yer yer açılmalar varsa doktoruna git; saç kliniğinden önce nedenini öğrenmek önemli. Bazı dökülmeler geç kalınırsa kalıcı olabilir. | yakın zamanda | güçlü (NHS rehberi) | NHS, Hair loss; AAD, Hairstyles that pull can lead to hair loss |
| `skin_doctor_mole` | Değişen ben ya da leke | Bir ben ya da leke boyut, biçim ya da renk değiştiriyor, kaşınıyor ya da kanıyorsa dermatoloğa görün. Asimetri, düzensiz kenar, birden çok renk ve 6 mm üstü boyut uyarıcıdır. | yakın zamanda | güçlü (AAD rehberi) | AAD, What to look for: ABCDEs of melanoma |
| `skin_doctor_pregnancy` | Gebeysen ya da planlıyorsan | Cilt için kullandığın her ürünü ve ilacı doktoruna ya da dermatoloğuna söyle; bazıları gebelikte uygun değil. | — | uzman görüşü | Türk Dermatoloji Derneği, akne broşürü; AAD, Is any acne treatment safe to use during pregnancy? |

Dayanak notları:
- NHS Hirsutism: "acil randevu" ölçütleri yüz ve vücutta aniden çıkan kalın koyu tüy, sesin kalınlaşması, kasların
  büyümesi; "acil olmayan" ölçüt kalın koyu tüy + düzensiz adet. Endocrine Society: ani başlangıç ya da hızlı
  ilerleme hiperandrojenik bozukluğu, virilizasyon bulguları (klitoromegali, ses kalınlaşması, kas artışı)
  androjen salgılayan tümör olasılığını düşündürür. TDD: "aniden oluşan ... kıllanma" ve adet düzensizliğinde
  dermatolog. Klitoromegali kullanıcı metnine alınmadı (öz-değerlendirmesi güç; doktor muayenesi konusu).
- NHS Acanthosis nigricans sayfası nadir kanser ilişkisini anar; kullanıcı metninde "bazen başka nedenler de olur"
  ile yetinildi (korkutmadan doktora yönlendirme). Kiloya atıf yok (NHS'in "en sık neden obezite" cümlesi dil
  kuralı gereği alınmadı; insülin direnci–prediyabet ilişkisi AAD'den).
- NHS Hair loss sayfası tek açık ölçüt olarak "endişeleniyorsan" der ve ticari klinikten önce hekim önerir;
  "kalıcı olabilir" AAD traksiyon alopesisi sayfasından.

## 6. Fotoğraf takibi için klinik not

Fotoğraf **tanı aracı değildir**; görüntü analizi yapılmaz, hiçbir yere gönderilmez. Amaç kullanıcının değişimi
görmesi ve muayenede doktoruna göstermesidir. Tutarlılık ipuçları St Vincent's Hospital Melbourne hasta
rehberinden; "aynı saat/açı/makyajsız/filtresiz" maddeleri ürün önerisidir (uzman görüşü).

| Anahtar | Metin (≤ 180) | Kanıt | Kaynak |
|---|---|---|---|
| `skin_photo_intro` | Fotoğraflar yalnızca bu cihazda şifreli saklanır, galeriye düşmez. Tanı aracı değildir; değişimi görmene ve doktoruna göstermene yarar. | — | — |
| `skin_photo_tip_light` | Aynı yerde, gün ışığında ya da aynı tavan lambasıyla çek; flaş ve tek yönlü lamba gölge yapar. | uzman görüşü | St Vincent's Hospital Melbourne, Photographing dermatological conditions |
| `skin_photo_tip_distance` | Telefonu cilde paralel tut ve en az 30–40 cm uzaktan çek; yakın plan için bir de karış mesafeden. | uzman görüşü | St Vincent's Hospital Melbourne, Photographing dermatological conditions |
| `skin_photo_tip_consistency` | Her seferinde aynı açı, aynı mesafe ve günün aynı saati; makyajsız ve ürün sürülmemiş ciltle. Filtre kullanma. | uzman görüşü | Ürün önerisi (karşılaştırılabilirlik) |
| `skin_photo_tip_helper` | Mümkünse fotoğrafı bir başkası çeksin; sırt, boyun ve saç derisi için bu çok kolaylaştırır. | uzman görüşü | St Vincent's Hospital Melbourne, Photographing dermatological conditions |
| `skin_photo_not_diagnosis` | Fotoğraf tanı koymaz; bir değişiklik fark edersen doktoruna göster. | — | — |

Uygulama notu: karşılaştırma görünümünde iki fotoğraf yan yana, tarih ve (varsa) o günün rutin işaretleriyle;
"iyileşti / kötüleşti" gibi otomatik yargı yok. Saç derisi ve vücut fotoğrafı için aynı ipuçları geçerli.

## 7. Kanıt özeti satırları (pcos-kanit-ozeti.md'ye eklenecek: "MVP 3: Cilt ve saç (#25)")

| Konu | Uygulamadaki kullanım | Dayanak | Düzey |
|---|---|---|---|
| Cilt bulguları PCOS'un parçası | `skin_intro` | NHS PMOS sayfası (son inceleme 30 Haziran 2026): yağlı cilt ve akne, hirsutizm ya da saç dökülmesi, boyun/koltuk altında koyu kalın alanlar; 2023 kılavuzu: hirsutizm tek başına hiperandrojenizmi öngörür | güçlü |
| Nazik temizlik, non-komedojenik ürün, sıkmama | `skin_step_cleanser*`, `skin_tip_hands_off`, `skin_tip_hair_oily` | AAD "Acne: Tips for managing"; TDD akne broşürü; NHS Acne | uzman görüşü |
| Nemlendirici | `skin_step_moisturizer` | TDD akne broşürü; AAD; Araviiskaia ve Dréno 2016 (dermokozmetiklerde kontrollü çalışma eksikliği) | uzman görüşü |
| Güneş koruyucu SPF 30+ geniş spektrum | `skin_step_sunscreen*` | AAD Sunscreen FAQs; Green ve ark. 2011 J Clin Oncol (günlük güneş koruyucu, melanom HR 0,50; invaziv 0,27); TDD akne broşürü | güçlü |
| Niasinamid | `skin_step_niacinamide` | Liu ve ark. 2020 Cochrane (nikotinamid vs klindamisin, düşük kesinlik); Hakozaki ve ark. 2002 Br J Dermatol (yüzde 5, 4 hafta, pigmentasyon; üretici çalışması); Navarrete-Solís ve ark. 2011 (yüzde 4 vs hidrokinon, melazma) | sınırlı |
| Azelaik asit | `skin_step_azelaic` | Liu ve ark. 2020 Cochrane (reçeteli yüzdelerde tretinoin ile benzer, benzoil peroksitten zayıf; orta kesinlik); AAD gebelik sayfası ("güvenli olduğu düşünülüyor") | sınırlı (kozmetik yüzde) |
| Salisilik asit (durulanan, ≤ yüzde 2) | `skin_step_salicylic` | Liu ve ark. 2020 Cochrane (düşük kesinlik); SCCS/1646/22 ve AB Ek III sınırları | sınırlı |
| AHA | `skin_step_aha` | Liu ve ark. 2020 Cochrane (peeling karşılaştırmaları, düşük kesinlik); SCCNFP/0799/04 (UV duyarlılığı artar) | sınırlı |
| Yama testi, alerjen | `skin_routine_rule_patch`, `skin_allergen*` | AAD "How to test skin care products"; AB Tüzüğü 2023/1545 | uzman görüşü |
| Gebelikte kaçınılacaklar | `skin_avoid_*` | AAD gebelik sayfası (izotretinoin, tazaroten, spironolakton asla; tretinoin/adapalen bırakılır; tetrasiklin 15. haftadan önce); MotherSafe 2021 (hidrokinon kaçın, retinol kozmetikte önemsiz, salisilik/glikolik/güneş koruyucu/lazer güvenli); NHS Lanarkshire aromaterapi rehberi 2022 | güçlü (güvenlik); diğerleri uzman görüşü |
| Saça nazik bakım | `skin_hair_gentle`, `skin_hair_wash` | AAD traksiyon alopesisi sayfası | uzman görüşü |
| Saç incelmesinde hekim | `skin_hair_thinning`, `skin_doctor_hair_loss` | NHS Hair loss; NHS PMOS | güçlü (NHS rehberi) |
| Tüy alma yöntemleri | `skin_hair_removal_*` | Martin ve ark. 2018 Endocrine Society (zayıf öneriler); TDD hirsutizm broşürü; MotherSafe 2021 | uzman görüşü |
| Lazer/ışık | `skin_hair_removal_laser` | Teede 2023 (MJA özeti: "should be considered", deneyimli uygulayıcı); Tan ve ark. 2024 JAMA Dermatol; NHS Hirsutism | güçlü (öneri); çalışmalar küçük |
| Hızlı virilizasyon | `skin_doctor_virilization` | NHS Hirsutism (acil ölçütler); Martin ve ark. 2018; TDD | güçlü (NHS rehberi; kılavuz) |
| Akne için hekim ölçütleri | `skin_doctor_acne_*`, `skin_doctor_infection` | NHS Acne | güçlü (NHS rehberi) |
| Akantozis nigrikans | `skin_acanthosis_help`, `skin_doctor_acanthosis` | AAD (prediyabet işareti); NHS (nedenler: tip 2 diyabet, PMOS, hipotiroidi, Cushing, ilaçlar; nadiren kanser) | güçlü (NHS/AAD rehberi) |
| Ben/leke değişimi | `skin_doctor_mole` | AAD ABCDE | güçlü (AAD rehberi) |
| Fotoğraf tutarlılığı | `skin_photo_tip_*` | St Vincent's Hospital Melbourne hasta rehberi | uzman görüşü |

## 8. Kaynakça ve doğrulama durumu

Doğrulama 12 Ekim 2026 oturumunda web aramasıyla yapıldı. Etiketler plan-icerigi.md ile aynı: **doğrulandı**
(başlık + kurum/dergi + yıl ve ilgili içerik okundu), **kısmen** (varlık doğrulandı; içerik ya da künye ayrıntısı
bellekten ya da ikincil kaynaktan), **doğrulanamadı**. Doğrulanamayan kaynaklara dayanan hiçbir iddia kullanıcı
metnine girmedi. Sayım: 33 kaynak → 27 doğrulandı, 5 kısmen, 1 doğrulanamadı.

**Kılavuzlar**
1. Teede HJ, Tay CT, Laven J ve ark.; International PCOS Network. Recommendations from the 2023 international
   evidence-based guideline for the assessment and management of polycystic ovary syndrome. Hum Reprod.
   2023;38(9):1655–1679. doi:10.1093/humrep/dead156. — doğrulandı (künye; hirsutizm önerileri 2 numaralı özetten)
2. Teede HJ, Mousa A, Tay CT ve ark. Summary of the 2023 international evidence-based guideline for the assessment
   and management of polycystic ovary syndrome: an Australian perspective. Med J Aust. 2024;221(7).
   doi:10.5694/mja2.52432. — doğrulandı (COCP; lazer/ışık "should be considered", deneyimli uygulayıcı;
   antiandrojen etkili kontrasepsiyonla; siproteron ikinci basamak; akne/hirsutizm tedavisi yaşam kalitesi)
3. International Evidence-based Guideline for the Assessment and Management of Polycystic Ovary Syndrome 2023
   (Monash University, tam kılavuz). — kısmen (Bölüm 1 okundu: 1.1.1 düzensiz döngü tanımı, 1.1.5 ovulatuvar
   disfonksiyon, 1.3.1 hirsutizm hiperandrojenizmi öngörür; Bölüm 4 hirsutizm/akne/alopesi ayrıntıları ve mFG
   eşiği okunamadı)
4. Martin KA, Anderson RR, Chang RJ ve ark. Evaluation and Treatment of Hirsutism in Premenopausal Women: An
   Endocrine Society Clinical Practice Guideline. J Clin Endocrinol Metab. 2018;103(4):1233–1257.
   doi:10.1210/jc.2018-00241. — doğrulandı (1.1, 1.3, 3.1, 3.5, 4.1, 4.2, 4.3; virilizasyon; tıraş; ağda;
   depilatuvar)
5. Tan K, Coster T, Mousa A ve ark. Laser and light-based therapies for hirsutism management in women with
   polycystic ovarian syndrome: a systematic review. JAMA Dermatol. 2024;160(7):746–757.
   doi:10.1001/jamadermatol.2024.0623. — doğrulandı
6. Reynolds RV ve ark. Guidelines of care for the management of acne vulgaris. J Am Acad Dermatol. 2024;
   doi:10.1016/j.jaad.2023.12.017 (90(5):1006.e1–1006.e30). — kısmen (başlık, dergi, DOI, ilk yazar ve güçlü
   öneriler doğrulandı: benzoil peroksit, topikal retinoid, topikal antibiyotik, doksisiklin; cilt/sayfa
   bellekten; kılavuz cilt bakımı ya da reçetesiz ürün önerisi içermiyor)
7. Liu H, Yu H, Xia J ve ark. Topical azelaic acid, salicylic acid, nicotinamide, sulphur, zinc and fruit acid
   (alpha-hydroxy acid) for acne. Cochrane Database Syst Rev. 2020;5:CD011368. — doğrulandı

**Kurum sayfaları**
8. AAD. Acne: Tips for managing. aad.org. — doğrulandı
9. AAD. Is any acne treatment safe to use during pregnancy? aad.org. — doğrulandı
10. AAD. How to test skin care products before use. aad.org. — doğrulandı
11. AAD. Sunscreen FAQs. aad.org. — doğrulandı
12. AAD. Hairstyles that pull can lead to hair loss. aad.org. — doğrulandı
13. AAD. Acanthosis nigricans: Overview. aad.org. — doğrulandı
14. AAD. What to look for: ABCDEs of melanoma. aad.org. — doğrulandı
15. Türk Dermatoloji Derneği. Hasta Bilgilendirme Broşürü: Hirsutizm (Kıllanma Artışı). turkdermatoloji.org.tr
    (tarihsiz). — doğrulandı
16. Türk Dermatoloji Derneği. Hasta Bilgilendirme Broşürü: Akne Hastalığında Tetikleyici Faktörler.
    turkdermatoloji.org.tr (tarihsiz). — doğrulandı
17. NHS. Hirsutism. nhs.uk/conditions/hirsutism. — doğrulandı (sayfa artık "PMOS" adını kullanıyor)
18. NHS. Acne. nhs.uk/conditions/acne. — doğrulandı
19. NHS. Acanthosis nigricans. nhs.uk/conditions/acanthosis-nigricans. — doğrulandı
20. NHS. Hair loss. nhs.uk/conditions/hair-loss. — doğrulandı
21. NHS. Polycystic ovary syndrome (PMOS). nhs.uk/conditions/polycystic-ovary-syndrome-pcos (son inceleme
    30 Haziran 2026). — doğrulandı
22. MotherSafe, Royal Hospital for Women (NSW Health). Skin Care, Hair Care and Cosmetic Treatments in Pregnancy
    and Breastfeeding. Nisan 2021. — doğrulandı
23. NHS Lanarkshire, University Hospital Wishaw. Aromatherapy Guideline for Midwives. Şubat 2022. — doğrulandı
    (içerik ebe uygulamasına yönelik; gebelikte seyreltme sınırı, clary sage rahmi uyarır)
24. St Vincent's Hospital Melbourne. Photographing dermatological conditions. svhm.org.au. — doğrulandı

**Mevzuat ve bilimsel komiteler**
25. SCCS. Opinion on salicylic acid (SCCS/1646/22, nihai görüş 9 Haziran 2023); AB Kozmetik Tüzüğü (EC) 1223/2009
    Ek III salisilik asit kısıtı (Tüzük (EU) 2021/850 ile): leave-on yüzde 2, vücut losyonu yüzde 0,5, durulanan
    saç ürünü yüzde 3, 3 yaş altı yok. — kısmen (ikincil düzenleyici özetlerden; resmi metin okunmadı)
26. AB Kozmetik Tüzüğü Ek II/1339: hidrokinon kozmetikte yasak (yapay tırnak sistemleri istisnası Ek III/14);
    Türkiye Kozmetik Yönetmeliği AB eklerini izler (TİTCK duyurusunda Ek II satır numaraları AB ile örtüşüyor;
    hidrokinon satırı görülmedi). — kısmen
27. Tüzük (EU) 2023/1545: koku alerjenlerinin etiketlenmesi (56 ek alerjen; eşik leave-on yüzde 0,001, durulanan
    yüzde 0,01; 31 Temmuz 2026 / 31 Temmuz 2028). — doğrulandı (Obelis özeti)
28. SCCNFP. Updated Position Paper Concerning Consumer Safety of Alpha-Hydroxy Acids (SCCNFP/0799/04, 25 Mayıs
    2004); SCCNFP/0370/00. — doğrulandı

**Çalışmalar**
29. Hakozaki T, Minwalla L, Zhuang J ve ark. The effect of niacinamide on reducing cutaneous pigmentation and
    suppression of melanosome transfer. Br J Dermatol. 2002;147(1):20–31. — doğrulandı (yazarlar Procter & Gamble)
30. Navarrete-Solís J ve ark. A double-blind, randomized clinical trial of niacinamide 4% versus hydroquinone 4% in
    the treatment of melasma. Dermatol Res Pract. 2011;2011:379173. — kısmen (başlık, dergi, yıl doğrulandı;
    yazarlar ve makale numarası bellekten)
31. Araviiskaia E, Dréno B. The role of topical dermocosmetics in acne vulgaris. J Eur Acad Dermatol Venereol.
    2016;30(6):926–935. doi:10.1111/jdv.13579. — doğrulandı ("rigorous controlled studies" eksikliği)
32. Green AC, Williams GM, Logan V, Strutton GM. Reduced melanoma after regular sunscreen use: randomized trial
    follow-up. J Clin Oncol. 2011;29(3):257–263. — doğrulandı
33. Bozzo P, Chua-Gocheco A, Einarson A. Safety of skin care products during pregnancy. Can Fam Physician.
    2011;57(6):665–667. — doğrulanamadı (sayfa açılamadı; yalnızca ikincil kaynaklarda anılıyor; hiçbir metin
    buna dayandırılmadı, gebelik maddeleri AAD ve MotherSafe'e dayanır)

## 9. Değerlendirilip alınmayanlar ve reddedilenler (ekip için, kullanıcıya görünmez)

| Aday | Gerekçe | Karar |
|---|---|---|
| Benzoil peroksit | AAD 2024'te güçlü öneri ve gebelikte güvenli; ama eczane/reçete ürünü, tahriş ve giysi beyazlatma; Ayla tedavi önermez | yalnızca "doktoruna danış" |
| Topikal retinoidler (adapalen, tretinoin), retinol | Reçeteli (adapalen bazı ülkelerde reçetesiz); gebelikte bırakılır; kozmetik retinol ihtiyatla çıkarıldı | kaçınılacaklar listesi |
| Çay ağacı yağı | Cochrane tamamlayıcı tedavi derlemesinde düşük kaliteli kanıt (bu oturumda doğrulanmadı); alerjen ve oksidasyon; uçucu yağ | eklenmedi |
| Kükürt, çinko (topikal) | Cochrane 2020 hedefinde ama sonuç verisi okunamadı | eklenmedi |
| Spironolakton, siproteron, finasterid, eflornitin, minoksidil, metformin, COCP | Tedavi; wellness sınırı dışı | yalnızca "doktoruna danış"; gebelik uyarısı `skin_avoid_rx` |
| Hidrokinon, kojik asit, arbutin | Hidrokinon kozmetikte yasak ve gebelikte kaçınılır; kojik/arbutin için kaynak doğrulanmadı | hidrokinon kaçınılacaklar; diğerleri eklenmedi |
| Kimyasal peeling, mikroiğneleme, lazerle leke tedavisi | İşlem; uzman gerektirir | eklenmedi |
| Ferriman-Gallwey puanlama | Klinisyen aracı; öz-puanlama tanı dili yaratır | hesaplanmaz |
| Akne derecelendirme (IGA, Leeds) | Klinisyen aracı | öz-beyan (Yok/Hafif/Orta/Yoğun) |
| Fotoğraftan otomatik "iyileşme" yorumu | Tıbbi cihaz sınırı (MDCG 2019-11); görüntü analizi tanıya yaklaşır | yok |
| Cilt tipi tanım metni ("yıkadıktan bir saat sonra...") | Kaynak doğrulanmadı | yardım metni yalnızca "emin değilsen Normal" |
| Lekelerin güneşle koyulaşması cümlesi | Kaynak doğrulanmadı | güneş koruyucu kartından çıkarıldı |
| Diyet–akne ilişkisi | AAD 2024 kılavuzu diyet için öneri vermedi; beslenme sekmesi ve dil kuralı | eklenmedi |
| Kilo ve akantozis | NHS "en sık neden obezite" der; yeme bozukluğu riski ve dil kuralı | yalnızca insülin direnci/prediyabet ve "doktoruna göster" |

## 10. Uygulama notları (ekip için)

1. **Görünürlük.** Cilt ve saç bölümü kişiselleştirme rızasından bağımsızdır; profil alanları ayrı rıza ile
   saklanır (sağlık verisi). Profil boşsa yalnızca temel ritim (temizleyici, nemlendirici, güneş koruyucu) görünür.
2. **Gebelik planı.** `Goal.TTC` ya da `pregnantOrBreastfeeding` açıkken 3.5 uygulanır; kart listesi
   `skin_avoid*` bandıyla başlar. Önceliği kapatan kullanıcıya rutinin değişeceği tek cümleyle söylenir.
3. **Akne yoğun.** `AcneSelfReport.SEVERE` seçilince aktif kartlar gizlenir, `skin_doctor_acne_severe` rutinin
   üstünde durur; "Doktor notuna ekle" düğmesi görünür.
4. **Kanıt ve kaynak.** Her kartta `KanitCipi` + `plan_source_format`; `skin_review_pending` tüm kartların
   üstünde tek bant olarak durur, uzman onayı alınınca kaldırılır (CHANGELOG'a yazılır).
5. **Hatırlatma.** Sabah/akşam rutin hatırlatmaları `ReminderScheduler`'a yeni tür olarak eklenir (WorkManager
   yok); "Gizli bildirimler" ayarına uyar; metinler `skin_notif_*`. Bir adım işaretlenmezse hiçbir "kaçırdın"
   dili yok; haftalık sayı nötr (`skin_routine_week_count`).
6. **Adım işaretleme.** Biçim morfu `Shape.kt` polar biçimleriyle; `LocalReducedMotion` açıkken anında.
7. **Fotoğraf.** ADR-0010 şifreli saklama; `FLAG_SECURE`; dışa aktarma ve tam silme kapsar; karşılaştırma
   görünümünde otomatik yorum yok.
8. **Doktor notu.** 5. bölümde seçilen maddeler, akantozis öz-beyanı ve tüylenme/saç öz-beyanı doktor raporuna
   "Cilt ve saç" başlığıyla gider; puan ya da derece yazılmaz.
9. **Ekran görüntüleri.** Yeni durumlar `EkranGoruntuleriTest`'e ve %200 yazı (`y0N`) serisine: profil, sabah
   rutini (normal / hassas / gebelik planı), akşam rutini (akne hafif / leke / yoğun), kaçınılacaklar bandı, tüy
   alma bilgisi, "ne zaman doktora", fotoğraf karşılaştırma.
10. **Dil denetimi.** `skin_` önekli metinler için plan-icerigi F.7'deki yasaklı sözcük testi genişletilir:
    kusur, kusursuz, pürüzsüz, lekesiz, mükemmel, kilo.
11. **XML.** `%` yerine "yüzde"; kesme işareti `\'` ya da `&#8217;` (mevcut `strings_plan.xml` ikincisini kullanır;
    scratchpad taslağı `\'` ile yazıldı, birleştirmede tek biçime çevrilir).
