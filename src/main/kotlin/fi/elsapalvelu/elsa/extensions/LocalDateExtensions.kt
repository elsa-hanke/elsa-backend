package fi.elsapalvelu.elsa.extensions

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

fun LocalDate.duringYears(endDate: LocalDate): List<Int> {
    return (this.year..endDate.year).toList()
}

fun LocalDate.daysBetween(endDate: LocalDate): Int {
    return ChronoUnit.DAYS.between(
        this, endDate
    ).toInt() + 1
}

fun Int.startOfYearDate(): LocalDate {
    return LocalDate.of(this, 1, 1)
}

fun Int.endOfYearDate(): LocalDate {
    return YearMonth.of(this, 12).atEndOfMonth()
}
