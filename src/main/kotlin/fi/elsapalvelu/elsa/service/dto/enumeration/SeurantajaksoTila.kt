package fi.elsapalvelu.elsa.service.dto.enumeration

import fi.elsapalvelu.elsa.domain.Seurantajakso
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
            return haeTila(
                seurantajaksoDTO.hyvaksytty,
                seurantajaksoDTO.korjausehdotus,
                seurantajaksoDTO.kouluttajanArvio,
                seurantajaksoDTO.seurantakeskustelunYhteisetMerkinnat
            )
        }

        fun fromSeurantajakso(seurantajakso: Seurantajakso): SeurantajaksoTila {
            return haeTila(
                seurantajakso.hyvaksytty,
                seurantajakso.korjausehdotus,
                seurantajakso.kouluttajanArvio,
                seurantajakso.seurantakeskustelunYhteisetMerkinnat
            )
        }

        private fun haeTila(
            hyvaksytty: Boolean?,
            korjausehdotus: String?,
            kouluttajanArvio: String?,
            seurantakeskustelunYhteisetMerkinnat: String?
        ): SeurantajaksoTila {
            return if (hyvaksytty == true) HYVAKSYTTY
            else if (!korjausehdotus.isNullOrBlank()) PALAUTETTU_KORJATTAVAKSI
            else if (kouluttajanArvio == null && seurantakeskustelunYhteisetMerkinnat == null) ODOTTAA_ARVIOINTIA_JA_YHTEISIA_MERKINTOJA
            else if (kouluttajanArvio == null) ODOTTAA_ARVIOINTIA
            else if (seurantakeskustelunYhteisetMerkinnat == null) ODOTTAA_YHTEISIA_MERKINTOJA
            else ODOTTAA_HYVAKSYNTAA
        }
    }
}
