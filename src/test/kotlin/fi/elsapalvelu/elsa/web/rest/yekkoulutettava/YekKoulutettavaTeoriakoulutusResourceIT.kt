package fi.elsapalvelu.elsa.web.rest.yekkoulutettava

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.Teoriakoulutus
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.repository.TeoriakoulutusRepository
import fi.elsapalvelu.elsa.security.YEK_KOULUTETTAVA
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari.ErikoistuvaLaakariTeoriakoulutusResourceIT
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import java.time.LocalDate
import java.time.ZoneId

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class YekKoulutettavaTeoriakoulutusResourceIT {

    @Autowired
    private lateinit var teoriakoulutusRepository: TeoriakoulutusRepository

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restTeoriakoulutusMockMvc: MockMvc

    private lateinit var teoriakoulutus: Teoriakoulutus

    private lateinit var user: User

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    // @Test
    // @Transactional
    // @Throws(Exception::class)
    // fun getAllTeoriakoulutukset() {
    //     initTest()
    //     val erikoisala = ErikoisalaHelper.createEntity()
    //     em.persist(erikoisala)
    //     em.flush()
    //     teoriakoulutus.opintooikeus?.erikoisala = erikoisala
    //     teoriakoulutusRepository.saveAndFlush(teoriakoulutus)
//
    //     restTeoriakoulutusMockMvc.perform(get(ENTITY_API_URL))
    //         .andExpect(status().isOk)
    //         .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
    //         .andExpect(jsonPath("$.teoriakoulutukset.[*].id").value(hasItem(teoriakoulutus.id?.toInt())))
    //         .andExpect(jsonPath("$.teoriakoulutukset.[*].koulutuksenNimi").value(hasItem(DEFAULT_KOULUTUKSEN_NIMI)))
    //         .andExpect(jsonPath("$.teoriakoulutukset.[*].koulutuksenPaikka").value(hasItem(DEFAULT_KOULUTUKSEN_PAIKKA)))
    //         .andExpect(jsonPath("$.teoriakoulutukset.[*].alkamispaiva").value(hasItem(DEFAULT_ALKAMISPAIVA.toString())))
    //         .andExpect(
    //             jsonPath("$.teoriakoulutukset.[*].paattymispaiva").value(hasItem(DEFAULT_PAATTYMISPAIVA.toString()))
    //         )
    //         .andExpect(
    //             jsonPath("$.teoriakoulutukset.[*].erikoistumiseenHyvaksyttavaTuntimaara").value(
    //                 hasItem(
    //                     DEFAULT_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA
    //                 )
    //             )
    //         )
    //         .andExpect(
    //             jsonPath("$.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara").value(
    //                 DEFAULT_ERIKOISALAN_VAATIMA_TEORIAKOULUTUSTEN_VAHIMMAISMAARA
    //             )
    //         )
    // }

    fun initTest() {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>(
        )
        val authorities = listOf(SimpleGrantedAuthority(YEK_KOULUTETTAVA))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        teoriakoulutus = ErikoistuvaLaakariTeoriakoulutusResourceIT.createEntity(em, user)
    }

    companion object {

        private const val DEFAULT_KOULUTUKSEN_NIMI = "AAAAAAAAAA"
        private const val UPDATED_KOULUTUKSEN_NIMI = "BBBBBBBBBB"

        private const val DEFAULT_KOULUTUKSEN_PAIKKA = "AAAAAAAAAA"
        private const val UPDATED_KOULUTUKSEN_PAIKKA = "BBBBBBBBBB"

        private val DEFAULT_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_ALKAMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_PAATTYMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_PAATTYMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private const val DEFAULT_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA: Double = 5.0
        private const val UPDATED_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA: Double = 10.0

        private val ENTITY_API_URL: String = "/api/yek-koulutettava/teoriakoulutukset"

        // @JvmStatic
        // fun createEntity(em: EntityManager, user: User? = null): Teoriakoulutus {
        //     val teoriakoulutus = Teoriakoulutus(
        //         koulutuksenNimi = DEFAULT_KOULUTUKSEN_NIMI,
        //         koulutuksenPaikka = DEFAULT_KOULUTUKSEN_PAIKKA,
        //         alkamispaiva = DEFAULT_ALKAMISPAIVA,
        //         paattymispaiva = DEFAULT_PAATTYMISPAIVA,
        //         erikoistumiseenHyvaksyttavaTuntimaara = DEFAULT_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA
        //     )
        //     // Lisätään pakollinen tieto
        //     var erikoistuvaLaakari =
        //         em.findAll(ErikoistuvaLaakari::class).firstOrNull { it.kayttaja?.user == user }
        //     if (erikoistuvaLaakari == null) {
        //         erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
        //         em.persist(erikoistuvaLaakari)
        //         em.flush()
        //     }
        //     val newOpintooikeus =
        //         OpintooikeusHelper.addOpintooikeusForYekKoulutettava(em, erikoistuvaLaakari)
        //     OpintooikeusHelper.setOpintooikeusKaytossa(erikoistuvaLaakari, newOpintooikeus)
        //     teoriakoulutus.opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()
        //     return teoriakoulutus
        // }

        // @JvmStatic
        // fun createUpdatedEntity(em: EntityManager): Teoriakoulutus {
        //     val teoriakoulutus = Teoriakoulutus(
        //         koulutuksenNimi = UPDATED_KOULUTUKSEN_NIMI,
        //         koulutuksenPaikka = UPDATED_KOULUTUKSEN_PAIKKA,
        //         alkamispaiva = UPDATED_ALKAMISPAIVA,
        //         paattymispaiva = UPDATED_PAATTYMISPAIVA,
        //         erikoistumiseenHyvaksyttavaTuntimaara = UPDATED_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA
        //     )
        //     // Lisätään pakollinen tieto
        //     val erikoistuvaLaakari: ErikoistuvaLaakari
        //     if (em.findAll(ErikoistuvaLaakari::class).isEmpty()) {
        //         erikoistuvaLaakari = ErikoistuvaLaakariHelper.createUpdatedEntity(em)
        //         em.persist(erikoistuvaLaakari)
        //         em.flush()
        //     } else {
        //         erikoistuvaLaakari = em.findAll(ErikoistuvaLaakari::class).get(0)
        //     }
        //     teoriakoulutus.opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()
        //     return teoriakoulutus
        // }
    }
}
