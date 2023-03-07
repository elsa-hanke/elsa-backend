package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Paivakirjamerkinta
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.PaivakirjamerkintaRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.service.mapper.PaivakirjamerkintaMapper
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.OpintooikeusHelper
import fi.elsapalvelu.elsa.web.rest.helpers.TeoriakoulutusHelper
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
class ErikoistuvaLaakariPaivakirjamerkintaResourceIT {
    @Autowired
    private lateinit var paivakirjamerkintaRepository: PaivakirjamerkintaRepository

    @Autowired
    private lateinit var erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository

    @Autowired
    private lateinit var paivakirjamerkintaMapper: PaivakirjamerkintaMapper

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restPaivakirjamerkintaMockMvc: MockMvc

    private lateinit var paivakirjamerkinta: Paivakirjamerkinta

    private lateinit var user: User

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createPaivakirjamerkinta() {
        initTest()
        val databaseSizeBeforeCreate = paivakirjamerkintaRepository.findAll().size

        val paivakirjamerkintaDTO = paivakirjamerkintaMapper.toDto(paivakirjamerkinta)
        restPaivakirjamerkintaMockMvc.perform(
            post(ENTITY_API_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(paivakirjamerkintaDTO))
        ).andExpect(status().isCreated)

        val paivakirjamerkintaList = paivakirjamerkintaRepository.findAll()
        assertThat(paivakirjamerkintaList).hasSize(databaseSizeBeforeCreate + 1)
        val testPaivakirjamerkinta = paivakirjamerkintaList[paivakirjamerkintaList.size - 1]

        assertThat(testPaivakirjamerkinta.paivamaara).isEqualTo(DEFAULT_PAIVAMAARA)
        assertThat(testPaivakirjamerkinta.oppimistapahtumanNimi).isEqualTo(DEFAULT_OPPIMISTAPAHTUMAN_NIMI)
        assertThat(testPaivakirjamerkinta.muunAiheenNimi).isEqualTo(DEFAULT_MUUN_AIHEEN_NIMI)
        assertThat(testPaivakirjamerkinta.reflektio).isEqualTo(DEFAULT_REFLEKTIO)
        assertThat(testPaivakirjamerkinta.yksityinen).isEqualTo(DEFAULT_YKSITYINEN)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createPaivakirjamerkintaWithoutOpintooikeus() {
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

        paivakirjamerkinta = createEntity(em)

        val paivakirjamerkintaDTO = paivakirjamerkintaMapper.toDto(paivakirjamerkinta)
        restPaivakirjamerkintaMockMvc.perform(
            post(ENTITY_API_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(paivakirjamerkintaDTO))
        ).andExpect(status().is5xxServerError)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun createPaivakirjamerkintaWithExistingId() {
        initTest()
        paivakirjamerkinta.id = 1L
        val paivakirjamerkintaDTO = paivakirjamerkintaMapper.toDto(paivakirjamerkinta)

        val databaseSizeBeforeCreate = paivakirjamerkintaRepository.findAll().size

        restPaivakirjamerkintaMockMvc.perform(
            post(ENTITY_API_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(paivakirjamerkintaDTO))
        ).andExpect(status().isBadRequest)

        val paivakirjamerkintaList = paivakirjamerkintaRepository.findAll()
        assertThat(paivakirjamerkintaList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun checkPaivamaaraIsRequired() {
        initTest()
        val databaseSizeBeforeTest = paivakirjamerkintaRepository.findAll().size
        paivakirjamerkinta.paivamaara = null

        val paivakirjamerkintaDTO = paivakirjamerkintaMapper.toDto(paivakirjamerkinta)

        restPaivakirjamerkintaMockMvc.perform(
            post(ENTITY_API_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(paivakirjamerkintaDTO))
        ).andExpect(status().isBadRequest)

        val paivakirjamerkintaList = paivakirjamerkintaRepository.findAll()
        assertThat(paivakirjamerkintaList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun checkOppimistapahtumanNimiIsRequired() {
        initTest()
        val databaseSizeBeforeTest = paivakirjamerkintaRepository.findAll().size
        paivakirjamerkinta.oppimistapahtumanNimi = null

        val paivakirjamerkintaDTO = paivakirjamerkintaMapper.toDto(paivakirjamerkinta)

        restPaivakirjamerkintaMockMvc.perform(
            post(ENTITY_API_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(paivakirjamerkintaDTO))
        ).andExpect(status().isBadRequest)

        val paivakirjamerkintaList = paivakirjamerkintaRepository.findAll()
        assertThat(paivakirjamerkintaList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllPaivakirjamerkinnat() {
        initTest()
        paivakirjamerkintaRepository.saveAndFlush(paivakirjamerkinta)

        restPaivakirjamerkintaMockMvc.perform(get("$ENTITY_API_URL?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content.[*].id").value(hasItem(paivakirjamerkinta.id?.toInt())))
            .andExpect(jsonPath("$.content.[*].paivamaara").value(hasItem(DEFAULT_PAIVAMAARA.toString())))
            .andExpect(jsonPath("$.content.[*].oppimistapahtumanNimi").value(hasItem(DEFAULT_OPPIMISTAPAHTUMAN_NIMI)))
            .andExpect(jsonPath("$.content.[*].muunAiheenNimi").value(hasItem(DEFAULT_MUUN_AIHEEN_NIMI)))
            .andExpect(jsonPath("$.content.[*].reflektio").value(hasItem(DEFAULT_REFLEKTIO)))
            .andExpect(jsonPath("$.content.[*].yksityinen").value(hasItem(DEFAULT_YKSITYINEN)))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllPaivakirjamerkinnatShouldReturnOnlyForOpintooikeusKaytossa() {
        initTest()
        paivakirjamerkintaRepository.saveAndFlush(paivakirjamerkinta)

        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(user.id!!)
        requireNotNull(erikoistuvaLaakari)
        val newOpintooikeus = OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(em, erikoistuvaLaakari)
        OpintooikeusHelper.setOpintooikeusKaytossa(erikoistuvaLaakari, newOpintooikeus)

        val paivakirjamerkintaForAnotherOpintooikeus = createEntity(em, user)
        em.persist(paivakirjamerkintaForAnotherOpintooikeus)

        restPaivakirjamerkintaMockMvc.perform(get("$ENTITY_API_URL?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$.content.[0].id").value(paivakirjamerkintaForAnotherOpintooikeus.id))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getPaivakirjamerkinta() {
        initTest()
        paivakirjamerkintaRepository.saveAndFlush(paivakirjamerkinta)

        val id = paivakirjamerkinta.id
        assertNotNull(id)

        restPaivakirjamerkintaMockMvc.perform(get(ENTITY_API_URL_ID, paivakirjamerkinta.id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(paivakirjamerkinta.id?.toInt()))
            .andExpect(jsonPath("$.paivamaara").value(DEFAULT_PAIVAMAARA.toString()))
            .andExpect(jsonPath("$.oppimistapahtumanNimi").value(DEFAULT_OPPIMISTAPAHTUMAN_NIMI))
            .andExpect(jsonPath("$.muunAiheenNimi").value(DEFAULT_MUUN_AIHEEN_NIMI))
            .andExpect(jsonPath("$.reflektio").value(DEFAULT_REFLEKTIO))
            .andExpect(jsonPath("$.yksityinen").value(DEFAULT_YKSITYINEN))
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllPaivakirjamerkinnatByPaivamaaraIsEqualToSomething() {
        initTest()
        paivakirjamerkintaRepository.saveAndFlush(paivakirjamerkinta)

        defaultPaivakirjamerkintaShouldBeFound("paivamaara.equals=$DEFAULT_PAIVAMAARA")
        defaultPaivakirjamerkintaShouldNotBeFound("paivamaara.equals=$UPDATED_PAIVAMAARA")
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllPaivakirjamerkinnatByPaivamaaraIsNullOrNotNull() {
        initTest()
        paivakirjamerkintaRepository.saveAndFlush(paivakirjamerkinta)

        defaultPaivakirjamerkintaShouldBeFound("paivamaara.specified=true")
        defaultPaivakirjamerkintaShouldNotBeFound("paivamaara.specified=false")
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getAllPaivakirjamerkinnatByPaivamaaraIsGreaterThanOrEqualToSomething() {
        initTest()
        paivakirjamerkintaRepository.saveAndFlush(paivakirjamerkinta)

        defaultPaivakirjamerkintaShouldBeFound("paivamaara.greaterThanOrEqual=$DEFAULT_PAIVAMAARA")
        defaultPaivakirjamerkintaShouldNotBeFound("paivamaara.greaterThanOrEqual=$UPDATED_PAIVAMAARA")
    }

    @Throws(Exception::class)
    private fun defaultPaivakirjamerkintaShouldBeFound(filter: String) {
        restPaivakirjamerkintaMockMvc.perform(get("$ENTITY_API_URL?sort=id,desc&$filter"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content.[*].id").value(hasItem(paivakirjamerkinta.id?.toInt())))
            .andExpect(jsonPath("$.content.[*].paivamaara").value(hasItem(DEFAULT_PAIVAMAARA.toString())))
            .andExpect(jsonPath("$.content.[*].oppimistapahtumanNimi").value(hasItem(DEFAULT_OPPIMISTAPAHTUMAN_NIMI)))
            .andExpect(jsonPath("$.content.[*].muunAiheenNimi").value(hasItem(DEFAULT_MUUN_AIHEEN_NIMI)))
            .andExpect(jsonPath("$.content.[*].reflektio").value(hasItem(DEFAULT_REFLEKTIO)))
            .andExpect(jsonPath("$.content.[*].yksityinen").value(hasItem(DEFAULT_YKSITYINEN)))
    }

    @Throws(Exception::class)
    private fun defaultPaivakirjamerkintaShouldNotBeFound(filter: String) {
        restPaivakirjamerkintaMockMvc.perform(get("$ENTITY_API_URL?sort=id,desc&$filter"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").isArray)
            .andExpect(jsonPath("$.content").isEmpty)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun getNonExistingPaivakirjamerkinta() {
        initTest()
        restPaivakirjamerkintaMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun putNewPaivakirjamerkinta() {
        initTest()
        paivakirjamerkintaRepository.saveAndFlush(paivakirjamerkinta)

        val databaseSizeBeforeUpdate = paivakirjamerkintaRepository.findAll().size

        val updatedPaivakirjamerkinta = paivakirjamerkintaRepository.findById(paivakirjamerkinta.id!!).get()
        em.detach(updatedPaivakirjamerkinta)
        updatedPaivakirjamerkinta.paivamaara = UPDATED_PAIVAMAARA
        updatedPaivakirjamerkinta.oppimistapahtumanNimi = UPDATED_OPPIMISTAPAHTUMAN_NIMI
        updatedPaivakirjamerkinta.muunAiheenNimi = UPDATED_MUUN_AIHEEN_NIMI
        updatedPaivakirjamerkinta.reflektio = UPDATED_REFLEKTIO
        updatedPaivakirjamerkinta.yksityinen = UPDATED_YKSITYINEN
        val paivakirjamerkintaDTO = paivakirjamerkintaMapper.toDto(updatedPaivakirjamerkinta)

        restPaivakirjamerkintaMockMvc.perform(
            put(ENTITY_API_URL_ID, paivakirjamerkintaDTO.id).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(paivakirjamerkintaDTO))
        ).andExpect(status().isOk)

        val paivakirjamerkintaList = paivakirjamerkintaRepository.findAll()
        assertThat(paivakirjamerkintaList).hasSize(databaseSizeBeforeUpdate)
        val testPaivakirjamerkinta = paivakirjamerkintaList[paivakirjamerkintaList.size - 1]
        assertThat(testPaivakirjamerkinta.paivamaara).isEqualTo(UPDATED_PAIVAMAARA)
        assertThat(testPaivakirjamerkinta.oppimistapahtumanNimi).isEqualTo(UPDATED_OPPIMISTAPAHTUMAN_NIMI)
        assertThat(testPaivakirjamerkinta.muunAiheenNimi).isEqualTo(UPDATED_MUUN_AIHEEN_NIMI)
        assertThat(testPaivakirjamerkinta.reflektio).isEqualTo(UPDATED_REFLEKTIO)
        assertThat(testPaivakirjamerkinta.yksityinen).isEqualTo(UPDATED_YKSITYINEN)
    }

    @Test
    @Transactional
    fun putNonExistingPaivakirjamerkinta() {
        initTest()
        val databaseSizeBeforeUpdate = paivakirjamerkintaRepository.findAll().size
        paivakirjamerkinta.id = count.incrementAndGet()

        val paivakirjamerkintaDTO = paivakirjamerkintaMapper.toDto(paivakirjamerkinta)

        restPaivakirjamerkintaMockMvc.perform(
            put(ENTITY_API_URL_ID, paivakirjamerkintaDTO.id).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(paivakirjamerkintaDTO))
        )
            .andExpect(status().isBadRequest)

        val paivakirjamerkintaList = paivakirjamerkintaRepository.findAll()
        assertThat(paivakirjamerkintaList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun putWithMissingIdPaivakirjamerkinta() {
        initTest()
        paivakirjamerkintaRepository.saveAndFlush(paivakirjamerkinta)

        val updatedPaivakirjamerkinta = paivakirjamerkintaRepository.findById(paivakirjamerkinta.id!!).get()
        em.detach(updatedPaivakirjamerkinta)
        val id = updatedPaivakirjamerkinta.id
        updatedPaivakirjamerkinta.id = null
        updatedPaivakirjamerkinta.paivamaara = UPDATED_PAIVAMAARA
        updatedPaivakirjamerkinta.oppimistapahtumanNimi = UPDATED_OPPIMISTAPAHTUMAN_NIMI
        updatedPaivakirjamerkinta.muunAiheenNimi = UPDATED_MUUN_AIHEEN_NIMI
        updatedPaivakirjamerkinta.reflektio = UPDATED_REFLEKTIO
        updatedPaivakirjamerkinta.yksityinen = UPDATED_YKSITYINEN
        val paivakirjamerkintaDTO = paivakirjamerkintaMapper.toDto(updatedPaivakirjamerkinta)

        restPaivakirjamerkintaMockMvc.perform(
            put(ENTITY_API_URL_ID, id).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(paivakirjamerkintaDTO))
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun putWithIdMismatchPaivakirjamerkinta() {
        initTest()
        val databaseSizeBeforeUpdate = paivakirjamerkintaRepository.findAll().size
        paivakirjamerkinta.id = count.incrementAndGet()

        val paivakirjamerkintaDTO = paivakirjamerkintaMapper.toDto(paivakirjamerkinta)

        restPaivakirjamerkintaMockMvc.perform(
            put(ENTITY_API_URL_ID, count.incrementAndGet()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(paivakirjamerkintaDTO))
        ).andExpect(status().isBadRequest)

        val paivakirjamerkintaList = paivakirjamerkintaRepository.findAll()
        assertThat(paivakirjamerkintaList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun putWithMissingIdPathParamPaivakirjamerkinta() {
        initTest()
        val databaseSizeBeforeUpdate = paivakirjamerkintaRepository.findAll().size
        paivakirjamerkinta.id = count.incrementAndGet()

        val paivakirjamerkintaDTO = paivakirjamerkintaMapper.toDto(paivakirjamerkinta)

        restPaivakirjamerkintaMockMvc.perform(
            put(ENTITY_API_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(paivakirjamerkintaDTO))
        )
            .andExpect(status().isMethodNotAllowed)

        val paivakirjamerkintaList = paivakirjamerkintaRepository.findAll()
        assertThat(paivakirjamerkintaList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    @Throws(Exception::class)
    fun deletePaivakirjamerkinta() {
        initTest()
        paivakirjamerkintaRepository.saveAndFlush(paivakirjamerkinta)

        val databaseSizeBeforeDelete = paivakirjamerkintaRepository.findAll().size

        restPaivakirjamerkintaMockMvc.perform(
            delete(ENTITY_API_URL_ID, paivakirjamerkinta.id).with(csrf())
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        val paivakirjamerkintaList = paivakirjamerkintaRepository.findAll()
        assertThat(paivakirjamerkintaList).hasSize(databaseSizeBeforeDelete - 1)
    }

    @Test
    @Transactional
    fun getPaivakirjamerkintaForm() {
        initTest()
        paivakirjamerkintaRepository.saveAndFlush(paivakirjamerkinta)

        val teoriakoulutus = TeoriakoulutusHelper.createEntity(em, user)
        em.persist(teoriakoulutus)
        em.flush()

        restPaivakirjamerkintaMockMvc.perform(get("/api/erikoistuva-laakari/paivakirjamerkinta-lomake"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.aihekategoriat").value(Matchers.hasSize<Any>(6)))
            .andExpect(jsonPath("$.teoriakoulutukset").value(Matchers.hasSize<Any>(1)))
    }

    @Test
    @Transactional
    fun getPaivakirjamerkinnatRajaimet() {
        initTest()
        paivakirjamerkintaRepository.saveAndFlush(paivakirjamerkinta)

        restPaivakirjamerkintaMockMvc.perform(get("/api/erikoistuva-laakari/paivakirjamerkinnat-rajaimet"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.aihekategoriat").value(Matchers.hasSize<Any>(6)))
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

        paivakirjamerkinta = createEntity(em, user)
    }

    companion object {

        private val DEFAULT_PAIVAMAARA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_PAIVAMAARA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private const val DEFAULT_OPPIMISTAPAHTUMAN_NIMI = "AAAAAAAAAA"
        private const val UPDATED_OPPIMISTAPAHTUMAN_NIMI = "BBBBBBBBBB"

        private const val DEFAULT_MUUN_AIHEEN_NIMI = "AAAAAAAAAA"
        private const val UPDATED_MUUN_AIHEEN_NIMI = "BBBBBBBBBB"

        private const val DEFAULT_REFLEKTIO = "AAAAAAAAAA"
        private const val UPDATED_REFLEKTIO = "BBBBBBBBBB"

        private const val DEFAULT_YKSITYINEN: Boolean = false
        private const val UPDATED_YKSITYINEN: Boolean = true

        private const val ENTITY_API_URL: String = "/api/erikoistuva-laakari/paivakirjamerkinnat"
        private const val ENTITY_API_URL_ID: String = "$ENTITY_API_URL/{id}"

        private val random: Random = SecureRandom()
        private val count: AtomicLong = AtomicLong(random.nextInt().toLong() + (2L * Integer.MAX_VALUE))

        @JvmStatic
        fun createEntity(em: EntityManager, user: User? = null): Paivakirjamerkinta {
            val paivakirjamerkinta = Paivakirjamerkinta(
                paivamaara = DEFAULT_PAIVAMAARA,
                oppimistapahtumanNimi = DEFAULT_OPPIMISTAPAHTUMAN_NIMI,
                muunAiheenNimi = DEFAULT_MUUN_AIHEEN_NIMI,
                reflektio = DEFAULT_REFLEKTIO,
                yksityinen = DEFAULT_YKSITYINEN
            )

            var erikoistuvaLaakari =
                em.findAll(ErikoistuvaLaakari::class).firstOrNull { it.kayttaja?.user == user }
            if (erikoistuvaLaakari == null) {
                erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
                em.persist(erikoistuvaLaakari)
                em.flush()
            }
            paivakirjamerkinta.opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()

            return paivakirjamerkinta
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Paivakirjamerkinta {
            val paivakirjamerkinta = Paivakirjamerkinta(

                paivamaara = UPDATED_PAIVAMAARA,

                oppimistapahtumanNimi = UPDATED_OPPIMISTAPAHTUMAN_NIMI,

                muunAiheenNimi = UPDATED_MUUN_AIHEEN_NIMI,

                reflektio = UPDATED_REFLEKTIO,

                yksityinen = UPDATED_YKSITYINEN

            )

            val erikoistuvaLaakari: ErikoistuvaLaakari
            if (em.findAll(ErikoistuvaLaakari::class).isEmpty()) {
                erikoistuvaLaakari = ErikoistuvaLaakariHelper.createUpdatedEntity(em)
                em.persist(erikoistuvaLaakari)
                em.flush()
            } else {
                erikoistuvaLaakari = em.findAll(ErikoistuvaLaakari::class)[0]
            }
            paivakirjamerkinta.opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()

            return paivakirjamerkinta
        }
    }
}
