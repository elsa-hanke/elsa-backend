package fi.elsapalvelu.elsa.externalintegration.arkistointi.uef

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.externalintegration.arkistointi.dynasty.DynastyArkistointiExternalTestSupport
import fi.elsapalvelu.elsa.externalintegration.arkistointi.dynasty.DynastyDocumentReadback
import fi.elsapalvelu.elsa.externalintegration.arkistointi.dynasty.DynastyExternalTestConfiguration
import fi.elsapalvelu.elsa.externalintegration.arkistointi.dynasty.externalDynastyClient
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyClient
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyArchiveProfile
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.UefDynastyArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.UefDynastyMetadataMapper
import org.springframework.beans.factory.annotation.Autowired
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles
import java.time.Clock

@SpringBootTest(classes = [UefArkistointiExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
class UefArkistointiExternalIntegrationTests : DynastyArkistointiExternalTestSupport() {
    override val university = YliopistoEnum.ITA_SUOMEN_YLIOPISTO

    @Autowired
    private lateinit var uefDynastyClient: DynastyClient

    @Autowired
    private lateinit var properties: ApplicationProperties

    private val readback by lazy {
        val settings = DynastyArchiveProfile.UEF.archiveProperties(properties.getArkistointi()).dynasty
        DynastyDocumentReadback(uefDynastyClient, requireNotNull(settings.documentSchemaId))
    }

    @ParameterizedTest(name = "UEF Dynasty valmistuminen {0}")
    @EnumSource(Programme::class)
    fun shouldArchiveGraduationWithParallelDocuments(programme: Programme) = archiveGraduation(programme) { caseId, documentIds ->
        readback.verify(caseId, documentIds, listOf("Yhteenveto", "Liite", "Liite"))
    }

    @ParameterizedTest(name = "UEF Dynasty koejakso {0} (55/2020)")
    @EnumSource(Programme::class)
    fun shouldArchiveKoejaksoWithSecrecyMetadata(programme: Programme) =
        archiveKoejakso(programme, degreeRegulation = "55/2020") { caseId, documentIds ->
            readback.verify(caseId, documentIds, listOf("Lausunto"), expectedPublicityClassName = "Osittain salassa pidettävä")
        }
}

@SpringBootConfiguration
@Profile("external-integration")
@Import(DynastyExternalTestConfiguration::class, UefDynastyArkistointiAdapter::class, UefDynastyMetadataMapper::class)
class UefArkistointiExternalIntegrationTestApplication {
    @Bean
    fun uefDynastyClient(properties: ApplicationProperties, mapper: ObjectMapper, clock: Clock): DynastyClient {
        val settings = DynastyArchiveProfile.UEF.archiveProperties(properties.getArkistointi()).dynasty
        return externalDynastyClient(settings, mapper, clock)
    }
}
