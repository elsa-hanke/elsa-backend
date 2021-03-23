package fi.elsapalvelu.elsa.service.dto.enumeration

import fi.elsapalvelu.elsa.service.dto.KoejaksonKoulutussopimusDTO

enum class KoulutussopimusTila {
    EI_ALOITETTU, TALLENNETTU_KESKENERAISENA, ODOTTAA_HYVAKSYNTAA, PALAUTETTU_KORJATTAVAKSI, HYVAKSYTTY;

    companion object {
        fun fromSopimus(koejaksonKoulutussopimusDTO: KoejaksonKoulutussopimusDTO?): KoulutussopimusTila {
            if (koejaksonKoulutussopimusDTO == null) {
                return EI_ALOITETTU
            }
            if (koejaksonKoulutussopimusDTO.vastuuhenkilo?.sopimusHyvaksytty == true) {
                return HYVAKSYTTY
            }
            if (koejaksonKoulutussopimusDTO.korjausehdotus != null) {
                return PALAUTETTU_KORJATTAVAKSI
            }
            if (koejaksonKoulutussopimusDTO.lahetetty == false) {
                return TALLENNETTU_KESKENERAISENA
            }
            return ODOTTAA_HYVAKSYNTAA
        }
    }
}
