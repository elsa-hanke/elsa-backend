package fi.elsapalvelu.elsa.service.integration

import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.YliopistoRepository

abstract class AbstractOpintotietodataFetchingService(
    private val yliopistoRepository: YliopistoRepository,
    private val yliopisto: YliopistoEnum
) : OpintotietodataFetchingService {

    override fun shouldFetchOpintotietodata(): Boolean =
        yliopistoRepository.findOneByNimi(yliopisto)?.haeOpintotietodata == true

    override fun getYliopisto(): YliopistoEnum = yliopisto
}

