package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Erikoisala
import fi.elsapalvelu.elsa.domain.SuoritteenKategoria
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.LocalDate
import java.time.ZoneId
import jakarta.persistence.EntityManager

class SuoritteenKategoriaHelper {

    companion object {

        private const val DEFAULT_NIMI = "AAAAAAAAAA"
        private const val UPDATED_NIMI = "BBBBBBBBBB"

        @JvmStatic
        fun createEntity(
            em: EntityManager,
            existingErikoisala: Erikoisala? = null
        ): SuoritteenKategoria {
            val suoritteenKategoria = SuoritteenKategoria(
                nimi = DEFAULT_NIMI
            )

            var erikoisala = existingErikoisala
            if (erikoisala == null) {
                if (em.findAll(Erikoisala::class).isEmpty()) {
                    erikoisala = ErikoisalaHelper.createEntity()
                    em.persist(erikoisala)
                    em.flush()
                } else {
                    erikoisala = em.findAll(Erikoisala::class)[0]
                }
            }
            suoritteenKategoria.erikoisala = erikoisala

            return suoritteenKategoria
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager): SuoritteenKategoria {
            val suoritteenKategoria = SuoritteenKategoria(
                nimi = UPDATED_NIMI
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
            suoritteenKategoria.erikoisala = erikoisala
            return suoritteenKategoria
        }
    }
}
