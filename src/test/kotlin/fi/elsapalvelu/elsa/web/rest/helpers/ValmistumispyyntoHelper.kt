package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.Valmistumispyynto
import java.time.LocalDate

object ValmistumispyyntoHelper {

    private const val DEFAULT_OSAAMISEN_ARVIOIJA_KORJAUSEHDOTUS = "osaamisen arvioija korjausehdotus"
    private const val DEFAULT_VIRKAILIJA_KORJAUSEHDOTUS = "virkailija korjausehdotus"
    private const val DEFAULT_HYVAKSYJA_KORJAUSEHDOTUS = "hyvaksyja korjausehdotus"
    private const val DEFAULT_VIRKAILIJAN_SAATE = "virkailijan saate"

    private fun baseValmistumispyynto(
        opintooikeus: Opintooikeus
    ) = Valmistumispyynto(
        opintooikeus = opintooikeus
    )

    fun createValmistumispyyntoOdottaaArviointia(
        opintooikeus: Opintooikeus,
        erikoistujanKuittausaika: LocalDate? = LocalDate.now()
    ): Valmistumispyynto =
        baseValmistumispyynto(opintooikeus).apply {
            this.erikoistujanKuittausaika = erikoistujanKuittausaika
        }

    fun createValmistumispyyntoVirkailijanTarkastusPalautettu(
        opintooikeus: Opintooikeus,
        virkailija: Kayttaja,
        virkailijanPalautusaika: LocalDate? = LocalDate.now()
    ): Valmistumispyynto =
        baseValmistumispyynto(opintooikeus).apply {
            this.virkailija = virkailija
            this.virkailijanPalautusaika = virkailijanPalautusaika
            this.virkailijanKorjausehdotus =
                DEFAULT_VIRKAILIJA_KORJAUSEHDOTUS
        }

    fun createValmistumispyyntoOdottaaHyvaksyntaa(
        opintooikeus: Opintooikeus,
        vastuuhenkiloOsaamisenArvioija: Kayttaja,
        virkailija: Kayttaja,
        vastuuhenkiloOsaamisenArvioijaKuittausaika: LocalDate? = LocalDate.now(),
        virkailijanKuittausaika: LocalDate? = LocalDate.now()
    ): Valmistumispyynto {
        val valmistumispyynto = Valmistumispyynto(
            opintooikeus = opintooikeus,
            erikoistujanKuittausaika = LocalDate.now(),
            vastuuhenkiloOsaamisenArvioija = vastuuhenkiloOsaamisenArvioija,
            vastuuhenkiloOsaamisenArvioijaKuittausaika = vastuuhenkiloOsaamisenArvioijaKuittausaika,
            virkailija = virkailija,
            virkailijanKuittausaika = virkailijanKuittausaika,
            virkailijanSaate = DEFAULT_VIRKAILIJAN_SAATE
        )
        return valmistumispyynto
    }

    fun createValmistumispyyntoOsaamisenArviointiPalautettu(
        opintooikeus: Opintooikeus,
        vastuuhenkiloOsaamisenArvioija: Kayttaja,
        vastuuhenkiloOsaamisenArvioijaPalautusaika: LocalDate? = LocalDate.now()
    ): Valmistumispyynto {
        val valmistumispyynto = Valmistumispyynto(
            opintooikeus = opintooikeus,
            vastuuhenkiloOsaamisenArvioija = vastuuhenkiloOsaamisenArvioija,
            vastuuhenkiloOsaamisenArvioijaPalautusaika = vastuuhenkiloOsaamisenArvioijaPalautusaika,
            vastuuhenkiloOsaamisenArvioijaKorjausehdotus = ValmistumispyyntoHelper.DEFAULT_OSAAMISEN_ARVIOIJA_KORJAUSEHDOTUS
        )
        return valmistumispyynto
    }

    fun createValmistumispyyntoOdottaaVirkailijanTarkastusta(
        opintooikeus: Opintooikeus,
        vastuuhenkiloOsaamisenArvioija: Kayttaja,
        erikoistujanKuittausaika: LocalDate? = LocalDate.now(),
        vastuuhenkiloOsaamisenArvioijaKuittausaika: LocalDate? = LocalDate.now()
    ): Valmistumispyynto {
        val valmistumispyynto = Valmistumispyynto(
            opintooikeus = opintooikeus,
            erikoistujanKuittausaika = erikoistujanKuittausaika,
            vastuuhenkiloOsaamisenArvioija = vastuuhenkiloOsaamisenArvioija,
            vastuuhenkiloOsaamisenArvioijaKuittausaika = vastuuhenkiloOsaamisenArvioijaKuittausaika
        )
        return valmistumispyynto
    }

    fun createValmistumispyyntoHyvaksyntaPalautettu(
        opintooikeus: Opintooikeus,
        vastuuhenkiloHyvaksyja: Kayttaja,
        vastuuhenkiloHyvaksyjaPalautusaika: LocalDate? = LocalDate.now()
    ): Valmistumispyynto {
        val valmistumispyynto = Valmistumispyynto(
            opintooikeus = opintooikeus,
            vastuuhenkiloHyvaksyja = vastuuhenkiloHyvaksyja,
            vastuuhenkiloHyvaksyjaPalautusaika = vastuuhenkiloHyvaksyjaPalautusaika,
            vastuuhenkiloHyvaksyjaKorjausehdotus = ValmistumispyyntoHelper.DEFAULT_HYVAKSYJA_KORJAUSEHDOTUS
        )
        return valmistumispyynto
    }

    fun createValmistumispyyntoHyvaksyntaTarkastusAndOsaamisenArviointiPalautettu(
        opintooikeus: Opintooikeus,
        vastuuhenkiloOsaamisenArvioija: Kayttaja,
        vastuuhenkiloOsaamisenArvioijaPalautusaika: LocalDate? = LocalDate.now(),
        virkailija: Kayttaja,
        virkailijanPalautusaika: LocalDate? = LocalDate.now(),
        vastuuhenkiloHyvaksyja: Kayttaja,
        vastuuhenkiloHyvaksyjaPalautusaika: LocalDate? = LocalDate.now()
    ): Valmistumispyynto {
        val valmistumispyynto = Valmistumispyynto(
            opintooikeus = opintooikeus,
            vastuuhenkiloOsaamisenArvioija = vastuuhenkiloOsaamisenArvioija,
            vastuuhenkiloOsaamisenArvioijaPalautusaika = vastuuhenkiloOsaamisenArvioijaPalautusaika,
            virkailija = virkailija,
            virkailijanPalautusaika = virkailijanPalautusaika,
            vastuuhenkiloHyvaksyja = vastuuhenkiloHyvaksyja,
            vastuuhenkiloHyvaksyjaPalautusaika = vastuuhenkiloHyvaksyjaPalautusaika,
            vastuuhenkiloHyvaksyjaKorjausehdotus = ValmistumispyyntoHelper.DEFAULT_HYVAKSYJA_KORJAUSEHDOTUS
        )
        return valmistumispyynto
    }

    fun createValmistumispyyntoHyvaksytty(
        opintooikeus: Opintooikeus,
        vastuuhenkiloOsaamisenArvioija: Kayttaja,
        virkailija: Kayttaja,
        vastuuhenkiloHyvaksyja: Kayttaja,
        erikoistujanKuittausaika: LocalDate? = LocalDate.now(),
        vastuuhenkiloOsaamisenArvioijaKuittausaika: LocalDate? = LocalDate.now(),
        virkailijanKuittausaika: LocalDate? = LocalDate.now(),
        vastuuhenkiloHyvaksyjaKuittausaika: LocalDate? = LocalDate.now()
    ): Valmistumispyynto {
        val valmistumispyynto = Valmistumispyynto(
            opintooikeus = opintooikeus,
            erikoistujanKuittausaika = erikoistujanKuittausaika,
            vastuuhenkiloOsaamisenArvioija = vastuuhenkiloOsaamisenArvioija,
            vastuuhenkiloOsaamisenArvioijaKuittausaika = vastuuhenkiloOsaamisenArvioijaKuittausaika,
            virkailija = virkailija,
            virkailijanKuittausaika = virkailijanKuittausaika,
            virkailijanSaate = ValmistumispyyntoHelper.DEFAULT_VIRKAILIJAN_SAATE,
            vastuuhenkiloHyvaksyja = vastuuhenkiloHyvaksyja,
            vastuuhenkiloHyvaksyjaKuittausaika = vastuuhenkiloHyvaksyjaKuittausaika
        )
        return valmistumispyynto
    }

    fun createValmistumispyyntoHyvaksyntaAndTarkastusPalautettu(
        opintooikeus: Opintooikeus,
        virkailija: Kayttaja,
        virkailijanPalautusaika: LocalDate? = LocalDate.now(),
        vastuuhenkiloHyvaksyja: Kayttaja,
        vastuuhenkiloHyvaksyjaPalautusaika: LocalDate? = LocalDate.now()
    ): Valmistumispyynto {
        val valmistumispyynto = Valmistumispyynto(
            opintooikeus = opintooikeus,
            virkailija = virkailija,
            virkailijanPalautusaika = virkailijanPalautusaika,
            vastuuhenkiloHyvaksyja = vastuuhenkiloHyvaksyja,
            vastuuhenkiloHyvaksyjaPalautusaika = vastuuhenkiloHyvaksyjaPalautusaika,
            vastuuhenkiloHyvaksyjaKorjausehdotus = ValmistumispyyntoHelper.DEFAULT_HYVAKSYJA_KORJAUSEHDOTUS
        )
        return valmistumispyynto
    }

}
