package fi.elsapalvelu.elsa.web.rest.helpers

import fi.elsapalvelu.elsa.domain.tyoskentely.Keskeytysaika
import fi.elsapalvelu.elsa.domain.tyoskentely.PoissaolonSyy
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
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
        tyoskentelyjakso: Tyoskentelyjakso,
        poissaolonSyy: PoissaolonSyy = PoissaolonSyyHelper.createEntity().also(em::persist)
    ): Keskeytysaika {
        val keskeytysaika = Keskeytysaika(
            alkamispaiva = DEFAULT_ALKAMISPAIVA,
            paattymispaiva = DEFAULT_PAATTYMISPAIVA,
            poissaoloprosentti = DEFAULT_POISSAOLOPROSENTTI
        )

        keskeytysaika.poissaolonSyy = poissaolonSyy
        keskeytysaika.tyoskentelyjakso = tyoskentelyjakso

        return keskeytysaika
    }

    fun createUpdatedEntity(
        em: EntityManager,
        tyoskentelyjakso: Tyoskentelyjakso,
        poissaolonSyy: PoissaolonSyy = PoissaolonSyyHelper.createUpdatedEntity().also(em::persist)
    ): Keskeytysaika {
        val keskeytysaika = Keskeytysaika(
            alkamispaiva = UPDATED_ALKAMISPAIVA,
            paattymispaiva = UPDATED_PAATTYMISPAIVA,
            poissaoloprosentti = UPDATED_POISSAOLOPROSENTTI
        )

        keskeytysaika.poissaolonSyy = poissaolonSyy
        keskeytysaika.tyoskentelyjakso = tyoskentelyjakso

        return keskeytysaika
    }

}
