package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari.tyoskentelyjakso

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.koulutus.KaytannonKoulutusTyyppi
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.testSamlPrincipal
import fi.elsapalvelu.elsa.web.rest.ResourceIntegrationTestBase
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariTyoskentelyjaksoHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_KAYTANNON_KOULUTUS
import fi.elsapalvelu.elsa.web.rest.helpers.PoissaolonSyyHelper
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
open class ErikoistuvaLaakariTyoskentelyjaksoResourceTestBase : ResourceIntegrationTestBase() {

    @BeforeEach
    fun setup() {
        // Lisätään voimassaoleva poissaolon syy ja päättymistä ei määritetty, poissaolon syy ja päättyminen määritetty,
        // poissaolon syy, jonka voimassaolo ei ole alkanut vielä, poissaolon syy, jonka voimassaolo on jo päättynyt
        em.persist(PoissaolonSyyHelper.createEntity(LocalDate.ofEpochDay(0L), null))
        em.persist(PoissaolonSyyHelper.createEntity(LocalDate.ofEpochDay(0L), LocalDate.ofEpochDay(20L)))
        em.persist(PoissaolonSyyHelper.createEntity(LocalDate.ofEpochDay(15L), LocalDate.ofEpochDay(20L)))
        persistAndFlush(PoissaolonSyyHelper.createEntity(LocalDate.ofEpochDay(0L), LocalDate.ofEpochDay(5L)))
    }

    protected fun initTest(userId: String? = null, kaytannonKoulutus: KaytannonKoulutusTyyppi? = DEFAULT_KAYTANNON_KOULUTUS) {
        user = KayttajaResourceWithMockUserIT.createEntity()
        persistAndFlush(user)
        TestSecurityContextHolder.getContext().authentication = Saml2Authentication(testSamlPrincipal(userId ?: user.id, mapOf<String, List<Any>>()),
            "test", listOf(SimpleGrantedAuthority(ERIKOISTUVA_LAAKARI)))
        tyoskentelyjakso = ErikoistuvaLaakariTyoskentelyjaksoHelper.createEntity(em, user, kaytannonKoulutus = kaytannonKoulutus)
    }
}
