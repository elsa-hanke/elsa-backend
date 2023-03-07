package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.KaytannonKoulutusTyyppi
import fi.elsapalvelu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.mapper.ErikoisalaMapper
import fi.elsapalvelu.elsa.service.mapper.KeskeytysaikaMapper
import fi.elsapalvelu.elsa.service.mapper.KuntaMapper
import fi.elsapalvelu.elsa.service.mapper.TyoskentelyjaksoMapper
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
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
import java.io.File
import java.time.LocalDate
import jakarta.persistence.EntityManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class ErikoistuvaLaakariTyoskentelyjaksoResourceIT {

    @Autowired
    private lateinit var tyoskentelyjaksoRepository: TyoskentelyjaksoRepository

    @Autowired
    private lateinit var suoritusarviointiRepository: SuoritusarviointiRepository

    @Autowired
    private lateinit var keskeytysaikaRepository: KeskeytysaikaRepository

    @Autowired
    private lateinit var erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var kayttajaRepository: KayttajaRepository

    @Autowired
    private lateinit var kayttajaYliopistoErikoisalaRepository: KayttajaYliopistoErikoisalaRepository

    @Autowired
    private lateinit var opintooikeusRepository: OpintooikeusRepository

    @Autowired
    private lateinit var opintoopasRepository: OpintoopasRepository

    @Autowired
    private lateinit var tyoskentelyjaksoMapper: TyoskentelyjaksoMapper

    @Autowired
    private lateinit var kuntaMapper: KuntaMapper

    @Autowired
    private lateinit var erikoisalaMapper: ErikoisalaMapper

    @Autowired
    private lateinit var keskeytysaikaMapper: KeskeytysaikaMapper

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restTyoskentelyjaksoMockMvc: MockMvc

    @Autowired
    private lateinit var restKeskeytysaikaMockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    private lateinit var tyoskentelyjakso: Tyoskentelyjakso

    private lateinit var keskeytysaika: Keskeytysaika

    private lateinit var tempFile1: File

    private lateinit var tempFile2: File

    private lateinit var mockMultipartFile1: MockMultipartFile

    private lateinit var mockMultipartFile2: MockMultipartFile

    private lateinit var user: User

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)

        // Lisätään voimassaoleva poissaolon syy ja päättymistä ei määritetty
        em.persist(PoissaolonSyyHelper.createEntity(LocalDate.ofEpochDay(0L), null))
        // Lisätään voimassaoleva poissaolon syy ja päättyminen määritetty
        em.persist(
            PoissaolonSyyHelper.createEntity(
                LocalDate.ofEpochDay(0L),
                LocalDate.ofEpochDay(20L)
            )
        )
        // Lisätään poissaolon syy, jonka voimassaolo ei ole alkanut vielä
        em.persist(
            PoissaolonSyyHelper.createEntity(
                LocalDate.ofEpochDay(15L),
                LocalDate.ofEpochDay(20L)
            )
        )
        // Lisätään poissaolon syy, jonka voimassaolo on jo päättynyt
        em.persist(
            PoissaolonSyyHelper.createEntity(
                LocalDate.ofEpochDay(0L),
                LocalDate.ofEpochDay(5L)
            )
        )

        em.flush()
    }

    @Test
    @Transactional
    fun createTyoskentelyjakso() {
        initTest()
        initMockFiles()

        val tyoskentelyjaksoTableSizeBeforeCreate = tyoskentelyjaksoRepository.findAll().size
        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoDTO)

        restTyoskentelyjaksoMockMvc.perform(
            multipart("/api/erikoistuva-laakari/tyoskentelyjaksot")
                .file(mockMultipartFile1).file(mockMultipartFile2)
                .param("tyoskentelyjaksoJson", tyoskentelyjaksoJson)
                .with(csrf())
        ).andExpect(status().isCreated)

        val kirjautunutErikoistuvaLaakari =
            erikoistuvaLaakariRepository.findOneByKayttajaUserId(user.id!!)
        requireNotNull(kirjautunutErikoistuvaLaakari)
        val defaultTyoskentelypaikka = TyoskentelypaikkaHelper.createEntity(em)
        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(tyoskentelyjaksoTableSizeBeforeCreate + 1)
        val testTyoskentelyjakso = tyoskentelyjaksoList[tyoskentelyjaksoList.size - 1]
        assertThat(testTyoskentelyjakso.opintooikeus?.id).isEqualTo(
            kirjautunutErikoistuvaLaakari.getOpintooikeusKaytossa()?.id
        )
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.nimi).isEqualTo(defaultTyoskentelypaikka.nimi)
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.tyyppi).isEqualTo(
            defaultTyoskentelypaikka.tyyppi
        )
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.muuTyyppi).isNull()
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.kunta).isEqualTo(defaultTyoskentelypaikka.kunta)
        assertThat(testTyoskentelyjakso.alkamispaiva).isEqualTo(DEFAULT_ALKAMISPAIVA)
        assertThat(testTyoskentelyjakso.paattymispaiva).isEqualTo(DEFAULT_PAATTYMISPAIVA)
        assertThat(testTyoskentelyjakso.osaaikaprosentti).isEqualTo(DEFAULT_OSAAIKAPROSENTTI)
        assertThat(testTyoskentelyjakso.kaytannonKoulutus).isEqualTo(DEFAULT_KAYTANNON_KOULUTUS)
        assertThat(testTyoskentelyjakso.omaaErikoisalaaTukeva).isNull()
        assertThat(testTyoskentelyjakso.hyvaksyttyAiempaanErikoisalaan).isEqualTo(
            DEFAULT_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN
        )
        val asiakirjaList = testTyoskentelyjakso.asiakirjat
        assertThat(asiakirjaList).hasSize(2)

        val testAsiakirja = asiakirjaList.first()
        assertThat(testAsiakirja.id).isNotNull
        assertThat(testAsiakirja.nimi).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PDF_NIMI)
        assertThat(testAsiakirja.lisattypvm?.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(testAsiakirja.tyyppi).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PDF_TYYPPI)
        assertThat(testAsiakirja.asiakirjaData?.data).isEqualTo(
            AsiakirjaHelper.ASIAKIRJA_PDF_DATA
        )

        val testAsiakirja2 = asiakirjaList.last()
        assertThat(testAsiakirja2.id).isNotNull
        assertThat(testAsiakirja2.nimi).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PNG_NIMI)
        assertThat(testAsiakirja2.lisattypvm?.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(testAsiakirja2.tyyppi).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PNG_TYYPPI)
        assertThat(testAsiakirja2.asiakirjaData?.data).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PNG_DATA)
    }

    @Test
    @Transactional
    fun createTyoskentelyjaksoWithExistingId() {
        initTest()

        val tyoskentelyjaksoTableSizeBeforeCreate = tyoskentelyjaksoRepository.findAll().size

        tyoskentelyjakso.id = 1L
        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoDTO)

        restTyoskentelyjaksoMockMvc.perform(
            multipart("/api/erikoistuva-laakari/tyoskentelyjaksot")
                .param("tyoskentelyjaksoJson", tyoskentelyjaksoJson)
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createTyoskentelyjaksoWithExistingTyoskentelypaikkaId() {
        initTest()

        val tyoskentelyjaksoTableSizeBeforeCreate = tyoskentelyjaksoRepository.findAll().size

        tyoskentelyjakso.tyoskentelypaikka!!.id = 1L
        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoDTO)

        restTyoskentelyjaksoMockMvc.perform(
            multipart("/api/erikoistuva-laakari/tyoskentelyjaksot")
                .param("tyoskentelyjaksoJson", tyoskentelyjaksoJson)
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createTyoskentelyjaksoWithInvalidDates() {
        initTest()

        val tyoskentelyjaksoTableSizeBeforeCreate = tyoskentelyjaksoRepository.findAll().size

        tyoskentelyjakso.alkamispaiva = LocalDate.of(2020, 1, 25)
        tyoskentelyjakso.paattymispaiva = LocalDate.of(2020, 1, 5)
        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoDTO)

        restTyoskentelyjaksoMockMvc.perform(
            multipart("/api/erikoistuva-laakari/tyoskentelyjaksot")
                .param("tyoskentelyjaksoJson", tyoskentelyjaksoJson)
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun getTyoskentelyjakso() {
        initTest()

        tyoskentelyjakso.asiakirjat.add(
            AsiakirjaHelper.createEntity(
                em,
                user,
                tyoskentelyjakso
            )
        )
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val id = tyoskentelyjakso.id
        assertNotNull(id)

        restTyoskentelyjaksoMockMvc.perform(
            get(
                "/api/erikoistuva-laakari/tyoskentelyjaksot/{id}",
                id
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(tyoskentelyjakso.id as Any))
            .andExpect(jsonPath("$.alkamispaiva").value(DEFAULT_ALKAMISPAIVA.toString()))
            .andExpect(jsonPath("$.paattymispaiva").value(DEFAULT_PAATTYMISPAIVA.toString()))
            .andExpect(jsonPath("$.osaaikaprosentti").value(DEFAULT_OSAAIKAPROSENTTI))
            .andExpect(jsonPath("$.kaytannonKoulutus").value(DEFAULT_KAYTANNON_KOULUTUS.toString()))
            .andExpect(
                jsonPath("$.hyvaksyttyAiempaanErikoisalaan").value(
                    DEFAULT_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN
                )
            )
            .andExpect(jsonPath("$.asiakirjat").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$.asiakirjat[0].id").exists())
            .andExpect(jsonPath("$.asiakirjat[0].nimi").value(AsiakirjaHelper.ASIAKIRJA_PDF_NIMI))
            .andExpect(
                jsonPath("$.asiakirjat[0].lisattypvm").value(
                    Matchers.containsString(
                        LocalDate.now().toString()
                    )
                )
            )
            .andExpect(jsonPath("$.asiakirjat[0].tyyppi").value(AsiakirjaHelper.ASIAKIRJA_PDF_TYYPPI))
            .andExpect(jsonPath("$.asiakirjat[0].asiakirjaData.fileInputStream").doesNotExist())
            .andExpect(jsonPath("$.asiakirjat[0].asiakirjaData.fileSize").doesNotExist())
    }

    @Test
    @Transactional
    fun getAnotherUserTyoskentelyjakso() {
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        initTest(erikoistuvaLaakari.kayttaja?.user?.id)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val id = tyoskentelyjakso.id
        assertNotNull(id)

        restTyoskentelyjaksoMockMvc.perform(
            get(
                "/api/erikoistuva-laakari/tyoskentelyjaksot/{id}",
                id
            )
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun updateTyoskentelyjaksoWithoutSuoritusarvioinnit() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeUpdate = tyoskentelyjaksoRepository.findAll().size

        val id = tyoskentelyjakso.id
        assertNotNull(id)
        val existingTyoskentelyjakso = tyoskentelyjaksoRepository.findById(id).get()
        val updatedTyoskentelypaikka = TyoskentelypaikkaHelper.createUpdatedEntity(em)
        val omaaErikoisalaaTukeva = em.findAll(Erikoisala::class).first()

        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(existingTyoskentelyjakso)
        tyoskentelyjaksoDTO.apply {
            tyoskentelypaikka?.nimi = updatedTyoskentelypaikka.nimi
            tyoskentelypaikka?.tyyppi = updatedTyoskentelypaikka.tyyppi
            tyoskentelypaikka?.muuTyyppi = updatedTyoskentelypaikka.muuTyyppi
            tyoskentelypaikka?.kunta = kuntaMapper.toDto(updatedTyoskentelypaikka.kunta!!)
            alkamispaiva = UPDATED_ALKAMISPAIVA
            paattymispaiva = UPDATED_PAATTYMISPAIVA
            osaaikaprosentti = UPDATED_OSAAIKAPROSENTTI
            kaytannonKoulutus = UPDATED_KAYTANNON_KOULUTUS
            this.omaaErikoisalaaTukeva = erikoisalaMapper.toDto(omaaErikoisalaaTukeva)
            omaaErikoisalaaTukevaId = omaaErikoisalaaTukeva.id
            hyvaksyttyAiempaanErikoisalaan = UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN
        }

        val updatedTyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoDTO)

        restTyoskentelyjaksoMockMvc.perform(
            put("/api/erikoistuva-laakari/tyoskentelyjaksot")
                .param("tyoskentelyjaksoJson", updatedTyoskentelyjaksoJson)
                .with(csrf())
        ).andExpect(status().isOk)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
        val testTyoskentelyjakso = tyoskentelyjaksoList[tyoskentelyjaksoList.size - 1]

        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.nimi).isEqualTo(updatedTyoskentelypaikka.nimi)
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.tyyppi).isEqualTo(
            updatedTyoskentelypaikka.tyyppi
        )
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.muuTyyppi).isEqualTo(
            updatedTyoskentelypaikka.muuTyyppi
        )
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.kunta).isEqualTo(updatedTyoskentelypaikka.kunta)
        assertThat(testTyoskentelyjakso.alkamispaiva).isEqualTo(UPDATED_ALKAMISPAIVA)
        assertThat(testTyoskentelyjakso.paattymispaiva).isEqualTo(UPDATED_PAATTYMISPAIVA)
        assertThat(testTyoskentelyjakso.osaaikaprosentti).isEqualTo(UPDATED_OSAAIKAPROSENTTI)
        assertThat(testTyoskentelyjakso.kaytannonKoulutus).isEqualTo(UPDATED_KAYTANNON_KOULUTUS)
        assertThat(testTyoskentelyjakso.omaaErikoisalaaTukeva).isEqualTo(omaaErikoisalaaTukeva)
        assertThat(testTyoskentelyjakso.hyvaksyttyAiempaanErikoisalaan)
            .isEqualTo(UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN)
    }

    @Test
    @Transactional
    fun updateTyoskentelyjaksoWithSuoritusarvioinnitShouldUpdateOnlyPaattymispaiva() {
        initTest()

        assertThat(tyoskentelyjakso.suoritusarvioinnit).isEmpty()

        tyoskentelyjakso.suoritusarvioinnit.add(
            SuoritusarviointiHelper.createEntity(em, user, UPDATED_ALKAMISPAIVA.plusDays(1))
        )
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeUpdate = tyoskentelyjaksoRepository.findAll().size

        val id = tyoskentelyjakso.id
        assertNotNull(id)

        val existingTyoskentelyjakso = tyoskentelyjaksoRepository.findById(id).get()
        val updatedTyoskentelypaikka = TyoskentelypaikkaHelper.createUpdatedEntity(em)
        val omaaErikoisalaaTukeva = em.findAll(Erikoisala::class).first()

        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(existingTyoskentelyjakso)
        tyoskentelyjaksoDTO.apply {
            tyoskentelypaikka?.nimi = updatedTyoskentelypaikka.nimi
            tyoskentelypaikka?.tyyppi = updatedTyoskentelypaikka.tyyppi
            tyoskentelypaikka?.muuTyyppi = updatedTyoskentelypaikka.muuTyyppi
            tyoskentelypaikka?.kunta = kuntaMapper.toDto(updatedTyoskentelypaikka.kunta!!)
            alkamispaiva = UPDATED_ALKAMISPAIVA
            paattymispaiva = UPDATED_PAATTYMISPAIVA
            osaaikaprosentti = UPDATED_OSAAIKAPROSENTTI
            kaytannonKoulutus = UPDATED_KAYTANNON_KOULUTUS
            this.omaaErikoisalaaTukeva = erikoisalaMapper.toDto(omaaErikoisalaaTukeva)
            omaaErikoisalaaTukevaId = omaaErikoisalaaTukeva.id
            hyvaksyttyAiempaanErikoisalaan = UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN
        }

        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoDTO)

        restTyoskentelyjaksoMockMvc.perform(
            put("/api/erikoistuva-laakari/tyoskentelyjaksot")
                .param("tyoskentelyjaksoJson", tyoskentelyjaksoJson)
                .with(csrf())
        ).andExpect(status().isOk)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
        val testTyoskentelyjakso = tyoskentelyjaksoList[tyoskentelyjaksoList.size - 1]

        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.nimi).isEqualTo(updatedTyoskentelypaikka.nimi)
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.tyyppi).isEqualTo(
            updatedTyoskentelypaikka.tyyppi
        )
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.muuTyyppi).isEqualTo(updatedTyoskentelypaikka.muuTyyppi)
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.kunta).isEqualTo(updatedTyoskentelypaikka.kunta)
        assertThat(testTyoskentelyjakso.alkamispaiva).isEqualTo(UPDATED_ALKAMISPAIVA)
        assertThat(testTyoskentelyjakso.paattymispaiva).isEqualTo(UPDATED_PAATTYMISPAIVA)
        assertThat(testTyoskentelyjakso.osaaikaprosentti).isEqualTo(UPDATED_OSAAIKAPROSENTTI)
        assertThat(testTyoskentelyjakso.kaytannonKoulutus).isEqualTo(UPDATED_KAYTANNON_KOULUTUS)
        assertThat(testTyoskentelyjakso.omaaErikoisalaaTukeva?.id).isEqualTo(omaaErikoisalaaTukeva.id)
        assertThat(testTyoskentelyjakso.hyvaksyttyAiempaanErikoisalaan)
            .isEqualTo(UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN)
    }

    @Test
    @Transactional
    fun updateTyoskentelyjaksoWithSuoritusarvioinnitAndInvalidPaattymispaiva() {
        initTest()

        assertThat(tyoskentelyjakso.suoritusarvioinnit).isEmpty()

        tyoskentelyjakso.suoritusarvioinnit.add(
            SuoritusarviointiHelper.createEntity(em, user, LocalDate.of(2020, 1, 20))
        )
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val id = tyoskentelyjakso.id
        assertNotNull(id)

        val existingTyoskentelyjakso = tyoskentelyjaksoRepository.findById(id).get()
        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(existingTyoskentelyjakso)

        // Asetetaan päättymispäivä suoritusarvioinnin ulkopuolelle
        tyoskentelyjaksoDTO.apply {
            paattymispaiva = LocalDate.of(2020, 1, 19)
        }

        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoDTO)

        restTyoskentelyjaksoMockMvc.perform(
            put("/api/erikoistuva-laakari/tyoskentelyjaksot")
                .param("tyoskentelyjaksoJson", tyoskentelyjaksoJson)
                .with(csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun updateTyoskentelyjaksoWithSuoritusarvioinnitAndValidPaattymispaiva() {
        initTest()

        assertThat(tyoskentelyjakso.suoritusarvioinnit).isEmpty()

        val suoritusarviointiTapahtumanAjankohta = LocalDate.of(2020, 1, 20)

        tyoskentelyjakso.suoritusarvioinnit.add(
            SuoritusarviointiHelper.createEntity(em, user, suoritusarviointiTapahtumanAjankohta)
        )
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val id = tyoskentelyjakso.id
        assertNotNull(id)

        val existingTyoskentelyjakso = tyoskentelyjaksoRepository.findById(id).get()
        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(existingTyoskentelyjakso)

        // Asetetaan päättymispäivä suoritusarvioinnin päivälle
        tyoskentelyjaksoDTO.apply {
            paattymispaiva = suoritusarviointiTapahtumanAjankohta
        }

        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoDTO)

        restTyoskentelyjaksoMockMvc.perform(
            put("/api/erikoistuva-laakari/tyoskentelyjaksot")
                .param("tyoskentelyjaksoJson", tyoskentelyjaksoJson)
                .with(csrf())
        ).andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.paattymispaiva").value(suoritusarviointiTapahtumanAjankohta.toString()))
    }

    @Test
    @Transactional
    fun updateTyoskentelyjaksoWithoutId() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeUpdate = tyoskentelyjaksoRepository.findAll().size

        val id = tyoskentelyjakso.id
        assertNotNull(id)
        val updatedTyoskentelyjakso = tyoskentelyjaksoRepository.findById(id).get()
        em.detach(updatedTyoskentelyjakso)

        updatedTyoskentelyjakso.id = null
        updatedTyoskentelyjakso.alkamispaiva = UPDATED_ALKAMISPAIVA
        updatedTyoskentelyjakso.paattymispaiva = UPDATED_PAATTYMISPAIVA
        updatedTyoskentelyjakso.osaaikaprosentti = UPDATED_OSAAIKAPROSENTTI
        updatedTyoskentelyjakso.kaytannonKoulutus = UPDATED_KAYTANNON_KOULUTUS
        updatedTyoskentelyjakso.hyvaksyttyAiempaanErikoisalaan =
            UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN
        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(updatedTyoskentelyjakso)
        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoDTO)

        restTyoskentelyjaksoMockMvc.perform(
            put("/api/erikoistuva-laakari/tyoskentelyjaksot")
                .param("tyoskentelyjaksoJson", tyoskentelyjaksoJson)
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun updateAnotherUserTyoskentelyjakso() {
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        initTest(erikoistuvaLaakari.kayttaja?.user?.id)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeUpdate = tyoskentelyjaksoRepository.findAll().size

        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjakso)
        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoDTO)

        restTyoskentelyjaksoMockMvc.perform(
            put("/api/erikoistuva-laakari/tyoskentelyjaksot")
                .param("tyoskentelyjaksoJson", tyoskentelyjaksoJson)
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun deleteTyoskentelyjakso() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeDelete = tyoskentelyjaksoRepository.findAll().size

        restTyoskentelyjaksoMockMvc.perform(
            delete("/api/erikoistuva-laakari/tyoskentelyjaksot/{id}", tyoskentelyjakso.id)
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf())
        ).andExpect(status().isNoContent)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(tyoskentelyjaksoTableSizeBeforeDelete - 1)
    }

    @Test
    @Transactional
    fun deleteAnotherUserTyoskentelyjakso() {
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        initTest(erikoistuvaLaakari.kayttaja?.user?.id)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeDelete = tyoskentelyjaksoRepository.findAll().size

        restTyoskentelyjaksoMockMvc.perform(
            delete("/api/erikoistuva-laakari/tyoskentelyjaksot/{id}", tyoskentelyjakso.id)
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(tyoskentelyjaksoTableSizeBeforeDelete)
    }

    @Test
    @Transactional
    fun deleteTyoskentelyjaksoWithSuoritusarviointi() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeDelete = tyoskentelyjaksoRepository.findAll().size

        em.detach(tyoskentelyjakso)

        val suoritusarviointi = SuoritusarviointiHelper.createEntity(em, user)

        suoritusarviointiRepository.saveAndFlush(suoritusarviointi)

        restTyoskentelyjaksoMockMvc.perform(
            delete("/api/erikoistuva-laakari/tyoskentelyjaksot/{id}", tyoskentelyjakso.id)
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(tyoskentelyjaksoTableSizeBeforeDelete)
    }

    @Test
    @Transactional
    fun createKeskeytysaika() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeCreate = keskeytysaikaRepository.findAll().size

        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        restKeskeytysaikaMockMvc.perform(
            post("/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO))
                .with(csrf())
        ).andExpect(status().isCreated)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeCreate + 1)
        val testKeskeytysaika = keskeytysaikaList[keskeytysaikaList.size - 1]
        assertThat(testKeskeytysaika.alkamispaiva).isEqualTo(KeskeytysaikaHelper.DEFAULT_ALKAMISPAIVA)
        assertThat(testKeskeytysaika.paattymispaiva).isEqualTo(KeskeytysaikaHelper.DEFAULT_PAATTYMISPAIVA)
        assertThat(testKeskeytysaika.poissaoloprosentti).isEqualTo(KeskeytysaikaHelper.DEFAULT_POISSAOLOPROSENTTI)
    }

    @Test
    @Transactional
    fun createKeskeytysaikaWithExistingId() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeCreate = keskeytysaikaRepository.findAll().size

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
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createKeskeytysaikaForAnotherUser() {
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        initTest(erikoistuvaLaakari.kayttaja?.user?.id)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeCreate = keskeytysaikaRepository.findAll().size

        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        restKeskeytysaikaMockMvc.perform(
            post("/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createKeskeytysaikaWithInvalidDates() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaika.alkamispaiva = LocalDate.of(2020, 1, 1)
        keskeytysaika.paattymispaiva = LocalDate.of(2020, 12, 1)

        val tyoskentelyjaksoTableSizeBeforeCreate = keskeytysaikaRepository.findAll().size

        var keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        restKeskeytysaikaMockMvc.perform(
            post("/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        var keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)

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
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)

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
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)
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

        restTyoskentelyjaksoMockMvc.perform(
            get(
                "/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot/{id}",
                id
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(keskeytysaika.id as Any))
            .andExpect(jsonPath("$.alkamispaiva").value(KeskeytysaikaHelper.DEFAULT_ALKAMISPAIVA.toString()))
            .andExpect(jsonPath("$.paattymispaiva").value(KeskeytysaikaHelper.DEFAULT_PAATTYMISPAIVA.toString()))
            .andExpect(jsonPath("$.poissaoloprosentti").value(KeskeytysaikaHelper.DEFAULT_POISSAOLOPROSENTTI))
    }

    @Test
    @Transactional
    fun getAnotherUserKeskeytysaika() {
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        initTest(erikoistuvaLaakari.kayttaja?.user?.id)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val id = keskeytysaika.id
        assertNotNull(id)

        restTyoskentelyjaksoMockMvc.perform(
            get(
                "/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot/{id}",
                id
            )
        )
            .andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun updateKeskeytysaika() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val tyoskentelyjaksoTableSizeBeforeUpdate = keskeytysaikaRepository.findAll().size

        val id = keskeytysaika.id
        assertNotNull(id)

        val updatedKeskeytysaika = keskeytysaikaRepository.findById(id).get()
        em.detach(updatedKeskeytysaika)
        updatedKeskeytysaika.alkamispaiva = KeskeytysaikaHelper.UPDATED_ALKAMISPAIVA
        updatedKeskeytysaika.paattymispaiva = KeskeytysaikaHelper.UPDATED_PAATTYMISPAIVA
        updatedKeskeytysaika.poissaoloprosentti = KeskeytysaikaHelper.UPDATED_POISSAOLOPROSENTTI
        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(updatedKeskeytysaika)

        restKeskeytysaikaMockMvc.perform(
            put("/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
        val testKeskeytysaika = keskeytysaikaList[keskeytysaikaList.size - 1]
        assertThat(testKeskeytysaika.alkamispaiva).isEqualTo(KeskeytysaikaHelper.UPDATED_ALKAMISPAIVA)
        assertThat(testKeskeytysaika.paattymispaiva).isEqualTo(KeskeytysaikaHelper.UPDATED_PAATTYMISPAIVA)
        assertThat(testKeskeytysaika.poissaoloprosentti).isEqualTo(KeskeytysaikaHelper.UPDATED_POISSAOLOPROSENTTI)
    }

    @Test
    @Transactional
    fun updateAnotherUserKeskeytysaika() {
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        initTest(erikoistuvaLaakari.kayttaja?.user?.id)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val tyoskentelyjaksoTableSizeBeforeUpdate = keskeytysaikaRepository.findAll().size

        val id = keskeytysaika.id
        assertNotNull(id)

        val updatedKeskeytysaika = keskeytysaikaRepository.findById(id).get()
        em.detach(updatedKeskeytysaika)
        updatedKeskeytysaika.alkamispaiva = KeskeytysaikaHelper.UPDATED_ALKAMISPAIVA
        updatedKeskeytysaika.paattymispaiva = KeskeytysaikaHelper.UPDATED_PAATTYMISPAIVA
        updatedKeskeytysaika.poissaoloprosentti = KeskeytysaikaHelper.UPDATED_POISSAOLOPROSENTTI
        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(updatedKeskeytysaika)

        restKeskeytysaikaMockMvc.perform(
            put("/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun updateKeskeytysaikaWithoutId() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val tyoskentelyjaksoTableSizeBeforeUpdate = keskeytysaikaRepository.findAll().size

        val id = keskeytysaika.id
        assertNotNull(id)

        val updatedKeskeytysaika = keskeytysaikaRepository.findById(id).get()
        em.detach(updatedKeskeytysaika)
        updatedKeskeytysaika.id = null
        updatedKeskeytysaika.alkamispaiva = KeskeytysaikaHelper.UPDATED_ALKAMISPAIVA
        updatedKeskeytysaika.paattymispaiva = KeskeytysaikaHelper.UPDATED_PAATTYMISPAIVA
        updatedKeskeytysaika.poissaoloprosentti = KeskeytysaikaHelper.UPDATED_POISSAOLOPROSENTTI
        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(updatedKeskeytysaika)

        restKeskeytysaikaMockMvc.perform(
            put("/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun deleteKeskeytysaika() {
        initTest()

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val keskeytysaikaTableSizeBeforeDelete = keskeytysaikaRepository.findAll().size

        restKeskeytysaikaMockMvc.perform(
            delete("/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot/{id}", keskeytysaika.id)
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf())
        ).andExpect(status().isNoContent)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(keskeytysaikaTableSizeBeforeDelete - 1)
    }

    @Test
    @Transactional
    fun deleteAnotherUserKeskeytysaika() {
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        initTest(erikoistuvaLaakari.kayttaja?.user?.id)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val keskeytysaikaTableSizeBeforeDelete = keskeytysaikaRepository.findAll().size

        restKeskeytysaikaMockMvc.perform(
            delete("/api/erikoistuva-laakari/tyoskentelyjaksot/poissaolot/{id}", keskeytysaika.id)
                .accept(MediaType.APPLICATION_JSON)
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(keskeytysaikaTableSizeBeforeDelete)
    }

    @Test
    @Transactional
    fun getKeskeytysaikaForm() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val id = tyoskentelyjakso.id
        assertNotNull(id)

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/poissaolo-lomake"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.poissaolonSyyt").value(Matchers.hasSize<Any>(2)))
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
            .andExpect(jsonPath("$.kunnat").value(Matchers.hasSize<Any>(478)))
            .andExpect(jsonPath("$.erikoisalat").value(Matchers.hasSize<Any>(60)))
    }

    @Test
    @Transactional
    fun getTyoskentelyjaksoTable() {
        initTest()

        tyoskentelyjakso.kaytannonKoulutus = KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO
        tyoskentelyjakso.tyoskentelypaikka!!.tyyppi = TyoskentelyjaksoTyyppi.TERVEYSKESKUS
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        em.detach(tyoskentelyjakso)

        val tyoskentelyjakso2 = createEntity(
            em,
            user,
            LocalDate.of(2020, 2, 1),
            LocalDate.of(2020, 2, 15)
        )
        tyoskentelyjakso2.hyvaksyttyAiempaanErikoisalaan = true
        tyoskentelyjakso2.kaytannonKoulutus =
            KaytannonKoulutusTyyppi.OMAA_ERIKOISALAA_TUKEVA_KOULUTUS
        tyoskentelyjakso2.tyoskentelypaikka!!.tyyppi =
            TyoskentelyjaksoTyyppi.YLIOPISTOLLINEN_SAIRAALA
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso2)
        em.detach(tyoskentelyjakso2)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)
        em.detach(keskeytysaika)

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot-taulukko"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.tyoskentelyjaksot").value(Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$.keskeytykset").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyaikaYhteensa").value(45.0))
            .andExpect(jsonPath("$.tilastot.arvioErikoistumiseenHyvaksyttavista").value(45.0))
            .andExpect(jsonPath("$.tilastot.arvioPuuttuvastaKoulutuksesta").value(2145.0))
            .andExpect(
                jsonPath("$.tilastot.koulutustyypit.terveyskeskusVaadittuVahintaan").value(
                    273.75
                )
            )
            .andExpect(jsonPath("$.tilastot.koulutustyypit.terveyskeskusSuoritettu").value(30.0))
            .andExpect(
                jsonPath("$.tilastot.koulutustyypit.yliopistosairaalaVaadittuVahintaan").value(
                    821.25
                )
            )
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaalaSuoritettu").value(15.0))
            .andExpect(
                jsonPath("$.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenVaadittuVahintaan").value(
                    821.25
                )
            )
            .andExpect(
                jsonPath("$.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenSuoritettu").value(
                    0.0
                )
            )
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yhteensaVaadittuVahintaan").value(2190.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yhteensaSuoritettu").value(45.0))
            .andExpect(jsonPath("$.tilastot.kaytannonKoulutus").value(Matchers.hasSize<Any>(4)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot").value(Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot[0].suoritettu").value(30.0))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot[1].suoritettu").value(15.0))
    }

    @Test
    @Transactional
    fun getTyoskentelyjaksoTableShouldReturnOnlyForOpintooikeusKaytossa() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        em.detach(tyoskentelyjakso)

        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(user.id!!)
        requireNotNull(erikoistuvaLaakari)
        val newOpintooikeus =
            OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(em, erikoistuvaLaakari)
        OpintooikeusHelper.setOpintooikeusKaytossa(erikoistuvaLaakari, newOpintooikeus)

        val tyoskentelyjaksoForAnotherOpintooikeus = createEntity(
            em,
            user,
            LocalDate.of(2020, 2, 1),
            LocalDate.of(2020, 2, 15)
        )
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjaksoForAnotherOpintooikeus)
        em.detach(tyoskentelyjaksoForAnotherOpintooikeus)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjaksoForAnotherOpintooikeus)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)
        em.detach(keskeytysaika)

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot-taulukko"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.tyoskentelyjaksot").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$.keskeytykset").value(Matchers.hasSize<Any>(1)))
            .andExpect(
                jsonPath("$.tyoskentelyjaksot[0].id").value(
                    tyoskentelyjaksoForAnotherOpintooikeus.id
                )
            )
    }

    @Test
    @Transactional
    fun getTyoskentelyjaksoTableWithTerveyskeskusMax() {
        initTest()

        val pituus = 273.75
        val opintoopas = tyoskentelyjakso.opintooikeus?.opintoopas
        opintoopas?.terveyskeskuskoulutusjaksonVahimmaispituus = pituus
        opintoopas?.terveyskeskuskoulutusjaksonMaksimipituus = pituus
        opintoopasRepository.saveAndFlush(opintoopas)

        tyoskentelyjakso.kaytannonKoulutus = KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO
        tyoskentelyjakso.tyoskentelypaikka!!.tyyppi = TyoskentelyjaksoTyyppi.TERVEYSKESKUS
        tyoskentelyjakso.paattymispaiva = tyoskentelyjakso.alkamispaiva?.plusYears(1)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val tyoskentelyjakso2 = createEntity(
            em,
            user,
            LocalDate.of(2020, 2, 1),
            LocalDate.of(2020, 2, 15)
        )
        tyoskentelyjakso2.kaytannonKoulutus =
            KaytannonKoulutusTyyppi.OMAA_ERIKOISALAA_TUKEVA_KOULUTUS
        tyoskentelyjakso2.tyoskentelypaikka!!.tyyppi =
            TyoskentelyjaksoTyyppi.YLIOPISTOLLINEN_SAIRAALA
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso2)

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot-taulukko"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.tyoskentelyjaksot").value(Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$.keskeytykset").value(Matchers.hasSize<Any>(0)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyaikaYhteensa").value(288.75))
            .andExpect(jsonPath("$.tilastot.arvioErikoistumiseenHyvaksyttavista").value(288.75))
            .andExpect(jsonPath("$.tilastot.arvioPuuttuvastaKoulutuksesta").value(1901.25))
            .andExpect(
                jsonPath("$.tilastot.koulutustyypit.terveyskeskusVaadittuVahintaan").value(
                    273.75
                )
            )
            .andExpect(jsonPath("$.tilastot.koulutustyypit.terveyskeskusSuoritettu").value(273.75))
            .andExpect(
                jsonPath("$.tilastot.koulutustyypit.yliopistosairaalaVaadittuVahintaan").value(
                    821.25
                )
            )
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaalaSuoritettu").value(15.0))
            .andExpect(
                jsonPath("$.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenVaadittuVahintaan").value(
                    821.25
                )
            )
            .andExpect(
                jsonPath("$.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenSuoritettu").value(
                    0.0
                )
            )
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yhteensaVaadittuVahintaan").value(2190.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yhteensaSuoritettu").value(288.75))
            .andExpect(jsonPath("$.tilastot.kaytannonKoulutus").value(Matchers.hasSize<Any>(4)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot").value(Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot[0].suoritettu").value(367.0))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot[1].suoritettu").value(15.0))
    }

    @Test
    @Transactional
    fun getTyoskentelyjaksoTableWithoutTerveyskeskusMax() {
        initTest()

        val opintoopas = tyoskentelyjakso.opintooikeus?.opintoopas
        opintoopas?.terveyskeskuskoulutusjaksonMaksimipituus = null
        opintoopasRepository.saveAndFlush(opintoopas)

        tyoskentelyjakso.kaytannonKoulutus = KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO
        tyoskentelyjakso.tyoskentelypaikka!!.tyyppi = TyoskentelyjaksoTyyppi.TERVEYSKESKUS
        tyoskentelyjakso.paattymispaiva = tyoskentelyjakso.alkamispaiva?.plusYears(1)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val tyoskentelyjakso2 = createEntity(
            em,
            user,
            LocalDate.of(2020, 2, 1),
            LocalDate.of(2020, 2, 15)
        )
        tyoskentelyjakso2.kaytannonKoulutus =
            KaytannonKoulutusTyyppi.OMAA_ERIKOISALAA_TUKEVA_KOULUTUS
        tyoskentelyjakso2.tyoskentelypaikka!!.tyyppi =
            TyoskentelyjaksoTyyppi.YLIOPISTOLLINEN_SAIRAALA
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso2)

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot-taulukko"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.tyoskentelyjaksot").value(Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$.keskeytykset").value(Matchers.hasSize<Any>(0)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyaikaYhteensa").value(382.0))
            .andExpect(jsonPath("$.tilastot.arvioErikoistumiseenHyvaksyttavista").value(382.0))
            .andExpect(jsonPath("$.tilastot.arvioPuuttuvastaKoulutuksesta").value(1808.0))
            .andExpect(
                jsonPath("$.tilastot.koulutustyypit.terveyskeskusVaadittuVahintaan").value(
                    273.75
                )
            )
            .andExpect(jsonPath("$.tilastot.koulutustyypit.terveyskeskusSuoritettu").value(367.0))
            .andExpect(
                jsonPath("$.tilastot.koulutustyypit.yliopistosairaalaVaadittuVahintaan").value(
                    821.25
                )
            )
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaalaSuoritettu").value(15.0))
            .andExpect(
                jsonPath("$.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenVaadittuVahintaan").value(
                    821.25
                )
            )
            .andExpect(
                jsonPath("$.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenSuoritettu").value(
                    0.0
                )
            )
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yhteensaVaadittuVahintaan").value(2190.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yhteensaSuoritettu").value(382.0))
            .andExpect(jsonPath("$.tilastot.kaytannonKoulutus").value(Matchers.hasSize<Any>(4)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot").value(Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot[0].suoritettu").value(367.0))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot[1].suoritettu").value(15.0))
    }

    @Test
    @Transactional
    fun updateLiitettyKoejaksoon() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val id = tyoskentelyjakso.id
        assertNotNull(id)

        val liitettyKoejaksoon = tyoskentelyjakso.liitettyKoejaksoon
        assertFalse(liitettyKoejaksoon)

        val tyoskentelyjaksoDTO =
            TyoskentelyjaksoDTO(id = tyoskentelyjakso.id, liitettyKoejaksoon = true)

        restTyoskentelyjaksoMockMvc.perform(
            patch("/api/erikoistuva-laakari/tyoskentelyjaksot/koejakso")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(tyoskentelyjaksoDTO))
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.liitettyKoejaksoon").value(true))
    }

    @Test
    @Transactional
    fun updateTyoskentelyjaksoAsiakirjat() {
        initTest()
        initMockFiles()

        val asiakirja = AsiakirjaHelper.createEntity(
            em,
            user,
            tyoskentelyjakso
        )
        tyoskentelyjakso.asiakirjat.add(asiakirja)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        restTyoskentelyjaksoMockMvc.perform(
            multipart("/api/erikoistuva-laakari/tyoskentelyjaksot/${tyoskentelyjakso.id}/asiakirjat")
                .file(
                    MockMultipartFile(
                        "addedFiles",
                        AsiakirjaHelper.ASIAKIRJA_PNG_NIMI,
                        AsiakirjaHelper.ASIAKIRJA_PNG_TYYPPI,
                        tempFile2.readBytes()
                    )
                )
                .param("deletedFiles", asiakirja.id!!.toString())
                .with { it.method = "PUT"; it }
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.asiakirjat").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$.asiakirjat[0].nimi").value(AsiakirjaHelper.ASIAKIRJA_PNG_NIMI))
    }

    @Test
    @Transactional
    fun getTerveyskeskuskoulutusjaksoTooShort() {
        initTest(kaytannonKoulutus = KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot/terveyskeskuskoulutusjakso"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun getTerveyskeskuskoulutusjaksoWrongKaytannonKoulutus() {
        initTest(kaytannonKoulutus = KaytannonKoulutusTyyppi.OMAN_ERIKOISALAN_KOULUTUS)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot/terveyskeskuskoulutusjakso"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun getTerveyskeskuskoulutusjaksoWithoutVastuuhenkilo() {
        initTest(kaytannonKoulutus = KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        tyoskentelyjaksoRepository.saveAndFlush(
            createEntity(
                em,
                user = user,
                paattymispaiva = DEFAULT_ALKAMISPAIVA.plusYears(1)
            )
        )

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot/terveyskeskuskoulutusjakso"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun getTerveyskeskuskoulutusjakso() {
        initTest(kaytannonKoulutus = KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        tyoskentelyjaksoRepository.saveAndFlush(
            createEntity(
                em,
                user = user,
                paattymispaiva = DEFAULT_ALKAMISPAIVA.plusYears(1),
                kaytannonKoulutus = KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO
            )
        )
        val opintooikeus = opintooikeusRepository.findAll().first()

        val vastuuhenkiloUser = userRepository.saveAndFlush(
            KayttajaResourceWithMockUserIT.createEntity(
                authority = Authority(
                    VASTUUHENKILO
                )
            )
        )
        val vastuuhenkiloKayttaja = kayttajaRepository.saveAndFlush(
            KayttajaHelper.createEntity(
                em,
                user = vastuuhenkiloUser
            )
        )
        kayttajaYliopistoErikoisalaRepository.saveAndFlush(
            KayttajaYliopistoErikoisala(
                kayttaja = vastuuhenkiloKayttaja,
                yliopisto = opintooikeus.yliopisto,
                erikoisala = Erikoisala(50),
                vastuuhenkilonTehtavat = mutableSetOf(VastuuhenkilonTehtavatyyppi(2))
            )
        )

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot/terveyskeskuskoulutusjakso"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.erikoistuvanErikoisala").value(opintooikeus.erikoisala?.nimi))
            .andExpect(jsonPath("$.erikoistuvanNimi").value(opintooikeus.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.erikoistuvanOpiskelijatunnus").value(opintooikeus.opiskelijatunnus))
            .andExpect(jsonPath("$.erikoistuvanSyntymaaika").value(opintooikeus.erikoistuvaLaakari?.syntymaaika.toString()))
            .andExpect(jsonPath("$.erikoistuvanYliopisto").value(opintooikeus.yliopisto?.nimi.toString()))
            .andExpect(jsonPath("$.laillistamispaiva").value(ErikoistuvaLaakariHelper.DEFAULT_LAILLISTAMISPAIVA.toString()))
            .andExpect(jsonPath("$.laillistamispaivanLiite").value(ErikoistuvaLaakariHelper.DEFAULT_LAILLISTAMISTODISTUS_DATA_AS_STRING))
            .andExpect(jsonPath("$.laillistamispaivanLiitteenNimi").value(ErikoistuvaLaakariHelper.DEFAULT_LAILLISTAMISTODISTUS_NIMI))
            .andExpect(jsonPath("$.laillistamispaivanLiitteenTyyppi").value(ErikoistuvaLaakariHelper.DEFAULT_LAILLISTAMISTODISTUS_TYYPPI))
            .andExpect(jsonPath("$.asetus").value(opintooikeus.asetus?.nimi))
            .andExpect(jsonPath("$.tyoskentelyjaksot").value(Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$.yleislaaketieteenVastuuhenkilonNimi").value(vastuuhenkiloKayttaja.getNimi()))
            .andExpect(jsonPath("$.yleislaaketieteenVastuuhenkilonNimike").value(vastuuhenkiloKayttaja.nimike))
    }

    @Test
    @Transactional
    fun createTerveyskeskuskoulutusjakso() {
        initTest(kaytannonKoulutus = KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO)
        initMockFiles()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        tyoskentelyjaksoRepository.saveAndFlush(
            createEntity(
                em,
                user = user,
                paattymispaiva = DEFAULT_ALKAMISPAIVA.plusYears(1),
                kaytannonKoulutus = KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO
            )
        )
        val opintooikeus = opintooikeusRepository.findAll().first()

        val vastuuhenkiloUser = userRepository.saveAndFlush(
            KayttajaResourceWithMockUserIT.createEntity(
                authority = Authority(
                    VASTUUHENKILO
                )
            )
        )
        val vastuuhenkiloKayttaja = kayttajaRepository.saveAndFlush(
            KayttajaHelper.createEntity(
                em,
                user = vastuuhenkiloUser
            )
        )
        kayttajaYliopistoErikoisalaRepository.saveAndFlush(
            KayttajaYliopistoErikoisala(
                kayttaja = vastuuhenkiloKayttaja,
                yliopisto = opintooikeus.yliopisto,
                erikoisala = Erikoisala(50),
                vastuuhenkilonTehtavat = mutableSetOf(VastuuhenkilonTehtavatyyppi(2))
            )
        )

        val laillistamispaiva = LocalDate.now()

        restTyoskentelyjaksoMockMvc.perform(
            multipart("/api/erikoistuva-laakari/tyoskentelyjaksot/terveyskeskuskoulutusjakson-hyvaksynta")
                .file(
                    MockMultipartFile(
                        "laillistamispaivanLiite",
                        AsiakirjaHelper.ASIAKIRJA_PNG_NIMI,
                        AsiakirjaHelper.ASIAKIRJA_PNG_TYYPPI,
                        tempFile2.readBytes()
                    )
                )
                .param("laillistamispaiva", laillistamispaiva.toString())
                .with { it.method = "POST"; it }
                .with(csrf())
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").exists())

        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(user.id!!)

        assertEquals(laillistamispaiva, erikoistuvaLaakari?.laillistamispaiva)
        assertThat(erikoistuvaLaakari?.laillistamispaivanLiitetiedosto).isNotNull
        assertEquals(
            AsiakirjaHelper.ASIAKIRJA_PNG_NIMI,
            erikoistuvaLaakari?.laillistamispaivanLiitetiedostonNimi
        )
        assertEquals(
            AsiakirjaHelper.ASIAKIRJA_PNG_TYYPPI,
            erikoistuvaLaakari?.laillistamispaivanLiitetiedostonTyyppi
        )
    }

    fun initTest(userId: String? = null, kaytannonKoulutus: KaytannonKoulutusTyyppi? = DEFAULT_KAYTANNON_KOULUTUS) {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>(
        )
        val authorities = listOf(SimpleGrantedAuthority(ERIKOISTUVA_LAAKARI))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(userId ?: user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        tyoskentelyjakso = createEntity(em, user, kaytannonKoulutus = kaytannonKoulutus)
    }

    fun initMockFiles() {
        tempFile1 = File.createTempFile("file", "pdf")
        tempFile1.writeBytes(AsiakirjaHelper.ASIAKIRJA_PDF_DATA)
        tempFile1.deleteOnExit()

        tempFile2 = File.createTempFile("file", "png")
        tempFile2.writeBytes(AsiakirjaHelper.ASIAKIRJA_PNG_DATA)
        tempFile2.deleteOnExit()

        mockMultipartFile1 = MockMultipartFile(
            "files",
            AsiakirjaHelper.ASIAKIRJA_PDF_NIMI,
            AsiakirjaHelper.ASIAKIRJA_PDF_TYYPPI,
            tempFile1.readBytes()
        )

        mockMultipartFile2 = MockMultipartFile(
            "files",
            AsiakirjaHelper.ASIAKIRJA_PNG_NIMI,
            AsiakirjaHelper.ASIAKIRJA_PNG_TYYPPI,
            tempFile2.readBytes()
        )
    }

    companion object {

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
        fun createEntity(
            em: EntityManager,
            user: User? = null,
            alkamispaiva: LocalDate? = DEFAULT_ALKAMISPAIVA,
            paattymispaiva: LocalDate? = DEFAULT_PAATTYMISPAIVA,
            kaytannonKoulutus: KaytannonKoulutusTyyppi? = DEFAULT_KAYTANNON_KOULUTUS
        ): Tyoskentelyjakso {
            val tyoskentelyjakso = Tyoskentelyjakso(
                alkamispaiva = alkamispaiva,
                paattymispaiva = paattymispaiva,
                osaaikaprosentti = DEFAULT_OSAAIKAPROSENTTI,
                kaytannonKoulutus = kaytannonKoulutus,
                hyvaksyttyAiempaanErikoisalaan = DEFAULT_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN
            )

            // Lisätään pakollinen tieto
            tyoskentelyjakso.tyoskentelypaikka = TyoskentelypaikkaHelper.createEntity(em)

            // Lisätään pakollinen tieto
            var erikoistuvaLaakari =
                em.findAll(ErikoistuvaLaakari::class).firstOrNull { it.kayttaja?.user == user }
            if (erikoistuvaLaakari == null) {
                erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
                em.persist(erikoistuvaLaakari)
                em.flush()
            }
            tyoskentelyjakso.opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()

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
            tyoskentelyjakso.opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()

            return tyoskentelyjakso
        }
    }
}
