package fi.elsapalvelu.elsa.service.impl.kayttaja

import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.repository.kayttaja.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.service.PdfContentValidator
import fi.elsapalvelu.elsa.service.PdfTestData
import fi.elsapalvelu.elsa.service.kayttaja.AsiakirjaService
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import org.apache.pdfbox.pdmodel.PDDocument
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import org.springframework.http.MediaType
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.stream.Stream
import kotlin.test.assertFailsWith

class LaillistamistodistusValidationTest {
    private val repository = mock<ErikoistuvaLaakariRepository>()
    private val asiakirjaService = mock<AsiakirjaService>()
    private val service = ErikoistuvaLaakariServiceImpl(
        erikoistuvaLaakariRepository = repository,
        erikoistuvaLaakariMapper = mock(),
        erikoistuvaLaakariQueryService = mock(),
        yliopistoService = mock(),
        yliopistoMapper = mock(),
        erikoisalaService = mock(),
        erikoisalaMapper = mock(),
        userRepository = mock(),
        kayttajaRepository = mock(),
        opintooikeusRepository = mock(),
        verificationTokenService = mock(),
        mailService = mock(),
        asetusRepository = mock(),
        opintoopasRepository = mock(),
        fileValidationService = FileValidationServiceImpl(asiakirjaService, PdfContentValidator())
    )

    @ParameterizedTest
    @MethodSource("invalidUploads")
    fun `all certificate callers reject invalid uploads before changing the date or stored original`(
        data: ByteArray, name: String?, contentType: String?, errorKey: String
    ) {
        val original = "old certificate".toByteArray()
        val oldDate = LocalDate.of(2020, 1, 1)
        val resident = ErikoistuvaLaakari(
            laillistamispaiva = oldDate,
            laillistamistodistus = AsiakirjaData(data = original),
            laillistamispaivanLiitetiedostonNimi = "old.pdf",
            laillistamispaivanLiitetiedostonTyyppi = MediaType.APPLICATION_PDF_VALUE
        )
        whenever(repository.findOneByKayttajaUserId("resident")).thenReturn(resident)

        val exception = assertFailsWith<BadRequestAlertException> {
            service.updateLaillistamispaiva("resident", LocalDate.of(2026, 10, 1), data, name, contentType)
        }

        assertThat(exception.errorKey).isEqualTo("dataillegal.$errorKey")
        assertThat(resident.laillistamispaiva).isEqualTo(oldDate)
        assertThat(resident.laillistamistodistus?.data).containsExactly(*original)
        assertThat(resident.laillistamispaivanLiitetiedostonNimi).isEqualTo("old.pdf")
        assertThat(resident.laillistamispaivanLiitetiedostonTyyppi).isEqualTo(MediaType.APPLICATION_PDF_VALUE)
        verifyNoInteractions(repository, asiakirjaService)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `accepts a usable certificate and stores the unchanged original`(restricted: Boolean) {
        val resident = ErikoistuvaLaakari()
        val data = PdfTestData.certificate(openingPassword = if (restricted) "" else null)
        val original = data.copyOf()
        val date = LocalDate.of(2026, 10, 1)
        whenever(repository.findOneByKayttajaUserId("resident")).thenReturn(resident)

        service.updateLaillistamispaiva("resident", date, data, "certificate.pdf", MediaType.APPLICATION_PDF_VALUE)

        assertThat(resident.laillistamispaiva).isEqualTo(date)
        assertThat(resident.laillistamistodistus?.data).containsExactly(*original)
        assertThat(data).containsExactly(*original)
        assertThat(resident.laillistamispaivanLiitetiedostonNimi).isEqualTo("certificate.pdf")
        verify(repository).save(resident)
        verifyNoInteractions(asiakirjaService)
    }

    @Test
    fun `date-only updates do not revalidate or replace an existing legacy certificate`() {
        val original = AsiakirjaData(data = "legacy bytes".toByteArray())
        val resident = ErikoistuvaLaakari(laillistamistodistus = original, laillistamispaivanLiitetiedostonNimi = "old.pdf")
        val date = LocalDate.of(2026, 10, 1)
        whenever(repository.findOneByKayttajaUserId("resident")).thenReturn(resident)

        service.updateLaillistamispaiva("resident", date, null, null, null)

        assertThat(resident.laillistamispaiva).isEqualTo(date)
        assertThat(resident.laillistamistodistus).isSameAs(original)
        assertThat(resident.laillistamispaivanLiitetiedostonNimi).isEqualTo("old.pdf")
        verify(repository).save(resident)
    }

    companion object {
        @JvmStatic
        fun invalidUploads(): Stream<Arguments> = Stream.of(
            Arguments.of(ByteArray(0), "empty.pdf", MediaType.APPLICATION_PDF_VALUE, "tiedosto-on-tyhja"),
            Arguments.of(emptyPdf(), "no-pages.pdf", MediaType.APPLICATION_PDF_VALUE, "pdf-tiedostoa-ei-voitu-kasitella"),
            Arguments.of("not PDF".toByteArray(), "bad.pdf", MediaType.APPLICATION_PDF_VALUE, "pdf-tiedostoa-ei-voitu-kasitella"),
            Arguments.of(PdfTestData.certificate().copyOf(20), "truncated.pdf", MediaType.APPLICATION_PDF_VALUE, "pdf-tiedostoa-ei-voitu-kasitella"),
            Arguments.of(PdfTestData.certificate(openingPassword = "secret"), "locked.pdf", MediaType.APPLICATION_PDF_VALUE, "pdf-tiedosto-vaatii-salasanan"),
            Arguments.of(byteArrayOf(1), null, MediaType.APPLICATION_PDF_VALUE, "tiedosto-ei-ole-kelvollinen"),
            Arguments.of(byteArrayOf(1), " ", MediaType.APPLICATION_PDF_VALUE, "tiedosto-ei-ole-kelvollinen"),
            Arguments.of(byteArrayOf(1), "a".repeat(256), MediaType.APPLICATION_PDF_VALUE, "tiedosto-ei-ole-kelvollinen"),
            Arguments.of(byteArrayOf(1), "bad.txt", "text/plain", "tiedostotyyppi-ei-ole-sallittu"),
            Arguments.of(byteArrayOf(1), "bad.pdf", null, "tiedostotyyppi-ei-ole-sallittu")
        )

        private fun emptyPdf(): ByteArray = ByteArrayOutputStream().use { output ->
            PDDocument().use { document -> document.save(output) }
            output.toByteArray()
        }
    }
}
