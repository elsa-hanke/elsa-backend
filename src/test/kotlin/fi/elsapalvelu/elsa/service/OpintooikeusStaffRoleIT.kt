package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.YEK_KOULUTETTAVA
import fi.elsapalvelu.elsa.service.kayttaja.OpintooikeusService
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoisalaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.OpintooikeusHelper
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDate
import kotlin.test.assertEquals

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class OpintooikeusStaffRoleIT {
    @Autowired private lateinit var opintooikeusService: OpintooikeusService
    @Autowired private lateinit var em: EntityManager
    @Autowired private lateinit var clock: Clock

    @ParameterizedTest
    @CsvSource(
        "ROLE_KOULUTTAJA, true", "ROLE_KOULUTTAJA, false", "ROLE_VASTUUHENKILO, true", "ROLE_VASTUUHENKILO, false",
        "ROLE_OPINTOHALLINNON_VIRKAILIJA, true", "ROLE_OPINTOHALLINNON_VIRKAILIJA, false",
        "ROLE_TEKNINEN_PAAKAYTTAJA, true", "ROLE_TEKNINEN_PAAKAYTTAJA, false",
        "ROLE_ADMIN, true", "ROLE_ADMIN, false"
    )
    fun `staff role survives reconciliation with no selected right`(role: String, validRights: Boolean) {
        val user = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(role))
        user.activeAuthority = Authority(role)
        em.persist(user)
        val erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user, erikoisala = erikoisala)
        OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(em, erikoistuvaLaakari)
        OpintooikeusHelper.addOpintooikeusForYekKoulutettava(em, erikoistuvaLaakari)
        erikoistuvaLaakari.opintooikeudet.forEach {
            it.kaytossa = false
            if (!validRights) {
                it.opintooikeudenPaattymispaiva = LocalDate.now(clock).minusYears(1)
                it.viimeinenKatselupaiva = LocalDate.now(clock).minusDays(1)
            }
        }
        val userId = user.id!!
        em.flush()
        em.clear()

        opintooikeusService.checkOpintooikeusAndRoles(em.find(User::class.java, userId))
        em.flush()
        em.clear()

        val persistedUser = em.find(User::class.java, userId)
        val expected = if (validRights) setOf(role, ERIKOISTUVA_LAAKARI, YEK_KOULUTETTAVA) else setOf(role)
        assertEquals(expected, persistedUser.authorities.map { it.name }.toSet())
        assertEquals(role, persistedUser.activeAuthority?.name)
        val selected = em.find(ErikoistuvaLaakari::class.java, erikoistuvaLaakari.id).opintooikeudet.count { it.kaytossa }
        assertEquals(if (validRights) 1 else 0, selected)
    }
    @Test
    fun `reconciliation does not merge a stale user over current staff roles or profile`() {
        val user = KayttajaResourceWithMockUserIT.createEntity(authority = Authority("ROLE_KOULUTTAJA"))
        user.activeAuthority = Authority("ROLE_KOULUTTAJA")
        em.persist(user)
        em.flush()
        val stale = User(id = user.id, firstName = "Old", authorities = user.authorities.toMutableSet(),
            activeAuthority = user.activeAuthority)
        user.firstName = "Current"
        user.authorities.add(Authority("ROLE_TEKNINEN_PAAKAYTTAJA"))
        em.flush()
        em.clear()

        opintooikeusService.checkOpintooikeusAndRoles(stale)
        em.flush()
        em.clear()

        val persisted = em.find(User::class.java, user.id)
        assertEquals("Current", persisted.firstName)
        assertEquals(setOf("ROLE_KOULUTTAJA", "ROLE_TEKNINEN_PAAKAYTTAJA"), persisted.authorities.map { it.name }.toSet())
        assertEquals(persisted.authorities, stale.authorities)
    }

}
