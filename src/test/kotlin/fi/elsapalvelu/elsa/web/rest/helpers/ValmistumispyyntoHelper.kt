package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.Valmistumispyynto
import java.time.LocalDate

object ValmistumispyyntoHelper {

    private const val DEFAULT_VIRKAILIJA_KORJAUSEHDOTUS =
        "virkailija korjausehdotus"

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
}
