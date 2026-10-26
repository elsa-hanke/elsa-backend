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
import java.net.URI
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import javax.cache.Caching
import org.assertj.core.api.Assertions.assertThat
import org.ehcache.core.config.DefaultConfiguration
import org.ehcache.jsr107.EhcacheCachingProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.verify
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.support.TransactionTemplate

/** Real committed PostgreSQL transactions and PDF generation; only external delivery and mail are mocked. */
@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///koejaksoWorkflow?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true"
])
@Import(KoejaksoWorkflowIT.TestConfig::class)
class KoejaksoWorkflowIT : ResourceIntegrationTestBase() {
    @Autowired private lateinit var contracts: KoejaksonKoulutussopimusRepository
    @Autowired private lateinit var assessments: KoejaksonVastuuhenkilonArvioRepository
    @Autowired private lateinit var contractMapper: KoejaksonKoulutussopimusMapper
    @Autowired private lateinit var assessmentMapper: KoejaksonVastuuhenkilonArvioMapper
    @Autowired private lateinit var contractService: KoejaksonKoulutussopimusService
    @Autowired private lateinit var assessmentService: KoejaksonVastuuhenkilonArvioService
    @Autowired private lateinit var transactions: TransactionTemplate
    @MockitoBean private lateinit var legacy: SynchronousArkistointiService
    @MockitoBean private lateinit var mail: MailService

    @BeforeEach
    fun configureDelivery() {
        whenever(legacy.onKaytossa(any(), any())).thenReturn(true)
        whenever(legacy.muodostaSahke(anyOrNull(), any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any(), any()))
            .thenReturn(ArkistointiPackage("/tmp/workflow-test.zip", null))
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `combined roles perform intermediate and final decisions through separate endpoints`(type: CaseType) {
        val fixture = fixture(combinedRoles = true)
        transactions.executeWithoutResult {
            if (type == CaseType.SOPIMUS) {
                contracts.findById(fixture.contractId).orElseThrow().kouluttajat!!.single().sopimusHyvaksytty = false
            } else {
                assessments.findById(fixture.assessmentId).orElseThrow().virkailijaHyvaksynyt = false
            }
        }
        setCombinedRoleContext(fixture.approverId)
        val dto = if (type == CaseType.SOPIMUS) contractDto(fixture) else assessmentDto(fixture)
        val endpoint = if (type == CaseType.SOPIMUS) "kouluttaja/koejakso/koulutussopimus" else "virkailija/koejakso/vastuuhenkilonarvio"
        testMockMvc.perform(put("/api/$endpoint").contentType(MediaType.APPLICATION_JSON)
            .content(convertObjectToJsonBytes(dto)).with(csrf())).andExpect(status().isOk)
        assertNotApproved(fixture, type)
        transactions.executeWithoutResult {
            val approved = if (type == CaseType.SOPIMUS) contracts.findById(fixture.contractId).orElseThrow().vastuuhenkiloHyvaksynyt
            else assessments.findById(fixture.assessmentId).orElseThrow().vastuuhenkiloHyvaksynyt
            assertThat(approved).isFalse()
        }

        approve(fixture, type).andExpect(status().isOk)
        verify(legacy, times(1)).laheta(any(), any(), any(), any(), anyOrNull(), anyOrNull())
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `combined roles return intermediate decisions without performing a final decision`(type: CaseType) {
        val fixture = fixture(combinedRoles = true)
        transactions.executeWithoutResult {
            if (type == CaseType.SOPIMUS) {
                contracts.findById(fixture.contractId).orElseThrow().kouluttajat!!.single().sopimusHyvaksytty = false
            } else {
                assessments.findById(fixture.assessmentId).orElseThrow().apply {
                    virkailijaHyvaksynyt = false
                    virkailijanKuittausaika = null
                }
            }
        }
        setCombinedRoleContext(fixture.approverId)
        val correction = "Valivaiheen korjausehdotus"
        val dto = if (type == CaseType.SOPIMUS) contractDto(fixture).apply { korjausehdotus = correction }
        else assessmentDto(fixture).apply { virkailijanKorjausehdotus = correction }
        val endpoint = if (type == CaseType.SOPIMUS) "kouluttaja/koejakso/koulutussopimus" else "virkailija/koejakso/vastuuhenkilonarvio"

        testMockMvc.perform(put("/api/$endpoint").contentType(MediaType.APPLICATION_JSON)
            .content(convertObjectToJsonBytes(dto)).with(csrf())).andExpect(status().isOk)

        transactions.executeWithoutResult {
            if (type == CaseType.SOPIMUS) {
                val contract = contracts.findById(fixture.contractId).orElseThrow()
                assertThat(contract.korjausehdotus).isEqualTo(correction)
                assertThat(contract.vastuuhenkilonKorjausehdotus).isNull()
                assertThat(contract.vastuuhenkiloHyvaksynyt).isFalse()
                assertThat(contract.kouluttajat!!.single().sopimusHyvaksytty).isFalse()
            } else {
                val assessment = assessments.findById(fixture.assessmentId).orElseThrow()
                assertThat(assessment.virkailijanKorjausehdotus).isEqualTo(correction)
                assertThat(assessment.vastuuhenkilonKorjausehdotus).isNull()
                assertThat(assessment.vastuuhenkiloHyvaksynyt).isFalse()
                assertThat(assessment.virkailijaHyvaksynyt).isFalse()
            }
        }
        assertNotApproved(fixture, type)
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `an actor argument does not grant a caller another authority or assignment`(type: CaseType) {
        val fixture = fixture()
        authenticateUser(fixture.trainerId, KOULUTTAJA)
        assertThrows<AccessDeniedException> {
            if (type == CaseType.SOPIMUS) contractService.update(contractDto(fixture), fixture.trainerId, KoejaksoUpdateActor.APPROVER)
            else assessmentService.update(assessmentDto(fixture), fixture.trainerId, KoejaksoUpdateActor.APPROVER)
        }
        // Even a caller holding the requested authority must be the assigned approver.
        authenticateUser(fixture.trainerId, VASTUUHENKILO)
        assertThrows<AccessDeniedException> {
            if (type == CaseType.SOPIMUS) contractService.update(contractDto(fixture), fixture.trainerId, KoejaksoUpdateActor.APPROVER)
            else assessmentService.update(assessmentDto(fixture), fixture.trainerId, KoejaksoUpdateActor.APPROVER)
        }
        assertNotApproved(fixture, type)
        verifyNoInteractions(mail)
    }

    @Test
    fun `contract without trainers cannot be finally approved`() {
        val fixture = fixture()
        transactions.executeWithoutResult { contracts.findById(fixture.contractId).orElseThrow().kouluttajat!!.clear() }

        approve(fixture, CaseType.SOPIMUS).andExpect(status().isBadRequest)

        assertNotApproved(fixture, CaseType.SOPIMUS)
    }

    @Test
    fun `incomplete contract without trainers can still be returned for correction`() {
        val fixture = fixture()
        transactions.executeWithoutResult { contracts.findById(fixture.contractId).orElseThrow().kouluttajat!!.clear() }

        approve(fixture, CaseType.SOPIMUS, correction = "Lisaa kouluttaja").andExpect(status().isOk)

        assertNotApproved(fixture, CaseType.SOPIMUS)
        assertThat(contractDto(fixture).lahetetty).isFalse()
    }


    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `previous approvals are required inside the write transaction`(type: CaseType) {
        val fixture = fixture()
        transactions.executeWithoutResult {
            if (type == CaseType.SOPIMUS) contracts.findById(fixture.contractId).orElseThrow().kouluttajat!!.single().sopimusHyvaksytty = false
            else assessments.findById(fixture.assessmentId).orElseThrow().virkailijaHyvaksynyt = false
        }
        authenticateUser(fixture.approverId, VASTUUHENKILO)
        assertThrows<fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException> { approveDirect(fixture, type) }
        assertNotApproved(fixture, type)
        verifyNoInteractions(legacy, mail)
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `repeated final approval does not send another document`(type: CaseType) {
        val fixture = fixture()
        approve(fixture, type).andExpect(status().isOk)
        approve(fixture, type).andExpect(status().isBadRequest)
        verify(legacy, times(1)).laheta(any(), any(), any(), any(), anyOrNull(), anyOrNull())
    }

    @ParameterizedTest
    @EnumSource(value = CaseType::class, names = ["SOPIMUS", "KOEJAKSO"])
    fun `concurrent approvals serialize and deliver once`(type: CaseType) {
        val fixture = fixture()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val secondStarted = CountDownLatch(1)
        doAnswer {
            entered.countDown()
            check(release.await(20, TimeUnit.SECONDS))
            null
        }.whenever(legacy).laheta(any(), any(), any(), any(), anyOrNull(), anyOrNull())
        val executor = Executors.newFixedThreadPool(2)
        try {
            val first = executor.submit {
                authenticateUser(fixture.approverId, VASTUUHENKILO)
                approveDirect(fixture, type)
            }
            assertThat(entered.await(20, TimeUnit.SECONDS)).isTrue()
            val second = executor.submit<Boolean> {
                authenticateUser(fixture.approverId, VASTUUHENKILO)
                secondStarted.countDown()
                try {
                    approveDirect(fixture, type)
                    true
                } catch (_: fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException) { false }
            }
            assertThat(secondStarted.await(10, TimeUnit.SECONDS)).isTrue()
            assertThrows<TimeoutException> { second.get(200, TimeUnit.MILLISECONDS) }
            release.countDown()
            first.get(20, TimeUnit.SECONDS)
            assertThat(second.get(20, TimeUnit.SECONDS)).isFalse()
            verify(legacy, times(1)).laheta(any(), any(), any(), any(), anyOrNull(), anyOrNull())
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `owner cannot delete an approved contract`() {
        val fixture = fixture()
        approve(fixture, CaseType.SOPIMUS).andExpect(status().isOk)
        authenticateUser(fixture.traineeId, ERIKOISTUVA_LAAKARI)
        testMockMvc.perform(delete("/api/erikoistuva-laakari/koejakso/koulutussopimus/{id}", fixture.contractId)
            .with(csrf())).andExpect(status().isBadRequest)
        assertThat(contracts.findById(fixture.contractId)).isPresent()
    }

    @Test
    fun `owner can delete a sent contract awaiting trainer approval`() {
        val fixture = fixture()
        transactions.executeWithoutResult {
            contracts.findById(fixture.contractId).orElseThrow().kouluttajat!!.single().sopimusHyvaksytty = false
        }
        authenticateUser(fixture.traineeId, ERIKOISTUVA_LAAKARI)
        testMockMvc.perform(delete("/api/erikoistuva-laakari/koejakso/koulutussopimus/{id}", fixture.contractId)
            .with(csrf())).andExpect(status().isNoContent)
        assertThat(contracts.findById(fixture.contractId)).isEmpty()
    }

    @Test
    fun `deletion waits for approval and cannot remove its result`() {
        val fixture = fixture()
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val deleteStarted = CountDownLatch(1)
        doAnswer {
            entered.countDown()
            check(release.await(20, TimeUnit.SECONDS))
            null
        }.whenever(legacy).laheta(any(), any(), any(), any(), anyOrNull(), anyOrNull())
        val executor = Executors.newFixedThreadPool(2)
        try {
            val approval = executor.submit {
                authenticateUser(fixture.approverId, VASTUUHENKILO)
                approveDirect(fixture, CaseType.SOPIMUS)
            }
            assertThat(entered.await(20, TimeUnit.SECONDS)).isTrue()
            val deletion = executor.submit<Boolean> {
                authenticateUser(fixture.traineeId, ERIKOISTUVA_LAAKARI)
                deleteStarted.countDown()
                try {
                    contractService.delete(fixture.contractId, fixture.traineeId)
                    true
                } catch (_: fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException) { false }
            }
            assertThat(deleteStarted.await(10, TimeUnit.SECONDS)).isTrue()
            assertThrows<TimeoutException> { deletion.get(200, TimeUnit.MILLISECONDS) }
            release.countDown()
            approval.get(20, TimeUnit.SECONDS)
            assertThat(deletion.get(20, TimeUnit.SECONDS)).isFalse()
            assertThat(contracts.findById(fixture.contractId)).isPresent()
        } finally {
            release.countDown()
            executor.shutdownNow()
        }
    }

    private fun approveDirect(fixture: Fixture, type: CaseType) {
        if (type == CaseType.SOPIMUS) contractService.update(contractDto(fixture), fixture.approverId, KoejaksoUpdateActor.APPROVER)
        else assessmentService.update(assessmentDto(fixture).apply { koejaksoHyvaksytty = true }, fixture.approverId, KoejaksoUpdateActor.APPROVER)
    }

    private fun assertNotApproved(fixture: Fixture, type: CaseType) {
        transactions.executeWithoutResult {
            val approved = if (type == CaseType.SOPIMUS) contracts.findById(fixture.contractId).orElseThrow().vastuuhenkiloHyvaksynyt
            else assessments.findById(fixture.assessmentId).orElseThrow().vastuuhenkiloHyvaksynyt
            assertThat(approved).isFalse()
        }
    }

    private fun authenticateUser(userId: String, authority: String) {
        TestSecurityContextHolder.setAuthentication(Saml2Authentication(
            testSamlPrincipal(userId, emptyMap()), "test", listOf(SimpleGrantedAuthority(authority))
        ))
    }

    private fun setCombinedRoleContext(userId: String) {
        TestSecurityContextHolder.setAuthentication(Saml2Authentication(
            testSamlPrincipal(userId, emptyMap()), "test",
            listOf(KOULUTTAJA, OPINTOHALLINNON_VIRKAILIJA, VASTUUHENKILO).map(::SimpleGrantedAuthority)
        ))
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

    private fun fixture(combinedRoles: Boolean = false): Fixture = transactions.execute {
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
        val trainer = if (combinedRoles) approver else KayttajaHelper.createEntity(em, trainerUser).also(em::persist)
        val supervisor = KayttajaHelper.createEntity(em).also(em::persist)
        val reviewerUser = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(OPINTOHALLINNON_VIRKAILIJA)).also(em::persist)
        val reviewer = if (combinedRoles) approver else KayttajaHelper.createEntity(em, reviewerUser).also(em::persist)
        reviewer.yliopistot.add(university)
        if (combinedRoles) {
            approverUser.authorities.addAll(listOf(Authority(KOULUTTAJA), Authority(OPINTOHALLINNON_VIRKAILIJA)))
        }
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
                URI("urn:elsa:koejakso-workflow-it"), DefaultConfiguration(javaClass.classLoader)
            ).also(customizer::customize)
    }
}
