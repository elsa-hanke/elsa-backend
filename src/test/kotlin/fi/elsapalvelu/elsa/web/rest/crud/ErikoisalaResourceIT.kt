package fi.elsapalvelu.elsa.web.rest.crud

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import fi.elsapalvelu.elsa.domain.Erikoisala
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager

@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class ErikoisalaResourceIT {

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private val DEFAULT_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLO_ALKAA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_VOIMASSAOLO_PAATTYY: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLO_PAATTYY: LocalDate = LocalDate.now(ZoneId.systemDefault())

        @JvmStatic
        fun createEntity(em: EntityManager): Erikoisala {
            val erikoisala = Erikoisala(
                nimi = DEFAULT_NIMI,
                voimassaoloAlkaa = DEFAULT_VOIMASSAOLO_ALKAA,
                voimassaoloPaattyy = DEFAULT_VOIMASSAOLO_PAATTYY
            )

            return erikoisala
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Erikoisala {
            val erikoisala = Erikoisala(
                nimi = UPDATED_NIMI,
                voimassaoloAlkaa = UPDATED_VOIMASSAOLO_ALKAA,
                voimassaoloPaattyy = UPDATED_VOIMASSAOLO_PAATTYY
            )

            return erikoisala
        }
    }
}
