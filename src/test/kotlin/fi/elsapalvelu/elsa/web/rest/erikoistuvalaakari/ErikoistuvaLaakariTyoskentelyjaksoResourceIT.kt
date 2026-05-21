package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import com.fasterxml.jackson.databind.ObjectMapper
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.*
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.*
import fi.elsapalvelu.elsa.service.dto.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.mapper.*
import fi.elsapalvelu.elsa.web.rest.ResourceIntegrationTestBase
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.*
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.*
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.*
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

private const val API_TYOSKENTELYJAKSOT = "/api/erikoistuva-laakari/tyoskentelyjaksot"

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class ErikoistuvaLaakariTyoskentelyjaksoResourceIT: ResourceIntegrationTestBase() {

    @Autowired private lateinit var tyoskentelyjaksoRepository: TyoskentelyjaksoRepository
    @Autowired private lateinit var suoritusarviointiRepository: SuoritusarviointiRepository
    @Autowired private lateinit var keskeytysaikaRepository: KeskeytysaikaRepository
    @Autowired private lateinit var erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository
    @Autowired private lateinit var userRepository: UserRepository
    @Autowired private lateinit var kayttajaRepository: KayttajaRepository
    @Autowired private lateinit var kayttajaYliopistoErikoisalaRepository: KayttajaYliopistoErikoisalaRepository
    @Autowired private lateinit var opintooikeusRepository: OpintooikeusRepository
    @Autowired private lateinit var opintoopasRepository: OpintoopasRepository
    @Autowired private lateinit var tyoskentelyjaksoMapper: TyoskentelyjaksoMapper
    @Autowired private lateinit var kuntaMapper: KuntaMapper
    @Autowired private lateinit var erikoisalaMapper: ErikoisalaMapper
    @Autowired private lateinit var keskeytysaikaMapper: KeskeytysaikaMapper
    @Autowired private lateinit var restTyoskentelyjaksoMockMvc: MockMvc
    @Autowired private lateinit var restKeskeytysaikaMockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    private lateinit var tyoskentelyjakso: Tyoskentelyjakso
    private lateinit var keskeytysaika: Keskeytysaika
    private lateinit var tempFile1: File
    private lateinit var tempFile2: File
    private lateinit var mockMultipartFile1: MockMultipartFile
    private lateinit var mockMultipartFile2: MockMultipartFile
    private lateinit var user: User

    @BeforeEach
    fun setup() {
        // Lisätään voimassaoleva poissaolon syy ja päättymistä ei määritetty
        em.persist(PoissaolonSyyHelper.createEntity(LocalDate.ofEpochDay(0L), null))
        // Lisätään voimassaoleva poissaolon syy ja päättyminen määritetty
        em.persist(PoissaolonSyyHelper.createEntity(LocalDate.ofEpochDay(0L), LocalDate.ofEpochDay(20L)))
        // Lisätään poissaolon syy, jonka voimassaolo ei ole alkanut vielä
        em.persist(PoissaolonSyyHelper.createEntity(LocalDate.ofEpochDay(15L), LocalDate.ofEpochDay(20L)))
        // Lisätään poissaolon syy, jonka voimassaolo on jo päättynyt
        persistAndFlush(PoissaolonSyyHelper.createEntity(LocalDate.ofEpochDay(0L), LocalDate.ofEpochDay(5L)))
    }

    @Test
    fun createTyoskentelyjakso() {
        initTest()
        initMockFiles()

        val tyoskentelyjaksoTableSizeBeforeCreate = tyoskentelyjaksoRepository.findAll().size

        restTyoskentelyjaksoMockMvc.perform(multipart(API_TYOSKENTELYJAKSOT).file(mockMultipartFile1).file(mockMultipartFile2)
                .param("tyoskentelyjaksoJson", objectMapper.writeValueAsString(tyoskentelyjaksoMapper.toDto(tyoskentelyjakso))).with(csrf())).andExpect(status().isCreated)

        val kirjautunutErikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(user.id!!)
        requireNotNull(kirjautunutErikoistuvaLaakari)
        val defaultTyoskentelypaikka = TyoskentelypaikkaHelper.createEntity(em)
        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(tyoskentelyjaksoTableSizeBeforeCreate + 1)
        val testTyoskentelyjakso = tyoskentelyjaksoList[tyoskentelyjaksoList.size - 1]
        assertThat(testTyoskentelyjakso.opintooikeus?.id).isEqualTo(kirjautunutErikoistuvaLaakari.getOpintooikeusKaytossa()?.id)
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.nimi).isEqualTo(defaultTyoskentelypaikka.nimi)
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.tyyppi).isEqualTo(defaultTyoskentelypaikka.tyyppi)
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.muuTyyppi).isNull()
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.kunta).isEqualTo(defaultTyoskentelypaikka.kunta)
        assertThat(testTyoskentelyjakso.alkamispaiva).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_ALKAMISPAIVA)
        assertThat(testTyoskentelyjakso.paattymispaiva).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_PAATTYMISPAIVA)
        assertThat(testTyoskentelyjakso.osaaikaprosentti).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_OSAAIKAPROSENTTI)
        assertThat(testTyoskentelyjakso.kaytannonKoulutus).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_KAYTANNON_KOULUTUS)
        assertThat(testTyoskentelyjakso.omaaErikoisalaaTukeva).isNull()
        assertThat(testTyoskentelyjakso.hyvaksyttyAiempaanErikoisalaan).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN)
        assertThat(testTyoskentelyjakso.asiakirjat).hasSize(2)

        val testAsiakirja = testTyoskentelyjakso.asiakirjat.first()
        assertThat(testAsiakirja.id).isNotNull
        assertThat(testAsiakirja.nimi).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PDF_NIMI)
        assertThat(testAsiakirja.lisattypvm?.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(testAsiakirja.tyyppi).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PDF_TYYPPI)
        assertThat(testAsiakirja.asiakirjaData?.data).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PDF_DATA)

        val testAsiakirja2 = testTyoskentelyjakso.asiakirjat.last()
        assertThat(testAsiakirja2.id).isNotNull
        assertThat(testAsiakirja2.nimi).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PNG_NIMI)
        assertThat(testAsiakirja2.lisattypvm?.toLocalDate()).isEqualTo(LocalDate.now())
        assertThat(testAsiakirja2.tyyppi).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PNG_TYYPPI)
        assertThat(testAsiakirja2.asiakirjaData?.data).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PNG_DATA)
    }

    @Test
    fun createTyoskentelyjaksoWithExistingId() {
        initTest()

        val tyoskentelyjaksoTableSizeBeforeCreate = tyoskentelyjaksoRepository.findAll().size

        tyoskentelyjakso.id = 1L
        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoMapper.toDto(tyoskentelyjakso))

        restTyoskentelyjaksoMockMvc.perform(multipart(API_TYOSKENTELYJAKSOT).param("tyoskentelyjaksoJson", tyoskentelyjaksoJson).with(csrf())).andExpect(status().isBadRequest)

        assertThat(tyoskentelyjaksoRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)
    }

    @Test
    fun createTyoskentelyjaksoWithExistingTyoskentelypaikkaId() {
        initTest()

        val tyoskentelyjaksoTableSizeBeforeCreate = tyoskentelyjaksoRepository.findAll().size

        tyoskentelyjakso.tyoskentelypaikka!!.id = 1L
        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoMapper.toDto(tyoskentelyjakso))

        restTyoskentelyjaksoMockMvc.perform(multipart(API_TYOSKENTELYJAKSOT).param("tyoskentelyjaksoJson", tyoskentelyjaksoJson).with(csrf())).andExpect(status().isBadRequest)

        assertThat(tyoskentelyjaksoRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)
    }

    @Test
    fun createTyoskentelyjaksoWithInvalidDates() {
        initTest()

        val tyoskentelyjaksoTableSizeBeforeCreate = tyoskentelyjaksoRepository.findAll().size

        tyoskentelyjakso.alkamispaiva = LocalDate.of(2020, 1, 25)
        tyoskentelyjakso.paattymispaiva = LocalDate.of(2020, 1, 5)
        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoMapper.toDto(tyoskentelyjakso))

        restTyoskentelyjaksoMockMvc.perform(multipart(API_TYOSKENTELYJAKSOT).param("tyoskentelyjaksoJson", tyoskentelyjaksoJson).with(csrf())).andExpect(status().isBadRequest)

        assertThat(tyoskentelyjaksoRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)
    }

    @Test
    fun getTyoskentelyjakso() {
        initTest()

        tyoskentelyjakso.asiakirjat.add(AsiakirjaHelper.createEntity(em, user, tyoskentelyjakso))
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        assertNotNull(tyoskentelyjakso.id)

        restTyoskentelyjaksoMockMvc.perform(get("$API_TYOSKENTELYJAKSOT/{id}", tyoskentelyjakso.id)).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(tyoskentelyjakso.id as Any))
            .andExpect(jsonPath("$.alkamispaiva").value(ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_ALKAMISPAIVA.toString()))
            .andExpect(jsonPath("$.paattymispaiva").value(ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_PAATTYMISPAIVA.toString()))
            .andExpect(jsonPath("$.osaaikaprosentti").value(ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_OSAAIKAPROSENTTI))
            .andExpect(jsonPath("$.kaytannonKoulutus").value(ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_KAYTANNON_KOULUTUS.toString()))
            .andExpect(jsonPath("$.hyvaksyttyAiempaanErikoisalaan").value(ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN))
            .andExpect(jsonPath("$.asiakirjat").value(Matchers.hasSize<Any>(1))).andExpect(jsonPath("$.asiakirjat[0].id").exists())
            .andExpect(jsonPath("$.asiakirjat[0].nimi").value(AsiakirjaHelper.ASIAKIRJA_PDF_NIMI))
            .andExpect(jsonPath("$.asiakirjat[0].lisattypvm").value(Matchers.containsString(LocalDate.now().toString())))
            .andExpect(jsonPath("$.asiakirjat[0].tyyppi").value(AsiakirjaHelper.ASIAKIRJA_PDF_TYYPPI))
            .andExpect(jsonPath("$.asiakirjat[0].asiakirjaData.fileInputStream").doesNotExist()).andExpect(jsonPath("$.asiakirjat[0].asiakirjaData.fileSize").doesNotExist())
    }

    @Test
    fun getAnotherUserTyoskentelyjakso() {
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        initTest(erikoistuvaLaakari.kayttaja?.user?.id)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        assertNotNull(tyoskentelyjakso.id)
        restTyoskentelyjaksoMockMvc.perform(get("$API_TYOSKENTELYJAKSOT/{id}", tyoskentelyjakso.id)).andExpect(status().isNotFound)
    }

    @Test
    fun updateTyoskentelyjaksoWithoutSuoritusarvioinnit() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeUpdate = tyoskentelyjaksoRepository.findAll().size

        val id = tyoskentelyjakso.id
        assertNotNull(id)
        val existingTyoskentelyjakso = tyoskentelyjaksoRepository.findById(id).get()
        val updatedTyoskentelypaikka = TyoskentelypaikkaHelper.createUpdatedEntity(em)
        val omaaErikoisalaaTukeva = em.findAll(Erikoisala::class).first()

        val tyoskentelyjaksoDTO = populateTyoskentelyjaksoDto(existingTyoskentelyjakso, updatedTyoskentelypaikka, omaaErikoisalaaTukeva)

        restTyoskentelyjaksoMockMvc.perform(put(API_TYOSKENTELYJAKSOT).param("tyoskentelyjaksoJson", objectMapper.writeValueAsString(tyoskentelyjaksoDTO))
                .with(csrf())).andExpect(status().isOk)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
        val testTyoskentelyjakso = tyoskentelyjaksoList[tyoskentelyjaksoList.size - 1]

        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.nimi).isEqualTo(updatedTyoskentelypaikka.nimi)
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.tyyppi).isEqualTo(updatedTyoskentelypaikka.tyyppi)
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.muuTyyppi).isEqualTo(updatedTyoskentelypaikka.muuTyyppi)
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.kunta).isEqualTo(updatedTyoskentelypaikka.kunta)
        assertThat(testTyoskentelyjakso.alkamispaiva).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_ALKAMISPAIVA)
        assertThat(testTyoskentelyjakso.paattymispaiva).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_PAATTYMISPAIVA)
        assertThat(testTyoskentelyjakso.osaaikaprosentti).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_OSAAIKAPROSENTTI)
        assertThat(testTyoskentelyjakso.kaytannonKoulutus).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_KAYTANNON_KOULUTUS)
        assertThat(testTyoskentelyjakso.omaaErikoisalaaTukeva).isEqualTo(omaaErikoisalaaTukeva)
        assertThat(testTyoskentelyjakso.hyvaksyttyAiempaanErikoisalaan).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN)
    }

    @Test
    fun updateTyoskentelyjaksoWithSuoritusarvioinnitShouldUpdateOnlyPaattymispaiva() {
        initTest()

        assertThat(tyoskentelyjakso.suoritusarvioinnit).isEmpty()

        tyoskentelyjakso.suoritusarvioinnit.add(SuoritusarviointiHelper.createEntity(em, user, ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_ALKAMISPAIVA.plusDays(1)))
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeUpdate = tyoskentelyjaksoRepository.findAll().size

        assertNotNull(tyoskentelyjakso.id)

        val existingTyoskentelyjakso = tyoskentelyjaksoRepository.findById(tyoskentelyjakso.id).get()
        val updatedTyoskentelypaikka = TyoskentelypaikkaHelper.createUpdatedEntity(em)
        val omaaErikoisalaaTukeva = em.findAll(Erikoisala::class).first()

        val tyoskentelyjaksoDTO = populateTyoskentelyjaksoDto(existingTyoskentelyjakso, updatedTyoskentelypaikka, omaaErikoisalaaTukeva)

        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoDTO)
        restTyoskentelyjaksoMockMvc.perform(put(API_TYOSKENTELYJAKSOT).param("tyoskentelyjaksoJson", tyoskentelyjaksoJson).with(csrf())).andExpect(status().isOk)

        val tyoskentelyjaksoList = tyoskentelyjaksoRepository.findAll()
        assertThat(tyoskentelyjaksoList).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
        val testTyoskentelyjakso = tyoskentelyjaksoList[tyoskentelyjaksoList.size - 1]

        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.nimi).isEqualTo(updatedTyoskentelypaikka.nimi)
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.tyyppi).isEqualTo(updatedTyoskentelypaikka.tyyppi)
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.muuTyyppi).isEqualTo(updatedTyoskentelypaikka.muuTyyppi)
        assertThat(testTyoskentelyjakso.tyoskentelypaikka?.kunta).isEqualTo(updatedTyoskentelypaikka.kunta)
        assertThat(testTyoskentelyjakso.alkamispaiva).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_ALKAMISPAIVA)
        assertThat(testTyoskentelyjakso.paattymispaiva).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_PAATTYMISPAIVA)
        assertThat(testTyoskentelyjakso.osaaikaprosentti).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_OSAAIKAPROSENTTI)
        assertThat(testTyoskentelyjakso.kaytannonKoulutus).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_KAYTANNON_KOULUTUS)
        assertThat(testTyoskentelyjakso.omaaErikoisalaaTukeva?.id).isEqualTo(omaaErikoisalaaTukeva.id)
        assertThat(testTyoskentelyjakso.hyvaksyttyAiempaanErikoisalaan).isEqualTo(ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN)
    }

    private fun populateTyoskentelyjaksoDto(existingTyoskentelyjakso: Tyoskentelyjakso, updatedTyosklypaikka: Tyoskentelypaikka, omaaErikoisala: Erikoisala): TyoskentelyjaksoDTO {
        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(existingTyoskentelyjakso)
        tyoskentelyjaksoDTO.apply {
            tyoskentelypaikka?.nimi = updatedTyosklypaikka.nimi
            tyoskentelypaikka?.tyyppi = updatedTyosklypaikka.tyyppi
            tyoskentelypaikka?.muuTyyppi = updatedTyosklypaikka.muuTyyppi
            tyoskentelypaikka?.kunta = kuntaMapper.toDto(updatedTyosklypaikka.kunta!!)
            alkamispaiva = ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_ALKAMISPAIVA
            paattymispaiva = ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_PAATTYMISPAIVA
            osaaikaprosentti = ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_OSAAIKAPROSENTTI
            kaytannonKoulutus = ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_KAYTANNON_KOULUTUS
            this.omaaErikoisalaaTukeva = erikoisalaMapper.toDto(omaaErikoisala)
            omaaErikoisalaaTukevaId = omaaErikoisala.id
            hyvaksyttyAiempaanErikoisalaan = ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN
        }
        return tyoskentelyjaksoDTO
    }

    @Test
    fun updateTyoskentelyjaksoWithSuoritusarvioinnitAndInvalidPaattymispaiva() {
        initTest()

        assertThat(tyoskentelyjakso.suoritusarvioinnit).isEmpty()
        tyoskentelyjakso.suoritusarvioinnit.add(SuoritusarviointiHelper.createEntity(em, user, LocalDate.of(2020, 1, 20)))
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        assertNotNull(tyoskentelyjakso.id)

        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjaksoRepository.findById(tyoskentelyjakso.id).get())

        // Asetetaan päättymispäivä suoritusarvioinnin ulkopuolelle
        tyoskentelyjaksoDTO.apply { paattymispaiva = LocalDate.of(2020, 1, 19) }

        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoDTO)

        restTyoskentelyjaksoMockMvc.perform(put(API_TYOSKENTELYJAKSOT).param("tyoskentelyjaksoJson", tyoskentelyjaksoJson).with(csrf())).andExpect(status().isBadRequest)
    }

    @Test
    fun updateTyoskentelyjaksoWithSuoritusarvioinnitAndValidPaattymispaiva() {
        initTest()

        assertThat(tyoskentelyjakso.suoritusarvioinnit).isEmpty()

        val suoritusarviointiTapahtumanAjankohta = LocalDate.of(2020, 1, 20)

        tyoskentelyjakso.suoritusarvioinnit.add(SuoritusarviointiHelper.createEntity(em, user, suoritusarviointiTapahtumanAjankohta))
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        assertNotNull(tyoskentelyjakso.id)

        val tyoskentelyjaksoDTO = tyoskentelyjaksoMapper.toDto(tyoskentelyjaksoRepository.findById(tyoskentelyjakso.id).get())

        // Asetetaan päättymispäivä suoritusarvioinnin päivälle
        tyoskentelyjaksoDTO.apply { paattymispaiva = suoritusarviointiTapahtumanAjankohta }

        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoDTO)

        restTyoskentelyjaksoMockMvc.perform(put(API_TYOSKENTELYJAKSOT).param("tyoskentelyjaksoJson", tyoskentelyjaksoJson).with(csrf())).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.paattymispaiva").value(suoritusarviointiTapahtumanAjankohta.toString()))
    }

    @Test
    fun updateTyoskentelyjaksoWithoutId() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeUpdate = tyoskentelyjaksoRepository.findAll().size

        assertNotNull(tyoskentelyjakso.id)
        val updatedTyoskentelyjakso = tyoskentelyjaksoRepository.findById(tyoskentelyjakso.id).get()
        em.detach(updatedTyoskentelyjakso)

        updatedTyoskentelyjakso.id = null
        updatedTyoskentelyjakso.alkamispaiva = ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_ALKAMISPAIVA
        updatedTyoskentelyjakso.paattymispaiva = ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_PAATTYMISPAIVA
        updatedTyoskentelyjakso.osaaikaprosentti = ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_OSAAIKAPROSENTTI
        updatedTyoskentelyjakso.kaytannonKoulutus = ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_KAYTANNON_KOULUTUS
        updatedTyoskentelyjakso.hyvaksyttyAiempaanErikoisalaan = ErikoistuvaLaakariTyoskentelyjaksoHelper.UPDATED_HYVAKSYTTY_AIEMPAAN_ERIKOISALAAN
        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoMapper.toDto(updatedTyoskentelyjakso))

        restTyoskentelyjaksoMockMvc.perform(put(API_TYOSKENTELYJAKSOT).param("tyoskentelyjaksoJson", tyoskentelyjaksoJson).with(csrf())).andExpect(status().isBadRequest)

        assertThat(tyoskentelyjaksoRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
    }

    @Test
    fun updateAnotherUserTyoskentelyjakso() {
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        initTest(erikoistuvaLaakari.kayttaja?.user?.id)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        val tyoskentelyjaksoTableSizeBeforeUpdate = tyoskentelyjaksoRepository.findAll().size
        val tyoskentelyjaksoJson = objectMapper.writeValueAsString(tyoskentelyjaksoMapper.toDto(tyoskentelyjakso))

        restTyoskentelyjaksoMockMvc.perform(put(API_TYOSKENTELYJAKSOT).param("tyoskentelyjaksoJson", tyoskentelyjaksoJson).with(csrf())).andExpect(status().isBadRequest)

        assertThat(tyoskentelyjaksoRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
    }

    @Test
    fun deleteTyoskentelyjakso() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        val tyoskentelyjaksoTableSizeBeforeDelete = tyoskentelyjaksoRepository.findAll().size

        restTyoskentelyjaksoMockMvc.perform(delete("$API_TYOSKENTELYJAKSOT/{id}", tyoskentelyjakso.id).accept(APPLICATION_JSON).with(csrf())).andExpect(status().isNoContent)
        assertThat(tyoskentelyjaksoRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeDelete - 1)
    }

    @Test
    fun deleteAnotherUserTyoskentelyjakso() {
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        initTest(erikoistuvaLaakari.kayttaja?.user?.id)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeDelete = tyoskentelyjaksoRepository.findAll().size

        restTyoskentelyjaksoMockMvc.perform(delete("$API_TYOSKENTELYJAKSOT/{id}", tyoskentelyjakso.id).accept(APPLICATION_JSON)
                .with(csrf())).andExpect(status().isBadRequest)

        assertThat(tyoskentelyjaksoRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeDelete)
    }

    @Test
    fun deleteTyoskentelyjaksoWithSuoritusarviointi() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeDelete = tyoskentelyjaksoRepository.findAll().size

        em.detach(tyoskentelyjakso)
        suoritusarviointiRepository.saveAndFlush(SuoritusarviointiHelper.createEntity(em, user))

        restTyoskentelyjaksoMockMvc.perform(delete("$API_TYOSKENTELYJAKSOT/{id}", tyoskentelyjakso.id).accept(APPLICATION_JSON).with(csrf())).andExpect(status().isBadRequest)

        assertThat(tyoskentelyjaksoRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeDelete)
    }

    @Test
    fun createKeskeytysaika() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeCreate = keskeytysaikaRepository.findAll().size

        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        restKeskeytysaikaMockMvc.perform(post("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isCreated)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeCreate + 1)
        val testKeskeytysaika = keskeytysaikaList[keskeytysaikaList.size - 1]
        assertThat(testKeskeytysaika.alkamispaiva).isEqualTo(KeskeytysaikaHelper.DEFAULT_ALKAMISPAIVA)
        assertThat(testKeskeytysaika.paattymispaiva).isEqualTo(KeskeytysaikaHelper.DEFAULT_PAATTYMISPAIVA)
        assertThat(testKeskeytysaika.poissaoloprosentti).isEqualTo(KeskeytysaikaHelper.DEFAULT_POISSAOLOPROSENTTI)
    }

    @Test
    fun createKeskeytysaikaWithExistingId() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeCreate = keskeytysaikaRepository.findAll().size

        keskeytysaika.id = 1L
        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        restKeskeytysaikaMockMvc.perform(post("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isBadRequest)

        assertThat(keskeytysaikaRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)
    }

    @Test
    fun createKeskeytysaikaForAnotherUser() {
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        initTest(erikoistuvaLaakari.kayttaja?.user?.id)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)

        val tyoskentelyjaksoTableSizeBeforeCreate = keskeytysaikaRepository.findAll().size

        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        restKeskeytysaikaMockMvc.perform(post("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isBadRequest)

        assertThat(keskeytysaikaRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)
    }

    @Test
    fun createKeskeytysaikaWithInvalidDates() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaika.alkamispaiva = LocalDate.of(2020, 1, 1)
        keskeytysaika.paattymispaiva = LocalDate.of(2020, 12, 1)

        val tyoskentelyjaksoTableSizeBeforeCreate = keskeytysaikaRepository.findAll().size

        var keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        restKeskeytysaikaMockMvc.perform(post("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isBadRequest)

        assertThat(keskeytysaikaRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)

        keskeytysaika.alkamispaiva = LocalDate.of(2019, 12, 1)
        keskeytysaika.paattymispaiva = LocalDate.of(2020, 1, 10)

        keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        restKeskeytysaikaMockMvc.perform(post("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isBadRequest)

        assertThat(keskeytysaikaRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)

        keskeytysaika.alkamispaiva = LocalDate.of(2020, 1, 15)
        keskeytysaika.paattymispaiva = LocalDate.of(2020, 1, 10)

        keskeytysaikaDTO = keskeytysaikaMapper.toDto(keskeytysaika)
        keskeytysaikaDTO.tyoskentelyjaksoId = tyoskentelyjakso.id
        restKeskeytysaikaMockMvc.perform(post("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isBadRequest)

        assertThat(keskeytysaikaRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeCreate)
    }

    @Test
    fun getKeskeytysaika() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        assertNotNull(keskeytysaika.id)

        restTyoskentelyjaksoMockMvc.perform(get("$API_TYOSKENTELYJAKSOT/poissaolot/{id}", keskeytysaika.id)).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.id").value(keskeytysaika.id as Any))
            .andExpect(jsonPath("$.alkamispaiva").value(KeskeytysaikaHelper.DEFAULT_ALKAMISPAIVA.toString()))
            .andExpect(jsonPath("$.paattymispaiva").value(KeskeytysaikaHelper.DEFAULT_PAATTYMISPAIVA.toString()))
            .andExpect(jsonPath("$.poissaoloprosentti").value(KeskeytysaikaHelper.DEFAULT_POISSAOLOPROSENTTI))
    }

    @Test
    fun getAnotherUserKeskeytysaika() {
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        initTest(erikoistuvaLaakari.kayttaja?.user?.id)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        assertNotNull(keskeytysaika.id)

        restTyoskentelyjaksoMockMvc.perform(get("$API_TYOSKENTELYJAKSOT/poissaolot/{id}", keskeytysaika.id)).andExpect(status().isNotFound)
    }

    @Test
    fun updateKeskeytysaika() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val tyoskentelyjaksoTableSizeBeforeUpdate = keskeytysaikaRepository.findAll().size

        assertNotNull(keskeytysaika.id)

        val updatedKeskeytysaika = keskeytysaikaRepository.findById(keskeytysaika.id).get()
        em.detach(updatedKeskeytysaika)
        updatedKeskeytysaika.alkamispaiva = KeskeytysaikaHelper.UPDATED_ALKAMISPAIVA
        updatedKeskeytysaika.paattymispaiva = KeskeytysaikaHelper.UPDATED_PAATTYMISPAIVA
        updatedKeskeytysaika.poissaoloprosentti = KeskeytysaikaHelper.UPDATED_POISSAOLOPROSENTTI
        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(updatedKeskeytysaika)

        restKeskeytysaikaMockMvc.perform(put("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isOk)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
        val testKeskeytysaika = keskeytysaikaList[keskeytysaikaList.size - 1]
        assertThat(testKeskeytysaika.alkamispaiva).isEqualTo(KeskeytysaikaHelper.UPDATED_ALKAMISPAIVA)
        assertThat(testKeskeytysaika.paattymispaiva).isEqualTo(KeskeytysaikaHelper.UPDATED_PAATTYMISPAIVA)
        assertThat(testKeskeytysaika.poissaoloprosentti).isEqualTo(KeskeytysaikaHelper.UPDATED_POISSAOLOPROSENTTI)
    }

    @Test
    fun updateAnotherUserKeskeytysaika() {
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        initTest(erikoistuvaLaakari.kayttaja?.user?.id)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val tyoskentelyjaksoTableSizeBeforeUpdate = keskeytysaikaRepository.findAll().size

        assertNotNull(keskeytysaika.id)

        val updatedKeskeytysaika = keskeytysaikaRepository.findById(keskeytysaika.id).get()
        em.detach(updatedKeskeytysaika)
        updatedKeskeytysaika.alkamispaiva = KeskeytysaikaHelper.UPDATED_ALKAMISPAIVA
        updatedKeskeytysaika.paattymispaiva = KeskeytysaikaHelper.UPDATED_PAATTYMISPAIVA
        updatedKeskeytysaika.poissaoloprosentti = KeskeytysaikaHelper.UPDATED_POISSAOLOPROSENTTI
        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(updatedKeskeytysaika)

        restKeskeytysaikaMockMvc.perform(put("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isBadRequest)

        assertThat(keskeytysaikaRepository.findAll()).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
    }

    @Test
    fun updateKeskeytysaikaWithoutId() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val tyoskentelyjaksoTableSizeBeforeUpdate = keskeytysaikaRepository.findAll().size

        assertNotNull(keskeytysaika.id)

        val updatedKeskeytysaika = keskeytysaikaRepository.findById(keskeytysaika.id).get()
        em.detach(updatedKeskeytysaika)
        updatedKeskeytysaika.id = null
        updatedKeskeytysaika.alkamispaiva = KeskeytysaikaHelper.UPDATED_ALKAMISPAIVA
        updatedKeskeytysaika.paattymispaiva = KeskeytysaikaHelper.UPDATED_PAATTYMISPAIVA
        updatedKeskeytysaika.poissaoloprosentti = KeskeytysaikaHelper.UPDATED_POISSAOLOPROSENTTI
        val keskeytysaikaDTO = keskeytysaikaMapper.toDto(updatedKeskeytysaika)

        restKeskeytysaikaMockMvc.perform(put("$API_TYOSKENTELYJAKSOT/poissaolot").contentType(APPLICATION_JSON)
            .content(convertObjectToJsonBytes(keskeytysaikaDTO)).with(csrf())).andExpect(status().isBadRequest)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(tyoskentelyjaksoTableSizeBeforeUpdate)
    }

    @Test
    fun deleteKeskeytysaika() {
        initTest()

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val keskeytysaikaTableSizeBeforeDelete = keskeytysaikaRepository.findAll().size

        restKeskeytysaikaMockMvc.perform(delete("$API_TYOSKENTELYJAKSOT/poissaolot/{id}", keskeytysaika.id).accept(APPLICATION_JSON).with(csrf())).andExpect(status().isNoContent)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(keskeytysaikaTableSizeBeforeDelete - 1)
    }

    @Test
    fun deleteAnotherUserKeskeytysaika() {
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        erikoistuvaLaakariRepository.saveAndFlush(erikoistuvaLaakari)

        initTest(erikoistuvaLaakari.kayttaja?.user?.id)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)

        val keskeytysaikaTableSizeBeforeDelete = keskeytysaikaRepository.findAll().size

        restKeskeytysaikaMockMvc.perform(delete("$API_TYOSKENTELYJAKSOT/poissaolot/{id}", keskeytysaika.id).accept(APPLICATION_JSON).with(csrf())).andExpect(status().isBadRequest)

        val keskeytysaikaList = keskeytysaikaRepository.findAll()
        assertThat(keskeytysaikaList).hasSize(keskeytysaikaTableSizeBeforeDelete)
    }

    @Test
    fun getKeskeytysaikaForm() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        assertNotNull(tyoskentelyjakso.id)

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/poissaolo-lomake")).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.poissaolonSyyt").value(Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$.tyoskentelyjaksot").value(Matchers.hasSize<Any>(1))).andExpect(jsonPath("$.tyoskentelyjaksot[0].id").value(tyoskentelyjakso.id as Any))
    }

    @Test
    fun getTyoskentelyjaksoForm() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        assertNotNull(tyoskentelyjakso.id)

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjakso-lomake")).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.kunnat").value(Matchers.hasSize<Any>(478)))
            .andExpect(jsonPath("$.erikoisalat").value(Matchers.hasSize<Any>(61)))
    }

    @Test
    fun getTyoskentelyjaksoTable() {
        initTest()

        tyoskentelyjakso.kaytannonKoulutus = KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO
        tyoskentelyjakso.tyoskentelypaikka!!.tyyppi = TyoskentelyjaksoTyyppi.TERVEYSKESKUS
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        em.detach(tyoskentelyjakso)

        val tyoskentelyjakso2 = ErikoistuvaLaakariTyoskentelyjaksoHelper.createEntity(em, user, LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 15))
        tyoskentelyjakso2.hyvaksyttyAiempaanErikoisalaan = true
        tyoskentelyjakso2.kaytannonKoulutus = KaytannonKoulutusTyyppi.OMAA_ERIKOISALAA_TUKEVA_KOULUTUS
        tyoskentelyjakso2.tyoskentelypaikka!!.tyyppi = TyoskentelyjaksoTyyppi.YLIOPISTOLLINEN_SAIRAALA
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso2)
        em.detach(tyoskentelyjakso2)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjakso)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)
        em.detach(keskeytysaika)

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot-taulukko")).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.tyoskentelyjaksot").value(Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$.keskeytykset").value(Matchers.hasSize<Any>(1))).andExpect(jsonPath("$.tilastot.tyoskentelyaikaYhteensa").value(45.0))
            .andExpect(jsonPath("$.tilastot.arvioErikoistumiseenHyvaksyttavista").value(45.0)).andExpect(jsonPath("$.tilastot.arvioPuuttuvastaKoulutuksesta").value(2145.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.terveyskeskusVaadittuVahintaan").value(273.75))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.terveyskeskusSuoritettu").value(30.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaalaVaadittuVahintaan").value(821.25))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaalaSuoritettu").value(15.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenVaadittuVahintaan").value(821.25))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenSuoritettu").value(0.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yhteensaVaadittuVahintaan").value(2190.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yhteensaSuoritettu").value(45.0)).andExpect(jsonPath("$.tilastot.kaytannonKoulutus").value(Matchers.hasSize<Any>(4)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot").value(Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot[0].suoritettu").value(30.0)).andExpect(jsonPath("$.tilastot.tyoskentelyjaksot[1].suoritettu").value(15.0))
    }

    @Test
    fun getTyoskentelyjaksoTableShouldReturnOnlyForOpintooikeusKaytossa() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        em.detach(tyoskentelyjakso)

        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(user.id!!)
        requireNotNull(erikoistuvaLaakari)
        OpintooikeusHelper.setOpintooikeusKaytossa(erikoistuvaLaakari, OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(em, erikoistuvaLaakari))

        val tyoskentelyjaksoForOtherOpintooikeus = ErikoistuvaLaakariTyoskentelyjaksoHelper.createEntity(em, user, LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 15))
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjaksoForOtherOpintooikeus)
        em.detach(tyoskentelyjaksoForOtherOpintooikeus)

        keskeytysaika = KeskeytysaikaHelper.createEntity(em, tyoskentelyjaksoForOtherOpintooikeus)
        keskeytysaikaRepository.saveAndFlush(keskeytysaika)
        em.detach(keskeytysaika)

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot-taulukko")).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.tyoskentelyjaksot").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$.keskeytykset").value(Matchers.hasSize<Any>(1))).andExpect(jsonPath("$.tyoskentelyjaksot[0].id").value(tyoskentelyjaksoForOtherOpintooikeus.id))
    }

    @Test
    fun getTyoskentelyjaksoTableWithTerveyskeskusMax() {
        initTest()

        val opintoopas = tyoskentelyjakso.opintooikeus?.opintoopas
        opintoopas?.let {
            it.terveyskeskuskoulutusjaksonVahimmaispituus = 273.75
            it.terveyskeskuskoulutusjaksonMaksimipituus = 273.75
            opintoopasRepository.saveAndFlush(it)
        }

        tyoskentelyjakso.kaytannonKoulutus = KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO
        tyoskentelyjakso.tyoskentelypaikka!!.tyyppi = TyoskentelyjaksoTyyppi.TERVEYSKESKUS
        tyoskentelyjakso.paattymispaiva = tyoskentelyjakso.alkamispaiva?.plusYears(1)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val tyoskentelyjakso2 = ErikoistuvaLaakariTyoskentelyjaksoHelper.createEntity(em, user, LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 15))
        tyoskentelyjakso2.kaytannonKoulutus = KaytannonKoulutusTyyppi.OMAA_ERIKOISALAA_TUKEVA_KOULUTUS
        tyoskentelyjakso2.tyoskentelypaikka!!.tyyppi = TyoskentelyjaksoTyyppi.YLIOPISTOLLINEN_SAIRAALA
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso2)

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot-taulukko")).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.tyoskentelyjaksot").value(Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$.keskeytykset").value(Matchers.hasSize<Any>(0))).andExpect(jsonPath("$.tilastot.tyoskentelyaikaYhteensa").value(288.75))
            .andExpect(jsonPath("$.tilastot.arvioErikoistumiseenHyvaksyttavista").value(288.75)).andExpect(jsonPath("$.tilastot.arvioPuuttuvastaKoulutuksesta").value(1901.25))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.terveyskeskusVaadittuVahintaan").value(273.75))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.terveyskeskusSuoritettu").value(273.75))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaalaVaadittuVahintaan").value(821.25))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaalaSuoritettu").value(15.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenVaadittuVahintaan").value(821.25))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenSuoritettu").value(0.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yhteensaVaadittuVahintaan").value(2190.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yhteensaSuoritettu").value(288.75)).andExpect(jsonPath("$.tilastot.kaytannonKoulutus").value(Matchers.hasSize<Any>(4)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot").value(Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot[0].suoritettu").value(367.0)).andExpect(jsonPath("$.tilastot.tyoskentelyjaksot[1].suoritettu").value(15.0))
    }

    @Test
    fun getTyoskentelyjaksoTableWithoutTerveyskeskusMax() {
        initTest()

        val opintoopas = tyoskentelyjakso.opintooikeus?.opintoopas
        opintoopas?.let {
            it.terveyskeskuskoulutusjaksonMaksimipituus = null
            opintoopasRepository.saveAndFlush(it)
        }

        tyoskentelyjakso.kaytannonKoulutus = KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO
        tyoskentelyjakso.tyoskentelypaikka!!.tyyppi = TyoskentelyjaksoTyyppi.TERVEYSKESKUS
        tyoskentelyjakso.paattymispaiva = tyoskentelyjakso.alkamispaiva?.plusYears(1)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        val tyoskentelyjakso2 = ErikoistuvaLaakariTyoskentelyjaksoHelper.createEntity(em, user, LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 15))
        tyoskentelyjakso2.kaytannonKoulutus = KaytannonKoulutusTyyppi.OMAA_ERIKOISALAA_TUKEVA_KOULUTUS
        tyoskentelyjakso2.tyoskentelypaikka!!.tyyppi = TyoskentelyjaksoTyyppi.YLIOPISTOLLINEN_SAIRAALA
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso2)

        restTyoskentelyjaksoMockMvc.perform(get("/api/erikoistuva-laakari/tyoskentelyjaksot-taulukko"))
            .andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.tyoskentelyjaksot").value(Matchers.hasSize<Any>(2))).andExpect(jsonPath("$.keskeytykset").value(Matchers.hasSize<Any>(0)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyaikaYhteensa").value(382.0)).andExpect(jsonPath("$.tilastot.arvioErikoistumiseenHyvaksyttavista").value(382.0))
            .andExpect(jsonPath("$.tilastot.arvioPuuttuvastaKoulutuksesta").value(1808.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.terveyskeskusVaadittuVahintaan").value(273.75))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.terveyskeskusSuoritettu").value(367.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaalaVaadittuVahintaan").value(821.25))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaalaSuoritettu").value(15.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenVaadittuVahintaan").value(821.25))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yliopistosairaaloidenUlkopuolinenSuoritettu").value(0.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yhteensaVaadittuVahintaan").value(2190.0))
            .andExpect(jsonPath("$.tilastot.koulutustyypit.yhteensaSuoritettu").value(382.0)).andExpect(jsonPath("$.tilastot.kaytannonKoulutus").value(Matchers.hasSize<Any>(4)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot").value(Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$.tilastot.tyoskentelyjaksot[0].suoritettu").value(367.0)).andExpect(jsonPath("$.tilastot.tyoskentelyjaksot[1].suoritettu").value(15.0))
    }

    @Test
    fun updateLiitettyKoejaksoon() {
        initTest()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        assertNotNull(tyoskentelyjakso.id)
        assertFalse(tyoskentelyjakso.liitettyKoejaksoon)

        val tyoskentelyjaksoDTO = TyoskentelyjaksoDTO(id = tyoskentelyjakso.id, liitettyKoejaksoon = true)

        restTyoskentelyjaksoMockMvc.perform(patch("$API_TYOSKENTELYJAKSOT/koejakso").contentType(APPLICATION_JSON).content(convertObjectToJsonBytes(tyoskentelyjaksoDTO))
            .with(csrf())).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.liitettyKoejaksoon").value(true))
    }

    @Test
    fun updateTyoskentelyjaksoAsiakirjat() {
        initTest()
        initMockFiles()

        val asiakirja = AsiakirjaHelper.createEntity(em, user, tyoskentelyjakso)
        tyoskentelyjakso.asiakirjat.add(asiakirja)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)

        restTyoskentelyjaksoMockMvc.perform(multipart("$API_TYOSKENTELYJAKSOT/${tyoskentelyjakso.id}/asiakirjat")
                .file(MockMultipartFile("addedFiles", AsiakirjaHelper.ASIAKIRJA_PNG_NIMI, AsiakirjaHelper.ASIAKIRJA_PNG_TYYPPI, tempFile2.readBytes()))
                .param("deletedFiles", asiakirja.id!!.toString()).with { it.method = "PUT"; it }.with(csrf())).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.asiakirjat").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$.asiakirjat[0].nimi").value(AsiakirjaHelper.ASIAKIRJA_PNG_NIMI))
    }

    @Test
    fun getTerveyskeskuskoulutusjaksoTooShort() {
        initTest(kaytannonKoulutus = KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        restTyoskentelyjaksoMockMvc.perform(get("$API_TYOSKENTELYJAKSOT/terveyskeskuskoulutusjakso")).andExpect(status().isBadRequest)
    }

    @Test
    fun getTerveyskeskuskoulutusjaksoWrongKaytannonKoulutus() {
        initTest(kaytannonKoulutus = KaytannonKoulutusTyyppi.OMAN_ERIKOISALAN_KOULUTUS)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        restTyoskentelyjaksoMockMvc.perform(get("$API_TYOSKENTELYJAKSOT/terveyskeskuskoulutusjakso")).andExpect(status().isBadRequest)
    }

    @Test
    fun getTerveyskeskuskoulutusjaksoWithoutVastuuhenkilo() {
        initTest(kaytannonKoulutus = KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO)
        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        tyoskentelyjaksoRepository.saveAndFlush(ErikoistuvaLaakariTyoskentelyjaksoHelper.createEntity(em, user = user,
            paattymispaiva = ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_ALKAMISPAIVA.plusYears(1)))

        restTyoskentelyjaksoMockMvc.perform(get("$API_TYOSKENTELYJAKSOT/terveyskeskuskoulutusjakso")).andExpect(status().isBadRequest)
    }

    @Test
    fun getTerveyskeskuskoulutusjakso() {
        initTest(kaytannonKoulutus = KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO)

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        tyoskentelyjaksoRepository.saveAndFlush(ErikoistuvaLaakariTyoskentelyjaksoHelper.createEntity(em, user = user,
            paattymispaiva = ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_ALKAMISPAIVA.plusYears(1), kaytannonKoulutus = KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO))
        val opintooikeus = opintooikeusRepository.findAll().first()

        val vastuuhenkiloUser = userRepository.saveAndFlush(KayttajaResourceWithMockUserIT.createEntity(authority = Authority(VASTUUHENKILO)))
        val vastuuhenkiloKayttaja = kayttajaRepository.saveAndFlush(KayttajaHelper.createEntity(em, user = vastuuhenkiloUser))
        kayttajaYliopistoErikoisalaRepository.saveAndFlush(
            KayttajaYliopistoErikoisala(kayttaja = vastuuhenkiloKayttaja, yliopisto = opintooikeus.yliopisto, erikoisala = Erikoisala(50),
                vastuuhenkilonTehtavat = mutableSetOf(VastuuhenkilonTehtavatyyppi(2))))

        restTyoskentelyjaksoMockMvc.perform(get("$API_TYOSKENTELYJAKSOT/terveyskeskuskoulutusjakso"))
            .andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.id").doesNotExist())
            .andExpect(jsonPath("$.erikoistuvanErikoisala").value(opintooikeus.erikoisala?.nimi))
            .andExpect(jsonPath("$.erikoistuvanNimi").value(opintooikeus.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.erikoistuvanOpiskelijatunnus").value(opintooikeus.opiskelijatunnus))
            .andExpect(jsonPath("$.erikoistuvanSyntymaaika").value(opintooikeus.erikoistuvaLaakari?.syntymaaika.toString()))
            .andExpect(jsonPath("$.erikoistuvanYliopisto").value(opintooikeus.yliopisto?.nimi.toString()))
            .andExpect(jsonPath("$.laillistamispaiva").value(ErikoistuvaLaakariHelper.DEFAULT_LAILLISTAMISPAIVA.toString()))
            .andExpect(jsonPath("$.laillistamispaivanLiite").value(ErikoistuvaLaakariHelper.DEFAULT_LAILLISTAMISTODISTUS_DATA_AS_STRING))
            .andExpect(jsonPath("$.laillistamispaivanLiitteenNimi").value(ErikoistuvaLaakariHelper.DEFAULT_LAILLISTAMISTODISTUS_NIMI))
            .andExpect(jsonPath("$.laillistamispaivanLiitteenTyyppi").value(ErikoistuvaLaakariHelper.DEFAULT_LAILLISTAMISTODISTUS_TYYPPI))
            .andExpect(jsonPath("$.asetus").value(opintooikeus.asetus?.nimi)).andExpect(jsonPath("$.tyoskentelyjaksot").value(Matchers.hasSize<Any>(2)))
            .andExpect(jsonPath("$.yleislaaketieteenVastuuhenkilonNimi").value(vastuuhenkiloKayttaja.getNimi()))
            .andExpect(jsonPath("$.yleislaaketieteenVastuuhenkilonNimike").value(vastuuhenkiloKayttaja.nimike))
    }

    @Test
    fun createTerveyskeskuskoulutusjakso() {
        initTest(kaytannonKoulutus = KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO)
        initMockFiles()

        tyoskentelyjaksoRepository.saveAndFlush(tyoskentelyjakso)
        tyoskentelyjaksoRepository.saveAndFlush(ErikoistuvaLaakariTyoskentelyjaksoHelper.createEntity(em, user = user,
            paattymispaiva = ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_ALKAMISPAIVA.plusYears(1), kaytannonKoulutus = KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO))
        val opintooikeus = opintooikeusRepository.findAll().first()

        val vastuuhenkiloUser = userRepository.saveAndFlush(KayttajaResourceWithMockUserIT.createEntity(authority = Authority(VASTUUHENKILO)))
        val vastuuhenkiloKayttaja = kayttajaRepository.saveAndFlush(KayttajaHelper.createEntity(em, user = vastuuhenkiloUser))
        kayttajaYliopistoErikoisalaRepository.saveAndFlush(KayttajaYliopistoErikoisala(kayttaja = vastuuhenkiloKayttaja, yliopisto = opintooikeus.yliopisto,
                erikoisala = Erikoisala(50), vastuuhenkilonTehtavat = mutableSetOf(VastuuhenkilonTehtavatyyppi(2))))

        val laillistamispaiva = LocalDate.now()

        restTyoskentelyjaksoMockMvc.perform(multipart("$API_TYOSKENTELYJAKSOT/terveyskeskuskoulutusjakson-hyvaksynta")
                .file(MockMultipartFile("laillistamispaivanLiite", AsiakirjaHelper.ASIAKIRJA_PNG_NIMI, AsiakirjaHelper.ASIAKIRJA_PNG_TYYPPI, tempFile2.readBytes()))
                .param("laillistamispaiva", laillistamispaiva.toString()).with { it.method = "POST"; it }.with(csrf())).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.id").exists())

        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(user.id!!)

        assertEquals(laillistamispaiva, erikoistuvaLaakari?.laillistamispaiva)
        assertThat(erikoistuvaLaakari?.laillistamistodistus).isNotNull
        assertEquals(AsiakirjaHelper.ASIAKIRJA_PNG_NIMI, erikoistuvaLaakari?.laillistamispaivanLiitetiedostonNimi)
        assertEquals(AsiakirjaHelper.ASIAKIRJA_PNG_TYYPPI, erikoistuvaLaakari?.laillistamispaivanLiitetiedostonTyyppi)
    }

    fun initTest(userId: String? = null, kaytannonKoulutus: KaytannonKoulutusTyyppi? = ErikoistuvaLaakariTyoskentelyjaksoHelper.DEFAULT_KAYTANNON_KOULUTUS) {
        user = KayttajaResourceWithMockUserIT.createEntity()
        persistAndFlush(user)
        TestSecurityContextHolder.getContext().authentication = Saml2Authentication(DefaultSaml2AuthenticatedPrincipal(userId ?: user.id, mapOf<String, List<Any>>()),
            "test", listOf(SimpleGrantedAuthority(ERIKOISTUVA_LAAKARI)))
        tyoskentelyjakso = ErikoistuvaLaakariTyoskentelyjaksoHelper.createEntity(em, user, kaytannonKoulutus = kaytannonKoulutus)
    }

    fun initMockFiles() {
        tempFile1 = File.createTempFile("file", "pdf")
        tempFile1.writeBytes(AsiakirjaHelper.ASIAKIRJA_PDF_DATA)
        tempFile1.deleteOnExit()
        tempFile2 = File.createTempFile("file", "png")
        tempFile2.writeBytes(AsiakirjaHelper.ASIAKIRJA_PNG_DATA)
        tempFile2.deleteOnExit()
        mockMultipartFile1 = MockMultipartFile("files", AsiakirjaHelper.ASIAKIRJA_PDF_NIMI, AsiakirjaHelper.ASIAKIRJA_PDF_TYYPPI, tempFile1.readBytes())
        mockMultipartFile2 = MockMultipartFile("files", AsiakirjaHelper.ASIAKIRJA_PNG_NIMI, AsiakirjaHelper.ASIAKIRJA_PNG_TYYPPI, tempFile2.readBytes())
    }
}
