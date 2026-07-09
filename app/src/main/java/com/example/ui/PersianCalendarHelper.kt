package com.example.ui

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object PersianCalendarHelper {

    private val JALALI_MONTH_NAMES_EN = arrayOf(
        "Farvardin", "Ordibehesht", "Khordad",
        "Tir", "Mordad", "Shahrivar",
        "Mehr", "Aban", "Azar",
        "Dey", "Bahman", "Esfand"
    )

    private val JALALI_MONTH_NAMES_FA = arrayOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    private val WEEK_DAYS_FA = mapOf(
        Calendar.SATURDAY to "شنبه",
        Calendar.SUNDAY to "یکشنبه",
        Calendar.MONDAY to "دوشنبه",
        Calendar.TUESDAY to "سه‌شنبه",
        Calendar.WEDNESDAY to "چهارشنبه",
        Calendar.THURSDAY to "پنجشنبه",
        Calendar.FRIDAY to "جمعه"
    )

    // Converts Gregorian date to Jalali Triple(Year, Month 1-based, Day)
    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): Triple<Int, Int, Int> {
        val gDMonth = intArrayOf(0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334)
        val gy2 = if (gm > 2) gy + 1 else gy
        var days = 355666 + (365 * gy) + ((gy2 + 3) / 4) - ((gy2 + 99) / 100) + ((gy2 + 399) / 400) + gd + gDMonth[gm - 1]
        var jy = -1595 + (33 * (days / 12053))
        days %= 12053
        jy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            jy += (days - 1) / 365
            days = (days - 1) % 365
        }
        val jm: Int
        val jd: Int
        if (days < 186) {
            jm = 1 + (days / 31)
            jd = 1 + (days % 31)
        } else {
            jm = 7 + ((days - 186) / 30)
            jd = 1 + ((days - 186) % 30)
        }
        return Triple(jy, jm, jd)
    }

    // Convert date string "yyyy-MM-dd" to Persian date string "1405/04/16"
    fun formatToPersianSlash(dateStr: String): String {
        try {
            val parts = dateStr.split("-")
            if (parts.size != 3) return dateStr
            val gy = parts[0].toInt()
            val gm = parts[1].toInt()
            val gd = parts[2].toInt()
            val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
            return String.format(Locale.US, "%04d/%02d/%02d", jy, jm, jd)
        } catch (e: Exception) {
            return dateStr
        }
    }

    // Convert date string "yyyy-MM-dd" to Persian full details e.g., "Tuesday, 16 Tir 1405" or "سه‌شنبه، ۱۶ تیر ۱۴۰۵"
    fun formatToPersianFull(dateStr: String, usePersianScript: Boolean = false): String {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return dateStr
            val cal = Calendar.getInstance()
            cal.time = date
            
            val gy = cal.get(Calendar.YEAR)
            val gm = cal.get(Calendar.MONTH) + 1
            val gd = cal.get(Calendar.DAY_OF_MONTH)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

            val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)

            if (usePersianScript) {
                val dayNameFa = WEEK_DAYS_FA[dayOfWeek] ?: ""
                val monthNameFa = JALALI_MONTH_NAMES_FA[jm - 1]
                val fJy = toPersianDigits(jy.toString())
                val fJd = toPersianDigits(jd.toString())
                return "$dayNameFa، $fJd $monthNameFa $fJy"
            } else {
                val englishDayName = SimpleDateFormat("EEEE", Locale.ENGLISH).format(date)
                val monthNameEn = JALALI_MONTH_NAMES_EN[jm - 1]
                return "$englishDayName, $jd $monthNameEn $jy"
            }
        } catch (e: Exception) {
            return dateStr
        }
    }

    fun getPersianDayNumber(dateStr: String): String {
        try {
            val parts = dateStr.split("-")
            if (parts.size != 3) return ""
            val gy = parts[0].toInt()
            val gm = parts[1].toInt()
            val gd = parts[2].toInt()
            val (_, _, jd) = gregorianToJalali(gy, gm, gd)
            return jd.toString()
        } catch (e: Exception) {
            return ""
        }
    }

    fun getPersianShortDayName(dateStr: String): String {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return ""
            val cal = Calendar.getInstance()
            cal.time = date
            return when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SATURDAY -> "ش"
                Calendar.SUNDAY -> "ی"
                Calendar.MONDAY -> "د"
                Calendar.TUESDAY -> "س"
                Calendar.WEDNESDAY -> "چ"
                Calendar.THURSDAY -> "پ"
                Calendar.FRIDAY -> "ج"
                else -> ""
            }
        } catch (e: Exception) {
            return ""
        }
    }

    fun toPersianDigits(text: String): String {
        val persianDigits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val builder = StringBuilder()
        for (ch in text) {
            if (ch in '0'..'9') {
                builder.append(persianDigits[ch - '0'])
            } else {
                builder.append(ch)
            }
        }
        return builder.toString()
    }

    // Converts Jalali date to Gregorian Triple(Year, Month 1-based, Day)
    fun jalaliToGregorian(jy: Int, jm: Int, jd: Int): Triple<Int, Int, Int> {
        val jy2 = jy + 1595
        var days = -355668 + (365 * jy2) + ((jy2 / 33) * 8) + (((jy2 % 33) + 3) / 4)
        if (jm < 7) {
            days += (jm - 1) * 31
        } else {
            days += ((jm - 7) * 30) + 186
        }
        days += jd
        var gy = 400 * (days / 146097)
        days %= 146097
        if (days > 36524) {
            days--
            gy += 100 * (days / 36524)
            days %= 36524
            if (days >= 365) days++
        }
        gy += 4 * (days / 1461)
        days %= 1461
        if (days > 365) {
            gy += (days - 1) / 365
            days = (days - 1) % 365
        }
        var gd = days + 1
        val salG = intArrayOf(0, 31, if ((gy % 4 == 0 && gy % 100 != 0) || gy % 400 == 0) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        var gm = 1
        while (gm <= 12 && gd > salG[gm]) {
            gd -= salG[gm]
            gm++
        }
        return Triple(gy, gm, gd)
    }

    // Format a Gregorian date "yyyy-MM-dd" to Jalali "yyyy-MM-dd"
    fun formatGregorianToJalaliHyphen(dateStr: String): String {
        try {
            val parts = dateStr.split("-")
            if (parts.size != 3) return dateStr
            val gy = parts[0].toInt()
            val gm = parts[1].toInt()
            val gd = parts[2].toInt()
            val (jy, jm, jd) = gregorianToJalali(gy, gm, gd)
            return String.format(Locale.US, "%04d-%02d-%02d", jy, jm, jd)
        } catch (e: Exception) {
            return dateStr
        }
    }

    // Format a Jalali date "yyyy-MM-dd" to Gregorian "yyyy-MM-dd"
    fun formatJalaliToGregorianHyphen(dateStr: String): String {
        try {
            val parts = dateStr.split("-")
            if (parts.size != 3) return dateStr
            val jy = parts[0].toInt()
            val jm = parts[1].toInt()
            val jd = parts[2].toInt()
            val (gy, gm, gd) = jalaliToGregorian(jy, jm, jd)
            return String.format(Locale.US, "%04d-%02d-%02d", gy, gm, gd)
        } catch (e: Exception) {
            return dateStr
        }
    }
}
