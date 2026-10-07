package fi.elsapalvelu.elsa.service.impl.valmistuminen

import fi.elsapalvelu.elsa.service.PdfTextTestSupport
import fi.elsapalvelu.elsa.service.dto.valmistuminen.UusiValmistumispyyntoDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyynnonTarkistusUpdateDTO
import fi.elsapalvelu.elsa.web.rest.errors.UnsupportedPdfCharactersException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import kotlin.test.assertFailsWith

class ValmistumispyynnonTarkistusPdfTextValidationTest {
    private val service = ValmistumispyynnonTarkistusService(
        tyoskentelyjaksoService = mock(),
        terveyskeskuskoulutusjaksonHyvaksyntaRepository = mock(),
        opintooikeusRepository = mock(),
        opintosuoritusRepository = mock(),
        teoriakoulutusRepository = mock(),
        opintosuoritusMapper = mock(),
        koejaksonVastuuhenkilonArvioRepository = mock(),
        vanhentumisService = mock(),
        pdfTextFieldValidator = PdfTextTestSupport.fieldValidator()
    )

    @Test
    fun `rejects unsupported text in the explanation for expired studies`() {
        val error = assertFailsWith<UnsupportedPdfCharactersException> {
            service.validateValmistumispyyntoPdfText(UusiValmistumispyyntoDTO(selvitysVanhentuneistaSuorituksista = "💗"))
        }
        assertContext(error, "selvitys-vanhentuneista-suorituksista-otsikko", null)
    }

    @ParameterizedTest
    @ValueSource(strings = ["<p>💗</p>", "<p>&#x1F497;</p>", "<p>&#128151;</p>"])
    fun `rejects literal and encoded unsupported characters in officer draft and approval summaries`(html: String) {
        listOf(false, true).forEach { draft ->
            val error = assertFailsWith<UnsupportedPdfCharactersException> {
                service.validateVirkailijanPdfText(42L, ValmistumispyynnonTarkistusUpdateDTO(virkailijanYhteenveto = html, keskenerainen = draft))
            }
            assertContext(error, "virkailijan-valmistumisen-yhteenveto", 42L)
        }
    }

    @Test
    fun `rejects unsupported officer notes`() {
        val error = assertFailsWith<UnsupportedPdfCharactersException> {
            service.validateVirkailijanPdfText(42L, ValmistumispyynnonTarkistusUpdateDTO(lisatiedotVastuuhenkilolle = "💗"))
        }
        assertContext(error, "lisatiedot-vastuuhenkilolle", 42L)
    }

    @Test
    fun `accepts supported text and escaped literal entity text`() {
        service.validateValmistumispyyntoPdfText(UusiValmistumispyyntoDTO(selvitysVanhentuneistaSuorituksista = "Hyvä selvitys – Jyväskylä"))
        service.validateVirkailijanPdfText(42L, ValmistumispyynnonTarkistusUpdateDTO(
            lisatiedotVastuuhenkilolle = "Hyvä kehitys",
            virkailijanYhteenveto = "<p><strong>Hyvä lääkäri</strong> &amp; pätevä – &amp;#x1F497;</p>"
        ))
    }

    @Test
    fun `allows returning an existing invalid summary without approving it`() {
        service.validateVirkailijanPdfText(42L, ValmistumispyynnonTarkistusUpdateDTO(
            korjausehdotus = "Korjaa selvitys",
            virkailijanYhteenveto = "<p>&#x1F497;</p>"
        ))
    }

    private fun assertContext(error: UnsupportedPdfCharactersException, field: String, id: Long?) {
        assertThat(error.field).isEqualTo(field)
        assertThat(error.pdfSource).isEqualTo("valmistumispyynto")
        assertThat(error.sourceId).isEqualTo(id)
        assertThat(error.unsupportedCharacters).containsExactly("💗 (U+1F497)")
    }
}
