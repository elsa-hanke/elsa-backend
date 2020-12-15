package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Erikoisala
import fi.elsapalvelu.elsa.domain.enumeration.ErikoisalaTyyppi
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

        private val DEFAULT_TYYPPI: ErikoisalaTyyppi = ErikoisalaTyyppi.LAAKETIEDE
        private val UPDATED_TYYPPI: ErikoisalaTyyppi = ErikoisalaTyyppi.HAMMASLAAKETIEDE

        private const val DEFAULT_KAYTANNON_KOULUTUKSEN_VAHIMMAISPITUUS: Double = 1.0
        private const val UPDATED_KAYTANNON_KOULUTUKSEN_VAHIMMAISPITUUS: Double = 2.0

        private const val DEFAULT_TERVEYSKESKUSKOULUTUSJAKSON_VAHIMMAISPITUUS: Double = 1.0
        private const val UPDATED_TERVEYSKESKUSKOULUTUSJAKSON_VAHIMMAISPITUUS: Double = 2.0

        private const val DEFAULT_YLIOPISTOSAIRAALAJAKSON_VAHIMMAISPITUUS: Double = 1.0
        private const val UPDATED_YLIOPISTOSAIRAALAJAKSON_VAHIMMAISPITUUS: Double = 2.0

        private const val DEFAULT_YLIOPISTOSAIRAALAN_ULKOPUOLISEN_TYOSKENTELYN_VAHIMMAISPITUUS: Double = 1.0
        private const val UPDATED_YLIOPISTOSAIRAALAN_ULKOPUOLISEN_TYOSKENTELYN_VAHIMMAISPITUUS: Double = 2.0

        @JvmStatic
        fun createEntity(): Erikoisala {
            val erikoisala = Erikoisala(
                nimi = DEFAULT_NIMI,
                voimassaoloAlkaa = DEFAULT_VOIMASSAOLO_ALKAA,
                voimassaoloPaattyy = DEFAULT_VOIMASSAOLO_PAATTYY,
                tyyppi = DEFAULT_TYYPPI,
                kaytannonKoulutuksenVahimmaispituus = DEFAULT_KAYTANNON_KOULUTUKSEN_VAHIMMAISPITUUS,
                terveyskeskuskoulutusjaksonVahimmaispituus = DEFAULT_TERVEYSKESKUSKOULUTUSJAKSON_VAHIMMAISPITUUS,
                yliopistosairaalajaksonVahimmaispituus = DEFAULT_YLIOPISTOSAIRAALAJAKSON_VAHIMMAISPITUUS,
                yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus =
                    DEFAULT_YLIOPISTOSAIRAALAN_ULKOPUOLISEN_TYOSKENTELYN_VAHIMMAISPITUUS
            )

            return erikoisala
        }

        @JvmStatic
        fun createUpdatedEntity(): Erikoisala {
            val erikoisala = Erikoisala(
                nimi = UPDATED_NIMI,
                voimassaoloAlkaa = UPDATED_VOIMASSAOLO_ALKAA,
                voimassaoloPaattyy = UPDATED_VOIMASSAOLO_PAATTYY,
                tyyppi = UPDATED_TYYPPI,
                kaytannonKoulutuksenVahimmaispituus = UPDATED_KAYTANNON_KOULUTUKSEN_VAHIMMAISPITUUS,
                terveyskeskuskoulutusjaksonVahimmaispituus = UPDATED_TERVEYSKESKUSKOULUTUSJAKSON_VAHIMMAISPITUUS,
                yliopistosairaalajaksonVahimmaispituus = UPDATED_YLIOPISTOSAIRAALAJAKSON_VAHIMMAISPITUUS,
                yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus =
                    UPDATED_YLIOPISTOSAIRAALAN_ULKOPUOLISEN_TYOSKENTELYN_VAHIMMAISPITUUS
            )

            return erikoisala
        }
    }
}
