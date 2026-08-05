package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksynta
import java.time.LocalDate

object TerveyskeskustyoHelper {

    private val DEFAULT_KUITTAUSAIKA = LocalDate.ofEpochDay(7)

    fun createTerveyskeskustyoHyvaksyntaHyvaksytty(
        opintooikeus: Opintooikeus,
        hyvaksyttyPvm: LocalDate? = DEFAULT_KUITTAUSAIKA
    ): TerveyskeskuskoulutusjaksonHyvaksynta {
        return TerveyskeskuskoulutusjaksonHyvaksynta(
            opintooikeus = opintooikeus,
            vastuuhenkiloHyvaksynyt = true,
            vastuuhenkilonKuittausaika = hyvaksyttyPvm
        )
    }
}
