package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.*
import fi.elsapalvelu.elsa.repository.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.ValmistumispyyntoRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.dto.enumeration.ValmistumispyynnonTila
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.helpers.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import jakarta.persistence.EntityManager

private const val ENDPOINT_BASE_URL = "/api/erikoistuva-laakari"
private const val VALMISTUMISPYYNTO_SUORITUSTEN_TILA_ENDPOINT = "/valmistumispyynto-suoritusten-tila"
private const val VALMISTUMISPYYNTO_ENDPOINT = "/valmistumispyynto"

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class ErikoistuvaLaakariValmistumispyyntoResourceIT {

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var valmistumispyyntoRepository: ValmistumispyyntoRepository

    @Autowired
    private lateinit var erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository

    @Autowired
    private lateinit var restValmistumispyyntoMockMvc: MockMvc

    private lateinit var vastuuhenkiloArvioija: Kayttaja

    private lateinit var vastuuhenkiloHyvaksyja: Kayttaja

    private lateinit var virkailija: Kayttaja

    private lateinit var user: User

    private lateinit var tempFile: File

    private lateinit var mockMultipartFile: MockMultipartFile

    private lateinit var erikoistuvaLaakari: ErikoistuvaLaakari

    private lateinit var opintooikeus: Opintooikeus

    // 1.1.1981
    private val now = 347155200L

    @MockBean
    private lateinit var clock: Clock

    @BeforeEach
    fun setup() {
        `when`(clock.instant()).thenReturn(Instant.ofEpochSecond(now))
        `when`(clock.zone).thenReturn(ZoneId.systemDefault())
    }

    @Test
    @Transactional
    fun getValmistumispyyntoSuoritustenTilaErikoistuvaLaakariHasVanhojaSuorituksiaFalse() {
        initTestWithVoimassaolevatSuoritukset()

        em.persist(
            OpintosuoritusHelper.createEntity(
                em,
                user,
                tyyppiEnum = OpintosuoritusTyyppiEnum.VALTAKUNNALLINEN_KUULUSTELU,
                suorituspaiva = LocalDate.ofEpochDay(4000L)
            )
        )

        restValmistumispyyntoMockMvc.perform(
            get(
                ENDPOINT_BASE_URL + VALMISTUMISPYYNTO_SUORITUSTEN_TILA_ENDPOINT
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.erikoisalaTyyppi").value(ErikoisalaTyyppi.LAAKETIEDE.toString()))
            .andExpect(jsonPath("$.vanhojaTyoskentelyjaksojaOrSuorituksiaExists").value(false))
            .andExpect(jsonPath("$.kuulusteluVanhentunut").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyyntoSuoritustenTilaErikoistuvaHammaslaakariHasVanhojaSuorituksiaFalse() {
        initTestWithVoimassaolevatSuoritukset(ErikoisalaTyyppi.HAMMASLAAKETIEDE)

        em.persist(
            OpintosuoritusHelper.createEntity(
                em,
                user,
                tyyppiEnum = OpintosuoritusTyyppiEnum.VALTAKUNNALLINEN_KUULUSTELU,
                suorituspaiva = LocalDate.ofEpochDay(4000L)
            )
        )

        restValmistumispyyntoMockMvc.perform(
            get(
                ENDPOINT_BASE_URL + VALMISTUMISPYYNTO_SUORITUSTEN_TILA_ENDPOINT
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.erikoisalaTyyppi").value(ErikoisalaTyyppi.HAMMASLAAKETIEDE.toString()))
            .andExpect(jsonPath("$.vanhojaTyoskentelyjaksojaOrSuorituksiaExists").value(false))
            .andExpect(jsonPath("$.kuulusteluVanhentunut").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyyntoSuoritustenTilaErikoistuvaLaakariVanhentunutTyoskentelyjaksoExists() {
        initTestWithVoimassaolevatSuoritukset()

        em.persist(
            TyoskentelyjaksoHelper.createEntity(
                em,
                user,
                alkamispaiva = LocalDate.ofEpochDay(30L),
                paattymispaiva = LocalDate.ofEpochDay(100L),
                tyoskentelyjaksoTyyppi = TyoskentelyjaksoTyyppi.KESKUSSAIRAALA
            )
        )

        restValmistumispyyntoMockMvc.perform(
            get(
                ENDPOINT_BASE_URL + VALMISTUMISPYYNTO_SUORITUSTEN_TILA_ENDPOINT
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.erikoisalaTyyppi").value(ErikoisalaTyyppi.LAAKETIEDE.toString()))
            .andExpect(jsonPath("$.vanhojaTyoskentelyjaksojaOrSuorituksiaExists").value(true))
            .andExpect(jsonPath("$.kuulusteluVanhentunut").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyyntoSuoritustenTilaErikoistuvaLaakariVanhentunutTyoskentelyjaksoExistsOnlyByAlkamispaiva() {
        initTestWithVoimassaolevatSuoritukset()

        em.persist(
            TyoskentelyjaksoHelper.createEntity(
                em,
                user,
                alkamispaiva = LocalDate.ofEpochDay(364L),
                paattymispaiva = LocalDate.ofEpochDay(800L),
                tyoskentelyjaksoTyyppi = TyoskentelyjaksoTyyppi.KESKUSSAIRAALA
            )
        )

        restValmistumispyyntoMockMvc.perform(
            get(
                ENDPOINT_BASE_URL + VALMISTUMISPYYNTO_SUORITUSTEN_TILA_ENDPOINT
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.erikoisalaTyyppi").value(ErikoisalaTyyppi.LAAKETIEDE.toString()))
            .andExpect(jsonPath("$.vanhojaTyoskentelyjaksojaOrSuorituksiaExists").value(true))
            .andExpect(jsonPath("$.kuulusteluVanhentunut").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyyntoSuoritustenTilaErikoistuvaLaakariVanhentunutTerveyskeskusjaksoIgnored() {
        initTestWithVoimassaolevatSuoritukset()

        em.persist(
            TyoskentelyjaksoHelper.createEntity(
                em,
                user,
                alkamispaiva = LocalDate.ofEpochDay(30L),
                paattymispaiva = LocalDate.ofEpochDay(100L),
                tyoskentelyjaksoTyyppi = TyoskentelyjaksoTyyppi.TERVEYSKESKUS,
                kaytannonKoulutus = KaytannonKoulutusTyyppi.TERVEYSKESKUSTYO
            )
        )

        restValmistumispyyntoMockMvc.perform(
            get(
                ENDPOINT_BASE_URL + VALMISTUMISPYYNTO_SUORITUSTEN_TILA_ENDPOINT
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.erikoisalaTyyppi").value(ErikoisalaTyyppi.LAAKETIEDE.toString()))
            .andExpect(jsonPath("$.vanhojaTyoskentelyjaksojaOrSuorituksiaExists").value(false))
            .andExpect(jsonPath("$.kuulusteluVanhentunut").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyyntoSuoritustenTilaErikoistuvaHammaslaakariVanhentunutTyoskentelyjaksoExists() {
        initTestWithVoimassaolevatSuoritukset(ErikoisalaTyyppi.HAMMASLAAKETIEDE)

        em.persist(
            TyoskentelyjaksoHelper.createEntity(
                em,
                user,
                alkamispaiva = LocalDate.ofEpochDay(1500L),
                paattymispaiva = LocalDate.ofEpochDay(1600L),
                tyoskentelyjaksoTyyppi = TyoskentelyjaksoTyyppi.KESKUSSAIRAALA
            )
        )

        restValmistumispyyntoMockMvc.perform(
            get(
                ENDPOINT_BASE_URL + VALMISTUMISPYYNTO_SUORITUSTEN_TILA_ENDPOINT
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.erikoisalaTyyppi").value(ErikoisalaTyyppi.HAMMASLAAKETIEDE.toString()))
            .andExpect(jsonPath("$.vanhojaTyoskentelyjaksojaOrSuorituksiaExists").value(true))
            .andExpect(jsonPath("$.kuulusteluVanhentunut").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyyntoSuoritustenTilaErikoistuvaHammaslaakariVanhentunutTyoskentelyjaksoExistsOnlyByAlkamispaiva() {
        initTestWithVoimassaolevatSuoritukset(ErikoisalaTyyppi.HAMMASLAAKETIEDE)

        em.persist(
            TyoskentelyjaksoHelper.createEntity(
                em,
                user,
                alkamispaiva = LocalDate.ofEpochDay(1825L),
                paattymispaiva = LocalDate.ofEpochDay(2500L),
                tyoskentelyjaksoTyyppi = TyoskentelyjaksoTyyppi.KESKUSSAIRAALA
            )
        )

        restValmistumispyyntoMockMvc.perform(
            get(
                ENDPOINT_BASE_URL + VALMISTUMISPYYNTO_SUORITUSTEN_TILA_ENDPOINT
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.erikoisalaTyyppi").value(ErikoisalaTyyppi.HAMMASLAAKETIEDE.toString()))
            .andExpect(jsonPath("$.vanhojaTyoskentelyjaksojaOrSuorituksiaExists").value(true))
            .andExpect(jsonPath("$.kuulusteluVanhentunut").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyyntoSuoritustenTilaErikoistuvaLaakariVanhentunutOpintosuoritusExists() {
        initTestWithVoimassaolevatSuoritukset()

        em.persist(OpintosuoritusHelper.createEntity(em, user, suorituspaiva = LocalDate.ofEpochDay(100L)))

        restValmistumispyyntoMockMvc.perform(
            get(
                ENDPOINT_BASE_URL + VALMISTUMISPYYNTO_SUORITUSTEN_TILA_ENDPOINT
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.erikoisalaTyyppi").value(ErikoisalaTyyppi.LAAKETIEDE.toString()))
            .andExpect(jsonPath("$.vanhojaTyoskentelyjaksojaOrSuorituksiaExists").value(true))
            .andExpect(jsonPath("$.kuulusteluVanhentunut").value(false))
    }

    @Test
    @Transactional
    fun getValmistumispyyntoSuoritustenTilaErikoistuvaHammaslaakariVanhentunutOpintosuoritusExists() {
        initTestWithVoimassaolevatSuoritukset(ErikoisalaTyyppi.HAMMASLAAKETIEDE)

        em.persist(OpintosuoritusHelper.createEntity(em, user, suorituspaiva = LocalDate.ofEpochDay(1500L)))

        restValmistumispyyntoMockMvc.perform(
            get(
                ENDPOINT_BASE_URL + VALMISTUMISPYYNTO_SUORITUSTEN_TILA_ENDPOINT
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.erikoisalaTyyppi").value(ErikoisalaTyyppi.HAMMASLAAKETIEDE.toString()))
            .andExpect(jsonPath("$.vanhojaTyoskentelyjaksojaOrSuorituksiaExists").value(true))
            .andExpect(jsonPath("$.kuulusteluVanhentunut").value(false))
    }

    @Test
    @Transactional
    fun createValmistumispyyntoVanhentuneitaSuorituksiaNotExists() {
        initTestWithVoimassaolevatSuoritukset()
        initValmistumispyynnonHyvaksyjat()

        val valmistumispyyntoTableSizeBeforeCreate = valmistumispyyntoRepository.findAll().size

        restValmistumispyyntoMockMvc.perform(
            multipart("/api/erikoistuva-laakari/valmistumispyynto")
                .with { it.method = "POST"; it }
                .with(csrf())
        ).andExpect(status().isCreated)

        val valmistumispyynnotList = valmistumispyyntoRepository.findAll()
        assertThat(valmistumispyynnotList).hasSize(valmistumispyyntoTableSizeBeforeCreate + 1)

        val valmistumispyynto = valmistumispyynnotList.first()
        assertThat(valmistumispyynto.erikoistujanKuittausaika).isEqualTo(LocalDate.now())
        assertThat(valmistumispyynto.selvitysVanhentuneistaSuorituksista).isNull()
    }

    @Test
    @Transactional
    fun tryToCreateValmistumispyyntoWithoutLaillistamispaivaAndTodistus() {
        initTestWithVoimassaolevatSuoritukset()
        initValmistumispyynnonHyvaksyjat()

        erikoistuvaLaakari.laillistamispaiva = null
        erikoistuvaLaakari.laillistamistodistus = null
        erikoistuvaLaakari.laillistamispaivanLiitetiedostonNimi = null
        erikoistuvaLaakari.laillistamispaivanLiitetiedostonTyyppi = null

        val valmistumispyyntoTableSizeBeforeCreate = valmistumispyyntoRepository.findAll().size

        restValmistumispyyntoMockMvc.perform(
            multipart("/api/erikoistuva-laakari/valmistumispyynto")
                .with { it.method = "POST"; it }
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val valmistumispyynnotList = valmistumispyyntoRepository.findAll()
        assertThat(valmistumispyynnotList).hasSize(valmistumispyyntoTableSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createValmistumispyyntoWithLaillistamispaivaAndTodistus() {
        initTestWithVoimassaolevatSuoritukset()
        initValmistumispyynnonHyvaksyjat()
        initMockFile()

        erikoistuvaLaakari.laillistamispaiva = null
        erikoistuvaLaakari.laillistamistodistus = null
        erikoistuvaLaakari.laillistamispaivanLiitetiedostonNimi = null
        erikoistuvaLaakari.laillistamispaivanLiitetiedostonTyyppi = null

        val valmistumispyyntoTableSizeBeforeCreate = valmistumispyyntoRepository.findAll().size
        val laillistamispaiva = LocalDate.now()

        restValmistumispyyntoMockMvc.perform(
            multipart("/api/erikoistuva-laakari/valmistumispyynto")
                .file(mockMultipartFile)
                .param("laillistamispaiva", laillistamispaiva.toString())
                .with { it.method = "POST"; it }
                .with(csrf())
        ).andExpect(status().isCreated)

        val valmistumispyynnotList = valmistumispyyntoRepository.findAll()
        assertThat(valmistumispyynnotList).hasSize(valmistumispyyntoTableSizeBeforeCreate + 1)

        val erikoistuvaLaakari = erikoistuvaLaakariRepository.findOneByKayttajaUserId(user.id!!)
        assertThat(erikoistuvaLaakari?.laillistamispaiva).isEqualTo(laillistamispaiva)
        assertThat(erikoistuvaLaakari?.laillistamistodistus?.data).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PDF_DATA)
        assertThat(erikoistuvaLaakari?.laillistamispaivanLiitetiedostonNimi).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PDF_NIMI)
        assertThat(erikoistuvaLaakari?.laillistamispaivanLiitetiedostonTyyppi).isEqualTo(AsiakirjaHelper.ASIAKIRJA_PDF_TYYPPI)
    }

    @Test
    @Transactional
    fun tryToCreateValmistumispyyntoWithLaillistamistodistusWithInvalidContentType() {
        initTestWithVoimassaolevatSuoritukset()
        initValmistumispyynnonHyvaksyjat()
        initMockFile()

        erikoistuvaLaakari.laillistamispaiva = null
        erikoistuvaLaakari.laillistamistodistus = null
        erikoistuvaLaakari.laillistamispaivanLiitetiedostonNimi = null
        erikoistuvaLaakari.laillistamispaivanLiitetiedostonTyyppi = null

        val valmistumispyyntoTableSizeBeforeCreate = valmistumispyyntoRepository.findAll().size
        val laillistamispaiva = LocalDate.now()

        restValmistumispyyntoMockMvc.perform(
            multipart("/api/erikoistuva-laakari/valmistumispyynto")
                .file(
                    MockMultipartFile(
                        "laillistamistodistus",
                        "invalidFile",
                        "application/x-msdownload",
                        tempFile.readBytes()
                    )
                )
                .param("laillistamispaiva", laillistamispaiva.toString())
                .with { it.method = "POST"; it }
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val valmistumispyynnotList = valmistumispyyntoRepository.findAll()
        assertThat(valmistumispyynnotList).hasSize(valmistumispyyntoTableSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createValmistumispyyntoVanhentunutSuoritusExistsSelvitysNull() {
        initTestWithVoimassaolevatSuoritukset()
        initValmistumispyynnonHyvaksyjat()

        em.persist(OpintosuoritusHelper.createEntity(em, user, suorituspaiva = LocalDate.ofEpochDay(100L)))

        val valmistumispyyntoTableSizeBeforeCreate = valmistumispyyntoRepository.findAll().size

        restValmistumispyyntoMockMvc.perform(
            multipart("/api/erikoistuva-laakari/valmistumispyynto")
                .with { it.method = "POST"; it }
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val valmistumispyynnotList = valmistumispyyntoRepository.findAll()
        assertThat(valmistumispyynnotList).hasSize(valmistumispyyntoTableSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createValmistumispyyntoWithSelvitysVanhentuneistaSuorituksista() {
        initTestWithVoimassaolevatSuoritukset()
        initValmistumispyynnonHyvaksyjat()

        em.persist(OpintosuoritusHelper.createEntity(em, user, suorituspaiva = LocalDate.ofEpochDay(100L)))

        val valmistumispyyntoTableSizeBeforeCreate = valmistumispyyntoRepository.findAll().size
        val selvitysVanhentuneistaSuorituksista = "selvitysVanhentuneistaSuorituksista"

        restValmistumispyyntoMockMvc.perform(
            multipart("/api/erikoistuva-laakari/valmistumispyynto")
                .with { it.method = "POST"; it }
                .param("selvitysVanhentuneistaSuorituksista", selvitysVanhentuneistaSuorituksista)
                .with(csrf())
        ).andExpect(status().isCreated)

        val valmistumispyynnotList = valmistumispyyntoRepository.findAll()
        assertThat(valmistumispyynnotList).hasSize(valmistumispyyntoTableSizeBeforeCreate + 1)

        val valmistumispyynto = valmistumispyynnotList.first()
        assertThat(valmistumispyynto.erikoistujanKuittausaika).isEqualTo(LocalDate.now())
        assertThat(valmistumispyynto.selvitysVanhentuneistaSuorituksista).isEqualTo(selvitysVanhentuneistaSuorituksista)
    }

    @Test
    @Transactional
    fun tryToCreateValmistumispyyntoAlreadyExists() {
        initTestWithVoimassaolevatSuoritukset()

        val valmistumispyynto = Valmistumispyynto(
            opintooikeus = opintooikeus,
            vastuuhenkiloOsaamisenArvioijaPalautusaika = LocalDate.now(),
            selvitysVanhentuneistaSuorituksista = "selvitysVanhentuneistaSuorituksista",
            vastuuhenkiloOsaamisenArvioijaKorjausehdotus = "korjausehdotus"
        )
        em.persist(valmistumispyynto)

        val valmistumispyyntoTableSizeBeforeCreate = valmistumispyyntoRepository.findAll().size

        restValmistumispyyntoMockMvc.perform(
            multipart("/api/erikoistuva-laakari/valmistumispyynto")
                .with { it.method = "POST"; it }
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val valmistumispyynnotList = valmistumispyyntoRepository.findAll()
        assertThat(valmistumispyynnotList).hasSize(valmistumispyyntoTableSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun updateValmistumispyyntoPalautettu() {
        initTestWithVoimassaolevatSuoritukset()
        initValmistumispyynnonHyvaksyjat()

        val valmistumispyynto = Valmistumispyynto(
            opintooikeus = opintooikeus,
            vastuuhenkiloOsaamisenArvioijaPalautusaika = LocalDate.now(),
            selvitysVanhentuneistaSuorituksista = "selvitysVanhentuneistaSuorituksista",
            vastuuhenkiloOsaamisenArvioijaKorjausehdotus = "korjausehdotus"
        )
        em.persist(valmistumispyynto)

        val valmistumispyyntoTableSizeBeforeCreate = valmistumispyyntoRepository.findAll().size
        val selvitysVanhentuneistaSuorituksistaUpdated = "selvitysVanhentuneistaSuorituksistaUpdated"

        restValmistumispyyntoMockMvc.perform(
            multipart("/api/erikoistuva-laakari/valmistumispyynto")
                .with { it.method = "PUT"; it }
                .param("selvitysVanhentuneistaSuorituksista", selvitysVanhentuneistaSuorituksistaUpdated)
                .with(csrf())
        ).andExpect(status().isOk)

        val valmistumispyynnotList = valmistumispyyntoRepository.findAll()
        assertThat(valmistumispyynnotList).hasSize(valmistumispyyntoTableSizeBeforeCreate)

        val valmistumispyyntoSaved = valmistumispyynnotList.first()
        assertThat(valmistumispyyntoSaved.erikoistujanKuittausaika).isEqualTo(LocalDate.now())
        assertThat(valmistumispyyntoSaved.vastuuhenkiloOsaamisenArvioijaKorjausehdotus).isNull()
        assertThat(valmistumispyyntoSaved.vastuuhenkiloOsaamisenArvioijaPalautusaika).isNull()
        assertThat(valmistumispyyntoSaved.selvitysVanhentuneistaSuorituksista).isEqualTo(
            selvitysVanhentuneistaSuorituksistaUpdated
        )
    }

    @Test
    @Transactional
    fun tryToUpdateValmistumispyyntoWithLaillistamispaivaWithInvalidContentType() {
        initTestWithVoimassaolevatSuoritukset()
        initValmistumispyynnonHyvaksyjat()
        initMockFile()

        val valmistumispyynto = Valmistumispyynto(
            opintooikeus = opintooikeus,
            vastuuhenkiloOsaamisenArvioijaPalautusaika = LocalDate.now(),
            selvitysVanhentuneistaSuorituksista = "selvitysVanhentuneistaSuorituksista",
            vastuuhenkiloOsaamisenArvioijaKorjausehdotus = "korjausehdotus"
        )
        em.persist(valmistumispyynto)

        erikoistuvaLaakari.laillistamispaiva = null
        erikoistuvaLaakari.laillistamistodistus = null
        erikoistuvaLaakari.laillistamispaivanLiitetiedostonNimi = null
        erikoistuvaLaakari.laillistamispaivanLiitetiedostonTyyppi = null

        val valmistumispyyntoTableSizeBeforeCreate = valmistumispyyntoRepository.findAll().size
        val laillistamispaiva = LocalDate.now()

        restValmistumispyyntoMockMvc.perform(
            multipart("/api/erikoistuva-laakari/valmistumispyynto")
                .file(
                    MockMultipartFile(
                        "laillistamistodistus",
                        "invalidFile",
                        "application/x-msdownload",
                        tempFile.readBytes()
                    )
                )
                .param("laillistamispaiva", laillistamispaiva.toString())
                .with { it.method = "PUT"; it }
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val valmistumispyynnotList = valmistumispyyntoRepository.findAll()
        assertThat(valmistumispyynnotList).hasSize(valmistumispyyntoTableSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun tryToUpdateValmistumispyyntoNotExists() {
        initTestWithVoimassaolevatSuoritukset()

        val valmistumispyyntoTableSizeBeforeCreate = valmistumispyyntoRepository.findAll().size

        restValmistumispyyntoMockMvc.perform(
            multipart("/api/erikoistuva-laakari/valmistumispyynto")
                .with { it.method = "PUT"; it }
                .with(csrf())
        ).andExpect(status().is5xxServerError)

        val valmistumispyynnotList = valmistumispyyntoRepository.findAll()
        assertThat(valmistumispyynnotList).hasSize(valmistumispyyntoTableSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun getValmistumispyyntoUusi() {
        initTestWithVoimassaolevatSuoritukset()
        initValmistumispyynnonHyvaksyjat()

        restValmistumispyyntoMockMvc.perform(
            get(
                ENDPOINT_BASE_URL + VALMISTUMISPYYNTO_ENDPOINT
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.tila").value(ValmistumispyynnonTila.UUSI.toString()))
            .andExpect(jsonPath("$.vastuuhenkiloOsaamisenArvioijaNimi").value(vastuuhenkiloArvioija.getNimi()))
            .andExpect(jsonPath("$.vastuuhenkiloOsaamisenArvioijaNimike").value(vastuuhenkiloArvioija.nimike))
            .andExpect(jsonPath("$.vastuuhenkiloHyvaksyjaNimi").value(vastuuhenkiloHyvaksyja.getNimi()))
            .andExpect(jsonPath("$.vastuuhenkiloHyvaksyjaNimike").value(vastuuhenkiloHyvaksyja.nimike))
            .andExpect(jsonPath("$.erikoistujanLaillistamispaiva").value(ErikoistuvaLaakariHelper.DEFAULT_LAILLISTAMISPAIVA.toString()))
            .andExpect(jsonPath("$.erikoistujanLaillistamistodistus").value(ErikoistuvaLaakariHelper.DEFAULT_LAILLISTAMISTODISTUS_DATA_AS_STRING))
            .andExpect(jsonPath("$.erikoistujanLaillistamistodistusNimi").value(ErikoistuvaLaakariHelper.DEFAULT_LAILLISTAMISTODISTUS_NIMI))
            .andExpect(jsonPath("$.erikoistujanLaillistamistodistusTyyppi").value(ErikoistuvaLaakariHelper.DEFAULT_LAILLISTAMISTODISTUS_TYYPPI))
    }

    @Test
    @Transactional
    fun getValmistumispyyntoOdottaaOsaamisenArviointia() {
        initTestWithVoimassaolevatSuoritukset()
        initValmistumispyynnonHyvaksyjat()

        val valmistumispyynto = Valmistumispyynto(
            opintooikeus = opintooikeus,
            erikoistujanKuittausaika = LocalDate.now()
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                ENDPOINT_BASE_URL + VALMISTUMISPYYNTO_ENDPOINT
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.tila").value(ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_TARKASTUSTA.toString()))
            .andExpect(jsonPath("$.erikoistujanNimi").value(opintooikeus.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.erikoistujanOpiskelijatunnus").value(opintooikeus.opiskelijatunnus))
            .andExpect(jsonPath("$.erikoistujanSyntymaaika").value(opintooikeus.erikoistuvaLaakari?.syntymaaika.toString()))
            .andExpect(jsonPath("$.erikoistujanYliopisto").value(opintooikeus.yliopisto?.nimi.toString()))
            .andExpect(jsonPath("$.erikoistujanAsetus").value(opintooikeus.asetus?.nimi))
            .andExpect(jsonPath("$.opintooikeudenMyontamispaiva").value(opintooikeus.opintooikeudenMyontamispaiva.toString()))
            .andExpect(jsonPath("$.erikoistujanKuittausaika").value(LocalDate.now().toString()))
            .andExpect(jsonPath("$.muokkauspaiva").value(LocalDate.now().toString()))
    }

    @Test
    @Transactional
    fun getValmistumispyyntoVastuuhenkiloOsaamisenArvioijaPalauttanut() {
        initTestWithVoimassaolevatSuoritukset()
        initValmistumispyynnonHyvaksyjat()

        val korjausehdotus = "korjausehdotusVastuuhenkiloArvioija"
        val valmistumispyynto = Valmistumispyynto(
            opintooikeus = opintooikeus,
            vastuuhenkiloOsaamisenArvioijaPalautusaika = LocalDate.now(),
            vastuuhenkiloOsaamisenArvioijaKorjausehdotus = korjausehdotus
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                ENDPOINT_BASE_URL + VALMISTUMISPYYNTO_ENDPOINT
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.tila").value(ValmistumispyynnonTila.VASTUUHENKILON_TARKASTUS_PALAUTETTU.toString()))
            .andExpect(jsonPath("$.vastuuhenkiloOsaamisenArvioijaKorjausehdotus").value(korjausehdotus))
            .andExpect(jsonPath("$.vastuuhenkiloOsaamisenArvioijaPalautusaika").value(LocalDate.now().toString()))
    }

    @Test
    @Transactional
    fun getValmistumispyyntoOdottaaVirkailijanTarkastusta() {
        initTestWithVoimassaolevatSuoritukset()
        initValmistumispyynnonHyvaksyjat()

        val valmistumispyynto = Valmistumispyynto(
            opintooikeus = opintooikeus,
            erikoistujanKuittausaika = LocalDate.now(),
            vastuuhenkiloOsaamisenArvioijaKuittausaika = LocalDate.now()
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                ENDPOINT_BASE_URL + VALMISTUMISPYYNTO_ENDPOINT
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.tila").value(ValmistumispyynnonTila.ODOTTAA_VIRKAILIJAN_TARKASTUSTA.toString()))
            .andExpect(jsonPath("$.erikoistujanKuittausaika").value(LocalDate.now().toString()))
            .andExpect(jsonPath("$.vastuuhenkiloOsaamisenArvioijaKuittausaika").value(LocalDate.now().toString()))
    }

    @Test
    @Transactional
    fun getValmistumispyyntoVirkailijaPalauttanut() {
        initTestWithVoimassaolevatSuoritukset()
        initValmistumispyynnonHyvaksyjat()

        val korjausehdotus = "korjausehdotusVirkailija"
        val valmistumispyynto = Valmistumispyynto(
            opintooikeus = opintooikeus,
            virkailijanPalautusaika = LocalDate.now(),
            virkailijanKorjausehdotus = korjausehdotus
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                ENDPOINT_BASE_URL + VALMISTUMISPYYNTO_ENDPOINT
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.tila").value(ValmistumispyynnonTila.VIRKAILIJAN_TARKASTUS_PALAUTETTU.toString()))
            .andExpect(jsonPath("$.virkailijanPalautusaika").value(LocalDate.now().toString()))
            .andExpect(jsonPath("$.virkailijanKorjausehdotus").value(korjausehdotus))
    }

    @Test
    @Transactional
    fun getValmistumispyyntoOdottaaVastuuhenkilonHyvaksyntaa() {
        initTestWithVoimassaolevatSuoritukset()
        initValmistumispyynnonHyvaksyjat()

        val valmistumispyynto = Valmistumispyynto(
            opintooikeus = opintooikeus,
            erikoistujanKuittausaika = LocalDate.now(),
            vastuuhenkiloOsaamisenArvioijaKuittausaika = LocalDate.now(),
            virkailijanKuittausaika = LocalDate.now()
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                ENDPOINT_BASE_URL + VALMISTUMISPYYNTO_ENDPOINT
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.tila").value(ValmistumispyynnonTila.ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA.toString()))
            .andExpect(jsonPath("$.erikoistujanKuittausaika").value(LocalDate.now().toString()))
            .andExpect(jsonPath("$.vastuuhenkiloOsaamisenArvioijaKuittausaika").value(LocalDate.now().toString()))
            .andExpect(jsonPath("$.virkailijanKuittausaika").value(LocalDate.now().toString()))
            .andExpect(jsonPath("$.vastuuhenkiloHyvaksyjaKuittausaika").isEmpty)
    }

    @Test
    @Transactional
    fun getValmistumispyyntoVastuuhenkiloHyvaksyjaPalauttanut() {
        initTestWithVoimassaolevatSuoritukset()
        initValmistumispyynnonHyvaksyjat()

        val korjausehdotus = "korjausehdotusVastuuhenkiloHyvaksyja"
        val valmistumispyynto = Valmistumispyynto(
            opintooikeus = opintooikeus,
            vastuuhenkiloHyvaksyjaKorjausehdotus = korjausehdotus,
            vastuuhenkiloHyvaksyjaPalautusaika = LocalDate.now()
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                ENDPOINT_BASE_URL + VALMISTUMISPYYNTO_ENDPOINT
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.tila").value(ValmistumispyynnonTila.VASTUUHENKILON_HYVAKSYNTA_PALAUTETTU.toString()))
            .andExpect(jsonPath("$.vastuuhenkiloHyvaksyjaKorjausehdotus").value(korjausehdotus))
            .andExpect(jsonPath("$.vastuuhenkiloHyvaksyjaPalautusaika").value(LocalDate.now().toString()))
    }

    @Test
    @Transactional
    fun getValmistumispyyntoOdottaaAllekirjoitusta() {
        initTestWithVoimassaolevatSuoritukset()
        initValmistumispyynnonHyvaksyjat()

        val valmistumispyynto = Valmistumispyynto(
            opintooikeus = opintooikeus,
            erikoistujanKuittausaika = LocalDate.now(),
            vastuuhenkiloOsaamisenArvioijaKuittausaika = LocalDate.now(),
            virkailijanKuittausaika = LocalDate.now(),
            vastuuhenkiloHyvaksyjaKuittausaika = LocalDate.now()
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                ENDPOINT_BASE_URL + VALMISTUMISPYYNTO_ENDPOINT
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.tila").value(ValmistumispyynnonTila.ODOTTAA_ALLEKIRJOITUKSIA.toString()))
            .andExpect(jsonPath("$.erikoistujanKuittausaika").value(LocalDate.now().toString()))
            .andExpect(jsonPath("$.vastuuhenkiloOsaamisenArvioijaKuittausaika").value(LocalDate.now().toString()))
            .andExpect(jsonPath("$.virkailijanKuittausaika").value(LocalDate.now().toString()))
            .andExpect(jsonPath("$.vastuuhenkiloHyvaksyjaKuittausaika").value(LocalDate.now().toString()))
    }

    @Test
    @Transactional
    fun getValmistumispyyntoAllekirjoitettu() {
        initTestWithVoimassaolevatSuoritukset()
        initValmistumispyynnonHyvaksyjat()

        val valmistumispyynto = Valmistumispyynto(
            opintooikeus = opintooikeus,
            erikoistujanKuittausaika = LocalDate.now(),
            vastuuhenkiloOsaamisenArvioijaKuittausaika = LocalDate.now(),
            virkailijanKuittausaika = LocalDate.now(),
            vastuuhenkiloHyvaksyjaKuittausaika = LocalDate.now()
        )
        em.persist(valmistumispyynto)

        restValmistumispyyntoMockMvc.perform(
            get(
                ENDPOINT_BASE_URL + VALMISTUMISPYYNTO_ENDPOINT
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.tila").value(ValmistumispyynnonTila.ODOTTAA_ALLEKIRJOITUKSIA.toString()))
            .andExpect(jsonPath("$.erikoistujanKuittausaika").value(LocalDate.now().toString()))
            .andExpect(jsonPath("$.vastuuhenkiloOsaamisenArvioijaKuittausaika").value(LocalDate.now().toString()))
            .andExpect(jsonPath("$.virkailijanKuittausaika").value(LocalDate.now().toString()))
            .andExpect(jsonPath("$.vastuuhenkiloHyvaksyjaKuittausaika").value(LocalDate.now().toString()))
    }

    private fun initTestWithVoimassaolevatSuoritukset(erikoisalaTyyppi: ErikoisalaTyyppi = ErikoisalaTyyppi.LAAKETIEDE) {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>()
        val authorities = listOf(SimpleGrantedAuthority(ERIKOISTUVA_LAAKARI))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        val erikoisala = ErikoisalaHelper.createEntity(tyyppi = erikoisalaTyyppi)
        em.persist(erikoisala)
        erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user, erikoisala = erikoisala)
        em.persist(erikoistuvaLaakari)
        em.flush()

        opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()!!

        val tyoskentelyjakso = TyoskentelyjaksoHelper.createEntity(
            em,
            user,
            alkamispaiva = LocalDate.ofEpochDay(2000L),
            paattymispaiva = LocalDate.ofEpochDay(2100L),
            tyoskentelyjaksoTyyppi = TyoskentelyjaksoTyyppi.KESKUSSAIRAALA
        )
        em.persist(tyoskentelyjakso)

        em.persist(
            TyoskentelyjaksoHelper.createEntity(
                em,
                user,
                alkamispaiva = LocalDate.ofEpochDay(2500L),
                paattymispaiva = LocalDate.ofEpochDay(2600L),
                tyoskentelyjaksoTyyppi = TyoskentelyjaksoTyyppi.KESKUSSAIRAALA
            )
        )

        em.persist(OpintosuoritusHelper.createEntity(em, user, suorituspaiva = LocalDate.ofEpochDay(3500L)))
    }

    private fun initValmistumispyynnonHyvaksyjat() {
        val vastuuhenkiloArvioijaUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(
                VASTUUHENKILO
            )
        )
        em.persist(vastuuhenkiloArvioijaUser)

        val vastuuhenkiloHyvaksyjaUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(
                VASTUUHENKILO
            )
        )
        em.persist(vastuuhenkiloHyvaksyjaUser)

        val virkailijaUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(
                OPINTOHALLINNON_VIRKAILIJA
            )
        )
        em.persist(virkailijaUser)

        vastuuhenkiloArvioija = KayttajaHelper.createEntity(em, vastuuhenkiloArvioijaUser)
        vastuuhenkiloArvioija.nimike = VASTUUHENKILO_ARVIOIJA_NIMIKE
        em.persist(vastuuhenkiloArvioija)

        vastuuhenkiloHyvaksyja = KayttajaHelper.createEntity(em, vastuuhenkiloHyvaksyjaUser)
        vastuuhenkiloHyvaksyja.nimike = VASTUUHENKILO_HYVAKSYJA_NIMIKE
        em.persist(vastuuhenkiloHyvaksyja)

        virkailija = KayttajaHelper.createEntity(em, virkailijaUser)
        virkailija.tila = KayttajatilinTila.AKTIIVINEN
        em.persist(virkailija)

        val vastuuhenkiloArvioijaYliopistoAndErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = vastuuhenkiloArvioija,
            yliopisto = opintooikeus.yliopisto,
            erikoisala = opintooikeus.erikoisala
        )
        em.persist(vastuuhenkiloArvioijaYliopistoAndErikoisala)
        vastuuhenkiloArvioija.yliopistotAndErikoisalat.add(vastuuhenkiloArvioijaYliopistoAndErikoisala)

        val vastuuhenkiloHyvaksyjaYliopistoAndErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = vastuuhenkiloHyvaksyja,
            yliopisto = opintooikeus.yliopisto,
            erikoisala = opintooikeus.erikoisala
        )
        em.persist(vastuuhenkiloHyvaksyjaYliopistoAndErikoisala)
        vastuuhenkiloHyvaksyja.yliopistotAndErikoisalat.add(vastuuhenkiloHyvaksyjaYliopistoAndErikoisala)

        val osaamisenArviointiTehtavatyyppi =

            VastuuhenkilonTehtavatyyppi(nimi = VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_OSAAMISEN_ARVIOINTI)
        em.persist(osaamisenArviointiTehtavatyyppi)

        val hyvaksyntaTehtavatyyppi =
            VastuuhenkilonTehtavatyyppi(nimi = VastuuhenkilonTehtavatyyppiEnum.VALMISTUMISPYYNNON_HYVAKSYNTA)
        em.persist(hyvaksyntaTehtavatyyppi)

        vastuuhenkiloArvioija.yliopistotAndErikoisalat.first().vastuuhenkilonTehtavat.add(
            osaamisenArviointiTehtavatyyppi
        )
        vastuuhenkiloHyvaksyja.yliopistotAndErikoisalat.first().vastuuhenkilonTehtavat.add(hyvaksyntaTehtavatyyppi)
        virkailija.yliopistot.add(opintooikeus.yliopisto!!)
    }

    fun initMockFile() {
        tempFile = File.createTempFile("file", "pdf")
        tempFile.writeBytes(AsiakirjaHelper.ASIAKIRJA_PDF_DATA)
        tempFile.deleteOnExit()

        mockMultipartFile = MockMultipartFile(
            "laillistamistodistus",
            AsiakirjaHelper.ASIAKIRJA_PDF_NIMI,
            AsiakirjaHelper.ASIAKIRJA_PDF_TYYPPI,
            tempFile.readBytes()
        )
    }

    companion object {
        private const val VASTUUHENKILO_ARVIOIJA_NIMIKE = "arvioija"
        private const val VASTUUHENKILO_HYVAKSYJA_NIMIKE = "hyvaksyja"
    }
}
