package fi.elsapalvelu.elsa.service.integration

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.perustiedot.YliopistoRepository

abstract class AbstractOpintosuorituksetFetchingService(
    private val yliopistoRepository: YliopistoRepository,
    private val yliopisto: YliopistoEnum
) : OpintosuorituksetFetchingService {

    override fun shouldFetchOpintosuoritukset(): Boolean =
        yliopistoRepository.findOneByNimi(yliopisto)?.haeOpintotietodata == true

    override fun getYliopisto(): YliopistoEnum = yliopisto
}

