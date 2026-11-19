package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.UefDynastyMetadataMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!e2e-archive")
class UefDynastyArkistointiAdapter(
    @Qualifier("uefDynastyClient") client: DynastyClient,
    metadata: UefDynastyMetadataMapper,
    steps: DynastyStepExecutor
) : DynastyArkistointiAdapter(YliopistoEnum.ITA_SUOMEN_YLIOPISTO, client, metadata, steps, parallelDocuments = true)
