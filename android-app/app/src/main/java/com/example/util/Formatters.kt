package com.example.util

import com.example.data.local.ExpiryStatus
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

object Formatters {
    private val numberFormat: NumberFormat = NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = 2
    }

    private val shortDateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    private val monthYearFormat = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
    private val shortMonthFormat = SimpleDateFormat("MMM", Locale.ENGLISH)
    private val dateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.ENGLISH)
    private val isoDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun formatPkr(amount: Double, currencySymbol: String = "Rs."): String {
        val sym = currencySymbol.trim().ifBlank { "Rs." }
        val prefix = if (amount < 0) "-$sym " else "$sym "
        return prefix + numberFormat.format(abs(amount))
    }

    fun formatCompactPkr(amount: Double, currencySymbol: String = "Rs."): String {
        val sym = currencySymbol.trim().ifBlank { "Rs." }
        val absVal = abs(amount)
        val sign = if (amount < 0) "-" else ""
        return when {
            absVal >= 1_000_000 -> "${sign}$sym ${String.format(Locale.US, "%.1fM", absVal / 1_000_000.0)}"
            absVal >= 100_000 -> "${sign}$sym ${String.format(Locale.US, "%.1fK", absVal / 1_000.0)}"
            else -> formatPkr(amount, sym)
        }
    }

    fun formatDate(millis: Long): String = shortDateFormat.format(Date(millis))

    fun formatDateTime(millis: Long): String = dateTimeFormat.format(Date(millis))

    fun formatIsoDate(millis: Long): String = isoDateFormat.format(Date(millis))

    fun formatMonthYear(year: Int, monthZeroBased: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return monthYearFormat.format(cal.time)
    }

    fun formatShortMonth(yearMonthKey: String): String {
        val parts = yearMonthKey.split("-")
        if (parts.size != 2) return yearMonthKey
        val y = parts[0].toIntOrNull() ?: return yearMonthKey
        val m = (parts[1].toIntOrNull() ?: 1) - 1
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, y)
            set(Calendar.MONTH, m.coerceIn(0, 11))
            set(Calendar.DAY_OF_MONTH, 1)
        }
        return shortMonthFormat.format(cal.time)
    }

    fun lastSixYearMonths(anchorYear: Int, anchorMonthZeroBased: Int): List<String> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, anchorYear)
            set(Calendar.MONTH, anchorMonthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, -5)
        }
        return List(6) {
            val ym = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            cal.add(Calendar.MONTH, 1)
            ym
        }
    }

    fun sixMonthsAgoMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.MONTH, -11)
        }
        return cal.timeInMillis
    }

    fun monthBoundsMillis(year: Int, monthZeroBased: Int): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, monthZeroBased)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val end = (start.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
        }
        return start.timeInMillis to end.timeInMillis
    }

    fun todayStartMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun daysUntilExpiry(expiryMillis: Long, nowMillis: Long = System.currentTimeMillis()): Long {
        return TimeUnit.MILLISECONDS.toDays(expiryMillis - nowMillis)
    }

    fun expiryStatus(expiryMillis: Long, nowMillis: Long = System.currentTimeMillis()): ExpiryStatus {
        val days = daysUntilExpiry(expiryMillis, nowMillis)
        return when {
            expiryMillis < nowMillis -> ExpiryStatus.EXPIRED
            days <= 30 -> ExpiryStatus.EXPIRING_SOON
            days <= 90 -> ExpiryStatus.WATCH
            else -> ExpiryStatus.SAFE
        }
    }

    fun expiryLabel(expiryMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
        val days = daysUntilExpiry(expiryMillis, nowMillis)
        return when {
            expiryMillis < nowMillis -> "Expired (${abs(days)}d ago)"
            days == 0L -> "Expires Today"
            days <= 30 -> "Expires in ${days}d"
            days <= 90 -> "${days}d left"
            else -> formatDate(expiryMillis)
        }
    }

    fun csvEscape(value: String): String {
        val needsQuotes = value.contains(",") || value.contains("\"") || value.contains("\n")
        return if (needsQuotes) "\"${value.replace("\"", "\"\"")}\"" else value
    }
}
