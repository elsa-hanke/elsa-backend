package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Kunta

class KuntaHelper {

    companion object {

        private const val DEFAULT_ABBREVIATION = "AAAAAAAAAA"
        private const val UPDATED_ABBREVIATION = "BBBBBBBBBB"

        private const val DEFAULT_SHORT_NAME = "AAAAAAAAAA"
        private const val UPDATED_SHORT_NAME = "BBBBBBBBBB"

        private const val DEFAULT_LONG_NAME = "AAAAAAAAAA"
        private const val UPDATED_LONG_NAME = "BBBBBBBBBB"

        private const val DEFAULT_DESCRIPTION = "AAAAAAAAAA"
        private const val UPDATED_DESCRIPTION = "BBBBBBBBBB"

        private const val DEFAULT_KORTNAMN = "AAAAAAAAAA"
        private const val UPDATED_KORTNAMN = "BBBBBBBBBB"

        private const val DEFAULT_KORVAAVA_KOODI = "AAAAAAAAAA"
        private const val UPDATED_KORVAAVA_KOODI = "BBBBBBBBBB"

        private const val DEFAULT_LANGT_NAMN = "AAAAAAAAAA"
        private const val UPDATED_LANGT_NAMN = "BBBBBBBBBB"

        private const val DEFAULT_MAAKUNTA = "AAAAAAAAAA"
        private const val UPDATED_MAAKUNTA = "BBBBBBBBBB"

        private const val DEFAULT_SAIRAANHOITOPIIRI = "AAAAAAAAAA"
        private const val UPDATED_SAIRAANHOITOPIIRI = "BBBBBBBBBB"

        @JvmStatic
        fun createEntity(): Kunta {
            val kunta = Kunta(
                id = "001",
                abbreviation = DEFAULT_ABBREVIATION,
                shortName = DEFAULT_SHORT_NAME,
                longName = DEFAULT_LONG_NAME,
                description = DEFAULT_DESCRIPTION,
                kortnamn = DEFAULT_KORTNAMN,
                korvaavaKoodi = DEFAULT_KORVAAVA_KOODI,
                langtNamn = DEFAULT_LANGT_NAMN,
                maakunta = DEFAULT_MAAKUNTA,
                sairaanhoitopiiri = DEFAULT_SAIRAANHOITOPIIRI
            )

            return kunta
        }

        @JvmStatic
        fun createUpdatedEntity(): Kunta {
            val kunta = Kunta(
                id = "001",
                abbreviation = UPDATED_ABBREVIATION,
                shortName = UPDATED_SHORT_NAME,
                longName = UPDATED_LONG_NAME,
                description = UPDATED_DESCRIPTION,
                kortnamn = UPDATED_KORTNAMN,
                korvaavaKoodi = UPDATED_KORVAAVA_KOODI,
                langtNamn = UPDATED_LANGT_NAMN,
                maakunta = UPDATED_MAAKUNTA,
                sairaanhoitopiiri = UPDATED_SAIRAANHOITOPIIRI
            )

            return kunta
        }
    }
}
