package com.ayla.pcos.report

/**
 * Metin ölçümü (pt). [ReportLayout] sayfalamayı yalnızca bu iki değerle yapar; böylece sayfalama JVM'de,
 * Android olmadan sınanır (ADR-0009). Üretimde [PaintTextMeasurer] (`Paint.measureText`), testte sabit
 * genişlikli sahte ölçer kullanılır.
 */
interface TextMeasurer {
    /** [text]'in [style] ile çizildiğinde kaplayacağı genişlik (pt). */
    fun width(text: String, style: TextStyle): Float

    /** Bir satırın yüksekliği (pt), satır aralığı dâhil. */
    fun lineHeight(style: TextStyle): Float
}
