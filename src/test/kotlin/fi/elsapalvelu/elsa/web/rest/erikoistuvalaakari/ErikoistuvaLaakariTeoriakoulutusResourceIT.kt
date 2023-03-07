package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Teoriakoulutus
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.TeoriakoulutusRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.service.mapper.TeoriakoulutusMapper
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.createByteArray
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper.Companion.ASIAKIRJA_PDF_NIMI
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoisalaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.OpintooikeusHelper
import fi.elsapalvelu.elsa.web.rest.helpers.OpintoopasHelper.Companion.DEFAULT_ERIKOISALAN_VAATIMA_TEORIAKOULUTUSTEN_VAHIMMAISMAARA
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDate
import java.time.ZoneId
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import jakarta.persistence.EntityManager
import kotlin.test.assertNotNull

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class ErikoistuvaLaakariTeoriakoulutusResourceIT {
    @Autowired
    private lateinit var teoriakoulutusRepository: TeoriakoulutusRepository

    @Autowired
    private lateinit var teoriakoulutusMapper: TeoriakoulutusMapper

    @Autowired
    private lateinit var erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository

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

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createTeoriakoulutus() {
        initTest()
        val databaseSizeBeforeCreate = teoriakoulutusRepository.findAll().size

        val teoriakoulutusDTO = teoriakoulutusMapper.toDto(teoriakoulutus)
        restTeoriakoulutusMockMvc.perform(
            multipart(ENTITY_API_URL)
                .file(
                    MockMultipartFile(
                        "todistusFiles",
                        "todistus.pdf",
                        "application/pdf",
                        DEFAULT_FILE
                    )
                )
                .param("koulutuksenNimi", teoriakoulutusDTO.koulutuksenNimi)
                .param("koulutuksenPaikka", teoriakoulutusDTO.koulutuksenPaikka)
                .param("alkamispaiva", teoriakoulutusDTO.alkamispaiva.toString())
                .param("paattymispaiva", teoriakoulutusDTO.paattymispaiva.toString())
                .param(
                    "erikoistumiseenHyvaksyttavaTuntimaara",
                    teoriakoulutusDTO.erikoistumiseenHyvaksyttavaTuntimaara.toString()
                )
                .with { it.method = "POST"; it }
                .with(csrf())
        ).andExpect(status().isCreated)

        val teoriakoulutusList = teoriakoulutusRepository.findAll()
        assertThat(teoriakoulutusList).hasSize(databaseSizeBeforeCreate + 1)
        val testTeoriakoulutus = teoriakoulutusList[teoriakoulutusList.size - 1]

        assertThat(testTeoriakoulutus.koulutuksenNimi).isEqualTo(DEFAULT_KOULUTUKSEN_NIMI)
        assertThat(testTeoriakoulutus.koulutuksenPaikka).isEqualTo(DEFAULT_KOULUTUKSEN_PAIKKA)
        assertThat(testTeoriakoulutus.alkamispaiva).isEqualTo(DEFAULT_ALKAMISPAIVA)
        assertThat(testTeoriakoulutus.paattymispaiva).isEqualTo(DEFAULT_PAATTYMISPAIVA)
        assertThat(testTeoriakoulutus.erikoistumiseenHyvaksyttavaTuntimaara).isEqualTo(
            DEFAULT_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA
        )
        assertThat(testTeoriakoulutus.todistukset.firstOrNull()?.asiakirjaData?.data)
            .isEqualTo(DEFAULT_FILE)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createTeoriakoulutusWithoutOpintooikeus() {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>(
        )
        val authorities = listOf(SimpleGrantedAuthority(ERIKOISTUVA_LAAKARI))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        teoriakoulutus = createEntity(em)
        val databaseSizeBeforeCreate = teoriakoulutusRepository.findAll().size

        val teoriakoulutusDTO = teoriakoulutusMapper.toDto(teoriakoulutus)
        restTeoriakoulutusMockMvc.perform(
            multipart(ENTITY_API_URL)
                .param("koulutuksenNimi", teoriakoulutusDTO.koulutuksenNimi)
                .param("koulutuksenPaikka", teoriakoulutusDTO.koulutuksenPaikka)
                .param("alkamispaiva", teoriakoulutusDTO.alkamispaiva.toString())
                .param("paattymispaiva", teoriakoulutusDTO.paattymispaiva.toString())
                .param(
                    "erikoistumiseenHyvaksyttavaTuntimaara",
                    teoriakoulutusDTO.erikoistumiseenHyvaksyttavaTuntimaara.toString()
                )
                .with { it.method = "POST"; it }
                .with(csrf())
        ).andExpect(status().is5xxServerError)

        val teoriakoulutusList = teoriakoulutusRepository.findAll()
        assertThat(teoriakoulutusList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createTeoriakoulutusWithExistingId() {
        initTest()
        teoriakoulutus.id = 1L
        val teoriakoulutusDTO = teoriakoulutusMapper.toDto(teoriakoulutus)

        val databaseSizeBeforeCreate = teoriakoulutusRepository.findAll().size

        restTeoriakoulutusMockMvc.perform(
            multipart(ENTITY_API_URL)
                .param("id", teoriakoulutusDTO.id.toString())
                .param("koulutuksenNimi", teoriakoulutusDTO.koulutuksenNimi)
                .param("koulutuksenPaikka", teoriakoulutusDTO.koulutuksenPaikka)
                .param("alkamispaiva", teoriakoulutusDTO.alkamispaiva.toString())
                .param("paattymispaiva", teoriakoulutusDTO.paattymispaiva.toString())
                .param(
                    "erikoistumiseenHyvaksyttavaTuntimaara",
                    teoriakoulutusDTO.erikoistumiseenHyvaksyttavaTuntimaara.toString()
                )
                .with { it.method = "POST"; it }
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val teoriakoulutusList = teoriakoulutusRepository.findAll()
        assertThat(teoriakoulutusList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun checkKoulutuksenNimiIsRequired() {
        initTest()
        val databaseSizeBeforeTest = teoriakoulutusRepository.findAll().size
        teoriakoulutus.koulutuksenNimi = null

        val teoriakoulutusDTO = teoriakoulutusMapper.toDto(teoriakoulutus)

        restTeoriakoulutusMockMvc.perform(
            multipart(ENTITY_API_URL)
                .param("koulutuksenNimi", teoriakoulutusDTO.koulutuksenNimi)
                .param("koulutuksenPaikka", teoriakoulutusDTO.koulutuksenPaikka)
                .param("alkamispaiva", teoriakoulutusDTO.alkamispaiva.toString())
                .param("paattymispaiva", teoriakoulutusDTO.paattymispaiva.toString())
                .param(
                    "erikoistumiseenHyvaksyttavaTuntimaara",
                    teoriakoulutusDTO.erikoistumiseenHyvaksyttavaTuntimaara.toString()
                )
                .with { it.method = "POST"; it }
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val teoriakoulutusList = teoriakoulutusRepository.findAll()
        assertThat(teoriakoulutusList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun checkKoulutuksenPaikkaIsRequired() {
        initTest()
        val databaseSizeBeforeTest = teoriakoulutusRepository.findAll().size
        teoriakoulutus.koulutuksenPaikka = null

        val teoriakoulutusDTO = teoriakoulutusMapper.toDto(teoriakoulutus)

        restTeoriakoulutusMockMvc.perform(
            multipart(ENTITY_API_URL)
                .param("koulutuksenNimi", teoriakoulutusDTO.koulutuksenNimi)
                .param("koulutuksenPaikka", teoriakoulutusDTO.koulutuksenPaikka)
                .param("alkamispaiva", teoriakoulutusDTO.alkamispaiva.toString())
                .param("paattymispaiva", teoriakoulutusDTO.paattymispaiva.toString())
                .param(
                    "erikoistumiseenHyvaksyttavaTuntimaara",
                    teoriakoulutusDTO.erikoistumiseenHyvaksyttavaTuntimaara.toString()
                )
                .with { it.method = "POST"; it }
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val teoriakoulutusList = teoriakoulutusRepository.findAll()
        assertThat(teoriakoulutusList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun checkAlkamispaivaIsRequired() {
        initTest()
        val databaseSizeBeforeTest = teoriakoulutusRepository.findAll().size
        teoriakoulutus.alkamispaiva = null

        val teoriakoulutusDTO = teoriakoulutusMapper.toDto(teoriakoulutus)

        restTeoriakoulutusMockMvc.perform(
            multipart(ENTITY_API_URL)
                .param("koulutuksenNimi", teoriakoulutusDTO.koulutuksenNimi)
                .param("koulutuksenPaikka", teoriakoulutusDTO.koulutuksenPaikka)
                .param("alkamispaiva", teoriakoulutusDTO.alkamispaiva.toString())
                .param("paattymispaiva", teoriakoulutusDTO.paattymispaiva.toString())
                .param(
                    "erikoistumiseenHyvaksyttavaTuntimaara",
                    teoriakoulutusDTO.erikoistumiseenHyvaksyttavaTuntimaara.toString()
                )
                .with { it.method = "POST"; it }
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val teoriakoulutusList = teoriakoulutusRepository.findAll()
        assertThat(teoriakoulutusList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllTeoriakoulutukset() {
        initTest()
        val erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)
        em.flush()
        teoriakoulutus.opintooikeus?.erikoisala = erikoisala
        teoriakoulutusRepository.saveAndFlush(teoriakoulutus)

        restTeoriakoulutusMockMvc.perform(get(ENTITY_API_URL))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.teoriakoulutukset.[*].id").value(hasItem(teoriakoulutus.id?.toInt())))
            .andExpect(jsonPath("$.teoriakoulutukset.[*].koulutuksenNimi").value(hasItem(DEFAULT_KOULUTUKSEN_NIMI)))
            .andExpect(jsonPath("$.teoriakoulutukset.[*].koulutuksenPaikka").value(hasItem(DEFAULT_KOULUTUKSEN_PAIKKA)))
            .andExpect(jsonPath("$.teoriakoulutukset.[*].alkamispaiva").value(hasItem(DEFAULT_ALKAMISPAIVA.toString())))
            .andExpect(
                jsonPath("$.teoriakoulutukset.[*].paattymispaiva").value(hasItem(DEFAULT_PAATTYMISPAIVA.toString()))
            )
            .andExpect(
                jsonPath("$.teoriakoulutukset.[*].erikoistumiseenHyvaksyttavaTuntimaara").value(
                    hasItem(
                        DEFAULT_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA
                    )
                )
            )
            .andExpect(
                jsonPath("$.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara").value(
                    DEFAULT_ERIKOISALAN_VAATIMA_TEORIAKOULUTUSTEN_VAHIMMAISMAARA
                )
            )
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllTeoriakoulutuksetShouldReturnOnlyForOpintooikeusKaytossa() {
        initTest()
        val erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)
        em.flush()
        teoriakoulutus.opintooikeus?.erikoisala = erikoisala
        teoriakoulutusRepository.saveAndFlush(teoriakoulutus)

        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(user.id!!)
        requireNotNull(erikoistuvaLaakari)
        val newOpintooikeus = OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(em, erikoistuvaLaakari)
        OpintooikeusHelper.setOpintooikeusKaytossa(erikoistuvaLaakari, newOpintooikeus)

        val teoriakoulutusForAnotherOpintooikeus = createEntity(em, user)
        em.persist(teoriakoulutusForAnotherOpintooikeus)

        restTeoriakoulutusMockMvc.perform(get(ENTITY_API_URL))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.teoriakoulutukset").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$.teoriakoulutukset[0].id").value(teoriakoulutusForAnotherOpintooikeus.id))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getTeoriakoulutus() {
        initTest()
        teoriakoulutusRepository.saveAndFlush(teoriakoulutus)

        val id = teoriakoulutus.id
        assertNotNull(id)

        restTeoriakoulutusMockMvc.perform(get(ENTITY_API_URL_ID, teoriakoulutus.id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(teoriakoulutus.id?.toInt()))
            .andExpect(jsonPath("$.koulutuksenNimi").value(DEFAULT_KOULUTUKSEN_NIMI))
            .andExpect(jsonPath("$.koulutuksenPaikka").value(DEFAULT_KOULUTUKSEN_PAIKKA))
            .andExpect(jsonPath("$.alkamispaiva").value(DEFAULT_ALKAMISPAIVA.toString()))
            .andExpect(jsonPath("$.paattymispaiva").value(DEFAULT_PAATTYMISPAIVA.toString()))
            .andExpect(
                jsonPath("$.erikoistumiseenHyvaksyttavaTuntimaara").value(
                    DEFAULT_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA
                )
            )
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingTeoriakoulutus() {
        initTest()
        restTeoriakoulutusMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun putNewTeoriakoulutus() {
        initTest()
        val asiakirja = AsiakirjaHelper.createEntity(em, user, null, teoriakoulutus)
        teoriakoulutus.todistukset.add(asiakirja)
        teoriakoulutusRepository.saveAndFlush(teoriakoulutus)

        val databaseSizeBeforeUpdate = teoriakoulutusRepository.findAll().size

        val updatedTeoriakoulutus = teoriakoulutusRepository.findById(teoriakoulutus.id!!).get()
        val asiakirjaId = updatedTeoriakoulutus.todistukset.firstOrNull()?.id
        em.detach(updatedTeoriakoulutus)
        updatedTeoriakoulutus.koulutuksenNimi = UPDATED_KOULUTUKSEN_NIMI
        updatedTeoriakoulutus.koulutuksenPaikka = UPDATED_KOULUTUKSEN_PAIKKA
        updatedTeoriakoulutus.alkamispaiva = UPDATED_ALKAMISPAIVA
        updatedTeoriakoulutus.paattymispaiva = UPDATED_PAATTYMISPAIVA
        updatedTeoriakoulutus.erikoistumiseenHyvaksyttavaTuntimaara = UPDATED_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA
        val teoriakoulutusDTO = teoriakoulutusMapper.toDto(updatedTeoriakoulutus)

        restTeoriakoulutusMockMvc.perform(
            multipart(ENTITY_API_URL_ID, teoriakoulutus.id)
                .file(
                    MockMultipartFile(
                        "todistusFiles",
                        "todistus.pdf",
                        "application/pdf",
                        UPDATED_FILE
                    )
                )
                .param("deletedAsiakirjaIdsJson", "[$asiakirjaId]")
                .param("id", teoriakoulutusDTO.id.toString())
                .param("koulutuksenNimi", teoriakoulutusDTO.koulutuksenNimi)
                .param("koulutuksenPaikka", teoriakoulutusDTO.koulutuksenPaikka)
                .param("alkamispaiva", teoriakoulutusDTO.alkamispaiva.toString())
                .param("paattymispaiva", teoriakoulutusDTO.paattymispaiva.toString())
                .param(
                    "erikoistumiseenHyvaksyttavaTuntimaara",
                    teoriakoulutusDTO.erikoistumiseenHyvaksyttavaTuntimaara.toString()
                )
                .with { it.method = "PUT"; it }
                .with(csrf())
        ).andExpect(status().isOk)

        val teoriakoulutusList = teoriakoulutusRepository.findAll()
        assertThat(teoriakoulutusList).hasSize(databaseSizeBeforeUpdate)
        val testTeoriakoulutus = teoriakoulutusList[teoriakoulutusList.size - 1]
        assertThat(testTeoriakoulutus.koulutuksenNimi).isEqualTo(UPDATED_KOULUTUKSEN_NIMI)
        assertThat(testTeoriakoulutus.koulutuksenPaikka).isEqualTo(UPDATED_KOULUTUKSEN_PAIKKA)
        assertThat(testTeoriakoulutus.alkamispaiva).isEqualTo(UPDATED_ALKAMISPAIVA)
        assertThat(testTeoriakoulutus.paattymispaiva).isEqualTo(UPDATED_PAATTYMISPAIVA)
        assertThat(testTeoriakoulutus.erikoistumiseenHyvaksyttavaTuntimaara).isEqualTo(
            UPDATED_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA
        )
        assertThat(testTeoriakoulutus.todistukset).hasSize(1)
        assertThat(testTeoriakoulutus.todistukset.firstOrNull()?.asiakirjaData?.data)
            .isEqualTo(UPDATED_FILE)
    }

    @Test
    @Transactional
    fun putNewTeoriakoulutusWithSameAsiakirjaNimi() {
        initTest()
        val asiakirja = AsiakirjaHelper.createEntity(em, user, null, teoriakoulutus)
        teoriakoulutus.todistukset.add(asiakirja)
        teoriakoulutusRepository.saveAndFlush(teoriakoulutus)

        val databaseSizeBeforeUpdate = teoriakoulutusRepository.findAll().size

        val updatedTeoriakoulutus = teoriakoulutusRepository.findById(teoriakoulutus.id!!).get()
        em.detach(updatedTeoriakoulutus)
        updatedTeoriakoulutus.koulutuksenNimi = UPDATED_KOULUTUKSEN_NIMI
        updatedTeoriakoulutus.koulutuksenPaikka = UPDATED_KOULUTUKSEN_PAIKKA
        updatedTeoriakoulutus.alkamispaiva = UPDATED_ALKAMISPAIVA
        updatedTeoriakoulutus.paattymispaiva = UPDATED_PAATTYMISPAIVA
        updatedTeoriakoulutus.erikoistumiseenHyvaksyttavaTuntimaara = UPDATED_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA
        val teoriakoulutusDTO = teoriakoulutusMapper.toDto(updatedTeoriakoulutus)

        restTeoriakoulutusMockMvc.perform(
            multipart(ENTITY_API_URL_ID, teoriakoulutus.id)
                .file(
                    MockMultipartFile(
                        "todistusFiles",
                        ASIAKIRJA_PDF_NIMI,
                        "application/pdf",
                        UPDATED_FILE
                    )
                )
                .param("id", teoriakoulutusDTO.id.toString())
                .param("koulutuksenNimi", teoriakoulutusDTO.koulutuksenNimi)
                .param("koulutuksenPaikka", teoriakoulutusDTO.koulutuksenPaikka)
                .param("alkamispaiva", teoriakoulutusDTO.alkamispaiva.toString())
                .param("paattymispaiva", teoriakoulutusDTO.paattymispaiva.toString())
                .param(
                    "erikoistumiseenHyvaksyttavaTuntimaara",
                    teoriakoulutusDTO.erikoistumiseenHyvaksyttavaTuntimaara.toString()
                )
                .with { it.method = "PUT"; it }
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val teoriakoulutusList = teoriakoulutusRepository.findAll()
        assertThat(teoriakoulutusList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun putNonExistingTeoriakoulutus() {
        initTest()
        val databaseSizeBeforeUpdate = teoriakoulutusRepository.findAll().size
        teoriakoulutus.id = count.incrementAndGet()

        val teoriakoulutusDTO = teoriakoulutusMapper.toDto(teoriakoulutus)

        restTeoriakoulutusMockMvc.perform(
            multipart(ENTITY_API_URL_ID, teoriakoulutus.id)
                .param("id", teoriakoulutusDTO.id.toString())
                .param("koulutuksenNimi", teoriakoulutusDTO.koulutuksenNimi)
                .param("koulutuksenPaikka", teoriakoulutusDTO.koulutuksenPaikka)
                .param("alkamispaiva", teoriakoulutusDTO.alkamispaiva.toString())
                .param("paattymispaiva", teoriakoulutusDTO.paattymispaiva.toString())
                .param(
                    "erikoistumiseenHyvaksyttavaTuntimaara",
                    teoriakoulutusDTO.erikoistumiseenHyvaksyttavaTuntimaara.toString()
                )
                .with { it.method = "PUT"; it }
                .with(csrf())
        )
            .andExpect(status().isBadRequest)

        val teoriakoulutusList = teoriakoulutusRepository.findAll()
        assertThat(teoriakoulutusList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun putWithIdMismatchTeoriakoulutus() {
        initTest()
        val databaseSizeBeforeUpdate = teoriakoulutusRepository.findAll().size
        teoriakoulutus.id = count.incrementAndGet()

        val teoriakoulutusDTO = teoriakoulutusMapper.toDto(teoriakoulutus)

        restTeoriakoulutusMockMvc.perform(
            multipart(ENTITY_API_URL_ID, count.incrementAndGet())
                .param("id", teoriakoulutusDTO.id.toString())
                .param("koulutuksenNimi", teoriakoulutusDTO.koulutuksenNimi)
                .param("koulutuksenPaikka", teoriakoulutusDTO.koulutuksenPaikka)
                .param("alkamispaiva", teoriakoulutusDTO.alkamispaiva.toString())
                .param("paattymispaiva", teoriakoulutusDTO.paattymispaiva.toString())
                .param(
                    "erikoistumiseenHyvaksyttavaTuntimaara",
                    teoriakoulutusDTO.erikoistumiseenHyvaksyttavaTuntimaara.toString()
                )
                .with { it.method = "PUT"; it }
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val teoriakoulutusList = teoriakoulutusRepository.findAll()
        assertThat(teoriakoulutusList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun putWithMissingIdTeoriakoulutus() {
        initTest()
        teoriakoulutusRepository.saveAndFlush(teoriakoulutus)

        val databaseSizeBeforeUpdate = teoriakoulutusRepository.findAll().size

        val updatedTeoriakoulutus = teoriakoulutusRepository.findById(teoriakoulutus.id!!).get()
        em.detach(updatedTeoriakoulutus)
        updatedTeoriakoulutus.koulutuksenNimi = UPDATED_KOULUTUKSEN_NIMI
        updatedTeoriakoulutus.koulutuksenPaikka = UPDATED_KOULUTUKSEN_PAIKKA
        updatedTeoriakoulutus.alkamispaiva = UPDATED_ALKAMISPAIVA
        updatedTeoriakoulutus.paattymispaiva = UPDATED_PAATTYMISPAIVA
        updatedTeoriakoulutus.erikoistumiseenHyvaksyttavaTuntimaara = UPDATED_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA
        val teoriakoulutusDTO = teoriakoulutusMapper.toDto(updatedTeoriakoulutus)

        restTeoriakoulutusMockMvc.perform(
            multipart(ENTITY_API_URL_ID, teoriakoulutus.id)
                .param("id", null)
                .param("koulutuksenNimi", teoriakoulutusDTO.koulutuksenNimi)
                .param("koulutuksenPaikka", teoriakoulutusDTO.koulutuksenPaikka)
                .param("alkamispaiva", teoriakoulutusDTO.alkamispaiva.toString())
                .param("paattymispaiva", teoriakoulutusDTO.paattymispaiva.toString())
                .param(
                    "erikoistumiseenHyvaksyttavaTuntimaara",
                    teoriakoulutusDTO.erikoistumiseenHyvaksyttavaTuntimaara.toString()
                )
                .with { it.method = "PUT"; it }
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val teoriakoulutusList = teoriakoulutusRepository.findAll()
        assertThat(teoriakoulutusList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun putWithMissingIdPathParamTeoriakoulutus() {
        initTest()
        val databaseSizeBeforeUpdate = teoriakoulutusRepository.findAll().size

        val teoriakoulutusDTO = teoriakoulutusMapper.toDto(teoriakoulutus)

        restTeoriakoulutusMockMvc.perform(
            multipart(ENTITY_API_URL)
                .param("id", teoriakoulutusDTO.id.toString())
                .param("koulutuksenNimi", teoriakoulutusDTO.koulutuksenNimi)
                .param("koulutuksenPaikka", teoriakoulutusDTO.koulutuksenPaikka)
                .param("alkamispaiva", teoriakoulutusDTO.alkamispaiva.toString())
                .param("paattymispaiva", teoriakoulutusDTO.paattymispaiva.toString())
                .param(
                    "erikoistumiseenHyvaksyttavaTuntimaara",
                    teoriakoulutusDTO.erikoistumiseenHyvaksyttavaTuntimaara.toString()
                )
                .with { it.method = "PUT"; it }
                .with(csrf())
        )
            .andExpect(status().isMethodNotAllowed)

        val teoriakoulutusList = teoriakoulutusRepository.findAll()
        assertThat(teoriakoulutusList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deleteTeoriakoulutus() {
        initTest()
        teoriakoulutusRepository.saveAndFlush(teoriakoulutus)

        val databaseSizeBeforeDelete = teoriakoulutusRepository.findAll().size

        restTeoriakoulutusMockMvc.perform(
            delete(ENTITY_API_URL_ID, teoriakoulutus.id).with(csrf())
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        val teoriakoulutusList = teoriakoulutusRepository.findAll()
        assertThat(teoriakoulutusList).hasSize(databaseSizeBeforeDelete - 1)
    }

    fun initTest() {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>(
        )
        val authorities = listOf(SimpleGrantedAuthority(ERIKOISTUVA_LAAKARI))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        teoriakoulutus = createEntity(em, user)
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

        val DEFAULT_FILE: ByteArray = createByteArray(1, "0")
        val UPDATED_FILE: ByteArray = createByteArray(1, "1")

        private val ENTITY_API_URL: String = "/api/erikoistuva-laakari/teoriakoulutukset"
        private val ENTITY_API_URL_ID: String = ENTITY_API_URL + "/{id}"

        private val random: Random = SecureRandom()
        private val count: AtomicLong = AtomicLong(random.nextInt().toLong() + (2L * Integer.MAX_VALUE))

        @JvmStatic
        fun createEntity(em: EntityManager, user: User? = null): Teoriakoulutus {
            val teoriakoulutus = Teoriakoulutus(
                koulutuksenNimi = DEFAULT_KOULUTUKSEN_NIMI,
                koulutuksenPaikka = DEFAULT_KOULUTUKSEN_PAIKKA,
                alkamispaiva = DEFAULT_ALKAMISPAIVA,
                paattymispaiva = DEFAULT_PAATTYMISPAIVA,
                erikoistumiseenHyvaksyttavaTuntimaara = DEFAULT_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA
            )

            // Lisätään pakollinen tieto
            var erikoistuvaLaakari =
                em.findAll(ErikoistuvaLaakari::class).firstOrNull { it.kayttaja?.user == user }
            if (erikoistuvaLaakari == null) {
                erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
                em.persist(erikoistuvaLaakari)
                em.flush()
            }
            teoriakoulutus.opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()

            return teoriakoulutus
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Teoriakoulutus {
            val teoriakoulutus = Teoriakoulutus(
                koulutuksenNimi = UPDATED_KOULUTUKSEN_NIMI,
                koulutuksenPaikka = UPDATED_KOULUTUKSEN_PAIKKA,
                alkamispaiva = UPDATED_ALKAMISPAIVA,
                paattymispaiva = UPDATED_PAATTYMISPAIVA,
                erikoistumiseenHyvaksyttavaTuntimaara = UPDATED_ERIKOISTUMISEEN_HYVAKSYTTAVA_TUNTIMAARA
            )

            // Lisätään pakollinen tieto
            val erikoistuvaLaakari: ErikoistuvaLaakari
            if (em.findAll(ErikoistuvaLaakari::class).isEmpty()) {
                erikoistuvaLaakari = ErikoistuvaLaakariHelper.createUpdatedEntity(em)
                em.persist(erikoistuvaLaakari)
                em.flush()
            } else {
                erikoistuvaLaakari = em.findAll(ErikoistuvaLaakari::class).get(0)
            }
            teoriakoulutus.opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()

            return teoriakoulutus
        }
    }
}
