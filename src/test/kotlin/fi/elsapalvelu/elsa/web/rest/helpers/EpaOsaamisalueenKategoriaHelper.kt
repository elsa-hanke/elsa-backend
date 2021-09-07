package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.EpaOsaamisalueenKategoria
import java.time.LocalDate
import java.time.ZoneId

class EpaOsaamisalueenKategoriaHelper {

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private const val DEFAULT_JARJESTYSNUMERO: Int = 1
        private const val UPDATED_JARJESTYSNUMERO: Int = 2

        private val DEFAULT_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_VOIMASSAOLO_LOPPUU: LocalDate = LocalDate.ofEpochDay(30L)
        private val UPDATED_VOIMASSAOLO_LOPPUU: LocalDate = LocalDate.now(ZoneId.systemDefault())

        @JvmStatic
        fun createEntity(): EpaOsaamisalueenKategoria {
            val epaOsaamisalueenKategoria = EpaOsaamisalueenKategoria(
                nimi = DEFAULT_NIMI,
                jarjestysnumero = DEFAULT_JARJESTYSNUMERO,
                voimassaoloAlkaa = DEFAULT_VOIMASSAOLO_ALKAA,
                voimassaoloLoppuu = DEFAULT_VOIMASSAOLO_LOPPUU
            )

            return epaOsaamisalueenKategoria
        }

        @JvmStatic
        fun createUpdatedEntity(): EpaOsaamisalueenKategoria {
            val epaOsaamisalueenKategoria = EpaOsaamisalueenKategoria(
                nimi = UPDATED_NIMI,
                jarjestysnumero = UPDATED_JARJESTYSNUMERO,
                voimassaoloAlkaa = UPDATED_VOIMASSAOLO_ALKAA,
                voimassaoloLoppuu = UPDATED_VOIMASSAOLO_LOPPUU
            )

            return epaOsaamisalueenKategoria
        }
    }
}
