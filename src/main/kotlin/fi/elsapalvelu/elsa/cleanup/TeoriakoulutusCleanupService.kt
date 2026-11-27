package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.domain.koulutus.Teoriakoulutus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

interface  TeoriakoulutusCleanupService {

    fun cleanTeoriakoulutukset(teoriakoulutukset: Set<Teoriakoulutus>)

    fun cleanTeoriakoulutus(teoriakoulutus: Teoriakoulutus)
}

@Component
class TeoriakoulutusCleanupServiceImpl(
    private val asiakirjaCleanupService: AsiakirjaCleanupService
) : TeoriakoulutusCleanupService {

    private final val log = LoggerFactory.getLogger(TeoriakoulutusCleanupServiceImpl::class.java)

    override fun cleanTeoriakoulutukset(teoriakoulutukset: Set<Teoriakoulutus>) {
        teoriakoulutukset.forEach { cleanTeoriakoulutus(it) }
    }

    override fun cleanTeoriakoulutus(teoriakoulutus: Teoriakoulutus) {
        log.info("Cleaning teoriakoulutus with id: ${teoriakoulutus.id}")
        teoriakoulutus.koulutuksenNimi=""
        teoriakoulutus.koulutuksenPaikka=""
        asiakirjaCleanupService.cleanAsiakirjaContents(teoriakoulutus.todistukset)
    }
}
