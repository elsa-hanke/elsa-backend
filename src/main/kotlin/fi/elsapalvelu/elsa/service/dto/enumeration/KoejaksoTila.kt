package fi.elsapalvelu.elsa.service.dto.enumeration

import fi.elsapalvelu.elsa.service.dto.*

enum class KoejaksoTila {
    EI_AKTIIVINEN, UUSI, TALLENNETTU_KESKENERAISENA, ODOTTAA_HYVAKSYNTAA, ODOTTAA_ERIKOISTUVAN_HYVAKSYNTAA, PALAUTETTU_KORJATTAVAKSI, HYVAKSYTTY;

    companion object {
        fun fromSopimus(koejaksonKoulutussopimusDTO: KoejaksonKoulutussopimusDTO?): KoejaksoTila {
            if (koejaksonKoulutussopimusDTO == null) {
                return UUSI
            }
            if (koejaksonKoulutussopimusDTO.vastuuhenkilo?.sopimusHyvaksytty == true) {
                return HYVAKSYTTY
            }
            if (!koejaksonKoulutussopimusDTO.korjausehdotus.isNullOrBlank()) {
                return PALAUTETTU_KORJATTAVAKSI
            }
            if (koejaksonKoulutussopimusDTO.lahetetty == false) {
                return TALLENNETTU_KESKENERAISENA
            }
            return ODOTTAA_HYVAKSYNTAA
        }

        fun fromAloituskeskustelu(aloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO?): KoejaksoTila {
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

        fun fromValiarvointi(
            aloituskeskusteluHyvaksytty: Boolean,
            valiarviointiDTO: KoejaksonValiarviointiDTO?
        ): KoejaksoTila {
            if (!aloituskeskusteluHyvaksytty) {
                return EI_AKTIIVINEN
            }
            if (valiarviointiDTO == null) {
                return UUSI
            }
            if (valiarviointiDTO.erikoistuvaAllekirjoittanut == true) {
                return HYVAKSYTTY
            }
            if (valiarviointiDTO.lahiesimies?.sopimusHyvaksytty == true) {
                return ODOTTAA_ERIKOISTUVAN_HYVAKSYNTAA
            }
            if (!valiarviointiDTO.korjausehdotus.isNullOrBlank()) {
                return PALAUTETTU_KORJATTAVAKSI
            }
            return ODOTTAA_HYVAKSYNTAA
        }

        fun fromKehittamistoimenpiteet(
            kehittamistoimenpiteita: Boolean,
            kehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO?
        ): KoejaksoTila {
            if (!kehittamistoimenpiteita) {
                return EI_AKTIIVINEN
            }
            if (kehittamistoimenpiteetDTO == null) {
                return UUSI
            }
            if (kehittamistoimenpiteetDTO.erikoistuvaAllekirjoittanut == true) {
                return HYVAKSYTTY
            }
            if (kehittamistoimenpiteetDTO.lahiesimies?.sopimusHyvaksytty == true) {
                return ODOTTAA_ERIKOISTUVAN_HYVAKSYNTAA
            }
            if (!kehittamistoimenpiteetDTO.korjausehdotus.isNullOrBlank()) {
                return PALAUTETTU_KORJATTAVAKSI
            }
            return ODOTTAA_HYVAKSYNTAA
        }

        fun fromLoppukeskustelu(
            aktiivinen: Boolean,
            loppukeskusteluDTO: KoejaksonLoppukeskusteluDTO?
        ): KoejaksoTila {
            if (!aktiivinen) {
                return EI_AKTIIVINEN
            }
            if (loppukeskusteluDTO == null) {
                return UUSI
            }
            if (loppukeskusteluDTO.erikoistuvaAllekirjoittanut == true) {
                return HYVAKSYTTY
            }
            if (loppukeskusteluDTO.lahiesimies?.sopimusHyvaksytty == true) {
                return ODOTTAA_ERIKOISTUVAN_HYVAKSYNTAA
            }
            if (!loppukeskusteluDTO.korjausehdotus.isNullOrBlank()) {
                return PALAUTETTU_KORJATTAVAKSI
            }
            return ODOTTAA_HYVAKSYNTAA
        }

        fun fromVastuuhenkilonArvio(
            loppukeskusteluHyvaksytty: Boolean,
            vastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO?
        ): KoejaksoTila {
            if (!loppukeskusteluHyvaksytty) {
                return EI_AKTIIVINEN
            }
            if (vastuuhenkilonArvioDTO == null) {
                return UUSI
            }
            if (vastuuhenkilonArvioDTO.erikoistuvaAllekirjoittanut == true) {
                return HYVAKSYTTY
            }
            if (vastuuhenkilonArvioDTO.vastuuhenkilo?.sopimusHyvaksytty == true) {
                return ODOTTAA_ERIKOISTUVAN_HYVAKSYNTAA
            }
            return ODOTTAA_HYVAKSYNTAA
        }
    }
}
