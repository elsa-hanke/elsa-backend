package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.UefDynastyMetadataMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!e2e-archive")
class UefDynastyArkistointiAdapter(
    @Qualifier("uefDynastyClient") client: DynastyClient,
    metadata: UefDynastyMetadataMapper,
    steps: DynastyStepExecutor,
    properties: ApplicationProperties
) : DynastyArkistointiAdapter(DynastyDestination.UEF, client, metadata, steps, properties)
