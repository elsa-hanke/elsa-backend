package fi.elsapalvelu.elsa.service.koejakso

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonKoulutussopimusRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.security.testSamlPrincipal
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiService
import fi.elsapalvelu.elsa.service.dto.arkistointi.ArkistointiPackage
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonKoulutussopimusDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonVastuuhenkilonArvioDTO
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonKoulutussopimusMapper
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonVastuuhenkilonArvioMapper
import fi.elsapalvelu.elsa.web.rest.ResourceIntegrationTestBase
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KoejaksonVaiheetHelper
import fi.elsapalvelu.elsa.web.rest.helpers.YliopistoHelper
import java.io.IOException
import java.net.URI
import java.time.LocalDate
import javax.cache.Caching
import org.assertj.core.api.Assertions.assertThat
import org.ehcache.core.config.DefaultConfiguration
import org.ehcache.jsr107.EhcacheCachingProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mockito.mockingDetails
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.support.TransactionTemplate

/** Real committed PostgreSQL transactions and PDF generation; only external delivery and mail are mocked. */
@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///koejaksoApprovalTransaction?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true"
])
@Import(KoejaksoApprovalTransactionIT.TestConfig::class)
class KoejaksoApprovalTransactionIT : ResourceIntegrationTestBase() {
    @Autowired private lateinit var contracts: KoejaksonKoulutussopimusRepository
    @Autowired private lateinit var assessments: KoejaksonVastuuhenkilonArvioRepository
    @Autowired private lateinit var contractMapper: KoejaksonKoulutussopimusMapper
    @Autowired private lateinit var assessmentMapper: KoejaksonVastuuhenkilonArvioMapper
    @Autowired private lateinit var transactions: TransactionTemplate
    @MockitoBean private lateinit var legacy: SynchronousArkistointiService
    @MockitoBean private lateinit var mail: MailService

    @BeforeEach
    fun configureDelivery() {
        whenever(legacy.onKaytossa(any(), any())).thenReturn(true)
        whenever(legacy.muodostaSahke(anyOrNull(), any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(ArkistointiPackage("/tmp/approval-transaction-test.zip", null))
    }

    @ParameterizedTest
    @CsvSource("SOPIMUS, IO", "KOEJAKSO, IO", "SOPIMUS, RUNTIME", "KOEJAKSO, RUNTIME", "SOPIMUS, ERROR", "KOEJAKSO, ERROR")
    fun `delivery failure rolls back approval and real PDF and sends no mail`(type: CaseType, failure: String) {
        val fixture = fixture()
        val initialDocuments = documentCount(fixture)
        val exception = when (failure) {
            "IO" -> IOException("Simulated transport failure")
            "ERROR" -> AssertionError("Simulated delivery error")
            else -> IllegalStateException("Simulated delivery failure")
        }
        doAnswer { throw exception }.whenever(legacy).laheta(any(), any(), any(), any(), anyOrNull(), anyOrNull())
        authenticateUser(fixture.approverId, VASTUUHENKILO)

        val result = approve(fixture, type).andExpect(status().isInternalServerError).andReturn()

        assertThat(result.resolvedException).isNotNull()
        val rootCause = generateSequence(result.resolvedException as Throwable?) { it.cause }.last()
        assertThat(rootCause).isSameAs(exception)
        assertNotApproved(fixture, type)
        assertThat(documentCount(fixture)).isEqualTo(initialDocuments)
        verifyNoInteractions(mail)
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `approval mail is not sent before the enclosing transaction commits`(type: CaseType) {
        val fixture = fixture()
        authenticateUser(fixture.approverId, VASTUUHENKILO)

        transactions.executeWithoutResult {
            approve(fixture, type).andExpect(status().isOk)
            verifyNoInteractions(mail)
        }

        assertThat(mockingDetails(mail).invocations).isNotEmpty()
        assertThat(documentCount(fixture)).isEqualTo(1)
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `later rollback suppresses queued approval mail`(type: CaseType) {
        val fixture = fixture()
        val initialDocuments = documentCount(fixture)
        authenticateUser(fixture.approverId, VASTUUHENKILO)

        assertThrows<IllegalStateException> {
            transactions.executeWithoutResult {
                approve(fixture, type).andExpect(status().isOk)
                verifyNoInteractions(mail)
                error("Simulated later failure")
            }
        }

        assertNotApproved(fixture, type)
        assertThat(documentCount(fixture)).isEqualTo(initialDocuments)
        verifyNoInteractions(mail)
    }

    private fun authenticateUser(userId: String, authority: String) {
        TestSecurityContextHolder.setAuthentication(Saml2Authentication(
            testSamlPrincipal(userId, emptyMap()), "test", listOf(SimpleGrantedAuthority(authority))
        ))
    }

    private fun assertNotApproved(fixture: Fixture, type: CaseType) {
        transactions.executeWithoutResult {
            val approved = if (type == CaseType.SOPIMUS) contracts.findById(fixture.contractId).orElseThrow().vastuuhenkiloHyvaksynyt
            else assessments.findById(fixture.assessmentId).orElseThrow().vastuuhenkiloHyvaksynyt
            assertThat(approved).isFalse()
        }
    }

    private fun documentCount(fixture: Fixture): Long = transactions.execute {
        em.createQuery("select count(a) from Asiakirja a where a.opintooikeus.id = :id", Long::class.javaObjectType)
            .setParameter("id", fixture.studyRightId).singleResult
    }

    private fun approve(fixture: Fixture, type: CaseType, accepted: Boolean = true, correction: String? = null): org.springframework.test.web.servlet.ResultActions {
        authenticateUser(fixture.approverId, VASTUUHENKILO)
        val dto = if (type == CaseType.SOPIMUS) contractDto(fixture).apply { korjausehdotus = correction }
        else assessmentDto(fixture).apply { koejaksoHyvaksytty = accepted; vastuuhenkilonKorjausehdotus = correction }
        val endpoint = if (type == CaseType.SOPIMUS) "koulutussopimus" else "vastuuhenkilonarvio"
        return testMockMvc.perform(put("/api/vastuuhenkilo/koejakso/$endpoint")
            .contentType(MediaType.APPLICATION_JSON).content(convertObjectToJsonBytes(dto)).with(csrf()))
    }
    private fun contractDto(fixture: Fixture): KoejaksonKoulutussopimusDTO = transactions.execute {
        contractMapper.toDto(contracts.findById(fixture.contractId).orElseThrow())
    }
    private fun assessmentDto(fixture: Fixture): KoejaksonVastuuhenkilonArvioDTO = transactions.execute {
        assessmentMapper.toDto(assessments.findById(fixture.assessmentId).orElseThrow())
    }

    private fun fixture(): Fixture = transactions.execute {
        val approverUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(VASTUUHENKILO))
        em.persist(approverUser)
        em.flush()
        authenticateUser(approverUser.id!!, VASTUUHENKILO)
        val university = YliopistoHelper.createReferenceData(em, YliopistoEnum.TAMPEREEN_YLIOPISTO)
        val traineeUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(ERIKOISTUVA_LAAKARI))
        em.persist(traineeUser)
        val trainee = ErikoistuvaLaakariHelper.createEntity(em, user = traineeUser, yliopisto = university)
        val approver = createVastuuhenkiloForKoejakso(trainee, user = approverUser)
        val trainerUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(KOULUTTAJA)).also(em::persist)
        val trainer = KayttajaHelper.createEntity(em, trainerUser).also(em::persist)
        val supervisor = KayttajaHelper.createEntity(em).also(em::persist)
        val reviewerUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(OPINTOHALLINNON_VIRKAILIJA)).also(em::persist)
        val reviewer = KayttajaHelper.createEntity(em, reviewerUser).also(em::persist)
        reviewer.yliopistot.add(university)
        val phases = KoejaksonVaiheetHelper.persistKoejaksoVaiheet(em, trainee, trainer, supervisor, approver, university)
        phases.koulutussopimus.erikoistuvanAllekirjoitusaika = LocalDate.now()
        val assessment = KoejaksonVaiheetHelper.createVastuuhenkilonArvio(trainee, approver).apply {
            erikoistuvanKuittausaika = LocalDate.now()
            virkailija = reviewer
            virkailijaHyvaksynyt = true
            virkailijanKuittausaika = LocalDate.now()
        }
        em.persist(assessment)
        em.flush()
        Fixture(
            phases.koulutussopimus.id!!, assessment.id!!, trainee.getOpintooikeusKaytossa()!!.id!!,
            approverUser.id!!, traineeUser.id!!, trainer.user!!.id!!, reviewer.user!!.id!!
        )
    }

    private data class Fixture(
        val contractId: Long,
        val assessmentId: Long,
        val studyRightId: Long,
        val approverId: String,
        val traineeId: String,
        val trainerId: String,
        val reviewerId: String
    ) {
        fun caseId(type: CaseType) = if (type == CaseType.SOPIMUS) contractId else assessmentId
    }

    @TestConfiguration(proxyBeanMethods = false)
    class TestConfig {
        @Bean
        fun isolatedCache(customizer: JCacheManagerCustomizer): javax.cache.CacheManager =
            (Caching.getCachingProvider() as EhcacheCachingProvider).getCacheManager(
                URI("urn:elsa:koejakso-approval-transaction-it"), DefaultConfiguration(javaClass.classLoader)
            ).also(customizer::customize)
    }
}
