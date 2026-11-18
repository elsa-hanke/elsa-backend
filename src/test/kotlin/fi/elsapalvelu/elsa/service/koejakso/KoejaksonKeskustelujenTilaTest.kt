package fi.elsapalvelu.elsa.service.koejakso

import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila.*
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonKehittamistoimenpiteetDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonKouluttajaDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonLoppukeskusteluDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonValiarviointiDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class KoejaksonKeskustelujenTilaTest {
    @ParameterizedTest
    @MethodSource("keskustelutapaukset")
    fun `kolmen keskusteluvaiheen nykyiset tilat ja etusijat`(tapaus: Keskustelutapaus) {
        val tulos = when (tapaus.vaihe) {
            Vaihe.VALIARVIOINTI -> KoejaksonTilasaannot.maaritaValiarvioinninTila(tapaus.aktiivinen,
                if (tapaus.olemassa) KoejaksonValiarviointiDTO(
                    lahikouluttaja = tapaus.kouluttaja, lahiesimies = tapaus.esimies, korjausehdotus = tapaus.korjaus
                ) else null, tapaus.kayttajaId)
            Vaihe.KEHITTAMISTOIMENPITEET -> KoejaksonTilasaannot.maaritaKehittamistoimenpiteidenTila(tapaus.aktiivinen,
                if (tapaus.olemassa) KoejaksonKehittamistoimenpiteetDTO(
                    lahikouluttaja = tapaus.kouluttaja, lahiesimies = tapaus.esimies, korjausehdotus = tapaus.korjaus
                ) else null, tapaus.kayttajaId)
            Vaihe.LOPPUKESKUSTELU -> KoejaksonTilasaannot.maaritaLoppukeskustelunTila(tapaus.aktiivinen,
                if (tapaus.olemassa) KoejaksonLoppukeskusteluDTO(
                    lahikouluttaja = tapaus.kouluttaja, lahiesimies = tapaus.esimies, korjausehdotus = tapaus.korjaus
                ) else null, tapaus.kayttajaId)
        }
        assertThat(tulos).isEqualTo(tapaus.odotettu)
    }

    enum class Vaihe { VALIARVIOINTI, KEHITTAMISTOIMENPITEET, LOPPUKESKUSTELU }

    data class Keskustelutapaus(
        val kuvaus: String,
        val odotettu: KoejaksoTila,
        val aktiivinen: Boolean = true,
        val olemassa: Boolean = true,
        val kouluttaja: KoejaksonKouluttajaDTO? = null,
        val esimies: KoejaksonKouluttajaDTO? = null,
        val korjaus: String? = null,
        val kayttajaId: Long? = null,
        val vaihe: Vaihe = Vaihe.VALIARVIOINTI
    ) {
        override fun toString() = "$vaihe: $kuvaus"
    }

    companion object {
        @JvmStatic
        fun keskustelutapaukset(): List<Keskustelutapaus> {
            val kouluttaja = KoejaksonKouluttajaDTO(id = 1L, sopimusHyvaksytty = true)
            val esimies = KoejaksonKouluttajaDTO(id = 2L, sopimusHyvaksytty = true)
            val tapaukset = listOf(
                Keskustelutapaus("edeltava vaihe ohittaa jo hyvaksytyn vaiheen", EI_AKTIIVINEN, aktiivinen = false, esimies = esimies),
                Keskustelutapaus("puuttuva vaihe on epaaktiivinen ennen edeltavaa vaihetta", EI_AKTIIVINEN, aktiivinen = false, olemassa = false),
                Keskustelutapaus("puuttuva aktiivinen vaihe", UUSI, olemassa = false),
                Keskustelutapaus("esimiehen hyvaksynta ohittaa palautuksen", HYVAKSYTTY, esimies = esimies, korjaus = "korjaa", kayttajaId = 1L),
                Keskustelutapaus("kouluttaja odottaa esimiesta ennen palautusta", ODOTTAA_ESIMIEHEN_HYVAKSYNTAA,
                    kouluttaja = kouluttaja, korjaus = "korjaa", kayttajaId = 1L),
                Keskustelutapaus("esimies nakee palautuksen", PALAUTETTU_KORJATTAVAKSI,
                    kouluttaja = kouluttaja, esimies = KoejaksonKouluttajaDTO(id = 2L), korjaus = "korjaa", kayttajaId = 2L),
                Keskustelutapaus("ulkopuolinen ei nae palautusta", ODOTTAA_HYVAKSYNTAA,
                    kouluttaja = kouluttaja, esimies = esimies.copy(sopimusHyvaksytty = false), korjaus = "korjaa", kayttajaId = 3L),
                Keskustelutapaus("erikoistuva ei odota esimiesta kouluttajan tavoin", ODOTTAA_HYVAKSYNTAA, kouluttaja = kouluttaja),
                Keskustelutapaus("tyhja korjausehdotus ei ole palautus", ODOTTAA_HYVAKSYNTAA, korjaus = " \t", kayttajaId = 1L),
                Keskustelutapaus("tallennettu aktiivinen vaihe odottaa hyvaksyntaa", ODOTTAA_HYVAKSYNTAA),
                Keskustelutapaus("null tunnukset vastaavat null katselijaa palautuksessa", PALAUTETTU_KORJATTAVAKSI, korjaus = "korjaa")
            )
            return Vaihe.entries.flatMap { vaihe -> tapaukset.map { it.copy(vaihe = vaihe) } }
        }
    }
}
