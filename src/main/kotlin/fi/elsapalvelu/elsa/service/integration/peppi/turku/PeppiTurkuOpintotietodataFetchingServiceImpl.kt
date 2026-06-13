package fi.elsapalvelu.elsa.service.integration.peppi.turku

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.integration.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.integration.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.dto.OpintotietodataDTO
import fi.elsapalvelu.elsa.service.integration.peppi.PeppiCommonOpintotietodataFetchingServiceImpl
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class PeppiTurkuOpintotietodataFetchingServiceImpl(
    @Qualifier("PeppiTurku") private val peppiTurkuClientBuilder: OkHttpClientBuilder,
    private val commonOpintotietodataFetchingServiceImpl: PeppiCommonOpintotietodataFetchingServiceImpl,
    private val applicationProperties: ApplicationProperties,
    private val yliopistoRepository: YliopistoRepository
) : OpintotietodataFetchingService {

    override suspend fun fetchOpintotietodata(hetu: String): OpintotietodataDTO? {
        val endpointBaseUrl = "${applicationProperties.getSecurity().getPeppiTurku().endpointUrl!!}/student"
        return commonOpintotietodataFetchingServiceImpl.fetchOpintotietodata(
            endpointBaseUrl,
            peppiTurkuClientBuilder.okHttpClient(),
            hetu,
            YliopistoEnum.TURUN_YLIOPISTO
        )
    }

    override fun shouldFetchOpintotietodata(): Boolean {
        return yliopistoRepository.findOneByNimi(YliopistoEnum.TURUN_YLIOPISTO)?.haeOpintotietodata == true
    }

    override fun getYliopisto(): YliopistoEnum {
        return YliopistoEnum.TURUN_YLIOPISTO
    }
}
