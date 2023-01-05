package fi.elsapalvelu.elsa.service.helpers

import fi.elsapalvelu.elsa.domain.Keskeytysaika
import fi.elsapalvelu.elsa.domain.PoissaolonSyy
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.enumeration.PoissaolonSyyTyyppi
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.time.LocalDate

class KeskeytysaikaMockHelper {

    companion object {
        @JvmStatic
        fun createKeskeytysaikaWithTyoskentelyjaksoMock(
            id: Long?,
            alkamispaiva: LocalDate?,
            paattymispaiva: LocalDate?,
            poissaoloprosentti: Int = 100,
            poissaolonSyyTyyppi: PoissaolonSyyTyyppi = PoissaolonSyyTyyppi.VAHENNETAAN_SUORAAN
        ): Keskeytysaika {
            val poissaolonSyy = PoissaolonSyy(
                null,
                "",
                poissaolonSyyTyyppi,
                false,
                LocalDate.ofEpochDay(0L),
                null
            )
            val tyoskentelyjaksoMock = mock(Tyoskentelyjakso::class.java)
            val keskeytysaika = Keskeytysaika(
                id,
                alkamispaiva = alkamispaiva,
                paattymispaiva = paattymispaiva,
                poissaoloprosentti = poissaoloprosentti,
                poissaolonSyy = poissaolonSyy,
                tyoskentelyjakso = tyoskentelyjaksoMock
            )

            return keskeytysaika
        }

        @JvmStatic
        fun createKeskeytysaikaMock(
            id: Long?,
            alkamispaiva: LocalDate?,
            paattymispaiva: LocalDate?,
            poissaoloprosentti: Int = 100,
            poissaolonSyyTyyppi: PoissaolonSyyTyyppi = PoissaolonSyyTyyppi.VAHENNETAAN_SUORAAN,
            vahennetaanKerran: Boolean = false,
            poissaolonSyy: PoissaolonSyy? = null
        ): Keskeytysaika {
            val keskeytysaikaMock = mock(Keskeytysaika::class.java)
            `when`(keskeytysaikaMock.id).thenReturn(id)
            `when`(keskeytysaikaMock.alkamispaiva).thenReturn(alkamispaiva)
            `when`(keskeytysaikaMock.paattymispaiva).thenReturn(paattymispaiva)
            `when`(keskeytysaikaMock.poissaoloprosentti).thenReturn(poissaoloprosentti)
            if (poissaolonSyy != null) {
                `when`(keskeytysaikaMock.poissaolonSyy).thenReturn(poissaolonSyy)
            } else {
                val poissaolonSyyMock = mock(PoissaolonSyy::class.java)
                `when`(poissaolonSyyMock.vahennystyyppi).thenReturn(poissaolonSyyTyyppi)
                `when`(poissaolonSyyMock.vahennetaanKerran).thenReturn(vahennetaanKerran)
                `when`(keskeytysaikaMock.poissaolonSyy).thenReturn(poissaolonSyyMock)
            }

            return keskeytysaikaMock
        }
    }
}
