package com.jadwalshalat.gt5.prayer

import java.time.LocalDate
import java.time.chrono.HijrahDate
import java.time.temporal.ChronoField

/**
 * Konverter tanggal Masehi ke Kalender Hijriah (Umm Al-Qura) 100% offline
 * menggunakan java.time.chrono.HijrahDate bawaan Android API 26+.
 */
object HijriCalendarHelper {

    private val HIJRI_MONTHS_ID = arrayOf(
        "Muharram",
        "Safar",
        "Rabiul Awal",
        "Rabiul Akhir",
        "Jumadil Awal",
        "Jumadil Akhir",
        "Rajab",
        "Syaban",
        "Ramadan",
        "Syawal",
        "Zulkaidah",
        "Zulhijah"
    )

    fun formatIndonesianHijriDate(date: LocalDate): String {
        return try {
            val hijri = HijrahDate.from(date)
            val day = hijri.get(ChronoField.DAY_OF_MONTH)
            val monthIndex = (hijri.get(ChronoField.MONTH_OF_YEAR) - 1).coerceIn(0, 11)
            val year = hijri.get(ChronoField.YEAR)
            "$day ${HIJRI_MONTHS_ID[monthIndex]} $year H"
        } catch (_: Exception) {
            ""
        }
    }
}
