package fi.elsapalvelu.elsa.service.kayttaja.etusivu

import fi.elsapalvelu.elsa.domain.kayttaja.Kayttaja
import fi.elsapalvelu.elsa.domain.kayttaja.KayttajaYliopistoErikoisala
import fi.elsapalvelu.elsa.domain.kayttaja.User
import fi.elsapalvelu.elsa.domain.perustiedot.Erikoisala
import fi.elsapalvelu.elsa.domain.perustiedot.Yliopisto
import fi.elsapalvelu.elsa.domain.perustiedot.YliopistoEnum
import fi.elsapalvelu.elsa.service.criteria.ErikoistujanEteneminenCriteria
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

class EtusivunSeurantaTest {
    private val tiedot = EtusivunTestitiedot()
    private val palvelu = tiedot.luoPalvelu()
    private val rajaimet = ErikoistujanEteneminenCriteria()
    private val sivutus = PageRequest.of(1, 5)

    @Test
    fun `puuttuvalle kayttajalle palautetaan tyhjat rajaimet ja null seurantasivut`() {
        assertThat(palvelu.getErikoistujienSeurantaKouluttajaRajaimet("puuttuva").erikoisalat).isEmpty()
        assertThat(palvelu.getErikoistujienSeurantaVastuuhenkiloRajaimet("puuttuva").erikoisalat).isEmpty()
        assertThat(palvelu.getErikoistujienSeurantaForKouluttaja("puuttuva", rajaimet, sivutus)).isNull()
        assertThat(palvelu.getErikoistujienSeurantaForVastuuhenkilo("puuttuva", rajaimet, sivutus)).isNull()
        assertThat(palvelu.getErikoistujienSeurantaForVirkailija("puuttuva", rajaimet, sivutus)).isNull()
        assertThat(palvelu.getKoulutettavienSeurantaForVastuuhenkilo("puuttuva", rajaimet, sivutus)).isNull()
        assertThat(palvelu.getKoulutettavienSeurantaForVirkailija("puuttuva", rajaimet, sivutus)).isNull()
    }

    @Test
    fun `rajaimet ryhmitellaan yliopistoon ja yhteinen erikoisalaluettelo jarjestetaan ilman kaksoiskappaleita`() {
        val helsinki = Yliopisto(id = 1L, nimi = YliopistoEnum.HELSINGIN_YLIOPISTO)
        val turku = Yliopisto(id = 2L, nimi = YliopistoEnum.TURUN_YLIOPISTO)
        val kayttaja = Kayttaja(yliopistotAndErikoisalat = mutableSetOf(
            KayttajaYliopistoErikoisala(id = 1L, yliopisto = helsinki, erikoisala = Erikoisala(nimi = "Z")),
            KayttajaYliopistoErikoisala(id = 2L, yliopisto = helsinki, erikoisala = Erikoisala(nimi = "A")),
            KayttajaYliopistoErikoisala(id = 3L, yliopisto = turku, erikoisala = Erikoisala(nimi = "A"))
        ))
        whenever(tiedot.kayttajaRepository.findOneByUserId("vastuuhenkilo")).thenReturn(Optional.of(kayttaja))

        val tulos = palvelu.getErikoistujienSeurantaVastuuhenkiloRajaimet("vastuuhenkilo")

        assertThat(tulos.erikoisalat).containsExactly("A", "Z")
        assertThat(tulos.kayttajaYliopistoErikoisalat!!.first().erikoisalat).containsExactly("A", "Z")
        assertThat(palvelu.getErikoistujienSeurantaKouluttajaRajaimet("vastuuhenkilo")).isEqualTo(tulos)
    }

    @Test
    fun `vastuuhenkilon haku valittaa kielivalinnan ja kayttooikeudet seka sailyttaa sivutuksen`() {
        val kayttaja = Kayttaja(id = 7L, user = User(langKey = "sv"))
        whenever(tiedot.kayttajaRepository.findOneByUserId("vastuuhenkilo")).thenReturn(Optional.of(kayttaja))
        whenever(tiedot.erikoistujienSeurantaQueryService.findByErikoisalaAndYliopisto(rajaimet, sivutus, "sv", kayttaja.yliopistotAndErikoisalat))
            .thenReturn(PageImpl(listOf(tiedot.opintooikeus), sivutus, 13))

        val tulos = palvelu.getErikoistujienSeurantaForVastuuhenkilo("vastuuhenkilo", rajaimet, sivutus)!!

        assertThat(tulos.totalElements).isEqualTo(13)
        assertThat(tulos.number).isEqualTo(1)
        assertThat(tulos.content.single().opintooikeusId).isEqualTo(11L)
    }

    @Test
    fun `virkailija ilman yliopistoa ei saa seurantaa`() {
        whenever(tiedot.kayttajaRepository.findOneByUserId("virkailija")).thenReturn(Optional.of(Kayttaja(id = 7L)))

        assertThat(palvelu.getErikoistujienSeurantaForVirkailija("virkailija", rajaimet, sivutus)).isNull()
        assertThat(palvelu.getKoulutettavienSeurantaForVirkailija("virkailija", rajaimet, sivutus)).isNull()
        verifyNoInteractions(tiedot.erikoistujienSeurantaQueryService)
    }
}
