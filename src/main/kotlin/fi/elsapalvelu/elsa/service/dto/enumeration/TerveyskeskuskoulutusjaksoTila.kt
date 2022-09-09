package fi.elsapalvelu.elsa.service.dto.enumeration

import fi.elsapalvelu.elsa.domain.TerveyskeskuskoulutusjaksonHyvaksynta

enum class TerveyskeskuskoulutusjaksoTila {
    ODOTTAA_VIRKAILIJAN_TARKISTUSTA,
    ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA,
    PALAUTETTU_KORJATTAVAKSI,
    HYVAKSYTTY;

    companion object {
        fun fromHyvaksynta(hyvaksynta: TerveyskeskuskoulutusjaksonHyvaksynta?): TerveyskeskuskoulutusjaksoTila {
            return if (hyvaksynta?.vastuuhenkiloHyvaksynyt == true) HYVAKSYTTY
            else if (!hyvaksynta?.korjausehdotus.isNullOrBlank()) PALAUTETTU_KORJATTAVAKSI
            else if (hyvaksynta?.virkailijaHyvaksynyt == true) ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA
            else ODOTTAA_VIRKAILIJAN_TARKISTUSTA
        }
    }
}
