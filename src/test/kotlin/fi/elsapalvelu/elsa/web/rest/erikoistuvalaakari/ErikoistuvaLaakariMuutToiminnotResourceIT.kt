package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.service.dto.UusiLahikouluttajaDTO
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoisalaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.OpintooikeusHelper
import org.assertj.core.api.Assertions.assertThat
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class ErikoistuvaLaakariMuutToiminnotResourceIT {
    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restLahikouluttajatMockMvc: MockMvc

    @Autowired
    private lateinit var kayttajaRepository: KayttajaRepository

    private lateinit var erikoistuvaLaakari: ErikoistuvaLaakari

    private lateinit var existingYliopisto: Yliopisto

    private lateinit var existingErikoisala: Erikoisala

    @BeforeEach
    fun initTest() {
        val erikoistuvaUser = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(erikoistuvaUser)
        em.flush()
        val userDetails = mapOf<String, List<Any>>()
        val authorities = listOf(SimpleGrantedAuthority(ERIKOISTUVA_LAAKARI))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(erikoistuvaUser.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        val existingErikoistuvaLaakari =
            em.findAll(ErikoistuvaLaakari::class).firstOrNull { it.kayttaja?.user == erikoistuvaUser }
        if (existingErikoistuvaLaakari == null) {
            erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, erikoistuvaUser)
            em.persist(erikoistuvaLaakari)
            em.flush()
        } else {
            erikoistuvaLaakari = existingErikoistuvaLaakari
        }
    }

    @Test
    @Transactional
    fun `test that new kouluttaja is added with same yliopisto and erikoisala than erikoistuva`() {
        val databaseSizeBeforeCreate = kayttajaRepository.findAll().size
        val uusiLahikouluttajaDTO = UusiLahikouluttajaDTO(DEFAULT_NIMI, DEFAULT_EMAIL)

        restLahikouluttajatMockMvc.perform(
            post("/api/erikoistuva-laakari/lahikouluttajat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(uusiLahikouluttajaDTO))
                .with(csrf())
        ).andExpect(status().isCreated)

        val opintooikeus = em.findAll(Opintooikeus::class).get(0)
        val kayttajatList = kayttajaRepository.findAll()
        assertThat(kayttajatList).hasSize(databaseSizeBeforeCreate + 1)
        val testLahikouluttaja = kayttajatList[kayttajatList.size - 1]

        assertThat(testLahikouluttaja?.getNimi()).isEqualTo(DEFAULT_NIMI)
        assertThat(testLahikouluttaja?.user?.email).isEqualTo(DEFAULT_EMAIL)
        assertThat(testLahikouluttaja?.user?.login).isEqualTo(DEFAULT_EMAIL)
        assertThat(testLahikouluttaja?.user?.activated).isEqualTo(true)
        assertThat(
            testLahikouluttaja?.user?.authorities?.contains(
                Authority(name = KOULUTTAJA)
            )
        )

        assertThat(testLahikouluttaja?.yliopistotAndErikoisalat).hasSize(1)
        assertThat(testLahikouluttaja?.yliopistotAndErikoisalat?.firstOrNull()?.yliopisto).isEqualTo(opintooikeus.yliopisto)
        assertThat(testLahikouluttaja?.yliopistotAndErikoisalat?.firstOrNull()?.erikoisala).isEqualTo(opintooikeus.erikoisala)
    }

    @Test
    @Transactional
    fun `test that new kouluttaja is added with same yliopisto and erikoisala than opintooikeusKaytossa`() {
        val databaseSizeBeforeCreate = kayttajaRepository.findAll().size
        val uusiLahikouluttajaDTO = UusiLahikouluttajaDTO(DEFAULT_NIMI, DEFAULT_EMAIL)

        val newOpintooikeus = OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(em, erikoistuvaLaakari)
        OpintooikeusHelper.setOpintooikeusKaytossa(erikoistuvaLaakari, newOpintooikeus)

        restLahikouluttajatMockMvc.perform(
            post("/api/erikoistuva-laakari/lahikouluttajat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(uusiLahikouluttajaDTO))
                .with(csrf())
        ).andExpect(status().isCreated)

        val kayttajatList = kayttajaRepository.findAll()
        assertThat(kayttajatList).hasSize(databaseSizeBeforeCreate + 1)
        val testLahikouluttaja = kayttajatList[kayttajatList.size - 1]

        assertThat(testLahikouluttaja?.getNimi()).isEqualTo(DEFAULT_NIMI)
        assertThat(testLahikouluttaja?.user?.email).isEqualTo(DEFAULT_EMAIL)
        assertThat(testLahikouluttaja?.user?.login).isEqualTo(DEFAULT_EMAIL)
        assertThat(testLahikouluttaja?.user?.activated).isEqualTo(true)
        assertThat(
            testLahikouluttaja?.user?.authorities?.contains(
                Authority(name = KOULUTTAJA)
            )
        )

        assertThat(testLahikouluttaja?.yliopistotAndErikoisalat).hasSize(1)
        assertThat(testLahikouluttaja?.yliopistotAndErikoisalat?.firstOrNull()?.yliopisto).isEqualTo(newOpintooikeus.yliopisto)
        assertThat(testLahikouluttaja?.yliopistotAndErikoisalat?.firstOrNull()?.erikoisala).isEqualTo(newOpintooikeus.erikoisala)
    }

    @Test
    @Transactional
    fun `test that existing kouluttaja with same yliopisto and erikoisala is not added`() {
        val opintooikeus = em.findAll(Opintooikeus::class).get(0)
        initKouluttajaWithYliopistoAndErikoisala(opintooikeus.yliopisto, opintooikeus.erikoisala)

        val databaseSizeBeforeCreate = kayttajaRepository.findAll().size
        val uusiLahikouluttajaDTO = UusiLahikouluttajaDTO(DEFAULT_NIMI, DEFAULT_EMAIL)

        restLahikouluttajatMockMvc.perform(
            post("/api/erikoistuva-laakari/lahikouluttajat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(uusiLahikouluttajaDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val kayttajatList = kayttajaRepository.findAll()
        assertThat(kayttajatList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun `test that existing kouluttaja is updated with yliopisto and erikoisala`() {
        initKouluttajaWithYliopistoAndErikoisala()

        val databaseSizeBeforeCreate = kayttajaRepository.findAll().size
        val uusiLahikouluttajaDTO = UusiLahikouluttajaDTO(DEFAULT_NIMI, DEFAULT_EMAIL)

        restLahikouluttajatMockMvc.perform(
            post("/api/erikoistuva-laakari/lahikouluttajat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(uusiLahikouluttajaDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val opintooikeus = em.findAll(Opintooikeus::class).get(0)
        val kayttajatList = kayttajaRepository.findAll()
        assertThat(kayttajatList).hasSize(databaseSizeBeforeCreate)
        val testLahikouluttaja = kayttajatList[kayttajatList.size - 1]
        val yliopistotAndErikoisalatList = testLahikouluttaja?.yliopistotAndErikoisalat?.toList()
        requireNotNull(yliopistotAndErikoisalatList)

        assertThat(yliopistotAndErikoisalatList).hasSize(2)
        assertThat(yliopistotAndErikoisalatList[1].yliopisto).isEqualTo(opintooikeus.yliopisto)
        assertThat(yliopistotAndErikoisalatList[1].erikoisala).isEqualTo(opintooikeus.erikoisala)
    }

    fun initKouluttajaWithYliopistoAndErikoisala(yliopisto: Yliopisto? = null, erikoisala: Erikoisala? = null) {
        val kouluttajaUser =
            KayttajaResourceWithMockUserIT.createEntity(DEFAULT_NIMI, DEFAULT_EMAIL, Authority(name = KOULUTTAJA))
        em.persist(kouluttajaUser)
        em.flush()
        val kouluttajaKayttaja = KayttajaHelper.createEntity(em, kouluttajaUser)
        em.persist(kouluttajaKayttaja)
        em.flush()

        val kouluttajaYliopisto = yliopisto ?: run {
            existingYliopisto = Yliopisto(nimi = DEFAULT_YLIOPISTO)
            em.persist(existingYliopisto)
            existingYliopisto
        }
        val kouluttajaErikoisala = erikoisala ?: run {
            existingErikoisala = ErikoisalaHelper.createEntity()
            em.persist(existingErikoisala)
            existingErikoisala
        }
        val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = kouluttajaKayttaja,
            yliopisto = kouluttajaYliopisto,
            erikoisala = kouluttajaErikoisala
        )
        em.persist(yliopistoAndErikoisala)
        kouluttajaKayttaja.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)
        kouluttajaKayttaja.user = kouluttajaUser
        em.flush()
    }

    companion object {
        private const val DEFAULT_NIMI: String = "Test User"
        private const val DEFAULT_EMAIL: String = "test.user@test.test"
        private val DEFAULT_YLIOPISTO = YliopistoEnum.TAMPEREEN_YLIOPISTO
    }
}
