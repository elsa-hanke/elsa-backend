package fi.elsapalvelu.elsa.web.rest.yekkoulutettava

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Opintosuoritus
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.domain.enumeration.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.security.YEK_KOULUTETTAVA
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.*
import jakarta.persistence.EntityManager
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class YekKoulutettavaTeoriakoulutusResourceIT {

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restTeoriakoulutusMockMvc: MockMvc

    private lateinit var opintosuoritus: Opintosuoritus
    private lateinit var user: User

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllTeoriakoulutukset() {
        initTest()

        restTeoriakoulutusMockMvc.perform(get(ENTITY_API_URL))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].id").value(opintosuoritus.id as Any))
            .andExpect(jsonPath("$[0].nimi_fi").value(OpintosuoritusHelper.DEFAULT_NIMI_FI))
            .andExpect(jsonPath("$[0].nimi_sv").value(OpintosuoritusHelper.DEFAULT_NIMI_SV))
    }


    fun initTest(userId: String? = KayttajaHelper.DEFAULT_ID) {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()

        val userDetails = mapOf<String, List<Any>>()
        val authorities = listOf(SimpleGrantedAuthority(YEK_KOULUTETTAVA))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        // Lisätään pakollinen tieto
        var erikoistuvaLaakari =
            em.findAll(ErikoistuvaLaakari::class).firstOrNull { it.kayttaja?.user == user }
        if (erikoistuvaLaakari == null) {
            erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
            em.persist(erikoistuvaLaakari)
            em.flush()
        }

        val opintooikeus = OpintooikeusHelper.addOpintooikeusForYekKoulutettava(em, erikoistuvaLaakari)
        OpintooikeusHelper.setOpintooikeusKaytossa(erikoistuvaLaakari, opintooikeus)

        val opintosuoritusTyyppi = OpintosuoritusTyyppiEnum.YEK_TEORIAKOULUTUS

        opintosuoritus = OpintosuoritusHelper.createEntity(
            em,
            user,
            OPINTOSUORITUS1_KURSSIKOODI,
            opintosuoritusTyyppi,
            opintooikeus = opintooikeus
        )
        em.persist(opintosuoritus)
    }

    companion object {
        private const val OPINTOSUORITUS1_KURSSIKOODI = "YLEE0028"
        private val ENTITY_API_URL: String = "/api/yek-koulutettava/teoriakoulutukset"
    }
}
