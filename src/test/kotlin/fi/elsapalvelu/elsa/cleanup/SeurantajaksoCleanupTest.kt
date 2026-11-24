package fi.elsapalvelu.elsa.cleanup

import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.koulutus.Koulutusjakso
import fi.elsapalvelu.elsa.domain.seuranta.Seurantajakso
import fi.elsapalvelu.elsa.service.dto.enumeration.SeurantajaksoTila
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDate

class SeurantajaksoCleanupTest {
    private val cleanup = CleanupOpintoOikeus()

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `clears all text without changing approval structural fields or linked content`(approved: Boolean) {
        val period = Koulutusjakso(id = 3L, nimi = "Period", muutOsaamistavoitteet = "Goals")
        val followUp = Seurantajakso(
            id = 1L,
            alkamispaiva = LocalDate.of(2020, 1, 1),
            paattymispaiva = LocalDate.of(2020, 6, 1),
            omaArviointi = "Self assessment",
            lisahuomioita = "Notes",
            seuraavanJaksonTavoitteet = "Next goals",
            huolenaiheet = "Concerns",
            kouluttajanArvio = "Educator assessment",
            erikoisalanTyoskentelyvalmiudet = "Readiness",
            jatkotoimetJaRaportointi = "Next actions",
            seurantakeskustelunYhteisetMerkinnat = "Shared notes",
            korjausehdotus = "Correction",
            hyvaksytty = approved,
            edistyminenTavoitteidenMukaista = false,
            seuraavanKeskustelunAjankohta = LocalDate.of(2020, 7, 1),
            luotu = LocalDate.of(2020, 1, 2),
            tallennettu = LocalDate.of(2020, 2, 1),
            opintooikeus = Opintooikeus(id = 2L),
            kouluttaja = Kayttaja(id = 4L),
            koulutusjaksot = mutableSetOf(period)
        )
        val before = followUp.copy()

        cleanup.clearSeurantajaksoContent(followUp)

        assertTextCleared(followUp)
        assertThat(followUp).usingRecursiveComparison().ignoringFields(
            "omaArviointi", "lisahuomioita", "seuraavanJaksonTavoitteet", "huolenaiheet",
            "kouluttajanArvio", "erikoisalanTyoskentelyvalmiudet", "jatkotoimetJaRaportointi",
            "seurantakeskustelunYhteisetMerkinnat", "korjausehdotus"
        ).isEqualTo(before)
        assertThat(followUp.opintooikeus).isSameAs(before.opintooikeus)
        assertThat(followUp.kouluttaja).isSameAs(before.kouluttaja)
        assertThat(followUp.koulutusjaksot).isSameAs(before.koulutusjaksot)
        assertThat(period.nimi).isEqualTo("Period")
        assertThat(period.muutOsaamistavoitteet).isEqualTo("Goals")
        assertThat(SeurantajaksoTila.fromSeurantajakso(followUp)).isEqualTo(
            if (approved) SeurantajaksoTila.HYVAKSYTTY else SeurantajaksoTila.ODOTTAA_ARVIOINTIA_JA_YHTEISIA_MERKINTOJA
        )
    }

    @Test
    fun `null input fields and absent relationships are safe and cleanup is idempotent`() {
        val followUp = Seurantajakso(koulutusjaksot = null)

        cleanup.clearSeurantajaksoContent(followUp)
        assertTextCleared(followUp)
        val once = followUp.copy()
        cleanup.clearSeurantajaksoContent(followUp)

        assertThat(followUp).usingRecursiveComparison().isEqualTo(once)
        assertThat(followUp.koulutusjaksot).isNull()
        assertThat(followUp.edistyminenTavoitteidenMukaista).isNull()
    }

    @Test
    fun `empty optional text is normalized to null`() {
        val followUp = Seurantajakso(
            omaArviointi = "", lisahuomioita = "", seuraavanJaksonTavoitteet = "", huolenaiheet = "",
            kouluttajanArvio = "", erikoisalanTyoskentelyvalmiudet = "", jatkotoimetJaRaportointi = "",
            seurantakeskustelunYhteisetMerkinnat = "", korjausehdotus = "", edistyminenTavoitteidenMukaista = true
        )

        cleanup.clearSeurantajaksoContent(followUp)

        assertTextCleared(followUp)
        assertThat(followUp.edistyminenTavoitteidenMukaista).isTrue()
    }

    private fun assertTextCleared(followUp: Seurantajakso) {
        assertThat(followUp.omaArviointi).isEmpty()
        assertThat(listOf(
            followUp.lisahuomioita, followUp.seuraavanJaksonTavoitteet, followUp.huolenaiheet,
            followUp.kouluttajanArvio, followUp.erikoisalanTyoskentelyvalmiudet, followUp.jatkotoimetJaRaportointi,
            followUp.seurantakeskustelunYhteisetMerkinnat, followUp.korjausehdotus
        )).containsOnlyNulls()
}
}
