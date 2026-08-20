package fi.elsapalvelu.elsa.service.integration.peppi.turku

import fi.elsapalvelu.elsa.required

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.perustiedot.YliopistoRepository
import fi.elsapalvelu.elsa.service.integration.AbstractOpintotietodataFetchingService
import fi.elsapalvelu.elsa.service.integration.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.dto.koulutus.OpintotietodataDTO
import fi.elsapalvelu.elsa.service.integration.peppi.PeppiCommonOpintotietodataFetchingServiceImpl
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class PeppiTurkuOpintotietodataFetchingServiceImpl(
    @Qualifier("PeppiTurku") private val peppiTurkuClientBuilder: OkHttpClientBuilder,
    private val commonOpintotietodataFetchingServiceImpl: PeppiCommonOpintotietodataFetchingServiceImpl,
    private val applicationProperties: ApplicationProperties,
    yliopistoRepository: YliopistoRepository
) : AbstractOpintotietodataFetchingService(yliopistoRepository, YliopistoEnum.TURUN_YLIOPISTO) {

    override suspend fun fetchOpintotietodata(hetu: String): OpintotietodataDTO? {
        val endpointBaseUrl = "${applicationProperties.getSecurity().getPeppiTurku().endpointUrl.required()}/student"
        return commonOpintotietodataFetchingServiceImpl.fetchOpintotietodata(
            endpointBaseUrl,
            peppiTurkuClientBuilder.okHttpClient(),
            hetu,
            YliopistoEnum.TURUN_YLIOPISTO
        )
    }
}
