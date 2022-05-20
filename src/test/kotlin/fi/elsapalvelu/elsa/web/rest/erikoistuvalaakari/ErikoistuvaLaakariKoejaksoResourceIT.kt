package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.VastuuhenkilonTehtavatyyppiEnum
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.mapper.*
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoisalaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.OpintooikeusHelper
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.core.IsNull
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
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager
import kotlin.test.assertNotNull

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class ErikoistuvaLaakariKoejaksoResourceIT {

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
    private lateinit var koejaksonVastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository

    @Autowired
    private lateinit var kayttajaRepository: KayttajaRepository

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
    private lateinit var koejaksonVastuuhenkilonArvioMapper: KoejaksonVastuuhenkilonArvioMapper

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restKoejaksoMockMvc: MockMvc

    private lateinit var koejaksonKoulutussopimus: KoejaksonKoulutussopimus

    private lateinit var koejaksonAloituskeskustelu: KoejaksonAloituskeskustelu

    private lateinit var koejaksonValiarviointi: KoejaksonValiarviointi

    private lateinit var koejaksonKehittamistoimenpiteet: KoejaksonKehittamistoimenpiteet

    private lateinit var koejaksonLoppukeskustelu: KoejaksonLoppukeskustelu

    private lateinit var koejaksonVastuuhenkilonArvio: KoejaksonVastuuhenkilonArvio

    private lateinit var user: User

    private lateinit var erikoistuvaLaakari: ErikoistuvaLaakari

    private lateinit var vastuuhenkilo: Kayttaja

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @Transactional
    fun getKoejaksonVaiheet() {
        initKoejakso()

        em.persist(koejaksonKoulutussopimus)
        em.persist(koejaksonAloituskeskustelu)
        em.persist(koejaksonValiarviointi)
        em.persist(koejaksonKehittamistoimenpiteet)
        em.persist(koejaksonLoppukeskustelu)
        em.persist(koejaksonVastuuhenkilonArvio)

        restKoejaksoMockMvc.perform(
            get(
                "/api/erikoistuva-laakari/koejakso"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.koulutussopimus").value(IsNull.notNullValue()))
            .andExpect(jsonPath("$.aloituskeskustelu").value(IsNull.notNullValue()))
            .andExpect(jsonPath("$.valiarviointi").value(IsNull.notNullValue()))
            .andExpect(jsonPath("$.kehittamistoimenpiteet").value(IsNull.notNullValue()))
            .andExpect(jsonPath("$.loppukeskustelu").value(IsNull.notNullValue()))
            .andExpect(jsonPath("$.vastuuhenkilonArvio").value(IsNull.notNullValue()))

        em.clear()
    }

    @Test
    @Transactional
    fun getNullKoejaksonVaiheetForAnotherOpintooikeus() {
        initKoejakso()

        em.persist(koejaksonKoulutussopimus)
        em.persist(koejaksonAloituskeskustelu)
        em.persist(koejaksonValiarviointi)
        em.persist(koejaksonKehittamistoimenpiteet)
        em.persist(koejaksonLoppukeskustelu)
        em.persist(koejaksonVastuuhenkilonArvio)

        val newOpintooikeus =
            OpintooikeusHelper.addOpintooikeusForErikoistuvaLaakari(em, erikoistuvaLaakari)
        OpintooikeusHelper.setOpintooikeusKaytossa(erikoistuvaLaakari, newOpintooikeus)

        restKoejaksoMockMvc.perform(
            get(
                "/api/erikoistuva-laakari/koejakso"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.koulutussopimus").value(IsNull.nullValue()))
            .andExpect(jsonPath("$.aloituskeskustelu").value(IsNull.nullValue()))
            .andExpect(jsonPath("$.valiarviointi").value(IsNull.nullValue()))
            .andExpect(jsonPath("$.kehittamistoimenpiteet").value(IsNull.nullValue()))
            .andExpect(jsonPath("$.loppukeskustelu").value(IsNull.nullValue()))
            .andExpect(jsonPath("$.vastuuhenkilonArvio").value(IsNull.nullValue()))

        em.clear()
    }

    @Test
    @Transactional
    fun shouldReturnKoulutussopimusLomakeWithVastuuhenkiloByTehtavatyyppi() {
        initKoejakso()

        createVastuuhenkilo(addTehtavatyyppiForKoejakso = false)

        restKoejaksoMockMvc.perform(
            get(
                "/api/erikoistuva-laakari/koulutussopimus-lomake"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.vastuuhenkilo.id").value(vastuuhenkilo.id))
    }

    @Test
    @Transactional
    fun shouldThrowExceptionIfMultipleVastuuhenkiloWithSameYliopistoErikoisalaAndTehtavatyyppiExists() {
        initKoejakso()
        createVastuuhenkilo(addTehtavatyyppiForKoejakso = true)

        restKoejaksoMockMvc.perform(
            get(
                "/api/erikoistuva-laakari/koulutussopimus-lomake"
            )
        )
            .andExpect(status().is5xxServerError)
    }

    @Test
    @Transactional
    fun createKoulutussopimusWithExistingId() {
        initKoejakso()

        koejaksonKoulutussopimusRepository.saveAndFlush(koejaksonKoulutussopimus)

        val koejaksonKoulutussopimusDTO =
            koejaksonKoulutussopimusMapper.toDto(koejaksonKoulutussopimus)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/koulutussopimus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonKoulutussopimusDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val sopimus = koejaksonKoulutussopimusRepository.findById(koejaksonKoulutussopimus.id!!)
        assertThat(sopimus.get().muokkauspaiva).isEqualTo(koejaksonKoulutussopimus.muokkauspaiva)
    }

    @Test
    @Transactional
    fun createKoulutussopimusWithKouluttajaAck() {
        initKoejakso()

        val kouluttaja = koejaksonKoulutussopimus.kouluttajat?.iterator()?.next()
        kouluttaja?.sopimusHyvaksytty = true
        kouluttaja?.kuittausaika = LocalDate.now()

        val databaseSizeBeforeCreate = koejaksonKoulutussopimusRepository.findAll().size

        val koejaksonKoulutussopimusDTO =
            koejaksonKoulutussopimusMapper.toDto(koejaksonKoulutussopimus)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/koulutussopimus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonKoulutussopimusDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val koulutussopimusList = koejaksonKoulutussopimusRepository.findAll()
        assertThat(koulutussopimusList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createKoulutussopimusWithVastuuhenkiloAck() {
        initKoejakso()

        koejaksonKoulutussopimus.vastuuhenkilonKuittausaika = LocalDate.now()

        val databaseSizeBeforeCreate = koejaksonKoulutussopimusRepository.findAll().size

        val koejaksonKoulutussopimusDTO =
            koejaksonKoulutussopimusMapper.toDto(koejaksonKoulutussopimus)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/koulutussopimus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonKoulutussopimusDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val koulutussopimusList = koejaksonKoulutussopimusRepository.findAll()
        assertThat(koulutussopimusList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createKoulutussopimus() {
        initKoejakso()

        val databaseSizeBeforeCreate = koejaksonKoulutussopimusRepository.findAll().size

        val koejaksonKoulutussopimusDTO =
            koejaksonKoulutussopimusMapper.toDto(koejaksonKoulutussopimus)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/koulutussopimus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonKoulutussopimusDTO))
                .with(csrf())
        ).andExpect(status().isCreated)

        val koulutussopimusList = koejaksonKoulutussopimusRepository.findAll()
        assertThat(koulutussopimusList).hasSize(databaseSizeBeforeCreate + 1)
        val sopimus = koulutussopimusList[koulutussopimusList.size - 1]
        assertThat(sopimus.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()).isEqualTo(
            koejaksonKoulutussopimusDTO.erikoistuvanNimi
        )
        assertThat(sopimus.opintooikeus?.opiskelijatunnus).isEqualTo(koejaksonKoulutussopimusDTO.erikoistuvanOpiskelijatunnus)
        assertThat(sopimus.opintooikeus?.erikoistuvaLaakari?.syntymaaika).isEqualTo(
            koejaksonKoulutussopimusDTO.erikoistuvanSyntymaaika
        )
        assertThat(sopimus.opintooikeus?.yliopisto?.nimi.toString()).isEqualTo(
            koejaksonKoulutussopimusDTO.erikoistuvanYliopisto
        )
        assertThat(sopimus.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.phoneNumber).isEqualTo(
            koejaksonKoulutussopimusDTO.erikoistuvanPuhelinnumero
        )
        assertThat(sopimus.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.email).isEqualTo(
            koejaksonKoulutussopimusDTO.erikoistuvanSahkoposti
        )
        assertThat(sopimus.opintooikeus?.opintooikeudenMyontamispaiva).isEqualTo(
            koejaksonKoulutussopimusDTO.opintooikeudenMyontamispaiva
        )
        assertThat(sopimus.koejaksonAlkamispaiva).isEqualTo(koejaksonKoulutussopimusDTO.koejaksonAlkamispaiva)
        assertThat(sopimus.lahetetty).isEqualTo(koejaksonKoulutussopimusDTO.lahetetty)
        assertThat(sopimus.muokkauspaiva).isNotNull
        assertThat(sopimus.vastuuhenkilo?.id).isEqualTo(koejaksonKoulutussopimusDTO.vastuuhenkilo?.id)
        assertThat(sopimus.vastuuhenkiloHyvaksynyt).isFalse
        assertThat(sopimus.vastuuhenkilonKuittausaika).isNull()
        assertThat(sopimus.korjausehdotus).isNull()
        assertThat(sopimus.kouluttajat).hasSize(1)
        val kouluttaja = sopimus.kouluttajat?.iterator()?.next()
        val kouluttajaDTO = koejaksonKoulutussopimusDTO.kouluttajat?.iterator()?.next()
        assertThat(kouluttaja?.kouluttaja?.id).isEqualTo(kouluttajaDTO?.kayttajaId)
        assertThat(kouluttaja?.toimipaikka).isNull()
        assertThat(kouluttaja?.lahiosoite).isNull()
        assertThat(kouluttaja?.postitoimipaikka).isNull()
        assertThat(kouluttaja?.sopimusHyvaksytty).isFalse
        assertThat(kouluttaja?.kuittausaika).isNull()
        assertThat(sopimus.koulutuspaikat).hasSize(1)
        val koulutuspaikka = sopimus.koulutuspaikat?.iterator()?.next()
        val koulutuspaikkaDTO = koejaksonKoulutussopimusDTO.koulutuspaikat?.iterator()?.next()
        assertThat(koulutuspaikka?.nimi).isEqualTo(koulutuspaikkaDTO?.nimi)
        assertThat(koulutuspaikka?.yliopisto?.id).isEqualTo(koulutuspaikkaDTO?.yliopistoId)
    }

    @Test
    @Transactional
    fun shouldNotCreateKoulutussopimusWithDifferentVastuuhenkiloThanAllowed() {
        initKoejakso()

        val erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)
        val vastuuhenkilo = createVastuuhenkilo(erikoisala = erikoisala)
        koejaksonKoulutussopimus.vastuuhenkilo = vastuuhenkilo

        val koejaksonKoulutussopimusDTO =
            koejaksonKoulutussopimusMapper.toDto(koejaksonKoulutussopimus)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/koulutussopimus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonKoulutussopimusDTO))
                .with(csrf())
        ).andExpect(status().is5xxServerError)
    }

    @Test
    @Transactional
    fun updateKoulutussopimus() {
        initKoejakso()

        koejaksonKoulutussopimusRepository.saveAndFlush(koejaksonKoulutussopimus)

        val databaseSizeBeforeUpdate = koejaksonKoulutussopimusRepository.findAll().size

        val id = koejaksonKoulutussopimus.id
        assertNotNull(id)
        val updatedKoulutussopimus = koejaksonKoulutussopimusRepository.findById(id).get()
        em.detach(updatedKoulutussopimus)

        updatedKoulutussopimus.koejaksonAlkamispaiva = UPDATED_ALKAMISPAIVA
        updatedKoulutussopimus.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.phoneNumber =
            UPDATED_PHONE

        val updatedKoulutuspaikka = updatedKoulutussopimus.koulutuspaikat?.iterator()?.next()
        updatedKoulutuspaikka?.nimi = UPDATED_KOULUTUSPAIKKA

        val updatedKouluttaja = KayttajaHelper.createUpdatedEntity(
            em,
            UPDATED_KOULUTTAJA_NIMI
        )
        em.persist(updatedKouluttaja)
        updatedKoulutussopimus.kouluttajat?.add(
            createKoulutussopimuksenKouluttaja(
                updatedKoulutussopimus,
                updatedKouluttaja
            )
        )

        val koulutussopimusDTO = koejaksonKoulutussopimusMapper.toDto(updatedKoulutussopimus)

        restKoejaksoMockMvc.perform(
            put("/api/erikoistuva-laakari/koejakso/koulutussopimus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koulutussopimusDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val koulutussopimusList = koejaksonKoulutussopimusRepository.findAll()
        assertThat(koulutussopimusList).hasSize(databaseSizeBeforeUpdate)
        val testKoulutussopimus = koulutussopimusList[koulutussopimusList.size - 1]
        assertThat(testKoulutussopimus.koejaksonAlkamispaiva).isEqualTo(UPDATED_ALKAMISPAIVA)
        assertThat(testKoulutussopimus.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.phoneNumber).isEqualTo(
            UPDATED_PHONE
        )

        val testKoulutuspaikka = testKoulutussopimus.koulutuspaikat?.iterator()?.next()
        assertThat(testKoulutuspaikka?.nimi).isEqualTo(UPDATED_KOULUTUSPAIKKA)

        assertThat(testKoulutussopimus.kouluttajat).hasSize(2)
        val testKouluttaja =
            testKoulutussopimus.kouluttajat?.filter { it.kouluttaja?.id == updatedKouluttaja.id }
                ?.get(0)
        assertThat(testKouluttaja?.kouluttaja?.user?.id).isEqualTo(updatedKouluttaja.user?.id)
    }

    @Test
    @Transactional
    fun shouldNotUpdateKoulutussopimusWithDifferentVastuuhenkilo() {
        initKoejakso()

        koejaksonKoulutussopimusRepository.saveAndFlush(koejaksonKoulutussopimus)

        val id = koejaksonKoulutussopimus.id
        assertNotNull(id)
        val updatedKoulutussopimus = koejaksonKoulutussopimusRepository.findById(id).get()
        em.detach(updatedKoulutussopimus)

        val erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)
        val updatedVastuuhenkilo = createVastuuhenkilo(erikoisala = erikoisala)
        updatedKoulutussopimus.vastuuhenkilo = updatedVastuuhenkilo

        val koulutussopimusDTO = koejaksonKoulutussopimusMapper.toDto(updatedKoulutussopimus)

        restKoejaksoMockMvc.perform(
            put("/api/erikoistuva-laakari/koejakso/koulutussopimus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koulutussopimusDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun createAloituskeskusteluWithExistingId() {
        initKoejakso()

        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        val koejaksonAloituskeskusteluDTO =
            koejaksonAloituskeskusteluMapper.toDto(koejaksonAloituskeskustelu)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/aloituskeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonAloituskeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val keskustelu =
            koejaksonAloituskeskusteluRepository.findById(koejaksonAloituskeskustelu.id!!)
        assertThat(keskustelu.get().muokkauspaiva).isEqualTo(koejaksonAloituskeskustelu.muokkauspaiva)
    }

    @Test
    @Transactional
    fun createAloituskeskusteluWithLahikouluttajaAck() {
        initKoejakso()

        koejaksonAloituskeskustelu.lahikouluttajaHyvaksynyt = true
        koejaksonAloituskeskustelu.lahikouluttajanKuittausaika = LocalDate.now()

        val databaseSizeBeforeCreate = koejaksonAloituskeskusteluRepository.findAll().size

        val koejaksonAloituskeskusteluDTO =
            koejaksonAloituskeskusteluMapper.toDto(koejaksonAloituskeskustelu)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/aloituskeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonAloituskeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val aloituskeskusteluList = koejaksonAloituskeskusteluRepository.findAll()
        assertThat(aloituskeskusteluList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createAloituskeskusteluWithLahiesimiesAck() {
        initKoejakso()

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()

        val databaseSizeBeforeCreate = koejaksonAloituskeskusteluRepository.findAll().size

        val koejaksonAloituskeskusteluDTO =
            koejaksonAloituskeskusteluMapper.toDto(koejaksonAloituskeskustelu)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/aloituskeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonAloituskeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val aloituskeskusteluList = koejaksonAloituskeskusteluRepository.findAll()
        assertThat(aloituskeskusteluList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createAloituskeskustelu() {
        initKoejakso()

        val databaseSizeBeforeCreate = koejaksonAloituskeskusteluRepository.findAll().size

        val koejaksonAloituskeskusteluDTO =
            koejaksonAloituskeskusteluMapper.toDto(koejaksonAloituskeskustelu)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/aloituskeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonAloituskeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isCreated)

        val aloituskeskusteluList = koejaksonAloituskeskusteluRepository.findAll()
        assertThat(aloituskeskusteluList).hasSize(databaseSizeBeforeCreate + 1)
        val keskustelu = aloituskeskusteluList[aloituskeskusteluList.size - 1]
        assertThat(keskustelu.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()).isEqualTo(
            koejaksonAloituskeskusteluDTO.erikoistuvanNimi
        )
        assertThat(keskustelu.opintooikeus?.erikoisala?.nimi).isEqualTo(
            koejaksonAloituskeskusteluDTO.erikoistuvanErikoisala
        )
        assertThat(keskustelu.opintooikeus?.opiskelijatunnus).isEqualTo(
            koejaksonAloituskeskusteluDTO.erikoistuvanOpiskelijatunnus
        )
        assertThat(keskustelu.opintooikeus?.yliopisto?.nimi.toString()).isEqualTo(
            koejaksonAloituskeskusteluDTO.erikoistuvanYliopisto
        )
        assertThat(keskustelu.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.email).isEqualTo(
            koejaksonAloituskeskusteluDTO.erikoistuvanSahkoposti
        )
        assertThat(keskustelu.koejaksonSuorituspaikka).isEqualTo(koejaksonAloituskeskusteluDTO.koejaksonSuorituspaikka)
        assertThat(keskustelu.koejaksonToinenSuorituspaikka).isEqualTo(koejaksonAloituskeskusteluDTO.koejaksonToinenSuorituspaikka)
        assertThat(keskustelu.koejaksonAlkamispaiva).isEqualTo(koejaksonAloituskeskusteluDTO.koejaksonAlkamispaiva)
        assertThat(keskustelu.koejaksonPaattymispaiva).isEqualTo(koejaksonAloituskeskusteluDTO.koejaksonPaattymispaiva)
        assertThat(keskustelu.suoritettuKokoaikatyossa).isEqualTo(koejaksonAloituskeskusteluDTO.suoritettuKokoaikatyossa)
        assertThat(keskustelu.tyotunnitViikossa).isEqualTo(koejaksonAloituskeskusteluDTO.tyotunnitViikossa)
        assertThat(keskustelu.lahikouluttaja?.id).isEqualTo(koejaksonAloituskeskusteluDTO.lahikouluttaja?.id)
        assertThat(keskustelu.lahiesimies?.id).isEqualTo(koejaksonAloituskeskusteluDTO.lahiesimies?.id)
        assertThat(keskustelu.koejaksonOsaamistavoitteet).isEqualTo(koejaksonAloituskeskusteluDTO.koejaksonOsaamistavoitteet)
        assertThat(keskustelu.lahetetty).isEqualTo(koejaksonAloituskeskusteluDTO.lahetetty)
        assertThat(keskustelu.muokkauspaiva).isNotNull
    }

    @Test
    @Transactional
    fun updateAloituskeskustelu() {
        initKoejakso()

        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        val databaseSizeBeforeUpdate = koejaksonAloituskeskusteluRepository.findAll().size

        val id = koejaksonAloituskeskustelu.id
        assertNotNull(id)
        val updatedAloituskeskustelu = koejaksonAloituskeskusteluRepository.findById(id).get()
        em.detach(updatedAloituskeskustelu)

        updatedAloituskeskustelu.koejaksonAlkamispaiva = UPDATED_ALKAMISPAIVA
        updatedAloituskeskustelu.koejaksonPaattymispaiva = UPDATED_PAATTYMISPAIVA

        val updatedKouluttaja = KayttajaHelper.createUpdatedEntity(
            em,
            UPDATED_VASTUUHENKILO_NIMI
        )
        em.persist(updatedKouluttaja)
        updatedAloituskeskustelu.lahikouluttaja = updatedKouluttaja

        val aloituskeskusteluDTO = koejaksonAloituskeskusteluMapper.toDto(updatedAloituskeskustelu)

        aloituskeskusteluDTO.erikoistuvanSahkoposti = UPDATED_EMAIL

        restKoejaksoMockMvc.perform(
            put("/api/erikoistuva-laakari/koejakso/aloituskeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(aloituskeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val aloituskeskusteluList = koejaksonAloituskeskusteluRepository.findAll()
        assertThat(aloituskeskusteluList).hasSize(databaseSizeBeforeUpdate)
        val testAloituskeskustelu = aloituskeskusteluList[aloituskeskusteluList.size - 1]
        assertThat(testAloituskeskustelu.koejaksonAlkamispaiva).isEqualTo(UPDATED_ALKAMISPAIVA)
        assertThat(testAloituskeskustelu.koejaksonPaattymispaiva).isEqualTo(UPDATED_PAATTYMISPAIVA)
        assertThat(testAloituskeskustelu.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.email).isEqualTo(
            UPDATED_EMAIL
        )

        val testLahikouluttaja =
            kayttajaRepository.findById(testAloituskeskustelu.lahikouluttaja?.id!!)
        assertThat(testLahikouluttaja.get().user?.id).isEqualTo(updatedKouluttaja.user?.id)
    }

    @Test
    @Transactional
    fun createValiarviointi() {
        initKoejakso()

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        val databaseSizeBeforeCreate = koejaksonValiarviointiRepository.findAll().size

        val koejaksonValiarviointiDTO =
            koejaksonValiarviointiMapper.toDto(koejaksonValiarviointi)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/valiarviointi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonValiarviointiDTO))
                .with(csrf())
        ).andExpect(status().isCreated)

        val valiarviointiList = koejaksonValiarviointiRepository.findAll()
        assertThat(valiarviointiList).hasSize(databaseSizeBeforeCreate + 1)
        val arviointi = valiarviointiList[valiarviointiList.size - 1]
        assertThat(arviointi.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()).isEqualTo(
            koejaksonValiarviointiDTO.erikoistuvanNimi
        )
        assertThat(arviointi.opintooikeus?.erikoisala?.nimi).isEqualTo(koejaksonValiarviointiDTO.erikoistuvanErikoisala)
        assertThat(arviointi.opintooikeus?.opiskelijatunnus).isEqualTo(koejaksonValiarviointiDTO.erikoistuvanOpiskelijatunnus)
        assertThat(arviointi.opintooikeus?.yliopisto?.nimi.toString()).isEqualTo(
            koejaksonValiarviointiDTO.erikoistuvanYliopisto
        )
        assertThat(arviointi.lahikouluttaja?.id).isEqualTo(koejaksonValiarviointiDTO.lahikouluttaja?.id)
        assertThat(arviointi.lahiesimies?.id).isEqualTo(koejaksonValiarviointiDTO.lahiesimies?.id)
        assertThat(arviointi.muokkauspaiva).isNotNull
    }

    @Test
    @Transactional
    fun createValiarviointiWithoutAloituskeskustelu() {
        initKoejakso()

        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        val databaseSizeBeforeCreate = koejaksonValiarviointiRepository.findAll().size

        val koejaksonValiarviointiDTO =
            koejaksonValiarviointiMapper.toDto(koejaksonValiarviointi)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/valiarviointi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonValiarviointiDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val valiarviointiList = koejaksonValiarviointiRepository.findAll()
        assertThat(valiarviointiList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createKehittamistoimenpiteet() {
        initKoejakso()

        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)
        koejaksonValiarviointi.lahiesimiesHyvaksynyt = true
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        val databaseSizeBeforeCreate = koejaksonKehittamistoimenpiteetRepository.findAll().size

        val koejaksonKehittamistoimenpiteetDTO =
            koejaksonKehittamistoimenpiteetMapper.toDto(koejaksonKehittamistoimenpiteet)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/kehittamistoimenpiteet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonKehittamistoimenpiteetDTO))
                .with(csrf())
        ).andExpect(status().isCreated)

        val kehittamistoimenpiteetList = koejaksonKehittamistoimenpiteetRepository.findAll()
        assertThat(kehittamistoimenpiteetList).hasSize(databaseSizeBeforeCreate + 1)
        val arviointi = kehittamistoimenpiteetList[kehittamistoimenpiteetList.size - 1]
        assertThat(arviointi.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()).isEqualTo(
            koejaksonKehittamistoimenpiteetDTO.erikoistuvanNimi
        )
        assertThat(arviointi.opintooikeus?.erikoisala?.nimi).isEqualTo(
            koejaksonKehittamistoimenpiteetDTO.erikoistuvanErikoisala
        )
        assertThat(arviointi.opintooikeus?.opiskelijatunnus).isEqualTo(
            koejaksonKehittamistoimenpiteetDTO.erikoistuvanOpiskelijatunnus
        )
        assertThat(arviointi.opintooikeus?.yliopisto?.nimi.toString()).isEqualTo(
            koejaksonKehittamistoimenpiteetDTO.erikoistuvanYliopisto
        )
        assertThat(arviointi.lahikouluttaja?.id).isEqualTo(koejaksonKehittamistoimenpiteetDTO.lahikouluttaja?.id)
        assertThat(arviointi.lahiesimies?.id).isEqualTo(koejaksonKehittamistoimenpiteetDTO.lahiesimies?.id)
        assertThat(arviointi.muokkauspaiva).isNotNull
    }

    @Test
    @Transactional
    fun createKehittamistoimenpiteetWithoutValiarviointi() {
        initKoejakso()

        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        val databaseSizeBeforeCreate = koejaksonKehittamistoimenpiteetRepository.findAll().size

        val koejaksonKehittamistoimenpiteetDTO =
            koejaksonKehittamistoimenpiteetMapper.toDto(koejaksonKehittamistoimenpiteet)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/kehittamistoimenpiteet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonKehittamistoimenpiteetDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val kehittamistoimenpiteetList = koejaksonKehittamistoimenpiteetRepository.findAll()
        assertThat(kehittamistoimenpiteetList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createLoppukeskusteluWithKehittamistoimenpiteet() {
        initKoejakso()

        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)
        koejaksonValiarviointi.lahiesimiesHyvaksynyt = true
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        koejaksonKehittamistoimenpiteet.lahiesimiesHyvaksynyt = true
        koejaksonKehittamistoimenpiteet.muokkauspaiva = LocalDate.now()
        koejaksonKehittamistoimenpiteetRepository.saveAndFlush(koejaksonKehittamistoimenpiteet)

        val databaseSizeBeforeCreate = koejaksonLoppukeskusteluRepository.findAll().size

        val koejaksonLoppukeskusteluDTO =
            koejaksonLoppukeskusteluMapper.toDto(koejaksonLoppukeskustelu)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/loppukeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonLoppukeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isCreated)

        val loppukeskusteluList = koejaksonLoppukeskusteluRepository.findAll()
        assertThat(loppukeskusteluList).hasSize(databaseSizeBeforeCreate + 1)
        val arviointi = loppukeskusteluList[loppukeskusteluList.size - 1]
        assertThat(arviointi.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()).isEqualTo(
            koejaksonLoppukeskusteluDTO.erikoistuvanNimi
        )
        assertThat(arviointi.opintooikeus?.erikoisala?.nimi).isEqualTo(koejaksonLoppukeskusteluDTO.erikoistuvanErikoisala)
        assertThat(arviointi.opintooikeus?.opiskelijatunnus).isEqualTo(
            koejaksonLoppukeskusteluDTO.erikoistuvanOpiskelijatunnus
        )
        assertThat(arviointi.opintooikeus?.yliopisto?.nimi.toString()).isEqualTo(
            koejaksonLoppukeskusteluDTO.erikoistuvanYliopisto
        )
        assertThat(arviointi.lahikouluttaja?.id).isEqualTo(koejaksonLoppukeskusteluDTO.lahikouluttaja?.id)
        assertThat(arviointi.lahiesimies?.id).isEqualTo(koejaksonLoppukeskusteluDTO.lahiesimies?.id)
        assertThat(arviointi.muokkauspaiva).isNotNull
    }

    @Test
    @Transactional
    fun createLoppukeskusteluWithKehittamistoimenpiteetInvalid() {
        initKoejakso()

        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)
        koejaksonValiarviointi.lahiesimiesHyvaksynyt = true
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        koejaksonKehittamistoimenpiteetRepository.saveAndFlush(koejaksonKehittamistoimenpiteet)

        val databaseSizeBeforeCreate = koejaksonLoppukeskusteluRepository.findAll().size

        val koejaksonLoppukeskusteluDTO =
            koejaksonLoppukeskusteluMapper.toDto(koejaksonLoppukeskustelu)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/loppukeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonLoppukeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val loppukeskusteluList = koejaksonLoppukeskusteluRepository.findAll()
        assertThat(loppukeskusteluList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createLoppukeskusteluWithoutKehittamistoimenpiteet() {
        initKoejakso()

        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)
        koejaksonValiarviointi.lahiesimiesHyvaksynyt = true
        koejaksonValiarviointi.edistyminenTavoitteidenMukaista = true
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        val databaseSizeBeforeCreate = koejaksonLoppukeskusteluRepository.findAll().size

        val koejaksonLoppukeskusteluDTO =
            koejaksonLoppukeskusteluMapper.toDto(koejaksonLoppukeskustelu)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/loppukeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonLoppukeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isCreated)

        val loppukeskusteluList = koejaksonLoppukeskusteluRepository.findAll()
        assertThat(loppukeskusteluList).hasSize(databaseSizeBeforeCreate + 1)
        val arviointi = loppukeskusteluList[loppukeskusteluList.size - 1]
        assertThat(arviointi.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()).isEqualTo(
            koejaksonLoppukeskusteluDTO.erikoistuvanNimi
        )
        assertThat(arviointi.opintooikeus?.erikoisala?.nimi).isEqualTo(koejaksonLoppukeskusteluDTO.erikoistuvanErikoisala)
        assertThat(arviointi.opintooikeus?.opiskelijatunnus).isEqualTo(
            koejaksonLoppukeskusteluDTO.erikoistuvanOpiskelijatunnus
        )
        assertThat(arviointi.opintooikeus?.yliopisto?.nimi.toString()).isEqualTo(
            koejaksonLoppukeskusteluDTO.erikoistuvanYliopisto
        )
        assertThat(arviointi.lahikouluttaja?.id).isEqualTo(koejaksonLoppukeskusteluDTO.lahikouluttaja?.id)
        assertThat(arviointi.lahiesimies?.id).isEqualTo(koejaksonLoppukeskusteluDTO.lahiesimies?.id)
        assertThat(arviointi.muokkauspaiva).isNotNull
    }

    @Test
    @Transactional
    fun createLoppukeskusteluWithoutKehittamistoimenpiteetInvalid() {
        initKoejakso()

        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)
        koejaksonValiarviointi.lahiesimiesHyvaksynyt = true
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        val databaseSizeBeforeCreate = koejaksonLoppukeskusteluRepository.findAll().size

        val koejaksonLoppukeskusteluDTO =
            koejaksonLoppukeskusteluMapper.toDto(koejaksonLoppukeskustelu)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/loppukeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonLoppukeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val loppukeskusteluList = koejaksonLoppukeskusteluRepository.findAll()
        assertThat(loppukeskusteluList).hasSize(databaseSizeBeforeCreate)
    }

    @Test
    @Transactional
    fun createVastuuhenkilonArvio() {
        initKoejakso()

        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)
        koejaksonKehittamistoimenpiteetRepository.saveAndFlush(koejaksonKehittamistoimenpiteet)
        koejaksonLoppukeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonLoppukeskusteluRepository.saveAndFlush(koejaksonLoppukeskustelu)

        val databaseSizeBeforeCreate = koejaksonVastuuhenkilonArvioRepository.findAll().size

        val koejaksonVastuuhenkilonArvioDTO =
            koejaksonVastuuhenkilonArvioMapper.toDto(koejaksonVastuuhenkilonArvio)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/vastuuhenkilonarvio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonVastuuhenkilonArvioDTO))
                .with(csrf())
        ).andExpect(status().isCreated)

        val vastuuhenkilonArvioList = koejaksonVastuuhenkilonArvioRepository.findAll()
        assertThat(vastuuhenkilonArvioList).hasSize(databaseSizeBeforeCreate + 1)
        val arviointi = vastuuhenkilonArvioList[vastuuhenkilonArvioList.size - 1]
        assertThat(arviointi.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()).isEqualTo(
            koejaksonVastuuhenkilonArvioDTO.erikoistuvanNimi
        )
        assertThat(arviointi.opintooikeus?.erikoisala?.nimi).isEqualTo(
            koejaksonVastuuhenkilonArvioDTO.erikoistuvanErikoisala
        )
        assertThat(arviointi.opintooikeus?.opiskelijatunnus).isEqualTo(
            koejaksonVastuuhenkilonArvioDTO.erikoistuvanOpiskelijatunnus
        )
        assertThat(arviointi.opintooikeus?.yliopisto?.nimi.toString()).isEqualTo(
            koejaksonVastuuhenkilonArvioDTO.erikoistuvanYliopisto
        )
        assertThat(arviointi.vastuuhenkilo?.id).isEqualTo(koejaksonVastuuhenkilonArvioDTO.vastuuhenkilo?.id)
        assertThat(arviointi.muokkauspaiva).isNotNull
    }

    @Test
    @Transactional
    fun shouldReturnVastuuhenkilonArvioLomakeWithVastuuhenkiloByTehtavatyyppi() {
        initKoejakso()

        createVastuuhenkilo(addTehtavatyyppiForKoejakso = false)

        restKoejaksoMockMvc.perform(
            get(
                "/api/erikoistuva-laakari/vastuuhenkilonarvio-lomake"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.vastuuhenkilo.id").value(vastuuhenkilo.id))
    }

    @Test
    @Transactional
    fun shouldNotCreateVastuuhenkilonArvioWithDifferentVastuuhenkiloThanAllowed() {
        initKoejakso()

        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)
        koejaksonKehittamistoimenpiteetRepository.saveAndFlush(koejaksonKehittamistoimenpiteet)
        koejaksonLoppukeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonLoppukeskusteluRepository.saveAndFlush(koejaksonLoppukeskustelu)

        val erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)
        val updatedVastuuhenkilo = createVastuuhenkilo(erikoisala = erikoisala)
        koejaksonVastuuhenkilonArvio.vastuuhenkilo = updatedVastuuhenkilo

        val koejaksonVastuuhenkilonArvioDTO =
            koejaksonVastuuhenkilonArvioMapper.toDto(koejaksonVastuuhenkilonArvio)

        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/vastuuhenkilonarvio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonVastuuhenkilonArvioDTO))
                .with(csrf())
        ).andExpect(status().is5xxServerError)
    }

    @Test
    @Transactional
    fun shouldNotUpdateVastuuhenkilonArvioWithDifferentVastuuhenkilo() {
        initKoejakso()

        koejaksonVastuuhenkilonArvioRepository.saveAndFlush(koejaksonVastuuhenkilonArvio)

        val id = koejaksonVastuuhenkilonArvio.id
        assertNotNull(id)
        val updatedVastuuhenkilonArvio = koejaksonVastuuhenkilonArvioRepository.findById(id).get()
        em.detach(updatedVastuuhenkilonArvio)

        val erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)
        val updatedVastuuhenkilo = createVastuuhenkilo(erikoisala = erikoisala)
        updatedVastuuhenkilonArvio.vastuuhenkilo = updatedVastuuhenkilo

        val vastuuhenkilonArvioDTO = koejaksonVastuuhenkilonArvioMapper.toDto(updatedVastuuhenkilonArvio)

        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/vastuuhenkilonarvio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(vastuuhenkilonArvioDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun createVastuuhenkilonArvioWithoutLoppukeskustelu() {
        initKoejakso()

        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)
        koejaksonKehittamistoimenpiteetRepository.saveAndFlush(koejaksonKehittamistoimenpiteet)
        koejaksonLoppukeskusteluRepository.saveAndFlush(koejaksonLoppukeskustelu)

        val databaseSizeBeforeCreate = koejaksonVastuuhenkilonArvioRepository.findAll().size

        val koejaksonVastuuhenkilonArvioDTO =
            koejaksonVastuuhenkilonArvioMapper.toDto(koejaksonVastuuhenkilonArvio)
        restKoejaksoMockMvc.perform(
            post("/api/erikoistuva-laakari/koejakso/vastuuhenkilonarvio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koejaksonVastuuhenkilonArvioDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val vastuuhenkilonArvioList = koejaksonVastuuhenkilonArvioRepository.findAll()
        assertThat(vastuuhenkilonArvioList).hasSize(databaseSizeBeforeCreate)
    }

    private fun initKoejakso(userId: String? = DEFAULT_ID) {
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
        erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
        em.persist(erikoistuvaLaakari)

        vastuuhenkilo = createVastuuhenkilo()

        val kouluttaja = KayttajaHelper.createEntity(em)
        em.persist(kouluttaja)

        val esimies = KayttajaHelper.createEntity(em)
        em.persist(esimies)

        val yliopisto = Yliopisto(nimi = DEFAULT_YLIOPISTO)
        em.persist(yliopisto)

        koejaksonKoulutussopimus = createKoulutussopimus(erikoistuvaLaakari, vastuuhenkilo)
        koejaksonKoulutussopimus.kouluttajat =
            mutableSetOf(createKoulutussopimuksenKouluttaja(koejaksonKoulutussopimus, kouluttaja))
        koejaksonKoulutussopimus.koulutuspaikat =
            mutableSetOf(
                createKoulutussopimuksenKoulutuspaikka(
                    koejaksonKoulutussopimus,
                    yliopisto
                )
            )

        koejaksonAloituskeskustelu =
            createAloituskeskustelu(erikoistuvaLaakari, kouluttaja, esimies)

        koejaksonValiarviointi =
            createValiarviointi(erikoistuvaLaakari, kouluttaja, esimies)

        koejaksonKehittamistoimenpiteet =
            createKehittamistoimenpiteet(erikoistuvaLaakari, kouluttaja, esimies)

        koejaksonLoppukeskustelu = createLoppukeskustelu(erikoistuvaLaakari, kouluttaja, esimies)

        koejaksonVastuuhenkilonArvio = createVastuuhenkilonArvio(erikoistuvaLaakari, vastuuhenkilo)
    }

    private fun createVastuuhenkilo(
        yliopisto: Yliopisto? = null,
        erikoisala: Erikoisala? = null,
        addTehtavatyyppiForKoejakso: Boolean = true
    ): Kayttaja {
        val vastuuhenkiloUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(
                VASTUUHENKILO
            )
        )
        em.persist(vastuuhenkiloUser)

        val vastuuhenkilo = KayttajaHelper.createEntity(em, vastuuhenkiloUser)
        em.persist(vastuuhenkilo)

        val opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()
        val yliopistoErikoisala = KayttajaYliopistoErikoisala(
                kayttaja = vastuuhenkilo,
                yliopisto = yliopisto ?: opintooikeus?.yliopisto,
                erikoisala = erikoisala ?: opintooikeus?.erikoisala
            )
        em.persist(yliopistoErikoisala)

        if (addTehtavatyyppiForKoejakso) {
            em.findAll(VastuuhenkilonTehtavatyyppi::class)
                .firstOrNull { it.nimi == VastuuhenkilonTehtavatyyppiEnum.KOEJAKSOSOPIMUSTEN_JA_KOEJAKSOJEN_HYVAKSYMINEN }
                ?.let {
                    yliopistoErikoisala.vastuuhenkilonTehtavat.add(it)
                }
        }

        return vastuuhenkilo
    }

    companion object {

        private const val DEFAULT_ID = "c47f46ad-21c4-47e8-9c7c-ba44f60c8bae"

        private const val UPDATED_EMAIL = "doe.john@example.com"
        private const val UPDATED_PHONE = "+358101001010"

        private val DEFAULT_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(2L)
        private val DEFAULT_PAATTYMISPAIVA: LocalDate = LocalDate.ofEpochDay(5L)
        private val DEFAULT_MUOKKAUSPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val UPDATED_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(3L)
        private val UPDATED_PAATTYMISPAIVA: LocalDate = LocalDate.ofEpochDay(10L)

        private const val UPDATED_KOULUTTAJA_NIMI = "Kalle Kouluttaja"
        private const val UPDATED_VASTUUHENKILO_NIMI = "Ville Vastuuhenkilö"

        private const val DEFAULT_KOULUTUSPAIKKA = "TAYS Päivystyskeskus"
        private val DEFAULT_YLIOPISTO = YliopistoEnum.TAMPEREEN_YLIOPISTO

        private const val UPDATED_KOULUTUSPAIKKA = "HUS Päivystyskeskus"

        private const val DEFAULT_OSAAMISTAVOITTEET = "Lorem ipsum"

        private const val DEFAULT_VAHVUUDET = "Lorem ipsum"
        private const val DEFAULT_KEHITTAMISTOIMENPITEET = "Lorem ipsum"

        @JvmStatic
        fun createKoulutussopimus(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            vastuuhenkilo: Kayttaja
        ): KoejaksonKoulutussopimus {
            return KoejaksonKoulutussopimus(
                opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa(),
                koejaksonAlkamispaiva = DEFAULT_ALKAMISPAIVA,
                lahetetty = false,
                muokkauspaiva = DEFAULT_MUOKKAUSPAIVA,
                vastuuhenkilo = vastuuhenkilo,
            )
        }

        @JvmStatic
        fun createKoulutussopimuksenKouluttaja(
            koejaksonKoulutussopimus: KoejaksonKoulutussopimus,
            kouluttaja: Kayttaja
        ): KoulutussopimuksenKouluttaja {
            return KoulutussopimuksenKouluttaja(
                kouluttaja = kouluttaja,
                koulutussopimus = koejaksonKoulutussopimus
            )
        }

        @JvmStatic
        fun createKoulutussopimuksenKoulutuspaikka(
            koejaksonKoulutussopimus: KoejaksonKoulutussopimus,
            yliopisto: Yliopisto
        ): KoulutussopimuksenKoulutuspaikka {
            return KoulutussopimuksenKoulutuspaikka(
                nimi = DEFAULT_KOULUTUSPAIKKA,
                yliopisto = yliopisto,
                koulutussopimus = koejaksonKoulutussopimus
            )
        }

        @JvmStatic
        fun createAloituskeskustelu(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            lahikouluttaja: Kayttaja,
            lahiesimies: Kayttaja
        ): KoejaksonAloituskeskustelu {
            val opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()
            return KoejaksonAloituskeskustelu(
                opintooikeus = opintooikeus,
                koejaksonSuorituspaikka = DEFAULT_KOULUTUSPAIKKA,
                koejaksonAlkamispaiva = DEFAULT_ALKAMISPAIVA,
                koejaksonPaattymispaiva = DEFAULT_PAATTYMISPAIVA,
                suoritettuKokoaikatyossa = true,
                lahikouluttaja = lahikouluttaja,
                lahiesimies = lahiesimies,
                koejaksonOsaamistavoitteet = DEFAULT_OSAAMISTAVOITTEET,
                lahetetty = false,
                muokkauspaiva = DEFAULT_MUOKKAUSPAIVA
            )
        }

        @JvmStatic
        fun createValiarviointi(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            lahikouluttaja: Kayttaja,
            lahiesimies: Kayttaja
        ): KoejaksonValiarviointi {
            val opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()
            return KoejaksonValiarviointi(
                opintooikeus = opintooikeus,
                edistyminenTavoitteidenMukaista = false,
                vahvuudet = DEFAULT_VAHVUUDET,
                kehittamistoimenpiteet = DEFAULT_KEHITTAMISTOIMENPITEET,
                lahikouluttaja = lahikouluttaja,
                lahiesimies = lahiesimies,
                muokkauspaiva = DEFAULT_MUOKKAUSPAIVA
            )
        }

        @JvmStatic
        fun createKehittamistoimenpiteet(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            lahikouluttaja: Kayttaja,
            lahiesimies: Kayttaja
        ): KoejaksonKehittamistoimenpiteet {
            val opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()
            return KoejaksonKehittamistoimenpiteet(
                opintooikeus = opintooikeus,
                kehittamistoimenpiteetRiittavat = true,
                lahikouluttaja = lahikouluttaja,
                lahiesimies = lahiesimies,
                muokkauspaiva = DEFAULT_MUOKKAUSPAIVA
            )
        }

        @JvmStatic
        fun createLoppukeskustelu(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            lahikouluttaja: Kayttaja,
            lahiesimies: Kayttaja
        ): KoejaksonLoppukeskustelu {
            val opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa()
            return KoejaksonLoppukeskustelu(
                opintooikeus = opintooikeus,
                esitetaanKoejaksonHyvaksymista = true,
                lahikouluttaja = lahikouluttaja,
                lahiesimies = lahiesimies,
                muokkauspaiva = DEFAULT_MUOKKAUSPAIVA
            )
        }

        @JvmStatic
        fun createVastuuhenkilonArvio(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            vastuuhenkilo: Kayttaja
        ): KoejaksonVastuuhenkilonArvio {
            return KoejaksonVastuuhenkilonArvio(
                opintooikeus = erikoistuvaLaakari.getOpintooikeusKaytossa(),
                vastuuhenkilo = vastuuhenkilo,
                muokkauspaiva = DEFAULT_MUOKKAUSPAIVA
            )
        }
    }
}
