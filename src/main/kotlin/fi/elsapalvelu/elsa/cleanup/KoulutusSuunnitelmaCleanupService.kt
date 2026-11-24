package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.domain.koulutus.Koulutussuunnitelma
import org.springframework.stereotype.Component

@Component
class KoulutusSuunnitelmaCleanupService(
    private val asiakirjaCleanupService: AsiakirjaCleanupService
) {

    fun clearKoulutusSuunnitelma(koulutusSuunnitelma: Koulutussuunnitelma) {
        koulutusSuunnitelma.koulutussuunnitelmaAsiakirja
    }
}
