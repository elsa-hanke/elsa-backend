package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.service.mapper.*
import fi.elsapalvelu.elsa.web.rest.KayttajaResourceWithMockUserIT
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

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @Transactional
    fun getKoejakso() {
        initTest()

        restKoejaksoMockMvc.perform(
            get(
                "/api/erikoistuva-laakari/koejakso"
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.koulutussopimus").isEmpty)
    }

    @Test
    @Transactional
    fun createKoulutussopimusWithExistingId() {
        initTest()

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
        initTest()

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
        initTest()

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
        initTest()

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
        assertThat(sopimus.erikoistuvanNimi).isEqualTo(koejaksonKoulutussopimusDTO.erikoistuvanNimi)
        assertThat(sopimus.erikoistuvanOpiskelijatunnus).isEqualTo(koejaksonKoulutussopimusDTO.erikoistuvanOpiskelijatunnus)
        assertThat(sopimus.erikoistuvanSyntymaaika).isEqualTo(koejaksonKoulutussopimusDTO.erikoistuvanSyntymaaika)
        assertThat(sopimus.erikoistuvanYliopisto).isEqualTo(koejaksonKoulutussopimusDTO.erikoistuvanYliopisto)
        assertThat(sopimus.erikoistuvanPuhelinnumero).isEqualTo(koejaksonKoulutussopimusDTO.erikoistuvanPuhelinnumero)
        assertThat(sopimus.erikoistuvanSahkoposti).isEqualTo(koejaksonKoulutussopimusDTO.erikoistuvanSahkoposti)
        assertThat(sopimus.opintooikeudenMyontamispaiva).isEqualTo(koejaksonKoulutussopimusDTO.opintooikeudenMyontamispaiva)
        assertThat(sopimus.koejaksonAlkamispaiva).isEqualTo(koejaksonKoulutussopimusDTO.koejaksonAlkamispaiva)
        assertThat(sopimus.lahetetty).isEqualTo(koejaksonKoulutussopimusDTO.lahetetty)
        assertThat(sopimus.muokkauspaiva).isNotNull
        assertThat(sopimus.vastuuhenkilo?.id).isEqualTo(koejaksonKoulutussopimusDTO.vastuuhenkilo?.id)
        assertThat(sopimus.vastuuhenkilonNimi).isEqualTo(koejaksonKoulutussopimusDTO.vastuuhenkilo?.nimi)
        assertThat(sopimus.vastuuhenkilonNimike).isEqualTo(koejaksonKoulutussopimusDTO.vastuuhenkilo?.nimike)
        assertThat(sopimus.vastuuhenkiloHyvaksynyt).isFalse
        assertThat(sopimus.vastuuhenkilonKuittausaika).isNull()
        assertThat(sopimus.korjausehdotus).isNull()
        assertThat(sopimus.kouluttajat).hasSize(1)
        val kouluttaja = sopimus.kouluttajat?.iterator()?.next()
        val kouluttajaDTO = koejaksonKoulutussopimus.kouluttajat?.iterator()?.next()
        assertThat(kouluttaja?.nimi).isEqualTo(kouluttajaDTO?.nimi)
        assertThat(kouluttaja?.nimike).isEqualTo(kouluttajaDTO?.nimike)
        assertThat(kouluttaja?.kouluttaja?.id).isEqualTo(kouluttajaDTO?.kouluttaja?.id)
        assertThat(kouluttaja?.toimipaikka).isEqualTo(kouluttajaDTO?.toimipaikka)
        assertThat(kouluttaja?.lahiosoite).isEqualTo(kouluttajaDTO?.lahiosoite)
        assertThat(kouluttaja?.postitoimipaikka).isEqualTo(kouluttajaDTO?.postitoimipaikka)
        assertThat(kouluttaja?.puhelin).isEqualTo(kouluttajaDTO?.puhelin)
        assertThat(kouluttaja?.sahkoposti).isEqualTo(kouluttajaDTO?.sahkoposti)
        assertThat(kouluttaja?.sopimusHyvaksytty).isFalse
        assertThat(kouluttaja?.kuittausaika).isNull()
        assertThat(sopimus.koulutuspaikat).hasSize(1)
        val koulutuspaikka = sopimus.koulutuspaikat?.iterator()?.next()
        val koulutuspaikkaDTO = koejaksonKoulutussopimus.koulutuspaikat?.iterator()?.next()
        assertThat(koulutuspaikka?.nimi).isEqualTo(koulutuspaikkaDTO?.nimi)
        assertThat(koulutuspaikka?.yliopisto).isEqualTo(koulutuspaikkaDTO?.yliopisto)
    }

    @Test
    @Transactional
    fun updateKoulutussopimus() {
        initTest()

        koejaksonKoulutussopimusRepository.saveAndFlush(koejaksonKoulutussopimus)

        val databaseSizeBeforeUpdate = koejaksonKoulutussopimusRepository.findAll().size

        val id = koejaksonKoulutussopimus.id
        assertNotNull(id)
        val updatedKoulutussopimus = koejaksonKoulutussopimusRepository.findById(id).get()
        em.detach(updatedKoulutussopimus)

        updatedKoulutussopimus.koejaksonAlkamispaiva = UPDATED_ALKAMISPAIVA
        updatedKoulutussopimus.erikoistuvanSahkoposti = UPDATED_EMAIL
        updatedKoulutussopimus.erikoistuvanPuhelinnumero = UPDATED_PHONE

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

        val updatedVastuuhenkilo = KayttajaHelper.createUpdatedEntity(
            em,
            UPDATED_VASTUUHENKILO_NIMI
        )
        em.persist(updatedVastuuhenkilo)
        updatedKoulutussopimus.vastuuhenkilo = updatedVastuuhenkilo
        updatedKoulutussopimus.vastuuhenkilonNimi = updatedVastuuhenkilo.getNimi()
        updatedKoulutussopimus.vastuuhenkilonNimike = updatedVastuuhenkilo.nimike

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
        assertThat(testKoulutussopimus.erikoistuvanSahkoposti).isEqualTo(UPDATED_EMAIL)
        assertThat(testKoulutussopimus.erikoistuvanPuhelinnumero).isEqualTo(UPDATED_PHONE)

        val testKoulutuspaikka = testKoulutussopimus.koulutuspaikat?.iterator()?.next()
        assertThat(testKoulutuspaikka?.nimi).isEqualTo(UPDATED_KOULUTUSPAIKKA)

        assertThat(testKoulutussopimus.kouluttajat).hasSize(2)
        val testKouluttaja =
            testKoulutussopimus.kouluttajat?.filter { it.kouluttaja?.id == updatedKouluttaja.id }
                ?.get(0)
        assertThat(testKouluttaja?.kouluttaja?.user?.id).isEqualTo(updatedKouluttaja.user?.id)

        val testVastuuhenkilo = kayttajaRepository.findById(testKoulutussopimus.vastuuhenkilo?.id!!)
        assertThat(testVastuuhenkilo.get().user?.id).isEqualTo(updatedVastuuhenkilo.user?.id)
        assertThat(testKoulutussopimus.vastuuhenkilonNimi).isEqualTo(UPDATED_VASTUUHENKILO_NIMI)
    }

    @Test
    @Transactional
    fun createAloituskeskusteluWithExistingId() {
        initTest()

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
        initTest()

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
        initTest()

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
        initTest()

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
        assertThat(keskustelu.erikoistuvanNimi).isEqualTo(koejaksonAloituskeskusteluDTO.erikoistuvanNimi)
        assertThat(keskustelu.erikoistuvanErikoisala).isEqualTo(koejaksonAloituskeskusteluDTO.erikoistuvanErikoisala)
        assertThat(keskustelu.erikoistuvanOpiskelijatunnus).isEqualTo(koejaksonAloituskeskusteluDTO.erikoistuvanOpiskelijatunnus)
        assertThat(keskustelu.erikoistuvanYliopisto).isEqualTo(koejaksonAloituskeskusteluDTO.erikoistuvanYliopisto)
        assertThat(keskustelu.erikoistuvanSahkoposti).isEqualTo(koejaksonAloituskeskusteluDTO.erikoistuvanSahkoposti)
        assertThat(keskustelu.koejaksonSuorituspaikka).isEqualTo(koejaksonAloituskeskusteluDTO.koejaksonSuorituspaikka)
        assertThat(keskustelu.koejaksonToinenSuorituspaikka).isEqualTo(koejaksonAloituskeskusteluDTO.koejaksonToinenSuorituspaikka)
        assertThat(keskustelu.koejaksonAlkamispaiva).isEqualTo(koejaksonAloituskeskusteluDTO.koejaksonAlkamispaiva)
        assertThat(keskustelu.koejaksonPaattymispaiva).isEqualTo(koejaksonAloituskeskusteluDTO.koejaksonPaattymispaiva)
        assertThat(keskustelu.suoritettuKokoaikatyossa).isEqualTo(koejaksonAloituskeskusteluDTO.suoritettuKokoaikatyossa)
        assertThat(keskustelu.tyotunnitViikossa).isEqualTo(koejaksonAloituskeskusteluDTO.tyotunnitViikossa)
        assertThat(keskustelu.lahikouluttaja?.id).isEqualTo(koejaksonAloituskeskusteluDTO.lahikouluttaja?.id)
        assertThat(keskustelu.lahikouluttajanNimi).isEqualTo(koejaksonAloituskeskusteluDTO.lahikouluttaja?.nimi)
        assertThat(keskustelu.lahiesimies?.id).isEqualTo(koejaksonAloituskeskusteluDTO.lahiesimies?.id)
        assertThat(keskustelu.lahiesimiehenNimi).isEqualTo(koejaksonAloituskeskusteluDTO.lahiesimies?.nimi)
        assertThat(keskustelu.koejaksonOsaamistavoitteet).isEqualTo(koejaksonAloituskeskusteluDTO.koejaksonOsaamistavoitteet)
        assertThat(keskustelu.lahetetty).isEqualTo(koejaksonAloituskeskusteluDTO.lahetetty)
        assertThat(keskustelu.muokkauspaiva).isNotNull
    }

    @Test
    @Transactional
    fun updateAloituskeskustelu() {
        initTest()

        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        val databaseSizeBeforeUpdate = koejaksonAloituskeskusteluRepository.findAll().size

        val id = koejaksonAloituskeskustelu.id
        assertNotNull(id)
        val updatedAloituskeskustelu = koejaksonAloituskeskusteluRepository.findById(id).get()
        em.detach(updatedAloituskeskustelu)

        updatedAloituskeskustelu.koejaksonAlkamispaiva = UPDATED_ALKAMISPAIVA
        updatedAloituskeskustelu.koejaksonPaattymispaiva = UPDATED_PAATTYMISPAIVA
        updatedAloituskeskustelu.erikoistuvanSahkoposti = UPDATED_EMAIL

        val updatedKouluttaja = KayttajaHelper.createUpdatedEntity(
            em,
            UPDATED_VASTUUHENKILO_NIMI
        )
        em.persist(updatedKouluttaja)
        updatedAloituskeskustelu.lahikouluttaja = updatedKouluttaja
        updatedAloituskeskustelu.lahikouluttajanNimi = updatedKouluttaja.getNimi()

        val aloituskeskusteluDTO = koejaksonAloituskeskusteluMapper.toDto(updatedAloituskeskustelu)

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
        assertThat(testAloituskeskustelu.erikoistuvanSahkoposti).isEqualTo(UPDATED_EMAIL)

        val testLahikouluttaja =
            kayttajaRepository.findById(testAloituskeskustelu.lahikouluttaja?.id!!)
        assertThat(testLahikouluttaja.get().user?.id).isEqualTo(updatedKouluttaja.user?.id)
        assertThat(testAloituskeskustelu.lahikouluttajanNimi).isEqualTo(UPDATED_VASTUUHENKILO_NIMI)
    }

    @Test
    @Transactional
    fun createValiarviointi() {
        initTest()

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
        assertThat(arviointi.erikoistuvanNimi).isEqualTo(koejaksonValiarviointiDTO.erikoistuvanNimi)
        assertThat(arviointi.erikoistuvanErikoisala).isEqualTo(koejaksonValiarviointiDTO.erikoistuvanErikoisala)
        assertThat(arviointi.erikoistuvanOpiskelijatunnus).isEqualTo(koejaksonValiarviointiDTO.erikoistuvanOpiskelijatunnus)
        assertThat(arviointi.erikoistuvanYliopisto).isEqualTo(koejaksonValiarviointiDTO.erikoistuvanYliopisto)
        assertThat(arviointi.lahikouluttaja?.id).isEqualTo(koejaksonValiarviointiDTO.lahikouluttaja?.id)
        assertThat(arviointi.lahikouluttajanNimi).isEqualTo(koejaksonValiarviointiDTO.lahikouluttaja?.nimi)
        assertThat(arviointi.lahiesimies?.id).isEqualTo(koejaksonValiarviointiDTO.lahiesimies?.id)
        assertThat(arviointi.lahiesimiehenNimi).isEqualTo(koejaksonValiarviointiDTO.lahiesimies?.nimi)
        assertThat(arviointi.muokkauspaiva).isNotNull
    }

    @Test
    @Transactional
    fun ackValiarviointi() {
        initTest()

        koejaksonValiarviointi.lahiesimiesHyvaksynyt = true
        koejaksonValiarviointi.lahiesimiehenKuittausaika = DEFAULT_MYONTAMISPAIVA
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        val databaseSizeBeforeUpdate = koejaksonValiarviointiRepository.findAll().size

        val id = koejaksonValiarviointi.id
        assertNotNull(id)
        val updatedValiarviointi = koejaksonValiarviointiRepository.findById(id).get()
        em.detach(updatedValiarviointi)

        updatedValiarviointi.erikoistuvaAllekirjoittanut = true

        val valiarviointiDTO = koejaksonValiarviointiMapper.toDto(updatedValiarviointi)

        restKoejaksoMockMvc.perform(
            put("/api/erikoistuva-laakari/koejakso/valiarviointi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(valiarviointiDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val valiarviointiList = koejaksonValiarviointiRepository.findAll()
        assertThat(valiarviointiList).hasSize(databaseSizeBeforeUpdate)
        val testValiarviointi = valiarviointiList[valiarviointiList.size - 1]
        assertThat(testValiarviointi.erikoistuvaAllekirjoittanut).isEqualTo(true)
        assertThat(testValiarviointi.muokkauspaiva).isEqualTo(DEFAULT_MUOKKAUSPAIVA)
    }

    @Test
    @Transactional
    fun createValiarviointiWithoutAloituskeskustelu() {
        initTest()

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
        initTest()

        koejaksonValiarviointi.erikoistuvaAllekirjoittanut = true
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
        assertThat(arviointi.erikoistuvanNimi).isEqualTo(koejaksonKehittamistoimenpiteetDTO.erikoistuvanNimi)
        assertThat(arviointi.erikoistuvanErikoisala).isEqualTo(koejaksonKehittamistoimenpiteetDTO.erikoistuvanErikoisala)
        assertThat(arviointi.erikoistuvanOpiskelijatunnus).isEqualTo(
            koejaksonKehittamistoimenpiteetDTO.erikoistuvanOpiskelijatunnus
        )
        assertThat(arviointi.erikoistuvanYliopisto).isEqualTo(koejaksonKehittamistoimenpiteetDTO.erikoistuvanYliopisto)
        assertThat(arviointi.lahikouluttaja?.id).isEqualTo(koejaksonKehittamistoimenpiteetDTO.lahikouluttaja?.id)
        assertThat(arviointi.lahikouluttajanNimi).isEqualTo(koejaksonKehittamistoimenpiteetDTO.lahikouluttaja?.nimi)
        assertThat(arviointi.lahiesimies?.id).isEqualTo(koejaksonKehittamistoimenpiteetDTO.lahiesimies?.id)
        assertThat(arviointi.lahiesimiehenNimi).isEqualTo(koejaksonKehittamistoimenpiteetDTO.lahiesimies?.nimi)
        assertThat(arviointi.muokkauspaiva).isNotNull
    }

    @Test
    @Transactional
    fun ackKehittamistoimenpiteet() {
        initTest()

        koejaksonKehittamistoimenpiteet.lahiesimiesHyvaksynyt = true
        koejaksonKehittamistoimenpiteet.lahiesimiehenKuittausaika = DEFAULT_MYONTAMISPAIVA
        koejaksonKehittamistoimenpiteetRepository.saveAndFlush(koejaksonKehittamistoimenpiteet)

        val databaseSizeBeforeUpdate = koejaksonKehittamistoimenpiteetRepository.findAll().size

        val id = koejaksonKehittamistoimenpiteet.id
        assertNotNull(id)
        val updatedKehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetRepository.findById(id).get()
        em.detach(updatedKehittamistoimenpiteet)

        updatedKehittamistoimenpiteet.erikoistuvaAllekirjoittanut = true

        val kehittamistoimenpiteetDTO =
            koejaksonKehittamistoimenpiteetMapper.toDto(updatedKehittamistoimenpiteet)

        restKoejaksoMockMvc.perform(
            put("/api/erikoistuva-laakari/koejakso/kehittamistoimenpiteet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kehittamistoimenpiteetDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val kehittamistoimenpiteetList = koejaksonKehittamistoimenpiteetRepository.findAll()
        assertThat(kehittamistoimenpiteetList).hasSize(databaseSizeBeforeUpdate)
        val testKehittamistoimenpiteet =
            kehittamistoimenpiteetList[kehittamistoimenpiteetList.size - 1]
        assertThat(testKehittamistoimenpiteet.erikoistuvaAllekirjoittanut).isEqualTo(true)
        assertThat(testKehittamistoimenpiteet.muokkauspaiva).isEqualTo(DEFAULT_MUOKKAUSPAIVA)
    }

    @Test
    @Transactional
    fun createKehittamistoimenpiteetWithoutValiarviointi() {
        initTest()

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
        initTest()

        koejaksonValiarviointi.erikoistuvaAllekirjoittanut = true
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        koejaksonKehittamistoimenpiteet.erikoistuvaAllekirjoittanut = true
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
        assertThat(arviointi.erikoistuvanNimi).isEqualTo(koejaksonLoppukeskusteluDTO.erikoistuvanNimi)
        assertThat(arviointi.erikoistuvanErikoisala).isEqualTo(koejaksonLoppukeskusteluDTO.erikoistuvanErikoisala)
        assertThat(arviointi.erikoistuvanOpiskelijatunnus).isEqualTo(
            koejaksonLoppukeskusteluDTO.erikoistuvanOpiskelijatunnus
        )
        assertThat(arviointi.erikoistuvanYliopisto).isEqualTo(koejaksonLoppukeskusteluDTO.erikoistuvanYliopisto)
        assertThat(arviointi.lahikouluttaja?.id).isEqualTo(koejaksonLoppukeskusteluDTO.lahikouluttaja?.id)
        assertThat(arviointi.lahikouluttajanNimi).isEqualTo(koejaksonLoppukeskusteluDTO.lahikouluttaja?.nimi)
        assertThat(arviointi.lahiesimies?.id).isEqualTo(koejaksonLoppukeskusteluDTO.lahiesimies?.id)
        assertThat(arviointi.lahiesimiehenNimi).isEqualTo(koejaksonLoppukeskusteluDTO.lahiesimies?.nimi)
        assertThat(arviointi.muokkauspaiva).isNotNull
    }

    @Test
    @Transactional
    fun createLoppukeskusteluWithKehittamistoimenpiteetInvalid() {
        initTest()

        koejaksonValiarviointi.erikoistuvaAllekirjoittanut = true
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
        initTest()

        koejaksonValiarviointi.erikoistuvaAllekirjoittanut = true
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
        assertThat(arviointi.erikoistuvanNimi).isEqualTo(koejaksonLoppukeskusteluDTO.erikoistuvanNimi)
        assertThat(arviointi.erikoistuvanErikoisala).isEqualTo(koejaksonLoppukeskusteluDTO.erikoistuvanErikoisala)
        assertThat(arviointi.erikoistuvanOpiskelijatunnus).isEqualTo(
            koejaksonLoppukeskusteluDTO.erikoistuvanOpiskelijatunnus
        )
        assertThat(arviointi.erikoistuvanYliopisto).isEqualTo(koejaksonLoppukeskusteluDTO.erikoistuvanYliopisto)
        assertThat(arviointi.lahikouluttaja?.id).isEqualTo(koejaksonLoppukeskusteluDTO.lahikouluttaja?.id)
        assertThat(arviointi.lahikouluttajanNimi).isEqualTo(koejaksonLoppukeskusteluDTO.lahikouluttaja?.nimi)
        assertThat(arviointi.lahiesimies?.id).isEqualTo(koejaksonLoppukeskusteluDTO.lahiesimies?.id)
        assertThat(arviointi.lahiesimiehenNimi).isEqualTo(koejaksonLoppukeskusteluDTO.lahiesimies?.nimi)
        assertThat(arviointi.muokkauspaiva).isNotNull
    }

    @Test
    @Transactional
    fun createLoppukeskusteluWithoutKehittamistoimenpiteetInvalid() {
        initTest()

        koejaksonValiarviointi.erikoistuvaAllekirjoittanut = true
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
    fun ackLoppukeskustelu() {
        initTest()

        koejaksonLoppukeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonLoppukeskustelu.lahiesimiehenKuittausaika = DEFAULT_MYONTAMISPAIVA
        koejaksonLoppukeskusteluRepository.saveAndFlush(koejaksonLoppukeskustelu)

        val databaseSizeBeforeUpdate = koejaksonLoppukeskusteluRepository.findAll().size

        val id = koejaksonLoppukeskustelu.id
        assertNotNull(id)
        val updatedLoppukeskustelu = koejaksonLoppukeskusteluRepository.findById(id).get()
        em.detach(updatedLoppukeskustelu)

        updatedLoppukeskustelu.erikoistuvaAllekirjoittanut = true

        val loppukeskusteluDTO = koejaksonLoppukeskusteluMapper.toDto(updatedLoppukeskustelu)

        restKoejaksoMockMvc.perform(
            put("/api/erikoistuva-laakari/koejakso/loppukeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(loppukeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val loppukeskusteluList = koejaksonLoppukeskusteluRepository.findAll()
        assertThat(loppukeskusteluList).hasSize(databaseSizeBeforeUpdate)
        val testLoppukeskustelu = loppukeskusteluList[loppukeskusteluList.size - 1]
        assertThat(testLoppukeskustelu.erikoistuvaAllekirjoittanut).isEqualTo(true)
        assertThat(testLoppukeskustelu.muokkauspaiva).isEqualTo(DEFAULT_MUOKKAUSPAIVA)
    }

    @Test
    @Transactional
    fun createVastuuhenkilonArvio() {
        initTest()

        koejaksonLoppukeskustelu.erikoistuvaAllekirjoittanut = true
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
        assertThat(arviointi.erikoistuvanNimi).isEqualTo(koejaksonVastuuhenkilonArvioDTO.erikoistuvanNimi)
        assertThat(arviointi.erikoistuvanErikoisala).isEqualTo(koejaksonVastuuhenkilonArvioDTO.erikoistuvanErikoisala)
        assertThat(arviointi.erikoistuvanOpiskelijatunnus).isEqualTo(
            koejaksonVastuuhenkilonArvioDTO.erikoistuvanOpiskelijatunnus
        )
        assertThat(arviointi.erikoistuvanYliopisto).isEqualTo(koejaksonVastuuhenkilonArvioDTO.erikoistuvanYliopisto)
        assertThat(arviointi.vastuuhenkilo?.id).isEqualTo(koejaksonVastuuhenkilonArvioDTO.vastuuhenkilo?.id)
        assertThat(arviointi.vastuuhenkilonNimi).isEqualTo(koejaksonVastuuhenkilonArvioDTO.vastuuhenkilo?.nimi)
        assertThat(arviointi.muokkauspaiva).isNotNull
    }

    @Test
    @Transactional
    fun ackVastuuhenkilonArvio() {
        initTest()

        koejaksonVastuuhenkilonArvio.koejaksoHyvaksytty = true
        koejaksonVastuuhenkilonArvio.vastuuhenkiloAllekirjoittanut = true
        koejaksonVastuuhenkilonArvio.vastuuhenkilonKuittausaika = DEFAULT_MYONTAMISPAIVA
        koejaksonVastuuhenkilonArvioRepository.saveAndFlush(koejaksonVastuuhenkilonArvio)

        val databaseSizeBeforeUpdate = koejaksonVastuuhenkilonArvioRepository.findAll().size

        val id = koejaksonVastuuhenkilonArvio.id
        assertNotNull(id)
        val updatedVastuuhenkilonArvio = koejaksonVastuuhenkilonArvioRepository.findById(id).get()
        em.detach(updatedVastuuhenkilonArvio)

        updatedVastuuhenkilonArvio.erikoistuvaAllekirjoittanut = true

        val vastuuhenkilonArvioDTO =
            koejaksonVastuuhenkilonArvioMapper.toDto(updatedVastuuhenkilonArvio)

        restKoejaksoMockMvc.perform(
            put("/api/erikoistuva-laakari/koejakso/vastuuhenkilonarvio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(vastuuhenkilonArvioDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val vastuuhenkilonArvioList = koejaksonVastuuhenkilonArvioRepository.findAll()
        assertThat(vastuuhenkilonArvioList).hasSize(databaseSizeBeforeUpdate)
        val testVastuuhenkilonArvio = vastuuhenkilonArvioList[vastuuhenkilonArvioList.size - 1]
        assertThat(testVastuuhenkilonArvio.erikoistuvaAllekirjoittanut).isEqualTo(true)
        assertThat(testVastuuhenkilonArvio.muokkauspaiva).isEqualTo(DEFAULT_MUOKKAUSPAIVA)
    }

    @Test
    @Transactional
    fun createVastuuhenkilonArvioWithoutLoppukeskustelu() {
        initTest()

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

    fun initTest(userId: String? = DEFAULT_ID) {
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
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, user)
        em.persist(erikoistuvaLaakari)

        val vastuuhenkilo = KayttajaHelper.createEntity(em)
        em.persist(vastuuhenkilo)

        val kouluttaja = KayttajaHelper.createEntity(em)
        em.persist(kouluttaja)

        val esimies = KayttajaHelper.createEntity(em)
        em.persist(esimies)

        koejaksonKoulutussopimus = createKoulutussopimus(erikoistuvaLaakari, vastuuhenkilo)
        koejaksonKoulutussopimus.kouluttajat =
            mutableSetOf(createKoulutussopimuksenKouluttaja(koejaksonKoulutussopimus, kouluttaja))
        koejaksonKoulutussopimus.koulutuspaikat =
            mutableSetOf(createKoulutussopimuksenKoulutuspaikka(koejaksonKoulutussopimus))

        koejaksonAloituskeskustelu =
            createAloituskeskustelu(erikoistuvaLaakari, kouluttaja, esimies)

        koejaksonValiarviointi =
            createValiarviointi(erikoistuvaLaakari, kouluttaja, esimies)

        koejaksonKehittamistoimenpiteet =
            createKehittamistoimenpiteet(erikoistuvaLaakari, kouluttaja, esimies)

        koejaksonLoppukeskustelu = createLoppukeskustelu(erikoistuvaLaakari, kouluttaja, esimies)

        koejaksonVastuuhenkilonArvio = createVastuuhenkilonArvio(erikoistuvaLaakari, vastuuhenkilo)
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

        private val UPDATED_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(3L)
        private val UPDATED_PAATTYMISPAIVA: LocalDate = LocalDate.ofEpochDay(10L)

        private const val DEFAULT_KOULUTTAJA_ID = "4b73bc2c-88c4-11eb-8dcd-0242ac130003"
        private const val DEFAULT_ESIMIES_ID = "43c0ebfa-92f9-11eb-a8b3-0242ac130003"
        private const val DEFAULT_VASTUUHENKILO_ID = "53d6e70e-88c4-11eb-8dcd-0242ac130003"

        private const val UPDATED_KOULUTTAJA_ID = "914cb8c5-c56d-4ab4-81a8-e51b5db0a85b"
        private const val UPDATED_KOULUTTAJA_NIMI = "Kalle Kouluttaja"
        private const val UPDATED_VASTUUHENKILO_ID = "1df48f72-8bbe-11eb-8dcd-0242ac130003"
        private const val UPDATED_VASTUUHENKILO_NIMI = "Ville Vastuuhenkilö"

        private const val DEFAULT_KOULUTUSPAIKKA = "TAYS Päivystyskeskus"
        private const val DEFAULT_YLIOPISTO = "TAYS"

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
                erikoistuvaLaakari = erikoistuvaLaakari,
                erikoistuvanNimi = erikoistuvaLaakari.kayttaja?.getNimi(),
                erikoistuvanOpiskelijatunnus = erikoistuvaLaakari.opiskeluoikeudet.firstOrNull()?.opiskelijatunnus,
                erikoistuvanSyntymaaika = DEFAULT_SYNTYMAAIKA,
                erikoistuvanYliopisto = erikoistuvaLaakari.opiskeluoikeudet.firstOrNull()?.yliopisto?.nimi,
                opintooikeudenMyontamispaiva = DEFAULT_MYONTAMISPAIVA,
                koejaksonAlkamispaiva = DEFAULT_ALKAMISPAIVA,
                erikoistuvanPuhelinnumero = erikoistuvaLaakari.kayttaja?.user?.phoneNumber,
                erikoistuvanSahkoposti = erikoistuvaLaakari.kayttaja?.user?.email,
                lahetetty = false,
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
                erikoistuvanErikoisala = erikoistuvaLaakari.opiskeluoikeudet.firstOrNull()?.erikoisala?.nimi,
                erikoistuvanOpiskelijatunnus = erikoistuvaLaakari.opiskeluoikeudet.firstOrNull()?.opiskelijatunnus,
                erikoistuvanYliopisto = erikoistuvaLaakari.opiskeluoikeudet.firstOrNull()?.yliopisto?.nimi,
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
            return KoejaksonValiarviointi(
                erikoistuvaLaakari = erikoistuvaLaakari,
                erikoistuvanNimi = erikoistuvaLaakari.kayttaja?.getNimi(),
                erikoistuvanErikoisala = erikoistuvaLaakari.opiskeluoikeudet.firstOrNull()?.erikoisala?.nimi,
                erikoistuvanOpiskelijatunnus = erikoistuvaLaakari.opiskeluoikeudet.firstOrNull()?.opiskelijatunnus,
                erikoistuvanYliopisto = erikoistuvaLaakari.opiskeluoikeudet.firstOrNull()?.yliopisto?.nimi,
                edistyminenTavoitteidenMukaista = false,
                vahvuudet = DEFAULT_VAHVUUDET,
                kehittamistoimenpiteet = DEFAULT_KEHITTAMISTOIMENPITEET,
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
                erikoistuvanErikoisala = erikoistuvaLaakari.opiskeluoikeudet.firstOrNull()?.erikoisala?.nimi,
                erikoistuvanOpiskelijatunnus = erikoistuvaLaakari.opiskeluoikeudet.firstOrNull()?.opiskelijatunnus,
                erikoistuvanYliopisto = erikoistuvaLaakari.opiskeluoikeudet.firstOrNull()?.yliopisto?.nimi,
                kehittamistoimenpiteetRiittavat = true,
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
                erikoistuvanErikoisala = erikoistuvaLaakari.opiskeluoikeudet.firstOrNull()?.erikoisala?.nimi,
                erikoistuvanOpiskelijatunnus = erikoistuvaLaakari.opiskeluoikeudet.firstOrNull()?.opiskelijatunnus,
                erikoistuvanYliopisto = erikoistuvaLaakari.opiskeluoikeudet.firstOrNull()?.yliopisto?.nimi,
                esitetaanKoejaksonHyvaksymista = true,
                lahikouluttaja = lahikouluttaja,
                lahikouluttajanNimi = lahikouluttaja.getNimi(),
                lahiesimies = lahiesimies,
                lahiesimiehenNimi = lahiesimies.getNimi(),
                muokkauspaiva = DEFAULT_MUOKKAUSPAIVA
            )
        }

        @JvmStatic
        fun createVastuuhenkilonArvio(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            vastuuhenkilo: Kayttaja
        ): KoejaksonVastuuhenkilonArvio {
            return KoejaksonVastuuhenkilonArvio(
                erikoistuvaLaakari = erikoistuvaLaakari,
                erikoistuvanNimi = erikoistuvaLaakari.kayttaja?.getNimi(),
                erikoistuvanErikoisala = erikoistuvaLaakari.opiskeluoikeudet.firstOrNull()?.erikoisala?.nimi,
                erikoistuvanOpiskelijatunnus = erikoistuvaLaakari.opiskeluoikeudet.firstOrNull()?.opiskelijatunnus,
                erikoistuvanYliopisto = erikoistuvaLaakari.opiskeluoikeudet.firstOrNull()?.yliopisto?.nimi,
                vastuuhenkilo = vastuuhenkilo,
                vastuuhenkilonNimi = vastuuhenkilo.getNimi(),
                vastuuhenkilonNimike = vastuuhenkilo.nimike,
                muokkauspaiva = DEFAULT_MUOKKAUSPAIVA
            )
        }
    }
}
