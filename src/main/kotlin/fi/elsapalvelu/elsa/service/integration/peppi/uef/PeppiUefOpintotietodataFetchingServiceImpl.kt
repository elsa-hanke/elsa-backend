package fi.elsapalvelu.elsa.service.integration.peppi.uef

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.integration.AbstractOpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.integration.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.dto.OpintotietodataDTO
import fi.elsapalvelu.elsa.service.integration.peppi.PeppiCommonOpintotietodataFetchingServiceImpl
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class PeppiUefOpintotietodataFetchingServiceImpl(
    @Qualifier("PeppiUef") private val peppiUefClientBuilder: OkHttpClientBuilder,
    private val commonOpintotietodataFetchingServiceImpl: PeppiCommonOpintotietodataFetchingServiceImpl,
    private val applicationProperties: ApplicationProperties,
    yliopistoRepository: YliopistoRepository
) : AbstractOpintotietodataFetchingService(yliopistoRepository, YliopistoEnum.ITA_SUOMEN_YLIOPISTO) {

    override suspend fun fetchOpintotietodata(hetu: String): OpintotietodataDTO? {
        val endpointBaseUrl = "${applicationProperties.getSecurity().getPeppiUef().endpointUrl!!}/elsa-1"
        return commonOpintotietodataFetchingServiceImpl.fetchOpintotietodata(
            endpointBaseUrl,
            peppiUefClientBuilder.okHttpClient(),
            hetu,
            YliopistoEnum.ITA_SUOMEN_YLIOPISTO
        )
    }
}
