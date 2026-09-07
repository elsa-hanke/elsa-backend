package fi.elsapalvelu.elsa.repository.arkistointi

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJob
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobAsiakirja
import fi.elsapalvelu.elsa.domain.arkistointi.ArkistointiJobStatus
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.web.rest.helpers.AsiakirjaHelper
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@SpringBootTest(classes = [ElsaBackendApp::class])
@Transactional
class ArkistointiJobRepositoryIT {

    @Autowired
    private lateinit var jobRepository: ArkistointiJobRepository

    @Autowired
    private lateinit var asiakirjaRepository: ArkistointiJobAsiakirjaRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `job ja asiakirjaviite tallennetaan ja haetaan`() {
        val asiakirja = AsiakirjaHelper.createEntity(entityManager)
        entityManager.persist(asiakirja)
        val job = uusiJob("tallennus-1")
        job.createdAt = null
        job.updatedAt = null
        job.lisaaAsiakirja(
            ArkistointiJobAsiakirja(
                asiakirja = asiakirja,
                asiakirjatyyppi = RecordType.YHTEENVETO,
                jarjestysnumero = 0,
                filename = "yhteenveto.pdf",
                contentType = "application/pdf",
                sha256 = "a".repeat(64)
            )
        )

        val tallennettu = jobRepository.saveAndFlush(job)
        entityManager.clear()

        val loydetty = jobRepository.findByKey("tallennus-1")
        val asiakirjat = asiakirjaRepository
            .findAllByJobIdOrderByJarjestysnumeroAsc(tallennettu.id!!)

        assertThat(loydetty).isNotNull
        assertThat(loydetty?.createdAt).isNotNull
        assertThat(loydetty?.updatedAt).isNotNull
        assertThat(asiakirjat).hasSize(1)
        assertThat(asiakirjat.single().asiakirja?.id).isEqualTo(asiakirja.id)
        assertThat(asiakirjat.single().job?.id).isEqualTo(tallennettu.id)
    }

    @Test
    fun `samaa key arvoa ei voi tallentaa kahdesti`() {
        jobRepository.saveAndFlush(uusiJob("sama-avain"))

        assertThrows<DataIntegrityViolationException> {
            jobRepository.saveAndFlush(uusiJob("sama-avain"))
        }
    }

    @Test
    fun `updated at paivittyy jobia muutettaessa`() {
        val job = jobRepository.saveAndFlush(uusiJob("paivitysaika"))
        job.updatedAt = Instant.EPOCH
        job.status = ArkistointiJobStatus.KASITTELYSSA

        jobRepository.saveAndFlush(job)

        assertThat(job.updatedAt).isAfter(Instant.EPOCH)
    }

    @Test
    fun `jobit voidaan hakea statuksen perusteella`() {
        val odottava = jobRepository.saveAndFlush(uusiJob("odottava"))
        jobRepository.saveAndFlush(
            uusiJob("lahetetty", ArkistointiJobStatus.LAHETETTY)
        )

        val loydetyt = jobRepository.findAllByStatus(ArkistointiJobStatus.ODOTTAA)

        assertThat(loydetyt.map { it.id }).contains(odottava.id)
        assertThat(loydetyt).allMatch { it.status == ArkistointiJobStatus.ODOTTAA }
    }

    private fun uusiJob(
        key: String,
        status: ArkistointiJobStatus = ArkistointiJobStatus.ODOTTAA
    ) = ArkistointiJob(
        university = YliopistoEnum.TURUN_YLIOPISTO,
        caseType = CaseType.VALMISTUMINEN,
        status = status,
        payload = "{\"asia\":\"valmistuminen\"}",
        key = key
    )
}
