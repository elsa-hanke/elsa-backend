package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.ElsaBackendApp
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.koulutus.Koulutusjakso
import fi.elsapalvelu.elsa.domain.koulutus.Koulutussuunnitelma
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.domain.seuranta.Paivakirjamerkinta
import fi.elsapalvelu.elsa.domain.seuranta.Seurantajakso
import fi.elsapalvelu.elsa.repository.seuranta.SeurantajaksoRepository
import fi.elsapalvelu.elsa.service.dto.enumeration.SeurantajaksoTila
import fi.elsapalvelu.elsa.web.rest.helpers.ErikoistuvaLaakariHelper
import fi.elsapalvelu.elsa.web.rest.helpers.YliopistoHelper
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate

@SpringBootTest(classes = [ElsaBackendApp::class], properties = [
    "spring.datasource.url=jdbc:tc:postgresql:16.9:///cleanupOrchestrator?TC_TMPFS=/testtmpfs:rw&TC_DAEMON=true"
])
@Import(OpintooikeusCleanupOrchestratorIT.TestConfig::class)
class SeurantajaksoCleanupIT {
    @Autowired private lateinit var orchestrator: OpintooikeusCleanupOrchestrator
    @Autowired private lateinit var followUps: SeurantajaksoRepository
    @Autowired private lateinit var em: EntityManager
    @Autowired private lateinit var transactions: TransactionTemplate

    private val originalDate = LocalDate.of(2020, 1, 2)

    @Test
    fun `clears approved and returned follow ups by owner and preserves shared links`() {
        val fixture = fixture()

        orchestrator.clearContent(fixture.rightId)
        orchestrator.clearContent(fixture.rightId)

        transactions.executeWithoutResult { assertFixture(fixture, cleared = true) }
    }

    @Test
    fun `failure after flush rolls back follow ups plan and diary and retry succeeds`() {
        val fixture = fixture()

        assertThrows<IllegalStateException> {
            transactions.executeWithoutResult {
                orchestrator.clearContent(fixture.rightId)
                em.flush()
                em.clear()
                assertFixture(fixture, cleared = true)
                error("Simulated failure after follow-up flush")
            }
        }

        transactions.executeWithoutResult { assertFixture(fixture, cleared = false) }

        orchestrator.clearContent(fixture.rightId)

        transactions.executeWithoutResult { assertFixture(fixture, cleared = true) }
    }

    @Test
    fun `follow up cleanup works without a training plan or diary`() {
        val rightId = requireNotNull(transactions.execute {
            val right = right()
            em.persist(followUp(right, approved = false))
            requireNotNull(right.id)
        })

        orchestrator.clearContent(rightId)

        transactions.executeWithoutResult {
            assertText(followUps.findByOpintooikeusId(rightId).single(), cleared = true)
        }
    }

    private fun fixture(): Fixture = requireNotNull(transactions.execute {
        val right = right()
        val other = right()
        val plan = Koulutussuunnitelma(opintooikeus = right, motivaatiokirje = "Motivation")
        val otherPlan = Koulutussuunnitelma(opintooikeus = other)
        em.persist(plan)
        em.persist(otherPlan)
        val period = Koulutusjakso(
            nimi = "Other right period", muutOsaamistavoitteet = "Goals", koulutussuunnitelma = otherPlan
        )
        em.persist(period)
        val approved = followUp(right, approved = true)
        val returned = followUp(right, approved = false)
        val unrelated = followUp(other, approved = false)
        listOf(approved, returned, unrelated).forEach { followUp ->
            followUp.koulutusjaksot = mutableSetOf(period)
            em.persist(followUp)
        }
        val diary = Paivakirjamerkinta(
            opintooikeus = right, paivamaara = originalDate,
            oppimistapahtumanNimi = "Event", muunAiheenNimi = "Topic", reflektio = "Reflection"
        )
        em.persist(diary)
        em.flush()
        // Spring auditing initializes these dates to today; seed older dates for callback/rollback checks.
        em.createNativeQuery("update seurantajakso set luotu = :date, tallennettu = :date where id in (:ids)")
            .setParameter("date", originalDate)
            .setParameter("ids", listOf(approved.id, returned.id, unrelated.id))
            .executeUpdate()
        Fixture(
            requireNotNull(right.id), requireNotNull(other.id), requireNotNull(plan.id),
            requireNotNull(diary.id), requireNotNull(period.id)
        )
    })

    private fun right(): Opintooikeus {
        val student = ErikoistuvaLaakariHelper.createEntity(
            em, yliopisto = YliopistoHelper.createReferenceData(em, YliopistoEnum.TAMPEREEN_YLIOPISTO)
        )
        return requireNotNull(student.getOpintooikeusKaytossa())
    }

    private fun followUp(right: Opintooikeus, approved: Boolean) = Seurantajakso(
        opintooikeus = right,
        kouluttaja = right.erikoistuvaLaakari?.kayttaja,
        alkamispaiva = originalDate,
        paattymispaiva = originalDate.plusMonths(6),
        seuraavanKeskustelunAjankohta = originalDate.plusMonths(7),
        hyvaksytty = approved,
        edistyminenTavoitteidenMukaista = approved,
        omaArviointi = "Self assessment",
        lisahuomioita = "Notes",
        seuraavanJaksonTavoitteet = "Goals",
        huolenaiheet = "Concerns",
        kouluttajanArvio = "Assessment",
        erikoisalanTyoskentelyvalmiudet = "Readiness",
        jatkotoimetJaRaportointi = "Actions",
        seurantakeskustelunYhteisetMerkinnat = "Shared notes",
        korjausehdotus = "Correction"
    )

    private fun assertFixture(fixture: Fixture, cleared: Boolean) {
        val entries = followUps.findByOpintooikeusId(fixture.rightId)
        assertThat(entries).hasSize(2)
        assertThat(entries.map { it.hyvaksytty }).containsExactlyInAnyOrder(true, false)
        val unrelated = followUps.findByOpintooikeusId(fixture.otherRightId).single()
        for (entry in entries + unrelated) {
            val entryCleared = cleared && entry.opintooikeus?.id == fixture.rightId
            assertText(entry, entryCleared)
            assertThat(entry.alkamispaiva).isEqualTo(originalDate)
            assertThat(entry.paattymispaiva).isEqualTo(originalDate.plusMonths(6))
            assertThat(entry.seuraavanKeskustelunAjankohta).isEqualTo(originalDate.plusMonths(7))
            assertThat(entry.edistyminenTavoitteidenMukaista).isEqualTo(entry.hyvaksytty)
            assertThat(entry.luotu).isEqualTo(originalDate)
            assertThat(entry.tallennettu).isEqualTo(if (entryCleared) LocalDate.now() else originalDate)
            assertThat(entry.kouluttaja?.id).isEqualTo(entry.opintooikeus?.erikoistuvaLaakari?.kayttaja?.id)
            val period = requireNotNull(entry.koulutusjaksot).single()
            assertThat(period.id).isEqualTo(fixture.periodId)
            assertThat(period.koulutussuunnitelma?.opintooikeus?.id).isEqualTo(fixture.otherRightId)
            assertThat(period.nimi).isEqualTo("Other right period")
            assertThat(period.muutOsaamistavoitteet).isEqualTo("Goals")
            assertThat(SeurantajaksoTila.fromSeurantajakso(entry)).isEqualTo(
                if (entry.hyvaksytty) SeurantajaksoTila.HYVAKSYTTY
                else if (entryCleared) SeurantajaksoTila.ODOTTAA_ARVIOINTIA_JA_YHTEISIA_MERKINTOJA
                else SeurantajaksoTila.PALAUTETTU_KORJATTAVAKSI
            )
        }
        assertThat(em.find(Koulutussuunnitelma::class.java, fixture.planId).motivaatiokirje)
            .isEqualTo(if (cleared) null else "Motivation")
        val diary = em.find(Paivakirjamerkinta::class.java, fixture.diaryId)
        assertThat(diary.oppimistapahtumanNimi).isEqualTo(if (cleared) "" else "Event")
        assertThat(diary.muunAiheenNimi).isEqualTo(if (cleared) "" else "Topic")
        assertThat(diary.reflektio).isEqualTo(if (cleared) null else "Reflection")
    }

    private fun assertText(entry: Seurantajakso, cleared: Boolean) {
        assertThat(entry.omaArviointi).isEqualTo(if (cleared) "" else "Self assessment")
        val optionalText = listOf(
            entry.lisahuomioita, entry.seuraavanJaksonTavoitteet, entry.huolenaiheet, entry.kouluttajanArvio,
            entry.erikoisalanTyoskentelyvalmiudet, entry.jatkotoimetJaRaportointi,
            entry.seurantakeskustelunYhteisetMerkinnat, entry.korjausehdotus
        )
        if (cleared) assertThat(optionalText).containsOnlyNulls()
        else assertThat(optionalText).containsExactly("Notes", "Goals", "Concerns", "Assessment", "Readiness", "Actions", "Shared notes", "Correction")
    }

    private data class Fixture(
        val rightId: Long,
        val otherRightId: Long,
        val planId: Long,
        val diaryId: Long,
        val periodId: Long
    )
}
