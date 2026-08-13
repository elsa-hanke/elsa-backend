package fi.elsapalvelu.elsa.service.integration.sisu.helsinki

import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.service.integration.IntegrationAlertService
import fi.elsapalvelu.elsa.service.integration.IntegrationAuthenticationAlertInterceptor
import fi.elsapalvelu.elsa.service.kayttaja.AlertPublisherService
import okhttp3.tls.HeldCertificate
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito
import org.springframework.core.io.DefaultResourceLoader
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPairGenerator

class SisuHyClientBuilderImplTest {

    @TempDir
    private lateinit var temporaryDirectory: Path

    @Test
    fun `apollo client is constructed without runtime linkage errors`() {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply {
            initialize(RSA_KEY_SIZE)
        }.generateKeyPair()
        val heldCertificate = HeldCertificate.Builder()
            .keyPair(keyPair)
            .commonName("localhost")
            .build()
        val certificate = temporaryDirectory.resolve("sisu-test.crt")
        val privateKey = temporaryDirectory.resolve("sisu-test.key")
        Files.writeString(certificate, heldCertificate.certificatePem())
        Files.writeString(privateKey, heldCertificate.privateKeyPkcs8Pem())

        val properties = ApplicationProperties().apply {
            getSecurity().getSisuHy().apply {
                apiKey = "test-api-key"
                graphqlEndpointUrl = "https://example.invalid/graphql"
                certificateLocation = certificate.toUri().toString()
                privateKeyLocation = privateKey.toUri().toString()
            }
        }
        val builder = SisuHyClientBuilderImpl(
            properties,
            DefaultResourceLoader(),
            IntegrationAlertService(Mockito.mock(AlertPublisherService::class.java))
        )

        assertThatCode { builder.apolloClient() }
            .doesNotThrowAnyException()
        assertThat(
            builder.okHttpClient().interceptors.any {
                it is IntegrationAuthenticationAlertInterceptor
            }
        ).isTrue
    }

    private companion object {
        const val RSA_KEY_SIZE = 2048
    }
}
