package fi.elsapalvelu.elsa.externalintegration.arkistointi.turku

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.externalintegration.arkistointi.dynasty.DynastyArkistointiExternalTestSupport
import fi.elsapalvelu.elsa.externalintegration.arkistointi.dynasty.DynastyDocumentReadback
import fi.elsapalvelu.elsa.externalintegration.arkistointi.dynasty.DynastyExternalTestConfiguration
import fi.elsapalvelu.elsa.externalintegration.arkistointi.dynasty.externalDynastyClient
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyClient
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyArchiveProfile
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.TurkuDynastyArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.TurkuDynastyMetadataMapper
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles
import java.time.Clock

@SpringBootTest(classes = [TurkuArkistointiExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
class TurkuArkistointiExternalIntegrationTests : DynastyArkistointiExternalTestSupport() {
    override val university = YliopistoEnum.TURUN_YLIOPISTO

    @Autowired
    private lateinit var turkuDynastyClient: DynastyClient

    @Autowired
    private lateinit var properties: ApplicationProperties

    private val readback by lazy {
        val settings = DynastyArchiveProfile.TURKU.archiveProperties(properties.getArkistointi()).dynasty
        DynastyDocumentReadback(turkuDynastyClient, requireNotNull(settings.documentSchemaId))
    }

    @ParameterizedTest(name = "Turku Dynasty valmistuminen {0}")
    @EnumSource(Programme::class)
    fun shouldArchiveGraduationWithAttachments(programme: Programme) =
        archiveGraduation(programme) { caseId, documentIds ->
            readback.verify(caseId, documentIds, listOf(null, "Liite", "Liite"))
        }

    @ParameterizedTest(name = "Turku Dynasty koejakso {0}")
    @EnumSource(Programme::class)
    fun shouldArchiveKoejaksoWithSecrecyMetadata(programme: Programme) =
        archiveKoejakso(programme) { caseId, documentIds ->
            readback.verify(caseId, documentIds, listOf(null), expectedPublicityClassName = "Osittain salassa pidettävä")
        }
}

@SpringBootConfiguration
@Profile("external-integration")
@Import(DynastyExternalTestConfiguration::class, TurkuDynastyArkistointiAdapter::class, TurkuDynastyMetadataMapper::class)
class TurkuArkistointiExternalIntegrationTestApplication {
    @Bean
    fun turkuDynastyClient(properties: ApplicationProperties, mapper: ObjectMapper, clock: Clock): DynastyClient {
        val settings = DynastyArchiveProfile.TURKU.archiveProperties(properties.getArkistointi()).dynasty
        return externalDynastyClient(settings, mapper, clock, forceHttp11 = true)
    }
}
