package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.Valmistumispyynto
import java.time.LocalDate

class ValmistumispyyntoHelper {

    companion object {

        private const val DEFAULT_OSAAMISEN_ARVIOIJA_KORJAUSEHDOTUS = "osaamisen arvioija korjausehdotus"
        private const val DEFAULT_VIRKAILIJA_KORJAUSEHDOTUS = "virkailija korjausehdotus"
        private const val DEFAULT_HYVAKSYJA_KORJAUSEHDOTUS = "hyvaksyja korjausehdotus"
        private const val DEFAULT_VIRKAILIJAN_SAATE = "virkailijan saate"

        @JvmStatic
        fun createValmistumispyyntoOdottaaArviointia(
            opintooikeus: Opintooikeus,
            erikoistujanKuittausaika: LocalDate? = LocalDate.now()
        ): Valmistumispyynto {
            val valmistumispyynto = Valmistumispyynto(
                opintooikeus = opintooikeus,
                erikoistujanKuittausaika = erikoistujanKuittausaika
            )
            return valmistumispyynto
        }

        @JvmStatic
        fun createValmistumispyyntoOsaamisenArviointiPalautettu(
            opintooikeus: Opintooikeus,
            vastuuhenkiloOsaamisenArvioija: Kayttaja,
            vastuuhenkiloOsaamisenArvioijaPalautusaika: LocalDate? = LocalDate.now()
        ): Valmistumispyynto {
            val valmistumispyynto = Valmistumispyynto(
                opintooikeus = opintooikeus,
                vastuuhenkiloOsaamisenArvioija = vastuuhenkiloOsaamisenArvioija,
                vastuuhenkiloOsaamisenArvioijaPalautusaika = vastuuhenkiloOsaamisenArvioijaPalautusaika,
                vastuuhenkiloOsaamisenArvioijaKorjausehdotus = DEFAULT_OSAAMISEN_ARVIOIJA_KORJAUSEHDOTUS
            )
            return valmistumispyynto
        }

        @JvmStatic
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

        @JvmStatic
        fun createValmistumispyyntoVirkailijanTarkastusPalautettu(
            opintooikeus: Opintooikeus,
            virkailija: Kayttaja,
            virkailijanPalautusaika: LocalDate? = LocalDate.now()
        ): Valmistumispyynto {
            val valmistumispyynto = Valmistumispyynto(
                opintooikeus = opintooikeus,
                virkailija = virkailija,
                virkailijanPalautusaika = virkailijanPalautusaika,
                virkailijanKorjausehdotus = DEFAULT_VIRKAILIJA_KORJAUSEHDOTUS
            )
            return valmistumispyynto
        }

        @JvmStatic
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

        @JvmStatic
        fun createValmistumispyyntoHyvaksyntaPalautettu(
            opintooikeus: Opintooikeus,
            vastuuhenkiloHyvaksyja: Kayttaja,
            vastuuhenkiloHyvaksyjaPalautusaika: LocalDate? = LocalDate.now()
        ): Valmistumispyynto {
            val valmistumispyynto = Valmistumispyynto(
                opintooikeus = opintooikeus,
                vastuuhenkiloHyvaksyja = vastuuhenkiloHyvaksyja,
                vastuuhenkiloHyvaksyjaPalautusaika = vastuuhenkiloHyvaksyjaPalautusaika,
                vastuuhenkiloHyvaksyjaKorjausehdotus = DEFAULT_HYVAKSYJA_KORJAUSEHDOTUS
            )
            return valmistumispyynto
        }

        @JvmStatic
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
                vastuuhenkiloHyvaksyjaKorjausehdotus = DEFAULT_HYVAKSYJA_KORJAUSEHDOTUS
            )
            return valmistumispyynto
        }

        @JvmStatic
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
                vastuuhenkiloHyvaksyjaKorjausehdotus = DEFAULT_HYVAKSYJA_KORJAUSEHDOTUS
            )
            return valmistumispyynto
        }

        @JvmStatic
        fun createValmistumispyyntoOdottaaAllekirjoituksia(
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
                virkailijanSaate = DEFAULT_VIRKAILIJAN_SAATE,
                vastuuhenkiloHyvaksyja = vastuuhenkiloHyvaksyja,
                vastuuhenkiloHyvaksyjaKuittausaika = vastuuhenkiloHyvaksyjaKuittausaika
            )
            return valmistumispyynto
        }

        @JvmStatic
        fun createValmistumispyyntoAllekirjoitettu(
            opintooikeus: Opintooikeus,
            vastuuhenkiloOsaamisenArvioija: Kayttaja,
            virkailija: Kayttaja,
            vastuuhenkiloHyvaksyja: Kayttaja,
            erikoistujanKuittausaika: LocalDate? = LocalDate.now(),
            vastuuhenkiloOsaamisenArvioijaKuittausaika: LocalDate? = LocalDate.now(),
            virkailijanKuittausaika: LocalDate? = LocalDate.now(),
            vastuuhenkiloHyvaksyjaKuittausaika: LocalDate? = LocalDate.now(),
            allekirjoitusaika: LocalDate? = LocalDate.now()
        ): Valmistumispyynto {
            val valmistumispyynto = Valmistumispyynto(
                opintooikeus = opintooikeus,
                erikoistujanKuittausaika = erikoistujanKuittausaika,
                vastuuhenkiloOsaamisenArvioija = vastuuhenkiloOsaamisenArvioija,
                vastuuhenkiloOsaamisenArvioijaKuittausaika = vastuuhenkiloOsaamisenArvioijaKuittausaika,
                virkailija = virkailija,
                virkailijanKuittausaika = virkailijanKuittausaika,
                virkailijanSaate = DEFAULT_VIRKAILIJAN_SAATE,
                vastuuhenkiloHyvaksyja = vastuuhenkiloHyvaksyja,
                vastuuhenkiloHyvaksyjaKuittausaika = vastuuhenkiloHyvaksyjaKuittausaika,
                allekirjoitusaika = allekirjoitusaika
            )
            return valmistumispyynto
        }
    }
}
