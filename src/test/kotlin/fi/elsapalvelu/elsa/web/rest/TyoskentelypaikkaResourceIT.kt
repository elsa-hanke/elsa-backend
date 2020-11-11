package fi.elsapalvelu.elsa.web.rest

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import fi.elsapalvelu.elsa.domain.Tyoskentelypaikka
import fi.elsapalvelu.elsa.domain.enumeration.TyoskentelyjaksoTyyppi
import fi.elsapalvelu.elsa.service.mapper.TyoskentelypaikkaMapper
import fi.elsapalvelu.elsa.web.rest.errors.ExceptionTranslator
import javax.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.mockito.MockitoAnnotations
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.validation.Validator


@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class TyoskentelypaikkaResourceIT {

    @Autowired
    private lateinit var tyoskentelypaikkaMapper: TyoskentelypaikkaMapper

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

    private lateinit var restTyoskentelypaikkaMockMvc: MockMvc

    private lateinit var tyoskentelypaikka: Tyoskentelypaikka

    @BeforeEach
    fun initTest() {
        tyoskentelypaikka = createEntity(em)
    }

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private const val DEFAULT_KUNTA = "AAAAAAAAAA"
        private const val UPDATED_KUNTA = "BBBBBBBBBB"

        private val DEFAULT_TYYPPI: TyoskentelyjaksoTyyppi = TyoskentelyjaksoTyyppi.TERVEYSKESKUS
        private val UPDATED_TYYPPI: TyoskentelyjaksoTyyppi = TyoskentelyjaksoTyyppi.YLIOPISTOLLINEN_SAIRAALA

        @JvmStatic
        fun createEntity(em: EntityManager): Tyoskentelypaikka {
            val tyoskentelypaikka = Tyoskentelypaikka(
                nimi = DEFAULT_NIMI,
                kunta = DEFAULT_KUNTA,
                tyyppi = DEFAULT_TYYPPI
            )

            return tyoskentelypaikka
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Tyoskentelypaikka {
            val tyoskentelypaikka = Tyoskentelypaikka(
                nimi = UPDATED_NIMI,
                kunta = UPDATED_KUNTA,
                tyyppi = UPDATED_TYYPPI
            )

            return tyoskentelypaikka
        }
    }
}
