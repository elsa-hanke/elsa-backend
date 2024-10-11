package fi.elsapalvelu.elsa.web.rest.tekninenpaakayttaja

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.TEKNINEN_PAAKAYTTAJA
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajienYhdistaminenDTO
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoisalaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import jakarta.persistence.EntityManager
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.springframework.transaction.annotation.Transactional
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
    }

    @Test
    @Transactional
    fun shouldYhdistaaErikoistujaJaKouluttajaTilit() {
        initTest()
        flushAndClear()

        val kayttajienYhdistaminenDTo = KayttajienYhdistaminenDTO(
            ensimmainenKayttajaId = erikoistuvaLaakari.kayttaja!!.id,
            toinenKayttajaId = kouluttaja.id,
            yhteinenSahkoposti = "john.doe@example.com"
        )

        restKayttajahallintaMockMvc.perform(
            MockMvcRequestBuilders.patch(YHDISTA_KAYTTAJATILIT_ENDPOINT_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajienYhdistaminenDTo))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(MockMvcResultMatchers.jsonPath("$").value(Matchers.hasSize<Int>(11)))
    }

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
        private val defaultYliopistoEnum = YliopistoEnum.HELSINGIN_YLIOPISTO
        private val erikoisala1Nimi = "erikoisala1"
        private val erikoisala2Nimi = "erikoisala2"
        private val asetus1Nimi = "asetus1"
        private val asetus2Nimi = "asetus2"
    }
}
