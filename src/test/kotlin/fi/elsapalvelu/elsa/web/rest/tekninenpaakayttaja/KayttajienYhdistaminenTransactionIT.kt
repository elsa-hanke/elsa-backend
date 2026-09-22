package fi.elsapalvelu.elsa.web.rest.tekninenpaakayttaja

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.arviointi.Arviointityokalu
import fi.elsapalvelu.elsa.domain.arviointi.ArviointityokalunTila
import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.kayttaja.VerificationToken
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonValiarviointiRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.TEKNINEN_PAAKAYTTAJA
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajienYhdistaminenDTO
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant

// Deliberately not @Transactional: the HTTP request must commit or roll back its own transaction.
@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class KayttajienYhdistaminenTransactionIT {
    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoSpyBean
    private lateinit var midtermRepository: KoejaksonValiarviointiRepository

    private lateinit var retained: Kayttaja
    private lateinit var source: Kayttaja
    private lateinit var admin: Kayttaja
    private lateinit var tool: Arviointityokalu
    private lateinit var token: VerificationToken

    @BeforeEach
    fun setupCommittedFixture() {
        transactionTemplate.executeWithoutResult {
            retained = createAccount(ERIKOISTUVA_LAAKARI)
            source = createAccount(KOULUTTAJA)
            admin = createAccount(TEKNINEN_PAAKAYTTAJA)
            token = VerificationToken(user = source.user).also { em.persist(it) }
            tool = Arviointityokalu(
                kayttaja = source,
                nimi = "Tool belonging to the source account",
                tila = ArviointityokalunTila.JULKAISTU,
                luontiaika = Instant.parse("2025-01-15T10:00:00Z"),
                muokkausaika = Instant.parse("2025-01-16T11:00:00Z")
            ).also { em.persist(it) }
        }
        TestSecurityContextHolder.getContext().authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(admin.user!!.id, emptyMap()),
            "test",
            listOf(SimpleGrantedAuthority(TEKNINEN_PAAKAYTTAJA))
        )
    }

    @AfterEach
    fun cleanupCommittedFixture() {
        transactionTemplate.executeWithoutResult {
            em.find(Arviointityokalu::class.java, tool.id)?.let { em.remove(it) }
            em.find(VerificationToken::class.java, token.id)?.let { em.remove(it) }
            em.flush()
            listOf(source, retained, admin).forEach { account ->
                em.find(Kayttaja::class.java, account.id)?.let { em.remove(it) }
            }
            em.flush()
            listOf(source, retained, admin).forEach { account ->
                em.find(User::class.java, account.user!!.id)?.let { em.remove(it) }
            }
        }
        TestSecurityContextHolder.clearContext()
    }

    @Test
    fun shouldCommitTheMergeAndMakeItVisibleInANewTransaction() {
        mergeAccounts("committed.merge@example.com")
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[*].onnistui").value(Matchers.everyItem(Matchers.equalTo(true))))

        transactionTemplate.executeWithoutResult {
            assertThat(em.find(Kayttaja::class.java, source.id)).isNull()
            assertThat(em.find(User::class.java, source.user!!.id)).isNull()
            assertThat(em.find(VerificationToken::class.java, token.id)).isNull()
            val saved = em.find(User::class.java, retained.user!!.id)
            assertThat(saved.email).isEqualTo("committed.merge@example.com")
            assertThat(saved.authorities.map { it.name }).containsExactlyInAnyOrder(ERIKOISTUVA_LAAKARI, KOULUTTAJA)
            assertThat(em.find(Arviointityokalu::class.java, tool.id).kayttaja?.id).isEqualTo(retained.id)
        }
    }

    @Test
    fun shouldRollBackDeletedAccountsAndTransferredReferencesWhenTheFinalEmailWriteFails() {
        // DTO validation accepts this value. The User column/validation rejects it at flush/commit,
        // after the service has transferred references and explicitly flushed the source deletion.
        mergeAccounts("a".repeat(255) + "@example.com")
            .andExpect(status().is5xxServerError)

        assertOriginalAccountsRemain()
    }

    @Test
    fun shouldStopTheMergeInsteadOfReturningSuccessWhenAnIntermediateStepFails() {
        // A failure without a database rollback-only marker used to be swallowed, allowing
        // deletion and later steps to continue. The repository otherwise remains real.
        doThrow(IllegalStateException("Simulated failure while loading midterm assessments"))
            .`when`(midtermRepository)
            .findAllByLahikouluttajaIdOrLahiesimiesId(source.id!!, source.id!!)

        mergeAccounts("failed.merge@example.com")
            .andExpect(status().is5xxServerError)

        assertOriginalAccountsRemain()
    }

    private fun assertOriginalAccountsRemain() {
        transactionTemplate.executeWithoutResult {
            assertThat(em.find(Kayttaja::class.java, source.id)).isNotNull
            val savedSource = em.find(User::class.java, source.user!!.id)
            assertThat(savedSource.email).isEqualTo(source.user!!.email)
            assertThat(savedSource.authorities.map { it.name }).containsExactly(KOULUTTAJA)
            val savedRetained = em.find(User::class.java, retained.user!!.id)
            assertThat(savedRetained.email).isEqualTo(retained.user!!.email)
            assertThat(savedRetained.authorities.map { it.name }).containsExactly(ERIKOISTUVA_LAAKARI)
            assertThat(em.find(VerificationToken::class.java, token.id).user?.id).isEqualTo(source.user!!.id)
            val savedTool = em.find(Arviointityokalu::class.java, tool.id)
            assertThat(savedTool.kayttaja?.id).isEqualTo(source.id)
            assertThat(savedTool.nimi).isEqualTo(tool.nimi)
            assertThat(savedTool.muokkausaika).isEqualTo(tool.muokkausaika)
        }
    }

    private fun mergeAccounts(email: String) = mockMvc.perform(
        patch("/api/tekninen-paakayttaja/yhdista-kayttajatilit")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content(convertObjectToJsonBytes(KayttajienYhdistaminenDTO(retained.id, source.id, email)))
    )

    private fun createAccount(role: String): Kayttaja {
        val user = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(role)).apply {
            activeAuthority = Authority(role)
        }
        em.persist(user)
        return KayttajaHelper.createEntity(em, user).also { em.persist(it) }
    }
}
