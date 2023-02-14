package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.OpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.dto.OpintotietodataDTO
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

private const val studentEndpoint = "student"

@Service
class PeppiTurkuOpintotietodataFetchingServiceImpl(
    @Qualifier("PeppiTurku") private val peppiTurkuClientBuilder: OkHttpClientBuilder,
    private val commonOpintotietodataFetchingServiceImpl: PeppiCommonOpintotietodataFetchingServiceImpl,
    private val applicationProperties: ApplicationProperties,
    private val yliopistoRepository: YliopistoRepository
) : OpintotietodataFetchingService {

    override suspend fun fetchOpintotietodata(hetu: String): OpintotietodataDTO? {
        val endpointBaseUrl = "${applicationProperties.getSecurity().getPeppiTurku().endpointUrl!!}/$studentEndpoint"
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
