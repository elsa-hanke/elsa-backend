package fi.elsapalvelu.elsa.service.impl.koejakso

import fi.elsapalvelu.elsa.domain.kayttaja.*
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonValiarviointi
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonValiarviointiRepository
import fi.elsapalvelu.elsa.service.PdfTextFieldValidator
import fi.elsapalvelu.elsa.service.PdfTextValidator
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonValiarviointiDTO
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonValiarviointiMapper
import fi.elsapalvelu.elsa.web.rest.errors.UnsupportedPdfCharactersException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import org.springframework.core.io.ClassPathResource
import java.util.Optional
import kotlin.test.assertFailsWith

class KoejaksonValiarviointiPdfTextValidationTest {
    private val repository = mock<KoejaksonValiarviointiRepository>()
    private val mapper = mock<KoejaksonValiarviointiMapper>()
    private val opintooikeusRepository = mock<OpintooikeusRepository>()
    private val mailService = mock<MailService>()
    private val validator = PdfTextFieldValidator(PdfTextValidator(
        ClassPathResource("fonts/LiberationSerif-Bold.ttf"),
        ClassPathResource("fonts/LiberationSerif-Regular.ttf"),
        ClassPathResource("fonts/NotoSans-Italic.ttf"),
        ClassPathResource("fonts/NotoSans-Regular.ttf")
    ))
    private val service = KoejaksonValiarviointiServiceImpl(
        koejaksonValiarviointiRepository = repository,
        koejaksonValiarviointiMapper = mapper,
        mailService = mailService,
        kayttajaRepository = mock(),
        opintooikeusRepository = opintooikeusRepository,
        kouluttajavaltuutusService = mock(),
        opintooikeusService = mock(),
        pdfTextFieldValidator = validator,
        koejaksonAloituskeskusteluRepository = mock()
    )

    @ParameterizedTest
    @ValueSource(strings = ["vahvuudet", "selvitys-kehittamistoimenpiteista", "muu"])
    fun `create rejects unsupported text before saving or sending email`(field: String) {
        val dto = KoejaksonValiarviointiDTO()
        whenever(opintooikeusRepository.findById(1L)).thenReturn(Optional.of(Opintooikeus()))
        whenever(mapper.toEntity(dto)).thenReturn(unsupportedEntity(field))

        val exception = assertFailsWith<UnsupportedPdfCharactersException> { service.create(dto, 1L) }

        assertThat(exception.field).isEqualTo(field)
        assertThat(exception.unsupportedCharacters).containsExactly("💗 (U+1F497)")
        verify(repository, never()).save(any())
        verifyNoInteractions(mailService)
    }

    @ParameterizedTest
    @ValueSource(strings = ["vahvuudet", "selvitys-kehittamistoimenpiteista", "muu"])
    fun `editor update rejects unsupported text before changing existing data`(field: String) {
        val dto = KoejaksonValiarviointiDTO(id = 42L)
        val stored = KoejaksonValiarviointi(id = 42L)
        stored.lahikouluttaja = Kayttaja(user = User(id = "editor"))
        whenever(repository.findById(42L)).thenReturn(Optional.of(stored))
        whenever(mapper.toEntity(dto)).thenReturn(unsupportedEntity(field))

        val exception = assertFailsWith<UnsupportedPdfCharactersException> { service.update(dto, "editor") }

        assertThat(exception.field).isEqualTo(field)
        assertThat(exception.unsupportedCharacters).containsExactly("💗 (U+1F497)")
        assertThat(stored.vahvuudet).isNull()
        assertThat(stored.kehittamistoimenpiteet).isNull()
        assertThat(stored.muuKategoria).isNull()
        verify(repository, never()).save(any())
        verifyNoInteractions(mailService)
    }

    @ParameterizedTest
    @ValueSource(strings = ["vahvuudet", "selvitys-kehittamistoimenpiteista", "muu"])
    fun `ignored fields from another role do not block update`(field: String) {
        val dto = KoejaksonValiarviointiDTO(id = 42L)
        whenever(repository.findById(42L)).thenReturn(Optional.of(KoejaksonValiarviointi(id = 42L)))
        whenever(mapper.toEntity(dto)).thenReturn(unsupportedEntity(field))
        whenever(mapper.toDto(any<KoejaksonValiarviointi>())).thenReturn(dto)

        service.update(dto, "other-user")

        verify(repository, never()).save(any())
        verifyNoInteractions(mailService)
    }

    private fun unsupportedEntity(field: String) = KoejaksonValiarviointi().apply {
        when (field) {
            "vahvuudet" -> vahvuudet = "Teksti 💗"
            "selvitys-kehittamistoimenpiteista" -> kehittamistoimenpiteet = "Teksti 💗"
            "muu" -> muuKategoria = "Teksti 💗"
        }
    }
}
