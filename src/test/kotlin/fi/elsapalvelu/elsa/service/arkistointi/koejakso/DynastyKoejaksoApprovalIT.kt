package fi.elsapalvelu.elsa.service.arkistointi.koejakso

import com.github.kagkarlsson.scheduler.Scheduler
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobScheduler
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiChecksum
import fi.elsapalvelu.elsa.service.arkistointi.support.AsyncArchiveItSupport
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonVastuuhenkilonArvioMapper
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/** Approval HTTP -> real PDF, snapshot, PostgreSQL and db-scheduler -> test-only Dynasty adapter. */
@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///dynastyKoejakso?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
    "application.arkistointi.turku.kaytossa=true",
    "application.arkistointi.uef.kaytossa=true",
    "application.arkistointi.turku.metadata.cases.koejakso.async-enabled=true",
    "application.arkistointi.uef.metadata.cases.koejakso.async-enabled=true",
    "application.arkistointi.turku.metadata.cases.koejakso.documents.arviointi.retention-period=20",
    "application.arkistointi.uef.metadata.cases.koejakso.documents.arviointi.retention-period=20"
])
@Import(DynastyKoejaksoApprovalIT.Adapters::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DynastyKoejaksoApprovalIT : AsyncArchiveItSupport() {
    @Autowired private lateinit var scheduler: Scheduler
    @Autowired private lateinit var assessments: KoejaksonVastuuhenkilonArvioRepository
    @Autowired private lateinit var mapper: KoejaksonVastuuhenkilonArvioMapper
    @Autowired private lateinit var deliveries: Deliveries
    @MockitoSpyBean private lateinit var jobScheduler: ArkistointiJobScheduler
    @MockitoBean lateinit var mail: MailService

    @ParameterizedTest
    @CsvSource("TURUN_YLIOPISTO,true", "TURUN_YLIOPISTO,false",
        "ITA_SUOMEN_YLIOPISTO,true", "ITA_SUOMEN_YLIOPISTO,false")
    fun `final decision is delivered with frozen metadata and document`(university: YliopistoEnum, accepted: Boolean) {
        val fixture = koejaksoFixture(university, CaseType.KOEJAKSO)
        approve(fixture, accepted).andExpect(status().isOk)
        val job = jobs.findByKey(key(university, fixture.second))!!
        val payload = objectMapper.readValue(job.payload, KoejaksoArkistointiPayload::class.java)
        assertThat(payload.university).isEqualTo(university)
        assertThat(payload.caseId).isEqualTo(fixture.second)
        assertThat(payload.koejaksoHyvaksytty).isEqualTo(accepted)
        val identity = payload.documents.single()
        assertThat(identity.recordType).isEqualTo(RecordType.ARVIOINTI)
        assertThat(identity.sourceDocumentId).isNotEqualTo(identity.snapshotDocumentId)
        val originalBytes = transactions.execute { _ ->
            val reference = jobs.findById(job.id!!).orElseThrow().asiakirjat.single()
            assertThat(reference.asiakirja!!.id).isEqualTo(identity.snapshotDocumentId)
            assertThat(reference.asiakirja!!.archiveSnapshot).isTrue()
            assertThat(reference.jarjestysnumero).isEqualTo(identity.jarjestysnumero)
            assertThat(reference.asiakirjatyyppi).isEqualTo(identity.recordType)
            em.find(Asiakirja::class.java, identity.sourceDocumentId).asiakirjaData!!.data!!.copyOf()
        }
        // Change live data after commit: the adapter must still receive the original decision and PDF.
        transactions.executeWithoutResult { _ ->
            assessments.findById(fixture.second).orElseThrow().koejaksoHyvaksytty = !accepted
            em.find(Asiakirja::class.java, identity.sourceDocumentId).asiakirjaData!!.data = byteArrayOf(1, 2)
        }
        scheduler.triggerCheckForDueExecutions()
        awaitDelivered(job.id!!)
        val delivered = deliveries.byJob.getValue(job.id!!)
        assertThat(delivered.university).isEqualTo(university)
        assertThat(delivered.caseType).isEqualTo(CaseType.KOEJAKSO)
        assertThat(delivered.key).isEqualTo(job.id)
        assertThat(delivered.payload).isEqualTo(job.payload)
        val document = delivered.asiakirjat.single()
        assertThat(document.asiakirjaId).isEqualTo(identity.snapshotDocumentId)
        assertThat(document.asiakirjatyyppi).isEqualTo(RecordType.ARVIOINTI)
        assertThat(document.sisalto).isEqualTo(originalBytes)
        assertThat(ArkistointiChecksum.sha256(document.sisalto)).isEqualTo(document.sha256)
        approve(fixture, accepted).andExpect(status().isBadRequest)
        assertThat(jobs.findByKey(key(university, fixture.second))!!.id).isEqualTo(job.id)
    }

    @ParameterizedTest
    @EnumSource(value = YliopistoEnum::class, names = ["TURUN_YLIOPISTO", "ITA_SUOMEN_YLIOPISTO"])
    fun `failure after scheduling rolls back approval PDF snapshot job and schedule`(university: YliopistoEnum) {
        val fixture = koejaksoFixture(university, CaseType.KOEJAKSO)
        val documentsBefore = count("asiakirja")
        val tasksBefore = count("scheduled_tasks")
        // The spy is wrapped by a MANDATORY transaction proxy, including during stubbing.
        transactions.executeWithoutResult { _ ->
            doAnswer { invocation ->
                assertThat(invocation.callRealMethod()).isEqualTo(true)
                error("Test failure after transactional scheduling")
            }.whenever(jobScheduler).schedule(any(), any())
        }
        approve(fixture, true).andExpect(status().isInternalServerError)
        assertThat(assessments.findById(fixture.second).orElseThrow().vastuuhenkiloHyvaksynyt).isFalse()
        assertThat(assessments.findById(fixture.second).orElseThrow().vastuuhenkilonKuittausaika).isNull()
        assertThat(jobs.findByKey(key(university, fixture.second))).isNull()
        assertThat(count("asiakirja")).isEqualTo(documentsBefore)
        assertThat(count("scheduled_tasks")).isEqualTo(tasksBefore)
        verifyNoInteractions(mail)
    }

    @ParameterizedTest
    @EnumSource(value = YliopistoEnum::class, names = ["TURUN_YLIOPISTO", "ITA_SUOMEN_YLIOPISTO"])
    fun `return for correction creates no archive delivery`(university: YliopistoEnum) {
        val fixture = koejaksoFixture(university, CaseType.KOEJAKSO)
        val documentsBefore = count("asiakirja")
        val tasksBefore = count("scheduled_tasks")
        approve(fixture, true, "Korjattava").andExpect(status().isOk)
        assertThat(jobs.findByKey(key(university, fixture.second))).isNull()
        assertThat(count("asiakirja")).isEqualTo(documentsBefore)
        assertThat(count("scheduled_tasks")).isEqualTo(tasksBefore)
        assertThat(assessments.findById(fixture.second).orElseThrow().vastuuhenkilonKorjausehdotus).isEqualTo("Korjattava")
        verifyNoInteractions(jobScheduler)
    }

    private fun approve(fixture: Triple<Long, Long, String>, accepted: Boolean, correction: String? = null): org.springframework.test.web.servlet.ResultActions {
        setSecurityContext(fixture.third, VASTUUHENKILO)
        val dto = transactions.execute { _ ->
            mapper.toDto(assessments.findById(fixture.second).orElseThrow()).apply {
                koejaksoHyvaksytty = accepted
                vastuuhenkilonKorjausehdotus = correction
            }
        }
        return testMockMvc.perform(put("/api/vastuuhenkilo/koejakso/vastuuhenkilonarvio")
            .contentType(MediaType.APPLICATION_JSON).content(convertObjectToJsonBytes(dto)).with(csrf()))
    }

    private fun key(university: YliopistoEnum, id: Long) = "koejakso-arvio:$university:$id"
    private fun count(table: String) = jdbc.queryForObject("select count(*) from $table", Long::class.java)

    class Deliveries {
        val byJob = ConcurrentHashMap<UUID, ArkistointiSubmission>()
    }

    @TestConfiguration(proxyBeanMethods = false)
    class Adapters {
        @Bean fun deliveries() = Deliveries()
        @Bean fun turku(deliveries: Deliveries): ArkistointiAdapter = adapter(YliopistoEnum.TURUN_YLIOPISTO, deliveries)
        @Bean fun uef(deliveries: Deliveries): ArkistointiAdapter = adapter(YliopistoEnum.ITA_SUOMEN_YLIOPISTO, deliveries)

        @Bean
        fun isolatedCache(customizer: org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer): javax.cache.CacheManager =
            (javax.cache.Caching.getCachingProvider() as org.ehcache.jsr107.EhcacheCachingProvider).getCacheManager(
                java.net.URI("urn:elsa:dynasty-koejakso-it"), org.ehcache.core.config.DefaultConfiguration(javaClass.classLoader)
            ).also(customizer::customize)

        private fun adapter(value: YliopistoEnum, deliveries: Deliveries) = object : ArkistointiAdapter {
            override val university = value
            override val supportedCaseTypes = setOf(CaseType.KOEJAKSO)
            override fun arkistoi(pyynto: ArkistointiSubmission): ArkistointiResult {
                check(deliveries.byJob.putIfAbsent(pyynto.jobId, pyynto) == null) { "Unexpected duplicate delivery" }
                return ArkistointiResult.Success("test:${pyynto.jobId}")
            }
        }
    }
}
