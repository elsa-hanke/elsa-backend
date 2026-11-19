package fi.elsapalvelu.elsa.externalintegration.arkistointi.dynasty

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.config.Aikakonfiguraatio
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.arkistointi.DynastyProperties
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyClient
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyStepExecutor
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyStepLedger
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import okhttp3.OkHttpClient
import okhttp3.Protocol
import org.mockito.kotlin.mock
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import java.time.Clock
import java.util.concurrent.TimeUnit

@TestConfiguration(proxyBeanMethods = false)
@EnableConfigurationProperties(ApplicationProperties::class)
@Import(Aikakonfiguraatio::class, ArchivePayloadReader::class, DynastyStepExecutor::class)
class DynastyExternalTestConfiguration {
    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    @Bean
    fun dynastyStepLedger(): DynastyStepLedger = mock()
}

internal fun externalDynastyClient(
    settings: DynastyProperties, mapper: ObjectMapper, clock: Clock, forceHttp11: Boolean = false
): DynastyClient {
    val transport = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .callTimeout(settings.callTimeout).readTimeout(settings.callTimeout)
        .retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false)
        .addInterceptor(DynastyTrafficLoggingInterceptor(settings))
    // Turku's test proxy has returned INTERNAL_ERROR over HTTP/2.
    if (forceHttp11) transport.protocols(listOf(Protocol.HTTP_1_1))
    return DynastyClient(settings, mapper, transport.build(), clock)
}
