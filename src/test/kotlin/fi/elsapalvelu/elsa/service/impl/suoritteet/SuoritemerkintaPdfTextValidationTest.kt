package fi.elsapalvelu.elsa.service.impl.suoritteet

import fi.elsapalvelu.elsa.repository.kayttaja.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.suoritteet.SuoritemerkintaRepository
import fi.elsapalvelu.elsa.repository.tyoskentely.TyoskentelyjaksoRepository
import fi.elsapalvelu.elsa.service.PdfTextTestSupport
import fi.elsapalvelu.elsa.service.dto.suoritteet.SuoritemerkintaDTO
import fi.elsapalvelu.elsa.service.dto.suoritteet.UusiSuoritemerkintaDTO
import fi.elsapalvelu.elsa.service.mapper.suoritteet.SuoritemerkintaMapper
import fi.elsapalvelu.elsa.web.rest.errors.UnsupportedPdfCharactersException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import java.time.LocalDate
import kotlin.test.assertFailsWith

class SuoritemerkintaPdfTextValidationTest {
    private val repository = mock<SuoritemerkintaRepository>()
    private val residentRepository = mock<ErikoistuvaLaakariRepository>()
    private val workRepository = mock<TyoskentelyjaksoRepository>()
    private val mapper = mock<SuoritemerkintaMapper>()
    private val service = SuoritemerkintaServiceImpl(repository, residentRepository, workRepository, mapper, PdfTextTestSupport.fieldValidator())
    private val date = LocalDate.of(2026, 10, 1)

    @Test
    fun `rejects batch creation before any procedure entry is mapped or saved`() {
        val dto = UusiSuoritemerkintaDTO(tyoskentelyjaksoId = 1L, suorituspaiva = date, lisatiedot = "💗")
        val error = assertFailsWith<UnsupportedPdfCharactersException> { service.create(dto, "resident") }
        assertContext(error, null)
        verifyNoInteractions(repository, residentRepository, workRepository, mapper)
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `rejects individual create and update before any persistence`(update: Boolean) {
        val id = if (update) 42L else null
        val dto = SuoritemerkintaDTO(id = id, tyoskentelyjaksoId = 1L, suorituspaiva = date, lisatiedot = "💗")
        val error = assertFailsWith<UnsupportedPdfCharactersException> { service.save(dto, "resident") }
        assertContext(error, id)
        verifyNoInteractions(repository, residentRepository, workRepository, mapper)
    }

    private fun assertContext(error: UnsupportedPdfCharactersException, id: Long?) {
        assertThat(error.field).isEqualTo("lisatiedot")
        assertThat(error.pdfSource).isEqualTo("suoritemerkinta")
        assertThat(error.sourceId).isEqualTo(id)
        assertThat(error.sourceDate).isEqualTo(date)
        assertThat(error.unsupportedCharacters).containsExactly("💗 (U+1F497)")
    }
}
