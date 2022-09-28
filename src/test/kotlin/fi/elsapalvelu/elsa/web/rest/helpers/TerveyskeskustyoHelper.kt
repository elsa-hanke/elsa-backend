package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.TerveyskeskuskoulutusjaksonHyvaksynta
import java.time.LocalDate

class TerveyskeskustyoHelper {

    companion object {

        private val DEFAULT_KUITTAUSAIKA = LocalDate.ofEpochDay(7)

        @JvmStatic
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
}
