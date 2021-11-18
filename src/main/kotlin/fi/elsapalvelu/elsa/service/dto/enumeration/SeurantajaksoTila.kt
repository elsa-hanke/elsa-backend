package fi.elsapalvelu.elsa.service.dto.enumeration

import fi.elsapalvelu.elsa.service.dto.*

enum class SeurantajaksoTila {
    ODOTTAA_ARVIOINTIA,
    ODOTTAA_YHTEISIA_MERKINTOJA,
    ODOTTAA_ARVIOINTIA_JA_YHTEISIA_MERKINTOJA,
    ODOTTAA_HYVAKSYNTAA,
    PALAUTETTU_KORJATTAVAKSI,
    HYVAKSYTTY;

    companion object {
        fun fromSeurantajakso(seurantajaksoDTO: SeurantajaksoDTO): SeurantajaksoTila {
            return if (seurantajaksoDTO.hyvaksytty == true) HYVAKSYTTY
            else if (!seurantajaksoDTO.korjausehdotus.isNullOrBlank()) PALAUTETTU_KORJATTAVAKSI
            else if (seurantajaksoDTO.kouluttajanArvio == null && seurantajaksoDTO.seurantakeskustelunYhteisetMerkinnat == null) ODOTTAA_ARVIOINTIA_JA_YHTEISIA_MERKINTOJA
            else if (seurantajaksoDTO.kouluttajanArvio == null) ODOTTAA_ARVIOINTIA
            else if (seurantajaksoDTO.seurantakeskustelunYhteisetMerkinnat == null) ODOTTAA_YHTEISIA_MERKINTOJA
            else ODOTTAA_HYVAKSYNTAA
        }
    }
}
