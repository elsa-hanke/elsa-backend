package fi.elsapalvelu.elsa.service.dto.enumeration

import fi.elsapalvelu.elsa.service.dto.KoejaksonAloituskeskusteluDTO

enum class AloituskeskusteluTila {
    UUSI, TALLENNETTU_KESKENERAISENA, ODOTTAA_HYVAKSYNTAA, PALAUTETTU_KORJATTAVAKSI, HYVAKSYTTY;

    companion object {
        fun fromSopimus(aloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO?): AloituskeskusteluTila {
            if (aloituskeskusteluDTO == null) {
                return UUSI
            }
            if (aloituskeskusteluDTO.lahiesimies?.sopimusHyvaksytty == true) {
                return HYVAKSYTTY
            }
            if (!aloituskeskusteluDTO.korjausehdotus.isNullOrBlank()) {
                return PALAUTETTU_KORJATTAVAKSI
            }
            if (aloituskeskusteluDTO.lahetetty == false) {
                return TALLENNETTU_KESKENERAISENA
            }
            return ODOTTAA_HYVAKSYNTAA
        }
    }
}
