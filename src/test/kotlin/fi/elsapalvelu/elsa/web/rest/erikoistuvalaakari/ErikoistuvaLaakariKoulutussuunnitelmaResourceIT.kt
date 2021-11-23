package fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.Koulutussuunnitelma
import fi.elsapalvelu.elsa.domain.User
import fi.elsapalvelu.elsa.repository.KoulutussuunnitelmaRepository
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.service.mapper.KoulutussuunnitelmaMapper
import fi.elsapalvelu.elsa.web.rest.KayttajaResourceWithMockUserIT
import fi.elsapalvelu.elsa.web.rest.errors.ExceptionTranslator
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.DEFAULT_ELAMANKENTTA
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.DEFAULT_ELAMANKENTTA_YKSITYINEN
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.DEFAULT_FILE
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.DEFAULT_MOTIVAATIOKIRJE
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.DEFAULT_MOTIVAATIOKIRJE_YKSITYINEN
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.DEFAULT_OPISKELU_JA_TYOHISTORIA
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.DEFAULT_OPISKELU_JA_TYOHISTORIA_YKSITYINEN
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.DEFAULT_OSAAMISEN_KARTUTTAMINEN
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.DEFAULT_OSAAMISEN_KARTUTTAMINEN_YKSITYINEN
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.DEFAULT_TULEVAISUUDEN_VISIOINTI
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.DEFAULT_TULEVAISUUDEN_VISIOINTI_YKSITYINEN
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.DEFAULT_VAHVUUDET
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.DEFAULT_VAHVUUDET_YKSITYINEN
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.UPDATED_ELAMANKENTTA
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.UPDATED_ELAMANKENTTA_YKSITYINEN
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.UPDATED_MOTIVAATIOKIRJE
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.UPDATED_MOTIVAATIOKIRJE_YKSITYINEN
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.UPDATED_OPISKELU_JA_TYOHISTORIA
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.UPDATED_OPISKELU_JA_TYOHISTORIA_YKSITYINEN
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.UPDATED_OSAAMISEN_KARTUTTAMINEN
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.UPDATED_OSAAMISEN_KARTUTTAMINEN_YKSITYINEN
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.UPDATED_TULEVAISUUDEN_VISIOINTI
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.UPDATED_TULEVAISUUDEN_VISIOINTI_YKSITYINEN
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.UPDATED_VAHVUUDET
import fi.elsapalvelu.elsa.web.rest.helpers.KoulutussuunnitelmaHelper.Companion.UPDATED_VAHVUUDET_YKSITYINEN
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.mock.web.MockMultipartFile
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.DefaultSaml2AuthenticatedPrincipal
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.security.test.context.TestSecurityContextHolder
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.Validator
import javax.persistence.EntityManager
import kotlin.test.assertNotNull


@AutoConfigureMockMvc
@SpringBootTest(classes = [ElsaBackendApp::class])
class ErikoistuvaLaakariKoulutussuunnitelmaResourceIT {

    @Autowired
    private lateinit var koulutussuunnitelmaRepository: KoulutussuunnitelmaRepository

    @Autowired
    private lateinit var koulutussuunnitelmaMapper: KoulutussuunnitelmaMapper

    @Autowired
    private lateinit var jacksonMessageConverter: MappingJackson2HttpMessageConverter

    @Autowired
    private lateinit var pageableArgumentResolver: PageableHandlerMethodArgumentResolver

    @Autowired
    private lateinit var exceptionTranslator: ExceptionTranslator

    @Autowired
    private lateinit var validator: Validator

    @Autowired
    private lateinit var em: EntityManager

    @Autowired
    private lateinit var restKoulutussuunnitelmaMockMvc: MockMvc

    private lateinit var koulutussuunnitelma: Koulutussuunnitelma

    private lateinit var user: User

    @BeforeEach
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    @Transactional
    fun getKoulutussuunnitelma() {
        initTest()

        koulutussuunnitelmaRepository.saveAndFlush(koulutussuunnitelma)

        val id = koulutussuunnitelma.id
        assertNotNull(id)

        restKoulutussuunnitelmaMockMvc.perform(get("/api/erikoistuva-laakari/koulutussuunnitelma"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").value(koulutussuunnitelma.id?.toInt()))
            .andExpect(jsonPath("$.motivaatiokirje").value(DEFAULT_MOTIVAATIOKIRJE))
            .andExpect(
                jsonPath("$.motivaatiokirjeYksityinen").value(
                    DEFAULT_MOTIVAATIOKIRJE_YKSITYINEN
                )
            )
            .andExpect(jsonPath("$.opiskeluJaTyohistoria").value(DEFAULT_OPISKELU_JA_TYOHISTORIA))
            .andExpect(
                jsonPath("$.opiskeluJaTyohistoriaYksityinen").value(
                    DEFAULT_OPISKELU_JA_TYOHISTORIA_YKSITYINEN
                )
            )
            .andExpect(jsonPath("$.vahvuudet").value(DEFAULT_VAHVUUDET))
            .andExpect(jsonPath("$.vahvuudetYksityinen").value(DEFAULT_VAHVUUDET_YKSITYINEN))
            .andExpect(jsonPath("$.tulevaisuudenVisiointi").value(DEFAULT_TULEVAISUUDEN_VISIOINTI))
            .andExpect(
                jsonPath("$.tulevaisuudenVisiointiYksityinen").value(
                    DEFAULT_TULEVAISUUDEN_VISIOINTI_YKSITYINEN
                )
            )
            .andExpect(jsonPath("$.osaamisenKartuttaminen").value(DEFAULT_OSAAMISEN_KARTUTTAMINEN))
            .andExpect(
                jsonPath("$.osaamisenKartuttaminenYksityinen").value(
                    DEFAULT_OSAAMISEN_KARTUTTAMINEN_YKSITYINEN
                )
            )
            .andExpect(jsonPath("$.elamankentta").value(DEFAULT_ELAMANKENTTA))
            .andExpect(jsonPath("$.elamankenttaYksityinen").value(DEFAULT_ELAMANKENTTA_YKSITYINEN))
    }

    @Test
    @Transactional
    fun getNonExistingKoulutussuunnitelma() {
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

        restKoulutussuunnitelmaMockMvc.perform(get("/api/erikoistuva-laakari/koulutussuunnitelma"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.motivaatiokirje").isEmpty)
            .andExpect(jsonPath("$.motivaatiokirjeYksityinen").value(false))
            .andExpect(jsonPath("$.opiskeluJaTyohistoria").isEmpty)
            .andExpect(jsonPath("$.opiskeluJaTyohistoriaYksityinen").value(false))
            .andExpect(jsonPath("$.vahvuudet").isEmpty)
            .andExpect(jsonPath("$.vahvuudetYksityinen").value(false))
            .andExpect(jsonPath("$.tulevaisuudenVisiointi").isEmpty)
            .andExpect(jsonPath("$.tulevaisuudenVisiointiYksityinen").value(false))
            .andExpect(jsonPath("$.osaamisenKartuttaminen").isEmpty)
            .andExpect(jsonPath("$.osaamisenKartuttaminenYksityinen").value(false))
            .andExpect(jsonPath("$.elamankentta").isEmpty)
            .andExpect(jsonPath("$.elamankenttaYksityinen").value(false))
    }

    @Test
    @Transactional
    fun getKoulutussuunnitelmaWithoutErikoistuvaLaakariRole() {
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

        koulutussuunnitelma = KoulutussuunnitelmaHelper.createEntity(em, user)
        koulutussuunnitelmaRepository.saveAndFlush(koulutussuunnitelma)

        val id = koulutussuunnitelma.id
        assertNotNull(id)

        restKoulutussuunnitelmaMockMvc.perform(get("/api/erikoistuva-laakari/koulutussuunnitelma"))
            .andExpect(status().isForbidden)
    }

    @Test
    @Transactional
    fun getKoulutussuunnitelmaWithoutErikoistuvaLaakari() {
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

        val koulutussuunnitelma = Koulutussuunnitelma(
            motivaatiokirje = DEFAULT_MOTIVAATIOKIRJE,
            motivaatiokirjeYksityinen = DEFAULT_MOTIVAATIOKIRJE_YKSITYINEN,
            opiskeluJaTyohistoria = DEFAULT_OPISKELU_JA_TYOHISTORIA,
            opiskeluJaTyohistoriaYksityinen = DEFAULT_OPISKELU_JA_TYOHISTORIA_YKSITYINEN,
            vahvuudet = DEFAULT_VAHVUUDET,
            vahvuudetYksityinen = DEFAULT_VAHVUUDET_YKSITYINEN,
            tulevaisuudenVisiointi = DEFAULT_TULEVAISUUDEN_VISIOINTI,
            tulevaisuudenVisiointiYksityinen = DEFAULT_TULEVAISUUDEN_VISIOINTI_YKSITYINEN,
            osaamisenKartuttaminen = DEFAULT_OSAAMISEN_KARTUTTAMINEN,
            osaamisenKartuttaminenYksityinen = DEFAULT_OSAAMISEN_KARTUTTAMINEN_YKSITYINEN,
            elamankentta = DEFAULT_ELAMANKENTTA,
            elamankenttaYksityinen = DEFAULT_ELAMANKENTTA_YKSITYINEN
        )

        // Koulutussuunnitelma kuuluu toiselle erikoistuvalle lääkärille
        val erikoistuvaLaakari = ErikoistuvaLaakariHelper.createEntity(em)
        em.persist(erikoistuvaLaakari)
        em.flush()
        koulutussuunnitelma.erikoistuvaLaakari = erikoistuvaLaakari

        koulutussuunnitelmaRepository.saveAndFlush(koulutussuunnitelma)

        val id = koulutussuunnitelma.id
        assertNotNull(id)

        restKoulutussuunnitelmaMockMvc.perform(get("/api/erikoistuva-laakari/koulutussuunnitelma"))
            .andExpect(status().isNotFound)
    }

    @Test
    @Transactional
    fun putKoulutussuunnitelma() {
        initTest()

        koulutussuunnitelmaRepository.saveAndFlush(koulutussuunnitelma)

        val databaseSizeBeforeUpdate = koulutussuunnitelmaRepository.findAll().size

        restKoulutussuunnitelmaMockMvc.perform(
            multipart("/api/erikoistuva-laakari/koulutussuunnitelma")
                .file(
                    MockMultipartFile(
                        "koulutussuunnitelmaFile",
                        "koulutussuunnitelma.pdf",
                        "application/pdf",
                        DEFAULT_FILE
                    )
                )
                .file(
                    MockMultipartFile(
                        "motivaatiokirjeFile",
                        "motivaatiokirje.pdf",
                        "application/pdf",
                        DEFAULT_FILE
                    )
                )
                .param("id", koulutussuunnitelma.id.toString())
                .param("motivaatiokirje", UPDATED_MOTIVAATIOKIRJE)
                .param(
                    "motivaatiokirjeYksityinen",
                    if (UPDATED_MOTIVAATIOKIRJE_YKSITYINEN) "true" else "false"
                )
                .param("opiskeluJaTyohistoria", UPDATED_OPISKELU_JA_TYOHISTORIA)
                .param(
                    "opiskeluJaTyohistoriaYksityinen",
                    UPDATED_OPISKELU_JA_TYOHISTORIA_YKSITYINEN.toString()
                )
                .param("vahvuudet", UPDATED_VAHVUUDET)
                .param("vahvuudetYksityinen", UPDATED_VAHVUUDET_YKSITYINEN.toString())
                .param("tulevaisuudenVisiointi", UPDATED_TULEVAISUUDEN_VISIOINTI)
                .param(
                    "tulevaisuudenVisiointiYksityinen",
                    UPDATED_TULEVAISUUDEN_VISIOINTI_YKSITYINEN.toString()
                )
                .param("osaamisenKartuttaminen", UPDATED_OSAAMISEN_KARTUTTAMINEN)
                .param(
                    "osaamisenKartuttaminenYksityinen",
                    UPDATED_OSAAMISEN_KARTUTTAMINEN_YKSITYINEN.toString()
                )
                .param("elamankentta", UPDATED_ELAMANKENTTA)
                .param("elamankenttaYksityinen", UPDATED_ELAMANKENTTA_YKSITYINEN.toString())
                .param(
                    "erikoistuvaLaakariId",
                    koulutussuunnitelma.erikoistuvaLaakari!!.id.toString()
                )
                .param("koulutussuunnitelmaAsiakirjaUpdated", true.toString())
                .param("motivaatiokirjeAsiakirjaUpdated", true.toString())
                .with { it.method = "PUT"; it }
                .with(SecurityMockMvcRequestPostProcessors.csrf())
        ).andExpect(status().isOk)

        val koulutussuunnitelmaList = koulutussuunnitelmaRepository.findAll()
        Assertions.assertThat(koulutussuunnitelmaList).hasSize(databaseSizeBeforeUpdate)
        val testKoulutussuunnitelma = koulutussuunnitelmaList[koulutussuunnitelmaList.size - 1]
        Assertions.assertThat(testKoulutussuunnitelma.motivaatiokirje)
            .isEqualTo(UPDATED_MOTIVAATIOKIRJE)
        Assertions.assertThat(testKoulutussuunnitelma.motivaatiokirjeYksityinen)
            .isEqualTo(UPDATED_MOTIVAATIOKIRJE_YKSITYINEN)
        Assertions.assertThat(testKoulutussuunnitelma.opiskeluJaTyohistoria)
            .isEqualTo(UPDATED_OPISKELU_JA_TYOHISTORIA)
        Assertions.assertThat(testKoulutussuunnitelma.opiskeluJaTyohistoriaYksityinen)
            .isEqualTo(UPDATED_OPISKELU_JA_TYOHISTORIA_YKSITYINEN)
        Assertions.assertThat(testKoulutussuunnitelma.vahvuudet).isEqualTo(UPDATED_VAHVUUDET)
        Assertions.assertThat(testKoulutussuunnitelma.vahvuudetYksityinen)
            .isEqualTo(UPDATED_VAHVUUDET_YKSITYINEN)
        Assertions.assertThat(testKoulutussuunnitelma.tulevaisuudenVisiointi)
            .isEqualTo(UPDATED_TULEVAISUUDEN_VISIOINTI)
        Assertions.assertThat(testKoulutussuunnitelma.tulevaisuudenVisiointiYksityinen)
            .isEqualTo(UPDATED_TULEVAISUUDEN_VISIOINTI_YKSITYINEN)
        Assertions.assertThat(testKoulutussuunnitelma.osaamisenKartuttaminen)
            .isEqualTo(UPDATED_OSAAMISEN_KARTUTTAMINEN)
        Assertions.assertThat(testKoulutussuunnitelma.osaamisenKartuttaminenYksityinen)
            .isEqualTo(UPDATED_OSAAMISEN_KARTUTTAMINEN_YKSITYINEN)
        Assertions.assertThat(testKoulutussuunnitelma.elamankentta).isEqualTo(UPDATED_ELAMANKENTTA)
        Assertions.assertThat(testKoulutussuunnitelma.elamankenttaYksityinen)
            .isEqualTo(UPDATED_ELAMANKENTTA_YKSITYINEN)
        Assertions.assertThat(testKoulutussuunnitelma.koulutussuunnitelmaAsiakirja).isNotNull
        val koulutussuunnitelmaBlob =
            testKoulutussuunnitelma.koulutussuunnitelmaAsiakirja?.asiakirjaData?.data
        Assertions.assertThat(
            koulutussuunnitelmaBlob?.getBytes(
                1,
                koulutussuunnitelmaBlob.length().toInt()
            )
        )
            .isEqualTo(DEFAULT_FILE)
        Assertions.assertThat(testKoulutussuunnitelma.motivaatiokirjeAsiakirja).isNotNull
        val motivaatiokirjeBlob =
            testKoulutussuunnitelma.motivaatiokirjeAsiakirja?.asiakirjaData?.data
        Assertions.assertThat(
            motivaatiokirjeBlob?.getBytes(
                1,
                motivaatiokirjeBlob.length().toInt()
            )
        )
            .isEqualTo(DEFAULT_FILE)
    }

    fun initTest(userId: String? = null) {
        user = KayttajaResourceWithMockUserIT.createEntity()
        em.persist(user)
        em.flush()
        val userDetails = mapOf<String, List<Any>>(
        )
        val authorities = listOf(SimpleGrantedAuthority(ERIKOISTUVA_LAAKARI))
        val authentication = Saml2Authentication(
            DefaultSaml2AuthenticatedPrincipal(userId ?: user.id, userDetails),
            "test",
            authorities
        )
        TestSecurityContextHolder.getContext().authentication = authentication

        koulutussuunnitelma = KoulutussuunnitelmaHelper.createEntity(em, user)
    }

}
