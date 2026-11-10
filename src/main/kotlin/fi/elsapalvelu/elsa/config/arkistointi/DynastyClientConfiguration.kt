package fi.elsapalvelu.elsa.config.arkistointi

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Each destination owns its client, connection pool and token cache. */
@Configuration
class DynastyClientConfiguration {
    @Bean
    fun turkuDynastyClient(properties: ApplicationProperties, mapper: ObjectMapper): DynastyClient =
        DynastyClient(properties.getArkistointi().getTurku().dynasty, mapper)
}
