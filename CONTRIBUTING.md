# Katkı rehberi

Önce [CLAUDE.md](CLAUDE.md)'yi oku: teknik çerçeve, kod kuralları ve klinik/hukuki sınırlar oradadır.
Bu dosya iş akışını özetler.

## İş akışı
1. İş GitHub Issues'tan seçilir (`mvp-1`, `mvp-2` … etiketleri). Yeni fikir önce issue olur.
2. Küçük, konu odaklı commit'ler; mesaj Türkçe ve emir kipinde ("Takvim ekranını ekle").
   Bir issue'yu kapatan commit mesajına `Fixes #N` yazılır.
3. Kullanıcıya görünen her metin `res/values/strings.xml`'e; tarih/büyük harf için Türkçe yerel ayar.
4. Yeni ekran: `XxxScreen` (ViewModel bağlama) + `XxxContent` (durumsuz) ayrımı; ekran görüntüsü
   testine (`app/src/test/.../ekran`) bir görüntü eklenir.
5. Davranış değişikliği testle gelir: döngü motoru `core/cycle/src/test`, saf kurallar
   `app/src/test`.

## CI çıktısını okumak
Her push'ta üç iş çalışır: *Derle ve test et*, *Sürüm derlemesi (R8) ve lint*, *Ekran görüntüleri*.
Derleme hataları, test sayıları, Kotlin ve lint uyarıları GitHub açıklaması (annotation) olarak
yayımlanır:

```bash
gh run list --repo hesaplarimdijital-coder/pcos --limit 3
gh run view <run-id> --repo hesaplarimdijital-coder/pcos --json jobs -q '.jobs[].databaseId'
gh api repos/hesaplarimdijital-coder/pcos/check-runs/<job-id>/annotations
```

Ekran görüntüleri `ekran-goruntuleri` dalına yüklenir:

```bash
git fetch origin ekran-goruntuleri:refs/remotes/origin/ekran-goruntuleri --force
git archive origin/ekran-goruntuleri | tar -x -C /tmp/ekran
```

## Sınırlar
- Tanı, doz önerisi, ilaç etkileşim yorumu eklenmez (TİTCK/MDCG 2019-11 wellness sınırı).
- Üçüncü taraf analitik/reklam SDK'sı eklenmez; veri cihazdan çıkmaz.
- Anahtar dosyaları (`*.jks`, `keystore.properties`) asla commit'lenmez.
