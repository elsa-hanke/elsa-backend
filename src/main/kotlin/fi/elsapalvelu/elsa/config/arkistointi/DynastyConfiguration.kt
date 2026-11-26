package fi.elsapalvelu.elsa.config.arkistointi

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyClient
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyArchiveProfile
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyArkistointiAdapterFactory
import java.time.Clock
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

/** Each destination owns its client, connection pool and token cache. */
@Configuration
class DynastyConfiguration {
    @Bean
    fun turkuDynastyClient(properties: ApplicationProperties, mapper: ObjectMapper, clock: Clock): DynastyClient =
        DynastyClient(DynastyArchiveProfile.TURKU.archiveProperties(properties.getArkistointi()).dynasty, mapper, clock)

    @Bean
    fun uefDynastyClient(properties: ApplicationProperties, mapper: ObjectMapper, clock: Clock): DynastyClient =
        DynastyClient(DynastyArchiveProfile.UEF.archiveProperties(properties.getArkistointi()).dynasty, mapper, clock)

    @Bean("turkuDynastyArkistointiAdapter")
    @Profile("!e2e-archive")
    internal fun turkuDynastyArkistointiAdapter(
        @Qualifier("turkuDynastyClient") client: DynastyClient,
        factory: DynastyArkistointiAdapterFactory
    ): DynastyArkistointiAdapter = factory.create(DynastyArchiveProfile.TURKU, client)

    @Bean("uefDynastyArkistointiAdapter")
    @Profile("!e2e-archive")
    internal fun uefDynastyArkistointiAdapter(
        @Qualifier("uefDynastyClient") client: DynastyClient,
        factory: DynastyArkistointiAdapterFactory
    ): DynastyArkistointiAdapter = factory.create(DynastyArchiveProfile.UEF, client)
}
