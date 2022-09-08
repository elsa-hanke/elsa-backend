package fi.elsapalvelu.elsa.service.dto.enumeration

import fi.elsapalvelu.elsa.service.dto.ValmistumispyyntoDTO

enum class ValmistumispyynnonTila {
    UUSI,
    ODOTTAA_VASTUUHENKILON_TARKISTUSTA,
    VASTUUHENKILON_TARKISTUS_PALAUTETTU,
    ODOTTAA_VIRKAILIJAN_TARKISTUSTA,
    VIRKAILIJAN_TARKISTUS_PALAUTETTU,
    ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA,
    VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU,
    ODOTTAA_ALLEKIRJOITUSTA,
    ALLEKIRJOITETTU;

    companion object {
        fun fromValmistumispyyntoErikoistuja(valmistumispyyntoDTO: ValmistumispyyntoDTO?): ValmistumispyynnonTila {
            return if (valmistumispyyntoDTO?.erikoistujanKuittausaika != null)
                if (valmistumispyyntoDTO.vastuuhenkiloOsaamisenArvioijaKuittausaika == null) ODOTTAA_VASTUUHENKILON_TARKISTUSTA
                else if (valmistumispyyntoDTO.virkailijanKuittausaika == null) ODOTTAA_VIRKAILIJAN_TARKISTUSTA
                else if (valmistumispyyntoDTO.vastuuhenkiloHyvaksyjaKuittausaika == null) ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA
                else if (valmistumispyyntoDTO.allekirjoitusaika == null) ODOTTAA_ALLEKIRJOITUSTA
                else ALLEKIRJOITETTU
            else if (valmistumispyyntoDTO?.vastuuhenkiloOsaamisenArvioijaKorjausehdotus != null) VASTUUHENKILON_TARKISTUS_PALAUTETTU
            else if (valmistumispyyntoDTO?.virkailijanKorjausehdotus != null) VIRKAILIJAN_TARKISTUS_PALAUTETTU
            else if (valmistumispyyntoDTO?.vastuuhenkiloHyvaksyjaKorjausehdotus != null) VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU
            else UUSI
        }
    }
}
