package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
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

}
