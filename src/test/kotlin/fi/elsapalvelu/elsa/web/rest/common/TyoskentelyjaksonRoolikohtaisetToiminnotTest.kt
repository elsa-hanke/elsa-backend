package fi.elsapalvelu.elsa.web.rest.common

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.domain.kayttaja.OpintooikeudenTila
import fi.elsapalvelu.elsa.service.dto.perustiedot.AsetusDTO
import fi.elsapalvelu.elsa.domain.koulutus.KaytannonKoulutusTyyppi
import fi.elsapalvelu.elsa.security.ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA
import fi.elsapalvelu.elsa.security.testSamlPrincipal
import fi.elsapalvelu.elsa.service.dto.kayttaja.OpintooikeusDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.UserDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.KeskeytysaikaDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelyjaksoDTO
import fi.elsapalvelu.elsa.service.dto.tyoskentely.TyoskentelypaikkaDTO
import fi.elsapalvelu.elsa.service.kayttaja.*
import fi.elsapalvelu.elsa.service.tyoskentely.*
import fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari.ErikoistuvaLaakariTyoskentelyjaksoResource
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import fi.elsapalvelu.elsa.web.rest.yekkoulutettava.YekKoulutettavaTyoskentelyjaksoResource
import jakarta.validation.ValidationException
import java.security.Principal
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.kotlin.*
import org.springframework.http.ResponseEntity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.multipart.MultipartFile

class TyoskentelyjaksonRoolikohtaisetToiminnotTest {
    enum class Koulutus(val opintooikeusId: Long, val luontipolku: String, val kuvaus: String) {
        ERIKOISTUMINEN(11, "/api/tyoskentelyjaksot", "erikoistujan"),
        YEK(22, "/api/yektyoskentelyjaksot", "yek koulutettavan")
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `luonti sailyttaa koulutuksen opintooikeuden koulutustyypin ja sijainnin`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        whenever(tiedot.jaksot.create(any(), any(), any())).thenAnswer {
            it.getArgument<TyoskentelyjaksoDTO>(0).apply { id = 7 }
        }
        val vastaus = tiedot.toiminnot.luoTyoskentelyjakso(tiedot.json(tiedot.jakso()), null, tiedot.principal)

        assertThat(vastaus.statusCode.value()).isEqualTo(201)
        assertThat(vastaus.headers.location.toString()).isEqualTo("${koulutus.luontipolku}/7")
        val jakso = argumentCaptor<TyoskentelyjaksoDTO>()
        verify(tiedot.jaksot).create(jakso.capture(), eq(koulutus.opintooikeusId), eq(mutableSetOf()))
        assertThat(jakso.firstValue.kaytannonKoulutus).isEqualTo(
            if (koulutus == Koulutus.YEK) KaytannonKoulutusTyyppi.OMAN_ERIKOISALAN_KOULUTUS else KaytannonKoulutusTyyppi.TUTKIMUSTYO
        )
        tiedot.tarkistaOpintooikeudenValinta()
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `paivitys sailyttaa annetun koulutustyypin ja poistettavat liitteet`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        whenever(tiedot.jaksot.update(any(), any(), any(), anyOrNull())).thenAnswer { it.getArgument<TyoskentelyjaksoDTO>(0) }
        val vastaus = tiedot.toiminnot.paivitaTyoskentelyjakso(tiedot.json(tiedot.jakso(7)), null, "[3,4]", tiedot.principal)

        assertThat(vastaus.statusCode.value()).isEqualTo(200)
        val jakso = argumentCaptor<TyoskentelyjaksoDTO>()
        verify(tiedot.jaksot).update(jakso.capture(), eq(koulutus.opintooikeusId), eq(mutableSetOf()), eq(mutableSetOf(3, 4)))
        assertThat(jakso.firstValue.kaytannonKoulutus).isEqualTo(KaytannonKoulutusTyyppi.TUTKIMUSTYO)
        tiedot.tarkistaOpintooikeudenValinta()
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `jakson paallekkaisyys estaa tallennuksen`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        whenever(tiedot.jaksojenTarkistus.validateTyoskentelyjakso(any(), any())).thenReturn(false)
        val virhe = assertThrows<BadRequestAlertException> {
            tiedot.toiminnot.luoTyoskentelyjakso(tiedot.json(tiedot.jakso()), null, tiedot.principal)
        }
        assertThat(virhe.errorKey).isEqualTo("dataillegal.paallekkaisten-tyoskentelyjaksojen-yhteenlaskettu-aika-ylittyy")
        verify(tiedot.jaksot, never()).create(any(), any(), any())
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `poissaolon luonti tarkistaa poissaolot mutta ei tyoajan kasvua`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        val poissaolo = tiedot.poissaolo()
        whenever(tiedot.poissaolot.save(any(), any())).thenReturn(poissaolo.copy(id = 8))

        val vastaus = tiedot.toiminnot.luoKeskeytysaika(poissaolo, tiedot.principal)

        assertThat(vastaus.statusCode.value()).isEqualTo(201)
        assertThat(vastaus.headers.location.toString()).isEqualTo("/api/tyoskentelyjaksot/poissaolot/8")
        verify(tiedot.poissaolojenTarkistus).validateKeskeytysaika(koulutus.opintooikeusId, poissaolo)
        verify(tiedot.jaksojenTarkistus, never()).validateKeskeytysaika(any(), any())
        verify(tiedot.poissaolot).save(poissaolo, koulutus.opintooikeusId)
        tiedot.tarkistaOpintooikeudenValinta()
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `poissaolon paivitys tarkistaa poissaolot ja tyoajan ennen tallennusta`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        val poissaolo = tiedot.poissaolo(8)
        whenever(tiedot.poissaolot.save(any(), any())).thenReturn(poissaolo)

        assertThat(tiedot.toiminnot.paivitaKeskeytysaika(poissaolo, tiedot.principal).statusCode.value()).isEqualTo(200)

        inOrder(tiedot.poissaolojenTarkistus, tiedot.jaksojenTarkistus, tiedot.poissaolot) {
            verify(tiedot.poissaolojenTarkistus).validateKeskeytysaika(koulutus.opintooikeusId, poissaolo)
            verify(tiedot.jaksojenTarkistus).validateKeskeytysaika(koulutus.opintooikeusId, poissaolo)
            verify(tiedot.poissaolot).save(poissaolo, koulutus.opintooikeusId)
        }
        tiedot.tarkistaOpintooikeudenValinta()
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `poissaolojen paallekkaisyys estaa myos tyoajan tarkistuksen`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        whenever(tiedot.poissaolojenTarkistus.validateKeskeytysaika(any(), any())).thenReturn(false)
        val virhe = assertThrows<BadRequestAlertException> { tiedot.toiminnot.paivitaKeskeytysaika(tiedot.poissaolo(8), tiedot.principal) }

        assertThat(virhe.entityName).isEqualTo("keskeytysaika")
        assertThat(virhe.errorKey).isEqualTo("dataillegal.paallekkaisten-poissaolojen-yhteenlaskettu-aika-ylittyy")
        verify(tiedot.jaksojenTarkistus, never()).validateKeskeytysaika(any(), any())
        verifyNoInteractions(tiedot.poissaolot)
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `tyoajan ylitys estaa poissaolon paivityksen ja poiston`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        whenever(tiedot.jaksojenTarkistus.validateKeskeytysaika(any(), any())).thenReturn(false)
        whenever(tiedot.jaksojenTarkistus.validateKeskeytysaikaDelete(any(), any())).thenReturn(false)

        assertThrows<BadRequestAlertException> { tiedot.toiminnot.paivitaKeskeytysaika(tiedot.poissaolo(8), tiedot.principal) }
        assertThrows<BadRequestAlertException> { tiedot.toiminnot.poistaKeskeytysaika(8L, tiedot.principal) }
        verifyNoInteractions(tiedot.poissaolot)
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `terveyskeskusjakson lukitus sailyttaa virheavaimen`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        whenever(tiedot.poissaolot.save(any(), any())).thenThrow(ValidationException("Lukittu"))
        val virhe = assertThrows<BadRequestAlertException> { tiedot.toiminnot.luoKeskeytysaika(tiedot.poissaolo(), tiedot.principal) }

        assertThat(virhe.defaultMessage).isEqualTo("Lukittu")
        assertThat(virhe.entityName).isEqualTo("tyoskentelyjakso")
        assertThat(virhe.errorKey).isEqualTo("dataillegal.terveyskeskuskoulutusjaksoon-liitettya-tyoskentelyjaksoa-ei-voi-paivittaa")
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `virkailijan muokkauskielto sailyttaa roolikohtaisen viestin`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        val virkailija = Saml2Authentication(testSamlPrincipal("opiskelija", emptyMap()), "test",
            listOf(SimpleGrantedAuthority(ERIKOISTUVA_LAAKARI_IMPERSONATED_VIRKAILIJA)))
        val virhe = assertThrows<BadRequestAlertException> {
            tiedot.toiminnot.luoTyoskentelyjakso("virheellinen JSON", null, virkailija)
        }
        assertThat(virhe.defaultMessage).isEqualTo("Ei oikeuksia muokata ${koulutus.kuvaus} tietoja")
        verifyNoInteractions(tiedot.jaksot, tiedot.poissaolot)
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `poissaolon tunnistevirhe sailyy ennen kayttajan hakua`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        val virhe = assertThrows<BadRequestAlertException> { tiedot.toiminnot.paivitaKeskeytysaika(tiedot.poissaolo(), null) }
        assertThat(virhe.entityName).isEqualTo("tyoskentelyjakso")
        assertThat(virhe.errorKey).isEqualTo("idnull")
        verify(tiedot.kayttajat, never()).getAuthenticatedUser(anyOrNull())
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `poistot kayttavat koulutuksen opintooikeutta`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        whenever(tiedot.jaksot.delete(any(), any())).thenReturn(true)
        whenever(tiedot.jaksojenTarkistus.validateKeskeytysaikaDelete(any(), any())).thenReturn(true)
        assertThat(tiedot.toiminnot.poistaTyoskentelyjakso(7L, tiedot.principal).statusCode.value()).isEqualTo(204)
        assertThat(tiedot.toiminnot.poistaKeskeytysaika(8L, tiedot.principal).statusCode.value()).isEqualTo(204)
        verify(tiedot.jaksot).delete(7, koulutus.opintooikeusId)
        verify(tiedot.poissaolot).delete(8, koulutus.opintooikeusId)
        tiedot.tarkistaOpintooikeudenValinta(2)
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `puuttuvan jakson ja liitteiden virhestatus sailyy`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        val haku = assertThrows<ResponseStatusException> { tiedot.toiminnot.haeTyoskentelyjakso(7L, tiedot.principal) }
        val liitteet = assertThrows<ResponseStatusException> { tiedot.toiminnot.paivitaAsiakirjat(7L, null, null, tiedot.principal) }
        val poisto = assertThrows<ResponseStatusException> { tiedot.toiminnot.poistaTyoskentelyjakso(7L, tiedot.principal) }
        assertThat(haku.statusCode.value()).isEqualTo(404)
        assertThat(liitteet.statusCode.value()).isEqualTo(403)
        assertThat(poisto.statusCode.value()).isEqualTo(400)
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `koejaksoon liittaminen palauttaa jakson ja irrottaminen tyhjan vastauksen`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        whenever(tiedot.jaksot.updateLiitettyKoejaksoon(any(), any(), any())).thenAnswer {
            TyoskentelyjaksoDTO(id = 7, liitettyKoejaksoon = it.getArgument(2))
        }

        val liitetty = tiedot.toiminnot.paivitaKoejaksokytkenta(TyoskentelyjaksoDTO(id = 7, liitettyKoejaksoon = true), tiedot.principal)
        val irrotettu = tiedot.toiminnot.paivitaKoejaksokytkenta(TyoskentelyjaksoDTO(id = 7, liitettyKoejaksoon = false), tiedot.principal)

        assertThat(liitetty.statusCode.value()).isEqualTo(200)
        assertThat(liitetty.body?.liitettyKoejaksoon).isTrue()
        assertThat(irrotettu.statusCode.value()).isEqualTo(200)
        assertThat(irrotettu.body).isNull()
        verify(tiedot.jaksot).updateLiitettyKoejaksoon(7, koulutus.opintooikeusId, true)
        verify(tiedot.jaksot).updateLiitettyKoejaksoon(7, koulutus.opintooikeusId, false)
        tiedot.tarkistaOpintooikeudenValinta(2)
    }

    private data class YhteisetToiminnot(
        val luoTyoskentelyjakso: (String, List<MultipartFile>?, Principal?) -> ResponseEntity<TyoskentelyjaksoDTO>,
        val paivitaTyoskentelyjakso: (String, List<MultipartFile>?, String?, Principal?) -> ResponseEntity<TyoskentelyjaksoDTO>,
        val luoKeskeytysaika: (KeskeytysaikaDTO, Principal?) -> ResponseEntity<KeskeytysaikaDTO>,
        val paivitaKeskeytysaika: (KeskeytysaikaDTO, Principal?) -> ResponseEntity<KeskeytysaikaDTO>,
        val poistaKeskeytysaika: (Long, Principal?) -> ResponseEntity<Unit>,
        val poistaTyoskentelyjakso: (Long, Principal?) -> ResponseEntity<Unit>,
        val haeTyoskentelyjakso: (Long, Principal?) -> ResponseEntity<TyoskentelyjaksoDTO>,
        val paivitaAsiakirjat: (Long, List<MultipartFile>?, List<Int>?, Principal?) -> ResponseEntity<TyoskentelyjaksoDTO>,
        val paivitaKoejaksokytkenta: (TyoskentelyjaksoDTO, Principal?) -> ResponseEntity<TyoskentelyjaksoDTO>
    )

    private class Testitiedot(private val koulutus: Koulutus) {
        val jaksot = mock<TyoskentelyjaksoService>()
        val poissaolot = mock<KeskeytysaikaService>()
        val kayttajat = mock<UserService>()
        val opintooikeudet = mock<OpintooikeusService>()
        val jaksojenTarkistus = mock<OverlappingTyoskentelyjaksoValidationService>()
        val poissaolojenTarkistus = mock<OverlappingKeskeytysaikaValidationService>()
        val principal: Principal = Saml2Authentication(testSamlPrincipal("opiskelija", emptyMap()), "test", emptyList())
        private val mapper = jacksonObjectMapper().findAndRegisterModules()
        val toiminnot: YhteisetToiminnot

        init {
            whenever(kayttajat.getAuthenticatedUser(anyOrNull())).thenReturn(UserDTO(id = "opiskelija"))
            whenever(opintooikeudet.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId("opiskelija")).thenReturn(11)
            whenever(opintooikeudet.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId("opiskelija", YEK_ERIKOISALA_ID)).thenReturn(22)
            whenever(opintooikeudet.findOneByKaytossaAndErikoistuvaLaakariKayttajaUserId("opiskelija"))
                .thenReturn(OpintooikeusDTO(asetus = AsetusDTO(), tila = OpintooikeudenTila.AKTIIVINEN))
            whenever(jaksot.validateAlkamisJaPaattymispaiva(any(), any())).thenReturn(true)
            whenever(jaksojenTarkistus.validateTyoskentelyjakso(any(), any())).thenReturn(true)
            whenever(jaksojenTarkistus.validateKeskeytysaika(any(), any())).thenReturn(true)
            whenever(poissaolojenTarkistus.validateKeskeytysaika(any(), any())).thenReturn(true)
            val tarkistus = TyoskentelyjaksonTarkistuspalvelu(mock(), jaksojenTarkistus, jaksot, opintooikeudet)
            val jaksojenMuokkaus = TyoskentelyjaksonMuokkauspalvelu(jaksot, tarkistus, mapper)
            val poissaolojenMuokkaus = KeskeytysajanMuokkauspalvelu(poissaolot, poissaolojenTarkistus, jaksojenTarkistus, tarkistus)
            toiminnot = if (koulutus == Koulutus.YEK) {
                val resurssi = YekKoulutettavaTyoskentelyjaksoResource(
                    kayttajat, jaksot, mock(), mock(), mock(), poissaolot, mock(), mapper,
                    jaksojenMuokkaus, poissaolojenMuokkaus, opintooikeudet, mock(), tarkistus, mock(), mock()
                )
                YhteisetToiminnot(
                    resurssi::createTyoskentelyjakso, resurssi::updateTyoskentelyjakso, resurssi::createKeskeytysaika,
                    resurssi::updateKeskeytysaika, resurssi::deleteKeskeytysaika, resurssi::deleteTyoskentelyjakso,
                    resurssi::getTyoskentelyjakso, resurssi::updateTyoskentelyjaksoAsiakirjat, resurssi::updateLiitettyKoejaksoon
                )
            } else {
                val resurssi = ErikoistuvaLaakariTyoskentelyjaksoResource(
                    kayttajat, jaksot, mock(), mock(), mock(), poissaolot, mock(), mapper,
                    jaksojenMuokkaus, poissaolojenMuokkaus, opintooikeudet, mock(), tarkistus, mock(), mock()
                )
                YhteisetToiminnot(
                    resurssi::createTyoskentelyjakso, resurssi::updateTyoskentelyjakso, resurssi::createKeskeytysaika,
                    resurssi::updateKeskeytysaika, resurssi::deleteKeskeytysaika, resurssi::deleteTyoskentelyjakso,
                    resurssi::getTyoskentelyjakso, resurssi::updateTyoskentelyjaksoAsiakirjat, resurssi::updateLiitettyKoejaksoon
                )
            }
        }

        fun json(arvo: Any): String = mapper.writeValueAsString(arvo)
        fun jakso(id: Long? = null) = TyoskentelyjaksoDTO(id = id, alkamispaiva = LocalDate.of(2020, 1, 1),
            paattymispaiva = LocalDate.of(2020, 12, 31), osaaikaprosentti = 100,
            kaytannonKoulutus = KaytannonKoulutusTyyppi.TUTKIMUSTYO, tyoskentelypaikka = TyoskentelypaikkaDTO())
        fun poissaolo(id: Long? = null) = KeskeytysaikaDTO(id = id, alkamispaiva = LocalDate.of(2020, 2, 1),
            paattymispaiva = LocalDate.of(2020, 2, 2), tyoskentelyjaksoId = 7, tyoskentelyjakso = jakso(7), poissaoloprosentti = 100)

        fun tarkistaOpintooikeudenValinta(kertoja: Int = 1) {
            if (koulutus == Koulutus.YEK) {
                verify(opintooikeudet, times(kertoja)).findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId("opiskelija", YEK_ERIKOISALA_ID)
                verify(opintooikeudet, never()).findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(any())
            } else {
                verify(opintooikeudet, times(kertoja)).findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId("opiskelija")
                verify(opintooikeudet, never()).findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId(any(), any())
            }
        }
    }
}
