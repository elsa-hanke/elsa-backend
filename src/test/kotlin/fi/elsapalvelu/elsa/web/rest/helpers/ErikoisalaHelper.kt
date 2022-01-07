package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Erikoisala
import fi.elsapalvelu.elsa.domain.enumeration.ErikoisalaTyyppi

class ErikoisalaHelper {

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private val DEFAULT_TYYPPI: ErikoisalaTyyppi = ErikoisalaTyyppi.LAAKETIEDE
        private val UPDATED_TYYPPI: ErikoisalaTyyppi = ErikoisalaTyyppi.HAMMASLAAKETIEDE

        @JvmStatic
        fun createEntity(): Erikoisala {
            val erikoisala = Erikoisala(
                nimi = DEFAULT_NIMI,
                tyyppi = DEFAULT_TYYPPI
            )

            return erikoisala
        }

        @JvmStatic
        fun createUpdatedEntity(): Erikoisala {
            val erikoisala = Erikoisala(
                nimi = UPDATED_NIMI,
                tyyppi = UPDATED_TYYPPI,
            )

            return erikoisala
        }
    }
}
