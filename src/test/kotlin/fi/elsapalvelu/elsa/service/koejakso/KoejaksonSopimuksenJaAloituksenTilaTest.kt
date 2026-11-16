package fi.elsapalvelu.elsa.service.koejakso

import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila
import fi.elsapalvelu.elsa.service.dto.enumeration.KoejaksoTila.*
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonAloituskeskusteluDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonKouluttajaDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoejaksonKoulutussopimusDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoulutussopimuksenKouluttajaDTO
import fi.elsapalvelu.elsa.service.dto.koejakso.KoulutussopimuksenVastuuhenkiloDTO
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

class KoejaksonSopimuksenJaAloituksenTilaTest {
    @ParameterizedTest
    @MethodSource("sopimustapaukset")
    fun `sopimuksen tila huomioi hyvaksyntojen etusijan ja katselijan`(tapaus: Sopimustapaus) {
        assertThat(KoejaksonTilasaannot.maaritaSopimuksenTila(tapaus.sopimus, tapaus.vastuuhenkilo, tapaus.kayttajaId)).isEqualTo(tapaus.odotettu)
    }

    @ParameterizedTest
    @MethodSource("aloitustapaukset")
    fun `aloituksen tila huomioi esimiehen ja palautuksen etusijan`(tapaus: Aloitustapaus) {
        assertThat(KoejaksonTilasaannot.maaritaAloituskeskustelunTila(tapaus.aloitus, tapaus.kayttajaId)).isEqualTo(tapaus.odotettu)
    }

    data class Sopimustapaus(
        val kuvaus: String,
        val sopimus: KoejaksonKoulutussopimusDTO?,
        val odotettu: KoejaksoTila,
        val vastuuhenkilo: Boolean = false,
        val kayttajaId: Long? = null
    ) {
        override fun toString() = kuvaus
    }

    data class Aloitustapaus(val kuvaus: String, val aloitus: KoejaksonAloituskeskusteluDTO?, val odotettu: KoejaksoTila, val kayttajaId: Long? = null) {
        override fun toString() = kuvaus
    }

    companion object {
        @JvmStatic
        fun sopimustapaukset(): List<Sopimustapaus> = listOf(
            Sopimustapaus("puuttuva sopimus", null, UUSI),
            Sopimustapaus("vastuuhenkilon hyvaksynta ohittaa palautuksen ja luonnoksen", sopimus {
                vastuuhenkilo = KoulutussopimuksenVastuuhenkiloDTO(sopimusHyvaksytty = true)
                korjausehdotus = "korjaa"
                lahetetty = false
            }, HYVAKSYTTY),
            Sopimustapaus("kouluttajan palautus ohittaa luonnoksen", sopimus { korjausehdotus = "korjaa"; lahetetty = false }, PALAUTETTU_KORJATTAVAKSI),
            Sopimustapaus("vastuuhenkilon palautus nakyy luonnoksessa", sopimus {
                vastuuhenkilonKorjausehdotus = "korjaa"; lahetetty = false
            }, PALAUTETTU_KORJATTAVAKSI),
            Sopimustapaus("uudelleen lahetetty palautus ei nay erikoistuvalle", sopimus { vastuuhenkilonKorjausehdotus = "korjaa" }, ODOTTAA_HYVAKSYNTAA),
            Sopimustapaus("uudelleen lahetetty palautus nakyy vastuuhenkilolle", sopimus {
                vastuuhenkilonKorjausehdotus = "korjaa"
            }, PALAUTETTU_KORJATTAVAKSI, vastuuhenkilo = true),
            Sopimustapaus("tyhja korjaus ei ole palautus", sopimus { korjausehdotus = " \t"; lahetetty = false }, TALLENNETTU_KESKENERAISENA),
            Sopimustapaus("null lahetystieto ei tarkoita luonnosta", sopimus { lahetetty = null }, ODOTTAA_HYVAKSYNTAA),
            Sopimustapaus("puuttuva kouluttajaluettelo", sopimus { kouluttajat = null }, ODOTTAA_HYVAKSYNTAA),
            Sopimustapaus("tyhja kouluttajaluettelo", sopimus(), ODOTTAA_HYVAKSYNTAA),
            Sopimustapaus("kaikki kouluttajat hyvaksyneet kouluttajan nakymassa", sopimus {
                kouluttajat = mutableSetOf(KoulutussopimuksenKouluttajaDTO(kayttajaId = 1L, sopimusHyvaksytty = true))
            }, ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA, kayttajaId = 1L),
            Sopimustapaus("kaikki kouluttajat hyvaksyneet erikoistuvan nakymassa", sopimus {
                kouluttajat = mutableSetOf(KoulutussopimuksenKouluttajaDTO(kayttajaId = 1L, sopimusHyvaksytty = true))
            }, ODOTTAA_HYVAKSYNTAA),
            Sopimustapaus("vain katsova kouluttaja hyvaksyi", sopimus {
                kouluttajat = mutableSetOf(
                    KoulutussopimuksenKouluttajaDTO(kayttajaId = 1L, sopimusHyvaksytty = true),
                    KoulutussopimuksenKouluttajaDTO(kayttajaId = 2L, sopimusHyvaksytty = false)
                )
            }, ODOTTAA_TOISEN_KOULUTTAJAN_HYVAKSYNTAA, kayttajaId = 1L),
            Sopimustapaus("null tunnus verrataan edelleen null katselijaan", sopimus {
                kouluttajat = mutableSetOf(KoulutussopimuksenKouluttajaDTO(sopimusHyvaksytty = true))
            }, ODOTTAA_VASTUUHENKILON_HYVAKSYNTAA)
        )

        @JvmStatic
        fun aloitustapaukset(): List<Aloitustapaus> = listOf(
            Aloitustapaus("puuttuva aloitus", null, UUSI),
            Aloitustapaus("esimiehen hyvaksynta ohittaa palautuksen", aloitus {
                lahiesimies = KoejaksonKouluttajaDTO(id = 2L, sopimusHyvaksytty = true)
                korjausehdotus = "korjaa"; lahetetty = false
            }, HYVAKSYTTY),
            Aloitustapaus("esimiehen odotus ohittaa palautuksen erikoistuvalle", aloitus {
                lahikouluttaja = KoejaksonKouluttajaDTO(id = 1L, sopimusHyvaksytty = true)
                lahiesimies = KoejaksonKouluttajaDTO(id = 2L)
                korjausehdotus = "korjaa"; lahetetty = false
            }, ODOTTAA_ESIMIEHEN_HYVAKSYNTAA),
            Aloitustapaus("esimies nakee palautuksen", aloitus {
                lahikouluttaja = KoejaksonKouluttajaDTO(id = 1L, sopimusHyvaksytty = true)
                lahiesimies = KoejaksonKouluttajaDTO(id = 2L)
                korjausehdotus = "korjaa"
            }, PALAUTETTU_KORJATTAVAKSI, kayttajaId = 2L),
            Aloitustapaus("palautus ennen luonnosta", aloitus { korjausehdotus = "korjaa"; lahetetty = false }, PALAUTETTU_KORJATTAVAKSI),
            Aloitustapaus("tyhja palautus ja luonnos", aloitus { korjausehdotus = " \t"; lahetetty = false }, TALLENNETTU_KESKENERAISENA),
            Aloitustapaus("null lahetystieto", aloitus { lahetetty = null }, ODOTTAA_HYVAKSYNTAA),
            Aloitustapaus("null esimiehen tunnus ja null katselija", aloitus {
                lahikouluttaja = KoejaksonKouluttajaDTO(sopimusHyvaksytty = true)
            }, ODOTTAA_HYVAKSYNTAA)
        )

        private fun sopimus(muutos: KoejaksonKoulutussopimusDTO.() -> Unit = {}) = KoejaksonKoulutussopimusDTO(lahetetty = true).apply(muutos)
        private fun aloitus(muutos: KoejaksonAloituskeskusteluDTO.() -> Unit = {}) = KoejaksonAloituskeskusteluDTO(lahetetty = true).apply(muutos)
    }
}
