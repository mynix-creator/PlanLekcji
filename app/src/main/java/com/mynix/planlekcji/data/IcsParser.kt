package com.mynix.planlekcji.data

import android.util.Log
import java.io.InputStream
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object IcsParser {
    data class ParsedLesson(
        val dayOfWeek: Int,      // 1=Mon … 5=Fri
        val startTime: LocalTime,
        val endTime: LocalTime,
        val subject: String,
        val teacher: String      // extracted from DESCRIPTION
    )

    fun parse(inputStream: InputStream): List<ParsedLesson> {
        val content = inputStream.bufferedReader(Charsets.UTF_8).readText()

        // Unfold lines (ICS wraps long lines with \r\n + whitespace)
        val unfolded = content.replace(Regex("\r\n[ \t]"), "")

        // Split into VEVENT blocks
        val events = unfolded.split("BEGIN:VEVENT")
            .drop(1)
            .map { it.substringBefore("END:VEVENT") }

        // Find the last Monday in the file to identify last week
        val allDates = events.mapNotNull { block ->
            val dtstart = Regex("DTSTART:(\\d{8})").find(block)?.groupValues?.get(1)
            dtstart?.let { LocalDate.parse(it, DateTimeFormatter.BASIC_ISO_DATE) }
        }
        val lastDate = allDates.maxOrNull() ?: return emptyList()
        // Find Monday of the last week
        val lastMonday = lastDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val lastFriday = lastMonday.plusDays(4)

        // Parse only events from last Mon–Fri week
        return events.mapNotNull { block ->
            try {
                val dtStartStr = Regex("DTSTART:(\\d{8}T\\d{6})").find(block)
                    ?.groupValues?.get(1) ?: return@mapNotNull null
                val dtEndStr = Regex("DTEND:(\\d{8}T\\d{6})").find(block)
                    ?.groupValues?.get(1) ?: return@mapNotNull null

                val fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
                val startDt = LocalDateTime.parse(dtStartStr, fmt)
                val endDt = LocalDateTime.parse(dtEndStr, fmt)
                val date = startDt.toLocalDate()

                // Skip if not in last week or weekend
                if (date < lastMonday || date > lastFriday) return@mapNotNull null
                val dow = date.dayOfWeek.value   // 1=Mon…5=Fri
                if (dow > 5) return@mapNotNull null

                val summary = Regex("SUMMARY:(.+)").find(block)
                    ?.groupValues?.get(1)?.trim() ?: return@mapNotNull null

                // Extract teacher from DESCRIPTION
                // Format: "Przedmiot\: Subject\nNauczyciel\: Firstname Lastname"
                val desc = Regex("DESCRIPTION:(.+?)(?=\r?\n[A-Z])", RegexOption.DOT_MATCHES_ALL)
                    .find(block)?.groupValues?.get(1)?.trim() ?: ""
                val unescapedDesc = desc
                    .replace("\\n", "\n")
                    .replace("\\:", ":")
                    .replace("\\,", ",")
                val teacher = Regex("Nauczyciel:\\s*(.+)").find(unescapedDesc)
                    ?.groupValues?.get(1)?.trim()
                    ?.lines()?.firstOrNull()?.trim() ?: ""

                ParsedLesson(
                    dayOfWeek = dow,
                    startTime = startDt.toLocalTime(),
                    endTime = endDt.toLocalTime(),
                    subject = summary,
                    teacher = teacher
                )
            } catch (e: Exception) {
                Log.w("IcsParser", "Skipping event: ${e.message}")
                null
            }
        }.sortedWith(compareBy({ it.dayOfWeek }, { it.startTime }))
    }
}
