package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Erikoisala
import fi.elsapalvelu.elsa.domain.Oppimistavoite
import fi.elsapalvelu.elsa.domain.OppimistavoitteenKategoria
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager

class OppimistavoiteHelper {

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private val DEFAULT_VOIMASSAOLON_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLON_ALKAMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_VOIMASSAOLON_PAATTYMISPAIVA: LocalDate = LocalDate.ofEpochDay(30L)
        private val UPDATED_VOIMASSAOLON_PAATTYMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        @JvmStatic
        fun createEntity(em: EntityManager, erikoisala: Erikoisala? = null, voimassaoloAlkaa: LocalDate? = DEFAULT_VOIMASSAOLON_ALKAMISPAIVA,
                         voimassaoloPaattyy: LocalDate? = DEFAULT_VOIMASSAOLON_PAATTYMISPAIVA): Oppimistavoite {
            val oppimistavoite = Oppimistavoite(
                nimi = DEFAULT_NIMI,
                voimassaolonAlkamispaiva = voimassaoloAlkaa,
                voimassaolonPaattymispaiva = voimassaoloPaattyy
            )

            // Lisätään pakollinen tieto
            val oppimistavoitteenKategoria: OppimistavoitteenKategoria
            if (em.findAll(OppimistavoitteenKategoria::class).isEmpty()) {
                oppimistavoitteenKategoria = OppimistavoitteenKategoriaHelper.createEntity(em, erikoisala)
                em.persist(oppimistavoitteenKategoria)
                em.flush()
            } else {
                oppimistavoitteenKategoria = em.findAll(OppimistavoitteenKategoria::class).get(0)
            }
            oppimistavoite.kategoria = oppimistavoitteenKategoria
            return oppimistavoite
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): Oppimistavoite {
            val oppimistavoite = Oppimistavoite(
                nimi = UPDATED_NIMI,
                voimassaolonAlkamispaiva = UPDATED_VOIMASSAOLON_ALKAMISPAIVA,
                voimassaolonPaattymispaiva = UPDATED_VOIMASSAOLON_PAATTYMISPAIVA
            )

            // Lisätään pakollinen tieto
            val oppimistavoitteenKategoria: OppimistavoitteenKategoria
            if (em.findAll(OppimistavoitteenKategoria::class).isEmpty()) {
                oppimistavoitteenKategoria = OppimistavoitteenKategoriaHelper.createUpdatedEntity(em)
                em.persist(oppimistavoitteenKategoria)
                em.flush()
            } else {
                oppimistavoitteenKategoria = em.findAll(OppimistavoitteenKategoria::class).get(0)
            }
            oppimistavoite.kategoria = oppimistavoitteenKategoria
            return oppimistavoite
        }
    }
}
