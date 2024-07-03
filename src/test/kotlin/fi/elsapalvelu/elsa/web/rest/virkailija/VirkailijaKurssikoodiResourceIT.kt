package fi.elsapalvelu.elsa.web.rest.virkailija

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.Authority
import fi.elsapalvelu.elsa.domain.Kayttaja
import fi.elsapalvelu.elsa.domain.Yliopisto
import fi.elsapalvelu.elsa.domain.enumeration.OpintosuoritusTyyppiEnum
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.OpintosuoritusKurssikoodiRepository
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusKurssikoodiDTO
import fi.elsapalvelu.elsa.service.dto.OpintosuoritusTyyppiDTO
import fi.elsapalvelu.elsa.service.mapper.OpintosuoritusKurssikoodiMapper
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KurssikoodiHelper
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.collection.IsCollectionWithSize
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

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class VirkailijaKurssikoodiResourceIT {

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restKurssikoodiMockMvc: MockMvc

    @Autowired
    private lateinit var opintosuoritusKurssikoodiRepository: OpintosuoritusKurssikoodiRepository

    @Autowired
    private lateinit var opintosuoritusKurssikoodiMapper: OpintosuoritusKurssikoodiMapper

    private lateinit var virkailija: Kayttaja

    private lateinit var defaultYliopisto: Yliopisto

    private lateinit var anotherYliopisto: Yliopisto

    @BeforeEach
    fun setup() {
        defaultYliopisto = Yliopisto(nimi = defaultYliopistoEnum)
        em.persist(defaultYliopisto)

        anotherYliopisto = Yliopisto(nimi = anotherYliopistoEnum)
        em.persist(anotherYliopisto)
    }

    @Test
    @Transactional
    fun getKurssikoodit() {
        initTest()
        KurssikoodiHelper.createEntity(
            em,
            tunniste = "test1",
            tyyppi = OpintosuoritusTyyppiEnum.JOHTAMISOPINTO,
            yliopisto = defaultYliopisto
        )
        KurssikoodiHelper.createEntity(
            em,
            tunniste = "test2",
            tyyppi = OpintosuoritusTyyppiEnum.SATEILYSUOJAKOULUTUS,
            yliopisto = defaultYliopisto
        )
        KurssikoodiHelper.createEntity(
            em,
            tunniste = "test3",
            tyyppi = OpintosuoritusTyyppiEnum.JOHTAMISOPINTO,
            yliopisto = anotherYliopisto
        )

        restKurssikoodiMockMvc.perform(
            MockMvcRequestBuilders.get("/api/virkailija/kurssikoodit")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$").value(IsCollectionWithSize.hasSize<Int>(2))
            )
    }

    @Test
    @Transactional
    fun getOpintosuoritusTyypit() {
        initTest()

        restKurssikoodiMockMvc.perform(
            MockMvcRequestBuilders.get("/api/virkailija/kurssikoodit/tyypit")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$").value(IsCollectionWithSize.hasSize<Int>(10))
            )
    }

    @Test
    @Transactional
    fun getKurssikoodi() {
        initTest()

        val kurssikoodi = KurssikoodiHelper.createEntity(
            em,
            tunniste = "test1",
            tyyppi = OpintosuoritusTyyppiEnum.JOHTAMISOPINTO,
            yliopisto = defaultYliopisto
        )

        restKurssikoodiMockMvc.perform(
            MockMvcRequestBuilders.get("/api/virkailija/kurssikoodit/${kurssikoodi.id}")
                .accept(MediaType.APPLICATION_JSON)
        )
            .andExpect(MockMvcResultMatchers.status().isOk)
            .andExpect(
                MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON_VALUE)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.id").value(kurssikoodi.id)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.tunniste").value(kurssikoodi.tunniste)
            )
            .andExpect(
                MockMvcResultMatchers.jsonPath("$.tyyppi.id").value(kurssikoodi.tyyppi?.id)
            )
    }

    @Test
    @Transactional
    fun createKurssikoodi() {
        initTest()

        val kurssikoodiDTO = OpintosuoritusKurssikoodiDTO(
            tunniste = "test1",
            tyyppi = OpintosuoritusTyyppiDTO(id = 1)
        )

        val databaseSizeBeforeCreate = opintosuoritusKurssikoodiRepository.findAll().size

        restKurssikoodiMockMvc.perform(
            MockMvcRequestBuilders.post("/api/virkailija/kurssikoodit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kurssikoodiDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(MockMvcResultMatchers.status().isCreated)

        val kurssikooditList = opintosuoritusKurssikoodiRepository.findAll()
        assertThat(kurssikooditList).hasSize(databaseSizeBeforeCreate + 1)

        val kurssikoodi = kurssikooditList[kurssikooditList.size - 1]
        assertThat(kurssikoodi.tunniste).isEqualTo(kurssikoodiDTO.tunniste)
        assertThat(kurssikoodi.tyyppi?.id).isEqualTo(kurssikoodiDTO.tyyppi?.id)
        assertThat(kurssikoodi.yliopisto?.id).isEqualTo(defaultYliopisto.id)
    }

    @Test
    @Transactional
    fun updateKurssikoodi() {
        initTest()

        val entity = KurssikoodiHelper.createEntity(
            em,
            tunniste = "test1",
            tyyppi = OpintosuoritusTyyppiEnum.JOHTAMISOPINTO,
            yliopisto = defaultYliopisto
        )

        val updatedDTO = opintosuoritusKurssikoodiMapper.toDto(entity)
        updatedDTO.tunniste = "test2"

        val databaseSizeBeforeCreate = opintosuoritusKurssikoodiRepository.findAll().size

        restKurssikoodiMockMvc.perform(
            MockMvcRequestBuilders.put("/api/virkailija/kurssikoodit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(updatedDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(MockMvcResultMatchers.status().isOk)

        val kurssikooditList = opintosuoritusKurssikoodiRepository.findAll()
        assertThat(kurssikooditList).hasSize(databaseSizeBeforeCreate)

        val kurssikoodi = kurssikooditList[kurssikooditList.size - 1]
        assertThat(kurssikoodi.tunniste).isEqualTo(updatedDTO.tunniste)
        assertThat(kurssikoodi.tyyppi?.id).isEqualTo(entity.tyyppi?.id)
        assertThat(kurssikoodi.yliopisto?.id).isEqualTo(defaultYliopisto.id)
    }

    @Test
    @Transactional
    fun deleteKurssikoodi() {
        initTest()

        val entity = KurssikoodiHelper.createEntity(
            em,
            tunniste = "test1",
            tyyppi = OpintosuoritusTyyppiEnum.JOHTAMISOPINTO,
            yliopisto = defaultYliopisto
        )

        val databaseSizeBeforeCreate = opintosuoritusKurssikoodiRepository.findAll().size

        restKurssikoodiMockMvc.perform(
            MockMvcRequestBuilders.delete("/api/virkailija/kurssikoodit/${entity.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(MockMvcResultMatchers.status().isNoContent)

        val kurssikooditList = opintosuoritusKurssikoodiRepository.findAll()
        assertThat(kurssikooditList).hasSize(databaseSizeBeforeCreate - 1)
    }

    fun initTest() {
        val user = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(OPINTOHALLINNON_VIRKAILIJA)
        )
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>()
        val authorities = listOf(SimpleGrantedAuthority(OPINTOHALLINNON_VIRKAILIJA))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        virkailija = KayttajaHelper.createEntity(em, user)
        em.persist(virkailija)

        virkailija.yliopistot.add(defaultYliopisto)
    }

    companion object {
        private val defaultYliopistoEnum = YliopistoEnum.HELSINGIN_YLIOPISTO
        private val anotherYliopistoEnum = YliopistoEnum.OULUN_YLIOPISTO
    }
}
