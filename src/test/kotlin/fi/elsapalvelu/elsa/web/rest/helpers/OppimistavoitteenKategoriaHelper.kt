package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Erikoisala
import fi.elsapalvelu.elsa.domain.OppimistavoitteenKategoria
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager

class OppimistavoitteenKategoriaHelper {

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        private val DEFAULT_VOIMASSAOLON_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        private val UPDATED_VOIMASSAOLON_ALKAMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        private val DEFAULT_VOIMASSAOLON_PAATTYMISPAIVA: LocalDate = LocalDate.ofEpochDay(30L)
        private val UPDATED_VOIMASSAOLON_PAATTYMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        @JvmStatic
        fun createEntity(em: EntityManager, erikoisala: Erikoisala? = null, voimassaoloAlkaa: LocalDate? = DEFAULT_VOIMASSAOLON_ALKAMISPAIVA,
                         voimassaoloPaattyy: LocalDate? = DEFAULT_VOIMASSAOLON_PAATTYMISPAIVA): OppimistavoitteenKategoria {
            val oppimistavoitteenKategoria = OppimistavoitteenKategoria(
                nimi = DEFAULT_NIMI,
                voimassaolonAlkamispaiva = voimassaoloAlkaa,
                voimassaolonPaattymispaiva = voimassaoloPaattyy
            )

            erikoisala?.let {
                oppimistavoitteenKategoria.erikoisala = erikoisala
                return oppimistavoitteenKategoria
            }

            val newErikoisala = ErikoisalaHelper.createEntity()
            em.persist(newErikoisala)
            em.flush()

            oppimistavoitteenKategoria.erikoisala = newErikoisala
            return oppimistavoitteenKategoria
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): OppimistavoitteenKategoria {
            val oppimistavoitteenKategoria = OppimistavoitteenKategoria(
                nimi = UPDATED_NIMI,
                voimassaolonAlkamispaiva = UPDATED_VOIMASSAOLON_ALKAMISPAIVA,
                voimassaolonPaattymispaiva = UPDATED_VOIMASSAOLON_PAATTYMISPAIVA
            )

            // Lisätään pakollinen tieto
            val erikoisala: Erikoisala
            if (em.findAll(Erikoisala::class).isEmpty()) {
                erikoisala = ErikoisalaHelper.createUpdatedEntity()
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
