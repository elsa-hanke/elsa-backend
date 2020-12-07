package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.Keskeytysaika
import fi.elsapalvelu.elsa.domain.PoissaolonSyy
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.web.rest.findAll
import java.time.LocalDate
import javax.persistence.EntityManager

class KeskeytysaikaHelper {

    companion object {

        val DEFAULT_ALKAMISPAIVA: LocalDate = LocalDate.of(2020, 1, 5)
        val UPDATED_ALKAMISPAIVA: LocalDate = LocalDate.of(2020, 1, 20)

        val DEFAULT_PAATTYMISPAIVA: LocalDate = LocalDate.of(2020, 1, 10)
        val UPDATED_PAATTYMISPAIVA: LocalDate = LocalDate.of(2020, 1, 25)

        const val DEFAULT_OSAAIKAPROSENTTI: Int = 50
        const val UPDATED_OSAAIKAPROSENTTI: Int = 60

        @JvmStatic
        fun createEntity(em: EntityManager, tyoskentelyjakso: Tyoskentelyjakso): Keskeytysaika {
            val keskeytysaika = Keskeytysaika(
                alkamispaiva = DEFAULT_ALKAMISPAIVA,
                paattymispaiva = DEFAULT_PAATTYMISPAIVA,
                osaaikaprosentti = DEFAULT_OSAAIKAPROSENTTI
            )

            // Lisätään pakollinen tieto
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

            // Lisätään pakollinen tieto
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
