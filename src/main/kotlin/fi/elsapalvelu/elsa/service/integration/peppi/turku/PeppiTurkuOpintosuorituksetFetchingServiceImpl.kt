package fi.elsapalvelu.elsa.service.integration.peppi.turku

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.integration.AbstractOpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.integration.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.integration.peppi.PeppiCommonOpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetPersistenceDTO
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class PeppiTurkuOpintosuorituksetFetchingServiceImpl(
    @Qualifier("PeppiTurku") private val peppiTurkuClientBuilder: OkHttpClientBuilder,
    private val commonOpintosuorituksetFetchingService: PeppiCommonOpintosuorituksetFetchingService,
    private val applicationProperties: ApplicationProperties,
    yliopistoRepository: YliopistoRepository
) : AbstractOpintosuorituksetFetchingService(yliopistoRepository, YliopistoEnum.TURUN_YLIOPISTO) {

    override suspend fun fetchOpintosuoritukset(hetu: String): OpintosuorituksetPersistenceDTO? {
        val endpointBaseUrl =
            "${applicationProperties.getSecurity().getPeppiTurku().endpointUrl!!}/study_accomplishments"
        return commonOpintosuorituksetFetchingService.fetchOpintosuoritukset(
            endpointBaseUrl,
            peppiTurkuClientBuilder.okHttpClient(),
            hetu,
            YliopistoEnum.TURUN_YLIOPISTO
        )
    }
}
