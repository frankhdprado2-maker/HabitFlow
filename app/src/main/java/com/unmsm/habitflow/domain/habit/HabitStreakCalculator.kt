package com.unmsm.habitflow.domain.habit

import com.unmsm.habitflow.domain.model.Habit
import com.unmsm.habitflow.domain.model.HabitEvent
import com.unmsm.habitflow.domain.model.HabitStatus
import java.text.Normalizer
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.WeekFields
import java.util.Locale

data class HabitStreakMetrics(
    val currentStreak: Int = 0,
    val bestStreak: Int = 0,
    val completedScheduledDays: Int = 0,
    val scheduledDays: Int = 0,
    val completionRate: Double = 0.0,
    val unit: StreakUnit = StreakUnit.Day
)

enum class StreakUnit { Day, Week }

object HabitStreakCalculator {
    fun calculate(
        habit: Habit,
        events: List<HabitEvent>,
        today: LocalDate,
        zoneId: ZoneId
    ): HabitStreakMetrics {
        val schedule = LegacySchedule.parse(habit.frequency)
        val completedDates = events.asSequence()
            .filter { it.habitId == habit.id && it.status == HabitStatus.Completed }
            .map { it.localDate(zoneId) }
            .filter { !it.isAfter(today) }
            .toSet()
        if (completedDates.isEmpty() || schedule == LegacySchedule.Unknown) return HabitStreakMetrics()
        return when (schedule) {
            is LegacySchedule.TimesPerWeek -> weeklyMetrics(completedDates, schedule.times, today)
            else -> dailyMetrics(schedule, completedDates, today)
        }
    }

    private fun dailyMetrics(
        schedule: LegacySchedule,
        completedDates: Set<LocalDate>,
        today: LocalDate
    ): HabitStreakMetrics {
        val firstDate = completedDates.minOrNull() ?: return HabitStreakMetrics()
        val scheduled = generateSequence(firstDate) { date ->
            date.plusDays(1).takeUnless { it.isAfter(today) }
        }.filter(schedule::isScheduled).toList()
        if (scheduled.isEmpty()) return HabitStreakMetrics()
        val completedScheduled = scheduled.filter(completedDates::contains).toSet()

        var best = 0
        var run = 0
        scheduled.forEach { date ->
            if (date in completedScheduled) {
                run += 1
                best = maxOf(best, run)
            } else {
                run = 0
            }
        }

        val currentCandidates = scheduled.asReversed().let { descending ->
            if (descending.firstOrNull() == today && today !in completedScheduled) descending.drop(1) else descending
        }
        val current = currentCandidates.takeWhile(completedScheduled::contains).size
        return HabitStreakMetrics(
            currentStreak = current,
            bestStreak = best,
            completedScheduledDays = completedScheduled.size,
            scheduledDays = scheduled.size,
            completionRate = completedScheduled.size.toDouble() / scheduled.size,
            unit = StreakUnit.Day
        )
    }

    private fun weeklyMetrics(
        completedDates: Set<LocalDate>,
        timesPerWeek: Int,
        today: LocalDate
    ): HabitStreakMetrics {
        val weekFields = WeekFields.ISO
        fun weekKey(date: LocalDate) = date.get(weekFields.weekBasedYear()) to date.get(weekFields.weekOfWeekBasedYear())
        val completedByWeek = completedDates.groupingBy(::weekKey).eachCount()
        val firstMonday = completedDates.minOrNull()!!.with(DayOfWeek.MONDAY)
        val currentMonday = today.with(DayOfWeek.MONDAY)
        val weeks = generateSequence(firstMonday) { monday ->
            monday.plusWeeks(1).takeUnless { it.isAfter(currentMonday) }
        }.toList()
        val satisfied = weeks.filter { completedByWeek[weekKey(it)]?.let { it >= timesPerWeek } == true }.toSet()

        var best = 0
        var run = 0
        weeks.forEach { monday ->
            if (monday in satisfied) {
                run += 1
                best = maxOf(best, run)
            } else {
                run = 0
            }
        }
        val currentCandidates = weeks.asReversed().let { descending ->
            if (descending.firstOrNull() == currentMonday && currentMonday !in satisfied) descending.drop(1) else descending
        }
        val current = currentCandidates.takeWhile(satisfied::contains).size
        val scheduledDays = weeks.size * timesPerWeek
        val completedForGoal = completedByWeek.values.sumOf { minOf(it, timesPerWeek) }
        return HabitStreakMetrics(
            currentStreak = current,
            bestStreak = best,
            completedScheduledDays = completedForGoal,
            scheduledDays = scheduledDays,
            completionRate = if (scheduledDays == 0) 0.0 else completedForGoal.toDouble() / scheduledDays,
            unit = StreakUnit.Week
        )
    }
}

private sealed interface LegacySchedule {
    fun isScheduled(date: LocalDate): Boolean

    data object Daily : LegacySchedule {
        override fun isScheduled(date: LocalDate) = true
    }

    data class Weekdays(val days: Set<DayOfWeek>) : LegacySchedule {
        override fun isScheduled(date: LocalDate) = date.dayOfWeek in days
    }

    data class TimesPerWeek(val times: Int) : LegacySchedule {
        override fun isScheduled(date: LocalDate) = true
    }

    data object Unknown : LegacySchedule {
        override fun isScheduled(date: LocalDate) = false
    }

    companion object {
        fun parse(value: String): LegacySchedule {
            val normalized = value.normalized()
            if (normalized in setOf("diario", "todos los dias", "cada dia")) return Daily
            Regex("^(\\d+) veces?( por)? semana$").matchEntire(normalized)?.let { match ->
                return TimesPerWeek(match.groupValues[1].toInt().coerceIn(1, 7))
            }
            val tokens = normalized.split("-").map(String::trim).filter(String::isNotEmpty)
            if (tokens.size == 2) {
                val start = tokens[0].toDayOfWeek()
                val end = tokens[1].toDayOfWeek()
                if (start != null && end != null) {
                    val days = generateSequence(start) { day ->
                        if (day == end) null else DayOfWeek.of(day.value % 7 + 1)
                    }.toSet()
                    return Weekdays(days)
                }
            }
            val listedDays = tokens.mapNotNull(String::toDayOfWeek).toSet()
            return if (listedDays.isNotEmpty() && listedDays.size == tokens.size) Weekdays(listedDays) else Unknown
        }
    }
}

private fun HabitEvent.localDate(zoneId: ZoneId): LocalDate =
    java.time.Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDate()

private fun String.normalized(): String =
    Normalizer.normalize(lowercase(Locale("es", "PE")).trim(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .replace(Regex("\\s+"), " ")

private fun String.toDayOfWeek(): DayOfWeek? = when (normalized().take(3)) {
    "lun" -> DayOfWeek.MONDAY
    "mar" -> DayOfWeek.TUESDAY
    "mie" -> DayOfWeek.WEDNESDAY
    "jue" -> DayOfWeek.THURSDAY
    "vie" -> DayOfWeek.FRIDAY
    "sab" -> DayOfWeek.SATURDAY
    "dom" -> DayOfWeek.SUNDAY
    else -> null
}
