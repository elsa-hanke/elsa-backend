package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Erikoisala
import fi.elsapalvelu.elsa.domain.SuoritteenKategoria
import fi.elsapalvelu.elsa.web.rest.findAll
import jakarta.persistence.EntityManager

object SuoritteenKategoriaHelper {

    private const val DEFAULT_NIMI = "AAAAAAAAAA"
    private const val UPDATED_NIMI = "BBBBBBBBBB"

    fun createEntity(
        em: EntityManager,
        existingErikoisala: Erikoisala? = null
    ): SuoritteenKategoria {

        val suoritteenKategoria = SuoritteenKategoria(
            nimi = DEFAULT_NIMI
        )

        suoritteenKategoria.erikoisala =
            existingErikoisala ?: resolveErikoisala(em, updated = false)

        return suoritteenKategoria
    }

    fun createUpdatedEntity(em: EntityManager): SuoritteenKategoria {

        val suoritteenKategoria = SuoritteenKategoria(
            nimi = UPDATED_NIMI
        )

        suoritteenKategoria.erikoisala =
            resolveErikoisala(em, updated = true)

        return suoritteenKategoria
    }

    private fun resolveErikoisala(
        em: EntityManager,
        updated: Boolean
    ): Erikoisala {
        val existing = em.findAll(Erikoisala::class).firstOrNull()

        if (existing != null) return existing

        val created =
            if (updated) {
                ErikoisalaHelper.createUpdatedEntity()
            } else {
                ErikoisalaHelper.createEntity()
            }

        em.persist(created)
        em.flush()

        return created
    }
}
