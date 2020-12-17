package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import fi.elsapalvelu.elsa.domain.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.Keskeytysaika
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.Tyoskentelypaikka
import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi
import fi.elsapalvelu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.service.mapper.KeskeytysaikaMapper
import fi.elsapalvelu.elsa.service.mapper.TyoskentelyjaksoMapper
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.*
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import javax.persistence.EntityManager
import kotlin.test.assertNotNull

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
class ErikoistuvaLaakariTyoskentelyjaksoResourceIT {

    @Autowired
    private lateinit var tyoskentelyjaksoRepository: TyoskentelyjaksoRepository

    @Autowired
    private lateinit var suoritusarviointiRepository: SuoritusarviointiRepository

    @Autowired
    private lateinit var keskeytysaikaRepository: KeskeytysaikaRepository

    @Autowired
    private lateinit var erikoisalaRepository: ErikoisalaRepository

    @Autowired
    private lateinit var poissaolonSyyRepository: PoissaolonSyyRepository

    @Autowired
    private lateinit var tyoskentelyjaksoMapper: TyoskentelyjaksoMapper

    @Autowired
    private lateinit var keskeytysaikaMapper: KeskeytysaikaMapper

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restTyoskentelyjaksoMockMvc: MockMvc

    @Autowired
    private lateinit var restKeskeytysaikaMockMvc: MockMvc

    private lateinit var tyoskentelyjakso: Tyoskentelyjakso

    private lateinit var keskeytysaika: Keskeytysaika

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    @Transactional
    fun createTyoskentelyjakso() {
        initTest()

        val databaseSizeBeforeCreate = tyoskentelyjaksoRepository.findAll().size

        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
        restTyoskentelyjaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/tyoskentelyjaksot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(tyoskentelyjaksoDTO))
                .with(csrf())
        ).andExpect(status().isCreated)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(databaseSizeBeforeCreate + 1)
        val testTyoskentelyjakso = tyoskentelyjaksoList[tyoskentelyjaksoList.size - 1]
        assertThat(testTyoskentelyjakso.alkamispaiva).isEqualTo(DEFAULT_ALKAMISPAIVA)
        assertThat(testTyoskentelyjakso.paattymispaiva).isEqualTo(DEFAULT_PAATTYMISPAIVA)
        assertThat(testTyoskentelyjakso.osaaikaprosentti).isEqualTo(DEFAULT_OSAAIKAPROSENTTI)
        assertThat(testTyoskentelyjakso.kaytannonKoulutus).isEqualTo(DEFAULT_KAYTANNON_KOULUTUS)
        assertThat(testTyoskentelyjakso.hyvaksyttyAiempaanErikoisalaan)
            .isEqualTo(DEFAULT_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN)
    }

    @Test
    @Transactional
    fun createTyoskentelyjaksoForAnotherUser() {
        initTest(null)

        val databaseSizeBeforeCreate = tyoskentelyjaksoRepository.findAll().size

        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
        restTyoskentelyjaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/tyoskentelyjaksot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(tyoskentelyjaksoDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createTyoskentelyjaksoWithExistingId() {
        initTest()

        val databaseSizeBeforeCreate = tyoskentelyjaksoRepository.findAll().size

        tyoskentelyjakso.id = 1L
        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
        restTyoskentelyjaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/tyoskentelyjaksot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(tyoskentelyjaksoDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createTyoskentelyjaksoWithExistingTyoskenetelypaikkaId() {
        initTest()

        val databaseSizeBeforeCreate = tyoskentelyjaksoRepository.findAll().size

        tyoskentelyjakso.tyoskentelypaikka!!.id = 1L
        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
        restTyoskentelyjaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/tyoskentelyjaksot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(tyoskentelyjaksoDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createTyoskentelyjaksoWithShortPeriod() {
        initTest()

        val databaseSizeBeforeCreate = tyoskentelyjaksoRepository.findAll().size

        tyoskentelyjakso.alkamispaiva = LocalDate.of(2020, 1, 25)
        tyoskentelyjakso.paattymispaiva = LocalDate.of(2020, 2, 5)
        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
        restTyoskentelyjaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/tyoskentelyjaksot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(tyoskentelyjaksoDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createTyoskentelyjaksoWithInvalidDates() {
        initTest()

        val databaseSizeBeforeCreate = tyoskentelyjaksoRepository.findAll().size

        tyoskentelyjakso.alkamispaiva = LocalDate.of(2020, 1, 25)
        tyoskentelyjakso.paattymispaiva = LocalDate.of(2020, 1, 5)
        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
        restTyoskentelyjaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/tyoskentelyjaksot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(tyoskentelyjaksoDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun getTyoskentelyjakso() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val id = tyoskentelyjakso.id
        assertNotNull(id)

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(tyoskentelyjakso.id as Any))
            .andExpect(jsonPath("$.alkamispaiva").value(DEFAULT_ALKAMISPAIVA.toString()))
            .andExpect(jsonPath("$.paattymispaiva").value(DEFAULT_PAATTYMISPAIVA.toString()))
            .andExpect(jsonPath("$.osaaikaprosentti").value(DEFAULT_OSAAIKAPROSENTTI))
            .andExpect(jsonPath("$.kaytannonKoulutus").value(DEFAULT_KAYTANNON_KOULUTUS.toString()))
            .andExpect(jsonPath("$.hyvaksyttyAiempaanErikoisalaan").value(DEFAULT_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN))
    }

    @Test
    @Transactional
    fun getAnotherUserTyoskentelyjakso() {
        initTest(null)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val id = tyoskentelyjakso.id
        assertNotNull(id)

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot/{id}", id))
            .andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun updateTyoskentelyjakso() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val databaseSizeBeforeUpdate = tyoskentelyjaksoRepository.findAll().size

        val id = tyoskentelyjakso.id
        assertNotNull(id)
        val updatedTyoskentelyjakso = tyoskentelyjaksoRepository.findById(id).get()

        updatedTyoskentelyjakso.alkamispaiva = UPDATED_ALKAMISPAIVA
        updatedTyoskentelyjakso.paattymispaiva = UPDATED_PAATTYMISPAIVA
        updatedTyoskentelyjakso.osaaikaprosentti = UPDATED_OSAAIKAPROSENTTI
        updatedTyoskentelyjakso.kaytannonKoulutus = UPDATED_KAYTANNON_KOULUTUS
        updatedTyoskentelyjakso.hyvaksyttyAiempaanErikoisalaan = UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN
        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(updatedTyoskentelyjakso)

        restTyoskentelyjaksoMockMvc.perform(
            put("/api/erikoistuva-laakari/tyoskentelyjaksot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(tyoskentelyjaksoDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(databaseSizeBeforeUpdate)
        val testTyoskentelyjakso = tyoskentelyjaksoList[tyoskentelyjaksoList.size - 1]
        assertThat(testTyoskentelyjakso.alkamispaiva).isEqualTo(UPDATED_ALKAMISPAIVA)
        assertThat(testTyoskentelyjakso.paattymispaiva).isEqualTo(UPDATED_PAATTYMISPAIVA)
        assertThat(testTyoskentelyjakso.osaaikaprosentti).isEqualTo(UPDATED_OSAAIKAPROSENTTI)
        assertThat(testTyoskentelyjakso.kaytannonKoulutus).isEqualTo(UPDATED_KAYTANNON_KOULUTUS)
        assertThat(testTyoskentelyjakso.hyvaksyttyAiempaanErikoisalaan)
            .isEqualTo(UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN)
    }

    @Test
    @Transactional
    fun updateTyoskentelyjaksoWithoutId() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val databaseSizeBeforeUpdate = tyoskentelyjaksoRepository.findAll().size

        val id = tyoskentelyjakso.id
        assertNotNull(id)
        val updatedTyoskentelyjakso = tyoskentelyjaksoRepository.findById(id).get()
        em.detach(updatedTyoskentelyjakso)

        updatedTyoskentelyjakso.id = null
        updatedTyoskentelyjakso.alkamispaiva = UPDATED_ALKAMISPAIVA
        updatedTyoskentelyjakso.paattymispaiva = UPDATED_PAATTYMISPAIVA
        updatedTyoskentelyjakso.osaaikaprosentti = UPDATED_OSAAIKAPROSENTTI
        updatedTyoskentelyjakso.kaytannonKoulutus = UPDATED_KAYTANNON_KOULUTUS
        updatedTyoskentelyjakso.hyvaksyttyAiempaanErikoisalaan = UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN
        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(updatedTyoskentelyjakso)

        restTyoskentelyjaksoMockMvc.perform(
            put("/api/erikoistuva-laakari/tyoskentelyjaksot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(tyoskentelyjaksoDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun updateAnotherUserTyoskentelyjakso() {
        initTest(null)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val databaseSizeBeforeUpdate = tyoskentelyjaksoRepository.findAll().size

        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
        restTyoskentelyjaksoMockMvc.perform(
            put("/api/erikoistuva-laakari/tyoskentelyjaksot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(tyoskentelyjaksoDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun deleteTyoskentelyjakso() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val databaseSizeBeforeDelete = tyoskentelyjaksoRepository.findAll().size

        restTyoskentelyjaksoMockMvc.perform(
            delete("/api/erikoistuva-laakari/tyoskentelyjaksot/{id}", tyoskentelyjakso.id)
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf())
        ).andExpect(status().isNoContent)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(databaseSizeBeforeDelete - 1)
    }

    @Test
    @Transactional
    fun deleteAnotherUserTyoskentelyjakso() {
        initTest(null)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val databaseSizeBeforeDelete = tyoskentelyjaksoRepository.findAll().size

        restTyoskentelyjaksoMockMvc.perform(
            delete("/api/erikoistuva-laakari/tyoskentelyjaksot/{id}", tyoskentelyjakso.id)
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf())
        ).andExpect(status().isNoContent)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(databaseSizeBeforeDelete)
    }

    @Test
    @Transactional
    fun deleteTyoskentelyjaksoWithSuoritusarviointi() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val databaseSizeBeforeDelete = tyoskentelyjaksoRepository.findAll().size

        em.detach(tyoskentelyjakso)

        val suoritusarviointi = SuoritusarviointiHelper.createEntity(em, DEFAULT_ID)

        suoritusarviointiRepository.saveAndFlush(suoritusarviointi)

        restTyoskentelyjaksoMockMvc.perform(
            delete("/api/erikoistuva-laakari/tyoskentelyjaksot/{id}", tyoskentelyjakso.id)
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf())
        ).andExpect(status().isNoContent)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(databaseSizeBeforeDelete)
    }

    @Test
    @Transactional
    fun createKeskeytysaika() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)

        val databaseSizeBeforeCreate = keskeytysaikaRepository.findAll().size

        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        restKeskeytysaikaMockMvc.perform(
            post("/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO))
                .with(csrf())
        ).andExpect(status().isCreated)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(databaseSizeBeforeCreate + 1)
        val testKeskeytysaika = keskeytysaikaList[keskeytysaikaList.size - 1]
        assertThat(testKeskeytysaika.alkamispaiva).isEqualTo(KeskeytysaikaHelper.DEFAULT_ALKAMISPAIVA)
        assertThat(testKeskeytysaika.paattymispaiva).isEqualTo(KeskeytysaikaHelper.DEFAULT_PAATTYMISPAIVA)
        assertThat(testKeskeytysaika.osaaikaprosentti).isEqualTo(KeskeytysaikaHelper.DEFAULT_OSAAIKAPROSENTTI)
    }

    @Test
    @Transactional
    fun createKeskeytysaikaWithExistingId() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)

        val databaseSizeBeforeCreate = keskeytysaikaRepository.findAll().size

        keskeytysaika.id = 1L
        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        restKeskeytysaikaMockMvc.perform(
            post("/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createKeskeytysaikaForAnotherUser() {
        initTest(null)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)

        val databaseSizeBeforeCreate = keskeytysaikaRepository.findAll().size

        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        restKeskeytysaikaMockMvc.perform(
            post("/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createKeskeytysaikaWithInvalidDates() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaika.alkamispaiva = LocalDate.of(2020, 1, 1)
        keskeytysaika.paattymispaiva = LocalDate.of(2020, 12, 1)

        val databaseSizeBeforeCreate = keskeytysaikaRepository.findAll().size

        var keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        restKeskeytysaikaMockMvc.perform(
            post("/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        var keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(databaseSizeBeforeCreate)

        keskeytysaika.alkamispaiva = LocalDate.of(2019, 12, 1)
        keskeytysaika.paattymispaiva = LocalDate.of(2020, 1, 10)

        keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        restKeskeytysaikaMockMvc.perform(
            post("/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(databaseSizeBeforeCreate)

        keskeytysaika.alkamispaiva = LocalDate.of(2020, 1, 15)
        keskeytysaika.paattymispaiva = LocalDate.of(2020, 1, 10)

        keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        restKeskeytysaikaMockMvc.perform(
            post("/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun getKeskeytysaika() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val id = keskeytysaika.id
        assertNotNull(id)

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot/{id}", id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(keskeytysaika.id as Any))
            .andExpect(jsonPath("$.alkamispaiva").value(KeskeytysaikaHelper.DEFAULT_ALKAMISPAIVA.toString()))
            .andExpect(jsonPath("$.paattymispaiva").value(KeskeytysaikaHelper.DEFAULT_PAATTYMISPAIVA.toString()))
            .andExpect(jsonPath("$.osaaikaprosentti").value(KeskeytysaikaHelper.DEFAULT_OSAAIKAPROSENTTI))
    }

    @Test
    @Transactional
    fun getAnotherUserKeskeytysaika() {
        initTest(null)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val id = keskeytysaika.id
        assertNotNull(id)

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot/{id}", id))
            .andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun updateKeskeytysaika() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val databaseSizeBeforeUpdate = keskeytysaikaRepository.findAll().size

        val id = keskeytysaika.id
        assertNotNull(id)

        val updatedKeskeytysaika = keskeytysaikaRepository.findById(id).get()
        em.detach(updatedKeskeytysaika)
        updatedKeskeytysaika.alkamispaiva = KeskeytysaikaHelper.UPDATED_ALKAMISPAIVA
        updatedKeskeytysaika.paattymispaiva = KeskeytysaikaHelper.UPDATED_PAATTYMISPAIVA
        updatedKeskeytysaika.osaaikaprosentti = KeskeytysaikaHelper.UPDATED_OSAAIKAPROSENTTI
        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(updatedKeskeytysaika)

        restKeskeytysaikaMockMvc.perform(
            put("/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(databaseSizeBeforeUpdate)
        val testKeskeytysaika = keskeytysaikaList[keskeytysaikaList.size - 1]
        assertThat(testKeskeytysaika.alkamispaiva).isEqualTo(KeskeytysaikaHelper.UPDATED_ALKAMISPAIVA)
        assertThat(testKeskeytysaika.paattymispaiva).isEqualTo(KeskeytysaikaHelper.UPDATED_PAATTYMISPAIVA)
        assertThat(testKeskeytysaika.osaaikaprosentti).isEqualTo(KeskeytysaikaHelper.UPDATED_OSAAIKAPROSENTTI)
    }

    @Test
    @Transactional
    fun updateAnotherUserKeskeytysaika() {
        initTest(null)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val databaseSizeBeforeUpdate = keskeytysaikaRepository.findAll().size

        val id = keskeytysaika.id
        assertNotNull(id)

        val updatedKeskeytysaika = keskeytysaikaRepository.findById(id).get()
        em.detach(updatedKeskeytysaika)
        updatedKeskeytysaika.alkamispaiva = KeskeytysaikaHelper.UPDATED_ALKAMISPAIVA
        updatedKeskeytysaika.paattymispaiva = KeskeytysaikaHelper.UPDATED_PAATTYMISPAIVA
        updatedKeskeytysaika.osaaikaprosentti = KeskeytysaikaHelper.UPDATED_OSAAIKAPROSENTTI
        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(updatedKeskeytysaika)

        restKeskeytysaikaMockMvc.perform(
            put("/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun updateKeskeytysaikaWithoutId() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val databaseSizeBeforeUpdate = keskeytysaikaRepository.findAll().size

        val id = keskeytysaika.id
        assertNotNull(id)

        val updatedKeskeytysaika = keskeytysaikaRepository.findById(id).get()
        em.detach(updatedKeskeytysaika)
        updatedKeskeytysaika.id = null
        updatedKeskeytysaika.alkamispaiva = KeskeytysaikaHelper.UPDATED_ALKAMISPAIVA
        updatedKeskeytysaika.paattymispaiva = KeskeytysaikaHelper.UPDATED_PAATTYMISPAIVA
        updatedKeskeytysaika.osaaikaprosentti = KeskeytysaikaHelper.UPDATED_OSAAIKAPROSENTTI
        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(updatedKeskeytysaika)

        restKeskeytysaikaMockMvc.perform(
            put("/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun deleteKeskeytysaika() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val databaseSizeBeforeDelete = keskeytysaikaRepository.findAll().size

        restKeskeytysaikaMockMvc.perform(
            delete("/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot/{id}", keskeytysaika.id)
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf())
        ).andExpect(status().isNoContent)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(databaseSizeBeforeDelete - 1)
    }

    @Test
    @Transactional
    fun deleteAnotherUserKeskeytysaika() {
        initTest(null)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val databaseSizeBeforeDelete = keskeytysaikaRepository.findAll().size

        restKeskeytysaikaMockMvc.perform(
            delete("/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot/{id}", keskeytysaika.id)
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf())
        ).andExpect(status().isNoContent)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(databaseSizeBeforeDelete)
    }

    @Test
    @Transactional
    fun getKeskeytysaikaForm() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val poissaolonSyy = PoissaolonSyyHelper.createEntity()
        poissaolonSyyRepository.saveAndFlush(poissaolonSyy)

        val id = tyoskentelyjakso.id
        assertNotNull(id)

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/poissaolo-lomake"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.poissaolonSyyt").value(Matchers.hasSize<Any>(16)))
            .andExpect(jsonPath("$.tyoskentelyjaksot").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$.tyoskentelyjaksot[0].id").value(id as Any))
    }

    @Test
    @Transactional
    fun getTyoskentelyjaksoForm() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val id = tyoskentelyjakso.id
        assertNotNull(id)

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjakso-lomake"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.kunnat").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$.erikoisalat").value(Matchers.hasSize<Any>(58)))
            .andExpect(jsonPath("$.erikoisalat[0].id").value(1))
    }

    @Test
    @Transactional
    fun getTyoskentelyjaksoTable() {
        initTest()

        tyoskentelyjakso.kaytannonKoulutus = KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO
        tyoskentelyjakso.tyoskentelypaikka!!.tyyppi = TyoskentelyjaksoTyyppi.TERVEYSKESKUS
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        em.detach(tyoskentelyjakso)

        val tyoskentelyjakso2 = createEntity(em, DEFAULT_ID)
        tyoskentelyjakso2.hyvaksyttyAiempaanErikoisalaan = true
        tyoskentelyjakso2.kaytannonKoulutus = KaytannonKoulutusTyyppi.OMAA_ERIKOISALAA_TUKEVA_KOULUTUS
        tyoskentelyjakso2.tyoskentelypaikka!!.tyyppi = TyoskentelyjaksoTyyppi.YLIOPISTOLLINEN_SAIRAALA
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso2)
        em.detach(tyoskentelyjakso2)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)
        em.detach(keskeytysaika)

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot-taulukko"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.poissaolonSyyt").value(Matchers.hasSize<Any>(15)))
            .andExpect(jsonPath("$.tyoskentelyjaksot").value(Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$.keskeytykset").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyaikaYhteensa").value(57.0))
            .andExpect(jsonPath("$.tilastot.arvioErikoistumiseenHyvaksyttavista").value(57.0))
            .andExpect(jsonPath("$.tilastot.arvioPuuttuvastaKoulutuksesta").value(1768.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.terveyskeskusVaadittuVahintaan").value(273.75))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.terveyskeskusSuoritettu").value(27.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaalaVaadittuVahintaan").value(365.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaalaSuoritettu").value(30.0))
            .andExpect(
                jsonPath("$.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenVaadittuVahintaan").value(365.0)
            )
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenSuoritettu").value(0.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yhteensaVaadittuVahintaan").value(1825.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yhteensaSuoritettu").value(57.0))
            .andExpect(jsonPath("$.tilastot.kaytannonKoulutus").value(Matchers.hasSize<Any>(4)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot").value(Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot[0].suoritettu").value(27.0))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot[1].suoritettu").value(30.0))
    }

    fun initTest(userId: String? = DEFAULT_ID) {
        val userDetails = mapOf<String, Any>(
            "uid" to DEFAULT_ID,
            "sub" to DEFAULT_LOGIN,
            "email" to DEFAULT_EMAIL
        )
        val authorities = listOf(SimpleGrantedAuthority(ERIKOISTUVA_LAAKARI))
        val user = DefaultOAuth2User(authorities, userDetails, "sub")
        val authentication = OAuth2AuthenticationToken(user, authorities, "oidc")
        TestSecurityContextHolder.getContext().authentication = authentication

        tyoskentelyjakso = createEntity(em, userId)
    }

    companion object {

        private const val DEFAULT_ID = "c47f46ad-21c4-47e8-9c7c-ba44f60c8bae"
        private const val DEFAULT_LOGIN = "johndoe"
        private const val DEFAULT_EMAIL = "john.doe@example.com"

        private val DEFAULT_ALKAMISPAIVA: LocalDate = LocalDate.of(2020, 1, 1)
        private val UPDATED_ALKAMISPAIVA: LocalDate = LocalDate.of(2020, 2, 1)

        private val DEFAULT_PAATTYMISPAIVA: LocalDate = LocalDate.of(2020, 1, 30)
        private val UPDATED_PAATTYMISPAIVA: LocalDate = LocalDate.of(2020, 4, 1)

        private const val DEFAULT_OSAAIKAPROSENTTI: Int = 100
        private const val UPDATED_OSAAIKAPROSENTTI: Int = 50

        private val DEFAULT_KAYTANNON_KOULUTUS: KaytannonKoulutusTyyppi =
            KaytannonKoulutusTyyppi.OMAN_ERIKOISALAN_KOULUTUS
        private val UPDATED_KAYTANNON_KOULUTUS: KaytannonKoulutusTyyppi =
            KaytannonKoulutusTyyppi.OMAA_ERIKOISALAA_TUKEVA_KOULUTUS

        private const val DEFAULT_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN: Boolean = false
        private const val UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN: Boolean = true

        @JvmStatic
        fun createEntity(em: EntityManager, userId: String? = null): Tyoskentelyjakso {
            val tyoskentelyjakso = Tyoskentelyjakso(
                alkamispaiva = DEFAULT_ALKAMISPAIVA,
                paattymispaiva = DEFAULT_PAATTYMISPAIVA,
                osaaikaprosentti = DEFAULT_OSAAIKAPROSENTTI,
                kaytannonKoulutus = DEFAULT_KAYTANNON_KOULUTUS,
                hyvaksyttyAiempaanErikoisalaan = DEFAULT_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN
            )

            // Lisätään pakollinen tieto
            tyoskentelyjakso.tyoskentelypaikka = TyoskentelypaikkaHelper.createEntity(em)

            // Lisätään pakollinen tieto
            val erikoistuvaLaakari: ErikoistuvaLaakari
            if (em.findAll(ErikoistuvaLaakari::class).isEmpty()) {
                erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, userId)
                em.persist(erikoistuvaLaakari)
                em.flush()
            } else {
                erikoistuvaLaakari = em.findAll(ErikoistuvaLaakari::class).get(0)
            }
            tyoskentelyjakso.erikoistuvaLaakari = erikoistuvaLaakari

            return tyoskentelyjakso
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Tyoskentelyjakso {
            val tyoskentelyjakso = Tyoskentelyjakso(
                alkamispaiva = UPDATED_ALKAMISPAIVA,
                paattymispaiva = UPDATED_PAATTYMISPAIVA,
                osaaikaprosentti = UPDATED_OSAAIKAPROSENTTI,
                kaytannonKoulutus = UPDATED_KAYTANNON_KOULUTUS,
                hyvaksyttyAiempaanErikoisalaan = UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN
            )

            // Lisätään pakollinen tieto
            val tyoskentelypaikka: Tyoskentelypaikka
            if (em.findAll(Tyoskentelypaikka::class).isEmpty()) {
                tyoskentelypaikka = TyoskentelypaikkaHelper.createUpdatedEntity(em)
                em.persist(tyoskentelypaikka)
                em.flush()
            } else {
                tyoskentelypaikka = em.findAll(Tyoskentelypaikka::class).get(0)
            }
            tyoskentelyjakso.tyoskentelypaikka = tyoskentelypaikka

            // Lisätään pakollinen tieto
            val erikoistuvaLaakari: ErikoistuvaLaakari
            if (em.findAll(ErikoistuvaLaakari::class).isEmpty()) {
                erikoistuvaLaakari = ErikoistuvaLaakariHelper.createUpdatedEntity(em)
                em.persist(erikoistuvaLaakari)
                em.flush()
            } else {
                erikoistuvaLaakari = em.findAll(ErikoistuvaLaakari::class).get(0)
            }
            tyoskentelyjakso.erikoistuvaLaakari = erikoistuvaLaakari

            return tyoskentelyjakso
        }
    }
}
