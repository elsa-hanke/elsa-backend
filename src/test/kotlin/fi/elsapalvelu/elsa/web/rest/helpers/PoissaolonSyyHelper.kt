package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.PoissaolonSyy
import java.time.LocalDate
import java.time.ZoneId

class PoissaolonSyyHelper {

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private val DEFAULT_VOIMASSAOLON_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLON_ALKAMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_VOIMASSAOLON_PAATTYMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLON_PAATTYMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        @JvmStatic
        fun createEntity(): PoissaolonSyy {
            val poissaolonSyy = PoissaolonSyy(
                nimi = DEFAULT_NIMI,
                voimassaolonAlkamispaiva = DEFAULT_VOIMASSAOLON_ALKAMISPAIVA,
                voimassaolonPaattymispaiva = DEFAULT_VOIMASSAOLON_PAATTYMISPAIVA
            )

            return poissaolonSyy
        }

        @JvmStatic
        fun createUpdatedEntity(): PoissaolonSyy {
            val poissaolonSyy = PoissaolonSyy(
                nimi = UPDATED_NIMI,
                voimassaolonAlkamispaiva = UPDATED_VOIMASSAOLON_ALKAMISPAIVA,
                voimassaolonPaattymispaiva = UPDATED_VOIMASSAOLON_PAATTYMISPAIVA
            )

            return poissaolonSyy
        }
    }
}
