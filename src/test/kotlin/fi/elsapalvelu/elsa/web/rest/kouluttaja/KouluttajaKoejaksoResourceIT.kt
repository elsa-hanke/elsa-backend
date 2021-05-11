package fi.elsapalvelu.elsa.web.rest.kouluttaja

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.mapper.*
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
    private lateinit var koejaksonValiarviointiRepository: KoejaksonValiarviointiRepository

    @Autowired
    private lateinit var koejaksonKehittamistoimenpiteetRepository: KoejaksonKehittamistoimenpiteetRepository

    @Autowired
    private lateinit var koejaksonLoppukeskusteluRepository: KoejaksonLoppukeskusteluRepository

    @Autowired
    private lateinit var koejaksonKoulutussopimusMapper: KoejaksonKoulutussopimusMapper

    @Autowired
    private lateinit var koejaksonAloituskeskusteluMapper: KoejaksonAloituskeskusteluMapper

    @Autowired
    private lateinit var koejaksonValiarviointiMapper: KoejaksonValiarviointiMapper

    @Autowired
    private lateinit var koejaksonKehittamistoimenpiteetMapper: KoejaksonKehittamistoimenpiteetMapper

    @Autowired
    private lateinit var koejaksonLoppukeskusteluMapper: KoejaksonLoppukeskusteluMapper

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restKoejaksoMockMvc: MockMvc

    private lateinit var koejaksonKoulutussopimus: KoejaksonKoulutussopimus

    private lateinit var koejaksonAloituskeskustelu: KoejaksonAloituskeskustelu

    private lateinit var koejaksonValiarviointi: KoejaksonValiarviointi

    private lateinit var koejaksonKehittamistoimenpiteet: KoejaksonKehittamistoimenpiteet

    private lateinit var koejaksonLoppukeskustelu: KoejaksonLoppukeskustelu

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    @Transactional
    fun getKoejaksot() {
        initTest()

        val id = koejaksonKoulutussopimus.id
        assertNotNull(id)

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        koejaksonValiarviointi.erikoistuvaAllekirjoittanut = true
        koejaksonValiarviointi.kehittamistoimenpiteet = DEFAULT_KEHITTAMISTOIMENPITEET
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        koejaksonKehittamistoimenpiteet.erikoistuvaAllekirjoittanut = true
        koejaksonKehittamistoimenpiteetRepository.saveAndFlush(koejaksonKehittamistoimenpiteet)

        restKoejaksoMockMvc.perform(get("/api/kouluttaja/koejaksot"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$[0].koulutussopimus.id").value(koejaksonKoulutussopimus.id as Any))
            .andExpect(jsonPath("$[0].koulutusSopimuksenTila").value(KoejaksoTila.ODOTTAA_HYVAKSYNTAA.name as Any))
            .andExpect(jsonPath("$[0].aloituskeskustelu.id").value(koejaksonAloituskeskustelu.id as Any))
            .andExpect(jsonPath("$[0].aloituskeskustelunTila").value(KoejaksoTila.HYVAKSYTTY.name as Any))
            .andExpect(jsonPath("$[0].valiarvioinninTila").value(KoejaksoTila.HYVAKSYTTY.name as Any))
            .andExpect(jsonPath("$[0].kehittamistoimenpiteidenTila").value(KoejaksoTila.HYVAKSYTTY.name as Any))
            .andExpect(jsonPath("$[0].loppukeskustelunTila").value(KoejaksoTila.ODOTTAA_HYVAKSYNTAA.name as Any))
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
    fun getAloituskeskustelu() {
        initTest()

        val id = koejaksonAloituskeskustelu.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/kouluttaja/koejakso/aloituskeskustelu/{id}", id
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(koejaksonAloituskeskustelu.id as Any))
            .andExpect(jsonPath("$.erikoistuvanNimi").value(koejaksonAloituskeskustelu.erikoistuvanNimi as Any))
            .andExpect(jsonPath("$.erikoistuvanOpiskelijatunnus").value(koejaksonAloituskeskustelu.erikoistuvanOpiskelijatunnus as Any))
            .andExpect(jsonPath("$.erikoistuvanYliopisto").value(koejaksonAloituskeskustelu.erikoistuvanYliopisto as Any))
            .andExpect(jsonPath("$.erikoistuvanSahkoposti").value(koejaksonAloituskeskustelu.erikoistuvanSahkoposti as Any))
            .andExpect(jsonPath("$.lahetetty").value(koejaksonAloituskeskustelu.lahetetty as Any))
            .andExpect(jsonPath("$.lahikouluttaja.id").value(koejaksonAloituskeskustelu.lahikouluttaja?.id as Any))
            .andExpect(jsonPath("$.lahiesimies.id").value(koejaksonAloituskeskustelu.lahiesimies?.id as Any))
            .andExpect(jsonPath("$.korjausehdotus").isEmpty)
    }

    @Test
    @Transactional
    fun getValiarviointi() {
        initTest()

        val id = koejaksonValiarviointi.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/kouluttaja/koejakso/valiarviointi/{id}", id
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(koejaksonValiarviointi.id as Any))
            .andExpect(jsonPath("$.erikoistuvanNimi").value(koejaksonValiarviointi.erikoistuvanNimi as Any))
            .andExpect(jsonPath("$.erikoistuvanOpiskelijatunnus").value(koejaksonValiarviointi.erikoistuvanOpiskelijatunnus as Any))
            .andExpect(jsonPath("$.erikoistuvanYliopisto").value(koejaksonValiarviointi.erikoistuvanYliopisto as Any))
            .andExpect(jsonPath("$.lahikouluttaja.id").value(koejaksonValiarviointi.lahikouluttaja?.id as Any))
            .andExpect(jsonPath("$.lahiesimies.id").value(koejaksonValiarviointi.lahiesimies?.id as Any))
            .andExpect(jsonPath("$.korjausehdotus").isEmpty)
    }

    @Test
    @Transactional
    fun getKehittamistoimenpiteet() {
        initTest()

        val id = koejaksonKehittamistoimenpiteet.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/kouluttaja/koejakso/kehittamistoimenpiteet/{id}", id
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(koejaksonKehittamistoimenpiteet.id as Any))
            .andExpect(jsonPath("$.erikoistuvanNimi").value(koejaksonKehittamistoimenpiteet.erikoistuvanNimi as Any))
            .andExpect(
                jsonPath("$.erikoistuvanOpiskelijatunnus").value(
                    koejaksonKehittamistoimenpiteet.erikoistuvanOpiskelijatunnus as Any
                )
            )
            .andExpect(jsonPath("$.erikoistuvanYliopisto").value(koejaksonKehittamistoimenpiteet.erikoistuvanYliopisto as Any))
            .andExpect(jsonPath("$.lahikouluttaja.id").value(koejaksonKehittamistoimenpiteet.lahikouluttaja?.id as Any))
            .andExpect(jsonPath("$.lahiesimies.id").value(koejaksonKehittamistoimenpiteet.lahiesimies?.id as Any))
            .andExpect(jsonPath("$.korjausehdotus").isEmpty)
    }

    @Test
    @Transactional
    fun getLoppukeskustelu() {
        initTest()

        val id = koejaksonLoppukeskustelu.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/kouluttaja/koejakso/loppukeskustelu/{id}", id
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(koejaksonLoppukeskustelu.id as Any))
            .andExpect(jsonPath("$.erikoistuvanNimi").value(koejaksonLoppukeskustelu.erikoistuvanNimi as Any))
            .andExpect(jsonPath("$.erikoistuvanOpiskelijatunnus").value(koejaksonLoppukeskustelu.erikoistuvanOpiskelijatunnus as Any))
            .andExpect(jsonPath("$.erikoistuvanYliopisto").value(koejaksonLoppukeskustelu.erikoistuvanYliopisto as Any))
            .andExpect(jsonPath("$.lahikouluttaja.id").value(koejaksonLoppukeskustelu.lahikouluttaja?.id as Any))
            .andExpect(jsonPath("$.lahiesimies.id").value(koejaksonLoppukeskustelu.lahiesimies?.id as Any))
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

    @Test
    @Transactional
    fun updateValiarviointiKouluttaja() {
        initTest()

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        val databaseSizeBeforeUpdate = koejaksonValiarviointiRepository.findAll().size

        val id = koejaksonValiarviointi.id
        assertNotNull(id)
        val updatedValiarviointi = koejaksonValiarviointiRepository.findById(id).get()
        em.detach(updatedValiarviointi)

        updatedValiarviointi.kehittamistoimenpiteet = DEFAULT_KEHITTAMISTOIMENPITEET
        updatedValiarviointi.edistyminenTavoitteidenMukaista = false
        updatedValiarviointi.vahvuudet = DEFAULT_VAHVUUDET
        updatedValiarviointi.lahikouluttajaHyvaksynyt = true
        updatedValiarviointi.lahikouluttajanKuittausaika = DEFAULT_MYONTAMISPAIVA

        val valiarvointiDTO = koejaksonValiarviointiMapper.toDto(updatedValiarviointi)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/valiarviointi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(valiarvointiDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val valiarviointiList = koejaksonValiarviointiRepository.findAll()
        assertThat(valiarviointiList).hasSize(databaseSizeBeforeUpdate)
        val testValiarviointi = valiarviointiList[valiarviointiList.size - 1]
        assertThat(testValiarviointi.korjausehdotus).isNull()
        assertThat(testValiarviointi.lahikouluttajaHyvaksynyt).isEqualTo(true)
        assertThat(testValiarviointi.lahikouluttajanKuittausaika).isNotNull
        assertThat(testValiarviointi.lahiesimiesHyvaksynyt).isEqualTo(false)
        assertThat(testValiarviointi.lahiesimiehenKuittausaika).isNull()
        assertThat(testValiarviointi.edistyminenTavoitteidenMukaista).isEqualTo(false)
        assertThat(testValiarviointi.kehittamistoimenpiteet).isEqualTo(
            DEFAULT_KEHITTAMISTOIMENPITEET
        )
        assertThat(testValiarviointi.vahvuudet).isEqualTo(DEFAULT_VAHVUUDET)
    }

    @Test
    @Transactional
    fun ackValiarviointiEsimies() {
        initTest(DEFAULT_ESIMIES_ID)

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        koejaksonValiarviointi.lahikouluttajaHyvaksynyt = true
        koejaksonValiarviointi.lahikouluttajanKuittausaika = DEFAULT_MYONTAMISPAIVA
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        val databaseSizeBeforeUpdate = koejaksonValiarviointiRepository.findAll().size

        val id = koejaksonValiarviointi.id
        assertNotNull(id)
        val updatedValiarviointi = koejaksonValiarviointiRepository.findById(id).get()
        em.detach(updatedValiarviointi)

        updatedValiarviointi.lahiesimiesHyvaksynyt = true
        updatedValiarviointi.lahiesimiehenKuittausaika = DEFAULT_MYONTAMISPAIVA

        val valiarviointiDTO = koejaksonValiarviointiMapper.toDto(updatedValiarviointi)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/valiarviointi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(valiarviointiDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val valiarviointiList = koejaksonValiarviointiRepository.findAll()
        assertThat(valiarviointiList).hasSize(databaseSizeBeforeUpdate)
        val testValiarviointi = valiarviointiList[valiarviointiList.size - 1]
        assertThat(testValiarviointi.korjausehdotus).isNull()
        assertThat(testValiarviointi.lahiesimiesHyvaksynyt).isEqualTo(true)
        assertThat(testValiarviointi.lahiesimiehenKuittausaika).isNotNull
        assertThat(testValiarviointi.lahikouluttajaHyvaksynyt).isEqualTo(true)
        assertThat(testValiarviointi.lahikouluttajanKuittausaika).isNotNull
    }

    @Test
    @Transactional
    fun declineValiarviointiEsimies() {
        initTest(DEFAULT_ESIMIES_ID)

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        koejaksonValiarviointi.lahikouluttajaHyvaksynyt = true
        koejaksonValiarviointi.lahikouluttajanKuittausaika = DEFAULT_MYONTAMISPAIVA
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        val databaseSizeBeforeUpdate = koejaksonValiarviointiRepository.findAll().size

        val id = koejaksonValiarviointi.id
        assertNotNull(id)
        val updatedValiarviointi = koejaksonValiarviointiRepository.findById(id).get()
        em.detach(updatedValiarviointi)

        updatedValiarviointi.lahiesimiesHyvaksynyt = false
        updatedValiarviointi.korjausehdotus = UPDATED_KORJAUSEHDOTUS

        val valiarviointiDTO = koejaksonValiarviointiMapper.toDto(updatedValiarviointi)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/valiarviointi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(valiarviointiDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val valiarviointiList = koejaksonValiarviointiRepository.findAll()
        assertThat(valiarviointiList).hasSize(databaseSizeBeforeUpdate)
        val testValiarviointi = valiarviointiList[valiarviointiList.size - 1]
        assertThat(testValiarviointi.korjausehdotus).isEqualTo(UPDATED_KORJAUSEHDOTUS)
        assertThat(testValiarviointi.lahiesimiesHyvaksynyt).isEqualTo(false)
        assertThat(testValiarviointi.lahiesimiehenKuittausaika).isNull()
        assertThat(testValiarviointi.lahikouluttajaHyvaksynyt).isEqualTo(false)
        assertThat(testValiarviointi.lahikouluttajanKuittausaika).isNull()
    }

    @Test
    @Transactional
    fun updateKehittamistoimenpiteetKouluttaja() {
        initTest()

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        koejaksonValiarviointi.erikoistuvaAllekirjoittanut = true
        koejaksonValiarviointi.muokkauspaiva = LocalDate.now()
        koejaksonValiarviointi.kehittamistoimenpiteet = DEFAULT_KEHITTAMISTOIMENPITEET
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        val databaseSizeBeforeUpdate = koejaksonKehittamistoimenpiteetRepository.findAll().size

        val id = koejaksonKehittamistoimenpiteet.id
        assertNotNull(id)
        val updatedKehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetRepository.findById(id).get()
        em.detach(updatedKehittamistoimenpiteet)

        updatedKehittamistoimenpiteet.kehittamistoimenpiteetRiittavat = true
        updatedKehittamistoimenpiteet.lahikouluttajaHyvaksynyt = true
        updatedKehittamistoimenpiteet.lahikouluttajanKuittausaika = DEFAULT_MYONTAMISPAIVA

        val kehittamistoimenpiteetDTO =
            koejaksonKehittamistoimenpiteetMapper.toDto(updatedKehittamistoimenpiteet)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/kehittamistoimenpiteet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kehittamistoimenpiteetDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val kehittamistoimenpiteetList = koejaksonKehittamistoimenpiteetRepository.findAll()
        assertThat(kehittamistoimenpiteetList).hasSize(databaseSizeBeforeUpdate)
        val testKehittamistoimenpiteet =
            kehittamistoimenpiteetList[kehittamistoimenpiteetList.size - 1]
        assertThat(testKehittamistoimenpiteet.korjausehdotus).isNull()
        assertThat(testKehittamistoimenpiteet.lahikouluttajaHyvaksynyt).isEqualTo(true)
        assertThat(testKehittamistoimenpiteet.lahikouluttajanKuittausaika).isNotNull
        assertThat(testKehittamistoimenpiteet.lahiesimiesHyvaksynyt).isEqualTo(false)
        assertThat(testKehittamistoimenpiteet.lahiesimiehenKuittausaika).isNull()
        assertThat(testKehittamistoimenpiteet.kehittamistoimenpiteetRiittavat).isEqualTo(true)
    }

    @Test
    @Transactional
    fun ackKehittamistoimenpiteetEsimies() {
        initTest(DEFAULT_ESIMIES_ID)

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        koejaksonValiarviointi.erikoistuvaAllekirjoittanut = true
        koejaksonValiarviointi.muokkauspaiva = LocalDate.now()
        koejaksonValiarviointi.kehittamistoimenpiteet = DEFAULT_KEHITTAMISTOIMENPITEET
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        koejaksonKehittamistoimenpiteet.lahikouluttajaHyvaksynyt = true
        koejaksonKehittamistoimenpiteet.lahikouluttajanKuittausaika = DEFAULT_MYONTAMISPAIVA
        koejaksonKehittamistoimenpiteetRepository.saveAndFlush(koejaksonKehittamistoimenpiteet)

        val databaseSizeBeforeUpdate = koejaksonKehittamistoimenpiteetRepository.findAll().size

        val id = koejaksonKehittamistoimenpiteet.id
        assertNotNull(id)
        val updatedKehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetRepository.findById(id).get()
        em.detach(updatedKehittamistoimenpiteet)

        updatedKehittamistoimenpiteet.lahiesimiesHyvaksynyt = true
        updatedKehittamistoimenpiteet.lahiesimiehenKuittausaika = DEFAULT_MYONTAMISPAIVA

        val kehittamistoimenpiteetDTO =
            koejaksonKehittamistoimenpiteetMapper.toDto(updatedKehittamistoimenpiteet)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/kehittamistoimenpiteet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kehittamistoimenpiteetDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val kehittamistoimenpiteetList = koejaksonKehittamistoimenpiteetRepository.findAll()
        assertThat(kehittamistoimenpiteetList).hasSize(databaseSizeBeforeUpdate)
        val testKehittamistoimenpiteet =
            kehittamistoimenpiteetList[kehittamistoimenpiteetList.size - 1]
        assertThat(testKehittamistoimenpiteet.korjausehdotus).isNull()
        assertThat(testKehittamistoimenpiteet.lahiesimiesHyvaksynyt).isEqualTo(true)
        assertThat(testKehittamistoimenpiteet.lahiesimiehenKuittausaika).isNotNull
        assertThat(testKehittamistoimenpiteet.lahikouluttajaHyvaksynyt).isEqualTo(true)
        assertThat(testKehittamistoimenpiteet.lahikouluttajanKuittausaika).isNotNull
    }

    @Test
    @Transactional
    fun declineKehittamistoimenpiteetEsimies() {
        initTest(DEFAULT_ESIMIES_ID)

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        koejaksonValiarviointi.erikoistuvaAllekirjoittanut = true
        koejaksonValiarviointi.muokkauspaiva = LocalDate.now()
        koejaksonValiarviointi.kehittamistoimenpiteet = DEFAULT_KEHITTAMISTOIMENPITEET
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        koejaksonKehittamistoimenpiteet.lahikouluttajaHyvaksynyt = true
        koejaksonKehittamistoimenpiteet.lahikouluttajanKuittausaika = DEFAULT_MYONTAMISPAIVA
        koejaksonKehittamistoimenpiteetRepository.saveAndFlush(koejaksonKehittamistoimenpiteet)

        val databaseSizeBeforeUpdate = koejaksonKehittamistoimenpiteetRepository.findAll().size

        val id = koejaksonKehittamistoimenpiteet.id
        assertNotNull(id)
        val updatedKehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetRepository.findById(id).get()
        em.detach(updatedKehittamistoimenpiteet)

        updatedKehittamistoimenpiteet.lahiesimiesHyvaksynyt = false
        updatedKehittamistoimenpiteet.korjausehdotus = UPDATED_KORJAUSEHDOTUS

        val kehittamistoimenpiteetDTO =
            koejaksonKehittamistoimenpiteetMapper.toDto(updatedKehittamistoimenpiteet)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/kehittamistoimenpiteet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kehittamistoimenpiteetDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val kehittamistoimenpiteetList = koejaksonKehittamistoimenpiteetRepository.findAll()
        assertThat(kehittamistoimenpiteetList).hasSize(databaseSizeBeforeUpdate)
        val testKehittamistoimenpiteet =
            kehittamistoimenpiteetList[kehittamistoimenpiteetList.size - 1]
        assertThat(testKehittamistoimenpiteet.korjausehdotus).isEqualTo(UPDATED_KORJAUSEHDOTUS)
        assertThat(testKehittamistoimenpiteet.lahiesimiesHyvaksynyt).isEqualTo(false)
        assertThat(testKehittamistoimenpiteet.lahiesimiehenKuittausaika).isNull()
        assertThat(testKehittamistoimenpiteet.lahikouluttajaHyvaksynyt).isEqualTo(false)
        assertThat(testKehittamistoimenpiteet.lahikouluttajanKuittausaika).isNull()
    }

    @Test
    @Transactional
    fun updateLoppukeskusteluKouluttaja() {
        initTest()

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        koejaksonValiarviointi.erikoistuvaAllekirjoittanut = true
        koejaksonValiarviointi.muokkauspaiva = LocalDate.now()
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        val databaseSizeBeforeUpdate = koejaksonLoppukeskusteluRepository.findAll().size

        val id = koejaksonLoppukeskustelu.id
        assertNotNull(id)
        val updatedLoppukeskustelu = koejaksonLoppukeskusteluRepository.findById(id).get()
        em.detach(updatedLoppukeskustelu)

        updatedLoppukeskustelu.esitetaanKoejaksonHyvaksymista = true
        updatedLoppukeskustelu.lahikouluttajaHyvaksynyt = true
        updatedLoppukeskustelu.lahikouluttajanKuittausaika = DEFAULT_MYONTAMISPAIVA

        val loppukeskusteluDTO = koejaksonLoppukeskusteluMapper.toDto(updatedLoppukeskustelu)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/loppukeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(loppukeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val loppukeskusteluList = koejaksonLoppukeskusteluRepository.findAll()
        assertThat(loppukeskusteluList).hasSize(databaseSizeBeforeUpdate)
        val testLoppukeskustelu = loppukeskusteluList[loppukeskusteluList.size - 1]
        assertThat(testLoppukeskustelu.korjausehdotus).isNull()
        assertThat(testLoppukeskustelu.lahikouluttajaHyvaksynyt).isEqualTo(true)
        assertThat(testLoppukeskustelu.lahikouluttajanKuittausaika).isNotNull
        assertThat(testLoppukeskustelu.lahiesimiesHyvaksynyt).isEqualTo(false)
        assertThat(testLoppukeskustelu.lahiesimiehenKuittausaika).isNull()
        assertThat(testLoppukeskustelu.esitetaanKoejaksonHyvaksymista).isEqualTo(true)
    }

    @Test
    @Transactional
    fun ackLoppukeskusteluEsimies() {
        initTest(DEFAULT_ESIMIES_ID)

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        koejaksonValiarviointi.erikoistuvaAllekirjoittanut = true
        koejaksonValiarviointi.muokkauspaiva = LocalDate.now()
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        koejaksonLoppukeskustelu.lahikouluttajaHyvaksynyt = true
        koejaksonLoppukeskustelu.lahikouluttajanKuittausaika = DEFAULT_MYONTAMISPAIVA
        koejaksonLoppukeskusteluRepository.saveAndFlush(koejaksonLoppukeskustelu)

        val databaseSizeBeforeUpdate = koejaksonLoppukeskusteluRepository.findAll().size

        val id = koejaksonLoppukeskustelu.id
        assertNotNull(id)
        val updatedLoppukeskustelu = koejaksonLoppukeskusteluRepository.findById(id).get()
        em.detach(updatedLoppukeskustelu)

        updatedLoppukeskustelu.lahiesimiesHyvaksynyt = true
        updatedLoppukeskustelu.lahiesimiehenKuittausaika = DEFAULT_MYONTAMISPAIVA

        val loppukeskusteluDTO = koejaksonLoppukeskusteluMapper.toDto(updatedLoppukeskustelu)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/loppukeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(loppukeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val loppukeskusteluList = koejaksonLoppukeskusteluRepository.findAll()
        assertThat(loppukeskusteluList).hasSize(databaseSizeBeforeUpdate)
        val testLoppukeskustelu = loppukeskusteluList[loppukeskusteluList.size - 1]
        assertThat(testLoppukeskustelu.korjausehdotus).isNull()
        assertThat(testLoppukeskustelu.lahiesimiesHyvaksynyt).isEqualTo(true)
        assertThat(testLoppukeskustelu.lahiesimiehenKuittausaika).isNotNull
        assertThat(testLoppukeskustelu.lahikouluttajaHyvaksynyt).isEqualTo(true)
        assertThat(testLoppukeskustelu.lahikouluttajanKuittausaika).isNotNull
    }

    @Test
    @Transactional
    fun declineLoppukeskusteluEsimies() {
        initTest(DEFAULT_ESIMIES_ID)

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        koejaksonValiarviointi.erikoistuvaAllekirjoittanut = true
        koejaksonValiarviointi.muokkauspaiva = LocalDate.now()
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        koejaksonLoppukeskustelu.lahikouluttajaHyvaksynyt = true
        koejaksonLoppukeskustelu.lahikouluttajanKuittausaika = DEFAULT_MYONTAMISPAIVA
        koejaksonLoppukeskusteluRepository.saveAndFlush(koejaksonLoppukeskustelu)

        val databaseSizeBeforeUpdate = koejaksonLoppukeskusteluRepository.findAll().size

        val id = koejaksonLoppukeskustelu.id
        assertNotNull(id)
        val updatedLoppukeskustelu = koejaksonLoppukeskusteluRepository.findById(id).get()
        em.detach(updatedLoppukeskustelu)

        updatedLoppukeskustelu.lahiesimiesHyvaksynyt = false
        updatedLoppukeskustelu.korjausehdotus = UPDATED_KORJAUSEHDOTUS

        val loppukeskusteluDTO = koejaksonLoppukeskusteluMapper.toDto(updatedLoppukeskustelu)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/loppukeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(loppukeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val loppukeskusteluList = koejaksonLoppukeskusteluRepository.findAll()
        assertThat(loppukeskusteluList).hasSize(databaseSizeBeforeUpdate)
        val testLoppukeskustelu = loppukeskusteluList[loppukeskusteluList.size - 1]
        assertThat(testLoppukeskustelu.korjausehdotus).isEqualTo(UPDATED_KORJAUSEHDOTUS)
        assertThat(testLoppukeskustelu.lahiesimiesHyvaksynyt).isEqualTo(false)
        assertThat(testLoppukeskustelu.lahiesimiehenKuittausaika).isNull()
        assertThat(testLoppukeskustelu.lahikouluttajaHyvaksynyt).isEqualTo(false)
        assertThat(testLoppukeskustelu.lahikouluttajanKuittausaika).isNull()
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
        koejaksonValiarviointi =
            createValiarviointi(erikoistuvaLaakari, kouluttaja, esimies)
        em.persist(koejaksonValiarviointi)
        koejaksonKehittamistoimenpiteet =
            createKehittamistoimenpiteet(erikoistuvaLaakari, kouluttaja, esimies)
        em.persist(koejaksonKehittamistoimenpiteet)
        koejaksonLoppukeskustelu =
            createLoppukeskustelu(erikoistuvaLaakari, kouluttaja, esimies)
        em.persist(koejaksonLoppukeskustelu)
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

        private const val DEFAULT_VAHVUUDET = "Lorem ipsum"
        private const val DEFAULT_KEHITTAMISTOIMENPITEET = "Lorem ipsum"

        @JvmStatic
        fun createKoulutussopimus(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            vastuuhenkilo: Kayttaja
        ): KoejaksonKoulutussopimus {
            return KoejaksonKoulutussopimus(
                erikoistuvaLaakari = erikoistuvaLaakari,
                erikoistuvanNimi = erikoistuvaLaakari.kayttaja?.getNimi(),
                erikoistuvanOpiskelijatunnus = erikoistuvaLaakari.opiskelijatunnus,
                erikoistuvanSyntymaaika = DEFAULT_SYNTYMAAIKA,
                erikoistuvanYliopisto = erikoistuvaLaakari.kayttaja?.yliopisto?.nimi,
                opintooikeudenMyontamispaiva = DEFAULT_MYONTAMISPAIVA,
                koejaksonAlkamispaiva = DEFAULT_ALKAMISPAIVA,
                erikoistuvanPuhelinnumero = erikoistuvaLaakari.puhelinnumero,
                erikoistuvanSahkoposti = erikoistuvaLaakari.kayttaja?.user?.email,
                lahetetty = true,
                muokkauspaiva = DEFAULT_MUOKKAUSPAIVA,
                vastuuhenkilo = vastuuhenkilo,
                vastuuhenkilonNimi = vastuuhenkilo.getNimi(),
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
                nimi = kouluttaja.getNimi(),
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
                erikoistuvanNimi = erikoistuvaLaakari.kayttaja?.getNimi(),
                erikoistuvanErikoisala = erikoistuvaLaakari.erikoisala?.nimi,
                erikoistuvanOpiskelijatunnus = erikoistuvaLaakari.opiskelijatunnus,
                erikoistuvanYliopisto = erikoistuvaLaakari.kayttaja?.yliopisto?.nimi,
                erikoistuvanSahkoposti = erikoistuvaLaakari.kayttaja?.user?.email,
                koejaksonSuorituspaikka = DEFAULT_KOULUTUSPAIKKA,
                koejaksonAlkamispaiva = DEFAULT_ALKAMISPAIVA,
                koejaksonPaattymispaiva = DEFAULT_PAATTYMISPAIVA,
                suoritettuKokoaikatyossa = true,
                lahikouluttaja = lahikouluttaja,
                lahikouluttajanNimi = lahikouluttaja.getNimi(),
                lahiesimies = lahiesimies,
                lahiesimiehenNimi = lahiesimies.getNimi(),
                koejaksonOsaamistavoitteet = DEFAULT_OSAAMISTAVOITTEET,
                lahetetty = true,
                muokkauspaiva = DEFAULT_MUOKKAUSPAIVA
            )
        }

        @JvmStatic
        fun createValiarviointi(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            lahikouluttaja: Kayttaja,
            lahiesimies: Kayttaja
        ): KoejaksonValiarviointi {
            return KoejaksonValiarviointi(
                erikoistuvaLaakari = erikoistuvaLaakari,
                erikoistuvanNimi = erikoistuvaLaakari.kayttaja?.getNimi(),
                erikoistuvanErikoisala = erikoistuvaLaakari.erikoisala?.nimi,
                erikoistuvanOpiskelijatunnus = erikoistuvaLaakari.opiskelijatunnus,
                erikoistuvanYliopisto = erikoistuvaLaakari.kayttaja?.yliopisto?.nimi,
                lahikouluttaja = lahikouluttaja,
                lahikouluttajanNimi = lahikouluttaja.getNimi(),
                lahiesimies = lahiesimies,
                lahiesimiehenNimi = lahiesimies.getNimi(),
                muokkauspaiva = DEFAULT_MUOKKAUSPAIVA
            )
        }

        @JvmStatic
        fun createKehittamistoimenpiteet(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            lahikouluttaja: Kayttaja,
            lahiesimies: Kayttaja
        ): KoejaksonKehittamistoimenpiteet {
            return KoejaksonKehittamistoimenpiteet(
                erikoistuvaLaakari = erikoistuvaLaakari,
                erikoistuvanNimi = erikoistuvaLaakari.kayttaja?.getNimi(),
                erikoistuvanErikoisala = erikoistuvaLaakari.erikoisala?.nimi,
                erikoistuvanOpiskelijatunnus = erikoistuvaLaakari.opiskelijatunnus,
                erikoistuvanYliopisto = erikoistuvaLaakari.kayttaja?.yliopisto?.nimi,
                lahikouluttaja = lahikouluttaja,
                lahikouluttajanNimi = lahikouluttaja.getNimi(),
                lahiesimies = lahiesimies,
                lahiesimiehenNimi = lahiesimies.getNimi(),
                muokkauspaiva = DEFAULT_MUOKKAUSPAIVA
            )
        }

        @JvmStatic
        fun createLoppukeskustelu(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            lahikouluttaja: Kayttaja,
            lahiesimies: Kayttaja
        ): KoejaksonLoppukeskustelu {
            return KoejaksonLoppukeskustelu(
                erikoistuvaLaakari = erikoistuvaLaakari,
                erikoistuvanNimi = erikoistuvaLaakari.kayttaja?.getNimi(),
                erikoistuvanErikoisala = erikoistuvaLaakari.erikoisala?.nimi,
                erikoistuvanOpiskelijatunnus = erikoistuvaLaakari.opiskelijatunnus,
                erikoistuvanYliopisto = erikoistuvaLaakari.kayttaja?.yliopisto?.nimi,
                lahikouluttaja = lahikouluttaja,
                lahikouluttajanNimi = lahikouluttaja.getNimi(),
                lahiesimies = lahiesimies,
                lahiesimiehenNimi = lahiesimies.getNimi(),
                muokkauspaiva = DEFAULT_MUOKKAUSPAIVA
            )
        }
    }
}
