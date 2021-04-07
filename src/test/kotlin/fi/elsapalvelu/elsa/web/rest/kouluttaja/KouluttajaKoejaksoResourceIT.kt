package fi.elsapalvelu.elsa.web.rest.kouluttaja

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.repository.KoejaksonAloituskeskusteluRepository
import fi.elsapalvelu.elsa.repository.KoejaksonKoulutussopimusRepository
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.service.mapper.KoejaksonAloituskeskusteluMapper
import fi.elsapalvelu.elsa.service.mapper.KoejaksonKoulutussopimusMapper
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager
import kotlin.test.assertNotNull

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
class KouluttajaKoejaksoResourceIT {

    @Autowired
    private lateinit var koejaksonKoulutussopimusRepository: KoejaksonKoulutussopimusRepository

    @Autowired
    private lateinit var koejaksonAloituskeskusteluRepository: KoejaksonAloituskeskusteluRepository

    @Autowired
    private lateinit var koejaksonKoulutussopimusMapper: KoejaksonKoulutussopimusMapper

    @Autowired
    private lateinit var koejaksonAloituskeskusteluMapper: KoejaksonAloituskeskusteluMapper

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restKoejaksoMockMvc: MockMvc

    private lateinit var koejaksonKoulutussopimus: KoejaksonKoulutussopimus

    private lateinit var koejaksonAloituskeskustelu: KoejaksonAloituskeskustelu

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    @Transactional
    fun getKoulutussopimus() {
        initTest()

        val id = koejaksonKoulutussopimus.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/kouluttaja/koejakso/koulutussopimus/{id}", id
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(koejaksonKoulutussopimus.id as Any))
            .andExpect(jsonPath("$.erikoistuvanNimi").value(koejaksonKoulutussopimus.erikoistuvanNimi as Any))
            .andExpect(jsonPath("$.erikoistuvanOpiskelijatunnus").value(koejaksonKoulutussopimus.erikoistuvanOpiskelijatunnus as Any))
            .andExpect(jsonPath("$.erikoistuvanYliopisto").value(koejaksonKoulutussopimus.erikoistuvanYliopisto as Any))
            .andExpect(jsonPath("$.erikoistuvanPuhelinnumero").value(koejaksonKoulutussopimus.erikoistuvanPuhelinnumero as Any))
            .andExpect(jsonPath("$.erikoistuvanSahkoposti").value(koejaksonKoulutussopimus.erikoistuvanSahkoposti as Any))
            .andExpect(jsonPath("$.lahetetty").value(koejaksonKoulutussopimus.lahetetty as Any))
            .andExpect(jsonPath("$.vastuuhenkilo.id").value(koejaksonKoulutussopimus.vastuuhenkilo?.id as Any))
            .andExpect(jsonPath("$.korjausehdotus").isEmpty)
    }

    @Test
    @Transactional
    fun ackKoulutussopimusInProgress() {
        initTest()

        koejaksonKoulutussopimus.lahetetty = false
        koejaksonKoulutussopimusRepository.saveAndFlush(koejaksonKoulutussopimus)

        val databaseSizeBeforeUpdate = koejaksonKoulutussopimusRepository.findAll().size

        val id = koejaksonKoulutussopimus.id
        assertNotNull(id)
        val updatedKoulutussopimus = koejaksonKoulutussopimusRepository.findById(id).get()
        em.detach(updatedKoulutussopimus)

        updatedKoulutussopimus.kouluttajat.forEach {
            it.nimike = UPDATED_NIMIKE
            it.sahkoposti = UPDATED_EMAIL
            it.puhelin = UPDATED_PHONE
            it.lahiosoite = UPDATED_LAHIOSOITE
            it.toimipaikka = UPDATED_TOIMIPAIKKA
            it.postitoimipaikka = UPDATED_POSTITOIMIPAIKKA
            it.sopimusHyvaksytty = true
        }

        val koulutussopimusDTO = koejaksonKoulutussopimusMapper.toDto(updatedKoulutussopimus)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/koulutussopimus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koulutussopimusDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val koulutussopimusList = koejaksonKoulutussopimusRepository.findAll()
        assertThat(koulutussopimusList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun ackKoulutussopimus() {
        initTest()

        val databaseSizeBeforeUpdate = koejaksonKoulutussopimusRepository.findAll().size

        val id = koejaksonKoulutussopimus.id
        assertNotNull(id)
        val updatedKoulutussopimus = koejaksonKoulutussopimusRepository.findById(id).get()
        em.detach(updatedKoulutussopimus)

        updatedKoulutussopimus.kouluttajat.forEach {
            it.nimike = UPDATED_NIMIKE
            it.sahkoposti = UPDATED_EMAIL
            it.puhelin = UPDATED_PHONE
            it.lahiosoite = UPDATED_LAHIOSOITE
            it.toimipaikka = UPDATED_TOIMIPAIKKA
            it.postitoimipaikka = UPDATED_POSTITOIMIPAIKKA
            it.sopimusHyvaksytty = true
        }

        val koulutussopimusDTO = koejaksonKoulutussopimusMapper.toDto(updatedKoulutussopimus)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/koulutussopimus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koulutussopimusDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val koulutussopimusList = koejaksonKoulutussopimusRepository.findAll()
        assertThat(koulutussopimusList).hasSize(databaseSizeBeforeUpdate)
        val testKoulutussopimus = koulutussopimusList[koulutussopimusList.size - 1]
        assertThat(testKoulutussopimus.korjausehdotus).isNull()

        assertThat(testKoulutussopimus.kouluttajat).hasSize(1)
        val testKouluttaja = testKoulutussopimus.kouluttajat.iterator().next()
        assertThat(testKouluttaja.nimike).isEqualTo(UPDATED_NIMIKE)
        assertThat(testKouluttaja.sahkoposti).isEqualTo(UPDATED_EMAIL)
        assertThat(testKouluttaja.puhelin).isEqualTo(UPDATED_PHONE)
        assertThat(testKouluttaja.lahiosoite).isEqualTo(UPDATED_LAHIOSOITE)
        assertThat(testKouluttaja.toimipaikka).isEqualTo(UPDATED_TOIMIPAIKKA)
        assertThat(testKouluttaja.postitoimipaikka).isEqualTo(UPDATED_POSTITOIMIPAIKKA)
        assertThat(testKouluttaja.sopimusHyvaksytty).isEqualTo(true)
        assertThat(testKouluttaja.kuittausaika).isNotNull
    }

    @Test
    @Transactional
    fun declineKoulutussopimus() {
        initTest()

        val databaseSizeBeforeUpdate = koejaksonKoulutussopimusRepository.findAll().size

        val id = koejaksonKoulutussopimus.id
        assertNotNull(id)
        val updatedKoulutussopimus = koejaksonKoulutussopimusRepository.findById(id).get()
        em.detach(updatedKoulutussopimus)

        updatedKoulutussopimus.kouluttajat.forEach {
            it.nimike = UPDATED_NIMIKE
            it.sahkoposti = UPDATED_EMAIL
            it.puhelin = UPDATED_PHONE
            it.lahiosoite = UPDATED_LAHIOSOITE
            it.toimipaikka = UPDATED_TOIMIPAIKKA
            it.postitoimipaikka = UPDATED_POSTITOIMIPAIKKA
            it.sopimusHyvaksytty = false
        }

        updatedKoulutussopimus.korjausehdotus = UPDATED_KORJAUSEHDOTUS

        val koulutussopimusDTO = koejaksonKoulutussopimusMapper.toDto(updatedKoulutussopimus)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/koulutussopimus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koulutussopimusDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val koulutussopimusList = koejaksonKoulutussopimusRepository.findAll()
        assertThat(koulutussopimusList).hasSize(databaseSizeBeforeUpdate)
        val testKoulutussopimus = koulutussopimusList[koulutussopimusList.size - 1]
        assertThat(testKoulutussopimus.korjausehdotus).isEqualTo(UPDATED_KORJAUSEHDOTUS)
        assertThat(testKoulutussopimus.lahetetty).isEqualTo(false)

        assertThat(testKoulutussopimus.kouluttajat).hasSize(1)
        val testKouluttaja = testKoulutussopimus.kouluttajat.iterator().next()
        assertThat(testKouluttaja.nimike).isEqualTo(UPDATED_NIMIKE)
        assertThat(testKouluttaja.sahkoposti).isEqualTo(UPDATED_EMAIL)
        assertThat(testKouluttaja.puhelin).isEqualTo(UPDATED_PHONE)
        assertThat(testKouluttaja.lahiosoite).isEqualTo(UPDATED_LAHIOSOITE)
        assertThat(testKouluttaja.toimipaikka).isEqualTo(UPDATED_TOIMIPAIKKA)
        assertThat(testKouluttaja.postitoimipaikka).isEqualTo(UPDATED_POSTITOIMIPAIKKA)
        assertThat(testKouluttaja.sopimusHyvaksytty).isEqualTo(false)
        assertThat(testKouluttaja.kuittausaika).isNull()
    }

    @Test
    @Transactional
    fun ackAloituskeskusteluInProgress() {
        initTest()

        koejaksonAloituskeskustelu.lahetetty = false
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        val databaseSizeBeforeUpdate = koejaksonAloituskeskusteluRepository.findAll().size

        val id = koejaksonAloituskeskustelu.id
        assertNotNull(id)
        val updatedAloituskeskustelu = koejaksonAloituskeskusteluRepository.findById(id).get()
        em.detach(updatedAloituskeskustelu)

        updatedAloituskeskustelu.lahikouluttajaHyvaksynyt = true
        updatedAloituskeskustelu.lahikouluttajanKuittausaika = LocalDate.now()

        val aloituskeskusteluDTO = koejaksonAloituskeskusteluMapper.toDto(updatedAloituskeskustelu)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/aloituskeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(aloituskeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val aloituskeskusteluList = koejaksonAloituskeskusteluRepository.findAll()
        assertThat(aloituskeskusteluList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun ackAloituskeskusteluKouluttaja() {
        initTest()

        val databaseSizeBeforeUpdate = koejaksonAloituskeskusteluRepository.findAll().size

        val id = koejaksonAloituskeskustelu.id
        assertNotNull(id)
        val updatedAloituskeskustelu = koejaksonAloituskeskusteluRepository.findById(id).get()
        em.detach(updatedAloituskeskustelu)

        updatedAloituskeskustelu.lahikouluttajaHyvaksynyt = true
        updatedAloituskeskustelu.lahikouluttajanKuittausaika = DEFAULT_MYONTAMISPAIVA

        val aloituskeskusteluDTO = koejaksonAloituskeskusteluMapper.toDto(updatedAloituskeskustelu)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/aloituskeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(aloituskeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val aloituskeskusteluList = koejaksonAloituskeskusteluRepository.findAll()
        assertThat(aloituskeskusteluList).hasSize(databaseSizeBeforeUpdate)
        val testAloituskeskustelu = aloituskeskusteluList[aloituskeskusteluList.size - 1]
        assertThat(testAloituskeskustelu.korjausehdotus).isNull()
        assertThat(testAloituskeskustelu.lahikouluttajaHyvaksynyt).isEqualTo(true)
        assertThat(testAloituskeskustelu.lahikouluttajanKuittausaika).isNotNull
        assertThat(testAloituskeskustelu.lahiesimiesHyvaksynyt).isEqualTo(false)
        assertThat(testAloituskeskustelu.lahiesimiehenKuittausaika).isNull()
        assertThat(testAloituskeskustelu.lahetetty).isEqualTo(true)
    }

    @Test
    @Transactional
    fun declineAloituskeskusteluKouluttaja() {
        initTest()

        val databaseSizeBeforeUpdate = koejaksonAloituskeskusteluRepository.findAll().size

        val id = koejaksonAloituskeskustelu.id
        assertNotNull(id)
        val updatedAloituskeskustelu = koejaksonAloituskeskusteluRepository.findById(id).get()
        em.detach(updatedAloituskeskustelu)

        updatedAloituskeskustelu.lahikouluttajaHyvaksynyt = false
        updatedAloituskeskustelu.korjausehdotus = UPDATED_KORJAUSEHDOTUS

        val aloituskeskusteluDTO = koejaksonAloituskeskusteluMapper.toDto(updatedAloituskeskustelu)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/aloituskeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(aloituskeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val aloituskeskusteluList = koejaksonAloituskeskusteluRepository.findAll()
        assertThat(aloituskeskusteluList).hasSize(databaseSizeBeforeUpdate)
        val testAloituskeskustelu = aloituskeskusteluList[aloituskeskusteluList.size - 1]
        assertThat(testAloituskeskustelu.korjausehdotus).isEqualTo(UPDATED_KORJAUSEHDOTUS)
        assertThat(testAloituskeskustelu.lahiesimiesHyvaksynyt).isEqualTo(false)
        assertThat(testAloituskeskustelu.lahiesimiehenKuittausaika).isNull()
        assertThat(testAloituskeskustelu.lahikouluttajaHyvaksynyt).isEqualTo(false)
        assertThat(testAloituskeskustelu.lahikouluttajanKuittausaika).isNull()
        assertThat(testAloituskeskustelu.lahetetty).isEqualTo(false)
    }

    @Test
    @Transactional
    fun ackAloituskeskusteluEsimies() {
        initTest(DEFAULT_ESIMIES_ID)

        koejaksonAloituskeskustelu.lahikouluttajaHyvaksynyt = true
        koejaksonAloituskeskustelu.lahikouluttajanKuittausaika = DEFAULT_MYONTAMISPAIVA
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        val databaseSizeBeforeUpdate = koejaksonAloituskeskusteluRepository.findAll().size

        val id = koejaksonAloituskeskustelu.id
        assertNotNull(id)
        val updatedAloituskeskustelu = koejaksonAloituskeskusteluRepository.findById(id).get()
        em.detach(updatedAloituskeskustelu)

        updatedAloituskeskustelu.lahiesimiesHyvaksynyt = true
        updatedAloituskeskustelu.lahiesimiehenKuittausaika = DEFAULT_MYONTAMISPAIVA

        val aloituskeskusteluDTO = koejaksonAloituskeskusteluMapper.toDto(updatedAloituskeskustelu)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/aloituskeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(aloituskeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val aloituskeskusteluList = koejaksonAloituskeskusteluRepository.findAll()
        assertThat(aloituskeskusteluList).hasSize(databaseSizeBeforeUpdate)
        val testAloituskeskustelu = aloituskeskusteluList[aloituskeskusteluList.size - 1]
        assertThat(testAloituskeskustelu.korjausehdotus).isNull()
        assertThat(testAloituskeskustelu.lahiesimiesHyvaksynyt).isEqualTo(true)
        assertThat(testAloituskeskustelu.lahiesimiehenKuittausaika).isNotNull
        assertThat(testAloituskeskustelu.lahikouluttajaHyvaksynyt).isEqualTo(true)
        assertThat(testAloituskeskustelu.lahikouluttajanKuittausaika).isNotNull
        assertThat(testAloituskeskustelu.lahetetty).isEqualTo(true)
    }

    @Test
    @Transactional
    fun declineAloituskeskusteluEsimies() {
        initTest(DEFAULT_ESIMIES_ID)

        koejaksonAloituskeskustelu.lahikouluttajaHyvaksynyt = true
        koejaksonAloituskeskustelu.lahikouluttajanKuittausaika = DEFAULT_MYONTAMISPAIVA
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        val databaseSizeBeforeUpdate = koejaksonAloituskeskusteluRepository.findAll().size

        val id = koejaksonAloituskeskustelu.id
        assertNotNull(id)
        val updatedAloituskeskustelu = koejaksonAloituskeskusteluRepository.findById(id).get()
        em.detach(updatedAloituskeskustelu)

        updatedAloituskeskustelu.lahiesimiesHyvaksynyt = false
        updatedAloituskeskustelu.korjausehdotus = UPDATED_KORJAUSEHDOTUS

        val aloituskeskusteluDTO = koejaksonAloituskeskusteluMapper.toDto(updatedAloituskeskustelu)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/aloituskeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(aloituskeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val aloituskeskusteluList = koejaksonAloituskeskusteluRepository.findAll()
        assertThat(aloituskeskusteluList).hasSize(databaseSizeBeforeUpdate)
        val testAloituskeskustelu = aloituskeskusteluList[aloituskeskusteluList.size - 1]
        assertThat(testAloituskeskustelu.korjausehdotus).isEqualTo(UPDATED_KORJAUSEHDOTUS)
        assertThat(testAloituskeskustelu.lahiesimiesHyvaksynyt).isEqualTo(false)
        assertThat(testAloituskeskustelu.lahiesimiehenKuittausaika).isNull()
        assertThat(testAloituskeskustelu.lahikouluttajaHyvaksynyt).isEqualTo(false)
        assertThat(testAloituskeskustelu.lahikouluttajanKuittausaika).isNull()
        assertThat(testAloituskeskustelu.lahetetty).isEqualTo(false)
    }

    fun initTest(userId: String? = DEFAULT_KOULUTTAJA_ID) {
        val userDetails = mapOf<String, Any>(
            "uid" to userId!!,
            "sub" to DEFAULT_LOGIN,
            "email" to DEFAULT_EMAIL
        )
        val authorities = listOf(SimpleGrantedAuthority(KOULUTTAJA))
        val user = DefaultOAuth2User(authorities, userDetails, "sub")
        val authentication = OAuth2AuthenticationToken(user, authorities, "oidc")
        TestSecurityContextHolder.getContext().authentication = authentication
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, DEFAULT_ID)
        em.persist(erikoistuvaLaakari)

        val vastuuhenkilo = KayttajaHelper.createEntity(
            em, DEFAULT_VASTUUHENKILO_ID
        )
        em.persist(vastuuhenkilo)

        val kouluttaja = KayttajaHelper.createEntity(
            em, DEFAULT_KOULUTTAJA_ID
        )
        em.persist(kouluttaja)

        val esimies = KayttajaHelper.createEntity(
            em, DEFAULT_ESIMIES_ID
        )
        em.persist(esimies)

        koejaksonKoulutussopimus = createKoulutussopimus(erikoistuvaLaakari, vastuuhenkilo)
        koejaksonKoulutussopimus.kouluttajat =
            mutableSetOf(createKoulutussopimuksenKouluttaja(koejaksonKoulutussopimus, kouluttaja))
        koejaksonKoulutussopimus.koulutuspaikat =
            mutableSetOf(createKoulutussopimuksenKoulutuspaikka(koejaksonKoulutussopimus))
        em.persist(koejaksonKoulutussopimus)

        koejaksonAloituskeskustelu =
            createAloituskeskustelu(erikoistuvaLaakari, kouluttaja, esimies)
        em.persist(koejaksonAloituskeskustelu)
    }

    companion object {

        private const val DEFAULT_ID = "c47f46ad-21c4-47e8-9c7c-ba44f60c8bae"
        private const val DEFAULT_LOGIN = "johndoe"
        private const val DEFAULT_EMAIL = "john.doe@example.com"

        private const val UPDATED_EMAIL = "doe.john@example.com"
        private const val UPDATED_PHONE = "+358101001010"

        private val DEFAULT_SYNTYMAAIKA: LocalDate = LocalDate.ofEpochDay(0L)
        private val DEFAULT_MYONTAMISPAIVA: LocalDate = LocalDate.ofEpochDay(1L)
        private val DEFAULT_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(2L)
        private val DEFAULT_PAATTYMISPAIVA: LocalDate = LocalDate.ofEpochDay(5L)
        private val DEFAULT_MUOKKAUSPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private const val DEFAULT_KOULUTTAJA_ID = "4b73bc2c-88c4-11eb-8dcd-0242ac130003"
        private const val DEFAULT_VASTUUHENKILO_ID = "53d6e70e-88c4-11eb-8dcd-0242ac130003"
        private const val DEFAULT_ESIMIES_ID = "43c0ebfa-92f9-11eb-a8b3-0242ac130003"

        private const val DEFAULT_KOULUTUSPAIKKA = "TAYS Päivystyskeskus"
        private const val DEFAULT_YLIOPISTO = "TAYS"

        private const val UPDATED_NIMIKE = "Nimike"
        private const val UPDATED_LAHIOSOITE = "Testitie"
        private const val UPDATED_TOIMIPAIKKA = "Sairaala"
        private const val UPDATED_POSTITOIMIPAIKKA = "Tampere"
        private const val UPDATED_KORJAUSEHDOTUS = "Lorem Ipsum"

        private const val DEFAULT_OSAAMISTAVOITTEET = "Lorem ipsum"

        @JvmStatic
        fun createKoulutussopimus(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            vastuuhenkilo: Kayttaja
        ): KoejaksonKoulutussopimus {
            return KoejaksonKoulutussopimus(
                erikoistuvaLaakari = erikoistuvaLaakari,
                erikoistuvanNimi = erikoistuvaLaakari.kayttaja?.nimi,
                erikoistuvanOpiskelijatunnus = erikoistuvaLaakari.opiskelijatunnus,
                erikoistuvanSyntymaaika = DEFAULT_SYNTYMAAIKA,
                erikoistuvanYliopisto = erikoistuvaLaakari.kayttaja?.yliopisto?.nimi,
                opintooikeudenMyontamispaiva = DEFAULT_MYONTAMISPAIVA,
                koejaksonAlkamispaiva = DEFAULT_ALKAMISPAIVA,
                erikoistuvanPuhelinnumero = erikoistuvaLaakari.puhelinnumero,
                erikoistuvanSahkoposti = erikoistuvaLaakari.sahkoposti,
                lahetetty = true,
                muokkauspaiva = DEFAULT_MUOKKAUSPAIVA,
                vastuuhenkilo = vastuuhenkilo,
                vastuuhenkilonNimi = vastuuhenkilo.nimi,
                vastuuhenkilonNimike = vastuuhenkilo.nimike,
            )
        }

        @JvmStatic
        fun createKoulutussopimuksenKouluttaja(
            koejaksonKoulutussopimus: KoejaksonKoulutussopimus,
            kouluttaja: Kayttaja
        ): KoulutussopimuksenKouluttaja {
            return KoulutussopimuksenKouluttaja(
                kouluttaja = kouluttaja,
                nimi = kouluttaja.nimi,
                koulutussopimus = koejaksonKoulutussopimus
            )
        }

        @JvmStatic
        fun createKoulutussopimuksenKoulutuspaikka(
            koejaksonKoulutussopimus: KoejaksonKoulutussopimus,
        ): KoulutussopimuksenKoulutuspaikka {
            return KoulutussopimuksenKoulutuspaikka(
                nimi = DEFAULT_KOULUTUSPAIKKA,
                yliopisto = DEFAULT_YLIOPISTO,
                koulutussopimus = koejaksonKoulutussopimus
            )
        }

        @JvmStatic
        fun createAloituskeskustelu(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            lahikouluttaja: Kayttaja,
            lahiesimies: Kayttaja
        ): KoejaksonAloituskeskustelu {
            return KoejaksonAloituskeskustelu(
                erikoistuvaLaakari = erikoistuvaLaakari,
                erikoistuvanNimi = erikoistuvaLaakari.kayttaja?.nimi,
                erikoistuvanErikoisala = erikoistuvaLaakari.erikoisala?.nimi,
                erikoistuvanOpiskelijatunnus = erikoistuvaLaakari.opiskelijatunnus,
                erikoistuvanYliopisto = erikoistuvaLaakari.kayttaja?.yliopisto?.nimi,
                erikoistuvanSahkoposti = erikoistuvaLaakari.sahkoposti,
                koejaksonSuorituspaikka = DEFAULT_KOULUTUSPAIKKA,
                koejaksonAlkamispaiva = DEFAULT_ALKAMISPAIVA,
                koejaksonPaattymispaiva = DEFAULT_PAATTYMISPAIVA,
                suoritettuKokoaikatyossa = true,
                lahikouluttaja = lahikouluttaja,
                lahikouluttajanNimi = lahikouluttaja.nimi,
                lahiesimies = lahiesimies,
                lahiesimiehenNimi = lahiesimies.nimi,
                koejaksonOsaamistavoitteet = DEFAULT_OSAAMISTAVOITTEET,
                lahetetty = true,
                muokkauspaiva = DEFAULT_MUOKKAUSPAIVA
            )
        }
    }
}
