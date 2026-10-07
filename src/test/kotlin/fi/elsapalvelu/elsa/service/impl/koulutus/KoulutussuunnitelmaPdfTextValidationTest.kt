package fi.elsapalvelu.elsa.service.impl.koulutus

import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.koulutus.KoulutussuunnitelmaRepository
import fi.elsapalvelu.elsa.service.PdfTextTestSupport
import fi.elsapalvelu.elsa.service.dto.kayttaja.AsiakirjaDTO
import fi.elsapalvelu.elsa.service.dto.koulutus.KoulutussuunnitelmaDTO
import fi.elsapalvelu.elsa.service.mapper.koulutus.KoulutussuunnitelmaMapper
import fi.elsapalvelu.elsa.web.rest.errors.UnsupportedPdfCharactersException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import kotlin.test.assertFailsWith

class KoulutussuunnitelmaPdfTextValidationTest {
    private val repository = mock<KoulutussuunnitelmaRepository>()
    private val mapper = mock<KoulutussuunnitelmaMapper>()
    private val opintooikeusRepository = mock<OpintooikeusRepository>()
    private val service = KoulutussuunnitelmaServiceImpl(repository, mapper, opintooikeusRepository, PdfTextTestSupport.fieldValidator())

    @ParameterizedTest
    @ValueSource(strings = [
        "motivaatiokirje", "opiskelu-ja-tyohistoria", "vahvuudet", "tulevaisuuden-visiointi",
        "osaamisen-kartuttaminen", "elamankentta", "motivaatiokirje-tiedostonimi"
    ])
    fun `rejects every PDF text field before reading or changing the saved plan`(field: String) {
        listOf(null, 42L).forEach { id ->
            val dto = KoulutussuunnitelmaDTO(id = id)
            when (field) {
                "motivaatiokirje" -> dto.motivaatiokirje = "💗"
                "opiskelu-ja-tyohistoria" -> dto.opiskeluJaTyohistoria = "💗"
                "vahvuudet" -> dto.vahvuudet = "💗"
                "tulevaisuuden-visiointi" -> dto.tulevaisuudenVisiointi = "💗"
                "osaamisen-kartuttaminen" -> dto.osaamisenKartuttaminen = "💗"
                "elamankentta" -> dto.elamankentta = "💗"
                "motivaatiokirje-tiedostonimi" -> dto.motivaatiokirjeAsiakirja = AsiakirjaDTO(nimi = "💗.pdf")
            }
            val error = assertFailsWith<UnsupportedPdfCharactersException> { service.save(dto, 1L) }
            assertThat(error.field).isEqualTo(field)
            assertThat(error.pdfSource).isEqualTo("koulutussuunnitelma")
            assertThat(error.sourceId).isEqualTo(id)
            assertThat(error.unsupportedCharacters).containsExactly("💗 (U+1F497)")
            assertThat(dto.id).isEqualTo(id)
            verifyNoInteractions(repository, mapper, opintooikeusRepository)
        }
    }
}
