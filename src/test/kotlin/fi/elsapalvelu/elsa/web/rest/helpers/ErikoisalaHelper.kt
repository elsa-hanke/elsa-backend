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
        fun createEntity(
            nimi: String = DEFAULT_NIMI,
            virtaPatevyyskoodi: String? = DEFAULT_VIRTAPATEVYYSKOODI
        ): Erikoisala {
            return Erikoisala(
                nimi = nimi,
                tyyppi = DEFAULT_TYYPPI,
                virtaPatevyyskoodi = virtaPatevyyskoodi,
                liittynytElsaan = true
            )
        }

        @JvmStatic
        fun createUpdatedEntity(
            nimi: String = UPDATED_NIMI,
            virtaPatevyyskoodi: String? = UPDATED_VIRTAPATEVYYSKOODI
        ): Erikoisala {
            return Erikoisala(
                nimi = nimi,
                tyyppi = UPDATED_TYYPPI,
                virtaPatevyyskoodi = virtaPatevyyskoodi,
                liittynytElsaan = true
            )
        }
    }
}
