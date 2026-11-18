package fi.elsapalvelu.elsa.web.rest.common

import fi.elsapalvelu.elsa.config.YEK_ERIKOISALA_ID
import fi.elsapalvelu.elsa.service.dto.kayttaja.ErikoistuvaLaakariDTO
import fi.elsapalvelu.elsa.service.dto.kayttaja.UserDTO
import fi.elsapalvelu.elsa.service.dto.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksyntaDTO
import fi.elsapalvelu.elsa.service.kayttaja.ErikoistuvaLaakariService
import fi.elsapalvelu.elsa.service.kayttaja.OpintooikeusService
import fi.elsapalvelu.elsa.service.kayttaja.UserService
import fi.elsapalvelu.elsa.service.koulutus.OpintosuoritusService
import fi.elsapalvelu.elsa.service.tyoskentely.TyoskentelyjaksonTarkistuspalvelu
import fi.elsapalvelu.elsa.service.valmistuminen.TerveyskeskuskoulutusjaksonHyvaksyntaService
import fi.elsapalvelu.elsa.service.valmistuminen.TerveyskeskuskoulutusjaksonLahetyspalvelu
import fi.elsapalvelu.elsa.web.rest.TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME
import fi.elsapalvelu.elsa.web.rest.erikoistuvalaakari.ErikoistuvaLaakariTyoskentelyjaksoResource
import fi.elsapalvelu.elsa.web.rest.errors.BadRequestAlertException
import fi.elsapalvelu.elsa.web.rest.yekkoulutettava.YekKoulutettavaTyoskentelyjaksoResource
import java.security.Principal
import java.time.LocalDate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.*
import org.springframework.http.ResponseEntity
import org.springframework.mock.web.MockMultipartFile
import org.springframework.web.multipart.MultipartFile

class TerveyskeskuskoulutusjaksonLahetysTest {
    enum class Koulutus(val opintooikeusId: Long) { ERIKOISTUMINEN(11), YEK(22) }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `luonti sailyttaa opintooikeuden tarkistusten jarjestyksen ja liitteen tiedot`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)

        val vastaus = tiedot.laheta(paiva, liite, tiedot.principal)

        assertThat(vastaus.statusCode.value()).isEqualTo(200)
        assertThat(vastaus.body).isSameAs(tiedot.tulos)
        inOrder(tiedot.erikoistujat, tiedot.opintosuoritukset, tiedot.hyvaksyntapalvelu) {
            if (koulutus == Koulutus.ERIKOISTUMINEN) {
                verify(tiedot.erikoistujat).findOneByKayttajaUserId("opiskelija")
                verify(tiedot.opintosuoritukset).getTerveyskoulutusjaksoSuoritettu(11, 33)
                verify(tiedot.hyvaksyntapalvelu).getTerveyskoulutusjaksoSuoritettu(11)
            } else {
                verify(tiedot.hyvaksyntapalvelu).getTerveyskoulutusjaksoSuoritettuYek(22)
            }
            verify(tiedot.hyvaksyntapalvelu).existsByOpintooikeusId(koulutus.opintooikeusId)
            verify(tiedot.erikoistujat).updateLaillistamispaiva("opiskelija", paiva, liite.bytes, liite.originalFilename, liite.contentType)
            verify(tiedot.hyvaksyntapalvelu).create(koulutus.opintooikeusId)
        }
        verify(tiedot.erikoistujat, never()).laillistamispaivaAndTodistusExists(any())
        tiedot.tarkistaKoulutuskohtainenHaku()
    }

    @Test
    fun `erikoistujan rekisteroity suoritus estaa muut tarkistukset ja tallennukset`() {
        val tiedot = Testitiedot(Koulutus.ERIKOISTUMINEN)
        whenever(tiedot.opintosuoritukset.getTerveyskoulutusjaksoSuoritettu(11, 33)).thenReturn(true)

        val virhe = assertThrows<BadRequestAlertException> { tiedot.laheta(null, null, tiedot.principal) }

        tarkistaVirhe(virhe, "dataillegal.terveyskeskuskoulutusjaksolla-on-opintosuoritus")
        verifyNoInteractions(tiedot.hyvaksyntapalvelu)
        tiedot.tarkistaEiTallennuksia()
    }

    @Test
    fun `yek ei tarkista erikoistujan rekisteroitya suoritusta`() {
        val tiedot = Testitiedot(Koulutus.YEK)
        whenever(tiedot.opintosuoritukset.getTerveyskoulutusjaksoSuoritettu(any(), any())).thenReturn(true)

        assertThat(tiedot.laheta(paiva, liite, tiedot.principal).statusCode.value()).isEqualTo(200)

        verifyNoInteractions(tiedot.opintosuoritukset)
        verify(tiedot.erikoistujat, never()).findOneByKayttajaUserId(any())
        tiedot.tarkistaKoulutuskohtainenHaku()
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `riittamaton kesto hylataan ennen laillistamista ja olemassaolon tarkistusta`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        whenever(tiedot.hyvaksyntapalvelu.getTerveyskoulutusjaksoSuoritettu(any())).thenReturn(false)
        whenever(tiedot.hyvaksyntapalvelu.getTerveyskoulutusjaksoSuoritettuYek(any())).thenReturn(false)

        val virhe = assertThrows<BadRequestAlertException> { tiedot.laheta(null, null, tiedot.principal) }

        tarkistaVirhe(virhe, "dataillegal.terveyskeskuskoulutusjakson-pituus-liian-pieni")
        verify(tiedot.erikoistujat, never()).laillistamispaivaAndTodistusExists(any())
        verify(tiedot.hyvaksyntapalvelu, never()).existsByOpintooikeusId(any())
        tiedot.tarkistaEiTallennuksia()
    }

    @ParameterizedTest
    @MethodSource("puutteellisetLaillistamistiedot")
    fun `puutteellinen laillistaminen hylataan ennen olemassaolon tarkistusta`(
        koulutus: Koulutus, annettuPaiva: LocalDate?, annettuLiite: MultipartFile?
    ) {
        val tiedot = Testitiedot(koulutus)
        whenever(tiedot.hyvaksyntapalvelu.existsByOpintooikeusId(any())).thenReturn(true)

        val virhe = assertThrows<BadRequestAlertException> { tiedot.laheta(annettuPaiva, annettuLiite, tiedot.principal) }

        tarkistaVirhe(virhe, "dataillegal.laillistamispaiva-ja-todistus-vaaditaan")
        verify(tiedot.hyvaksyntapalvelu, never()).existsByOpintooikeusId(any())
        tiedot.tarkistaEiTallennuksia()
    }

    @ParameterizedTest
    @MethodSource("puutteellisetLaillistamistiedot")
    fun `tallennettu laillistaminen sallii puuttuvat tiedot ja sailyttaa annetut arvot`(
        koulutus: Koulutus, annettuPaiva: LocalDate?, annettuLiite: MultipartFile?
    ) {
        val tiedot = Testitiedot(koulutus)
        whenever(tiedot.erikoistujat.laillistamispaivaAndTodistusExists("opiskelija")).thenReturn(true)

        assertThat(tiedot.laheta(annettuPaiva, annettuLiite, tiedot.principal).statusCode.value()).isEqualTo(200)

        verify(tiedot.erikoistujat).laillistamispaivaAndTodistusExists("opiskelija")
        verify(tiedot.erikoistujat).updateLaillistamispaiva(
            "opiskelija", annettuPaiva, annettuLiite?.bytes, annettuLiite?.originalFilename, annettuLiite?.contentType
        )
        verify(tiedot.hyvaksyntapalvelu).create(koulutus.opintooikeusId)
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `olemassa olevaa hakemusta ei luoda uudelleen eika laillistamista paiviteta`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        whenever(tiedot.hyvaksyntapalvelu.existsByOpintooikeusId(any())).thenReturn(true)

        val virhe = assertThrows<BadRequestAlertException> { tiedot.laheta(paiva, liite, tiedot.principal) }

        tarkistaVirhe(virhe, "dataillegal.terveyskeskuskoulutusjakson-hyvaksynta-on-jo-lahetetty")
        tiedot.tarkistaEiTallennuksia()
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `uudelleenlahetys paivittaa laillistamisen ennen hyvaksyntaa toistamatta luontitarkistuksia`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)

        val vastaus = tiedot.lahetaUudelleen(null, null, tiedot.principal)

        assertThat(vastaus.statusCode.value()).isEqualTo(200)
        assertThat(vastaus.body).isSameAs(tiedot.tulos)
        inOrder(tiedot.hyvaksyntapalvelu, tiedot.erikoistujat) {
            verify(tiedot.hyvaksyntapalvelu).findByOpintooikeusId(koulutus.opintooikeusId)
            verify(tiedot.erikoistujat).updateLaillistamispaiva("opiskelija", null, null, null, null)
            verify(tiedot.hyvaksyntapalvelu).update("opiskelija", false, 55, null, null, null, null)
        }
        verify(tiedot.erikoistujat, never()).laillistamispaivaAndTodistusExists(any())
        verify(tiedot.hyvaksyntapalvelu, never()).getTerveyskoulutusjaksoSuoritettu(any())
        verify(tiedot.hyvaksyntapalvelu, never()).getTerveyskoulutusjaksoSuoritettuYek(any())
        verify(tiedot.hyvaksyntapalvelu, never()).existsByOpintooikeusId(any())
        verifyNoInteractions(tiedot.opintosuoritukset)
        tiedot.tarkistaKoulutuskohtainenHaku()
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `uudelleenlahetys valittaa uudet laillistamistiedot vain erikoistujapalvelulle`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)

        tiedot.lahetaUudelleen(paiva, liite, tiedot.principal)

        verify(tiedot.erikoistujat).updateLaillistamispaiva("opiskelija", paiva, liite.bytes, liite.originalFilename, liite.contentType)
        verify(tiedot.hyvaksyntapalvelu).update("opiskelija", false, 55, null, null, null, null)
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `puuttuvaa hakemusta ei uudelleenlaheteta eika laillistamista muuteta`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        whenever(tiedot.hyvaksyntapalvelu.findByOpintooikeusId(any())).thenReturn(null)

        val virhe = assertThrows<BadRequestAlertException> { tiedot.lahetaUudelleen(paiva, liite, tiedot.principal) }

        tarkistaVirhe(virhe, "dataillegal.terveyskeskuskoulutusjakson-hyvaksyntaa-ei-loydy")
        tiedot.tarkistaEiTallennuksia()
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `laillistamisen paivitysvirhe estaa seka luonnin etta uudelleenlahetyksen`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        val virhe = IllegalStateException("Laillistamisen päivitys epäonnistui")
        whenever(tiedot.erikoistujat.updateLaillistamispaiva(any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())).thenThrow(virhe)

        assertThat(assertThrows<IllegalStateException> { tiedot.laheta(paiva, liite, tiedot.principal) }).isSameAs(virhe)
        assertThat(assertThrows<IllegalStateException> { tiedot.lahetaUudelleen(paiva, liite, tiedot.principal) }).isSameAs(virhe)

        verify(tiedot.hyvaksyntapalvelu, never()).create(any())
        verify(tiedot.hyvaksyntapalvelu, never()).update(any(), any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `hyvaksynnan tallennusvirhe valittyy laillistamisen paivityksen jalkeen`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        val virhe = IllegalStateException("Hyväksynnän tallennus epäonnistui")
        whenever(tiedot.hyvaksyntapalvelu.create(any())).thenThrow(virhe)
        whenever(tiedot.hyvaksyntapalvelu.update("opiskelija", false, 55, null, null)).thenThrow(virhe)

        assertThat(assertThrows<IllegalStateException> { tiedot.laheta(paiva, liite, tiedot.principal) }).isSameAs(virhe)
        assertThat(assertThrows<IllegalStateException> { tiedot.lahetaUudelleen(null, null, tiedot.principal) }).isSameAs(virhe)

        inOrder(tiedot.erikoistujat, tiedot.hyvaksyntapalvelu) {
            verify(tiedot.erikoistujat).updateLaillistamispaiva("opiskelija", paiva, liite.bytes, liite.originalFilename, liite.contentType)
            verify(tiedot.hyvaksyntapalvelu).create(koulutus.opintooikeusId)
            verify(tiedot.erikoistujat).updateLaillistamispaiva("opiskelija", null, null, null, null)
            verify(tiedot.hyvaksyntapalvelu).update("opiskelija", false, 55, null, null, null, null)
        }
    }

    @ParameterizedTest
    @EnumSource(Koulutus::class)
    fun `palvelun tyhja tulos sailyttaa onnistuneen HTTP vastauksen`(koulutus: Koulutus) {
        val tiedot = Testitiedot(koulutus)
        whenever(tiedot.hyvaksyntapalvelu.create(any())).thenReturn(null)
        whenever(tiedot.hyvaksyntapalvelu.update("opiskelija", false, 55, null, null)).thenReturn(null)

        val luotu = tiedot.laheta(paiva, liite, tiedot.principal)
        assertThat(luotu.statusCode.value()).isEqualTo(200)
        assertThat(luotu.body).isNull()
        val vastaus = tiedot.lahetaUudelleen(null, null, tiedot.principal)
        assertThat(vastaus.statusCode.value()).isEqualTo(200)
        assertThat(vastaus.body).isNull()
    }

    private fun tarkistaVirhe(virhe: BadRequestAlertException, avain: String) {
        assertThat(virhe.entityName).isEqualTo(TERVEYSKESKUSKOULUTUSJAKSO_ENTITY_NAME)
        assertThat(virhe.errorKey).isEqualTo(avain)
    }

    private class Testitiedot(private val koulutus: Koulutus) {
        val kayttajat = mock<UserService>()
        val opintooikeudet = mock<OpintooikeusService>()
        val erikoistujat = mock<ErikoistuvaLaakariService>()
        val opintosuoritukset = mock<OpintosuoritusService>()
        val hyvaksyntapalvelu = mock<TerveyskeskuskoulutusjaksonHyvaksyntaService>()
        val tulos = TerveyskeskuskoulutusjaksonHyvaksyntaDTO(id = 55)
        val principal = Principal { "opiskelija" }
        val laheta: (LocalDate?, MultipartFile?, Principal?) -> ResponseEntity<TerveyskeskuskoulutusjaksonHyvaksyntaDTO>
        val lahetaUudelleen: (LocalDate?, MultipartFile?, Principal?) -> ResponseEntity<TerveyskeskuskoulutusjaksonHyvaksyntaDTO>

        init {
            whenever(kayttajat.getAuthenticatedUser(anyOrNull())).thenReturn(UserDTO(id = "opiskelija"))
            whenever(opintooikeudet.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId("opiskelija")).thenReturn(11)
            whenever(opintooikeudet.findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId("opiskelija", YEK_ERIKOISALA_ID)).thenReturn(22)
            whenever(erikoistujat.findOneByKayttajaUserId("opiskelija")).thenReturn(ErikoistuvaLaakariDTO(id = 33))
            whenever(hyvaksyntapalvelu.getTerveyskoulutusjaksoSuoritettu(any())).thenReturn(true)
            whenever(hyvaksyntapalvelu.getTerveyskoulutusjaksoSuoritettuYek(any())).thenReturn(true)
            whenever(hyvaksyntapalvelu.create(any())).thenReturn(tulos)
            whenever(hyvaksyntapalvelu.findByOpintooikeusId(any())).thenReturn(TerveyskeskuskoulutusjaksonHyvaksyntaDTO(id = 55))
            whenever(hyvaksyntapalvelu.update("opiskelija", false, 55, null, null)).thenReturn(tulos)
            val tarkistukset = TyoskentelyjaksonTarkistuspalvelu(mock(), mock(), mock(), opintooikeudet)
            val lahetyspalvelu = TerveyskeskuskoulutusjaksonLahetyspalvelu(erikoistujat, opintosuoritukset, hyvaksyntapalvelu)
            if (koulutus == Koulutus.YEK) {
                val resurssi = YekKoulutettavaTyoskentelyjaksoResource(
                    kayttajat, mock(), mock(),
                    mock(), mock(), mock(),
                    opintooikeudet, hyvaksyntapalvelu, tarkistukset,
                    lahetyspalvelu, mock(), mock()
                )
                laheta = resurssi::createTerveyskeskuskoulutusjaksonHyvaksynta
                lahetaUudelleen = resurssi::updateTerveyskeskuskoulutusjaksonHyvaksynta
            } else {
                val resurssi = ErikoistuvaLaakariTyoskentelyjaksoResource(
                    kayttajat, mock(), mock(),
                    mock(), mock(), mock(),
                    opintooikeudet, hyvaksyntapalvelu, tarkistukset,
                    lahetyspalvelu, mock(), mock()
                )
                laheta = resurssi::createTerveyskeskuskoulutusjaksonHyvaksynta
                lahetaUudelleen = resurssi::updateTerveyskeskuskoulutusjaksonHyvaksynta
            }
        }

        fun tarkistaEiTallennuksia() {
            verify(erikoistujat, never()).updateLaillistamispaiva(any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())
            verify(hyvaksyntapalvelu, never()).create(any())
            verify(hyvaksyntapalvelu, never()).update(any(), any(), any(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull())
        }

        fun tarkistaKoulutuskohtainenHaku() {
            if (koulutus == Koulutus.YEK) {
                verify(opintooikeudet).findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId("opiskelija", YEK_ERIKOISALA_ID)
                verify(opintooikeudet, never()).findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId(any())
                verify(hyvaksyntapalvelu, never()).getTerveyskoulutusjaksoSuoritettu(any())
            } else {
                verify(opintooikeudet).findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserId("opiskelija")
                verify(opintooikeudet, never()).findOneIdByKaytossaAndErikoistuvaLaakariKayttajaUserIdAndErikoisalaId(any(), any())
                verify(hyvaksyntapalvelu, never()).getTerveyskoulutusjaksoSuoritettuYek(any())
            }
        }
    }

    companion object {
        private val paiva = LocalDate.of(2020, 2, 29)
        private val liite = MockMultipartFile("laillistamispaivanLiite", "todistus.pdf", "application/pdf", byteArrayOf(1, 2, 3))

        @JvmStatic
        fun puutteellisetLaillistamistiedot(): List<Arguments> = Koulutus.entries.flatMap { koulutus ->
            listOf(Arguments.of(koulutus, null, null), Arguments.of(koulutus, paiva, null), Arguments.of(koulutus, null, liite))
        }
    }
}
