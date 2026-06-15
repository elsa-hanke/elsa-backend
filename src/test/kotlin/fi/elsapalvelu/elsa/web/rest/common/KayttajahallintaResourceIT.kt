package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.*
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.*
import fi.elsapalvelu.elsa.service.dto.*
import fi.elsapalvelu.elsa.service.dto.enumeration.ReassignedVastuuhenkilonTehtavaTyyppi
import fi.elsapalvelu.elsa.service.dto.kayttajahallinta.*
import fi.elsapalvelu.elsa.service.mapper.*
import fi.elsapalvelu.elsa.web.rest.*
import fi.elsapalvelu.elsa.web.rest.helpers.*
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper.DEFAULT_YLIOPISTO
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.*
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.*
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional

private const val TEKNINEN_PAAKAYTTAJA_ROLE_PATH = "tekninen-paakayttaja"
private const val VIRKAILIJA_ROLE_PATH = "virkailija"

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class KayttajahallintaResourceIT : ResourceIntegrationTestBase() {

    @Autowired private lateinit var erikoisalaRepository: ErikoisalaRepository
    @Autowired private lateinit var kayttajaRepository: KayttajaRepository
    @Autowired private lateinit var yliopistoMapper: YliopistoMapper
    @Autowired private lateinit var erikoisalaMapper: ErikoisalaMapper
    @Autowired private lateinit var vastuuhenkilonTehtavatyyppiMapper: VastuuhenkilonTehtavatyyppiMapper
    @Autowired private lateinit var kayttajaYliopistoErikoisalaMapper: KayttajaYliopistoErikoisalaMapper

    private lateinit var yliopisto: Yliopisto

    @Test
    fun getErikoistuvaLaakariForVirkailijaSameYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, true)
        val id = KayttajahallintaResourceHelper.persistErikoistuvaLaakari(em, yliopisto)
        testMockMvc.perform(get("/api/$VIRKAILIJA_ROLE_PATH/erikoistuvat-laakarit/$id")).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.kayttaja.id").value(id)).andExpect(jsonPath("$.erikoistuvaLaakari.id").exists())
    }

    @Test
    fun getErikoistuvaLaakariForVirkailijaDifferentYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, false)
        val id = KayttajahallintaResourceHelper.persistErikoistuvaLaakari(em, yliopisto)
        testMockMvc.perform(get("/api/$VIRKAILIJA_ROLE_PATH/erikoistuvat-laakarit/$id")).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun getNonExistingErikoistuvaLaakari(rolePath: String) {
        initTest(getRole(rolePath))
        val id = KayttajahallintaResourceHelper.count.incrementAndGet()
        testMockMvc.perform(get("/api/$rolePath/erikoistuvat-laakarit/$id")).andExpect(status().isBadRequest)
    }

    @Test
    fun getVastuuhenkiloForVirkailijaSameYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, true)
        val erikoisala = erikoisalaRepository.findById(1).get()
        val id = KayttajahallintaResourceHelper.persistVastuuhenkilo(em, yliopisto, erikoisala)
        testMockMvc.perform(get("/api/$VIRKAILIJA_ROLE_PATH/kayttajat/$id")).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.kayttaja.id").value(id))
    }

    @Test
    fun getVastuuhenkiloForVirkailijaDifferentYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, false)
        val erikoisala = erikoisalaRepository.findById(1).get()
        val id = KayttajahallintaResourceHelper.persistVastuuhenkilo(em, yliopisto, erikoisala)
        testMockMvc.perform(get("/api/$VIRKAILIJA_ROLE_PATH/kayttajat/$id")).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun getNonExistingKayttaja(rolePath: String) {
        initTest(getRole(rolePath))
        val id = KayttajahallintaResourceHelper.count.incrementAndGet()
        testMockMvc.perform(get("/api/$rolePath/kayttajat/$id")).andExpect(status().isBadRequest)
    }

    @Test
    fun getKouluttajaForVirkailijaSameYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, true)
        val erikoisala = erikoisalaRepository.findById(1).get()
        val id = KayttajahallintaResourceHelper.persistKouluttaja(em, yliopisto, erikoisala)
        testMockMvc.perform(get("/api/$VIRKAILIJA_ROLE_PATH/kayttajat/$id")).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.kayttaja.id").value(id))
    }

    @Test
    fun getKouluttajaForVirkailijaDifferentYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, false)
        val erikoisala = erikoisalaRepository.findById(1).get()
        val id = KayttajahallintaResourceHelper.persistKouluttaja(em, yliopisto, erikoisala)
        testMockMvc.perform(get("/api/$VIRKAILIJA_ROLE_PATH/kayttajat/$id")).andExpect(status().isBadRequest)
    }

    @Test
    fun getVirkailijaForVirkailijaSameYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, true)
        val id = KayttajahallintaResourceHelper.persistVirkailija(em, yliopisto)
        testMockMvc.perform(get("/api/$VIRKAILIJA_ROLE_PATH/kayttajat/$id")).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.kayttaja.id").value(id))
    }

    @Test
    fun getVirkailijaForVirkailijaDifferentYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, false)
        val id = KayttajahallintaResourceHelper.persistVirkailija(em, yliopisto)
        testMockMvc.perform(get("/api/$VIRKAILIJA_ROLE_PATH/kayttajat/$id")).andExpect(status().isBadRequest)
    }

    @Test
    fun tryToGetPaakayttajaForVirkailija() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, false)
        val id = KayttajahallintaResourceHelper.persistPaakayttaja(em)
        testMockMvc.perform(get("/api/$TEKNINEN_PAAKAYTTAJA_ROLE_PATH/kayttajat/$id")).andExpect(status().isForbidden)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun getKayttajaForm(rolePath: String) {
        initTest(getRole(rolePath))
        val erikoisalatCountByLiittyElsaan = erikoisalaRepository.findAllByLiittynytElsaanTrue().count()
        testMockMvc.perform(get("/api/$rolePath/erikoistuva-laakari-lomake")).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.yliopistot").value(Matchers.hasSize<Any>(1))).andExpect(jsonPath("$.erikoisalat").value(Matchers.hasSize<Any>(erikoisalatCountByLiittyElsaan)))
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun getErikoistuvatLaakarit(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto)
        persistAndFlush(erikoistuvaLaakari)
        persistAndFlush(ErikoistuvaLaakariHelper.createEntity(em, yliopisto = persistYliopisto(YliopistoEnum.HELSINGIN_YLIOPISTO)))

        // Virkailijalle listataan vain saman yliopiston erikoistujat.
        val expectedSize = if (rolePath == TEKNINEN_PAAKAYTTAJA_ROLE_PATH) 2 else 1

        testMockMvc.perform(get("/api/$rolePath/erikoistuvat-laakarit")).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(Matchers.hasSize<Any>(expectedSize))).andExpect(jsonPath("$.content[0].kayttajaId").value(erikoistuvaLaakari.kayttaja?.id))
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun getKouluttajat(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkilo = KayttajahallintaResourceHelper.createKouluttaja(em, yliopisto, erikoisala)
        KayttajahallintaResourceHelper.createKouluttaja(em, persistYliopisto(YliopistoEnum.ITA_SUOMEN_YLIOPISTO), erikoisala)

        // Virkailijalle listataan vain saman yliopiston kouluttajat.
        val expectedSize = if (rolePath == TEKNINEN_PAAKAYTTAJA_ROLE_PATH) 2 else 1

        testMockMvc.perform(get("/api/$rolePath/kouluttajat")).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(Matchers.hasSize<Any>(expectedSize))).andExpect(jsonPath("$.content[0].kayttajaId").value(vastuuhenkilo.id))
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun getVastuuhenkilot(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkilo = KayttajahallintaResourceHelper.createVastuuhenkilo(em, yliopisto, erikoisala)
        KayttajahallintaResourceHelper.createVastuuhenkilo(em, persistYliopisto(YliopistoEnum.ITA_SUOMEN_YLIOPISTO), erikoisala)

        // Virkailijalle listataan vain saman yliopiston vastuuhenkilöt.
        val expectedSize = if (rolePath == TEKNINEN_PAAKAYTTAJA_ROLE_PATH) 2 else 1

        testMockMvc.perform(get("/api/$rolePath/vastuuhenkilot")).andExpect(status().isOk).andExpect(content().contentType(APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.content").value(Matchers.hasSize<Any>(expectedSize))).andExpect(jsonPath("$.content[0].kayttajaId").value(vastuuhenkilo.id))
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun getVirkailijat(rolePath: String) {
        initTest(getRole(rolePath), true)

        KayttajahallintaResourceHelper.createVirkailija(em, yliopisto)

        val yliopisto2 = persistYliopisto(YliopistoEnum.ITA_SUOMEN_YLIOPISTO)
        KayttajahallintaResourceHelper.createVirkailija(em, yliopisto2)
        KayttajahallintaResourceHelper.createVirkailija(em, yliopisto2)

        // Virkailijalle listataan vain saman yliopiston virkailijat.
        val expectedSize = if (rolePath == TEKNINEN_PAAKAYTTAJA_ROLE_PATH) 3 else 2

        testMockMvc.perform(get("/api/$rolePath/virkailijat")).andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON_VALUE)).andExpect(jsonPath("$.content").value(Matchers.hasSize<Any>(expectedSize)))
    }

    @Test
    fun getPaakayttajatForVirkailijaForbidden() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, true)
        KayttajahallintaResourceHelper.createPaakayttaja(em)
        testMockMvc.perform(get("/api/$VIRKAILIJA_ROLE_PATH/paakayttajat")).andExpect(status().isForbidden)
    }

    @Test
    fun postErikoistuvaLaakariByVirkailijaFromSameYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, true)

        val kayttajahallintaErikoistuvaLaakariDTO = KayttajahallintaResourceHelper.getDefaultErikoistuvaLaakariDTO()
        kayttajahallintaErikoistuvaLaakariDTO.yliopistoId = yliopisto.id
        kayttajahallintaErikoistuvaLaakariDTO.erikoisalaId = createPersistedErikoisala().id
        kayttajahallintaErikoistuvaLaakariDTO.asetusId = em.findAll(Asetus::class).get(0).id
        kayttajahallintaErikoistuvaLaakariDTO.opintoopasId = em.findAll(Opintoopas::class).get(0).id

        testMockMvc.perform(post("/api/$VIRKAILIJA_ROLE_PATH/erikoistuvat-laakarit").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaErikoistuvaLaakariDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isCreated)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun postErikoistuvaLaakariWithExistingEmail(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto)
        persistAndFlush(erikoistuvaLaakari)

        val kayttajahallintaErikoistuvaLaakariDTO = KayttajahallintaResourceHelper.getDefaultErikoistuvaLaakariDTO()
        kayttajahallintaErikoistuvaLaakariDTO.yliopistoId = yliopisto.id
        kayttajahallintaErikoistuvaLaakariDTO.erikoisalaId = createPersistedErikoisala().id
        kayttajahallintaErikoistuvaLaakariDTO.sahkopostiosoite = erikoistuvaLaakari.kayttaja?.user?.email

        testMockMvc.perform(post("/api/$rolePath/erikoistuvat-laakarit").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaErikoistuvaLaakariDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun postErikoistuvaLaakariWithoutValidYliopisto(rolePath: String) {
        initTest(getRole(rolePath), true)

        val kayttajahallintaErikoistuvaLaakariDTO = KayttajahallintaResourceHelper.getDefaultErikoistuvaLaakariDTO()
        kayttajahallintaErikoistuvaLaakariDTO.yliopistoId = KayttajahallintaResourceHelper.count.incrementAndGet()
        kayttajahallintaErikoistuvaLaakariDTO.erikoisalaId = createPersistedErikoisala().id

        testMockMvc.perform(post("/api/$rolePath/erikoistuvat-laakarit").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaErikoistuvaLaakariDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun postErikoistuvaLaakariWithoutValidErikoisala(rolePath: String) {
        initTest(getRole(rolePath), true)

        val kayttajahallintaErikoistuvaLaakariDTO = KayttajahallintaResourceHelper.getDefaultErikoistuvaLaakariDTO()
        kayttajahallintaErikoistuvaLaakariDTO.yliopistoId = yliopisto.id
        kayttajahallintaErikoistuvaLaakariDTO.erikoisalaId = KayttajahallintaResourceHelper.count.incrementAndGet()

        testMockMvc.perform(post("/api/$rolePath/erikoistuvat-laakarit").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaErikoistuvaLaakariDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isBadRequest)
    }

    @Test
    fun patchErikoistuvaLaakariNotAllowedToModifyDifferentYliopistoAssignedToVirkailija() {
        initTest(getRole(OPINTOHALLINNON_VIRKAILIJA), false)

        val erikoistuvaLaakari = createPersistedErikoistuvaLaakari()

        val kayttajahallintaErikoistuvaLaakariDTO = KayttajahallintaErikoistuvaLaakariUpdateDTO(sahkoposti = KayttajaHelper.DEFAULT_EMAIL + "x")
        testMockMvc.perform(patch("/api/$VIRKAILIJA_ROLE_PATH/erikoistuvat-laakarit/${erikoistuvaLaakari.kayttaja?.user?.id}").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaErikoistuvaLaakariDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun patchErikoistuvaLaakari(rolePath: String) {
        initTest(getRole(rolePath), true)

        val kayttajahallintaErikoistuvaLaakariDTO = KayttajahallintaErikoistuvaLaakariUpdateDTO(sahkoposti = KayttajaHelper.DEFAULT_EMAIL + "x")
        val userId = createPersistedErikoistuvaLaakari().kayttaja?.user?.id
        testMockMvc.perform(patch("/api/$rolePath/erikoistuvat-laakarit/$userId").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaErikoistuvaLaakariDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isOk)

        val updatedErikoistuvaLaakari = kayttajaRepository.findOneByUserId(userId!!).get()
        assertThat(updatedErikoistuvaLaakari.user?.email).isEqualTo(kayttajahallintaErikoistuvaLaakariDTO.sahkoposti)
    }

    @Test
    fun resendErikoistuvaLaakariInvitationByVirkailijaInSameYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, true)

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto)
        persistAndFlush(erikoistuvaLaakari)
        em.persist(VerificationToken(user = User(id = erikoistuvaLaakari.kayttaja?.user?.id)))

        testMockMvc.perform(put("/api/$VIRKAILIJA_ROLE_PATH/erikoistuvat-laakarit/${erikoistuvaLaakari.id}/kutsu").with(SecurityMockMvcRequestPostProcessors.csrf()))
            .andExpect(status().isNoContent)
    }

    @Test
    fun resendErikoistuvaLaakariInvitationByVirkailijaDifferentYliopisto() {
        initTest(OPINTOHALLINNON_VIRKAILIJA, false)

        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, yliopisto = yliopisto)
        persistAndFlush(erikoistuvaLaakari)
        em.persist(VerificationToken(user = User(id = erikoistuvaLaakari.kayttaja?.user?.id)))

        testMockMvc.perform(put("/api/$VIRKAILIJA_ROLE_PATH/erikoistuvat-laakarit/$${erikoistuvaLaakari.id}/kutsu").with(SecurityMockMvcRequestPostProcessors.csrf()))
            .andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun postVastuuhenkiloWithoutYliopistotAndErikoisalat(rolePath: String) {
        initTest(getRole(rolePath), true)
        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO().apply { yliopisto = YliopistoDTO(id = 1000) }
        testMockMvc.perform(post("/api/$rolePath/vastuuhenkilot").contentType(APPLICATION_JSON).content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().is5xxServerError)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun postVastuuhenkiloWithoutYliopisto(rolePath: String) {
        initTest(getRole(rolePath), true)
        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO()
            .apply { yliopistotAndErikoisalat = setOf(KayttajaYliopistoErikoisalaDTO(yliopisto = YliopistoDTO(id = 1000))) }
        testMockMvc.perform(post("/api/$rolePath/vastuuhenkilot").contentType(APPLICATION_JSON).content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().is5xxServerError)
    }

    @Test
    fun postVastuuhenkiloNotAllowedToCreateDifferentYliopistoAssignedToVirkailija() {
        initTest(getRole(OPINTOHALLINNON_VIRKAILIJA))

        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO().apply {
            yliopisto = YliopistoDTO(id = 1000)
            yliopistotAndErikoisalat = setOf(KayttajaYliopistoErikoisalaDTO(yliopisto = YliopistoDTO(id = 1000), erikoisala = ErikoisalaDTO(id = 1)))
        }

        testMockMvc.perform(post("/api/$VIRKAILIJA_ROLE_PATH/vastuuhenkilot").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun postVastuuhenkiloWithEmailAddressInUse(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkilo = createPersistedVastuuhenkilo(yliopisto, erikoisala)
        vastuuhenkilo.user?.email = KayttajaHelper.DEFAULT_EMAIL

        val yliopistoDTO = yliopistoMapper.toDto(yliopisto)
        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO().apply {
            yliopisto = yliopistoDTO
            yliopistotAndErikoisalat = setOf(KayttajaYliopistoErikoisalaDTO(yliopisto = yliopistoDTO, erikoisala = erikoisalaMapper.toDto(erikoisala)))
            sahkoposti = KayttajaHelper.DEFAULT_EMAIL
        }

        testMockMvc.perform(post("/api/$rolePath/vastuuhenkilot").contentType(APPLICATION_JSON).content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun postVastuuhenkiloWithEppnInUse(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkilo = createPersistedVastuuhenkilo(yliopisto, erikoisala)
        vastuuhenkilo.user?.eppn = KayttajahallintaResourceHelper.DEFAULT_EPPN
        vastuuhenkilo.user?.email = KayttajaHelper.DEFAULT_EMAIL

        val yliopistoDTO = yliopistoMapper.toDto(yliopisto)
        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO().apply {
            yliopisto = yliopistoDTO
            yliopistotAndErikoisalat = setOf(KayttajaYliopistoErikoisalaDTO(yliopisto = yliopistoDTO, erikoisala = erikoisalaMapper.toDto(erikoisala)))
            sahkoposti = KayttajaHelper.DEFAULT_EMAIL + "x"
        }

        testMockMvc.perform(post("/api/$rolePath/vastuuhenkilot").contentType(APPLICATION_JSON).content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
                .with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun postVastuuhenkiloOnlyInCurrentErikoisalaNotAllTehtavatAssigned(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkilonTehtavat = erikoisala.vastuuhenkilonTehtavatyypit!!.toList().dropLast(1).map { vastuuhenkilonTehtavatyyppiMapper.toDto(it) }

        val yliopistoDTO = yliopistoMapper.toDto(yliopisto)
        val kayttajaYliopistoErikoisalaDTO = KayttajaYliopistoErikoisalaDTO(yliopisto = yliopistoDTO, erikoisala = erikoisalaMapper.toDto(erikoisala))
        kayttajaYliopistoErikoisalaDTO.vastuuhenkilonTehtavat.addAll(vastuuhenkilonTehtavat)
        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO().apply {
            yliopisto = yliopistoDTO
            yliopistotAndErikoisalat = setOf(kayttajaYliopistoErikoisalaDTO)
        }

        testMockMvc.perform(post("/api/$rolePath/vastuuhenkilot").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun postVastuuhenkiloOnlyInCurrentErikoisalaAllTehtavatAssigned(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkilonTehtavat = erikoisala.vastuuhenkilonTehtavatyypit!!.toList().map { vastuuhenkilonTehtavatyyppiMapper.toDto(it) }

        val yliopistoDTO = yliopistoMapper.toDto(yliopisto)
        val kayttajaYliopistoErikoisalaDTO = KayttajaYliopistoErikoisalaDTO(yliopisto = yliopistoDTO, erikoisala = erikoisalaMapper.toDto(erikoisala))
        kayttajaYliopistoErikoisalaDTO.vastuuhenkilonTehtavat.addAll(vastuuhenkilonTehtavat)
        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO().apply {
            yliopisto = yliopistoDTO
            yliopistotAndErikoisalat = setOf(kayttajaYliopistoErikoisalaDTO)
        }

        testMockMvc.perform(post("/api/$rolePath/vastuuhenkilot").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isCreated)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun postVastuuhenkiloErikosalaTehtavaAlreadyAssignedButNotReassigned(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkilo = createPersistedVastuuhenkilo(yliopisto, erikoisala)

        val vastuuhenkilonTehtava = em.findAll(VastuuhenkilonTehtavatyyppi::class)[0]
        vastuuhenkilo.yliopistotAndErikoisalat.first().vastuuhenkilonTehtavat.add(vastuuhenkilonTehtava)

        val yliopistoDTO = yliopistoMapper.toDto(yliopisto)
        val kayttajaYliopistoErikoisalaDTO = KayttajaYliopistoErikoisalaDTO(yliopisto = yliopistoDTO, erikoisala = erikoisalaMapper.toDto(erikoisala))
        kayttajaYliopistoErikoisalaDTO.vastuuhenkilonTehtavat.add(vastuuhenkilonTehtavatyyppiMapper.toDto(vastuuhenkilonTehtava))
        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO().apply {
            yliopisto = yliopistoDTO
            yliopistotAndErikoisalat = setOf(kayttajaYliopistoErikoisalaDTO)
        }

        testMockMvc.perform(post("/api/$rolePath/vastuuhenkilot").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun postVastuuhenkiloErikosalaTehtavaAlreadyAssignedAndReassignedByRemove(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkilo = createPersistedVastuuhenkilo(yliopisto, erikoisala)

        val vastuuhenkilonTehtava = em.findAll(VastuuhenkilonTehtavatyyppi::class)[0]
        vastuuhenkilo.yliopistotAndErikoisalat.first().vastuuhenkilonTehtavat.add(vastuuhenkilonTehtava)

        val yliopistoDTO = yliopistoMapper.toDto(yliopisto)
        val kayttajaYliopistoErikoisalaDTO = KayttajaYliopistoErikoisalaDTO(yliopisto = yliopistoDTO, erikoisala = erikoisalaMapper.toDto(erikoisala))
        kayttajaYliopistoErikoisalaDTO.vastuuhenkilonTehtavat.add(vastuuhenkilonTehtavatyyppiMapper.toDto(vastuuhenkilonTehtava))
        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO().apply {
            yliopisto = yliopistoDTO
            yliopistotAndErikoisalat = setOf(kayttajaYliopistoErikoisalaDTO)
            reassignedTehtavat = setOf(ReassignedVastuuhenkilonTehtavaDTO(kayttajaYliopistoErikoisalaMapper.toDto(vastuuhenkilo.yliopistotAndErikoisalat.first()),
                    vastuuhenkilonTehtava.id, ReassignedVastuuhenkilonTehtavaTyyppi.REMOVE))
        }

        testMockMvc.perform(post("/api/$rolePath/vastuuhenkilot").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isCreated)

        val originalVastuuhenkilonTehtavat = kayttajaRepository.findById(vastuuhenkilo.id!!).get().yliopistotAndErikoisalat.first().vastuuhenkilonTehtavat
        assertThat(originalVastuuhenkilonTehtavat).doesNotContain(vastuuhenkilonTehtava)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun putVastuuhenkiloTehtavaRemovedButNotReassigned(rolePath: String) {
        initTest(getRole(rolePath), true)

        val vastuuhenkilo = createPersistedVastuuhenkilo(yliopisto, erikoisalaRepository.findById(1).get())

        val vastuuhenkilonTehtava = em.findAll(VastuuhenkilonTehtavatyyppi::class)[0]
        vastuuhenkilo.yliopistotAndErikoisalat.first().vastuuhenkilonTehtavat.add(vastuuhenkilonTehtava)

        val kayttajaYliopistoErikoisalaDTO = kayttajaYliopistoErikoisalaMapper.toDto(vastuuhenkilo.yliopistotAndErikoisalat.first())
        kayttajaYliopistoErikoisalaDTO.vastuuhenkilonTehtavat.clear()
        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO().apply { yliopistotAndErikoisalat = setOf(kayttajaYliopistoErikoisalaDTO) }

        testMockMvc.perform(put("/api/$rolePath/vastuuhenkilot/${vastuuhenkilo.id}").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun putVastuuhenkiloTehtavaRemovedAndReassigned(rolePath: String) {
        initTest(getRole(rolePath), true)

        val erikoisala = erikoisalaRepository.findById(1).get()
        val vastuuhenkiloForReassignedTehtava = createPersistedVastuuhenkilo(yliopisto, erikoisala)
        val vastuuhenkilo = KayttajahallintaResourceHelper.createVastuuhenkilo(em, yliopisto, erikoisala)
        vastuuhenkilo.tila = KayttajatilinTila.KUTSUTTU
        em.persist(vastuuhenkilo)

        val vastuuhenkilonTehtava = em.findAll(VastuuhenkilonTehtavatyyppi::class)[0]
        vastuuhenkilo.yliopistotAndErikoisalat.first().vastuuhenkilonTehtavat.add(vastuuhenkilonTehtava)

        val kayttajaYliopistoErikoisalaDTO = kayttajaYliopistoErikoisalaMapper.toDto(vastuuhenkilo.yliopistotAndErikoisalat.first())
        kayttajaYliopistoErikoisalaDTO.vastuuhenkilonTehtavat.clear()
        val updatedSahkoposti = KayttajaHelper.DEFAULT_EMAIL + "x"
        val updatedEppn = KayttajahallintaResourceHelper.DEFAULT_EPPN + "x"
        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO().apply {
            sahkoposti = updatedSahkoposti
            eppn = updatedEppn
            yliopistotAndErikoisalat = setOf(kayttajaYliopistoErikoisalaDTO)
            reassignedTehtavat = setOf(ReassignedVastuuhenkilonTehtavaDTO(
                    kayttajaYliopistoErikoisalaMapper.toDto(vastuuhenkiloForReassignedTehtava.yliopistotAndErikoisalat.first()), vastuuhenkilonTehtava.id,
                    ReassignedVastuuhenkilonTehtavaTyyppi.ADD))
        }

        testMockMvc.perform(put("/api/$rolePath/vastuuhenkilot/${vastuuhenkilo.id}").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isOk)

        val updatedVastuuhenkilo = kayttajaRepository.findById(vastuuhenkilo.id!!).get()
        assertThat(updatedVastuuhenkilo.user?.email).isEqualTo(updatedSahkoposti)
        assertThat(updatedVastuuhenkilo.user?.eppn).isEqualTo(updatedEppn)
        assertThat(updatedVastuuhenkilo.yliopistotAndErikoisalat).size().isEqualTo(1)
        assertThat(updatedVastuuhenkilo.yliopistotAndErikoisalat.first().vastuuhenkilonTehtavat).size().isEqualTo(0)

        val updatedVastuuhenkiloWithReassignedTehtava = kayttajaRepository.findById(vastuuhenkiloForReassignedTehtava.id!!).get()
        assertThat(updatedVastuuhenkiloWithReassignedTehtava.yliopistotAndErikoisalat.first().vastuuhenkilonTehtavat).contains(vastuuhenkilonTehtava)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun putVastuuhenkiloWithNewErikoisala(rolePath: String) {
        initTest(getRole(rolePath), true)

        val vastuuhenkilo = createPersistedVastuuhenkilo(yliopisto, erikoisalaRepository.findById(1).get())
        val erikoisala2 = erikoisalaRepository.findById(2).get()

        val vastuuhenkilonTehtava = em.findAll(VastuuhenkilonTehtavatyyppi::class)[0]
        val yliopistotAndErikoisalatList = vastuuhenkilo.yliopistotAndErikoisalat.toList()
        val existingKayttajaYliopistoErikoisalaDTO = kayttajaYliopistoErikoisalaMapper.toDto(yliopistotAndErikoisalatList[0])
        val newKayttajaYliopistoErikoisalaDTO = KayttajaYliopistoErikoisalaDTO(erikoisala = erikoisalaMapper.toDto(erikoisala2),
            yliopisto = yliopistoMapper.toDto(yliopisto), vastuuhenkilonTehtavat = mutableSetOf(vastuuhenkilonTehtavatyyppiMapper.toDto(vastuuhenkilonTehtava)))

        val khallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO()
            .apply { yliopistotAndErikoisalat = setOf(existingKayttajaYliopistoErikoisalaDTO, newKayttajaYliopistoErikoisalaDTO) }

        testMockMvc.perform(put("/api/$rolePath/vastuuhenkilot/${vastuuhenkilo.id}").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(khallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isOk)

        val updatedVastuuhenkilo = kayttajaRepository.findById(vastuuhenkilo.id!!).get()
        assertThat(updatedVastuuhenkilo.yliopistotAndErikoisalat).size().isEqualTo(2)
        val newYliopistoAndErikoisala = updatedVastuuhenkilo.yliopistotAndErikoisalat.toList()[1]
        assertThat(newYliopistoAndErikoisala.yliopisto).isEqualTo(yliopisto)
        assertThat(newYliopistoAndErikoisala.erikoisala).isEqualTo(erikoisala2)
        assertThat(newYliopistoAndErikoisala.vastuuhenkilonTehtavat).contains(vastuuhenkilonTehtava)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun putVastuuhenkiloWithRemovedErikoisala(rolePath: String) {
        initTest(getRole(rolePath), true)

        val vastuuhenkilo = KayttajahallintaResourceHelper.createVastuuhenkilo(em, yliopisto, erikoisalaRepository.findById(1).get())
        val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(kayttaja = vastuuhenkilo, yliopisto = yliopisto, erikoisala = erikoisalaRepository.findById(2).get())
        em.persist(yliopistoAndErikoisala)
        vastuuhenkilo.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

        val remainingKayttajaYliopistoErikoisalaDTO = kayttajaYliopistoErikoisalaMapper.toDto(vastuuhenkilo.yliopistotAndErikoisalat.first())
        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO().apply { yliopistotAndErikoisalat = setOf(remainingKayttajaYliopistoErikoisalaDTO) }

        testMockMvc.perform(put("/api/$rolePath/vastuuhenkilot/${vastuuhenkilo.id}").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isOk)

        val updatedVastuuhenkilo = kayttajaRepository.findById(vastuuhenkilo.id!!).get()
        assertThat(updatedVastuuhenkilo.yliopistotAndErikoisalat).size().isEqualTo(1)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun putVastuuhenkiloWithRemovedErikoisalaTehtavaNotReassigned(rolePath: String) {
        initTest(getRole(rolePath), true)

        val vastuuhenkilo = KayttajahallintaResourceHelper.createVastuuhenkilo(em, yliopisto, erikoisalaRepository.findById(1).get())
        val vastuuhenkilonTehtava = em.findAll(VastuuhenkilonTehtavatyyppi::class)[0]
        val yliopistoAndErikoisala = KayttajaYliopistoErikoisala(kayttaja = vastuuhenkilo, yliopisto = yliopisto, erikoisala = erikoisalaRepository.findById(2).get())
        em.persist(yliopistoAndErikoisala)
        yliopistoAndErikoisala.vastuuhenkilonTehtavat.add(vastuuhenkilonTehtava)
        vastuuhenkilo.yliopistotAndErikoisalat.add(yliopistoAndErikoisala)

        val remainingKayttajaYliopistoErikoisalaDTO = kayttajaYliopistoErikoisalaMapper.toDto(vastuuhenkilo.yliopistotAndErikoisalat.first())
        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO().apply { yliopistotAndErikoisalat = setOf(remainingKayttajaYliopistoErikoisalaDTO) }

        testMockMvc.perform(put("/api/$rolePath/vastuuhenkilot/${vastuuhenkilo.id}").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun postVirkailijaWithoutYliopisto(rolePath: String) {
        initTest(getRole(rolePath), true)
        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO()
        testMockMvc.perform(post("/api/$rolePath/virkailijat").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().is5xxServerError)
    }

    @Test
    fun postVirkailijaNotAllowedToCreateDifferentYliopistoAssignedToVirkailija() {
        initTest(getRole(OPINTOHALLINNON_VIRKAILIJA))
        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO().apply { yliopisto = YliopistoDTO(id = 1000) }
        testMockMvc.perform(post("/api/$VIRKAILIJA_ROLE_PATH/virkailijat").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun postVirkailijaWithEmailAddressInUse(rolePath: String) {
        initTest(getRole(rolePath), true)

        val virkailija = createPersistedVirkailija(yliopisto)
        virkailija.user?.email = KayttajaHelper.DEFAULT_EMAIL

        val yliopistoDTO = yliopistoMapper.toDto(yliopisto)
        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO().apply {
            yliopisto = yliopistoDTO
            sahkoposti = KayttajaHelper.DEFAULT_EMAIL
        }

        testMockMvc.perform(post("/api/$rolePath/virkailijat").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun postVirkailijaWithEppnInUse(rolePath: String) {
        initTest(getRole(rolePath), true)

        val virkailija = createPersistedVirkailija(yliopisto)
        virkailija.user?.eppn = KayttajahallintaResourceHelper.DEFAULT_EPPN
        virkailija.user?.email = KayttajaHelper.DEFAULT_EMAIL

        val yliopistoDTO = yliopistoMapper.toDto(yliopisto)
        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO().apply {
            yliopisto = yliopistoDTO
            sahkoposti = KayttajaHelper.DEFAULT_EMAIL + "x"
        }

        testMockMvc.perform(post("/api/$rolePath/virkailijat").contentType(APPLICATION_JSON).content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO))
            .with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun postVirkailija(rolePath: String) {
        initTest(getRole(rolePath), true)

        val yliopistoDTO = yliopistoMapper.toDto(yliopisto)
        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO().apply { yliopisto = yliopistoDTO }

        testMockMvc.perform(post("/api/$rolePath/virkailijat").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isCreated)
    }

    @Test
    fun tryToPostPaakayttajaAsVirkailija() {
        initTest(getRole(OPINTOHALLINNON_VIRKAILIJA))
        val kayttajahallintaKayttajaDTO = KayttajahallintaResourceHelper.getDefaultKayttajaDTO()
        testMockMvc.perform(post("/api/$VIRKAILIJA_ROLE_PATH/paakayttajat").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isBadRequest)
    }

    @Test
    fun patchVirkailijaNotAllowedToModifyDifferentYliopistoAssignedToVirkailija() {
        initTest(getRole(OPINTOHALLINNON_VIRKAILIJA), false)

        val virkailija = KayttajahallintaResourceHelper.createVirkailija(em, yliopisto).apply { user?.eppn = KayttajahallintaResourceHelper.DEFAULT_EPPN }
        em.persist(virkailija)

        val kayttajahallintaKayttajaDTO = KayttajahallintaKayttajaDTO(sahkoposti = virkailija.user?.email + "x", eppn = virkailija.user?.eppn + "x")

        testMockMvc.perform(patch("/api/$VIRKAILIJA_ROLE_PATH/virkailijat/${virkailija.id}").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isBadRequest)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun patchVirkailijaOnlyEmailUpdated(rolePath: String) {
        initTest(getRole(rolePath), true)

        val virkailija = KayttajahallintaResourceHelper.createVirkailija(em, yliopisto).apply { user?.eppn = KayttajahallintaResourceHelper.DEFAULT_EPPN }
        em.persist(virkailija)

        val kayttajahallintaKayttajaDTO = KayttajahallintaKayttajaDTO(sahkoposti = virkailija.user?.email + "x", eppn = virkailija.user?.eppn + "x")

        testMockMvc.perform(patch("/api/$rolePath/virkailijat/${virkailija.id}").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isOk)

        val updatedVirkailija = kayttajaRepository.findById(virkailija.id!!).get()
        assertThat(updatedVirkailija.user?.email).isEqualTo(kayttajahallintaKayttajaDTO.sahkoposti)
        assertThat(updatedVirkailija.user?.eppn).isEqualTo(virkailija.user?.eppn)
    }

    @ParameterizedTest
    @ValueSource(strings = [TEKNINEN_PAAKAYTTAJA_ROLE_PATH, VIRKAILIJA_ROLE_PATH])
    fun patchVirkailija(rolePath: String) {
        initTest(getRole(rolePath), true)

        val virkailija = KayttajahallintaResourceHelper.createVirkailija(em, yliopisto).apply { user?.eppn = KayttajahallintaResourceHelper.DEFAULT_EPPN }
        virkailija.tila = KayttajatilinTila.KUTSUTTU
        em.persist(virkailija)

        val kayttajahallintaKayttajaDTO = KayttajahallintaKayttajaDTO(sahkoposti = virkailija.user?.email + "x", eppn = virkailija.user?.eppn + "x")

        testMockMvc.perform(patch("/api/$rolePath/virkailijat/${virkailija.id}").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isOk)

        val updatedVirkailija = kayttajaRepository.findById(virkailija.id!!).get()
        assertThat(updatedVirkailija.user?.email).isEqualTo(kayttajahallintaKayttajaDTO.sahkoposti)
        assertThat(updatedVirkailija.user?.eppn).isEqualTo(kayttajahallintaKayttajaDTO.eppn)
    }

    @Test
    fun tryToPatchPaakayttajaAsVirkailija() {
        initTest(getRole(OPINTOHALLINNON_VIRKAILIJA), true)

        val paakayttaja = KayttajahallintaResourceHelper.createPaakayttaja(em).apply { user?.eppn = KayttajahallintaResourceHelper.DEFAULT_EPPN }
        em.persist(paakayttaja)

        val kayttajahallintaKayttajaDTO = KayttajahallintaKayttajaDTO(sahkoposti = paakayttaja.user?.email + "x", eppn = paakayttaja.user?.eppn + "x")

        testMockMvc.perform(patch("/api/$VIRKAILIJA_ROLE_PATH/paakayttajat/${paakayttaja.id}").contentType(APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kayttajahallintaKayttajaDTO)).with(SecurityMockMvcRequestPostProcessors.csrf())).andExpect(status().isBadRequest)
    }

    fun getRole(rolePath: String): String = if (rolePath == TEKNINEN_PAAKAYTTAJA_ROLE_PATH) { TEKNINEN_PAAKAYTTAJA } else OPINTOHALLINNON_VIRKAILIJA

    fun initTest(role: String, addDefaultYliopistoForRole: Boolean = false) {
        yliopisto = persistYliopisto(DEFAULT_YLIOPISTO)
        val user = KayttajaResourceWithMockUserIT.createEntity(authority = Authority(role))
        persistAndFlush(user)
        val authentication = Saml2Authentication(DefaultSaml2AuthenticatedPrincipal(user.id, mapOf<String, List<Any>>()), "test", listOf(SimpleGrantedAuthority(role)))

        val kayttaja = KayttajaHelper.createEntity(em, user)
        if (role == OPINTOHALLINNON_VIRKAILIJA) {
            if (addDefaultYliopistoForRole) kayttaja.yliopistot.add(yliopisto) else kayttaja.yliopistot.add(persistYliopisto(YliopistoEnum.ITA_SUOMEN_YLIOPISTO))
        }
        em.persist(kayttaja)

        TestSecurityContextHolder.getContext().authentication = authentication
    }
}
