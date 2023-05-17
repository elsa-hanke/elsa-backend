package fi.elsapalvelu.elsa.service.helpers

import fi.elsapalvelu.elsa.domain.Keskeytysaika
import fi.elsapalvelu.elsa.domain.Opintooikeus
import fi.elsapalvelu.elsa.domain.Tyoskentelyjakso
import fi.elsapalvelu.elsa.domain.Tyoskentelypaikka
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.time.LocalDate

class TyoskentelyjaksoMockHelper {

    companion object {

        @JvmStatic
        fun createTyoskentelyjaksoMock(
            id: Long?,
            alkamispaiva: LocalDate?,
            paattymispaiva: LocalDate?,
            osaaikaprosentti: Int = 100,
            keskeytykset: MutableSet<Keskeytysaika> = mutableSetOf(),
            hyvaksyttyAiemmin: Boolean? = false
        ): Tyoskentelyjakso {
            val tyoskentelyjaksoMock = mock(Tyoskentelyjakso::class.java)
            `when`(tyoskentelyjaksoMock.id).thenReturn(id)
            `when`(tyoskentelyjaksoMock.alkamispaiva).thenReturn(alkamispaiva)
            `when`(tyoskentelyjaksoMock.paattymispaiva).thenReturn(paattymispaiva)
            `when`(tyoskentelyjaksoMock.osaaikaprosentti).thenReturn(osaaikaprosentti)
            `when`(tyoskentelyjaksoMock.keskeytykset).thenReturn(keskeytykset)
            `when`(tyoskentelyjaksoMock.tyoskentelypaikka).thenReturn(mock(Tyoskentelypaikka::class.java))
            `when`(tyoskentelyjaksoMock.opintooikeus).thenReturn(mock(Opintooikeus::class.java))
            `when`(tyoskentelyjaksoMock.hyvaksyttyAiempaanErikoisalaan).thenReturn(hyvaksyttyAiemmin)

            return tyoskentelyjaksoMock
        }

        @JvmStatic
        fun createTyoskentelyjaksoWithMockDependencies(
            id: Long?,
            alkamispaiva: LocalDate?,
            paattymispaiva: LocalDate?,
            osaaikaprosentti: Int = 100,
            keskeytykset: MutableSet<Keskeytysaika> = mutableSetOf()
        ): Tyoskentelyjakso {
            val tyoskentelyjakso = Tyoskentelyjakso(
                id = id,
                alkamispaiva = alkamispaiva,
                paattymispaiva = paattymispaiva,
                osaaikaprosentti = osaaikaprosentti,
                keskeytykset = keskeytykset
            )
            tyoskentelyjakso.tyoskentelypaikka = mock(Tyoskentelypaikka::class.java)
            tyoskentelyjakso.opintooikeus = mock(Opintooikeus::class.java)

            return tyoskentelyjakso
        }
    }
}
