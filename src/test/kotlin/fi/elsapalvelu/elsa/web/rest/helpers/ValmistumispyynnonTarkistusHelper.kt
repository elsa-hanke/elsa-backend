package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.ValmistumispyynnonTarkistus
import fi.elsapalvelu.elsa.domain.Valmistumispyynto
import java.time.LocalDate

class ValmistumispyynnonTarkistusHelper {

    companion object {

        private val DEFAULT_YEK_SUORITUSPAIVA = LocalDate.ofEpochDay(5)
        private val DEFAULT_PTL_SUORITUSPAIVA = LocalDate.ofEpochDay(10)
        private val DEFAULT_AIEMPI_EL_SUORITUSPAIVA = LocalDate.ofEpochDay(15)
        private val DEFAULT_LT_TUTKINTO_SUORITUSPAIVA = LocalDate.ofEpochDay(15)
        private const val DEFAULT_KOMMENTIT_VIRKAILIJOILLE = "kommentit virkailijoille"

        @JvmStatic
        fun createValmistumispyynnonTarkistusOdottaaHyvaksyntaa(
            valmistumispyynto: Valmistumispyynto
        ): ValmistumispyynnonTarkistus {
            return ValmistumispyynnonTarkistus(
                valmistumispyynto = valmistumispyynto,
                yekSuoritettu = true,
                yekSuorituspaiva = DEFAULT_YEK_SUORITUSPAIVA,
                ptlSuoritettu = true,
                ptlSuorituspaiva = DEFAULT_PTL_SUORITUSPAIVA,
                aiempiElKoulutusSuoritettu = true,
                aiempiElKoulutusSuorituspaiva = DEFAULT_AIEMPI_EL_SUORITUSPAIVA,
                ltTutkintoSuoritettu = true,
                ltTutkintoSuorituspaiva = DEFAULT_LT_TUTKINTO_SUORITUSPAIVA,
                yliopistosairaalanUlkopuolinenTyoTarkistettu = true,
                yliopistosairaalatyoTarkistettu = true,
                kokonaistyoaikaTarkistettu = true,
                teoriakoulutusTarkistettu = true,
                kommentitVirkailijoille = DEFAULT_KOMMENTIT_VIRKAILIJOILLE
            )
        }
    }
}
