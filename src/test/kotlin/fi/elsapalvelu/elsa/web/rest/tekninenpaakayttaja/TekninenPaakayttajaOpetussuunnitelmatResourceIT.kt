package fi.elsapalvelu.elsa.web.rest.tekninenpaakayttaja

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.TEKNINEN_PAAKAYTTAJA
import fi.elsapalvelu.elsa.service.mapper.ArvioitavaKokonaisuusMapper
import fi.elsapalvelu.elsa.service.mapper.ArvioitavanKokonaisuudenKategoriaWithErikoisalaMapper
import fi.elsapalvelu.elsa.service.mapper.OpintoopasMapper
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
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
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import javax.persistence.EntityManager

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class TekninenPaakayttajaOpetussuunnitelmatResourceIT {

    @Autowired
    private lateinit var erikoisalaRepository: ErikoisalaRepository

    @Autowired
    private lateinit var opintoopasRepository: OpintoopasRepository

    @Autowired
    private lateinit var arvioitavanKokonaisuudenKategoriaRepository: ArvioitavanKokonaisuudenKategoriaRepository

    @Autowired
    private lateinit var arvioitavaKokonaisuusRepository: ArvioitavaKokonaisuusRepository

    @Autowired
    private lateinit var suoritusarviointiRepository: SuoritusarviointiRepository

    @Autowired
    private lateinit var opintoopasMapper: OpintoopasMapper

    @Autowired
    private lateinit var arvioitavanKokonaisuudenKategoriaWithErikoisalaMapper: ArvioitavanKokonaisuudenKategoriaWithErikoisalaMapper

    @Autowired
    private lateinit var arvioitavaKokonaisuusMapper: ArvioitavaKokonaisuusMapper

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restOpetussuunnitelmatMockMvc: MockMvc

    private lateinit var user: User

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @Transactional
    fun getErikoisalat() {
        initTest()

        restOpetussuunnitelmatMockMvc.perform(
            get("/api/tekninen-paakayttaja/erikoisalat")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(Matchers.hasSize<Int>(8)))
    }

    @Test
    @Transactional
    fun getErikoisala() {
        initTest()

        val erikoisala = erikoisalaRepository.saveAndFlush(ErikoisalaHelper.createEntity())

        restOpetussuunnitelmatMockMvc.perform(
            get("/api/tekninen-paakayttaja/erikoisalat/${erikoisala.id}")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(erikoisala.id))
            .andExpect(jsonPath("$.nimi").value(erikoisala.nimi))
            .andExpect(jsonPath("$.tyyppi").value(erikoisala.tyyppi.toString()))
    }

    @Test
    @Transactional
    fun getOpintooppaat() {
        initTest()

        val erikoisala = erikoisalaRepository.saveAndFlush(ErikoisalaHelper.createEntity())
        opintoopasRepository.saveAndFlush(
            OpintoopasHelper.createEntity(
                em,
                erikoisala = erikoisala
            )
        )

        restOpetussuunnitelmatMockMvc.perform(
            get("/api/tekninen-paakayttaja/erikoisalat/${erikoisala.id}/oppaat")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(Matchers.hasSize<Int>(1)))
    }

    @Test
    @Transactional
    fun getUusinOpas() {
        initTest()

        val erikoisala = erikoisalaRepository.saveAndFlush(ErikoisalaHelper.createEntity())
        val opintoopas1 = opintoopasRepository.saveAndFlush(
            OpintoopasHelper.createEntity(
                em,
                erikoisala = erikoisala
            )
        )
        val opintoopas2 = OpintoopasHelper.createEntity(
            em,
            erikoisala = erikoisala
        )
        opintoopas2.voimassaoloAlkaa = opintoopas1.voimassaoloPaattyy?.plusDays(1)
        opintoopas2.voimassaoloPaattyy = opintoopas2.voimassaoloAlkaa?.plusYears(1)
        opintoopasRepository.saveAndFlush(opintoopas2)

        restOpetussuunnitelmatMockMvc.perform(
            get("/api/tekninen-paakayttaja/erikoisalat/${erikoisala.id}/uusinopas")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(opintoopas2.id))
            .andExpect(jsonPath("$.nimi").value(opintoopas2.nimi))
    }

    @Test
    @Transactional
    fun getOpintoopas() {
        initTest()

        val erikoisala = erikoisalaRepository.saveAndFlush(ErikoisalaHelper.createEntity())
        val opintoopas =
            OpintoopasHelper.createEntity(
                em,
                erikoisala = erikoisala
            )
        opintoopas.kaytannonKoulutuksenVahimmaispituus = 730.0
        opintoopas.terveyskeskuskoulutusjaksonVahimmaispituus = 547.5
        opintoopas.terveyskeskuskoulutusjaksonMaksimipituus = 730.0
        opintoopas.yliopistosairaalajaksonVahimmaispituus = 273.75
        opintoopas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus = 0.0
        opintoopas.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara = 0.0
        opintoopas.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara = 10.0
        opintoopas.erikoisalanVaatimaJohtamisopintojenVahimmaismaara = 25.0

        opintoopasRepository.saveAndFlush(opintoopas)

        restOpetussuunnitelmatMockMvc.perform(
            get("/api/tekninen-paakayttaja/opintoopas/${opintoopas.id}")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(opintoopas.id))
            .andExpect(jsonPath("$.nimi").value(opintoopas.nimi))
            .andExpect(jsonPath("$.voimassaoloAlkaa").value(opintoopas.voimassaoloAlkaa.toString()))
            .andExpect(jsonPath("$.voimassaoloPaattyy").value(opintoopas.voimassaoloPaattyy.toString()))
            .andExpect(jsonPath("$.kaytannonKoulutuksenVahimmaispituusVuodet").value(2))
            .andExpect(jsonPath("$.kaytannonKoulutuksenVahimmaispituusKuukaudet").value(0))
            .andExpect(jsonPath("$.terveyskeskuskoulutusjaksonVahimmaispituusVuodet").value(1))
            .andExpect(jsonPath("$.terveyskeskuskoulutusjaksonVahimmaispituusKuukaudet").value(6))
            .andExpect(jsonPath("$.terveyskeskuskoulutusjaksonMaksimipituusVuodet").value(2))
            .andExpect(jsonPath("$.terveyskeskuskoulutusjaksonMaksimipituusKuukaudet").value(0))
            .andExpect(jsonPath("$.yliopistosairaalajaksonVahimmaispituusVuodet").value(0))
            .andExpect(jsonPath("$.yliopistosairaalajaksonVahimmaispituusKuukaudet").value(9))
            .andExpect(
                jsonPath("$.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet").value(
                    0
                )
            )
            .andExpect(
                jsonPath("$.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusKuukaudet").value(
                    0
                )
            )
            .andExpect(jsonPath("$.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara").value(0))
            .andExpect(
                jsonPath("$.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara").value(
                    10.0
                )
            )
            .andExpect(jsonPath("$.erikoisalanVaatimaJohtamisopintojenVahimmaismaara").value(25.0))
            .andExpect(jsonPath("$.arviointiasteikkoId").value(opintoopas.arviointiasteikko?.id))
            .andExpect(jsonPath("$.arviointiasteikkoNimi").value(opintoopas.arviointiasteikko?.nimi.toString()))
            .andExpect(jsonPath("$.erikoisala.id").value(erikoisala.id))
    }

    @Test
    @Transactional
    fun createOpintoopas() {
        initTest()

        val erikoisala = erikoisalaRepository.saveAndFlush(ErikoisalaHelper.createEntity())
        val opintoopas =
            OpintoopasHelper.createEntity(
                em,
                erikoisala = erikoisala
            )
        opintoopas.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara = 0.0
        opintoopas.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara = 10.0
        opintoopas.erikoisalanVaatimaJohtamisopintojenVahimmaismaara = 25.0

        val opintoopasDTO = opintoopasMapper.toDto(opintoopas)

        opintoopasDTO.kaytannonKoulutuksenVahimmaispituusVuodet = 2
        opintoopasDTO.kaytannonKoulutuksenVahimmaispituusKuukaudet = 0
        opintoopasDTO.terveyskeskuskoulutusjaksonVahimmaispituusVuodet = 1
        opintoopasDTO.terveyskeskuskoulutusjaksonVahimmaispituusKuukaudet = 6
        opintoopasDTO.terveyskeskuskoulutusjaksonMaksimipituusVuodet = 2
        opintoopasDTO.terveyskeskuskoulutusjaksonMaksimipituusKuukaudet = 0
        opintoopasDTO.yliopistosairaalajaksonVahimmaispituusVuodet = 0
        opintoopasDTO.yliopistosairaalajaksonVahimmaispituusKuukaudet = 9
        opintoopasDTO.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet = 0
        opintoopasDTO.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusKuukaudet = 0

        val databaseSizeBeforeCreate = opintoopasRepository.findAll().size

        restOpetussuunnitelmatMockMvc.perform(
            post("/api/tekninen-paakayttaja/opintoopas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(opintoopasDTO))
                .with(csrf())
        ).andExpect(status().isCreated)

        val opintoopasList = opintoopasRepository.findAll()
        assertThat(opintoopasList).hasSize(databaseSizeBeforeCreate + 1)
        val opas = opintoopasList[opintoopasList.size - 1]
        assertThat(opas.nimi).isEqualTo(opintoopasDTO.nimi)
        assertThat(opas.nimiSv).isEqualTo(opintoopasDTO.nimiSv)
        assertThat(opas.voimassaoloAlkaa).isEqualTo(opintoopasDTO.voimassaoloAlkaa)
        assertThat(opas.voimassaoloPaattyy).isEqualTo(opintoopasDTO.voimassaoloPaattyy)
        assertThat(opas.kaytannonKoulutuksenVahimmaispituus).isEqualTo(730.0)
        assertThat(opas.terveyskeskuskoulutusjaksonVahimmaispituus).isEqualTo(547.5)
        assertThat(opas.terveyskeskuskoulutusjaksonMaksimipituus).isEqualTo(730.0)
        assertThat(opas.yliopistosairaalajaksonVahimmaispituus).isEqualTo(273.75)
        assertThat(opas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus).isEqualTo(0.0)
        assertThat(opas.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara).isEqualTo(0.0)
        assertThat(opas.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara).isEqualTo(10.0)
        assertThat(opas.erikoisalanVaatimaJohtamisopintojenVahimmaismaara).isEqualTo(25.0)
        assertThat(opas.erikoisala?.id).isEqualTo(erikoisala.id)
        assertThat(opas.arviointiasteikko).isNotNull
    }

    @Test
    @Transactional
    fun updateOpintoopas() {
        initTest()

        val erikoisala = erikoisalaRepository.saveAndFlush(ErikoisalaHelper.createEntity())
        val opintoopas =
            OpintoopasHelper.createEntity(
                em,
                erikoisala = erikoisala
            )
        opintoopas.kaytannonKoulutuksenVahimmaispituus = 730.0
        opintoopas.terveyskeskuskoulutusjaksonVahimmaispituus = 547.5
        opintoopas.terveyskeskuskoulutusjaksonMaksimipituus = 730.0
        opintoopas.yliopistosairaalajaksonVahimmaispituus = 273.75
        opintoopas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus = 0.0
        opintoopas.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara = 0.0
        opintoopas.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara = 10.0
        opintoopas.erikoisalanVaatimaJohtamisopintojenVahimmaismaara = 25.0

        opintoopasRepository.saveAndFlush(opintoopas)

        val opintoopasDTO = opintoopasMapper.toDto(opintoopas)

        opintoopasDTO.nimi = "updated"
        opintoopasDTO.nimiSv = "updated sv"
        opintoopasDTO.voimassaoloAlkaa = opintoopasDTO.voimassaoloAlkaa?.plusMonths(1)
        opintoopasDTO.voimassaoloPaattyy = null
        opintoopasDTO.kaytannonKoulutuksenVahimmaispituusVuodet = 0
        opintoopasDTO.kaytannonKoulutuksenVahimmaispituusKuukaudet = 0
        opintoopasDTO.terveyskeskuskoulutusjaksonVahimmaispituusVuodet = 0
        opintoopasDTO.terveyskeskuskoulutusjaksonVahimmaispituusKuukaudet = 0
        opintoopasDTO.terveyskeskuskoulutusjaksonMaksimipituusVuodet = 0
        opintoopasDTO.terveyskeskuskoulutusjaksonMaksimipituusKuukaudet = 0
        opintoopasDTO.yliopistosairaalajaksonVahimmaispituusVuodet = 0
        opintoopasDTO.yliopistosairaalajaksonVahimmaispituusKuukaudet = 0
        opintoopasDTO.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusVuodet = 0
        opintoopasDTO.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituusKuukaudet = 0
        opintoopasDTO.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara = 0.0
        opintoopasDTO.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara = 0.0
        opintoopasDTO.erikoisalanVaatimaJohtamisopintojenVahimmaismaara = 0.0
        opintoopasDTO.arviointiasteikkoId = 2

        restOpetussuunnitelmatMockMvc.perform(
            put("/api/tekninen-paakayttaja/opintoopas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(opintoopasDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val opintoopasList = opintoopasRepository.findAll()
        val opas = opintoopasList[opintoopasList.size - 1]
        assertThat(opas.nimi).isEqualTo(opintoopasDTO.nimi)
        assertThat(opas.nimiSv).isEqualTo(opintoopasDTO.nimiSv)
        assertThat(opas.voimassaoloAlkaa).isEqualTo(opintoopasDTO.voimassaoloAlkaa)
        assertThat(opas.voimassaoloPaattyy).isEqualTo(opintoopasDTO.voimassaoloPaattyy)
        assertThat(opas.kaytannonKoulutuksenVahimmaispituus).isEqualTo(0.0)
        assertThat(opas.terveyskeskuskoulutusjaksonVahimmaispituus).isEqualTo(0.0)
        assertThat(opas.terveyskeskuskoulutusjaksonMaksimipituus).isEqualTo(0.0)
        assertThat(opas.yliopistosairaalajaksonVahimmaispituus).isEqualTo(0.0)
        assertThat(opas.yliopistosairaalanUlkopuolisenTyoskentelynVahimmaispituus).isEqualTo(0.0)
        assertThat(opas.erikoisalanVaatimaTeoriakoulutustenVahimmaismaara).isEqualTo(0.0)
        assertThat(opas.erikoisalanVaatimaSateilysuojakoulutustenVahimmaismaara).isEqualTo(0.0)
        assertThat(opas.erikoisalanVaatimaJohtamisopintojenVahimmaismaara).isEqualTo(0.0)
        assertThat(opas.erikoisala?.id).isEqualTo(erikoisala.id)
        assertThat(opas.arviointiasteikko?.id).isEqualTo(2)
    }

    @Test
    @Transactional
    fun createNewLatestOpintoopas() {
        initTest()

        val erikoisala = erikoisalaRepository.saveAndFlush(ErikoisalaHelper.createEntity())
        val opintoopas =
            OpintoopasHelper.createEntity(
                em,
                erikoisala = erikoisala
            )
        opintoopas.voimassaoloPaattyy = null
        opintoopasRepository.saveAndFlush(opintoopas)

        val opintoopas2 =
            OpintoopasHelper.createEntity(
                em,
                erikoisala = erikoisala
            )
        opintoopas2.voimassaoloAlkaa = opintoopas.voimassaoloAlkaa?.plusMonths(6)
        opintoopas2.voimassaoloPaattyy = null

        val opintoopasDTO = opintoopasMapper.toDto(opintoopas2)

        restOpetussuunnitelmatMockMvc.perform(
            post("/api/tekninen-paakayttaja/opintoopas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(opintoopasDTO))
                .with(csrf())
        ).andExpect(status().isCreated)

        val opintoopasList = opintoopasRepository.findAll()
        val opas = opintoopasList[opintoopasList.size - 1]
        assertThat(opas.nimi).isEqualTo(opintoopasDTO.nimi)
        assertThat(opas.voimassaoloAlkaa).isEqualTo(opintoopasDTO.voimassaoloAlkaa)
        assertThat(opas.voimassaoloPaattyy).isEqualTo(opintoopasDTO.voimassaoloPaattyy)

        val oldOpas = opintoopasList[opintoopasList.size - 2]
        assertThat(oldOpas.nimi).isEqualTo(opintoopas.nimi)
        assertThat(oldOpas.voimassaoloAlkaa).isEqualTo(opintoopas.voimassaoloAlkaa)
        assertThat(oldOpas.voimassaoloPaattyy).isEqualTo(opintoopasDTO.voimassaoloAlkaa?.minusDays(1))
    }

    @Test
    @Transactional
    fun createOverlappingOpintoopas() {
        initTest()

        val erikoisala = erikoisalaRepository.saveAndFlush(ErikoisalaHelper.createEntity())
        val opintoopas =
            OpintoopasHelper.createEntity(
                em,
                erikoisala = erikoisala
            )
        opintoopasRepository.saveAndFlush(opintoopas)

        val opintoopas2 =
            OpintoopasHelper.createEntity(
                em,
                erikoisala = erikoisala
            )

        val opintoopasDTO = opintoopasMapper.toDto(opintoopas2)

        restOpetussuunnitelmatMockMvc.perform(
            post("/api/tekninen-paakayttaja/opintoopas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(opintoopasDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun createOpintoopasWithoutErikoisala() {
        initTest()

        val erikoisala = erikoisalaRepository.saveAndFlush(ErikoisalaHelper.createEntity())
        val opintoopas =
            OpintoopasHelper.createEntity(
                em,
                erikoisala = erikoisala
            )

        val opintoopasDTO = opintoopasMapper.toDto(opintoopas)
        opintoopasDTO.erikoisala = null

        restOpetussuunnitelmatMockMvc.perform(
            post("/api/tekninen-paakayttaja/opintoopas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(opintoopasDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun createOpintoopasWithIncorrectVoimassaolo() {
        initTest()

        val erikoisala = erikoisalaRepository.saveAndFlush(ErikoisalaHelper.createEntity())
        val opintoopas =
            OpintoopasHelper.createEntity(
                em,
                erikoisala = erikoisala
            )
        opintoopas.voimassaoloAlkaa = opintoopas.voimassaoloAlkaa?.plusYears(1)

        val opintoopasDTO = opintoopasMapper.toDto(opintoopas)

        restOpetussuunnitelmatMockMvc.perform(
            post("/api/tekninen-paakayttaja/opintoopas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(opintoopasDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun createOpintoopasWithIncorrectVoimassaoloPaattyy() {
        initTest()

        val erikoisala = erikoisalaRepository.saveAndFlush(ErikoisalaHelper.createEntity())
        val opintoopas =
            OpintoopasHelper.createEntity(
                em,
                erikoisala = erikoisala
            )
        opintoopasRepository.saveAndFlush(opintoopas)

        val opintoopas2 =
            OpintoopasHelper.createEntity(
                em,
                erikoisala = erikoisala
            )
        opintoopas2.voimassaoloAlkaa = opintoopas2.voimassaoloAlkaa?.minusYears(1)
        opintoopas2.voimassaoloPaattyy = null

        val opintoopasDTO = opintoopasMapper.toDto(opintoopas2)

        restOpetussuunnitelmatMockMvc.perform(
            post("/api/tekninen-paakayttaja/opintoopas")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(opintoopasDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun getArvioitavatKokonaisuudenKategoriat() {
        initTest()

        val erikoisala = erikoisalaRepository.saveAndFlush(ErikoisalaHelper.createEntity())
        arvioitavanKokonaisuudenKategoriaRepository.saveAndFlush(
            ArvioitavanKokonaisuudenKategoriaHelper.createEntity(em, erikoisala)
        )

        restOpetussuunnitelmatMockMvc.perform(
            get("/api/tekninen-paakayttaja/erikoisalat/${erikoisala.id}/arvioitavankokonaisuudenkategoriat")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(Matchers.hasSize<Int>(1)))
    }

    @Test
    @Transactional
    fun getArvioitavatKokonaisuudet() {
        initTest()

        val erikoisala = erikoisalaRepository.saveAndFlush(ErikoisalaHelper.createEntity())
        val kategoria = ArvioitavanKokonaisuudenKategoriaHelper.createEntity(em, erikoisala)
        arvioitavanKokonaisuudenKategoriaRepository.saveAndFlush(kategoria)

        arvioitavaKokonaisuusRepository.saveAndFlush(
            ArvioitavaKokonaisuusHelper.createEntity(
                em,
                existingKategoria = kategoria
            )
        )

        arvioitavaKokonaisuusRepository.saveAndFlush(
            ArvioitavaKokonaisuusHelper.createEntity(
                em,
                existingKategoria = kategoria
            )
        )

        flushAndClear()

        restOpetussuunnitelmatMockMvc.perform(
            get("/api/tekninen-paakayttaja/erikoisalat/${erikoisala.id}/arvioitavatkokonaisuudet")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$").value(Matchers.hasSize<Int>(1)))
            .andExpect(jsonPath("$[0].id").value(kategoria.id))
            .andExpect(jsonPath("$[0].nimi").value(kategoria.nimi))
            .andExpect(jsonPath("$[0].arvioitavatKokonaisuudet").value(Matchers.hasSize<Int>(2)))
    }

    @Test
    @Transactional
    fun getArvioitavanKokonaisuudenKategoria() {
        initTest()

        val erikoisala = erikoisalaRepository.saveAndFlush(ErikoisalaHelper.createEntity())
        val kategoria = ArvioitavanKokonaisuudenKategoriaHelper.createEntity(em, erikoisala)
        arvioitavanKokonaisuudenKategoriaRepository.saveAndFlush(kategoria)

        restOpetussuunnitelmatMockMvc.perform(
            get("/api/tekninen-paakayttaja/arvioitavankokonaisuudenkategoria/${kategoria.id}")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(kategoria.id))
            .andExpect(jsonPath("$.nimi").value(kategoria.nimi))
            .andExpect(jsonPath("$.nimiSv").value(kategoria.nimiSv))
            .andExpect(jsonPath("$.jarjestysnumero").value(kategoria.jarjestysnumero))
            .andExpect(jsonPath("$.erikoisala.id").value(kategoria.erikoisala?.id))
    }

    @Test
    @Transactional
    fun createArvioitavanKokonaisuudenKategoria() {
        initTest()

        val erikoisala = erikoisalaRepository.saveAndFlush(ErikoisalaHelper.createEntity())
        val kategoria = ArvioitavanKokonaisuudenKategoriaHelper.createEntity(em, erikoisala)
        val kategoriaDTO = arvioitavanKokonaisuudenKategoriaWithErikoisalaMapper.toDto(kategoria)

        val databaseSizeBeforeCreate = arvioitavanKokonaisuudenKategoriaRepository.findAll().size

        restOpetussuunnitelmatMockMvc.perform(
            post("/api/tekninen-paakayttaja/arvioitavankokonaisuudenkategoria")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kategoriaDTO))
                .with(csrf())
        ).andExpect(status().isCreated)

        val kategoriaList = arvioitavanKokonaisuudenKategoriaRepository.findAll()
        assertThat(kategoriaList).hasSize(databaseSizeBeforeCreate + 1)
        val result = kategoriaList[kategoriaList.size - 1]
        assertThat(result.nimi).isEqualTo(kategoriaDTO.nimi)
        assertThat(result.nimiSv).isEqualTo(kategoriaDTO.nimiSv)
        assertThat(result.jarjestysnumero).isEqualTo(kategoriaDTO.jarjestysnumero)
    }

    @Test
    @Transactional
    fun updateArvioitavanKokonaisuudenKategoria() {
        initTest()

        val erikoisala = erikoisalaRepository.saveAndFlush(ErikoisalaHelper.createEntity())
        val kategoria = ArvioitavanKokonaisuudenKategoriaHelper.createEntity(em, erikoisala)
        arvioitavanKokonaisuudenKategoriaRepository.saveAndFlush(kategoria)

        val kategoriaDTO = arvioitavanKokonaisuudenKategoriaWithErikoisalaMapper.toDto(kategoria)
        kategoriaDTO.nimi = "updated"
        kategoriaDTO.nimiSv = "updated sv"
        kategoriaDTO.jarjestysnumero = 15

        restOpetussuunnitelmatMockMvc.perform(
            put("/api/tekninen-paakayttaja/arvioitavankokonaisuudenkategoria")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kategoriaDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val kategoriaList = arvioitavanKokonaisuudenKategoriaRepository.findAll()
        val result = kategoriaList[kategoriaList.size - 1]
        assertThat(result.nimi).isEqualTo(kategoriaDTO.nimi)
        assertThat(result.nimiSv).isEqualTo(kategoriaDTO.nimiSv)
        assertThat(result.jarjestysnumero).isEqualTo(kategoriaDTO.jarjestysnumero)
    }

    @Test
    @Transactional
    fun getArvioitavaKokonaisuus() {
        initTest()

        val erikoisala = erikoisalaRepository.saveAndFlush(ErikoisalaHelper.createEntity())
        val kategoria = ArvioitavanKokonaisuudenKategoriaHelper.createEntity(em, erikoisala)
        arvioitavanKokonaisuudenKategoriaRepository.saveAndFlush(kategoria)

        val kokonaisuus = arvioitavaKokonaisuusRepository.saveAndFlush(
            ArvioitavaKokonaisuusHelper.createEntity(
                em,
                existingKategoria = kategoria
            )
        )

        restOpetussuunnitelmatMockMvc.perform(
            get("/api/tekninen-paakayttaja/arvioitavakokonaisuus/${kokonaisuus.id}")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(kokonaisuus.id))
            .andExpect(jsonPath("$.nimi").value(kokonaisuus.nimi))
            .andExpect(jsonPath("$.nimiSv").value(kokonaisuus.nimiSv))
            .andExpect(jsonPath("$.kuvaus").value(kokonaisuus.kuvaus))
            .andExpect(jsonPath("$.kuvausSv").value(kokonaisuus.kuvausSv))
            .andExpect(jsonPath("$.voimassaoloAlkaa").value(kokonaisuus.voimassaoloAlkaa.toString()))
            .andExpect(jsonPath("$.voimassaoloLoppuu").value(kokonaisuus.voimassaoloLoppuu.toString()))
            .andExpect(jsonPath("$.kategoria.id").value(kokonaisuus.kategoria?.id))
    }

    @Test
    @Transactional
    fun createArvioitavaKokonaisuus() {
        initTest()

        val erikoisala = erikoisalaRepository.saveAndFlush(ErikoisalaHelper.createEntity())
        val kategoria = arvioitavanKokonaisuudenKategoriaRepository.saveAndFlush(
            ArvioitavanKokonaisuudenKategoriaHelper.createEntity(em, erikoisala)
        )

        val kokonaisuus =
            ArvioitavaKokonaisuusHelper.createEntity(em, existingKategoria = kategoria)
        val kokonaisuusDTO = arvioitavaKokonaisuusMapper.toDto(kokonaisuus)

        val databaseSizeBeforeCreate = arvioitavaKokonaisuusRepository.findAll().size

        restOpetussuunnitelmatMockMvc.perform(
            post("/api/tekninen-paakayttaja/arvioitavakokonaisuus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kokonaisuusDTO))
                .with(csrf())
        ).andExpect(status().isCreated)

        val kokonaisuusList = arvioitavaKokonaisuusRepository.findAll()
        assertThat(kokonaisuusList).hasSize(databaseSizeBeforeCreate + 1)
        val result = kokonaisuusList[kokonaisuusList.size - 1]
        assertThat(result.nimi).isEqualTo(kokonaisuusDTO.nimi)
        assertThat(result.nimiSv).isEqualTo(kokonaisuusDTO.nimiSv)
        assertThat(result.kuvaus).isEqualTo(kokonaisuusDTO.kuvaus)
        assertThat(result.kuvausSv).isEqualTo(kokonaisuusDTO.kuvausSv)
        assertThat(result.voimassaoloAlkaa).isEqualTo(kokonaisuusDTO.voimassaoloAlkaa)
        assertThat(result.voimassaoloLoppuu).isEqualTo(kokonaisuusDTO.voimassaoloLoppuu)
        assertThat(result.kategoria?.id).isEqualTo(kokonaisuusDTO.kategoria?.id)
    }

    @Test
    @Transactional
    fun updateArvioitavaKokonaisuus() {
        initTest()

        val erikoisala = erikoisalaRepository.saveAndFlush(ErikoisalaHelper.createEntity())
        val kategoria = arvioitavanKokonaisuudenKategoriaRepository.saveAndFlush(
            ArvioitavanKokonaisuudenKategoriaHelper.createEntity(em, erikoisala)
        )

        val kokonaisuus = arvioitavaKokonaisuusRepository.saveAndFlush(
            ArvioitavaKokonaisuusHelper.createEntity(em, existingKategoria = kategoria)
        )

        val kokonaisuusDTO = arvioitavaKokonaisuusMapper.toDto(kokonaisuus)
        kokonaisuusDTO.nimi = "updated"
        kokonaisuusDTO.nimiSv = "updated sv"
        kokonaisuusDTO.kuvaus = "kuvaus"
        kokonaisuusDTO.kuvausSv = "kuvaus sv"
        kokonaisuusDTO.voimassaoloAlkaa = kokonaisuusDTO.voimassaoloAlkaa?.plusDays(1)
        kokonaisuusDTO.voimassaoloLoppuu = kokonaisuusDTO.voimassaoloLoppuu?.plusDays(1)

        restOpetussuunnitelmatMockMvc.perform(
            put("/api/tekninen-paakayttaja/arvioitavakokonaisuus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kokonaisuusDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val kokonaisuusList = arvioitavaKokonaisuusRepository.findAll()
        val result = kokonaisuusList[kokonaisuusList.size - 1]
        assertThat(result.nimi).isEqualTo(kokonaisuusDTO.nimi)
        assertThat(result.nimiSv).isEqualTo(kokonaisuusDTO.nimiSv)
        assertThat(result.kuvaus).isEqualTo(kokonaisuusDTO.kuvaus)
        assertThat(result.kuvausSv).isEqualTo(kokonaisuusDTO.kuvausSv)
        assertThat(result.voimassaoloAlkaa).isEqualTo(kokonaisuusDTO.voimassaoloAlkaa.toString())
        assertThat(result.voimassaoloLoppuu).isEqualTo(kokonaisuusDTO.voimassaoloLoppuu.toString())
        assertThat(result.kategoria?.id).isEqualTo(kokonaisuusDTO.kategoria?.id)
    }

    @Test
    @Transactional
    fun createArvioitavaKokonaisuusWithIncorrectVoimassaolo() {
        initTest()

        val erikoisala = erikoisalaRepository.saveAndFlush(ErikoisalaHelper.createEntity())
        val kategoria = arvioitavanKokonaisuudenKategoriaRepository.saveAndFlush(
            ArvioitavanKokonaisuudenKategoriaHelper.createEntity(em, erikoisala)
        )

        val kokonaisuus =
            ArvioitavaKokonaisuusHelper.createEntity(em, existingKategoria = kategoria)
        val kokonaisuusDTO = arvioitavaKokonaisuusMapper.toDto(kokonaisuus)
        kokonaisuusDTO.voimassaoloAlkaa = kokonaisuusDTO.voimassaoloAlkaa?.plusYears(1)

        restOpetussuunnitelmatMockMvc.perform(
            post("/api/tekninen-paakayttaja/arvioitavakokonaisuus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kokonaisuusDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)
    }

    @Test
    @Transactional
    fun updateArvioitavaKokonaisuusWithSuoritusarviointi() {
        initTest()

        val erikoisala = erikoisalaRepository.saveAndFlush(ErikoisalaHelper.createEntity())
        val kategoria = arvioitavanKokonaisuudenKategoriaRepository.saveAndFlush(
            ArvioitavanKokonaisuudenKategoriaHelper.createEntity(em, erikoisala)
        )

        val kokonaisuus = arvioitavaKokonaisuusRepository.saveAndFlush(
            ArvioitavaKokonaisuusHelper.createEntity(em, existingKategoria = kategoria)
        )

        suoritusarviointiRepository.saveAndFlush(
            SuoritusarviointiHelper.createEntity(
                em,
                arvioitavaKokonaisuus = kokonaisuus
            )
        )

        val kokonaisuusDTO = arvioitavaKokonaisuusMapper.toDto(kokonaisuus)
        kokonaisuusDTO.voimassaoloAlkaa = kokonaisuusDTO.voimassaoloAlkaa?.plusDays(1)

        restOpetussuunnitelmatMockMvc.perform(
            put("/api/tekninen-paakayttaja/arvioitavakokonaisuus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kokonaisuusDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)
    }

    fun initTest(userId: String? = null) {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>(
        )
        val authorities = listOf(SimpleGrantedAuthority(TEKNINEN_PAAKAYTTAJA))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(userId ?: user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication
    }

    private fun flushAndClear() {
        em.flush()
        em.clear()
    }
}
