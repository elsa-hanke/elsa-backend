package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajahallintaResourceHelper
import org.junit.jupiter.api.Test
import fi.elsapalvelu.elsa.service.impl.kayttaja.KayttajaQueryService
import fi.elsapalvelu.elsa.web.rest.ResourceIntegrationTestBase
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class KayttajaQueryServiceIT : ResourceIntegrationTestBase() {

    @Autowired private lateinit var kayttajaQueryService: KayttajaQueryService

    @ParameterizedTest
    @CsvSource(
        "ROLE_OPINTOHALLINNON_VIRKAILIJA,true,1",
        "ROLE_OPINTOHALLINNON_VIRKAILIJA,false,1",
        "ROLE_TEKNINEN_PAAKAYTTAJA,true,2",
        "ROLE_TEKNINEN_PAAKAYTTAJA,false,2"
    )
    fun userSearchAppliesUniversityScopeForOfficials(role: String, nullAuthority: Boolean, expectedSize: Int) {
        val yliopisto = persistYliopisto(YliopistoEnum.TAMPEREEN_YLIOPISTO)
        val sameUniversity = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto)
        persistAndFlush(sameUniversity)
        val otherUniversity = persistYliopisto(YliopistoEnum.HELSINGIN_YLIOPISTO)
        persistAndFlush(ErikoistuvaLaakariHelper.createEntity(em, yliopisto = otherUniversity))

        val result = kayttajaQueryService.findByCriteriaAndAuthorities(
            activeAuthority = role,
            criteria = null,
            pageable = PageRequest.of(0, 20),
            langkey = "fi",
            authorities = listOf(ERIKOISTUVA_LAAKARI),
            yliopistot = listOf(yliopisto.id),
            nullAuthority = nullAuthority
        )

        assertThat(result.totalElements).isEqualTo(expectedSize.toLong())
        assertThat(result.content.map { it.kayttajaId }).contains(sameUniversity.kayttaja?.id)
    }

    @ParameterizedTest
    @CsvSource("ROLE_OPINTOHALLINNON_VIRKAILIJA,1", "ROLE_TEKNINEN_PAAKAYTTAJA,2")
    fun unassignedUsersWithoutStudyRightsAreExcludedOnlyFromUniversityScopedSearch(role: String, expectedSize: Int) {
        val university = persistYliopisto(YliopistoEnum.TAMPEREEN_YLIOPISTO)
        val trainee = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = university)
        persistAndFlush(trainee)
        val unassignedUser = KayttajaResourceWithMockUserIT.createEntity().apply { authorities.clear() }
        persistAndFlush(unassignedUser)
        persistAndFlush(KayttajaHelper.createEntity(em, unassignedUser))

        val result = kayttajaQueryService.findByCriteriaAndAuthorities(
            role, null, PageRequest.of(0, 20), "fi", listOf(ERIKOISTUVA_LAAKARI), listOf(university.id), true
        )
        assertThat(result.totalElements).isEqualTo(expectedSize.toLong())
        assertThat(result.content.map { it.kayttajaId }).contains(trainee.kayttaja?.id)
    }

    @Test
    fun trainerSearchWithNoActiveAuthorityRemainsUnscoped() {
        val university = persistYliopisto(YliopistoEnum.TAMPEREEN_YLIOPISTO)
        val otherUniversity = persistYliopisto(YliopistoEnum.HELSINGIN_YLIOPISTO)
        val specialty = em.find(Erikoisala::class.java, 1L)
        val first = KayttajahallintaResourceHelper.createKouluttaja(em, university, specialty)
        val second = KayttajahallintaResourceHelper.createKouluttaja(em, otherUniversity, specialty)
        em.flush()
        val result = kayttajaQueryService.findByCriteriaAndAuthorities(
            null, null, PageRequest.of(0, 20), "fi", listOf(KOULUTTAJA), listOf(university.id), false
        )
        assertThat(result.content.map { it.kayttajaId }).containsExactlyInAnyOrder(first.id, second.id)
    }

}
