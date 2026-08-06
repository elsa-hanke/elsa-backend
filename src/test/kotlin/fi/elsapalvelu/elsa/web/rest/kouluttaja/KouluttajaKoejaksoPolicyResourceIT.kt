package fi.elsapalvelu.elsa.web.rest.kouluttaja

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonAloituskeskustelu
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonKehittamistoimenpiteet
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonKoulutussopimus
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonLoppukeskustelu
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonValiarviointi
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonAloituskeskusteluRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonKehittamistoimenpiteetRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonKoulutussopimusRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonLoppukeskusteluRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonValiarviointiRepository
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonAloituskeskusteluMapper
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonKehittamistoimenpiteetMapper
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonLoppukeskusteluMapper
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonValiarviointiMapper
import fi.elsapalvelu.elsa.web.rest.ResourceIntegrationTestBase
import fi.elsapalvelu.elsa.web.rest.common.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KoejaksonVaiheetHelper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.annotation.Transactional
import kotlin.test.assertNotNull

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class KouluttajaKoejaksoPolicyResourceIT : ResourceIntegrationTestBase() {

    @Autowired private lateinit var koejaksonKoulutussopimusRepository: KoejaksonKoulutussopimusRepository
    @Autowired private lateinit var koejaksonAloituskeskusteluRepository: KoejaksonAloituskeskusteluRepository
    @Autowired private lateinit var koejaksonValiarviointiRepository: KoejaksonValiarviointiRepository
    @Autowired private lateinit var koejaksonKehittamistoimenpiteetRepository: KoejaksonKehittamistoimenpiteetRepository
    @Autowired private lateinit var koejaksonLoppukeskusteluRepository: KoejaksonLoppukeskusteluRepository
    @Autowired private lateinit var koejaksonAloituskeskusteluMapper: KoejaksonAloituskeskusteluMapper
    @Autowired private lateinit var koejaksonValiarviointiMapper: KoejaksonValiarviointiMapper
    @Autowired private lateinit var koejaksonKehittamistoimenpiteetMapper: KoejaksonKehittamistoimenpiteetMapper
    @Autowired private lateinit var koejaksonLoppukeskusteluMapper: KoejaksonLoppukeskusteluMapper

    private lateinit var koejaksonKoulutussopimus: KoejaksonKoulutussopimus
    private lateinit var koejaksonAloituskeskustelu: KoejaksonAloituskeskustelu
    private lateinit var koejaksonValiarviointi: KoejaksonValiarviointi
    private lateinit var koejaksonKehittamistoimenpiteet: KoejaksonKehittamistoimenpiteet
    private lateinit var koejaksonLoppukeskustelu: KoejaksonLoppukeskustelu
    private lateinit var user: User

    @Test
    fun getAloituskeskusteluAsEsimies() {
        initTest()
        assertNotNull(koejaksonAloituskeskustelu.id)

        testMockMvc.perform(get("/api/kouluttaja/koejakso/aloituskeskustelu/{id}", koejaksonAloituskeskustelu.id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(koejaksonAloituskeskustelu.id))
            .andExpect(jsonPath("$.lahikouluttaja.id").value(koejaksonAloituskeskustelu.lahikouluttaja?.id))
            .andExpect(jsonPath("$.lahiesimies.id").value(koejaksonAloituskeskustelu.lahiesimies?.id))
    }

    @Test
    fun getValiarviointiAsEsimies() {
        initTest()
        assertNotNull(koejaksonValiarviointi.id)

        testMockMvc.perform(get("/api/kouluttaja/koejakso/valiarviointi/{id}", koejaksonValiarviointi.id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(koejaksonValiarviointi.id))
            .andExpect(jsonPath("$.lahikouluttaja.id").value(koejaksonValiarviointi.lahikouluttaja?.id))
            .andExpect(jsonPath("$.lahiesimies.id").value(koejaksonValiarviointi.lahiesimies?.id))
    }

    @Test
    fun getKehittamistoimenpiteetAsEsimies() {
        initTest()
        assertNotNull(koejaksonKehittamistoimenpiteet.id)

        testMockMvc.perform(get("/api/kouluttaja/koejakso/kehittamistoimenpiteet/{id}", koejaksonKehittamistoimenpiteet.id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(koejaksonKehittamistoimenpiteet.id))
            .andExpect(jsonPath("$.lahikouluttaja.id").value(koejaksonKehittamistoimenpiteet.lahikouluttaja?.id))
            .andExpect(jsonPath("$.lahiesimies.id").value(koejaksonKehittamistoimenpiteet.lahiesimies?.id))
    }

    @Test
    fun getLoppukeskusteluAsEsimies() {
        initTest()
        assertNotNull(koejaksonLoppukeskustelu.id)

        testMockMvc.perform(get("/api/kouluttaja/koejakso/loppukeskustelu/{id}", koejaksonLoppukeskustelu.id))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(koejaksonLoppukeskustelu.id))
            .andExpect(jsonPath("$.lahikouluttaja.id").value(koejaksonLoppukeskustelu.lahikouluttaja?.id))
            .andExpect(jsonPath("$.lahiesimies.id").value(koejaksonLoppukeskustelu.lahiesimies?.id))
    }

    @Test
    fun updateAloituskeskusteluAsEsimiesWithoutKouluttajaSopimusHyvaksytty() {
        initTestWithKouluttajaSopimusNotHyvaksytty()
        val updatedAloituskeskustelu = koejaksonAloituskeskusteluRepository.findById(koejaksonAloituskeskustelu.id!!).get()
        em.detach(updatedAloituskeskustelu)
        updatedAloituskeskustelu.lahiesimiesHyvaksynyt = true
        updatedAloituskeskustelu.lahiesimiehenKuittausaika = KoejaksonVaiheetHelper.DEFAULT_MYONTAMISPAIVA
        val aloituskeskusteluDTO = koejaksonAloituskeskusteluMapper.toDto(updatedAloituskeskustelu)

        testMockMvc.perform(put("/api/kouluttaja/koejakso/aloituskeskustelu").contentType(MediaType.APPLICATION_JSON)
            .content(convertObjectToJsonBytes(aloituskeskusteluDTO)).with(csrf()))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun updateValiarviointiAsEsimiesWithoutKouluttajaSopimusHyvaksytty() {
        initTestWithKouluttajaSopimusNotHyvaksytty()
        val updatedValiarviointi = koejaksonValiarviointiRepository.findById(koejaksonValiarviointi.id!!).get()
        em.detach(updatedValiarviointi)
        updatedValiarviointi.lahiesimiesHyvaksynyt = true
        updatedValiarviointi.lahiesimiehenKuittausaika = KoejaksonVaiheetHelper.DEFAULT_MYONTAMISPAIVA
        val valiarviointiDTO = koejaksonValiarviointiMapper.toDto(updatedValiarviointi)

        testMockMvc.perform(put("/api/kouluttaja/koejakso/valiarviointi").contentType(MediaType.APPLICATION_JSON)
            .content(convertObjectToJsonBytes(valiarviointiDTO)).with(csrf()))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun updateKehittamistoimenpiteetAsEsimiesWithoutKouluttajaSopimusHyvaksytty() {
        initTestWithKouluttajaSopimusNotHyvaksytty()
        val updatedKehittamistoimenpiteet = koejaksonKehittamistoimenpiteetRepository.findById(koejaksonKehittamistoimenpiteet.id!!).get()
        em.detach(updatedKehittamistoimenpiteet)
        updatedKehittamistoimenpiteet.lahiesimiesHyvaksynyt = true
        updatedKehittamistoimenpiteet.lahiesimiehenKuittausaika = KoejaksonVaiheetHelper.DEFAULT_MYONTAMISPAIVA
        val kehittamistoimenpiteetDTO = koejaksonKehittamistoimenpiteetMapper.toDto(updatedKehittamistoimenpiteet)

        testMockMvc.perform(put("/api/kouluttaja/koejakso/kehittamistoimenpiteet").contentType(MediaType.APPLICATION_JSON)
            .content(convertObjectToJsonBytes(kehittamistoimenpiteetDTO)).with(csrf()))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun updateLoppukeskusteluAsEsimiesWithoutKouluttajaSopimusHyvaksytty() {
        initTestWithKouluttajaSopimusNotHyvaksytty()
        val updatedLoppukeskustelu = koejaksonLoppukeskusteluRepository.findById(koejaksonLoppukeskustelu.id!!).get()
        em.detach(updatedLoppukeskustelu)
        updatedLoppukeskustelu.lahiesimiesHyvaksynyt = true
        updatedLoppukeskustelu.lahiesimiehenKuittausaika = KoejaksonVaiheetHelper.DEFAULT_MYONTAMISPAIVA
        val loppukeskusteluDTO = koejaksonLoppukeskusteluMapper.toDto(updatedLoppukeskustelu)

        testMockMvc.perform(put("/api/kouluttaja/koejakso/loppukeskustelu").contentType(MediaType.APPLICATION_JSON)
            .content(convertObjectToJsonBytes(loppukeskusteluDTO)).with(csrf()))
            .andExpect(status().isBadRequest)
    }

    private fun initTestWithKouluttajaSopimusNotHyvaksytty() {
        initTest()
        koejaksonKoulutussopimus.kouluttajat?.forEach {
            it.sopimusHyvaksytty = false
            it.kuittausaika = null
        }
        koejaksonKoulutussopimusRepository.saveAndFlush(koejaksonKoulutussopimus)
    }

    private fun initTest() {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        setSecurityContext(user.id!!, KOULUTTAJA)
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        em.persist(erikoistuvaLaakari)
        val vastuuhenkilo = KayttajaHelper.createEntity(em)
        em.persist(vastuuhenkilo)
        val kouluttaja = KayttajaHelper.createEntity(em)
        em.persist(kouluttaja)
        val esimies = KayttajaHelper.createEntity(em, user)
        em.persist(esimies)
        val yliopisto = Yliopisto(nimi = YliopistoEnum.TAMPEREEN_YLIOPISTO)
        em.persist(yliopisto)
        val vaiheet = KoejaksonVaiheetHelper.persistKoejaksoVaiheet(
            em,
            erikoistuvaLaakari,
            kouluttaja,
            esimies,
            vastuuhenkilo,
            yliopisto
        )
        koejaksonKoulutussopimus = vaiheet.koulutussopimus
        koejaksonAloituskeskustelu = vaiheet.aloituskeskustelu
        koejaksonValiarviointi = vaiheet.valiarviointi
        koejaksonKehittamistoimenpiteet = vaiheet.kehittamistoimenpiteet
        koejaksonLoppukeskustelu = vaiheet.loppukeskustelu
    }
}

