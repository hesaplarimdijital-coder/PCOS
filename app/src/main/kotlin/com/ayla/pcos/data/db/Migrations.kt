package com.ayla.pcos.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/*
 * Şema geçişleri. Her sürüm artışı buraya bir Migration ekler ve AppModule'deki addMigrations listesine
 * girer; yıkıcı geçiş yalnızca downgrade içindir (fallbackToDestructiveMigrationOnDowngrade).
 *
 * SQL metinleri Room'un ilgili sürüm için üreteceği CREATE cümleleriyle (app/schemas/.../N.json)
 * birebir olmalıdır: Room açılışta tabloyu okuyup varlıkla karşılaştırır; sütun türü, NOT NULL ya da
 * birincil anahtar farkı "Migration didn't properly handle" ile açılışı durdurur. Kotlin varsayılanları
 * (ör. bbt_disturbed = false) SQL DEFAULT değildir; Room da üretmez, bu yüzden burada da yazılmaz.
 */

/**
 * Sürüm 1 → 2 (ADR-0009, MVP 3): doğurganlık kayıtları, cilt/saç profili, rutin işaretleri ve doktora
 * sorular. Yalnızca tablo ekler; mevcut kayıtlara dokunmaz.
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `fertility_entries` (" +
                "`date` TEXT NOT NULL, " +
                "`bbt_celsius` REAL, " +
                "`bbt_time` TEXT, " +
                "`bbt_disturbed` INTEGER NOT NULL, " +
                "`lh_result` TEXT, " +
                "`mucus` TEXT, " +
                "`updated_at` INTEGER NOT NULL, " +
                "PRIMARY KEY(`date`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `skin_profile` (" +
                "`id` INTEGER NOT NULL, " +
                "`skin_type` TEXT, " +
                "`acne` TEXT, " +
                "`dark_spots` INTEGER NOT NULL, " +
                "`acanthosis` INTEGER NOT NULL, " +
                "`unwanted_hair` INTEGER NOT NULL, " +
                "`hair_thinning` INTEGER NOT NULL, " +
                "`pregnant_or_breastfeeding` INTEGER NOT NULL, " +
                "`morning_reminder` TEXT, " +
                "`evening_reminder` TEXT, " +
                "`updated_at` INTEGER NOT NULL, " +
                "PRIMARY KEY(`id`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `skin_routine_checks` (" +
                "`date` TEXT NOT NULL, " +
                "`slot` TEXT NOT NULL, " +
                "`done_steps` TEXT NOT NULL, " +
                "`updated_at` INTEGER NOT NULL, " +
                "PRIMARY KEY(`date`, `slot`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `doctor_questions` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`text` TEXT NOT NULL, " +
                "`created_at` INTEGER NOT NULL, " +
                "`asked_at` INTEGER)",
        )
    }
}

/** AppModule ve testlerin aynı listeyi kullanması için. */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
