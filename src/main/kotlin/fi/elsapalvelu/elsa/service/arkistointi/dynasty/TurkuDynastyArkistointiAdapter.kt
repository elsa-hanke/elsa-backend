package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.TurkuDynastyMetadataMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!e2e-archive")
class TurkuDynastyArkistointiAdapter(
    @Qualifier("turkuDynastyClient") client: DynastyClient,
    metadata: TurkuDynastyMetadataMapper,
    steps: DynastyStepExecutor
) : DynastyArkistointiAdapter(YliopistoEnum.TURUN_YLIOPISTO, client, metadata, steps, parallelDocuments = false)
