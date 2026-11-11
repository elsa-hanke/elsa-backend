package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.kayttaja.*
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.security.*
import fi.elsapalvelu.elsa.web.rest.ResourceIntegrationTestBase
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class AccountMergeSearchResourceIT : ResourceIntegrationTestBase() {
    private lateinit var ownUniversity: Yliopisto
    private lateinit var otherUniversity: Yliopisto

    @BeforeEach
    fun initUniversities() {
        ownUniversity = persistYliopisto(YliopistoEnum.TAMPEREEN_YLIOPISTO)
        otherUniversity = persistYliopisto(YliopistoEnum.ITA_SUOMEN_YLIOPISTO)
    }

    @ParameterizedTest
    @ValueSource(strings = [ERIKOISTUVA_LAAKARI, YEK_KOULUTETTAVA, "no-authorities"])
    fun clerkOnlyFindsTraineesWithStudyRightInOwnUniversity(role: String) {
        login(OPINTOHALLINNON_VIRKAILIJA)
        val own = trainee(ownUniversity, role)
        trainee(otherUniversity, role)
        assertThat(search()).containsExactly(own.kayttaja!!.id)
    }

    @Test
    fun clerkDoesNotFindForeignTraineeThroughAdditionalTrainerRole() {
        login(OPINTOHALLINNON_VIRKAILIJA)
        val foreign = trainee(otherUniversity)
        foreign.kayttaja!!.user!!.authorities.add(Authority(KOULUTTAJA))
        assertThat(search()).isEmpty()
        val result = testMockMvc.perform(get("/api/virkailija/erikoistujat-ja-kouluttajat")
            .param("nimi.contains", "MergeSearch"))
            .andExpect(status().isOk).andReturn()
        assertThat(objectMapper.readTree(result.response.contentAsString).path("page").path("totalElements").asLong()).isZero()
    }

    @Test
    fun clerkStillFindsTrainerWithoutTraineeAccount() {
        login(OPINTOHALLINNON_VIRKAILIJA)
        val trainer = account(KOULUTTAJA)
        assertThat(search()).containsExactly(trainer.id)
    }

    @Test
    fun trainerRoleCannotExposeTraineeAccountWithoutStudyRights() {
        login(OPINTOHALLINNON_VIRKAILIJA)
        val trainer = account(KOULUTTAJA)
        persistAndFlush(ErikoistuvaLaakari(kayttaja = trainer))
        assertThat(search()).isEmpty()
    }

    @Test
    fun traineeRoleWithoutStudyRightsCannotBypassRestrictionAsTrainer() {
        login(OPINTOHALLINNON_VIRKAILIJA)
        val trainer = account(KOULUTTAJA)
        trainer.user!!.authorities.add(Authority(ERIKOISTUVA_LAAKARI))
        assertThat(search()).isEmpty()
    }

    @Test
    fun anyOwnUniversityStudyRightQualifiesRegardlessOfActiveRightAndWithoutDuplicates() {
        login(OPINTOHALLINNON_VIRKAILIJA)
        val resident = trainee(otherUniversity)
        val original = resident.opintooikeudet.first()
        repeat(2) { index ->
            val ownRight = Opintooikeus(
                yliopisto = ownUniversity, kaytossa = false, erikoistuvaLaakari = resident,
                yliopistoOpintooikeusId = "merge-search-$index", tila = original.tila,
                opintooikeudenMyontamispaiva = original.opintooikeudenMyontamispaiva,
                opintooikeudenPaattymispaiva = original.opintooikeudenPaattymispaiva,
                viimeinenKatselupaiva = original.viimeinenKatselupaiva,
                osaamisenArvioinninOppaanPvm = original.osaamisenArvioinninOppaanPvm,
                erikoisala = original.erikoisala, opintoopas = original.opintoopas, asetus = original.asetus
            )
            persistAndFlush(ownRight)
        }
        assertThat(resident.aktiivinenOpintooikeus).isEqualTo(original.id)
        assertThat(search()).containsExactly(resident.kayttaja!!.id)
    }

    @Test
    fun adminWithAdditionalClerkRoleSeesAllUniversities() {
        login(TEKNINEN_PAAKAYTTAJA, additionalRole = OPINTOHALLINNON_VIRKAILIJA)
        val own = trainee(ownUniversity)
        val foreign = trainee(otherUniversity)
        // Multiple matching roles must not duplicate a result or inflate the count.
        foreign.kayttaja!!.user!!.authorities.add(Authority(KOULUTTAJA))
        assertThat(search("tekninen-paakayttaja")).containsExactlyInAnyOrder(own.kayttaja!!.id, foreign.kayttaja!!.id)
    }

    @Test
    fun activeClerkWithAdditionalAdminRoleRemainsRestricted() {
        login(OPINTOHALLINNON_VIRKAILIJA, additionalRole = TEKNINEN_PAAKAYTTAJA)
        val own = trainee(ownUniversity)
        trainee(otherUniversity)
        assertThat(search()).containsExactly(own.kayttaja!!.id)
    }

    @Test
    fun clerkWithoutUniversityCannotPerformUnrestrictedSearch() {
        login(OPINTOHALLINNON_VIRKAILIJA, withUniversity = false)
        trainee(otherUniversity)
        testMockMvc.perform(get("/api/virkailija/erikoistujat-ja-kouluttajat")
            .param("nimi.contains", "MergeSearch"))
            .andExpect(status().is5xxServerError)
    }

    private fun account(role: String): Kayttaja {
        val candidate = KayttajaResourceWithMockUserIT.createEntity(
            nimi = "MergeSearch Candidate", authority = Authority(if (role == "no-authorities") ERIKOISTUVA_LAAKARI else role)
        ).apply {
            activeAuthority = if (role == "no-authorities") null else Authority(role)
            if (role == "no-authorities") authorities.clear()
        }
        persistAndFlush(candidate)
        return KayttajaHelper.createEntity(em, candidate).also { persistAndFlush(it) }
    }

    private fun trainee(university: Yliopisto, role: String = ERIKOISTUVA_LAAKARI): ErikoistuvaLaakari {
        val candidate = account(role)
        return ErikoistuvaLaakariHelper.createEntity(em, user = candidate.user, yliopisto = university)
    }

    private fun login(role: String, additionalRole: String? = null, withUniversity: Boolean = true) {
        val caller = account(role)
        caller.user!!.firstName = "SearchCaller"
        if (withUniversity) caller.yliopistot.add(ownUniversity)
        additionalRole?.let { caller.user!!.authorities.add(Authority(it)) }
        em.flush()
        TestSecurityContextHolder.getContext().authentication = Saml2Authentication(
            testSamlPrincipal(caller.user!!.id, emptyMap()), "test", listOf(SimpleGrantedAuthority(role))
        )
    }

    private fun search(rolePath: String = "virkailija"): List<Long> {
        em.flush()
        val response = testMockMvc.perform(get("/api/$rolePath/erikoistujat-ja-kouluttajat")
            .param("nimi.contains", "MergeSearch").param("size", "100"))
            .andExpect(status().isOk).andReturn().response
        return objectMapper.readTree(response.contentAsString).path("content").map { it.path("kayttajaId").asLong() }
    }
}
