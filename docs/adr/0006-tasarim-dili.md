# ADR-0006: Tasarım dili — Ayla tokenları, yaylı hareket, polar biçimler; Material Expressive ertelendi

- Tarih: 2026-10-11 · Durum: Kabul edildi

## Bağlam
Phase 1'de Ayla Tasarım Sistemi (tokens.json, 12 bileşen, 15 ekran taslağı) üretildi: sıcak nötr zemin,
nar birincil, adaçayı/kayısı/lavanta destek, Bricolage Grotesque + Figtree, "hale halkası" imza bileşeni.
Material 3 Expressive bileşenleri (MaterialShapes, LoadingIndicator, FAB menüleri) BOM 2026.09'da mevcut
ancak API'leri bulut oturumunda doğrulanamıyor ve markaya özgü biçimler isteniyor.

## Karar
- Tokenlar birebir `ui/theme`: `Color.kt` (iki tema, her çift ≥ 4,5:1 + `AylaColors` ek rolleri), `Type.kt`
  (değişken font eksenleri `FontVariation`), `Shape.kt` (radius ölçeği + `polar()` GenericShape ifade biçimleri),
  `Motion.kt` (spatial 0,72/380, slow 0,85/110, effect taşmaz, emphasizedDecelerate giriş).
- `MaterialTheme` standart; Expressive `MaterialExpressiveTheme`/`MaterialShapes` **kullanılmaz** (ertelendi).
- Erişilebilirlik sözleşmesi: renk tek başına anlam taşımaz; dokunma hedefi ≥ 48 dp; `LocalReducedMotion`
  (ANIMATOR_DURATION_SCALE == 0) açıkken nefes/morf animasyonları durur; her Canvas bileşeni contentDescription taşır.
- İmza bileşenler Compose Canvas ile elle çizilir (HaleHalkasi, HapPaketi, takvim hücreleri, döngü çubukları).

## Sonuçlar
+ Marka tutarlılığı, sürüm bağımsızlığı, ölçülebilir kontrast.
− Expressive'in hazır hareket şemalarından yararlanılmıyor; MVP 2'de `MotionScheme` ile karşılaştırma yapılacak.

## Alternatifler
Material Expressive tam geçiş (ertelendi), dinamik renk (kapatıldı: faz renkleri sabit kalmalı).
