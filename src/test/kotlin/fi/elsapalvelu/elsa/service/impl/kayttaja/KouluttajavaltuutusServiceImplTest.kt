package fi.elsapalvelu.elsa.service.impl.kayttaja

import fi.elsapalvelu.elsa.repository.kayttaja.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KouluttajavaltuutusRepository
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.mapper.kayttaja.KouluttajavaltuutusMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.util.Optional

@ExtendWith(MockitoExtension::class)
class KouluttajavaltuutusServiceImplTest {

    @Mock
    private lateinit var kouluttajavaltuutusRepository: KouluttajavaltuutusRepository

    @Mock
    private lateinit var kouluttajavaltuutusMapper: KouluttajavaltuutusMapper

    @Mock
    private lateinit var erikoistuvaLaakariRepository: ErikoistuvaLaakariRepository

    @Mock
    private lateinit var kayttajaRepository: KayttajaRepository

    @Mock
    private lateinit var mailService: MailService

    @InjectMocks
    private lateinit var service: KouluttajavaltuutusServiceImpl

    @Test
    fun `adding authorization does nothing when authorized user does not exist`() {
        whenever(kayttajaRepository.findById(123L)).thenReturn(Optional.empty())

        service.lisaaValtuutus("erikoistuva-user", 123L)

        verify(kouluttajavaltuutusRepository, never()).save(any())
        verifyNoInteractions(kouluttajavaltuutusMapper, erikoistuvaLaakariRepository, mailService)
    }
}
