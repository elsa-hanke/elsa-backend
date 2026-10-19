package fi.elsapalvelu.elsa.service.impl.koejakso

import fi.elsapalvelu.elsa.domain.kayttaja.*
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonLoppukeskustelu
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonLoppukeskusteluRepository
import fi.elsapalvelu.elsa.service.PdfTextFieldValidator
import fi.elsapalvelu.elsa.service.PdfTextValidator
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonLoppukeskusteluDTO
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonLoppukeskusteluMapper
import fi.elsapalvelu.elsa.web.rest.errors.UnsupportedPdfCharactersException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import org.springframework.core.io.ClassPathResource
import java.util.Optional
import kotlin.test.assertFailsWith

class KoejaksonLoppukeskusteluPdfTextValidationTest {
    private val repository = mock<KoejaksonLoppukeskusteluRepository>()
    private val mapper = mock<KoejaksonLoppukeskusteluMapper>()
    private val opintooikeusRepository = mock<OpintooikeusRepository>()
    private val mailService = mock<MailService>()
    private val validator = PdfTextFieldValidator(PdfTextValidator(
        ClassPathResource("fonts/LiberationSerif-Bold.ttf"),
        ClassPathResource("fonts/LiberationSerif-Regular.ttf"),
        ClassPathResource("fonts/NotoSans-Italic.ttf"),
        ClassPathResource("fonts/NotoSans-Regular.ttf")
    ))
    private val service = KoejaksonLoppukeskusteluServiceImpl(
        koejaksonLoppukeskusteluRepository = repository,
        koejaksonLoppukeskusteluMapper = mapper,
        mailService = mailService,
        kayttajaRepository = mock(),
        opintooikeusRepository = opintooikeusRepository,
        kouluttajavaltuutusService = mock(),
        opintooikeusService = mock(),
        pdfTextFieldValidator = validator,
        koejaksonAloituskeskusteluRepository = mock(),
        koejaksonValiarviointiRepository = mock(),
        koejaksonKehittamistoimenpiteetRepository = mock()
    )

    @ParameterizedTest
    @ValueSource(strings = ["selvitys-jatkotoimista"])
    fun `create rejects unsupported text before saving or sending email`(field: String) {
        val dto = KoejaksonLoppukeskusteluDTO()
        whenever(opintooikeusRepository.findById(1L)).thenReturn(Optional.of(Opintooikeus()))
        whenever(mapper.toEntity(dto)).thenReturn(unsupportedEntity(field))

        val exception = assertFailsWith<UnsupportedPdfCharactersException> { service.create(dto, 1L) }

        assertThat(exception.field).isEqualTo(field)
        assertThat(exception.unsupportedCharacters).containsExactly("💗 (U+1F497)")
        verify(repository, never()).save(any())
        verifyNoInteractions(mailService)
    }

    @ParameterizedTest
    @ValueSource(strings = ["selvitys-jatkotoimista"])
    fun `editor update rejects unsupported text before changing existing data`(field: String) {
        val dto = KoejaksonLoppukeskusteluDTO(id = 42L)
        val stored = KoejaksonLoppukeskustelu(id = 42L)
        stored.lahikouluttaja = Kayttaja(user = User(id = "editor"))
        whenever(repository.findById(42L)).thenReturn(Optional.of(stored))
        whenever(mapper.toEntity(dto)).thenReturn(unsupportedEntity(field))

        val exception = assertFailsWith<UnsupportedPdfCharactersException> { service.update(dto, "editor") }

        assertThat(exception.field).isEqualTo(field)
        assertThat(exception.unsupportedCharacters).containsExactly("💗 (U+1F497)")
        assertThat(stored.jatkotoimenpiteet).isNull()
        verify(repository, never()).save(any())
        verifyNoInteractions(mailService)
    }

    @ParameterizedTest
    @ValueSource(strings = ["selvitys-jatkotoimista"])
    fun `ignored fields from another role do not block update`(field: String) {
        val dto = KoejaksonLoppukeskusteluDTO(id = 42L)
        whenever(repository.findById(42L)).thenReturn(Optional.of(KoejaksonLoppukeskustelu(id = 42L)))
        whenever(mapper.toEntity(dto)).thenReturn(unsupportedEntity(field))
        whenever(mapper.toDto(any<KoejaksonLoppukeskustelu>())).thenReturn(dto)

        service.update(dto, "other-user")

        verify(repository, never()).save(any())
        verifyNoInteractions(mailService)
    }

    private fun unsupportedEntity(field: String) = KoejaksonLoppukeskustelu().apply {
        when (field) {
            "selvitys-jatkotoimista" -> jatkotoimenpiteet = "Teksti 💗"
        }
    }
}
