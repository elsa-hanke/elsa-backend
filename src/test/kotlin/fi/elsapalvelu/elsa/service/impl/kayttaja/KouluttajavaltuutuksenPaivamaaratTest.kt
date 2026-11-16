package fi.elsapalvelu.elsa.service.impl.kayttaja

import fi.elsapalvelu.elsa.domain.kayttaja.Authority
import fi.elsapalvelu.elsa.domain.kayttaja.ErikoistuvaLaakari
import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.Kouluttajavaltuutus
import fi.elsapalvelu.elsa.domain.kayttaja.Opintooikeus
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.repository.kayttaja.ErikoistuvaLaakariRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KayttajaRepository
import fi.elsapalvelu.elsa.repository.kayttaja.KouluttajavaltuutusRepository
import fi.elsapalvelu.elsa.security.KOULUTTAJA
import fi.elsapalvelu.elsa.service.dto.kayttaja.KayttajaDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.KouluttajavaltuutusDTO
import fi.elsapalvelu.elsa.service.helpers.KELLON_TESTITAPAUKSET
import fi.elsapalvelu.elsa.service.helpers.KellonTestitapaus
import fi.elsapalvelu.elsa.service.mapper.kayttaja.KouluttajavaltuutusMapper
import java.time.Instant
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class KouluttajavaltuutuksenPaivamaaratTest {
    private val valtuutukset = mock<KouluttajavaltuutusRepository>()
    private val valtuutusMapper = mock<KouluttajavaltuutusMapper>()
    private val erikoistujat = mock<ErikoistuvaLaakariRepository>()
    private val kayttajat = mock<KayttajaRepository>()
    private val kouluttaja = Kayttaja(id = 2L, user = User(id = "kouluttaja", authorities = mutableSetOf(Authority(KOULUTTAJA))))
    private val opintooikeus = Opintooikeus(id = 11L, kaytossa = true,
        erikoisala = Erikoisala(nimi = "Yleislääketiede"), yliopisto = Yliopisto(nimi = YliopistoEnum.TURUN_YLIOPISTO))
    private val aiempiAika: Instant = Instant.parse("2019-01-01T00:00:00Z")

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `uuden valtuutuksen luonti ja muokkausaika tulevat kellosta`(aika: KellonTestitapaus) {
        val valtuutus = valmisteleValtuutus()
        val lomake = KouluttajavaltuutusDTO(valtuutettu = KayttajaDTO(id = 2L))

        palvelu(aika).save("erikoistuja", lomake, false)

        assertThat(valtuutus.valtuutuksenLuontiaika).isEqualTo(aika.aikaleima())
        assertThat(valtuutus.valtuutuksenMuokkausaika).isEqualTo(aika.aikaleima())
        assertThat(valtuutus.valtuuttajaOpintooikeus).isSameAs(opintooikeus)
    }

    @ParameterizedTest
    @MethodSource("muokkaustapaukset")
    fun `olemassa olevan valtuutuksen jatkaminen sailyttaa luontiajan`(tapaus: Muokkaustapaus) {
        val valtuutus = valmisteleValtuutus().apply {
            id = 1L
            valtuuttajaOpintooikeus = opintooikeus
            valtuutuksenLuontiaika = aiempiAika
        }
        val lomake = KouluttajavaltuutusDTO(id = if (tapaus.tunnisteella) 1L else null,
            valtuutettu = KayttajaDTO(id = 2L), paattymispaiva = tapaus.aika.paiva.plusMonths(6))
        whenever(valtuutukset.findById(1L)).thenReturn(Optional.of(valtuutus))
        whenever(valtuutukset.findByValtuuttajaOpintooikeusIdAndValtuutettuUserId(11L, "kouluttaja")).thenReturn(Optional.of(valtuutus))
        if (!tapaus.tunnisteella) whenever(valtuutusMapper.toEntity(lomake)).thenReturn(Kouluttajavaltuutus())

        palvelu(tapaus.aika).save("erikoistuja", lomake, false)

        assertThat(valtuutus.valtuutuksenLuontiaika).isEqualTo(aiempiAika)
        assertThat(valtuutus.valtuutuksenMuokkausaika).isEqualTo(tapaus.aika.aikaleima())
        assertThat(valtuutus.paattymispaiva).isEqualTo(lomake.paattymispaiva)
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `automaattinen valtuutus alkaa kellon paivasta ja kestaa kuusi kuukautta`(aika: KellonTestitapaus) {
        valmisteleValtuutus()

        palvelu(aika).lisaaValtuutus("erikoistuja", 2L)

        val lomake = argumentCaptor<KouluttajavaltuutusDTO>()
        verify(valtuutusMapper).toEntity(lomake.capture())
        assertThat(lomake.firstValue.alkamispaiva).isEqualTo(aika.paiva)
        assertThat(lomake.firstValue.paattymispaiva).isEqualTo(aika.paiva.plusMonths(6))
    }

    @ParameterizedTest
    @MethodSource(KELLON_TESTITAPAUKSET)
    fun `valtuutusten haut sisaltavat kellon paivana paattyvat valtuutukset`(aika: KellonTestitapaus) {
        valmisteleValtuutus()

        palvelu(aika).findAllValtuutettuByValtuuttajaKayttajaUserId("erikoistuja")
        palvelu(aika).findValtuutettuByValtuuttajaAndValtuutettu("erikoistuja", "kouluttaja")

        verify(valtuutukset).findAllByValtuuttajaOpintooikeusIdAndPaattymispaivaAfter(11L, aika.paiva.minusDays(1))
        verify(valtuutukset).findByValtuuttajaOpintooikeusIdAndValtuutettuUserIdAndPaattymispaivaAfter(11L, "kouluttaja", aika.paiva.minusDays(1))
    }

    private fun valmisteleValtuutus(): Kouluttajavaltuutus {
        val erikoistuja = ErikoistuvaLaakari(id = 5L, kayttaja = Kayttaja(user = User(id = "erikoistuja")), opintooikeudet = mutableSetOf(opintooikeus))
        val valtuutus = Kouluttajavaltuutus(valtuutettu = kouluttaja)
        whenever(erikoistujat.findOneByKayttajaUserId("erikoistuja")).thenReturn(erikoistuja)
        whenever(kayttajat.findById(2L)).thenReturn(Optional.of(kouluttaja))
        whenever(valtuutusMapper.toEntity(any<KouluttajavaltuutusDTO>())).thenAnswer {
            val lomake = it.arguments[0] as KouluttajavaltuutusDTO
            valtuutus.apply {
                alkamispaiva = lomake.alkamispaiva
                paattymispaiva = lomake.paattymispaiva
            }
        }
        whenever(valtuutusMapper.toDto(any<Kouluttajavaltuutus>())).thenReturn(KouluttajavaltuutusDTO())
        whenever(valtuutukset.save(any<Kouluttajavaltuutus>())).thenAnswer { it.arguments[0] }
        return valtuutus
    }

    private fun palvelu(aika: KellonTestitapaus) = KouluttajavaltuutusServiceImpl(valtuutukset, valtuutusMapper, erikoistujat, kayttajat, mock(), aika.kello())

    data class Muokkaustapaus(val aika: KellonTestitapaus, val tunnisteella: Boolean)

    companion object {
        @JvmStatic
        fun muokkaustapaukset(): List<Muokkaustapaus> = KellonTestitapaus.tapaukset().flatMap { aika ->
            listOf(false, true).map { Muokkaustapaus(aika, it) }
        }
    }
}
