package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.EpaOsaamisalue
import java.time.LocalDate
import java.time.ZoneId

class EpaOsaamisalueHelper {

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private const val DEFAULT_KUVAUS = "AAAAAAAAAA"
        private const val UPDATED_KUVAUS = "BBBBBBBBBB"

        private val DEFAULT_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_VOIMASSAOLO_LOPPUU: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLO_LOPPUU: LocalDate = LocalDate.now(ZoneId.systemDefault())

        @JvmStatic
        fun createEntity(): EpaOsaamisalue {
            val epaOsaamisalue = EpaOsaamisalue(
                nimi = DEFAULT_NIMI,
                kuvaus = DEFAULT_KUVAUS,
                voimassaoloAlkaa = DEFAULT_VOIMASSAOLO_ALKAA,
                voimassaoloLoppuu = DEFAULT_VOIMASSAOLO_LOPPUU
            )

            return epaOsaamisalue
        }

        @JvmStatic
        fun createUpdatedEntity(): EpaOsaamisalue {
            val epaOsaamisalue = EpaOsaamisalue(
                nimi = UPDATED_NIMI,
                kuvaus = UPDATED_KUVAUS,
                voimassaoloAlkaa = UPDATED_VOIMASSAOLO_ALKAA,
                voimassaoloLoppuu = UPDATED_VOIMASSAOLO_LOPPUU
            )

            return epaOsaamisalue
        }
    }
}
