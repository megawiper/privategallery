package com.privategallery.app.core.media

import com.privategallery.app.domain.model.MediaItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * STEP 12 — DATE GROUPING ENGINE
 *
 * Groups media by actual date metadata (dateTakenMillis, falling back to
 * dateAddedMillis), never by filename. Produces the Today / Yesterday /
 * specific-date sections the Home screen (STEP 11) renders.
 */
data class DateGroup(
    val label: String,
    val items: List<MediaItem>
)

object DateGroupingEngine {

    fun group(items: List<MediaItem>, now: Long = System.currentTimeMillis()): List<DateGroup> {
        val calendarToday = Calendar.getInstance().apply { timeInMillis = now }
        val todayYear = calendarToday.get(Calendar.YEAR)
        val todayDayOfYear = calendarToday.get(Calendar.DAY_OF_YEAR)

        val calendarYesterday = Calendar.getInstance().apply {
            timeInMillis = now
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val yesterdayYear = calendarYesterday.get(Calendar.YEAR)
        val yesterdayDayOfYear = calendarYesterday.get(Calendar.DAY_OF_YEAR)

        val formatter = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

        return items
            .sortedByDescending { effectiveDate(it) }
            .groupBy { item ->
                val cal = Calendar.getInstance().apply { timeInMillis = effectiveDate(item) }
                val year = cal.get(Calendar.YEAR)
                val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
                when {
                    year == todayYear && dayOfYear == todayDayOfYear -> "Today"
                    year == yesterdayYear && dayOfYear == yesterdayDayOfYear -> "Yesterday"
                    else -> formatter.format(Date(effectiveDate(item)))
                }
            }
            .map { (label, groupedItems) -> DateGroup(label, groupedItems) }
    }

    private fun effectiveDate(item: MediaItem): Long {
        return item.dateTakenMillis ?: item.dateAddedMillis
    }
}
