package fi.elsapalvelu.elsa.cleanup


import fi.elsapalvelu.elsa.domain.koulutus.Koulutussuunnitelma
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

interface KoulutusSuunnitelmaCleanupService {
    fun clearKoulutusSuunnitelma(koulutusSuunnitelma: Koulutussuunnitelma)
}

@Component
class KoulutusSuunnitelmaCleanupServiceImpl(
    private val asiakirjaCleanupService: AsiakirjaCleanupService
) : KoulutusSuunnitelmaCleanupService {
    private final val log = LoggerFactory.getLogger(KoulutusSuunnitelmaCleanupService::class.java)

    override fun clearKoulutusSuunnitelma(koulutusSuunnitelma: Koulutussuunnitelma) {
        if( koulutusSuunnitelma.koulutussuunnitelmaAsiakirja != null) {
            asiakirjaCleanupService.cleanAsiakirjaContent(koulutusSuunnitelma.koulutussuunnitelmaAsiakirja!!)
        }
        koulutusSuunnitelma.elamankentta = ""

        // TODO koulutusSuunnitelma.koulutusjaksot
        koulutusSuunnitelma.motivaatiokirje = ""
        if( koulutusSuunnitelma.motivaatiokirjeAsiakirja != null) {
            asiakirjaCleanupService.cleanAsiakirjaContent(koulutusSuunnitelma.motivaatiokirjeAsiakirja!!)
        }

        koulutusSuunnitelma.opiskeluJaTyohistoria = ""
        koulutusSuunnitelma.osaamisenKartuttaminen = ""
        koulutusSuunnitelma.tulevaisuudenVisiointi = ""
        koulutusSuunnitelma.vahvuudet = ""
        koulutusSuunnitelma.tulevaisuudenVisiointi = ""
    }
}
