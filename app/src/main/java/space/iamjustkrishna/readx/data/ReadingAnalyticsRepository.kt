package space.iamjustkrishna.readx.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

data class DayReadingStat(
    val dayInitial: String,
    val date: LocalDate,
    val minutes: Int,
    val isToday: Boolean,
    val isPeak: Boolean
)

data class WeeklyAnalyticsData(
    val dailyStats: List<DayReadingStat>,
    val totalTimeFormatted: String,
    val totalMinutes: Int,
    val totalPagesRead: Int,
    val booksFinishedCount: Int,
    val percentageComparison: String,
    val isPositiveTrend: Boolean
)

class ReadingAnalyticsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _weeklyStats = MutableStateFlow(computeWeeklyStats())
    val weeklyStats: StateFlow<WeeklyAnalyticsData> = _weeklyStats.asStateFlow()

    fun refreshStats() {
        _weeklyStats.value = computeWeeklyStats()
    }

    /**
     * Logs a completed reading session for today.
     */
    fun logReadingSession(durationSeconds: Long, pagesReadCount: Int, completedDocUri: String? = null) {
        if (durationSeconds <= 0 && pagesReadCount <= 0 && completedDocUri == null) return

        val today = LocalDate.now()
        val key = dateKey(today)
        val raw = prefs.getString(key, null)

        val json = if (!raw.isNullOrBlank()) {
            runCatching { JSONObject(raw) }.getOrElse { JSONObject() }
        } else {
            JSONObject()
        }

        val prevDuration = json.optLong("durationSeconds", 0L)
        val prevPages = json.optInt("pagesRead", 0)
        val completedArray = json.optJSONArray("completedBooks") ?: JSONArray()

        json.put("durationSeconds", prevDuration + durationSeconds)
        json.put("pagesRead", prevPages + pagesReadCount)

        if (completedDocUri != null) {
            var alreadyContains = false
            for (i in 0 until completedArray.length()) {
                if (completedArray.getString(i) == completedDocUri) {
                    alreadyContains = true
                    break
                }
            }
            if (!alreadyContains) {
                completedArray.put(completedDocUri)
            }
        }
        json.put("completedBooks", completedArray)

        prefs.edit().putString(key, json.toString()).apply()
        refreshStats()
    }

    fun computeWeeklyStats(): WeeklyAnalyticsData {
        val today = LocalDate.now()
        val mondayThisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val mondayLastWeek = mondayThisWeek.minusWeeks(1)

        val dayInitials = listOf("M", "T", "W", "T", "F", "S", "S")
        val dailyStats = mutableListOf<DayReadingStat>()

        var thisWeekTotalSeconds = 0L
        var thisWeekTotalPages = 0
        val thisWeekCompletedBooks = mutableSetOf<String>()

        var maxMinutes = 0

        // Calculate this week stats for 7 days (Monday -> Sunday)
        for (i in 0 until 7) {
            val date = mondayThisWeek.plusDays(i.toLong())
            val key = dateKey(date)
            val raw = prefs.getString(key, null)

            var daySeconds = 0L
            var dayPages = 0

            if (!raw.isNullOrBlank()) {
                runCatching {
                    val obj = JSONObject(raw)
                    daySeconds = obj.optLong("durationSeconds", 0L)
                    dayPages = obj.optInt("pagesRead", 0)
                    val arr = obj.optJSONArray("completedBooks")
                    if (arr != null) {
                        for (k in 0 until arr.length()) {
                            thisWeekCompletedBooks.add(arr.getString(k))
                        }
                    }
                }
            }

            val dayMinutes = (daySeconds / 60).toInt()
            if (dayMinutes > maxMinutes) maxMinutes = dayMinutes

            thisWeekTotalSeconds += daySeconds
            thisWeekTotalPages += dayPages

            dailyStats.add(
                DayReadingStat(
                    dayInitial = dayInitials[i],
                    date = date,
                    minutes = dayMinutes,
                    isToday = date == today,
                    isPeak = false
                )
            )
        }

        // Mark peak day(s) if any activity occurred
        val finalizedDailyStats = if (maxMinutes > 0) {
            dailyStats.map { it.copy(isPeak = it.minutes == maxMinutes) }
        } else {
            dailyStats
        }

        // Calculate last week total seconds for comparison trend
        var lastWeekTotalSeconds = 0L
        for (i in 0 until 7) {
            val date = mondayLastWeek.plusDays(i.toLong())
            val key = dateKey(date)
            val raw = prefs.getString(key, null)
            if (!raw.isNullOrBlank()) {
                runCatching {
                    val obj = JSONObject(raw)
                    lastWeekTotalSeconds += obj.optLong("durationSeconds", 0L)
                }
            }
        }

        val thisWeekTotalMinutes = (thisWeekTotalSeconds / 60).toInt()
        val lastWeekTotalMinutes = (lastWeekTotalSeconds / 60).toInt()

        val (comparisonText, isPositiveTrend) = when {
            lastWeekTotalMinutes == 0 && thisWeekTotalMinutes > 0 -> {
                "+100% vs last week" to true
            }
            lastWeekTotalMinutes > 0 -> {
                val diffPercent = (((thisWeekTotalMinutes - lastWeekTotalMinutes).toFloat() / lastWeekTotalMinutes.toFloat()) * 100).toInt()
                if (diffPercent >= 0) {
                    "+$diffPercent% vs last week" to true
                } else {
                    "$diffPercent% vs last week" to false
                }
            }
            else -> {
                "0% vs last week" to true
            }
        }

        val formattedTotalTime = formatMinutes(thisWeekTotalMinutes)

        return WeeklyAnalyticsData(
            dailyStats = finalizedDailyStats,
            totalTimeFormatted = formattedTotalTime,
            totalMinutes = thisWeekTotalMinutes,
            totalPagesRead = thisWeekTotalPages,
            booksFinishedCount = thisWeekCompletedBooks.size,
            percentageComparison = comparisonText,
            isPositiveTrend = isPositiveTrend
        )
    }

    private fun formatMinutes(totalMinutes: Int): String {
        return if (totalMinutes >= 60) {
            val hours = totalMinutes / 60
            val mins = totalMinutes % 60
            if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
        } else {
            "${totalMinutes}m"
        }
    }

    private fun dateKey(date: LocalDate): String = "stats_$date"

    private companion object {
        const val PREFS_NAME = "readx_reading_analytics"
    }
}
