package fi.elsapalvelu.elsa.service

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.Authority
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.KayttajaYliopistoErikoisala
import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.enumeration.OpintooikeudenTila
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.YEK_KOULUTETTAVA
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoisalaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.OpintooikeusHelper
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import javax.xml.bind.ValidationException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class OpintooikeusServiceIT {

    @Autowired
    private lateinit var opintooikeusService: OpintooikeusService

    @Autowired
    private lateinit var em: EntityManager

    private lateinit var erikoistuvaLaakari: ErikoistuvaLaakari

    private lateinit var elOikeus: Opintooikeus

    private lateinit var yekOikeus: Opintooikeus

    @BeforeEach
    fun setup() {
        val user =
            KayttajaResourceWithMockUserIT.createEntity(authority = Authority(ERIKOISTUVA_LAAKARI))
        user.authorities.add(Authority(YEK_KOULUTETTAVA))
        user.activeAuthority = Authority(ERIKOISTUVA_LAAKARI)
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>()
        val authorities = listOf(SimpleGrantedAuthority(ERIKOISTUVA_LAAKARI))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        val erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)

        erikoistuvaLaakari =
            ErikoistuvaLaakariHelper.createEntity(em, user, erikoisala = erikoisala)

        elOikeus = OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(em, erikoistuvaLaakari)
        yekOikeus = OpintooikeusHelper.addOpintooikeusForYekKoulutettava(em, erikoistuvaLaakari)
    }

    @Test
    fun `test change opintooikeus kaytossa`() {
        opintooikeusService.setOpintooikeusKaytossa(erikoistuvaLaakari.kayttaja?.user?.id!!, elOikeus.id!!)

        assertTrue(elOikeus.kaytossa)
        assertFalse(yekOikeus.kaytossa)
        assertEquals(elOikeus.id!!, erikoistuvaLaakari.aktiivinenOpintooikeus)
    }

    @Test
    fun `test change opintooikeus kaytossa to YEK`() {
        val opintooikeusKaytossa = erikoistuvaLaakari.aktiivinenOpintooikeus
        opintooikeusService.setOpintooikeusKaytossa(erikoistuvaLaakari.kayttaja?.user?.id!!, yekOikeus.id!!)

        assertTrue(yekOikeus.kaytossa)
        assertFalse(elOikeus.kaytossa)
        assertEquals(opintooikeusKaytossa, erikoistuvaLaakari.aktiivinenOpintooikeus)
    }

    @Test
    fun `test set aktiivinen opintooikeus`() {
        opintooikeusService.setOpintooikeusKaytossa(erikoistuvaLaakari.kayttaja?.user?.id!!, yekOikeus.id!!)
        opintooikeusService.setAktiivinenOpintooikeusKaytossa(erikoistuvaLaakari.kayttaja?.user?.id!!)

        assertFalse(elOikeus.kaytossa)
        assertFalse(yekOikeus.kaytossa)
        assertEquals(1, erikoistuvaLaakari.opintooikeudet.filter { it.kaytossa == true }.size)
    }

    @Test
    fun `test set expired aktiivinen opintooikeus should change to valid oikeus`() {
        val expiredOikeus = OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(
            em,
            erikoistuvaLaakari,
            paattymispaiva = LocalDate.ofEpochDay(1L),
            viimeinenKatselupaiva = LocalDate.ofEpochDay(1L),
            tila = OpintooikeudenTila.VALMISTUNUT
        )
        erikoistuvaLaakari.aktiivinenOpintooikeus = expiredOikeus.id
        em.persist(erikoistuvaLaakari)
        em.flush()

        val userId = erikoistuvaLaakari.kayttaja?.user?.id!!

        val validOikeudet =
            opintooikeusService.findAllValidByErikoistuvaLaakariKayttajaUserId(userId)
        assertEquals(3, validOikeudet.size)

        opintooikeusService.setOpintooikeusKaytossa(userId, yekOikeus.id!!)
        opintooikeusService.setAktiivinenOpintooikeusKaytossa(userId)

        assertFalse(expiredOikeus.kaytossa)
        assertFalse(yekOikeus.kaytossa)
        assertEquals(1, erikoistuvaLaakari.opintooikeudet.filter { it.kaytossa == true }.size)
    }

    @Test
    fun `test set aktiivinen opintooikeus without valid oikeus should throw`() {
        val removableOikeudet = erikoistuvaLaakari.opintooikeudet.filter { it.erikoisala?.id != YEK_ERIKOISALA_ID }
        removableOikeudet.forEach {
            em.remove(it)
        }
        erikoistuvaLaakari.opintooikeudet.removeAll(removableOikeudet)
        em.flush()
        val expiredOikeus = OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(
            em,
            erikoistuvaLaakari,
            paattymispaiva = LocalDate.ofEpochDay(1L),
            viimeinenKatselupaiva = LocalDate.ofEpochDay(1L),
            tila = OpintooikeudenTila.VALMISTUNUT
        )
        erikoistuvaLaakari.aktiivinenOpintooikeus = expiredOikeus.id
        em.persist(erikoistuvaLaakari)
        em.flush()

        val userId = erikoistuvaLaakari.kayttaja?.user?.id!!

        val validOikeudet =
            opintooikeusService.findAllValidByErikoistuvaLaakariKayttajaUserId(userId)
        assertEquals(1, validOikeudet.size)

        opintooikeusService.setOpintooikeusKaytossa(userId, yekOikeus.id!!)
        assertThrows<ValidationException> { opintooikeusService.setAktiivinenOpintooikeusKaytossa(userId) }
    }

    @Test
    fun `test check expired YEK oikeus`() {
        val elKaytossa = erikoistuvaLaakari.opintooikeudet.firstOrNull { it.kaytossa == true }
        elKaytossa?.kaytossa = false
        em.persist(elKaytossa)

        yekOikeus.tila = OpintooikeudenTila.VALMISTUNUT
        yekOikeus.viimeinenKatselupaiva = LocalDate.ofEpochDay(1L)
        yekOikeus.kaytossa = true
        em.persist(yekOikeus)

        val user = erikoistuvaLaakari.kayttaja?.user!!
        user.activeAuthority = Authority(YEK_KOULUTETTAVA)
        em.persist(user)
        em.flush()

        opintooikeusService.checkOpintooikeusAndRoles(erikoistuvaLaakari.kayttaja?.user!!)

        assertEquals(ERIKOISTUVA_LAAKARI, user.activeAuthority?.name)
        assertFalse(user.authorities.contains(Authority(YEK_KOULUTETTAVA)))
        assertTrue(user.authorities.contains(Authority(ERIKOISTUVA_LAAKARI)))
        assertFalse(yekOikeus.kaytossa)
        assertEquals(1, erikoistuvaLaakari.opintooikeudet.filter { it.kaytossa == true }.size)

        val oikeusKaytossa = erikoistuvaLaakari.opintooikeudet.firstOrNull { it.kaytossa == true }
        assertEquals(erikoistuvaLaakari.aktiivinenOpintooikeus, oikeusKaytossa?.id)
    }

    @Test
    fun `test check expired EL oikeus with multiple opinto-oikeus`() {
        val elKaytossa = erikoistuvaLaakari.opintooikeudet.firstOrNull { it.kaytossa == true }
        elKaytossa?.kaytossa = false
        em.persist(elKaytossa)

        val expiredOikeus = OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(
            em,
            erikoistuvaLaakari,
            paattymispaiva = LocalDate.ofEpochDay(1L),
            viimeinenKatselupaiva = LocalDate.ofEpochDay(1L),
            tila = OpintooikeudenTila.VALMISTUNUT
        )
        expiredOikeus.kaytossa = true
        erikoistuvaLaakari.aktiivinenOpintooikeus = expiredOikeus.id
        em.persist(erikoistuvaLaakari)
        em.flush()

        val user = erikoistuvaLaakari.kayttaja?.user!!

        opintooikeusService.checkOpintooikeusAndRoles(erikoistuvaLaakari.kayttaja?.user!!)

        val oikeusKaytossa = erikoistuvaLaakari.opintooikeudet.firstOrNull { it.kaytossa == true }

        if (oikeusKaytossa?.erikoisala?.id == YEK_ERIKOISALA_ID) {
            assertEquals(YEK_KOULUTETTAVA, user.activeAuthority?.name)
            assertTrue(yekOikeus.kaytossa)
        } else {
            assertEquals(ERIKOISTUVA_LAAKARI, user.activeAuthority?.name)
            assertFalse(yekOikeus.kaytossa)
        }
        assertTrue(user.authorities.contains(Authority(YEK_KOULUTETTAVA)))
        assertTrue(user.authorities.contains(Authority(ERIKOISTUVA_LAAKARI)))

        assertEquals(1, erikoistuvaLaakari.opintooikeudet.filter { it.kaytossa == true }.size)
        assertEquals(erikoistuvaLaakari.aktiivinenOpintooikeus, oikeusKaytossa?.id)
    }

    @Test
    fun `test check expired EL oikeus without other EL opinto-oikeus`() {
        val removableOikeudet = erikoistuvaLaakari.opintooikeudet.filter { it.erikoisala?.id != YEK_ERIKOISALA_ID }
        removableOikeudet.forEach {
            em.remove(it)
        }
        erikoistuvaLaakari.opintooikeudet.removeAll(removableOikeudet)
        em.flush()

        val expiredOikeus = OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(
            em,
            erikoistuvaLaakari,
            paattymispaiva = LocalDate.ofEpochDay(1L),
            viimeinenKatselupaiva = LocalDate.ofEpochDay(1L),
            tila = OpintooikeudenTila.VALMISTUNUT
        )
        expiredOikeus.kaytossa = true
        erikoistuvaLaakari.aktiivinenOpintooikeus = expiredOikeus.id
        em.persist(erikoistuvaLaakari)
        em.flush()

        val user = erikoistuvaLaakari.kayttaja?.user!!

        opintooikeusService.checkOpintooikeusAndRoles(erikoistuvaLaakari.kayttaja?.user!!)

        assertEquals(YEK_KOULUTETTAVA, user.activeAuthority?.name)
        assertTrue(yekOikeus.kaytossa)
        assertTrue(user.authorities.contains(Authority(YEK_KOULUTETTAVA)))
        assertFalse(user.authorities.contains(Authority(ERIKOISTUVA_LAAKARI)))

        assertEquals(1, erikoistuvaLaakari.opintooikeudet.filter { it.kaytossa == true }.size)
    }

    @Test
    fun `test check expired oikeus without other opinto-oikeus`() {
        val removableOikeudet = erikoistuvaLaakari.opintooikeudet
        removableOikeudet.forEach {
            em.remove(it)
        }
        erikoistuvaLaakari.opintooikeudet.removeAll(removableOikeudet)
        em.flush()

        val expiredOikeus = OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(
            em,
            erikoistuvaLaakari,
            paattymispaiva = LocalDate.ofEpochDay(1L),
            viimeinenKatselupaiva = LocalDate.ofEpochDay(1L),
            tila = OpintooikeudenTila.VALMISTUNUT
        )
        expiredOikeus.kaytossa = true
        erikoistuvaLaakari.aktiivinenOpintooikeus = expiredOikeus.id
        em.persist(erikoistuvaLaakari)
        em.flush()

        val user = erikoistuvaLaakari.kayttaja?.user!!

        opintooikeusService.checkOpintooikeusAndRoles(erikoistuvaLaakari.kayttaja?.user!!)

        assertNull(user.activeAuthority)
        assertFalse(user.authorities.contains(Authority(YEK_KOULUTETTAVA)))
        assertFalse(user.authorities.contains(Authority(ERIKOISTUVA_LAAKARI)))

        assertTrue(expiredOikeus.kaytossa)
        assertEquals(1, erikoistuvaLaakari.opintooikeudet.filter { it.kaytossa == true }.size)
    }

    @Test
    fun `test check expired oikeus for kouluttaja`() {
        val removableOikeudet = erikoistuvaLaakari.opintooikeudet
        removableOikeudet.forEach {
            em.remove(it)
        }
        erikoistuvaLaakari.opintooikeudet.removeAll(removableOikeudet)
        em.flush()

        val expiredOikeus = OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(
            em,
            erikoistuvaLaakari,
            paattymispaiva = LocalDate.ofEpochDay(1L),
            viimeinenKatselupaiva = LocalDate.ofEpochDay(1L),
            tila = OpintooikeudenTila.VALMISTUNUT
        )
        expiredOikeus.kaytossa = true
        erikoistuvaLaakari.aktiivinenOpintooikeus = expiredOikeus.id
        em.persist(erikoistuvaLaakari)
        em.flush()

        val yliopistoErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = erikoistuvaLaakari.kayttaja,
            yliopisto = expiredOikeus.yliopisto,
            erikoisala = expiredOikeus.erikoisala
        )
        em.persist(yliopistoErikoisala)

        erikoistuvaLaakari.kayttaja?.yliopistotAndErikoisalat?.add(yliopistoErikoisala)
        em.persist(erikoistuvaLaakari.kayttaja)

        val user = erikoistuvaLaakari.kayttaja?.user!!
        user.authorities.add(Authority(KOULUTTAJA))
        em.persist(user)
        em.flush()

        opintooikeusService.checkOpintooikeusAndRoles(erikoistuvaLaakari.kayttaja?.user!!)

        assertEquals(KOULUTTAJA, user.activeAuthority?.name)
        assertFalse(user.authorities.contains(Authority(YEK_KOULUTETTAVA)))
        assertFalse(user.authorities.contains(Authority(ERIKOISTUVA_LAAKARI)))

        assertTrue(expiredOikeus.kaytossa)
        assertEquals(1, erikoistuvaLaakari.opintooikeudet.filter { it.kaytossa == true }.size)
    }
}
