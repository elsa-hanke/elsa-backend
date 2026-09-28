package fi.elsapalvelu.elsa.service.impl.kayttaja

import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kouluttajavaltuutus
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.repository.kayttaja.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KouluttajavaltuutusRepository
import fi.elsapalvelu.elsa.service.dto.kayttaja.KouluttajavaltuutusDTO
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.mapper.kayttaja.KouluttajavaltuutusMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
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
import org.springframework.security.access.AccessDeniedException
import java.time.Instant
import java.time.LocalDate
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
        verify(kouluttajavaltuutusMapper, never()).toEntity(any<KouluttajavaltuutusDTO>())
        verify(erikoistuvaLaakariRepository, never()).findOneByKayttajaUserId(any<String>())
        verifyNoInteractions(mailService)
    }

    @Test
    fun `updating another user's authorization is denied before changing the entity`() {
        val currentOpintooikeus = Opintooikeus(id = 1L, kaytossa = true)
        val otherOpintooikeus = Opintooikeus(id = 2L)
        val originalEndDate = LocalDate.of(2026, 12, 31)
        val originalModifiedAt = Instant.parse("2026-01-01T00:00:00Z")
        val existingAuthorization = Kouluttajavaltuutus(
            id = 123L,
            paattymispaiva = originalEndDate,
            valtuutuksenMuokkausaika = originalModifiedAt,
            valtuuttajaOpintooikeus = otherOpintooikeus
        )
        val request = KouluttajavaltuutusDTO(
            id = existingAuthorization.id,
            paattymispaiva = LocalDate.of(2027, 12, 31)
        )

        whenever(kouluttajavaltuutusMapper.toEntity(request)).thenReturn(
            Kouluttajavaltuutus(id = request.id, paattymispaiva = request.paattymispaiva)
        )
        whenever(erikoistuvaLaakariRepository.findOneByKayttajaUserId("current-user"))
            .thenReturn(ErikoistuvaLaakari(opintooikeudet = mutableSetOf(currentOpintooikeus)))
        whenever(kouluttajavaltuutusRepository.findById(existingAuthorization.id!!))
            .thenReturn(Optional.of(existingAuthorization))

        assertThrows(AccessDeniedException::class.java) {
            service.save("current-user", request)
        }

        assertEquals(originalEndDate, existingAuthorization.paattymispaiva)
        assertEquals(originalModifiedAt, existingAuthorization.valtuutuksenMuokkausaika)
        verify(kouluttajavaltuutusRepository, never()).save(any())
        verify(kouluttajavaltuutusMapper, never()).toDto(any<Kouluttajavaltuutus>())
        verifyNoInteractions(mailService)
    }
}
