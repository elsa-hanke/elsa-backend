package fi.elsapalvelu.elsa.scheduler

import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.OpintooikeusRepository
import fi.elsapalvelu.elsa.scheduler.jobs.ScheduledTerveyskeskuskoulutusjaksoSuoritusmerkinta
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.service.kayttaja.OpintooikeusService
import fi.elsapalvelu.elsa.service.koulutus.OpintosuoritusService
import fi.elsapalvelu.elsa.service.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksyntaService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.*
import org.thymeleaf.spring6.SpringTemplateEngine
import org.thymeleaf.context.Context
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import java.util.Locale
import java.util.Optional
import java.util.ResourceBundle

class ScheduledTerveyskeskuskoulutusjaksoSuoritusmerkintaTest {
    private val studyRights: OpintooikeusService = mock()
    private val approvals: TerveyskeskuskoulutusjaksonHyvaksyntaService = mock()
    private val mail: MailService = mock()
    private val users: KayttajaRepository = mock()
    private val repository: OpintooikeusRepository = mock()
    private val credits: OpintosuoritusService = mock()
    private val job = ScheduledTerveyskeskuskoulutusjaksoSuoritusmerkinta(
        studyRights, approvals, mail, users, repository, credits
    )

    @Test
    fun `empty candidate list does not perform completion checks or send mail`() {
        whenever(studyRights.findAllByTerveyskoulutusjaksoSuorittamatta()).thenReturn(emptyList())
        job.check()
        verifyNoInteractions(credits, approvals, repository, users, mail)
    }

    @Test
    fun `neither completion source satisfied leaves study right pending`() {
        val right = candidate()
        job.runJob()
        verify(credits).getTerveyskoulutusjaksoSuoritettu(1L, 101L)
        verify(approvals).getTerveyskoulutusjaksoSuoritettu(1L)
        assertThat(right.terveyskoulutusjaksoSuoritettu).isFalse()
        verifyNoInteractions(repository, users, mail)
    }

    @Test
    fun `existing academic credit marks completion without requesting an email or checking approval`() {
        val right = candidate()
        whenever(credits.getTerveyskoulutusjaksoSuoritettu(1L, 101L)).thenReturn(true)
        job.runJob()
        assertThat(right.terveyskoulutusjaksoSuoritettu).isTrue()
        verify(repository).save(same(right))
        verifyNoInteractions(approvals, users, mail)
    }

    @Test
    fun `approved period marks completion and notifies the trainee with the correct template and link`() {
        val right = approvedCandidate()
        job.runJob()
        assertThat(right.terveyskoulutusjaksoSuoritettu).isTrue()
        verify(repository).save(same(right))
        verifyNotification(right)
        verifyNoMoreInteractions(mail)

        // Render the actual selected resource: a renamed template or broken action URL must fail this test.
        val template = argumentCaptor<String>()
        val title = argumentCaptor<String>()
        verify(mail).sendEmailFromTemplate(
            user = any(), cc = any(), templateName = template.capture(), titleKey = title.capture(),
            titleProperties = any(), properties = any()
        )
        val resolver = ClassLoaderTemplateResolver().apply {
            prefix = "templates/mail/"
            characterEncoding = "UTF-8"
        }
        val engine = SpringTemplateEngine().apply { setTemplateResolver(resolver) }
        val locale = Locale.forLanguageTag("fi")
        val body = engine.process(template.firstValue, Context(locale).apply {
            setVariable("baseUrl", "https://elsa.example.test")
        })
        assertThat(body).contains(
            "Sinulla on mahdollisuus hakea suoritusmerkintää terveyskeskuskoulutusjaksosta.",
            "href=\"https://elsa.example.test/tyoskentelyjaksot/terveyskeskuskoulutusjakson-hyvaksynta\""
        ).doesNotContain("\${", "th:href")
        assertThat(ResourceBundle.getBundle("i18n/messages", locale).getString(title.firstValue))
            .isEqualTo("[ELSA-palvelu] Sinulla on mahdollisuus hakea suoritusmerkintää terveyskeskuskoulutusjaksosta")
    }

    @Test
    fun `batch uses each study rights own trainee and only notifies eligible trainees`() {
        val credited = right(1L)
        val approved = right(2L)
        val pending = right(3L)
        whenever(studyRights.findAllByTerveyskoulutusjaksoSuorittamatta()).thenReturn(listOf(credited, approved, pending))
        whenever(credits.getTerveyskoulutusjaksoSuoritettu(1L, 101L)).thenReturn(true)
        whenever(approvals.getTerveyskoulutusjaksoSuoritettu(2L)).thenReturn(true)
        whenever(users.findById(202L)).thenReturn(Optional.of(approved.erikoistuvaLaakari!!.kayttaja!!))
        job.runJob()
        verify(credits).getTerveyskoulutusjaksoSuoritettu(2L, 102L)
        verify(credits).getTerveyskoulutusjaksoSuoritettu(3L, 103L)
        verify(repository).save(same(credited))
        verify(repository).save(same(approved))
        verify(repository, never()).save(same(pending))
        assertThat(pending.terveyskoulutusjaksoSuoritettu).isFalse()
        verifyNotification(approved)
        verifyNoMoreInteractions(mail, users, repository)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `completed study right is excluded on next run so notification is not repeated`(academicCredit: Boolean) {
        val right = right(1L)
        // Model the candidate service's contract: only study rights with the completion flag false are returned.
        whenever(studyRights.findAllByTerveyskoulutusjaksoSuorittamatta()).thenAnswer {
            listOf(right).filterNot { it.terveyskoulutusjaksoSuoritettu }
        }
        whenever(credits.getTerveyskoulutusjaksoSuoritettu(1L, 101L)).thenReturn(academicCredit)
        if (!academicCredit) {
            whenever(approvals.getTerveyskoulutusjaksoSuoritettu(1L)).thenReturn(true)
            whenever(users.findById(201L)).thenReturn(Optional.of(right.erikoistuvaLaakari!!.kayttaja!!))
        }
        job.runJob()
        job.runJob()
        verify(repository).save(same(right))
        verify(credits).getTerveyskoulutusjaksoSuoritettu(1L, 101L)
        if (academicCredit) verifyNoInteractions(mail) else verifyNotification(right)
        verifyNoMoreInteractions(mail)
    }

    @Test
    fun `candidate lookup failure is caught without checking completion or sending mail`() {
        whenever(studyRights.findAllByTerveyskoulutusjaksoSuorittamatta()).thenThrow(IllegalStateException("Lookup failed"))
        assertDoesNotThrow { job.runJob() }
        verifyNoInteractions(credits, approvals, repository, users, mail)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `completion lookup failure leaves flag unchanged and does not send mail`(creditLookupFails: Boolean) {
        val right = candidate()
        if (creditLookupFails) {
            whenever(credits.getTerveyskoulutusjaksoSuoritettu(1L, 101L)).thenThrow(IllegalStateException("Credit lookup failed"))
        } else {
            whenever(approvals.getTerveyskoulutusjaksoSuoritettu(1L)).thenThrow(IllegalStateException("Approval lookup failed"))
        }
        assertDoesNotThrow { job.runJob() }
        assertThat(right.terveyskoulutusjaksoSuoritettu).isFalse()
        verifyNoInteractions(repository, users, mail)
    }

    @Test
    fun `save failure is caught and notification is not requested`() {
        val right = approvedCandidate()
        whenever(repository.save(same(right))).thenThrow(IllegalStateException("Save failed"))
        assertDoesNotThrow { job.runJob() }
        verify(repository).save(same(right))
        verifyNoInteractions(users, mail)
    }

    @Test
    fun `missing recipient is caught without dispatching mail`() {
        val right = candidate()
        whenever(approvals.getTerveyskoulutusjaksoSuoritettu(1L)).thenReturn(true)
        whenever(users.findById(201L)).thenReturn(Optional.empty())
        assertDoesNotThrow { job.runJob() }
        verify(repository).save(same(right))
        verifyNoInteractions(mail)
    }

    @Test
    fun `synchronous dispatch failure is caught after completion was saved`() {
        val right = approvedCandidate()
        doThrow(IllegalStateException("Dispatch failed")).whenever(mail).sendEmailFromTemplate(
            user = any(), cc = any(), templateName = any(), titleKey = any(), titleProperties = any(), properties = any()
        )
        assertDoesNotThrow { job.runJob() }
        inOrder(repository, mail) {
            verify(repository).save(same(right))
            verify(mail).sendEmailFromTemplate(
                user = any(), cc = any(), templateName = any(), titleKey = any(), titleProperties = any(), properties = any()
            )
        }
        // SMTP delivery happens outside this job; no successful-delivery assertion is made here.
    }

    private fun candidate(): Opintooikeus = right(1L).also {
        whenever(studyRights.findAllByTerveyskoulutusjaksoSuorittamatta()).thenReturn(listOf(it))
    }

    private fun approvedCandidate(): Opintooikeus = candidate().also {
        whenever(approvals.getTerveyskoulutusjaksoSuoritettu(1L)).thenReturn(true)
        whenever(users.findById(201L)).thenReturn(Optional.of(it.erikoistuvaLaakari!!.kayttaja!!))
    }

    private fun right(id: Long) = Opintooikeus(
        id = id, terveyskoulutusjaksoSuoritettu = false,
        erikoistuvaLaakari = ErikoistuvaLaakari(
            id = 100L + id, kayttaja = Kayttaja(
                id = 200L + id, user = User(id = "trainee-$id", email = "trainee-$id@example.test")
            )
        )
    )

    private fun verifyNotification(right: Opintooikeus) {
        verify(users).findById(right.erikoistuvaLaakari!!.kayttaja!!.id!!)
        verify(mail).sendEmailFromTemplate(
            user = same(right.erikoistuvaLaakari!!.kayttaja!!.user!!), cc = eq(emptyList()),
            templateName = eq("tkkjaksonSuoritusmerkintaHaettavissa.html"),
            titleKey = eq("email.tkkjaksonsuoritusmerkintahaettavissa.title"),
            titleProperties = argThat { isEmpty() }, properties = eq(emptyMap())
        )
    }
}
