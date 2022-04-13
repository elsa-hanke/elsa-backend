package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Erikoisala
import fi.elsapalvelu.elsa.domain.enumeration.ErikoisalaTyyppi

class ErikoisalaHelper {

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val DEFAULT_VIRTAPATEVYYSKOODI = "FFFFFFFFFF"
        private const val UPDATED_NIMI = "BBBBBBBBBB"
        private const val UPDATED_VIRTAPATEVYYSKOODI = "GGGGGGGGGG"

        private val DEFAULT_TYYPPI: ErikoisalaTyyppi = ErikoisalaTyyppi.LAAKETIEDE
        private val UPDATED_TYYPPI: ErikoisalaTyyppi = ErikoisalaTyyppi.HAMMASLAAKETIEDE

        @JvmStatic
        fun createEntity(virtaPatevyyskoodi: String? = null): Erikoisala {
            val erikoisala = Erikoisala(
                nimi = DEFAULT_NIMI,
                tyyppi = DEFAULT_TYYPPI,
                virtaPatevyyskoodi = virtaPatevyyskoodi ?: DEFAULT_VIRTAPATEVYYSKOODI
            )

            return erikoisala
        }

        @JvmStatic
        fun createUpdatedEntity(virtaPatevyyskoodi: String? = null): Erikoisala {
            val erikoisala = Erikoisala(
                nimi = UPDATED_NIMI,
                tyyppi = UPDATED_TYYPPI,
                virtaPatevyyskoodi = virtaPatevyyskoodi ?: UPDATED_VIRTAPATEVYYSKOODI
            )

            return erikoisala
        }
    }
}
