package fi.elsapalvelu.elsa.service.integration

import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.YliopistoRepository

abstract class AbstractOpintosuorituksetFetchingService(
    private val yliopistoRepository: YliopistoRepository,
    private val yliopisto: YliopistoEnum
) : OpintosuorituksetFetchingService {

    override fun shouldFetchOpintosuoritukset(): Boolean =
        yliopistoRepository.findOneByNimi(yliopisto)?.haeOpintotietodata == true

    override fun getYliopisto(): YliopistoEnum = yliopisto
}

