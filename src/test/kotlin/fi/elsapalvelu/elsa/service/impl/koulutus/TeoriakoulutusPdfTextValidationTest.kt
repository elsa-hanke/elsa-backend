package fi.elsapalvelu.elsa.service.impl.koulutus

import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.koulutus.TeoriakoulutusRepository
import fi.elsapalvelu.elsa.repository.seuranta.PaivakirjamerkintaRepository
import fi.elsapalvelu.elsa.service.PdfTextTestSupport
import fi.elsapalvelu.elsa.service.dto.koulutus.TeoriakoulutusDTO
import fi.elsapalvelu.elsa.service.mapper.kayttaja.AsiakirjaMapper
import fi.elsapalvelu.elsa.service.mapper.koulutus.TeoriakoulutusMapper
import fi.elsapalvelu.elsa.web.rest.errors.UnsupportedPdfCharactersException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import java.time.LocalDate
import kotlin.test.assertFailsWith

class TeoriakoulutusPdfTextValidationTest {
    private val repository = mock<TeoriakoulutusRepository>()
    private val mapper = mock<TeoriakoulutusMapper>()
    private val asiakirjaMapper = mock<AsiakirjaMapper>()
    private val opintooikeusRepository = mock<OpintooikeusRepository>()
    private val paivakirjamerkintaRepository = mock<PaivakirjamerkintaRepository>()
    private val service = TeoriakoulutusServiceImpl(
        teoriakoulutusRepository = repository,
        teoriakoulutusMapper = mapper,
        opintooikeusRepository = opintooikeusRepository,
        asiakirjaMapper = asiakirjaMapper,
        paivakirjamerkintaRepository = paivakirjamerkintaRepository,
        pdfTextFieldValidator = PdfTextTestSupport.fieldValidator()
    )

    @ParameterizedTest
    @ValueSource(strings = ["koulutuksen-nimi", "paikka"])
    fun `rejects unsupported create and update text before mapping attachments or changing stored training`(field: String) {
        listOf(null, 42L).forEach { id ->
            val date = LocalDate.of(2026, 10, 1)
            val dto = TeoriakoulutusDTO(id = id, alkamispaiva = date)
            if (field == "koulutuksen-nimi") dto.koulutuksenNimi = "💗" else dto.koulutuksenPaikka = "💗"
            val error = assertFailsWith<UnsupportedPdfCharactersException> { service.save(dto, emptySet(), null, 1L) }
            assertThat(error.field).isEqualTo(field)
            assertThat(error.pdfSource).isEqualTo("teoriakoulutus")
            assertThat(error.sourceId).isEqualTo(id)
            assertThat(error.sourceDate).isEqualTo(date)
            assertThat(error.unsupportedCharacters).containsExactly("💗 (U+1F497)")
            verifyNoInteractions(repository, mapper, asiakirjaMapper, opintooikeusRepository, paivakirjamerkintaRepository)
        }
    }
}
