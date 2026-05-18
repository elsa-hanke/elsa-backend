package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Keskeytysaika
import fi.elsapalvelu.elsa.domain.PoissaolonSyy
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.LocalDate
import jakarta.persistence.EntityManager

object KeskeytysaikaHelper {

    val DEFAULT_ALKAMISPAIVA: LocalDate = LocalDate.of(2020, 1, 5)
    val UPDATED_ALKAMISPAIVA: LocalDate = LocalDate.of(2020, 1, 20)

    val DEFAULT_PAATTYMISPAIVA: LocalDate = LocalDate.of(2020, 1, 10)
    val UPDATED_PAATTYMISPAIVA: LocalDate = LocalDate.of(2020, 1, 25)

    const val DEFAULT_POISSAOLOPROSENTTI: Int = 50
    const val UPDATED_POISSAOLOPROSENTTI: Int = 60

    fun createEntity(
        em: EntityManager,
        tyoskentelyjakso: Tyoskentelyjakso
    ): Keskeytysaika {
        val keskeytysaika = Keskeytysaika(
            alkamispaiva = DEFAULT_ALKAMISPAIVA,
            paattymispaiva = DEFAULT_PAATTYMISPAIVA,
            poissaoloprosentti = DEFAULT_POISSAOLOPROSENTTI
        )

        val poissaolonSyy = getOrCreatePoissaolonSyy(em) {
            PoissaolonSyyHelper.createEntity()
        }

        keskeytysaika.poissaolonSyy = poissaolonSyy
        keskeytysaika.tyoskentelyjakso = tyoskentelyjakso

        return keskeytysaika
    }

    fun createUpdatedEntity(
        em: EntityManager,
        tyoskentelyjakso: Tyoskentelyjakso
    ): Keskeytysaika {
        val keskeytysaika = Keskeytysaika(
            alkamispaiva = UPDATED_ALKAMISPAIVA,
            paattymispaiva = UPDATED_PAATTYMISPAIVA,
            poissaoloprosentti = UPDATED_POISSAOLOPROSENTTI
        )

        val poissaolonSyy = getOrCreatePoissaolonSyy(em) {
            PoissaolonSyyHelper.createUpdatedEntity()
        }

        keskeytysaika.poissaolonSyy = poissaolonSyy
        keskeytysaika.tyoskentelyjakso = tyoskentelyjakso

        return keskeytysaika
    }

    private fun getOrCreatePoissaolonSyy(
        em: EntityManager,
        create: () -> PoissaolonSyy
    ): PoissaolonSyy {
        val existing = em.findAll(PoissaolonSyy::class)

        return if (existing.isEmpty()) {
            create().also {
                em.persist(it)
                em.flush()
            }
        } else {
            existing.first()
        }
    }
}
