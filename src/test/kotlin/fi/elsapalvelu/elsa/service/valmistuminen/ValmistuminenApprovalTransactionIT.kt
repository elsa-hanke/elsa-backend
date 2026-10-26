package fi.elsapalvelu.elsa.service.valmistuminen

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.kayttaja.*
import fi.elsapalvelu.elsa.domain.perustiedot.*
import fi.elsapalvelu.elsa.repository.valmistuminen.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.security.testSamlPrincipal
import fi.elsapalvelu.elsa.service.arkistointi.SynchronousArkistointiService
import fi.elsapalvelu.elsa.service.dto.arkistointi.ArkistointiPackage
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoHyvaksyntaFormDTO
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.web.rest.ResourceIntegrationTestBase
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication as withAuthentication
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.support.TransactionTemplate
import java.io.IOException

/** Real PDFs and committed PostgreSQL state; only external delivery and mail are mocked. */
@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///valmistuminenApprovalTransaction?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true"
])
class ValmistuminenApprovalTransactionIT : ResourceIntegrationTestBase() {
    @Autowired private lateinit var requests: ValmistumispyyntoRepository
    @Autowired private lateinit var transactions: TransactionTemplate
    @MockitoBean private lateinit var archive: SynchronousArkistointiService
    @MockitoBean private lateinit var mail: MailService
    private lateinit var opintooikeus: Opintooikeus
    private lateinit var anotherVastuuhenkilo: Kayttaja
    private lateinit var vastuuhenkiloAuthentication: Saml2Authentication

    @Test
    fun `checked transport failure rolls back approval and generated documents without mail`() {
        val id = transactions.execute {
            initTest()
            val request = ValmistumispyyntoHelper.createValmistumispyyntoOdottaaHyvaksyntaa(
                opintooikeus, anotherVastuuhenkilo, virkailija
            )
            em.persist(request)
            em.persist(ValmistumispyynnonTarkistusHelper.createValmistumispyynnonTarkistusOdottaaHyvaksyntaa(request))
            em.flush()
            request.id!!
        }
        whenever(archive.onKaytossa(any(), any())).thenReturn(true)
        whenever(archive.muodostaSahke(any(), any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(ArkistointiPackage("/tmp/approval-transaction-test.zip", null))
        doAnswer { throw IOException("Simulated transport failure") }
            .whenever(archive).laheta(any(), any(), any(), any(), anyOrNull(), anyOrNull())

        testMockMvc.perform(put("/api/vastuuhenkilo/valmistumispyynnon-hyvaksynta/{id}", id)
            .contentType(MediaType.APPLICATION_JSON)
            .content(convertObjectToJsonBytes(ValmistumispyyntoHyvaksyntaFormDTO(null)))
            .with(csrf()).with(withAuthentication(vastuuhenkiloAuthentication)))
            .andExpect(status().isInternalServerError)

        transactions.executeWithoutResult {
            val request = requests.findById(id).orElseThrow()
            assertThat(request.vastuuhenkiloHyvaksyjaKuittausaika).isNull()
            assertThat(request.yhteenvetoAsiakirja).isNull()
            assertThat(request.liitteetAsiakirja).isNull()
            assertThat(request.erikoistujanTiedotAsiakirja).isNull()
            val count = em.createQuery(
                "select count(a) from Asiakirja a where a.opintooikeus.id = :id", Long::class.javaObjectType
            ).setParameter("id", opintooikeus.id).singleResult
            assertThat(count).isZero()
        }
        verifyNoInteractions(mail)
    }

    private fun initTest() {
        val vastuuhenkiloUser = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(vastuuhenkiloUser)

        val authorities = listOf(SimpleGrantedAuthority(VASTUUHENKILO))
        val authentication = Saml2Authentication(
            testSamlPrincipal(vastuuhenkiloUser.id, mapOf()),
            "test",
            authorities
        )
        TestSecurityContextHolder.setAuthentication(authentication)
        vastuuhenkiloAuthentication = authentication

        val erikoistuvaLaakariUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(ERIKOISTUVA_LAAKARI)
        )
        em.persist(erikoistuvaLaakariUser)

        // Always create a fresh, dedicated Yliopisto so that @AfterEach can safely delete it
        // without touching any Yliopisto row that belongs to another test class's committed data.
        val freshYliopisto = YliopistoHelper.createReferenceData(em, YliopistoEnum.TAMPEREEN_YLIOPISTO)

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, erikoistuvaLaakariUser, yliopisto = freshYliopisto)
        em.persist(erikoistuvaLaakari)

        opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()!!

        val tehtavatyypit = em.findAll(VastuuhenkilonTehtavatyyppi::class)

        vastuuhenkilo = KayttajaHelper.createEntity(em, vastuuhenkiloUser)
        initVastuuhenkiloErikoisalat(
            vastuuhenkilo,
            opintooikeus.yliopisto!!,
            opintooikeus.erikoisala!!,
            tehtavatyypit.filter {
                it.nimi == VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA
            }.toMutableSet()
        )
        em.persist(vastuuhenkilo)

        val anotherVastuuhenkiloUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(VASTUUHENKILO)
        )
        em.persist(anotherVastuuhenkiloUser)
        anotherVastuuhenkilo = KayttajaHelper.createEntity(em, anotherVastuuhenkiloUser)
        initVastuuhenkiloErikoisalat(
            anotherVastuuhenkilo,
            opintooikeus.yliopisto!!,
            opintooikeus.erikoisala!!,
            tehtavatyypit.filter {
                it.nimi == VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI
            }.toMutableSet()
        )
        em.persist(anotherVastuuhenkilo)

        val virkailijaUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(OPINTOHALLINNON_VIRKAILIJA)
        )
        em.persist(virkailijaUser)
        virkailija = KayttajaHelper.createEntity(em, virkailijaUser)
        em.persist(virkailija)
        virkailija.yliopistot.add(opintooikeus.yliopisto!!)
    }

    private fun initVastuuhenkiloErikoisalat(
        kayttaja: Kayttaja,
        yliopisto: Yliopisto,
        erikoisala: Erikoisala,
        tehtavat: MutableSet<VastuuhenkilonTehtavatyyppi>
    ) {
        // The fixture needs distinct assignments, not new reference specialties.
        // Reuse Liquibase seeds rather than introducing audited reference writes.
        val (extra1, extra2) = em.findAll(Erikoisala::class)
            .filter { it.id != erikoisala.id }
            .sortedBy { it.id }
            .take(2)

        listOf(extra1, erikoisala, extra2).forEach { ala ->
            kayttaja.yliopistotAndErikoisalat.add(
                KayttajaYliopistoErikoisala(
                    kayttaja = kayttaja,
                    yliopisto = yliopisto,
                    erikoisala = ala,
                    vastuuhenkilonTehtavat = tehtavat
                )
            )
        }
    }



}
