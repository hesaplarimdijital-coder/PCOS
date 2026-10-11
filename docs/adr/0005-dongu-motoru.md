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
