package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Tyoskentelypaikka
import fi.elsapalvelu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi

class TyoskentelypaikkaHelper {

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private const val DEFAULT_KUNTA = "AAAAAAAAAA"
        private const val UPDATED_KUNTA = "BBBBBBBBBB"

        private val DEFAULT_TYYPPI: TyoskentelyjaksoTyyppi = TyoskentelyjaksoTyyppi.TERVEYSKESKUS
        private val UPDATED_TYYPPI: TyoskentelyjaksoTyyppi = TyoskentelyjaksoTyyppi.MUU
        private const val UPDATED_MUU_TYYPPI = "CCCCCCCCCC"

        @JvmStatic
        fun createEntity(): Tyoskentelypaikka {
            val tyoskentelypaikka = Tyoskentelypaikka(
                nimi = DEFAULT_NIMI,
                kunta = DEFAULT_KUNTA,
                tyyppi = DEFAULT_TYYPPI,
                muuTyyppi = null
            )

            return tyoskentelypaikka
        }

        @JvmStatic
        fun createUpdatedEntity(): Tyoskentelypaikka {
            val tyoskentelypaikka = Tyoskentelypaikka(
                nimi = UPDATED_NIMI,
                kunta = UPDATED_KUNTA,
                tyyppi = UPDATED_TYYPPI,
                muuTyyppi = UPDATED_MUU_TYYPPI
            )

            return tyoskentelypaikka
        }
    }
}
