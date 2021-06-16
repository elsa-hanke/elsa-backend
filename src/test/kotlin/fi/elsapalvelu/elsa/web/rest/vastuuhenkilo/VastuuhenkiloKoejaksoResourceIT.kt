package fi.elsapalvelu.elsa.web.rest.vastuuhenkilo

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import fi.elsapalvelu.elsa.domain.*
import fi.elsapalvelu.elsa.repository.*
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTyyppi
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KayttajaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KoejaksonVaiheetHelper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import javax.persistence.EntityManager
import kotlin.test.assertNotNull

@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
class VastuuhenkiloKoejaksoResourceIT {

    @Autowired
    private lateinit var koejaksonAloituskeskusteluRepository: KoejaksonAloituskeskusteluRepository

    @Autowired
    private lateinit var koejaksonValiarviointiRepository: KoejaksonValiarviointiRepository

    @Autowired
    private lateinit var koejaksonKehittamistoimenpiteetRepository: KoejaksonKehittamistoimenpiteetRepository

    @Autowired
    private lateinit var koejaksonLoppukeskusteluRepository: KoejaksonLoppukeskusteluRepository

    @Autowired
    private lateinit var vastuuhenkilonArvioRepository: KoejaksonVastuuhenkilonArvioRepository

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restKoejaksoMockMvc: MockMvc

    private lateinit var koejaksonKoulutussopimus: KoejaksonKoulutussopimus

    private lateinit var koejaksonAloituskeskustelu: KoejaksonAloituskeskustelu

    private lateinit var koejaksonValiarviointi: KoejaksonValiarviointi

    private lateinit var koejaksonKehittamistoimenpiteet: KoejaksonKehittamistoimenpiteet

    private lateinit var koejaksonLoppukeskustelu: KoejaksonLoppukeskustelu

    private lateinit var vastuuhenkilonArvio: KoejaksonVastuuhenkilonArvio

    @BeforeEach
    fun setup() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    @Transactional
    fun getKoejaksot() {
        initTest()

        val id = koejaksonKoulutussopimus.id
        assertNotNull(id)

        koejaksonAloituskeskustelu.lahiesimiesHyvaksynyt = true
        koejaksonAloituskeskustelu.lahiesimiehenKuittausaika = LocalDate.now()
        koejaksonAloituskeskusteluRepository.saveAndFlush(koejaksonAloituskeskustelu)

        koejaksonValiarviointi.erikoistuvaAllekirjoittanut = true
        koejaksonValiarviointi.kehittamistoimenpiteet = KoejaksonVaiheetHelper.DEFAULT_KEHITTAMISTOIMENPITEET
        koejaksonValiarviointiRepository.saveAndFlush(koejaksonValiarviointi)

        koejaksonKehittamistoimenpiteet.erikoistuvaAllekirjoittanut = true
        koejaksonKehittamistoimenpiteetRepository.saveAndFlush(koejaksonKehittamistoimenpiteet)

        koejaksonLoppukeskustelu.erikoistuvaAllekirjoittanut = true
        koejaksonLoppukeskusteluRepository.saveAndFlush(koejaksonLoppukeskustelu)

        vastuuhenkilonArvio.vastuuhenkiloHyvaksynyt = false
        vastuuhenkilonArvioRepository.saveAndFlush(vastuuhenkilonArvio)

        restKoejaksoMockMvc.perform(get("/api/vastuuhenkilo/koejaksot"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$[0].id").value(koejaksonKoulutussopimus.id as Any))
            .andExpect(jsonPath("$[0].tila").value(KoejaksoTila.ODOTTAA_HYVAKSYNTAA.name as Any))
            .andExpect(jsonPath("$[1].id").value(vastuuhenkilonArvio.id as Any))
            .andExpect(jsonPath("$[1].tila").value(KoejaksoTila.ODOTTAA_HYVAKSYNTAA.name as Any))
            .andExpect(jsonPath("$[1].tyyppi").value(KoejaksoTyyppi.VASTUUHENKILON_ARVIO.name as Any))
            .andExpect(jsonPath("$[1].erikoistuvanNimi").value(koejaksonLoppukeskustelu.erikoistuvanNimi as Any))
            .andExpect(jsonPath("$[1].hyvaksytytVaiheet[0].id").value(koejaksonLoppukeskustelu.id as Any))
            .andExpect(jsonPath("$[1].hyvaksytytVaiheet[0].tyyppi").value("LOPPUKESKUSTELU"))
            .andExpect(jsonPath("$[1].hyvaksytytVaiheet[1].id").value(koejaksonKehittamistoimenpiteet.id as Any))
            .andExpect(jsonPath("$[1].hyvaksytytVaiheet[1].tyyppi").value("KEHITTAMISTOIMENPITEET"))
            .andExpect(jsonPath("$[1].hyvaksytytVaiheet[2].id").value(koejaksonValiarviointi.id as Any))
            .andExpect(jsonPath("$[1].hyvaksytytVaiheet[2].tyyppi").value("VALIARVIOINTI"))
            .andExpect(jsonPath("$[1].hyvaksytytVaiheet[3].id").value(koejaksonAloituskeskustelu.id as Any))
            .andExpect(jsonPath("$[1].hyvaksytytVaiheet[3].tyyppi").value("ALOITUSKESKUSTELU"))
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
            .andExpect(status().isOk).andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
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

    fun initTest(userId: String? = KoejaksonVaiheetHelper.DEFAULT_VASTUUHENKILO_ID) {
        val userDetails = mapOf<String, Any>(
            "uid" to userId!!,
            "sub" to KoejaksonVaiheetHelper.DEFAULT_LOGIN,
            "email" to KoejaksonVaiheetHelper.DEFAULT_EMAIL
        )
        val authorities = listOf(SimpleGrantedAuthority(VASTUUHENKILO))
        val user = DefaultOAuth2User(authorities, userDetails, "sub")
        val authentication = OAuth2AuthenticationToken(user, authorities, "oidc")
        TestSecurityContextHolder.getContext().authentication = authentication
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em, KoejaksonVaiheetHelper.DEFAULT_ID)
        em.persist(erikoistuvaLaakari)

        val vastuuhenkilo = KayttajaHelper.createEntity(
            em, KoejaksonVaiheetHelper.DEFAULT_VASTUUHENKILO_ID
        )
        em.persist(vastuuhenkilo)

        val kouluttaja = KayttajaHelper.createEntity(
            em, KoejaksonVaiheetHelper.DEFAULT_KOULUTTAJA_ID
        )
        em.persist(kouluttaja)

        val esimies = KayttajaHelper.createEntity(
            em, KoejaksonVaiheetHelper.DEFAULT_ESIMIES_ID
        )
        em.persist(esimies)

        koejaksonKoulutussopimus = KoejaksonVaiheetHelper.createKoulutussopimus(erikoistuvaLaakari, vastuuhenkilo)
        koejaksonKoulutussopimus.kouluttajat =
            mutableSetOf(
                KoejaksonVaiheetHelper.createKoulutussopimuksenKouluttaja(
                    koejaksonKoulutussopimus,
                    kouluttaja
                )
            )
        koejaksonKoulutussopimus.koulutuspaikat =
            mutableSetOf(KoejaksonVaiheetHelper.createKoulutussopimuksenKoulutuspaikka(koejaksonKoulutussopimus))
        em.persist(koejaksonKoulutussopimus)

        koejaksonAloituskeskustelu =
            KoejaksonVaiheetHelper.createAloituskeskustelu(erikoistuvaLaakari, kouluttaja, esimies)
        em.persist(koejaksonAloituskeskustelu)
        koejaksonValiarviointi =
            KoejaksonVaiheetHelper.createValiarviointi(erikoistuvaLaakari, kouluttaja, esimies)
        em.persist(koejaksonValiarviointi)
        koejaksonKehittamistoimenpiteet =
            KoejaksonVaiheetHelper.createKehittamistoimenpiteet(erikoistuvaLaakari, kouluttaja, esimies)
        em.persist(koejaksonKehittamistoimenpiteet)
        koejaksonLoppukeskustelu =
            KoejaksonVaiheetHelper.createLoppukeskustelu(erikoistuvaLaakari, kouluttaja, esimies)
        em.persist(koejaksonLoppukeskustelu)
        vastuuhenkilonArvio = KoejaksonVaiheetHelper.createVastuuhenkilonArvio(erikoistuvaLaakari, vastuuhenkilo)
    }
}
