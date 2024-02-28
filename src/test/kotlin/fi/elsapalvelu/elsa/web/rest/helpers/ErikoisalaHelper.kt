package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Erikoisala
import fi.elsapalvelu.elsa.domain.enumeration.ErikoisalaTyyppi

class ErikoisalaHelper {

    companion object {

        private const val DEFAULT_ID = 62L
        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val DEFAULT_VIRTAPATEVYYSKOODI = "FFFFFFFFFF"
        private const val UPDATED_NIMI = "BBBBBBBBBB"
        private const val UPDATED_VIRTAPATEVYYSKOODI = "GGGGGGGGGG"

        private val DEFAULT_TYYPPI: ErikoisalaTyyppi = ErikoisalaTyyppi.LAAKETIEDE
        private val UPDATED_TYYPPI: ErikoisalaTyyppi = ErikoisalaTyyppi.HAMMASLAAKETIEDE

        @JvmStatic
        fun createEntity(
            nimi: String = DEFAULT_NIMI,
            virtaPatevyyskoodi: String? = DEFAULT_VIRTAPATEVYYSKOODI,
            tyyppi: ErikoisalaTyyppi = DEFAULT_TYYPPI
        ): Erikoisala {
            return Erikoisala(
                nimi = nimi,
                tyyppi = tyyppi,
                virtaPatevyyskoodi = virtaPatevyyskoodi,
                liittynytElsaan = true
            )
        }

        @JvmStatic
        fun createUpdatedEntity(
            nimi: String = UPDATED_NIMI,
            virtaPatevyyskoodi: String? = UPDATED_VIRTAPATEVYYSKOODI,
            tyyppi: ErikoisalaTyyppi = UPDATED_TYYPPI
        ): Erikoisala {
            return Erikoisala(
                nimi = nimi,
                tyyppi = tyyppi,
                virtaPatevyyskoodi = virtaPatevyyskoodi,
                liittynytElsaan = true
            )
        }

        @JvmStatic
        fun createEntityWithId(
            id: Long = DEFAULT_ID,
            nimi: String = DEFAULT_NIMI,
            virtaPatevyyskoodi: String? = DEFAULT_VIRTAPATEVYYSKOODI,
            tyyppi: ErikoisalaTyyppi = DEFAULT_TYYPPI
        ): Erikoisala {
            return Erikoisala(
                id = id,
                nimi = nimi,
                tyyppi = tyyppi,
                virtaPatevyyskoodi = virtaPatevyyskoodi,
                liittynytElsaan = true
            )
        }

    }
}
