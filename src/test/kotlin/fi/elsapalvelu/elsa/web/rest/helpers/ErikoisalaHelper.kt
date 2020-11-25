package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Erikoisala
import java.time.LocalDate
import java.time.ZoneId

class ErikoisalaHelper {

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private val DEFAULT_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_VOIMASSAOLO_PAATTYY: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLO_PAATTYY: LocalDate = LocalDate.now(ZoneId.systemDefault())

        @JvmStatic
        fun createEntity(): Erikoisala {
            val erikoisala = Erikoisala(
                nimi = DEFAULT_NIMI,
                voimassaoloAlkaa = DEFAULT_VOIMASSAOLO_ALKAA,
                voimassaoloPaattyy = DEFAULT_VOIMASSAOLO_PAATTYY
            )

            return erikoisala
        }

        @JvmStatic
        fun createUpdatedEntity(): Erikoisala {
            val erikoisala = Erikoisala(
                nimi = UPDATED_NIMI,
                voimassaoloAlkaa = UPDATED_VOIMASSAOLO_ALKAA,
                voimassaoloPaattyy = UPDATED_VOIMASSAOLO_PAATTYY
            )

            return erikoisala
        }
    }
}
