package fi.elsapalvelu.elsa.web.rest.tekninenpaakayttaja

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.repository.ErikoisalaRepository
import fi.elsapalvelu.elsa.repository.OpintoopasRepository
import fi.elsapalvelu.elsa.security.TEKNINEN_PAAKAYTTAJA
import fi.elsapalvelu.elsa.service.mapper.OpintoopasMapper
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoisalaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.OpintoopasHelper
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
    private lateinit var opintoopasMapper: OpintoopasMapper

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
}
