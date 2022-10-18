package fi.elsapalvelu.elsa.extensions

import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

private val log = LoggerFactory.getLogger("LocalDateExtensions")

fun LocalDate.duringYears(endDate: LocalDate): List<Int> {
    return (this.year..endDate.year).toList()
}

fun LocalDate.daysBetween(endDate: LocalDate): Int {
    return ChronoUnit.DAYS.between(
        this, endDate
    ).toInt() + 1
}

fun LocalDate.isInRange(startDate: LocalDate, endDate: LocalDate?): Boolean {
    return startDate <= this && (endDate == null || endDate >= this)
}

fun LocalDate.periodLessThan(endDate: LocalDate?, lessThanYears: Int): Boolean {
    return ChronoUnit.YEARS.between(
        this, endDate
    ) < lessThanYears
}

fun Int.startOfYearDate(): LocalDate {
    return LocalDate.of(this, 1, 1)
}

fun Int.endOfYearDate(): LocalDate {
    return YearMonth.of(this, 12).atEndOfMonth()
}

fun String.tryParseToLocalDate(): LocalDate? {
    return try {
        LocalDate.parse(this)
    }
    catch(ex: Exception) {
        return tryParseToLocalDateTime()?.toLocalDate()
    }
}

fun String.tryParseToLocalDateTime(): LocalDateTime? {
    return try {
        LocalDateTime.parse(this)
    }
    catch(ex: Exception) {
        log.warn("$this ei ole kelvollinen päivämäärä." )
        null
    }
}

