package fi.elsapalvelu.elsa.service.impl.arviointi

import fi.elsapalvelu.elsa.domain.arviointi.Suoritusarviointi
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.tyoskentely.Tyoskentelyjakso
import fi.elsapalvelu.elsa.repository.arviointi.SuoritusarviointiRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.service.dto.arviointi.SuoritusarviointiDTO
import fi.elsapalvelu.elsa.service.kayttaja.MailProperty
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.mapper.arviointi.SuoritusarviointiMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Optional

class SuoritusarviointiCompletionTest {
    private val repository = mock<SuoritusarviointiRepository>()
    private val kayttajat = mock<KayttajaRepository>()
    private val mapper = mock<SuoritusarviointiMapper>()
    private val mail = mock<MailService>()
    private val clock = mock<Clock> {
        on { zone } doReturn ZoneId.of("Europe/Helsinki")
        // The Helsinki date is already the next day, unlike UTC.
        on { instant() } doReturn Instant.parse("2026-11-15T23:00:00Z")
    }
    private val trainer = Kayttaja(id = 2L, user = User(id = "trainer"))
    private val trainee = Kayttaja(id = 3L, user = User(id = "trainee"))
    private val assessment = Suoritusarviointi(
        id = 42L,
        keskenerainen = true,
        arvioinninAntaja = trainer,
        tyoskentelyjakso = Tyoskentelyjakso(
            opintooikeus = Opintooikeus(erikoistuvaLaakari = ErikoistuvaLaakari(kayttaja = trainee))
        )
    )
    private val service = SuoritusarviointiServiceImpl(
        suoritusarviointiRepository = repository,
        erikoistuvaLaakariRepository = mock(),
        tyoskentelyjaksoRepository = mock(),
        kayttajaRepository = kayttajat,
        suoritusarviointiMapper = mapper,
        suoritusarvioinninArvioitavaKokonaisuusMapper = mock(),
        arviointityokaluRepository = mock(),
        mailService = mail,
        asiakirjaRepository = mock(),
        asiakirjaMapper = mock(),
        arviointityokaluKysymysRepository = mock(),
        arviointityokaluKysymysVaihtoehtoRepository = mock(),
        pdfTextFieldValidator = mock(),
        clock = clock
    )

    init {
        whenever(repository.findOneById(42L)).thenReturn(Optional.of(assessment))
        whenever(repository.save(any<Suoritusarviointi>())).thenAnswer { it.arguments[0] }
        whenever(kayttajat.findOneByUserId("trainer")).thenReturn(Optional.of(trainer))
        whenever(kayttajat.findById(3L)).thenReturn(Optional.of(trainee))
        whenever(mapper.toDto(any<Suoritusarviointi>())).thenAnswer {
            val saved = it.arguments[0] as Suoritusarviointi
            SuoritusarviointiDTO(arviointiAika = saved.arviointiAika, keskenerainen = saved.keskenerainen)
        }
    }

    @Test
    fun `draft save has no completion date or notification`() {
        val result = save(draft = true)

        assertThat(assessment.arviointiAika).isNull()
        assertThat(result.arviointiAika).isNull()
        assertThat(result.keskenerainen).isTrue()
        verifyNoInteractions(mail)
    }

    @Test
    fun `first completion uses clock date and sends new assessment notification`() {
        save(draft = true)
        val result = save(draft = false)

        assertThat(result.arviointiAika).isEqualTo(LocalDate.of(2026, 11, 16))
        assertThat(assessment.arviointiAika).isEqualTo(result.arviointiAika)
        assertThat(result.keskenerainen).isFalse()
        verifyNotification("arviointiAnnettuEmail", "email.arviointiannettu.title")
        verifyNoMoreInteractions(mail)
    }

    @Test
    fun `editing completed assessment refreshes date and sends modification notification`() {
        assessment.keskenerainen = false
        assessment.arviointiAika = LocalDate.of(2020, 1, 2)

        val result = save(draft = false)

        assertThat(assessment.arviointiAika).isEqualTo(LocalDate.of(2026, 11, 16))
        assertThat(result.arviointiAika).isEqualTo(LocalDate.of(2026, 11, 16))
        assertThat(assessment.sanallinenArviointi).isEqualTo("Paivitetty arviointi")
        verifyNotification("arviointiaMuokattuEmail", "email.arviointiamuokattu.title")
        verifyNoMoreInteractions(mail)
    }

    private fun save(draft: Boolean): SuoritusarviointiDTO = service.save(
        SuoritusarviointiDTO(
            id = 42L, keskenerainen = draft, sanallinenArviointi = "Paivitetty arviointi",
            arviointiAika = LocalDate.of(1999, 1, 1)
        ),
        mutableSetOf(), null, "trainer"
    )

    private fun verifyNotification(template: String, title: String) {
        verify(mail).sendEmailFromTemplate(
            user = trainee.user!!,
            templateName = template,
            titleKey = title,
            properties = mapOf(MailProperty.ID to "42")
        )
    }
}
