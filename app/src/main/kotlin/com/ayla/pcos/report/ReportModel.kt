package com.ayla.pcos.report

/**
 * Doktor raporunun içerik modeli (ADR-0009, Karar 1). Saf Kotlin: Android ya da kaynak (`R`) bağımlılığı yoktur.
 *
 * Model yalnızca **hazır dizeleri** taşır; kullanıcıya görünen her metin (başlıklar, etiketler, birimler, sayılar)
 * `strings.xml`'den üretilip buraya konur. Sayfalama [ReportLayout], çizim [PdfReportWriter] işidir; ikisi de
 * modeli yorumlamaz, olduğu gibi dizer.
 */
data class DoctorReport(
    /** Belge başlığı: ilk sayfada büyük, sonraki sayfalarda küçük üst bilgi. */
    val title: String,
    /** Alt başlık (ör. kapsanan tarih aralığı ve oluşturulma günü); boşsa çizilmez. */
    val subtitle: String,
    /** İlk sayfada, başlığın hemen altında çerçeveli ibare ("kayıt özetidir; tanı ya da tavsiye değildir"); boşsa çizilmez. */
    val disclaimer: String,
    val sections: List<ReportSection>,
    /** Her sayfanın altında solda; sağda sayfa etiketi durur. Boşsa yalnızca sayfa etiketi çizilir. */
    val footer: String,
)

/** Başlıklı bölüm; başlık sayfa sonunda tek başına kalmaz (altına en az iki satır içerik sığmıyorsa yeni sayfaya geçer). */
data class ReportSection(val title: String, val blocks: List<ReportBlock>)

/**
 * Metin stilleri. Punto ve kalınlık burada tanımlıdır ki ölçer ([TextMeasurer]) ile yazıcı aynı değerleri kullansın;
 * yerleşim motoru punto değil, ölçerin verdiği genişlik ve satır yüksekliğiyle çalışır.
 */
enum class TextStyle(val pointSize: Float, val bold: Boolean) {
    TITLE(18f, false),
    SUBTITLE(11f, false),
    SECTION(13f, true),
    BODY(10f, false),
    SMALL(8f, false),
    BOLD(10f, true),
}

/**
 * Sıcaklık grafiğinde bir ölçüm. [day] döngü günüdür (1 = adet başlangıcı); [excluded] bozuk ölçüm
 * (hastalık, geç uyanma vb.): içi boş çizilir ve çizgiye katılmaz, ama eksen aralığına dâhildir.
 */
data class ChartPoint(val day: Int, val celsius: Double, val excluded: Boolean = false)

/** Bir bölümün içindeki yerleşim birimleri; sıra korunur. */
sealed interface ReportBlock {
    /** Kelime bazlı kaydırılan metin; `\n` yeni satır başlatır. */
    data class Paragraph(val text: String, val style: TextStyle = TextStyle.BODY) : ReportBlock

    /** "•" ile girintili maddeler; her madde kendi içinde kaydırılır. */
    data class Bullets(val items: List<String>) : ReportBlock

    /** İki sütun: anahtar kalın (%35), değer (%65). */
    data class KeyValues(val rows: List<Pair<String, String>>) : ReportBlock

    /**
     * Başlık satırı kalın ve altı çizgili; sayfa kırılımında başlık yinelenir. [weights] sütun genişlik oranları
     * (toplamı önemsiz, normalize edilir); verilmezse ya da sütun sayısıyla uyuşmazsa sütunlar eşittir. Eksik hücreler
     * boş sayılır.
     */
    data class Table(val header: List<String>, val rows: List<List<String>>, val weights: List<Float>? = null) : ReportBlock

    /** Çerçeveli uyarı/ibare (6 pt iç boşluk). */
    data class Note(val text: String) : ReportBlock

    /**
     * Bazal vücut sıcaklığı grafiği (160 pt yükseklik). x = döngü günü, y = °C (min − 0,2 … max + 0,2, en fazla
     * sekiz etiket). [coverlineCelsius] kesikli yatay çizgi, [lhPositiveDays] x ekseninin hemen üstünde küçük dolu
     * kareler, [ovulationDay] dikey ince çizgi. Sığmazsa bütünüyle yeni sayfaya geçer.
     */
    data class TemperatureChart(
        val points: List<ChartPoint>,
        val coverlineCelsius: Double? = null,
        val lhPositiveDays: Set<Int> = emptySet(),
        val ovulationDay: Int? = null,
        /** Eksen başlıkları; hazır dize (ör. "Döngü günü", "°C"). */
        val xLabel: String,
        val yLabel: String,
    ) : ReportBlock

    /** Dikey boşluk (pt); sayfa başında düşer. */
    data class Spacer(val height: Float) : ReportBlock
}
