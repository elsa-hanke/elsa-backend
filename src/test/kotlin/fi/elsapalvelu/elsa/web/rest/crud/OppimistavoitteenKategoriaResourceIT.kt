package fi.elsapalvelu.elsa.web.rest.crud

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.config.TestSecurityConfiguration
import fi.elsapalvelu.elsa.domain.Erikoisala
import fi.elsapalvelu.elsa.domain.OppimistavoitteenKategoria
import fi.elsapalvelu.elsa.web.rest.findAll
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.test.context.support.WithMockUser
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager

@SpringBootTest(classes = [ElsaBackendApp::class, TestSecurityConfiguration::class])
@AutoConfigureMockMvc
@WithMockUser
class OppimistavoitteenKategoriaResourceIT {

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private val DEFAULT_VOIMASSAOLON_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLON_ALKAMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_VOIMASSAOLON_PAATTYMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLON_PAATTYMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        @JvmStatic
        fun createEntity(em: EntityManager): OppimistavoitteenKategoria {
            val oppimistavoitteenKategoria = OppimistavoitteenKategoria(
                nimi = DEFAULT_NIMI,
                voimassaolonAlkamispaiva = DEFAULT_VOIMASSAOLON_ALKAMISPAIVA,
                voimassaolonPaattymispaiva = DEFAULT_VOIMASSAOLON_PAATTYMISPAIVA
            )

            // Add required entity
            val erikoisala: Erikoisala
            if (em.findAll(Erikoisala::class).isEmpty()) {
                erikoisala = ErikoisalaResourceIT.createEntity(em)
                em.persist(erikoisala)
                em.flush()
            } else {
                erikoisala = em.findAll(Erikoisala::class).get(0)
            }
            oppimistavoitteenKategoria.erikoisala = erikoisala
            return oppimistavoitteenKategoria
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): OppimistavoitteenKategoria {
            val oppimistavoitteenKategoria = OppimistavoitteenKategoria(
                nimi = UPDATED_NIMI,
                voimassaolonAlkamispaiva = UPDATED_VOIMASSAOLON_ALKAMISPAIVA,
                voimassaolonPaattymispaiva = UPDATED_VOIMASSAOLON_PAATTYMISPAIVA
            )

            // Add required entity
            val erikoisala: Erikoisala
            if (em.findAll(Erikoisala::class).isEmpty()) {
                erikoisala = ErikoisalaResourceIT.createUpdatedEntity(em)
                em.persist(erikoisala)
                em.flush()
            } else {
                erikoisala = em.findAll(Erikoisala::class).get(0)
            }
            oppimistavoitteenKategoria.erikoisala = erikoisala
            return oppimistavoitteenKategoria
        }
    }
}
