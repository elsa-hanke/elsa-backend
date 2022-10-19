package fi.elsapalvelu.elsa.extensions

private const val DAYS_IN_YEAR = 365
private const val MONTHS_IN_YEAR = 12
private const val DAYS_IN_MONTH = 30.5

fun Double.toYears(): Int {
    return (this / DAYS_IN_YEAR).toInt()
}

fun Double.toMonths(): Int {
    return this.toMonthsDouble().toInt()
}

fun Double.toMonthsDouble(): Double {
    return (this % DAYS_IN_YEAR) / DAYS_IN_YEAR * MONTHS_IN_YEAR
}

fun Double.toDays(): Int {
    val months = this.toMonthsDouble()
    return ((months - months.toInt()) * DAYS_IN_MONTH).toInt()
}

