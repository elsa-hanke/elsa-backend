package fi.elsapalvelu.elsa.service.impl.tyoskentely

import fi.elsapalvelu.elsa.domain.tyoskentely.Keskeytysaika
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.repository.tyoskentely.KeskeytysaikaRepository
import fi.elsapalvelu.elsa.repository.tyoskentely.TyoskentelyjaksoRepository
import jakarta.validation.ValidationException
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class KeskeytysaikaServiceImplDeleteTest {
    private val opintooikeusId = 5L
    private val keskeytysaikaRepository = mock<KeskeytysaikaRepository>()
    private val tyoskentelyjaksoRepository = mock<TyoskentelyjaksoRepository>()
    private val service = KeskeytysaikaServiceImpl(keskeytysaikaRepository, tyoskentelyjaksoRepository, mock())

    @Test
    fun `poisto tarkistaa keskeytysaikaan kuuluvan tyoskentelyjakson eika samaa id-arvoa kayttavaa jaksoa`() {
        // Keskeytysaika 20 kuuluu jaksoon 10, kun taas jakso 20 on liitetty terveyskeskuskoulutusjaksoon.
        val tavallinenJakso = Tyoskentelyjakso(id = 10L)
        whenever(keskeytysaikaRepository.findOneByIdAndTyoskentelyjaksoOpintooikeusId(20L, opintooikeusId))
            .thenReturn(Keskeytysaika(id = 20L, tyoskentelyjakso = tavallinenJakso))
        whenever(tyoskentelyjaksoRepository.findOneByIdAndOpintooikeusId(10L, opintooikeusId)).thenReturn(tavallinenJakso)
        whenever(tyoskentelyjaksoRepository.findOneByIdAndOpintooikeusId(20L, opintooikeusId))
            .thenReturn(Tyoskentelyjakso(id = 20L, liitettyTerveyskeskuskoulutusjaksoon = true))

        service.delete(20L, opintooikeusId)

        verify(keskeytysaikaRepository).deleteById(20L)
    }

    @Test
    fun `liitetyn tyoskentelyjakson keskeytysaikaa ei voi poistaa`() {
        val liitettyJakso = Tyoskentelyjakso(id = 10L, liitettyTerveyskeskuskoulutusjaksoon = true)
        whenever(keskeytysaikaRepository.findOneByIdAndTyoskentelyjaksoOpintooikeusId(20L, opintooikeusId))
            .thenReturn(Keskeytysaika(id = 20L, tyoskentelyjakso = liitettyJakso))
        whenever(tyoskentelyjaksoRepository.findOneByIdAndOpintooikeusId(10L, opintooikeusId)).thenReturn(liitettyJakso)

        assertThatThrownBy { service.delete(20L, opintooikeusId) }.isInstanceOf(ValidationException::class.java)

        verify(keskeytysaikaRepository, never()).deleteById(any())
    }
}
