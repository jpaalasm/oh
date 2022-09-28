
package net.paalasmaa

import java.time.*
import java.time.format.DateTimeFormatter

enum class OpeningState { open, close }
data class OpeningItem(val type: OpeningState, val value: Int)
typealias InputType = Map<String, List<OpeningItem>>

val dayNamesCapitalized = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
var dayNames = dayNamesCapitalized.map { it.lowercase() }

private fun transformToFlatTransitionList(openingHoursInput: InputType): Result<List<Pair<Int, OpeningItem>>> {
    val openingHoursDaySequence = dayNames.map { openingHoursInput[it] }.filterNotNull()

    if (!openingHoursDaySequence.flatten().all { it.value in 0..86400 }) {
        return Result.failure(Exception("The time values must be in range 0...86400"))
    }

    if (openingHoursDaySequence.size < dayNames.size) {
        return Result.failure(Exception("All days not provided in the input"))
    }

    if (openingHoursInput.size > 7) {
        return Result.failure(Exception("Excessive keys in input (should be just weekday names)"))
    }

    for (itemsForDay in openingHoursDaySequence) {
        val timestampsForDay = itemsForDay.map { it.value }
        if (timestampsForDay != timestampsForDay.sorted()) {
            return Result.failure(Exception("Timestamps within a day are not in sorted order"))
        }
    }

    var flatTransitions = openingHoursDaySequence.withIndex()
        .flatMap { (dayIndex, openingItems) -> openingItems.map { Pair(dayIndex, it) } }
        .toMutableList()

    // If the first transition is "close", move it to the end of the list so things start from an "open"
    if (flatTransitions.firstOrNull()?.second?.type == OpeningState.close) {
        flatTransitions.add(flatTransitions.removeAt(0))
    }

    return Result.success(flatTransitions)
}

data class OpenPeriod(val closeDayIndex: Int, val openTime: Int, val closeTime: Int)

fun transformToOpenPeriods(openingHoursInput: InputType): Result<List<List<OpenPeriod>>> {
    transformToFlatTransitionList(openingHoursInput).fold(
        onSuccess = {
            var transitionsCopy = it.toMutableList()

            var openPeriodsByDay = List(7) { mutableListOf<OpenPeriod>() }

            while (transitionsCopy.size >= 2) {
                val open = transitionsCopy.removeAt(0)
                val close = transitionsCopy.removeAt(0)

                if (open.second.type != OpeningState.open || close.second.type != OpeningState.close) {
                    return Result.failure(Exception("The input does not consists of consecutive open-close pairs"))
                }

                openPeriodsByDay[open.first].add(OpenPeriod(
                    close.first, open.second.value, close.second.value
                ))
            }

            return Result.success(openPeriodsByDay)
        },
        onFailure = {
            return Result.failure(it)
        }
    )
}

private fun formatTimestamp(timestamp: Int): String {
    val dateTime = Instant.ofEpochSecond(timestamp.toLong())
                          .atZone(ZoneOffset.UTC)
                          .toLocalDateTime()
    return dateTime.format(DateTimeFormatter.ofPattern("h:m a")).uppercase()
}

fun formatOpeningHours(openPeriods: List<List<OpenPeriod>>): List<String> {
    var result: MutableList<String> = mutableListOf()

    for ((dayName, dayItems) in (dayNamesCapitalized zip openPeriods)) {
        var timePart = when (dayItems) {
            listOf<OpenPeriod>() -> { "Closed" }
            else -> {
                dayItems.joinToString(", ") { "${formatTimestamp(it.openTime)} - ${formatTimestamp(it.closeTime)}" }
            }
        }

        result.add("${dayName}: $timePart")
    }

    return result
}