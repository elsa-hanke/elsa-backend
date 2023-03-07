package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.KayttajatilinTila
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.ErikoisalaRepository
import fi.elsapalvelu.elsa.repository.KayttajaRepository
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.security.OPINTOHALLINNON_VIRKAILIJA
import fi.elsapalvelu.elsa.security.TEKNINEN_PAAKAYTTAJA
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.dto.ErikoisalaDTO
import fi.elsapalvelu.elsa.service.dto.KayttajaYliopistoErikoisalaDTO
import fi.elsapalvelu.elsa.service.dto.ReassignedVastuuhenkilonTehtavaDTO
import fi.elsapalvelu.elsa.service.dto.YliopistoDTO
import fi.elsapalvelu.elsa.service.dto.enumeration.ReassignedVastuuhenkilonTehtavaTyyppi
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaErikoistuvaLaakariUpdateDTO
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.KayttajahallintaKayttajaDTO
import fi.elsapalvelu.elsa.service.mapper.ErikoisalaMapper
import fi.elsapalvelu.elsa.service.mapper.KayttajaYliopistoErikoisalaMapper
import fi.elsapalvelu.elsa.service.mapper.VastuuhenkilonTehtavatyyppiMapper
import fi.elsapalvelu.elsa.service.mapper.YliopistoMapper
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.findAll
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoisalaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper.Companion.DEFAULT_YLIOPISTO
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.LocalDate
import java.util.*
import java.util.concurrent.atomic.AtomicLong
import jakarta.persistence.EntityManager

private const val tekninenPaakayttajaRolePath = "tekninen-paakayttaja"
private const val virkailijaRolePath = "virkailija"

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class KayttajahallintaResourceIT {

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var erikoisalaRepository: ErikoisalaRepository

    @Autowired
    private lateinit var kayttajaRepository: KayttajaRepository

    @Autowired
    private lateinit var yliopistoMapper: YliopistoMapper

    @Autowired
    private lateinit var erikoisalaMapper: ErikoisalaMapper

    @Autowired
    private lateinit var vastuuhenkilonTehtavatyyppiMapper: VastuuhenkilonTehtavatyyppiMapper

    @Autowired
    private lateinit var kayttajaYliopistoErikoisalaMapper: KayttajaYliopistoErikoisalaMapper

    @Autowired
    private lateinit var restMockMvc: MockMvc

    private lateinit var yliopisto: Yliopisto

    @BeforeEach
    fun setup() {
        yliopisto = Yliopisto(nimi = DEFAULT_YLIOPISTO)
        em.persist(yliopisto)
    }

    @Test
    @Transactional
    fun getErikoistuvaLaakariForVirkailijaSameYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, true)

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto)
        em.persist(erikoistuvaLaakari)
        em.flush()

        val id = erikoistuvaLaakari.kayttaja?.id

        restMockMvc.perform(get("/api/$virkailijaRolePath/erikoistuvat-laakarit/$id"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.kayttaja.id").value(id))
            .andExpect(jsonPath("$.erikoistuvaLaakari.id").exists())
    }

    @Test
    @Transactional
    fun getErikoistuvaLaakariForVirkailijaDifferentYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, false)

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto)
        em.persist(erikoistuvaLaakari)
        em.flush()

        val id = erikoistuvaLaakari.kayttaja?.id

        restMockMvc.perform(get("/api/$virkailijaRolePath/erikoistuvat-laakarit/$id"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun getErikoistuvaLaakariForPaakayttaja() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto)
        em.persist(erikoistuvaLaakari)
        em.flush()

        val id = erikoistuvaLaakari.kayttaja?.id

        restMockMvc.perform(get("/api/$tekninenPaakayttajaRolePath/erikoistuvat-laakarit/$id"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.kayttaja.id").value(id))
            .andExpect(jsonPath("$.erikoistuvaLaakari.id").exists())
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun getNonExistingErikoistuvaLaakari(rolePath: String) {
        initTest(getRole(rolePath))

        val id = count.incrementAndGet()

        restMockMvc.perform(get("/api/$rolePath/erikoistuvat-laakarit/$id"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun getVastuuhenkiloForVirkailijaSameYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkilo = createVastuuhenkilo(yliopisto, erikoisala)
        em.persist(vastuuhenkilo)
        em.flush()

        val id = vastuuhenkilo.id

        restMockMvc.perform(get("/api/$virkailijaRolePath/kayttajat/$id"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.kayttaja.id").value(id))
    }

    @Test
    @Transactional
    fun getVastuuhenkiloForVirkailijaDifferentYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, false)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkilo = createVastuuhenkilo(yliopisto, erikoisala)
        em.persist(vastuuhenkilo)
        em.flush()

        val id = vastuuhenkilo.id

        restMockMvc.perform(get("/api/$virkailijaRolePath/kayttajat/$id"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun getVastuuhenkiloForPaakayttaja() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkilo = createVastuuhenkilo(yliopisto, erikoisala)
        em.persist(vastuuhenkilo)
        em.flush()

        val id = vastuuhenkilo.id

        restMockMvc.perform(get("/api/$tekninenPaakayttajaRolePath/kayttajat/$id"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.kayttaja.id").value(id))
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun getNonExistingKayttaja(rolePath: String) {
        initTest(getRole(rolePath))

        val id = count.incrementAndGet()

        restMockMvc.perform(get("/api/$rolePath/kayttajat/$id"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun getKouluttajaForVirkailijaSameYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val kouluttaja = createKouluttaja(yliopisto, erikoisala)
        em.persist(kouluttaja)
        em.flush()

        val id = kouluttaja.id

        restMockMvc.perform(get("/api/$virkailijaRolePath/kayttajat/$id"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.kayttaja.id").value(id))
    }

    @Test
    @Transactional
    fun getKouluttajaForVirkailijaDifferentYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, false)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val kouluttaja = createKouluttaja(yliopisto, erikoisala)
        em.persist(kouluttaja)
        em.flush()

        val id = kouluttaja.id

        restMockMvc.perform(get("/api/$virkailijaRolePath/kayttajat/$id"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun getKouluttajaForPaakayttaja() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val kouluttaja = createKouluttaja(yliopisto, erikoisala)
        em.persist(kouluttaja)
        em.flush()

        val id = kouluttaja.id

        restMockMvc.perform(get("/api/$tekninenPaakayttajaRolePath/kayttajat/$id"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.kayttaja.id").value(id))
    }

    @Test
    @Transactional
    fun getVirkailijaForVirkailijaSameYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, true)

        val virkailija = createVirkailija(yliopisto)
        em.persist(virkailija)
        em.flush()

        val id = virkailija.id

        restMockMvc.perform(get("/api/$virkailijaRolePath/kayttajat/$id"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.kayttaja.id").value(id))
    }

    @Test
    @Transactional
    fun getVirkailijaForVirkailijaDifferentYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, false)

        val virkailija = createVirkailija(yliopisto)
        em.persist(virkailija)
        em.flush()

        val id = virkailija.id

        restMockMvc.perform(get("/api/$virkailijaRolePath/kayttajat/$id"))
            .andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun getVirkailijaForPaakayttaja() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)

        val virkailija = createVirkailija(yliopisto)
        em.persist(virkailija)
        em.flush()

        val id = virkailija.id

        restMockMvc.perform(get("/api/$tekninenPaakayttajaRolePath/kayttajat/$id"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.kayttaja.id").value(id))
    }

    @Test
    @Transactional
    fun getPaakayttajaForPaakayttaja() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)

        val paakayttaja = createPaakayttaja()
        em.persist(paakayttaja)
        em.flush()

        val id = paakayttaja.id

        restMockMvc.perform(get("/api/$tekninenPaakayttajaRolePath/kayttajat/$id"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.kayttaja.id").value(id))
    }

    @Test
    @Transactional
    fun tryToGetPaakayttajaForVirkailija() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, false)

        val paakayttaja = createPaakayttaja()
        em.persist(paakayttaja)
        em.flush()

        val id = paakayttaja.id

        restMockMvc.perform(get("/api/$tekninenPaakayttajaRolePath/kayttajat/$id"))
            .andExpect(status().isForbidden)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun getKayttajaForm(rolePath: String) {
        initTest(getRole(rolePath))

        val erikoisalatCountByLiittynytElsaan = erikoisalaRepository.findAllByLiittynytElsaanTrue().count()

        restMockMvc.perform(get("/api/$rolePath/erikoistuva-laakari-lomake"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.yliopistot").value(Matchers.hasSize<Any>(1)))
            .andExpect(jsonPath("$.erikoisalat").value(Matchers.hasSize<Any>(erikoisalatCountByLiittynytElsaan)))
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun getErikoistuvatLaakarit(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto)
        em.persist(erikoistuvaLaakari)
        em.flush()

        val yliopisto2 = Yliopisto(nimi = YliopistoEnum.HELSINGIN_YLIOPISTO)
        em.persist(yliopisto2)

        val erikoistuvaLaakari2 = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto2)
        em.persist(erikoistuvaLaakari2)
        em.flush()

        // Virkailijalle listataan vain saman yliopiston erikoistujat.
        val expectedSize = if (rolePath == tekninenPaakayttajaRolePath) 2 else 1

        restMockMvc.perform(get("/api/$rolePath/erikoistuvat-laakarit"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(Matchers.hasSize<Any>(expectedSize)))
            .andExpect(jsonPath("$.content[0].kayttajaId").value(erikoistuvaLaakari.kayttaja?.id))
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun getKouluttajat(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkilo = createKouluttaja(yliopisto, erikoisala)

        val yliopisto2 = Yliopisto(nimi = YliopistoEnum.ITA_SUOMEN_YLIOPISTO)
        em.persist(yliopisto2)
        createKouluttaja(yliopisto2, erikoisala)

        // Virkailijalle listataan vain saman yliopiston kouluttajat.
        val expectedSize = if (rolePath == tekninenPaakayttajaRolePath) 2 else 1

        restMockMvc.perform(get("/api/$rolePath/kouluttajat"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(Matchers.hasSize<Any>(expectedSize)))
            .andExpect(jsonPath("$.content[0].kayttajaId").value(vastuuhenkilo.id))
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun getVastuuhenkilot(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkilo = createVastuuhenkilo(yliopisto, erikoisala)

        val yliopisto2 = Yliopisto(nimi = YliopistoEnum.ITA_SUOMEN_YLIOPISTO)
        em.persist(yliopisto2)
        createVastuuhenkilo(yliopisto2, erikoisala)

        // Virkailijalle listataan vain saman yliopiston vastuuhenkilöt.
        val expectedSize = if (rolePath == tekninenPaakayttajaRolePath) 2 else 1

        restMockMvc.perform(get("/api/$rolePath/vastuuhenkilot"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(Matchers.hasSize<Any>(expectedSize)))
            .andExpect(jsonPath("$.content[0].kayttajaId").value(vastuuhenkilo.id))
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun getVirkailijat(rolePath: String) {
        initTest(getRole(rolePath), true)

        createVirkailija(yliopisto)

        val yliopisto2 = Yliopisto(nimi = YliopistoEnum.ITA_SUOMEN_YLIOPISTO)
        em.persist(yliopisto2)
        createVirkailija(yliopisto2)
        createVirkailija(yliopisto2)

        // Virkailijalle listataan vain saman yliopiston virkailijat.
        val expectedSize = if (rolePath == tekninenPaakayttajaRolePath) 3 else 2

        restMockMvc.perform(get("/api/$rolePath/virkailijat"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(Matchers.hasSize<Any>(expectedSize)))
    }

    @Test
    @Transactional
    fun getPaakayttajatForPaakayttaja() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)

        createPaakayttaja()

        restMockMvc.perform(get("/api/$tekninenPaakayttajaRolePath/paakayttajat"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(Matchers.hasSize<Any>(2)))
    }

    @Test
    @Transactional
    fun getPaakayttajatForVirkailijaForbidden() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, true)

        createPaakayttaja()

        restMockMvc.perform(get("/api/$virkailijaRolePath/paakayttajat"))
            .andExpect(status().isForbidden)
    }

    @Test
    @Transactional
    fun postErikoistuvaLaakariByVirkailijaFromSameYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, true)

        val erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)

        val asetus = em.findAll(Asetus::class).get(0)

        val opintoopas = em.findAll(Opintoopas::class).get(0)

        val kayttajahallintaErikoistuvaLaakariDTO = getDefaultErikoistuvaLaakariDTO()
        kayttajahallintaErikoistuvaLaakariDTO.yliopistoId = yliopisto.id
        kayttajahallintaErikoistuvaLaakariDTO.erikoisalaId = erikoisala.id
        kayttajahallintaErikoistuvaLaakariDTO.asetusId = asetus.id
        kayttajahallintaErikoistuvaLaakariDTO.opintoopasId = opintoopas.id

        restMockMvc.perform(
            post("/api/$virkailijaRolePath/erikoistuvat-laakarit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaErikoistuvaLaakariDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isCreated)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun postErikoistuvaLaakariWithExistingEmail(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto)
        em.persist(erikoistuvaLaakari)
        em.flush()

        val erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)

        val kayttajahallintaErikoistuvaLaakariDTO = getDefaultErikoistuvaLaakariDTO()
        kayttajahallintaErikoistuvaLaakariDTO.yliopistoId = yliopisto.id
        kayttajahallintaErikoistuvaLaakariDTO.erikoisalaId = erikoisala.id
        kayttajahallintaErikoistuvaLaakariDTO.sahkopostiosoite = erikoistuvaLaakari.kayttaja?.user?.email

        restMockMvc.perform(
            post("/api/$rolePath/erikoistuvat-laakarit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaErikoistuvaLaakariDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun postErikoistuvaLaakariWithoutValidYliopisto(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = ErikoisalaHelper.createEntity()
        em.persist(erikoisala)

        val kayttajahallintaErikoistuvaLaakariDTO = getDefaultErikoistuvaLaakariDTO()
        kayttajahallintaErikoistuvaLaakariDTO.yliopistoId = count.incrementAndGet()
        kayttajahallintaErikoistuvaLaakariDTO.erikoisalaId = erikoisala.id

        restMockMvc.perform(
            post("/api/$rolePath/erikoistuvat-laakarit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaErikoistuvaLaakariDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun postErikoistuvaLaakariWithoutValidErikoisala(rolePath: String) {
        initTest(getRole(rolePath), true)

        val kayttajahallintaErikoistuvaLaakariDTO = getDefaultErikoistuvaLaakariDTO()
        kayttajahallintaErikoistuvaLaakariDTO.yliopistoId = yliopisto.id
        kayttajahallintaErikoistuvaLaakariDTO.erikoisalaId = count.incrementAndGet()

        restMockMvc.perform(
            post("/api/$rolePath/erikoistuvat-laakarit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaErikoistuvaLaakariDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun patchErikoistuvaLaakariNotAllowedToModifyDifferentYliopistoAssignedToVirkailija() {
        initTest(getRole(OPINTOHALLINNON_VIRKAILIJA), false)

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        em.persist(erikoistuvaLaakari)

        val kayttajahallintaErikoistuvaLaakariDTO =
            KayttajahallintaErikoistuvaLaakariUpdateDTO(sahkoposti = KayttajaHelper.DEFAULT_EMAIL + "x")
        val userId = erikoistuvaLaakari.kayttaja?.user?.id
        restMockMvc.perform(
            patch("/api/$virkailijaRolePath/erikoistuvat-laakarit/$userId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaErikoistuvaLaakariDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun patchErikoistuvaLaakari(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        em.persist(erikoistuvaLaakari)

        val kayttajahallintaErikoistuvaLaakariDTO =
            KayttajahallintaErikoistuvaLaakariUpdateDTO(sahkoposti = KayttajaHelper.DEFAULT_EMAIL + "x")
        val userId = erikoistuvaLaakari.kayttaja?.user?.id
        restMockMvc.perform(
            patch("/api/$rolePath/erikoistuvat-laakarit/$userId")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaErikoistuvaLaakariDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isOk)

        val updatedErikoistuvaLaakari = kayttajaRepository.findOneByUserId(userId!!).get()
        assertThat(updatedErikoistuvaLaakari.user?.email).isEqualTo(kayttajahallintaErikoistuvaLaakariDTO.sahkoposti)
    }

    @Test
    @Transactional
    fun resendErikoistuvaLaakariInvitationByVirkailijaInSameYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, true)

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto)
        em.persist(erikoistuvaLaakari)
        em.flush()

        val id = erikoistuvaLaakari.id

        val verificationToken = VerificationToken(
            user = User(id = erikoistuvaLaakari.kayttaja?.user?.id)
        )
        em.persist(verificationToken)

        restMockMvc.perform(
            put("/api/$virkailijaRolePath/erikoistuvat-laakarit/$id/kutsu")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isNoContent)
    }

    @Test
    @Transactional
    fun resendErikoistuvaLaakariInvitationByVirkailijaDifferentYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, false)

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto)
        em.persist(erikoistuvaLaakari)
        em.flush()

        val id = erikoistuvaLaakari.id

        val verificationToken = VerificationToken(
            user = User(id = erikoistuvaLaakari.kayttaja?.user?.id)
        )
        em.persist(verificationToken)

        restMockMvc.perform(
            put("/api/$virkailijaRolePath/erikoistuvat-laakarit/$id/kutsu")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun resendErikoistuvaLaakariInvitationByPaakayttajaDifferentYliopisto() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto)
        em.persist(erikoistuvaLaakari)
        em.flush()

        val id = erikoistuvaLaakari.id

        val verificationToken = VerificationToken(
            user = User(id = erikoistuvaLaakari.kayttaja?.user?.id)
        )
        em.persist(verificationToken)

        restMockMvc.perform(
            put("/api/$tekninenPaakayttajaRolePath/erikoistuvat-laakarit/$id/kutsu")
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isNoContent)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun postVastuuhenkiloWithoutYliopistotAndErikoisalat(rolePath: String) {
        initTest(getRole(rolePath), true)

        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO().apply {
            yliopisto = YliopistoDTO(id = 1000)
        }

        restMockMvc.perform(
            post("/api/$rolePath/vastuuhenkilot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().is5xxServerError)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun postVastuuhenkiloWithoutYliopisto(rolePath: String) {
        initTest(getRole(rolePath), true)

        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO().apply {
            yliopistotAndErikoisalat = setOf(KayttajaYliopistoErikoisalaDTO(yliopisto = YliopistoDTO(id = 1000)))
        }

        restMockMvc.perform(
            post("/api/$rolePath/vastuuhenkilot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().is5xxServerError)
    }

    @Test
    @Transactional
    fun postVastuuhenkiloAsTekninenPaakayttajaWithDifferentYliopistoInYliopistotAndErikoisalat() {
        initTest(TEKNINEN_PAAKAYTTAJA)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val erikoisalaDTO = erikoisalaMapper.toDto(erikoisala)

        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO().apply {
            yliopisto = YliopistoDTO(id = 1000)
            yliopistotAndErikoisalat = setOf(KayttajaYliopistoErikoisalaDTO(yliopisto = YliopistoDTO(id = 1001), erikoisala = erikoisalaDTO))
        }

        restMockMvc.perform(
            post("/api/$tekninenPaakayttajaRolePath/vastuuhenkilot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun postVastuuhenkiloNotAllowedToCreateDifferentYliopistoAssignedToVirkailija() {
        initTest(getRole(OPINTOHALLINNON_VIRKAILIJA))

        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO().apply {
            yliopisto = YliopistoDTO(id = 1000)
            yliopistotAndErikoisalat = setOf(KayttajaYliopistoErikoisalaDTO(yliopisto = YliopistoDTO(id = 1000), erikoisala = ErikoisalaDTO(id = 1)))
        }

        restMockMvc.perform(
            post("/api/$virkailijaRolePath/vastuuhenkilot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun postVastuuhenkiloWithEmailAddressInUse(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkilo = createVastuuhenkilo(yliopisto, erikoisala)
        em.persist(vastuuhenkilo)
        vastuuhenkilo.user?.email = KayttajaHelper.DEFAULT_EMAIL

        val yliopistoDTO = yliopistoMapper.toDto(yliopisto)
        val erikoisalaDTO = erikoisalaMapper.toDto(erikoisala)
        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO().apply {
            yliopisto = yliopistoDTO
            yliopistotAndErikoisalat = setOf(KayttajaYliopistoErikoisalaDTO(yliopisto = yliopistoDTO, erikoisala = erikoisalaDTO))
            sahkoposti = KayttajaHelper.DEFAULT_EMAIL
        }

        restMockMvc.perform(
            post("/api/$rolePath/vastuuhenkilot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun postVastuuhenkiloWithEppnInUse(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkilo = createVastuuhenkilo(yliopisto, erikoisala)
        em.persist(vastuuhenkilo)
        vastuuhenkilo.user?.eppn = DEFAULT_EPPN
        vastuuhenkilo.user?.email = KayttajaHelper.DEFAULT_EMAIL

        val yliopistoDTO = yliopistoMapper.toDto(yliopisto)
        val erikoisalaDTO = erikoisalaMapper.toDto(erikoisala)
        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO().apply {
            yliopisto = yliopistoDTO
            yliopistotAndErikoisalat = setOf(KayttajaYliopistoErikoisalaDTO(yliopisto = yliopistoDTO, erikoisala = erikoisalaDTO))
            sahkoposti = KayttajaHelper.DEFAULT_EMAIL + "x"
        }

        restMockMvc.perform(
            post("/api/$rolePath/vastuuhenkilot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun postVastuuhenkiloOnlyInCurrentErikoisalaNotAllTehtavatAssigned(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkilonTehtavat = erikoisala.vastuuhenkilonTehtavatyypit!!.toList().dropLast(1)
            .map { vastuuhenkilonTehtavatyyppiMapper.toDto(it) }

        val yliopistoDTO = yliopistoMapper.toDto(yliopisto)
        val erikoisalaDTO = erikoisalaMapper.toDto(erikoisala)
        val kayttajaYliopistoErikoisalaDTO =
            KayttajaYliopistoErikoisalaDTO(yliopisto = yliopistoDTO, erikoisala = erikoisalaDTO)
        kayttajaYliopistoErikoisalaDTO.vastuuhenkilonTehtavat.addAll(vastuuhenkilonTehtavat)
        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO().apply {
            yliopisto = yliopistoDTO
            yliopistotAndErikoisalat = setOf(kayttajaYliopistoErikoisalaDTO)
        }

        restMockMvc.perform(
            post("/api/$rolePath/vastuuhenkilot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun postVastuuhenkiloOnlyInCurrentErikoisalaAllTehtavatAssigned(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkilonTehtavat =
            erikoisala.vastuuhenkilonTehtavatyypit!!.toList().map { vastuuhenkilonTehtavatyyppiMapper.toDto(it) }

        val yliopistoDTO = yliopistoMapper.toDto(yliopisto)
        val erikoisalaDTO = erikoisalaMapper.toDto(erikoisala)
        val kayttajaYliopistoErikoisalaDTO =
            KayttajaYliopistoErikoisalaDTO(yliopisto = yliopistoDTO, erikoisala = erikoisalaDTO)
        kayttajaYliopistoErikoisalaDTO.vastuuhenkilonTehtavat.addAll(vastuuhenkilonTehtavat)
        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO().apply {
            yliopisto = yliopistoDTO
            yliopistotAndErikoisalat = setOf(kayttajaYliopistoErikoisalaDTO)
        }

        restMockMvc.perform(
            post("/api/$rolePath/vastuuhenkilot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isCreated)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun postVastuuhenkiloErikosalaTehtavaAlreadyAssignedButNotReassigned(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkilo = createVastuuhenkilo(yliopisto, erikoisala)
        em.persist(vastuuhenkilo)

        val vastuuhenkilonTehtava = em.findAll(VastuuhenkilonTehtavatyyppi::class)[0]
        vastuuhenkilo.yliopistotAndErikoisalat.first().vastuuhenkilonTehtavat.add(vastuuhenkilonTehtava)

        val yliopistoDTO = yliopistoMapper.toDto(yliopisto)
        val erikoisalaDTO = erikoisalaMapper.toDto(erikoisala)
        val kayttajaYliopistoErikoisalaDTO =
            KayttajaYliopistoErikoisalaDTO(yliopisto = yliopistoDTO, erikoisala = erikoisalaDTO)
        kayttajaYliopistoErikoisalaDTO.vastuuhenkilonTehtavat.add(
            vastuuhenkilonTehtavatyyppiMapper.toDto(
                vastuuhenkilonTehtava
            )
        )
        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO().apply {
            yliopisto = yliopistoDTO
            yliopistotAndErikoisalat = setOf(kayttajaYliopistoErikoisalaDTO)
        }

        restMockMvc.perform(
            post("/api/$rolePath/vastuuhenkilot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun postVastuuhenkiloErikosalaTehtavaAlreadyAssignedAndReassignedByRemove(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkilo = createVastuuhenkilo(yliopisto, erikoisala)
        em.persist(vastuuhenkilo)

        val vastuuhenkilonTehtava = em.findAll(VastuuhenkilonTehtavatyyppi::class)[0]
        vastuuhenkilo.yliopistotAndErikoisalat.first().vastuuhenkilonTehtavat.add(vastuuhenkilonTehtava)

        val yliopistoDTO = yliopistoMapper.toDto(yliopisto)
        val erikoisalaDTO = erikoisalaMapper.toDto(erikoisala)
        val kayttajaYliopistoErikoisalaDTO =
            KayttajaYliopistoErikoisalaDTO(yliopisto = yliopistoDTO, erikoisala = erikoisalaDTO)
        kayttajaYliopistoErikoisalaDTO.vastuuhenkilonTehtavat.add(
            vastuuhenkilonTehtavatyyppiMapper.toDto(
                vastuuhenkilonTehtava
            )
        )
        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO().apply {
            yliopisto = yliopistoDTO
            yliopistotAndErikoisalat = setOf(kayttajaYliopistoErikoisalaDTO)
            reassignedTehtavat = setOf(
                ReassignedVastuuhenkilonTehtavaDTO(
                    kayttajaYliopistoErikoisalaMapper.toDto(vastuuhenkilo.yliopistotAndErikoisalat.first()),
                    vastuuhenkilonTehtava.id,
                    ReassignedVastuuhenkilonTehtavaTyyppi.REMOVE
                )
            )
        }

        restMockMvc.perform(
            post("/api/$rolePath/vastuuhenkilot")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isCreated)

        val originalVastuuhenkilonTehtavat = kayttajaRepository.findById(vastuuhenkilo.id!!)
            .get().yliopistotAndErikoisalat.first().vastuuhenkilonTehtavat
        assertThat(!originalVastuuhenkilonTehtavat.contains(vastuuhenkilonTehtava))
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun putVastuuhenkiloTehtavaRemovedButNotReassigned(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkilo = createVastuuhenkilo(yliopisto, erikoisala)
        em.persist(vastuuhenkilo)

        val vastuuhenkilonTehtava = em.findAll(VastuuhenkilonTehtavatyyppi::class)[0]
        vastuuhenkilo.yliopistotAndErikoisalat.first().vastuuhenkilonTehtavat.add(vastuuhenkilonTehtava)

        val kayttajaYliopistoErikoisalaDTO =
            kayttajaYliopistoErikoisalaMapper.toDto(vastuuhenkilo.yliopistotAndErikoisalat.first())
        kayttajaYliopistoErikoisalaDTO.vastuuhenkilonTehtavat.clear()
        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO().apply {
            yliopistotAndErikoisalat = setOf(kayttajaYliopistoErikoisalaDTO)
        }

        restMockMvc.perform(
            put("/api/$rolePath/vastuuhenkilot/${vastuuhenkilo.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun putVastuuhenkiloTehtavaRemovedAndReassigned(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkiloForReassignedTehtava = createVastuuhenkilo(yliopisto, erikoisala)
        em.persist(vastuuhenkiloForReassignedTehtava)
        val vastuuhenkilo = createVastuuhenkilo(yliopisto, erikoisala)
        vastuuhenkilo.tila = KayttajatilinTila.KUTSUTTU
        em.persist(vastuuhenkilo)

        val vastuuhenkilonTehtava = em.findAll(VastuuhenkilonTehtavatyyppi::class)[0]
        vastuuhenkilo.yliopistotAndErikoisalat.first().vastuuhenkilonTehtavat.add(vastuuhenkilonTehtava)

        val kayttajaYliopistoErikoisalaDTO =
            kayttajaYliopistoErikoisalaMapper.toDto(vastuuhenkilo.yliopistotAndErikoisalat.first())
        kayttajaYliopistoErikoisalaDTO.vastuuhenkilonTehtavat.clear()
        val updatedSahkoposti = KayttajaHelper.DEFAULT_EMAIL + "x"
        val updatedEppn = DEFAULT_EPPN + "x"
        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO().apply {
            sahkoposti = updatedSahkoposti
            eppn = updatedEppn
            yliopistotAndErikoisalat = setOf(kayttajaYliopistoErikoisalaDTO)
            reassignedTehtavat = setOf(
                ReassignedVastuuhenkilonTehtavaDTO(
                    kayttajaYliopistoErikoisalaMapper.toDto(vastuuhenkiloForReassignedTehtava.yliopistotAndErikoisalat.first()),
                    vastuuhenkilonTehtava.id,
                    ReassignedVastuuhenkilonTehtavaTyyppi.ADD
                )
            )
        }

        restMockMvc.perform(
            put("/api/$rolePath/vastuuhenkilot/${vastuuhenkilo.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isOk)

        val updatedVastuuhenkilo = kayttajaRepository.findById(vastuuhenkilo.id!!).get()
        assertThat(updatedVastuuhenkilo.user?.email).isEqualTo(updatedSahkoposti)
        assertThat(updatedVastuuhenkilo.user?.eppn).isEqualTo(updatedEppn)
        assertThat(updatedVastuuhenkilo.yliopistotAndErikoisalat).size().isEqualTo(1)
        assertThat(updatedVastuuhenkilo.yliopistotAndErikoisalat.first().vastuuhenkilonTehtavat).size().isEqualTo(0)

        val updatedVastuuhenkiloWithReassignedTehtava =
            kayttajaRepository.findById(vastuuhenkiloForReassignedTehtava.id!!).get()
        assertThat(
            updatedVastuuhenkiloWithReassignedTehtava.yliopistotAndErikoisalat.first().vastuuhenkilonTehtavat.contains(
                vastuuhenkilonTehtava
            )
        )
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun putVastuuhenkiloWithNewErikoisala(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val erikoisala2 = erikoisalaRepository.findById(2).get()
        val vastuuhenkilo = createVastuuhenkilo(yliopisto, erikoisala)
        em.persist(vastuuhenkilo)

        val vastuuhenkilonTehtava = em.findAll(VastuuhenkilonTehtavatyyppi::class)[0]
        val yliopistotAndErikoisalatList = vastuuhenkilo.yliopistotAndErikoisalat.toList()
        val existingKayttajaYliopistoErikoisalaDTO =
            kayttajaYliopistoErikoisalaMapper.toDto(yliopistotAndErikoisalatList[0])
        val newKayttajaYliopistoErikoisalaDTO = KayttajaYliopistoErikoisalaDTO(
            erikoisala = erikoisalaMapper.toDto(erikoisala2),
            yliopisto = yliopistoMapper.toDto(yliopisto),
            vastuuhenkilonTehtavat = mutableSetOf(vastuuhenkilonTehtavatyyppiMapper.toDto(vastuuhenkilonTehtava))
        )

        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO().apply {
            yliopistotAndErikoisalat = setOf(existingKayttajaYliopistoErikoisalaDTO, newKayttajaYliopistoErikoisalaDTO)
        }

        restMockMvc.perform(
            put("/api/$rolePath/vastuuhenkilot/${vastuuhenkilo.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isOk)

        val updatedVastuuhenkilo = kayttajaRepository.findById(vastuuhenkilo.id!!).get()
        assertThat(updatedVastuuhenkilo.yliopistotAndErikoisalat).size().isEqualTo(2)
        val newYliopistoAndErikoisala = updatedVastuuhenkilo.yliopistotAndErikoisalat.toList()[1]
        assertThat(newYliopistoAndErikoisala.yliopisto).isEqualTo(yliopisto)
        assertThat(newYliopistoAndErikoisala.erikoisala).isEqualTo(erikoisala2)
        assertThat(newYliopistoAndErikoisala.vastuuhenkilonTehtavat).contains(vastuuhenkilonTehtava)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun putVastuuhenkiloWithRemovedErikoisala(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val erikoisala2 = erikoisalaRepository.findById(2).get()
        val vastuuhenkilo = createVastuuhenkilo(yliopisto, erikoisala)
        val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = vastuuhenkilo,
            yliopisto = yliopisto,
            erikoisala = erikoisala2
        )
        em.persist(yliopistoAndErikoisala)
        vastuuhenkilo.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

        val remainingKayttajaYliopistoErikoisalaDTO =
            kayttajaYliopistoErikoisalaMapper.toDto(vastuuhenkilo.yliopistotAndErikoisalat.first())
        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO().apply {
            yliopistotAndErikoisalat = setOf(remainingKayttajaYliopistoErikoisalaDTO)
        }

        restMockMvc.perform(
            put("/api/$rolePath/vastuuhenkilot/${vastuuhenkilo.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isOk)

        val updatedVastuuhenkilo = kayttajaRepository.findById(vastuuhenkilo.id!!).get()
        assertThat(updatedVastuuhenkilo.yliopistotAndErikoisalat).size().isEqualTo(1)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun putVastuuhenkiloWithRemovedErikoisalaTehtavaNotReassigned(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val erikoisala2 = erikoisalaRepository.findById(2).get()
        val vastuuhenkilo = createVastuuhenkilo(yliopisto, erikoisala)
        val vastuuhenkilonTehtava = em.findAll(VastuuhenkilonTehtavatyyppi::class)[0]
        val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = vastuuhenkilo,
            yliopisto = yliopisto,
            erikoisala = erikoisala2
        )
        em.persist(yliopistoAndErikoisala)
        yliopistoAndErikoisala.vastuuhenkilonTehtavat.add(vastuuhenkilonTehtava)
        vastuuhenkilo.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

        val remainingKayttajaYliopistoErikoisalaDTO =
            kayttajaYliopistoErikoisalaMapper.toDto(vastuuhenkilo.yliopistotAndErikoisalat.first())
        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO().apply {
            yliopistotAndErikoisalat = setOf(remainingKayttajaYliopistoErikoisalaDTO)
        }

        restMockMvc.perform(
            put("/api/$rolePath/vastuuhenkilot/${vastuuhenkilo.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun postVirkailijaWithoutYliopisto(rolePath: String) {
        initTest(getRole(rolePath), true)

        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO()

        restMockMvc.perform(
            post("/api/$rolePath/virkailijat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().is5xxServerError)
    }

    @Test
    @Transactional
    fun postVirkailijaNotAllowedToCreateDifferentYliopistoAssignedToVirkailija() {
        initTest(getRole(OPINTOHALLINNON_VIRKAILIJA))

        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO().apply {
            yliopisto = YliopistoDTO(id = 1000)
        }

        restMockMvc.perform(
            post("/api/$virkailijaRolePath/virkailijat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun postVirkailijaWithEmailAddressInUse(rolePath: String) {
        initTest(getRole(rolePath), true)

        val virkailija = createVirkailija(yliopisto)
        em.persist(virkailija)
        virkailija.user?.email = KayttajaHelper.DEFAULT_EMAIL

        val yliopistoDTO = yliopistoMapper.toDto(yliopisto)
        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO().apply {
            yliopisto = yliopistoDTO
            sahkoposti = KayttajaHelper.DEFAULT_EMAIL
        }

        restMockMvc.perform(
            post("/api/$rolePath/virkailijat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun postVirkailijaWithEppnInUse(rolePath: String) {
        initTest(getRole(rolePath), true)

        val virkailija = createVirkailija(yliopisto)
        em.persist(virkailija)
        virkailija.user?.eppn = DEFAULT_EPPN
        virkailija.user?.email = KayttajaHelper.DEFAULT_EMAIL

        val yliopistoDTO = yliopistoMapper.toDto(yliopisto)
        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO().apply {
            yliopisto = yliopistoDTO
            sahkoposti = KayttajaHelper.DEFAULT_EMAIL + "x"
        }

        restMockMvc.perform(
            post("/api/$rolePath/virkailijat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun postVirkailija(rolePath: String) {
        initTest(getRole(rolePath), true)

        val yliopistoDTO = yliopistoMapper.toDto(yliopisto)
        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO().apply {
            yliopisto = yliopistoDTO
        }

        restMockMvc.perform(
            post("/api/$rolePath/virkailijat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isCreated)
    }

    @Test
    @Transactional
    fun tryToPostPaakayttajaAsVirkailija() {
        initTest(getRole(OPINTOHALLINNON_VIRKAILIJA))

        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO()

        restMockMvc.perform(
            post("/api/$virkailijaRolePath/paakayttajat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun postPaakayttajaWithEmailAddressInUse() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)

        val paakayttaja = createPaakayttaja()
        em.persist(paakayttaja)
        paakayttaja.user?.email = KayttajaHelper.DEFAULT_EMAIL

        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO().apply {
            sahkoposti = KayttajaHelper.DEFAULT_EMAIL
        }

        restMockMvc.perform(
            post("/api/$tekninenPaakayttajaRolePath/paakayttajat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun postPaakayttajaWithEppnInUse() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)

        val paakayttaja = createPaakayttaja()
        em.persist(paakayttaja)
        paakayttaja.user?.eppn = DEFAULT_EPPN
        paakayttaja.user?.email = KayttajaHelper.DEFAULT_EMAIL

        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO().apply {
            sahkoposti = KayttajaHelper.DEFAULT_EMAIL + "x"
        }

        restMockMvc.perform(
            post("/api/$tekninenPaakayttajaRolePath/paakayttajat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun postPaakayttaja() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)

        val kayttajahallintaKayttajaDTO = getDefaultKayttajaDTO()

        restMockMvc.perform(
            post("/api/$tekninenPaakayttajaRolePath/paakayttajat")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isCreated)
    }

    @Test
    @Transactional
    fun patchVirkailijaNotAllowedToModifyDifferentYliopistoAssignedToVirkailija() {
        initTest(getRole(OPINTOHALLINNON_VIRKAILIJA), false)

        val virkailija = createVirkailija(yliopisto).apply { user?.eppn = DEFAULT_EPPN }
        em.persist(virkailija)

        val kayttajahallintaKayttajaDTO = KayttajahallintaKayttajaDTO(
            sahkoposti = virkailija.user?.email + "x",
            eppn = virkailija.user?.eppn + "x"
        )

        restMockMvc.perform(
            patch("/api/$virkailijaRolePath/virkailijat/${virkailija.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun patchVirkailijaOnlyEmailUpdated(rolePath: String) {
        initTest(getRole(rolePath), true)

        val virkailija = createVirkailija(yliopisto).apply { user?.eppn = DEFAULT_EPPN }
        em.persist(virkailija)

        val kayttajahallintaKayttajaDTO = KayttajahallintaKayttajaDTO(
            sahkoposti = virkailija.user?.email + "x",
            eppn = virkailija.user?.eppn + "x"
        )

        restMockMvc.perform(
            patch("/api/$rolePath/virkailijat/${virkailija.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isOk)

        val updatedVirkailija = kayttajaRepository.findById(virkailija.id!!).get()
        assertThat(updatedVirkailija.user?.email).isEqualTo(kayttajahallintaKayttajaDTO.sahkoposti)
        assertThat(updatedVirkailija.user?.eppn).isEqualTo(virkailija.user?.eppn)
    }

    @ParameterizedTest
    @ValueSource(strings = [tekninenPaakayttajaRolePath, virkailijaRolePath])
    @Transactional
    fun patchVirkailija(rolePath: String) {
        initTest(getRole(rolePath), true)

        val virkailija = createVirkailija(yliopisto).apply { user?.eppn = DEFAULT_EPPN }
        virkailija.tila = KayttajatilinTila.KUTSUTTU
        em.persist(virkailija)

        val kayttajahallintaKayttajaDTO = KayttajahallintaKayttajaDTO(
            sahkoposti = virkailija.user?.email + "x",
            eppn = virkailija.user?.eppn + "x"
        )

        restMockMvc.perform(
            patch("/api/$rolePath/virkailijat/${virkailija.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isOk)

        val updatedVirkailija = kayttajaRepository.findById(virkailija.id!!).get()
        assertThat(updatedVirkailija.user?.email).isEqualTo(kayttajahallintaKayttajaDTO.sahkoposti)
        assertThat(updatedVirkailija.user?.eppn).isEqualTo(kayttajahallintaKayttajaDTO.eppn)
    }

    @Test
    @Transactional
    fun tryToPatchPaakayttajaAsVirkailija() {
        initTest(getRole(OPINTOHALLINNON_VIRKAILIJA), true)

        val paakayttaja = createPaakayttaja().apply { user?.eppn = DEFAULT_EPPN }
        em.persist(paakayttaja)

        val kayttajahallintaKayttajaDTO = KayttajahallintaKayttajaDTO(
            sahkoposti = paakayttaja.user?.email + "x",
            eppn = paakayttaja.user?.eppn + "x"
        )

        restMockMvc.perform(
            patch("/api/$virkailijaRolePath/paakayttajat/${paakayttaja.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun patchPaakayttajaOnlyEmailUpdated() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)

        val paakayttaja = createPaakayttaja().apply { user?.eppn = DEFAULT_EPPN }
        em.persist(paakayttaja)

        val kayttajahallintaKayttajaDTO = KayttajahallintaKayttajaDTO(
            sahkoposti = paakayttaja.user?.email + "x",
            eppn = paakayttaja.user?.eppn + "x"
        )

        restMockMvc.perform(
            patch("/api/$tekninenPaakayttajaRolePath/virkailijat/${paakayttaja.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isOk)

        val updatedPaakayttaja = kayttajaRepository.findById(paakayttaja.id!!).get()
        assertThat(updatedPaakayttaja.user?.email).isEqualTo(kayttajahallintaKayttajaDTO.sahkoposti)
        assertThat(updatedPaakayttaja.user?.eppn).isEqualTo(paakayttaja.user?.eppn)
    }

    @Test
    @Transactional
    fun patchPaakayttaja() {
        initTest(TEKNINEN_PAAKAYTTAJA, false)

        val paakayttaja = createVirkailija(yliopisto).apply { user?.eppn = DEFAULT_EPPN }
        paakayttaja.tila = KayttajatilinTila.KUTSUTTU
        em.persist(paakayttaja)

        val kayttajahallintaKayttajaDTO = KayttajahallintaKayttajaDTO(
            sahkoposti = paakayttaja.user?.email + "x",
            eppn = paakayttaja.user?.eppn + "x"
        )

        restMockMvc.perform(
            patch("/api/$tekninenPaakayttajaRolePath/virkailijat/${paakayttaja.id}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isOk)

        val updatedPaakayttaja = kayttajaRepository.findById(paakayttaja.id!!).get()
        assertThat(updatedPaakayttaja.user?.email).isEqualTo(kayttajahallintaKayttajaDTO.sahkoposti)
        assertThat(updatedPaakayttaja.user?.eppn).isEqualTo(kayttajahallintaKayttajaDTO.eppn)
    }

    private fun createKouluttaja(
        yliopisto: Yliopisto,
        erikoisala: Erikoisala
    ): Kayttaja {
        val kouluttajaUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(
                KOULUTTAJA
            )
        )
        em.persist(kouluttajaUser)
        return createKayttajaWithYliopistoAndErikoisala(kouluttajaUser, yliopisto, erikoisala)
    }

    private fun createVastuuhenkilo(
        yliopisto: Yliopisto,
        erikoisala: Erikoisala
    ): Kayttaja {
        val vastuuhenkiloUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(
                VASTUUHENKILO
            )
        )
        em.persist(vastuuhenkiloUser)
        return createKayttajaWithYliopistoAndErikoisala(vastuuhenkiloUser, yliopisto, erikoisala)
    }

    private fun createVirkailija(
        yliopisto: Yliopisto
    ): Kayttaja {
        val virkailijaUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(
                OPINTOHALLINNON_VIRKAILIJA
            )
        )
        em.persist(virkailijaUser)
        return createKayttajaWithYliopisto(virkailijaUser, yliopisto)
    }

    private fun createPaakayttaja(): Kayttaja {
        val paakayttajaUser = KayttajaResourceWithMockUserIT.createEntity(
            authority = Authority(
                TEKNINEN_PAAKAYTTAJA
            )
        )
        em.persist(paakayttajaUser)

        val kayttaja = KayttajaHelper.createEntity(em, paakayttajaUser)
        em.persist(kayttaja)

        return kayttaja
    }

    private fun createKayttajaWithYliopistoAndErikoisala(
        user: User,
        yliopisto: Yliopisto,
        erikoisala: Erikoisala
    ): Kayttaja {
        val kayttaja = KayttajaHelper.createEntity(em, user)
        em.persist(kayttaja)

        val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(
            kayttaja = kayttaja,
            yliopisto = yliopisto,
            erikoisala = erikoisala
        )

        em.persist(yliopistoAndErikoisala)
        kayttaja.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

        return kayttaja
    }

    private fun createKayttajaWithYliopisto(
        user: User,
        yliopisto: Yliopisto
    ): Kayttaja {
        val kayttaja = KayttajaHelper.createEntity(em, user).apply {
            yliopistot = mutableSetOf(yliopisto)
        }
        em.persist(kayttaja)

        return kayttaja
    }

    fun getRole(rolePath: String): String {
        return if (rolePath == tekninenPaakayttajaRolePath) {
            TEKNINEN_PAAKAYTTAJA
        } else OPINTOHALLINNON_VIRKAILIJA
    }

    fun initTest(role: String, addDefaultYliopistoForRole: Boolean = false) {
        val user = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(role))
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>()
        val authorities = listOf(SimpleGrantedAuthority(role))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )

        val kayttaja = KayttajaHelper.createEntity(em, user)
        if (role == OPINTOHALLINNON_VIRKAILIJA) {
            if (addDefaultYliopistoForRole) {
                kayttaja.yliopistot.add(yliopisto)
            } else {
                val yliopisto = Yliopisto(nimi = YliopistoEnum.ITA_SUOMEN_YLIOPISTO)
                em.persist(yliopisto)
                kayttaja.yliopistot.add(yliopisto)
            }
        }
        em.persist(kayttaja)

        TestSecurityContextHolder.getContext().authentication = authentication
    }

    companion object {
        private val random: Random = SecureRandom()
        private val count: AtomicLong = AtomicLong(random.nextInt().toLong() + (2L * Integer.MAX_VALUE))
        private const val DEFAULT_FIRST_NAME = "John"
        private const val DEFAULT_LAST_NAME = "DOE"
        private const val DEFAULT_EPPN = "johdoe"

        @JvmStatic
        fun getDefaultErikoistuvaLaakariDTO(): KayttajahallintaErikoistuvaLaakariDTO {
            return KayttajahallintaErikoistuvaLaakariDTO(
                etunimi = DEFAULT_FIRST_NAME,
                sukunimi = DEFAULT_LAST_NAME,
                sahkopostiosoite = KayttajaHelper.DEFAULT_EMAIL,
                opiskelijatunnus = "123456",
                opintooikeusAlkaa = LocalDate.ofEpochDay(0L),
                opintooikeusPaattyy = LocalDate.ofEpochDay(30L),
                osaamisenArvioinninOppaanPvm = LocalDate.ofEpochDay(0L)
            )
        }

        @JvmStatic
        fun getDefaultKayttajaDTO(): KayttajahallintaKayttajaDTO {
            return KayttajahallintaKayttajaDTO(
                etunimi = DEFAULT_FIRST_NAME,
                sukunimi = DEFAULT_LAST_NAME,
                sahkoposti = KayttajaHelper.DEFAULT_EMAIL,
                eppn = DEFAULT_EPPN
            )
        }


    }
}
