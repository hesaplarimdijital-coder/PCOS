package com.ayla.pcos.data.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Tarihler ISO metni olarak saklanır; SQL'de sıralama ve karşılaştırma doğru çalışır. */
class Converters {
    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun localTimeToString(value: LocalTime?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalTime(value: String?): LocalTime? = value?.let(LocalTime::parse)

    @TypeConverter
    fun instantToLong(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun longToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun timesToString(value: List<LocalTime>?): String? = value?.joinToString(",") { it.toString() }

    @TypeConverter
    fun stringToTimes(value: String?): List<LocalTime>? =
        value?.split(",")?.filter { it.isNotBlank() }?.map(LocalTime::parse)

    @TypeConverter
    fun stringsToString(value: List<String>?): String? = value?.joinToString("|")

    @TypeConverter
    fun stringToStrings(value: String?): List<String>? =
        value?.split("|")?.filter { it.isNotBlank() }
}
