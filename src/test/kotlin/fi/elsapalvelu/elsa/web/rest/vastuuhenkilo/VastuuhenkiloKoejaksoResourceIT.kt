package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.repository.KoejaksonKoulutussopimusRepository
import fi.elsapalvelu.elsa.repository.KoejaksonVastuuhenkilonArvioRepository
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTyyppi
import fi.elsapalvelu.elsa.service.mapper.KoejaksonKoulutussopimusMapper
import fi.elsapalvelu.elsa.service.mapper.KoejaksonVastuuhenkilonArvioMapper
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
import javax.persistence.EntityManager
import kotlin.test.assertNotNull

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class VastuuhenkiloKoejaksoResourceIT {

    @Autowired
    private lateinit var koejaksonKoulutussopimusRepository: KoejaksonKoulutussopimusRepository

    @Autowired
    private lateinit var koejaksonKoulutussopimusMapper: KoejaksonKoulutussopimusMapper

    @Autowired
    private lateinit var koejaksonVastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository

    @Autowired
    private lateinit var vastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository

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

    private lateinit var vastuuhenkilo: Kayttaja

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @Transactional
    fun getKoejaksotByVastuuhenkilo() {
        initTest()

        koejaksonKoulutussopimus.vastuuhenkiloHyvaksynyt = true
        koejaksonKoulutussopimusRepository.saveAndFlush(koejaksonKoulutussopimus)

        koejaksonVastuuhenkilonArvio.vastuuhenkiloAllekirjoittanut = false
        vastuuhenkilonArvioRepository.saveAndFlush(koejaksonVastuuhenkilonArvio)

        restKoejaksoMockMvc.perform(get("/api/vastuuhenkilo/koejaksot"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$[0].id").value(koejaksonVastuuhenkilonArvio.id as Any))
            .andExpect(jsonPath("$[0].tila").value(KoejaksoTila.ODOTTAA_HYVAKSYNTAA.name as Any))
            .andExpect(jsonPath("$[0].tyyppi").value(KoejaksoTyyppi.VASTUUHENKILON_ARVIO.name as Any))
            .andExpect(jsonPath("$[0].erikoistuvanNimi").value(koejaksonLoppukeskustelu.erikoistuvanNimi as Any))
            .andExpect(jsonPath("$[0].hyvaksytytVaiheet[0].id").value(koejaksonLoppukeskustelu.id as Any))
            .andExpect(jsonPath("$[0].hyvaksytytVaiheet[0].tyyppi").value("LOPPUKESKUSTELU"))
            .andExpect(
                jsonPath("$[0].hyvaksytytVaiheet[1].id").value(
                    koejaksonKehittamistoimenpiteet.id as Any
                )
            )
            .andExpect(jsonPath("$[0].hyvaksytytVaiheet[1].tyyppi").value("KEHITTAMISTOIMENPITEET"))
            .andExpect(jsonPath("$[0].hyvaksytytVaiheet[2].id").value(koejaksonValiarviointi.id as Any))
            .andExpect(jsonPath("$[0].hyvaksytytVaiheet[2].tyyppi").value("VALIARVIOINTI"))
            .andExpect(jsonPath("$[0].hyvaksytytVaiheet[3].id").value(koejaksonAloituskeskustelu.id as Any))
            .andExpect(jsonPath("$[0].hyvaksytytVaiheet[3].tyyppi").value("ALOITUSKESKUSTELU"))
            .andExpect(jsonPath("$[1].id").value(koejaksonKoulutussopimus.id as Any))
            .andExpect(jsonPath("$[1].tila").value(KoejaksoTila.HYVAKSYTTY.name as Any))
            .andExpect(jsonPath("$[1].tyyppi").value("KOULUTUSSOPIMUS"))
    }

    @Test
    @Transactional
    fun getKoulutussopimus() {
        initTest()

        val id = koejaksonKoulutussopimus.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/koulutussopimus/{id}", id
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(koejaksonKoulutussopimus.id as Any))
            .andExpect(jsonPath("$.erikoistuvanNimi").value(koejaksonKoulutussopimus.erikoistuvanNimi as Any))
            .andExpect(jsonPath("$.erikoistuvanOpiskelijatunnus").value(koejaksonKoulutussopimus.erikoistuvanOpiskelijatunnus as Any))
            .andExpect(jsonPath("$.erikoistuvanYliopisto").value(koejaksonKoulutussopimus.erikoistuvanYliopisto as Any))
            .andExpect(jsonPath("$.erikoistuvanPuhelinnumero").value(koejaksonKoulutussopimus.erikoistuvanPuhelinnumero as Any))
            .andExpect(jsonPath("$.erikoistuvanSahkoposti").value(koejaksonKoulutussopimus.erikoistuvanSahkoposti as Any))
            .andExpect(jsonPath("$.lahetetty").value(koejaksonKoulutussopimus.lahetetty as Any))
            .andExpect(jsonPath("$.vastuuhenkilo.id").value(koejaksonKoulutussopimus.vastuuhenkilo?.id as Any))
            .andExpect(jsonPath("$.korjausehdotus").isEmpty)
    }

    @Test
    @Transactional
    fun getAloituskeskustelu() {
        initTest()

        koejaksonAloituskeskustelu.lahikouluttajaHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true

        val id = koejaksonAloituskeskustelu.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/aloituskeskustelu/{id}", id
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
    fun getAloituskeskustelu_LahiKouluttajaNotAccepted_ExpectNotFound() {
        initTest()

        koejaksonAloituskeskustelu.lahikouluttajaHyvaksynyt = false
        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true

        val id = koejaksonAloituskeskustelu.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/aloituskeskustelu/{id}", id
            )
        ).andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun getAloituskeskustelu_LahiEsimiesNotAccepted_ExpectNotFound() {
        initTest()

        koejaksonAloituskeskustelu.lahikouluttajaHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = false

        val id = koejaksonAloituskeskustelu.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/aloituskeskustelu/{id}", id
            )
        ).andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun getAloituskeskustelu_VastuuhenkilonArvioDoesNotExist_ExpectNotFound() {
        initTest(false)

        koejaksonAloituskeskustelu.lahikouluttajaHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true

        val id = koejaksonAloituskeskustelu.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/aloituskeskustelu/{id}", id
            )
        ).andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun getAloituskeskustelu_VastuuhenkilonArvioForSameErikoistuvaDoesNotExist_ExpectNotFound() {
        initTest(false)

        val newUser = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(newUser)
        em.flush()
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, newUser)
        em.persist(erikoistuvaLaakari)

        koejaksonVastuuhenkilonArvio = createVastuuhenkilonArvio(erikoistuvaLaakari, vastuuhenkilo)
            em.persist(koejaksonVastuuhenkilonArvio)

        koejaksonAloituskeskustelu.lahikouluttajaHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true

        val id = koejaksonAloituskeskustelu.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/aloituskeskustelu/{id}", id
            )
        ).andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun getValiarviointi() {
        initTest()

        koejaksonValiarviointi.lahikouluttajaHyvaksynyt = true
        koejaksonValiarviointi.lahiesimiesHyvaksynyt = true

        val id = koejaksonValiarviointi.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/valiarviointi/{id}", id
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
    fun getValiarviointi_LahiKouluttajaNotAccepted_ExpectNotFound() {
        initTest()

        koejaksonValiarviointi.lahikouluttajaHyvaksynyt = false
        koejaksonValiarviointi.lahiesimiesHyvaksynyt = true

        val id = koejaksonValiarviointi.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/valiarviointi/{id}", id
            )
        ).andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun getValiarviointi_LahiEsimiesNotAccepted_ExpectNotFound() {
        initTest()

        koejaksonValiarviointi.lahikouluttajaHyvaksynyt = true
        koejaksonValiarviointi.lahiesimiesHyvaksynyt = false

        val id = koejaksonValiarviointi.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/valiarviointi/{id}", id
            )
        ).andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun getValiarviointi_VastuuhenkilonArvioDoesNotExist_ExpectNotFound() {
        initTest(false)

        koejaksonValiarviointi.lahikouluttajaHyvaksynyt = true
        koejaksonValiarviointi.lahiesimiesHyvaksynyt = true

        val id = koejaksonValiarviointi.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/valiarviointi/{id}", id
            )
        ).andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun getValiarviointi_VastuuhenkilonArvioForSameErikoistuvaDoesNotExist_ExpectNotFound() {
        initTest(false)

        val newUser = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(newUser)
        em.flush()
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, newUser)
        em.persist(erikoistuvaLaakari)

        koejaksonVastuuhenkilonArvio = createVastuuhenkilonArvio(erikoistuvaLaakari, vastuuhenkilo)
            em.persist(koejaksonVastuuhenkilonArvio)

        koejaksonValiarviointi.lahikouluttajaHyvaksynyt = true
        koejaksonValiarviointi.lahiesimiesHyvaksynyt = true

        val id = koejaksonValiarviointi.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/valiarviointi/{id}", id
            )
        ).andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun getKehittamistoimenpiteet() {
        initTest()

        val id = koejaksonKehittamistoimenpiteet.id
        assertNotNull(id)

        koejaksonKehittamistoimenpiteet.lahikouluttajaHyvaksynyt = true
        koejaksonKehittamistoimenpiteet.lahiesimiesHyvaksynyt = true

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/kehittamistoimenpiteet/{id}", id
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
    fun getKehittamistoimenpiteet_LahiKouluttajaNotAccepted_ExpectNotFound() {
        initTest()

        koejaksonKehittamistoimenpiteet.lahikouluttajaHyvaksynyt = false
        koejaksonKehittamistoimenpiteet.lahiesimiesHyvaksynyt = true

        val id = koejaksonKehittamistoimenpiteet.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/kehittamistoimenpiteet/{id}", id
            )
        ).andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun getKehittamistoimenpiteet_LahiEsimiesNotAccepted_ExpectNotFound() {
        initTest()

        koejaksonKehittamistoimenpiteet.lahikouluttajaHyvaksynyt = true
        koejaksonKehittamistoimenpiteet.lahiesimiesHyvaksynyt = false

        val id = koejaksonKehittamistoimenpiteet.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/kehittamistoimenpiteet/{id}", id
            )
        ).andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun getKehittamistoimenpiteet_VastuuhenkilonArvioDoesNotExist_ExpectNotFound() {
        initTest(false)

        koejaksonKehittamistoimenpiteet.lahikouluttajaHyvaksynyt = true
        koejaksonKehittamistoimenpiteet.lahiesimiesHyvaksynyt = true

        val id = koejaksonKehittamistoimenpiteet.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/kehittamistoimenpiteet/{id}", id
            )
        ).andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun getKehittamistoimenpiteet_VastuuhenkilonArvioForSameErikoistuvaDoesNotExist_ExpectNotFound() {
        initTest(false)

        val newUser = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(newUser)
        em.flush()
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, newUser)
        em.persist(erikoistuvaLaakari)

        koejaksonVastuuhenkilonArvio = createVastuuhenkilonArvio(erikoistuvaLaakari, vastuuhenkilo)
            em.persist(koejaksonVastuuhenkilonArvio)

        koejaksonKehittamistoimenpiteet.lahikouluttajaHyvaksynyt = true
        koejaksonKehittamistoimenpiteet.lahiesimiesHyvaksynyt = true

        val id = koejaksonKehittamistoimenpiteet.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/kehittamistoimenpiteet/{id}", id
            )
        ).andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun getLoppukeskustelu() {
        initTest()

        val id = koejaksonLoppukeskustelu.id
        assertNotNull(id)

        koejaksonLoppukeskustelu.lahikouluttajaHyvaksynyt = true
        koejaksonLoppukeskustelu.lahiesimiesHyvaksynyt = true

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/loppukeskustelu/{id}", id
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
    fun getLoppukeskustelu_LahiKouluttajaNotAccepted_ExpectNotFound() {
        initTest()

        koejaksonLoppukeskustelu.lahikouluttajaHyvaksynyt = false
        koejaksonLoppukeskustelu.lahiesimiesHyvaksynyt = true

        val id = koejaksonLoppukeskustelu.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/loppukeskustelu/{id}", id
            )
        ).andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun getLoppukeskustelu_LahiEsimiesNotAccepted_ExpectNotFound() {
        initTest()

        koejaksonLoppukeskustelu.lahikouluttajaHyvaksynyt = true
        koejaksonLoppukeskustelu.lahiesimiesHyvaksynyt = false

        val id = koejaksonLoppukeskustelu.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/loppukeskustelu/{id}", id
            )
        ).andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun getLoppukeskustelu_VastuuhenkilonArvioDoesNotExist_ExpectNotFound() {
        initTest(false)

        koejaksonLoppukeskustelu.lahikouluttajaHyvaksynyt = true
        koejaksonLoppukeskustelu.lahiesimiesHyvaksynyt = true

        val id = koejaksonLoppukeskustelu.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/loppukeskustelu/{id}", id
            )
        ).andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun getLoppukeskustelu_VastuuhenkilonArvioForSameErikoistuvaDoesNotExist_ExpectNotFound() {
        initTest(false)

        val newUser = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(newUser)
        em.flush()
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, newUser)
        em.persist(erikoistuvaLaakari)

        koejaksonVastuuhenkilonArvio = createVastuuhenkilonArvio(erikoistuvaLaakari, vastuuhenkilo)
            em.persist(koejaksonVastuuhenkilonArvio)

        koejaksonLoppukeskustelu.lahikouluttajaHyvaksynyt = true
        koejaksonLoppukeskustelu.lahiesimiesHyvaksynyt = true

        val id = koejaksonLoppukeskustelu.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/loppukeskustelu/{id}", id
            )
        ).andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun getVastuuhenkilonArvio() {
        initTest()

        val id = koejaksonVastuuhenkilonArvio.id
        assertNotNull(id)

        restKoejaksoMockMvc.perform(
            get(
                "/api/vastuuhenkilo/koejakso/vastuuhenkilonarvio/{id}", id
            )
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(koejaksonVastuuhenkilonArvio.id as Any))
            .andExpect(jsonPath("$.erikoistuvanNimi").value(koejaksonVastuuhenkilonArvio.erikoistuvanNimi as Any))
            .andExpect(jsonPath("$.erikoistuvanOpiskelijatunnus").value(koejaksonVastuuhenkilonArvio.erikoistuvanOpiskelijatunnus as Any))
            .andExpect(jsonPath("$.erikoistuvanYliopisto").value(koejaksonVastuuhenkilonArvio.erikoistuvanYliopisto as Any))
            .andExpect(jsonPath("$.vastuuhenkilo.id").value(koejaksonVastuuhenkilonArvio.vastuuhenkilo?.id as Any))
    }

    @Test
    @Transactional
    fun ackKoulutussopimusInProgressWithErikoistuva() {
        initTest()

        koejaksonKoulutussopimus.lahetetty = false
        koejaksonKoulutussopimusRepository.saveAndFlush(koejaksonKoulutussopimus)

        val databaseSizeBeforeUpdate = koejaksonKoulutussopimusRepository.findAll().size

        val id = koejaksonKoulutussopimus.id
        assertNotNull(id)
        val updatedKoulutussopimus = koejaksonKoulutussopimusRepository.findById(id).get()
        em.detach(updatedKoulutussopimus)

        updatedKoulutussopimus.vastuuhenkiloHyvaksynyt = true
        updatedKoulutussopimus.vastuuhenkilonKuittausaika =
            KoejaksonVaiheetHelper.DEFAULT_KUITTAUSAIKA_VASTUUHENKILO

        val koulutussopimusDTO = koejaksonKoulutussopimusMapper.toDto(updatedKoulutussopimus)

        restKoejaksoMockMvc.perform(
            put("/api/vastuuhenkilo/koejakso/koulutussopimus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koulutussopimusDTO))
                .with(csrf())
        ).andExpect(status().isBadRequest)

        val koulutussopimusList = koejaksonKoulutussopimusRepository.findAll()
        assertThat(koulutussopimusList).hasSize(databaseSizeBeforeUpdate)
    }

    @Test
    @Transactional
    fun ackKoulutussopimusInProgressWithKouluttaja() {
        initTest()

        koejaksonKoulutussopimus.kouluttajat?.forEach { it.sopimusHyvaksytty = false }
        koejaksonKoulutussopimusRepository.saveAndFlush(koejaksonKoulutussopimus)

        val databaseSizeBeforeUpdate = koejaksonKoulutussopimusRepository.findAll().size

        val id = koejaksonKoulutussopimus.id
        assertNotNull(id)
        val updatedKoulutussopimus = koejaksonKoulutussopimusRepository.findById(id).get()
        em.detach(updatedKoulutussopimus)

        updatedKoulutussopimus.vastuuhenkiloHyvaksynyt = true
        updatedKoulutussopimus.vastuuhenkilonKuittausaika =
            KoejaksonVaiheetHelper.DEFAULT_KUITTAUSAIKA_VASTUUHENKILO

        val koulutussopimusDTO = koejaksonKoulutussopimusMapper.toDto(updatedKoulutussopimus)

        restKoejaksoMockMvc.perform(
            put("/api/vastuuhenkilo/koejakso/koulutussopimus")
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

        updatedKoulutussopimus.vastuuhenkiloHyvaksynyt = true
        updatedKoulutussopimus.vastuuhenkilonKuittausaika =
            KoejaksonVaiheetHelper.DEFAULT_KUITTAUSAIKA_VASTUUHENKILO

        val koulutussopimusDTO = koejaksonKoulutussopimusMapper.toDto(updatedKoulutussopimus)

        restKoejaksoMockMvc.perform(
            put("/api/vastuuhenkilo/koejakso/koulutussopimus")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(koulutussopimusDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val koulutussopimusList = koejaksonKoulutussopimusRepository.findAll()
        assertThat(koulutussopimusList).hasSize(databaseSizeBeforeUpdate)
        val testKoulutussopimus = koulutussopimusList[koulutussopimusList.size - 1]
        assertThat(testKoulutussopimus.korjausehdotus).isNull()
        assertThat(testKoulutussopimus.vastuuhenkilonKuittausaika).isEqualTo(
            KoejaksonVaiheetHelper.DEFAULT_KUITTAUSAIKA_VASTUUHENKILO
        )
        assertThat(testKoulutussopimus.vastuuhenkiloHyvaksynyt).isEqualTo(true)
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

        updatedKoulutussopimus.vastuuhenkiloHyvaksynyt = false
        updatedKoulutussopimus.korjausehdotus = KoejaksonVaiheetHelper.UPDATED_KORJAUSEHDOTUS

        val koulutussopimusDTO = koejaksonKoulutussopimusMapper.toDto(updatedKoulutussopimus)

        restKoejaksoMockMvc.perform(
            put("/api/vastuuhenkilo/koejakso/koulutussopimus")
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
        assertThat(testKouluttaja?.sopimusHyvaksytty).isEqualTo(false)
        assertThat(testKouluttaja?.kuittausaika).isNull()
    }

    @Test
    @Transactional
    fun ackVastuuhenkilonArvio() {
        initTest()

        val databaseSizeBeforeUpdate = koejaksonVastuuhenkilonArvioRepository.findAll().size

        val id = koejaksonVastuuhenkilonArvio.id
        assertNotNull(id)
        val updatedVastuuhenkilonArvio = koejaksonVastuuhenkilonArvioRepository.findById(id).get()
        em.detach(updatedVastuuhenkilonArvio)

        updatedVastuuhenkilonArvio.koejaksoHyvaksytty = true
        updatedVastuuhenkilonArvio.vastuuhenkiloAllekirjoittanut = true
        updatedVastuuhenkilonArvio.vastuuhenkilonKuittausaika =
            KoejaksonVaiheetHelper.DEFAULT_KUITTAUSAIKA_VASTUUHENKILO

        val vastuuhenkilonArvioDTO =
            koejaksonVastuuhenkilonArvioMapper.toDto(updatedVastuuhenkilonArvio)

        restKoejaksoMockMvc.perform(
            put("/api/vastuuhenkilo/koejakso/vastuuhenkilonarvio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(vastuuhenkilonArvioDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val vastuuhenkilonArvioList = koejaksonVastuuhenkilonArvioRepository.findAll()
        assertThat(vastuuhenkilonArvioList).hasSize(databaseSizeBeforeUpdate)
        val testVastuuhenkilonArvio = vastuuhenkilonArvioList[vastuuhenkilonArvioList.size - 1]
        assertThat(testVastuuhenkilonArvio.koejaksoHyvaksytty).isEqualTo(true)
        assertThat(testVastuuhenkilonArvio.vastuuhenkiloAllekirjoittanut).isEqualTo(true)
        assertThat(testVastuuhenkilonArvio.vastuuhenkilonKuittausaika).isEqualTo(
            KoejaksonVaiheetHelper.DEFAULT_KUITTAUSAIKA_VASTUUHENKILO
        )
    }

    @Test
    @Transactional
    fun declineVastuuhenkilonArvio() {
        initTest()

        val databaseSizeBeforeUpdate = koejaksonVastuuhenkilonArvioRepository.findAll().size

        val id = koejaksonVastuuhenkilonArvio.id
        assertNotNull(id)
        val updatedVastuuhenkilonArvio = koejaksonVastuuhenkilonArvioRepository.findById(id).get()
        em.detach(updatedVastuuhenkilonArvio)

        updatedVastuuhenkilonArvio.koejaksoHyvaksytty = false
        updatedVastuuhenkilonArvio.vastuuhenkiloAllekirjoittanut = true
        updatedVastuuhenkilonArvio.vastuuhenkilonKuittausaika =
            KoejaksonVaiheetHelper.DEFAULT_KUITTAUSAIKA_VASTUUHENKILO

        val vastuuhenkilonArvioDTO =
            koejaksonVastuuhenkilonArvioMapper.toDto(updatedVastuuhenkilonArvio)

        restKoejaksoMockMvc.perform(
            put("/api/vastuuhenkilo/koejakso/vastuuhenkilonarvio")
                .contentType(MediaType.APPLICATION_JSON)
                .content(convertObjectToJsonBytes(vastuuhenkilonArvioDTO))
                .with(csrf())
        ).andExpect(status().isOk)

        val vastuuhenkilonArvioList = koejaksonVastuuhenkilonArvioRepository.findAll()
        assertThat(vastuuhenkilonArvioList).hasSize(databaseSizeBeforeUpdate)
        val testVastuuhenkilonArvio = vastuuhenkilonArvioList[vastuuhenkilonArvioList.size - 1]
        assertThat(testVastuuhenkilonArvio.koejaksoHyvaksytty).isEqualTo(false)
        assertThat(testVastuuhenkilonArvio.vastuuhenkilonKuittausaika).isNotNull
    }

    fun initTest(createVastuuhenkilonArvio: Boolean? = true) {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>(
        )
        val authorities = listOf(SimpleGrantedAuthority(VASTUUHENKILO))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication
        val erikoistuvaLaakari =
            ErikoistuvaLaakariHelper.createEntity(em)
        em.persist(erikoistuvaLaakari)

        vastuuhenkilo = KayttajaHelper.createEntity(em, user)
        em.persist(vastuuhenkilo)

        val kouluttaja = KayttajaHelper.createEntity(em)
        em.persist(kouluttaja)

        val esimies = KayttajaHelper.createEntity(em)
        em.persist(esimies)

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
                    koejaksonKoulutussopimus
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

        if (createVastuuhenkilonArvio == true) {
            koejaksonVastuuhenkilonArvio = createVastuuhenkilonArvio(erikoistuvaLaakari, vastuuhenkilo)
            em.persist(koejaksonVastuuhenkilonArvio)
        }
    }

    companion object {

        @JvmStatic
        fun createVastuuhenkilonArvio(
            erikoistuvaLaakari: ErikoistuvaLaakari,
            vastuuhenkilo: Kayttaja
        ): KoejaksonVastuuhenkilonArvio {
            return KoejaksonVastuuhenkilonArvio(
                erikoistuvaLaakari = erikoistuvaLaakari,
                erikoistuvanNimi = erikoistuvaLaakari.kayttaja?.getNimi(),
                erikoistuvanOpiskelijatunnus = erikoistuvaLaakari.opiskelijatunnus,
                erikoistuvanYliopisto = erikoistuvaLaakari.kayttaja?.yliopisto?.nimi,
                erikoistuvanErikoisala = KoejaksonVaiheetHelper.DEFAULT_ERIKOISALA,
                muokkauspaiva = KoejaksonVaiheetHelper.DEFAULT_MUOKKAUSPAIVA,
                vastuuhenkilo = vastuuhenkilo,
                vastuuhenkilonNimi = vastuuhenkilo.getNimi(),
                vastuuhenkilonNimike = vastuuhenkilo.nimike
            )
        }
    }
}
