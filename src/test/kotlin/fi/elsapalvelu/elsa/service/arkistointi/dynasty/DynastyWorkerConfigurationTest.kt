package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.TurkuDynastyMetadataMapper

import fi.elsapalvelu.elsa.config.arkistointi.DynastyProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapterRegistry
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.Clock

class DynastyWorkerConfigurationTest {
    private val properties = ApplicationProperties()
    private val turku = properties.getArkistointi().getTurku()
    private val settings = turku.dynasty
    private val mapper = jacksonObjectMapper()
    private val registry = ArkistointiAdapterRegistry(listOf(TurkuDynastyArkistointiAdapter(
        DynastyClient(properties.getArkistointi().getTurku().dynasty, mapper, Clock.systemDefaultZone()),
        TurkuDynastyMetadataMapper(properties, mapper, ArchivePayloadReader(mapper), Clock.systemUTC()),
        DynastyStepExecutor(mock<DynastyStepLedger>()), properties
    )))
    private val provider = ArkistointiConfigurationProvider(properties)

    private fun configure() {
        turku.kaytossa = true
        settings.apply {
            baseUrl = "https://dynasty.example"
            tokenUrl = "https://auth.example/token"
            clientId = "client"
            clientSecret = "private-secret"
            organization = "organization"
            caseSchemaId = "case"
            documentSchemaId = "document"
            tutkintoasetukset = mapOf("56/2015" to "Confirmed regulation text")
        }
    }

    @Test
    fun `worker requires Dynasty settings only when university is enabled`() {
        configure()
        settings.clientSecret = null
        turku.kaytossa = false
        assertThatCode { registry.validateWorkerConfiguration(provider) }.doesNotThrowAnyException()
        turku.kaytossa = true
        assertThatThrownBy { registry.validateWorkerConfiguration(provider) }
            .hasMessageContaining("DYNASTY_CONFIGURATION_INVALID")
    }

    @Test
    fun `Turku does not archive koulutussopimus in Dynasty`() {
        configure()
        assertThat(provider.onKaytossa(DynastyArchiveProfile.TURKU.university, CaseType.SOPIMUS)).isFalse()
        assertThat(provider.isAsyncEnabled(DynastyArchiveProfile.TURKU.university, CaseType.SOPIMUS)).isFalse()
    }

    @Test
    fun `both Turku cases pass worker validation with transport and regulation settings alone`() {
        configure()
        assertThatCode { registry.validateWorkerConfiguration(provider) }.doesNotThrowAnyException()
    }

    @Test
    fun `invalid transport settings fail startup without disclosing values`() {
        val invalidSettings: List<(DynastyProperties) -> Unit> = listOf(
            { it.baseUrl = null }, { it.tokenUrl = null },
            { it.baseUrl = "http://dynasty.example" }, { it.tokenUrl = "https://user:private-secret@auth.example" },
            { it.clientId = "" }, { it.clientSecret = "" }, { it.organization = "" }, { it.scope = "" },
            { it.caseSchemaId = "" }, { it.documentSchemaId = "" }
        )
        invalidSettings.forEach { invalidate ->
            configure()
            settings.scope = "external_interface_api"
            invalidate(settings)
            assertThatThrownBy { registry.validateWorkerConfiguration(provider) }
                .hasMessageContaining("DYNASTY_CONFIGURATION_INVALID").hasMessageNotContaining("private-secret")
        }
    }

    @Test
    fun `invalid regulation mappings fail startup`() {
        listOf(emptyMap(), mapOf("56/2015" to " ")).forEach { mappings ->
            configure()
            settings.tutkintoasetukset = mappings
            assertThatThrownBy { registry.validateWorkerConfiguration(provider) }.hasMessageContaining("DYNASTY_CONFIGURATION_INVALID")
        }
    }

    @Test
    fun `UEF cannot reuse Turku adapter`() {
        configure()
        turku.kaytossa = false
        properties.getArkistointi().getUef().kaytossa = true
        assertThatThrownBy { registry.validateWorkerConfiguration(provider) }
            .hasMessageContaining("ITA_SUOMEN_YLIOPISTO").hasMessageContaining("no asynchronous ArkistointiAdapter")
    }

}
