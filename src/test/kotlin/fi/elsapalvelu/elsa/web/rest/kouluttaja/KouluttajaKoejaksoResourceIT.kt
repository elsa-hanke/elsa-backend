package fi.elsapalvelu.elsa.web.rest.kouluttaja

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.domain.enumeration.YliopistoEnum
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTyyppi
import fi.elsapalvelu.elsa.service.mapper.*
import fi.elsapalvelu.elsa.web.rest.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KoejaksonVaiheetHelper
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import javax.persistence.EntityManager
import kotlin.test.assertNotNull

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class KouluttajaKoejaksoResourceIT {

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
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restKoejaksoMockMvc: MockMvc

    private lateinit var koejaksonKoulutussopimus: KoejaksonKoulutussopimus

    private lateinit var koejaksonAloituskeskustelu: KoejaksonAloituskeskustelu

    private lateinit var koejaksonValiarviointi: KoejaksonValiarviointi

    private lateinit var koejaksonKehittamistoimenpiteet: KoejaksonKehittamistoimenpiteet

    private lateinit var koejaksonLoppukeskustelu: KoejaksonLoppukeskustelu

    private lateinit var user: User

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @Transactional
    fun getKoejaksotByKouluttaja() {
        initTest()

        koejaksonKoulutussopimus.vastuuhenkiloHyvaksynyt = true
        koejaksonKoulutussopimusRepository.saveAndFlush(koejaksonKoulutussopimus)

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        koejaksonValiarviointi.erikoistuvaAllekirjoittanut = true
        koejaksonValiarviointi.kehittamistoimenpiteet =
            KoejaksonVaiheetHelper.DEFAULT_KEHITTAMISTOIMENPITEET
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        koejaksonKehittamistoimenpiteet.erikoistuvaAllekirjoittanut = true
        koejaksonKehittamistoimenpiteetRepository.saveAndFlush(koejaksonKehittamistoimenpiteet)

        restKoejaksoMockMvc.perform(get("/api/kouluttaja/koejaksot"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$[0].id").value(koejaksonLoppukeskustelu.id))
            .andExpect(jsonPath("$[0].tila").value(KoejaksoTila.ODOTTAA_HYVAKSYNTAA.name))
            .andExpect(jsonPath("$[0].tyyppi").value(KoejaksoTyyppi.LOPPUKESKUSTELU.name))
            .andExpect(jsonPath("$[0].erikoistuvanNimi").value(koejaksonLoppukeskustelu.erikoistuvanNimi))
            .andExpect(
                jsonPath("$[0].hyvaksytytVaiheet[0].id").value(
                    koejaksonKehittamistoimenpiteet.id
                )
            )
            .andExpect(jsonPath("$[0].hyvaksytytVaiheet[0].tyyppi").value("KEHITTAMISTOIMENPITEET"))
            .andExpect(jsonPath("$[0].hyvaksytytVaiheet[1].id").value(koejaksonValiarviointi.id))
            .andExpect(jsonPath("$[0].hyvaksytytVaiheet[1].tyyppi").value("VALIARVIOINTI"))
            .andExpect(jsonPath("$[0].hyvaksytytVaiheet[2].id").value(koejaksonAloituskeskustelu.id))
            .andExpect(jsonPath("$[0].hyvaksytytVaiheet[2].tyyppi").value("ALOITUSKESKUSTELU"))
            .andExpect(jsonPath("$[1].id").value(koejaksonKoulutussopimus.id))
            .andExpect(jsonPath("$[1].tila").value(KoejaksoTila.ODOTTAA_ALLEKIRJOITUKSIA.name))
            .andExpect(jsonPath("$[1].tyyppi").value("KOULUTUSSOPIMUS"))
            .andExpect(jsonPath("$[1].erikoistuvanNimi").value(koejaksonKoulutussopimus.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
    }

    @Test
    @Transactional
    fun getKoulutussopimus() {
        initTest()

        val id = koejaksonKoulutussopimus.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/kouluttaja/koejakso/koulutussopimus/{id}", id
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(koejaksonKoulutussopimus.id))
            .andExpect(jsonPath("$.erikoistuvanNimi").value(koejaksonKoulutussopimus.opintooikeus?.erikoistuvaLaakari?.kayttaja?.getNimi()))
            .andExpect(jsonPath("$.erikoistuvanOpiskelijatunnus").value(koejaksonKoulutussopimus.opintooikeus?.opiskelijatunnus))
            .andExpect(jsonPath("$.erikoistuvanYliopisto").value(koejaksonKoulutussopimus.opintooikeus?.yliopisto?.nimi.toString()))
            .andExpect(jsonPath("$.erikoistuvanPuhelinnumero").value(koejaksonKoulutussopimus.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.phoneNumber))
            .andExpect(jsonPath("$.erikoistuvanSahkoposti").value(koejaksonKoulutussopimus.opintooikeus?.erikoistuvaLaakari?.kayttaja?.user?.email))
            .andExpect(jsonPath("$.lahetetty").value(koejaksonKoulutussopimus.lahetetty))
            .andExpect(jsonPath("$.vastuuhenkilo.id").value(koejaksonKoulutussopimus.vastuuhenkilo?.id))
            .andExpect(jsonPath("$.korjausehdotus").isEmpty)
    }

    @Test
    @Transactional
    fun getAloituskeskustelu() {
        initTest()

        val id = koejaksonAloituskeskustelu.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/kouluttaja/koejakso/aloituskeskustelu/{id}", id
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(koejaksonAloituskeskustelu.id as Any))
            .andExpect(jsonPath("$.erikoistuvanNimi").value(koejaksonAloituskeskustelu.erikoistuvanNimi as Any))
            .andExpect(jsonPath("$.erikoistuvanOpiskelijatunnus").value(koejaksonAloituskeskustelu.erikoistuvanOpiskelijatunnus as Any))
            .andExpect(jsonPath("$.erikoistuvanYliopisto").value(koejaksonAloituskeskustelu.erikoistuvanYliopisto as Any))
            .andExpect(jsonPath("$.erikoistuvanSahkoposti").value(koejaksonAloituskeskustelu.erikoistuvanSahkoposti as Any))
            .andExpect(jsonPath("$.lahetetty").value(koejaksonAloituskeskustelu.lahetetty as Any))
            .andExpect(jsonPath("$.lahikouluttaja.id").value(koejaksonAloituskeskustelu.lahikouluttaja?.id as Any))
            .andExpect(jsonPath("$.lahiesimies.id").value(koejaksonAloituskeskustelu.lahiesimies?.id as Any))
            .andExpect(jsonPath("$.korjausehdotus").isEmpty)
    }

    @Test
    @Transactional
    fun getValiarviointi() {
        initTest()

        val id = koejaksonValiarviointi.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/kouluttaja/koejakso/valiarviointi/{id}", id
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(koejaksonValiarviointi.id as Any))
            .andExpect(jsonPath("$.erikoistuvanNimi").value(koejaksonValiarviointi.erikoistuvanNimi as Any))
            .andExpect(jsonPath("$.erikoistuvanOpiskelijatunnus").value(koejaksonValiarviointi.erikoistuvanOpiskelijatunnus as Any))
            .andExpect(jsonPath("$.erikoistuvanYliopisto").value(koejaksonValiarviointi.erikoistuvanYliopisto as Any))
            .andExpect(jsonPath("$.lahikouluttaja.id").value(koejaksonValiarviointi.lahikouluttaja?.id as Any))
            .andExpect(jsonPath("$.lahiesimies.id").value(koejaksonValiarviointi.lahiesimies?.id as Any))
            .andExpect(jsonPath("$.korjausehdotus").isEmpty)
    }

    @Test
    @Transactional
    fun getKehittamistoimenpiteet() {
        initTest()

        val id = koejaksonKehittamistoimenpiteet.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/kouluttaja/koejakso/kehittamistoimenpiteet/{id}", id
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(koejaksonKehittamistoimenpiteet.id as Any))
            .andExpect(jsonPath("$.erikoistuvanNimi").value(koejaksonKehittamistoimenpiteet.erikoistuvanNimi as Any))
            .andExpect(
                jsonPath("$.erikoistuvanOpiskelijatunnus").value(
                    koejaksonKehittamistoimenpiteet.erikoistuvanOpiskelijatunnus as Any
                )
            )
            .andExpect(jsonPath("$.erikoistuvanYliopisto").value(koejaksonKehittamistoimenpiteet.erikoistuvanYliopisto as Any))
            .andExpect(jsonPath("$.lahikouluttaja.id").value(koejaksonKehittamistoimenpiteet.lahikouluttaja?.id as Any))
            .andExpect(jsonPath("$.lahiesimies.id").value(koejaksonKehittamistoimenpiteet.lahiesimies?.id as Any))
            .andExpect(jsonPath("$.korjausehdotus").isEmpty)
    }

    @Test
    @Transactional
    fun getLoppukeskustelu() {
        initTest()

        val id = koejaksonLoppukeskustelu.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/kouluttaja/koejakso/loppukeskustelu/{id}", id
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(koejaksonLoppukeskustelu.id as Any))
            .andExpect(jsonPath("$.erikoistuvanNimi").value(koejaksonLoppukeskustelu.erikoistuvanNimi as Any))
            .andExpect(jsonPath("$.erikoistuvanOpiskelijatunnus").value(koejaksonLoppukeskustelu.erikoistuvanOpiskelijatunnus as Any))
            .andExpect(jsonPath("$.erikoistuvanYliopisto").value(koejaksonLoppukeskustelu.erikoistuvanYliopisto as Any))
            .andExpect(jsonPath("$.lahikouluttaja.id").value(koejaksonLoppukeskustelu.lahikouluttaja?.id as Any))
            .andExpect(jsonPath("$.lahiesimies.id").value(koejaksonLoppukeskustelu.lahiesimies?.id as Any))
            .andExpect(jsonPath("$.korjausehdotus").isEmpty)
    }

    @Test
    @Transactional
    fun ackKoulutussopimusInProgress() {
        initTest()

        koejaksonKoulutussopimus.lahetetty = false
        koejaksonKoulutussopimusRepository.saveAndFlush(koejaksonKoulutussopimus)

        val databaseSizeBeforeUpdate = koejaksonKoulutussopimusRepository.findAll().size

        val id = koejaksonKoulutussopimus.id
        assertNotNull(id)
        val updatedKoulutussopimus = koejaksonKoulutussopimusRepository.findById(id).get()
        em.detach(updatedKoulutussopimus)

        updatedKoulutussopimus.kouluttajat?.forEach {
            it.lahiosoite = KoejaksonVaiheetHelper.UPDATED_LAHIOSOITE
            it.toimipaikka = KoejaksonVaiheetHelper.UPDATED_TOIMIPAIKKA
            it.postitoimipaikka = KoejaksonVaiheetHelper.UPDATED_POSTITOIMIPAIKKA
            it.sopimusHyvaksytty = true
        }

        val koulutussopimusDTO = koejaksonKoulutussopimusMapper.toDto(updatedKoulutussopimus)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/koulutussopimus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koulutussopimusDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val koulutussopimusList = koejaksonKoulutussopimusRepository.findAll()
        assertThat(koulutussopimusList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun ackKoulutussopimus() {
        initTest()

        val databaseSizeBeforeUpdate = koejaksonKoulutussopimusRepository.findAll().size

        val id = koejaksonKoulutussopimus.id
        assertNotNull(id)
        val updatedKoulutussopimus = koejaksonKoulutussopimusRepository.findById(id).get()
        em.detach(updatedKoulutussopimus)

        updatedKoulutussopimus.kouluttajat?.forEach {
            it.lahiosoite = KoejaksonVaiheetHelper.UPDATED_LAHIOSOITE
            it.toimipaikka = KoejaksonVaiheetHelper.UPDATED_TOIMIPAIKKA
            it.postitoimipaikka = KoejaksonVaiheetHelper.UPDATED_POSTITOIMIPAIKKA
            it.sopimusHyvaksytty = true
        }

        val koulutussopimusDTO = koejaksonKoulutussopimusMapper.toDto(updatedKoulutussopimus)
        koulutussopimusDTO.kouluttajat?.forEach {
            it.puhelin = KoejaksonVaiheetHelper.UPDATED_PHONE
        }

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/koulutussopimus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koulutussopimusDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val koulutussopimusList = koejaksonKoulutussopimusRepository.findAll()
        assertThat(koulutussopimusList).hasSize(databaseSizeBeforeUpdate)
        val testKoulutussopimus = koulutussopimusList[koulutussopimusList.size - 1]
        assertThat(testKoulutussopimus.korjausehdotus).isNull()

        assertThat(testKoulutussopimus.kouluttajat).hasSize(1)
        val testKouluttaja = testKoulutussopimus.kouluttajat?.iterator()?.next()
        assertThat(testKouluttaja?.kouluttaja?.user?.phoneNumber).isEqualTo(KoejaksonVaiheetHelper.UPDATED_PHONE)
        assertThat(testKouluttaja?.lahiosoite).isEqualTo(KoejaksonVaiheetHelper.UPDATED_LAHIOSOITE)
        assertThat(testKouluttaja?.toimipaikka).isEqualTo(KoejaksonVaiheetHelper.UPDATED_TOIMIPAIKKA)
        assertThat(testKouluttaja?.postitoimipaikka).isEqualTo(KoejaksonVaiheetHelper.UPDATED_POSTITOIMIPAIKKA)
        assertThat(testKouluttaja?.sopimusHyvaksytty).isEqualTo(true)
        assertThat(testKouluttaja?.kuittausaika).isNotNull
    }

    @Test
    @Transactional
    fun declineKoulutussopimus() {
        initTest()

        val databaseSizeBeforeUpdate = koejaksonKoulutussopimusRepository.findAll().size

        val id = koejaksonKoulutussopimus.id
        assertNotNull(id)
        val updatedKoulutussopimus = koejaksonKoulutussopimusRepository.findById(id).get()
        em.detach(updatedKoulutussopimus)

        updatedKoulutussopimus.kouluttajat?.forEach {
            it.lahiosoite = KoejaksonVaiheetHelper.UPDATED_LAHIOSOITE
            it.toimipaikka = KoejaksonVaiheetHelper.UPDATED_TOIMIPAIKKA
            it.postitoimipaikka = KoejaksonVaiheetHelper.UPDATED_POSTITOIMIPAIKKA
            it.sopimusHyvaksytty = false
        }

        updatedKoulutussopimus.korjausehdotus = KoejaksonVaiheetHelper.UPDATED_KORJAUSEHDOTUS

        val koulutussopimusDTO = koejaksonKoulutussopimusMapper.toDto(updatedKoulutussopimus)
        koulutussopimusDTO.kouluttajat?.forEach {
            it.puhelin = KoejaksonVaiheetHelper.UPDATED_PHONE
        }

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/koulutussopimus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koulutussopimusDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val koulutussopimusList = koejaksonKoulutussopimusRepository.findAll()
        assertThat(koulutussopimusList).hasSize(databaseSizeBeforeUpdate)
        val testKoulutussopimus = koulutussopimusList[koulutussopimusList.size - 1]
        assertThat(testKoulutussopimus.korjausehdotus).isEqualTo(KoejaksonVaiheetHelper.UPDATED_KORJAUSEHDOTUS)
        assertThat(testKoulutussopimus.lahetetty).isEqualTo(false)

        assertThat(testKoulutussopimus.kouluttajat).hasSize(1)
        val testKouluttaja = testKoulutussopimus.kouluttajat?.iterator()?.next()
        assertThat(testKouluttaja?.kouluttaja?.user?.phoneNumber).isEqualTo(KoejaksonVaiheetHelper.UPDATED_PHONE)
        assertThat(testKouluttaja?.lahiosoite).isEqualTo(KoejaksonVaiheetHelper.UPDATED_LAHIOSOITE)
        assertThat(testKouluttaja?.toimipaikka).isEqualTo(KoejaksonVaiheetHelper.UPDATED_TOIMIPAIKKA)
        assertThat(testKouluttaja?.postitoimipaikka).isEqualTo(KoejaksonVaiheetHelper.UPDATED_POSTITOIMIPAIKKA)
        assertThat(testKouluttaja?.sopimusHyvaksytty).isEqualTo(false)
        assertThat(testKouluttaja?.kuittausaika).isNull()
    }

    @Test
    @Transactional
    fun ackAloituskeskusteluInProgress() {
        initTest()

        koejaksonAloituskeskustelu.lahetetty = false
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        val databaseSizeBeforeUpdate = koejaksonAloituskeskusteluRepository.findAll().size

        val id = koejaksonAloituskeskustelu.id
        assertNotNull(id)
        val updatedAloituskeskustelu = koejaksonAloituskeskusteluRepository.findById(id).get()
        em.detach(updatedAloituskeskustelu)

        updatedAloituskeskustelu.lahikouluttajaHyvaksynyt = true
        updatedAloituskeskustelu.lahikouluttajanKuittausaika = LocalDate.now()

        val aloituskeskusteluDTO = koejaksonAloituskeskusteluMapper.toDto(updatedAloituskeskustelu)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/aloituskeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(aloituskeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val aloituskeskusteluList = koejaksonAloituskeskusteluRepository.findAll()
        assertThat(aloituskeskusteluList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun ackAloituskeskusteluKouluttaja() {
        initTest()

        val databaseSizeBeforeUpdate = koejaksonAloituskeskusteluRepository.findAll().size

        val id = koejaksonAloituskeskustelu.id
        assertNotNull(id)
        val updatedAloituskeskustelu = koejaksonAloituskeskusteluRepository.findById(id).get()
        em.detach(updatedAloituskeskustelu)

        updatedAloituskeskustelu.lahikouluttajaHyvaksynyt = true
        updatedAloituskeskustelu.lahikouluttajanKuittausaika =
            KoejaksonVaiheetHelper.DEFAULT_MYONTAMISPAIVA

        val aloituskeskusteluDTO = koejaksonAloituskeskusteluMapper.toDto(updatedAloituskeskustelu)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/aloituskeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(aloituskeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val aloituskeskusteluList = koejaksonAloituskeskusteluRepository.findAll()
        assertThat(aloituskeskusteluList).hasSize(databaseSizeBeforeUpdate)
        val testAloituskeskustelu = aloituskeskusteluList[aloituskeskusteluList.size - 1]
        assertThat(testAloituskeskustelu.korjausehdotus).isNull()
        assertThat(testAloituskeskustelu.lahikouluttajaHyvaksynyt).isEqualTo(true)
        assertThat(testAloituskeskustelu.lahikouluttajanKuittausaika).isNotNull
        assertThat(testAloituskeskustelu.lahiesimiesHyvaksynyt).isEqualTo(false)
        assertThat(testAloituskeskustelu.lahiesimiehenKuittausaika).isNull()
        assertThat(testAloituskeskustelu.lahetetty).isEqualTo(true)
    }

    @Test
    @Transactional
    fun declineAloituskeskusteluKouluttaja() {
        initTest()

        val databaseSizeBeforeUpdate = koejaksonAloituskeskusteluRepository.findAll().size

        val id = koejaksonAloituskeskustelu.id
        assertNotNull(id)
        val updatedAloituskeskustelu = koejaksonAloituskeskusteluRepository.findById(id).get()
        em.detach(updatedAloituskeskustelu)

        updatedAloituskeskustelu.lahikouluttajaHyvaksynyt = false
        updatedAloituskeskustelu.korjausehdotus = KoejaksonVaiheetHelper.UPDATED_KORJAUSEHDOTUS

        val aloituskeskusteluDTO = koejaksonAloituskeskusteluMapper.toDto(updatedAloituskeskustelu)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/aloituskeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(aloituskeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val aloituskeskusteluList = koejaksonAloituskeskusteluRepository.findAll()
        assertThat(aloituskeskusteluList).hasSize(databaseSizeBeforeUpdate)
        val testAloituskeskustelu = aloituskeskusteluList[aloituskeskusteluList.size - 1]
        assertThat(testAloituskeskustelu.korjausehdotus).isEqualTo(KoejaksonVaiheetHelper.UPDATED_KORJAUSEHDOTUS)
        assertThat(testAloituskeskustelu.lahiesimiesHyvaksynyt).isEqualTo(false)
        assertThat(testAloituskeskustelu.lahiesimiehenKuittausaika).isNull()
        assertThat(testAloituskeskustelu.lahikouluttajaHyvaksynyt).isEqualTo(false)
        assertThat(testAloituskeskustelu.lahikouluttajanKuittausaika).isNull()
        assertThat(testAloituskeskustelu.lahetetty).isEqualTo(false)
    }

    @Test
    @Transactional
    fun ackAloituskeskusteluEsimies() {
        initTest(true)

        koejaksonAloituskeskustelu.lahikouluttajaHyvaksynyt = true
        koejaksonAloituskeskustelu.lahikouluttajanKuittausaika =
            KoejaksonVaiheetHelper.DEFAULT_MYONTAMISPAIVA
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        val databaseSizeBeforeUpdate = koejaksonAloituskeskusteluRepository.findAll().size

        val id = koejaksonAloituskeskustelu.id
        assertNotNull(id)
        val updatedAloituskeskustelu = koejaksonAloituskeskusteluRepository.findById(id).get()
        em.detach(updatedAloituskeskustelu)

        updatedAloituskeskustelu.lahiesimiesHyvaksynyt = true
        updatedAloituskeskustelu.lahiesimiehenKuittausaika =
            KoejaksonVaiheetHelper.DEFAULT_MYONTAMISPAIVA

        val aloituskeskusteluDTO = koejaksonAloituskeskusteluMapper.toDto(updatedAloituskeskustelu)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/aloituskeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(aloituskeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val aloituskeskusteluList = koejaksonAloituskeskusteluRepository.findAll()
        assertThat(aloituskeskusteluList).hasSize(databaseSizeBeforeUpdate)
        val testAloituskeskustelu = aloituskeskusteluList[aloituskeskusteluList.size - 1]
        assertThat(testAloituskeskustelu.korjausehdotus).isNull()
        assertThat(testAloituskeskustelu.lahiesimiesHyvaksynyt).isEqualTo(true)
        assertThat(testAloituskeskustelu.lahiesimiehenKuittausaika).isNotNull
        assertThat(testAloituskeskustelu.lahikouluttajaHyvaksynyt).isEqualTo(true)
        assertThat(testAloituskeskustelu.lahikouluttajanKuittausaika).isNotNull
        assertThat(testAloituskeskustelu.lahetetty).isEqualTo(true)
    }

    @Test
    @Transactional
    fun declineAloituskeskusteluEsimies() {
        initTest(true)

        koejaksonAloituskeskustelu.lahikouluttajaHyvaksynyt = true
        koejaksonAloituskeskustelu.lahikouluttajanKuittausaika =
            KoejaksonVaiheetHelper.DEFAULT_MYONTAMISPAIVA
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        val databaseSizeBeforeUpdate = koejaksonAloituskeskusteluRepository.findAll().size

        val id = koejaksonAloituskeskustelu.id
        assertNotNull(id)
        val updatedAloituskeskustelu = koejaksonAloituskeskusteluRepository.findById(id).get()
        em.detach(updatedAloituskeskustelu)

        updatedAloituskeskustelu.lahiesimiesHyvaksynyt = false
        updatedAloituskeskustelu.korjausehdotus = KoejaksonVaiheetHelper.UPDATED_KORJAUSEHDOTUS

        val aloituskeskusteluDTO = koejaksonAloituskeskusteluMapper.toDto(updatedAloituskeskustelu)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/aloituskeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(aloituskeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val aloituskeskusteluList = koejaksonAloituskeskusteluRepository.findAll()
        assertThat(aloituskeskusteluList).hasSize(databaseSizeBeforeUpdate)
        val testAloituskeskustelu = aloituskeskusteluList[aloituskeskusteluList.size - 1]
        assertThat(testAloituskeskustelu.korjausehdotus).isEqualTo(KoejaksonVaiheetHelper.UPDATED_KORJAUSEHDOTUS)
        assertThat(testAloituskeskustelu.lahiesimiesHyvaksynyt).isEqualTo(false)
        assertThat(testAloituskeskustelu.lahiesimiehenKuittausaika).isNull()
        assertThat(testAloituskeskustelu.lahikouluttajaHyvaksynyt).isEqualTo(false)
        assertThat(testAloituskeskustelu.lahikouluttajanKuittausaika).isNull()
        assertThat(testAloituskeskustelu.lahetetty).isEqualTo(false)
    }

    @Test
    @Transactional
    fun updateValiarviointiKouluttaja() {
        initTest()

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        val databaseSizeBeforeUpdate = koejaksonValiarviointiRepository.findAll().size

        val id = koejaksonValiarviointi.id
        assertNotNull(id)
        val updatedValiarviointi = koejaksonValiarviointiRepository.findById(id).get()
        em.detach(updatedValiarviointi)

        updatedValiarviointi.kehittamistoimenpiteet =
            KoejaksonVaiheetHelper.DEFAULT_KEHITTAMISTOIMENPITEET
        updatedValiarviointi.edistyminenTavoitteidenMukaista = false
        updatedValiarviointi.vahvuudet = KoejaksonVaiheetHelper.DEFAULT_VAHVUUDET
        updatedValiarviointi.lahikouluttajaHyvaksynyt = true
        updatedValiarviointi.lahikouluttajanKuittausaika =
            KoejaksonVaiheetHelper.DEFAULT_MYONTAMISPAIVA

        val valiarvointiDTO = koejaksonValiarviointiMapper.toDto(updatedValiarviointi)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/valiarviointi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(valiarvointiDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val valiarviointiList = koejaksonValiarviointiRepository.findAll()
        assertThat(valiarviointiList).hasSize(databaseSizeBeforeUpdate)
        val testValiarviointi = valiarviointiList[valiarviointiList.size - 1]
        assertThat(testValiarviointi.korjausehdotus).isNull()
        assertThat(testValiarviointi.lahikouluttajaHyvaksynyt).isEqualTo(true)
        assertThat(testValiarviointi.lahikouluttajanKuittausaika).isNotNull
        assertThat(testValiarviointi.lahiesimiesHyvaksynyt).isEqualTo(false)
        assertThat(testValiarviointi.lahiesimiehenKuittausaika).isNull()
        assertThat(testValiarviointi.edistyminenTavoitteidenMukaista).isEqualTo(false)
        assertThat(testValiarviointi.kehittamistoimenpiteet).isEqualTo(
            KoejaksonVaiheetHelper.DEFAULT_KEHITTAMISTOIMENPITEET
        )
        assertThat(testValiarviointi.vahvuudet).isEqualTo(KoejaksonVaiheetHelper.DEFAULT_VAHVUUDET)
    }

    @Test
    @Transactional
    fun ackValiarviointiEsimies() {
        initTest(true)

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        koejaksonValiarviointi.lahikouluttajaHyvaksynyt = true
        koejaksonValiarviointi.lahikouluttajanKuittausaika =
            KoejaksonVaiheetHelper.DEFAULT_MYONTAMISPAIVA
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        val databaseSizeBeforeUpdate = koejaksonValiarviointiRepository.findAll().size

        val id = koejaksonValiarviointi.id
        assertNotNull(id)
        val updatedValiarviointi = koejaksonValiarviointiRepository.findById(id).get()
        em.detach(updatedValiarviointi)

        updatedValiarviointi.lahiesimiesHyvaksynyt = true
        updatedValiarviointi.lahiesimiehenKuittausaika =
            KoejaksonVaiheetHelper.DEFAULT_MYONTAMISPAIVA

        val valiarviointiDTO = koejaksonValiarviointiMapper.toDto(updatedValiarviointi)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/valiarviointi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(valiarviointiDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val valiarviointiList = koejaksonValiarviointiRepository.findAll()
        assertThat(valiarviointiList).hasSize(databaseSizeBeforeUpdate)
        val testValiarviointi = valiarviointiList[valiarviointiList.size - 1]
        assertThat(testValiarviointi.korjausehdotus).isNull()
        assertThat(testValiarviointi.lahiesimiesHyvaksynyt).isEqualTo(true)
        assertThat(testValiarviointi.lahiesimiehenKuittausaika).isNotNull
        assertThat(testValiarviointi.lahikouluttajaHyvaksynyt).isEqualTo(true)
        assertThat(testValiarviointi.lahikouluttajanKuittausaika).isNotNull
    }

    @Test
    @Transactional
    fun declineValiarviointiEsimies() {
        initTest(true)

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        koejaksonValiarviointi.lahikouluttajaHyvaksynyt = true
        koejaksonValiarviointi.lahikouluttajanKuittausaika =
            KoejaksonVaiheetHelper.DEFAULT_MYONTAMISPAIVA
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        val databaseSizeBeforeUpdate = koejaksonValiarviointiRepository.findAll().size

        val id = koejaksonValiarviointi.id
        assertNotNull(id)
        val updatedValiarviointi = koejaksonValiarviointiRepository.findById(id).get()
        em.detach(updatedValiarviointi)

        updatedValiarviointi.lahiesimiesHyvaksynyt = false
        updatedValiarviointi.korjausehdotus = KoejaksonVaiheetHelper.UPDATED_KORJAUSEHDOTUS

        val valiarviointiDTO = koejaksonValiarviointiMapper.toDto(updatedValiarviointi)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/valiarviointi")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(valiarviointiDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val valiarviointiList = koejaksonValiarviointiRepository.findAll()
        assertThat(valiarviointiList).hasSize(databaseSizeBeforeUpdate)
        val testValiarviointi = valiarviointiList[valiarviointiList.size - 1]
        assertThat(testValiarviointi.korjausehdotus).isEqualTo(KoejaksonVaiheetHelper.UPDATED_KORJAUSEHDOTUS)
        assertThat(testValiarviointi.lahiesimiesHyvaksynyt).isEqualTo(false)
        assertThat(testValiarviointi.lahiesimiehenKuittausaika).isNull()
        assertThat(testValiarviointi.lahikouluttajaHyvaksynyt).isEqualTo(false)
        assertThat(testValiarviointi.lahikouluttajanKuittausaika).isNull()
    }

    @Test
    @Transactional
    fun updateKehittamistoimenpiteetKouluttaja() {
        initTest()

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        koejaksonValiarviointi.erikoistuvaAllekirjoittanut = true
        koejaksonValiarviointi.muokkauspaiva = LocalDate.now()
        koejaksonValiarviointi.kehittamistoimenpiteet =
            KoejaksonVaiheetHelper.DEFAULT_KEHITTAMISTOIMENPITEET
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        val databaseSizeBeforeUpdate = koejaksonKehittamistoimenpiteetRepository.findAll().size

        val id = koejaksonKehittamistoimenpiteet.id
        assertNotNull(id)
        val updatedKehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetRepository.findById(id).get()
        em.detach(updatedKehittamistoimenpiteet)

        updatedKehittamistoimenpiteet.kehittamistoimenpiteetRiittavat = true
        updatedKehittamistoimenpiteet.lahikouluttajaHyvaksynyt = true
        updatedKehittamistoimenpiteet.lahikouluttajanKuittausaika =
            KoejaksonVaiheetHelper.DEFAULT_MYONTAMISPAIVA

        val kehittamistoimenpiteetDTO =
            koejaksonKehittamistoimenpiteetMapper.toDto(updatedKehittamistoimenpiteet)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/kehittamistoimenpiteet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kehittamistoimenpiteetDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val kehittamistoimenpiteetList = koejaksonKehittamistoimenpiteetRepository.findAll()
        assertThat(kehittamistoimenpiteetList).hasSize(databaseSizeBeforeUpdate)
        val testKehittamistoimenpiteet =
            kehittamistoimenpiteetList[kehittamistoimenpiteetList.size - 1]
        assertThat(testKehittamistoimenpiteet.korjausehdotus).isNull()
        assertThat(testKehittamistoimenpiteet.lahikouluttajaHyvaksynyt).isEqualTo(true)
        assertThat(testKehittamistoimenpiteet.lahikouluttajanKuittausaika).isNotNull
        assertThat(testKehittamistoimenpiteet.lahiesimiesHyvaksynyt).isEqualTo(false)
        assertThat(testKehittamistoimenpiteet.lahiesimiehenKuittausaika).isNull()
        assertThat(testKehittamistoimenpiteet.kehittamistoimenpiteetRiittavat).isEqualTo(true)
    }

    @Test
    @Transactional
    fun ackKehittamistoimenpiteetEsimies() {
        initTest(true)

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        koejaksonValiarviointi.erikoistuvaAllekirjoittanut = true
        koejaksonValiarviointi.muokkauspaiva = LocalDate.now()
        koejaksonValiarviointi.kehittamistoimenpiteet =
            KoejaksonVaiheetHelper.DEFAULT_KEHITTAMISTOIMENPITEET
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        koejaksonKehittamistoimenpiteet.lahikouluttajaHyvaksynyt = true
        koejaksonKehittamistoimenpiteet.lahikouluttajanKuittausaika =
            KoejaksonVaiheetHelper.DEFAULT_MYONTAMISPAIVA
        koejaksonKehittamistoimenpiteetRepository.saveAndFlush(koejaksonKehittamistoimenpiteet)

        val databaseSizeBeforeUpdate = koejaksonKehittamistoimenpiteetRepository.findAll().size

        val id = koejaksonKehittamistoimenpiteet.id
        assertNotNull(id)
        val updatedKehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetRepository.findById(id).get()
        em.detach(updatedKehittamistoimenpiteet)

        updatedKehittamistoimenpiteet.lahiesimiesHyvaksynyt = true
        updatedKehittamistoimenpiteet.lahiesimiehenKuittausaika =
            KoejaksonVaiheetHelper.DEFAULT_MYONTAMISPAIVA

        val kehittamistoimenpiteetDTO =
            koejaksonKehittamistoimenpiteetMapper.toDto(updatedKehittamistoimenpiteet)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/kehittamistoimenpiteet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kehittamistoimenpiteetDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val kehittamistoimenpiteetList = koejaksonKehittamistoimenpiteetRepository.findAll()
        assertThat(kehittamistoimenpiteetList).hasSize(databaseSizeBeforeUpdate)
        val testKehittamistoimenpiteet =
            kehittamistoimenpiteetList[kehittamistoimenpiteetList.size - 1]
        assertThat(testKehittamistoimenpiteet.korjausehdotus).isNull()
        assertThat(testKehittamistoimenpiteet.lahiesimiesHyvaksynyt).isEqualTo(true)
        assertThat(testKehittamistoimenpiteet.lahiesimiehenKuittausaika).isNotNull
        assertThat(testKehittamistoimenpiteet.lahikouluttajaHyvaksynyt).isEqualTo(true)
        assertThat(testKehittamistoimenpiteet.lahikouluttajanKuittausaika).isNotNull
    }

    @Test
    @Transactional
    fun declineKehittamistoimenpiteetEsimies() {
        initTest(true)

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        koejaksonValiarviointi.erikoistuvaAllekirjoittanut = true
        koejaksonValiarviointi.muokkauspaiva = LocalDate.now()
        koejaksonValiarviointi.kehittamistoimenpiteet =
            KoejaksonVaiheetHelper.DEFAULT_KEHITTAMISTOIMENPITEET
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        koejaksonKehittamistoimenpiteet.lahikouluttajaHyvaksynyt = true
        koejaksonKehittamistoimenpiteet.lahikouluttajanKuittausaika =
            KoejaksonVaiheetHelper.DEFAULT_MYONTAMISPAIVA
        koejaksonKehittamistoimenpiteetRepository.saveAndFlush(koejaksonKehittamistoimenpiteet)

        val databaseSizeBeforeUpdate = koejaksonKehittamistoimenpiteetRepository.findAll().size

        val id = koejaksonKehittamistoimenpiteet.id
        assertNotNull(id)
        val updatedKehittamistoimenpiteet =
            koejaksonKehittamistoimenpiteetRepository.findById(id).get()
        em.detach(updatedKehittamistoimenpiteet)

        updatedKehittamistoimenpiteet.lahiesimiesHyvaksynyt = false
        updatedKehittamistoimenpiteet.korjausehdotus = KoejaksonVaiheetHelper.UPDATED_KORJAUSEHDOTUS

        val kehittamistoimenpiteetDTO =
            koejaksonKehittamistoimenpiteetMapper.toDto(updatedKehittamistoimenpiteet)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/kehittamistoimenpiteet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(kehittamistoimenpiteetDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val kehittamistoimenpiteetList = koejaksonKehittamistoimenpiteetRepository.findAll()
        assertThat(kehittamistoimenpiteetList).hasSize(databaseSizeBeforeUpdate)
        val testKehittamistoimenpiteet =
            kehittamistoimenpiteetList[kehittamistoimenpiteetList.size - 1]
        assertThat(testKehittamistoimenpiteet.korjausehdotus).isEqualTo(KoejaksonVaiheetHelper.UPDATED_KORJAUSEHDOTUS)
        assertThat(testKehittamistoimenpiteet.lahiesimiesHyvaksynyt).isEqualTo(false)
        assertThat(testKehittamistoimenpiteet.lahiesimiehenKuittausaika).isNull()
        assertThat(testKehittamistoimenpiteet.lahikouluttajaHyvaksynyt).isEqualTo(false)
        assertThat(testKehittamistoimenpiteet.lahikouluttajanKuittausaika).isNull()
    }

    @Test
    @Transactional
    fun updateLoppukeskusteluKouluttaja() {
        initTest()

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        koejaksonValiarviointi.erikoistuvaAllekirjoittanut = true
        koejaksonValiarviointi.muokkauspaiva = LocalDate.now()
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        val databaseSizeBeforeUpdate = koejaksonLoppukeskusteluRepository.findAll().size

        val id = koejaksonLoppukeskustelu.id
        assertNotNull(id)
        val updatedLoppukeskustelu = koejaksonLoppukeskusteluRepository.findById(id).get()
        em.detach(updatedLoppukeskustelu)

        updatedLoppukeskustelu.esitetaanKoejaksonHyvaksymista = true
        updatedLoppukeskustelu.lahikouluttajaHyvaksynyt = true
        updatedLoppukeskustelu.lahikouluttajanKuittausaika =
            KoejaksonVaiheetHelper.DEFAULT_MYONTAMISPAIVA

        val loppukeskusteluDTO = koejaksonLoppukeskusteluMapper.toDto(updatedLoppukeskustelu)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/loppukeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(loppukeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val loppukeskusteluList = koejaksonLoppukeskusteluRepository.findAll()
        assertThat(loppukeskusteluList).hasSize(databaseSizeBeforeUpdate)
        val testLoppukeskustelu = loppukeskusteluList[loppukeskusteluList.size - 1]
        assertThat(testLoppukeskustelu.korjausehdotus).isNull()
        assertThat(testLoppukeskustelu.lahikouluttajaHyvaksynyt).isEqualTo(true)
        assertThat(testLoppukeskustelu.lahikouluttajanKuittausaika).isNotNull
        assertThat(testLoppukeskustelu.lahiesimiesHyvaksynyt).isEqualTo(false)
        assertThat(testLoppukeskustelu.lahiesimiehenKuittausaika).isNull()
        assertThat(testLoppukeskustelu.esitetaanKoejaksonHyvaksymista).isEqualTo(true)
    }

    @Test
    @Transactional
    fun ackLoppukeskusteluEsimies() {
        initTest(true)

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        koejaksonValiarviointi.erikoistuvaAllekirjoittanut = true
        koejaksonValiarviointi.muokkauspaiva = LocalDate.now()
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        koejaksonLoppukeskustelu.lahikouluttajaHyvaksynyt = true
        koejaksonLoppukeskustelu.lahikouluttajanKuittausaika =
            KoejaksonVaiheetHelper.DEFAULT_MYONTAMISPAIVA
        koejaksonLoppukeskusteluRepository.saveAndFlush(koejaksonLoppukeskustelu)

        val databaseSizeBeforeUpdate = koejaksonLoppukeskusteluRepository.findAll().size

        val id = koejaksonLoppukeskustelu.id
        assertNotNull(id)
        val updatedLoppukeskustelu = koejaksonLoppukeskusteluRepository.findById(id).get()
        em.detach(updatedLoppukeskustelu)

        updatedLoppukeskustelu.lahiesimiesHyvaksynyt = true
        updatedLoppukeskustelu.lahiesimiehenKuittausaika =
            KoejaksonVaiheetHelper.DEFAULT_MYONTAMISPAIVA

        val loppukeskusteluDTO = koejaksonLoppukeskusteluMapper.toDto(updatedLoppukeskustelu)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/loppukeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(loppukeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val loppukeskusteluList = koejaksonLoppukeskusteluRepository.findAll()
        assertThat(loppukeskusteluList).hasSize(databaseSizeBeforeUpdate)
        val testLoppukeskustelu = loppukeskusteluList[loppukeskusteluList.size - 1]
        assertThat(testLoppukeskustelu.korjausehdotus).isNull()
        assertThat(testLoppukeskustelu.lahiesimiesHyvaksynyt).isEqualTo(true)
        assertThat(testLoppukeskustelu.lahiesimiehenKuittausaika).isNotNull
        assertThat(testLoppukeskustelu.lahikouluttajaHyvaksynyt).isEqualTo(true)
        assertThat(testLoppukeskustelu.lahikouluttajanKuittausaika).isNotNull
    }

    @Test
    @Transactional
    fun declineLoppukeskusteluEsimies() {
        initTest(true)

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        koejaksonValiarviointi.erikoistuvaAllekirjoittanut = true
        koejaksonValiarviointi.muokkauspaiva = LocalDate.now()
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        koejaksonLoppukeskustelu.lahikouluttajaHyvaksynyt = true
        koejaksonLoppukeskustelu.lahikouluttajanKuittausaika =
            KoejaksonVaiheetHelper.DEFAULT_MYONTAMISPAIVA
        koejaksonLoppukeskusteluRepository.saveAndFlush(koejaksonLoppukeskustelu)

        val databaseSizeBeforeUpdate = koejaksonLoppukeskusteluRepository.findAll().size

        val id = koejaksonLoppukeskustelu.id
        assertNotNull(id)
        val updatedLoppukeskustelu = koejaksonLoppukeskusteluRepository.findById(id).get()
        em.detach(updatedLoppukeskustelu)

        updatedLoppukeskustelu.lahiesimiesHyvaksynyt = false
        updatedLoppukeskustelu.korjausehdotus = KoejaksonVaiheetHelper.UPDATED_KORJAUSEHDOTUS

        val loppukeskusteluDTO = koejaksonLoppukeskusteluMapper.toDto(updatedLoppukeskustelu)

        restKoejaksoMockMvc.perform(
            put("/api/kouluttaja/koejakso/loppukeskustelu")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(loppukeskusteluDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val loppukeskusteluList = koejaksonLoppukeskusteluRepository.findAll()
        assertThat(loppukeskusteluList).hasSize(databaseSizeBeforeUpdate)
        val testLoppukeskustelu = loppukeskusteluList[loppukeskusteluList.size - 1]
        assertThat(testLoppukeskustelu.korjausehdotus).isEqualTo(KoejaksonVaiheetHelper.UPDATED_KORJAUSEHDOTUS)
        assertThat(testLoppukeskustelu.lahiesimiesHyvaksynyt).isEqualTo(false)
        assertThat(testLoppukeskustelu.lahiesimiehenKuittausaika).isNull()
        assertThat(testLoppukeskustelu.lahikouluttajaHyvaksynyt).isEqualTo(false)
        assertThat(testLoppukeskustelu.lahikouluttajanKuittausaika).isNull()
    }

    fun initTest(isEsimies: Boolean = false) {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>(
        )
        val authorities = listOf(SimpleGrantedAuthority(KOULUTTAJA))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication
        val erikoistuvaLaakari =
            ErikoistuvaLaakariHelper.createEntity(em)
        em.persist(erikoistuvaLaakari)

        val vastuuhenkilo = KayttajaHelper.createEntity(em)
        em.persist(vastuuhenkilo)

        val kouluttaja = KayttajaHelper.createEntity(em, if (isEsimies) null else user)
        em.persist(kouluttaja)

        val esimies = KayttajaHelper.createEntity(em, if (isEsimies) user else null)
        em.persist(esimies)

        val yliopisto = Yliopisto(nimi = YliopistoEnum.TAMPEREEN_YLIOPISTO)
        em.persist(yliopisto)

        koejaksonKoulutussopimus =
            KoejaksonVaiheetHelper.createKoulutussopimus(erikoistuvaLaakari, vastuuhenkilo)
        koejaksonKoulutussopimus.kouluttajat =
            mutableSetOf(
                KoejaksonVaiheetHelper.createKoulutussopimuksenKouluttaja(
                    koejaksonKoulutussopimus,
                    kouluttaja
                )
            )
        koejaksonKoulutussopimus.koulutuspaikat =
            mutableSetOf(
                KoejaksonVaiheetHelper.createKoulutussopimuksenKoulutuspaikka(
                    koejaksonKoulutussopimus, yliopisto
                )
            )
        em.persist(koejaksonKoulutussopimus)

        koejaksonAloituskeskustelu =
            KoejaksonVaiheetHelper.createAloituskeskustelu(erikoistuvaLaakari, kouluttaja, esimies)
        em.persist(koejaksonAloituskeskustelu)
        koejaksonValiarviointi =
            KoejaksonVaiheetHelper.createValiarviointi(erikoistuvaLaakari, kouluttaja, esimies)
        em.persist(koejaksonValiarviointi)
        koejaksonKehittamistoimenpiteet =
            KoejaksonVaiheetHelper.createKehittamistoimenpiteet(
                erikoistuvaLaakari,
                kouluttaja,
                esimies
            )
        em.persist(koejaksonKehittamistoimenpiteet)
        koejaksonLoppukeskustelu =
            KoejaksonVaiheetHelper.createLoppukeskustelu(erikoistuvaLaakari, kouluttaja, esimies)
        em.persist(koejaksonLoppukeskustelu)
    }
}
