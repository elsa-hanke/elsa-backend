package fi.elsapalvelu.elsa.service.kayttaja.etusivu

import fi.elsapalvelu.elsa.domain.kayttaja.AvoinAsiaTyyppiEnum
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonAloituskeskustelu
import fi.elsapalvelu.elsa.domain.koejakso.KoejaksonKoulutussopimus
import fi.elsapalvelu.elsa.domain.perustiedot.ErikoisalaTyyppi
import fi.elsapalvelu.elsa.domain.seuranta.Seurantajakso
import fi.elsapalvelu.elsa.domain.valmistuminen.Valmistumispyynto
import jakarta.persistence.EntityNotFoundException
import java.time.LocalDate
import java.util.Locale
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class EtusivunAvoimetAsiatTest {
    private val tiedot = EtusivunTestitiedot()
    private val palvelu = tiedot.luoPalvelu()
    private val paiva = LocalDate.of(2020, 1, 20)

    @Test
    fun `avoimet asiat jarjestetaan paivan mukaan ja saman paivan lajijarjestys sailyy`() {
        whenever(tiedot.koejaksonKoulutussopimusRepository.findByOpintooikeusId(11L))
            .thenReturn(Optional.of(KoejaksonKoulutussopimus(korjausehdotus = "korjaa", muokkauspaiva = paiva)))
        whenever(tiedot.koejaksonAloituskeskusteluRepository.findByOpintooikeusId(11L))
            .thenReturn(Optional.of(KoejaksonAloituskeskustelu(korjausehdotus = "korjaa", muokkauspaiva = paiva)))
        whenever(tiedot.seurantajaksoRepository.findByOpintooikeusId(11L))
            .thenReturn(listOf(Seurantajakso(id = 8L, korjausehdotus = "korjaa", tallennettu = paiva.minusDays(1))))
        whenever(tiedot.terveyskeskuskoulutusjaksonHyvaksyntaService.getTerveyskoulutusjaksoSuoritettu(11L)).thenReturn(true)

        val tulos = palvelu.getAvoimetAsiatForErikoistuja("opiskelija")!!

        assertThat(tulos.map { it.tyyppi }).containsExactly(
            AvoinAsiaTyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSO, AvoinAsiaTyyppiEnum.SEURANTAJAKSO,
            AvoinAsiaTyyppiEnum.KOULUTUSSOPIMUS, AvoinAsiaTyyppiEnum.ALOITUSKESKUSTELU
        )
        assertThat(tulos[1].id).isEqualTo(8L)
        verify(tiedot.messageSource).getMessage(eq("avoimetasiat.koulutussopimus"), isNull(), eq(Locale.forLanguageTag("sv")))
    }

    @Test
    fun `yek avoimet asiat eivat sisalla koejakson tai seurantajakson palautuksia`() {
        whenever(tiedot.terveyskeskuskoulutusjaksonHyvaksyntaService.getTerveyskoulutusjaksoSuoritettuYek(11L)).thenReturn(true)
        whenever(tiedot.valmistumispyyntoRepository.findByOpintooikeusId(11L))
            .thenReturn(Valmistumispyynto(id = 9L, virkailijanPalautusaika = paiva))

        val tulos = palvelu.getAvoimetAsiatForYekKoulutettava("opiskelija")!!

        assertThat(tulos.map { it.tyyppi }).containsExactly(AvoinAsiaTyyppiEnum.TERVEYSKESKUSKOULUTUSJAKSO, AvoinAsiaTyyppiEnum.VALMISTUMISPYYNTO)
        verifyNoInteractions(tiedot.koejaksonKoulutussopimusRepository, tiedot.koejaksonAloituskeskusteluRepository,
            tiedot.koejaksonVastuuhenkilonArvioRepository, tiedot.seurantajaksoRepository)
    }

    @Test
    fun `valmistumispyynnon palautuspaiva valitaan arvioijan virkailijan ja hyvaksyjan etusijajarjestyksessa`() {
        whenever(tiedot.valmistumispyyntoRepository.findByOpintooikeusId(11L)).thenReturn(Valmistumispyynto(
            id = 9L, vastuuhenkiloOsaamisenArvioijaPalautusaika = paiva,
            virkailijanPalautusaika = paiva.minusDays(2), vastuuhenkiloHyvaksyjaPalautusaika = paiva.plusDays(2)
        ))

        assertThat(palvelu.getAvoimetAsiatForErikoistuja("opiskelija")!!.single().pvm).isEqualTo(paiva)
    }

    @Test
    fun `hammaslaaketieteessa ei ehdoteta terveyskeskuskoulutusjakson hyvaksyntaa`() {
        tiedot.opintooikeus.erikoisala!!.tyyppi = ErikoisalaTyyppi.HAMMASLAAKETIEDE

        assertThat(palvelu.getAvoimetAsiatForErikoistuja("opiskelija")).isEmpty()
        verifyNoInteractions(tiedot.opintosuoritusService, tiedot.terveyskeskuskoulutusjaksonHyvaksyntaService)
    }

    @Test
    fun `rekisteroity terveyskeskussuoritus estaa uuden hakemuksen ehdottamisen`() {
        whenever(tiedot.opintosuoritusService.getTerveyskoulutusjaksoSuoritettu(11L, 21L)).thenReturn(true)

        assertThat(palvelu.getAvoimetAsiatForErikoistuja("opiskelija")).isEmpty()
        verifyNoInteractions(tiedot.terveyskeskuskoulutusjaksonHyvaksyntaService, tiedot.terveyskeskuskoulutusjaksonHyvaksyntaRepository)
    }

    @Test
    fun `vanhenevan katseluoikeuden haku kattaa taman paivan ja seuraavan kuukauden viimeisen paivan`() {
        palvelu.getAvoimetAsiatForErikoistuja("opiskelija")
        palvelu.getVanhenevatKatseluoikeudetForKouluttaja("kouluttaja")

        verify(tiedot.kouluttajavaltuutusRepository).findAllByValtuuttajaOpintooikeusIdAndPaattymispaivaBeforeAndPaattymispaivaAfter(
            11L, LocalDate.of(2020, 3, 1), LocalDate.of(2020, 1, 30)
        )
        verify(tiedot.kouluttajavaltuutusRepository).findAllByValtuutettuUserIdAndPaattymispaivaBeforeAndPaattymispaivaAfter(
            "kouluttaja", LocalDate.of(2020, 3, 1), LocalDate.of(2020, 1, 30)
        )
    }

    @Test
    fun `puuttuva kieli kayttaa suomea ja tyhjat korjausehdotukset ohitetaan`() {
        whenever(tiedot.userRepository.findById("opiskelija")).thenReturn(Optional.of(User(langKey = null)))
        whenever(tiedot.koejaksonKoulutussopimusRepository.findByOpintooikeusId(11L))
            .thenReturn(Optional.of(KoejaksonKoulutussopimus(korjausehdotus = "  ")))
        whenever(tiedot.valmistumispyyntoRepository.findByOpintooikeusId(11L)).thenReturn(Valmistumispyynto(virkailijanPalautusaika = paiva))

        assertThat(palvelu.getAvoimetAsiatForErikoistuja("opiskelija")!!.single().tyyppi).isEqualTo(AvoinAsiaTyyppiEnum.VALMISTUMISPYYNTO)
        verify(tiedot.messageSource).getMessage(eq("avoimetasiat.valmistumispyynto"), anyOrNull(), eq(Locale.forLanguageTag("fi")))
    }

    @Test
    fun `puuttuva kayttaja aiheuttaa edelleen EntityNotFoundException poikkeuksen`() {
        whenever(tiedot.userRepository.findById("opiskelija")).thenReturn(Optional.empty())

        assertThatThrownBy { palvelu.getAvoimetAsiatForErikoistuja("opiskelija") }.isInstanceOf(EntityNotFoundException::class.java)
    }
}
