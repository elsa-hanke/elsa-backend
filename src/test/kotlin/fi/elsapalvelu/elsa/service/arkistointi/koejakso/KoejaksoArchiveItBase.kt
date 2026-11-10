package fi.elsapalvelu.elsa.service.arkistointi.koejakso

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.arkistointi.ArkistointiJobRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonKoulutussopimusRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiService
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobCreator
import fi.elsapalvelu.elsa.service.arkistointi.job.creation.ArkistointiJobScheduler
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiAsiakirjaSnapshotService
import fi.elsapalvelu.elsa.service.arkistointi.support.AsyncArchiveItSupport
import fi.elsapalvelu.elsa.service.dto.arkistointi.ArkistointiPackage
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonKoulutussopimusDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonVastuuhenkilonArvioDTO
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.koejakso.KoejaksonKoulutussopimusService
import fi.elsapalvelu.elsa.service.koejakso.KoejaksonVastuuhenkilonArvioService
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonKoulutussopimusMapper
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonVastuuhenkilonArvioMapper
import fi.elsapalvelu.elsa.web.rest.ResourceIntegrationTestBase
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KoejaksonVaiheetHelper
import fi.elsapalvelu.elsa.web.rest.helpers.YliopistoHelper
import java.time.LocalDate
import java.util.UUID
import javax.cache.CacheManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.convention.TestBean
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.transaction.support.TransactionTemplate

/** Runs the parameterized test once for both koejakso case types (contract and final assessment). */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@ParameterizedTest
@EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
annotation class ForEachKoejaksoCaseType

/**
 * Commits to a disposable PostgreSQL DB, using real PDFs, snapshots, creator and transactional scheduling.
 *
 * All bean overrides live here so every subclass shares one Spring context.
 */
@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///koejaksoArchive?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
    "application.arkistointi.tre.kaytossa=true",
    "application.arkistointi.tre.metadata.cases.koejakso.async-enabled=true",
    "application.arkistointi.tre.metadata.cases.sopimus.async-enabled=true",
    "application.arkistointi.tre.metadata.cases.sopimus.title=Koulutussopimus",
    "application.arkistointi.tre.metadata.cases.sopimus.type=Sopimus",
    "application.arkistointi.tre.metadata.cases.sopimus.documents.sopimus.retention-period=20",
    "db-scheduler.enabled=false"
])
@Import(KoejaksoArchiveItBase.TestConfig::class)
open class KoejaksoArchiveItBase : ResourceIntegrationTestBase() {
    @Autowired protected lateinit var transactions: TransactionTemplate
    @Autowired protected lateinit var jdbc: JdbcTemplate
    @Autowired protected lateinit var jobs: ArkistointiJobRepository
    @Autowired protected lateinit var properties: ApplicationProperties
    @MockitoSpyBean protected lateinit var koulutussopimusRepository: KoejaksonKoulutussopimusRepository
    @MockitoSpyBean protected lateinit var vastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository
    @Autowired protected lateinit var koulutussopimusMapper: KoejaksonKoulutussopimusMapper
    @Autowired protected lateinit var vastuuhenkilonArvioMapper: KoejaksonVastuuhenkilonArvioMapper
    @Autowired protected lateinit var koulutussopimusService: KoejaksonKoulutussopimusService
    @Autowired protected lateinit var vastuuhenkilonArvioService: KoejaksonVastuuhenkilonArvioService
    @MockitoSpyBean protected lateinit var snapshots: ArkistointiAsiakirjaSnapshotService
    @MockitoSpyBean protected lateinit var scheduler: ArkistointiJobScheduler
    @MockitoSpyBean protected lateinit var archiveMapper: ObjectMapper
    @MockitoSpyBean protected lateinit var creator: ArkistointiJobCreator
    @MockitoSpyBean protected lateinit var legacy: SynchronousArkistointiService
    @MockitoBean protected lateinit var mail: MailService
    @TestBean(name = "louhiAsyncArkistointiAdapter", methodName = "createArchiveAdapter")
    private lateinit var testArchiveAdapter: ArkistointiAdapter
    protected val adapter: TestAdapter get() = testArchiveAdapter as TestAdapter

    @BeforeEach
    fun configure() {
        properties.getArkistointi().getTre().kaytossa = true
        setAsyncEnabled(CaseType.KOEJAKSO, true)
        setAsyncEnabled(CaseType.SOPIMUS, true)
        adapter.behavior = { ArkistointiResult.Success("external-koejakso") }
        doReturn(ArkistointiPackage("/tmp/koejakso-test.zip", null)).whenever(legacy).muodostaSahke(
            anyOrNull(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any()
        )
        doAnswer { null }.whenever(legacy).laheta(any(), any(), any(), any(), anyOrNull(), anyOrNull())
    }

    protected fun setAsyncEnabled(type: CaseType, enabled: Boolean) {
        properties.getArkistointi().getTre().metadata!!.getCaseMetadata(type)!!.asyncEnabled = enabled
    }

    protected fun job(fixture: Fixture, type: CaseType) = jobs.findByKey(key(fixture, type))!!

    protected fun key(fixture: Fixture, type: CaseType): String {
        val prefix = if (type == CaseType.SOPIMUS) "koejakso-sopimus" else "koejakso-arvio"
        return "$prefix:${YliopistoEnum.TAMPEREEN_YLIOPISTO.name}:${fixture.caseId(type)}"
    }

    protected fun assertNoJob(fixture: Fixture, type: CaseType) {
        assertThat(jobs.findByKey(key(fixture, type))).isNull()
    }

    protected fun assertRolledBack(fixture: Fixture, type: CaseType) {
        transactions.executeWithoutResult {
            val approved = if (type == CaseType.SOPIMUS) koulutussopimusRepository.findById(fixture.koulutussopimusId).orElseThrow().vastuuhenkiloHyvaksynyt
            else vastuuhenkilonArvioRepository.findById(fixture.vastuuhenkilonArvioId).orElseThrow().vastuuhenkiloHyvaksynyt
            assertThat(approved).isFalse()
        }
        assertNoJob(fixture, type)
        assertThat(documentCount(fixture)).isZero()
        verifyNoInteractions(mail)
    }

    protected fun assertAwaitingFinalApproval(fixture: Fixture, type: CaseType) {
        assertNoJob(fixture, type)
        assertThat(documentCount(fixture)).isZero()
        verifyNoInteractions(snapshots, creator, scheduler)
    }

    protected fun documentCount(fixture: Fixture) =
        jdbc.queryForObject("select count(*) from asiakirja where opintooikeus_id = ?", Int::class.java, fixture.studyRightId)

    protected fun taskCount(id: UUID) =
        jdbc.queryForObject("select count(*) from scheduled_tasks where task_instance = ?", Int::class.java, id.toString())

    /** Removes the earlier approval so the final decision lacks its prerequisite. */
    protected fun withdrawPreviousApproval(fixture: Fixture, type: CaseType) {
        transactions.executeWithoutResult {
            if (type == CaseType.SOPIMUS) {
                koulutussopimusRepository.findById(fixture.koulutussopimusId).orElseThrow().kouluttajat!!.forEach { kouluttaja -> kouluttaja.sopimusHyvaksytty = false }
            } else {
                vastuuhenkilonArvioRepository.findById(fixture.vastuuhenkilonArvioId).orElseThrow().virkailijaHyvaksynyt = false
            }
        }
    }

    /** Calls the service directly, bypassing the HTTP approval guards. */
    protected fun updateApproval(fixture: Fixture, type: CaseType) {
        setSecurityContext(fixture.approverId, VASTUUHENKILO)
        if (type == CaseType.SOPIMUS) {
            koulutussopimusService.update(koulutussopimusDto(fixture), fixture.approverId)
        } else {
            vastuuhenkilonArvioService.update(vastuuhenkilonArvioDto(fixture).apply { koejaksoHyvaksytty = true }, fixture.approverId, null, null)
        }
    }

    protected fun approve(fixture: Fixture, type: CaseType, accepted: Boolean = true, correction: String? = null): ResultActions {
        setSecurityContext(fixture.approverId, VASTUUHENKILO)
        val dto = if (type == CaseType.SOPIMUS) koulutussopimusDto(fixture).apply { korjausehdotus = correction }
        else vastuuhenkilonArvioDto(fixture).apply { koejaksoHyvaksytty = accepted; vastuuhenkilonKorjausehdotus = correction }
        val endpoint = if (type == CaseType.SOPIMUS) "koulutussopimus" else "vastuuhenkilonarvio"
        return testMockMvc.perform(put("/api/vastuuhenkilo/koejakso/$endpoint")
            .contentType(MediaType.APPLICATION_JSON).content(convertObjectToJsonBytes(dto)).with(csrf()))
    }

    protected fun koulutussopimusDto(fixture: Fixture): KoejaksonKoulutussopimusDTO = transactions.execute {
        koulutussopimusMapper.toDto(koulutussopimusRepository.findById(fixture.koulutussopimusId).orElseThrow())
    }

    protected fun vastuuhenkilonArvioDto(fixture: Fixture): KoejaksonVastuuhenkilonArvioDTO = transactions.execute {
        vastuuhenkilonArvioMapper.toDto(vastuuhenkilonArvioRepository.findById(fixture.vastuuhenkilonArvioId).orElseThrow())
    }

    protected fun fixture(): Fixture = transactions.execute {
        val approverUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(VASTUUHENKILO))
        em.persist(approverUser)
        em.flush()
        setSecurityContext(approverUser.id!!, VASTUUHENKILO)
        val university = YliopistoHelper.createReferenceData(em, YliopistoEnum.TAMPEREEN_YLIOPISTO)
        val traineeUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(ERIKOISTUVA_LAAKARI))
        em.persist(traineeUser)
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user = traineeUser, yliopisto = university)
        val hyvaksyja = createVastuuhenkiloForKoejakso(erikoistuvaLaakari, user = approverUser)
        val trainerUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(KOULUTTAJA)).also(em::persist)
        val kouluttaja = KayttajaHelper.createEntity(em, trainerUser).also(em::persist)
        val supervisor = KayttajaHelper.createEntity(em).also(em::persist)
        val reviewerUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(OPINTOHALLINNON_VIRKAILIJA)).also(em::persist)
        val tarkastaja = KayttajaHelper.createEntity(em, reviewerUser).apply { yliopistot.add(university) }.also(em::persist)
        val phases = KoejaksonVaiheetHelper.persistKoejaksoVaiheet(em, erikoistuvaLaakari, kouluttaja, supervisor, hyvaksyja, university)
        phases.koulutussopimus.erikoistuvanAllekirjoitusaika = LocalDate.now()
        val assessment = KoejaksonVaiheetHelper.createVastuuhenkilonArvio(erikoistuvaLaakari, hyvaksyja).apply {
            erikoistuvanKuittausaika = LocalDate.now()
            virkailija = tarkastaja
            virkailijaHyvaksynyt = true
            virkailijanKuittausaika = LocalDate.now()
        }
        em.persist(assessment)
        em.flush()
        Fixture(
            phases.koulutussopimus.id!!, assessment.id!!, erikoistuvaLaakari.getOpintooikeusKaytossa()!!.id!!,
            approverUser.id!!, traineeUser.id!!, trainerUser.id!!, reviewerUser.id!!
        )
    }

    protected data class Fixture(
        val koulutussopimusId: Long,
        val vastuuhenkilonArvioId: Long,
        val studyRightId: Long,
        val approverId: String,
        val traineeId: String,
        val trainerId: String,
        val reviewerId: String
    ) {
        fun caseId(type: CaseType) = if (type == CaseType.SOPIMUS) koulutussopimusId else vastuuhenkilonArvioId
    }

    @TestConfiguration(proxyBeanMethods = false)
    class TestConfig {
        @Bean
        fun isolatedCache(customizer: JCacheManagerCustomizer): CacheManager =
            AsyncArchiveItSupport.isolatedArchiveCache("urn:elsa:koejakso-archive-it", javaClass.classLoader, customizer)
    }

    class TestAdapter : ArkistointiAdapter {
        override val university = YliopistoEnum.TAMPEREEN_YLIOPISTO
        override val supportedCaseTypes = setOf(CaseType.SOPIMUS, CaseType.KOEJAKSO)
        var behavior: (ArkistointiSubmission) -> ArkistointiResult = { ArkistointiResult.Success("external-koejakso") }
        override fun arkistoi(pyynto: ArkistointiSubmission) = behavior(pyynto)
    }

    companion object {
        @JvmStatic
        fun createArchiveAdapter(): ArkistointiAdapter = TestAdapter()
    }
}
