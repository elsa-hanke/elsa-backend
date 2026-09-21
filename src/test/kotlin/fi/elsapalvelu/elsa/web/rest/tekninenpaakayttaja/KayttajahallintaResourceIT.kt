package fi.elsapalvelu.elsa.web.rest.tekninenpaakayttaja

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.koejakso.*
import fi.elsapalvelu.elsa.domain.tyoskentely.*
import fi.elsapalvelu.elsa.domain.arviointi.*
import fi.elsapalvelu.elsa.domain.suoritteet.*
import fi.elsapalvelu.elsa.domain.koulutus.*
import fi.elsapalvelu.elsa.domain.seuranta.*
import fi.elsapalvelu.elsa.domain.valmistuminen.*
import fi.elsapalvelu.elsa.domain.kayttaja.*
import fi.elsapalvelu.elsa.domain.perustiedot.*
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.TEKNINEN_PAAKAYTTAJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.security.YEK_KOULUTETTAVA
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajienYhdistaminenDTO
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoisalaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate

private const val YHDISTA_KAYTTAJATILIT_ENDPOINT_URL: String =
    "/api/tekninen-paakayttaja/yhdista-kayttajatilit"


@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class KayttajahallintaResourceIT {

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restKayttajahallintaMockMvc: MockMvc

    private lateinit var user: User
    private lateinit var defaultYliopisto: Yliopisto
    private lateinit var erikoisala1: Erikoisala
    private lateinit var erikoistuvaLaakari: ErikoistuvaLaakari
    private lateinit var asetus1: Asetus
    private lateinit var kouluttaja: Kayttaja
    private lateinit var valtuuttaja: ErikoistuvaLaakari
    private lateinit var valtuutus: Kouluttajavaltuutus
    private lateinit var kouluttajanYliopistoErikoisala: KayttajaYliopistoErikoisala
    private lateinit var erikoistuvanToken: VerificationToken
    private lateinit var kouluttajanToken: VerificationToken

    @BeforeEach
    fun setup() {
        defaultYliopisto = Yliopisto(nimi = defaultYliopistoEnum)
        em.persist(defaultYliopisto)

        erikoisala1 = ErikoisalaHelper.createEntity(nimi = erikoisala1Nimi)
        em.persist(erikoisala1)

        asetus1 = Asetus(nimi = asetus1Nimi)
        em.persist(asetus1)

        val erikoistuvaUser =
            KayttajaResourceWithMockUserIT.createEntity(authority = Authority(name = ERIKOISTUVA_LAAKARI))
        erikoistuvaUser.activeAuthority = Authority(name = ERIKOISTUVA_LAAKARI)
        em.persist(erikoistuvaUser)
        erikoistuvaLaakari =
            ErikoistuvaLaakariHelper.createEntity(
                em,
                user = erikoistuvaUser,
                yliopisto = defaultYliopisto,
                erikoisala = erikoisala1,
                asetus = asetus1,
                opintooikeudenPaattymispaiva = LocalDate.now().plusYears(2)
            ).apply {
                kayttaja?.user?.firstName = "John"
                kayttaja?.user?.lastName = "Doe"
            }
        em.persist(erikoistuvaLaakari)

        val kouluttajaUser =
            KayttajaResourceWithMockUserIT.createEntity(authority = Authority(name = KOULUTTAJA))
        kouluttajaUser.activeAuthority = Authority(name = KOULUTTAJA)
        em.persist(kouluttajaUser)
        kouluttaja = KayttajaHelper.createEntity(em, kouluttajaUser)
        em.persist(kouluttaja)

        initMergeData()
    }

    @ParameterizedTest
    @ValueSource(strings = [ERIKOISTUVA_LAAKARI, YEK_KOULUTETTAVA])
    @Transactional
    fun shouldMergeTrainerIntoTraineeAndPersistAccountData(traineeRole: String) {
        setActiveRole(erikoistuvaLaakari.kayttaja!!, traineeRole)
        initTest()
        flushAndClear()

        assertSuccessfulMerge(mergeAccounts(), expectedSteps = 13)
        flushAndClear()

        assertMergedAccounts(traineeRole)
        val savedValtuutus = em.find(Kouluttajavaltuutus::class.java, valtuutus.id)
        assertThat(savedValtuutus.valtuutettu?.id).isEqualTo(erikoistuvaLaakari.kayttaja!!.id)
        assertThat(savedValtuutus.valtuuttajaOpintooikeus?.id)
            .isEqualTo(valtuuttaja.getOpintooikeusKaytossa()!!.id)
        assertThat(savedValtuutus.alkamispaiva).isEqualTo(valtuutus.alkamispaiva)
        assertThat(savedValtuutus.paattymispaiva).isEqualTo(valtuutus.paattymispaiva)
        assertThat(savedValtuutus.valtuutuksenLuontiaika).isEqualTo(valtuutus.valtuutuksenLuontiaika)
        assertThat(savedValtuutus.valtuutuksenMuokkausaika).isEqualTo(valtuutus.valtuutuksenMuokkausaika)

        val savedLinks = findRetainedUniversityLinks()
        assertThat(savedLinks).hasSize(1)
        assertThat(savedLinks.single().yliopisto?.id).isEqualTo(defaultYliopisto.id)
        assertThat(savedLinks.single().erikoisala?.id).isEqualTo(erikoisala1.id)
        assertThat(em.find(KayttajaYliopistoErikoisala::class.java, kouluttajanYliopistoErikoisala.id)).isNull()
    }

    @Test
    @Transactional
    fun shouldKeepExistingAuthorizationAndUniversityLinkWithoutDuplicates() {
        val retainedKayttaja = erikoistuvaLaakari.kayttaja!!
        retainedKayttaja.user!!.authorities.add(Authority(KOULUTTAJA))
        val existingValtuutus = createValtuutus(retainedKayttaja).apply {
            paattymispaiva = paattymispaiva!!.plusYears(1)
        }
        val existingLink = KayttajaYliopistoErikoisala(
            kayttaja = retainedKayttaja,
            yliopisto = defaultYliopisto,
            erikoisala = erikoisala1
        )
        em.persist(existingLink)
        retainedKayttaja.yliopistotAndErikoisalat.add(existingLink)
        initTest()
        flushAndClear()

        // The service omits the role-addition result when the trainer role already exists.
        assertSuccessfulMerge(mergeAccounts(), expectedSteps = 12)
        flushAndClear()

        assertMergedAccounts(ERIKOISTUVA_LAAKARI)
        val savedValtuutukset = em.createQuery(
            "select v from Kouluttajavaltuutus v where v.valtuutettu.id = :kayttajaId",
            Kouluttajavaltuutus::class.java
        )
            .setParameter("kayttajaId", retainedKayttaja.id)
            .resultList
        assertThat(savedValtuutukset.map { it.id }).containsExactly(existingValtuutus.id)
        assertThat(savedValtuutukset.single().paattymispaiva).isEqualTo(existingValtuutus.paattymispaiva)
        assertThat(em.find(Kouluttajavaltuutus::class.java, valtuutus.id)).isNull()
        assertThat(findRetainedUniversityLinks().map { it.id }).containsExactly(existingLink.id)
        assertThat(em.find(KayttajaYliopistoErikoisala::class.java, kouluttajanYliopistoErikoisala.id)).isNull()
    }

    @ParameterizedTest
    @CsvSource(
        "$ERIKOISTUVA_LAAKARI, $ERIKOISTUVA_LAAKARI",
        "$ERIKOISTUVA_LAAKARI, $YEK_KOULUTETTAVA",
        "$ERIKOISTUVA_LAAKARI, $VASTUUHENKILO",
        "$YEK_KOULUTETTAVA, $ERIKOISTUVA_LAAKARI",
        "$YEK_KOULUTETTAVA, $YEK_KOULUTETTAVA",
        "$YEK_KOULUTETTAVA, $VASTUUHENKILO",
        "$KOULUTTAJA, $KOULUTTAJA",
        "$VASTUUHENKILO, $KOULUTTAJA"
    )
    @Transactional
    fun shouldRejectUnsupportedRolesWithoutChangingData(retainedRole: String, sourceRole: String) {
        setActiveRole(erikoistuvaLaakari.kayttaja!!, retainedRole)
        setActiveRole(kouluttaja, sourceRole)
        initTest()
        flushAndClear()

        mergeAccounts()
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("error.invalidauthorities"))
        flushAndClear()

        assertAccountsUnchanged(retainedRole, sourceRole)
    }

    @ParameterizedTest
    @ValueSource(strings = [ERIKOISTUVA_LAAKARI, YEK_KOULUTETTAVA])
    @Transactional
    fun shouldRejectMergingAnAccountWithItself(traineeRole: String) {
        setActiveRole(erikoistuvaLaakari.kayttaja!!, traineeRole)
        initTest()
        flushAndClear()

        mergeAccounts(toinenKayttajaId = erikoistuvaLaakari.kayttaja!!.id)
            .andExpect(MockMvcResultMatchers.status().isBadRequest)
            .andExpect(MockMvcResultMatchers.jsonPath("$.message").value("error.invalidauthorities"))
        flushAndClear()

        assertAccountsUnchanged(traineeRole, KOULUTTAJA)
    }

    private fun initMergeData() {
        valtuuttaja = ErikoistuvaLaakariHelper.createEntity(
            em, yliopisto = defaultYliopisto, erikoisala = erikoisala1, asetus = asetus1
        )
        valtuutus = createValtuutus(kouluttaja)
        kouluttajanYliopistoErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = kouluttaja,
            yliopisto = defaultYliopisto,
            erikoisala = erikoisala1
        )
        em.persist(kouluttajanYliopistoErikoisala)
        kouluttaja.yliopistotAndErikoisalat.add(kouluttajanYliopistoErikoisala)
        erikoistuvanToken = VerificationToken(user = erikoistuvaLaakari.kayttaja!!.user)
        kouluttajanToken = VerificationToken(user = kouluttaja.user)
        em.persist(erikoistuvanToken)
        em.persist(kouluttajanToken)
    }

    private fun createValtuutus(kayttaja: Kayttaja): Kouluttajavaltuutus = Kouluttajavaltuutus(
        alkamispaiva = LocalDate.of(2025, 1, 1),
        paattymispaiva = LocalDate.of(2026, 1, 1),
        valtuutuksenLuontiaika = Instant.parse("2025-01-01T10:00:00Z"),
        valtuutuksenMuokkausaika = Instant.parse("2025-01-02T10:00:00Z"),
        valtuuttajaOpintooikeus = valtuuttaja.getOpintooikeusKaytossa(),
        valtuutettu = kayttaja
    ).also { em.persist(it) }

    private fun setActiveRole(kayttaja: Kayttaja, role: String) {
        kayttaja.user!!.activeAuthority = Authority(role)
        kayttaja.user!!.authorities = mutableSetOf(Authority(role))
    }

    private fun mergeAccounts(toinenKayttajaId: Long? = kouluttaja.id): ResultActions {
        val request = KayttajienYhdistaminenDTO(
            ensimmainenKayttajaId = erikoistuvaLaakari.kayttaja!!.id,
            toinenKayttajaId = toinenKayttajaId,
            yhteinenSahkoposti = MERGED_EMAIL
        )
        return restKayttajahallintaMockMvc.perform(
            MockMvcRequestBuilders.patch(YHDISTA_KAYTTAJATILIT_ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(request))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        )
    }

    private fun assertSuccessfulMerge(result: ResultActions, expectedSteps: Int) {
        result
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.jsonPath("$").value(Matchers.hasSize<Int>(expectedSteps)))
            .andExpect(MockMvcResultMatchers.jsonPath("$[*].onnistui").value(Matchers.everyItem(Matchers.equalTo(true))))
    }

    private fun assertMergedAccounts(traineeRole: String) {
        val originalUser = erikoistuvaLaakari.kayttaja!!.user!!
        val retainedKayttaja = em.find(Kayttaja::class.java, erikoistuvaLaakari.kayttaja!!.id)
        assertThat(retainedKayttaja).isNotNull
        assertThat(retainedKayttaja.user?.id).isEqualTo(originalUser.id)
        val retainedUser = em.find(User::class.java, originalUser.id)
        assertThat(retainedUser.email).isEqualTo(MERGED_EMAIL)
        assertThat(retainedUser.login).isEqualTo(originalUser.login)
        assertThat(retainedUser.firstName).isEqualTo(originalUser.firstName)
        assertThat(retainedUser.lastName).isEqualTo(originalUser.lastName)
        assertThat(retainedUser.activeAuthority?.name).isEqualTo(traineeRole)
        assertThat(retainedUser.authorities.map { it.name }).containsExactlyInAnyOrder(traineeRole, KOULUTTAJA)
        val retainedTrainee = em.find(ErikoistuvaLaakari::class.java, erikoistuvaLaakari.id)
        assertThat(retainedTrainee.kayttaja?.id).isEqualTo(retainedKayttaja.id)
        assertThat(retainedTrainee.getOpintooikeusKaytossa()!!.id)
            .isEqualTo(erikoistuvaLaakari.getOpintooikeusKaytossa()!!.id)
        assertThat(em.find(Kayttaja::class.java, kouluttaja.id)).isNull()
        assertThat(em.find(User::class.java, kouluttaja.user!!.id)).isNull()
        assertThat(em.find(VerificationToken::class.java, kouluttajanToken.id)).isNull()
        assertThat(em.find(VerificationToken::class.java, erikoistuvanToken.id)?.user?.id).isEqualTo(originalUser.id)
    }

    private fun assertAccountsUnchanged(retainedRole: String, sourceRole: String) {
        listOf(erikoistuvaLaakari.kayttaja!! to retainedRole, kouluttaja to sourceRole).forEach { (kayttaja, role) ->
            val originalUser = kayttaja.user!!
            val savedKayttaja = em.find(Kayttaja::class.java, kayttaja.id)
            assertThat(savedKayttaja).isNotNull
            assertThat(savedKayttaja.user?.id).isEqualTo(originalUser.id)
            val savedUser = em.find(User::class.java, originalUser.id)
            assertThat(savedUser.email).isEqualTo(originalUser.email)
            assertThat(savedUser.activeAuthority?.name).isEqualTo(role)
            assertThat(savedUser.authorities.map { it.name }).containsExactly(role)
        }
        assertThat(em.find(Kouluttajavaltuutus::class.java, valtuutus.id)?.valtuutettu?.id).isEqualTo(kouluttaja.id)
        assertThat(em.find(KayttajaYliopistoErikoisala::class.java, kouluttajanYliopistoErikoisala.id)?.kayttaja?.id)
            .isEqualTo(kouluttaja.id)
        assertThat(findRetainedUniversityLinks()).isEmpty()
        assertThat(em.find(VerificationToken::class.java, erikoistuvanToken.id)?.user?.id)
            .isEqualTo(erikoistuvaLaakari.kayttaja!!.user!!.id)
        assertThat(em.find(VerificationToken::class.java, kouluttajanToken.id)?.user?.id).isEqualTo(kouluttaja.user!!.id)
    }

    private fun findRetainedUniversityLinks(): List<KayttajaYliopistoErikoisala> = em.createQuery(
        "select link from KayttajaYliopistoErikoisala link where link.kayttaja.id = :kayttajaId",
        KayttajaYliopistoErikoisala::class.java
    )
        .setParameter("kayttajaId", erikoistuvaLaakari.kayttaja!!.id)
        .resultList

    fun initTest(userId: String? = null) {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>(
        )
        val authorities = listOf(SimpleGrantedAuthority(TEKNINEN_PAAKAYTTAJA))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(userId ?: user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication
    }

    private fun flushAndClear() {
        em.flush()
        em.clear()
    }

    companion object {
        private const val MERGED_EMAIL = "merged.account@example.com"
        private val defaultYliopistoEnum = YliopistoEnum.HELSINGIN_YLIOPISTO
        private const val erikoisala1Nimi = "erikoisala1"
        private const val asetus1Nimi = "asetus1"
    }
}
