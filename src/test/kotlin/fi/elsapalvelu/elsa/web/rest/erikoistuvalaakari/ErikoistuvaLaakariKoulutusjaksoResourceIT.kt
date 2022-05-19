package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.Koulutusjakso
import fi.elsapalvelu.elsa.domain.Koulutussuunnitelma
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.KoulutusjaksoRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.service.mapper.KoulutusjaksoMapper
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.ArvioitavaKokonaisuusHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.OpintooikeusHelper
import fi.elsapalvelu.elsa.web.rest.helpers.TyoskentelyjaksoHelper
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
import javax.persistence.EntityManager
import kotlin.test.assertNotNull

@SpringBootTest(classes = [ElsaBackendApp::class])
@AutoConfigureMockMvc
class ErikoistuvaLaakariKoulutusjaksoResourceIT {
    @Autowired
    private lateinit var koulutusjaksoRepository: KoulutusjaksoRepository

    @Autowired
    private lateinit var erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository

    @Autowired
    private lateinit var koulutusjaksoMapper: KoulutusjaksoMapper

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restKoulutusjaksoMockMvc: MockMvc

    private lateinit var koulutusjakso: Koulutusjakso

    private lateinit var user: User

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @Transactional
    fun createKoulutusjakso() {
        initTest()

        val databaseSizeBeforeCreate = koulutusjaksoRepository.findAll().size

        val koulutusjaksoDTO = koulutusjaksoMapper.toDto(koulutusjakso)
        restKoulutusjaksoMockMvc.perform(
            post(ENTITY_API_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koulutusjaksoDTO))
        ).andExpect(status().isCreated)

        val koulutusjaksoList = koulutusjaksoRepository.findAll()
        assertThat(koulutusjaksoList).hasSize(databaseSizeBeforeCreate + 1)
        val testKoulutusjakso = koulutusjaksoList[koulutusjaksoList.size - 1]

        assertThat(testKoulutusjakso.nimi).isEqualTo(DEFAULT_NIMI)
        assertThat(testKoulutusjakso.muutOsaamistavoitteet).isEqualTo(DEFAULT_MUUT_OSAAMISTAVOITTEET)
        assertThat(testKoulutusjakso.luotu).isEqualTo(DEFAULT_LUOTU)
        assertThat(testKoulutusjakso.tallennettu).isEqualTo(DEFAULT_TALLENNETTU)
        assertThat(testKoulutusjakso.lukittu).isEqualTo(DEFAULT_LUKITTU)
    }

    @Test
    @Transactional
    fun createKoulutusjaksoWithoutKoulutussuunnitelma() {
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

        koulutusjakso = Koulutusjakso(
            nimi = DEFAULT_NIMI,
            muutOsaamistavoitteet = DEFAULT_MUUT_OSAAMISTAVOITTEET,
            luotu = DEFAULT_LUOTU,
            tallennettu = DEFAULT_TALLENNETTU,
            lukittu = DEFAULT_LUKITTU
        )

        val databaseSizeBeforeCreate = koulutusjaksoRepository.findAll().size

        val koulutusjaksoDTO = koulutusjaksoMapper.toDto(koulutusjakso)
        restKoulutusjaksoMockMvc.perform(
            post(ENTITY_API_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koulutusjaksoDTO))
        ).andExpect(status().is5xxServerError)

        val koulutusjaksoList = koulutusjaksoRepository.findAll()
        assertThat(koulutusjaksoList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createKoulutusjaksoWithExistingId() {
        initTest()

        koulutusjakso.id = 1L
        val koulutusjaksoDTO = koulutusjaksoMapper.toDto(koulutusjakso)

        val databaseSizeBeforeCreate = koulutusjaksoRepository.findAll().size

        restKoulutusjaksoMockMvc.perform(
            post(ENTITY_API_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koulutusjaksoDTO))
        ).andExpect(status().isBadRequest)

        val koulutusjaksoList = koulutusjaksoRepository.findAll()
        assertThat(koulutusjaksoList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun checkNimiIsRequired() {
        initTest()

        val databaseSizeBeforeTest = koulutusjaksoRepository.findAll().size
        koulutusjakso.nimi = null

        val koulutusjaksoDTO = koulutusjaksoMapper.toDto(koulutusjakso)

        restKoulutusjaksoMockMvc.perform(
            post(ENTITY_API_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koulutusjaksoDTO))
        ).andExpect(status().isBadRequest)

        val koulutusjaksoList = koulutusjaksoRepository.findAll()
        assertThat(koulutusjaksoList).hasSize(databaseSizeBeforeTest)
    }

    @Test
    @Transactional
    fun getAllKoulutusjaksot() {
        initTest()

        koulutusjaksoRepository.saveAndFlush(koulutusjakso)

        restKoulutusjaksoMockMvc.perform(get(ENTITY_API_URL + "?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(koulutusjakso.id?.toInt())))
            .andExpect(jsonPath("$.[*].nimi").value(hasItem(DEFAULT_NIMI)))
            .andExpect(jsonPath("$.[*].muutOsaamistavoitteet").value(hasItem(DEFAULT_MUUT_OSAAMISTAVOITTEET)))
            .andExpect(jsonPath("$.[*].luotu").value(hasItem(DEFAULT_LUOTU.toString())))
            .andExpect(jsonPath("$.[*].tallennettu").value(hasItem(DEFAULT_TALLENNETTU.toString())))
            .andExpect(jsonPath("$.[*].lukittu").value(hasItem(DEFAULT_LUKITTU)))
    }

    @Test
    @Transactional
    fun getAllKoulutusjaksotShouldReturnOnlyForOpintooikeusKaytossa() {
        initTest()

        koulutusjaksoRepository.saveAndFlush(koulutusjakso)

        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(user.id!!)
        requireNotNull(erikoistuvaLaakari)
        val newOpintooikeus = OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(em, erikoistuvaLaakari)
        OpintooikeusHelper.setOpintooikeusKaytossa(erikoistuvaLaakari, newOpintooikeus)

        val koulutussuunnitelma = KoulutussuunnitelmaHelper.createEntity(em, user)
        em.persist(koulutussuunnitelma)
        val koulutusjaksoForAnotherOpintooikeus = createEntity(em, user, koulutussuunnitelma)
        em.persist(koulutusjaksoForAnotherOpintooikeus)

        restKoulutusjaksoMockMvc.perform(get("$ENTITY_API_URL?sort=id,desc"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$[0].id").value(koulutusjaksoForAnotherOpintooikeus.id))
    }

    @Test
    @Transactional
    fun getKoulutusjakso() {
        initTest()

        koulutusjaksoRepository.saveAndFlush(koulutusjakso)

        val id = koulutusjakso.id
        assertNotNull(id)

        restKoulutusjaksoMockMvc.perform(get(ENTITY_API_URL_ID, koulutusjakso.id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(koulutusjakso.id?.toInt()))
            .andExpect(jsonPath("$.nimi").value(DEFAULT_NIMI))
            .andExpect(jsonPath("$.muutOsaamistavoitteet").value(DEFAULT_MUUT_OSAAMISTAVOITTEET))
            .andExpect(jsonPath("$.luotu").value(DEFAULT_LUOTU.toString()))
            .andExpect(jsonPath("$.tallennettu").value(DEFAULT_TALLENNETTU.toString()))
            .andExpect(jsonPath("$.lukittu").value(DEFAULT_LUKITTU))
    }

    @Test
    @Transactional
    fun getNonExistingKoulutusjakso() {
        initTest()

        restKoulutusjaksoMockMvc.perform(get(ENTITY_API_URL_ID, Long.MAX_VALUE))
            .andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun updateKoulutusjakso() {
        initTest()

        koulutusjaksoRepository.saveAndFlush(koulutusjakso)

        val databaseSizeBeforeUpdate = koulutusjaksoRepository.findAll().size

        val updatedKoulutusjakso = koulutusjaksoRepository.findById(koulutusjakso.id!!).get()
        em.detach(updatedKoulutusjakso)
        updatedKoulutusjakso.nimi = UPDATED_NIMI
        updatedKoulutusjakso.muutOsaamistavoitteet = UPDATED_MUUT_OSAAMISTAVOITTEET
        updatedKoulutusjakso.luotu = UPDATED_LUOTU
        updatedKoulutusjakso.tallennettu = UPDATED_TALLENNETTU
        updatedKoulutusjakso.lukittu = UPDATED_LUKITTU
        val koulutusjaksoDTO = koulutusjaksoMapper.toDto(updatedKoulutusjakso)

        restKoulutusjaksoMockMvc.perform(
            put(ENTITY_API_URL_ID, koulutusjaksoDTO.id).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koulutusjaksoDTO))
        ).andExpect(status().isOk)

        val koulutusjaksoList = koulutusjaksoRepository.findAll()
        assertThat(koulutusjaksoList).hasSize(databaseSizeBeforeUpdate)
        val testKoulutusjakso = koulutusjaksoList[koulutusjaksoList.size - 1]
        assertThat(testKoulutusjakso.nimi).isEqualTo(UPDATED_NIMI)
        assertThat(testKoulutusjakso.muutOsaamistavoitteet).isEqualTo(UPDATED_MUUT_OSAAMISTAVOITTEET)
        assertThat(testKoulutusjakso.luotu).isEqualTo(UPDATED_LUOTU)
        assertThat(testKoulutusjakso.tallennettu).isEqualTo(UPDATED_TALLENNETTU)
        assertThat(testKoulutusjakso.lukittu).isEqualTo(UPDATED_LUKITTU)
    }

    @Test
    @Transactional
    fun updateLockedKoulutusjakso() {
        initTest()

        koulutusjakso.lukittu = true
        koulutusjaksoRepository.saveAndFlush(koulutusjakso)

        val databaseSizeBeforeUpdate = koulutusjaksoRepository.findAll().size

        val updatedKoulutusjakso = koulutusjaksoRepository.findById(koulutusjakso.id!!).get()
        em.detach(updatedKoulutusjakso)
        updatedKoulutusjakso.nimi = UPDATED_NIMI
        updatedKoulutusjakso.muutOsaamistavoitteet = UPDATED_MUUT_OSAAMISTAVOITTEET
        updatedKoulutusjakso.luotu = UPDATED_LUOTU
        updatedKoulutusjakso.tallennettu = UPDATED_TALLENNETTU
        val koulutusjaksoDTO = koulutusjaksoMapper.toDto(updatedKoulutusjakso)

        restKoulutusjaksoMockMvc.perform(
            put(ENTITY_API_URL_ID, koulutusjaksoDTO.id).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koulutusjaksoDTO))
        ).andExpect(status().isBadRequest)

        val koulutusjaksoList = koulutusjaksoRepository.findAll()
        assertThat(koulutusjaksoList).hasSize(databaseSizeBeforeUpdate)
        val testKoulutusjakso = koulutusjaksoList[koulutusjaksoList.size - 1]
        assertThat(testKoulutusjakso.nimi).isEqualTo(DEFAULT_NIMI)
        assertThat(testKoulutusjakso.muutOsaamistavoitteet).isEqualTo(DEFAULT_MUUT_OSAAMISTAVOITTEET)
        assertThat(testKoulutusjakso.luotu).isEqualTo(DEFAULT_LUOTU)
        assertThat(testKoulutusjakso.tallennettu).isEqualTo(DEFAULT_TALLENNETTU)
    }

    @Test
    @Transactional
    fun updateNonExistingKoulutusjakso() {
        initTest()

        val databaseSizeBeforeUpdate = koulutusjaksoRepository.findAll().size
        koulutusjakso.id = count.incrementAndGet()

        val koulutusjaksoDTO = koulutusjaksoMapper.toDto(koulutusjakso)

        restKoulutusjaksoMockMvc.perform(
            put(ENTITY_API_URL_ID, koulutusjaksoDTO.id).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koulutusjaksoDTO))
        )
            .andExpect(status().isBadRequest)

        val koulutusjaksoList = koulutusjaksoRepository.findAll()
        assertThat(koulutusjaksoList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun updateKoulutusjaksoWithIdMismatch() {
        initTest()

        val databaseSizeBeforeUpdate = koulutusjaksoRepository.findAll().size
        koulutusjakso.id = count.incrementAndGet()

        val koulutusjaksoDTO = koulutusjaksoMapper.toDto(koulutusjakso)

        restKoulutusjaksoMockMvc.perform(
            put(ENTITY_API_URL_ID, count.incrementAndGet()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koulutusjaksoDTO))
        ).andExpect(status().isBadRequest)

        val koulutusjaksoList = koulutusjaksoRepository.findAll()
        assertThat(koulutusjaksoList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun updateKoulutusjaksoWithMissingId() {
        initTest()

        em.detach(koulutusjakso)
        val koulutusjaksoDTO = koulutusjaksoMapper.toDto(koulutusjakso)
        koulutusjaksoDTO.id = null

        restKoulutusjaksoMockMvc.perform(
            put(ENTITY_API_URL_ID, count.incrementAndGet()).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koulutusjaksoDTO))
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun updateKoulutusjaksoWithMissingIdPathParam() {
        initTest()

        val databaseSizeBeforeUpdate = koulutusjaksoRepository.findAll().size
        koulutusjakso.id = count.incrementAndGet()

        val koulutusjaksoDTO = koulutusjaksoMapper.toDto(koulutusjakso)

        restKoulutusjaksoMockMvc.perform(
            put(ENTITY_API_URL).with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koulutusjaksoDTO))
        )
            .andExpect(status().isMethodNotAllowed)

        val koulutusjaksoList = koulutusjaksoRepository.findAll()
        assertThat(koulutusjaksoList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun deleteKoulutusjakso() {
        initTest()

        koulutusjaksoRepository.saveAndFlush(koulutusjakso)

        val databaseSizeBeforeDelete = koulutusjaksoRepository.findAll().size

        restKoulutusjaksoMockMvc.perform(
            delete(ENTITY_API_URL_ID, koulutusjakso.id).with(csrf())
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isNoContent)

        val koulutusjaksoList = koulutusjaksoRepository.findAll()
        assertThat(koulutusjaksoList).hasSize(databaseSizeBeforeDelete - 1)
    }

    @Test
    @Transactional
    fun deleteLockedKoulutusjakso() {
        initTest()

        koulutusjakso.lukittu = true
        koulutusjaksoRepository.saveAndFlush(koulutusjakso)

        val databaseSizeBeforeDelete = koulutusjaksoRepository.findAll().size

        restKoulutusjaksoMockMvc.perform(
            delete(ENTITY_API_URL_ID, koulutusjakso.id).with(csrf())
                .accept(MediaType.APPLICATION_JSON)
        ).andExpect(status().isBadRequest)

        val koulutusjaksoList = koulutusjaksoRepository.findAll()
        assertThat(koulutusjaksoList).hasSize(databaseSizeBeforeDelete)
    }

    @Test
    @Transactional
    fun getKoulutusjaksoForm() {
        initTest()

        koulutusjaksoRepository.saveAndFlush(koulutusjakso)

        val id = koulutusjakso.id
        assertNotNull(id)

        val tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(em, user)
        em.persist(tyoskentelyjakso)
        em.flush()

        val arvioitavaKokonaisuus = ArvioitavaKokonaisuusHelper.createEntity(em)
        em.persist(arvioitavaKokonaisuus)
        em.flush()

        restKoulutusjaksoMockMvc.perform(get("/api/erikoistuva-laakari/koulutussuunnitelma/koulutusjakso-lomake"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.tyoskentelyjaksot").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$.kunnat").value(Matchers.hasSize<Any>(478)))
            .andExpect(jsonPath("$.arvioitavanKokonaisuudenKategoriat").value(Matchers.hasSize<Any>(1)))
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

        koulutusjakso = createEntity(em, user)
    }

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private const val DEFAULT_MUUT_OSAAMISTAVOITTEET = "AAAAAAAAAA"
        private const val UPDATED_MUUT_OSAAMISTAVOITTEET = "BBBBBBBBBB"

        private val DEFAULT_LUOTU: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_LUOTU: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_TALLENNETTU: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_TALLENNETTU: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private const val DEFAULT_LUKITTU: Boolean = false
        private const val UPDATED_LUKITTU: Boolean = true

        private val ENTITY_API_URL: String = "/api/erikoistuva-laakari/koulutussuunnitelma/koulutusjaksot"
        private val ENTITY_API_URL_ID: String = ENTITY_API_URL + "/{id}"

        private val random: Random = SecureRandom()
        private val count: AtomicLong = AtomicLong(random.nextInt().toLong() + (2L * Integer.MAX_VALUE))

        @JvmStatic
        fun createEntity(
            em: EntityManager,
            user: User? = null,
            existingKoulutussuunnitelma: Koulutussuunnitelma? = null
        ): Koulutusjakso {
            val koulutusjakso = Koulutusjakso(
                nimi = DEFAULT_NIMI,
                muutOsaamistavoitteet = DEFAULT_MUUT_OSAAMISTAVOITTEET,
                luotu = DEFAULT_LUOTU,
                tallennettu = DEFAULT_TALLENNETTU,
                lukittu = DEFAULT_LUKITTU
            )

            // Lisätään pakollinen tieto
            if (existingKoulutussuunnitelma != null) {
                koulutusjakso.koulutussuunnitelma = existingKoulutussuunnitelma
            } else {
                val koulutussuunnitelma: Koulutussuunnitelma
                if (em.findAll(Koulutussuunnitelma::class).isEmpty()) {
                    koulutussuunnitelma = KoulutussuunnitelmaHelper.createEntity(em, user)
                    em.persist(koulutussuunnitelma)
                    em.flush()
                } else {
                    koulutussuunnitelma = em.findAll(Koulutussuunnitelma::class).get(0)
                }
                koulutusjakso.koulutussuunnitelma = koulutussuunnitelma
            }

            return koulutusjakso
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Koulutusjakso {
            val koulutusjakso = Koulutusjakso(
                nimi = UPDATED_NIMI,
                muutOsaamistavoitteet = UPDATED_MUUT_OSAAMISTAVOITTEET,
                luotu = UPDATED_LUOTU,
                tallennettu = UPDATED_TALLENNETTU,
                lukittu = UPDATED_LUKITTU
            )
            // Lisätään pakollinen tieto
            val koulutussuunnitelma: Koulutussuunnitelma
            if (em.findAll(Koulutussuunnitelma::class).isEmpty()) {
                koulutussuunnitelma = KoulutussuunnitelmaHelper.createUpdatedEntity(em)
                em.persist(koulutussuunnitelma)
                em.flush()
            } else {
                koulutussuunnitelma = em.findAll(Koulutussuunnitelma::class).get(0)
            }
            koulutusjakso.koulutussuunnitelma = koulutussuunnitelma

            return koulutusjakso
        }
    }
}
