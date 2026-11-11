package fi.elsapalvelu.elsa.externalintegration.arkistointi.turku

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.config.arkistointi.DynastyClientConfiguration
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.externalintegration.ExternalIntegrationTestSupport
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiTestData
import fi.elsapalvelu.elsa.service.arkistointi.ArkistointiTestScenario
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyStepExecutor
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyStepKey
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.DynastyStepLedger
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.TurkuDynastyArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.dynasty.metadata.TurkuDynastyMetadataMapper
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiChecksum
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiDocumentIdentity
import fi.elsapalvelu.elsa.service.arkistointi.job.document.LoadedArkistointiAsiakirja
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchivePayloadReader
import fi.elsapalvelu.elsa.service.arkistointi.job.payload.ArchiveStudyRightPayload
import fi.elsapalvelu.elsa.service.arkistointi.support.arkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.valmistuminen.ValmistuminenArkistointiPayload
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.util.UUID

@SpringBootTest(classes = [TurkuArkistointiExternalIntegrationTestApplication::class])
@ActiveProfiles("external-integration")
@Disabled
class TurkuArkistointiExternalIntegrationTests : ExternalIntegrationTestSupport() {
    @Autowired
    private lateinit var adapter: TurkuDynastyArkistointiAdapter

    @Autowired
    private lateinit var ledger: DynastyStepLedger

    @Autowired
    private lateinit var mapper: ObjectMapper

    @BeforeEach
    fun prepareLedger() {
        reset(ledger)
        whenever(ledger.begin(any(), any(), any())).thenReturn(DynastyStepLedger.Decision.Proceed)
        whenever(ledger.complete(any(), any(), any(), anyOrNull())).thenReturn(true)
    }

    @ParameterizedTest(name = "Turku Dynasty valmistuminen {0}")
    @EnumSource(Programme::class)
    fun shouldArchiveGraduationWithAttachments(programme: Programme) {
        val submission = createSubmission(programme)
        log.info("Sending Turku Dynasty archive test: programme={}, jobId={}", programme, submission.jobId)

        val result = adapter.arkistoi(submission)

        assertThat(result).describedAs("Dynasty delivery must succeed for $programme")
            .isInstanceOf(ArkistointiResult.Success::class.java)
        val caseId = (result as ArkistointiResult.Success).externalId
        assertThat(caseId).isNotBlank()
        verify(ledger).complete(eq(submission.jobId), eq(DynastyStepKey.Case), eq(submission.execution.version), eq(caseId))
        verify(ledger).complete(eq(submission.jobId), eq(DynastyStepKey.Task), eq(submission.execution.version), any())
        submission.asiakirjat.forEach { document ->
            verify(ledger).complete(
                eq(submission.jobId), eq(DynastyStepKey.Document(document.jarjestysnumero)), eq(submission.execution.version), any()
            )
        }
        verify(ledger).complete(eq(submission.jobId), eq(DynastyStepKey.ArchiveCase), eq(submission.execution.version), isNull())
        log.info("Turku Dynasty archive test succeeded: jobId={}, caseId={}", submission.jobId, caseId)
    }

    private fun createSubmission(programme: Programme): ArkistointiSubmission {
        val scenario = ArkistointiTestScenario(
            name = "Turku Dynasty valmistuminen $programme",
            yliopisto = YliopistoEnum.TURUN_YLIOPISTO,
            caseType = CaseType.VALMISTUMINEN,
            recordTypes = listOf(RecordType.YHTEENVETO, RecordType.LIITE, RecordType.LAILLISTAMISTODISTUS),
            yek = programme == Programme.YEK
        )
        val input = ArkistointiTestData.createInput(scenario)
        val documents = input.asiakirjat.mapIndexed { index, record ->
            val document = record.asiakirja
            val bytes = requireNotNull(document.asiakirjaData?.data)
            LoadedArkistointiAsiakirja(
                requireNotNull(document.id), record.type, index, requireNotNull(document.nimi),
                "application/pdf", ArkistointiChecksum.sha256(bytes), bytes
            )
        }
        val studyRight = ArchiveStudyRightPayload.create(input.opintooikeus, scenario.yliopisto).copy(
            yek = scenario.yek,
            specialtyType = programme.specialtyType
        )
        val payload = ValmistuminenArkistointiPayload(
            caseId = requireNotNull(input.opintooikeus.id), studyRight = studyRight,
            reviewer = "ELSA integraatiotesti", reviewDate = LocalDate.now().minusDays(2),
            approver = "ELSA integraatiotesti", approvalDate = LocalDate.now().minusDays(1),
            documents = documents.map {
                ArkistointiDocumentIdentity(it.asiakirjaId, it.asiakirjaId, it.asiakirjatyyppi, it.jarjestysnumero)
            },
            licensingDate = LocalDate.of(2019, 1, 1)
        )
        return arkistointiSubmission(UUID.randomUUID(), scenario.yliopisto, scenario.caseType, mapper.writeValueAsString(payload), documents)
    }

    enum class Programme(val specialtyType: ErikoisalaTyyppi) {
        EL(ErikoisalaTyyppi.LAAKETIEDE),
        EHL(ErikoisalaTyyppi.HAMMASLAAKETIEDE),
        YEK(ErikoisalaTyyppi.LAAKETIEDE)
    }
}

@SpringBootConfiguration
@Profile("external-integration")
@EnableConfigurationProperties(ApplicationProperties::class)
@Import(
    DynastyClientConfiguration::class,
    TurkuDynastyArkistointiAdapter::class,
    TurkuDynastyMetadataMapper::class,
    ArchivePayloadReader::class,
    DynastyStepExecutor::class
)
class TurkuArkistointiExternalIntegrationTestApplication {
    @Bean
    fun objectMapper(): ObjectMapper = jacksonObjectMapper().registerModule(JavaTimeModule())

    @Bean
    fun dynastyStepLedger(): DynastyStepLedger = mock()
}
