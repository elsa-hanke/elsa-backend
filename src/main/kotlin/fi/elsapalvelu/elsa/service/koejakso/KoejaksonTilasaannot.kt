package fi.elsapalvelu.elsa.service.koejakso

import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonAloituskeskustelu
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonKoulutussopimus
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonVastuuhenkilonArvio
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila.*
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonAloituskeskusteluDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonKehittamistoimenpiteetDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonKouluttajaDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonKoulutussopimusDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonLoppukeskusteluDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonValiarviointiDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonVastuuhenkilonArvioDTO
import java.time.LocalDate

/** Koejakson roolikohtaiset vaihetilat, arvion käsittelytila ja suorituksen yhteenveto. */
object KoejaksonTilasaannot {
    fun maaritaSopimuksenTila(
        sopimus: KoejaksonKoulutussopimusDTO?,
        vastuuhenkilo: Boolean,
        kayttajaId: Long? = null
    ): KoejaksoTila = when {
        sopimus == null -> UUSI
        sopimus.vastuuhenkilo?.sopimusHyvaksytty == true -> HYVAKSYTTY
        !sopimus.korjausehdotus.isNullOrBlank() || (
            (sopimus.lahetetty == false || vastuuhenkilo) && !sopimus.vastuuhenkilonKorjausehdotus.isNullOrBlank()
        ) -> PALAUTETTU_KORJATTAVAKSI
        sopimus.lahetetty == false -> TALLENNETTU_KESKENERAISENA
        sopimus.kouluttajat?.all { it.sopimusHyvaksytty == true } == true -> {
            if (sopimus.kouluttajat?.any { it.kayttajaId == kayttajaId } == true) ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA
            else ODOTTAA_HYVAKSYNTAA
        }
        sopimus.kouluttajat?.find { it.kayttajaId == kayttajaId }?.sopimusHyvaksytty == true -> ODOTTAA_TOISEN_KOULUTTAJAN_HYVAKSYNTAA
        else -> ODOTTAA_HYVAKSYNTAA
    }

    fun maaritaAloituskeskustelunTila(
        aloituskeskustelu: KoejaksonAloituskeskusteluDTO?,
        kayttajaId: Long? = null
    ): KoejaksoTila = when {
        aloituskeskustelu == null -> UUSI
        aloituskeskustelu.lahiesimies?.sopimusHyvaksytty == true -> HYVAKSYTTY
        aloituskeskustelu.lahikouluttaja?.sopimusHyvaksytty == true &&
            aloituskeskustelu.lahiesimies?.id != kayttajaId -> ODOTTAA_ESIMIEHEN_HYVAKSYNTAA
        !aloituskeskustelu.korjausehdotus.isNullOrBlank() -> PALAUTETTU_KORJATTAVAKSI
        aloituskeskustelu.lahetetty == false -> TALLENNETTU_KESKENERAISENA
        else -> ODOTTAA_HYVAKSYNTAA
    }

    fun maaritaValiarvioinninTila(
        aloituskeskusteluHyvaksytty: Boolean,
        valiarviointi: KoejaksonValiarviointiDTO?,
        kayttajaId: Long? = null
    ): KoejaksoTila = maaritaKeskustelunTila(
        aktiivinen = aloituskeskusteluHyvaksytty,
        olemassa = valiarviointi != null,
        lahikouluttaja = valiarviointi?.lahikouluttaja,
        lahiesimies = valiarviointi?.lahiesimies,
        korjausehdotus = valiarviointi?.korjausehdotus,
        kayttajaId = kayttajaId
    )

    fun maaritaKehittamistoimenpiteidenTila(
        kehittamistoimenpiteita: Boolean,
        kehittamistoimenpiteet: KoejaksonKehittamistoimenpiteetDTO?,
        kayttajaId: Long? = null
    ): KoejaksoTila = maaritaKeskustelunTila(
        aktiivinen = kehittamistoimenpiteita,
        olemassa = kehittamistoimenpiteet != null,
        lahikouluttaja = kehittamistoimenpiteet?.lahikouluttaja,
        lahiesimies = kehittamistoimenpiteet?.lahiesimies,
        korjausehdotus = kehittamistoimenpiteet?.korjausehdotus,
        kayttajaId = kayttajaId
    )

    fun maaritaLoppukeskustelunTila(
        aktiivinen: Boolean,
        loppukeskustelu: KoejaksonLoppukeskusteluDTO?,
        kayttajaId: Long? = null
    ): KoejaksoTila = maaritaKeskustelunTila(
        aktiivinen = aktiivinen,
        olemassa = loppukeskustelu != null,
        lahikouluttaja = loppukeskustelu?.lahikouluttaja,
        lahiesimies = loppukeskustelu?.lahiesimies,
        korjausehdotus = loppukeskustelu?.korjausehdotus,
        kayttajaId = kayttajaId
    )

    /** Vaiheketjussa uudelleen kuittaus piilottaa aiemman palautuksen erikoistuvalta. */
    fun maaritaVastuuhenkilonArvionVaihetila(
        loppukeskusteluHyvaksytty: Boolean,
        arvio: KoejaksonVastuuhenkilonArvioDTO?,
        virkailija: Boolean = false,
        vastuuhenkilo: Boolean = false
    ): KoejaksoTila {
        if (!loppukeskusteluHyvaksytty) return EI_AKTIIVINEN
        if (arvio == null) return UUSI
        return maaritaArvionKasittelytila(
            vastuuhenkiloHyvaksynyt = arvio.vastuuhenkilo?.sopimusHyvaksytty == true,
            odottaaVastuuhenkiloa = arvio.virkailija?.sopimusHyvaksytty == true && !vastuuhenkilo,
            odottaaVirkailijaa = virkailija && arvio.virkailija?.sopimusHyvaksytty != true && arvio.erikoistuvanKuittausaika != null,
            palautettuVastuuhenkilolle = vastuuhenkilo && arvio.vastuuhenkilonKorjausehdotus != null,
            palautettuKorjattavaksi = arvio.erikoistuvanKuittausaika == null && (
                !arvio.virkailijanKorjausehdotus.isNullOrBlank() || !arvio.vastuuhenkilonKorjausehdotus.isNullOrBlank()
            )
        )
    }

    /** Käsittelynäkymä näyttää aiemman palautuksen myös erikoistuvan kuittauksen jälkeen. */
    fun maaritaVastuuhenkilonArvionKasittelytila(
        arvio: KoejaksonVastuuhenkilonArvio?,
        virkailija: Boolean = false,
        vastuuhenkilo: Boolean = false
    ): KoejaksoTila = maaritaArvionKasittelytila(
        vastuuhenkiloHyvaksynyt = arvio?.vastuuhenkiloHyvaksynyt == true,
        odottaaVastuuhenkiloa = arvio?.virkailijaHyvaksynyt == true && !vastuuhenkilo,
        odottaaVirkailijaa = virkailija && arvio?.virkailijaHyvaksynyt == false && arvio.erikoistuvanKuittausaika != null,
        palautettuVastuuhenkilolle = vastuuhenkilo && arvio?.vastuuhenkilonKorjausehdotus != null,
        palautettuKorjattavaksi = !arvio?.virkailijanKorjausehdotus.isNullOrBlank() || !arvio?.vastuuhenkilonKorjausehdotus.isNullOrBlank()
    )

    /** Yhteenveto kertoo suorituksen tuloksesta, ei arvioasiakirjan käsittelyn hyväksymisestä. */
    fun maaritaYhteenvetotila(
        vastuuhenkilonArvio: KoejaksonVastuuhenkilonArvio? = null,
        koulutussopimus: KoejaksonKoulutussopimus? = null,
        aloituskeskustelu: KoejaksonAloituskeskustelu? = null,
        hyvaksyttySuoritus: Boolean = false,
        tanaan: LocalDate = LocalDate.now()
    ): KoejaksoTila = when {
        hyvaksyttySuoritus || vastuuhenkilonArvio?.koejaksoHyvaksytty == true -> HYVAKSYTTY
        onkoAloitettu(koulutussopimus != null, koulutussopimus?.koejaksonAlkamispaiva, tanaan) ||
            onkoAloitettu(aloituskeskustelu != null, aloituskeskustelu?.koejaksonAlkamispaiva, tanaan) -> ODOTTAA_HYVAKSYNTAA
        else -> EI_AKTIIVINEN
    }

    private fun maaritaKeskustelunTila(
        aktiivinen: Boolean,
        olemassa: Boolean,
        lahikouluttaja: KoejaksonKouluttajaDTO?,
        lahiesimies: KoejaksonKouluttajaDTO?,
        korjausehdotus: String?,
        kayttajaId: Long?
    ): KoejaksoTila = when {
        !aktiivinen -> EI_AKTIIVINEN
        !olemassa -> UUSI
        lahiesimies?.sopimusHyvaksytty == true -> HYVAKSYTTY
        lahikouluttaja?.sopimusHyvaksytty == true && lahikouluttaja.id == kayttajaId -> ODOTTAA_ESIMIEHEN_HYVAKSYNTAA
        !korjausehdotus.isNullOrBlank() && (lahikouluttaja?.id == kayttajaId || lahiesimies?.id == kayttajaId) -> PALAUTETTU_KORJATTAVAKSI
        else -> ODOTTAA_HYVAKSYNTAA
    }

    private fun maaritaArvionKasittelytila(
        vastuuhenkiloHyvaksynyt: Boolean,
        odottaaVastuuhenkiloa: Boolean,
        odottaaVirkailijaa: Boolean,
        palautettuVastuuhenkilolle: Boolean,
        palautettuKorjattavaksi: Boolean
    ): KoejaksoTila = when {
        vastuuhenkiloHyvaksynyt -> HYVAKSYTTY
        odottaaVastuuhenkiloa -> ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA
        odottaaVirkailijaa -> ODOTTAA_HYVAKSYNTAA
        palautettuVastuuhenkilolle || palautettuKorjattavaksi -> PALAUTETTU_KORJATTAVAKSI
        else -> ODOTTAA_HYVAKSYNTAA
    }

    // Olemassa oleva vaihe ilman aloituspäivää tulkitaan edelleen alkaneeksi.
    private fun onkoAloitettu(olemassa: Boolean, alkamispaiva: LocalDate?, tanaan: LocalDate): Boolean =
        olemassa && (alkamispaiva?.isAfter(tanaan) != true)
}
