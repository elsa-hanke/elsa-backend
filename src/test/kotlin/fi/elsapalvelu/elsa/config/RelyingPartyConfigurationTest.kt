package fi.elsapalvelu.elsa.config

import fi.elsapalvelu.elsa.repository.perustiedot.YliopistoRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verifyNoInteractions
import org.springframework.core.io.ResourceLoader

@ExtendWith(MockitoExtension::class)
class RelyingPartyConfigurationTest {

    @Mock
    private lateinit var resourceLoader: ResourceLoader

    @Mock
    private lateinit var yliopistoRepository: YliopistoRepository

    @Test
    fun `disabled identity providers do not require SAML configuration`() {
        val registrations = configuration(ApplicationProperties()).relyingPartyRegistrations()

        assertThat(registrations).isNull()
        verifyNoInteractions(resourceLoader, yliopistoRepository)
    }

    @Test
    fun `enabled Suomi identity provider fails fast when certificate location is missing`() {
        val properties = ApplicationProperties().apply {
            getSecurity().getSuomifi().enabled = true
        }

        assertThrows<IllegalArgumentException> {
            configuration(properties).relyingPartyRegistrations()
        }
        verifyNoInteractions(resourceLoader, yliopistoRepository)
    }

    @Test
    fun `enabled Haka identity provider fails fast when certificate location is missing`() {
        val properties = ApplicationProperties().apply {
            getSecurity().getHaka().enabled = true
        }

        assertThrows<IllegalArgumentException> {
            configuration(properties).relyingPartyRegistrations()
        }
        verifyNoInteractions(resourceLoader, yliopistoRepository)
    }

    private fun configuration(properties: ApplicationProperties) = RelyingPartyConfiguration(
        resourceLoader,
        properties,
        yliopistoRepository
    )
}
