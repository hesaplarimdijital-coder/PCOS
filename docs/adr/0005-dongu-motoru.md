# ADR-0005: Döngü motoru — ağırlıklı medyan tabanlı aralık tahmini ve güven düzeyi

- Tarih: 2026-10-11 · Durum: Kabul edildi

## Bağlam
PCOS'ta döngüler 21–35 gün normunun dışında ve değişkendir. "Adetin 14 Ekim'de" gibi tek gün tahmini
yanlış ve güven kırıcıdır; aralık + açık güven düzeyi istenir (kullanıcı araştırması, Drip ve Mensinator
incelemesi). Motor saf Kotlin olmalı ki JVM'de hızlı test edilsin.

## Karar (`core/cycle`)
- Döngü = ardışık adet başlangıçları arası; `< 14 gün` otomatik hariç (`IMPLAUSIBLY_SHORT`), kullanıcı
  hariç tutabilir (`USER`). Gelecek tarihli kayıtlar hesaba hiç girmez.
- Tahmin: son 6 tamamlanmış döngü, azalan ağırlık 0,65ⁿ; merkez = ağırlıklı ortalama; yayılım = ağırlıklı
  MAD × 1,25; tabanlar n=1 → 7 gün, n=2 → ≥ 5 gün; sınır 3–21 gün. Pencere: [merkez − yayılım, merkez + 1,5·yayılım]
  (PCOS'ta gecikme kısalmadan olasıdır; asimetri bu yüzden). En erken 15. gün.
- Güven: HIGH (n ≥ 4, ham yayılım ≤ 3 gün, en uzun ≤ 45), MEDIUM (n ≥ 2, yayılım/merkez ≤ 0,25), LOW.
  Pencere geçilmişse `PastWindow`.
- Uyarılar: 60. gün nazik, 90. gün "doktora danış; gebelik olasılığı varsa test" (NHS: 12 hafta kanama yok);
  son 365 günde < 4 adet (takip ≥ 1 yıl) → rahim iç tabakası koruması notu.
- Hap modu (`Pill.kt`): 21/7, 24/4, sürekli 28; hap numarası, plasebo, paket sayısı; tahmin yapılmaz.
- BBT (`OvulationDetector`): 3-üstü-6 kuralı, +0,2 °C üçüncü ölçüm. MVP 3'te arayüze bağlanır.

## Sonuçlar
+ 32 birim testi; aralık + güven kullanıcıya dürüst; hap kullanıcısı yanlış tahminle karşılaşmaz.
− Model istatistiksel, kişisel öğrenme yok (semptom tabanlı tahmin MVP 3 araştırma konusu).
− Eşikler literatürden; klinik doğrulama yapılmadı ve "tıbbi tavsiye değildir" ibaresi zorunlu.

## Alternatifler
Sabit 28 gün (yanlış); yalnızca ortalama (aykırı değerlere duyarlı); makine öğrenmesi (veri yok, açıklanabilirlik düşük).

## Güncelleme (Ekim 2026) — #24: Doğurganlık motoru (LH/BBT)

- Tarih: 2026-10-12 · Durum: Kabul edildi · `Fertility.kt`, `FertilityEngine.kt`, `OvulationDetector.kt`

### Kararlar
- **Yumurtlama yalnızca ısı yükselmesiyle doğrulanır.** LH testi ve servikal mukus kayıt amaçlıdır,
  karara girmez. PCOS'ta bazal LH yüksek olabildiğinden idrar LH testi yanlış pozitif verebilir (2023
  uluslararası kılavuz; test üreticilerinin PCOS uyarısı). Doğrulama `CycleEngine` tahminine karışmaz
  (ayrı modül, ayrı çağrı); yumurtlama tabanlı adet beklentisi penceresi bu sürümde yoktur.
- **Bozuk ölçüm ve akla yatkınlık süzgeci.** `TemperatureReading.disturbed` (Sensiplan "bozucu etkenler":
  ateş, alkol, az/kesintili uyku, geç ölçüm — kullanıcı işaretler) ve 35,0–38,5 °C dışı değerler örtü
  çizgisi ile yükselme hesabına girmez; grafikte içi boş çizilir. Aralık klinik eşik değil, giriş hatası
  korumasıdır (35 °C hipotermi sınırı, 38,5 °C ateş eşiğinin belirgin üstü). Motor ateşi kendiliğinden
  çıkarsamaz.
- **Sensiplan 1. istisna kuralı** uygulanır: üçüncü ölçüm örtü çizgisinin +0,2 °C üstüne çıkmıyorsa ama
  dördüncü ardışık ölçüm de çizginin üstündeyse doğrulama dördüncü ölçümle tamamlanır
  (`OvulationEstimate.confirmedOn`). 2. istisna kuralı (yüksek ölçümlerden birinin çizgiye düşmesi)
  ertelendi (Frank-Herrmann ve ark., Hum Reprod 2007).
- **Döngüye bölme.** Her adet başlangıcı sınırdır (< 14 gün kuralı burada uygulanmaz); ilk adet
  başlangıcından önceki kayıtlar hesaba girmez; son döngü açık uçludur. Gelecek tarihli kayıt ve adet
  başlangıçları atılır; aynı güne iki kayıt varsa listede sonuncusu kazanır (belirleyici).
- **Hap modunda `FertilityOverview.Disabled`**: hiçbir hesap yapılmaz, bölüm arayüzde gizlenir.
- **Notlar ve eşikler** (`FertilityNote`; metinler arayüzde, kanıt etiketiyle):
  - `TOO_FEW_READINGS`: geçerli ölçüm < 9 (= 6 önceki + 3 yüksek, kuralın asgarisi) ve doğrulama yok.
  - `LH_POSITIVE_WITHOUT_RISE`: LH pozitif gününe kadar ≥ 6 geçerli ölçüm, sonrasında ≥ 5 geçerli ölçüm
    ve doğrulama yok. 5 günün dayanağı: LH yükselişinden yumurtlamaya 24–36 saat, ısı yükselmesi 1–2 gün
    sonra, üçüncü yüksek ölçüm pozitiften ≈ 5 gün sonra (Direito ve ark., Fertil Steril 2013). Eksik
    veriyle tetiklenmez ki "LH yanıltıcı" yanlış notu üretilmesin.
  - `RISE_WITHOUT_LH_POSITIVE`: doğrulama var, en az bir negatif LH var, hiç pozitif yok (test zamanlaması).
  - `LONG_LUTEAL`: açık döngüde yumurtlama sonrası > 18 gün ve adet yok. Doğal aile planlamasında gebelik
    olasılığının klasik işaretidir (normal luteal faz 11–17 gün: Lenton ve ark., BJOG 1984; Crawford ve
    ark., Fertil Steril 2017). Tanı değildir; test ve hekim yönlendirmesi arayüz metnindedir.
  - `MANY_DISTURBED`: bozuk ölçüm ≥ 3 ve ≥ %30 — klinik eşik değil, grafik okunabilirliği için ürün sezgisi.
- Kapalı döngülerde luteal faz uzunluğu hesaplanmaz (kısa luteal faz yorumu klinik alana girer).
- Test sayısı 32 → 79.

### Sonuçlar
+ Doğrulama kuralı Sensiplan ile uyumlu; PCOS'a özgü LH uyarısı yanlış pozitif üretmeyecek biçimde kurgulandı.
− Doğrulama en erken üçüncü/dördüncü yüksek ölçümde gelir (geriye dönük); "bugün yumurtluyorsun" denmez.
− Doğum kontrolü için kullanılamaz; her ekranda ibare zorunlu (TİTCK wellness sınırı).
