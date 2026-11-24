package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.domain.kayttaja.Asiakirja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.koulutus.Koulutusjakso
import fi.elsapalvelu.elsa.domain.koulutus.Koulutussuunnitelma
import fi.elsapalvelu.elsa.domain.koulutus.Teoriakoulutus
import fi.elsapalvelu.elsa.domain.seuranta.PaivakirjaAihekategoria
import fi.elsapalvelu.elsa.domain.seuranta.Paivakirjamerkinta
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

class CleanupOpintoOikeusTest {

    private val cleanup = CleanupOpintoOikeus()

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `clears diary text without changing structural fields or linked content`(yksityinen: Boolean) {
        val category = PaivakirjaAihekategoria(id = 3L, nimi = "Category")
        val training = Teoriakoulutus(id = 4L, koulutuksenNimi = "Training")
        val entry = Paivakirjamerkinta(
            id = 1L,
            paivamaara = LocalDate.of(2020, 1, 2),
            oppimistapahtumanNimi = "Learning event",
            muunAiheenNimi = "Custom topic",
            reflektio = "Reflection",
            yksityinen = yksityinen,
            opintooikeus = Opintooikeus(id = 2L),
            aihekategoriat = mutableSetOf(category),
            teoriakoulutus = training
        )
        val before = entry.copy()

        cleanup.clearPaivakirjamerkintaContent(entry)

        assertDiaryTextCleared(entry)
        assertThat(entry).usingRecursiveComparison()
            .ignoringFields("oppimistapahtumanNimi", "muunAiheenNimi", "reflektio")
            .isEqualTo(before)
        assertThat(entry.opintooikeus).isSameAs(before.opintooikeus)
        assertThat(entry.aihekategoriat).isSameAs(before.aihekategoriat)
        assertThat(entry.aihekategoriat).containsExactly(category)
        assertThat(category.nimi).isEqualTo("Category")
        assertThat(entry.teoriakoulutus).isSameAs(training)
        assertThat(training.koulutuksenNimi).isEqualTo("Training")
    }

    @Test
    fun `normalizes null diary text and accepts absent optional relationships`() {
        val entry = Paivakirjamerkinta(aihekategoriat = null)

        cleanup.clearPaivakirjamerkintaContent(entry)

        assertDiaryTextCleared(entry)
        assertThat(entry.aihekategoriat).isNull()
        assertThat(entry.teoriakoulutus).isNull()
        assertThat(entry.opintooikeus).isNull()
        assertThat(entry.paivamaara).isNull()
        assertThat(entry.id).isNull()
    }

    @Test
    fun `normalizes empty reflection and repeated diary cleanup is unchanged`() {
        val entry = Paivakirjamerkinta(oppimistapahtumanNimi = "", muunAiheenNimi = "", reflektio = "")
        cleanup.clearPaivakirjamerkintaContent(entry)
        assertDiaryTextCleared(entry)
        val afterFirstCleanup = entry.copy()

        cleanup.clearPaivakirjamerkintaContent(entry)

        assertThat(entry).usingRecursiveComparison().isEqualTo(afterFirstCleanup)
    }

    private fun assertDiaryTextCleared(entry: Paivakirjamerkinta) {
        assertThat(entry.oppimistapahtumanNimi).isEmpty()
        assertThat(entry.muunAiheenNimi).isEmpty()
        assertThat(entry.reflektio).isNull()
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `clears all six narratives without changing other fields`(yksityinen: Boolean) {
        val plan = Koulutussuunnitelma(
            id = 1L,
            motivaatiokirje = "Motivation",
            motivaatiokirjeYksityinen = yksityinen,
            opiskeluJaTyohistoria = "Study and work history",
            opiskeluJaTyohistoriaYksityinen = yksityinen,
            vahvuudet = "Strengths",
            vahvuudetYksityinen = yksityinen,
            tulevaisuudenVisiointi = "Future plans",
            tulevaisuudenVisiointiYksityinen = yksityinen,
            osaamisenKartuttaminen = "Learning goals",
            osaamisenKartuttaminenYksityinen = yksityinen,
            elamankentta = "Life circumstances",
            elamankenttaYksityinen = yksityinen,
            opintooikeus = Opintooikeus(id = 2L),
            koulutussuunnitelmaAsiakirja = Asiakirja(id = 3L, nimi = "Training plan"),
            motivaatiokirjeAsiakirja = Asiakirja(id = 4L, nimi = "Motivation letter"),
            koulutusjaksot = mutableSetOf(Koulutusjakso(id = 5L, nimi = "Training period")),
            muokkauspaiva = LocalDate.of(2020, 1, 2)
        )
        val before = plan.copy()

        cleanup.clearKoulutussuunnitelmaContent(plan)

        assertNarrativesCleared(plan)
        assertThat(plan.id).isEqualTo(before.id)
        assertThat(plan.motivaatiokirjeYksityinen).isEqualTo(before.motivaatiokirjeYksityinen)
        assertThat(plan.opiskeluJaTyohistoriaYksityinen).isEqualTo(before.opiskeluJaTyohistoriaYksityinen)
        assertThat(plan.vahvuudetYksityinen).isEqualTo(before.vahvuudetYksityinen)
        assertThat(plan.tulevaisuudenVisiointiYksityinen).isEqualTo(before.tulevaisuudenVisiointiYksityinen)
        assertThat(plan.osaamisenKartuttaminenYksityinen).isEqualTo(before.osaamisenKartuttaminenYksityinen)
        assertThat(plan.elamankenttaYksityinen).isEqualTo(before.elamankenttaYksityinen)
        assertThat(plan.opintooikeus).isSameAs(before.opintooikeus)
        assertThat(plan.koulutussuunnitelmaAsiakirja).isSameAs(before.koulutussuunnitelmaAsiakirja)
        assertThat(plan.motivaatiokirjeAsiakirja).isSameAs(before.motivaatiokirjeAsiakirja)
        assertThat(plan.koulutusjaksot).isSameAs(before.koulutusjaksot)
        assertThat(plan.koulutusjaksot).hasSize(1)
        assertThat(plan.koulutusjaksot?.single()?.nimi).isEqualTo("Training period")
        assertThat(plan.koulutussuunnitelmaAsiakirja?.nimi).isEqualTo("Training plan")
        assertThat(plan.motivaatiokirjeAsiakirja?.nimi).isEqualTo("Motivation letter")
        assertThat(plan.muokkauspaiva).isEqualTo(before.muokkauspaiva)
    }

    @Test
    fun `accepts already null narratives and absent optional relationships`() {
        val plan = Koulutussuunnitelma(koulutusjaksot = null)
        val before = plan.copy()

        cleanup.clearKoulutussuunnitelmaContent(plan)

        assertNarrativesCleared(plan)
        assertThat(plan).usingRecursiveComparison().isEqualTo(before)
    }

    @Test
    fun `normalizes empty strings to null`() {
        val plan = Koulutussuunnitelma(
            motivaatiokirje = "",
            opiskeluJaTyohistoria = "",
            vahvuudet = "",
            tulevaisuudenVisiointi = "",
            osaamisenKartuttaminen = "",
            elamankentta = ""
        )

        cleanup.clearKoulutussuunnitelmaContent(plan)

        assertNarrativesCleared(plan)
    }

    @Test
    fun `clears mixed content and repeated cleanup leaves the plan unchanged`() {
        val plan = Koulutussuunnitelma(
            motivaatiokirje = "Motivation",
            opiskeluJaTyohistoria = null,
            vahvuudet = "",
            tulevaisuudenVisiointi = "Future plans",
            osaamisenKartuttaminen = null,
            elamankentta = ""
        )
        cleanup.clearKoulutussuunnitelmaContent(plan)
        assertNarrativesCleared(plan)
        val afterFirstCleanup = plan.copy()

        cleanup.clearKoulutussuunnitelmaContent(plan)

        assertNarrativesCleared(plan)
        assertThat(plan).usingRecursiveComparison().isEqualTo(afterFirstCleanup)
    }

    private fun assertNarrativesCleared(plan: Koulutussuunnitelma) {
        assertThat(plan.motivaatiokirje).isNull()
        assertThat(plan.opiskeluJaTyohistoria).isNull()
        assertThat(plan.vahvuudet).isNull()
        assertThat(plan.tulevaisuudenVisiointi).isNull()
        assertThat(plan.osaamisenKartuttaminen).isNull()
        assertThat(plan.elamankentta).isNull()
    }
}
