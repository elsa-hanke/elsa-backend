package fi.elsapalvelu.elsa.service.valmistuminen

import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.service.dto.suoritteet.VanhentuneetSuorituksetDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.UusiValmistumispyyntoDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoDTO
import fi.elsapalvelu.elsa.service.kayttaja.ErikoistuvaLaakariService
import fi.elsapalvelu.elsa.service.kayttaja.FileValidationService
import fi.elsapalvelu.elsa.web.rest.VALMISTUMISPYYNTO_ENTITY_NAME
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockMultipartFile
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ValmistumispyyntoApplicationServiceTest {

    @Mock
    private lateinit var valmistumispyyntoService: ValmistumispyyntoService

    @Mock
    private lateinit var erikoistuvaLaakariService: ErikoistuvaLaakariService

    @Mock
    private lateinit var fileValidationService: FileValidationService

    private lateinit var applicationService: ValmistumispyyntoApplicationService

    @BeforeEach
    fun setup() {
        applicationService = ValmistumispyyntoApplicationService(
            valmistumispyyntoService,
            erikoistuvaLaakariService,
            fileValidationService
        )
        whenever(valmistumispyyntoService.findErikoisalaTyyppiByOpintooikeusId(OPINTOOIKEUS_ID))
            .thenReturn(ErikoisalaTyyppi.LAAKETIEDE)
        whenever(
            valmistumispyyntoService.findSuoritustenTila(
                OPINTOOIKEUS_ID,
                ErikoisalaTyyppi.LAAKETIEDE
            )
        ).thenReturn(VanhentuneetSuorituksetDTO(false, false))
    }

    @Test
    fun findSuoritustenTilaShouldComposeSpecialtyAndExpirationState() {
        whenever(
            valmistumispyyntoService.findSuoritustenTila(
                OPINTOOIKEUS_ID,
                ErikoisalaTyyppi.LAAKETIEDE
            )
        ).thenReturn(VanhentuneetSuorituksetDTO(true, false))

        val result = applicationService.findSuoritustenTila(OPINTOOIKEUS_ID)

        assertThat(result.erikoisalaTyyppi).isEqualTo(ErikoisalaTyyppi.LAAKETIEDE)
        assertThat(result.vanhojaTyoskentelyjaksojaOrSuorituksiaExists).isTrue
        assertThat(result.kuulusteluVanhentunut).isFalse
    }

    @Test
    fun createShouldUpdateLicensingDataAndCreateRequestAfterValidation() {
        val request = UusiValmistumispyyntoDTO()
        val expected = ValmistumispyyntoDTO(id = 1L)
        stubLicensingDataExists()
        whenever(valmistumispyyntoService.create(OPINTOOIKEUS_ID, request)).thenReturn(expected)

        val result = applicationService.create(USER_ID, OPINTOOIKEUS_ID, request, null)

        assertThat(result).isSameAs(expected)
        verify(erikoistuvaLaakariService).updateLaillistamispaiva(USER_ID, null, null, null, null)
        verify(valmistumispyyntoService).create(OPINTOOIKEUS_ID, request)
    }

    @Test
    fun createShouldRequireLicensingDateAndCertificateWhenNotStored() {
        whenever(erikoistuvaLaakariService.laillistamispaivaAndTodistusExists(USER_ID))
            .thenReturn(false)

        assertErrorKey("dataillegal.laillistamispaiva-ja-todistus-vaaditaan") {
            applicationService.create(USER_ID, OPINTOOIKEUS_ID, UusiValmistumispyyntoDTO(), null)
        }

        verify(valmistumispyyntoService, never()).create(any(), any())
        verifyLicensingDataWasNotUpdated()
    }

    @Test
    fun createShouldRequireExplanationForExpiredStudies() {
        stubLicensingDataExists()
        whenever(
            valmistumispyyntoService.findSuoritustenTila(
                OPINTOOIKEUS_ID,
                ErikoisalaTyyppi.LAAKETIEDE
            )
        ).thenReturn(VanhentuneetSuorituksetDTO(true, false))

        assertErrorKey("dataillegal.selvitys-vanhentuneista-suorituksista-vaaditaan") {
            applicationService.create(USER_ID, OPINTOOIKEUS_ID, UusiValmistumispyyntoDTO(), null)
        }

        verify(valmistumispyyntoService, never()).create(any(), any())
        verifyLicensingDataWasNotUpdated()
    }

    @Test
    fun createShouldRejectExistingRequest() {
        stubLicensingDataExists()
        whenever(valmistumispyyntoService.existsByOpintooikeusId(OPINTOOIKEUS_ID)).thenReturn(true)

        assertErrorKey("dataillegal.valmistumispyynto-on-jo-lahetetty") {
            applicationService.create(USER_ID, OPINTOOIKEUS_ID, UusiValmistumispyyntoDTO(), null)
        }

        verify(valmistumispyyntoService, never()).create(any(), any())
        verifyLicensingDataWasNotUpdated()
    }

    @Test
    fun createShouldRejectInvalidCertificate() {
        val certificate = MockMultipartFile(
            "laillistamistodistus",
            "licence.exe",
            "application/x-msdownload",
            byteArrayOf(1)
        )
        val request = UusiValmistumispyyntoDTO(laillistamispaiva = LocalDate.of(2024, 1, 1))
        whenever(fileValidationService.validate(listOf(certificate))).thenReturn(false)

        assertErrorKey("dataillegal.tiedosto-ei-ole-kelvollinen") {
            applicationService.create(USER_ID, OPINTOOIKEUS_ID, request, certificate)
        }

        verify(valmistumispyyntoService, never()).create(any(), any())
        verifyLicensingDataWasNotUpdated()
    }

    @Test
    fun updateWhenNotSentShouldRejectRequestThatHasAlreadyBeenSent() {
        stubLicensingDataExists()
        whenever(valmistumispyyntoService.onkoLahetetty(OPINTOOIKEUS_ID)).thenReturn(true)

        assertErrorKey("dataillegal.lahetettya-valmistumispyyntoa-ei-saa-muokata") {
            applicationService.updateWhenNotSent(
                USER_ID,
                OPINTOOIKEUS_ID,
                UusiValmistumispyyntoDTO(),
                null
            )
        }

        verify(valmistumispyyntoService, never()).update(any(), any())
        verifyLicensingDataWasNotUpdated()
    }

    @Test
    fun updateShouldPreserveUpdatesWithoutApplyingSentRequestPolicy() {
        val request = UusiValmistumispyyntoDTO()
        val expected = ValmistumispyyntoDTO(id = 1L)
        stubLicensingDataExists()
        whenever(valmistumispyyntoService.update(OPINTOOIKEUS_ID, request)).thenReturn(expected)

        val result = applicationService.update(USER_ID, OPINTOOIKEUS_ID, request, null)

        assertThat(result).isSameAs(expected)
        verify(valmistumispyyntoService, never()).onkoLahetetty(any())
        verify(valmistumispyyntoService).update(OPINTOOIKEUS_ID, request)
    }

    @Test
    fun updateShouldValidateAndStoreCertificateBeforeUpdatingRequest() {
        val certificate = MockMultipartFile(
            "laillistamistodistus",
            "licence.pdf",
            "application/pdf",
            byteArrayOf(1, 2, 3)
        )
        val request = UusiValmistumispyyntoDTO(laillistamispaiva = LocalDate.of(2024, 1, 1))
        val expected = ValmistumispyyntoDTO(id = 1L)
        whenever(fileValidationService.validate(listOf(certificate))).thenReturn(true)
        whenever(valmistumispyyntoService.update(OPINTOOIKEUS_ID, request)).thenReturn(expected)

        val result = applicationService.update(USER_ID, OPINTOOIKEUS_ID, request, certificate)

        assertThat(result).isSameAs(expected)
        verify(fileValidationService).validate(listOf(certificate))
        verify(erikoistuvaLaakariService).updateLaillistamispaiva(
            USER_ID,
            LocalDate.of(2024, 1, 1),
            byteArrayOf(1, 2, 3),
            "licence.pdf",
            "application/pdf"
        )
        verify(valmistumispyyntoService).update(OPINTOOIKEUS_ID, request)
    }

    private fun assertErrorKey(expectedErrorKey: String, action: () -> Unit) {
        val exception = assertThrows<BadRequestAlertException>(action)

        assertThat(exception.entityName).isEqualTo(VALMISTUMISPYYNTO_ENTITY_NAME)
        assertThat(exception.errorKey).isEqualTo(expectedErrorKey)
    }

    private fun stubLicensingDataExists() {
        whenever(erikoistuvaLaakariService.laillistamispaivaAndTodistusExists(USER_ID))
            .thenReturn(true)
    }

    private fun verifyLicensingDataWasNotUpdated() {
        verify(erikoistuvaLaakariService, never()).updateLaillistamispaiva(
            any(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull()
        )
    }

    companion object {
        private const val USER_ID = "user-1"
        private const val OPINTOOIKEUS_ID = 10L
    }
}
