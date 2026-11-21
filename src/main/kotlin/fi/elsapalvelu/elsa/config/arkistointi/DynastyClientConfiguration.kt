package fi.elsapalvelu.elsa.config.arkistointi

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyClient
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyArchiveProfile
import java.time.Clock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Each destination owns its client, connection pool and token cache. */
@Configuration
class DynastyClientConfiguration {
    @Bean
    fun turkuDynastyClient(properties: ApplicationProperties, mapper: ObjectMapper, clock: Clock): DynastyClient =
        DynastyClient(DynastyArchiveProfile.TURKU.archiveProperties(properties.getArkistointi()).dynasty, mapper, clock)

    @Bean
    fun uefDynastyClient(properties: ApplicationProperties, mapper: ObjectMapper, clock: Clock): DynastyClient =
        DynastyClient(DynastyArchiveProfile.UEF.archiveProperties(properties.getArkistointi()).dynasty, mapper, clock)
}
