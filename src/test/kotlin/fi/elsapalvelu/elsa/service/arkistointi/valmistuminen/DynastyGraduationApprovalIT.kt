package fi.elsapalvelu.elsa.service.arkistointi.valmistuminen

import com.github.kagkarlsson.scheduler.Scheduler
import fi.elsapalvelu.elsa.config.ApplicationProperties
import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.security.VASTUUHENKILO
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiAdapter
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiResult
import fi.elsapalvelu.elsa.service.arkistointi.job.delivery.ArkistointiSubmission
import fi.elsapalvelu.elsa.service.arkistointi.job.document.ArkistointiChecksum
import fi.elsapalvelu.elsa.service.arkistointi.support.AsyncArchiveItSupport
import fi.elsapalvelu.elsa.service.dto.arkistointi.CaseType
import fi.elsapalvelu.elsa.service.dto.arkistointi.RecordType
import fi.elsapalvelu.elsa.service.dto.valmistuminen.ValmistumispyyntoHyvaksyntaFormDTO
import fi.elsapalvelu.elsa.service.kayttaja.MailService
import fi.elsapalvelu.elsa.web.rest.convertObjectToJsonBytes
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///dynastyGraduation?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true",
    "application.arkistointi.turku.metadata.cases.valmistuminen.documents.yhteenveto.retention-period=20",
    "application.arkistointi.turku.metadata.cases.valmistuminen.documents.liite.retention-period=20",
    "application.arkistointi.turku.metadata.cases.valmistuminen.documents.laillistamistodistus.retention-period=20",
    "application.arkistointi.uef.metadata.cases.valmistuminen.documents.yhteenveto.retention-period=20",
    "application.arkistointi.uef.metadata.cases.valmistuminen.documents.liite.retention-period=20",
    "application.arkistointi.uef.metadata.cases.valmistuminen.documents.laillistamistodistus.retention-period=20",
    "application.arkistointi.turku.kaytossa=true", "application.arkistointi.uef.kaytossa=true",
    "application.arkistointi.turku.metadata.cases.valmistuminen.async-enabled=true",
    "application.arkistointi.uef.metadata.cases.valmistuminen.async-enabled=true"
])
@Import(DynastyGraduationApprovalIT.Adapters::class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class DynastyGraduationApprovalIT : AsyncArchiveItSupport() {
    @Autowired private lateinit var scheduler: Scheduler
    @Autowired private lateinit var properties: ApplicationProperties
    @MockitoBean lateinit var mail: MailService

    @ParameterizedTest
    @CsvSource("TURUN_YLIOPISTO,EL", "TURUN_YLIOPISTO,EHL", "TURUN_YLIOPISTO,YEK",
        "ITA_SUOMEN_YLIOPISTO,EL", "ITA_SUOMEN_YLIOPISTO,EHL", "ITA_SUOMEN_YLIOPISTO,YEK")
    fun `approval freezes all three documents and worker delivers them`(university: YliopistoEnum, kind: String) {
        val fixture = graduationFixture(kind, university, true)
        transactions.executeWithoutResult { _ ->
            val request = em.find(fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto::class.java, fixture.first)
            request.opintooikeus!!.erikoistuvaLaakari!!.apply {
                if (laillistamistodistus != null) laillistamistodistus = AsiakirjaData(data = certificate.copyOf())
                laillistamispaivanLiitetiedostonNimi = "certificate.pdf"
                laillistamispaivanLiitetiedostonTyyppi = "application/pdf"
            }
        }
        setSecurityContext(fixture.second, VASTUUHENKILO)
        approve(fixture.first).andExpect(status().isOk)
        val job = transactions.execute { _ -> jobs.findByKey("valmistuminen:$university:${fixture.first}")!! }
        val before = jdbc.queryForObject("select count(*) from asiakirja", Long::class.java)!!
        // The student's current certificate changes after approval; the worker must see saved bytes.
        transactions.executeWithoutResult { _ ->
            val request = em.find(fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto::class.java, fixture.first)
            request.opintooikeus!!.erikoistuvaLaakari!!.laillistamistodistus = AsiakirjaData(data = byteArrayOf(1, 2))
        }
        scheduler.triggerCheckForDueExecutions()
        awaitDelivered(job.id!!)
        // Repeating approval must not materialize another source or snapshot.
        approve(fixture.first).andExpect(status().isBadRequest)
        assertThat(jdbc.queryForObject("select count(*) from asiakirja", Long::class.java)).isEqualTo(before)
    }

    @ParameterizedTest
    @CsvSource("TURUN_YLIOPISTO,missing", "ITA_SUOMEN_YLIOPISTO,missing",
        "TURUN_YLIOPISTO,empty", "ITA_SUOMEN_YLIOPISTO,empty",
        "TURUN_YLIOPISTO,metadata", "ITA_SUOMEN_YLIOPISTO,metadata")
    fun `invalid certificate rolls back documents job and schedule`(university: YliopistoEnum, problem: String) {
        val fixture = graduationFixture("YEK", university, problem != "missing")
        transactions.executeWithoutResult { _ ->
            val request = em.find(fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto::class.java, fixture.first)
            request.opintooikeus!!.erikoistuvaLaakari!!.apply {
                if (laillistamistodistus != null) laillistamistodistus = AsiakirjaData(data = certificate.copyOf())
                laillistamispaivanLiitetiedostonNimi = "certificate.pdf"
                laillistamispaivanLiitetiedostonTyyppi = "application/pdf"
            }
        }
        setSecurityContext(fixture.second, VASTUUHENKILO)
        transactions.executeWithoutResult { _ ->
            val request = em.find(fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto::class.java, fixture.first)
            val trainee = request.opintooikeus!!.erikoistuvaLaakari!!
            if (problem == "empty") trainee.laillistamistodistus = AsiakirjaData(data = byteArrayOf())
            if (problem == "metadata") trainee.laillistamispaivanLiitetiedostonNimi = ""
        }
        val before = jdbc.queryForObject("select count(*) from asiakirja", Long::class.java)
        val tasks = jdbc.queryForObject("select count(*) from scheduled_tasks", Long::class.java)
        approve(fixture.first).andExpect(status().isBadRequest)
        assertThat(jobs.findByKey("valmistuminen:$university:${fixture.first}")).isNull()
        assertThat(jdbc.queryForObject("select count(*) from scheduled_tasks", Long::class.java)).isEqualTo(tasks)
        transactions.executeWithoutResult { _ ->
            val request = em.find(fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto::class.java, fixture.first)
            assertThat(request.vastuuhenkiloHyvaksyjaKuittausaika).isNull()
        }
        assertThat(jdbc.queryForObject("select count(*) from asiakirja", Long::class.java)).isEqualTo(before)
    }

    @ParameterizedTest
    @CsvSource("TURUN_YLIOPISTO", "ITA_SUOMEN_YLIOPISTO")
    fun `disabled archive does not require certificate`(university: YliopistoEnum) {
        val fixture = graduationFixture("YEK", university, false)
        setSecurityContext(fixture.second, VASTUUHENKILO)
        val settings = properties.getArkistointi()
        try {
            if (university == YliopistoEnum.TURUN_YLIOPISTO) settings.getTurku().kaytossa = false
            else settings.getUef().kaytossa = false
            approve(fixture.first).andExpect(status().isOk)
            assertThat(jobs.findByKey("valmistuminen:$university:${fixture.first}")).isNull()
        } finally {
            settings.getTurku().kaytossa = true
            settings.getUef().kaytossa = true
        }
    }

    private fun approve(id: Long) = testMockMvc.perform(put("/api/vastuuhenkilo/valmistumispyynnon-hyvaksynta/$id")
        .contentType(MediaType.APPLICATION_JSON).with(csrf())
        .content(convertObjectToJsonBytes(ValmistumispyyntoHyvaksyntaFormDTO(null))))

    companion object {
        private val certificate = fi.elsapalvelu.elsa.service.pdf.PdfTestData.certificate()
    }

    @TestConfiguration(proxyBeanMethods = false)
    class Adapters {
        @Bean
        fun isolatedCache(customizer: org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer): javax.cache.CacheManager =
            (javax.cache.Caching.getCachingProvider() as org.ehcache.jsr107.EhcacheCachingProvider).getCacheManager(
                java.net.URI("urn:elsa:dynasty-graduation-it"), org.ehcache.core.config.DefaultConfiguration(javaClass.classLoader)
            ).also(customizer::customize)

        @Bean fun turku(): ArkistointiAdapter = adapter(YliopistoEnum.TURUN_YLIOPISTO)
        @Bean fun uef(): ArkistointiAdapter = adapter(YliopistoEnum.ITA_SUOMEN_YLIOPISTO)
        private fun adapter(value: YliopistoEnum) = object : ArkistointiAdapter {
            override val university = value
            override val supportedCaseTypes = setOf(CaseType.VALMISTUMINEN)
            override fun arkistoi(pyynto: ArkistointiSubmission): ArkistointiResult {
                assertThat(pyynto.asiakirjat.map { it.asiakirjatyyppi }).containsExactly(
                    RecordType.YHTEENVETO, RecordType.LIITE, RecordType.LAILLISTAMISTODISTUS)
                pyynto.asiakirjat.forEach { assertThat(ArkistointiChecksum.sha256(it.sisalto)).isEqualTo(it.sha256) }
                assertThat(pyynto.asiakirjat.last().sisalto).isEqualTo(certificate)
                return ArkistointiResult.Success("test:${pyynto.jobId}")
            }
        }
    }
}
