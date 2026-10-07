package fi.elsapalvelu.elsa.service.impl.koulutus

import fi.elsapalvelu.elsa.repository.koulutus.KoulutusjaksoRepository
import fi.elsapalvelu.elsa.service.PdfTextTestSupport
import fi.elsapalvelu.elsa.service.arviointi.ArvioitavaKokonaisuusService
import fi.elsapalvelu.elsa.service.dto.koulutus.KoulutusjaksoDTO
import fi.elsapalvelu.elsa.service.koulutus.KoulutussuunnitelmaService
import fi.elsapalvelu.elsa.service.mapper.koulutus.KoulutusjaksoMapper
import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyjaksoService
import fi.elsapalvelu.elsa.web.rest.errors.UnsupportedPdfCharactersException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import kotlin.test.assertFailsWith

class KoulutusjaksoPdfTextValidationTest {
    private val repository = mock<KoulutusjaksoRepository>()
    private val mapper = mock<KoulutusjaksoMapper>()
    private val suunnitelmaService = mock<KoulutussuunnitelmaService>()
    private val tyoskentelyjaksoService = mock<TyoskentelyjaksoService>()
    private val kokonaisuusService = mock<ArvioitavaKokonaisuusService>()
    private val service = KoulutusjaksoServiceImpl(repository, mapper, suunnitelmaService, tyoskentelyjaksoService, kokonaisuusService, PdfTextTestSupport.fieldValidator())

    @ParameterizedTest
    @ValueSource(strings = ["koulutusjakson-nimi", "muut-osaamistavoitteet"])
    fun `rejects create and update text before mapping or saving`(field: String) {
        listOf(null, 42L).forEach { id ->
            val dto = KoulutusjaksoDTO(id = id)
            if (field == "koulutusjakson-nimi") dto.nimi = "💗" else dto.muutOsaamistavoitteet = "💗"
            val error = assertFailsWith<UnsupportedPdfCharactersException> { service.save(dto, 1L) }
            assertThat(error.field).isEqualTo(field)
            assertThat(error.pdfSource).isEqualTo("koulutusjakso")
            assertThat(error.sourceId).isEqualTo(id)
            assertThat(error.unsupportedCharacters).containsExactly("💗 (U+1F497)")
            assertThat(dto.koulutussuunnitelma).isNull()
            verifyNoInteractions(repository, mapper, suunnitelmaService, tyoskentelyjaksoService, kokonaisuusService)
        }
    }
}
