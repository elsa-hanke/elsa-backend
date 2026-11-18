package fi.elsapalvelu.elsa.domain

import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonAloituskeskustelu
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonKehittamistoimenpiteet
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonKoulutussopimus
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonLoppukeskustelu
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonValiarviointi
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonVastuuhenkilonArvio
import fi.elsapalvelu.elsa.domain.koulutus.Koulutussuunnitelma
import fi.elsapalvelu.elsa.domain.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksynta
import fi.elsapalvelu.elsa.domain.valmistuminen.ValmistumispyynnonTarkistus
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import fi.elsapalvelu.elsa.service.helpers.KellonTestitapaus
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class MuokkauspaivanKelloTest {
    @ParameterizedTest
    @MethodSource("tapaukset")
    fun `luonti ja paivitys asettavat muokkauspaivan kellosta`(tapaus: Entiteettitapaus) {
        val entiteetti = tapaus.luo()
        val kuuntelija = MuokkauspaivanKuuntelija(tapaus.aika.kello())

        kuuntelija.paivita(entiteetti)
        assertThat(entiteetti.muokkauspaiva).isEqualTo(tapaus.aika.paiva)
        entiteetti.muokkauspaiva = LocalDate.of(2018, 1, 1)
        kuuntelija.paivita(entiteetti)
        assertThat(entiteetti.muokkauspaiva).isEqualTo(tapaus.aika.paiva)
    }

    data class Entiteettitapaus(val aika: KellonTestitapaus, val nimi: String, val luo: () -> Muokkauspaivallinen) {
        override fun toString() = "$nimi / $aika"
    }
    companion object {
        @JvmStatic
        fun tapaukset(): List<Entiteettitapaus> {
            val entiteetit: List<Pair<String, () -> Muokkauspaivallinen>> = listOf(
                "aloituskeskustelu" to { KoejaksonAloituskeskustelu() }, "valiarviointi" to { KoejaksonValiarviointi() },
                "kehittamistoimenpiteet" to { KoejaksonKehittamistoimenpiteet() }, "loppukeskustelu" to { KoejaksonLoppukeskustelu() },
                "koulutussopimus" to { KoejaksonKoulutussopimus() }, "vastuuhenkilon arvio" to { KoejaksonVastuuhenkilonArvio() },
                "koulutussuunnitelma" to { Koulutussuunnitelma() }, "valmistumispyynto" to { Valmistumispyynto() },
                "valmistumispyynnon tarkistus" to { ValmistumispyynnonTarkistus() }, "terveyskeskuskoulutusjakso" to { TerveyskeskuskoulutusjaksonHyvaksynta() }
            )
            return KellonTestitapaus.tapaukset().flatMap { aika -> entiteetit.map { Entiteettitapaus(aika, it.first, it.second) } }
        }
    }
}
