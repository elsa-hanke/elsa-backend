package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Keskeytysaika
import fi.elsapalvelu.elsa.domain.PoissaolonSyy
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.LocalDate
import java.time.ZoneId
import javax.persistence.EntityManager

class KeskeytysaikaHelper {

    companion object {

        val DEFAULT_ALKAMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        val UPDATED_ALKAMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        val DEFAULT_PAATTYMISPAIVA: LocalDate = LocalDate.ofEpochDay(0L)
        val UPDATED_PAATTYMISPAIVA: LocalDate = LocalDate.now(ZoneId.systemDefault())

        const val DEFAULT_OSAAIKAPROSENTTI: Int = 0
        const val UPDATED_OSAAIKAPROSENTTI: Int = 1

        @JvmStatic
        fun createEntity(em: EntityManager, tyoskentelyjakso: Tyoskentelyjakso): Keskeytysaika {
            val keskeytysaika = Keskeytysaika(
                alkamispaiva = DEFAULT_ALKAMISPAIVA,
                paattymispaiva = DEFAULT_PAATTYMISPAIVA,
                osaaikaprosentti = DEFAULT_OSAAIKAPROSENTTI
            )

            // Add required entity
            val poissaolonSyy: PoissaolonSyy
            if (em.findAll(PoissaolonSyy::class).isEmpty()) {
                poissaolonSyy = PoissaolonSyyHelper.createEntity()
                em.persist(poissaolonSyy)
                em.flush()
            } else {
                poissaolonSyy = em.findAll(PoissaolonSyy::class).get(0)
            }
            keskeytysaika.poissaolonSyy = poissaolonSyy

            keskeytysaika.tyoskentelyjakso = tyoskentelyjakso

            return keskeytysaika
        }

        @JvmStatic
        fun createUpdatedEntity(em: EntityManager, tyoskentelyjakso: Tyoskentelyjakso): Keskeytysaika {
            val keskeytysaika = Keskeytysaika(
                alkamispaiva = UPDATED_ALKAMISPAIVA,
                paattymispaiva = UPDATED_PAATTYMISPAIVA,
                osaaikaprosentti = UPDATED_OSAAIKAPROSENTTI
            )

            // Add required entity
            val poissaolonSyy: PoissaolonSyy
            if (em.findAll(PoissaolonSyy::class).isEmpty()) {
                poissaolonSyy = PoissaolonSyyHelper.createUpdatedEntity()
                em.persist(poissaolonSyy)
                em.flush()
            } else {
                poissaolonSyy = em.findAll(PoissaolonSyy::class).get(0)
            }
            keskeytysaika.poissaolonSyy = poissaolonSyy

            keskeytysaika.tyoskentelyjakso = tyoskentelyjakso

            return keskeytysaika
        }
    }
}
