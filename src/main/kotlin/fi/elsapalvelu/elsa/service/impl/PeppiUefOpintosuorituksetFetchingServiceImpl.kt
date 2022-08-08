package fi.elsapalvelu.elsa.service.impl

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.YliopistoRepository
import fi.elsapalvelu.elsa.service.OkHttpClientBuilder
import fi.elsapalvelu.elsa.service.OpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.PeppiCommonOpintosuorituksetFetchingService
import fi.elsapalvelu.elsa.service.dto.OpintosuorituksetPersistenceDTO
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

private const val studyAccomplishmentsEndpoint = "elsa-2"

@Service
class PeppiUefOpintosuorituksetFetchingServiceImpl(
    @Qualifier("PeppiUef") private val peppiUefClientBuilder: OkHttpClientBuilder,
    private val commonOpintosuorituksetFetchingService: PeppiCommonOpintosuorituksetFetchingService,
    private val applicationProperties: ApplicationProperties,
    private val yliopistoRepository: YliopistoRepository
) : OpintosuorituksetFetchingService {

    override suspend fun fetchOpintosuoritukset(hetu: String): OpintosuorituksetPersistenceDTO? {
        val endpointBaseUrl =
            "${applicationProperties.getSecurity().getPeppiUef().endpointUrl!!}/${studyAccomplishmentsEndpoint}"
        return commonOpintosuorituksetFetchingService.fetchOpintosuoritukset(
            endpointBaseUrl,
            peppiUefClientBuilder.okHttpClient(),
            hetu,
            YliopistoEnum.ITA_SUOMEN_YLIOPISTO
        )
    }

    override fun shouldFetchOpintosuoritukset(): Boolean {
        return yliopistoRepository.findOneByNimi(YliopistoEnum.ITA_SUOMEN_YLIOPISTO)?.haeOpintotietodata == true
    }
}
