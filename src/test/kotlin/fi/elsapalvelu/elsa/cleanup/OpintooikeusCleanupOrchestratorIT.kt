package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.AsiakirjaData
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.koulutus.Koulutusjakso
import fi.elsapalvelu.elsa.domain.koulutus.Koulutussuunnitelma
import fi.elsapalvelu.elsa.domain.koulutus.Teoriakoulutus
import fi.elsapalvelu.elsa.domain.seuranta.PaivakirjaAihekategoria
import fi.elsapalvelu.elsa.domain.seuranta.Paivakirjamerkinta
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.koulutus.KoulutussuunnitelmaRepository
import fi.elsapalvelu.elsa.repository.seuranta.PaivakirjamerkintaRepository
import fi.elsapalvelu.elsa.service.constants.OPINTOOIKEUS_NOT_FOUND_ERROR
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.YliopistoHelper
import jakarta.persistence.EntityManager
import jakarta.persistence.EntityNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.transaction.support.TransactionTemplate
import org.ehcache.core.config.DefaultConfiguration
import org.ehcache.jsr107.EhcacheCachingProvider
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import javax.cache.Caching

@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///cleanupOrchestrator?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true"
])
@Import(OpintooikeusCleanupOrchestratorIT.TestConfig::class)
class OpintooikeusCleanupOrchestratorIT {

    @Autowired private lateinit var orchestrator: OpintooikeusCleanupOrchestrator
    @Autowired private lateinit var plans: KoulutussuunnitelmaRepository
    @Autowired private lateinit var diaries: PaivakirjamerkintaRepository
    @Autowired private lateinit var em: EntityManager
    @Autowired private lateinit var transactions: TransactionTemplate

    @Test
    fun `commits cleared narratives and preserves other rights and linked content`() {
        val fixture = fixture()

        orchestrator.clearContent(fixture.rightId)

        transactions.executeWithoutResult {
            val plan = loadPlan(fixture.rightId)
            assertNarrativesCleared(plan)
            assertPreservedFields(plan, fixture)
            assertThat(plan.muokkauspaiva).isEqualTo(LocalDate.now())
            assertNarrativesRetained(loadPlan(fixture.otherRightId))
            assertDiaryCleanup(fixture)
        }
    }

    @Test
    fun `failure after flush rolls back cleanup and allows retry`() {
        val fixture = fixture()

        assertThrows<IllegalStateException> {
            transactions.executeWithoutResult {
                orchestrator.clearContent(fixture.rightId)
                em.flush()
                em.clear()
                assertNarrativesCleared(loadPlan(fixture.rightId))
                assertDiaryCleanup(fixture)
                error("Simulated failure after flush")
            }
        }

        transactions.executeWithoutResult {
            val plan = loadPlan(fixture.rightId)
            assertNarrativesRetained(plan)
            assertThat(plan.muokkauspaiva).isEqualTo(fixture.originalDate)
            assertPreservedFields(plan, fixture)
            assertNarrativesRetained(loadPlan(fixture.otherRightId))
            assertDiaryCleanup(fixture, cleared = false)
        }

        orchestrator.clearContent(fixture.rightId)

        transactions.executeWithoutResult {
            assertNarrativesCleared(loadPlan(fixture.rightId))
            assertDiaryCleanup(fixture)
        }
    }

    @Test
    fun `repeated cleanup is safe`() {
        val fixture = fixture()
        orchestrator.clearContent(fixture.rightId)

        orchestrator.clearContent(fixture.rightId)

        transactions.executeWithoutResult {
            val plan = loadPlan(fixture.rightId)
            assertNarrativesCleared(plan)
            assertPreservedFields(plan, fixture)
            assertDiaryCleanup(fixture)
        }
    }

    @Test
    fun `right without a training plan or diary entries is a no-op`() {
        val rightId = requireNotNull(transactions.execute {
            val student = ErikoistuvaLaakariHelper.createEntity(
                em, yliopisto = YliopistoHelper.createReferenceData(em, YliopistoEnum.TAMPEREEN_YLIOPISTO)
            )
            requireNotNull(student.getOpintooikeusKaytossa()?.id)
        })

        orchestrator.clearContent(rightId)

        transactions.executeWithoutResult {
            assertThat(plans.findOneByOpintooikeusId(rightId)).isNull()
            assertThat(diaries.findAllByOpintooikeusId(rightId)).isEmpty()
            assertThat(em.find(Opintooikeus::class.java, rightId)).isNotNull()
        }
    }

    @Test
    fun `missing right raises not found`() {
        val exception = assertThrows<EntityNotFoundException> {
            orchestrator.clearContent(-1L)
        }

        assertThat(exception.message).isEqualTo(OPINTOOIKEUS_NOT_FOUND_ERROR)
    }

    private fun fixture(): Fixture = requireNotNull(transactions.execute {
        val student = ErikoistuvaLaakariHelper.createEntity(
            em, yliopisto = YliopistoHelper.createReferenceData(em, YliopistoEnum.TAMPEREEN_YLIOPISTO)
        )
        val right = requireNotNull(student.getOpintooikeusKaytossa())
        val otherRight = right.copy(
            id = null,
            yliopistoOpintooikeusId = UUID.randomUUID().toString(),
            kaytossa = false,
            tyoskentelyjaksot = mutableSetOf(),
            teoriakoulutukset = mutableSetOf(),
            opintosuoritukset = mutableSetOf(),
            annetutValtuutukset = mutableSetOf()
        )
        em.persist(otherRight)
        val document = document(right, "Training plan")
        val motivation = document(right, "Motivation letter")
        val plan = plan(right).apply {
            koulutussuunnitelmaAsiakirja = document
            motivaatiokirjeAsiakirja = motivation
        }
        em.persist(plan)
        val period = Koulutusjakso(
            nimi = "Training period",
            muutOsaamistavoitteet = "Period goals",
            koulutussuunnitelma = plan
        )
        em.persist(period)
        em.persist(plan(otherRight))
        val diaryFixture = diaryFixture(right, otherRight)
        em.flush()
        val originalDate = LocalDate.of(2020, 1, 2)
        // The entity's creation callback always uses today; seed an older persisted date.
        em.createNativeQuery("update koulutussuunnitelma set muokkauspaiva = :date where id = :id")
            .setParameter("date", originalDate)
            .setParameter("id", plan.id)
            .executeUpdate()
        Fixture(
            requireNotNull(right.id), requireNotNull(otherRight.id), requireNotNull(plan.id),
            requireNotNull(document.id), requireNotNull(motivation.id), requireNotNull(period.id), originalDate, diaryFixture
        )
    })

    @Test
    fun `diary cleanup does not require a training plan`() {
        val rightId = requireNotNull(transactions.execute {
            val student = ErikoistuvaLaakariHelper.createEntity(
                em, yliopisto = YliopistoHelper.createReferenceData(em, YliopistoEnum.TAMPEREEN_YLIOPISTO)
            )
            val right = requireNotNull(student.getOpintooikeusKaytossa())
            em.persist(diary(right, true))
            requireNotNull(right.id)
        })

        orchestrator.clearContent(rightId)

        transactions.executeWithoutResult {
            assertThat(plans.findOneByOpintooikeusId(rightId)).isNull()
            assertDiaryText(diaries.findAllByOpintooikeusId(rightId).single(), cleared = true)
        }
    }

    private fun diaryFixture(right: Opintooikeus, otherRight: Opintooikeus): DiaryFixture {
        val category = PaivakirjaAihekategoria(nimi = "Shared category", kuvaus = "Category description")
        em.persist(category)
        val training = Teoriakoulutus(
            koulutuksenNimi = "Other right training",
            koulutuksenPaikka = "Training location",
            alkamispaiva = LocalDate.of(2020, 1, 2),
            opintooikeus = otherRight
        )
        em.persist(training)
        val linked = diary(right, true).apply {
            aihekategoriat = mutableSetOf(category)
            teoriakoulutus = training
        }
        val standalone = diary(right, false)
        val other = diary(otherRight, false).apply {
            aihekategoriat = mutableSetOf(category)
            teoriakoulutus = training
        }
        listOf(linked, standalone, other).forEach(em::persist)
        return DiaryFixture(
            requireNotNull(linked.id), requireNotNull(standalone.id),
            requireNotNull(other.id), requireNotNull(category.id), requireNotNull(training.id)
        )
    }

    private fun diary(right: Opintooikeus, privateEntry: Boolean) = Paivakirjamerkinta(
        paivamaara = LocalDate.of(2020, 1, 2),
        oppimistapahtumanNimi = "Learning event",
        muunAiheenNimi = "Custom topic",
        reflektio = "Reflection",
        yksityinen = privateEntry,
        opintooikeus = right
    )

    private fun assertDiaryCleanup(fixture: Fixture, cleared: Boolean = true) {
        val entries = diaries.findAllByOpintooikeusId(fixture.rightId)
        assertThat(entries.map { it.id }).containsExactlyInAnyOrder(fixture.diary.linkedId, fixture.diary.standaloneId)
        for (entry in entries) {
            assertDiaryText(entry, cleared)
            assertThat(entry.opintooikeus?.id).isEqualTo(fixture.rightId)
            assertThat(entry.paivamaara).isEqualTo(LocalDate.of(2020, 1, 2))
            assertThat(entry.yksityinen).isEqualTo(entry.id == fixture.diary.linkedId)
        }
        val linked = entries.single { it.id == fixture.diary.linkedId }
        assertThat(linked.aihekategoriat?.map { it.id }).containsExactly(fixture.diary.categoryId)
        assertThat(linked.aihekategoriat?.single()?.nimi).isEqualTo("Shared category")
        assertThat(linked.aihekategoriat?.single()?.kuvaus).isEqualTo("Category description")
        assertThat(linked.teoriakoulutus?.id).isEqualTo(fixture.diary.trainingId)
        assertThat(linked.teoriakoulutus?.opintooikeus?.id).isEqualTo(fixture.otherRightId)
        assertThat(linked.teoriakoulutus?.koulutuksenNimi).isEqualTo("Other right training")
        assertThat(linked.teoriakoulutus?.koulutuksenPaikka).isEqualTo("Training location")
        val standalone = entries.single { it.id == fixture.diary.standaloneId }
        assertThat(standalone.aihekategoriat).isEmpty()
        assertThat(standalone.teoriakoulutus).isNull()
        val other = diaries.findAllByOpintooikeusId(fixture.otherRightId).single()
        assertThat(other.id).isEqualTo(fixture.diary.otherEntryId)
        assertDiaryText(other, cleared = false)
        assertThat(other.teoriakoulutus?.id).isEqualTo(fixture.diary.trainingId)
        assertThat(other.aihekategoriat?.map { it.id }).containsExactly(fixture.diary.categoryId)
    }

    private fun assertDiaryText(entry: Paivakirjamerkinta, cleared: Boolean) {
        assertThat(entry.oppimistapahtumanNimi).isEqualTo(if (cleared) "" else "Learning event")
        assertThat(entry.muunAiheenNimi).isEqualTo(if (cleared) "" else "Custom topic")
        assertThat(entry.reflektio).isEqualTo(if (cleared) null else "Reflection")
    }

    private fun document(right: Opintooikeus, name: String) = Asiakirja(
        opintooikeus = right,
        nimi = name,
        tyyppi = "application/pdf",
        lisattypvm = LocalDateTime.of(2020, 1, 2, 12, 0),
        asiakirjaData = AsiakirjaData(data = byteArrayOf(1, 2, 3))
    )

    private fun plan(right: Opintooikeus) = Koulutussuunnitelma(
        opintooikeus = right,
        motivaatiokirje = "Motivation",
        motivaatiokirjeYksityinen = true,
        opiskeluJaTyohistoria = "History",
        vahvuudet = "Strengths",
        vahvuudetYksityinen = true,
        tulevaisuudenVisiointi = "Future",
        osaamisenKartuttaminen = "Learning",
        osaamisenKartuttaminenYksityinen = true,
        elamankentta = "Life"
    )

    private fun loadPlan(rightId: Long) = requireNotNull(plans.findOneByOpintooikeusId(rightId))

    private fun assertNarrativesCleared(plan: Koulutussuunnitelma) {
        assertThat(narratives(plan)).containsOnlyNulls()
    }

    private fun assertNarrativesRetained(plan: Koulutussuunnitelma) {
        assertThat(narratives(plan)).containsExactly("Motivation", "History", "Strengths", "Future", "Learning", "Life")
    }

    private fun narratives(plan: Koulutussuunnitelma) = listOf(
        plan.motivaatiokirje, plan.opiskeluJaTyohistoria, plan.vahvuudet,
        plan.tulevaisuudenVisiointi, plan.osaamisenKartuttaminen, plan.elamankentta
    )

    private fun assertPreservedFields(plan: Koulutussuunnitelma, fixture: Fixture) {
        assertThat(plan.id).isEqualTo(fixture.planId)
        assertThat(plan.opintooikeus?.id).isEqualTo(fixture.rightId)
        assertThat(listOf(
            plan.motivaatiokirjeYksityinen, plan.opiskeluJaTyohistoriaYksityinen, plan.vahvuudetYksityinen,
            plan.tulevaisuudenVisiointiYksityinen, plan.osaamisenKartuttaminenYksityinen, plan.elamankenttaYksityinen
        )).containsExactly(true, false, true, false, true, false)
        assertThat(plan.koulutussuunnitelmaAsiakirja?.id).isEqualTo(fixture.documentId)
        assertThat(plan.motivaatiokirjeAsiakirja?.id).isEqualTo(fixture.motivationId)
        assertThat(plan.koulutussuunnitelmaAsiakirja?.nimi).isEqualTo("Training plan")
        assertThat(plan.motivaatiokirjeAsiakirja?.nimi).isEqualTo("Motivation letter")
        for (document in listOf(plan.koulutussuunnitelmaAsiakirja, plan.motivaatiokirjeAsiakirja)) {
            assertThat(document?.asiakirjaData?.data).containsExactly(1, 2, 3)
            assertThat(document?.lisattypvm).isEqualTo(LocalDateTime.of(2020, 1, 2, 12, 0))
        }
        val period = requireNotNull(plan.koulutusjaksot).single()
        assertThat(period.id).isEqualTo(fixture.periodId)
        assertThat(period.nimi).isEqualTo("Training period")
        assertThat(period.muutOsaamistavoitteet).isEqualTo("Period goals")
    }

    private data class Fixture(
        val rightId: Long,
        val otherRightId: Long,
        val planId: Long,
        val documentId: Long,
        val motivationId: Long,
        val periodId: Long,
        val originalDate: LocalDate,
        val diary: DiaryFixture
    )

    private data class DiaryFixture(
        val linkedId: Long,
        val standaloneId: Long,
        val otherEntryId: Long,
        val categoryId: Long,
        val trainingId: Long
    )

    @TestConfiguration(proxyBeanMethods = false)
    class TestConfig {
        @Bean
        fun isolatedCache(customizer: JCacheManagerCustomizer): javax.cache.CacheManager =
            (Caching.getCachingProvider() as EhcacheCachingProvider).getCacheManager(
                URI("urn:elsa:cleanup-orchestrator-it"), DefaultConfiguration(javaClass.classLoader)
            ).also(customizer::customize)
    }
}
