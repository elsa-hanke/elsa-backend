package fi.elsapalvelu.elsa.service.arkistointi.dynasty

import fi.elsapalvelu.elsa.config.arkistointi.DynastyProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.arkistointi.ArkistointiConfigurationProvider
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapterRegistry
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class DynastyWorkerConfigurationTest {
    private val properties = ApplicationProperties()
    private val turku = properties.getArkistointi().getTurku()
    private val settings = turku.dynasty
    private val mapper = jacksonObjectMapper()
    private val registry = ArkistointiAdapterRegistry(listOf(TurkuDynastyArkistointiAdapter(
        DynastyClient(properties, mapper), TurkuDynastyMetadataMapper(properties, mapper, ArchivePayloadReader(mapper)),
        mock<DynastyStepLedger>(), mapper
    )))
    private val provider = ArkistointiConfigurationProvider(properties)

    private fun configure(caseType: CaseType = CaseType.VALMISTUMINEN) {
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
        val types = if (caseType == CaseType.VALMISTUMINEN) listOf("yhteenveto", "liite", "laillistamistodistus")
            else listOf("arviointi")
        turku.metadata = ApplicationProperties.Arkistointi.Metadata().apply {
            cases = mapOf(caseType.value to ApplicationProperties.Arkistointi.Case().apply {
                asyncEnabled = true
                documents = types.associateWith {
                    ApplicationProperties.Arkistointi.DocumentMetadata().apply { retentionPeriod = "10" }
                }
            })
        }
    }

    @Test
    fun `disabled university and synchronous cases do not require Dynasty settings`() {
        configure()
        settings.clientSecret = null
        turku.kaytossa = false
        assertThatCode { registry.validateWorkerConfiguration(provider) }.doesNotThrowAnyException()
        turku.kaytossa = true
        turku.metadata!!.getCaseMetadata(CaseType.VALMISTUMINEN)!!.asyncEnabled = false
        assertThatCode { registry.validateWorkerConfiguration(provider) }.doesNotThrowAnyException()
    }

    @Test
    fun `graduation does not require koejakso secrecy configuration`() {
        configure()
        assertThatCode { registry.validateWorkerConfiguration(provider) }.doesNotThrowAnyException()
    }

    @Test
    fun `invalid transport settings fail startup without disclosing values`() {
        val invalidSettings: List<(DynastyProperties) -> Unit> = listOf(
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
    fun `missing document and invalid regulation mappings fail startup`() {
        configure()
        turku.metadata!!.getCaseMetadata(CaseType.VALMISTUMINEN)!!.documents = emptyMap()
        assertThatThrownBy { registry.validateWorkerConfiguration(provider) }.hasMessageContaining("DYNASTY_CONFIGURATION_INVALID")
        listOf(emptyMap(), mapOf("56/2015" to " ")).forEach { mappings ->
            configure()
            settings.tutkintoasetukset = mappings
            assertThatThrownBy { registry.validateWorkerConfiguration(provider) }.hasMessageContaining("DYNASTY_CONFIGURATION_INVALID")
        }
    }

    @Test
    fun `koejakso requires nonblank nonconflicting secrecy fields`() {
        configure(CaseType.KOEJAKSO)
        assertThatThrownBy { registry.validateWorkerConfiguration(provider) }.hasMessageContaining("DYNASTY_SECRECY_UNCONFIGURED")
        settings.koejaksoPublicityClassId = "vendor-confirmed-class"
        settings.koejaksoSecrecyFields = mapOf("reason" to "law", "duration" to "50", "start" to "approval")
        assertThatCode { registry.validateWorkerConfiguration(provider) }.doesNotThrowAnyException()
        settings.koejaksoSecrecyFields = settings.koejaksoSecrecyFields - "reason" + ("AD" to "override")
        assertThatThrownBy { registry.validateWorkerConfiguration(provider) }.hasMessageContaining("DYNASTY_SECRECY_UNCONFIGURED")
    }
}
