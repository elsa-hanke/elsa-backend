package fi.elsapalvelu.elsa.service.impl.tyoskentely

import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.tyoskentely.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.PdfTextTestSupport
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelypaikkaDTO
import fi.elsapalvelu.elsa.service.mapper.kayttaja.AsiakirjaMapper
import fi.elsapalvelu.elsa.service.mapper.tyoskentely.TyoskentelyjaksoMapper
import fi.elsapalvelu.elsa.web.rest.errors.UnsupportedPdfCharactersException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import java.time.LocalDate
import kotlin.test.assertFailsWith

class TyoskentelyjaksoPdfTextValidationTest {
    private val repository = mock<TyoskentelyjaksoRepository>()
    private val mapper = mock<TyoskentelyjaksoMapper>()
    private val asiakirjaMapper = mock<AsiakirjaMapper>()
    private val opintooikeusRepository = mock<OpintooikeusRepository>()
    private val service = TyoskentelyjaksoServiceImpl(
        tyoskentelyjaksoRepository = repository,
        kuntaRepository = mock(),
        erikoisalaRepository = mock(),
        tyoskentelyjaksoMapper = mapper,
        tyoskentelyjaksoWithKeskeytysajatMapper = mock(),
        asiakirjaMapper = asiakirjaMapper,
        tyoskentelyjaksonPituusCounterService = mock(),
        opintooikeusRepository = opintooikeusRepository,
        pdfTextFieldValidator = PdfTextTestSupport.fieldValidator()
    )

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `rejects the workplace before changing the work period or mapping attachments`(update: Boolean) {
        val id = if (update) 42L else null
        val date = LocalDate.of(2026, 10, 1)
        val dto = TyoskentelyjaksoDTO(id = id, alkamispaiva = date, tyoskentelypaikka = TyoskentelypaikkaDTO(nimi = "💗"))
        val error = assertFailsWith<UnsupportedPdfCharactersException> {
            if (update) service.update(dto, 1L, mutableSetOf(), null)
            else service.create(dto, 1L, mutableSetOf())
        }
        assertThat(error.field).isEqualTo("tyoskentelypaikka")
        assertThat(error.pdfSource).isEqualTo("tyoskentelyjakso")
        assertThat(error.sourceId).isEqualTo(id)
        assertThat(error.sourceDate).isEqualTo(date)
        assertThat(error.unsupportedCharacters).containsExactly("💗 (U+1F497)")
        verifyNoInteractions(repository, mapper, asiakirjaMapper, opintooikeusRepository)
    }
}
