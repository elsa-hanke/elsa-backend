package fi.elsapalvelu.elsa.scheduler

import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.OpintooikeusHerate
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusHerateRepository
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.scheduler.jobs.ScheduledPaattyvaOpintooikeusHerate
import fi.elsapalvelu.elsa.service.kayttaja.MailProperty
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.*
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ScheduledPaattyvaOpintooikeusHerateTest {
    private val rights: OpintooikeusRepository = mock()
    private val mail: MailService = mock()
    private val reminders: OpintooikeusHerateRepository = mock()
    // Monday in Helsinki, still Sunday in UTC: the injected zone must determine the week.
    private val now = Instant.parse("2026-10-18T21:30:00Z")
    private val clock = Clock.fixed(now, ZoneId.of("Europe/Helsinki"))
    private val job = ScheduledPaattyvaOpintooikeusHerate(rights, mail, reminders, clock)

    @ParameterizedTest
    @CsvSource("2026-10-19", "2026-10-22", "2026-10-25")
    fun `uses the same Monday to Sunday windows throughout a week`(date: LocalDate) {
        val datedJob = ScheduledPaattyvaOpintooikeusHerate(
            rights, mail, reminders, Clock.fixed(date.atStartOfDay(clock.zone).toInstant(), clock.zone)
        )
        datedJob.runJob()

        verify(rights).findAllPaattyvatByTimeFrame(LocalDate.parse("2026-12-19"), LocalDate.parse("2026-12-25"))
        verify(rights).findAllPaattyvatByTimeFrame(LocalDate.parse("2027-01-19"), LocalDate.parse("2027-01-25"))
        verify(rights).findAllPaattyvatByTimeFrame(LocalDate.parse("2027-02-19"), LocalDate.parse("2027-02-25"))
        verify(rights).findAllPaattyvatByTimeFrame(LocalDate.parse("2027-04-19"), LocalDate.parse("2027-04-25"))
        verify(rights).findAllPaattyvatByTimeFrame(LocalDate.parse("2027-10-19"), LocalDate.parse("2027-10-25"))
        verifyNoMoreInteractions(rights)
        verifyNoInteractions(mail, reminders)
    }

    @ParameterizedTest
    @CsvSource("2,true", "3,false", "4,true", "6,true", "6,false", "12,false")
    fun `sends the appropriate reminder only for the eligible study right type`(months: Long, fixedTerm: Boolean) {
        val from = LocalDate.parse("2026-10-19").plusMonths(months)
        val to = LocalDate.parse("2026-10-25").plusMonths(months)
        val eligible = right("eligible", to, fixedTerm)
        val ineligible = right("ineligible", from, !fixedTerm)
        // At six months both types are eligible; the other windows filter the opposite type.
        whenever(rights.findAllPaattyvatByTimeFrame(from, to)).thenReturn(
            if (months == 6L) listOf(eligible) else listOf(eligible, ineligible)
        )

        job.runJob()

        verifyMessage(eligible, fixedTerm)
        val saved = argumentCaptor<OpintooikeusHerate>()
        verify(reminders).save(saved.capture())
        assertThat(saved.firstValue.erikoistuvaLaakari).isSameAs(eligible.erikoistuvaLaakari)
        assertThat(saved.firstValue.maaraaikainenPaattymassaHerateLahetetty).isEqualTo(if (fixedTerm) now else null)
        assertThat(saved.firstValue.paattymassaHerateLahetetty).isEqualTo(if (fixedTerm) null else now)
        verifyNoMoreInteractions(mail)
    }

    @Test
    fun `uses Helsinki date when UTC is still in previous week`() {
        val eligible = right("monday", LocalDate.parse("2026-12-19"), true)
        whenever(rights.findAllPaattyvatByTimeFrame(LocalDate.parse("2026-12-19"), LocalDate.parse("2026-12-25")))
            .thenReturn(listOf(eligible))
        job.runJob()
        verifyMessage(eligible, true)
    }

    @ParameterizedTest
    @CsvSource("true", "false")
    fun `updates existing reminder without losing other timestamps and resends on another run`(fixedTerm: Boolean) {
        val eligible = right("existing", LocalDate.parse("2027-04-19"), fixedTerm)
        val old = Instant.parse("2026-01-01T00:00:00Z")
        val reminder = OpintooikeusHerate(
            id = 1L, erikoistuvaLaakari = eligible.erikoistuvaLaakari,
            useaVoimassaolevaHerateLahetetty = old,
            maaraaikainenPaattymassaHerateLahetetty = old, paattymassaHerateLahetetty = old
        )
        whenever(rights.findAllPaattyvatByTimeFrame(LocalDate.parse("2027-04-19"), LocalDate.parse("2027-04-25")))
            .thenReturn(listOf(eligible))
        whenever(reminders.findOneByErikoistuvaLaakariKayttajaUserId("existing")).thenReturn(reminder)
        job.runJob()
        val later = now.plusSeconds(3600)
        ScheduledPaattyvaOpintooikeusHerate(rights, mail, reminders, Clock.fixed(later, clock.zone)).runJob()

        verifyMessage(eligible, fixedTerm, 2)
        verify(reminders, times(2)).save(same(reminder))
        assertThat(reminder.useaVoimassaolevaHerateLahetetty).isEqualTo(old)
        assertThat(reminder.maaraaikainenPaattymassaHerateLahetetty).isEqualTo(if (fixedTerm) later else old)
        assertThat(reminder.paattymassaHerateLahetetty).isEqualTo(if (fixedTerm) old else later)
    }

    @Test
    fun `skips missing recipient and continues to next study right`() {
        val valid = right("valid", LocalDate.parse("2027-04-19"), true)
        val noResident = right("unused", valid.opintooikeudenPaattymispaiva!!, true).apply { erikoistuvaLaakari = null }
        val noAccount = right("unused3", valid.opintooikeudenPaattymispaiva!!, true).apply {
            erikoistuvaLaakari!!.kayttaja = null
        }
        val noUser = right("unused2", valid.opintooikeudenPaattymispaiva!!, true).apply {
            erikoistuvaLaakari!!.kayttaja = Kayttaja()
        }
        whenever(rights.findAllPaattyvatByTimeFrame(LocalDate.parse("2027-04-19"), LocalDate.parse("2027-04-25")))
            .thenReturn(listOf(noResident, noAccount, noUser, valid))
        assertDoesNotThrow { job.runJob() }
        verifyMessage(valid, true)
        verify(reminders).save(any())
        verifyNoMoreInteractions(mail)
    }

    @Test
    fun `dispatch exception does not record a reminder and does not stop other recipients`() {
        val failed = right("failed", LocalDate.parse("2027-04-19"), true)
        val valid = right("valid", LocalDate.parse("2027-04-25"), false)
        whenever(rights.findAllPaattyvatByTimeFrame(LocalDate.parse("2027-04-19"), LocalDate.parse("2027-04-25")))
            .thenReturn(listOf(failed, valid))
        doThrow(IllegalStateException("Dispatch failed")).whenever(mail).sendEmailFromTemplate(
            user = same(failed.erikoistuvaLaakari!!.kayttaja!!.user!!), cc = any(), templateName = any(),
            titleKey = any(), titleProperties = any(), properties = any()
        )
        assertDoesNotThrow { job.runJob() }
        verify(reminders, never()).findOneByErikoistuvaLaakariKayttajaUserId("failed")
        verifyMessage(valid, false)
        val saved = argumentCaptor<OpintooikeusHerate>()
        verify(reminders).save(saved.capture())
        assertThat(saved.firstValue.erikoistuvaLaakari).isSameAs(valid.erikoistuvaLaakari)
    }

    @Test
    fun `persistence exception does not stop reminders for other recipients`() {
        val failed = right("failed", LocalDate.parse("2027-04-19"), true)
        val valid = right("valid", LocalDate.parse("2027-04-25"), false)
        whenever(rights.findAllPaattyvatByTimeFrame(LocalDate.parse("2027-04-19"), LocalDate.parse("2027-04-25")))
            .thenReturn(listOf(failed, valid))
        whenever(reminders.save(any<OpintooikeusHerate>())).thenAnswer { invocation ->
            val reminder = invocation.getArgument<OpintooikeusHerate>(0)
            if (reminder.erikoistuvaLaakari === failed.erikoistuvaLaakari) {
                throw IllegalStateException("Persistence failed")
            }
            reminder
        }
        assertDoesNotThrow { job.runJob() }
        verifyMessage(failed, true)
        verifyMessage(valid, false)
        val saved = argumentCaptor<OpintooikeusHerate>()
        verify(reminders, times(2)).save(saved.capture())
        assertThat(saved.lastValue.erikoistuvaLaakari).isSameAs(valid.erikoistuvaLaakari)
        assertThat(saved.lastValue.paattymassaHerateLahetetty).isEqualTo(now)
    }

    private fun right(userId: String, end: LocalDate, fixedTerm: Boolean) = Opintooikeus(
        opintooikeudenMyontamispaiva = end.minusYears(if (fixedTerm) 3 else 4),
        opintooikeudenPaattymispaiva = end,
        erikoistuvaLaakari = ErikoistuvaLaakari(kayttaja = Kayttaja(user = User(id = userId, email = "$userId@example.test")))
    )

    private fun verifyMessage(right: Opintooikeus, fixedTerm: Boolean, count: Int = 1) {
        val properties = mutableMapOf(
            MailProperty.DATE to right.opintooikeudenPaattymispaiva.toString(),
            MailProperty.URL_PATH to "https://www.laaketieteelliset.fi/ammatillinen-jatkokoulutus/lomakkeet/"
        )
        if (fixedTerm) {
            properties[MailProperty.URL_PATH] = "https://www.laaketieteelliset.fi/ammatillinen-jatkokoulutus/koejakso"
            properties[MailProperty.SECOND_URL_PATH] = "https://www.laaketieteelliset.fi/ammatillinen-jatkokoulutus/lomakkeet/"
        }
        verify(mail, times(count)).sendEmailFromTemplate(
            user = same(right.erikoistuvaLaakari!!.kayttaja!!.user!!), cc = eq(emptyList()),
            templateName = eq(if (fixedTerm) "opintooikeusMaarakainenPaattymassa.html" else "opintooikeusPaattymassa.html"),
            titleKey = eq("email.opintooikeuspaattymassa.title"), titleProperties = argThat { isEmpty() },
            properties = eq(properties)
        )
    }
}
