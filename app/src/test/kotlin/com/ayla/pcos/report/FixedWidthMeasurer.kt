package com.ayla.pcos.report

/**
 * Sabit genişlikli sahte ölçer (ADR-0009): her karakter punto × [charWidth] pt, satır yüksekliği punto ×
 * [lineHeightFactor] pt. Yerleşim testleri Android olmadan, belirleyici sonuçla çalışır.
 */
class FixedWidthMeasurer(
    private val charWidth: Float = 0.55f,
    private val lineHeightFactor: Float = 1.4f,
) : TextMeasurer {
    override fun width(text: String, style: TextStyle): Float = text.length * charWidth * style.pointSize

    override fun lineHeight(style: TextStyle): Float = style.pointSize * lineHeightFactor
}
