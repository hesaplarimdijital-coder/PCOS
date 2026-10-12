package com.ayla.pcos.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.LocalDate

/*
 * java.time türleri için ISO-8601 metin serileştiricileri. Fotoğraf dizini (ADR-0010) ve Navigation 3
 * gezinme anahtarları (ADR-0003) bunları kullanır; yedek dosyası (DataExporter) kendi metin alanlarıyla
 * çalışır ve bunlara bağlı değildir.
 */

/** ISO-8601 gün (`2026-10-12`). */
internal object LocalDateIso : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDate) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): LocalDate = LocalDate.parse(decoder.decodeString())
}

/** ISO-8601 an (`2026-10-12T07:00:00Z`). */
internal object InstantIso : KSerializer<Instant> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Instant", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Instant) = encoder.encodeString(value.toString())

    override fun deserialize(decoder: Decoder): Instant = Instant.parse(decoder.decodeString())
}
