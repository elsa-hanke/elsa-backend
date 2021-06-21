package fi.elsapalvelu.elsa.service.dto.enumeration

import fi.elsapalvelu.elsa.service.dto.*

enum class KoejaksoTila {
    EI_AKTIIVINEN,
    UUSI,
    TALLENNETTU_KESKENERAISENA,
    ODOTTAA_HYVAKSYNTAA,
    ODOTTAA_ERIKOISTUVAN_HYVAKSYNTAA,
    ODOTTAA_ESIMIEHEN_HYVAKSYNTAA,
    ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA,
    ODOTTAA_TOISEN_KOULUTTAJAN_HYVAKSYNTAA,
    PALAUTETTU_KORJATTAVAKSI,
    HYVAKSYTTY;

    companion object {
        fun fromSopimus(koejaksonKoulutussopimusDTO: KoejaksonKoulutussopimusDTO?, kayttajaId: Long? = null): KoejaksoTila {
            return if (koejaksonKoulutussopimusDTO == null) UUSI
            else if (koejaksonKoulutussopimusDTO.vastuuhenkilo?.sopimusHyvaksytty == true) HYVAKSYTTY
            else if (!koejaksonKoulutussopimusDTO.korjausehdotus.isNullOrBlank()) PALAUTETTU_KORJATTAVAKSI
            else if (koejaksonKoulutussopimusDTO.lahetetty == false) TALLENNETTU_KESKENERAISENA
            else if (koejaksonKoulutussopimusDTO.kouluttajat?.all { it.sopimusHyvaksytty == true } == true) ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA
            else if (koejaksonKoulutussopimusDTO.kouluttajat?.find { it.kayttajaId == kayttajaId }?.sopimusHyvaksytty == true) ODOTTAA_TOISEN_KOULUTTAJAN_HYVAKSYNTAA
            else ODOTTAA_HYVAKSYNTAA
        }

        fun fromAloituskeskustelu(aloituskeskusteluDTO: KoejaksonAloituskeskusteluDTO?, kayttajaId: Long? = null): KoejaksoTila {
            return if (aloituskeskusteluDTO == null) UUSI
            else if (aloituskeskusteluDTO.lahiesimies?.sopimusHyvaksytty == true) HYVAKSYTTY
            else if (aloituskeskusteluDTO.lahikouluttaja?.sopimusHyvaksytty == true && aloituskeskusteluDTO.lahiesimies?.id != kayttajaId) ODOTTAA_ESIMIEHEN_HYVAKSYNTAA
            else if (!aloituskeskusteluDTO.korjausehdotus.isNullOrBlank()) PALAUTETTU_KORJATTAVAKSI
            else if (aloituskeskusteluDTO.lahetetty == false) TALLENNETTU_KESKENERAISENA
            else ODOTTAA_HYVAKSYNTAA
        }

        fun fromValiarvointi(
            aloituskeskusteluHyvaksytty: Boolean,
            valiarviointiDTO: KoejaksonValiarviointiDTO?,
            kayttajaId: Long? = null
        ): KoejaksoTila {
            return if (!aloituskeskusteluHyvaksytty) EI_AKTIIVINEN
            else if (valiarviointiDTO == null) UUSI
            else if (valiarviointiDTO.erikoistuvaAllekirjoittanut == true) HYVAKSYTTY
            else if (valiarviointiDTO.lahiesimies?.sopimusHyvaksytty == true) ODOTTAA_ERIKOISTUVAN_HYVAKSYNTAA
            else if (valiarviointiDTO.lahikouluttaja?.sopimusHyvaksytty == true && valiarviointiDTO.lahiesimies?.id != kayttajaId) ODOTTAA_ESIMIEHEN_HYVAKSYNTAA
            else if (!valiarviointiDTO.korjausehdotus.isNullOrBlank()) PALAUTETTU_KORJATTAVAKSI
            else ODOTTAA_HYVAKSYNTAA
        }

        fun fromKehittamistoimenpiteet(
            kehittamistoimenpiteita: Boolean,
            kehittamistoimenpiteetDTO: KoejaksonKehittamistoimenpiteetDTO?,
            kayttajaId: Long? = null
        ): KoejaksoTila {
            return if (!kehittamistoimenpiteita) EI_AKTIIVINEN
            else if (kehittamistoimenpiteetDTO == null) UUSI
            else if (kehittamistoimenpiteetDTO.erikoistuvaAllekirjoittanut == true) HYVAKSYTTY
            else if (kehittamistoimenpiteetDTO.lahiesimies?.sopimusHyvaksytty == true) ODOTTAA_ERIKOISTUVAN_HYVAKSYNTAA
             else if (kehittamistoimenpiteetDTO.lahikouluttaja?.sopimusHyvaksytty == true && kehittamistoimenpiteetDTO.lahiesimies?.id != kayttajaId) ODOTTAA_ESIMIEHEN_HYVAKSYNTAA
            else if (!kehittamistoimenpiteetDTO.korjausehdotus.isNullOrBlank()) PALAUTETTU_KORJATTAVAKSI
            else ODOTTAA_HYVAKSYNTAA
        }

        fun fromLoppukeskustelu(
            aktiivinen: Boolean,
            loppukeskusteluDTO: KoejaksonLoppukeskusteluDTO?,
            kayttajaId: Long? = null
        ): KoejaksoTila {
            return if (!aktiivinen) EI_AKTIIVINEN
            else if (loppukeskusteluDTO == null) UUSI
            else if (loppukeskusteluDTO.erikoistuvaAllekirjoittanut == true) HYVAKSYTTY
            else if (loppukeskusteluDTO.lahiesimies?.sopimusHyvaksytty == true) ODOTTAA_ERIKOISTUVAN_HYVAKSYNTAA
            else if (loppukeskusteluDTO.lahikouluttaja?.sopimusHyvaksytty == true && loppukeskusteluDTO.lahiesimies?.id != kayttajaId) ODOTTAA_ESIMIEHEN_HYVAKSYNTAA
            else if (!loppukeskusteluDTO.korjausehdotus.isNullOrBlank()) PALAUTETTU_KORJATTAVAKSI
            else ODOTTAA_HYVAKSYNTAA
        }

        fun fromVastuuhenkilonArvio(
            loppukeskusteluHyvaksytty: Boolean,
            vastuuhenkilonArvioDTO: KoejaksonVastuuhenkilonArvioDTO?
        ): KoejaksoTila {
            return if (!loppukeskusteluHyvaksytty) EI_AKTIIVINEN
            else if (vastuuhenkilonArvioDTO == null) UUSI
            else if (vastuuhenkilonArvioDTO.erikoistuvaAllekirjoittanut == true) HYVAKSYTTY
            else if (vastuuhenkilonArvioDTO.vastuuhenkilo?.sopimusHyvaksytty == true) ODOTTAA_ERIKOISTUVAN_HYVAKSYNTAA
            else ODOTTAA_HYVAKSYNTAA
        }
    }
}
