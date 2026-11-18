package fi.elsapalvelu.elsa.service.impl.koejakso

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class KoejaksonPaivamaaratapaus(val hetki: String, val aikavyohyke: String, val paiva: LocalDate, val tiedostonPaiva: String) {
    fun kello(): Clock = Clock.fixed(Instant.parse(hetki), ZoneId.of(aikavyohyke))
    fun paikallinenAika(): LocalDateTime = LocalDateTime.ofInstant(Instant.parse(hetki), ZoneId.of(aikavyohyke))

    companion object {
        fun tapaukset(): List<KoejaksonPaivamaaratapaus> = listOf(
            KoejaksonPaivamaaratapaus("2020-02-29T22:30:00Z", "UTC", LocalDate.of(2020, 2, 29), "20200229"),
            KoejaksonPaivamaaratapaus("2020-02-29T22:30:00Z", "Europe/Helsinki", LocalDate.of(2020, 3, 1), "20200301"),
            KoejaksonPaivamaaratapaus("2020-12-31T22:30:00Z", "Europe/Helsinki", LocalDate.of(2021, 1, 1), "20210101")
        )
    }
}
