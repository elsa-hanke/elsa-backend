package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.dto.OpintotietodataDTO
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

private const val studentEndpoint = "elsa-1"

@Service
class PeppiUefOpintotietodataFetchingServiceImpl(
    @Qualifier("PeppiUef") private val peppiUefClientBuilder: OkHttpClientBuilder,
    private val commonOpintotietodataFetchingServiceImpl: PeppiCommonOpintotietodataFetchingServiceImpl,
    private val applicationProperties: ApplicationProperties,
    private val yliopistoRepository: YliopistoRepository
) : OpintotietodataFetchingService {

    override suspend fun fetchOpintotietodata(hetu: String): OpintotietodataDTO? {
        val endpointBaseUrl = "${applicationProperties.getSecurity().getPeppiUef().endpointUrl!!}/$studentEndpoint"
        return commonOpintotietodataFetchingServiceImpl.fetchOpintotietodata(
            endpointBaseUrl,
            peppiUefClientBuilder.okHttpClient(),
            hetu,
            YliopistoEnum.ITA_SUOMEN_YLIOPISTO
        )
    }

    override fun shouldFetchOpintotietodata(): Boolean {
        return yliopistoRepository.findOneByNimi(YliopistoEnum.ITA_SUOMEN_YLIOPISTO)?.haeOpintotietodata == true
    }

    override fun getYliopisto(): YliopistoEnum {
        return YliopistoEnum.ITA_SUOMEN_YLIOPISTO
    }
}
