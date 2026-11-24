package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional


interface AsiakirjaCleanupService {
    fun cleanAsiakirjaContent(asiakirja: Asiakirja)
}

@Component
class AsiakirjaCleanupServiceImpl(
) : AsiakirjaCleanupService {

    @Transactional(propagation = Propagation.MANDATORY)
    override fun cleanAsiakirjaContent(asiakirja: Asiakirja) {
        asiakirja.asiakirjaData?.data = null
        asiakirja.nimi = ""
    }
}
