package fi.elsapalvelu.elsa.service.impl.arviointi

import fi.elsapalvelu.elsa.domain.arviointi.ArviointityokaluKysymys
import fi.elsapalvelu.elsa.domain.arviointi.Suoritusarviointi
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.repository.arviointi.ArviointityokaluKysymysRepository
import fi.elsapalvelu.elsa.repository.arviointi.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.service.PdfTextFieldValidator
import fi.elsapalvelu.elsa.service.PdfTextValidator
import fi.elsapalvelu.elsa.service.dto.arviointi.SuoritusarvioinninArviointityokalunVastausDTO
import fi.elsapalvelu.elsa.service.dto.arviointi.SuoritusarviointiDTO
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.web.rest.errors.UnsupportedPdfCharactersException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import org.springframework.core.io.ClassPathResource
import java.time.LocalDate
import java.util.Optional
import kotlin.test.assertFailsWith

class SuoritusarviointiPdfTextValidationTest {
    private val repository = mock<SuoritusarviointiRepository>()
    private val kayttajaRepository = mock<KayttajaRepository>()
    private val kysymysRepository = mock<ArviointityokaluKysymysRepository>()
    private val mailService = mock<MailService>()
    private val validator = PdfTextFieldValidator(PdfTextValidator(
        ClassPathResource("fonts/LiberationSerif-Bold.ttf"),
        ClassPathResource("fonts/LiberationSerif-Regular.ttf"),
        ClassPathResource("fonts/NotoSans-Italic.ttf"),
        ClassPathResource("fonts/NotoSans-Regular.ttf")
    ))
    private val service = SuoritusarviointiServiceImpl(
        suoritusarviointiRepository = repository,
        erikoistuvaLaakariRepository = mock(),
        tyoskentelyjaksoRepository = mock(),
        kayttajaRepository = kayttajaRepository,
        suoritusarviointiMapper = mock(),
        suoritusarvioinninArvioitavaKokonaisuusMapper = mock(),
        arviointityokaluRepository = mock(),
        mailService = mailService,
        asiakirjaRepository = mock(),
        asiakirjaMapper = mock(),
        arviointityokaluKysymysRepository = kysymysRepository,
        arviointityokaluKysymysVaihtoehtoRepository = mock(),
        pdfTextFieldValidator = validator
    )

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `rejects unsupported tool answers in drafts and final assessments before mutating data`(draft: Boolean) {
        val kouluttaja = Kayttaja(id = 2L, user = User(id = "kouluttaja"))
        val stored = Suoritusarviointi(id = 42L, arvioinninAntaja = kouluttaja, sanallinenArviointi = "Vanha arviointi")
        val date = LocalDate.of(2026, 10, 1)
        val dto = SuoritusarviointiDTO(
            id = 42L,
            sanallinenArviointi = "Uusi arviointi",
            tapahtumanAjankohta = date,
            keskenerainen = draft,
            arviointityokaluVastaukset = mutableSetOf(
                SuoritusarvioinninArviointityokalunVastausDTO(arviointityokaluKysymysId = 3L, tekstiVastaus = "Tuettu teksti"),
                SuoritusarvioinninArviointityokalunVastausDTO(arviointityokaluKysymysId = 4L, tekstiVastaus = "💗")
            )
        )
        whenever(repository.findOneById(42L)).thenReturn(Optional.of(stored))
        whenever(kayttajaRepository.findOneByUserId("kouluttaja")).thenReturn(Optional.of(kouluttaja))
        whenever(kysymysRepository.findById(3L)).thenReturn(Optional.of(ArviointityokaluKysymys(otsikko = "Erikoistujan nimi")))
        whenever(kysymysRepository.findById(4L)).thenReturn(Optional.of(ArviointityokaluKysymys(otsikko = "Ohjaajan nimi")))

        val exception = assertFailsWith<UnsupportedPdfCharactersException> {
            service.save(dto, mutableSetOf(), null, "kouluttaja")
        }

        assertThat(exception.field).isEqualTo("Ohjaajan nimi")
        assertThat(exception.unsupportedCharacters).containsExactly("💗 (U+1F497)")
        assertThat(exception.sourceId).isEqualTo(42L)
        assertThat(exception.sourceDate).isEqualTo(date)
        assertThat(stored.sanallinenArviointi).isEqualTo("Vanha arviointi")
        assertThat(stored.arviointityokaluVastaukset).isEmpty()
        verify(repository, never()).save(any())
        verifyNoInteractions(mailService)
    }
}
