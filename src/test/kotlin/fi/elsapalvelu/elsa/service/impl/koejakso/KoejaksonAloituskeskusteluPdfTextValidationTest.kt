package fi.elsapalvelu.elsa.service.impl.koejakso

import fi.elsapalvelu.elsa.domain.kayttaja.*
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonAloituskeskustelu
import fi.elsapalvelu.elsa.repository.kayttaja.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.repository.koejakso.KoejaksonAloituskeskusteluRepository
import fi.elsapalvelu.elsa.service.PdfTextFieldValidator
import fi.elsapalvelu.elsa.service.PdfTextValidator
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonAloituskeskusteluDTO
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.mapper.koejakso.KoejaksonAloituskeskusteluMapper
import fi.elsapalvelu.elsa.web.rest.errors.UnsupportedPdfCharactersException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import org.springframework.core.io.ClassPathResource
import java.util.Optional
import kotlin.test.assertFailsWith

class KoejaksonAloituskeskusteluPdfTextValidationTest {
    private val repository = mock<KoejaksonAloituskeskusteluRepository>()
    private val mapper = mock<KoejaksonAloituskeskusteluMapper>()
    private val opintooikeusRepository = mock<OpintooikeusRepository>()
    private val erikoistuvaLaakariRepository = mock<ErikoistuvaLaakariRepository>()
    private val mailService = mock<MailService>()
    private val validator = PdfTextFieldValidator(PdfTextValidator(
        ClassPathResource("fonts/LiberationSerif-Bold.ttf"),
        ClassPathResource("fonts/LiberationSerif-Regular.ttf"),
        ClassPathResource("fonts/NotoSans-Italic.ttf"),
        ClassPathResource("fonts/NotoSans-Regular.ttf")
    ))
    private val service = KoejaksonAloituskeskusteluServiceImpl(
        erikoistuvaLaakariRepository = erikoistuvaLaakariRepository,
        koejaksonAloituskeskusteluRepository = repository,
        koejaksonAloituskeskusteluMapper = mapper,
        mailService = mailService,
        kayttajaRepository = mock(),
        opintooikeusRepository = opintooikeusRepository,
        kouluttajavaltuutusService = mock(),
        opintooikeusService = mock(),
        pdfTextFieldValidator = validator,
        userRepository = mock()
    )

    @Test
    fun `supported Finnish text and draft save remain allowed`() {
        val dto = KoejaksonAloituskeskusteluDTO(lahetetty = false)
        val entity = KoejaksonAloituskeskustelu(
            koejaksonSuorituspaikka = "Terveyskeskus – Jyväskylä",
            koejaksonOsaamistavoitteet = "Ensimmäinen tavoite\nToinen tavoite"
        )
        whenever(opintooikeusRepository.findById(1L)).thenReturn(Optional.of(Opintooikeus()))
        whenever(mapper.toEntity(dto)).thenReturn(entity)
        whenever(repository.save(entity)).thenReturn(entity)
        whenever(mapper.toDto(entity)).thenReturn(dto)

        assertThat(service.create(dto, 1L)).isEqualTo(dto)

        verify(repository).save(entity)
        verifyNoInteractions(mailService)
    }

    @ParameterizedTest
    @ValueSource(strings = ["koejakson-suorituspaikka", "koejakson-toinen-suorituspaikka", "koejakso-osaamistavoitteet"])
    fun `create rejects unsupported text before saving or sending email`(field: String) {
        val dto = KoejaksonAloituskeskusteluDTO()
        whenever(opintooikeusRepository.findById(1L)).thenReturn(Optional.of(Opintooikeus()))
        whenever(mapper.toEntity(dto)).thenReturn(unsupportedEntity(field))

        val exception = assertFailsWith<UnsupportedPdfCharactersException> { service.create(dto, 1L) }

        assertThat(exception.field).isEqualTo(field)
        assertThat(exception.unsupportedCharacters).containsExactly("💗 (U+1F497)")
        verify(repository, never()).save(any())
        verifyNoInteractions(mailService)
    }

    @ParameterizedTest
    @ValueSource(strings = ["koejakson-suorituspaikka", "koejakson-toinen-suorituspaikka", "koejakso-osaamistavoitteet"])
    fun `editor update rejects unsupported text before changing existing data`(field: String) {
        val dto = KoejaksonAloituskeskusteluDTO(id = 42L)
        val stored = KoejaksonAloituskeskustelu(id = 42L)
        val erikoistuva = ErikoistuvaLaakari(id = 2L)
        stored.opintooikeus = Opintooikeus(erikoistuvaLaakari = erikoistuva)
        whenever(erikoistuvaLaakariRepository.findOneByKayttajaUserId("editor")).thenReturn(erikoistuva)
        whenever(repository.findById(42L)).thenReturn(Optional.of(stored))
        whenever(mapper.toEntity(dto)).thenReturn(unsupportedEntity(field))

        val exception = assertFailsWith<UnsupportedPdfCharactersException> { service.update(dto, "editor") }

        assertThat(exception.field).isEqualTo(field)
        assertThat(exception.unsupportedCharacters).containsExactly("💗 (U+1F497)")
        assertThat(stored.koejaksonSuorituspaikka).isNull()
        assertThat(stored.koejaksonToinenSuorituspaikka).isNull()
        assertThat(stored.koejaksonOsaamistavoitteet).isNull()
        verify(repository, never()).save(any())
        verifyNoInteractions(mailService)
    }

    @ParameterizedTest
    @ValueSource(strings = ["koejakson-suorituspaikka", "koejakson-toinen-suorituspaikka", "koejakso-osaamistavoitteet"])
    fun `ignored fields from another role do not block update`(field: String) {
        val dto = KoejaksonAloituskeskusteluDTO(id = 42L)
        whenever(repository.findById(42L)).thenReturn(Optional.of(KoejaksonAloituskeskustelu(id = 42L)))
        whenever(mapper.toEntity(dto)).thenReturn(unsupportedEntity(field))
        whenever(mapper.toDto(any<KoejaksonAloituskeskustelu>())).thenReturn(dto)

        service.update(dto, "other-user")

        verify(repository, never()).save(any())
        verifyNoInteractions(mailService)
    }

    private fun unsupportedEntity(field: String) = KoejaksonAloituskeskustelu().apply {
        when (field) {
            "koejakson-suorituspaikka" -> koejaksonSuorituspaikka = "Teksti 💗"
            "koejakson-toinen-suorituspaikka" -> koejaksonToinenSuorituspaikka = "Teksti 💗"
            "koejakso-osaamistavoitteet" -> koejaksonOsaamistavoitteet = "Teksti 💗"
        }
    }
}
