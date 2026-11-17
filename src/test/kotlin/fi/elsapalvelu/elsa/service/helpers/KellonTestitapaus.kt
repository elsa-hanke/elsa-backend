package fi.elsapalvelu.elsa.service.helpers

import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

data class KellonTestitapaus(val hetki: String, val aikavyohyke: String, val paiva: LocalDate, val tiedostonPaiva: String) {
    fun kello(): Clock = Clock.fixed(Instant.parse(hetki), ZoneId.of(aikavyohyke))
    fun aikaleima(): Instant = Instant.parse(hetki)
    fun paikallinenAika(): LocalDateTime = LocalDateTime.ofInstant(aikaleima(), ZoneId.of(aikavyohyke))

    companion object {
        @JvmStatic
        fun tapaukset(): List<KellonTestitapaus> = listOf(
            KellonTestitapaus("2020-02-29T22:30:00Z", "UTC", LocalDate.of(2020, 2, 29), "20200229"),
            KellonTestitapaus("2020-02-29T22:30:00Z", "Europe/Helsinki", LocalDate.of(2020, 3, 1), "20200301"),
            KellonTestitapaus("2020-12-31T22:30:00Z", "Europe/Helsinki", LocalDate.of(2021, 1, 1), "20210101")
        )
    }
}

const val KELLON_TESTITAPAUKSET = "fi.elsapalvelu.elsa.service.helpers.KellonTestitapaus#tapaukset"
