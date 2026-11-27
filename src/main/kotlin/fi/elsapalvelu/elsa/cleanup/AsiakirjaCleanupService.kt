package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional


interface AsiakirjaCleanupService {
    fun cleanAsiakirjaContent(asiakirja: Asiakirja)
    fun cleanAsiakirjaContents(asiakirjat: Set<Asiakirja>)
}

@Component
class AsiakirjaCleanupServiceImpl(
) : AsiakirjaCleanupService {

    private final val log = LoggerFactory.getLogger(AsiakirjaCleanupServiceImpl::class.java)

    @Transactional(propagation = Propagation.MANDATORY)
    override fun cleanAsiakirjaContents(asiakirjat: Set<Asiakirja>) {
        if(asiakirjat.isEmpty()) {
            return
        }
        asiakirjat.forEach { cleanAsiakirjaContent(it) }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    override fun cleanAsiakirjaContent(asiakirja: Asiakirja) {
        log.info("Cleaning asiakirja content for asiakirja with id: ${asiakirja.id}")
        asiakirja.asiakirjaData?.data = null
        asiakirja.nimi = ""
    }
}
