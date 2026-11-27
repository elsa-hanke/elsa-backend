package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.domain.koulutus.Koulutusjakso
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

interface KoulutusjaksoCleanupService {
    fun cleanKoulutusjaksot(koulutusjaksot: Set<Koulutusjakso>?)
    fun cleanKoulutusjakso(koulutusjakso: Koulutusjakso)

}

@Component
class KoulutusjaksoCleanupServiceImpl : KoulutusjaksoCleanupService {
    private final val log = LoggerFactory.getLogger(KoulutusjaksoCleanupServiceImpl::class.java)

    override fun cleanKoulutusjaksot(koulutusjaksot: Set<Koulutusjakso>?) {
        koulutusjaksot?.forEach { cleanKoulutusjakso(it) }
    }

    override fun cleanKoulutusjakso(koulutusjakso: Koulutusjakso) {

        koulutusjakso.koulutussuunnitelma
        koulutusjakso.nimi
        koulutusjakso.tyoskentelyjaksot
        koulutusjakso.muutOsaamistavoitteet
        koulutusjakso.osaamistavoitteet
    }
}
