package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.suoritteet.SuoritteenKategoria
import jakarta.persistence.EntityManager

object SuoritteenKategoriaHelper {

    private const val DEFAULT_NIMI = "AAAAAAAAAA"
    private const val UPDATED_NIMI = "BBBBBBBBBB"

    fun createEntity(
        em: EntityManager,
        erikoisala: Erikoisala? = null
    ): SuoritteenKategoria {

        val suoritteenKategoria = SuoritteenKategoria(
            nimi = DEFAULT_NIMI
        )

        suoritteenKategoria.erikoisala =
            erikoisala ?: luoErikoisala(em, updated = false)

        return suoritteenKategoria
    }

    fun createUpdatedEntity(em: EntityManager, erikoisala: Erikoisala? = null): SuoritteenKategoria {

        val suoritteenKategoria = SuoritteenKategoria(
            nimi = UPDATED_NIMI
        )

        suoritteenKategoria.erikoisala =
            erikoisala ?: luoErikoisala(em, updated = true)

        return suoritteenKategoria
    }

    private fun luoErikoisala(
        em: EntityManager,
        updated: Boolean
    ): Erikoisala {
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
