package fi.elsapalvelu.elsa.service.koejakso

import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonVastuuhenkilonArvio
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila.*
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonKouluttajaDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonVastuuhenkilonArvioDTO
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class KoejaksonArvionTilaTest {
    @ParameterizedTest
    @MethodSource("arviotapaukset")
    fun `dto ja kasittelynakyma sailyttavat roolikohtaiset etusijat`(tapaus: Arviotapaus) {
        val kuittaus = if (tapaus.kuitattu) LocalDate.of(2020, 1, 1) else null
        val dto = KoejaksonVastuuhenkilonArvioDTO(
            vastuuhenkilo = KoejaksonKouluttajaDTO(sopimusHyvaksytty = tapaus.hyvaksytty),
            virkailija = KoejaksonKouluttajaDTO(sopimusHyvaksytty = tapaus.virkailijaHyvaksyi),
            erikoistuvanKuittausaika = kuittaus, virkailijanKorjausehdotus = tapaus.virkailijanKorjaus,
            vastuuhenkilonKorjausehdotus = tapaus.vastuuhenkilonKorjaus, koejaksoHyvaksytty = false
        )
        val arvio = KoejaksonVastuuhenkilonArvio(
            vastuuhenkiloHyvaksynyt = tapaus.hyvaksytty, virkailijaHyvaksynyt = tapaus.virkailijaHyvaksyi,
            erikoistuvanKuittausaika = kuittaus, virkailijanKorjausehdotus = tapaus.virkailijanKorjaus,
            vastuuhenkilonKorjausehdotus = tapaus.vastuuhenkilonKorjaus, koejaksoHyvaksytty = false
        )

        assertThat(KoejaksonTilasaannot.maaritaVastuuhenkilonArvionVaihetila(true, dto, tapaus.virkailija, tapaus.vastuuhenkilo)).isEqualTo(tapaus.dtoTila)
        assertThat(KoejaksonTilasaannot.maaritaVastuuhenkilonArvionKasittelytila(arvio, tapaus.virkailija, tapaus.vastuuhenkilo)).isEqualTo(tapaus.kasittelynTila)
    }

    @Test
    fun `vaiheketjun saatavuus tarkistetaan ennen hyvaksyntaa ja puuttuva arvio on uusi`() {
        val arvio = KoejaksonVastuuhenkilonArvioDTO(vastuuhenkilo = KoejaksonKouluttajaDTO(sopimusHyvaksytty = true))
        assertThat(KoejaksonTilasaannot.maaritaVastuuhenkilonArvionVaihetila(false, arvio)).isEqualTo(EI_AKTIIVINEN)
        assertThat(KoejaksonTilasaannot.maaritaVastuuhenkilonArvionVaihetila(true, null)).isEqualTo(UUSI)
        assertThat(KoejaksonTilasaannot.maaritaVastuuhenkilonArvionKasittelytila(null)).isEqualTo(ODOTTAA_HYVAKSYNTAA)
    }

    @Test
    fun `dto puuttuva virkailijan hyvaksynta ei esta kuitatun arvion kasittelya`() {
        val arvio = KoejaksonVastuuhenkilonArvioDTO(
            erikoistuvanKuittausaika = LocalDate.of(2020, 1, 1), virkailijanKorjausehdotus = "korjaa"
        )
        assertThat(KoejaksonTilasaannot.maaritaVastuuhenkilonArvionVaihetila(true, arvio, virkailija = true)).isEqualTo(ODOTTAA_HYVAKSYNTAA)
    }

    data class Arviotapaus(
        val kuvaus: String,
        val dtoTila: KoejaksoTila,
        val kasittelynTila: KoejaksoTila = dtoTila,
        val hyvaksytty: Boolean = false,
        val virkailijaHyvaksyi: Boolean = false,
        val kuitattu: Boolean = false,
        val virkailija: Boolean = false,
        val vastuuhenkilo: Boolean = false,
        val virkailijanKorjaus: String? = null,
        val vastuuhenkilonKorjaus: String? = null
    ) {
        override fun toString() = kuvaus
    }

    companion object {
        @JvmStatic
        fun arviotapaukset(): List<Arviotapaus> = listOf(
            Arviotapaus("kasittelyn hyvaksynta ei tarkoita myonteista koejakson tulosta", HYVAKSYTTY,
                hyvaksytty = true, virkailijanKorjaus = "korjaa", vastuuhenkilo = true, vastuuhenkilonKorjaus = "korjaa"),
            Arviotapaus("virkailijan hyvaksynta ohittaa palautuksen erikoistuvalle", ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA,
                virkailijaHyvaksyi = true, virkailijanKorjaus = "korjaa"),
            Arviotapaus("vastuuhenkilo ei odota omaa hyvaksyntaansa", ODOTTAA_HYVAKSYNTAA, virkailijaHyvaksyi = true, vastuuhenkilo = true),
            Arviotapaus("virkailija nakee uudelleen kuitatun arvion ennen vanhaa palautusta", ODOTTAA_HYVAKSYNTAA,
                kuitattu = true, virkailija = true, virkailijanKorjaus = "korjaa"),
            Arviotapaus("vastuuhenkilo nakee myos tyhjan oman korjausehdotuksen palautuksena", PALAUTETTU_KORJATTAVAKSI,
                vastuuhenkilo = true, vastuuhenkilonKorjaus = ""),
            Arviotapaus("kaksiroolinen virkailija kasittelee kuitatun arvion ennen omaa palautustaan", ODOTTAA_HYVAKSYNTAA,
                kuitattu = true, virkailija = true, vastuuhenkilo = true, vastuuhenkilonKorjaus = "korjaa"),
            Arviotapaus("kaksiroolinen vastuuhenkilo nakee palautuksen virkailijan hyvaksyttyakin", PALAUTETTU_KORJATTAVAKSI,
                virkailijaHyvaksyi = true, virkailija = true, vastuuhenkilo = true, vastuuhenkilonKorjaus = "korjaa"),
            Arviotapaus("kuittaamaton palautus nakyy molemmissa nakymissa", PALAUTETTU_KORJATTAVAKSI, virkailijanKorjaus = "korjaa"),
            Arviotapaus("uudelleen kuittaus piilottaa vanhan palautuksen vain vaiheketjussa", ODOTTAA_HYVAKSYNTAA,
                kasittelynTila = PALAUTETTU_KORJATTAVAKSI, kuitattu = true, virkailijanKorjaus = "korjaa"),
            Arviotapaus("tyhja palaute ei nay erikoistuvalle palautuksena", ODOTTAA_HYVAKSYNTAA, virkailijanKorjaus = " ", vastuuhenkilonKorjaus = ""),
            Arviotapaus("kasittelematon arvio", ODOTTAA_HYVAKSYNTAA)
        )
    }
}
